package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/** مدة حركة الحذف بالملّي ثانية (يستخدمها LabChatState قبل إزالة الرسالة فعليًا). */
const val DELETE_EFFECT_MS = 1400

/** (للتشخيص فقط) لم يعد التفتيت يعتمد على التقاط صورة، لكن أبقيت الواجهة لتوافق بقية الملفات. */
object DustDebug {
    @Volatile var onError: ((String) -> Unit)? = null
}

/** حدود فقاعة كل رسالة (بإحداثيات الجذر)، تسجّلها MessageBubble ويقرؤها تأثير الغبار. */
object DustRegistry {
    val bubbleBounds = HashMap<String, Rect>()
}

private fun cellRand(a: Int, b: Int, salt: Int): Float {
    var x = a * 374761393 + b * 668265263 + salt * 1442695041
    x = (x xor (x ushr 13)) * 1274126177
    x = x xor (x ushr 16)
    return (x and 0xFFFFFF) / 16777215f
}

/**
 * تحويل الفقاعة إلى رمل — بدون أي التقاط صور (التقاط الطبقة كان يعيد صورة فارغة على بعض الأجهزة):
 *  1) نرسم محتوى الفقاعة الحقيقي مرة واحدة داخل طبقة.
 *  2) نمحو منها خلايا صغيرة جدًا (BlendMode.Clear) بموجة من اليسار لليمين.
 *  3) في مكان كل خلية ممحوة نرسم حبيبة رمل بلون الفقاعة/النص تتطاير للأعلى وتتلاشى.
 *  4) في آخر الحركة يتلاشى أي متبقٍ (مثل صف التفاعلات) ثم تُزال الرسالة وتنزلق الباقية.
 */
fun Modifier.dissolveOnDelete(active: Boolean, messageId: String, bubbleColor: Color, textColor: Color): Modifier = composed {
    val progress = remember { Animatable(0f) }
    var rowOrigin by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(active) {
        if (active) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(DELETE_EFFECT_MS, easing = LinearEasing))
        } else {
            progress.snapTo(0f)
        }
    }

    this
        .onGloballyPositioned { rowOrigin = it.positionInRoot() }
        .drawWithContent {
            if (!active) {
                drawContent()
                return@drawWithContent
            }
            val p = progress.value
            val d = density

            // منطقة الفقاعة داخل الصف (إن لم تُعرف نستخدم الصف كله)
            var left = 0f
            var top = 0f
            var right = size.width
            var bottom = size.height
            val rb = DustRegistry.bubbleBounds[messageId]
            if (rb != null) {
                left = (rb.left - rowOrigin.x).coerceIn(0f, size.width)
                top = (rb.top - rowOrigin.y).coerceIn(0f, size.height)
                right = (rb.right - rowOrigin.x).coerceIn(0f, size.width)
                bottom = (rb.bottom - rowOrigin.y).coerceIn(0f, size.height)
            }
            val rw = right - left
            val rh = bottom - top
            if (rw < 2f || rh < 2f) {
                drawContent()
                return@drawWithContent
            }
            val step = max(3f * d, sqrt(rw * rh / 3200f))
            val cols = ceil(rw / step).toInt().coerceAtLeast(1)
            val rows = ceil(rh / step).toInt().coerceAtLeast(1)
            val fade = if (p < 0.55f) 1f else (1f - (p - 0.55f) / 0.45f).coerceIn(0f, 1f)

            // 1+2) المحتوى الحقيقي مع محو الخلايا التي بدأت تتحول إلى رمل
            drawIntoCanvas { canvas ->
                val paint = Paint().apply { alpha = fade }
                canvas.saveLayer(Rect(-size.width, -size.height, size.width * 2f, size.height * 2f), paint)
                this@drawWithContent.drawContent()
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        val delay = (c.toFloat() / cols) * 0.35f + cellRand(r, c, 1) * 0.15f
                        val lp = ((p - delay) / 0.5f).coerceIn(0f, 1f)
                        if (lp > 0f) {
                            drawRect(
                                color = Color.Black,
                                topLeft = Offset(left + c * step, top + r * step),
                                size = Size(step + 1f, step + 1f),
                                blendMode = BlendMode.Clear
                            )
                        }
                    }
                }
                canvas.restore()
            }

            // 3) حبيبات الرمل
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val delay = (c.toFloat() / cols) * 0.35f + cellRand(r, c, 1) * 0.15f
                    val lp = ((p - delay) / 0.5f).coerceIn(0f, 1f)
                    if (lp <= 0f || lp >= 1f) continue
                    val sa = cellRand(r, c, 2)
                    val sb = cellRand(r, c, 3)
                    val a = (1f - lp) * (1f - lp)
                    val dx = lp * (25f + sa * 90f) * d + sin(lp * 9f + sb * 12f) * 6f * d * lp
                    val dy = -lp * (15f + sb * 80f) * d + sin(lp * 7f + sa * 10f) * 4f * d * lp
                    val base = if (cellRand(r, c, 4) < 0.28f) textColor else bubbleColor
                    val radius = step * (0.55f - 0.35f * lp) * (0.6f + sb * 0.8f)
                    drawCircle(
                        color = base.copy(alpha = base.alpha * a),
                        radius = radius,
                        center = Offset(left + c * step + step / 2f + dx, top + r * step + step / 2f + dy)
                    )
                }
            }
        }
}
