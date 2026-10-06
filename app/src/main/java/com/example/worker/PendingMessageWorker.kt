package com.example.worker

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.cache.AppDatabase
import com.example.supabase
import com.example.ui.MessageInsert
import com.example.ui.MessageRow
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.time.Duration.Companion.days

class PendingMessageWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            val messageDao = db.cachedMessageDao()
            
            val pendingMessages = messageDao.getPendingMessages()
            if (pendingMessages.isEmpty()) {
                return@withContext Result.success()
            }

            var needsRetry = false

            for (msg in pendingMessages) {
                try {
                    var finalMediaUrl: String? = null
                    var bucketName: String? = null
                    var path: String? = null

                    // If it's an attachment and not yet uploaded (URL starts with content://)
                    if (msg.media_url != null && msg.media_url.startsWith("content://")) {
                        bucketName = when (msg.message_type) {
                            "image" -> "chat-images"
                            "video" -> "chat-videos"
                            "voice", "audio" -> "voice-messages"
                            else -> "chat-files"
                        }
                        val uri = Uri.parse(msg.media_url)
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val bytes = inputStream?.readBytes()
                        if (bytes != null) {
                            val fileName = UUID.randomUUID().toString() + "-" + System.currentTimeMillis()
                            path = "${msg.chat_id}/${msg.sender_id}/$fileName"
                            supabase.storage[bucketName].upload(path, bytes) { upsert = true }
                            finalMediaUrl = supabase.storage[bucketName].createSignedUrl(path, 3650.days)
                        } else {
                            messageDao.updateMessageStatus(msg.id, "FAILED")
                            continue
                        }
                    }

                    val insertData = MessageInsert(
                        chat_id = msg.chat_id,
                        sender_id = msg.sender_id,
                        content = msg.content,
                        message_type = msg.message_type,
                        media_url = finalMediaUrl ?: msg.media_url,
                        reply_to_id = msg.reply_to_id,
                        media_aspect_ratio = msg.media_aspect_ratio,
                        media_group_id = msg.media_group_id
                    )

                    val result = supabase.postgrest["messages"].insert(insertData) {
                        select()
                    }.decodeSingle<MessageRow>()

                    messageDao.deleteMessageById(msg.id)
                    messageDao.insertCachedMessage(
                        com.example.cache.CachedMessage(
                            id = result.id,
                            chat_id = result.chat_id,
                            sender_id = result.sender_id,
                            content = result.content,
                            created_at = result.created_at,
                            status = "SENT",
                            message_type = result.message_type,
                            media_url = result.media_url,
                            reply_to_id = result.reply_to_id,
                            media_aspect_ratio = result.media_aspect_ratio,
                            media_group_id = result.media_group_id
                        )
                    )
                    
                } catch (e: Exception) {
                    if (isNetworkError(e)) {
                        needsRetry = true
                    } else {
                        messageDao.updateMessageStatus(msg.id, "FAILED")
                    }
                }
            }

            if (needsRetry) {
                Result.retry()
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            if (isNetworkError(e)) Result.retry() else Result.failure()
        }
    }

    private fun isNetworkError(e: Exception): Boolean {
        val msg = e.message?.lowercase() ?: ""
        return e is java.net.UnknownHostException || 
               e is java.net.ConnectException || 
               e is java.net.SocketTimeoutException ||
               "timeout" in msg || "network" in msg || "connection" in msg
    }
}
