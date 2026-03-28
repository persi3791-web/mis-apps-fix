package com.example.cornell.ui.utils

import androidx.emoji2.text.EmojiCompat
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Procesa texto con EmojiCompat para renderizar emojis correctamente.
 */
fun processEmojis(text: String): String {
    return try {
        EmojiCompat.get().process(text).toString()
    } catch (_: Throwable) {
        text
    }
}

/**
 * Parsea Markdown básico a AnnotatedString para Jetpack Compose.
 * Equivalente a parse_markdown_to_kivy() del código Python original.
 * Soporta: **negrita**, *cursiva*, • viñetas. Incluye soporte para emojis.
 */
fun parseMarkdownToAnnotatedString(
    text: String,
    defaultColor: Color = Color.Unspecified,
    bulletColor: Color = Color(0xFF3385FF)
): AnnotatedString {
    if (text.isEmpty()) return AnnotatedString("")
    val processed = processEmojis(text)
    var html = processed
    // **texto** → placeholder para negrita
    html = Regex("\\*\\*(.*?)\\*\\*").replace(html) { "«B»${it.groupValues[1]}«/B»" }
    // *texto* → placeholder para cursiva (evitar * entre palabras)
    html = Regex("(?<!\\*)\\*([^*]+)\\*(?!\\*)").replace(html) { "«I»${it.groupValues[1]}«/I»" }
    // • → placeholder viñeta
    html = html.replace("•", "«BUL»•«/BUL»")

    return buildAnnotatedString {
        var i = 0
        val baseStyle = SpanStyle(color = defaultColor)
        while (i < html.length) {
            when {
                html.startsWith("«B»", i) -> {
                    val end = html.indexOf("«/B»", i)
                    if (end != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = defaultColor)) {
                            append(html.substring(i + 3, end))
                        }
                        i = end + 4
                    } else {
                        withStyle(baseStyle) { append(html[i]) }
                        i++
                    }
                }
                html.startsWith("«I»", i) -> {
                    val end = html.indexOf("«/I»", i)
                    if (end != -1) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = defaultColor)) {
                            append(html.substring(i + 3, end))
                        }
                        i = end + 4
                    } else {
                        withStyle(baseStyle) { append(html[i]) }
                        i++
                    }
                }
                html.startsWith("«BUL»", i) -> {
                    val end = html.indexOf("«/BUL»", i)
                    if (end != -1) {
                        withStyle(SpanStyle(color = bulletColor)) {
                            append(html.substring(i + 5, end))
                        }
                        i = end + 6
                    } else {
                        withStyle(baseStyle) { append(html[i]) }
                        i++
                    }
                }
                else -> {
                    append(html[i])
                    i++
                }
            }
        }
    }
}
