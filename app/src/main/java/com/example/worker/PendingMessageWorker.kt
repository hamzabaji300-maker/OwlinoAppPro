package com.example.worker

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.cache.AppDatabase
import com.example.supabase
import com.example.ui.AttachmentType
import com.example.util.LocalMediaException
import com.example.util.MediaUploader
import com.example.util.NetworkUtils
import com.example.util.UploadLimitException
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
                // الشاشة ما زالت ترسل هذه الرسالة الآن: لا نرسلها مرة ثانية، نعيد الفحص لاحقًا
                if (msg.id in com.example.AppState.inFlightMessageIds) {
                    needsRetry = true
                    continue
                }
                // قد تكون أُرسلت من الشاشة بعد أن أخذنا القائمة: نتأكد أنها ما زالت قيد الإرسال
                val fresh = messageDao.getCachedMessageById(msg.id)
                if (fresh == null || fresh.status != "SENDING") continue

                try {
                    var finalMediaUrl: String? = null
                    var bucketName: String? = null
                    var path: String? = null

                    // If it's an attachment and not yet uploaded (local URI: content:// or file://)
                    if (msg.media_url != null && (msg.media_url.startsWith("content://") || msg.media_url.startsWith("file://"))) {
                        bucketName = when (msg.message_type) {
                            "image" -> "chat-images"
                            "video" -> "chat-videos"
                            "voice", "audio" -> "voice-messages"
                            else -> "chat-files"
                        }
                        val uri = Uri.parse(msg.media_url)
                        val attachType = when (msg.message_type) {
                            "image" -> AttachmentType.IMAGE
                            "video" -> AttachmentType.VIDEO
                            "voice", "audio" -> AttachmentType.VOICE
                            else -> AttachmentType.DOCUMENT
                        }
                        val fileName = UUID.randomUUID().toString() + "-" + System.currentTimeMillis()
                        path = "${msg.chat_id}/${msg.sender_id}/$fileName"
                        try {
                            // نفس مسار الرفع الرئيسي: فحص الحدود + ضغط الصور
                            finalMediaUrl = MediaUploader.prepareAndUpload(
                                context = context,
                                supabase = supabase,
                                bucketName = bucketName,
                                path = path,
                                uri = uri,
                                type = attachType,
                                upsert = true
                            )
                        } catch (e: UploadLimitException) {
                            messageDao.updateMessageStatus(msg.id, "FAILED")
                            continue
                        } catch (e: LocalMediaException) {
                            // الملف المحلي لم يعد موجودًا: لا فائدة من إعادة المحاولة
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

                    // نفس معرّف الرسالة المؤقتة: لو وصلت سابقًا لا تتكرر
                    val result = com.example.util.MessageSender.insert(supabase, insertData, msg.id)

                    if (result.id != msg.id) {
                        messageDao.deleteMessageById(msg.id)
                        // نخبر الشاشة المفتوحة (إن وُجدت) أن النسخة المؤقتة استُبدلت، حتى لا تظهر مرتين
                        com.example.AppState.messageReplaced.tryEmit(msg.id)
                    }
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
                    if (NetworkUtils.isNetworkError(context, e)) {
                        needsRetry = true
                    } else {
                        messageDao.updateMessageStatus(msg.id, "FAILED")
                    }
                }
            }

            if (needsRetry) {
                // حد أقصى للمحاولات حتى لا يبقى العامل يحاول للأبد
                if (runAttemptCount >= 12) {
                    pendingMessages
                        .filter { it.id !in com.example.AppState.inFlightMessageIds }
                        .forEach { messageDao.updateMessageStatus(it.id, "FAILED") }
                    Result.failure()
                } else {
                    Result.retry()
                }
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            if (NetworkUtils.isNetworkError(context, e)) Result.retry() else Result.failure()
        }
    }
}
