package com.example.ui

import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.util.MediaAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private fun fmtTime(ms: Long): String {
    val t = (ms.coerceAtLeast(0L) / 1000L).toInt()
    val h = t / 3600
    val m = (t % 3600) / 60
    val s = t % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

private fun skipLabel(deltaMs: Long): String {
    val a = kotlin.math.abs(deltaMs)
    val n = if (a >= 60_000L) "${a / 60_000L}د" else "${a / 1000L}ث"
    return if (deltaMs > 0) "+$n" else "−$n"
}

private fun toPlayableUri(source: String): Uri =
    if (source.startsWith("/")) Uri.fromFile(File(source)) else Uri.parse(source)

/**
 * مشغّل فيديو كامل الشاشة داخل التطبيق.
 * source: مسار ملف محلي، أو content:// ، أو file:// ، أو رابط بعيد (يُضاف له توكن الجلسة تلقائيًا لروابط الـ Worker).
 * يدعم: تشغيل/إيقاف، نقر مزدوج يمين/يسار = ±10 ثوانٍ، أزرار ±10 ثوانٍ و±1 دقيقة، وشريط تمرير لأي موضع.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerDialog(
    source: String,
    title: String,
    subtitle: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val player = remember(source) {
        val upstream = DefaultDataSource.Factory(
            context,
            DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15_000)
                .setReadTimeoutMs(30_000)
        )
        // يُستدعى عند كل طلب (بما فيه طلبات Range بعد التقديم)، فيأخذ التوكن الحالي دائمًا
        val resolving = ResolvingDataSource.Factory(upstream) { spec ->
            val h = MediaAuth.headersFor(source)
            if (h.isEmpty()) spec else spec.withAdditionalHeaders(h)
        }
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(resolving))
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(toPlayableUri(source)))
                prepare()
                playWhenReady = true
            }
    }

    var isPlaying by remember { mutableStateOf(false) }
    var buffering by remember { mutableStateOf(true) }
    var positionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var showControls by remember { mutableStateOf(true) }
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableStateOf(0f) }
    var toast by remember { mutableStateOf<String?>(null) }
    var authRetried by remember { mutableStateOf(false) }
    var interaction by remember { mutableStateOf(0) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(state: Int) {
                buffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) {
                    if (player.duration > 0) durationMs = player.duration
                    errorText = null
                }
                if (state == Player.STATE_ENDED) showControls = true
            }
            override fun onPlayerError(error: PlaybackException) {
                val c = error.cause
                val unauthorized = c is HttpDataSource.InvalidResponseCodeException && c.responseCode == 401
                if (unauthorized && !authRetried) {
                    // الجلسة انتهت: نجدّدها مرة واحدة ثم نكمل من نفس الموضع
                    authRetried = true
                    val pos = player.currentPosition
                    scope.launch {
                        withContext(Dispatchers.IO) { MediaAuth.refreshTokenBlocking() }
                        player.prepare()
                        player.seekTo(pos)
                        player.play()
                    }
                } else {
                    errorText = "تعذّر تشغيل الفيديو"
                }
            }
        }
        player.addListener(listener)
        val lifecycle = ProcessLifecycleOwner.get().lifecycle
        val observer = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_STOP) player.pause()
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            player.removeListener(listener)
            player.release()
        }
    }

    // تحديث موضع التشغيل
    LaunchedEffect(player) {
        while (true) {
            if (!dragging) positionMs = player.currentPosition
            val d = player.duration
            if (d > 0) durationMs = d
            delay(250)
        }
    }
    // إخفاء الأزرار تلقائيًا بعد 3 ثوانٍ أثناء التشغيل
    LaunchedEffect(showControls, isPlaying, interaction, dragging) {
        if (showControls && isPlaying && !dragging) {
            delay(3000)
            showControls = false
        }
    }
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(700)
            toast = null
        }
    }

    fun skip(deltaMs: Long) {
        val max = if (player.duration > 0) player.duration else Long.MAX_VALUE
        val target = (player.currentPosition + deltaMs).coerceIn(0L, max)
        player.seekTo(target)
        positionMs = target
        toast = skipLabel(deltaMs)
        interaction++
    }

    fun togglePlay() {
        if (player.playbackState == Player.STATE_ENDED) {
            player.seekTo(0)
            player.play()
        } else if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
        interaction++
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            dialogWindow?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            dialogWindow?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.BLACK))
        }

        // أزرار التحكم والشريط دائمًا من اليسار لليمين مهما كانت لغة التطبيق
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            setShutterBackgroundColor(android.graphics.Color.BLACK)
                            this.player = player
                        }
                    },
                    update = { it.player = player },
                    modifier = Modifier.fillMaxSize()
                )

                // طبقة اللمس: نقرة = إظهار/إخفاء الأزرار، نقرتان = ±10 ثوانٍ
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(player) {
                            detectTapGestures(
                                onTap = { showControls = !showControls },
                                onDoubleTap = { o ->
                                    if (o.x < size.width / 2f) skip(-10_000L) else skip(10_000L)
                                }
                            )
                        }
                )

                if (buffering && errorText == null) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center).size(46.dp)
                    )
                }

                if (errorText != null) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(errorText ?: "", color = Color.White, fontSize = 16.sp)
                        Button(
                            onClick = {
                                errorText = null
                                authRetried = false
                                player.prepare()
                                player.play()
                            },
                            modifier = Modifier.padding(top = 12.dp)
                        ) { Text("إعادة المحاولة") }
                    }
                }

                toast?.let { t ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50))
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    ) { Text(t, color = Color.White, fontSize = 18.sp) }
                }

                AnimatedVisibility(
                    visible = showControls,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // الشريط العلوي
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .fillMaxWidth()
                                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)))
                                .statusBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                            }
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(title, color = Color.White, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (subtitle.isNotEmpty()) {
                                    Text(subtitle, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, maxLines = 1)
                                }
                            }
                        }

                        // الأزرار الوسطى: -1د  -10ث  تشغيل/إيقاف  +10ث  +1د
                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(onClick = { skip(-60_000L) }) {
                                Text("−1د", color = Color.White, fontSize = 14.sp)
                            }
                            IconButton(onClick = { skip(-10_000L) }) {
                                Icon(Icons.Filled.Replay10, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
                            }
                            IconButton(
                                onClick = { togglePlay() },
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            IconButton(onClick = { skip(10_000L) }) {
                                Icon(Icons.Filled.Forward10, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
                            }
                            IconButton(onClick = { skip(60_000L) }) {
                                Text("+1د", color = Color.White, fontSize = 14.sp)
                            }
                        }

                        // الشريط السفلي: الوقت وشريط التمرير
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
                                .navigationBarsPadding()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val shownPos = if (dragging) dragValue.toLong() else positionMs
                            Text(fmtTime(shownPos), color = Color.White, fontSize = 12.sp)
                            Slider(
                                value = shownPos.coerceIn(0L, maxOf(durationMs, 1L)).toFloat(),
                                onValueChange = {
                                    dragging = true
                                    dragValue = it
                                },
                                onValueChangeFinished = {
                                    player.seekTo(dragValue.toLong())
                                    positionMs = dragValue.toLong()
                                    dragging = false
                                    interaction++
                                },
                                valueRange = 0f..maxOf(durationMs, 1L).toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color.White,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                            )
                            Text(fmtTime(durationMs), color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
