# 🚨 RCE Lab - Guía de Ejecución del Ataque

## 📖 **Descripción**

Esta guía documenta cómo ejecutar un ataque de **Remote Code Execution (RCE)** que replica la vulnerabilidad descrita en el artículo de NowSecure sobre aplicaciones Android MultiDex. El ataque utiliza técnicas de **Man-in-the-Middle (MITM)** para inyectar payloads maliciosos en tráfico HTTP.

## 🎯 **Objetivo**

Demostrar cómo un atacante puede comprometer una aplicación Android mediante:

- Interceptación de tráfico HTTP no cifrado
- Inyección de archivos DEX maliciosos
- Ejecución automática de código remoto
- Persistencia en el sistema

---

## 🛠️ **Requisitos Previos**

### **Hardware:**

- Mac/Linux con Python 3.6+
- Dispositivo Android (físico o emulador)
- Conectividad WiFi/Red compartida

### **Software:**

- Android Studio + SDK
- mitmproxy (`pip install mitmproxy`)
- ADB (Android Debug Bridge)

---

## 📱 **FASE 1: Preparación del Entorno**

### **1.1 Clonar el Repositorio**

```bash
git clone <repository-url>
cd RCELab
```

### **1.2 Compilar el Payload**

```bash
# Compilar el payload malicioso
./build_payload.sh
```

### **1.3 Verificar Archivos Críticos**

```bash
ls -la payload/build/payload.zip    # Payload malicioso (debe existir)
ls -la mitmproxy/inject_payload.py  # Script de interceptación
```

---

## 🌐 **FASE 2: Configuración de mitmproxy**

### **2.1 Obtener IP del Atacante**

```bash
# En Mac/Linux
ifconfig | grep inet

# Ejemplo de salida:
# inet 192.168.0.189 netmask 0xffffff00 broadcast 192.168.0.255
```

### **2.2 Iniciar mitmproxy**

```bash
# Desde el directorio raíz del proyecto
mitmproxy -s mitmproxy/inject_payload.py --listen-port 8080
```

**Resultado esperado:**

```
Loading script mitmproxy/inject_payload.py
🎯 PayloadInjector inicializado
📦 Payload path: /path/to/payload/build/payload.zip
Proxy server listening at http://*:8080
```

---

## 📱 **FASE 3: Configuración del Dispositivo Android**

### **3.1 Conectar Dispositivo**

```bash
# Verificar conexión ADB
adb devices

# Resultado esperado:
# List of devices attached
# emulator-5554   device
```

### **3.2 Configurar Proxy Automáticamente**

```bash
# Configurar proxy HTTP (reemplazar con tu IP)
adb shell settings put global http_proxy 192.168.0.189:8080

# Verificar configuración
adb shell settings get global http_proxy
# Resultado esperado: 192.168.0.189:8080
```

### **3.3 Instalar la Aplicación**

```bash
# Compilar e instalar
./gradlew installDebug

# Resultado esperado:
# BUILD SUCCESSFUL
# Installing APK 'app-debug.apk' on 'device'
# Installed on 1 device
```

---

## 🚀 **FASE 4: Ejecución del Ataque**

### **4.1 Iniciar la Aplicación**

```bash
# Iniciar la app automáticamente
adb shell am start -n com.app.lab.rce/.MainActivity
```

### **4.2 Observar mitmproxy**

En la interfaz de mitmproxy deberías ver:

```
GET http://updates.gameapp.com/assets/update.zip
→ 200 application/zip 2.7k
GET http://api.adnetwork.com/content/content.zip
→ [Errno 8] nodename nor servname provided
```

### **4.3 Monitorear Logs del Dispositivo**

```bash
# Limpiar logs y monitorear en tiempo real
adb logcat -c

# Monitorear el ataque en vivo
adb logcat | grep -i "RCEService\|CompromiseReceiver"

# Alternativamente, para ver solo logs críticos:
adb logcat | grep -E "COMPROMISO|RCE EJECUTADA|COMPROMETIDO"
```

---

## ✅ **FASE 5: Verificación del Compromiso**

### **5.1 Logs de Ejecución Exitosa**

```bash
# Comando para monitorear el ataque
adb logcat | grep -i "RCEService\|CompromiseReceiver"

# Logs esperados (secuencia de ataque exitoso):
I RCEService: 🎯 Servicio automático iniciado
I RCEService: 📡 Verificaciones automáticas iniciadas (cada 15s)
I RCEService: 📡 Solicitando: http://updates.gameapp.com/assets/update.zip
I RCEService: 📥 Descarga exitosa: content_1234567890.zip
I RCEService: 📂 Extraído: payload.dex
W RCEService: ⚠️ Contenido ejecutable detectado: payload.dex
W RCEService: 🚨 EJECUTANDO PAYLOAD DINÁMICO
E RCE_PAYLOAD: 💀 SISTEMA COMPLETAMENTE COMPROMETIDO 💀
E RCEService: 💀 RCE EJECUTADA - Clase: pwn.Shell
E RCEService: 🔴 SISTEMA COMPROMETIDO
E CompromiseReceiver: 🚨 COMPROMISO DETECTADO - Fuente: BackgroundService - Hora: XX:XX:XX
W CompromiseReceiver: 💀 Iniciando actividades post-explotación...
W CompromiseReceiver: 📤 Exfiltrando datos sensibles
W CompromiseReceiver: 📡 Estableciendo comunicación C&C
W CompromiseReceiver: 🔒 Estableciendo persistencia
E CompromiseReceiver: ===== ANÁLISIS FORENSE =====
E CompromiseReceiver: Fuente: BackgroundService
E CompromiseReceiver: Timestamp: 1234567890
E CompromiseReceiver: PID: XXXX
E CompromiseReceiver: UID: XXXXX
E CompromiseReceiver: ============================
```

