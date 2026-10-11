package com.example.util

/**
 * صلاحية ملفات R2: رابط الملف يحمل وقت انتهائه (المعامل e بالثواني).
 * الملفات القديمة على Supabase لا تحمل هذا المعامل فلا تنتهي.
 */
object MediaExpiry {

    /** وقت انتهاء الرابط بالمللي ثانية، أو null إن لم يكن رابط R2. */
    fun expiresAtMillis(url: String): Long? {
        if (!url.contains("/m/")) return null
        val query = url.substringAfter('?', "")
        if (query.isEmpty()) return null
        val e = query.split('&')
            .firstOrNull { it.startsWith("e=") }
            ?.substringAfter("e=")
            ?.toLongOrNull() ?: return null
        return e * 1000L
    }

    fun isExpired(url: String): Boolean {
        val at = expiresAtMillis(url) ?: return false
        return System.currentTimeMillis() > at
    }

    const val LABEL = "انتهت صلاحية الملف"
}
