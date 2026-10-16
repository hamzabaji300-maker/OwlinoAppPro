package com.example.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Etat de lecture audio pour un message précis. */
data class AudioPlayState(
    val messageId: String? = null,
    val isPreparing: Boolean = false,
    val isPlaying: Boolean = false,
    val positionMs: Int = 0,
    val durationMs: Int = 0
)

/**
 * Lecteur audio unique pour les messages vocaux / fichiers audio du chat.
 * Un seul message est lu à la fois; relancer le même message met en pause / reprend.
 */
object AudioPlayerManager {
    private var player: MediaPlayer? = null
    private val _state = MutableStateFlow(AudioPlayState())
    val state: StateFlow<AudioPlayState> = _state

    /** Source locale (déjà téléchargée) sinon l'URL d'origine (content://, file://, https://). */
    fun toggle(context: Context, messageId: String, source: String) {
        val cur = _state.value
        if (cur.messageId == messageId && player != null && !cur.isPreparing) {
            try {
                val p = player!!
                if (p.isPlaying) {
                    p.pause()
                    _state.value = cur.copy(isPlaying = false, positionMs = p.currentPosition)
                } else {
                    p.start()
                    _state.value = cur.copy(isPlaying = true)
                }
            } catch (e: Exception) {
                release()
            }
            return
        }
        play(context.applicationContext, messageId, source)
    }

    private fun play(app: Context, messageId: String, source: String) {
        release()
        _state.value = AudioPlayState(messageId = messageId, isPreparing = true)
        try {
            val mp = MediaPlayer()
            player = mp
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            val uri = if (source.startsWith("/")) Uri.fromFile(java.io.File(source)) else Uri.parse(source)
            mp.setDataSource(app, uri)
            mp.setOnPreparedListener { p ->
                if (player !== p) return@setOnPreparedListener
                try {
                    p.start()
                    _state.value = AudioPlayState(messageId, false, true, 0, p.duration.coerceAtLeast(0))
                } catch (e: Exception) { release() }
            }
            mp.setOnCompletionListener {
                if (player === it) {
                    _state.value = AudioPlayState(messageId, false, false, 0, it.duration.coerceAtLeast(0))
                }
            }
            mp.setOnErrorListener { p, _, _ ->
                if (player === p) release()
                true
            }
            mp.prepareAsync()
        } catch (e: Exception) {
            release()
        }
    }

    /** À appeler périodiquement pendant la lecture pour rafraîchir la progression. */
    fun refreshPosition() {
        val p = player ?: return
        val cur = _state.value
        if (cur.isPlaying) {
            try {
                _state.value = cur.copy(positionMs = p.currentPosition, durationMs = p.duration.coerceAtLeast(cur.durationMs))
            } catch (e: Exception) { }
        }
    }

    fun release() {
        try { player?.release() } catch (e: Exception) { }
        player = null
        _state.value = AudioPlayState()
    }
}
