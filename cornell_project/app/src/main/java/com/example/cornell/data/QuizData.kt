package com.example.cornell.data

import com.google.gson.annotations.SerializedName

data class QuizQuestion(
    @SerializedName("question") val question: String,
    @SerializedName("pregunta") val pregunta: String? = null,
    @SerializedName("options") val options: List<String>,
    @SerializedName("opciones") val opciones: List<String>? = null,
    @SerializedName("correct_index") val correctIndex: Int,
    @SerializedName("correct") val correct: Int? = null,
    @SerializedName("explanation") val explanation: String = "",
    @SerializedName("explicacion") val explicacion: String? = null
) {
    fun getOptionsList(): List<String> = options.ifEmpty { opciones ?: emptyList() }
    val safeCorrectIndex: Int get() = correctIndex.takeIf { it in 0..3 } ?: (correct ?: 0)
    val displayExplanation: String get() = explanation.ifBlank { explicacion ?: "" }
}

data class QuizData(
    @SerializedName("note_id") val noteId: String = "",
    @SerializedName("questions") val questions: List<QuizQuestion>,
    @SerializedName("preguntas") val preguntas: List<QuizQuestion>? = null
) {
    fun getQuestionsList(): List<QuizQuestion> = questions.ifEmpty { preguntas ?: emptyList() }
}

data class QuizState(
    @SerializedName("current_index") val currentIndex: Int = 0,
    @SerializedName("answered") val answered: List<Boolean> = emptyList(),
    @SerializedName("selected_option") val selectedOption: List<Int> = emptyList()
)
