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

    // ---- Plume dorée (grande plume de l'écran Owlino Feather), pointe vers le haut, repère [-1,1] ----
    private val quillV = floatArrayOf(
        0.088f, -1.000f, 0.092f, -0.996f, 0.099f, -0.987f, 0.109f, -0.975f, 0.122f, -0.958f,
        0.136f, -0.937f, 0.148f, -0.914f, 0.160f, -0.887f, 0.172f, -0.857f, 0.181f, -0.827f,
        0.189f, -0.798f, 0.194f, -0.768f, 0.198f, -0.739f, 0.201f, -0.711f, 0.204f, -0.684f,
        0.207f, -0.659f, 0.208f, -0.635f, 0.210f, -0.617f, 0.211f, -0.605f, 0.211f, -0.599f,
        0.167f, -0.539f, 0.167f, -0.533f, 0.167f, -0.520f, 0.167f, -0.500f, 0.166f, -0.474f,
        0.166f, -0.446f, 0.164f, -0.415f, 0.162f, -0.382f, 0.159f, -0.347f, 0.157f, -0.315f,
        0.158f, -0.285f, 0.161f, -0.258f, 0.166f, -0.234f, 0.169f, -0.215f, 0.171f, -0.203f,
        0.173f, -0.197f, 0.197f, -0.168f, 0.198f, -0.163f, 0.199f, -0.154f, 0.201f, -0.140f,
        0.203f, -0.122f, 0.206f, -0.099f, 0.210f, -0.070f, 0.215f, -0.036f, 0.221f, 0.003f,
        0.227f, 0.039f, 0.232f, 0.073f, 0.237f, 0.104f, 0.242f, 0.133f, 0.246f, 0.155f,
        0.249f, 0.169f, 0.250f, 0.177f, 0.246f, 0.183f, 0.238f, 0.195f, 0.227f, 0.213f,
        0.212f, 0.236f, 0.197f, 0.261f, 0.182f, 0.285f, 0.169f, 0.310f, 0.155f, 0.336f,
        0.145f, 0.360f, 0.139f, 0.383f, 0.136f, 0.404f, 0.136f, 0.424f, 0.137f, 0.440f,
        0.137f, 0.450f, 0.137f, 0.455f, 0.068f, 0.507f, 0.157f, 0.497f, 0.067f, 0.579f,
        0.029f, 0.641f, 0.029f, 0.641f, -0.015f, 0.641f, -0.043f, 0.607f, -0.128f, 0.616f,
        -0.090f, 0.553f, -0.188f, 0.553f, -0.109f, 0.516f, -0.107f, 0.514f, -0.105f, 0.510f,
        -0.101f, 0.503f, -0.096f, 0.494f, -0.095f, 0.483f, -0.098f, 0.470f, -0.104f, 0.454f,
        -0.115f, 0.436f, -0.127f, 0.416f, -0.142f, 0.393f, -0.158f, 0.368f, -0.176f, 0.340f,
        -0.190f, 0.320f, -0.199f, 0.306f, -0.203f, 0.299f, -0.204f, 0.292f, -0.206f, 0.279f,
        -0.209f, 0.260f, -0.213f, 0.234f, -0.218f, 0.208f, -0.223f, 0.183f, -0.228f, 0.158f,
        -0.235f, 0.132f, -0.240f, 0.107f, -0.244f, 0.081f, -0.246f, 0.055f, -0.248f, 0.028f,
        -0.249f, -0.001f, -0.249f, -0.031f, -0.250f, -0.063f, -0.250f, -0.098f, -0.248f, -0.133f,
        -0.245f, -0.170f, -0.241f, -0.209f, -0.234f, -0.248f, -0.228f, -0.290f, -0.221f, -0.333f,
        -0.215f, -0.378f, -0.208f, -0.425f, -0.200f, -0.471f, -0.193f, -0.517f, -0.185f, -0.561f,
        -0.177f, -0.605f, -0.167f, -0.647f, -0.157f, -0.686f, -0.145f, -0.724f, -0.131f, -0.758f,
        -0.121f, -0.785f, -0.114f, -0.802f, -0.111f, -0.811f, -0.109f, -0.815f, -0.104f, -0.822f,
        -0.097f, -0.834f, -0.088f, -0.850f, -0.076f, -0.867f, -0.061f, -0.885f, -0.044f, -0.905f,
        -0.024f, -0.925f, -0.009f, -0.941f, 0.001f, -0.951f, 0.006f, -0.957f
    )
    private val quillR = floatArrayOf(
        0.067f, -0.960f, 0.061f, -0.946f, 0.052f, -0.928f, 0.042f, -0.907f, 0.031f, -0.883f,
        0.020f, -0.857f, 0.010f, -0.830f, 0.001f, -0.801f, -0.008f, -0.771f, -0.016f, -0.738f,
        -0.025f, -0.704f, -0.034f, -0.669f, -0.042f, -0.633f, -0.050f, -0.597f, -0.059f, -0.559f,
        -0.067f, -0.519f, -0.075f, -0.479f, -0.082f, -0.439f, -0.088f, -0.398f, -0.094f, -0.357f,
        -0.099f, -0.315f, -0.103f, -0.272f, -0.107f, -0.229f, -0.110f, -0.186f, -0.111f, -0.144f,
        -0.113f, -0.103f, -0.113f, -0.062f, -0.112f, -0.021f, -0.111f, 0.020f, -0.109f, 0.060f,
        -0.106f, 0.100f, -0.101f, 0.139f, -0.096f, 0.178f, -0.090f, 0.217f, -0.083f, 0.255f,
        -0.076f, 0.294f, -0.068f, 0.332f, -0.061f, 0.371f, -0.054f, 0.409f, -0.046f, 0.447f,
        -0.038f, 0.485f, -0.030f, 0.523f, -0.022f, 0.561f, -0.013f, 0.599f, -0.004f, 0.637f,
        0.005f, 0.675f, 0.014f, 0.713f, 0.024f, 0.751f, 0.033f, 0.787f, 0.042f, 0.826f,
        0.053f, 0.867f, 0.063f, 0.908f, 0.073f, 0.945f, 0.081f, 0.977f, 0.087f, 1.000f
    )
    private val quillB = floatArrayOf(
        0.010f, -0.830f, 0.088f, -0.947f, 0.010f, -0.830f, -0.040f, -0.868f, -0.016f, -0.738f,
        0.097f, -0.910f, -0.016f, -0.738f, -0.083f, -0.788f, -0.042f, -0.633f, 0.108f, -0.862f,
        -0.042f, -0.633f, -0.116f, -0.689f, -0.067f, -0.519f, 0.119f, -0.802f, -0.067f, -0.519f,
        -0.144f, -0.577f, -0.088f, -0.398f, 0.130f, -0.730f, -0.088f, -0.398f, -0.166f, -0.456f,
        -0.103f, -0.272f, 0.139f, -0.639f, -0.103f, -0.272f, -0.183f, -0.331f, -0.111f, -0.144f,
        0.145f, -0.533f, -0.111f, -0.144f, -0.199f, -0.209f, -0.112f, -0.021f, 0.110f, -0.359f,
        -0.112f, -0.021f, -0.208f, -0.092f, -0.106f, 0.100f, 0.105f, -0.221f, -0.106f, 0.100f,
        -0.206f, 0.025f, -0.090f, 0.217f, 0.125f, -0.109f, -0.090f, 0.217f, -0.195f, 0.139f,
        -0.068f, 0.332f, 0.153f, -0.004f, -0.068f, 0.332f, -0.171f, 0.256f, -0.046f, 0.447f,
        0.172f, 0.116f, -0.046f, 0.447f, -0.128f, 0.386f
    )
    private val quillVane = Path().apply {
        moveTo(quillV[0], quillV[1])
        var i = 2
        while (i < quillV.size) { lineTo(quillV[i], quillV[i + 1]); i += 2 }
        close()
    }
    private val quillRachis = Path().apply {
        moveTo(quillR[0], quillR[1])
        var i = 2
        while (i < quillR.size) { lineTo(quillR[i], quillR[i + 1]); i += 2 }
    }
    private val quillBarbs = Path().apply {
        var i = 0
        while (i < quillB.size) { moveTo(quillB[i], quillB[i + 1]); lineTo(quillB[i + 2], quillB[i + 3]); i += 4 }
    }

    /** Grande plume dorée (style plume d'écriture). h = demi-hauteur en pixels. Rendu logiciel requis pour la lueur. */
    fun drawQuill(cv: Canvas, cx: Float, cy: Float, h: Float, rot: Float, dark: Boolean, alpha: Int = 255) {
        val m = Matrix()
        m.setScale(h, h)
        m.postRotate(rot)
        m.postTranslate(cx, cy)
        val v = Path()
        quillVane.transform(m, v)
        val pts = floatArrayOf(0.2f, -1f, -0.2f, 0.5f)
        m.mapPoints(pts)

        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(pts[0], pts[1], pts[2], pts[3], 0xFFFFE699.toInt(), 0xFFC9941C.toInt(), Shader.TileMode.CLAMP)
        p.alpha = alpha
        p.setShadowLayer(max(3f, h * 0.10f), 0f, h * 0.04f, if (dark) 0x66FFC857 else 0x40B8860B)
        cv.drawPath(v, p)
        p.clearShadowLayer()
        p.shader = null

        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        p.strokeWidth = max(1f, h * 0.012f)
        p.color = 0xFFB07A0A.toInt()
        p.alpha = alpha
        val b = Path()
        quillBarbs.transform(m, b)
        cv.drawPath(b, p)

        p.strokeWidth = max(1.5f, h * 0.022f)
        p.color = 0xFF8A5A00.toInt()
        p.alpha = alpha
        cv.drawPath(v, p)

        val r = Path()
        quillRachis.transform(m, r)
        p.strokeWidth = max(1.5f, h * 0.03f)
        p.color = 0xFF7A4E00.toInt()
        p.alpha = alpha
        cv.drawPath(r, p)
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
        Feather.drawQuill(cv, 295f * k, 205f * k, 118f * k, 22f, dark, 255)
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
