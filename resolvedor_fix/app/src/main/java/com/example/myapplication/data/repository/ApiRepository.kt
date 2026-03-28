package com.example.myapplication.data.repository

import android.util.Base64
import com.example.myapplication.data.api.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.FieldNamingPolicy
import java.io.File
import java.util.concurrent.TimeUnit

class ApiRepository {
    
    companion object {
        val API_KEYS_GEMINI = listOf(
            "AIzaSyBH-4FcDt9vpEJlDwxMeCW1QigrtF3Zt2k",
            "AIzaSyDnl11aCnK7qOE-K9viBBJim0QD_UrF5uM"
        )
        
        val API_URL_GEMINI = "https://generativelanguage.googleapis.com/v1beta/"
        
        val GOOGLE_API_KEYS = listOf(
            "AIzaSyBuzM-cCtTJ9cplHi7OTbEaSBhrAGx7dBA",
            "AIzaSyDvYMA-M5UsNn2n8kMNwl8AW5L7BgKZF6A"
        )
        
        val CX = "96728b66d8b3841df"
        
        val YOUTUBE_API_KEYS = listOf(
            "AIzaSyDDOez2klqoPhFOWNaBiiuSFhxRDWDill8",
            "AIzaSyBwbnsB7Uk-M464-rxhY2aG6mDPL5ZFZSI"
        )
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(90, TimeUnit.SECONDS)  // Aumentado para dar más tiempo
        .readTimeout(90, TimeUnit.SECONDS)     // Aumentado para dar más tiempo
        .writeTimeout(90, TimeUnit.SECONDS)    // Agregado write timeout
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .retryOnConnectionFailure(true)  // Reintentar en fallos de conexión
        .build()
    
    // Gson sin FieldNamingPolicy - usamos @SerializedName explícitamente donde sea necesario
    private val gson = GsonBuilder()
        .create()
    
    private val geminiRetrofit = Retrofit.Builder()
        .baseUrl(API_URL_GEMINI)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
    
    private val googleRetrofit = Retrofit.Builder()
        .baseUrl("https://www.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    private val youtubeRetrofit = Retrofit.Builder()
        .baseUrl("https://www.googleapis.com/youtube/v3/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    private val geminiApi = geminiRetrofit.create(GeminiApiService::class.java)
    private val googleApi = googleRetrofit.create(GoogleSearchApiService::class.java)
    private val youtubeApi = youtubeRetrofit.create(YouTubeApiService::class.java)
    
    suspend fun callGeminiApi(
        problem: String,
        additionalInstructions: String = "",
        images: List<ImageData> = emptyList(),
        maxRetries: Int = 8,
        delayMs: Long = 5000
    ): Pair<String?, UsageMetadata> {
        val instruccionesExtra = if (additionalInstructions.isNotEmpty()) {
            "Instrucciones extra para resolver:\n$additionalInstructions\n\n"
        } else ""
        
        val parts = mutableListOf<Part>()
        parts.add(Part(text = instruccionesExtra +
            "Eres un experto matemático que resuelve problemas mostrando solo el procedimiento, " +
            "usando fórmulas delimitadas con \\( ... \\) para inline y \\[ ... \\] para bloques. " +
            "No agregues texto explicativo adicional.\n\n$problem"))
        
        images.forEach { img ->
            parts.add(Part(inlineData = InlineData(
                mimeType = img.mimeType,
                data = img.base64Data
            )))
        }
        
        val request = GeminiRequest(listOf(Content(parts)))
        
        // Log del JSON que se envía (para debugging) - exactamente como Python
        val requestJson = gson.toJson(request)
        android.util.Log.d("GeminiApi", "=== INICIANDO LLAMADA GEMINI (SOLUCIÓN) ===")
        android.util.Log.d("GeminiApi", "Request JSON completo: $requestJson")
        android.util.Log.d("GeminiApi", "Número de imágenes: ${images.size}")
        android.util.Log.d("GeminiApi", "Max retries: $maxRetries, Delay: ${delayMs}ms")
        
        // Bucle de FAILOVER (igual que Python)
        for (apiKey in API_KEYS_GEMINI) {
            android.util.Log.d("GeminiApi", "Usando clave Gemini (Solución): ${apiKey.take(8)}...")
            
            // Bucle de REINTENTO (EXACTAMENTE como Python)
            for (attempt in 1..maxRetries) {
                try {
                    android.util.Log.d("GeminiApi", "Intento $attempt/$maxRetries con clave ${apiKey.take(8)}...")
                    val response = geminiApi.generateContent(apiKey, request)
                    val code = response.code()
                    
                    // EXACTAMENTE como Python: primero verificar errores 400-499 (excepto 429)
                    if (code in 400..499 && code != 429) {
                        android.util.Log.w("GeminiApi", "Error de cliente $code con clave ${apiKey.take(8)}. Probando siguiente clave.")
                        break // Rompe bucle de reintento, va a la siguiente CLAVE (igual que Python)
                    }
                    
                    // EXACTAMENTE como Python: si es 500+ o 429, reintentar
                    if (code >= 500 || code == 429) {
                        android.util.Log.d("GeminiApi", "Intento $attempt/$maxRetries: Error $code con clave ${apiKey.take(8)}. Reintentando en ${delayMs}ms...")
                        if (attempt < maxRetries) {
                            kotlinx.coroutines.delay(delayMs)
                            continue // Siguiente reintento (misma clave) - igual que Python
                        }
                    }
                    
                    // Si llegamos aquí, la respuesta es exitosa (2xx)
                    if (response.isSuccessful) {
                        val body = response.body()
                        android.util.Log.d("GeminiApi", "Respuesta exitosa. Body: ${body != null}")
                        if (body != null) {
                            val candidates = body.candidates
                            val usage = body.usageMetadata
                            android.util.Log.d("GeminiApi", "Candidates: ${candidates?.size ?: 0}")
                            
                            // EXACTAMENTE como Python: verificar candidates y content
                            if (candidates != null && candidates.isNotEmpty() && candidates[0].content != null) {
                                val partsResp = candidates[0].content.parts
                                android.util.Log.d("GeminiApi", "Parts en respuesta: ${partsResp.size}")
                                
                                // EXACTAMENTE como Python: buscar texto en parts
                                for (p in partsResp) {
                                    if (!p.text.isNullOrBlank() && p.text.trim().isNotEmpty()) {
                                        android.util.Log.d("GeminiApi", "✅ SOLUCIÓN GENERADA EXITOSAMENTE. Longitud: ${p.text.length}")
                                        return Pair(p.text, usage ?: UsageMetadata())
                                    }
                                }
                                android.util.Log.w("GeminiApi", "Todos los parts están vacíos o null")
                            } else {
                                android.util.Log.w("GeminiApi", "No hay candidates o content en la respuesta")
                            }
                        } else {
                            android.util.Log.w("GeminiApi", "Body de respuesta es null")
                        }
                        
                        // EXACTAMENTE como Python: si respuesta exitosa pero sin texto, reintentar
                        android.util.Log.d("GeminiApi", "Intento $attempt: respuesta incompleta, reintentando...")
                        if (attempt < maxRetries) {
                            kotlinx.coroutines.delay(delayMs)
                            continue
                        }
                    }
                } catch (e: Exception) {
                    // EXACTAMENTE como Python: capturar RequestException y reintentar
                    android.util.Log.e("GeminiApi", "Intento $attempt/$maxRetries: Error de red/timeout: ${e.message}. Reintentando en ${delayMs}ms...")
                    if (attempt < maxRetries) {
                        kotlinx.coroutines.delay(delayMs)
                        continue
                    }
                }
            }
            
            android.util.Log.w("GeminiApi", "Clave ${apiKey.take(8)} falló después de $maxRetries intentos.")
        }
        
        android.util.Log.e("GeminiApi", "Todas las claves fallaron para generar solución")
        return Pair(null, UsageMetadata())
    }
    
    suspend fun callGeminiForMnemonics(
        solutionText: String,
        maxRetries: Int = 8,
        delayMs: Long = 5000
    ): Pair<String?, UsageMetadata> {
        // Prompt exactamente igual al código Python
        val prompt = "Eres un experto en el lore de League of Legends. Tu tarea es crear una mnemotecnia para memorizar la siguiente solución matemática, que está dividida en pasos (1., 2., 3., etc.).\n\n" +
            "Reglas estrictas:\n" +
            "1. Usa personajes, habilidades, objetos o lore de League of Legends.\n" +
            "2. Cada mnemotecnia para un paso (ej: 1.) debe conectarse con la del paso anterior, creando una historia continua.\n" +
            "3. El estilo debe ser directo, como una acción o un hecho. (Ejemplo: \"1. Veigar calcula la velocidad de su Q...\", \"2. La concentración de mana de Cassiopeia...\").\n" +
            "4. NO uses frases como \"Recuerda...\", \"Piensa en...\" ni similares.\n" +
            "5. NO uses ningún separador como \"---\".\n" +
            "6. Responde ÚNICAMENTE con la mnemotecnia, empezando por \"1. ...\".\n\n" +
            "Solución a memorizar:\n===\n$solutionText\n===\nGenera la mnemotecnia ahora:"
        
        val request = GeminiRequest(listOf(Content(listOf(Part(text = prompt)))))
        
        // Log del JSON que se envía (para debugging) - exactamente como Python
        val requestJson = gson.toJson(request)
        android.util.Log.d("GeminiMnemo", "=== INICIANDO LLAMADA GEMINI (MNEMOTECNIA) ===")
        android.util.Log.d("GeminiMnemo", "Request JSON completo: $requestJson")
        android.util.Log.d("GeminiMnemo", "Longitud del prompt: ${prompt.length}")
        android.util.Log.d("GeminiMnemo", "Max retries: $maxRetries, Delay: ${delayMs}ms")
        
        // Bucle de FAILOVER (igual que Python)
        for (apiKey in API_KEYS_GEMINI) {
            android.util.Log.d("GeminiMnemo", "Usando clave Gemini (Mnemotecnia): ${apiKey.take(8)}...")
            
            // Bucle de REINTENTO (EXACTAMENTE como Python)
            for (attempt in 1..maxRetries) {
                try {
                    android.util.Log.d("GeminiMnemo", "Intento $attempt/$maxRetries con clave ${apiKey.take(8)}...")
                    val response = geminiApi.generateContent(apiKey, request)
                    val code = response.code()
                    
                    // EXACTAMENTE como Python: primero verificar errores 400-499 (excepto 429)
                    if (code in 400..499 && code != 429) {
                        android.util.Log.w("GeminiMnemo", "Error de cliente $code con clave ${apiKey.take(8)}. Probando siguiente clave.")
                        break // Rompe bucle de reintento, va a la siguiente CLAVE (igual que Python)
                    }
                    
                    // EXACTAMENTE como Python: si es 500+ o 429, reintentar
                    if (code >= 500 || code == 429) {
                        android.util.Log.d("GeminiMnemo", "Intento $attempt/$maxRetries (Mnemotecnia): Error $code con clave ${apiKey.take(8)}. Reintentando en ${delayMs}ms...")
                        if (attempt < maxRetries) {
                            kotlinx.coroutines.delay(delayMs)
                            continue // Siguiente reintento (misma clave) - igual que Python
                        }
                    }
                    
                    // Si llegamos aquí, la respuesta es exitosa (2xx)
                    if (response.isSuccessful) {
                        val body = response.body()
                        android.util.Log.d("GeminiMnemo", "Respuesta exitosa. Body: ${body != null}")
                        if (body != null) {
                            val candidates = body.candidates
                            val usage = body.usageMetadata
                            android.util.Log.d("GeminiMnemo", "Candidates: ${candidates?.size ?: 0}")
                            
                            // EXACTAMENTE como Python: verificar candidates y content
                            if (candidates != null && candidates.isNotEmpty() && candidates[0].content != null) {
                                val partsResp = candidates[0].content.parts
                                android.util.Log.d("GeminiMnemo", "Parts en respuesta: ${partsResp.size}")
                                
                                // EXACTAMENTE como Python: buscar texto en parts
                                for (p in partsResp) {
                                    if (!p.text.isNullOrBlank() && p.text.trim().isNotEmpty()) {
                                        android.util.Log.d("GeminiMnemo", "✅ MNEMOTECNIA GENERADA EXITOSAMENTE. Longitud: ${p.text.length}")
                                        return Pair(p.text, usage ?: UsageMetadata())
                                    }
                                }
                                android.util.Log.w("GeminiMnemo", "Todos los parts están vacíos o null")
                            } else {
                                android.util.Log.w("GeminiMnemo", "No hay candidates o content en la respuesta")
                            }
                        } else {
                            android.util.Log.w("GeminiMnemo", "Body de respuesta es null")
                        }
                        
                        // EXACTAMENTE como Python: si respuesta exitosa pero sin texto, reintentar
                        android.util.Log.d("GeminiMnemo", "Intento $attempt (Mnemotecnia): respuesta incompleta...")
                        if (attempt < maxRetries) {
                            kotlinx.coroutines.delay(delayMs)
                            continue
                        }
                    }
                } catch (e: Exception) {
                    // EXACTAMENTE como Python: capturar RequestException y reintentar
                    android.util.Log.e("GeminiMnemo", "Intento $attempt/$maxRetries (Mnemotecnia): Error de red/timeout: ${e.message}. Reintentando en ${delayMs}ms...")
                    if (attempt < maxRetries) {
                        kotlinx.coroutines.delay(delayMs)
                        continue
                    }
                }
            }
            
            android.util.Log.w("GeminiMnemo", "Clave ${apiKey.take(8)} (Mnemotecnia) falló después de $maxRetries intentos.")
        }
        
        android.util.Log.e("GeminiMnemo", "Todas las claves fallaron para generar mnemotecnia")
        return Pair(null, UsageMetadata())
    }
    
    suspend fun generateTitle(content: String): String {
        val prompt = "Resume este texto en máximo 6 palabras para usarlo como título: $content"
        val request = GeminiRequest(listOf(Content(listOf(Part(text = prompt)))))
        
        for (apiKey in API_KEYS_GEMINI) {
            try {
                val response = geminiApi.generateContent(apiKey, request)
                if (response.isSuccessful) {
                    val body = response.body()
                    val candidates = body?.candidates
                    if (candidates != null && candidates.isNotEmpty()) {
                        val text = candidates[0].content.parts.firstOrNull()?.text
                        if (!text.isNullOrBlank()) {
                            return text.trim().replace("\n", " ")
                        }
                    }
                }
            } catch (e: Exception) {
                continue
            }
        }
        return "Instrucción sin título"
    }
    
    suspend fun searchGoogle(query: String): String {
        for (apiKey in GOOGLE_API_KEYS) {
            try {
                val response = googleApi.search(apiKey, CX, query)
                if (response.isSuccessful) {
                    val body = response.body()
                    val items = body?.items
                    if (items != null && items.isNotEmpty()) {
                        return items[0].link
                    }
                }
            } catch (e: Exception) {
                continue
            }
        }
        return "No encontrado"
    }
    
    suspend fun searchYouTube(query: String): String {
        for (apiKey in YOUTUBE_API_KEYS) {
            try {
                val response = youtubeApi.search(query = query, key = apiKey)
                if (response.isSuccessful) {
                    val body = response.body()
                    val items = body?.items
                    if (items != null && items.isNotEmpty()) {
                        val videoId = items[0].id.videoId
                        return "https://www.youtube.com/watch?v=$videoId"
                    }
                }
            } catch (e: Exception) {
                continue
            }
        }
        return "Video no disponible"
    }
}

data class ImageData(
    val base64Data: String,
    val mimeType: String
)



