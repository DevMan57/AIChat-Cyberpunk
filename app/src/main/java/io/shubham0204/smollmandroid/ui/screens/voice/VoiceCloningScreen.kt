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

package io.shubham0204.smollmandroid.ui.screens.voice

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.shubham0204.smollmandroid.voice.VoiceCloningManager
import kotlinx.coroutines.launch

/**
 * Voice Cloning Screen
 * Allows users to record reference audio and clone their voice
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceCloningScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val voiceCloningManager = remember { VoiceCloningManager(context) }
    val recordingState by voiceCloningManager.recordingState.collectAsStateWithLifecycle()
    val recordingDuration by voiceCloningManager.recordingDuration.collectAsStateWithLifecycle()
    val clonedVoiceAvailable by voiceCloningManager.clonedVoiceAvailable.collectAsStateWithLifecycle()
    
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice Cloning") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status Card
            StatusCard(
                clonedVoiceAvailable = clonedVoiceAvailable,
                onDeleteClick = { showDeleteConfirm = true }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Recording visualization
            RecordingVisualizer(
                isRecording = recordingState is VoiceCloningManager.RecordingState.Recording,
                duration = recordingDuration
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Recording button or status
            AnimatedContent(
                targetState = recordingState,
                label = "recording_state"
            ) { state ->
                when (state) {
                    is VoiceCloningManager.RecordingState.Idle -> {
                        RecordButton(
                            onClick = { voiceCloningManager.startRecording() },
                            enabled = !clonedVoiceAvailable
                        )
                    }
                    is VoiceCloningManager.RecordingState.Recording -> {
                        StopRecordButton(
                            onClick = { voiceCloningManager.stopRecording() }
                        )
                    }
                    is VoiceCloningManager.RecordingState.Processing -> {
                        ProcessingIndicator()
                    }
                    is VoiceCloningManager.RecordingState.Success -> {
                        CloneButton(
                            onClick = {
                                scope.launch {
                                    (state as? VoiceCloningManager.RecordingState.Success)?.file?.let {
                                        voiceCloningManager.cloneVoice(it)
                                    }
                                }
                            }
                        )
                    }
                    is VoiceCloningManager.RecordingState.Error -> {
                        ErrorMessage((state as VoiceCloningManager.RecordingState.Error).message)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Instructions
            Text(
                text = "Record 5-30 seconds of clear speech to clone your voice. " +
                       "Speak naturally in a quiet environment for best results.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Voice Profile?") },
            text = { Text("This will remove your cloned voice. You'll need to record again to use voice cloning.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        voiceCloningManager.deleteVoiceProfile()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StatusCard(
    clonedVoiceAvailable: Boolean,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (clonedVoiceAvailable) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (clonedVoiceAvailable) 
                        Icons.Default.CheckCircle 
                    else 
                        Icons.Default.Info,
                    contentDescription = null,
                    tint = if (clonedVoiceAvailable) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (clonedVoiceAvailable) 
                            "Voice Cloned" 
                        else 
                            "No Voice Profile",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (clonedVoiceAvailable) 
                            "Your voice is ready to use" 
                        else 
                            "Record your voice to enable cloning",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (clonedVoiceAvailable) {
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete voice",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordingVisualizer(
    isRecording: Boolean,
    duration: Long
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Box(
        modifier = Modifier
            .size(200.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                if (isRecording) 
                    MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                else 
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isRecording) Icons.Default.Mic else Icons.Default.MicNone,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = if (isRecording) 
                MaterialTheme.colorScheme.error 
            else 
                MaterialTheme.colorScheme.primary
        )
    }
    
    if (duration > 0) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = formatDuration(duration),
            style = MaterialTheme.typography.headlineMedium,
            color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun RecordButton(onClick: () -> Unit, enabled: Boolean) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(120.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Mic,
                contentDescription = "Record",
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("Record", fontSize = 14.sp)
        }
    }
}

@Composable
private fun StopRecordButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(120.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Stop,
                contentDescription = "Stop",
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("Stop", fontSize = 14.sp)
        }
    }
}

@Composable
private fun CloneButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(28.dp)
    ) {
        Icon(Icons.Default.AutoFixHigh, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Clone Voice from Recording")
    }
}

@Composable
private fun ProcessingIndicator() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            strokeWidth = 4.dp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Processing voice...",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun ErrorMessage(message: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / 1000) / 60
    return String.format("%d:%02d", minutes, seconds)
}
