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

package io.shubham0204.smollmandroid.character

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.shubham0204.smollmandroid.data.AppRoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

private val Context.characterDataStore: DataStore<Preferences> by preferencesDataStore(name = "character_prefs")

/**
 * Character Manager - Handles character persistence and management
 */
@Single
class CharacterManager(private val context: Context) {
    
    companion object {
        private const val TAG = "CharacterManager"
        private val CURRENT_CHARACTER_ID = longPreferencesKey("current_character_id")
        private val CHARACTERS_LIST = stringPreferencesKey("characters_list")
    }
    
    private val dataStore = context.characterDataStore
    private val json = Json { ignoreUnknownKeys = true }
    
    private val _currentCharacter = MutableStateFlow<Character>(Character.createAva())
    val currentCharacter: StateFlow<Character> = _currentCharacter
    
    private val _characters = MutableStateFlow<List<Character>>(emptyList())
    val characters: StateFlow<List<Character>> = _characters
    
    init {
        loadCharacters()
        loadCurrentCharacter()
    }
    
    /**
     * Get the current active character
     */
    fun getCurrentCharacter(): Character = _currentCharacter.value
    
    /**
     * Set the current active character
     */
    suspend fun setCurrentCharacter(character: Character) {
        _currentCharacter.value = character
        dataStore.edit { prefs ->
            prefs[CURRENT_CHARACTER_ID] = character.id
        }
        Log.d(TAG, "Set current character to: ${character.name}")
    }
    
    /**
     * Get all characters including presets
     */
    fun getAllCharacters(): List<Character> {
        val presets = Character.getPresetCharacters()
        val custom = _characters.value
        return presets + custom
    }
    
    /**
     * Get only custom (user-created) characters
     */
    fun getCustomCharacters(): List<Character> = _characters.value
    
    /**
     * Add a new custom character
     */
    suspend fun addCharacter(character: Character): Character {
        val newId = System.currentTimeMillis() // Use timestamp as ID
        val newCharacter = character.copy(
            id = newId,
            isPreset = false,
            isDefault = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        val updatedList = _characters.value + newCharacter
        _characters.value = updatedList
        saveCharacters(updatedList)
        
        Log.d(TAG, "Added character: ${newCharacter.name}")
        return newCharacter
    }
    
    /**
     * Update an existing custom character
     */
    suspend fun updateCharacter(character: Character) {
        if (character.isPreset) {
            Log.w(TAG, "Cannot update preset character")
            return
        }
        
        val updatedList = _characters.value.map { 
            if (it.id == character.id) {
                character.copy(updatedAt = System.currentTimeMillis())
            } else it
        }
        _characters.value = updatedList
        saveCharacters(updatedList)
        
        // Update current character if it's the one being edited
        if (_currentCharacter.value.id == character.id) {
            _currentCharacter.value = character
        }
        
        Log.d(TAG, "Updated character: ${character.name}")
    }
    
    /**
     * Delete a custom character
     */
    suspend fun deleteCharacter(character: Character) {
        if (character.isPreset) {
            Log.w(TAG, "Cannot delete preset character")
            return
        }
        
        val updatedList = _characters.value.filter { it.id != character.id }
        _characters.value = updatedList
        saveCharacters(updatedList)
        
        // Reset to Ava if the deleted character was current
        if (_currentCharacter.value.id == character.id) {
            setCurrentCharacter(Character.createAva())
        }
        
        Log.d(TAG, "Deleted character: ${character.name}")
    }
    
    /**
     * Assign a voice to a character
     */
    suspend fun assignVoiceToCharacter(characterId: Long, voiceId: String?) {
        val character = getAllCharacters().find { it.id == characterId } ?: return
        
        if (character.isPreset) {
            // For preset characters, we need to create a custom copy
            val customCopy = character.copy(
                id = System.currentTimeMillis(),
                voiceId = voiceId,
                isPreset = false,
                isDefault = false
            )
            addCharacter(customCopy)
            setCurrentCharacter(customCopy)
        } else {
            updateCharacter(character.copy(voiceId = voiceId))
        }
    }
    
    /**
     * Reset to default character (Ava)
     */
    suspend fun resetToDefault() {
        setCurrentCharacter(Character.createAva())
    }
    
    /**
     * Duplicate a character
     */
    suspend fun duplicateCharacter(character: Character): Character {
        val duplicate = character.copy(
            id = System.currentTimeMillis(),
            name = "${character.name} (Copy)",
            isPreset = false,
            isDefault = false,
            createdAt = System.currentTimeMillis()
        )
        return addCharacter(duplicate)
    }
    
    private fun loadCharacters() {
        runBlocking {
            try {
                val charactersJson = dataStore.data.map { prefs ->
                    prefs[CHARACTERS_LIST] ?: "[]"
                }.first()
                
                val loadedCharacters = json.decodeFromString<List<Character>>(charactersJson)
                _characters.value = loadedCharacters
                Log.d(TAG, "Loaded ${loadedCharacters.size} custom characters")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading characters", e)
                _characters.value = emptyList()
            }
        }
    }
    
    private fun loadCurrentCharacter() {
        runBlocking {
            try {
                val currentId = dataStore.data.map { prefs ->
                    prefs[CURRENT_CHARACTER_ID] ?: -1L
                }.first()
                
                val character = getAllCharacters().find { it.id == currentId }
                    ?: Character.createAva()
                
                _currentCharacter.value = character
                Log.d(TAG, "Loaded current character: ${character.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading current character", e)
                _currentCharacter.value = Character.createAva()
            }
        }
    }
    
    private suspend fun saveCharacters(characters: List<Character>) {
        try {
            val charactersJson = json.encodeToString(characters)
            dataStore.edit { prefs ->
                prefs[CHARACTERS_LIST] = charactersJson
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving characters", e)
        }
    }
}