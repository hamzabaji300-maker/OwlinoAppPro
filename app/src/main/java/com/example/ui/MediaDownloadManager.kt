package com.example.ui

import android.content.Context
import android.util.Log
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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
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

    private fun extFor(att: Attachment): String {
        att.fileName?.substringAfterLast('.', "")?.takeIf { it.length in 1..5 }?.let { return it.lowercase() }
        att.url.substringBefore('?').substringAfterLast('.', "").takeIf { it.length in 2..4 }?.let { return it.lowercase() }
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
            try {
                MediaStorage.ensureFolders(app)
                val dir = MediaStorage.dirFor(app, kindFor(att.type))
                dir.mkdirs()
                val target = File(dir, MediaStorage.fileNameFor(id, extFor(att)))
                val tmpFile = File(dir, target.name + ".tmp")
                tmp = tmpFile

                val conn = (URL(att.url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    instanceFollowRedirects = true
                }
                conn.connect()
                if (conn.responseCode !in 200..299) throw IOException("HTTP " + conn.responseCode)
                val total = conn.contentLengthLong.takeIf { it > 0 } ?: (att.fileSize ?: 0L)

                conn.inputStream.use { input ->
                    tmpFile.outputStream().use { out ->
                        val buf = ByteArray(64 * 1024)
                        var done = 0L
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
                                setState(id, MediaDlState.Downloading(if (total > 0) done.toFloat() / total else 0f, done, total))
                            }
                        }
                    }
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
