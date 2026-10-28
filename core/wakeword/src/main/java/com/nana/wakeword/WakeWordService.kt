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
import com.nana.common.Result
import com.nana.logging.NanaLogger
import com.nana.core.wakeword.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * Foreground service for continuous wake word detection.
 * Holds a partial wake lock to work with screen off.
 */
class WakeWordService : Service() {
    private lateinit var detector: WakeWordDetector
    private var audioRecord: AudioRecord? = null
    @Volatile private var recording = false
    @Volatile private var starting = false
    private var startupJob: Job? = null
    private var audioJob: Job? = null
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
        detector = WakeWordDetector(this)
        createNotificationChannel()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_START -> {
                try {
                    // Promote before loading Porcupine or touching the microphone. Android
                    // requires a started foreground service to promote promptly.
                    showForegroundNotification("Starting wake word detection...")
                    startDetection()
                } catch (e: SecurityException) {
                    NanaLogger.e("WakeWordService", "Microphone permission is required", e)
                    stopSelf(startId)
                } catch (e: Exception) {
                    NanaLogger.e("WakeWordService", "Could not start foreground service", e)
                    stopSelf(startId)
                }
            }
            ACTION_STOP -> {
                stopDetection()
                stopSelf(startId)
            }
        }
        return START_STICKY
    }

    private fun startDetection() {
        if (recording || starting) return
        starting = true

        startupJob = scope.launch(Dispatchers.IO) {
            try {
                val initResult = detector.initialize()
                if (initResult.isFailure) {
                    NanaLogger.e("WakeWordService", "Detector init failed: ${(initResult as? Result.Failure)?.error?.message}")
                    stopSelf()
                    return@launch
                }

                startAudioRecording()
                updateNotification("Listening for 'Hey NaNa'...")
            } catch (e: SecurityException) {
                NanaLogger.e("WakeWordService", "Microphone permission is required", e)
                stopSelf()
            } catch (e: Exception) {
                NanaLogger.e("WakeWordService", "Could not start audio recording", e)
                stopSelf()
            } finally {
                starting = false
            }
        }
    }

    private fun startAudioRecording() {
        val minimumBufferSize = AudioRecord.getMinBufferSize(
            WakeWordDetector.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            WakeWordDetector.ENCODING
        )
        if (minimumBufferSize <= 0) {
            throw IllegalStateException("This device does not support the wake word audio format")
        }

        val bufferSize = max(minimumBufferSize * 4, detector.getFrameLength() * 2)
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            WakeWordDetector.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            WakeWordDetector.ENCODING,
            bufferSize
        )
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            throw IllegalStateException("Audio recorder could not be initialized")
        }

        try {
            recorder.startRecording()
            if (recorder.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                throw IllegalStateException("Audio recorder did not enter recording state")
            }
            audioRecord = recorder
            recording = true
            audioJob = scope.launch(Dispatchers.IO) { processAudioLoop() }
        } catch (e: Exception) {
            recorder.release()
            throw e
        }
    }

    private fun processAudioLoop() {
        val frameLength = detector.getFrameLength()
        val buffer = ShortArray(frameLength)

        while (recording) {
            val recorder = audioRecord ?: break
            val read = recorder.read(buffer, 0, frameLength)
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
        starting = false
        startupJob?.cancel()
        startupJob = null
        audioJob?.cancel()
        audioJob = null
        runCatching { audioRecord?.stop() }
        runCatching { audioRecord?.release() }
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
        detector.release()
        wakeLock?.release()
        wakeLock = null
        scope.coroutineContext[Job]?.cancel()
        super.onDestroy()
    }

    fun setDetectionCallback(callback: (String) -> Unit) {
        detectionCallback = callback
    }
}
