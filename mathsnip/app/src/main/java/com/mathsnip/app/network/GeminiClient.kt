package com.mathsnip.app.network

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit

object GeminiClient {

    var isEnabled: Boolean = false
    private var keys: MutableList<String> = mutableListOf()
    private var currentKeyIndex = 0

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun loadKeys(context: Context) {
        try {
            val fromAssets = context.assets.open("gemini_keys.txt")
                .bufferedReader()
                .readLines()
                .map { it.trim() }
                .filter { it.isNotBlank() && it.startsWith("AIza") }
            if (fromAssets.isNotEmpty()) {
                keys = fromAssets.toMutableList()
                currentKeyIndex = 0
                return
            }
        } catch (e: Exception) { }
        try {
            val paths = listOf(
                "/sdcard/Download/MathSnip/gemini_keys.txt",
                "/storage/emulated/0/Download/MathSnip/gemini_keys.txt"
            )
            for (path in paths) {
                val file = File(path)
                if (file.exists()) {
                    val loaded = file.readLines()
                        .map { it.trim() }
                        .filter { it.isNotBlank() && it.startsWith("AIza") }
                    if (loaded.isNotEmpty()) {
                        keys = loaded.toMutableList()
                        currentKeyIndex = 0
                        return
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun getKeyCount(): Int = keys.size

    private fun nextKey(): String? {
        if (keys.isEmpty()) return null
        val key = keys[currentKeyIndex % keys.size]
        currentKeyIndex = (currentKeyIndex + 1) % keys.size
        return key
    }

    fun extractContent(bitmap: Bitmap): String {
        if (!isEnabled || keys.isEmpty()) return ""
        return try {
            val key = nextKey() ?: return ""
            val b64 = bitmapToBase64(bitmap)
            val prompt = "Eres un experto en OCR. Extrae TODO el contenido de la imagen. Formulas en LaTeX. SIN comentarios, SOLO el contenido."
            callGemini(key, b64, prompt)?.trim() ?: ""
        } catch (e: Exception) { android.util.Log.e("GeminiClient", e.toString()); "" }
    }

    fun detectImageCoordinates(bitmap: Bitmap): List<FloatArray> {
        if (!isEnabled || keys.isEmpty()) return emptyList()
        return try {
            val key = nextKey() ?: return emptyList()
            val b64 = bitmapToBase64(bitmap)
            val prompt = "Identifica imagenes en esta pagina. SOLO JSON: {images: [{x: 0.1, y: 0.2, w: 0.5, h: 0.3}]}. Si no hay: {images: []}"
            val response = callGemini(key, b64, prompt) ?: return emptyList()
            parseCoordinates(response, bitmap.width, bitmap.height)
        } catch (e: Exception) { emptyList() }
    }

    fun improveResult(bitmap: Bitmap, mlKitText: String, latexText: String): String {
        if (!isEnabled || keys.isEmpty()) return ""
        return try {
            val key = nextKey() ?: return ""
            val b64 = bitmapToBase64(bitmap)
            val prompt = "Corrige este OCR viendo la imagen. OCR: $mlKitText. LaTeX: $latexText. Devuelve SOLO el contenido corregido, SIN explicaciones."
            callGemini(key, b64, prompt)?.trim() ?: ""
        } catch (e: Exception) { android.util.Log.e("GeminiClient", e.toString()); "" }
    }

    private fun callGemini(key: String, imageBase64: String, prompt: String): String? {
        return try {
            val parts = JSONArray()
            parts.put(JSONObject().apply {
                put("inline_data", JSONObject().apply {
                    put("mime_type", "image/jpeg")
                    put("data", imageBase64)
                })
            })
            parts.put(JSONObject().put("text", prompt))
            val content = JSONObject().put("parts", parts)
            val contents = JSONArray().put(content)
            val json = JSONObject().put("contents", contents)
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$key")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return null
            val result = JSONObject(body)
            result.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text")
        } catch (e: Exception) { null }
    }

    private fun parseCoordinates(json: String, imgWidth: Int, imgHeight: Int): List<FloatArray> {
        val result = mutableListOf<FloatArray>()
        try {
            val clean = json.trim().removePrefix("```json").removeSuffix("```").trim()
            val obj = JSONObject(clean)
            val images = obj.optJSONArray("images") ?: return emptyList()
            for (i in 0 until images.length()) {
                val img = images.getJSONObject(i)
                val x = (img.optDouble("x", 0.0) * imgWidth).toFloat()
                val y = (img.optDouble("y", 0.0) * imgHeight).toFloat()
                val w = (img.optDouble("w", 0.0) * imgWidth).toFloat()
                val h = (img.optDouble("h", 0.0) * imgHeight).toFloat()
                if (w > 20 && h > 20) result.add(floatArrayOf(x, y, w, h))
            }
        } catch (e: Exception) { e.printStackTrace() }
        return result
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }
}
