package com.example.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** نوع التوثيق: لا شيء / أزرق (أشخاص) / أحمر (شركات وحسابات رسمية). */
enum class VerifyType {
    NONE, BLUE, RED;

    /** القيمة كما تُخزَّن في قاعدة البيانات (عمود verified_type). */
    val raw: String
        get() = when (this) {
            NONE -> "none"
            BLUE -> "blue"
            RED -> "red"
        }

    val isVerified: Boolean get() = this != NONE

    companion object {
        /**
         * يحوّل القيمة الخام إلى النوع.
         * للتوافق مع النسخ القديمة: إن لم توجد قيمة verified_type (أو كانت none)
         * وكان is_verified = true فالنوع أزرق.
         */
        fun from(raw: String?, legacyVerified: Boolean? = false): VerifyType {
            return when (raw?.trim()?.lowercase()) {
                "blue" -> BLUE
                "red" -> RED
                else -> if (legacyVerified == true) BLUE else NONE
            }
        }
    }
}

object VerifiedColors {
    val Blue = Color(0xFF3B82F6)
    val Red = Color(0xFFEF4444)
}

/** الشارة الجديدة: تعتمد على نوع التوثيق. */
@Composable
fun VerifiedBadge(
    type: VerifyType,
    modifier: Modifier = Modifier,
    iconSize: Dp = 18.dp,
    yOffset: Dp = (-1).dp
) {
    if (type == VerifyType.NONE) return

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f),
        label = "VerifiedBadgeScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(stiffness = 400f),
        label = "VerifiedBadgeAlpha"
    )

    val description = if (type == VerifyType.RED) "حساب تجاري موثّق" else "حساب موثّق"

    Box(
        modifier = modifier
            .offset(y = yOffset)
            .scale(scale)
            .alpha(alpha)
            .requiredSize(iconSize)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        when (type) {
            VerifyType.BLUE -> {
                // دائرة بيضاء صغيرة تغطي علامة الصح فقط (حتى لا يتسرب الأبيض عند الحواف)
                Box(
                    modifier = Modifier
                        .requiredSize(iconSize * 0.45f)
                        .background(Color.White, CircleShape)
                )
                Icon(
                    imageVector = Icons.Filled.Verified,
                    contentDescription = null,
                    tint = VerifiedColors.Blue,
                    modifier = Modifier.fillMaxSize()
                )
            }
            VerifyType.RED -> {
                // شكل مختلف عن الأزرق (ختم مموّج مع رمز مبنى بدل علامة الصح)
                // حتى يُميَّز بدون الاعتماد على اللون وحده
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val baseR = size.minDimension / 2f * 0.93f
                    val bumps = 8
                    val path = Path()
                    val steps = 96
                    for (i in 0..steps) {
                        val a = (2.0 * PI * i / steps).toFloat()
                        val r = baseR * (1f + 0.07f * cos(bumps * a)) / 1.07f
                        val x = cx + r * cos(a)
                        val y = cy + r * sin(a)
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    path.close()
                    drawPath(path, VerifiedColors.Red)
                }
                Icon(
                    imageVector = Icons.Filled.Business,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.requiredSize(iconSize * 0.58f)
                )
            }
            VerifyType.NONE -> Unit
        }
    }
}

/**
 * النسخة القديمة (للتوافق): true = أزرق.
 * إن مُرّر لون أحمر في tint (الاستعمال القديم للقناة الرسمية) تُرسم الشارة الحمراء.
 */
@Composable
fun VerifiedBadge(
    isVerified: Boolean,
    modifier: Modifier = Modifier,
    iconSize: Dp = 18.dp,
    yOffset: Dp = (-1).dp,
    tint: Color = VerifiedColors.Blue
) {
    if (!isVerified) return
    VerifiedBadge(
        type = if (tint == VerifiedColors.Red) VerifyType.RED else VerifyType.BLUE,
        modifier = modifier,
        iconSize = iconSize,
        yOffset = yOffset
    )
}
