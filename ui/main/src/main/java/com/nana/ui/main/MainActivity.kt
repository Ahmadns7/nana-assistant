package com.nana.ui.main

import android.content.Intent
import android.os.Bundle
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
        when (requestCode) {
            1002 -> viewModel.checkAccessibilityEnabled()
            1003 -> viewModel.checkNotificationListenerEnabled()
            1004 -> viewModel.checkOverlayPermission()
        }
    }
}
