package com.app.lab.rce

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dalvik.system.DexClassLoader
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

class VulnerableReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "VulnerableReceiver"
        private const val DEFAULT_URL = "http://10.0.2.2:8000/ad_bundle.zip"
        private const val ZIP_FILENAME = "ad_bundle.zip"
        private const val DEX_FILENAME = "payload.dex"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Broadcast received!")

        val url = intent.getStringExtra("url") ?: DEFAULT_URL
        Log.d(TAG, "Using URL: $url")

        Thread {
            try {
                downloadAndExecuteFromZip(context, url)
            } catch (e: Exception) {
                Log.e(TAG, "Error in downloadAndExecuteFromZip", e)
            }
        }.start()
    }

    private fun downloadAndExecuteFromZip(context: Context, urlString: String) {
        val zipFile = fetchZip(context, urlString)
        Log.d(TAG, "ZIP downloaded to: ${zipFile.absolutePath}")

        val dexFile = unzipDex(zipFile, context)
        Log.d(TAG, "DEX extracted to: ${dexFile.absolutePath}")

        val optimizedDir = context.getDir("dex", Context.MODE_PRIVATE)
        val classLoader = DexClassLoader(
            dexFile.absolutePath,
            optimizedDir.absolutePath,
            null,
            context.classLoader
        )

        try {
            val clazz = classLoader.loadClass("pwn.Shell")
            val method = clazz.getMethod("trigger", Context::class.java)
            method.invoke(null, context)
            Log.d(TAG, "Successfully executed pwn.Shell.trigger()")
        } catch (e: Exception) {
            Log.e(TAG, "Error executing DEX code", e)
        }
    }

    private fun fetchZip(context: Context, urlString: String): File {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.doInput = true
        connection.connect()

        val zipFile = File(context.filesDir, ZIP_FILENAME)
        val outputStream = FileOutputStream(zipFile)

        connection.inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }

        return zipFile
    }

    private fun unzipDex(zipFile: File, context: Context): File {
        val dexOut = File(context.filesDir, DEX_FILENAME)

        ZipInputStream(zipFile.inputStream()).use { zin ->
            var entry = zin.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.endsWith(".dex")) {
                    dexOut.outputStream().use { output ->
                        zin.copyTo(output)
                    }
                    Log.d(TAG, "Extracted DEX file: ${entry.name}")
                    break
                }
                entry = zin.nextEntry
            }
        }

        return dexOut
    }
}
