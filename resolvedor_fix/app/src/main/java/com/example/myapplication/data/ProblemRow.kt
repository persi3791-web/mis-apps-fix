package com.example.myapplication.data

data class ProblemRow(
    val numeroAvion: String,
    val problem: String,
    val enlaceImagen: String = "", // La URL original
    var enlaceImagenReal: String = "", // La URL resuelta (directa a la imagen)
    var enlaceGoogle: String = "",
    var enlaceYoutube: String = "",
    var solucion: String = "",
    var mnemotecnia: String = "",
    
    // --- Campos para tokens ---
    var tokensPrompt: Int = 0,
    var tokensRespuesta: Int = 0,
    var tokensTotal: Int = 0,
    var tokensPromptMnemo: Int = 0,
    var tokensRespuestaMnemo: Int = 0,
    var tokensTotalMnemo: Int = 0
)

data class ProblemResult(
    val numeroAvion: String,
    val problema: String,
    val enlaceImagen: String,
    val enlaceGoogle: String,
    val enlaceYoutube: String,
    val solucion: String,
    val mnemotecnia: String,
    val tokensPrompt: Int = 0,
    val tokensRespuesta: Int = 0,
    val tokensTotal: Int = 0,
    val tokensPromptMnemo: Int = 0,
    val tokensRespuestaMnemo: Int = 0,
    val tokensTotalMnemo: Int = 0
)

data class HistoryItem(
    var titulo: String,
    val contenido: String,
    val fecha: String
)

data class AppState(
    val rows: List<ProblemRow> = emptyList(),
    val inputText: String = "",
    val adicionalInstructions: String = ""
)



