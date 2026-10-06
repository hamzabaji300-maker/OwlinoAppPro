package com.example.ui.plusui

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/**
 * Plumette d'oreiller (duvet) douce et duveteuse.
 * Générée une seule fois au Canvas dans un petit Bitmap (aucun fichier image externe),
 * puis dessinée à chaque image avec rotation / échelle : très léger pour le GPU.
 */
object DownFeather {
    const val W = 208
    const val H = 128

    val bitmap: Bitmap by lazy { build() }

    // Tige courbe (croissant) : P0 = pointe du calamus, P2 = bout duveteux
    private const val P0X = 0.83f
    private const val P0Y = -0.30f
    private const val CX = 0.34f
    private const val CY = 0.84f
    private const val P2X = -0.83f
    private const val P2Y = -0.10f

    private fun hh(i: Int): Float {
        val s = sin(i * 12.9898) * 43758.5453
        return (s - floor(s)).toFloat()
    }

    private fun bx(t: Float): Float {
        val a = (1 - t) * (1 - t); val b = 2 * (1 - t) * t; val c = t * t
        return a * P0X + b * CX + c * P2X
    }

    private fun by(t: Float): Float {
        val a = (1 - t) * (1 - t); val b = 2 * (1 - t) * t; val c = t * t
        return a * P0Y + b * CY + c * P2Y
    }

    /** Tangente normalisée en t. */
    private fun tg(t: Float): FloatArray {
        val x = 2 * (1 - t) * (CX - P0X) + 2 * t * (P2X - CX)
        val y = 2 * (1 - t) * (CY - P0Y) + 2 * t * (P2Y - CY)
        val n = hypot(x, y)
        return floatArrayOf(x / n, y / n)
    }

    private fun build(): Bitmap {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        bmp.setHasMipMap(true)
        val cv = Canvas(bmp)
        val s = W / 2.9f
        val ox = W * 0.52f
        val oy = H * 0.40f

        // Barbes duveteuses des deux côtés de la tige
        val barbs = Path()
        val n = 64
        for (sd in intArrayOf(1, -1)) {
            for (i in 0 until n) {
                val t = min(1f, 0.05f + 0.95f * (i + 0.8f * hh(i * 7 + sd * 3)) / n)
                val rx = bx(t)
                val ry = by(t)
                val tv = tg(t)
                var ln = (0.13f + 0.30f * Math.pow(t.toDouble(), 1.2).toFloat()) *
                    (0.7f + 0.6f * hh(i * 13 + sd * 5)) * (if (sd > 0) 1f else 0.85f)
                if (t > 0.85f) ln *= 1f + 0.8f * (t - 0.85f) / 0.15f * hh(i * 5 + sd)
                val a = Math.toRadians(((42f - 24f * t) * (0.7f + 0.6f * hh(i * 11 + sd * 2))).toDouble()).toFloat() * sd
                val ca = cos(a)
                val sa = sin(a)
                val dx = tv[0] * ca - tv[1] * sa
                val dy = tv[0] * sa + tv[1] * ca
                val nx = -dy
                val ny = dx
                val bend = (hh(i * 17 + sd) - 0.5f) * 0.35f
                val ex = rx + dx * ln
                val ey = ry + dy * ln
                val qx = rx + dx * ln * 0.55f + nx * ln * bend
                val qy = ry + dy * ln * 0.55f + ny * ln * bend
                barbs.moveTo(ox + rx * s, oy + ry * s)
                barbs.quadTo(ox + qx * s, oy + qy * s, ox + ex * s, oy + ey * s)
            }
        }

        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.color = 0xFFFFFCF4.toInt()

        // 4 couches : halo doux -> duvet -> barbes fines
        val layers = arrayOf(
            floatArrayOf(18f, 80f, 13f),
            floatArrayOf(7f, 105f, 4.5f),
            floatArrayOf(3.0f, 140f, 1.3f),
            floatArrayOf(1.2f, 215f, 0f)
        )
        for (l in layers) {
            p.strokeWidth = l[0]
            p.color = 0xFFFFFCF4.toInt()
            p.alpha = l[1].toInt()
            p.maskFilter = if (l[2] > 0f) BlurMaskFilter(l[2], BlurMaskFilter.Blur.NORMAL) else null
            cv.drawPath(barbs, p)
        }
        p.maskFilter = null

        // Tige très fine + petit calamus
        val rachis = Path()
        for (k in 0..40) {
            val t = k / 40f
            val x = ox + bx(t) * s
            val y = oy + by(t) * s
            if (k == 0) rachis.moveTo(x, y) else rachis.lineTo(x, y)
        }
        p.strokeWidth = 0.9f
        p.color = 0xFFFFFFFF.toInt()
        p.alpha = 120
        cv.drawPath(rachis, p)

        val t0 = tg(0f)
        p.strokeWidth = 1.8f
        p.alpha = 230
        cv.drawLine(
            ox + P0X * s, oy + P0Y * s,
            ox + (P0X - t0[0] * 0.14f) * s, oy + (P0Y - t0[1] * 0.14f) * s, p
        )
        return bmp
    }
}
