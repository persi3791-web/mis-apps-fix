package com.example.imagenes

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import kotlin.random.Random

data class FlashcardRow(var q: String, var a: String, var imgUrl: String = "PENDIENTE")

class MainActivity : AppCompatActivity() {

    private val API_KEYS_GEMINI = listOf(
        "AIzaSyBH-4FcDt9vpEJlDwxMeCW1QigrtF3Zt2k",
        "AIzaSyDnl11aCnK7qOE-K9viBBJim0QD_UrF5uM"
    )
    private val GOOGLE_API_KEY = "AIzaSyBuzM-cCtTJ9cplHi7OTbEaSBhrAGx7dBA"
    private val GOOGLE_CX = "96728b66d8b3841df"
    private val CLOUD_NAME = "dy6gi3ft4"
    private val CLOUDINARY_UPLOAD_PRESET = "ml_default"
    private val CLOUDINARY_URL = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload"

    private val currentData = mutableListOf<FlashcardRow>()
    private var colQState = 0
    private var colAState = 0
    private var useGPT4 = false
    private val prefs by lazy { getSharedPreferences("imagenes_prefs", MODE_PRIVATE) }
    private var exportFolderUri: String? = null

    private lateinit var adapter: RowsAdapter
    private lateinit var tvLog: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnGenImages: Button
    private lateinit var headerQ: TextView
    private lateinit var headerA: TextView
    private lateinit var etInput: EditText
    private lateinit var etCount: EditText

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvLog = findViewById(R.id.tvLog)
        progressBar = findViewById(R.id.progressBar)
        btnGenImages = findViewById(R.id.btnGenImages)
        headerQ = findViewById(R.id.headerQ)
        headerA = findViewById(R.id.headerA)
        etInput = findViewById(R.id.etInputText)
        etCount = findViewById(R.id.etCount)

        val recycler = findViewById<RecyclerView>(R.id.recyclerView)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = RowsAdapter(currentData)
        recycler.adapter = adapter

        checkPermissions()

        findViewById<Button>(R.id.btnAiCreate).setOnClickListener {
            val topic = etInput.text.toString()
            val count = etCount.text.toString().toIntOrNull() ?: 10
            if (topic.isNotEmpty()) startAiCreation(topic, count) else log("Error: Escribe un tema.")
        }
        findViewById<Button>(R.id.btnUpload).setOnClickListener {
            val txt = etInput.text.toString()
            if (txt.isNotEmpty()) parseManualUpload(txt) else log("Error: Input vacio.")
        }
        findViewById<Button>(R.id.btnClear).setOnClickListener {
            currentData.clear(); adapter.notifyDataSetChanged()
            colQState = 0; colAState = 0; updateHeaderColors()
            log("Memoria borrada."); progressBar.progress = 0
        }
        btnGenImages.setOnClickListener { startImageGenerationProcess() }
        findViewById<Button>(R.id.btnExport).setOnClickListener { exportToCsv() }
        findViewById<Button>(R.id.btnModelToggle).setOnClickListener {
            useGPT4 = !useGPT4; updateModelButton()
        }
        updateModelButton()

