package com.example.ui

import android.content.Context
import coil.Coil
import coil.request.ImageRequest

/**
 * تحميل مسبق للصور الشخصية إلى ذاكرة Coil (الذاكرة + القرص) بنفس المفتاح والمقاس المستعملين في قائمة المحادثات،
 * فتظهر الصور فورًا عند فتح التطبيق بدل أن تبقى فارغة ثم تتأخر.
 */
object AvatarPrefetch {
    private const val AVATAR_DP = 56

    fun prefetch(context: Context, urls: List<String?>) {
        val app = context.applicationContext
        val px = (AVATAR_DP * app.resources.displayMetrics.density).toInt().coerceAtLeast(1)
        val loader = Coil.imageLoader(app)
        urls.asSequence()
            .filterNotNull()
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { url ->
                loader.enqueue(
                    ImageRequest.Builder(app)
                        .data(url)
                        .size(px)
                        .memoryCacheKey(url)
                        .diskCacheKey(url)
                        .build()
                )
            }
    }
}
