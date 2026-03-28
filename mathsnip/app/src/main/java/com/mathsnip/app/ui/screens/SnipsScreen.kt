package com.mathsnip.app.ui.screens

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.mathsnip.app.MainViewModel
import com.mathsnip.app.ScanState
import com.mathsnip.app.data.Snip
import com.mathsnip.app.ui.theme.*
import java.io.File

@Composable
fun SnipsScreen(viewModel: MainViewModel) {
    val snips by viewModel.snips.collectAsState()
    val ui by viewModel.ui.collectAsState()
    val context = LocalContext.current
    var showSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    var selectedSnip by remember { mutableStateOf<Snip?>(null) }

    // Launcher galería
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val file = uriToFile(context, it)
            if (file != null) viewModel.scanImage(file)
        }
    }

    // Launcher galería para Imgur
    val githubLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val file = uriToFile(context, it)
            if (file != null) viewModel.uploadImageToGitHub(file)
        }
    }

    // Launcher cámara
    var cameraFile by remember { mutableStateOf<File?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) cameraFile?.let { viewModel.scanImage(it) }
    }

    // Launcher cámara para Imgur
    var cameraFileGitHub by remember { mutableStateOf<File?>(null) }
    val cameraGitHubLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) cameraFileGitHub?.let { viewModel.uploadImageToGitHub(it) }
    }

    // Permiso cámara
    val cameraPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val file = File(context.externalCacheDir, "cam_${System.currentTimeMillis()}.jpg")
            cameraFile = file
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            cameraLauncher.launch(uri)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BG)) {
        Column(modifier = Modifier.fillMaxSize()) {
            MathTopBar(
                title = "Snips",
                subtitle = "${snips.count { it.type == "snip" || it.type.isEmpty() }} capturas",
                actions = {
                    IconButton(onClick = {}) {
                        Text("⋯", fontSize = 22.sp, color = Muted)
                    }
                }
            )
            SearchBar("Buscar snips...")

            // Banner de progreso
            if (ui.scanState == ScanState.SCANNING) {
                ProcessingBanner(ui.scanProgress.ifEmpty { "Escaneando imagen..." })
            }

            // Resultado exitoso
            if (ui.scanState == ScanState.SUCCESS && ui.scanMessage.isNotEmpty()) {
                SuccessBanner(ui.scanMessage)
            }

            if (snips.isEmpty() && ui.scanState != ScanState.SCANNING) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text("∑", fontSize = 56.sp, color = Muted)
                        Text(
                            "Sin snips aún",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPri
                        )
                        Text(
                            "Toma una foto o sube una imagen para reconocer texto y fórmulas",
                            fontSize = 13.sp,
                            color = Muted
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(snips.filter { it.type == "snip" || it.type.isEmpty() || it.type == "image" },
                        key = { it.id }) { snip ->
                        SnipCard(
                            snip = snip,
                            onCopy = { viewModel.copyToClipboard(snip.latex) },
                            onDelete = { showDeleteDialog = snip.id },
                            onClick = { selectedSnip = snip }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { showSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 80.dp),
            containerColor = Green,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(26.dp))
        }

        // Bottom Sheet
        if (showSheet) {
            ModalBottomSheetContent(
                title = "Nueva captura",
                items = listOf(
                    Triple("📷", "Tomar foto (OCR + LaTeX)") {
                        showSheet = false
                        cameraPermLauncher.launch(Manifest.permission.CAMERA)
                    },
                    Triple("🖼️", "Imagen desde galería (OCR + LaTeX)") {
                        showSheet = false
                        galleryLauncher.launch("image/*")
                    },
                    Triple("🔗", "Foto → Link GitHub") {
                        showSheet = false
                        val file = File(context.externalCacheDir, "cam_${System.currentTimeMillis()}.jpg")
                        cameraFileGitHub = file
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                        cameraGitHubLauncher.launch(uri)
                    },
                    Triple("⬆", "Imagen → Link GitHub") {
                        showSheet = false
                        githubLauncher.launch("image/*")
                    },
                ),
                onDismiss = { showSheet = false }
            )
        }

        // Delete dialog
        showDeleteDialog?.let { id ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                containerColor = Surface,
                shape = RoundedCornerShape(20.dp),
                title = { Text("¿Eliminar snip?", color = TextPri) },
                text = { Text("Esta acción no se puede deshacer.", color = TextSec) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteSnip(id)
                            showDeleteDialog = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Pink),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("Eliminar", color = Color.White) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) {
                        Text("Cancelar", color = Muted)
                    }
                }
            )
        }
    }

    LaunchedEffect(ui.scanState) {
        if (ui.scanState == ScanState.SUCCESS) {
            kotlinx.coroutines.delay(3000)
            viewModel.resetScan()
        }
    }
}

@Composable
fun SuccessBanner(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GreenBg)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("✓", fontSize = 16.sp, color = Green)
        Text(
            text = if (message.length > 60) message.take(60) + "…" else message,
            fontSize = 13.sp,
            color = Green,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SnipCard(
    snip: Snip,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val (icon, color) = when (snip.type) {
        "image" -> "🔗" to Teal
        else -> "∑" to Green
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .clickable(onClick = onClick)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 14.sp)
            }
            Text(
                when (snip.type) {
                    "image" -> "Imagen → Imgur"
                    else -> "Snip matemático"
                },
                fontSize = 12.sp,
                color = color,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(snip.date, fontSize = 11.sp, color = Muted)
            Box {
                Text(
                    "⋯",
                    fontSize = 18.sp,
                    color = Muted,
                    modifier = Modifier.clickable { showMenu = true }.padding(4.dp)
                )
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(SurfaceEl)
                ) {
                    DropdownMenuItem(
                        text = { Text("Copiar", color = TextPri, fontSize = 14.sp) },
                        onClick = { showMenu = false; onCopy() }
                    )
                    DropdownMenuItem(
                        text = { Text("Eliminar", color = Pink, fontSize = 14.sp) },
                        onClick = { showMenu = false; onDelete() }
                    )
                }
            }
        }

        HorizontalDivider(color = Divider, thickness = 0.5.dp)

        // Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface2)
                .padding(14.dp)
        ) {
            Text(
                text = if (snip.latex.length > 200) snip.latex.take(200) + "\n…" else snip.latex,
                fontSize = 13.sp,
                color = if (snip.type == "image") Accent else Orange,
                lineHeight = 20.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }

        // Copy button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onCopy)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("⎘  Copiar", fontSize = 12.sp, color = Accent)
        }
    }
}

fun uriToFile(context: Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val file = File(context.cacheDir, "img_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { inputStream.copyTo(it) }
        file
    } catch (e: Exception) { null }
}
