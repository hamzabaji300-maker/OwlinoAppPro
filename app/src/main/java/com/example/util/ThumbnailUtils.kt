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
}
