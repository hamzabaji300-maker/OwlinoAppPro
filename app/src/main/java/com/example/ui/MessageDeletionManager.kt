package com.example.ui

import android.content.Context
import android.util.Log
import com.example.supabase
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
data class HiddenMessageRow(val user_id: String, val message_id: String)

/**
 * Single source of truth for deleting messages:
 *  - delete for me      -> message id is stored in hidden_messages (server) + local cache, hidden everywhere
 *  - delete for everyone -> own messages are removed from the messages table
 * "Hidden" ids survive restarts (SharedPreferences) and sync across devices (Supabase).
 */
object MessageDeletionManager {
    private const val PREFS = "hidden_messages"
    private const val KEY_IDS = "ids"
    private const val KEY_PENDING = "pending"

    private val _hidden = MutableStateFlow<Set<String>>(emptySet())
    val hidden: StateFlow<Set<String>> = _hidden

    private var appCtx: Context? = null

    fun loadCache(context: Context) {
        appCtx = context.applicationContext
        val p = appCtx!!.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _hidden.value = (p.getStringSet(KEY_IDS, emptySet()) ?: emptySet()).toSet()
    }

    private fun save(pending: Set<String>? = null) {
        val c = appCtx ?: return
        val e = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        e.putStringSet(KEY_IDS, _hidden.value)
        if (pending != null) e.putStringSet(KEY_PENDING, pending)
        e.apply()
    }

    private fun pendingIds(): Set<String> =
        (appCtx?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.getStringSet(KEY_PENDING, emptySet()) ?: emptySet()).toSet()

    fun clear() {
        _hidden.value = emptySet()
        appCtx?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()?.clear()?.apply()
    }

    /** Delete for me: hide immediately, then sync. Network failures are retried by refresh(). */
    suspend fun deleteForMe(myId: String, messageIds: Collection<String>): Boolean {
        if (messageIds.isEmpty()) return true
        _hidden.value = _hidden.value + messageIds
        save(pendingIds() + messageIds)
        try {
            val rows = messageIds.map { HiddenMessageRow(user_id = myId, message_id = it) }
            supabase.postgrest["hidden_messages"].upsert(rows)
            save(pendingIds() - messageIds.toSet())
        } catch (e: Exception) {
            Log.e("MessageDeletion", "hidden_messages sync failed, will retry", e)
        }
        return true
    }

    /** Delete for everyone: only the user's own messages. Rolls back if the server refuses. */
    suspend fun deleteForEveryone(myId: String, messageIds: Collection<String>): Boolean {
        if (messageIds.isEmpty()) return true
        val before = _hidden.value
        _hidden.value = _hidden.value + messageIds
        return try {
            supabase.postgrest["messages"].delete {
                filter {
                    isIn("id", messageIds.toList())
                    eq("sender_id", myId)
                }
            }
            // The rows no longer exist on the server; keep them hidden locally so stale caches never show them.
            save()
            true
        } catch (e: Exception) {
            Log.e("MessageDeletion", "delete for everyone failed", e)
            _hidden.value = before
            save()
            false
        }
    }

    /** Push pending local hides, then merge the server list (never treat a failed fetch as empty). */
    suspend fun refresh(myId: String) {
        try {
            val pending = pendingIds()
            if (pending.isNotEmpty()) {
                supabase.postgrest["hidden_messages"].upsert(pending.map { HiddenMessageRow(myId, it) })
                save(emptySet())
            }
            val server = supabase.postgrest["hidden_messages"]
                .select { filter { eq("user_id", myId) } }
                .decodeList<HiddenMessageRow>()
                .map { it.message_id }
                .toSet()
            _hidden.value = server + _hidden.value
            save()
        } catch (e: Exception) {
            Log.e("MessageDeletion", "refresh failed", e)
        }
    }
}
