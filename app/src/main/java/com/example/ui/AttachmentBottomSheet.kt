package com.example.ui

import android.Manifest
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class MediaItem(val id: Long, val uri: Uri, val type: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentBottomSheet(
    onDismiss: () -> Unit,
    onMediaSelected: (List<Uri>) -> Unit,
    onDocumentClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onLocationClick: () -> Unit,
    onCameraClick: () -> Unit,
    onContactClick: () -> Unit
) {
    val context = LocalContext.current
    var mediaItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    val selectedUris = remember { mutableStateListOf<Uri>() }
    var hasPermission by remember { mutableStateOf(false) }
    
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                permission
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
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
                val projection = arrayOf(
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.MEDIA_TYPE
                )
                val selection = (MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                        + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                        + " OR "
                        + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                        + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)
                        
                val queryUri = MediaStore.Files.getContentUri("external")
                
                context.contentResolver.query(
                    queryUri,
                    projection,
                    selection,
                    null,
                    MediaStore.Files.FileColumns.DATE_ADDED + " DESC"
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                    val typeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                    var count = 0
                    while (cursor.moveToNext() && count < 100) {
                        val id = cursor.getLong(idColumn)
                        val type = cursor.getInt(typeColumn)
                        val contentUri = ContentUris.withAppendedId(queryUri, id)
                        items.add(MediaItem(id, contentUri, type))
                        count++
                    }
                }
                withContext(Dispatchers.Main) {
                    mediaItems = items
                }
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
            // Action Buttons Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AttachmentOption(
                    icon = Icons.Outlined.Description,
                    label = "ملف",
                    color = Color(0xFF673AB7),
                    onClick = onDocumentClick
                )
                AttachmentOption(
                    icon = Icons.Outlined.CameraAlt,
                    label = "الكاميرا",
                    color = Color(0xFFE91E63),
                    onClick = onCameraClick
                )
                AttachmentOption(
                    icon = Icons.Outlined.Image,
                    label = "المعرض",
                    color = Color(0xFF9C27B0),
                    onClick = onGalleryClick
                )
                AttachmentOption(
                    icon = Icons.Outlined.LocationOn,
                    label = "الموقع",
                    color = Color(0xFF4CAF50),
                    onClick = onLocationClick
                )
                AttachmentOption(
                    icon = Icons.Outlined.Person,
                    label = "جهة اتصال",
                    color = Color(0xFF2196F3),
                    onClick = onContactClick
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
            
            // Media Grid
            if (mediaItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    if (hasPermission) {
                         CircularProgressIndicator()
                    } else {
                         Text("يحتاج التطبيق إلى إذن للوصول إلى الصور")
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(max = 350.dp)) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(mediaItems, key = { it.uri.toString() }) { item ->
                            val isSelected = selectedUris.contains(item.uri)
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clickable {
                                        if (isSelected) {
                                            selectedUris.remove(item.uri)
                                        } else {
                                            selectedUris.add(item.uri)
                                        }
                                    }
                            ) {
                                AsyncImage(
                                    model = item.uri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                // Selection overlay
                                if (isSelected) {
                                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .size(24.dp)
                                            .align(Alignment.TopEnd)
                                            .background(Color(0xFF4CAF50), CircleShape)
                                            .border(1.dp, Color.White, CircleShape)
                                            .padding(2.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .padding(6.dp)
                                            .size(20.dp)
                                            .align(Alignment.TopEnd)
                                            .border(1.5.dp, Color.White, CircleShape)
                                            .background(Color.Black.copy(alpha = 0.2f), CircleShape)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Floating Send Button if any selected
                    if (selectedUris.isNotEmpty()) {
                        FloatingActionButton(
                            onClick = { 
                                onMediaSelected(selectedUris.toList())
                                onDismiss()
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp),
                            containerColor = Color(0xFF4CAF50),
                            contentColor = Color.White
                        ) {
                            Icon(Icons.Filled.Send, contentDescription = "Send")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AttachmentOption(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(color = color, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}
