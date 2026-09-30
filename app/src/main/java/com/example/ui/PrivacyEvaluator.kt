package com.example.ui

import com.example.supabase
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.Serializable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

object BlockManager {
    suspend fun getBlockedByMe(myId: String): List<String> {
        return try {
            supabase.postgrest["blocked_users"]
                .select(Columns.list("blocked_id")) {
                    filter { eq("blocker_id", myId) }
                }.decodeList<BlockedUserRow>().map { it.blocked_id }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getWhoBlockedMe(myId: String): List<String> {
        return try {
            supabase.postgrest["blocked_users"]
                .select(Columns.list("blocker_id")) {
                    filter { eq("blocked_id", myId) }
                }.decodeList<BlockedUserRow>().map { it.blocker_id }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun blockUser(myId: String, blockedId: String): Boolean {
        return try {
            supabase.postgrest["blocked_users"].insert(
                BlockedUserRow(blocker_id = myId, blocked_id = blockedId)
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun unblockUser(myId: String, blockedId: String): Boolean {
        return try {
            supabase.postgrest["blocked_users"].delete {
                filter { 
                    eq("blocker_id", myId)
                    eq("blocked_id", blockedId)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
