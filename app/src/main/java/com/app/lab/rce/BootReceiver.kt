package com.app.lab.rce

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, 
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                Log.i(TAG, "📱 Sistema iniciado - Activando servicio automático")
                
                // Inicializar servicio automático al arranque
                val serviceIntent = Intent(context, AdUpdateService::class.java)
                context.startForegroundService(serviceIntent)
                
                Log.d(TAG, "🟢 Servicio automático iniciado en boot")
            }
        }
    }
} 