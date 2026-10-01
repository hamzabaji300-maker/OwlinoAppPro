package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.deleteRed
import kotlinx.coroutines.delay

@Composable
fun MessageContextMenuOverlay(
    message: MessageModel,
    showFullMenu: Boolean = true,
    onDismissRequest: () -> Unit,
    onReactionSelected: (String) -> Unit,
    onReply: () -> Unit = {},
    onEdit: () -> Unit = {},
    onCopy: () -> Unit = {},
    onDelete: () -> Unit = {},
    onForward: () -> Unit = {},
    onSelect: () -> Unit = {},
    onPin: () -> Unit = {},
    onSave: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    bounds: Rect = Rect.Zero
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    val handleDismiss = {
        visible = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { handleDismiss() }
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(250)) + fadeIn(tween(250)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(200)) + fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {} // block clicks
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 10.dp, top = 14.dp)
                ) {
                    // Always show reactions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 💎 💸 📉 📈 🪙 🪎 — إيموجيات Google Noto المتحركة (تُجلب من الرابط)
                        val reactions = listOf(
                            "\uD83D\uDC8E", "\uD83D\uDCB8", "\uD83D\uDCC9",
                            "\uD83D\uDCC8", "\uD83E\uDE99", "\uD83E\uDE8E"
                        )
                        reactions.forEach { reaction ->
                            val reactionUrl = com.example.emoji.NotoEmojiMap.remoteUrlFor(reaction)
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        onReactionSelected(reaction)
                                        handleDismiss()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (reactionUrl != null) {
                                    LottieEmojiReaction(url = reactionUrl, size = 38.dp)
                                }
                            }
                        }
                    }

                    if (showFullMenu) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)))
                        Spacer(modifier = Modifier.height(6.dp))

                        val textColor = MaterialTheme.colorScheme.onSurface
                        val iconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        
                        Row(modifier = Modifier.fillMaxWidth()) {
                           Column(modifier = Modifier.weight(1f)) {
                               MessageMenuItem("Reply", Icons.AutoMirrored.Outlined.Reply, textColor, iconColor, { onReply(); handleDismiss() })
                               MessageMenuItem("Forward", Icons.AutoMirrored.Outlined.ArrowForward, textColor, iconColor, { onForward(); handleDismiss() })
                               MessageMenuItem("Copy", Icons.Outlined.ContentCopy, textColor, iconColor, { onCopy(); handleDismiss() })
                               MessageMenuItem("Select", Icons.Outlined.CheckCircleOutline, textColor, iconColor, { onSelect(); handleDismiss() })
                           }
                           Column(modifier = Modifier.weight(1f)) {
                               if (message.isMine) {
                                   MessageMenuItem("Edit", Icons.Outlined.Edit, textColor, iconColor, { onEdit(); handleDismiss() })
                               }
                               MessageMenuItem(if (message.isPinned) "Unpin" else "Pin", Icons.Outlined.PushPin, textColor, iconColor, { onPin(); handleDismiss() })
                               MessageMenuItem(if (message.isSaved) "Unsave" else "Save", Icons.Outlined.StarBorder, textColor, iconColor, { onSave(); handleDismiss() })
                               MessageMenuItem("Delete", Icons.Outlined.DeleteOutline, deleteRed, deleteRed, { onDelete(); handleDismiss() })
                           }
                        }
                    }
                }
            }
        }
    }

    // Auto dismiss after animation
    LaunchedEffect(visible) {
        if (!visible) {
            delay(200)
            onDismissRequest()
        }
    }
}

@Composable
fun MessageMenuItem(
    text: String,
    icon: ImageVector,
    textColor: Color = Color.Black,
    iconTint: Color = Color.Gray,
    onClick: () -> Unit
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.6f, stiffness = 400f)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = interactionSource, indication = androidx.compose.foundation.LocalIndication.current) { onClick() }
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = iconTint,
            modifier = Modifier.size(20.dp).scale(scale)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}
