package com.phani.voicelock.lock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.phani.voicelock.util.Permissions
import com.phani.voicelock.voiceLockApp

/** Restarts protection after the phone reboots. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (context.voiceLockApp.prefs.lockEnabled && Permissions.hasUsageAccess(context)) {
            runCatching { LockService.start(context) }
        }
    }
}
