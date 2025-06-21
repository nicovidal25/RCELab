package com.app.lab.rce

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CompromiseReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "CompromiseReceiver"
        const val ACTION_COMPROMISED = "com.app.lab.rce.COMPROMISED"
        
        // Callback para notificar a la UI
        var onCompromiseDetected: ((String, Long) -> Unit)? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_COMPROMISED) {
            val timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis())
            val source = intent.getStringExtra("source") ?: "Unknown"
            
            val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
            
            Log.e(TAG, "🚨 COMPROMISO DETECTADO - Fuente: $source - Hora: $timeStr")
            
            // Notificar a la UI si está disponible
            onCompromiseDetected?.invoke(source, timestamp)
            
            // Simular actividades post-explotación
            simulatePostExploitActivity(source)
        }
    }
    
    private fun simulatePostExploitActivity(source: String) {
        Log.w(TAG, "💀 Iniciando actividades post-explotación...")
        Log.w(TAG, "📤 Exfiltrando datos sensibles")
        Log.w(TAG, "📡 Estableciendo comunicación C&C")
        Log.w(TAG, "🔒 Estableciendo persistencia")
        
        Log.e(TAG, """
            ===== ANÁLISIS FORENSE =====
            Fuente: $source
            Timestamp: ${System.currentTimeMillis()}
            PID: ${android.os.Process.myPid()}
            UID: ${android.os.Process.myUid()}
            ============================
        """.trimIndent())
    }
} 