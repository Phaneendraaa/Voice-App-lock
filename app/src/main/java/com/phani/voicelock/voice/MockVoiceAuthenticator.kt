package com.phani.voicelock.voice

import com.phani.voicelock.data.Prefs
import kotlin.math.sqrt

/**
 * Demo engine: accepts any recording that contains audible sound.
 * NOT secure. Exists so the UI and lock flow can be tried on an emulator.
 */
class MockVoiceAuthenticator(private val prefs: Prefs) : VoiceAuthenticator {
    private var samples = 0

    override suspend fun prepare(): Result<Unit> = Result.success(Unit)

    override suspend fun enrollSample(audio: FloatArray, expectedPhrase: String): EnrollSampleResult {
        if (rms(audio) < MIN_RMS) return EnrollSampleResult(false, null, "I couldn't hear you. Try again.")
        samples++
        return EnrollSampleResult(true, expectedPhrase, null)
    }

    override suspend fun finishEnrollment(): Boolean {
        if (samples == 0) return false
        prefs.saveVoiceprint(floatArrayOf(1f))
        return true
    }

    override fun resetEnrollment() {
        samples = 0
    }

    override suspend fun verify(audio: FloatArray, expectedPhrase: String, threshold: Float): VerifyResult =
        if (rms(audio) >= MIN_RMS) VerifyResult.Success
        else VerifyResult.Rejected(wordsMatched = false, voiceScore = null, heard = null)

    override fun release() = Unit

    private fun rms(a: FloatArray): Float {
        if (a.isEmpty()) return 0f
        var s = 0.0
        for (x in a) s += x * x
        return sqrt(s / a.size).toFloat()
    }

    private companion object {
        const val MIN_RMS = 0.01f
    }
}
