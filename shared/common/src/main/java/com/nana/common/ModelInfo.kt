package com.nana.common

import kotlinx.serialization.Serializable

@Serializable
data class ModelInfo(
    val id: String,
    val name: String,
    val type: ModelType,
    val version: String,
    val sizeBytes: Long,
    val ramUsageMB: Int,
    val estimatedLatencyMs: Int,
    val deviceTier: DeviceTier,
    val path: String,
    val downloaded: Boolean = false,
    val active: Boolean = false
) {
    enum class ModelType { WAKE_WORD, STT, LLM, TTS, OCR }

    enum class DeviceTier { LOW, MID, HIGH, FLAGSHIP }

    val sizeMB: Double get() = sizeBytes / (1024.0 * 1024.0)

    fun isCompatibleWith(deviceTier: DeviceTier): Boolean {
        return when (deviceTier) {
            DeviceTier.LOW -> this.deviceTier == DeviceTier.LOW
            DeviceTier.MID -> this.deviceTier in listOf(DeviceTier.LOW, DeviceTier.MID)
            DeviceTier.HIGH -> this.deviceTier in listOf(DeviceTier.LOW, DeviceTier.MID, DeviceTier.HIGH)
            DeviceTier.FLAGSHIP -> true
        }
    }
}

object ModelCatalog {
    val WAKE_WORD_MODELS = listOf(
        ModelInfo(
            id = "vosk_selected_phrase",
            name = "Custom wake phrase (selected Vosk model)",
            type = ModelInfo.ModelType.WAKE_WORD,
            version = "Vosk grammar",
            sizeBytes = 0,
            ramUsageMB = 80,
            estimatedLatencyMs = 300,
            deviceTier = ModelInfo.DeviceTier.LOW,
            path = "uses-selected-stt-model"
        )
    )

    val STT_MODELS = listOf(
        ModelInfo(
            id = "vosk_small_en",
            name = "Vosk Small EN-US (0.15)",
            type = ModelInfo.ModelType.STT,
            version = "0.15",
            sizeBytes = 40_000_000,
            ramUsageMB = 80,
            estimatedLatencyMs = 300,
            deviceTier = ModelInfo.DeviceTier.LOW,
            path = "stt/vosk-model-small-en-us-0.15"
        ),
        ModelInfo(
            id = "vosk_small_multi",
            name = "Vosk Small Multi-lang",
            type = ModelInfo.ModelType.STT,
            version = "0.15",
            sizeBytes = 80_000_000,
            ramUsageMB = 120,
            estimatedLatencyMs = 400,
            deviceTier = ModelInfo.DeviceTier.MID,
            path = "stt/vosk-model-small-multi-0.15"
        )
    )

    val LLM_MODELS = listOf(
        ModelInfo(
            id = "gemma_2b_q4",
            name = "Gemma 2B IT (Q4_K_M)",
            type = ModelInfo.ModelType.LLM,
            version = "2B",
            sizeBytes = 1_500_000_000,
            ramUsageMB = 1800,
            estimatedLatencyMs = 2000,
            deviceTier = ModelInfo.DeviceTier.HIGH,
            path = "llm/gemma-2b-it-q4_k_m.gguf"
        ),
        ModelInfo(
            id = "gemma_3_1b_q4",
            name = "Gemma 3 1B IT (Q4_K_M)",
            type = ModelInfo.ModelType.LLM,
            version = "3-1B",
            sizeBytes = 800_000_000,
            ramUsageMB = 1000,
            estimatedLatencyMs = 1200,
            deviceTier = ModelInfo.DeviceTier.MID,
            path = "llm/gemma-3-1b-it-q4_k_m.gguf"
        ),
        ModelInfo(
            id = "phi_3_mini_q4",
            name = "Phi-3 Mini 4K (Q4_K_M)",
            type = ModelInfo.ModelType.LLM,
            version = "3-mini",
            sizeBytes = 2_300_000_000,
            ramUsageMB = 2500,
            estimatedLatencyMs = 2500,
            deviceTier = ModelInfo.DeviceTier.HIGH,
            path = "llm/phi-3-mini-4k-instruct-q4_k_m.gguf"
        )
    )

    val TTS_MODELS = listOf(
        ModelInfo(
            id = "android_tts",
            name = "Android System TTS",
            type = ModelInfo.ModelType.TTS,
            version = "system",
            sizeBytes = 0,
            ramUsageMB = 50,
            estimatedLatencyMs = 100,
            deviceTier = ModelInfo.DeviceTier.LOW,
            path = "system"
        ),
        ModelInfo(
            id = "piper_en_lessac",
            name = "Piper EN-US Lessac Medium",
            type = ModelInfo.ModelType.TTS,
            version = "1.0",
            sizeBytes = 50_000_000,
            ramUsageMB = 100,
            estimatedLatencyMs = 300,
            deviceTier = ModelInfo.DeviceTier.MID,
            path = "tts/piper/en_US-lessac-medium.onnx"
        )
    )

    val OCR_MODELS = listOf(
        ModelInfo(
            id = "mlkit_text_recognition",
            name = "ML Kit Text Recognition",
            type = ModelInfo.ModelType.OCR,
            version = "16.0.0",
            sizeBytes = 20_000_000,
            ramUsageMB = 100,
            estimatedLatencyMs = 500,
            deviceTier = ModelInfo.DeviceTier.LOW,
            path = "bundled"
        )
    )

    fun allModels() = WAKE_WORD_MODELS + STT_MODELS + LLM_MODELS + TTS_MODELS + OCR_MODELS
}
