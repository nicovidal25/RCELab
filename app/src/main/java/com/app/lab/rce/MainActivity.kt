package com.app.lab.rce

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    private val isSecureState = mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startAdUpdateService()
        startSecurityMonitoring()

        setContent {
            MaterialTheme {
                val isSecure by isSecureState
                MainScreen(isSecure = isSecure)
            }
        }
    }

    private fun startAdUpdateService() {
        startService(Intent(this, AdUpdateService::class.java))
    }

    private fun startSecurityMonitoring() {
        lifecycleScope.launch {
            while (true) {
                isSecureState.value = !File(filesDir, "pwned.txt").exists()
                delay(5000)
            }
        }
    }
}
