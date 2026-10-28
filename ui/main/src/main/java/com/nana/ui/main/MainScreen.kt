package com.nana.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nana.ui.main.viewmodel.MainViewModel

private val Ink = Color(0xFF172633)
private val Muted = Color(0xFF687781)
private val Canvas = Color(0xFFF4F6F4)
private val Mint = Color(0xFFB9E7D6)
private val Lime = Color(0xFFD4EC9C)

@Composable
fun MainScreen(viewModel: MainViewModel, onRequestPermissions: () -> Unit, onOpenSettings: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val rows = QuickFeature.entries.toList().chunked(2)

    Scaffold(containerColor = Canvas) { insets ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(insets),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("NaNa", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink)
                        Text("YOUR ON-DEVICE ASSISTANT", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Muted, letterSpacing = 1.5.sp)
                    }
                    IconButton(onClick = viewModel::showAllApps) { Icon(Icons.Default.Apps, contentDescription = "Open all apps", tint = Ink) }
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, contentDescription = "Open settings", tint = Ink) }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("A little help, hands-free.", fontSize = 25.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                    Text("Private tools for the things you do every day.", fontSize = 14.sp, color = Muted)
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Ink)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Spacer(Modifier.size(8.dp).background(if (state.isListening) Lime else Mint, CircleShape))
                            Text(if (state.isListening) "LISTENING" else "READY WHEN YOU ARE", fontSize = 10.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold, color = Mint)
                        }
                        Text("What can I\nhelp with?", fontSize = 31.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text(state.statusText, fontSize = 13.sp, lineHeight = 19.sp, color = Color(0xFFD0D9DC), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Button(
                            onClick = viewModel::onTapToTalk,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 13.dp)
                        ) {
                            Text("Start with your voice", fontWeight = FontWeight.SemiBold)
                            Text("  ·  ", color = Muted)
                            Text("Tap to talk", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MiniAction("Play / pause", "▶", Modifier.weight(1f), viewModel::onMediaPlayPause)
                    MiniAction("Next track", "⏭", Modifier.weight(1f), viewModel::onMediaNext)
                    MiniAction("WhatsApp", "◌", Modifier.weight(1f), viewModel::onOpenWhatsApp)
                }
            }

            if (state.missingPermissions.isNotEmpty()) {
                item {
                    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1D8))) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Finish setting up NaNa", fontWeight = FontWeight.SemiBold, color = Ink)
                            Text("Allow the access you want to use: ${state.missingPermissions.joinToString()}", fontSize = 13.sp, color = Muted)
                            Button(onClick = onRequestPermissions, shape = RoundedCornerShape(14.dp)) { Text("Review access") }
                        }
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text("Everyday essentials", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                        Text("20 more things, one tap away", fontSize = 13.sp, color = Muted)
                    }
                    Text("ON DEVICE", fontSize = 9.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold, color = Muted)
                }
            }

            items(rows) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { feature ->
                        QuickFeatureCard(feature, Modifier.weight(1f)) { viewModel.performQuickFeature(feature) }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            item {
                Card(
                    onClick = onOpenSettings,
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Make NaNa yours", fontWeight = FontWeight.SemiBold, color = Ink)
                            Text("Voice, wake word, model and privacy settings", fontSize = 12.sp, color = Muted)
                        }
                        Text("→", fontSize = 23.sp, color = Ink)
                    }
                }
            }
        }
    }

    if (state.showAppPicker) {
        AlertDialog(
            onDismissRequest = viewModel::dismissAppPicker,
            title = { Text("All apps", color = Ink, fontWeight = FontWeight.SemiBold) },
            text = {
                if (state.installedApps.isEmpty()) {
                    Text("No launchable apps were found.", color = Muted)
                } else {
                    Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                        state.installedApps.forEach { app ->
                            TextButton(onClick = { viewModel.launchInstalledApp(app.packageName) }, modifier = Modifier.fillMaxWidth()) {
                                Text(app.label, modifier = Modifier.fillMaxWidth(), color = Ink)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = viewModel::dismissAppPicker) { Text("Close") } }
        )
    }
}

@Composable
private fun MiniAction(label: String, glyph: String, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier.height(78.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(glyph, fontSize = 19.sp, color = Ink)
            Text(label, fontSize = 11.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun QuickFeatureCard(feature: QuickFeature, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier.height(94.dp), shape = RoundedCornerShape(19.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(Modifier.size(38.dp).background(Mint.copy(alpha = 0.6f), CircleShape), contentAlignment = Alignment.Center) {
                Text(feature.symbol, fontSize = 17.sp, color = Ink, fontWeight = FontWeight.SemiBold)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(feature.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink, maxLines = 2, lineHeight = 16.sp)
                Text(feature.detail.uppercase(), fontSize = 9.sp, letterSpacing = 0.7.sp, color = Muted, maxLines = 1)
            }
        }
    }
}
