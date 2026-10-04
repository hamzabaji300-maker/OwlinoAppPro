package com.owlino.plus

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.view.animation.LinearInterpolator
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
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

/** En-tête : ciel, plumettes duveteuses, baleine qui saute hors des nuages (animé), nuages (animés). Repère interne : 591 unités. */
class HeaderView(c: Context, private val bgEnd: Int, private val dark: Boolean = false) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private var t = 0f

    private val anim = ValueAnimator.ofFloat(0f, (2 * Math.PI).toFloat()).apply {
        duration = 10000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            t = it.animatedValue as Float
            invalidate()
        }
    }

    // Plumes : [x0, longueur, cycles/10s, phase chute, phase balancement, amplitude, alpha max, cycles balancement]
    private val feathers: Array<FloatArray> = Array(20) { i ->
        val kind = i % 3 // 0 = grande/lente, 1 = moyenne, 2 = petite/rapide
        val len = when (kind) { 0 -> 40f; 1 -> 27f; else -> 16f }
        val cycles = if (kind == 2) 2f else 1f
        val alpha = when (kind) { 0 -> 0.9f; 1 -> 0.7f; else -> 0.5f }
        val amp = when (kind) { 0 -> 26f; 1 -> 20f; else -> 14f }
        floatArrayOf(
            (i * 137.5f + 23f) % 591f, len, cycles,
            ((i * 0.381966f) + i * 0.013f) % 1f, (i * 1.7f) % 6.2831f, amp, alpha,
            (2 + (i % 3)).toFloat()
        )
    }
    private val featherPath = Path().apply {
        // Plume le long de l'axe Y : pointe en haut (0,-1), base en bas (0,1)
        moveTo(0f, -1f)
        quadTo(0.55f, -0.45f, 0.2f, 0.55f)
        quadTo(0.1f, 0.85f, 0f, 1f)
        quadTo(-0.1f, 0.85f, -0.2f, 0.55f)
        quadTo(-0.55f, -0.45f, 0f, -1f)
        close()
    }
    private val featherRib = Path().apply {
        moveTo(0f, -0.9f)
        lineTo(0f, 1.15f)
    }

    private fun drawFeathers(cv: Canvas) {
        val tt = t / (2f * Math.PI.toFloat())
        for (f in feathers) {
            val prog = ((tt * f[2] + f[3]) % 1f + 1f) % 1f
            val y = -30f + 375f * prog
            val fadeIn = min(1f, prog / 0.08f)
            val fadeOut = if (prog > 0.65f) 1f - (prog - 0.65f) / 0.35f else 1f
            val a = f[6] * fadeIn * fadeOut
            if (a <= 0.01f) continue
            val ph = f[4]
            val x = f[0] + f[5] * sin(f[7] * t + ph)
            val ang = 25f * (f[0] % 7f - 3f) / 3f + 55f * sin(f[7] * t + ph + 1.2f) + 360f * prog * (if (f[2] > 1f) 1f else 0.5f)
            cv.save()
            cv.translate(x, y)
            cv.rotate(ang)
            cv.scale(f[1] * 0.4f, f[1])
            p.style = Paint.Style.FILL
            p.color = 0xFFFFF6E4.toInt()
            p.alpha = (a * 200f).toInt()
            cv.drawPath(featherPath, p)
            p.style = Paint.Style.STROKE
            p.strokeWidth = 0.05f
            p.color = Color.WHITE
            p.alpha = (a * 230f).toInt()
            cv.drawPath(featherRib, p)
            cv.restore()
        }
        p.style = Paint.Style.FILL
        p.alpha = 255
    }
    // Plumettes d'oreiller (duvet) : [x0, largeur, cycles/10s, phase chute, phase balancement, amplitude, alpha max, cycles balancement]
    private val downs: Array<FloatArray> = Array(18) { i ->
        val kind = i % 3 // 0 = plus grande/lente, 1 = moyenne, 2 = petite/plus rapide
        val wd = when (kind) { 0 -> 92f; 1 -> 72f; else -> 54f }
        val cycles = if (kind == 2) 2f else 1f
        val alpha = when (kind) { 0 -> 0.95f; 1 -> 0.85f; else -> 0.7f }
        val amp = when (kind) { 0 -> 22f; 1 -> 18f; else -> 12f }
        floatArrayOf(
            (i * 137.5f + 23f) % 591f, wd, cycles,
            ((i * 0.381966f) + i * 0.013f) % 1f, (i * 1.7f) % 6.2831f, amp, alpha,
            (2 + (i % 3)).toFloat()
        )
    }
    private val dp = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    init { DownFeather.bitmap } // génère la plumette une seule fois

    private fun drawDowns(cv: Canvas) {
        val tt = t / (2f * Math.PI.toFloat())
        val bmp = DownFeather.bitmap
        val bw = DownFeather.W.toFloat()
        val bh = DownFeather.H.toFloat()
        for (f in downs) {
            val prog = ((tt * f[2] + f[3]) % 1f + 1f) % 1f
            val y = -30f + 375f * prog
            val fadeIn = min(1f, prog / 0.08f)
            val fadeOut = if (prog > 0.65f) 1f - (prog - 0.65f) / 0.35f else 1f
            val a = f[6] * fadeIn * fadeOut
            if (a <= 0.01f) continue
            val ph = f[4]
            val x = f[0] + f[5] * sin(f[7] * t + ph)
            val ang = 18f * (f[0] % 7f - 3f) / 3f + 40f * sin(f[7] * t + ph + 1.2f) + 70f * prog * f[2]
            val sc = f[1] / bw
            val sy = 0.7f + 0.3f * cos(2f * t + ph * 1.3f) // léger effet de retournement
            cv.save()
            cv.translate(x, y)
            cv.rotate(ang)
            cv.scale(sc, sc * sy)
            dp.alpha = (a * 255f).toInt()
            cv.drawBitmap(bmp, -bw / 2f, -bh / 2f, dp)
            cv.restore()
        }
    }
    private val cloudsA = arrayOf(
        floatArrayOf(30f, 340f, 45f), floatArrayOf(120f, 325f, 42f), floatArrayOf(190f, 335f, 38f),
        floatArrayOf(400f, 335f, 38f), floatArrayOf(470f, 320f, 44f), floatArrayOf(560f, 330f, 46f),
        floatArrayOf(640f, 335f, 40f)
    )
    private val cloudsB = arrayOf(
        floatArrayOf(-20f, 350f, 50f), floatArrayOf(75f, 345f, 40f), floatArrayOf(160f, 350f, 35f),
        floatArrayOf(430f, 350f, 35f), floatArrayOf(520f, 345f, 42f), floatArrayOf(610f, 350f, 45f)
    )
    private val cloudsC = arrayOf(
        floatArrayOf(250f, 352f, 38f), floatArrayOf(296f, 345f, 46f), floatArrayOf(342f, 352f, 38f)
    )

    // Baleine (tournée vers la droite, centrée sur 0,0)
    private val body = Path().apply {
        moveTo(80f, 5f)
        cubicTo(78f, -25f, 40f, -38f, 0f, -34f)
        cubicTo(-30f, -30f, -55f, -15f, -70f, -10f)
        lineTo(-88f, -30f)
        quadTo(-84f, -12f, -78f, -4f)
        quadTo(-84f, 10f, -90f, 22f)
        lineTo(-70f, 8f)
        cubicTo(-45f, 25f, -10f, 34f, 25f, 30f)
        cubicTo(60f, 26f, 82f, 22f, 80f, 5f)
        close()
    }
    private val belly = Path().apply {
        moveTo(80f, 5f)
        cubicTo(82f, 22f, 60f, 26f, 25f, 30f)
        cubicTo(-10f, 34f, -45f, 25f, -70f, 8f)
        cubicTo(-40f, 14f, 0f, 10f, 30f, 12f)
        cubicTo(55f, 12f, 72f, 10f, 80f, 5f)
        close()
    }
    private val fin = Path().apply {
        moveTo(20f, 16f)
        quadTo(5f, 42f, -12f, 38f)
        quadTo(8f, 26f, 12f, 14f)
        close()
    }

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

    private fun drawWhale(cv: Canvas, u: Float) {
        if (u >= 0.8f) return // cachée dans les nuages
        val v = u / 0.8f
        val x = 226f + 140f * v
        val y = 395f - 150f * 4f * v * (1f - v)
        cv.save()
        cv.translate(x, y)
        cv.rotate(-55f + 110f * v)
        cv.scale(0.85f, 0.85f)
        p.color = 0xFF3F7FE0.toInt()
        cv.drawPath(body, p)
        p.color = 0xFFCFE2FF.toInt()
        cv.drawPath(belly, p)
        p.color = 0xFF2F63B8.toInt()
        cv.drawPath(fin, p)
        p.color = 0xFF1B2A4A.toInt()
        cv.drawCircle(52f, -4f, 3.8f, p)
        cv.restore()
    }

    override fun onDraw(cv: Canvas) {
        cv.save()
        val k = width / 591f
        cv.scale(k, k)

        // Ciel
        p.shader = LinearGradient(0f, 0f, 0f, 340f, 0xFF5C4BC8.toInt(), 0xFF4E3EB0.toInt(), Shader.TileMode.CLAMP)
        cv.drawRect(0f, 0f, 591f, 345f, p)
        p.shader = null

        // Plumettes duveteuses qui tombent (derrière la baleine)
        drawDowns(cv)

        // Baleine : saute des nuages puis retombe lentement dedans
        drawWhale(cv, t / (2f * Math.PI.toFloat()))

        // Nuages (devant la baleine, mouvement léger)
        // Mode nuit : nuages noirs (couleur de fond) ; mode jour : nuages blancs
        val cloudColor = if (dark) bgEnd else Color.WHITE
        p.color = cloudColor
        val da = 14f * sin(t)
        val db = -12f * sin(t + 1f)
        val dc = 8f * sin(t + 2f)
        val dy = 2f * sin(2f * t)
        for (cl in cloudsA) cv.drawCircle(cl[0] + da, cl[1] + dy, cl[2], p)
        for (cl in cloudsB) cv.drawCircle(cl[0] + db, cl[1] - dy, cl[2], p)
        for (cl in cloudsC) cv.drawCircle(cl[0] + dc, cl[1] + dy, cl[2], p)
        p.shader = LinearGradient(
            0f, 340f, 0f, 610f,
            intArrayOf(cloudColor, cloudColor, bgEnd), floatArrayOf(0f, 0.3f, 1f), Shader.TileMode.CLAMP
        )
        cv.drawRect(-10f, 340f, 601f, 620f, p)
        p.shader = null

        cv.restore()
    }
}

