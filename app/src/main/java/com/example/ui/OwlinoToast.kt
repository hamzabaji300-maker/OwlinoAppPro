package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * نظام إشعارات سفلية موحّد لكل التطبيق — بنفس الشكل بالضبط لي كيبان فشاشة
 * الرسائل عند نسخ/تثبيت رسالة (كارد أبيض على شكل حبة دواء pill، حدود خفيفة،
 * ظل ناعم)، بحركة انبثاق زنبركية من الأسفل + نفس أيقونة الميزة نفسها لي
 * فشاشة الإعدادات (ماشي أيقونة عامة)، متحركة بنفس أسلوب PIN/SAVE فالمحادثة.
 * مثبّت مرة وحدة فجذر التطبيق (HooXApp) عبر OwlinoToastOverlay().
 */
private data class OwlinoToastData(
    val id: Long,
    val text: String,
    val icon: ImageVector?,
    val tint: Color
)

private object OwlinoToastHost {
    var current by mutableStateOf<OwlinoToastData?>(null)

    fun show(text: String, icon: ImageVector?, tint: Color) {
        current = OwlinoToastData(System.currentTimeMillis(), text, icon, tint)
    }

    fun consume(id: Long) {
        if (current?.id == id) current = null
    }
}

fun showOwlinoToast(text: String, icon: ImageVector? = Icons.Outlined.Info, tint: Color = Color(0xFF4B5563)) {
    OwlinoToastHost.show(text, icon, tint)
}

@Composable
fun OwlinoToastOverlay() {
    val toast = OwlinoToastHost.current

    LaunchedEffect(toast?.id) {
        if (toast != null) {
            delay(3000)
            OwlinoToastHost.consume(toast.id)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(bottom = 8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = toast != null,
            enter = slideInVertically(
                initialOffsetY = { 100 },
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f)
            ) + fadeIn(tween(200)) + scaleIn(initialScale = 0.9f, animationSpec = tween(200)),
            exit = slideOutVertically(
                targetOffsetY = { 50 },
                animationSpec = tween(150)
            ) + fadeOut(tween(150)) + scaleOut(targetScale = 0.95f, animationSpec = tween(150)),
            modifier = Modifier
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)
                    .shadow(
                        elevation = 8.dp,
                        shape = CircleShape,
                        ambientColor = Color(0x14000000),
                        spotColor = Color(0x14000000)
                    )
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.95f))
                    .border(1.dp, Color.Black.copy(alpha = 0.05f), CircleShape)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .widthIn(max = 360.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = toast?.text ?: "",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF111827),
                        letterSpacing = (-0.5).sp
                    )
                    if (toast?.icon != null) {
                        Spacer(modifier = Modifier.width(16.dp))
                        AnimatedToastIcon(toast.id, toast.icon, toast.tint)
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedToastIcon(toastId: Long, icon: ImageVector, tint: Color) {
    val scaleAnim = remember(toastId) { Animatable(0f) }
    val rotateAnim = remember(toastId) { Animatable(-180f) }
    val alphaAnim = remember(toastId) { Animatable(0f) }

    LaunchedEffect(toastId) {
        delay(100)
        launch { alphaAnim.animateTo(1f, tween(100)) }
        launch { scaleAnim.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 300f)) }
        launch { rotateAnim.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 300f)) }
    }

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier
            .size(18.dp)
            .graphicsLayer {
                scaleX = scaleAnim.value
                scaleY = scaleAnim.value
                rotationZ = rotateAnim.value
                alpha = alphaAnim.value
            }
    )
}
