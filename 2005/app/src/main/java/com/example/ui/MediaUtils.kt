package com.example.ui

import android.content.Context
import androidx.compose.runtime.*
import com.example.util.LocalFileManager

@Composable
fun rememberMediaSource(context: Context, messageId: String, url: String, isThumbnail: Boolean = false): Any {
    // Synchronous initial check so there is no flickering/recomposition if file exists
    var source by remember(messageId, url, isThumbnail) {
        val indexed = if (!isThumbnail) MediaIndex.get(messageId) else null
        val fileName = LocalFileManager.getFileNameForMessage(messageId, url, isThumbnail)
        val localFile = if (indexed != null) java.io.File(indexed) else LocalFileManager.getLocalFile(context, fileName)
        mutableStateOf<Any>(localFile ?: url)
    }

    LaunchedEffect(messageId, url, isThumbnail) {
        // If it was already resolved to a local File in the initial check, skip download
        if (source is String && !url.startsWith("content://") && !url.startsWith("file://") && (isThumbnail || MediaIndex.get(messageId) == null)) {
            val fileName = LocalFileManager.getFileNameForMessage(messageId, url, isThumbnail)
            val newLocal = LocalFileManager.downloadAndSave(context, url, fileName)
            if (newLocal != null) {
                source = newLocal
            }
        }
    }
    return source
}
