package com.phani.voicelock.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.phani.voicelock.VoiceLockApp
import com.phani.voicelock.data.AppInfo
import com.phani.voicelock.data.IntruderEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as VoiceLockApp
    val prefs get() = app.prefs
    val repo get() = app.apps

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps

    private val _locked = MutableStateFlow(prefs.lockedPackages)
    val locked: StateFlow<Set<String>> = _locked

    private val _lockEnabled = MutableStateFlow(prefs.lockEnabled)
    val lockEnabled: StateFlow<Boolean> = _lockEnabled

    private val _hasVoiceprint = MutableStateFlow(prefs.hasVoiceprint)
    val hasVoiceprint: StateFlow<Boolean> = _hasVoiceprint

    private val _intruders = MutableStateFlow<List<IntruderEntry>>(emptyList())
    val intruders: StateFlow<List<IntruderEntry>> = _intruders

    init {
        viewModelScope.launch { _apps.value = repo.loadLaunchable() }
        refresh()
    }

    /** Re-read state that other screens or the lock service may have changed. */
    fun refresh() {
        _locked.value = prefs.lockedPackages
        _lockEnabled.value = prefs.lockEnabled
        _hasVoiceprint.value = prefs.hasVoiceprint
        _intruders.value = prefs.intruderLog()
    }

    fun setAppLocked(pkg: String, on: Boolean) {
        prefs.setLocked(pkg, on)
        _locked.value = prefs.lockedPackages
    }

    fun setLockEnabled(on: Boolean) {
        prefs.lockEnabled = on
        _lockEnabled.value = on
    }

    fun clearIntruders() {
        prefs.clearIntruders()
        _intruders.value = emptyList()
    }
}
