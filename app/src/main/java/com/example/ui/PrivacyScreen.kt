package com.example.ui

import com.example.ui.SettingsColors

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.*

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.example.ui.i18n.LocalTranslation
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns




@kotlinx.serialization.Serializable
data class PrivacyUpdate(
    val id: String? = null,
    @kotlinx.serialization.SerialName("privacy_settings")
    val privacySettings: PrivacySettings
)

@Composable
fun PrivacyScreen(onBack: () -> Unit, onBlockedUsersClick: () -> Unit = {}) {
        val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    var privacySettings by remember { mutableStateOf(PrivacySettings()) }
    var isLoading by remember { mutableStateOf(true) }
    var showProfilePhotoDialog by remember { mutableStateOf(false) }

    // Fetch on mount
    LaunchedEffect(Unit) {
        try {
            val user = supabase.auth.currentUserOrNull()
            if (user != null) {
                val profile = supabase.postgrest["profiles"]
                    .select(Columns.list("id", "privacy_settings")) {
                        filter { eq("id", user.id) }
                    }
                    .decodeSingleOrNull<Profile>()
                
                if (profile?.privacySettings != null) {
                    privacySettings = profile.privacySettings
                }
            }
        } catch (e: Exception) {
                e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    fun updateSettings(newSettings: PrivacySettings) {
        privacySettings = newSettings
        coroutineScope.launch {
            try {
                val user = supabase.auth.currentUserOrNull()
                if (user != null) {
                    supabase.postgrest["profiles"].update(
                        PrivacyUpdate(id = user.id, privacySettings = newSettings)
                    ) {
                        filter { eq("id", user.id) }
                    }
                } else {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        showOwlinoToast("Update FAILED: User is null")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val err = e.message ?: "Unknown error"
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    showOwlinoToast("Update FAILED: $err")
                }
            }
        }
    }

    if (showProfilePhotoDialog) {
        AlertDialog(
            onDismissRequest = { showProfilePhotoDialog = false },
            title = { Text("Profile Photo Visibility", color = SettingsColors.textPrimary) },
            text = {
                Column {
                    listOf("everyone", "contacts", "nobody").forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    updateSettings(privacySettings.copy(profilePhoto = option))
                                    showProfilePhotoDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = privacySettings.profilePhoto == option,
                                onClick = {
                                    updateSettings(privacySettings.copy(profilePhoto = option))
                                    showProfilePhotoDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(option.replaceFirstChar { it.uppercase() }, color = SettingsColors.textPrimary)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProfilePhotoDialog = false }) {
                    Text("Cancel", color = SettingsColors.blueAccent)
                }
            },
            containerColor = SettingsColors.surface
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(SettingsColors.background)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Lucide.ArrowLeft,
                contentDescription = "Back",
                tint = SettingsColors.textPrimary,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onBack() }
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = com.example.ui.i18n.LocalTranslation.current.privacy,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = SettingsColors.textPrimary
            )
        }
        HorizontalDivider(color = SettingsColors.divider, thickness = 1.dp)

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SettingsColors.blueAccent)
            }
            return
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Visibility
            Text(
                text = "Visibility",
                color = SettingsColors.textSecondary,
                fontSize = 12.sp, lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(SettingsColors.cardRadius))
                    .background(SettingsColors.surface)
                                
            ) {
                PrivacyItemValue(
                    icon = Lucide.User,
                    title = "Profile photo",
                    value = privacySettings.profilePhoto.replaceFirstChar { it.uppercase() },
                    iconTint = SettingsColors.textSecondary,
                    onClick = { showProfilePhotoDialog = true }
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Activity
            Text(
                text = "Activity",
                color = SettingsColors.textSecondary,
                fontSize = 12.sp, lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(SettingsColors.cardRadius))
                    .background(SettingsColors.surface)
                                
            ) {
                PrivacyItemToggle(
                    icon = Lucide.Eye,
                    title = "Read receipts",
                    subtitle = "Show when you've read a message",
                    isChecked = privacySettings.readReceipts,
                    onCheckedChange = { updateSettings(privacySettings.copy(readReceipts = it)) },
                    iconTint = SettingsColors.textSecondary
                )
                HorizontalDivider(color = SettingsColors.divider, thickness = 1.dp, modifier = Modifier.padding(start = 56.dp, end = 16.dp))
                PrivacyItemToggle(
                    icon = Lucide.EyeOff,
                    title = "Last seen",
                    subtitle = "Share when you were last online",
                    isChecked = privacySettings.lastSeen == "everyone",
                    onCheckedChange = { 
                        updateSettings(privacySettings.copy(lastSeen = if(it) "everyone" else "nobody")) 
                    },
                    iconTint = SettingsColors.textSecondary
                )
                HorizontalDivider(color = SettingsColors.divider, thickness = 1.dp, modifier = Modifier.padding(start = 56.dp, end = 16.dp))
                PrivacyItemToggle(
                    icon = Lucide.MessageSquare,
                    title = "Typing indicator",
                    subtitle = "Signal while you're typing",
                    isChecked = privacySettings.typingIndicator,
                    onCheckedChange = { updateSettings(privacySettings.copy(typingIndicator = it)) },
                    iconTint = SettingsColors.textSecondary
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Chat security
            Text(
                text = "Chat security",
                color = SettingsColors.textSecondary,
                fontSize = 12.sp, lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(SettingsColors.cardRadius))
                    .background(SettingsColors.surface)
                                
            ) {
                PrivacyItemToggle(
                    icon = Lucide.CalendarClock,
                    title = "Disappearing messages",
                    subtitle = "Automatically delete new messages",
                    isChecked = privacySettings.disappearingMessages,
                    onCheckedChange = { updateSettings(privacySettings.copy(disappearingMessages = it)) },
                    iconTint = SettingsColors.textSecondary
                )
                HorizontalDivider(color = SettingsColors.divider, thickness = 1.dp, modifier = Modifier.padding(start = 56.dp, end = 16.dp))
                PrivacyItemToggle(
                    icon = Lucide.Camera,
                    title = "Screenshot protection",
                    subtitle = "Prevent screenshots inside the app",
                    isChecked = privacySettings.screenshotProtection,
                    onCheckedChange = { updateSettings(privacySettings.copy(screenshotProtection = it)) },
                    iconTint = SettingsColors.textSecondary
                )
                HorizontalDivider(color = SettingsColors.divider, thickness = 1.dp, modifier = Modifier.padding(start = 56.dp, end = 16.dp))
                PrivacyItemAction(
                    icon = Lucide.Ban,
                    title = "Manage blocked users",
                    iconTint = SettingsColors.textSecondary,
                    onClick = onBlockedUsersClick
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun PrivacyItemToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    iconTint: Color
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val t = com.example.ui.i18n.LocalTranslation.current
    val handleChange: (Boolean) -> Unit = { checked ->
        onCheckedChange(checked)
        showToggleToast(context, title, checked, t, icon)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { handleChange(!isChecked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = SettingsColors.textPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, fontSize = 12.sp, lineHeight = 16.sp, color = SettingsColors.textSecondary)
        }
        Switch(
            modifier = Modifier.scale(0.85f),
            checked = isChecked,
            onCheckedChange = handleChange,
            
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = SettingsColors.blueAccent,
                uncheckedThumbColor = SettingsColors.textSecondary,
                uncheckedTrackColor = SettingsColors.divider,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
fun PrivacyItemValue(
    icon: ImageVector,
    title: String,
    value: String,
    iconTint: Color,
    onClick: () -> Unit
) {
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = SettingsColors.textPrimary, modifier = Modifier.weight(1f))
        Text(value, fontSize = 12.sp, lineHeight = 16.sp, color = SettingsColors.textSecondary)
    }
}

@Composable
fun PrivacyItemAction(
    icon: ImageVector,
    title: String,
    iconTint: Color,
    onClick: () -> Unit
) {
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = SettingsColors.textPrimary, modifier = Modifier.weight(1f))
        Icon(Lucide.ChevronRight, contentDescription = null, tint = SettingsColors.divider, modifier = Modifier.size(20.dp))
    }
}
