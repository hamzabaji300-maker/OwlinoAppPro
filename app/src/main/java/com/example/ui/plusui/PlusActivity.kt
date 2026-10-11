package com.example.ui.plusui

import android.app.Activity
import android.content.ContextWrapper
import android.app.Dialog
import android.graphics.Color
import android.graphics.Rect
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
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Copie de PlusActivity : hébergée dans Compose via AndroidView (onBack remplace finish()). */
class PlusActivity(private val act: Activity, private val onBack: () -> Unit) : ContextWrapper(act) {

    private class Feat(val title: String, val desc: String, val kind: Int, val color: Int)

    private val feats = listOf(
        Feat("Supprimer des messages traces", "Supprimez un message envoyé sans qu'aucune trace ne reste dans la conversation – une exclusivité Owlino Pro", 0, 0xFF7B5CF0.toInt()),
        Feat("Quittez le groupe discrètement", "Quittez n'importe quel groupe sans laisser de traces – une exclusivité Owlino Pro", 1, 0xFF7762EE.toInt()),
        Feat("Modifier sans laisser de traces", "Modifiez vos messages sans que la mention « modifié » n'apparaisse – une exclusivité Owlino Pro", 2, 0xFF6C79F0.toInt()),
        Feat("Lire les messages vocaux", "Transformez les messages vocaux en texte pour les lire discrètement", 3, 0xFF5A8CF0.toInt()),
        Feat("Owlino sans publicité", "Profitez d'Owlino sans aucune publicité", 4, 0xFF4B9CF0.toInt()),
        Feat("Paramètres du Mode invisible", "Contrôlez qui voit votre statut en ligne et votre dernière connexion", 5, 0xFF3FA8EE.toInt()),
        Feat("Badge spécial", "Affichez un badge exclusif Owlino Pro sur votre profil", 6, 0xFF35B4E8.toInt()),
        Feat("Icônes d'appli uniques", "Personnalisez l'icône de l'application avec des designs exclusifs", 7, 0xFF2BC0DE.toInt()),
        Feat("Accès gratuit aux stickers payants", "Utilisez gratuitement tous les stickers payants", 8, 0xFF22C8D6.toInt()),
        Feat("Discussion en direct avec l'Assistance", "Discutez en direct avec notre équipe d'assistance", 9, 0xFF1ED0CF.toInt())
    )

    private var k = 1f
    private var annual = false
    private var sb = 0
    private var nb = 0
    private lateinit var pal: Pal
    private var clipTop = 0f
    private lateinit var scrollV: ScrollView

    private lateinit var priceLine: TextView
    private val rowViews = ArrayList<View>()
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

    private fun greenButton(onClick: () -> Unit): TextView = tv("ادفع", 24f, Color.WHITE, true).apply {
        gravity = Gravity.CENTER
        background = GradientDrawable().apply {
            setColor(0xFF3AAE2E.toInt())
            cornerRadius = 60f * k
        }
        setOnClickListener { onClick() }
    }

    private var sheetPrice: TextView? = null

    private fun select(a: Boolean) {
        annual = a
        radioM.sel = !a
        radioA.sel = a
        radioM.invalidate()
        radioA.invalidate()
        priceLine.text = priceText()
        sheetPrice?.text = priceText()
    }

