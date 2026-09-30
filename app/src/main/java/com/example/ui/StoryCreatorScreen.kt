package com.example.ui

import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha

import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.example.ui.i18n.LocalTranslation
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import kotlinx.coroutines.delay


fun getRecentPhotos(context: android.content.Context): List<Uri> {
    val uris = mutableListOf<Uri>()
    val projection = arrayOf(
        android.provider.MediaStore.Images.Media._ID
    )
    val sortOrder = "${android.provider.MediaStore.Images.Media.DATE_ADDED} DESC"
    try {
        context.contentResolver.query(
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media._ID)
            while (cursor.moveToNext() && uris.size < 100) {
                val id = cursor.getLong(idColumn)
                val contentUri = android.content.ContentUris.withAppendedId(
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                uris.add(contentUri)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return uris
}


data class ImageSticker(val id: Long, val uri: Uri, var offset: Offset = Offset.Zero, var scale: Float = 1f, var rotation: Float = 0f)

data class DrawingPath(val points: List<Offset>, val color: Color, val tool: String)
data class StoryTextData(val id: Long, var text: String, var color: Color, var align: TextAlign, var style: String, var offset: Offset, var font: FontFamily)

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StoryCreatorScreen(
    onBack: () -> Unit,
    onPublish: (String, String) -> Unit // url, type (image/video)
) {
    
    val context = LocalContext.current
    val permissionsToRequest = remember {
        val perms = mutableListOf(android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            perms.add(android.Manifest.permission.READ_MEDIA_IMAGES)
            perms.add(android.Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            perms.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        perms.toTypedArray()
    }
    var hasPermissions by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermissions = permissions.values.all { it }
    }

    LaunchedEffect(Unit) {
        val allGranted = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            hasPermissions = true
        } else {
            permissionLauncher.launch(permissionsToRequest)
        }
    }
    
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    LaunchedEffect(Unit) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            cameraProvider = providerFuture.get()
        }, ContextCompat.getMainExecutor(context))
    }

    var step by remember { mutableStateOf("gallery") } // camera, editor, gallery
    var creationType by remember { mutableStateOf("story") } // story, post, video
    var isCreationTypeMenuOpen by remember { mutableStateOf(false) }
    var cameraMode by remember { mutableStateOf("photo") }
    
    var capturedMedia by remember { mutableStateOf<Uri?>(null) }
    var mediaType by remember { mutableStateOf("image") }
    
    // Camera state
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var flashEnabled by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingTime by remember { mutableStateOf(0) }
    
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingTime = 0
            while (true) {
                delay(50)
                recordingTime += 50
            }
        }
    }
    
    // Drawing state
    var isDrawingMode by remember { mutableStateOf(false) }
    val paths = remember { mutableStateListOf<DrawingPath>() }
    var currentPath by remember { mutableStateOf<DrawingPath?>(null) }
    var drawColor by remember { mutableStateOf(Color.White) }
    var drawTool by remember { mutableStateOf("pen") } // pen, marker, neon, eraser
    var showColorPicker by remember { mutableStateOf(false) }
    var showShapesMenu by remember { mutableStateOf(false) }
    
    // Text state
    var isTextMode by remember { mutableStateOf(false) }
    var isDurationMenuOpen by remember { mutableStateOf(false) }
    var storyDuration by remember { mutableStateOf(24) }
    val storyTexts = remember { mutableStateListOf<StoryTextData>() }
    var currentText by remember { mutableStateOf("") }
    var currentTextColor by remember { mutableStateOf(Color.White) }
    var currentTextAlign by remember { mutableStateOf(TextAlign.Center) }
    var currentTextStyle by remember { mutableStateOf("transparent") } // transparent, filled, solid
    var currentFont by remember { mutableStateOf(FontFamily.Default) }
    var currentFontName by remember { mutableStateOf("Roboto") }
    var showTextColorPicker by remember { mutableStateOf(false) }
    var showFontMenu by remember { mutableStateOf(false) }

    // Media Pan/Zoom

    var recentPhotos by remember { mutableStateOf<List<Uri>>(emptyList()) }
    LaunchedEffect(hasPermissions, step) {
        if (hasPermissions && step == "gallery") {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val photos = getRecentPhotos(context)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    recentPhotos = photos
                }
            }
        }
    }

    val selectedGalleryMedias = remember { mutableStateListOf<Uri>() }
    val imageStickers = remember { mutableStateListOf<ImageSticker>() }
    var currentEditIndex by remember { mutableStateOf(0) }
    var mediaScale by remember { mutableStateOf(1f) }
    var mediaOffset by remember { mutableStateOf(Offset.Zero) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            capturedMedia = uri
            val type = context.contentResolver.getType(uri)
            mediaType = if (type?.startsWith("video/") == true) "video" else "image"
            step = "editor"
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        
        AnimatedContent(targetState = step, label = "StoryCreatorSteps") { currentStep ->
            when (currentStep) {
                "camera" -> {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                        // Camera Preview
                        var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
                        
                        var previewViewRef by remember { mutableStateOf<androidx.camera.view.PreviewView?>(null) }
                        
                        
                        if (hasPermissions) {
                            AndroidView(
                                factory = { ctx ->
                                    androidx.camera.view.PreviewView(ctx).apply {
                                        layoutParams = ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                        scaleType = androidx.camera.view.PreviewView.ScaleType.FILL_CENTER
                                        previewViewRef = this
                                    }
                                },
                                update = { },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Camera permission required", color = Color.White)
                            }
                        }
                        
                        LaunchedEffect(lensFacing, previewViewRef, lifecycleOwner, cameraProvider) {
                            val pv = previewViewRef ?: return@LaunchedEffect
                            val provider = cameraProvider ?: return@LaunchedEffect
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(pv.surfaceProvider)
                            }
                            val imageCap = ImageCapture.Builder().build()
                            imageCapture = imageCap
                            val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                            try {
                                provider.unbindAll()
                                val camera = provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCap)
                                cameraControl = camera.cameraControl
                            } catch(exc: Exception) {
                            }
                        }
                        
                        LaunchedEffect(flashEnabled, cameraControl) {
                            cameraControl?.enableTorch(flashEnabled)
                        }
                        
                        // Top Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!isRecording) {
                                IconButton(onClick = onBack) {
                                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = Color.White)
                                }
                                
                                Box(contentAlignment = Alignment.TopCenter) {
                                    Row(
                                        modifier = Modifier
                                            .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                                            .padding(horizontal = 16.dp, vertical = 6.dp)
                                            .clickable { isCreationTypeMenuOpen = !isCreationTypeMenuOpen },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = when(creationType) {
                                                "story" -> "New Story"
                                                "post" -> "New Post"
                                                else -> "New Video"
                                            },
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = Color.White)
                                    }
                                    
                                    if (isCreationTypeMenuOpen) {
                                        Column(
                                            modifier = Modifier
                                                .padding(top = 40.dp)
                                                .background(Color(0xFF1C1C1E).copy(alpha = 0.9f), RoundedCornerShape(16.dp))
                                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                                                .width(150.dp)
                                        ) {
                                            listOf("story" to "New Story", "post" to "New Post", "video" to "New Video").forEach { (type, label) ->
                                                Text(
                                                    text = label,
                                                    color = Color.White,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            creationType = type
                                                            isCreationTypeMenuOpen = false
                                                        }
                                                        .padding(16.dp),
                                                    textAlign = TextAlign.Center
                                                )
                                                if (type != "video") Divider(color = Color.White.copy(alpha = 0.1f))
                                            }
                                        }
                                    }
                                }
                                
                                Row {
                                    IconButton(onClick = { flashEnabled = !flashEnabled }) {
                                        Icon(if (flashEnabled) Icons.Filled.FlashOn else Icons.Outlined.FlashOff, contentDescription = "Flash", tint = if (flashEnabled) Color.Yellow else Color.White)
                                    }
                                    IconButton(onClick = { /* Toggle Grid */ }) {
                                        Icon(Icons.Outlined.GridOn, contentDescription = "Grid", tint = Color.White)
                                    }
                                }
                            } else {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Row(
                                        modifier = Modifier.background(Color.Black.copy(alpha=0.5f), CircleShape).padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.size(10.dp).background(Color.Red, CircleShape))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        val min = (recordingTime / 1000) / 60
                                        val sec = (recordingTime / 1000) % 60
                                        Text(String.format("%02d:%02d", min, sec), color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        
                        // Bottom Controls
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha=0.5f))))
                                .navigationBarsPadding()
                                .padding(bottom = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Gallery Button
                                if (!isRecording) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFFA855F7))))
                                            .border(2.dp, Color.White.copy(alpha=0.2f), RoundedCornerShape(12.dp))
                                            .clickable { step = "gallery" },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Outlined.Image, contentDescription = "Gallery", tint = Color.White)
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(56.dp))
                                }
                                
                                // Capture Button
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clickable {
                                            if (creationType == "video" || cameraMode == "video") {
                                                isRecording = !isRecording
                                            } else {
                                                val photoFile = java.io.File(context.cacheDir, "story_photo_${System.currentTimeMillis()}.jpg")
                                                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                                                imageCapture?.takePicture(
                                                    outputOptions,
                                                    ContextCompat.getMainExecutor(context),
                                                    object : ImageCapture.OnImageSavedCallback {
                                                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                                            capturedMedia = android.net.Uri.fromFile(photoFile)
                                                            mediaType = "image"
                                                            step = "editor"
                                                        }
                                                        override fun onError(exc: ImageCaptureException) {
                                                        }
                                                    }
                                                )
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isRecording) {
                                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                            drawCircle(color = Color.White.copy(alpha=0.3f), style = androidx.compose.ui.graphics.drawscope.Stroke(4.dp.toPx()))
                                            drawArc(
                                                color = Color.Red,
                                                startAngle = -90f,
                                                sweepAngle = (recordingTime / 15000f).coerceAtMost(1f) * 360f,
                                                useCenter = false,
                                                style = androidx.compose.ui.graphics.drawscope.Stroke(4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                            )
                                        }
                                    } else {
                                        Box(modifier = Modifier.fillMaxSize().border(4.dp, Color.White, CircleShape))
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(if (isRecording) 32.dp else 64.dp)
                                            .clip(if (isRecording) RoundedCornerShape(8.dp) else CircleShape)
                                            .then(
                                                if (!isRecording && creationType != "video" && cameraMode == "live") 
                                                    Modifier.background(Brush.linearGradient(listOf(Color(0xFFEC4899), Color(0xFFA855F7))))
                                                else 
                                                    Modifier.background(if (isRecording || creationType == "video" || cameraMode == "video") Color.Red else Color.White)
                                            )
                                    )
                                }
                                
                                // Flip Camera
                                if (!isRecording) {
                                    IconButton(
                                        onClick = {
                                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                                        },
                                        modifier = Modifier.size(56.dp).background(Color.Black.copy(alpha=0.3f), CircleShape)
                                    ) {
                                        Icon(Icons.Outlined.FlipCameraIos, contentDescription = "Flip", tint = Color.White)
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(56.dp))
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Modes
                            if (creationType == "story" && !isRecording) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf("live" to "Live", "photo" to "Photo", "video" to "Video").forEach { (mode, label) ->
                                        Text(
                                            text = label,
                                            color = if (cameraMode == mode) Color.White else Color.White.copy(alpha=0.7f),
                                            fontWeight = if (cameraMode == mode) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(if (cameraMode == mode) Color.Black.copy(alpha=0.4f) else Color.Transparent)
                                                .clickable { cameraMode = mode }
                                                .padding(horizontal = 20.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                                "editor" -> {
                    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            // Canvas Box
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = 8.dp)
                                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                                    .background(Color(0xFF1C1C1E))
                                    .pointerInput(Unit) {
                                        detectTransformGestures { _, pan, zoom, _ ->
                                            mediaScale = (mediaScale * zoom).coerceIn(0.5f, 5f)
                                            mediaOffset += pan
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (capturedMedia != null) {
                                    AsyncImage(
                                        model = capturedMedia?.toString(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize().graphicsLayer(
                                            scaleX = mediaScale,
                                            scaleY = mediaScale,
                                            translationX = mediaOffset.x,
                                            translationY = mediaOffset.y
                                        )
                                    )
                                }
                                
                                // Canvas for Drawings
                                Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }.pointerInput(Unit) {
                                    if (isDrawingMode) {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                currentPath = DrawingPath(listOf(offset), drawColor, drawTool)
                                            },
                                            onDrag = { change, _ ->
                                                val points = currentPath?.points?.toMutableList() ?: mutableListOf()
                                                points.add(change.position)
                                                currentPath = currentPath?.copy(points = points)
                                            },
                                            onDragEnd = {
                                                currentPath?.let { paths.add(it) }
                                                currentPath = null
                                            }
                                        )
                                    }
                                }) {
                                    paths.forEach { path ->
                                        val drawPath = androidx.compose.ui.graphics.Path().apply {
                                            if (path.points.isNotEmpty()) {
                                                moveTo(path.points.first().x, path.points.first().y)
                                                for (i in 1 until path.points.size) {
                                                    lineTo(path.points[i].x, path.points[i].y)
                                                }
                                            }
                                        }
                                        when (path.tool) {
                                            "marker" -> drawPath(drawPath, color = path.color.copy(alpha=0.5f), style = Stroke(width = 40f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                                            "neon" -> {
                                                drawPath(drawPath, color = path.color, style = Stroke(width = 20f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                                                drawPath(drawPath, color = Color.White, style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                                            }
                                            "eraser" -> drawPath(drawPath, color = Color.Transparent, blendMode = BlendMode.Clear, style = Stroke(width = 50f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                                            else -> drawPath(drawPath, color = path.color, style = Stroke(width = 12f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                                        }
                                    }
                                    currentPath?.let { path ->
                                        val drawPath = androidx.compose.ui.graphics.Path().apply {
                                            if (path.points.isNotEmpty()) {
                                                moveTo(path.points.first().x, path.points.first().y)
                                                for (i in 1 until path.points.size) {
                                                    lineTo(path.points[i].x, path.points[i].y)
                                                }
                                            }
                                        }
                                        when (path.tool) {
                                            "marker" -> drawPath(drawPath, color = path.color.copy(alpha=0.5f), style = Stroke(width = 40f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                                            "neon" -> {
                                                drawPath(drawPath, color = path.color, style = Stroke(width = 20f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                                                drawPath(drawPath, color = Color.White, style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                                            }
                                            "eraser" -> drawPath(drawPath, color = Color.Transparent, blendMode = BlendMode.Clear, style = Stroke(width = 50f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                                            else -> drawPath(drawPath, color = path.color, style = Stroke(width = 12f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                                        }
                                    }
                                }
                                
                                                                // Stickers
                                imageStickers.forEach { sticker ->
                                    Box(
                                        modifier = Modifier
                                            .offset { IntOffset(sticker.offset.x.toInt(), sticker.offset.y.toInt()) }
                                            .graphicsLayer(
                                                scaleX = sticker.scale,
                                                scaleY = sticker.scale,
                                                rotationZ = sticker.rotation
                                            )
                                            .size(150.dp, 220.dp)
                                            .clip(RoundedCornerShape(24.dp))
                                            .border(3.dp, Color.White, RoundedCornerShape(24.dp))
                                            .pointerInput(sticker.id) {
                                                detectTransformGestures { _, pan, zoom, rotation ->
                                                    val index = imageStickers.indexOfFirst { it.id == sticker.id }
                                                    if (index != -1) {
                                                        val currentSticker = imageStickers[index]
                                                        imageStickers[index] = currentSticker.copy(
                                                            offset = currentSticker.offset + pan,
                                                            scale = (currentSticker.scale * zoom).coerceIn(0.5f, 5f),
                                                            rotation = currentSticker.rotation + rotation
                                                        )
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = sticker.uri,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                // Text Overlays
                                storyTexts.forEachIndexed { index, textData ->
                                    var offset by remember(textData.id) { mutableStateOf(textData.offset) }
                                    var scale by remember(textData.id) { mutableStateOf(1f) }
                                    var rotation by remember(textData.id) { mutableStateOf(0f) }
                                    
                                    Box(
                                        modifier = Modifier
                                            .offset { IntOffset(offset.x.toInt(), offset.y.toInt()) }
                                            .graphicsLayer(
                                                scaleX = scale,
                                                scaleY = scale,
                                                rotationZ = rotation
                                            )
                                            .pointerInput(textData.id) {
                                                if (!isDrawingMode && !isTextMode) {
                                                    detectTransformGestures { _, pan, zoom, rot ->
                                                        offset += pan
                                                        textData.offset = offset
                                                        scale = (scale * zoom).coerceIn(0.5f, 5f)
                                                        rotation += rot
                                                    }
                                                }
                                            }
                                            .background(
                                                when(textData.style) {
                                                    "filled" -> Color.Black.copy(alpha = 0.5f)
                                                    "solid" -> Color.White
                                                    else -> Color.Transparent
                                                },
                                                RoundedCornerShape(12.dp)
                                            )
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = textData.text,
                                            color = if (textData.style == "solid") Color.Black else textData.color,
                                            textAlign = textData.align,
                                            fontFamily = textData.font,
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Bold,
                                            style = androidx.compose.ui.text.TextStyle(
                                                shadow = if (textData.style == "transparent") Shadow(Color.Black.copy(alpha=0.5f), offset = Offset(0f, 4f), blurRadius = 8f) else null
                                            )
                                        )
                                    }
                                }
                            // Caption Bar Overlay
                            if (!isDrawingMode && !isTextMode) {
                                // Floating Menu
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = isDurationMenuOpen,
                                    enter = androidx.compose.animation.slideInVertically(initialOffsetY = { 50 }, animationSpec = androidx.compose.animation.core.spring(stiffness = 300f, dampingRatio = 0.8f)) + androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(initialScale = 0.9f),
                                    exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { 50 }) + androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(targetScale = 0.9f),
                                    modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 76.dp, end = 28.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .width(140.dp)
                                            .background(Color(0xFF1C1C1E).copy(alpha = 0.9f), RoundedCornerShape(16.dp))
                                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                                    ) {
                                        listOf(6, 12, 24, 48).forEachIndexed { index, hours ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { 
                                                        storyDuration = hours
                                                        isDurationMenuOpen = false
                                                    }
                                                    .padding(16.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("$hours ساعات", color = Color.White, fontSize = 15.sp)
                                                if (storyDuration == hours) {
                                                    Box(modifier = Modifier.size(8.dp).background(Color.Blue, CircleShape))
                                                }
                                            }
                                            if (index < 3) HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha=0.5f))))
                                        .padding(start = 12.dp, end = 12.dp, bottom = 16.dp, top = 48.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.Black.copy(alpha=0.4f), RoundedCornerShape(24.dp))
                                            .border(1.dp, Color.White.copy(alpha=0.1f), RoundedCornerShape(24.dp))
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Add a caption...",
                                            color = Color.White.copy(alpha=0.7f),
                                            fontSize = 17.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        
                                        Box(modifier = Modifier.padding(start = 8.dp).size(28.dp), contentAlignment = Alignment.Center) {
                                            if (!isDurationMenuOpen) {
                                                val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                                val isPressed by interactionSource.collectIsPressedAsState()
                                                val durationScale by androidx.compose.animation.core.animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f)
                                                
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .graphicsLayer(scaleX = durationScale, scaleY = durationScale)
                                                        .background(Color.Transparent, CircleShape)
                                                        .border(1.5.dp, Color.White.copy(alpha=0.8f), CircleShape)
                                                        .clickable(interactionSource = interactionSource, indication = null) { isDurationMenuOpen = !isDurationMenuOpen },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(storyDuration.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            } else {
                                                // When menu is open, they can click here to close it without selecting
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .background(Color.White.copy(alpha=0.2f), CircleShape)
                                                        .clickable { isDurationMenuOpen = false },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Outlined.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                            // Top Bar Editor (Absolute overlay)
                            Row(
                                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp).align(Alignment.TopCenter),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { step = "camera" }) {
                                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = Color.White)
                                }
                                Text(
                                    text = when(creationType) { "story" -> "New Story"; "post" -> "New Post"; else -> "New Video" },
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 18.sp
                                )
                                IconButton(onClick = { android.widget.Toast.makeText(context, "Saved to Gallery", android.widget.Toast.LENGTH_SHORT).show() }) {
                                    Icon(Icons.Outlined.Download, contentDescription = "Save", tint = Color.White)
                                }
                            }
                        }
                        
                        // Bottom Bar Editor
                        Row(
                                modifier = Modifier
                                    
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 24.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .clickable { isDrawingMode = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Outlined.Brush, contentDescription = "Draw", tint = Color.White, modifier = Modifier.size(24.dp))
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .clickable { isTextMode = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Outlined.Title, contentDescription = "Text", tint = Color.White, modifier = Modifier.size(24.dp))
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(Color.White.copy(alpha=0.15f), CircleShape)
                                            .border(1.dp, Color.White, CircleShape)
                                            .clip(CircleShape)
                                            .clickable { step = "gallery" },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Outlined.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }
                                
                                val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                val isPressed by interactionSource.collectIsPressedAsState()
                                val nextScale by androidx.compose.animation.core.animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f)
                                Button(
                                    onClick = { onPublish(capturedMedia?.toString() ?: "", mediaType) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E90FF)),
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                                    interactionSource = interactionSource,
                                    modifier = Modifier.graphicsLayer(scaleX = nextScale, scaleY = nextScale)
                                ) {
                                    Text(com.example.ui.i18n.LocalTranslation.current.next, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Outlined.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                            // Drawing Mode Overlay
                            if (isDrawingMode) {
                                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.2f))) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(onClick = { if(paths.isNotEmpty()) paths.removeLast() }) {
                                            Icon(Icons.Outlined.Undo, contentDescription = "Undo", tint = if (paths.isEmpty()) Color.White.copy(alpha=0.5f) else Color.White)
                                        }
                                        TextButton(onClick = { paths.clear() }) {
                                            Text(com.example.ui.i18n.LocalTranslation.current.clearAll, color = Color.White, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                    
                                    Column(
                                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black).navigationBarsPadding().padding(vertical = 16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        // Colors and Tools
                                            if (showShapesMenu) {
                                                Column(
                                                    modifier = Modifier
                                                        .align(Alignment.End)
                                                        .padding(bottom = 8.dp, end = 24.dp)
                                                        .background(Color(0xFF2C2C2E), RoundedCornerShape(16.dp))
                                                        .width(180.dp)
                                                        .padding(vertical = 8.dp)
                                                ) {
                                                    listOf("Circle" to Icons.Outlined.Circle, "Rectangle" to Icons.Outlined.CropSquare, "Star" to Icons.Outlined.StarBorder, "Bubble" to Icons.Outlined.ChatBubbleOutline, "Arrow" to Icons.Outlined.CallMade).forEach { (name, icon) ->
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth().clickable { showShapesMenu = false }.padding(horizontal = 20.dp, vertical = 12.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(icon, contentDescription = name, tint = Color.White, modifier = Modifier.size(20.dp))
                                                            Spacer(modifier = Modifier.width(16.dp))
                                                            Text(name, color = Color.White, fontSize = 15.sp)
                                                        }
                                                    }
                                                }
                                            }
                                            Box(
                                            modifier = Modifier
                                                .padding(horizontal = 16.dp)
                                                .background(Color(0xFF1C1C1E), RoundedCornerShape(24.dp))
                                                .padding(8.dp)
                                                .fillMaxWidth(0.9f)
                                        ) {
                                            if (showColorPicker) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    IconButton(onClick = { showColorPicker = false }, modifier = Modifier.size(32.dp)) {
                                                        Icon(Icons.Outlined.ChevronLeft, contentDescription = null, tint = Color.White)
                                                    }
                                                    val colors = listOf(Color.Red, Color(0xFFFF9500), Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color(0xFF5856D6), Color.Black, Color.White)
                                                    colors.forEach { c ->
                                                        Box(
                                                            modifier = Modifier
                                                                .size(24.dp)
                                                                .clip(CircleShape)
                                                                .background(c)
                                                                .border(2.dp, if(drawColor == c) Color.White else Color.Transparent, CircleShape)
                                                                .clickable { drawColor = c; showColorPicker = false }
                                                        )
                                                    }
                                                }
                                            } else {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .clip(CircleShape)
                                                            .background(drawColor)
                                                            .border(2.dp, Color.White, CircleShape)
                                                            .clickable { showColorPicker = true }
                                                    )
                                                    
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        listOf("pen" to Icons.Outlined.Edit, "arrow" to Icons.Outlined.CallMade, "marker" to Icons.Outlined.Highlight, "neon" to Icons.Outlined.AutoAwesome, "eraser" to Icons.Outlined.CleaningServices).forEach { (tool, icon) ->
                                                            IconButton(
                                                                onClick = { drawTool = tool },
                                                                modifier = Modifier.background(if (drawTool == tool) Color.White.copy(alpha=0.2f) else Color.Transparent, CircleShape)
                                                            ) {
                                                                Icon(icon, contentDescription = tool, tint = Color.White)
                                                            }
                                                        }
                                                    }
                                                    
                                                    IconButton(onClick = { showShapesMenu = !showShapesMenu }) {
                                                        Icon(Icons.Outlined.Add, contentDescription = "Shapes", tint = Color.White)
                                                    }
                                                }
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(24.dp))
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(onClick = { isDrawingMode = false }) {
                                                Icon(Icons.Outlined.Close, contentDescription = "Close", tint = Color.White)
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                                Text(com.example.ui.i18n.LocalTranslation.current.draw, color = Color.White, fontWeight = FontWeight.Bold)
                                                Text(com.example.ui.i18n.LocalTranslation.current.sticker, color = Color.White.copy(alpha=0.5f), fontWeight = FontWeight.Bold)
                                                Text("TEXT", color = Color.White.copy(alpha=0.5f), fontWeight = FontWeight.Bold, modifier = Modifier.clickable { isDrawingMode = false; isTextMode = true })
                                            }
                                            IconButton(onClick = { isDrawingMode = false }) {
                                                Icon(Icons.Outlined.Check, contentDescription = "Done", tint = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                            // Text Mode Overlay
                            androidx.compose.animation.AnimatedVisibility(
                                visible = isTextMode,
                                enter = androidx.compose.animation.fadeIn(),
                                exit = androidx.compose.animation.fadeOut(),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.6f))) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(onClick = { currentTextStyle = when(currentTextStyle) { "transparent" -> "filled"; "filled" -> "solid"; else -> "transparent" } }) {
                                            Icon(Icons.Outlined.AutoAwesome, contentDescription = "Style", tint = Color.White)
                                        }
                                        IconButton(onClick = { currentTextAlign = when(currentTextAlign) { TextAlign.Center -> TextAlign.Left; TextAlign.Left -> TextAlign.Right; else -> TextAlign.Center } }) {
                                            Icon(Icons.Outlined.FormatAlignCenter, contentDescription = "Align", tint = Color.White)
                                        }
                                        TextButton(onClick = {
                                            if (currentText.isNotBlank()) {
                                                storyTexts.add(StoryTextData(System.currentTimeMillis(), currentText, currentTextColor, currentTextAlign, currentTextStyle, Offset(0f, 0f), currentFont))
                                            }
                                            isTextMode = false
                                            currentText = ""
                                        }) {
                                            Text(com.example.ui.i18n.LocalTranslation.current.done, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        }
                                    }
                                    
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        TextField(
                                            value = currentText,
                                            onValueChange = { currentText = it },
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = when(currentTextStyle) {
                                                    "filled" -> Color.Black.copy(alpha=0.5f)
                                                    "solid" -> Color.White
                                                    else -> Color.Transparent
                                                },
                                                unfocusedContainerColor = when(currentTextStyle) {
                                                    "filled" -> Color.Black.copy(alpha=0.5f)
                                                    "solid" -> Color.White
                                                    else -> Color.Transparent
                                                },
                                                focusedTextColor = if(currentTextStyle == "solid") Color.Black else currentTextColor,
                                                unfocusedTextColor = if(currentTextStyle == "solid") Color.Black else currentTextColor,
                                                cursorColor = currentTextColor,
                                                focusedIndicatorColor = Color.Transparent,
                                                unfocusedIndicatorColor = Color.Transparent
                                            ),
                                            textStyle = androidx.compose.ui.text.TextStyle(
                                                fontSize = 32.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = currentTextAlign,
                                                fontFamily = currentFont,
                                                shadow = if (currentTextStyle == "transparent") Shadow(Color.Black.copy(alpha=0.5f), offset = Offset(0f, 4f), blurRadius = 8f) else null
                                            ),
                                            modifier = Modifier.fillMaxWidth(0.8f).clip(RoundedCornerShape(16.dp)),
                                            placeholder = {
                                                Text(com.example.ui.i18n.LocalTranslation.current.addText, color = Color.White.copy(alpha=0.5f), textAlign = currentTextAlign, modifier = Modifier.fillMaxWidth(), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                                            }
                                        )
                                    }
                                    
                                    Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp)) {
                                        if (showFontMenu) {
                                            Column(
                                                modifier = Modifier
                                                    .align(Alignment.End)
                                                    .padding(bottom = 8.dp, end = 24.dp)
                                                    .background(Color(0xFF2C2C2E), RoundedCornerShape(16.dp))
                                                    .width(180.dp)
                                                    .heightIn(max = 250.dp)
                                                    .verticalScroll(rememberScrollState())
                                                    .padding(vertical = 8.dp)
                                            ) {
                                                listOf("Roboto", "Impact", "Courier New", "Comic Sans MS", "Georgia", "Playfair", "Arial").forEach { fontName ->
                                                    Text(
                                                        text = fontName,
                                                        color = Color.White,
                                                        modifier = Modifier.fillMaxWidth().clickable { currentFontName = fontName; showFontMenu = false }.padding(horizontal = 20.dp, vertical = 12.dp)
                                                    )
                                                }
                                            }
                                        }
                                        if (showTextColorPicker) {
                                            Column(
                                                modifier = Modifier.padding(horizontal = 16.dp).background(Color(0xFF1C1C1E), RoundedCornerShape(24.dp)).padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    val row1 = listOf(Color.Red, Color(0xFFFF9500), Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color(0xFF5856D6))
                                                    row1.forEach { c -> Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(c).border(2.dp, if(currentTextColor == c) Color.White else Color.Transparent, CircleShape).clickable { currentTextColor = c; showTextColorPicker = false }) }
                                                }
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    val row2 = listOf(Color.Black, Color.White, Color.LightGray, Color.Gray, Color.DarkGray, Color.Magenta, Color.LightGray)
                                                    row2.forEach { c -> Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(c).border(2.dp, if(currentTextColor == c) Color.White else Color.Transparent, CircleShape).clickable { currentTextColor = c; showTextColorPicker = false }) }
                                                }
                                            }
                                        } else {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).background(Color(0xFF1C1C1E), RoundedCornerShape(32.dp)).padding(horizontal = 16.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red))).border(2.dp, Color.White, CircleShape).clickable { showTextColorPicker = true }
                                                )
                                                IconButton(onClick = {
                                                    currentTextAlign = when(currentTextAlign) {
                                                        TextAlign.Center -> TextAlign.Left
                                                        TextAlign.Left -> TextAlign.Right
                                                        else -> TextAlign.Center
                                                    }
                                                }) {
                                                    Icon(when(currentTextAlign){ TextAlign.Left -> Icons.Outlined.FormatAlignLeft; TextAlign.Right -> Icons.Outlined.FormatAlignRight; else -> Icons.Outlined.FormatAlignCenter }, contentDescription = null, tint = Color.White)
                                                }
                                                IconButton(onClick = {
                                                    currentTextStyle = when(currentTextStyle) {
                                                        "transparent" -> "filled"
                                                        "filled" -> "solid"
                                                        else -> "transparent"
                                                    }
                                                }) {
                                                    Text("A", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                                                }
                                                Row(
                                                    modifier = Modifier.background(Color.White.copy(alpha=0.1f), RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 8.dp).clickable { showFontMenu = !showFontMenu },
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Outlined.UnfoldMore, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(currentFontName, color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                    }
                "gallery" -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.6f)).clickable { step = "camera" })
                        
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .fillMaxHeight(0.85f)
                                .background(Color(0xFF1C1C1D), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        ) {
                            Text(
                                text = "Choose photo or video",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(20.dp)
                            )
                            
                            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
                                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp)
                                            .height(60.dp)
                                            .background(Color(0xFF2C2C2E), RoundedCornerShape(12.dp))
                                            .clickable { galleryLauncher.launch("image/*") },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Outlined.Folder, contentDescription = null, tint = Color.White)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(com.example.ui.i18n.LocalTranslation.current.browseAllFolders, color = Color.White, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                                items(recentPhotos.size) { index ->
                                    val uri = recentPhotos[index]
                                    val selectedIndex = selectedGalleryMedias.indexOf(uri)
                                    val isSelected = selectedIndex != -1
                                    
                                    Box(
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                if (isSelected) {
                                                    selectedGalleryMedias.remove(uri)
                                                } else {
                                                    selectedGalleryMedias.add(uri)
                                                }
                                            }
                                    ) {
                                        AsyncImage(
                                            model = uri,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize().then(
                                                if (isSelected) Modifier.alpha(0.8f).scale(1.1f) else Modifier
                                            )
                                        )
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(8.dp)
                                                    .size(24.dp)
                                                    .align(Alignment.TopEnd)
                                                    .background(Color(0xFF1E90FF), CircleShape)
                                                    .border(2.dp, Color.White, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("${selectedIndex + 1}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .padding(8.dp)
                                                    .size(24.dp)
                                                    .align(Alignment.TopEnd)
                                                    .background(Color.Black.copy(alpha=0.2f), CircleShape)
                                                    .border(2.dp, Color.White.copy(alpha=0.5f), CircleShape)
                                            )
                                        }
                                    }
                                }
                            }
                            
                            
                            Divider(color = Color.White.copy(alpha = 0.1f))
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { 
                                        if (selectedGalleryMedias.isNotEmpty()) {
                                            if (capturedMedia != null) {
                                                // Just add as stickers
                                                selectedGalleryMedias.forEachIndexed { i, uri ->
                                                    imageStickers.add(ImageSticker(id = System.currentTimeMillis() + i, uri = uri))
                                                }
                                            } else {
                                                // Start new
                                                capturedMedia = selectedGalleryMedias[0]
                                                mediaType = "image"
                                                imageStickers.clear()
                                                selectedGalleryMedias.drop(1).forEachIndexed { i, uri ->
                                                    imageStickers.add(ImageSticker(id = System.currentTimeMillis() + i, uri = uri))
                                                }
                                            }
                                            step = "editor"
                                            selectedGalleryMedias.clear()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = if (selectedGalleryMedias.isNotEmpty()) Color(0xFF1E90FF) else Color.White.copy(alpha=0.1f)),
                                    enabled = selectedGalleryMedias.isNotEmpty()
                                ) {
                                    Text(com.example.ui.i18n.LocalTranslation.current.next, fontWeight = FontWeight.Bold, color = if (selectedGalleryMedias.isNotEmpty()) Color.White else Color.White.copy(alpha=0.5f))
                                    if (selectedGalleryMedias.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier.size(20.dp).background(Color.White, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("${selectedGalleryMedias.size}", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Button(
                                    onClick = { },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha=0.1f))
                                ) {
                                    Icon(Icons.Outlined.Dashboard, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(com.example.ui.i18n.LocalTranslation.current.collage, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}
