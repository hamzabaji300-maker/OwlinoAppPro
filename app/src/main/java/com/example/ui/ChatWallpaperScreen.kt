package com.example.ui

import com.example.ui.SettingsColors

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.*

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.example.ui.i18n.LocalTranslation
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp










@Composable
fun ChatWallpaperScreen(onBack: () -> Unit) {
                val accentColors = AppAccents
    val themes = AppThemes
    val chatBackgrounds = ChatBackgrounds
    val config = LocalSettingsTheme.current
    val updateConfig = LocalSettingsThemeUpdater.current
    val selectedAccent = config.accent
    val selectedTheme = config.theme
    val selectedChatBg = config.chatBackground
    
    // update colors
     // Default to "Dark"

    // --- Dynamic Preview Colors based on selectedTheme, selectedAccent, and selectedChatBg ---
    val previewBg = selectedChatBg
    val previewIncomingBg = when {
        selectedTheme.isDark && selectedTheme.name == "Midnight" -> Color(0xFF1E293B)
        selectedTheme.isDark && selectedTheme.name == "AMOLED" -> Color(0xFF18181B)
        selectedTheme.isDark -> Color(0xFF262626)
        else -> SettingsColors.textPrimary
    }
    val previewIncomingText = if (selectedTheme.isDark) SettingsColors.textPrimary else Color.Black
    val previewOutgoingBg = if (selectedTheme.isDark) selectedAccent.copy(alpha = 0.2f) else selectedAccent
    val previewOutgoingText = if (selectedTheme.isDark) selectedAccent else SettingsColors.textPrimary
    val previewCardBg = if (selectedTheme.isDark) previewIncomingBg else SettingsColors.textPrimary
    val previewCardSurface = if (selectedTheme.isDark) Color(0xFF1E1E1E) else Color(0xFFF3F4F6)
    val previewDivider = if (selectedTheme.isDark) SettingsColors.divider else Color(0xFFE5E7EB)
    // -------------------------------------------------------------------------

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(SettingsColors.background)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Lucide.ArrowLeft,
                contentDescription = "Back",
                tint = SettingsColors.textPrimary,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBack
                    )
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = "Color Theme",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = SettingsColors.textPrimary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. Theme Preview Card (Interactive)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(SettingsColors.cardRadius))
                    .background(SettingsColors.surface)
            ) {
                // Header (Stays dark like the Settings UI)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(selectedAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Lucide.MessageCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Theme Preview",
                        color = SettingsColors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Lucide.Search, contentDescription = null, tint = selectedAccent, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(14.dp))
                    Icon(Lucide.Bell, contentDescription = null, tint = selectedAccent, modifier = Modifier.size(22.dp))
                }
                
                HorizontalDivider(color = SettingsColors.divider, thickness = 1.dp)
                
                // Chat Area (Dynamically changes)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(previewBg)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Left Message
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                            .background(previewIncomingBg)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text("This shows how the platform theme looks.", color = previewIncomingText, fontSize = 12.sp, lineHeight = 16.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Right Message
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp))
                            .background(previewOutgoingBg)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .align(Alignment.End)
                    ) {
                        Text("Nice! It matches the buttons and cards.", color = previewOutgoingText, fontSize = 12.sp, lineHeight = 16.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Inner Interactive Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(previewCardBg)
                            .border(1.dp, previewDivider, RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(previewCardSurface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Lucide.Heart, contentDescription = null, tint = selectedAccent, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Box(modifier = Modifier.size(width = 120.dp, height = 8.dp).clip(CircleShape).background(if (selectedTheme.isDark) Color(0xFFD4D4D4) else Color(0xFF9CA3AF)))
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(modifier = Modifier.size(width = 180.dp, height = 8.dp).clip(CircleShape).background(if (selectedTheme.isDark) Color(0xFF737373) else Color(0xFFD1D5DB)))
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(SettingsColors.cardRadius))
                                .background(selectedAccent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Primary Action", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Accent Color Section
            Text(
                text = "Accent Color",
                fontSize = 12.sp, lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = SettingsColors.textSecondary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
            
            // Accent Color Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(SettingsColors.cardRadius))
                    .background(SettingsColors.surface)
                    .padding(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    accentColors.chunked(6).forEach { rowColors ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            if (rowColors.size == 3) {
                                Spacer(modifier = Modifier.weight(1.5f))
                                rowColors.forEach { color ->
                                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                        AccentColorItem(color, selectedAccent == color) { updateConfig(config.copy(accent = color)) }
                                    }
                                }
                                Spacer(modifier = Modifier.weight(1.5f))
                            } else {
                                rowColors.forEach { color ->
                                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                        AccentColorItem(color, selectedAccent == color) { updateConfig(config.copy(accent = color)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Color Theme Section
            Text(
                text = "Color Theme",
                fontSize = 12.sp, lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = SettingsColors.textSecondary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            // Color Theme Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(SettingsColors.cardRadius))
                    .background(SettingsColors.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    themes.chunked(3).forEach { rowThemes ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            rowThemes.forEach { theme ->
                                Box(modifier = Modifier.weight(1f).padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
                                    ThemeOptionItem(
                                        theme = theme,
                                        isSelected = selectedTheme.name == theme.name,
                                        onClick = { updateConfig(config.copy(theme = theme)) }
                                    )
                                }
                            }
                            if (rowThemes.size < 3) {
                                repeat(3 - rowThemes.size) {
                                    Spacer(modifier = Modifier.weight(1f).padding(horizontal = 4.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. Chat Background Section
            Text(
                text = "Chat Background",
                fontSize = 12.sp, lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = SettingsColors.textSecondary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            // Chat Background Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(SettingsColors.cardRadius))
                    .background(SettingsColors.surface)
                    .padding(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    chatBackgrounds.chunked(6).forEach { rowColors ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            if (rowColors.size == 3) {
                                Spacer(modifier = Modifier.weight(1.5f))
                                rowColors.forEach { color ->
                                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                        AccentColorItem(color, selectedChatBg == color) { updateConfig(config.copy(chatBackground = color)) }
                                    }
                                }
                                Spacer(modifier = Modifier.weight(1.5f))
                            } else {
                                rowColors.forEach { color ->
                                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                        AccentColorItem(color, selectedChatBg == color) { updateConfig(config.copy(chatBackground = color)) }
                                    }
                                }
                                if (rowColors.size < 6 && rowColors.size != 3) {
                                    repeat(6 - rowColors.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun AccentColorItem(color: Color, isSelected: Boolean, onClick: () -> Unit) {
        
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (isSelected) color.copy(alpha = 0.2f) else Color.Transparent)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) color else Color.Transparent,
                shape = CircleShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(if (isSelected) 24.dp else 30.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(Lucide.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun ThemeOptionItem(theme: ThemeData, isSelected: Boolean, onClick: () -> Unit) {
        
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(SettingsColors.cardRadius))
                .background(Color(0xFF262626)) // Fallback border color
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) Color(0xFF3B82F6) else Color.Transparent,
                    shape = RoundedCornerShape(SettingsColors.cardRadius)
                )
        ) {
            Canvas(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(SettingsColors.cardRadius))) {
                drawRect(color = theme.thumbBg)
                // Draw the curved bottom section
                drawOval(
                    color = theme.thumbBubble,
                    topLeft = Offset(-size.width * 0.15f, size.height * 0.45f),
                    size = Size(size.width * 1.3f, size.height * 1.2f)
                )
            }
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(SettingsColors.textPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3B82F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Lucide.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = theme.name,
            fontSize = 12.sp,
            color = SettingsColors.textSecondary,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}
