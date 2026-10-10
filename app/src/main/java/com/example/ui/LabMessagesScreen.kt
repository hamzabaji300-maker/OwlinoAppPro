package com.example.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import com.example.ui.i18n.LocalTranslation
import com.example.ui.i18n.rememberExtraStrings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.bot.BotManager
import com.example.util.MediaStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/** الأنواع الأربعة: كل زر في الشريط السفلي يفتح شاشة رسائل من نوعه. */
enum class LabKind(val title: String, val isChannel: Boolean, val isGroup: Boolean, val isBot: Boolean) {
    CHANNEL("قناة Owlino", true, false, false),
    GROUP("مجموعة الأصدقاء", false, true, false),
    BOT("Owlino Bot", false, false, true),
    PRIVATE("سارة", false, false, false)
}

private fun nowLabel(): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

private fun labMsg(
    kind: LabKind, text: String, mine: Boolean, sender: String = "",
    markup: String? = null, views: String? = null,
    attachments: List<Attachment> = emptyList(), id: String = UUID.randomUUID().toString(),
    replyTo: MessageModel? = null, poll: Poll? = null
) = MessageModel(
    id = id, chatId = kind.name, senderId = if (mine) "me" else sender.ifBlank { "other" },
    senderName = sender, text = text, time = nowLabel(), isMine = mine,
    replyToId = replyTo?.id, replyTo = replyTo,
    attachments = attachments, replyMarkup = markup, viewsLabel = views,
    createdAtExact = java.time.Instant.now().toString(),
    poll = poll,
    channelReactions = if (kind.isChannel) listOf(ChannelReaction("👍", 120), ChannelReaction("🔥", 45)) else emptyList()
)

private const val START_MARKUP =
    """{"inline_keyboard":[[{"text":"📋 القائمة","callback_data":"menu"},{"text":"ℹ️ حول","callback_data":"about"}],[{"text":"🌐 الموقع","url":"https://example.com"}]]}"""
private const val MENU_MARKUP =
    """{"keyboard":[["📋 الطلبات","⚙️ الإعدادات"],["❓ مساعدة"]]}"""

/** حالة كل شاشة (تبقى محفوظة عند التنقل بين الأزرار الأربعة). */
class LabChatState(val kind: LabKind) {
    val messages = mutableStateListOf<MessageModel>()
    var text by mutableStateOf("")
    var replyingTo by mutableStateOf<MessageModel?>(null)
    var typing by mutableStateOf(false)
    var botStarted by mutableStateOf(kind != LabKind.BOT)
    var showReplyKb by mutableStateOf(true)
    var showBotMenu by mutableStateOf(false)
    var editingMessage by mutableStateOf<MessageModel?>(null)
    var selected by mutableStateOf<Set<String>>(emptySet())
    var deleteIds by mutableStateOf<Set<String>?>(null)
    var pinDialog by mutableStateOf<MessageModel?>(null)
    var forwardMsg by mutableStateOf<MessageModel?>(null)
    var toast by mutableStateOf<ToastNotification?>(null)
    var highlightedId by mutableStateOf<String?>(null)
    var deletingIds by mutableStateOf<Set<String>>(emptySet())
    var showPanel by mutableStateOf(false)
    var panelExpanded by mutableStateOf(false)
    var closeAttachSignal by mutableStateOf(0)
    var showPollComposer by mutableStateOf(false)

    /** إرسال استفتاء ثم محاكاة تصويت أعضاء آخرين خلال ثوانٍ (لا يوجد خادم في المختبر). */
    fun sendPoll(poll: Poll, scope: CoroutineScope) {
        val id = UUID.randomUUID().toString()
        messages += labMsg(kind, "", true, poll = poll, id = id, views = if (kind.isChannel) "1" else null)
        scope.launch {
            val rnd = java.util.Random()
            repeat(4) {
                delay(1600)
                update(messages.firstOrNull { it.id == id } ?: return@launch) { m ->
                    val p = m.poll ?: return@update m
                    if (p.endsAt != null && System.currentTimeMillis() >= p.endsAt) return@update m
                    val opts = p.options.toMutableList()
                    val k = rnd.nextInt(opts.size)
                    opts[k] = opts[k].copy(votes = opts[k].votes + 1 + rnd.nextInt(3))
                    m.copy(poll = p.copy(options = opts))
                }
            }
        }
    }

