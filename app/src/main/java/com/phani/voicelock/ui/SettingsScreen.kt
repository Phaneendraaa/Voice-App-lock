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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.phani.voicelock.ui.theme.Coral
import java.text.DateFormat
import java.util.Date

@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit, onRerecord: () -> Unit) {
    val prefs = vm.prefs
    val intruders by vm.intruders.collectAsState()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }

    var phrase by remember { mutableStateOf(prefs.passphrase) }
    var digits by remember { mutableFloatStateOf(prefs.challengeDigits.toFloat()) }
    var relock by remember { mutableFloatStateOf(prefs.relockSeconds.toFloat()) }
    var strict by remember { mutableFloatStateOf(prefs.threshold) }
    var demo by remember { mutableStateOf(prefs.demoMode) }
    var debug by remember { mutableStateOf(prefs.debugInfo) }
    var showPin by remember { mutableStateOf(false) }
    var phraseNote by remember { mutableStateOf<String?>(null) }

    AppScaffold(title = "Settings", onBack = onBack) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Section("Unlock phrase")
            OutlinedTextField(
                value = phrase,
                onValueChange = { phrase = it },
                label = { Text("Phrase") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        prefs.passphrase = phrase
                        phraseNote = "Saved. Re-record your voice so it matches the new phrase."
                    },
                    enabled = phrase.trim().split(" ").size >= 2,
                ) { Text("Save phrase") }
                TextButton(onClick = onRerecord) { Text("Re-record voice") }
            }
            phraseNote?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Text("Use 2+ words. Longer phrases are harder to guess.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Divider()
            Section("Random digits at unlock: ${digits.toInt()}")
            Slider(
                value = digits,
                onValueChange = { digits = it; prefs.challengeDigits = it.toInt() },
                valueRange = 0f..4f,
                steps = 3,
            )
            Text(
                "You'll be asked to say the phrase plus random digits. A fixed recording of your voice can't match them.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Divider()
            Section("Voice strictness: ${"%.2f".format(strict)}")
            Slider(
                value = strict,
                onValueChange = { strict = it; prefs.threshold = it },
                valueRange = 0.3f..0.9f,
            )
            Text(
                "Higher blocks more strangers but may reject you in noise. Try 0.5-0.6 first.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Divider()
            Section(if (relock.toInt() == 0) "Re-lock: immediately" else "Re-lock after ${relock.toInt()} s away")
            Slider(
                value = relock,
                onValueChange = { relock = it; prefs.relockSeconds = it.toInt() },
                valueRange = 0f..300f,
                steps = 9,
            )

            Divider()
            Section("Fallback PIN")
            OutlinedButton(onClick = { showPin = true }) { Text(if (prefs.hasPin) "Change PIN" else "Set PIN") }

            Divider()
            Section("Developer")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Demo mode", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Simulated voice check for emulators. NOT secure.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Coral,
                    )
                }
                Switch(checked = demo, onCheckedChange = { demo = it; prefs.demoMode = it })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Show debug info on failure", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Shows what was heard and the voice score. Turn off once tuned.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = debug, onCheckedChange = { debug = it; prefs.debugInfo = it })
            }

            Divider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Intruder log", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (intruders.isNotEmpty()) TextButton(onClick = vm::clearIntruders) { Text("Clear") }
            }
            if (intruders.isEmpty()) {
                Text("No failed attempts.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val fmt = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }
                intruders.take(20).forEach {
                    Column {
                        Text(vm.repo.label(it.packageName), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${fmt.format(Date(it.timestamp))} · ${it.reason}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showPin) {
        AlertDialog(
            onDismissRequest = { showPin = false },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showPin = false }) { Text("Cancel") } },
            text = { PinSetup { prefs.setPin(it); showPin = false } },
        )
    }
}

@Composable
private fun Section(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp))
}
