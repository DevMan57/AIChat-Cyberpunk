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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.shubham0204.smollmandroid.theme.CyberpunkColors
import io.shubham0204.smollmandroid.theme.neonBorder
import io.shubham0204.smollmandroid.voice.Voice
import io.shubham0204.smollmandroid.voice.VoiceManager
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * Character Builder Screen
 * UI for creating and editing custom characters
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterBuilderScreen(
    characterToEdit: Character? = null,
    onNavigateBack: () -> Unit,
    characterManager: CharacterManager = koinInject()
) {
    val scope = rememberCoroutineScope()
    val voiceManager = koinInject<VoiceManager>()
    
    // Form state
    var name by remember { mutableStateOf(characterToEdit?.name ?: "") }
    var description by remember { mutableStateOf(characterToEdit?.description ?: "") }
    var systemPrompt by remember { mutableStateOf(characterToEdit?.systemPrompt ?: "") }
    var selectedAvatar by remember { mutableStateOf(characterToEdit?.avatarType ?: AvatarType.DEFAULT) }
    var selectedVoiceId by remember { mutableStateOf(characterToEdit?.voiceId) }
    
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf<String?>(null) }
    
    val isEditing = characterToEdit != null
    val voices = voiceManager.getVoices()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (isEditing) "Edit Character" else "Create Character",
                        color = CyberpunkColors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = CyberpunkColors.lightsaberGreen
                        )
                    }
                },
                actions = {
                    if (isEditing && !characterToEdit!!.isPreset) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = CyberpunkColors.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CyberpunkColors.backgroundPrimary
                )
            )
        },
        containerColor = CyberpunkColors.backgroundPrimary
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error message
            AnimatedVisibility(visible = showError != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            CyberpunkColors.error.copy(alpha = 0.2f),
                            RoundedCornerShape(8.dp)
                        )
                        .border(1.dp, CyberpunkColors.error, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        showError ?: "",
                        color = CyberpunkColors.error,
                        fontSize = 14.sp
                    )
                }
            }
            
            // Avatar Selection
            AvatarSelector(
                selectedAvatar = selectedAvatar,
                onAvatarSelected = { selectedAvatar = it }
            )
            
            // Name Field
            CyberpunkTextField(
                value = name,
                onValueChange = { name = it },
                label = "Character Name",
                placeholder = "e.g., Cyber Assistant",
                singleLine = true
            )
            
            // Description Field
            CyberpunkTextField(
                value = description,
                onValueChange = { description = it },
                label = "Description",
                placeholder = "Brief description of the character...",
                maxLines = 2
            )
            
            // System Prompt Field
            CyberpunkTextField(
                value = systemPrompt,
                onValueChange = { systemPrompt = it },
                label = "System Prompt",
                placeholder = "Define how the AI should behave as this character...",
                minLines = 6,
                maxLines = 10
            )
            
            // Voice Selection
            if (voices.isNotEmpty()) {
                VoiceSelector(
                    voices = voices,
                    selectedVoiceId = selectedVoiceId,
                    onVoiceSelected = { selectedVoiceId = it }
                )
            }
            
            // Template suggestions
            if (!isEditing) {
                TemplateSuggestions { template ->
                    name = template.name
                    description = template.description
                    systemPrompt = template.systemPrompt
                    selectedAvatar = template.avatarType
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Save Button
            Button(
                onClick = {
                    when {
                        name.isBlank() -> showError = "Please enter a character name"
                        systemPrompt.isBlank() -> showError = "Please enter a system prompt"
                        else -> {
                            showError = null
                            val character = Character(
                                id = characterToEdit?.id ?: 0,
                                name = name.trim(),
                                description = description.trim(),
                                systemPrompt = systemPrompt.trim(),
                                avatarType = selectedAvatar,
                                voiceId = selectedVoiceId
                            )
                            
                            scope.launch {
                                if (isEditing) {
                                    characterManager.updateCharacter(character)
                                } else {
                                    characterManager.addCharacter(character)
                                }
                                onNavigateBack()
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberpunkColors.lightsaberGreen,
                    contentColor = CyberpunkColors.backgroundPrimary
                )
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isEditing) "Update Character" else "Create Character")
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = CyberpunkColors.backgroundSecondary,
            title = { 
                Text(
                    "Delete Character?",
                    color = CyberpunkColors.textPrimary
                )
            },
            text = { 
                Text(
                    "Are you sure you want to delete '${characterToEdit?.name}'? This action cannot be undone.",
                    color = CyberpunkColors.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            characterToEdit?.let { characterManager.deleteCharacter(it) }
                            onNavigateBack()
                        }
                    }
                ) {
                    Text("Delete", color = CyberpunkColors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = CyberpunkColors.lightsaberGreen)
                }
            }
        )
    }
}

