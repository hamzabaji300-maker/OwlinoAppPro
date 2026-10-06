package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.zIndex

@Composable
fun MainAppScreen(onNavigateNext: () -> Unit = {}) {
    val title = "Welcome to Owlino"
    val animStates = title.map { remember { Animatable(0f) } }
    val pAlpha = remember { Animatable(0f) }
    
    val containerScale = remember { Animatable(0.95f) }
    val containerAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            containerScale.animateTo(1f, tween(500))
        }
        launch {
            containerAlpha.animateTo(1f, tween(500))
        }

        title.forEachIndexed { index, _ ->
            launch {
                delay(index * 30L)
                animStates[index].animateTo(
                    targetValue = 1f,
                    animationSpec = tween(500, easing = CubicBezierEasing(0.2f, 0.65f, 0.3f, 0.9f))
                )
            }
        }
        launch {
            delay(600)
            pAlpha.animateTo(1f, tween(500))
        }
        launch {
            delay(3500) // 3.5 seconds total wait
            onNavigateNext()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB)) // bg-gray-50
            .graphicsLayer {
                scaleX = containerScale.value
                scaleY = containerScale.value
                alpha = containerAlpha.value
            },
        contentAlignment = Alignment.Center
    ) {
        // Subtle background glow for main app
        Box(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .fillMaxHeight(0.75f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF3B82F6).copy(alpha = 0.05f), Color.Transparent)
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.zIndex(10f)
        ) {
            Row {
                title.forEachIndexed { index, char ->
                    val progress = animStates[index].value
                    
                    // Maintain spaces
                    val charText = if (char == ' ') " " else char.toString()
                    
                    Text(
                        text = charText,
                        fontSize = 30.sp, // text-3xl
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF111827), // text-gray-900
                        letterSpacing = (-0.5).sp, // tracking-tight
                        modifier = Modifier.graphicsLayer {
                            translationY = 20f * (1f - progress) // initial y: 20
                            alpha = progress // initial opacity: 0
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "You are successfully signed in.",
                fontSize = 16.sp,
                color = Color(0xFF6B7280), // text-gray-500
                modifier = Modifier.graphicsLayer { alpha = pAlpha.value }
            )
        }
    }
}
