package com.nana.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nana.ui.settings.viewmodel.SettingsViewModel

private val Ink = Color(0xFF172633)
private val Muted = Color(0xFF687781)
private val Canvas = Color(0xFFF4F6F4)
private val Mint = Color(0xFFB9E7D6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val feedback by viewModel.feedback.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val openSystemSettings: (String) -> Unit = { action ->
        runCatching { context.startActivity(Intent(action)) }
    }

    Scaffold(
        containerColor = Canvas,
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold, color = Ink) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Ink)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Canvas)
            )
        }
    ) { insets ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(insets),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(bottom = 3.dp)) {
                    Text("Make NaNa yours.", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                    Text("Choose how your private assistant listens and responds.", fontSize = 14.sp, color = Muted)
                }
            }

            if (feedback != null) {
                item {
                    Card(shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = Mint.copy(alpha = 0.55f))) {
                        Text(feedback.orEmpty(), Modifier.fillMaxWidth().padding(13.dp), fontSize = 13.sp, color = Ink)
                    }
                }
            }

            item {
                SettingsGroup(title = "Wake word", detail = "Hands-free access") {
                    ToggleRow(
                        title = "Hey NaNa detection",
                        subtitle = "Keep listening while NaNa is enabled",
                        checked = settings.wakeWordEnabled,
                        onCheckedChange = viewModel::setWakeWordEnabled
                    )
                    SliderRow(
                        title = "Wake word sensitivity",
                        valueLabel = "${(settings.wakeWordSensitivity * 100).toInt()}%",
                        value = settings.wakeWordSensitivity,
                        range = 0.2f..0.9f,
                        onValueChange = viewModel::setWakeWordSensitivity
                    )
                    Text("Wake word detection needs microphone access, a Hey NaNa model and its Picovoice key.", fontSize = 12.sp, color = Muted)
                }
            }

            item {
                SettingsGroup(title = "Voice", detail = "Sound like you") {
                    SelectorRow(
                        title = "Speech engine",
                        current = settings.ttsEngine.displayEngine(),
                        options = listOf("System voice" to "system", "Piper offline voice" to "piper"),
                        onSelect = viewModel::setTTSEngine
                    )
                    SliderRow(
                        title = "Speech speed",
                        valueLabel = "${"%.1f".format(settings.ttsRate)}×",
                        value = settings.ttsRate,
                        range = 0.6f..1.4f,
                        onValueChange = viewModel::setTTSRate
                    )
                    SliderRow(
                        title = "Voice pitch",
                        valueLabel = "${"%.1f".format(settings.ttsPitch)}×",
                        value = settings.ttsPitch,
                        range = 0.7f..1.4f,
                        onValueChange = viewModel::setTTSPitch
                    )
                    SelectorRow(
                        title = "Language",
                        current = settings.language,
                        options = listOf("English (US)" to "en-US", "English (UK)" to "en-GB", "Español" to "es-ES", "Français" to "fr-FR"),
                        onSelect = viewModel::setLanguage
                    )
                }
            }

            item {
                SettingsGroup(title = "On-device intelligence", detail = "Models stay on your phone") {
                    SelectorRow(
                        title = "Assistant model",
                        current = settings.llmModel.displayModel(),
                        options = listOf(
                            "Gemma 3 · 1B" to "gemma_3_1b_q4",
                            "Gemma · 2B" to "gemma_2b_q4",
                            "Phi-3 Mini" to "phi_3_mini_q4"
                        ),
                        onSelect = viewModel::setLLMModel
                    )
                    ToggleRow(
                        title = "Battery-aware mode",
                        subtitle = "Reduce background activity when power is low",
                        checked = settings.batteryAwareMode,
                        onCheckedChange = viewModel::setBatteryAwareMode
                    )
                }
            }

            item {
                SettingsGroup(title = "Control & privacy", detail = "Keep important actions clear") {
                    ToggleRow(
                        title = "Confirm sensitive actions",
                        subtitle = "Ask before sending messages or making calls",
                        checked = settings.confirmActions,
                        onCheckedChange = viewModel::setConfirmActions
                    )
                    ToggleRow(
                        title = "Verbose accessibility captions",
                        subtitle = "Show more detailed spoken feedback",
                        checked = settings.accessibilityVerbose,
                        onCheckedChange = viewModel::setAccessibilityVerbose
                    )
                }
            }

            item {
                SettingsGroup(title = "Finish setup", detail = "Grant only what you plan to use") {
                    SetupButton("Accessibility service", "Screen reading and controls") {
                        openSystemSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    }
                    SetupButton("Notification access", "Read and manage notifications") {
                        openSystemSettings(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    }
                    SetupButton("App permissions", "Microphone, phone and messages") {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = android.net.Uri.parse("package:${context.packageName}")
                        }
                        runCatching { context.startActivity(intent) }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.size(8.dp).background(Mint, CircleShape))
                    Text("  NaNa Assistant  ·  Version 1.0.2", fontSize = 12.sp, color = Muted)
                }
            }
        }
    }
}

@Composable
private fun SettingsGroup(title: String, detail: String, content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(23.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                Text(detail, fontSize = 12.sp, color = Muted)
            }
            content()
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Ink)
            Text(subtitle, fontSize = 12.sp, color = Muted)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SliderRow(title: String, valueLabel: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Ink)
            Text(valueLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Muted)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = range)
    }
}

@Composable
private fun SelectorRow(title: String, current: String, options: List<Pair<String, String>>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Ink)
        Column(horizontalAlignment = Alignment.End) {
            TextButton(onClick = { expanded = true }) { Text(current, color = MaterialTheme.colorScheme.primary) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (label, value) ->
                    DropdownMenuItem(text = { Text(label) }, onClick = { onSelect(value); expanded = false })
                }
            }
        }
    }
}

@Composable
private fun SetupButton(title: String, subtitle: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp), contentPadding = PaddingValues(horizontal = 15.dp, vertical = 12.dp)) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
        }
    }
}

private fun String.displayEngine(): String = when (this) {
    "piper" -> "Piper offline voice"
    else -> "System voice"
}

private fun String.displayModel(): String = when (this) {
    "gemma_2b_q4" -> "Gemma · 2B"
    "phi_3_mini_q4" -> "Phi-3 Mini"
    else -> "Gemma 3 · 1B"
}
