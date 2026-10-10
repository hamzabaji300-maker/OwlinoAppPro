package com.example.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.emoji.EmojiMessageUtils
import com.example.emoji.NotoEmojiMap
import com.example.util.GifClient
import com.example.util.GifItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

private enum class PanelTab { EMOJI, GIF, STICKERS }

private class EmojiCategory(val icon: ImageVector, val emojis: List<String>)

private fun e(s: String) = s.trim().split(" ").filter { it.isNotBlank() }

private val EMOJI_CATEGORIES = listOf(
    EmojiCategory(Icons.Outlined.SentimentSatisfiedAlt, e("😀 😃 😄 😁 😆 😅 😂 🤣 🥲 ☺️ 😊 😇 🙂 🙃 😉 😌 😍 🥰 😘 😗 😙 😚 😋 😛 😝 😜 🤪 🤨 🧐 🤓 😎 🥸 🤩 🥳 😏 😒 😞 😔 😟 😕 🙁 ☹️ 😣 😖 😫 😩 🥺 😢 😭 😤 😠 😡 🤬 🤯 😳 🥵 🥶 😱 😨 😰 😥 😓 🤗 🤔 🤭 🤫 🤥 😶 😐 😑 😬 🙄 😯 😦 😧 😮 😲 🥱 😴 🤤 😪 😵 🤐 🥴 🤢 🤮 🤧 😷 🤒 🤕 👍 👎 👏 🙌 🙏 🤝 👋 ✌️ 🤞 👌 💪")),
    EmojiCategory(Icons.Outlined.Pets, e("🐶 🐱 🐭 🐹 🐰 🦊 🐻 🐼 🐨 🐯 🦁 🐮 🐷 🐸 🐵 🙈 🙉 🙊 🐔 🐧 🐦 🐤 🦆 🦅 🦉 🦇 🐺 🐗 🐴 🦄 🐝 🐛 🦋 🐌 🐞 🐜 🐢 🐍 🦎 🐙 🦑 🦐 🦀 🐡 🐠 🐟 🐬 🐳 🐋 🦈 🐊 🐅 🐆 🦓 🦍 🐘 🦏 🐪 🐫 🦒 🦘 🐃 🐂 🐄 🐎 🐖 🐏 🐑 🐐 🦌 🐕 🐩 🐈 🐓 🦃 🐇 🐁 🐀 🦔")),
    EmojiCategory(Icons.Outlined.Fastfood, e("🍏 🍎 🍐 🍊 🍋 🍌 🍉 🍇 🍓 🫐 🍈 🍒 🍑 🥭 🍍 🥥 🥝 🍅 🍆 🥑 🥦 🥬 🥒 🌽 🥕 🧄 🧅 🥔 🍠 🥐 🥯 🍞 🥖 🧀 🥚 🍳 🥞 🧇 🥓 🥩 🍗 🍖 🌭 🍔 🍟 🍕 🥪 🌮 🌯 🥗 🍝 🍜 🍲 🍛 🍣 🍱 🥟 🍤 🍙 🍚 🍘 🍥 🍦 🍧 🍨 🍩 🍪 🎂 🍰 🧁 🍫 🍬 🍭 ☕ 🍵 🥤 🍺 🍷")),
    EmojiCategory(Icons.Outlined.SportsSoccer, e("⚽ 🏀 🏈 ⚾ 🥎 🎾 🏐 🏉 🥏 🎱 🏓 🏸 🏒 🥍 🏏 🥅 ⛳ 🏹 🎣 🥊 🥋 🎽 🛹 🛼 ⛸️ 🥌 🎿 🏂 🤼 🤸 🤺 🤾 🏇 🧘 🏄 🏊 🚣 🧗 🚴 🎪 🎭 🎨 🎬 🎤 🎧 🎼 🎹 🥁 🎷 🎺 🎸 🎻 🎲 🎯 🎳 🎮 🎰 🧩")),
    EmojiCategory(Icons.Outlined.DirectionsCar, e("🚗 🚕 🚙 🚌 🚎 🚓 🚑 🚒 🚐 🚚 🚛 🚜 🛴 🚲 🛵 🚨 🚔 🚍 🚘 🚖 🚡 🚠 🚟 🚃 🚋 🚞 🚝 🚄 🚅 🚈 🚂 🚆 🚇 🚊 🚉 ✈️ 🛫 🛬 💺 🚁 🚀 🛸 🚢 ⛵ 🛶 ⚓ 🏖️ 🏝️ 🏔️ ⛰️ 🌋 🏕️ 🏠 🏡 🏢 🏰 🗼 🗽 ⛪ 🕌 🌃 🌆 🌇 🌉 🎠 🎡 🎢")),
    EmojiCategory(Icons.Outlined.Lightbulb, e("⌚ 📱 💻 ⌨️ 🖥️ 🖨️ 🖱️ 💽 💾 📷 📹 🎥 📞 ☎️ 📺 📻 ⏰ 🔋 🔌 💡 🔦 🕯️ 💸 💵 💰 💳 💎 🔧 🔨 🛠️ 🔩 ⚙️ 🧲 💣 🔪 🛡️ 🔮 💊 💉 🔬 🔭 🧹 🧺 🚽 🚿 🛁 🔑 🚪 🛋️ 🛏️ 🎁 🎈 🎉 🎊 ✉️ 📦 📝 📚 📎 ✂️ 🔒 🔓")),
    EmojiCategory(Icons.Outlined.Favorite, e("❤️ 🧡 💛 💚 💙 💜 🖤 🤍 🤎 💔 ❣️ 💕 💞 💓 💗 💖 💘 💝 💟 ☮️ ✝️ ☪️ ☸️ ✡️ ☯️ ♈ ♉ ♊ ♋ ♌ ♍ ♎ ♏ ♐ ♑ ♒ ♓ ✅ ❌ ❎ ❓ ❔ ❕ ❗ ‼️ ⁉️ 💯 🔥 ✨ ⭐ 🌟 ⚡ 💥 💫 💢 💤 🔔 🎵 🎶 ➕ ➖ ➗ ✖️ ♾️ ♻️ ⚠️ 🚫 ⭕"))
)

