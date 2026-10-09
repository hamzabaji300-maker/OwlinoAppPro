package com.example.cache
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_messages")
data class CachedMessage(
    @PrimaryKey val id: String,
    val chat_id: String,
    val sender_id: String,
    val content: String,
    val created_at: String,
    val status: String,
    val message_type: String? = null,
    val media_url: String? = null,
    val thumbnail_url: String? = null,
    val reply_to_id: String? = null,
    val media_aspect_ratio: Float? = null,
    val media_group_id: String? = null,
    val edited_at: String? = null,
    val file_size: Long? = null,
    val duration_ms: Long? = null,
    val link_url: String? = null,
    val link_title: String? = null,
    val link_description: String? = null,
    val link_image_url: String? = null,
    // رموز التفاعل مفصولة بـ \u001F (إضافة محلية فقط - Migration 6->7)
    val reactions: String? = null,
    val reply_markup: String? = null
)
