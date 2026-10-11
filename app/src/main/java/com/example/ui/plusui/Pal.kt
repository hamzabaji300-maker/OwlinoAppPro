package com.example.ui.plusui

import android.content.Context
import android.graphics.Color

/** Palette jour / nuit (choix mémorisé dans les préférences). */
class Pal(val dark: Boolean) {
    // Écran « Owlino Pro »
    val bg = if (dark) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
    val bgTransparent = if (dark) 0x00000000 else 0x00FFFFFF
    val card = if (dark) 0xFF2B2E33.toInt() else 0xFFF1F2F5.toInt()
    val sheet = if (dark) 0xFF2B2E33.toInt() else 0xFFFFFFFF.toInt()
    val text = if (dark) Color.WHITE else 0xFF111114.toInt()
    val grey = if (dark) 0xFF9A9DA3.toInt() else 0xFF6B6E75.toInt()
    val divider = if (dark) 0xFF383B40.toInt() else 0xFFDADCE1.toInt()
    val chev = if (dark) 0xFF8A8D93.toInt() else 0xFF9A9DA3.toInt()
    val dotOff = if (dark) 0xFF55585E.toInt() else 0xFFCDD0D5.toInt()
    val dotOn = text
    val radioOff = if (dark) 0xFFD0D2D6.toInt() else 0xFF9A9DA3.toInt()

    // Écran « Plume »
    val fBg = if (dark) 0xFF0F0F11.toInt() else 0xFFF0F0F3.toInt()
    val fCard = if (dark) 0xFF1C1C1E.toInt() else 0xFFFFFFFF.toInt()
    val fTitle = if (dark) Color.WHITE else 0xFF1C1C1E.toInt()
    val fSub = if (dark) 0xFFC7C7CC.toInt() else 0xFF3A3A3C.toInt()
    val fGrey = 0xFF8E8E93.toInt()

    companion object {
        /** Fourni par le thème de l'application (LocalSettingsTheme), plus par les SharedPreferences. */
        @Volatile var appDark: Boolean = true
        fun load(c: Context) = Pal(appDark)
    }
}
