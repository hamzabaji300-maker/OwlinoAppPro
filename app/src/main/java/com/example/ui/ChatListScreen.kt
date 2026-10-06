package com.example.ui
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.*
import androidx.compose.material.icons.filled.Home
import kotlinx.coroutines.flow.first
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.material.icons.automirrored.outlined.ExitToApp





import io.github.jan.supabase.realtime.realtime
import com.composables.icons.lucide.*
import io.github.jan.supabase.storage.storage
import com.composables.icons.lucide.Lucide
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import androidx.compose.runtime.DisposableEffect
import io.github.jan.supabase.realtime.broadcastFlow

import androidx.compose.animation.animateContentSize
import com.example.data.DatabaseProvider
import com.example.data.toEntity
import com.example.data.toModel
import io.github.jan.supabase.postgrest.query.Columns

import io.github.jan.supabase.postgrest.postgrest
import com.example.supabase
import androidx.compose.ui.platform.LocalContext

import androidx.compose.animation.animateColorAsState

import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.animation.core.FastOutSlowInEasing

import androidx.compose.animation.core.animateFloatAsState

import androidx.compose.foundation.interaction.collectIsPressedAsState

import io.github.jan.supabase.auth.auth

import kotlinx.coroutines.*


import androidx.compose.animation.*

import androidx.compose.animation.core.tween

import androidx.compose.animation.core.animateDpAsState

import androidx.compose.foundation.background

import androidx.compose.foundation.clickable

import androidx.compose.foundation.combinedClickable

import androidx.compose.foundation.interaction.MutableInteractionSource

import androidx.compose.material3.MaterialTheme

import androidx.compose.foundation.layout.*

import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.lazy.LazyRow

import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.lazy.LazyListState

import androidx.compose.foundation.lazy.rememberLazyListState

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.zIndex

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.automirrored.filled.Chat

import androidx.compose.material.icons.automirrored.outlined.Chat

import androidx.compose.material.icons.outlined.*

import androidx.compose.foundation.border

import androidx.compose.material.icons.filled.Star

import androidx.compose.material.icons.filled.Verified

import androidx.compose.material.icons.filled.PushPin


import androidx.compose.ui.text.style.TextAlign

import androidx.compose.foundation.BorderStroke

import androidx.compose.material.icons.outlined.VolumeOff

import androidx.compose.material.icons.outlined.SmartToy

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.ui.layout.onSizeChanged


import androidx.compose.ui.input.nestedscroll.NestedScrollConnection

import androidx.compose.ui.input.nestedscroll.NestedScrollSource

import androidx.compose.ui.input.nestedscroll.nestedScroll

import androidx.compose.ui.geometry.Offset

import androidx.compose.ui.platform.LocalDensity

import androidx.compose.ui.unit.IntOffset

import kotlin.math.roundToInt

import androidx.compose.runtime.mutableFloatStateOf


import androidx.compose.ui.Alignment
import com.example.ui.i18n.LocalTranslation

import androidx.compose.ui.Modifier
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

import androidx.compose.ui.draw.scale

import androidx.compose.ui.draw.rotate

import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.material.icons.rounded.Star

import androidx.compose.ui.layout.boundsInRoot

import androidx.compose.ui.draw.clip

import androidx.compose.ui.draw.clipToBounds

import androidx.compose.ui.draw.blur

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Warning

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.graphics.vector.ImageVector



import com.example.R

import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Notifications

import androidx.compose.material.icons.rounded.Search

import androidx.compose.material.icons.rounded.Notifications

import androidx.compose.material.icons.rounded.NotificationsNone

import androidx.compose.ui.text.buildAnnotatedString

import androidx.compose.ui.text.withStyle

import androidx.compose.ui.text.SpanStyle

import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Description

import androidx.compose.material.icons.filled.Check

import androidx.compose.material.icons.filled.Person

import androidx.compose.material.icons.filled.AutoAwesome

import androidx.compose.ui.graphics.Brush

import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp

import com.example.ui.theme.*

