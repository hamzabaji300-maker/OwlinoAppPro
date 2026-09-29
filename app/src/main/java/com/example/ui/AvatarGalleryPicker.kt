package com.example.ui

import android.Manifest
import android.content.ContentUris
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarGalleryBottomSheet(
    onDismiss: () -> Unit,
    onImagePicked: (Uri) -> Unit
) {
    val t = com.example.ui.i18n.LocalTranslation.current
    val context = LocalContext.current
    var mediaItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var hasPermission by remember { mutableStateOf(false) }

    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> hasPermission = isGranted }

    LaunchedEffect(Unit) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, permission) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            hasPermission = true
        } else {
            permissionLauncher.launch(permission)
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            withContext(Dispatchers.IO) {
                val items = mutableListOf<MediaItem>()
                val projection = arrayOf(MediaStore.Images.Media._ID)
                val queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                context.contentResolver.query(
                    queryUri,
                    projection,
                    null,
                    null,
                    MediaStore.Images.Media.DATE_ADDED + " DESC"
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    var count = 0
                    while (cursor.moveToNext() && count < 150) {
                        val id = cursor.getLong(idColumn)
                        val contentUri = ContentUris.withAppendedId(queryUri, id)
                        items.add(MediaItem(id, contentUri, 1))
                        count++
                    }
                }
                withContext(Dispatchers.Main) { mediaItems = items }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Text(
                text = t.gallery,
                fontSize = 17.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            if (mediaItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    if (hasPermission) {
                        CircularProgressIndicator()
                    } else {
                        Text(t.mediaPermissionNeeded, modifier = Modifier.padding(horizontal = 24.dp))
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                    contentPadding = PaddingValues(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(mediaItems, key = { it.uri.toString() }) { item ->
                        AsyncImage(
                            model = item.uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clickable {
                                    onImagePicked(item.uri)
                                    onDismiss()
                                }
                        )
                    }
                }
            }
        }
    }
}

private fun decodeSampledBitmap(context: android.content.Context, uri: Uri, maxDim: Int): Bitmap? {
    return try {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        var sample = 1
        while (opts.outWidth / sample > maxDim || opts.outHeight / sample > maxDim) sample *= 2
        val opts2 = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts2) }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun CircularAvatarCropScreen(
    imageUri: Uri,
    onConfirm: (ByteArray) -> Unit,
    onCancel: () -> Unit
) {
    val t = com.example.ui.i18n.LocalTranslation.current
    val context = LocalContext.current
    val density = LocalDensity.current

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(imageUri) {
        bitmap = withContext(Dispatchers.IO) { decodeSampledBitmap(context, imageUri, 1600) }
    }

    val circleDiameterDp = 280.dp
    val circleDiameterPx = with(density) { circleDiameterDp.toPx() }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        val bmp = bitmap
        if (bmp != null) {
            val baseScale = circleDiameterPx / min(bmp.width, bmp.height).toFloat()
            val displayScale = baseScale * scale

            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.None,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = displayScale
                        scaleY = displayScale
                        translationX = offset.x
                        translationY = offset.y
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 4f)
                            offset += pan
                        }
                    }
            )

            // Mask: darken everything outside the crop circle
            Canvas(modifier = Modifier.fillMaxSize()) {
                val path = Path().apply {
                    addOval(
                        androidx.compose.ui.geometry.Rect(
                            center = center,
                            radius = circleDiameterPx / 2f
                        )
                    )
                }
                clipPath(path, clipOp = androidx.compose.ui.graphics.ClipOp.Difference) {
                    drawRect(color = Color.Black.copy(alpha = 0.6f))
                }
                drawCircle(
                    color = Color.White,
                    radius = circleDiameterPx / 2f,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        // Top bar: cancel + confirm
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Filled.Close, contentDescription = t.cancel, tint = Color.White)
            }
            Text(t.moveAndScale, color = Color.White, fontSize = 15.sp)
            IconButton(
                enabled = bitmap != null && !isSaving,
                onClick = {
                    val bmp = bitmap ?: return@IconButton
                    isSaving = true
                    val baseScale = circleDiameterPx / min(bmp.width, bmp.height).toFloat()
                    val displayScale = baseScale * scale
                    val bitmapCenterX = bmp.width / 2f
                    val bitmapCenterY = bmp.height / 2f
                    val r = circleDiameterPx / 2f
                    val cropSize = (2 * r) / displayScale
                    var cropLeft = bitmapCenterX - (r + offset.x) / displayScale
                    var cropTop = bitmapCenterY - (r + offset.y) / displayScale
                    cropLeft = cropLeft.coerceIn(0f, (bmp.width - cropSize).coerceAtLeast(0f))
                    cropTop = cropTop.coerceIn(0f, (bmp.height - cropSize).coerceAtLeast(0f))
                    val safeCropSize = min(cropSize, min(bmp.width - cropLeft, bmp.height - cropTop))
                    try {
                        val cropped = Bitmap.createBitmap(
                            bmp,
                            cropLeft.toInt().coerceAtLeast(0),
                            cropTop.toInt().coerceAtLeast(0),
                            safeCropSize.toInt().coerceAtLeast(1),
                            safeCropSize.toInt().coerceAtLeast(1)
                        )
                        val output = if (cropped.width > 640) {
                            Bitmap.createScaledBitmap(cropped, 640, 640, true)
                        } else cropped
                        val stream = ByteArrayOutputStream()
                        output.compress(Bitmap.CompressFormat.JPEG, 87, stream)
                        onConfirm(stream.toByteArray())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        isSaving = false
                    }
                }
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Check, contentDescription = t.done, tint = Color.White)
                }
            }
        }
    }
}
