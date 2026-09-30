package com.example.ui

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.core.Fit
import app.rive.runtime.kotlin.core.Loop

@Composable
fun RiveEmojiReaction(
    artboardName: String,
    size: Dp,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource? = null,
    zoomFactor: Float = 1.5f,
    verticalOffset: Float = -14f,
    onClick: (() -> Unit)? = null
) {
    var riveViewRef by remember { mutableStateOf<RiveAnimationView?>(null) }

    val actualInteractionSource = interactionSource ?: remember { MutableInteractionSource() }

    LaunchedEffect(riveViewRef) {
        riveViewRef?.let { view ->
            kotlinx.coroutines.delay(700)
            try {
                view.play(animationName = "Reveal", loop = Loop.ONESHOT)
            } catch (e: Exception) {
            }
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = actualInteractionSource,
                indication = null,
                onClick = {
                    try {
                        riveViewRef?.play(animationName = "Reveal", loop = Loop.ONESHOT)
                    } catch (e: Exception) {
                    }
                    onClick?.invoke()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // key(artboardName): كنجبرو Compose يهدم الـview القديمة ويبني وحدة جديدة من الصفر
        // (factory من جديد) كل مرة يتبدل فيها الإيموجي - أضمن بزاف من محاولة "تحديث" نفس
        // الـview القديمة (setRiveBytes مرة تانية على نفس الكائن كان أحياناً كيحتاج ضغطتين
        // باش يتثبت بسبب حالة داخلية فمكتبة Rive نفسها ما كنتحكموش فيها).
        androidx.compose.runtime.key(artboardName) {
            AndroidView(
                modifier = Modifier.fillMaxSize().graphicsLayer(
                    scaleX = zoomFactor,
                    scaleY = zoomFactor,
                    translationY = verticalOffset
                ),
                factory = { context ->
                    val view = RiveAnimationView(context)
                    try {
                        val bytes = context.assets.open("emoji_reactions.riv").readBytes()
                        view.setRiveBytes(
                            bytes = bytes,
                            artboardName = artboardName,
                            stateMachineName = "controller",
                            autoplay = true,
                            fit = Fit.CONTAIN
                        )
                        view.play(animationName = "idle", loop = Loop.ONESHOT)
                        riveViewRef = view
                    } catch (e: Exception) {
                    }
                    view
                }
            )
        }
    }

    // شبكة أمان: مكتبة Rive أحياناً كتاخد وقت شوية باش تربط الـ state machine بالـview
    // الجديدة، وplay("idle") اللي كيتنفذ مباشرة بعد setRiveBytes ممكن ما يخدمش (بلا خطأ ولا
    // exception) إذا تنفذ قبل ما تكمل المكتبة التهيئة. هاد إعادة محاولة وحدة زايدة بعد تأخير
    // بسيط كافية باش تضمن الظهور من أول ضغطة بلا ما تحتاج تعاود من بعد.
    LaunchedEffect(riveViewRef, artboardName) {
        val view = riveViewRef ?: return@LaunchedEffect
        kotlinx.coroutines.delay(150)
        try {
            view.play(animationName = "idle", loop = Loop.ONESHOT)
        } catch (e: Exception) {
        }
    }
}
