package com.phani.voicelock.lock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.phani.voicelock.MainActivity
import com.phani.voicelock.VoiceLockApp
import com.phani.voicelock.util.Permissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground service that watches which app is in front (UsageStats) and puts [LockActivity]
 * on top when a protected app opens.
 */
class LockService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var anchor: View? = null

    private val screenOff = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            LockState.clearAll() // lock everything again when the screen turns off
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        ContextCompat.registerReceiver(
            this, screenOff, IntentFilter(Intent.ACTION_SCREEN_OFF), ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        addAnchor()
        scope.launch { watch() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        scope.cancel()
        runCatching { unregisterReceiver(screenOff) }
        removeAnchor()
        super.onDestroy()
    }

    private suspend fun watch() {
        val usage = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val prefs = (application as VoiceLockApp).prefs
        var since = System.currentTimeMillis() - 10 * 60_000L
        var foreground: String? = null

        while (currentCoroutineContext().isActive) {
            if (!prefs.lockEnabled || !Permissions.hasUsageAccess(this)) {
                stopSelf()
                return
            }
            val now = System.currentTimeMillis()

            val events = usage.queryEvents(since, now)
            val event = UsageEvents.Event()
            var latest: String? = null
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == EVENT_ACTIVITY_RESUMED) latest = event.packageName
            }
            since = now - 500
            if (latest != null) foreground = latest

            val fg = foreground
            if (fg != null) {
                LockState.onForeground(fg, now, prefs.relockSeconds * 1000L)
                val protectedApp = fg != packageName && fg in prefs.lockedPackages
                if (protectedApp && !LockState.isUnlocked(fg) && LockState.canShow(now)) {
                    LockState.beginShowing(fg, now)
                    showLock(fg)
                }
            }
            delay(POLL_MS)
        }
    }

    private fun showLock(pkg: String) {
        val intent = Intent(this, LockActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            .putExtra(LockActivity.EXTRA_PKG, pkg)
        runCatching { startActivity(intent) }.onFailure { LockState.endShowing() }
    }

    /**
     * A 1px invisible overlay. Newer Android versions only let an app start an activity from the
     * background if it currently has a visible window, and this keeps one alive.
     */
    private fun addAnchor() {
        if (!Settings.canDrawOverlays(this)) return
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val view = View(this)
        val lp = WindowManager.LayoutParams(
            1, 1,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.START or Gravity.TOP }
        runCatching {
            wm.addView(view, lp)
            anchor = view
        }
    }

    private fun removeAnchor() {
        val view = anchor ?: return
        runCatching { (getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(view) }
        anchor = null
    }

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "VoiceLock protection", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Keeps VoiceLock running so protected apps stay locked."
                setShowBadge(false)
            },
        )
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("VoiceLock is on")
            .setContentText("Your protected apps are locked.")
            .setContentIntent(open)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "voicelock_protection"
        private const val NOTIFICATION_ID = 1
        private const val POLL_MS = 400L

        /** UsageEvents.Event.ACTIVITY_RESUMED (API 29+) / MOVE_TO_FOREGROUND share this value. */
        private const val EVENT_ACTIVITY_RESUMED = 1

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, LockService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LockService::class.java))
        }
    }
}
