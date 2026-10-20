package com.phani.voicelock.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phani.voicelock.ui.theme.Coral
import com.phani.voicelock.ui.theme.Mint
import com.phani.voicelock.util.Permissions
import com.phani.voicelock.voiceLockApp

@Composable
fun EnrollScreen(onBack: () -> Unit, onFinished: () -> Unit, vm: EnrollViewModel = viewModel()) {
    val ctx = LocalContext.current
    val prefs = ctx.voiceLockApp.prefs
    val state by vm.state.collectAsState()
    var hasPin by remember { mutableStateOf(prefs.hasPin) }
    var hasMic by remember { mutableStateOf(Permissions.hasMic(ctx)) }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasMic = it }

    AppScaffold(title = "Voice profile", onBack = onBack) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when {
                !hasPin -> PinSetup { prefs.setPin(it); hasPin = true }

                !hasMic -> {
                    Text("Microphone access is needed to record your voice.", textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) }) { Text("Allow microphone") }
                }

                state.phase == Phase.Preparing -> {
                    Spacer(Modifier.height(48.dp))
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("Starting voice engine…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                state.phase == Phase.NeedsModels -> {
                    Spacer(Modifier.height(32.dp))
                    Text("One-time download", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "VoiceLock needs two small voice models (about 60 MB). After this it works fully offline, " +
                            "and your voice never leaves the phone.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    state.error?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(it, color = Coral, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = vm::downloadModels, modifier = Modifier.fillMaxWidth()) { Text("Download models") }
                }

                state.phase == Phase.Downloading -> {
                    Spacer(Modifier.height(48.dp))
                    LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    Text(state.message ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                else -> {
                    Text(
                        "Tap the mic and say your phrase clearly, three times in a quiet place.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "“${vm.passphrase}”",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    MicOrb(
                        level = state.level,
                        listening = state.phase == Phase.Listening,
                        enabled = state.phase == Phase.Idle,
                        onClick = vm::record,
                    )
                    Spacer(Modifier.height(8.dp))
                    ProgressDots(done = state.samplesDone, total = EnrollViewModel.REQUIRED_SAMPLES)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        when (state.phase) {
                            Phase.Listening -> "Listening…"
                            Phase.Processing -> "Checking…"
                            else -> state.message ?: ""
                        },
                        textAlign = TextAlign.Center,
                        color = if (state.phase == Phase.Done) Mint else MaterialTheme.colorScheme.onSurface,
                    )
                    state.error?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = Coral, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
                    }
                    if (state.phase == Phase.Done) {
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = onFinished, modifier = Modifier.fillMaxWidth()) { Text("Done") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressDots(done: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(total) { i ->
            Spacer(
                Modifier
                    .size(12.dp)
                    .background(
                        if (i < done) Mint else MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape,
                    ),
            )
        }
    }
}
