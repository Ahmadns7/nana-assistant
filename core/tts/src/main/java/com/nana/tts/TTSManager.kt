package com.nana.tts

import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.nana.common.Constants
import com.nana.common.Result
import com.nana.logging.NanaLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Text-to-speech manager supporting Android system TTS and offline Piper.
 */
class TTSManager(private val context: Context) {
    private var systemTTS: TextToSpeech? = null
    private var piperEngine: PiperEngine? = null
    private var currentEngine: Engine = Engine.SYSTEM
    private var initialized = false
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var speakCallback: ((Boolean) -> Unit)? = null
    private var currentUtteranceId: String? = null

    enum class Engine { SYSTEM, PIPER }

    suspend fun initialize(engine: Engine = Engine.SYSTEM): Result<Unit> = withContext(Dispatchers.IO) {
        currentEngine = engine
        return@withContext when (engine) {
            Engine.SYSTEM -> initializeSystemTTS()
            Engine.PIPER -> initializePiper()
        }
    }

    private suspend fun initializeSystemTTS(): Result<Unit> {
        return try {
            systemTTS = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val langResult = systemTTS!!.setLanguage(Locale.getDefault())
                    if (langResult < TextToSpeech.LANG_AVAILABLE) {
                        NanaLogger.w("TTSManager", "Language not supported, using US English")
                        systemTTS!!.setLanguage(Locale.US)
                    }
                    systemTTS!!.setSpeechRate(Constants.TTS_DEFAULT_RATE)
                    systemTTS!!.setPitch(Constants.TTS_DEFAULT_PITCH)
                    systemTTS!!.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            currentUtteranceId = utteranceId
                        }
                        override fun onDone(utteranceId: String?) {
                            speakCallback?.invoke(true)
                        }
                        override fun onError(utteranceId: String?, error: Int) {
                            NanaLogger.e("TTSManager", "TTS error: $error")
                            speakCallback?.invoke(false)
                        }
                    })
                    initialized = true
                    NanaLogger.i("TTSManager", "System TTS initialized")
                } else {
                    NanaLogger.e("TTSManager", "System TTS initialization failed")
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(Result.Error("TTS_INIT_FAILED", "System TTS init failed", e))
        }
    }

    private suspend fun initializePiper(): Result<Unit> {
        return try {
            piperEngine = PiperEngine(context)
            val initResult = piperEngine!!.initialize()
            if (initResult.isSuccess) {
                initialized = true
                NanaLogger.i("TTSManager", "Piper TTS initialized")
            }
            initResult
        } catch (e: Exception) {
            Result.Failure(Result.Error("TTS_INIT_FAILED", "Piper init failed", e))
        }
    }

    suspend fun speak(text: String, language: Locale? = null): Result<Unit> = withContext(Dispatchers.IO) {
        if (!initialized) return@withContext Result.Failure(Result.Error("NOT_INITIALIZED", "TTS not initialized"))
        if (text.isBlank()) return@withContext Result.Success(Unit)

        NanaLogger.logTTSStart(text, currentEngine.name)
        return@withContext when (currentEngine) {
            Engine.SYSTEM -> speakSystem(text, language)
            Engine.PIPER -> speakPiper(text, language)
        }
    }

    private suspend fun speakSystem(text: String, language: Locale?): Result<Unit> {
        return try {
            val utteranceId = java.util.UUID.randomUUID().toString()
            val params = android.os.Bundle().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
                }
            }
            val lang = language ?: Locale.getDefault()
            systemTTS!!.setLanguage(lang)
            val result = systemTTS!!.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            if (result != TextToSpeech.SUCCESS) {
                Result.Failure(Result.Error("TTS_SPEAK_FAILED", "System TTS speak failed"))
            } else {
                Result.Success(Unit)
            }
        } catch (e: Exception) {
            Result.Failure(Result.Error("TTS_SPEAK_FAILED", "System TTS error", e))
        }
    }

    private suspend fun speakPiper(text: String, language: Locale?): Result<Unit> {
        return piperEngine?.synthesize(text, language) ?: Result.Failure(Result.Error("PIPER_NOT_READY", "Piper engine not ready"))
    }

    fun stop() {
        systemTTS?.stop()
        piperEngine?.stop()
        speakCallback?.invoke(false)
    }

    fun setSpeakCallback(callback: (Boolean) -> Unit) {
        speakCallback = callback
    }

    fun setEngine(engine: Engine) {
        if (engine != currentEngine) {
            scope.launch { initialize(engine) }
        }
    }

    fun getCurrentEngine(): Engine = currentEngine

    fun isInitialized(): Boolean = initialized

    fun shutdown() {
        stop()
        systemTTS?.shutdown()
        piperEngine?.release()
        scope.coroutineContext[Job]?.cancel()
        initialized = false
    }
}
