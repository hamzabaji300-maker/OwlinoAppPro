package com.example.util

import android.content.Context
import android.net.Uri
import com.example.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
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
        bytes: ByteArray,
        fileName: String? = null,
        onProgress: ((Float) -> Unit)? = null
    ): String = send(supabase, chatId, scope, kind, contentType, bytes.size.toLong(), fileName) { out ->
        var off = 0
        while (off < bytes.size) {
            val n = minOf(65536, bytes.size - off)
            out.write(bytes, off, n)
            off += n
            onProgress?.invoke(off.toFloat() / bytes.size)
        }
    }

    /**
     * رفع بالتدفق (دون تحميل الملف كله في الذاكرة).
     * بعض مزوّدي content:// يعطون حجمًا خاطئًا في OpenableColumns.SIZE، لذلك ننسخ المصدر أولًا
     * إلى ملف في cacheDir ونرفع هذا الملف بطوله الحقيقي. ملفات file:// تُرفع مباشرة.
     * [size] يُبقى للتوافق فقط ولا يُعتمد عليه.
     */
    @Suppress("UNUSED_PARAMETER")
    suspend fun uploadStream(
        context: Context,
        supabase: SupabaseClient,
        chatId: String,
        scope: String,
        kind: String,
        contentType: String,
        uri: Uri,
        size: Long,
        fileName: String? = null,
        onSized: ((Long) -> Unit)? = null,
        onProgress: ((Float) -> Unit)? = null
    ): String {
        var temp: File? = null
        try {
            val file: File = withContext(Dispatchers.IO) {
                val path = uri.path
                if (uri.scheme == "file" && path != null) {
                    File(path)
                } else {
                    val t = File.createTempFile("upl_", ".tmp", context.cacheDir)
                    temp = t
                    val input = context.contentResolver.openInputStream(uri)
                        ?: throw LocalMediaException("تعذّر قراءة الملف")
                    input.use { src -> t.outputStream().use { dst -> src.copyTo(dst) } }
                    t
                }
            }
            val real = file.length()
            if (real <= 0L) throw LocalMediaException("الملف فارغ أو تعذّرت قراءته")
            onSized?.invoke(real)
            return send(supabase, chatId, scope, kind, contentType, real, fileName) { out ->
                file.inputStream().use { src ->
                    val buf = ByteArray(65536)
                    var sent = 0L
                    while (true) {
                        val n = src.read(buf)
                        if (n <= 0) break
                        out.write(buf, 0, n)
                        sent += n
                        onProgress?.invoke(sent.toFloat() / real)
                    }
                    if (sent != real) throw IOException("حجم الملف تغيّر أثناء الرفع")
                }
            }
        } finally {
            try { temp?.delete() } catch (_: Exception) {}
        }
    }

    private suspend fun send(
        supabase: SupabaseClient,
        chatId: String,
        scope: String,
        kind: String,
        contentType: String,
        size: Long,
        fileName: String?,
        write: (OutputStream) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val base = BuildConfig.MEDIA_WORKER_URL.trim().trimEnd('/')
        val nameParam = fileName?.takeIf { it.isNotBlank() }?.let { "&name=${enc(it.take(180))}" } ?: ""
        val endpoint = "$base/upload?scope=${enc(scope)}&chat=${enc(chatId)}&kind=${enc(kind)}$nameParam"
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
