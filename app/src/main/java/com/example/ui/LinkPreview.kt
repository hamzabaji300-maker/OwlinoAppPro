package com.example.ui

import androidx.compose.runtime.mutableStateMapOf
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Telegram-style link preview: minimal Open Graph / meta scraper.
 */
data class LinkPreviewData(
    val url: String,
    val siteName: String? = null,
    val title: String? = null,
    val description: String? = null,
    val imageUrl: String? = null
)

val urlRegex = Regex("(https?://[\\w\\-.]+(?::\\d+)?(?:/[^\\s]*)?)", RegexOption.IGNORE_CASE)

fun extractFirstUrl(text: String): String? {
    val match = urlRegex.find(text) ?: return null
    var raw = match.value
    // Trim trailing punctuation that's most likely not part of the URL itself
    while (raw.isNotEmpty() && raw.last() in ".,!?)]}؛،:؟\"'”’") {
        raw = raw.dropLast(1)
    }
    return raw.ifBlank { null }
}

object LinkPreviewCache {
    // Cache holds null for "tried and failed / no data" so we don't refetch endlessly.
    val cache = mutableStateMapOf<String, LinkPreviewData?>()

    // Simple disk-backed persistence so a previously-seen link's preview renders instantly
    // on the next app launch instead of flashing empty while it re-fetches over the network.
    private const val PREFS_NAME = "link_preview_cache"

    private fun prefsKey(url: String): String = "lp_" + url.hashCode()

    fun loadPersisted(context: android.content.Context, url: String): LinkPreviewData? {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            val raw = prefs.getString(prefsKey(url), null) ?: return null
            val parts = raw.split("\u0001")
            if (parts.size < 5) return null
            fun unesc(s: String) = if (s == "\u0000") null else s
            LinkPreviewData(
                url = url,
                siteName = unesc(parts[1]),
                title = unesc(parts[2]),
                description = unesc(parts[3]),
                imageUrl = unesc(parts[4])
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun persist(context: android.content.Context, data: LinkPreviewData) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            fun esc(s: String?) = (s ?: "\u0000").replace("\u0001", " ")
            val raw = listOf(data.url, esc(data.siteName), esc(data.title), esc(data.description), esc(data.imageUrl))
                .joinToString("\u0001")
            prefs.edit().putString(prefsKey(data.url), raw).apply()
        } catch (e: Exception) {
            // best-effort only - in-memory cache still works for this session
        }
    }

    private val client: HttpClient by lazy {
        HttpClient(OkHttp) {
            install(HttpTimeout) {
                requestTimeoutMillis = 15000
                connectTimeoutMillis = 8000
                socketTimeoutMillis = 15000
            }
            expectSuccess = false
        }
    }

    private fun metaTag(html: String, key: String): String? {
        val patterns = listOf(
            Regex("<meta[^>]+property=[\"']$key[\"'][^>]*content=[\"']([^\"']*)[\"']", RegexOption.IGNORE_CASE),
            Regex("<meta[^>]+content=[\"']([^\"']*)[\"'][^>]*property=[\"']$key[\"']", RegexOption.IGNORE_CASE),
            Regex("<meta[^>]+name=[\"']$key[\"'][^>]*content=[\"']([^\"']*)[\"']", RegexOption.IGNORE_CASE),
            Regex("<meta[^>]+content=[\"']([^\"']*)[\"'][^>]*name=[\"']$key[\"']", RegexOption.IGNORE_CASE)
        )
        for (p in patterns) {
            p.find(html)?.let { return decodeHtmlEntities(it.groupValues[1].trim()) }
        }
        return null
    }

    private fun decodeHtmlEntities(s: String): String = s
        .replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'")
        .replace("&lt;", "<").replace("&gt;", ">")

    private fun resolveImageUrl(raw: String?, pageUrl: String): String? {
        if (raw.isNullOrBlank()) return null
        return try {
            when {
                raw.startsWith("http://") || raw.startsWith("https://") -> raw
                raw.startsWith("//") -> "https:$raw"
                raw.startsWith("/") -> {
                    val base = io.ktor.http.Url(pageUrl)
                    "${base.protocol.name}://${base.host}$raw"
                }
                else -> raw
            }
        } catch (e: Exception) {
            raw
        }
    }

