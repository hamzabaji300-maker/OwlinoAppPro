package com.example.util

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * مفضلة الـ GIF: تُحفظ على الهاتف، فتظهر مباشرة في لوحة GIF دون بحث ودون إنترنت (للقائمة).
 */
object GifFavorites {
    private const val PREFS = "gif_favorites"
    private const val KEY = "items"
    private const val MAX = 200

    private val _items = MutableStateFlow<List<GifItem>>(emptyList())
    val items: StateFlow<List<GifItem>> = _items

    @Volatile
    private var loaded = false

    fun ensureLoaded(context: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            val raw = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY, null)
            _items.value = parse(raw)
            loaded = true
        }
    }

    fun isFavorite(id: String): Boolean = _items.value.any { it.id == id }

    /** يضيف أو يزيل. يرجع true إن أصبحت مفضّلة. */
    fun toggle(context: Context, gif: GifItem): Boolean {
        ensureLoaded(context)
        val current = _items.value
        val exists = current.any { it.id == gif.id }
        val updated = if (exists) current.filter { it.id != gif.id } else (listOf(gif) + current).take(MAX)
        _items.value = updated
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, serialize(updated)).apply()
        return !exists
    }

    private fun serialize(list: List<GifItem>): String {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("title", it.title)
                    .put("preview", it.previewUrl)
                    .put("url", it.url)
                    .put("w", it.width)
                    .put("h", it.height)
            )
        }
        return arr.toString()
    }

    private fun parse(raw: String?): List<GifItem> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val id = o.optString("id")
                val url = o.optString("url")
                if (id.isBlank() || url.isBlank()) return@mapNotNull null
                GifItem(
                    id = id,
                    title = o.optString("title"),
                    previewUrl = o.optString("preview").ifBlank { url },
                    url = url,
                    width = o.optInt("w"),
                    height = o.optInt("h")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
