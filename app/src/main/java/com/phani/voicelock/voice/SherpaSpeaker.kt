package com.phani.voicelock.voice

import com.k2fsa.sherpa.onnx.SpeakerEmbeddingExtractor
import com.k2fsa.sherpa.onnx.SpeakerEmbeddingExtractorConfig
import java.io.File
import kotlin.math.sqrt

/** Turns audio into a fixed-size "voiceprint" vector using an on-device speaker-embedding model. */
class SherpaSpeaker(modelFile: File) : AutoCloseable {
    private val extractor = SpeakerEmbeddingExtractor(
        config = SpeakerEmbeddingExtractorConfig(
            model = modelFile.absolutePath,
            numThreads = 2,
            debug = false,
        ),
    )

    /** L2-normalised embedding, or an empty array if the clip was too short to use. */
    fun embed(audio: FloatArray): FloatArray {
        val stream = extractor.createStream()
        try {
            stream.acceptWaveform(audio, AudioRecorder.SAMPLE_RATE)
            stream.inputFinished()
            if (!extractor.isReady(stream)) return FloatArray(0)
            return normalize(extractor.compute(stream))
        } finally {
            stream.release()
        }
    }

    override fun close() = extractor.release()

    companion object {
        fun normalize(v: FloatArray): FloatArray {
            var s = 0.0
            for (x in v) s += x * x
            val n = sqrt(s).toFloat()
            return if (n == 0f) v else FloatArray(v.size) { v[it] / n }
        }

        /** Cosine similarity of two vectors. Both are expected to be L2-normalised already. */
        fun cosine(a: FloatArray, b: FloatArray): Float {
            if (a.isEmpty() || a.size != b.size) return -1f
            var dot = 0.0
            for (i in a.indices) dot += a[i] * b[i]
            return dot.toFloat()
        }

        /** Mean of several embeddings, re-normalised. */
        fun average(all: List<FloatArray>): FloatArray {
            val dim = all.first().size
            val sum = FloatArray(dim)
            for (e in all) for (i in 0 until dim) sum[i] += e[i]
            return normalize(sum)
        }
    }
}
