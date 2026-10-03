package com.example.ui

import android.content.Context
import java.io.File

object CrashCatcher {
    fun setup(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            val stackTrace = exception.stackTraceToString()
            val file = File(context.filesDir, "last_crash.txt")
            file.writeText(stackTrace)
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
