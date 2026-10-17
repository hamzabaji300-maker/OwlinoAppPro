package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import com.example.ui.AttachmentType
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import kotlin.time.Duration.Companion.days

/**
 * المكان الوحيد المسؤول عن تجهيز الملف (ضغط + فحص الحجم) ورفعه.
 * لاحقًا عند الانتقال إلى Cloudflare R2 نغيّر دالة upload() هنا فقط.
 */
object MediaUploader {

    // جودة الصور وأطول ضلع تتبعان إعدادات «الإرسال والاستقبال»
    private val MAX_IMAGE_EDGE: Int get() = SendReceivePrefs.maxImageEdge
    private const val SKIP_COMPRESS_BELOW = 300 * 1024L
    private val JPEG_QUALITY: Int get() = SendReceivePrefs.jpegQuality

    // الحجم النهائي الذي رُفع فعلًا (بعد الضغط) لكل رسالة، لإرساله للمستقبل مع الرسالة
    private val uploadedSizes = java.util.concurrent.ConcurrentHashMap<String, Long>()

    /** يرجع حجم الملف الذي رُفع لهذه الرسالة (مرة واحدة) أو null. */
    fun takeUploadedSize(messageId: String?): Long? = messageId?.let { uploadedSizes.remove(it) }

    /** نسبة العرض/الارتفاع للفيديو كما ستظهر فعلًا (بعد تطبيق التدوير)، أو null. */
    fun videoAspectRatio(context: Context, uri: Uri): Float? {
        val r = android.media.MediaMetadataRetriever()
        return try {
            r.setDataSource(context, uri)
            var w = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            var h = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val rot = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            if (rot == 90 || rot == 270) { val t = w; w = h; h = t }
            if (w <= 0 || h <= 0) null else (w.toFloat() / h.toFloat())
        } catch (e: Exception) {
            null
        } finally {
            try { r.release() } catch (_: Exception) {}
        }
    }


