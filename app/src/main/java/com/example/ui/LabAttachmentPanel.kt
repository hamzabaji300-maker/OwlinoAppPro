package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * لوحة المرفقات (تحل محل AttachmentPickerPanel الأصلية التي ترتبط بكامل التطبيق).
 * نفس التوقيع: يستدعيها MessageInputBar الأصلي كما هو.
 * الاختيار عبر منتقي النظام (لا يحتاج صلاحيات تخزين).
 */
@Composable
fun AttachmentPickerPanel(
    panelHeight: Dp,
    onAttachmentSelected: (List<Uri>, AttachmentType) -> Unit
) {
    val theme = LocalSettingsTheme.current.theme
    val images = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { l ->
        if (l.isNotEmpty()) onAttachmentSelected(l, AttachmentType.IMAGE)
    }
    val videos = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { l ->
        if (l.isNotEmpty()) onAttachmentSelected(l, AttachmentType.VIDEO)
    }
    val audios = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { l ->
        if (l.isNotEmpty()) onAttachmentSelected(l, AttachmentType.AUDIO)
    }
    val files = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { l ->
        if (l.isNotEmpty()) onAttachmentSelected(l, AttachmentType.DOCUMENT)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(theme.surfaceColor)
            .padding(vertical = 22.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        PanelTile("صور", Icons.Outlined.Image, Color(0xFF8774E1)) { images.launch("image/*") }
        PanelTile("فيديو", Icons.Outlined.Videocam, Color(0xFFEF4444)) { videos.launch("video/*") }
        PanelTile("صوت", Icons.Outlined.MusicNote, Color(0xFFF59E0B)) { audios.launch("audio/*") }
        PanelTile("ملف", Icons.Outlined.InsertDriveFile, Color(0xFF3B82F6)) { files.launch("*/*") }
    }
}

@Composable
private fun PanelTile(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    val theme = LocalSettingsTheme.current.theme
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(8.dp)
    ) {
        Box(
            modifier = Modifier.size(56.dp).clip(CircleShape).background(color),
            contentAlignment = Alignment.Center
        ) { Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(28.dp)) }
        Spacer(Modifier.height(8.dp))
        Text(label, color = theme.textPrimary, fontSize = 13.sp)
    }
}
