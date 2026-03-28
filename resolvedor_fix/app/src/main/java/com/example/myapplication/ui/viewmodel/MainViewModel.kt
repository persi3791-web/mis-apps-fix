package com.example.myapplication.ui.viewmodel

import android.app.Application
import android.os.Environment
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.AppState
import com.example.myapplication.data.GeminiResult
import com.example.myapplication.data.HistoryItem
import com.example.myapplication.data.ProblemRow
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // --- KEYS (Idénticas al script Python) ---
    private val API_KEYS_GEMINI = listOf(
        "AIzaSyBH-4FcDt9vpEJlDwxMeCW1QigrtF3Zt2k",
        "AIzaSyDnl11aCnK7qOE-K9viBBJim0QD_UrF5uM"
    )
    // URL exacta usada en Python
    private val API_URL_GEMINI = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
    
    private val GOOGLE_API_KEYS = listOf(
        "AIzaSyBuzM-cCtTJ9cplHi7OTbEaSBhrAGx7dBA",
        "AIzaSyDvYMA-M5UsNn2n8kMNwl8AW5L7BgKZF6A"
    )
    private val CX = "96728b66d8b3841df"
    
    private val YOUTUBE_API_KEYS = listOf(
        "AIzaSyDDOez2klqoPhFOWNaBiiuSFhxRDWDill8",
        "AIzaSyBwbnsB7Uk-M464-rxhY2aG6mDPL5ZFZSI"
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(90, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val gson = Gson()
    private val context = application.applicationContext

    // --- STATES ---
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText

    private val _rows = MutableStateFlow<List<ProblemRow>>(emptyList())
    val rows: StateFlow<List<ProblemRow>> = _rows

    private val _progressText = MutableStateFlow("")
    val progressText: StateFlow<String> = _progressText

    private val _isResolving = MutableStateFlow(false)
    val isResolving: StateFlow<Boolean> = _isResolving

    private val _adicionalInstructions = MutableStateFlow("")
    val adicionalInstructions: StateFlow<String> = _adicionalInstructions

    private val _historyItems = MutableStateFlow<List<HistoryItem>>(emptyList())
    val historyItems: StateFlow<List<HistoryItem>> = _historyItems

    private val _showCompletionMessage = MutableStateFlow<Pair<String, String>?>(null)
    val showCompletionMessage: StateFlow<Pair<String, String>?> = _showCompletionMessage

    private val prefs get() = context.getSharedPreferences("resolvedor_prefs", android.content.Context.MODE_PRIVATE)

    private val _exportFolderUri = MutableStateFlow<String?>(prefs.getString("export_folder_uri", null))
    val exportFolderUri: StateFlow<String?> = _exportFolderUri

    fun setExportFolder(uri: android.net.Uri) {
        context.contentResolver.takePersistableUriPermission(uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        prefs.edit().putString("export_folder_uri", uri.toString()).apply()
        _exportFolderUri.value = uri.toString()
    }

    private fun getNextCsvNumber(): Int {
        val n = prefs.getInt("csv_counter", 0) + 1
        prefs.edit().putInt("csv_counter", n).apply()
        return n
    }

    // --- MODELO IA SELECCIONADO ---
    enum class AiModel { GEMINI, GPT4 }
    private val _selectedModel = MutableStateFlow(AiModel.GEMINI)
    val selectedModel: StateFlow<AiModel> = _selectedModel

    fun toggleModel() {
        _selectedModel.value = if (_selectedModel.value == AiModel.GEMINI) AiModel.GPT4 else AiModel.GEMINI
    }

    init {
        loadState()
        loadHistory()
    }

    // --- UI FUNCTIONS ---
    fun updateInputText(text: String) { 
        _inputText.value = text
        saveState()
    }
    
    fun updateInstructions(text: String) {
        _adicionalInstructions.value = text
        saveState()
    }
    
    fun dismissCompletionMessage() { 
        _showCompletionMessage.value = null 
    }

    // --- PARSEO ---
    fun parseTable() {
        val text = _inputText.value.trim()
        if (text.isEmpty()) {
            _progressText.value = "Por favor pega la tabla con datos válidos."
            return
        }

        val newRows = mutableListOf<ProblemRow>()
        val lines = text.split("\n")
        // Saltar filas si parecen encabezados (contienen "---" o palabras como "numero"/"problema")
        val firstLine = lines.getOrElse(0) { "" }.lowercase()
        val secondLine = lines.getOrElse(1) { "" }
        val hasHeader = firstLine.contains("numero") || firstLine.contains("problema") || secondLine.contains("---")
        val startIdx = if (hasHeader) 2 else 0

        for (i in startIdx until lines.size) {
            var line = lines[i].trim()
            if (line.isBlank()) continue
            if (line.startsWith("|")) line = line.substring(1)
            
            val cols = line.split("|").map { it.trim() }
            if (cols.size >= 2) {
                val numero = cols[0]
                val problema = cols[1]
                val rawUrl = if (cols.size > 2) cols[2] else ""
                val cleanUrl = extractUrlFromMarkdown(rawUrl)
                
                newRows.add(ProblemRow(numero, problema, cleanUrl))
            }
        }
        
        _rows.value = newRows
        _progressText.value = "${newRows.size} problemas cargados."
        saveState()
    }

    private fun extractUrlFromMarkdown(text: String): String {
        if (text.isEmpty()) return ""
        val regexMd = Regex("!\\[.*?\\]\\((https?://[^\\)\\s]+)\\)")
        val matchMd = regexMd.find(text)
        if (matchMd != null) return matchMd.groupValues[1]
        
        val regexUrl = Regex("(https?://\\S+)")
        val matchUrl = regexUrl.find(text)
        return matchUrl?.groupValues?.get(1)?.trim()?.trimEnd(')', '.', ';') ?: ""
    }

    // --- RESOLUCIÓN PRINCIPAL ---
    fun resolveSequential() {
        if (_rows.value.isEmpty()) return
        
        viewModelScope.launch(Dispatchers.IO) {
            _isResolving.value = true
            _progressText.value = "Iniciando..."

            val currentRows = _rows.value.toMutableList()
            
            currentRows.forEachIndexed { index, row ->
                val i = index + 1
                _progressText.value = "Procesando $i/${currentRows.size}..."
                
                // 1. Imagen
                var realImgUrl = row.enlaceImagen
                if (realImgUrl.isNotEmpty()) {
                    val resolved = resolveRealImageUrl(realImgUrl)
                    if (resolved != null) realImgUrl = resolved
                }
                row.enlaceImagenReal = realImgUrl

                // 2. Descargar Imagen Base64
                var base64Image: String? = null
                var mimeType: String? = null
                if (realImgUrl.isNotEmpty()) {
                    val result = downloadImageToBase64(realImgUrl)
                    if (result != null) {
                        base64Image = result.first
                        mimeType = result.second
                    }
                }

                // 3. Solución IA (Gemini o GPT-4 según modelo seleccionado)
                updateUIState(currentRows, index, "Resolviendo...")
                val resSolucion = callAiApi(row.problem, base64Image, mimeType, isMnemonic = false)
                
                if (resSolucion != null) {
                    row.solucion = resSolucion.text
                    row.tokensPrompt = resSolucion.promptTokens
                    row.tokensRespuesta = resSolucion.candidateTokens
                    row.tokensTotal = resSolucion.totalTokens
                } else {
                    row.solucion = "Error generando solución"
                }

                // 4. Búsquedas
                row.enlaceGoogle = searchGoogle(row.problem)
                row.enlaceYoutube = searchYoutube(row.problem)

                // 5. Mnemotecnia IA
                updateUIState(currentRows, index, "Creando Mnemotecnia...")
                if (row.solucion.isNotEmpty() && !row.solucion.startsWith("Error")) {
                    val resMnemo = callAiApi(row.solucion, null, null, isMnemonic = true)
                    if (resMnemo != null) {
                        row.mnemotecnia = resMnemo.text
                        row.tokensPromptMnemo = resMnemo.promptTokens
                        row.tokensRespuestaMnemo = resMnemo.candidateTokens
                        row.tokensTotalMnemo = resMnemo.totalTokens
                    } else {
                        row.mnemotecnia = "Error generando mnemotecnia"
                    }
                } else {
                    row.mnemotecnia = "No hay solución base"
                }

                saveState()
                updateUIState(currentRows, index, "Listo")
                kotlinx.coroutines.delay(1500) // Pausa para evitar rate limit
            }

            exportToCsv(currentRows)
            _progressText.value = "Terminado. CSV guardado en Descargas."
            _isResolving.value = false
            withContext(Dispatchers.Main) {
                _showCompletionMessage.value = Pair("Éxito", "CSV guardado en carpeta Download/Scripts/🟢 Aplicaciones/resultados_problemas.csv")
            }
        }
    }

    private suspend fun updateUIState(list: List<ProblemRow>, index: Int, status: String) {
        // Truco para forzar actualización en Compose
        withContext(Dispatchers.Main) {
            _rows.value = ArrayList(list)
        }
    }

    // --- AUTO-FORMATO DE TABLA ---
    fun autoFormatInput() {
        val text = _inputText.value.trim()
        if (text.isEmpty()) { _progressText.value = "Nada que ordenar."; return }
        _progressText.value = "IA analizando..."
        viewModelScope.launch(Dispatchers.IO) {
            val prompt = "Analiza este texto y organiza en tabla Markdown.\n" +
                "Identifica NUMERO, ENUNCIADO y ENLACE (http) por fila.\n" +
                "Devuelve SOLO filas en formato: | numero | enunciado | enlace |\n" +
                "Sin encabezados. Sin explicaciones. Si no hay numero usa 1,2,3.\n" +
                "Texto:\n===\n" + text + "\n==="
            val result = callAiApi(prompt, null, null, isMnemonic = false)
            withContext(Dispatchers.Main) {
                if (result != null && result.text.isNotBlank()) {
                    _inputText.value = result.text.lines().filter { it.trim().startsWith("|") }.joinToString("\n")
                    _progressText.value = "Tabla ordenada por IA."
                } else {
                    _progressText.value = "Error al ordenar."
                }
                saveState()
            }
        }
    }


    // --- LEER TOKEN DE ASSETS ---
    private fun readAssetToken(filename: String): String? {
        return try {
            context.assets.open(filename).bufferedReader().readText().trim()
        } catch (e: Exception) {
            Log.e("AssetToken", "No se pudo leer $filename: ${e.message}")
            null
        }
    }

    // --- DISPATCHER DE MODELO ---
    private fun callAiApi(text: String, base64Img: String?, mime: String?, isMnemonic: Boolean): GeminiResult? {
        return when (_selectedModel.value) {
            AiModel.GEMINI -> callGeminiApi(text, base64Img, mime, isMnemonic)
            AiModel.GPT4 -> callGPT4Api(text, base64Img, mime, isMnemonic)
        }
    }

    // --- GPT-4o API (GitHub Models) ---
    private fun callGPT4Api(text: String, base64Img: String?, mime: String?, isMnemonic: Boolean): GeminiResult? {
        val githubToken = readAssetToken("github_token.txt")
        if (githubToken.isNullOrEmpty()) {
            Log.e("GPT4Api", "Token no encontrado en assets/github_token.txt")
            return null
        }

        val prompt = if (isMnemonic) {
            """
            Eres un experto en el lore de League of Legends. Tu tarea es crear una mnemotecnia para memorizar la siguiente solución matemática, que está dividida en pasos (1., 2., 3., etc.).

            Reglas estrictas:
            1. Usa personajes, habilidades, objetos o lore de League of Legends.
            2. Cada mnemotecnia para un paso (ej: 1.) debe conectarse con la del paso anterior, creando una historia continua.
            3. El estilo debe ser directo, como una acción o un hecho.
            4. NO uses frases como "Recuerda...", "Piensa en..." ni similares.
            5. NO uses ningún separador como "---".
            6. Responde ÚNICAMENTE con la mnemotecnia, empezando por "1. ...".

            Solución a memorizar:
            ===
            $text
            ===
            Genera la mnemotecnia ahora:
            """.trimIndent()
        } else {
            val extra = _adicionalInstructions.value
            val instruccionesExtra = if (extra.isNotEmpty()) "Instrucciones extra para resolver:\n$extra\n\n" else ""
            "${instruccionesExtra}Eres un experto matemático que resuelve problemas mostrando solo el procedimiento, " +
            "usando fórmulas delimitadas con \\( ... \\) para inline y \\[ ... \\] para bloques. " +
            "No agregues texto explicativo adicional.\n\n$text"
        }

        val messages = JSONArray()
        val userMessage = JSONObject()
        userMessage.put("role", "user")

        if (base64Img != null && !isMnemonic) {
            val contentArray = JSONArray()
            contentArray.put(JSONObject().put("type", "text").put("text", prompt))
            val imageUrlObj = JSONObject().put("url", "data:${mime ?: "image/jpeg"};base64,$base64Img")
            contentArray.put(JSONObject().put("type", "image_url").put("image_url", imageUrlObj))
            userMessage.put("content", contentArray)
        } else {
            userMessage.put("content", prompt)
        }
        messages.put(userMessage)

        val bodyJson = JSONObject()
            .put("model", "gpt-4o")
            .put("messages", messages)
            .put("max_tokens", 2000)

        val bodyStr = bodyJson.toString().toRequestBody("application/json".toMediaType())

        for (attempt in 1..4) {
            try {
                val request = Request.Builder()
                    .url("https://models.inference.ai.azure.com/chat/completions")
                    .addHeader("Authorization", "Bearer $githubToken")
                    .addHeader("Content-Type", "application/json")
                    .post(bodyStr)
                    .build()

                val response = client.newCall(request).execute()
                val respBody = response.body?.string() ?: ""
                val code = response.code

                if (code in 400..499 && code != 429) {
                    Log.e("GPT4Api", "Error cliente $code: $respBody")
                    return null
                }
                if (code >= 500 || code == 429) {
                    Log.w("GPT4Api", "Intento $attempt: Error $code, reintentando...")
                    if (attempt < 4) { Thread.sleep(5000); continue }
                    return null
                }

                if (response.isSuccessful) {
                    val json = JSONObject(respBody)
                    val choices = json.optJSONArray("choices")
                    val content = choices?.getJSONObject(0)?.optJSONObject("message")?.optString("content") ?: ""
                    val usage = json.optJSONObject("usage")
                    val pTokens = usage?.optInt("prompt_tokens") ?: 0
                    val cTokens = usage?.optInt("completion_tokens") ?: 0
                    val tTokens = usage?.optInt("total_tokens") ?: 0
                    if (content.isNotEmpty()) {
                        Log.d("GPT4Api", "✅ Respuesta GPT-4o exitosa")
                        return GeminiResult(content, pTokens, cTokens, tTokens)
                    }
                }
            } catch (e: Exception) {
                Log.e("GPT4Api", "Intento $attempt error: ${e.message}")
                if (attempt < 4) { Thread.sleep(5000); continue }
            }
        }
        Log.e("GPT4Api", "GPT-4o: todos los intentos fallaron")
        return null
    }

    // --- GEMINI API (Corregida) ---
    private fun callGeminiApi(text: String, base64Img: String?, mime: String?, isMnemonic: Boolean): GeminiResult? {
        val prompt = if (isMnemonic) {
            """
            Eres un experto en el lore de League of Legends. Tu tarea es crear una mnemotecnia para memorizar la siguiente solución matemática, que está dividida en pasos (1., 2., 3., etc.).

            Reglas estrictas:
            1. Usa personajes, habilidades, objetos o lore de League of Legends.
            2. Cada mnemotecnia para un paso (ej: 1.) debe conectarse con la del paso anterior, creando una historia continua.
            3. El estilo debe ser directo, como una acción o un hecho. (Ejemplo: "1. Veigar calcula la velocidad de su Q...", "2. La concentración de mana de Cassiopeia...").
            4. NO uses frases como "Recuerda...", "Piensa en..." ni similares.
            5. NO uses ningún separador como "---".
            6. Responde ÚNICAMENTE con la mnemotecnia, empezando por "1. ...".

            Solución a memorizar:
            ===
            $text
            ===
            Genera la mnemotecnia ahora:
            """.trimIndent()
        } else {
            val extra = _adicionalInstructions.value
            val instruccionesExtra = if (extra.isNotEmpty()) {
                "Instrucciones extra para resolver:\n$extra\n\n"
            } else ""
            "$instruccionesExtra" +
            "Eres un experto matemático que resuelve problemas mostrando solo el procedimiento, " +
            "usando fórmulas delimitadas con \\( ... \\) para inline y \\[ ... \\] para bloques. " +
            "No agregues texto explicativo adicional.\n\n$text"
        }

        val partsArray = JSONArray()
        partsArray.put(JSONObject().put("text", prompt))
        
        if (base64Img != null && !isMnemonic) {
            val inlineData = JSONObject()
            inlineData.put("mime_type", mime ?: "image/jpeg")
            inlineData.put("data", base64Img)
            partsArray.put(JSONObject().put("inline_data", inlineData))
        }

        val jsonBody = JSONObject()
        jsonBody.put("contents", JSONArray().put(JSONObject().put("parts", partsArray)))

        val body = jsonBody.toString().toRequestBody("application/json".toMediaType())

        // Reintentos y Rotación de Keys (EXACTAMENTE como Python)
        for (apiKey in API_KEYS_GEMINI) {
            Log.d("GeminiApi", "Usando clave Gemini: ${apiKey.take(8)}...")
            
            // Bucle de REINTENTO
            for (attempt in 1..8) {
                try {
                    // CORRECCIÓN: Usar header x-goog-api-key en lugar de URL param
                    val request = Request.Builder()
                        .url(API_URL_GEMINI) 
                        .addHeader("x-goog-api-key", apiKey) // <--- CLAVE AQUÍ
                        .addHeader("Content-Type", "application/json")
                        .post(body)
                        .build()
                    
                    val response = client.newCall(request).execute()
                    val respBody = response.body?.string() ?: ""
                    val code = response.code

                    // EXACTAMENTE como Python: primero verificar errores 400-499 (excepto 429)
                    if (code in 400..499 && code != 429) {
                        Log.w("GeminiApi", "Error de cliente $code con clave ${apiKey.take(8)}. Probando siguiente clave.")
                        break // Rompe bucle de reintento, va a la siguiente CLAVE
                    }
                    
                    // EXACTAMENTE como Python: si es 500+ o 429, reintentar
                    if (code >= 500 || code == 429) {
                        Log.d("GeminiApi", "Intento $attempt/8: Error $code. Reintentando...")
                        if (attempt < 8) {
                            Thread.sleep(5000)
                            continue
                        }
                    }

                    if (response.isSuccessful) {
                        val respJson = JSONObject(respBody)
                        
                        // Extraer texto
                        var generatedText: String? = null
                        val candidates = respJson.optJSONArray("candidates")
                        if (candidates != null && candidates.length() > 0) {
                            val content = candidates.getJSONObject(0).optJSONObject("content")
                            val parts = content?.optJSONArray("parts")
                            if (parts != null && parts.length() > 0) {
                                generatedText = parts.getJSONObject(0).optString("text")
                            }
                        }

                        // Extraer tokens (usageMetadata)
                        val usage = respJson.optJSONObject("usageMetadata")
                        val pToken = usage?.optInt("promptTokenCount") ?: 0
                        val cToken = usage?.optInt("candidatesTokenCount") ?: 0
                        val tToken = usage?.optInt("totalTokenCount") ?: 0

                        if (!generatedText.isNullOrEmpty()) {
                            Log.d("GeminiApi", "✅ ${if (isMnemonic) "MNEMOTECNIA" else "SOLUCIÓN"} GENERADA EXITOSAMENTE")
                            return GeminiResult(generatedText, pToken, cToken, tToken)
                        }
                        
                        // Si respuesta exitosa pero sin texto, reintentar
                        Log.d("GeminiApi", "Intento $attempt: respuesta incompleta, reintentando...")
                        if (attempt < 8) {
                            Thread.sleep(5000)
                            continue
                        }
                    } else {
                        Log.e("GeminiError", "Code: $code, Body: $respBody")
                    }
                } catch (e: Exception) {
                    // EXACTAMENTE como Python: capturar RequestException y reintentar
                    Log.e("GeminiApi", "Intento $attempt/8: Error de red/timeout: ${e.message}. Reintentando...")
                    if (attempt < 8) {
                        Thread.sleep(5000)
                        continue
                    }
                }
            }
            
            Log.w("GeminiApi", "Clave ${apiKey.take(8)} falló después de 8 intentos.")
            Thread.sleep(1000) // Esperar antes de cambiar de key
        }
        
        Log.e("GeminiApi", "Todas las claves fallaron")
        return null
    }

    // --- YOUTUBE & GOOGLE (Corregidas) ---
    private fun searchYoutube(query: String): String {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        
        // El bucle de rotación de claves permanece igual
        for (key in YOUTUBE_API_KEYS) {
            try {
                val url = "https://www.googleapis.com/youtube/v3/search?part=snippet&q=$encodedQuery&maxResults=1&type=video&key=$key"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "{}")
                    val items = json.optJSONArray("items")
                    if (items != null && items.length() > 0) {
                        val videoId = items.getJSONObject(0).optJSONObject("id")?.optString("videoId")
                        // Si encuentra un ID de video válido, devuelve el enlace
                        if (!videoId.isNullOrEmpty()) return "https://www.youtube.com/watch?v=$videoId"
                    }
                    // Si la búsqueda es exitosa pero no hay 'items', sale del bucle con el valor de error por defecto.
                } else {
                    // Si el código de respuesta no es exitoso (e.g., cuota excedida), 
                    // simplemente se registra el error y se prueba con la siguiente clave
                    Log.e("YT_Error", "API Key $key falló con código ${response.code}")
                }
            } catch (e: Exception) { 
                Log.e("YT_Error", "Excepción de red con Key $key: ${e.message}")
                // Continúa con la siguiente clave
            }
        }
        
        // COMPORTAMIENTO IDÉNTICO AL PYTHON: Si todas las claves fallan o no encuentran video, 
        // devuelve la cadena literal "Video no disponible".
        return "Video no disponible"
    }

    private fun searchGoogle(query: String): String {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        for (key in GOOGLE_API_KEYS) {
            try {
                val url = "https://www.googleapis.com/customsearch/v1?key=$key&cx=$CX&q=$encodedQuery"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "{}")
                    val items = json.optJSONArray("items")
                    if (items != null && items.length() > 0) {
                        return items.getJSONObject(0).optString("link")
                    }
                }
            } catch (e: Exception) { 
                Log.e("GoogleSearch", "Error: ${e.message}")
            }
        }
        return "No encontrado"
    }

    // --- IMÁGENES ---
    private fun resolveRealImageUrl(url: String): String? {
        try {
            if (url.lowercase().endsWith(".png") || 
                url.lowercase().endsWith(".jpg") || 
                url.lowercase().endsWith(".jpeg") ||
                url.lowercase().endsWith(".webp") ||
                url.lowercase().endsWith(".gif")) {
                return url
            }
            
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return null
            
            val regexImg = Regex("<img[^>]+src=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
            val match = regexImg.find(html)
            if (match != null) {
                var src = match.groupValues[1]
                if (src.startsWith("//")) src = "https:$src"
                else if (src.startsWith("/")) {
                   val uri = java.net.URI(url)
                   src = "${uri.scheme}://${uri.host}$src"
                }
                return src
            }
            
            // Buscar og:image
            val regexOg = Regex("<meta[^>]+property=[\"']og:image[\"'][^>]+content=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
            val matchOg = regexOg.find(html)
            if (matchOg != null) {
                var src = matchOg.groupValues[1]
                if (src.startsWith("//")) src = "https:$src"
                else if (src.startsWith("/")) {
                   val uri = java.net.URI(url)
                   src = "${uri.scheme}://${uri.host}$src"
                }
                return src
            }
        } catch (e: Exception) {
            Log.e("ResolveImg", "Error: ${e.message}")
        }
        return null
    }

    private fun downloadImageToBase64(url: String): Pair<String, String>? {
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null
            val bytes = response.body?.bytes() ?: return null
            val mime = response.header("Content-Type")?.split(";")?.get(0) ?: "image/jpeg"
            return Pair(Base64.encodeToString(bytes, Base64.NO_WRAP), mime)
        } catch (e: Exception) { 
            Log.e("DownloadImg", "Error: ${e.message}")
            return null 
        }
    }

    // --- CSV & HISTORY ---
    private suspend fun exportToCsv(rows: List<ProblemRow>) {
        val number = getNextCsvNumber()
        val filename = "resultados_$number.csv"
        val folderUriStr = _exportFolderUri.value

        try {
            if (folderUriStr != null) {
                // Guardar en carpeta seleccionada por usuario via SAF
                val folderUri = android.net.Uri.parse(folderUriStr)
                val docUri = DocumentFile.fromTreeUri(context, folderUri)
                val file = docUri?.createFile("text/csv", filename)
                if (file != null) {
                    context.contentResolver.openOutputStream(file.uri)?.use { fos ->
                        writeCsvContent(fos, rows)
                    }
                    withContext(Dispatchers.Main) {
                        _showCompletionMessage.value = Pair("Éxito", "CSV guardado: $filename")
                    }
                    return
                }
            }
            // Fallback: carpeta Downloads
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val file = java.io.File(downloadsDir, filename)
            java.io.FileOutputStream(file).use { fos -> writeCsvContent(fos, rows) }
            withContext(Dispatchers.Main) {
                _showCompletionMessage.value = Pair("Éxito", "CSV guardado en Downloads: $filename")
            }
        } catch (e: Exception) {
            Log.e("ExportCsv", "Error: ${e.message}")
        }
    }

    private fun writeCsvContent(fos: java.io.OutputStream, rows: List<ProblemRow>) {
        val bom = ubyteArrayOf(0xEF.toUByte(), 0xBB.toUByte(), 0xBF.toUByte()).toByteArray()
        fos.write(bom)
        val header = "Número de Avión,Problema,Enlace Imagen,Enlace Google,Enlace YouTube,Solución IA,Mnemotecnia,Tokens Prompt,Tokens Respuesta,Tokens Total,Tokens Prompt Mnemotecnia,Tokens Respuesta Mnemotecnia,Tokens Total Mnemotecnia\n"
        fos.write(header.toByteArray(Charsets.UTF_8))
        for (row in rows) {
            val line = buildString {
                append(escapeCsv(row.numeroAvion)).append(",")
                append(escapeCsv(row.problem)).append(",")
                append(escapeCsv(row.enlaceImagenReal)).append(",")
                append(escapeCsv(row.enlaceGoogle)).append(",")
                append(escapeCsv(row.enlaceYoutube)).append(",")
                append(escapeCsv(row.solucion)).append(",")
                append(escapeCsv(row.mnemotecnia)).append(",")
                append(row.tokensPrompt).append(",")
                append(row.tokensRespuesta).append(",")
                append(row.tokensTotal).append(",")
                append(row.tokensPromptMnemo).append(",")
                append(row.tokensRespuestaMnemo).append(",")
                append(row.tokensTotalMnemo).append("\n")
            }
            fos.write(line.toByteArray(Charsets.UTF_8))
        }
    }

    private fun escapeCsv(text: String): String {
        var s = text.replace("\"", "\"\"")
        if (s.contains(",") || s.contains("\n") || s.contains("\"")) {
            s = "\"$s\""
        }
        return s
    }

    // Funciones de historial
    fun loadHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(context.filesDir, "instructions_history.json")
                if (file.exists()) {
                    val type = object : TypeToken<List<HistoryItem>>() {}.type
                    val list: List<HistoryItem> = gson.fromJson(file.readText(), type)
                    withContext(Dispatchers.Main) {
                        _historyItems.value = list
                    }
                }
            } catch (e: Exception) {
                Log.e("LoadHistory", "${e.message}")
            }
        }
    }

    fun saveInstructionsToHistory() {
        val content = _adicionalInstructions.value
        if (content.trim().isEmpty()) return
        
        viewModelScope.launch(Dispatchers.IO) {
            var titulo = "Instrucción sin título"
            val gen = callGeminiApi("Resume este texto en máximo 6 palabras para usarlo como título: $content", null, null, false)
            if (gen != null && gen.text.isNotBlank()) {
                titulo = gen.text.replace("\n", " ").trim().take(50)
            }

            val newItem = HistoryItem(titulo, content, SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()))
            val currentList = _historyItems.value.toMutableList()
            currentList.add(newItem)
            withContext(Dispatchers.Main) {
                _historyItems.value = currentList
            }
            
            val file = File(context.filesDir, "instructions_history.json")
            file.writeText(gson.toJson(currentList))
        }
    }
    
    // Métodos necesarios para la UI
    private fun saveState() {
         viewModelScope.launch(Dispatchers.IO) {
            try {
                val state = AppState(_rows.value, _inputText.value, _adicionalInstructions.value)
                val file = File(context.filesDir, "state.json")
                file.writeText(gson.toJson(state))
            } catch (e: Exception) {
                Log.e("SaveState", "${e.message}")
            }
        }
    }
    
    private fun loadState() {
         viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(context.filesDir, "state.json")
                if(file.exists()){
                     val state = gson.fromJson(file.readText(), AppState::class.java)
                     withContext(Dispatchers.Main){
                         _inputText.value = state.inputText
                         _adicionalInstructions.value = state.adicionalInstructions
                         _rows.value = state.rows
                         if (state.rows.isNotEmpty()) {
                             _progressText.value = "${state.rows.size} problemas recuperados."
                         }
                     }
                }
            } catch (e: Exception) {
                Log.e("LoadState", "${e.message}")
            }
        }
    }
    
    fun deleteHistoryItem(index: Int) {
        viewModelScope.launch(Dispatchers.Main) {
            val list = _historyItems.value.toMutableList()
            if (index in list.indices) { 
                list.removeAt(index)
                _historyItems.value = list
                saveHistoryList(list) 
            }
        }
    }
    
    fun renameHistoryItem(index: Int, newTitle: String) {
        viewModelScope.launch(Dispatchers.Main) {
            val list = _historyItems.value.toMutableList()
            if (index in list.indices) { 
                list[index] = list[index].copy(titulo = newTitle)
                _historyItems.value = list
                saveHistoryList(list) 
            }
        }
    }
    
    fun clearHistory() { 
        _historyItems.value = emptyList()
        saveHistoryList(emptyList()) 
    }
    
    fun restoreFromHistory(item: HistoryItem) { 
        _adicionalInstructions.value = item.contenido
        saveState() 
    }

    private fun saveHistoryList(list: List<HistoryItem>) {
        viewModelScope.launch(Dispatchers.IO) { 
            try {
                val file = File(context.filesDir, "instructions_history.json")
                file.writeText(gson.toJson(list))
            } catch (e: Exception) {
                Log.e("SaveHistory", "${e.message}")
            }
        }
    }

    // ------------------ FUNCIONES DE EXPORTAR/IMPORTAR HISTORIAL ------------------
    
    fun exportHistory(callback: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val history = _historyItems.value
                if (history.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        callback(false, "No hay historial para exportar.")
                    }
                    return@launch
                }
                
                // Ruta absoluta definida para Android: /storage/emulated/0/Download/Scripts/🟢 Aplicaciones/
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val scriptsDir = File(downloadsDir, "Scripts/🟢 Aplicaciones")
                if (!scriptsDir.exists()) {
                    scriptsDir.mkdirs()
                }
                
                val exportFile = File(scriptsDir, "historial_backup.json")
                exportFile.writeText(gson.toJson(history), Charsets.UTF_8)
                Log.d("ExportHistory", "Historial exportado a: ${exportFile.absolutePath}")
                
                withContext(Dispatchers.Main) {
                    callback(true, "Copia de seguridad guardada en:\n${exportFile.absolutePath}")
                }
            } catch (e: Exception) {
                Log.e("ExportHistory", "Error al exportar: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    callback(false, "Error al exportar:\n${e.message}")
                }
            }
        }
    }
    
    fun importHistory(mode: String, callback: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val scriptsDir = File(downloadsDir, "Scripts/🟢 Aplicaciones")
                val importFile = File(scriptsDir, "historial_backup.json")
                
                if (!importFile.exists()) {
                    withContext(Dispatchers.Main) {
                        callback(false, "No se encontró el archivo 'historial_backup.json'\nen la ruta especificada.")
                    }
                    return@launch
                }
                
                val jsonContent = importFile.readText(Charsets.UTF_8)
                val type = object : TypeToken<List<HistoryItem>>() {}.type
                val newData: List<HistoryItem> = gson.fromJson(jsonContent, type) ?: emptyList()
                
                if (newData.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        callback(false, "El archivo de respaldo está vacío o no es válido.")
                    }
                    return@launch
                }
                
                val finalList = if (mode == "replace") {
                    newData
                } else {
                    // Fusionar: añadir todas las entradas nuevas
                    val current = withContext(Dispatchers.Main) {
                        _historyItems.value.toMutableList()
                    }
                    current.addAll(newData)
                    current
                }
                
                // Actualizar el estado en el hilo principal
                withContext(Dispatchers.Main) {
                    _historyItems.value = finalList
                }
                
                // Guardar en el hilo IO
                saveHistoryList(finalList)
                
                withContext(Dispatchers.Main) {
                    callback(true, "Historial actualizado correctamente.")
                }
            } catch (e: Exception) {
                Log.e("ImportHistory", "Error al importar: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    callback(false, "Error al importar:\n${e.message}")
                }
            }
        }
    }
    
    fun checkImportFileExists(): Boolean {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val scriptsDir = File(downloadsDir, "Scripts/🟢 Aplicaciones")
            val importFile = File(scriptsDir, "historial_backup.json")
            importFile.exists()
        } catch (e: Exception) {
            false
        }
    }
    
    fun getImportFileCount(): Int {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val scriptsDir = File(downloadsDir, "Scripts/🟢 Aplicaciones")
            val importFile = File(scriptsDir, "historial_backup.json")
            
            if (!importFile.exists()) return 0
            
            val jsonContent = importFile.readText(Charsets.UTF_8)
            val type = object : TypeToken<List<HistoryItem>>() {}.type
            val data: List<HistoryItem> = gson.fromJson(jsonContent, type) ?: emptyList()
            data.size
        } catch (e: Exception) {
            0
        }
    }
    
    // ------------------ FUNCIONES CON URI (Storage Access Framework) ------------------
    
    fun exportHistoryToUri(context: Context, uri: Uri, callback: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val history = _historyItems.value
                if (history.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        callback(false, "No hay historial para exportar.")
                    }
                    return@launch
                }
                
                val jsonContent = gson.toJson(history)
                
                context.contentResolver.openOutputStream(uri)?.use { outputStream: OutputStream ->
                    outputStream.write(jsonContent.toByteArray(Charsets.UTF_8))
                } ?: run {
                    withContext(Dispatchers.Main) {
                        callback(false, "Error al abrir el archivo para escribir.")
                    }
                    return@launch
                }
                
                Log.d("ExportHistory", "Historial exportado a URI: $uri")
                
                withContext(Dispatchers.Main) {
                    callback(true, "Copia de seguridad guardada correctamente.")
                }
            } catch (e: Exception) {
                Log.e("ExportHistory", "Error al exportar: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    callback(false, "Error al exportar:\n${e.message}")
                }
            }
        }
    }
    
    fun importHistoryFromUri(context: Context, uri: Uri, mode: String, callback: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonContent = context.contentResolver.openInputStream(uri)?.use { inputStream: InputStream ->
                    inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                } ?: run {
                    withContext(Dispatchers.Main) {
                        callback(false, "Error al abrir el archivo para leer.")
                    }
                    return@launch
                }
                
                val type = object : TypeToken<List<HistoryItem>>() {}.type
                val newData: List<HistoryItem> = gson.fromJson(jsonContent, type) ?: emptyList()
                
                if (newData.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        callback(false, "El archivo de respaldo está vacío o no es válido.")
                    }
                    return@launch
                }
                
                val finalList = if (mode == "replace") {
                    newData
                } else {
                    // Fusionar: añadir todas las entradas nuevas
                    val current = withContext(Dispatchers.Main) {
                        _historyItems.value.toMutableList()
                    }
                    current.addAll(newData)
                    current
                }
                
                // Actualizar el estado en el hilo principal
                withContext(Dispatchers.Main) {
                    _historyItems.value = finalList
                }
                
                // Guardar en el hilo IO
                saveHistoryList(finalList)
                
                withContext(Dispatchers.Main) {
                    callback(true, "Historial actualizado correctamente.")
                }
            } catch (e: Exception) {
                Log.e("ImportHistory", "Error al importar: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    callback(false, "Error al importar:\n${e.message}")
                }
            }
        }
    }
}
