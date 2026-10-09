package com.example.worker

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaMetadataRetriever
import android.net.ConnectivityManager
import android.os.BatteryManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import coil.imageLoader
import coil.request.ImageRequest
import com.example.cache.AppDatabase
import com.example.cache.CachedMessage
import com.example.cache.insertMergedAll
import com.example.supabase
import com.example.ui.MediaIndex
import com.example.util.MediaStorage
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Prefetch Local-First: يجلب آخر 50 رسالة لكل محادثة (الأكثر نشاطاً أولاً) ويكتبها في Room كاملة
 * (مصغّرات، نسب الصور، مدة الصوت، معاينة الروابط، التعديل) ويحمّل الوسائط الصغيرة مسبقاً.
 * يقرأ فقط من الشبكة؛ لا يغيّر أي استعلام/مخطط على السيرفر.
 */
class BackgroundSyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val ONE_TIME_NAME = "BackgroundSyncNow"
        private const val MAX_THUMBS_PER_RUN = 150
        private const val TOP_CHATS_FOR_MEDIA = 12
        private const val TOP_CHATS_FOR_FULL_IMAGES = 5
        private const val MAX_VOICE_PER_CHAT = 12
        private const val MAX_FULL_IMAGES_PER_CHAT = 8
        private const val MAX_LINK_PREVIEWS_PER_RUN = 15
        private const val RUN_BUDGET_BYTES = 40L * 1024L * 1024L
        private const val VOICE_DIR_CAP_BYTES = 150L * 1024L * 1024L
        private const val VOICE_MAX_UNMETERED = 3L * 1024L * 1024L
        private const val VOICE_MAX_METERED = 300L * 1024L
        private val URL_REGEX = Regex("https?://[\\w\\d\\-_]+(\\.[\\w\\d\\-_]+)+([\\w\\d\\-.,@?^=%&:/~+#]*[\\w\\d\\-@?^=%&/~+#])?")

        /** تشغيل فوري (فتح التطبيق / عودة الشبكة / وصول رسالة). KEEP: لا يتكدس أكثر من تشغيل واحد. */
        fun enqueueNow(context: Context, delaySeconds: Long = 0L) {
            try {
                val req = androidx.work.OneTimeWorkRequestBuilder<BackgroundSyncWorker>()
                    .setInitialDelay(delaySeconds, java.util.concurrent.TimeUnit.SECONDS)
                    .setInputData(androidx.work.workDataOf("messages_only" to true))
                    .setConstraints(
                        androidx.work.Constraints.Builder()
                            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                            .build()
                    )
                    .build()
                androidx.work.WorkManager.getInstance(context.applicationContext)
                    .enqueueUniqueWork(ONE_TIME_NAME, androidx.work.ExistingWorkPolicy.KEEP, req)
            } catch (e: Throwable) { }
        }
    }

    private fun isBatteryLow(): Boolean = try {
        val i = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = i?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        level >= 0 && scale > 0 && (level * 100 / scale) < 15 && !charging
    } catch (e: Exception) { false }

    private fun isMetered(): Boolean = try {
        (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).isActiveNetworkMetered
    } catch (e: Exception) { false }

    private fun dirSize(dir: File): Long = try {
        dir.listFiles()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L
    } catch (e: Exception) { 0L }

    private fun localDuration(path: String): Long? {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(path)
            r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        } catch (e: Exception) { null } finally {
            try { r.release() } catch (e: Exception) { }
        }
    }

    /** يحمّل مقطعاً صوتياً صغيراً إلى مجلد Owlino المحلي (نفس اسم MediaDownloadManager: OWL-<id>.<ext>). */
    private fun downloadVoice(msgId: String, url: String, maxBytes: Long): Pair<String, Long>? {
        var tmp: File? = null
        return try {
            MediaStorage.ensureFolders(context)
            val dir = MediaStorage.dirFor(context, MediaStorage.Kind.VOICE)
            dir.mkdirs()
            val ext = url.substringBefore('?').substringAfterLast('.', "").takeIf { it.length in 2..4 }?.lowercase() ?: "ogg"
            val target = File(dir, MediaStorage.fileNameFor(msgId, ext))
            if (target.exists() && target.length() > 0) {
                MediaIndex.put(msgId, target.absolutePath)
                return target.absolutePath to 0L
            }
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 20000
                instanceFollowRedirects = true
            }
            conn.connect()
            if (conn.responseCode !in 200..299 || conn.contentLengthLong > maxBytes) {
                conn.disconnect()
                return null
            }
            val tmpFile = File(dir, target.name + ".tmp")
            tmp = tmpFile
            var total = 0L
            conn.inputStream.use { input ->
                tmpFile.outputStream().use { out ->
                    val buf = ByteArray(32 * 1024)
                    while (true) {
                        val r = input.read(buf)
                        if (r < 0) break
                        total += r
                        if (total > maxBytes) throw java.io.IOException("too big")
                        out.write(buf, 0, r)
                    }
                }
            }
            if (!tmpFile.renameTo(target)) {
                tmpFile.copyTo(target, overwrite = true)
                tmpFile.delete()
            }
            MediaIndex.put(msgId, target.absolutePath)
            target.absolutePath to total
        } catch (e: Exception) {
            try { tmp?.delete() } catch (x: Exception) { }
            null
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val currentUserId = try { supabase.auth.currentUserOrNull()?.id } catch(e: Exception) { null }
            if (currentUserId == null) {
                return@withContext Result.success()
            }
            // إلغاء آمن عند انخفاض البطارية (ما لم يكن الجهاز يشحن)
            if (isBatteryLow()) return@withContext Result.success()

            val messagesOnly = inputData.getBoolean("messages_only", false)
            val metered = isMetered()
            val imageLoader = context.imageLoader

            // Fetch Latest Messages (Chats)
            val myMemberships = try {
                supabase.postgrest["chat_members"].select(io.github.jan.supabase.postgrest.query.Columns.list("chat_id")) {
                    filter { eq("user_id", currentUserId) }
                }.decodeList<com.example.ui.ChatMemberRow>()
            } catch(e: Exception) { emptyList() }

            val chatIds = myMemberships.map { it.chat_id }

            if (chatIds.isNotEmpty()) {
                val cacheDb = AppDatabase.getDatabase(context)
                val msgDao = cacheDb.cachedMessageDao()

                // الأولوية: المحادثات الأكثر نشاطاً/الأحدث أولاً
                val activity: Map<String, Long> = try {
                    cacheDb.cachedChatDao().getAllCachedChats().firstOrNull()?.associate { it.chat_id to it.timestamp } ?: emptyMap()
                } catch (e: Exception) { emptyMap() }
                val ordered = chatIds.distinct().sortedByDescending { activity[it] ?: 0L }

                // 1) آخر 50 رسالة لكل محادثة (قراءة فقط، 4 طلبات بالتوازي كحد أقصى)
                val perChat = java.util.concurrent.ConcurrentHashMap<String, List<com.example.ui.MessageRow>>()
                val sem = Semaphore(4)
                withTimeoutOrNull(5 * 60_000L) {
                    coroutineScope {
                        ordered.map { cid ->
                            async {
                                sem.withPermit {
                                    val rows = try {
                                        supabase.postgrest["messages"].select() {
                                            filter { eq("chat_id", cid) }
                                            order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                                            limit(50)
                                        }.decodeList<com.example.ui.MessageRow>()
                                    } catch (e: kotlinx.coroutines.CancellationException) {
                                        throw e
                                    } catch (e: Exception) { null }
                                    if (rows != null) perChat[cid] = rows
                                }
                            }
                        }.awaitAll()
                    }
                }

                // كل رسائل المحادثات (كل محادثة مرتبة من الأحدث) - تستعملها بقية الدالة لآخر رسالة
                val allMsgs = ordered.flatMap { perChat[it] ?: emptyList() }

                // 2) كتابة الصفوف الأساسية في Room فوراً (غير مُتلِفة) + مطابقة المحذوف على السيرفر
                val baseByChat = HashMap<String, List<CachedMessage>>()
                val existingById = HashMap<String, CachedMessage>()
                for (cid in ordered) {
                    ensureActive()
                    val rows = perChat[cid] ?: continue
                    if (rows.isEmpty()) continue
                    try {
                        msgDao.getCachedMessagesByIds(rows.map { it.id }).forEach { existingById[it.id] = it }
                        val base = rows.map {
                            CachedMessage(
                                id = it.id,
                                chat_id = it.chat_id,
                                sender_id = it.sender_id,
                                content = it.content,
                                created_at = it.created_at,
                                status = "SENT",
                                message_type = it.message_type,
                                media_url = it.media_url,
                                reply_to_id = it.reply_to_id,
                                media_aspect_ratio = it.media_aspect_ratio,
                                media_group_id = it.media_group_id,
                                edited_at = it.edited_at,
                                thumbnail_url = it.thumbnail_url
                            )
                        }
                        baseByChat[cid] = base
                        msgDao.insertMergedAll(base)
                        val times = rows.map { it.created_at }
                        val from = times.minOrNull()
                        val to = times.maxOrNull()
                        if (from != null && to != null) {
                            msgDao.deleteSyncedMissingInWindow(cid, from, to, rows.map { it.id })
                        }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) { }
                }

                // 3) الوسائط والبيانات الغنية بحدود صارمة (تخزين/بيانات الجوال/وقت)
                var budget = RUN_BUDGET_BYTES
                var thumbs = 0
                var links = 0
                val voiceDir = try { MediaStorage.dirFor(context, MediaStorage.Kind.VOICE) } catch (e: Exception) { null }
                var voiceDirBytes = if (voiceDir != null) dirSize(voiceDir) else 0L
                withTimeoutOrNull(6 * 60_000L) {
                    for ((rank, cid) in ordered.withIndex()) {
                        ensureActive()
                        if (isBatteryLow()) break
                        val rows = perChat[cid] ?: continue
                        val base = baseByChat[cid]?.associateBy { it.id } ?: continue
                        val updates = ArrayList<CachedMessage>()

                        // المصغّرات لكل الرسائل الحديثة (أو الصورة نفسها إن لم توجد مصغّرة)
                        for (msg in rows) {
                            if (thumbs >= MAX_THUMBS_PER_RUN) break
                            val thumb = msg.thumbnail_url ?: if (msg.message_type == "image") msg.media_url else null
                            if (thumb != null) {
                                imageLoader.enqueue(
                                    ImageRequest.Builder(context).data(thumb).size(256)
                                        .memoryCachePolicy(coil.request.CachePolicy.DISABLED).build()
                                )
                                thumbs++
                                kotlinx.coroutines.delay(20) // لا نُغرق الشبكة/المعالج
                            }
                        }

                        // الصور الكاملة لآخر عدد معقول في أعلى المحادثات (شبكة غير محدودة فقط)
                        if (!metered && rank < TOP_CHATS_FOR_FULL_IMAGES) {
                            rows.filter { it.message_type == "image" && it.media_url != null }
                                .take(MAX_FULL_IMAGES_PER_CHAT)
                                .forEach {
                                    imageLoader.enqueue(
                                        ImageRequest.Builder(context).data(it.media_url).size(720)
                                            .memoryCachePolicy(coil.request.CachePolicy.DISABLED).build()
                                    )
                                    kotlinx.coroutines.delay(40)
                                }
                        }

                        // المقاطع الصوتية الصغيرة -> ملف محلي + مدة محفوظة (تُشغَّل فوراً حتى بلا شبكة)
                        if (rank < TOP_CHATS_FOR_MEDIA) {
                            val voices = rows.filter { it.message_type == "voice" && it.media_url != null }.take(MAX_VOICE_PER_CHAT)
                            for (v in voices) {
                                ensureActive()
                                val old = existingById[v.id]
                                var path = MediaIndex.get(v.id)
                                if (path == null && budget > 0 && voiceDirBytes < VOICE_DIR_CAP_BYTES) {
                                    val got = downloadVoice(v.id, v.media_url!!, if (metered) VOICE_MAX_METERED else VOICE_MAX_UNMETERED)
                                    if (got != null) {
                                        path = got.first
                                        budget -= got.second
                                        voiceDirBytes += got.second
                                    }
                                }
                                if (path != null && old?.duration_ms == null) {
                                    val d = localDuration(path)
                                    val b = base[v.id]
                                    if (d != null && b != null) updates.add(b.copy(duration_ms = d))
                                }
                            }
                        }

                        // معاينات الروابط (عدد محدود لكل تشغيل، مع مهلة لكل رابط)
                        for (msg in rows) {
                            if (links >= MAX_LINK_PREVIEWS_PER_RUN) break
                            if (existingById[msg.id]?.link_url != null) continue
                            val url = URL_REGEX.find(msg.content)?.value ?: continue
                            links++
                            ensureActive()
                            val preview = try {
                                withTimeoutOrNull(4000L) { com.example.ui.LinkPreviewCache.fetch(url, context) }
                            } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { null }
                            val b = base[msg.id]
                            if (preview != null && b != null) {
                                updates.add(
                                    b.copy(
                                        link_url = preview.url,
                                        link_title = preview.title,
                                        link_description = preview.description,
                                        link_image_url = preview.imageUrl
                                    )
                                )
                                if (preview.imageUrl != null) {
                                    imageLoader.enqueue(ImageRequest.Builder(context).data(preview.imageUrl).build())
                                }
                            }
                        }

                        if (updates.isNotEmpty()) {
                            try {
                                // نجمع تحديثات الرسالة الواحدة (صوت/رابط) في صف واحد
                                val merged = updates.groupBy { it.id }.map { (_, list) ->
                                    list.reduce { a, b ->
                                        a.copy(
                                            duration_ms = a.duration_ms ?: b.duration_ms,
                                            link_url = a.link_url ?: b.link_url,
                                            link_title = a.link_title ?: b.link_title,
                                            link_description = a.link_description ?: b.link_description,
                                            link_image_url = a.link_image_url ?: b.link_image_url
                                        )
                                    }
                                }
                                msgDao.insertMergedAll(merged)
                            } catch (e: kotlinx.coroutines.CancellationException) {
                                throw e
                            } catch (e: Exception) { }
                        }
                    }
                }

                // تشغيل فوري (فتح/شبكة/إشعار): لا نكتب صفوف قائمة المحادثات أبداً؛ الشاشة الرئيسية و realtime مسؤولان عنها
                if (messagesOnly) return@withContext Result.success()

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

                val newEntities = remoteChats.mapNotNull { rc ->
                    val isGroupOrChannel = rc.type == "group" || rc.type == "channel"
                    val existingChat = roomChats.find { it.id == rc.id }
                    val lastMsg = allMsgs.firstOrNull { it.chat_id == rc.id }
                    // فشل جلب رسائل هذه المحادثة: لا نمسح آخر رسالة ولا الترتيب (كان يُنزل المحادثة لآخر القائمة)
                    if (existingChat != null) {
                        if (lastMsg == null) return@mapNotNull null
                        val ts = com.example.ui.parseTimestampSafe(lastMsg.created_at)
                        val fresh = chatDao.getChatById(rc.id) ?: existingChat
                        if (ts <= fresh.timestamp) return@mapNotNull null
                        return@mapNotNull fresh.copy(
                            message = lastMsg.content,
                            time = com.example.ui.formatTimeSafe(lastMsg.created_at),
                            timestamp = ts,
                            isMine = lastMsg.sender_id == currentUserId,
                            isReadReceipt = false,
                            lastMediaType = lastMsg.message_type,
                            lastMediaUrl = lastMsg.media_url,
                            lastThumbnailUrl = lastMsg.thumbnail_url
                        )
                    }

                    val otherUserId = allMembers.find { it.chat_id == rc.id && it.user_id != currentUserId }?.user_id
                    val otherProfile = profiles[otherUserId]
                    // إذا فشل جلب الأعضاء/البروفايلات نحتفظ بالاسم/الصورة/المعرّف المحفوظين بدل "Chat xxxx"
                    val realName = if (isGroupOrChannel) (rc.title ?: existingChat?.name ?: "Chat ${rc.id.take(4)}")
                                    else (otherProfile?.fullName ?: otherProfile?.username ?: existingChat?.name ?: "Chat ${rc.id.take(4)}")
                    val avatar = if (isGroupOrChannel) rc.avatar_url
                                 else if (otherProfile != null) otherProfile.avatarUrl
                                 else existingChat?.avatarUrl
                    val msgText = lastMsg?.content ?: ""
                    val timeStr = if (lastMsg != null) com.example.ui.formatTimeSafe(lastMsg.created_at) else "Now"
                    val timestamp = if (lastMsg != null) com.example.ui.parseTimestampSafe(lastMsg.created_at) else 0L

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
                        isOnline = otherProfile?.isOnlineNow ?: existingChat?.isOnline ?: false,
                        participantIds = if (otherUserId != null) otherUserId else (existingChat?.participantIds ?: "[]"),
                        draft = existingChat?.draft ?: "",
                        timestamp = timestamp,
                        unreadCount = existingChat?.unreadCount ?: 0,
                        isBot = existingChat?.isBot ?: false,
                        isVerified = if (rc.id == com.example.ui.OFFICIAL_CHANNEL_ID) true
                            else (otherProfile?.verifyType?.isVerified ?: existingChat?.isVerified ?: false),
                        verifiedType = if (rc.id == com.example.ui.OFFICIAL_CHANNEL_ID) com.example.ui.VerifyType.RED.raw
                            else (otherProfile?.verifyType?.raw ?: existingChat?.verifiedType ?: "none"),
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
                        isDefaultAvatar = existingChat?.isDefaultAvatar ?: false,
                        lastMediaType = if (lastMsg != null) lastMsg.message_type else existingChat?.lastMediaType,
                        lastMediaUrl = if (lastMsg != null) lastMsg.media_url else existingChat?.lastMediaUrl,
                        lastThumbnailUrl = if (lastMsg != null) lastMsg.thumbnail_url else existingChat?.lastThumbnailUrl
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
