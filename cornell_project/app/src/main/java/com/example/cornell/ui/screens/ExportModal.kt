package com.example.cornell.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cornell.api.ModelPreference
import com.example.cornell.ui.CornellViewModel
import com.example.cornell.ui.theme.CornellCard
import com.example.cornell.ui.theme.CornellPrimary
import com.example.cornell.ui.theme.CornellText
import com.example.cornell.ui.theme.CornellTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExportModal(
    onDismiss: () -> Unit,
    viewModel: CornellViewModel
) {
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportBackup(it) }
        onDismiss()
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importBackup(it) }
        onDismiss()
    }

    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val defaultFileName = "cornell_backup_$timestamp.json"

    var useGpt4 by remember { mutableStateOf(ModelPreference.useGpt4) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        tint = CornellPrimary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        "Gestionar Datos",
                        style = MaterialTheme.typography.titleLarge,
                        color = CornellText
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = CornellTextSecondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CornellCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Exportar TODO", style = MaterialTheme.typography.titleMedium, color = CornellText)
                        Text("Incluye: Notas, Resumen, Flashcards, Quizzes, Transcripción", style = MaterialTheme.typography.bodySmall, color = CornellTextSecondary)
                        OutlinedButton(onClick = { exportLauncher.launch(defaultFileName) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                            Text("Exportar Backup Completo")
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = CornellCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Importar TODO", style = MaterialTheme.typography.titleMedium, color = CornellText)
                        Text("Añade las notas del backup sin borrar las actuales", style = MaterialTheme.typography.bodySmall, color = CornellTextSecondary)
                        OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                            Text("Seleccionar Archivo de Backup")
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = CornellCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Modelo de IA", style = MaterialTheme.typography.titleMedium, color = CornellText)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(if (useGpt4) "GPT-4o activo" else "Gemini activo", style = MaterialTheme.typography.bodyMedium, color = CornellText)
                                Text(if (useGpt4) "GitHub Models (gratuito)" else "Google AI (gratuito)", style = MaterialTheme.typography.bodySmall, color = CornellTextSecondary)
                            }
                            Switch(
                                checked = useGpt4,
                                onCheckedChange = { useGpt4 = it; ModelPreference.useGpt4 = it }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {},
        containerColor = CornellCard
    )
}
