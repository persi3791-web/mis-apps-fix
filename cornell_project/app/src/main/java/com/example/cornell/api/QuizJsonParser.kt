package com.example.cornell.api

import android.util.Log
import com.example.cornell.data.QuizQuestion
import org.json.JSONArray
import org.json.JSONObject
import java.util.regex.Pattern

/**
 * Equivale exactamente a extract_json_flexible(text, expected_structure='quiz')
 * de Python con los mismos 3 pasos de extracción.
 */
object QuizJsonParser {

    private const val TAG = "QuizJsonParser"

    /**
     * Equivale a extract_json_flexible(text, expected_structure='quiz') de Python.
     * Nunca lanza excepción. Devuelve lista vacía en el peor caso.
     */
    fun parse(rawText: String?): List<QuizQuestion> {
        if (rawText.isNullOrBlank() || rawText.trim().length < 10) {
            Log.e(TAG, "Texto vacío o muy corto")
            return emptyList()
        }

        val clean = rawText.trim()
            .replace(Regex("```json\\s*"), "")
            .replace(Regex("```\\s*"), "")
            .trim()

        // PASO 2: Parseo estricto con todas las combinaciones de { }
        val strictResult = tryStrictParse(clean)
        if (strictResult.isNotEmpty()) {
            Log.d(TAG, "JSON estricto: ${strictResult.size} preguntas")
            return strictResult
        }

        // PASO 3: Regex sobre el array de questions
        val regexResult = tryRegex(clean)
        if (regexResult.isNotEmpty()) {
            Log.d(TAG, "Regex: ${regexResult.size} preguntas")
            return regexResult
        }

        Log.e(TAG, "No se pudo extraer JSON. Preview: ${rawText.take(200)}")
        return emptyList()
    }

    private fun tryStrictParse(clean: String): List<QuizQuestion> {
        val starts = mutableListOf<Int>()
        val ends = mutableListOf<Int>()
        for (i in clean.indices) {
            when (clean[i]) {
                '{' -> starts.add(i)
                '}' -> ends.add(i)
            }
        }

        for (start in starts) {
            for (k in ends.indices.reversed()) {
                val end = ends[k]
                if (end <= start) continue
                try {
                    val jsonStr = clean.substring(start, end + 1)
                    val parsed = JSONObject(jsonStr)
                    val arr = when {
                        parsed.has("questions") -> parsed.optJSONArray("questions")
                        parsed.has("preguntas") -> parsed.optJSONArray("preguntas")
                        else -> null
                    }
                    if (arr != null && arr.length() > 0) {
                        return normalize(arr)
                    }
                } catch (_: Exception) { }
            }
        }
        return emptyList()
    }

    private fun tryRegex(clean: String): List<QuizQuestion> {
        val pattern = Pattern.compile(
            """["'](?:questions|preguntas)["']\s*:\s*(\[.*?])""",
            Pattern.DOTALL
        )
        val matcher = pattern.matcher(clean)
        if (matcher.find()) {
            try {
                val arr = JSONArray(matcher.group(1))
                val questions = normalize(arr)
                if (questions.isNotEmpty()) return questions
            } catch (_: Exception) { }
        }
        return emptyList()
    }

    /**
     * Normaliza el array de preguntas al modelo QuizQuestion.
     * Equivale al bloque de normalización en _call_gemini_quiz_api() de Python.
     */
    private fun normalize(array: JSONArray): List<QuizQuestion> {
        val result = mutableListOf<QuizQuestion>()
        for (i in 0 until array.length()) {
            try {
                val q = array.getJSONObject(i)
                val question = q.optString("question", "").ifBlank {
                    q.optString("pregunta", "Sin pregunta")
                }
                val optArr = when {
                    q.has("options") -> q.optJSONArray("options")
                    q.has("opciones") -> q.optJSONArray("opciones")
                    else -> null
                }
                val options = mutableListOf<String>()
                optArr?.let { arr ->
                    for (j in 0 until arr.length()) {
                        options.add(arr.getString(j))
                    }
                }
                var correctIndex = when {
                    q.has("correct_index") -> q.getInt("correct_index")
                    q.has("correct") -> q.getInt("correct")
                    q.has("correcta") -> q.getInt("correcta")
                    else -> 0
                }
                correctIndex = correctIndex.coerceIn(0, (options.size - 1).coerceAtLeast(0))
                val explanation = q.optString("explanation", "").ifBlank {
                    q.optString("explicacion", "")
                }
                if (question.isNotBlank() && options.isNotEmpty()) {
                    result.add(
                        QuizQuestion(
                            question = question,
                            pregunta = null,
                            options = options,
                            opciones = null,
                            correctIndex = correctIndex,
                            correct = null,
                            explanation = explanation,
                            explicacion = null
                        )
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error normalizando pregunta $i: ${e.message}")
            }
        }
        return result
    }
}
