package com.mathsnip.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mathsnip.app.ApiStatus
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.mathsnip.app.MainViewModel
import com.mathsnip.app.R
import com.mathsnip.app.ui.theme.*

@Composable
fun SettingsScreen(viewModel: MainViewModel, onNavigate: (String) -> Unit) {
    val ui by viewModel.ui.collectAsState()

    LaunchedEffect(Unit) { viewModel.checkApi() }

    Column(modifier = Modifier.fillMaxSize().background(BG)) {
        MathTopBar(title = "")

        LazyColumn(modifier = Modifier.fillMaxSize()) {

            // Profile header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().background(Surface3)
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier.size(54.dp).clip(CircleShape).background(AccentDim),
                        contentAlignment = Alignment.Center
                    ) { Text("M", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Accent) }
                    Column {
                        Text("MathSnip", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPri)
                        Text(stringResource(R.string.free_plan), fontSize = 13.sp, color = Green)
                    }
                }
            }

            // API Status
            item {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Surface)
                        .padding(horizontal = 16.dp)
                        .height(52.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("⚡", fontSize = 18.sp, color = Accent)
                    when (ui.apiStatus) {
                        ApiStatus.CHECKING -> {
                            Text(stringResource(R.string.api_checking), fontSize = 14.sp, color = Muted)
                            Spacer(Modifier.width(8.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Accent, strokeWidth = 2.dp
                            )
                        }
                        ApiStatus.CONNECTED ->
                            Text(stringResource(R.string.api_connected), fontSize = 14.sp, color = Green)
                        ApiStatus.ERROR ->
                            Text(stringResource(R.string.api_error), fontSize = 14.sp, color = Pink)
                    }
                }
            }

            // Menu items
            item {
                SettingsRow("⚙", stringResource(R.string.settings_general)) { onNavigate("gen_settings") }
                SettingsRow("✏", stringResource(R.string.settings_editor)) { onNavigate("editor_settings") }
                SettingsRow("PDF", stringResource(R.string.settings_pdf)) { onNavigate("pdf_settings") }
                SettingsRow("⬆", stringResource(R.string.settings_export)) { onNavigate("export_settings") }
                SettingsRow("🌐", stringResource(R.string.settings_lang)) { onNavigate("lang_settings") }
            }

            // GPT-4o IA — activar/desactivar
            item {
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Surface)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("🤖", fontSize = 20.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("GPT-4o IA", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPri)
                            Text("Análisis avanzado de imágenes y fórmulas", fontSize = 12.sp, color = Muted)
                        }
                        Switch(
                            checked = ui.gptEnabled,
                            onCheckedChange = { viewModel.toggleGpt(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Accent,
                                uncheckedThumbColor = Color(0xFF666680),
                                uncheckedTrackColor = Surface2,
                                uncheckedBorderColor = Color.Transparent
                            )
                        )
                    }
                    if (ui.gptEnabled) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(AccentBg)
                                .padding(10.dp)
                        ) {
                            Text(
                                "✓ GPT-4o activo — usando IA para OCR y fórmulas",
                                fontSize = 12.sp,
                                color = Accent
                            )
                        }
                    }
                }
            }

            // GPT-4o Token
            item {
                Spacer(Modifier.height(8.dp))
                var gptTokenInput by remember { mutableStateOf("") }
                var showGptToken by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Surface)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("🔑", fontSize = 20.sp)
                        Text(
                            "Token GPT-4o",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPri,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (ui.gptTokenValid) GreenDim else Surface2)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                if (ui.gptTokenValid) "✓ Activo" else "Sin configurar",
                                fontSize = 11.sp,
                                color = if (ui.gptTokenValid) Green else Muted
                            )
                        }
                    }
                    Text(
                        "Token de GitHub con acceso a GitHub Models (GPT-4o)",
                        fontSize = 12.sp,
                        color = Muted
                    )
                    OutlinedTextField(
                        value = gptTokenInput,
                        onValueChange = { gptTokenInput = it },
                        placeholder = { Text("ghp_... o gho_...", color = Muted, fontSize = 13.sp) },
                        visualTransformation = if (showGptToken) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            Text(
                                if (showGptToken) "🙈" else "👁",
                                fontSize = 16.sp,
                                modifier = Modifier.clickable { showGptToken = !showGptToken }.padding(8.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Accent,
                            unfocusedBorderColor = Divider,
                            focusedTextColor = TextPri,
                            unfocusedTextColor = TextSec,
                            cursorColor = Accent
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    Button(
                        onClick = {
                            if (gptTokenInput.isNotBlank()) {
                                viewModel.setGptToken(gptTokenInput.trim())
                                gptTokenInput = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent),
                        shape = RoundedCornerShape(10.dp),
                        enabled = gptTokenInput.isNotBlank()
                    ) {
                        Text("Guardar Token GPT-4o", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // GitHub Token — para subir imágenes
            item {
                Spacer(Modifier.height(8.dp))
                var tokenInput by remember { mutableStateOf("") }
                var showToken by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Surface)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("🐙", fontSize = 20.sp)
                        Text(
                            "GitHub Token",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPri,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (ui.githubTokenValid) GreenDim else Surface2)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                if (ui.githubTokenValid) "✓ Activo" else "Sin configurar",
                                fontSize = 11.sp,
                                color = if (ui.githubTokenValid) Green else Muted
                            )
                        }
                    }
                    Text(
                        "Necesario para subir imágenes y generar links permanentes",
                        fontSize = 12.sp,
                        color = Muted
                    )
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        placeholder = { Text("ghp_...", color = Muted, fontSize = 13.sp) },
                        visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            Text(
                                if (showToken) "🙈" else "👁",
                                fontSize = 16.sp,
                                modifier = Modifier.clickable { showToken = !showToken }.padding(8.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Accent,
                            unfocusedBorderColor = Divider,
                            focusedTextColor = TextPri,
                            unfocusedTextColor = TextSec,
                            cursorColor = Accent
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    Button(
                        onClick = {
                            if (tokenInput.isNotBlank()) {
                                viewModel.setGitHubToken(tokenInput.trim())
                                tokenInput = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent),
                        shape = RoundedCornerShape(10.dp),
                        enabled = tokenInput.isNotBlank()
                    ) {
                        Text("Guardar Token", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

            item {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .background(Surface)
                        .clickable {}
                        .padding(horizontal = 16.dp)
                        .height(54.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        stringResource(R.string.sign_out),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Pink
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun GenSettingsScreen(onBack: () -> Unit) {
    var darkMode by remember { mutableStateOf(true) }
    var autoCopy by remember { mutableStateOf(true) }
    var enhanced by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize().background(BG)) {
        MathTopBarBack(stringResource(R.string.settings_general), onBack)
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            item { SectionHeader(stringResource(R.string.general)) }
            item { ToggleRow(stringResource(R.string.dark_mode), checked = darkMode) { darkMode = it } }
            item {
                ToggleRow(
                    stringResource(R.string.auto_copy),
                    stringResource(R.string.auto_copy_sub), autoCopy
                ) { autoCopy = it }
            }
            item { SectionHeader(stringResource(R.string.advanced)) }
            item {
                ToggleRow(
                    stringResource(R.string.enhanced_diagram),
                    stringResource(R.string.enhanced_diagram_sub), enhanced
                ) { enhanced = it }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun EditorSettingsScreen(viewModel: com.mathsnip.app.MainViewModel, onBack: () -> Unit) {
    var lineNumbers by remember { mutableStateOf(false) }
    val ui by viewModel.ui.collectAsState()

    val allDelimOptions = listOf(
        "\\( ... \\)", "$ ... $", "$$ ... $$", "\\[ ... \\]",
        "\\begin{equation} ... \\end{equation}",
        "\\begin{equation*} ... \\end{equation*}",
        "\\begin{align} ... \\end{align}",
        "\\begin{align*} ... \\end{align*}",
        "\\begin{math} ... \\end{math}"
    )

    var showInlineMenu by remember { mutableStateOf(false) }
    var showBlockUnMenu by remember { mutableStateOf(false) }
    var showBlockNMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(BG)) {
        MathTopBarBack(stringResource(R.string.settings_editor), onBack)
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            item { SectionHeader(stringResource(R.string.general)) }
            item { ToggleRow(stringResource(R.string.show_line_numbers), checked = lineNumbers) { lineNumbers = it } }
            item { DropdownRow(stringResource(R.string.tab_size), "4") }
            item { SectionHeader(stringResource(R.string.latex_settings)) }

            item {
                Box {
                    DropdownRow(stringResource(R.string.inline_delimiters), ui.inlineDelim) { showInlineMenu = true }
                    DropdownMenu(expanded = showInlineMenu, onDismissRequest = { showInlineMenu = false }, modifier = Modifier.background(SurfaceEl)) {
                        allDelimOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt, color = TextPri, fontSize = 14.sp) },
                                onClick = {
                                    showInlineMenu = false
                                    viewModel.saveDelimiters(opt, ui.blockDelimUn, ui.blockDelimN)
                                }
                            )
                        }
                    }
                }
            }

            item {
                Box {
                    DropdownRow(stringResource(R.string.block_delimiters_un), ui.blockDelimUn) { showBlockUnMenu = true }
                    DropdownMenu(expanded = showBlockUnMenu, onDismissRequest = { showBlockUnMenu = false }, modifier = Modifier.background(SurfaceEl)) {
                        allDelimOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt, color = TextPri, fontSize = 14.sp) },
                                onClick = {
                                    showBlockUnMenu = false
                                    viewModel.saveDelimiters(ui.inlineDelim, opt, ui.blockDelimN)
                                }
                            )
                        }
                    }
                }
            }

            item {
                Box {
                    DropdownRow(stringResource(R.string.block_delimiters_n), ui.blockDelimN) { showBlockNMenu = true }
                    DropdownMenu(expanded = showBlockNMenu, onDismissRequest = { showBlockNMenu = false }, modifier = Modifier.background(SurfaceEl)) {
                        allDelimOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt, color = TextPri, fontSize = 14.sp) },
                                onClick = {
                                    showBlockNMenu = false
                                    viewModel.saveDelimiters(ui.inlineDelim, ui.blockDelimUn, opt)
                                }
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun PDFSettingsScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(BG)) {
        MathTopBarBack(stringResource(R.string.settings_pdf), onBack)
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            item { DropdownRow(stringResource(R.string.section_numbering), stringResource(R.string.preserve)) }
            item { DropdownRow(stringResource(R.string.equation_numbering), stringResource(R.string.preserve)) }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun ExportSettingsScreen(onBack: () -> Unit) {
    val sections = listOf(
        R.string.export_docx to "Georgia",
        R.string.export_pdf_html to "Lora",
        R.string.export_pdf_latex to "CMU Serif",
        R.string.export_latex to "CMU Serif"
    )
    var showExport by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize().background(BG)) {
        MathTopBarBack(stringResource(R.string.settings_export), onBack)
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            sections.forEach { (labelRes, font) ->
                item { SectionHeader(stringResource(labelRes)) }
                item { DropdownRow(stringResource(R.string.font), font) }
                item { DropdownRow(stringResource(R.string.font_size), "11") }
                item { ToggleRow(stringResource(R.string.always_show_export), checked = showExport) { showExport = it } }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun LangSettingsScreen(onBack: () -> Unit) {
    val langs = listOf("Español" to "es", "English" to "en")
    var selected by remember { mutableStateOf("es") }

    Column(modifier = Modifier.fillMaxSize().background(BG)) {
        MathTopBarBack(stringResource(R.string.settings_lang), onBack)
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            item { SectionHeader(stringResource(R.string.language)) }
            langs.forEach { (name, code) ->
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clickable { selected = code }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(name, fontSize = 15.sp, color = TextPri, modifier = Modifier.weight(1f))
                        if (selected == code)
                            Text("✓", fontSize = 20.sp, color = Accent)
                    }
                    HorizontalDivider(color = Divider, thickness = 0.5.dp)
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
