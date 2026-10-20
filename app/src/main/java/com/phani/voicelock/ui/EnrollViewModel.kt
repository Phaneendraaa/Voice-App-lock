package com.phani.voicelock.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.phani.voicelock.VoiceLockApp
import com.phani.voicelock.voice.AudioRecorder
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Phase { Preparing, Idle, Listening, Processing, Done }

data class EnrollState(
    val phase: Phase = Phase.Preparing,
    val samplesDone: Int = 0,
    val level: Float = 0f,
    val message: String? = null,
    val error: String? = null,
)

class EnrollViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as VoiceLockApp
    private val auth = app.newAuthenticator()
    private val recorder = AudioRecorder()
    private var job: Job? = null

    private val _state = MutableStateFlow(EnrollState())
    val state: StateFlow<EnrollState> = _state

    val passphrase: String get() = app.prefs.passphrase

    init {
        auth.resetEnrollment()
        viewModelScope.launch {
            auth.prepare().fold(
                onSuccess = { _state.update { it.copy(phase = Phase.Idle) } },
                onFailure = { e ->
                    _state.update { it.copy(phase = Phase.Idle, error = e.message ?: "Voice engine could not start.") }
                },
            )
        }
    }

    fun record() {
        if (job?.isActive == true || _state.value.phase == Phase.Done) return
        job = viewModelScope.launch {
            _state.update { it.copy(phase = Phase.Listening, message = null, level = 0f) }
            val audio = recorder.record(onLevel = { l -> _state.update { s -> s.copy(level = l) } })
            _state.update { it.copy(phase = Phase.Processing, level = 0f) }

            val result = auth.enrollSample(audio, passphrase)
            if (!result.ok) {
                _state.update { it.copy(phase = Phase.Idle, message = result.message ?: "Try again.") }
                return@launch
            }
            val done = _state.value.samplesDone + 1
            if (done >= REQUIRED_SAMPLES) {
                val saved = auth.finishEnrollment()
                _state.update {
                    if (saved) it.copy(phase = Phase.Done, samplesDone = done, message = "Voice saved.")
                    else it.copy(phase = Phase.Idle, samplesDone = 0, message = "Could not save your voice. Start again.")
                }
            } else {
                _state.update { it.copy(phase = Phase.Idle, samplesDone = done, message = "Got it. Say it once more.") }
            }
        }
    }

    fun cancel() {
        job?.cancel()
        _state.update { if (it.phase == Phase.Listening || it.phase == Phase.Processing) it.copy(phase = Phase.Idle, level = 0f) else it }
    }

    override fun onCleared() {
        job?.cancel()
        auth.release()
    }

    companion object {
        const val REQUIRED_SAMPLES = 3
    }
}