private val EMOJI_KEYWORDS = mapOf(
    "heart" to "❤️💕💖💗", "قلب" to "❤️💕💖💗", "love" to "😍🥰😘❤️", "حب" to "😍🥰😘❤️",
    "laugh" to "😂🤣😆😄", "ضحك" to "😂🤣😆😄", "cry" to "😭😢", "بكاء" to "😭😢", "sad" to "😢😞😔",
    "حزن" to "😢😞😔", "fire" to "🔥", "نار" to "🔥", "like" to "👍❤️", "اعجاب" to "👍❤️",
    "clap" to "👏🙌", "تصفيق" to "👏🙌", "pray" to "🙏", "دعاء" to "🙏", "cool" to "😎", "angry" to "😡😠🤬",
    "غضب" to "😡😠🤬", "kiss" to "😘💋", "قبلة" to "😘💋", "party" to "🥳🎉🎊", "حفلة" to "🥳🎉🎊",
    "cat" to "🐱🐈", "قط" to "🐱🐈", "dog" to "🐶🐕", "كلب" to "🐶🐕", "food" to "🍕🍔🍟", "طعام" to "🍕🍔🍟"
)

private val POPULAR_STICKERS = e("😂 😍 🥳 😎 🤩 😭 😡 🥰 🤔 😱 🤯 🥺 😴 🤪 🙌 👍 🔥 ❤️ 💯 🎉 🙏 😘 🤗 😇")

