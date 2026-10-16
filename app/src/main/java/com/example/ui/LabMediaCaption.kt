package com.example.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

/**
 * شاشة معاينة الوسائط قبل الإرسال (مثل تيليجرام): تعرض الصور/الفيديوهات المختارة،
 * وتتيح كتابة تعليق يُرسل مع الصور في رسالة واحدة.
 */
@Composable
fun LabMediaCaptionScreen(
    uris: List<Uri>,
    type: AttachmentType,
    initialCaption: String,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    BackHandler { onDismiss() }
    val accent = LocalSettingsTheme.current.accent
    var caption by remember { mutableStateOf(initialCaption) }
    val pager = rememberPagerState(pageCount = { uris.size })
    val scope = rememberCoroutineScope()
    val isVideo = type == AttachmentType.VIDEO

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
    ) {
        // الشريط العلوي
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(Color(0x33FFFFFF)).clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Outlined.Close, contentDescription = null, tint = Color.White) }
            Spacer(Modifier.width(12.dp))
            Text(
                if (uris.size == 1) (if (isVideo) "فيديو" else "صورة") else "${pager.currentPage + 1} / ${uris.size}",
                color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold
            )
        }

        // المعاينة الكبيرة
        HorizontalPager(state = pager, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
            Box(Modifier.fillMaxSize().padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = uris[page],
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))
                )
                if (isVideo) {
                    Icon(Icons.Outlined.PlayCircle, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(64.dp))
                }
            }
        }

        // شريط المصغّرات عند اختيار أكثر من ملف
        if (uris.size > 1) {
            LazyRow(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(uris) { i, u ->
                    val sel = pager.currentPage == i
                    AsyncImage(
                        model = u,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(if (sel) 2.dp else 0.dp, if (sel) accent else Color.Transparent, RoundedCornerShape(10.dp))
                            .clickable { scope.launch { pager.animateScrollToPage(i) } }
                    )
                }
            }
        }

        // حقل التعليق + زر الإرسال
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 10.dp, top = 4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp, max = 140.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF1F1F1F))
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (caption.isEmpty()) Text("إضافة تعليق…", color = Color(0xFF8E8E93), fontSize = 16.sp)
                BasicTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(accent).clickable { onSend(caption) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
    }
}
