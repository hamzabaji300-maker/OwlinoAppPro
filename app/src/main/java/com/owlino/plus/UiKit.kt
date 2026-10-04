package com.owlino.plus

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.TextView
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

internal const val MP = ViewGroup.LayoutParams.MATCH_PARENT
internal const val WC = ViewGroup.LayoutParams.WRAP_CONTENT

/** Couleurs de l'interface (reprises du thème « Classic » de l'application Owlino). */
object UiColors {
    val BLUE = 0xFF3B82F6.toInt()
    val GREEN = 0xFF22C55E.toInt()
    fun bg(d: Boolean) = if (d) 0xFF191919.toInt() else 0xFFF9FAFB.toInt()
    fun surface(d: Boolean) = if (d) 0xFF272727.toInt() else 0xFFFFFFFF.toInt()
    fun text(d: Boolean) = if (d) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
    fun text2(d: Boolean) = if (d) 0xFFAAAAAA.toInt() else 0xFF6B7280.toInt()
    fun preview(d: Boolean) = if (d) 0xFFCFCFCF.toInt() else 0xFF222222.toInt()
    fun chip(d: Boolean) = if (d) 0x1AFFFFFF else 0xFFF3F4F6.toInt()
    fun border(d: Boolean) = if (d) 0x0FFFFFFF else 0x0A000000
}

fun Context.dp(v: Float): Int = (v * resources.displayMetrics.density + 0.5f).toInt()
fun Context.dpf(v: Float): Float = v * resources.displayMetrics.density

/** weight : 0 = normal, 1 = medium, 2 = gras. */
fun Context.label(s: String, sp: Float, color: Int, weight: Int = 0): TextView = TextView(this).apply {
    text = s
    setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
    setTextColor(color)
    typeface = when (weight) {
        2 -> Typeface.DEFAULT_BOLD
        1 -> Typeface.create("sans-serif-medium", Typeface.NORMAL)
        else -> Typeface.DEFAULT
    }
    maxLines = 1
    ellipsize = TextUtils.TruncateAt.END
    includeFontPadding = false
}

fun Context.rounded(color: Int, radiusDp: Float, strokeDp: Float = 0f, stroke: Int = 0): GradientDrawable =
    GradientDrawable().apply {
        setColor(color)
        cornerRadius = dpf(radiusDp)
        if (strokeDp > 0f) setStroke(max(1, dp(strokeDp)), stroke)
    }

fun Context.oval(color: Int, strokeDp: Float = 0f, stroke: Int = 0): GradientDrawable =
    GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
        if (strokeDp > 0f) setStroke(max(1, dp(strokeDp)), stroke)
    }

/** Icônes dessinées au Canvas (grille 24x24). */
class UiGlyph(c: Context, var kind: Int, var color: Int) : View(c) {
    companion object {
        const val MENU = 0
        const val SEARCH = 1
        const val PENCIL = 2
        const val CHEV_DOWN = 3
        const val MORE = 4
        const val MEGA = 5
        const val BELL_OFF = 6
        const val STAR = 7
        const val PIN = 8
        const val CHECK = 9
        const val DONE_ALL = 10
        const val BOT = 11
        const val VERIFIED = 12
        const val MIC = 13
        const val CLOSE = 14
        const val ARROW_BACK = 15
    }

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    private fun stroke(w: Float) {
        p.style = Paint.Style.STROKE
        p.strokeWidth = w
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
    }

    private fun fill() {
        p.style = Paint.Style.FILL
    }

    private fun line(cv: Canvas, x1: Float, y1: Float, x2: Float, y2: Float) = cv.drawLine(x1, y1, x2, y2, p)

    private fun poly(cv: Canvas, vararg v: Float) {
        val path = Path()
        path.moveTo(v[0], v[1])
        var i = 2
        while (i < v.size) {
            path.lineTo(v[i], v[i + 1])
            i += 2
        }
        cv.drawPath(path, p)
    }

