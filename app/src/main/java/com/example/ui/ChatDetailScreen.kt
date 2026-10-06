@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui
import androidx.compose.ui.graphics.luminance
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import kotlin.math.roundToInt
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.*
import kotlinx.coroutines.flow.*
import io.github.jan.supabase.storage.storage
import androidx.compose.material.icons.filled.*
import com.example.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState

import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.lazy.grid.items

import kotlinx.coroutines.*
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Check
import androidx.compose.ui.graphics.vector.ImageVector

import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import com.example.R
import androidx.compose.foundation.background
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.border
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawWithContent

import androidx.compose.ui.draw.blur
import com.example.ui.i18n.LocalTranslation
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.broadcast
import io.github.jan.supabase.realtime.decodeRecord
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import androidx.compose.runtime.DisposableEffect
import com.example.ui.theme.*
import androidx.compose.material.icons.filled.ContentCopy

data class ToastData(val message: String, val icon:androidx.compose.ui.graphics.vector.ImageVector)
enum class ToastType { PIN, UNPIN, COPY, SAVE, UNSAVE, INFO }
data class ToastNotification(val id: Long, val text: String, val type: ToastType)


// يحاول يحول نص الطابع الزمني (من Supabase) لـ Instant، ويرجع null إذا فشل بدل ما يوقف التطبيق
fun parseInstantSafe(value: String?): java.time.Instant? {
    if (value.isNullOrBlank()) return null
    return try {
        java.time.Instant.parse(value)
    } catch (e: Exception) {
        try {
            java.time.OffsetDateTime.parse(value).toInstant()
        } catch (e2: Exception) {
            null
        }
    }
}

// --- Image Grouping Logic ---
fun groupImageMessages(messages: List<MessageModel>): List<MessageModel> {
    if (messages.isEmpty()) return emptyList()

    // الخطوة 1: نجمع كل الرسائل اللي عندها نفس media_group_id من أي مكان في القائمة،
    // بلا شرط يكونو متجاورين — هذا يصلح المشكل اللي كانت تصير فيه صورة توصل بعد
    // رسالة ثانية (إيموجي مثلاً) انبعثت بيناتهم أثناء الرفع، وتخرج من الألبوم بالغلط
    val byGroupId: Map<String, List<MessageModel>> = messages
        .filter { it.mediaGroupId != null }
        .groupBy { it.mediaGroupId!! }
        .filterValues { msgs ->
            msgs.map { it.isMine to it.senderId }.distinct().size == 1 &&
            msgs.all { m -> m.attachments.isNotEmpty() && m.attachments.all { it.type == AttachmentType.IMAGE } }
        }

    val consumedIds = byGroupId.values.flatten().map { it.id }.toHashSet()
    val emittedGroupIds = mutableSetOf<String>()
    val result = mutableListOf<MessageModel>()
    var pendingSequential = mutableListOf<MessageModel>()

    fun flushSequential() {
        if (pendingSequential.isEmpty()) return
        result.addAll(groupSequentialByAdjacency(pendingSequential))
        pendingSequential = mutableListOf()
    }

    for (msg in messages) {
        if (msg.id in consumedIds) {
            val gid = msg.mediaGroupId!!
            if (gid !in emittedGroupIds) {
                flushSequential()
                val group = byGroupId.getValue(gid).sortedBy {
                    parseInstantSafe(it.createdAtExact)?.toEpochMilli() ?: 0L
                }
                result.add(mergeMessageGroup(group))
                emittedGroupIds.add(gid)
            }
        } else {
            pendingSequential.add(msg)
        }
    }
    flushSequential()
    return result
}

// الخطوة 2: خوارزمية التجميع القديمة (بالمجاورة + نافذة وقت 4 ثواني) — تبقى تخدم
// كطريقة احتياطية بس على الرسائل اللي ما عندهاش media_group_id (بيانات قديمة)
private fun groupSequentialByAdjacency(messages: List<MessageModel>): List<MessageModel> {
    if (messages.isEmpty()) return emptyList()
    val grouped = mutableListOf<MessageModel>()
    var currentGroup = mutableListOf<MessageModel>()

    for (msg in messages) {
        if (currentGroup.isEmpty()) {
            currentGroup.add(msg)
            continue
        }
        val prevMsg = currentGroup.last()

        val isSameSender = msg.isMine == prevMsg.isMine && msg.senderId == prevMsg.senderId
        val isMsgImage = msg.attachments.isNotEmpty() && msg.attachments.all { it.type == AttachmentType.IMAGE }
        val isPrevImage = prevMsg.attachments.isNotEmpty() && prevMsg.attachments.all { it.type == AttachmentType.IMAGE }

        val msgInstant = parseInstantSafe(msg.createdAtExact)
        val prevInstant = parseInstantSafe(prevMsg.createdAtExact)
        val isSameBatch = if (msgInstant != null && prevInstant != null) {
            kotlin.math.abs(java.time.Duration.between(prevInstant, msgInstant).toMillis()) <= 4000L
        } else {
            msg.time == prevMsg.time
        }
        val isSameReply = msg.replyToId == prevMsg.replyToId
        val isBothUnpinned = !msg.isPinned && !prevMsg.isPinned

        if (isSameSender && isMsgImage && isPrevImage && isSameBatch && isSameReply && isBothUnpinned) {
            currentGroup.add(msg)
        } else {
            grouped.add(mergeMessageGroup(currentGroup))
            currentGroup = mutableListOf(msg)
        }
    }
    if (currentGroup.isNotEmpty()) {
        grouped.add(mergeMessageGroup(currentGroup))
    }
    return grouped
}

fun mergeMessageGroup(group: List<MessageModel>): MessageModel {
    if (group.size == 1) return group.first()
    val first = group.first()
    val combinedAttachments = group.flatMap { it.attachments }
    val combinedText = group.mapNotNull { it.text.takeIf { t -> t.isNotBlank() } }.joinToString("\n")
    return first.copy(
        text = combinedText,
        attachments = combinedAttachments
    )
}
// -----------------------------

