package com.example.myapplication.data.repository

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.myapplication.data.AppState
import com.example.myapplication.data.HistoryItem
import com.example.myapplication.util.PermissionHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class StorageRepository(private val context: Context) {
    
    private val gson = Gson()
    
    // Ruta igual al código Python: /storage/emulated/0/Download/Scripts/🟢 Aplicaciones/
    private val baseDir = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "Scripts/🟢 Aplicaciones"
    )
    
    private val savePath = File(baseDir, "resultados_problemas.csv")
    private val stateFile = File(baseDir, "state.json")
    private val historyFile = File(baseDir, "instructions_history.json")
    private val tempImgDir = File(baseDir, "temp_imgs")
    
    init {
        // Asegurar que el directorio base existe
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }
        if (!tempImgDir.exists()) {
            tempImgDir.mkdirs()
        }
    }
    
    fun getTempImgDir(): File = tempImgDir
    
    fun saveState(state: AppState) {
        try {
            if (!PermissionHelper.hasStoragePermission(context)) {
                Log.w("StorageRepository", "No hay permiso de almacenamiento para guardar estado")
                return
            }
            baseDir.mkdirs()
            stateFile.writeText(gson.toJson(state), Charsets.UTF_8)
            Log.d("StorageRepository", "Estado guardado en: ${stateFile.absolutePath}")
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error guardando estado: ${e.message}", e)
            e.printStackTrace()
        }
    }
    
    fun loadState(): AppState {
        return try {
            if (stateFile.exists()) {
                val json = stateFile.readText(Charsets.UTF_8)
                gson.fromJson(json, AppState::class.java) ?: AppState()
            } else {
                AppState()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            AppState()
        }
    }
    
    fun loadInstructionsHistory(): List<HistoryItem> {
        return try {
            if (historyFile.exists()) {
                val json = historyFile.readText(Charsets.UTF_8)
                val type = object : TypeToken<List<HistoryItem>>() {}.type
                gson.fromJson<List<HistoryItem>>(json, type) ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    fun saveInstructionsHistory(history: List<HistoryItem>) {
        try {
            if (!PermissionHelper.hasStoragePermission(context)) {
                Log.w("StorageRepository", "No hay permiso de almacenamiento para guardar historial")
                return
            }
            baseDir.mkdirs()
            historyFile.writeText(gson.toJson(history), Charsets.UTF_8)
            Log.d("StorageRepository", "Historial guardado en: ${historyFile.absolutePath}")
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error guardando historial: ${e.message}", e)
            e.printStackTrace()
        }
    }
    
    fun getCsvPath(): File = savePath
    
    fun canWriteToStorage(): Boolean {
        return try {
            PermissionHelper.hasStoragePermission(context) && baseDir.canWrite()
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error verificando permisos: ${e.message}", e)
            false
        }
    }
}



