package com.example.cornell.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cornell.data.FlashcardItem
import com.example.cornell.data.QuizQuestion
import com.example.cornell.ui.theme.CornellCard
import com.example.cornell.ui.theme.CornellPrimary
import com.example.cornell.ui.theme.CornellText
import com.example.cornell.ui.theme.CornellTextSecondary
import com.example.cornell.ui.theme.QuizCorrect
import com.example.cornell.ui.theme.QuizDefault
import com.example.cornell.ui.theme.QuizWrong

@Composable
fun TabQuizContent(
    questions: List<QuizQuestion>,
    currentIndex: Int,
    answered: List<Boolean>,
    selectedOption: List<Int>,
    isLoading: Boolean,
    statusMessage: String,
    quantityHint: String,
    onQuantityChange: (String) -> Unit,
    onGenerate: () -> Unit,
    onClear: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSelectOption: (Int) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // Cabecera: cantidad + Generar + Borrar (estructura spec)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = quantityHint,
                onValueChange = onQuantityChange,
                label = { Text("Cantidad (ej. 5)") },
                modifier = Modifier.weight(1f)
            )
            androidx.compose.material3.Button(
                onClick = onGenerate,
                enabled = !isLoading,
                modifier = Modifier.weight(1.2f)
            ) {
                Text(if (questions.isEmpty()) "Generar" else "Regenerar")
            }
            androidx.compose.material3.Button(
                onClick = onClear,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = QuizWrong),
                modifier = Modifier.weight(0.8f)
            ) {
                Text("Borrar")
            }
        }
        Spacer(Modifier.height(12.dp))
        // quiz_spinner
        if (isLoading) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    Modifier.size(48.dp),
                    color = CornellPrimary
                )
            }
            Spacer(Modifier.height(8.dp))
        }
        // quiz_status (visible cuando no hay preguntas o hay mensaje)
        if (statusMessage.isNotEmpty()) {
            Text(
                statusMessage,
                color = CornellTextSecondary,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                textAlign = TextAlign.Center
            )
        }
        if (questions.isNotEmpty()) {
            // quiz_counter
            Text(
                "Pregunta ${currentIndex + 1} de ${questions.size}",
                color = CornellTextSecondary,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                textAlign = TextAlign.Center
            )
            val q = questions.getOrNull(currentIndex) ?: return
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = CornellCard),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    q.question,
                    color = CornellText,
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(20.dp)
                )
            }
            val options = q.getOptionsList()
            val correctIdx = q.safeCorrectIndex
            val alreadyAnswered = answered.getOrNull(currentIndex) == true
            val selectedIdx = selectedOption.getOrNull(currentIndex) ?: -1
            options.forEachIndexed { idx, opt ->
                val isCorrect = idx == correctIdx
                val isSelectedWrong = alreadyAnswered && selectedIdx == idx && selectedIdx != correctIdx
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .then(
                            if (!alreadyAnswered) Modifier.clickable { onSelectOption(idx) }
                            else Modifier
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            alreadyAnswered && isCorrect -> QuizCorrect
                            isSelectedWrong -> QuizWrong
                            else -> QuizDefault
                        }
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(opt, color = CornellText)
                        if (alreadyAnswered && isCorrect && q.displayExplanation.isNotBlank()) {
                            Spacer(Modifier.padding(vertical = 8.dp))
                            Text(
                                q.displayExplanation,
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                color = CornellTextSecondary
                            )
                        }
                    }
                }
            }
            // Botones de navegación Anterior / Siguiente (spec: btnPrev, btnNext)
            val canGoPrev = currentIndex > 0
            val canGoNext = currentIndex < questions.size - 1
            Row(
                Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.OutlinedButton(
                    onClick = onPrev,
                    enabled = canGoPrev,
                    modifier = Modifier.weight(1f),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = CornellText)
                ) {
                    Text("← Anterior")
                }
                androidx.compose.material3.OutlinedButton(
                    onClick = onNext,
                    enabled = canGoNext,
                    modifier = Modifier.weight(1f),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = CornellText)
                ) {
                    Text("Siguiente →")
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun TabFlashcardsContent(
    cards: List<FlashcardItem>,
    flipped: Map<Int, Boolean>,
    isLoading: Boolean,
    quantityHint: String,
    onQuantityChange: (String) -> Unit,
    onGenerate: () -> Unit,
    onClear: () -> Unit,
    onFlip: (Int) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            if (cards.isEmpty()) "Todavía no se han generado flashcards." else "Se generaron ${cards.size} flashcards",
            color = CornellTextSecondary
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = quantityHint,
                onValueChange = onQuantityChange,
                label = { Text("Cantidad") },
                modifier = Modifier.weight(0.3f)
            )
            androidx.compose.material3.Button(onClick = onGenerate, enabled = !isLoading) {
                if (isLoading) CircularProgressIndicator(Modifier.height(24.dp))
                else Text(if (cards.isEmpty()) "Generar" else "Regenerar")
            }
        }
        androidx.compose.material3.TextButton(onClick = onClear) {
            Text("Limpiar flashcards")
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(cards) { index, card ->
                val isFlipped = flipped[index] == true
                Card(
                    onClick = { onFlip(index) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isFlipped) CornellPrimary else CornellCard
                    )
                ) {
                    Column(Modifier.padding(24.dp)) {
                        Text(
                            if (isFlipped) "Respuesta" else "Pregunta",
                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                            color = CornellTextSecondary
                        )
                        Text(
                            if (isFlipped) card.displayRespuesta else card.displayPregunta,
                            color = CornellText,
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TabChatContent(
    messages: List<Pair<String, String>>,
    isLoading: Boolean,
    onSend: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages.size) { i ->
                val (role, text) = messages[i]
                Card(
                    modifier = Modifier.fillMaxWidth(if (role == "user") 0.75f else 1f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (role == "user") CornellPrimary else CornellCard
                    )
                ) {
                    Text(
                        text,
                        modifier = Modifier.padding(12.dp),
                        color = CornellText
                    )
                }
            }
            if (isLoading) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = CornellCard)) {
                        Row(Modifier.padding(12.dp)) {
                            CircularProgressIndicator(Modifier.size(24.dp))
                            Spacer(Modifier.padding(8.dp))
                            Text("Pensando...", color = CornellTextSecondary)
                        }
                    }
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("Pregunta algo sobre tus notas...") },
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            )
            IconButton(
                onClick = {
                    if (input.isNotBlank()) {
                        onSend(input.trim())
                        input = ""
                    }
                },
                enabled = !isLoading && input.isNotBlank()
            ) {
                Icon(Icons.Default.Send, contentDescription = "Enviar", tint = CornellPrimary)
            }
        }
    }
}

@Composable
fun TabTranscContent(originalText: String) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("TRANSCRIPCIÓN COMPLETA", color = CornellPrimary, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(8.dp))
        Card(colors = CardDefaults.cardColors(containerColor = CornellCard)) {
            Text(
                originalText.ifBlank { "No hay contenido original disponible." },
                modifier = Modifier.padding(16.dp),
                color = CornellText
            )
        }
    }
}
