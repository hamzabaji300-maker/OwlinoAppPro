package com.example.ui

import java.util.UUID

// Global offset to sync server time with local device time inconsistencies
var globalServerTimeOffsetSeconds: Long = 0L



fun formatTimeSafe(timestamp: String?): String {
    if (timestamp.isNullOrEmpty()) return "00:00"
    if (!timestamp.contains("-") || timestamp.length <= 10) return timestamp

    return try {
        var cleanDt = timestamp.replace(" ", "T")
        // If it lacks seconds (e.g., T13:29 or T13:29+00), add :00 for seconds
        if (cleanDt.matches(Regex(".*T\\d{2}:\\d{2}$"))) {
            cleanDt += ":00"
        } else if (cleanDt.matches(Regex(".*T\\d{2}:\\d{2}[+-]\\d{2}$"))) {
            // e.g. T13:29+00 -> T13:29:00+00
            cleanDt = cleanDt.substring(0, cleanDt.length - 3) + ":00" + cleanDt.substring(cleanDt.length - 3)
        }
        
        // If it ends with just +XX or -XX (no minutes offset), add :00 to the offset
        if (cleanDt.matches(Regex(".*[+-]\\d{2}$"))) {
            cleanDt += ":00"
        }

        val instant = if (cleanDt.contains("Z") || cleanDt.indexOf('+', 10) != -1 || cleanDt.indexOf('-', 10) != -1) {
            java.time.OffsetDateTime.parse(cleanDt).toInstant()
        } else {
            java.time.OffsetDateTime.parse(cleanDt + "Z").toInstant()
        }
            
        // Use the device's local timezone to display the time correctly for the current user
        val targetZone = java.time.ZoneId.systemDefault()
        
        val outFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm").withZone(targetZone)
        outFormatter.format(instant)
    } catch (e: Exception) {
        "00:00"
    }
}

