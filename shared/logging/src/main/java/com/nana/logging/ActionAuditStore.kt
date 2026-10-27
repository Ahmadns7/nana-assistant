package com.nana.logging

import android.content.Context
import com.nana.common.ActionLog
import com.nana.common.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Persistent action audit log for post-session analysis.
 * Writes to rotating daily files in app-private storage.
 */
class ActionAuditStore(private val context: Context) {
    private val logDir = File(context.filesDir, "audit_logs").apply { mkdirs() }
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var currentWriter: FileWriter? = null
    private var currentDate: String = ""

    suspend fun log(actionLog: ActionLog): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            ensureWriterForToday()
            currentWriter?.write(formatLogEntry(actionLog))
            currentWriter?.write("
")
            currentWriter?.flush()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(Result.Error("AUDIT_WRITE_FAILED", "Failed to write audit log", e))
        }
    }

    private fun ensureWriterForToday() {
        val today = dateFormat.format(System.currentTimeMillis())
        if (today != currentDate) {
            currentWriter?.close()
            val file = File(logDir, "actions_$today.log")
            currentWriter = FileWriter(file, true)
            currentDate = today
        }
    }

    private fun formatLogEntry(log: ActionLog): String {
        return "%d|%s|%s|%s|%s|%s|%d|%d|%s|%s".format(
            log.timestamp,
            log.sessionId,
            log.intentCategory.name,
            log.intent.replace("
", " ").take(200),
            log.screenStateHash,
            log.actionTaken,
            log.result.ordinal,
            log.durationMs,
            log.stepNumber,
            log.errorMessage ?: ""
        )
    }

    suspend fun getLogsForDate(date: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val file = File(logDir, "actions_$date.log")
            if (!file.exists()) return@withContext Result.Success(emptyList())
            Result.Success(file.readLines())
        } catch (e: Exception) {
            Result.Failure(Result.Error("AUDIT_READ_FAILED", "Failed to read audit log", e))
        }
    }

    suspend fun getRecentLogs(days: Int = 7): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val files = logDir.listFiles()?.filter { it.name.startsWith("actions_") }?.sortedByDescending { it.name }?.take(days) ?: emptyArray()
            val allLines = mutableListOf<String>()
            for (file in files) {
                allLines.addAll(file.readLines())
            }
            Result.Success(allLines)
        } catch (e: Exception) {
            Result.Failure(Result.Error("AUDIT_READ_FAILED", "Failed to read recent audit logs", e))
        }
    }

    fun close() {
        currentWriter?.close()
        currentWriter = null
    }
}
