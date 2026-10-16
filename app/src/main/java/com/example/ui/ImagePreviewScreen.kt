package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
 
 
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlin.math.roundToInt

fun convertToArabic(num: Int): String {
    val arabicDigits = listOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    return num.toString().map { char ->
        if (char.isDigit()) arabicDigits[char.digitToInt()] else char
    }.joinToString("")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImagePreviewScreen(
    initialMessageId: String,
    messages: List<MessageModel>,
    onDismiss: () -> Unit,
    onSaveToGallery: (String) -> Unit = {},
    onReply: (MessageModel) -> Unit = {},
    onShare: (String) -> Unit = {},
    onDelete: (MessageModel) -> Unit = {}
) {
    // Filter messages that have an IMAGE attachment
    val imageMessages = remember(messages) {
        messages.filter { msg ->
            msg.attachments.any { it.type == AttachmentType.IMAGE }
        }.sortedBy { it.timestamp }
    }

    if (imageMessages.isEmpty()) {
        onDismiss()
        return
    }

    val initialIndex = remember {
        val index = imageMessages.indexOfFirst { it.id == initialMessageId }
        if (index >= 0) index else 0
    }

    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { imageMessages.size })
    var isMenuOpen by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val currentMsg = imageMessages.getOrNull(pagerState.currentPage)
            val currentImageUrl = currentMsg?.attachments?.firstOrNull { it.type == AttachmentType.IMAGE }?.url ?: ""

            // Main Content Area (Image Canvas with Swiper)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val msg = imageMessages[page]
                val imgUrl = msg.attachments.firstOrNull { it.type == AttachmentType.IMAGE }?.url ?: ""
                
                var scale by remember { mutableStateOf(1f) }
                var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 3f)
                                if (scale > 1f) {
                                    offset += pan
                                } else {
                                    offset = androidx.compose.ui.geometry.Offset.Zero
                                }
                            }
                        }
                ) {
                    AsyncImage(
                        model = imgUrl,
                        contentDescription = "Preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                    )
                }
            }

            // Header Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.8f), Color.Black.copy(alpha = 0.4f), Color.Transparent)))
                    .padding(horizontal = 12.dp, vertical = 16.dp)
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = currentMsg?.senderName?.ifEmpty { "Badji Abde L Hani" } ?: "Badji Abde L Hani",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "اليوم عند ${currentMsg?.time ?: "18:27"}",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { /* Magic Wand Action */ }) {
                            Icon(Icons.Filled.AutoFixHigh, contentDescription = "Edit", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { /* Forward Action */ }) {
                            Icon(Icons.Filled.ArrowForward, contentDescription = "Forward", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        IconButton(
                            onClick = { isMenuOpen = !isMenuOpen },
                            modifier = Modifier.background(if (isMenuOpen) Color.White.copy(alpha = 0.1f) else Color.Transparent, RoundedCornerShape(50))
                        ) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Menu", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // Photo Counter
            val currentArabic = convertToArabic(118 + pagerState.currentPage)
            val totalArabic = convertToArabic(118 + imageMessages.size - 1)
            val counterText = "$currentArabic من $totalArabic"

            Text(
                text = counterText,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
            )

            // Overlays & Interactive Menus
            if (isMenuOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { isMenuOpen = false }
                        )
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 60.dp, end = 12.dp)
                        .background(Color(0xFF272727), RoundedCornerShape(12.dp))
                        .padding(vertical = 4.dp)
                        .width(IntrinsicSize.Max)
                ) {
                    Column {
                        MenuItem(
                            icon = Icons.Filled.Visibility,
                            text = "Afficher dans l'échange",
                            onClick = { isMenuOpen = false; onDismiss() }
                        )
                        MenuItem(
                            icon = Icons.Filled.Download,
                            text = "حفظ في المعرض",
                            onClick = { 
                                isMenuOpen = false
                                onSaveToGallery(currentImageUrl)
                            }
                        )
                        MenuItem(
                            icon = Icons.Filled.Reply,
                            text = "Répondre",
                            onClick = { 
                                isMenuOpen = false
                                currentMsg?.let { onReply(it) }
                                onDismiss()
                            }
                        )
                        MenuItem(
                            icon = Icons.Filled.Share,
                            text = "Partager",
                            onClick = { 
                                isMenuOpen = false
                                onShare(currentImageUrl)
                            }
                        )
                        MenuItem(
                            icon = Icons.Filled.Delete,
                            text = "Supprimer",
                            onClick = { 
                                isMenuOpen = false
                                currentMsg?.let { onDelete(it) }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = text, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, color = Color.White, fontSize = 13.sp)
    }
}
