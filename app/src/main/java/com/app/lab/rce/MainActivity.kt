package com.app.lab.rce

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Start service
        val serviceIntent = Intent(this, AdUpdateService::class.java)
        startService(serviceIntent)
        
        setContent {
            MaterialTheme {
                RCELabInterface()
            }
        }
    }
    
    @Composable
    fun RCELabInterface() {
        var isSecure by remember { mutableStateOf(true) }
        
        // Monitor security status
        LaunchedEffect(Unit) {
            while (true) {
                // Check if evidence file exists (indicates compromise)
                val evidenceFile = File(filesDir, "pwned.txt")
                isSecure = !evidenceFile.exists()
                delay(5000) // Check every 5 seconds
            }
        }
        
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Title
                Text(
                    text = "RCE Lab",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Spacer(modifier = Modifier.height(48.dp))
                
                // Status
                Text(
                    text = if (isSecure) "SECURE" else "COMPROMISED",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSecure) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
