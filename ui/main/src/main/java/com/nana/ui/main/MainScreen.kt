package com.nana.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nana.ui.main.theme.NaNaIcons
import com.nana.ui.main.viewmodel.MainViewModel

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (uiState.isListening) Color.Green.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceContainerHighest
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (uiState.isListening) "Listening..." else "NaNa Assistant",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (uiState.isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = uiState.statusText,
                    fontSize = 16.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                if (uiState.lastTranscription.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Heard: \"${uiState.lastTranscription}\"",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionButton(
                icon = NaNaIcons.Mic,
                text = "Tap to Talk",
                onClick = { viewModel.onTapToTalk() },
                enabled = !uiState.isListening
            )

            QuickActionButton(
                icon = NaNaIcons.Play,
                text = "Play/Pause Media",
                onClick = { viewModel.onMediaPlayPause() }
            )

            QuickActionButton(
                icon = NaNaIcons.Next,
                text = "Next Track",
                onClick = { viewModel.onMediaNext() }
            )

            QuickActionButton(
                icon = NaNaIcons.WhatsApp,
                text = "Open WhatsApp",
                onClick = { viewModel.onOpenWhatsApp() }
            )

            QuickActionButton(
                icon = NaNaIcons.Settings,
                text = "Settings",
                onClick = { viewModel.navigateToSettings() }
            )
        }

        if (uiState.missingPermissions.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Missing Permissions", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(modifier = Modifier.height(8.dp))
                    uiState.missingPermissions.forEach { perm ->
                        Text("\u2022 $perm", color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.requestPermissions() }) {
                        Text("Grant Permissions")
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}
