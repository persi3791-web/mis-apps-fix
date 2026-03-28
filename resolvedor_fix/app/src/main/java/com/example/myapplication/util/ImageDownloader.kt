package com.example.myapplication.util

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object ImageDownloader {
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
    
    fun extractUrlFromMarkdown(text: String): String {
        if (text.isEmpty()) return ""
        
        // Buscar patrón markdown ![](URL)
        val markdownPattern = Pattern.compile("!\\[.*?\\]\\((https?://[^\\)\\s]+)\\)")
        val markdownMatcher = markdownPattern.matcher(text)
        if (markdownMatcher.find()) {
            return markdownMatcher.group(1)?.trim() ?: ""
        }
        
        // Si no hay markdown, capturar primera URL
        val urlPattern = Pattern.compile("(https?://\\S+)")
        val urlMatcher = urlPattern.matcher(text)
        if (urlMatcher.find()) {
            return urlMatcher.group(1)?.trim()?.trimEnd(')', '.', ';', ',') ?: ""
        }
        
        return ""
    }
    
    suspend fun resolveRealImageUrl(possibleUrl: String): String? = withContext(Dispatchers.IO) {
        if (possibleUrl.isEmpty()) return@withContext null
        
        try {
            val lower = possibleUrl.lowercase()
            val imageExtensions = listOf(".png", ".jpg", ".jpeg", ".webp", ".gif", ".bmp", ".tiff")
            if (imageExtensions.any { lower.endsWith(it) }) {
                return@withContext possibleUrl
            }
            
            // Intentar HEAD request
            try {
                val headRequest = Request.Builder().url(possibleUrl).head().build()
                val headResponse = okHttpClient.newCall(headRequest).execute()
                val contentType = headResponse.header("Content-Type") ?: ""
                if (contentType.contains("image", ignoreCase = true)) {
                    return@withContext possibleUrl
                }
            } catch (e: Exception) {
                // Continue
            }
            
            // Obtener la página y buscar <img src=...>
            val request = Request.Builder().url(possibleUrl).build()
            val response = okHttpClient.newCall(request).execute()
            val contentType = response.header("Content-Type") ?: ""
            if (contentType.contains("image", ignoreCase = true)) {
                return@withContext possibleUrl
            }
            
            val html = response.body?.string() ?: return@withContext null
            val imgPattern = Pattern.compile("<img[^>]+src=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE)
            val imgMatcher = imgPattern.matcher(html)
            if (imgMatcher.find()) {
                val src = imgMatcher.group(1)
                val baseUrl = URL(possibleUrl)
                val resolvedUrl = URL(baseUrl, src)
                return@withContext resolvedUrl.toString()
            }
            
            // Buscar og:image
            val ogPattern = Pattern.compile("<meta[^>]+property=[\"']og:image[\"'][^>]+content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE)
            val ogMatcher = ogPattern.matcher(html)
            if (ogMatcher.find()) {
                val src = ogMatcher.group(1)
                val baseUrl = URL(possibleUrl)
                val resolvedUrl = URL(baseUrl, src)
                return@withContext resolvedUrl.toString()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        null
    }
    
    suspend fun downloadImageToBase64(url: String, tempDir: File): Pair<String?, String?> = withContext(Dispatchers.IO) {
        if (url.isEmpty()) return@withContext Pair(null, null)
        
        try {
            if (!tempDir.exists()) {
                tempDir.mkdirs()
            }
            
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext Pair(null, null)
            }
            
            val contentType = response.header("Content-Type") ?: "image/jpeg"
            val body = response.body?.bytes() ?: return@withContext Pair(null, null)
            
            val base64 = Base64.encodeToString(body, Base64.NO_WRAP)
            val mimeType = contentType.split(";")[0].trim()
            
            return@withContext Pair(base64, mimeType)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Pair(null, null)
        }
    }
}


