package com.example.util

import android.content.Context

/** جودة الفيديو عند الإرسال: عادي (720p) أو عالٍ (1080p). */
enum class VideoQuality(val shortSide: Int, val bitrate: Int) {
    NORMAL(720, 2_000_000),   // حوالي 15 ميغابايت للدقيقة
    HIGH(1080, 4_000_000)     // حوالي 30 ميغابايت للدقيقة (الفيديو الطويل قد يتجاوز حد 50 ميغابايت)
}

object VideoQualityPrefs {
    private const val PREFS = "video_quality_prefs"
    private const val KEY = "quality"

    @Volatile
    var current: VideoQuality = VideoQuality.NORMAL
        private set

    fun load(context: Context) {
        val raw = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        current = VideoQuality.values().firstOrNull { it.name == raw } ?: VideoQuality.NORMAL
    }

    fun set(context: Context, quality: VideoQuality) {
        current = quality
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, quality.name).apply()
    }
}
