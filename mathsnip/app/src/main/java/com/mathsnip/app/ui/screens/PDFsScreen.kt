package com.mathsnip.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mathsnip.app.MainViewModel
import com.mathsnip.app.ScanState
import com.mathsnip.app.data.Snip
import com.mathsnip.app.ui.theme.*
import java.io.File

@Composable
fun PDFsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val ui by viewModel.ui.collectAsState()
    val snips by viewModel.snips.collectAsState()
    val pdfSnips = snips.filter { it.type == "pdf" }
    var showSheet by remember { mutableStateOf(false) }
    var viewingSnip by remember { mutableStateOf<Snip?>(null) }
    var viewingPdfUri by remember { mutableStateOf<Uri?>(null) }

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewingPdfUri = it }
    }

    Box(modifier = Modifier.fillMaxSize().background(BG)) {
        Column(modifier = Modifier.fillMaxSize()) {
            MathTopBar(
                title = "PDFs",
                subtitle = "${pdfSnips.size} documentos",
                actions = { IconButton(onClick = {}) { Text("⋯", fontSize = 22.sp, color = Muted) } }
            )
            SearchBar("Buscar documentos...")

            if (ui.scanState == ScanState.SCANNING) {
                ProcessingBanner(ui.scanProgress)
            }

            if (pdfSnips.isEmpty() && ui.scanState != ScanState.SCANNING) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text("📄", fontSize = 56.sp)
                        Text("Sin documentos PDF", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPri)
                        Text("Sube un PDF para extraer texto e imágenes automáticamente", fontSize = 13.sp, color = Muted)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { pdfLauncher.launch("application/pdf") },
                            colors = ButtonDefaults.buttonColors(containerColor = Pink),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(0.6f)
                        ) { Text("⬆  Subir PDF", color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(pdfSnips, key = { it.id }) { snip ->
                        PdfSnipCard(
                            snip = snip,
                            onClick = { viewingSnip = snip },
                            onDelete = { viewModel.deleteSnip(snip.id) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        FloatingActionButton(
            onClick = { showSheet = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 80.dp),
            containerColor = Pink, contentColor = Color.White, shape = CircleShape
        ) { Icon(Icons.Default.Add, null, modifier = Modifier.size(26.dp)) }

        if (showSheet) {
            ModalBottomSheetContent(
                title = "Añadir PDF",
                items = listOf(
                    Triple("⬆", "Subir PDF desde archivos") { showSheet = false; pdfLauncher.launch("application/pdf") },
                    Triple("📷", "Tomar fotos del documento") { showSheet = false },
                ),
                onDismiss = { showSheet = false }
            )
        }
    }

    viewingPdfUri?.let { uri ->
        PdfViewerScreen(
            uri = uri,
            context = context,
            onDismiss = { viewingPdfUri = null },
            onProcessVision = {
                viewingPdfUri = null
                viewModel.processPdf(uri)
            }
        )
    }

    viewingSnip?.let { snip ->
        PdfResultScreen(
            snip = snip,
            context = context,
            onDismiss = { viewingSnip = null },
            onDelete = { viewModel.deleteSnip(snip.id); viewingSnip = null }
        )
    }

    LaunchedEffect(ui.scanState) {
        if (ui.scanState == ScanState.SUCCESS) {
            kotlinx.coroutines.delay(2000)
            viewModel.resetScan()
        }
    }
}

@Composable
fun PdfViewerScreen(uri: Uri, context: Context, onDismiss: () -> Unit, onProcessVision: () -> Unit) {
    var pages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var currentPage by remember { mutableStateOf(0) }

    LaunchedEffect(uri) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val file = File(context.cacheDir, "viewer_${System.currentTimeMillis()}.pdf")
                file.outputStream().use { inputStream?.copyTo(it) }
                val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(fd)
                val bitmaps = mutableListOf<Bitmap>()
                for (i in 0 until renderer.pageCount) {
                    val page = renderer.openPage(i)
                    val scale = 2.0f
                    val w = (page.width * scale).toInt()
                    val h = (page.height * scale).toInt()
                    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    android.graphics.Canvas(bitmap).drawColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    bitmaps.add(bitmap)
                }
                renderer.close()
                fd.close()
                file.delete()
                pages = bitmaps
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF111111))) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(Surface3).padding(horizontal = 8.dp).height(56.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) { Text("←", fontSize = 22.sp, color = Accent) }
                    Text("PDFs", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPri, modifier = Modifier.weight(1f))
                    Text("${currentPage + 1} / ${pages.size}", fontSize = 13.sp, color = Muted)
                }
                HorizontalDivider(color = Divider)

                Box(modifier = Modifier.weight(1f)) {
                    if (pages.isNotEmpty()) {
                        Image(
                            bitmap = pages[currentPage].asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Pink)
                        }
                    }
                }

                if (pages.size > 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(Surface3).padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { if (currentPage > 0) currentPage-- }, enabled = currentPage > 0) {
                            Text("‹", fontSize = 28.sp, color = if (currentPage > 0) TextPri else Muted)
                        }
                        Text("Pág ${currentPage + 1} de ${pages.size}", fontSize = 13.sp, color = TextSec)
                        IconButton(onClick = { if (currentPage < pages.size - 1) currentPage++ }, enabled = currentPage < pages.size - 1) {
                            Text("›", fontSize = 28.sp, color = if (currentPage < pages.size - 1) TextPri else Muted)
                        }
                    }
                }
            }

            Button(
                onClick = onProcessVision,
                colors = ButtonDefaults.buttonColors(containerColor = Pink),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 100.dp).height(52.dp)
            ) {
                Text("👁 Visión", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun PdfResultScreen(snip: Snip, context: Context, onDismiss: () -> Unit, onDelete: () -> Unit) {
    var selectedFormat by remember { mutableStateOf("Markdown") }
    val formats = listOf("Markdown", "LaTeX", "HTML", "Texto")
    val content = snip.latex

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(modifier = Modifier.fillMaxSize().background(BG)) {
            Row(
                modifier = Modifier.fillMaxWidth().background(Surface3).padding(horizontal = 8.dp).height(56.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) { Text("←", fontSize = 22.sp, color = Accent) }
                Text("Resultado", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPri, modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("MathSnip", content))
                }) { Text("⎘", fontSize = 20.sp, color = Accent) }
            }
            HorizontalDivider(color = Divider)

            Row(
                modifier = Modifier.fillMaxWidth().background(Surface3).padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                formats.forEach { fmt ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedFormat == fmt) Pink else Surface2)
                            .clickable { selectedFormat = fmt }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(fmt, fontSize = 13.sp, color = if (selectedFormat == fmt) Color.White else TextSec, fontWeight = FontWeight.Medium)
                    }
                }
            }
            HorizontalDivider(color = Divider)

            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().background(Surface2).verticalScroll(rememberScrollState()).padding(16.dp)
            ) {
                Text(
                    text = content.ifBlank { "Sin contenido" },
                    fontSize = 13.sp, color = TextSec, lineHeight = 20.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().background(Surface3).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("MathSnip", content))
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("⎘ Copiar", color = Color.White, fontWeight = FontWeight.Bold) }

                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = PinkDim),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("🗑", fontSize = 18.sp) }
            }
        }
    }
}

