package com.example.cornell.api

import com.example.cornell.api.ModelPreference
import com.example.cornell.api.GptRepository
import com.example.cornell.data.CornellData
import com.example.cornell.data.FlashcardItem
import com.example.cornell.data.QuizQuestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.atomic.AtomicInteger

object GeminiRepository {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val apiKeys = listOf(
        "AIzaSyAW-Txvm4uBFbroL1g1saPrqFQqTaUkg1s",
        "AIzaSyCZeOdjUIU-F1zFObd2G9g590NXoEXHN2k",
        "AIzaSyAnHSwYcyJ6W7UI-7zYQW-LRA6SuR2MD1c",
        "AIzaSyBXCMX1ctS2U1sbfmES8IQF4dKqLpgf8Fc",
        "AIzaSyBsfWBhJUURhCYZioe6n6ZoTveQ6MKuHEc",
        "AIzaSyAoBQpl3IU7qbo0gp1BD-jHJ1zaeZEEGrc",
        "AIzaSyDYeT-T1WqnkoZ7puod6x4emw4ONy0NbfI",
        "AIzaSyAxyuTvE1tM3eaSLeac0Qf-mkgAevybimY",
        "AIzaSyA7BWO9mL5sNoga-gxODz8v8LsbPBAukaM",
        "AIzaSyBVdmjcLT7U8o-HpFmIp5zRVWWoHb9V0fs"
    )

    private val keyIndex = AtomicInteger(0)
    private fun nextKey(): String = apiKeys[keyIndex.getAndIncrement() % apiKeys.size]

    private val safetySettings = listOf(
        SafetySetting("HARM_CATEGORY_HARASSMENT", "BLOCK_ONLY_HIGH"),
        SafetySetting("HARM_CATEGORY_HATE_SPEECH", "BLOCK_ONLY_HIGH"),
        SafetySetting("HARM_CATEGORY_SEXUALLY_EXPLICIT", "BLOCK_ONLY_HIGH"),
        SafetySetting("HARM_CATEGORY_DANGEROUS_CONTENT", "BLOCK_ONLY_HIGH")
    )

