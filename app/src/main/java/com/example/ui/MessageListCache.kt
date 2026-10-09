package com.example.ui
import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import android.util.Log

object MessageListCache {
    private val json = Json { ignoreUnknownKeys = true }

    fun saveMessages(context: Context, chatId: String, messages: List<MessageModel>) {
        val prefs = context.getSharedPreferences("app_messages_cache", Context.MODE_PRIVATE)
        try {
            val jsonStr = json.encodeToString<List<MessageModel>>(messages.take(50)) // Cache only last 50 to keep it fast
            prefs.edit().putString("chat_msgs_$chatId", jsonStr).apply()
        } catch(e: Exception) {
        }
    }

    /**
     * Préchauffage (thread d'arrière-plan, au démarrage) : charge le fichier de préférences et décode les
     * dernières conversations en mémoire. Ainsi l'ouverture d'une conversation ne lit plus le disque
     * ni ne décode du JSON sur le thread principal.
     */
    fun warmUp(context: Context, maxChats: Int = 40) {
        try {
            val prefs = context.getSharedPreferences("app_messages_cache", Context.MODE_PRIVATE)
            var n = 0
            for ((key, value) in prefs.all) {
                if (n >= maxChats) break
                if (!key.startsWith("chat_msgs_") || value !is String) continue
                val chatId = key.removePrefix("chat_msgs_")
                if (com.example.AppState.chatMessagesCache.containsKey(chatId)) continue
                try {
                    val msgs = json.decodeFromString<List<MessageModel>>(value)
                    com.example.AppState.chatMessagesCache.putIfAbsent(chatId, msgs)
                    n++
                } catch (e: Exception) {
                }
            }
        } catch (e: Throwable) {
        }
    }

    fun loadMessages(context: Context, chatId: String): List<MessageModel>? {
        val prefs = context.getSharedPreferences("app_messages_cache", Context.MODE_PRIVATE)
        return try {
            val jsonStr = prefs.getString("chat_msgs_$chatId", null)
            if (jsonStr != null) {
                val msgs = json.decodeFromString<List<MessageModel>>(jsonStr)
                msgs
            } else {
                null
            }
        } catch(e: Exception) { 
            null 
        }
    }
}
