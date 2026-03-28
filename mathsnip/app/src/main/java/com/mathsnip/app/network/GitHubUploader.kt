package com.mathsnip.app.network

import android.util.Base64
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

object GitHubUploader {

    private const val USERNAME = "persi3791-web"
    private const val REPO_BASE = "mathsnip-images"
    private const val MAX_REPO_SIZE_MB = 800 // Cambia de repo al llegar a 800MB

    // Token se configura desde la app
    var token: String = ""

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Sube imagen a GitHub y devuelve link raw permanente
     * Crea repos automáticamente si se llena
     */
    fun uploadImage(imageFile: File): Result<String> {
        if (token.isEmpty()) {
            return Result.failure(Exception("GitHub token no configurado"))
        }

        return try {
            // Buscar repo disponible (que no esté lleno)
            val repoName = findAvailableRepo()

            // Asegurarse que el repo existe
            ensureRepoExists(repoName)

            // Subir imagen
            val fileName = "img_${System.currentTimeMillis()}.jpg"
            val bytes = imageFile.readBytes()
            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

            val json = JSONObject()
                .put("message", "Add image $fileName")
                .put("content", b64)
                .toString()

            val request = Request.Builder()
                .url("https://api.github.com/repos/$USERNAME/$repoName/contents/$fileName")
                .addHeader("Authorization", "token $token")
                .addHeader("Accept", "application/vnd.github.v3+json")
                .put(json.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return Result.failure(Exception("Respuesta vacía"))

            if (response.isSuccessful) {
                val link = "https://raw.githubusercontent.com/$USERNAME/$repoName/main/$fileName"
                Result.success(link)
            } else {
                Result.failure(Exception("Error GitHub: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Encuentra el repo disponible o crea uno nuevo
     */
    private fun findAvailableRepo(): String {
        var index = 1
        while (true) {
            val repoName = if (index == 1) REPO_BASE else "$REPO_BASE-$index"
            val sizeMB = getRepoSizeMB(repoName)

            when {
                sizeMB < 0 -> return repoName // Repo no existe aún, usar este
                sizeMB < MAX_REPO_SIZE_MB -> return repoName // Tiene espacio
                else -> index++ // Lleno, probar el siguiente
            }
        }
    }

    /**
     * Obtiene el tamaño del repo en MB. Devuelve -1 si no existe.
     */
    private fun getRepoSizeMB(repoName: String): Int {
        return try {
            val request = Request.Builder()
                .url("https://api.github.com/repos/$USERNAME/$repoName")
                .addHeader("Authorization", "token $token")
                .addHeader("Accept", "application/vnd.github.v3+json")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return -1

            val body = response.body?.string() ?: return -1
            val json = JSONObject(body)
            val sizeKB = json.optInt("size", 0)
            sizeKB / 1024 // Convertir KB a MB
        } catch (e: Exception) { -1 }
    }

    /**
     * Crea el repo si no existe
     */
    private fun ensureRepoExists(repoName: String) {
        val sizeMB = getRepoSizeMB(repoName)
        if (sizeMB >= 0) return // Ya existe

        val json = JSONObject()
            .put("name", repoName)
            .put("description", "MathSnip images storage")
            .put("private", false) // Público para links directos
            .put("auto_init", true)
            .toString()

        val request = Request.Builder()
            .url("https://api.github.com/user/repos")
            .addHeader("Authorization", "token $token")
            .addHeader("Accept", "application/vnd.github.v3+json")
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute()

        // Esperar un momento para que GitHub inicialice el repo
        Thread.sleep(2000)
    }

    /**
     * Verifica que el token es válido
     */
    fun verifyToken(): Boolean {
        return try {
            val request = Request.Builder()
                .url("https://api.github.com/user")
                .addHeader("Authorization", "token $token")
                .get()
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) { false }
    }
}
