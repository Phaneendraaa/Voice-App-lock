package com.phani.voicelock.lock

/**
 * In-memory unlock bookkeeping shared by the service and the lock screen.
 * An app stays unlocked while it is in front, and for `relockSeconds` after the user leaves it.
 */
object LockState {
    private val unlocked = HashSet<String>()
    private val leftAt = HashMap<String, Long>()
    private var lastForeground: String? = null

    @Volatile private var showingFor: String? = null
    @Volatile private var showingSince = 0L

    @Synchronized
    fun markUnlocked(pkg: String) {
        unlocked += pkg
        leftAt.remove(pkg)
    }

    @Synchronized
    fun isUnlocked(pkg: String) = pkg in unlocked

    @Synchronized
    fun clearAll() {
        unlocked.clear()
        leftAt.clear()
    }

    /** Called by the service on every tick with the app currently in front. */
    @Synchronized
    fun onForeground(fg: String, now: Long, relockMs: Long) {
        if (fg != lastForeground) {
            lastForeground?.let { if (it in unlocked) leftAt[it] = now }
            lastForeground = fg
        }
        val iter = unlocked.iterator()
        while (iter.hasNext()) {
            val pkg = iter.next()
            val left = leftAt[pkg] ?: continue
            if (pkg != fg && now - left >= relockMs) {
                iter.remove()
                leftAt.remove(pkg)
            }
        }
    }

    /** True if no lock screen is currently up (or the last one looks stuck). */
    fun canShow(now: Long): Boolean = showingFor == null || now - showingSince > 8_000

    fun beginShowing(pkg: String, now: Long) {
        showingFor = pkg
        showingSince = now
    }

    fun endShowing() {
        showingFor = null
    }
}
