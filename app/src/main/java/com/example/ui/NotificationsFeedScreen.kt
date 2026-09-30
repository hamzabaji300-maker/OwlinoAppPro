package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

enum class NotificationType {
    STORY_LIKE, NEW_MESSAGE, SECURITY_ALERT, NEW_FOLLOW
}

data class MockNotification(
    val id: String,
    val type: NotificationType,
    val avatarUrl: String? = null,
    val textUsername: String?,
    val textBody: String,
    val timeAgo: String,
    val rightImageUrl: String? = null,
    val isFollowing: Boolean = false // For NEW_FOLLOW
)

val mockNotifications7Days = listOf(
    MockNotification(
        id = "1",
        type = NotificationType.STORY_LIKE,
        avatarUrl = "https://i.pravatar.cc/150?img=11",
        textUsername = "_charaf_42_, x_han__en et 3 autres personnes",
        textBody = " ont aimé votre story.",
        timeAgo = "6 j",
        rightImageUrl = "https://i.pravatar.cc/150?img=20"
    )
)

val mockNotifications30Days = listOf(
    MockNotification(
        id = "2",
        type = NotificationType.SECURITY_ALERT,
        avatarUrl = null,
        textUsername = null,
        textBody = "Un appareil Samsung SM-A055F non reconnu vient de se connecter près de Algiers, Algeria, DZ.",
        timeAgo = "1 sem",
        rightImageUrl = "https://i.pravatar.cc/150?img=30" // Map placeholder
    ),
    MockNotification(
        id = "3",
        type = NotificationType.NEW_MESSAGE,
        avatarUrl = "https://i.pravatar.cc/150?img=33",
        textUsername = null,
        textBody = "Vous avez un message de x_abdelhani_x.",
        timeAgo = "2 sem",
        rightImageUrl = null
    ),
    MockNotification(
        id = "4",
        type = NotificationType.STORY_LIKE, // Let's reuse for comment like
        avatarUrl = "https://i.pravatar.cc/150?img=44",
        textUsername = "_oxx1k_",
        textBody = " a aimé votre commentaire : « 🤣 😂 »",
        timeAgo = "3 sem",
        rightImageUrl = "https://i.pravatar.cc/150?img=50"
    )
)

val mockNotificationsEarlier = listOf(
    MockNotification(
        id = "5",
        type = NotificationType.NEW_FOLLOW,
        avatarUrl = "https://i.pravatar.cc/150?img=68",
        textUsername = "imane12345655",
        textBody = ", que vous connaissez peut-être, est sur Instagram.",
        timeAgo = "4 sem",
        isFollowing = false
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsFeedScreen(onBack: () -> Unit) {
    val themeConfig = LocalSettingsTheme.current
    val theme = themeConfig.theme
    val bgColor = theme.bgColor
    val surfaceColor = theme.surfaceColor
    val textColor = theme.textPrimary
    val textSecondary = theme.textSecondary
    val accentColor = themeConfig.accent

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontWeight = FontWeight.Bold, color = textColor, fontSize = 22.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bgColor,
                    titleContentColor = textColor
                )
            )
        },
        containerColor = bgColor
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                UpToDateHeader(textColor, textSecondary, accentColor, surfaceColor)
            }
            
            if (mockNotifications7Days.isNotEmpty()) {
                item {
                    SectionHeader("Last 7 days", textColor)
                }
                items(mockNotifications7Days) { notif ->
                    NotificationItemRow(notif, textColor, textSecondary, accentColor, bgColor)
                }
            }

            if (mockNotifications30Days.isNotEmpty()) {
                item {
                    SectionHeader("Last 30 days", textColor)
                }
                items(mockNotifications30Days) { notif ->
                    NotificationItemRow(notif, textColor, textSecondary, accentColor, bgColor)
                }
            }

            if (mockNotificationsEarlier.isNotEmpty()) {
                item {
                    SectionHeader("Earlier", textColor)
                }
                items(mockNotificationsEarlier) { notif ->
                    NotificationItemRow(notif, textColor, textSecondary, accentColor, bgColor)
                }
            }
        }
    }
}

@Composable
fun UpToDateHeader(textColor: Color, textSecondary: Color, accentColor: Color, surfaceColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .border(2.dp, textSecondary.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = textColor, modifier = Modifier.size(30.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text("You're up to date", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text("See what's new for ather.dz", color = accentColor, fontSize = 14.sp)
        }
    }
}

@Composable
fun SectionHeader(title: String, textColor: Color) {
    Text(
        text = title,
        color = textColor,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
fun NotificationItemRow(notif: MockNotification, textColor: Color, textSecondary: Color, accentColor: Color, bgColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Icon/Avatar
        Box(
            modifier = Modifier.size(48.dp)
        ) {
            if (notif.type == NotificationType.SECURITY_ALERT) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color(0xFFE53935)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PriorityHigh, contentDescription = null, tint = Color.White)
                }
            } else {
                AsyncImage(
                    model = notif.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(textSecondary.copy(alpha = 0.2f)),
                    contentScale = ContentScale.Crop
                )
                if (notif.type == NotificationType.STORY_LIKE) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color.White) // Should ideally be background color of the screen to cut out, but white or bgColor is fine. Let's use red directly.
                            .border(2.dp, bgColor, CircleShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color(0xFFE53935)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Text
        val annotatedText = buildAnnotatedString {
            if (notif.textUsername != null) {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = textColor)) {
                    append(notif.textUsername)
                }
            }
            withStyle(style = SpanStyle(color = textColor)) {
                append(notif.textBody)
            }
            withStyle(style = SpanStyle(color = textSecondary)) {
                append(" ${notif.timeAgo}")
            }
        }

        Text(
            text = annotatedText,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Right element
        if (notif.type == NotificationType.NEW_FOLLOW) {
            Button(
                onClick = { /*TODO*/ },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)), // Blue follow button
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("Follow", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        } else if (notif.rightImageUrl != null) {
            AsyncImage(
                model = notif.rightImageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(textSecondary.copy(alpha = 0.2f)),
                contentScale = ContentScale.Crop
            )
        }
    }
}
