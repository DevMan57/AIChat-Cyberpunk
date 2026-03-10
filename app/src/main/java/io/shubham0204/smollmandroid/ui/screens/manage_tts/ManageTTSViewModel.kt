package io.shubham0204.smollmandroid.ui.screens.manage_tts

import android.content.Context
import androidx.lifecycle.ViewModel
import io.shubham0204.smollmandroid.ui.screens.manage_asr.DownloadService
import io.shubham0204.smollmandroid.ui.screens.manage_asr.ToastNotifService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.koin.android.annotation.KoinViewModel
import java.io.File

data class ManageTTSUIState(
    val isDownloaded: Boolean = false,
    val downloadProgress: Int? = null,
    val currentFileIndex: Int = 0,
    val totalFiles: Int = POCKET_TTS_MODEL_FILES.size,
    val currentFileName: String = "",
    val errorMessage: String? = null,
)

sealed interface ManageTTSUIEvent {
    data object DownloadModels : ManageTTSUIEvent
    data object DeleteModels : ManageTTSUIEvent
}

@KoinViewModel
class ManageTTSViewModel(
    private val context: Context,
    private val downloadService: DownloadService,
    private val toastNotifService: ToastNotifService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(initState())
    val uiState: StateFlow<ManageTTSUIState> = _uiState

    fun onEvent(event: ManageTTSUIEvent) {
        when (event) {
            is ManageTTSUIEvent.DownloadModels -> startDownloadAll()
            is ManageTTSUIEvent.DeleteModels -> deleteModels()
        }
    }

    private fun initState(): ManageTTSUIState {
        return ManageTTSUIState(isDownloaded = areModelsDownloaded())
    }

    private fun areModelsDownloaded(): Boolean {
        val modelDir = File(context.filesDir, POCKET_TTS_MODEL_DIR)
        return POCKET_TTS_MODEL_FILES.all { File(modelDir, it.fileName).exists() }
    }

    private fun startDownloadAll() {
        val modelDir = File(context.filesDir, POCKET_TTS_MODEL_DIR)
        modelDir.mkdirs()

        _uiState.update { it.copy(downloadProgress = 0, currentFileIndex = 0, errorMessage = null) }
        downloadNextFile(0, modelDir)
    }

    private fun downloadNextFile(index: Int, modelDir: File) {
        if (index >= POCKET_TTS_MODEL_FILES.size) {
            _uiState.update { it.copy(downloadProgress = null, isDownloaded = true) }
            toastNotifService.showShortToast("TTS models downloaded successfully.")
            return
        }

        val fileInfo = POCKET_TTS_MODEL_FILES[index]

        // Skip already downloaded files
        if (File(modelDir, fileInfo.fileName).exists()) {
            downloadNextFile(index + 1, modelDir)
            return
        }

        _uiState.update {
            it.copy(
                currentFileIndex = index,
                currentFileName = fileInfo.fileName,
                downloadProgress = 0,
            )
        }

        downloadService.startDownload(
            url = fileInfo.downloadUrl,
            destDir = modelDir.absolutePath,
            destFileName = fileInfo.fileName,
            onStart = {
                toastNotifService.showShortToast("Downloading ${fileInfo.fileName}...")
            },
            onProgress = { progress ->
                // Calculate overall progress across all files
                val fileWeight = 100.0 / POCKET_TTS_MODEL_FILES.size
                val overallProgress = (index * fileWeight + progress * fileWeight / 100).toInt()
                _uiState.update { it.copy(downloadProgress = overallProgress) }
            },
            onSuccess = {
                downloadNextFile(index + 1, modelDir)
            },
            onFailure = { failureMessage ->
                _uiState.update {
                    it.copy(
                        downloadProgress = null,
                        errorMessage = "Failed: ${fileInfo.fileName} - $failureMessage"
                    )
                }
                toastNotifService.showLongToast("Download failed: $failureMessage")
            },
        )
    }

    private fun deleteModels() {
        val modelDir = File(context.filesDir, POCKET_TTS_MODEL_DIR)
        if (modelDir.exists()) {
            modelDir.deleteRecursively()
        }
        _uiState.update { it.copy(isDownloaded = false) }
        toastNotifService.showShortToast("TTS models deleted.")
    }
}
