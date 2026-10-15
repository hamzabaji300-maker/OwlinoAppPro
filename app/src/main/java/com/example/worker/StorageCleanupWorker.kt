package com.example.worker

import android.content.Context
import android.os.Environment
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.io.File

/**
 * ينظّف فقط النسخ المؤقتة (temp_*) التي ينشئها التطبيق أثناء إرسال المرفقات
 * ولم يعد أحد يحتاجها (أقدم من 30 يومًا).
 *
 * لا يلمس أبدًا الصور والفيديوهات المحمّلة أو المحفوظة (ملفات الرسائل المخزّنة محليًا)،
 * لأنها هي ما يبقى ظاهرًا للمستخدم بعد انتهاء صلاحية الملف على الخادم.
 */
class StorageCleanupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val dir = applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                ?: return Result.success()
            val mediaDir = File(dir, "Owlino_Media")
            if (!mediaDir.exists() || !mediaDir.isDirectory) return Result.success()

            val maxAgeMs = 30L * 24L * 60L * 60L * 1000L
            val now = System.currentTimeMillis()
            mediaDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.startsWith("temp_") && (now - file.lastModified()) > maxAgeMs) {
                    file.delete()
                }
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}
