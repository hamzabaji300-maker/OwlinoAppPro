package com.example.ui.plusui

import android.animation.ValueAnimator
import android.app.Activity
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.Window
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Flux « Offrir des plumes aux amis » (même enchaînement que Telegram) :
 * 1) feuille de choix de l'ami  2) feuille des packs de plumes.
 * Tout est dessiné au Canvas, aucune image, aucune librairie.
 */
class GiftFlow(private val act: Activity, private val k: Float, private val pal: Pal) {

    class Contact(val name: String, val c1: Int, val c2: Int)
    private class Item(val name: String, val kind: Int, val letter: String, val c1: Int, val c2: Int, val contact: Contact?)
    private class Pack(val label: String, val price: String, val count: Int, val scale: Float)

    companion object {
        const val GREEN = 0xFF1AA85F.toInt()
        const val LINK = "https://owlino.app/feathers/gift/demo"

        /** Contacts de démonstration : liste fixe, facile à modifier. */
        val CONTACTS = listOf(
            Contact("Hani", 0xFF4FB5E8.toInt(), 0xFF2E86DE.toInt()),
            Contact("أحمد", 0xFFFF9A5A.toInt(), 0xFFE8603C.toInt()),
            Contact("سارة", 0xFFF06FA8.toInt(), 0xFFC94A8A.toInt()),
            Contact("يوسف", 0xFF7B8BF0.toInt(), 0xFF5A5FD8.toInt()),
            Contact("مريم", 0xFFB67BEF.toInt(), 0xFF8B54D6.toInt()),
            Contact("كريم", 0xFF3FC9A0.toInt(), 0xFF1F9E78.toInt()),
            Contact("ليلى", 0xFFF5B84B.toInt(), 0xFFE08A1E.toInt()),
            Contact("عمر", 0xFF58C0D8.toInt(), 0xFF3D8FB8.toInt()),
            Contact("نور", 0xFFEF7C7C.toInt(), 0xFFC95454.toInt()),
            Contact("ياسين", 0xFF8BC34A.toInt(), 0xFF5E9E2E.toInt())
        )
    }

    private val packs = listOf(
        Pack("١٠٠ ريشة", "٣١٩٫٠٠ د.ج.", 1, 1f),
        Pack("٢٥٠ ريشة", "٧٩٩٫٠٠ د.ج.", 1, 1.2f),
        Pack("٥٠٠ ريشة", "١٬٥٩٩٫٠٠ د.ج.", 2, 1f),
        Pack("١ ٠٠٠ ريشة", "٣٬١٩٩٫٠٠ د.ج.", 3, 1f),
        Pack("٢ ٥٠٠ ريشة", "٧٬٩٩٩٫٠٠ د.ج.", 4, 1f),
        Pack("١٠ ٠٠٠ ريشة", "٣١٬٩٩٩٫٠٠ د.ج.", 5, 1f)
    )
    private val extraPacks = listOf(
        Pack("٢٥ ٠٠٠ ريشة", "٧٩٬٩٩٩٫٠٠ د.ج.", 5, 1f),
        Pack("٥٠ ٠٠٠ ريشة", "١٥٩٬٩٩٩٫٠٠ د.ج.", 5, 1f)
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

    private fun bottomDialog(heightFraction: Float, dim: Float): Dialog {
        val dlg = Dialog(act, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar)
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dlg.setCanceledOnTouchOutside(true)
        dlg.setCancelable(true)
        return dlg.also { d ->
            d.window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setGravity(Gravity.BOTTOM)
                setDimAmount(dim)
                setLayout(ViewGroup.LayoutParams.MATCH_PARENT, (act.resources.displayMetrics.heightPixels * heightFraction).toInt())
            }
        }
    }

