package com.phani.voicelock.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.phani.voicelock.ui.theme.Mint
import com.phani.voicelock.util.Permissions

@Composable
fun OnboardingScreen(onContinue: () -> Unit) {
    val ctx = LocalContext.current
    var mic by remember { mutableStateOf(Permissions.hasMic(ctx)) }
    var overlay by remember { mutableStateOf(Permissions.hasOverlay(ctx)) }
    var usage by remember { mutableStateOf(Permissions.hasUsageAccess(ctx)) }
    var notif by remember { mutableStateOf(Permissions.hasNotifications(ctx)) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        mic = Permissions.hasMic(ctx)
        overlay = Permissions.hasOverlay(ctx)
        usage = Permissions.hasUsageAccess(ctx)
        notif = Permissions.hasNotifications(ctx)
    }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { mic = it }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notif = it }

    AppScaffold(title = "Set up VoiceLock") { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "VoiceLock needs a few permissions to watch which app opens and to listen for your voice. " +
                    "Voice processing stays on this phone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PermissionRow(
                "Microphone", "Listen for your unlock phrase.", mic,
            ) { micLauncher.launch(Manifest.permission.RECORD_AUDIO) }
            PermissionRow(
                "Display over other apps", "Show the lock screen on top of a protected app.", overlay,
            ) { ctx.startActivity(Permissions.overlayIntent(ctx)) }
            PermissionRow(
                "Usage access", "Detect which app is in front.", usage,
            ) { ctx.startActivity(Permissions.usageAccessIntent()) }
            if (Build.VERSION.SDK_INT >= 33) {
                PermissionRow(
                    "Notifications", "Required to keep the lock running in the background.", notif,
                ) { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
            }
            Text(
                "Tip: on Xiaomi, Oppo, Vivo, Realme and Samsung phones, also allow autostart and remove battery " +
                    "limits for VoiceLock, or the system may stop the lock.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = { ctx.startActivity(Permissions.batteryIntent()) }, modifier = Modifier.fillMaxWidth()) {
                Text("Open battery settings")
            }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onContinue,
                enabled = mic && overlay && usage,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Continue") }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PermissionRow(title: String, desc: String, granted: Boolean, onGrant: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                if (granted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (granted) Mint else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!granted) OutlinedButton(onClick = onGrant) { Text("Allow") }
        }
    }
}