@Composable
fun ChatDetailScreen(chatId: String, name: String, isChannel: Boolean = false, onBack: () -> Unit, onDiscoverUsers: () -> Unit = {
    }, onProfileClick: (String) -> Unit = {}) {
      val __themeConfig = com.example.ui.LocalSettingsTheme.current
  val __theme = __themeConfig.theme
  val __bgColor = __theme.bgColor
  val __surfaceColor = __theme.surfaceColor
  val __textPrimary = __theme.textPrimary
  val __textSecondary = __theme.textSecondary
  val __dividerColor = __theme.dividerColor

  var messageText by remember { mutableStateOf("") }
  var isTyping by remember { mutableStateOf(false) }
  val context = androidx.compose.ui.platform.LocalContext.current
  val chatDao = androidx.compose.runtime.remember {
com.example.data.DatabaseProvider.getDatabase(context).chatDao() }

  val cachedDb = androidx.compose.runtime.remember {
com.example.cache.AppDatabase.getDatabase(context) }
  val cachedMessageDao = cachedDb.cachedMessageDao()
  val cachedChatDao = cachedDb.cachedChatDao()
  val currentChat by chatDao.observeChatById(chatId).collectAsState(initial = com.example.ui.GlobalAppState.roomChats?.find { it.id == chatId })
  val isBotManagerChat = chatId == com.example.bot.BotManager.CHAT_ID
  val isUserBotChat = com.example.bot.BotManager.isUserBotChat(chatId)
  val userBotId = if (isUserBotChat) com.example.bot.BotManager.botIdOf(chatId) else ""
  val isBotChat = isBotManagerChat || isUserBotChat || currentChat?.isBot == true
  var botStarted by remember(chatId) { mutableStateOf(com.example.bot.BotManager.isStarted(context, chatId)) }
  var showBotMenu by remember(chatId) { mutableStateOf(false) }
  var showReplyKb by remember(chatId) { mutableStateOf(true) }
  var botUsers by remember(chatId) { mutableStateOf<Int?>(null) }
  var botBlocked by remember(chatId) { mutableStateOf(false) }
  val botKbd = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
  val botFocus = androidx.compose.ui.platform.LocalFocusManager.current
  val botUriHandler = androidx.compose.ui.platform.LocalUriHandler.current
  var botCommands by remember(chatId) { mutableStateOf(if (isBotManagerChat) com.example.bot.BotManager.defaultCommands else emptyList<com.example.bot.BotManager.Command>()) }
  LaunchedEffect(chatId, currentChat?.isBot) {
      // أوامر أي بوت (غير المدير) تُجلب من Supabase: معرّف البوت = participantIds للمحادثة
      if (isBotManagerChat) {
          // البوت الرسمي: أوامره من قاعدة البيانات، والمدمجة احتياطية إذا فشل الجلب
          val fromDb = withContext(Dispatchers.IO) { com.example.bot.BotManager.fetchCommands(com.example.bot.BotManager.OFFICIAL_ID) }
          if (fromDb.isNotEmpty()) botCommands = fromDb
      } else if (isUserBotChat || currentChat?.isBot == true) {
          val botId = if (isUserBotChat) userBotId else (currentChat?.participantIds ?: return@LaunchedEffect)
          botCommands = withContext(Dispatchers.IO) { com.example.bot.BotManager.fetchCommands(botId) }
      }
  }

  val markChatAsReadLocally: suspend () -> Unit = {
    try {
        val existingChat = chatDao.getChatById(chatId)
        if (existingChat != null) {
            chatDao.insert(existingChat.copy(unreadCount = 0))
        }
        val existingCachedChat = cachedChatDao.getCachedChatById(chatId)
        if (existingCachedChat != null) {

cachedChatDao.insertCachedChats(listOf(existingCachedChat.copy(unread_count = 0)))
        }
     } catch(e: Exception) {
     }
  }

    // الدردشة المفتوحة: ما نزيدوش عداد "غير مقروء" لرسائلها، وعند الخروج نصفّروه محلياً
    androidx.compose.runtime.DisposableEffect(chatId) {
        com.example.ui.GlobalAppState.openChatId = chatId
        onDispose {
            if (com.example.ui.GlobalAppState.openChatId == chatId) {
                com.example.ui.GlobalAppState.openChatId = null
            }
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch { markChatAsReadLocally() }
        }
    }

    var messages by remember(chatId) { 
        mutableStateOf<List<MessageModel>>(
            com.example.AppState.chatMessagesCache[chatId] ?: MessageListCache.loadMessages(context, chatId) ?: emptyList()
        ) 
    }
    androidx.compose.runtime.LaunchedEffect(messages) {
        if (messages.isNotEmpty()) {
            MessageListCache.saveMessages(context, chatId, messages)
        }
        com.example.AppState.chatMessagesCache[chatId] = messages
    }

    // تشخيص مؤقت: نراقب كل تحديث للرسائل، وإذا لقينا رسالة isMine ما يطابقش
    // sender_id الحقيقي تاعها، نبين تحذير فوري على الشاشة (باش نمسك اللحظة بالضبط)
    androidx.compose.runtime.LaunchedEffect(messages) {
        val realMyId = com.example.supabase.auth.currentSessionOrNull()?.user?.id
        if (realMyId != null) {
            messages.forEach { m ->
                if (m.senderId.isNotBlank() && (m.senderId == realMyId) != m.isMine) {
                    android.widget.Toast.makeText(
                        context,
                        "⚠️ تشخيص: رسالة '${m.text.take(15)}' جهتها غلط! senderId=${m.senderId.take(6)} isMine=${m.isMine}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    var firstUnreadIndex by remember { mutableStateOf<Int?>(null) }
    var previewImageId by remember { mutableStateOf<String?>(null) }
    var showChannelInfo by remember { mutableStateOf(false) }
    var unreadNewMessagesCount by remember { mutableIntStateOf(0) }
    var isNetworkFetchComplete by remember { mutableStateOf(false) }
    var myReadMessageIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var myProfile by remember { mutableStateOf<Profile?>(null) }
    var otherUserProfile by remember { mutableStateOf<Profile?>(null) }
    var isOnline by remember { mutableStateOf(false) }
    var lastSeen by remember { mutableStateOf<String?>(null) }
    var otherUserId by remember { mutableStateOf<String?>(null) }
    val blockedByMeSet by com.example.ui.BlockManager.blockedByMe.collectAsState()
    val blockedMeSet by com.example.ui.BlockManager.blockedMe.collectAsState()
    val amIBlocked = otherUserId?.let { it in blockedMeSet } == true
    val haveIBlocked = otherUserId?.let { it in blockedByMeSet } == true
    var liveTime by remember { mutableStateOf(java.time.Instant.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            liveTime = java.time.Instant.now()
            kotlinx.coroutines.delay(10_000)
        }
    }

    androidx.compose.runtime.DisposableEffect(chatId) {
      com.example.AppState.currentChatId = chatId
      onDispose {
        if (com.example.AppState.currentChatId == chatId) {
            com.example.AppState.currentChatId = null
        }
      }
    }

    // Cache loaded in the main LaunchedEffect

    var myUserId by remember { mutableStateOf<String?>(null) }
    var isChannelAdmin by remember { mutableStateOf(false) }
    var channelSubscriberCount by remember { mutableStateOf<Int?>(null) }

  val typingScope = rememberCoroutineScope()
  var typingJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
  var lastTypingSentTime by remember { mutableStateOf(0L) }
  var globalTypingChannel by remember { mutableStateOf<io.github.jan.supabase.realtime.RealtimeChannel?>(null) }
  var activeTypingChannel by remember {
mutableStateOf<io.github.jan.supabase.realtime.RealtimeChannel?>(null) }
   fun sendTypingStatus(isTypingNow: Boolean) {
     if (myUserId == null) return
     val channelToUse = activeTypingChannel ?: return
     val globalChan = globalTypingChannel
     typingScope.launch(kotlinx.coroutines.Dispatchers.IO) {
         try {
            channelToUse.broadcast(event = "typing", message = TypingEvent(myUserId!!, chatId, isTypingNow))
            globalChan?.broadcast(event = "typing", message = TypingEvent(myUserId!!, chatId, isTypingNow))
         } catch (e: Exception) {
            e.printStackTrace()
         }
     }
   }

  fun onUserType() {
    val now = System.currentTimeMillis()
    if (now - lastTypingSentTime > 1000L) {
        lastTypingSentTime = now
        sendTypingStatus(true)
    }
    typingJob?.cancel()
    typingJob = typingScope.launch {
        kotlinx.coroutines.delay(3000)
        sendTypingStatus(false)
        lastTypingSentTime = 0L
    }
  }

  LaunchedEffect(messageText) {
    kotlinx.coroutines.delay(500)
    try {
        chatDao.updateDraft(chatId, messageText)
    } catch(e: Exception) {}
  }
  
  // ===== إحصائيات القناة الحقيقية (مشاهدات + تفاعلات) من message_reads و reactions — بنفس تصميم بطاقة المنشور =====
  LaunchedEffect(isChannel, chatId, messages.size) {
    if (!isChannel) return@LaunchedEffect
    while (true) {
      try {
        val ids = messages.map { it.id }.takeLast(100)
        if (ids.isNotEmpty()) {
          val reads = com.example.supabase.postgrest["message_reads"]
            .select() { filter { isIn("message_id", ids) } }.decodeList<MessageReadRow>()
          val reacts = com.example.supabase.postgrest["reactions"]
            .select() { filter { isIn("message_id", ids) } }.decodeList<com.example.ui.ReactionRow>()
          val viewsBy = reads.groupBy { it.message_id }.mapValues { e -> e.value.mapNotNull { r -> r.user_id }.distinct().size }
          val reactBy = reacts.groupBy { it.message_id }
          val updated = messages.map { m ->
            val v = viewsBy[m.id] ?: 0
            val rs = reactBy[m.id].orEmpty()
            val grouped = rs.groupBy { r -> r.emoji }.map { e -> ChannelReaction(e.key, e.value.size) }
            val total = rs.size
            m.copy(
              viewsLabel = if (v > 0) formatReactionCount(v) else m.viewsLabel,
              channelReactions = grouped,
              totalInteractionsLabel = if (total > 0) formatReactionCount(total) else null
            )
          }
          if (updated != messages) messages = updated
        }
      } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
      } catch (e: Exception) {}
      kotlinx.coroutines.delay(15000)
    }
  }

  LaunchedEffect(chatId) {
    if (chatId.startsWith("dummy_")) {
        messages = listOf(
            MessageModel(id = "m1", text = "Welcome to the dummy chat!", time = "10:00", isMine = false),
            MessageModel(id = "m2", text = "This is a placeholder UI.", time = "10:01", isMine = false)
        )
        return@LaunchedEffect
    }
    if (chatId == com.example.bot.BotManager.CHAT_ID || isUserBotChat) return@LaunchedEffect // محادثة بوت: لا تمر برسائل المستخدمين العادية
    
    val currentUserId = com.example.supabase.auth.currentSessionOrNull()?.user?.id
    if (currentUserId != null) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            com.example.ui.PinnedMessagesManager.load(chatId, currentUserId)
        }
    }
    // Load cache first
    val initialMessagesSnapshot = messages.toList()
    val hasLocalMessages = initialMessagesSnapshot.isNotEmpty()
    
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
       try {
          if (!hasLocalMessages) {
              val cachedList = cachedMessageDao.getMessagesForChat(chatId).first()
              if (cachedList.isNotEmpty()) {
              val mapped = cachedList.map {
                val attachments = if (it.media_url != null && it.message_type != null) {
                    val type = when(it.message_type) {
                        "image" -> AttachmentType.IMAGE
                        "video" -> AttachmentType.VIDEO
                        "voice" -> AttachmentType.VOICE
                        else -> AttachmentType.DOCUMENT
                    }
                    listOf(Attachment(messageId = it.id, type = type, url = it.media_url, thumbnailUrl = it.thumbnail_url, aspectRatio = it.media_aspect_ratio))
                } else {
                    emptyList()
                }
                MessageModel(
                    id = it.id,
                    text = it.content,
                    time = formatTimeSafe(it.created_at),
                    isMine = it.sender_id == currentUserId,
                    senderName = if (it.sender_id == currentUserId) "" else name,
                    attachments = attachments,
                    replyToId = it.reply_to_id,
                    status = when (it.status) {
                        "SENDING" -> MessageStatus.SENDING
                        "FAILED" -> MessageStatus.FAILED
                        "READ" -> MessageStatus.READ
                        "DELIVERED" -> MessageStatus.DELIVERED
                        else -> MessageStatus.SENT
                    },
                    createdAtExact = it.created_at,
                    mediaGroupId = it.media_group_id
                )
              }
              val linkedMapped = mapped.map { msg ->
                if (msg.replyToId != null) {
                    val replyTarget = mapped.find { it.id == msg.replyToId }
                    msg.copy(replyTo = replyTarget)
                } else msg
              }
              kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                messages = linkedMapped
              }
          }
          }
       } catch(e: Exception) { e.printStackTrace() }
    }

     kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
       try {
           val session = com.example.supabase.auth.currentSessionOrNull()
           val uid = session?.user?.id
           if (uid != null && !isChannel) {
               val members = com.example.supabase.postgrest["chat_members"].select() {
                   filter { eq("chat_id", chatId) }
               }.decodeList<ChatMemberRow>()
               val otherMember = members.firstOrNull { it.user_id != uid }
               if (otherMember != null && otherMember.user_id != null) {
                   val profile = com.example.supabase.postgrest["profiles"].select() {
                       filter { eq("id", otherMember.user_id) }
                   }.decodeSingleOrNull<Profile>()

                   // نحدّث otherUserId فوراً هنا (بلا انتظار فحوصات الحظر تحت)، حتى يشتغل
                   // زر البروفايل من أول نقرة بدل ما ينتظر جولتين إضافيتين من الشبكة.
                   kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                       otherUserProfile = profile
                       isOnline = profile?.isOnline == true
                       lastSeen = profile?.lastSeen
                       otherUserId = otherMember.user_id
                   }

                   val currentUserId = supabase.auth.currentUserOrNull()?.id
                   var myProf: Profile? = null
                   

                   if (currentUserId != null) {
                       myProf = supabase.postgrest["profiles"].select { filter { eq("id",
currentUserId) } }.decodeSingleOrNull<Profile>()
                       
                   }

                   kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                       myProfile = myProf
                       
                   }
             }
          }
       } catch(e: Exception) { e.printStackTrace() }

    try {
       // Load draft
       val existingChat = chatDao.getChatById(chatId)
       if (existingChat != null && existingChat.draft.isNotBlank() && messageText.isBlank())
{
           messageText = existingChat.draft
       }

       val session = com.example.supabase.auth.currentSessionOrNull()
       myUserId = session?.user?.id
       if (myUserId == null) return@withContext

       if (isChannel) {
           try {
               val myMembership = com.example.supabase.postgrest["chat_members"].select() {
                   filter { eq("chat_id", chatId); eq("user_id", myUserId!!) }
               }.decodeSingleOrNull<ChatMemberRow>()
               val subCount = com.example.supabase.postgrest["chat_members"]
                   .select(io.github.jan.supabase.postgrest.query.Columns.list("user_id")) {
                       filter { eq("chat_id", chatId) }
                       count(io.github.jan.supabase.postgrest.query.Count.EXACT)
                   }.countOrNull() ?: 0L
               kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                   isChannelAdmin = myMembership?.role == "admin"
                   channelSubscriberCount = subCount.toInt()
               }
           } catch (e: Exception) { e.printStackTrace() }
       }

       val currentLocalMessages = messages.toList()
       val prefs = context.getSharedPreferences("chat_cleared_times", android.content.Context.MODE_PRIVATE)
       val clearedAtMs = prefs.getLong(chatId, 0L)
       val incrementalMessages = com.example.supabase.postgrest["messages"]
           .select() {
              filter { 
                  eq("chat_id", chatId)
                  val lastKnownExactTime = currentLocalMessages.maxByOrNull { it.createdAtExact ?: "" }?.createdAtExact
                  if (lastKnownExactTime != null) {
                      gt("created_at", lastKnownExactTime)
                  } else if (clearedAtMs > 0) {
                      gt("created_at", java.time.Instant.ofEpochMilli(clearedAtMs).toString())
                  }
              }
              order("created_at", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
           }
           .decodeList<MessageRow>()

       // طبقة أمان ضد الرسائل الناقصة: الجلب فوق ياخذ بس الأحدث من آخر رسالة معروفة، فإذا وصلت رسالة
       // قديمة (إشعار وصل لكن الرسالة ما دخلتش للقائمة) ما كانت تتجلب أبداً. هنا نراجع آخر 60 رسالة
       // ونزيدو أي واحدة ناقصة عندنا.
       val recentRows = try {
           com.example.supabase.postgrest["messages"]
               .select() {
                  filter { 
                      eq("chat_id", chatId) 
                      if (clearedAtMs > 0) {
                          gt("created_at", java.time.Instant.ofEpochMilli(clearedAtMs).toString())
                      }
                  }
                  order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                  limit(60)
               }
               .decodeList<MessageRow>()
       } catch (e: kotlinx.coroutines.CancellationException) {
           throw e
       } catch (e: Exception) { emptyList<MessageRow>() }
       val knownMessageIds = currentLocalMessages.map { it.id }.toSet() + incrementalMessages.map { it.id }
       val gapMessages = recentRows.filter { it.id !in knownMessageIds }.sortedBy { it.created_at }
       val remoteMessages = incrementalMessages + gapMessages

       // Fetch reactions
       val allMessageIds = remoteMessages.map { it.id }
          val allReactions = mutableMapOf<String, MutableList<String>>()
          if (allMessageIds.isNotEmpty()) {
              try {
                 val remoteReactions = mutableListOf<com.example.ui.ReactionRow>()
                 kotlinx.coroutines.coroutineScope {
                     val deferreds = allMessageIds.chunked(200).map { chunk ->
                         async {
                             com.example.supabase.postgrest["reactions"]
                                .select() {
                                   filter { isIn("message_id", chunk) }
                                }.decodeList<com.example.ui.ReactionRow>()
                         }
                     }
                     for (d in deferreds) {
                         remoteReactions.addAll(d.await())
                     }
                 }
                 for (r in remoteReactions) {
                    allReactions.getOrPut(r.message_id) { mutableListOf() }.add(r.emoji)
                 }
              } catch (e: kotlinx.coroutines.CancellationException) {
                 throw e
              } catch(e: Exception) {}
          }

          val localUserId = myUserId
          // رسائلي (الجديدة + المحمّلة مسبقاً من الكاش): نراجع من السيرفر أيها قرأها الطرف الآخر
          val myMessageIds = (
              currentLocalMessages.filter { it.isMine && it.createdAtExact != null }.map { it.id } +
              remoteMessages.filter { it.sender_id == localUserId }.map { it.id }
          ).distinct().takeLast(150)
          val readMessageIds = mutableSetOf<String>()
          if (myMessageIds.isNotEmpty() && localUserId != null) {
              try {
                 val reads = mutableListOf<MessageReadRow>()
                 kotlinx.coroutines.coroutineScope {
                     val deferreds = myMessageIds.chunked(200).map { chunk ->
                         async {
                             com.example.supabase.postgrest["message_reads"]
                                .select() {
                                   filter {
                                       isIn("message_id", chunk)
                                       neq("user_id", localUserId)
                                   }
                                }.decodeList<MessageReadRow>()
                         }
                     }
                     for (d in deferreds) {
                         reads.addAll(d.await())
                     }
                 }
                 readMessageIds.addAll(reads.map { it.message_id })
              } catch (e: kotlinx.coroutines.CancellationException) {
                 throw e
              } catch(e: Exception) { e.printStackTrace() }
          }

          // كل رسائل الطرف الآخر (الجديدة + اللي كانت محمّلة مسبقاً في الكاش من القائمة الرئيسية)
          // لازم تتسجّل "مقروءة" في السيرفر — وإلا تبقى تتحسب غير مقروءة في القائمة (الرقم يبقى 5 مثلاً)
          val otherMessagesIds = (
              currentLocalMessages.filter { !it.isMine && it.createdAtExact != null }.map { it.id } +
              remoteMessages.filter { it.sender_id != localUserId }.map { it.id }
          ).distinct().takeLast(150)

          val myPreviousReads = try {
             val reads = mutableListOf<String>()
             kotlinx.coroutines.coroutineScope {
                 val deferreds = otherMessagesIds.chunked(200).map { chunk ->
                     async {
                         com.example.supabase.postgrest["message_reads"]
                            .select() {
                               filter {
                                   isIn("message_id", chunk)
                                   eq("user_id", localUserId!!)
                               }
                            }.decodeList<MessageReadRow>().map { it.message_id }
                     }
                 }
                 for (d in deferreds) {
                     reads.addAll(d.await())
                 }
             }
             reads.toSet()
          } catch (e: kotlinx.coroutines.CancellationException) {
             throw e
          } catch (e: Exception) { emptySet<String>() }
      myReadMessageIds = myPreviousReads

      if (otherMessagesIds.isNotEmpty() && localUserId != null) {
          kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                markChatAsReadLocally()
                val unreadIds = otherMessagesIds.filter { it !in myPreviousReads }
                if (unreadIds.isNotEmpty()) {
                    val readsToInsert = unreadIds.map { MessageReadRow(it, localUserId) }
                    com.example.supabase.postgrest["message_reads"].upsert(readsToInsert)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch(e: Exception) { e.printStackTrace() }
          }
      }

      var fetchedMsgs = remoteMessages.map { row ->
        val isMine = row.sender_id == localUserId
        val timeStr = formatTimeSafe(row.created_at)

         val attachments = if (row.media_url != null && row.message_type != null) {
            val type = when(row.message_type) {
                "image" -> AttachmentType.IMAGE
                "video" -> AttachmentType.VIDEO
                "voice" -> AttachmentType.VOICE
                else -> AttachmentType.DOCUMENT
            }
            listOf(Attachment(messageId = row.id, type = type, url = row.media_url, thumbnailUrl = row.thumbnail_url, aspectRatio = row.media_aspect_ratio))
         } else {
            emptyList()
         }

         MessageModel(
           id = row.id,
           text = row.content,
           time = timeStr,
           isMine = isMine,
           senderName = if (isMine) "" else name,
           attachments = attachments,
           replyToId = row.reply_to_id,
           reactions = allReactions[row.id] ?: emptyList(),
           status = if (isMine && readMessageIds.contains(row.id)) MessageStatus.READ else MessageStatus.SENT,
           createdAtExact = row.created_at,
           mediaGroupId = row.media_group_id,
           isEdited = row.edited_at != null
         )
       }

      // Link replyTo objects
       val fullList = currentLocalMessages + fetchedMsgs
       fetchedMsgs = fetchedMsgs.map { msg ->
          if (msg.replyToId != null) {
              val replyTarget = fullList.find { it.id == msg.replyToId }
              msg.copy(replyTo = replyTarget)
          } else msg
       }

              // ندمج الرسائل الجديدة/الناقصة في مكانها الزمني الصحيح، بدون إعادة ترتيب الباقي
              // (رسائلي اللي لسه ما عندهاش وقت السيرفر تبقى في مكانها)
              val mergedByTime = currentLocalMessages.toMutableList().also { list ->
                  for (incoming in fetchedMsgs) {
                      if (list.any { it.id == incoming.id }) continue
                      val incomingTime = parseTimestampSafe(incoming.createdAtExact)
                      var idx = list.size
                      while (idx > 0) {
                          val prevTime = list[idx - 1].createdAtExact?.let { parseTimestampSafe(it) }
                          if (prevTime == null || prevTime <= incomingTime) break
                          idx--
                      }
                      list.add(idx, incoming)
                  }
              }
              // حالة القراءة: أي رسالة لي قرأها الطرف الآخر تصير READ (ولا ننزّل READ لـ SENT أبداً)
              val finalMergedMessages = mergedByTime.map { m ->
                  if (m.isMine && m.status != MessageStatus.READ && m.id in readMessageIds) m.copy(status = MessageStatus.READ) else m
              }

       val firstUnreadNew = fetchedMsgs.firstOrNull { !it.isMine && it.id !in myPreviousReads }
       if (firstUnreadNew != null) {
           val unreadIdx = finalMergedMessages.indexOfFirst { it.id == firstUnreadNew.id }
           if (unreadIdx >= 0) {
               firstUnreadIndex = unreadIdx + 1 // بقيت +1 (كانت +2) لأننا شلنا كارد "Not a contact" اللي كان item إضافي فوق القائمة
               unreadNewMessagesCount = fetchedMsgs.count { !it.isMine && it.id !in myPreviousReads }
           }
       }
       isNetworkFetchComplete = true
          
       kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
           messages = finalMergedMessages
       }

       // messages updated via Room collect

       try {
          val toCache = fetchedMsgs.map { msg ->
             val remoteMsg = remoteMessages.find { it.id == msg.id }
             com.example.cache.CachedMessage(
                id = msg.id,
                chat_id = msg.chatId,
                sender_id = msg.senderId.ifBlank { remoteMsg?.sender_id ?: "" },
                content = msg.text,
                created_at = remoteMsg?.created_at ?: "",
                status = if (msg.status == MessageStatus.READ) "READ" else "SENT",
                message_type = remoteMsg?.message_type,
                media_url = remoteMsg?.media_url,
                thumbnail_url = remoteMsg?.thumbnail_url,
                reply_to_id = msg.replyToId,
                media_aspect_ratio = remoteMsg?.media_aspect_ratio,
                media_group_id = remoteMsg?.media_group_id
             )
          }
          cachedMessageDao.insertCachedMessages(toCache)
          if (readMessageIds.isNotEmpty()) {
              readMessageIds.toList().chunked(500).forEach { cachedMessageDao.markMessagesRead(it) }
          }
       } catch(e: Exception) {}

    } catch (e: kotlinx.coroutines.CancellationException) {
       throw e
    } catch (e: Exception) {
       e.printStackTrace()
       // Just for debugging
    }
    }
  }

  var realtimeError by remember { mutableStateOf<String?>(null) }

  DisposableEffect(chatId, myUserId) {
    if (chatId.startsWith("dummy_")) return@DisposableEffect onDispose {}
    if (myUserId == null) return@DisposableEffect onDispose {}
     val chatChannel = com.example.supabase.channel("room_$chatId")
     val globalChan = com.example.supabase.channel("global_typing")
     globalTypingChannel = globalChan

     activeTypingChannel = chatChannel
     val handler = kotlinx.coroutines.CoroutineExceptionHandler { _, _ -> }
     val job = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO +
handler).launch {
       try {
           // Typing flow
           chatChannel.broadcastFlow<TypingEvent>("typing")
               .onEach { action ->
                  if (action.user_id != myUserId) {
                      kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        isTyping = action.is_typing
                      }
                  }
               }
               .launchIn(this)

            // Message edits made by the other side (or from another device)
            val msgUpdateFlow = chatChannel.postgresChangeFlow<io.github.jan.supabase.realtime.PostgresAction.Update>(schema = "public") {
                table = "messages"
            }
            msgUpdateFlow.onEach { action ->
                try {
                    val rec = action.decodeRecord<MessageRow>()
                    if (rec.chat_id == chatId) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            messages = messages.map {
                                if (it.id == rec.id && (it.text != rec.content || (rec.edited_at != null && !it.isEdited)))
                                    it.copy(text = rec.content, isEdited = rec.edited_at != null || it.isEdited)
                                else it
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ChatDetail", "message update decode failed", e)
                }
            }.launchIn(this)

            // Pins made by the other side: reload immediately (no need to leave and re-enter the chat)
            val pinInsFlow = chatChannel.postgresChangeFlow<io.github.jan.supabase.realtime.PostgresAction.Insert>(schema = "public") {
                table = "pinned_messages"
            }
            val pinDelFlow = chatChannel.postgresChangeFlow<io.github.jan.supabase.realtime.PostgresAction.Delete>(schema = "public") {
                table = "pinned_messages"
            }
            pinInsFlow.onEach {
                val uid = com.example.supabase.auth.currentSessionOrNull()?.user?.id
                if (uid != null) com.example.ui.PinnedMessagesManager.load(chatId, uid)
            }.launchIn(this)
            pinDelFlow.onEach {
                val uid = com.example.supabase.auth.currentSessionOrNull()?.user?.id
                if (uid != null) com.example.ui.PinnedMessagesManager.load(chatId, uid)
            }.launchIn(this)

            // Reactions flow
            val reactionsFlow = chatChannel.postgresChangeFlow<io.github.jan.supabase.realtime.PostgresAction>(schema = "public") {
               table = "reactions"
            }
            reactionsFlow.onEach { action ->
               try {
                  when (action) {
                     is io.github.jan.supabase.realtime.PostgresAction.Insert -> {
                         val record = action.decodeRecord<com.example.ui.ReactionRow>()
                         kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                           messages = messages.map {
                               if (it.id == record.message_id &&
!it.reactions.contains(record.emoji)) {
                                   it.copy(reactions = it.reactions + record.emoji)
                               } else it
                           }
                         }
                     }
                     is io.github.jan.supabase.realtime.PostgresAction.Delete -> {
                       val record = kotlinx.serialization.json.Json{ ignoreUnknownKeys = true
}.decodeFromJsonElement(com.example.ui.ReactionRow.serializer(), action.oldRecord)
                       kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                         messages = messages.map {
                             if (it.id == record.message_id &&
it.reactions.contains(record.emoji)) {
                                 val newList = it.reactions.toMutableList()
                                 newList.remove(record.emoji)
                                 it.copy(reactions = newList)
                             } else it
                         }
                       }
                    }
                    else -> {}
                 }
              } catch(e: Exception) {}
           }.launchIn(this)

              // Messages deleted on the server (e.g. "delete for everyone" by the other user)
            val msgDeleteFlow = chatChannel.postgresChangeFlow<io.github.jan.supabase.realtime.PostgresAction.Delete>(schema = "public") {
                table = "messages"
            }
            msgDeleteFlow.onEach { action ->
                try {
                    val deletedId = action.oldRecord["id"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null }
                    if (deletedId != null) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            if (messages.any { it.id == deletedId }) {
                                messages = messages.filter { it.id != deletedId }
                            }
                        }
                    }
                } catch (e: Exception) {}
            }.launchIn(this)

            // Messages flow
              val flow = chatChannel.postgresChangeFlow<PostgresAction.Insert>(schema =
"public") {
                table = "messages"
                filter("chat_id", io.github.jan.supabase.postgrest.query.filter.FilterOperator.EQ,
chatId)
           }
           flow.onEach { action ->
              try {
                 val record = action.decodeRecord<MessageRow>()
                 kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    val tempMatch = messages.find { it.status == MessageStatus.SENDING
&& it.text == record.content && it.isMine }
                    if (tempMatch != null) {
                        messages = messages.map { if (it.id == tempMatch.id) it.copy(id =
record.id, status = MessageStatus.SENT, time = formatTimeSafe(record.created_at)) else it }
                    } else {
                        val alreadyExists = messages.any { it.id == record.id }
                        if (!alreadyExists) {
                            val isMine = record.sender_id == myUserId
                            val localUserId = myUserId
                            if (!isMine && localUserId != null) {

kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            try {
                               markChatAsReadLocally()
com.example.supabase.postgrest["message_reads"].upsert(MessageReadRow(record.id,
localUserId))
                         } catch(e: Exception) {}
                     }
                   }
                   val timeStr = formatTimeSafe(record.created_at)

                    val attachments = if (record.media_url != null &&
record.message_type != null) {
                       val type = when(record.message_type) {
                           "image" -> AttachmentType.IMAGE
                           "video" -> AttachmentType.VIDEO
                           "voice" -> AttachmentType.VOICE
                           else -> AttachmentType.DOCUMENT
                       }
                       listOf(Attachment(messageId = record.id, type = type, url =
record.media_url, thumbnailUrl = record.thumbnail_url, aspectRatio = record.media_aspect_ratio))
                    } else {
                       emptyList()
                    }

                        val replyTarget = if (record.reply_to_id != null) messages.find { it.id ==
record.reply_to_id } else null
                        val newMsg = MessageModel(
                          id = record.id,
                          text = record.content,
                          time = timeStr,
                          isMine = isMine,
                          senderName = if (isMine) "" else name,
                          replyToId = record.reply_to_id,
                          replyTo = replyTarget,
                          attachments = attachments,
                          status = MessageStatus.SENT,
                          createdAtExact = record.created_at,
                          mediaGroupId = record.media_group_id,
                          isEdited = record.edited_at != null
                        )
                        messages = (messages + newMsg).distinctBy { it.id }

                        // Optimistic update of local chat lists (for received messages)
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            try {
                                val textStr = if (record.media_url != null) {
                                    if (record.message_type == "image") "[Photo]" else "[Attachment]"
                                } else record.content
                                val existingChat = chatDao.getChatById(record.chat_id)
                                if (existingChat != null) {
                                    chatDao.insert(existingChat.copy(
                                        message = textStr,
                                        time = timeStr,
                                        timestamp = System.currentTimeMillis(),
                                        unreadCount = if (isMine) existingChat.unreadCount else 0,
                                        isMine = isMine,
                                        isReadReceipt = false,
                                        lastMediaType = record.message_type,
                                        lastMediaUrl = record.media_url,
                                        lastThumbnailUrl = record.thumbnail_url
                                    ))
                                }
                                val existingCachedChat = cachedChatDao.getCachedChatById(record.chat_id)
                                if (existingCachedChat != null) {
                                    cachedChatDao.insertCachedChats(listOf(existingCachedChat.copy(
                                        last_message = textStr,
                                        time_str = timeStr,
                                        timestamp = System.currentTimeMillis(),
                                        unread_count = if (isMine) existingCachedChat.unread_count else existingCachedChat.unread_count + 1
                                    )))
                                }
                            } catch(e: Exception) {}
                        }

                        try {
                           cachedMessageDao.insertCachedMessage(
                              com.example.cache.CachedMessage(
                                id = record.id,
                                chat_id = record.chat_id,
                                sender_id = record.sender_id,
                                content = record.content,
                                created_at = record.created_at,
                                status = "SENT",
                                message_type = record.message_type,
                                media_url = record.media_url,
                                thumbnail_url = record.thumbnail_url,
                                reply_to_id = record.reply_to_id,
                                media_aspect_ratio = record.media_aspect_ratio,
                                media_group_id = record.media_group_id
                               )
                            )
                         } catch(e: Exception) {}
                     }
                 }
               }
            } catch (e: Exception) {
               e.printStackTrace()
               kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                  realtimeError = "فشل في تحديث الرسائل"
               }
            }
         }.launchIn(this)
         val readsFlow = chatChannel.postgresChangeFlow<io.github.jan.supabase.realtime.PostgresAction.Insert>(schema = "public") {
            table = "message_reads"
         }

        readsFlow.onEach { action ->
           try {
              val record = action.decodeRecord<MessageReadRow>()
              if (record.user_id != myUserId) {
                  kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    messages = messages.map {
                        if (it.id == record.message_id) it.copy(status = MessageStatus.READ)
else it
                    }
                  }
              } else {
                  kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    myReadMessageIds = myReadMessageIds + record.message_id
                  }
              }
           } catch (e: Exception) { e.printStackTrace() }
        }.launchIn(this)

         // Profiles flow (for online status)
         val profileFlow = chatChannel.postgresChangeFlow<io.github.jan.supabase.realtime.PostgresAction.Update>(schema = "public") {
            table = "profiles"
         }
         profileFlow.onEach { action ->
             try {
                val record = action.decodeRecord<Profile>()
                if (record.id == otherUserId) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                      isOnline = record.isOnline == true
                      lastSeen = record.lastSeen
                    }
                }
             } catch(e: Exception) {
             }
          }.launchIn(this)

           com.example.supabase.realtime.connect()

           chatChannel.subscribe(blockUntilSubscribed = true)
           globalChan.subscribe(blockUntilSubscribed = true)
           kotlinx.coroutines.awaitCancellation()
        } catch (e: Exception) {
           if (e !is kotlinx.coroutines.CancellationException) {
               e.printStackTrace()
               kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                  realtimeError = "انقطع الاتصال بالخادم"
               }
           }
        }
     }

      onDispose {
        if (activeTypingChannel == chatChannel) activeTypingChannel = null
        globalTypingChannel = null
        job.cancel()
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
               com.example.supabase.realtime.removeChannel(chatChannel)
               // Do NOT remove globalChan, ChatListScreen relies on it
            } catch(e: Exception) {}
        }
      }
  }

  var activeFilter by remember { mutableStateOf<String?>(null) }
  var replyingTo by remember { mutableStateOf<MessageModel?>(null) }
  var viewingImageUrls by remember { mutableStateOf<List<String>?>(null) }
   var viewingImageInitialIndex by remember { mutableStateOf(0) }
   var editingMessage by remember { mutableStateOf<MessageModel?>(null) }
   var selectedMessages by remember { mutableStateOf(setOf<String>()) }
   val isSelectionMode = selectedMessages.isNotEmpty()
   val allPinnedMap by com.example.ui.PinnedMessagesManager.pinnedMessages.collectAsState()
   val pinnedRows = allPinnedMap[chatId] ?: emptyList()
   val pinnedRowIds = remember(pinnedRows) { pinnedRows.map { it.message_id }.toSet() }
   val pinnedMessages = remember(pinnedRows, messages) {
       messages.filter { it.id in pinnedRowIds }.map { it.copy(isPinned = true) }
   }
   LaunchedEffect(pinnedRowIds, messages) {
       if (messages.any { (it.id in pinnedRowIds) != it.isPinned }) {
           messages = messages.map { it.copy(isPinned = it.id in pinnedRowIds) }
       }
   }
   val savedMessageIds by com.example.ui.SavedMessagesManager.saved.collectAsState()
   LaunchedEffect(savedMessageIds, messages) {
       if (messages.any { (it.id in savedMessageIds) != it.isSaved }) {
           messages = messages.map { it.copy(isSaved = it.id in savedMessageIds) }
       }
   }
   var pinDialogMessage by remember { mutableStateOf<MessageModel?>(null) }
   var deleteDialogIds by remember { mutableStateOf<Set<String>?>(null) }
   LaunchedEffect(chatId) {
       while (true) {
           kotlinx.coroutines.delay(4000)
           val uid = com.example.supabase.auth.currentSessionOrNull()?.user?.id
           if (uid != null) com.example.ui.PinnedMessagesManager.load(chatId, uid)
       }
   }
   val hiddenMessageIds by com.example.ui.MessageDeletionManager.hidden.collectAsState()
   androidx.compose.runtime.LaunchedEffect(messages, hiddenMessageIds) {
       if (hiddenMessageIds.isNotEmpty() && messages.any { it.id in hiddenMessageIds }) {
           messages = messages.filter { it.id !in hiddenMessageIds }
       }
   }
   var currentPinIndex by remember { androidx.compose.runtime.mutableIntStateOf(0) }
   var showForwardDialog by remember { mutableStateOf<MessageModel?>(null) }
  var topMenuExpanded by remember { mutableStateOf(false) }
  var showBlockDialog by remember { mutableStateOf(false) }
  var showUnblockDialog by remember { mutableStateOf(false) }
  var isSearchMode by remember { mutableStateOf(false) }
  var searchQuery by remember { mutableStateOf("") }
  var displayLimit by remember(chatId) {
      // نقرا الموضع المحفوظ من قبل، وإذا كان بعيد (أبعد من آخر 50 رسالة)، نبداو بعدد كافي
      // من البداية باش القائمة تحتوي على موضع القراءة المحفوظ فأول عرض، بدل ما تسقط فحالة
      // "القائمة قصيرة" وتقفز للأسفل بدل ما ترجعك لمكانك.
      val savedIdx = context.getSharedPreferences("chat_scroll_prefs", android.content.Context.MODE_PRIVATE)
          .getInt("scroll_index_$chatId", -1)
      mutableIntStateOf(if (savedIdx > 50) savedIdx + 20 else 50)
  }
  
  val processedMessages = remember(chatId) { 
      androidx.compose.runtime.mutableStateListOf<UiMessage>().apply {
          val initialMsgs = com.example.AppState.chatMessagesCache[chatId] ?: emptyList()
          if (initialMsgs.isNotEmpty()) {
              val filtered = initialMsgs // Assuming no initial activeFilter/searchQuery applied on start, or if so, we can let LaunchedEffect correct it
              val limited = if (filtered.size > displayLimit) filtered.takeLast(displayLimit) else filtered
              val groupedLimited = groupImageMessages(limited)
              addAll(groupedLimited.mapIndexed { index, msg ->
                  val isFirst = index == 0 || groupedLimited[index - 1].isMine != msg.isMine
                  val isLast = index == groupedLimited.lastIndex || groupedLimited[index + 1].isMine != msg.isMine
                  UiMessage(msg, isFirst, isLast)
              })
          }
      }
  }
  
  LaunchedEffect(activeFilter, searchQuery, messages, displayLimit) {
      kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
          val filtered = if (searchQuery.isNotEmpty()) {
              messages.filter { it.text.contains(searchQuery, ignoreCase = true) }
          } else if (activeFilter == null) messages else messages.filter { msg ->
               when (activeFilter) {
                  "Favorites" -> msg.isSaved
                  "Photos" -> msg.text.contains("[Photo]", ignoreCase = true) || msg.text.contains("[Album]", ignoreCase = true) || msg.text.contains("photo", ignoreCase = true)
                  "Videos" -> msg.text.contains("[Video]", ignoreCase = true) || msg.text.contains("video", ignoreCase = true)
                  "Documents" -> msg.text.contains("[Document]", ignoreCase = true) || msg.text.contains("document", ignoreCase = true)
                  "Voice Messages" -> msg.text.contains("[Voice Message]", ignoreCase = true) || msg.text.contains("voice", ignoreCase = true)
                  "Links" -> msg.text.contains("[Link]", ignoreCase = true) || msg.text.contains("http", ignoreCase = true) || msg.text.contains("www.", ignoreCase = true) || msg.text.contains(".com", ignoreCase = true)
                  "Shared Files" -> msg.text.contains("[File]", ignoreCase = true) || msg.text.contains("file", ignoreCase = true)
                  else -> true
               }
          }
             
          val limited = if (filtered.size > displayLimit) filtered.takeLast(displayLimit) else filtered
             
          val groupedLimited = groupImageMessages(limited)
             
          val uiList = groupedLimited.mapIndexed { index, msg ->
              val isFirst = index == 0 || groupedLimited[index - 1].isMine != msg.isMine
              val isLast = index == groupedLimited.lastIndex || groupedLimited[index + 1].isMine != msg.isMine
              UiMessage(msg, isFirst, isLast)
          }
          
          kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
              if (processedMessages.isEmpty() || uiList.isEmpty()) {
                  processedMessages.clear()
                  processedMessages.addAll(uiList)
              } else {
                  val oldIds = processedMessages.map { it.msg.id }
                  val newIds = uiList.map { it.msg.id }
                  
                  if (oldIds == newIds) {
                      for (i in uiList.indices) {
                          if (processedMessages[i] != uiList[i]) {
                              processedMessages[i] = uiList[i]
                          }
                      }
                  } else if (newIds.containsAll(oldIds)) {
                      val toAdd = newIds - oldIds.toSet()
                      if (toAdd.isNotEmpty() && newIds.take(toAdd.size) == toAdd) {
                          processedMessages.addAll(0, uiList.take(toAdd.size))
                          for (i in toAdd.size until uiList.size) {
                              if (processedMessages[i] != uiList[i]) {
                                  processedMessages[i] = uiList[i]
                              }
                          }
                      } else {
                          processedMessages.clear()
                          processedMessages.addAll(uiList)
                      }
                  } else {
                      processedMessages.clear()
                      processedMessages.addAll(uiList)
                  }
              }
          }
      }
  }
     var selectedMessageForContext by remember { mutableStateOf<MessageModel?>(null) }
  var contextMenuShowFull by remember { mutableStateOf(false) }
  val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
  var toastNotification by remember { mutableStateOf<ToastNotification?>(null) }

  val isDark by com.example.ThemeManager.isDarkMode.collectAsState()

  val __forcedBg = __themeConfig.chatBackground
  val __forcedSurface = __theme.surfaceColor
  val __forcedTextPrimary = __theme.textPrimary
  val __forcedTextSecondary = __theme.textSecondary
  val __forcedAccent = __themeConfig.accent

  val colorScheme = if (__theme.isDark) androidx.compose.material3.darkColorScheme(background=__forcedBg, surface=__forcedSurface, surfaceVariant=__forcedSurface, onBackground=__forcedTextPrimary, onSurface=__forcedTextPrimary, onSurfaceVariant=__forcedTextSecondary, primary=__forcedAccent, onPrimary=androidx.compose.ui.graphics.Color.White) else androidx.compose.material3.lightColorScheme(
      background = __forcedBg,
      surface = __forcedSurface,
      surfaceVariant = __forcedSurface,
      onBackground = __forcedTextPrimary,
      onSurface = __forcedTextPrimary,
      onSurfaceVariant = __forcedTextSecondary,
      primary = __forcedAccent,
      onPrimary = Color.White
  )

    val prefs = context.getSharedPreferences("chat_scroll_prefs", android.content.Context.MODE_PRIVATE)
    val savedIndex = remember(chatId) { prefs.getInt("scroll_index_$chatId", -1) }
    val savedOffset = remember(chatId) { prefs.getInt("scroll_offset_$chatId", 0) }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = if (savedIndex != -1) savedIndex else 0,
        initialFirstVisibleItemScrollOffset = if (savedIndex != -1) savedOffset else 0
    )
    LaunchedEffect(listState) {
        androidx.compose.runtime.snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                prefs.edit()
                    .putInt("scroll_index_$chatId", index)
                    .putInt("scroll_offset_$chatId", offset)
                    .apply()
                if (index < 10 && displayLimit < messages.size) {
                    displayLimit += 50
                }
            }
    }
    var highlightedMessageId by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val replyKeyboardRows = remember(messages) { currentReplyKeyboard(messages) }
    val botScreenH = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp
    val botExtraBottom = when {
        isBotChat && botStarted && showReplyKb && replyKeyboardRows != null ->
            minOf(botScreenH * 0.32f, replyKeyboardRows.size * 58f + 16f).dp
        else -> 0.dp
    }
    fun addLocalBotMessage(text: String, mine: Boolean, imageUri: String? = null, markup: String? = null) {
        val id = java.util.UUID.randomUUID().toString()
        messages = (messages + MessageModel(
            id = id,
            chatId = chatId,
            text = text,
            time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
            isMine = mine,
            senderName = if (mine) "" else name,
            attachments = if (imageUri != null) listOf(Attachment(messageId = id, type = AttachmentType.IMAGE, url = imageUri)) else emptyList(),
            replyMarkup = markup
        )).distinctBy { it.id }
    }
    fun runBotManagerText(raw: String) {
        val t = raw.trim()
        if (t.isEmpty()) return
        addLocalBotMessage(t, mine = true)
        coroutineScope.launch {
            val replies = withContext(Dispatchers.IO) { com.example.bot.BotManager.onText(t) }
            replies.forEachIndexed { i, r ->
                val markup = if (i == replies.lastIndex && t.lowercase().startsWith("/start")) com.example.bot.BotManager.START_MARKUP else null
                addLocalBotMessage(r, mine = false, markup = markup)
            }
        }
    }
    fun runBotManagerImage(uri: android.net.Uri?, type: AttachmentType) {
        addLocalBotMessage("", mine = true, imageUri = uri?.toString())
        coroutineScope.launch {
            val replies = withContext(Dispatchers.IO) {
                com.example.bot.BotManager.onImage(context, uri, type == AttachmentType.IMAGE)
            }
            replies.forEach { addLocalBotMessage(it, mine = false) }
        }
    }
    fun sendToUserBot(text: String, onResult: (Boolean) -> Unit = {}) {
        val t = text.trim()
        if (t.isEmpty()) return
        coroutineScope.launch {
            val mid = withContext(Dispatchers.IO) { com.example.bot.BotManager.sendToBot(userBotId, t) }
            if (mid != null) {
                messages = (messages + MessageModel(
                    id = "bm_$mid", chatId = chatId, text = t,
                    time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                    isMine = true
                )).distinctBy { it.id }
                onResult(true)
            } else {
                toastNotification = ToastNotification(System.currentTimeMillis(), com.example.bot.BotManagerStrings.SEND_FAILED, ToastType.INFO)
                onResult(false)
            }
        }
    }
    fun botSend(text: String) {
        if (isBotManagerChat) runBotManagerText(text) else if (isUserBotChat) sendToUserBot(text)
    }
    // بوتات المستخدمين: مزامنة المحادثة (ردود البوت + الأزرار + تعديل/تحديث الأوامر) كل ثانيتين
    LaunchedEffect(chatId) {
        if (!isUserBotChat) return@LaunchedEffect
        var tick = 0
        while (true) {
            val rows = withContext(Dispatchers.IO) { com.example.bot.BotManager.fetchThread(userBotId) }
            if (rows.isNotEmpty()) {
                val incoming = rows.map { r ->
                    MessageModel(
                        id = "bm_${r.id}", chatId = chatId, text = r.text ?: "",
                        time = formatTimeSafe(r.created_at), timestamp = parseTimestampSafe(r.created_at),
                        isMine = r.direction == "in", senderName = if (r.direction == "in") "" else name,
                        isEdited = r.edited_at != null,
                        replyMarkup = r.reply_markup?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.toString(),
                        createdAtExact = r.created_at
                    )
                }
                val ids = incoming.map { it.id }.toSet()
                val merged = (messages.filterNot { it.id in ids } + incoming).sortedBy { it.timestamp }
                if (merged != messages) messages = merged
            }
            if (tick++ % 10 == 0) {
                botCommands = withContext(Dispatchers.IO) { com.example.bot.BotManager.fetchCommands(userBotId) }
            }
            delay(700)
        }
    }
    LaunchedEffect(chatId, botStarted, botBlocked) {
        if (isBotChat) {
            val bid = if (isBotManagerChat) com.example.bot.BotManager.OFFICIAL_ID else userBotId
            botUsers = withContext(Dispatchers.IO) { com.example.bot.BotManager.fetchUserCount(bid) }
        }
    }
    LaunchedEffect(chatId) {
        if (isUserBotChat) {
            val st = withContext(Dispatchers.IO) { com.example.bot.BotManager.userStatus(userBotId) }
            when (st) {
                "blocked" -> { botBlocked = true; botStarted = false }
                "none" -> botStarted = false
                "active" -> { botStarted = true; com.example.bot.BotManager.markStarted(context, chatId) }
            }
        }
    }
    LaunchedEffect(Unit) {
        RichTextActions.onCopy = { t ->
            try {
                val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cm.setPrimaryClip(android.content.ClipData.newPlainText("text", t))
                toastNotification = ToastNotification(System.currentTimeMillis(), com.example.bot.BotManagerStrings.COPIED_TOAST, ToastType.COPY)
            } catch (e: Exception) { e.printStackTrace() }
        }
        RichTextActions.onNotice = { msg ->
            toastNotification = ToastNotification(System.currentTimeMillis(), msg, ToastType.INFO)
        }
    }
    val botAvatarCtx = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(chatId, currentChat?.avatarUrl) {
        if (isBotChat && currentChat?.avatarUrl == null) {
            val bid = if (isBotManagerChat) com.example.bot.BotManager.OFFICIAL_ID else userBotId
            withContext(Dispatchers.IO) {
                com.example.bot.BotManager.syncBotAvatar(botAvatarCtx, chatId, bid, currentChat?.name ?: name)
            }
        }
    }
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    var wasKeyboardOpen by remember { mutableStateOf(false) }
    val isImeVisible = WindowInsets.isImeVisible
    LaunchedEffect(isImeVisible) { if (isImeVisible) { showReplyKb = false; showBotMenu = false } }