/** لوحة الإيموجي / GIF / الملصقات: كارد سفلي صغير مع شريط تبويبات منزلق (بنمط تيليجرام). */
@Composable
fun LabEmojiGifPanel(
    height: Dp,
    recentEmojis: List<String>,
    recentStickers: List<String>,
    onEmoji: (String) -> Unit,
    onBackspace: () -> Unit,
    onGif: (GifItem) -> Unit,
    onSticker: (String) -> Unit,
    onClearStickers: () -> Unit
) {
    val theme = LocalSettingsTheme.current.theme
    val fieldBg = if (theme.isDark) Color(0xFF2B2B2B) else Color(0xFFEDEDF2)
    var tab by remember { mutableStateOf(PanelTab.EMOJI) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(theme.surfaceColor)
    ) {
        Crossfade(targetState = tab, label = "panel_tab") { t ->
            when (t) {
                PanelTab.EMOJI -> EmojiTab(recentEmojis, onEmoji, fieldBg)
                PanelTab.GIF -> GifTab(onGif, fieldBg)
                PanelTab.STICKERS -> StickersTab(recentStickers, onSticker)
            }
        }
        // الشريط السفلي: دائمًا بترتيب (إيموجي | GIF | ملصقات) من اليسار لليمين كما في تيليجرام
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PanelTabBar(
                    selected = tab.ordinal,
                    labels = listOf("الرموز التعبيرية", "صور متحركة", "الملصقات"),
                    onSelect = { tab = PanelTab.values()[it] },
                    modifier = Modifier.weight(1f)
                )
                if (tab != PanelTab.GIF) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .shadow(8.dp, CircleShape)
                            .clip(CircleShape)
                            .background(barColor())
                            .clickable { if (tab == PanelTab.EMOJI) onBackspace() else onClearStickers() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (tab == PanelTab.EMOJI) Icons.AutoMirrored.Outlined.Backspace else Icons.Outlined.Settings,
                            contentDescription = null,
                            tint = theme.textPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun barColor(): Color =
    if (LocalSettingsTheme.current.theme.isDark) Color(0xFF1C1C1C).copy(alpha = 0.97f) else Color(0xFFF2F2F7).copy(alpha = 0.97f)

/** شريط تبويبات بمؤشر منزلق (نفس spring الشريط السفلي في مشروعك). */
@Composable
private fun PanelTabBar(selected: Int, labels: List<String>, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val theme = LocalSettingsTheme.current.theme
    val accent = androidx.compose.material3.MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .height(54.dp)
            .shadow(8.dp, CircleShape)
            .clip(CircleShape)
            .background(barColor())
            .padding(4.dp)
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val w = maxWidth / labels.size
            val off by animateDpAsState(w * selected, spring(dampingRatio = 0.75f, stiffness = 400f), label = "panel_pill")
            Box(
                Modifier
                    .offset(x = off)
                    .width(w)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(accent.copy(alpha = if (theme.isDark) 0.28f else 0.16f))
            )
            Row(Modifier.fillMaxSize()) {
                labels.forEachIndexed { i, l ->
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(i) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            l,
                            color = if (i == selected) accent else theme.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQuery: (String) -> Unit,
    hint: String,
    fieldBg: Color,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    val theme = LocalSettingsTheme.current.theme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .height(50.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(fieldBg)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = theme.textSecondary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) Text(hint, color = theme.textSecondary, fontSize = 15.sp)
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = TextStyle(color = theme.textPrimary, fontSize = 15.sp),
                cursorBrush = SolidColor(theme.textPrimary),
                modifier = Modifier.fillMaxWidth()
            )
        }
        trailing()
    }
}

@Composable
private fun EmojiTab(recent: List<String>, onEmoji: (String) -> Unit, fieldBg: Color) {
    val theme = LocalSettingsTheme.current.theme
    val accent = androidx.compose.material3.MaterialTheme.colorScheme.primary
    var query by remember { mutableStateOf("") }
    var cat by remember { mutableIntStateOf(1) } // 0 = المستخدمة حديثًا
    val list: List<String> = when {
        query.isNotBlank() -> {
            val q = query.trim().lowercase()
            EMOJI_KEYWORDS.filterKeys { it.contains(q) || q.contains(it) }.values
                .flatMap { EmojiMessageUtils.splitGraphemes(it) }.distinct()
        }
        cat == 0 -> recent
        else -> EMOJI_CATEGORIES[cat - 1].emojis
    }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CatIcon(Icons.Outlined.AccessTime, cat == 0, accent, theme.textSecondary, fieldBg) { cat = 0; query = "" }
            Spacer(Modifier.width(6.dp))
            Row(
                Modifier.weight(1f).clip(CircleShape).background(fieldBg).horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                EMOJI_CATEGORIES.forEachIndexed { i, c ->
                    CatIcon(c.icon, cat == i + 1, accent, theme.textSecondary, Color.Transparent) { cat = i + 1; query = "" }
                }
            }
        }
        SearchBar(query, { query = it }, "بحث", fieldBg)
        if (list.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(bottom = 90.dp), contentAlignment = Alignment.Center) {
                Text(if (query.isNotBlank()) "لا توجد نتائج" else "لا توجد رموز مستخدمة حديثًا", color = theme.textSecondary, fontSize = 14.sp)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(8),
                contentPadding = PaddingValues(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 100.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(list) { em ->
                    Box(
                        Modifier.aspectRatio(1f).clip(CircleShape).clickable { onEmoji(em) },
                        contentAlignment = Alignment.Center
                    ) { Text(em, fontSize = 28.sp) }
                }
            }
        }
    }
}

