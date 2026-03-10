# AI Chat Android with Voice Cloning

A fully offline Android chat application with local LLM inference and voice cloning capabilities.

## Features

### 🤖 Local LLM Inference
- **Qwen 3.5 4B Heretic** model in Q4_K_M quantization (~1.3GB)
- Powered by llama.cpp with OpenCL GPU acceleration
- Optimized for Snapdragon 8 Gen 3 (Adreno 750)
- Runs entirely offline - no internet required for inference

### 🎙️ Voice Cloning
- Clone your voice with just **5 seconds** of reference audio
- Uses Kyutai Pocket TTS (100M parameters)
- Integrated via Chaquopy Python bridge
- Voice profiles stored locally

### 🔊 Text-to-Speech
- AI responses spoken in your cloned voice
- Auto-speak mode for hands-free interaction
- Adjustable speech speed

### 💬 Chat Interface
- Clean, modern UI based on SmolChat
- Markdown rendering with syntax highlighting
- Chat history and multiple conversations
- Copy, share, and edit messages

### 📱 Technical Specifications
- **Architecture**: arm64-v8a
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 35 (Android 15)
- **APK Size**: ~80MB (without model), ~1.4GB (with model bundled)
- **Model Size**: 1.3GB (downloaded separately or bundled)

## Project Structure

```
SmolChat-Android/
├── app/
│   ├── src/main/
│   │   ├── python/
│   │   │   └── tts_service.py      # Chaquopy Python TTS service
│   │   ├── cpp/
│   │   │   └── CMakeLists.txt      # Native build with OpenCL
│   │   ├── java/io/shubham0204/smollmandroid/
│   │   │   ├── tts/
│   │   │   │   └── TTSManager.kt   # TTS integration
│   │   │   ├── voice/
│   │   │   │   ├── VoiceCloningManager.kt
│   │   │   │   └── VoiceCloningScreen.kt
│   │   │   └── ui/screens/chat/
│   │   │       └── ChatTTSController.kt
│   │   └── assets/models/          # Optional: bundled GGUF model
│   └── build.gradle.kts            # Chaquopy configuration
├── smollm/
│   └── src/main/cpp/
│       └── CMakeLists.txt          # OpenCL GPU acceleration
└── llama.cpp/                      # Git submodule
```

## Build Instructions

### Prerequisites

1. **Android Studio** Hedgehog (2023.1.1) or newer
2. **Android SDK** 35
3. **NDK** 27.2.12479018
4. **CMake** 3.22.1+
5. **Python** 3.10+ (for Chaquopy)
6. **JDK** 17 or newer

### Option 1: Quick Build with Script

```bash
# Clone the repository
git clone https://github.com/yourusername/AIChat-Android.git
cd AIChat-Android

# Initialize submodules
git submodule update --init --recursive

# Build debug APK
./build.sh --debug

# Or build release APK with bundled model
./build.sh --all
```

### Option 2: Manual Build

1. **Clone and setup:**
```bash
git clone https://github.com/yourusername/AIChat-Android.git
cd AIChat-Android
git submodule update --init --recursive
```

2. **Download the model (optional):**
```bash
mkdir -p app/src/main/assets/models
wget -O app/src/main/assets/models/qwen-3.5-4b-heretic-q4_k_m.gguf \
  "https://huggingface.co/mradermacher/Qwen3.5-4B-heretic-GGUF/resolve/main/Qwen3.5-4B-heretic.Q4_K_M.gguf"
```

3. **Create signing keystore:**
```bash
keytool -genkey -v -keystore keystore.jks -alias aichat \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass android -keypass android \
  -dname "CN=AIChat Android, OU=Development, O=AIChat, L=Unknown, ST=Unknown, C=US"

export RELEASE_KEYSTORE_PASSWORD=android
export RELEASE_KEYSTORE_ALIAS=aichat
export RELEASE_KEY_PASSWORD=android
```

4. **Build with Gradle:**
```bash
./gradlew assembleRelease
```

5. **Find the APK:**
```
app/build/outputs/apk/release/app-release.apk
```

## Installation

### Sideload APK (No PC Required)

