package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = primaryPurple,
    secondary = textDark,
    tertiary = textLight,
    background = Color.White,
    surface = Color.White
)

/**
 * الثيم الرئيسي للتطبيق.
 * [accentColor] — إذا مُرِّر، يُستبدل به لون primary و secondary في ColorScheme بشكل عالمي،
 * مما يجبر كل الأزرار وحقول الإدخال والمؤشرات وعناصر Material على وراثة لون التمييز المختار.
 */
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    accentColor: Color? = null,
    content: @Composable () -> Unit
) {
    val baseColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // إذا كان هناك لون تمييز مخصص، نحقنه في primary و secondary ليعمل على كل المكونات
    val colorScheme = if (accentColor != null) {
        baseColorScheme.copy(
            primary = accentColor,
            secondary = accentColor,
            onPrimary = Color.White,
            onSecondary = Color.White
        )
    } else {
        baseColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

