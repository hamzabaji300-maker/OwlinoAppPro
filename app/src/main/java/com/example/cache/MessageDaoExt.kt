package com.example.cache

/**
 * كتابة غير مُتلِفة: REPLACE يمسح الأعمدة المحلية (مدة الصوت، حجم الملف، معاينة الرابط، التفاعلات،
 * حالة READ) إذا كان القادم من الشبكة لا يحملها. هنا نملأ الناقص من الصف الموجود قبل الكتابة.
 */
private fun CachedMessage.mergedWith(old: CachedMessage?): CachedMessage {
    if (old == null) return this
    return copy(
        status = if (old.status == "READ" && status == "SENT") "READ" else status,
        thumbnail_url = thumbnail_url ?: old.thumbnail_url,
        media_aspect_ratio = media_aspect_ratio ?: old.media_aspect_ratio,
        media_group_id = media_group_id ?: old.media_group_id,
        edited_at = edited_at ?: old.edited_at,
        file_size = file_size ?: old.file_size,
        duration_ms = duration_ms ?: old.duration_ms,
        link_url = link_url ?: old.link_url,
        link_title = link_title ?: old.link_title,
        link_description = link_description ?: old.link_description,
        link_image_url = link_image_url ?: old.link_image_url,
        reactions = reactions ?: old.reactions,
        reply_markup = reply_markup ?: old.reply_markup
    )
}

suspend fun MessageDao.insertMerged(message: CachedMessage) {
    insertCachedMessage(message.mergedWith(getCachedMessageById(message.id)))
}

suspend fun MessageDao.insertMergedAll(messages: List<CachedMessage>) {
    if (messages.isEmpty()) return
    val oldById = HashMap<String, CachedMessage>()
    messages.map { it.id }.chunked(500).forEach { chunk ->
        getCachedMessagesByIds(chunk).forEach { oldById[it.id] = it }
    }
    insertCachedMessages(messages.map { it.mergedWith(oldById[it.id]) })
}
