package com.owlino.plus

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.sin

/** Pictogrammes dessinés au Canvas (aucune image, aucun asset). Repère : [-1,1]. */
object Glyph {
    fun draw(c: Canvas, kind: Int, cx: Float, cy: Float, s: Float, bg: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = Color.WHITE
        p.strokeWidth = 0.16f
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        p.style = Paint.Style.STROKE
        c.save()
        c.translate(cx, cy)
        c.scale(s, s)
        when (kind) {
            0 -> {
                c.drawRoundRect(RectF(-0.5f, -0.3f, 0.5f, 0.7f), 0.1f, 0.1f, p)
                c.drawLine(-0.68f, -0.42f, 0.68f, -0.42f, p)
                c.drawLine(-0.2f, -0.65f, 0.2f, -0.65f, p)
            }
            1 -> {
                p.style = Paint.Style.FILL
                val path = Path()
                path.moveTo(-0.55f, 0.7f)
                path.lineTo(-0.55f, -0.1f)
                path.arcTo(RectF(-0.55f, -0.65f, 0.55f, 0.45f), 180f, 180f, false)
                path.lineTo(0.55f, 0.7f)
                path.lineTo(0.28f, 0.45f)
                path.lineTo(0f, 0.7f)
                path.lineTo(-0.28f, 0.45f)
                path.close()
                c.drawPath(path, p)
                p.color = bg
                c.drawCircle(-0.2f, -0.1f, 0.09f, p)
                c.drawCircle(0.2f, -0.1f, 0.09f, p)
            }
            2 -> {
                p.strokeWidth = 0.3f
                c.drawLine(-0.45f, 0.45f, 0.45f, -0.45f, p)
                p.strokeWidth = 0.16f
                c.drawLine(-0.7f, 0.72f, -0.25f, 0.72f, p)
            }
            3 -> {
                c.drawLine(-0.75f, 0f, -0.2f, 0f, p)
                c.drawLine(-0.4f, -0.2f, -0.2f, 0f, p)
                c.drawLine(-0.4f, 0.2f, -0.2f, 0f, p)
                c.drawLine(0f, -0.45f, 0.7f, -0.45f, p)
                c.drawLine(0.35f, -0.45f, 0.35f, 0.5f, p)
            }
            4 -> {
                c.drawCircle(0f, 0f, 0.6f, p)
                c.drawLine(0.42f, -0.42f, -0.42f, 0.42f, p)
            }
            5 -> {
                p.style = Paint.Style.FILL
                val path = Path()
                path.moveTo(0f, -0.7f)
                path.lineTo(0.6f, -0.45f)
                path.lineTo(0.6f, 0.05f)
                path.quadTo(0.6f, 0.5f, 0f, 0.75f)
                path.quadTo(-0.6f, 0.5f, -0.6f, 0.05f)
                path.lineTo(-0.6f, -0.45f)
                path.close()
                c.drawPath(path, p)
                p.style = Paint.Style.STROKE
                p.color = bg
                val ck = Path()
                ck.moveTo(-0.25f, 0f)
                ck.lineTo(-0.05f, 0.22f)
                ck.lineTo(0.28f, -0.2f)
                c.drawPath(ck, p)
            }
            6 -> {
                p.style = Paint.Style.FILL
                val path = Path()
                for (i in 0 until 10) {
                    val a = Math.toRadians((-90 + i * 36).toDouble())
                    val r = if (i % 2 == 0) 0.72f else 0.3f
                    val x = (cos(a) * r).toFloat()
                    val y = (sin(a) * r).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                c.drawPath(path, p)
            }
            7 -> {
                c.drawCircle(0f, -0.05f, 0.58f, p)
                val tail = Path()
                tail.moveTo(-0.35f, 0.45f)
                tail.lineTo(-0.5f, 0.75f)
                tail.lineTo(-0.1f, 0.58f)
                c.drawPath(tail, p)
                p.style = Paint.Style.FILL
                c.drawCircle(0f, -0.05f, 0.1f, p)
            }
            8 -> {
                p.style = Paint.Style.FILL
                c.drawCircle(0f, 0.1f, 0.52f, p)
                c.drawCircle(-0.4f, -0.4f, 0.22f, p)
                c.drawCircle(0.4f, -0.4f, 0.22f, p)
                p.color = bg
                c.drawCircle(-0.17f, 0.02f, 0.07f, p)
                c.drawCircle(0.17f, 0.02f, 0.07f, p)
            }
            else -> {
                c.drawCircle(0f, 0f, 0.62f, p)
                p.style = Paint.Style.FILL
                p.textAlign = Paint.Align.CENTER
                p.typeface = Typeface.DEFAULT_BOLD
                p.textSize = 1.0f
                c.drawText("?", 0f, 0.34f, p)
            }
        }
        c.restore()
    }
}

/** En-tête : ciel, planètes, fusée, nuages (animés) et flamme (animée). Repère interne : 591 unités de large. */
class HeaderView(c: Context) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private var t = 0f

