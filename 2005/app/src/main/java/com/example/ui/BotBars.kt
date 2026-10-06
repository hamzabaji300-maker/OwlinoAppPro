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

private fun onAccent(accent: Color): Color = if (accent.luminance() > 0.6f) Color.Black else Color.White

/** بطاقة START: تحل محل لوحة المفاتيح عند دخول أي بوت لأول مرة، بلون التمييز من الإعدادات. */
@Composable
fun BotStartBar(label: String = BotManagerStrings.START, onClick: () -> Unit) {
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
                text = label,
                color = onAccent(accent),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }
}

/** اقتراحات الأوامر المنبثقة فوق شريط الكتابة عندما يكتب المستخدم "/" (تُصفّى حسب ما كُتب). */
@Composable
fun BotCommandSuggestions(commands: List<BotManager.Command>, typedText: String, onPick: (String) -> Unit) {
    val theme = LocalSettingsTheme.current.theme
    val accent by ThemeManager.accentColor.collectAsState()
    if (!typedText.startsWith("/")) return
    val filtered = commands.filter { it.command.startsWith(typedText.trim().lowercase()) }
    if (filtered.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = theme.surfaceColor,
            border = BorderStroke(1.dp, theme.dividerColor),
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
                        Text(c.description, color = theme.textSecondary, fontSize = 13.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}
