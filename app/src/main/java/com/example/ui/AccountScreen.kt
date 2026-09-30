package com.example.ui

import com.example.ui.SettingsColors

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.supabase
import io.github.jan.supabase.auth.auth








@Composable
fun AccountScreen(onBack: () -> Unit, onEditProfileClick: () -> Unit) {
        val textColor = SettingsColors.textPrimary
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("user_profile_prefs", android.content.Context.MODE_PRIVATE) }
    
    var username by remember { mutableStateOf("Loading...") }
    var email by remember { mutableStateOf("Loading...") }
    val currentUser = supabase.auth.currentUserOrNull()
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
    val userId = currentUser.id
            username = prefs.getString("username_$userId", null) ?: currentUser.email?.substringBefore("@") ?: "user"
            email = prefs.getString("email_$userId", null) ?: currentUser.email ?: "unknown"
        }
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
                text = com.example.ui.i18n.LocalTranslation.current.account,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = SettingsColors.textPrimary
            )
        }

        HorizontalDivider(color = SettingsColors.divider, thickness = 1.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Connected Identity
            Text(
                text = com.example.ui.i18n.LocalTranslation.current.connectedIdentity,
                color = SettingsColors.textSecondary,
                fontSize = 12.sp, lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(SettingsColors.cardRadius))
                    .background(SettingsColors.surface)
                    
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Lucide.Mail, contentDescription = null, tint = SettingsColors.textSecondary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(com.example.ui.i18n.LocalTranslation.current.googleAccount, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = SettingsColors.textPrimary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(com.example.ui.i18n.LocalTranslation.current.signedInViaGoogle, fontSize = 12.sp, lineHeight = 16.sp, color = SettingsColors.textSecondary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SettingsColors.greenAccent.copy(alpha = 0.15f).copy(alpha=0.5f))
                        .border(1.dp, SettingsColors.greenAccent.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Lucide.Check, contentDescription = null, tint = SettingsColors.greenAccent, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(com.example.ui.i18n.LocalTranslation.current.connected, color = SettingsColors.greenAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Identity
            Text(
                text = com.example.ui.i18n.LocalTranslation.current.identity,
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Lucide.AtSign, contentDescription = null, tint = SettingsColors.textSecondary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(com.example.ui.i18n.LocalTranslation.current.username, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = SettingsColors.textPrimary, modifier = Modifier.weight(1f))
                    
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { 
                        clipboardManager.setText(AnnotatedString("@$username")) 
                    }) {
                        Text("@$username", fontSize = 12.sp, lineHeight = 16.sp, color = SettingsColors.textSecondary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Lucide.Copy, contentDescription = "Copy", tint = SettingsColors.textSecondary, modifier = Modifier.size(16.dp))
                    }
                }
                
                HorizontalDivider(color = SettingsColors.divider, thickness = 1.dp, modifier = Modifier.padding(start = 56.dp, end = 16.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Lucide.Mail, contentDescription = null, tint = SettingsColors.textSecondary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(com.example.ui.i18n.LocalTranslation.current.email, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = SettingsColors.textPrimary, modifier = Modifier.weight(1f))
                    Text(email, fontSize = 12.sp, lineHeight = 16.sp, color = SettingsColors.textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Actions
            Text(
                text = com.example.ui.i18n.LocalTranslation.current.actions,
                color = SettingsColors.textSecondary,
                fontSize = 12.sp, lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(SettingsColors.cardRadius))
                    .background(SettingsColors.surface)
                    
                    .clickable { onEditProfileClick() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Lucide.User, contentDescription = null, tint = SettingsColors.textSecondary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(com.example.ui.i18n.LocalTranslation.current.editProfile, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = SettingsColors.textPrimary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(com.example.ui.i18n.LocalTranslation.current.editProfileDesc, fontSize = 12.sp, lineHeight = 16.sp, color = SettingsColors.textSecondary)
                }
                Icon(Lucide.ChevronRight, contentDescription = null, tint = SettingsColors.divider, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Danger zone
            Text(
                text = com.example.ui.i18n.LocalTranslation.current.dangerZone,
                color = SettingsColors.textSecondary,
                fontSize = 12.sp, lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(SettingsColors.cardRadius))
                    .background(SettingsColors.surface)
                    
                    .clickable { /* Handle delete account */ }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Lucide.UserMinus, contentDescription = null, tint = SettingsColors.redAccent, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(com.example.ui.i18n.LocalTranslation.current.deleteAccount, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = SettingsColors.redAccent)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(com.example.ui.i18n.LocalTranslation.current.deleteAccountDesc, fontSize = 12.sp, lineHeight = 16.sp, color = SettingsColors.textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
