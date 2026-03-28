package com.mathsnip.app.network

import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

object TextRecognizer {

    var inlineDelim: String = "\\( ... \\)"
    var blockDelimUn: String = "\\[ ... \\]"
    var blockDelimN: String = "\\begin{equation}"

    fun applyDelimiters(latex: String): String {
        if (latex.isBlank()) return latex
        val trimmed = latex.trim()
        if (trimmed.startsWith("\\(") || trimmed.startsWith("\\[") ||
            trimmed.startsWith("$$") || trimmed.startsWith("$") ||
            trimmed.startsWith("\\begin{")) return trimmed
        val parts = blockDelimUn.split(" ... ")
        val open = parts.firstOrNull()?.trim() ?: "\\["
        val close = parts.lastOrNull()?.trim() ?: "\\]"
        return "$open\n$trimmed\n$close"
    }

    private fun isMathHeavy(text: String): Boolean {
        val mathSymbols = listOf("+", "-", "=", "×", "÷", "∑", "∫", "√", "²", "³", "π", "^", "/")
        val mathCount = mathSymbols.count { text.contains(it) }
        return mathCount >= 2 || text.contains(Regex("[a-z]\\s*[=+\\-]\\s*[0-9a-z]"))
    }

    suspend fun recognizeText(file: File): Result<String> {
        return suspendCancellableCoroutine { cont ->
            try {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    ?: return@suspendCancellableCoroutine cont.resume(Result.failure(Exception("No se pudo leer la imagen")))
                val image = InputImage.fromBitmap(bitmap, 0)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        cont.resume(Result.success(visionText.text.trim()))
                    }
                    .addOnFailureListener { e ->
                        cont.resume(Result.failure(e))
                    }
            } catch (e: Exception) {
                cont.resume(Result.failure(e))
            }
        }
    }

    fun combineResults(mlKitText: String, latexResult: String): String {
        val cleanText = mlKitText.trim()
        val cleanLatex = latexResult.trim()
        if (cleanText.isEmpty() && cleanLatex.isEmpty()) return ""
        if (cleanLatex.isEmpty()) return cleanText
        if (cleanText.isEmpty()) return applyDelimiters(cleanLatex)
        if (isLatexGarbage(cleanLatex)) return cleanText
        if (!hasMathContent(cleanText)) return cleanText
        return applyDelimiters(cleanLatex)
    }

    private fun isLatexGarbage(latex: String): Boolean {
        val scriptCount = latex.split("scriptstyle").size - 1
        val mathrm = latex.split("mathrm").size - 1
        if (scriptCount > 5 || mathrm > 10) return true
        val backslashDensity = latex.count { it == '\\' }.toFloat() / latex.length
        if (backslashDensity > 0.3f && latex.length > 100) return true
        return false
    }

    private fun hasMathContent(text: String): Boolean {
        val mathSymbols = listOf("=", "+", "-", "×", "÷", "∑", "∫", "√", "²", "³", "π", "∞", "≈", "≠", "≤", "≥")
        val mathCount = mathSymbols.count { text.contains(it) }
        val hasEquation = text.contains(Regex("[a-zA-Z]\\s*[=]\\s*[0-9a-zA-Z]"))
        return mathCount >= 2 || hasEquation
    }
}
