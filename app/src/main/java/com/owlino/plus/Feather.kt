package com.owlino.plus

import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.max
import kotlin.math.sqrt

/** Plume de pigeon blanche, dessinée au Canvas (pointe vers le haut avant rotation). */
object Feather {
    private val vane = Path().apply {
        moveTo(0.05f, 1f)
        cubicTo(-0.60f, 0.50f, -0.72f, -0.35f, 0f, -1f)
        cubicTo(0.68f, -0.45f, 0.58f, 0.40f, 0.05f, 1f)
        close()
    }
    private val rachis = Path().apply {
        moveTo(0.07f, 1.22f)
        quadTo(-0.08f, 0.1f, 0f, -0.95f)
    }
    private val barbs = Path().apply {
        for (j in 0..8) {
            val y = -0.72f + j * 0.19f
            val hw = 0.62f * sqrt(max(0f, 1f - y * y))
            moveTo(0f, y + 0.02f); lineTo(-hw * 0.92f, y - 0.26f)
            moveTo(0f, y + 0.02f); lineTo(hw * 0.9f, y - 0.28f)
        }
    }

    /** h = demi-hauteur en pixels. La vue doit être en rendu logiciel pour l'ombre. */
    fun draw(cv: Canvas, cx: Float, cy: Float, h: Float, rot: Float, dark: Boolean, alpha: Int = 255, detail: Boolean = true) {
        val m = Matrix()
        m.setScale(h, h)
        m.postRotate(rot)
        m.postTranslate(cx, cy)
        val v = Path()
        vane.transform(m, v)
        val pts = floatArrayOf(-0.5f, -0.8f, 0.5f, 0.8f)
        m.mapPoints(pts)

        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(pts[0], pts[1], pts[2], pts[3], Color.WHITE, 0xFFEBEFF6.toInt(), Shader.TileMode.CLAMP)
        p.alpha = alpha
        p.setShadowLayer(max(2f, h * 0.14f), 0f, h * 0.05f, if (dark) 0x66FFFFFF else 0x38000000)
        cv.drawPath(v, p)
        p.clearShadowLayer()
        p.shader = null

        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = max(1f, h * 0.03f)
        p.color = if (dark) 0xFFC9D1DE.toInt() else 0xFFD3D9E2.toInt()
        p.alpha = alpha
        cv.drawPath(v, p)

        if (detail && h > 40f) {
            val b = Path()
            barbs.transform(m, b)
            p.strokeWidth = max(0.8f, h * 0.012f)
            p.color = 0xFFDCE2EB.toInt()
            p.alpha = alpha
            cv.drawPath(b, p)
            val r = Path()
            rachis.transform(m, r)
            p.strokeWidth = max(1f, h * 0.03f)
            p.color = 0xFFC4CCD8.toInt()
            p.alpha = alpha
            cv.drawPath(r, p)
        }
    }
}

/** Grande plume blanche + petites plumes en arrière-plan. Repère : 591 unités de large. */
class HeroView(c: Context, private val dark: Boolean) : View(c) {
    private val list = ArrayList<FloatArray>()

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        val r = java.util.Random(7)
        while (list.size < 42) {
            val x = 110f + r.nextFloat() * 380f
            val y = 55f + r.nextFloat() * 285f
            val dx = x - 295f
            val dy = y - 205f
            if (dx * dx + dy * dy < 125f * 125f) continue
            list.add(floatArrayOf(x, y, 7f + r.nextFloat() * 10f, r.nextFloat() * 360f, 0.45f + r.nextFloat() * 0.55f))
        }
    }

    override fun onDraw(cv: Canvas) {
        val k = width / 591f
        for (f in list) Feather.draw(cv, f[0] * k, f[1] * k, f[2] * k, f[3], dark, (f[4] * 255).toInt(), false)
        Feather.draw(cv, 295f * k, 205f * k, 112f * k, 28f, dark, 255, true)
    }
}

/** Petite plume (icône du solde). */
class FeatherIconView(c: Context, private val dark: Boolean) : View(c) {
    init { setLayerType(LAYER_TYPE_SOFTWARE, null) }
    override fun onDraw(cv: Canvas) {
        Feather.draw(cv, width / 2f, height / 2f, height * 0.36f, 25f, dark, 255, false)
    }
}

/** Icône « offrir aux amis » : deux personnes. */
class GiftIconView(c: Context, private val color: Int) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(cv: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        p.style = Paint.Style.STROKE
        p.strokeWidth = w * 0.07f
        p.strokeCap = Paint.Cap.ROUND
        p.color = color
        cv.drawCircle(w * 0.34f, h * 0.34f, w * 0.14f, p)
        cv.drawArc(RectF(w * 0.08f, h * 0.56f, w * 0.60f, h * 1.1f), 180f, 180f, false, p)
        cv.drawCircle(w * 0.70f, h * 0.30f, w * 0.12f, p)
        cv.drawArc(RectF(w * 0.52f, h * 0.50f, w * 0.94f, h * 1.0f), 180f, 180f, false, p)
        p.style = Paint.Style.FILL
        cv.drawCircle(w * 0.92f, h * 0.10f, w * 0.05f, p)
    }
}
