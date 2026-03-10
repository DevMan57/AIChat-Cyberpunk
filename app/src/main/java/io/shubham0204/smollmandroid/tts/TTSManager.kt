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
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import io.shubham0204.smollmandroid.voice.WavUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Text-to-Speech Manager using Sherpa ONNX directly (no Python bridge).
 * Uses Kyutai Pocket TTS ONNX model for fast on-device synthesis.
 */
class TTSManager(private val context: Context) {

    companion object {
        private const val TAG = "TTSManager"
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var tts: OfflineTts? = null
    private var sampleRate: Int = 24000
    private var audioTrack: AudioTrack? = null
    @Volatile
    private var isPlaying = false

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    init {
        initializeTTS()
    }

    private fun initializeTTS() {
        try {
            val modelDir = File(context.filesDir, "tts_models")
            modelDir.mkdirs()

            val modelFile = File(modelDir, "model.onnx")
            val tokensFile = File(modelDir, "tokens.txt")

            if (!modelFile.exists() || !tokensFile.exists()) {
                Log.w(TAG, "TTS model files not found in ${modelDir.absolutePath}")
                _isReady.value = false
                return
            }

            // Detect espeak-ng data dir (used by VITS models)
            val espeakDataDir = File(modelDir, "espeak-ng-data")

            val vitsConfig = OfflineTtsVitsModelConfig(
                model = modelFile.absolutePath,
                tokens = tokensFile.absolutePath,
                dataDir = if (espeakDataDir.exists()) espeakDataDir.absolutePath else "",
                noiseScale = 0.667f,
                noiseScaleW = 0.8f,
                lengthScale = 1.0f,
            )

            val modelConfig = OfflineTtsModelConfig(
                vits = vitsConfig,
                // Use efficient thread count for Snapdragon 8 Gen 3
                numThreads = Runtime.getRuntime().availableProcessors().coerceIn(2, 4),
                debug = false,
            )

            val ttsConfig = OfflineTtsConfig(
                model = modelConfig,
            )

            tts = OfflineTts(config = ttsConfig)
            sampleRate = tts?.sampleRate() ?: 24000

            _isReady.value = true
            Log.d(TAG, "Sherpa ONNX TTS initialized (sampleRate=$sampleRate)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Sherpa ONNX TTS", e)
            _isReady.value = false
        }
    }

    suspend fun speak(text: String, speakerId: Int = 0, speed: Float = 1.0f) = withContext(Dispatchers.IO) {
        val engine = tts ?: return@withContext
        if (text.isBlank()) return@withContext

        try {
            _isSpeaking.value = true

            val audio = engine.generate(text = text, sid = speakerId, speed = speed)
            val pcmBytes = floatSamplesToPcm16(audio.samples)

            if (pcmBytes.isNotEmpty()) {
                playAudio(pcmBytes, audio.sampleRate)
            }
        } catch (e: Exception) {
            Log.e(TAG, "TTS synthesis failed", e)
        } finally {
            _isSpeaking.value = false
        }
    }

    suspend fun synthesizeToFile(text: String, outputFile: File, speakerId: Int = 0, speed: Float = 1.0f): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val engine = tts ?: return@withContext false

                val audio = engine.generate(text = text, sid = speakerId, speed = speed)
                val pcmBytes = floatSamplesToPcm16(audio.samples)

                if (pcmBytes.isNotEmpty()) {
                    val wavData = WavUtils.createWavFile(pcmBytes, audio.sampleRate, 1, 16)
                    FileOutputStream(outputFile).use { it.write(wavData) }
                    return@withContext true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Synthesis to file failed", e)
            }
            return@withContext false
        }

    /**
     * Convert float samples [-1.0, 1.0] to PCM 16-bit byte array.
     */
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

    private fun playAudio(audioBytes: ByteArray, rate: Int = sampleRate) {
        try {
            stopAudio()

            val minBufferSize = AudioTrack.getMinBufferSize(
                rate, CHANNEL_CONFIG, AUDIO_FORMAT
            )

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(rate)
                        .setEncoding(AUDIO_FORMAT)
                        .setChannelMask(CHANNEL_CONFIG)
                        .build()
                )
                .setBufferSizeInBytes(minBufferSize.coerceAtLeast(audioBytes.size))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack?.write(audioBytes, 0, audioBytes.size)
            audioTrack?.play()
            isPlaying = true
        } catch (e: Exception) {
            Log.e(TAG, "Audio playback failed", e)
        }
    }

    fun stopAudio() {
        try {
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
            isPlaying = false
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio", e)
        }
    }

    fun release() {
        stopAudio()
        tts?.release()
        tts = null
    }
}
