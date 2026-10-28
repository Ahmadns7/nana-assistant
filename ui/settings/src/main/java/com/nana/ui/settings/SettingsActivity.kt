package com.nana.ui.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.nana.settings.SettingsRepository
import com.nana.storage.SettingsStore
import com.nana.ui.settings.theme.NaNaSettingsTheme
import com.nana.ui.settings.viewmodel.SettingsViewModel
import com.nana.wakeword.WakeWordManager

class SettingsActivity : ComponentActivity() {
    private val viewModel: SettingsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(
                    SettingsRepository(SettingsStore(applicationContext)),
                    WakeWordManager(applicationContext)
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NaNaSettingsTheme {
                SettingsScreen(viewModel = viewModel, onBack = ::finish)
            }
        }
    }
}
