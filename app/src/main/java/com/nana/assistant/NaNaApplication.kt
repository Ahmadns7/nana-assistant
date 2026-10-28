package com.nana.assistant

import android.app.Application
import com.nana.logging.NanaLogger

class NaNaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NanaLogger.init(BuildConfig.DEBUG)

    }
}