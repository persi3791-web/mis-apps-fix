package com.example.myapplication.ui

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.myapplication.data.HistoryItem
import com.example.myapplication.data.ProblemRow
import com.example.myapplication.ui.viewmodel.MainViewModel
import com.example.myapplication.util.PermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onPermissionDenied: () -> Unit = {}
) {
    val context = LocalContext.current
    val inputText by viewModel.inputText.collectAsState()
    val rows by viewModel.rows.collectAsState()
    val progressText by viewModel.progressText.collectAsState()
    val isResolving by viewModel.isResolving.collectAsState()
    val adicionalInstructions by viewModel.adicionalInstructions.collectAsState()
    val historyItems by viewModel.historyItems.collectAsState()
    val showCompletionMessage by viewModel.showCompletionMessage.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val exportFolderUri by viewModel.exportFolderUri.collectAsState()
    
    var showInstructionsDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showSaveOptionsDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf<HistoryItem?>(null) }
    var showLongPressDialog by remember { mutableStateOf<HistoryItem?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<Int?>(null) }
    var showRenameDialog by remember { mutableStateOf<Pair<Int, String>?>(null) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showMessageDialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showImportChoiceDialog by remember { mutableStateOf(false) }
    var pendingImportMode by remember { mutableStateOf<String?>(null) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    
    // Launcher para crear archivo (exportar)
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            viewModel.exportHistoryToUri(context, it) { success, message ->
                if (success) {
                    showMessageDialog = Pair("Éxito", message)
                } else {
                    showMessageDialog = Pair("Error", message)
                }
            }
        }
    }
    
    // Launcher para abrir archivo (importar)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val mode = pendingImportMode ?: "merge"
            pendingImportMode = null
            viewModel.importHistoryFromUri(context, it, mode) { success, message ->
                if (success) {
                    viewModel.loadHistory()
                    showHistoryDialog = false
                    showHistoryDialog = true
                    showMessageDialog = Pair("Completado", message)
                } else {
                    showMessageDialog = Pair("Error", message)
                }
            }
        }
    }
    
    // Launcher para permisos
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            // Ejecutar la acción pendiente después de otorgar permisos
            pendingAction?.invoke()
            pendingAction = null
        } else {
            showMessageDialog = Pair("Permiso necesario", "Se necesita permiso de almacenamiento para realizar esta acción.")
            pendingAction = null
        }
    }
    

    // Launcher para seleccionar carpeta de exportación
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: android.net.Uri? ->
        uri?.let { viewModel.setExportFolder(it) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Campo de texto principal
        OutlinedTextField(
            value = inputText,
            onValueChange = { viewModel.updateInputText(it) },
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.28f),
            label = { Text("Pega aquí la tabla con columnas:\nNúmero | Problema | Enlace Imagen\nCada fila en una línea") },
            maxLines = 10
        )
        
        // Botones superiores
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = { showInstructionsDialog = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF336699)
                )
            ) {
                Text("Más indicaciones")
            }
            Button(
                onClick = { 
                    viewModel.loadHistory()
                    showHistoryDialog = true 
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF996633)
                )
            ) {
                Text("Historial")
            }
        }

        // Botón modelo IA
        Button(
            onClick = { viewModel.toggleModel() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selectedModel == com.example.myapplication.ui.viewmodel.MainViewModel.AiModel.GEMINI)
                    Color(0xFF1A6B3A) else Color(0xFF5A2D82)
            )
        ) {
            Text(
                text = if (selectedModel == com.example.myapplication.ui.viewmodel.MainViewModel.AiModel.GEMINI)
                    "🤖 Modelo: Gemini 2.0 (toca para cambiar a GPT-4o)"
                else
                    "🤖 Modelo: GPT-4o (toca para cambiar a Gemini)",
                fontSize = 13.sp
            )
        }

        // Botones parse + auto-ordenar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = { viewModel.parseTable() },
                modifier = Modifier.weight(1.6f)
            ) {
                Text("Procesar tabla")
            }
            Button(
                onClick = { viewModel.autoFormatInput() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF5C3D1E)
                )
            ) {
                Text("Auto-ordenar", fontSize = 12.sp)
            }
        }
        
        // Tabla con scroll
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.44f),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Encabezados
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE0E0E0))
                        .padding(8.dp)
                ) {
                    TableHeader("Número", 0.12f)
                    TableHeader("Problema", 0.18f)
                    TableHeader("Enlace Imagen", 0.15f)
                    TableHeader("Enlace Google", 0.15f)
                    TableHeader("Video YouTube", 0.15f)
                    TableHeader("Solución IA", 0.15f)
                    TableHeader("Mnemotecnia", 0.1f)
                }
                
                // Filas
                rows.forEachIndexed { index, row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.Gray)
                            .padding(4.dp)
                    ) {
                        TableCell(row.numeroAvion, 0.12f)
                        TableCell(row.problem, 0.18f)
                        TableCell(row.enlaceImagen, 0.15f)
                        TableCell(
                            if (row.enlaceGoogle.isEmpty()) "Pendiente" else row.enlaceGoogle,
                            0.15f,
                            if (row.enlaceGoogle.isEmpty()) Color.Gray else Color(0xFF0066CC)
                        )
                        TableCell(
                            if (row.enlaceYoutube.isEmpty()) "Pendiente" else row.enlaceYoutube,
                            0.15f,
                            if (row.enlaceYoutube.isEmpty()) Color.Gray else Color(0xFF0066CC)
                        )
                        TableCell(
                            if (row.solucion.isEmpty()) "Pendiente" else row.solucion,
                            0.15f,
                            if (row.solucion.isEmpty()) Color.Gray else Color.Unspecified
                        )
                        TableCell(
                            if (row.mnemotecnia.isEmpty()) "Pendiente" else row.mnemotecnia,
                            0.1f,
                            if (row.mnemotecnia.isEmpty()) Color.Gray else Color.Unspecified
                        )
                    }
                }
            }
        }
        
        // Botón resolver
        Button(
            onClick = { viewModel.resolveSequential() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isResolving && rows.isNotEmpty()
        ) {
            Text("Resolver problemas secuencialmente")
        }
        
        // Progreso
        Text(
            text = progressText,
            modifier = Modifier.fillMaxWidth(),
            fontSize = 14.sp
        )
    }
    
        // Botón engranaje flotante
        androidx.compose.material3.FloatingActionButton(
            onClick = { showSettingsDialog = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(42.dp),
            containerColor = Color(0xFF333333),
            contentColor = Color.White
        ) {
            Text("⚙️", fontSize = 18.sp)
        }
    } // Cierre Box

    // Dialog: Instrucciones
    if (showInstructionsDialog) {
        InstructionsDialog(
            initialText = adicionalInstructions,
            onDismiss = { showInstructionsDialog = false },
            onSaveOnly = { text ->
                viewModel.updateInstructions(text)
                showInstructionsDialog = false
                showMessageDialog = Pair("Guardado", "Indicaciones guardadas (no añadidas al historial).")
            },
            onSaveToHistory = { text ->
                viewModel.updateInstructions(text)
                if (text.trim().isNotEmpty()) {
                    viewModel.saveInstructionsToHistory()
                    showInstructionsDialog = false
                    showMessageDialog = Pair("Guardado en historial", "Indicaciones guardadas en historial.")
                } else {
                    showMessageDialog = Pair("Nada que guardar", "No hay texto para añadir al historial.")
                }
            }
        )
    }
    
    // Dialog: Historial
    if (showHistoryDialog) {
        HistoryDialog(
            items = historyItems,
            onDismiss = { showHistoryDialog = false },
            onItemClick = { item ->
                showRestoreDialog = item
            },
            onItemLongPress = { item ->
                val index = historyItems.indexOf(item)
                if (index >= 0) {
                    showLongPressDialog = item
                }
            },
            onClear = { showClearConfirmDialog = true },
            onExport = {
                // Verificar permisos primero
                val requiredPermissions = PermissionHelper.getRequiredPermissions()
                if (requiredPermissions.isNotEmpty() && 
                    !requiredPermissions.all { permission ->
                        android.content.pm.PackageManager.PERMISSION_GRANTED == 
                        ContextCompat.checkSelfPermission(context, permission)
                    }) {
                    pendingAction = { exportLauncher.launch("historial_backup.json") }
                    permissionLauncher.launch(requiredPermissions)
                } else {
                    // Abrir selector de archivos para exportar
                    exportLauncher.launch("historial_backup.json")
                }
            },
            onImport = {
                // Verificar permisos primero
                val requiredPermissions = PermissionHelper.getRequiredPermissions()
                if (requiredPermissions.isNotEmpty() && 
                    !requiredPermissions.all { permission ->
                        android.content.pm.PackageManager.PERMISSION_GRANTED == 
                        ContextCompat.checkSelfPermission(context, permission)
                    }) {
                    pendingAction = { showImportChoiceDialog = true }
                    permissionLauncher.launch(requiredPermissions)
                } else {
                    // Mostrar diálogo para elegir modo de importación
                    showImportChoiceDialog = true
                }
            }
        )
    }
    
    // Dialog: Elegir modo de importación (Fusionar o Reemplazar)
    if (showImportChoiceDialog) {
        ImportChoiceDialog(
            importCount = 0, // No sabemos el conteo hasta que se seleccione el archivo
            onDismiss = { showImportChoiceDialog = false },
            onMerge = {
                showImportChoiceDialog = false
                pendingImportMode = "merge"
                importLauncher.launch(arrayOf("application/json"))
            },
            onReplace = {
                showImportChoiceDialog = false
                pendingImportMode = "replace"
                importLauncher.launch(arrayOf("application/json"))
            }
        )
    }
    
    // Dialog: Restaurar
    showRestoreDialog?.let { item ->
        RestoreDialog(
            item = item,
            onDismiss = { showRestoreDialog = null },
            onRestore = {
                viewModel.restoreFromHistory(item)
                showRestoreDialog = null
                showHistoryDialog = false
                showMessageDialog = Pair("Restaurado", "La indicación fue restaurada y cargada en el editor.")
            }
        )
    }
    
    // Dialog: Opciones (long press)
    showLongPressDialog?.let { item ->
        val index = historyItems.indexOf(item)
        if (index >= 0) {
            LongPressOptionsDialog(
                item = item,
                onDismiss = { showLongPressDialog = null },
                onDelete = {
                    showDeleteConfirmDialog = index
                    showLongPressDialog = null
                },
                onRename = {
                    showRenameDialog = Pair(index, item.titulo)
                    showLongPressDialog = null
                }
            )
        }
    }
    
    // Dialog: Confirmar eliminar
    showDeleteConfirmDialog?.let { index ->
        ConfirmDialog(
            title = "Confirmar eliminación",
            message = "¿Eliminar esta entrada permanentemente?",
            onConfirm = {
                viewModel.deleteHistoryItem(index)
                showDeleteConfirmDialog = null
                showMessageDialog = Pair("Eliminado", "Entrada eliminada.")
            },
            onDismiss = { showDeleteConfirmDialog = null }
        )
    }
    
    // Dialog: Renombrar
    showRenameDialog?.let { (index, currentTitle) ->
        RenameDialog(
            currentTitle = currentTitle,
            onDismiss = { showRenameDialog = null },
            onConfirm = { newTitle ->
                if (newTitle.isNotEmpty()) {
                    viewModel.renameHistoryItem(index, newTitle)
                    showRenameDialog = null
                    showMessageDialog = Pair("Renombrado", "Entrada renombrada a '$newTitle'.")
                }
            }
        )
    }
    
    // Dialog: Confirmar borrar todo
    if (showClearConfirmDialog) {
        ConfirmDialog(
            title = "Confirmar borrado",
            message = "¿Seguro que deseas borrar todo el historial?",
            onConfirm = {
                viewModel.clearHistory()
                showClearConfirmDialog = false
                showMessageDialog = Pair("Hecho", "Historial borrado correctamente.")
            },
            onDismiss = { showClearConfirmDialog = false }
        )
    }
    
    // Dialog: Mensaje simple
    showMessageDialog?.let { (title, message) ->
        MessageDialog(
            title = title,
            message = message,
            onDismiss = { showMessageDialog = null }
        )
    }
    
    // Dialog: Ajustes
    if (showSettingsDialog) {
        Dialog(onDismissRequest = { showSettingsDialog = false }) {
            androidx.compose.material3.Card(
                modifier = Modifier.fillMaxWidth(0.9f).wrapContentHeight(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("⚙️ Ajustes", fontSize = 20.sp, fontWeight = FontWeight.Bold)

                    Text("Carpeta de exportación CSV:", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (exportFolderUri != null) "✅ Carpeta seleccionada" else "📁 Usando carpeta Downloads por defecto",
                        fontSize = 12.sp,
                        color = if (exportFolderUri != null) Color(0xFF4CAF50) else Color.Gray
                    )
                    Button(
                        onClick = { folderPickerLauncher.launch(null) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                    ) {
                        Text("📂 Seleccionar carpeta")
                    }

                    Text("Los CSV se guardan como: resultados_1.csv, resultados_2.csv...", fontSize = 12.sp, color = Color.Gray)

                    Button(
                        onClick = { showSettingsDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF616161))
                    ) {
                        Text("Cerrar")
                    }
                }
            }
        }
    }

    // Dialog: Mensaje de finalización (CSV guardado)
    showCompletionMessage?.let { (title, message) ->
        MessageDialog(
            title = title,
            message = message,
            onDismiss = { viewModel.dismissCompletionMessage() }
        )
    }
}