    private val anim = ValueAnimator.ofFloat(0f, (2 * Math.PI).toFloat()).apply {
        duration = 8000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            t = it.animatedValue as Float
            invalidate()
        }
    }

    private val stars = arrayOf(
        floatArrayOf(170f, 230f, 6f), floatArrayOf(127f, 258f, 6f), floatArrayOf(212f, 293f, 7f),
        floatArrayOf(374f, 218f, 5f), floatArrayOf(435f, 246f, 5f), floatArrayOf(483f, 206f, 5f),
        floatArrayOf(210f, 208f, 4f)
    )
    private val cloudsA = arrayOf(
        floatArrayOf(30f, 340f, 45f), floatArrayOf(120f, 325f, 42f), floatArrayOf(190f, 335f, 38f),
        floatArrayOf(400f, 335f, 38f), floatArrayOf(470f, 320f, 44f), floatArrayOf(560f, 330f, 46f),
        floatArrayOf(640f, 335f, 40f)
    )
    private val cloudsB = arrayOf(
        floatArrayOf(-20f, 350f, 50f), floatArrayOf(75f, 345f, 40f), floatArrayOf(160f, 350f, 35f),
        floatArrayOf(430f, 350f, 35f), floatArrayOf(520f, 345f, 42f), floatArrayOf(610f, 350f, 45f)
    )

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        anim.start()
    }

    override fun onDetachedFromWindow() {
        anim.cancel()
        super.onDetachedFromWindow()
    }

    override fun onMeasure(w: Int, h: Int) {
        val ww = MeasureSpec.getSize(w)
        setMeasuredDimension(ww, (620f * ww / 591f).toInt())
    }

    override fun onDraw(cv: Canvas) {
        cv.save()
        val k = width / 591f
        cv.scale(k, k)

        // Ciel
        p.shader = LinearGradient(0f, 0f, 0f, 340f, 0xFF5C4BC8.toInt(), 0xFF4E3EB0.toInt(), Shader.TileMode.CLAMP)
        cv.drawRect(0f, 0f, 591f, 345f, p)
        p.shader = null

        // Planètes
        p.color = 0xFF5445B5.toInt()
        cv.drawCircle(0f, 170f, 32f, p)
        p.color = 0xFF8472E8.toInt()
        cv.drawCircle(40f, 253f, 58f, p)
        p.shader = LinearGradient(502f, 160f, 570f, 228f, 0xFF4D63F0.toInt(), 0xFF2FD0C0.toInt(), Shader.TileMode.CLAMP)
        cv.drawCircle(536f, 194f, 34f, p)
        p.shader = null
        p.color = 0xFF3DBE8A.toInt()
        cv.drawCircle(554f, 266f, 12f, p)

        // Étoiles
        p.color = Color.WHITE
        p.alpha = 150
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f
        for (s in stars) {
            cv.drawLine(s[0] - s[2], s[1], s[0] + s[2], s[1], p)
            cv.drawLine(s[0], s[1] - s[2], s[0], s[1] + s[2], p)
        }
        p.style = Paint.Style.FILL
        p.alpha = 255

        // Nuages (mouvement léger)
        p.color = Color.WHITE
        val da = 14f * sin(t)
        val db = -12f * sin(t + 1f)
        val dy = 2f * sin(2f * t)
        for (cl in cloudsA) cv.drawCircle(cl[0] + da, cl[1] + dy, cl[2], p)
        for (cl in cloudsB) cv.drawCircle(cl[0] + db, cl[1] - dy, cl[2], p)
        p.shader = LinearGradient(
            0f, 340f, 0f, 610f,
            intArrayOf(Color.WHITE, Color.WHITE, Color.BLACK), floatArrayOf(0f, 0.3f, 1f), Shader.TileMode.CLAMP
        )
        cv.drawRect(-10f, 340f, 601f, 620f, p)
        p.shader = null

        // Fusée (fixe)
        val cx = 296f
        val finL = Path().apply {
            moveTo(278f, 268f); lineTo(262f, 288f); lineTo(262f, 304f); lineTo(278f, 296f); close()
        }
        val finR = Path().apply {
            moveTo(314f, 268f); lineTo(330f, 288f); lineTo(330f, 304f); lineTo(314f, 296f); close()
        }
        p.color = 0xFF4AA5A8.toInt()
        cv.drawPath(finL, p)
        cv.drawPath(finR, p)
        val body = Path().apply {
            moveTo(cx, 208f)
            cubicTo(322f, 230f, 322f, 270f, 316f, 298f)
            lineTo(276f, 298f)
            cubicTo(270f, 270f, 270f, 230f, cx, 208f)
            close()
        }
        p.color = 0xFFEAEAF0.toInt()
        cv.drawPath(body, p)
        p.color = 0xFF7F8CB8.toInt()
        cv.drawCircle(cx, 252f, 15f, p)
        p.color = 0xFF3AA0E0.toInt()
        cv.drawCircle(cx, 252f, 11f, p)
        p.color = 0x99FFFFFF.toInt()
        cv.drawCircle(cx - 3f, 248f, 3.5f, p)
        p.color = 0xFFB0B4C8.toInt()
        cv.drawRect(285f, 298f, 307f, 305f, p)

        // Flamme (animée)
        val f = 1f + 0.15f * sin(t * 48f) + 0.08f * sin(t * 71f)
        val len = 30f * f
        val flame = Path().apply {
            moveTo(284f, 305f)
            cubicTo(284f, 320f, 292f, 305f + len * 0.9f, cx, 305f + len)
            cubicTo(300f, 305f + len * 0.9f, 308f, 320f, 308f, 305f)
            close()
        }
        p.color = 0xFFF5A623.toInt()
        cv.drawPath(flame, p)
        val inner = Path().apply {
            moveTo(290f, 305f)
            cubicTo(290f, 314f, 294f, 305f + len * 0.55f, cx, 305f + len * 0.6f)
            cubicTo(298f, 305f + len * 0.55f, 302f, 314f, 302f, 305f)
            close()
        }
        p.color = 0xFFFFE27A.toInt()
        cv.drawPath(inner, p)

        cv.restore()
    }
}

