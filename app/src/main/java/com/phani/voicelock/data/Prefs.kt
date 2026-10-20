package com.phani.voicelock.data

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class IntruderEntry(val timestamp: Long, val packageName: String, val reason: String)

class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("voicelock", Context.MODE_PRIVATE)

    var onboardingDone: Boolean
        get() = sp.getBoolean("onboarding_done", false)
        set(v) = sp.edit { putBoolean("onboarding_done", v) }

    /** Master switch for the lock service. */
    var lockEnabled: Boolean
        get() = sp.getBoolean("lock_enabled", false)
        set(v) = sp.edit { putBoolean("lock_enabled", v) }

    /** When true the voice check is simulated. For UI testing only. */
    var demoMode: Boolean
        get() = sp.getBoolean("demo_mode", false)
        set(v) = sp.edit { putBoolean("demo_mode", v) }

    var passphrase: String
        get() = sp.getString("passphrase", DEFAULT_PASSPHRASE) ?: DEFAULT_PASSPHRASE
        set(v) = sp.edit { putString("passphrase", v.trim().lowercase()) }

    /** Number of random digits appended to the passphrase at unlock time (0 = off). */
    var challengeDigits: Int
        get() = sp.getInt("challenge_digits", 2)
        set(v) = sp.edit { putInt("challenge_digits", v.coerceIn(0, 4)) }

    var relockSeconds: Int
        get() = sp.getInt("relock_seconds", 30)
        set(v) = sp.edit { putInt("relock_seconds", v.coerceIn(0, 600)) }

    /** Cosine-similarity threshold for the voiceprint. Higher = stricter. */
    var threshold: Float
        get() = sp.getFloat("threshold", 0.6f)
        set(v) = sp.edit { putFloat("threshold", v.coerceIn(0.3f, 0.9f)) }

    var lockedPackages: Set<String>
        get() = HashSet(sp.getStringSet("locked", emptySet()) ?: emptySet())
        set(v) = sp.edit { putStringSet("locked", HashSet(v)) }

    fun setLocked(pkg: String, locked: Boolean) {
        lockedPackages = if (locked) lockedPackages + pkg else lockedPackages - pkg
    }

    // ---- Voiceprint (encrypted) ----

    val hasVoiceprint: Boolean get() = sp.contains("voiceprint")

    fun saveVoiceprint(v: FloatArray) {
        sp.edit { putString("voiceprint", SecureStore.encrypt(SecureStore.floatsToBytes(v))) }
    }

    fun loadVoiceprint(): FloatArray? {
        val enc = sp.getString("voiceprint", null) ?: return null
        return runCatching { SecureStore.bytesToFloats(SecureStore.decrypt(enc)) }.getOrNull()
    }

    fun clearVoiceprint() = sp.edit { remove("voiceprint") }

    // ---- Fallback PIN (PBKDF2, salted) ----

    val hasPin: Boolean get() = sp.contains("pin_hash")

    fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        sp.edit {
            putString("pin_salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            putString("pin_hash", Base64.encodeToString(hashPin(pin, salt), Base64.NO_WRAP))
        }
    }

    fun verifyPin(pin: String): Boolean {
        val salt = sp.getString("pin_salt", null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return false
        val expected = sp.getString("pin_hash", null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return false
        return MessageDigest.isEqual(expected, hashPin(pin, salt))
    }

    private fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 100_000, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    // ---- Intruder log ----

    fun intruderLog(): List<IntruderEntry> =
        (sp.getString("intruders", "") ?: "")
            .lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull {
                val p = it.split("|", limit = 3)
                if (p.size == 3) IntruderEntry(p[0].toLongOrNull() ?: 0L, p[1], p[2]) else null
            }
            .toList()

    fun addIntruder(pkg: String, reason: String) {
        val line = "${System.currentTimeMillis()}|$pkg|${reason.replace('|', '/').replace('\n', ' ')}"
        val all = (listOf(line) + (sp.getString("intruders", "") ?: "").lines().filter { it.isNotBlank() }).take(50)
        sp.edit { putString("intruders", all.joinToString("\n")) }
    }

    fun clearIntruders() = sp.edit { remove("intruders") }

    companion object {
        const val DEFAULT_PASSPHRASE = "open sesame"
    }
}