    private val api: GeminiApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeminiApi::class.java)
    }

    suspend fun generateText(prompt: String, maxRetries: Int = 3): String? = withContext(Dispatchers.IO) {
        if (ModelPreference.useGpt4) return@withContext GptRepository.generateText(prompt)
        val request = GeminiGenerateRequest(
            contents = listOf(ContentItem(parts = listOf(Part(prompt)))),
            generationConfig = GenerationConfig(),
            safetySettings = safetySettings
        )
        repeat(maxRetries) { attempt ->
            val key = nextKey()
            val response = api.generateContent(key, request)
            if (response.isSuccessful) {
                val body = response.body()
                val text = body?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!text.isNullOrBlank()) return@withContext text
            }
            when (response.code()) {
                429 -> Thread.sleep(2000)
                404 -> return@withContext null
            }
        }
        null
    }

    suspend fun generateCornellFromText(text: String, title: String): CornellData? = withContext(Dispatchers.IO) {
        val subject = detectSubject(text, title)
        val summary = if (text.length <= 7000) text else summarizeChunks(text)
        generateCornellFromSummary(summary, title, subject)
    }

    private suspend fun detectSubject(text: String, title: String): String {
        if (text.length <= 100) return title.ifBlank { "General" }
        val prompt = "En una sola línea, ¿cuál es el tema de este texto? Solo el tema, sin explicación.\n\n${text.take(800)}"
        val raw = generateText(prompt)?.trim()
        if (!raw.isNullOrBlank() && raw.length < 80) return raw.trimEnd('.')
        return title.ifBlank { "General" }
    }

    private suspend fun summarizeChunks(text: String): String {
        val chunkSize = 7000
        val lines = text.split("\n")
        val chunks = mutableListOf<String>()
        var current = StringBuilder()
        for (line in lines) {
            if (current.length + line.length + 1 > chunkSize && current.isNotEmpty()) {
                chunks.add(current.toString().trim())
                current = StringBuilder(line).append("\n")
            } else {
                if (current.isNotEmpty()) current.append("\n")
                current.append(line)
            }
        }
        if (current.isNotEmpty()) chunks.add(current.toString().trim())
        if (chunks.isEmpty()) return text.take(7000)
        val summaries = chunks.mapIndexed { i, chunk ->
            generateText(
                "Resume este fragmento (${i + 1}/${chunks.size}) en viñetas cortas. Solo texto con viñetas (•).\n\n$chunk"
            ) ?: chunk.take(800)
        }
        return summaries.joinToString("\n\n").take(4500)
    }

    private suspend fun generateCornellFromSummary(summary: String, title: String, subject: String): CornellData {
        val safeTitle = title.ifBlank { subject }
        val materialIdeas = summary.take(3000)
        val ideasClave = generateText(
            "Eres un estudiante experto en el Método Cornell. Extrae SOLO las ideas clave del material. " +
                "Usa • para conceptos, **negritas** para términos. Mínimo 5 puntos. Solo el contenido, sin títulos ni JSON. " +
                "Preserva emojis, emoticones y caracteres Unicode si aparecen en el material.\n\n$materialIdeas"
        )?.take(2500)?.trim()
            ?: summary.lines().filter { it.isNotBlank() }.take(maxOf(5, summary.lines().size / 3))
                .joinToString("\n") { if (it.startsWith("•")) it else "• $it" }.take(2500)

        kotlinx.coroutines.delay(5000)
        val materialNotas = summary.take(4000)
        val notasClase = generateText(
            "Eres un estudiante experto en el Método Cornell. Extrae notas detalladas del material. " +
                "Usa • y **negritas**. Mínimo 8 puntos. Solo el contenido, sin títulos ni JSON. " +
                "Preserva emojis, emoticones y caracteres Unicode si aparecen en el material.\n\n$materialNotas"
        )?.take(3000)?.trim()
            ?: summary.lines().filter { it.isNotBlank() }.drop(summary.lines().size / 3).take(summary.lines().size / 3)
                .joinToString("\n") { if (it.startsWith("•")) it else "• $it" }.take(3000)

        kotlinx.coroutines.delay(5000)
        val materialResumen = summary.take(3000)
        val resumen = generateText(
            "Eres un estudiante experto en el Método Cornell. Escribe un resumen conciso en un párrafo (máx 200 palabras). " +
                "Usa **negritas** para términos importantes. Solo el párrafo, sin títulos ni JSON. " +
                "Preserva emojis, emoticones y caracteres Unicode si aparecen en el material.\n\n$materialResumen"
        )?.take(1200)?.trim()
            ?: summary.lines().filter { it.isNotBlank() }.takeLast(8).joinToString(" ").take(1000)

        return CornellData(
            asignatura = subject,
            titulo = safeTitle,
            ideasClave = ideasClave.ifBlank { "• Tema: $subject\n• Ver detalles en notas" },
            notasClase = notasClase.ifBlank { summary.take(3000) },
            resumen = resumen.ifBlank { "Resumen de $safeTitle en $subject." }
        )
    }

    /**
     * Resultado de generación de cuestionario.
     * @param questions preguntas generadas
     * @param isFallback true si se usó quiz de respaldo (API falló)
     */
    data class QuizResult(val questions: List<QuizQuestion>, val isFallback: Boolean)

    /**
     * Genera cuestionario según la especificación Cornell Quiz.
     * Usa el prompt exacto de la spec, QuizJsonParser (extract_json_flexible) y fallback si falla la API.
     */
    suspend fun generateQuiz(cornellData: CornellData, originalText: String, quantity: Int): QuizResult? = withContext(Dispatchers.IO) {
        val cantidad = quantity.coerceIn(1, 50)
        val contenido = buildCornellContent(cornellData, originalText)
        val prompt = buildQuizPrompt(contenido, cantidad)

        var questions: List<QuizQuestion>? = null
        repeat(MAX_RETRIES) { attempt ->
            val content = generateText(prompt, maxRetries = 2)
            if (!content.isNullOrBlank()) {
                val parsed = QuizJsonParser.parse(content)
                questions = parsed
                if (parsed.isNotEmpty()) return@withContext QuizResult(parsed, false)
            }
            if (attempt < MAX_RETRIES - 1) kotlinx.coroutines.delay(3000)
        }

        // Fallback: quiz con datos limitados cuando todos los intentos fallan
        buildFallbackQuiz(cornellData)?.let { return@withContext QuizResult(it, true) }
        null
    }

    /**
     * Construye el contenido Cornell para el prompt. Equivale a CornellNote.buildCornellContent()
     */
    private fun buildCornellContent(cornellData: CornellData, originalText: String): String {
        val sb = StringBuilder()
        sb.append("ASIGNATURA: ").append(cornellData.asignatura).append("\n")
        sb.append("TÍTULO: ").append(cornellData.titulo.ifBlank { "Sin título" }).append("\n\n")
        sb.append("IDEAS CLAVE:\n").append(cornellData.ideasClave).append("\n\n")
        sb.append("NOTAS DE CLASE:\n").append(cornellData.notasClase).append("\n\n")
        sb.append("RESUMEN:\n").append(cornellData.resumen)
        if (originalText.trim().length > 50) {
            val t = if (originalText.length > 5000) originalText.take(5000) else originalText
            sb.append("\n\nTEXTO ORIGINAL COMPLETO:\n").append(t)
        }
        val result = sb.toString()
        return if (result.length > 6000) result.take(6000) + "..." else result
    }

    /**
     * El mismo prompt del código Python (buildQuizPrompt):
     * Eres un profesor experto... TAREA: Crea EXACTAMENTE {cantidad} preguntas...
     */
    private fun buildQuizPrompt(contenido: String, cantidad: Int): String {
        return """
Eres un profesor experto creando un cuestionario de estudio.

TAREA: Crea EXACTAMENTE $cantidad preguntas de opción múltiple basadas en el siguiente material.

FORMATO REQUERIDO - RESPONDE SOLO CON ESTE JSON (sin texto adicional):
{
  "questions": [
    {
      "question": "texto de la pregunta",
      "options": ["opción A", "opción B", "opción C", "opción D"],
      "correct_index": 0,
      "explanation": "explicación breve"
    }
  ]
}

REGLAS:
- El campo "correct_index" debe ser el índice (0-3) de la respuesta correcta
- Incluye EXACTAMENTE $cantidad preguntas
- NO agregues texto antes o después del JSON
- NO uses bloques ```json```

MATERIAL:
$contenido

RESPONDE SOLO CON EL JSON:
        """.trimIndent()
    }

    /** Fallback cuando todos los intentos de API fallan */
    private fun buildFallbackQuiz(cornellData: CornellData): List<QuizQuestion>? {
        val options = listOf(
            cornellData.asignatura,
            "Opción B",
            "Opción C",
            "Opción D"
        )
        return listOf(
            QuizQuestion(
                question = "¿Cuál es el tema principal de '${cornellData.titulo.ifBlank { "esta nota" }}'?",
                pregunta = null,
                options = options,
                opciones = null,
                correctIndex = 0,
                correct = null,
                explanation = "El tema principal es ${cornellData.asignatura}",
                explicacion = null
            )
        )
    }

    private const val MAX_RETRIES = 3

    suspend fun generateFlashcards(cornellData: CornellData, originalText: String, quantity: Int): List<FlashcardItem>? = withContext(Dispatchers.IO) {
        val content = """
            ASIGNATURA: ${cornellData.asignatura}
            TÍTULO: ${cornellData.titulo}
            IDEAS: ${cornellData.ideasClave}
            NOTAS: ${cornellData.notasClase}
            RESUMEN: ${cornellData.resumen}
            ${if (originalText.length > 50) "TEXTO: ${originalText.take(5000)}" else ""}
        """.trimIndent().take(6000)

        val prompt = """
            Eres un profesor. Crea EXACTAMENTE $quantity flashcards. Responde SOLO con este JSON:
            {"flashcards": [{"pregunta": "¿...?", "respuesta": "..."}]}
            Material:
            $content
            RESPONDE SOLO CON EL JSON:
        """.trimIndent()

        val raw = generateText(prompt) ?: return@withContext null
        parseFlashcardsJson(raw, quantity)
    }

    private fun parseFlashcardsJson(raw: String, quantity: Int): List<FlashcardItem>? {
        val clean = raw.replace("```json", "").replace("```", "").trim()
        val start = clean.indexOf('{')
        val end = clean.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return try {
            val json = JSONObject(clean.substring(start, end + 1))
            val arr = json.optJSONArray("flashcards") ?: json.optJSONArray("tarjetas") ?: return null
            val list = mutableListOf<FlashcardItem>()
            for (i in 0 until minOf(arr.length(), quantity)) {
                val obj = arr.getJSONObject(i)
                list.add(
                    FlashcardItem(
                        pregunta = obj.optString("pregunta", obj.optString("front", "")),
                        respuesta = obj.optString("respuesta", obj.optString("back", obj.optString("reverso", "")))
                    )
                )
            }
            list
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Evalúa la explicación del estudiante contra el material Cornell.
     * Retorna JSON con score, found, missing, tip.
     */
    suspend fun evaluateExplanation(noteContent: String, explanation: String): String? = withContext(Dispatchers.IO) {
        val content = noteContent.take(5000)
        val prompt = """
            Eres un evaluador educativo experto en el Método Cornell.
            Evalúa si el estudiante comprendió el material de sus notas basándote en su explicación con sus propias palabras.

            MATERIAL DE REFERENCIA:
            \"\"\"
            $content
            \"\"\"

            EXPLICACIÓN DEL ESTUDIANTE:
            \"\"\"
            $explanation
            \"\"\"

            Responde SOLO con este JSON exacto (sin texto adicional, sin ```json):
            {"score": <número entero 0-100>, "found": ["concepto que explicó bien"], "missing": ["concepto importante que faltó"], "tip": "sugerencia concreta en español simple"}

            Criterios:
            90-100: Explicación completa y clara con sus palabras
            70-89:  Bien explicado, faltan algunos detalles
            50-69:  Captó la idea principal pero incompleto
            30-49:  Muy incompleto o confuso
            0-29:   No demostró comprensión real
        """.trimIndent()
        generateText(prompt, maxRetries = 3)
    }

    suspend fun chatWithContext(messages: List<Pair<String, String>>, noteContext: String): String? = withContext(Dispatchers.IO) {
        // FIX: rutear a GPT-4o cuando está activado
        if (ModelPreference.useGpt4) {
            val enrichedMessages = messages.mapIndexed { index, (role, text) ->
                val partText = if (role == "user" && index == 0) {
                    "Eres un asistente educativo. Ayuda al estudiante con sus notas Cornell.\n\n$noteContext\n\nPregunta del estudiante: $text"
                } else text
                Pair(role, partText)
            }
            return@withContext GptRepository.chat(enrichedMessages)
        }

        val contents = messages.mapIndexed { index, (role, text) ->
            val partText = if (role == "user" && index == 0) {
                "Eres un asistente educativo. Ayuda al estudiante con sus notas Cornell.\n\n$noteContext\n\nPregunta del estudiante: $text"
            } else text
            val apiRole = if (role == "model") "model" else "user"
            ContentItem(role = apiRole, parts = listOf(Part(partText)))
        }
        val request = GeminiGenerateRequest(
            contents = contents,
            generationConfig = GenerationConfig(),
            safetySettings = safetySettings
        )
        repeat(3) {
            val response = api.generateContent(nextKey(), request)
            if (response.isSuccessful) {
                val text = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!text.isNullOrBlank()) return@withContext text
            }
        }
        null
    }
}
