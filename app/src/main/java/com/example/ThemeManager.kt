package com.example

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.example.ui.AppAccents
import com.example.ui.AppThemes
import com.example.ui.SettingsThemeConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object ThemeManager {
    private const val PREFS_NAME = "theme_prefs_v2"
    private const val KEY_IS_DARK = "is_dark"
    private const val KEY_THEME_NAME = "theme_name"
    private const val KEY_ACCENT_COLOR = "accent_color"
    private const val KEY_CHAT_BG_COLOR = "chat_bg_color"
    
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode

    fun init(context: Context) {
        val config = loadSettingsTheme(context)
        _isDarkMode.value = config.theme.isDark
    }

    fun loadSettingsTheme(context: Context): SettingsThemeConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val themeName = prefs.getString(KEY_THEME_NAME, AppThemes[0].name) ?: AppThemes[0].name
        val accentValue = prefs.getLong(KEY_ACCENT_COLOR, AppAccents[0].value.toLong())
        
        // Find default chat bg based on dark mode if missing
        val themeData = AppThemes.find { it.name == themeName } ?: AppThemes[0]
        val defaultChatBg = if (themeData.isDark) Color(0xFF000000).value.toLong() else Color(0xFFF9FAFB).value.toLong()
        val chatBgValue = prefs.getLong(KEY_CHAT_BG_COLOR, defaultChatBg)
        
        val accentColor = Color(accentValue.toULong())
        val chatBgColor = Color(chatBgValue.toULong())
        
        _isDarkMode.value = themeData.isDark
        
        return SettingsThemeConfig(theme = themeData, accent = accentColor, chatBackground = chatBgColor)
    }

    fun saveSettingsTheme(context: Context, config: SettingsThemeConfig) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_THEME_NAME, config.theme.name)
            .putLong(KEY_ACCENT_COLOR, config.accent.value.toLong())
            .putLong(KEY_CHAT_BG_COLOR, config.chatBackground.value.toLong())
            .putBoolean(KEY_IS_DARK, config.theme.isDark)
            .apply()
        _isDarkMode.value = config.theme.isDark
    }

    fun toggleTheme(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val newMode = !_isDarkMode.value
        prefs.edit().putBoolean(KEY_IS_DARK, newMode).apply()
        _isDarkMode.value = newMode
    }
}