    /** Pleine largeur + hauteur fixe, appliqué APRÈS setContentView (sinon la fenêtre flottante repasse en WRAP_CONTENT). */
    private fun applyLayout(dlg: Dialog, heightFraction: Float) {
        dlg.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, (act.resources.displayMetrics.heightPixels * heightFraction).toInt())
            setGravity(Gravity.BOTTOM)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    /** Glisse de bas en haut (350 ms). */
    private fun slideIn(sheet: View) {
        sheet.visibility = View.INVISIBLE
        sheet.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
            override fun onLayoutChange(v: View, l: Int, t: Int, r: Int, b: Int, ol: Int, ot: Int, or2: Int, ob: Int) {
                v.removeOnLayoutChangeListener(this)
                v.translationY = (b - t).toFloat()
                v.visibility = View.VISIBLE
                v.animate().translationY(0f).setDuration(350).setInterpolator(DecelerateInterpolator(2f)).start()
            }
        })
    }

    fun start() = showPicker()

    // ------------------------------------------------------------------ Étape 1 : choix de l'ami
    private fun showPicker() {
        val dlg = bottomDialog(0.6f, 0.5f)
        val sheetBg = pal.sheet
        val r = 40f * k

        val sheet = FrameLayout(act).apply {
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            background = GradientDrawable().apply {
                setColor(sheetBg)
                cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
            }
        }
        val col = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        sheet.addView(col, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        // Poignée
        col.addView(View(act).apply {
            background = GradientDrawable().apply { setColor(0xFFC4C7CC.toInt()); cornerRadius = 10f * k }
        }, LinearLayout.LayoutParams(px(64f), px(8f)).apply {
            gravity = Gravity.CENTER_HORIZONTAL; topMargin = px(14f); bottomMargin = px(18f)
        })

        // Champ de recherche (icône à droite)
        val searchBar = LinearLayout(act).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(if (pal.dark) 0xFF3A3D42.toInt() else 0xFFF1F2F5.toInt())
                cornerRadius = 60f * k
            }
        }
        val input = EditText(act).apply {
            hint = "إرسال إلى..."
            setHintTextColor(0xFF8E8E93.toInt())
            setTextColor(pal.text)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, 22f * k)
            isSingleLine = true
            gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
            background = null
            setPadding(px(10f), 0, px(10f), 0)
        }
        searchBar.addView(input, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        searchBar.addView(GiftSearchIconView(act, 0xFF8E8E93.toInt()),
            LinearLayout.LayoutParams(px(36f), px(36f)).apply { rightMargin = px(20f); leftMargin = px(8f) })
        col.addView(searchBar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(64f)).apply {
            leftMargin = px(24f); rightMargin = px(24f); bottomMargin = px(12f)
        })

        // Grille 4 colonnes, 1er élément à droite
        val grid = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(act).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            clipToPadding = false
            setPadding(0, 0, 0, px(110f))
            addView(grid)
        }
        col.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        val items = ArrayList<Item>().apply {
            add(Item("إعادة النشر في قصة", 0, "", 0xFF3FD0C8.toInt(), 0xFF2E96DE.toInt(), null))
            add(Item("الرسائل المحفوظة", 1, "", 0xFF52D184.toInt(), 0xFF2FB566.toInt(), null))
            for (c in CONTACTS) add(Item(c.name, 2, Character.toUpperCase(c.name[0]).toString(), c.c1, c.c2, c))
        }

        fun onItem(it: Item) {
            val c = it.contact
            if (c == null) {
                Toast.makeText(act, it.name, Toast.LENGTH_SHORT).show()
            } else {
                dlg.dismiss()
                showPacks(c.name, it.letter)
            }
        }

        fun cell(item: Item): View = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            addView(GiftAvatarView(act, item.kind, item.letter, item.c1, item.c2), LinearLayout.LayoutParams(px(64f), px(64f)))
            addView(tv(item.name, 17f, pal.fSub).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                maxLines = 2
                minLines = 2
                ellipsize = TextUtils.TruncateAt.END
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = px(8f); leftMargin = px(6f); rightMargin = px(6f)
            })
            setOnClickListener { onItem(item) }
        }

        fun rebuild(q: String) {
            grid.removeAllViews()
            val list = items.filter { q.isBlank() || it.name.contains(q.trim(), ignoreCase = true) }
            var i = 0
            while (i < list.size) {
                val row = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL }
                val cells = ArrayList<View>()
                for (j in 0 until 4) cells.add(if (i + j < list.size) cell(list[i + j]) else View(act))
                // RTL : le 1er élément est à droite
                for (v in cells.reversed()) row.addView(v, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                grid.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = px(10f); bottomMargin = px(10f)
                })
                i += 4
            }
        }
        rebuild("")
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) { rebuild(s?.toString() ?: "") }
        })

        // Bouton flottant « Copier le lien »
        sheet.addView(tv("نسخ الرابط", 22f, GREEN, true).apply {
            gravity = Gravity.CENTER
            background = GradientDrawable().apply { setColor(pal.fCard); cornerRadius = 80f * k }
            elevation = 10f * k
            setOnClickListener {
                val cm = act.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("owlino-gift-link", LINK))
                Toast.makeText(act, "تم نسخ الرابط", Toast.LENGTH_SHORT).show()
            }
        }, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(66f), Gravity.BOTTOM).apply {
            leftMargin = px(24f); rightMargin = px(24f); bottomMargin = px(22f)
        })

        slideIn(sheet)
        dlg.setContentView(sheet)
        applyLayout(dlg, 0.6f)
        dlg.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        dlg.show()
    }

    // ------------------------------------------------------------------ Étape 2 : packs de plumes
    private fun showPacks(name: String, letter: String) {
        val dlg = bottomDialog(0.8f, 0.6f)
        val r = 40f * k

        val sheet = FrameLayout(act).apply {
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(v: View, o: Outline) {
                    o.setRoundRect(0, 0, v.width, v.height + r.toInt(), r)
                }
            }
        }
        val scroll = ScrollView(act).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            isFillViewport = true
        }
        val content = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        sheet.addView(scroll, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        // ---- Section haute
        val top = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(pal.fBg)
        }
        top.addView(GiftSparkleView(act, k, letter),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(215f)))
        top.addView(tv("إهداء الريش", 30f, pal.fTitle, true).apply { gravity = Gravity.CENTER },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = px(6f) })

        val a = "مع الريش، سيتمكن "
        val b = " من فتح المحتوى\nوالخدمات في تطبيق أولينو. "
        val link = "رؤية أمثلة"
        val sub = SpannableStringBuilder(a + name + b + link)
        sub.setSpan(StyleSpan(Typeface.BOLD), a.length, a.length + name.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sub.setSpan(ForegroundColorSpan(pal.fTitle), a.length, a.length + name.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        val ls = a.length + name.length + b.length
        sub.setSpan(object : ClickableSpan() {
            override fun onClick(w: View) { Toast.makeText(act, "رؤية أمثلة", Toast.LENGTH_SHORT).show() }
            override fun updateDrawState(ds: TextPaint) { ds.color = GREEN; ds.isUnderlineText = false }
        }, ls, ls + link.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        top.addView(tv(sub, 20f, pal.fGrey).apply {
            gravity = Gravity.CENTER
            textDirection = View.TEXT_DIRECTION_RTL
            setLineSpacing(0f, 1.3f)
            movementMethod = LinkMovementMethod.getInstance()
            highlightColor = Color.TRANSPARENT
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = px(14f); bottomMargin = px(34f); leftMargin = px(30f); rightMargin = px(30f)
        })
        content.addView(top, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        // ---- Section basse
        val bottom = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(pal.fCard)
        }
        bottom.addView(tv("اختيار الحزمة", 20f, GREEN).apply { gravity = Gravity.RIGHT },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = px(26f); bottomMargin = px(14f); rightMargin = px(26f); leftMargin = px(26f)
            })
        for (p in packs) bottom.addView(packRow(p, name), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(72f)))

        // Contenu extensible
        val extra = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        for (p in extraPacks) extra.addView(packRow(p, name), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(72f)))
        bottom.addView(extra, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0))

        // Ligne « Plus d'options »
        val more = FrameLayout(act)
        more.addView(tv("خيارات أكثر", 21f, GREEN).apply { gravity = Gravity.RIGHT },
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.RIGHT or Gravity.CENTER_VERTICAL).apply { rightMargin = px(26f) })
        val arrow = ChevronView(act, k, 2, GREEN)
        more.addView(arrow, FrameLayout.LayoutParams(px(56f), px(56f), Gravity.LEFT or Gravity.CENTER_VERTICAL).apply { leftMargin = px(16f) })
        more.addView(View(act).apply { setBackgroundColor(pal.divider) },
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1, Gravity.BOTTOM))
        var open = false
        more.setOnClickListener {
            open = !open
            val from = (extra.layoutParams as ViewGroup.LayoutParams).height
            val to = if (open) px(72f) * extraPacks.size else 0
            ValueAnimator.ofInt(from, to).apply {
                duration = 300
                interpolator = DecelerateInterpolator(1.6f)
                addUpdateListener {
                    val lp = extra.layoutParams
                    lp.height = it.animatedValue as Int
                    extra.layoutParams = lp
                }
                start()
            }
            arrow.animate().rotation(if (open) 180f else 0f).setDuration(300).start()
        }
        bottom.addView(more, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(72f)))

        // Conditions
        val t1 = "شراؤك للريش يعني موافقتك\nعلى "
        val t2 = "الشروط والأحكام"
        val terms = SpannableStringBuilder(t1 + t2 + ".")
        terms.setSpan(ForegroundColorSpan(GREEN), t1.length, t1.length + t2.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        bottom.addView(tv(terms, 17f, pal.fGrey).apply {
            gravity = Gravity.CENTER
            textDirection = View.TEXT_DIRECTION_RTL
            setLineSpacing(0f, 1.25f)
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = px(24f); bottomMargin = px(44f); leftMargin = px(30f); rightMargin = px(30f)
        })
        content.addView(bottom, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        slideIn(sheet)
        dlg.setContentView(sheet)
        applyLayout(dlg, 0.8f)
        dlg.show()
    }

    private fun packRow(p: Pack, name: String): FrameLayout {
        val row = FrameLayout(act)
        row.addView(GiftFeatherStackView(act, p.count, p.scale),
            FrameLayout.LayoutParams(px(72f), px(56f), Gravity.RIGHT or Gravity.CENTER_VERTICAL).apply { rightMargin = px(18f) })
        row.addView(tv(p.label, 22f, pal.fTitle, true),
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.RIGHT or Gravity.CENTER_VERTICAL).apply { rightMargin = px(96f) })
        row.addView(tv(p.price, 19f, pal.fGrey),
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.LEFT or Gravity.CENTER_VERTICAL).apply { leftMargin = px(26f) })
        row.addView(View(act).apply { setBackgroundColor(pal.divider) },
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1, Gravity.BOTTOM))
        row.setOnClickListener { Toast.makeText(act, "شراء ${p.label} لـ $name", Toast.LENGTH_SHORT).show() }
        return row
    }
}

