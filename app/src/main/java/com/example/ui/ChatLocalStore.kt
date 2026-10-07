package com.example.ui

import android.content.Context
import com.example.cache.AppDatabase
import com.example.cache.CachedMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatLocalStore(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val dao = db.cachedMessageDao()

    companion object {
        /** عدد الرسائل الأخيرة التي تُرسم من Room (نافذة كافية للفتح الفوري). */
        const val WINDOW = 200
        private const val SEP = '\u001F'

        fun encodeReactions(list: List<String>): String? =
            if (list.isEmpty()) null else list.joinToString(SEP.toString())

        fun decodeReactions(raw: String?): List<String> =
            if (raw.isNullOrEmpty()) emptyList() else raw.split(SEP).filter { it.isNotEmpty() }
    }

    fun getMessages(chatId: String, currentUserId: String?, otherUserName: String = ""): Flow<List<MessageModel>> {
        return dao.getRecentMessagesForChat(chatId, WINDOW).map { cachedList ->
            cachedList.map { it.toMessageModel(currentUserId, otherUserName) }.let { list ->
                // Link replies locally
                list.map { msg ->
                    if (msg.replyToId != null) {
                        msg.copy(replyTo = list.find { it.id == msg.replyToId })
                    } else msg
                }
            }
        }
    }

    private fun CachedMessage.toMessageModel(currentUserId: String?, otherUserName: String): MessageModel {
        val attachments = if (media_url != null && message_type != null) {
            val type = when (message_type) {
                "image" -> AttachmentType.IMAGE
                "video" -> AttachmentType.VIDEO
                "voice" -> AttachmentType.VOICE
                else -> AttachmentType.DOCUMENT
            }
            listOf(
                Attachment(
                    messageId = id,
                    type = type,
                    url = media_url,
                    thumbnailUrl = thumbnail_url,
                    aspectRatio = media_aspect_ratio,
                    fileSize = file_size,
                    durationMs = duration_ms
                )
            )
        } else emptyList()

        val isMine = sender_id == currentUserId

        return MessageModel(
            id = id,
            chatId = chat_id,
            senderId = sender_id,
            text = content,
            time = formatTimeSafe(created_at),
            isMine = isMine,
            senderName = if (isMine) "" else otherUserName,
            attachments = attachments,
            replyToId = reply_to_id,
            status = when (status) {
                "SENDING" -> MessageStatus.SENDING
                "FAILED" -> MessageStatus.FAILED
                "READ" -> MessageStatus.READ
                "DELIVERED" -> MessageStatus.DELIVERED
                else -> MessageStatus.SENT
            },
            createdAtExact = created_at,
            mediaGroupId = media_group_id,
            isEdited = edited_at != null,
            reactions = decodeReactions(reactions),
            replyMarkup = reply_markup
        ).also {
            // Restore link preview if present
            if (link_url != null) {
                // To restore LinkPreview data natively, we can seed the cache
                // The cache is in LinkPreviewCache
                if (LinkPreviewCache.cache[link_url] == null) {
                    val preview = LinkPreviewData(
                        url = link_url,
                        title = link_title,
                        description = link_description,
                        imageUrl = link_image_url
                    )
                    LinkPreviewCache.cache[link_url] = preview
                }
            }
        }
    }
}


private fun mergeAttachments(cur: List<Attachment>, db: List<Attachment>): List<Attachment> {
    if (cur.isEmpty()) return db
    if (db.isEmpty() || cur.size != db.size) return cur
    return cur.mapIndexed { i, c ->
        val d = db[i]
        c.copy(
            thumbnailUrl = c.thumbnailUrl ?: d.thumbnailUrl,
            aspectRatio = c.aspectRatio ?: d.aspectRatio,
            fileSize = c.fileSize ?: d.fileSize,
            durationMs = c.durationMs ?: d.durationMs
        )
    }
}

/**
 * يدمج ما في Room (المصدر الوحيد) مع الحالة الحيّة في الواجهة بدون فقدان أي شيء:
 * - الحقول الحيّة (تفاعلات، تثبيت، حفظ، أرقام القناة…) تبقى كما هي،
 * - الرسائل المؤقتة/غير المكتوبة بعد تبقى في مكانها الزمني،
 * - المحذوفة لا تعود.
 * الناتج مرتّب زمنياً ومستقر بالمعرّف (diff بلا وميض).
 */
fun mergeRoomIntoUi(db: List<MessageModel>, cur: List<MessageModel>, deleted: Set<String>): List<MessageModel> {
    val curById = HashMap<String, MessageModel>(cur.size * 2 + 1)
    cur.forEach { curById[it.id] = it }
    val dbIds = HashSet<String>(db.size * 2 + 1)
    db.forEach { dbIds.add(it.id) }

    val mergedDb = ArrayList<MessageModel>(db.size)
    for (d in db) {
        if (d.id in deleted) continue
        val c = curById[d.id]
        if (c == null) { mergedDb.add(d); continue }
        mergedDb.add(
            d.copy(
                senderName = if (c.senderName.isNotEmpty()) c.senderName else d.senderName,
                text = if (d.isEdited && !c.isEdited) d.text else c.text,
                isEdited = d.isEdited || c.isEdited,
                reactions = c.reactions,
                isPinned = c.isPinned,
                isSaved = c.isSaved,
                status = if (c.status == MessageStatus.READ) MessageStatus.READ else d.status,
                attachments = mergeAttachments(c.attachments, d.attachments),
                replyTo = c.replyTo ?: d.replyTo,
                timestamp = c.timestamp,
                replyMarkup = c.replyMarkup ?: d.replyMarkup,
                viewsLabel = c.viewsLabel,
                forwardsLabel = c.forwardsLabel,
                totalInteractionsLabel = c.totalInteractionsLabel,
                channelReactions = c.channelReactions
            )
        )
    }

    val extras = cur.filter { it.id !in dbIds && it.id !in deleted }
    if (extras.isEmpty()) return mergedDb

    val result = ArrayList<MessageModel>(mergedDb.size + extras.size)
    val firstDbTime = db.firstNotNullOfOrNull { it.createdAtExact }?.let { parseTimestampSafe(it) }
    val older = ArrayList<MessageModel>()
    val rest = ArrayList<MessageModel>()
    for (e in extras) {
        val t = e.createdAtExact
        if (firstDbTime != null && t != null && parseTimestampSafe(t) < firstDbTime) older.add(e) else rest.add(e)
    }
    result.addAll(older)
    result.addAll(mergedDb)
    val temps = ArrayList<MessageModel>()
    for (e in rest) {
        val t = e.createdAtExact
        if (t == null) { temps.add(e); continue }
        val et = parseTimestampSafe(t)
        var idx = result.size
        while (idx > 0) {
            val prev = result[idx - 1].createdAtExact
            if (prev == null || parseTimestampSafe(prev) <= et) break
            idx--
        }
        result.add(idx, e)
    }
    result.addAll(temps)
    return result
}
