package com.example.util

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

/**
 * Public media folders, exactly like Telegram / WhatsApp:
 *   /storage/emulated/0/Owlino/Owlino Images, Owlino Video, Owlino Audio, Owlino Voice Notes, Owlino Documents
 * The public folder needs "All files access" (API 30+) or WRITE_EXTERNAL_STORAGE (API < 30).
 * Without it, files go to a private folder with the same layout (still works, but hidden from file managers).
 */
object MediaStorage {
    const val ROOT_NAME = "Owlino"
    private const val PREFS = "media_permission"

    enum class Kind(val folder: String) {
        IMAGE("Owlino Images"),
        VIDEO("Owlino Video"),
        AUDIO("Owlino Audio"),
        VOICE("Owlino Voice Notes"),
        DOCUMENT("Owlino Documents")
    }

    fun hasPublicAccess(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun publicRoot(): File = File(Environment.getExternalStorageDirectory(), ROOT_NAME)

    fun privateRoot(context: Context): File {
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        return File(base, ROOT_NAME)
    }

    fun root(context: Context): File = if (hasPublicAccess(context)) publicRoot() else privateRoot(context)

    fun allRoots(context: Context): List<File> = listOf(publicRoot(), privateRoot(context)).distinct()

    fun dirFor(context: Context, kind: Kind): File = File(root(context), kind.folder)

    fun ensureFolders(context: Context) {
        try {
            Kind.values().forEach { dirFor(context, it).mkdirs() }
        } catch (e: Exception) {
            Log.e("MediaStorage", "ensureFolders failed", e)
        }
    }

    fun fileNameFor(messageId: String, ext: String): String = "OWL-$messageId.$ext"

    fun scanFile(context: Context, file: File) {
        try {
            MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
        } catch (e: Exception) {
            Log.e("MediaStorage", "scan failed", e)
        }
    }

    // ---------------- permission prompt (shown once when entering the app) ----------------

    fun shouldPrompt(context: Context): Boolean {
        if (hasPublicAccess(context)) return false
        val asked = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("asked", 0)
        return asked < 2
    }

    fun markAsked(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        p.edit().putInt("asked", p.getInt("asked", 0) + 1).apply()
    }

    private fun findActivity(context: Context): Activity? {
        var c: Context? = context
        while (c is ContextWrapper) {
            if (c is Activity) return c
            c = c.baseContext
        }
        return null
    }

    fun requestPublicAccess(context: Context) {
        markAsked(context)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    .setData(Uri.parse("package:" + context.packageName))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            } else {
                val act = findActivity(context)
                if (act != null) {
                    ActivityCompat.requestPermissions(act, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 4711)
                }
            }
        } catch (e: Exception) {
            Log.e("MediaStorage", "permission request failed", e)
        }
    }

    // ---------------- open a downloaded file with an external app ----------------

    fun mimeFor(file: File, fallback: String? = null): String {
        val ext = file.extension.lowercase()
        // امتداد bin لا يدل على النوع، فنعتمد النوع الذي نعرفه من الرسالة
        if (ext == "bin" && !fallback.isNullOrBlank()) return fallback
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: fallback ?: "*/*"
    }

    fun openFile(context: Context, path: String, mime: String? = null) {
        try {
            val file = File(path)
            val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, mimeFor(file, mime))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No app can open this file", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("MediaStorage", "open failed", e)
        }
    }

    /** اسم الملف من ترويسة Content-Disposition (يدعم filename*=UTF-8'' و filename=). */
    fun nameFromDisposition(cd: String?): String? {
        if (cd.isNullOrBlank()) return null
        Regex("filename\\*\\s*=\\s*UTF-8''([^;]+)", RegexOption.IGNORE_CASE).find(cd)?.groupValues?.get(1)?.let {
            return try { java.net.URLDecoder.decode(it.trim().replace("+", "%2B"), "UTF-8") } catch (e: Exception) { it }
        }
        Regex("filename\\s*=\\s*\"?([^\";]+)\"?", RegexOption.IGNORE_CASE).find(cd)?.groupValues?.get(1)?.let { return it.trim() }
        return null
    }

    fun openRemote(context: Context, url: String, mime: String) {
        // ملفات R2 تحتاج جلسة المستخدم، والتطبيقات الخارجية لا تستطيع إرسالها: نحمّل أولًا ثم نفتح محليًا
        if (MediaAuth.isWorkerUrl(url)) {
            val app = context.applicationContext
            Toast.makeText(app, "جارٍ فتح الملف...", Toast.LENGTH_SHORT).show()
            Thread {
                var failure: String? = null
                var opened: File? = null
                var openMime = mime
                var tmp: File? = null
                var conn: java.net.HttpURLConnection? = null
                try {
                    val kind = if (mime.startsWith("video")) Kind.VIDEO else Kind.DOCUMENT
                    val dir = dirFor(app, kind)
                    dir.mkdirs()
                    val hash = Integer.toHexString(url.substringBefore('?').hashCode())
                    // نسخة مكتملة من فتح سابق (الملفات الناقصة تبقى .tmp ولا تُعدّ سليمة أبدًا)
                    val cached = dir.listFiles { f ->
                        f.isFile && f.name.startsWith("open_$hash.") && !f.name.endsWith(".tmp") && f.length() > 0L
                    }?.firstOrNull()
                    if (cached != null) {
                        opened = cached
                    } else {
                        val c = MediaAuth.openAuthorized(url, 15000, 60000)
                        conn = c
                        val code = c.responseCode
                        if (code == 404 || code == 410) failure = "انتهت صلاحية الملف"
                        else if (code !in 200..299) failure = "تعذّر فتح الملف"
                        else {
                            val headerType = c.getHeaderField("Content-Type")?.substringBefore(';')?.trim()?.lowercase()
                                ?.takeIf { it.isNotEmpty() && it != "application/octet-stream" }
                            if ((openMime == "*/*" || openMime.isBlank()) && headerType != null) openMime = headerType
                            val headerExt = nameFromDisposition(c.getHeaderField("Content-Disposition"))
                                ?.substringAfterLast('.', "")?.takeIf { it.length in 1..5 }?.lowercase()
                            val ext = if (openMime.startsWith("video")) "mp4"
                                else headerExt
                                    ?: MimeTypeMap.getSingleton().getExtensionFromMimeType(openMime)
                                    ?: "bin"
                            val file = File(dir, "open_$hash.$ext")
                            val part = File(dir, file.name + ".tmp")
                            tmp = part
                            val expected = c.contentLengthLong
                            var copied = 0L
                            c.inputStream.use { i -> part.outputStream().use { o -> copied = i.copyTo(o) } }
                            if (copied <= 0L || (expected > 0 && copied != expected)) {
                                // قُطع الاتصال في المنتصف: لا نترك ملفًا ناقصًا
                                part.delete()
                                failure = "تعذّر فتح الملف"
                            } else {
                                if (!part.renameTo(file)) {
                                    part.copyTo(file, overwrite = true)
                                    part.delete()
                                }
                                opened = file
                            }
                        }
                    }
                } catch (e: Exception) {
                    try { tmp?.delete() } catch (_: Exception) {}
                    failure = "تعذّر فتح الملف"
                } finally {
                    try { conn?.disconnect() } catch (_: Exception) {}
                }
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    val f = opened
                    if (f != null) openFile(app, f.absolutePath, openMime)
                    else Toast.makeText(app, failure ?: "تعذّر فتح الملف", Toast.LENGTH_SHORT).show()
                }
            }.start()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(url), mime)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("MediaStorage", "open remote failed", e)
        }
    }
}
