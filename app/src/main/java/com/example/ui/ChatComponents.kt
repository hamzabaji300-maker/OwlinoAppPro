package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.platform.LocalConfiguration
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import com.example.emoji.NotoEmojiMap
import com.example.emoji.EmojiMessageUtils





@Composable
fun MessageReactionBar(onReact: (String) -> Unit) {
        val emojis = listOf("⭐", "👍", "❤️", "😂", "😮", "😢", "🔥")
    Row(
        modifier = Modifier
            .background(Color.White, RoundedCornerShape(24.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .shadow(4.dp, RoundedCornerShape(24.dp)),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        emojis.forEach { emoji ->
            Text(
                text = emoji,
                fontSize = 24.sp,
                modifier = Modifier
                    .clickable { onReact(emoji) }
                    .padding(4.dp)
            )
        }
    }
}

@Composable
fun MessageMenu(msg: MessageModel, onAction: (String) -> Unit) {
    val actions = listOf(
        "reply" to "Reply",
        "copy" to "Copy",
        "pin" to (if (msg.isPinned) "Unpin" else "Pin"),
        "save" to (if (msg.isSaved) "Unsave" else "Save"),
        "delete" to "Delete"
    )
    Column(
        modifier = Modifier
            .width(180.dp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .shadow(12.dp, RoundedCornerShape(12.dp))
            .padding(vertical = 4.dp)
    ) {
        actions.forEach { (id, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAction(id) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (id == "delete") Color.Red else Color.Black)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MessageBubble(
    msg: MessageModel,
    showSender: Boolean = false,
    onImageClick: ((String) -> Unit)? = null,
    isFirstInCluster: Boolean,
    isLastInCluster: Boolean,
    onReplyClick: (MessageModel) -> Unit,
    onDoubleTap: (MessageModel) -> Unit,
    onLightLongPress: (MessageModel, Offset) -> Unit,
    onHeavyLongPress: (MessageModel, Offset) -> Unit,
    onCancelUpload: ((MessageModel) -> Unit)? = null
) {
    val __themeConfig = com.example.ui.LocalSettingsTheme.current
    val __theme = __themeConfig.theme
    val isMe = msg.isMine
    val bubbleColor = if (isMe) __themeConfig.accent else if (__theme.isDark) Color(0xFF262626) else Color.White
    val textColor = if (isMe) Color.White else if (__theme.isDark) Color.White else Color.Black
    val timeColor = textColor.copy(alpha = 0.6f)

    // Forme « Telegram » : coins 18dp, coin voisin 5dp dans un groupe, petite queue sur le dernier message.
    val shape = RoundedCornerShape(
        topStart = if (!isMe && !isFirstInCluster) 5.dp else 18.dp,
        topEnd = if (isMe && !isFirstInCluster) 5.dp else 18.dp,
        bottomStart = if (!isMe) (if (isLastInCluster) 0.dp else 5.dp) else 18.dp,
        bottomEnd = if (isMe) (if (isLastInCluster) 0.dp else 5.dp) else 18.dp
    )
    val __isRtl = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
    val __tailOnRight = if (isMe) !__isRtl else __isRtl

    var touchOffset by remember { mutableStateOf(Offset.Zero) }
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    val emojiOnlyImageAttachments = msg.attachments.filter { it.type == AttachmentType.IMAGE }
    val emojiOnlyAudioAtt = msg.attachments.firstOrNull { it.type == AttachmentType.AUDIO || it.type == AttachmentType.VOICE }
    val documentAtt = msg.attachments.firstOrNull { it.type == AttachmentType.DOCUMENT }
    val emojiOnlyCleanText = msg.text.replace("[Photo]", "").replace("[Album]", "")
        .replace("[Document]", "").replace("[Documents]", "").trim()
    val emojiOnlySequence = remember(emojiOnlyCleanText) { EmojiMessageUtils.parseSupportedEmojiSequence(emojiOnlyCleanText) }
    val isEmojiOnlyMessage = emojiOnlySequence != null && emojiOnlyImageAttachments.isEmpty() && emojiOnlyAudioAtt == null && documentAtt == null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = if (isLastInCluster) 2.dp else 0.5.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (isMe && msg.reactions.isNotEmpty()) {
            Row(
                modifier = Modifier.offset(y = (-8).dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                msg.reactions.forEach { emoji ->
                    val reactionUrl = NotoEmojiMap.remoteUrlFor(NotoEmojiMap.reactionToEmoji(emoji))
                    if (reactionUrl != null) {
                        LottieEmojiReaction(url = reactionUrl, size = 34.dp)
                    }
                }
            }
            Spacer(Modifier.width(3.dp))
        }
        val __senderSlot = showSender && !isMe
        val __nameAbove = __senderSlot && isFirstInCluster && !isEmojiOnlyMessage && msg.senderName.isNotBlank()
        if (__senderSlot) {
            // صورة المرسل على اليسار (تظهر عند آخر رسالة في العنقود، وبقية الرسائل تحافظ على نفس المحاذاة)
            Box(modifier = Modifier.size(30.dp)) {
                if (isLastInCluster) {
                    if (!msg.senderAvatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = msg.senderAvatarUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        val __ini = msg.senderName.trim().take(1).uppercase().ifEmpty { "?" }
                        Box(
                            modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color(0xFF8E8E93)),
                            contentAlignment = Alignment.Center
                        ) { Text(__ini, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
        }
        Column(
            modifier = Modifier
                .widthIn(min = 60.dp, max = (if (msg.reactions.isNotEmpty()) screenWidth * 0.74f else screenWidth * 0.85f) - (if (__senderSlot) 38.dp else 0.dp))
                .let { m ->
                    if (isEmojiOnlyMessage || !isLastInCluster) m else m.drawBehind {
                        val d = 1.dp.toPx()
                        val w = size.width
                        val h = size.height
                        val tail = androidx.compose.ui.graphics.Path()
                        if (__tailOnRight) {
                            tail.moveTo(w - 6f * d, h - 12f * d)
                            tail.lineTo(w, h - 12f * d)
                            tail.quadraticBezierTo(w + 0.5f * d, h - 1f * d, w + 6f * d, h)
                            tail.lineTo(w - 6f * d, h)
                        } else {
                            tail.moveTo(6f * d, h - 12f * d)
                            tail.lineTo(0f, h - 12f * d)
                            tail.quadraticBezierTo(-0.5f * d, h - 1f * d, -6f * d, h)
                            tail.lineTo(6f * d, h)
                        }
                        tail.close()
                        drawPath(tail, bubbleColor)
                    }
                }
                .let { if (isEmojiOnlyMessage) it else it.clip(shape).background(bubbleColor) }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { onDoubleTap(msg) },
                        onPress = { offset ->
                            touchOffset = offset
                            val releaseTimeout = kotlinx.coroutines.withTimeoutOrNull(250) {
                                tryAwaitRelease()
                            }
                            if (releaseTimeout == null) {
                                // 250ms passed, not released
                                onLightLongPress(msg, offset)
                                val releaseTimeout2 = kotlinx.coroutines.withTimeoutOrNull(250) {
                                    tryAwaitRelease()
                                }
                                if (releaseTimeout2 == null) {
                                    // 500ms total passed
                                    onHeavyLongPress(msg, offset)
                                    tryAwaitRelease() // wait for actual release
                                }
                            }
                        }
                    )
                }
        ) {
            if (__nameAbove) {
                val __nameColors = listOf(Color(0xFFE17076), Color(0xFFEDA86C), Color(0xFFA695E7), Color(0xFF7BC862), Color(0xFF6EC9CB), Color(0xFF65AADD), Color(0xFFEE7AAE))
                val __nameColor = __nameColors[(msg.senderId.hashCode() and 0x7fffffff) % __nameColors.size]
                Text(
                    text = msg.senderName,
                    color = __nameColor,
                    fontSize = 13.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 0.dp)
                )
            }
            if (msg.replyTo != null) {
                Row(
                    modifier = Modifier
                        .padding(start = 4.dp, top = 4.dp, end = 4.dp, bottom = 4.dp)
                        .height(IntrinsicSize.Min)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isMe) Color(0xFFFDE9B4) else Color(0xFFF2F2F2))
                        .clickable { onReplyClick(msg.replyTo) }
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .background(if (isMe) Color(0xFFC78B22) else Color(0xFF3390EC))
                    )
                    Column(
                        modifier = Modifier
                            .padding(start = 6.dp, top = 2.dp, end = 6.dp, bottom = 2.dp)
                    ) {
                        Text(
                            text = if (msg.replyTo.isMine) "You" else msg.replyTo.senderName.takeIf { it.isNotBlank() } ?: "User",
                            color = if (isMe) Color(0xFFC78B22) else Color(0xFF3390EC),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = msg.replyTo.text,
                            color = Color.Black,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = androidx.compose.ui.text.TextStyle(textDirection = androidx.compose.ui.text.style.TextDirection.Content)
                        )
                    }
                }
            }

            val imageAttachments = emojiOnlyImageAttachments
            val audioAtt = emojiOnlyAudioAtt
            val cleanText = emojiOnlyCleanText

            if (msg.poll != null) {
                PollBubbleContent(msg = msg, isMe = isMe, textColor = textColor, accent = __themeConfig.accent)
            } else if (isEmojiOnlyMessage) {
                val units = emojiOnlySequence!!
                Column(modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp)) {
                    // يتحرك مرة واحدة عند الإرسال فقط، وبعدها ثابت. الضغط عليه يحرّكه لثوانٍ ثم يتوقف
                    val autoPlayEmoji = remember(msg.timestamp, msg.text) {
                        EmojiPlayRegistry.shouldAutoPlay(msg.timestamp, msg.text)
                    }
                    if (units.size == 1) {
                        // إيموجي وحد -> كبير (100dp)
                        val path = NotoEmojiMap.remoteUrlFor(units[0])
                        if (path != null) {
                            LottieEmojiReaction(url = path, size = 100.dp, tapToPlay = true, autoPlayOnce = autoPlayEmoji)
                        }
                    } else {
                        // حتى 5 إيموجيات -> متحركة. أكثر من 5 -> ثابتة (من نفس الرابط) بلا فقاعة
                        val animated = units.size <= 5
                        val perRow = if (units.size <= 12) 6 else 8
                        val emojiSize = if (units.size <= 12) 40.dp else 32.dp
                        units.chunked(perRow).forEach { rowUnits ->
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                rowUnits.forEach { e ->
                                    val path = NotoEmojiMap.remoteUrlFor(e)
                                    if (path != null) {
                                        LottieEmojiReaction(
                                            url = path,
                                            size = emojiSize,
                                            animate = false,
                                            tapToPlay = animated,
                                            autoPlayOnce = animated && autoPlayEmoji
                                        )
                                    }
                                }
                            }
                        }
                    }
                    // الوقت + علامة القراءة داخل كارد رقيق وشفاف تحت الإيموجي (زي تيليجرام)
                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 2.dp, end = 2.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.18f))
                            .padding(start = 5.dp, end = 4.dp, top = 0.dp, bottom = 0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(msg.time, fontSize = 9.sp, lineHeight = 11.sp, fontWeight = FontWeight.Normal, color = Color.White)
                        MessageIndicators(
                            isMe = isMe,
                            status = msg.status,
                            isRead = false,
                            isSaved = msg.isSaved,
                            isPinned = msg.isPinned,
                            isImageOverlay = true,
                            compact = true
                        )
                    }
                }
            } else if (imageAttachments.isNotEmpty()) {
                Box(modifier = Modifier.padding(2.dp).clip(RoundedCornerShape(14.dp))) {
                    val count = imageAttachments.size
                    val spacing = 2.dp
                    // مقاسات مصغّرة زي تيليجرام: عرض أقصى 66% من الشاشة وارتفاع أقصى 40% منها
                    val collageW = screenWidth * 0.66f
                    
                    if (count == 1) {
                        val att = imageAttachments.first()
                        val singleRatio = (att.aspectRatio ?: 1f).coerceIn(0.4f, 2.5f)
                        val singleMaxH = screenHeight * 0.40f
                        var singleW = collageW
                        var singleH = singleW / singleRatio
                        if (singleH > singleMaxH) {
                            singleH = singleMaxH
                            singleW = singleH * singleRatio
                        }
                        singleW = singleW.coerceAtLeast(120.dp)
                        DownloadableImage(
                            att = att,
                            isMe = isMe,
                            modifier = Modifier.size(width = singleW, height = singleH),
                            onOpen = { onImageClick?.invoke(att.messageId) }
                        )
                    } else {
                        // معرض بالنسبة الأصلية لكل صورة (بلا تربيع/قص كبير): كل صف يتقاسم نفس الارتفاع
                        // والعرض يتوزع حسب نسبة كل صورة، فتظهر الصور كاملة تقريباً.
                        val shown = imageAttachments.take(4)
                        val rows: List<List<Attachment>> = when (shown.size) {
                            2 -> listOf(shown)
                            3 -> listOf(listOf(shown[0]), listOf(shown[1], shown[2]))
                            else -> listOf(listOf(shown[0], shown[1]), listOf(shown[2], shown[3]))
                        }
                        Column(modifier = Modifier.width(collageW), verticalArrangement = Arrangement.spacedBy(spacing)) {
                            rows.forEach { rowItems ->
                                var sumRatio = 0f
                                rowItems.forEach { sumRatio += (it.aspectRatio ?: 1f).coerceIn(0.5f, 2.0f) }
                                val maxRowH = if (rowItems.size == 1) screenHeight * 0.30f else screenHeight * 0.22f
                                val rowH = ((collageW - spacing * (rowItems.size - 1)) / sumRatio).coerceAtMost(maxRowH)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                    rowItems.forEach { att ->
                                        val r = (att.aspectRatio ?: 1f).coerceIn(0.5f, 2.0f)
                                        val isLastTile = count > 4 && att.messageId == shown.last().messageId
                                        MediaTile(
                                            att = att,
                                            isMe = isMe,
                                            modifier = Modifier.weight(r).height(rowH),
                                            extraCount = if (isLastTile) count - 4 else 0,
                                            onClick = { onImageClick?.invoke(att.messageId) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    if (cleanText.isBlank()) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.Black.copy(alpha = 0.28f))
                                .padding(start = 5.dp, end = 4.dp, top = 0.dp, bottom = 0.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(msg.time, fontSize = 9.sp, lineHeight = 11.sp, fontWeight = FontWeight.Normal, color = Color.White)
                            MessageIndicators(
                                isMe = isMe,
                                status = msg.status,
                                isRead = false,
                                isSaved = msg.isSaved,
                                isPinned = msg.isPinned,
                                isImageOverlay = true,
                                compact = true
                            )
                        }
                    }
                }
            } else if (audioAtt != null) {
                val audioCtx = androidx.compose.ui.platform.LocalContext.current
                val audioFiles by MediaIndex.files.collectAsState()
                val audioState by AudioPlayerManager.state.collectAsState()
                val isThis = audioState.messageId == audioAtt.messageId
                val playing = isThis && audioState.isPlaying
                val preparing = isThis && audioState.isPreparing
                LaunchedEffect(playing) {
                    while (playing) {
                        AudioPlayerManager.refreshPosition()
                        kotlinx.coroutines.delay(120)
                    }
                }
                val totalMs = if (isThis && audioState.durationMs > 0) audioState.durationMs.toLong() else (audioAtt.durationMs ?: 0L)
                val posMs = if (isThis) audioState.positionMs.toLong() else 0L
                val progress = if (totalMs > 0) (posMs.toFloat() / totalMs).coerceIn(0f, 1f) else 0f
                fun fmt(ms: Long): String { val s = (ms / 1000).toInt(); return "${s / 60}:${(s % 60).toString().padStart(2, '0')}" }
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).width(200.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isMe) Color.White else Color(0xFF007AFF))
                            .clickable {
                                val src = audioFiles[audioAtt.messageId] ?: audioAtt.url
                                AudioPlayerManager.toggle(audioCtx, audioAtt.messageId, src)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (preparing) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = if (isMe) Color(0xFF4FA953) else Color.White)
                        } else {
                            Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = if (isMe) Color(0xFF4FA953) else Color.White, modifier = Modifier.size(24.dp))
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(modifier = Modifier.fillMaxWidth().height(20.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            val h = listOf(2, 4, 3, 5, 8, 4, 2, 5, 7, 4, 3, 2, 4, 6)
                            h.forEachIndexed { i, v ->
                                val active = (i + 1).toFloat() / h.size <= progress
                                val base = if (isMe) Color.White else Color(0xFF007AFF)
                                Box(modifier = Modifier.weight(1f).height((v*2).dp).clip(CircleShape).background(if (active) base else base.copy(if (isMe) 0.6f else 0.4f)))
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(top=2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(if (isThis && (playing || posMs > 0)) fmt(posMs) else fmt(totalMs), fontSize = 11.sp, color = timeColor)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(msg.time, fontSize = 10.sp, color = timeColor)
                                MessageIndicators(
                                    isMe = isMe,
                                    status = msg.status,
                                    isRead = false,
                                    isSaved = msg.isSaved,
                                    isPinned = msg.isPinned,
                                    isImageOverlay = false
                                )
                            }
                        }
                    }
                }
            } else if (msg.attachments.any { it.type == AttachmentType.VIDEO }) {
                VideoBubbleContent(
                    att = msg.attachments.first { it.type == AttachmentType.VIDEO },
                    isMe = isMe,
                    msg = msg,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight
                )
            } else if (documentAtt != null) {
                FileBubbleContent(
                    att = documentAtt,
                    caption = cleanText,
                    isMe = isMe,
                    textColor = textColor,
                    timeColor = timeColor,
                    msg = msg,
                    onCancelUpload = onCancelUpload?.let { cb -> { cb(msg) } }
                )
            } else if (cleanText.isNotBlank()) {
                val detectedUrl = remember(cleanText) { extractFirstUrl(cleanText) }
                var isSingleLine by remember(cleanText) { mutableStateOf(detectedUrl == null) }
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr
                ) {
                    if (isSingleLine) {
                        val richSingle = remember(cleanText) { buildRichMessageContent(cleanText, 19.sp, 20.dp) }
                        FlowRow(
                            modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = if (__nameAbove && msg.replyTo == null) 0.dp else 6.dp, bottom = 6.dp)
                        ) {
                            Text(
                                text = richSingle.text,
                                inlineContent = richSingle.inlineContent,
                                color = textColor,
                                fontSize = 16.5.sp,
                                lineHeight = 24.sp,
                                maxLines = 1,
                                style = androidx.compose.ui.text.TextStyle(textDirection = androidx.compose.ui.text.style.TextDirection.Content),
                                onTextLayout = { result ->
                                    if (result.lineCount > 1 || result.hasVisualOverflow) {
                                        isSingleLine = false
                                    }
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Row(
                                modifier = Modifier.align(Alignment.Bottom),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(msg.time, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = timeColor)
                                MessageIndicators(
                                    isMe = isMe,
                                    status = msg.status,
                                    isRead = false,
                                    isSaved = msg.isSaved,
                                    isPinned = msg.isPinned,
                                    isImageOverlay = false
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = if (__nameAbove && msg.replyTo == null) 0.dp else 6.dp, bottom = 5.dp)
                        ) {
                            if (detectedUrl != null) {
                                LinkPreviewCard(url = detectedUrl, isMe = isMe, textColor = textColor)
                            }
                            val richMulti = remember(cleanText) { buildRichMessageContent(cleanText, 19.sp, 20.dp) }
                            Text(
                                text = richMulti.text,
                                inlineContent = richMulti.inlineContent,
                                color = textColor,
                                fontSize = 16.5.sp,
                                lineHeight = 24.sp,
                                style = androidx.compose.ui.text.TextStyle(textDirection = androidx.compose.ui.text.style.TextDirection.Content)
                            )
                            Row(
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .padding(top = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(msg.time, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = timeColor)
                                MessageIndicators(
                                    isMe = isMe,
                                    status = msg.status,
                                    isRead = false,
                                    isSaved = msg.isSaved,
                                    isPinned = msg.isPinned,
                                    isImageOverlay = false
                                )
                            }
                        }
                    }
                }
            }
        }

        if (!isMe && msg.reactions.isNotEmpty()) {
            Spacer(Modifier.width(3.dp))
            Row(
                modifier = Modifier.offset(y = (-8).dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                msg.reactions.forEach { emoji ->
                    val reactionUrl = NotoEmojiMap.remoteUrlFor(NotoEmojiMap.reactionToEmoji(emoji))
                    if (reactionUrl != null) {
                        LottieEmojiReaction(url = reactionUrl, size = 34.dp)
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------
// Channel "post" card — used instead of MessageBubble when the chat is a broadcast
// channel. Full-width card (not a left/right bubble), with a footer that shows the
// view count, forward count, and per-emoji reaction pills like Telegram channels.
// ------------------------------------------------------------------------------------
@Composable
fun ChannelPostCard(
    msg: MessageModel,
    channelName: String,
    onImageClick: ((String) -> Unit)? = null,
    onLightLongPress: (MessageModel, Offset) -> Unit = { _, _ -> },
    onHeavyLongPress: (MessageModel, Offset) -> Unit = { _, _ -> }
) {
    val __theme = com.example.ui.LocalSettingsTheme.current.theme
    val textColor = __theme.textPrimary
    val subColor = __theme.textSecondary
    var showBreakdown by remember(msg.id) { mutableStateOf(false) }

    // المنشور يُرسم بنفس مكوّن الفقاعة (نص/صور/فيديو/ملفات/صوت) فتظهر كل أنواع المحتوى،
    // ويكون بحجم المحتوى (فقاعة صغيرة) مع الوقت والمشاهدات في نفس السطر الضيق بجانب النص.
    val bubbleMsg = msg.copy(
        isMine = false,
        reactions = emptyList(),
        time = if (!msg.viewsLabel.isNullOrBlank()) "\uD83D\uDC41 ${msg.viewsLabel}  ${msg.time}" else msg.time
    )

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        MessageBubble(
            msg = bubbleMsg,
            showSender = false,
            onImageClick = onImageClick,
            isFirstInCluster = true,
            isLastInCluster = false,
            onReplyClick = { },
            onDoubleTap = { },
            onLightLongPress = onLightLongPress,
            onHeavyLongPress = onHeavyLongPress
        )

        // صف التفاعلات: شريحة forward + شريحة تفاعلات (أعلى 4 + مجموع كلي) + شريحة تفاعلي الشخصي
        if (!msg.forwardsLabel.isNullOrBlank() || msg.channelReactions.isNotEmpty() || msg.reactions.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .padding(start = 12.dp, top = 4.dp, end = 12.dp)
                    .horizontalScroll(rememberScrollState())
            ) {
                if (!msg.forwardsLabel.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(subColor.copy(alpha = 0.08f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(msg.forwardsLabel, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Filled.ArrowForward, contentDescription = "Forwards", tint = subColor, modifier = Modifier.size(14.dp))
                    }
                }
                if (msg.channelReactions.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(subColor.copy(alpha = 0.08f))
                            .clickable { showBreakdown = true }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        val topFour = msg.channelReactions.sortedByDescending { it.count }.take(4)
                        topFour.forEach { reaction ->
                            val url = remember(reaction.emoji) { NotoEmojiMap.remoteUrlFor(reaction.emoji) }
                            if (url != null) {
                                LottieEmojiReaction(url = url, size = 17.dp)
                            } else {
                                Text(reaction.emoji, fontSize = 13.sp)
                            }
                        }
                        val totalLabel = msg.totalInteractionsLabel
                            ?: formatReactionCount(msg.channelReactions.sumOf { it.count })
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(totalLabel, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
                if (msg.reactions.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(subColor.copy(alpha = 0.14f))
                            .border(1.dp, subColor.copy(alpha = 0.3f), RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        msg.reactions.forEach { emoji ->
                            val myUrl = remember(emoji) { NotoEmojiMap.remoteUrlFor(NotoEmojiMap.reactionToEmoji(emoji)) }
                            if (myUrl != null) {
                                LottieEmojiReaction(url = myUrl, size = 16.dp)
                            }
                        }
                    }
                }
            }
        }
    }
    if (showBreakdown) {
        ReactionBreakdownDialog(
            totalLabel = msg.totalInteractionsLabel,
            reactions = msg.channelReactions,
            onDismiss = { showBreakdown = false }
        )
    }
}

@Composable
fun ReactionBreakdownDialog(
    totalLabel: String?,
    reactions: List<ChannelReaction>,
    onDismiss: () -> Unit
) {
    val __theme = com.example.ui.LocalSettingsTheme.current.theme
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(__theme.surfaceColor)
                .padding(16.dp)
        ) {
            if (!totalLabel.isNullOrBlank()) {
                Text("$totalLabel تفاعلاً", color = __theme.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
            }
            val rows = reactions.chunked(3)
            rows.forEach { rowItems ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    rowItems.forEach { reaction ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .background(__theme.textSecondary.copy(alpha = 0.08f))
                                .padding(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            if (reaction.count > 0) {
                                Text(formatReactionCount(reaction.count), color = __theme.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            val url = remember(reaction.emoji) { NotoEmojiMap.remoteUrlFor(reaction.emoji) }
                            if (url != null) {
                                LottieEmojiReaction(url = url, size = 20.dp)
                            } else {
                                Text(reaction.emoji, fontSize = 16.sp)
                            }
                        }
                    }
                    repeat(3 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun MessageInputView(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachmentClick: () -> Unit,
    replyingTo: MessageModel?,
    onCancelReply: () -> Unit,
    chatName: String,
    isBlocked: Boolean = false,
    onUnblock: () -> Unit = {}
) {
    if (isBlocked) {
        Column(
            modifier = Modifier.fillMaxWidth().background(Color(0xFFF0F2F5)).padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(com.example.ui.i18n.LocalTranslation.current.youBlockedThisUser, fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onUnblock) {
                Text(com.example.ui.i18n.LocalTranslation.current.unblockUserAllCaps, color = Color(0xFF007AFF), fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
            }
        }
        return
    }

    var isRecording by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }
    var slideOffsetX by remember { mutableFloatStateOf(0f) }
    var slideOffsetY by remember { mutableFloatStateOf(0f) }
    var recordingDuration by remember { mutableIntStateOf(0) }

    LaunchedEffect(isRecording, isLocked) {
        if (isRecording || isLocked) {
            recordingDuration = 0
            while (true) {
                delay(1000)
                recordingDuration++
            }
        }
    }

    val isTextMode = text.trim().isNotEmpty()

    Column(modifier = Modifier.fillMaxWidth().background(Color.Transparent).padding(horizontal = 8.dp, vertical = 6.dp)) {
        AnimatedVisibility(visible = replyingTo != null) {
            if (replyingTo != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.05f))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.width(3.dp).height(36.dp).background(Color(0xFF007AFF)))
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (replyingTo.isMine) "You" else chatName, color = Color(0xFF007AFF), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(parseBoldMarkdown(replyingTo.text.take(30)), fontSize = 14.sp, color = Color.Gray, maxLines = 1, style = androidx.compose.ui.text.TextStyle(textDirection = androidx.compose.ui.text.style.TextDirection.Content))
                    }
                    IconButton(onClick = onCancelReply, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, "Cancel", tint = Color.Gray)
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(if (isRecording || isLocked) Color(0xFFF5F6F8) else Color.White)
                .border(1.dp, Color.Black.copy(0.05f), RoundedCornerShape(24.dp))
                .padding(end = 4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            if (isRecording || isLocked) {
                Row(
                    modifier = Modifier.weight(1f).height(44.dp).padding(start = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color.Red))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${recordingDuration / 60}:${(recordingDuration % 60).toString().padStart(2, '0')}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.width(16.dp))
                    if (!isLocked) {
                        Text("< Slide to cancel", color = Color.Gray, fontSize = 14.sp)
                    } else {
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { isLocked = false; isRecording = false }) {
                            Icon(Icons.Default.Delete, "Cancel", tint = Color.Red)
                        }
                    }
                }
            } else {
                IconButton(onClick = onAttachmentClick, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Outlined.AttachFile, "Attach", tint = Color.Gray)
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    placeholder = { Text(com.example.ui.i18n.LocalTranslation.current.messageInputPlaceholder, color = Color.Gray) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    maxLines = 5
                )
            }

            Box(
                modifier = Modifier.size(44.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isTextMode || isLocked) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF007AFF))
                            .clickable {
                                if (isTextMode) {
                                    onSend()
                                } else if (isLocked) {
                                    isLocked = false
                                    isRecording = false
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ArrowUpward, "Send", tint = Color.White)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(slideOffsetX.roundToInt(), slideOffsetY.roundToInt()) }
                            .size(if (isRecording && !isLocked) 60.dp else 44.dp)
                            .clip(CircleShape)
                            .background(if (isRecording && !isLocked) Color(0xFF007AFF) else Color.Transparent)
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = {
                                        isRecording = true
                                        slideOffsetX = 0f
                                        slideOffsetY = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        if (isRecording && !isLocked) {
                                            if (slideOffsetX + dragAmount.x < 0) slideOffsetX += dragAmount.x
                                            if (slideOffsetY + dragAmount.y < 0) slideOffsetY += dragAmount.y

                                            if (slideOffsetX < -200) {
                                                isRecording = false
                                                slideOffsetX = 0f
                                                slideOffsetY = 0f
                                            } else if (slideOffsetY < -150) {
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
                                        }
                                    },
                                    onDragCancel = {
                                        if (isRecording && !isLocked) {
                                            isRecording = false
                                            slideOffsetX = 0f
                                            slideOffsetY = 0f
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Mic, "Mic", tint = if (isRecording && !isLocked) Color.White else Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun MessageIndicators(
    isMe: Boolean,
    status: MessageStatus?,
    isRead: Boolean,
    isSaved: Boolean,
    isPinned: Boolean,
    isImageOverlay: Boolean = false,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val currentStatus = if (isRead) MessageStatus.READ else (status ?: MessageStatus.SENT)
    val neutralColor = Color(0xFF8A8A8A)
    val readBlue = Color(0xFF4FA8E8)
    val iconColor = if (isImageOverlay) Color.White else neutralColor
    val readColor = if (compact) Color(0xFF4FC3F7) else if (isImageOverlay) Color.White else readBlue

    Row(modifier = modifier.padding(start = if (compact) 2.dp else 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        if (isSaved) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = "Saved",
                tint = Color(0xFFFFC107),
                modifier = Modifier.size(if (isImageOverlay) 12.dp else 13.dp)
            )
        }
        if (isPinned) {
            Icon(
                imageVector = Icons.Filled.PushPin,
                contentDescription = "Pinned",
                tint = if (isImageOverlay || isMe || LocalSettingsTheme.current.theme.isDark) Color.White else neutralColor,
                modifier = Modifier.size(if (isImageOverlay) 12.dp else 12.dp).graphicsLayer(rotationZ = 45f)
            )
        }
        
        if (isMe) {
            androidx.compose.animation.AnimatedContent(
                targetState = currentStatus,
                transitionSpec = {
                    (androidx.compose.animation.scaleIn(animationSpec = androidx.compose.animation.core.tween(200, delayMillis = 50)) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(200))) togetherWith
                    (androidx.compose.animation.scaleOut(animationSpec = androidx.compose.animation.core.tween(150)) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(150)))
                },
                label = "status_anim"
            ) { state ->
                when (state) {
                    MessageStatus.SENDING -> Icon(Icons.Outlined.Schedule, null, tint = iconColor, modifier = Modifier.size(if (compact) 10.dp else 12.dp))
                    MessageStatus.SENT -> Icon(Icons.Filled.Check, null, tint = iconColor, modifier = Modifier.size(if (compact) 10.dp else 14.dp))
                    MessageStatus.DELIVERED -> Icon(Icons.Filled.DoneAll, null, tint = iconColor, modifier = Modifier.size(if (compact) 11.dp else 15.dp))
                    MessageStatus.READ -> Icon(Icons.Filled.DoneAll, null, tint = readColor, modifier = Modifier.size(if (compact) 11.dp else 15.dp))
                    MessageStatus.FAILED -> Icon(Icons.Outlined.ErrorOutline, null, tint = Color.Red, modifier = Modifier.size(if (compact) 11.dp else 13.dp))
                }
            }
        }
    }
}

fun parseBoldMarkdown(text: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        var currentIndex = 0
        val boldRegex = Regex("\\*\\*(.*?)\\*\\*", setOf(kotlin.text.RegexOption.DOT_MATCHES_ALL, kotlin.text.RegexOption.MULTILINE))
        val matches = boldRegex.findAll(text)
        
        for (match in matches) {
            append(text.substring(currentIndex, match.range.first))
            withStyle(style = SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)) {
                append(match.groupValues[1])
            }
            currentIndex = match.range.last + 1
        }
        
        if (currentIndex < text.length) {
            append(text.substring(currentIndex))
        }
    }
}

// Purple, Telegram-style color used for clickable links inside message text and in preview cards.
val LinkColor = Color(0xFF7C4DFF)

// Telegram-style blue for @mentions (users / bots)
val MentionColor = Color(0xFF3390EC)

/** أفعال النص الغني: نسخ الكود (التوكن مثلا) بضغطة، والضغط على @mention. تُضبط من شاشة الدردشة. */
object RichTextActions {
    var onCopy: ((String) -> Unit)? = null
    var onMention: ((String) -> Unit)? = null
    /** إشعار داخل التطبيق (نفس شكل إشعارات النسخ/التثبيت) بدل Toast النظام. */
    var onNotice: ((String) -> Unit)? = null
    /** Context d'application, utilisé pour ouvrir les liens de façon fiable. */
    var appContext: android.content.Context? = null

    /** Ouvre un lien http(s) dans le navigateur/app associée, sans jamais planter. */
    fun openUrl(rawUrl: String) {
        val ctx = appContext ?: return
        val url = if (rawUrl.contains("://")) rawUrl else "https://$rawUrl"
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
        } catch (e: Exception) {
            onNotice?.invoke("Impossible d'ouvrir le lien")
        }
    }
}

private val codeRegex = Regex("```([\\s\\S]+?)```|`([^`\\n]+)`")
private val mentionRegex = Regex("(?<![\\w@])@[A-Za-z][A-Za-z0-9_]{2,31}")

// Same as parseBoldMarkdown, but also turns any URL found in the text into a colored,
// underlined, tappable link (opens the link's app/browser on click) — like Telegram.
fun parseMessageText(text: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        var currentIndex = 0
        val boldRegex = Regex("\\*\\*(.*?)\\*\\*", setOf(kotlin.text.RegexOption.DOT_MATCHES_ALL, kotlin.text.RegexOption.MULTILINE))
        for (match in boldRegex.findAll(text)) {
            if (match.range.first > currentIndex) {
                appendLinkified(text.substring(currentIndex, match.range.first), null)
            }
            appendLinkified(match.groupValues[1], SpanStyle(fontWeight = FontWeight.Bold))
            currentIndex = match.range.last + 1
        }
        if (currentIndex < text.length) {
            appendLinkified(text.substring(currentIndex), null)
        }
    }
}

private fun androidx.compose.ui.text.AnnotatedString.Builder.appendLinkified(segment: String, boldStyle: SpanStyle?) {
    var idx = 0
    for (m in urlRegex.findAll(segment)) {
        if (m.range.first < idx) continue
        if (m.range.first > idx) {
            val plain = segment.substring(idx, m.range.first)
            if (boldStyle != null) withStyle(boldStyle) { append(plain) } else append(plain)
        }
        var url = m.value
        while (url.isNotEmpty() && url.last() in ".,!?)]}؛،:؟\"'”’") url = url.dropLast(1)
        val linkStyle = SpanStyle(
            color = LinkColor,
            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
            fontWeight = boldStyle?.fontWeight
        )
        withLink(LinkAnnotation.Clickable("url", TextLinkStyles(style = linkStyle, pressedStyle = SpanStyle(background = LinkColor.copy(alpha = 0.22f))), androidx.compose.ui.text.LinkInteractionListener { _ -> RichTextActions.openUrl(url) })) {
            append(url)
        }
        val consumedEnd = m.range.first + url.length
        val leftover = segment.substring(consumedEnd, m.range.last + 1)
        if (leftover.isNotEmpty()) {
            if (boldStyle != null) withStyle(boldStyle) { append(leftover) } else append(leftover)
        }
        idx = m.range.last + 1
    }
    if (idx < segment.length) {
        val rest = segment.substring(idx)
        if (boldStyle != null) withStyle(boldStyle) { append(rest) } else append(rest)
    }
}

// Emoji that appear in the middle of a regular text message (not an emoji-only message)
// used to render as a flat, static glyph from the system font. This makes them animated
// too, like Telegram: each emoji grapheme inside the text becomes an inline placeholder
// that plays the same animated Noto emoji used for emoji-only messages and reactions.
data class RichMessageContent(
    val text: androidx.compose.ui.text.AnnotatedString,
    val inlineContent: Map<String, androidx.compose.foundation.text.InlineTextContent>
)

fun buildRichMessageContent(
    text: String,
    emojiSize: androidx.compose.ui.unit.TextUnit,
    emojiSizeDp: androidx.compose.ui.unit.Dp
): RichMessageContent {
    val inlineMap = mutableMapOf<String, androidx.compose.foundation.text.InlineTextContent>()
    val annotated = buildAnnotatedString {
        var pos = 0
        for (cm in codeRegex.findAll(text)) {
            if (cm.range.first > pos) {
                appendRichBody(text.substring(pos, cm.range.first), inlineMap, emojiSize, emojiSizeDp)
            }
            val code = (cm.groupValues[1].ifEmpty { cm.groupValues[2] }).trim('\n')
            appendCode(code)
            pos = cm.range.last + 1
        }
        if (pos < text.length) {
            appendRichBody(text.substring(pos), inlineMap, emojiSize, emojiSizeDp)
        }
    }
    return RichMessageContent(annotated, inlineMap)
}

private fun androidx.compose.ui.text.AnnotatedString.Builder.appendRichBody(
    text: String,
    inlineMap: MutableMap<String, androidx.compose.foundation.text.InlineTextContent>,
    emojiSize: androidx.compose.ui.unit.TextUnit,
    emojiSizeDp: androidx.compose.ui.unit.Dp
) {
    var currentIndex = 0
    val boldRegex = Regex("\\*\\*(.*?)\\*\\*", setOf(kotlin.text.RegexOption.DOT_MATCHES_ALL, kotlin.text.RegexOption.MULTILINE))
    for (match in boldRegex.findAll(text)) {
        if (match.range.first > currentIndex) {
            appendLinkifiedWithEmoji(text.substring(currentIndex, match.range.first), null, inlineMap, emojiSize, emojiSizeDp)
        }
        appendLinkifiedWithEmoji(match.groupValues[1], SpanStyle(fontWeight = FontWeight.Bold), inlineMap, emojiSize, emojiSizeDp)
        currentIndex = match.range.last + 1
    }
    if (currentIndex < text.length) {
        appendLinkifiedWithEmoji(text.substring(currentIndex), null, inlineMap, emojiSize, emojiSizeDp)
    }
}

/** كود بخط monospace: الضغط عليه ينسخ نص الكود وحده (مثل توكن BotFather). */
private fun androidx.compose.ui.text.AnnotatedString.Builder.appendCode(code: String) {
    val style = SpanStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
        background = Color(0x1F808080)
    )
    withLink(
        LinkAnnotation.Clickable(
            tag = "code",
            styles = TextLinkStyles(style = style, pressedStyle = SpanStyle(background = Color(0x55808080))),
            linkInteractionListener = androidx.compose.ui.text.LinkInteractionListener { _ -> RichTextActions.onCopy?.invoke(code) }
        )
    ) {
        append(code)
    }
}

/** @username يظهر بالأزرق (مستخدم أو بوت). */
private fun androidx.compose.ui.text.AnnotatedString.Builder.appendMentionsWithEmoji(
    text: String,
    boldStyle: SpanStyle?,
    inlineMap: MutableMap<String, androidx.compose.foundation.text.InlineTextContent>,
    emojiSize: androidx.compose.ui.unit.TextUnit,
    emojiSizeDp: androidx.compose.ui.unit.Dp
) {
    var idx = 0
    for (m in mentionRegex.findAll(text)) {
        if (m.range.first > idx) {
            appendTextWithEmoji(text.substring(idx, m.range.first), boldStyle, inlineMap, emojiSize, emojiSizeDp)
        }
        val mention = m.value
        withLink(
            LinkAnnotation.Clickable(
                tag = "mention",
                styles = TextLinkStyles(
                    style = SpanStyle(color = MentionColor, fontWeight = boldStyle?.fontWeight),
                    pressedStyle = SpanStyle(background = MentionColor.copy(alpha = 0.25f))
                ),
                linkInteractionListener = androidx.compose.ui.text.LinkInteractionListener { _ -> RichTextActions.onMention?.invoke(mention) }
            )
        ) {
            append(mention)
        }
        idx = m.range.last + 1
    }
    if (idx < text.length) {
        appendTextWithEmoji(text.substring(idx), boldStyle, inlineMap, emojiSize, emojiSizeDp)
    }
}

private fun androidx.compose.ui.text.AnnotatedString.Builder.appendLinkifiedWithEmoji(
    segment: String,
    boldStyle: SpanStyle?,
    inlineMap: MutableMap<String, androidx.compose.foundation.text.InlineTextContent>,
    emojiSize: androidx.compose.ui.unit.TextUnit,
    emojiSizeDp: androidx.compose.ui.unit.Dp
) {
    var idx = 0
    for (m in urlRegex.findAll(segment)) {
        if (m.range.first < idx) continue
        if (m.range.first > idx) {
            appendMentionsWithEmoji(segment.substring(idx, m.range.first), boldStyle, inlineMap, emojiSize, emojiSizeDp)
        }
        var url = m.value
        while (url.isNotEmpty() && url.last() in ".,!?)]}؛،:؟\"'”’") url = url.dropLast(1)
        val linkStyle = SpanStyle(
            color = LinkColor,
            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
            fontWeight = boldStyle?.fontWeight
        )
        withLink(LinkAnnotation.Clickable("url", TextLinkStyles(style = linkStyle, pressedStyle = SpanStyle(background = LinkColor.copy(alpha = 0.22f))), androidx.compose.ui.text.LinkInteractionListener { _ -> RichTextActions.openUrl(url) })) {
            append(url)
        }
        val consumedEnd = m.range.first + url.length
        val leftover = segment.substring(consumedEnd, m.range.last + 1)
        if (leftover.isNotEmpty()) {
            appendMentionsWithEmoji(leftover, boldStyle, inlineMap, emojiSize, emojiSizeDp)
        }
        idx = m.range.last + 1
    }
    if (idx < segment.length) {
        appendMentionsWithEmoji(segment.substring(idx), boldStyle, inlineMap, emojiSize, emojiSizeDp)
    }
}

private fun androidx.compose.ui.text.AnnotatedString.Builder.appendTextWithEmoji(
    text: String,
    boldStyle: SpanStyle?,
    inlineMap: MutableMap<String, androidx.compose.foundation.text.InlineTextContent>,
    emojiSize: androidx.compose.ui.unit.TextUnit,
    emojiSizeDp: androidx.compose.ui.unit.Dp
) {
    val graphemes = com.example.emoji.EmojiMessageUtils.splitGraphemes(text)
    val plain = StringBuilder()
    fun flushPlain() {
        if (plain.isNotEmpty()) {
            val s = plain.toString()
            if (boldStyle != null) withStyle(boldStyle) { append(s) } else append(s)
            plain.clear()
        }
    }
    for (g in graphemes) {
        if (com.example.emoji.EmojiMessageUtils.isEmojiUnit(g)) {
            flushPlain()
            val id = "emoji_${inlineMap.size}"
            inlineMap[id] = androidx.compose.foundation.text.InlineTextContent(
                androidx.compose.ui.text.Placeholder(
                    width = emojiSize,
                    height = emojiSize,
                    placeholderVerticalAlign = androidx.compose.ui.text.PlaceholderVerticalAlign.TextCenter
                )
            ) {
                val url = com.example.emoji.NotoEmojiMap.remoteUrlFor(g)
                if (url != null) {
                    LottieEmojiReaction(
                        url = url,
                        size = emojiSizeDp
                    )
                } else {
                    Text(g, fontSize = emojiSize)
                }
            }
            appendInlineContent(id, g)
        } else {
            plain.append(g)
        }
    }
    flushPlain()
}


// Telegram-style link preview card: colored accent bar + site/title/description on top,
// large full-width thumbnail at the bottom. Tapping anywhere opens the link.
@Composable
fun LinkPreviewCard(url: String, isMe: Boolean, textColor: Color, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var preview by remember(url) {
        mutableStateOf(LinkPreviewCache.cache[url] ?: LinkPreviewCache.loadPersisted(context, url))
    }
    LaunchedEffect(url) {
        preview = LinkPreviewCache.fetch(url, context)
    }
    val data = preview ?: return
    if (data.title.isNullOrBlank() && data.description.isNullOrBlank() && data.imageUrl.isNullOrBlank()) return

    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { RichTextActions.openUrl(url) }
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(LinkColor)
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f).padding(vertical = 1.dp)) {
                if (!data.siteName.isNullOrBlank()) {
                    Text(
                        text = data.siteName,
                        color = LinkColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!data.title.isNullOrBlank()) {
                    Text(
                        text = data.title,
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!data.description.isNullOrBlank()) {
                    Text(
                        text = data.description,
                        color = textColor.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (!data.imageUrl.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            AsyncImage(
                model = data.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp, max = 200.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
        }
    }
}

// Telegram-style file bubble: circular file icon + filename + size/extension, optional caption below.
// While the file is still uploading (status == SENDING), shows a progress ring with a
// cancel (X) button in the middle instead of the file icon, and "جاري الرفع..." instead
// of the size - matching Telegram's own upload bubble.
@Composable
fun FileBubbleContent(
    att: Attachment,
    caption: String,
    isMe: Boolean,
    textColor: Color,
    timeColor: Color,
    msg: MessageModel,
    onCancelUpload: (() -> Unit)? = null
) {
    val fileName = att.fileName?.takeIf { it.isNotBlank() } ?: att.url.substringAfterLast('/').ifBlank { "File" }
    val ext = fileName.substringAfterLast('.', "").uppercase().take(4)
    val sizeStr = formatFileSize(att.fileSize)
    val subtitle = listOf(sizeStr, ext).filter { it.isNotBlank() }.joinToString("  ·  ")
    val isUploading = msg.status == MessageStatus.SENDING
    val isFailed = msg.status == MessageStatus.FAILED

    val fileCtx = androidx.compose.ui.platform.LocalContext.current
    val fileIndex by MediaIndex.files.collectAsState()
    val fileStates by MediaDownloadManager.states.collectAsState()
    val localFilePath = fileIndex[att.messageId]
    val fileDl = fileStates[att.messageId]
    val isRemoteDoc = !att.url.startsWith("content://") && !att.url.startsWith("file://")
    val canDownloadDoc = !isMe && localFilePath == null && isRemoteDoc && !isUploading

    Column(
        modifier = Modifier
            .clickable(enabled = !isUploading) {
                when {
                    localFilePath != null -> com.example.util.MediaStorage.openFile(fileCtx, localFilePath, att.mimeType)
                    canDownloadDoc && fileDl is MediaDlState.Downloading -> MediaDownloadManager.cancel(att.messageId)
                    canDownloadDoc -> MediaDownloadManager.download(fileCtx, att)
                    isRemoteDoc -> com.example.util.MediaStorage.openRemote(fileCtx, att.url, att.mimeType ?: "*/*")
                }
            }
            // عرض ثابت صغير مثل فقاعة الرسالة الصوتية (200dp)، بدل أن تتمدد على كل الشاشة
            .width(if (caption.isBlank()) 220.dp else 260.dp)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (isMe) Color.White else Color(0xFF3390EC)),
                contentAlignment = Alignment.Center
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = if (isMe) Color(0xFFC78B22) else Color.White,
                        strokeWidth = 2.5.dp
                    )
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "إلغاء",
                        tint = if (isMe) Color(0xFFC78B22) else Color.White,
                        modifier = Modifier
                            .size(16.dp)
                            .let { if (onCancelUpload != null) it.clickable { onCancelUpload() } else it }
                    )
                } else if (isFailed) {
                    Icon(
                        Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(22.dp)
                    )
                } else if (canDownloadDoc && fileDl is MediaDlState.Downloading) {
                    MediaProgressRing(progress = fileDl.progress, modifier = Modifier.size(32.dp), color = Color.White)
                    Icon(Icons.Filled.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                } else if (canDownloadDoc) {
                    Icon(
                        if (fileDl is MediaDlState.Failed) Icons.Filled.Refresh else Icons.Filled.Download,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        Icons.Outlined.InsertDriveFile,
                        contentDescription = null,
                        tint = if (isMe) Color(0xFFC78B22) else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileName,
                    color = textColor,
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // سطر واحد: الحجم/الحالة في اليسار، والوقت وعلامة الإرسال في اليمين (بدل سطر منفصل)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusText = when {
                        isUploading -> "جاري الرفع..."
                        isFailed -> "فشل الإرسال"
                        else -> subtitle
                    }
                    Text(
                        text = statusText,
                        color = if (isFailed) Color(0xFFE53935) else timeColor,
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (caption.isBlank()) {
                        Text(msg.time, fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Medium, color = timeColor)
                        MessageIndicators(
                            isMe = isMe,
                            status = msg.status,
                            isRead = false,
                            isSaved = msg.isSaved,
                            isPinned = msg.isPinned,
                            isImageOverlay = false
                        )
                    }
                }
            }
        }
        if (caption.isNotBlank()) {
            val richCaption = remember(caption) { buildRichMessageContent(caption, 17.sp, 18.dp) }
            Text(
                text = richCaption.text,
                inlineContent = richCaption.inlineContent,
                color = textColor,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                modifier = Modifier.padding(top = 6.dp, start = 2.dp)
            )
        }
        if (caption.isNotBlank()) {
            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(msg.time, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = timeColor)
                MessageIndicators(
                    isMe = isMe,
                    status = msg.status,
                    isRead = false,
                    isSaved = msg.isSaved,
                    isPinned = msg.isPinned,
                    isImageOverlay = false
                )
            }
        }
    }
}

@Composable
private fun MediaTile(att: Attachment, isMe: Boolean, modifier: Modifier, extraCount: Int, onClick: () -> Unit) {
    Box(modifier = modifier) {
        DownloadableImage(
            att = att,
            isMe = isMe,
            modifier = Modifier.fillMaxSize(),
            onOpen = onClick
        )
        if (extraCount > 0) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "+$extraCount", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun VideoBubbleContent(
    att: Attachment,
    isMe: Boolean,
    msg: MessageModel,
    screenWidth: androidx.compose.ui.unit.Dp,
    screenHeight: androidx.compose.ui.unit.Dp
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val files by MediaIndex.files.collectAsState()
    val ratio = (att.aspectRatio ?: 1.78f).coerceIn(0.5f, 2.2f)
    var w = screenWidth * 0.66f
    var h = w / ratio
    val maxH = screenHeight * 0.40f
    if (h > maxH) { h = maxH; w = h * ratio }
    w = w.coerceAtLeast(160.dp)

    Box(modifier = Modifier.padding(2.dp).clip(RoundedCornerShape(14.dp))) {
        DownloadableImage(
            att = att,
            isMe = isMe,
            isVideo = true,
            modifier = Modifier.size(width = w, height = h),
            onOpen = {
                val local = files[att.messageId]
                if (local != null) {
                    com.example.util.MediaStorage.openFile(ctx, local, att.mimeType ?: "video/*")
                } else if (!att.url.startsWith("content://") && !att.url.startsWith("file://")) {
                    com.example.util.MediaStorage.openRemote(ctx, att.url, att.mimeType ?: "video/*")
                }
            }
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.35f))
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(msg.time, fontSize = 10.sp, color = Color.White)
            MessageIndicators(
                isMe = isMe,
                status = msg.status,
                isRead = false,
                isSaved = msg.isSaved,
                isPinned = msg.isPinned,
                isImageOverlay = true,
                compact = true
            )
        }
    }
}
