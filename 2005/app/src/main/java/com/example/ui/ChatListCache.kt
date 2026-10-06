package com.example.ui
import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import android.util.Log

object ChatListCache {
    private val json = Json { ignoreUnknownKeys = true }

    fun saveChats(context: Context, chats: List<ChatModel>) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        try {
            val jsonStr = json.encodeToString<List<ChatModel>>(chats)
            prefs.edit().putString("cached_chat_models", jsonStr).apply()
        } catch(e: Exception) {
        }
    }

    fun loadChats(context: Context): List<ChatModel>? {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return try {
            val jsonStr = prefs.getString("cached_chat_models", null)
            if (jsonStr != null) {
                val chats = json.decodeFromString<List<ChatModel>>(jsonStr)
                chats
            } else {
                null
            }
        } catch(e: Exception) { 
            null 
        }
    }
}
