package com.example.cornell.data

import com.google.gson.annotations.SerializedName

data class Note(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("date") val date: String,
    @SerializedName("folder") val folder: String = "General",
    @SerializedName("folders") val folders: List<String> = emptyList(),
    @SerializedName("cornell_data") val cornellData: CornellData,
    @SerializedName("original_text") val originalText: String = "",
    @SerializedName("quiz_data") val quizData: QuizData? = null,
    @SerializedName("quiz_state") val quizState: QuizState? = null,
    @SerializedName("flashcards_data") val flashcardsData: FlashcardsData? = null,
    @SerializedName("flashcard_state") val flashcardState: Map<String, FlashcardState>? = null,
    @SerializedName("explanation_text") val explanationText: String = "",
    @SerializedName("explanation_score") val explanationScore: Int = -1,
    @SerializedName("explanation_label") val explanationLabel: String = "",
    @SerializedName("explanation_found") val explanationFeedbackFound: String = "",
    @SerializedName("explanation_missing") val explanationFeedbackMissing: String = "",
    @SerializedName("explanation_tip") val explanationFeedbackTip: String = ""
)

data class FlashcardState(
    @SerializedName("is_flipped") val isFlipped: Boolean = false
)
