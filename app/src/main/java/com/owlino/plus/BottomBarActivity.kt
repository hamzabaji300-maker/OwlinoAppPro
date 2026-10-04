package com.owlino.plus

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import kotlin.math.max

/** Écran « شريط سفلي » : uniquement une barre de navigation en bas. */
class BottomBarActivity : Activity() {
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val k = resources.displayMetrics.widthPixels / 591f
        val pal = Pal.load(this)

        window.statusBarColor = pal.bg
        window.navigationBarColor = BottomBarView.barColor(pal.dark)
        window.decorView.systemUiVisibility =
            if (pal.dark) 0 else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        val root = FrameLayout(this)
        root.setBackgroundColor(pal.bg)
        root.addView(
            BottomBarView(this, k, pal.dark),
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (88f * k).toInt(), Gravity.BOTTOM)
        )
        setContentView(root)
    }
}

/**
 * Barre du bas : « Chats » (bulles de discussion + badge, dans une pastille bleu clair) et « Followers ».
 * Tout est dessiné au Canvas. Un appui change l'élément sélectionné (la pastille glisse).
 */
class BottomBarView(c: Context, private val k: Float, private val dark: Boolean) : View(c) {
    companion object {
        fun barColor(dark: Boolean) = if (dark) 0xFF1A1B1F.toInt() else 0xFFF8F9FD.toInt()
    }

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ev = ArgbEvaluator()
    private var sel = 0
    private var prog = 0f // 0 = Chats, 1 = Followers
    private var anim: ValueAnimator? = null

    private val barBg = barColor(dark)
    private val pillCol = if (dark) 0xFF2A3350.toInt() else 0xFFE6EDFF.toInt()
    private val blue = if (dark) 0xFF8FB0FF.toInt() else 0xFF4A7BE5.toInt()
    private val grey = if (dark) 0xFF9A9DA3.toInt() else 0xFF5F6168.toInt()
    private val badgeFill = if (dark) 0xFF3E5190.toInt() else 0xFFD3DFFF.toInt()
    private val badgeText = if (dark) 0xFFDCE6FF.toInt() else 0xFF2F55C4.toInt()

    private fun mix(f: Float, a: Int, b: Int): Int = ev.evaluate(f.coerceIn(0f, 1f), a, b) as Int

