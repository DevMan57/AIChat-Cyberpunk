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

import android.media.MediaRecorder
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.shubham0204.smollmandroid.theme.CyberpunkColors
import io.shubham0204.smollmandroid.theme.VoiceWaveColors
import io.shubham0204.smollmandroid.theme.neonBorder
import io.shubham0204.smollmandroid.theme.neonShadow
import io.shubham0204.smollmandroid.voice.Voice
import io.shubham0204.smollmandroid.voice.VoiceManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.io.File

/**
 * Voice Management Screen
 * Manage multiple voice slots, record and test voices
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceManagementScreen(
    onNavigateBack: () -> Unit,
    voiceManager: VoiceManager = koinInject()
) {
    val scope = rememberCoroutineScope()
    val voices by voiceManager.voices.collectAsStateWithLifecycle()
    val currentVoice by voiceManager.currentVoice.collectAsStateWithLifecycle()
    
    var showRecordDialog by remember { mutableStateOf(false) }
    var voiceToRename by remember { mutableStateOf<Voice?>(null) }
    var voiceToDelete by remember { mutableStateOf<Voice?>(null) }
    var voiceToReplace by remember { mutableStateOf<Voice?>(null) }
    
    val availableSlots = voiceManager.getAvailableSlots()
    val maxSlots = voiceManager.getMaxSlots()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Voice Library",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CyberpunkColors.backgroundPrimary
                )
            )
        },
        floatingActionButton = {
            if (voices.size < maxSlots) {
                FloatingActionButton(
                    onClick = { showRecordDialog = true },
                    containerColor = CyberpunkColors.lightsaberGreen,
                    contentColor = CyberpunkColors.backgroundPrimary,
                    modifier = Modifier.neonShadow(CyberpunkColors.lightsaberGreen)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Voice")
                }
            }
        },
        containerColor = CyberpunkColors.backgroundPrimary
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Slots indicator
            VoiceSlotsIndicator(
                usedSlots = voices.size,
                maxSlots = maxSlots
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (voices.isEmpty()) {
                // Empty state
                EmptyVoiceState(
                    onAddClick = { showRecordDialog = true }
                )
            } else {
                // Voice list
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = voices,
                        key = { it.id }
                    ) { voice ->
                        VoiceCard(
                            voice = voice,
                            isCurrent = voice.id == currentVoice?.id,
                            onSetCurrent = { voiceManager.setCurrentVoice(voice.id) },
                            onPlay = { voiceManager.playVoice(voice.id) },
                            onStop = { voiceManager.stopPlayback() },
                            onRename = { voiceToRename = voice },
                            onReplace = { voiceToReplace = voice },
                            onDelete = { voiceToDelete = voice }
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
    
    // Record dialog
    if (showRecordDialog) {
        VoiceRecordDialog(
            onDismiss = { showRecordDialog = false },
            onSave = { name, file ->
                voiceManager.addVoice(name, file)
                showRecordDialog = false
            }
        )
    }
    
    // Rename dialog
    if (voiceToRename != null) {
        RenameVoiceDialog(
            currentName = voiceToRename!!.name,
            onDismiss = { voiceToRename = null },
            onConfirm = { newName ->
                voiceManager.renameVoice(voiceToRename!!.id, newName)
                voiceToRename = null
            }
        )
    }
    
    // Replace dialog
    if (voiceToReplace != null) {
        VoiceRecordDialog(
            title = "Replace Voice: ${voiceToReplace!!.name}",
            onDismiss = { voiceToReplace = null },
            onSave = { _, file ->
                voiceManager.replaceVoice(voiceToReplace!!.id, file)
                voiceToReplace = null
            }
        )
    }
    
    // Delete dialog
    if (voiceToDelete != null) {
        AlertDialog(
            onDismissRequest = { voiceToDelete = null },
            containerColor = CyberpunkColors.backgroundSecondary,
            title = { 
                Text(
                    "Delete Voice?",
                    color = CyberpunkColors.textPrimary
                )
            },
            text = { 
                Text(
                    "Are you sure you want to delete '${voiceToDelete?.name}'?",
                    color = CyberpunkColors.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        voiceManager.deleteVoice(voiceToDelete!!.id)
                        voiceToDelete = null
                    }
                ) {
                    Text("Delete", color = CyberpunkColors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { voiceToDelete = null }) {
                    Text("Cancel", color = CyberpunkColors.lightsaberGreen)
                }
            }
        )
    }
}

@Composable
private fun VoiceSlotsIndicator(
    usedSlots: Int,
    maxSlots: Int
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = CyberpunkColors.backgroundCard
        ),
        modifier = Modifier
            .fillMaxWidth()
            .neonBorder(CyberpunkColors.lightsaberPurple.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Voice Slots",
                    color = CyberpunkColors.textPrimary,
                    fontSize = 16.sp
                )
                Text(
                    "$usedSlots of $maxSlots used",
                    color = CyberpunkColors.textSecondary,
                    fontSize = 12.sp
                )
            }
            
            // Slot indicators
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(maxSlots) { index ->
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                if (index < usedSlots) CyberpunkColors.lightsaberGreen
                                else CyberpunkColors.backgroundElevated,
                                CircleShape
                            )
                            .border(
                                1.dp,
                                if (index < usedSlots) CyberpunkColors.lightsaberGreen
                                else CyberpunkColors.borderCyan.copy(alpha = 0.3f),
                                CircleShape
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyVoiceState(
    onAddClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        CyberpunkColors.lightsaberGreen.copy(alpha = 0.1f),
                        CircleShape
                    )
                    .border(
                        2.dp,
                        CyberpunkColors.borderCyan,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = null,
                    tint = CyberpunkColors.lightsaberGreen,
                    modifier = Modifier.size(48.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                "No Voices Yet",
                color = CyberpunkColors.textPrimary,
                fontSize = 20.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "Record your voice to enable voice cloning\nfor your characters",
                color = CyberpunkColors.textSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberpunkColors.lightsaberGreen,
                    contentColor = CyberpunkColors.backgroundPrimary
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Record Voice")
            }
        }
    }
}

@Composable
private fun VoiceCard(
    voice: Voice,
    isCurrent: Boolean,
    onSetCurrent: () -> Unit,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    onRename: () -> Unit,
    onReplace: () -> Unit,
    onDelete: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(false) }
    
    val borderColor = if (isCurrent) CyberpunkColors.lightsaberGreen else CyberpunkColors.borderCyan.copy(alpha = 0.3f)
    val backgroundColor = if (isCurrent) CyberpunkColors.lightsaberGreen.copy(alpha = 0.1f) else CyberpunkColors.backgroundCard
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        modifier = Modifier
            .fillMaxWidth()
            .neonBorder(borderColor, glowRadius = if (isCurrent) 8.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Voice icon with wave animation when playing
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (isPlaying) CyberpunkColors.lightsaberGreen.copy(alpha = 0.3f)
                                else CyberpunkColors.backgroundSecondary,
                                CircleShape
                            )
                            .border(
                                2.dp,
                                if (isPlaying) CyberpunkColors.lightsaberGreen else CyberpunkColors.borderCyan,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isPlaying) {
                            VoiceWaveAnimation()
                        } else {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = null,
                                tint = CyberpunkColors.lightsaberGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            voice.name,
                            color = CyberpunkColors.textPrimary,
                            fontSize = 16.sp
                        )
                        Text(
                            "Created ${formatDate(voice.createdAt)}",
                            color = CyberpunkColors.textMuted,
                            fontSize = 12.sp
                        )
                    }
                }
                
                // Current indicator
                if (isCurrent) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = CyberpunkColors.lightsaberGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Active",
                            color = CyberpunkColors.lightsaberGreen,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Play/Stop button
                Button(
                    onClick = {
                        if (isPlaying) {
                            onStop()
                            isPlaying = false
                        } else {
                            onPlay()
                            isPlaying = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlaying) CyberpunkColors.error else CyberpunkColors.lightsaberGreen,
                        contentColor = CyberpunkColors.backgroundPrimary
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isPlaying) "Stop" else "Preview")
                }
                
                if (!isCurrent) {
                    Button(
                        onClick = onSetCurrent,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberpunkColors.lightsaberPurple.copy(alpha = 0.3f),
                            contentColor = CyberpunkColors.lightsaberPurple
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Use")
                    }
                }
                
                IconButton(onClick = onRename) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Rename",
                        tint = CyberpunkColors.lightsaberGreen
                    )
                }
                
                IconButton(onClick = onReplace) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Replace",
                        tint = CyberpunkColors.textSecondary
                    )
                }
                
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = CyberpunkColors.error
                    )
                }
            }
        }
    }
}

@Composable
private fun VoiceWaveAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        repeat(4) { index ->
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500 + index * 100),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "wave_$index"
            )
            
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((16 * scale).dp)
                    .background(CyberpunkColors.lightsaberGreen, RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
private fun VoiceRecordDialog(
    title: String = "Record New Voice",
    onDismiss: () -> Unit,
    onSave: (String, File) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var voiceName by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableStateOf(0L) }
    var recordedFile by remember { mutableStateOf<File?>(null) }
    
    // Simple recording simulation - in real implementation would use VoiceCloningManager
    DisposableEffect(isRecording) {
        if (isRecording) {
            val startTime = System.currentTimeMillis()
            val job = scope.launch {
                while (isRecording) {
                    recordingDuration = System.currentTimeMillis() - startTime
                    delay(100)
                }
            }
            onDispose { job.cancel() }
        } else {
            onDispose { }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberpunkColors.backgroundSecondary,
        title = { 
            Text(title, color = CyberpunkColors.textPrimary)
        },
        text = {
            Column {
                if (recordedFile == null) {
                    // Recording UI
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .background(
                                if (isRecording) CyberpunkColors.error.copy(alpha = 0.1f)
                                else CyberpunkColors.backgroundCard,
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                2.dp,
                                if (isRecording) CyberpunkColors.error else CyberpunkColors.lightsaberGreen,
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (isRecording) {
                                // Recording animation
                                val scale by rememberInfiniteTransition(label = "pulse").animateFloat(
                                    initialValue = 1f,
                                    targetValue = 1.2f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(800),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "pulse"
                                )
                                
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .scale(scale)
                                        .background(
                                            CyberpunkColors.error.copy(alpha = 0.2f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = CyberpunkColors.error,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    formatDuration(recordingDuration),
                                    color = CyberpunkColors.error,
                                    fontSize = 24.sp
                                )
                                Text(
                                    "Recording...",
                                    color = CyberpunkColors.textSecondary,
                                    fontSize = 14.sp
                                )
                            } else {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = CyberpunkColors.lightsaberGreen,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Tap record to start",
                                    color = CyberpunkColors.textSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Record button
                    Button(
                        onClick = {
                            if (isRecording) {
                                // Stop recording
                                isRecording = false
                                // In real implementation, save the recorded file
                                recordedFile = File(context.cacheDir, "temp_recording.wav")
                            } else {
                                // Start recording
                                isRecording = true
                                recordingDuration = 0
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording) CyberpunkColors.error else CyberpunkColors.lightsaberGreen,
                            contentColor = CyberpunkColors.backgroundPrimary
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Icon(
                            if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isRecording) "Stop Recording" else "Start Recording")
                    }
                    
                    if (!isRecording) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Record 5-30 seconds of clear speech for best results",
                            color = CyberpunkColors.textMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    // Save UI
                    Text(
                        "Recording complete!",
                        color = CyberpunkColors.success,
                        fontSize = 16.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = voiceName,
                        onValueChange = { voiceName = it },
                        label = { Text("Voice Name", color = CyberpunkColors.textSecondary) },
                        placeholder = { Text("e.g., My Voice", color = CyberpunkColors.textMuted) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberpunkColors.lightsaberGreen,
                            unfocusedBorderColor = CyberpunkColors.borderCyan,
                            focusedTextColor = CyberpunkColors.textPrimary,
                            unfocusedTextColor = CyberpunkColors.textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        "Duration: ${formatDuration(recordingDuration)}",
                        color = CyberpunkColors.textSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            if (recordedFile != null) {
                TextButton(
                    onClick = { onSave(voiceName, recordedFile!!) },
                    enabled = voiceName.isNotBlank()
                ) {
                    Text("Save", color = if (voiceName.isNotBlank()) CyberpunkColors.lightsaberGreen else CyberpunkColors.textMuted)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = CyberpunkColors.textSecondary)
            }
        }
    )
}

@Composable
private fun RenameVoiceDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberpunkColors.backgroundSecondary,
        title = { 
            Text("Rename Voice", color = CyberpunkColors.textPrimary)
        },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Voice Name", color = CyberpunkColors.textSecondary) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberpunkColors.lightsaberGreen,
                    unfocusedBorderColor = CyberpunkColors.borderCyan,
                    focusedTextColor = CyberpunkColors.textPrimary,
                    unfocusedTextColor = CyberpunkColors.textPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(newName) },
                enabled = newName.isNotBlank() && newName != currentName
            ) {
                Text(
                    "Rename",
                    color = if (newName.isNotBlank() && newName != currentName)
                        CyberpunkColors.lightsaberGreen else CyberpunkColors.textMuted
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = CyberpunkColors.textSecondary)
            }
        }
    )
}

private fun formatDuration(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / 1000) / 60
    return String.format("%d:%02d", minutes, seconds)
}

private fun formatDate(timestamp: Long): String {
    val days = (System.currentTimeMillis() - timestamp) / (1000 * 60 * 60 * 24)
    return when {
        days == 0L -> "Today"
        days == 1L -> "Yesterday"
        days < 7 -> "$days days ago"
        days < 30 -> "${days / 7} weeks ago"
        else -> "${days / 30} months ago"
    }
}