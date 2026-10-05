package com.owlino.plus

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Type d'un résultat de recherche. */
enum class SKind { PERSON, GROUP, CHANNEL, BOT }

class SearchEntity(
    val name: String,
    val username: String,
    val kind: SKind,
    val color: Int,
    val verified: Boolean = false,
    val count: String = ""
)

/**
 * Pager horizontal sans dépendance : on glisse entre les pages, avec élan et « snap ».
 * [onProgress] reçoit la position fractionnaire (0.0 = page 1, 1.0 = page 2, 0.5 = entre les deux),
 * ce qui permet à la pastille des onglets de suivre le doigt en temps réel.
 */
class TabPager(c: Context) : ViewGroup(c) {
    var onProgress: ((Float) -> Unit)? = null
    var onPageChanged: ((Int) -> Unit)? = null
    var onDragStart: (() -> Unit)? = null

    var current = 0
        private set

    private val slop = ViewConfiguration.get(c).scaledTouchSlop
    private val density = c.resources.displayMetrics.density
    private var vt: VelocityTracker? = null
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var dragging = false
    private var anim: ValueAnimator? = null

    fun setPages(pages: List<View>) {
        removeAllViews()
        pages.forEach { addView(it, LayoutParams(MP, MP)) }
    }

    private fun maxScroll(): Int = max(0, (childCount - 1) * width)

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        val w = MeasureSpec.getSize(wSpec)
        val h = MeasureSpec.getSize(hSpec)
        setMeasuredDimension(w, h)
        val cw = MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY)
        val ch = MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY)
        for (i in 0 until childCount) getChildAt(i).measure(cw, ch)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val w = r - l
        val h = b - t
        for (i in 0 until childCount) getChildAt(i).layout(i * w, 0, (i + 1) * w, h)
        if (changed) scrollTo(current * w, 0)
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        if (width > 0) onProgress?.invoke(l.toFloat() / width)
    }

    fun setCurrent(i: Int, animate: Boolean) {
        if (childCount == 0) return
        val target = i.coerceIn(0, childCount - 1)
        val changed = target != current
        current = target
        val to = target * width
        anim?.cancel()
        if (!animate || width == 0) {
            scrollTo(to, 0)
        } else {
            anim = ValueAnimator.ofInt(scrollX, to).apply {
                duration = 280
                interpolator = DecelerateInterpolator(1.5f)
                addUpdateListener { scrollTo(it.animatedValue as Int, 0) }
                start()
            }
        }
        if (changed) onPageChanged?.invoke(target)
    }

    override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = e.x
                downY = e.y
                lastX = e.x
                dragging = false
                anim?.cancel()
                vt?.recycle()
                vt = VelocityTracker.obtain()
                vt?.addMovement(e)
            }
            MotionEvent.ACTION_MOVE -> {
                vt?.addMovement(e)
                val dx = e.x - downX
                val dy = e.y - downY
                if (!dragging && abs(dx) > slop && abs(dx) > abs(dy) * 1.3f) {
                    dragging = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                    lastX = e.x
                    onDragStart?.invoke()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!dragging) {
                    vt?.recycle()
                    vt = null
                }
            }
        }
        return dragging
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        vt?.addMovement(e)
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> return true
            MotionEvent.ACTION_MOVE -> {
                if (dragging) {
                    val dx = lastX - e.x
                    lastX = e.x
                    scrollTo((scrollX + dx).roundToInt().coerceIn(0, maxScroll()), 0)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (dragging) {
                    vt?.computeCurrentVelocity(1000)
                    settle(vt?.xVelocity ?: 0f)
                }
                dragging = false
                vt?.recycle()
                vt = null
            }
        }
        return true
    }

    private fun settle(vx: Float) {
        if (width == 0) return
        val pos = scrollX.toFloat() / width
        var target = pos.roundToInt()
        if (abs(vx) > 500f * density) {
            // Un geste rapide change de page même si on n'a pas dépassé la moitié
            target = if (vx < 0) ceil(pos.toDouble()).toInt() else floor(pos.toDouble()).toInt()
        }
        setCurrent(target, true)
    }
}

/**
 * Onglets de recherche : une pastille (carte) glisse derrière l'onglet actif et suit le doigt
 * pendant le balayage des pages, exactement comme dans la barre du bas.
 * Si la place le permet, les onglets se répartissent à parts égales sur la largeur.
 */
