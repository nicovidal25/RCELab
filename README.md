# RCE Lab - Cyber Range

Laboratorio de Ejecución de Código Remoto (RCE) para entrenamiento en ciberseguridad.

## 🎯 Descripción

Este proyecto replica la vulnerabilidad de RCE descrita en el [artículo de NowSecure](https://www.nowsecure.com/blog/2017/06/15/a-pattern-for-remote-code-execution-using-arbitrary-file-writes-and-multidex-applications/) sobre patrones de ejecución remota usando escritura arbitraria de archivos y aplicaciones MultiDex.

**Diseñado para Cyber Range**: La aplicación ejecuta verificaciones automáticas cada 15 segundos sin intervención del usuario.

## ⚡ Características Principales

### Comportamiento Automático

- **Servicio de Background**: Se ejecuta automáticamente al iniciar la app
- **Verificaciones Periódicas**: Cada 15 segundos busca payloads nuevos
- **Persistencia**: Se reinicia automáticamente al arrancar el dispositivo
- **Sin Intervención del Usuario**: Perfecto para escenarios de cyber range

### Vulnerabilidades Simuladas

- **Descarga No Validada**: Descarga ZIPs sin verificar origen
- **Carga Dinámica de DEX**: Ejecuta código sin validación
- **Escritura Arbitraria**: Extrae archivos sin restricciones
- **MultiDex**: Aprovecha la funcionalidad de múltiples DEX

## 🏗️ Arquitectura

```
RCE Lab/
├── app/src/main/java/com/app/lab/rce/
│   ├── MainActivity.kt          # Actividad principal con UI
│   ├── MainScreen.kt           # Interfaz de usuario (Compose)
│   ├── AdUpdateService.kt      # Servicio automático de verificación
│   ├── BootReceiver.kt         # Persistencia al arranque
│   ├── CompromiseReceiver.kt   # Manejo de notificaciones
│   └── VulnerableReceiver.kt   # Receiver vulnerable (legacy)
├── payload/src/pwn/
│   └── Shell.java              # Payload malicioso
└── build_payload.sh            # Script para compilar payload
```

## 🚀 Configuración Rápida

### 1. Compilar Payload

```bash
./build_payload.sh
```

### 2. Servir Payload

```bash
cd payload/build && python3 -m http.server 8000
```

### 3. Instalar App

```bash
./gradlew installDebug
```

## 📱 Flujo Automático

1. **Inicio**: La app se inicia automáticamente
2. **Servicio**: Se activa el servicio de background
3. **Verificación**: Cada 15 segundos verifica `http://10.0.2.2:8000/payload.zip`
4. **Descarga**: Si encuentra el payload, lo descarga automáticamente
5. **Extracción**: Extrae el DEX del ZIP
6. **Ejecución**: Carga y ejecuta el código malicioso
7. **Compromiso**: Notifica que el sistema fue comprometido

## 🔍 Monitoring

### Logs en Tiempo Real

```bash
adb logcat | grep -E 'RCEService|RCE_PAYLOAD|MainActivity'
```

### Eventos Clave

- `📡 Verificando actualizaciones...` - Verificación automática
- `📦 Payload descargado` - Descarga exitosa
- `💀 RCE EJECUTADA` - Compromiso exitoso
- `🚨 SISTEMA COMPROMETIDO` - Notificación final

## 🎓 Uso en Cyber Range

### Escenarios de Entrenamiento

1. **Análisis de Malware**: Observar comportamiento automático
2. **Respuesta a Incidentes**: Detectar y responder al compromiso
3. **Análisis Forense**: Investigar logs y artefactos
4. **Mitigación**: Implementar contramedidas

### Configuración de Red

- **Emulador Android**: `10.0.2.2:8000`
- **Dispositivo Real**: Ajustar IP en `AdUpdateService.kt`

## ⚠️ Advertencias

- **Solo para Entrenamiento**: No usar en producción
- **Entorno Controlado**: Solo ejecutar en cyber ranges
- **Monitoreo Requerido**: Supervisar toda la actividad

## 🛡️ Contramedidas

### Prevención

- Validación de firmas digitales
- Verificación de checksums
- Certificate pinning
- Restricciones de red

### Detección

- Monitoreo de tráfico HTTP
- Análisis de logs del sistema
- Detección de carga dinámica de DEX
- Behavioral analysis

## 📊 Métricas de Compromiso

La app registra métricas detalladas:

- Tiempo hasta compromiso
- Fuente del payload
- Método de ejecución
- Persistencia establecida

## 🔧 Personalización

### Cambiar Intervalo de Verificación

```kotlin
// En AdUpdateService.kt
private const val UPDATE_INTERVAL_MS = 15000L // 15 segundos
```

### Cambiar URL del Payload

```kotlin
// En AdUpdateService.kt
private const val PAYLOAD_URL = "http://10.0.2.2:8000/payload.zip"
```

## 📚 Referencias

- [NowSecure Article](https://www.nowsecure.com/blog/2017/06/15/a-pattern-for-remote-code-execution-using-arbitrary-file-writes-and-multidex-applications/)
- [Android MultiDex](https://developer.android.com/studio/build/multidex)
- [DexClassLoader](https://developer.android.com/reference/dalvik/system/DexClassLoader)
