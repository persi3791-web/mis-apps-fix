package com.mathsnip.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.mathsnip.app.ui.screens.*
import com.mathsnip.app.ui.theme.*

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            MathSnipTheme {
                MathSnipApp(viewModel)
            }
        }
    }
}

data class NavTab(
    val id: String,
    val icon: String,
    val label: String,
    val color: androidx.compose.ui.graphics.Color
)

@Composable
fun MathSnipApp(viewModel: MainViewModel) {
    var currentTab by remember { mutableStateOf("home") }
    var subScreen by remember { mutableStateOf<String?>(null) }

    val tabs = listOf(
        NavTab("home",     "⌂",  "Inicio",   Accent),
        NavTab("files",    "⊞",  "Archivos", Accent),
        NavTab("notes",    "📝", "Notas",    Blue),
        NavTab("pdfs",     "📄", "PDFs",     Pink),
        NavTab("snips",    "∑",  "Snips",    Green),
        NavTab("settings", "⚙",  "Ajustes",  Muted),
    )

    subScreen?.let { sub ->
        when (sub) {
            "gen_settings"    -> GenSettingsScreen    { subScreen = null }
            "editor_settings" -> EditorSettingsScreen(viewModel) { subScreen = null }
            "pdf_settings"    -> PDFSettingsScreen    { subScreen = null }
            "export_settings" -> ExportSettingsScreen { subScreen = null }
            "lang_settings"   -> LangSettingsScreen   { subScreen = null }
        }
        return
    }

    Scaffold(
        containerColor = BG,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Column {
                HorizontalDivider(color = Divider, thickness = 1.dp)
                NavigationBar(
                    containerColor = Surface3,
                    tonalElevation = 0.dp,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    tabs.forEach { tab ->
                        val selected = currentTab == tab.id
                        val color = if (selected) tab.color else Muted
                        NavigationBarItem(
                            selected = selected,
                            onClick = { currentTab = tab.id },
                            icon = {
                                Text(
                                    tab.icon,
                                    fontSize = if (selected) 22.sp else 20.sp,
                                    color = color
                                )
                            },
                            label = {
                                Text(
                                    tab.label,
                                    fontSize = 10.sp,
                                    color = color,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = if (selected) tab.color.copy(alpha = 0.15f) else Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BG)
        ) {
            when (currentTab) {
                "home"     -> HomeScreen(viewModel) { dest -> currentTab = dest }
                "files"    -> FilesScreen()
                "notes"    -> NotesScreen()
                "pdfs"     -> PDFsScreen(viewModel)
                "snips"    -> SnipsScreen(viewModel)
                "settings" -> SettingsScreen(viewModel) { sub -> subScreen = sub }
            }
        }
    }
}
