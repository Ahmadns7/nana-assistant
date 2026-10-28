package com.nana.ui.main.viewmodel

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nana.accessibility.NanaAccessibilityService
import com.nana.common.isAccessibilityServiceEnabled
import com.nana.common.isNotificationListenerEnabled
import com.nana.common.isOverlayPermissionGranted
import com.nana.media.MediaController
import com.nana.phone.PhoneController
import com.nana.wakeword.WakeWordManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application,
    private val wakeWordManager: WakeWordManager,
    private val mediaController: MediaController,
    private val phoneController: PhoneController
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    data class UiState(
        val isListening: Boolean = false,
        val isProcessing: Boolean = false,
        val statusText: String = "Ready. Say \"Hey NaNa\" or tap the mic.",
        val lastTranscription: String = "",
        val missingPermissions: List<String> = emptyList()
    )

    init {
        checkPermissions()
        checkServices()
        startWakeWord()
    }

    private fun checkPermissions() {
        val missing = mutableListOf<String>()
        if (getApplication<Application>().checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            missing.add("Record Audio")
        }
        if (getApplication<Application>().checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            missing.add("Phone Calls")
        }
        if (getApplication<Application>().checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            missing.add("SMS")
        }
        _uiState.value = _uiState.value.copy(missingPermissions = missing)
    }

    private fun checkServices() {
        val context = getApplication<Application>()
        val a11yEnabled = context.isAccessibilityServiceEnabled(NanaAccessibilityService::class.java)
        val notifEnabled = context.isNotificationListenerEnabled(com.nana.notifications.NotificationListener::class.java)
        val overlayEnabled = context.isOverlayPermissionGranted()

        val missing = mutableListOf<String>()
        if (!a11yEnabled) missing.add("Accessibility Service")
        if (!notifEnabled) missing.add("Notification Access")
        if (!overlayEnabled) missing.add("Display Over Apps")

        _uiState.value = _uiState.value.copy(missingPermissions = _uiState.value.missingPermissions + missing)
    }

    private fun startWakeWord() {
        if (getApplication<Application>().checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            _uiState.value = _uiState.value.copy(
                statusText = "Allow microphone access to enable wake word detection."
            )
            return
        }

        viewModelScope.launch {
            wakeWordManager.start()
                .onSuccess {
                    wakeWordManager.setDetectionCallback {
                        _uiState.value = _uiState.value.copy(
                            isListening = true,
                            statusText = "Wake word detected! Listening for command..."
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isListening = false,
                        statusText = error.message
                    )
                }
        }
    }

    fun onPermissionsResult() {
        checkPermissions()
        startWakeWord()
    }

    fun checkAccessibilityEnabled() {
        checkServices()
    }

    fun checkNotificationListenerEnabled() {
        checkServices()
    }

    fun checkOverlayPermission() {
        checkServices()
    }

    fun openAccessibilitySettings() {
        val a11yIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        getApplication<Application>().startActivity(a11yIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun onTapToTalk() {
        _uiState.value = _uiState.value.copy(
            isListening = true,
            isProcessing = true,
            statusText = "Listening..."
        )
    }

    fun onMediaPlayPause() {
        viewModelScope.launch {
            val state = mediaController.getPlaybackState()
            if (state == android.media.session.PlaybackState.STATE_PLAYING) {
                mediaController.pause()
            } else {
                mediaController.play()
            }
        }
    }

    fun onMediaNext() {
        viewModelScope.launch {
            mediaController.next()
        }
    }

    fun onOpenWhatsApp() {
        val intent = getApplication<Application>().packageManager.getLaunchIntentForPackage("com.whatsapp")
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent?.let { getApplication<Application>().startActivity(it) }
    }

    fun navigateToSettings() {
    }

    override fun onCleared() {
        viewModelScope.launch { wakeWordManager.stop() }
        super.onCleared()
    }
}
