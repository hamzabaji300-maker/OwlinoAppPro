package com.example.worker

import android.content.Context
import android.os.Environment
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.io.File

class StorageCleanupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        try {
            val dir = applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: return Result.success()
            val mediaDir = File(dir, "Owlino_Media")
            if (!mediaDir.exists() || !mediaDir.isDirectory) {
                return Result.success()
            }

            val thirtyDaysInMillis = 30L * 24L * 60L * 60L * 1000L
            val currentTime = System.currentTimeMillis()
            var deletedCount = 0

            mediaDir.listFiles()?.forEach { file ->
                if (file.isFile && (currentTime - file.lastModified()) > thirtyDaysInMillis) {
                    if (file.delete()) {
                        deletedCount++
                    }
                }
            }
            
            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure()
        }
    }
}
