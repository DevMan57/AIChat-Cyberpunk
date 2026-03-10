/*
 * Copyright (C) 2024 AI Chat Android
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.shubham0204.smollmandroid.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import io.shubham0204.smollmandroid.voice.WavUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.LongBuffer
import java.util.concurrent.LinkedBlockingQueue
import kotlin.coroutines.coroutineContext
import java.util.Random

/**
 * Text-to-Speech Manager using PocketTTS ONNX models directly via ONNX Runtime.
 * 5-model pipeline: text_conditioner → mimi_encoder (voice cloning) → flow_lm_main
 *   → flow_lm_flow (Euler ODE) → mimi_decoder (audio).
 *
 * Streaming: audio frames are decoded in a parallel thread and written to AudioTrack
 * as they become available, giving low first-audio latency.
 */
class TTSManager(private val context: Context) {

    companion object {
        private const val TAG = "TTSManager"
        private const val SAMPLE_RATE = 24000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val MODEL_DIR_NAME = "tts_models/pocket_tts"

        // PocketTTS architecture constants
        private const val HIDDEN_DIM = 1024
        private const val LATENT_DIM = 32
        private const val NUM_KV_LAYERS = 6
        private const val KV_HEADS = 16
        private const val KV_HEAD_DIM = 64
        private const val KV_GROUPS = 2 // key + value
        private const val SAMPLES_PER_FRAME = 1920 // 80ms at 24kHz
        private const val DEFAULT_EULER_STEPS = 8
        private const val DEFAULT_TEMPERATURE = 0.7f
        private const val EOS_THRESHOLD = 0.0f
        private const val FRAMES_AFTER_EOS = 3
        private const val MAX_FRAMES = 2000 // safety limit ~160 seconds

        val REQUIRED_MODEL_FILES = listOf(
            "text_conditioner.onnx",
            "flow_lm_main_int8.onnx",
            "flow_lm_flow_int8.onnx",
            "mimi_decoder_int8.onnx",
            "mimi_encoder.onnx",
            "tokenizer.model",
        )
    }

    private var ortEnv: OrtEnvironment? = null
    private var textConditionerSession: OrtSession? = null
    private var flowLmMainSession: OrtSession? = null
    private var flowLmFlowSession: OrtSession? = null
    private var mimiDecoderSession: OrtSession? = null
    private var mimiEncoderSession: OrtSession? = null
    private var tokenizer: SentencePieceTokenizer? = null

    private var audioTrack: AudioTrack? = null
    @Volatile
    private var stopRequested = false

    // Cached speaker embedding to avoid re-encoding every call
    private var cachedVoiceFilePath: String? = null
    private var cachedSpeakerLatents: FloatArray? = null

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    init {
        initializeTTS()
    }

    private fun initializeTTS() {
        try {
            val modelDir = File(context.filesDir, MODEL_DIR_NAME)

            // Check all required files exist
            for (file in REQUIRED_MODEL_FILES) {
                if (!File(modelDir, file).exists()) {
                    Log.w(TAG, "Missing TTS model file: $file in ${modelDir.absolutePath}")
                    _isReady.value = false
                    return
                }
            }

            val env = OrtEnvironment.getEnvironment()
            ortEnv = env

            val numThreads = Runtime.getRuntime().availableProcessors().coerceIn(2, 4)

            fun createSession(fileName: String): OrtSession {
                val opts = OrtSession.SessionOptions()
                opts.setIntraOpNumThreads(numThreads)
                opts.setInterOpNumThreads(1)
                opts.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
                return env.createSession(
                    File(modelDir, fileName).absolutePath,
                    opts
                )
            }

            textConditionerSession = createSession("text_conditioner.onnx")
            flowLmMainSession = createSession("flow_lm_main_int8.onnx")
            flowLmFlowSession = createSession("flow_lm_flow_int8.onnx")
            mimiDecoderSession = createSession("mimi_decoder_int8.onnx")
            mimiEncoderSession = createSession("mimi_encoder.onnx")

            tokenizer = SentencePieceTokenizer.load(File(modelDir, "tokenizer.model"))

            _isReady.value = true
            Log.d(TAG, "PocketTTS engine initialized (threads=$numThreads)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize PocketTTS engine", e)
            _isReady.value = false
        }
    }

