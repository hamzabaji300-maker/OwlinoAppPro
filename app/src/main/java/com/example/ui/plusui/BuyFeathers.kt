package com.example.ui.plusui

import android.animation.ValueAnimator
import android.app.Activity
import android.app.Dialog
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

/**
 * Écran « اختيار الحزمة » : s'ouvre au clic sur « شراء الريش ».
 * Même organisation que l'écran d'achat de Telegram, avec des plumes argentées.
 */
class BuyFeathers(private val act: Activity, private val k: Float, private val pal: Pal) {

    private class Pack(val label: String, val price: String, val count: Int, val scale: Float)

    private val packs = listOf(
        Pack("١٠٠ ريشة", "٣١٩٫٠٠ د.ج.", 1, 1f),
        Pack("١٥٠ ريشة", "٤٧٩٫٠٠ د.ج.", 1, 1.15f),
        Pack("٢٥٠ ريشة", "٧٩٩٫٠٠ د.ج.", 2, 1f),
        Pack("٣٥٠ ريشة", "١٬١١٩٫٠٠ د.ج.", 2, 1.15f),
        Pack("٥٠٠ ريشة", "١٬٥٩٩٫٠٠ د.ج.", 3, 1f),
        Pack("٧٥٠ ريشة", "٢٬٣٩٩٫٠٠ د.ج.", 3, 1.15f),
        Pack("١ ٠٠٠ ريشة", "٣٬١٩٩٫٠٠ د.ج.", 4, 1f),
        Pack("١ ٥٠٠ ريشة", "٤٬٧٩٩٫٠٠ د.ج.", 4, 1.15f),
        Pack("٢ ٥٠٠ ريشة", "٧٬٩٩٩٫٠٠ د.ج.", 5, 1f),
        Pack("٥ ٠٠٠ ريشة", "١٥٬٩٩٩٫٠٠ د.ج.", 5, 1.1f),
        Pack("١٠ ٠٠٠ ريشة", "٣١٬٩٩٩٫٠٠ د.ج.", 5, 1.2f)
    )

    private fun px(v: Float) = (v * k).toInt()

    private fun tv(text: CharSequence, size: Float, color: Int, bold: Boolean = false): TextView =
        TextView(act).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_PX, size * k)
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
            includeFontPadding = false
        }

    private fun row(p: Pack): FrameLayout {
        val r = FrameLayout(act)
        r.addView(GiftFeatherStackView(act, p.count, p.scale),
            FrameLayout.LayoutParams(px(72f), px(56f), Gravity.RIGHT or Gravity.CENTER_VERTICAL).apply { rightMargin = px(18f) })
        r.addView(tv(p.label, 22f, pal.fTitle, true),
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.RIGHT or Gravity.CENTER_VERTICAL).apply { rightMargin = px(96f) })
        r.addView(tv(p.price, 19f, pal.fGrey),
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.LEFT or Gravity.CENTER_VERTICAL).apply { leftMargin = px(26f) })
        r.addView(View(act).apply { setBackgroundColor(pal.divider) },
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1, Gravity.BOTTOM).apply { rightMargin = px(18f) })
        val sel = android.util.TypedValue()
        if (act.theme.resolveAttribute(android.R.attr.selectableItemBackground, sel, true)) r.setBackgroundResource(sel.resourceId)
        r.setOnClickListener { Toast.makeText(act, "شراء ${p.label} • ${p.price}", Toast.LENGTH_SHORT).show() }
        return r
    }

    @Suppress("DEPRECATION")
    fun start() {
        val dlg = Dialog(act, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar)
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val root = FrameLayout(act).apply {
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            setBackgroundColor(pal.fBg)
        }
        root.addView(SilverHeroView(act, k, pal.dark),
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(380f)))

        // Feuille blanche aux coins arrondis avec la liste
        val r = 40f * k
        val sheet = FrameLayout(act).apply {
            background = GradientDrawable().apply {
                setColor(pal.fCard)
                cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
            }
        }
        val scroll = ScrollView(act).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            clipToPadding = false
        }
        val list = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        list.addView(tv("اختيار الحزمة", 20f, GiftFlow.GREEN, true).apply { gravity = Gravity.RIGHT },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = px(34f); bottomMargin = px(14f); rightMargin = px(26f); leftMargin = px(26f)
            })
        for (p in packs) list.addView(row(p), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(72f)))

        val t1 = "شراؤك للريش يعني موافقتك\nعلى "
        val t2 = "الشروط والأحكام"
        val terms = SpannableStringBuilder(t1 + t2 + ".")
        terms.setSpan(ForegroundColorSpan(GiftFlow.GREEN), t1.length, t1.length + t2.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        list.addView(tv(terms, 17f, pal.fGrey).apply {
            gravity = Gravity.CENTER
            textDirection = View.TEXT_DIRECTION_RTL
            setLineSpacing(0f, 1.25f)
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = px(24f); bottomMargin = px(44f); leftMargin = px(30f); rightMargin = px(30f)
        })
        scroll.addView(list, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        sheet.addView(scroll, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        root.addView(sheet, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT).apply { topMargin = px(330f) })

        // Flèche retour
        val back = ChevronView(act, k, 3, pal.fTitle)
        root.addView(back, FrameLayout.LayoutParams(px(90f), px(90f)).apply { topMargin = px(40f) })
        back.setOnClickListener { dlg.dismiss() }

        root.setOnApplyWindowInsetsListener { _, ins ->
            val cy = max(85f * k, ins.systemWindowInsetTop + 35f * k)
            (back.layoutParams as FrameLayout.LayoutParams).topMargin = (cy - 45f * k).toInt()
            back.requestLayout()
            scroll.setPadding(0, 0, 0, ins.systemWindowInsetBottom)
            ins
        }

        // La feuille glisse de bas en haut
        sheet.visibility = View.INVISIBLE
        sheet.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
            override fun onLayoutChange(v: View, l: Int, t: Int, rr: Int, b: Int, ol: Int, ot: Int, or2: Int, ob: Int) {
                v.removeOnLayoutChangeListener(this)
                v.translationY = (b - t).toFloat()
                v.visibility = View.VISIBLE
                v.animate().translationY(0f).setDuration(350).setInterpolator(DecelerateInterpolator(2f)).start()
            }
        })

        dlg.setContentView(root)
        dlg.window?.apply {
            setBackgroundDrawable(ColorDrawable(pal.fBg))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = Color.TRANSPARENT
            navigationBarColor = pal.fCard
            decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    (if (pal.dark) 0 else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
        }
        dlg.show()
    }
}

