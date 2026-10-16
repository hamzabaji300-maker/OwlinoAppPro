package com.example.ui

import android.content.Context
import com.example.supabase
import com.example.data.ChatDao
import com.example.cache.AppDatabase as CacheDatabase
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable

@Serializable
data class TrashedChatInsert(val user_id: String, val chat_id: String)

@Serializable
data class ClearedChatInsert(val user_id: String, val chat_id: String, val cleared_at: String? = null)

@Serializable
data class ArchivedChatInsert(val user_id: String, val chat_id: String)

object ChatLifecycleManager {

    suspend fun deleteChats(
        context: Context,
        myId: String,
        chatIds: Collection<String>,
        forEveryone: Boolean,
        chatDao: ChatDao
    ): Boolean {
        if (chatIds.isEmpty()) return true
        
        val cachedDb = CacheDatabase.getDatabase(context)
        val cachedChatDao = cachedDb.cachedChatDao()
        val cachedMessageDao = cachedDb.cachedMessageDao()
        val prefs = context.getSharedPreferences("chat_cleared_times", Context.MODE_PRIVATE)

        val allChats = chatDao.getAllChats().first()
        val snapshots = chatIds.mapNotNull { id -> allChats.find { it.id == id } }
        // "Delete for everyone" is only allowed for direct chats (never groups/channels)
        val everyoneIds = if (forEveryone) {
            snapshots.filter { !it.isGroup && !it.isChannel }.map { it.id }
        } else emptyList()
        val nowMs = System.currentTimeMillis()
        val nowIso = java.time.Instant.ofEpochMilli(nowMs).toString()

        chatIds.forEach { id ->
            chatDao.deleteChatById(id)
            cachedChatDao.deleteChatById(id)
            cachedMessageDao.deleteMessagesForChat(id)
            prefs.edit().putLong(id, nowMs).apply()
        }

        try {
            val trashedRows = chatIds.map { TrashedChatInsert(user_id = myId, chat_id = it) }
            supabase.postgrest["trashed_chats"].upsert(trashedRows)

            val clearedRows = chatIds.map { ClearedChatInsert(user_id = myId, chat_id = it, cleared_at = nowIso) }
            supabase.postgrest["cleared_chats"].upsert(clearedRows)

            everyoneIds.forEach { id ->
                supabase.postgrest["messages"].delete {
                    filter { eq("chat_id", id) }
                }
            }
            return true
        } catch (e: Exception) {
            snapshots.forEach { chat ->
                chatDao.insert(chat)
            }
            return false
        }
    }

    suspend fun clearHistory(
        context: Context,
        myId: String,
        chatIds: Collection<String>,
        forEveryone: Boolean,
        chatDao: ChatDao
    ): Boolean {
        if (chatIds.isEmpty()) return true
        
        val cachedDb = CacheDatabase.getDatabase(context)
        val cachedMessageDao = cachedDb.cachedMessageDao()
        val prefs = context.getSharedPreferences("chat_cleared_times", Context.MODE_PRIVATE)

        val allChatsForClear = chatDao.getAllChats().first()
        val everyoneIds = if (forEveryone) {
            chatIds.filter { id -> allChatsForClear.find { it.id == id }?.let { !it.isGroup && !it.isChannel } == true }
        } else emptyList()
        val nowMs = System.currentTimeMillis()
        val nowIso = java.time.Instant.ofEpochMilli(nowMs).toString()

        chatIds.forEach { id ->
            chatDao.clearChatMessage(id)
            cachedMessageDao.deleteMessagesForChat(id)
            prefs.edit().putLong(id, nowMs).apply()
        }

        try {
            val clearedRows = chatIds.map { ClearedChatInsert(user_id = myId, chat_id = it, cleared_at = nowIso) }
            supabase.postgrest["cleared_chats"].upsert(clearedRows)

            everyoneIds.forEach { id ->
                supabase.postgrest["messages"].delete {
                    filter { eq("chat_id", id) }
                }
            }
            return true
        } catch (e: Exception) {
            return false
        }
    }

    suspend fun setArchived(
        context: Context?,
        myId: String,
        chatIds: Collection<String>,
        archived: Boolean,
        chatDao: ChatDao
    ): Boolean {
        if (chatIds.isEmpty()) return true
        
        val allChats = chatDao.getAllChats().first()
        val previousStates = chatIds.associateWith { chatId ->
            allChats.find { it.id == chatId }?.isArchived ?: false
        }

        chatIds.forEach { id ->
            chatDao.updateArchiveState(id, archived)
        }

        try {
            if (archived) {
                val archivedRows = chatIds.map { ArchivedChatInsert(user_id = myId, chat_id = it) }
                supabase.postgrest["archived_chats"].upsert(archivedRows)
            } else {
                supabase.postgrest["archived_chats"].delete {
                    filter {
                        eq("user_id", myId)
                        isIn("chat_id", chatIds.toList())
                    }
                }
            }
            return true
        } catch (e: Exception) {
            chatIds.forEach { id ->
                chatDao.updateArchiveState(id, previousStates[id] ?: false)
            }
            return false
        }
    }

    suspend fun refreshLifecycle(myId: String, chatDao: ChatDao, context: Context) {
        try {
            val trashedIds = supabase.postgrest["trashed_chats"]
                .select { filter { eq("user_id", myId) } }
                .decodeList<TrashedChatInsert>().map { it.chat_id }.toSet()

            val clearedChats = supabase.postgrest["cleared_chats"]
                .select { filter { eq("user_id", myId) } }
                .decodeList<ClearedChatInsert>()

            val archivedIds = supabase.postgrest["archived_chats"]
                .select { filter { eq("user_id", myId) } }
                .decodeList<ArchivedChatInsert>().map { it.chat_id }.toSet()

            val prefs = context.getSharedPreferences("chat_cleared_times", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            
            clearedChats.forEach { cleared ->
                if (cleared.cleared_at != null) {
                    val time = parseTimestampSafe(cleared.cleared_at)
                    editor.putLong(cleared.chat_id, time)
                } else {
                    editor.putLong(cleared.chat_id, System.currentTimeMillis())
                }
            }
            editor.apply()

            val allChats = chatDao.getAllChats().first()
            val cachedDb = CacheDatabase.getDatabase(context)

            allChats.forEach { chat ->
                if (chat.id in trashedIds) {
                    chatDao.deleteChatById(chat.id)
                    cachedDb.cachedChatDao().deleteChatById(chat.id)
                    cachedDb.cachedMessageDao().deleteMessagesForChat(chat.id)
                } else {
                    val shouldBeArchived = chat.id in archivedIds
                    if (chat.isArchived != shouldBeArchived) {
                        chatDao.updateArchiveState(chat.id, shouldBeArchived)
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore fetch failure
        }
    }
}
