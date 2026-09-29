package com.example.ui

import com.example.ui.SettingsColors

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.*

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.example.ui.i18n.LocalTranslation
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.scale

private val ScreenBg = Color(0xFF000000)
private val CardBg = Color(0xFF18181B)
private val TextGray = Color(0xFFAAAAAA)
private val DividerColor = Color(0xFF2A2A2A)
private val BlueToggle = Color(0xFF007AFF)




@OptIn(ExperimentalMaterial3Api::class)




@Composable
fun NotificationsScreen(onBack: () -> Unit) {
        Scaffold(
        containerColor = ScreenBg,
        topBar = {
            TopAppBar(
                title = { Text(com.example.ui.i18n.LocalTranslation.current.notificationsSettings, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Lucide.ArrowLeft, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ScreenBg,
                    titleContentColor = SettingsColors.textPrimary,
                    navigationIconContentColor = SettingsColors.textPrimary
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Section 1
            item {
                NotificationSectionHeader("Show notifications for")
                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(SettingsColors.cardRadius)).background(CardBg)) {
                    NotificationToggleRow(
                        icon = Lucide.User,
                        title = "All accounts",
                        subtitle = "Turn off to only receive notifications f...",
                        isChecked = true
                    )
                }
                Text(
                    "Turn this off if you want to receive notifications only from your active account.",
                    color = TextGray,
                    fontSize = 12.sp, lineHeight = 16.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 32.dp, end = 16.dp)
                )
            }

            // Section 2: Push
            item {
                NotificationSectionHeader("Push")
                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(SettingsColors.cardRadius)).background(CardBg)) {
                    Column {
                        NotificationToggleRow(Lucide.MessageSquare, "Messages", "", true)
                        HorizontalDivider(color = DividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 56.dp))
                        NotificationToggleRow(Lucide.AtSign, "Mentions & replies", "", true)
                        HorizontalDivider(color = DividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 56.dp))
                        NotificationToggleRow(Lucide.Phone, "Calls", "", true)
                        HorizontalDivider(color = DividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 56.dp))
                        NotificationToggleRow(Lucide.Users, "Groups", "", true)
                        HorizontalDivider(color = DividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 56.dp))
                        NotificationToggleRow(Lucide.Radio, "Channels", "", false)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Section 3: Interactions
            item {
                NotificationSectionHeader("Interactions")
                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(SettingsColors.cardRadius)).background(CardBg)) {
                    Column {
                        NotificationToggleRow(Lucide.Heart, "Likes", "When someone likes your post or co...", true)
                        HorizontalDivider(color = DividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 56.dp))
                        NotificationToggleRow(Lucide.MessageSquare, "Comments", "When someone comments on your p...", true)
                        HorizontalDivider(color = DividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 56.dp))
                        NotificationToggleRow(Lucide.MessageSquare, "Replies", "When someone replies to your comm...", true)
                        HorizontalDivider(color = DividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 56.dp))
                        NotificationToggleRow(Lucide.User, "Follows", "When someone starts following you", true)
                        HorizontalDivider(color = DividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 56.dp))
                        NotificationToggleRow(Lucide.AtSign, "Mentions", "When someone mentions you", true)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Section 4: In-app
            item {
                NotificationSectionHeader("In-app")
                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(SettingsColors.cardRadius)).background(CardBg)) {
                    Column {
                        NotificationToggleRow(Lucide.Volume2, "Sounds", "", true)
                        HorizontalDivider(color = DividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 56.dp))
                        NotificationToggleRow(Lucide.Vibrate, "Vibrate", "", false)
                        HorizontalDivider(color = DividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 56.dp))
                        NotificationToggleRow(Lucide.Bell, "Show message preview", "Display sender & content in banners", true)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Section 5: Digest
            item {
                NotificationSectionHeader("Digest")
                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(SettingsColors.cardRadius)).background(CardBg)) {
                    Column {
                        NotificationToggleRow(Lucide.Mail, "Weekly email digest", "Summary of your week, every Monday", false)
                        HorizontalDivider(color = DividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 56.dp))
                        NotificationToggleRow(Lucide.Moon, "Quiet hours", "Mute notifications 10:00 PM – 7:00 AM", false)
                    }
                }
                Text(
                    "Quiet hours will automatically mute all notifications during the specified time.",
                    color = TextGray,
                    fontSize = 12.sp, lineHeight = 16.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 32.dp, end = 16.dp)
                )
            }
        }
    }
}

@Composable
fun NotificationSectionHeader(text: String) {

    Text(
        text = text,
        color = TextGray,
        fontSize = 12.sp, lineHeight = 16.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun NotificationToggleRow(icon: ImageVector, title: String, subtitle: String, isChecked: Boolean) {

    var checkedState by remember { mutableStateOf(isChecked) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { checkedState = !checkedState }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextGray, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = SettingsColors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, color = TextGray, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Switch(
            modifier = Modifier.scale(0.85f),
            checked = checkedState,
            onCheckedChange = { checkedState = it },
            
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = BlueToggle,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.DarkGray,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}
