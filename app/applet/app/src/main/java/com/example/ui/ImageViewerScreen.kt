package com.example.ui

/**
 * ترجمة حرفية لملف App.tsx الأصلي (React + Tailwind + Framer Motion)
 * إلى Jetpack Compose، بنفس البنية والعناصر تماماً:
 *  - MenuItem: عنصر واحد داخل القائمة المنسدلة السوداء
 *  - CatArt: رسمة القط المرسومة بالكامل بأشكال هندسية (بدون صورة)
 *  - ImageViewerScreen: الشاشة الرئيسية (الهيدر الشفاف، عداد الصور،
 *    منطقة السحب بين الصور، القائمة المنسدلة عند الضغط على النقاط الثلاث)
 *
 * ملاحظة: هذا الملف يطابق الصورة التجريبية (Demo) كما هي في App.tsx
 * الأصلي (نفس الاسم "Badji Abde L Hani"، نفس العداد يبدأ من 118،
 * ونفس 3 شرائح: القط + صورتين). أي ربط ببيانات حقيقية من التطبيق
 * (قائمة صور المحادثة الفعلية، الحفظ الفعلي، المشاركة الفعلية...)
 * يُترك عمداً لتعديل لاحق، تماماً كما طلب المستخدم عدم تغيير أي شيء
 * في هذه الترجمة.
 */

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh // بديل Wand2
import androidx.compose.material.icons.filled.Delete // Trash2
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RemoveRedEye // Eye
import androidx.compose.material.icons.filled.Reply // CornerUpLeft
import androidx.compose.material.icons.filled.Share // Share2
import androidx.compose.material.icons.filled.SubdirectoryArrowRight // بديل CornerUpRight
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
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlin.math.abs
import kotlin.math.roundToInt

