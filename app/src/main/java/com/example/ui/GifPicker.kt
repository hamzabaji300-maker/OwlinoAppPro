package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.util.GifFavorites
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.util.GifClient
import com.example.util.GifItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/**
 * شاشة اختيار الـ GIF (من Klipy). عند الاختيار نُرسل الرابط فقط.
 * الشروط: عبارة البحث "Search KLIPY" وشعار "Powered by KLIPY" (من إرشادات Klipy).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GifPickerSheet(
    customerId: String,
    onDismiss: () -> Unit,
    onPick: (GifItem) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    var gifs by remember { mutableStateOf<List<GifItem>>(emptyList()) }
    var page by remember { mutableIntStateOf(1) }
    var hasNext by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val gridState = rememberLazyGridState()
    val context = LocalContext.current
    // المفضلة تظهر أولًا إن وُجدت (بدون بحث)، وإلا يظهر الرائج
    var showFavorites by remember {
        GifFavorites.ensureLoaded(context)
        mutableStateOf(GifFavorites.items.value.isNotEmpty())
    }
    val favorites by GifFavorites.items.collectAsState()

    suspend fun load(reset: Boolean) {
        if (!GifClient.isConfigured) {
            error = "ميزة GIF غير مفعّلة بعد (مفتاح Klipy غير مضبوط)"
            return
        }
        loading = true
        error = null
        try {
            val next = if (reset) 1 else page + 1
            val res = if (query.isBlank()) GifClient.trending(customerId, next)
            else GifClient.search(query.trim(), customerId, next)
            gifs = if (reset) res.items else gifs + res.items
            page = next
            hasNext = res.hasNext
            if (reset && res.items.isEmpty()) error = "لا توجد نتائج"
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (reset) gifs = emptyList()
            error = "تعذّر تحميل الـ GIF. تحقق من الإنترنت وحاول مجددًا"
        } finally {
            loading = false
        }
    }

    // بحث بعد توقف الكتابة (يوفّر طلبات المفتاح التجريبي: 100 طلب/ساعة)
    LaunchedEffect(query, showFavorites) {
        if (showFavorites && query.isBlank()) return@LaunchedEffect
        if (query.isNotEmpty()) delay(500)
        load(true)
    }

    val nearEnd by remember {
        derivedStateOf {
            val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= gifs.size - 6
        }
    }
    LaunchedEffect(nearEnd, hasNext, loading, showFavorites) {
        if (!showFavorites && nearEnd && hasNext && !loading && gifs.isNotEmpty()) load(false)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 12.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    if (it.isNotBlank()) showFavorites = false
                },
                singleLine = true,
                placeholder = { Text("Search KLIPY") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = showFavorites,
                    onClick = { showFavorites = true; query = "" },
                    label = { Text("المفضلة (${favorites.size})") }
                )
                FilterChip(
                    selected = !showFavorites,
                    onClick = { showFavorites = false },
                    label = { Text("الرائج") }
                )
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp)) {
                val shown = if (showFavorites) favorites else gifs
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = gridState,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(shown, key = { it.id }) { gif ->
                        val isFav = favorites.any { it.id == gif.id }
                        val ratio = (gif.aspectRatio ?: 1.3f).coerceIn(0.6f, 2.2f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(ratio)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onPick(gif) }
                        ) {
                            AsyncImage(
                                model = gif.previewUrl,
                                contentDescription = gif.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // نجمة المفضلة: اضغطها لإضافة الـ GIF للمفضلة أو إزالتها
                            Icon(
                                imageVector = if (isFav) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = if (isFav) "إزالة من المفضلة" else "إضافة للمفضلة",
                                tint = if (isFav) Color(0xFFFFC107) else Color.White,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(30.dp)
                                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                    .clickable { GifFavorites.toggle(context, gif) }
                                    .padding(5.dp)
                            )
                        }
                    }
                }
                if (showFavorites && favorites.isEmpty()) {
                    Text(
                        text = "لا توجد مفضلة بعد.\nاضغط النجمة على أي GIF لإضافته هنا.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center).padding(24.dp)
                    )
                }
                if (!showFavorites && loading && gifs.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                val err = error
                if (!showFavorites && err != null && gifs.isEmpty() && !loading) {
                    Text(
                        text = err,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center).padding(24.dp)
                    )
                }
            }

            Text(
                text = "Powered by KLIPY",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
        }
    }
}
