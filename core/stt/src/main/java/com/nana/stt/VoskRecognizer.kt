package com.nana.stt

import android.content.Context
import com.nana.common.Constants
import com.nana.common.Result
import com.nana.logging.NanaLogger
import com.alphacephei.vosk.Model
import com.alphacephei.vosk.Recognizer
import com.alphacephei.vosk.Vosk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

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
            Vosk.setLogLevel(0) // Disable Vosk internal logs

            val modelFile = resolveModelPath()
            if (!File(modelFile).exists()) {
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

    private fun resolveModelPath(): String {
        val modelsDir = context.getModelsDir()
        val modelDir = File(modelsDir, Constants.STT_MODEL_DIR)
        if (modelDir.exists()) return modelDir.absolutePath

        // Try to extract from assets (for first run)
        return extractModelFromAssets().absolutePath
    }

    private fun extractModelFromAssets(): File {
        val modelsDir = context.getModelsDir()
        val modelDir = File(modelsDir, Constants.STT_MODEL_DIR)
        modelDir.mkdirs()

        // Vosk models are typically zip files - extract if needed
        val assetName = "models/${Constants.STT_MODEL_DIR}.zip"
        try {
            context.assets.open(assetName).use { input ->
                val zipFile = File(modelsDir, "${Constants.STT_MODEL_DIR}.zip")
                FileOutputStream(zipFile).use { output ->
                    input.copyTo(output)
                }
                // Extract zip
                java.util.zip.ZipFile(zipFile).use { zip ->
                    zip.entries().asSequence().forEach { entry ->
                        val outFile = File(modelDir, entry.name)
                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            zip.getInputStream(entry).use { input ->
                                FileOutputStream(outFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                    }
                }
                zipFile.delete()
            }
        } catch (e: Exception) {
            NanaLogger.w("VoskRecognizer", "Model not in assets, expecting pre-downloaded: ${e.message}")
        }
        return modelDir
    }

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    /**
     * Feed audio data to the recognizer.
     * @param pcm Short array of PCM audio at sampleRate
     */
    fun acceptWaveform(pcm: ShortArray): Boolean {
        if (!initialized || recognizer == null) return false
        return recognizer!!.acceptWaveform(pcm, pcm.size)
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
