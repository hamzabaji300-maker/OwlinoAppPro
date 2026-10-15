package com.example.ui

import android.app.Activity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage



// ============================================================
// 1. مكوّن عنصر القائمة MenuItem
// ============================================================
@Composable
private fun MenuItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1
        )
    }
}

// ============================================================
// 2. مكوّن رسمة القط CatArt
// ============================================================
@Composable
private fun CatArt(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF03D758)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                val bodyLeft = w * 0.075f
                val bodyTop = h * 0.20f
                val bodyWidth = w * 0.85f
                val bodyHeight = h * 0.80f + h * 0.15f
                drawRoundRect(
                    color = Color(0xFF080808),
                    topLeft = Offset(bodyLeft, bodyTop),
                    size = androidx.compose.ui.geometry.Size(bodyWidth, bodyHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(bodyWidth * 0.45f, bodyWidth * 0.45f)
                )

                rotate(degrees = -25f, pivot = Offset(w * (0.02f + 0.175f), h * (-0.12f + 0.175f))) {
                    val earLeftPath = Path().apply {
                        val left = w * 0.02f
                        val top = h * -0.12f
                        val size = w * 0.35f
                        moveTo(left + size / 2f, top)
                        lineTo(left, top + size)
                        lineTo(left + size, top + size)
                        close()
                    }
                    drawPath(earLeftPath, color = Color(0xFF080808))
                }

                rotate(degrees = 20f, pivot = Offset(w * (0.98f + 0.2f), h * (-0.15f + 0.2f))) {
                    val earRightPath = Path().apply {
                        val left = w * (1f - 0.02f - 0.40f)
                        val top = h * -0.15f
                        val size = w * 0.40f
                        moveTo(left + size / 2f, top)
                        lineTo(left, top + size)
                        lineTo(left + size, top + size)
                        close()
                    }
                    drawPath(earRightPath, color = Color(0xFF080808))
                }

                rotate(degrees = -12f, pivot = Offset(w * (0.10f + 0.15f), h * (0.28f + 0.09f))) {
                    val leftEye = Path().apply {
                        val l = w * 0.10f
                        val t = h * 0.28f
                        val ew = w * 0.30f
                        val eh = h * 0.18f
                        moveTo(l, t + eh * 0.40f)
                        lineTo(l + ew, t)
                        lineTo(l + ew, t + eh)
                        lineTo(l, t + eh)
                        close()
                    }
                    drawPath(leftEye, color = Color.White)
                }

                rotate(degrees = 15f, pivot = Offset(w * (1f - 0.10f - 0.19f), h * (0.20f + 0.12f))) {
                    val rightEye = Path().apply {
                        val l = w * (1f - 0.10f - 0.38f)
                        val t = h * 0.20f
                        val ew = w * 0.38f
                        val eh = h * 0.24f
                        moveTo(l, t)
                        lineTo(l + ew, t + eh * 0.30f)
                        lineTo(l + ew, t + eh)
                        lineTo(l, t + eh)
                        close()
                    }
                    drawPath(rightEye, color = Color.White)
                }

                rotate(degrees = -15f, pivot = Offset(w * (1f - 0.32f - 0.02f), h * (0.52f + 0.03f))) {
                    val fang = Path().apply {
                        val l = w * (1f - 0.32f - 0.04f)
                        val t = h * 0.52f
                        val fw = w * 0.04f
                        val fh = h * 0.06f
                        moveTo(l, t)
                        lineTo(l + fw, t)
                        lineTo(l + fw / 2f, t + fh)
                        close()
                    }
                    drawPath(fang, color = Color.White)
                }

                val whiskerColor = Color(0xFF1A1A1A)
                drawWhisker(w, h, topPct = 0.55f, leftPct = -0.15f, widthPct = 0.25f, angle = -10f, color = whiskerColor)
                drawWhisker(w, h, topPct = 0.62f, leftPct = -0.12f, widthPct = 0.25f, angle = 5f, color = whiskerColor)
                drawWhisker(w, h, topPct = 0.69f, leftPct = -0.08f, widthPct = 0.22f, angle = 20f, color = whiskerColor)
                drawWhiskerRight(w, h, topPct = 0.48f, rightPct = -0.15f, widthPct = 0.30f, angle = 10f, color = whiskerColor)
                drawWhiskerRight(w, h, topPct = 0.55f, rightPct = -0.12f, widthPct = 0.28f, angle = 0f, color = whiskerColor)
                drawWhiskerRight(w, h, topPct = 0.62f, rightPct = -0.08f, widthPct = 0.25f, angle = -10f, color = whiskerColor)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWhisker(
    w: Float, h: Float, topPct: Float, leftPct: Float, widthPct: Float, angle: Float, color: Color
) {
    val startX = w * leftPct
    val y = h * topPct
    rotate(degrees = angle, pivot = Offset(startX + (w * widthPct) / 2f, y)) {
        drawLine(color = color, start = Offset(startX, y), end = Offset(startX + w * widthPct, y), strokeWidth = h * 0.015f, cap = StrokeCap.Round)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWhiskerRight(
    w: Float, h: Float, topPct: Float, rightPct: Float, widthPct: Float, angle: Float, color: Color
) {
    val startX = w * (1f + rightPct)
    val y = h * topPct
    rotate(degrees = angle, pivot = Offset(startX - (w * widthPct) / 2f, y)) {
        drawLine(color = color, start = Offset(startX, y), end = Offset(startX - w * widthPct, y), strokeWidth = h * 0.015f, cap = StrokeCap.Round)
    }
}

// ============================================================
// 3. الشاشة الرئيسية ImageViewerScreen
// ============================================================

sealed class ViewerSlide {
    object Cat : ViewerSlide()
    data class Remote(val messageId: String, val url: String) : ViewerSlide()
}

@Composable
fun ImageViewerScreen(
    imageMessages: List<MessageModel>,
    initialIndex: Int,
    onBack: (MessageModel?) -> Unit = {},
    onShowInChat: (MessageModel) -> Unit = {},
    onSaveToGallery: (MessageModel) -> Unit = {},
    onReply: (MessageModel) -> Unit = {},
    onShare: (MessageModel) -> Unit = {},
    onDelete: (MessageModel) -> Unit = {}
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) } // التحكم في إظهار وإخفاء الهيدر وشريط الحالة
    
    val safeInitialIndex = if(initialIndex >= 0 && initialIndex < imageMessages.size) initialIndex else 0
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(initialPage = safeInitialIndex) { 
        maxOf(1, imageMessages.size) 
    }
    val currentIndex = pagerState.settledPage
    
    val currentMsg = imageMessages.getOrNull(currentIndex)
    
    BackHandler(enabled = true) {
        onBack(imageMessages.getOrNull(pagerState.settledPage))
    }
    
    val view = LocalView.current
    val context = LocalContext.current

    // إدارة لون أيقونات شريط حالة النظام (بيضاء دائماً)
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, view) }
        val originalLightStatusBars = insetsController?.isAppearanceLightStatusBars ?: true
        
        insetsController?.isAppearanceLightStatusBars = false // إجبار الأيقونات على اللون الأبيض
        
        onDispose {
            insetsController?.isAppearanceLightStatusBars = originalLightStatusBars
            insetsController?.show(WindowInsetsCompat.Type.statusBars())
        }
    }

    // إدارة إخفاء/إظهار شريط الحالة بناءً على التفاعل
    LaunchedEffect(showControls) {
        val window = (context as? Activity)?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, view) }
        if (showControls) {
            insetsController?.show(WindowInsetsCompat.Type.statusBars())
        } else {
            insetsController?.hide(WindowInsetsCompat.Type.statusBars())
            isMenuOpen = false
        }
    }

    val slides = remember(imageMessages) {
        if (imageMessages.isEmpty()) listOf(ViewerSlide.Cat)
        else imageMessages.map { msg ->
            val url = msg.attachments.firstOrNull { it.type == AttachmentType.IMAGE }?.url ?: ""
            ViewerSlide.Remote(msg.attachments.firstOrNull { it.type == com.example.ui.AttachmentType.IMAGE }?.messageId ?: msg.id, url)
        }
    }

    fun convertToArabic(num: Int): String {
        val arabicDigits = "٠١٢٣٤٥٦٧٨٩"
        return num.toString().map { ch -> if (ch.isDigit()) arabicDigits[ch - '0'] else ch }.joinToString("")
    }
    val senderName = currentMsg?.senderName?.ifEmpty { "Badji Abde L Hani" } ?: "Badji Abde L Hani"
    val dateTimeLabel = currentMsg?.let { "اليوم عند ${it.time}" } ?: "اليوم عند 18:27"

    val currentArabic = convertToArabic(currentIndex + 1)
    val totalArabic = convertToArabic(slides.size)
    val counterText = "$currentArabic من $totalArabic"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // منطقة المحتوى الرئيسية
        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            flingBehavior = androidx.compose.foundation.pager.PagerDefaults.flingBehavior(
                state = pagerState,
                snapAnimationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
            ),
            modifier = Modifier.fillMaxSize()
        ) { page ->
            var scale by remember { mutableStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.5f
                                }
                            },
                            onTap = { showControls = !showControls }
                        )
                    }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown()
                            do {
                                val event = awaitPointerEvent()
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()
                                
                                val newScale = (scale * zoom).coerceIn(1f, 5f)
                                
                                if (newScale > 1f) {
                                    scale = newScale
                                    val maxX = (size.width * (scale - 1)) / 2f
                                    val maxY = (size.height * (scale - 1)) / 2f
                                    offset = Offset(
                                        x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                                        y = (offset.y + pan.y).coerceIn(-maxY, maxY)
                                    )
                                    if (zoom != 1f || pan != Offset.Zero) {
                                        event.changes.forEach { 
                                            if (it.positionChanged()) {
                                                it.consume()
                                            }
                                        }
                                    }
                                } else {
                                    scale = 1f
                                    offset = Offset.Zero
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        },
                    contentAlignment = Alignment.Center
                ) {
                    when (val slide = slides.getOrNull(page)) {
                        is ViewerSlide.Cat -> CatArt(modifier = Modifier.fillMaxSize())
                        is ViewerSlide.Remote -> AsyncImage(
                            model = coil.request.ImageRequest.Builder(context)
                                .data(rememberMediaSource(context, slide.messageId, slide.url))
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                        null -> {}
                    }
                }
            }
        }

        // الشريط العلوي الشفاف (Header)
        AnimatedVisibility(
            visible = showControls,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = 0.80f),
                            0.5f to Color.Black.copy(alpha = 0.40f),
                            1.0f to Color.Transparent
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 16.dp)
                    .windowInsetsPadding(WindowInsets.statusBars),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onBack(imageMessages.getOrNull(pagerState.settledPage)) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Column {
                        Text(senderName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                        Text(dateTimeLabel, color = Color.White.copy(alpha = 0.70f), fontSize = 11.sp, maxLines = 1, modifier = Modifier.padding(top = 1.dp))
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconOnlyButton(icon = Icons.Filled.AutoFixHigh, contentDescription = "تعديل")
                    IconOnlyButton(icon = Icons.Filled.SubdirectoryArrowRight, contentDescription = "تحويل")
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isMenuOpen) Color.White.copy(alpha = 0.10f) else Color.Transparent)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { isMenuOpen = !isMenuOpen }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "المزيد", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // عداد الصور
        AnimatedVisibility(
            visible = showControls,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 80.dp)
        ) {
            Text(
                text = counterText,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // القائمة المنسدلة السوداء
        if (isMenuOpen && showControls) {
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
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 60.dp, end = 12.dp)
                    .width(IntrinsicSize.Max)
                    .background(Color(0xFF272727), RoundedCornerShape(12.dp))
                    .padding(vertical = 4.dp)
            ) {
                Column {
                    MenuItem(Icons.Filled.RemoveRedEye, "Afficher dans l'échange") {
                        isMenuOpen = false
                        currentMsg?.let { onShowInChat(it) }
                    }
                    MenuItem(Icons.Filled.Download, "حفظ في المعرض") {
                        isMenuOpen = false
                        currentMsg?.let { onSaveToGallery(it) }
                    }
                    MenuItem(Icons.Filled.Reply, "Répondre") {
                        isMenuOpen = false
                        currentMsg?.let { onReply(it) }
                    }
                    MenuItem(Icons.Filled.Share, "Partager") {
                        isMenuOpen = false
                        currentMsg?.let { onShare(it) }
                    }
                    MenuItem(Icons.Filled.Delete, "Supprimer") {
                        isMenuOpen = false
                        currentMsg?.let { onDelete(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun IconOnlyButton(icon: ImageVector, contentDescription: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}
