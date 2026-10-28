package com.nana.stt

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import com.nana.common.Constants
import com.nana.common.Result
import com.nana.common.getModelsDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

data class OfflineSpeechModel(
    val id: String,
    val name: String,
    val directory: String,
    val url: String,
    val sizeLabel: String,
    val sizeBytes: Long,
    val downloaded: Boolean = false,
    val active: Boolean = false,
    val progress: Int? = null
)

/** Small, mobile-friendly Vosk models published by the Vosk project. */
object SttModelCatalog {
    val models = listOf(
        OfflineSpeechModel("en_us", "English (US)", "vosk-model-small-en-us-0.15", "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip", "40 MB", 40_000_000),
        OfflineSpeechModel("en_in", "English (India)", "vosk-model-small-en-in-0.4", "https://alphacephei.com/vosk/models/vosk-model-small-en-in-0.4.zip", "36 MB", 36_000_000),
        OfflineSpeechModel("hi", "Hindi", "vosk-model-small-hi-0.22", "https://alphacephei.com/vosk/models/vosk-model-small-hi-0.22.zip", "42 MB", 42_000_000),
        OfflineSpeechModel("es", "Spanish", "vosk-model-small-es-0.42", "https://alphacephei.com/vosk/models/vosk-model-small-es-0.42.zip", "39 MB", 39_000_000),
        OfflineSpeechModel("fr", "French", "vosk-model-small-fr-0.22", "https://alphacephei.com/vosk/models/vosk-model-small-fr-0.22.zip", "41 MB", 41_000_000),
        OfflineSpeechModel("de", "German", "vosk-model-small-de-0.15", "https://alphacephei.com/vosk/models/vosk-model-small-de-0.15.zip", "45 MB", 45_000_000)
    )

    fun activeId(context: Context): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(ACTIVE_MODEL, null) ?: Constants.STT_MODEL_DIR.let { legacy -> models.firstOrNull { it.directory == legacy }?.id ?: "en_us" }

    fun modelDirectory(context: Context, modelId: String = activeId(context)): File? =
        models.firstOrNull { it.id == modelId }?.let { File(File(context.getModelsDir(), "stt"), it.directory) }

    fun isInstalled(context: Context, modelId: String): Boolean = modelDirectory(context, modelId)?.let(::isValidModel) == true

    private fun isValidModel(directory: File): Boolean =
        File(directory, "am/final.mdl").isFile && File(directory, "conf/model.conf").isFile

    internal fun setActiveId(context: Context, modelId: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(ACTIVE_MODEL, modelId).apply()
    }

    internal const val PREFS = "offline_speech_models"
    internal const val ACTIVE_MODEL = "active_model"
}

/** Queues model archives through Android DownloadManager and installs them privately. */
class SttModelDownloadManager(private val context: Context) {
    private val manager = context.getSystemService(DownloadManager::class.java)
    private val prefs = context.getSharedPreferences(SttModelCatalog.PREFS, Context.MODE_PRIVATE)

    data class ModelState(val model: OfflineSpeechModel, val queued: Boolean)

    fun models(): List<ModelState> = SttModelCatalog.models.map { model ->
        ModelState(
            model.copy(
                downloaded = SttModelCatalog.isInstalled(context, model.id),
                active = SttModelCatalog.activeId(context) == model.id,
                progress = prefs.getLong(downloadKey(model.id), -1L).takeIf { it >= 0L }?.let(::progressFor)
            ),
            prefs.contains(downloadKey(model.id))
        )
    }

