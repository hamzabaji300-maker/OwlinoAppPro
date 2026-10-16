package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import com.example.supabase
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Group
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val HomeBlue = Color(0xFF3B82F6)

/**
 * الشريط العلوي (ثابت): ☰/← + كارد البحث (يتحول إلى حقل كتابة في مكانه) + أفاتار المستخدم
 */
@Composable
fun HomeTopBar(
    hint: String,
    avatarUrl: String?,
    initial: String,
    isSearching: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchActivate: () -> Unit,
    onCloseSearch: () -> Unit,
    onMenuClick: () -> Unit,
    onAvatarClick: () -> Unit
) {
    com.example.ui.redesign.RdPlainText {
        HomeTopBarImpl(hint, avatarUrl, initial, isSearching, query, onQueryChange, onSearchActivate, onCloseSearch, onMenuClick, onAvatarClick)
    }
}

@Composable
private fun HomeTopBarImpl(
    hint: String,
    avatarUrl: String?,
    initial: String,
    isSearching: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchActivate: () -> Unit,
    onCloseSearch: () -> Unit,
    onMenuClick: () -> Unit,
    onAvatarClick: () -> Unit
) {
    val themeConfig = LocalSettingsTheme.current
    val theme = themeConfig.theme
    val isDark = theme.isDark
    val cardColor = com.example.ui.SettingsColors.surface
    val cardBorder = com.example.ui.redesign.RdColors.border(isDark)
    val focusRequester = androidx.compose.runtime.remember { androidx.compose.ui.focus.FocusRequester() }
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 14.dp, top = 10.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { if (isSearching) onCloseSearch() else onMenuClick() }) {
            com.example.ui.redesign.RdGlyph(
                kind = if (isSearching) com.example.ui.redesign.RdGlyphKind.ARROW_BACK else com.example.ui.redesign.RdGlyphKind.MENU,
                color = theme.textPrimary,
                modifier = Modifier.size(26.dp)
            )
        }

        Surface(
            shape = RoundedCornerShape(50),
            color = cardColor,
            shadowElevation = 0.dp,
            tonalElevation = 0.dp,
            border = BorderStroke(1.dp, cardBorder),
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .padding(horizontal = 4.dp)
                .clip(RoundedCornerShape(50))
                .clickable(
                    interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) {
                    focusRequester.requestFocus()
                    keyboard?.show()
                }
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(start = 20.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = theme.textPrimary),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(HomeBlue),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { keyboard?.hide() }),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .onFocusChanged { if (it.isFocused && !isSearching) onSearchActivate() },
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (query.isEmpty()) {
                                Text(
                                    text = hint,
                                    fontSize = 16.sp,
                                    color = theme.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            inner()
                        }
                    }
                )
                if (query.isNotEmpty()) {
                    com.example.ui.redesign.RdGlyph(
                        kind = com.example.ui.redesign.RdGlyphKind.CLOSE,
                        color = theme.textPrimary.copy(alpha = 0.75f),
                        modifier = Modifier.size(22.dp).clip(CircleShape).clickable { onQueryChange("") }
                    )
                } else {
                    com.example.ui.redesign.RdGlyph(
                        kind = com.example.ui.redesign.RdGlyphKind.SEARCH,
                        color = theme.textPrimary.copy(alpha = 0.75f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // أفاتار بحلقة زرقاء
        Box(
            modifier = Modifier
                .size(46.dp)
                .border(2.dp, HomeBlue.copy(alpha = 0.55f), CircleShape)
                .padding(4.dp)
                .clip(CircleShape)
                .background(Color(0xFF1A5FB4))
                .clickable { onAvatarClick() },
            contentAlignment = Alignment.Center
        ) {
            if (!avatarUrl.isNullOrBlank()) {
                coil.compose.AsyncImage(
                    model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                        .data(avatarUrl).crossfade(true).build(),
                    contentDescription = "My Profile",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(text = initial, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

/** زر «رسالة جديدة» الممتد (قلم + نص) */
@Composable
fun HomeExtendedFab(label: String, onClick: () -> Unit) {
    val isDark = LocalSettingsTheme.current.theme.isDark
    val container = com.example.ui.SettingsColors.surface
    val content = if (isDark) Color(0xFFDCE3F9) else Color(0xFF1B2B5B)
    Surface(
        shape = CircleShape,
        color = container,
        shadowElevation = 4.dp,
        tonalElevation = 0.dp,
        modifier = Modifier
            .padding(bottom = 4.dp, end = 4.dp)
            .size(56.dp)
            .clip(CircleShape)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            com.example.ui.redesign.RdGlyph(com.example.ui.redesign.RdGlyphKind.PENCIL, content, Modifier.size(24.dp))
        }
    }
}

/** الشريط السفلي: المحادثات (مع عدّاد) + متابعين — يبقى ظاهراً دائماً (تصميم BottomBarView: كارد ينزلق 240ms) */
@Composable
fun HomeBottomBar(
    chatsLabel: String,
    followersLabel: String,
    selectedIndex: Int,
    unreadCount: Int,
    onChatsClick: () -> Unit,
    onFollowersClick: () -> Unit
) {
    val isDark = LocalSettingsTheme.current.theme.isDark
    Surface(color = com.example.ui.redesign.RdColors.bg(isDark), shadowElevation = 0.dp, tonalElevation = 0.dp) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            com.example.ui.redesign.RdBottomBar(
                chatsLabel = chatsLabel,
                followersLabel = followersLabel,
                selectedIndex = selectedIndex,
                unreadCount = unreadCount,
                isDark = isDark,
                onChatsClick = onChatsClick,
                onFollowersClick = onFollowersClick
            )
        }
    }
}

@Composable
private fun HomeNavItem(
    modifier: Modifier,
    label: String,
    selected: Boolean,
    inactive: Color,
    badge: String?,
    icon: @Composable (Boolean, Color) -> Unit,
    onClick: () -> Unit
) {
    val tint = if (selected) HomeBlue else inactive
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            icon(selected, tint)
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 12.dp, y = (-7).dp)
                        .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                        .background(HomeBlue, CircleShape)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(badge, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(label, color = tint, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1)
    }
}

/** محتوى البحث (يُعرض تحت الكارد الثابت دون تغيير الكارد نفسه): نفس شاشة البحث الجديدة (أنواع + صفحات تُسحب) */
@Composable
fun DiscoverSearchContent(
    searchQuery: String,
    onUserClick: (String) -> Unit,
    onBotClick: (String, String) -> Unit = { _, _ -> },
    onChannelClick: (String, String) -> Unit = { _, _ -> }
) {
    DiscoverUsersBody(
        searchQuery = searchQuery,
        onUserClick = onUserClick,
        onBotClick = onBotClick,
        onChannelClick = onChannelClick,
        modifier = Modifier.fillMaxSize()
    )
}