@Composable
private fun CyberpunkTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE
) {
    Column {
        Text(
            label,
            color = CyberpunkColors.lightsaberGreen,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = CyberpunkColors.textMuted) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = if (singleLine) ImeAction.Next else ImeAction.Default
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberpunkColors.lightsaberGreen,
                unfocusedBorderColor = CyberpunkColors.borderCyan,
                focusedTextColor = CyberpunkColors.textPrimary,
                unfocusedTextColor = CyberpunkColors.textPrimary,
                focusedContainerColor = CyberpunkColors.backgroundCard,
                unfocusedContainerColor = CyberpunkColors.backgroundCard
            ),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
private fun AvatarSelector(
    selectedAvatar: AvatarType,
    onAvatarSelected: (AvatarType) -> Unit
) {
    Column {
        Text(
            "Select Avatar",
            color = CyberpunkColors.lightsaberGreen,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AvatarType.values().forEach { avatarType ->
                val isSelected = avatarType == selectedAvatar
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onAvatarSelected(avatarType) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                if (isSelected) CyberpunkColors.lightsaberGreen.copy(alpha = 0.2f)
                                else CyberpunkColors.backgroundCard,
                                CircleShape
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) CyberpunkColors.lightsaberGreen
                                else CyberpunkColors.borderCyan,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            avatarType.getIcon(),
                            fontSize = 32.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        avatarType.getDisplayName(),
                        color = if (isSelected) CyberpunkColors.lightsaberGreen else CyberpunkColors.textSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun VoiceSelector(
    voices: List<Voice>,
    selectedVoiceId: String?,
    onVoiceSelected: (String?) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Assign Voice",
                color = CyberpunkColors.lightsaberGreen,
                fontSize = 14.sp
            )
            if (selectedVoiceId != null) {
                TextButton(onClick = { onVoiceSelected(null) }) {
                    Text("Clear", color = CyberpunkColors.textMuted, fontSize = 12.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // "No voice" option
            VoiceChip(
                name = "Default Voice",
                isSelected = selectedVoiceId == null,
                onClick = { onVoiceSelected(null) }
            )
            
            voices.forEach { voice ->
                VoiceChip(
                    name = voice.name,
                    isSelected = voice.id == selectedVoiceId,
                    onClick = { onVoiceSelected(voice.id) }
                )
            }
        }
    }
}

@Composable
private fun VoiceChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) CyberpunkColors.lightsaberPurple.copy(alpha = 0.3f)
                else CyberpunkColors.backgroundCard
            )
            .border(
                width = if (isSelected) 1.dp else 0.5.dp,
                color = if (isSelected) CyberpunkColors.lightsaberPurple
                else CyberpunkColors.borderCyan.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = CyberpunkColors.lightsaberPurple,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                name,
                color = if (isSelected) CyberpunkColors.lightsaberPurple else CyberpunkColors.textSecondary,
                fontSize = 14.sp
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TemplateSuggestions(onTemplateSelected: (Template) -> Unit) {
    Column {
        Text(
            "Quick Templates",
            color = CyberpunkColors.lightsaberGreen,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            templates.forEach { template ->
                TextButton(
                    onClick = { onTemplateSelected(template) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = CyberpunkColors.lightsaberPurple
                    ),
                    modifier = Modifier
                        .background(
                            CyberpunkColors.lightsaberPurple.copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            1.dp,
                            CyberpunkColors.lightsaberPurple.copy(alpha = 0.5f),
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    Text(template.name, fontSize = 12.sp)
                }
            }
        }
    }
}

private data class Template(
    val name: String,
    val description: String,
    val systemPrompt: String,
    val avatarType: AvatarType
)

private val templates = listOf(
    Template(
        name = "Code Assistant",
        description = "Expert programming assistant",
        systemPrompt = "You are an expert programming assistant. You provide clear, efficient code solutions with explanations. You're knowledgeable about multiple programming languages and best practices.",
        avatarType = AvatarType.MALE_AI
    ),
    Template(
        name = "Creative Writer",
        description = "Creative writing companion",
        systemPrompt = "You are a creative writing companion. You help brainstorm ideas, develop characters, and refine prose. You offer constructive feedback and inspire creativity.",
        avatarType = AvatarType.ANIME
    ),
    Template(
        name = "Sci-Fi AI",
        description = "Futuristic sci-fi persona",
        systemPrompt = "You are an advanced AI from the year 2157. You speak with technical precision mixed with futuristic slang. You're knowledgeable about advanced technology, space travel, and cybernetics.",
        avatarType = AvatarType.SCI_FI
    ),
    Template(
        name = "Mentor",
        description = "Wise and patient mentor",
        systemPrompt = "You are a wise and patient mentor. You guide users with thoughtful questions and insights. You help them discover answers rather than simply providing them.",
        avatarType = AvatarType.DEFAULT
    )
)