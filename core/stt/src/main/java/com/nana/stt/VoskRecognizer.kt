package com.nana.stt

import android.content.Context
import com.nana.common.Constants
import com.nana.common.Result
import com.nana.logging.NanaLogger
import com.nana.common.getModelsDir
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Offline speech-to-text using Vosk.
 * Supports streaming recognition with partial results.
 */
class VoskRecognizer(
    private val context: Context,
    private val modelPath: String? = null,
    private val sampleRate: Int = Constants.STT_SAMPLE_RATE
) {
    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var initialized = false

    interface Callback {
        fun onPartialResult(text: String)
        fun onFinalResult(text: String, confidence: Float)
        fun onError(error: String)
    }

    private var callback: Callback? = null

    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            LibVosk.setLogLevel(LogLevel.WARNINGS) // Disable Vosk internal logs

            val modelFile = resolveModelPath()
            if (!File(modelFile).isDirectory) {
                return@withContext Result.Failure(Result.Error("MODEL_NOT_FOUND", "Vosk model not found at $modelFile"))
            }

            model = Model(modelFile)
            recognizer = Recognizer(model!!, sampleRate.toFloat())
            initialized = true

            NanaLogger.i("VoskRecognizer", "Vosk initialized: model=$modelFile sampleRate=$sampleRate")
            Result.Success(Unit)
        } catch (e: Exception) {
            NanaLogger.e("VoskRecognizer", "Initialization failed", e)
            Result.Failure(Result.Error("INIT_FAILED", "Vosk initialization failed", e, recoverable = false))
        }
    }

    private fun resolveModelPath(): String = SttModelCatalog.modelDirectory(context)?.absolutePath
        ?: File(File(context.getModelsDir(), "stt"), Constants.STT_MODEL_DIR).absolutePath

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    /**
     * Feed audio data to the recognizer.
     * @param pcm Short array of PCM audio at sampleRate
     */
    fun acceptWaveForm(pcm: ShortArray): Boolean {
        if (!initialized || recognizer == null) return false
        return recognizer!!.acceptWaveForm(pcm, pcm.size)
    }

    fun getPartialResult(): String {
        if (!initialized || recognizer == null) return ""
        return recognizer!!.partialResult
    }

    fun getFinalResult(): String {
        if (!initialized || recognizer == null) return ""
        return recognizer!!.finalResult
    }

    fun reset() {
        recognizer?.let { it.finalResult }
    }

    fun release() {
        recognizer?.close()
        model?.close()
        recognizer = null
        model = null
        initialized = false
    }

    companion object {
        fun parsePartialResult(json: String): String {
            return try {
                val obj = org.json.JSONObject(json)
                obj.getString("partial")
            } catch (e: Exception) {
                ""
            }
        }

        fun parseFinalResult(json: String): Pair<String, Float> {
            return try {
                val obj = org.json.JSONObject(json)
                val text = obj.getString("text")
                val confidence = if (obj.has("confidence")) obj.getDouble("confidence").toFloat() else 1.0f
                text to confidence
            } catch (e: Exception) {
                "" to 0f
            }
        }
    }
}
