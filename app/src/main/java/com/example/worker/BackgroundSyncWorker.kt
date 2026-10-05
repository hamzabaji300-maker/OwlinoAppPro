package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import coil.imageLoader
import coil.request.ImageRequest
import com.example.supabase
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.auth.auth
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.firstOrNull
import android.util.Log

class BackgroundSyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val currentUserId = try { supabase.auth.currentUserOrNull()?.id } catch(e: Exception) { null }
            if (currentUserId == null) {
                return@withContext Result.success()
            }
            
            val imageLoader = context.imageLoader
            
            // Fetch Latest Messages (Chats)
            val myMemberships = try {
                supabase.postgrest["chat_members"].select(io.github.jan.supabase.postgrest.query.Columns.list("chat_id")) {
                    filter { eq("user_id", currentUserId) }
                }.decodeList<com.example.ui.ChatMemberRow>()
            } catch(e: Exception) { emptyList() }
            
            val chatIds = myMemberships.map { it.chat_id }
            
            if (chatIds.isNotEmpty()) {
                val allMsgs = try {
                    supabase.postgrest["messages"].select() {
                        filter { isIn("chat_id", chatIds) }
                        order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    }.decodeList<com.example.ui.MessageRow>()
                } catch(e: Exception) { emptyList() }
                
                if (allMsgs.isNotEmpty()) {
                    try {
                        val toCache = allMsgs.map {
                            com.example.cache.CachedMessage(
                                id = it.id,
                                chat_id = it.chat_id,
                                sender_id = it.sender_id,
                                content = it.content,
                                created_at = it.created_at,
                                status = "SENT",
                                message_type = it.message_type,
                                media_url = it.media_url,
                                reply_to_id = it.reply_to_id
                            )
                        }
                        val cacheDb = com.example.cache.AppDatabase.getDatabase(context)
                        cacheDb.cachedMessageDao().insertCachedMessages(toCache)
                    } catch(e: Exception) { }
                }
                
                // Prefetch remote chats for ChatEntities
                val remoteChats = try {
                    supabase.postgrest["chats"].select(io.github.jan.supabase.postgrest.query.Columns.list("id, type, title, avatar_url")) {
                        filter { isIn("id", chatIds) }
                    }.decodeList<com.example.ui.ChatRow>()
                } catch(e: Exception) { emptyList() }
                
                val allMembers = try {
                    supabase.postgrest["chat_members"].select(io.github.jan.supabase.postgrest.query.Columns.list("chat_id, user_id")) {
                        filter { isIn("chat_id", chatIds) }
                    }.decodeList<com.example.ui.ChatMemberRow>()
                } catch (e: Exception) { emptyList() }
                
                val otherUserIds = allMembers.filter { it.user_id != currentUserId }.mapNotNull { it.user_id }.distinct()
                val profiles = if (otherUserIds.isNotEmpty()) {
                    try {
                        supabase.postgrest["profiles"].select() {
                            filter { isIn("id", otherUserIds) }
                        }.decodeList<com.example.ui.Profile>().associateBy { it.id }
                    } catch(e: Exception) { emptyMap() }
                } else emptyMap()

                val dataDb = com.example.data.DatabaseProvider.getDatabase(context)
                val chatDao = dataDb.chatDao()
                val roomChats = chatDao.getAllChats().firstOrNull() ?: emptyList()

                val newEntities = remoteChats.map { rc ->
                    val isGroupOrChannel = rc.type == "group" || rc.type == "channel"
                    val otherUserId = allMembers.find { it.chat_id == rc.id && it.user_id != currentUserId }?.user_id
                    val otherProfile = profiles[otherUserId]
                    val realName = if (isGroupOrChannel) (rc.title ?: "Chat ${rc.id.take(4)}")
                                    else (otherProfile?.fullName ?: otherProfile?.username ?: "Chat ${rc.id.take(4)}")
                    val avatar = if (isGroupOrChannel) rc.avatar_url else otherProfile?.avatarUrl
                    
                    val lastMsg = allMsgs.firstOrNull { it.chat_id == rc.id }
                    val msgText = lastMsg?.content ?: ""
                    
                    val timeStr = if (lastMsg != null) {
                        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                        formatter.timeZone = java.util.TimeZone.getTimeZone("UTC")
                        try {
                            val parsed = formatter.parse(lastMsg.created_at)
                            val now = java.util.Date()
                            val diff = now.time - (parsed?.time ?: 0)
                            when {
                                diff < 60000 -> "Now"
                                diff < 3600000 -> "${diff / 60000}m"
                                diff < 86400000 -> "${diff / 3600000}h"
                                else -> "${diff / 86400000}d"
                            }
                        } catch(e: Exception) { "Now" }
                    } else "Now"

                    val timestamp = try {
                         val f = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                         f.timeZone = java.util.TimeZone.getTimeZone("UTC")
                         f.parse(lastMsg?.created_at ?: "")?.time ?: 0L
                    } catch(e: Exception) { 0L }

                    val existingChat = roomChats.find { it.id == rc.id }
                    
                    // prefetch avatar
                    if (avatar != null && avatar.isNotEmpty()) {
                         val req = ImageRequest.Builder(context).data(avatar).build()
                         imageLoader.enqueue(req)
                    }

                    com.example.data.ChatEntity(
                        id = rc.id,
                        name = realName,
                        avatarUrl = avatar,
                        isGroup = rc.type == "group",
                        time = timeStr,
                        message = msgText,
                        isOnline = otherProfile?.isOnlineNow ?: false,
                        participantIds = if (otherUserId != null) otherUserId else "[]",
                        draft = existingChat?.draft ?: "",
                        timestamp = timestamp,
                        unreadCount = existingChat?.unreadCount ?: 0,
                        isBot = existingChat?.isBot ?: false,
                        isVerified = existingChat?.isVerified ?: false,
                        hasStar = existingChat?.hasStar ?: false,
                        isMuted = existingChat?.isMuted ?: false,
                        isFavorite = existingChat?.isFavorite ?: false,
                        isBlocked = existingChat?.isBlocked ?: false,
                        isArchived = existingChat?.isArchived ?: false,
                        isChannel = rc.type == "channel",
                        hasSparkleBadge = existingChat?.hasSparkleBadge ?: false,
                        isReadReceipt = existingChat?.isReadReceipt ?: false,
                        isMine = lastMsg?.sender_id == currentUserId,
                        isNotes = existingChat?.isNotes ?: false,
                        isDefaultAvatar = existingChat?.isDefaultAvatar ?: false
                    )
                }
                chatDao.insertAll(newEntities)
                
                val newCachedToInsert = newEntities.map {
                    com.example.cache.CachedChat(
                        chat_id = it.id,
                        name = it.name,
                        last_message = it.message,
                        time_str = it.time,
                        timestamp = it.timestamp,
                        unread_count = it.unreadCount
                    )
                }
                com.example.cache.AppDatabase.getDatabase(context).cachedChatDao().insertCachedChats(newCachedToInsert)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
