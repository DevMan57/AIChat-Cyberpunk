package io.shubham0204.smollmandroid.ui.screens.manage_tts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.Check
import compose.icons.feathericons.Download
import compose.icons.feathericons.Trash2
import io.shubham0204.smollmandroid.ui.components.AppBarTitleText
import io.shubham0204.smollmandroid.ui.theme.SmolLMAndroidTheme
import org.koin.androidx.compose.koinViewModel

class ManageTTSActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: ManageTTSViewModel = koinViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            ScreenUI(uiState, viewModel::onEvent)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ScreenUI(uiState: ManageTTSUIState, onEvent: (ManageTTSUIEvent) -> Unit) {
        SmolLMAndroidTheme {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = { AppBarTitleText("Manage Text-to-Speech") },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(FeatherIcons.ArrowLeft, contentDescription = "Back")
                            }
                        },
                    )
                },
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(16.dp)
                        .fillMaxSize()
                ) {
                    // Status Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (uiState.isDownloaded)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (uiState.isDownloaded) {
                                Icon(
                                    FeatherIcons.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp),
                                )
                            } else {
                                Icon(
                                    FeatherIcons.Download,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    if (uiState.isDownloaded) "PocketTTS Ready" else "PocketTTS Not Downloaded",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    if (uiState.isDownloaded)
                                        "Voice cloning TTS engine is ready to use"
                                    else
                                        "Download ~${POCKET_TTS_TOTAL_SIZE_MB} MB of models for on-device TTS with voice cloning",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Model files list
                    Text(
                        "Model Files",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        items(POCKET_TTS_MODEL_FILES) { modelFile ->
                            ModelFileItem(modelFile)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action buttons
                    if (!uiState.isDownloaded) {
                        Button(
                            onClick = { onEvent(ManageTTSUIEvent.DownloadModels) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = uiState.downloadProgress == null,
                        ) {
                            Icon(FeatherIcons.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download All Models (~${POCKET_TTS_TOTAL_SIZE_MB} MB)")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onEvent(ManageTTSUIEvent.DeleteModels) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                        ) {
                            Icon(FeatherIcons.Trash2, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete TTS Models")
                        }
                    }

                    // Error message
                    uiState.errorMessage?.let { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    // Download progress dialog
                    if (uiState.downloadProgress != null) {
                        DownloadProgressDialog(uiState)
                    }
                }
            }
        }
    }

    @Composable
    private fun ModelFileItem(modelFile: TTSModelFile) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        modelFile.fileName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        modelFile.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                    )
                }
            }
        }
    }

    @Composable
    private fun DownloadProgressDialog(state: ManageTTSUIState) {
        Dialog(onDismissRequest = {}) {
            Card(shape = MaterialTheme.shapes.medium) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Downloading TTS Models...", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "File ${state.currentFileIndex + 1}/${state.totalFiles}: ${state.currentFileName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { (state.downloadProgress ?: 0) / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${state.downloadProgress}%", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
