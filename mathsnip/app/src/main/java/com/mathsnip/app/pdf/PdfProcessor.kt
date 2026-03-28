package com.mathsnip.app.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.mathsnip.app.network.GitHubUploader
import com.mathsnip.app.network.GptClient
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.mathsnip.app.network.TextRecognizer
import com.mathsnip.app.network.ApiClient
import java.io.File
import java.io.FileOutputStream

data class PdfPage(val pageNumber: Int, val bitmap: Bitmap)
data class PdfImageRegion(val pageNumber: Int, val imageFile: File, val x: Int, val y: Int, val width: Int, val height: Int)
data class ExtractedImage(val pageNumber: Int, val localFile: File, val githubLink: String = "", val markdownTag: String = "")
data class PdfPageResult(val pageNumber: Int, val text: String, val images: List<ExtractedImage>)
data class PdfProcessResult(
    val pageCount: Int,
    val pages: List<PdfPageResult>,
    val extractedImages: List<ExtractedImage>,
    val markdownContent: String,
    val latexContent: String,
    val htmlContent: String,
    val plainText: String
)

object PdfProcessor {

    private const val RENDER_DPI = 220
    private const val MIN_IMAGE_AREA = 60 * 60

    fun copyPdfToCache(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val file = File(context.cacheDir, "pdf_${System.currentTimeMillis()}.pdf")
            file.outputStream().use { inputStream.copyTo(it) }
            file
        } catch (e: Exception) { null }
    }

    fun renderPages(pdfFile: File): List<PdfPage> {
        val pages = mutableListOf<PdfPage>()
        try {
            val fd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val scale = RENDER_DPI / 72f
                val width = (page.width * scale).toInt()
                val height = (page.height * scale).toInt()
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                pages.add(PdfPage(i + 1, bitmap))
            }
            renderer.close()
            fd.close()
        } catch (e: Exception) { e.printStackTrace() }
        return pages
    }

    fun detectImageRegions(bitmap: Bitmap, pageNumber: Int, cacheDir: File): List<PdfImageRegion> {
        val regions = mutableListOf<PdfImageRegion>()
        val latch = java.util.concurrent.CountDownLatch(1)
        val image = InputImage.fromBitmap(bitmap, 0)
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .build()
        val detector = ObjectDetection.getClient(options)
        // Si Gemini está activo usar sus coordenadas más precisas
        if (GptClient.isEnabled) {
            val geminiRegions = GptClient.detectImageCoordinates(bitmap)
            if (geminiRegions.isNotEmpty()) {
                for ((idx, coords) in geminiRegions.withIndex()) {
                    val x = coords[0].toInt().coerceAtLeast(0)
                    val y = coords[1].toInt().coerceAtLeast(0)
                    val w = coords[2].toInt().coerceAtMost(bitmap.width - x)
                    val h = coords[3].toInt().coerceAtMost(bitmap.height - y)
                    if (w > 50 && h > 50) {
                        val cropped = Bitmap.createBitmap(bitmap, x, y, w, h)
                        val imageFile = File(cacheDir, "pdf_img_p${pageNumber}_${idx}.jpg")
                        FileOutputStream(imageFile).use { out -> cropped.compress(Bitmap.CompressFormat.JPEG, 92, out) }
                        cropped.recycle()
                        regions.add(PdfImageRegion(pageNumber, imageFile, x, y, w, h))
                    }
                }
                latch.countDown()
                return regions
            }
        }
        // Si Gemini está activo usar sus coordenadas más precisas
        if (GptClient.isEnabled) {
            val geminiRegions = GptClient.detectImageCoordinates(bitmap)
            if (geminiRegions.isNotEmpty()) {
                for ((idx, coords) in geminiRegions.withIndex()) {
                    val x = coords[0].toInt().coerceAtLeast(0)
                    val y = coords[1].toInt().coerceAtLeast(0)
                    val w = coords[2].toInt().coerceAtMost(bitmap.width - x)
                    val h = coords[3].toInt().coerceAtMost(bitmap.height - y)
                    if (w > 50 && h > 50) {
                        val cropped = Bitmap.createBitmap(bitmap, x, y, w, h)
                        val imageFile = File(cacheDir, "pdf_img_p${pageNumber}_${idx}.jpg")
                        FileOutputStream(imageFile).use { out -> cropped.compress(Bitmap.CompressFormat.JPEG, 92, out) }
                        cropped.recycle()
                        regions.add(PdfImageRegion(pageNumber, imageFile, x, y, w, h))
                    }
                }
                latch.countDown()
                return regions
            }
        }
        detector.process(image)
            .addOnSuccessListener { objects ->
                for (obj in objects) {
                    val box = obj.boundingBox
                    val x = box.left.coerceAtLeast(0)
                    val y = box.top.coerceAtLeast(0)
                    val w = box.width().coerceAtMost(bitmap.width - x)
                    val h = box.height().coerceAtMost(bitmap.height - y)
                    if (w > 50 && h > 50) {
                        val cropped = Bitmap.createBitmap(bitmap, x, y, w, h)
                        val imageFile = File(cacheDir, "pdf_img_p${pageNumber}_${regions.size}.jpg")
                        FileOutputStream(imageFile).use { out ->
                            cropped.compress(Bitmap.CompressFormat.JPEG, 92, out)
                        }
                        cropped.recycle()
                        regions.add(PdfImageRegion(pageNumber, imageFile, x, y, w, h))
                    }
                }
                latch.countDown()
            }
            .addOnFailureListener { latch.countDown() }
        latch.await(10, java.util.concurrent.TimeUnit.SECONDS)
        return regions
    }


    private fun isImageLikeBlock(pixels: List<Int>): Boolean {
        if (pixels.isEmpty()) return false
        var colorVariation = 0; var nonBW = 0; var colorful = 0
        for (pixel in pixels) {
            val r = Color.red(pixel); val g = Color.green(pixel); val b = Color.blue(pixel)
            val isWhite = r > 235 && g > 235 && b > 235
            val isBlack = r < 25 && g < 25 && b < 25
            val isGray = Math.abs(r-g) < 15 && Math.abs(g-b) < 15
            if (!isWhite && !isBlack) nonBW++
            if (!isWhite && !isBlack && !isGray) colorful++
            colorVariation += maxOf(r,g,b) - minOf(r,g,b)
        }
        val avgVar = colorVariation / pixels.size
        val nonBWRatio = nonBW.toFloat() / pixels.size
        val colorRatio = colorful.toFloat() / pixels.size
        return (avgVar > 20 && nonBWRatio > 0.25f) || colorRatio > 0.15f || (nonBWRatio > 0.55f && avgVar > 12)
    }

    suspend fun processPdf(context: Context, pdfUri: Uri, onProgress: (String) -> Unit = {}): PdfProcessResult {
        val allImages = mutableListOf<ExtractedImage>()
        val pageResults = mutableListOf<PdfPageResult>()
        onProgress("Copiando PDF...")
        val pdfFile = copyPdfToCache(context, pdfUri) ?: return PdfProcessResult(0, emptyList(), emptyList(), "", "", "", "")
        onProgress("Renderizando páginas...")
        val pages = renderPages(pdfFile)
        for (page in pages) {
            onProgress("Procesando página ${page.pageNumber}/${pages.size}...")
            val pageFile = File(context.cacheDir, "page_${page.pageNumber}.jpg")
            FileOutputStream(pageFile).use { out -> page.bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out) }
            onProgress("Extrayendo texto p.${page.pageNumber}...")
            // Extraer contenido con Gemini o APIs
            val cleanText = if (GptClient.isEnabled) {
                onProgress("Gemini analizando p.${page.pageNumber}...")
                val result = GptClient.extractContent(page.bitmap)
                if (result.isNotBlank()) result else TextRecognizer.combineResults(
                    TextRecognizer.recognizeText(pageFile).getOrDefault(""),
                    ApiClient.recognize(pageFile).getOrDefault("")
                )
            } else {
                val mlKitText = TextRecognizer.recognizeText(pageFile).getOrDefault("")
                val latexText = ApiClient.recognize(pageFile).getOrDefault("")
                TextRecognizer.combineResults(mlKitText, latexText)
            }
            onProgress("Detectando imágenes p.${page.pageNumber}...")
            val regions = detectImageRegions(page.bitmap, page.pageNumber, context.cacheDir)
            val pageImages = mutableListOf<ExtractedImage>()
            for (region in regions) {
                onProgress("Subiendo imagen p.${page.pageNumber}...")
                val uploadResult = if (GitHubUploader.token.isNotEmpty()) GitHubUploader.uploadImage(region.imageFile) else Result.failure(Exception("Sin token"))
                val imgTag = "![](" + (if (uploadResult.isSuccess) uploadResult.getOrThrow() else "") + ")"
                val extracted = ExtractedImage(page.pageNumber, region.imageFile, uploadResult.getOrDefault(""), imgTag)
                pageImages.add(extracted); allImages.add(extracted)
            }
            pageResults.add(PdfPageResult(page.pageNumber, cleanText, pageImages))
            pageFile.delete(); page.bitmap.recycle()
        }
        pdfFile.delete()
        return PdfProcessResult(pages.size, pageResults, allImages, buildMarkdown(pageResults), buildLatex(pageResults), buildHtml(pageResults), buildPlainText(pageResults))
    }

    private fun buildMarkdown(pages: List<PdfPageResult>): String {
        val sb = StringBuilder()
        for (page in pages) {
            if (pages.size > 1) sb.appendLine("## Página ${page.pageNumber}\n")
            if (page.text.isNotBlank()) sb.appendLine(page.text)
            page.images.forEach { sb.appendLine("\n${it.markdownTag}\n") }
        }
        return sb.toString().trim()
    }

    private fun buildLatex(pages: List<PdfPageResult>): String {
        val sb = StringBuilder()
        sb.appendLine("\\documentclass{article}\\begin{document}")
        for (page in pages) {
            if (page.text.isNotBlank()) sb.appendLine(page.text)
            page.images.forEach { if (it.githubLink.isNotEmpty()) sb.appendLine("% img: ${it.githubLink}") }
            if (pages.size > 1) sb.appendLine("\\newpage")
        }
        sb.appendLine("\\end{document}")
        return sb.toString().trim()
    }

    private fun buildHtml(pages: List<PdfPageResult>): String {
        val sb = StringBuilder()
        sb.appendLine("<html><body>")
        for (page in pages) {
            page.text.lines().forEach { sb.appendLine("<p>$it</p>") }
            page.images.forEach { if (it.githubLink.isNotEmpty()) sb.appendLine("<img src=\"${it.githubLink}\" style=\"max-width:100%\"/>") }
        }
        sb.appendLine("</body></html>")
        return sb.toString().trim()
    }

    private fun buildPlainText(pages: List<PdfPageResult>) = pages.joinToString("\n\n") { it.text }.trim()
}
