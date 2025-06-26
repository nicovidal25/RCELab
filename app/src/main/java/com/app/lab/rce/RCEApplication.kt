package com.app.lab.rce

import android.content.Intent
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication

class RCEApplication : MultiDexApplication() {

    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        val serviceIntent = Intent(this, AdUpdateService::class.java)
        startService(serviceIntent)
    }
}
