package com.example.ui

import android.widget.ImageView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

/**
 * يعرض إيموجي متحرك (APNG) من رابط بعيد (R2 CDN).
 * Glide يتكفل بكل شيء: تحميل مرة وحدة، تخزين في كاش القرص + الذاكرة،
 * والمرات الجاية يقرا من الكاش المحلي بلا نت.
 * هذا هو اللي يخلي حجم التطبيق صغير حتى لو عندك آلاف الإيموجيات.
 */
@Composable
fun ApngEmojiReaction(
    url: String,
    size: Dp,
    modifier: Modifier = Modifier,
    loopForever: Boolean = true,
    instanceKey: Any = url,
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        key(instanceKey) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context -> ImageView(context) },
                update = { imageView ->
                    Glide.with(imageView)
                        .load(url)
                        .diskCacheStrategy(DiskCacheStrategy.DATA) // يخزن الملف الخام في كاش القرص
                        .into(imageView)
                }
            )
        }
    }
}
