package com.phani.voicelock.voice

import android.content.Context
import com.phani.voicelock.data.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * The real check: words via Vosk AND voice via a sherpa-onnx speaker embedding.
 * Unlock needs both to pass.
 */
class RealVoiceAuthenticator(context: Context, private val prefs: Prefs) : VoiceAuthenticator {
    private val models = ModelManager(context)
    private val lock = Mutex()
    private var asr: VoskAsr? = null
    private var speaker: SherpaSpeaker? = null
    private val enrolled = mutableListOf<FloatArray>()

    override suspend fun prepare(): Result<Unit> = withContext(Dispatchers.Default) {
        lock.withLock {
            runCatching {
                if (!models.isReady()) throw ModelsMissingException()
                if (asr == null) asr = VoskAsr(models.voskDir)
                if (speaker == null) speaker = SherpaSpeaker(models.speakerFile)
            }
        }
    }

    override suspend fun enrollSample(audio: FloatArray, expectedPhrase: String): EnrollSampleResult =
        withContext(Dispatchers.Default) {
            val asr = asr
            val speaker = speaker
            if (asr == null || speaker == null) return@withContext EnrollSampleResult(false, null, "Voice engine isn't ready.")
            if (audio.size < MIN_SAMPLES) return@withContext EnrollSampleResult(false, null, "That was too short. Try again.")

            val heard = asr.transcribe(audio)
            if (!PhraseMatcher.matches(expectedPhrase, heard)) {
                val said = if (heard.isBlank()) "nothing I could understand" else "“$heard”"
                return@withContext EnrollSampleResult(false, heard, "I heard $said. Say the phrase exactly.")
            }
            val embedding = speaker.embed(audio)
            if (embedding.isEmpty()) return@withContext EnrollSampleResult(false, heard, "I couldn't use that recording. Try again.")

            enrolled += embedding
            EnrollSampleResult(true, heard, null)
        }

    override suspend fun finishEnrollment(): Boolean {
        if (enrolled.isEmpty()) return false
        prefs.saveVoiceprint(SherpaSpeaker.average(enrolled))
        return true
    }

    override fun resetEnrollment() {
        enrolled.clear()
    }

    override suspend fun verify(audio: FloatArray, expectedPhrase: String, threshold: Float): VerifyResult =
        withContext(Dispatchers.Default) {
            val asr = asr
            val speaker = speaker
            if (asr == null || speaker == null) return@withContext VerifyResult.Failed("Voice engine isn't ready. Use your PIN.")
            val voiceprint = prefs.loadVoiceprint() ?: return@withContext VerifyResult.Failed("No voice profile found. Use your PIN.")

            // Always run both checks so a failure doesn't reveal which half failed first.
            val heard = asr.transcribe(audio)
            val wordsOk = PhraseMatcher.matches(expectedPhrase, heard)
            val score = SherpaSpeaker.cosine(speaker.embed(audio), voiceprint)

            if (wordsOk && score >= threshold) VerifyResult.Success
            else VerifyResult.Rejected(wordsMatched = wordsOk, voiceScore = score, heard = heard)
        }

    override fun release() {
        runCatching { asr?.close() }
        runCatching { speaker?.close() }
        asr = null
        speaker = null
    }

    private companion object {
        const val MIN_SAMPLES = 12_000 // 0.75 s
    }
}
