package com.example.myapplication.data.api

import com.google.gson.annotations.SerializedName

data class GeminiRequest(
    val contents: List<Content>
)

data class Content(
    val parts: List<Part>
)

data class Part(
    val text: String? = null,
    @SerializedName("inline_data")
    val inlineData: InlineData? = null
)

data class InlineData(
    @SerializedName("mime_type")
    val mimeType: String,
    val data: String
)

data class GeminiResponse(
    val candidates: List<Candidate>? = null,
    val usageMetadata: UsageMetadata? = null
)

data class Candidate(
    val content: ContentResponse
)

data class ContentResponse(
    val parts: List<PartResponse>
)

data class PartResponse(
    val text: String? = null
)

data class UsageMetadata(
    @SerializedName("promptTokenCount")
    val promptTokenCount: Int = 0,
    @SerializedName("candidatesTokenCount")
    val candidatesTokenCount: Int = 0,
    @SerializedName("totalTokenCount")
    val totalTokenCount: Int = 0
)



