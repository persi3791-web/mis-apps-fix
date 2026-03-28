package com.mathsnip.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mathsnip.app.MainViewModel
import com.mathsnip.app.ui.theme.*

@Composable
fun HomeScreen(viewModel: MainViewModel, onNavigate: (String) -> Unit) {
    val snips by viewModel.snips.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BG),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Header con gradiente
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Surface3, BG),
                            startY = 0f,
                            endY = 300f
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "MathSnip",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPri
                            )
                            Text(
                                "Captura y organiza contenido",
                                fontSize = 13.sp,
                                color = Muted
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(AccentBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("∑", fontSize = 22.sp, color = Accent)
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatChip("Snips", snips.count { it.type == "snip" || it.type.isEmpty() }.toString(), Accent)
                        StatChip("PDFs", snips.count { it.type == "pdf" }.toString(), Pink)
                        StatChip("Imágenes", snips.count { it.type == "image" }.toString(), Green)
                    }
                }
            }
        }

        // Search
        item {
            SearchBar("Buscar en tus capturas...")
        }

        // Filter chips
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { FilterChip("Todo", true, Accent) }
                item { FilterChip("Notas", false, Blue) }
                item { FilterChip("PDFs", false, Pink) }
                item { FilterChip("Snips", false, Green) }
                item { FilterChip("Imágenes", false, Teal) }
            }
        }

        item { Spacer(Modifier.height(20.dp)) }

        // Quick actions
        item {
            Text(
                "Capturar",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPri,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Cámara card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(Accent.copy(0.3f), AccentDim)))
                        .clickable { onNavigate("snips") }
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("📷", fontSize = 28.sp)
                        Text("Cámara", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPri)
                        Text("Toma una foto", fontSize = 11.sp, color = TextSec)
                    }
                }

                // PDF card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(Pink.copy(0.3f), PinkDim)))
                        .clickable { onNavigate("pdfs") }
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("📄", fontSize = 28.sp)
                        Text("PDF", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPri)
                        Text("Extrae contenido", fontSize = 11.sp, color = TextSec)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Galería
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(Green.copy(0.3f), GreenDim)))
                        .clickable { onNavigate("snips") }
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🖼️", fontSize = 28.sp)
                        Text("Galería", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPri)
                        Text("Sube imagen", fontSize = 11.sp, color = TextSec)
                    }
                }

                // Notas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(Purple.copy(0.3f), PurpleDim)))
                        .clickable { onNavigate("notes") }
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("✏️", fontSize = 28.sp)
                        Text("Notas", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPri)
                        Text("Escribe texto", fontSize = 11.sp, color = TextSec)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }

        // Recientes
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recientes", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPri)
                Text("Ver todo →", fontSize = 13.sp, color = Accent,
                    modifier = Modifier.clickable { onNavigate("snips") })
            }
        }

        if (snips.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Surface)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("∑", fontSize = 40.sp, color = Muted)
                        Text("Sin capturas aún", fontSize = 15.sp, color = Muted)
                        Text("Usa la cámara o sube un PDF para comenzar",
                            fontSize = 13.sp, color = Muted.copy(0.6f))
                    }
                }
            }
        } else {
            items(snips.take(5)) { snip ->
                RecentItem(snip, onClick = { viewModel.copyToClipboard(snip.latex) })
            }
        }
    }
}

@Composable
fun RecentItem(snip: com.mathsnip.app.data.Snip, onClick: () -> Unit) {
    val (icon, color) = when (snip.type) {
        "pdf" -> "📄" to Pink
        "image" -> "🖼️" to Teal
        else -> "∑" to Green
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 16.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (snip.latex.length > 60) snip.latex.take(60) + "…" else snip.latex,
                fontSize = 13.sp,
                color = TextSec,
                lineHeight = 18.sp
            )
            Text(snip.date, fontSize = 11.sp, color = Muted)
        }
        Text("⎘", fontSize = 16.sp, color = Muted)
    }
}
