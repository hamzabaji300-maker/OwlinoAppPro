@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.composables.icons.lucide.*
import com.composables.icons.lucide.Lucide
import com.example.ui.i18n.LocalTranslation
import com.example.ui.theme.*
import kotlin.math.roundToInt
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

// ===== مقتطفات حرفية من ChatDetailScreen الأصلية: ما يظهر/يحدث عند الضغط المطوّل =====

enum class ToastType { PIN, UNPIN, COPY, SAVE, UNSAVE, INFO }
data class ToastNotification(val id: Long, val text: String, val type: ToastType)

@Composable
fun SelectionTopBar(
    selectedCount: Int = 0, count: Int = selectedCount,
    onClearSelection: () -> Unit = {},
    onClear: () -> Unit = onClearSelection,
    onCopy: () -> Unit = {},
    onForward: () -> Unit = {},
    onDelete: () -> Unit = {},
    onPin: () -> Unit = {}
) {
    TopAppBar(
        title = { Text("$selectedCount") },
        navigationIcon = {
            IconButton(onClick = onClearSelection) {
                Icon(androidx.compose.material.icons.Icons.Default.Close, contentDescription = "Clear")
            }
        },
        actions = {
            IconButton(onClick = onCopy) { Icon(androidx.compose.material.icons.Icons.Default.ContentCopy, "Copy") }
            IconButton(onClick = onDelete) { Icon(androidx.compose.material.icons.Icons.Default.Delete, "Delete") }
        }
    )
}


fun PinnedMessageBar(
    message: MessageModel,
    onUnpin: () -> Unit = {},
    onClick: () -> Unit = {},
    count: Int = 1,
    currentIndex: Int = 0
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .fillMaxWidth()
            .scale(scale)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.05f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Indicator lines
            Row(
                modifier = Modifier
                    .width(3.dp)
                    .height(24.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    for (i in 0 until count) {
                        val isActive = i == currentIndex
                        val color by androidx.compose.animation.animateColorAsState(
                            targetValue = if (isActive) Color(0xFF34C759) else Color(0xFF34C759).copy(alpha = 0.3f),
                            animationSpec = tween(300)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Pinned Message" + if (count > 1) " #${currentIndex + 1}" else "",
                        fontSize = 14.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        color = Color(0xFF34C759),
                        maxLines = 1,
                        lineHeight = 16.sp
                    )
                }
                Text(
                    text = message.text.ifEmpty { "Attachment" },
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Pin icon
            Icon(
                imageVector = Icons.Filled.PushPin,
                contentDescription = "Pinned",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = 45f }
            )
        }
    }
}

// ===== حوارات تأكيد الحظر/فك الحظر — مفصولة عن ChatDetailScreen عمداً =====
// ChatDetailScreen ضخمة بزاف، وإلا زدنا فيها كود كتوصل لحد كيولّد فيه R8 كود غالط
// (VerifyError: copy1 ... type=Reference: Composer). فصل هاد الأجزاء كيخفف عدد الـ registers.