/** Logo « Owlino [Plus] ». */
class LogoView(c: Context) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(cv: Canvas) {
        cv.save()
        val k = width / 591f
        cv.scale(k, k)
        p.typeface = Typeface.DEFAULT_BOLD
        p.textSize = 58f
        p.color = Color.WHITE
        val w1 = p.measureText("Owlino")
        val w2 = p.measureText("Plus")
        val pill = w2 + 34f
        val x0 = (591f - (w1 + 12f + pill)) / 2f
        cv.drawText("Owlino", x0, 80f, p)
        cv.drawRoundRect(RectF(x0 + w1 + 12f, 33f, x0 + w1 + 12f + pill, 87f), 12f, 12f, p)
        p.color = 0xFF5B49C6.toInt()
        cv.drawText("Plus", x0 + w1 + 12f + 17f, 80f, p)
        cv.restore()
    }
}

/** Flèche : dir 0 = droite, 1 = gauche, 2 = bas. */
class ChevronView(c: Context, private val k: Float, private val dir: Int, private val color: Int) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(cv: Canvas) {
        p.style = Paint.Style.STROKE
        p.color = color
        p.strokeWidth = 4.5f * k
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        val x = width / 2f
        val y = height / 2f
        val s = 11f * k
        val path = Path()
        when (dir) {
            0 -> { path.moveTo(x - s * 0.5f, y - s); path.lineTo(x + s * 0.5f, y); path.lineTo(x - s * 0.5f, y + s) }
            1 -> { path.moveTo(x + s * 0.5f, y - s); path.lineTo(x - s * 0.5f, y); path.lineTo(x + s * 0.5f, y + s) }
            else -> { path.moveTo(x - s * 1.6f, y - s * 0.5f); path.lineTo(x, y + s * 0.5f); path.lineTo(x + s * 1.6f, y - s * 0.5f) }
        }
        cv.drawPath(path, p)
    }
}

