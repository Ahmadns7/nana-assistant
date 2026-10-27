package com.nana.ui.main.viewmodel

import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nana.accessibility.NanaAccessibilityService
import com.nana.common.Result
import com.nana.logging.NanaLogger
import com.nana.media.MediaController
import com.nana.phone.PhoneController
import com.nana.wakeword.WakeWordManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MainViewModel @Inject constructor(
    private val wakeWordManager: WakeWordManager,
    private val mediaController: MediaController,
    private val phoneController: PhoneController,
    private val context: android.content.Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    data class UiState(
        val isListening: Boolean = false,
        val isProcessing: Boolean = false,
        val statusText: String = "Ready. Say "Hey NaNa" or tap the mic.",
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
        if (context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            missing.add("Record Audio")
        }
        if (context.checkSelfPermission(android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            missing.add("Phone Calls")
        }
        if (context.checkSelfPermission(android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            missing.add("SMS")
        }
        _uiState.value = _uiState.value.copy(missingPermissions = missing)
    }

    private fun checkServices() {
        viewModelScope.launch {
            val a11yEnabled = context.isAccessibilityServiceEnabled(NanaAccessibilityService::class.java)
            val notifEnabled = context.isNotificationListenerEnabled(com.nana.notifications.NotificationListener::class.java)
            val overlayEnabled = context.isOverlayPermissionGranted()
            
            val missing = mutableListOf<String>()
            if (!a11yEnabled) missing.add("Accessibility Service")
            if (!notifEnabled) missing.add("Notification Access")
            if (!overlayEnabled) missing.add("Display Over Apps")
            
            _uiState.value = _uiState.value.copy(missingPermissions = _uiState.value.missingPermissions + missing)
        }
    }

    private fun startWakeWord() {
        viewModelScope.launch {
            wakeWordManager.start().onSuccess {
                wakeWordManager.setDetectionCallback { keyword ->
                    _uiState.value = _uiState.value.copy(
                        isListening = true,
                        statusText = "Wake word detected! Listening for command..."
                    )
                    // Start STT
                }
            }
        }
    }

    fun onPermissionsResult(grantResults: IntArray) {
        checkPermissions()
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

    fun requestPermissions() {
        val permissions = mutableListOf<String>()
        if (context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(android.Manifest.permission.RECORD_AUDIO)
        }
        if (context.checkSelfPermission(android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(android.Manifest.permission.CALL_PHONE)
        }
        if (context.checkSelfPermission(android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(android.Manifest.permission.SEND_SMS)
        }
        
        if (permissions.isNotEmpty()) {
            (context as androidx.activity.ComponentActivity).requestPermissions(
                permissions.toTypedArray(), 1001
            )
        }
        
        // Open accessibility settings
        val a11yIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        context.startActivity(a11yIntent)
    }

    fun onTapToTalk() {
        _uiState.value = _uiState.value.copy(
            isListening = true,
            isProcessing = true,
            statusText = "Listening..."
        )
        // TODO: Start STT service
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
        val intent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent?.let { context.startActivity(it) }
    }

    fun navigateToSettings() {
        // Navigation handled by Navigation component
    }

    override fun onCleared() {
        wakeWordManager.stop()
        super.onCleared()
    }
}
