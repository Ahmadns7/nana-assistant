package com.nana.ui.settings.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nana.common.Result
import com.nana.settings.SettingsRepository
import com.nana.wakeword.WakeWordManager
import com.nana.stt.SttModelDownloadManager
import com.nana.stt.SttModelCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

class SettingsViewModel(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val wakeWordManager: WakeWordManager,
    private val speechModels: SttModelDownloadManager
) : ViewModel() {

    private val _settings = MutableStateFlow(
        SettingsRepository.AllSettings(
            wakeWordEnabled = false,
            wakeWordSensitivity = 0.5f,
            wakeWordPhrase = "hey nana",
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

    private val _speechModels = MutableStateFlow(speechModels.models())
    val availableSpeechModels = _speechModels.asStateFlow()
    private var downloadWatchJob: Job? = null

    init {
        viewModelScope.launch {
            _settings.value = settingsRepository.getAllSettings()
            refreshSpeechModels()
            watchDownloads()
        }
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _feedback.value = null
            if (enabled) {
                val modelId = SttModelCatalog.activeId(context)
                if (!SttModelCatalog.isInstalled(context, modelId)) {
                    _feedback.value = "Download a small speech model before enabling wake-word detection."
                    return@launch
                }
                when (val result = wakeWordManager.start(_settings.value.wakeWordPhrase, modelId)) {
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

    fun setWakeWordPhrase(phrase: String) {
        val normalized = phrase.trim().lowercase().replace(Regex("\\s+"), " ")
        if (normalized.length !in 3..40 || normalized.split(' ').size > 5) {
            _feedback.value = "Use a wake phrase between 3 and 40 characters, up to five words."
            return
        }
        updateSetting("Wake phrase") {
            wakeWordManager.stop()
            _settings.value = _settings.value.copy(wakeWordPhrase = normalized)
            val saved = settingsRepository.setWakeWordPhrase(normalized)
            if (_settings.value.wakeWordEnabled && SttModelCatalog.isInstalled(context, SttModelCatalog.activeId(context))) {
                wakeWordManager.start(normalized, SttModelCatalog.activeId(context))
            }
            saved
        }
    }

    fun downloadSpeechModel(modelId: String) {
        viewModelScope.launch {
            _feedback.value = when (val result = speechModels.enqueue(modelId)) {
                is Result.Success<*> -> "Download queued in Android Download Manager. It will use Wi-Fi."
                is Result.Failure -> result.error.message
            }
            refreshSpeechModels()
            watchDownloads()
        }
    }

    fun selectSpeechModel(modelId: String) {
        viewModelScope.launch {
            when (val result = speechModels.setActive(modelId)) {
                is Result.Success<*> -> {
                    wakeWordManager.stop()
                    _feedback.value = "${_speechModels.value.firstOrNull { it.model.id == modelId }?.model?.name ?: "Speech model"} selected."
                    if (_settings.value.wakeWordEnabled) wakeWordManager.start(_settings.value.wakeWordPhrase, modelId)
                }
                is Result.Failure -> _feedback.value = result.error.message
            }
            refreshSpeechModels()
        }
    }

    private suspend fun refreshSpeechModels() {
        when (val result = speechModels.refresh()) {
            is Result.Success<*> -> {
                @Suppress("UNCHECKED_CAST")
                _speechModels.value = result.value as List<SttModelDownloadManager.ModelState>
            }
            is Result.Failure -> {
                _speechModels.value = speechModels.models()
                _feedback.value = result.error.message
            }
        }
    }

    private fun watchDownloads() {
        if (downloadWatchJob?.isActive == true) return
        downloadWatchJob = viewModelScope.launch {
            while (_speechModels.value.any { it.queued }) {
                kotlinx.coroutines.delay(1500)
                refreshSpeechModels()
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
