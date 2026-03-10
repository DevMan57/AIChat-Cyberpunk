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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.shubham0204.smollmandroid.llm.RemoteLLMManager
import io.shubham0204.smollmandroid.theme.CyberpunkColors
import io.shubham0204.smollmandroid.theme.neonBorder
import io.shubham0204.smollmandroid.ui.theme.AppThemeMode
import io.shubham0204.smollmandroid.ui.theme.ThemeManager
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Settings Screen
 * Theme toggle and app settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    themeManager: ThemeManager = koinInject(),
    remoteLLMManager: RemoteLLMManager = koinInject()
) {
    val currentThemeMode by themeManager.currentThemeMode.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Settings",
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
            // Theme Section
            SettingsSectionTitle("Theme")
            
            ThemeSelector(
                currentTheme = currentThemeMode,
                onThemeSelected = { themeManager.setThemeMode(it) }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Appearance Preview
            SettingsSectionTitle("Appearance Preview")
            
            ThemePreview(
                isCyberpunk = currentThemeMode == AppThemeMode.CYBERPUNK
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            // Remote LLM Section (LM Studio Link)
            SettingsSectionTitle("Remote LLM (LM Studio Link)")

            RemoteLLMSettings(remoteLLMManager = remoteLLMManager)

            Spacer(modifier = Modifier.height(8.dp))

            // Info Section
            SettingsSectionTitle("About")
            
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = CyberpunkColors.backgroundCard
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .neonBorder(CyberpunkColors.borderCyan.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "AI Chat Android",
                        color = CyberpunkColors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Version 1.0.0",
                        color = CyberpunkColors.textSecondary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "A local AI chat application with character system, voice cloning, and cyberpunk aesthetics.",
                        color = CyberpunkColors.textMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        title,
        color = CyberpunkColors.lightsaberGreen,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun ThemeSelector(
    currentTheme: AppThemeMode,
    onThemeSelected: (AppThemeMode) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = CyberpunkColors.backgroundCard
        ),
        modifier = Modifier
            .fillMaxWidth()
            .neonBorder(CyberpunkColors.borderCyan.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            ThemeOption(
                title = "Cyberpunk",
                subtitle = "Neon dark theme with cyan, magenta, and purple accents",
                icon = "🌃",
                isSelected = currentTheme == AppThemeMode.CYBERPUNK,
                accentColor = CyberpunkColors.lightsaberGreen,
                onClick = { onThemeSelected(AppThemeMode.CYBERPUNK) }
            )
            
            ThemeOption(
                title = "Dark",
                subtitle = "Classic dark theme",
                icon = "🌙",
                isSelected = currentTheme == AppThemeMode.DARK,
                accentColor = CyberpunkColors.textSecondary,
                onClick = { onThemeSelected(AppThemeMode.DARK) }
            )
            
            ThemeOption(
                title = "Light",
                subtitle = "Clean light theme",
                icon = "☀️",
                isSelected = currentTheme == AppThemeMode.LIGHT,
                accentColor = CyberpunkColors.textSecondary,
                onClick = { onThemeSelected(AppThemeMode.LIGHT) }
            )
            
            ThemeOption(
                title = "System",
                subtitle = "Follow system settings",
                icon = "⚙️",
                isSelected = currentTheme == AppThemeMode.SYSTEM,
                accentColor = CyberpunkColors.textSecondary,
                onClick = { onThemeSelected(AppThemeMode.SYSTEM) }
            )
        }
    }
}

@Composable
private fun ThemeOption(
    title: String,
    subtitle: String,
    icon: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    if (isSelected) accentColor.copy(alpha = 0.2f)
                    else CyberpunkColors.backgroundSecondary,
                    CircleShape
                )
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) accentColor else CyberpunkColors.borderCyan.copy(alpha = 0.3f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 24.sp)
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Text
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = if (isSelected) accentColor else CyberpunkColors.textPrimary,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
            Text(
                subtitle,
                color = CyberpunkColors.textSecondary,
                fontSize = 12.sp
            )
        }
        
        // Checkmark
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Selected",
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun ThemePreview(
    isCyberpunk: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isCyberpunk) CyberpunkColors.backgroundSecondary 
            else Color(0xFF1A1A1A)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .neonBorder(
                if (isCyberpunk) CyberpunkColors.lightsaberGreen.copy(alpha = 0.5f)
                else Color.Gray.copy(alpha = 0.3f)
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Preview Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isCyberpunk) {
                    // Cyberpunk colors
                    ColorPreviewDot(CyberpunkColors.lightsaberGreen)
                    ColorPreviewDot(CyberpunkColors.lightsaberPurple)
                    ColorPreviewDot(CyberpunkColors.lightsaberPurple)
                    ColorPreviewDot(CyberpunkColors.lightsaberGreen)
                } else {
                    // Dark theme colors
                    ColorPreviewDot(Color(0xFF5B8CFF))
                    ColorPreviewDot(Color(0xFF4CAF50))
                    ColorPreviewDot(Color(0xFFFF9800))
                    ColorPreviewDot(Color(0xFFE91E63))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Preview Text
            Text(
                "Sample Chat Message",
                color = if (isCyberpunk) CyberpunkColors.textPrimary else Color.White,
                fontSize = 16.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .background(
                        if (isCyberpunk) CyberpunkColors.lightsaberGreen.copy(alpha = 0.15f)
                        else Color(0xFF5B8CFF).copy(alpha = 0.2f),
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        1.dp,
                        if (isCyberpunk) CyberpunkColors.lightsaberGreen.copy(alpha = 0.5f)
                        else Color(0xFF5B8CFF).copy(alpha = 0.5f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp)
            ) {
                Text(
                    "This is how chat bubbles look",
                    color = if (isCyberpunk) CyberpunkColors.textPrimary else Color.White,
                    fontSize = 14.sp
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .align(Alignment.End)
                    .background(
                        if (isCyberpunk) CyberpunkColors.lightsaberPurple.copy(alpha = 0.1f)
                        else Color(0xFF4CAF50).copy(alpha = 0.2f),
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        1.dp,
                        if (isCyberpunk) CyberpunkColors.lightsaberPurple.copy(alpha = 0.4f)
                        else Color(0xFF4CAF50).copy(alpha = 0.5f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp)
            ) {
                Text(
                    "AI response bubble style",
                    color = if (isCyberpunk) CyberpunkColors.textPrimary else Color.White,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun ColorPreviewDot(color: Color) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(color, CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
    )
}

@Composable
private fun RemoteLLMSettings(remoteLLMManager: RemoteLLMManager) {
    val scope = rememberCoroutineScope()
    var isEnabled by remember { mutableStateOf(remoteLLMManager.isEnabled.get()) }
    var serverUrl by remember { mutableStateOf(remoteLLMManager.getServerUrl()) }
    var modelName by remember { mutableStateOf(remoteLLMManager.getModelName()) }
    var apiKey by remember { mutableStateOf(remoteLLMManager.getApiKey()) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = CyberpunkColors.textPrimary,
        unfocusedTextColor = CyberpunkColors.textPrimary,
        focusedBorderColor = CyberpunkColors.lightsaberGreen,
        unfocusedBorderColor = CyberpunkColors.borderCyan.copy(alpha = 0.5f),
        focusedLabelColor = CyberpunkColors.lightsaberGreen,
        unfocusedLabelColor = CyberpunkColors.textSecondary,
        cursorColor = CyberpunkColors.lightsaberGreen,
    )

    Card(
        colors = CardDefaults.cardColors(
            containerColor = CyberpunkColors.backgroundCard
        ),
        modifier = Modifier
            .fillMaxWidth()
            .neonBorder(CyberpunkColors.borderCyan.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Enable toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Enable Remote Inference",
                        color = CyberpunkColors.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Connect to LM Studio or any OpenAI-compatible server",
                        color = CyberpunkColors.textSecondary,
                        fontSize = 12.sp
                    )
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { enabled ->
                        isEnabled = enabled
                        remoteLLMManager.setEnabled(enabled)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberpunkColors.lightsaberGreen,
                        checkedTrackColor = CyberpunkColors.lightsaberGreen.copy(alpha = 0.3f),
                        uncheckedThumbColor = CyberpunkColors.textMuted,
                        uncheckedTrackColor = CyberpunkColors.backgroundSecondary,
                    )
                )
            }

            // Server URL
            OutlinedTextField(
                value = serverUrl,
                onValueChange = {
                    serverUrl = it
                    remoteLLMManager.setServerUrl(it)
                },
                label = { Text("Server URL") },
                placeholder = { Text("http://192.168.1.100:1234", color = CyberpunkColors.textMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors,
                enabled = isEnabled
            )

            // Model name
            OutlinedTextField(
                value = modelName,
                onValueChange = {
                    modelName = it
                    remoteLLMManager.setModelName(it)
                },
                label = { Text("Model Name") },
                placeholder = { Text("Leave blank to auto-detect", color = CyberpunkColors.textMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors,
                enabled = isEnabled
            )

            // API Key
            OutlinedTextField(
                value = apiKey,
                onValueChange = {
                    apiKey = it
                    remoteLLMManager.setApiKey(it)
                },
                label = { Text("API Key (optional)") },
                placeholder = { Text("Bearer token", color = CyberpunkColors.textMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors,
                visualTransformation = if (apiKey.isNotEmpty()) PasswordVisualTransformation() else VisualTransformation.None,
                enabled = isEnabled
            )

            // Test connection button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        isTesting = true
                        testResult = null
                        scope.launch {
                            val result = remoteLLMManager.testConnection()
                            result.onSuccess { models ->
                                testResult = if (models.isEmpty()) {
                                    "Connected (no models listed)"
                                } else {
                                    "Connected - Models: ${models.joinToString(", ")}"
                                }
                                // Auto-fill model name if blank
                                if (modelName.isBlank() && models.isNotEmpty()) {
                                    modelName = models.first()
                                    remoteLLMManager.setModelName(modelName)
                                }
                            }
                            result.onFailure { e ->
                                testResult = "Failed: ${e.message}"
                            }
                            isTesting = false
                        }
                    },
                    enabled = isEnabled && !isTesting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberpunkColors.lightsaberGreen.copy(alpha = 0.2f),
                        contentColor = CyberpunkColors.lightsaberGreen,
                        disabledContainerColor = CyberpunkColors.backgroundSecondary,
                        disabledContentColor = CyberpunkColors.textMuted,
                    ),
                    modifier = Modifier.neonBorder(
                        if (isEnabled) CyberpunkColors.lightsaberGreen.copy(alpha = 0.5f)
                        else Color.Transparent
                    )
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = CyberpunkColors.lightsaberGreen,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Test Connection")
                }
            }

            // Test result
            if (testResult != null) {
                val isSuccess = testResult!!.startsWith("Connected")
                Text(
                    testResult!!,
                    color = if (isSuccess) CyberpunkColors.lightsaberGreen else Color(0xFFFF4444),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSuccess) CyberpunkColors.lightsaberGreen.copy(alpha = 0.1f)
                            else Color(0xFFFF4444).copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                )
            }
        }
    }
}