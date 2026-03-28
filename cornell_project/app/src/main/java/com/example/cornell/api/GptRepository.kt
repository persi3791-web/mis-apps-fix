package com.example.cornell.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object GptRepository {

    var githubToken: String = "PLACEHOLDER"
    private const val ENDPOINT = "https://models.inference.ai.azure.com/chat/completions"
    private const val MODEL = "gpt-4o"

    suspend fun generateText(prompt: String): String? = withContext(Dispatchers.IO) {
        chat(listOf(Pair("user", prompt)))
    }

    suspend fun chat(messages: List<Pair<String, String>>): String? = withContext(Dispatchers.IO) {
        if (githubToken.isBlank()) return@withContext "⚠️ Configura tu GitHub Token en Ajustes"
        try {
            val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $githubToken")
                doOutput = true
                connectTimeout = 30000
                readTimeout = 90000  // GPT-4o puede tardar más que Gemini
            }

            val msgs = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "Eres un asistente educativo experto en el Método Cornell. Responde siempre en español.")
                })
                messages.forEach { (role, content) ->
                    put(JSONObject().apply {
                        put("role", role)
                        put("content", content)
                    })
                }
            }

            val body = JSONObject().apply {
                put("model", MODEL)
                put("messages", msgs)
                put("max_tokens", 1500)
                put("temperature", 0.7)
            }.toString()

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }

            val code = conn.responseCode
            val resp = try {
                (if (code == 200) conn.inputStream else conn.errorStream)
                    ?.bufferedReader(Charsets.UTF_8)?.readText() ?: ""
            } catch (e: Exception) { "" }

            if (code == 200 && resp.isNotBlank()) {
                try {
                    JSONObject(resp)
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                } catch (e: Exception) {
                    "⚠️ Error al parsear respuesta: ${e.message}"
                }
            } else {
                // Mostrar mensaje de error detallado para diagnosticar
                val detail = try {
                    if (resp.isNotBlank()) {
                        val errJson = JSONObject(resp)
                        errJson.optJSONObject("error")?.optString("message")
                            ?: errJson.optString("message")
                            ?: resp.take(200)
                    } else "Sin respuesta del servidor"
                } catch (_: Exception) { resp.take(200) }
                "⚠️ Error $code: $detail"
            }
        } catch (e: java.net.SocketTimeoutException) {
            "⚠️ Tiempo de espera agotado. Intenta de nuevo."
        } catch (e: Exception) {
            "⚠️ Error de conexión: ${e.message}"
        }
    }
}
