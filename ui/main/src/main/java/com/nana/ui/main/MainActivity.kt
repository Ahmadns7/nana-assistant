package com.nana.ui.main

import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.nana.media.MediaController
import com.nana.phone.PhoneController
import com.nana.ui.main.theme.NaNaTheme
import com.nana.ui.main.viewmodel.MainViewModel
import com.nana.wakeword.WakeWordManager

class MainActivity : ComponentActivity() {
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
                    PhoneController(applicationContext)
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NaNaTheme {
                MainScreen(viewModel = viewModel, onRequestPermissions = ::requestAppPermissions)
            }
        }
    }

    private fun requestAppPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS
        ).filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }

        if (permissions.isEmpty()) {
            viewModel.openAccessibilitySettings()
        } else {
            permissionLauncher.launch(permissions.toTypedArray())
        }
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