@Composable
private fun CatIcon(icon: ImageVector, active: Boolean, accent: Color, inactive: Color, bg: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(if (active) accent.copy(alpha = 0.22f) else bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, contentDescription = null, tint = if (active) accent else inactive, modifier = Modifier.size(24.dp)) }
}

@Composable
private fun GifTab(onGif: (GifItem) -> Unit, fieldBg: Color) {
    val theme = LocalSettingsTheme.current.theme
    var query by remember { mutableStateOf("") }
    var gifs by remember { mutableStateOf<List<GifItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(query) {
        if (!GifClient.isConfigured) {
            error = "ميزة GIF غير مفعّلة بعد: ضع مفتاح Klipy في ملف klipy.key بجانب مجلد app أو في متغير البيئة KLIPY_API_KEY"
            return@LaunchedEffect
        }
        if (query.isNotBlank()) delay(500)
        loading = true
        error = null
        try {
            val res = if (query.isBlank()) GifClient.trending("lab", 1) else GifClient.search(query.trim(), "lab", 1)
            gifs = res.items
            if (res.items.isEmpty()) error = "لا توجد نتائج"
        } catch (ex: CancellationException) {
            throw ex
        } catch (ex: Exception) {
            gifs = emptyList()
            error = "تعذّر تحميل الـ GIF. تحقق من الإنترنت وحاول مجددًا"
        } finally {
            loading = false
        }
    }

    val quick = listOf(
        Icons.Outlined.FavoriteBorder to "love", Icons.Outlined.ThumbUp to "like", Icons.Outlined.ThumbDown to "dislike",
        Icons.Outlined.Celebration to "party", Icons.Outlined.WavingHand to "hello", Icons.Outlined.SentimentSatisfiedAlt to "happy"
    )
    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.height(8.dp))
        SearchBar(query, { query = it }, "Search KLIPY", fieldBg) {
            quick.forEach { (ic, kw) ->
                Icon(
                    ic, contentDescription = null, tint = theme.textSecondary,
                    modifier = Modifier.size(34.dp).clip(CircleShape).clickable { query = kw }.padding(5.dp)
                )
            }
        }
        if (gifs.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(bottom = 90.dp, start = 24.dp, end = 24.dp), contentAlignment = Alignment.Center) {
                Text(
                    if (loading) "جارٍ التحميل…" else (error ?: ""),
                    color = theme.textSecondary, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 6.dp, top = 4.dp, end = 6.dp, bottom = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item(span = { GridItemSpan(2) }) {
                    Text(
                        if (query.isBlank()) "الصور المتحركة الشائعة" else "نتائج البحث",
                        color = theme.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                    )
                }
                items(gifs, key = { it.id }) { g ->
                    AsyncImage(
                        model = g.previewUrl,
                        contentDescription = g.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio((g.aspectRatio ?: 1.3f).coerceIn(0.7f, 2f))
                            .clip(RoundedCornerShape(10.dp))
                            .background(fieldBg)
                            .clickable { onGif(g) }
                    )
                }
                item(span = { GridItemSpan(2) }) {
                    Text("Powered by KLIPY", color = theme.textSecondary, fontSize = 11.sp, modifier = Modifier.padding(8.dp))
                }
            }
        }
    }
}

@Composable
private fun StickersTab(recent: List<String>, onSticker: (String) -> Unit) {
    val theme = LocalSettingsTheme.current.theme
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        contentPadding = PaddingValues(start = 10.dp, top = 12.dp, end = 10.dp, bottom = 100.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(span = { GridItemSpan(5) }) {
            Text("الملصقات الشائعة", color = theme.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(6.dp))
        }
        items(POPULAR_STICKERS) { em -> StickerCell(em, onSticker) }
        if (recent.isNotEmpty()) {
            item(span = { GridItemSpan(5) }) {
                Text("مستخدمة حديثًا", color = theme.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp, top = 18.dp, end = 6.dp, bottom = 6.dp))
            }
            items(recent) { em -> StickerCell(em, onSticker) }
        }
    }
}

@Composable
private fun StickerCell(emoji: String, onSticker: (String) -> Unit) {
    val url = remember(emoji) { NotoEmojiMap.remoteUrlFor(emoji) }
    Box(
        Modifier.aspectRatio(1f).clip(RoundedCornerShape(12.dp)).clickable { onSticker(emoji) },
        contentAlignment = Alignment.Center
    ) {
        if (url != null) com.example.ui.LottieEmojiReaction(url = url, size = 52.dp, loopForever = false)
        else Text(emoji, fontSize = 34.sp)
    }
}
