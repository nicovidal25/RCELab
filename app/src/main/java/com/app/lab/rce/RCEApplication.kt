package com.app.lab.rce

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication

/**
 * Custom Application class for NowSecure 2017 vector
 * Uses androidx.multidex but configured to work with MultiDex 1.0.1 behavior
 */
class RCEApplication : MultiDexApplication() {

    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(base)
        // NOWSECURE 2017 VECTOR: Enable MultiDex support
        // The com.android.support:multidex:1.0.1 dependency provides the underlying behavior
        // while androidx.multidx.MultiDex provides the API compatibility
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("RCEApplication", "Application initialized")
        
        // Start background service
        val serviceIntent = Intent(this, AdUpdateService::class.java)
        startService(serviceIntent)
    }
}
