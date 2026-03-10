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

package io.shubham0204.smollmandroid.voice

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.chaquo.python.Python
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Voice Cloning Manager
 * Handles voice recording and cloning using Pocket TTS
 */
class VoiceCloningManager(private val context: Context) {
    
    companion object {
        private const val TAG = "VoiceCloningManager"
        private const val SAMPLE_RATE = 24000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val MIN_RECORDING_DURATION_MS = 5000L // 5 seconds minimum
        private const val MAX_RECORDING_DURATION_MS = 30000L // 30 seconds maximum
    }
    
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var isRecording = false
    private var recordingStartTime: Long = 0
    
    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState
    
    private val _recordingDuration = MutableStateFlow(0L)
    val recordingDuration: StateFlow<Long> = _recordingDuration
    
    private val _clonedVoiceAvailable = MutableStateFlow(false)
    val clonedVoiceAvailable: StateFlow<Boolean> = _clonedVoiceAvailable
    
    private val voiceProfilesDir = File(context.filesDir, "voice_profiles")
    private val recordingsDir = File(context.filesDir, "recordings")
    
    sealed class RecordingState {
        object Idle : RecordingState()
        object Recording : RecordingState()
        object Processing : RecordingState()
        data class Error(val message: String) : RecordingState()
        data class Success(val file: File) : RecordingState()
    }
    
    init {
        voiceProfilesDir.mkdirs()
        recordingsDir.mkdirs()
        checkExistingVoiceProfile()
    }
    
    private fun checkExistingVoiceProfile() {
        val profileFile = File(voiceProfilesDir, "default_voice.npy")
        _clonedVoiceAvailable.value = profileFile.exists()
    }
    
    /**
     * Start recording reference audio for voice cloning
     */
    fun startRecording() {
        if (isRecording) return
        
        try {
            val minBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT
            )
            
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                minBufferSize * 2
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                _recordingState.value = RecordingState.Error("Failed to initialize audio recorder")
                return
            }
            
            isRecording = true
            recordingStartTime = System.currentTimeMillis()
            _recordingState.value = RecordingState.Recording
            _recordingDuration.value = 0
            
            audioRecord?.startRecording()
            
            recordingThread = Thread {
                recordAudio(minBufferSize)
            }
            recordingThread?.start()
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            _recordingState.value = RecordingState.Error(e.message ?: "Unknown error")
            stopRecording()
        }
    }
    
    /**
     * Stop recording and save the audio file
     */
    fun stopRecording(): File? {
        if (!isRecording) return null
        
        isRecording = false
        val duration = System.currentTimeMillis() - recordingStartTime
        
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            recordingThread?.join(1000)
            
            _recordingState.value = RecordingState.Idle
            
            // Check minimum duration
            if (duration < MIN_RECORDING_DURATION_MS) {
                _recordingState.value = RecordingState.Error(
                    "Recording too short. Please record at least 5 seconds."
                )
                return null
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
        }
        
        return getLastRecordingFile()
    }
    
    private fun recordAudio(bufferSize: Int) {
        val audioData = mutableListOf<Short>()
        val buffer = ShortArray(bufferSize / 2)
        
        while (isRecording) {
            val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
            if (read > 0) {
                for (i in 0 until read) {
                    audioData.add(buffer[i])
                }
            }
            
            // Update duration
            val duration = System.currentTimeMillis() - recordingStartTime
            _recordingDuration.value = duration
            
            // Auto-stop at max duration
            if (duration >= MAX_RECORDING_DURATION_MS) {
                stopRecording()
                break
            }
        }
        
        // Save to file
        if (audioData.isNotEmpty()) {
            saveRecording(audioData.toShortArray())
        }
    }
    
    private fun saveRecording(audioData: ShortArray) {
        try {
            val outputFile = File(recordingsDir, "reference_${System.currentTimeMillis()}.wav")
            
            // Convert to bytes
            val byteBuffer = ByteBuffer.allocate(audioData.size * 2)
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
            audioData.forEach { byteBuffer.putShort(it) }
            
            val wavData = createWavFile(byteBuffer.array(), SAMPLE_RATE, 1, 16)
            FileOutputStream(outputFile).use { it.write(wavData) }
            
            _recordingState.value = RecordingState.Success(outputFile)
            Log.d(TAG, "Recording saved: ${outputFile.absolutePath}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save recording", e)
            _recordingState.value = RecordingState.Error("Failed to save recording")
        }
    }
    
    /**
     * Clone voice from reference audio file
     */
    suspend fun cloneVoice(audioFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            _recordingState.value = RecordingState.Processing
            
            val python = Python.getInstance()
            val module = python.getModule("tts_service")
            
            // Call Python voice cloning
            val success = module.callAttr(
                "clone_voice_from_audio",
                module.callAttr("create_tts_service"),
                audioFile.absolutePath
            )?.toBoolean() ?: false
            
            if (success) {
                // Save voice profile
                val profileFile = File(voiceProfilesDir, "default_voice.npy")
                module.callAttr("save_voice_profile", 
                    module.callAttr("create_tts_service"),
                    profileFile.absolutePath
                )
                _clonedVoiceAvailable.value = true
            }
            
            _recordingState.value = RecordingState.Idle
            return@withContext success
            
        } catch (e: Exception) {
            Log.e(TAG, "Voice cloning failed", e)
            _recordingState.value = RecordingState.Error("Voice cloning failed: ${e.message}")
            return@withContext false
        }
    }
    
    /**
     * Load existing voice profile
     */
    fun loadVoiceProfile(): Boolean {
        return try {
            val profileFile = File(voiceProfilesDir, "default_voice.npy")
            if (profileFile.exists()) {
                val python = Python.getInstance()
                val module = python.getModule("tts_service")
                val success = module.callAttr(
                    "load_voice_profile",
                    module.callAttr("create_tts_service"),
                    profileFile.absolutePath
                )?.toBoolean() ?: false
                
                _clonedVoiceAvailable.value = success
                success
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load voice profile", e)
            false
        }
    }
    
    /**
     * Delete voice profile
     */
    fun deleteVoiceProfile(): Boolean {
        return try {
            val profileFile = File(voiceProfilesDir, "default_voice.npy")
            if (profileFile.exists()) {
                profileFile.delete()
            }
            _clonedVoiceAvailable.value = false
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete voice profile", e)
            false
        }
    }
    
    private fun getLastRecordingFile(): File? {
        return recordingsDir.listFiles()
            ?.filter { it.name.startsWith("reference_") }
            ?.maxByOrNull { it.lastModified() }
    }
    
    private fun createWavFile(
        pcmData: ByteArray,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ): ByteArray {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val totalDataLen = pcmData.size + 36
        val blockAlign = channels * bitsPerSample / 8
        
        val buffer = ByteBuffer.allocate(pcmData.size + 44)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        
        // WAV header
        buffer.put("RIFF".toByteArray())
        buffer.putInt(totalDataLen)
        buffer.put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray())
        buffer.putInt(16) // Subchunk1Size
        buffer.putShort(1) // AudioFormat (PCM)
        buffer.putShort(channels.toShort())
        buffer.putInt(sampleRate)
        buffer.putInt(byteRate)
        buffer.putShort(blockAlign.toShort())
        buffer.putShort(bitsPerSample.toShort())
        buffer.put("data".toByteArray())
        buffer.putInt(pcmData.size)
        buffer.put(pcmData)
        
        return buffer.array()
    }
    
    fun release() {
        stopRecording()
    }
}
