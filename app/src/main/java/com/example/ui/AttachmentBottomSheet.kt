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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class MediaItem(val id: Long, val uri: Uri, val type: Int, val bucketName: String)
data class RecentFileItem(val id: Long, val uri: Uri, val name: String, val size: Long)
enum class AttachmentPickerTab { PHOTOS, VIDEOS, FILES }

// Remembers the real display name/size for a file the user just picked from the Files
// tab, keyed by its Uri string. The Uri alone (especially a plain file:// path found via
// the filesystem fallback below) isn't always re-queryable for metadata later in the send
// pipeline, so we stash what we already know here instead of re-deriving it.
object PickedFileMetaCache {
    val map = mutableMapOf<String, Pair<String, Long?>>()
}

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
                    while (cursor.moveToNext() && count < 1000) {
                        val id = cursor.getLong(idColumn)
                        val type = cursor.getInt(typeColumn)
                        val contentUri = ContentUris.withAppendedId(queryUri, id)
                        items.add(MediaItem(id, contentUri, type, "أخرى"))
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

// ============================================================================
// Telegram-style in-place attachment picker: replaces the keyboard (same
// height) instead of opening as a floating dialog, and is split into two
// tabs — "الصور" (Photos) and "الملفات" (Files) — that swap the content
// below them depending on which one is selected.
// ============================================================================

@Composable
fun AttachmentPickerPanel(
    panelHeight: Dp,
    onAttachmentSelected: (List<Uri>, AttachmentType) -> Unit,
    onOpenGifPicker: () -> Unit = {},
    allowPoll: Boolean = false,
    onPollClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(AttachmentPickerTab.PHOTOS) }

    var mediaItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var selectedAlbum by remember { mutableStateOf<String?>(null) }
    val selectedUris = remember { mutableStateListOf<Uri>() }
    var hasMediaPermission by remember { mutableStateOf(false) }
    var mediaLoaded by remember { mutableStateOf(false) }

    var currentDir by remember { mutableStateOf<java.io.File?>(null) }
    var dirEntries by remember { mutableStateOf<List<java.io.File>>(emptyList()) }
    var dirLoading by remember { mutableStateOf(false) }
    var hasAllFilesAccess by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.os.Environment.isExternalStorageManager()
            } else {
                // Bug fixed here: this used to be hardcoded to `true` on pre-Android-11
                // devices, which skipped ever actually requesting READ_EXTERNAL_STORAGE -
                // the folder browser then failed silently (canRead=false, listFiles=null)
                // because the permission was never really granted.
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_EXTERNAL_STORAGE
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val mediaPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results -> hasMediaPermission = results.values.any { it } }
    val filesPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasAllFilesAccess = granted }

    LaunchedEffect(Unit) {
        // Android 14+ (API 34) lets the user grant "Select photos" instead of "Allow all",
        // which grants READ_MEDIA_VISUAL_USER_SELECTED instead of READ_MEDIA_IMAGES - check
        // for either so partial access doesn't keep re-prompting or reporting "no permission".
        val partialAccessPermission = if (Build.VERSION.SDK_INT >= 34) {
            "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"
        } else null

        val hasImages = androidx.core.content.ContextCompat.checkSelfPermission(
            context, mediaPermission
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        // من أندرويد 13 صلاحية الفيديو منفصلة عن الصور
        val hasVideos = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_MEDIA_VIDEO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasFull = hasImages && hasVideos
        val hasPartial = partialAccessPermission != null &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, partialAccessPermission
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasFull || hasPartial) {
            hasMediaPermission = true
        } else {
            val videoPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_VIDEO
            } else null
            val toRequest = listOfNotNull(mediaPermission, videoPermission, partialAccessPermission)
            mediaPermissionLauncher.launch(toRequest.toTypedArray())
        }
    }

    LaunchedEffect(hasMediaPermission) {
        if (hasMediaPermission) {
            mediaItems = loadRecentMedia(context)
            mediaLoaded = true
        }
    }

    // Re-check "all files access" whenever the app comes back to the foreground (the user
    // grants it in a system Settings screen, then returns here) so the browser starts
    // itself with no extra taps once permission is granted.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val nowGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    android.os.Environment.isExternalStorageManager()
                } else {
                    androidx.core.content.ContextCompat.checkSelfPermission(
                        context, Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                }
                if (nowGranted != hasAllFilesAccess) {
                    hasAllFilesAccess = nowGranted
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Legacy (pre-Android 11) devices: MANAGE_EXTERNAL_STORAGE doesn't exist, so request the
    // normal runtime storage permission once when the Files tab is opened and not yet granted.
    LaunchedEffect(selectedTab) {
        if (selectedTab == AttachmentPickerTab.FILES &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.R && !hasAllFilesAccess
        ) {
            filesPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    // Start browsing at the storage root the first time the Files tab becomes usable.
    LaunchedEffect(selectedTab, hasAllFilesAccess) {
        if (selectedTab == AttachmentPickerTab.FILES && hasAllFilesAccess && currentDir == null) {
            currentDir = android.os.Environment.getExternalStorageDirectory()
        }
    }

    var dirDebugInfo by remember { mutableStateOf("") }

    // Load the current folder's contents whenever it changes - a real, navigable file
    // browser (folders first, then files, alphabetical) exactly like Telegram's own
    // "Internal storage" / "External storage" browser.
    LaunchedEffect(currentDir) {
        val dir = currentDir ?: return@LaunchedEffect
        dirLoading = true
        var debug = "path=${dir.path} exists=${dir.exists()} canRead=${dir.canRead()}"
        dirEntries = withContext(Dispatchers.IO) {
            try {
                val raw = dir.listFiles()
                debug += " listFiles=${if (raw == null) "null" else raw.size.toString()}"
                raw
                    ?.filter { !it.isHidden }
                    ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                    ?: emptyList()
            } catch (e: Exception) {
                debug += " exception=${e.javaClass.simpleName}:${e.message}"
                emptyList()
            }
        }
        dirDebugInfo = debug
        dirLoading = false
    }


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(panelHeight)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // مقبض السحب العلوي (مثل تيليجرام)
        Box(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.width(36.dp).height(4.dp).clip(CircleShape).background(Color.Gray.copy(alpha = 0.5f)))
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedTab) {
                AttachmentPickerTab.PHOTOS, AttachmentPickerTab.VIDEOS -> {
                    val wantVideo = selectedTab == AttachmentPickerTab.VIDEOS
                    val tabItems = remember(mediaItems, wantVideo) {
                        mediaItems.filter {
                            (it.type == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) == wantVideo
                        }
                    }
                    if (tabItems.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (hasMediaPermission && !mediaLoaded) {
                                CircularProgressIndicator()
                            } else if (hasMediaPermission) {
                                Text(
                                    if (wantVideo) "لا توجد فيديوهات على الهاتف" else "لا توجد صور على الهاتف",
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(24.dp)
                                )
                            } else {
                                Text(
                                    "يحتاج التطبيق إلى إذن للوصول إلى الصور والفيديو",
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(24.dp)
                                )
                            }
                        }
                    } else {
                        val albums = remember(tabItems) {
                            tabItems.groupBy { it.bucketName }
                                .toList()
                                .sortedByDescending { (_, items) -> items.maxOf { it.id } }
                        }
                        val album = selectedAlbum
                        val itemsToShow = if (album != null) {
                            tabItems.filter { it.bucketName == album }
                        } else emptyList()

                        Column(Modifier.fillMaxSize()) {
                            if (album != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedAlbum = null }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.Image, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text("..", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(Modifier.width(8.dp))
                                    Text(album, fontSize = 12.sp, color = Color.Gray)
                                }
                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                            }

                            Box(Modifier.weight(1f).fillMaxWidth()) {
                                if (album == null) {
                                    // Album grid: one cover thumbnail per folder, like Telegram's gallery.
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(2),
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(6.dp, 6.dp, 6.dp, 66.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(albums, key = { it.first }) { (bucketName, bucketItems) ->
                                            Column(
                                                modifier = Modifier.clickable { selectedAlbum = bucketName }
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .aspectRatio(1f)
                                                        .clip(RoundedCornerShape(10.dp))
                                                ) {
                                                    MediaThumb(
                                                        item = bucketItems.first(),
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    bucketName,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text("${bucketItems.size}", fontSize = 11.sp, color = Color.Gray)
                                            }
                                        }
                                    }
                                } else {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(3),
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(2.dp, 2.dp, 2.dp, 66.dp),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        items(itemsToShow, key = { it.uri.toString() }) { item ->
                                            val isSelected = selectedUris.contains(item.uri)
                                            Box(
                                                modifier = Modifier
                                                    .aspectRatio(1f)
                                                    .clickable {
                                                        if (isSelected) selectedUris.remove(item.uri)
                                                        else selectedUris.add(item.uri)
                                                    }
                                            ) {
                                                MediaThumb(
                                                    item = item,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                if (item.type == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                                                    Icon(
                                                        imageVector = Icons.Filled.PlayArrow,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier
                                                            .padding(6.dp)
                                                            .size(22.dp)
                                                            .align(Alignment.BottomStart)
                                                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                                            .padding(2.dp)
                                                    )
                                                }
                                                if (isSelected) {
                                                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
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
                                }
                                if (selectedUris.isNotEmpty()) {
                                    FloatingActionButton(
                                        onClick = {
                                            onAttachmentSelected(
                                                selectedUris.toList(),
                                                if (wantVideo) AttachmentType.VIDEO else AttachmentType.IMAGE
                                            )
                                            selectedUris.clear()
                                        },
                                        modifier = Modifier.align(Alignment.BottomEnd).padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 66.dp),
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
                AttachmentPickerTab.FILES -> {
                    if (!hasAllFilesAccess) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFF007AFF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.Folder, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                                }
                                Spacer(Modifier.height(14.dp))
                                Text(
                                    "لعرض ملفاتك هنا مباشرة (بدون أي نافذة اختيار) يحتاج التطبيق إذن الوصول لكل الملفات — مرة واحدة فقط",
                                    textAlign = TextAlign.Center,
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                                Spacer(Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                                            // Pre-Android-11: just the normal runtime permission dialog.
                                            filesPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                                        } else {
                                            try {
                                                val intent = android.content.Intent(
                                                    android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                                    android.net.Uri.fromParts("package", context.packageName, null)
                                                )
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                try {
                                                    context.startActivity(
                                                        android.content.Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                                    )
                                                } catch (e2: Exception) {}
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
                                ) {
                                    Text("منح الإذن", color = Color.White)
                                }
                            }
                        }
                    } else {
                        val rootPath = android.os.Environment.getExternalStorageDirectory().path
                        val dir = currentDir
                        Column(Modifier.fillMaxSize()) {
                            if (dir != null && dir.path != rootPath) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { currentDir = dir.parentFile ?: android.os.Environment.getExternalStorageDirectory() }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.Folder, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text("..", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(Modifier.width(8.dp))
                                    Text(dir.name, fontSize = 12.sp, color = Color.Gray)
                                }
                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                            }
                            Box(Modifier.weight(1f).fillMaxWidth()) {
                                if (dirLoading) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                                } else if (dirEntries.isEmpty()) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                                            Text("المجلد فارغ", color = Color.Gray)
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                dirDebugInfo,
                                                color = Color.Gray,
                                                fontSize = 10.sp,
                                                textAlign = TextAlign.Center
                                            )
                                            Text(
                                                "isExternalStorageManager=${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) android.os.Environment.isExternalStorageManager() else "n/a"}",
                                                color = Color.Gray,
                                                fontSize = 10.sp,
                                                textAlign = TextAlign.Center
                                            )
                                            Text(
                                                "hasAllFilesAccess=$hasAllFilesAccess sdk=${Build.VERSION.SDK_INT} versionCode=${com.example.BuildConfig.VERSION_CODE}",
                                                color = Color.Red,
                                                fontSize = 10.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                } else {
                                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 66.dp)) {
                                        items(dirEntries, key = { it.absolutePath }) { entry ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        if (entry.isDirectory) {
                                                            currentDir = entry
                                                        } else {
                                                            val uri = Uri.fromFile(entry)
                                                            PickedFileMetaCache.map[uri.toString()] = Pair(entry.name, entry.length())
                                                            onAttachmentSelected(listOf(uri), AttachmentType.DOCUMENT)
                                                        }
                                                    }
                                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier.size(38.dp).clip(CircleShape)
                                                        .background(if (entry.isDirectory) Color(0xFF007AFF) else Color(0xFF673AB7)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        if (entry.isDirectory) Icons.Outlined.Folder else Icons.Outlined.Description,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Spacer(Modifier.width(12.dp))
                                                Column(Modifier.weight(1f)) {
                                                    Text(
                                                        entry.name,
                                                        fontSize = 15.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    if (!entry.isDirectory) {
                                                        Text(formatFileSize(entry.length()), fontSize = 12.sp, color = Color.Gray)
                                                    } else {
                                                        Text("مجلد", fontSize = 12.sp, color = Color.Gray)
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
            }
            // شريط التبويبات السفلي العائم بنمط تيليجرام: فوق المحتوى، مع مؤشر منزلق
            PickerBottomBar(
                selected = selectedTab,
                showPoll = allowPoll,
                onPoll = onPollClick,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) { tab ->
                selectedTab = tab
                if (tab != AttachmentPickerTab.FILES) {
                    selectedAlbum = null
                    selectedUris.clear()
                }
            }
        }
    }
}

@Composable
private fun PickerBottomBar(
    selected: AttachmentPickerTab,
    showPoll: Boolean = false,
    onPoll: () -> Unit = {},
    modifier: Modifier = Modifier,
    onSelect: (AttachmentPickerTab) -> Unit
) {
    val theme = LocalSettingsTheme.current.theme
    val accent = MaterialTheme.colorScheme.primary
    // (التبويب، الأيقونة، العنوان، تدرّج لون الشارة) — شارات ملوّنة بدل أيقونات رمادية
    val items = buildList<List<Any?>> {
        add(listOf(AttachmentPickerTab.PHOTOS, Icons.Outlined.Image, "المعرض", Color(0xFF34D399), Color(0xFF059669)))
        add(listOf(AttachmentPickerTab.VIDEOS, Icons.Outlined.Videocam, "الفيديو", Color(0xFFFB7185), Color(0xFFE11D48)))
        add(listOf(AttachmentPickerTab.FILES, Icons.Outlined.Description, "ملف", Color(0xFF60A5FA), Color(0xFF2563EB)))
        // استفتاء: يظهر في القنوات والمجموعات فقط (ليس في الدردشة الفردية)
        if (showPoll) add(listOf(null, Icons.Outlined.Poll, "استفتاء", Color(0xFFA78BFA), Color(0xFF7C3AED)))
    }
    val rtl = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
    val selIndex = items.indexOfFirst { it[0] == selected }.coerceAtLeast(0)
    val barBg = if (theme.isDark) Color(0xFF1F1F1F).copy(alpha = 0.97f) else Color(0xFFF2F2F7).copy(alpha = 0.97f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
            .height(46.dp)   // بنفس ارتفاع كارد الكتابة تقريبًا
            .shadow(4.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.2f), spotColor = Color.Black.copy(alpha = 0.2f))
            .clip(CircleShape)
            .background(barBg)
            .padding(3.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val tabWidth = maxWidth / items.size
            val visualIndex = if (rtl) items.size - 1 - selIndex else selIndex
            val indicatorOffset by androidx.compose.animation.core.animateDpAsState(
                targetValue = tabWidth * visualIndex,
                animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.75f, stiffness = 400f),
                label = "picker_pill"
            )
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(tabWidth)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(accent.copy(alpha = if (theme.isDark) 0.28f else 0.16f))
            )
            Row(modifier = Modifier.fillMaxSize()) {
                items.forEach { item ->
                    val tab = item[0] as AttachmentPickerTab?
                    val icon = item[1] as ImageVector
                    val label = item[2] as String
                    val c1 = item[3] as Color
                    val c2 = item[4] as Color
                    val isSel = tab != null && tab == selected
                    val tint by androidx.compose.animation.animateColorAsState(
                        if (isSel) accent else theme.textSecondary, label = "picker_tint"
                    )
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) { if (tab == null) onPoll() else onSelect(tab) },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(c1, c2))),
                            contentAlignment = Alignment.Center
                        ) { Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(16.dp)) }
                        Spacer(Modifier.width(6.dp))
                        Text(label, color = tint, fontSize = 12.sp, fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerTabButton(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (selected) Color(0xFF007AFF) else Color(0xFFEFEFEF)
    val fg = if (selected) Color.White else Color(0xFF555555)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = fg, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = fg, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun RecentFilesList(files: List<RecentFileItem>, onFileClick: (Uri) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 66.dp)) {
        items(files, key = { it.id }) { file ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        PickedFileMetaCache.map[file.uri.toString()] = Pair(file.name, file.size)
                        onFileClick(file.uri)
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val ext = file.name.substringAfterLast('.', "").uppercase().take(4)
                Box(
                    modifier = Modifier.size(42.dp).clip(CircleShape).background(Color(0xFF673AB7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Description,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        file.name,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val sub = listOf(formatFileSize(file.size), ext).filter { it.isNotBlank() }.joinToString("  ·  ")
                    if (sub.isNotBlank()) Text(sub, fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}

private suspend fun loadRecentMedia(context: android.content.Context): List<MediaItem> =
    withContext(Dispatchers.IO) {
        val items = mutableListOf<MediaItem>()
        try {
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.MEDIA_TYPE,
                MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME
            )
            val selection = (MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                    + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                    + " OR "
                    + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                    + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)
            val queryUri = MediaStore.Files.getContentUri("external")
            context.contentResolver.query(
                queryUri, projection, selection, null,
                MediaStore.Files.FileColumns.DATE_ADDED + " DESC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val typeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                val bucketColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
                var count = 0
                // No artificial low cap - a real gallery should show everything, grouped by
                // album below so the user isn't scrolling one giant flat grid.
                while (cursor.moveToNext() && count < 5000) {
                    val id = cursor.getLong(idColumn)
                    val type = cursor.getInt(typeColumn)
                    val bucket = if (bucketColumn >= 0) cursor.getString(bucketColumn) else null
                    // الفيديو من مجموعة الفيديو الرسمية حتى تعمل المصغّرة بشكل صحيح
                    val itemUri = if (type == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                        ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    } else {
                        ContentUris.withAppendedId(queryUri, id)
                    }
                    items.add(MediaItem(id, itemUri, type, bucket ?: "أخرى"))
                    count++
                }
            }
        } catch (e: Exception) {
            // permission or provider issue - show empty state instead of crashing
        }
        items
    }

// Recent files = merges two sources so it works reliably across OEM Android skins:
// 1) The Downloads collection (needs no extra permission on API 29+, where browsers/
//    Telegram/etc. save received files).
// 2) A broad MediaStore.Files query for anything that isn't image/video/audio (some
//    OEM skins don't populate the Downloads collection consistently, so this is a
//    second pass that often finds files the first query misses).
// Results are merged and de-duplicated by name+size.
private suspend fun loadRecentFiles(context: android.content.Context): List<RecentFileItem> =
    withContext(Dispatchers.IO) {
        val items = mutableListOf<RecentFileItem>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val queryUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                val projection = arrayOf(
                    MediaStore.Downloads._ID,
                    MediaStore.Downloads.DISPLAY_NAME,
                    MediaStore.Downloads.SIZE
                )
                context.contentResolver.query(
                    queryUri, projection, null, null,
                    MediaStore.Downloads.DATE_ADDED + " DESC"
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.SIZE)
                    var count = 0
                    while (cursor.moveToNext() && count < 50) {
                        val id = cursor.getLong(idCol)
                        val name = cursor.getString(nameCol) ?: "file"
                        val size = cursor.getLong(sizeCol)
                        items.add(RecentFileItem(id, ContentUris.withAppendedId(queryUri, id), name, size))
                        count++
                    }
                }
            } catch (e: Exception) {
                // Downloads collection unavailable on this OEM/ROM - fall through to the
                // broader MediaStore.Files query below instead of giving up entirely.
            }
        }

        // Always also try the broad Files query (not just as an API<29 fallback) since
        // some devices don't reliably populate the Downloads collection.
        try {
            val queryUri = MediaStore.Files.getContentUri("external")
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.SIZE
            )
            val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} != ? AND " +
                    "${MediaStore.Files.FileColumns.MEDIA_TYPE} != ? AND " +
                    "${MediaStore.Files.FileColumns.MEDIA_TYPE} != ?"
            val args = arrayOf(
                MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
                MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO.toString()
            )
            context.contentResolver.query(
                queryUri, projection, selection, args,
                MediaStore.Files.FileColumns.DATE_ADDED + " DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                var count = 0
                while (cursor.moveToNext() && count < 50) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "file"
                    val size = cursor.getLong(sizeCol)
                    items.add(RecentFileItem(id, ContentUris.withAppendedId(queryUri, id), name, size))
                    count++
                }
            }
        } catch (e: Exception) {
            // permission or provider issue - the Downloads results above (if any) still stand
        }

        // Third pass: a bounded recursive scan of external storage itself. Once the user
        // has granted "all files access" (MANAGE_EXTERNAL_STORAGE, API 30+) this is the
        // most reliable source of all - it doesn't depend on MediaStore indexing anything
        // correctly. Depth-limited; skips only Android/data and Android/obb (huge, private
        // app-internal caches) but still walks into Android/media, since that's exactly
        // where Telegram, WhatsApp, etc. keep the files/documents they've received on
        // modern (scoped-storage) Android versions.
        val mediaExtensions = setOf(
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic",
            "mp4", "mkv", "webm", "3gp", "mov",
            "mp3", "m4a", "aac", "ogg", "wav"
        )
        try {
            val root = android.os.Environment.getExternalStorageDirectory()
            fun scan(dir: java.io.File, depth: Int) {
                if (depth > 6 || items.size > 200) return
                if (dir.name == "data" || dir.name == "obb") {
                    val parent = dir.parentFile?.name
                    if (parent == "Android") return
                }
                val children = dir.listFiles() ?: return
                for (f in children) {
                    if (items.size > 200) return
                    if (f.isDirectory) {
                        scan(f, depth + 1)
                    } else if (f.isFile) {
                        val ext = f.name.substringAfterLast('.', "").lowercase()
                        if (ext !in mediaExtensions && !f.isHidden) {
                            items.add(RecentFileItem(f.lastModified(), Uri.fromFile(f), f.name, f.length()))
                        }
                    }
                }
            }
            if (root.exists() && root.canRead()) {
                scan(root, 0)
            }
        } catch (e: Exception) {
            // no filesystem access on this device/API level - the queries above still stand
        }

        // De-dupe (the same physical file can show up from more than one source), sort by
        // most recently modified first, and cap the list.
        items.distinctBy { "${it.name}_${it.size}" }
            .sortedByDescending { it.id }
            .take(60)
    }


// مصغّرة لعنصر في المعرض: صورة عادية أو لقطة من الفيديو (Coil لا يفك الفيديو وحده)
@Composable
private fun MediaThumb(item: MediaItem, modifier: Modifier = Modifier) {
    if (item.type != MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
        AsyncImage(
            model = item.uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
        return
    }
    val context = LocalContext.current
    val thumb by produceState<android.graphics.Bitmap?>(initialValue = null, item.uri) {
        value = withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(item.uri, android.util.Size(320, 320), null)
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Video.Thumbnails.getThumbnail(
                        context.contentResolver, item.id, MediaStore.Video.Thumbnails.MINI_KIND, null
                    )
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    Box(modifier = modifier.background(Color(0xFF222222))) {
        thumb?.let {
            androidx.compose.foundation.Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
