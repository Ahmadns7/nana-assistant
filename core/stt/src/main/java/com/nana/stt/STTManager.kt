package com.nana.stt

import android.content.Context
import android.content.Intent
import com.nana.common.Result
import com.nana.logging.NanaLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * High-level manager for STT service.
 */
class STTManager(private val context: Context) {
    private var serviceStarted = false
    private var resultCallback: ((String, Float) -> Unit)? = null
    private var partialCallback: ((String) -> Unit)? = null

    suspend fun startListening(): Result<Unit> = withContext(Dispatchers.IO) {
        val intent = Intent(context, STTService::class.java).setAction(STTService.ACTION_START_LISTENING)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        serviceStarted = true
        Result.Success(Unit)
    }

    suspend fun stopListening(): Result<Unit> = withContext(Dispatchers.IO) {
        val intent = Intent(context, STTService::class.java).setAction(STTService.ACTION_STOP_LISTENING)
        context.stopService(intent)
        serviceStarted = false
        Result.Success(Unit)
    }

    fun setResultCallback(callback: (String, Float) -> Unit) {
        resultCallback = callback
    }

    fun setPartialCallback(callback: (String) -> Unit) {
        partialCallback = callback
    }

    fun isListening(): Boolean = serviceStarted
}
