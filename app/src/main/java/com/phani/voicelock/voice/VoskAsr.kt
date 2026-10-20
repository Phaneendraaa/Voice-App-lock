package com.phani.voicelock.voice

import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File

/** Offline speech-to-text using a small Vosk model. */
class VoskAsr(modelDir: File) : AutoCloseable {
    private val model = Model(modelDir.absolutePath)

    fun transcribe(audio: FloatArray): String {
        val pcm = ShortArray(audio.size) { (audio[it].coerceIn(-1f, 1f) * 32767f).toInt().toShort() }
        val recognizer = Recognizer(model, AudioRecorder.SAMPLE_RATE.toFloat())
        try {
            var pos = 0
            while (pos < pcm.size) {
                val n = minOf(4000, pcm.size - pos)
                recognizer.acceptWaveForm(pcm.copyOfRange(pos, pos + n), n)
                pos += n
            }
            return JSONObject(recognizer.finalResult).optString("text", "")
        } finally {
            recognizer.close()
        }
    }

    override fun close() = model.close()
}