/** Grande plume argentée + étincelles argentées qui scintillent. */
class SilverHeroView(c: Context, private val k: Float, private val dark: Boolean) : View(c) {
    private class Sp(val dx: Float, val y: Float, val s: Float, val kind: Int, val color: Int, val f: Int, val ph: Float)

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val list = ArrayList<Sp>()
    private var t = 0f
    private var anim: ValueAnimator? = null
    private val colors = if (dark)
        intArrayOf(0xFFE6EBF2.toInt(), 0xFFC4CCD8.toInt(), 0xFF9AA5B5.toInt())
    else
        intArrayOf(0xFFAEB7C4.toInt(), 0xFF8E99A8.toInt(), 0xFFC5CCD6.toInt())

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        val rnd = java.util.Random(5)
        var guard = 0
        while (list.size < 46 && guard++ < 800) {
            val dx = (rnd.nextFloat() * 2f - 1f) * 280f
            val y = 20f + rnd.nextFloat() * 320f
            if (hypot(dx, y - 175f) < 110f) continue
            list.add(Sp(dx, y, 5f + rnd.nextFloat() * 9f, rnd.nextInt(2), colors[rnd.nextInt(colors.size)], 1 + rnd.nextInt(3), rnd.nextFloat()))
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        anim?.cancel()
        anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 6000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { t = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        anim?.cancel()
        anim = null
        super.onDetachedFromWindow()
    }

    private fun star4(cv: Canvas, x: Float, y: Float, s: Float) {
        val path = Path()
        path.moveTo(x, y - s)
        path.quadTo(x, y, x + s, y)
        path.quadTo(x, y, x, y + s)
        path.quadTo(x, y, x - s, y)
        path.quadTo(x, y, x, y - s)
        cv.drawPath(path, p)
    }

    private fun star5(cv: Canvas, x: Float, y: Float, s: Float) {
        val path = Path()
        for (i in 0 until 10) {
            val a = Math.toRadians((-90 + i * 36).toDouble())
            val rr = if (i % 2 == 0) s else s * 0.45f
            val px = x + (cos(a) * rr).toFloat()
            val py = y + (sin(a) * rr).toFloat()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        cv.drawPath(path, p)
    }

    override fun onDraw(cv: Canvas) {
        val cx = width / 2f
        val tau = (2.0 * Math.PI).toFloat()
        for (s in list) {
            val tw = 0.5f + 0.5f * sin(tau * (s.f * t + s.ph))
            val sc = 0.55f + 0.45f * tw
            val x = cx + s.dx * k + 4f * k * sin(tau * (t + s.ph))
            val y = s.y * k + 4f * k * cos(tau * (t + s.ph * 2f))
            p.style = Paint.Style.FILL
            p.color = s.color
            p.alpha = (255 * (0.25f + 0.75f * tw)).toInt()
            if (s.kind == 0) star4(cv, x, y, s.s * k * sc * 1.3f) else star5(cv, x, y, s.s * k * sc)
        }
        p.alpha = 255
        Feather.drawQuill(cv, cx, 175f * k, 105f * k, 22f, dark, 255)
    }
}
