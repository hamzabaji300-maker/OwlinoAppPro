package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ThemeManager
import com.example.bot.BotManager
import com.example.bot.BotManagerStrings

/** لون النص فوق لون التمييز: أسود إذا كان اللون فاتح جداً، وإلا أبيض. */
private fun onAccent(accent: Color): Color = if (accent.luminance() > 0.6f) Color.Black else Color.White

/**
 * بطاقة START: تحل محل لوحة المفاتيح عند دخول أي بوت لأول مرة،
 * ومتلوّنة بلون التمييز (Accent) المحدد في الإعدادات.
 */
@Composable
fun BotStartBar(onClick: () -> Unit) {
    val accent by ThemeManager.accentColor.collectAsState()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 14.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(accent)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = BotManagerStrings.START,
                color = onAccent(accent),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }
}

/**
 * قائمة الأوامر (مثل تيليجرام): زر Menu + قائمة /الأوامر.
 * تظهر القائمة عند الضغط على Menu أو عندما يبدأ النص بـ "/" (مع التصفية).
 */
@Composable
fun BotCommandsPanel(
    commands: List<BotManager.Command>,
    typedText: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    onPick: (String) -> Unit
) {
    val accent by ThemeManager.accentColor.collectAsState()
    val typingCommand = typedText.startsWith("/")
    val filtered = if (typingCommand) {
        commands.filter { it.command.startsWith(typedText.trim().lowercase()) }
    } else commands
    val showList = (expanded || typingCommand) && filtered.isNotEmpty()

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        if (showList) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = LocalSettingsTheme.current.theme.surfaceColor,
                border = BorderStroke(1.dp, LocalSettingsTheme.current.theme.dividerColor),
                shadowElevation = 0.dp,
                tonalElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    filtered.forEach { c ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(c.command) }
                                .padding(horizontal = 16.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(c.command, color = accent, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text(
                                c.description,
                                color = LocalSettingsTheme.current.theme.textSecondary,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
        if (commands.isNotEmpty()) Box(
            modifier = Modifier
                .padding(top = 6.dp, bottom = 2.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(accent)
                .clickable(onClick = onToggle)
        ) {
            Text(
                text = BotManagerStrings.MENU,
                color = onAccent(accent),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }
}
