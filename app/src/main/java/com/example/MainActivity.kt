package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.safeDrawing
import com.example.ui.LabChatState
import com.example.ui.LabKind
import com.example.ui.LabMessagesScreen
import com.example.ui.LocalSettingsTheme
import com.example.ui.i18n.LocalTranslation
import com.example.ui.i18n.TranslationManager
import com.example.ui.theme.MyApplicationTheme

private data class LabTab(val kind: LabKind, val label: String, val icon: ImageVector)

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

            val tabs = remember {
                listOf(
                    LabTab(LabKind.CHANNEL, "القنوات", Icons.Outlined.Campaign),
                    LabTab(LabKind.GROUP, "المجموعات", Icons.Outlined.Groups),
                    LabTab(LabKind.BOT, "البوتات", Icons.Outlined.SmartToy),
                    LabTab(LabKind.PRIVATE, "الدردشة", Icons.Outlined.Chat)
                )
            }
            // حالة كل شاشة تبقى محفوظة أثناء التنقل بين الأزرار
            val states = remember { tabs.associate { it.kind to LabChatState(it.kind) } }
            var selected by rememberSaveable { mutableIntStateOf(0) }

            MyApplicationTheme(darkTheme = isDark, accentColor = accent) {
                CompositionLocalProvider(
                    LocalSettingsTheme provides themeConfig,
                    LocalTranslation provides translation
                ) {
                    Scaffold(
                        contentWindowInsets = WindowInsets.safeDrawing.exclude(WindowInsets.ime),
                        bottomBar = {
                            NavigationBar(containerColor = themeConfig.theme.surfaceColor) {
                                tabs.forEachIndexed { i, t ->
                                    NavigationBarItem(
                                        selected = selected == i,
                                        onClick = { selected = i },
                                        icon = { Icon(t.icon, contentDescription = t.label) },
                                        label = { Text(t.label) },
                                        colors = NavigationBarItemDefaults.colors(selectedIconColor = accent, selectedTextColor = accent)
                                    )
                                }
                            }
                        }
                    ) { padding ->
                        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
                            LabMessagesScreen(state = states.getValue(tabs[selected].kind))
                        }
                    }
                }
            }
        }
    }
}
