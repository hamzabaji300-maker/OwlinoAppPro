package com.example.ui

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** مدة حركة الحذف بالملّي ثانية (يستخدمها LabChatState قبل إزالة الرسالة فعليًا). */
const val DELETE_EFFECT_MS = 800

/**
 * حركة حذف الرسالة: تضبّب ثم تتفتت إلى قطع صغيرة تتطاير وتتلاشى (موجة من اليسار لليمين)،
 * وبعدها تنزلق باقي الرسائل لملء المكان عبر animateItem().
 * التضبيب يعمل على أندرويد 12+ فقط؛ التفتت والتلاشي يعملان على كل الإصدارات.
 */
fun Modifier.dissolveOnDelete(active: Boolean): Modifier = composed {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(active) {
        if (active) progress.animateTo(1f, tween(DELETE_EFFECT_MS, easing = LinearEasing))
        else progress.snapTo(0f)
    }
    val p = progress.value
    if (!active && p == 0f) {
        this
    } else {
        this
            .graphicsLayer {
                if (Build.VERSION.SDK_INT >= 31 && p > 0f) {
                    val r = (p * 14f * density).coerceAtLeast(0.01f)
                    renderEffect = BlurEffect(r, r, TileMode.Decal)
                }
            }
            .drawWithContent {
                if (p <= 0f) {
                    drawContent()
                    return@drawWithContent
                }
                val cols = 7
                val cw = size.width / cols
                val rows = max(1, min(8, (size.height / cw).roundToInt()))
                val ch = size.height / rows
                val canvas = drawContext.canvas
                val layerBounds = Rect(-size.width, -size.height, size.width * 2f, size.height * 2f)
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        val seed = ((r * 31 + c * 17) % 100) / 100f
                        val delay = (c.toFloat() / cols) * 0.45f + seed * 0.15f
                        val lp = ((p - delay) / 0.4f).coerceIn(0f, 1f)
                        val alpha = 1f - lp
                        if (alpha <= 0f) continue
                        val dx = lp * (10f + seed * 50f) * density
                        val dy = -lp * (20f + seed * 45f) * density
                        val cell = Rect(c * cw, r * ch, (c + 1) * cw, (r + 1) * ch)
                        val paint = Paint().apply { this.alpha = alpha }
                        canvas.saveLayer(layerBounds, paint)
                        translate(dx, dy) {
                            rotate(lp * (seed - 0.5f) * 50f, pivot = cell.center) {
                                clipRect(cell.left, cell.top, cell.right, cell.bottom) {
                                    this@drawWithContent.drawContent()
                                }
                            }
                        }
                        canvas.restore()
                    }
                }
            }
    }
}
