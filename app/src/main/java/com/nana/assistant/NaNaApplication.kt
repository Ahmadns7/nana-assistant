package com.nana.assistant

import android.app.Application
import com.nana.logging.NanaLogger
import timber.log.Timber

class NaNaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NanaLogger.init(BuildConfig.DEBUG)
        
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}