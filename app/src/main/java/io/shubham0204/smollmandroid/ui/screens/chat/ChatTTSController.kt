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

package io.shubham0204.smollmandroid.ui.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.shubham0204.smollmandroid.tts.TTSManager
import io.shubham0204.smollmandroid.voice.VoiceCloningManager
import kotlinx.coroutines.launch

/**
 * TTS Controller for Chat Screen
 * Manages TTS playback for LLM responses
 */
@Composable
fun ChatTTSController(
    messages: List<io.shubham0204.smollmandroid.data.ChatMessage>,
    isGeneratingResponse: Boolean,
    latestResponse: String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val ttsManager = remember { TTSManager(context) }
    val voiceCloningManager = remember { VoiceCloningManager(context) }
    
    val ttsReady by ttsManager.isReady.collectAsStateWithLifecycle()
    val isSpeaking by ttsManager.isSpeaking.collectAsStateWithLifecycle()
    val voiceCloned by voiceCloningManager.clonedVoiceAvailable.collectAsStateWithLifecycle()
    
    var autoSpeakEnabled by remember { mutableStateOf(false) }
    var showTTSSettings by remember { mutableStateOf(false) }
    
    // Auto-speak latest assistant message with voice cloning support
    LaunchedEffect(messages.size, isGeneratingResponse) {
        if (autoSpeakEnabled && !isGeneratingResponse && messages.isNotEmpty()) {
            val lastMessage = messages.last()
            if (!lastMessage.isUserMessage) {
                scope.launch {
                    val voiceFile = voiceCloningManager.getVoiceProfileFile()
                    ttsManager.speak(lastMessage.message, voiceFile = voiceFile)
                }
            }
        }
    }
    
    // TTS Control Bar
    AnimatedVisibility(visible = ttsReady) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // TTS Status
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (voiceCloned) 
                            Icons.Default.RecordVoiceOver 
                        else 
                            Icons.Default.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (voiceCloned)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (voiceCloned) "Cloned Voice" else "Default TTS",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                
                // Controls
                Row {
                    // Auto-speak toggle
                    IconButton(
                        onClick = { autoSpeakEnabled = !autoSpeakEnabled },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (autoSpeakEnabled) 
                                Icons.Default.NotificationsActive 
                            else 
                                Icons.Default.NotificationsNone,
                            contentDescription = "Auto-speak",
                            tint = if (autoSpeakEnabled)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Stop speaking
                    if (isSpeaking) {
                        IconButton(
                            onClick = { ttsManager.stopAudio() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Stop,
                                contentDescription = "Stop speaking",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    
                    // Settings
                    IconButton(
                        onClick = { showTTSSettings = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "TTS Settings"
                        )
                    }
                }
            }
        }
    }
    
    // TTS Settings Dialog
    if (showTTSSettings) {
        TTSSettingsDialog(
            onDismiss = { showTTSSettings = false },
            voiceCloningManager = voiceCloningManager
        )
    }
}

@Composable
private fun TTSSettingsDialog(
    onDismiss: () -> Unit,
    voiceCloningManager: VoiceCloningManager
) {
    val context = LocalContext.current
    val voiceCloned by voiceCloningManager.clonedVoiceAvailable.collectAsStateWithLifecycle()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Voice Settings") },
        text = {
            Column {
                // Voice Status
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (voiceCloned)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (voiceCloned)
                                Icons.Default.CheckCircle
                            else
                                Icons.Default.Info,
                            contentDescription = null,
                            tint = if (voiceCloned)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (voiceCloned) "Voice Cloned" else "Using Default Voice",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = if (voiceCloned) 
                                    "Your cloned voice is active" 
                                else 
                                    "Record your voice to enable cloning",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // Manage TTS Models button
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        val intent = android.content.Intent(
                            context,
                            io.shubham0204.smollmandroid.ui.screens.manage_tts.ManageTTSActivity::class.java
                        )
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Manage TTS Models")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Instructions
                Text(
                    text = "Voice cloning requires 5-10 seconds of clear speech. " +
                           "Your voice data stays on-device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        dismissButton = if (!voiceCloned) {
            {
                TextButton(
                    onClick = {
                        // Navigate to voice cloning screen
                        onDismiss()
                        // Start voice cloning activity
                        val intent = android.content.Intent(
                            context,
                            io.shubham0204.smollmandroid.ui.screens.voice.VoiceCloningActivity::class.java
                        )
                        context.startActivity(intent)
                    }
                ) {
                    Text("Clone Voice")
                }
            }
        } else null
    )
}
