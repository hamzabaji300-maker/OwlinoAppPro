package com.owlino.plus

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Écran « Followers » : même barre du haut que l'accueil, puis onglets Followers / Following / Mutual
 * et la liste des utilisateurs. Interface seulement, données de démonstration.
 */
class FollowersScreenView(
    private val c: Context,
    private val dark: Boolean,
    private val onSearching: (Boolean) -> Unit
) : LinearLayout(c) {

    private class U(
        val name: String,
        val username: String,
        val color: Int,
        val verified: Boolean = false,
        val isNew: Boolean = false,
        var following: Boolean = false,
        val mutual: Boolean = false
    )

    private val users = listOf(
        U("Elena Rostova", "elena_r", 0xFFEC4899.toInt(), verified = true),
        U("Marcus Chen", "marcus.c", 0xFF0EA5E9.toInt(), isNew = true, following = true, mutual = true),
        U("Sarah Jenkins", "sarahj", 0xFF8B5CF6.toInt(), following = true, mutual = true),
        U("Alex Rivera", "arivera99", 0xFFF97316.toInt()),
        U("David Kim", "dkim_dev", 0xFF10B981.toInt()),
        U("Emma Watson", "emma.w", 0xFFF43F5E.toInt(), following = true, mutual = true),
        U("Lucas Silva", "lucass", 0xFF6366F1.toInt()),
        U("Tech Insider", "techinsider", 0xFF14B8A6.toInt(), verified = true, following = true, mutual = true),
        U("Design UI", "design_ui", 0xFFA855F7.toInt()),
        U("Crypto King", "cryptoking", 0xFFEAB308.toInt())
    )

    private var tab = 0
    private var query = ""
    private val chips = ArrayList<TextView>()
    private val dots = ArrayList<View>()
    private val listBox = LinearLayout(c)
    val topBar: TopBarView

    init {
        orientation = VERTICAL
        setBackgroundColor(UiColors.bg(dark))
        topBar = TopBarView(c, dark, "Search messages...", { s -> onSearching(s) }, { q ->
            query = q
            rebuild()
        })
        addView(topBar, LayoutParams(MP, WC))

        // Onglets : Followers / Following / Mutual + flèche de tri
        val rowWrap = LinearLayout(c)
        rowWrap.orientation = HORIZONTAL
        rowWrap.gravity = Gravity.CENTER_VERTICAL
        rowWrap.setPadding(c.dp(16f), c.dp(10f), c.dp(16f), c.dp(16f))
        val chipsRow = LinearLayout(c)
        chipsRow.orientation = HORIZONTAL
        val names = listOf("Followers", "Following", "Mutual")
        names.forEachIndexed { i, n ->
            val holder = FrameLayout(c)
            val chip = c.label(n, 13f, 0, 2)
            chip.setPadding(c.dp(16f), c.dp(6f), c.dp(16f), c.dp(6f))
            chip.setOnClickListener {
                tab = i
                updateChips()
                rebuild()
            }
            chips.add(chip)
            holder.addView(chip, FrameLayout.LayoutParams(WC, WC))
            val dot = View(c)
            dot.background = c.oval(UiColors.BLUE)
            holder.addView(dot, FrameLayout.LayoutParams(c.dp(7f), c.dp(7f), Gravity.TOP or Gravity.END))
            dots.add(dot)
            val lp = LayoutParams(WC, WC)
            if (i > 0) lp.leftMargin = c.dp(8f)
            chipsRow.addView(holder, lp)
        }
        val hs = HorizontalScrollView(c)
        hs.isHorizontalScrollBarEnabled = false
        hs.addView(chipsRow, FrameLayout.LayoutParams(WC, WC))
        rowWrap.addView(hs, LayoutParams(0, WC, 1f))
        val sortBox = FrameLayout(c)
        sortBox.setPadding(c.dp(8f), c.dp(8f), c.dp(8f), c.dp(8f))
        sortBox.addView(Glyph(c, Glyph.CHEV_DOWN, UiColors.text(dark)), FrameLayout.LayoutParams(c.dp(16f), c.dp(16f)))
        rowWrap.addView(sortBox, LayoutParams(WC, WC))
        addView(rowWrap, LayoutParams(MP, WC))

        listBox.orientation = VERTICAL
        val sv = ScrollView(c)
        sv.isVerticalScrollBarEnabled = false
        sv.addView(listBox, FrameLayout.LayoutParams(MP, WC))
        addView(sv, LayoutParams(MP, 0, 1f))

        updateChips()
        rebuild()
    }

    fun isSearching() = topBar.isSearching
    fun closeSearch() = topBar.closeSearch()

    private fun updateChips() {
        chips.forEachIndexed { i, chip ->
            val sel = i == tab
            chip.background = c.rounded(if (sel) UiColors.text(dark) else UiColors.surface(dark), 20f)
            chip.setTextColor(if (sel) UiColors.bg(dark) else UiColors.text(dark))
        }
        // Point bleu sur « Followers » quand il y a de nouveaux abonnés et que l'onglet n'est pas actif
        dots.forEachIndexed { i, d -> d.visibility = if (i == 0 && tab != 0 && users.any { it.isNew }) View.VISIBLE else View.GONE }
    }

    private fun currentList(): List<U> {
        val base = when (tab) {
            0 -> users.take(8)
            1 -> users.drop(1).take(8)
            else -> users.filter { it.mutual }
        }
        val q = query.trim().lowercase()
        return if (q.isEmpty()) base else base.filter { it.name.lowercase().contains(q) || it.username.lowercase().contains(q) }
    }

    private fun rebuild() {
        listBox.removeAllViews()
        val shown = currentList()
        if (shown.isEmpty()) {
            val box = LinearLayout(c)
            box.orientation = VERTICAL
            box.gravity = Gravity.CENTER_HORIZONTAL
            box.setPadding(0, c.dp(80f), 0, 0)
            val circle = View(c)
            circle.background = c.oval(UiColors.chip(dark))
            box.addView(circle, LayoutParams(c.dp(80f), c.dp(80f)))
            val t1 = c.label("Users not found", 20f, UiColors.text(dark), 2)
            val l1 = LayoutParams(WC, WC)
            l1.topMargin = c.dp(16f)
            box.addView(t1, l1)
            val t2 = c.label("No matching search", 15f, 0xFF9CA3AF.toInt())
            val l2 = LayoutParams(WC, WC)
            l2.topMargin = c.dp(8f)
            box.addView(t2, l2)
            listBox.addView(box, LayoutParams(MP, WC))
            return
        }
        shown.forEach { listBox.addView(row(it), LayoutParams(MP, WC)) }
    }

    private fun styleButton(b: TextView, u: U) {
        b.text = if (u.following) "Following" else "Follow"
        b.background = c.rounded(if (u.following) UiColors.chip(dark) else UiColors.BLUE, 8f)
        b.setTextColor(if (u.following) UiColors.text(dark) else Color.WHITE)
    }

    private fun row(u: U): View {
        val r = LinearLayout(c)
        r.orientation = HORIZONTAL
        r.gravity = Gravity.CENTER_VERTICAL
        r.setPadding(c.dp(16f), c.dp(8f), c.dp(16f), c.dp(8f))

        r.addView(
            CircleAvatar(c, u.name.take(1).uppercase(), u.color, Color.WHITE, UiColors.chip(dark)),
            LayoutParams(c.dp(52f), c.dp(52f))
        )

        val col = LinearLayout(c)
        col.orientation = VERTICAL
        val nameRow = LinearLayout(c)
        nameRow.orientation = HORIZONTAL
        nameRow.gravity = Gravity.CENTER_VERTICAL
        nameRow.addView(c.label(u.username, 14.5f, UiColors.text(dark), 2), LayoutParams(WC, WC))
        if (u.verified) {
            val lp = LayoutParams(c.dp(16f), c.dp(16f))
            lp.leftMargin = c.dp(4f)
            nameRow.addView(Glyph(c, Glyph.VERIFIED, 0), lp)
        }
        if (u.isNew) {
            val d = View(c)
            d.background = c.oval(UiColors.BLUE)
            val lp = LayoutParams(c.dp(8f), c.dp(8f))
            lp.leftMargin = c.dp(6f)
            nameRow.addView(d, lp)
        }
        col.addView(nameRow, LayoutParams(MP, WC))
        col.addView(c.label(u.name, 13.5f, UiColors.text2(dark)), LayoutParams(WC, WC))
        val cl = LayoutParams(0, WC, 1f)
        cl.leftMargin = c.dp(12f)
        r.addView(col, cl)

        val btn = c.label("", 14f, 0, 2)
        btn.gravity = Gravity.CENTER
        btn.setPadding(c.dp(20f), 0, c.dp(20f), 0)
        styleButton(btn, u)
        btn.setOnClickListener {
            u.following = !u.following
            styleButton(btn, u)
        }
        val bl = LayoutParams(WC, c.dp(32f))
        bl.leftMargin = c.dp(8f)
        r.addView(btn, bl)

        val more = FrameLayout(c)
        more.addView(Glyph(c, Glyph.MORE, 0xFF9CA3AF.toInt()), FrameLayout.LayoutParams(c.dp(22f), c.dp(22f), Gravity.CENTER))
        r.addView(more, LayoutParams(c.dp(36f), c.dp(36f)))
        return r
    }
}
