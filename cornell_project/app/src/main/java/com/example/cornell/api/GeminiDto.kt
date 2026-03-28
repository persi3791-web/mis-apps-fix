package com.example.cornell.api

import com.google.gson.annotations.SerializedName

data class GeminiGenerateRequest(
    val contents: List<ContentItem>,
    val generationConfig: GenerationConfig,
    val safetySettings: List<SafetySetting>
)

data class ContentItem(
    val role: String? = null,
    val parts: List<Part>
)

data class Part(
    val text: String
)

data class GenerationConfig(
    val temperature: Double = 0.7,
    val topK: Int = 40,
    val topP: Double = 0.95,
    @SerializedName("maxOutputTokens") val maxOutputTokens: Int = 2048
)

data class SafetySetting(
    val category: String,
    val threshold: String
)

data class GeminiGenerateResponse(
    val candidates: List<Candidate>?
)

data class Candidate(
    val content: Content?,
    @SerializedName("finishReason") val finishReason: String?
)

data class Content(
    val parts: List<Part>?
)
