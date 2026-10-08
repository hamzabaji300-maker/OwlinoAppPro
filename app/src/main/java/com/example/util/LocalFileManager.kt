package com.example.util

import android.content.Context
import android.os.Environment
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.security.MessageDigest

object LocalFileManager {
    private const val FOLDER_NAME = "Owlino_Media"

    fun getLocalFile(context: Context, fileName: String): File? {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: return null
        val mediaDir = File(dir, FOLDER_NAME)
        if (!mediaDir.exists()) return null
        
        val file = File(mediaDir, fileName)
        return if (file.exists() && file.length() > 0) file else null
    }

    suspend fun downloadAndSave(context: Context, urlString: String, fileName: String): File? {
        return withContext(Dispatchers.IO) {
            try {
                // Return immediately if it's a local content:// URI (already local)
                if (urlString.startsWith("content://") || urlString.startsWith("file://")) {
                    return@withContext null
                }

                val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: return@withContext null
                val mediaDir = File(dir, FOLDER_NAME)
                if (!mediaDir.exists()) mediaDir.mkdirs()

                val file = File(mediaDir, fileName)
                // If exists and valid, skip
                if (file.exists() && file.length() > 0) return@withContext file

                val tempFile = File(mediaDir, "$fileName.tmp")

                URL(urlString).openStream().use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                // Rename atomic
                tempFile.renameTo(file)
                file
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    fun getFileNameForMessage(messageId: String, url: String, isThumbnail: Boolean = false): String {
        // Strip query params to get a stable name just in case, but messageId is unique enough
        // We'll append an extension based on the URL or just default to .jpg
        val ext = if (url.contains(".mp4", ignoreCase = true)) ".mp4" else ".jpg"
        val suffix = if (isThumbnail) "_thumb" else ""
        return "${messageId}$suffix$ext"
    }

    fun saveFile(context: Context, bytes: ByteArray, fileName: String): File? {
        try {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: return null
            val mediaDir = File(dir, FOLDER_NAME)
            if (!mediaDir.exists()) mediaDir.mkdirs()

            val file = File(mediaDir, fileName)
            file.writeBytes(bytes)
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /** ينسخ ملفًا (content:// أو file://) إلى مجلد التطبيق بالتدفق، دون تحميله كله في الذاكرة. */
    fun saveFromUri(context: Context, uri: android.net.Uri, fileName: String): File? {
        return try {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: return null
            val mediaDir = File(dir, FOLDER_NAME)
            if (!mediaDir.exists()) mediaDir.mkdirs()
            val file = File(mediaDir, fileName)
            val input = context.contentResolver.openInputStream(uri) ?: return null
            input.use { src -> file.outputStream().use { dst -> src.copyTo(dst) } }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
