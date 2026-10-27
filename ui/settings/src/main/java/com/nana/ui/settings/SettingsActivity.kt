package com.nana.ui.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nana.settings.SettingsRepository
import com.nana.ui.settings.viewmodel.SettingsViewModel
import com.nana.ui.settings.theme.NaNaSettingsTheme

class SettingsActivity : ComponentActivity() {
    private val viewModel: SettingsViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NaNaSettingsTheme {
                SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
