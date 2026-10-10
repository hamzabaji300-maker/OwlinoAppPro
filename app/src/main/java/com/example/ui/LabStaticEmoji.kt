package com.example.ui

import androidx.compose.foundation.layout.Box
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
