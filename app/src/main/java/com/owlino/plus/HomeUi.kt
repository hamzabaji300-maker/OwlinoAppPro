package com.owlino.plus

import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Barre du haut de l'accueil : ☰ (ou ←) + carte de recherche + avatar avec anneau bleu.
 * Interface seulement : aucune logique réseau.
 */
class TopBarView(
    private val c: Context,
    private val dark: Boolean,
    private val hint: String,
    private val onSearching: (Boolean) -> Unit,
    private val onQuery: (String) -> Unit
) : LinearLayout(c) {
    private val menu = Glyph(c, Glyph.MENU, UiColors.text(dark))
    private val iconTint = if (dark) 0xBFFFFFFF.toInt() else 0xBF000000.toInt()
    private val clear = Glyph(c, Glyph.CLOSE, iconTint)
    private val search = Glyph(c, Glyph.SEARCH, iconTint)
    private val edit = EditText(c)
    var isSearching = false
        private set

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        isFocusableInTouchMode = true
        setPadding(c.dp(8f), c.dp(10f), c.dp(14f), c.dp(8f))

        val menuBox = FrameLayout(c)
        menuBox.addView(menu, FrameLayout.LayoutParams(c.dp(26f), c.dp(26f), Gravity.CENTER))
        menuBox.setOnClickListener { if (isSearching) closeSearch() }
        addView(menuBox, LayoutParams(c.dp(48f), c.dp(48f)))

        val pill = LinearLayout(c)
        pill.orientation = HORIZONTAL
        pill.gravity = Gravity.CENTER_VERTICAL
        pill.background = c.rounded(UiColors.surface(dark), 26f, 1f, UiColors.border(dark))
        pill.setPadding(c.dp(20f), 0, c.dp(16f), 0)

        edit.background = null
        edit.hint = hint
        edit.setHintTextColor(UiColors.text2(dark))
        edit.setTextColor(UiColors.text(dark))
        edit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        edit.setSingleLine()
        edit.setPadding(0, 0, 0, 0)
        edit.inputType = InputType.TYPE_CLASS_TEXT
        edit.imeOptions = EditorInfo.IME_ACTION_SEARCH
        edit.gravity = Gravity.CENTER_VERTICAL or Gravity.START
        edit.setOnEditorActionListener { _, _, _ ->
            hideKeyboard()
            true
        }
        edit.setOnFocusChangeListener { _, f -> setSearching(f) }
        edit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, cnt: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, cnt: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val q = s?.toString() ?: ""
                clear.visibility = if (q.isEmpty()) View.GONE else View.VISIBLE
                search.visibility = if (q.isEmpty()) View.VISIBLE else View.GONE
                onQuery(q)
            }
        })
        pill.addView(edit, LayoutParams(0, MP, 1f))

        clear.visibility = View.GONE
        clear.setOnClickListener { edit.setText("") }
        pill.addView(clear, LayoutParams(c.dp(22f), c.dp(22f)))
        pill.addView(search, LayoutParams(c.dp(24f), c.dp(24f)))

        val pl = LayoutParams(0, c.dp(52f), 1f)
        pl.setMargins(c.dp(4f), 0, c.dp(4f), 0)
        addView(pill, pl)

        // Avatar avec anneau bleu
        val ring = FrameLayout(c)
        ring.background = c.oval(Color.TRANSPARENT, 2f, 0x8C3B82F6.toInt())
        val inner = CircleAvatar(c, "H", 0xFF1A5FB4.toInt())
        val il = FrameLayout.LayoutParams(c.dp(38f), c.dp(38f))
        il.setMargins(c.dp(4f), c.dp(4f), c.dp(4f), c.dp(4f))
        ring.addView(inner, il)
        val rl = LayoutParams(c.dp(46f), c.dp(46f))
        rl.leftMargin = c.dp(2f)
        addView(ring, rl)
    }

    private fun setSearching(v: Boolean) {
        if (isSearching == v) return
        isSearching = v
        menu.kind = if (v) Glyph.ARROW_BACK else Glyph.MENU
        menu.invalidate()
        onSearching(v)
    }

    private fun hideKeyboard() {
        val imm = c.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(edit.windowToken, 0)
    }

    fun closeSearch() {
        edit.setText("")
        requestFocus() // retire le focus du champ (ce conteneur est focusable)
        hideKeyboard()
        setSearching(false)
    }
}

