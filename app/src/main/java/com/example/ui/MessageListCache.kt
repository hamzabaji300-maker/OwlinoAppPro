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
