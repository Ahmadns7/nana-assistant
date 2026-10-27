package com.nana.notifications

import android.content.Context
import com.nana.common.Result
import com.nana.common.SessionContext
import com.nana.logging.NanaLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Voice-driven notification triage.
 * "Read my notifications" -> summarize
 * "Reply to the last one" -> voice reply
 */
class NotificationTriage(
    private val context: Context,
    private val notificationListener: NotificationListener
) {
    suspend fun readNotifications(filterPackage: String? = null): Result<String> = withContext(Dispatchers.IO) {
        val result = notificationListener.readNotificationsAloud(filterPackage)
        return@withContext result.flatMap { notifications ->
            if (notifications.isEmpty()) {
                Result.Success("No notifications${filterPackage?.let { " from $it" } ?: ""}")
            } else {
                val summary = StringBuilder()
                summary.append("${notifications.size} notification${if (notifications.size > 1) "s" else ""}")
                
                notifications.take(5).forEachIndexed { index, n ->
                    summary.append("
${index + 1}. ${n.title}: ${n.text.take(100)}")
                }
                
                if (notifications.size > 5) {
                    summary.append("
... and ${notifications.size - 5} more")
                }
                
                Result.Success(summary.toString())
            }
        }
    }

    suspend fun replyToLast(message: String): Result<Unit> = withContext(Dispatchers.IO) {
        val result = notificationListener.readNotificationsAloud(null)
        return@withContext result.flatMap { notifications ->
            if (notifications.isEmpty()) {
                Result.Failure(Result.Error("NO_NOTIFICATIONS", "No notifications to reply to"))
            } else {
                // Find the most recent one with reply action
                val repliable = notifications.find { n ->
                    // Check if notification supports reply
                    true // Simplified
                } ?: notifications.first()
                
                notificationListener.replyToNotification(repliable.key, message)
            }
        }
    }

    suspend fun dismissLast(): Result<Unit> = withContext(Dispatchers.IO) {
        val result = notificationListener.readNotificationsAloud(null)
        return@withContext result.flatMap { notifications ->
            if (notifications.isEmpty()) {
                Result.Failure(Result.Error("NO_NOTIFICATIONS", "No notifications to dismiss"))
            } else {
                notificationListener.dismissNotification(notifications.first().key)
            }
        }
    }

    suspend fun dismissAll(filterPackage: String? = null): Result<Int> = withContext(Dispatchers.IO) {
        val result = notificationListener.readNotificationsAloud(filterPackage)
        return@withContext result.flatMap { notifications ->
            var count = 0
            notifications.forEach { n ->
                notificationListener.dismissNotification(n.key).onSuccess { count++ }
            }
            Result.Success(count)
        }
    }
}
