package com.example.ui

import android.content.Context
import android.util.Log
import com.example.util.MediaAuth
import com.example.util.MediaStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

sealed class MediaDlState {
    data class Downloading(val progress: Float, val bytes: Long, val total: Long) : MediaDlState()
    data class Failed(val reason: String) : MediaDlState()
}

/**
 * Downloads chat media into the public Owlino folders. Application-level scope: a download keeps running
 * if the user leaves the chat.
 */
object MediaDownloadManager {
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + kotlinx.coroutines.CoroutineExceptionHandler { _, e ->
            Log.e("MediaDownload", "Unhandled error", e)
        }
    )
    private val jobs = ConcurrentHashMap<String, Job>()

    private val _states = MutableStateFlow<Map<String, MediaDlState>>(emptyMap())
    val states: StateFlow<Map<String, MediaDlState>> = _states

    private fun setState(id: String, s: MediaDlState) { _states.value = _states.value + (id to s) }
    private fun clearState(id: String) { _states.value = _states.value - id }

    fun kindFor(type: AttachmentType): MediaStorage.Kind = when (type) {
        AttachmentType.IMAGE -> MediaStorage.Kind.IMAGE
        AttachmentType.VIDEO -> MediaStorage.Kind.VIDEO
        AttachmentType.AUDIO -> MediaStorage.Kind.AUDIO
        AttachmentType.VOICE -> MediaStorage.Kind.VOICE
        AttachmentType.DOCUMENT -> MediaStorage.Kind.DOCUMENT
    }

    /** اسم الملف من ترويسة Content-Disposition (يدعم filename*=UTF-8'' و filename=). */
    private fun nameFromDisposition(cd: String?): String? {
        if (cd.isNullOrBlank()) return null
        Regex("filename\\*\\s*=\\s*UTF-8''([^;]+)", RegexOption.IGNORE_CASE).find(cd)?.groupValues?.get(1)?.let {
            return try { java.net.URLDecoder.decode(it.trim().replace("+", "%2B"), "UTF-8") } catch (e: Exception) { it }
        }
        Regex("filename\\s*=\\s*\"?([^\";]+)\"?", RegexOption.IGNORE_CASE).find(cd)?.groupValues?.get(1)?.let { return it.trim() }
        return null
    }

    private fun cleanMime(raw: String?): String? =
        raw?.substringBefore(';')?.trim()?.lowercase()?.takeIf { it.isNotEmpty() && it != "application/octet-stream" }

    /** الامتداد الأنسب: اسم الرسالة، ثم الترويسات، ثم النوع، ثم الرابط، و bin آخر خيار. */
    private fun extFor(att: Attachment, headerName: String?, headerType: String?): String {
        att.fileName?.substringAfterLast('.', "")?.takeIf { it.length in 1..5 }?.let { return it.lowercase() }
        headerName?.substringAfterLast('.', "")?.takeIf { it.length in 1..5 }?.let { return it.lowercase() }
        val mime = cleanMime(att.mimeType) ?: cleanMime(headerType)
        mime?.let { m ->
            android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(m)?.takeIf { it.isNotEmpty() }?.let { return it.lowercase() }
        }
        att.url.substringBefore('?').substringAfterLast('.', "").takeIf { it.length in 2..4 && !it.equals("bin", true) }?.let { return it.lowercase() }
        return when (att.type) {
            AttachmentType.IMAGE -> "jpg"
            AttachmentType.VIDEO -> "mp4"
            AttachmentType.AUDIO -> "m4a"
            AttachmentType.VOICE -> "ogg"
            AttachmentType.DOCUMENT -> "bin"
        }
    }

    fun download(context: Context, att: Attachment) {
        val app = context.applicationContext
        val id = att.messageId
        if (att.url.startsWith("content://") || att.url.startsWith("file://")) return
        if (jobs[id]?.isActive == true) return
        if (MediaIndex.get(id) != null) return

        setState(id, MediaDlState.Downloading(0f, 0L, att.fileSize ?: 0L))
        jobs[id] = scope.launch {
            var tmp: File? = null
            var conn: HttpURLConnection? = null
            try {
                MediaStorage.ensureFolders(app)
                val dir = MediaStorage.dirFor(app, kindFor(att.type))
                dir.mkdirs()

                // يجدّد الجلسة تلقائيًا مرة واحدة عند 401 (تطبيق بقي في الخلفية فانتهى التوكن)
                val c = MediaAuth.openAuthorized(att.url)
                conn = c
                val code = c.responseCode
                // 404 أو 410: الملف حُذف من الخادم (انتهت مدته)
                if (code == 404 || code == 410) throw IOException("expired")
                if (code == 401 || code == 403) throw IOException("forbidden")
                if (code !in 200..299) throw IOException("HTTP " + code)

                val headerName = nameFromDisposition(c.getHeaderField("Content-Disposition"))
                val headerType = c.getHeaderField("Content-Type")
                val target = File(dir, MediaStorage.fileNameFor(id, extFor(att, headerName, headerType)))
                val tmpFile = File(dir, target.name + ".tmp")
                tmp = tmpFile
                // الطول المعلن من الخادم فقط هو المرجع للتحقق (حجم الرسالة قد يكون تقريبيًا)
                val serverLen = c.contentLengthLong.takeIf { it > 0 } ?: -1L
                val total = if (serverLen > 0) serverLen else (att.fileSize ?: 0L)

                var done = 0L
                c.inputStream.use { input ->
                    tmpFile.outputStream().use { out ->
                        val buf = ByteArray(64 * 1024)
                        var lastEmit = 0L
                        while (true) {
                            val read = input.read(buf)
                            if (read < 0) break
                            ensureActive()
                            out.write(buf, 0, read)
                            done += read
                            val now = System.currentTimeMillis()
                            if (now - lastEmit > 100) {
                                lastEmit = now
                                setState(id, MediaDlState.Downloading(if (total > 0) (done.toFloat() / total).coerceAtMost(1f) else 0f, done, total))
                            }
                        }
                    }
                }
                // تنزيل مقطوع أو فارغ لا يُعدّ سليمًا أبدًا
                if (done <= 0L || (serverLen > 0 && done != serverLen)) {
                    throw IOException("incomplete")
                }
                if (!tmpFile.renameTo(target)) {
                    tmpFile.copyTo(target, overwrite = true)
                    tmpFile.delete()
                }
                MediaStorage.scanFile(app, target)
                MediaIndex.put(id, target.absolutePath)
                clearState(id)
            } catch (e: CancellationException) {
                tmp?.delete()
                clearState(id)
                throw e
            } catch (e: Exception) {
                Log.e("MediaDownload", "download failed for $id", e)
                tmp?.delete()
                setState(id, MediaDlState.Failed(e.message ?: "error"))
            } finally {
                try { conn?.disconnect() } catch (_: Exception) {}
                jobs.remove(id)
            }
        }
    }

    fun cancel(messageId: String) {
        jobs[messageId]?.cancel()
        clearState(messageId)
    }

    fun isBusy(messageId: String): Boolean = _states.value[messageId] is MediaDlState.Downloading
}
