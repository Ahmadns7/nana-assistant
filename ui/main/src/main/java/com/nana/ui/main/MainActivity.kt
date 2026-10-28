package com.nana.ui.main

import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.nana.media.MediaController
import com.nana.phone.PhoneController
import com.nana.settings.SettingsRepository
import com.nana.storage.SettingsStore
import com.nana.ui.main.theme.NaNaTheme
import com.nana.ui.main.viewmodel.MainViewModel
import com.nana.ui.settings.SettingsActivity
import com.nana.wakeword.WakeWordManager
import com.nana.stt.STTManager
import com.nana.common.Constants

class MainActivity : ComponentActivity() {
    private val speechResultReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.getStringExtra("error")?.takeIf(String::isNotBlank)?.let {
                viewModel.onSpeechError(it)
                return
            }
            viewModel.onSpeechUpdate(
                intent?.getStringExtra("text").orEmpty(),
                intent?.getBooleanExtra("partial", false) ?: false
            )
        }
    }
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.onPermissionsResult()
    }

    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(
                    application,
                    WakeWordManager(applicationContext),
                    MediaController(applicationContext),
                    PhoneController(applicationContext),
                    SettingsRepository(SettingsStore(applicationContext)),
                    STTManager(applicationContext)
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NaNaTheme {
                MainScreen(viewModel = viewModel, onRequestPermissions = ::requestAppPermissions, onOpenSettings = ::openSettings)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(this, speechResultReceiver, IntentFilter(Constants.ACTION_STT_RESULT), ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onStop() {
        runCatching { unregisterReceiver(speechResultReceiver) }
        super.onStop()
    }

    private fun requestAppPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO
        ).filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }

        if (permissions.isEmpty()) {
            viewModel.openAccessibilitySettings()
        } else {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            1002 -> viewModel.checkAccessibilityEnabled()
            1003 -> viewModel.checkNotificationListenerEnabled()
            1004 -> viewModel.checkOverlayPermission()
        }
    }
}
