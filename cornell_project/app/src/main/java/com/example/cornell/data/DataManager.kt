package com.example.cornell.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import java.io.File

data class BackupData(
    @SerializedName("notes") val notes: List<Note>,
    @SerializedName("folders") val folders: List<String>,
    @SerializedName("version") val version: Int = 1
)

object DataManager {
    private const val NOTES_FILE = "notas_data.json"
    private const val FOLDERS_FILE = "carpetas_data.json"

    private var appContext: Context? = null
    private val gson = Gson()

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun notesFile(): File? = appContext?.filesDir?.let { File(it, NOTES_FILE) }
    private fun foldersFile(): File? = appContext?.filesDir?.let { File(it, FOLDERS_FILE) }

    fun createBackupJson(): String {
        val notes = loadNotes()
        val folders = loadFolders()
        return gson.toJson(BackupData(notes = notes, folders = folders))
    }

    fun importFromBackupJson(json: String): Pair<List<Note>, List<String>>? {
        return try {
            val type = object : TypeToken<BackupData>() {}.type
            val backup = gson.fromJson<BackupData>(json, type) ?: return null
            Pair(backup.notes, backup.folders)
        } catch (_: Throwable) {
            null
        }
    }

    /** Reemplaza todo con los datos importados (uso interno). */
    fun applyBackup(notes: List<Note>, folders: List<String>) {
        saveNotes(notes)
        saveFolders(folders)
    }

    /** Fusiona los datos importados con los existentes. No reemplaza, añade. */
    fun mergeBackup(importedNotes: List<Note>, importedFolders: List<String>) {
        val existingNotes = loadNotes()
        val existingFolders = loadFolders()

        val newFolders = existingFolders + importedFolders.filter { it !in existingFolders }

        val mergedNotes = existingNotes + importedNotes.mapIndexed { index, note ->
            val newId = "import_${System.currentTimeMillis()}_${index}_${note.id}"
            note.copy(
                id = newId,
                quizData = note.quizData?.copy(noteId = newId),
                flashcardsData = note.flashcardsData?.copy(noteId = newId)
            )
        }

        saveNotes(mergedNotes)
        saveFolders(newFolders)
    }

    fun loadNotes(): List<Note> {
        return try {
            val file = notesFile() ?: return emptyList()
            if (!file.exists()) return emptyList()
            val jsonText = file.readText(Charsets.UTF_8)
            if (jsonText.isBlank()) return emptyList()
            
            // Intentar deserialización directa primero
            try {
                val type = object : TypeToken<List<Note>>() {}.type
                val notes = gson.fromJson<List<Note>>(jsonText, type) ?: return emptyList()
                // Asegurar que todas las notas tengan valores válidos para campos nuevos
                val migrated = notes.mapNotNull { note ->
                    try {
                        note.copy(
                            explanationText = note.explanationText,
                            explanationScore = if (note.explanationScore < -1 || note.explanationScore > 100) -1 else note.explanationScore,
                            explanationLabel = note.explanationLabel,
                            explanationFeedbackFound = note.explanationFeedbackFound,
                            explanationFeedbackMissing = note.explanationFeedbackMissing,
                            explanationFeedbackTip = note.explanationFeedbackTip
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Si hay error, usar valores por defecto
                        note.copy(
                            explanationText = "",
                            explanationScore = -1,
                            explanationLabel = "",
                            explanationFeedbackFound = "",
                            explanationFeedbackMissing = "",
                            explanationFeedbackTip = ""
                        )
                    }
                }
                // Guardar las notas migradas para persistir los valores por defecto
                if (migrated.isNotEmpty()) {
                    saveNotes(migrated)
                }
                return migrated
            } catch (e: Exception) {
                // Si falla, la deserialización directa no funciona (notas muy antiguas)
                e.printStackTrace()
                emptyList()
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun saveNotes(notes: List<Note>) {
        notesFile()?.writeText(gson.toJson(notes), Charsets.UTF_8)
    }

    fun loadFolders(): List<String> {
        return try {
            val file = foldersFile() ?: return emptyList()
            if (!file.exists()) return emptyList()
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson<List<String>>(file.readText(Charsets.UTF_8), type) ?: emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun saveFolders(folders: List<String>) {
        foldersFile()?.writeText(gson.toJson(folders), Charsets.UTF_8)
    }
}
