package com.example.ui

import com.example.ui.SettingsColors
import kotlinx.coroutines.launch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.example.ui.i18n.LocalTranslation
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


private val ColorPhotos = androidx.compose.ui.graphics.Color(0xFF10B981)
private val ColorVideos = androidx.compose.ui.graphics.Color(0xFF3B82F6)
private val ColorVoice = androidx.compose.ui.graphics.Color(0xFF8B5CF6)
private val ColorDocs = androidx.compose.ui.graphics.Color(0xFFF59E0B)

@Composable
fun StorageSettingsScreen(onBack: () -> Unit) {
        val context = androidx.compose.ui.platform.LocalContext.current
    val (privChats, setPrivChats) = rememberBooleanPreference("save_gallery_private", true)
    val (groups, setGroups) = rememberBooleanPreference("save_gallery_groups", false)
    val (channels, setChannels) = rememberBooleanPreference("save_gallery_channels", true)

    var cacheSizeMb by remember { mutableStateOf(0f) }
    var filesSizeMb by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val cSize = context.cacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum().toFloat() / (1024 * 1024)
            val fSize = context.filesDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum().toFloat() / (1024 * 1024)
            cacheSizeMb = cSize
            filesSizeMb = fSize
        }
    }
    
    val totalSizeMb = cacheSizeMb + filesSizeMb
    val maxGb = 16f
    val currentGb = totalSizeMb / 1024f
    val progressOverall = currentGb / maxGb
    val percentStr = String.format("%.1f%%", progressOverall * 100)
    val currentGbStr = String.format("%.2f", currentGb)
    val redAccent = SettingsColors.redAccent

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(SettingsColors.background)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Lucide.ArrowLeft,
                contentDescription = "Back",
                tint = SettingsColors.textPrimary,
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onBack() }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Storage and data",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = SettingsColors.textPrimary
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            item {
                Text(
                    text = "STORAGE USAGE",
                    fontSize = 12.sp, lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SettingsColors.textSecondary,
                    modifier = Modifier.padding(start = 32.dp, bottom = 8.dp, top = 8.dp)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(SettingsColors.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Text(currentGbStr, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp, color = SettingsColors.textPrimary)
                            Text(" / 16 GB", color = SettingsColors.textSecondary, fontSize = 15.sp, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
                            Spacer(modifier = Modifier.weight(1f))
                            Text(percentStr, color = SettingsColors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(bottom = 6.dp))
                        }
                        LinearProgressIndicator(
                            progress = { progressOverall },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = SettingsColors.textPrimary,
                            trackColor = SettingsColors.divider
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        UsageItemRow(icon = Lucide.Image, color = ColorPhotos, title = "Cache (Photos/Data)", size = String.format("%.1f MB", cacheSizeMb), progress = if (totalSizeMb > 0) cacheSizeMb / totalSizeMb else 0f)
                        Spacer(modifier = Modifier.height(16.dp))
                        UsageItemRow(icon = Lucide.Video, color = ColorVideos, title = "Downloads/Files", size = String.format("%.1f MB", filesSizeMb), progress = if (totalSizeMb > 0) filesSizeMb / totalSizeMb else 0f)
                        Spacer(modifier = Modifier.height(16.dp))
                        UsageItemRow(icon = Lucide.Mic, color = ColorVoice, title = "Voice messages", size = "84 MB", progress = 0.06f)
                        Spacer(modifier = Modifier.height(16.dp))
                        UsageItemRow(icon = Lucide.File, color = ColorDocs, title = "Documents", size = "96 MB", progress = 0.07f)
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "AUTO-DOWNLOAD MEDIA",
                    fontSize = 12.sp, lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SettingsColors.textSecondary,
                    modifier = Modifier.padding(start = 32.dp, bottom = 8.dp)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(SettingsColors.surface)
                ) {
                    ToggleRowItem(icon = Lucide.Volume2, title = "When using mobile data", isChecked = false)
                    HorizontalDivider(color = SettingsColors.divider, thickness = 1.dp, modifier = Modifier.padding(start = 56.dp))
                    ToggleRowItem(icon = Lucide.Wifi, title = "When connected on Wi-Fi", isChecked = true)
                    HorizontalDivider(color = SettingsColors.divider, thickness = 1.dp, modifier = Modifier.padding(start = 56.dp))
                    ToggleRowItem(icon = Lucide.Plane, title = "When roaming", isChecked = false)
                    HorizontalDivider(color = SettingsColors.divider, thickness = 1.dp)
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { }
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Icon(Lucide.RefreshCw, contentDescription = null, tint = SettingsColors.redAccent, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Reset auto-download settings", color = SettingsColors.redAccent, fontSize = 15.sp)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "SAVE TO GALLERY",
                    fontSize = 12.sp, lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SettingsColors.textSecondary,
                    modifier = Modifier.padding(start = 32.dp, bottom = 8.dp)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(SettingsColors.surface)
                ) {
                    ToggleRowItem(icon = Lucide.User, title = "Private chats", isChecked = privChats, onCheckedChange = setPrivChats)
                    HorizontalDivider(color = SettingsColors.divider, thickness = 1.dp, modifier = Modifier.padding(start = 56.dp))
                    ToggleRowItem(icon = Lucide.Users, title = "Groups", isChecked = groups, onCheckedChange = setGroups)
                    HorizontalDivider(color = SettingsColors.divider, thickness = 1.dp, modifier = Modifier.padding(start = 56.dp))
                    ToggleRowItem(icon = Lucide.Megaphone, title = "Channels", isChecked = channels, onCheckedChange = setChannels)
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "CLEANUP",
                    fontSize = 12.sp, lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SettingsColors.textSecondary,
                    modifier = Modifier.padding(start = 32.dp, bottom = 8.dp)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(SettingsColors.surface)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                    context.cacheDir.deleteRecursively()
                                    cacheSizeMb = 0f
                                }
                            }
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Icon(Lucide.Database, contentDescription = null, tint = SettingsColors.textPrimary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Clear cache", color = SettingsColors.textPrimary, fontSize = 15.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(String.format("%.1f MB", cacheSizeMb), color = SettingsColors.textSecondary, fontSize = 12.sp, lineHeight = 16.sp)
                    }
                    HorizontalDivider(color = SettingsColors.divider, thickness = 1.dp, modifier = Modifier.padding(start = 56.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                    context.filesDir.deleteRecursively()
                                    filesSizeMb = 0f
                                }
                            }
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Icon(Lucide.Trash2, contentDescription = null, tint = SettingsColors.redAccent, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Delete all downloads", color = SettingsColors.redAccent, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Remove cached photos, videos and files", color = SettingsColors.textSecondary, fontSize = 12.sp, lineHeight = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UsageItemRow(icon: ImageVector, color: Color, title: String, size: String, progress: Float) {    
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.size(36.dp).background(color.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, fontSize = 15.sp, color = SettingsColors.textPrimary)
                Text(size, color = SettingsColors.textSecondary, fontSize = 12.sp, lineHeight = 16.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = color,
                trackColor = color.copy(alpha = 0.1f)
            )
        }
    }
}

@Composable
private fun ToggleRowItem(icon: ImageVector, title: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit = {}) {    val blueAccent = SettingsColors.blueAccent    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = SettingsColors.textPrimary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, color = SettingsColors.textPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
        
        Switch(
            modifier = Modifier.scale(0.85f),
            checked = isChecked,
            onCheckedChange = { onCheckedChange(it) },
            
            colors = SwitchDefaults.colors(
                checkedThumbColor = SettingsColors.textPrimary,
                checkedTrackColor = SettingsColors.blueAccent,
                uncheckedThumbColor = SettingsColors.textPrimary,
                uncheckedTrackColor = SettingsColors.surface,
                uncheckedBorderColor = SettingsColors.divider
            )
        )
    }
}
