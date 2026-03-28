package com.example.cornell.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import com.example.cornell.data.Note
import com.example.cornell.ui.theme.CornellBackground
import com.example.cornell.ui.theme.CornellCard
import com.example.cornell.ui.theme.CornellPrimary
import com.example.cornell.ui.theme.CornellText
import com.example.cornell.ui.theme.CornellTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    notes: List<Note>,
    folders: List<String>,
    onNoteClick: (String) -> Unit,
    onFolderClick: (String) -> Unit,
    onCreateFolder: () -> Unit,
    onConfirmCreateFolder: (String) -> Unit,
    onRenameNote: (String, String) -> Unit,
    onDeleteNote: (String) -> Unit,
    onMoveNoteToFolder: (String, String) -> Unit,
    onCopyNoteToFolder: (String, String) -> Unit,
    onExportNoteToPdf: (Note) -> Unit,
    onNewNote: () -> Unit,
    onSettingsClick: () -> Unit = {}
) {
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var optionsNoteId by remember { mutableStateOf<String?>(null) }
    var showRenameNoteDialog by remember { mutableStateOf(false) }
    var renameNoteTitle by remember { mutableStateOf("") }
    var showDeleteNoteConfirm by remember { mutableStateOf(false) }
    var showAddToFolderDialog by remember { mutableStateOf(false) }

    val noteForOptions = remember(optionsNoteId) { notes.find { it.id == optionsNoteId } }

    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("Nueva Carpeta") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Nombre de la carpeta") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            onConfirmCreateFolder(newFolderName.trim())
                            newFolderName = ""
                            showCreateFolderDialog = false
                        }
                    }
                ) { Text("CREAR") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false; newFolderName = "" }) {
                    Text("CANCELAR")
                }
            }
        )
    }

    if (showAddToFolderDialog && optionsNoteId != null) {
        val note = notes.find { it.id == optionsNoteId }
        if (note != null) {
            AlertDialog(
                onDismissRequest = { showAddToFolderDialog = false; optionsNoteId = null },
                title = { Text("Agregar a carpeta") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        folders.filter { it != note.folder && it !in (note.folders + listOfNotNull(note.folder)) }.forEach { folderName ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onCopyNoteToFolder(note.id, folderName)
                                        showAddToFolderDialog = false
                                        optionsNoteId = null
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Folder, null, tint = CornellPrimary)
                                Spacer(Modifier.size(12.dp))
                                Text(folderName, color = CornellText)
                            }
                        }
                        if (folders.none { it != note.folder && it !in (note.folders + listOfNotNull(note.folder)) }) {
                            Text("La nota ya está en todas las carpetas o no hay otras carpetas.", color = CornellTextSecondary)
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showAddToFolderDialog = false; optionsNoteId = null }) { Text("CERRAR") } }
            )
        }
    }
    if (optionsNoteId != null && noteForOptions != null && !showRenameNoteDialog && !showDeleteNoteConfirm && !showAddToFolderDialog) {
        AlertDialog(
            onDismissRequest = { optionsNoteId = null },
            title = {},
            text = {
        Card(
            modifier = Modifier.padding(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2E3035)),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showAddToFolderDialog = true
                        }
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, null, tint = CornellText)
                        Spacer(Modifier.size(12.dp))
                        Text("Agregar a carpeta", color = CornellText)
                    }
                }
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            renameNoteTitle = noteForOptions.title
                            optionsNoteId = null
                            showRenameNoteDialog = true
                        }
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, null, tint = CornellText)
                        Spacer(Modifier.size(12.dp))
                        Text("Renombrar", color = CornellText)
                    }
                }
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onExportNoteToPdf(noteForOptions)
                            optionsNoteId = null
                        }
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PictureAsPdf, null, tint = CornellText)
                        Spacer(Modifier.size(12.dp))
                        Text("Exportar nota a PDF", color = CornellText)
                    }
                }
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDeleteNoteConfirm = true }
                        .padding(12.dp)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.size(12.dp))
                        Text("Borrar nota", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }
    if (showRenameNoteDialog && optionsNoteId != null) {
        val note = notes.find { it.id == optionsNoteId }
        if (note != null) {
            AlertDialog(
                onDismissRequest = { showRenameNoteDialog = false; optionsNoteId = null },
                title = { Text("Renombrar nota") },
                text = {
                    OutlinedTextField(
                        value = renameNoteTitle,
                        onValueChange = { renameNoteTitle = it },
                        label = { Text("Nuevo título") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (renameNoteTitle.isNotBlank()) {
                            onRenameNote(note.id, renameNoteTitle.trim())
                            showRenameNoteDialog = false
                            optionsNoteId = null
                        }
                    }) { Text("RENOMBRAR") }
                },
                dismissButton = { TextButton(onClick = { showRenameNoteDialog = false; optionsNoteId = null }) { Text("CANCELAR") } }
            )
        }
    }
    if (showDeleteNoteConfirm && optionsNoteId != null) {
        val note = notes.find { it.id == optionsNoteId }
        if (note != null) {
            AlertDialog(
                onDismissRequest = { showDeleteNoteConfirm = false; optionsNoteId = null },
                title = { Text("Eliminar nota") },
                text = { Text("¿Eliminar \"${note.title}\"?") },
                confirmButton = {
                    TextButton(onClick = {
                        onDeleteNote(note.id)
                        showDeleteNoteConfirm = false
                        optionsNoteId = null
                    }) { Text("ELIMINAR", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = { TextButton(onClick = { showDeleteNoteConfirm = false; optionsNoteId = null }) { Text("CANCELAR") } }
            )
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CornellBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewNote,
                containerColor = CornellPrimary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nueva nota")
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Mis Notas",
                    style = MaterialTheme.typography.headlineMedium,
                    color = CornellText
                )
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Ajustes",
                        tint = CornellTextSecondary
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "CARPETAS",
                style = MaterialTheme.typography.labelSmall,
                color = CornellTextSecondary
            )
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FolderChip(
                    icon = Icons.Default.CreateNewFolder,
                    label = "Crear\nCarpeta",
                    onClick = { showCreateFolderDialog = true }
                )
                folders.forEach { name ->
                    FolderChip(
                        icon = Icons.Default.Description,
                        label = name,
                        onClick = { onFolderClick(name) }
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "RECIENTES",
                style = MaterialTheme.typography.labelSmall,
                color = CornellTextSecondary
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(notes.reversed()) { note ->
            NoteListItem(
                note = note,
                onClick = { onNoteClick(note.id) },
                onOptions = { optionsNoteId = note.id }
            )
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun FolderChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = CornellCard),
        modifier = Modifier.size(80.dp, 100.dp)
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = CornellPrimary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = CornellText,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun NoteListItem(
    note: Note,
    onClick: () -> Unit,
    onOptions: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = CornellCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Description,
                contentDescription = null,
                tint = CornellTextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    note.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = CornellText
                )
                Text(
                    "${note.date}  •  ${note.folder}",
                    style = MaterialTheme.typography.bodySmall,
                    color = CornellTextSecondary
                )
            }
            IconButton(onClick = onOptions) {
                Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = CornellTextSecondary)
            }
        }
    }
}
