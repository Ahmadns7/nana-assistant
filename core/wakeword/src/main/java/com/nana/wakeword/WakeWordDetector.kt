package com.nana.wakeword

import android.content.Context
import com.nana.common.Result
import com.nana.logging.NanaLogger
import com.nana.stt.SttModelCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer

/** Offline phrase detection constrained to the selected small Vosk model. */
class WakeWordDetector(private val context: Context) {
    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var initialized = false
    private var wakePhrase = DEFAULT_PHRASE

    suspend fun initialize(phrase: String, modelId: String): Result<Unit> = withContext(Dispatchers.IO) {
        release()
        try {
            val normalized = normalizePhrase(phrase)
            if (normalized.isBlank()) {
                return@withContext Result.Failure(Result.Error("INVALID_WAKE_PHRASE", "Enter a wake phrase first."))
            }
            val modelDirectory = SttModelCatalog.modelDirectory(context, modelId)
                ?.takeIf { SttModelCatalog.isInstalled(context, modelId) }
                ?: return@withContext Result.Failure(Result.Error("MODEL_NOT_FOUND", "Download a small speech model before enabling wake-word detection."))

            LibVosk.setLogLevel(LogLevel.WARNINGS)
            val loadedModel = Model(modelDirectory.absolutePath)
            model = loadedModel
            val grammar = JSONArray().put(normalized).toString()
            recognizer = Recognizer(loadedModel, SAMPLE_RATE.toFloat(), grammar)
            wakePhrase = normalized
            initialized = true
            NanaLogger.i("WakeWordDetector", "Vosk wake phrase ready: $normalized ($modelId)")
            Result.Success(Unit)
        } catch (error: Exception) {
            release()
            NanaLogger.e("WakeWordDetector", "Could not initialize Vosk wake phrase", error)
            Result.Failure(Result.Error("WAKE_WORD_INIT_FAILED", error.message ?: "Could not initialize offline wake-word detection.", error))
        }
    }

    fun process(pcm: ShortArray): Boolean {
        val activeRecognizer = recognizer ?: return false
        if (!initialized) return false
        return try {
            if (activeRecognizer.acceptWaveForm(pcm, pcm.size)) {
                val text = JSONObject(activeRecognizer.result).optString("text")
                normalizePhrase(text) == wakePhrase
            } else {
                false
            }
        } catch (error: Exception) {
            NanaLogger.e("WakeWordDetector", "Vosk wake phrase processing failed", error)
            false
        }
    }

    fun getFrameLength(): Int = FRAME_LENGTH
    fun getSampleRate(): Int = SAMPLE_RATE
    fun release() {
        initialized = false
        recognizer?.close()
        model?.close()
        recognizer = null
        model = null
    }

    companion object {
        const val SAMPLE_RATE = 16000
        const val CHANNELS = 1
        const val ENCODING = android.media.AudioFormat.ENCODING_PCM_16BIT
        const val FRAME_LENGTH = 3200 // 200 ms chunks work well with the Vosk streaming decoder.
        const val DEFAULT_PHRASE = "hey nana"

        fun normalizePhrase(value: String): String = value
            .lowercase()
            .replace(Regex("[^\\p{L}\\p{N} ]"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
    }
}