    override fun onDraw(cv: Canvas) {
        val s = min(width, height) / 24f
        cv.save()
        cv.translate((width - 24f * s) / 2f, (height - 24f * s) / 2f)
        cv.scale(s, s)
        p.reset()
        p.isAntiAlias = true
        p.color = color
        when (kind) {
            MENU -> {
                stroke(2f)
                line(cv, 3f, 6f, 21f, 6f)
                line(cv, 3f, 12f, 21f, 12f)
                line(cv, 3f, 18f, 21f, 18f)
            }
            SEARCH -> {
                stroke(2.2f)
                cv.drawCircle(10.5f, 10.5f, 6.5f, p)
                line(cv, 15.4f, 15.4f, 20.5f, 20.5f)
            }
            PENCIL -> {
                fill()
                val a = Path()
                a.moveTo(3f, 17.25f); a.lineTo(3f, 21f); a.lineTo(6.75f, 21f); a.lineTo(17.81f, 9.94f); a.lineTo(14.06f, 6.19f); a.close()
                cv.drawPath(a, p)
                val b = Path()
                b.moveTo(20.71f, 7.04f); b.cubicTo(21.1f, 6.65f, 21.1f, 6.02f, 20.71f, 5.63f)
                b.lineTo(18.37f, 3.29f); b.cubicTo(17.98f, 2.9f, 17.35f, 2.9f, 16.96f, 3.29f)
                b.lineTo(15.13f, 5.12f); b.lineTo(18.88f, 8.87f); b.close()
                cv.drawPath(b, p)
            }
            CHEV_DOWN -> {
                stroke(2.4f)
                poly(cv, 6f, 9f, 12f, 15f, 18f, 9f)
            }
            MORE -> {
                fill()
                cv.drawCircle(12f, 5f, 2f, p)
                cv.drawCircle(12f, 12f, 2f, p)
                cv.drawCircle(12f, 19f, 2f, p)
            }
            MEGA -> {
                stroke(1.9f)
                poly(cv, 3f, 10f, 7f, 10f, 15f, 5.5f, 15f, 18.5f, 7f, 14f, 3f, 14f, 3f, 10f)
                line(cv, 7f, 14f, 8.5f, 19.5f)
                line(cv, 18.5f, 9.5f, 18.5f, 14.5f)
            }
            BELL_OFF -> {
                stroke(1.9f)
                val b = Path()
                b.moveTo(5.5f, 17f)
                b.cubicTo(7f, 15.5f, 7.5f, 14f, 7.5f, 11f)
                b.cubicTo(7.5f, 8.5f, 9.5f, 6.5f, 12f, 6.5f)
                b.cubicTo(14.5f, 6.5f, 16.5f, 8.5f, 16.5f, 11f)
                b.cubicTo(16.5f, 14f, 17f, 15.5f, 18.5f, 17f)
                b.close()
                cv.drawPath(b, p)
                val k = Path()
                k.moveTo(10f, 19.5f); k.quadTo(12f, 21.5f, 14f, 19.5f)
                cv.drawPath(k, p)
                line(cv, 4.5f, 4.5f, 19.5f, 19.5f)
            }
            STAR -> {
                p.style = Paint.Style.FILL_AND_STROKE
                p.strokeWidth = 1.2f
                p.strokeJoin = Paint.Join.ROUND
                val path = Path()
                for (i in 0 until 10) {
                    val r = if (i % 2 == 0) 9.2f else 4.4f
                    val a = -Math.PI / 2.0 + i * Math.PI / 5.0
                    val x = 12f + (r * cos(a)).toFloat()
                    val y = 12.8f + (r * sin(a)).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                cv.drawPath(path, p)
            }
            PIN -> {
                fill()
                val b = Path()
                b.moveTo(16f, 9f); b.lineTo(16f, 4f); b.lineTo(17f, 4f)
                b.cubicTo(17.55f, 4f, 18f, 3.55f, 18f, 3f)
                b.cubicTo(18f, 2.45f, 17.55f, 2f, 17f, 2f)
                b.lineTo(7f, 2f)
                b.cubicTo(6.45f, 2f, 6f, 2.45f, 6f, 3f)
                b.cubicTo(6f, 3.55f, 6.45f, 4f, 7f, 4f)
                b.lineTo(8f, 4f); b.lineTo(8f, 9f)
                b.cubicTo(8f, 10.66f, 6.66f, 12f, 5f, 12f)
                b.lineTo(5f, 14f); b.lineTo(10.97f, 14f); b.lineTo(10.97f, 21f); b.lineTo(11.97f, 22f); b.lineTo(12.97f, 21f); b.lineTo(12.97f, 14f)
                b.lineTo(19f, 14f); b.lineTo(19f, 12f)
                b.cubicTo(17.34f, 12f, 16f, 10.66f, 16f, 9f)
                b.close()
                cv.drawPath(b, p)
            }
            CHECK -> {
                stroke(2.4f)
                poly(cv, 5f, 12.5f, 10f, 17.5f, 19f, 7.5f)
            }
            DONE_ALL -> {
                stroke(2.2f)
                poly(cv, 2f, 13f, 7f, 18f, 17f, 8f)
                poly(cv, 11.5f, 16.5f, 13f, 18f, 22.5f, 8f)
            }
            BOT -> {
                stroke(1.8f)
                cv.drawRoundRect(RectF(4f, 8f, 20f, 20f), 3f, 3f, p)
                line(cv, 12f, 8f, 12f, 4.5f)
                line(cv, 2f, 12f, 2f, 16f)
                line(cv, 22f, 12f, 22f, 16f)
                fill()
                cv.drawCircle(12f, 3.8f, 1.4f, p)
                cv.drawCircle(9f, 13.8f, 1.3f, p)
                cv.drawCircle(15f, 13.8f, 1.3f, p)
            }
            VERIFIED -> {
                val path = Path()
                for (i in 0..72) {
                    val th = i * 5.0 * Math.PI / 180.0
                    val r = 9.4 + 0.8 * cos(8.0 * th)
                    val x = (12.0 + r * cos(th)).toFloat()
                    val y = (12.0 + r * sin(th)).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                p.color = 0xFF3B82F6.toInt()
                fill()
                cv.drawPath(path, p)
                p.color = Color.WHITE
                stroke(2f)
                poly(cv, 7.8f, 12.3f, 10.8f, 15.2f, 16.4f, 9.2f)
            }
            MIC -> {
                fill()
                cv.drawRoundRect(RectF(9f, 3f, 15f, 14f), 3f, 3f, p)
                stroke(1.8f)
                val a = Path()
                a.moveTo(6f, 11f); a.cubicTo(6f, 14.3f, 8.7f, 17f, 12f, 17f); a.cubicTo(15.3f, 17f, 18f, 14.3f, 18f, 11f)
                cv.drawPath(a, p)
                line(cv, 12f, 17f, 12f, 21f)
            }
            CLOSE -> {
                stroke(2.2f)
                line(cv, 6f, 6f, 18f, 18f)
                line(cv, 18f, 6f, 6f, 18f)
            }
            ARROW_BACK -> {
                stroke(2.2f)
                line(cv, 20f, 12f, 4f, 12f)
                poly(cv, 11f, 5f, 4f, 12f, 11f, 19f)
            }
        }
        cv.restore()
    }
}

/** Avatar rond avec une lettre (aucune image externe). */
class CircleAvatar(
    c: Context,
    private val letter: String,
    private val bg: Int,
    private val fg: Int = Color.WHITE,
    private val border: Int = 0,
    shadow: Boolean = false
) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        if (shadow) {
            elevation = c.dpf(2f)
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(v: View, o: Outline) {
                    o.setOval(0, 0, v.width, v.height)
                }
            }
        }
    }

    override fun onDraw(cv: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = min(width, height) / 2f
        p.style = Paint.Style.FILL
        p.color = bg
        cv.drawCircle(cx, cy, r, p)
        if (border != 0) {
            p.style = Paint.Style.STROKE
            p.strokeWidth = context.dpf(1f)
            p.color = border
            cv.drawCircle(cx, cy, r - p.strokeWidth / 2f, p)
        }
        p.style = Paint.Style.FILL
        p.color = fg
        p.textSize = r * 0.8f
        p.textAlign = Paint.Align.CENTER
        p.typeface = Typeface.DEFAULT_BOLD
        cv.drawText(letter, cx, cy - (p.ascent() + p.descent()) / 2f, p)
    }
}