/** Écran d'accueil (liste des discussions) : interface seulement, données de démonstration. */
class HomeScreenView(
    private val c: Context,
    private val dark: Boolean,
    private val onSearching: (Boolean) -> Unit
) : FrameLayout(c) {

    private class Chat(
        val name: String,
        val msg: String,
        val time: String,
        val color: Int,
        val letter: String,
        val bot: Boolean = false,
        val channel: Boolean = false,
        val group: Boolean = false,
        val verified: Boolean = false,
        val star: Boolean = false,
        val muted: Boolean = false,
        val pinned: Boolean = false,
        val unread: Int = 0,
        val mine: Boolean = false,
        val read: Boolean = false,
        val media: String = ""
    )

    private val chats = listOf(
        Chat("BotManager", "Appuyez sur DÉMARRER pour commencer", "Now", 0xFFC7C3CF.toInt(), "B", bot = true, verified = true),
        Chat("hani", "Appuyez sur DÉMARRER pour commencer", "Now", 0xFFFFE600.toInt(), "h", bot = true),
        Chat("Owlino", "Déconnexion…", "12:47", 0xFF16A34A.toInt(), "O", verified = true, pinned = true),
        Chat("قناتي", "Photo", "12:47", 0xFFFFE600.toInt(), "ق", channel = true, verified = true, mine = true, read = true, media = "photo"),
        Chat("Abdo Abdo", "@mony_bot", "08:46", 0xFF7B1FA2.toInt(), "A", star = true, pinned = true, mine = true, read = true),
        Chat("Hani Kanon", "@botmanager", "08:34", 0xFF3A2A22.toInt(), "H", star = true, muted = true, verified = true, pinned = true),
        Chat("Maamar", "Photo", "20:31", 0xFF1C1C1E.toInt(), "M", muted = true, media = "photo"),
        Chat("هههه", "Voice message", "10:09", 0xFF6B7280.toInt(), "ه", media = "voice"),
        Chat("Owlino Team", "Welcome to the group!", "Yesterday", 0xFF0EA5E9.toInt(), "T", group = true, unread = 3),
        Chat("Sara", "See you tomorrow", "Mon", 0xFFEC4899.toInt(), "S", unread = 2)
    )

    private var filter = 0
    private var query = ""
    private val listBox = LinearLayout(c)
    private val tabs = ArrayList<TextView>()
    private val fab = FrameLayout(c)
    val topBar: TopBarView

    init {
        setBackgroundColor(UiColors.bg(dark))
        listBox.orientation = LinearLayout.VERTICAL
        topBar = TopBarView(c, dark, "Search messages...", { s ->
            fab.visibility = if (s) View.GONE else View.VISIBLE
            onSearching(s)
        }, { q ->
            query = q
            rebuild()
        })

        val col = LinearLayout(c)
        col.orientation = LinearLayout.VERTICAL
        col.addView(topBar, LinearLayout.LayoutParams(MP, WC))
        val tl = LinearLayout.LayoutParams(MP, c.dp(38f))
        tl.setMargins(c.dp(16f), c.dp(4f), c.dp(16f), c.dp(4f))
        col.addView(buildTabs(), tl)
        col.addView(listBox, LinearLayout.LayoutParams(MP, WC))
        col.addView(View(c), LinearLayout.LayoutParams(1, c.dp(96f)))

        val sv = ScrollView(c)
        sv.isVerticalScrollBarEnabled = false
        sv.addView(col, LayoutParams(MP, WC))
        addView(sv, LayoutParams(MP, MP))

        // Bouton « nouveau message » (crayon)
        fab.background = c.oval(UiColors.surface(dark))
        fab.elevation = c.dpf(4f)
        fab.addView(
            Glyph(c, Glyph.PENCIL, if (dark) 0xFFDCE3F9.toInt() else 0xFF1B2B5B.toInt()),
            LayoutParams(c.dp(24f), c.dp(24f), Gravity.CENTER)
        )
        val fl = LayoutParams(c.dp(56f), c.dp(56f), Gravity.BOTTOM or Gravity.END)
        fl.setMargins(0, 0, c.dp(20f), c.dp(20f))
        addView(fab, fl)

        updateTabs()
        rebuild()
    }

    fun isSearching() = topBar.isSearching
    fun closeSearch() = topBar.closeSearch()

    private fun buildTabs(): LinearLayout {
        val box = LinearLayout(c)
        box.orientation = LinearLayout.HORIZONTAL
        box.gravity = Gravity.CENTER_VERTICAL
        box.background = c.rounded(UiColors.surface(dark), 19f, 1f, if (dark) 0x0DFFFFFF else 0x0D000000)
        box.setPadding(c.dp(4f), c.dp(4f), c.dp(4f), c.dp(4f))
        val labels = listOf("All Chats", "Unread", "Groups", "Channels")
        labels.forEachIndexed { i, s ->
            val tv = c.label(s, 12f, 0, 1)
            tv.gravity = Gravity.CENTER
            tv.setOnClickListener {
                filter = i
                updateTabs()
                rebuild()
            }
            tabs.add(tv)
            box.addView(tv, LinearLayout.LayoutParams(0, MP, 1f))
        }
        return box
    }

    private fun updateTabs() {
        tabs.forEachIndexed { i, tv ->
            if (i == filter) {
                tv.background = c.rounded(UiColors.BLUE, 15f)
                tv.setTextColor(Color.WHITE)
            } else {
                tv.background = null
                tv.setTextColor(if (dark) 0xFFE5E7EB.toInt() else 0xFF111827.toInt())
            }
        }
    }

    private fun rebuild() {
        listBox.removeAllViews()
        val q = query.trim().lowercase()
        val shown = chats.filter {
            val okFilter = when (filter) {
                1 -> it.unread > 0
                2 -> it.group
                3 -> it.channel
                else -> true
            }
            okFilter && (q.isEmpty() || it.name.lowercase().contains(q))
        }
        if (shown.isEmpty()) {
            val e = c.label("No chats", 15f, UiColors.text2(dark))
            e.gravity = Gravity.CENTER
            listBox.addView(e, LinearLayout.LayoutParams(MP, c.dp(160f)))
            return
        }
        shown.forEach { listBox.addView(row(it), LinearLayout.LayoutParams(MP, WC)) }
    }

    private fun row(ch: Chat): View {
        val text2 = UiColors.text2(dark)
        val r = LinearLayout(c)
        r.orientation = LinearLayout.HORIZONTAL
        r.gravity = Gravity.CENTER_VERTICAL
        r.setPadding(c.dp(16f), c.dp(10f), c.dp(16f), c.dp(10f))
        val tv = TypedValue()
        if (c.theme.resolveAttribute(android.R.attr.selectableItemBackground, tv, true)) r.setBackgroundResource(tv.resourceId)

        r.addView(
            CircleAvatar(c, ch.letter, ch.color, if (ch.color == 0xFFFFE600.toInt()) 0xFF222222.toInt() else Color.WHITE, 0, true),
            LinearLayout.LayoutParams(c.dp(56f), c.dp(56f))
        )

        val col = LinearLayout(c)
        col.orientation = LinearLayout.VERTICAL

        // Ligne du haut : nom + badges | heure
        val top = LinearLayout(c)
        top.orientation = LinearLayout.HORIZONTAL
        top.gravity = Gravity.CENTER_VERTICAL
        val left = LinearLayout(c)
        left.orientation = LinearLayout.HORIZONTAL
        left.gravity = Gravity.CENTER_VERTICAL
        left.addView(c.label(ch.name, 16f, UiColors.text(dark), 1), LinearLayout.LayoutParams(WC, WC))
        if (ch.bot) left.addView(botChip(), gapStart(6f))
        if (ch.channel) left.addView(Glyph(c, Glyph.MEGA, 0xFF9CA3AF.toInt()), sized(14f, 6f))
        if (ch.star) left.addView(Glyph(c, Glyph.STAR, 0xFFFBBF24.toInt()), sized(16f, 6f))
        if (ch.muted) left.addView(Glyph(c, Glyph.BELL_OFF, text2), sized(15f, 6f))
        if (ch.verified) left.addView(Glyph(c, Glyph.VERIFIED, 0), sized(16f, 6f))
        top.addView(left, LinearLayout.LayoutParams(0, WC, 1f))

        val timeBox = LinearLayout(c)
        timeBox.orientation = LinearLayout.HORIZONTAL
        timeBox.gravity = Gravity.CENTER_VERTICAL
        if (ch.pinned) {
            timeBox.background = c.rounded((text2 and 0x00FFFFFF) or 0x1F000000, 20f)
            timeBox.setPadding(c.dp(6f), c.dp(1f), c.dp(7f), c.dp(1f))
            val pin = Glyph(c, Glyph.PIN, 0xFF8E8E93.toInt())
            pin.rotation = 45f
            timeBox.addView(pin, LinearLayout.LayoutParams(c.dp(12f), c.dp(12f)))
            val gap = LinearLayout.LayoutParams(WC, WC)
            gap.leftMargin = c.dp(3f)
            timeBox.addView(c.label(ch.time, 12f, text2, 1), gap)
        } else {
            timeBox.addView(c.label(ch.time, 12f, text2, 1), LinearLayout.LayoutParams(WC, WC))
        }
        val tbl = LinearLayout.LayoutParams(WC, WC)
        tbl.leftMargin = c.dp(8f)
        top.addView(timeBox, tbl)
        col.addView(top, LinearLayout.LayoutParams(MP, WC))

        // Ligne du bas : aperçu | coches ou compteur
        val bottom = LinearLayout(c)
        bottom.orientation = LinearLayout.HORIZONTAL
        bottom.gravity = Gravity.CENTER_VERTICAL
        val msgBox = LinearLayout(c)
        msgBox.orientation = LinearLayout.HORIZONTAL
        msgBox.gravity = Gravity.CENTER_VERTICAL
        if (ch.media == "photo") {
            val th = View(c)
            val gd = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xFF8D6E63.toInt(), 0xFF6D8B74.toInt())
            )
            gd.cornerRadius = c.dpf(4f)
            th.background = gd
            msgBox.addView(th, sized(20f, 0f))
            msgBox.addView(View(c), LinearLayout.LayoutParams(c.dp(4f), 1))
        } else if (ch.media == "voice") {
            msgBox.addView(Glyph(c, Glyph.MIC, text2), sized(16f, 0f))
            msgBox.addView(View(c), LinearLayout.LayoutParams(c.dp(4f), 1))
        }
        msgBox.addView(c.label(ch.msg, 14f, UiColors.preview(dark)), LinearLayout.LayoutParams(WC, WC))
        bottom.addView(msgBox, LinearLayout.LayoutParams(0, WC, 1f))

        if (ch.mine) {
            val tick = Glyph(c, if (ch.read) Glyph.DONE_ALL else Glyph.CHECK, if (ch.read) UiColors.BLUE else 0xFF9CA3AF.toInt())
            val tp = LinearLayout.LayoutParams(c.dp(21f), c.dp(21f))
            tp.leftMargin = c.dp(8f)
            bottom.addView(tick, tp)
        } else if (ch.unread > 0) {
            val pill = c.label(ch.unread.toString(), 12f, Color.WHITE, 2)
            pill.gravity = Gravity.CENTER
            pill.minWidth = c.dp(20f)
            pill.minHeight = c.dp(20f)
            pill.setPadding(c.dp(6f), 0, c.dp(6f), 0)
            pill.background = c.rounded(if (ch.muted) 0xFF8E8E93.toInt() else UiColors.GREEN, 10f)
            val pp = LinearLayout.LayoutParams(WC, WC)
            pp.leftMargin = c.dp(8f)
            bottom.addView(pill, pp)
        }
        val bl = LinearLayout.LayoutParams(MP, WC)
        bl.topMargin = c.dp(3f)
        col.addView(bottom, bl)

        val cl = LinearLayout.LayoutParams(0, WC, 1f)
        cl.leftMargin = c.dp(14f)
        r.addView(col, cl)
        return r
    }

    private fun sized(sizeDp: Float, startDp: Float): LinearLayout.LayoutParams {
        val lp = LinearLayout.LayoutParams(c.dp(sizeDp), c.dp(sizeDp))
        lp.leftMargin = c.dp(startDp)
        return lp
    }

    private fun gapStart(startDp: Float): LinearLayout.LayoutParams {
        val lp = LinearLayout.LayoutParams(WC, WC)
        lp.leftMargin = c.dp(startDp)
        return lp
    }

    private fun botChip(): View {
        val tint = if (dark) 0xFF60A5FA.toInt() else 0xFF2563EB.toInt()
        val box = LinearLayout(c)
        box.orientation = LinearLayout.HORIZONTAL
        box.gravity = Gravity.CENTER_VERTICAL
        box.background = c.rounded(if (dark) 0x801E3A8A.toInt() else 0xFFDBEAFE.toInt(), 4f)
        box.setPadding(c.dp(5f), c.dp(2f), c.dp(5f), c.dp(2f))
        box.addView(Glyph(c, Glyph.BOT, tint), LinearLayout.LayoutParams(c.dp(12f), c.dp(12f)))
        val t = c.label("BOT", 10f, tint, 2)
        t.letterSpacing = 0.1f
        val lp = LinearLayout.LayoutParams(WC, WC)
        lp.leftMargin = c.dp(2f)
        box.addView(t, lp)
        return box
    }
}
