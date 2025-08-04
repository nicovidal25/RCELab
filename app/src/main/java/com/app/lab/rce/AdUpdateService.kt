package com.app.lab.rce

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import dalvik.system.DexClassLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class AdUpdateService : Service() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private companion object {
        const val CONNECT_TIMEOUT = 5000
        const val READ_TIMEOUT = 10000
        const val DOWNLOAD_INTERVAL = 10000L
        const val PAYLOAD_CLASS_NAME = "pwn.Shell"
        const val CLASSES2_ZIP_FILENAME = "classes2.zip"
        const val SECONDARY_DEXES_FILENAME = "com.app.lab.rce-classes2.zip"

        val DOWNLOAD_URLS = listOf(
            "http://api.vungle.com/api/v1/bundles/update.zip"
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        checkForExistingMaliciousDex()
        startPeriodicDownloads()
    }

    /**
     * Simulates MultiDex 1.0.1 auto-loading behavior
     * In the real vulnerability, MultiDex automatically loads classes2.dex on app startup
     */
    private fun checkForExistingMaliciousDex() {
        val secondaryDexesDir = File(filesDir.parent, "code_cache/secondary-dexes")
        val maliciousDex = File(secondaryDexesDir, SECONDARY_DEXES_FILENAME)
        if (maliciousDex.exists()) {
            simulateMultiDexAutoLoad(maliciousDex)
        }
    }

    /**
     * Simulates the MultiDex 1.0.1 auto-loading behavior
     * In reality, this happens automatically during Application.attachBaseContext()
     */
    private fun simulateMultiDexAutoLoad(dexFile: File) {
        val optimizedDir = File(cacheDir, "optimized").apply { mkdirs() }
        val dexClassLoader = DexClassLoader(
            dexFile.absolutePath,
            optimizedDir.absolutePath,
            null,
            classLoader
        )

        runCatching {
            dexClassLoader.loadClass(PAYLOAD_CLASS_NAME).run {
                getDeclaredConstructor().newInstance()
            }
        }.onFailure { e ->
            Log.e("AdUpdateService", "Error loading malicious DEX", e)
        }
    }

    private var downloadJob: Job? = null

    private fun startPeriodicDownloads() {
        downloadJob = scope.launch {
            while (true) {
                DOWNLOAD_URLS.forEach { url ->
                    runCatching {
                        downloadAndExtractZip(url)
                    }.onFailure { e ->
                        Log.e("AdUpdateService", "Failed to download or extract zip from: $url", e)
                    }
                }
                delay(DOWNLOAD_INTERVAL)
            }
        }
    }

    private suspend fun downloadAndExtractZip(urlString: String) = withContext(Dispatchers.IO) {
        var tempFile: File? = null
        try {
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
            }

            tempFile = File.createTempFile("update", ".zip", cacheDir)

            connection.inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            extractZip(tempFile)

        } catch (e: Exception) {
            Log.e("AdUpdateService", "Exception downloading/extracting ZIP from: $urlString", e)
        } finally {
            tempFile?.delete()
        }
    }

    private fun extractZip(zipFile: File) {
        runCatching {
            ZipInputStream(zipFile.inputStream()).use { zipStream ->
                var entryCount = 0
                generateSequence { zipStream.nextEntry }
                    .filterNot { it.isDirectory }
                    .forEach { zipEntry ->
                        entryCount++
                        extractZipEntry(zipStream, zipEntry)
                    }
            }
        }.onFailure { e ->
            Log.e("AdUpdateService", "Error extracting ZIP file", e)
        }
    }

    private fun extractZipEntry(zipStream: ZipInputStream, zipEntry: ZipEntry) {
        val entryName = zipEntry.name

        val outputFile = File(cacheDir, entryName)
        outputFile.parentFile?.mkdirs()

        outputFile.outputStream().use { output ->
            zipStream.copyTo(output)
        }


        if (entryName.contains(CLASSES2_ZIP_FILENAME)) {
            checkPathTraversalSuccess()
        }

        zipStream.closeEntry()
    }

    /**
     * Check if the path traversal successfully placed the malicious DEX
     */
    private fun checkPathTraversalSuccess() {
        val secondaryDexesDir = File(filesDir.parent, "code_cache/secondary-dexes")
        val maliciousDex = File(secondaryDexesDir, SECONDARY_DEXES_FILENAME)
        if (maliciousDex.exists()) {
            stopPeriodicDownloads()
        }
    }

    /**
     * NOWSECURE 2017 PATTERN: Plant malicious DEX for next app restart
     * This simulates the original vulnerability where the attack payload
     * gets executed automatically when the app restarts
     */
    private fun plantMaliciousDexForNextRestart() {
        runCatching {
            val secondaryDexesDir = File(filesDir.parent, "code_cache/secondary-dexes").apply {
                mkdirs()
            }

            val maliciousDex = File(secondaryDexesDir, SECONDARY_DEXES_FILENAME)
            if (maliciousDex.exists()) {
                stopPeriodicDownloads()
            }
        }.onFailure { e ->
            Log.e("AdUpdateService", "Error planting malicious DEX for next restart", e)
        }
    }

    private fun stopPeriodicDownloads() {
        downloadJob?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
