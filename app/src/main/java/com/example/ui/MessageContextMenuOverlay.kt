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
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.ui.graphics.graphicsLayer
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

// ===================== قوائم الإيموجي =====================
/** صف التفاعل السريع في الدردشة الفردية/المجموعات (افتراضي تيليجرام). */
val TgQuickReactions = listOf("❤️", "👍", "👎", "🔥", "🥰", "👏", "😁")

/** شبكة التفاعلات الموسّعة (نفس ترتيب تيليجرام، 8 أعمدة). */
val TgAllReactions = listOf(
    "❤️", "👍", "👎", "🔥", "🥰", "👏", "😁", "🤔",
    "🤯", "😱", "🤬", "😢", "🎉", "🤩", "🤮", "💩",
    "🙏", "👌", "🕊️", "🤡", "🥱", "🥴", "😍", "🐳",
    "💯", "🤣", "⚡", "🍌", "🏆", "💔", "🤨", "😐",
    "🍓", "🍾", "💋", "😈", "😴", "😭", "🤓", "👻",
    "👀", "🎃", "🙈", "😇", "😨", "🤝", "🤗", "🎅",
    "🎄", "💅", "🤪", "🗿", "🆒", "💘", "🙉", "🦄",
    "😘", "💊", "🙊", "😎", "👾", "😡"
)

/** تفاعلات القنوات المخصّصة: تظهر وحدها في بطاقة مربعة وسط الشاشة. */
val ChannelReactionEmojis = listOf(
    "\uD83D\uDC8E", "\uD83D\uDCB8", "\uD83D\uDCC9", "\uD83D\uDCC8", "\uD83E\uDE99", "\uD83E\uDE8E",
    "\uD83D\uDC4D", "\uD83D\uDD25", "\u2764\uFE0F", "\uD83D\uDE02", "\uD83D\uDE0D", "\uD83D\uDE22",
    "\uD83D\uDE21", "\uD83D\uDC4F", "\uD83D\uDE4F", "\uD83D\uDCAF", "\uD83C\uDF89", "\uD83D\uDE31",
    "\uD83E\uDD14", "\uD83D\uDC40", "\uD83D\uDC94", "\uD83D\uDE34", "\uD83E\uDD1D", "\uD83D\uDE4C",
    "\uD83D\uDE05", "\uD83E\uDD73", "\uD83D\uDE0E", "\uD83E\uDD29", "\uD83D\uDE2D", "\uD83E\uDD7A",
    "\uD83D\uDE24", "\uD83E\uDD17", "\uD83D\uDC4C", "\u270C\uFE0F", "\uD83D\uDE80", "\u2B50",
    "\uD83C\uDFC6", "\uD83D\uDCAA", "\uD83E\uDD23", "\uD83D\uDE0A", "\uD83D\uDE48", "\uD83C\uDF39"
).distinct()

private class OverlayItem(val label: String, val icon: ImageVector, val danger: Boolean = false, val action: () -> Unit)

