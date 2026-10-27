package com.nana.wakeword

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
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
 * Foreground service for continuous wake word detection.
 * Holds a partial wake lock to work with screen off.
 */
class WakeWordService : Service() {
    private lateinit var detector: WakeWordDetector
    private var audioRecord: AudioRecord? = null
    private var recording = false
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var wakeLock: PowerManager.WakeLock? = null
    private var detectionCallback: ((String) -> Unit)? = null
    private val notificationId = 1001
    private val channelId = "wake_word_channel"

    companion object {
        const val ACTION_START = "com.nana.wakeword.START"
        const val ACTION_STOP = "com.nana.wakeword.STOP"
        const val ACTION_DETECTION = "com.nana.wakeword.DETECTION"
        const val EXTRA_KEYWORD = "keyword"
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
            ACTION_START -> startDetection()
            ACTION_STOP -> stopDetection()
        }
        return START_STICKY
    }

    private fun startDetection() {
        if (recording) return

        scope.launch(Dispatchers.IO) {
            val initResult = detector.initialize()
            if (initResult.isFailure) {
                NanaLogger.e("WakeWordService", "Detector init failed: ${initResult.getOrNull()?.error?.message}")
                stopSelf()
                return@launch
            }

            startAudioRecording()
            showForegroundNotification("Listening for "Hey NaNa"...")
        }
    }

    private fun startAudioRecording() {
        val bufferSize = AudioRecord.getMinBufferSize(
            WakeWordDetector.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            WakeWordDetector.ENCODING
        ) * 4

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            WakeWordDetector.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            WakeWordDetector.ENCODING,
            bufferSize
        ).also {
            it.startRecording()
            recording = true
            processAudioLoop()
        }
    }

    private fun processAudioLoop() {
        val frameLength = detector.getFrameLength()
        val buffer = ShortArray(frameLength)

        while (recording && audioRecord != null) {
            val read = audioRecord!!.read(buffer, 0, frameLength)
            if (read == frameLength) {
                val keywordIndex = detector.process(buffer)
                if (keywordIndex >= 0) {
                    NanaLogger.i("WakeWordService", "Wake word detected! index=$keywordIndex")
                    onWakeWordDetected()
                }
            } else if (read < 0) {
                NanaLogger.w("WakeWordService", "Audio read error: $read")
            }
        }
    }

    private fun onWakeWordDetected() {
        // Vibrate briefly for feedback
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
        vibrator?.vibrate(android.os.VibrationEffect.createOneShot(100, android.os.VibrationEffect.DEFAULT_AMPLITUDE))

        // Broadcast detection
        val intent = Intent(Constants.ACTION_WAKE_WORD_DETECTED)
        sendBroadcast(intent)

        // Update notification
        updateNotification("Wake word detected!")

        // Callback for UI
        detectionCallback?.invoke("hey_nana")
    }

    private fun stopDetection() {
        recording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        hideForegroundNotification()
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NaNa:WakeWord")
        wakeLock?.acquire()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Wake Word Detection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Continuous wake word listening"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun showForegroundNotification(text: String) {
        val notification = buildNotification(text)
        startForeground(notificationId, notification)
    }

    private fun updateNotification(text: String) {
        val notification = buildNotification(text)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(notificationId, notification)
    }

    private fun hideForegroundNotification() {
        stopForeground(true)
        val manager = getSystemService(NotificationManager::class.java)
        manager.cancel(notificationId)
    }

    private fun buildNotification(text: String): Notification {
        val intent = Intent(this, WakeWordService::class.java).setAction(ACTION_STOP)
        val pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("NaNa Assistant")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
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
        stopDetection()
        wakeLock?.release()
        wakeLock = null
        scope.coroutineContext[Job]?.cancel()
        super.onDestroy()
    }

    fun setDetectionCallback(callback: (String) -> Unit) {
        detectionCallback = callback
    }
}
