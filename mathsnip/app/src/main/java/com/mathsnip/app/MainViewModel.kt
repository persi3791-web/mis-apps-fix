package com.mathsnip.app

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mathsnip.app.data.Snip
import com.mathsnip.app.data.SnipRepository
import com.mathsnip.app.network.ApiClient
import com.mathsnip.app.network.GptClient
import com.mathsnip.app.network.GitHubUploader
import com.mathsnip.app.network.TextRecognizer
import com.mathsnip.app.pdf.PdfProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

enum class ScanState { IDLE, SCANNING, SUCCESS, ERROR }
enum class ApiStatus { CHECKING, CONNECTED, ERROR }

data class UiState(
    val scanState: ScanState = ScanState.IDLE,
    val scanMessage: String = "",
    val scanProgress: String = "",
    val apiStatus: ApiStatus = ApiStatus.CHECKING,
    val pdfPageCount: Int = 0,
    val pdfImageCount: Int = 0,
    val lastMarkdown: String = "",
    val githubTokenValid: Boolean = false,
    val gptEnabled: Boolean = false,
    val gptTokenValid: Boolean = false,
    val inlineDelim: String = "\\( ... \\)",
    val blockDelimUn: String = "\\[ ... \\]",
    val blockDelimN: String = "\\begin{equation}"
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = SnipRepository(app)
    val snips: StateFlow<List<Snip>> = repo.snips.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    init {
        checkApi()
        loadGitHubToken()
        loadDelimiters()
        loadGptState()
    }

    private fun loadGptState() {
        viewModelScope.launch(Dispatchers.IO) {
            val token = try { getApplication<Application>().assets.open("gpt_token.txt").bufferedReader().readText().trim() } catch(e: Exception) { repo.getGptToken() }
            val enabled = repo.getGptEnabled()
            val orKey = try {
                File("/storage/emulated/0/Download/Aplicaciones \uD83D\uDD34\uD83D\uDD34/APIS/APIS OpenRouter.txt")
                    .readText().trim()
            } catch (e: Exception) { "" }
            if (token.isNotEmpty()) {
                GptClient.loadKeys(token, orKey)
            }
            GptClient.isEnabled = enabled
            _ui.update { it.copy(gptEnabled = enabled, gptTokenValid = token.isNotEmpty()) }
        }
    }

    fun setGptToken(token: String) {
        GptClient.loadKeys(token, "")
        viewModelScope.launch(Dispatchers.IO) {
            repo.saveGptToken(token)
            _ui.update { it.copy(gptTokenValid = token.isNotEmpty()) }
        }
    }

    fun toggleGpt(enabled: Boolean) {
        GptClient.isEnabled = enabled
        _ui.update { it.copy(gptEnabled = enabled) }
        viewModelScope.launch(Dispatchers.IO) { repo.saveGptEnabled(enabled) }
    }

    fun checkApi() {
        _ui.update { it.copy(apiStatus = ApiStatus.CHECKING) }
        viewModelScope.launch(Dispatchers.IO) {
            val ok = ApiClient.checkStatus()
            _ui.update { it.copy(apiStatus = if (ok) ApiStatus.CONNECTED else ApiStatus.ERROR) }
        }
    }

    fun loadGitHubToken() {
        viewModelScope.launch(Dispatchers.IO) {
            val token = try { getApplication<Application>().assets.open("github_token.txt").bufferedReader().readText().trim() } catch(e: Exception) { repo.getGithubToken() }
            if (token.isNotEmpty()) {
                GitHubUploader.token = token
                _ui.update { it.copy(githubTokenValid = true) }
            }
        }
    }

    fun setGitHubToken(token: String) {
        GitHubUploader.token = token
        viewModelScope.launch(Dispatchers.IO) {
            repo.saveGithubToken(token)
            val valid = GitHubUploader.verifyToken()
            _ui.update { it.copy(githubTokenValid = valid) }
        }
    }

    fun loadDelimiters() {
        viewModelScope.launch(Dispatchers.IO) {
            val (inline, blockUn, blockN) = repo.getDelimiters()
            _ui.update { it.copy(inlineDelim = inline, blockDelimUn = blockUn, blockDelimN = blockN) }
            TextRecognizer.inlineDelim = inline
            TextRecognizer.blockDelimUn = blockUn
            TextRecognizer.blockDelimN = blockN
            GptClient.inlineDelim = inline
            GptClient.blockDelimUn = blockUn
            GptClient.blockDelimN = blockN
        }
    }

    fun saveDelimiters(inline: String, blockUn: String, blockN: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.saveDelimiters(inline, blockUn, blockN)
            _ui.update { it.copy(inlineDelim = inline, blockDelimUn = blockUn, blockDelimN = blockN) }
            TextRecognizer.inlineDelim = inline
            TextRecognizer.blockDelimUn = blockUn
            TextRecognizer.blockDelimN = blockN
            GptClient.inlineDelim = inline
            GptClient.blockDelimUn = blockUn
            GptClient.blockDelimN = blockN
        }
    }

    fun scanImage(file: File) {
        _ui.update { it.copy(scanState = ScanState.SCANNING, scanMessage = "", scanProgress = "Analizando...") }
        viewModelScope.launch(Dispatchers.IO) {
            val finalResult = if (GptClient.isEnabled) {
                _ui.update { it.copy(scanProgress = "GPT-4o analizando...") }
                val bmp = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                if (bmp != null) GptClient.extractContent(bmp) else ""
            } else {
                _ui.update { it.copy(scanProgress = "Reconociendo...") }
                val mlKit = TextRecognizer.recognizeText(file).getOrDefault("")
                val latex = ApiClient.recognize(file).getOrDefault("")
                TextRecognizer.combineResults(mlKit, latex)
            }
            if (finalResult.isNotBlank()) {
                repo.addSnip(Snip(latex = finalResult, imagePath = file.absolutePath))
                _ui.update { it.copy(scanState = ScanState.SUCCESS, scanMessage = finalResult, scanProgress = "") }
            } else {
                _ui.update { it.copy(scanState = ScanState.ERROR, scanMessage = "No se pudo reconocer", scanProgress = "") }
            }
        }
    }

    fun processPdf(uri: Uri) {
        _ui.update { it.copy(scanState = ScanState.SCANNING, scanMessage = "", scanProgress = "Iniciando...") }
        viewModelScope.launch(Dispatchers.IO) {
            val ctx = getApplication<Application>()
            val result = PdfProcessor.processPdf(ctx, uri) { p -> _ui.update { it.copy(scanProgress = p) } }
            if (result.pageCount > 0) {
                repo.addSnip(Snip(latex = result.markdownContent, imagePath = "", type = "pdf"))
                _ui.update { it.copy(
                    scanState = ScanState.SUCCESS,
                    scanMessage = "PDF procesado",
                    scanProgress = "",
                    pdfPageCount = result.pageCount,
                    pdfImageCount = result.extractedImages.size,
                    lastMarkdown = result.markdownContent
                ) }
            } else {
                _ui.update { it.copy(scanState = ScanState.ERROR, scanMessage = "No se pudo procesar", scanProgress = "") }
            }
        }
    }

    fun uploadImageToGitHub(file: File) {
        if (GitHubUploader.token.isEmpty()) {
            _ui.update { it.copy(scanState = ScanState.ERROR, scanMessage = "Configura GitHub Token") }
            return
        }
        _ui.update { it.copy(scanState = ScanState.SCANNING, scanProgress = "Subiendo...") }
        viewModelScope.launch(Dispatchers.IO) {
            val result = GitHubUploader.uploadImage(file)
            if (result.isSuccess) {
                val link = result.getOrThrow()
                val md = "![](" + link + ")"
                repo.addSnip(Snip(latex = md, imagePath = file.absolutePath, type = "image"))
                _ui.update { it.copy(scanState = ScanState.SUCCESS, scanMessage = md, scanProgress = "") }
            } else {
                _ui.update { it.copy(scanState = ScanState.ERROR, scanMessage = "Error subiendo", scanProgress = "") }
            }
        }
    }

    fun deleteSnip(id: String) { viewModelScope.launch { repo.deleteSnip(id) } }

    fun copyToClipboard(text: String) {
        val cm = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("MathSnip", text))
    }

    fun resetScan() { _ui.update { it.copy(scanState = ScanState.IDLE, scanMessage = "", scanProgress = "") } }
}
