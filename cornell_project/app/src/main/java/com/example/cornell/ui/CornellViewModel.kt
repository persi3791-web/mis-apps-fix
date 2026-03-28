package com.example.cornell.ui

import android.app.Application
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cornell.api.GeminiRepository
import com.example.cornell.data.CornellData
import com.example.cornell.data.DataManager
import com.example.cornell.data.FlashcardItem
import com.example.cornell.data.FlashcardsData
import com.example.cornell.data.FlashcardState
import com.example.cornell.data.Note
import com.example.cornell.data.QuizData
import com.example.cornell.data.QuizQuestion
import com.example.cornell.data.QuizState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CornellUiState(
    val notes: List<Note> = emptyList(),
    val folders: List<String> = emptyList(),
    val currentNote: Note? = null,
    val isLoading: Boolean = false,
    val isNoteEditing: Boolean = false,
    val toastMessage: String? = null,
    val navigateToNoteId: String? = null,
    val quizQuestions: List<QuizQuestion> = emptyList(),
    val quizCurrentIndex: Int = 0,
    val quizAnswered: List<Boolean> = emptyList(),
    val quizSelectedOption: List<Int> = emptyList(), // índice de opción elegida por pregunta (-1 = ninguna)
    val quizStatusMessage: String = "Todavía no se ha generado el cuestionario.",
    val flashcards: List<FlashcardItem> = emptyList(),
    val flashcardFlipped: Map<Int, Boolean> = emptyMap(),
    val chatMessages: List<Pair<String, String>> = emptyList(), // role to text
    val isChatLoading: Boolean = false,
    // Explicación (tab Estudiar)
    val explanationText: String = "",
    val explanationPartial: String = "",
    val explanationScore: Int = -1,
    val explanationScoreLabel: String = "",
    val explanationFeedbackFound: String = "",
    val explanationFeedbackMissing: String = "",
    val explanationFeedbackTip: String = "",
    val isExplanationEvaluating: Boolean = false,
    val explanationVoiceStatus: String = "Toca 🎙 para empezar a hablar",
    val explanationMicLevel: Float = 0f
)

class CornellViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CornellUiState())
    val uiState: StateFlow<CornellUiState> = _uiState.asStateFlow()

    init {
        try {
            DataManager.init(application)
            loadAll()
        } catch (_: Throwable) {
            _uiState.value = CornellUiState()
        }
    }

    private fun loadAll() {
        _uiState.update {
            it.copy(
                notes = DataManager.loadNotes(),
                folders = DataManager.loadFolders()
            )
        }
    }

    fun loadNotesForFolder(folder: String?) {
        loadAll()
    }

    fun createFolder(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            _uiState.update { it.copy(toastMessage = "Escribe un nombre") }
            return
        }
        val current = _uiState.value.folders
        if (trimmed in current) {
            _uiState.update { it.copy(toastMessage = "La carpeta ya existe") }
            return
        }
        val updated = current + trimmed
        DataManager.saveFolders(updated)
        _uiState.update { it.copy(folders = updated, toastMessage = "Carpeta creada") }
    }

    fun renameFolder(oldName: String, newName: String): Boolean {
        val trimmed = newName.trim()
        if (trimmed.isBlank() || trimmed == oldName) return false
        if (trimmed in _uiState.value.folders) return false
        val newFolders = _uiState.value.folders.map { if (it == oldName) trimmed else it }
        DataManager.saveFolders(newFolders)
        val newNotes = _uiState.value.notes.map { n ->
            if (n.folder == oldName) n.copy(folder = trimmed) else n
        }
        DataManager.saveNotes(newNotes)
        _uiState.update {
            it.copy(folders = newFolders, notes = newNotes, toastMessage = "Carpeta renombrada")
        }
        return true
    }

    fun deleteFolder(folderName: String) {
        val newNotes = _uiState.value.notes.map { n ->
            if (n.folder == folderName) n.copy(folder = "General") else n
        }
        DataManager.saveNotes(newNotes)
        val newFolders = _uiState.value.folders.filter { it != folderName }
        DataManager.saveFolders(newFolders)
        _uiState.update {
            it.copy(notes = newNotes, folders = newFolders, toastMessage = "Carpeta eliminada")
        }
    }

    fun openNote(noteId: String) {
        try {
            val currentNoteId = _uiState.value.currentNote?.id
            if (currentNoteId != null && currentNoteId != noteId) {
                saveExplanationToNote()
            }
            val note = _uiState.value.notes.find { it.id == noteId } ?: run {
                _uiState.update { it.copy(toastMessage = "Nota no encontrada") }
                return
            }
            val quizList = try { note.quizData?.getQuestionsList() ?: emptyList() } catch (_: Exception) { emptyList() }
            val quizState = try { note.quizState ?: QuizState(0, List(quizList.size) { false }, emptyList()) } catch (_: Exception) { QuizState(0, emptyList(), emptyList()) }
            val flashList = try { note.flashcardsData?.getFlashcardsList() ?: emptyList() } catch (_: Exception) { emptyList() }
            val flipped = try {
                (note.flashcardState ?: emptyMap()).mapKeys { it.key.toIntOrNull() ?: -1 }
                    .filter { it.key >= 0 }
                    .mapValues { it.value.isFlipped }
            } catch (_: Exception) { emptyMap() }
            
            // Asegurar valores por defecto seguros para campos de explicación
            val explanationText = note.explanationText ?: ""
            val explanationScore = if (note.explanationScore < -1 || note.explanationScore > 100) -1 else note.explanationScore
            val explanationLabel = note.explanationLabel ?: ""
            val explanationFeedbackFound = note.explanationFeedbackFound ?: ""
            val explanationFeedbackMissing = note.explanationFeedbackMissing ?: ""
            val explanationFeedbackTip = note.explanationFeedbackTip ?: ""
            
            _uiState.update {
                it.copy(
                    currentNote = note,
                    quizQuestions = quizList,
                    quizCurrentIndex = quizState.currentIndex.coerceIn(0, (quizList.size - 1).coerceAtLeast(0)),
                    quizAnswered = quizState.answered,
                    quizSelectedOption = quizState.selectedOption.take(quizList.size).let { list ->
                        if (list.size == quizList.size) list else list + List(quizList.size - list.size) { -1 }
                    },
                    quizStatusMessage = if (quizList.isEmpty()) "Todavía no se ha generado el cuestionario." else "",
                    flashcards = flashList,
                    flashcardFlipped = flipped,
                    chatMessages = emptyList(),
                    explanationText = explanationText,
                    explanationPartial = "",
                    explanationScore = explanationScore,
                    explanationScoreLabel = explanationLabel,
                    explanationFeedbackFound = explanationFeedbackFound,
                    explanationFeedbackMissing = explanationFeedbackMissing,
                    explanationFeedbackTip = explanationFeedbackTip,
                    explanationVoiceStatus = "Toca 🎙 para empezar a hablar",
                    explanationMicLevel = 0f
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.update { it.copy(toastMessage = "Error al abrir la nota: ${e.message}") }
        }
    }

    fun clearCurrentNote() {
        saveExplanationToNote()
        _uiState.update {
            it.copy(
                currentNote = null,
                quizQuestions = emptyList(),
                quizCurrentIndex = 0,
                quizAnswered = emptyList(),
                quizSelectedOption = emptyList(),
                quizStatusMessage = "Todavía no se ha generado el cuestionario.",
                flashcards = emptyList(),
                flashcardFlipped = emptyMap(),
                chatMessages = emptyList(),
                explanationText = "",
                explanationPartial = "",
                explanationScore = -1,
                explanationScoreLabel = "",
                explanationFeedbackFound = "",
                explanationFeedbackMissing = "",
                explanationFeedbackTip = "",
                explanationVoiceStatus = "Toca 🎙 para empezar a hablar",
                explanationMicLevel = 0f
            )
        }
    }

    fun generateCornell(userText: String, title: String, folder: String) {
        if (userText.isBlank()) {
            _uiState.update { it.copy(toastMessage = "Escribe tus apuntes") }
            return
        }
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val data = GeminiRepository.generateCornellFromText(userText, title)
            if (data != null) {
                val id = System.currentTimeMillis().toString()
                val date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
                val note = Note(
                    id = id,
                    title = data.titulo.ifBlank { title }.ifBlank { "Sin título" },
                    date = date,
                    folder = folder.ifBlank { "General" },
                    cornellData = data,
                    originalText = userText
                )
                val newNotes = _uiState.value.notes + note
                DataManager.saveNotes(newNotes)
                _uiState.update {
                    it.copy(
                        notes = newNotes,
                        currentNote = note,
                        isLoading = false,
                        navigateToNoteId = note.id,
                        quizQuestions = emptyList(),
                        quizAnswered = emptyList(),
                        flashcards = emptyList(),
                        flashcardFlipped = emptyMap(),
                        chatMessages = emptyList()
                    )
                }
            } else {
                _uiState.update {
                    it.copy(isLoading = false, toastMessage = "Error al generar la nota")
                }
            }
        }
    }

    fun saveCurrentNoteEdits(title: String, ideasClave: String, notasClase: String, resumen: String) {
        val note = _uiState.value.currentNote ?: return
        val updated = note.copy(
            title = title,
            cornellData = note.cornellData.copy(
                titulo = title,
                ideasClave = ideasClave,
                notasClase = notasClase,
                resumen = resumen
            )
        )
        val newNotes = _uiState.value.notes.map { if (it.id == note.id) updated else it }
        DataManager.saveNotes(newNotes)
        _uiState.update {
            it.copy(notes = newNotes, currentNote = updated, isNoteEditing = false, toastMessage = "Nota guardada")
        }
    }

    fun setNoteEditing(editing: Boolean) {
        _uiState.update { it.copy(isNoteEditing = editing) }
    }

    fun moveNoteToFolder(noteId: String, folderName: String) {
        val newNotes = _uiState.value.notes.map { n ->
            if (n.id == noteId) n.copy(folder = folderName) else n
        }
        DataManager.saveNotes(newNotes)
        val updated = _uiState.value.currentNote
        _uiState.update {
            it.copy(
                notes = newNotes,
                currentNote = if (updated?.id == noteId) updated.copy(folder = folderName) else updated,
                toastMessage = "Nota movida a $folderName"
            )
        }
    }

    fun renameNote(noteId: String, newTitle: String) {
        val t = newTitle.trim()
        if (t.isBlank()) return
        val newNotes = _uiState.value.notes.map { n ->
            if (n.id == noteId) {
                n.copy(
                    title = t,
                    cornellData = n.cornellData.copy(titulo = t)
                )
            } else n
        }
        DataManager.saveNotes(newNotes)
        val updated = _uiState.value.currentNote
        _uiState.update {
            it.copy(
                notes = newNotes,
                currentNote = if (updated?.id == noteId) updated.copy(title = t, cornellData = updated.cornellData.copy(titulo = t)) else updated,
                toastMessage = "Nota renombrada"
            )
        }
    }

    fun deleteNote(noteId: String) {
        val newNotes = _uiState.value.notes.filter { it.id != noteId }
        DataManager.saveNotes(newNotes)
        _uiState.update {
            it.copy(
                notes = newNotes,
                currentNote = if (it.currentNote?.id == noteId) null else it.currentNote,
                toastMessage = "Nota eliminada"
            )
        }
    }

    fun consumeToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    fun clearNavigateToNoteId() {
        _uiState.update { it.copy(navigateToNoteId = null) }
    }

    fun generateQuiz(quantity: Int) {
        val note = _uiState.value.currentNote ?: run {
            _uiState.update { it.copy(toastMessage = "No hay nota cargada") }
            return
        }
        val cornell = note.cornellData
        val cantidad = if (quantity in 1..50) quantity else 5
        val invalidQtyToast = if (quantity !in 1..50) "Cantidad inválida, usando 5 por defecto" else null
        _uiState.update {
            it.copy(
                isLoading = true,
                quizStatusMessage = "Generando $cantidad preguntas... ⏳",
                toastMessage = invalidQtyToast
            )
        }
        viewModelScope.launch {
            val result = GeminiRepository.generateQuiz(cornell, note.originalText, cantidad)
            if (result != null) {
                val questions = result.questions
                val quizData = QuizData(noteId = note.id, questions = questions)
                val updatedNote = note.copy(
                    quizData = quizData,
                    quizState = QuizState(0, List(questions.size) { false }, List(questions.size) { -1 })
                )
                val newNotes = _uiState.value.notes.map { if (it.id == note.id) updatedNote else it }
                DataManager.saveNotes(newNotes)
                _uiState.update {
                    it.copy(
                        notes = newNotes,
                        currentNote = updatedNote,
                        quizQuestions = questions,
                        quizCurrentIndex = 0,
                        quizAnswered = List(questions.size) { false },
                        quizSelectedOption = List(questions.size) { -1 },
                        isLoading = false,
                        quizStatusMessage = "",
                        toastMessage = if (result.isFallback) "⚠️ Quiz generado con datos limitados" else "✓ Cuestionario generado"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        quizStatusMessage = "Error de conexión. Intenta de nuevo.",
                        toastMessage = "Error: No se pudo conectar con la API"
                    )
                }
            }
        }
    }

    fun setQuizAnswer(questionIndex: Int, selectedOptionIndex: Int) {
        if (questionIndex !in _uiState.value.quizAnswered.indices) return
        if (_uiState.value.quizAnswered.getOrNull(questionIndex) == true) return // ya respondida
        val q = _uiState.value.quizQuestions.getOrNull(questionIndex) ?: return
        val correct = selectedOptionIndex == q.safeCorrectIndex
        _uiState.update { state ->
            val answered = state.quizAnswered.toMutableList().apply {
                if (questionIndex in indices) this[questionIndex] = true
            }
            val selected = state.quizSelectedOption.toMutableList().apply {
                while (size <= questionIndex) add(-1)
                if (questionIndex in indices) this[questionIndex] = selectedOptionIndex
            }
            state.copy(
                quizAnswered = answered,
                quizSelectedOption = selected,
                toastMessage = if (correct) "✅ ¡Correcto!" else "❌ Incorrecto"
            )
        }
        saveQuizState()
    }

    fun setQuizCurrentIndex(newIndex: Int) {
        val current = _uiState.value.quizCurrentIndex
        val questions = _uiState.value.quizQuestions
        val clamped = newIndex.coerceIn(0, (questions.size - 1).coerceAtLeast(0))
        if (clamped == current) return
        // Reset la pregunta a la que navegamos (como en spec: next/prev resetea la pregunta destino)
        _uiState.update { state ->
            val answered = state.quizAnswered.toMutableList()
            val selected = state.quizSelectedOption.toMutableList()
            while (answered.size <= clamped) answered.add(false)
            while (selected.size <= clamped) selected.add(-1)
            answered[clamped] = false
            selected[clamped] = -1
            state.copy(
                quizCurrentIndex = clamped,
                quizAnswered = answered,
                quizSelectedOption = selected
            )
        }
        saveQuizState()
    }

    private fun saveQuizState() {
        val note = _uiState.value.currentNote ?: return
        val state = _uiState.value
        val quizState = QuizState(state.quizCurrentIndex, state.quizAnswered, state.quizSelectedOption)
        val updatedNote = note.copy(quizState = quizState)
        val newNotes = _uiState.value.notes.map { if (it.id == note.id) updatedNote else it }
        DataManager.saveNotes(newNotes)
        _uiState.update { it.copy(currentNote = updatedNote, notes = newNotes) }
    }

    fun clearQuiz() {
        val note = _uiState.value.currentNote ?: return
        val updatedNote = note.copy(quizData = null, quizState = null)
        val newNotes = _uiState.value.notes.map { if (it.id == note.id) updatedNote else it }
        DataManager.saveNotes(newNotes)
        _uiState.update {
            it.copy(
                notes = newNotes,
                currentNote = updatedNote,
                quizQuestions = emptyList(),
                quizCurrentIndex = 0,
                quizAnswered = emptyList(),
                quizSelectedOption = emptyList(),
                quizStatusMessage = "Todavía no se ha generado el cuestionario.",
                toastMessage = "Cuestionario eliminado"
            )
        }
    }

    fun generateFlashcards(quantity: Int) {
        val note = _uiState.value.currentNote ?: return
        val cornell = note.cornellData
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val cards = GeminiRepository.generateFlashcards(cornell, note.originalText, quantity)
            if (!cards.isNullOrEmpty()) {
                val flashData = FlashcardsData(noteId = note.id, flashcards = cards)
                val updatedNote = note.copy(
                    flashcardsData = flashData,
                    flashcardState = emptyMap()
                )
                val newNotes = _uiState.value.notes.map { if (it.id == note.id) updatedNote else it }
                DataManager.saveNotes(newNotes)
                _uiState.update {
                    it.copy(
                        notes = newNotes,
                        currentNote = updatedNote,
                        flashcards = cards,
                        flashcardFlipped = emptyMap(),
                        isLoading = false,
                        toastMessage = "Flashcards generadas"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(isLoading = false, toastMessage = "No se pudieron generar flashcards")
                }
            }
        }
    }

    fun setFlashcardFlipped(cardIndex: Int, flipped: Boolean) {
        _uiState.update { state ->
            state.copy(flashcardFlipped = state.flashcardFlipped + (cardIndex to flipped))
        }
        val note = _uiState.value.currentNote ?: return
        val newState = (note.flashcardState ?: emptyMap()) + (cardIndex.toString() to FlashcardState(_uiState.value.flashcardFlipped[cardIndex] == true))
        val updatedNote = note.copy(flashcardState = newState)
        val newNotes = _uiState.value.notes.map { if (it.id == note.id) updatedNote else it }
        DataManager.saveNotes(newNotes)
        _uiState.update { it.copy(currentNote = updatedNote, notes = newNotes) }
    }

    fun clearFlashcards() {
        val note = _uiState.value.currentNote ?: return
        val updatedNote = note.copy(flashcardsData = null, flashcardState = null)
        val newNotes = _uiState.value.notes.map { if (it.id == note.id) updatedNote else it }
        DataManager.saveNotes(newNotes)
        _uiState.update {
            it.copy(
                notes = newNotes,
                currentNote = updatedNote,
                flashcards = emptyList(),
                flashcardFlipped = emptyMap(),
                toastMessage = "Flashcards eliminadas"
            )
        }
    }

    fun setExplanationText(text: String) {
        _uiState.update { it.copy(explanationText = text) }
    }

    fun setExplanationVoicePartial(partial: String) {
        _uiState.update { it.copy(explanationPartial = partial) }
    }

    fun appendExplanationFromVoice(final: String) {
        _uiState.update { state ->
            val base = state.explanationText
            val combined = if (base.isNotBlank() && !base.endsWith(" ")) "$base $final" else base + final
            state.copy(
                explanationText = combined,
                explanationPartial = "",
                explanationVoiceStatus = "✅ Transcrito — puedes seguir hablando o evaluar",
                toastMessage = "✅ Voz transcrita"
            )
        }
        saveExplanationToNote()
    }

    fun setExplanationVoiceStatus(status: String) {
        _uiState.update { it.copy(explanationVoiceStatus = status) }
    }

    fun setExplanationMicLevel(level: Float) {
        _uiState.update { it.copy(explanationMicLevel = level) }
    }

    fun evaluateExplanation() {
        val note = _uiState.value.currentNote ?: run {
            _uiState.update { it.copy(toastMessage = "Abre una nota primero") }
            return
        }
        val text = (_uiState.value.explanationText + _uiState.value.explanationPartial).trim()
        if (text.isBlank()) {
            _uiState.update { it.copy(toastMessage = "✏️ Escribe o graba tu explicación primero") }
            return
        }
        val noteContent = buildNoteContent(note)
        if (noteContent.isBlank()) {
            _uiState.update { it.copy(toastMessage = "⚠️ No hay contenido en la nota") }
            return
        }
        _uiState.update {
            it.copy(
                isExplanationEvaluating = true,
                explanationScore = -1,
                explanationScoreLabel = "⏳ Evaluando con IA...",
                explanationFeedbackFound = "",
                explanationFeedbackMissing = "",
                explanationFeedbackTip = ""
            )
        }
        viewModelScope.launch {
            val raw = GeminiRepository.evaluateExplanation(noteContent, text)
            if (raw != null) {
                try {
                    val cleaned = raw.trim().replace("```json", "").replace("```", "").trim()
                    val json = org.json.JSONObject(cleaned)
                    val score = (json.optInt("score", 0)).coerceIn(0, 100)
                    val foundList = json.optJSONArray("found")?.let { arr ->
                        (0 until arr.length()).map { arr.getString(it) }
                    } ?: emptyList()
                    val missingList = json.optJSONArray("missing")?.let { arr ->
                        (0 until arr.length()).map { arr.getString(it) }
                    } ?: emptyList()
                    val tip = json.optString("tip", "")
                    val label = when {
                        score < 40 -> "Necesitas practicar más 💪"
                        score < 65 -> "Vas bien, sigue intentando 🔥"
                        score < 80 -> "¡Casi perfecto! ⭐"
                        else -> "¡Excelente dominio! 🏆"
                    }
                    val foundStr = if (foundList.isNotEmpty())
                        "✓  Conceptos que explicaste bien:\n" + foundList.joinToString("\n") { "   • $it" }
                    else ""
                    val missingStr = if (missingList.isNotEmpty())
                        "✗  Te faltó mencionar:\n" + missingList.joinToString("\n") { "   • $it" }
                    else ""
                    val tipStr = if (tip.isNotBlank()) "💡  Sugerencia: $tip" else ""
                    _uiState.update {
                        it.copy(
                            isExplanationEvaluating = false,
                            explanationScore = score,
                            explanationScoreLabel = label,
                            explanationFeedbackFound = foundStr,
                            explanationFeedbackMissing = missingStr,
                            explanationFeedbackTip = tipStr
                        )
                    }
                    saveExplanationToNote()
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isExplanationEvaluating = false,
                            explanationScoreLabel = "❌ Error al evaluar",
                            toastMessage = "Error al procesar la respuesta"
                        )
                    }
                }
            } else {
                _uiState.update {
                    it.copy(
                        isExplanationEvaluating = false,
                        explanationScoreLabel = "❌ Error al evaluar",
                        toastMessage = "Error al conectar con la API — intenta de nuevo"
                    )
                }
            }
        }
    }

    private fun buildNoteContent(note: Note): String {
        val c = note.cornellData
        var content = """
            TÍTULO: ${c.titulo}
            ASIGNATURA: ${c.asignatura}

            IDEAS CLAVE:
            ${c.ideasClave}

            NOTAS DETALLADAS:
            ${c.notasClase}

            RESUMEN:
            ${c.resumen}
        """.trimIndent()
        if (note.originalText.trim().length > 30) {
            content += "\n\nTRANSCRIPCIÓN ORIGINAL:\n${note.originalText.take(3000)}"
        }
        return content.trim()
    }

    fun clearExplanation() {
        _uiState.update {
            it.copy(
                explanationText = "",
                explanationPartial = "",
                explanationScore = -1,
                explanationScoreLabel = "",
                explanationFeedbackFound = "",
                explanationFeedbackMissing = "",
                explanationFeedbackTip = "",
                explanationVoiceStatus = "Toca 🎙 para empezar a hablar",
                explanationMicLevel = 0f
            )
        }
        saveExplanationToNote()
    }

    private fun saveExplanationToNote() {
        try {
            val note = _uiState.value.currentNote ?: return
            val state = _uiState.value
            val updatedNote = note.copy(
                explanationText = (state.explanationText + state.explanationPartial).take(10000), // Limitar tamaño
                explanationScore = state.explanationScore.coerceIn(-1, 100),
                explanationLabel = state.explanationScoreLabel.take(200),
                explanationFeedbackFound = state.explanationFeedbackFound.take(5000),
                explanationFeedbackMissing = state.explanationFeedbackMissing.take(5000),
                explanationFeedbackTip = state.explanationFeedbackTip.take(1000)
            )
            val newNotes = _uiState.value.notes.map { if (it.id == note.id) updatedNote else it }
            DataManager.saveNotes(newNotes)
            _uiState.update {
                it.copy(notes = newNotes, currentNote = if (it.currentNote?.id == note.id) updatedNote else it.currentNote)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // No mostrar error al usuario, solo loguear
        }
    }

    fun sendChatMessage(userMessage: String) {
        if (userMessage.isBlank()) return
        val note = _uiState.value.currentNote ?: return
        _uiState.update {
            it.copy(
                chatMessages = it.chatMessages + ("user" to userMessage),
                isChatLoading = true
            )
        }
        val context = note.cornellData.let { c ->
            """
                TÍTULO: ${c.titulo}
                ASIGNATURA: ${c.asignatura}
                IDEAS CLAVE: ${c.ideasClave}
                NOTAS: ${c.notasClase}
                RESUMEN: ${c.resumen}
            """.trimIndent()
        }
        val messages = _uiState.value.chatMessages
        viewModelScope.launch {
            val reply = GeminiRepository.chatWithContext(
                messages.map { it.first to it.second },
                context
            )
            _uiState.update { state ->
                val newMessages = if (reply != null) state.chatMessages + ("model" to reply) else state.chatMessages
                state.copy(chatMessages = newMessages, isChatLoading = false, toastMessage = if (reply == null) "Error al obtener respuesta" else null)
            }
        }
    }

    fun getNotesInFolder(folder: String): List<Note> =
        _uiState.value.notes.filter { it.folder == folder || folder in it.folders }.reversed()

    fun copyNoteToFolder(noteId: String, folderName: String) {
        val note = _uiState.value.notes.find { it.id == noteId } ?: return
        if (note.folder == folderName || folderName in note.folders) {
            _uiState.update { it.copy(toastMessage = "La nota ya está en esa carpeta") }
            return
        }
        val updatedNote = note.copy(folders = note.folders + folderName)
        val newNotes = _uiState.value.notes.map { if (it.id == noteId) updatedNote else it }
        DataManager.saveNotes(newNotes)
        _uiState.update {
            it.copy(
                notes = newNotes,
                currentNote = if (it.currentNote?.id == noteId) updatedNote else it.currentNote,
                toastMessage = "Nota copiada a $folderName"
            )
        }
    }

    fun exportNoteToPdf(uri: Uri, note: Note) {
        try {
            val content = buildNoteContent(note)
            val pdf = PdfDocument()
            val paint = Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 28f
                isAntiAlias = true
            }
            val lineHeight = paint.textSize * 1.4f
            val pageWidth = 595
            val pageHeight = 842
            val margin = 50
            val maxWidthPx = (pageWidth - margin * 2).toFloat()
            var y = margin.toFloat()
            var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create())
            fun newPage() {
                pdf.finishPage(page)
                page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create())
                y = margin.toFloat()
            }
            val allLines = content.split("\n").flatMap { line ->
                if (paint.measureText(line) <= maxWidthPx) listOf(line)
                else {
                    val words = line.split(" ")
                    val result = mutableListOf<String>()
                    var current = ""
                    for (w in words) {
                        val test = if (current.isEmpty()) w else "$current $w"
                        if (paint.measureText(test) <= maxWidthPx) current = test
                        else {
                            if (current.isNotEmpty()) result.add(current)
                            current = w
                        }
                    }
                    if (current.isNotEmpty()) result.add(current)
                    result
                }
            }
            for (line in allLines) {
                if (y > pageHeight - margin) newPage()
                page.canvas.drawText(line, margin.toFloat(), y, paint)
                y += lineHeight
            }
            pdf.finishPage(page)
            getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                pdf.writeTo(out)
            }
            pdf.close()
            _uiState.update { it.copy(toastMessage = "Nota exportada a PDF") }
        } catch (e: Throwable) {
            _uiState.update { it.copy(toastMessage = "Error al exportar PDF: ${e.message}") }
        }
    }

    fun exportBackup(uri: Uri) {
        try {
            getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                out.write(DataManager.createBackupJson().toByteArray(Charsets.UTF_8))
            }
            _uiState.update { it.copy(toastMessage = "Backup exportado correctamente") }
        } catch (e: Throwable) {
            _uiState.update { it.copy(toastMessage = "Error al exportar: ${e.message}") }
        }
    }

    fun importBackup(uri: Uri) {
        try {
            val json = getApplication<Application>().contentResolver.openInputStream(uri)
                ?.bufferedReader(Charsets.UTF_8)?.readText() ?: run {
                _uiState.update { it.copy(toastMessage = "No se pudo leer el archivo") }
                return
            }
            val data = DataManager.importFromBackupJson(json)
            if (data != null) {
                DataManager.mergeBackup(data.first, data.second)
                loadAll()
                _uiState.update { it.copy(toastMessage = "Backup importado: se agregaron ${data.first.size} notas") }
            } else {
                _uiState.update { it.copy(toastMessage = "Archivo de backup inválido") }
            }
        } catch (e: Throwable) {
            _uiState.update { it.copy(toastMessage = "Error al importar: ${e.message}") }
        }
    }
}
