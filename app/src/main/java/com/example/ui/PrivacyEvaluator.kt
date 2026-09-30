package com.example.ui

import com.example.supabase
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.Serializable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

@Serializable
data class BlockedUserRow(
    val blocker_id: String,
    val blocked_id: String,
    val created_at: String? = null
)

object PrivacyEvaluator {

    // Checks if `myProfile` can see `theirProfile`'s presence (last seen / online)
    fun canSeePresence(
        myRule: String,
        theirRule: String,
        amIBlockedByThem: Boolean,
        haveIBlockedThem: Boolean
    ): Boolean {
        if (amIBlockedByThem || haveIBlockedThem) return false
        if (theirRule == "nobody") return false
        if (myRule == "nobody") return false
        return true
    }

    fun canSeeProfilePhoto(
        theirPhotoRule: String,
        amIBlockedByThem: Boolean,
        haveIBlockedThem: Boolean
    ): Boolean {
        if (amIBlockedByThem || haveIBlockedThem) return false
        if (theirPhotoRule == "nobody") return false
        return true
    }
}

/**
 * BlockManager — single source of truth for block state.
 *
 * Exposes:
 *   - [blockedByMe]  : StateFlow<Set<String>>  — IDs the current user has blocked
 *   - [blockedMe]    : StateFlow<Set<String>>  — IDs that have blocked the current user
 *
 * Writes follow Room-first / Supabase-second with automatic rollback:
 *   - [block]   : writes Room -> updates flows -> writes Supabase; rolls back if Supabase fails
 *   - [unblock] : same strategy in reverse
 *   - [refresh] : loads both lists from Supabase and syncs Room
 *
 * Callers must NOT call chatDao.updateBlockState or postgrest["blocked_users"] directly.
 */
object BlockManager {

    // ---- public state ----

    private val _blockedByMe = MutableStateFlow<Set<String>>(emptySet())
    val blockedByMe: StateFlow<Set<String>> = _blockedByMe.asStateFlow()

    private val _blockedMe = MutableStateFlow<Set<String>>(emptySet())
    val blockedMe: StateFlow<Set<String>> = _blockedMe.asStateFlow()

    // ---- legacy read accessors (delegate to flow snapshot — no Supabase call) ----

    suspend fun getBlockedByMe(myId: String): List<String> = _blockedByMe.value.toList()

    suspend fun getWhoBlockedMe(myId: String): List<String> = _blockedMe.value.toList()

    // ---- core write operations ----

    /**
     * Blocks [targetUserId] on behalf of [myId].
     * 1. Updates Room for every chat where [targetUserId] is a participant.
     * 2. Updates [_blockedByMe] flow immediately.
     * 3. Writes to Supabase blocked_users table.
     * 4. On Supabase failure: rolls back Room + flow, returns false.
     */
    suspend fun block(
        myId: String,
        targetUserId: String,
        chatDao: com.example.data.ChatDao
    ): Boolean {
        updateRoomBlockState(chatDao, targetUserId, blocked = true)
        _blockedByMe.value = _blockedByMe.value + targetUserId
        return try {
            supabase.postgrest["blocked_users"].insert(
                BlockedUserRow(blocker_id = myId, blocked_id = targetUserId)
            )
            true
        } catch (e: Exception) {
            // Rollback
            _blockedByMe.value = _blockedByMe.value - targetUserId
            updateRoomBlockState(chatDao, targetUserId, blocked = false)
            false
        }
    }

    /**
     * Unblocks [targetUserId] on behalf of [myId].
     * Same Room-first strategy as [block].
     */
    suspend fun unblock(
        myId: String,
        targetUserId: String,
        chatDao: com.example.data.ChatDao
    ): Boolean {
        updateRoomBlockState(chatDao, targetUserId, blocked = false)
        _blockedByMe.value = _blockedByMe.value - targetUserId
        return try {
            supabase.postgrest["blocked_users"].delete {
                filter {
                    eq("blocker_id", myId)
                    eq("blocked_id", targetUserId)
                }
            }
            true
        } catch (e: Exception) {
            // Rollback
            _blockedByMe.value = _blockedByMe.value + targetUserId
            updateRoomBlockState(chatDao, targetUserId, blocked = true)
            false
        }
    }

