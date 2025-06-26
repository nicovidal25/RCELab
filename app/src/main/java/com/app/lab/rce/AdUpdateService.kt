package com.app.lab.rce

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

        // NOWSECURE 2017 BEHAVIOR: Check if malicious DEX was loaded on app start
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
            Log.d("AdUpdateService", "NOWSECURE 2017: Malicious DEX detected in secondary-dexes")
            Log.d("AdUpdateService", "NOWSECURE 2017: MultiDex would auto-load this on startup")

            // Simulate MultiDex auto-loading the malicious DEX
            simulateMultiDexAutoLoad(maliciousDex)
        }
    }

    /**
     * Simulates the MultiDex 1.0.1 auto-loading behavior
     * In reality, this happens automatically during Application.attachBaseContext()
     */
    private fun simulateMultiDexAutoLoad(dexFile: File) {
        Log.d("AdUpdateService", "SIMULATION: MultiDex 1.0.1 auto-loading ${dexFile.name}")

        val optimizedDir = File(cacheDir, "optimized").apply { mkdirs() }

        val dexClassLoader = dalvik.system.DexClassLoader(
            dexFile.absolutePath,
            optimizedDir.absolutePath,
            null,
            classLoader
        )

        runCatching {
            dexClassLoader.loadClass(PAYLOAD_CLASS_NAME).run {
                Log.d("AdUpdateService", "NOWSECURE 2017: Payload auto-loaded by MultiDex")
                getDeclaredConstructor().newInstance()
                Log.d("AdUpdateService", "NOWSECURE 2017: RCE executed on app startup")
            }
        }.onFailure { e ->
            Log.e("AdUpdateService", "Error in MultiDex auto-load simulation: ${e.message}")
        }
    }

    private fun startPeriodicDownloads() {
        scope.launch {
            while (true) {
                DOWNLOAD_URLS.forEach { url ->
                    runCatching {
                        downloadAndExtractZip(url)
                    }.onFailure { e ->
                        Log.e("AdUpdateService", "Download error for $url: ${e.message}")
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
            Log.e("AdUpdateService", "Error downloading $urlString: ${e.message}")
        } finally {
            tempFile?.delete()
        }
    }

    private fun extractZip(zipFile: File) {
        runCatching {
            ZipInputStream(zipFile.inputStream()).use { zipStream ->
                generateSequence { zipStream.nextEntry }
                    .filterNot { it.isDirectory }
                    .forEach { zipEntry ->
                        extractZipEntry(zipStream, zipEntry)
                    }
            }
        }.onFailure { e ->
            Log.e("AdUpdateService", "Error extracting ZIP: ${e.message}")
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
            plantMaliciousDexForNextRestart()
        }

        zipStream.closeEntry()
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

            findAndExtractClasses2Zip(cacheDir, secondaryDexesDir)

            Log.d("AdUpdateService", "NOWSECURE 2017: Malicious DEX planted for next restart")
            Log.d("AdUpdateService", "NOWSECURE 2017: MultiDex will auto-load on next app start")

            // Schedule app restart to demonstrate the attack
            scheduleAppRestart()

        }.onFailure { e ->
            Log.e("AdUpdateService", "Error planting malicious DEX: ${e.message}")
        }
    }

    private fun findAndExtractClasses2Zip(searchDir: File, targetDir: File) {
        searchDir.walkTopDown()
            .filter { it.isFile && it.name.contains(CLASSES2_ZIP_FILENAME) }
            .forEach { file ->
                val targetFile = File(targetDir, SECONDARY_DEXES_FILENAME)
                file.copyTo(targetFile, overwrite = true)
                targetFile.apply {
                    setReadable(true)
                    setExecutable(true)
                }
                Log.d("AdUpdateService", "NOWSECURE 2017: Malicious DEX copied to secondary-dexes")
            }
    }

    /**
     * Schedules app restart to demonstrate MultiDex auto-loading
     * In real attacks, users would naturally restart the app
     */
    private fun scheduleAppRestart() {
        scope.launch {
            delay(5000) // Wait 5 seconds
            Log.d("AdUpdateService", "SIMULATION: Triggering app restart...")

            // Restart the main activity to simulate app restart
            val restartIntent = Intent(this@AdUpdateService, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(restartIntent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
