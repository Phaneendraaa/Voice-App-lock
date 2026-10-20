package com.phani.voicelock.voice

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class ModelsMissingException : Exception("Voice models are not downloaded yet.")

/**
 * Downloads the two on-device models for a [ModelProfile] once, into app-private storage:
 *  - Whisper (int8, via sherpa-onnx): turns speech into words
 *  - WeSpeaker (via sherpa-onnx): turns a voice into a voiceprint vector
 * After that, everything runs offline.
 */
class ModelManager(context: Context, private val profile: ModelProfile) {
    private val root = File(context.applicationContext.filesDir, "models")
    private val whisperDir = File(root, "whisper-${profile.whisper}")

    val encoder = File(whisperDir, "${profile.whisper}-encoder.int8.onnx")
    val decoder = File(whisperDir, "${profile.whisper}-decoder.int8.onnx")
    val tokens = File(whisperDir, "${profile.whisper}-tokens.txt")
    val speakerFile = File(root, "speaker-${profile.speakerFile}")

    private fun whisperReady() =
        encoder.length() > MIN_MODEL_BYTES && decoder.length() > MIN_MODEL_BYTES && tokens.length() > 10_000

    private fun speakerReady() = speakerFile.length() > MIN_MODEL_BYTES

    fun isReady(): Boolean = whisperReady() && speakerReady()

    suspend fun ensure(onProgress: (fraction: Float, label: String) -> Unit): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                root.mkdirs()
                removeLegacyFiles()

                if (!speakerReady()) {
                    val part = File(root, "${speakerFile.name}.part")
                    download("$SPEAKER_BASE/${profile.speakerFile}", part) {
                        onProgress(it * 0.15f, "Downloading voice-matching model…")
                    }
                    check(part.length() > MIN_MODEL_BYTES) { "Voice-matching model download was incomplete." }
                    speakerFile.delete()
                    check(part.renameTo(speakerFile)) { "Could not save the voice-matching model." }
                }

                if (!whisperReady()) {
                    val archive = File(root, "whisper.tar.bz2")
                    download("$WHISPER_BASE/sherpa-onnx-whisper-${profile.whisper}.tar.bz2", archive) {
                        onProgress(0.15f + it * 0.7f, "Downloading speech model…")
                    }
                    onProgress(0.86f, "Unpacking speech model (can take a minute or two)…")
                    extractWhisper(archive)
                    archive.delete()
                    check(whisperReady()) { "Speech model looked wrong after unpacking." }
                }
                onProgress(1f, "Ready")
            }
        }

    /** Pulls just the int8 encoder/decoder and tokens out of the release archive. */
    private suspend fun extractWhisper(archive: File) {
        whisperDir.mkdirs()
        val wanted = setOf(encoder.name, decoder.name, tokens.name)
        TarArchiveInputStream(BZip2CompressorInputStream(archive.inputStream().buffered(1 shl 16))).use { tar ->
            while (true) {
                currentCoroutineContext().ensureActive()
                val entry = tar.nextEntry ?: break
                val name = entry.name.substringAfterLast('/')
                if (entry.isDirectory || name !in wanted) continue
                // Only fixed, known file names are written, directly into our own folder.
                File(whisperDir, name).outputStream().use { tar.copyTo(it) }
            }
        }
    }

    private suspend fun download(url: String, dest: File, onFraction: (Float) -> Unit) {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 20_000
        conn.readTimeout = 30_000
        conn.instanceFollowRedirects = true
        try {
            if (conn.responseCode !in 200..299) throw IOException("Server returned ${conn.responseCode}")
            val total = conn.contentLengthLong
            var read = 0L
            conn.inputStream.use { input ->
                dest.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        read += n
                        if (total > 0) onFraction((read.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
    }

    /** Frees space used by the first version of the app (Vosk + single speaker model). */
    private fun removeLegacyFiles() {
        File(root, "vosk-model-small-en-in-0.4").deleteRecursively()
        File(root, "vosk.zip").delete()
        File(root, "speaker.onnx").delete()
        File(root, "speaker.onnx.part").delete()
    }

    private companion object {
        const val SPEAKER_BASE = "https://github.com/k2-fsa/sherpa-onnx/releases/download/speaker-recongition-models"
        const val WHISPER_BASE = "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models"
        const val MIN_MODEL_BYTES = 5L * 1024 * 1024
    }
}
