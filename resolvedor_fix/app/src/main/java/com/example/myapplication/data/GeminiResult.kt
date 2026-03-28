package com.example.myapplication.data

// Clase auxiliar para devolver respuesta + tokens desde la API
data class GeminiResult(
    val text: String,
    val promptTokens: Int,
    val candidateTokens: Int,
    val totalTokens: Int
)






