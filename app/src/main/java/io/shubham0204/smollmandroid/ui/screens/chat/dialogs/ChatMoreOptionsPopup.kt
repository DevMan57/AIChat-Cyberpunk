/*
 * Copyright (C) 2024 Shubham Panchal
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

package io.shubham0204.smollmandroid.ui.screens.chat.dialogs

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Cpu
import compose.icons.feathericons.Delete
import compose.icons.feathericons.Folder
import compose.icons.feathericons.Layout
import compose.icons.feathericons.Mic
import compose.icons.feathericons.Package
import compose.icons.feathericons.Settings
import compose.icons.feathericons.User
import compose.icons.feathericons.XCircle
import compose.icons.feathericons.Zap
import io.shubham0204.smollmandroid.R
import io.shubham0204.smollmandroid.data.Chat
import io.shubham0204.smollmandroid.theme.CyberpunkColors
import io.shubham0204.smollmandroid.ui.preview.dummyChats
import io.shubham0204.smollmandroid.ui.screens.chat.ChatScreenUIEvent

@Preview
@Composable
private fun PreviewChatMoreOptionsPopup() {
    ChatMoreOptionsPopup(
        chat = dummyChats[0],
        isExpanded = true,
        showRAMUsageLabel = true,
        onEditChatSettingsClick = {},
        onBenchmarkModelClick = {},
        onVoiceManagementClick = {},
        onSettingsClick = {},
        onEvent = {},
    )
}

@Composable
fun ChatMoreOptionsPopup(
    chat: Chat,
    isExpanded: Boolean,
    showRAMUsageLabel: Boolean,
    onEditChatSettingsClick: () -> Unit,
    onBenchmarkModelClick: () -> Unit,
    onVoiceManagementClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onEvent: (ChatScreenUIEvent) -> Unit,
) {
    DropdownMenu(
        expanded = isExpanded,
        onDismissRequest = {
            onEvent(ChatScreenUIEvent.DialogEvents.ToggleMoreOptionsPopup(visible = false))
        },
        containerColor = CyberpunkColors.backgroundSecondary,
    ) {
        val scope =
            object : EventScope {
                override fun onEvent(event: ChatScreenUIEvent) {
                    onEvent(event)
                }
            }
        with(scope) {
            // Character Management
            PopupMenuItem(
                icon = FeatherIcons.User,
                text = "Character Management",
                iconTint = CyberpunkColors.lightsaberPurple
            ) {
                // Character selector is accessed from the top bar
                onEvent(ChatScreenUIEvent.DialogEvents.ToggleMoreOptionsPopup(visible = false))
            }
            
            // Voice Management
            PopupMenuItem(
                icon = FeatherIcons.Mic,
                text = "Voice Library",
                iconTint = CyberpunkColors.lightsaberPurple,
                onClick = onVoiceManagementClick,
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = CyberpunkColors.borderCyan.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            PopupMenuItem(
                icon = FeatherIcons.Settings,
                text = stringResource(R.string.chat_options_edit_settings),
                iconTint = CyberpunkColors.lightsaberGreen
            ) {
                onEditChatSettingsClick()
            }
            PopupMenuItem(
                icon = FeatherIcons.Folder,
                text = stringResource(R.string.chat_options_change_folder),
                iconTint = CyberpunkColors.textSecondary
            ) {
                onEvent(ChatScreenUIEvent.DialogEvents.ToggleChangeFolderDialog(visible = true))
            }
            PopupMenuItem(
                icon = FeatherIcons.Package,
                text = stringResource(R.string.chat_options_change_model),
                iconTint = CyberpunkColors.textSecondary
            ) {
                onEvent(ChatScreenUIEvent.DialogEvents.ToggleSelectModelListDialog(visible = true))
            }
            PopupMenuItem(
                icon = FeatherIcons.Zap,
                text = stringResource(R.string.chat_options_benchmark_model),
                iconTint = CyberpunkColors.textSecondary,
                onClick = onBenchmarkModelClick,
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = CyberpunkColors.borderCyan.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            PopupMenuItem(
                icon = FeatherIcons.Delete,
                text = stringResource(R.string.dialog_title_delete_chat),
                isDestructive = true,
            ) {
                onEvent(ChatScreenUIEvent.ChatEvents.OnDeleteChat(chat))
            }
            PopupMenuItem(
                icon = FeatherIcons.XCircle,
                text = stringResource(R.string.chat_options_clear_messages),
                isDestructive = true,
            ) {
                onEvent(ChatScreenUIEvent.ChatEvents.OnDeleteChatMessages(chat))
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = CyberpunkColors.borderCyan.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            PopupMenuItem(
                icon = FeatherIcons.Layout,
                text = stringResource(R.string.chat_options_ctx_length_usage),
                iconTint = CyberpunkColors.textSecondary
            ) {
                onEvent(ChatScreenUIEvent.DialogEvents.ShowContextLengthUsageDialog(chat))
            }
            PopupMenuItem(
                icon = FeatherIcons.Cpu,
                text = stringResource(
                    if (showRAMUsageLabel) R.string.chat_options_hide_ram
                    else R.string.chat_options_show_ram
                ),
                iconTint = CyberpunkColors.textSecondary
            ) {
                onEvent(ChatScreenUIEvent.DialogEvents.ToggleRAMUsageLabel)
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = CyberpunkColors.borderCyan.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            // Settings
            PopupMenuItem(
                icon = FeatherIcons.Settings,
                text = "Settings",
                iconTint = CyberpunkColors.lightsaberGreen,
                onClick = onSettingsClick,
            )
        }
    }
}

private interface EventScope {
    fun onEvent(event: ChatScreenUIEvent)
}

@Composable
private fun EventScope.PopupMenuItem(
    icon: ImageVector,
    text: String,
    iconTint: Color = CyberpunkColors.textSecondary,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val tint = if (isDestructive) CyberpunkColors.error else iconTint
    val textColor = if (isDestructive) CyberpunkColors.error else CyberpunkColors.textPrimary
    
    DropdownMenuItem(
        leadingIcon = {
            Icon(
                icon,
                contentDescription = text,
                tint = tint
            )
        },
        text = {
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                color = textColor
            )
        },
        onClick = {
            onClick()
            onEvent(ChatScreenUIEvent.DialogEvents.ToggleMoreOptionsPopup(visible = false))
        },
    )
}