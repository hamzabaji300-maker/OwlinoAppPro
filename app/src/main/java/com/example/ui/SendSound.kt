package com.example.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.R

/** Son court joué quand l'utilisateur envoie un message, une photo, un vocal, etc. */
object SendSound {
    private const val VOLUME = 0.12f
    private var pool: SoundPool? = null
    private var soundId = 0
    @Volatile private var loaded = false
    @Volatile private var pendingPlay = false

    @Synchronized
    private fun init(ctx: Context) {
        if (pool != null) return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val sp = SoundPool.Builder().setMaxStreams(2).setAudioAttributes(attrs).build()
        sp.setOnLoadCompleteListener { p, id, status ->
            if (status == 0) {
                loaded = true
                if (pendingPlay) {
                    pendingPlay = false
                    p.play(id, VOLUME, VOLUME, 1, 0, 1f)
                }
            }
        }
        soundId = sp.load(ctx.applicationContext, R.raw.sound_sent, 1)
        pool = sp
    }

    fun preload(ctx: Context) {
        try { init(ctx) } catch (e: Throwable) {}
    }

    fun play(ctx: Context) {
        try {
            init(ctx)
            if (loaded) pool?.play(soundId, VOLUME, VOLUME, 1, 0, 1f) else pendingPlay = true
        } catch (e: Throwable) {}
    }
}
