package com.example.emoji

import android.icu.text.BreakIterator

object EmojiMessageUtils {

    // أقصى عدد إيموجيات باش تبقى الرسالة "إيموجي فقط" (بلا فقاعة).
    // التحريك يقرره العارض (ChatComponents): متحرك حتى 12، وأكثر يتعرض كنص خفيف.
    private const val MAX_UNITS = 60

    // يقسّم النص لإيموجيات كاملة (grapheme clusters): يتعامل صح مع الإيموجي المركّب
    // (ألوان البشرة، الأعلام، ZWJ مثل 👨‍👩‍👧، keycap ...) ويعتبره وحدة وحدة.
    fun splitGraphemes(text: String): List<String> {
        val result = mutableListOf<String>()
        val iter = BreakIterator.getCharacterInstance()
        iter.setText(text)
        var start = iter.first()
        var end = iter.next()
        while (end != BreakIterator.DONE) {
            result.add(text.substring(start, end))
            start = end
            end = iter.next()
        }
        return result
    }

    // إيموجيات تظهر كإيموجي بدون الحاجة لـ FE0F
    private fun isDefaultEmojiCodePoint(cp: Int): Boolean {
        if (cp in 0x1F300..0x1FAFF) return true
        if (cp in 0x1F1E6..0x1F1FF) return true
        if (cp == 0x1F004 || cp == 0x1F0CF || cp == 0x1F18E) return true
        if (cp in 0x1F191..0x1F19A) return true
        if (cp in 0x23E9..0x23EC) return true
        if (cp in 0x2648..0x2653) return true
        if (cp in 0x2753..0x2755) return true
        if (cp in 0x2795..0x2797) return true
        return cp == 0x231A || cp == 0x231B || cp == 0x23F0 || cp == 0x23F3 ||
            cp == 0x25FD || cp == 0x25FE || cp == 0x2614 || cp == 0x2615 ||
            cp == 0x267F || cp == 0x2693 || cp == 0x26A1 || cp == 0x26AA || cp == 0x26AB ||
            cp == 0x26BD || cp == 0x26BE || cp == 0x26C4 || cp == 0x26C5 || cp == 0x26CE ||
            cp == 0x26D4 || cp == 0x26EA || cp == 0x26F2 || cp == 0x26F3 || cp == 0x26F5 ||
            cp == 0x26FA || cp == 0x26FD || cp == 0x2705 || cp == 0x270A || cp == 0x270B ||
            cp == 0x2728 || cp == 0x274C || cp == 0x274E || cp == 0x2757 ||
            cp == 0x27B0 || cp == 0x27BF || cp == 0x2B1B || cp == 0x2B1C ||
            cp == 0x2B50 || cp == 0x2B55
    }

    private fun isEmojiGrapheme(g: String): Boolean {
        if (g.isEmpty()) return false
        if (g.contains('\u20E3')) return true // keycap مثل 1️⃣
        val cp = g.codePointAt(0)
        if (cp <= 0x7F || cp == 0xFE0F || cp == 0x200D) return false
        if (g.contains('\uFE0F')) return true // ❤️ ☺️ ✌️ ...
        return isDefaultEmojiCodePoint(cp)
    }

    fun isEmojiUnit(g: String): Boolean = isEmojiGrapheme(g)

    // إيموجيات موجودة في أي مكان من النص (ولو مختلطة مع كلام عادي)، بترتيب ظهورها.
    // تُستعمل لمعاينة الإيموجي متحركاً فوق صندوق الكتابة قبل الإرسال.
    fun extractEmojisAnywhere(text: String, maxCount: Int = 20): List<String> {
        if (text.isEmpty()) return emptyList()
        val result = mutableListOf<String>()
        for (g in splitGraphemes(text)) {
            if (isEmojiGrapheme(g)) {
                result.add(g)
                if (result.size >= maxCount) break
            }
        }
        return result
    }

    // إذا الرسالة كلها إيموجي (وحدة أو أكثر) يرجع القائمة، غير كذا null.
    // ما نشيلوش FE0F لأن Google تسمّي بعض الملفات بيه (مثل 2764_fe0f).
    fun parseSupportedEmojiSequence(text: String): List<String>? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        val units = splitGraphemes(trimmed)
        if (units.isEmpty() || units.size > MAX_UNITS) return null
        if (units.any { !isEmojiGrapheme(it) }) return null
        return units
    }
}
