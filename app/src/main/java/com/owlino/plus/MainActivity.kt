package com.owlino.plus

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private var k = 1f

    private fun pill(text: String, size: Float, fill: Int, textColor: Int, onClick: () -> Unit): TextView =
        TextView(this).apply {
            this.text = text
            setTextColor(textColor)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, size * k)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = GradientDrawable().apply { setColor(fill); cornerRadius = 60f * k }
            setOnClickListener { onClick() }
        }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        k = resources.displayMetrics.widthPixels / 591f
        val pal = Pal.load(this)
        window.statusBarColor = pal.bg
        window.navigationBarColor = pal.bg
        window.decorView.systemUiVisibility =
            if (pal.dark) 0 else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(pal.bg)
        }
        fun lp(w: Int, h: Int, top: Int) = LinearLayout.LayoutParams(w, h).apply { topMargin = top }

        col.addView(pill("Owlino Plus", 30f, 0xFF6C5CE7.toInt(), Color.WHITE) {
            startActivity(Intent(this, PlusActivity::class.java))
        }, lp((420 * k).toInt(), (96 * k).toInt(), 0))

        col.addView(pill("Owlino Feather", 30f, 0xFF19BD6B.toInt(), Color.WHITE) {
            startActivity(Intent(this, FeatherActivity::class.java))
        }, lp((420 * k).toInt(), (96 * k).toInt(), (28 * k).toInt()))

        // Choix du mode d'affichage
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val on = 0xFF6C5CE7.toInt()
        val off = if (pal.dark) 0xFF2B2E33.toInt() else 0xFFE4E5EA.toInt()
        val offText = if (pal.dark) Color.WHITE else 0xFF111114.toInt()
        row.addView(pill("الوضع الليلي", 24f, if (pal.dark) on else off, if (pal.dark) Color.WHITE else offText) {
            Pal.save(this, true); recreate()
        }, LinearLayout.LayoutParams((200 * k).toInt(), (72 * k).toInt()))
        row.addView(pill("الوضع النهاري", 24f, if (pal.dark) off else on, if (pal.dark) offText else Color.WHITE) {
            Pal.save(this, false); recreate()
        }, LinearLayout.LayoutParams((200 * k).toInt(), (72 * k).toInt()).apply { leftMargin = (20 * k).toInt() })
        col.addView(row, lp(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, (60 * k).toInt()))

        setContentView(col)
    }
}
