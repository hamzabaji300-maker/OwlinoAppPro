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

    private const val MAX_IMAGE_EDGE = 1600
    private const val SKIP_COMPRESS_BELOW = 300 * 1024L
    private const val JPEG_QUALITY = 80

    /** يجهّز الملف ويرفعه ويرجع رابطه الموقّع. يرمي UploadLimitException عند تجاوز الحد. */
    suspend fun prepareAndUpload(
        context: Context,
        supabase: SupabaseClient,
        bucketName: String,
        path: String,
        uri: Uri,
        type: AttachmentType,
        upsert: Boolean = false
    ): String = withContext(Dispatchers.IO) {
        val data = prepare(context, uri, type)
        val bucket = supabase.storage[bucketName]
        if (upsert) {
            bucket.upload(path, data) { this.upsert = true }
        } else {
            bucket.upload(path, data)
        }
        bucket.createSignedUrl(path, 3650.days)
    }

    /** يفحص الحد ثم يرجع البايتات الجاهزة للرفع (الصور مضغوطة). */
    suspend fun prepare(context: Context, uri: Uri, type: AttachmentType): ByteArray =
        withContext(Dispatchers.IO) {
            val size = getFileSize(context, uri)
            UploadLimits.check(type, size)?.let { throw UploadLimitException(it) }

            try {
                if (type == AttachmentType.IMAGE) {
                    compressImage(context, uri, size)
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