MaterialTheme(colorScheme = colorScheme) {
Box(modifier = Modifier.fillMaxSize()) {
    ChatWallpaper()

    Scaffold(
      // نستثني ime هنا لأنها ستُحتسب مرة واحدة فقط عبر imePadding() تحت — احتسابها هنا
      // وهناك في نفس الوقت كان يضاعف ارتفاع الكيبورد ويترك فراغاً بحجمه فوق الكيبورد الحقيقي.
      contentWindowInsets = WindowInsets.safeDrawing.exclude(WindowInsets.ime),
      containerColor = androidx.compose.ui.graphics.Color.Transparent
    ) { paddingValues ->
    Box(modifier = Modifier.fillMaxSize().padding(paddingValues).consumeWindowInsets(paddingValues).imePadding()) {

       Column(modifier = Modifier.fillMaxSize()) {

         if (realtimeError != null) {
             Text(
               text = realtimeError ?: "",
               color = Color.White,
               fontSize = 14.sp,
               modifier = Modifier
                   .fillMaxWidth()
                   .background(Color.Red)
                   .padding(16.dp)
             )
         }
         Box(modifier = Modifier.weight(1f).fillMaxWidth()) {

       if (name == "ديشو") {
                Box(modifier = Modifier.fillMaxSize().padding(top = 100.dp), contentAlignment =
Alignment.Center) {
                   Column(
                      horizontalAlignment = Alignment.CenterHorizontally,
                      modifier =
Modifier.padding(32.dp).background(MaterialTheme.colorScheme.background,
RoundedCornerShape(16.dp)).padding(24.dp)
                   ){
                      Icon(Icons.Outlined.Block, contentDescription = "Blocked", tint =
Color.Red, modifier = Modifier.size(64.dp))
                      Spacer(modifier = Modifier.height(16.dp))
                      Text("تم حظرك", fontSize = 24.sp, fontWeight = FontWeight.Bold, color =
MaterialTheme.colorScheme.onBackground)
                      Spacer(modifier = Modifier.height(8.dp))
                      Text(com.example.ui.i18n.LocalTranslation.current.blockedByThem,
fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign =
androidx.compose.ui.text.style.TextAlign.Center)
                   }
                }
            } else {
                   ChatMessages(
            messages = processedMessages,
            isChannel = isChannel,
            hasPinnedBanner = pinnedMessages.isNotEmpty(),
            activeContextMenuMessageId = selectedMessageForContext?.id,
            listState = listState,
            savedIndex = savedIndex,
            savedOffset = savedOffset,
            name = name,
            onImageClick = { previewImageId = it },
            selectedMessages = selectedMessages,
            isSelectionMode = isSelectionMode,
            isTyping = isTyping,
            firstUnreadIndex = firstUnreadIndex,
            
                highlightedMessageId = highlightedMessageId,
                onHighlightMessage = { highlightedMessageId = it },
            onInlineButtonClick = { m, b ->
                val link = b.url
                if (link != null) { try { botUriHandler.openUri(link) } catch (e: Exception) {} }
                else if (isBotManagerChat) runBotManagerText(b.callbackData ?: b.text)
                else if (isUserBotChat) {
                    val cd = b.callbackData
                    val mid = m.id.removePrefix("bm_").toLongOrNull()
                    if (cd != null && mid != null) coroutineScope.launch {
                        withContext(Dispatchers.IO) { com.example.bot.BotManager.sendCallback(userBotId, mid, cd) }
                    }
                }
            },
            onToggleSelect = { msgId ->
                selectedMessages = if (msgId in selectedMessages) selectedMessages - msgId
else selectedMessages + msgId
            },
            onReactionSelected = { msg, reaction ->
                val existingReaction = msg.reactions.contains(reaction)
                messages = messages.map { if (it.id == msg.id) it.copy(reactions = if (existingReaction) emptyList() else listOf(reaction)) else it }
                coroutineScope.launch {
                   try {
                      val uid = com.example.supabase.auth.currentSessionOrNull()?.user?.id ?:
                      return@launch
                      // احذف أي رد فعل سابق لهذا المستخدم على هذه الرسالة (بغض النظر عن الإيموجي)
                      com.example.supabase.postgrest["reactions"].delete {
                          filter {
                              eq("message_id", msg.id)
                              eq("user_id", uid)
                          }
                      }
                      if (!existingReaction) {
                          val req = com.example.ui.ReactionInsert(message_id = msg.id, user_id
                          = uid, emoji = reaction)
                          com.example.supabase.postgrest["reactions"].insert(req)
                      }
                      // ملاحظة: تم حذف إعادة القراءة الفورية من السيرفر بعد الكتابة مباشرة —
                      // كانت تسبق تثبيت الـ delete/insert أحياناً وترجع بيانات قديمة، فكانت
                      // تلغي التحديث الفوري فوق وترجع الإيموجي القديم حتى خروج/دخول الشاشة.
                      // التحديث المحلي فوق كافي، وأي تغيير من مستخدم آخر كيوصل عبر realtime.
                 } catch (e: Exception) {
                 }
             }
          },
          onMoreClick = { },
          onLongClick = { msg, bounds, showFull ->
             if (!isSelectionMode) {
                 wasKeyboardOpen = isImeVisible
                 if (isImeVisible) {
                     keyboardController?.hide()
                 }
                 selectedMessageForContext = msg
                 contextMenuShowFull = showFull
                 // خلي أعلى الرسالة يوصل لثلث الشاشة الأعلى دايماً، باش تبان كاملة فوق القائمة والإيموجيات
                 val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
                 val targetTopPx = screenHeightPx * 0.32f
                 if (bounds.top > targetTopPx) {
                     val scrollAmount = bounds.top - targetTopPx
                     coroutineScope.launch {
                         listState.animateScrollBy(scrollAmount)
                     }
                 }
             }
          },
          onReply = { if (!isSelectionMode) replyingTo = it },
          modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.99f }.drawWithContent {
             drawContent()
             drawRect(
                 brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    0.0f to androidx.compose.ui.graphics.Color.Transparent,
                    (110.dp.toPx() / size.height) to androidx.compose.ui.graphics.Color.Black,
                    1.0f - (110.dp.toPx() / size.height) to
androidx.compose.ui.graphics.Color.Black,
                    1.0f to androidx.compose.ui.graphics.Color.Transparent,
                    startY = 0f,
                    endY = size.height
                 ),
                 blendMode = androidx.compose.ui.graphics.BlendMode.DstIn
             )
          },
          contentPadding = PaddingValues(top = 80.dp, bottom = if (isChannel) 12.dp else 90.dp + botExtraBottom, start = 12.dp, end = 12.dp)
        )
          }

       Column(modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()) {
         if (isSelectionMode) {
             SelectionTopBar(
                count = selectedMessages.size,
                onClearSelection = { selectedMessages = emptySet() },
                onDelete = {
                   deleteDialogIds = selectedMessages
                },
                onCopy = {
                   val texts = messages.filter { it.id in selectedMessages }.joinToString("n") {
it.text }

clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(texts))
                    selectedMessages = emptySet()
                    toastNotification = ToastNotification(System.currentTimeMillis(), "Message copied to clipboard", ToastType.COPY)
                 }
              )
           } else {
              val myLastSeenRule = myProfile?.privacySettings?.lastSeen ?: "everyone"
              val theirLastSeenRule = otherUserProfile?.privacySettings?.lastSeen ?:
"everyone"
              val theirPhotoRule = otherUserProfile?.privacySettings?.profilePhoto ?:
"everyone"
              val canSeePresence = PrivacyEvaluator.canSeePresence(myLastSeenRule,
theirLastSeenRule, amIBlocked, haveIBlocked)
              val correctedCurrentTime = liveTime.plusSeconds(com.example.ui.globalServerTimeOffsetSeconds)
              val isActuallyOnline = if (lastSeen != null) {
                  com.example.ui.isUserOnline(lastSeen, correctedCurrentTime)
              } else {
                  isOnline
              }
              val showOnline = canSeePresence && isActuallyOnline
              val showLastSeen = if (canSeePresence) lastSeen else null
              val canSeePhoto = PrivacyEvaluator.canSeeProfilePhoto(theirPhotoRule,amIBlocked, haveIBlocked)
              val showAvatar = if (canSeePhoto) (otherUserProfile?.avatarUrl ?: currentChat?.avatarUrl) else null

             FloatingTopBar(
                 name = currentChat?.name ?: name, 
                 avatarUrl = showAvatar, 
                 isChannel = isChannel,
                 subscriberCount = channelSubscriberCount,
                 isTyping = (canSeePresence && isTyping), 
                 isOnline = showOnline, 
                 isBlocked = haveIBlocked || (isUserBotChat && botBlocked),
                 onBlockToggle = {
                     if (isUserBotChat) {
                         val newState = !botBlocked
                         coroutineScope.launch {
                             val ok = withContext(Dispatchers.IO) { com.example.bot.BotManager.setBlocked(userBotId, newState) }
                             if (ok) { botBlocked = newState; if (newState) botStarted = false }
                         }
                     } else if (haveIBlocked) { showUnblockDialog = true } else { showBlockDialog = true }
                 },
                 blockedByThem = amIBlocked,
                 isVerified = currentChat?.isVerified == true,
                 lastSeen = showLastSeen, 
                 statusOverride = if (isBotChat) com.example.bot.BotManagerStrings.usersLabel(botUsers ?: 0) else null,
                 onBack = onBack, 
                 activeFilter = activeFilter, 
                 onFilterChange = { activeFilter = it }, 
                 onSearch = { isSearchMode = true },
                 menuExpanded = topMenuExpanded,
                 onMenuExpandedChange = { topMenuExpanded = it },
                 onProfileClick = { if (isChannel) { showChannelInfo = true } else if (otherUserId != null) onProfileClick(otherUserId!!) },
                 isMuted = currentChat?.isMuted ?: false,
                 isPinned = currentChat?.hasStar ?: false,
                 isArchived = currentChat?.isArchived ?: false,
                 onArchiveToggle = {
                     coroutineScope.launch {
                         val newState = !(currentChat?.isArchived ?: false)
                         val myId = com.example.supabase.auth.currentUserOrNull()?.id
                         if (myId != null) {
                             val success = com.example.ui.ChatLifecycleManager.setArchived(context, myId, listOf(chatId), newState, chatDao)
                             if (!success) android.widget.Toast.makeText(context, "خطأ في الاتصال", android.widget.Toast.LENGTH_LONG).show()
                         }
                     }
                 },
                 onDeleteChat = {
                     coroutineScope.launch {
                         val myId = com.example.supabase.auth.currentUserOrNull()?.id
                         if (myId != null) {
                             com.example.ui.ChatLifecycleManager.deleteChats(context, myId, listOf(chatId), false, chatDao)
                             onBack()
                         }
                     }
                 },
                 onClearHistory = {
                     coroutineScope.launch {
                         val myId = com.example.supabase.auth.currentUserOrNull()?.id
                         if (myId != null) {
                             val success = com.example.ui.ChatLifecycleManager.clearHistory(context, myId, listOf(chatId), false, chatDao)
                             if (!success) android.widget.Toast.makeText(context, "خطأ في الاتصال", android.widget.Toast.LENGTH_LONG).show()
                         }
                     }
                 },
                 onMuteToggle = { coroutineScope.launch { 
                     val newState = !(currentChat?.isMuted ?: false)
                     val myId = com.example.supabase.auth.currentUserOrNull()?.id
                     if (myId != null) {
                         val success = com.example.ui.ChatStateManager.setMuted(myId, listOf(chatId), newState, chatDao)
                         if (!success) {
                             android.widget.Toast.makeText(context, "خطأ في الاتصال، يرجى المحاولة لاحقاً", android.widget.Toast.LENGTH_LONG).show()
                         }
                     }
                 } },
                 onPinToggle = { coroutineScope.launch { 
                     val newState = !(currentChat?.hasStar ?: false)
                     val myId = com.example.supabase.auth.currentUserOrNull()?.id
                     if (myId != null) {
                         val success = com.example.ui.ChatStateManager.setPinned(myId, listOf(chatId), newState, chatDao)
                         if (!success) {
                             android.widget.Toast.makeText(context, "خطأ في الاتصال، يرجى المحاولة لاحقاً", android.widget.Toast.LENGTH_LONG).show()
                         }
                     }
                 } },
                 isSearchMode = isSearchMode,
                 onSearchModeChange = { isSearchMode = it },
                 searchQuery = searchQuery,
                 onSearchQueryChange = { searchQuery = it }
             )
           }

            AnimatedVisibility(
               visible = activeFilter != null,
               enter = slideInVertically(initialOffsetY = { -it / 2 }) + fadeIn(),
               exit = slideOutVertically(targetOffsetY = { -it / 2 }) + fadeOut(),
               modifier = Modifier.padding(top = 8.dp).zIndex(30f)
            ) {
               Surface(
                  shape = RoundedCornerShape(16.dp),
                  color = MaterialTheme.colorScheme.primaryContainer,
                  modifier = Modifier.padding(horizontal = 16.dp).height(32.dp)
               ) {
                  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
                      Text("Filter: ${activeFilter}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Medium)
                      Spacer(modifier = Modifier.width(8.dp))
                      Icon(Icons.Outlined.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp).clickable { activeFilter = null }, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                  }
               }
            }

            AnimatedVisibility(
               visible = pinnedMessages.isNotEmpty(),
               enter = slideInVertically(
                   initialOffsetY = { -it / 2 },
                   animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f)
               ) + fadeIn(tween(200)) + scaleIn(initialScale = 0.95f, animationSpec = tween(200)),
               exit = slideOutVertically(
                   targetOffsetY = { -it / 2 },
                   animationSpec = tween(150)
               ) + fadeOut(tween(150)) + scaleOut(targetScale = 0.95f, animationSpec = tween(150)),
               modifier = Modifier.padding(top = 0.dp).offset(y = (-10).dp).zIndex(30f)
            ){
               if (pinnedMessages.isNotEmpty()) {
                   PinnedMessageBar(
                      message = pinnedMessages[currentPinIndex % pinnedMessages.size],
                      count = pinnedMessages.size,
                      currentIndex = currentPinIndex % pinnedMessages.size,
                      onUnpin = {
                         if (pinnedMessages.isNotEmpty()) {
                             val targetMsg = pinnedMessages[currentPinIndex % pinnedMessages.size]
                             coroutineScope.launch {
                                 val myId = supabase.auth.currentUserOrNull()?.id ?: return@launch
                                 com.example.ui.PinnedMessagesManager.unpin(chatId, targetMsg.id, myId)
                             }
                         }
                      },
                  onClick = {
                     if (pinnedMessages.isNotEmpty()) {
                         val targetMsg = pinnedMessages[currentPinIndex % pinnedMessages.size]
                         val index = messages.indexOfFirst { it.id == targetMsg.id }
                         if (index >= 0) {
                             coroutineScope.launch {
                                listState.animateScrollToItem(index)
                                highlightedMessageId = targetMsg.id
                                kotlinx.coroutines.delay(1500)
                                if (highlightedMessageId == targetMsg.id) highlightedMessageId = null
                             }
                         }
                         if (pinnedMessages.size > 1) {
                             currentPinIndex = (currentPinIndex + 1) % pinnedMessages.size
                         }
                     }
                  }
               )
             }
           }
        }

           
           var previousMessagesSize by remember {
androidx.compose.runtime.mutableIntStateOf(messages.size) }

           LaunchedEffect(messages.size) {
             if (messages.size > previousMessagesSize && previousMessagesSize > 0) {
                 if (listState.canScrollForward) {
                     val newlyAdded = messages.size - previousMessagesSize
                     val newNotMine = messages.takeLast(newlyAdded).count { !it.isMine }
                     unreadNewMessagesCount += newNotMine
                 }
                }
                previousMessagesSize = messages.size
            }

              LaunchedEffect(listState.canScrollForward) {
                 if (!listState.canScrollForward) {
                     unreadNewMessagesCount = 0
                 }
              }
              val showScrollToBottom by remember {androidx.compose.runtime.derivedStateOf { listState.canScrollForward } }

              androidx.compose.animation.AnimatedVisibility(
                 visible = showScrollToBottom,
                 enter = androidx.compose.animation.scaleIn(
                     animationSpec = androidx.compose.animation.core.spring(
                         stiffness = 400f,
                         dampingRatio = 0.8f // roughly translates to damping 25
                     ),
                     initialScale = 0f
                 ) + androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(
                     initialOffsetY = { 40 },
                     animationSpec = androidx.compose.animation.core.spring(stiffness = 400f, dampingRatio = 0.8f)
                 ),
                 exit = androidx.compose.animation.scaleOut(
                     animationSpec = androidx.compose.animation.core.spring(stiffness = 400f, dampingRatio = 0.8f),
                     targetScale = 0f
                 ) + androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(
                     targetOffsetY = { 40 },
                     animationSpec = androidx.compose.animation.core.spring(stiffness = 400f, dampingRatio = 0.8f)
                 ),
                 modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 85.dp)
              ){
                 Box {
                     Surface(
                         onClick = {
                            coroutineScope.launch {
                               if (messages.isNotEmpty() && listState.layoutInfo.totalItemsCount > 0) {
                                   try {
                                       val target = listState.layoutInfo.totalItemsCount - 1
                                       if (target >= 0) listState.animateScrollToItem(target)
                                   } catch (e: Exception) {}
                               }
                            }
                            unreadNewMessagesCount = 0
                         },
                         shape = CircleShape,
                         color = com.example.ui.SettingsColors.surface,
                         border = androidx.compose.foundation.BorderStroke(1.dp, if (__theme.isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f)),
                         modifier = Modifier.size(46.dp),
                         shadowElevation = 0.dp,
                         tonalElevation = 0.dp
                     ){
                         Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Lucide.ChevronDown,
                                contentDescription = "Scroll to bottom",
                                tint = if (__theme.isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280),
                                modifier = Modifier.size(32.dp).alpha(0.9f)
                            )
                         }
                     }
                     if (unreadNewMessagesCount > 0) {
                        Box(
                           modifier = Modifier
                             .align(Alignment.TopEnd)
                             .offset(x = 4.dp, y = (-4).dp)
                             .height(20.dp)
                             .widthIn(min = 20.dp)
                             .clip(RoundedCornerShape(12.dp))
                             .background(Color(0xFF8BC34A))
                             .padding(horizontal = 6.dp),
                           contentAlignment = Alignment.Center
                        ){
                           Text(
                             text = if (unreadNewMessagesCount > 999) "999+" else unreadNewMessagesCount.toString(),
                             color = Color.White,
                             fontSize = 13.5.sp,
                             fontWeight = FontWeight.Bold
                           )
                        }
                    }
                }
            }
                 // ===== رسائل التأكيد: حظر / فك الحظر (مفصولة فدالة خاصة لتخفيف حجم ChatDetailScreen) =====
                 if (showBlockDialog || showUnblockDialog) {
                     BlockConfirmDialogs(
                         showBlock = showBlockDialog,
                         showUnblock = showUnblockDialog,
                         dialogName = currentChat?.name ?: name,
                         chatId = chatId,
                         otherUserId = otherUserId,
                         chatDao = chatDao,
                         scope = coroutineScope,
                         surfaceColor = __surfaceColor,
                         textPrimary = __textPrimary,
                         onDismiss = { showBlockDialog = false; showUnblockDialog = false },
                         onBlockedChange = { }
                     )
                 }
                 if (name != "ديشو") {
                     Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                        if (isBotChat && botStarted) {
                            BotCommandSuggestions(
                                commands = botCommands,
                                typedText = messageText,
                                onPick = { cmd ->
                                    if (isBotManagerChat || isUserBotChat) { messageText = ""; botSend(cmd) }
                                    else messageText = cmd
                                }
                            )
                        }
                        if (isBotChat && !botStarted) {
                            BotStartBar(label = if (botBlocked) com.example.bot.BotManagerStrings.RESTART else com.example.bot.BotManagerStrings.START, onClick = {
                                if (isUserBotChat) {
                                    // لا نعتبر البوت "مبدوءا" إلا بعد نجاح الإرسال فعلا للسيرفر
                                    sendToUserBot("/start") { ok ->
                                        if (ok) {
                                            botBlocked = false
                                            botStarted = true
                                            com.example.bot.BotManager.markStarted(context, chatId)
                                        }
                                    }
                                } else {
                                    botBlocked = false
                                    botStarted = true
                                    com.example.bot.BotManager.markStarted(context, chatId)
                                    if (isBotManagerChat) botSend("/start") else messageText = "/start"
                                }
                            })
                        } else if (isChannel && !isChannelAdmin) {
                            // لا شيء — حذفنا شريط "Mute" حتى يظهر آخر منشور ملاصقاً لأسفل الشاشة فعلياً
                        } else {
                            if (haveIBlocked) {
                                // بطاقة فك الحظر: نفس أبعاد بطاقة لوحة المفاتيح بالضبط
                                UnblockInputBar(
                                    text = LocalTranslation.current.unblockUserAction,
                                    onClick = { showUnblockDialog = true }
                                )
                            } else if (amIBlocked) {
                                // الطرف الآخر حظرني: لوحة المفاتيح تختفي بالكامل
                            } else {
                 if (isBotChat && botStarted && showBotMenu && botCommands.isNotEmpty()) {
                     BotCommandsSheet(commands = botCommands, onPick = { cmd ->
                         showBotMenu = false
                         if (isBotManagerChat || isUserBotChat) { messageText = ""; botSend(cmd) } else messageText = cmd
                     })
                 }
                 MessageInputBar(
                     trailing = if (isBotChat && botStarted && replyKeyboardRows != null) ({
                         ReplyKeyboardToggle(active = showReplyKb, onClick = {
                             showReplyKb = !showReplyKb
                             if (showReplyKb) { showBotMenu = false; botFocus.clearFocus(); botKbd?.hide() }
                         })
                     }) else null,
                     leading = if (isBotChat && botStarted && botCommands.isNotEmpty()) ({
                         BotMenuPill(open = showBotMenu, onClick = {
                             showBotMenu = !showBotMenu
                             if (showBotMenu) { botFocus.clearFocus(); botKbd?.hide() }
                         })
                     }) else null,
                     text = messageText,
                 onTextChange = {
                     messageText = it
                     onUserType()
                 },
                 replyingTo = replyingTo,
                 onCancelReply = { replyingTo = null },
                 editingMessage = editingMessage,
                 onCancelEdit = {
                     editingMessage = null
                     messageText = ""
                 },
                 onSend = {
                     if (isBotManagerChat || isUserBotChat) {
                         val t = messageText
                         messageText = ""
                         SendSound.play(context)
                         botSend(t)
                         return@MessageInputBar
                     }
                     typingJob?.cancel()
                   sendTypingStatus(false)
                   lastTypingSentTime = 0L
                   if (messageText.isNotBlank()) {
                       val textToSend = messageText
                       messageText = ""
                       if (editingMessage != null) {
                           val editedId = editingMessage!!.id
                           val oldText = editingMessage!!.text
                           val oldEdited = editingMessage!!.isEdited
                           messages = messages.map { if (it.id == editedId)
it.copy(text = textToSend, isEdited = true) else it }
                           editingMessage = null
                           coroutineScope.launch {
                               try {
                                   supabase.postgrest["messages"].update({
                                       set("content", textToSend)
                                       set("edited_at", java.time.Instant.now().toString())
                                   }) {
                                       filter { eq("id", editedId) }
                                   }
                               } catch (e: Exception) {
                                   android.util.Log.e("ChatDetail", "edit message failed", e)
                                   messages = messages.map { if (it.id == editedId) it.copy(text = oldText, isEdited = oldEdited) else it }
                               }
                           }
                       } else {
                           val replyMsg = replyingTo
                           replyingTo = null

                      if (myUserId != null) {
                          val tempId = java.util.UUID.randomUUID().toString()
                          val newMsg = MessageModel(
                            id = tempId,
                            text = textToSend,
                            time =
java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                            isMine = true,
                            replyToId = replyMsg?.id,
                            replyTo = replyMsg,
                            status = MessageStatus.SENDING
                          )
                          messages = (messages + newMsg).distinctBy { it.id }
                          SendSound.play(context)

                          // Optimistic update of local chat lists
                          coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                              try {
                                  val existingChat = chatDao.getChatById(chatId)
                                  if (existingChat != null) {
                                      chatDao.insert(existingChat.copy(
                                          message = textToSend,
                                          time = newMsg.time,
                                          timestamp = System.currentTimeMillis(),
                                          isMine = true,
                                          isReadReceipt = false,
                                          lastMediaType = null,
                                          lastMediaUrl = null,
                                          lastThumbnailUrl = null
                                      ))
                                  }
                                  val existingCachedChat = cachedChatDao.getCachedChatById(chatId)
                                  if (existingCachedChat != null) {
                                      cachedChatDao.insertCachedChats(listOf(existingCachedChat.copy(
                                          last_message = textToSend,
                                          time_str = newMsg.time,
                                          timestamp = System.currentTimeMillis()
                                      )))
                                  }
                                  
                                  cachedMessageDao.insertCachedMessage(
                                      com.example.cache.CachedMessage(
                                          id = tempId,
                                          chat_id = chatId,
                                          sender_id = myUserId ?: "",
                                          content = textToSend,
                                          created_at = java.time.Instant.now().toString(),
                                          status = "SENDING",
                                          message_type = null,
                                          media_url = null,
                                          reply_to_id = replyMsg?.id,
                                          media_aspect_ratio = null,
                                          media_group_id = null
                                      )
                                  )
                              } catch(e: Exception) {}
                          }

                         coroutineScope.launch {
                           try {
                              val insertData = MessageInsert(
                                 chat_id = chatId,
                                 sender_id = myUserId ?: "",
                                 content = textToSend,
                                 reply_to_id = replyMsg?.id
                              )

                          val result =
com.example.supabase.postgrest["messages"].insert(insertData) {
                             select()
                          }.decodeSingle<MessageRow>()

                              val timeStr = formatTimeSafe(result.created_at)

                              try {
                                 cachedMessageDao.insertCachedMessage(
                                    com.example.cache.CachedMessage(
                                      id = result.id,
                                      chat_id = result.chat_id,
                                      sender_id = result.sender_id,
                                      content = result.content,
                                      created_at = result.created_at,
                                      status = "SENT",
                                      message_type = result.message_type,
                                      media_url = result.media_url,
                                      reply_to_id = result.reply_to_id,
                                      media_aspect_ratio = result.media_aspect_ratio,
                                      media_group_id = result.media_group_id
                                    )
                                 )
                                 cachedMessageDao.deleteMessageById(tempId)
                              } catch(e: Exception) {}

                              val alreadyExists = messages.any { it.id == result.id && it.id !=
tempId }
                           messages = if (alreadyExists) {
                              messages.filter { it.id != tempId }
                           } else {
                              messages.map {
                                 if (it.id == tempId) it.copy(id = result.id, status =
MessageStatus.SENT, time = timeStr) else it
                              }
                           }
                        } catch(e: Exception) {
                           e.printStackTrace()
                           val msgTxt = e.message?.lowercase() ?: ""
                           val isNetworkError = e is java.net.UnknownHostException || e is java.net.ConnectException || e is java.net.SocketTimeoutException || "timeout" in msgTxt || "network" in msgTxt || "connection" in msgTxt || "unable to resolve host" in msgTxt || "failed to connect" in msgTxt
                           
                           if (isNetworkError) {
                               val workRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.worker.PendingMessageWorker>()
                                   .setConstraints(androidx.work.Constraints.Builder().setRequiredNetworkType(androidx.work.NetworkType.CONNECTED).build())
                                   .build()
                               androidx.work.WorkManager.getInstance(context).enqueueUniqueWork("PendingMessageUpload", androidx.work.ExistingWorkPolicy.APPEND_OR_REPLACE, workRequest)
                           } else {
                               messages = messages.map {
                                   if (it.id == tempId) it.copy(status = MessageStatus.FAILED) else it
                               }
                               kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                   try { cachedMessageDao.updateMessageStatus(tempId, "FAILED") } catch(ex: Exception) {}
                               }
                               kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                   android.widget.Toast.makeText(context, "حدث خطأ أثناء إرسال الرسالة: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                               }
                           }
                        }
                     }
                   }
                 }
               }
             },
             onAttachmentSelected = { uris, type ->
                   if (isUserBotChat) {
                       android.widget.Toast.makeText(context, "Bots don't support attachments yet", android.widget.Toast.LENGTH_SHORT).show()
                       return@MessageInputBar
                   }
                   if (isBotManagerChat) {
                       runBotManagerImage(uris.firstOrNull(), type)
                       return@MessageInputBar
                   }
                   coroutineScope.launch {
                        if (myUserId == null) return@launch
                        val bucketName = when(type) {
                            AttachmentType.IMAGE -> "chat-images"
                            AttachmentType.VIDEO -> "chat-videos"
                            AttachmentType.VOICE, AttachmentType.AUDIO ->"voice-messages"
                            else -> "chat-files"
                        }
                       val msgType = when(type) {
                          AttachmentType.IMAGE -> "image"
                          AttachmentType.VIDEO -> "video"
                          AttachmentType.VOICE, AttachmentType.AUDIO -> "voice"
                          else -> "file"
                       }
                       val textStr = if (type == AttachmentType.IMAGE) (if(uris.count() > 1)"[Album]" else "[Photo]")
                          else if (type == AttachmentType.VOICE || type == AttachmentType.AUDIO) "[Voice]"
                          else if (uris.count() > 1) "[Documents]" else "[Document]"

                    val batchId = java.util.UUID.randomUUID().toString()
                     // Section 6.3: pre-compute real filename+size for DOCUMENT attachments.
                     val docMetaByUri = if (type == AttachmentType.DOCUMENT) {
                            uris.associateWith { uri ->
                                val cached = PickedFileMetaCache.map[uri.toString()]
                                if (cached != null) {
                                    cached
                                } else {
                                    var name: String? = null
                                    var size: Long? = null
                                    try {
                                        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                                            val nameIdx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                            val sizeIdx = c.getColumnIndex(android.provider.OpenableColumns.SIZE)
                                            if (c.moveToFirst()) {
                                                if (nameIdx >= 0) name = c.getString(nameIdx)
                                                if (sizeIdx >= 0 && !c.isNull(sizeIdx)) size = c.getLong(sizeIdx)
                                            }
                                        }
                                    } catch (e: Exception) {}
                                    Pair(name, size)
                                }
                            }
                     } else null
                    val tempMsgsWithUris = uris.map { uri ->
                        var calculatedRatio: Float? = null
                        if (type == AttachmentType.IMAGE) {
                            try {
                                val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                android.graphics.BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri), null, options)
                                if (options.outWidth > 0 && options.outHeight > 0) {
                                    val r = options.outWidth.toFloat() / options.outHeight.toFloat()
                                    if (!r.isNaN() && !r.isInfinite()) {
                                        calculatedRatio = r
                                    }
                                }
                            } catch(e: Exception) {}
                        } else if (type == AttachmentType.VOICE) {
                            // نستخرج مدة التسجيل (مللي) من اسم الملف ونخزّنها بالثواني في حقل النسبة
                            // (media_aspect_ratio) لأنه غير مستخدم أصلًا للصوت، فنتفادى تعديل قاعدة البيانات.
                            try {
                                val m = Regex("voice_dur(\\d+)_").find(uri.toString())
                                val durMs = m?.groupValues?.get(1)?.toLongOrNull()
                                if (durMs != null) calculatedRatio = durMs / 1000f
                            } catch (e: Exception) {}
                        }
                        
                        val tempId = java.util.UUID.randomUUID().toString()
                        var localUriStr = uri.toString()
                        try {
                            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            if (bytes != null) {
                                val ext = when (type) {
                                    AttachmentType.IMAGE -> ".jpg"
                                    AttachmentType.VIDEO -> ".mp4"
                                    AttachmentType.AUDIO, AttachmentType.VOICE -> ".m4a"
                                    else -> ""
                                }
                                val localFile = com.example.util.LocalFileManager.saveFile(context, bytes, "temp_${tempId}$ext")
                                if (localFile != null) {
                                    localUriStr = localFile.toURI().toString()
                                }
                            }
                        } catch(e: Exception) {}

                        val docFileName = docMetaByUri?.get(uri)?.first
                        val docFileSize = docMetaByUri?.get(uri)?.second
                        val tempAttachment = Attachment(messageId = tempId, type = type, url = localUriStr, aspectRatio = calculatedRatio, fileName = docFileName, fileSize = docFileSize)
                        val tempMsg = MessageModel(
                            id = tempId,
                            text = textStr,
                            time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                            isMine = true,
                            replyToId = replyingTo?.id,
                            replyTo = replyingTo,
                            attachments = listOf(tempAttachment),
                            status = MessageStatus.SENDING,
                            mediaGroupId = batchId
                        )
                        Pair(tempMsg, android.net.Uri.parse(localUriStr))
                    }
                    
                    // Insert all optimistic messages at once
                    messages = (messages + tempMsgsWithUris.map { it.first }).distinctBy { it.id }
                    SendSound.play(context)

                    // Optimistic update of local chat lists
                    coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val existingChat = chatDao.getChatById(chatId)
                            if (existingChat != null) {
                                val firstLocalUri = tempMsgsWithUris.firstOrNull()?.second?.toString()
                                chatDao.insert(existingChat.copy(
                                    message = textStr,
                                    time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                                    timestamp = System.currentTimeMillis(),
                                    isMine = true,
                                    isReadReceipt = false,
                                    lastMediaType = msgType,
                                    lastMediaUrl = firstLocalUri,
                                    lastThumbnailUrl = if (type == AttachmentType.IMAGE) firstLocalUri else null
                                ))
                            }
                            val existingCachedChat = cachedChatDao.getCachedChatById(chatId)
                            if (existingCachedChat != null) {
                                cachedChatDao.insertCachedChats(listOf(existingCachedChat.copy(
                                    last_message = textStr,
                                    time_str = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                                    timestamp = System.currentTimeMillis()
                                )))
                            }
                            for (pair in tempMsgsWithUris) {
                                cachedMessageDao.insertCachedMessage(
                                    com.example.cache.CachedMessage(
                                        id = pair.first.id,
                                        chat_id = chatId,
                                        sender_id = myUserId ?: "",
                                        content = textStr,
                                        created_at = java.time.Instant.now().toString(),
                                        status = "SENDING",
                                        message_type = msgType,
                                        media_url = pair.second.toString(),
                                        reply_to_id = replyingTo?.id,
                                        media_aspect_ratio = pair.first.attachments.firstOrNull()?.aspectRatio,
                                        media_group_id = batchId
                                    )
                                )
                            }
                        } catch(e: Exception) {}
                    }
                    // Upload in parallel
                    for (pair in tempMsgsWithUris) {
                        val tempMsg = pair.first
                        val uri = pair.second
                        launch(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                val calculatedRatio = tempMsg.attachments.firstOrNull()?.aspectRatio
                                val fileName = java.util.UUID.randomUUID().toString() + "-" +System.currentTimeMillis()
                                val path = "$chatId/$myUserId/$fileName"
                                val inputStream = context.contentResolver.openInputStream(uri)
                                val bytes = inputStream?.readBytes()
                                if (bytes != null) {
                                    com.example.supabase.storage[bucketName].upload(path,bytes)
                                    val signedUrl = com.example.supabase.storage[bucketName].createSignedUrl(path,kotlin.time.Duration.parse("3650d"))

                                    // نولّد ونرفع نسخة صغيرة مضغوطة (للصور بس) — هذا اللي يخلي
                                    // العرض داخل الدردشة سريع (كيما تيليجرام)، بدل تحميل الصورة الكاملة كل مرة
                                    var thumbnailSignedUrl: String? = null
                                    if (type == AttachmentType.IMAGE) {
                                        try {
                                            val thumbBytes = com.example.util.ThumbnailUtils.generateThumbnailBytes(context, uri)
                                            if (thumbBytes != null) {
                                                val thumbPath = "$chatId/$myUserId/thumb_$fileName"
                                                com.example.supabase.storage[bucketName].upload(thumbPath, thumbBytes)
                                                thumbnailSignedUrl = com.example.supabase.storage[bucketName].createSignedUrl(thumbPath, kotlin.time.Duration.parse("3650d"))
                                            }
                                        } catch (e: Exception) {
                                            // إذا فشل توليد المصغرة لأي سبب، نكمل بالصورة الأصلية بلا ما نوقف الإرسال
                                        }
                                    }

                                    val insertData = MessageInsert(
                                        chat_id = chatId,
                                        sender_id = myUserId ?: "",
                                        content = textStr,
                                        message_type = msgType,
                                        media_url = signedUrl,
                                        thumbnail_url = thumbnailSignedUrl,
                                        reply_to_id = replyingTo?.id,
                                        media_aspect_ratio = calculatedRatio,
                                        media_group_id = batchId
                                    )
                                    val result = com.example.supabase.postgrest["messages"].insert(insertData) {
                                        select()
                                    }.decodeSingle<MessageRow>()
                                    
                                    val timeStr = formatTimeSafe(result.created_at)
                                    val docMeta = docMetaByUri?.get(uri)
                                    val attachment = Attachment(messageId = result.id, type = type, url = signedUrl, thumbnailUrl = result.thumbnail_url, aspectRatio = calculatedRatio, fileName = docMeta?.first, fileSize = docMeta?.second)
                                    try {
                                        cachedMessageDao.insertCachedMessage(
                                            com.example.cache.CachedMessage(
                                                id = result.id,
                                                chat_id = result.chat_id,
                                                sender_id = result.sender_id,
                                                content = result.content,
                                                created_at = result.created_at,
                                                status = "SENT",
                                                message_type = result.message_type,
                                                media_url = result.media_url,
                                                thumbnail_url = result.thumbnail_url,
                                                reply_to_id = result.reply_to_id,
                                                media_aspect_ratio = calculatedRatio,
                                                media_group_id = result.media_group_id
                                            )
                                        )
                                        cachedMessageDao.deleteMessageById(tempMsg.id)
                                    } catch(e: Exception) {}
                                    
                                    val newMsg = MessageModel(
                                        id = result.id,
                                        text = result.content,
                                        time = timeStr,
                                        isMine = true,
                                        replyToId = replyingTo?.id,
                                        replyTo = replyingTo,
                                        attachments = listOf(attachment),
                                        status = MessageStatus.SENT,
                                        createdAtExact = result.created_at,
                                        mediaGroupId = result.media_group_id
                                    )
                                    
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        val alreadyExists = messages.any { it.id == result.id && it.id != tempMsg.id }
                                        messages = if (alreadyExists) {
                                            messages.filter { it.id != tempMsg.id }
                                        } else {
                                            messages.map {
                                                if (it.id == tempMsg.id) newMsg else it
                                            }
                                        }
                                    }
                                }
                            } catch(e: Exception) {
                                e.printStackTrace()
                                val msgTxt = e.message?.lowercase() ?: ""
                                val isNetworkError = e is java.net.UnknownHostException || e is java.net.ConnectException || e is java.net.SocketTimeoutException || "timeout" in msgTxt || "network" in msgTxt || "connection" in msgTxt || "unable to resolve host" in msgTxt || "failed to connect" in msgTxt
                                
                                if (isNetworkError) {
                                    val workRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.worker.PendingMessageWorker>()
                                        .setConstraints(androidx.work.Constraints.Builder().setRequiredNetworkType(androidx.work.NetworkType.CONNECTED).build())
                                        .build()
                                    androidx.work.WorkManager.getInstance(context).enqueueUniqueWork("PendingMessageUpload", androidx.work.ExistingWorkPolicy.APPEND_OR_REPLACE, workRequest)
                                    // نحدث الواجهة كمان حتى في حالة خطأ الشبكة، باش الصورة ما تبقاش
                                    // تدور بلا نهاية بصح تبين حالة "فشل/إعادة المحاولة" واضحة للمستخدم
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        messages = messages.map {
                                            if (it.id == tempMsg.id) it.copy(status = MessageStatus.FAILED) else it
                                        }
                                    }
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        try { cachedMessageDao.updateMessageStatus(tempMsg.id, "FAILED") } catch(ex: Exception) {}
                                    }
                                } else {
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        messages = messages.map {
                                            if (it.id == tempMsg.id) it.copy(status = MessageStatus.FAILED) else it
                                        }
                                        android.widget.Toast.makeText(context, "حدث خطأ أثناء إرسال المرفق: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        try { cachedMessageDao.updateMessageStatus(tempMsg.id, "FAILED") } catch(ex: Exception) {}
                                    }
                                }
                            }
                        }
                    }
                    replyingTo = null
                  }
                }
              )
              }
          }
          if (isBotChat && botStarted && showReplyKb && replyKeyboardRows != null) {
              ReplyKeyboardGrid(rows = replyKeyboardRows, onKey = { label ->
                  if (isBotManagerChat || isUserBotChat) botSend(label) else messageText = label
              })
          }
          }
          } // End of Column
      } // End of Blur Box

       androidx.compose.animation.AnimatedVisibility(
         visible = viewingImageUrls != null,
         enter = androidx.compose.animation.scaleIn(initialScale = 0.8f, animationSpec =
tween(300)) + androidx.compose.animation.fadeIn(animationSpec = tween(300)),
         exit = androidx.compose.animation.scaleOut(targetScale = 0.8f, animationSpec =
tween(300)) + androidx.compose.animation.fadeOut(animationSpec = tween(300)),
         modifier = Modifier.fillMaxSize()
       ){
            Box(modifier =
Modifier.fillMaxSize().background(MaterialTheme.colorScheme.onBackground).clickable(
                interactionSource = remember {
androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { viewingImageUrls = null }, contentAlignment = Alignment.Center) {
                viewingImageUrls?.let { urls ->
                   val pagerState = rememberPagerState(
                       initialPage = viewingImageInitialIndex,
                       pageCount = { urls.size }
                   )
                   HorizontalPager(
                       state = pagerState,
                       modifier = Modifier.fillMaxSize()
                   ) { page ->
                       var scale by remember { mutableStateOf(1f) }
                       var offset by remember {
mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
                       val transformableState = rememberTransformableState { zoomChange,
offsetChange, _ ->
                          scale = (scale * zoomChange).coerceIn(1f, 5f)
                          offset += offsetChange
                       }

                 Box(
                    modifier = Modifier
                      .fillMaxSize()
                      .pointerInput(Unit) {
                           detectTapGestures(
                                onDoubleTap = {
                                   scale = if (scale > 1f) 1f else 2.5f
                                   offset = androidx.compose.ui.geometry.Offset.Zero
                                }
                           )
                      }
                      .transformable(state = transformableState),
                    contentAlignment = Alignment.Center
                 ){
                    coil.compose.AsyncImage(
                      model = urls[page],
                      contentDescription = "Full screen image",
                      modifier = Modifier
                           .fillMaxSize()
                           .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                 translationY = offset.y
                               ),
                             contentScale = androidx.compose.ui.layout.ContentScale.Fit
                         )
                     }
                 }
               }
               Icon(
                  imageVector = Icons.Outlined.Close,
                  contentDescription = "Close",
                  tint = MaterialTheme.colorScheme.onBackground,
                  modifier = Modifier
                     .align(Alignment.TopEnd)
                     .padding(16.dp)
                     .size(32.dp)
                     .clickable { viewingImageUrls = null }
               )
           }
       }

       showForwardDialog?.let { _ ->
          ForwardMessageDialog(
             primaryColor = primaryPurple,
             onDismiss = { showForwardDialog = null }
          )
       }

      }
  }



    val reactionScope = androidx.compose.runtime.rememberCoroutineScope()
    selectedMessageForContext?.let { msg ->
            MessageContextMenuOverlay(
               message = msg,
               showFullMenu = contextMenuShowFull,
               isChannel = isChannel,
               onDismissRequest = { 
                   selectedMessageForContext = null
                   if (wasKeyboardOpen) keyboardController?.show()
               },
               onReactionSelected = { reaction ->
                 // القناة والمحادثة: نفس المسار — يُحفظ التفاعل في جدول reactions فتظهر العدادات للجميع
                 val existingReaction = msg.reactions.contains(reaction)
                 messages = messages.map { if (it.id == msg.id) it.copy(reactions = if (existingReaction) emptyList() else listOf(reaction)) else it }
                 selectedMessageForContext = null
                 if (wasKeyboardOpen) keyboardController?.show()
                 reactionScope.launch {
                   try {
                      val uid = com.example.supabase.auth.currentSessionOrNull()?.user?.id
                      ?: return@launch
                      // احذف أي رد فعل سابق لهذا المستخدم على هذه الرسالة (بغض النظر عن الإيموجي)
                      com.example.supabase.postgrest["reactions"].delete {
                          filter {
                              eq("message_id", msg.id)
                              eq("user_id", uid)
                          }
                      }
                      if (!existingReaction) {
                          val req = com.example.ui.ReactionInsert(message_id = msg.id,
                          user_id = uid, emoji = reaction)
                          com.example.supabase.postgrest["reactions"].insert(req)
                      }
                      // نفس التعديل: شلنا إعادة القراءة الفورية اللي كانت كتلغي التحديث
                      // المحلي وترجع الإيموجي القديم بسبب تأخر تثبيت الكتابة فقاعدة البيانات.
                  } catch (e: Exception) {
                  }
                }
             },
             onReply = { replyingTo = msg },
             onEdit = {
                editingMessage = msg
                messageText = msg.text
             },
             onCopy = {

clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(msg.text))
              toastNotification = ToastNotification(System.currentTimeMillis(), "Message copied to clipboard", ToastType.COPY)
           },
           onDelete = {
              deleteDialogIds = setOf(msg.id)
           },
           onSelect = {
              selectedMessages = setOf(msg.id)
           },
           onForward = {
              showForwardDialog = msg
           },
           onPin = {
              val isCurrentlyPinned = pinnedRows.any { it.message_id == msg.id }
              if (!isCurrentlyPinned) {
                  pinDialogMessage = msg
              } else {
                  coroutineScope.launch {
                      val myId = supabase.auth.currentUserOrNull()?.id ?: return@launch
                      com.example.ui.PinnedMessagesManager.unpin(chatId, msg.id, myId)
                      toastNotification = ToastNotification(System.currentTimeMillis(), "Message unpinned", ToastType.UNPIN)
                  }
              }
           },
             onSave = {
                val isNowSaved = !msg.isSaved
                messages = messages.map { if (it.id == msg.id) it.copy(isSaved =
isNowSaved) else it }
                coroutineScope.launch {
                    val sid = supabase.auth.currentUserOrNull()?.id
                    if (sid != null) com.example.ui.SavedMessagesManager.setSaved(sid, msg.id, isNowSaved)
                }
                if (isNowSaved) {
                    toastNotification = ToastNotification(System.currentTimeMillis(), "Message saved", ToastType.SAVE)
                } else {
                    toastNotification = ToastNotification(System.currentTimeMillis(), "Message unsaved", ToastType.UNSAVE)
                }
             },
             onMoreClick = { selectedMessageForContext = null }
           )
       }

       val delIds = deleteDialogIds
       if (delIds != null) {
           val dt = com.example.ui.i18n.rememberExtraStrings()
           val delMsgs = messages.filter { it.id in delIds }
           val allMine = delMsgs.isNotEmpty() && delMsgs.all { it.isMine }
           val otherName = currentChat?.name ?: name
           val isGroupLike = isChannel || currentChat?.isGroup == true || currentChat?.isChannel == true
           com.example.ui.AppConfirmDialog(
               title = if (delIds.size == 1) dt.msgDeleteTitle else String.format(dt.msgDeleteTitleFmt, delIds.size),
               message = if (delIds.size == 1) dt.msgDeleteBody else String.format(dt.msgDeleteBodyFmt, delIds.size),
               checkboxLabel = if (allMine) (if (isGroupLike) dt.msgAlsoDeleteEveryone else String.format(dt.msgAlsoDeleteForFmt, otherName)) else null,
               confirmText = dt.dlgDelete,
               cancelText = dt.dlgCancel,
               onDismiss = { deleteDialogIds = null },
               onConfirm = { alsoForEveryone ->
                   val ids = delIds
                   deleteDialogIds = null
                   selectedMessages = emptySet()
                   coroutineScope.launch {
                       val myId = supabase.auth.currentUserOrNull()?.id
                       if (myId != null) {
                           val ok = if (alsoForEveryone && allMine) {
                               com.example.ui.MessageDeletionManager.deleteForEveryone(myId, ids)
                           } else {
                               com.example.ui.MessageDeletionManager.deleteForMe(myId, ids)
                           }
                           toastNotification = if (ok) {
                               ToastNotification(System.currentTimeMillis(), dt.msgDeletedToast, ToastType.COPY)
                           } else {
                               ToastNotification(System.currentTimeMillis(), dt.actionFailedToast, ToastType.COPY)
                           }
                       }
                   }
               }
           )
       }
       if (pinDialogMessage != null) {
           val t = com.example.ui.i18n.rememberExtraStrings()
           androidx.compose.material3.AlertDialog(
               onDismissRequest = { pinDialogMessage = null },
               containerColor = SettingsColors.surface,
               title = { Text(t.pinMessageTitle, color = SettingsColors.textPrimary) },
               text = { Text(t.pinMessageTitle, color = SettingsColors.textSecondary) },
               confirmButton = {
                   Column(horizontalAlignment = Alignment.End) {
                       androidx.compose.material3.TextButton(onClick = {
                           val msg = pinDialogMessage!!
                           pinDialogMessage = null
                           coroutineScope.launch {
                               val myId = supabase.auth.currentUserOrNull()?.id ?: return@launch
                               com.example.ui.PinnedMessagesManager.pin(chatId, msg.id, "me", myId)
                           }
                       }) { Text(t.pinForMeOnly, color = SettingsColors.blueAccent) }
                       
                       androidx.compose.material3.TextButton(onClick = {
                           val msg = pinDialogMessage!!
                           pinDialogMessage = null
                           coroutineScope.launch {
                               val myId = supabase.auth.currentUserOrNull()?.id ?: return@launch
                               com.example.ui.PinnedMessagesManager.pin(chatId, msg.id, "both", myId)
                           }
                       }) { Text(String.format(t.pinForBothFmt, name), color = SettingsColors.blueAccent) }
                   }
               },
               dismissButton = {
                   androidx.compose.material3.TextButton(onClick = { pinDialogMessage = null }) { Text(com.example.ui.i18n.LocalTranslation.current.cancel, color = SettingsColors.textSecondary) }
               }
           )
       }

       LaunchedEffect(toastNotification) {
          if (toastNotification != null) {
              kotlinx.coroutines.delay(3000)
              toastNotification = null
          }
        }
        Box(modifier = Modifier.fillMaxSize().padding(bottom = 72.dp), contentAlignment = Alignment.BottomCenter) {
            AnimatedVisibility(
                visible = toastNotification != null,
                enter = slideInVertically(
                    initialOffsetY = { 100 },
                    animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f)
                ) + fadeIn(tween(200)) + scaleIn(initialScale = 0.9f, animationSpec = tween(200)),
                exit = slideOutVertically(
                    targetOffsetY = { 50 },
                    animationSpec = tween(150)
                ) + fadeOut(tween(150)) + scaleOut(targetScale = 0.95f, animationSpec = tween(150)),
                modifier = Modifier
                    .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth()
            ) {
                toastNotification?.let { toast ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentWidth(Alignment.CenterHorizontally)
                            .shadow(
                                elevation = 8.dp,
                                shape = CircleShape,
                                ambientColor = Color(0x14000000),
                                spotColor = Color(0x14000000)
                            )
                            .clip(CircleShape)
                            .background(__theme.surfaceColor.copy(alpha = 0.97f))
                            .border(1.dp, (if (__theme.isDark) Color.White else Color.Black).copy(alpha = 0.08f), CircleShape)
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .widthIn(max = 360.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = toast.text,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = __theme.textPrimary,
                                letterSpacing = (-0.5).sp
                            )
                            
                            Spacer(modifier = Modifier.width(16.dp))

                            // Icon logic based on toast.type
                            when (toast.type) {
                                ToastType.COPY -> {
                                    val scaleAnim = remember { androidx.compose.animation.core.Animatable(0.5f) }
                                    val rotateAnim = remember { androidx.compose.animation.core.Animatable(0f) }
                                    val alphaAnim = remember { androidx.compose.animation.core.Animatable(0f) }
                                    
                                    LaunchedEffect(toast.id) {
                                        kotlinx.coroutines.delay(100)
                                        launch { alphaAnim.animateTo(1f, tween(100)) }
                                        launch {
                                            scaleAnim.animateTo(1.3f, tween(150, easing = LinearOutSlowInEasing))
                                            scaleAnim.animateTo(0.9f, tween(150, easing = FastOutSlowInEasing))
                                            scaleAnim.animateTo(1f, tween(150, easing = FastOutLinearInEasing))
                                        }
                                        launch {
                                            rotateAnim.animateTo(-10f, tween(150))
                                            rotateAnim.animateTo(5f, tween(150))
                                            rotateAnim.animateTo(0f, tween(150))
                                        }
                                    }
                                    
                                    Icon(
                                        imageVector = Icons.Outlined.ContentCopy,
                                        contentDescription = null,
                                        tint = Color(0xFF4B5563),
                                        modifier = Modifier
                                            .size(18.dp)
                                            .graphicsLayer {
                                                scaleX = scaleAnim.value
                                                scaleY = scaleAnim.value
                                                rotationZ = rotateAnim.value
                                                alpha = alphaAnim.value
                                            }
                                    )
                                }
                                ToastType.SAVE, ToastType.PIN -> {
                                    val scaleAnim = remember { androidx.compose.animation.core.Animatable(0f) }
                                    val rotateAnim = remember { androidx.compose.animation.core.Animatable(-180f) }
                                    val alphaAnim = remember { androidx.compose.animation.core.Animatable(0f) }
                                    
                                    LaunchedEffect(toast.id) {
                                        kotlinx.coroutines.delay(100)
                                        launch { alphaAnim.animateTo(1f, tween(100)) }
                                        launch { scaleAnim.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 300f)) }
                                        launch { rotateAnim.animateTo(if (toast.type == ToastType.PIN) 45f else 0f, spring(dampingRatio = 0.6f, stiffness = 300f)) }
                                    }
                                    
                                    Icon(
                                        imageVector = if (toast.type == ToastType.PIN) Icons.Filled.PushPin else Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = if (toast.type == ToastType.PIN) Color(0xFF6B7280) else Color(0xFFF5C518),
                                        modifier = Modifier
                                            .size(18.dp)
                                            .graphicsLayer {
                                                scaleX = scaleAnim.value
                                                scaleY = scaleAnim.value
                                                rotationZ = rotateAnim.value
                                                alpha = alphaAnim.value
                                            }
                                    )
                                }
                                ToastType.UNSAVE, ToastType.UNPIN -> {
                                    val scaleAnim = remember { androidx.compose.animation.core.Animatable(0f) }
                                    val rotateAnim = remember { androidx.compose.animation.core.Animatable(180f) }
                                    val alphaAnim = remember { androidx.compose.animation.core.Animatable(0f) }
                                    
                                    LaunchedEffect(toast.id) {
                                        kotlinx.coroutines.delay(100)
                                        launch { alphaAnim.animateTo(1f, tween(100)) }
                                        launch { scaleAnim.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 300f)) }
                                        launch { rotateAnim.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 300f)) }
                                    }
                                    
                                    Icon(
                                        imageVector = if (toast.type == ToastType.UNPIN) Icons.Outlined.PushPin else Icons.Outlined.StarBorder,
                                        contentDescription = null,
                                        tint = Color(0xFF6B7280),
                                        modifier = Modifier
                                            .size(18.dp)
                                            .graphicsLayer {
                                                scaleX = scaleAnim.value
                                                scaleY = scaleAnim.value
                                                rotationZ = rotateAnim.value
                                                alpha = alphaAnim.value
                                            }
                                    )
                                }
                                ToastType.INFO -> {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = null,
                                        tint = Color(0xFF4B5563),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

    if (previewImageId != null) {
        val imageMessages = remember(messages) {
            messages.filter { msg -> msg.attachments.any { it.type == AttachmentType.IMAGE } }.sortedBy { it.timestamp }
        }
        val initialIndex = imageMessages.indexOfFirst { msg ->
            msg.id == previewImageId || msg.attachments.any { it.messageId == previewImageId }
        }
        
        ImageViewerScreen(
            imageMessages = imageMessages,
            initialIndex = if (initialIndex >= 0) initialIndex else 0,
            onBack = { lastMsg -> 
                previewImageId = null 
                if (lastMsg != null) {
                    // Find the index in processedMessages (which listState uses)
                    // The grouped message either has the same ID, or contains the attachment with the ID
                    val index = processedMessages.indexOfFirst { uiMsg ->
                        uiMsg.msg.id == lastMsg.id || uiMsg.msg.attachments.any { it.messageId == lastMsg.id }
                    }
                    if (index >= 0) {
                        coroutineScope.launch { 
                            kotlinx.coroutines.delay(100) // تأخير لضمان استقرار واجهة القائمة بعد إغلاق العارض
                            // شلنا كارد "Not a contact" اللي كان item إضافي فوق القائمة، فما بقاش خاصنا أي offset هنا
                            val offset = 0
                            listState.animateScrollToItem(maxOf(0, index + offset)) 
                        }
                    }
                }
            },
            onShowInChat = { msg ->
                previewImageId = null
                val index = processedMessages.indexOfFirst { uiMsg ->
                    uiMsg.msg.id == msg.id || uiMsg.msg.attachments.any { it.messageId == msg.id }
                }
                if (index >= 0) {
                    // شلنا كارد "Not a contact"، فما بقاش خاصنا offset
                    val offset = 0
                    coroutineScope.launch { listState.animateScrollToItem(maxOf(0, index + offset)) }
                    // We must highlight the grouped message's ID, not the raw msg.id, because ChatMessages uses uiMsg.msg.id for highlighting
                    val groupedMsgId = processedMessages[index].msg.id
                    highlightedMessageId = groupedMsgId
                    coroutineScope.launch {
                        kotlinx.coroutines.delay(1500)
                        if (highlightedMessageId == groupedMsgId) highlightedMessageId = null
                    }
                }
            },
            onSaveToGallery = { msg ->
                val url = msg.attachments.firstOrNull { it.type == AttachmentType.IMAGE }?.url
                if (!url.isNullOrEmpty()) {
                    val uri = android.net.Uri.parse(url)
                    val scheme = uri.scheme
                    if (scheme == "http" || scheme == "https") {
                        val request = android.app.DownloadManager.Request(uri)
                            .setTitle("Saving Image")
                            .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_PICTURES, "IMG_${System.currentTimeMillis()}.jpg")
                            .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        val dm = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                        dm.enqueue(request)
                        android.widget.Toast.makeText(context, "Downloading image...", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                val resolver = context.contentResolver
                                val contentValues = android.content.ContentValues().apply {
                                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}.jpg")
                                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES)
                                }
                                val imageUri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                                if (imageUri != null) {
                                    resolver.openInputStream(uri)?.use { input ->
                                        resolver.openOutputStream(imageUri)?.use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        android.widget.Toast.makeText(context, "تم الحفظ في المعرض", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    android.widget.Toast.makeText(context, "فشل الحفظ", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
            },
            onReply = { msg ->
                previewImageId = null
                if (!isSelectionMode) replyingTo = msg
            },
            onShare = { msg ->
                val url = msg.attachments.firstOrNull { it.type == AttachmentType.IMAGE }?.url
                if (!url.isNullOrEmpty()) {
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, url)
                    }
                    context.startActivity(android.content.Intent.createChooser(intent, "Share Image"))
                }
            },
            onDelete = { msg ->
                previewImageId = null
                deleteDialogIds = setOf(msg.id)
            }
        )
    }

    // ===== لوحة معلومات القناة: تنزلق من يمين الشاشة، ليست بشاشة كاملة =====
    if (showChannelInfo) {
        val channelMedia = remember(messages) {
            messages.sortedByDescending { it.timestamp }
                .flatMap { m ->
                    m.attachments
                        .filter { it.type == AttachmentType.IMAGE || it.type == AttachmentType.VIDEO }
                        .map { att -> ChannelMediaItem(messageId = m.id, url = att.thumbnailUrl ?: att.url, isVideo = att.type == AttachmentType.VIDEO) }
                }
        }
        ChannelInfoPanel(
            name = currentChat?.name ?: name,
            avatarUrl = currentChat?.avatarUrl,
            isVerified = currentChat?.isVerified == true,
            subscribersLabel = channelSubscriberCount?.let { c -> if (c == 1) "1 abonné" else "$c abonnés" },
            description = null,
            mediaItems = channelMedia,
            admins = emptyList(),
            onInvite = {
                try {
                    val shareText = "انضم إلى ${currentChat?.name ?: name} على Owlino"
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(android.content.Intent.createChooser(intent, "دعوة"))
                } catch (e: Exception) {}
            },
            isMuted = currentChat?.isMuted ?: false,
            onMuteToggle = {
                coroutineScope.launch {
                    val newState = !(currentChat?.isMuted ?: false)
                    val myId = com.example.supabase.auth.currentUserOrNull()?.id
                    if (myId != null) {
                        val success = com.example.ui.ChatStateManager.setMuted(myId, listOf(chatId), newState, chatDao)
                        if (!success) {
                            android.widget.Toast.makeText(context, "خطأ في الاتصال، يرجى المحاولة لاحقاً", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            onMediaClick = { msgId ->
                showChannelInfo = false
                previewImageId = msgId
            },
            onReport = {
                android.widget.Toast.makeText(context, "تم إرسال البلاغ", android.widget.Toast.LENGTH_SHORT).show()
            },
            onLeave = {
                showChannelInfo = false
                coroutineScope.launch {
                    try {
                        // مغادرة حقيقية في السيرفر (الأدمن لا يغادر قناته)
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            com.example.supabase.postgrest.rpc("leave_channel", kotlinx.serialization.json.buildJsonObject { put("p_chat_id", kotlinx.serialization.json.JsonPrimitive(chatId)) })
                        }
                    } catch (e: Exception) {}
                    try {
                        chatDao.deleteChatById(chatId)
                        cachedChatDao.deleteChatById(chatId)
                    } catch (e: Exception) {}
                }
                onBack()
            },
            onDismiss = { showChannelInfo = false }
        )
    }
}
}
@Composable
fun ChatHeaderMenu(
    chatType: String?,
    isMuted: Boolean,
    isPinned: Boolean,
    isArchived: Boolean,
    expanded: Boolean,
    onAction: (String) -> Unit,
    onClose: () -> Unit,
    isBlocked: Boolean = false
) {
    val menuTr = LocalTranslation.current
    var menuState by remember(expanded) { mutableStateOf(if (chatType == "channel") "more" else "main") }
    var previousState by remember(expanded) { mutableStateOf(menuState) }

    MaterialTheme(
        shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(14.dp)),
        colorScheme = MaterialTheme.colorScheme.copy(surfaceTint = Color.Transparent, surface = com.example.ui.SettingsColors.surface)
    ) {
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = onClose,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .background(com.example.ui.SettingsColors.surface)
                .width(180.dp)
        ) {
            androidx.compose.animation.AnimatedContent(
                targetState = menuState,
                transitionSpec = {
                    if (targetState != "main" && initialState == "main") {
                        (androidx.compose.animation.slideInHorizontally(animationSpec = tween(150)) { it } + androidx.compose.animation.fadeIn(animationSpec = tween(150))) .togetherWith(
                        androidx.compose.animation.slideOutHorizontally(animationSpec = tween(150)) { -it } + androidx.compose.animation.fadeOut(animationSpec = tween(150)))
                    } else {
                        (androidx.compose.animation.slideInHorizontally(animationSpec = tween(150)) { -it } + androidx.compose.animation.fadeIn(animationSpec = tween(150))) .togetherWith(
                        androidx.compose.animation.slideOutHorizontally(animationSpec = tween(150)) { it } + androidx.compose.animation.fadeOut(animationSpec = tween(150)))
                    }
                }
            ) { state ->
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    when (state) {
                        "main" -> {
                            if (chatType != "channel") {
                                MenuRowItem(if (isMuted) "Unmute" else "Mute", if (isMuted) com.composables.icons.lucide.Lucide.BellOff else com.composables.icons.lucide.Lucide.Bell, { 
                                    if (isMuted) {
                                        onAction("mute")
                                    } else {
                                        previousState = "main"
                                        menuState = "mute"
                                    }
                                })
                                MenuRowItem(if (isPinned) "Unpin Chat" else "Pin Chat", com.composables.icons.lucide.Lucide.Pin, { onAction("pin") })
                                MenuRowItem("Favorites", com.composables.icons.lucide.Lucide.Star, { onAction("favorites") })
                                MenuRowItem(if (isArchived) "Unarchive" else "Archive", com.composables.icons.lucide.Lucide.Archive, { onAction("archive") })
                                MenuRowItem("Clear History", com.composables.icons.lucide.Lucide.Eraser, { onAction("clear_history") }, tint = Color(0xFFF59E0B))
                                MenuRowItem("Delete Chat", com.composables.icons.lucide.Lucide.Trash2, { onAction("delete_chat") }, tint = Color(0xFFEF4444))
                                MenuRowItem("More", androidx.compose.material.icons.Icons.Default.MoreHoriz, { 
                                    previousState = "main"
                                    menuState = "more" 
                                })
                                MenuRowItem(
                                    text = if (isBlocked) menuTr.unblock else menuTr.block,
                                    icon = if (isBlocked) com.composables.icons.lucide.Lucide.UserCheck else com.composables.icons.lucide.Lucide.Ban,
                                    onClick = { onAction("block") },
                                    tint = if (isBlocked) null else Color(0xFFEF4444)
                                )
                            }
                        }
                        "more" -> {
                            MenuRowItem("Photos", com.composables.icons.lucide.Lucide.Image, { onAction("photos") })
                            MenuRowItem("Videos", com.composables.icons.lucide.Lucide.Video, { onAction("videos") })
                            MenuRowItem("Documents", com.composables.icons.lucide.Lucide.FileText, { onAction("documents") })
                            MenuRowItem("Voice", com.composables.icons.lucide.Lucide.Mic, { onAction("voice") })
                            MenuRowItem("Links", com.composables.icons.lucide.Lucide.Link2, { onAction("links") })
                            MenuRowItem("Shared Files", com.composables.icons.lucide.Lucide.Folder, { onAction("files") })
                            MenuRowItem("Wallpaper", com.composables.icons.lucide.Lucide.Image, { onAction("wallpaper") })
                            
                            if (chatType != "channel") {
                                MenuRowItem("Back", com.composables.icons.lucide.Lucide.ArrowLeft, { 
                                    previousState = "more"
                                    menuState = "main" 
                                })
                            }
                        }
                        "mute" -> {
                            MenuRowItem("1 hour", com.composables.icons.lucide.Lucide.BellOff, { onAction("mute_1h") })
                            MenuRowItem("8 hours", com.composables.icons.lucide.Lucide.BellOff, { onAction("mute_8h") })
                            MenuRowItem("24 hours", com.composables.icons.lucide.Lucide.BellOff, { onAction("mute_24h") })
                            MenuRowItem("Always", com.composables.icons.lucide.Lucide.BellOff, { onAction("mute_forever") })
                            
                            MenuRowItem("Back", com.composables.icons.lucide.Lucide.ArrowLeft, { 
                                previousState = "mute"
                                menuState = "main" 
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MenuRowItem(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, tint: Color? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = text, tint = tint ?: MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = tint ?: MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun FloatingTopBar(
    name: String, avatarUrl: String? = null, isChannel: Boolean = false,
    subscriberCount: Int? = null,
    isTyping: Boolean = false, isOnline: Boolean = false, lastSeen: String? = null, statusOverride: String? = null, isBlocked: Boolean = false, onBack: () -> Unit,
    activeFilter: String? = null, onFilterChange: (String?) -> Unit = {}, onSearch: () -> Unit = {},
    menuExpanded: Boolean, onMenuExpandedChange: (Boolean) -> Unit,
    onProfileClick: () -> Unit = {},
    isMuted: Boolean = false,
    isPinned: Boolean = false,
    isVerified: Boolean = false,
    onMuteToggle: () -> Unit = {},
    onPinToggle: () -> Unit = {},
    isSearchMode: Boolean = false,
    onSearchModeChange: (Boolean) -> Unit = {},
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onBlockToggle: () -> Unit = {},
    blockedByThem: Boolean = false,
    isArchived: Boolean = false,
    onArchiveToggle: () -> Unit = {},
    onDeleteChat: () -> Unit = {},
    onClearHistory: () -> Unit = {}
) {
    val isDark by com.example.ThemeManager.isDarkMode.collectAsState()
    val glassBg = com.example.ui.SettingsColors.surface
    val glassBorder = if (isDark) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.05f) else androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.05f)
    val textColor = if (isDark) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color(0xFF111827)
    val topBarIconColor = if (isDark) androidx.compose.ui.graphics.Color(0xFFE5E7EB) else androidx.compose.ui.graphics.Color(0xFF374151)
    val statusColor = if (isTyping || isOnline) androidx.compose.ui.graphics.Color(0xFF007AFF) else androidx.compose.ui.graphics.Color(0xFF6B7280)
    val context = androidx.compose.ui.platform.LocalContext.current

  if (isSearchMode) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(androidx.compose.ui.graphics.Color.Transparent)
        .padding(top = 8.dp, start = 8.dp, end = 8.dp, bottom = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = glassBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, glassBorder),
            modifier = Modifier.fillMaxWidth().height(46.dp),
            shadowElevation = 0.dp,
            tonalElevation = 0.dp
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Close Search", modifier = Modifier.clickable { onSearchModeChange(false); onSearchQueryChange("") }, tint = topBarIconColor)
                Spacer(modifier = Modifier.width(8.dp))
                androidx.compose.foundation.text.BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    textStyle = androidx.compose.ui.text.TextStyle(color = textColor, fontSize = 16.sp),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(com.example.ui.i18n.LocalTranslation.current.searchMessages, color = textColor.copy(alpha = 0.5f), fontSize = 16.sp)
                        }
                        innerTextField()
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    Icon(Icons.Outlined.Close, contentDescription = "Clear", modifier = Modifier.clickable { onSearchQueryChange("") }, tint = topBarIconColor)
                }
            }
        }
    }
  } else {

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(androidx.compose.ui.graphics.Color.Transparent)
      .padding(top = 8.dp, start = 8.dp, end = 8.dp, bottom = 8.dp),
      verticalAlignment = Alignment.CenterVertically
  ){
       // Independent Back Button
       Surface(
          shape = CircleShape,
          color = glassBg,
          border = androidx.compose.foundation.BorderStroke(1.dp, glassBorder),
          modifier = Modifier.size(42.dp).clickable { onBack() },
          shadowElevation = 0.dp,
          tonalElevation = 0.dp
       ){
          Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
               imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
               contentDescription = "Back",
               tint = topBarIconColor,
               modifier = Modifier.size(22.dp)
            )
          }
       }
       
       Spacer(modifier = Modifier.width(8.dp))
       
       // Independent Profile Area
       Surface(
          shape = RoundedCornerShape(24.dp),
          color = glassBg,
          border = androidx.compose.foundation.BorderStroke(1.dp, glassBorder),
          modifier = Modifier.weight(1f).height(42.dp).clickable { onProfileClick() },
          shadowElevation = 0.dp,
          tonalElevation = 0.dp
       ){
          Row(modifier = Modifier.fillMaxSize().padding(start = 4.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
              // Avatar
               Box(contentAlignment = Alignment.Center, modifier = Modifier.size(34.dp)) {
                   Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(androidx.compose.ui.graphics.Color(0xFFE5E7EB))) {
                      if (avatarUrl != null) {
                          coil.compose.AsyncImage(
                            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current).data(avatarUrl).crossfade(false).build(),
                            contentDescription = "Avatar",
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                     } else {
                         Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                             Text(name.take(1).uppercase(), color = androidx.compose.ui.graphics.Color.Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                         }
                     }
                   }
                   if (!isChannel && isOnline) {
                       Box(
                          modifier = Modifier
                             .align(Alignment.BottomEnd)
                             .size(11.dp)
                             .clip(CircleShape)
                             .background(if (isDark) androidx.compose.ui.graphics.Color(0xFF1C1C1D) else androidx.compose.ui.graphics.Color.White)
                       ){
                          Box(
                             modifier = Modifier
                                .padding(1.5.dp)
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(androidx.compose.ui.graphics.Color(0xFF22C55E))
                          )
                       }
                   }
               }
               
               Spacer(modifier = Modifier.width(10.dp))
               
               // Name and Status
               Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                   Row(verticalAlignment = Alignment.CenterVertically) {
                       Text(if (blockedByThem) "-" else name, color = textColor, fontSize = 15.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                       if (isVerified && !blockedByThem) {
                           Spacer(modifier = Modifier.width(4.dp))
                           com.example.ui.VerifiedBadge(isVerified = true, iconSize = 16.dp)
                       }
                       if (isMuted) {
                           Spacer(modifier = Modifier.width(4.dp))
                           Icon(com.composables.icons.lucide.Lucide.BellOff, contentDescription = "Muted", tint = androidx.compose.ui.graphics.Color.Gray, modifier = Modifier.size(12.dp))
                       }
                       if (isPinned) {
                           Spacer(modifier = Modifier.width(4.dp))
                           Icon(com.composables.icons.lucide.Lucide.Pin, contentDescription = "Pinned", tint = androidx.compose.ui.graphics.Color.Gray, modifier = Modifier.size(12.dp))
                       }
                   }
                   
                   val statusText = if (statusOverride != null) {
                       statusOverride
                   } else if (isChannel) {
                       val c = subscriberCount ?: 0
                       if (c == 1) "1 Subscriber" else "$c Subscribers"
                   } else if (isTyping) {
                       "typing..."
                   } else if (isOnline) {
                       "online"
                   } else {
                       "last seen recently"
                   }
                   if (!blockedByThem) {
                       Text(statusText, color = statusColor, fontSize = 12.sp, lineHeight = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                   }
               }
          }
       }
       
       Spacer(modifier = Modifier.width(8.dp))
       
       // Actions
       Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
           if (!isChannel) {
               Surface(
                  shape = CircleShape,
                  color = glassBg,
                  border = androidx.compose.foundation.BorderStroke(1.dp, glassBorder),
                  modifier = Modifier.size(42.dp).clickable { onSearch() },
                  shadowElevation = 0.dp,
                  tonalElevation = 0.dp
               ){
                  Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                      Icon(com.composables.icons.lucide.Lucide.Search, contentDescription = "Search", tint = topBarIconColor, modifier = Modifier.size(18.dp))
                  }
               }
           }
           
           Surface(
              shape = CircleShape,
              color = glassBg,
              border = androidx.compose.foundation.BorderStroke(1.dp, glassBorder),
              modifier = Modifier.size(42.dp).clickable { onMenuExpandedChange(true) },
              shadowElevation = 0.dp,
              tonalElevation = 0.dp
           ){
              Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                  Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More", tint = topBarIconColor, modifier = Modifier.size(20.dp))
                  ChatHeaderMenu(
                      chatType = if (isChannel) "channel" else "chat",
                      isMuted = isMuted,
                      isPinned = isPinned,
                      isArchived = isArchived,
                      isBlocked = isBlocked,
                      expanded = menuExpanded,
                      onAction = { action ->
                          onMenuExpandedChange(false)
                          if (action == "mute" || action.startsWith("mute_")) onMuteToggle()
                          if (action == "pin") onPinToggle()
                          if (action == "block") onBlockToggle()
                          if (action == "archive") onArchiveToggle()
                          if (action == "delete_chat") onDeleteChat()
                          if (action == "clear_history") onClearHistory()
                          if (action == "favorites") onFilterChange("Favorites")
                          if (action == "photos") onFilterChange("Photos")
                          if (action == "videos") onFilterChange("Videos")
                          if (action == "documents") onFilterChange("Documents")
                          if (action == "voice") onFilterChange("Voice Messages")
                          if (action == "links") onFilterChange("Links")
                          if (action == "files") onFilterChange("Shared Files")
                      },
                      onClose = { onMenuExpandedChange(false) }
                  )
              }
           }
       }
  }

}
}
fun formatChatDateHeader(timestampMillis: Long, languageCode: String): String {
   val cal = java.util.Calendar.getInstance()
   cal.timeInMillis = timestampMillis
   val today = java.util.Calendar.getInstance()
   val yesterday = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
   fun sameDay(a: java.util.Calendar, b: java.util.Calendar) =
      a.get(java.util.Calendar.YEAR) == b.get(java.util.Calendar.YEAR) &&
      a.get(java.util.Calendar.DAY_OF_YEAR) == b.get(java.util.Calendar.DAY_OF_YEAR)
   val locale = when (languageCode) {
      "ar" -> java.util.Locale("ar")
      "fr" -> java.util.Locale.FRENCH
      else -> java.util.Locale.ENGLISH
   }
   return when {
      sameDay(cal, today) -> when (languageCode) {
         "ar" -> "اليوم"
         "fr" -> "Aujourd'hui"
         else -> "Today"
      }
      sameDay(cal, yesterday) -> when (languageCode) {
         "ar" -> "أمس"
         "fr" -> "Hier"
         else -> "Yesterday"
      }
      else -> java.text.SimpleDateFormat("d MMMM", locale).format(java.util.Date(timestampMillis))
   }
}

