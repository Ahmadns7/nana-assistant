package com.nana.ui.main.viewmodel

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nana.accessibility.NanaAccessibilityService
import com.nana.common.Result
import com.nana.common.isAccessibilityServiceEnabled
import com.nana.common.isNotificationListenerEnabled
import com.nana.common.isOverlayPermissionGranted
import com.nana.media.MediaController
import com.nana.phone.PhoneController
import com.nana.settings.SettingsRepository
import com.nana.ui.main.QuickFeature
import com.nana.wakeword.WakeWordManager
import com.nana.stt.SttModelCatalog
import com.nana.stt.STTManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application,
    private val wakeWordManager: WakeWordManager,
    private val mediaController: MediaController,
    private val phoneController: PhoneController,
    private val settingsRepository: SettingsRepository,
    private val sttManager: STTManager
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    data class UiState(
        val isListening: Boolean = false,
        val isProcessing: Boolean = false,
        val statusText: String = "Ready when you are.",
        val lastTranscription: String = "",
        val missingPermissions: List<String> = emptyList(),
        val showAppPicker: Boolean = false,
        val installedApps: List<InstalledApp> = emptyList()
    )

    data class InstalledApp(val label: String, val packageName: String)

    init {
        refreshAccess()
        viewModelScope.launch {
            mediaController.initialize()
            syncWakeWord()
        }
    }

    fun refreshAccess() {
        val context = getApplication<Application>()
        val missing = mutableListOf<String>()
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            missing.add("Microphone")
        }
        if (context.checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            missing.add("Phone calls")
        }
        if (context.checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            missing.add("SMS")
        }
        if (!context.isAccessibilityServiceEnabled(NanaAccessibilityService::class.java)) {
            missing.add("Accessibility")
        }
        if (!context.isNotificationListenerEnabled(com.nana.notifications.NotificationListener::class.java)) {
            missing.add("Notification access")
        }
        if (!context.isOverlayPermissionGranted()) {
            missing.add("Display over apps")
        }
        _uiState.value = _uiState.value.copy(missingPermissions = missing)
    }

    private suspend fun syncWakeWord() {
        if (!settingsRepository.getWakeWordEnabled()) {
            wakeWordManager.stop()
            return
        }
        if (getApplication<Application>().checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            _uiState.value = _uiState.value.copy(statusText = "Allow microphone access to enable wake word detection.")
            return
        }

        val modelId = SttModelCatalog.activeId(getApplication<Application>())
        if (!SttModelCatalog.isInstalled(getApplication<Application>(), modelId)) {
            _uiState.value = _uiState.value.copy(statusText = "Download a small offline speech model in Settings to enable voice features.")
            return
        }
        val phrase = settingsRepository.getWakeWordPhrase()
        wakeWordManager.start(phrase, modelId)
            .onSuccess {
                wakeWordManager.setDetectionCallback {
                    _uiState.value = _uiState.value.copy(
                        isListening = true,
                        statusText = "Wake word detected. Listening for your command…"
                    )
                }
            }
            .onFailure { error -> setStatus(error.message) }
    }

    fun onPermissionsResult() {
        refreshAccess()
        viewModelScope.launch { syncWakeWord() }
    }

    fun onResume() {
        refreshAccess()
    }

    fun openAccessibilitySettings() = openSystemPanel(Settings.ACTION_ACCESSIBILITY_SETTINGS, "Accessibility settings")
    fun openNotificationSettings() = openSystemPanel(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS, "Notification access settings")
    fun openAppSettings() = openSystemPanel(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "App permissions", "package:${getApplication<Application>().packageName}")

    fun onTapToTalk() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val modelId = SttModelCatalog.activeId(context)
            if (!SttModelCatalog.isInstalled(context, modelId)) {
                setStatus("Download a small offline speech model in Settings to use voice input.")
                return@launch
            }
            when (val result = sttManager.startListening()) {
                is Result.Success<*> -> _uiState.value = _uiState.value.copy(isListening = true, isProcessing = false, statusText = "Listening on this device…")
                is Result.Failure -> setStatus(result.error.message)
            }
        }
    }

    fun onSpeechUpdate(text: String, partial: Boolean) {
        _uiState.value = _uiState.value.copy(
            isListening = partial,
            isProcessing = partial,
            lastTranscription = text,
            statusText = if (partial) text else if (text.isBlank()) "No speech was recognized." else "Heard: $text"
        )
    }

    fun onMediaPlayPause() {
        mediaAction {
            if (mediaController.isPlaying()) mediaController.pause() else mediaController.play()
        }
    }

    fun onMediaNext() = mediaAction { mediaController.next() }

    fun onOpenWhatsApp() {
        val context = getApplication<Application>()
        val intent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
        if (intent == null) setStatus("WhatsApp is not installed.")
        else runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { setStatus("Could not open WhatsApp.") }
    }

    fun showAllApps() {
        val context = getApplication<Application>()
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = context.packageManager.queryIntentActivities(launcherIntent, 0)
            .mapNotNull { info ->
                val packageName = info.activityInfo?.packageName ?: return@mapNotNull null
                if (packageName == context.packageName) return@mapNotNull null
                InstalledApp(info.loadLabel(context.packageManager).toString(), packageName)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
        _uiState.value = _uiState.value.copy(showAppPicker = true, installedApps = apps)
    }

    fun dismissAppPicker() {
        _uiState.value = _uiState.value.copy(showAppPicker = false)
    }

    fun launchInstalledApp(packageName: String) {
        val launchIntent = getApplication<Application>().packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent == null) {
            setStatus("That app can no longer be opened.")
        } else {
            runCatching { getApplication<Application>().startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                .onSuccess { setStatus("Opening app.") }
                .onFailure { setStatus("Could not open that app.") }
        }
        dismissAppPicker()
    }

    fun performQuickFeature(feature: QuickFeature) {
        when (feature) {
            QuickFeature.PREVIOUS_TRACK -> mediaAction { mediaController.previous() }
            QuickFeature.VOLUME_UP -> mediaAction { mediaController.adjustVolume(AudioManager.ADJUST_RAISE) }
            QuickFeature.VOLUME_DOWN -> mediaAction { mediaController.adjustVolume(AudioManager.ADJUST_LOWER) }
            QuickFeature.NOW_PLAYING -> viewModelScope.launch {
                mediaController.initialize()
                setStatus(mediaController.getNowPlayingInfo()?.toSpokenString() ?: "Nothing is playing right now.")
            }
            QuickFeature.FLASHLIGHT -> viewModelScope.launch {
                setStatus(phoneController.toggleFlashlight().message)
            }
            QuickFeature.BATTERY -> setStatus(phoneController.getBatteryInfo().message)
            QuickFeature.STORAGE -> setStatus(phoneController.getStorageInfo().message)
            QuickFeature.PHONE -> openApp(Intent.ACTION_DIAL, "android.intent.category.APP_DIALER")
            QuickFeature.MESSAGES -> openApp(Intent.ACTION_MAIN, "android.intent.category.APP_MESSAGING")
            QuickFeature.CAMERA -> openApp(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
            QuickFeature.MAPS -> openApp(Intent.ACTION_MAIN, "android.intent.category.APP_MAPS")
            QuickFeature.CLOCK -> openApp(android.provider.AlarmClock.ACTION_SHOW_ALARMS)
            QuickFeature.CALENDAR -> openApp(Intent.ACTION_MAIN, "android.intent.category.APP_CALENDAR")
            QuickFeature.CONTACTS -> openApp(Intent.ACTION_MAIN, "android.intent.category.APP_CONTACTS")
            QuickFeature.CALCULATOR -> openApp(Intent.ACTION_MAIN, "android.intent.category.APP_CALCULATOR")
            QuickFeature.BROWSER -> openApp(Intent.ACTION_MAIN, "android.intent.category.APP_BROWSER")
            QuickFeature.WIFI -> openSystemPanel(Settings.Panel.ACTION_WIFI, "Wi-Fi settings")
            QuickFeature.BLUETOOTH -> openSystemPanel(Settings.ACTION_BLUETOOTH_SETTINGS, "Bluetooth settings")
            QuickFeature.DND -> openSystemPanel(Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS, "Do Not Disturb settings")
            QuickFeature.DISPLAY -> openSystemPanel(Settings.ACTION_DISPLAY_SETTINGS, "Display settings")
        }
    }

    private fun mediaAction(action: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            mediaController.initialize()
            action().fold(
                onSuccess = { setStatus("Media command sent.") },
                onFailure = { setStatus(it.message) }
            )
        }
    }

    private fun openApp(action: String, category: String? = null) {
        val intent = Intent(action).apply {
            category?.let(::addCategory)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { getApplication<Application>().startActivity(intent) }
            .onSuccess { setStatus("Opening ${category?.substringAfterLast('.')?.lowercase() ?: "app"}.") }
            .onFailure { setStatus("That app is not available on this phone.") }
    }

    private fun openSystemPanel(action: String, label: String, data: String? = null) {
        val intent = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).apply { data?.let { this.data = android.net.Uri.parse(it) } }
        runCatching { getApplication<Application>().startActivity(intent) }
            .onSuccess { setStatus("Opening $label.") }
            .onFailure { setStatus("$label are not available on this phone.") }
    }

    fun checkAccessibilityEnabled() = refreshAccess()
    fun checkNotificationListenerEnabled() = refreshAccess()
    fun checkOverlayPermission() = refreshAccess()

    private fun setStatus(message: String) {
        _uiState.value = _uiState.value.copy(isListening = false, isProcessing = false, statusText = message)
    }

    override fun onCleared() {
        viewModelScope.launch { wakeWordManager.stop() }
        mediaController.release()
        super.onCleared()
    }
}
