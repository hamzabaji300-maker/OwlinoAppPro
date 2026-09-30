package com.example.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MessageCircle
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.Settings
import com.composables.icons.lucide.User

@Composable
fun CustomBottomNavigation(
    modifier: Modifier = Modifier,
    activeTab: String = "Chat",
    onTabSelected: (String) -> Unit = {},
    onSearchClick: () -> Unit = {}
) {
    val isDark = com.example.ThemeManager.isDarkMode.collectAsState().value
    
    // Background and border colors matching the React glassmorphism design (white/90 or #1c1c1d/90)
    val bgContainer = if (isDark) Color(0xFF1C1C1D).copy(alpha = 0.9f) else Color.White.copy(alpha = 0.9f)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f)
    val shadowColor = if (isDark) Color.Black.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.08f)
    
    // Active Pill background (purple-100/50 light, purple-900/30 dark)
    val pillBgColor = if (isDark) Color(0xFF9333EA).copy(alpha = 0.3f) else Color(0xFF9333EA).copy(alpha = 0.1f)
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding() // Proper Edge-to-Edge handling
            .padding(bottom = 24.dp, start = 16.dp, end = 16.dp), // bottom-6 and px-4
        horizontalArrangement = Arrangement.spacedBy(12.dp), // gap-3
        verticalAlignment = Alignment.CenterVertically
    ) {
        
        // --- Part 1: Main Navigation Bar (3 tabs) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .shadow(elevation = 16.dp, shape = CircleShape, spotColor = shadowColor, ambientColor = shadowColor)
                .background(bgContainer, CircleShape)
                .border(1.dp, borderColor, CircleShape)
                .padding(4.dp) // p-1 equivalent (4dp)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val tabWidth = maxWidth / 3
                
                val indicatorOffset by animateDpAsState(
                    targetValue = when (activeTab) {
                        "Settings" -> 0.dp
                        "Chat" -> tabWidth
                        "Search" -> tabWidth * 2
                        else -> tabWidth
                    },
                    animationSpec = spring(
                        dampingRatio = 0.75f, 
                        stiffness = 400f
                    ),
                    label = "pill_anim"
                )
                
                // Sliding Active Pill (Framer Motion 'layoutId' equivalent)
                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .width(tabWidth)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(pillBgColor)
                )
                
                // Tabs Content
                Row(modifier = Modifier.fillMaxSize()) {
                    NavItem(
                        title = "Settings",
                        icon = Lucide.Settings,
                        isActive = activeTab == "Settings",
                        isDark = isDark,
                        onClick = { onTabSelected("Settings") },
                        modifier = Modifier.weight(1f)
                    )
                    NavItem(
                        title = "Chat",
                        icon = Lucide.MessageCircle,
                        isActive = activeTab == "Chat",
                        isDark = isDark,
                        onClick = { onTabSelected("Chat") },
                        modifier = Modifier.weight(1f)
                    )
                    NavItem(
                        title = "Search",
                        icon = Lucide.Search,
                        isActive = activeTab == "Search",
                        isDark = isDark,
                        onClick = { onSearchClick() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun NavItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = Color(0xFF9333EA) // text-purple-600
    val inactiveColor = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280) // text-gray-400 / text-gray-500
    val tintColor = if (isActive) activeColor else inactiveColor

    // Emulate the React 'fill' behavior for Lucide icons (which are stroked by default in Compose)
    // In Compose, we can't easily change the fill of a vector asset dynamically without creating a custom icon or using a tint on a filled version.
    // However, since we are using Lucide icons, they are designed as stroked icons.
    // To simulate the 'fill-current' or 'fill-purple-600/20', we will use a Box with a background clip for the 20% fill effect,
    // and for 'Chat' we will switch to the Filled variant of the icon if available, or just rely on the tint.
    // Since we are using standard Lucide, we will do our best to emulate the visual weight.
    
    // Actually, a better way to simulate the glowing fill inside stroked icons is to draw a colored circle behind them.
    val fillAlpha = if (isActive && title != "Chat") 0.2f else 0f
    
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Emulate the fill-purple-600/20 for non-Chat icons
            if (isActive && title != "Chat") {
                Box(
                    modifier = Modifier
                        .size(16.dp) // Slightly smaller than the icon to fit inside the strokes
                        .background(activeColor.copy(alpha = fillAlpha), CircleShape)
                )
            }
            
            // For Chat, if we really want a solid fill, we'd need a filled icon. 
            // We will use the standard stroke icon for now but tint it strongly.
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = tintColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = tintColor,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