    private fun select(i: Int) {
        if (i == sel) return
        sel = i
        anim?.cancel()
        anim = ValueAnimator.ofFloat(prog, i.toFloat()).apply {
            duration = 240
            interpolator = DecelerateInterpolator(1.6f)
            addUpdateListener { prog = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> return true
            MotionEvent.ACTION_UP -> {
                select(if (e.x < width / 2f) 0 else 1)
                performClick()
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onDetachedFromWindow() {
        anim?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(cv: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()

        // Fond + fine ligne en haut
        p.style = Paint.Style.FILL
        p.color = barBg
        cv.drawRect(0f, 0f, w, h, p)
        p.color = if (dark) 0x22FFFFFF else 0x14000000
        cv.drawRect(0f, 0f, w, max(1f, 0.6f * k), p)

        // Pastille bleu clair (glisse entre les deux éléments)
        val pcx = w * (0.25f + 0.5f * prog)
        val pw = 112f * k
        val ph = 78f * k
        val pt = (h - ph) / 2f
        p.color = pillCol
        cv.drawRoundRect(RectF(pcx - pw / 2f, pt, pcx + pw / 2f, pt + ph), 36f * k, 36f * k, p)

        val s = 34f * k
        val cy = h / 2f - 11f * k
        val labelY = h / 2f + 27f * k

        for (i in 0..1) {
            val cx = w * (0.25f + 0.5f * i)
            val sf = if (i == 0) 1f - prog else prog
            val col = mix(sf, grey, blue)
            if (i == 0) chatIcon(cv, cx, cy, s, col, mix(sf, barBg, pillCol)) else peopleIcon(cv, cx, cy, s, col)

            p.style = Paint.Style.FILL
            p.color = col
            p.textSize = 15f * k
            p.textAlign = Paint.Align.CENTER
            p.typeface = if (sf > 0.5f) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            cv.drawText(if (i == 0) "المحادثات" else "Followers", cx, labelY, p)
        }
    }

    /** Deux bulles de discussion pleines + badge « ٢٠ » en haut à droite. */
    private fun chatIcon(cv: Canvas, cx: Float, cy: Float, s: Float, color: Int, gap: Int) {
        val x0 = cx - s / 2f
        val y0 = cy - s / 2f

        val back = Path().apply {
            addRoundRect(RectF(x0 + 0.42f * s, y0 + 0.24f * s, x0 + 0.98f * s, y0 + 0.72f * s), 0.13f * s, 0.13f * s, Path.Direction.CW)
            moveTo(x0 + 0.62f * s, y0 + 0.70f * s)
            lineTo(x0 + 0.90f * s, y0 + 0.70f * s)
            lineTo(x0 + 0.90f * s, y0 + 0.95f * s)
            close()
        }
        val front = Path().apply {
            addRoundRect(RectF(x0 + 0.02f * s, y0 + 0.06f * s, x0 + 0.70f * s, y0 + 0.62f * s), 0.17f * s, 0.17f * s, Path.Direction.CW)
            moveTo(x0 + 0.10f * s, y0 + 0.58f * s)
            lineTo(x0 + 0.10f * s, y0 + 0.90f * s)
            lineTo(x0 + 0.40f * s, y0 + 0.60f * s)
            close()
        }
        p.style = Paint.Style.FILL
        p.color = color
        cv.drawPath(back, p)
        // Liseré de la couleur du fond pour séparer les deux bulles
        p.style = Paint.Style.STROKE
        p.strokeWidth = 0.09f * s
        p.strokeJoin = Paint.Join.ROUND
        p.color = gap
        cv.drawPath(front, p)
        p.style = Paint.Style.FILL
        p.color = color
        cv.drawPath(front, p)

        // Badge
        val bx = x0 + 0.97f * s
        val by = y0 + 0.08f * s
        val br = 10f * k
        p.color = gap
        cv.drawCircle(bx, by, br + 2f * k, p)
        p.color = badgeFill
        cv.drawCircle(bx, by, br, p)
        p.color = badgeText
        p.textSize = 11.5f * k
        p.textAlign = Paint.Align.CENTER
        p.typeface = Typeface.DEFAULT_BOLD
        cv.drawText("٢٠", bx, by - (p.ascent() + p.descent()) / 2f, p)
    }

    /** Deux silhouettes (contour). */
    private fun peopleIcon(cv: Canvas, cx: Float, cy: Float, s: Float, color: Int) {
        p.style = Paint.Style.STROKE
        p.strokeWidth = 0.085f * s
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        p.color = color
        cv.drawCircle(cx - 0.12f * s, cy - 0.16f * s, 0.17f * s, p)
        cv.drawCircle(cx + 0.30f * s, cy - 0.22f * s, 0.13f * s, p)
        val b1 = Path().apply {
            moveTo(cx - 0.46f * s, cy + 0.40f * s)
            cubicTo(cx - 0.46f * s, cy + 0.10f * s, cx + 0.22f * s, cy + 0.10f * s, cx + 0.22f * s, cy + 0.40f * s)
        }
        val b2 = Path().apply {
            moveTo(cx + 0.36f * s, cy - 0.01f * s)
            cubicTo(cx + 0.58f * s, cy - 0.01f * s, cx + 0.64f * s, cy + 0.20f * s, cx + 0.64f * s, cy + 0.40f * s)
        }
        cv.drawPath(b1, p)
        cv.drawPath(b2, p)
        p.style = Paint.Style.FILL
    }
}
