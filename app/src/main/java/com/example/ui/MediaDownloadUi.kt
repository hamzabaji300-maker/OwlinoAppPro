package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.MediaStorage
import java.io.File

/** Circular progress drawn by hand (independent from the Material3 progress API version). */
@Composable
fun MediaProgressRing(progress: Float, modifier: Modifier = Modifier, color: Color = Color.White, stroke: Dp = 2.5.dp) {
    Canvas(modifier = modifier) {
        val w = stroke.toPx()
        val inset = w / 2f
        val sweep = 360f * progress.coerceIn(0.04f, 1f)
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(this.size.width - w, this.size.height - w),
            style = Stroke(width = w, cap = StrokeCap.Round)
        )
    }
}

fun mediaSizeLabel(bytes: Long?): String {
    if (bytes == null || bytes <= 0L) return ""
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1.0 -> String.format("%.1f MB", mb)
        kb >= 1.0 -> String.format("%.0f KB", kb)
        else -> "$bytes B"
    }
}

/**
 * Telegram/WhatsApp style media tile.
 *  - already on the phone  -> real image (instant, from the Owlino folder)
 *  - not downloaded        -> blurred preview + download button (tap to download)
 *  - downloading           -> progress ring with cancel
 * Own sent media and local (content://) media are shown normally.
 */
@Composable
fun DownloadableImage(
    att: Attachment,
    isMe: Boolean,
    modifier: Modifier = Modifier,
    isVideo: Boolean = false,
    contentScale: ContentScale = ContentScale.Crop,
    onOpen: () -> Unit
) {
    val context = LocalContext.current
    val files by MediaIndex.files.collectAsState()
    val ready by MediaIndex.ready.collectAsState()
    val states by MediaDownloadManager.states.collectAsState()

    val localPath = files[att.messageId]
    val isLocalUri = att.url.startsWith("content://") || att.url.startsWith("file://")
    val dl = states[att.messageId]

    Box(modifier = modifier) {
        if (localPath != null || isMe || isLocalUri) {
            // Real picture
            val model: Any = if (localPath != null) {
                File(localPath)
            } else {
                rememberMediaSource(context, att.messageId, att.thumbnailUrl ?: att.url, isThumbnail = att.thumbnailUrl != null)
            }
            coil.compose.SubcomposeAsyncImage(
                model = coil.request.ImageRequest.Builder(context)
                    .data(model)
                    .memoryCacheKey("media_" + att.messageId + if (localPath != null) "_l" else "_r")
                    .crossfade(false)
                    .build(),
                loading = { Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.12f))) },
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize().clickable { onOpen() }
            )
            if (isVideo) {
                Box(
                    modifier = Modifier.align(Alignment.Center).size(48.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                }
            }
        } else {
            // Blurred preview (tiny thumbnail), tap to download
            val thumb = att.thumbnailUrl
            if (thumb != null) {
                val src = rememberMediaSource(context, att.messageId, thumb, isThumbnail = true)
                coil.compose.AsyncImage(
                    model = coil.request.ImageRequest.Builder(context)
                        .data(src)
                        .size(coil.size.Size(32, 32))
                        .memoryCacheKey("blur_" + att.messageId)
                        .crossfade(false)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    filterQuality = FilterQuality.Low,
                    modifier = Modifier.fillMaxSize().blur(14.dp)
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF2B2F36)))
            }
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.22f)))

            if (ready) {
                when (dl) {
                    is MediaDlState.Downloading -> {
                        Box(
                            modifier = Modifier.align(Alignment.Center).size(50.dp).clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .clickable { MediaDownloadManager.cancel(att.messageId) },
                            contentAlignment = Alignment.Center
                        ) {
                            MediaProgressRing(progress = dl.progress, modifier = Modifier.size(44.dp))
                            Icon(Icons.Filled.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                    is MediaDlState.Failed -> {
                        Box(
                            modifier = Modifier.align(Alignment.Center).size(50.dp).clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .clickable { MediaDownloadManager.download(context, att) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                        }
                    }
                    else -> {
                        Box(
                            modifier = Modifier.align(Alignment.Center).size(50.dp).clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .clickable { MediaDownloadManager.download(context, att) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                        }
                    }
                }
                val label = mediaSizeLabel(att.fileSize)
                if (label.isNotEmpty() && dl !is MediaDlState.Downloading) {
                    Box(
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                            .clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(alpha = 0.45f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(label, color = Color.White, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