@Composable
fun RowScope.TableHeader(text: String, weight: Float) {
    Text(
        text = text,
        modifier = Modifier.weight(weight),
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp
    )
}

@Composable
fun RowScope.TableCell(text: String, weight: Float, color: Color = Color.Unspecified) {
    Text(
        text = text,
        modifier = Modifier
            .weight(weight)
            .padding(4.dp),
        fontSize = 10.sp,
        color = if (color != Color.Unspecified) color else Color.Unspecified,
        maxLines = 5
    )
}

@Composable
fun InstructionsDialog(
    initialText: String,
    onDismiss: () -> Unit,
    onSaveOnly: (String) -> Unit,
    onSaveToHistory: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.78f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Más indicaciones - Instrucciones para la IA",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Escribe aquí las indicaciones o instrucciones extra para resolver...") },
                    maxLines = 20
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onSaveOnly(text) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text("Guardar solamente")
                    }
                    Button(
                        onClick = { onSaveToHistory(text) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3)
                        )
                    ) {
                        Text("Guardar en historial")
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryDialog(
    items: List<HistoryItem>,
    onDismiss: () -> Unit,
    onItemClick: (HistoryItem) -> Unit,
    onItemLongPress: (HistoryItem) -> Unit,
    onClear: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.86f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Historial de indicaciones (tarjetas)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(8.dp)
                )
                
                if (items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Aún no hay instrucciones guardadas.")
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items.reversed().take(200).forEachIndexed { revIndex, item ->
                            val index = items.size - 1 - revIndex
                            HistoryCard(
                                item = item,
                                index = index,
                                onClick = { onItemClick(item) },
                                onLongPress = { onItemLongPress(item) }
                            )
                        }
                    }
                }
                
                // Botonera inferior: Exportar, Importar, Borrar, Cerrar
                // Usamos un GridLayout de 2 columnas para que los botones sean grandes y táctiles
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = onExport,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1B5E20) // Verde oscuro
                            )
                        ) {
                            Text("Exportar JSON", fontSize = 12.sp)
                        }
                        Button(
                            onClick = onImport,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1976D2) // Azul
                            )
                        ) {
                            Text("Importar JSON", fontSize = 12.sp)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = onClear,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD32F2F) // Rojo
                            )
                        ) {
                            Text("Borrar todo", fontSize = 12.sp)
                        }
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF616161) // Gris
                            )
                        ) {
                            Text("Cerrar", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryCard(
    item: HistoryItem,
    index: Int,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x0FFFFFFF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = index.toString(),
                modifier = Modifier.width(44.dp),
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.titulo,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = if (item.contenido.length <= 220) item.contenido else item.contenido.take(217) + "...",
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Text(
                    text = item.fecha,
                    fontSize = 10.sp,
                    color = Color.Gray,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

@Composable
fun RestoreDialog(
    item: HistoryItem,
    onDismiss: () -> Unit,
    onRestore: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "¿Restaurar esta indicación?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.titulo,
                    fontWeight = FontWeight.Bold
                )
                // Contenido con scroll por si es muy largo
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = item.contenido,
                        fontSize = 12.sp
                    )
                }

                // Botones siempre visibles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onRestore,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text("Restaurar")
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }
                }
            }
        }
    }
}

