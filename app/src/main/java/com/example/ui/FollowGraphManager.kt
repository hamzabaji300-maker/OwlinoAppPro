package com.example.ui

import android.content.Context
import android.util.Log
import com.example.supabase
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

@Serializable
private data class FgFollowerRow(val follower_id: String)

@Serializable
private data class FgFollowingRow(val following_id: String)

/**
 * Keeps followers / following (and their profiles) loaded in the background from app start,
 * cached on disk, so the followers screen opens instantly with no reload flicker.
 * Also tracks "new followers" (not yet seen) so the UI can show a dot.
 */
object FollowGraphManager {
    private const val PREFS = "follow_graph"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; isLenient = true }

    private val _followerIds = MutableStateFlow<Set<String>>(emptySet())
    val followerIds: StateFlow<Set<String>> = _followerIds

    private val _followingIds = MutableStateFlow<Set<String>>(emptySet())
    val followingIds: StateFlow<Set<String>> = _followingIds

    private val _profiles = MutableStateFlow<Map<String, Profile>>(emptyMap())
    val profiles: StateFlow<Map<String, Profile>> = _profiles

    private val _newFollowerIds = MutableStateFlow<Set<String>>(emptySet())
    /** Followers the user has not looked at yet (drives the dot). */
    val newFollowerIds: StateFlow<Set<String>> = _newFollowerIds

    private val _loaded = MutableStateFlow(false)
    /** True once data exists (from cache or from the server). */
    val loaded: StateFlow<Boolean> = _loaded

    private var appCtx: Context? = null
    private var seen: Set<String> = emptySet()
    private var seenInitialized = false

    private fun prefs() = appCtx?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun loadCache(context: Context) {
        appCtx = context.applicationContext
        val p = prefs() ?: return
        try {
            _followerIds.value = json.decodeFromString(SetSerializer(String.serializer()), p.getString("followers", "[]") ?: "[]")
            _followingIds.value = json.decodeFromString(SetSerializer(String.serializer()), p.getString("following", "[]") ?: "[]")
            val list = json.decodeFromString(ListSerializer(Profile.serializer()), p.getString("profiles", "[]") ?: "[]")
            _profiles.value = list.associateBy { it.id }
            seen = json.decodeFromString(SetSerializer(String.serializer()), p.getString("seen", "[]") ?: "[]")
            seenInitialized = p.getBoolean("seen_init", false)
            _newFollowerIds.value = if (seenInitialized) _followerIds.value - seen else emptySet()
            _loaded.value = _followerIds.value.isNotEmpty() || _followingIds.value.isNotEmpty()
        } catch (e: Exception) {
            Log.e("FollowGraph", "cache load failed", e)
        }
    }

    private fun saveCache() {
        val p = prefs() ?: return
        try {
            p.edit()
                .putString("followers", json.encodeToString(SetSerializer(String.serializer()), _followerIds.value))
                .putString("following", json.encodeToString(SetSerializer(String.serializer()), _followingIds.value))
                .putString("profiles", json.encodeToString(ListSerializer(Profile.serializer()), _profiles.value.values.toList()))
                .putString("seen", json.encodeToString(SetSerializer(String.serializer()), seen))
                .putBoolean("seen_init", seenInitialized)
                .apply()
        } catch (e: Exception) {
            Log.e("FollowGraph", "cache save failed", e)
        }
    }

    fun clear() {
        _followerIds.value = emptySet()
        _followingIds.value = emptySet()
        _profiles.value = emptyMap()
        _newFollowerIds.value = emptySet()
        _loaded.value = false
        seen = emptySet()
        seenInitialized = false
        prefs()?.edit()?.clear()?.apply()
    }

    /** Silent background refresh: never blanks the lists, a failed fetch keeps what we have. */
    suspend fun refresh(myId: String) {
        try {
            val followers = supabase.postgrest["followers"].select(Columns.list("follower_id")) {
                filter { eq("following_id", myId) }
            }.decodeList<FgFollowerRow>().map { it.follower_id }.toSet()

            val following = supabase.postgrest["followers"].select(Columns.list("following_id")) {
                filter { eq("follower_id", myId) }
            }.decodeList<FgFollowingRow>().map { it.following_id }.toSet()

            val needed = (followers + following)
            val have = _profiles.value
            val missing = needed.filter { it !in have }
            val fetched = mutableMapOf<String, Profile>()
            missing.chunked(100).forEach { chunk ->
                supabase.postgrest["profiles"].select { filter { isIn("id", chunk) } }
                    .decodeList<Profile>().forEach { fetched[it.id] = it }
            }
            // Refresh profile data of people already cached too (names/avatars change), but only a small batch
            val stale = needed.filter { it in have }.take(100)
            if (stale.isNotEmpty()) {
                supabase.postgrest["profiles"].select { filter { isIn("id", stale) } }
                    .decodeList<Profile>().forEach { fetched[it.id] = it }
            }

            _profiles.value = (have + fetched).filterKeys { it in needed }
            _followingIds.value = following
            _followerIds.value = followers

            if (!seenInitialized) {
                // First run on this device/account: existing followers are not "new"
                seen = followers
                seenInitialized = true
            }
            _newFollowerIds.value = followers - seen
            _loaded.value = true
            saveCache()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("FollowGraph", "refresh failed", e)
        }
    }

    /** Called when the user has looked at the followers list. */
    fun markFollowersSeen() {
        seen = _followerIds.value
        seenInitialized = true
        _newFollowerIds.value = emptySet()
        saveCache()
    }

    fun setFollowing(userId: String, following: Boolean) {
        _followingIds.value = if (following) _followingIds.value + userId else _followingIds.value - userId
        saveCache()
    }

    fun removeFollower(userId: String) {
        _followerIds.value = _followerIds.value - userId
        _newFollowerIds.value = _newFollowerIds.value - userId
        saveCache()
    }
}