fun parseTimestampSafe(timestamp: String?): Long {
    if (timestamp.isNullOrEmpty()) return System.currentTimeMillis()
    if (!timestamp.contains("-") || timestamp.length <= 10) return System.currentTimeMillis() 

    return try {
        var cleanDt = timestamp.replace(" ", "T")
        if (cleanDt.matches(Regex(".*T\\d{2}:\\d{2}$"))) {
            cleanDt += ":00"
        } else if (cleanDt.matches(Regex(".*T\\d{2}:\\d{2}[+-]\\d{2}$"))) {
            cleanDt = cleanDt.substring(0, cleanDt.length - 3) + ":00" + cleanDt.substring(cleanDt.length - 3)
        }
        
        if (cleanDt.matches(Regex(".*[+-]\\d{2}$"))) {
            cleanDt += ":00"
        }

        val instant = if (cleanDt.contains("Z") || cleanDt.indexOf('+', 10) != -1 || cleanDt.indexOf('-', 10) != -1) {
            java.time.OffsetDateTime.parse(cleanDt).toInstant()
        } else {
            java.time.OffsetDateTime.parse(cleanDt + "Z").toInstant()
        }
            
        // Return the raw timestamp milliseconds for correct absolute sorting
        instant.toEpochMilli()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
}
data class User(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val avatarUrl: String? = null,
    val isOnline: Boolean = false,
    val lastSeen: Long? = null
)

enum class AttachmentType {
    IMAGE, VIDEO, DOCUMENT, AUDIO, VOICE
}

data class Attachment(
    val id: String = UUID.randomUUID().toString(),
    val messageId: String, // Foreign key
    val type: AttachmentType,
    val url: String, // Local URI or remote URL (الحجم الأصلي الكامل)
    val thumbnailUrl: String? = null, // نسخة صغيرة مضغوطة للعرض السريع داخل الفقاعة
    val fileName: String? = null,
    val fileSize: Long? = null,
    val mimeType: String? = null,
    val durationMs: Long? = null, // For audio/video
    val aspectRatio: Float? = null
)

enum class MessageStatus {
    SENDING, SENT, DELIVERED, READ, FAILED
}

// معرّف القناة الرسمية الحقيقية (موجودة في Supabase — جدول chats بنوع channel) ويشترك فيها كل المستخدمين تلقائياً
const val OFFICIAL_CHANNEL_ID = "00000000-0000-4000-8000-0000000000a1"
// اسم قديم محفوظ للتوافق: كان يشير للقناة الوهمية، والآن يشير للقناة الرسمية الحقيقية
const val FAKE_CHANNEL_ID = OFFICIAL_CHANNEL_ID

@androidx.compose.runtime.Stable
data class ChannelReaction(
    val emoji: String,
    val count: Int
)

fun formatReactionCount(n: Int): String {
    if (n < 1000) return n.toString()
    val thousands = n / 1000.0
    return if (thousands == Math.floor(thousands)) {
        "${thousands.toInt()}K"
    } else {
        String.format("%.1fK", thousands)
    }
}

@androidx.compose.runtime.Stable
data class MessageModel(
    val id: String = UUID.randomUUID().toString(),
    val chatId: String = "", // Foreign key
    val senderId: String = "", // Foreign key
    val senderName: String = "", // for group chat UI
    val text: String = "",
    val time: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isMine: Boolean,
    val isPinned: Boolean = false,
    val isEdited: Boolean = false,
    val replyToId: String? = null, // Foreign key to another MessageModel
    val replyTo: MessageModel? = null, // In-memory reference for UI
    val reactions: List<String> = emptyList(), // Can be stored as JSON or separate table
    val isSaved: Boolean = false,
    val attachments: List<Attachment> = emptyList(), // One-to-many relationship
    val status: MessageStatus = MessageStatus.SENT,
    val createdAtExact: String? = null,
    val mediaGroupId: String? = null,
    /** Telegram reply_markup (JSON): inline_keyboard / keyboard / remove_keyboard */
    val replyMarkup: String? = null,
    // --- Channel post extras (broadcast posts: views, forwards, per-emoji reaction pills) ---
    val viewsLabel: String? = null,
    val forwardsLabel: String? = null,
    val totalInteractionsLabel: String? = null,
    val channelReactions: List<ChannelReaction> = emptyList()
)

@androidx.compose.runtime.Stable
@kotlinx.serialization.Serializable
data class ChatModel(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val subtitle: String? = null,
    val avatarUrl: String? = null,
    val message: String = "",
    val draft: String = "",
    val time: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isOnline: Boolean = false,
    val isTyping: Boolean = false,
    val unreadCount: Int = 0,
    val hasStar: Boolean = false,
    val isMuted: Boolean = false,
    val isBot: Boolean = false,
    val isChannel: Boolean = false,
    val isGroup: Boolean = false,
    val isVerified: Boolean = false,
    val verifiedType: String = "none",
    val hasSparkleBadge: Boolean = false,
    val isReadReceipt: Boolean = false,
    val isMine: Boolean = false,
    val isNotes: Boolean = false,
    val isDefaultAvatar: Boolean = false,
    val isFavorite: Boolean = false,
    val isBlocked: Boolean = false,
    val isArchived: Boolean = false,
    val lastMediaType: String? = null,
    val lastMediaUrl: String? = null,
    val lastThumbnailUrl: String? = null,
    val participantIds: List<String> = emptyList() // Foreign keys
) {
    /** نوع التوثيق الذي يُعرض فعلًا: القناة الرسمية دائمًا حمراء. */
    val effectiveVerifyType: VerifyType
        get() = if (id == OFFICIAL_CHANNEL_ID) VerifyType.RED else VerifyType.from(verifiedType, isVerified)
}

/** نفس منطق ChatModel.effectiveVerifyType لكن لكيان Room (ChatEntity) المستخدم في شاشة المحادثة. */
val com.example.data.ChatEntity.effectiveVerifyType: VerifyType
    get() = if (id == OFFICIAL_CHANNEL_ID) VerifyType.RED else VerifyType.from(verifiedType, isVerified)

fun formatRelativeTime(timestamp: String?): String {
    if (timestamp.isNullOrEmpty()) return "آخر ظهور غير معروف"
    return try {
        val millis = parseTimestampSafe(timestamp)
        val instant = java.time.Instant.ofEpochMilli(millis)
        val now = java.time.Instant.now()
        val duration = java.time.Duration.between(instant, now)
        val minutes = duration.toMinutes()
        val hours = duration.toHours()
        val days = duration.toDays()

        val timeStr = when {
            minutes < 1 -> "منذ لحظات"
            minutes == 1L -> "منذ دقيقة"
            minutes == 2L -> "منذ دقيقتين"
            minutes in 3..10 -> "منذ $minutes دقائق"
            minutes < 60 -> "منذ $minutes دقيقة"
            hours == 1L -> "منذ ساعة"
            hours == 2L -> "منذ ساعتين"
            hours in 3..10 -> "منذ $hours ساعات"
            hours < 24 -> "منذ $hours ساعة"
            days == 1L -> "منذ يوم"
            days == 2L -> "منذ يومين"
            days in 3..10 -> "منذ $days أيام"
            else -> "منذ $days يوم"
        }
        "آخر ظهور $timeStr"
    } catch (e: Exception) {
        "آخر ظهور غير معروف"
    }
}

@kotlinx.serialization.Serializable
data class ProfileLink(
    val id: String,
    val platform: String,
    val title: String,
    val url: String,
    val description: String = ""
)