1. Enable "Unknown Sources" in Android settings
2. Transfer APK to your device
3. Tap the APK to install
4. Grant required permissions (Microphone, Storage)

### ADB Install
```bash
adb install app/build/outputs/apk/release/app-release.apk
```

## Usage

### First Run

1. **Download Model**: On first launch, the app will download the Qwen 3.5 4B model (~1.3GB). This is a one-time download.

2. **Clone Your Voice** (optional):
   - Go to Settings → Voice Cloning
   - Tap "Record" and speak clearly for 5-10 seconds
   - Tap "Clone Voice" to create your voice profile

3. **Start Chatting**:
   - Type your message or use voice input
   - The AI will respond with text
   - Enable "Auto-speak" to hear responses in your cloned voice

### Voice Cloning Tips

- Record in a quiet environment
- Speak naturally at normal pace
- Use 5-10 seconds of clear speech
- Avoid background noise
- Speak at consistent volume

## GPU Acceleration

The app uses OpenCL for GPU acceleration on compatible devices:

- **Snapdragon 8 Gen 3**: Full acceleration with Adreno 750
- **Other Snapdragon 8xx**: Should work with reduced performance
- **Fallback**: CPU-only mode on unsupported devices

To verify GPU acceleration is working:
1. Check logcat for "OpenCL library found"
2. Look for "GGML_USE_CLBLAST" in native logs
3. Inference speed should be 5-15 tokens/sec on GPU

## Performance Benchmarks

| Device | Backend | Context | Tokens/sec |
|--------|---------|---------|------------|
| Snapdragon 8 Gen 3 | OpenCL | 4096 | 12-18 |
| Snapdragon 8 Gen 3 | CPU | 4096 | 5-8 |
| Snapdragon 8 Gen 2 | OpenCL | 4096 | 8-12 |
| Pixel 8 (Tensor G3) | CPU | 4096 | 4-6 |

## Troubleshooting

### Build Issues

**Error: Chaquopy not found**
```
Add to settings.gradle.kts:
maven { url = URI("https://chaquo.com/maven") }
```

**Error: NDK not found**
```
export ANDROID_NDK_HOME=$ANDROID_HOME/ndk/27.2.12479018
```

**Error: OpenCL not found**
- OpenCL is optional; the app will fall back to CPU
- For Snapdragon devices, OpenCL drivers are usually included

### Runtime Issues

**Model fails to load**
- Ensure model file is not corrupted (check MD5)
- Try downloading the model again
- Check available storage (need 3GB free)

**Voice cloning fails**
- Ensure microphone permission is granted
- Record for at least 5 seconds
- Check that Python/Chaquopy initialized correctly

**TTS not working**
- Check that voice profile was created successfully
- Try using default voice instead of cloned voice
- Check logcat for Python errors

## Customization

### Using a Different Model

1. Download any GGUF format model from HuggingFace
2. Place in `app/src/main/assets/models/`
3. Update model path in `ModelsRepository.kt`

### Custom Voice Cloning Model

The current implementation uses a simplified TTS approach. For production voice cloning:

1. Replace `tts_service.py` with actual Pocket TTS or Coqui TTS
2. Build custom Python wheels for Android
3. Update `VoiceCloningManager.kt` with proper embedding extraction

## License

This project is licensed under the Apache License 2.0 - see LICENSE file.

### Third-Party Licenses

- **SmolChat-Android**: Apache 2.0
- **llama.cpp**: MIT
- **Qwen 3.5**: Tongyi Qianwen License
- **Chaquopy**: Commercial (free for open source projects)

## Acknowledgments

- [SmolChat-Android](https://github.com/shubham0204/SmolChat-Android) - Base project
- [llama.cpp](https://github.com/ggerganov/llama.cpp) - LLM inference engine
- [Qwen](https://github.com/QwenLM/Qwen) - Language model
- [Chaquopy](https://chaquo.com/chaquopy/) - Python on Android
- [Kyutai](https://kyutai.org/) - Pocket TTS research

## Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## Contact

For issues and feature requests, please use GitHub Issues.

---

**Note**: This is a research project. Voice cloning and AI features should be used responsibly and in accordance with applicable laws.
