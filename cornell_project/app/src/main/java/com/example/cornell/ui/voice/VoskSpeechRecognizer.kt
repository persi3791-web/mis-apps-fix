package com.example.cornell.ui.voice

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * Reconocimiento de voz en tiempo real con Vosk.
 * Transcripción palabra por palabra como en ChatGPT/Claude.
 */
class VoskSpeechRecognizer(
    private val context: Context,
    private val onPartialResult: (String) -> Unit,
    private val onFinalResult: (String) -> Unit,
    private val onStatus: (String) -> Unit,
    private val onError: (String) -> Unit
) {
    private var model: Model? = null
    private var speechService: SpeechService? = null

    private val modelPath: File
        get() = File(context.filesDir, "vosk-model-small-es-0.42")

    private val modelZipUrl = "https://alphacephei.com/vosk/models/vosk-model-small-es-0.42.zip"

    suspend fun initialize(onProgress: (Int) -> Unit): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!File(modelPath, "am/final.mdl").exists()) {
                onStatus("Descargando modelo de voz (39 MB)...")
                if (!downloadAndExtractModel(onProgress)) {
                    onError("Error al descargar el modelo")
                    return@withContext false
                }
            }
            onStatus("Cargando modelo...")
            model = Model(modelPath.absolutePath)
            onStatus("Listo — habla ahora")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error init Vosk", e)
            onError("Error: ${e.message}")
            false
        }
    }

    private suspend fun downloadAndExtractModel(onProgress: (Int) -> Unit): Boolean = withContext(Dispatchers.IO) {
        try {
            val zipFile = File(context.cacheDir, "vosk-model-es.zip")
            val url = URL(modelZipUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.connect()
            val total = conn.contentLength
            conn.inputStream.use { input ->
                FileOutputStream(zipFile).use { output ->
                    val buffer = ByteArray(8192)
                    var downloaded = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (total > 0) {
                            val pct = (downloaded * 100 / total).toInt()
                            onProgress(pct.coerceIn(0, 100))
                        }
                    }
                }
            }
            onStatus("Extrayendo modelo...")
            ZipInputStream(zipFile.inputStream()).use { zis ->
                var entry = zis.nextEntry
                val destDir = context.filesDir
                while (entry != null) {
                    val file = File(destDir, entry.name)
                    if (entry.isDirectory) {
                        file.mkdirs()
                    } else {
                        file.parentFile?.mkdirs()
                        FileOutputStream(file).use { out ->
                            zis.copyTo(out)
                        }
                    }
                    entry = zis.nextEntry
                }
            }
            zipFile.delete()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Download error", e)
            false
        }
    }

    fun startListening(): Boolean {
        val m = model ?: return false
        return try {
            val recognizer = Recognizer(m, 16000.0f)
            speechService = SpeechService(recognizer, 16000.0f)
            speechService?.startListening(object : RecognitionListener {
                override fun onPartialResult(hypothesis: String?) {
                    hypothesis?.let { parseAndEmit(it, false) }
                }
                override fun onResult(hypothesis: String?) {
                    hypothesis?.let { parseAndEmit(it, true) }
                }
                override fun onFinalResult(hypothesis: String?) {
                    hypothesis?.let { parseAndEmit(it, true) }
                }
                override fun onError(e: Exception?) {
                    onError(e?.message ?: "Error de reconocimiento")
                }
                override fun onTimeout() {}
            })
            true
        } catch (e: Exception) {
            Log.e(TAG, "Start error", e)
            onError(e.message ?: "Error al iniciar")
            false
        }
    }

    private fun parseAndEmit(json: String, isFinal: Boolean) {
        try {
            val obj = JSONObject(json)
            // Partial results use "partial", final use "text"
            val text = (obj.optString("text", obj.optString("partial", ""))).trim()
            if (text.isNotEmpty()) {
                if (isFinal) {
                    onFinalResult(text)
                } else {
                    onPartialResult(text)
                }
            }
        } catch (_: Exception) {}
    }

    fun stop() {
        try {
            speechService?.stop()
            speechService?.shutdown()
        } catch (_: Exception) {}
        speechService = null
    }

    fun isReady(): Boolean = model != null

    companion object {
        private const val TAG = "VoskSpeech"
    }
}