@Composable
private fun ChatListTypingIndicator() {
    val transition = rememberInfiniteTransition(label = "typing")
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("typing", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF3B82F6))
        Spacer(modifier = Modifier.width(2.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 4.dp)) {
            for (i in 0..2) {
                val yOffset by transition.animateFloat(
                    initialValue = 0f,
                    targetValue = -3f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(400, delayMillis = i * 150, easing = EaseInOut),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "dot"
                )
                Box(modifier = Modifier
                    .size(3.dp)
                    .offset(y = yOffset.dp)
                    .background(Color(0xFF3B82F6), CircleShape)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(onChatClick: (String, String, Boolean) -> Unit, onDiscoverUsers: () -> Unit = {}, onSettingsClick: () -> Unit = {}, onProfileSwipe: () -> Unit = {}, onUserProfileClick: (String) -> Unit = {}, onNotificationsClick: () -> Unit = {}, onEditProfileClick: () -> Unit = {}, onFollowersIconClick: () -> Unit = {}, onNewMessage: () -> Unit = {}, onSearchUserClick: (String) -> Unit = {}, onArchivedClick: () -> Unit = {}) {
        val __themeConfig = com.example.ui.LocalSettingsTheme.current
    val __theme = __themeConfig.theme
    val isDarkState = __theme.isDark
    val __bgColor = __theme.bgColor
    val __surfaceColor = __theme.surfaceColor
    val __textPrimary = SettingsColors.textPrimary
    val __textSecondary = SettingsColors.textSecondary
    val __dividerColor = __theme.dividerColor
    val __accent = __themeConfig.accent

    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val context = LocalContext.current
    val archiveSnackbarHost = remember { androidx.compose.material3.SnackbarHostState() }
    val trArch = com.example.ui.i18n.rememberExtraStrings()
    var showMediaFolderDialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1500)
        if (com.example.util.MediaStorage.shouldPrompt(context)) showMediaFolderDialog = true
    }
    if (showMediaFolderDialog) {
        AppConfirmDialog(
            title = trArch.mediaFolderTitle,
            message = trArch.mediaFolderBody,
            confirmText = trArch.mediaFolderAllow,
            cancelText = trArch.mediaFolderLater,
            destructive = false,
            onDismiss = {
                com.example.util.MediaStorage.markAsked(context)
                showMediaFolderDialog = false
            },
            onConfirm = {
                showMediaFolderDialog = false
                com.example.util.MediaStorage.requestPublicAccess(context)
            }
        )
    }
        var liveMyProfile by remember { mutableStateOf<Profile?>(com.example.ui.GlobalAppState.liveMyProfile) }
        
        // Update global whenever it changes
        androidx.compose.runtime.LaunchedEffect(liveMyProfile) {
            if (liveMyProfile != null) {
                com.example.ui.GlobalAppState.liveMyProfile = liveMyProfile
            }
        }
    // Collect block state from the single source of truth (BlockManager)
    val liveMyBlocks by BlockManager.blockedByMe.collectAsState()
    val liveWhoBlockedMe by BlockManager.blockedMe.collectAsState()
    val db = remember { DatabaseProvider.getDatabase(context) }
    val chatDao = db.chatDao()
    val debugEventChats = remember { androidx.compose.runtime.mutableStateMapOf<String, Boolean>() }
    
    val cachedDb = remember { com.example.cache.AppDatabase.getDatabase(context) }
    val cachedChatDao = cachedDb.cachedChatDao()
    val newCachedChatsRaw by cachedChatDao.getAllCachedChats().collectAsState(initial = com.example.ui.GlobalAppState.cachedChats)
    
    val liveLastSeenMap = remember { androidx.compose.runtime.mutableStateMapOf<String, String>() }
    val liveRealtimeUpdatesMap = remember { androidx.compose.runtime.mutableStateMapOf<String, java.time.Instant>() }
    val liveTypingMap = remember { androidx.compose.runtime.mutableStateMapOf<String, Boolean>() }
    // مستمع الكتابة العام (مستقل عن دورة حياة هذه الشاشة)
    androidx.compose.runtime.LaunchedEffect(Unit) { TypingHub.start() }
    val livePrivacyMap = remember { androidx.compose.runtime.mutableStateMapOf<String, PrivacySettings>() }
    var serverTimeOffsetSeconds by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    var rpcDebugMsg by remember { androidx.compose.runtime.mutableStateOf("Waiting for RPC...") }
    var liveTime by remember { androidx.compose.runtime.mutableStateOf(java.time.Instant.now()) }
    
    LaunchedEffect(Unit) {
        while (true) {
            liveTime = java.time.Instant.now()
            kotlinx.coroutines.delay(10_000)
        }
    }
    
    val roomChatsRaw by chatDao.getAllChats().collectAsState(initial = com.example.ui.GlobalAppState.roomChats)
    val roomChats = roomChatsRaw
    
    // Save to global state so next time we return, it's instant
    androidx.compose.runtime.LaunchedEffect(roomChatsRaw) {
        if (roomChatsRaw != null) {
            com.example.ui.GlobalAppState.roomChats = roomChatsRaw
        }
    }
    androidx.compose.runtime.LaunchedEffect(newCachedChatsRaw) {
        if (newCachedChatsRaw.isNotEmpty()) {
            com.example.ui.GlobalAppState.cachedChats = newCachedChatsRaw
        }
    }
    
    var deletedDummyIds by remember { androidx.compose.runtime.mutableStateOf(setOf<String>()) }
    
    val cachedScreenChats = remember { ChatListCache.loadChats(context) }
    var isInitialLoad by remember { androidx.compose.runtime.mutableStateOf(true) }
    
    LaunchedEffect(roomChats, newCachedChatsRaw) {
        if (roomChats != null || newCachedChatsRaw.isNotEmpty()) {
            kotlinx.coroutines.delay(100) // Give DBs a tiny moment to emit
            isInitialLoad = false
        }
    }
    
    LaunchedEffect(isInitialLoad, cachedScreenChats) {
        if (!isInitialLoad || !cachedScreenChats.isNullOrEmpty()) {
            com.example.ui.GlobalAppState.startupReady = true
        }
    }

    val chatsList = remember(roomChats, newCachedChatsRaw, deletedDummyIds, cachedScreenChats, isInitialLoad) {
        if (isInitialLoad && cachedScreenChats != null && cachedScreenChats.isNotEmpty()) {
            return@remember cachedScreenChats.filter { it.id !in deletedDummyIds && (it.id == FAKE_CHANNEL_ID || !it.id.startsWith("dummy_")) }
        }
        
        // القناة الرسمية صارت حقيقية: تأتي من Supabase مثل أي محادثة (لا قائمة افتراضية هنا)
        val defaults = emptyList<ChatModel>()
        
        val roomChatsModels = roomChats?.map { it.toModel() } ?: emptyList()
        val realChatsModels = newCachedChatsRaw.map { 
            ChatModel(
                id = it.chat_id,
                name = it.name,
                message = it.last_message,
                time = it.time_str,
                timestamp = it.timestamp,
                unreadCount = it.unread_count
            )
        }
        
        val combined = (roomChatsModels + realChatsModels + defaults).distinctBy { it.id }.sortedByDescending { it.timestamp }
        combined.filter { it.id !in deletedDummyIds && (it.id == FAKE_CHANNEL_ID || !it.id.startsWith("dummy_")) }
    }
    
    LaunchedEffect(chatsList) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            ChatListCache.saveChats(context, chatsList)
        }
    }

    LaunchedEffect(roomChats) {
        if (roomChats != null && roomChats!!.isEmpty()) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val initialChats = (chatsList ?: emptyList()).map { it.toEntity() }
                chatDao.insertAll(initialChats)
            }
        }
    }

    var refreshTrigger by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var isRefreshing by remember { androidx.compose.runtime.mutableStateOf(false) }
    var networkErrorMsg by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    androidx.compose.runtime.LaunchedEffect(networkErrorMsg) {
        if (networkErrorMsg != null) {
            android.widget.Toast.makeText(context, networkErrorMsg, android.widget.Toast.LENGTH_SHORT).show()
            kotlinx.coroutines.delay(3000)
            networkErrorMsg = null
        }
    }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val now = System.currentTimeMillis()
                if (now - com.example.ui.GlobalAppState.lastSyncTime > 60000) {
                    refreshTrigger++
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var isInitialSyncComplete by remember { mutableStateOf(com.example.ui.GlobalAppState.hasCompletedInitialChatSync) }
    LaunchedEffect(refreshTrigger) {

        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            // Fetch server time and calculate offset
            try {
                // To be safe against serialization mismatches, we use JsonElement
                val rawResponse = supabase.postgrest.rpc("get_server_time")
                val serverTimeRaw = rawResponse.decodeAs<kotlinx.serialization.json.JsonElement>().let { element ->
                    if (element is kotlinx.serialization.json.JsonPrimitive) element.content else element.toString()
                }
                rpcDebugMsg = "RPC Success: $serverTimeRaw"
                
                val cleanServerTime = serverTimeRaw.replace("\"", "")
                val serverInstant = java.time.OffsetDateTime.parse(cleanServerTime).toInstant()
                val localInstant = java.time.Instant.now()
                serverTimeOffsetSeconds = java.time.temporal.ChronoUnit.SECONDS.between(localInstant, serverInstant)
                com.example.ui.globalServerTimeOffsetSeconds = serverTimeOffsetSeconds
                rpcDebugMsg += "\nParsed OK. Offset: ${serverTimeOffsetSeconds}s"
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
                serverTimeOffsetSeconds = 0L
                com.example.ui.globalServerTimeOffsetSeconds = 0L
                rpcDebugMsg = "RPC Error: ${e::class.simpleName} - ${e.message}"
            }

            val myId = supabase.auth.currentUserOrNull()?.id ?: return@withContext
            try {
                liveMyProfile = supabase.postgrest["profiles"].select { filter { eq("id", myId) } }.decodeSingleOrNull<Profile>()
                // Refresh block state via BlockManager (updates StateFlows + Room atomically)
                BlockManager.refresh(context, myId, chatDao)
                ChatStateManager.refresh(myId, chatDao)
            } catch(e: Exception) {
            }
            try {
            val myMemberships = supabase.postgrest["chat_members"].select(Columns.list("chat_id")) {
                filter { eq("user_id", myId) }
            }.decodeList<ChatMemberRow>()
            val chatIds = myMemberships.map { it.chat_id }
            if (chatIds.isNotEmpty()) {
                val remoteChats = supabase.postgrest["chats"].select(Columns.list("id, type, title, avatar_url")) {
                    filter { isIn("id", chatIds) }
                }.decodeList<ChatRow>()
                
                val allMembers = supabase.postgrest["chat_members"].select(Columns.list("chat_id, user_id")) {
                    filter { isIn("chat_id", chatIds) }
                }.decodeList<ChatMemberRow>()
                
                val otherUserIds = allMembers.filter { it.user_id != myId }.mapNotNull { it.user_id }.distinct()
                
                val profiles = if (otherUserIds.isNotEmpty()) {
                    supabase.postgrest["profiles"].select() {
                        filter { isIn("id", otherUserIds) }
                    }.decodeList<Profile>().associateBy { it.id }
                } else emptyMap()
                
                profiles.values.forEach { p ->
                    if (p.lastSeenAt != null) {
                        liveLastSeenMap[p.id] = p.lastSeenAt
                        p.privacySettings?.let { livePrivacyMap[p.id] = it }
                    }
                }
                
                // Fetch latest message and unread counts
                val latestMessages = mutableMapOf<String, MessageRow>()
                val unreadCounts = mutableMapOf<String, Int>()
                var otherReadMessageIds = emptySet<String>()

                try {
                    val allMsgs = try {
                        supabase.postgrest["messages"].select() {
                            filter { isIn("chat_id", chatIds) }
                            order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                        }.decodeList<MessageRow>()
                    } catch(e: Exception) { emptyList() }
                    
                    if (allMsgs.isNotEmpty()) {
                        try {
                            // نحافظ على حالة "READ" المحفوظة (وإلا تنمسح علامة ✓✓ الزرقاء عند كل تحديث للقائمة)
                            val alreadyReadIds = try {
                                cachedDb.cachedMessageDao().getReadMessageIds().toHashSet()
                            } catch (e: Exception) { hashSetOf<String>() }
                            val toCache = allMsgs.map {
                               com.example.cache.CachedMessage(
                                   id = it.id,
                                   chat_id = it.chat_id,
                                   sender_id = it.sender_id,
                                   content = it.content,
                                   created_at = it.created_at,
                                   status = if (it.id in alreadyReadIds) "READ" else "SENT",
                                   message_type = it.message_type,
                                   media_url = it.media_url,
                                   thumbnail_url = it.thumbnail_url,
                                   reply_to_id = it.reply_to_id,
                                   media_aspect_ratio = it.media_aspect_ratio,
                                   media_group_id = it.media_group_id
                               )
                            }
                            // Preload all messages into cache so ChatDetailScreen is instant
                            cachedDb.cachedMessageDao().insertCachedMessages(toCache)
                        } catch(e: Exception) {}
                    }
                    
                    val allTheirMsgIds = allMsgs.filter { it.sender_id != myId }.map { it.id }
                    val myLastMsgs = mutableListOf<String>()
                    
                    for (chatId in chatIds) {
                        val chatMsgs = allMsgs.filter { it.chat_id == chatId }
                        chatMsgs.firstOrNull()?.let { latestMessages[chatId] = it }
                        chatMsgs.firstOrNull { it.sender_id == myId }?.let { myLastMsgs.add(it.id) }
                    }

                    val myReadMessageIds = try {
                        val reads = mutableListOf<String>()
                        kotlinx.coroutines.coroutineScope {
                            val deferreds = allTheirMsgIds.chunked(200).map { chunk ->
                                async {
                                    supabase.postgrest["message_reads"].select(Columns.list("message_id")) {
                                        filter {
                                             isIn("message_id", chunk)
                                            eq("user_id", myId)
                                         }
                                    }.decodeList<MessageReadRow>().map { it.message_id }
                                }
                            }
                            for (d in deferreds) {
                                reads.addAll(d.await())
                            }
                        }
                        val finalReads = reads.toSet()
                        finalReads
                    } catch(e: Exception) { 
                        emptySet() 
                    }
                    
                    otherReadMessageIds = try {
                        val reads = mutableListOf<String>()
                        kotlinx.coroutines.coroutineScope {
                            val deferreds = myLastMsgs.chunked(200).map { chunk ->
                                async {
                                    supabase.postgrest["message_reads"].select(Columns.list("message_id")) {
                                        filter {
                                             isIn("message_id", chunk)
                                            neq("user_id", myId)
                                         }
                                    }.decodeList<MessageReadRow>().map { it.message_id }
                                }
                            }
                            for (d in deferreds) {
                                reads.addAll(d.await())
                            }
                        }
                        val finalReads = reads.toSet()
                        finalReads
                    } catch(e: Exception) { 
                        emptySet() 
                    }

                    for (chatId in chatIds) {
                        val chatMsgs = allMsgs.filter { it.chat_id == chatId }
                        val unread = chatMsgs.count { it.sender_id != myId && it.id !in myReadMessageIds }
                        unreadCounts[chatId] = unread
                    }
                } catch(e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch(e: Exception) { e.printStackTrace() }

                val pinnedSet = try { supabase.postgrest["pinned_chats"].select(Columns.list("chat_id")) { filter { isIn("user_id", listOf(myId)) } }.decodeList<ChatMetadataRow>().map { it.chat_id }.toSet() } catch(e: Exception) { null }
                val mutedSet = try { supabase.postgrest["muted_chats"].select(Columns.list("chat_id")) { filter { isIn("user_id", listOf(myId)) } }.decodeList<ChatMetadataRow>().map { it.chat_id }.toSet() } catch(e: Exception) { null }
                val favSet = try { supabase.postgrest["favorite_chats"].select(Columns.list("chat_id")) { filter { isIn("user_id", listOf(myId)) } }.decodeList<ChatMetadataRow>().map { it.chat_id }.toSet() } catch(e: Exception) { null }
                
                val visibleRemoteChats = remoteChats

                val newEntities = visibleRemoteChats.map { rc ->
                    val isGroupOrChannel = rc.type == "group" || rc.type == "channel"
                    val otherUserId = allMembers.find { it.chat_id == rc.id && it.user_id != myId }?.user_id
                    val otherProfile = profiles[otherUserId]
                    val realName = if (isGroupOrChannel) {
                        rc.title ?: "Chat ${rc.id.take(4)}"
                    } else {
                        otherProfile?.fullName ?: otherProfile?.username ?: "Chat ${rc.id.take(4)}"
                    }
                    val avatar = if (isGroupOrChannel) rc.avatar_url else otherProfile?.avatarUrl
                    
                    val lastMsg = latestMessages[rc.id]
                    val msgText = lastMsg?.content ?: ""
                    
                    val timeStr = if (lastMsg != null) {
                        formatTimeSafe(lastMsg.created_at)
                    } else "Now"
                    
                    val timestamp = if (lastMsg != null) {
                        parseTimestampSafe(lastMsg.created_at)
                    } else 0L
                    
                    val existingChat = roomChats?.find { it.id == rc.id }
                    val preservedDraft = existingChat?.draft ?: ""
                    
                    // رقم السيرفر هو المرجع (حتى لو 0). نحتفظ بالرقم المحلي فقط إذا الحساب فشل (نت/خطأ).
                    val serverUnread: Int? = unreadCounts[rc.id]
                    val finalUnreadCount = serverUnread ?: existingChat?.unreadCount ?: 0
                    
                    val isMine = lastMsg?.sender_id == myId
                    // Check if it's already read locally for the SAME message
                    val isReadReceipt = if (lastMsg?.id in otherReadMessageIds) {
                        true
                    } else if (existingChat != null && existingChat.message == msgText && existingChat.time == timeStr) {
                        existingChat.isReadReceipt
                    } else {
                        false
                    }
                    
                    com.example.data.ChatEntity(
                        id = rc.id,
                        name = realName,
                        avatarUrl = avatar,
                        isGroup = rc.type == "group",
                        time = timeStr,
                        message = msgText,
                        isOnline = otherProfile?.isOnlineNow ?: false,
                        participantIds = if (otherUserId != null) otherUserId else "[]",
                        draft = preservedDraft,
                        timestamp = timestamp,
                        unreadCount = finalUnreadCount,
                        isBot = existingChat?.isBot ?: false,
                        isVerified = otherProfile?.isVerified == true || rc.id == OFFICIAL_CHANNEL_ID,
                        hasStar = if (pinnedSet != null) rc.id in pinnedSet else existingChat?.hasStar ?: false,
                        isMuted = if (mutedSet != null) rc.id in mutedSet else existingChat?.isMuted ?: false,
                        isFavorite = if (favSet != null) rc.id in favSet else existingChat?.isFavorite ?: false,
                        isBlocked = if (otherUserId != null && com.example.ui.BlockManager.blockedByMe.value.contains(otherUserId)) true else existingChat?.isBlocked ?: false,
                        isChannel = rc.type == "channel",
                        hasSparkleBadge = existingChat?.hasSparkleBadge ?: false,
                        isReadReceipt = isReadReceipt,
                        isMine = isMine,
                        isNotes = existingChat?.isNotes ?: false,
                        isArchived = existingChat?.isArchived ?: false,
                        isDefaultAvatar = existingChat?.isDefaultAvatar ?: false,
                        lastMediaType = lastMsg?.message_type,
                        lastMediaUrl = lastMsg?.media_url,
                        lastThumbnailUrl = lastMsg?.thumbnail_url
                    )
                }
                chatDao.insertAll(newEntities)
                
                val newCachedToInsert = newEntities.map {
                    com.example.cache.CachedChat(
                        chat_id = it.id,
                        name = it.name,
                        last_message = it.message,
                        time_str = it.time,
                        timestamp = it.timestamp,
                        unread_count = it.unreadCount
                    )
                }
                cachedChatDao.insertCachedChats(newCachedToInsert)
            }
            } catch(e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch(e: Exception) {
                e.printStackTrace()
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isRefreshing = false
            isInitialSyncComplete = true
            com.example.ui.GlobalAppState.hasCompletedInitialChatSync = true
            com.example.ui.GlobalAppState.lastSyncTime = System.currentTimeMillis()
        }
        }
    }
    
    DisposableEffect(Unit) {
        val channel = supabase.channel("chatlist_rt")
        val handler = kotlinx.coroutines.CoroutineExceptionHandler { _, _ -> }
        val job = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + handler).launch {
            try {
                val currentUserId = supabase.auth.currentUserOrNull()?.id
                if (currentUserId == null) return@launch
                
                val flow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                    table = "messages"
                }
                
                flow.onEach { action ->
                    try {
                        val record = action.decodeRecord<MessageRow>()

                        val existingChat = chatDao.getChatById(record.chat_id)
                        
                        if (existingChat != null) {
                            val timeStr = formatTimeSafe(record.created_at)
                            val timestamp = parseTimestampSafe(record.created_at)
                            
                            val isMe = record.sender_id == currentUserId
                            val unreadIncr = if (isMe || record.chat_id == com.example.ui.GlobalAppState.openChatId) 0 else 1
                            
                            val updatedChat = existingChat.copy(
                                message = record.content,
                                time = timeStr,
                                timestamp = timestamp,
                                unreadCount = existingChat.unreadCount + unreadIncr,
                                isMine = isMe,
                                isReadReceipt = false,
                                lastMediaType = record.message_type,
                                lastMediaUrl = record.media_url,
                                lastThumbnailUrl = record.thumbnail_url
                            )
                            
                            chatDao.insert(updatedChat)
                            
                            val updatedCachedChat = com.example.cache.CachedChat(
                                chat_id = updatedChat.id,
                                name = updatedChat.name,
                                last_message = updatedChat.message,
                                time_str = updatedChat.time,
                                timestamp = updatedChat.timestamp,
                                unread_count = updatedChat.unreadCount
                            )
                            cachedChatDao.insertCachedChats(listOf(updatedCachedChat))
                        } else {
                            // Missing local chat - Fetch and insert it instantly
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                try {
                                    val chatRow = supabase.postgrest["chats"].select(io.github.jan.supabase.postgrest.query.Columns.list("id, type, title, avatar_url")) {
                                        filter { eq("id", record.chat_id) }
                                    }.decodeSingleOrNull<ChatRow>()
                                    if (chatRow == null) return@launch
                                    
                                    val members = supabase.postgrest["chat_members"].select(io.github.jan.supabase.postgrest.query.Columns.list("chat_id, user_id")) {
                                        filter { eq("chat_id", record.chat_id) }
                                    }.decodeList<ChatMemberRow>()
                                    
                                    val isGroupOrChannel = chatRow.type == "group" || chatRow.type == "channel"
                                    val otherUserId = members.find { it.user_id != currentUserId }?.user_id
                                    
                                    var otherProfile: Profile? = null
                                    if (!isGroupOrChannel && otherUserId != null) {
                                        otherProfile = supabase.postgrest["profiles"].select() {
                                            filter { eq("id", otherUserId) }
                                        }.decodeSingleOrNull<Profile>()
                                    }
                                    
                                    val realName = if (isGroupOrChannel) (chatRow.title ?: "Chat ${chatRow.id.take(4)}")
                                                    else (otherProfile?.fullName ?: otherProfile?.username ?: "Chat ${chatRow.id.take(4)}")
                                    val avatar = if (isGroupOrChannel) chatRow.avatar_url else otherProfile?.avatarUrl
                                    
                                    val timeStr = formatTimeSafe(record.created_at)
                                    val timestamp = parseTimestampSafe(record.created_at)
                                    
                                    val isMe = record.sender_id == currentUserId
                                    val unreadCount = if (isMe || record.chat_id == com.example.ui.GlobalAppState.openChatId) 0 else 1
                                    
                                    val newEntity = com.example.data.ChatEntity(
                                        id = chatRow.id,
                                        name = realName,
                                        avatarUrl = avatar,
                                        isGroup = chatRow.type == "group",
                                        time = timeStr,
                                        message = record.content,
                                        isOnline = otherProfile?.isOnlineNow ?: false,
                                        participantIds = otherUserId ?: "[]",
                                        draft = "",
                                        timestamp = timestamp,
                                        unreadCount = unreadCount,
                                        isBot = false,
                                        isVerified = otherProfile?.isVerified == true || chatRow.id == OFFICIAL_CHANNEL_ID,
                                        hasStar = false,
                                        isMuted = false,
                                        isChannel = chatRow.type == "channel",
                                        hasSparkleBadge = false,
                                        isReadReceipt = false,
                                        isMine = isMe,
                                        isNotes = false,
                                        isDefaultAvatar = false,
                                        lastMediaType = record.message_type,
                                        lastMediaUrl = record.media_url,
                                        lastThumbnailUrl = record.thumbnail_url
                                    )
                                    chatDao.insert(newEntity)
                                    
                                    val updatedCachedChat = com.example.cache.CachedChat(
                                        chat_id = newEntity.id,
                                        name = newEntity.name,
                                        last_message = newEntity.message,
                                        time_str = newEntity.time,
                                        timestamp = newEntity.timestamp,
                                        unread_count = newEntity.unreadCount
                                    )
                                    cachedChatDao.insertCachedChats(listOf(updatedCachedChat))
                                } catch(e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }.launchIn(this)
                
                val readsFlow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                    table = "message_reads"
                }
                readsFlow.onEach { action ->
                    try {
                        val record = action.decodeRecord<MessageReadRow>()
                        if (record.user_id != currentUserId) {
                            val msgRow = supabase.postgrest["messages"].select() {
                                filter { eq("id", record.message_id) }
                            }.decodeSingleOrNull<MessageRow>()
                            
                            if (msgRow != null && msgRow.sender_id == currentUserId) {
                                val existingChat = chatDao.getChatById(msgRow.chat_id)
                                if (existingChat != null) {
                                    debugEventChats[msgRow.chat_id] = true
                                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                        kotlinx.coroutines.delay(3000)
                                        debugEventChats[msgRow.chat_id] = false
                                    }
                                    chatDao.insert(existingChat.copy(isReadReceipt = true))
                                    // Update cache too so it doesn't revert on quick recomposition
                                    try {
                                        cachedChatDao.insertCachedChats(listOf(
                                            com.example.cache.CachedChat(
                                                chat_id = existingChat.id,
                                                name = existingChat.name,
                                                last_message = existingChat.message,
                                                time_str = existingChat.time,
                                                timestamp = existingChat.timestamp,
                                                unread_count = existingChat.unreadCount
                                            )
                                        ))
                                    } catch (e: Exception) {}
                                }
                            }
                        }
                    } catch(e: Exception) {}
                }.launchIn(this)

                val profilesFlow = channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                    table = "profiles"
                }
                profilesFlow.onEach { action ->
                    try {
                        val record = action.decodeRecord<Profile>()
                        if (record.lastSeenAt != null) {
                            liveLastSeenMap[record.id] = record.lastSeenAt
                            record.privacySettings?.let { livePrivacyMap[record.id] = it }
                            liveRealtimeUpdatesMap[record.id] = java.time.Instant.now().plusSeconds(serverTimeOffsetSeconds)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }.launchIn(this)


                // NOTE: blocked_users realtime listener is intentionally removed from here.
                // It lives at the app level in MainActivity (started after login, stopped on logout)
                // so it remains active regardless of which screen is visible.
                
                val globalTypingChannel = supabase.channel("global_typing")
                globalTypingChannel.broadcastFlow<TypingEvent>("typing")
                    .onEach { event ->
                        if (event.user_id != currentUserId) {
                            liveTypingMap[event.chat_id] = event.is_typing
                        }
                    }.launchIn(this)

                // نفعّل بث "الكتابة" بشكل مستقل ومعزول تماماً عن باقي الاتصال (الشاتات/البروفايلات)،
                // باش لو صار أي تعطل هناك، ميزة "typing" في الشاشة الرئيسية تبقى تخدم بلا تأثر
                launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        globalTypingChannel.subscribe()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                supabase.realtime.connect()
                channel.subscribe()
                kotlinx.coroutines.awaitCancellation()
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    e.printStackTrace()
                }
            }
        }
        
        onDispose {
            job.cancel()
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    supabase.realtime.removeChannel(channel)
                } catch(e: Exception) {}
            }
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedChats by remember { mutableStateOf(emptySet<String>()) }
    var chatsToDelete by remember { mutableStateOf<List<String>?>(null) }
    var showReportToast by remember { mutableStateOf(false) }
    val isDark = __theme.isDark
    val bgColor = __theme.bgColor
    val textColor = SettingsColors.textPrimary
    val defaultFilter = com.example.ui.i18n.LocalTranslation.current.allChats
    var selectedFilter by remember { mutableStateOf(defaultFilter) }

    var topBarHeightPx by remember { mutableFloatStateOf(0f) }
    var headerHeightPx by remember { mutableFloatStateOf(0f) }
    val listState = rememberLazyListState()

    var dragAccumulator by remember { mutableFloatStateOf(0f) }
    Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
        detectDragGestures(
            onDragEnd = {
                if (dragAccumulator > 150f) {
                    // سحب من اليسار إلى اليمين -> فتح الإعدادات
                    onSettingsClick()
                } else if (dragAccumulator < -150f) {
                    // سحب من اليمين إلى اليسار -> فتح البحث
                    onDiscoverUsers()
                }
                dragAccumulator = 0f
            },
            onDrag = { change, dragAmount ->
                val horizontalDragAmount = dragAmount.x
                dragAccumulator += horizontalDragAmount
            }
        )
    }) {
    if (showReportToast) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            android.widget.Toast.makeText(context, "Report sent successfully", android.widget.Toast.LENGTH_SHORT).show()
            showReportToast = false
        }
    }
    
    if (chatsToDelete != null) {
        val chatIds = chatsToDelete ?: emptySet()
        val directChats = chatIds.mapNotNull { id -> chatsList?.find { it.id == id } }.filter { !it.isGroup && !it.isChannel }
        var deleteForEveryone by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { chatsToDelete = null },
            title = { Text(com.example.ui.i18n.LocalTranslation.current.deleteConversation) },
            text = { 
                Column {
                    Text(com.example.ui.i18n.LocalTranslation.current.deleteConversationConfirm)
                    if (directChats.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { deleteForEveryone = !deleteForEveryone }) {
                            androidx.compose.material3.Checkbox(checked = deleteForEveryone, onCheckedChange = { deleteForEveryone = it })
                            Text(com.example.ui.i18n.LocalTranslation.current.deleteForEveryone)
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    val ids = chatsToDelete
                    chatsToDelete = null
                    coroutineScope.launch {
                        val myId = supabase.auth.currentUserOrNull()?.id ?: return@launch
                        if (ids != null) {
                            ChatLifecycleManager.deleteChats(
                                context = context,
                                myId = myId,
                                chatIds = ids,
                                forEveryone = deleteForEveryone,
                                chatDao = chatDao
                            )
                        }
                    }
                }) { Text(com.example.ui.i18n.LocalTranslation.current.delete, color = Color.Red) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { chatsToDelete = null }) { Text(com.example.ui.i18n.LocalTranslation.current.cancel) }
            }
        )
    }

    var homeTab by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(0) }
    var isSearching by remember { mutableStateOf(false) }
    var discoverQuery by remember { mutableStateOf("") }
    val focusManagerHome = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboardControllerHome = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val closeHomeSearch = {
        discoverQuery = ""
        isSearching = false
        focusManagerHome.clearFocus()
    }
    androidx.activity.compose.BackHandler(enabled = isSearching) { closeHomeSearch() }
    androidx.activity.compose.BackHandler(enabled = !isSearching && homeTab == 1) { homeTab = 0 }

    Scaffold(
        modifier = Modifier.imePadding(),
        contentWindowInsets = WindowInsets.systemBars,
        containerColor = bgColor,
        snackbarHost = {
            androidx.compose.material3.SnackbarHost(archiveSnackbarHost) { data ->
                val sc = LocalSettingsTheme.current
                androidx.compose.material3.Snackbar(
                    snackbarData = data,
                    containerColor = sc.theme.surfaceColor,
                    contentColor = sc.theme.textPrimary,
                    actionColor = sc.accent,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        },
        floatingActionButtonPosition = androidx.compose.material3.FabPosition.End,
        floatingActionButton = {
            if (!isSearching && homeTab == 0) {
                HomeExtendedFab(
                    label = com.example.ui.i18n.LocalTranslation.current.newMessage,
                    onClick = { onNewMessage() }
                )
            }
        },
        bottomBar = {
            if (!isSearching) {
                val navT = com.example.ui.i18n.LocalTranslation.current
                HomeBottomBar(
                    chatsLabel = navT.chats,
                    followersLabel = navT.followersTab,
                    selectedIndex = homeTab,
                    unreadCount = (chatsList ?: emptyList()).count { it.unreadCount > 0 },
                    onChatsClick = {
                        if (homeTab != 0) homeTab = 0
                        else coroutineScope.launch { listState.animateScrollToItem(0) }
                    },
                    onFollowersClick = { homeTab = 1 }
                )
            }
        }
    ) { paddingValues ->
        val homeHeader: @Composable () -> Unit = {
                        Column(modifier = Modifier.background(bgColor)) {
                            androidx.compose.animation.AnimatedContent(
                                targetState = selectedChats.isNotEmpty(),
                                transitionSpec = {
                                    (fadeIn(animationSpec = tween(220, delayMillis = 40)) +
                                     scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 40)) +
                                     slideInVertically(initialOffsetY = { 20 }))
                                    .togetherWith(fadeOut(animationSpec = tween(150)) + slideOutVertically(targetOffsetY = { -20 }))
                                },
                                label = "TopActionBarAnimation"
                            ) { isSelectionMode ->
                                if (isSelectionMode) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { selectedChats = emptySet() }) {
                                                Icon(imageVector = Lucide.X, contentDescription = "Close", tint = textColor, modifier = Modifier.size(22.dp))
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = selectedChats.size.toString(),
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = __textPrimary
                                            )
                                        }

                                        val selectedChatsDetails = chatsList?.filter { it.id in selectedChats } ?: emptyList()
                                        val isCurrentlyMuted = if (selectedChatsDetails.isNotEmpty()) {
                                            selectedChatsDetails.all { it.isMuted }
                                        } else {
                                            false
                                        }
                                        
                                        val coroutineScope = rememberCoroutineScope()
                                        var moreMenuExpanded by remember { mutableStateOf(false) }
                                        var contextMenuView by remember { mutableStateOf("main") }
                                        
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { 
                                                val chatsToMute = selectedChats.toList()
                                                selectedChats = emptySet()
                                                coroutineScope.launch {
                                                    val newState = !isCurrentlyMuted
                                                    val myId = supabase.auth.currentUserOrNull()?.id
                                                    if (myId != null) {
                                                        val success = ChatStateManager.setMuted(myId, chatsToMute, newState, chatDao)
                                                        if (!success) {
                                                            android.widget.Toast.makeText(context, "خطأ في الاتصال، يرجى المحاولة لاحقاً", android.widget.Toast.LENGTH_LONG).show()
                                                        }
                                                    }
                                                }
                                            }) {
                                                Icon(imageVector = Lucide.BellOff, contentDescription = "Mute", tint = textColor, modifier = Modifier.size(20.dp))
                                            }
                                            IconButton(onClick = { 
                                                moreMenuExpanded = true
                                                contextMenuView = "confirm-delete"
                                            }) {
                                                Icon(imageVector = Lucide.Trash2, contentDescription = com.example.ui.i18n.LocalTranslation.current.delete, tint = textColor, modifier = Modifier.size(20.dp))
                                            }
                                            Box {
                                                IconButton(onClick = { moreMenuExpanded = true; contextMenuView = "main" }) {
                                                    Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = "More", tint = textColor, modifier = Modifier.size(20.dp))
                                                }
                                                val contextMenuBgColor = __theme.surfaceColor.copy(alpha=0.95f)
                                                val contextMenuBorderColor = if (__theme.isDark) Color.White.copy(alpha=0.05f) else Color.Black.copy(alpha=0.05f)

                                                if (moreMenuExpanded) {
                                                    androidx.compose.ui.window.Popup(
                                                        alignment = Alignment.TopEnd,
                                                        onDismissRequest = { 
                                                            moreMenuExpanded = false
                                                            contextMenuView = "main"
                                                        },
                                                        properties = androidx.compose.ui.window.PopupProperties(focusable = true)
                                                    ) {
                                                        androidx.compose.animation.AnimatedVisibility(
                                                            visible = moreMenuExpanded,
                                                            enter = androidx.compose.animation.scaleIn(initialScale = 0.95f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0f)) + androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { -15 }),
                                                            exit = androidx.compose.animation.scaleOut(targetScale = 0.95f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0f)) + androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(targetOffsetY = { -15 }),
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .padding(top = 40.dp, end = 4.dp)
                                                                    .width(220.dp)
                                                                    .background(color = contextMenuBgColor, shape = RoundedCornerShape(24.dp))
                                                                    .border(width = 1.dp, color = contextMenuBorderColor, shape = RoundedCornerShape(24.dp))
                                                                    .padding(vertical = 8.dp)
                                                            ) {
                                                                val selectedChatsDetailsMenu = chatsList?.filter { it.id in selectedChats } ?: emptyList()
                                                                val isCurrentlyMuted = selectedChatsDetailsMenu.isNotEmpty() && selectedChatsDetailsMenu.all { it.isMuted }
                                                                val isCurrentlyPinned = selectedChatsDetailsMenu.isNotEmpty() && selectedChatsDetailsMenu.all { it.hasStar }
                                                                val isCurrentlyFavorite = selectedChatsDetailsMenu.isNotEmpty() && selectedChatsDetailsMenu.all { it.isFavorite }
                                                                val isCurrentlyArchived = selectedChatsDetailsMenu.isNotEmpty() && selectedChatsDetailsMenu.all { it.isArchived }
                                                                
                                                                androidx.compose.animation.AnimatedContent(
                                                        targetState = contextMenuView,
                                                        label = "contextMenuView"
                                                    ) { view ->
                                                        when (view) {
                                                            "confirm-delete" -> {
                                                                Column(
                                                                    modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 200.dp),
                                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                                    verticalArrangement = Arrangement.Center
                                                                ) {
                                                                    Box(modifier = Modifier.size(56.dp).background(Color(0xFFFEF2F2), CircleShape), contentAlignment = Alignment.Center) {
                                                                        Icon(imageVector = Lucide.Trash2, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(28.dp))
                                                                    }
                                                                    Spacer(modifier = Modifier.height(12.dp))
                                                                    Text(com.example.ui.i18n.LocalTranslation.current.deleteChatConfirm, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = __textPrimary)
                                                                    Text(com.example.ui.i18n.LocalTranslation.current.cannotBeUndone, fontSize = 13.sp, color = __textSecondary, textAlign = TextAlign.Center, lineHeight = 18.sp, modifier = Modifier.padding(bottom = 8.dp, top = 4.dp))
                                                                    
                                                                    var deleteForEveryone by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                                                                    val selectedDirectChats = selectedChatsDetailsMenu.filter { !it.isGroup && !it.isChannel }
                                                                    if (selectedDirectChats.isNotEmpty()) {
                                                                        Row(
                                                                            verticalAlignment = Alignment.CenterVertically,
                                                                            modifier = Modifier
                                                                                .fillMaxWidth()
                                                                                .padding(bottom = 16.dp)
                                                                                .clickable(
                                                                                    interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                                                                    indication = null
                                                                                ) { deleteForEveryone = !deleteForEveryone }
                                                                        ) {
                                                                            androidx.compose.material3.Checkbox(
                                                                                checked = deleteForEveryone,
                                                                                onCheckedChange = { deleteForEveryone = it },
                                                                                colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = Color(0xFFEF4444))
                                                                            )
                                                                            Text(com.example.ui.i18n.LocalTranslation.current.deleteForEveryone, fontSize = 14.sp, color = __textPrimary)
                                                                        }
                                                                    }

                                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                                        androidx.compose.material3.Button(onClick = { contextMenuView = "main" }, modifier = Modifier.weight(1f).height(44.dp), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF333333) else Color(0xFFF3F4F6), contentColor = __textPrimary), contentPadding = PaddingValues(0.dp), shape = RoundedCornerShape(16.dp)) { Text(com.example.ui.i18n.LocalTranslation.current.cancel, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
                                                                        androidx.compose.material3.Button(onClick = { 
                                                                            val ids = selectedChats.toList()
                                                                            moreMenuExpanded = false; selectedChats = emptySet()
                                                                            coroutineScope.launch {
                                                                                val myId = supabase.auth.currentUserOrNull()?.id
                                                                                if (myId != null) {
                                                                                    val success = ChatLifecycleManager.deleteChats(
                                                                                        context = context,
                                                                                        myId = myId,
                                                                                        chatIds = ids,
                                                                                        forEveryone = deleteForEveryone,
                                                                                        chatDao = chatDao
                                                                                    )
                                                                                    if (!success) {
                                                                                        android.widget.Toast.makeText(context, "Failed to delete chat", android.widget.Toast.LENGTH_SHORT).show()
                                                                                    }
                                                                                }
                                                                            }
                                                                        }, modifier = Modifier.weight(1f).height(44.dp), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White), contentPadding = PaddingValues(0.dp), shape = RoundedCornerShape(16.dp)) { Text(com.example.ui.i18n.LocalTranslation.current.delete, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
                                                                    }
                                                                }
                                                            }
                                                            "confirm-block" -> {
                                                                Column(
                                                                    modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 200.dp),
                                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                                    verticalArrangement = Arrangement.Center
                                                                ) {
                                                                    Box(modifier = Modifier.size(56.dp).background(Color(0xFFFFF7ED), CircleShape), contentAlignment = Alignment.Center) {
                                                                        Icon(imageVector = Lucide.Ban, contentDescription = null, tint = Color(0xFFF97316), modifier = Modifier.size(28.dp))
                                                                    }
                                                                    Spacer(modifier = Modifier.height(12.dp))
                                                                    Text(com.example.ui.i18n.LocalTranslation.current.blockUserConfirmTitle, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = __textPrimary)
                                                                    Text(com.example.ui.i18n.LocalTranslation.current.blockUserConfirm, fontSize = 13.sp, color = __textSecondary, textAlign = TextAlign.Center, lineHeight = 18.sp, modifier = Modifier.padding(bottom = 16.dp, top = 4.dp))
                                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                                        androidx.compose.material3.Button(onClick = { contextMenuView = "more" }, modifier = Modifier.weight(1f).height(44.dp), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF333333) else Color(0xFFF3F4F6), contentColor = __textPrimary), contentPadding = PaddingValues(0.dp), shape = RoundedCornerShape(16.dp)) { Text(com.example.ui.i18n.LocalTranslation.current.cancel, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
                                                                        androidx.compose.material3.Button(onClick = {
                                                                            val ids = selectedChats.toList()
                                                                            moreMenuExpanded = false; selectedChats = emptySet()
                                                                            coroutineScope.launch {
                                                                                val myId = supabase.auth.currentUserOrNull()?.id
                                                                                ids.forEach { id ->
                                                                                    if (myId != null) {
                                                                                        val otherId = chatsList?.find { it.id == id }?.participantIds?.firstOrNull()
                                                                                        if (otherId != null) {
                                                                                            val success = com.example.ui.BlockManager.block(context, myId, otherId, chatDao)
                                                                                            if (!success) {
                                                                                                withContext(Dispatchers.Main) { android.widget.Toast.makeText(context, "Error blocking user", android.widget.Toast.LENGTH_LONG).show() }
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        }, modifier = Modifier.weight(1f).height(44.dp), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316), contentColor = Color.White), contentPadding = PaddingValues(0.dp), shape = RoundedCornerShape(16.dp)) { Text(com.example.ui.i18n.LocalTranslation.current.block, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
                                                                    }
                                                                }
                                                            }
                                                            "confirm-clear" -> {
                                                                Column(
                                                                    modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 200.dp),
                                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                                    verticalArrangement = Arrangement.Center
                                                                ) {
                                                                    Box(modifier = Modifier.size(56.dp).background(Color(0xFFFEF2F2), CircleShape), contentAlignment = Alignment.Center) {
                                                                        Icon(imageVector = Lucide.Eraser, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(28.dp))
                                                                    }
                                                                    Spacer(modifier = Modifier.height(12.dp))
                                                                    Text(com.example.ui.i18n.LocalTranslation.current.clearHistoryConfirmTitle, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = __textPrimary)
                                                                    Text(com.example.ui.i18n.LocalTranslation.current.clearHistoryConfirm, fontSize = 13.sp, color = __textSecondary, textAlign = TextAlign.Center, lineHeight = 18.sp, modifier = Modifier.padding(bottom = 16.dp, top = 4.dp))
                                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                                        androidx.compose.material3.Button(onClick = { contextMenuView = "more" }, modifier = Modifier.weight(1f).height(44.dp), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF333333) else Color(0xFFF3F4F6), contentColor = __textPrimary), contentPadding = PaddingValues(0.dp), shape = RoundedCornerShape(16.dp)) { Text(com.example.ui.i18n.LocalTranslation.current.cancel, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
                                                                        androidx.compose.material3.Button(onClick = { 
                                                                            val ids = selectedChats.toList()
                                                                            moreMenuExpanded = false; selectedChats = emptySet()
                                                                            coroutineScope.launch {
                                                                                val myId = supabase.auth.currentUserOrNull()?.id
                                                                                if (myId != null) {
                                                                                    val success = ChatLifecycleManager.clearHistory(
                                                                                        context = context,
                                                                                        myId = myId,
                                                                                        chatIds = ids,
                                                                                        forEveryone = false, // Clear history is usually for me only.
                                                                                        chatDao = chatDao
                                                                                    )
                                                                                    if (!success) {
                                                                                        android.widget.Toast.makeText(context, "Failed to clear history", android.widget.Toast.LENGTH_SHORT).show()
                                                                                    }
                                                                                }
                                                                            }
                                                                        }, modifier = Modifier.weight(1f).height(44.dp), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White), contentPadding = PaddingValues(0.dp), shape = RoundedCornerShape(16.dp)) { Text(com.example.ui.i18n.LocalTranslation.current.clear, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
                                                                    }
                                                                }
                                                            }
                                                            "more" -> {
                                                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                                                    CustomMenuItem(icon = Lucide.ChevronLeft, text = com.example.ui.i18n.LocalTranslation.current.back, fontWeight = FontWeight.Bold, onClick = { contextMenuView = "main" })
                                                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), thickness = 0.5.dp, color = __dividerColor)
                                                                    CustomMenuItem(icon = Lucide.Eraser, text = com.example.ui.i18n.LocalTranslation.current.clearChatHistory, onClick = { contextMenuView = "confirm-clear" })
                                                                    
                                                                    val selectedChatsDetailsMenuMore = chatsList?.filter { it.id in selectedChats } ?: emptyList()
                                                                    val isCurrentlyBlocked = selectedChatsDetailsMenuMore.isNotEmpty() && selectedChatsDetailsMenuMore.all { it.isBlocked }
                                                                    if (isCurrentlyBlocked) {
                                                                        CustomMenuItem(icon = Lucide.LockOpen, text = com.example.ui.i18n.LocalTranslation.current.unblock, onClick = {
                                                                            val ids = selectedChats.toList()
                                                                            moreMenuExpanded = false; selectedChats = emptySet()
                                                                            coroutineScope.launch {
                                                                                val myId = supabase.auth.currentUserOrNull()?.id
                                                                                ids.forEach { id ->
                                                                                    if (myId != null) {
                                                                                        val otherId = chatsList?.find { it.id == id }?.participantIds?.firstOrNull()
                                                                                        if (otherId != null) {
                                                                                            val success = com.example.ui.BlockManager.unblock(context, myId, otherId, chatDao)
                                                                                            if (!success) {
                                                                                                withContext(Dispatchers.Main) { android.widget.Toast.makeText(context, "Error unblocking user", android.widget.Toast.LENGTH_LONG).show() }
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        })
                                                                    } else {
                                                                        CustomMenuItem(icon = Lucide.Ban, iconTint = Color(0xFFF97316), iconBgColor = Color(0xFFFFF7ED), text = com.example.ui.i18n.LocalTranslation.current.blockUser, textColor = Color(0xFFF97316), onClick = { contextMenuView = "confirm-block" })
                                                                    }

                                                                    CustomMenuItem(icon = Lucide.Flag, iconTint = Color(0xFFEF4444), iconBgColor = Color(0xFFFEF2F2), text = com.example.ui.i18n.LocalTranslation.current.reportUser, textColor = Color(0xFFEF4444), onClick = {
                                                                        moreMenuExpanded = false; selectedChats = emptySet(); showReportToast = true
                                                                    })
                                                                }
                                                            }
                                                            "main" -> {
                                                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                                                    CustomMenuItem(icon = Lucide.BellOff, text = if (isCurrentlyMuted) com.example.ui.i18n.LocalTranslation.current.unmuteNotifications else com.example.ui.i18n.LocalTranslation.current.muteNotifications, isActive = isCurrentlyMuted, onClick = { 
                                                                        val chatsToMute = selectedChats.toList()
                                                                        moreMenuExpanded = false; selectedChats = emptySet()
                                                                        coroutineScope.launch {
                                                                            val newState = !isCurrentlyMuted
                                                                            val myId = supabase.auth.currentUserOrNull()?.id
                                                                            if (myId != null) {
                                                                                val success = ChatStateManager.setMuted(myId, chatsToMute, newState, chatDao)
                                                                                if (!success) {
                                                                                    android.widget.Toast.makeText(context, "خطأ في الاتصال، يرجى المحاولة لاحقاً", android.widget.Toast.LENGTH_LONG).show()
                                                                                }
                                                                            }
                                                                        }
                                                                    })
                                                                    CustomMenuItem(icon = Lucide.Pin, text = if (isCurrentlyPinned) com.example.ui.i18n.LocalTranslation.current.unpinChat else com.example.ui.i18n.LocalTranslation.current.pinChat, isActive = isCurrentlyPinned, onClick = { 
                                                                        val chatsToPin = selectedChats.toList()
                                                                        moreMenuExpanded = false; selectedChats = emptySet()
                                                                        coroutineScope.launch {
                                                                            val newState = !isCurrentlyPinned
                                                                            val myId = supabase.auth.currentUserOrNull()?.id
                                                                            if (myId != null) {
                                                                                val success = ChatStateManager.setPinned(myId, chatsToPin, newState, chatDao)
                                                                                if (!success) {
                                                                                    android.widget.Toast.makeText(context, "خطأ في الاتصال، يرجى المحاولة لاحقاً", android.widget.Toast.LENGTH_LONG).show()
                                                                                }
                                                                            }
                                                                        }
                                                                    })
                                                                    CustomMenuItem(icon = if (isCurrentlyFavorite) Lucide.Star else Lucide.Star, text = if (isCurrentlyFavorite) com.example.ui.i18n.LocalTranslation.current.removeFromFavorites else com.example.ui.i18n.LocalTranslation.current.addToFavorites, isActive = isCurrentlyFavorite, onClick = { 
                                                                        val chatsToFav = selectedChats.toList()
                                                                        moreMenuExpanded = false; selectedChats = emptySet()
                                                                        coroutineScope.launch {
                                                                            val newState = !isCurrentlyFavorite
                                                                            val myId = supabase.auth.currentUserOrNull()?.id
                                                                            if (myId != null) {
                                                                                val success = ChatStateManager.setFavorite(myId, chatsToFav, newState, chatDao)
                                                                                if (!success) {
                                                                                    android.widget.Toast.makeText(context, "خطأ في الاتصال، يرجى المحاولة لاحقاً", android.widget.Toast.LENGTH_LONG).show()
                                                                                }
                                                                            }
                                                                        }
                                                                    })
                                                                    CustomMenuItem(icon = Lucide.Archive, text = if (isCurrentlyArchived) "Unarchive" else "Archive", isActive = isCurrentlyArchived, onClick = { 
                                                                        val chatsToArchive = selectedChats.toList()
                                                                        moreMenuExpanded = false; selectedChats = emptySet()
                                                                        coroutineScope.launch {
                                                                            val newState = !isCurrentlyArchived
                                                                            val myId = supabase.auth.currentUserOrNull()?.id
                                                                            if (myId != null) {
                                                                                val success = ChatLifecycleManager.setArchived(context, myId, chatsToArchive, newState, chatDao)
                                                                                if (success) {
                                                                                    val res = archiveSnackbarHost.showSnackbar(
                                                                                        message = if (newState) trArch.chatArchivedToast else trArch.chatUnarchivedToast,
                                                                                        actionLabel = trArch.undoLabel,
                                                                                        duration = androidx.compose.material3.SnackbarDuration.Short
                                                                                    )
                                                                                    if (res == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                                                                        ChatLifecycleManager.setArchived(context, myId, chatsToArchive, !newState, chatDao)
                                                                                    }
                                                                                }
                                                                                if (!success) {
                                                                                    android.widget.Toast.makeText(context, "خطأ في الاتصال، يرجى المحاولة لاحقاً", android.widget.Toast.LENGTH_LONG).show()
                                                                                }
                                                                            }
                                                                        }
                                                                    })
                                                                    CustomMenuItem(icon = Lucide.User, text = com.example.ui.i18n.LocalTranslation.current.viewProfile, onClick = { 
                                                                        val participantId = selectedChatsDetailsMenu.firstOrNull()?.participantIds?.firstOrNull()
                                                                        moreMenuExpanded = false; selectedChats = emptySet()
                                                                        if (participantId != null) onUserProfileClick(participantId)
                                                                    })
                                                                    val shareContactText = com.example.ui.i18n.LocalTranslation.current.shareContact
                                                                    CustomMenuItem(icon = Lucide.Share2, text = shareContactText, onClick = {
                                                                        val chatName = selectedChatsDetailsMenu.firstOrNull()?.name ?: "Contact"
                                                                        moreMenuExpanded = false; selectedChats = emptySet()
                                                                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                                            type = "text/plain"
                                                                            putExtra(android.content.Intent.EXTRA_TEXT, "Contact: $chatName")
                                                                        }
                                                                        context.startActivity(android.content.Intent.createChooser(shareIntent, shareContactText))
                                                                    })
                                                                    CustomMenuItem(icon = Lucide.ChevronRight, text = com.example.ui.i18n.LocalTranslation.current.more, onClick = { contextMenuView = "more" })
                                                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), thickness = 0.5.dp, color = __dividerColor)
                                                                    CustomMenuItem(icon = Lucide.Trash2, iconTint = Color(0xFFEF4444), iconBgColor = Color(0xFFFEF2F2), text = com.example.ui.i18n.LocalTranslation.current.deleteConversation, textColor = Color(0xFFEF4444), onClick = { contextMenuView = "confirm-delete" })
                                                                }
                                                            }
                                                        }
                                                    }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        } // Close inner Row
                                    }
                                } else {
                                        val homeCtx = androidx.compose.ui.platform.LocalContext.current
                                        var isOnline by remember { mutableStateOf(true) }
                                        DisposableEffect(homeCtx) {
                                            val cm = homeCtx.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
                                            val cb = object : android.net.ConnectivityManager.NetworkCallback() {
                                                override fun onAvailable(network: android.net.Network) { isOnline = true }
                                                override fun onLost(network: android.net.Network) { isOnline = false }
                                            }
                                            val req = android.net.NetworkRequest.Builder().addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
                                            cm.registerNetworkCallback(req, cb)
                                            val caps = cm.getNetworkCapabilities(cm.activeNetwork)
                                            isOnline = caps?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                                            onDispose { cm.unregisterNetworkCallback(cb) }
                                        }
                                        val homeT = com.example.ui.i18n.LocalTranslation.current
                                        HomeTopBar(
                                            hint = if (isOnline) homeT.searchMessages.ifBlank { "Search" } else homeT.waitingForNetwork,
                                            avatarUrl = liveMyProfile?.avatarUrl,
                                            initial = (liveMyProfile?.let { (it.fullName?.takeIf { n -> n.isNotBlank() } ?: it.username) } ?: "").trim().take(1).uppercase().ifBlank { "H" },
                                            isSearching = isSearching,
                                            query = discoverQuery,
                                            onQueryChange = { discoverQuery = it },
                                            onSearchActivate = { isSearching = true },
                                            onCloseSearch = { closeHomeSearch() },
                                            onMenuClick = { onSettingsClick() },
                                            onAvatarClick = { onEditProfileClick() }
                                        )
                                }
                            }
                        }
        }
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isSearching || homeTab != 0 || selectedChats.isNotEmpty()) {
                homeHeader()
            }
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (isSearching) {
                    DiscoverSearchContent(
                        searchQuery = discoverQuery,
                        onChannelClick = { chId, chName ->
                            keyboardControllerHome?.hide()
                            focusManagerHome.clearFocus()
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                try {
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        com.example.supabase.postgrest.rpc("join_channel", kotlinx.serialization.json.buildJsonObject { put("p_chat_id", kotlinx.serialization.json.JsonPrimitive(chId)) })
                                    }
                                } catch (e: Exception) { e.printStackTrace() }
                                onChatClick(chId, chName, true)
                            }
                        },
                        onUserClick = { userId ->
                            keyboardControllerHome?.hide()
                            focusManagerHome.clearFocus()
                            onSearchUserClick(userId)
                        },
                        onBotClick = { botId, botName ->
                            keyboardControllerHome?.hide()
                            focusManagerHome.clearFocus()
                            if (botId == com.example.bot.BotManager.OFFICIAL_ID) {
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                    com.example.bot.BotManager.ensureChat(context)
                                    onChatClick(com.example.bot.BotManager.CHAT_ID, botName, false)
                                }
                            } else {
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                    com.example.bot.BotManager.ensureBotChat(context, botId, botName)
                                    onChatClick(com.example.bot.BotManager.botChatId(botId), botName, false)
                                }
                            }
                        }
                    )
                } else if (homeTab == 1) {
                    UserListScreen(
                        initialType = "followers",
                        onBack = { homeTab = 0 },
                        onUserClick = { _ -> },
                        onUpdateFollowers = {},
                        onUpdateFollowing = {},
                        embedded = true
                    )
                } else {
        // Heartbeat Debug View
        val heartbeatDebug by com.example.HeartbeatManager.debugLog
        
        @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { 
                isRefreshing = true
                refreshTrigger++ 
            },
            modifier = Modifier.fillMaxSize().clipToBounds()
        ) {
            val unreadText = com.example.ui.i18n.LocalTranslation.current.unread
            val groupsText = com.example.ui.i18n.LocalTranslation.current.groups
            val channelsText = com.example.ui.i18n.LocalTranslation.current.channels
            val archivedChats = remember(chatsList) { chatsList?.filter { it.isArchived } ?: emptyList() }
            val filteredChats = remember(chatsList, selectedFilter, searchQuery, unreadText, groupsText, channelsText) {
                val base = (chatsList ?: emptyList()).filter { !it.isArchived }
                val list = when (selectedFilter) {
                    unreadText -> base.filter { it.unreadCount > 0 }
                    groupsText -> base.filter { !it.isChannel && !it.isBot && it.subtitle != null && it.subtitle.contains("members") }
                    channelsText -> base.filter { it.isChannel }
                    else -> base
                }
                if (searchQuery.isNotBlank()) {
                    list.filter { 
                        it.name.contains(searchQuery, ignoreCase = true) || 
                        it.subtitle?.contains(searchQuery, ignoreCase = true) == true 
                    }
                } else {
                    list
                }
            }
            
            Box(modifier = Modifier.fillMaxSize()) {
                ChatList(
                    listState = listState,
                    chats = filteredChats,
                    isInitialSyncComplete = isInitialSyncComplete,
                    debugEventChats = debugEventChats,
                    selectedChats = selectedChats,
                    searchQuery = searchQuery,
                    liveLastSeenMap = liveLastSeenMap,
                    liveRealtimeUpdatesMap = liveRealtimeUpdatesMap,
                    liveTypingMap = liveTypingMap,
                    serverTimeOffsetSeconds = serverTimeOffsetSeconds,
                    rpcDebugMsg = rpcDebugMsg,
                    liveTime = liveTime,
                    archivedChats = archivedChats,
                    onArchivedClick = { onArchivedClick() },
                    onChatClick = { chatId, chatName, isChannel ->
                        if (selectedChats.isNotEmpty()) {
                            selectedChats = if (chatId in selectedChats) selectedChats - chatId else selectedChats + chatId
                        } else {
                            onChatClick(chatId, chatName, isChannel)
                        }
                    },
                    contentPadding = PaddingValues(bottom = 80.dp),
                    emptyMessage = when(selectedFilter) {
                        com.example.ui.i18n.LocalTranslation.current.groups -> "لا يوجد مجموعات"
                        com.example.ui.i18n.LocalTranslation.current.channels -> "لا يوجد قنوات"
                        com.example.ui.i18n.LocalTranslation.current.unread -> "لا توجد رسائل غير مقروءة"
                        else -> "لا توجد محادثات بعد"
                    },
                    onChatLongClick = { chat, bounds ->
                        if (selectedChats.isEmpty()) {
                            selectedChats = selectedChats + chat.id
                        } else {
                            selectedChats = if (chat.id in selectedChats) selectedChats - chat.id else selectedChats + chat.id
                        }
                    },
                    topBar = {
                        if (selectedChats.isEmpty()) {
                            homeHeader()
                        }
                    },
                    filterTabs = {
                        androidx.compose.material3.Surface(
                            color = bgColor.copy(alpha = 0.95f),
                            shadowElevation = if (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0) 2.dp else 0.dp
                        ) {
                            FilterTabs(selectedFilter = selectedFilter, onFilterSelected = { selectedFilter = it })
                        }
                    }
                )

                        
            }
        }
            }
        }
    }
    } // close Scaffold
}
}

