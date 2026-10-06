package com.example.ui

import android.content.Context
import android.util.Log
import com.example.supabase
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
data class SavedMessageRow(val user_id: String, val message_id: String)

/**
 * Starred / saved messages: shown as a yellow star in the bubble.
 * Persisted on the device and synced with Supabase (saved_messages), so they survive restarts and appear on all devices.
 */
object SavedMessagesManager {
    private const val PREFS = "saved_messages"
    private val _saved = MutableStateFlow<Set<String>>(emptySet())
    val saved: StateFlow<Set<String>> = _saved
    private var appCtx: Context? = null

    fun loadCache(context: Context) {
        appCtx = context.applicationContext
        val p = appCtx!!.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _saved.value = (p.getStringSet("ids", emptySet()) ?: emptySet()).toSet()
    }

    private fun save() {
        appCtx?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()?.putStringSet("ids", _saved.value)?.apply()
    }

    fun clear() {
        _saved.value = emptySet()
        appCtx?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()?.clear()?.apply()
    }

    /** Optimistic toggle. Keeps the local result on network errors (refresh() pushes it later). */
    suspend fun setSaved(myId: String, messageId: String, saved: Boolean): Boolean {
        _saved.value = if (saved) _saved.value + messageId else _saved.value - messageId
        save()
        return try {
            if (saved) {
                supabase.postgrest["saved_messages"].upsert(SavedMessageRow(myId, messageId))
            } else {
                supabase.postgrest["saved_messages"].delete {
                    filter {
                        eq("user_id", myId)
                        eq("message_id", messageId)
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e("SavedMessages", "sync failed", e)
            false
        }
    }

    suspend fun refresh(myId: String) {
        try {
            val server = supabase.postgrest["saved_messages"]
                .select { filter { eq("user_id", myId) } }
                .decodeList<SavedMessageRow>()
                .map { it.message_id }
                .toSet()
            _saved.value = server
            save()
        } catch (e: Exception) {
            Log.e("SavedMessages", "refresh failed", e)
        }
    }
}