    /** تصويت المستخدم: newVotes فارغة = سحب التصويت. يحترم إعادة التصويت والإغلاق. */
    fun vote(messageId: String, newVotes: Set<Int>) {
        val msg = messages.firstOrNull { it.id == messageId } ?: return
        val p = msg.poll ?: return
        if (p.endsAt != null && System.currentTimeMillis() >= p.endsAt) return
        val old = p.myVotes
        if (old.isNotEmpty() && !p.revoting && newVotes.isNotEmpty()) return
        val opts = p.options.mapIndexed { i, o ->
            o.copy(votes = (o.votes - (if (i in old) 1 else 0) + (if (i in newVotes) 1 else 0)).coerceAtLeast(0))
        }
        update(msg) { it.copy(poll = p.copy(options = opts, myVotes = newVotes)) }
    }
    var panelSearchActive by mutableStateOf(false)

    fun closePanel() {
        showPanel = false
        panelExpanded = false
        panelSearchActive = false
    }
    val recentEmojis = mutableStateListOf<String>()
    val recentStickers = mutableStateListOf<String>()

    private fun pushRecent(list: MutableList<String>, item: String, max: Int) {
        list.remove(item); list.add(0, item)
        while (list.size > max) list.removeAt(list.size - 1)
    }

    fun addEmoji(e: String) { text += e; pushRecent(recentEmojis, e, 32) }

    fun backspace() {
        val g = com.example.emoji.EmojiMessageUtils.splitGraphemes(text)
        text = g.dropLast(1).joinToString("")
    }

    /** إرسال GIF: رسالة بمرفق صورة برابط الـ GIF (كما في الأصل). */
    fun sendGif(gif: com.example.util.GifItem, scope: CoroutineScope) {
        val id = UUID.randomUUID().toString()
        val att = Attachment(messageId = id, type = AttachmentType.IMAGE, url = gif.url, aspectRatio = gif.aspectRatio)
        closePanel()
        messages += labMsg(kind, "", true, attachments = listOf(att), id = id, views = if (kind.isChannel) "1" else null)
        simulateReply(scope, "", attachmentName = "GIF")
    }

    /** إرسال ملصق: إيموجي وحده في رسالة، فتعرضه الفقاعة كإيموجي متحرك كبير. */
    fun sendSticker(emoji: String, scope: CoroutineScope) {
        pushRecent(recentStickers, emoji, 20)
        messages += labMsg(kind, emoji, true, views = if (kind.isChannel) "1" else null)
        simulateReply(scope, emoji)
    }

    /** حذف مع حركة التضبيب/التفتت ثم الإزالة الفعلية بعد انتهائها. */
    fun deleteWithEffect(ids: Set<String>, scope: CoroutineScope) {
        deletingIds = deletingIds + ids
        scope.launch {
            delay(DELETE_EFFECT_MS.toLong())
            messages.removeAll { it.id in ids }
            deletingIds = deletingIds - ids
        }
    }

    fun showToast(text: String, type: ToastType) { toast = ToastNotification(System.currentTimeMillis(), text, type) }

    /** استقبال رسالة معاد توجيهها من شاشة أخرى. */
    fun receiveForward(text: String, attachments: List<Attachment>) {
        messages += labMsg(kind, text, true, attachments = attachments.map { it.copy(messageId = it.messageId) })
    }

    /** نفس منطق الأصل: تفاعل واحد لكل رسالة، والضغط على نفس الإيموجي يزيله. */
    fun react(m: MessageModel, reaction: String) {
        val i = messages.indexOfFirst { it.id == m.id }
        if (i < 0) return
        val cur = messages[i]
        messages[i] = cur.copy(reactions = if (reaction in cur.reactions) emptyList() else listOf(reaction))
    }

    fun update(m: MessageModel, f: (MessageModel) -> MessageModel) {
        val i = messages.indexOfFirst { it.id == m.id }
        if (i >= 0) messages[i] = f(messages[i])
    }

    val botCommands = listOf(
        BotManager.Command("/start", "بدء المحادثة"),
        BotManager.Command("/menu", "عرض لوحة الأزرار"),
        BotManager.Command("/help", "المساعدة")
    )

