package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.deleteRed
import kotlinx.coroutines.delay

@Composable
fun ChatContextMenu(modifier: Modifier = Modifier, onDismissRequest: () -> Unit) {
    var isMoreExpanded by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .width(250.dp)
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .background(com.example.ui.SettingsColors.surface, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
    ) {
            AnimatedContent(
                targetState = isMoreExpanded,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally(initialOffsetX = { it }) + fadeIn(tween(150))).togetherWith(
                            slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(tween(150))
                        )
                    } else {
                        (slideInHorizontally(initialOffsetX = { -it }) + fadeIn(tween(150))).togetherWith(
                            slideOutHorizontally(targetOffsetX = { it }) + fadeOut(tween(150))
                        )
                    }.using(SizeTransform(clip = false))
                },
                label = "MenuTransition"
            ) { expanded ->
                if (expanded) {
                    Column(modifier = Modifier.background(com.example.ui.SettingsColors.surface).padding(vertical = 4.dp)) {
                        ChatContextMenuItem(
                            text = "Back",
                            icon = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                            onClick = { isMoreExpanded = false }
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant))
                        Spacer(modifier = Modifier.height(2.dp))
                        ChatContextMenuItem("Mute", Icons.Outlined.NotificationsOff, onClick = onDismissRequest)
                        ChatContextMenuItem("Select Messages", Icons.Outlined.CheckCircleOutline, onClick = onDismissRequest)
                        ChatContextMenuItem("Report", Icons.Outlined.OutlinedFlag, onClick = onDismissRequest)
                        ChatContextMenuItem("Block User", Icons.Outlined.WarningAmber, iconTint = Color(0xFFFF9800), textColor = Color(0xFFFF9800), onClick = onDismissRequest)
                        ChatContextMenuItem("Clear History", Icons.Outlined.LayersClear, onClick = onDismissRequest)
                    }
                } else {
                    Column(modifier = Modifier.background(com.example.ui.SettingsColors.surface).padding(vertical = 4.dp)) {
                        ChatContextMenuItem("Pin to Top", Icons.Outlined.PushPin, onClick = onDismissRequest)
                        ChatContextMenuItem("Archive Chat", Icons.Outlined.Archive, onClick = onDismissRequest)
                        ChatContextMenuItem("Mark as Read", Icons.Outlined.CheckCircleOutline, onClick = onDismissRequest)
                        ChatContextMenuItem("Add to Favorites", Icons.Outlined.StarBorder, onClick = onDismissRequest)
                        ChatContextMenuItem("View Profile", Icons.Outlined.PersonOutline, onClick = onDismissRequest)
                        ChatContextMenuItem("Share Contact", Icons.Outlined.Share, onClick = onDismissRequest)
                        ChatContextMenuItem(
                            text = "More",
                            icon = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            onClick = { isMoreExpanded = true }
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant))
                        Spacer(modifier = Modifier.height(2.dp))
                        ChatContextMenuItem(
                            text = "Delete Conversation",
                            icon = Icons.Outlined.DeleteOutline,
                            iconTint = androidx.compose.material3.MaterialTheme.colorScheme.error,
                            textColor = androidx.compose.material3.MaterialTheme.colorScheme.error,
                            onClick = onDismissRequest
                        )
                    }
                }
            }
        }
    }

@Composable
fun ChatContextMenuItem(
    text: String,
    icon: ImageVector,
    iconTint: Color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
    textColor: Color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
    trailingIcon: ImageVector? = null,
    trailingIconTint: Color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(com.example.ui.SettingsColors.surface)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = text,
            fontSize = 15.sp,
            color = textColor,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        if (trailingIcon != null) {
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = trailingIconTint,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