/** Avatar rond : 0 = republier en story, 1 = messages enregistrés, 2 = lettre. */
class GiftAvatarView(c: Context, private val kind: Int, private val letter: String, private val c1: Int, private val c2: Int) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(cv: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(0f, 0f, w, h, c1, c2, Shader.TileMode.CLAMP)
        cv.drawCircle(w / 2f, h / 2f, w / 2f, p)
        p.shader = null
        p.color = Color.WHITE
        when (kind) {
            0 -> {
                p.style = Paint.Style.STROKE
                p.strokeWidth = w * 0.075f
                p.strokeCap = Paint.Cap.ROUND
                p.strokeJoin = Paint.Join.ROUND
                val up = Path().apply {
                    moveTo(0.30f * w, 0.58f * h); lineTo(0.30f * w, 0.42f * h)
                    quadTo(0.30f * w, 0.33f * h, 0.39f * w, 0.33f * h); lineTo(0.70f * w, 0.33f * h)
                    moveTo(0.62f * w, 0.25f * h); lineTo(0.71f * w, 0.33f * h); lineTo(0.62f * w, 0.41f * h)
                }
                val dn = Path().apply {
                    moveTo(0.70f * w, 0.42f * h); lineTo(0.70f * w, 0.58f * h)
                    quadTo(0.70f * w, 0.67f * h, 0.61f * w, 0.67f * h); lineTo(0.30f * w, 0.67f * h)
                    moveTo(0.38f * w, 0.59f * h); lineTo(0.29f * w, 0.67f * h); lineTo(0.38f * w, 0.75f * h)
                }
                cv.drawPath(up, p)
                cv.drawPath(dn, p)
            }
            1 -> {
                p.style = Paint.Style.STROKE
                p.strokeWidth = w * 0.075f
                p.strokeJoin = Paint.Join.ROUND
                p.strokeCap = Paint.Cap.ROUND
                val bm = Path().apply {
                    moveTo(0.36f * w, 0.28f * h); lineTo(0.64f * w, 0.28f * h); lineTo(0.64f * w, 0.74f * h)
                    lineTo(0.50f * w, 0.62f * h); lineTo(0.36f * w, 0.74f * h); close()
                }
                cv.drawPath(bm, p)
            }
            else -> {
                p.style = Paint.Style.FILL
                p.typeface = Typeface.DEFAULT_BOLD
                p.textSize = w * 0.42f
                p.textAlign = Paint.Align.CENTER
                cv.drawText(letter, w / 2f, h / 2f - (p.ascent() + p.descent()) / 2f, p)
            }
        }
    }
}

