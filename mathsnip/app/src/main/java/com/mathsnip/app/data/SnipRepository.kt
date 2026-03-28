package com.mathsnip.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

data class Snip(
    val id: String = System.currentTimeMillis().toString(),
    val latex: String,
    val imagePath: String = "",
    val type: String = "snip",
    val date: String = java.text.SimpleDateFormat(
        "dd/MM HH:mm", java.util.Locale.getDefault()
    ).format(java.util.Date())
)

val Context.dataStore by preferencesDataStore(name = "mathsnip_prefs")

class SnipRepository(private val context: Context) {
    private val gson = Gson()
    private val KEY_SNIPS = stringPreferencesKey("snips")
    private val KEY_GITHUB_TOKEN = stringPreferencesKey("github_token")
    private val KEY_GPT_TOKEN = stringPreferencesKey("gpt_token")
    private val KEY_GPT_ENABLED = stringPreferencesKey("gpt_enabled")
    private val KEY_GEMINI = stringPreferencesKey("gemini_enabled")
    private val KEY_INLINE_DELIM = stringPreferencesKey("inline_delim")
    private val KEY_BLOCK_DELIM_UN = stringPreferencesKey("block_delim_un")
    private val KEY_BLOCK_DELIM_N = stringPreferencesKey("block_delim_n")

    val snips: Flow<List<Snip>> = context.dataStore.data.map { prefs ->
        val json = prefs[KEY_SNIPS] ?: "[]"
        try {
            val type = object : TypeToken<List<Snip>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    suspend fun saveGithubToken(token: String) {
        context.dataStore.edit { it[KEY_GITHUB_TOKEN] = token }
    }
    suspend fun getGithubToken(): String =
        context.dataStore.data.first()[KEY_GITHUB_TOKEN] ?: ""

    suspend fun saveGptToken(token: String) {
        context.dataStore.edit { it[KEY_GPT_TOKEN] = token }
    }
    suspend fun getGptToken(): String =
        context.dataStore.data.first()[KEY_GPT_TOKEN] ?: ""

    suspend fun saveGptEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_GPT_ENABLED] = enabled.toString() }
    }
    suspend fun getGptEnabled(): Boolean =
        context.dataStore.data.first()[KEY_GPT_ENABLED]?.toBoolean() ?: false

    suspend fun saveGeminiEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_GEMINI] = enabled.toString() }
    }
    suspend fun getGeminiEnabled(): Boolean =
        context.dataStore.data.first()[KEY_GEMINI]?.toBoolean() ?: false

    suspend fun saveDelimiters(inline: String, blockUn: String, blockN: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_INLINE_DELIM] = inline
            prefs[KEY_BLOCK_DELIM_UN] = blockUn
            prefs[KEY_BLOCK_DELIM_N] = blockN
        }
    }
    suspend fun getDelimiters(): Triple<String, String, String> {
        val prefs = context.dataStore.data.first()
        return Triple(
            prefs[KEY_INLINE_DELIM] ?: "\\( ... \\)",
            prefs[KEY_BLOCK_DELIM_UN] ?: "\\[ ... \\]",
            prefs[KEY_BLOCK_DELIM_N] ?: "\\begin{equation}"
        )
    }

    suspend fun addSnip(snip: Snip) {
        context.dataStore.edit { prefs ->
            val current = try {
                val json = prefs[KEY_SNIPS] ?: "[]"
                val type = object : TypeToken<List<Snip>>() {}.type
                gson.fromJson<List<Snip>>(json, type) ?: emptyList()
            } catch (e: Exception) { emptyList() }
            prefs[KEY_SNIPS] = gson.toJson(listOf(snip) + current.take(499))
        }
    }

    suspend fun deleteSnip(id: String) {
        context.dataStore.edit { prefs ->
            val current = try {
                val json = prefs[KEY_SNIPS] ?: "[]"
                val type = object : TypeToken<List<Snip>>() {}.type
                gson.fromJson<List<Snip>>(json, type) ?: emptyList()
            } catch (e: Exception) { emptyList() }
            prefs[KEY_SNIPS] = gson.toJson(current.filter { it.id != id })
        }
    }
}
