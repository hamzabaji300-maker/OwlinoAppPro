package com.example.ui
import kotlinx.serialization.Serializable

@Serializable
data class StoryViewRow(
    val id: String = "",
    val viewer_id: String,
    val story_id: String,
    val viewed_at: String? = null
)

@Serializable
data class StoryIdOnly(
    val story_id: String
)

@Serializable
data class StoryViewInsert(
    val viewer_id: String,
    val story_id: String,
    val viewed_at: String? = null
)
