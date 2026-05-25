#!/bin/bash
# Build IconExtractor.java into a dex for use with app_process on Android
set -e

ANDROID_SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
BUILD_TOOLS="$ANDROID_SDK/build-tools/37.0.0"
PLATFORM="$ANDROID_SDK/platforms/android-37.0"
SRC="src/main/java"
OUT="build"
DEX_NAME="adbstudio-server.dex"

echo "=== Building IconExtractor Server ==="

# Find highest available build-tools version
if [ ! -d "$BUILD_TOOLS" ]; then
    BUILD_TOOLS=$(ls -d "$ANDROID_SDK/build-tools"/*/ | sort -V | tail -1)
    echo "Using build-tools: $BUILD_TOOLS"
fi

# Find highest available platform
if [ ! -d "$PLATFORM" ]; then
    PLATFORM=$(ls -d "$ANDROID_SDK/platforms/android-"* | sort -V | tail -1)
    echo "Using platform: $PLATFORM"
fi

mkdir -p "$OUT/classes" "$OUT/dex"

echo "Compiling Java..."
javac -cp "$PLATFORM/android.jar" -d "$OUT/classes" "$SRC/com/adbstudio/IconExtractor.java"

echo "Converting to dex..."
"$BUILD_TOOLS/d8" --lib "$PLATFORM/android.jar" --output "$OUT/dex" "$OUT/classes/com/adbstudio/"*.class

echo "Copying dex to resources..."
DEST="$(dirname "$0")/../shared/src/jvmMain/resources/$DEX_NAME"
cp "$OUT/dex/classes.dex" "$DEST"
echo "Done: $DEST ($(wc -c < "$DEST") bytes)"
