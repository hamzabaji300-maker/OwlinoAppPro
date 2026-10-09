package com.example.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieDrawable
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// روابط رجعت 404 (ما عندهاش ملف متحرك عند Google) — ما نعاودوش نطلبوها في نفس الجلسة
private val failedEmojiUrls: MutableSet<String> =
    java.util.Collections.synchronizedSet(HashSet<String>())

// الإيموجيات الثابتة: نرسمو الإطار الأول مرة وحدة كـ Bitmap ونخزّنوه،
// وبعدها التمرير يرسم صورة عادية فقط (خفيف بزاف مقارنة بـ Lottie).
private val staticEmojiCache = android.util.LruCache<String, ImageBitmap>(400)

private class EmojiUrlInfo(val candidates: List<String>, val fallbackText: String)

// Google تسمّي بعض الملفات بـ _fe0f وبعضها بلا — نجرّبو الصيغتين
private fun buildEmojiUrlInfo(url: String): EmojiUrlInfo {
    val base = url.removeSuffix("/lottie.json")
    val slash = base.lastIndexOf('/')
    if (slash < 0) return EmojiUrlInfo(listOf(url), "")
    val root = base.substring(0, slash)
    val code = base.substring(slash + 1)
    val codes = mutableListOf(code)
    if (code.contains("_fe0f")) {
        codes.add(code.replace("_fe0f", ""))
    } else if (!code.contains("_")) {
        codes.add(code + "_fe0f")
    }
    val fallback = try {
        code.split("_").joinToString("") { String(Character.toChars(it.toInt(16))) }
    } catch (e: Exception) {
        ""
    }
    return EmojiUrlInfo(codes.distinct().map { "$root/$it/lottie.json" }, fallback)
}

private fun nextUsableIndex(candidates: List<String>, from: Int): Int {
    var i = from
    while (i < candidates.size) {
        if (candidates[i] !in failedEmojiUrls) return i
        i++
    }
    return candidates.size
}

private fun renderFirstFrame(composition: LottieComposition, px: Int): ImageBitmap {
    val drawable = LottieDrawable()
    drawable.setComposition(composition)
    drawable.setProgress(0f)
    drawable.setBounds(0, 0, px, px)
    val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    drawable.draw(Canvas(bmp))
    return bmp.asImageBitmap()
}

@Composable
private fun StaticNotoEmoji(url: String, size: Dp, onFailed: (Throwable?) -> Unit) {
    val px = with(LocalDensity.current) { size.roundToPx() }.coerceAtLeast(1)
    val key = "$url@$px"
    var bitmap by remember(key) { mutableStateOf(staticEmojiCache.get(key)) }

    if (bitmap == null) {
        val result = rememberLottieComposition(LottieCompositionSpec.Url(url))
        val composition by result

        LaunchedEffect(result.isFailure, key) {
            if (result.isFailure) onFailed(result.error)
        }
        LaunchedEffect(composition, key) {
            val c = composition
            if (c != null) {
                val bmp = withContext(Dispatchers.Default) { renderFirstFrame(c, px) }
                staticEmojiCache.put(key, bmp)
                bitmap = bmp
            }
        }
    }

    val shown = bitmap
    if (shown != null) {
        Image(bitmap = shown, contentDescription = null, modifier = Modifier.size(size))
    }
}

/**
 * يعرض إيموجي Google Noto من رابط بعيد (Lottie JSON).
 * animate = true  -> متحرك (Lottie).
 * animate = false -> نفس الملف لكن يظهر ثابت كصورة مخزّنة (سلس جداً مع التمرير).
 * إذا الإيموجي ما عندوش ملف (أو ما فيه نت) يرجع لعرضه كنص عادي بدل ما يختفي.
 */
/**
 * يسجّل الإيموجيات التي تحرّكت تلقائيًا في هذه الجلسة، حتى لا تتحرك مرتين
 * ولا تتحرك الرسائل القديمة عند التمرير (الشاشة تبقى خفيفة).
 */
object EmojiPlayRegistry {
    private val played: MutableSet<String> = java.util.Collections.synchronizedSet(HashSet<String>())

    /** true فقط للرسالة الحديثة جدًا (أُرسلت/وصلت الآن) ولم تتحرك من قبل. */
    fun shouldAutoPlay(timestamp: Long, text: String): Boolean {
        if (System.currentTimeMillis() - timestamp > 15_000L) return false
        return played.add("$timestamp|$text")
    }
}

@Composable
fun LottieEmojiReaction(
    url: String,
    size: Dp,
    modifier: Modifier = Modifier,
    loopForever: Boolean = true,
    animate: Boolean = true,
    // وضع الرسائل: ثابت دائمًا، يتحرك مرة واحدة عند الإرسال (autoPlayOnce)، وعند الضغط عليه لثوانٍ ثم يتوقف
    tapToPlay: Boolean = false,
    autoPlayOnce: Boolean = false,
) {
    val info = remember(url) { buildEmojiUrlInfo(url) }
    var index by remember(url) { mutableIntStateOf(nextUsableIndex(info.candidates, 0)) }
    var playing by remember(url) { mutableStateOf(tapToPlay && autoPlayOnce) }
    var tapped by remember(url) { mutableStateOf(false) }
    val showAnimation = if (tapToPlay) playing else animate

    Box(
        modifier = modifier
            .size(size)
            .let {
                if (tapToPlay) {
                    it.clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {
                        if (!playing) {
                            tapped = true
                            playing = true
                        }
                    }
                } else it
            },
        contentAlignment = Alignment.Center
    ) {
        if (index >= info.candidates.size) {
            if (info.fallbackText.isNotEmpty()) {
                Text(text = info.fallbackText, fontSize = (size.value * 0.7f).sp)
            }
        } else {
            val currentUrl = info.candidates[index]
            if (!showAnimation) {
                StaticNotoEmoji(currentUrl, size) { err ->
                    if (err?.message?.contains("404") == true) {
                        failedEmojiUrls.add(currentUrl)
                    }
                    index = nextUsableIndex(info.candidates, index + 1)
                }
            } else {
                val result = rememberLottieComposition(LottieCompositionSpec.Url(currentUrl))
                val composition by result
                // وضع الرسائل: دورة واحدة عند الإرسال، ودورتان عند الضغط، ثم يرجع ثابتًا
                val iterationsCount = when {
                    !tapToPlay -> if (loopForever) LottieConstants.IterateForever else 1
                    tapped -> 2
                    else -> 1
                }
                val progress by animateLottieCompositionAsState(
                    composition = composition,
                    iterations = iterationsCount,
                    isPlaying = true,
                    speed = 1f,
                )
                if (tapToPlay) {
                    LaunchedEffect(composition, tapped) {
                        val c = composition
                        if (c != null) {
                            val total = (c.duration * iterationsCount).toLong().coerceIn(1200L, 6000L)
                            kotlinx.coroutines.delay(total + 150L)
                            playing = false
                        }
                    }
                }

                LaunchedEffect(currentUrl, result.isFailure) {
                    if (result.isFailure) {
                        if (result.error?.message?.contains("404") == true) {
                            failedEmojiUrls.add(currentUrl)
                        }
                        index = nextUsableIndex(info.candidates, index + 1)
                    }
                }

                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.size(size),
                )
            }
        }
    }
}