// ============================================================
// 1. مكوّن عنصر القائمة MenuItem (يقابل الأسطر 18-27 في App.tsx)
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
// 2. مكوّن رسمة القط CatArt (يقابل الأسطر 29-79 في App.tsx)
//    مرسومة بالكامل عبر Canvas، مطابقة للنسب المئوية الأصلية
//    (clip-path polygons) بدون استخدام أي صورة خارجية.
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

                // جسم القط (مستطيل بأعلى مقوس تقريباً)
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

                // الأذن اليسرى (مثلث مائل -25 درجة)
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

                // الأذن اليمنى (مثلث مائل +20 درجة)
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

                // العين اليسرى (غاضبة) - شكل شبه منحرف أبيض مائل -12 درجة
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

                // العين اليمنى (غاضبة) - مائلة +15 درجة
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

                // الناب الصغير
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

                // الشوارب (يسار)
                val whiskerColor = Color(0xFF1A1A1A)
                drawWhisker(w, h, topPct = 0.55f, leftPct = -0.15f, widthPct = 0.25f, angle = -10f, color = whiskerColor)
                drawWhisker(w, h, topPct = 0.62f, leftPct = -0.12f, widthPct = 0.25f, angle = 5f, color = whiskerColor)
                drawWhisker(w, h, topPct = 0.69f, leftPct = -0.08f, widthPct = 0.22f, angle = 20f, color = whiskerColor)

                // الشوارب (يمين)
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
        drawLine(
            color = color,
            start = Offset(startX, y),
            end = Offset(startX + w * widthPct, y),
            strokeWidth = h * 0.015f,
            cap = StrokeCap.Round
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWhiskerRight(
    w: Float, h: Float, topPct: Float, rightPct: Float, widthPct: Float, angle: Float, color: Color
) {
    val startX = w * (1f + rightPct)
    val y = h * topPct
    rotate(degrees = angle, pivot = Offset(startX - (w * widthPct) / 2f, y)) {
        drawLine(
            color = color,
            start = Offset(startX, y),
            end = Offset(startX - w * widthPct, y),
            strokeWidth = h * 0.015f,
            cap = StrokeCap.Round
        )
    }
}

// ============================================================
// 3. الشاشة الرئيسية ImageViewerScreen
//    (يقابل الأسطر 81-192 في App.tsx بالكامل)
// ============================================================

/** نوع الشريحة: إما رسمة القط التجريبية أو رابط صورة حقيقي */
sealed class ViewerSlide {
    object Cat : ViewerSlide()
    data class Remote(val url: String) : ViewerSlide()
}

@Composable
fun ImageViewerScreen(
    // القيم الافتراضية هنا تطابق تماماً القيم الثابتة (Hardcoded)
    // في App.tsx الأصلي: نفس الاسم، نفس الشرائح الثلاث، نفس العداد
    // الذي يبدأ من 118. لم يُغيَّر أو يُحذف أي جزء من المنطق الأصلي.
    senderName: String = "Badji Abde L Hani",
    dateTimeLabel: String = "اليوم عند 18:27",
    slides: List<ViewerSlide> = listOf(
        ViewerSlide.Cat,
        ViewerSlide.Remote("cyberpunk_city_placeholder"),
        ViewerSlide.Remote("mystical_forest_placeholder")
    ),
    startCounterAt: Int = 118,
    onBack: () -> Unit = {},
    onShowInChat: () -> Unit = {},
    onSaveToGallery: () -> Unit = {},
    onReply: () -> Unit = {},
    onShare: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    var currentIndex by remember { mutableStateOf(0) }
    // direction لم تعد مطلوبة بشكل صريح في Compose (AnimatedContent
    // يحدد اتجاه الانزلاق تلقائياً بناءً على targetState مقارنة
    // بـ initialState)، لكن أبقيتها لتطابق البنية الأصلية منطقياً.
    var direction by remember { mutableStateOf(0) }

    fun paginate(newDirection: Int) {
        var newIndex = currentIndex + newDirection
        if (newIndex < 0) newIndex = slides.size - 1
        if (newIndex >= slides.size) newIndex = 0
        direction = newDirection
        currentIndex = newIndex
    }

    fun convertToArabic(num: Int): String {
        val arabicDigits = "٠١٢٣٤٥٦٧٨٩"
        return num.toString().map { ch ->
            if (ch.isDigit()) arabicDigits[ch - '0'] else ch
        }.joinToString("")
    }

    val currentArabic = convertToArabic(startCounterAt + currentIndex)
    val totalArabic = convertToArabic(startCounterAt + slides.size - 1)
    val counterText = "$currentArabic من $totalArabic"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // -------------------------------------------------
        // منطقة المحتوى الرئيسية (الصور القابلة للسحب)
        // (يقابل الأسطر 143-170)
        // -------------------------------------------------
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    var dragOffsetX = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dragOffsetX = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragOffsetX += dragAmount
                        },
                        onDragEnd = {
                            // نفس منطق onDragEnd الأصلي في Framer Motion:
                            // إذا السحب لليسار كفاية -> paginate(1) (التالي)
                            // إذا السحب لليمين كفاية -> paginate(-1) (السابق)
                            if (dragOffsetX < -50f) paginate(1)
                            else if (dragOffsetX > 50f) paginate(-1)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = currentIndex,
                transitionSpec = {
                    val slideForward = targetState > initialState ||
                        (initialState == slides.lastIndex && targetState == 0)
                    if (slideForward) {
                        (androidx.compose.animation.slideInHorizontally(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = 300f
                            )
                        ) { it } + fadeIn(tween(200))) togetherWith
                            (androidx.compose.animation.slideOutHorizontally(
                                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 300f)
                            ) { -it } + fadeOut(tween(200)))
                    } else {
                        (androidx.compose.animation.slideInHorizontally(
                            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 300f)
                        ) { -it } + fadeIn(tween(200))) togetherWith
                            (androidx.compose.animation.slideOutHorizontally(
                                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 300f)
                            ) { it } + fadeOut(tween(200)))
                    }
                },
                label = "image_pager"
            ) { index ->
                when (val slide = slides[index]) {
                    is ViewerSlide.Cat -> CatArt(modifier = Modifier.fillMaxSize())
                    is ViewerSlide.Remote -> AsyncImage(
                        model = slide.url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // -------------------------------------------------
        // الشريط العلوي الشفاف (Header)
        // (يقابل الأسطر 110-135)
        // -------------------------------------------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.80f),
                        0.5f to Color.Black.copy(alpha = 0.40f),
                        1.0f to Color.Transparent
                    )
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // الجهة اليسرى: زر الرجوع + الاسم والتاريخ
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
                            onClick = onBack
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "رجوع",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = senderName,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    Text(
                        text = dateTimeLabel,
                        color = Color.White.copy(alpha = 0.70f),
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            // الجهة اليمنى: أزرار (عصا سحرية، تحويل، النقاط الثلاث)
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
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "المزيد",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // -------------------------------------------------
        // عداد الصور
        // (يقابل الأسطر 138-140)
        // -------------------------------------------------
        Text(
            text = counterText,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
        )

        // -------------------------------------------------
        // القائمة المنسدلة السوداء + الخلفية الشفافة لإغلاقها
        // (يقابل الأسطر 172-189)
        // -------------------------------------------------
        if (isMenuOpen) {
            // طبقة شفافة لإغلاق القائمة عند الضغط خارجها
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
                    .padding(top = 40.dp, end = 8.dp)
                    .widthIn(min = 170.dp)
                    .background(Color(0xFF272727), RoundedCornerShape(12.dp))
                    .padding(vertical = 4.dp)
            ) {
                Column {
                    MenuItem(Icons.Filled.RemoveRedEye, "Afficher dans l'échange") {
                        isMenuOpen = false; onShowInChat()
                    }
                    MenuItem(Icons.Filled.Download, "حفظ في المعرض") {
                        isMenuOpen = false; onSaveToGallery()
                    }
                    MenuItem(Icons.Filled.Reply, "Répondre") {
                        isMenuOpen = false; onReply()
                    }
                    MenuItem(Icons.Filled.Share, "Partager") {
                        isMenuOpen = false; onShare()
                    }
                    MenuItem(Icons.Filled.Delete, "Supprimer") {
                        isMenuOpen = false; onDelete()
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
            .size(36.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
    }
}
