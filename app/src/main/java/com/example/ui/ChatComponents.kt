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
import kotlinx.coroutines.launch

object VoicePlayer {
    private var player: android.media.MediaPlayer? = null
    private var progressJob: kotlinx.coroutines.Job? = null
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)

    private val _playingId = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val playingId: kotlinx.coroutines.flow.StateFlow<String?> = _playingId

    private val _isPlaying = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isPlaying: kotlinx.coroutines.flow.StateFlow<Boolean> = _isPlaying

    private val _positionMs = kotlinx.coroutines.flow.MutableStateFlow(0L)
    val positionMs: kotlinx.coroutines.flow.StateFlow<Long> = _positionMs

    private fun startProgressLoop() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (true) {
                _positionMs.value = try { player?.currentPosition?.toLong() ?: 0L } catch (e: Exception) { 0L }
                kotlinx.coroutines.delay(80)
            }
        }
    }

    fun toggle(id: String, url: String) {
        val currentPlayer = player
        if (_playingId.value == id && currentPlayer != null) {
            try {
                if (currentPlayer.isPlaying) {
                    currentPlayer.pause()
                    _isPlaying.value = false
                    progressJob?.cancel()
                } else {
                    currentPlayer.start()
                    _isPlaying.value = true
                    startProgressLoop()
                }
            } catch (e: Exception) {}
            return
        }
        stop()
        try {
            val mp = android.media.MediaPlayer()
            mp.setDataSource(url)
            mp.setOnPreparedListener {
                try { it.start() } catch (e: Exception) {}
                _isPlaying.value = true
                startProgressLoop()
            }
            mp.setOnCompletionListener { stop() }
            mp.setOnErrorListener { _, _, _ -> stop(); true }
            mp.prepareAsync()
            player = mp
            _playingId.value = id
        } catch (e: Exception) {
            stop()
        }
    }

    fun stop() {
        progressJob?.cancel()
        try { player?.stop() } catch (e: Exception) {}
        try { player?.release() } catch (e: Exception) {}
        player = null
        _playingId.value = null
        _isPlaying.value = false
        _positionMs.value = 0L
    }
}





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
    onImageClick: ((String) -> Unit)? = null,
    isFirstInCluster: Boolean,
    isLastInCluster: Boolean,
    onReplyClick: (MessageModel) -> Unit,
    onDoubleTap: (MessageModel) -> Unit,
    onLightLongPress: (MessageModel, Offset) -> Unit,
    onHeavyLongPress: (MessageModel, Offset) -> Unit
) {
    val isMe = msg.isMine
    val bubbleColor = if (isMe) Color(0xFFFFF3D0) else Color.White
    val textColor = if (isMe) Color(0xFF222222) else Color.Black
    val timeColor = textColor.copy(alpha = 0.6f)

    val shape = RoundedCornerShape(
        topStart = if (!isMe && !isFirstInCluster) 4.dp else 20.dp,
        topEnd = if (isMe && !isFirstInCluster) 4.dp else 20.dp,
        bottomStart = if (!isMe && !isLastInCluster) 4.dp else 20.dp,
        bottomEnd = if (isMe && !isLastInCluster) 4.dp else 20.dp
    )

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
    val isEmojiOnlyMessage = emojiOnlySequence != null && emojiOnlyImageAttachments.isEmpty() &&
        emojiOnlyAudioAtt == null && documentAtt == null

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
        Column(
            modifier = Modifier
                .widthIn(min = 60.dp, max = if (msg.reactions.isNotEmpty()) screenWidth * 0.74f else screenWidth * 0.85f)
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

            if (isEmojiOnlyMessage) {
                val units = emojiOnlySequence!!
                Column(modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp)) {
                    if (units.size == 1) {
                        // إيموجي وحد -> كبير (100dp)
                        val path = NotoEmojiMap.remoteUrlFor(units[0])
                        if (path != null) {
                            LottieEmojiReaction(url = path, size = 100.dp)
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
                                        LottieEmojiReaction(url = path, size = emojiSize, animate = animated)
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
                        coil.compose.SubcomposeAsyncImage(
                                    loading = {
                                        androidx.compose.foundation.layout.Box(
                                            modifier = androidx.compose.ui.Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black.copy(alpha=0.1f)),
                                            contentAlignment = androidx.compose.ui.Alignment.Center
                                        ) {
                                            androidx.compose.material3.CircularProgressIndicator(
                                                modifier = androidx.compose.ui.Modifier.size(24.dp),
                                                color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                                strokeWidth = 2.dp
                                            )
                                        }
                                    },
                                    
                            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                .data(rememberMediaSource(androidx.compose.ui.platform.LocalContext.current, att.messageId, att.thumbnailUrl ?: att.url, isThumbnail = att.thumbnailUrl != null))
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = singleW, height = singleH)
                                .clickable { onImageClick?.invoke(att.messageId) },

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
                val playingId by VoicePlayer.playingId.collectAsState()
                val isPlayingNow by VoicePlayer.isPlaying.collectAsState()
                val positionMs by VoicePlayer.positionMs.collectAsState()
                val isThisOne = playingId == audioAtt.messageId
                val isThisPlaying = isThisOne && isPlayingNow
                val totalMs = ((audioAtt.aspectRatio ?: 0f) * 1000).toLong().coerceAtLeast(0L)
                val progress = if (isThisOne && totalMs > 0) (positionMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f) else 0f
                val waveform = remember(audioAtt.messageId) {
                    val rnd = java.util.Random(audioAtt.messageId.hashCode().toLong())
                    List(24) { 2 + rnd.nextInt(8) }
                }
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).width(200.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isMe) Color.White else Color(0xFF007AFF))
                            .clickable { VoicePlayer.toggle(audioAtt.messageId, audioAtt.url) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isThisPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            null,
                            tint = if (isMe) Color(0xFF4FA953) else Color.White,
                            modifier = Modifier.size(if (isThisPlaying) 20.dp else 24.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(modifier = Modifier.fillMaxWidth().height(20.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            waveform.forEachIndexed { idx, v ->
                                val played = isThisOne && (idx.toFloat() / waveform.size) < progress
                                Box(modifier = Modifier.weight(1f).height((v*2).dp).clip(CircleShape).background(
                                    if (played) (if (isMe) Color(0xFF4FA953) else Color(0xFF007AFF))
                                    else (if (isMe) Color.Black.copy(0.28f) else Color(0xFF007AFF).copy(0.4f))
                                ))
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(top=2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            val shownMs = if (isThisOne) positionMs else totalMs
                            val shownSec = (shownMs / 1000).toInt()
                            Text(String.format("%d:%02d", shownSec / 60, shownSec % 60), fontSize = 11.sp, color = timeColor)
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
            } else if (documentAtt != null) {
                FileBubbleContent(
                    att = documentAtt,
                    caption = cleanText,
                    isMe = isMe,
                    textColor = textColor,
                    timeColor = timeColor,
                    msg = msg
                )
            } else if (cleanText.isNotBlank()) {
                val detectedUrl = remember(cleanText) { extractFirstUrl(cleanText) }
                var isSingleLine by remember(cleanText) { mutableStateOf(detectedUrl == null) }
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr
                ) {
                    if (isSingleLine) {
                        FlowRow(
                            modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 10.dp)
                        ) {
                            Text(
                                text = parseMessageText(cleanText),
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
                            modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 8.dp)
                        ) {
                            if (detectedUrl != null) {
                                LinkPreviewCard(url = detectedUrl, isMe = isMe, textColor = textColor)
                            }
                            Text(
                                text = parseMessageText(cleanText),
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
                tint = if (isImageOverlay) Color.White else neutralColor,
                modifier = Modifier.size(if (isImageOverlay) 12.dp else 13.dp)
            )
        }
        if (isPinned) {
            Icon(
                imageVector = Icons.Filled.PushPin,
                contentDescription = "Pinned",
                tint = if (isImageOverlay) Color.White else neutralColor,
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
        withLink(LinkAnnotation.Url(url, TextLinkStyles(style = linkStyle))) {
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

// Telegram-style link preview card: colored accent bar + site/title/description on top,
// large full-width thumbnail at the bottom. Tapping anywhere opens the link.
@Composable
fun LinkPreviewCard(url: String, isMe: Boolean, textColor: Color, modifier: Modifier = Modifier) {
    var preview by remember(url) { mutableStateOf(LinkPreviewCache.cache[url]) }
    LaunchedEffect(url) {
        preview = LinkPreviewCache.fetch(url)
    }
    val data = preview ?: return
    if (data.title.isNullOrBlank() && data.description.isNullOrBlank() && data.imageUrl.isNullOrBlank()) return

    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { uriHandler.openUri(url) }
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
@Composable
fun FileBubbleContent(
    att: Attachment,
    caption: String,
    isMe: Boolean,
    textColor: Color,
    timeColor: Color,
    msg: MessageModel
) {
    val fileName = att.fileName?.takeIf { it.isNotBlank() } ?: att.url.substringAfterLast('/').ifBlank { "File" }
    val ext = fileName.substringAfterLast('.', "").uppercase().take(4)
    val sizeStr = formatFileSize(att.fileSize)
    val subtitle = listOf(sizeStr, ext).filter { it.isNotBlank() }.joinToString("  ·  ")

    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp).widthIn(min = 210.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isMe) Color.White else Color(0xFF3390EC)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.InsertDriveFile,
                    contentDescription = null,
                    tint = if (isMe) Color(0xFFC78B22) else Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileName,
                    color = textColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotBlank()) {
                    Text(text = subtitle, color = timeColor, fontSize = 12.sp)
                }
            }
        }
        if (caption.isNotBlank()) {
            Text(
                text = parseMessageText(caption),
                color = textColor,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                modifier = Modifier.padding(top = 6.dp, start = 2.dp)
            )
        }
        Row(
            modifier = Modifier.align(Alignment.End).padding(top = 2.dp),
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

@Composable
private fun MediaTile(att: Attachment, modifier: Modifier, extraCount: Int, onClick: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Box(modifier = modifier.clickable { onClick() }) {
        coil.compose.SubcomposeAsyncImage(
            loading = {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.1f)))
            },
            model = rememberMediaSource(context, att.messageId, att.thumbnailUrl ?: att.url, isThumbnail = att.thumbnailUrl != null),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
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