/**
 * قائمة الضغط المطوّل:
 *  - القناة: بطاقة مربعة في منتصف الشاشة فيها التفاعلات المخصّصة فقط، وتحتها قائمة الإجراءات.
 *  - الفردي/المجموعات (نمط تيليجرام): الفقاعة تبقى مضيئة، شريط تفاعلات فوقها، وقائمة إجراءات تحتها،
 *    وسهم يفتح شبكة التفاعلات الكاملة.
 */
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
    var expanded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val dismiss: () -> Unit = { visible = false }
    LaunchedEffect(visible) {
        if (!visible) {
            delay(180)
            onDismissRequest()
        }
    }

    val density = androidx.compose.ui.platform.LocalDensity.current
    val cfg = androidx.compose.ui.platform.LocalConfiguration.current
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface

    // حدود الفقاعة: نتابعها لحظات (أثناء انزلاق القائمة بعد الضغط) حتى تلتصق بها العناصر
    var anchor by remember { mutableStateOf(bounds) }
    LaunchedEffect(Unit) {
        repeat(45) {
            DustRegistry.bubbleBounds[message.id]?.let { anchor = it }
            delay(16)
        }
    }

    val scrimAlpha by androidx.compose.animation.core.animateFloatAsState(if (visible) 0.5f else 0f, tween(200), label = "ctx_scrim")

    // عناصر الإجراءات
    val menuItems = buildList {
        if (!isChannel) add(OverlayItem("رد", Icons.AutoMirrored.Outlined.Reply) { onReply(); dismiss() })
        if (message.text.isNotBlank()) add(OverlayItem("نسخ", Icons.Outlined.ContentCopy) { onCopy(); dismiss() })
        add(OverlayItem("إعادة توجيه", Icons.AutoMirrored.Outlined.ArrowForward) { onForward(); dismiss() })
        if (!isChannel) add(OverlayItem(if (message.isPinned) "إلغاء التثبيت" else "تثبيت", Icons.Outlined.PushPin) { onPin(); dismiss() })
        if (message.isMine && message.text.isNotBlank()) add(OverlayItem("تعديل", Icons.Outlined.Edit) { onEdit(); dismiss() })
        add(OverlayItem(if (message.isSaved) "إلغاء الحفظ" else "حفظ", Icons.Outlined.StarBorder) { onSave(); dismiss() })
        add(OverlayItem("تحديد", Icons.Outlined.CheckCircleOutline) { onSelect(); dismiss() })
        add(OverlayItem("حذف", Icons.Outlined.DeleteOutline, danger = true) { onDelete(); dismiss() })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // التعتيم مع "ثقب" يُبقي الفقاعة مضيئة (للدردشة)
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { dismiss() }
        ) {
            drawRect(Color.Black.copy(alpha = scrimAlpha))
            if (!isChannel && anchor.height > 1f) {
                drawRoundRect(
                    color = Color.Black,
                    topLeft = anchor.topLeft,
                    size = anchor.size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()),
                    blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                )
            }
        }

        if (isChannel) {
            // ============ القناة: بطاقة مربعة وسط الشاشة ============
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .widthIn(max = 340.dp)
                    .padding(horizontal = 22.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(160)) + scaleIn(spring(dampingRatio = 0.75f, stiffness = 400f), initialScale = 0.85f),
                    exit = fadeOut(tween(120)) + scaleOut(tween(140), targetScale = 0.9f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = surface,
                            shadowElevation = 14.dp,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                        ) {
                            Column(Modifier.padding(start = 10.dp, end = 10.dp, top = 12.dp, bottom = 6.dp)) {
                                Text(
                                    "اختر تفاعلًا",
                                    color = onSurface.copy(alpha = 0.6f), fontSize = 13.sp, fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(start = 6.dp, bottom = 6.dp)
                                )
                                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(6),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    gridItems(ChannelReactionEmojis, key = { it }) { e ->
                                        Box(
                                            Modifier
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { onReactionSelected(e); dismiss() },
                                            contentAlignment = Alignment.Center
                                        ) { StaticEmojiImage(e, 34.dp) }
                                    }
                                }
                            }
                        }
                        if (showFullMenu) {
                            Spacer(Modifier.height(10.dp))
                            OverlayActionsCard(menuItems, surface, onSurface, Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        } else {
            // ============ الفردي والمجموعات: نمط تيليجرام ============
            val screenH = cfg.screenHeightDp.dp
            val screenW = cfg.screenWidthDp.dp
            val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val has = anchor.height > 1f
            val aTop = with(density) { anchor.top.toDp() }
            val aBottom = with(density) { anchor.bottom.toDp() }
            val barH = 52.dp
            val barTop = if (has) (aTop - barH - 8.dp).coerceAtLeast(statusTop + 8.dp) else screenH * 0.3f
            val menuH = 44.dp * menuItems.size + 12.dp
            val menuTop = if (has) minOf(aBottom + 8.dp, screenH - menuH - navBottom - 12.dp) else screenH * 0.45f
            val side = if (message.isMine) Alignment.TopEnd else Alignment.TopStart

            // شريط التفاعلات / الشبكة الموسّعة
            AnimatedVisibility(
                visible = visible,
                modifier = Modifier.align(side).padding(horizontal = 12.dp).offset(y = barTop),
                enter = fadeIn(tween(160)) + scaleIn(spring(dampingRatio = 0.75f, stiffness = 400f), initialScale = 0.8f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)),
                exit = fadeOut(tween(120)) + scaleOut(tween(140), targetScale = 0.85f)
            ) {
                AnimatedContent(
                    targetState = expanded,
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                    label = "react_bar"
                ) { ex ->
                    if (!ex) {
                        Surface(shape = RoundedCornerShape(27.dp), color = surface, shadowElevation = 10.dp) {
                            Row(
                                Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TgQuickReactions.forEach { e ->
                                    Box(
                                        Modifier.size(38.dp).clip(CircleShape).clickable { onReactionSelected(e); dismiss() },
                                        contentAlignment = Alignment.Center
                                    ) { StaticEmojiImage(e, 30.dp) }
                                }
                                Spacer(Modifier.width(4.dp))
                                Box(
                                    Modifier.size(32.dp).clip(CircleShape).background(onSurface.copy(alpha = 0.08f)).clickable { expanded = true },
                                    contentAlignment = Alignment.Center
                                ) { Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = onSurface.copy(alpha = 0.7f), modifier = Modifier.size(22.dp)) }
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(22.dp), color = surface, shadowElevation = 12.dp,
                            modifier = Modifier.width(screenW - 24.dp).height(minOf(340.dp, screenH * 0.5f))
                        ) {
                            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(8),
                                contentPadding = PaddingValues(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                gridItems(TgAllReactions, key = { it }) { e ->
                                    Box(
                                        Modifier.aspectRatio(1f).clip(CircleShape).clickable { onReactionSelected(e); dismiss() },
                                        contentAlignment = Alignment.Center
                                    ) { StaticEmojiImage(e, 30.dp) }
                                }
                            }
                        }
                    }
                }
            }

            // قائمة الإجراءات تحت الفقاعة (تُخفى أثناء الشبكة الموسّعة، وفي الضغط الخفيف)
            if (showFullMenu) {
                AnimatedVisibility(
                    visible = visible && !expanded,
                    modifier = Modifier.align(side).padding(horizontal = 12.dp).offset(y = menuTop),
                    enter = fadeIn(tween(160)) + scaleIn(spring(dampingRatio = 0.8f, stiffness = 400f), initialScale = 0.9f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(if (message.isMine) 1f else 0f, 0f)),
                    exit = fadeOut(tween(120)) + scaleOut(tween(140), targetScale = 0.92f)
                ) {
                    OverlayActionsCard(menuItems, surface, onSurface, Modifier.width(250.dp))
                }
            }
        }
    }

}

@Composable
private fun OverlayActionsCard(items: List<OverlayItem>, surface: Color, onSurface: Color, modifier: Modifier) {
    Surface(shape = RoundedCornerShape(18.dp), color = surface, shadowElevation = 12.dp, modifier = modifier) {
        Column(Modifier.padding(vertical = 6.dp)) {
            items.forEach { item ->
                if (item.danger) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 2.dp).height(1.dp).background(onSurface.copy(alpha = 0.08f)))
                }
                val c = if (item.danger) deleteRed else onSurface
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clickable { item.action() }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(item.label, color = c, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Icon(item.icon, contentDescription = null, tint = if (item.danger) deleteRed else onSurface.copy(alpha = 0.7f), modifier = Modifier.size(21.dp))
                }
            }
        }
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
