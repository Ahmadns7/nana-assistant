package com.nana.notifications

import android.content.Context
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.nana.common.Result
import com.nana.common.SessionContext
import com.nana.logging.NanaLogger
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
     * Get all active notifications.
     */
    fun getActiveNotifications(): Array<StatusBarNotification> {
        return getActiveNotifications()
    }

    /**
     * Read notifications aloud (for voice triage).
     */
    suspend fun readNotificationsAloud(filterPackage: String? = null): Result<List<NotificationSummary>> = withContext(Dispatchers.IO) {
        val notifications = getActiveNotifications()
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
        val sbn = getActiveNotifications().firstOrNull { it.key == key }
        if (sbn == null) return@withContext Result.Failure(Result.Error("NOT_FOUND", "Notification not found"))

        val notification = sbn.notification
        val remoteInputs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notification.remoteInputs
        } else null

        if (remoteInputs == null || remoteInputs.isEmpty()) {
            return@withContext Result.Failure(Result.Error("NO_REMOTE_INPUT", "Notification doesn't support inline reply"))
        }

        val remoteInput = remoteInputs[0]
        val results = android.os.Bundle().apply {
            putCharSequence(remoteInput.resultKey, replyText)
        }

        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notification.remoteInputResultsIntent
        } else null

        if (pendingIntent != null) {
            try {
                pendingIntent.send(context = this, code = 0, intent = Intent().putExtras(results))
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Failure(Result.Error("REPLY_FAILED", "Failed to send reply", e))
            }
        } else {
            Result.Failure(Result.Error("NO_PENDING_INTENT", "No reply intent available"))
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
