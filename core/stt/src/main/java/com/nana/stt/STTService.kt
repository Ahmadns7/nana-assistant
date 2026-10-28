package com.nana.stt

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
import com.nana.core.stt.BuildConfig
import com.nana.storage.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Foreground service for speech-to-text recording and recognition.
 * Activated after wake word detection or tap-to-talk.
 */
class STTService : Service() {
    private lateinit var recognizer: VoskRecognizer
    private var audioRecord: AudioRecord? = null
    private var recording = false
    private var listeningForCommand = false
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var wakeLock: PowerManager.WakeLock? = null
    private var resultCallback: ((String, Float) -> Unit)? = null
    private var partialCallback: ((String) -> Unit)? = null
    private val notificationId = 1002
    private val channelId = "stt_channel"
    private var silenceFrames = 0
    private var speechStarted = false
    private val maxSilenceFrames = 50 // ~1 second at 20ms frames
    private val minSpeechFrames = 10 // ~200ms minimum

    companion object {
        const val ACTION_START_LISTENING = "com.nana.stt.START_LISTENING"
        const val ACTION_STOP_LISTENING = "com.nana.stt.STOP_LISTENING"
        const val ACTION_RESULT = "com.nana.stt.RESULT"
        const val EXTRA_TEXT = "text"
        const val EXTRA_CONFIDENCE = "confidence"
        const val EXTRA_PARTIAL = "partial"
    }

    override fun onCreate() {
        super.onCreate()
        NanaLogger.init(BuildConfig.DEBUG)
        createNotificationChannel()
        acquireWakeLock()
        recognizer = VoskRecognizer(this).apply {
            setCallback(object : VoskRecognizer.Callback {
                override fun onPartialResult(text: String) {
                    partialCallback?.invoke(text)
                    val intent = Intent(Constants.ACTION_STT_RESULT).putExtra(EXTRA_PARTIAL, text)
                    sendBroadcast(intent)
                }

                override fun onFinalResult(text: String, confidence: Float) {
                    NanaLogger.logSTTResult(text, confidence, 0)
                    resultCallback?.invoke(text, confidence)
                    val intent = Intent(Constants.ACTION_STT_RESULT)
                        .putExtra(EXTRA_TEXT, text)
                        .putExtra(EXTRA_CONFIDENCE, confidence)
                    sendBroadcast(intent)
                    stopListening()
                }

                override fun onError(error: String) {
                    NanaLogger.e("STTService", "Recognition error: $error")
                }
            })
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_START_LISTENING -> startListening()
            ACTION_STOP_LISTENING -> stopListening()
        }
        return START_STICKY
    }

    private fun startListening() {
        if (recording) return

        scope.launch(Dispatchers.IO) {
            val initResult = recognizer.initialize()
            if (initResult.isFailure) {
                NanaLogger.e("STTService", "Recognizer init failed: ${(initResult as? Result.Failure)?.error?.message}")
                stopSelf()
                return@launch
            }

            startAudioRecording()
            showForegroundNotification("Listening...")
            listeningForCommand = true
        }
    }

    private fun startAudioRecording() {
        val bufferSize = AudioRecord.getMinBufferSize(
            Constants.STT_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ) * 4

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            Constants.STT_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        ).also {
            it.startRecording()
            recording = true
            speechStarted = false
            silenceFrames = 0
            processAudioLoop()
        }
    }

    private fun processAudioLoop() {
        val frameSize = 512 // 32ms at 16kHz
        val buffer = ShortArray(frameSize)
        var listenFrames = 0

        while (recording && audioRecord != null) {
            val read = audioRecord!!.read(buffer, 0, frameSize)
            if (read == frameSize) {
                val hasSpeech = detectSpeech(buffer)
                if (hasSpeech) {
                    speechStarted = true
                    silenceFrames = 0
                } else if (speechStarted) {
                    silenceFrames++
                }
                val utteranceEnded = recognizer.acceptWaveForm(buffer)
                if (speechStarted) {
                    val partial = VoskRecognizer.parsePartialResult(recognizer.getPartialResult())
                    if (partial.isNotBlank()) sendSpeechUpdate(partial, partial = true)
                }
                if (utteranceEnded && speechStarted || silenceFrames >= maxSilenceFrames) {
                    val (text, confidence) = VoskRecognizer.parseFinalResult(recognizer.getFinalResult())
                    if (text.isNotBlank()) {
                        NanaLogger.logSTTResult(text, confidence, 0)
                        sendSpeechUpdate(text, partial = false, confidence = confidence)
                    }
                    stopListening()
                    resumeWakeWordIfEnabled()
                    break
                }
                listenFrames++
                if (listenFrames >= 900) {
                    stopListening()
                    resumeWakeWordIfEnabled()
                    break
                }
            }
        }
    }

    private fun resumeWakeWordIfEnabled() {
        scope.launch(Dispatchers.IO) {
            runCatching {
                val settings = SettingsStore(applicationContext)
                if (settings.getWakeWordEnabled()) {
                    val modelId = SttModelCatalog.activeId(applicationContext)
                    if (SttModelCatalog.isInstalled(applicationContext, modelId)) {
                        val wakeIntent = Intent()
                            .setClassName(packageName, "com.nana.wakeword.WakeWordService")
                            .setAction("com.nana.wakeword.START")
                            .putExtra("wake_phrase", settings.getWakeWordPhrase())
                            .putExtra("speech_model_id", modelId)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(wakeIntent)
                        else startService(wakeIntent)
                    }
                }
            }.onFailure { NanaLogger.e("STTService", "Could not resume wake-word service", it) }
            stopSelf()
        }
    }

    private fun sendSpeechUpdate(text: String, partial: Boolean, confidence: Float = 0f) {
        val intent = Intent(Constants.ACTION_STT_RESULT)
            .setPackage(packageName)
            .putExtra(EXTRA_TEXT, text)
            .putExtra(EXTRA_PARTIAL, partial)
            .putExtra(EXTRA_CONFIDENCE, confidence)
        sendBroadcast(intent)
    }

    private fun detectSpeech(pcm: ShortArray): Boolean {
        // Simple energy-based VAD
        var sum = 0L
        for (sample in pcm) {
            sum += (sample * sample).toLong()
        }
        val energy = sum / pcm.size
        return energy > 500000 // Threshold tuned for typical speech
    }

    private fun stopListening() {
        recording = false
        listeningForCommand = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        hideForegroundNotification()
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NaNa:STT")
        wakeLock?.acquire()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Speech Recognition",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active voice command recognition"
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
        val intent = Intent(this, STTService::class.java).setAction(ACTION_STOP_LISTENING)
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
        stopListening()
        recognizer.release()
        wakeLock?.release()
        wakeLock = null
        scope.coroutineContext[Job]?.cancel()
        super.onDestroy()
    }

    fun setResultCallback(callback: (String, Float) -> Unit) {
        resultCallback = callback
    }

    fun setPartialCallback(callback: (String) -> Unit) {
        partialCallback = callback
    }

    fun isListening(): Boolean = listeningForCommand
}
