package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
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
fun ReactionChip(reactions: List<String>, modifier: Modifier = Modifier) {
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
            if (url != null) LottieEmojiReaction(url = url, size = 20.dp) else Text(shown, fontSize = 15.sp)
        }
        if (reactions.size > 1) {
            Spacer(Modifier.width(3.dp))
            Text("${reactions.size}", fontSize = 12.sp, color = theme.textSecondary, fontWeight = FontWeight.Medium)
        }
    }
}