### **5.2 Verificar Interfaz de Usuario**

- Abrir la aplicación RCE Lab
- Estado debe mostrar: **"☠️ SISTEMA COMPROMETIDO"** (fondo rojo)
- Fuente: **"BackgroundService"**
- Logs mostrando actividad maliciosa

### **5.3 Verificar Archivos Comprometidos**

```bash
# Verificar payloads descargados
adb shell run-as com.app.lab.rce ls -la files/
# Debe mostrar: ad_bundle.zip y múltiples directorios temp_*

# Verificar archivos DEX extraídos
adb shell run-as com.app.lab.rce "find files/ -name '*.dex'"
# Debe mostrar múltiples archivos payload.dex
```

---

## 🔄 **FASE 6: Comportamiento Continuo**

### **6.1 Ataque Automático**

- El servicio verifica actualizaciones **cada 15 segundos**
- Cada verificación puede resultar en re-compromiso
- El sistema permanece comprometido persistentemente

### **6.2 Actividades Post-Explotación Simuladas**

```bash
W CompromiseReceiver: 💀 Iniciando actividades post-explotación...
W CompromiseReceiver: 📤 Simulando exfiltración de datos sensibles
W CompromiseReceiver: 📡 Estableciendo comunicación con servidor C&C
W CompromiseReceiver: 🔒 Estableciendo mecanismos de persistencia
```

---

## 🧹 **FASE 7: Limpieza Post-Ataque**

### **7.1 Detener mitmproxy**

```bash
# En la interfaz de mitmproxy presionar 'q' para salir
```

### **7.2 Desactivar Proxy**

```bash
# Remover configuración de proxy
adb shell settings put global http_proxy :0

# Verificar
adb shell settings get global http_proxy
# Resultado esperado: vacío o ":0"
```

### **7.3 Resetear Aplicación (Opcional)**

```bash
# Desinstalar aplicación
adb uninstall com.app.lab.rce

# O limpiar datos
adb shell pm clear com.app.lab.rce
```

---

## 🔍 **Indicadores de Compromiso (IoCs)**

### **Archivos:**

- `/data/data/com.app.lab.rce/files/ad_bundle.zip`
- `/data/data/com.app.lab.rce/files/temp_*/payload.dex`

### **Tráfico de Red:**

- Requests HTTP a dominios de actualización cada 15s
- Downloads de archivos ZIP (2.7k bytes)

### **Procesos:**

- Servicio `AdUpdateService` ejecutándose permanentemente
- Carga dinámica de clases DEX

### **Logs:**

- `RCE_PAYLOAD: SISTEMA COMPLETAMENTE COMPROMETIDO`
- `CompromiseReceiver: COMPROMISO DETECTADO`

---

## ⚠️ **Consideraciones de Seguridad**

### **Para Uso Legítimo:**

- ✅ Solo usar en entornos controlados (cyber ranges)
- ✅ Dispositivos de prueba dedicados
- ✅ Fines educativos y de investigación

### **Riesgos:**

- ❌ NO usar en dispositivos de producción
- ❌ NO usar en redes corporativas sin autorización
- ❌ El código puede ser detectado por antivirus

---

## 🎓 **Objetivos de Aprendizaje**

Al completar este laboratorio, los estudiantes habrán aprendido:

1. **Técnicas MITM:** Interceptación de tráfico HTTP
2. **Inyección de Payloads:** Modificación de responses en tiempo real
3. **Android Security:** Vulnerabilidades en aplicaciones MultiDex
4. **Evasión de Protecciones:** Bypass de SecurityException en Android 14+
5. **Persistencia:** Servicios automáticos y broadcast receivers
6. **Análisis Forense:** Identificación de IoCs y comportamiento malicioso

---

## 📞 **Troubleshooting**

### **Problema: mitmproxy no intercepta**

```bash
# Verificar configuración de proxy
adb shell settings get global http_proxy

# Reiniciar conexión de red
adb shell svc wifi disable && sleep 2 && adb shell svc wifi enable
```

### **Problema: RCE no se ejecuta**

```bash
# Verificar permisos del archivo DEX
adb shell run-as com.app.lab.rce "ls -la files/temp_*/payload.dex"

# Debe mostrar permisos de solo lectura (r--r--r--)
```

### **Problema: UI no se actualiza**

```bash
# Forzar reinicio de la aplicación
adb shell am force-stop com.app.lab.rce
adb shell am start -n com.app.lab.rce/.MainActivity
```

---

## 📚 **Referencias**

- [NowSecure - Android MultiDex RCE Vulnerability](https://www.nowsecure.com/)
- [mitmproxy Documentation](https://docs.mitmproxy.org/)
- [Android Security Best Practices](https://developer.android.com/security)

---

**⚠️ Disclaimer: Este laboratorio es solo para fines educativos y de investigación en ciberseguridad. El uso malicioso está prohibido.**
