package com.example.cornell

import android.app.Application
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat

/**
 * Inicializa EmojiCompat para soporte completo de emojis y caracteres Unicode
 * (estilo Notion) en toda la app.
 */
class CornellApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val config = BundledEmojiCompatConfig(this)
        EmojiCompat.init(config)
    }
}