    /**
     * Refreshes both block lists from Supabase and syncs Room accordingly.
     * Called on login and from the app-level realtime listener.
     */
    suspend fun refresh(
        myId: String,
        chatDao: com.example.data.ChatDao
    ) {
        try {
            val byMe = supabase.postgrest["blocked_users"]
                .select {
                    filter { eq("blocker_id", myId) }
                }.decodeList<BlockedUserRow>().map { it.blocked_id }.toSet()

            val byThem = supabase.postgrest["blocked_users"]
                .select {
                    filter { eq("blocked_id", myId) }
                }.decodeList<BlockedUserRow>().map { it.blocker_id }.toSet()

            _blockedByMe.value = byMe
            _blockedMe.value = byThem

            // Sync Room
            val allChats = chatDao.getAllChats().first()
            allChats.forEach { chat ->
                val otherId = try {
                    kotlinx.serialization.json.Json
                        .decodeFromString<List<String>>(chat.participantIds)
                        .firstOrNull { it != myId }
                        ?: kotlinx.serialization.json.Json
                            .decodeFromString<List<String>>(chat.participantIds)
                            .firstOrNull()
                } catch (e: Exception) {
                    if (chat.participantIds.isNotEmpty() && chat.participantIds != "[]")
                        chat.participantIds else null
                }
                if (otherId != null) {
                    val shouldBeBlocked = otherId in byMe
                    if (chat.isBlocked != shouldBeBlocked) {
                        chatDao.updateBlockState(chat.id, shouldBeBlocked)
                    }
                }
            }
        } catch (e: Exception) {
            // Network unavailable — leave existing state
        }
    }

    // ---- legacy suspend helpers (kept for callers that don't have chatDao handy) ----

    @Deprecated("Use block(myId, targetUserId, chatDao) instead")
    suspend fun blockUser(myId: String, blockedId: String): Boolean {
        return try {
            supabase.postgrest["blocked_users"].insert(
                BlockedUserRow(blocker_id = myId, blocked_id = blockedId)
            )
            _blockedByMe.value = _blockedByMe.value + blockedId
            true
        } catch (e: Exception) {
            false
        }
    }

    @Deprecated("Use unblock(myId, targetUserId, chatDao) instead")
    suspend fun unblockUser(myId: String, blockedId: String): Boolean {
        return try {
            supabase.postgrest["blocked_users"].delete {
                filter {
                    eq("blocker_id", myId)
                    eq("blocked_id", blockedId)
                }
            }
            _blockedByMe.value = _blockedByMe.value - blockedId
            true
        } catch (e: Exception) {
            false
        }
    }

    // ---- internal helpers ----

    private suspend fun updateRoomBlockState(
        chatDao: com.example.data.ChatDao,
        targetUserId: String,
        blocked: Boolean
    ) {
        try {
            val allChats = chatDao.getAllChats().first()
            allChats.forEach { chat ->
                val participants = try {
                    kotlinx.serialization.json.Json
                        .decodeFromString<List<String>>(chat.participantIds)
                } catch (e: Exception) {
                    if (chat.participantIds.isNotEmpty() && chat.participantIds != "[]")
                        listOf(chat.participantIds) else emptyList()
                }
                if (targetUserId in participants) {
                    chatDao.updateBlockState(chat.id, blocked)
                }
            }
        } catch (e: Exception) {
            // Ignore Room errors — caller may surface them
        }
    }

    fun clear() {
        _blockedByMe.value = emptySet()
        _blockedMe.value = emptySet()
    }
}
