package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Hash
import com.composables.icons.lucide.MessageSquare
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.Users
import com.composables.icons.lucide.X
import com.composables.icons.lucide.Plus

@Composable
fun ExpandableFab(
    onNewChat: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF8B5CF6)
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 135f else 0f, 
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "fabRotate"
    )
    val isDark = com.example.ThemeManager.isDarkMode.collectAsState().value
    
    // Glass colors - simulate backdrop-blur with semi-transparent backgrounds
    val glassBg = if (isDark) Color(0xFF2C2C2E).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.85f)
    val focusedGlassBg = if (isDark) Color(0xFF2C2C2E).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.95f)
    val textColor = if (isDark) Color.White else Color(0xFF111827) // gray-900
    val iconColor = if (isDark) Color(0xFFD1D5DB) else Color(0xFF374151) // gray-300 / gray-700
    val placeholderColor = if (isDark) Color(0xFF9CA3AF) else Color(0xCC6B7280) // gray-400 / gray-500/80

    Box(modifier = modifier) {
        
        // Full screen scrim overlay when expanded
        if (isExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isExpanded = false }
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Bottom
        ) {
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 40 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { 40 })
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(bottom = 16.dp, end = 4.dp)
                ) {
                    FabMenuItem(
                        text = "New Channel",
                        icon = Lucide.Hash,
                        onClick = { isExpanded = false; onNewChat() }
                    )
                    FabMenuItem(
                        text = "New Group",
                        icon = Lucide.Users,
                        onClick = { isExpanded = false; onNewChat() }
                    )
                    FabMenuItem(
                        text = "New Contact",
                        icon = Lucide.MessageSquare,
                        onClick = { isExpanded = false; onNewChat() }
                    )
                }
            }
            
            // Bottom Row (FAB)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                // FAB
                val fabBgColor = if (isExpanded) focusedGlassBg else accentColor
                val fabIconColor = if (isExpanded) {
                    if (isDark) Color(0xFFE5E7EB) else Color(0xFF374151)
                } else {
                    Color.White
                }
                
                Surface(
                    shape = CircleShape,
                    color = fabBgColor,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .size(56.dp)
                        .clickable { isExpanded = !isExpanded }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isExpanded) Lucide.Plus else Icons.Filled.Edit,
                            contentDescription = "New Chat",
                            tint = fabIconColor,
                            modifier = Modifier
                                .size(24.dp)
                                .rotate(rotation)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FabMenuItem(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val isDark = com.example.ThemeManager.isDarkMode.collectAsState().value
    val itemBg = if (isDark) Color(0xFF2C2C2E) else Color.White
    val itemText = if (isDark) Color.White else Color(0xFF111827)
    
    Surface(
        shape = CircleShape, // Fully rounded pill shape
        color = itemBg,
        shadowElevation = 4.dp,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = itemText,
                modifier = Modifier.padding(end = 12.dp)
            )
            Icon(
                imageVector = icon, 
                contentDescription = text, 
                tint = itemText,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
