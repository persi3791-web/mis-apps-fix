package com.mathsnip.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Paleta principal - Dark Material You
val BG        = Color(0xFF0D0D12)
val BG2       = Color(0xFF111118)
val Surface   = Color(0xFF1A1A24)
val Surface2  = Color(0xFF22222E)
val Surface3  = Color(0xFF161620)
val SurfaceEl = Color(0xFF2A2A38)

// Accent colors
val Accent    = Color(0xFF5B8DEF)
val AccentDim = Color(0xFF1E3566)
val AccentBg  = Color(0xFF0F1E3A)

val Green     = Color(0xFF2DD4A0)
val GreenDim  = Color(0xFF0D3D2E)
val GreenBg   = Color(0xFF091F18)

val Pink      = Color(0xFFEF5B8D)
val PinkDim   = Color(0xFF3D1E2E)

val Blue      = Color(0xFF5B8DEF)
val BlueDim   = Color(0xFF1E2E5C)

val Teal      = Color(0xFF2DD4C8)
val TealDim   = Color(0xFF0D3D3A)

val Orange    = Color(0xFFEF8C5B)
val OrangeDim = Color(0xFF3D2A1A)

val Purple    = Color(0xFF9B5BEF)
val PurpleDim = Color(0xFF2A1A5C)

// Text
val TextPri   = Color(0xFFF0F0FF)
val TextSec   = Color(0xFFB0B0D0)
val Muted     = Color(0xFF606080)
val Divider   = Color(0xFF252535)

// Card
val CardBg    = Color(0xFF1E1E2A)
val CardWarm  = Color(0xFF1C1C26)

private val DarkColorScheme = darkColorScheme(
    primary          = Accent,
    onPrimary        = Color.White,
    primaryContainer = AccentDim,
    secondary        = Green,
    onSecondary      = Color.White,
    secondaryContainer = GreenDim,
    tertiary         = Teal,
    background       = BG,
    onBackground     = TextPri,
    surface          = Surface,
    onSurface        = TextPri,
    surfaceVariant   = Surface2,
    onSurfaceVariant = TextSec,
    outline          = Divider,
    error            = Pink,
    onError          = Color.White,
)

val AppTypography = Typography(
    headlineLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPri),
    headlineMedium = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPri),
    headlineSmall = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPri),
    titleLarge = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPri),
    titleMedium = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPri),
    bodyLarge = TextStyle(fontSize = 15.sp, color = TextPri),
    bodyMedium = TextStyle(fontSize = 13.sp, color = TextSec),
    bodySmall = TextStyle(fontSize = 11.sp, color = Muted),
    labelLarge = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 10.sp, color = Muted),
)

@Composable
fun MathSnipTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = AppTypography,
        content     = content
    )
}
