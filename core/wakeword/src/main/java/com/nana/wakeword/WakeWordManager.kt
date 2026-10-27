package com.nana.wakeword

import android.content.Context
import android.content.Intent
import com.nana.common.Result
import com.nana.logging.NanaLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * High-level manager for wake word service lifecycle.
 */
class WakeWordManager(private val context: Context) {
    private var serviceStarted = false
    private var detectionCallback: ((String) -> Unit)? = null

    suspend fun start(): Result<Unit> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        if (serviceStarted) return@withContext Result.Success(Unit)

        val intent = Intent(context, WakeWordService::class.java).setAction(WakeWordService.ACTION_START)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        serviceStarted = true
        NanaLogger.i("WakeWordManager", "Wake word service started")
        Result.Success(Unit)
    }

    suspend fun stop(): Result<Unit> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        if (!serviceStarted) return@withContext Result.Success(Unit)

        val intent = Intent(context, WakeWordService::class.java).setAction(WakeWordService.ACTION_STOP)
        context.stopService(intent)
        serviceStarted = false
        NanaLogger.i("WakeWordManager", "Wake word service stopped")
        Result.Success(Unit)
    }

    fun setDetectionCallback(callback: (String) -> Unit) {
        detectionCallback = callback
    }

    fun isRunning(): Boolean = serviceStarted
}
