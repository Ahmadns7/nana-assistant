package com.nana.ui.main

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nana.assistant.R
import com.nana.common.Constants
import com.nana.ui.main.viewmodel.MainViewModel
import com.nana.logging.NanaLogger

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NanaLogger.init(BuildConfig.DEBUG)
        enableEdgeToEdge()
        setContent {
            com.nana.ui.main.theme.NaNaTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            viewModel.onPermissionsResult(grantResults)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1002) { // Accessibility
            viewModel.checkAccessibilityEnabled()
        } else if (requestCode == 1003) { // Notification listener
            viewModel.checkNotificationListenerEnabled()
        } else if (requestCode == 1004) { // Overlay
            viewModel.checkOverlayPermission()
        }
    }
}
