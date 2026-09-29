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

    suspend fun fetch(url: String): LinkPreviewData? {
        if (cache.containsKey(url)) return cache[url]
        return kotlinx.coroutines.withTimeoutOrNull(16000) {
            withContext(Dispatchers.IO) {
                try {
                    // A bot-style User-Agent (same convention Telegram/Twitter/Facebook use for
                    // their own link-preview crawlers) makes most sites - including YouTube -
                    // return a much lighter, preview-optimized page instead of the full JS bundle,
                    // which is both faster and more reliable on a slow mobile connection.
                    val response: HttpResponse = client.get(url) {
                        header("User-Agent", "TelegramBot (like TwitterBot)")
                        header("Accept", "text/html,application/xhtml+xml")
                        header("Accept-Language", "en-US,en;q=0.9,ar;q=0.8")
                    }
                    val html = response.bodyAsText().take(500_000)
                    val title = metaTag(html, "og:title")
                        ?: Regex("<title[^>]*>([^<]*)</title>", RegexOption.IGNORE_CASE)
                            .find(html)?.groupValues?.get(1)?.let { decodeHtmlEntities(it.trim()) }
                    val description = metaTag(html, "og:description") ?: metaTag(html, "description")
                    val image = resolveImageUrl(metaTag(html, "og:image"), url)
                    val siteName = metaTag(html, "og:site_name")
                        ?: try { io.ktor.http.Url(url).host } catch (e: Exception) { null }

                    val data = if (title == null && description == null && image == null) {
                        null
                    } else {
                        LinkPreviewData(url = url, siteName = siteName, title = title, description = description, imageUrl = image)
                    }
                    cache[url] = data
                    data
                } catch (e: Exception) {
                    cache[url] = null
                    null
                }
            }
        } ?: run {
            // Hit our own hard timeout ceiling: don't poison the cache, allow a retry later
            // (e.g. after the connection improves) instead of permanently giving up.
            null
        }
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
