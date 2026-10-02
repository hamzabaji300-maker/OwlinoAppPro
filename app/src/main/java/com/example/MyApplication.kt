package com.example

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.bumptech.glide.Glide

class MyApplication : Application(), ImageLoaderFactory {

    // باش ما نعاودوش تحميل نفس الإيموجي عدة مرات في نفس الجلسة
    @Volatile private var emojiPrefetchDone = false

    override fun onCreate() {
        super.onCreate()

        try {
            app.rive.runtime.kotlin.core.Rive.init(this)
        } catch (e: Exception) {
            // تفادياً لإيقاف التطبيق كامل إذا فشلت تهيئة Rive
        }

        // 1) محاولة فورية بمجرد فتح التطبيق (قبل حتى ما تفتح شاشة الدردشة)
        // Public Owlino media folders (like Telegram/WhatsApp) + index of media already on the phone
        Thread {
            try {
                com.example.util.MediaStorage.ensureFolders(this)
                com.example.ui.MediaIndex.scan(this)
            } catch (e: Exception) {
            }
        }.start()

        prefetchEmojiAssets()

        // 2) مراقبة الاتصال: إذا التطبيق فتح بلا نت، أو انقطع النت ثم رجع،
        //    نعاود المحاولة تلقائياً بمجرد رجوع الاتصال، بلا تدخل المستخدم
        try {
            val connectivityManager = getSystemService(ConnectivityManager::class.java)
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager?.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    prefetchEmojiAssets()
                }
            })
        } catch (e: Exception) {
            // تجاهل أي خطأ هنا، هذا تحسين إضافي مش أساسي لعمل التطبيق
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                // التطبيق الآن في الـ Foreground (المستخدم يشاهده)
                HeartbeatManager.start()
            }

            override fun onStop(owner: LifecycleOwner) {
                // التطبيق الآن في الـ Background تماماً (جميع الـ Activities مغلقة أو مخفية)
                HeartbeatManager.updateImmediateAndStop()
            }
        })
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(this.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.05)
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .crossfade(true)
            .respectCacheHeaders(false) // Ignore Supabase cache control headers that might prevent caching
            .build()
    }

    // يحمّل مسبقاً غير مجموعة صغيرة من الإيموجيات "الأكثر استعمالاً" (تجنباً لتحميل
    // مئات الميغا دفعة وحدة). الباقي يتحمل تلقائياً وبشكل ذكي أول مرة يظهر في محادثة
    // (عبر LottieEmojiReaction، تلقائياً عبر مكتبة Lottie بكاش القرص الخاص بيها).
    // كمان: التحميل المسبق يشتغل فقط على واي فاي (بلا بيانات الهاتف) حتى ما نكلفوش
    // المستخدم استهلاك باقة الانترنت بلا علمه.
    private val commonlyUsedEmojis = listOf(
        "\uD83D\uDE02", // 😂 الأكثر استعمالاً عالمياً
        "\uD83D\uDE0A", // 😊
        "\uD83D\uDE42", // 🙂
    )

    private fun prefetchEmojiAssets() {
        if (emojiPrefetchDone) return
        if (!isOnUnmeteredNetwork()) return // ننتظر واي فاي، ما نستهلكش بيانات الهاتف
        emojiPrefetchDone = true
        try {
            commonlyUsedEmojis.forEach { emoji ->
                val url = com.example.emoji.NotoEmojiMap.remoteUrlFor(emoji) ?: return@forEach
                com.airbnb.lottie.LottieCompositionFactory.fromUrl(this, url)
            }
        } catch (e: Exception) {
            emojiPrefetchDone = false
        }
    }

    private fun isOnUnmeteredNetwork(): Boolean {
        return try {
            val cm = getSystemService(ConnectivityManager::class.java) ?: return false
            val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        } catch (e: Exception) {
            false
        }
    }
}