    init {
        when (kind) {
            LabKind.CHANNEL -> {
                messages += labMsg(kind, "مرحبًا بكم في القناة الرسمية 👋\nهنا ستجدون آخر الأخبار والتحديثات.", false, views = "1.2K")
                messages += labMsg(kind, "تحديث جديد يصل قريبًا ✨ — تحسينات في شاشات الرسائل.", false, views = "980")
            }
            LabKind.GROUP -> {
                messages += labMsg(kind, "صباح الخير يا شباب ☀️", false, sender = "ياسين")
                messages += labMsg(kind, "هل جرّبتم الشاشة الجديدة؟", false, sender = "ياسين")
                messages += labMsg(kind, "نعم، التصميم رائع!", false, sender = "سارة")
            }
            LabKind.BOT -> Unit
            LabKind.PRIVATE -> {
                messages += labMsg(kind, "هاي 👋", false, sender = "سارة")
                messages += labMsg(kind, "كيف الحال؟", false, sender = "سارة")
            }
        }
    }

    private fun addTyped(scope: CoroutineScope, m: MessageModel, wait: Long = 900) {
        scope.launch {
            typing = true
            delay(wait)
            typing = false
            messages += m
        }
    }

    /** ردود محاكاة (لا يوجد خادم): بوت / عضو مجموعة / صديق. القناة لا ترد. */
    fun simulateReply(scope: CoroutineScope, userText: String, attachmentName: String? = null) {
        when {
            kind.isChannel -> Unit
            kind.isBot -> {
                val t = userText.trim()
                val m = when {
                    attachmentName != null -> labMsg(kind, "تم استلام الملف: $attachmentName ✅", false, "bot")
                    t == "/start" -> labMsg(kind, "أهلًا! أنا بوت تجريبي 🤖\nاختر من الأزرار أدناه.", false, "bot", markup = START_MARKUP)
                    t == "/menu" -> labMsg(kind, "هذه لوحة الأزرار:", false, "bot", markup = MENU_MARKUP)
                    t == "/help" -> labMsg(kind, "الأوامر: /start /menu /help\nيمكنك أيضًا إرسال ملف أو رسالة صوتية.", false, "bot")
                    else -> labMsg(kind, "أنت قلت: $t", false, "bot")
                }
                addTyped(scope, m)
            }
            kind.isGroup -> addTyped(scope, labMsg(kind, if (attachmentName != null) "وصلني $attachmentName 👍" else "تمام 👌", false, sender = "ياسين"), 1200)
            else -> addTyped(scope, labMsg(kind, if (attachmentName != null) "شكرًا على $attachmentName 🙏" else "😄 رائع!", false, sender = "سارة"), 1200)
        }
    }

    fun sendText(scope: CoroutineScope) {
        val t = text.trim()
        if (t.isEmpty()) return
        val editing = editingMessage
        if (editing != null) {
            update(editing) { it.copy(text = t, isEdited = true) }
            editingMessage = null
            text = ""
            return
        }
        val reply = replyingTo
        text = ""
        replyingTo = null
        messages += labMsg(kind, t, true, replyTo = reply, views = if (kind.isChannel) "1" else null)
        simulateReply(scope, t)
    }

    /** إرسال مرفقات حقيقية: نسخ الملف داخل التطبيق ثم عرضه في الفقاعة (صورة / فيديو / صوت / ملف / رسالة صوتية). */
    fun sendAttachments(context: Context, scope: CoroutineScope, uris: List<Uri>, type: AttachmentType) {
        scope.launch {
            val built = withContext(Dispatchers.IO) { uris.mapNotNull { copyToApp(context, it, type) } }
            if (built.isEmpty()) return@launch
            built.forEach { (att, _) -> MediaIndex.put(att.messageId, File(Uri.parse(att.url).path ?: "").absolutePath) }
            val marker = when (type) {
                AttachmentType.IMAGE -> "[Photo]"
                AttachmentType.DOCUMENT -> "[Document]"
                else -> ""
            }
            if (type == AttachmentType.IMAGE) {
                // عدة صور = ألبوم في فقاعة واحدة
                val id = built.first().first.messageId
                val atts = built.map { it.first.copy(messageId = id) }
                MediaIndex.put(id, File(Uri.parse(atts.first().url).path ?: "").absolutePath)
                messages += labMsg(kind, marker, true, attachments = atts, id = id, views = if (kind.isChannel) "1" else null)
            } else {
                built.forEach { (att, _) ->
                    messages += labMsg(kind, marker, true, attachments = listOf(att), id = att.messageId, views = if (kind.isChannel) "1" else null)
                }
            }
            simulateReply(scope, "", attachmentName = built.last().first.fileName)
        }
    }

