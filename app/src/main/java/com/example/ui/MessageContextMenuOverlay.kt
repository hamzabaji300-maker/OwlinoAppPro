package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.deleteRed
import kotlinx.coroutines.delay

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun MessageContextMenuOverlay(
    message: MessageModel,
    showFullMenu: Boolean = true,
    isChannel: Boolean = false,
    onDismissRequest: () -> Unit,
    onReactionSelected: (String) -> Unit,
    onReply: () -> Unit = {},
    onEdit: () -> Unit = {},
    onCopy: () -> Unit = {},
    onDelete: () -> Unit = {},
    onForward: () -> Unit = {},
    onSelect: () -> Unit = {},
    onPin: () -> Unit = {},
    onSave: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    bounds: Rect = Rect.Zero
) {
    var visible by remember { mutableStateOf(false) }
    // للقنوات فقط: القائمة تبدأ بالأيقونات العادية، وتتحول لصف الإيموجيات
    // فقط بعد الضغط على أيقونة "React" (بدل ظهور صف التفاعلات دائماً فوق).
    var showReactionPicker by remember { mutableStateOf(!isChannel) }
    var showFullEmojiPicker by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    val handleDismiss = {
        visible = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { handleDismiss() }
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(250)) + fadeIn(tween(250)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(200)) + fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {} // block clicks
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .animateContentSize(animationSpec = tween(220))
                        .padding(bottom = 10.dp, top = 14.dp)
                ) {
                    // Always show reactions
                    if (showReactionPicker) {
                    // 💎 💸 📉 📈 🪙 🪎 + باقي الإيموجيات — كلها إيموجيات النظام الثابتة
                    // (بدون أنيميشن هنا)، وتظهر متحركة فقط بعد اختيارها داخل شاشة الرسائل
                    val allQuickReactions = listOf(
                        "\uD83D\uDC8E", "\uD83D\uDCB8", "\uD83D\uDCC9",
                        "\uD83D\uDCC8", "\uD83E\uDE99", "\uD83E\uDE8E",
                        "\uD83D\uDE02", "\uD83D\uDE0D", "\uD83D\uDE22", "\uD83D\uDE21",
                        "\uD83D\uDC4D", "\uD83D\uDC4F", "\uD83D\uDE4F", "\uD83D\uDD25", "\uD83D\uDCAF",
                        "\uD83C\uDF89", "\uD83D\uDE31", "\uD83E\uDD14", "\uD83D\uDC40",
                        "\u2764\uFE0F", "\uD83D\uDC94", "\uD83D\uDE34", "\uD83E\uDD1D", "\uD83D\uDE4C",
                        "\uD83D\uDE05", "\uD83E\uDD73", "\uD83D\uDE0E", "\uD83E\uDD29",
                        "\uD83D\uDE2D", "\uD83E\uDD7A", "\uD83D\uDE24", "\uD83E\uDD17",
                        "\uD83D\uDC4C", "\u270C\uFE0F", "\uD83D\uDE80", "\u2B50",
                        "\uD83C\uDFC6", "\uD83D\uDCAA", "\uD83E\uDD23", "\uD83D\uDE0A",
                        "\uD83D\uDE48", "\uD83C\uDF39", "\uD83C\uDFAF"
                    )
                    run {
                        // صف 6 إيموجيات متحركة: للمحادثات بين الأشخاص (كما كان) وللقنوات (والباقي يظهر بعد "React")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            allQuickReactions.take(6).forEach { reaction ->
                                val reactionUrl = com.example.emoji.NotoEmojiMap.remoteUrlFor(reaction)
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            onReactionSelected(reaction)
                                            handleDismiss()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (reactionUrl != null) {
                                        LottieEmojiReaction(url = reactionUrl, size = 38.dp, animate = false)
                                    }
                                }
                            }
                        }
                    }

                    // القائمة الكاملة السفلية (36+ إيموجي) — للقناة فقط، تظهر بعد الضغط على "React"
                    if (isChannel) {
                    val moreReactions = listOf(
                        "\uD83D\uDE02", "\uD83D\uDE0D", "\uD83D\uDE22", "\uD83D\uDE21",
                        "\uD83D\uDC4F", "\uD83D\uDE4F", "\uD83D\uDD25", "\uD83D\uDCAF",
                        "\uD83C\uDF89", "\uD83D\uDE31", "\uD83E\uDD14", "\uD83D\uDC40",
                        "\uD83D\uDC94", "\uD83D\uDE34", "\uD83E\uDD1D", "\uD83D\uDE4C",
                        "\uD83D\uDE05", "\uD83E\uDD73", "\uD83D\uDE0E", "\uD83E\uDD29",
                        "\uD83D\uDE2D", "\uD83E\uDD7A", "\uD83D\uDE24", "\uD83E\uDD17",
                        "\uD83D\uDC4C", "\u270C\uFE0F", "\uD83D\uDE80", "\u2B50",
                        "\uD83C\uDFC6", "\uD83D\uDCAA", "\uD83E\uDD23", "\uD83D\uDE0A",
                        "\uD83D\uDE48", "\uD83E\uDD73", "\uD83C\uDF39", "\uD83C\uDFAF"
                    )
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 210.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        moreReactions.forEach { reaction ->
                            val reactionUrl = com.example.emoji.NotoEmojiMap.remoteUrlFor(reaction)
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        onReactionSelected(reaction)
                                        handleDismiss()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (reactionUrl != null) {
                                    LottieEmojiReaction(url = reactionUrl, size = 32.dp, animate = false)
                                }
                            }
                        }
                    }
                    }
                    }

                    if (showFullMenu && !(isChannel && showReactionPicker)) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)))
                        Spacer(modifier = Modifier.height(6.dp))

                        val textColor = MaterialTheme.colorScheme.onSurface
                        val iconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        
                        Row(modifier = Modifier.fillMaxWidth()) {
                           Column(modifier = Modifier.weight(1f)) {
                               if (isChannel) {
                                   MessageMenuItem("React", Icons.Outlined.AddReaction, textColor, iconColor, { showReactionPicker = true })
                               } else {
                                   MessageMenuItem("Reply", Icons.AutoMirrored.Outlined.Reply, textColor, iconColor, { onReply(); handleDismiss() })
                               }
                               MessageMenuItem("Forward", Icons.AutoMirrored.Outlined.ArrowForward, textColor, iconColor, { onForward(); handleDismiss() })
                               MessageMenuItem("Copy", Icons.Outlined.ContentCopy, textColor, iconColor, { onCopy(); handleDismiss() })
                           }
                           Column(modifier = Modifier.weight(1f)) {
                               MessageMenuItem("Select", Icons.Outlined.CheckCircleOutline, textColor, iconColor, { onSelect(); handleDismiss() })
                               if (message.isMine) {
                                   MessageMenuItem("Edit", Icons.Outlined.Edit, textColor, iconColor, { onEdit(); handleDismiss() })
                               }
                               if (!isChannel) {
                                   MessageMenuItem(if (message.isPinned) "Unpin" else "Pin", Icons.Outlined.PushPin, textColor, iconColor, { onPin(); handleDismiss() })
                               }
                               MessageMenuItem(if (message.isSaved) "Unsave" else "Save", Icons.Outlined.StarBorder, textColor, iconColor, { onSave(); handleDismiss() })
                               MessageMenuItem("Delete", Icons.Outlined.DeleteOutline, deleteRed, deleteRed, { onDelete(); handleDismiss() })
                           }
                        }
                    }
                }
            }
        }
    }

    // Auto dismiss after animation
    LaunchedEffect(visible) {
        if (!visible) {
            delay(200)
            onDismissRequest()
        }
    }

    if (showFullEmojiPicker) {
        FullEmojiPickerDialog(
            onEmojiPicked = { emoji ->
                onReactionSelected(emoji)
                showFullEmojiPicker = false
                handleDismiss()
            },
            onDismiss = { showFullEmojiPicker = false }
        )
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun FullEmojiPickerDialog(
    onEmojiPicked: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // قائمة موسّعة من الإيموجيات الشائعة مع كلمات مفتاحية بسيطة للبحث
    val emojiKeywords = listOf(
        "\uD83D\uDE02" to "laugh funny haha",
        "\uD83D\uDE0D" to "love heart eyes",
        "\uD83D\uDE22" to "sad cry",
        "\uD83D\uDE21" to "angry mad",
        "\uD83D\uDC4D" to "like thumbs up good",
        "\uD83D\uDC4F" to "clap applause",
        "\uD83D\uDE4F" to "pray thanks please",
        "\uD83D\uDD25" to "fire hot lit",
        "\uD83D\uDCAF" to "hundred perfect",
        "\uD83C\uDF89" to "party celebrate",
        "\uD83D\uDE31" to "scared shocked",
        "\uD83E\uDD14" to "think hmm",
        "\uD83D\uDC40" to "eyes look",
        "\u2764\uFE0F" to "love heart red",
        "\uD83D\uDC94" to "broken heart",
        "\uD83D\uDE34" to "sleepy tired",
        "\uD83E\uDD1D" to "handshake deal",
        "\uD83D\uDE4C" to "raise hands yay",
        "\uD83D\uDE05" to "sweat laugh",
        "\uD83E\uDD73" to "party face",
        "\uD83D\uDE0E" to "cool sunglasses",
        "\uD83E\uDD29" to "star struck",
        "\uD83D\uDE2D" to "loud crying",
        "\uD83E\uDD7A" to "pleading puppy",
        "\uD83D\uDE24" to "triumph proud",
        "\uD83D\uDC4C" to "ok okay",
        "\u270C\uFE0F" to "peace victory",
        "\uD83D\uDE80" to "rocket launch",
        "\u2B50" to "star",
        "\uD83C\uDFC6" to "trophy win",
        "\uD83D\uDCAA" to "strong muscle",
        "\uD83E\uDD23" to "rofl laugh",
        "\uD83D\uDE0A" to "smile happy",
        "\uD83D\uDE48" to "no evil monkey",
        "\uD83C\uDF39" to "rose flower",
        "\uD83C\uDFAF" to "target goal",
        "\uD83D\uDE0B" to "yum tasty",
        "\uD83E\uDD70" to "smile hearts",
        "\uD83D\uDE18" to "kiss heart",
        "\uD83E\uDD2A" to "crazy wild",
        "\uD83D\uDE43" to "upside down",
        "\uD83D\uDE1C" to "wink tongue",
        "\uD83E\uDD71" to "yawn tired",
        "\uD83E\uDD22" to "sick nauseated",
        "\uD83E\uDD75" to "hot heat",
        "\uD83E\uDD76" to "cold freezing",
        "\uD83D\uDC7B" to "ghost spooky",
        "\uD83D\uDC7D" to "alien",
        "\uD83D\uDE3A" to "cat happy",
        "\uD83D\uDC36" to "dog",
        "\uD83C\uDF55" to "pizza food",
        "\u2615" to "coffee",
        "\uD83C\uDF89" to "confetti party",
        "\uD83D\uDC8E" to "diamond gem",
        "\uD83D\uDCB8" to "money cash",
        "\uD83D\uDCC8" to "chart up",
        "\uD83D\uDCC9" to "chart down",
        "\uD83E\uDE99" to "coin crypto",
        "\uD83E\uDD11" to "money face"
    )

    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        if (query.isBlank()) emojiKeywords
        else emojiKeywords.filter { it.second.contains(query.trim(), ignoreCase = true) }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 480.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text("Search emoji", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontSize = 15.sp)
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                filtered.forEach { (emoji, _) ->
                    val url = com.example.emoji.NotoEmojiMap.remoteUrlFor(emoji)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onEmojiPicked(emoji) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (url != null) {
                            LottieEmojiReaction(url = url, size = 32.dp, animate = false)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageMenuItem(
    text: String,
    icon: ImageVector,
    textColor: Color = Color.Black,
    iconTint: Color = Color.Gray,
    onClick: () -> Unit
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.6f, stiffness = 400f)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = interactionSource, indication = androidx.compose.foundation.LocalIndication.current) { onClick() }
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = iconTint,
            modifier = Modifier.size(20.dp).scale(scale)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}
