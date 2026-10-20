package com.phani.voicelock.voice

import com.k2fsa.sherpa.onnx.SpeakerEmbeddingExtractor
import com.k2fsa.sherpa.onnx.SpeakerEmbeddingExtractorConfig
import java.io.File

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
            return VectorMath.normalize(extractor.compute(stream))
        } finally {
            stream.release()
        }
    }

    override fun close() = extractor.release()
}
