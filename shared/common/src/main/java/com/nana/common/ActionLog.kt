package com.nana.common

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Entity(tableName = Constants.ACTION_LOG_TABLE)
@Serializable
data class ActionLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val intent: String,
    val intentCategory: IntentCategory,
    val screenStateHash: String,
    val actionTaken: String,
    val actionParams: String, // JSON
    val result: ActionResult,
    val errorMessage: String? = null,
    val durationMs: Long,
    val stepNumber: Int = 0,
    val sessionId: String
) {
    enum class IntentCategory {
        MEDIA_CONTROL, APP_CONTROL, MESSAGING, PHONE_BASICS, SCREEN_QUERY,
        SYSTEM_TOGGLE, MACRO, SETTINGS, UNKNOWN
    }

    enum class ActionResult {
        SUCCESS, PARTIAL, FAILED, BLOCKED, REQUIRES_CONFIRMATION
    }

    companion object {
        fun create(
            intent: String,
            category: IntentCategory,
            screenStateHash: String,
            actionTaken: String,
            actionParams: String,
            result: ActionResult,
            durationMs: Long,
            stepNumber: Int,
            sessionId: String,
            errorMessage: String? = null
        ) = ActionLog(
            intent = intent,
            intentCategory = category,
            screenStateHash = screenStateHash,
            actionTaken = actionTaken,
            actionParams = actionParams,
            result = result,
            errorMessage = errorMessage,
            durationMs = durationMs,
            stepNumber = stepNumber,
            sessionId = sessionId
        )
    }
}

