#!/bin/bash

# Build script para RCE Lab - Cyber Range
echo "🔧 Compilando payload para RCE Lab..."

# Configurar paths de Android SDK
ANDROID_SDK_PATH="$HOME/Library/Android/sdk"
ANDROID_JAR="$ANDROID_SDK_PATH/platforms/android-34/android.jar"
D8_PATH="$ANDROID_SDK_PATH/build-tools/34.0.0/d8"

# Verificar que existan las herramientas
if [ ! -f "$ANDROID_JAR" ]; then
    echo "❌ Error: android.jar no encontrado en $ANDROID_JAR"
    echo "   Asegúrate de tener Android SDK instalado"
    exit 1
fi

if [ ! -f "$D8_PATH" ]; then
    echo "❌ Error: d8 no encontrado en $D8_PATH"
    echo "   Asegúrate de tener Android Build Tools instalado"
    exit 1
fi

# Crear directorios
mkdir -p payload/build

echo "📦 Compilando Shell.java..."
# Compilar Java a class
javac -cp "$ANDROID_JAR" -d payload/build payload/src/pwn/Shell.java

if [ $? -ne 0 ]; then
    echo "❌ Error compilando Java"
    exit 1
fi

echo "🔄 Convirtiendo a DEX..."
# Convertir class a DEX
cd payload/build
"$D8_PATH" --lib "$ANDROID_JAR" --output . pwn/Shell.class
cd ../..

if [ -f "payload/build/classes.dex" ]; then
    mv payload/build/classes.dex payload/build/payload.dex
    echo "✅ DEX creado: payload/build/payload.dex"
    
    echo "📦 Creando payload.zip..."
    cd payload/build
    zip -q payload.zip payload.dex
    cd ../..
    
    echo "✅ ZIP creado: payload/build/payload.zip"
    echo ""
    echo "🌐 Para servir el payload localmente:"
    echo "   cd payload/build && python3 -m http.server 8000"
    echo ""
    echo "📱 La app verificará automáticamente cada 15 segundos:"
    echo "   URL: http://10.0.2.2:8000/payload.zip"
    echo ""
    echo "🔍 Ver logs en tiempo real:"
    echo "   adb logcat | grep -E 'RCEService|RCE_PAYLOAD'"
else
    echo "❌ Error: No se pudo crear el DEX"
    exit 1
fi 