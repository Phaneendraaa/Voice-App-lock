package com.phani.voicelock.voice

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

class ModelsMissingException : Exception("Voice models are not downloaded yet.")

/**
 * Downloads the two on-device models once (about 60 MB total) into app-private storage:
 *  - Vosk small Indian-English model: turns speech into words
 *  - WeSpeaker ResNet34 (run through sherpa-onnx): turns a voice into a voiceprint vector
 * After that, everything runs offline.
 */
class ModelManager(context: Context) {
    private val root = File(context.applicationContext.filesDir, "models")
    val voskDir = File(root, VOSK_NAME)
    val speakerFile = File(root, "speaker.onnx")

    fun isReady(): Boolean = File(voskDir, "am").isDirectory && speakerFile.length() > MIN_MODEL_BYTES

    suspend fun ensure(onProgress: (fraction: Float, label: String) -> Unit): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                root.mkdirs()

                if (speakerFile.length() <= MIN_MODEL_BYTES) {
                    val part = File(root, "speaker.onnx.part")
                    download(SPEAKER_URL, part) { onProgress(it * 0.4f, "Downloading voice-matching model…") }
                    check(part.length() > MIN_MODEL_BYTES) { "Voice-matching model download was incomplete." }
                    speakerFile.delete()
                    check(part.renameTo(speakerFile)) { "Could not save the voice-matching model." }
                }

                if (!File(voskDir, "am").isDirectory) {
                    val zip = File(root, "vosk.zip")
                    download(VOSK_URL, zip) { onProgress(0.4f + it * 0.5f, "Downloading speech model…") }
                    onProgress(0.92f, "Unpacking…")
                    voskDir.deleteRecursively()
                    unzip(zip, root)
                    zip.delete()
                    check(File(voskDir, "am").isDirectory) { "Speech model looked wrong after unpacking." }
                }
                onProgress(1f, "Ready")
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

    private suspend fun unzip(zip: File, into: File) {
        val base = into.canonicalPath + File.separator
        ZipInputStream(zip.inputStream().buffered()).use { zin ->
            while (true) {
                currentCoroutineContext().ensureActive()
                val entry = zin.nextEntry ?: break
                val out = File(into, entry.name)
                // Refuse entries that would escape the target folder (zip-slip).
                if (!out.canonicalPath.startsWith(base)) throw IOException("Bad entry in archive: ${entry.name}")
                if (entry.isDirectory) {
                    out.mkdirs()
                } else {
                    out.parentFile?.mkdirs()
                    out.outputStream().use { zin.copyTo(it) }
                }
            }
        }
    }

    companion object {
        private const val VOSK_NAME = "vosk-model-small-en-in-0.4"
        private const val VOSK_URL = "https://alphacephei.com/vosk/models/$VOSK_NAME.zip"
        private const val SPEAKER_URL =
            "https://github.com/k2-fsa/sherpa-onnx/releases/download/speaker-recongition-models/wespeaker_en_voxceleb_resnet34.onnx"
        private const val MIN_MODEL_BYTES = 5L * 1024 * 1024
    }
}