    fun enqueue(modelId: String): Result<Unit> {
        val model = SttModelCatalog.models.firstOrNull { it.id == modelId }
            ?: return Result.Failure(Result.Error("MODEL_NOT_FOUND", "Speech model is not available."))
        if (SttModelCatalog.isInstalled(context, modelId)) {
            SttModelCatalog.setActiveId(context, modelId)
            return Result.Success(Unit)
        }
        if (prefs.contains(downloadKey(modelId))) return Result.Success(Unit)
        return try {
            val request = DownloadManager.Request(Uri.parse(model.url))
                .setTitle("${model.name} offline speech model")
                .setDescription("Downloading ${model.sizeLabel} through Android Download Manager")
                .setMimeType("application/zip")
                .setAllowedOverMetered(false)
                .setAllowedOverRoaming(false)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "nana-${model.id}.zip")
            prefs.edit().putLong(downloadKey(modelId), manager.enqueue(request)).apply()
            Result.Success(Unit)
        } catch (error: Exception) {
            Result.Failure(Result.Error("DOWNLOAD_FAILED", error.message ?: "Could not start Android Download Manager."))
        }
    }

    suspend fun refresh(): Result<List<ModelState>> = withContext(Dispatchers.IO) {
        try {
            var downloadError: String? = null
            SttModelCatalog.models.forEach { model ->
                val key = downloadKey(model.id)
                val id = prefs.getLong(key, -1L)
                if (id >= 0L) {
                    query(id)?.let { cursor ->
                        cursor.use {
                            if (!it.moveToFirst()) return@use
                            when (it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                                DownloadManager.STATUS_SUCCESSFUL -> runCatching { installDownloaded(model, id) }
                                    .onFailure { error ->
                                        manager.remove(id)
                                        File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "nana-${model.id}.zip").delete()
                                        prefs.edit().remove(key).apply()
                                        downloadError = error.message ?: "The downloaded speech model could not be installed."
                                    }
                                DownloadManager.STATUS_FAILED -> {
                                    manager.remove(id)
                                    prefs.edit().remove(key).apply()
                                    downloadError = "${model.name} download failed. Check your connection and try again."
                                }
                            }
                        }
                    } ?: prefs.edit().remove(key).apply()
                }
            }
            if (downloadError != null) return@withContext Result.Failure(Result.Error("MODEL_DOWNLOAD_FAILED", downloadError!!))
            Result.Success(models())
        } catch (error: Exception) {
            Result.Failure(Result.Error("MODEL_INSTALL_FAILED", error.message ?: "Could not install the speech model."))
        }
    }

    fun setActive(modelId: String): Result<Unit> {
        if (!SttModelCatalog.isInstalled(context, modelId)) {
            return Result.Failure(Result.Error("MODEL_NOT_INSTALLED", "Download this model before selecting it."))
        }
        SttModelCatalog.setActiveId(context, modelId)
        return Result.Success(Unit)
    }

    private fun installDownloaded(model: OfflineSpeechModel, downloadId: Long) {
        val archive = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "nana-${model.id}.zip")
        val destination = SttModelCatalog.modelDirectory(context, model.id)
            ?: throw IllegalStateException("Unknown speech model")
        val staging = File(destination.parentFile, "${model.directory}.installing")
        staging.deleteRecursively()
        if (!staging.mkdirs()) throw IllegalStateException("Could not prepare model storage")

        try {
            ZipInputStream(archive.inputStream().buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val segments = entry.name.replace('\\', '/').split('/').filter { it.isNotBlank() && it != "." }
                    val relative = if (segments.firstOrNull() == model.directory) segments.drop(1) else segments
                    if (relative.isEmpty() || relative.any { it == ".." }) continue
                    val output = File(staging, relative.joinToString(File.separator))
                    if (entry.isDirectory) output.mkdirs() else {
                        output.parentFile?.mkdirs()
                        FileOutputStream(output).use { out -> zip.copyTo(out) }
                    }
                    zip.closeEntry()
                }
            }
            if (!isValidModel(staging)) throw IllegalStateException("Downloaded archive is not a valid Vosk model")
            destination.deleteRecursively()
            if (!staging.renameTo(destination)) throw IllegalStateException("Could not finish model installation")
            SttModelCatalog.setActiveId(context, model.id)
            archive.delete()
            prefs.edit().remove(downloadKey(model.id)).apply()
            manager.remove(downloadId)
        } catch (error: Exception) {
            staging.deleteRecursively()
            throw error
        }
    }

    private fun query(id: Long): Cursor? = manager.query(DownloadManager.Query().setFilterById(id))

    private fun progressFor(id: Long): Int? = query(id)?.use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
        if (status == DownloadManager.STATUS_SUCCESSFUL) 100
        else if (status == DownloadManager.STATUS_FAILED) null
        else {
            val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            val soFar = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            if (total > 0) ((soFar * 100) / total).toInt() else 0
        }
    }

    private fun downloadKey(modelId: String) = "download_$modelId"

    companion object {
        private fun isValidModel(directory: File): Boolean =
            File(directory, "am/final.mdl").isFile && File(directory, "conf/model.conf").isFile
    }
}
