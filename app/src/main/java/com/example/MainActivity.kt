package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.animation.togetherWith
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ChatWallpaper
import com.example.ui.LabChatState
import com.example.ui.LabKind
import com.example.ui.LabMessagesScreen
import com.example.ui.LocalSettingsTheme
import com.example.ui.i18n.LocalTranslation
import com.example.ui.i18n.TranslationManager
import com.example.ui.theme.MyApplicationTheme

private data class HomeButton(val kind: LabKind, val label: String, val icon: ImageVector, val color: Color)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemeManager.init(applicationContext)
        setContent {
            val ctx = LocalContext.current
            val isDark by ThemeManager.isDarkMode.collectAsState()
            val accent by ThemeManager.accentColor.collectAsState()
            val themeConfig = remember(isDark, accent) { ThemeManager.loadSettingsTheme(ctx) }
            val translation by TranslationManager.currentTranslation.collectAsState()

            val buttons = remember {
                listOf(
                    HomeButton(LabKind.CHANNEL, "القنوات", Icons.Outlined.Campaign, Color(0xFF8774E1)),
                    HomeButton(LabKind.BOT, "البوتات", Icons.Outlined.SmartToy, Color(0xFF10B981)),
                    HomeButton(LabKind.GROUP, "المجموعات", Icons.Outlined.Groups, Color(0xFFF59E0B)),
                    HomeButton(LabKind.PRIVATE, "الدردشة الفردية", Icons.Outlined.Chat, Color(0xFF3B82F6))
                )
            }
            // حالة كل شاشة رسائل تبقى محفوظة عند الرجوع للرئيسية
            val states = remember { buttons.associate { it.kind to LabChatState(it.kind) } }
            var openKind by rememberSaveable { mutableStateOf<String?>(null) }
            val open = openKind?.let { runCatching { LabKind.valueOf(it) }.getOrNull() }

            BackHandler(enabled = open != null) { openKind = null }

            MyApplicationTheme(darkTheme = isDark, accentColor = accent) {
                CompositionLocalProvider(
                    LocalSettingsTheme provides themeConfig,
                    LocalTranslation provides translation
                ) {
                    androidx.compose.animation.AnimatedContent(
                        targetState = open,
                        transitionSpec = {
                            val ease = androidx.compose.animation.core.FastOutSlowInEasing
                            if (targetState != null) {
                                (androidx.compose.animation.slideInHorizontally(androidx.compose.animation.core.tween(340, easing = ease)) { it } +
                                    androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(240))) togetherWith
                                    (androidx.compose.animation.slideOutHorizontally(androidx.compose.animation.core.tween(340, easing = ease)) { -it / 3 } +
                                        androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(240)))
                            } else {
                                (androidx.compose.animation.slideInHorizontally(androidx.compose.animation.core.tween(340, easing = ease)) { -it / 3 } +
                                    androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(240))) togetherWith
                                    (androidx.compose.animation.slideOutHorizontally(androidx.compose.animation.core.tween(340, easing = ease)) { it } +
                                        androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(240)))
                            }
                        },
                        label = "screen_transition"
                    ) { shown ->
                        if (shown == null) {
                            HomeScreen(buttons, isDark = isDark, onToggleDark = { toggleDarkMode(ctx) }) { openKind = it.name }
                        } else {
                            LabMessagesScreen(
                                state = states.getValue(shown),
                                onBack = { openKind = null },
                                forwardTargets = states.values.filter { it.kind != shown }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** الشاشة الأولى: أربعة أزرار فقط، كل زر يفتح شاشة الرسائل الخاصة به. */
@androidx.compose.runtime.Composable
private fun HomeScreen(buttons: List<HomeButton>, isDark: Boolean, onToggleDark: () -> Unit, onClick: (LabKind) -> Unit) {
    val theme = LocalSettingsTheme.current.theme
    Box(Modifier.fillMaxSize()) {
        ChatWallpaper()
        // زر الوضع الليلي/النهاري لمعاينة الشاشات في الحالتين
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .safeDrawingPadding()
                .padding(16.dp)
                .size(46.dp)
                .clip(CircleShape)
                .background(theme.surfaceColor)
                .clickable { onToggleDark() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isDark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                contentDescription = "Toggle dark mode",
                tint = theme.textPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            buttons.forEachIndexed { i, b ->
                if (i > 0) Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(theme.surfaceColor)
                        .clickable { onClick(b.kind) }
                        .padding(horizontal = 20.dp, vertical = 22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(b.color),
                        contentAlignment = Alignment.Center
                    ) { Icon(b.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp)) }
                    Spacer(Modifier.size(16.dp))
                    Text(b.label, color = theme.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** تبديل بين ثيم "Classic Light" و"Classic Dark" مع خلفية المحادثة الافتراضية لكل منهما. */
private fun toggleDarkMode(context: android.content.Context) {
    val cur = ThemeManager.loadSettingsTheme(context)
    val toDark = !cur.theme.isDark
    val theme = com.example.ui.AppThemes.firstOrNull { it.isDark == toDark } ?: return
    val bg = if (toDark) Color(0xFF191919) else Color(0xFFF9FAFB)
    ThemeManager.saveSettingsTheme(context, cur.copy(theme = theme, chatBackground = bg))
}