    private suspend fun fetchOnce(url: String, userAgent: String): LinkPreviewData? {
        val response: HttpResponse = client.get(url) {
            header("User-Agent", userAgent)
            header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            header("Accept-Language", "en-US,en;q=0.9,ar;q=0.8")
        }
        val html = response.bodyAsText().take(700_000)
        val title = metaTag(html, "og:title")
            ?: Regex("<title[^>]*>([^<]*)</title>", RegexOption.IGNORE_CASE)
                .find(html)?.groupValues?.get(1)?.let { decodeHtmlEntities(it.trim()) }
        val description = metaTag(html, "og:description") ?: metaTag(html, "description")
        val image = resolveImageUrl(
            metaTag(html, "og:image") ?: metaTag(html, "twitter:image"),
            url
        )
        val siteName = metaTag(html, "og:site_name")
            ?: try { io.ktor.http.Url(url).host } catch (e: Exception) { null }

        return if (title == null && description == null && image == null) {
            null
        } else {
            LinkPreviewData(url = url, siteName = siteName, title = title, description = description, imageUrl = image)
        }
    }

    suspend fun fetch(url: String, context: android.content.Context? = null): LinkPreviewData? {
        if (cache.containsKey(url)) return cache[url]
        if (context != null) {
            val persisted = loadPersisted(context, url)
            if (persisted != null) {
                cache[url] = persisted
                return persisted
            }
        }
        return kotlinx.coroutines.withTimeoutOrNull(18000) {
            withContext(Dispatchers.IO) {
                // Preferred path: ask our own Supabase Edge Function to fetch the page
                // server-side. This is exactly what Telegram/WhatsApp do - the preview is
                // fetched from a server with real crawler reputation, not from the phone's
                // mobile/residential IP, which many news sites' bot-protection (Cloudflare,
                // Akamai...) blocks outright regardless of the User-Agent string sent.
                var data: LinkPreviewData? = try {
                    fetchViaEdgeFunction(url)
                } catch (e: Exception) {
                    null
                }
                // Fallback: fetch directly from the device (works fine for most sites,
                // including YouTube) - covers the case where the Edge Function isn't
                // deployed yet, or the function call itself failed.
                if (data == null) {
                    data = try {
                        fetchOnce(url, "TelegramBot (like TwitterBot)")
                    } catch (e: Exception) {
                        null
                    }
                }
                if (data == null) {
                    data = try {
                        fetchOnce(
                            url,
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                                "(KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                cache[url] = data
                if (data != null && context != null) {
                    persist(context, data)
                }
                data
            }
        } ?: run {
            // Hit our own hard timeout ceiling: don't poison the cache, allow a retry later
            // (e.g. after the connection improves) instead of permanently giving up.
            null
        }
    }

    // Supabase project ref/key are the same public client-side values already used
    // elsewhere in the app (SupabaseClient.kt) - safe to embed, same as there.
    private const val SUPABASE_URL = "https://tvleocnrlwifptaohbkh.supabase.co"
    private const val SUPABASE_ANON_KEY = "sb_publishable_aW224YQZFDWwcrdFqB00IQ_qwXuSr9r"

    private suspend fun fetchViaEdgeFunction(url: String): LinkPreviewData? {
        val encoded = java.net.URLEncoder.encode(url, "UTF-8")
        val response: HttpResponse = client.get("$SUPABASE_URL/functions/v1/link-preview?url=$encoded") {
            header("apikey", SUPABASE_ANON_KEY)
            header("Authorization", "Bearer $SUPABASE_ANON_KEY")
        }
        val body = response.bodyAsText()
        val title = jsonStringField(body, "title")
        val description = jsonStringField(body, "description")
        val imageUrl = jsonStringField(body, "imageUrl")
        val siteName = jsonStringField(body, "siteName")
        return if (title == null && description == null && imageUrl == null) {
            null
        } else {
            LinkPreviewData(url = url, siteName = siteName, title = title, description = description, imageUrl = imageUrl)
        }
    }

    // Minimal, dependency-free JSON string-field extractor (avoids pulling in a JSON
    // library just for this one small response shape).
    private fun jsonStringField(json: String, key: String): String? {
        val match = Regex("\"$key\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").find(json) ?: return null
        val raw = match.groupValues[1]
        return raw.replace("\\\"", "\"").replace("\\\\", "\\").replace("\\n", " ").replace("\\/", "/")
            .ifBlank { null }
    }
}

fun formatFileSize(bytes: Long?): String {
    if (bytes == null || bytes <= 0) return ""
    val units = listOf("B", "KB", "MB", "GB")
    var size = bytes.toDouble()
    var unitIdx = 0
    while (size >= 1024 && unitIdx < units.size - 1) {
        size /= 1024
        unitIdx++
    }
    return if (unitIdx == 0) "${size.toInt()} ${units[unitIdx]}" else "%.1f %s".format(size, units[unitIdx])
}
