package com.phani.voicelock.lock

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.phani.voicelock.ui.MicOrb
import com.phani.voicelock.ui.theme.Coral
import com.phani.voicelock.voiceLockApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LockScreen(
    vm: LockViewModel,
    targetPackage: String,
    canBiometric: Boolean,
    onBiometric: () -> Unit,
    onUnlocked: () -> Unit,
) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    val repo = ctx.voiceLockApp.apps
    val label = remember(targetPackage) { repo.label(targetPackage) }
    val icon by produceState<ImageBitmap?>(null, targetPackage) {
        value = withContext(Dispatchers.IO) { repo.icon(targetPackage)?.toBitmap(160, 160)?.asImageBitmap() }
    }

    LaunchedEffect(state.unlocked) { if (state.unlocked) onUnlocked() }

    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            icon?.let { Image(it, contentDescription = null, modifier = Modifier.size(64.dp)) }
            Spacer(Modifier.height(12.dp))
            Text("$label is locked", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(24.dp))

            if (state.pinMode) PinPane(vm, state, canBiometric, onBiometric) else VoicePane(vm, state, canBiometric, onBiometric)
        }
    }
}

@Composable
private fun VoicePane(vm: LockViewModel, state: LockUiState, canBiometric: Boolean, onBiometric: () -> Unit) {
    if (state.phase == LockPhase.Preparing) {
        CircularProgressIndicator()
        return
    }
    Text("Say", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(
        "“${state.displayPhrase}”",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(12.dp))
    MicOrb(
        level = state.level,
        listening = state.phase == LockPhase.Listening,
        enabled = state.phase == LockPhase.Idle,
        onClick = vm::listen,
    )
    Text(
        when (state.phase) {
            LockPhase.Listening -> "Listening…"
            LockPhase.Processing -> "Checking…"
            else -> state.message ?: "Tap the mic to speak"
        },
        textAlign = TextAlign.Center,
        color = if (state.failures > 0 && state.phase == LockPhase.Idle) Coral else MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(12.dp))
    TextButton(onClick = vm::usePin) { Text("Use PIN instead") }
    if (canBiometric) TextButton(onClick = onBiometric) { Text("Use fingerprint") }
}

@Composable
private fun PinPane(vm: LockViewModel, state: LockUiState, canBiometric: Boolean, onBiometric: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    state.message?.let {
        Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
    }
    OutlinedTextField(
        value = pin,
        onValueChange = { pin = it.filter(Char::isDigit).take(8) },
        label = { Text("PIN") },
        singleLine = true,
        isError = state.pinError != null,
        supportingText = { state.pinError?.let { Text(it) } },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
    Button(
        onClick = { vm.submitPin(pin); pin = "" },
        enabled = pin.length >= 4,
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Unlock") }
    if (state.voiceAvailable) TextButton(onClick = vm::backToVoice) { Text("Try voice again") }
    if (canBiometric) TextButton(onClick = onBiometric) { Text("Use fingerprint") }
}
