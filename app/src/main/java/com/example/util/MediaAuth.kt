package com.example.util

import com.example.BuildConfig
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.sync.withLock
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

    private val refreshLock = kotlinx.coroutines.sync.Mutex()

    /**
     * يجدّد جلسة المستخدم ويرجع التوكن الجديد (أو null عند الفشل).
     * [stale] هو التوكن الذي فشل: إن كان عامل آخر قد جدّد الجلسة للتو نستعمل الجديد دون تجديد ثانٍ.
     */
    suspend fun refreshToken(stale: String? = null): String? = refreshLock.withLock {
        try {
            val cur = token()
            if (stale != null && cur != null && cur != stale) return@withLock cur
            com.example.supabase.auth.refreshCurrentSession()
            token()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    /** نسخة حاجزة من [refreshToken] للاستعمال داخل معترضات OkHttp وخيوط عادية. */
    fun refreshTokenBlocking(stale: String? = null): String? =
        kotlinx.coroutines.runBlocking { refreshToken(stale) }

    /** يضيف الجلسة إلى اتصال HttpURLConnection إن كان الرابط للـ Worker. */
    fun apply(conn: HttpURLConnection, url: String) {
        if (!isWorkerUrl(url)) return
        token()?.let { conn.setRequestProperty("Authorization", "Bearer $it") }
    }

    /**
     * يفتح اتصال تنزيل ويقرأ رمز الاستجابة. إن رجع 401 لرابط الـ Worker يجدّد الجلسة
     * ويعيد الطلب مرة واحدة فقط. الاتصال المرجَع رمزه جاهز (responseCode) ويجب إغلاقه من المستدعي.
     * ينبغي استدعاؤه من خيط خلفي.
     */
    fun openAuthorized(url: String, connectTimeoutMs: Int = 15_000, readTimeoutMs: Int = 30_000): HttpURLConnection {
        fun open(auth: String?): HttpURLConnection {
            val c = java.net.URL(url).openConnection() as HttpURLConnection
            c.connectTimeout = connectTimeoutMs
            c.readTimeout = readTimeoutMs
            c.instanceFollowRedirects = true
            // بلا ضغط: حتى يكون Content-Length مطابقًا لعدد البايتات الفعلي فنكشف أي قطع في التنزيل
            c.setRequestProperty("Accept-Encoding", "identity")
            if (auth != null) c.setRequestProperty("Authorization", "Bearer $auth")
            return c
        }
        val worker = isWorkerUrl(url)
        val first = if (worker) token() else null
        var conn = open(first)
        if (worker && conn.responseCode == 401) {
            conn.disconnect()
            val fresh = refreshTokenBlocking(first)
            conn = open(fresh ?: first)
        }
        conn.responseCode
        return conn
    }

    /** رؤوس الطلب (للمشغّل الصوتي مثلًا). */
    fun headersFor(url: String): Map<String, String> {
        if (!isWorkerUrl(url)) return emptyMap()
        val t = token() ?: return emptyMap()
        return mapOf("Authorization" to "Bearer $t")
    }
}
