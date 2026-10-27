package com.nana.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nana.ui.main.viewmodel.MainViewModel
import com.nana.ui.main.theme.NaNaIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status card
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = if (uiState.isListening) Color.Green.copy(alpha = 0.1f) else androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHighest
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (uiState.isListening) "🎙️ Listening..." else "NaNa Assistant",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (uiState.isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
                
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = uiState.statusText,
                    fontSize = 16.sp,
                    maxLines = 3,
                    overflow = androidx.compose.ui.text.TextOverflow.Ellipsis
                )
                
                if (uiState.lastTranscription.isNotBlank()) {
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Heard: "${uiState.lastTranscription}"",
                        fontSize = 14.sp,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Quick actions
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

        // Permission warnings
        if (uiState.missingPermissions.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Missing Permissions", fontWeight = FontWeight.Bold, color = androidx.compose.material3.MaterialTheme.colorScheme.onErrorContainer)
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                    uiState.missingPermissions.forEach { perm ->
                        Text("• $perm", color = androidx.compose.material3.MaterialTheme.colorScheme.onErrorContainer)
                    }
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.material3.Shapes.Large,
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = if (enabled) androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer else androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHighest
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer)
            Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}
