package com.example.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun VerifiedBadge(
    isVerified: Boolean,
    modifier: Modifier = Modifier,
    iconSize: Dp = 18.dp,
    yOffset: Dp = (-1).dp
) {
    if (!isVerified) return

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f),
        label = "VerifiedBadgeScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(stiffness = 400f),
        label = "VerifiedBadgeAlpha"
    )

    Box(
        modifier = modifier.offset(y = yOffset)
            .scale(scale)
            .alpha(alpha)
            .requiredSize(iconSize),
        contentAlignment = Alignment.Center
    ) {
        // Inner white circle sized perfectly to only cover the checkmark (preventing white bleeding at the edges)
        Box(
            modifier = Modifier
                .requiredSize(iconSize * 0.45f)
                .background(Color.White, CircleShape)
        )
        
        // Outer blue badge
        Icon(
            imageVector = Icons.Filled.Verified,
            contentDescription = "Verified",
            tint = Color(0xFF3B82F6),
            modifier = Modifier.fillMaxSize()
        )
    }
}
