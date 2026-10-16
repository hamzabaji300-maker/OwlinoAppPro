package com.example.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * ضغط الفيديو على هاتف المرسل قبل الرفع (فلا يكلّفك معالجة على الخادم).
 * الدقة ومعدل البتات يحددهما اختيار المستخدم (عادي 720p / عالٍ 1080p) من VideoQualityPrefs.
 * إن كان الفيديو أصلًا أصغر من ذلك فلا يُعاد ضغطه.
 */
object VideoCompressor {

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
        val q = VideoQualityPrefs.current
        val shortSide = minOf(info.width, info.height)
        if (shortSide > q.shortSide) return true
        return info.bitrate > (q.bitrate * 13 / 10) // أكثر من 130% من المعدل المستهدف
    }

    /**
     * يضغط الفيديو إلى ملف مؤقت ويرجعه. يرمي استثناء إن فشل الضغط (فيعتمد المستدعي الأصل).
     * onProgress تُستدعى بنسبة من 0 إلى 1.
     */
    suspend fun compress(context: Context, input: Uri, info: Info?, onProgress: ((Float) -> Unit)? = null): File {
        val q = VideoQualityPrefs.current
        val out = File(context.cacheDir, "vid_${System.currentTimeMillis()}.mp4")
        val app = context.applicationContext
        try {
            withContext(Dispatchers.Main) {
                var transformerRef: Transformer? = null
                coroutineScope {
                    // نسأل المكتبة عن نسبة التقدم كل 300 ملّي ثانية
                    val poller = launch {
                        val holder = ProgressHolder()
                        while (true) {
                            delay(300)
                            val t = transformerRef ?: continue
                            if (t.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) {
                                onProgress?.invoke(holder.progress / 100f)
                            }
                        }
                    }
                    try {
                        suspendCancellableCoroutine<Unit> { cont ->
                            val encoderFactory = DefaultEncoderFactory.Builder(app)
                                .setRequestedVideoEncoderSettings(
                                    VideoEncoderSettings.Builder().setBitrate(q.bitrate).build()
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
                            transformerRef = transformer

                            // نصغّر الضلع الأقصر إلى الدقة المطلوبة دون تكبير
                            val effects = ArrayList<Effect>()
                            if (info != null && minOf(info.width, info.height) > q.shortSide) {
                                val w: Int
                                val h: Int
                                if (info.width < info.height) {
                                    w = q.shortSide
                                    h = (info.height.toLong() * q.shortSide / info.width).toInt() and 1.inv()
                                } else {
                                    h = q.shortSide
                                    w = (info.width.toLong() * q.shortSide / info.height).toInt() and 1.inv()
                                }
                                effects.add(Presentation.createForWidthAndHeight(w, h, Presentation.LAYOUT_SCALE_TO_FIT))
                            }
                            val edited = EditedMediaItem.Builder(MediaItem.fromUri(input))
                                .setEffects(Effects(emptyList(), effects))
                                .build()

                            cont.invokeOnCancellation {
                                Handler(Looper.getMainLooper()).post { try { transformer.cancel() } catch (e: Exception) {} }
                            }
                            transformer.start(edited, out.absolutePath)
                        }
                    } finally {
                        poller.cancel()
                    }
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
