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
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import io.shubham0204.smollmandroid.voice.WavUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Text-to-Speech Manager using Chaquopy Python bridge
 * Integrates with Pocket TTS / Sherpa ONNX for local TTS
 */
class TTSManager(private val context: Context) {

    companion object {
        private const val TAG = "TTSManager"
        private const val SAMPLE_RATE = 24000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var ttsModule: PyObject? = null
    private var ttsService: PyObject? = null
    private var audioTrack: AudioTrack? = null
    private var isPlaying = false

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    init {
        initializePython()
    }

    private fun initializePython() {
        try {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(context))
            }
            val python = Python.getInstance()

            ttsModule = python.getModule("tts_service")
            val modelDir = File(context.filesDir, "tts_models").absolutePath
            File(modelDir).mkdirs()

            ttsService = ttsModule?.callAttr("create_tts_service", modelDir)

            _isReady.value = ttsService != null
            Log.d(TAG, "Python TTS initialized: ${_isReady.value}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Python TTS", e)
            _isReady.value = false
        }
    }

    suspend fun speak(text: String, speed: Float = 1.0f) = withContext(Dispatchers.IO) {
        val module = ttsModule ?: return@withContext
        val service = ttsService ?: return@withContext
        if (text.isBlank()) return@withContext

        try {
            _isSpeaking.value = true

            val audioBytes = module.callAttr(
                "synthesize_text",
                service,
                text,
                0,
                speed
            )?.toJava(ByteArray::class.java)

            if (audioBytes != null && audioBytes.isNotEmpty()) {
                playAudio(audioBytes)
            } else {
                speakWithSystemTTS(text)
            }
        } catch (e: Exception) {
            Log.e(TAG, "TTS synthesis failed", e)
            speakWithSystemTTS(text)
        } finally {
            _isSpeaking.value = false
        }
    }

    suspend fun synthesizeToFile(text: String, outputFile: File, speed: Float = 1.0f): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val module = ttsModule ?: return@withContext false
                val service = ttsService ?: return@withContext false

                val audioBytes = module.callAttr(
                    "synthesize_text",
                    service,
                    text,
                    0,
                    speed
                )?.toJava(ByteArray::class.java)

                if (audioBytes != null && audioBytes.isNotEmpty()) {
                    val wavData = WavUtils.createWavFile(audioBytes, SAMPLE_RATE, 1, 16)
                    FileOutputStream(outputFile).use { it.write(wavData) }
                    return@withContext true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Synthesis to file failed", e)
            }
            return@withContext false
        }

    private fun playAudio(audioBytes: ByteArray) {
        try {
            stopAudio()

            val minBufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT
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
                        .setSampleRate(SAMPLE_RATE)
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

    private suspend fun speakWithSystemTTS(text: String) = withContext(Dispatchers.Main) {
        try {
            Log.d(TAG, "System TTS fallback: $text")
        } catch (e: Exception) {
            Log.e(TAG, "System TTS failed", e)
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
        ttsService = null
        ttsModule = null
    }
}