@Composable
fun ChatMessages(
   isChannel: Boolean = false,
   messages: List<UiMessage>,
   hasPinnedBanner: Boolean = false,
   name: String,
   onImageClick: (String) -> Unit = {},
   onReactionSelected: (MessageModel, String) -> Unit,
   onMoreClick: () -> Unit,
   onLongClick: (MessageModel, androidx.compose.ui.geometry.Rect, Boolean) -> Unit,
   activeContextMenuMessageId: String? = null,
   onReply: (MessageModel) -> Unit,
   modifier: Modifier = Modifier,
   listState: LazyListState = rememberLazyListState(),
   selectedMessages: Set<String> = emptySet(),
   isSelectionMode: Boolean = false,
   onToggleSelect: (String) -> Unit = {},
   contentPadding: PaddingValues = PaddingValues(start = 12.dp, end = 12.dp, top = 80.dp, bottom = 90.dp),
   isTyping: Boolean = false,
   firstUnreadIndex: Int? = null,
   isNetworkFetchComplete: Boolean = false,
   savedIndex: Int = -1,
   savedOffset: Int = 0,
   highlightedMessageId: String? = null,
   onHighlightMessage: (String?) -> Unit = {},
   onInlineButtonClick: (MessageModel, InlineButton) -> Unit = { _, _ -> }
){
   val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
  var initialScrollDone by remember { mutableStateOf(false) }
   // الكيبورد: نرجعو المنطق اللي كان خدام مزيان (تأكد المستخدم بلي فقاعاتو كترتفع مزيان بيه) —
   // بمجرد ما يظهر الكيبورد وكنا أصلاً فالأسفل أو آخر رسالة ديالي، كنمشيو لآخر رسالة بأنيميشن
   // سلسة. ما كنقفزوش لأول رسالة غير مقروءة إذا كان الكيبورد ظاهر (!imeVisible) باش ما
   // يبقاش يقفز للخلف فالمحادثات اللي فيها رسائل غير مقروءة.
   val imeVisible = WindowInsets.isImeVisible

  LaunchedEffect(messages.size, firstUnreadIndex, isNetworkFetchComplete, imeVisible) {
     if (messages.isNotEmpty()) {
         try {
            val target = messages.lastIndex
            if (!initialScrollDone) {
                // If we have a saved position, and the list is large enough to contain it, restore it.
                if (savedIndex != -1) {
                    if (messages.size > savedIndex) {
                        listState.scrollToItem(savedIndex, savedOffset)
                        initialScrollDone = true
                    } else if (isNetworkFetchComplete) {
                        // Fallback: network finished but list is still smaller than savedIndex (e.g. messages deleted)
                        listState.scrollToItem(target)
                        initialScrollDone = true
                    }
                    // If list is not large enough yet, we wait (do not set initialScrollDone to true).
                } else {
                    if (firstUnreadIndex != null && !imeVisible) {
                        listState.scrollToItem(firstUnreadIndex)
                    } else {
                        listState.scrollToItem(target)
                    }
                    initialScrollDone = true
                }
            } else {
                val atBottom = !listState.canScrollForward
                val isLastMessageMine = messages.lastOrNull()?.msg?.isMine == true
                if (atBottom || isLastMessageMine || imeVisible) {
                    // imeVisible مضافة: بمجرد ما يفتح الكيبورد، نرتفعو لآخر رسالة بأنيميشن.
                    listState.animateScrollToItem(target)
                }
            }
         } catch (e: Exception) {}
     }
  }

   if (messages.isEmpty()) {
       if (!isNetworkFetchComplete) {
           // Do not show empty state while loading to prevent flash
           Box(modifier = modifier.fillMaxSize().padding(contentPadding))
           return
       }
       Box(modifier = modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
          Surface(
             shape = RoundedCornerShape(16.dp),
             color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
          ){
             Text(
               text = "لا توجد رسائل بعد",
               fontSize = 14.sp,
               color = MaterialTheme.colorScheme.onBackground,
               modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
             )
          }
       }
       return
   }

   // ==== شارة التاريخ العائمة عند التمرير (متل تيليجرام) ====
   // ملاحظة مهمة: msg.timestamp (Long) ما كيتصاوبش من البيانات الحقيقية عند تحميل الرسائل
   // القديمة من الكاش/السيرفر (كيبقى بالقيمة الافتراضية = وقت الإنشاء = "دابا")، لهذا كانت
   // الشارة كتبين "اليوم" لجميع الرسائل بلا استثناء. المصدر الصحيح والموثوق هو createdAtExact
   // (نص ISO كيتصاوب فعلياً من created_at فكل الحالات) عبر parseTimestampSafe.
   val currentLangCode by com.example.ui.i18n.TranslationManager.currentLanguageCode.collectAsState()
   val idToTimestamp = remember(messages) {
       messages.associate { it.msg.id to parseTimestampSafe(it.msg.createdAtExact) }
   }
   val topDateLabel by remember(idToTimestamp, currentLangCode) {
       derivedStateOf {
           val firstMsgItem = listState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
               (info.key as? String)?.let { idToTimestamp.containsKey(it) } == true
           }
           val ts = (firstMsgItem?.key as? String)?.let { idToTimestamp[it] }
           ts?.let { formatChatDateHeader(it, currentLangCode) }
       }
   }
   var showDateBadge by remember { mutableStateOf(false) }
   LaunchedEffect(Unit) {
       snapshotFlow {
           Triple(listState.isScrollInProgress, listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
       }.collectLatest {
           showDateBadge = true
           kotlinx.coroutines.delay(1200)
           showDateBadge = false
       }
   }

   Box(modifier = modifier) {
     LazyColumn(
        state = listState,
        contentPadding = contentPadding,
        verticalArrangement = if (isChannel) Arrangement.Bottom else Arrangement.Top,
        modifier = Modifier
            .fillMaxSize()
            .imeNestedScroll()
     ){
       itemsIndexed(messages, key = { _, it -> it.msg.id }, contentType = { _, _ -> "message" }) { index, uiMsg ->
          val message = uiMsg.msg
          SwipeToReplyWrapper(modifier = Modifier, onReply = { if (!isSelectionMode) onReply(message) }) {
             val bounds = remember { arrayOf(androidx.compose.ui.geometry.Rect.Zero) }
             val isSelected = message.id in selectedMessages
             val isHighlighted = message.id == highlightedMessageId || message.id == activeContextMenuMessageId
             val isFirstInCluster = uiMsg.isFirst
             val isLastInCluster = uiMsg.isLast
             val itemBgColor by androidx.compose.animation.animateColorAsState(
                targetValue = if (isSelected) primaryPurple.copy(alpha = 0.1f) else if (isHighlighted) primaryPurple.copy(alpha = 0.15f) else Color.Transparent,
                animationSpec = tween(durationMillis = if (isHighlighted) 300 else 1000)
             )
             Box(modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates -> bounds[0] = coordinates.boundsInWindow() }
                .background(itemBgColor)
                .clickable(
                     enabled = isSelectionMode,
                     onClick = { onToggleSelect(message.id) }
                )
             ){
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = if (isSelectionMode) 12.dp else 0.dp), verticalAlignment = Alignment.CenterVertically) {
                     if (isSelectionMode) {
                         Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) primaryPurple else Color.Transparent)
                                .border(1.5.dp, if (isSelected) primaryPurple else primaryPurple.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                         ){
                            if (isSelected) {
                                Icon(Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(16.dp))
                            }
                         }
                         Spacer(modifier = Modifier.width(12.dp))
                     }

                     Column(modifier = Modifier.weight(1f)) {
                       if (isChannel) {
                         ChannelPostCard(
                            msg = message,
                            channelName = name,
                            onLightLongPress = { _, _ ->
                               if (isSelectionMode) onToggleSelect(message.id)
                               else {
                                   onLongClick(message, bounds[0], false)
                               }
                            },
                            onHeavyLongPress = { _, _ ->
                               if (!isSelectionMode) {
                                   onLongClick(message, bounds[0], true)
                               }
                            }
                         )
                       } else {
                         MessageBubble(
                            msg = message,
                            onImageClick = { clickedMsgId -> onImageClick(clickedMsgId) },
                            isFirstInCluster = isFirstInCluster,
                            isLastInCluster = isLastInCluster,
                            onReplyClick = {
                               if (message.replyTo != null) {
                                   val replyIndex = messages.indexOfFirst { it.msg.id == message.replyTo?.id }
                                   if (replyIndex >= 0) {
                                       coroutineScope.launch {
                                          listState.animateScrollToItem(replyIndex)
                                          onHighlightMessage(message.replyTo.id)
                                          kotlinx.coroutines.delay(1500)
                                          onHighlightMessage(null)
                                       }
                                   }
                               }
                            },
                            onDoubleTap = {
                               onReactionSelected(message, "love")
                            },
                            onLightLongPress = { _, _ ->
                               if (isSelectionMode) onToggleSelect(message.id)
                               else {
                                   onLongClick(message, bounds[0], false)
                               }
                            },
                            onHeavyLongPress = { _, _ ->
                               if (!isSelectionMode) {
                                   onLongClick(message, bounds[0], true)
                               }
                            }
                         )
                         val inlineRows = remember(message.replyMarkup) { parseInlineKeyboard(message.replyMarkup) }
                         if (!message.isMine && inlineRows.isNotEmpty()) {
                             InlineKeyboardGrid(rows = inlineRows, onClick = { b -> onInlineButtonClick(message, b) })
                         }
                       }
                     }
                }
             }
          }
        }
        if (isTyping) {
            item {
               TypingIndicatorBubble()
            }
        }
     } 

     androidx.compose.animation.AnimatedVisibility(
        visible = showDateBadge && topDateLabel != null,
        modifier = Modifier.align(Alignment.TopCenter).padding(top = if (hasPinnedBanner) 100.dp else 64.dp),
        enter = androidx.compose.animation.fadeIn(),
        exit = androidx.compose.animation.fadeOut()
     ) {
        Surface(
           shape = RoundedCornerShape(50),
           color = Color(0xCC202020)
        ) {
           Text(
              text = topDateLabel ?: "",
              color = Color.White,
              fontSize = 10.sp,
              modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
           )
        }
     }
   }
}


