package com.app.lab.rce

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class CompromiseReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_COMPROMISED = "com.app.lab.rce.COMPROMISED"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_COMPROMISED) {
            val timestamp = intent.getLongExtra("timestamp", 0L)
            val source = intent.getStringExtra("source") ?: "unknown"
            
            Log.e("CompromiseReceiver", "System compromised - Source: $source, Time: $timestamp")
        }
    }
} 