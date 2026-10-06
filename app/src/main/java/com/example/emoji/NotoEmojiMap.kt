package com.example.emoji

/**
 * إيموجيات Google Noto المتحركة (Lottie) — مستضافة رسمياً من Google (fonts.gstatic.com).
 * ترخيص الملفات: CC BY 4.0 (مجاني للاستعمال).
 *
 * ما عادش عندنا قائمة يدوية: الرابط يتحسب مباشرة من رموز Unicode الإيموجي
 * (مثال: 😂 -> 1f602 ، ❤️ -> 2764_fe0f ، 👍🏽 -> 1f44d_1f3fd)، فكل إيموجي عند Google
 * ملف متحرك ليه يشتغل تلقائياً. اللي ما عندوش ملف يرجع لخط النظام (شوف LottieEmojiReaction).
 */
object NotoEmojiMap {

    private const val BASE_URL = "https://fonts.gstatic.com/s/e/notoemoji/latest"

    // الرموز السداسية مفصولة بـ "_" (بحروف صغيرة)
    fun assetFor(emoji: String): String? {
        if (emoji.isEmpty()) return null
        val parts = mutableListOf<String>()
        var i = 0
        while (i < emoji.length) {
            val cp = emoji.codePointAt(i)
            parts.add(Integer.toHexString(cp))
            i += Character.charCount(cp)
        }
        return parts.joinToString("_")
    }

    fun remoteUrlFor(emoji: String): String? =
        assetFor(emoji)?.let { "$BASE_URL/$it/lottie.json" }

    fun isSupported(emoji: String): Boolean = EmojiMessageUtils.isEmojiUnit(emoji)

    // ردود الفعل القديمة كانت محفوظة بأسماء Rive ("joy" ...) — نحوّلوها لإيموجي Noto مقابل.
    // الردود الجديدة تتخزن مباشرة كإيموجي، فترجع كما هي.
    fun reactionToEmoji(reaction: String): String = when (reaction) {
        "joy" -> "\uD83D\uDE02"        // 😂
        "Mindblown" -> "\uD83E\uDD2F"  // 🤯
        "love" -> "\uD83D\uDE0D"       // 😍
        "Bullseye" -> "\uD83C\uDFAF"   // 🎯
        "Onfire" -> "\uD83D\uDD25"     // 🔥
        "Tada" -> "\uD83C\uDF89"       // 🎉
        else -> reaction
    }
}
