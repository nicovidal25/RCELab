package com.app.lab.rce

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }
    
    private lateinit var compromiseReceiver: CompromiseReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Inicializar el servicio automático
        initializeAdSDK()
        
        // Registrar receiver para notificaciones de compromiso
        setupCompromiseReceiver()
        
        setContent {
            MaterialTheme {
                RCELabScreen(this)
            }
        }
    }
    
    private fun initializeAdSDK() {
        Log.i(TAG, "🎯 Iniciando servicio automático...")
        
        val serviceIntent = Intent(this, AdUpdateService::class.java)
        startForegroundService(serviceIntent)
    }
    
    private fun setupCompromiseReceiver() {
        compromiseReceiver = CompromiseReceiver()
        val filter = IntentFilter(CompromiseReceiver.ACTION_COMPROMISED)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(compromiseReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(compromiseReceiver, filter)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (::compromiseReceiver.isInitialized) {
            unregisterReceiver(compromiseReceiver)
        }
    }
}