    /**
     * يولّد مصغّرة (صورة أو إطار من الفيديو) ويرفعها ويرجع رابطها.
     * يرجع null عند أي فشل: فشل المصغّرة لا يجب أن يوقف إرسال الرسالة.
     * يجب استدعاؤها قبل حذف أي ملف مؤقت يخص [uri].
     */
    suspend fun generateAndUploadThumbnail(
        context: Context,
        supabase: SupabaseClient,
        bucketName: String,
        chatId: String,
        userId: String,
        scope: String,
        uri: Uri,
        type: AttachmentType,
        baseName: String
    ): String? {
        if (type != AttachmentType.IMAGE && type != AttachmentType.VIDEO) return null
        return try {
            val bytes = withContext(Dispatchers.IO) {
                if (type == AttachmentType.VIDEO) ThumbnailUtils.generateVideoThumbnailBytes(context, uri)
                else ThumbnailUtils.generateThumbnailBytes(context, uri)
            } ?: return null
            if (R2Uploader.isConfigured) {
                R2Uploader.uploadBytes(supabase, chatId, scope, "thumb", "image/jpeg", bytes)
            } else {
                val thumbPath = "$chatId/$userId/thumb_$baseName"
                val bucket = supabase.storage[bucketName]
                bucket.upload(thumbPath, bytes) { this.upsert = true }
                bucket.createSignedUrl(thumbPath, 3650.days)
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    /** نوع MIME للمستند: من النظام إن عُرف، وإلا من امتداد الاسم. */
    fun guessMime(context: Context, originalUri: Uri?, fileName: String?): String? {
        val fromResolver = try {
            originalUri?.let { context.contentResolver.getType(it) }
        } catch (e: Exception) {
            null
        }
        if (!fromResolver.isNullOrBlank() && fromResolver != "application/octet-stream") return fromResolver
        val ext = fileName?.substringAfterLast('.', "")?.lowercase()?.takeIf { it.isNotEmpty() }
        val fromExt = ext?.let { android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) }
        return fromExt ?: fromResolver?.takeIf { it.isNotBlank() }
    }


    /** يجهّز الملف ويرفعه ويرجع رابطه الموقّع. يرمي UploadLimitException عند تجاوز الحد. */
    suspend fun prepareAndUpload(
        context: Context,
        supabase: SupabaseClient,
        bucketName: String,
        path: String,
        uri: Uri,
        type: AttachmentType,
        upsert: Boolean = false,
        chatId: String? = null,
        scope: String = "direct",
        messageId: String? = null,
        fileName: String? = null,
        mimeType: String? = null
    ): String = withContext(Dispatchers.IO) {
        // الطريق الجديد: Cloudflare R2 (إن كان الـ Worker مضبوطًا ونعرف المحادثة)
        if (chatId != null && R2Uploader.isConfigured) {
            try {
                return@withContext uploadToR2(context, supabase, chatId, scope, uri, type, messageId, fileName, mimeType)
            } finally {
                UploadProgress.clear(messageId)
            }
        }
        val data = prepare(context, uri, type)
        if (messageId != null) uploadedSizes[messageId] = data.size.toLong()
        val bucket = supabase.storage[bucketName]
        if (upsert) {
            bucket.upload(path, data) { this.upsert = true }
        } else {
            bucket.upload(path, data)
        }
        try {
            bucket.createSignedUrl(path, 3650.days)
        } catch (e: Exception) {
            kotlinx.coroutines.delay(600)
            bucket.createSignedUrl(path, 3650.days)
        }
    }

    private suspend fun uploadToR2(
        context: Context,
        supabase: SupabaseClient,
        chatId: String,
        scope: String,
        uri: Uri,
        type: AttachmentType,
        messageId: String? = null,
        fileName: String? = null,
        mimeType: String? = null
    ): String {
        val size = getFileSize(context, uri)
        UploadLimits.check(type, size)?.let { throw UploadLimitException(it) }
        try {
            if (type == AttachmentType.IMAGE) {
                val bytes = compressImage(context, uri, size)
                val url = R2Uploader.uploadBytes(
                    supabase, chatId, scope, "image", sniffImageType(bytes), bytes
                ) { p -> UploadProgress.set(messageId, p) }
                if (messageId != null) uploadedSizes[messageId] = bytes.size.toLong()
                return url
            }
            if (type == AttachmentType.VIDEO) {
                // نضغط الفيديو على الهاتف أولًا (720p)، ثم نرفعه بالتدفق من الملف المضغوط
                val prepared = prepareVideo(context, uri, size, messageId)
                val base = if (prepared.compressed) 0.5f else 0f
                try {
                    return R2Uploader.uploadStream(
                        context, supabase, chatId, scope, "video",
                        if (prepared.compressed) "video/mp4" else (mimeType?.takeIf { it.startsWith("video/") } ?: contentTypeOf(context, uri, type)),
                        prepared.uri, prepared.size,
                        onSized = { real -> if (messageId != null) uploadedSizes[messageId] = real }
                    ) { p -> UploadProgress.set(messageId, base + p * (1f - base)) }
                } finally {
                    prepared.tempFile?.delete()
                }
            }
            val kind = when (type) {
                AttachmentType.AUDIO, AttachmentType.VOICE -> "voice"
                else -> "file"
            }
            val contentType = mimeType?.takeIf { it.isNotBlank() } ?: contentTypeOf(context, uri, type)
            // دائمًا بالتدفق: يُنسخ المصدر إلى ملف حقيقي ثم يُرفع بطوله الفعلي (يحل مشكلة الأحجام الخاطئة من content://)
            return R2Uploader.uploadStream(
                context, supabase, chatId, scope, kind, contentType, uri, size,
                fileName = fileName,
                onSized = { real -> if (messageId != null) uploadedSizes[messageId] = real }
            ) { p -> UploadProgress.set(messageId, p) }
        } catch (e: java.io.FileNotFoundException) {
            throw LocalMediaException("الملف لم يعد متاحًا على الهاتف")
        } catch (e: SecurityException) {
            throw LocalMediaException("لا توجد صلاحية لقراءة الملف")
        }
    }

    private const val SMALL_VIDEO_BYTES = 8L * 1024 * 1024

    private class PreparedVideo(val uri: Uri, val size: Long, val tempFile: File?, val compressed: Boolean)

    /**
     * يضغط الفيديو إن كان يستحق ويرجع ما سيُرفع فعلًا.
     * إن فشل الضغط (ترميز غير مدعوم مثلًا) نرفع الأصل بشرط ألا يتجاوز 50 ميغابايت.
     */
    private suspend fun prepareVideo(context: Context, uri: Uri, originalSize: Long, messageId: String? = null): PreparedVideo {
        val info = VideoCompressor.readInfo(context, uri)
        var temp: File? = null
        // المقاطع القصيرة (حتى 8 ميغابايت) تُرفع كما هي: أسرع ولا تحتاج ضغطًا
        val small = originalSize in 1..SMALL_VIDEO_BYTES
        if (!small && VideoCompressor.needsCompression(info)) {
            temp = try {
                VideoCompressor.compress(context, uri, info) { p -> UploadProgress.set(messageId, p * 0.5f) }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Throwable) {
                null
            }
        }
        val limit = UploadLimits.maxOutputBytes(AttachmentType.VIDEO) ?: Long.MAX_VALUE
        // نعتمد المضغوط إذا كان أصغر فعلًا من الأصل
        if (temp != null && (originalSize <= 0 || temp.length() < originalSize)) {
            if (temp.length() > limit) {
                temp.delete()
                throw UploadLimitException(UploadLimits.VIDEO_TOO_LONG)
            }
            return PreparedVideo(Uri.fromFile(temp), temp.length(), temp, true)
        }
        temp?.delete()
        if (originalSize > limit) throw UploadLimitException(UploadLimits.VIDEO_TOO_LONG)
        return PreparedVideo(uri, originalSize, null, false)
    }

    private fun sniffImageType(b: ByteArray): String = when {
        b.size > 4 && b[0] == 0x89.toByte() && b[1] == 'P'.code.toByte() -> "image/png"
        b.size > 4 && b[0] == 'G'.code.toByte() && b[1] == 'I'.code.toByte() -> "image/gif"
        else -> "image/jpeg"
    }

    private fun contentTypeOf(context: Context, uri: Uri, type: AttachmentType): String {
        val fromResolver = try { context.contentResolver.getType(uri) } catch (e: Exception) { null }
        if (!fromResolver.isNullOrBlank()) return fromResolver
        return when (type) {
            AttachmentType.VIDEO -> "video/mp4"
            AttachmentType.AUDIO, AttachmentType.VOICE -> "audio/mp4"
            else -> "application/octet-stream"
        }
    }

    /** يفحص الحد ثم يرجع البايتات الجاهزة للرفع (الصور مضغوطة). */
    suspend fun prepare(context: Context, uri: Uri, type: AttachmentType): ByteArray =
        withContext(Dispatchers.IO) {
            val size = getFileSize(context, uri)
            UploadLimits.check(type, size)?.let { throw UploadLimitException(it) }

            try {
                if (type == AttachmentType.IMAGE) {
                    compressImage(context, uri, size)
                } else if (type == AttachmentType.VIDEO) {
                    val prepared = prepareVideo(context, uri, size)
                    try {
                        context.contentResolver.openInputStream(prepared.uri)?.use { it.readBytes() }
                            ?: throw LocalMediaException("تعذّر قراءة الملف")
                    } finally {
                        prepared.tempFile?.delete()
                    }
                } else {
                    // مرة واحدة فقط وبعد التأكد من الحد (أقصى 50 ميغابايت)
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw LocalMediaException("تعذّر قراءة الملف")
                }
            } catch (e: java.io.FileNotFoundException) {
                throw LocalMediaException("الملف لم يعد متاحًا على الهاتف")
            } catch (e: SecurityException) {
                throw LocalMediaException("لا توجد صلاحية لقراءة الملف")
            }
        }

    fun getFileSize(context: Context, uri: Uri): Long {
        try {
            if (uri.scheme == "file") {
                return File(uri.path ?: return 0L).length()
            }
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(OpenableColumns.SIZE)
                    if (idx >= 0 && !c.isNull(idx)) {
                        val s = c.getLong(idx)
                        if (s > 0) return s
                    }
                }
            }
            context.contentResolver.openFileDescriptor(uri, "r")?.use { return it.statSize }
        } catch (e: Exception) {
        }
        return 0L
    }

