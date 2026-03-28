package com.mathsnip.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mathsnip.app.ui.theme.*

@Composable
fun FilesScreen() {
    var showSheet by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(BG)) {
        Column(modifier = Modifier.fillMaxSize()) {
            MathTopBar(
                title = "Archivos",
                actions = {
                    IconButton(onClick = {}) {
                        Text("⋯", fontSize = 22.sp, color = Muted)
                    }
                }
            )
            SearchBar("Buscar archivos...")

            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip("Todos", true, Accent)
                FilterChip("Notas", false, Blue)
                FilterChip("PDFs", false, Pink)
            }

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("📁", fontSize = 56.sp)
                    Text("Sin archivos", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPri)
                    Text("Organiza tus capturas en carpetas", fontSize = 13.sp, color = Muted)
                }
            }
        }

        FloatingActionButton(
            onClick = { showSheet = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 80.dp),
            containerColor = Accent,
            contentColor = Color.White,
            shape = CircleShape
        ) { Icon(Icons.Default.Add, null, modifier = Modifier.size(26.dp)) }

        if (showSheet) {
            ModalBottomSheetContent(
                title = "Nuevo",
                items = listOf(
                    Triple("📄", "Nueva nota") { showSheet = false },
                    Triple("📁", "Nueva carpeta") { showSheet = false },
                    Triple("📋", "Subir PDF") { showSheet = false },
                    Triple("∑", "Nuevo snip") { showSheet = false },
                ),
                onDismiss = { showSheet = false }
            )
        }
    }
}

@Composable
fun NotesScreen() {
    var showSheet by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(BG)) {
        Column(modifier = Modifier.fillMaxSize()) {
            MathTopBar(
                title = "Notas",
                actions = {
                    IconButton(onClick = {}) {
                        Text("⬆", fontSize = 18.sp, color = Muted)
                    }
                    IconButton(onClick = {}) {
                        Text("⋯", fontSize = 22.sp, color = Muted)
                    }
                }
            )
            SearchBar("Buscar notas...")

            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Text("Todas  ⌄", fontSize = 13.sp, color = TextSec)
            }

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text("📝", fontSize = 56.sp)
                    Text(
                        "Sin notas",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPri
                    )
                    Text(
                        "Crea una nota vacía o sube un archivo de texto",
                        fontSize = 13.sp,
                        color = Muted
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { showSheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Blue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("+ Nueva nota", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showSheet = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 80.dp),
            containerColor = Blue,
            contentColor = Color.White,
            shape = CircleShape
        ) { Icon(Icons.Default.Add, null, modifier = Modifier.size(26.dp)) }

        if (showSheet) {
            ModalBottomSheetContent(
                title = "Nueva nota",
                items = listOf(
                    Triple("📄", "Nota vacía") { showSheet = false },
                    Triple("⬆", "Subir archivo") { showSheet = false },
                ),
                onDismiss = { showSheet = false }
            )
        }
    }
}
