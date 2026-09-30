package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun FastScroll(
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var trackHeight by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var dragY by remember { mutableFloatStateOf(0f) }

    val firstVisibleItemIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    val totalItems by remember { derivedStateOf { listState.layoutInfo.totalItemsCount } }

    val thumbHeightDp = 40.dp
    val density = LocalDensity.current
    val thumbHeightPx = with(density) { thumbHeightDp.toPx() }

    if (totalItems > 0 && trackHeight > thumbHeightPx) {
        val maxScrollFraction = if (totalItems > 1) (totalItems - 1).toFloat() else 1f
        val scrollFraction = firstVisibleItemIndex.toFloat() / maxScrollFraction
        val maxThumbY = trackHeight - thumbHeightPx
        val thumbY = (scrollFraction * maxThumbY).roundToInt()

        Box(
            modifier = modifier
                .fillMaxHeight()
                .padding(end = 4.dp)
                .onGloballyPositioned { coordinates ->
                    trackHeight = coordinates.size.height.toFloat()
                }
        ) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(0, if (isDragging) dragY.roundToInt().coerceIn(0, maxThumbY.toInt()) else thumbY) }
                    .size(width = 8.dp, height = thumbHeightDp)
                    .alpha(if (isDragging || listState.isScrollInProgress) 1f else 0.5f)
                    .background(Color.Gray, CircleShape)
                    .pointerInput(maxThumbY, totalItems) {
                        detectDragGestures(
                            onDragStart = { offset -> 
                                isDragging = true
                                dragY = thumbY.toFloat()
                            },
                            onDragEnd = { isDragging = false },
                            onDragCancel = { isDragging = false }
                        ) { change, dragAmount ->
                            change.consume()
                            dragY += dragAmount.y
                            val newFraction = (dragY / maxThumbY).coerceIn(0f, 1f)
                            val targetIndex = (newFraction * (totalItems - 1)).roundToInt().coerceIn(0, totalItems - 1)
                            coroutineScope.launch {
                                listState.scrollToItem(targetIndex)
                            }
                        }
                    }
            )
        }
    }
}