    private fun copyToApp(context: Context, uri: Uri, type: AttachmentType): Pair<Attachment, File>? {
        return try {
            var name: String? = null
            if (uri.scheme == "content") {
                context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                    if (c.moveToFirst()) {
                        val ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (ni >= 0) name = c.getString(ni)
                    }
                }
            }
            val picked = PickedFileMetaCache.map[uri.toString()]
            val fileName = name ?: picked?.first ?: uri.lastPathSegment?.substringAfterLast('/') ?: "file"
            val id = UUID.randomUUID().toString()
            val dir = File(context.filesDir, "lab_media").apply { mkdirs() }
            val dest = File(dir, "${id}_$fileName")
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { out -> input.copyTo(out) }
            } ?: return null
            if (dest.length() == 0L) { dest.delete(); return null }

            val durMs = if (type == AttachmentType.VOICE) Regex("voice_dur(\\d+)_").find(fileName)?.groupValues?.get(1)?.toLongOrNull() else null
            val mime = when (type) {
                AttachmentType.VOICE -> "audio/mp4"
                else -> context.contentResolver.getType(uri) ?: MediaStorage.mimeFor(dest, null)
            }
            var ratio: Float? = null
            if (type == AttachmentType.IMAGE) {
                val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(dest.absolutePath, o)
                if (o.outWidth > 0 && o.outHeight > 0) ratio = o.outWidth.toFloat() / o.outHeight
            }
            Attachment(
                messageId = id, type = type, url = Uri.fromFile(dest).toString(),
                fileName = if (type == AttachmentType.VOICE) "voice.m4a" else fileName,
                fileSize = dest.length(), mimeType = mime, durationMs = durMs, aspectRatio = ratio
            ) to dest
        } catch (e: Exception) {
            null
        }
    }
}

private fun buildUi(list: List<MessageModel>, isGroup: Boolean): List<UiMessage> =
    list.mapIndexed { i, m ->
        val prev = list.getOrNull(i - 1)
        val next = list.getOrNull(i + 1)
        UiMessage(
            msg = m,
            isFirst = prev == null || !sameSenderCluster(prev, m, isGroup),
            isLast = next == null || !sameSenderCluster(m, next, isGroup)
        )
    }

