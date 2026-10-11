package com.example.util

import android.content.Context
import android.net.Uri
import com.example.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * الرفع إلى Cloudflare R2 عبر الـ Worker الخاص بنا (ملف cloudflare/media-worker.js).
 * الـ Worker يتحقق أن المستخدم عضو في المحادثة ثم يحفظ الملف ويرجع رابط تحميل موقّع.
 * إن لم يُضبط عنوان الـ Worker (ملف media_worker.url) يعمل التطبيق كالسابق عبر Supabase Storage.
 */
object R2Uploader {

    val isConfigured: Boolean
        get() = BuildConfig.MEDIA_WORKER_URL.isNotBlank()

    suspend fun uploadBytes(
        supabase: SupabaseClient,
        chatId: String,
        scope: String,
        kind: String,
        contentType: String,
        bytes: ByteArray
    ): String = send(supabase, chatId, scope, kind, contentType, bytes.size.toLong()) { it.write(bytes) }

    /** رفع بالتدفق من الملف مباشرة (دون تحميله كله في الذاكرة). */
    suspend fun uploadStream(
        context: Context,
        supabase: SupabaseClient,
        chatId: String,
        scope: String,
        kind: String,
        contentType: String,
        uri: Uri,
        size: Long
    ): String = send(supabase, chatId, scope, kind, contentType, size) { out ->
        val input = context.contentResolver.openInputStream(uri)
            ?: throw LocalMediaException("تعذّر قراءة الملف")
        input.use { it.copyTo(out) }
    }

    private suspend fun send(
        supabase: SupabaseClient,
        chatId: String,
        scope: String,
        kind: String,
        contentType: String,
        size: Long,
        write: (OutputStream) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val base = BuildConfig.MEDIA_WORKER_URL.trim().trimEnd('/')
        val endpoint = "$base/upload?scope=${enc(scope)}&chat=${enc(chatId)}&kind=${enc(kind)}"
        var token = supabase.auth.currentAccessTokenOrNull()
            ?: throw IOException("لا توجد جلسة دخول")

        for (attempt in 0..1) {
            val conn = URL(endpoint).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "PUT"
                conn.doOutput = true
                conn.connectTimeout = 15_000
                conn.readTimeout = 90_000
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.setRequestProperty("Content-Type", contentType)
                conn.setFixedLengthStreamingMode(size)
                conn.outputStream.use { write(it) }

                val code = conn.responseCode
                val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.use { it.readText() } ?: ""

                when {
                    code in 200..299 -> return@withContext JSONObject(body).getString("url")
                    code == 401 && attempt == 0 -> {
                        // الجلسة انتهت: نجدّدها ونعيد المحاولة مرة واحدة
                        try {
                            supabase.auth.refreshCurrentSession()
                            token = supabase.auth.currentAccessTokenOrNull() ?: token
                        } catch (e: Exception) {
                        }
                    }
                    code == 413 -> throw UploadRejectedException("حجم الملف يتجاوز الحد المسموح")
                    code == 429 -> throw UploadRejectedException("وصلت إلى حد الرفع اليومي. حاول غدًا أو اشترك في Pro لحد أكبر")
                    code == 403 -> throw UploadRejectedException("غير مسموح لك بإرسال ملفات في هذه المحادثة")
                    code in 400..499 -> throw UploadRejectedException("رفض الخادم الملف (رمز $code)")
                    else -> throw IOException("خطأ في خادم الملفات (رمز $code)")
                }
            } finally {
                conn.disconnect()
            }
        }
        throw IOException("تعذّر رفع الملف، حاول مجددًا")
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
}
