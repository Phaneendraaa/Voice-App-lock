package com.phani.voicelock.voice

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Records one utterance from the microphone (16 kHz mono) and stops on its own after the
 * speaker finishes (energy-based end-of-speech detection). Caller must hold RECORD_AUDIO.
 */
class AudioRecorder {

    /**
     * @param maxMs hard cap on the recording length
     * @param onLevel called about 10x/second with a 0..1 loudness value for the UI
     * @return PCM floats in [-1, 1]; empty if the mic could not be opened
     */
    @SuppressLint("MissingPermission")
    suspend fun record(maxMs: Int = 7000, onLevel: (Float) -> Unit = {}): FloatArray =
        withContext(Dispatchers.IO) {
            val chunk = SAMPLE_RATE / 10 // 100 ms
            val minBuf = AudioRecord.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
            )
            val rec = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                max(minBuf, chunk * 2 * 4),
            )
            if (rec.state != AudioRecord.STATE_INITIALIZED) {
                rec.release()
                return@withContext FloatArray(0)
            }

            val buf = FloatArray(SAMPLE_RATE * maxMs / 1000)
            val shorts = ShortArray(chunk)
            var pos = 0
            var chunks = 0
            var noise = 0f
            var speech = false
            var silent = 0

            rec.startRecording()
            try {
                while (pos + chunk <= buf.size) {
                    ensureActive()
                    val n = rec.read(shorts, 0, chunk)
                    if (n <= 0) break
                    var sum = 0.0
                    for (i in 0 until n) {
                        val f = shorts[i] / 32768f
                        buf[pos + i] = f
                        sum += f * f
                    }
                    pos += n
                    val rms = sqrt(sum / n).toFloat()
                    onLevel((rms * 8f).coerceIn(0f, 1f))

                    chunks++
                    if (chunks <= 3) { // learn the noise floor from the first 300 ms
                        noise = max(noise, rms)
                        continue
                    }
                    val threshold = max(0.02f, noise * 3f)
                    if (rms > threshold) {
                        speech = true
                        silent = 0
                    } else if (speech) {
                        silent++
                        if (silent >= END_SILENCE_CHUNKS) break
                    } else if (chunks > NO_SPEECH_CHUNKS) {
                        break
                    }
                }
            } finally {
                runCatching { rec.stop() }
                rec.release()
            }
            buf.copyOf(pos)
        }

    companion object {
        const val SAMPLE_RATE = 16_000
        private const val END_SILENCE_CHUNKS = 9 // 0.9 s of quiet after speech
        private const val NO_SPEECH_CHUNKS = 50 // give up after 5 s of nothing
    }
}