    /**
     * Speak text using PocketTTS. Optionally provide a voice reference file for cloning.
     */
    suspend fun speak(
        text: String,
        voiceFile: File? = null,
        eulerSteps: Int = DEFAULT_EULER_STEPS,
        temperature: Float = DEFAULT_TEMPERATURE,
    ) = withContext(Dispatchers.IO) {
        val env = ortEnv ?: return@withContext
        if (text.isBlank() || !_isReady.value) return@withContext

        try {
            stopRequested = false
            _isSpeaking.value = true

            val normalizedText = normalizeText(text)
            val tokenIds = tokenizer?.encode(normalizedText) ?: return@withContext
            if (tokenIds.isEmpty()) return@withContext

            // 1. Text conditioning
            val textEmbeddings = runTextConditioner(env, tokenIds)

            // 2. Voice cloning: encode reference audio if provided
            val speakerLatents = if (voiceFile != null) {
                getSpeakerLatents(env, voiceFile)
            } else null

            // 3. Autoregressive generation with streaming decode
            generateAndStream(env, textEmbeddings, tokenIds.size, speakerLatents, eulerSteps, temperature)

        } catch (e: Exception) {
            Log.e(TAG, "TTS synthesis failed", e)
        } finally {
            _isSpeaking.value = false
        }
    }

    /**
     * Synthesize text to a WAV file.
     */
    suspend fun synthesizeToFile(
        text: String,
        outputFile: File,
        voiceFile: File? = null,
        eulerSteps: Int = DEFAULT_EULER_STEPS,
        temperature: Float = DEFAULT_TEMPERATURE,
    ): Boolean = withContext(Dispatchers.IO) {
        val env = ortEnv ?: return@withContext false
        if (text.isBlank() || !_isReady.value) return@withContext false

        try {
            val normalizedText = normalizeText(text)
            val tokenIds = tokenizer?.encode(normalizedText) ?: return@withContext false
            if (tokenIds.isEmpty()) return@withContext false

            val textEmbeddings = runTextConditioner(env, tokenIds)
            val speakerLatents = if (voiceFile != null) getSpeakerLatents(env, voiceFile) else null

            val allAudio = generateToBuffer(env, textEmbeddings, tokenIds.size, speakerLatents, eulerSteps, temperature)
            if (allAudio.isEmpty()) return@withContext false

            val pcmBytes = floatSamplesToPcm16(allAudio)
            val wavData = WavUtils.createWavFile(pcmBytes, SAMPLE_RATE, 1, 16)
            FileOutputStream(outputFile).use { it.write(wavData) }
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Synthesis to file failed", e)
            return@withContext false
        }
    }

    // ── Text conditioning ──────────────────────────────────────────────

    private fun runTextConditioner(env: OrtEnvironment, tokenIds: IntArray): FloatArray {
        val session = textConditionerSession!!
        val inputIds = LongArray(tokenIds.size) { tokenIds[it].toLong() }
        val inputTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(inputIds), longArrayOf(1, inputIds.size.toLong()))

        val results = session.run(mapOf("input_ids" to inputTensor))
        val outputTensor = results[0].value as Array<Array<FloatArray>>
        val seqLen = outputTensor[0].size
        val embedDim = outputTensor[0][0].size

        // Flatten [1, seqLen, embedDim] to [seqLen * embedDim]
        val flat = FloatArray(seqLen * embedDim)
        for (t in 0 until seqLen) {
            System.arraycopy(outputTensor[0][t], 0, flat, t * embedDim, embedDim)
        }

