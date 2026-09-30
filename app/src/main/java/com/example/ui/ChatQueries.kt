package com.example.ui

import kotlinx.serialization.Serializable

@Serializable
data class ChatInsert(
    val id: String? = null,
    val type: String,
    val created_by: String? = null,
    val title: String? = null,
    val avatar_url: String? = null
)

@Serializable
data class ChatRow(
    val id: String,
    val type: String? = null,
    val created_by: String? = null,
    val title: String? = null,
    val avatar_url: String? = null
)

@Serializable
data class ChatMemberRow(
    val chat_id: String,
    val user_id: String? = null,
    val role: String? = null
)

@Serializable
data class MessageInsert(
    val chat_id: String,
    val sender_id: String,
    val content: String,
    val message_type: String? = null,
    val media_url: String? = null,
    val thumbnail_url: String? = null,
    val reply_to_id: String? = null,
    val media_aspect_ratio: Float? = null,
    val media_group_id: String? = null
)

@Serializable
data class MessageRow(
    val id: String,
    val chat_id: String,
    val sender_id: String,
    val content: String,
    val message_type: String? = null,
    val media_url: String? = null,
    val thumbnail_url: String? = null,
    val reply_to_id: String? = null,
    val created_at: String,
    val media_aspect_ratio: Float? = null,
    val media_group_id: String? = null
)

@Serializable
data class ReactionInsert(
    val message_id: String,
    val user_id: String,
    val emoji: String
)

@Serializable
data class ReactionRow(
    val id: String,
    val message_id: String,
    val user_id: String,
    val emoji: String,
    val created_at: String? = null
)