@Composable
fun ProcessingBanner(progress: String) {
    Column(
        modifier = Modifier.fillMaxWidth().background(GreenBg).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Green, strokeWidth = 2.dp)
            Text(progress.ifEmpty { "Procesando..." }, fontSize = 13.sp, color = Green)
        }
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(1.dp)), color = Green, trackColor = GreenDim)
    }
}

@Composable
fun PdfSnipCard(snip: Snip, onClick: () -> Unit, onDelete: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface).clickable(onClick = onClick).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(Pink.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            Text("📄", fontSize = 22.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(snip.latex.lines().firstOrNull()?.take(40) ?: "Documento PDF", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPri)
            Spacer(Modifier.height(2.dp))
            Text("${snip.latex.lines().size} líneas · ${snip.date}", fontSize = 11.sp, color = Muted)
        }
        Box {
            Text("⋯", fontSize = 20.sp, color = Muted, modifier = Modifier.clickable { showMenu = true }.padding(4.dp))
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, modifier = Modifier.background(SurfaceEl)) {
                DropdownMenuItem(text = { Text("Ver contenido", color = TextPri, fontSize = 14.sp) }, onClick = { showMenu = false; onClick() })
                DropdownMenuItem(text = { Text("Eliminar", color = Pink, fontSize = 14.sp) }, onClick = { showMenu = false; onDelete() })
            }
        }
    }
}
