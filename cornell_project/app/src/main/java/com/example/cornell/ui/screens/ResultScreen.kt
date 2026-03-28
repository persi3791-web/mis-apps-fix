package com.example.cornell.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cornell.data.Note
import com.example.cornell.ui.theme.CornellBackground
import com.example.cornell.ui.theme.CornellCard
import com.example.cornell.ui.theme.CornellPrimary
import com.example.cornell.ui.theme.CornellSurface
import com.example.cornell.ui.theme.CornellText
import com.example.cornell.ui.theme.CornellTextSecondary
import com.example.cornell.ui.utils.parseMarkdownToAnnotatedString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    note: Note?,
    isEditing: Boolean,
    onBack: () -> Unit,
    onToggleEdit: () -> Unit,
    onSaveEdits: (title: String, ideas: String, notas: String, resumen: String) -> Unit,
    tabResumen: @Composable () -> Unit,
    tabCuestionario: @Composable () -> Unit,
    tabFlashcards: @Composable () -> Unit,
    tabChat: @Composable () -> Unit,
    tabExplicacion: @Composable () -> Unit,
    tabTransc: @Composable () -> Unit
) {
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Resumen", "Cuestionario", "Flashcards", "Chat", "Explicar", "Transcripción")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CornellBackground,
        topBar = {
            TopAppBar(
                title = { Text("Nota Cornell", color = CornellText) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = CornellText)
                    }
                },
                actions = {
                    IconButton(onClick = onToggleEdit) {
                        Icon(
                            if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = if (isEditing) "Guardar" else "Editar",
                            tint = if (isEditing) androidx.compose.ui.graphics.Color(0xFF66FF66) else CornellText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CornellSurface)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = tabIndex,
                containerColor = CornellSurface,
                contentColor = CornellPrimary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = tabIndex == index,
                        onClick = { tabIndex = index },
                        text = { Text(title, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }
            when (tabIndex) {
                0 -> tabResumen()
                1 -> tabCuestionario()
                2 -> tabFlashcards()
                3 -> tabChat()
                4 -> tabExplicacion()
                5 -> tabTransc()
            }
        }
    }
}

@Composable
fun TabResumenContent(
    note: Note?,
    isEditing: Boolean,
    titleValue: String,
    ideasValue: String,
    notasValue: String,
    resumenValue: String,
    onTitleChange: (String) -> Unit,
    onIdeasChange: (String) -> Unit,
    onNotasChange: (String) -> Unit,
    onResumenChange: (String) -> Unit
) {
    val data = note?.cornellData ?: return
    val scroll = rememberScrollState()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp)
    ) {
        if (isEditing) {
            OutlinedTextField(
                value = titleValue,
                onValueChange = onTitleChange,
                label = { Text("Título") },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text(
                note.title,
                style = MaterialTheme.typography.titleLarge,
                color = CornellText
            )
        }
        Text(
            data.asignatura,
            style = MaterialTheme.typography.bodySmall,
            color = CornellTextSecondary
        )
        Spacer(Modifier.padding(vertical = 8.dp))
        // Layout Cornell: Ideas Clave (izq 35%) | Notas Detalladas (der 65%)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Columna izquierda: IDEAS CLAVE
            Card(
                modifier = Modifier.weight(0.35f),
                colors = CardDefaults.cardColors(containerColor = CornellCard),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("IDEAS CLAVE", style = MaterialTheme.typography.labelSmall, color = CornellPrimary)
                    Spacer(Modifier.padding(vertical = 4.dp))
                    if (isEditing) {
                        OutlinedTextField(
                            value = ideasValue,
                            onValueChange = onIdeasChange,
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4
                        )
                    } else {
                        Text(
                            text = parseMarkdownToAnnotatedString(data.ideasClave, CornellText),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            // Columna derecha: NOTAS DETALLADAS
            Card(
                modifier = Modifier.weight(0.65f),
                colors = CardDefaults.cardColors(containerColor = CornellCard),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("NOTAS DETALLADAS", style = MaterialTheme.typography.labelSmall, color = CornellPrimary)
                    Spacer(Modifier.padding(vertical = 4.dp))
                    if (isEditing) {
                        OutlinedTextField(
                            value = notasValue,
                            onValueChange = onNotasChange,
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 6
                        )
                    } else {
                        Text(
                            text = parseMarkdownToAnnotatedString(data.notasClase, CornellText),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
        Spacer(Modifier.padding(vertical = 8.dp))
        Text("RESUMEN", style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color(0xFFFFCC33))
        if (isEditing) {
            OutlinedTextField(
                value = resumenValue,
                onValueChange = onResumenChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CornellCard),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = parseMarkdownToAnnotatedString(data.resumen, CornellText),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
