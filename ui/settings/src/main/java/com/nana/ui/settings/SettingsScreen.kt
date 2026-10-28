package com.nana.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nana.ui.settings.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("NaNa Settings") },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer)
        )
        
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(16.dp))
        
        SettingsSection("Wake Word") {
            SettingRow(
                title = "Enable Wake Word",
                subtitle = "Listen for \"Hey NaNa\"",
                trailing = { Switch(checked = settings.wakeWordEnabled, onCheckedChange = { viewModel.setWakeWordEnabled(it) }) }
            )
            SettingRow(
                title = "Sensitivity",
                subtitle = "${(settings.wakeWordSensitivity * 100).toInt()}%",
                trailing = { /* Slider */ }
            )
        }
        
        SettingsSection("Voice") {
            SettingRow(
                title = "TTS Engine",
                subtitle = settings.ttsEngine,
                trailing = { /* Dropdown */ }
            )
            SettingRow(
                title = "Speech Rate",
                subtitle = "${settings.ttsRate}x",
                trailing = { /* Slider */ }
            )
        }
        
        SettingsSection("AI Model") {
            SettingRow(
                title = "LLM Model",
                subtitle = settings.llmModel,
                trailing = { /* Dropdown */ }
            )
        }
        
        SettingsSection("Behavior") {
            SettingRow(
                title = "Battery Aware Mode",
                subtitle = "Reduce activity when battery low",
                trailing = { Switch(checked = settings.batteryAwareMode, onCheckedChange = { viewModel.setBatteryAwareMode(it) }) }
            )
            SettingRow(
                title = "Confirm Actions",
                subtitle = "Ask before sending messages, making calls",
                trailing = { Switch(checked = settings.confirmActions, onCheckedChange = { viewModel.setConfirmActions(it) }) }
            )
            SettingRow(
                title = "Verbose Accessibility",
                subtitle = "Detailed spoken feedback",
                trailing = { Switch(checked = settings.accessibilityVerbose, onCheckedChange = { viewModel.setAccessibilityVerbose(it) }) }
            )
        }
        
        SettingsSection("About") {
            SettingRow(
                title = "Version",
                subtitle = "1.0.1",
                trailing = {}
            )
            SettingRow(
                title = "View Action History",
                subtitle = "See what NaNa has done",
                trailing = { /* Navigate to history */ }
            )
            SettingRow(
                title = "Manage Voice Macros",
                subtitle = "Create custom commands",
                trailing = { /* Navigate to macros */ }
            )
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = title, fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            content()
        }
    }
}

@Composable
fun SettingRow(
    title: String,
    subtitle: String = "",
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = title, fontSize = 16.sp)
            if (subtitle.isNotBlank()) {
                Text(text = subtitle, fontSize = 14.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing()
    }
}
