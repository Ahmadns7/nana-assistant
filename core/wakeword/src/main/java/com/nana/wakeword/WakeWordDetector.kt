package com.nana.wakeword

import android.content.Context
import com.nana.common.Constants
import com.nana.common.Result
import com.nana.logging.NanaLogger
import ai.picovoice.porcupine.Porcupine
import ai.picovoice.porcupine.PorcupineException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Offline wake word detection using Porcupine.
 * Runs in a foreground service with real-time audio processing.
 */
class WakeWordDetector(
    private val context: Context,
    private val modelPath: String? = null
) {
    private var porcupine: Porcupine? = null
    private var initialized = false
    private val frameLength = 512 // Porcupine default

    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val keywordPath = modelPath ?: resolveModelPath()
            if (!File(keywordPath).exists()) {
                return@withContext Result.Failure(Result.Error("MODEL_NOT_FOUND", "Wake word model not found at $keywordPath"))
            }

            porcupine = Porcupine.Builder()
                .setAccessKey(getAccessKey()) // User must provide via build config or secure storage
                .setKeywordPath(keywordPath)
                .setSensitivity(Constants.WAKE_WORD_SENSITIVITY)
                .build(context)

            frameLength = porcupine!!.frameLength
            initialized = true
            NanaLogger.i("WakeWordDetector", "Porcupine initialized with model: $keywordPath, frameLength=$frameLength")
            Result.Success(Unit)
        } catch (e: PorcupineException) {
            NanaLogger.e("WakeWordDetector", "Porcupine init failed", e)
            Result.Failure(Result.Error("INIT_FAILED", "Porcupine initialization failed", e, recoverable = false))
        } catch (e: Exception) {
            NanaLogger.e("WakeWordDetector", "Unexpected error", e)
            Result.Failure(Result.Error("INIT_FAILED", "Wake word detector init failed", e))
        }
    }

    private fun resolveModelPath(): String {
        val modelsDir = context.getModelsDir()
        val modelFile = File(modelsDir, Constants.WAKE_WORD_MODEL_NAME)
        if (modelFile.exists()) return modelFile.absolutePath

        // Try to copy from assets
        return copyModelFromAssets().absolutePath
    }

    private fun copyModelFromAssets(): File {
        val modelsDir = context.getModelsDir()
        val dest = File(modelsDir, Constants.WAKE_WORD_MODEL_NAME)
        context.assets.open("models/${Constants.WAKE_WORD_MODEL_NAME}").use { input ->
            FileOutputStream(dest).use { output ->
                input.copyTo(output)
            }
        }
        return dest
    }

    private fun getAccessKey(): String {
        // In production: retrieve from secure storage or build config
        // For now, return placeholder - user must replace with actual Picovoice access key
        return System.getenv("PORCUPINE_ACCESS_KEY") ?: "YOUR_PORCUPINE_ACCESS_KEY_HERE"
    }

    /**
     * Process a single audio frame for wake word detection.
     * @param pcm Short array of length frameLength (512 samples at 16kHz)
     * @return keyword index if detected (-1 if none), -2 on error
     */
    fun process(pcm: ShortArray): Int {
        if (!initialized || porcupine == null) return -2
        return try {
            porcupine!!.process(pcm)
        } catch (e: Exception) {
            NanaLogger.e("WakeWordDetector", "Process error", e)
            -2
        }
    }

    fun getFrameLength(): Int = frameLength

    fun getSampleRate(): Int = Porcupine.sampleRate

    fun release() {
        porcupine?.delete()
        porcupine = null
        initialized = false
    }

    companion object {
        const val SAMPLE_RATE = 16000
        const val CHANNELS = 1
        const val ENCODING = android.media.AudioFormat.ENCODING_PCM_16BIT
    }
}
