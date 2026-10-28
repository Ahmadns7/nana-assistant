package com.nana.ui.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nana.settings.SettingsRepository
import com.nana.common.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _settings = MutableStateFlow(SettingsRepository.AllSettings(
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
    ))
    val settings = _settings.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val all = settingsRepository.getAllSettings()
            _settings.value = all
        }
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        _settings.value = _settings.value.copy(wakeWordEnabled = enabled)
        viewModelScope.launch {
            settingsRepository.setWakeWordEnabled(enabled)
        }
    }

    fun setWakeWordSensitivity(sensitivity: Float) {
        _settings.value = _settings.value.copy(wakeWordSensitivity = sensitivity)
        viewModelScope.launch {
            settingsRepository.setWakeWordSensitivity(sensitivity)
        }
    }

    fun setTTSEngine(engine: String) {
        _settings.value = _settings.value.copy(ttsEngine = engine)
        viewModelScope.launch {
            settingsRepository.setTTSEngine(engine)
        }
    }

    fun setTTSRate(rate: Float) {
        _settings.value = _settings.value.copy(ttsRate = rate)
        viewModelScope.launch {
            settingsRepository.setTTSRate(rate)
        }
    }

    fun setLLMModel(model: String) {
        _settings.value = _settings.value.copy(llmModel = model)
        viewModelScope.launch {
            settingsRepository.setLLMModel(model)
        }
    }

    fun setBatteryAwareMode(enabled: Boolean) {
        _settings.value = _settings.value.copy(batteryAwareMode = enabled)
        viewModelScope.launch {
            settingsRepository.setBatteryAwareMode(enabled)
        }
    }

    fun setAccessibilityVerbose(enabled: Boolean) {
        _settings.value = _settings.value.copy(accessibilityVerbose = enabled)
        viewModelScope.launch {
            settingsRepository.setAccessibilityVerbose(enabled)
        }
    }

    fun setConfirmActions(enabled: Boolean) {
        _settings.value = _settings.value.copy(confirmActions = enabled)
        viewModelScope.launch {
            settingsRepository.setConfirmActions(enabled)
        }
    }
}
