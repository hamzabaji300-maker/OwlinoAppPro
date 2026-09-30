package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MessageCircle
import com.composables.icons.lucide.Users

private val HomeBlue = Color(0xFF3B82F6)

/**
 * الشريط العلوي: ☰ (الإعدادات) + كارد البحث (شاشة البحث) + أفاتار المستخدم (تعديل الملف الشخصي)
 */
@Composable
fun HomeTopBar(
    hint: String,
    avatarUrl: String?,
    initial: String,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    onAvatarClick: () -> Unit
) {
    val themeConfig = LocalSettingsTheme.current
    val theme = themeConfig.theme
    val isDark = theme.isDark
    val cardColor = if (isDark) Color(0xFF1F1F22) else Color.White
    val cardBorder = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 14.dp, top = 10.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMenuClick) {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = "Menu",
                tint = theme.textPrimary,
                modifier = Modifier.size(26.dp)
            )
        }

        Surface(
            shape = RoundedCornerShape(50),
            color = cardColor,
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, cardBorder),
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .padding(horizontal = 4.dp)
                .clip(RoundedCornerShape(50))
                .clickable { onSearchClick() }
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(start = 20.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = hint,
                    modifier = Modifier.weight(1f),
                    fontSize = 16.sp,
                    color = theme.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search",
                    tint = theme.textPrimary.copy(alpha = 0.75f),
                    modifier = Modifier.size(24.dp)
                )
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
                Text(
                    text = initial,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/** زر «رسالة جديدة» الممتد (قلم + نص) */
@Composable
fun HomeExtendedFab(label: String, onClick: () -> Unit) {
    val isDark = LocalSettingsTheme.current.theme.isDark
    val container = if (isDark) Color(0xFF2B3A67) else Color(0xFFDCE3F9)
    val content = if (isDark) Color(0xFFDCE3F9) else Color(0xFF1B2B5B)
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = container,
        shadowElevation = 6.dp,
        modifier = Modifier
            .padding(bottom = 4.dp, end = 4.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Filled.Edit, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
            Text(label, color = content, fontSize = 17.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}

/** الشريط السفلي: المحادثات (مع عدّاد) + متابعين */
@Composable
fun HomeBottomBar(
    chatsLabel: String,
    followersLabel: String,
    unreadCount: Int,
    onChatsClick: () -> Unit,
    onFollowersClick: () -> Unit
) {
    val theme = LocalSettingsTheme.current.theme
    val isDark = theme.isDark
    val barColor = if (isDark) Color(0xFF17171A) else Color(0xFFF7F8FC)
    val inactive = if (isDark) Color(0xFF9CA3AF) else Color(0xFF5F6368)

    Surface(color = barColor, shadowElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(72.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeNavItem(
                modifier = Modifier.weight(1f),
                label = chatsLabel,
                selected = true,
                inactive = inactive,
                badge = if (unreadCount > 0) (if (unreadCount > 99) "99+" else unreadCount.toString()) else null,
                icon = { tint -> Icon(Lucide.MessageCircle, null, tint = tint, modifier = Modifier.size(28.dp)) },
                onClick = onChatsClick
            )
            HomeNavItem(
                modifier = Modifier.weight(1f),
                label = followersLabel,
                selected = false,
                inactive = inactive,
                badge = null,
                icon = { tint -> Icon(Lucide.Users, null, tint = tint, modifier = Modifier.size(28.dp)) },
                onClick = onFollowersClick
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
    icon: @Composable (Color) -> Unit,
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
            icon(tint)
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 12.dp, y = (-8).dp)
                        .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                        .background(HomeBlue, CircleShape)
                        .padding(horizontal = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(badge, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = tint, fontSize = 13.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1)
    }
}