        inputTensor.close()
        results.close()
        return flat
    }

    // ── Voice cloning: speaker encoding ────────────────────────────────

    private fun getSpeakerLatents(env: OrtEnvironment, voiceFile: File): FloatArray {
        // Use cache if same file
        val path = voiceFile.absolutePath
        cachedSpeakerLatents?.let {
            if (cachedVoiceFilePath == path) return it
        }

        val session = mimiEncoderSession!!
        val pcmSamples = loadWavAsPcmFloat(voiceFile)
        val inputTensor = OnnxTensor.createTensor(
            env,
            FloatBuffer.wrap(pcmSamples),
            longArrayOf(1, 1, pcmSamples.size.toLong())
        )

        val results = session.run(mapOf("audio" to inputTensor))
        val output = results[0].value as Array<Array<FloatArray>>
        val frames = output[0].size
        val dim = output[0][0].size
        val flat = FloatArray(frames * dim)
        for (f in 0 until frames) {
            System.arraycopy(output[0][f], 0, flat, f * dim, dim)
        }

        inputTensor.close()
        results.close()

        cachedVoiceFilePath = path
        cachedSpeakerLatents = flat
        Log.d(TAG, "Encoded speaker latents: $frames frames x $dim dim")
        return flat
    }

    fun invalidateVoiceCache() {
        cachedVoiceFilePath = null
        cachedSpeakerLatents = null
    }

    // ── Autoregressive generation ──────────────────────────────────────

    private suspend fun generateAndStream(
        env: OrtEnvironment,
        textEmbeddings: FloatArray,
        textLen: Int,
        speakerLatents: FloatArray?,
        eulerSteps: Int,
        temperature: Float,
    ) {
        // Set up streaming AudioTrack
        val minBuf = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val streamBufSize = minBuf.coerceAtLeast(SAMPLES_PER_FRAME * 4 * 2) // 4 frames buffer

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AUDIO_FORMAT)
                    .setChannelMask(CHANNEL_CONFIG)
                    .build()
            )
            .setBufferSizeInBytes(streamBufSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()

        // Latent queue for parallel decode
        val latentQueue = LinkedBlockingQueue<FloatArray?>(16)
        val decoderThread = Thread {
            runMimiDecoder(env, latentQueue)
        }
        decoderThread.start()

        try {
            runFlowLmLoop(env, textEmbeddings, textLen, speakerLatents, eulerSteps, temperature, latentQueue)
        } finally {
            latentQueue.put(null) // sentinel to stop decoder
            decoderThread.join(5000)
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        }
    }

    private suspend fun runFlowLmLoop(
        env: OrtEnvironment,
        textEmbeddings: FloatArray,
        textLen: Int,
        speakerLatents: FloatArray?,
        eulerSteps: Int,
        temperature: Float,
        latentQueue: LinkedBlockingQueue<FloatArray?>,
    ) {
        val session = flowLmMainSession!!

        // Get input/output names to understand the model's state interface
        val inputNames = session.inputNames.toList()
        val outputNames = session.outputNames.toList()

        Log.d(TAG, "FlowLM inputs: $inputNames")
        Log.d(TAG, "FlowLM outputs: $outputNames")

        // Build initial KV-cache state — 6 layers × (key, value) tensors
        val stateInputNames = inputNames.filter { it.contains("state") || it.contains("cache") || it.contains("kv") }
        val stateOutputNames = outputNames.filter { it.contains("state") || it.contains("cache") || it.contains("eos") }

        // Initialize state tensors — these will be updated each step
        var stateTensors = createInitialState(env, stateInputNames)

        // Create text embedding tensor (constant across steps)
        val textEmbTensor = OnnxTensor.createTensor(
            env,
            FloatBuffer.wrap(textEmbeddings),
            longArrayOf(1, textLen.toLong(), HIDDEN_DIM.toLong())
        )

        var framesAfterEos = 0
        var eosDetected = false

        for (step in 0 until MAX_FRAMES) {
            if (stopRequested || !coroutineContext.isActive) break

            // Build input latent: NaN-filled on first step (no prior audio), or zeros
            val inputLatent = if (step == 0 && speakerLatents == null) {
                FloatArray(LATENT_DIM) { Float.NaN }
            } else {
                FloatArray(LATENT_DIM) { 0.0f }
            }

            val latentTensor = OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(inputLatent),
                longArrayOf(1, 1, LATENT_DIM.toLong())
            )

            // Build inputs map
            val inputs = mutableMapOf<String, OnnxTensor>()
            inputs["sequence"] = latentTensor

            // Add text embeddings
            val textInputName = inputNames.find { it.contains("text") || it.contains("embed") || it.contains("cond") }
            if (textInputName != null) {
                inputs[textInputName] = textEmbTensor
            }

            // Add state tensors
            for ((name, tensor) in stateTensors) {
                inputs[name] = tensor
            }

            // Add speaker latents if provided
            if (speakerLatents != null) {
                val speakerInputName = inputNames.find { it.contains("speaker") || it.contains("prompt") }
                if (speakerInputName != null) {
                    inputs[speakerInputName] = OnnxTensor.createTensor(
                        env,
                        FloatBuffer.wrap(speakerLatents),
                        longArrayOf(1, (speakerLatents.size / LATENT_DIM).toLong(), LATENT_DIM.toLong())
                    )
                }
            }

            // Run flow_lm_main
            val results = session.run(inputs)

            // Extract conditioning vector and EOS logit
            val resultMap = outputNames.associateWith { name -> results.get(name) }
            val conditioningOutput = resultMap.entries.find {
                !it.key.contains("state") && !it.key.contains("cache") &&
                !it.key.contains("eos") && !it.key.contains("kv")
            }
            val eosOutput = resultMap.entries.find { it.key.contains("eos") }

            val conditioning = extractFloatArray(conditioningOutput?.value?.get()?.value)
            val eosLogit = extractScalar(eosOutput?.value?.get()?.value)

            // Update state tensors for next step
            val stateOutNames = outputNames.filter { it.contains("state") || it.contains("cache") || it.contains("kv") }
            val newStateTensors = mutableMapOf<String, OnnxTensor>()
            for ((i, name) in stateInputNames.withIndex()) {
                if (i < stateOutNames.size) {
                    val outVal = results.get(stateOutNames[i])
                    if (outVal?.isPresent == true) {
                        newStateTensors[name] = OnnxTensor.createTensor(env, (outVal.get() as OnnxTensor).value)
                    }
                }
            }

            // Clean up old state tensors
            for ((_, tensor) in stateTensors) {
                tensor.close()
            }
            stateTensors = newStateTensors

            latentTensor.close()
            results.close()

            // Check EOS
            if (eosLogit > EOS_THRESHOLD) {
                eosDetected = true
            }
            if (eosDetected) {
                framesAfterEos++
                if (framesAfterEos > FRAMES_AFTER_EOS) break
            }

            // Run Euler ODE solver to produce audio latent
            if (conditioning != null) {
                val audioLatent = runEulerSolver(env, conditioning, eulerSteps, temperature)
                latentQueue.put(audioLatent)
            }
        }

        // Cleanup
        textEmbTensor.close()
        for ((_, tensor) in stateTensors) {
            tensor.close()
        }
    }

    /**
     * Euler ODE solver using flow_lm_flow.
     * x starts from noise, iteratively refined via flow predictions.
     */
    private fun runEulerSolver(
        env: OrtEnvironment,
        conditioning: FloatArray,
        numSteps: Int,
        temperature: Float,
    ): FloatArray {
        val session = flowLmFlowSession!!
        val inputNames = session.inputNames.toList()

        // Start from Gaussian noise scaled by temperature
        val rng = Random()
        val x = FloatArray(LATENT_DIM) { rng.nextGaussian().toFloat() * temperature }
        val dt = 1.0f / numSteps

        for (step in 0 until numSteps) {
            val t = step.toFloat() / numSteps
            val s = (step + 1).toFloat() / numSteps

            // Create input tensors
            val condTensor = OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(conditioning),
                longArrayOf(1, conditioning.size.toLong())
            )
            val sTensor = OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(floatArrayOf(s)),
                longArrayOf(1)
            )
            val tTensor = OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(floatArrayOf(t)),
                longArrayOf(1)
            )
            val xTensor = OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(x),
                longArrayOf(1, LATENT_DIM.toLong())
            )

            // Build inputs — match names adaptively
            val inputs = mutableMapOf<String, OnnxTensor>()
            for (name in inputNames) {
                when {
                    name.contains("cond") || (name == "c") -> inputs[name] = condTensor
                    name == "s" || name.startsWith("s_") -> inputs[name] = sTensor
                    name == "t" || name.startsWith("t_") -> inputs[name] = tTensor
                    name == "x" || name.contains("current") || name.contains("noisy") -> inputs[name] = xTensor
                }
            }

            // Fallback: positional mapping if adaptive didn't fill all
            if (inputs.size < inputNames.size) {
                inputs.clear()
                if (inputNames.size >= 4) {
                    inputs[inputNames[0]] = condTensor
                    inputs[inputNames[1]] = sTensor
                    inputs[inputNames[2]] = tTensor
                    inputs[inputNames[3]] = xTensor
                }
            }

            val results = session.run(inputs)
            val flowDir = extractFloatArray(results[0].value) ?: FloatArray(LATENT_DIM)

            // Euler step: x += flow_dir * dt
            for (i in x.indices) {
                x[i] += flowDir[i] * dt
            }

            condTensor.close()
            sTensor.close()
            tTensor.close()
            xTensor.close()
            results.close()
        }

        return x
    }

    // ── Mimi decoder (runs in separate thread) ─────────────────────────

    private fun runMimiDecoder(
        env: OrtEnvironment,
        latentQueue: LinkedBlockingQueue<FloatArray?>,
    ) {
        val session = mimiDecoderSession!!
        val inputNames = session.inputNames.toList()
        val outputNames = session.outputNames.toList()

        // Initialize decoder state
        val stateInputNames = inputNames.filter { it.contains("state") || it.contains("cache") || it.contains("kv") }
        var stateTensors = createInitialState(env, stateInputNames)

        try {
            while (!stopRequested) {
                val latent = latentQueue.take() ?: break // null = sentinel

                val latentTensor = OnnxTensor.createTensor(
                    env,
                    FloatBuffer.wrap(latent),
                    longArrayOf(1, LATENT_DIM.toLong())
                )

                val inputs = mutableMapOf<String, OnnxTensor>()
                val latentInputName = inputNames.find { !it.contains("state") && !it.contains("cache") && !it.contains("kv") }
                if (latentInputName != null) {
                    inputs[latentInputName] = latentTensor
                }
                for ((name, tensor) in stateTensors) {
                    inputs[name] = tensor
                }

                val results = session.run(inputs)

                // Extract audio frame
                val audioOutput = outputNames.find { !it.contains("state") && !it.contains("cache") && !it.contains("kv") }
                val audioData = extractFloatArray(results.get(audioOutput)?.get()?.value)

                // Update decoder state
                val stateOutNames = outputNames.filter { it.contains("state") || it.contains("cache") || it.contains("kv") }
                val newStateTensors = mutableMapOf<String, OnnxTensor>()
                for ((i, name) in stateInputNames.withIndex()) {
                    if (i < stateOutNames.size) {
                        val outVal = results.get(stateOutNames[i])
                        if (outVal?.isPresent == true) {
                            newStateTensors[name] = OnnxTensor.createTensor(env, (outVal.get() as OnnxTensor).value)
                        }
                    }
                }
                for ((_, tensor) in stateTensors) tensor.close()
                stateTensors = newStateTensors

                latentTensor.close()
                results.close()

                // Write audio to AudioTrack
                if (audioData != null) {
                    val pcmBytes = floatSamplesToPcm16(audioData)
                    audioTrack?.write(pcmBytes, 0, pcmBytes.size)
                }
            }
        } catch (e: Exception) {
            if (!stopRequested) {
                Log.e(TAG, "Mimi decoder error", e)
            }
        } finally {
            for ((_, tensor) in stateTensors) tensor.close()
        }
    }

    // ── Non-streaming generation (for synthesizeToFile) ────────────────

    private suspend fun generateToBuffer(
        env: OrtEnvironment,
        textEmbeddings: FloatArray,
        textLen: Int,
        speakerLatents: FloatArray?,
        eulerSteps: Int,
        temperature: Float,
    ): FloatArray {
        val allFrames = mutableListOf<FloatArray>()
        val latentQueue = LinkedBlockingQueue<FloatArray?>(16)

        // Collect decoded audio frames
        val decoderThread = Thread {
            val session = mimiDecoderSession!!
            val inputNames = session.inputNames.toList()
            val outputNames = session.outputNames.toList()
            val stateInputNames = inputNames.filter { it.contains("state") || it.contains("cache") || it.contains("kv") }
            var stateTensors = createInitialState(env, stateInputNames)

            try {
                while (true) {
                    val latent = latentQueue.take() ?: break
                    val latentTensor = OnnxTensor.createTensor(
                        env, FloatBuffer.wrap(latent), longArrayOf(1, LATENT_DIM.toLong())
                    )
                    val inputs = mutableMapOf<String, OnnxTensor>()
                    val latentInputName = inputNames.find { !it.contains("state") && !it.contains("cache") && !it.contains("kv") }
                    if (latentInputName != null) inputs[latentInputName] = latentTensor
                    for ((name, tensor) in stateTensors) inputs[name] = tensor

                    val results = session.run(inputs)
                    val audioOutput = outputNames.find { !it.contains("state") && !it.contains("cache") && !it.contains("kv") }
                    val audioData = extractFloatArray(results.get(audioOutput)?.get()?.value)
                    if (audioData != null) {
                        synchronized(allFrames) { allFrames.add(audioData) }
                    }

                    val stateOutNames = outputNames.filter { it.contains("state") || it.contains("cache") || it.contains("kv") }
                    val newState = mutableMapOf<String, OnnxTensor>()
                    for ((i, name) in stateInputNames.withIndex()) {
                        if (i < stateOutNames.size) {
                            val outVal = results.get(stateOutNames[i])
                            if (outVal?.isPresent == true) {
                                newState[name] = OnnxTensor.createTensor(env, (outVal.get() as OnnxTensor).value)
                            }
                        }
                    }
                    for ((_, t) in stateTensors) t.close()
                    stateTensors = newState
                    latentTensor.close()
                    results.close()
                }
            } finally {
                for ((_, t) in stateTensors) t.close()
            }
        }
        decoderThread.start()

        runFlowLmLoop(env, textEmbeddings, textLen, speakerLatents, eulerSteps, temperature, latentQueue)
        latentQueue.put(null)
        decoderThread.join(30000)

        // Concatenate all audio frames
        val totalSamples = allFrames.sumOf { it.size }
        val result = FloatArray(totalSamples)
        var offset = 0
        for (frame in allFrames) {
            System.arraycopy(frame, 0, result, offset, frame.size)
            offset += frame.size
        }
        return result
    }

    // ── State initialization ───────────────────────────────────────────

    private fun createInitialState(env: OrtEnvironment, stateNames: List<String>): MutableMap<String, OnnxTensor> {
        val state = mutableMapOf<String, OnnxTensor>()
        for (name in stateNames) {
            // Detect shape from name patterns. Most PocketTTS ONNX exports use:
            // KV cache: [2, 1, 0, 16, 64] (empty sequence dim)
            // Counters: scalar long
            if (name.contains("counter") || name.contains("step") || name.contains("pos")) {
                state[name] = OnnxTensor.createTensor(env, LongBuffer.wrap(longArrayOf(0)), longArrayOf(1))
            } else {
                // KV cache with 0-length sequence dimension
                val zeros = FloatArray(0)
                state[name] = OnnxTensor.createTensor(
                    env,
                    FloatBuffer.wrap(zeros),
                    longArrayOf(KV_GROUPS.toLong(), 1, 0, KV_HEADS.toLong(), KV_HEAD_DIM.toLong())
                )
            }
        }
        return state
    }

    // ── Utilities ──────────────────────────────────────────────────────

    private fun normalizeText(text: String): String {
        var t = text.trim()
        // Ensure text ends with punctuation (PocketTTS requires this)
        if (t.isNotEmpty() && t.last() !in ".!?;:,") {
            t += "."
        }
        return t
    }

    private fun extractFloatArray(value: Any?): FloatArray? {
        return when (value) {
            is FloatArray -> value
            is Array<*> -> {
                @Suppress("UNCHECKED_CAST")
                try {
                    when (val first = value[0]) {
                        is FloatArray -> first
                        is Array<*> -> (first as? Array<FloatArray>)?.get(0)
                        else -> null
                    }
                } catch (_: Exception) { null }
            }
            else -> null
        }
    }

    private fun extractScalar(value: Any?): Float {
        return when (value) {
            is Float -> value
            is FloatArray -> if (value.isNotEmpty()) value[0] else 0f
            is Array<*> -> {
                val inner = value[0]
                when (inner) {
                    is Float -> inner
                    is FloatArray -> if (inner.isNotEmpty()) inner[0] else 0f
                    else -> 0f
                }
            }
            else -> 0f
        }
    }

    private fun floatSamplesToPcm16(samples: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(samples.size * 2)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        for (sample in samples) {
            val clamped = sample.coerceIn(-1.0f, 1.0f)
            val pcm16 = (clamped * 32767.0f).toInt().toShort()
            buffer.putShort(pcm16)
        }
        return buffer.array()
    }

    private fun loadWavAsPcmFloat(file: File): FloatArray {
        val bytes = file.readBytes()
        if (bytes.size < 44) return floatArrayOf()

        // Parse WAV header
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        buf.position(22)
        val channels = buf.short.toInt()
        val sampleRate = buf.int
        buf.position(34)
        val bitsPerSample = buf.short.toInt()

        // Find data chunk
        var dataStart = 44
        var dataSize = bytes.size - 44
        buf.position(36)
        while (buf.remaining() >= 8) {
            val chunkId = ByteArray(4)
            buf.get(chunkId)
            val chunkSize = buf.int
            if (String(chunkId) == "data") {
                dataStart = buf.position()
                dataSize = chunkSize
                break
            }
            buf.position(buf.position() + chunkSize)
        }

        val dataBuf = ByteBuffer.wrap(bytes, dataStart, dataSize.coerceAtMost(bytes.size - dataStart))
            .order(ByteOrder.LITTLE_ENDIAN)

        val numSamples = dataSize / (bitsPerSample / 8) / channels
        val samples = FloatArray(numSamples)

        for (i in 0 until numSamples) {
            if (!dataBuf.hasRemaining()) break
            when (bitsPerSample) {
                16 -> {
                    val s = dataBuf.short
                    samples[i] = s.toFloat() / 32768.0f
                    for (c in 1 until channels) {
                        if (dataBuf.hasRemaining()) dataBuf.short
                    }
                }
                32 -> {
                    samples[i] = dataBuf.float
                    for (c in 1 until channels) {
                        if (dataBuf.hasRemaining()) dataBuf.float
                    }
                }
                else -> samples[i] = 0f
            }
        }

        // Resample to 24kHz if needed
        if (sampleRate != SAMPLE_RATE) {
            return resample(samples, sampleRate, SAMPLE_RATE)
        }
        return samples
    }

    private fun resample(input: FloatArray, fromRate: Int, toRate: Int): FloatArray {
        val ratio = toRate.toDouble() / fromRate
        val outputLen = (input.size * ratio).toInt()
        val output = FloatArray(outputLen)
        for (i in 0 until outputLen) {
            val srcIdx = i / ratio
            val idx = srcIdx.toInt()
            val frac = (srcIdx - idx).toFloat()
            output[i] = if (idx + 1 < input.size) {
                input[idx] * (1 - frac) + input[idx + 1] * frac
            } else if (idx < input.size) {
                input[idx]
            } else 0f
        }
        return output
    }

    fun stopAudio() {
        stopRequested = true
        try {
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio", e)
        }
    }

    fun release() {
        stopAudio()
        textConditionerSession?.close()
        flowLmMainSession?.close()
        flowLmFlowSession?.close()
        mimiDecoderSession?.close()
        mimiEncoderSession?.close()
        textConditionerSession = null
        flowLmMainSession = null
        flowLmFlowSession = null
        mimiDecoderSession = null
        mimiEncoderSession = null
        ortEnv?.close()
        ortEnv = null
    }

    /**
     * Check if all required model files are present on disk.
     */
    fun areModelsDownloaded(): Boolean {
        val modelDir = File(context.filesDir, MODEL_DIR_NAME)
        return REQUIRED_MODEL_FILES.all { File(modelDir, it).exists() }
    }
}
