package com.example.ui

import com.example.supabase
import com.example.data.ChatDao
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable
import kotlinx.coroutines.flow.first

@Serializable
data class ChatActionRow(
    val user_id: String,
    val chat_id: String
)

object ChatStateManager {

    suspend fun setMuted(myId: String, chatIds: Collection<String>, muted: Boolean, chatDao: ChatDao): Boolean {
        return performAction(myId, chatIds, muted, chatDao, "muted_chats") { id, state -> chatDao.updateMuteState(id, state) }
    }

    suspend fun setPinned(myId: String, chatIds: Collection<String>, pinned: Boolean, chatDao: ChatDao): Boolean {
        return performAction(myId, chatIds, pinned, chatDao, "pinned_chats") { id, state -> chatDao.updatePinState(id, state) }
    }

    suspend fun setFavorite(myId: String, chatIds: Collection<String>, favorite: Boolean, chatDao: ChatDao): Boolean {
        return performAction(myId, chatIds, favorite, chatDao, "favorite_chats") { id, state -> chatDao.updateFavoriteState(id, state) }
    }

    private suspend fun performAction(
        myId: String,
        chatIds: Collection<String>,
        newState: Boolean,
        chatDao: ChatDao,
        tableName: String,
        updateRoom: suspend (String, Boolean) -> Unit
    ): Boolean {
        val allChats = chatDao.getAllChats().first()
        val previousStates = chatIds.associateWith { chatId ->
            val chat = allChats.find { it.id == chatId }
            when (tableName) {
                "muted_chats" -> chat?.isMuted ?: false
                "pinned_chats" -> chat?.hasStar ?: false
                "favorite_chats" -> chat?.isFavorite ?: false
                else -> false
            }
        }

        chatIds.forEach { chatId ->
            updateRoom(chatId, newState)
        }

        return try {
            val rows = chatIds.map { ChatActionRow(user_id = myId, chat_id = it) }
            if (newState) {
                // Works whether or not the table has a unique (user_id, chat_id) constraint:
                // remove any existing rows first so repeated calls never duplicate or fail.
                supabase.postgrest[tableName].delete {
                    filter {
                        eq("user_id", myId)
                        isIn("chat_id", chatIds.toList())
                    }
                }
                supabase.postgrest[tableName].insert(rows)
            } else {
                supabase.postgrest[tableName].delete {
                    filter {
                        eq("user_id", myId)
                        isIn("chat_id", chatIds.toList())
                    }
                }
            }
            true
        } catch (e: Exception) {
            chatIds.forEach { chatId ->
                val prev = previousStates[chatId] ?: false
                updateRoom(chatId, prev)
            }
            false
        }
    }

    suspend fun refresh(myId: String, chatDao: ChatDao) {
        try {
            val mutedIds = fetchSet(myId, "muted_chats")
            val pinnedIds = fetchSet(myId, "pinned_chats")
            val favoriteIds = fetchSet(myId, "favorite_chats")

            val allChats = chatDao.getAllChats().first()
            allChats.forEach { chat ->
                val shouldBeMuted = chat.id in mutedIds
                if (chat.isMuted != shouldBeMuted) chatDao.updateMuteState(chat.id, shouldBeMuted)
                
                val shouldBePinned = chat.id in pinnedIds
                if (chat.hasStar != shouldBePinned) chatDao.updatePinState(chat.id, shouldBePinned)
                
                val shouldBeFavorite = chat.id in favoriteIds
                if (chat.isFavorite != shouldBeFavorite) chatDao.updateFavoriteState(chat.id, shouldBeFavorite)
            }
        } catch (e: Exception) {
            // Network unavailable
        }
    }

    private suspend fun fetchSet(myId: String, tableName: String): Set<String> {
        return supabase.postgrest[tableName]
            .select {
                filter { eq("user_id", myId) }
            }.decodeList<ChatActionRow>().map { it.chat_id }.toSet()
    }

    fun clear() {
        // Nothing to clear currently
    }
}
