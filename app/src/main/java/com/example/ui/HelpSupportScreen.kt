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
fun HelpSupportScreen(onBack: () -> Unit) {
                val bannerBg = Color(0xFFEFF6FF)
    val bannerContent = Color(0xFF2563EB)

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
                text = com.example.ui.i18n.LocalTranslation.current.helpSupport,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = SettingsColors.textPrimary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Banner Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(SettingsColors.cardRadius))
                    .background(bannerBg)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Lucide.CircleHelp,
                        contentDescription = null,
                        tint = bannerContent,
                        modifier = Modifier.size(24.dp).padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Need assistance? Check our FAQ or reach out to our support team for help with Cryptvora.",
                        color = bannerContent,
                        fontSize = 12.sp,
                            lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section Title
            Text(
                text = "Support Options",
                fontSize = 12.sp, lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = SettingsColors.textSecondary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            // Section Content (Card)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(SettingsColors.cardRadius))
                    .background(SettingsColors.surface)
            ) {
                HelpSupportItem(
                    icon = Lucide.CircleHelp,
                    title = "FAQ & Guides",
                    subtitle = null,
                    isLast = false,
                    dividerColor = SettingsColors.divider
                )
                HelpSupportItem(
                    icon = Lucide.MessageSquare,
                    title = "Contact Support",
                    subtitle = null,
                    isLast = false,
                    dividerColor = SettingsColors.divider
                )
                HelpSupportItem(
                    icon = Lucide.Mail,
                    title = "Email Us",
                    subtitle = "support@cryptvora.com",
                    isLast = true,
                    dividerColor = SettingsColors.divider
                )
            }
        }
    }
}

@Composable
fun HelpSupportItem(
    icon: ImageVector,
    title: String,
    subtitle: String?,
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
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = SettingsColors.textPrimary)
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(subtitle, fontSize = 12.sp, lineHeight = 16.sp, color = SettingsColors.textSecondary)
                }
            }
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
