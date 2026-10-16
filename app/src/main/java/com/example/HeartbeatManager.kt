package com.example

import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.*
import java.time.Instant
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State

object HeartbeatManager {
    private var heartbeatJob: Job? = null
    private var authStatusJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 1. Persistent variable on the Object level
    private var persistentUserId: String? = null

    // Debug state for visual logging
    private val logList = java.util.LinkedList<String>()
    private val _debugLog = mutableStateOf("Init")
    val debugLog: State<String> = _debugLog

    private fun log(message: String) {
        val time = java.time.LocalTime.now().withNano(0).toString()
        val fullMsg = "[$time] $message"
        synchronized(logList) {
            logList.add(fullMsg)
            if (logList.size > 8) {
                logList.removeFirst()
            }
            _debugLog.value = logList.joinToString("\n")
        }
    }

    private fun logError(message: String, e: Throwable? = null) {
        val time = java.time.LocalTime.now().withNano(0).toString()
        val fullMsg = "[$time] ERROR: $message"
        synchronized(logList) {
            logList.add(fullMsg)
            if (logList.size > 8) {
                logList.removeFirst()
            }
            _debugLog.value = logList.joinToString("\n")
        }
    }

    fun start() {
        log("start() called. Current job active? ${heartbeatJob?.isActive}")
        
        // 2. Listen to AuthState changes once to update persistentUserId
        if (authStatusJob == null) {
            authStatusJob = scope.launch {
                com.example.supabase.auth.sessionStatus.collect { status ->
                    when (status) {
                        is SessionStatus.Authenticated -> {
                            persistentUserId = status.session.user?.id
                            log("AuthStatus: Authenticated. persistentUserId = $persistentUserId")
                        }
                        is SessionStatus.NotAuthenticated -> {
                            persistentUserId = null
                            log("AuthStatus: NotAuthenticated. persistentUserId cleared.")
                        }
                        else -> {
                            log("AuthStatus: ${status::class.simpleName}")
                        }
                    }
                }
            }
        }

        if (heartbeatJob?.isActive == true) return
        
        heartbeatJob = scope.launch {
            log("coroutine launched")
            log("Job started. Entering while(isActive) loop.")
            
            while (isActive) {
                try {
                    log("BEFORE freshId fetch")
                    val freshId = com.example.supabase.auth.currentSessionOrNull()?.user?.id
                    log("AFTER freshId fetch: $freshId")
                    
                    // Extra safety layer: update persistentUserId if freshId is valid
                    if (freshId != null) {
                        persistentUserId = freshId
                    }
                    
                    // 3. Print both values clearly
                    log("TICK. freshId: $freshId, persistent: $persistentUserId")
                    
                    if (persistentUserId != null) {
                        // Add timeout to prevent hanging the loop if network is slow/stuck
                        withTimeout(10_000) { 
                            com.example.supabase.postgrest.rpc("update_last_seen")
                        }
                        log("Update SUCCESS at ${java.time.LocalTime.now().withNano(0)}")
                    } else {
                        log("TICK skipped. Both IDs are null.")
                    }
                } catch (e: TimeoutCancellationException) {
                    logError("Update TIMEOUT (took >10s).")
                } catch (e: CancellationException) {
                    log("Job was cancelled cleanly.")
                    throw e // Re-throw to allow cancellation to complete
                } catch (e: Exception) {
                    logError("Failed to update last_seen_at", e)
                }
                
                try {
                    delay(30_000)
                } catch (e: CancellationException) {
                    log("Delay interrupted by cancellation.")
                    throw e
                }
            }
            log("Exited while(isActive) loop normally.")
        }
    }

    fun stop() {
        log("stop() called. Cancelling heartbeat job.")
        heartbeatJob?.cancel()
        heartbeatJob = null
        // Note: We do NOT cancel authStatusJob so it keeps listening even in background
    }

    fun updateImmediateAndStop() {
        log("updateImmediateAndStop() called. Updating last_seen_at immediately before stopping.")
        
        val currentUserId = persistentUserId
        
        if (currentUserId != null) {
            scope.launch {
                try {
                    withTimeout(5_000) { 
                        com.example.supabase.postgrest.rpc("update_last_seen")
                    }
                    log("Immediate update SUCCESS on stop at ${java.time.LocalTime.now().withNano(0)}")
                } catch (e: Exception) {
                    logError("Immediate update FAILED on stop", e)
                }
            }
        } else {
            log("Immediate update skipped. currentUserId is null.")
        }
        
        stop()
    }
}
