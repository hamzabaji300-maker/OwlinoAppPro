package com.example

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder

/** يفعّل عرض الـ GIF المتحرك في كل التطبيق (فقاعات الرسائل ولوحة GIF). */
class LabApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        // تحميل مسبق (في الخلفية) لملفات إيموجي التفاعلات حتى تظهر فورًا بلا تأخير عند فتح القائمة
        Thread {
            val emojis = listOf(
                "\uD83D\uDC8E", "\uD83D\uDCB8", "\uD83D\uDCC9", "\uD83D\uDCC8", "\uD83E\uDE99", "\uD83E\uDE8E",
                "\uD83D\uDE02", "\uD83D\uDE0D", "\uD83D\uDE22", "\uD83D\uDE21", "\uD83D\uDC4D", "\uD83D\uDC4F",
                "\uD83D\uDE4F", "\uD83D\uDD25", "\uD83D\uDCAF", "\uD83C\uDF89", "\uD83D\uDE31", "\uD83E\uDD14",
                "\uD83D\uDC40", "\u2764\uFE0F", "\uD83D\uDC94", "\uD83D\uDE34", "\uD83E\uDD1D", "\uD83D\uDE4C"
            )
            for (e in emojis) {
                val url = com.example.emoji.NotoEmojiMap.remoteUrlFor(e) ?: continue
                try { com.airbnb.lottie.LottieCompositionFactory.fromUrlSync(this, url) } catch (_: Throwable) { }
            }
        }.start()
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory()) else add(GifDecoder.Factory())
            }
            .build()
}
