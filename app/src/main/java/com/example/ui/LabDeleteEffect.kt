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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.Bitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.unit.IntSize
import androidx.compose.runtime.withFrameNanos
import java.util.Random
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/** مدة حركة الحذف بالملّي ثانية (يستخدمها LabChatState قبل إزالة الرسالة فعليًا). */
const val DELETE_EFFECT_MS = 1300

private class DustParticles(
    val n: Int,
    val xs: FloatArray,
    val ys: FloatArray,
    val argb: IntArray,
    val seedA: FloatArray,
    val seedB: FloatArray,
    val step: Float,
    val width: Float
)

/**
 * يحوّل صورة الفقاعة إلى آلاف الحبيبات الصغيرة (كل حبيبة بلون بكسل حقيقي).
 * ملاحظة مهمة: toImageBitmap() يعيد Bitmap من نوع HARDWARE على الأجهزة الحقيقية ولا يمكن قراءة بكسلاته مباشرة
 * (كان هذا سبب فشل الغبار والرجوع للتلاشي)، لذلك ننسخه أولًا إلى ARGB_8888.
 */
private fun buildDust(image: ImageBitmap): DustParticles? {
    var bmp = image.asAndroidBitmap()
    if (bmp.config == Bitmap.Config.HARDWARE) {
        bmp = bmp.copy(Bitmap.Config.ARGB_8888, false) ?: return null
    }
    val w = bmp.width
    val h = bmp.height
    if (w <= 0 || h <= 0) return null
    val pixels = IntArray(w * h)
    bmp.getPixels(pixels, 0, w, 0, 0, w, h)
    var opaque = 0
    for (c in pixels) if ((c ushr 24) > 30) opaque++
    if (opaque == 0) return null
    // حبيبات دقيقة: العدد ≈ 4500 داخل مساحة الفقاعة الفعلية فقط
    val step = max(2, ceil(sqrt(opaque / 4500.0)).toInt())
    val rnd = Random(7)
    val xs = ArrayList<Float>(); val ys = ArrayList<Float>()
    val cols = ArrayList<Int>(); val sa = ArrayList<Float>(); val sb = ArrayList<Float>()
    var y = 0
    while (y < h) {
        var x = 0
        while (x < w) {
            val c = pixels[(y + step / 2).coerceAtMost(h - 1) * w + (x + step / 2).coerceAtMost(w - 1)]
            if ((c ushr 24) > 30) {
                xs.add(x.toFloat()); ys.add(y.toFloat()); cols.add(c)
                sa.add(rnd.nextFloat()); sb.add(rnd.nextFloat())
            }
            x += step
        }
        y += step
    }
    if (xs.isEmpty()) return null
    return DustParticles(
        xs.size, xs.toFloatArray(), ys.toFloatArray(), cols.toIntArray(),
        sa.toFloatArray(), sb.toFloatArray(), step.toFloat(), w.toFloat()
    )
}

/**
 * حركة حذف الرسالة: تبقى الفقاعة كاملة ثم تتحوّل إلى غبار ناعم يتطاير للأعلى ويتلاشى
 * (موجة من اليسار لليمين)، بعدها تنزلق باقي الرسائل لملء المكان.
 * إذا فشل التقاط الصورة لأي سبب تتلاشى الفقاعة تدريجيًا بدل ذلك.
 */
fun Modifier.dissolveOnDelete(active: Boolean): Modifier = composed {
    val layer = rememberGraphicsLayer()
    val progress = remember { Animatable(0f) }
    var dust by remember { mutableStateOf<DustParticles?>(null) }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(active) {
        if (active) {
            // نترك إطارين ليُسجَّل المحتوى داخل الطبقة قبل التقاطه
            withFrameNanos { }
            withFrameNanos { }
            try {
                dust = buildDust(layer.toImageBitmap())
                failed = dust == null
            } catch (e: Throwable) {
                failed = true
            }
            progress.snapTo(0f)
            progress.animateTo(1f, tween(DELETE_EFFECT_MS, easing = LinearEasing))
        } else {
            progress.snapTo(0f)
            dust = null
            failed = false
        }
    }

    this.drawWithContent {
        if (!active) {
            drawContent()
            return@drawWithContent
        }
        val ps = dust
        if (ps == null) {
            // لحظة الالتقاط (أو الفشل): نرسم المحتوى عبر الطبقة
            layer.record(this, layoutDirection, IntSize(size.width.toInt(), size.height.toInt())) { this@drawWithContent.drawContent() }
            layer.alpha = if (failed) 1f - progress.value else 1f
            drawLayer(layer)
            return@drawWithContent
        }
        val p = progress.value
        val d = density
        val w = ps.width
        for (i in 0 until ps.n) {
            val sa = ps.seedA[i]
            val sb = ps.seedB[i]
            val delay = (ps.xs[i] / w) * 0.35f + sa * 0.15f
            val lp = ((p - delay) / 0.5f).coerceIn(0f, 1f)
            if (lp >= 1f) continue
            val base = Color(ps.argb[i])
            if (lp <= 0f) {
                drawRect(base, Offset(ps.xs[i], ps.ys[i]), Size(ps.step, ps.step))
            } else {
                val a = (1f - lp) * (1f - lp)
                val dx = lp * (25f + sa * 90f) * d + sin(lp * 9f + sb * 12f) * 6f * d * lp
                val dy = -lp * (15f + sb * 80f) * d + sin(lp * 7f + sa * 10f) * 4f * d * lp
                val r = ps.step * (0.55f - 0.35f * lp) * (0.6f + sb * 0.8f)
                drawCircle(
                    color = base.copy(alpha = base.alpha * a),
                    radius = r,
                    center = Offset(ps.xs[i] + ps.step / 2f + dx, ps.ys[i] + ps.step / 2f + dy)
                )
            }
        }
    }
}
