package com.example.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Trash2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun DataAndStorageScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cacheSize by remember { mutableStateOf("0 MB") }
    var dbSize by remember { mutableStateOf("0 MB") }
    
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val cacheDir = context.cacheDir
            val cacheBytes = cacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
            cacheSize = String.format("%.2f MB", cacheBytes / (1024.0 * 1024.0))

            val dbFile = context.getDatabasePath("chat_database")
            val dbBytes = if (dbFile.exists()) dbFile.length() else 0L
            dbSize = String.format("%.2f MB", dbBytes / (1024.0 * 1024.0))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(SettingsColors.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Lucide.ArrowLeft,
                contentDescription = "Back",
                tint = SettingsColors.textPrimary,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onBack() }
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = "Data and Storage",
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = SettingsColors.textPrimary
            )
        }
        HorizontalDivider(color = SettingsColors.divider, thickness = 1.dp)

        Column(modifier = Modifier.padding(16.dp)) {
            StorageItem(
                title = "Clear Cache",
                subtitle = "Frees up space ($cacheSize)",
                icon = Lucide.Trash2,
                onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            context.cacheDir.deleteRecursively()
                            cacheSize = "0.00 MB"
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            StorageItem(
                title = "Clear Local Database",
                subtitle = "Frees up space ($dbSize). Chats will reload from cloud.",
                icon = Lucide.Trash2,
                onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            val db = com.example.data.DatabaseProvider.getDatabase(context)
                            db.clearAllTables()
                            val dbFile = context.getDatabasePath("chat_database")
                            val dbBytes = if (dbFile.exists()) dbFile.length() else 0L
                            dbSize = String.format("%.2f MB", dbBytes / (1024.0 * 1024.0))
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun StorageItem(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SettingsColors.cardRadius))
            .background(SettingsColors.surface)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = SettingsColors.textPrimary)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, color = SettingsColors.textPrimary, fontWeight = FontWeight.Medium)
            Text(text = subtitle, fontSize = 13.sp, color = SettingsColors.textSecondary)
        }
    }
}
