package com.phani.voicelock

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.phani.voicelock.lock.LockActivity
import com.phani.voicelock.lock.LockService
import com.phani.voicelock.lock.LockState
import com.phani.voicelock.ui.AppNav
import com.phani.voicelock.ui.theme.VoiceLockTheme
import com.phani.voicelock.util.Permissions

class MainActivity : ComponentActivity() {
    private var needsUnlock by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = voiceLockApp.prefs
        // Make sure protection is running if it should be (e.g. the system killed the service).
        if (prefs.lockEnabled && Permissions.hasUsageAccess(this)) {
            runCatching { LockService.start(this) }
        }

        setContent {
            VoiceLockTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (!needsUnlock) AppNav()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // When protection is on, VoiceLock itself is protected too, otherwise anyone could switch it off.
        needsUnlock = voiceLockApp.prefs.lockEnabled && !LockState.isUnlocked(packageName)
        if (needsUnlock) {
            startActivity(
                Intent(this, LockActivity::class.java).putExtra(LockActivity.EXTRA_PKG, packageName),
            )
        }
    }
}