@Composable
fun LabToastHost(toastNotification: ToastNotification?, onClear: () -> Unit) {
    val __theme = LocalSettingsTheme.current.theme
       LaunchedEffect(toastNotification) {
          if (toastNotification != null) {
              kotlinx.coroutines.delay(3000)
              onClear()
          }
        }
        Box(modifier = Modifier.fillMaxSize().padding(bottom = 72.dp), contentAlignment = Alignment.BottomCenter) {
            AnimatedVisibility(
                visible = toastNotification != null,
                enter = slideInVertically(
                    initialOffsetY = { 100 },
                    animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f)
                ) + fadeIn(tween(200)) + scaleIn(initialScale = 0.9f, animationSpec = tween(200)),
                exit = slideOutVertically(
                    targetOffsetY = { 50 },
                    animationSpec = tween(150)
                ) + fadeOut(tween(150)) + scaleOut(targetScale = 0.95f, animationSpec = tween(150)),
                modifier = Modifier
                    .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth()
            ) {
                toastNotification?.let { toast ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentWidth(Alignment.CenterHorizontally)
                            .shadow(
                                elevation = 8.dp,
                                shape = CircleShape,
                                ambientColor = Color(0x14000000),
                                spotColor = Color(0x14000000)
                            )
                            .clip(CircleShape)
                            .background(__theme.surfaceColor.copy(alpha = 0.97f))
                            .border(1.dp, (if (__theme.isDark) Color.White else Color.Black).copy(alpha = 0.08f), CircleShape)
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .widthIn(max = 360.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = toast.text,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = __theme.textPrimary,
                                letterSpacing = (-0.5).sp
                            )
                            
                            Spacer(modifier = Modifier.width(16.dp))

                            // Icon logic based on toast.type
                            when (toast.type) {
                                ToastType.COPY -> {
                                    val scaleAnim = remember { androidx.compose.animation.core.Animatable(0.5f) }
                                    val rotateAnim = remember { androidx.compose.animation.core.Animatable(0f) }
                                    val alphaAnim = remember { androidx.compose.animation.core.Animatable(0f) }
                                    
                                    LaunchedEffect(toast.id) {
                                        kotlinx.coroutines.delay(100)
                                        launch { alphaAnim.animateTo(1f, tween(100)) }
                                        launch {
                                            scaleAnim.animateTo(1.3f, tween(150, easing = LinearOutSlowInEasing))
                                            scaleAnim.animateTo(0.9f, tween(150, easing = FastOutSlowInEasing))
                                            scaleAnim.animateTo(1f, tween(150, easing = FastOutLinearInEasing))
                                        }
                                        launch {
                                            rotateAnim.animateTo(-10f, tween(150))
                                            rotateAnim.animateTo(5f, tween(150))
                                            rotateAnim.animateTo(0f, tween(150))
                                        }
                                    }
                                    
                                    Icon(
                                        imageVector = Icons.Outlined.ContentCopy,
                                        contentDescription = null,
                                        tint = Color(0xFF4B5563),
                                        modifier = Modifier
                                            .size(18.dp)
                                            .graphicsLayer {
                                                scaleX = scaleAnim.value
                                                scaleY = scaleAnim.value
                                                rotationZ = rotateAnim.value
                                                alpha = alphaAnim.value
                                            }
                                    )
                                }
                                ToastType.SAVE, ToastType.PIN -> {
                                    val scaleAnim = remember { androidx.compose.animation.core.Animatable(0f) }
                                    val rotateAnim = remember { androidx.compose.animation.core.Animatable(-180f) }
                                    val alphaAnim = remember { androidx.compose.animation.core.Animatable(0f) }
                                    
                                    LaunchedEffect(toast.id) {
                                        kotlinx.coroutines.delay(100)
                                        launch { alphaAnim.animateTo(1f, tween(100)) }
                                        launch { scaleAnim.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 300f)) }
                                        launch { rotateAnim.animateTo(if (toast.type == ToastType.PIN) 45f else 0f, spring(dampingRatio = 0.6f, stiffness = 300f)) }
                                    }
                                    
                                    Icon(
                                        imageVector = if (toast.type == ToastType.PIN) Icons.Filled.PushPin else Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = if (toast.type == ToastType.PIN) Color(0xFF6B7280) else Color(0xFFF5C518),
                                        modifier = Modifier
                                            .size(18.dp)
                                            .graphicsLayer {
                                                scaleX = scaleAnim.value
                                                scaleY = scaleAnim.value
                                                rotationZ = rotateAnim.value
                                                alpha = alphaAnim.value
                                            }
                                    )
                                }
                                ToastType.UNSAVE, ToastType.UNPIN -> {
                                    val scaleAnim = remember { androidx.compose.animation.core.Animatable(0f) }
                                    val rotateAnim = remember { androidx.compose.animation.core.Animatable(180f) }
                                    val alphaAnim = remember { androidx.compose.animation.core.Animatable(0f) }
                                    
                                    LaunchedEffect(toast.id) {
                                        kotlinx.coroutines.delay(100)
                                        launch { alphaAnim.animateTo(1f, tween(100)) }
                                        launch { scaleAnim.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 300f)) }
                                        launch { rotateAnim.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 300f)) }
                                    }
                                    
                                    Icon(
                                        imageVector = if (toast.type == ToastType.UNPIN) Icons.Outlined.PushPin else Icons.Outlined.StarBorder,
                                        contentDescription = null,
                                        tint = Color(0xFF6B7280),
                                        modifier = Modifier
                                            .size(18.dp)
                                            .graphicsLayer {
                                                scaleX = scaleAnim.value
                                                scaleY = scaleAnim.value
                                                rotationZ = rotateAnim.value
                                                alpha = alphaAnim.value
                                            }
                                    )
                                }
                                ToastType.INFO -> {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = null,
                                        tint = Color(0xFF4B5563),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
}

@Composable
fun LabForwardDialog(primaryColor: Color, targets: List<String>, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        val theme = LocalSettingsTheme.current.theme
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = theme.surfaceColor,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    LocalTranslation.current.forwardTo,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.textPrimary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                targets.forEachIndexed { i, contact ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onPick(i) }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(primaryColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) { Text(contact.take(1), color = primaryColor, fontWeight = FontWeight.Bold) }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(contact, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = theme.textPrimary)
                    }
                }
            }
        }
    }
}
