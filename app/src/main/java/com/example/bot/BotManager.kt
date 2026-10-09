package com.example.bot

/** نسخة مبسّطة للمختبر: فقط ما تحتاجه لوحات البوت (BotKeyboards / BotBars). */
object BotManager {
    data class Command(val command: String, val description: String)
}

object BotManagerStrings {
    val START get() = "ابدأ"
    val MENU get() = "القائمة"
}