/** Logo « Owlino [Pro] ». */
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
        val w2 = p.measureText("Pro")
        val pill = w2 + 34f
        val x0 = (591f - (w1 + 12f + pill)) / 2f
        cv.drawText("Owlino", x0, 80f, p)
        cv.drawRoundRect(RectF(x0 + w1 + 12f, 33f, x0 + w1 + 12f + pill, 87f), 12f, 12f, p)
        p.color = 0xFF5B49C6.toInt()
        cv.drawText("Pro", x0 + w1 + 12f + 17f, 80f, p)
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
            3 -> { path.moveTo(x + s * 1.2f, y); path.lineTo(x - s * 1.2f, y); path.moveTo(x - s * 0.1f, y - s); path.lineTo(x - s * 1.2f, y); path.lineTo(x - s * 0.1f, y + s) }
            else -> { path.moveTo(x - s * 1.6f, y - s * 0.5f); path.lineTo(x, y + s * 0.5f); path.lineTo(x + s * 1.6f, y - s * 0.5f) }
        }
        cv.drawPath(path, p)
    }
}

class RadioView(c: Context, private val k: Float, private val off: Int) : View(c) {
    var sel = false
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(cv: Canvas) {
        val x = width / 2f
        val y = height / 2f
        p.style = Paint.Style.STROKE
        p.strokeWidth = 3.5f * k
        p.color = if (sel) 0xFF6C5CE7.toInt() else off
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

class DotsView(c: Context, private val k: Float, private val count: Int, private val off: Int, private val on: Int) : View(c) {
    var progress = 0f
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ev = ArgbEvaluator()
    override fun onDraw(cv: Canvas) {
        val gap = 27f * k
        val x0 = (width - gap * (count - 1)) / 2f
        for (i in 0 until count) {
            val t = max(0f, 1f - abs(i - progress))
            p.color = ev.evaluate(t, off, on) as Int
            cv.drawCircle(x0 + i * gap, height / 2f, (6f + 2.5f * t) * k, p)
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

/** Pager horizontal maison : glisse avec le doigt, ressort aux extrémités, animation de fin fluide. */
class Pager(
    c: Context,
    private val strip: ViewGroup,
    private val w: Int,
    private val n: Int,
    private val onProgress: (Float) -> Unit
) : FrameLayout(c) {
    private var downX = 0f
    private var downY = 0f
    private var startTx = 0f
    private var dragging = false
    private var vt: VelocityTracker? = null
    private var anim: ValueAnimator? = null
    private val slop = ViewConfiguration.get(c).scaledTouchSlop

    init {
        addView(strip, FrameLayout.LayoutParams(w * n, ViewGroup.LayoutParams.MATCH_PARENT))
        clipChildren = true
    }

    private fun update(pos: Float) {
        for (i in 0 until strip.childCount) {
            val d = min(1f, abs(i - pos))
            val ch = strip.getChildAt(i)
            ch.alpha = 1f - 0.55f * d
            val s = 1f - 0.08f * d
            ch.scaleX = s
            ch.scaleY = s
        }
        onProgress(pos)
    }

    fun jumpTo(i: Int) {
        strip.translationX = -i.toFloat() * w
        update(i.toFloat())
    }

    private fun goTo(i: Int) {
        anim?.cancel()
        val from = strip.translationX
        val to = -i.toFloat() * w
        anim = ValueAnimator.ofFloat(from, to).apply {
            duration = 340
            interpolator = DecelerateInterpolator(1.8f)
            addUpdateListener {
                strip.translationX = it.animatedValue as Float
                update(-strip.translationX / w)
            }
            start()
        }
    }

    private fun begin(e: MotionEvent) {
        downX = e.x
        downY = e.y
        anim?.cancel()
        startTx = strip.translationX
        vt?.recycle()
        vt = VelocityTracker.obtain()
        vt?.addMovement(e)
    }

    override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                begin(e)
                dragging = false
            }
            MotionEvent.ACTION_MOVE -> {
                vt?.addMovement(e)
                if (!dragging && abs(e.x - downX) > slop && abs(e.x - downX) > abs(e.y - downY)) {
                    dragging = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
            }
        }
        return dragging
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> begin(e)
            MotionEvent.ACTION_MOVE -> {
                vt?.addMovement(e)
                val dx = e.x - downX
                if (!dragging && abs(dx) > slop) {
                    dragging = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
                if (dragging) {
                    var tx = startTx + dx
                    val lo = -(n - 1f) * w
                    if (tx > 0f) tx *= 0.35f else if (tx < lo) tx = lo + (tx - lo) * 0.35f
                    strip.translationX = tx
                    update(-tx / w)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                vt?.addMovement(e)
                vt?.computeCurrentVelocity(1000)
                val vx = vt?.xVelocity ?: 0f
                val cur = -strip.translationX / w
                var target = when {
                    vx < -800f -> floor(cur).toInt() + 1
                    vx > 800f -> ceil(cur).toInt() - 1
                    else -> round(cur).toInt()
                }
                target = max(0, min(n - 1, target))
                vt?.recycle()
                vt = null
                dragging = false
                goTo(target)
            }
        }
        return true
    }
}