class RadioView(c: Context, private val k: Float) : View(c) {
    var sel = false
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(cv: Canvas) {
        val x = width / 2f
        val y = height / 2f
        p.style = Paint.Style.STROKE
        p.strokeWidth = 3.5f * k
        p.color = if (sel) 0xFF6C5CE7.toInt() else 0xFFD0D2D6.toInt()
        cv.drawCircle(x, y, 14f * k, p)
        if (sel) {
            p.style = Paint.Style.FILL
            cv.drawCircle(x, y, 8f * k, p)
        }
    }
}

/** Pastille ronde colorée + pictogramme. */
class IconView(c: Context, private val kind: Int, private val color: Int) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(cv: Canvas) {
        val r = width / 2f
        p.color = color
        cv.drawCircle(r, height / 2f, r, p)
        Glyph.draw(cv, kind, r, height / 2f, width * 0.26f, color)
    }
}

class DotsView(c: Context, private val k: Float, private val count: Int) : View(c) {
    var active = 0
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(cv: Canvas) {
        val gap = 27f * k
        val x0 = (width - gap * (count - 1)) / 2f
        for (i in 0 until count) {
            p.color = if (i == active) Color.WHITE else 0xFF55585E.toInt()
            cv.drawCircle(x0 + i * gap, height / 2f, 6f * k, p)
        }
    }
}

/** Grande illustration du bottom sheet. */
class BigIconView(c: Context, private val k: Float) : View(c) {
    var kind = 0
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    private fun sparkle(cv: Canvas, x: Float, y: Float, s: Float) {
        val path = Path()
        path.moveTo(x, y - s)
        path.quadTo(x, y, x + s, y)
        path.quadTo(x, y, x, y + s)
        path.quadTo(x, y, x - s, y)
        path.quadTo(x, y, x, y - s)
        cv.drawPath(path, p)
    }

    override fun onDraw(cv: Canvas) {
        val cx = width / 2f
        val cy = 180f * k
        val r = 103f * k
        p.shader = LinearGradient(
            cx + r * 0.7f, cy - r * 0.7f, cx - r * 0.7f, cy + r * 0.7f,
            0xFF18D6CF.toInt(), 0xFF5D66F2.toInt(), Shader.TileMode.CLAMP
        )
        cv.drawCircle(cx, cy, r, p)
        p.shader = null
        Glyph.draw(cv, kind, cx, cy, 48f * k, 0xFF3BA0E0.toInt())
        p.color = 0xFF3FC8F0.toInt()
        sparkle(cv, cx - 92f * k, cy - 84f * k, 14f * k)
        sparkle(cv, cx - 107f * k, cy - 68f * k, 6f * k)
        sparkle(cv, cx + 112f * k, cy - 42f * k, 8f * k)
    }
}
