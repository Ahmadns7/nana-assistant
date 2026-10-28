package com.nana.stt

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import com.nana.common.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

data class WhisperModel(
    val id: String,
    val name: String,
    val fileName: String,
    val url: String,
    val sizeLabel: String,
    val sizeBytes: Long,
    val sha1: String,
    val downloaded: Boolean = false,
    val active: Boolean = false,
    val progress: Int? = null
)

data class WhisperModelState(val model: WhisperModel, val queued: Boolean)

/** Tiny whisper.cpp checkpoints for optional foreground voice command transcription. */
object WhisperModelCatalog {
    const val PREFS = "whisper_models"
    const val BACKEND_KEY = "command_speech_backend"
    const val ACTIVE_MODEL_KEY = "active_whisper_model"
    const val BACKEND_VOSK = "vosk"
    const val BACKEND_WHISPER = "whisper"

    val models = listOf(
        WhisperModel(
            id = "tiny_en",
            name = "Whisper tiny.en · English",
            fileName = "ggml-tiny.en.bin",
            url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.en.bin?download=true",
            sizeLabel = "75 MiB",
            sizeBytes = 78_643_200,
            sha1 = "c78c86eb1a8faa21b369bcd33207cc90d64ae9df"
        ),
        WhisperModel(
            id = "tiny_multi",
            name = "Whisper tiny · multilingual",
            fileName = "ggml-tiny.bin",
            url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin?download=true",
            sizeLabel = "75 MiB",
            sizeBytes = 78_643_200,
            sha1 = "bd577a113a864445d4c299885e0cb97d4ba92b5f"
        )
    )

    fun modelFile(context: Context, modelId: String = activeModelId(context)): File? =
        models.firstOrNull { it.id == modelId }?.let { File(File(context.filesDir, "models/whisper"), it.fileName) }

    fun isInstalled(context: Context, modelId: String): Boolean =
        modelFile(context, modelId)?.let { file -> file.isFile && file.length() >= (models.first { it.id == modelId }.sizeBytes * 0.98).toLong() } == true

    fun activeModelId(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(ACTIVE_MODEL_KEY, models.first().id) ?: models.first().id

    fun backend(context: Context): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(BACKEND_KEY, BACKEND_VOSK) ?: BACKEND_VOSK

    fun isWhisperSupported(): Boolean = Build.SUPPORTED_ABIS.any { it == "arm64-v8a" } && android.os.Process.is64Bit()
}

/** User-started model transfers are handled by Android Download Manager; inference is local. */
class WhisperModelManager(private val context: Context) {
    private val downloadManager = context.getSystemService(DownloadManager::class.java)
    private val prefs = context.getSharedPreferences(WhisperModelCatalog.PREFS, Context.MODE_PRIVATE)

    fun models(): List<WhisperModelState> = WhisperModelCatalog.models.map { model ->
        val downloadId = prefs.getLong(downloadKey(model.id), -1L)
        WhisperModelState(
            model = model.copy(
                downloaded = WhisperModelCatalog.isInstalled(context, model.id),
                active = WhisperModelCatalog.backend(context) == WhisperModelCatalog.BACKEND_WHISPER &&
                    WhisperModelCatalog.activeModelId(context) == model.id,
                progress = downloadId.takeIf { it >= 0 }?.let(::progressFor)
            ),
            queued = downloadId >= 0
        )
    }

    fun enqueue(modelId: String): Result<Unit> {
        val model = WhisperModelCatalog.models.firstOrNull { it.id == modelId }
            ?: return Result.Failure(Result.Error("MODEL_NOT_FOUND", "Whisper model is not available."))
        if (!WhisperModelCatalog.isWhisperSupported()) {
            return Result.Failure(Result.Error("WHISPER_UNSUPPORTED", "Whisper inference currently requires a 64-bit ARM phone."))
        }
        if (prefs.contains(downloadKey(modelId))) return Result.Success(Unit)
        if (WhisperModelCatalog.isInstalled(context, modelId)) return Result.Success(Unit)
        return try {
            val request = DownloadManager.Request(Uri.parse(model.url))
                .setTitle("${model.name} model")
                .setDescription("Downloading ${model.sizeLabel} through Android Download Manager")
                .setMimeType("application/octet-stream")
                .setAllowedOverMetered(false)
                .setAllowedOverRoaming(false)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "nana-${model.id}.bin")
            prefs.edit().putLong(downloadKey(modelId), downloadManager.enqueue(request)).apply()
            Result.Success(Unit)
        } catch (error: Exception) {
            Result.Failure(Result.Error("DOWNLOAD_FAILED", error.message ?: "Could not start Android Download Manager."))
        }
    }