@Composable
fun LongPressOptionsDialog(
    item: HistoryItem,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Opciones para:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.titulo,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (item.contenido.length <= 180) item.contenido else item.contenido.take(180) + "...",
                    fontSize = 12.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF44336)
                        )
                    ) {
                        Text("Eliminar")
                    }
                    Button(
                        onClick = onRename,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF9800)
                        )
                    ) {
                        Text("Renombrar")
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }
                }
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .fillMaxHeight(0.4f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(text = message)
                
                Spacer(modifier = Modifier.weight(1f))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF44336)
                        )
                    ) {
                        Text("Sí")
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }
                }
            }
        }
    }
}

@Composable
fun RenameDialog(
    currentTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newTitle by remember { mutableStateOf(currentTitle) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .fillMaxHeight(0.4f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Cambiar nombre",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("Nuevo título:")
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onConfirm(newTitle) },
                        modifier = Modifier.weight(1f),
                        enabled = newTitle.isNotEmpty()
                    ) {
                        Text("Aceptar")
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }
                }
            }
        }
    }
}

@Composable
fun MessageDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .fillMaxHeight(0.38f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(text = message)
                
                Spacer(modifier = Modifier.weight(1f))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    Text("Aceptar")
                }
            }
        }
    }
}

@Composable
fun ImportChoiceDialog(
    importCount: Int,
    onDismiss: () -> Unit,
    onMerge: () -> Unit,
    onReplace: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.45f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                Text(
                    text = "Importar Respaldo",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (importCount > 0) {
                        "Se encontraron $importCount entradas en el respaldo.\n¿Qué deseas hacer?"
                    } else {
                        "Selecciona cómo deseas importar el respaldo:"
                    },
                    fontSize = 14.sp
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onMerge,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1976D2) // Azul
                        )
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Fusionar", fontSize = 12.sp)
                            Text("(Unir ambos)", fontSize = 10.sp)
                        }
                    }
                    Button(
                        onClick = onReplace,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD32F2F) // Rojo
                        )
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Reemplazar", fontSize = 12.sp)
                            Text("(Borrar actual)", fontSize = 10.sp)
                        }
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