/** Loupe de recherche. */
class GiftSearchIconView(c: Context, private val color: Int) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(cv: Canvas) {
        val w = width.toFloat()
        p.style = Paint.Style.STROKE
        p.strokeWidth = w * 0.09f
        p.strokeCap = Paint.Cap.ROUND
        p.color = color
        cv.drawCircle(w * 0.43f, w * 0.43f, w * 0.27f, p)
        cv.drawLine(w * 0.63f, w * 0.63f, w * 0.86f, w * 0.86f, p)
    }
}

/** Plumes argentées superposées en éventail (1 à 5). */
class GiftFeatherStackView(c: Context, private val count: Int, private val scale: Float) : View(c) {
    init { setLayerType(LAYER_TYPE_SOFTWARE, null) }
    override fun onDraw(cv: Canvas) {
        val h = height * 0.40f * scale
        val step = width * 0.10f
        val mid = (count - 1) / 2f
        for (i in 0 until count) {
            val o = i - mid
            Feather.drawQuill(cv, width / 2f + o * step, height / 2f, h, 22f + o * 9f, false, 255)
        }
    }
}

/** Grand cercle bleu-vert avec la lettre de l'ami + étincelles orange/dorées animées en continu. */
class GiftSparkleView(c: Context, private val k: Float, private val letter: String) : View(c) {
    private class Sp(val dx: Float, val y: Float, val s: Float, val kind: Int, val color: Int, val f: Int, val ph: Float)

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val list = ArrayList<Sp>()
    private var t = 0f
    private var anim: ValueAnimator? = null
    private val colors = intArrayOf(0xFFFF8A3D.toInt(), 0xFFFFC53D.toInt(), 0xFFF4A261.toInt(), 0xFFFF7A2F.toInt())

    init {
        val rnd = java.util.Random(11)
        var guard = 0
        while (list.size < 44 && guard++ < 600) {
            val dx = (rnd.nextFloat() * 2f - 1f) * 270f
            val y = 10f + rnd.nextFloat() * 195f
            if (hypot(dx, y - 107f) < 75f + 24f) continue
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
        val cy = 107f * k
        val r = 75f * k
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

        p.shader = LinearGradient(cx - r, cy - r, cx + r, cy + r, 0xFF4FC8D8.toInt(), 0xFF2E7FDC.toInt(), Shader.TileMode.CLAMP)
        cv.drawCircle(cx, cy, r, p)
        p.shader = null
        p.color = Color.WHITE
        p.typeface = Typeface.DEFAULT_BOLD
        p.textAlign = Paint.Align.CENTER
        p.textSize = r * 1.0f
        cv.drawText(letter, cx, cy - (p.ascent() + p.descent()) / 2f, p)
    }
}