@Composable
fun MessageAttachments(attachments: List<Attachment>, onImageClick: (String) -> Unit =
{}) {
   if (attachments.isEmpty()) return
   val mediaAttachments = attachments.filter { it.type == AttachmentType.IMAGE || it.type ==
AttachmentType.VIDEO }
   val docAttachments = attachments.filter { it.type == AttachmentType.DOCUMENT || it.type == AttachmentType.AUDIO || it.type == AttachmentType.VOICE }

  Column(modifier = Modifier.widthIn(max = 280.dp).padding(bottom = 2.dp)) {
     if (mediaAttachments.isNotEmpty()) {
         val total = mediaAttachments.size
         Box(modifier = Modifier.clip(RoundedCornerShape(12.dp))) {
           if (total == 1) {
               Box(modifier = Modifier.clickable { onImageClick(mediaAttachments[0].url) },
contentAlignment = Alignment.Center) {
                  val ratio = mediaAttachments[0].aspectRatio ?: 1f // fallback
                  coil.compose.AsyncImage(
                      model = mediaAttachments[0].url,
                      contentDescription = "Image attachment",
                      modifier = Modifier
                        .width(280.dp) // Fixed max width
                        .aspectRatio(ratio),
                      contentScale = androidx.compose.ui.layout.ContentScale.Crop
                  )
                  if (mediaAttachments[0].type == AttachmentType.VIDEO) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = MaterialTheme.colorScheme.background, modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.onBackground.copy(alpha=0.5f), CircleShape).padding(8.dp))
                 }
              }
           } else if (total == 2 || total == 3) {
              Row(
                 modifier = Modifier.width(280.dp).height(if(total == 2) 160.dp else 120.dp),
                 horizontalArrangement = Arrangement.spacedBy(2.dp)
              ){
                 mediaAttachments.forEach { attachment ->
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable {
onImageClick(attachment.url) }, contentAlignment = Alignment.Center) {
                        coil.compose.AsyncImage(
                            model = attachment.url,
                            contentDescription = "Image attachment",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        if (attachment.type == AttachmentType.VIDEO) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = MaterialTheme.colorScheme.background, modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.onBackground.copy(alpha=0.5f), CircleShape).padding(4.dp))
                        }
                    }
                 }
              }
           } else {
              Column(
                 modifier = Modifier.width(280.dp),
                 verticalArrangement = Arrangement.spacedBy(2.dp)
              ){
                 val rows = mediaAttachments.chunked(2)
                 rows.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ){
                        rowItems.forEach { attachment ->
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable {
onImageClick(attachment.url) }, contentAlignment = Alignment.Center) {
                               coil.compose.AsyncImage(
                                 model = attachment.url,
                                 contentDescription = "Image attachment",
                                 modifier = Modifier.fillMaxSize(),
                                 contentScale = androidx.compose.ui.layout.ContentScale.Crop
                           )
                           if (attachment.type == AttachmentType.VIDEO) {
                               Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = MaterialTheme.colorScheme.background, modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.onBackground.copy(alpha=0.5f), CircleShape).padding(4.dp))
                           }
                         }
                     }
                     if (rowItems.size == 1) {
                         Spacer(modifier = Modifier.weight(1f))
                     }
                  }
               }
             }
           }
         }
         Spacer(modifier = Modifier.height(4.dp))
      }

     docAttachments.forEach { attachment ->
       Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically
       ){
          val iconVector = when (attachment.type) {
            AttachmentType.AUDIO, AttachmentType.VOICE -> Icons.Filled.PlayArrow
            else -> Icons.AutoMirrored.Outlined.InsertDriveFile
          }
          val bgColor = when (attachment.type) {
            AttachmentType.AUDIO, AttachmentType.VOICE -> Color(0xFFFF9800)
            else -> Color(0xFF2196F3)
          }
          val titleStr = when (attachment.type) {
            AttachmentType.AUDIO, AttachmentType.VOICE -> "Voice Message"
            else -> attachment.fileName ?: "Document"
          }
          val subStr = when (attachment.type) {
            AttachmentType.AUDIO, AttachmentType.VOICE -> "Audio file"
            else -> "File"
          }

          Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(bgColor),
            contentAlignment = Alignment.Center
              ){
                   Icon(
                      imageVector = iconVector,
                      contentDescription = titleStr,
                      tint = MaterialTheme.colorScheme.background,
                      modifier = Modifier.size(24.dp)
                   )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                  text = titleStr,
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Medium,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = subStr,
                  fontSize = 13.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
fun SwipeToReplyWrapper(
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    onReply: () -> Unit,
    content: @Composable () -> Unit
) {
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val triggerOffset = with(density) { 60.dp.toPx() }
    val view = androidx.compose.ui.platform.LocalView.current
    
    Box(
        modifier = modifier.pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragEnd = {
                    if (offsetX.value >= triggerOffset || offsetX.value <= -triggerOffset) {
                        onReply()
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                    }
                    scope.launch {
                        offsetX.animateTo(0f, animationSpec = tween(300))
                    }
                },
                onDragCancel = {
                    scope.launch {
                        offsetX.animateTo(0f, animationSpec = tween(300))
                    }
                },
                onHorizontalDrag = { change, dragAmount ->
                    val newVal = offsetX.value + (dragAmount * 0.5f) // add resistance
                    scope.launch {
                        offsetX.snapTo(newVal.coerceIn(-triggerOffset * 1.5f, triggerOffset * 1.5f))
                    }
                }
            )
        },
        contentAlignment = Alignment.Center
    ) {
        // Icon in background
        Row(
            modifier = Modifier.matchParentSize(),
            horizontalArrangement = if (offsetX.value > 0) Arrangement.Start else Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconAlpha = (Math.abs(offsetX.value) / triggerOffset).coerceIn(0f, 1f)
            val iconScale = 0.5f + (0.5f * iconAlpha)
            Icon(
                imageVector = Icons.Default.Reply,
                contentDescription = "Reply",
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .graphicsLayer(alpha = iconAlpha, scaleX = iconScale, scaleY = iconScale),
                tint = Color.Gray
            )
        }
        
        Box(modifier = Modifier.graphicsLayer(translationX = offsetX.value)) {
            content()
        }
    }
}

