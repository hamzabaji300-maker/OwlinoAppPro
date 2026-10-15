package com.example.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.transformer.Effects
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * ضغط الفيديو على هاتف المرسل قبل الرفع (فلا يكلّفك معالجة على الخادم).
 *
 * الإعدادات هنا هي الموازنة بين الجودة والتكلفة، وتُعدَّل من الثوابت أدناه فقط:
 *  - الدقة: 720 على الضلع الأقصر (720p للأفقي، و720x1280 للعمودي). واضحة على شاشة الهاتف.
 *  - معدل بتات الفيديو 2 ميغابت/ثانية (حوالي 15 ميغابايت للدقيقة).
 * إن كان الفيديو أصلًا أصغر من ذلك فلا يُعاد ضغطه.
 */
object VideoCompressor {

    const val TARGET_SHORT_SIDE = 720
    const val TARGET_VIDEO_BITRATE = 2_000_000          // بت/ثانية
    private const val SKIP_IF_BITRATE_BELOW = 2_600_000 // لا نعيد ضغط ما هو أصغر من هذا

    class Info(val width: Int, val height: Int, val bitrate: Int, val durationMs: Long)

    fun readInfo(context: Context, uri: Uri): Info? {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(context, uri)
            var w = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: return null
            var h = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: return null
            val rotation = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            if (rotation == 90 || rotation == 270) { val t = w; w = h; h = t } // الأبعاد كما تُعرض
            val bitrate = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0
            val duration = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            Info(w, h, bitrate, duration)
        } catch (e: Exception) {
            null
        } finally {
            try { r.release() } catch (e: Exception) {}
        }
    }

    /** هل يستحق الفيديو إعادة الضغط؟ */
    fun needsCompression(info: Info?): Boolean {
        if (info == null) return true // لا نعرف: نجرّب الضغط
        val shortSide = minOf(info.width, info.height)
        if (shortSide > TARGET_SHORT_SIDE) return true
        return info.bitrate > SKIP_IF_BITRATE_BELOW
    }

    /**
     * يضغط الفيديو إلى ملف مؤقت ويرجعه. يرمي استثناء إن فشل الضغط (فيعتمد المستدعي الأصل).
     * يجب استدعاؤه من أي سياق؛ يعمل داخليًا على الخيط الرئيسي كما تتطلب المكتبة.
     */
    suspend fun compress(context: Context, input: Uri, info: Info?): File {
        val out = File(context.cacheDir, "vid_${System.currentTimeMillis()}.mp4")
        val app = context.applicationContext
        try {
            withContext(Dispatchers.Main) {
                suspendCancellableCoroutine<Unit> { cont ->
                    val encoderFactory = DefaultEncoderFactory.Builder(app)
                        .setRequestedVideoEncoderSettings(
                            VideoEncoderSettings.Builder().setBitrate(TARGET_VIDEO_BITRATE).build()
                        )
                        .build()

                    val transformer = Transformer.Builder(app)
                        .setVideoMimeType(MimeTypes.VIDEO_H264)
                        .setAudioMimeType(MimeTypes.AUDIO_AAC)
                        .setEncoderFactory(encoderFactory)
                        .addListener(object : Transformer.Listener {
                            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                                if (cont.isActive) cont.resume(Unit)
                            }

                            override fun onError(
                                composition: Composition,
                                exportResult: ExportResult,
                                exportException: ExportException
                            ) {
                                if (cont.isActive) cont.resumeWithException(exportException)
                            }
                        })
                        .build()

                    // نصغّر الضلع الأقصر إلى 720 دون تكبير: عمودي (العرض 720) أو أفقي (الارتفاع 720)
                    val effects = ArrayList<androidx.media3.common.Effect>()
                    if (info != null && minOf(info.width, info.height) > TARGET_SHORT_SIDE) {
                        effects.add(
                            if (info.width < info.height) Presentation.createForWidthAndHeight(
                                TARGET_SHORT_SIDE,
                                (info.height.toLong() * TARGET_SHORT_SIDE / info.width).toInt() and 1.inv(),
                                Presentation.LAYOUT_SCALE_TO_FIT
                            ) else Presentation.createForWidthAndHeight(
                                (info.width.toLong() * TARGET_SHORT_SIDE / info.height).toInt() and 1.inv(),
                                TARGET_SHORT_SIDE,
                                Presentation.LAYOUT_SCALE_TO_FIT
                            )
                        )
                    }
                    val edited = EditedMediaItem.Builder(MediaItem.fromUri(input))
                        .setEffects(Effects(emptyList(), effects))
                        .build()

                    cont.invokeOnCancellation {
                        Handler(Looper.getMainLooper()).post { try { transformer.cancel() } catch (e: Exception) {} }
                    }
                    transformer.start(edited, out.absolutePath)
                }
            }
        } catch (e: Throwable) {
            out.delete()
            throw e
        }
        if (!out.exists() || out.length() == 0L) {
            out.delete()
            throw java.io.IOException("فشل ضغط الفيديو")
        }
        return out
    }
}
