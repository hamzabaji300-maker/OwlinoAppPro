package com.example.util

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class GifItem(
    val id: String,
    val title: String,
    val previewUrl: String,
    val url: String,
    val width: Int,
    val height: Int
) {
    val aspectRatio: Float?
        get() = if (width > 0 && height > 0) width.toFloat() / height.toFloat() else null
}

data class GifPage(val items: List<GifItem>, val hasNext: Boolean)

/**
 * عميل بسيط لخدمة Klipy (بديل Tenor بعد إغلاقها).
 * الرسالة تحمل رابط الـ GIF فقط، فلا تكلّفك تخزينًا ولا نقلًا.
 * المفتاح يُقرأ من ملف .env (KLIPY_API_KEY) ولا يُكتب داخل الكود.
 */
object GifClient {
    private const val BASE = "https://api.klipy.com/api/v1"
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    val isConfigured: Boolean
        get() = BuildConfig.KLIPY_API_KEY.isNotBlank()

    suspend fun trending(customerId: String, page: Int = 1, perPage: Int = 24): GifPage =
        fetch("gifs/trending", linkedMapOf("page" to "$page", "per_page" to "$perPage", "customer_id" to customerId))

    suspend fun search(query: String, customerId: String, page: Int = 1, perPage: Int = 24): GifPage =
        fetch("gifs/search", linkedMapOf("q" to query, "page" to "$page", "per_page" to "$perPage", "customer_id" to customerId))

    private suspend fun fetch(path: String, params: Map<String, String>): GifPage = withContext(Dispatchers.IO) {
        val key = BuildConfig.KLIPY_API_KEY
        if (key.isBlank()) throw IllegalStateException("مفتاح Klipy غير مضبوط")
        val qs = params.entries.joinToString("&") { "${it.key}=${URLEncoder.encode(it.value, "UTF-8")}" }
        val conn = URL("$BASE/$key/$path?$qs").openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) Owlino")
            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) throw java.io.IOException("Klipy HTTP $code: ${body.take(160)}")
            parse(body)
        } finally {
            conn.disconnect()
        }
    }

    private fun JsonElement?.str(): String? =
        (this as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content

    private fun JsonElement?.int(): Int? = (this as? JsonPrimitive)?.intOrNull

    private fun parse(body: String): GifPage {
        val root = json.parseToJsonElement(body)
        var arr: JsonArray? = null
        var hasNext = false
        if (root is JsonArray) {
            arr = root
        } else if (root is JsonObject) {
            val d = root["data"]
            if (d is JsonArray) {
                arr = d
                hasNext = (root["has_next"] as? JsonPrimitive)?.booleanOrNull ?: false
            } else if (d is JsonObject) {
                arr = (d["data"] as? JsonArray) ?: (d["results"] as? JsonArray)
                hasNext = (d["has_next"] as? JsonPrimitive)?.booleanOrNull
                    ?: (root["has_next"] as? JsonPrimitive)?.booleanOrNull ?: false
            }
            if (arr == null) arr = root["results"] as? JsonArray
        }
        val items = arr?.mapNotNull { (it as? JsonObject)?.let { o -> toItem(o) } } ?: emptyList()
        return GifPage(items, hasNext)
    }

    private fun toItem(o: JsonObject): GifItem? {
        val id = o["slug"].str() ?: o["id"].str() ?: return null
        val title = o["title"].str() ?: ""
        val file = o["file"] as? JsonObject

        fun media(tiers: List<String>, formats: List<String>): Triple<String, Int, Int>? {
            for (t in tiers) {
                val tier = file?.get(t) as? JsonObject ?: continue
                for (f in formats) {
                    val m = tier[f] as? JsonObject ?: continue
                    val u = m["url"].str() ?: continue
                    return Triple(u, m["width"].int() ?: 0, m["height"].int() ?: 0)
                }
            }
            return null
        }

        var send = media(listOf("md", "sm", "hd", "xs"), listOf("gif", "webp"))
        var preview = media(listOf("sm", "xs", "md", "hd"), listOf("gif", "webp")) ?: send
        if (send == null) {
            // احتياطي: أول رابط gif/webp موجود داخل العنصر مهما كان شكله
            val urls = ArrayList<String>()
            collectUrls(o, urls)
            val u = urls.firstOrNull { it.substringBefore('?').endsWith(".gif", true) }
                ?: urls.firstOrNull { it.substringBefore('?').endsWith(".webp", true) }
                ?: return null
            send = Triple(u, 0, 0)
            preview = send
        }
        val p = preview ?: send
        return GifItem(id, title, p.first, send.first, send.second, send.third)
    }

    private fun collectUrls(e: JsonElement, out: MutableList<String>) {
        when (e) {
            is JsonObject -> e.values.forEach { collectUrls(it, out) }
            is JsonArray -> e.forEach { collectUrls(it, out) }
            is JsonPrimitive -> e.takeIf { it !is JsonNull }?.content?.let {
                if (it.startsWith("http")) out.add(it)
            }
        }
    }
}
