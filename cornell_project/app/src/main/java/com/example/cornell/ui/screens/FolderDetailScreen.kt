package com.example.cornell.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cornell.data.Note
import com.example.cornell.ui.theme.CornellBackground
import com.example.cornell.ui.theme.CornellCard
import com.example.cornell.ui.theme.CornellPrimary
import com.example.cornell.ui.theme.CornellSurface
import com.example.cornell.ui.theme.CornellText
import com.example.cornell.ui.theme.CornellTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    folderName: String,
    notes: List<Note>,
    folders: List<String>,
    onBack: () -> Unit,
    onNoteClick: (String) -> Unit,
    onRenameNote: (String, String) -> Unit,
    onDeleteNote: (String) -> Unit,
    onMoveNoteToFolder: (String, String) -> Unit,
    onCopyNoteToFolder: (String, String) -> Unit,
    onExportNoteToPdf: (Note) -> Unit,
    onRenameFolder: (String, String) -> Unit,
    onDeleteFolder: (String) -> Unit
) {
    var optionsNoteId by remember { mutableStateOf<String?>(null) }
    var showRenameNoteDialog by remember { mutableStateOf(false) }
    var renameNoteTitle by remember { mutableStateOf("") }
    var showDeleteNoteConfirm by remember { mutableStateOf(false) }
    var showAddToFolderDialog by remember { mutableStateOf(false) }
    var showFolderMenu by remember { mutableStateOf(false) }
    var showRenameFolderDialog by remember { mutableStateOf(false) }
    var renameFolderName by remember { mutableStateOf(folderName) }
    var showDeleteFolderConfirm by remember { mutableStateOf(false) }

    val noteForOptions = remember(optionsNoteId) { notes.find { it.id == optionsNoteId } }

    if (showAddToFolderDialog && optionsNoteId != null) {
        val note = notes.find { it.id == optionsNoteId }
        if (note != null) {
            AlertDialog(
                onDismissRequest = { showAddToFolderDialog = false; optionsNoteId = null },
                title = { Text("Agregar a carpeta") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        folders.filter { it != note.folder }.forEach { fName ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onCopyNoteToFolder(note.id, fName)
                                        showAddToFolderDialog = false
                                        optionsNoteId = null
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Folder, null, tint = CornellPrimary)
                                Spacer(Modifier.size(12.dp))
                                Text(fName, color = CornellText)
                            }
                        }
                        if (folders.none { it != note.folder }) {
                            Text("No hay otras carpetas disponibles.", color = CornellTextSecondary)
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
                    colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF2E3035)),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAddToFolderDialog = true }
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
    if (showRenameFolderDialog) {
        AlertDialog(
            onDismissRequest = { showRenameFolderDialog = false },
            title = { Text("Renombrar carpeta") },
            text = {
                OutlinedTextField(
                    value = renameFolderName,
                    onValueChange = { renameFolderName = it },
                    label = { Text("Nuevo nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (renameFolderName.isNotBlank()) {
                        onRenameFolder(folderName, renameFolderName.trim())
                        showRenameFolderDialog = false
                        showFolderMenu = false
                    }
                }) { Text("RENOMBRAR") }
            },
            dismissButton = { TextButton(onClick = { showRenameFolderDialog = false }) { Text("CANCELAR") } }
        )
    }
    if (showDeleteFolderConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteFolderConfirm = false },
            title = { Text("Borrar carpeta") },
            text = { Text("¿Eliminar la carpeta \"$folderName\"? Las notas se moverán a General.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteFolder(folderName)
                    showDeleteFolderConfirm = false
                    showFolderMenu = false
                    onBack()
                }) { Text("BORRAR", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteFolderConfirm = false }) { Text("CANCELAR") } }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CornellBackground,
        topBar = {
            TopAppBar(
                title = { Text(folderName, color = CornellText) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = CornellText)
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showFolderMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Opciones carpeta", tint = CornellText)
                        }
                        DropdownMenu(
                            expanded = showFolderMenu,
                            onDismissRequest = { showFolderMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Edit, null, Modifier.size(20.dp))
                                    Spacer(Modifier.size(8.dp))
                                    Text("Renombrar carpeta")
                                }},
                                onClick = {
                                    renameFolderName = folderName
                                    showFolderMenu = false
                                    showRenameFolderDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Delete, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                                    Spacer(Modifier.size(8.dp))
                                    Text("Borrar carpeta", color = MaterialTheme.colorScheme.error)
                                }},
                                onClick = {
                                    showFolderMenu = false
                                    showDeleteFolderConfirm = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CornellSurface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(notes) { note ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNoteClick(note.id) },
                    colors = CardDefaults.cardColors(containerColor = CornellCard)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Description, null, tint = CornellTextSecondary, modifier = Modifier.padding(end = 10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(note.title, color = CornellText)
                            Text("${note.date}  •  ${note.folder}", style = MaterialTheme.typography.bodySmall, color = CornellTextSecondary)
                        }
                        IconButton(onClick = { optionsNoteId = note.id }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = CornellTextSecondary)
                        }
                    }
                }
            }
        }
    }
}
