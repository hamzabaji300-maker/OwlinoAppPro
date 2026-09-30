package com.example.ui

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import com.example.ThemeManager

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class ThemeData(
    val name: String,
    val thumbBg: Color,
    val thumbBubble: Color,
    val bgColor: Color,
    val surfaceColor: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val dividerColor: Color,
    val isDark: Boolean
)

val AppThemes = listOf(
    ThemeData("Classic Light", Color(0xFFFFFFFF), Color(0xFFF3F4F6), Color(0xFFF9FAFB), Color(0xFFFFFFFF), Color(0xFF000000), Color(0xFF6B7280), Color(0xFFE5E7EB), false),
    ThemeData("Ocean Blue", Color(0xFFE0F2FE), Color(0xFFBAE6FD), Color(0xFFF0F9FF), Color(0xFFE0F2FE), Color(0xFF000000), Color(0xFF475569), Color(0xFFBAE6FD), false),
    ThemeData("Aqua Cyan", Color(0xFFCFFAFE), Color(0xFFA5F3FC), Color(0xFFECFEFF), Color(0xFFCFFAFE), Color(0xFF000000), Color(0xFF475569), Color(0xFFA5F3FC), false),
    ThemeData("Ice Blue", Color(0xFFF0F9FF), Color(0xFFE0F2FE), Color(0xFFF0F9FF), Color(0xFFE0F2FE), Color(0xFF000000), Color(0xFF475569), Color(0xFFBAE6FD), false),
    ThemeData("Soft Teal", Color(0xFFCCFBF1), Color(0xFF99F6E4), Color(0xFFF0FDFA), Color(0xFFCCFBF1), Color(0xFF000000), Color(0xFF475569), Color(0xFF99F6E4), false),
    ThemeData("Sage Green", Color(0xFFECFCCB), Color(0xFFD9F99D), Color(0xFFF7FEE7), Color(0xFFECFCCB), Color(0xFF000000), Color(0xFF475569), Color(0xFFD9F99D), false),
    ThemeData("Forest Green Light", Color(0xFFDCFCE7), Color(0xFFBBF7D0), Color(0xFFF0FDF4), Color(0xFFDCFCE7), Color(0xFF000000), Color(0xFF475569), Color(0xFFBBF7D0), false),
    ThemeData("Peach", Color(0xFFFFEDD5), Color(0xFFFED7AA), Color(0xFFFFF7ED), Color(0xFFFFEDD5), Color(0xFF000000), Color(0xFF475569), Color(0xFFFED7AA), false),
    ThemeData("Soft Orange", Color(0xFFFEF3C7), Color(0xFFFDE68A), Color(0xFFFFFBEB), Color(0xFFFEF3C7), Color(0xFF000000), Color(0xFF475569), Color(0xFFFDE68A), false),
    ThemeData("Champagne", Color(0xFFFAFAF9), Color(0xFFF5F5F4), Color(0xFFFAFAF9), Color(0xFFF5F5F4), Color(0xFF000000), Color(0xFF475569), Color(0xFFE7E5E4), false),
    ThemeData("Sand Beige", Color(0xFFFEF08A), Color(0xFFFDE047), Color(0xFFFEFCE8), Color(0xFFFEF08A), Color(0xFF000000), Color(0xFF475569), Color(0xFFFDE047), false),
    ThemeData("Soft Gray Blue", Color(0xFFF1F5F9), Color(0xFFE2E8F0), Color(0xFFF8FAFC), Color(0xFFF1F5F9), Color(0xFF000000), Color(0xFF475569), Color(0xFFE2E8F0), false),
    ThemeData("Lilac", Color(0xFFF3E8FF), Color(0xFFE9D5FF), Color(0xFFFAF5FF), Color(0xFFF3E8FF), Color(0xFF000000), Color(0xFF475569), Color(0xFFE9D5FF), false),
    ThemeData("Sky Blue", Color(0xFFE0F2FE), Color(0xFFBAE6FD), Color(0xFFF0F9FF), Color(0xFFE0F2FE), Color(0xFF000000), Color(0xFF475569), Color(0xFFBAE6FD), false),
    ThemeData("Mint Green", Color(0xFFD1FAE5), Color(0xFFA7F3D0), Color(0xFFECFDF5), Color(0xFFD1FAE5), Color(0xFF000000), Color(0xFF475569), Color(0xFFA7F3D0), false),
    ThemeData("Dark", Color(0xFF18181B), Color(0xFF27272A), Color(0xFF000000), Color(0xFF1C1C1E), Color(0xFFFFFFFF), Color(0xFFA0A0A0), Color(0xFF333333), true),
    ThemeData("Midnight", Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFFFFFFFF), Color(0xFF94A3B8), Color(0xFF334155), true),
    ThemeData("AMOLED", Color(0xFF000000), Color(0xFF0A0A0A), Color(0xFF000000), Color(0xFF161616), Color(0xFFFFFFFF), Color(0xFFA0A0A0), Color(0xFF262626), true)
)

val AppAccents = listOf(
    Color(0xFF3B82F6), Color(0xFF8B5CF6), Color(0xFFA855F7), Color(0xFFD946EF), Color(0xFFEC4899), Color(0xFFF43F5E),
    Color(0xFFEF4444), Color(0xFFF97316), Color(0xFFF59E0B), Color(0xFF10B981), Color(0xFF14B8A6), Color(0xFF06B6D4),
    Color(0xFF0EA5E9), Color(0xFF38BDF8), Color(0xFF7DD3FC), Color(0xFFC4B5FD), Color(0xFFA78BFA), Color(0xFF818CF8),
    Color(0xFFA3E635), Color(0xFFFACC15), Color(0xFFFB923C)
)

data class SettingsThemeConfig(
    val theme: ThemeData = AppThemes[0],
    val accent: Color = AppAccents[0]
)

val LocalSettingsTheme = compositionLocalOf { SettingsThemeConfig() }
val LocalSettingsThemeUpdater = compositionLocalOf<(SettingsThemeConfig) -> Unit> { {} }


object SettingsColors {
    val DarkBackground = Color(0xFF000000)
    val DarkSurface = Color(0xFF1C1C1E)
    val DarkForeground = Color(0xFFFFFFFF)
    val DarkMutedForeground = Color(0xFFA0A0A0)
    val DarkDivider = Color(0xFFFFFFFF).copy(alpha = 0.1f)

    val LightBackground = Color(0xFFF2F2F7)
    val LightSurface = Color(0xFFFFFFFF)
    val LightForeground = Color(0xFF000000)
    val LightMutedForeground = Color(0xFF8E8E93)
    val LightDivider = Color(0xFF000000).copy(alpha = 0.05f)

    val background: Color
        @Composable get() = if (ThemeManager.isDarkMode.collectAsState().value) DarkBackground else LightBackground
    
    val surface: Color
        @Composable get() = if (ThemeManager.isDarkMode.collectAsState().value) DarkSurface else LightSurface

    val textPrimary: Color
        @Composable get() = if (ThemeManager.isDarkMode.collectAsState().value) DarkForeground else LightForeground

    val textSecondary: Color
        @Composable get() = if (ThemeManager.isDarkMode.collectAsState().value) DarkMutedForeground else LightMutedForeground

    val divider: Color
        @Composable get() = if (ThemeManager.isDarkMode.collectAsState().value) DarkDivider else LightDivider

    val blueAccent = Color(0xFF8B5CF6)
    val redAccent = Color(0xFFEF4444)
    val greenAccent = Color(0xFF10B981)
    val cardRadius = 28.dp
}
