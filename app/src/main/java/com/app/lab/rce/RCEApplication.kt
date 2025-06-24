package com.app.lab.rce

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex

/**
 * Custom Application class for NowSecure 2017 vector
 * Uses androidx.multidex but configured to work with MultiDex 1.0.1 behavior
 */
class RCEApplication : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        // NOWSECURE 2017 VECTOR: Enable MultiDex support
        // The com.android.support:multidex:1.0.1 dependency provides the underlying behavior
        // while androidx.multidx.MultiDex provides the API compatibility
        MultiDex.install(this)
    }
}
