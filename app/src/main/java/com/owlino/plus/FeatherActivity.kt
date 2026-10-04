package com.owlino.plus

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.math.max

class FeatherActivity : Activity() {
    private var k = 1f
    private fun px(v: Float) = (v * k).toInt()

    private fun tv(text: CharSequence, size: Float, color: Int, bold: Boolean = false): TextView =
        TextView(this).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_PX, size * k)
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
            includeFontPadding = false
        }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        k = resources.displayMetrics.widthPixels / 591f
        val pal = Pal.load(this)
        val green = 0xFF19BD6B.toInt()

        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = pal.fBg
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                (if (pal.dark) 0 else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)

        val root = FrameLayout(this)
        root.setBackgroundColor(pal.fBg)

        root.addView(HeroView(this, pal.dark), FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(360f)))

        // Titre + sous-titre
        root.addView(tv("نجوم تيليجرام", 31f, pal.fTitle, true).apply { gravity = Gravity.CENTER },
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = px(350f) })

        val l1 = "قم بشراء النجوم لفتح المحتوى والخدمات\n"
        val l2 = "في التطبيقات المصغرة على تيليجرام. "
        val link = "المزيد عن النجوم"
        val sub = SpannableString(l1 + l2 + link + " ›")
        sub.setSpan(ForegroundColorSpan(0xFF1AA85F.toInt()), l1.length + l2.length, l1.length + l2.length + link.length + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        root.addView(tv(sub, 21f, pal.fSub).apply { gravity = Gravity.CENTER; setLineSpacing(0f, 1.3f) },
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = px(396f) })

        // Carte du solde
        val card = FrameLayout(this).apply {
            background = GradientDrawable().apply { setColor(pal.fCard); cornerRadius = 40f * k }
        }
        card.addView(FeatherIconView(this, pal.dark), FrameLayout.LayoutParams(px(70f), px(70f)).apply { leftMargin = px(247f); topMargin = px(33f) })
        card.addView(tv("٠", 36f, pal.fTitle, true),
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { leftMargin = px(326f); topMargin = px(48f) })
        card.addView(tv("رصيدك", 21f, pal.fGrey).apply { gravity = Gravity.CENTER },
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = px(97f) })
        card.addView(tv("شراء النجوم", 24f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = GradientDrawable().apply { setColor(green); cornerRadius = 80f * k }
            setOnClickListener { Toast.makeText(this@FeatherActivity, "شراء النجوم", Toast.LENGTH_SHORT).show() }
        }, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(73f)).apply {
            leftMargin = px(30f); rightMargin = px(30f); topMargin = px(150f)
        })
        val gift = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            addView(tv("إهداء النجوم للأصدقاء", 21f, 0xFF1BB46B.toInt()))
            addView(GiftIconView(this@FeatherActivity, 0xFF1BB46B.toInt()),
                LinearLayout.LayoutParams(px(38f), px(38f)).apply { leftMargin = px(12f) })
        }
        card.addView(gift, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(44f)).apply { topMargin = px(246f) })
        root.addView(card, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(322f)).apply {
            leftMargin = px(18f); rightMargin = px(18f); topMargin = px(522f)
        })

        // Flèche retour
        val back = ChevronView(this, k, 3, pal.fTitle)
        root.addView(back, FrameLayout.LayoutParams(px(90f), px(90f)).apply { topMargin = px(40f) })
        back.setOnClickListener { finish() }
        root.setOnApplyWindowInsetsListener { _, ins ->
            val cy = max(85f * k, ins.systemWindowInsetTop + 35f * k)
            (back.layoutParams as FrameLayout.LayoutParams).topMargin = (cy - 45f * k).toInt()
            back.requestLayout()
            ins
        }

        setContentView(root)
    }
}
