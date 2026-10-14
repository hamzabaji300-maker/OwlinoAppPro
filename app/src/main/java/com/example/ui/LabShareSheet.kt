package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val SharePalette = listOf(
    Color(0xFF34D399) to Color(0xFF059669),
    Color(0xFFFB923C) to Color(0xFFEA580C),
    Color(0xFF60A5FA) to Color(0xFF2563EB),
    Color(0xFFA78BFA) to Color(0xFF7C3AED),
    Color(0xFFF472B6) to Color(0xFFDB2777),
    Color(0xFF38BDF8) to Color(0xFF0284C7)
)

/**
 * ورقة المشاركة (مثل تيليجرام): بحث "إرسال إلى…" + شبكة وجهات بصور دائرية + شريط سفلي
 * ("نسخ الرابط" عند عدم اختيار أحد، و"إرسال (n)" عند اختيار وجهة أو أكثر).
 * الفهرس 0 = الرسائل المحفوظة، و i+1 = targetTitles[i].
 */
@Composable
fun LabShareSheet(
    targetTitles: List<String>,
    onDismiss: () -> Unit,
    onCopyLink: () -> Unit,
    onSend: (List<Int>) -> Unit
) {
    val cfg = LocalSettingsTheme.current
    val theme = cfg.theme
    val accent = cfg.accent
    val fieldBg = if (theme.isDark) Color(0xFF2B2B2B) else Color(0xFFEDEDF2)
    val ease = FastOutSlowInEasing

    var visible by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(setOf<Int>()) }
    LaunchedEffect(Unit) { visible = true }
    val close: () -> Unit = { visible = false }
    LaunchedEffect(visible) {
        if (!visible) {
            delay(240)
            onDismiss()
        }
    }
    BackHandler { close() }

    val entries = remember(targetTitles) { listOf("الرسائل المحفوظة") + targetTitles }
    val shown = remember(entries, query) {
        entries.withIndex().filter { query.isBlank() || it.value.contains(query.trim(), ignoreCase = true) }
    }
    val scrim by animateFloatAsState(if (visible) 0.5f else 0f, tween(220), label = "share_scrim")

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrim))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { close() }
        )
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(320, easing = ease)) { it } + fadeIn(tween(200)),
            exit = slideOutVertically(tween(240, easing = ease)) { it } + fadeOut(tween(160))
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = theme.surfaceColor,
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.78f)
            ) {
                Box(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 12.dp), contentAlignment = Alignment.Center) {
                            Box(Modifier.width(40.dp).height(4.dp).clip(CircleShape).background(theme.textSecondary.copy(alpha = 0.4f)))
                        }
                        // البحث
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp)
                                .height(50.dp)
                                .clip(RoundedCornerShape(25.dp))
                                .background(fieldBg)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Search, contentDescription = null, tint = theme.textSecondary, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(10.dp))
                            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                if (query.isEmpty()) Text("إرسال إلى…", color = theme.textSecondary, fontSize = 16.sp)
                                BasicTextField(
                                    value = query, onValueChange = { query = it }, singleLine = true,
                                    textStyle = TextStyle(color = theme.textPrimary, fontSize = 16.sp),
                                    cursorBrush = SolidColor(theme.textPrimary),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 90.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(shown, key = { it.index }) { entry ->
                                val idx = entry.index
                                val isSel = idx in selected
                                val (c1, c2) = SharePalette[idx % SharePalette.size]
                                Column(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { selected = if (isSel) selected - idx else selected + idx }
                                        .padding(vertical = 8.dp, horizontal = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(Modifier.size(66.dp), contentAlignment = Alignment.Center) {
                                        Box(
                                            Modifier.size(60.dp).clip(CircleShape).background(Brush.linearGradient(listOf(c1, c2))),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (idx == 0) {
                                                Icon(Icons.Outlined.BookmarkBorder, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                                            } else {
                                                Text(entry.value.take(1), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        val badgeScale by animateFloatAsState(if (isSel) 1f else 0f, tween(160), label = "share_badge")
                                        if (badgeScale > 0.01f) {
                                            Box(
                                                Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .graphicsLayer { scaleX = badgeScale; scaleY = badgeScale; alpha = badgeScale }
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(accent),
                                                contentAlignment = Alignment.Center
                                            ) { Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                                        }
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        entry.value, color = theme.textPrimary, fontSize = 12.sp, textAlign = TextAlign.Center,
                                        maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                    // الشريط السفلي: نسخ الرابط / إرسال
                    val has = selected.isNotEmpty()
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
                            .height(52.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(if (has) accent else fieldBg)
                            .clickable { if (has) onSend(selected.sorted()) else onCopyLink() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (has) "إرسال (${selected.size})" else "نسخ الرابط",
                            color = if (has) Color.White else accent,
                            fontSize = 17.sp, fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
