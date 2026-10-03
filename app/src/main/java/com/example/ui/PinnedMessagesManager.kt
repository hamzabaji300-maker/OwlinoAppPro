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

    suspend fun load(chatId: String, myId: String) {
        try {
            val rows = supabase.postgrest["pinned_messages"]
                .select {
                    filter { eq("chat_id", chatId) }
                }
                .decodeList<PinnedMessageRow>()
            
            _pinnedMessages.value = _pinnedMessages.value.toMutableMap().apply {
                put(chatId, rows)
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
        _pinnedMessages.value = _pinnedMessages.value.toMutableMap().apply {
            put(chatId, currentRows + newRow)
        }

        return try {
            supabase.postgrest["pinned_messages"].insert(newRow)
            true
        } catch (e: Exception) {
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
            true
        } catch (e: Exception) {
            // Rollback
            _pinnedMessages.value = _pinnedMessages.value.toMutableMap().apply {
                put(chatId, currentRows)
            }
            false
        }
    }
}
