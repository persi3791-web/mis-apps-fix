package com.example.cornell.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CornellDarkScheme = darkColorScheme(
    primary = CornellPrimary,
    onPrimary = Color.White,
    secondary = CornellAccent,
    background = CornellBackground,
    surface = CornellSurface,
    onBackground = CornellText,
    onSurface = CornellText,
    onSurfaceVariant = CornellTextSecondary,
    error = CornellError,
    onError = Color.White
)

@Composable
fun CORNELLTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CornellDarkScheme,
        typography = Typography,
        content = content
    )
}