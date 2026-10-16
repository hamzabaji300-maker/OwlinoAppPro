package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream

/**
 * يولد نسخة صغيرة مضغوطة (JPEG) من صورة، باش تتحمل بسرعة داخل فقاعة الدردشة
 * (بدل تحميل الصورة الأصلية الكاملة في كل مرة). نفس فكرة تيليجرام: نسخة صغيرة أول،
 * والأصلية غير وقتاش المستخدم يكبّر الصورة.
 */
object ThumbnailUtils {

    private const val MAX_DIMENSION = 480 // بكسل — كافي لعرضها في فقاعة بعرض ~280dp حتى على شاشات كثيفة
    private const val JPEG_QUALITY = 55   // جودة كافية للعرض الصغير، حجم صغير جداً (عادة أقل من 40-60 كيلوبايت)

    fun generateThumbnailBytes(context: Context, uri: Uri): ByteArray? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            val (origWidth, origHeight) = bounds.outWidth to bounds.outHeight
            if (origWidth <= 0 || origHeight <= 0) return null

            var sampleSize = 1
            while ((origWidth / sampleSize) > MAX_DIMENSION * 2 || (origHeight / sampleSize) > MAX_DIMENSION * 2) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val sampledBitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            } ?: return null

            val scale = MAX_DIMENSION.toFloat() / maxOf(sampledBitmap.width, sampledBitmap.height)
            val scaledBitmap = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    sampledBitmap,
                    (sampledBitmap.width * scale).toInt().coerceAtLeast(1),
                    (sampledBitmap.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else sampledBitmap

            // نحترم اتجاه EXIF حتى لا تظهر المصغّرة جانبية
            val finalBitmap = MediaUploader.applyOrientation(
                scaledBitmap,
                MediaUploader.readOrientation(context, uri)
            )

            val outputStream = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
            if (finalBitmap !== sampledBitmap) sampledBitmap.recycle()
            finalBitmap.recycle()
            outputStream.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * مصغّرة JPEG من إطار داخل الفيديو (لفقاعة الفيديو عند المرسل والمستقبل).
     * حد الـ Worker لنوع thumb هو 1 ميغابايت، لذلك نرجع null إن تجاوزت الناتج 900 كيلوبايت.
     */
    fun generateVideoThumbnailBytes(context: Context, uri: Uri, maxEdge: Int = 480): ByteArray? {
        val r = android.media.MediaMetadataRetriever()
        return try {
            r.setDataSource(context, uri)
            val frame = r.getFrameAtTime(500_000L, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: r.getFrameAtTime(0) ?: return null
            // getFrameAtTime يرجع الإطار بعد تطبيق تدوير الفيديو على أغلب الأجهزة (API 28+ دائمًا)،
            // لكن بعض الأجهزة القديمة ترجعه خام؛ نصلحه فقط إن كان اتجاه الفيديو عموديًا والإطار أفقيًا.
            val rotation = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                ?.toIntOrNull() ?: 0
            val rawW = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val rawH = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val needsFix = android.os.Build.VERSION.SDK_INT < 28 &&
                (rotation == 90 || rotation == 270) &&
                rawW > 0 && rawH > 0 && frame.width == rawW && frame.height == rawH && rawW > rawH
            val oriented = if (needsFix) {
                val m = android.graphics.Matrix().apply { postRotate(rotation.toFloat()) }
                android.graphics.Bitmap.createBitmap(frame, 0, 0, frame.width, frame.height, m, true)
            } else frame

            val scale = maxEdge.toFloat() / maxOf(oriented.width, oriented.height)
            val bmp = if (scale < 1f) android.graphics.Bitmap.createScaledBitmap(
                oriented,
                (oriented.width * scale).toInt().coerceAtLeast(1),
                (oriented.height * scale).toInt().coerceAtLeast(1), true
            ) else oriented
            val out = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 70, out)
            out.toByteArray().takeIf { it.size in 1..(900 * 1024) }
        } catch (e: Exception) {
            null
        } finally {
            try { r.release() } catch (_: Exception) {}
        }
    }
}
