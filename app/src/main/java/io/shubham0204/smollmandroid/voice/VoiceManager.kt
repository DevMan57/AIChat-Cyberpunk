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
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single
import java.io.File
import java.util.UUID

/**
 * Voice data class representing a saved voice
 */
@Serializable
data class Voice(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val filePath: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isDefault: Boolean = false
)

/**
 * Voice Manager - Handles multiple voice slots and voice assignments
 */
@Single
class VoiceManager(private val context: Context) {
    
    companion object {
        private const val TAG = "VoiceManager"
        private const val MAX_VOICES = 10
        private const val PREFS_NAME = "voice_manager_prefs"
        private const val KEY_VOICES_LIST = "voices_list"
        private const val KEY_CURRENT_VOICE_ID = "current_voice_id"
    }
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    
    private val voicesDir = File(context.filesDir, "voice_library").apply { mkdirs() }
    
    private val _voices = MutableStateFlow<List<Voice>>(emptyList())
    val voices: StateFlow<List<Voice>> = _voices
    
    private val _currentVoice = MutableStateFlow<Voice?>(null)
    val currentVoice: StateFlow<Voice?> = _currentVoice
    
    private var mediaPlayer: MediaPlayer? = null
    
    init {
        loadVoices()
    }
    
    /**
     * Get all saved voices
     */
    fun getVoices(): List<Voice> = _voices.value
    
    /**
     * Get voice by ID
     */
    fun getVoice(voiceId: String): Voice? = _voices.value.find { it.id == voiceId }
    
    /**
     * Add a new voice from a recording file
     */
    fun addVoice(name: String, sourceFile: File): Voice? {
        if (_voices.value.size >= MAX_VOICES) {
            Log.w(TAG, "Maximum voice limit reached ($MAX_VOICES)")
            return null
        }
        
        try {
            // Copy file to voices directory
            val voiceId = UUID.randomUUID().toString()
            val destFile = File(voicesDir, "$voiceId.wav")
            sourceFile.copyTo(destFile, overwrite = true)
            
            val voice = Voice(
                id = voiceId,
                name = name,
                filePath = destFile.absolutePath,
                createdAt = System.currentTimeMillis()
            )
            
            val updatedList = _voices.value + voice
            _voices.value = updatedList
            saveVoices(updatedList)
            
            // Set as current if first voice
            if (_voices.value.size == 1) {
                setCurrentVoice(voiceId)
            }
            
            Log.d(TAG, "Added voice: $name")
            return voice
            
        } catch (e: Exception) {
            Log.e(TAG, "Error adding voice", e)
            return null
        }
    }
    
    /**
     * Replace/update a voice recording
     */
    fun replaceVoice(voiceId: String, sourceFile: File): Boolean {
        val voice = _voices.value.find { it.id == voiceId } ?: return false
        
        return try {
            val destFile = File(voice.filePath)
            sourceFile.copyTo(destFile, overwrite = true)
            
            // Update timestamp
            val updatedVoice = voice.copy(createdAt = System.currentTimeMillis())
            val updatedList = _voices.value.map { 
                if (it.id == voiceId) updatedVoice else it 
            }
            _voices.value = updatedList
            saveVoices(updatedList)
            
            Log.d(TAG, "Replaced voice: ${voice.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error replacing voice", e)
            false
        }
    }
    
    /**
     * Rename a voice
     */
    fun renameVoice(voiceId: String, newName: String): Boolean {
        val voice = _voices.value.find { it.id == voiceId } ?: return false
        
        val updatedVoice = voice.copy(name = newName)
        val updatedList = _voices.value.map { 
            if (it.id == voiceId) updatedVoice else it 
        }
        _voices.value = updatedList
        saveVoices(updatedList)
        
        if (_currentVoice.value?.id == voiceId) {
            _currentVoice.value = updatedVoice
        }
        
        Log.d(TAG, "Renamed voice to: $newName")
        return true
    }
    
    /**
     * Delete a voice
     */
    fun deleteVoice(voiceId: String): Boolean {
        val voice = _voices.value.find { it.id == voiceId } ?: return false
        
        return try {
            // Delete file
            File(voice.filePath).delete()
            
            // Remove from list
            val updatedList = _voices.value.filter { it.id != voiceId }
            _voices.value = updatedList
            saveVoices(updatedList)
            
            // Reset current voice if needed
            if (_currentVoice.value?.id == voiceId) {
                _currentVoice.value = updatedList.firstOrNull()
                prefs.edit().putString(KEY_CURRENT_VOICE_ID, _currentVoice.value?.id).apply()
            }
            
            Log.d(TAG, "Deleted voice: ${voice.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting voice", e)
            false
        }
    }
    
    /**
     * Set the current active voice
     */
    fun setCurrentVoice(voiceId: String?) {
        val voice = voiceId?.let { getVoice(it) }
        _currentVoice.value = voice
        prefs.edit().putString(KEY_CURRENT_VOICE_ID, voiceId).apply()
        Log.d(TAG, "Set current voice to: ${voice?.name ?: "none"}")
    }
    
    /**
     * Play a voice sample
     */
    fun playVoice(voiceId: String, onComplete: (() -> Unit)? = null) {
        val voice = getVoice(voiceId) ?: return
        
        stopPlayback()
        
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(voice.filePath)
                prepare()
                setOnCompletionListener {
                    onComplete?.invoke()
                }
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing voice", e)
            onComplete?.invoke()
        }
    }
    
    /**
     * Stop voice playback
     */
    fun stopPlayback() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping playback", e)
        }
    }
    
    /**
     * Check if a voice is playing
     */
    fun isPlaying(): Boolean = mediaPlayer?.isPlaying ?: false
    
    /**
     * Get available voice slots count
     */
    fun getAvailableSlots(): Int = MAX_VOICES - _voices.value.size
    
    /**
     * Get max voice slots
     */
    fun getMaxSlots(): Int = MAX_VOICES
    
    /**
     * Export voice to external file
     */
    fun exportVoice(voiceId: String, destFile: File): Boolean {
        val voice = getVoice(voiceId) ?: return false
        
        return try {
            File(voice.filePath).copyTo(destFile, overwrite = true)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting voice", e)
            false
        }
    }
    
    /**
     * Import voice from external file
     */
    fun importVoice(name: String, sourceFile: File): Voice? {
        return addVoice(name, sourceFile)
    }
    
    /**
     * Get voice file for TTS processing
     */
    fun getVoiceFile(voiceId: String?): File? {
        return voiceId?.let { 
            getVoice(it)?.let { voice ->
                File(voice.filePath).takeIf { it.exists() }
            }
        }
    }
    
    private fun loadVoices() {
        try {
            val voicesJson = prefs.getString(KEY_VOICES_LIST, "[]") ?: "[]"
            val loadedVoices = json.decodeFromString<List<Voice>>(voicesJson)
            
            // Filter out voices with missing files
            val validVoices = loadedVoices.filter { File(it.filePath).exists() }
            
            _voices.value = validVoices
            
            // Load current voice
            val currentId = prefs.getString(KEY_CURRENT_VOICE_ID, null)
            _currentVoice.value = currentId?.let { getVoice(it) }
            
            Log.d(TAG, "Loaded ${validVoices.size} voices")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading voices", e)
            _voices.value = emptyList()
        }
    }
    
    private fun saveVoices(voices: List<Voice>) {
        try {
            val voicesJson = json.encodeToString(voices)
            prefs.edit().putString(KEY_VOICES_LIST, voicesJson).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving voices", e)
        }
    }
    
    fun release() {
        stopPlayback()
    }
}