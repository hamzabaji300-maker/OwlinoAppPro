package com.example.ui.plusui

import android.app.Activity
import android.content.ContextWrapper
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
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlin.math.max

/** Copie de FeatherActivity : hébergée dans Compose via AndroidView (plus d'Activity, onBack remplace finish()). */
class FeatherActivity(private val act: Activity, private val onBack: () -> Unit) : ContextWrapper(act) {
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
    fun build(): View {
        k = resources.displayMetrics.widthPixels / 591f
        val pal = Pal.load(this)
        val green = 0xFF19BD6B.toInt()

        val root = FrameLayout(this)
        root.setBackgroundColor(pal.fBg)

        // Contenu défilant (hauteur fixe dans le repère k)
        val content = FrameLayout(this)

        content.addView(HeroView(this, pal.dark), FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(360f)))

        // Titre + sous-titre
        content.addView(tv("ريش أولينو", 31f, pal.fTitle, true).apply { gravity = Gravity.CENTER },
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = px(350f) })

        val l1 = "قم بشراء الريش لفتح المحتوى والخدمات\n"
        val l2 = "في تطبيق أولينو. "
        val link = "المزيد عن الريش"
        val sub = SpannableString(l1 + l2 + link + " ›")
        sub.setSpan(ForegroundColorSpan(0xFF1AA85F.toInt()), l1.length + l2.length, l1.length + l2.length + link.length + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        content.addView(tv(sub, 21f, pal.fSub).apply { gravity = Gravity.CENTER; setLineSpacing(0f, 1.3f) },
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = px(396f) })

        // Carte du solde
        val card = FrameLayout(this).apply {
            background = GradientDrawable().apply { setColor(pal.fCard); cornerRadius = 40f * k }
        }
        card.addView(GoldFeatherIconView(this, pal.dark), FrameLayout.LayoutParams(px(70f), px(70f)).apply { leftMargin = px(247f); topMargin = px(33f) })
        card.addView(tv("٠", 36f, pal.fTitle, true),
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { leftMargin = px(326f); topMargin = px(48f) })
        card.addView(tv("رصيدك", 21f, pal.fGrey).apply { gravity = Gravity.CENTER },
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = px(97f) })
        card.addView(tv("شراء الريش", 24f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = GradientDrawable().apply { setColor(green); cornerRadius = 80f * k }
            setOnClickListener { BuyFeathers(act, k, pal).start() }
        }, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(73f)).apply {
            leftMargin = px(30f); rightMargin = px(30f); topMargin = px(150f)
        })
        val gift = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            addView(tv("إهداء الريش للأصدقاء", 21f, 0xFF1BB46B.toInt()))
            addView(GiftIconView(this@FeatherActivity, 0xFF1BB46B.toInt()),
                LinearLayout.LayoutParams(px(38f), px(38f)).apply { leftMargin = px(12f) })
        }
        gift.setOnClickListener { GiftFlow(act, k, pal).start() }
        card.addView(gift, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(44f)).apply { topMargin = px(246f) })
        content.addView(card, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(322f)).apply {
            leftMargin = px(18f); rightMargin = px(18f); topMargin = px(522f)
        })

        // Carte du code
        val codeCard = FrameLayout(this).apply {
            background = GradientDrawable().apply { setColor(pal.fCard); cornerRadius = 40f * k }
        }
        codeCard.addView(tv("هل لديك كود؟", 26f, pal.fTitle, true).apply { gravity = Gravity.CENTER },
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = px(30f) })
        codeCard.addView(tv("ألصق الكود هنا للحصول على الريشات", 20f, pal.fGrey).apply { gravity = Gravity.CENTER },
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = px(72f) })
        val input = EditText(this).apply {
            hint = "ألصق الكود هنا"
            setHintTextColor(pal.fGrey)
            setTextColor(pal.fTitle)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, 21f * k)
            isSingleLine = true
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            setPadding(px(24f), 0, px(24f), 0)
            background = GradientDrawable().apply {
                setColor(pal.fBg)
                cornerRadius = 30f * k
            }
        }
        codeCard.addView(input, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(70f)).apply {
            leftMargin = px(30f); rightMargin = px(30f); topMargin = px(115f)
        })
        codeCard.addView(tv("تفعيل الكود", 24f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = GradientDrawable().apply { setColor(green); cornerRadius = 80f * k }
            setOnClickListener {
                val code = input.text.toString().trim()
                val msg = if (code.isEmpty()) "الرجاء لصق الكود أولاً" else "تم استلام الكود: $code"
                Toast.makeText(this@FeatherActivity, msg, Toast.LENGTH_SHORT).show()
            }
        }, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(73f)).apply {
            leftMargin = px(30f); rightMargin = px(30f); topMargin = px(205f)
        })
        content.addView(codeCard, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(305f)).apply {
            leftMargin = px(18f); rightMargin = px(18f); topMargin = px(866f)
        })

        val scroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            clipToPadding = false
            addView(content, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(1200f)))
        }
        root.addView(scroll, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        input.setOnFocusChangeListener { _, has ->
            if (has) scroll.postDelayed({ scroll.smoothScrollTo(0, content.height) }, 300)
        }

        // Flèche retour
        val back = ChevronView(this, k, 3, pal.fTitle)
        root.addView(back, FrameLayout.LayoutParams(px(90f), px(90f)).apply { topMargin = px(40f) })
        back.setOnClickListener { onBack() }
        root.setOnApplyWindowInsetsListener { _, ins ->
            val cy = max(85f * k, ins.systemWindowInsetTop + 35f * k)
            (back.layoutParams as FrameLayout.LayoutParams).topMargin = (cy - 45f * k).toInt()
            back.requestLayout()
            // Barre de navigation + clavier : l'espace du bas suit pour garder le champ accessible
            scroll.setPadding(0, 0, 0, ins.systemWindowInsetBottom)
            ins
        }

        return root
    }
}
