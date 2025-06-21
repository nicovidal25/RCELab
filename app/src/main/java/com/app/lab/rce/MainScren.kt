package com.app.lab.rce

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dalvik.system.DexClassLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipInputStream
import javax.net.ssl.HttpsURLConnection

@Composable
fun RCELabScreen(context: Context) {
    var isCompromised by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var lastCompromiseSource by remember { mutableStateOf("") }
    val logLines = remember { mutableStateListOf<String>() }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    fun logLine(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        logLines.add("[$timestamp] $message")
        coroutineScope.launch {
            listState.animateScrollToItem(logLines.size - 1)
        }
    }

    LaunchedEffect(Unit) {
        CompromiseReceiver.onCompromiseDetected = { source, timestamp ->
            coroutineScope.launch {
                isCompromised = true
                lastCompromiseSource = source
                logLine("🚨 SISTEMA COMPROMETIDO - Fuente: $source")
            }
        }
        
        logLine("🎯 RCE Lab iniciado")
        logLine("📡 Servicio automático activo")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "RCE Lab",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isCompromised) Color(0xFFF44336) else Color(0xFF4CAF50)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isCompromised) "☠️ SISTEMA COMPROMETIDO" else "🔒 SISTEMA SEGURO",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                if (isCompromised && lastCompromiseSource.isNotEmpty()) {
                    Text(
                        text = "Fuente: $lastCompromiseSource",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        Button(
            onClick = {
                if (!isLoading) {
                    coroutineScope.launch {
                        isLoading = true
                        logLine("👆 Descarga manual iniciada")
                        vulnerableFlow(context, logLine = ::logLine) { compromised ->
                            isCompromised = compromised
                            lastCompromiseSource = "Manual"
                        }
                        isLoading = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            enabled = !isLoading
        ) {
            Text(
                text = if (isLoading) "Descargando..." else "Forzar descarga manual",
                fontSize = 16.sp
            )
        }

        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                items(logLines) { line ->
                    Text(
                        text = line,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

suspend fun vulnerableFlow(
    context: Context,
    logLine: (String) -> Unit,
    onCompromised: (Boolean) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            logLine("Descargando ad_bundle.zip...")

            val url = URL("https://updates.rcelab.com/ad_bundle.zip")
            val connection = url.openConnection() as HttpsURLConnection
            connection.requestMethod = "GET"
            connection.doInput = true
            connection.connect()

            val zipFile = File(context.filesDir, "ad_bundle.zip")
            FileOutputStream(zipFile).use { output ->
                connection.inputStream.use { input ->
                    input.copyTo(output)
                }
            }

            logLine("Extrayendo archivos...")
            val dexFile = File(context.filesDir, "payload.dex")

            ZipInputStream(zipFile.inputStream()).use { zin ->
                var entry = zin.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.name.endsWith(".dex")) {
                        dexFile.outputStream().use { output ->
                            zin.copyTo(output)
                        }
                        logLine("📂 Extraído: ${entry.name}")
                        break
                    }
                    entry = zin.nextEntry
                }
            }

            logLine("⚠️ Cargando código ejecutable...")

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

                logLine("💀 RCE EJECUTADA")
                withContext(Dispatchers.Main) {
                    onCompromised(true)
                }
            } catch (e: Exception) {
                logLine("❌ Error ejecutando: ${e.message}")
            }

        } catch (e: Exception) {
            logLine("❌ Error: ${e.message}")
        }
    }
}
