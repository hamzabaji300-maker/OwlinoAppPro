package com.owlino.plus

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val k = resources.displayMetrics.widthPixels / 591f
        val root = FrameLayout(this)
        root.setBackgroundColor(Color.BLACK)
        val btn = TextView(this).apply {
            text = "Owlino Plus"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, 30f * k)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(0xFF6C5CE7.toInt())
                cornerRadius = 60f * k
            }
            setOnClickListener { startActivity(Intent(this@MainActivity, PlusActivity::class.java)) }
        }
        root.addView(btn, FrameLayout.LayoutParams((420 * k).toInt(), (96 * k).toInt(), Gravity.CENTER))
        setContentView(root)
    }
}
