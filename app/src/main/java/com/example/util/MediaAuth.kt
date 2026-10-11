package com.example.util

import com.example.BuildConfig
import io.github.jan.supabase.auth.auth
import java.net.HttpURLConnection
import java.net.URI

/**
 * ملفات الدردشة على R2 لا تُفتح بالرابط وحده: يلزم تسجيل دخول مستخدم له حق القراءة.
 * هذا الكائن يضيف جلسة المستخدم (Authorization) لأي طلب موجّه إلى الـ Worker الخاص بنا فقط،
 * ولا يرسلها أبدًا إلى أي موقع آخر.
 */
object MediaAuth {

    private val workerHost: String? by lazy {
        try {
            BuildConfig.MEDIA_WORKER_URL.trim().takeIf { it.isNotEmpty() }?.let { URI(it).host }
        } catch (e: Exception) {
            null
        }
    }

    fun isWorkerUrl(url: String): Boolean {
        val host = workerHost ?: return false
        return try {
            URI(url).host == host
        } catch (e: Exception) {
            false
        }
    }

    fun token(): String? = try {
        com.example.supabase.auth.currentAccessTokenOrNull()
    } catch (e: Exception) {
        null
    }

    /** يضيف الجلسة إلى اتصال HttpURLConnection إن كان الرابط للـ Worker. */
    fun apply(conn: HttpURLConnection, url: String) {
        if (!isWorkerUrl(url)) return
        token()?.let { conn.setRequestProperty("Authorization", "Bearer $it") }
    }

    /** رؤوس الطلب (للمشغّل الصوتي مثلًا). */
    fun headersFor(url: String): Map<String, String> {
        if (!isWorkerUrl(url)) return emptyMap()
        val t = token() ?: return emptyMap()
        return mapOf("Authorization" to "Bearer $t")
    }
}