@Composable
fun FilterTabs(selectedFilter: String, onFilterSelected: (String) -> Unit) {
    // Même design que les onglets du prototype (SearchTabStrip) : pastille douce qui glisse derrière l'onglet actif.
    val isDark = com.example.ui.LocalSettingsTheme.current.theme.isDark
    val tr = com.example.ui.i18n.LocalTranslation.current
    val filters = listOf("All" to tr.allChats, tr.unread to tr.unread, tr.groups to tr.groups, tr.channels to tr.channels)
    val selIndex = filters.indexOfFirst { (id, _) -> selectedFilter == id || (selectedFilter == tr.allChats && id == "All") }.coerceAtLeast(0)
    val prog = androidx.compose.animation.core.animateFloatAsState(
        targetValue = selIndex.toFloat(),
        animationSpec = androidx.compose.animation.core.tween(260, easing = com.example.ui.redesign.RdDecelerateEasing(1f)),
        label = "rdFilterPill"
    )
    Column(modifier = androidx.compose.ui.Modifier.fillMaxWidth()) {
        com.example.ui.redesign.RdTabStrip(
            labels = filters.map { it.second },
            isDark = isDark,
            progress = { prog.value },
            onSelect = { i -> onFilterSelected(filters[i].first) }
        )
        Box(
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(com.example.ui.redesign.RdColors.divider(isDark))
        )
    }
}
@Composable
fun FilterChipItem(
    text: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val __themeConfig = com.example.ui.LocalSettingsTheme.current
    val __theme = __themeConfig.theme
    val isDarkState = __theme.isDark
    val __bgColor = __theme.bgColor
    val __surfaceColor = __theme.surfaceColor
    val __textPrimary = SettingsColors.textPrimary
    val __textSecondary = SettingsColors.textSecondary
    val __dividerColor = __theme.dividerColor
    val __accent = __themeConfig.accent

    val backgroundColor by androidx.compose.animation.animateColorAsState(if (isSelected) Color(0xFFEDE7F6) else com.example.ui.SettingsColors.surface, label = "bgColor")
    val contentColor by androidx.compose.animation.animateColorAsState(if (isSelected) com.example.ui.theme.primaryPurple else MaterialTheme.colorScheme.onSurfaceVariant, label = "contentColor")

    androidx.compose.material3.Surface(
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        modifier = androidx.compose.ui.Modifier.clickable(onClick = onClick)
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = androidx.compose.ui.Modifier
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .animateContentSize(animationSpec = tween(200)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, modifier = androidx.compose.ui.Modifier.size(14.dp), tint = contentColor)
            }
            Text(text = text, color = contentColor, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            if (count > 0) {
                Text(
                    text = count.toString(),
                    color = contentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun ChatList(
    listState: LazyListState = rememberLazyListState(),
    chats: List<com.example.ui.ChatModel>?,
    isInitialSyncComplete: Boolean = true,
    selectedChats: Set<String> = emptySet(),
    searchQuery: String = "",
    onChatClick: (String, String, Boolean) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onChatLongClick: (com.example.ui.ChatModel, androidx.compose.ui.geometry.Rect) -> Unit,
    debugEventChats: Map<String, Boolean> = emptyMap(),
    emptyMessage: String = com.example.ui.i18n.LocalTranslation.current.noChatsYet,
    liveLastSeenMap: Map<String, String> = emptyMap(),
    liveRealtimeUpdatesMap: Map<String, java.time.Instant> = emptyMap(),
    liveTypingMap: Map<String, Boolean> = emptyMap(),
    serverTimeOffsetSeconds: Long = 0L,
    rpcDebugMsg: String = "",
    liveTime: java.time.Instant? = null,
    archivedChats: List<com.example.ui.ChatModel> = emptyList(),
    onArchivedClick: () -> Unit = {},
    liveMyProfile: Profile? = null,
    liveMyBlocks: List<String> = emptyList(),
    liveWhoBlockedMe: List<String> = emptyList(),
    livePrivacyMap: Map<String, PrivacySettings> = emptyMap(),
    topBar: @Composable () -> Unit = {},
    filterTabs: @Composable () -> Unit = {}
) {
    val __themeConfig = com.example.ui.LocalSettingsTheme.current
    val __theme = __themeConfig.theme
    val isDarkState = __theme.isDark
    val __bgColor = __theme.bgColor
    val __surfaceColor = __theme.surfaceColor
    val __textPrimary = SettingsColors.textPrimary
    val __textSecondary = SettingsColors.textSecondary
    val __dividerColor = __theme.dividerColor
    val __accent = __themeConfig.accent

    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
    androidx.compose.foundation.lazy.LazyColumn(modifier = androidx.compose.ui.Modifier.fillMaxSize(), state = listState, contentPadding = contentPadding) {
        if (archivedChats.isNotEmpty()) {
            item {
                val unreadTotal = archivedChats.sumOf { it.unreadCount }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onArchivedClick() }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(50.dp).background(Color(0xFFE0E0E0), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(imageVector = Lucide.Archive, contentDescription = "Archived", tint = Color.Gray, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Archived chats", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = __textPrimary)
                        if (archivedChats.isNotEmpty()) {
                            Text("${archivedChats.size} chats", fontSize = 14.sp, color = __textSecondary)
                        }
                    }
                    if (unreadTotal > 0) {
                        Box(
                            modifier = Modifier.background(com.example.ui.theme.primaryPurple, CircleShape).padding(horizontal = 6.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(unreadTotal.toString(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item { topBar() }
        stickyHeader { filterTabs() }
        if (chats == null || (!isInitialSyncComplete && chats.isEmpty())) {
            items(5) {
                val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "shimmer")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.15f,
                    targetValue = 0.45f,
                    animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                        animation = androidx.compose.animation.core.tween(1000),
                        repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                    ),
                    label = "alpha"
                )
                Row(
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = androidx.compose.ui.Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray.copy(alpha = alpha)))
                    Spacer(modifier = androidx.compose.ui.Modifier.width(16.dp))
                    Column(modifier = androidx.compose.ui.Modifier.weight(1f)) {
                        Box(modifier = androidx.compose.ui.Modifier
                            .height(16.dp)
                            .fillMaxWidth(0.6f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.LightGray.copy(alpha = alpha)))
                        Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                        Box(modifier = androidx.compose.ui.Modifier
                            .height(14.dp)
                            .fillMaxWidth(0.4f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.LightGray.copy(alpha = alpha)))
                    }
                }
            }
        } else if (chats.isEmpty()) {
            item {
                Column(
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(top = 100.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Outlined.Chat, contentDescription = "No Chats", modifier = androidx.compose.ui.Modifier.size(64.dp), tint = Color.LightGray)
                    Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
                    Text(emptyMessage, fontSize = 20.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(chats, key = { it.id }, contentType = { "chat" }) { chat ->
                val otherId = chat.participantIds.firstOrNull() ?: ""
                val lastSeenStr = liveLastSeenMap[otherId]
                val lastRealtimeUpdate = liveRealtimeUpdatesMap[otherId]
                
                val correctedCurrentTime = (liveTime ?: java.time.Instant.now()).plusSeconds(serverTimeOffsetSeconds)
                
                val amIBlocked = otherId != null && otherId in liveWhoBlockedMe
                val haveIBlocked = otherId != null && otherId in liveMyBlocks
                val myLastSeenRule = liveMyProfile?.privacySettings?.lastSeen ?: "everyone"
                val theirLastSeenRule = livePrivacyMap[otherId]?.lastSeen ?: "everyone"
                val theirPhotoRule = livePrivacyMap[otherId]?.profilePhoto ?: "everyone"
                
                val canSeePresence = PrivacyEvaluator.canSeePresence(myLastSeenRule, theirLastSeenRule, amIBlocked, haveIBlocked)
                
                val isActuallyOnline = if (!canSeePresence) {
                    false
                } else if (lastRealtimeUpdate != null && java.time.temporal.ChronoUnit.SECONDS.between(lastRealtimeUpdate, correctedCurrentTime) in -5..60) {
                    true
                } else if (lastSeenStr != null) {
                    com.example.ui.isUserOnline(lastSeenStr, correctedCurrentTime)
                } else {
                    chat.isOnline
                }
                
                val canSeePhoto = PrivacyEvaluator.canSeeProfilePhoto(theirPhotoRule, amIBlocked, haveIBlocked)
                val finalAvatarUrl = if (canSeePhoto) chat.avatarUrl else null
                
                Box(modifier = androidx.compose.ui.Modifier.animateItem()) {
                    ChatItem(
                        name = chat.name,
                        subtitle = chat.subtitle,
                        avatarUrl = finalAvatarUrl,
                        message = chat.message,
                        draft = chat.draft,
                        time = chat.time,
                        isOnline = isActuallyOnline,
                        isTyping = chat.isTyping || liveTypingMap[chat.id] == true || TypingHub.map[chat.id] == true,
                        unreadCount = chat.unreadCount,
                        hasStar = chat.hasStar,
                        isFavorite = chat.isFavorite,
                        isMuted = chat.isMuted,
                        isBot = chat.isBot,
                        isChannel = chat.isChannel,
                        isVerified = chat.isVerified,
                        verifiedTint = if (chat.id == FAKE_CHANNEL_ID) Color(0xFFEF4444) else Color(0xFF3B82F6),
                        isBlocked = chat.isBlocked,
                        hasSparkleBadge = chat.hasSparkleBadge,
                        isReadReceipt = chat.isReadReceipt,
                        isMine = chat.isMine,
                        isNotes = chat.isNotes,
                        isDefaultAvatar = chat.isDefaultAvatar,
                        isSelected = chat.id in selectedChats,
                        lastMediaType = chat.lastMediaType,
                        lastMediaUrl = chat.lastMediaUrl,
                        lastThumbnailUrl = chat.lastThumbnailUrl,
                        searchQuery = searchQuery,
                        participantId = chat.participantIds.firstOrNull(),
                        onClick = { onChatClick(chat.id, chat.name, chat.isChannel) },
                        onLongClick = { bounds -> onChatLongClick(chat, bounds) },
                        debugShowEvent = debugEventChats[chat.id] == true
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun highlightQuery(text: String, query: String, color: Color, highlightColor: Color): androidx.compose.ui.text.AnnotatedString {
    if (query.isBlank()) return androidx.compose.ui.text.buildAnnotatedString { append(text) }
    return androidx.compose.ui.text.buildAnnotatedString {
        val lowerText = text.lowercase()
        val lowerQuery = query.lowercase()
        var lastIndex = 0
        var index = lowerText.indexOf(lowerQuery)
        while (index >= 0) {
            withStyle(style = androidx.compose.ui.text.SpanStyle(color = color)) {
                append(text.substring(lastIndex, index))
            }
            withStyle(style = androidx.compose.ui.text.SpanStyle(color = highlightColor, fontWeight = FontWeight.Bold)) {
                append(text.substring(index, index + query.length))
            }
            lastIndex = index + query.length
            index = lowerText.indexOf(lowerQuery, lastIndex)
        }
        withStyle(style = androidx.compose.ui.text.SpanStyle(color = color)) {
            append(text.substring(lastIndex))
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ChatItem(
    name: String,
    subtitle: String?,
    avatarUrl: String?,
    message: String,
    draft: String = "",
    time: String,
    isOnline: Boolean,
    isTyping: Boolean,
    unreadCount: Int,
    hasStar: Boolean,
    isFavorite: Boolean = false,
    isMuted: Boolean,
    isBot: Boolean,
    isChannel: Boolean,
    isVerified: Boolean = false,
    verifiedTint: Color = Color(0xFF3B82F6),
    isBlocked: Boolean = false,
    hasSparkleBadge: Boolean = false,
    isReadReceipt: Boolean = false,
    isMine: Boolean = false,
    isNotes: Boolean = false,
    isDefaultAvatar: Boolean = false,
    isSelected: Boolean = false,
    lastMediaType: String? = null,
    lastMediaUrl: String? = null,
    lastThumbnailUrl: String? = null,
    searchQuery: String = "",
    participantId: String? = null,
    onClick: () -> Unit,
    onLongClick: (androidx.compose.ui.geometry.Rect) -> Unit = {},
    debugShowEvent: Boolean = false
) {
    com.example.ui.redesign.RdPlainText {
    val __themeConfig = com.example.ui.LocalSettingsTheme.current
    val __theme = __themeConfig.theme
    val isDarkState = __theme.isDark
    val __bgColor = __theme.bgColor
    val __surfaceColor = __theme.surfaceColor
    val __textPrimary = SettingsColors.textPrimary
    val __textSecondary = SettingsColors.textSecondary
    val __dividerColor = __theme.dividerColor
    val __accent = __themeConfig.accent

    val isDark = __theme.isDark
    val bgColor = __theme.bgColor
    val textColor = SettingsColors.textPrimary
    val subtitleColor = SettingsColors.textSecondary
    var bounds = androidx.compose.ui.geometry.Rect.Zero
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(durationMillis = 150, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "chatItemScale"
    )
    val itemBgColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isSelected) com.example.ui.theme.primaryPurple.copy(alpha = 0.1f) else bgColor,
        animationSpec = tween(150),
        label = "chatItemBg"
    )

    Row(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(itemBgColor)
            .onGloballyPositioned { bounds = it.boundsInRoot() }
            .combinedClickable(
                onClick = onClick,
                onLongClick = { onLongClick(bounds) },
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar — 56dp with subtle elevation shadow, matching React's w-[56px] h-[56px] shadow
        Box(modifier = androidx.compose.ui.Modifier.size(56.dp), contentAlignment = Alignment.Center) {
            if (avatarUrl != null) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val imageRequest = remember(avatarUrl) {
                    coil.request.ImageRequest.Builder(context)
                        .data(avatarUrl)
                        .crossfade(false)
                        
                        .memoryCacheKey(avatarUrl)
                        .diskCacheKey(avatarUrl)
                        .build()
                }
                coil.compose.AsyncImage(
                    model = imageRequest,
                    contentDescription = "Profile Picture",
                    modifier = androidx.compose.ui.Modifier
                        .size(56.dp)
                        .graphicsLayer { shadowElevation = 3.dp.toPx(); shape = CircleShape; clip = false }
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onBackground),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else if (isDefaultAvatar) {
                Box(
                    modifier = androidx.compose.ui.Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = androidx.compose.material.icons.Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = androidx.compose.ui.Modifier.size(26.dp))
                }
            } else if (isNotes) {
                Box(
                    modifier = androidx.compose.ui.Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3B82F6)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = androidx.compose.material.icons.Icons.Outlined.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.background, modifier = androidx.compose.ui.Modifier.size(26.dp))
                }
            } else {
                Box(
                    modifier = androidx.compose.ui.Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF2C2C2C) else MaterialTheme.colorScheme.outlineVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name.take(1).uppercase(),
                        color = MaterialTheme.colorScheme.background,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (!isChannel && !isBot && !isNotes && isOnline) {
                Box(
                    modifier = androidx.compose.ui.Modifier
                        .size(16.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = (-2).dp, y = (-2).dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E))
                )
            }
            if (isSelected) {
                Box(
                    modifier = androidx.compose.ui.Modifier
                        .size(18.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = (2).dp, y = (-2).dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                        .padding(1.5.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00C853)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = androidx.compose.material.icons.Icons.Filled.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.background, modifier = androidx.compose.ui.Modifier.size(11.dp))
                }
            }
        }
        
        Spacer(modifier = androidx.compose.ui.Modifier.width(14.dp))
        
        // Text Content & Indicators
        Column(
            modifier = androidx.compose.ui.Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            // Top line: name -> type icons -> favorite -> muted -> verified/sparkle | time -> pin -> read-status
            Row(verticalAlignment = Alignment.CenterVertically, modifier = androidx.compose.ui.Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(
                        text = highlightQuery(name, searchQuery, __textPrimary, Color(0xFF3B82F6)),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = __textPrimary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = androidx.compose.ui.Modifier.weight(1f, fill = false)
                    )
                    if (debugShowEvent) {
                        Spacer(modifier = Modifier.width(4.dp))
                        androidx.compose.foundation.layout.Box(modifier = Modifier.background(Color.Red, androidx.compose.foundation.shape.RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                            Text("EVENT", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (isBot) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isDark) Color(0xFF1E3A8A).copy(alpha=0.5f) else Color(0xFFDBEAFE),
                            modifier = Modifier.padding(start = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)) {
                                com.example.ui.redesign.RdGlyph(com.example.ui.redesign.RdGlyphKind.BOT, if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB), Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "BOT",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
                                )
                            }
                        }
                    }
                    if (isChannel) {
                        Icon(imageVector = Lucide.Megaphone, contentDescription = "Channel", tint = Color(0xFF9CA3AF), modifier = Modifier.padding(start = 6.dp).size(14.dp))
                    }
                    if (isFavorite) {
                        Icon(imageVector = Icons.Rounded.Star, contentDescription = "Favorite", tint = Color(0xFFFBBF24), modifier = Modifier.padding(start = 6.dp).size(16.dp))
                    }
                    if (isMuted) {
                        Icon(imageVector = Lucide.BellOff, contentDescription = "Muted", tint = subtitleColor, modifier = Modifier.padding(start = 6.dp).size(15.dp))
                    }
                    if (isVerified) {
                        com.example.ui.VerifiedBadge(isVerified = true, modifier = Modifier.padding(start = 6.dp), iconSize = 16.dp, tint = verifiedTint)
                    }
                    if (hasSparkleBadge) {
                        val gradient = androidx.compose.ui.graphics.Brush.linearGradient(colors = listOf(Color(0xFFFFD700), Color(0xFFFFA500)))
                        Box(modifier = Modifier.padding(start = 6.dp).size(15.dp).clip(CircleShape).background(gradient), contentAlignment = Alignment.Center) {
                            Icon(imageVector = Lucide.Sparkles, contentDescription = "Sparkle", tint = Color.White, modifier = Modifier.size(10.dp))
                        }
                    }
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isBlocked,
                        enter = androidx.compose.animation.scaleIn(animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.5f, stiffness = 500f)) + androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.scaleOut() + androidx.compose.animation.fadeOut()
                    ) {
                        Icon(imageVector = Lucide.Ban, contentDescription = "Blocked", tint = Color(0xFFF87171), modifier = Modifier.padding(start = 6.dp).size(15.dp))
                    }
                }

                Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
                val hasTimeOrPin = time.isNotEmpty() || hasStar
                if (hasTimeOrPin) {
                    // زي تيليجرام: إذا الدردشة مثبّتة، الدبوس + الوقت داخل كارد رمادي رقيق واحد
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = if (hasStar) {
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(__textSecondary.copy(alpha = 0.12f))
                                .padding(start = 6.dp, end = 7.dp, top = 1.dp, bottom = 1.dp)
                        } else Modifier
                    ) {
                        if (hasStar) {
                            Icon(
                                imageVector = Icons.Filled.PushPin,
                                contentDescription = "Pinned",
                                tint = Color(0xFF8E8E93),
                                modifier = Modifier
                                    .size(12.dp)
                                    .rotate(45f)
                            )
                            if (time.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(3.dp))
                            }
                        }
                        if (time.isNotEmpty()) {
                            Text(
                                text = time,
                                color = __textSecondary,
                                fontSize = 12.sp,
                                lineHeight = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = androidx.compose.ui.Modifier.height(3.dp))
            
            // Bottom line: read-status icon -> message/typing/draft preview | unread badge
            Row(verticalAlignment = Alignment.CenterVertically, modifier = androidx.compose.ui.Modifier.fillMaxWidth()) {
                Row(modifier = androidx.compose.ui.Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    if (draft.isNotEmpty()) {
                        Text(text = "Draft: ", color = Color.Red, fontSize = 14.sp)
                        Text(text = draft, color = subtitleColor, fontSize = 14.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    } else if (isTyping) {
                        ChatListTypingIndicator()
                    } else {
                        val isImage = lastMediaType == "image" && !lastMediaUrl.isNullOrEmpty()
                        if (isImage) {
                            // نفضّل النسخة المصغّرة الجديدة (سريعة، صغيرة الحجم) على الصورة الكاملة
                            // -- هذا هو نفس الملف اللي يتحمل جوا فقاعة الدردشة، فيكون أصلاً محمّل
                            // في الكاش المحلي غالباً ويطلع فوري هنا كيفما
                            val thumbnail = lastThumbnailUrl?.takeIf { it.isNotBlank() }
                                ?: lastMediaUrl?.split(",")?.map { it.trim() }?.firstOrNull { it.isNotEmpty() }
                            if (thumbnail != null) {
                                androidx.compose.foundation.layout.Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.Gray.copy(alpha = 0.18f))
                                ) {
                                    coil.compose.AsyncImage(
                                        model = thumbnail,
                                        contentDescription = "Thumbnail",
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                        }
                        
                        // أيقونة صغيرة قدام الكلمة للفيديو/الصوت/الملف (الصور تعرض مصغّرتها فوق)
                        if (!isImage) {
                            val typeIcon = when (lastMediaType) {
                                "video" -> androidx.compose.material.icons.Icons.Filled.Videocam
                                "voice", "audio" -> androidx.compose.material.icons.Icons.Filled.Mic
                                "file" -> androidx.compose.material.icons.Icons.Filled.Description
                                else -> null
                            }
                            if (typeIcon != null) {
                                Icon(
                                    imageVector = typeIcon,
                                    contentDescription = null,
                                    tint = subtitleColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                        }

                        val rawMsg = message.replace("**", "").replace("[Album]", "").replace("[Photo]", "").replace("[Video]", "")
                            .replace("[Documents]", "").replace("[Document]", "").replace("[Attachment]", "").replace("[Voice]", "").replace("[Voice message]", "").trim()
                        val isArabicUi = com.example.ui.i18n.TranslationManager.currentLanguageCode.value == "ar"
                        // نوع الرسالة مكتوب قدام المصغّرة: Photo / Video / Voice message / Document / Album
                        val mediaLabel: String? = when {
                            message.contains("[Album]") -> if (isArabicUi) "ألبوم" else "Album"
                            lastMediaType == "image" -> if (isArabicUi) "صورة" else "Photo"
                            lastMediaType == "video" -> if (isArabicUi) "فيديو" else "Video"
                            lastMediaType == "voice" || lastMediaType == "audio" -> if (isArabicUi) "رسالة صوتية" else "Voice message"
                            lastMediaType == "file" -> if (isArabicUi) "ملف" else "Document"
                            else -> null
                        }
                        val displayMsg = when {
                            rawMsg.isNotEmpty() -> rawMsg
                            mediaLabel != null -> mediaLabel
                            else -> subtitle ?: ""
                        }

                        val previewEmojiSequence = remember(rawMsg, isImage) {
                            if (!isImage) com.example.emoji.EmojiMessageUtils.parseSupportedEmojiSequence(rawMsg) else null
                        }

                        val msgTextColor = if (isMine) Color(0xFF3B82F6) else com.example.ui.redesign.RdColors.preview(isDark)

                        if (previewEmojiSequence != null) {
                            // الإيموجيات في سطر واحد بعرض الصف كامل (زي تيليجرام) — والباقي "…"
                            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                val slot = 22.dp
                                val total = previewEmojiSequence.size
                                val rawFit = (maxWidth / slot).toInt().coerceAtLeast(1)
                                val fit = if (total > rawFit) (rawFit - 1).coerceAtLeast(1) else rawFit
                                val shown = previewEmojiSequence.take(minOf(fit, 12))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    shown.forEach { emojiUnit ->
                                        val previewUrl = com.example.emoji.NotoEmojiMap.remoteUrlFor(emojiUnit)
                                        if (previewUrl != null) {
                                            LottieEmojiReaction(
                                                url = previewUrl,
                                                size = 20.dp,
                                                modifier = Modifier.padding(end = 2.dp),
                                                animate = total <= 5
                                            )
                                        }
                                    }
                                    if (total > shown.size) {
                                        Text(text = "…", fontSize = 14.sp, color = subtitleColor)
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = highlightQuery(displayMsg, searchQuery, msgTextColor, Color(0xFF3B82F6)),
                                fontSize = 14.sp, 
                                maxLines = 1, 
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                
                Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // آخر رسالة مني -> علامة صح (تتحول للأزرق عند رؤيتها من الطرف الآخر) بدل رقم العداد
                    val showStatusTick = isMine && !isTyping && message.isNotEmpty()
                    if (showStatusTick) {
                        val readIcon = if (isReadReceipt) androidx.compose.material.icons.Icons.Filled.DoneAll else androidx.compose.material.icons.Icons.Filled.Check
                        Icon(
                            imageVector = readIcon,
                            contentDescription = "Read Status",
                            tint = if (isReadReceipt) Color(0xFF3B82F6) else Color(0xFF9CA3AF),
                            modifier = androidx.compose.ui.Modifier.size(21.dp).padding(start = 4.dp)
                        )
                    }
                    if (!showStatusTick && unreadCount > 0) {
                        // Unread pill: bg-[#22c55e], min 20x20
                        val badgeColor = if (isMuted) Color(0xFF8E8E93) else Color(0xFF22C55E)
                        Box(
                            modifier = androidx.compose.ui.Modifier
                                .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                                .clip(RoundedCornerShape(50))
                                .background(badgeColor)
                                .padding(horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = unreadCount.toString(),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
fun LazyListState.isScrollingUp(): Boolean {
    var previousIndex by remember(this) { androidx.compose.runtime.mutableIntStateOf(firstVisibleItemIndex) }
    var previousScrollOffset by remember(this) { androidx.compose.runtime.mutableIntStateOf(firstVisibleItemScrollOffset) }
    var isScrollingUp by remember(this) { androidx.compose.runtime.mutableStateOf(true) }
    LaunchedEffect(this) {
        androidx.compose.runtime.snapshotFlow {
            firstVisibleItemIndex to firstVisibleItemScrollOffset
        }.collect { (index, offset) ->
            if (previousIndex != index) {
                isScrollingUp = index < previousIndex
            } else if (previousScrollOffset != offset) {
                isScrollingUp = offset < previousScrollOffset
            }
            previousIndex = index
            previousScrollOffset = offset
        }
    }
    return isScrollingUp
}

@Composable
fun CustomMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.onBackground,
    iconBgColor: Color = Color(0xFFF4F5F7),
    text: String,
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    rightIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    rightIconTint: Color = Color(0xFF007AFF),
    isActive: Boolean = false,
    fontWeight: FontWeight = FontWeight.Medium,
    onClick: () -> Unit
) {
    val __themeConfig = com.example.ui.LocalSettingsTheme.current
    val __theme = __themeConfig.theme
    val isDarkState = __theme.isDark
    val __bgColor = __theme.bgColor
    val __surfaceColor = __theme.surfaceColor
    val __textPrimary = SettingsColors.textPrimary
    val __textSecondary = SettingsColors.textSecondary
    val __dividerColor = __theme.dividerColor
    val __accent = __themeConfig.accent

    androidx.compose.foundation.layout.Row(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = androidx.compose.ui.Modifier
                .size(32.dp)
                .background(iconBgColor, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = androidx.compose.ui.Modifier.size(18.dp))
        }
        Spacer(modifier = androidx.compose.ui.Modifier.width(10.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = fontWeight,
            color = if (textColor == MaterialTheme.colorScheme.onBackground) __textPrimary else textColor,
            modifier = androidx.compose.ui.Modifier.weight(1f)
        )
        if (isActive) {
            Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = Color(0xFF3B82F6), modifier = androidx.compose.ui.Modifier.size(18.dp))
        } else if (rightIcon != null) {
            Icon(imageVector = rightIcon, contentDescription = null, tint = rightIconTint, modifier = androidx.compose.ui.Modifier.size(18.dp))
        }
    }
}


@Composable
fun MainBottomNavBar(
    currentRoute: String = "chatList",
    onRouteSelected: (String) -> Unit = {}
) {
    val __themeConfig = com.example.ui.LocalSettingsTheme.current
    val __theme = __themeConfig.theme
    val isDarkState = __theme.isDark
    val __bgColor = __theme.bgColor
    val __surfaceColor = __theme.surfaceColor
    val __textPrimary = SettingsColors.textPrimary
    val __textSecondary = SettingsColors.textSecondary
    val __dividerColor = __theme.dividerColor
    val __accent = __themeConfig.accent

    val isDark = __theme.isDark
    val bgColor = __surfaceColor
    val activeColor = __accent
    val inactiveColor = __textSecondary

    androidx.compose.foundation.layout.Box(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        contentAlignment = androidx.compose.ui.Alignment.BottomCenter
    ) {
        androidx.compose.material3.Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
            color = bgColor,
            shadowElevation = 16.dp,
            modifier = androidx.compose.ui.Modifier.height(56.dp)
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = androidx.compose.ui.Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Chats
                BottomNavItemIcon(
                    icon = androidx.compose.material.icons.Icons.AutoMirrored.Filled.Chat,
                    isActive = currentRoute == "chatList",
                    activeColor = activeColor,
                    inactiveColor = inactiveColor,
                    badge = "٢٦",
                    onClick = { onRouteSelected("chatList") }
                )
                // Search
                val searchText = com.example.ui.i18n.LocalTranslation.current.search
                BottomNavItemIcon(
                    icon = androidx.compose.material.icons.Icons.Outlined.Search,
                    isActive = currentRoute == "discoverUsers" || currentRoute == searchText,
                    activeColor = activeColor,
                    inactiveColor = inactiveColor,
                    onClick = { onRouteSelected(searchText) }
                )
                // Settings
                val settingsText = com.example.ui.i18n.LocalTranslation.current.settings
                BottomNavItemIcon(
                    icon = androidx.compose.material.icons.Icons.Outlined.Settings,
                    isActive = currentRoute == "settings" || currentRoute == settingsText,
                    activeColor = activeColor,
                    inactiveColor = inactiveColor,
                    onClick = { onRouteSelected(settingsText) }
                )
                // Profile
                BottomNavItemIcon(
                    isProfile = true,
                    profileUrl = "https://i.pravatar.cc/150?u=You",
                    isActive = currentRoute == "swipeProfile" || currentRoute == "Profile",
                    activeColor = activeColor,
                    inactiveColor = inactiveColor,
                    onClick = { onRouteSelected("Profile") }
                )
            }
        }
    }
}

@Composable
fun BottomNavItemIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    isProfile: Boolean = false,
    profileUrl: String = "",
    isActive: Boolean,
    activeColor: androidx.compose.ui.graphics.Color,
    inactiveColor: androidx.compose.ui.graphics.Color,
    badge: String? = null,
    onClick: () -> Unit
) {
    val __themeConfig = com.example.ui.LocalSettingsTheme.current
    val __theme = __themeConfig.theme
    val isDarkState = __theme.isDark
    val __bgColor = __theme.bgColor
    val __surfaceColor = __theme.surfaceColor
    val __textPrimary = SettingsColors.textPrimary
    val __textSecondary = SettingsColors.textSecondary
    val __dividerColor = __theme.dividerColor
    val __accent = __themeConfig.accent

    val isDark = __theme.isDark
    val activeBg = __accent.copy(alpha = 0.2f)
    val contentColor = if (isActive) activeColor else inactiveColor
    val badgeBorder = if (isActive) activeBg else __surfaceColor

    androidx.compose.foundation.layout.Box(
        modifier = androidx.compose.ui.Modifier
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(if (isActive) activeBg else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Box {
            if (isProfile) {
                coil.compose.AsyncImage(
                    model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current).data(profileUrl).crossfade(false).build(),
                    contentDescription = null,
                    modifier = androidx.compose.ui.Modifier.size(22.dp).clip(androidx.compose.foundation.shape.CircleShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = androidx.compose.ui.Modifier.size(22.dp)
                )
            }
            
            if (badge != null) {
                Box(
                    modifier = androidx.compose.ui.Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-4).dp)
                        .background(activeColor, androidx.compose.foundation.shape.CircleShape)
                        .border(1.dp, badgeBorder, androidx.compose.foundation.shape.CircleShape)
                        .padding(horizontal = 3.dp, vertical = 0.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(badge, color = androidx.compose.ui.graphics.Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}





@kotlinx.serialization.Serializable
data class ChatMetadataRow(val chat_id: String)
@kotlinx.serialization.Serializable
data class ChatActionInsert(val user_id: String, val chat_id: String)
