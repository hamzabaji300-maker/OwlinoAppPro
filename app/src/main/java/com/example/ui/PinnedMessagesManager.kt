package com.example.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import io.github.jan.supabase.postgrest.postgrest
import com.example.supabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.PostgresAction

@kotlinx.serialization.Serializable
data class PinnedMessageRow(
    val id: String = java.util.UUID.randomUUID().toString(),
    val chat_id: String,
    val message_id: String,
    val pinned_by: String,
    val pinned_at: String = "",
    val scope: String = "me"
)

object PinnedMessagesManager {
    private val _pinnedMessages = MutableStateFlow<Map<String, List<PinnedMessageRow>>>(emptyMap())
    val pinnedMessages: StateFlow<Map<String, List<PinnedMessageRow>>> = _pinnedMessages

    // message ids with a pin/unpin in flight: reloads must not overwrite their optimistic state
    private val pending = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    suspend fun load(chatId: String, myId: String) {
        try {
            val rows = supabase.postgrest["pinned_messages"]
                .select {
                    filter { eq("chat_id", chatId) }
                }
                .decodeList<PinnedMessageRow>()
            
            val local = _pinnedMessages.value[chatId] ?: emptyList()
            val merged = rows.filter { it.message_id !in pending } + local.filter { it.message_id in pending }
            if (merged != local) {
                _pinnedMessages.value = _pinnedMessages.value.toMutableMap().apply {
                    put(chatId, merged)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("PinnedMessages", "load failed", e)
        }
    }

    suspend fun pin(chatId: String, messageId: String, scope: String, myId: String): Boolean {
        // Optimistic local update
        val newRow = PinnedMessageRow(
            chat_id = chatId,
            message_id = messageId,
            pinned_by = myId,
            scope = scope,
            pinned_at = java.time.format.DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.now())
        )
        val currentRows = _pinnedMessages.value[chatId] ?: emptyList()
        pending.add(messageId)
        _pinnedMessages.value = _pinnedMessages.value.toMutableMap().apply {
            put(chatId, currentRows + newRow)
        }

        return try {
            supabase.postgrest["pinned_messages"].insert(newRow)
            pending.remove(messageId)
            true
        } catch (e: Exception) {
            pending.remove(messageId)
            // Rollback
            _pinnedMessages.value = _pinnedMessages.value.toMutableMap().apply {
                put(chatId, currentRows)
            }
            false
        }
    }

    suspend fun unpin(chatId: String, messageId: String, myId: String): Boolean {
        val currentRows = _pinnedMessages.value[chatId] ?: emptyList()
        val rowToRemove = currentRows.find { it.message_id == messageId && it.chat_id == chatId }
        
        // Optimistic
        pending.add(messageId)
        _pinnedMessages.value = _pinnedMessages.value.toMutableMap().apply {
            put(chatId, currentRows.filter { it.message_id != messageId })
        }

        return try {
            supabase.postgrest["pinned_messages"].delete {
                filter {
                    eq("chat_id", chatId)
                    eq("message_id", messageId)
                }
            }
            pending.remove(messageId)
            true
        } catch (e: Exception) {
            pending.remove(messageId)
            // Rollback
            _pinnedMessages.value = _pinnedMessages.value.toMutableMap().apply {
                put(chatId, currentRows)
            }
            false
        }
    }
}