    /** اتجاه الصورة من EXIF (صورة الكاميرا العمودية غالبًا مخزّنة أفقية مع علامة تدوير). */
    fun readOrientation(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    private fun swapsAxes(orientation: Int) = orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
        orientation == ExifInterface.ORIENTATION_ROTATE_270 ||
        orientation == ExifInterface.ORIENTATION_TRANSPOSE ||
        orientation == ExifInterface.ORIENTATION_TRANSVERSE

    /** نسبة العرض/الارتفاع كما ستظهر فعلًا بعد تطبيق الاتجاه. */
    fun displayAspectRatio(context: Context, uri: Uri): Float? {
        return try {
            val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o) }
            if (o.outWidth <= 0 || o.outHeight <= 0) return null
            val swap = swapsAxes(readOrientation(context, uri))
            val w = if (swap) o.outHeight else o.outWidth
            val h = if (swap) o.outWidth else o.outHeight
            val r = w.toFloat() / h.toFloat()
            if (r.isNaN() || r.isInfinite()) null else r
        } catch (e: Exception) {
            null
        }
    }

    /** يطبّق اتجاه EXIF على الصورة ويرجع صورة جديدة (أو نفسها إن لم يلزم). */
    fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val m = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> { m.postRotate(180f); m.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSPOSE -> { m.postRotate(90f); m.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { m.postRotate(270f); m.postScale(-1f, 1f) }
            else -> return bitmap
        }
        val out = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
        if (out !== bitmap) bitmap.recycle()
        return out
    }

    private fun readAll(context: Context, uri: Uri): ByteArray =
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw LocalMediaException("تعذّر قراءة الصورة")

    private fun isGif(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use {
                val head = ByteArray(4)
                it.read(head) == 4 && String(head, Charsets.ISO_8859_1) == "GIF8"
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun compressImage(context: Context, uri: Uri, originalSize: Long): ByteArray {
        // الـ GIF المتحركة لا تُضغط (الضغط يحوّلها لصورة ثابتة)
        if (isGif(context, uri)) return readAll(context, uri)

        val orientation = readOrientation(context, uri)

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            // ليست صورة قابلة للفك (أو صيغة غير مدعومة): نرفعها كما هي
            return readAll(context, uri)
        }
        val longest = max(bounds.outWidth, bounds.outHeight)

        // صورة صغيرة أصلًا وبلا حاجة للتدوير: لا داعي لإعادة ضغطها
        if (originalSize in 1..SKIP_COMPRESS_BELOW &&
            longest <= MAX_IMAGE_EDGE &&
            orientation == ExifInterface.ORIENTATION_NORMAL
        ) {
            return readAll(context, uri)
        }

        // فك الصورة بحجم أصغر لتوفير الذاكرة (قوى العدد 2)
        var sample = 1
        while (longest / (sample * 2) >= MAX_IMAGE_EDGE) sample *= 2
        var bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return readAll(context, uri)

        val currentLongest = max(bitmap.width, bitmap.height)
        var resized = sample > 1
        if (currentLongest > MAX_IMAGE_EDGE) {
            val scale = MAX_IMAGE_EDGE.toFloat() / currentLongest
            val scaled = Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1),
                true
            )
            if (scaled !== bitmap) bitmap.recycle()
            bitmap = scaled
            resized = true
        }

        val needsRotation = orientation != ExifInterface.ORIENTATION_NORMAL &&
            orientation != ExifInterface.ORIENTATION_UNDEFINED
        bitmap = applyOrientation(bitmap, orientation)

        // الصور الشفافة (مثل PNG) نحفظها PNG حتى لا تصبح الخلفية سوداء
        val hasAlpha = bitmap.hasAlpha()
        val out = ByteArrayOutputStream()
        if (hasAlpha) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        } else {
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }
        bitmap.recycle()
        val compressed = out.toByteArray()

        // إذا لم نصغّر ولم ندوّر وكان الناتج أكبر من الأصل، نبقي الأصل
        if (!resized && !needsRotation && originalSize > 0 && compressed.size >= originalSize) {
            return readAll(context, uri)
        }
        return compressed
    }
}
