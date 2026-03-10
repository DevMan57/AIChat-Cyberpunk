#!/bin/bash
# Build script for AI Chat Android with Voice Cloning
# This script builds the complete APK with all features

set -e

echo "=========================================="
echo "AI Chat Android - Build Script"
echo "=========================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODEL_URL="https://huggingface.co/mradermacher/Qwen3.5-4B-heretic-GGUF/resolve/main/Qwen3.5-4B-heretic.Q4_K_M.gguf"
MODEL_NAME="qwen-3.5-4b-heretic-q4_k_m.gguf"
MODEL_SIZE="2.3GB"

# Functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    # Check Java
    if ! command -v java &> /dev/null; then
        log_error "Java not found. Please install JDK 17 or newer."
        exit 1
    fi
    
    JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt "17" ]; then
        log_error "Java 17 or newer required. Found: $JAVA_VERSION"
        exit 1
    fi
    log_info "Java version: $(java -version 2>&1 | head -1)"
    
    # Check Android SDK
    if [ -z "$ANDROID_HOME" ] && [ -z "$ANDROID_SDK_ROOT" ]; then
        log_error "ANDROID_HOME or ANDROID_SDK_ROOT not set"
        exit 1
    fi
    log_info "Android SDK found"
    
    # Check Python (for Chaquopy)
    if ! command -v python3 &> /dev/null; then
        log_warn "python3 not found. Chaquopy may not work properly."
    else
        log_info "Python version: $(python3 --version)"
    fi
}

# Download model if not present
download_model() {
    log_info "Checking for GGUF model..."
    
    MODEL_DIR="$PROJECT_DIR/app/src/main/assets/models"
    MODEL_FILE="$MODEL_DIR/$MODEL_NAME"
    
    mkdir -p "$MODEL_DIR"
    
    if [ -f "$MODEL_FILE" ]; then
        log_info "Model already exists: $MODEL_FILE"
        FILE_SIZE=$(du -h "$MODEL_FILE" | cut -f1)
        log_info "Model size: $FILE_SIZE"
    else
        log_warn "Model not found. Downloading from HuggingFace..."
        log_warn "This will download $MODEL_SIZE. Press Ctrl+C to skip."
        
        # Download with wget
        if command -v wget &> /dev/null; then
            wget --show-progress -O "$MODEL_FILE" "$MODEL_URL"
        elif command -v curl &> /dev/null; then
            curl -L --progress-bar -o "$MODEL_FILE" "$MODEL_URL"
        else
            log_error "Neither wget nor curl found. Please install one of them."
            exit 1
        fi
        
        log_info "Model downloaded successfully"
    fi
}

# Setup signing keystore
setup_keystore() {
    log_info "Setting up signing keystore..."
    
    KEYSTORE_FILE="$PROJECT_DIR/keystore.jks"
    
    if [ -f "$KEYSTORE_FILE" ]; then
        log_info "Keystore already exists"
    else
        log_warn "Creating debug keystore..."
        keytool -genkey -v \
            -keystore "$KEYSTORE_FILE" \
            -alias aichat \
            -keyalg RSA -keysize 2048 -validity 10000 \
            -storepass android \
            -keypass android \
            -dname "CN=AIChat Android, OU=Development, O=AIChat, L=Unknown, ST=Unknown, C=US"
        log_info "Debug keystore created"
    fi
    
    # Set environment variables for signing
    export RELEASE_KEYSTORE_PASSWORD="${RELEASE_KEYSTORE_PASSWORD:-android}"
    export RELEASE_KEYSTORE_ALIAS="${RELEASE_KEYSTORE_ALIAS:-aichat}"
    export RELEASE_KEY_PASSWORD="${RELEASE_KEY_PASSWORD:-android}"
}

# Clean build
clean_build() {
    log_info "Cleaning previous builds..."
    cd "$PROJECT_DIR"
    ./gradlew clean
}

# Build debug APK
build_debug() {
    log_info "Building debug APK..."
    cd "$PROJECT_DIR"
    ./gradlew assembleDebug
    
    if [ $? -eq 0 ]; then
        log_info "Debug APK built successfully!"
        log_info "Output: $PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
    else
        log_error "Debug build failed!"
        exit 1
    fi
}

# Build release APK
build_release() {
    log_info "Building release APK..."
    cd "$PROJECT_DIR"
    ./gradlew assembleRelease
    
    if [ $? -eq 0 ]; then
        log_info "Release APK built successfully!"
        log_info "Output: $PROJECT_DIR/app/build/outputs/apk/release/app-release.apk"
        
        # Show APK size
        APK_SIZE=$(du -h "$PROJECT_DIR/app/build/outputs/apk/release/app-release.apk" | cut -f1)
        log_info "APK size: $APK_SIZE"
    else
        log_error "Release build failed!"
        exit 1
    fi
}

# Print usage
usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -d, --debug      Build debug APK (default)"
    echo "  -r, --release    Build release APK"
    echo "  -c, --clean      Clean before building"
    echo "  -m, --model      Download and bundle the GGUF model"
    echo "  -a, --all        Clean, download model, and build release"
    echo "  -h, --help       Show this help message"
    echo ""
    echo "Environment Variables:"
    echo "  RELEASE_KEYSTORE_PASSWORD    Keystore password (default: android)"
    echo "  RELEASE_KEYSTORE_ALIAS       Key alias (default: aichat)"
    echo "  RELEASE_KEY_PASSWORD         Key password (default: android)"
}

# Main function
main() {
    BUILD_TYPE="debug"
    CLEAN=false
    DOWNLOAD_MODEL=false
    
    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            -d|--debug)
                BUILD_TYPE="debug"
                shift
                ;;
            -r|--release)
                BUILD_TYPE="release"
                shift
                ;;
            -c|--clean)
                CLEAN=true
                shift
                ;;
            -m|--model)
                DOWNLOAD_MODEL=true
                shift
                ;;
            -a|--all)
                CLEAN=true
                DOWNLOAD_MODEL=true
                BUILD_TYPE="release"
                shift
                ;;
            -h|--help)
                usage
                exit 0
                ;;
            *)
                log_error "Unknown option: $1"
                usage
                exit 1
                ;;
        esac
    done
    
    # Execute build steps
    check_prerequisites
    
    if [ "$DOWNLOAD_MODEL" = true ]; then
        download_model
    fi
    
    setup_keystore
    
    if [ "$CLEAN" = true ]; then
        clean_build
    fi
    
    if [ "$BUILD_TYPE" = "debug" ]; then
        build_debug
    else
        build_release
    fi
    
    log_info "Build completed successfully!"
}

# Run main function
main "$@"
