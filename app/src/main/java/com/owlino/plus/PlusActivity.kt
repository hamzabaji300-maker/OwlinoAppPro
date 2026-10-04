package com.owlino.plus

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class PlusActivity : Activity() {

    private class Feat(val title: String, val desc: String, val kind: Int, val color: Int)

    private val feats = listOf(
        Feat("Supprimer des messages traces", "Supprimez un message envoyé sans qu'aucune trace ne reste dans la conversation – une exclusivité Owlino Plus", 0, 0xFF7B5CF0.toInt()),
        Feat("Quittez le groupe discrètement", "Quittez n'importe quel groupe sans laisser de traces – une exclusivité Owlino Plus", 1, 0xFF7762EE.toInt()),
        Feat("Modifier sans laisser de traces", "Modifiez vos messages sans que la mention « modifié » n'apparaisse – une exclusivité Owlino Plus", 2, 0xFF6C79F0.toInt()),
        Feat("Lire les messages vocaux", "Transformez les messages vocaux en texte pour les lire discrètement", 3, 0xFF5A8CF0.toInt()),
        Feat("Owlino sans publicité", "Profitez d'Owlino sans aucune publicité", 4, 0xFF4B9CF0.toInt()),
        Feat("Paramètres du Mode invisible", "Contrôlez qui voit votre statut en ligne et votre dernière connexion", 5, 0xFF3FA8EE.toInt()),
        Feat("Badge spécial", "Affichez un badge exclusif Owlino Plus sur votre profil", 6, 0xFF35B4E8.toInt()),
        Feat("Icônes d'appli uniques", "Personnalisez l'icône de l'application avec des designs exclusifs", 7, 0xFF2BC0DE.toInt()),
        Feat("Accès gratuit aux stickers payants", "Utilisez gratuitement tous les stickers payants", 8, 0xFF22C8D6.toInt()),
        Feat("Discussion en direct avec l'Assistance", "Discutez en direct avec notre équipe d'assistance", 9, 0xFF1ED0CF.toInt())
    )

    private var k = 1f
    private var annual = false
    private var sb = 0
    private var nb = 0
    private var termsShown = true

    private lateinit var priceLine: TextView
    private lateinit var terms: TextView
    private lateinit var radioM: RadioView
    private lateinit var radioA: RadioView

    private fun px(v: Float) = (v * k).toInt()

    private fun tv(text: CharSequence, size: Float, color: Int, bold: Boolean = false): TextView =
        TextView(this).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_PX, size * k)
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
            includeFontPadding = false
        }

    private fun two(a: String, b: String, ca: Int, cb: Int, boldA: Boolean = false): SpannableStringBuilder {
        val s = SpannableStringBuilder(a + b)
        s.setSpan(ForegroundColorSpan(ca), 0, a.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        s.setSpan(ForegroundColorSpan(cb), a.length, a.length + b.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        if (boldA) s.setSpan(android.text.style.StyleSpan(Typeface.BOLD), 0, a.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return s
    }

    private fun priceText() =
        if (annual) "DZD 2075.00/an ensuite. Annulez à tout moment." else "DZD 275.00/mois ensuite. Annulez à tout moment."

    private fun termsText(): CharSequence {
        val a = "En appuyant sur S'abonner, vous acceptez nos "
        val link = "Conditions générales"
        val b = ".\nVotre abonnement se renouvellera automatiquement pour le même prix et la même offre jusqu'à ce que vous annuliez via Google Play"
        val s = SpannableString(a + link + b)
        s.setSpan(ForegroundColorSpan(0xFF2E8BFF.toInt()), a.length, a.length + link.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        s.setSpan(UnderlineSpan(), a.length, a.length + link.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return s
    }

    private fun greenButton(): TextView = tv("Essayer 14 jours gratuitement", 24f, Color.WHITE, true).apply {
        gravity = Gravity.CENTER
        background = GradientDrawable().apply {
            setColor(0xFF3AAE2E.toInt())
            cornerRadius = 60f * k
        }
        setOnClickListener { Toast.makeText(this@PlusActivity, "Owlino Plus", Toast.LENGTH_SHORT).show() }
    }

    private fun select(a: Boolean) {
        annual = a
        radioM.sel = !a
        radioA.sel = a
        radioM.invalidate()
        radioA.invalidate()
        priceLine.text = priceText()
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        k = resources.displayMetrics.widthPixels / 591f
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.BLACK
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        val root = FrameLayout(this)
        root.setBackgroundColor(Color.BLACK)

        // --- Fond : en-tête, logo, sous-titre
        val header = HeaderView(this)
        root.addView(header, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val logo = LogoView(this)
        root.addView(logo, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(120f)).apply { topMargin = px(70f) })

        val sub = tv("Déverrouillez des fonctionnalités exclusives", 22f, Color.WHITE).apply { gravity = Gravity.CENTER }
        root.addView(sub, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(30f)).apply { topMargin = px(160f) })

        // --- Carte des offres
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply { setColor(0xFF2B2E33.toInt()); cornerRadius = 20f * k }
            translationZ = 0.5f
        }
        radioM = RadioView(this, k).apply { sel = true }
        radioA = RadioView(this, k)

        val rowM = FrameLayout(this)
        rowM.addView(radioM, FrameLayout.LayoutParams(px(44f), px(44f), Gravity.CENTER_VERTICAL).apply { leftMargin = px(20f) })
        rowM.addView(tv("Abon. mensuel", 24f, Color.WHITE, true),
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL).apply { leftMargin = px(66f) })
        rowM.addView(tv(two("DZD275.00", "/mois", Color.WHITE, 0xFF9A9DA3.toInt()), 22f, Color.WHITE),
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.END or Gravity.CENTER_VERTICAL).apply { rightMargin = px(24f) })
        rowM.setOnClickListener { select(false) }

        val rowA = FrameLayout(this)
        rowA.addView(radioA, FrameLayout.LayoutParams(px(44f), px(44f), Gravity.CENTER_VERTICAL).apply { leftMargin = px(20f) })
        val titleA = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(tv("Abon. annuel", 24f, Color.WHITE, true))
            addView(tv("- 37%", 21f, Color.WHITE, true).apply {
                gravity = Gravity.CENTER
                setPadding(px(14f), px(5f), px(14f), px(5f))
                background = GradientDrawable().apply { setColor(0xFF5B3FD0.toInt()); cornerRadius = 30f * k }
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { leftMargin = px(14f) })
        }
        rowA.addView(titleA, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL).apply { leftMargin = px(66f) })
        val grey = 0xFF9A9DA3.toInt()
        val annualPrice = SpannableStringBuilder().apply {
            append(two("DZD172.92", "/mois", Color.WHITE, grey, true))
            append("\n")
            val l2 = "DZD2075.00/an"
            val st = length
            append(l2)
            setSpan(ForegroundColorSpan(grey), st, st + l2.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        rowA.addView(tv(annualPrice, 22f, Color.WHITE).apply { gravity = Gravity.END; setLineSpacing(0f, 1.25f) },
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.END or Gravity.CENTER_VERTICAL).apply { rightMargin = px(24f) })
        rowA.setOnClickListener { select(true) }

        card.addView(rowM, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(96f)))
        card.addView(View(this).apply { setBackgroundColor(0xFF383B40.toInt()) }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1))
        card.addView(rowA, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(96f)))

        // --- Liste scrollable
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(View(this), LinearLayout.LayoutParams(1, px(415f)))
        content.addView(card, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            leftMargin = px(24f); rightMargin = px(24f)
        })
        content.addView(View(this), LinearLayout.LayoutParams(1, px(24f)))
        for ((i, f) in feats.withIndex()) {
            val last = i == feats.size - 1
            val row = FrameLayout(this)
            row.addView(IconView(this, f.kind, f.color), FrameLayout.LayoutParams(px(54f), px(54f), Gravity.CENTER_VERTICAL).apply { leftMargin = px(48f) })
            row.addView(tv(f.title, 24f, Color.WHITE),
                FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL).apply {
                    leftMargin = px(121f); rightMargin = px(110f)
                })
            row.addView(ChevronView(this, k, 0, 0xFF8A8D93.toInt()), FrameLayout.LayoutParams(px(40f), px(40f), Gravity.END or Gravity.CENTER_VERTICAL).apply { rightMargin = px(38f) })
            row.setOnClickListener { openSheet(i) }
            content.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(if (last) 120f else 84f)))
        }
        content.addView(View(this), LinearLayout.LayoutParams(1, px(330f)))

        val scroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            addView(content)
        }
        root.addView(scroll, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        // --- Barre du bas
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0x00000000, 0xFF000000.toInt(), 0xFF000000.toInt())
            )
            setPadding(0, px(60f), 0, px(20f))
        }
        bar.addView(greenButton(), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(72f)).apply {
            leftMargin = px(24f); rightMargin = px(24f)
        })
        priceLine = tv(priceText(), 21f, Color.WHITE).apply { gravity = Gravity.CENTER }
        bar.addView(priceLine, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = px(14f) })
        terms = tv(termsText(), 20f, 0xFF9A9DA3.toInt()).apply { gravity = Gravity.CENTER }
        bar.addView(terms, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = px(14f); leftMargin = px(30f); rightMargin = px(30f)
        })
        root.addView(bar, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))

        // --- Flèche retour
        val back = ChevronView(this, k, 1, Color.WHITE)
        root.addView(back, FrameLayout.LayoutParams(px(90f), px(90f)).apply { topMargin = px(30f) })
        back.setOnClickListener { finish() }

        root.setOnApplyWindowInsetsListener { _, ins ->
            sb = ins.systemWindowInsetTop
            nb = ins.systemWindowInsetBottom
            val cy = max(75f * k, sb + 35f * k)
            (back.layoutParams as FrameLayout.LayoutParams).topMargin = (cy - 45f * k).toInt()
            back.requestLayout()
            bar.setPadding(0, px(60f), 0, px(20f) + nb)
            ins
        }

        // --- Effet de défilement : en-tête qui se réduit, carte épinglée
        scroll.setOnScrollChangeListener { _, _, y, _, _ ->
            val s = y.toFloat()
            val m = 150f * k
            header.translationY = -0.4f * min(s, m)
            logo.translationY = -0.15f * min(s, m)
            sub.alpha = max(0f, 1f - s / (60f * k))
            card.translationY = max(0f, s - m)
            val show = y <= 8
            if (show != termsShown) {
                termsShown = show
                terms.visibility = if (show) View.VISIBLE else View.GONE
            }
        }

        setContentView(root)
    }

    // --- Bottom sheet (détail d'une fonctionnalité)
    private fun openSheet(start: Int) {
        var idx = start
        val dlg = Dialog(this, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar)
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val sheet = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = GradientDrawable().apply {
                setColor(0xFF2B2E33.toInt())
                cornerRadii = floatArrayOf(40f * k, 40f * k, 40f * k, 40f * k, 0f, 0f, 0f, 0f)
            }
            setPadding(0, 0, 0, px(30f) + nb)
        }
        val chev = ChevronView(this, k, 2, 0xFFB0B3B8.toInt())
        chev.setOnClickListener { dlg.dismiss() }
        sheet.addView(chev, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(64f)))

        val big = BigIconView(this, k)
        sheet.addView(big, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(290f)))

        val title = tv("", 26f, Color.WHITE, true).apply { gravity = Gravity.CENTER }
        sheet.addView(title, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = px(40f); leftMargin = px(30f); rightMargin = px(30f)
        })
        val desc = tv("", 22f, 0xFF9A9DA3.toInt()).apply { gravity = Gravity.CENTER; setLineSpacing(0f, 1.15f) }
        sheet.addView(desc, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = px(14f); leftMargin = px(40f); rightMargin = px(40f)
        })
        val dots = DotsView(this, k, feats.size)
        sheet.addView(dots, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(16f)).apply { topMargin = px(30f) })

        sheet.addView(greenButton(), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(72f)).apply {
            topMargin = px(40f); leftMargin = px(24f); rightMargin = px(24f)
        })
        sheet.addView(tv(priceText(), 21f, Color.WHITE).apply { gravity = Gravity.CENTER },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = px(14f) })
        sheet.addView(tv(termsText(), 20f, 0xFF9A9DA3.toInt()).apply { gravity = Gravity.CENTER },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = px(14f); leftMargin = px(30f); rightMargin = px(30f)
            })

        fun bind() {
            val f = feats[idx]
            big.kind = f.kind
            big.invalidate()
            title.text = f.title
            desc.text = f.desc
            dots.active = idx
            dots.invalidate()
        }
        bind()

        // Balayage gauche/droite pour passer d'une fonctionnalité à l'autre
        val gd = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                if (abs(vx) > abs(vy) && abs(vx) > 300f) {
                    val n = if (vx < 0) idx + 1 else idx - 1
                    if (n in feats.indices) {
                        idx = n
                        bind()
                    }
                    return true
                }
                return false
            }
        })
        sheet.setOnTouchListener { _, e -> gd.onTouchEvent(e); true }

        dlg.setContentView(sheet)
        dlg.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.BOTTOM)
            setDimAmount(0.6f)
        }
        dlg.show()
    }
}
