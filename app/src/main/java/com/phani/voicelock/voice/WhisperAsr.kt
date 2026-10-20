package com.phani.voicelock.voice

import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig

/** Offline speech-to-text with an int8 Whisper model, run through sherpa-onnx. */
class WhisperAsr(models: ModelManager) : AutoCloseable {
    private val recognizer = OfflineRecognizer(
        config = OfflineRecognizerConfig(
            modelConfig = OfflineModelConfig(
                whisper = OfflineWhisperModelConfig(
                    encoder = models.encoder.absolutePath,
                    decoder = models.decoder.absolutePath,
                    language = "en",
                    task = "transcribe",
                ),
                tokens = models.tokens.absolutePath,
                numThreads = 4,
                debug = false,
            ),
        ),
    )

    fun transcribe(audio: FloatArray): String {
        val stream = recognizer.createStream()
        try {
            stream.acceptWaveform(audio, AudioRecorder.SAMPLE_RATE)
            recognizer.decode(stream)
            return recognizer.getResult(stream).text
        } finally {
            stream.release()
        }
    }

    override fun close() = recognizer.release()
}
