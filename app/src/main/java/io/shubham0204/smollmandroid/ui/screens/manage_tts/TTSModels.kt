package io.shubham0204.smollmandroid.ui.screens.manage_tts

/**
 * PocketTTS ONNX model files hosted on HuggingFace.
 * Uses INT8 quantized variants for speed and size.
 */
data class TTSModelFile(
    val fileName: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val description: String,
)

val POCKET_TTS_MODEL_FILES = listOf(
    TTSModelFile(
        fileName = "text_conditioner.onnx",
        downloadUrl = "https://huggingface.co/KevinAHM/pocket-tts-onnx/resolve/main/onnx/text_conditioner.onnx",
        sizeBytes = 16_000_000L,
        description = "Text embeddings (16 MB)"
    ),
    TTSModelFile(
        fileName = "flow_lm_main_int8.onnx",
        downloadUrl = "https://huggingface.co/KevinAHM/pocket-tts-onnx/resolve/main/onnx/flow_lm_main_int8.onnx",
        sizeBytes = 76_000_000L,
        description = "Transformer backbone INT8 (76 MB)"
    ),
    TTSModelFile(
        fileName = "flow_lm_flow_int8.onnx",
        downloadUrl = "https://huggingface.co/KevinAHM/pocket-tts-onnx/resolve/main/onnx/flow_lm_flow_int8.onnx",
        sizeBytes = 10_000_000L,
        description = "Flow solver INT8 (10 MB)"
    ),
    TTSModelFile(
        fileName = "mimi_decoder_int8.onnx",
        downloadUrl = "https://huggingface.co/KevinAHM/pocket-tts-onnx/resolve/main/onnx/mimi_decoder_int8.onnx",
        sizeBytes = 23_000_000L,
        description = "Audio decoder INT8 (23 MB)"
    ),
    TTSModelFile(
        fileName = "mimi_encoder.onnx",
        downloadUrl = "https://huggingface.co/KevinAHM/pocket-tts-onnx/resolve/main/onnx/mimi_encoder.onnx",
        sizeBytes = 73_000_000L,
        description = "Voice encoder (73 MB)"
    ),
    TTSModelFile(
        fileName = "tokenizer.model",
        downloadUrl = "https://huggingface.co/KevinAHM/pocket-tts-onnx/resolve/main/tokenizer.model",
        sizeBytes = 500_000L,
        description = "SentencePiece tokenizer (<1 MB)"
    ),
)

const val POCKET_TTS_TOTAL_SIZE_MB = 198
const val POCKET_TTS_MODEL_DIR = "tts_models/pocket_tts"