    /** Carte de choix mensuel / annuel (utilisée dans le bottom sheet de paiement). */
    private fun planCard(): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply { setColor(pal.card); cornerRadius = 20f * k }
        }
        radioM = RadioView(this, k, pal.radioOff).apply { sel = !annual }
        radioA = RadioView(this, k, pal.radioOff).apply { sel = annual }

        val rowM = FrameLayout(this)
        rowM.addView(radioM, FrameLayout.LayoutParams(px(44f), px(44f), Gravity.CENTER_VERTICAL).apply { leftMargin = px(20f) })
        rowM.addView(tv("Abon. mensuel", 24f, pal.text, true),
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL).apply { leftMargin = px(66f) })
        rowM.addView(tv(two("DZD275.00", "/mois", pal.text, pal.grey), 22f, pal.text),
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.END or Gravity.CENTER_VERTICAL).apply { rightMargin = px(24f) })
        rowM.setOnClickListener { select(false) }

        val rowA = FrameLayout(this)
        rowA.addView(radioA, FrameLayout.LayoutParams(px(44f), px(44f), Gravity.CENTER_VERTICAL).apply { leftMargin = px(20f) })
        val titleA = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(tv("Abon. annuel", 24f, pal.text, true))
            addView(tv("- 37%", 21f, Color.WHITE, true).apply {
                gravity = Gravity.CENTER
                setPadding(px(14f), px(5f), px(14f), px(5f))
                background = GradientDrawable().apply { setColor(0xFF5B3FD0.toInt()); cornerRadius = 30f * k }
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { leftMargin = px(14f) })
        }
        rowA.addView(titleA, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL).apply { leftMargin = px(66f) })
        val grey = pal.grey
        val annualPrice = SpannableStringBuilder().apply {
            append(two("DZD172.92", "/mois", pal.text, grey, true))
            append("\n")
            val l2 = "DZD2075.00/an"
            val st = length
            append(l2)
            setSpan(ForegroundColorSpan(grey), st, st + l2.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        rowA.addView(tv(annualPrice, 22f, pal.text).apply { gravity = Gravity.END; setLineSpacing(0f, 1.25f) },
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.END or Gravity.CENTER_VERTICAL).apply { rightMargin = px(24f) })
        rowA.setOnClickListener { select(true) }

        card.addView(rowM, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(96f)))
        card.addView(View(this).apply { setBackgroundColor(pal.divider) }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1))
        card.addView(rowA, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(96f)))
        return card
    }

    // --- Bottom sheet de paiement (choix de l'offre) avec animation de bas en haut
    private fun openPaySheet() {
        val dlg = Dialog(act, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar)
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val sheet = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = GradientDrawable().apply {
                setColor(pal.sheet)
                cornerRadii = floatArrayOf(40f * k, 40f * k, 40f * k, 40f * k, 0f, 0f, 0f, 0f)
            }
            setPadding(0, 0, 0, px(30f) + nb)
        }
        val chev = ChevronView(this, k, 2, 0xFFB0B3B8.toInt())
        chev.setOnClickListener { dlg.dismiss() }
        sheet.addView(chev, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(64f)))

        sheet.addView(planCard(), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = px(10f); leftMargin = px(24f); rightMargin = px(24f)
        })

        sheet.addView(greenButton {
            val plan = if (annual) "Abon. annuel – DZD 2075.00/an" else "Abon. mensuel – DZD 275.00/mois"
            Toast.makeText(this, "Owlino Pro : $plan", Toast.LENGTH_SHORT).show()
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(72f)).apply {
            topMargin = px(40f); leftMargin = px(24f); rightMargin = px(24f)
        })
        val sp = tv(priceText(), 21f, pal.text).apply { gravity = Gravity.CENTER }
        sheetPrice = sp
        sheet.addView(sp, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = px(14f) })
        dlg.setOnDismissListener { sheetPrice = null }

        // Animation : glisse de bas en haut
        sheet.visibility = View.INVISIBLE
        sheet.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
            override fun onLayoutChange(v: View, l: Int, t: Int, r: Int, b: Int, ol: Int, ot: Int, or2: Int, ob: Int) {
                v.removeOnLayoutChangeListener(this)
                v.translationY = (b - t).toFloat()
                v.visibility = View.VISIBLE
                v.animate().translationY(0f).setDuration(350).setInterpolator(DecelerateInterpolator(2f)).start()
            }
        })

        dlg.setContentView(sheet)
        dlg.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.BOTTOM)
            setDimAmount(0.6f)
        }
        dlg.show()
    }

    @Suppress("DEPRECATION")
    fun build(): View {
        k = resources.displayMetrics.widthPixels / 591f
        pal = Pal.load(this)

        val root = FrameLayout(this)
        root.setBackgroundColor(pal.bg)

        // --- Fond : en-tête, logo, sous-titre
        val header = HeaderView(this, pal.bg, pal.dark)
        root.addView(header, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val logo = LogoView(this)
        root.addView(logo, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(120f)).apply { topMargin = px(70f) })

        val sub = tv("Déverrouillez des fonctionnalités exclusives", 22f, Color.WHITE).apply { gravity = Gravity.CENTER }
        root.addView(sub, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(30f)).apply { topMargin = px(160f) })

        // --- Liste scrollable
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        // Espace réservé réduit : la liste commence juste sous le sous-titre
        content.addView(View(this), LinearLayout.LayoutParams(1, px(319f)))
        for ((i, f) in feats.withIndex()) {
            val last = i == feats.size - 1
            val row = FrameLayout(this)
            row.addView(IconView(this, f.kind, f.color), FrameLayout.LayoutParams(px(54f), px(54f), Gravity.CENTER_VERTICAL).apply { leftMargin = px(48f) })
            row.addView(tv(f.title, 24f, pal.text),
                FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL).apply {
                    leftMargin = px(121f); rightMargin = px(110f)
                })
            row.addView(ChevronView(this, k, 0, pal.chev), FrameLayout.LayoutParams(px(40f), px(40f), Gravity.END or Gravity.CENTER_VERTICAL).apply { rightMargin = px(38f) })
            rowViews.add(row)
            row.setOnClickListener { if (row.alpha > 0.3f) openSheet(i) }
            content.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(if (last) 120f else 84f)))
        }
        content.addView(View(this), LinearLayout.LayoutParams(1, px(330f)))

        val scroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            addView(content)
        }
        scrollV = scroll
        root.addView(scroll, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        clipTop = 0f

        // --- Barre du bas
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(pal.bgTransparent, pal.bg, pal.bg)
            )
            setPadding(0, px(60f), 0, px(20f))
        }
        bar.addView(greenButton { openPaySheet() }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(72f)).apply {
            leftMargin = px(24f); rightMargin = px(24f)
        })
        priceLine = tv(priceText(), 21f, pal.text).apply { gravity = Gravity.CENTER }
        bar.addView(priceLine, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = px(14f) })
        root.addView(bar, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))

        // --- Flèche retour
        val back = ChevronView(this, k, 1, Color.WHITE)
        root.addView(back, FrameLayout.LayoutParams(px(90f), px(90f)).apply { topMargin = px(30f) })
        back.setOnClickListener { onBack() }

        root.setOnApplyWindowInsetsListener { _, ins ->
            sb = ins.systemWindowInsetTop
            nb = ins.systemWindowInsetBottom
            val cy = max(75f * k, sb + 35f * k)
            (back.layoutParams as FrameLayout.LayoutParams).topMargin = (cy - 45f * k).toInt()
            back.requestLayout()
            bar.setPadding(0, px(60f), 0, px(20f) + nb)
            ins
        }

        // --- Effet de défilement : l'en-tête (ciel, baleine, nuages) reste fixe ;
        // les lignes s'effacent progressivement en arrivant au niveau des nuages.
        fun onScrolled(y: Int) {
            val full = 361f * k // centre de la 1re ligne au repos : pleinement visible
            val zero = 296f * k // sommet des nuages : totalement effacée
            clipTop = zero
            for (r in rowViews) {
                val cy = r.top - y + r.height / 2f
                r.alpha = ((cy - zero) / (full - zero)).coerceIn(0f, 1f)
            }
        }
        scroll.setOnScrollChangeListener { _, _, y, _, _ -> onScrolled(y) }
        root.post { onScrolled(0) }

        return root
    }

    // --- Bottom sheet (détail d'une fonctionnalité) avec pager fluide
    private fun openSheet(start: Int) {
        val dlg = Dialog(act, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar)
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val w = resources.displayMetrics.widthPixels

        val sheet = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = GradientDrawable().apply {
                setColor(pal.sheet)
                cornerRadii = floatArrayOf(40f * k, 40f * k, 40f * k, 40f * k, 0f, 0f, 0f, 0f)
            }
            setPadding(0, 0, 0, px(30f) + nb)
        }
        val chev = ChevronView(this, k, 2, 0xFFB0B3B8.toInt())
        chev.setOnClickListener { dlg.dismiss() }
        sheet.addView(chev, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(64f)))

        val strip = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for (f in feats) {
            val page = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            page.addView(BigIconView(this, k).apply { kind = f.kind },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(290f)))
            page.addView(tv(f.title, 26f, pal.text, true).apply { gravity = Gravity.CENTER },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = px(40f); leftMargin = px(30f); rightMargin = px(30f)
                })
            page.addView(tv(f.desc, 22f, pal.grey).apply { gravity = Gravity.CENTER; setLineSpacing(0f, 1.15f) },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = px(14f); leftMargin = px(40f); rightMargin = px(40f)
                })
            strip.addView(page, LinearLayout.LayoutParams(w, ViewGroup.LayoutParams.MATCH_PARENT))
        }

        val dots = DotsView(this, k, feats.size, pal.dotOff, pal.dotOn)
        val pager = Pager(this, strip, w, feats.size) { p ->
            dots.progress = p
            dots.invalidate()
        }
        sheet.addView(pager, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(470f)))
        pager.jumpTo(start)

        sheet.addView(dots, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(20f)).apply { topMargin = px(26f) })

        sheet.addView(greenButton { Toast.makeText(this@PlusActivity, "Owlino Pro", Toast.LENGTH_SHORT).show() }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(72f)).apply {
            topMargin = px(40f); leftMargin = px(24f); rightMargin = px(24f)
        })
        sheet.addView(tv(priceText(), 21f, pal.text).apply { gravity = Gravity.CENTER },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = px(14f) })
        sheet.addView(tv(termsText(), 20f, pal.grey).apply { gravity = Gravity.CENTER },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = px(14f); leftMargin = px(30f); rightMargin = px(30f)
            })

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
