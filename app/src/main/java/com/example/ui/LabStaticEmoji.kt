package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import coil.compose.SubcomposeAsyncImage
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import com.example.emoji.NotoEmojiMap
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * إيموجي ثابت للتفاعلات: يُرسم كنص مباشرة بلا تحميل من الإنترنت ولا Lottie،
 * فيظهر فورًا ولا يسبب أي تقطيع (بدل الإيموجي المتحرك الذي كان يتأخر ويتقطع).
 */
@Composable
fun StaticEmoji(emoji: String, size: Dp) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Text(emoji, fontSize = (size.value * 0.74f).sp)
    }
}

private const val EYE_PREFIX = "\uD83D\uDC41 "

/**
 * وقت الرسالة: إذا كان يبدأ برمز العين 👁 (مشاهدات منشورات القناة) نستبدله بأيقونة العين الحقيقية
 * (Visibility) كما في تيليجرام بدل إيموجي العين.
 */
@Composable
fun MsgTimeText(
    text: String,
    fontSize: TextUnit,
    color: Color,
    fontWeight: FontWeight? = null,
    lineHeight: TextUnit = TextUnit.Unspecified
) {
    if (text.startsWith(EYE_PREFIX)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.Visibility, contentDescription = null, tint = color,
                modifier = Modifier.size((fontSize.value * 1.5f).dp)
            )
            Spacer(Modifier.width(3.dp))
            Text(text.removePrefix(EYE_PREFIX), fontSize = fontSize, color = color, fontWeight = fontWeight, lineHeight = lineHeight)
        }
    } else {
        Text(text, fontSize = fontSize, color = color, fontWeight = fontWeight, lineHeight = lineHeight)
    }
}

/**
 * شريحة التفاعل بنمط واتساب (الدردشة الفردية والمجموعات): كبسولة دائرية صغيرة تحت زاوية الفقاعة
 * تحتوي إيموجي التفاعل (متحرك) وعدد التفاعلات إن زادت عن واحد.
 */
@Composable
fun ReactionChip(reactions: List<String>, messageId: String, modifier: Modifier = Modifier) {
    val theme = LocalSettingsTheme.current.theme
    val distinct = reactions.distinct()
    Row(
        modifier = modifier
            .shadow(2.dp, CircleShape)
            .clip(CircleShape)
            .background(theme.surfaceColor)
            .border(0.5.dp, theme.textSecondary.copy(alpha = 0.25f), CircleShape)
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        distinct.take(3).forEach { em ->
            val shown = NotoEmojiMap.reactionToEmoji(em)
            val url = remember(em) { NotoEmojiMap.remoteUrlFor(shown) }
            if (url != null) {
                val auto = remember(messageId, em) { ReactionPlayRegistry.shouldPlay("$messageId|$em") }
                LottieEmojiReaction(url = url, size = 20.dp, tapToPlay = true, autoPlayOnce = auto)
            } else Text(shown, fontSize = 15.sp)
        }
        if (reactions.size > 1) {
            Spacer(Modifier.width(3.dp))
            Text("${reactions.size}", fontSize = 12.sp, color = theme.textSecondary, fontWeight = FontWeight.Medium)
        }
    }
}

/** يرفع العنصر للأعلى بمقدار [amount] ويقلّل ارتفاعه المحجوز بنفس المقدار، فيتداخل مع العنصر الذي فوقه. */
fun Modifier.overlapUp(amount: Dp): Modifier = this.layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val a = amount.roundToPx()
    layout(placeable.width, (placeable.height - a).coerceAtLeast(0)) { placeable.place(0, -a) }
}

/** true داخل منشورات القناة: الفقاعة تمتد حتى هامش متساوٍ (8dp) على الجانبين. */
val LocalChannelFullWidth = androidx.compose.runtime.compositionLocalOf { false }

// ============ إيموجي ثابت من الشبكة (صورة PNG خفيفة تُخزَّن بالكاش) ============
private fun staticEmojiUrls(code: String): List<String> {
    if (code.isEmpty()) return emptyList()
    val alt = when {
        code.contains("_fe0f") -> code.replace("_fe0f", "")
        !code.contains("_") -> code + "_fe0f"
        else -> null
    }
    return listOfNotNull(code, alt).distinct().flatMap { c ->
        listOf(
            "https://cdn.jsdelivr.net/gh/googlefonts/noto-emoji@main/png/128/emoji_u$c.png",
            "https://raw.githubusercontent.com/googlefonts/noto-emoji/main/png/128/emoji_u$c.png"
        )
    }
}

/**
 * إيموجي Noto ثابت: نص النظام يظهر فورًا (بلا أي تأخير) ثم يُستبدل بصورة Noto عند وصولها،
 * وتُخزَّن الصور في كاش Coil فتظهر في المرات التالية مباشرة.
 * إن فشل التحميل يبقى نص الإيموجي.
 */
@Composable
fun StaticEmojiImage(emoji: String, size: Dp) {
    val code = remember(emoji) { NotoEmojiMap.assetFor(emoji) ?: "" }
    val urls = remember(code) { staticEmojiUrls(code) }
    var idx by remember(code) { mutableIntStateOf(0) }
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        if (idx >= urls.size) {
            Text(emoji, fontSize = (size.value * 0.74f).sp)
        } else {
            SubcomposeAsyncImage(
                model = urls[idx],
                contentDescription = null,
                modifier = Modifier.size(size),
                loading = { Text(emoji, fontSize = (size.value * 0.74f).sp) },
                onError = { idx++ }
            )
        }
    }
}

/** تحميل مسبق لصور الإيموجي الثابتة (في الخلفية) كي تظهر القائمة فورًا. */
fun prefetchStaticEmojis(context: android.content.Context, emojis: List<String>) {
    val loader = coil.Coil.imageLoader(context)
    for (e in emojis) {
        val url = staticEmojiUrls(NotoEmojiMap.assetFor(e) ?: "").firstOrNull() ?: continue
        loader.enqueue(coil.request.ImageRequest.Builder(context).data(url).build())
    }
}

/**
 * تحكّم تشغيل تفاعلات الرسائل: يتحرك التفاعل مرة واحدة فقط عند اختياره (أو عند دخول الشاشة)،
 * ثم يبقى ثابتًا، ولا يتحرك ثانية إلا بالضغط عليه أو بالخروج والدخول مجددًا.
 */
object ReactionPlayRegistry {
    private val armed = java.util.concurrent.ConcurrentHashMap<String, Long>()
    fun arm(key: String) { armed[key] = System.currentTimeMillis() }

    /** true خلال 6 ثوانٍ من اختيار التفاعل (أو دخول الشاشة)؛ غير مُستهلَك فلا تضيع الحركة بسبب إعادة التركيب. */
    fun shouldPlay(key: String): Boolean {
        val t = armed[key] ?: return false
        return System.currentTimeMillis() - t < 6000L
    }
    fun consume(key: String): Boolean = shouldPlay(key)
}
