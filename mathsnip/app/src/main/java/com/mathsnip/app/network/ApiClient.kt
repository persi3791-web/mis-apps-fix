package com.mathsnip.app.network

import android.util.Base64
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val URL = "https://josepe200517-mathocr.hf.space"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun checkStatus(): Boolean {
        repeat(2) { attempt ->
            try {
                val req = Request.Builder().url("$URL/").get().build()
                val resp = client.newCall(req).execute()
                val body = resp.body?.string() ?: return@repeat
                val json = JSONObject(body)
                if (json.getString("status") == "MathOCR API running") return true
            } catch (e: Exception) {
                if (attempt == 0) Thread.sleep(3000)
            }
        }
        return false
    }

    fun recognize(imageFile: File): Result<String> {
        return try {
            val bytes = imageFile.readBytes()
            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val json = JSONObject().put("image", b64).toString()
            val body = json.toRequestBody("application/json".toMediaType())
            val req = Request.Builder().url("$URL/predict").post(body).build()
            val resp = client.newCall(req).execute()
            val respBody = resp.body?.string() ?: return Result.failure(Exception("Empty response"))
            val result = JSONObject(respBody)
            val latex = result.optString("latex", "")
            if (latex.isNotEmpty()) Result.success(latex)
            else Result.failure(Exception(result.optString("error", "No recognized")))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
