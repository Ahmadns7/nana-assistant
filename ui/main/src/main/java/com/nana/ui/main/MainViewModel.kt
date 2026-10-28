package com.nana.ui.main.viewmodel

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.provider.Settings
import android.os.Build
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
import com.nana.stt.WhisperModelCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
        val loadingApps: Boolean = false,
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
            val isWhisper = WhisperModelCatalog.backend(context) == WhisperModelCatalog.BACKEND_WHISPER
            val hasActiveModel = if (isWhisper) {
                WhisperModelCatalog.isInstalled(context, WhisperModelCatalog.activeModelId(context))
            } else {
                SttModelCatalog.isInstalled(context, SttModelCatalog.activeId(context))
            }
            if (!hasActiveModel) {
                setStatus("Select and download an offline speech model in Settings first.")
                return@launch
            }
            when (val result = sttManager.startListening()) {
                is Result.Success<*> -> _uiState.value = _uiState.value.copy(isListening = true, isProcessing = false, statusText = if (isWhisper) "Listening · Whisper will transcribe on this phone…" else "Listening · Vosk offline…")
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
        if (!partial && text.isNotBlank()) executeVoiceCommand(text)
    }

    fun onSpeechError(message: String) = setStatus(message)

    private fun executeVoiceCommand(spoken: String) {
        val command = spoken.trim().lowercase().replace(Regex("\\s+"), " ")
        val context = getApplication<Application>()
        when {
            command.matches(Regex("(open|launch|start) .+")) && command.substringAfter(' ') !in setOf("settings", "wifi", "wi-fi", "bluetooth", "notifications", "do not disturb", "display settings") -> {
                val target = command.substringAfter(' ')
                val matches = context.packageManager.queryIntentActivities(
                    Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
                ).mapNotNull { info ->
                    val packageName = info.activityInfo?.packageName ?: return@mapNotNull null
                    InstalledApp(info.loadLabel(context.packageManager).toString(), packageName)
                }.distinctBy { it.packageName }
                val chosen = matches.firstOrNull { it.label.equals(target, ignoreCase = true) }
                    ?: matches.firstOrNull { it.label.contains(target, ignoreCase = true) }
                if (chosen != null) launchInstalledApp(chosen.packageName)
                else setStatus("I couldn't find an installed app called $target.")
            }
            command.contains("turn on flashlight") || command.contains("turn on torch") || command == "flashlight on" ->
                viewModelScope.launch { setStatus(phoneController.setFlashlight(true).message) }
            command.contains("turn off flashlight") || command.contains("turn off torch") || command == "flashlight off" ->
                viewModelScope.launch { setStatus(phoneController.setFlashlight(false).message) }
            command == "flashlight" || command == "toggle flashlight" || command == "toggle torch" ->
                viewModelScope.launch { setStatus(phoneController.toggleFlashlight().message) }
            command.contains("volume up") || command == "louder" -> {
                val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                setStatus("Media volume raised.")
            }
            command.contains("volume down") || command == "quieter" -> {
                val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                setStatus("Media volume lowered.")
            }
            command.contains("pause music") || command == "pause" || command == "stop music" -> mediaAction { mediaController.pause() }
            command.contains("play music") || command == "play" || command == "resume music" -> mediaAction { mediaController.play() }
            command.contains("next song") || command == "next" || command.contains("skip track") -> mediaAction { mediaController.next() }
            command.contains("previous song") || command.contains("previous track") -> mediaAction { mediaController.previous() }
            command.contains("what's playing") || command.contains("what is playing") || command.contains("now playing") -> viewModelScope.launch {
                mediaController.initialize()
                setStatus(mediaController.getNowPlayingInfo()?.toSpokenString() ?: "Nothing is playing right now.")
            }
            command.contains("battery") -> setStatus(phoneController.getBatteryInfo().message)
            command.contains("storage") || command.contains("space left") -> setStatus(phoneController.getStorageInfo().message)
            command.contains("wifi") || command.contains("wi-fi") -> openSystemPanel(Settings.Panel.ACTION_WIFI, "Wi-Fi controls")
            command.contains("bluetooth") -> openSystemPanel(Settings.ACTION_BLUETOOTH_SETTINGS, "Bluetooth settings")
            command.contains("do not disturb") || command == "dnd" -> openSystemPanel(Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS, "Do Not Disturb settings")
            command.contains("brightness") || command.contains("display settings") -> openSystemPanel(Settings.ACTION_DISPLAY_SETTINGS, "Display settings")
            command == "open settings" || command == "phone settings" -> openSystemPanel(Settings.ACTION_SETTINGS, "Android settings")
            command.matches(Regex("(call|dial) [0-9+() -]{3,}")) -> {
                val number = command.substringAfter(' ').filter { it.isDigit() || it == '+' }
                val intent = Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:$number")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(intent) }
                    .onSuccess { setStatus("Review the number in the dialer before placing the call.") }
                    .onFailure { setStatus("The dialer is unavailable on this phone.") }
            }
            command.contains("message") || command.contains("text") -> openApp(Intent.ACTION_MAIN, "android.intent.category.APP_MESSAGING")
            command.contains("notification") -> openSystemPanel(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS, "Notification access")
            command.contains("timer for ") -> {
                val duration = Regex("timer for (\\d+) (second|seconds|minute|minutes|hour|hours)").find(command)
                if (duration == null) setStatus("Try a timer command such as ‘set a timer for 5 minutes.’")
                else {
                    val count = duration.groupValues[1].toLongOrNull() ?: 0L
                    val unit = duration.groupValues[2]
                    val seconds = count * when (unit) { "hour", "hours" -> 3600L; "minute", "minutes" -> 60L; else -> 1L }
                    viewModelScope.launch {
                        val result = phoneController.setTimer(seconds, "NaNa timer")
                        setStatus(if (result.isSuccess) "Timer set for ${duration.groupValues[1]} $unit." else "Couldn't set a timer on this phone.")
                    }
                }
            }
            command.contains("help") || command.contains("what can you do") -> setStatus("Try ‘open Maps,’ ‘turn on flashlight,’ ‘volume up,’ ‘pause music,’ ‘battery status,’ ‘Wi-Fi settings,’ or ‘set a timer for 5 minutes.’")
            else -> setStatus("I heard ‘$spoken’. Try ‘help’ for offline commands this phone can run.")
        }
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
        _uiState.value = _uiState.value.copy(showAppPicker = true, loadingApps = true, installedApps = emptyList())
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) {
                val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                val activities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.queryIntentActivities(launcherIntent, 0)
                }
                activities.mapNotNull { info ->
                    val packageName = info.activityInfo?.packageName ?: return@mapNotNull null
                    if (packageName == context.packageName) return@mapNotNull null
                    InstalledApp(info.loadLabel(context.packageManager).toString(), packageName)
                }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
            }
            _uiState.value = _uiState.value.copy(installedApps = apps, loadingApps = false)
        }
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
