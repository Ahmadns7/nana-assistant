package com.nana.ui.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nana.common.Result
import com.nana.settings.SettingsRepository
import com.nana.wakeword.WakeWordManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val wakeWordManager: WakeWordManager
) : ViewModel() {

    private val _settings = MutableStateFlow(
        SettingsRepository.AllSettings(
            wakeWordEnabled = true,
            wakeWordSensitivity = 0.5f,
            ttsEngine = "system",
            ttsRate = 1.0f,
            ttsPitch = 1.0f,
            llmModel = "gemma_3_1b_q4",
            batteryAwareMode = false,
            accessibilityVerbose = false,
            language = "en-US",
            confirmActions = true
        )
    )
    val settings = _settings.asStateFlow()

    private val _feedback = MutableStateFlow<String?>(null)
    val feedback = _feedback.asStateFlow()

    init {
        viewModelScope.launch { _settings.value = settingsRepository.getAllSettings() }
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _feedback.value = null
            if (enabled) {
                when (val result = wakeWordManager.start()) {
                    is Result.Success<*> -> Unit
                    is Result.Failure -> {
                        _settings.value = _settings.value.copy(wakeWordEnabled = false)
                        settingsRepository.setWakeWordEnabled(false)
                        _feedback.value = result.error.message
                        return@launch
                    }
                }
            } else {
                wakeWordManager.stop()
            }
            _settings.value = _settings.value.copy(wakeWordEnabled = enabled)
            when (val result = settingsRepository.setWakeWordEnabled(enabled)) {
                is Result.Success<*> -> _feedback.value = "Wake word saved"
                is Result.Failure -> _feedback.value = result.error.message
            }
        }
    }

    fun setWakeWordSensitivity(value: Float) = updateSetting("Sensitivity") {
        val safe = value.coerceIn(0.2f, 0.9f)
        _settings.value = _settings.value.copy(wakeWordSensitivity = safe)
        settingsRepository.setWakeWordSensitivity(safe)
    }

    fun setTTSEngine(value: String) = updateSetting("Voice engine") {
        _settings.value = _settings.value.copy(ttsEngine = value)
        settingsRepository.setTTSEngine(value)
    }

    fun setTTSRate(value: Float) = updateSetting("Speech rate") {
        val safe = value.coerceIn(0.6f, 1.4f)
        _settings.value = _settings.value.copy(ttsRate = safe)
        settingsRepository.setTTSRate(safe)
    }

    fun setTTSPitch(value: Float) = updateSetting("Voice pitch") {
        val safe = value.coerceIn(0.7f, 1.4f)
        _settings.value = _settings.value.copy(ttsPitch = safe)
        settingsRepository.setTTSPitch(safe)
    }

    fun setLanguage(value: String) = updateSetting("Language") {
        _settings.value = _settings.value.copy(language = value)
        settingsRepository.setLanguage(value)
    }

    fun setLLMModel(value: String) = updateSetting("AI model") {
        _settings.value = _settings.value.copy(llmModel = value)
        settingsRepository.setLLMModel(value)
    }

    fun setBatteryAwareMode(value: Boolean) = updateSetting("Battery saver") {
        _settings.value = _settings.value.copy(batteryAwareMode = value)
        settingsRepository.setBatteryAwareMode(value)
    }

    fun setAccessibilityVerbose(value: Boolean) = updateSetting("Verbose captions") {
        _settings.value = _settings.value.copy(accessibilityVerbose = value)
        settingsRepository.setAccessibilityVerbose(value)
    }

    fun setConfirmActions(value: Boolean) = updateSetting("Action confirmations") {
        _settings.value = _settings.value.copy(confirmActions = value)
        settingsRepository.setConfirmActions(value)
    }

    private fun updateSetting(label: String, write: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _feedback.value = null
            when (val result = write()) {
                is Result.Success -> _feedback.value = "$label saved"
                is Result.Failure -> _feedback.value = result.error.message
            }
        }
    }
}
