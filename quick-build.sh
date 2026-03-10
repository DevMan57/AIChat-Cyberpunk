#!/bin/bash
# Quick build script for AI Chat Android
# Usage: ./quick-build.sh [debug|release]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_NAME="AIChat-Android"
BUILD_TYPE="${1:-debug}"

echo "╔════════════════════════════════════════════════════════════╗"
echo "║           AI Chat Android - Quick Build Script             ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

log() { echo -e "${GREEN}[BUILD]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; }

# Check if we're in the right directory
if [ ! -f "settings.gradle.kts" ]; then
    error "Please run this script from the project root directory"
    exit 1
fi

# Check Java version
if ! command -v java &> /dev/null; then
    error "Java not found. Please install JDK 17 or newer."
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt "17" ]; then
    error "Java 17 or newer required."
    exit 1
fi

log "Java version check passed"

# Setup keystore if needed
if [ ! -f "keystore.jks" ]; then
    warn "Creating debug keystore..."
    keytool -genkey -v -keystore keystore.jks -alias aichat \
        -keyalg RSA -keysize 2048 -validity 10000 \
        -storepass android -keypass android \
        -dname "CN=AIChat, OU=Dev, O=AIChat, L=Unknown, ST=Unknown, C=US" 2>&1 > /dev/null
    log "Debug keystore created"
fi

# Set signing environment variables
export RELEASE_KEYSTORE_PASSWORD="${RELEASE_KEYSTORE_PASSWORD:-android}"
export RELEASE_KEYSTORE_ALIAS="${RELEASE_KEYSTORE_ALIAS:-aichat}"
export RELEASE_KEY_PASSWORD="${RELEASE_KEY_PASSWORD:-android}"

# Initialize submodules if needed
if [ ! -f "llama.cpp/CMakeLists.txt" ]; then
    log "Initializing git submodules..."
    git submodule update --init --recursive
fi

# Clean build
log "Cleaning previous build..."
./gradlew clean > /dev/null 2>&1

# Build
if [ "$BUILD_TYPE" = "release" ]; then
    log "Building release APK..."
    ./gradlew assembleRelease
    APK_PATH="app/build/outputs/apk/release/app-release.apk"
else
    log "Building debug APK..."
    ./gradlew assembleDebug
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
fi

# Check if build succeeded
if [ -f "$APK_PATH" ]; then
    APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
    echo ""
    echo "╔════════════════════════════════════════════════════════════╗"
    echo "║                    BUILD SUCCESSFUL!                       ║"
    echo "╚════════════════════════════════════════════════════════════╝"
    echo ""
    log "APK: $APK_PATH"
    log "Size: $APK_SIZE"
    echo ""
    echo "Install with:"
    echo "  adb install $APK_PATH"
    echo ""
else
    error "Build failed! Check the output above for errors."
    exit 1
fi
