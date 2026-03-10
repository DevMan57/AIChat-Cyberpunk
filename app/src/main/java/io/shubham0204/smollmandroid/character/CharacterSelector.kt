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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.shubham0204.smollmandroid.theme.CyberpunkColors
import io.shubham0204.smollmandroid.theme.neonBorder
import io.shubham0204.smollmandroid.theme.neonShadow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Character Selector Screen
 * Grid of character cards for selection and management
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterSelectorScreen(
    onNavigateBack: () -> Unit,
    onCreateCharacter: () -> Unit,
    onEditCharacter: (Character) -> Unit,
    onCharacterSelected: ((Character) -> Unit)? = null,
    characterManager: CharacterManager = koinInject()
) {
    val scope = rememberCoroutineScope()
    val characters by characterManager.characters.collectAsStateWithLifecycle()
    val currentCharacter by characterManager.currentCharacter.collectAsStateWithLifecycle()
    
    var selectedTab by remember { mutableStateOf(0) }
    var characterToDelete by remember { mutableStateOf<Character?>(null) }
    
    val tabs = listOf("All", "Presets", "Custom")
    
    val displayedCharacters = when (selectedTab) {
        0 -> Character.getPresetCharacters() + characters
        1 -> Character.getPresetCharacters()
        2 -> characters
        else -> Character.getPresetCharacters() + characters
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Select Character",
                        color = CyberpunkColors.textPrimary
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Close", color = CyberpunkColors.lightsaberGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CyberpunkColors.backgroundPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateCharacter,
                containerColor = CyberpunkColors.lightsaberGreen,
                contentColor = CyberpunkColors.backgroundPrimary,
                modifier = Modifier.neonShadow(CyberpunkColors.lightsaberGreen)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Character")
            }
        },
        containerColor = CyberpunkColors.backgroundPrimary
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = CyberpunkColors.backgroundSecondary,
                contentColor = CyberpunkColors.lightsaberGreen,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = CyberpunkColors.lightsaberGreen
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                color = if (selectedTab == index) 
                                    CyberpunkColors.lightsaberGreen 
                                else 
                                    CyberpunkColors.textMuted
                            )
                        }
                    )
                }
            }
            
            // Current Character Card
            if (currentCharacter != null) {
                CurrentCharacterCard(
                    character = currentCharacter,
                    modifier = Modifier.padding(16.dp)
                )
            }
            
            // Character Grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = displayedCharacters,
                    key = { it.id }
                ) { character ->
                    CharacterCard(
                        character = character,
                        isSelected = character.id == currentCharacter.id,
                        isCurrent = character.id == currentCharacter.id,
                        onClick = {
                            scope.launch {
                                characterManager.setCurrentCharacter(character)
                                onCharacterSelected?.invoke(character)
                            }
                        },
                        onEditClick = {
                            if (!character.isPreset) {
                                onEditCharacter(character)
                            }
                        },
                        onDuplicateClick = {
                            scope.launch {
                                characterManager.duplicateCharacter(character)
                            }
                        },
                        onDeleteClick = {
                            if (!character.isPreset) {
                                characterToDelete = character
                            }
                        }
                    )
                }
                
                // Add spacer at bottom for FAB
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
    
    // Delete confirmation dialog
    if (characterToDelete != null) {
        AlertDialog(
            onDismissRequest = { characterToDelete = null },
            containerColor = CyberpunkColors.backgroundSecondary,
            title = { 
                Text(
                    "Delete Character?",
                    color = CyberpunkColors.textPrimary
                )
            },
            text = { 
                Text(
                    "Are you sure you want to delete '${characterToDelete?.name}'?",
                    color = CyberpunkColors.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            characterToDelete?.let { 
                                characterManager.deleteCharacter(it) 
                            }
                            characterToDelete = null
                        }
                    }
                ) {
                    Text("Delete", color = CyberpunkColors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { characterToDelete = null }) {
                    Text("Cancel", color = CyberpunkColors.lightsaberGreen)
                }
            }
        )
    }
}

@Composable
private fun CurrentCharacterCard(
    character: Character,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .neonShadow(CyberpunkColors.lightsaberPurple),
        colors = CardDefaults.cardColors(
            containerColor = CyberpunkColors.backgroundCard
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .neonBorder(CyberpunkColors.lightsaberPurple, glowRadius = 4.dp)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        CyberpunkColors.lightsaberPurple.copy(alpha = 0.2f),
                        CircleShape
                    )
                    .border(
                        2.dp,
                        CyberpunkColors.lightsaberPurple,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    character.avatarType.getIcon(),
                    fontSize = 32.sp
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Current Character",
                    color = CyberpunkColors.lightsaberPurple,
                    fontSize = 12.sp
                )
                Text(
                    character.name,
                    color = CyberpunkColors.textPrimary,
                    fontSize = 20.sp,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    character.description,
                    color = CyberpunkColors.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Active",
                tint = CyberpunkColors.lightsaberPurple,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CharacterCard(
    character: Character,
    isSelected: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDuplicateClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        label = "scale"
    )
    
    val borderColor = when {
        isCurrent -> CyberpunkColors.lightsaberPurple
        isSelected -> CyberpunkColors.lightsaberGreen
        character.isPreset -> CyberpunkColors.lightsaberPurple.copy(alpha = 0.6f)
        else -> CyberpunkColors.borderCyan.copy(alpha = 0.3f)
    }
    
    val backgroundColor = when {
        isCurrent -> CyberpunkColors.lightsaberPurple.copy(alpha = 0.15f)
        isSelected -> CyberpunkColors.lightsaberGreen.copy(alpha = 0.1f)
        else -> CyberpunkColors.backgroundCard
    }
    
    Card(
        modifier = Modifier
            .scale(scale)
            .clickable(onClick = onClick)
            .neonBorder(
                borderColor,
                glowRadius = if (isCurrent || isSelected) 8.dp else 2.dp
            ),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar with preset badge
            Box(
                contentAlignment = Alignment.TopEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            CyberpunkColors.backgroundSecondary,
                            CircleShape
                        )
                        .border(
                            2.dp,
                            borderColor,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        character.avatarType.getIcon(),
                        fontSize = 32.sp
                    )
                }
                
                if (character.isPreset) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(
                                CyberpunkColors.lightsaberPurple,
                                CircleShape
                            )
                            .border(1.dp, CyberpunkColors.backgroundPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "★",
                            fontSize = 10.sp,
                            color = CyberpunkColors.backgroundPrimary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Name
            Text(
                character.name,
                color = CyberpunkColors.textPrimary,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // Description
            Text(
                character.description,
                color = CyberpunkColors.textSecondary,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.height(28.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!character.isPreset) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = CyberpunkColors.lightsaberGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = onDuplicateClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Duplicate",
                            tint = CyberpunkColors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = CyberpunkColors.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact character selector for use in chat screen
 */
@Composable
fun CharacterSelectorCompact(
    onClick: () -> Unit,
    characterManager: CharacterManager = koinInject()
) {
    val currentCharacter by characterManager.currentCharacter.collectAsStateWithLifecycle()
    
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(
                CyberpunkColors.backgroundCard,
                RoundedCornerShape(20.dp)
            )
            .border(
                1.dp,
                CyberpunkColors.borderCyan,
                RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            currentCharacter.avatarType.getIcon(),
            fontSize = 20.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            currentCharacter.name,
            color = CyberpunkColors.textPrimary,
            fontSize = 14.sp,
            maxLines = 1
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            "▼",
            color = CyberpunkColors.lightsaberGreen,
            fontSize = 10.sp
        )
    }
}