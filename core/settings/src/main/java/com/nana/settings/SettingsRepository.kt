package com.nana.settings

import com.nana.storage.SettingsStore
import com.nana.common.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for all user settings.
 */
class SettingsRepository(private val settingsStore: SettingsStore) {

    // Wake word
    suspend fun getWakeWordEnabled(): Boolean = settingsStore.getWakeWordEnabled()
    suspend fun setWakeWordEnabled(enabled: Boolean): Result<Unit> = settingsStore.setWakeWordEnabled(enabled)
    suspend fun getWakeWordSensitivity(): Float = settingsStore.getWakeWordSensitivity()
    suspend fun setWakeWordSensitivity(sensitivity: Float): Result<Unit> = settingsStore.setWakeWordSensitivity(sensitivity)
    suspend fun getWakeWordPhrase(): String = settingsStore.getWakeWordPhrase()
    suspend fun setWakeWordPhrase(phrase: String): Result<Unit> = settingsStore.setWakeWordPhrase(phrase)

    // TTS
    suspend fun getTTSEngine(): String = settingsStore.getTTSEngine()
    suspend fun setTTSEngine(engine: String): Result<Unit> = settingsStore.setTTSEngine(engine)
    suspend fun getTTSRate(): Float = settingsStore.getTTSRate()
    suspend fun setTTSRate(rate: Float): Result<Unit> = settingsStore.setTTSRate(rate)
    suspend fun getTTSPitch(): Float = settingsStore.getTTSPitch()
    suspend fun setTTSPitch(pitch: Float): Result<Unit> = settingsStore.setTTSPitch(pitch)

    // LLM
    suspend fun getLLMModel(): String = settingsStore.getLLMModel()
    suspend fun setLLMModel(model: String): Result<Unit> = settingsStore.setLLMModel(model)

    // Battery
    suspend fun getBatteryAwareMode(): Boolean = settingsStore.getBatteryAwareMode()
    suspend fun setBatteryAwareMode(enabled: Boolean): Result<Unit> = settingsStore.setBatteryAwareMode(enabled)

    // Accessibility
    suspend fun getAccessibilityVerbose(): Boolean = settingsStore.getAccessibilityVerbose()
    suspend fun setAccessibilityVerbose(enabled: Boolean): Result<Unit> = settingsStore.setAccessibilityVerbose(enabled)

    // Language
    suspend fun getLanguage(): String = settingsStore.getLanguage()
    suspend fun setLanguage(language: String): Result<Unit> = settingsStore.setLanguage(language)

    // Confirmations
    suspend fun getConfirmActions(): Boolean = settingsStore.getConfirmActions()
    suspend fun setConfirmActions(enabled: Boolean): Result<Unit> = settingsStore.setConfirmActions(enabled)

    data class AllSettings(
        val wakeWordEnabled: Boolean,
        val wakeWordSensitivity: Float,
        val wakeWordPhrase: String,
        val ttsEngine: String,
        val ttsRate: Float,
        val ttsPitch: Float,
        val llmModel: String,
        val batteryAwareMode: Boolean,
        val accessibilityVerbose: Boolean,
        val language: String,
        val confirmActions: Boolean
    )

    suspend fun getAllSettings(): AllSettings = AllSettings(
        wakeWordEnabled = settingsStore.getWakeWordEnabled(),
        wakeWordSensitivity = settingsStore.getWakeWordSensitivity(),
        wakeWordPhrase = settingsStore.getWakeWordPhrase(),
        ttsEngine = settingsStore.getTTSEngine(),
        ttsRate = settingsStore.getTTSRate(),
        ttsPitch = settingsStore.getTTSPitch(),
        llmModel = settingsStore.getLLMModel(),
        batteryAwareMode = settingsStore.getBatteryAwareMode(),
        accessibilityVerbose = settingsStore.getAccessibilityVerbose(),
        language = settingsStore.getLanguage(),
        confirmActions = settingsStore.getConfirmActions()
    )
}