@Composable
fun SelectionTopBar(
    selectedCount: Int = 0, count: Int = selectedCount,
    onClearSelection: () -> Unit = {},
    onClear: () -> Unit = onClearSelection,
    onCopy: () -> Unit = {},
    onForward: () -> Unit = {},
    onDelete: () -> Unit = {},
    onPin: () -> Unit = {}
) {
    TopAppBar(
        title = { Text("$selectedCount") },
        navigationIcon = {
            IconButton(onClick = onClearSelection) {
                Icon(androidx.compose.material.icons.Icons.Default.Close, contentDescription = "Clear")
            }
        },
        actions = {
            IconButton(onClick = onCopy) { Icon(androidx.compose.material.icons.Icons.Default.ContentCopy, "Copy") }
            IconButton(onClick = onDelete) { Icon(androidx.compose.material.icons.Icons.Default.Delete, "Delete") }
        }
    )
}

@Composable
fun PinnedMessageBar(
    message: MessageModel,
    onUnpin: () -> Unit = {},
    onClick: () -> Unit = {},
    count: Int = 1,
    currentIndex: Int = 0
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .fillMaxWidth()
            .scale(scale)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.05f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Indicator lines
            Row(
                modifier = Modifier
                    .width(3.dp)
                    .height(24.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    for (i in 0 until count) {
                        val isActive = i == currentIndex
                        val color by androidx.compose.animation.animateColorAsState(
                            targetValue = if (isActive) Color(0xFF34C759) else Color(0xFF34C759).copy(alpha = 0.3f),
                            animationSpec = tween(300)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Pinned Message" + if (count > 1) " #${currentIndex + 1}" else "",
                        fontSize = 14.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        color = Color(0xFF34C759),
                        maxLines = 1,
                        lineHeight = 16.sp
                    )
                }
                Text(
                    text = message.text.ifEmpty { "Attachment" },
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Pin icon
            Icon(
                imageVector = Icons.Filled.PushPin,
                contentDescription = "Pinned",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = 45f }
            )
        }
    }
}

