package com.example.ui

import android.content.Context
import android.util.Log
import com.example.util.MediaStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * Which messages already have their media on the phone.
 * It is rebuilt from the Owlino folders at app start (file names are OWL-<messageId>.<ext>),
 * so it survives restarts/updates and heals itself if the user deletes files from the gallery.
 */
object MediaIndex {
    private val _files = MutableStateFlow<Map<String, String>>(emptyMap())
    val files: StateFlow<Map<String, String>> = _files

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready

    fun get(messageId: String): String? = _files.value[messageId]

    fun put(messageId: String, path: String) {
        _files.value = _files.value + (messageId to path)
    }

    /** Blocking; call from a background thread. */
    fun scan(context: Context) {
        val map = HashMap<String, String>()
        try {
            MediaStorage.allRoots(context.applicationContext).forEach { root ->
                MediaStorage.Kind.values().forEach { kind ->
                    val dir = File(root, kind.folder)
                    dir.listFiles()?.forEach { f ->
                        val n = f.name
                        if (f.isFile && f.length() > 0 && n.startsWith("OWL-") && !n.endsWith(".tmp")) {
                            val id = n.removePrefix("OWL-").substringBeforeLast('.')
                            if (id.isNotEmpty()) map[id] = f.absolutePath
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MediaIndex", "scan failed", e)
        }
        _files.value = map
        _ready.value = true
    }
}