/** شاشة الرسائل: نفس تركيب ChatDetailScreen الأصلية (خلفية + قائمة + شريط علوي عائم + شريط إدخال). */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun LabMessagesScreen(
    state: LabChatState,
    onBack: () -> Unit = {},
    forwardTargets: List<LabChatState> = emptyList()
) {
    val kind = state.kind
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val listState = rememberLazyListState()
    val extra = rememberExtraStrings()
    val theme = LocalSettingsTheme.current.theme
    var menuExpanded by remember { mutableStateOf(false) }
    var contextMsg by remember { mutableStateOf<MessageModel?>(null) }
    var contextFull by remember { mutableStateOf(false) }

    val snapshot = state.messages.toList()
    val uiMessages = remember(snapshot) { buildUi(snapshot, kind.isGroup) }
    val replyKeyboardRows = remember(snapshot) { currentReplyKeyboard(snapshot) }
    val pinned = snapshot.filter { it.isPinned }
    var pinIndex by remember { mutableStateOf(0) }
    val isSelectionMode = state.selected.isNotEmpty()
    val pollVote = remember(state) { { id: String, v: Set<Int> -> state.vote(id, v) } }
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val imeVisible = WindowInsets.isImeVisible
    val collapsedH = (configuration.screenHeightDp * 0.42f).dp
    val expandedH = (configuration.screenHeightDp * 0.82f).dp
    val panelHeightState = androidx.compose.animation.core.animateDpAsState(
        targetValue = if (state.panelExpanded || state.panelSearchActive) expandedH else collapsedH,
        animationSpec = androidx.compose.animation.core.tween(320, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "panel_height"
    )

    BackHandler(enabled = isSelectionMode) { state.selected = emptySet() }
    BackHandler(enabled = state.showPanel && !isSelectionMode) {
        if (state.panelExpanded) state.panelExpanded = false else state.closePanel()
    }
    // عند ظهور لوحة المفاتيح تُغلق اللوحة (يتبادلان نفس المكان)
    // (إلا إذا كانت لوحة المفاتيح ظهرت بسبب حقل البحث داخل اللوحة نفسها)
    LaunchedEffect(imeVisible) { if (imeVisible && !state.panelSearchActive) state.closePanel() }

    Box(modifier = Modifier.fillMaxSize()) {
        ChatWallpaper()
        Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)).imePadding()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    CompositionLocalProvider(
                        LocalPollVote provides pollVote,
                        LocalPollChannelAuthor provides (if (kind.isChannel) kind.title else null)
                    ) {
                    ChatMessages(
                        messages = uiMessages,
                        isChannel = kind.isChannel,
                        isGroup = kind.isGroup,
                        hasPinnedBanner = pinned.isNotEmpty(),
                        name = kind.title,
                        isNetworkFetchComplete = true,
                        isTyping = state.typing,
                        deletingIds = state.deletingIds,
                        listState = listState,
                        selectedMessages = state.selected,
                        isSelectionMode = isSelectionMode,
                        onToggleSelect = { id -> state.selected = if (id in state.selected) state.selected - id else state.selected + id },
                        highlightedMessageId = state.highlightedId,
                        onHighlightMessage = { state.highlightedId = it },
                        onReactionSelected = { m, r -> state.react(m, r) },
                        onMoreClick = {},
                        onLongClick = { m, bounds, heavy ->
                            if (!isSelectionMode) {
                                contextMsg = m
                                contextFull = heavy
                                // نفس سلوك الأصل: أعلى الرسالة يصل لثلث الشاشة العلوي لتظهر كاملة فوق القائمة
                                val targetTopPx = with(density) { configuration.screenHeightDp.dp.toPx() } * 0.32f
                                if (bounds.top > targetTopPx) {
                                    scope.launch { listState.animateScrollBy(bounds.top - targetTopPx) }
                                }
                            }
                        },
                        onReply = { if (!isSelectionMode) state.replyingTo = it },
                        modifier = Modifier.fillMaxSize(),
                        onInlineButtonClick = { _, b ->
                            if (b.url != null) uriHandler.openUri(b.url)
                            else {
                                state.messages += labMsg(kind, b.text, true)
                                state.simulateReply(scope, if (b.callbackData == "menu") "/menu" else "/help")
                            }
                        }
                    )
                    }
                    Column(modifier = Modifier.align(Alignment.TopCenter)) {
                        if (isSelectionMode) {
                            SelectionTopBar(
                                selectedCount = state.selected.size,
                                onClearSelection = { state.selected = emptySet() },
                                onCopy = {
                                    val txt = snapshot.filter { it.id in state.selected }.joinToString("\n") { it.text }
                                    clipboard.setText(AnnotatedString(txt))
                                    state.selected = emptySet()
                                    state.showToast("Message copied to clipboard", ToastType.COPY)
                                },
                                onDelete = { state.deleteIds = state.selected }
                            )
                        } else {
                            FloatingTopBar(
                                name = kind.title,
                                isChannel = kind.isChannel,
                                subscriberCount = if (kind.isChannel) 12400 else null,
                                isTyping = state.typing,
                                isOnline = kind == LabKind.PRIVATE,
                                statusOverride = when {
                                    kind.isBot -> "بوت"
                                    kind.isGroup -> "٣ أعضاء"
                                    else -> null
                                },
                                onBack = onBack,
                                menuExpanded = menuExpanded,
                                onMenuExpandedChange = { menuExpanded = it }
                            )
                            if (pinned.isNotEmpty()) {
                                val cur = pinned[pinIndex % pinned.size]
                                PinnedMessageBar(
                                    message = cur,
                                    count = pinned.size,
                                    currentIndex = pinIndex % pinned.size,
                                    onUnpin = {
                                        state.update(cur) { it.copy(isPinned = false) }
                                        state.showToast("Message unpinned", ToastType.UNPIN)
                                    },
                                    onClick = {
                                        val idx = uiMessages.indexOfFirst { it.msg.id == cur.id }
                                        if (idx >= 0) scope.launch {
                                            listState.animateScrollToItem(idx)
                                            state.highlightedId = cur.id
                                            delay(1500)
                                            state.highlightedId = null
                                        }
                                        pinIndex++
                                    }
                                )
                            }
                        }
                    }
                }

                Column(modifier = Modifier.navigationBarsPadding()) {
                    if (kind.isBot && !state.botStarted) {
                        BotStartBar(onClick = {
                            state.botStarted = true
                            state.messages += labMsg(kind, "/start", true)
                            state.simulateReply(scope, "/start")
                        })
                    } else {
                        if (kind.isBot && state.showBotMenu) {
                            BotCommandsSheet(commands = state.botCommands, onPick = { cmd ->
                                state.showBotMenu = false
                                state.messages += labMsg(kind, cmd, true)
                                state.simulateReply(scope, cmd)
                            })
                        }
                        MessageInputBar(
                            text = state.text,
                            onTextChange = { state.text = it },
                            onSend = { state.sendText(scope) },
                            closeAttachmentSignal = state.closeAttachSignal,
                            allowPoll = kind.isChannel || kind.isGroup,
                            onPollClick = { state.showPollComposer = true },
                            onAttachmentPanelToggle = { open -> if (open) state.closePanel() },
                            onOpenGifPicker = {
                                if (state.showPanel) state.closePanel()
                                else {
                                    state.closeAttachSignal++   // أغلق لوحة المرفقات أولًا
                                    focusManager.clearFocus()
                                    keyboard?.hide()
                                    state.showPanel = true
                                }
                            },
                            onAttachmentSelected = { uris, type -> state.sendAttachments(context, scope, uris, type) },
                            replyingTo = state.replyingTo,
                            onCancelReply = { state.replyingTo = null },
                            editingMessage = state.editingMessage,
                            onCancelEdit = { state.editingMessage = null; state.text = "" },
                            leading = if (kind.isBot || state.showPanel) ({
                                // سهم رفع/خفض لوحة الإيموجي بجانب علامة +
                                if (state.showPanel) {
                                    PanelExpandChevron(expanded = state.panelExpanded) {
                                        state.panelExpanded = !state.panelExpanded
                                        if (!state.panelExpanded) {
                                            focusManager.clearFocus()
                                            keyboard?.hide()
                                        }
                                    }
                                }
                                if (kind.isBot) {
                                    BotMenuPill(open = state.showBotMenu, onClick = { state.showBotMenu = !state.showBotMenu })
                                }
                            }) else null,
                            trailing = if (kind.isBot && replyKeyboardRows != null) ({
                                ReplyKeyboardToggle(active = state.showReplyKb, onClick = { state.showReplyKb = !state.showReplyKb })
                            }) else null
                        )
                        // فتح/إغلاق انسحابي: تنزلق اللوحة من الأسفل مع توسّع ناعم
                        androidx.compose.animation.AnimatedVisibility(
                            visible = state.showPanel,
                            enter = androidx.compose.animation.expandVertically(
                                animationSpec = androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                                expandFrom = Alignment.Top
                            ) + androidx.compose.animation.slideInVertically(
                                animationSpec = androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                            ) { it / 2 } + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220)),
                            exit = androidx.compose.animation.shrinkVertically(
                                animationSpec = androidx.compose.animation.core.tween(250, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                                shrinkTowards = Alignment.Top
                            ) + androidx.compose.animation.slideOutVertically(
                                animationSpec = androidx.compose.animation.core.tween(250, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                            ) { it / 2 } + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(160))
                        ) {
                            LabEmojiGifPanel(
                                heightProvider = { panelHeightState.value },
                                recentEmojis = state.recentEmojis,
                                onEmoji = { state.addEmoji(it) },
                                onBackspace = { state.backspace() },
                                onGif = { state.sendGif(it, scope) },
                                showTabBar = !state.panelSearchActive,
                                onSearchFocus = { state.panelSearchActive = it },
                                onSwipeUp = { state.panelExpanded = true },
                                onSwipeDown = { if (state.panelExpanded) state.panelExpanded = false else state.closePanel() }
                            )
                        }
                        if (kind.isBot && replyKeyboardRows != null && state.showReplyKb) {
                            ReplyKeyboardGrid(rows = replyKeyboardRows, onKey = { k ->
                                state.messages += labMsg(kind, k, true)
                                state.simulateReply(scope, k)
                            })
                        }
                    }
                }
            }
        }
    }

    // ===== شاشة إنشاء الاستفتاء (قنوات ومجموعات) =====
    androidx.compose.animation.AnimatedVisibility(
        visible = state.showPollComposer,
        enter = androidx.compose.animation.slideInVertically(androidx.compose.animation.core.tween(320, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { it } +
            androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220)),
        exit = androidx.compose.animation.slideOutVertically(androidx.compose.animation.core.tween(280, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { it } +
            androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(180))
    ) {
        LabPollComposer(
            isChannel = kind.isChannel,
            onDismiss = { state.showPollComposer = false },
            onCreate = { poll ->
                state.showPollComposer = false
                state.sendPoll(poll, scope)
            }
        )
    }

    // ===== القائمة الأصلية عند الضغط المطوّل =====
    contextMsg?.let { m ->
        MessageContextMenuOverlay(
            message = m,
            showFullMenu = contextFull,
            isChannel = kind.isChannel,
            onDismissRequest = { contextMsg = null },
            onReactionSelected = { r -> state.react(m, r); contextMsg = null },
            onReply = { state.replyingTo = m },
            onEdit = { state.editingMessage = m; state.text = m.text },
            onCopy = {
                clipboard.setText(AnnotatedString(m.text))
                state.showToast("Message copied to clipboard", ToastType.COPY)
            },
            onDelete = { state.deleteIds = setOf(m.id) },
            onSelect = { state.selected = setOf(m.id) },
            onForward = { state.forwardMsg = m },
            onPin = {
                if (!m.isPinned) state.pinDialog = m
                else {
                    state.update(m) { it.copy(isPinned = false) }
                    state.showToast("Message unpinned", ToastType.UNPIN)
                }
            },
            onSave = {
                val now = !m.isSaved
                state.update(m) { it.copy(isSaved = now) }
                state.showToast(if (now) "Message saved" else "Message unsaved", if (now) ToastType.SAVE else ToastType.UNSAVE)
            },
            onMoreClick = { contextMsg = null }
        )
    }

    // ===== حوار الحذف الأصلي =====
    state.deleteIds?.let { ids ->
        val delMsgs = snapshot.filter { it.id in ids }
        val allMine = delMsgs.isNotEmpty() && delMsgs.all { it.isMine }
        AppConfirmDialog(
            title = if (ids.size == 1) extra.msgDeleteTitle else String.format(extra.msgDeleteTitleFmt, ids.size),
            message = if (ids.size == 1) extra.msgDeleteBody else String.format(extra.msgDeleteBodyFmt, ids.size),
            checkboxLabel = if (allMine) (if (kind.isChannel || kind.isGroup) extra.msgAlsoDeleteEveryone else String.format(extra.msgAlsoDeleteForFmt, kind.title)) else null,
            confirmText = extra.dlgDelete,
            cancelText = extra.dlgCancel,
            onDismiss = { state.deleteIds = null },
            onConfirm = { _ ->
                state.deleteWithEffect(ids, scope)
                state.deleteIds = null
                state.selected = emptySet()
                state.showToast(extra.msgDeletedToast, ToastType.COPY)
            }
        )
    }

    // ===== حوار التثبيت الأصلي =====
    state.pinDialog?.let { pm ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { state.pinDialog = null },
            containerColor = SettingsColors.surface,
            title = { Text(extra.pinMessageTitle, color = SettingsColors.textPrimary) },
            text = { Text(extra.pinMessageTitle, color = SettingsColors.textSecondary) },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    androidx.compose.material3.TextButton(onClick = {
                        state.pinDialog = null
                        state.update(pm) { it.copy(isPinned = true) }
                        state.showToast("Message pinned", ToastType.PIN)
                    }) { Text(extra.pinForMeOnly, color = SettingsColors.blueAccent) }
                    androidx.compose.material3.TextButton(onClick = {
                        state.pinDialog = null
                        state.update(pm) { it.copy(isPinned = true) }
                        state.showToast("Message pinned", ToastType.PIN)
                    }) { Text(String.format(extra.pinForBothFmt, kind.title), color = SettingsColors.blueAccent) }
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { state.pinDialog = null }) {
                    Text(LocalTranslation.current.cancel, color = SettingsColors.textSecondary)
                }
            }
        )
    }

    // ===== إعادة التوجيه: إلى الشاشات الأخرى فعليًا =====
    state.forwardMsg?.let { fm ->
        LabForwardDialog(
            primaryColor = MaterialTheme.colorScheme.primary,
            targets = forwardTargets.map { it.kind.title },
            onPick = { i ->
                val target = forwardTargets[i]
                target.receiveForward(fm.text, fm.attachments)
                state.forwardMsg = null
                state.showToast("Forwarded to ${target.kind.title}", ToastType.INFO)
            },
            onDismiss = { state.forwardMsg = null }
        )
    }

    LabToastHost(toastNotification = state.toast, onClear = { state.toast = null })
}