// ===== حوارات تأكيد الحظر/فك الحظر — مفصولة عن ChatDetailScreen عمداً =====
// ChatDetailScreen ضخمة بزاف، وإلا زدنا فيها كود كتوصل لحد كيولّد فيه R8 كود غالط
// (VerifyError: copy1 ... type=Reference: Composer). فصل هاد الأجزاء كيخفف عدد الـ registers.
@Composable
private fun BlockConfirmDialogs(
    showBlock: Boolean,
    showUnblock: Boolean,
    dialogName: String,
    chatId: String,
    otherUserId: String?,
    chatDao: com.example.data.ChatDao,
    scope: kotlinx.coroutines.CoroutineScope,
    surfaceColor: Color,
    textPrimary: Color,
    onDismiss: () -> Unit,
    onBlockedChange: (Boolean) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val blockTr = LocalTranslation.current
    if (showBlock) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = surfaceColor,
            title = { Text(blockTr.blockUser, color = textPrimary, fontWeight = FontWeight.Medium) },
            text = { Text(String.format(blockTr.blockUserQuestionFmt, dialogName), color = textPrimary, fontSize = 16.sp) },
            confirmButton = {
                TextButton(onClick = {
                    onDismiss()
                    val myId = supabase.auth.currentUserOrNull()?.id
                    if (otherUserId != null && myId != null) {
                        scope.launch {
                            val success = BlockManager.block(context, myId, otherUserId, chatDao)
                            if (success) {
                                onBlockedChange(true)
                            } else {
                                android.widget.Toast.makeText(context, "Error blocking user", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }) { Text(blockTr.block, color = Color(0xFFEF4444)) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(blockTr.cancel, color = Color(0xFF3B82F6)) }
            }
        )
    }
    if (showUnblock) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = surfaceColor,
            title = { Text(blockTr.unblockUserAction, color = textPrimary, fontWeight = FontWeight.Medium) },
            text = { Text(String.format(blockTr.unblockUserQuestionFmt, dialogName), color = textPrimary, fontSize = 16.sp) },
            confirmButton = {
                TextButton(onClick = {
                    onDismiss()
                    val myId = supabase.auth.currentUserOrNull()?.id
                    if (otherUserId != null && myId != null) {
                        scope.launch {
                            val success = BlockManager.unblock(context, myId, otherUserId, chatDao)
                            if (success) {
                                onBlockedChange(false)
                            } else {
                                android.widget.Toast.makeText(context, "Error unblocking user", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }) { Text(blockTr.unblock, color = Color(0xFF3B82F6)) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(blockTr.cancel, color = Color(0xFF3B82F6)) }
            }
        )
    }
}

// ===== حوار Forward — مفصول عن ChatDetailScreen لنفس السبب =====
@Composable
private fun ForwardMessageDialog(primaryColor: Color, onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    com.example.ui.i18n.LocalTranslation.current.forwardTo,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(listOf("Elena Rostova", "Project Team", "Tech News", "Saved Messages"), key = { it }) { contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    android.widget.Toast.makeText(context, "Forwarded to $contact", android.widget.Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(40.dp).clip(CircleShape).background(primaryColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(contact.take(1), color = primaryColor, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(contact, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

// بطاقة "إلغاء الحظر": نفس الـ padding الخارجي + نفس الشكل (28dp) + نفس الارتفاع الأدنى (48dp)
// + نفس ارتفاع سطر النص (22sp مع padding عمودي 10dp) الخاص بـ MessageInputBar، فلا يتغير حجمها.
@Composable
fun UnblockInputBar(text: String, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().background(Color.Transparent).padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 14.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, Color.Black.copy(alpha = 0.05f), RoundedCornerShape(28.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = Color(0xFF007AFF),
                fontSize = 16.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
fun MessageInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachmentSelected: (List<android.net.Uri>, AttachmentType) -> Unit,
    replyingTo: MessageModel?,
    onCancelReply: () -> Unit,
    editingMessage: MessageModel?,
    onCancelEdit: () -> Unit,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    var isRecording by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }
    var slideOffsetX by remember { mutableFloatStateOf(0f) }
    var slideOffsetY by remember { mutableFloatStateOf(0f) }
    var recordingDuration by remember { mutableIntStateOf(0) }

    // Telegram-style attachment panel: swaps in place of the keyboard instead of
    // opening a floating dialog.
    var showAttachmentPanel by remember { mutableStateOf(false) }
    val screenHeightDp = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp
    val minPanelHeight = screenHeightDp * 0.75f
    var capturedKeyboardHeight by remember { mutableStateOf(minPanelHeight) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val imeVisible = WindowInsets.isImeVisible
    LaunchedEffect(imeBottomPx, imeVisible) {
        if (imeVisible && imeBottomPx > 0) {
            val heightDp = with(density) { imeBottomPx.toDp() }
            if (heightDp > minPanelHeight) capturedKeyboardHeight = heightDp
        }
    }
    val panelHeight = if (capturedKeyboardHeight > minPanelHeight) capturedKeyboardHeight else minPanelHeight

    
    // recordingDuration بالعُشر من الثانية (لعرض 0:02,6 مثل تيليجرام)
    LaunchedEffect(isRecording || isLocked) {
        if (isRecording || isLocked) {
            recordingDuration = 0
            while (true) {
                kotlinx.coroutines.delay(100)
                recordingDuration++
            }
        }
    }
    val recLangCode by com.example.ui.i18n.TranslationManager.currentLanguageCode.collectAsState()
    val recDecimalSep = if (recLangCode == "ar" || recLangCode == "fr") "," else "."
    val recHaptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val recContext = androidx.compose.ui.platform.LocalContext.current
    var recMediaRecorder by remember { mutableStateOf<android.media.MediaRecorder?>(null) }
    var recFile by remember { mutableStateOf<java.io.File?>(null) }
    var recStartTimeMs by remember { mutableStateOf(0L) }
    var recHasPermission by remember {
        mutableStateOf(androidx.core.content.ContextCompat.checkSelfPermission(recContext, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED)
    }
    val recPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted -> recHasPermission = granted }
    DisposableEffect(Unit) {
        onDispose {
            // نتخلص من المسجّل إن غادر المستخدم الشاشة أثناء التسجيل، بدون إرسال ملف ناقص
            try { recMediaRecorder?.stop() } catch (e: Exception) {}
            try { recMediaRecorder?.release() } catch (e: Exception) {}
            try { recFile?.delete() } catch (e: Exception) {}
        }
    }

    fun startVoiceRecording() {
        try {
            val dir = java.io.File(recContext.cacheDir, "voice_msgs").apply { mkdirs() }
            val file = java.io.File(dir, "voice_${System.currentTimeMillis()}.m4a")
            @Suppress("DEPRECATION")
            val recorder = if (android.os.Build.VERSION.SDK_INT >= 31) android.media.MediaRecorder(recContext) else android.media.MediaRecorder()
            recorder.setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioEncodingBitRate(64000)
            recorder.setAudioSamplingRate(44100)
            recorder.setOutputFile(file.absolutePath)
            recorder.prepare()
            recorder.start()
            recMediaRecorder = recorder
            recFile = file
            recStartTimeMs = System.currentTimeMillis()
        } catch (e: Exception) {
            recMediaRecorder = null
            recFile = null
            isRecording = false
        }
    }

    // send=true: يوقف التسجيل ويرسله كمرفق صوتي حقيقي عبر نفس مسار رفع المرفقات
    fun finishVoiceRecording(send: Boolean) {
        val recorder = recMediaRecorder
        val file = recFile
        val elapsedMs = System.currentTimeMillis() - recStartTimeMs
        recMediaRecorder = null
        recFile = null
        try { recorder?.stop() } catch (e: Exception) {}
        try { recorder?.release() } catch (e: Exception) {}
        if (send && file != null && elapsedMs >= 300) {
            // نضع مدة التسجيل بالمللي في اسم الملف لنستخرجها لاحقًا عند بناء المرفق
            val named = java.io.File(file.parent, "voice_dur${elapsedMs}_${file.name}")
            val finalFile = if (file.renameTo(named)) named else file
            onAttachmentSelected(listOf(android.net.Uri.fromFile(finalFile)), AttachmentType.VOICE)
        } else {
            try { file?.delete() } catch (e: Exception) {}
        }
    }
    
    val isTextMode = text.trim().isNotEmpty()
    
    Column(modifier = Modifier.fillMaxWidth().background(Color.Transparent).padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 14.dp)) {
        Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(com.example.ui.SettingsColors.surface)
                .border(1.dp, Color.Black.copy(alpha = 0.05f), RoundedCornerShape(28.dp))
        ) {
            // Reply / Edit preview (Inside the rounded bubble!)
            AnimatedVisibility(visible = replyingTo != null || editingMessage != null) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (editingMessage != null) Icons.Default.Edit else Icons.AutoMirrored.Outlined.Reply,
                            contentDescription = null,
                            tint = Color(0xFF555555),
                            modifier = Modifier.padding(horizontal = 8.dp).size(20.dp)
                        )
                        Box(modifier = Modifier.width(3.dp).height(30.dp).background(Color(0xFF555555)).clip(RoundedCornerShape(1.5.dp)))
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (editingMessage != null) "Edit Message" else if (replyingTo?.isMine == true) "You" else replyingTo?.senderName?.takeIf { it.isNotBlank() } ?: "User",
                                color = Color(0xFF555555), fontSize = 13.sp, fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = (editingMessage ?: replyingTo)?.text ?: "",
                                color = Color.Gray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = {
                            if (editingMessage != null) onCancelEdit() else onCancelReply()
                        }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.Close, "Cancel", tint = Color.Gray, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 4.dp).heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isRecording || isLocked) {
                    // Recording UI (بشكل تيليجرام): نقطة حمراء نابضة + المؤقت بالعُشر + "اسحب للإلغاء"
                    val dotAlpha by rememberInfiniteTransition(label = "rec_dot").animateFloat(
                        initialValue = 1f,
                        targetValue = 0.25f,
                        animationSpec = infiniteRepeatable(animation = tween(700), repeatMode = RepeatMode.Reverse),
                        label = "rec_dot_alpha"
                    )
                    Row(
                        modifier = Modifier.weight(1f).height(44.dp).padding(start = 16.dp, end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(10.dp).graphicsLayer { alpha = dotAlpha }.clip(CircleShape).background(Color(0xFFEF4444)))
                        Spacer(Modifier.width(10.dp))
                        val recTotalSec = recordingDuration / 10
                        Text(
                            String.format("%d:%02d%s%d", recTotalSec / 60, recTotalSec % 60, recDecimalSep, recordingDuration % 10),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                            maxLines = 1
                        )
                        
                        if (!isLocked) {
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(2.dp))
                            Text(LocalTranslation.current.slideToCancel, color = Color.Gray, fontSize = 15.sp, maxLines = 1, modifier = Modifier.offset { androidx.compose.ui.unit.IntOffset(slideOffsetX.roundToInt(), 0) })
                            Spacer(Modifier.weight(1f))
                        } else {
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { isLocked = false; isRecording = false; slideOffsetX = 0f; slideOffsetY = 0f; finishVoiceRecording(send = false) }) {
                                Icon(Icons.Outlined.Delete, "Delete", tint = Color.Red, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                } else {
                    // Text Input UI
                    leading?.invoke()
                    IconButton(
                        onClick = {
                            if (showAttachmentPanel) {
                                showAttachmentPanel = false
                            } else {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                showAttachmentPanel = true
                            }
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Default.Add, "Attach", tint = Color(0xFF007AFF), modifier = Modifier.size(28.dp))
                    }
                    
                    Box(
                        modifier = Modifier.weight(1f).padding(vertical = 10.dp)
                    ) {
                        if (text.isEmpty()) {
                            Text("Message", color = Color.Gray, fontSize = 16.sp)
                        }
                        androidx.compose.foundation.text.BasicTextField(
                            value = text,
                            onValueChange = onTextChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusEvent { if (it.isFocused) showAttachmentPanel = false },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 16.sp, 
                                lineHeight = 22.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                            maxLines = 5
                        )
                    }
                    trailing?.invoke()
                }
                
                // Send / Mic Button
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.animation.AnimatedContent(
                        targetState = isTextMode || isLocked,
                        transitionSpec = {
                            androidx.compose.animation.scaleIn() + androidx.compose.animation.fadeIn() togetherWith
                            androidx.compose.animation.scaleOut() + androidx.compose.animation.fadeOut()
                        },
                        label = "send_mic_anim"
                    ) { showSend ->
                        if (showSend) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable {
                                    if (isTextMode) {
                                        onSend()
                                    } else if (isLocked) {
                                        isLocked = false
                                        isRecording = false
                                        finishVoiceRecording(send = true)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.ArrowUpward, "Send", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .offset { androidx.compose.ui.unit.IntOffset(slideOffsetX.roundToInt(), slideOffsetY.roundToInt()) }
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isRecording && !isLocked) Color.Transparent else MaterialTheme.colorScheme.primary)
                                .pointerInput(Unit) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            if (!recHasPermission) {
                                                recPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                            } else {
                                                recHaptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                isRecording = true
                                                slideOffsetX = 0f
                                                slideOffsetY = 0f
                                                startVoiceRecording()
                                            }
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            if (isRecording && !isLocked) {
                                                if (slideOffsetX + dragAmount.x < 0) slideOffsetX += dragAmount.x
                                                if (slideOffsetY + dragAmount.y < 0) slideOffsetY += dragAmount.y
                                                
                                                if (slideOffsetX < -150) {
                                                    isRecording = false
                                                    slideOffsetX = 0f
                                                    slideOffsetY = 0f
                                                    finishVoiceRecording(send = false)
                                                } else if (slideOffsetY < -100) {
                                                    isLocked = true
                                                    isRecording = false
                                                    slideOffsetX = 0f
                                                    slideOffsetY = 0f
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            if (isRecording && !isLocked) {
                                                isRecording = false
                                                slideOffsetX = 0f
                                                slideOffsetY = 0f
                                                finishVoiceRecording(send = true)
                                            }
                                        },
                                        onDragCancel = {
                                            if (isRecording && !isLocked) {
                                                isRecording = false
                                                slideOffsetX = 0f
                                                slideOffsetY = 0f
                                                finishVoiceRecording(send = false)
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Mic, "Mic", tint = if (isRecording && !isLocked) Color.Transparent else Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                    }
                }
            }
        }

        // ===== أثناء التسجيل: دائرة الميكروفون الكبيرة + قفل التسجيل (تُرسم فوق البطاقة دون تغيير حجمها) =====
        val recScale by animateFloatAsState(
            targetValue = if (isRecording && !isLocked) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
            label = "rec_scale"
        )
        val haloScale by rememberInfiniteTransition(label = "rec_halo").animateFloat(
            initialValue = 1f,
            targetValue = 1.22f,
            animationSpec = infiniteRepeatable(animation = tween(800), repeatMode = RepeatMode.Reverse),
            label = "rec_halo_scale"
        )
        if (recScale > 0.01f) {
            // قفل التسجيل (فوق الدائرة)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-4).dp, y = (-68).dp)
                    .size(width = 40.dp, height = 64.dp)
                    .graphicsLayer { scaleX = recScale; scaleY = recScale; alpha = recScale.coerceIn(0f, 1f) }
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .background(Color(0xFF007AFF).copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(com.composables.icons.lucide.Lucide.Lock, contentDescription = "Lock", tint = Color(0xFF007AFF).copy(alpha = 0.65f), modifier = Modifier.size(20.dp))
            }
            // الدائرة الزرقاء الكبيرة (تتبع الإصبع)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-4).dp, y = (-4).dp)
                    .size(40.dp)
                    .offset { androidx.compose.ui.unit.IntOffset(slideOffsetX.roundToInt(), slideOffsetY.roundToInt()) },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .requiredSize(72.dp)
                        .graphicsLayer { scaleX = recScale * haloScale; scaleY = recScale * haloScale; alpha = 0.18f * recScale.coerceIn(0f, 1f) }
                        .clip(CircleShape)
                        .background(Color(0xFF007AFF))
                )
                Box(
                    modifier = Modifier
                        .requiredSize(72.dp)
                        .graphicsLayer { scaleX = recScale; scaleY = recScale; alpha = recScale.coerceIn(0f, 1f) }
                        .clip(CircleShape)
                        .background(Color(0xFF007AFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Mic, "Mic", tint = Color.White, modifier = Modifier.size(30.dp))
                }
            }
        }
    }

        AnimatedVisibility(
            visible = showAttachmentPanel,
            enter = expandVertically(animationSpec = tween(280, easing = LinearOutSlowInEasing)) +
                    fadeIn(animationSpec = tween(220)),
            exit = shrinkVertically(animationSpec = tween(220)) +
                    fadeOut(animationSpec = tween(160))
        ) {
            AttachmentPickerPanel(
                panelHeight = panelHeight,
                onAttachmentSelected = { uris, type ->
                    onAttachmentSelected(uris, type)
                    showAttachmentPanel = false
                }
            )
        }
    }

}
@Composable
fun TypingIndicatorBubble() {
  val dots = listOf(
     remember { Animatable(0f) },
      remember { Animatable(0f) },
      remember { Animatable(0f) }
  )

  LaunchedEffect(Unit) {
    dots.forEachIndexed { index, animatable ->
      launch {
         kotlinx.coroutines.delay(index * 200L)
         animatable.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
               animation = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
               repeatMode = RepeatMode.Reverse
            )
         )
      }
    }
  }

  Row(
     modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp, horizontal = 12.dp),
     horizontalArrangement = Arrangement.Start
  ){
     Surface(
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart =
4.dp, bottomEnd = 16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 1.dp
     ){
        Row(
             modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
             horizontalArrangement = Arrangement.spacedBy(4.dp),
             verticalAlignment = Alignment.CenterVertically
        ){
             dots.forEach { animatable ->
               Box(
                   modifier = Modifier
                     .size(6.dp)
                     .graphicsLayer {
                        translationY = -animatable.value * 4.dp.toPx()
                     }
                     .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha =
0.5f + (animatable.value * 0.5f)), CircleShape)
               )
             }
        }
        }
    }
}

@kotlinx.serialization.Serializable
data class TypingEvent(val user_id: String? = null, val chat_id: String, val is_typing: Boolean)

@kotlinx.serialization.Serializable
data class MessageReadRow(
  val message_id: String,
  val user_id: String? = null
)

@Composable
fun ChannelBottomBar() {
    Box(modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(16.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
        Text(com.example.ui.i18n.LocalTranslation.current.muteChannel, color = androidx.compose.ui.graphics.Color.Gray)
    }
}

@Composable
fun ChatWallpaper() {
    val themeConfig = LocalSettingsTheme.current
    val chatBgColor = themeConfig.chatBackground
    val isDarkBg = chatBgColor.luminance() < 0.4f
    Box(modifier = androidx.compose.ui.Modifier.fillMaxSize().background(chatBgColor)) {
        if (isDarkBg) {
            // خلفية داكنة: اللون المختار يظهر كما هو (أسود حقيقي) والرسومات تظهر كخطوط فاتحة خافتة فوقه
            val doodleMatrix = androidx.compose.ui.graphics.ColorMatrix(
                floatArrayOf(
                    0f, 0f, 0f, 0f, 255f,
                    0f, 0f, 0f, 0f, 255f,
                    0f, 0f, 0f, 0f, 255f,
                    -0.2126f * 1.2f, -0.7152f * 1.2f, -0.0722f * 1.2f, 0f, 245f * 1.2f
                )
            )
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.chat_wallpaper_bg),
                contentDescription = "Chat Background",
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(doodleMatrix)
            )
        } else {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.chat_wallpaper_bg),
                contentDescription = "Chat Background",
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                    color = chatBgColor,
                    blendMode = androidx.compose.ui.graphics.BlendMode.Color
                )
            )
            // طبقة لون المستخدم المختار تُطبَّق فوق صورة الخلفية بشفافية أقل لتظهر الرسومات
            Box(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxSize()
                    .background(chatBgColor.copy(alpha = 0.3f))
            )
        }
    }
}

fun formatShortRelativeTime(timestamp: String?): String {
    if (timestamp.isNullOrEmpty()) return ""
    return try {
        val millis = parseTimestampSafe(timestamp)
        val instant = java.time.Instant.ofEpochMilli(millis)
        val now = java.time.Instant.now()
        val duration = java.time.Duration.between(instant, now)
        val minutes = duration.toMinutes()
        val hours = duration.toHours()
        val days = duration.toDays()

        when {
            minutes < 1 -> "now"
            minutes < 60 -> "${minutes}m"
            hours < 24 -> "${hours}H"
            days < 7 -> "${days}j"
            days < 30 -> "${days / 7}w"
            else -> "${days / 30}M"
        }
    } catch (e: Exception) {
        ""
    }
}

@androidx.compose.runtime.Stable
data class UiMessage(
    val msg: MessageModel,
    val isFirst: Boolean,
    val isLast: Boolean
)



