package com.phani.voicelock.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.phani.voicelock.ui.theme.Coral
import com.phani.voicelock.ui.theme.Mint
import com.phani.voicelock.util.Permissions

@Composable
fun HomeScreen(
    vm: MainViewModel,
    onToggleLock: (Boolean) -> Unit,
    onApps: () -> Unit,
    onVoice: () -> Unit,
    onSettings: () -> Unit,
) {
    val ctx = LocalContext.current
    val enabled by vm.lockEnabled.collectAsState()
    val locked by vm.locked.collectAsState()
    val hasVoice by vm.hasVoiceprint.collectAsState()
    var problem by remember { mutableStateOf<String?>(null) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }

    fun tryToggle(on: Boolean) {
        problem = when {
            !on -> null
            !Permissions.hasCore(ctx) -> "Some permissions are missing. Open Settings > Permissions to fix."
            !hasVoice && !vm.prefs.demoMode -> "Set up your voice profile first."
            locked.isEmpty() -> "Choose at least one app to protect."
            else -> null
        }
        if (problem == null) onToggleLock(on)
    }

    AppScaffold(title = "VoiceLock") { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (enabled) "Protection is on" else "Protection is off",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (enabled) Mint else MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            "${locked.size} app${if (locked.size == 1) "" else "s"} locked",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = enabled, onCheckedChange = ::tryToggle)
                }
            }
            problem?.let { Text(it, color = Coral, style = MaterialTheme.typography.bodySmall) }

            if (vm.prefs.demoMode) {
                Text(
                    "Demo mode is on: voice checks are simulated and NOT secure.",
                    color = Coral,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            NavRow(Icons.Default.Apps, "Protected apps", "Choose which apps need your voice", onApps)
            NavRow(
                Icons.Default.Mic,
                "Voice profile",
                if (hasVoice) "Enrolled. Tap to re-record." else "Not set up yet",
                onVoice,
            )
            NavRow(Icons.Default.Settings, "Settings", "Phrase, strictness, PIN, intruder log", onSettings)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun NavRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
