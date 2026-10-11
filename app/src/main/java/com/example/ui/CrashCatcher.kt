package com.example.ui

import android.content.Context
import java.io.File

object CrashCatcher {
    fun setup(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            val stackTrace = exception.stackTraceToString()
            val file = File(context.filesDir, "last_crash.txt")
            try { file.writeText(stackTrace) } catch (e: Throwable) {}
            // Readable copy for diagnosis: Owlino/last_crash.txt and Download/Owlino_last_crash.txt
            val text = "Thread: " + thread.name + "\n" + stackTrace
            try {
                val root = File(android.os.Environment.getExternalStorageDirectory(), "Owlino")
                root.mkdirs()
                File(root, "last_crash.txt").writeText(text)
            } catch (e: Throwable) {}
            try {
                val dl = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                dl.mkdirs()
                File(dl, "Owlino_last_crash.txt").writeText(text)
            } catch (e: Throwable) {}
            try { File(context.getExternalFilesDir(null), "last_crash.txt").writeText(text) } catch (e: Throwable) {}
            defaultHandler?.uncaughtException(thread, exception)
        }
    }
    
    fun getLastCrash(context: Context): String? {
        val file = File(context.filesDir, "last_crash.txt")
        if (file.exists()) {
            return file.readText()
        }
        return null
    }
    
    fun clearLastCrash(context: Context) {
        val file = File(context.filesDir, "last_crash.txt")
        if (file.exists()) file.delete()
    }
}