        exportFolderUri = prefs.getString("export_folder_uri", null)

        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            showSettingsDialog()
        }

        headerQ.setOnClickListener { colQState = (colQState + 1) % 3; updateHeaderColors() }
        headerA.setOnClickListener { colAState = (colAState + 1) % 3; updateHeaderColors() }
    }

    private fun updateModelButton() {
        val btn = findViewById<Button>(R.id.btnModelToggle)
        if (useGPT4) { btn.text = "Modelo: GPT-4o"; btn.setBackgroundColor(Color.parseColor("#5A2D82")) }
        else { btn.text = "Modelo: Gemini 2.0"; btn.setBackgroundColor(Color.parseColor("#1A6B3A")) }
    }

    private fun readAssetToken(f: String): String =
        try { assets.open(f).bufferedReader().readText().trim() } catch (e: Exception) { "" }

    private fun callAI(prompt: String): String = if (useGPT4) callGPT4(prompt) else callGemini(prompt)

    private fun callGPT4(prompt: String): String {
        val token = readAssetToken("github_token.txt")
        if (token.isEmpty()) return ""
        try {
            val bodyObj = org.json.JSONObject()
            bodyObj.put("model", "gpt-4o")
            bodyObj.put("messages", org.json.JSONArray().put(org.json.JSONObject().put("role","user").put("content",prompt)))
            bodyObj.put("max_tokens", 1000)
            val res = client.newCall(Request.Builder()
                .url("https://models.inference.ai.azure.com/chat/completions")
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .post(bodyObj.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()).execute()
            if (res.isSuccessful)
                return JsonParser.parseString(res.body?.string()).asJsonObject
                    .getAsJsonArray("choices")[0].asJsonObject
                    .getAsJsonObject("message").get("content").asString.trim()
        } catch (e: Exception) { }
        return ""
    }

    private fun callGemini(prompt: String): String {
        for (key in API_KEYS_GEMINI) {
            try {
                val bodyObj = org.json.JSONObject()
                bodyObj.put("contents", org.json.JSONArray().put(
                    org.json.JSONObject().put("parts", org.json.JSONArray().put(
                        org.json.JSONObject().put("text", prompt)))))
                val res = client.newCall(Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$key")
                    .post(bodyObj.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                    .build()).execute()
                if (res.isSuccessful)
                    return JsonParser.parseString(res.body?.string()).asJsonObject
                        .getAsJsonArray("candidates")[0].asJsonObject
                        .getAsJsonObject("content").getAsJsonArray("parts")[0].asJsonObject
                        .get("text").asString.trim()
            } catch (e: Exception) { continue }
        }
        return ""
    }

    private fun updateHeaderColors() {
        val colors = listOf("#000000", "#165072", "#BF382B")
        headerQ.setBackgroundColor(Color.parseColor(colors[colQState]))
        headerA.setBackgroundColor(Color.parseColor(colors[colAState]))
        if (colQState > 0 || colAState > 0) {
            btnGenImages.setBackgroundColor(Color.parseColor("#2ECC71"))
            btnGenImages.text = "BUSCAR IMAGENES (${if (colQState == 2 || colAState == 2) "DIRECTO" else "IA"})!"
        } else {
            btnGenImages.setBackgroundColor(Color.parseColor("#7F8C8D"))
            btnGenImages.text = "2. GENERAR 3a COLUMNA"
        }
    }

    private fun log(msg: String) {
        runOnUiThread {
            tvLog.append("\n> $msg")
            tvLog.post { val s = tvLog.layout?.getLineTop(tvLog.lineCount) ?: 0; if (s > 0) tvLog.scrollTo(0, s - tvLog.height) }
        }
    }

    private fun startAiCreation(topic: String, qty: Int) {
        progressBar.progress = 10
        log("Solicitando a ${if (useGPT4) "GPT-4o" else "Gemini"} $qty cartas...")
        lifecycleScope.launch(Dispatchers.IO) {
            val prompt = "Actua como experto educativo. Genera EXACTAMENTE $qty flashcards del tema: $topic. Formato CSV punto y coma: Pregunta ; Respuesta. SOLO DOS COLUMNAS. Sin comillas, sin encabezados."
            val resultText = callAI(prompt)
            if (resultText.isNotEmpty()) {
                val newRows = mutableListOf<FlashcardRow>()
                resultText.lines().forEach { line ->
                    if (line.contains(";")) {
                        val parts = line.split(";")
                        if (parts.size >= 2) newRows.add(FlashcardRow(parts[0].trim(), parts[1].trim(), "PENDIENTE"))
                    }
                }
                withContext(Dispatchers.Main) {
                    currentData.clear(); currentData.addAll(newRows)
                    adapter.notifyDataSetChanged()
                    log("Exito! Creadas ${newRows.size} filas.")
                    progressBar.progress = 100
                }
            } else {
                withContext(Dispatchers.Main) { log("Error: ${if (useGPT4) "GPT-4o" else "Gemini"} no respondio.") }
            }
        }
    }

    private fun parseManualUpload(text: String) {
        val lines = text.lines().filter { it.isNotBlank() }
        val newRows = mutableListOf<FlashcardRow>()
        val hasHeader = lines.getOrElse(0) { "" }.lowercase().let { it.contains("pregunta") || it.contains("numero") }
        for (i in (if (hasHeader) 2 else 0) until lines.size) {
            val parts = lines[i].split("|")
            if (parts.size >= 2) newRows.add(FlashcardRow(parts[0].trim(), parts[1].trim(), if (parts.size > 2) parts[2].trim() else "PENDIENTE"))
        }
        currentData.clear(); currentData.addAll(newRows)
        adapter.notifyDataSetChanged()
        log("Tabla cargada (${newRows.size} filas).")
    }

    private fun startImageGenerationProcess() {
        if (currentData.isEmpty()) return
        if (colQState == 0 && colAState == 0) { log("Selecciona encabezados!"); return }
        progressBar.max = currentData.size; progressBar.progress = 0
        lifecycleScope.launch(Dispatchers.IO) {
            currentData.forEachIndexed { index, row ->
                var q = ""
                if (colQState == 2) q += "${row.q} "
                if (colAState == 2) q += "${row.a} "
                if (q.isEmpty() && (colQState == 1 || colAState == 1))
                    q = callAI("Contexto: P: ${row.q}, R: ${row.a}. Dame SOLO 3 palabras clave para imagen en Google.").trim()
                q = q.trim().ifEmpty { row.q }
                withContext(Dispatchers.Main) { log("[$index] Buscando: $q") }
                val imgUrl = searchGoogleImage(q)
                row.imgUrl = if (imgUrl.startsWith("http")) uploadToCloudinary(imgUrl) else "No Found"
                withContext(Dispatchers.Main) { adapter.notifyItemChanged(index); progressBar.progress = index + 1 }
            }
            withContext(Dispatchers.Main) { log("Proceso finalizado!") }
        }
    }

    private fun searchGoogleImage(query: String): String {
        return try {
            val url = "https://www.googleapis.com/customsearch/v1?key=$GOOGLE_API_KEY&cx=$GOOGLE_CX&q=$query&searchType=image&num=3"
            val response = client.newCall(Request.Builder().url(url).build()).execute()
            if (response.isSuccessful) {
                val items = JsonParser.parseString(response.body?.string()).asJsonObject.getAsJsonArray("items")
                if (items != null && items.size() > 0) items.get(Random.nextInt(minOf(items.size(), 3))).asJsonObject.get("link").asString else ""
            } else ""
        } catch (e: Exception) { "" }
    }

    private fun uploadToCloudinary(remoteUrl: String): String {
        return try {
            val formBody = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("file", remoteUrl).addFormDataPart("upload_preset", CLOUDINARY_UPLOAD_PRESET).build()
            val response = client.newCall(Request.Builder().url(CLOUDINARY_URL).post(formBody).build()).execute()
            if (response.isSuccessful) JsonParser.parseString(response.body?.string()).asJsonObject.get("secure_url").asString else remoteUrl
        } catch (e: Exception) { remoteUrl }
    }

    private fun getNextNumber(): Int {
        val n = prefs.getInt("csv_counter", 0) + 1
        prefs.edit().putInt("csv_counter", n).apply()
        return n
    }

    private fun exportToCsv() {
        if (currentData.isEmpty()) { log("Nada que exportar."); return }
        val filename = "Flashcards_${getNextNumber()}.csv"
        val sb = StringBuilder("Pregunta;Respuesta;Imagen\n")
        currentData.forEach { sb.append("${it.q};${it.a};${it.imgUrl}\n") }
        try {
            val folderUri = exportFolderUri
            if (folderUri != null) {
                val docUri = androidx.documentfile.provider.DocumentFile.fromTreeUri(this, android.net.Uri.parse(folderUri))
                val file = docUri?.createFile("text/csv", filename)
                if (file != null) {
                    contentResolver.openOutputStream(file.uri)?.use { it.write(sb.toString().toByteArray()) }
                    log("Guardado en carpeta: $filename")
                    return
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                })
                uri?.let { contentResolver.openOutputStream(it)?.use { s -> s.write(sb.toString().toByteArray()) } }
                log("Guardado en Downloads: $filename")
            } else {
                @Suppress("DEPRECATION")
                java.io.FileOutputStream(java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename))
                    .use { it.write(sb.toString().toByteArray()) }
                log("Guardado: $filename")
            }
        } catch (e: Exception) { log("Error: ${e.message}") }
    }

    private val folderPickerLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            exportFolderUri = it.toString()
            prefs.edit().putString("export_folder_uri", it.toString()).apply()
            log("Carpeta seleccionada correctamente.")
        }
    }

    private fun showSettingsDialog() {
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Ajustes")
            .setMessage("Carpeta actual: ${if (exportFolderUri != null) "Seleccionada" else "Downloads"} | Archivos: Flashcards_1.csv, Flashcards_2.csv...")
            .setPositiveButton("Seleccionar carpeta") { _, _ -> folderPickerLauncher.launch(null) }
            .setNegativeButton("Cerrar", null)
            .create()
        dialog.show()
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
    }

    inner class RowsAdapter(private val rows: List<FlashcardRow>) : RecyclerView.Adapter<RowsAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val etQ: EditText = v.findViewById(R.id.rowQ)
            val etA: EditText = v.findViewById(R.id.rowA)
            val tvImg: TextView = v.findViewById(R.id.rowImg)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_row, parent, false))
        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = rows[position]
            holder.etQ.setText(item.q); holder.etA.setText(item.a)
            holder.tvImg.text = if (item.imgUrl.length > 20) "URL OK" else item.imgUrl
            if (item.imgUrl.startsWith("http")) holder.tvImg.setTextColor(Color.GREEN)
            holder.etQ.setOnFocusChangeListener { _, _ -> item.q = holder.etQ.text.toString() }
            holder.etA.setOnFocusChangeListener { _, _ -> item.a = holder.etA.text.toString() }
        }
        override fun getItemCount() = rows.size
    }
}
