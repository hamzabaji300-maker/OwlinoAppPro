package com.example.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
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
    replyTo: MessageModel? = null
) = MessageModel(
    id = id, chatId = kind.name, senderId = if (mine) "me" else sender.ifBlank { "other" },
    senderName = sender, text = text, time = nowLabel(), isMine = mine,
    replyToId = replyTo?.id, replyTo = replyTo,
    attachments = attachments, replyMarkup = markup, viewsLabel = views,
    createdAtExact = java.time.Instant.now().toString(),
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
fun LabMessagesScreen(state: LabChatState, onBack: () -> Unit = {}) {
    val kind = state.kind
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    var menuExpanded by remember { mutableStateOf(false) }
    var contextMsg by remember { mutableStateOf<MessageModel?>(null) }

    val snapshot = state.messages.toList()
    val uiMessages = remember(snapshot) { buildUi(snapshot, kind.isGroup) }
    val replyKeyboardRows = remember(snapshot) { currentReplyKeyboard(snapshot) }

    Box(modifier = Modifier.fillMaxSize()) {
        ChatWallpaper()
        Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)).imePadding()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    ChatMessages(
                        messages = uiMessages,
                        isChannel = kind.isChannel,
                        isGroup = kind.isGroup,
                        name = kind.title,
                        isNetworkFetchComplete = true,
                        isTyping = state.typing,
                        onReactionSelected = { m, r ->
                            val i = state.messages.indexOfFirst { it.id == m.id }
                            if (i >= 0) {
                                val cur = state.messages[i]
                                state.messages[i] = cur.copy(reactions = if (r in cur.reactions) cur.reactions - r else cur.reactions + r)
                            }
                        },
                        onMoreClick = {},
                        onLongClick = { m, _, _ -> contextMsg = m },
                        onReply = { state.replyingTo = it },
                        modifier = Modifier.fillMaxSize(),
                        onInlineButtonClick = { _, b ->
                            if (b.url != null) uriHandler.openUri(b.url)
                            else {
                                state.messages += labMsg(kind, b.text, true)
                                state.simulateReply(scope, if (b.callbackData == "menu") "/menu" else "/help")
                            }
                        }
                    )
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
                            onAttachmentSelected = { uris, type -> state.sendAttachments(context, scope, uris, type) },
                            replyingTo = state.replyingTo,
                            onCancelReply = { state.replyingTo = null },
                            editingMessage = null,
                            onCancelEdit = {},
                            leading = if (kind.isBot) ({
                                BotMenuPill(open = state.showBotMenu, onClick = { state.showBotMenu = !state.showBotMenu })
                            }) else null,
                            trailing = if (kind.isBot && replyKeyboardRows != null) ({
                                ReplyKeyboardToggle(active = state.showReplyKb, onClick = { state.showReplyKb = !state.showReplyKb })
                            }) else null
                        )
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

    contextMsg?.let { m ->
        AlertDialog(
            onDismissRequest = { contextMsg = null },
            confirmButton = {
                Column {
                    TextButton(onClick = { state.replyingTo = m; contextMsg = null }) { Text("رد") }
                    if (m.text.isNotBlank()) TextButton(onClick = { clipboard.setText(AnnotatedString(m.text)); contextMsg = null }) { Text("نسخ") }
                    TextButton(onClick = { state.messages.removeAll { it.id == m.id }; contextMsg = null }) { Text("حذف", color = Color.Red) }
                }
            }
        )
    }
}
