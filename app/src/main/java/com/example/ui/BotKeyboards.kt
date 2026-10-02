package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ThemeManager
import com.example.bot.BotManager
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/* ============================ النماذج + المحلل (Telegram reply_markup) ============================ */

data class InlineButton(val text: String, val callbackData: String? = null, val url: String? = null)

private val markupJson = Json { ignoreUnknownKeys = true; isLenient = true }

private fun parseObj(raw: String?): JsonObject? = try {
    if (raw.isNullOrBlank()) null else markupJson.parseToJsonElement(raw).jsonObject
} catch (e: Exception) { null }

/** InlineKeyboardMarkup => صفوف الأزرار المضمّنة تحت الرسالة */
fun parseInlineKeyboard(raw: String?): List<List<InlineButton>> {
    val rows = parseObj(raw)?.get("inline_keyboard") as? JsonArray ?: return emptyList()
    return rows.mapNotNull { row ->
        (row as? JsonArray)?.mapNotNull { b ->
            val o = b as? JsonObject ?: return@mapNotNull null
            val text = o["text"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            InlineButton(text, o["callback_data"]?.jsonPrimitive?.contentOrNull, o["url"]?.jsonPrimitive?.contentOrNull)
        }?.takeIf { it.isNotEmpty() }
    }
}

/**
 * ReplyKeyboardMarkup الحالي: آخر رسالة بوت تحمل keyboard (يُعرض) أو remove_keyboard (يُخفى).
 * يرجع null إذا لا توجد لوحة أزرار.
 */
fun currentReplyKeyboard(messages: List<MessageModel>): List<List<String>>? {
    for (m in messages.asReversed()) {
        if (m.isMine || m.replyMarkup.isNullOrBlank()) continue
        val o = parseObj(m.replyMarkup) ?: continue
        if (o["remove_keyboard"]?.jsonPrimitive?.booleanOrNull == true) return null
        val kb = o["keyboard"] as? JsonArray ?: continue
        return kb.mapNotNull { row ->
            (row as? JsonArray)?.mapNotNull { b ->
                when (b) {
                    is JsonObject -> b["text"]?.jsonPrimitive?.contentOrNull
                    else -> b.jsonPrimitive.contentOrNull
                }
            }?.takeIf { it.isNotEmpty() }
        }.takeIf { it.isNotEmpty() }
    }
    return null
}

private fun onAccentColor(accent: Color): Color = if (accent.luminance() > 0.6f) Color.Black else Color.White

/* ============================ الواجهات ============================ */

/** زر القائمة (بيل ملوّن) داخل شريط الكتابة: ≡ يتحول إلى ✕ عند فتح قائمة الأوامر. */
@Composable
fun BotMenuPill(open: Boolean, onClick: () -> Unit) {
    val accent by ThemeManager.accentColor.collectAsState()
    Row(
        modifier = Modifier
            .padding(start = 6.dp, end = 4.dp)
            .heightIn(min = 36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(accent)
            .clickable(onClick = onClick)
            .padding(horizontal = if (open) 12.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = if (open) Icons.Default.Close else Icons.Default.Menu,
            contentDescription = "Menu",
            tint = onAccentColor(accent),
            modifier = Modifier.size(20.dp)
        )
        if (!open) {
            Text("Menu", color = onAccentColor(accent), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * قائمة الأوامر (مثل تيليجرام): بطاقة عائمة فوق شريط الكتابة، بارتفاع أقصى ثابت،
 * وإذا زادت الأوامر عن المساحة تصير تتسحب للأعلى والأسفل بدون ما يتغير حجم البطاقة.
 */
@Composable
fun BotCommandsSheet(commands: List<BotManager.Command>, onPick: (String) -> Unit) {
    val theme = LocalSettingsTheme.current.theme
    val maxH = (LocalConfiguration.current.screenHeightDp * 0.32f).dp
    Box(modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 6.dp)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxH)
                .clip(RoundedCornerShape(20.dp))
                .background(theme.surfaceColor)
        ) {
            items(commands, key = { it.command }) { c ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(c.command) }
                        .padding(horizontal = 20.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        c.description,
                        color = theme.textPrimary,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(c.command, color = theme.textSecondary, fontSize = 15.sp, maxLines = 1, modifier = Modifier.padding(start = 12.dp))
                }
            }
        }
    }
}

/** أيقونة تبديل لوحة الأزرار السفلية (4 مربعات) قبل زر الإرسال/المايك. */
@Composable
fun ReplyKeyboardToggle(active: Boolean, onClick: () -> Unit) {
    val accent by ThemeManager.accentColor.collectAsState()
    val tint = if (active) accent else Color.Gray
    Box(
        modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(22.dp)) {
            val gap = size.width * 0.14f
            val cell = (size.width - gap) / 2f
            val r = CornerRadius(cell * 0.28f, cell * 0.28f)
            val st = Stroke(width = 2.dp.toPx())
            for (ix in 0..1) for (iy in 0..1) {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(ix * (cell + gap), iy * (cell + gap)),
                    size = Size(cell, cell),
                    cornerRadius = r,
                    style = st
                )
            }
        }
    }
}

/** لوحة الأزرار (ReplyKeyboardMarkup) تحت شريط الكتابة: الضغط يرسل نص الزر. */
@Composable
fun ReplyKeyboardGrid(rows: List<List<String>>, onKey: (String) -> Unit) {
    val theme = LocalSettingsTheme.current.theme
    val maxH = (LocalConfiguration.current.screenHeightDp * 0.32f).dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxH)
            .background(theme.surfaceColor)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { label ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(theme.textPrimary.copy(alpha = 0.10f))
                            .clickable { onKey(label) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label, color = theme.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center, maxLines = 2, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

/** الأزرار المضمّنة (InlineKeyboardMarkup) تحت رسالة البوت. */
@Composable
fun InlineKeyboardGrid(rows: List<List<InlineButton>>, onClick: (InlineButton) -> Unit) {
    val theme = LocalSettingsTheme.current.theme
    val isDark = theme.bgColor.luminance() < 0.5f
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { b ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.28f))
                            .clickable { onClick(b) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            b.text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center, maxLines = 2, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
