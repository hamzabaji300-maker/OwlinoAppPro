package com.example.ui

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.auth.auth
import com.example.supabase
import android.util.Log

object SyncCoordinator {
    private var isStarted = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start(context: Context) {
        if (isStarted) return
        isStarted = true
        
        scope.launch {
            try {
                if (supabase.realtime.status.value.name != "CONNECTED") {
                    supabase.realtime.connect()
                }
            } catch (e: Exception) {
                Log.e("SyncCoordinator", "Realtime connect error", e)
            }
            setupChannels(context)
        }
    }
    
    private var refreshJob: Job? = null

    /** Trailing debounce: bursts of realtime events produce ONE refresh after they settle. */
    private fun debounceRefresh(context: Context) {
        refreshJob?.cancel()
        refreshJob = scope.launch {
            delay(500)
            val myId = try { supabase.auth.currentUserOrNull()?.id } catch (e: Exception) { null } ?: return@launch
            val db = com.example.data.DatabaseProvider.getDatabase(context)
            try {
                BlockManager.refresh(context, myId, db.chatDao())
                ChatStateManager.refresh(myId, db.chatDao())
                ChatLifecycleManager.refreshLifecycle(myId, db.chatDao(), context)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("SyncCoordinator", "Refresh error", e)
            }
        }
    }

    private suspend fun subscribeWithReconnect(channel: RealtimeChannel, context: Context, onEvent: suspend () -> Unit) {
        try {
            channel.subscribe()
            channel.status.collect { status ->
                if (status.name == "SUBSCRIBED") {
                    debounceRefresh(context)
                }
            }
        } catch (e: Exception) {
            Log.e("SyncCoordinator", "Channel error", e)
        }
    }

    private fun setupChannels(context: Context) {
        try {
            val chatStateChannel = supabase.realtime.channel("chat_state_rt_sync")
            listOf("muted_chats", "pinned_chats", "favorite_chats", "trashed_chats", "cleared_chats", "archived_chats").forEach { tableName ->
                val flowIns = chatStateChannel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") { table = tableName }
                val flowDel = chatStateChannel.postgresChangeFlow<PostgresAction.Delete>(schema = "public") { table = tableName }
                scope.launch { flowIns.collect { debounceRefresh(context) } }
                scope.launch { flowDel.collect { debounceRefresh(context) } }
            }
            scope.launch { subscribeWithReconnect(chatStateChannel, context) {} }
            
            val blockChannel = supabase.realtime.channel("blocked_users_rt_sync")
            val blockedInsFlow = blockChannel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") { table = "blocked_users" }
            val blockedDelFlow = blockChannel.postgresChangeFlow<PostgresAction.Delete>(schema = "public") { table = "blocked_users" }
            scope.launch { blockedInsFlow.collect { debounceRefresh(context) } }
            scope.launch { blockedDelFlow.collect { debounceRefresh(context) } }
            scope.launch { subscribeWithReconnect(blockChannel, context) {} }

            val pinnedMsgsChannel = supabase.realtime.channel("pinned_msgs_rt_sync")
            val pinnedInsFlow = pinnedMsgsChannel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") { table = "pinned_messages" }
            val pinnedDelFlow = pinnedMsgsChannel.postgresChangeFlow<PostgresAction.Delete>(schema = "public") { table = "pinned_messages" }
            scope.launch {
                pinnedInsFlow.collect { action ->
                    try {
                        val chatId = action.record["chat_id"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else it.toString().replace("\"", "") }
                        val myId = supabase.auth.currentUserOrNull()?.id
                        if (chatId != null && myId != null) {
                            PinnedMessagesManager.load(chatId, myId)
                        }
                    } catch (e: Exception) { Log.e("SyncCoordinator", "Pinned ins err", e) }
                }
            }
            scope.launch {
                pinnedDelFlow.collect { action ->
                    try {
                        val chatId = action.oldRecord["chat_id"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else it.toString().replace("\"", "") }
                        val myId = supabase.auth.currentUserOrNull()?.id
                        if (chatId != null && myId != null) {
                            PinnedMessagesManager.load(chatId, myId)
                        }
                    } catch (e: Exception) { Log.e("SyncCoordinator", "Pinned del err", e) }
                }
            }
            scope.launch { subscribeWithReconnect(pinnedMsgsChannel, context) {} }

            val msgStateChannel = supabase.realtime.channel("messages_revive_sync")
            val msgInsFlow = msgStateChannel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") { table = "messages" }
            scope.launch {
                msgInsFlow.collect { action ->
                    try {
                        val chatId = action.record["chat_id"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else it.toString().replace("\"", "") }
                        val myId = supabase.auth.currentUserOrNull()?.id
                        if (chatId != null && myId != null) {
                            val db = com.example.data.DatabaseProvider.getDatabase(context)
                            val chat = db.chatDao().getChatById(chatId)
                            if (chat == null) {
                                supabase.postgrest["trashed_chats"].delete {
                                    filter {
                                        eq("user_id", myId)
                                        eq("chat_id", chatId)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) { Log.e("SyncCoordinator", "Revive msg err", e) }
                }
            }
            scope.launch { subscribeWithReconnect(msgStateChannel, context) {} }
        } catch (e: Exception) {
            Log.e("SyncCoordinator", "setupChannels err", e)
        }
    }
}
