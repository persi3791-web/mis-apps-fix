package com.mathsnip.app.network

import android.graphics.Bitmap
import android.util.Base64
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object GptClient {

    var isEnabled: Boolean = false
    var inlineDelim: String = "\\( ... \\)"
    var blockDelimUn: String = "\\[ ... \\]"
    var blockDelimN: String = "\\begin{equation}"

    private var githubToken: String = ""
    private var openrouterKey: String = ""

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun loadKeys(githubTok: String, openrouterK: String) {
        githubToken = githubTok
        openrouterKey = openrouterK
    }

    fun detectImageCoordinates(bitmap: Bitmap): List<FloatArray> {
        if (!isEnabled || githubToken.isEmpty()) return emptyList()
        return try {
            val b64 = bitmapToBase64(bitmap)
            val prompt = "Analiza esta pagina. Identifica SOLO imagenes, figuras, diagramas y graficos (NO texto). " +
                "Devuelve SOLO JSON valido: {\"images\": [{\"x\": 0.1, \"y\": 0.2, \"w\": 0.5, \"h\": 0.3}]}. " +
                "Si no hay imagenes: {\"images\": []}"
            val response = callGithubGpt(b64, prompt) ?: return emptyList()
            parseCoordinates(response, bitmap.width, bitmap.height)
        } catch (e: Exception) { emptyList() }
    }

    fun extractContent(bitmap: Bitmap): String {
        if (!isEnabled) return ""
        // Construir prompt con delimitadores configurados
        val inlineExample = inlineDelim.replace(" ... ", "formula")
        val blockExample = blockDelimUn.replace(" ... ", "formula")
        val prompt = "Eres un experto en OCR matematico. Analiza esta imagen y extrae TODO el contenido con maxima precision.\n" +
            "REGLAS ESTRICTAS:\n" +
            "1. Texto normal: extraelo exactamente como aparece\n" +
            "2. Formulas inline: usa el delimitador $inlineExample\n" +
            "3. Formulas en bloque: usa el delimitador $blockExample\n" +
            "4. Usa LaTeX correcto: \\frac{}{}, \\sqrt{}, \\sum, \\int, etc\n" +
            "5. Tablas: formato markdown\n" +
            "6. Mantener el orden exacto de la imagen\n" +
            "7. SIN comentarios ni explicaciones\n" +
            "8. SOLO el contenido extraido"

        val b64 = bitmapToBase64(bitmap)
        val gptResult = callGithubGpt(b64, prompt)
        if (!gptResult.isNullOrEmpty()) return gptResult

        val orResult = callOpenRouter(b64, prompt)
        if (!orResult.isNullOrEmpty()) return orResult

        return ""
    }

    private fun callGithubGpt(imageBase64: String, prompt: String): String? {
        if (githubToken.isEmpty()) return null
        return try {
            val contentArr = JSONArray()
            contentArr.put(JSONObject().put("type", "text").put("text", prompt))
            contentArr.put(JSONObject().put("type", "image_url").put("image_url",
                JSONObject().put("url", "data:image/jpeg;base64,$imageBase64")))

            val messages = JSONArray()
            messages.put(JSONObject().put("role", "user").put("content", contentArr))

            val json = JSONObject()
                .put("model", "gpt-4o")
                .put("messages", messages)

            val request = Request.Builder()
                .url("https://models.inference.ai.azure.com/chat/completions")
                .addHeader("Authorization", "Bearer $githubToken")
                .addHeader("Content-Type", "application/json")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return null
            val result = JSONObject(body)
            val content = result.optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
            if (content.isNullOrEmpty()) null else content
        } catch (e: Exception) { null }
    }

    private fun callOpenRouter(imageBase64: String, prompt: String): String? {
        if (openrouterKey.isEmpty()) return null
        return try {
            val contentArr = JSONArray()
            contentArr.put(JSONObject().put("type", "text").put("text", prompt))
            contentArr.put(JSONObject().put("type", "image_url").put("image_url",
                JSONObject().put("url", "data:image/jpeg;base64,$imageBase64")))

            val messages = JSONArray()
            messages.put(JSONObject().put("role", "user").put("content", contentArr))

            val json = JSONObject()
                .put("model", "nvidia/nemotron-nano-12b-v2-vl:free")
                .put("messages", messages)

            val request = Request.Builder()
                .url("https://openrouter.ai/api/v1/chat/completions")
                .addHeader("Authorization", "Bearer $openrouterKey")
                .addHeader("Content-Type", "application/json")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return null
            val result = JSONObject(body)
            val content = result.optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
            if (content.isNullOrEmpty()) null else content
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
