package com.example.myapplication.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface GeminiApiService {
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    suspend fun generateContent(
        @Header("X-goog-api-key") apiKey: String,
        @Body request: GeminiRequest
    ): Response<GeminiResponse>
}

interface GoogleSearchApiService {
    @GET("customsearch/v1")
    suspend fun search(
        @Query("key") key: String,
        @Query("cx") cx: String,
        @Query("q") query: String
    ): Response<GoogleSearchResponse>
}

interface YouTubeApiService {
    @GET("search")
    suspend fun search(
        @Query("part") part: String = "snippet",
        @Query("q") query: String,
        @Query("maxResults") maxResults: Int = 1,
        @Query("type") type: String = "video",
        @Query("key") key: String
    ): Response<YouTubeSearchResponse>
}



