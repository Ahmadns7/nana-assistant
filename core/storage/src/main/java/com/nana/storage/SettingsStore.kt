package com.nana.storage

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesKeys
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.rx.preferenceDataStore
import com.nana.common.Result
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import io.reactivex.rxjava3.core.Single

/**
 * DataStore for user settings.
 */
class SettingsStore(private val context: Context) {
    private val dataStore = context.preferencesDataStore("nana_settings")

    // Keys
    private val WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
    private val WAKE_WORD_SENSITIVITY = floatPreferencesKey("wake_word_sensitivity")
    private val TTS_ENGINE = stringPreferencesKey("tts_engine")
    private val TTS_RATE = floatPreferencesKey("tts_rate")
    private val TTS_PITCH = floatPreferencesKey("tts_pitch")
    private val LLM_MODEL = stringPreferencesKey("llm_model")
    private val BATTERY_AWARE_MODE = booleanPreferencesKey("battery_aware_mode")
    private val ACCESSIBILITY_VERBOSE = booleanPreferencesKey("accessibility_verbose")
    private val LANGUAGE = stringPreferencesKey("language")
    private val CONFIRM_ACTIONS = booleanPreferencesKey("confirm_actions")

    suspend fun getWakeWordEnabled(): Boolean = dataStore.data
        .map { it[WAKE_WORD_ENABLED] ?: true }
        .first()

    suspend fun setWakeWordEnabled(enabled: Boolean): Result<Unit> = try {
        dataStore.edit { it[WAKE_WORD_ENABLED] = enabled }.await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(Result.Error("STORE_FAILED", "Failed to save setting", e))
    }

    suspend fun getWakeWordSensitivity(): Float = dataStore.data
        .map { it[WAKE_WORD_SENSITIVITY] ?: 0.5f }
        .first()

    suspend fun setWakeWordSensitivity(sensitivity: Float): Result<Unit> = try {
        dataStore.edit { it[WAKE_WORD_SENSITIVITY] = sensitivity }.await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(Result.Error("STORE_FAILED", "Failed to save setting", e))
    }

    suspend fun getTTSEngine(): String = dataStore.data
        .map { it[TTS_ENGINE] ?: "system" }
        .first()

    suspend fun setTTSEngine(engine: String): Result<Unit> = try {
        dataStore.edit { it[TTS_ENGINE] = engine }.await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(Result.Error("STORE_FAILED", "Failed to save setting", e))
    }

    suspend fun getTTSRate(): Float = dataStore.data
        .map { it[TTS_RATE] ?: 1.0f }
        .first()

    suspend fun setTTSRate(rate: Float): Result<Unit> = try {
        dataStore.edit { it[TTS_RATE] = rate }.await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(Result.Error("STORE_FAILED", "Failed to save setting", e))
    }

    suspend fun getLLMModel(): String = dataStore.data
        .map { it[LLM_MODEL] ?: "gemma_3_1b_q4" }
        .first()

    suspend fun setLLMModel(model: String): Result<Unit> = try {
        dataStore.edit { it[LLM_MODEL] = model }.await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(Result.Error("STORE_FAILED", "Failed to save setting", e))
    }

    suspend fun getBatteryAwareMode(): Boolean = dataStore.data
        .map { it[BATTERY_AWARE_MODE] ?: false }
        .first()

    suspend fun setBatteryAwareMode(enabled: Boolean): Result<Unit> = try {
        dataStore.edit { it[BATTERY_AWARE_MODE] = enabled }.await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(Result.Error("STORE_FAILED", "Failed to save setting", e))
    }

    suspend fun getAccessibilityVerbose(): Boolean = dataStore.data
        .map { it[ACCESSIBILITY_VERBOSE] ?: false }
        .first()

    suspend fun setAccessibilityVerbose(enabled: Boolean): Result<Unit> = try {
        dataStore.edit { it[ACCESSIBILITY_VERBOSE] = enabled }.await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(Result.Error("STORE_FAILED", "Failed to save setting", e))
    }

    suspend fun getLanguage(): String = dataStore.data
        .map { it[LANGUAGE] ?: "en-US" }
        .first()

    suspend fun setLanguage(language: String): Result<Unit> = try {
        dataStore.edit { it[LANGUAGE] = language }.await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(Result.Error("STORE_FAILED", "Failed to save setting", e))
    }

    suspend fun getConfirmActions(): Boolean = dataStore.data
        .map { it[CONFIRM_ACTIONS] ?: true }
        .first()

    suspend fun setConfirmActions(enabled: Boolean): Result<Unit> = try {
        dataStore.edit { it[CONFIRM_ACTIONS] = enabled }.await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(Result.Error("STORE_FAILED", "Failed to save setting", e))
    }
}