class SearchTabStrip(
    c: Context,
    private val dark: Boolean,
    private val labels: List<String>,
    private val onSelect: (Int) -> Unit
) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val n = labels.size
    private val padH = c.dpf(14f)
    private val gap = c.dpf(4f)
    private val margin = c.dpf(10f)
    private val widths = FloatArray(n)
    private val lefts = FloatArray(n)
    private var progress = 0f

    private val pillCol = if (dark) 0xFF2A3350.toInt() else 0xFFE6EDFF.toInt()
    private val blue = if (dark) 0xFF8FB0FF.toInt() else 0xFF4A7BE5.toInt()
    private val grey = if (dark) 0xFF9A9DA3.toInt() else 0xFF6B6E7A.toInt()

    init {
        p.textSize = c.dpf(15f).let { it * 1f }
        p.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        p.textAlign = Paint.Align.CENTER
    }

    fun setProgress(v: Float) {
        progress = v
        invalidate()
    }

    /** Abscisse du centre de la pastille (pour garder l'onglet visible dans le défilement horizontal). */
    fun centerX(v: Float): Float {
        val r = rectFor(v)
        return (r.left + r.right) / 2f
    }

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        p.textSize = context.dpf(15f)
        var natural = margin * 2 + gap * (n - 1)
        for (i in 0 until n) {
            widths[i] = p.measureText(labels[i]) + padH * 2
            natural += widths[i]
        }
        val avail = if (MeasureSpec.getMode(wSpec) == MeasureSpec.EXACTLY) MeasureSpec.getSize(wSpec).toFloat() else 0f
        val total = max(natural, avail)
        val extra = (total - natural) / n // répartition égale de l'espace restant
        var x = margin
        for (i in 0 until n) {
            widths[i] += extra
            lefts[i] = x
            x += widths[i] + gap
        }
        setMeasuredDimension(total.roundToInt(), context.dp(46f))
    }

    private fun rectFor(v: Float): RectF {
        val pv = v.coerceIn(0f, (n - 1).toFloat())
        val i = min(floor(pv.toDouble()).toInt(), n - 1)
        val j = min(i + 1, n - 1)
        val f = pv - i
        val l = lefts[i] + (lefts[j] - lefts[i]) * f
        val r = lefts[i] + widths[i] + (lefts[j] + widths[j] - lefts[i] - widths[i]) * f
        val h = context.dpf(34f)
        val top = (height - h) / 2f
        return RectF(l, top, r, top + h)
    }

    override fun onDraw(cv: Canvas) {
        val r = rectFor(progress)
        p.style = Paint.Style.FILL
        p.color = pillCol
        cv.drawRoundRect(r, r.height() / 2f, r.height() / 2f, p)

        val baseline = height / 2f - (p.ascent() + p.descent()) / 2f
        for (i in 0 until n) {
            val w = (1f - abs(progress - i)).coerceIn(0f, 1f)
            p.color = mixColor(w, grey, blue)
            cv.drawText(labels[i], lefts[i] + widths[i] / 2f, baseline, p)
        }
    }

    private fun mixColor(f: Float, a: Int, b: Int): Int {
        fun ch(x: Int, s: Int) = (x shr s) and 0xFF
        fun m(s: Int) = (ch(a, s) + (ch(b, s) - ch(a, s)) * f).roundToInt()
        return (m(24) shl 24) or (m(16) shl 16) or (m(8) shl 8) or m(0)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> return true
            MotionEvent.ACTION_UP -> {
                for (i in 0 until n) {
                    if (e.x >= lefts[i] - gap / 2 && e.x <= lefts[i] + widths[i] + gap / 2) {
                        onSelect(i)
                        break
                    }
                }
                performClick()
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}

/**
 * Écran de recherche : barre du haut identique à l'accueil (☰ devient ←), onglets
 * All / People / Groups / Channels / Bots, pages à balayer, liste de résultats filtrée en direct.
 * Interface seulement : données de démonstration.
 */
class SearchScreenView(
    private val c: Context,
    private val dark: Boolean,
    private val onClose: () -> Unit
) : LinearLayout(c) {

    private val all = listOf(
        SearchEntity("abdelhak Nnarr", "abdelhaknnarr", SKind.PERSON, 0xFFE87000.toInt()),
        SearchEntity("ريان ريان", "ريانريان", SKind.PERSON, 0xFFBF3A0A.toInt()),
        SearchEntity("user_600a02", "user_600a02", SKind.PERSON, 0xFFFFB74D.toInt()),
        SearchEntity("user_7eac4e", "user_7eac4e", SKind.PERSON, 0xFF64B5F6.toInt()),
        SearchEntity("Abdo Abdo", "abdoabdo", SKind.PERSON, 0xFF7B1FA2.toInt()),
        SearchEntity("امن ناس", "amnnas18", SKind.PERSON, 0xFF6D4C41.toInt()),
        SearchEntity("user_f2405f", "user_f2405f", SKind.PERSON, 0xFF81C784.toInt()),
        SearchEntity("youesyounes 572", "youesyounes572", SKind.PERSON, 0xFF7E57C2.toInt()),
        SearchEntity("Owlino", "owlino", SKind.PERSON, 0xFF16A34A.toInt(), verified = true),
        SearchEntity("Hani Kanon", "kanonhani", SKind.PERSON, 0xFF3A2A22.toInt(), verified = true),
        SearchEntity("BotManager", "botmanager", SKind.BOT, 0xFF8E8AA0.toInt(), verified = true),
        SearchEntity("Owlino Bot", "owlino_bot", SKind.BOT, 0xFF22C55E.toInt(), verified = true),
        SearchEntity("hani", "hani_bot", SKind.BOT, 0xFFFFC107.toInt()),
        SearchEntity("mony_bot", "mony_bot", SKind.BOT, 0xFF26A69A.toInt()),
        SearchEntity("Owlino News", "owlino_news", SKind.CHANNEL, 0xFF0EA5E9.toInt(), verified = true, count = "12.4K subscribers"),
        SearchEntity("Tech Insider", "techinsider", SKind.CHANNEL, 0xFF14B8A6.toInt(), verified = true, count = "52.3K subscribers"),
        SearchEntity("قناتي", "qanaty", SKind.CHANNEL, 0xFFFFC107.toInt(), count = "1.2K subscribers"),
        SearchEntity("Owlino Team", "owlino_team", SKind.GROUP, 0xFF3B82F6.toInt(), count = "3.2K members"),
        SearchEntity("Algeria Devs", "algeria_devs", SKind.GROUP, 0xFFEC4899.toInt(), count = "8.7K members"),
        SearchEntity("Crypto Talk", "crypto_talk", SKind.GROUP, 0xFFF97316.toInt(), count = "15.1K members")
    )

    private val tabs: List<Pair<String, SKind?>> = listOf(
        "All" to null,
        "People" to SKind.PERSON,
        "Groups" to SKind.GROUP,
        "Channels" to SKind.CHANNEL,
        "Bots" to SKind.BOT
    )

    private var query = ""
    private val pageBoxes = ArrayList<LinearLayout>()
    private val pager = TabPager(c)
    private val stripScroll = HorizontalScrollView(c)
    private val strip: SearchTabStrip
    val topBar: TopBarView

    init {
        orientation = VERTICAL
        setBackgroundColor(UiColors.bg(dark))
        layoutDirection = View.LAYOUT_DIRECTION_LTR

        topBar = TopBarView(c, dark, "Search messages...", true, {}, { close() }, { q ->
            query = q
            refreshAll()
        })
        addView(topBar, LayoutParams(MP, WC))

        strip = SearchTabStrip(c, dark, tabs.map { it.first }) { i -> pager.setCurrent(i, true) }
        stripScroll.isHorizontalScrollBarEnabled = false
        stripScroll.overScrollMode = View.OVER_SCROLL_NEVER
        stripScroll.isFillViewport = true
        stripScroll.addView(strip, FrameLayout.LayoutParams(WC, c.dp(46f)))
        addView(stripScroll, LayoutParams(MP, c.dp(46f)))

        val divider = View(c)
        divider.setBackgroundColor(if (dark) 0x1FFFFFFF else 0x14000000)
        addView(divider, LayoutParams(MP, c.dp(1f)))

        val pages = ArrayList<View>()
        tabs.forEach { _ ->
            val box = LinearLayout(c)
            box.orientation = VERTICAL
            pageBoxes.add(box)
            val sv = ScrollView(c)
            sv.isVerticalScrollBarEnabled = false
            sv.overScrollMode = View.OVER_SCROLL_NEVER
            sv.setOnScrollChangeListener { _, _, _, _, _ -> topBar.hideKeyboard() }
            sv.addView(box, FrameLayout.LayoutParams(MP, WC))
            pages.add(sv)
        }
        pager.setPages(pages)
        pager.onProgress = { v ->
            strip.setProgress(v)
            keepTabVisible(v)
        }
        pager.onDragStart = { topBar.hideKeyboard() }
        addView(pager, LayoutParams(MP, 0, 1f))

        refreshAll()
    }

    /** Appelé quand l'écran apparaît : champ actif + clavier. */
    fun open() {
        topBar.focusAndShowKeyboard()
    }

    /** Flèche ←, ou bouton retour du téléphone. */
    fun close() {
        topBar.hideKeyboard()
        topBar.clearText()
        pager.setCurrent(0, false)
        onClose()
    }

    private fun keepTabVisible(v: Float) {
        if (stripScroll.width == 0 || strip.width <= stripScroll.width) return
        val target = (strip.centerX(v) - stripScroll.width / 2f).roundToInt()
        stripScroll.scrollTo(target.coerceIn(0, strip.width - stripScroll.width), 0)
    }

    private fun refreshAll() {
        tabs.forEachIndexed { i, t -> fill(pageBoxes[i], t.second) }
    }

    private fun fill(box: LinearLayout, kind: SKind?) {
        box.removeAllViews()
        val q = query.trim().lowercase()
        var list = all.filter { kind == null || it.kind == kind }
        if (q.isNotEmpty()) {
            list = list.filter { it.name.lowercase().contains(q) || it.username.lowercase().contains(q) }
                .sortedBy { if (it.name.lowercase().startsWith(q) || it.username.lowercase().startsWith(q)) 0 else 1 }
        }
        if (list.isEmpty()) {
            val e = LinearLayout(c)
            e.orientation = VERTICAL
            e.gravity = Gravity.CENTER_HORIZONTAL
            e.setPadding(0, c.dp(90f), 0, 0)
            val circle = View(c)
            circle.background = c.oval(UiColors.chip(dark))
            e.addView(circle, LayoutParams(c.dp(80f), c.dp(80f)))
            val t1 = c.label("No results", 20f, UiColors.text(dark), 2)
            val l1 = LayoutParams(WC, WC)
            l1.topMargin = c.dp(16f)
            e.addView(t1, l1)
            val t2 = c.label("Try a different name or username", 15f, UiColors.text2(dark))
            val l2 = LayoutParams(WC, WC)
            l2.topMargin = c.dp(8f)
            e.addView(t2, l2)
            box.addView(e, LayoutParams(MP, WC))
            return
        }
        val header = c.label(if (q.isEmpty()) "Search remnants" else "Search results", 14f, UiColors.text2(dark), 1)
        header.setPadding(c.dp(16f), c.dp(14f), c.dp(16f), c.dp(8f))
        box.addView(header, LayoutParams(MP, WC))
        list.forEach { box.addView(row(it), LayoutParams(MP, WC)) }
        box.addView(View(c), LayoutParams(1, c.dp(24f)))
    }

    private fun row(en: SearchEntity): View {
        val wrap = LinearLayout(c)
        wrap.orientation = VERTICAL

        // Séparateur inséré à 72dp comme dans l'écran de référence
        val div = View(c)
        div.setBackgroundColor(if (dark) 0x1FFFFFFF else 0x14000000)
        val dl = LayoutParams(MP, c.dp(1f))
        dl.leftMargin = c.dp(72f)
        dl.rightMargin = c.dp(16f)
        wrap.addView(div, dl)

        val r = LinearLayout(c)
        r.orientation = HORIZONTAL
        r.gravity = Gravity.CENTER_VERTICAL
        r.setPadding(c.dp(16f), c.dp(7f), c.dp(16f), c.dp(7f))
        val tv = TypedValue()
        if (c.theme.resolveAttribute(android.R.attr.selectableItemBackground, tv, true)) r.setBackgroundResource(tv.resourceId)

        val letter = en.name.take(1)
        r.addView(CircleAvatar(c, letter, en.color), LayoutParams(c.dp(46f), c.dp(46f)))

        val col = LinearLayout(c)
        col.orientation = VERTICAL
        val nameRow = LinearLayout(c)
        nameRow.orientation = HORIZONTAL
        nameRow.gravity = Gravity.CENTER_VERTICAL
        nameRow.addView(c.label(en.name, 17f, UiColors.text(dark), 1), LayoutParams(WC, WC))
        if (en.verified) {
            val vl = LayoutParams(c.dp(16f), c.dp(16f))
            vl.leftMargin = c.dp(6f)
            nameRow.addView(UiGlyph(c, UiGlyph.VERIFIED, 0), vl)
        }
        col.addView(nameRow, LayoutParams(MP, WC))

        val sub = when (en.kind) {
            SKind.BOT -> "@${en.username} · bot"
            SKind.CHANNEL, SKind.GROUP -> "@${en.username} · ${en.count}"
            else -> "@${en.username}"
        }
        val sl = LayoutParams(WC, WC)
        sl.topMargin = c.dp(3f)
        col.addView(c.label(sub, 14.5f, UiColors.text2(dark)), sl)

        val cl = LayoutParams(0, WC, 1f)
        cl.leftMargin = c.dp(10f)
        r.addView(col, cl)
        r.addView(UiGlyph(c, UiGlyph.CHEV_RIGHT, 0xFF8E8E93.toInt()), LayoutParams(c.dp(20f), c.dp(20f)))

        wrap.addView(r, LayoutParams(MP, WC))
        return wrap
    }
}
