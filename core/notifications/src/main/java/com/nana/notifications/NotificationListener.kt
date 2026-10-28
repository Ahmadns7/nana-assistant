package com.nana.notifications

import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.nana.common.Result
import com.nana.common.SessionContext
import com.nana.logging.NanaLogger
import com.nana.core.notifications.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * NotificationListenerService for reading and triaging notifications.
 * Requires user to enable in Settings > Apps > Special access > Notification access.
 */
class NotificationListener : NotificationListenerService() {
    private var notificationCallback: ((StatusBarNotification) -> Unit)? = null
    private var removalCallback: ((StatusBarNotification) -> Unit)? = null

    companion object {
        const val ACTION_READ_NOTIFICATIONS = "com.nana.notifications.READ"
        const val ACTION_REPLY = "com.nana.notifications.REPLY"
        const val ACTION_DISMISS = "com.nana.notifications.DISMISS"
        const val EXTRA_PACKAGE = "package"
        const val EXTRA_TEXT = "text"
        const val EXTRA_ID = "id"
    }

    override fun onCreate() {
        super.onCreate()
        NanaLogger.init(BuildConfig.DEBUG)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn?.let {
            NanaLogger.i("NotificationListener", "Posted: ${it.packageName} - ${getNotificationText(it)}")
            notificationCallback?.invoke(it)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn?.let {
            NanaLogger.i("NotificationListener", "Removed: ${it.packageName}")
            removalCallback?.invoke(it)
        }
    }

    private fun getNotificationText(sbn: StatusBarNotification): String {
        val notification = sbn.notification
        val bundle = notification.extras
        val title = bundle.getString(NotificationCompat.EXTRA_TITLE) ?: ""
        val text = bundle.getString(NotificationCompat.EXTRA_TEXT) ?: ""
        val bigText = bundle.getString(NotificationCompat.EXTRA_BIG_TEXT) ?: ""
        return "$title $text $bigText".trim()
    }

    fun setNotificationCallback(callback: (StatusBarNotification) -> Unit) {
        notificationCallback = callback
    }

    fun setRemovalCallback(callback: (StatusBarNotification) -> Unit) {
        removalCallback = callback
    }

    /**
     * Read notifications aloud (for voice triage).
     */
    suspend fun readNotificationsAloud(filterPackage: String? = null): Result<List<NotificationSummary>> = withContext(Dispatchers.IO) {
        val notifications = super.getActiveNotifications()
            .filter { filterPackage == null || it.packageName == filterPackage }
            .map { sbn ->
                val text = getNotificationText(sbn)
                NotificationSummary(
                    packageName = sbn.packageName,
                    title = sbn.notification.extras.getString(NotificationCompat.EXTRA_TITLE) ?: "",
                    text = text,
                    timestamp = sbn.postTime,
                    key = sbn.key,
                    id = sbn.id,
                    tag = sbn.tag
                )
            }
        Result.Success(notifications.toList())
    }

    /**
     * Reply to a notification (if it has RemoteInput).
     */
    suspend fun replyToNotification(key: String, replyText: String): Result<Unit> = withContext(Dispatchers.IO) {
        val sbn = super.getActiveNotifications().firstOrNull { it.key == key }
        if (sbn == null) return@withContext Result.Failure(Result.Error("NOT_FOUND", "Notification not found"))

        val notification = sbn.notification
        val action = notification.actions?.firstOrNull {
            it.remoteInputs?.isNotEmpty() == true
        }

        if (action == null) {
            return@withContext Result.Failure(Result.Error("NO_REMOTE_INPUT", "Notification doesn't support inline reply"))
        }

        val remoteInputs = action.remoteInputs ?: emptyArray()
        val results = android.os.Bundle().apply {
            putCharSequence(remoteInputs[0].resultKey, replyText)
        }

        try {
            val intent = Intent().putExtras(results)
            RemoteInput.addResultsToIntent(remoteInputs, intent, results)
            action.actionIntent.send(this@NotificationListener, 0, intent)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(Result.Error("REPLY_FAILED", "Failed to send reply", e))
        }
    }

    /**
     * Dismiss a notification.
     */
    fun dismissNotification(key: String): Result<Unit> {
        return try {
            cancelNotification(key)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(Result.Error("DISMISS_FAILED", "Failed to dismiss", e))
        }
    }

    data class NotificationSummary(
        val packageName: String,
        val title: String,
        val text: String,
        val timestamp: Long,
        val key: String,
        val id: Int,
        val tag: String?
    )
}
