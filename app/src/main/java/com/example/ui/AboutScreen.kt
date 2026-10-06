package com.example.ui

import com.example.ui.SettingsColors

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.*

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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








@Composable
fun AboutScreen(onBack: () -> Unit) {
                
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
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBack
                    )
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = "About Cryptvora",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = SettingsColors.textPrimary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // App Icon & Info
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(SettingsColors.blueAccent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.Info,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Cryptvora",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = SettingsColors.textPrimary
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Version ${com.example.BuildConfig.VERSION_NAME} (${com.example.BuildConfig.VERSION_CODE})",
                fontSize = 12.sp, lineHeight = 16.sp,
                color = SettingsColors.textSecondary
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Section Title
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                Text(
                    text = "Legal",
                    fontSize = 12.sp, lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SettingsColors.textSecondary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
            }

            // Section Content (Card)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(SettingsColors.cardRadius))
                    .background(SettingsColors.surface)
            ) {
                AboutItem(
                    icon = Lucide.FileText,
                    title = com.example.ui.i18n.LocalTranslation.current.termsOfService,
                    isLast = false,
                    dividerColor = SettingsColors.divider
                )
                AboutItem(
                    icon = Lucide.ShieldAlert,
                    title = com.example.ui.i18n.LocalTranslation.current.privacyPolicy,
                    isLast = true,
                    dividerColor = SettingsColors.divider
                )
            }
        }
    }
}

@Composable
fun AboutItem(
    icon: ImageVector,
    title: String,
    isLast: Boolean,
    dividerColor: Color
) {
        
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SettingsColors.textSecondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = SettingsColors.textPrimary, modifier = Modifier.weight(1f))
            Icon(
                imageVector = Lucide.ChevronRight,
                contentDescription = null,
                tint = SettingsColors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
        if (!isLast) {
            HorizontalDivider(color = SettingsColors.divider, thickness = 1.dp, modifier = Modifier.padding(start = 56.dp))
        }
    }
}
