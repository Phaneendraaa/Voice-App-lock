package com.phani.voicelock

import android.app.Application
import android.content.Context
import com.phani.voicelock.data.AppRepository
import com.phani.voicelock.data.Prefs
import com.phani.voicelock.voice.MockVoiceAuthenticator
import com.phani.voicelock.voice.RealVoiceAuthenticator
import com.phani.voicelock.voice.VoiceAuthenticator

class VoiceLockApp : Application() {
    lateinit var prefs: Prefs
        private set
    lateinit var apps: AppRepository
        private set

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        apps = AppRepository(this)
    }

    /** A fresh authenticator. Caller owns it and must call release(). */
    fun newAuthenticator(): VoiceAuthenticator =
        if (prefs.demoMode) MockVoiceAuthenticator(prefs) else RealVoiceAuthenticator(this, prefs)
}

val Context.voiceLockApp: VoiceLockApp get() = applicationContext as VoiceLockApp
