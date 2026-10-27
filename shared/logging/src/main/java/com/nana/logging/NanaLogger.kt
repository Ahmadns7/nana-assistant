package com.nana.logging

import android.util.Log
import com.nana.common.ActionLog
import com.nana.common.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Centralized logging for NaNa with structured output and action audit trail.
 * All logs include session context for debugging.
 */
object NanaLogger {
    private const val TAG = "NaNa"
    private var debugEnabled = BuildConfig.DEBUG

    fun init(debug: Boolean = BuildConfig.DEBUG) {
        debugEnabled = debug
        if (debug) {
            Timber.plant(DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }
    }

    private class DebugTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority >= Log.DEBUG) {
                Log.println(priority, tag ?: TAG, message, t)
            }
        }
    }

    private class ReleaseTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority >= Log.WARN) {
                Log.println(priority, tag ?: TAG, message, t)
            }
        }
    }

    // Standard logging
    fun v(tag: String, msg: String, throwable: Throwable? = null) = log(Log.VERBOSE, tag, msg, throwable)
    fun d(tag: String, msg: String, throwable: Throwable? = null) = log(Log.DEBUG, tag, msg, throwable)
    fun i(tag: String, msg: String, throwable: Throwable? = null) = log(Log.INFO, tag, msg, throwable)
    fun w(tag: String, msg: String, throwable: Throwable? = null) = log(Log.WARN, tag, msg, throwable)
    fun e(tag: String, msg: String, throwable: Throwable? = null) = log(Log.ERROR, tag, msg, throwable)
    fun wtf(tag: String, msg: String, throwable: Throwable? = null) = log(Log.ASSERT, tag, msg, throwable)

    private fun log(priority: Int, tag: String, msg: String, throwable: Throwable?) {
        if (debugEnabled || priority >= Log.WARN) {
            Timber.log(priority, throwable, "[%s] %s", tag, msg)
        }
    }

    // Action audit logging - critical for debugging agent behavior
    fun logAction(actionLog: ActionLog) {
        val tag = "ACTION_AUDIT"
        val msg = "session=%s step=%d intent="%s" action="%s" result=%s duration=%dms error=%s".format(
            actionLog.sessionId,
            actionLog.stepNumber,
            actionLog.intent.take(100),
            actionLog.actionTaken,
            actionLog.result,
            actionLog.durationMs,
            actionLog.errorMessage ?: "none"
        )
        if (actionLog.result == ActionLog.ActionResult.FAILED || actionLog.result == ActionLog.ActionResult.BLOCKED) {
            e(tag, msg)
        } else {
            i(tag, msg)
        }
    }

    // Session lifecycle
    fun logSessionStart(sessionId: String, trigger: String) {
        i("SESSION", "START session=$sessionId trigger=$trigger")
    }

    fun logSessionEnd(sessionId: String, durationMs: Long, stepCount: Int, completed: Boolean) {
        i("SESSION", "END session=$sessionId duration=%dms steps=$stepCount completed=$completed".format(durationMs))
    }

    // Model performance
    fun logModelInference(modelId: String, inputTokens: Int, outputTokens: Int, latencyMs: Long, success: Boolean) {
        val tag = "MODEL_PERF"
        val msg = "model=$modelId in=$inputTokens out=$outputTokens latency=%dms success=$success".format(latencyMs)
        if (success) i(tag, msg) else w(tag, msg)
    }

    fun logSTTResult(text: String, confidence: Float, latencyMs: Long) {
        i("STT", "text="%s" confidence=%.2f latency=%dms".format(text.take(100), confidence, latencyMs))
    }

    fun logTTSStart(text: String, engine: String) {
        d("TTS", "engine=$engine text="%s"".format(text.take(100)))
    }

    fun logAccessibilityEvent(eventType: Int, packageName: String, className: String) {
        v("A11Y", "event=%d pkg=%s class=%s".format(eventType, packageName, className))
    }

    fun logAgentStep(sessionId: String, step: Int, action: String, screenHash: String, success: Boolean) {
        val tag = "AGENT_STEP"
        val msg = "session=$sessionId step=$step action=$action screen=$screenHash success=$success"
        if (success) d(tag, msg) else w(tag, msg)
    }

    fun logError(context: String, error: Throwable, recoverable: Boolean) {
        val tag = if (recoverable) "ERROR_RECOVERABLE" else "ERROR_FATAL"
        e(tag, "context=$context error=${error.message}", error)
    }

    // Convenience for coroutine scopes
    fun CoroutineScope.logAsync(block: () -> Unit) = launch(Dispatchers.IO) { block() }
}