    suspend fun refresh(): Result<List<WhisperModelState>> = withContext(Dispatchers.IO) {
        try {
            var failure: String? = null
            WhisperModelCatalog.models.forEach { model ->
                val key = downloadKey(model.id)
                val id = prefs.getLong(key, -1L)
                if (id >= 0) {
                    val cursor = downloadManager.query(DownloadManager.Query().setFilterById(id))
                    if (cursor == null) {
                        prefs.edit().remove(key).apply()
                    } else cursor.use {
                        if (!it.moveToFirst()) return@use
                        when (it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                            DownloadManager.STATUS_SUCCESSFUL -> runCatching { install(model, id) }
                                .onFailure { error ->
                                    File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "nana-${model.id}.bin").delete()
                                    downloadManager.remove(id)
                                    prefs.edit().remove(key).apply()
                                    failure = error.message ?: "Whisper model install failed."
                                }
                            DownloadManager.STATUS_FAILED -> {
                                downloadManager.remove(id)
                                prefs.edit().remove(key).apply()
                                failure = "${model.name} download failed. Check your connection and try again."
                            }
                        }
                    }
                }
            }
            if (failure != null) Result.Failure(Result.Error("MODEL_DOWNLOAD_FAILED", failure!!))
            else Result.Success(models())
        } catch (error: Exception) {
            Result.Failure(Result.Error("MODEL_INSTALL_FAILED", error.message ?: "Could not install the Whisper model."))
        }
    }

    fun select(modelId: String): Result<Unit> {
        if (!WhisperModelCatalog.isInstalled(context, modelId)) {
            return Result.Failure(Result.Error("MODEL_NOT_INSTALLED", "Download this Whisper model before selecting it."))
        }
        if (!WhisperModelCatalog.isWhisperSupported()) {
            return Result.Failure(Result.Error("WHISPER_UNSUPPORTED", "Whisper inference currently requires a 64-bit ARM phone."))
        }
        prefs.edit()
            .putString(WhisperModelCatalog.ACTIVE_MODEL_KEY, modelId)
            .putString(WhisperModelCatalog.BACKEND_KEY, WhisperModelCatalog.BACKEND_WHISPER)
            .apply()
        return Result.Success(Unit)
    }

    fun useVosk() {
        prefs.edit().putString(WhisperModelCatalog.BACKEND_KEY, WhisperModelCatalog.BACKEND_VOSK).apply()
    }

    fun remove(modelId: String): Result<Unit> {
        val model = WhisperModelCatalog.models.firstOrNull { it.id == modelId }
            ?: return Result.Failure(Result.Error("MODEL_NOT_FOUND", "Whisper model is not available."))
        val id = prefs.getLong(downloadKey(modelId), -1L)
        if (id >= 0) downloadManager.remove(id)
        File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "nana-${model.id}.bin").delete()
        WhisperModelCatalog.modelFile(context, modelId)?.delete()
        prefs.edit().remove(downloadKey(modelId)).apply()
        if (WhisperModelCatalog.activeModelId(context) == modelId) useVosk()
        return Result.Success(Unit)
    }

    private fun install(model: WhisperModel, downloadId: Long) {
        val archive = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "nana-${model.id}.bin")
        if (!archive.isFile || archive.length() < model.sizeBytes * 0.98) throw IllegalStateException("The downloaded file is incomplete.")
        if (sha1(archive) != model.sha1) throw IllegalStateException("Whisper model checksum did not match; the file was discarded.")
        val destination = WhisperModelCatalog.modelFile(context, model.id) ?: throw IllegalStateException("Unknown Whisper model")
        destination.parentFile?.mkdirs()
        val staging = File(destination.parentFile, "${destination.name}.installing")
        archive.copyTo(staging, overwrite = true)
        if (sha1(staging) != model.sha1 || !staging.renameTo(destination)) {
            staging.delete()
            throw IllegalStateException("Could not finish installing the Whisper model.")
        }
        archive.delete()
        prefs.edit().remove(downloadKey(model.id)).apply()
        downloadManager.remove(downloadId)
    }

    private fun sha1(file: File): String {
        val digest = MessageDigest.getInstance("SHA-1")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    private fun progressFor(id: Long): Int? = downloadManager.query(DownloadManager.Query().setFilterById(id))?.use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
        if (status == DownloadManager.STATUS_SUCCESSFUL) 100
        else if (status == DownloadManager.STATUS_FAILED) null
        else {
            val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            if (total > 0) ((downloaded * 100) / total).toInt() else 0
        }
    }

    private fun downloadKey(modelId: String) = "download_$modelId"
}
