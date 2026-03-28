package com.example.cornell.data

import com.google.gson.annotations.SerializedName

data class FlashcardItem(
    @SerializedName("pregunta") val pregunta: String,
    @SerializedName("front") val front: String? = null,
    @SerializedName("respuesta") val respuesta: String,
    @SerializedName("back") val back: String? = null,
    @SerializedName("reverso") val reverso: String? = null
) {
    val displayPregunta: String get() = pregunta.ifBlank { front ?: "" }
    val displayRespuesta: String get() = respuesta.ifBlank { back ?: reverso ?: "" }
}

data class FlashcardsData(
    @SerializedName("note_id") val noteId: String = "",
    @SerializedName("flashcards") val flashcards: List<FlashcardItem>,
    @SerializedName("tarjetas") val tarjetas: List<FlashcardItem>? = null
) {
    fun getFlashcardsList(): List<FlashcardItem> = flashcards.ifEmpty { tarjetas ?: emptyList() }
}
