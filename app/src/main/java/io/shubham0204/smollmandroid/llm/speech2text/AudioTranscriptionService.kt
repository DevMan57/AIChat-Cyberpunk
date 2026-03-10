package io.shubham0204.smollmandroid.llm.speech2text

import ai.moonshine.voice.JNI
import ai.moonshine.voice.TranscriptEvent
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import io.shubham0204.smollmandroid.ui.screens.manage_asr.ASRModel
import io.shubham0204.smollmandroid.ui.screens.manage_asr.ASRModelArch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.io.File

enum class SttEngine {
    /** Android's built-in SpeechRecognizer (zero-download, works on Samsung S24 Ultra) */
    NATIVE,
    /** Moonshine ASR (requires model download) */
    MOONSHINE
}

@Single
class AudioTranscriptionService(private val context: Context) {

    private val micTranscriber: MyTranscriber = MyTranscriber()
    private val nativeStt: NativeSpeechRecognitionService = NativeSpeechRecognitionService(context)
    private val logTag = "[AudioTranscriptionService]"
    private var model: ASRModel? = null
    private var activeEngine: SttEngine? = null

    /** Check if native (Android built-in) STT is available on this device */
    fun isNativeSttAvailable(): Boolean = nativeStt.isAvailable()

    /** Check if on-device (offline) native STT is available (API 31+, Samsung devices) */
    fun isNativeOnDeviceSttAvailable(): Boolean = nativeStt.isOnDeviceAvailable()

    /**
     * Start transcription using native Android SpeechRecognizer.
     * Zero-download, works immediately on devices with on-device recognition (e.g. Samsung S24 Ultra).
     */
    fun startNativeTranscription(
        onResult: (String) -> Unit,
        onPartialResult: ((String) -> Unit)? = null,
        onError: ((Int) -> Unit)? = null
    ): Error? {
        if (!checkIfAudioRecordingPermissionGranted()) {
            Log.e(logTag, "Permission to record audio was not granted.")
            return Error.AudioRecordingPermissionNotGranted(
                "Permission to record audio was not granted."
            )
        }
        if (!nativeStt.isAvailable()) {
            Log.e(logTag, "Native speech recognition not available on this device.")
            return Error.NativeSttNotAvailable(
                "Native speech recognition not available on this device."
            )
        }
        activeEngine = SttEngine.NATIVE
        Log.d(logTag, "Starting native transcription...")
        nativeStt.startListening(
            onResult = { text ->
                Log.d(logTag, "Native STT result: $text")
                CoroutineScope(Dispatchers.Main).launch {
                    onResult(text)
                }
            },
            onPartialResult = onPartialResult?.let { callback ->
                { text ->
                    CoroutineScope(Dispatchers.Main).launch {
                        callback(text)
                    }
                }
            },
            onError = onError
        )
        return null
    }

    /**
     * Start transcription using Moonshine ASR (requires downloaded model).
     */
    fun startTranscription(asrModel: ASRModel, onLineComplete: (String) -> Unit): Error? {
        if (model?.name != asrModel.name) {
            Log.d(logTag, "Loading model: $asrModel")
            model = asrModel
            val modelDir = File(context.filesDir, asrModel.name)
            micTranscriber.loadFromFiles(
                modelDir.absolutePath,
                when (asrModel.arch) {
                    ASRModelArch.TINY -> JNI.MOONSHINE_MODEL_ARCH_TINY
                    ASRModelArch.BASE -> JNI.MOONSHINE_MODEL_ARCH_BASE
                },
            )
        }
        if (!checkIfAudioRecordingPermissionGranted()) {
            Log.e(logTag, "Permission to record audio was not granted.")
            return Error.AudioRecordingPermissionNotGranted(
                "Permission to record audio was not granted."
            )
        }
        activeEngine = SttEngine.MOONSHINE
        micTranscriber.addListener { event ->
            if (event is TranscriptEvent.LineCompleted) {
                Log.d(logTag, "ASR Line completed: ${event.line.text}")
                CoroutineScope(Dispatchers.Main).launch {
                    onLineComplete(event.line.text)
                }
            }
        }
        Log.d(logTag, "Starting transcription...")
        micTranscriber.onMicPermissionGranted()
        micTranscriber.start()
        return null
    }

    fun stopTranscription() {
        Log.d(logTag, "Stopping transcription...")
        when (activeEngine) {
            SttEngine.NATIVE -> {
                nativeStt.stopListening()
            }
            SttEngine.MOONSHINE -> {
                try {
                    micTranscriber.stop()
                } catch (e: UnsatisfiedLinkError) {
                    Log.d(logTag, "Attempted to stop transcription without starting it.")
                }
            }
            null -> {
                // Try stopping both just in case
                try { micTranscriber.stop() } catch (_: Exception) {}
                nativeStt.stopListening()
            }
        }
        activeEngine = null
    }

    fun destroy() {
        stopTranscription()
        nativeStt.destroy()
    }

    private fun checkIfAudioRecordingPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
    }

    sealed interface Error {
        data class AudioRecordingPermissionNotGranted(val message: String) : Error
        data class NativeSttNotAvailable(val message: String) : Error
    }
}
