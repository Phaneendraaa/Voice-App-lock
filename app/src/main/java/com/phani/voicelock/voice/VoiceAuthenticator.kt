package com.phani.voicelock.voice

sealed interface VerifyResult {
    data object Success : VerifyResult

    /** The audio was processed but did not pass. */
    data class Rejected(
        val wordsMatched: Boolean,
        val voiceScore: Float?,
        val heard: String?,
    ) : VerifyResult

    /** The engine could not run (models missing, mic error...). */
    data class Failed(val message: String) : VerifyResult
}

data class EnrollSampleResult(val ok: Boolean, val heard: String?, val message: String?)

/**
 * One interface for "is this the right person saying the right words".
 * Audio is always 16 kHz mono float PCM in [-1, 1].
 */
interface VoiceAuthenticator {
    /** Load models. Safe to call repeatedly. */
    suspend fun prepare(): Result<Unit>

    /** Add one enrollment recording. */
    suspend fun enrollSample(audio: FloatArray, expectedPhrase: String): EnrollSampleResult

    /** Average the collected samples into a voiceprint and persist it. */
    suspend fun finishEnrollment(): Boolean

    fun resetEnrollment()

    suspend fun verify(audio: FloatArray, expectedPhrase: String, threshold: Float): VerifyResult

    fun release()
}
