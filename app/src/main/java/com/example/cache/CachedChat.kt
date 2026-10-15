package com.example.cache
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_chats")
data class CachedChat(
    @PrimaryKey val chat_id: String,
    val name: String,
    val last_message: String,
    val time_str: String,
    val timestamp: Long,
    val unread_count: Int
)
