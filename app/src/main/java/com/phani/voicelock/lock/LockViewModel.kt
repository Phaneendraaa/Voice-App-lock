package com.phani.voicelock.lock

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.phani.voicelock.VoiceLockApp
import com.phani.voicelock.voice.AudioRecorder
import com.phani.voicelock.voice.Challenge
import com.phani.voicelock.voice.Challenges
import com.phani.voicelock.voice.VerifyResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LockPhase { Preparing, Idle, Listening, Processing }

data class LockUiState(
    val phase: LockPhase = LockPhase.Preparing,
    val level: Float = 0f,
    val displayPhrase: String = "",
    val message: String? = null,
    val failures: Int = 0,
    val voiceAvailable: Boolean = true,
    val pinMode: Boolean = false,
    val pinError: String? = null,
    val unlocked: Boolean = false,
)

class LockViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as VoiceLockApp
    private val prefs get() = app.prefs
    private val auth = app.newAuthenticator()
    private val recorder = AudioRecorder()

    private var job: Job? = null
    private var target = ""
    private var challenge = Challenge("", "")
    private var pinFailures = 0
    private var pinLockedUntil = 0L

    private val _state = MutableStateFlow(LockUiState())
    val state: StateFlow<LockUiState> = _state

    fun start(pkg: String) {
        if (pkg == target) return
        target = pkg
        pinFailures = 0
        challenge = newChallenge()
        _state.value = LockUiState(displayPhrase = challenge.display)
        job?.cancel()
        job = viewModelScope.launch {
            val ready = auth.prepare()
            val voiceOk = ready.isSuccess && (prefs.hasVoiceprint || prefs.demoMode)
            if (!voiceOk) {
                _state.update {
                    it.copy(
                        phase = LockPhase.Idle,
                        voiceAvailable = false,
                        pinMode = true,
                        message = ready.exceptionOrNull()?.message ?: "Voice isn't set up. Use your PIN.",
                    )
                }
                return@launch
            }
            _state.update { it.copy(phase = LockPhase.Idle) }
            delay(500)
            listenOnce()
        }
    }

    /** Tap on the mic. */
    fun listen() {
        if (job?.isActive == true && _state.value.phase != LockPhase.Idle) return
        job?.cancel()
        job = viewModelScope.launch { listenOnce() }
    }

    private suspend fun listenOnce() {
        _state.update { it.copy(phase = LockPhase.Listening, message = null, level = 0f) }
        val audio = recorder.record(onLevel = { l -> _state.update { s -> s.copy(level = l) } })
        _state.update { it.copy(phase = LockPhase.Processing, level = 0f) }

        if (audio.size < MIN_SAMPLES) {
            _state.update { it.copy(phase = LockPhase.Idle, message = "I didn't hear anything. Tap the mic and try again.") }
            return
        }
        when (val r = auth.verify(audio, challenge.expected, prefs.threshold)) {
            is VerifyResult.Success -> unlock()
            is VerifyResult.Rejected -> fail(
                reason = (if (!r.wordsMatched) "Words didn't match" else "Voice didn't match") +
                    (r.voiceScore?.let { " (score ${"%.2f".format(it)})" } ?: ""),
                detail = buildString {
                    if (!r.wordsMatched) append("That wasn't the phrase. ")
                    else append("Your voice wasn't recognised. ")
                    append("Tap the mic to try again.")
                },
            )
            is VerifyResult.Failed -> _state.update {
                it.copy(phase = LockPhase.Idle, voiceAvailable = false, pinMode = true, message = r.message)
            }
        }
    }

    private fun fail(reason: String, detail: String) {
        val failures = _state.value.failures + 1
        prefs.addIntruder(target, reason)
        challenge = newChallenge()
        _state.update {
            it.copy(
                phase = LockPhase.Idle,
                failures = failures,
                displayPhrase = challenge.display,
                message = if (failures >= MAX_VOICE_FAILURES) "Too many tries. Use your PIN." else detail,
                pinMode = failures >= MAX_VOICE_FAILURES,
            )
        }
    }

    fun usePin() {
        job?.cancel()
        _state.update { it.copy(phase = LockPhase.Idle, level = 0f, pinMode = true, pinError = null) }
    }

    fun backToVoice() {
        if (!_state.value.voiceAvailable) return
        _state.update { it.copy(pinMode = false, pinError = null, failures = 0) }
        listen()
    }

    fun submitPin(pin: String) {
        val now = System.currentTimeMillis()
        if (now < pinLockedUntil) {
            _state.update { it.copy(pinError = "Too many tries. Wait ${((pinLockedUntil - now) / 1000) + 1} s.") }
            return
        }
        if (prefs.verifyPin(pin)) {
            unlock()
            return
        }
        prefs.addIntruder(target, "Wrong PIN")
        pinFailures++
        if (pinFailures >= MAX_PIN_FAILURES) {
            pinFailures = 0
            pinLockedUntil = now + PIN_LOCKOUT_MS
            _state.update { it.copy(pinError = "Too many tries. Wait 30 s.") }
        } else {
            _state.update { it.copy(pinError = "Wrong PIN") }
        }
    }

    fun onBiometricSuccess() = unlock()

    private fun unlock() {
        LockState.markUnlocked(target)
        _state.update { it.copy(unlocked = true) }
    }

    private fun newChallenge() = Challenges.create(prefs.passphrase, prefs.challengeDigits)

    override fun onCleared() {
        job?.cancel()
        auth.release()
    }

    private companion object {
        const val MIN_SAMPLES = 8_000 // 0.5 s at 16 kHz
        const val MAX_VOICE_FAILURES = 3
        const val MAX_PIN_FAILURES = 5
        const val PIN_LOCKOUT_MS = 30_000L
    }
}
