package com.nana.agent

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.nana.common.Constants
import com.nana.logging.NanaLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Foreground service for running long-running agent loops.
 * Keeps the agent alive while processing multi-step commands.
 */
class AgentService : Service() {
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var wakeLock: PowerManager.WakeLock? = null
    private val notificationId = 1003
    private val channelId = "agent_channel"

    companion object {
        const val ACTION_START = "com.nana.agent.START"
        const val ACTION_STOP = "com.nana.agent.STOP"
        const val ACTION_COMMAND = "com.nana.agent.COMMAND"
        const val EXTRA_INTENT = "intent"
        const val EXTRA_SESSION_ID = "session_id"
    }

    override fun onCreate() {
        super.onCreate()
        NanaLogger.init(BuildConfig.DEBUG)
        createNotificationChannel()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_START -> startAgent(intent)
            ACTION_STOP -> stopAgent()
            ACTION_COMMAND -> handleCommand(intent)
        }
        return START_STICKY
    }

    private fun startAgent(intent: Intent?) {
        showForegroundNotification("Agent ready")
    }

    private fun stopAgent() {
        scope.coroutineContext[Job]?.cancel()
        wakeLock?.release()
        wakeLock = null
        hideForegroundNotification()
        stopSelf()
    }

    private fun handleCommand(intent: Intent?) {
        // Handle incoming command for agent processing
        // This would delegate to AgentLoop
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NaNa:Agent")
        wakeLock?.acquire()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Agent Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background agent processing"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun showForegroundNotification(text: String) {
        val notification = buildNotification(text)
        startForeground(notificationId, notification)
    }

    private fun hideForegroundNotification() {
        stopForeground(true)
        val manager = getSystemService(NotificationManager::class.java)
        manager.cancel(notificationId)
    }

    private fun buildNotification(text: String): Notification {
        val intent = Intent(this, AgentService::class.java).setAction(ACTION_STOP)
        val pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("NaNa Agent")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_pause,
                "Stop",
                pendingIntent
            ).build())
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopAgent()
        super.onDestroy()
    }
}