package com.nana.llm

import android.content.Context
import com.nana.common.ModelCatalog
import com.nana.common.ModelInfo
import com.nana.common.Result
import com.nana.common.getModelsDir
import com.nana.logging.NanaLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Manages LLM model selection, downloading, and switching.
 */
class ModelManager(private val context: Context) {
    private var currentModel: ModelInfo? = null

    suspend fun getAvailableModels(): Result<List<ModelInfo>> = withContext(Dispatchers.IO) {
        val models = ModelCatalog.LLM_MODELS.map { info ->
            val modelFile = File(context.getModelsDir(), info.path)
            info.copy(downloaded = modelFile.exists(), active = currentModel?.id == info.id)
        }
        Result.Success(models)
    }

    suspend fun setActiveModel(modelId: String): Result<ModelInfo> = withContext(Dispatchers.IO) {
        val models = ModelCatalog.LLM_MODELS
        val model = models.find { it.id == modelId } ?: return@withContext Result.Failure(Result.Error("NOT_FOUND", "Model $modelId not found"))
        
        val modelFile = File(context.getModelsDir(), model.path)
        if (!modelFile.exists()) {
            return@withContext Result.Failure(Result.Error("NOT_DOWNLOADED", "Model not downloaded. Download first."))
        }

        currentModel = model.copy(active = true)
        NanaLogger.i("ModelManager", "Active model set to: $modelId")
        Result.Success(currentModel!!)
    }

    fun getCurrentModel(): ModelInfo? = currentModel

    suspend fun downloadModel(modelId: String, progressCallback: (Float) -> Unit): Result<File> = withContext(Dispatchers.IO) {
        // TODO: Implement model download from Hugging Face or other source
        // For now, return error - models must be pre-bundled or manually placed
        Result.Failure(Result.Error("NOT_IMPLEMENTED", "Model download not implemented. Place model in files/models/llm/"))
    }

    fun getDeviceTier(): ModelInfo.DeviceTier {
        val totalMem = getTotalMemoryMB()
        return when {
            totalMem < 3000 -> ModelInfo.DeviceTier.LOW
            totalMem < 6000 -> ModelInfo.DeviceTier.MID
            totalMem < 10000 -> ModelInfo.DeviceTier.HIGH
            else -> ModelInfo.DeviceTier.FLAGSHIP
        }
    }

    private fun getTotalMemoryMB(): Int {
        val memInfo = android.app.ActivityManager.MemoryInfo()
        (context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager).getMemoryInfo(memInfo)
        return (memInfo.totalMem / (1024 * 1024)).toInt()
    }

    fun getRecommendedModel(): ModelInfo {
        val tier = getDeviceTier()
        return ModelCatalog.LLM_MODELS
            .filter { it.isCompatibleWith(tier) }
            .maxByOrNull { it.ramUsageMB } 
            ?: ModelCatalog.LLM_MODELS.first()
    }
}
