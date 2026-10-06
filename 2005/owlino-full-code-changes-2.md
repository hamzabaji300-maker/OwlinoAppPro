# Owlino — الأكواد الكاملة القابلة للنسخ لكل التعديلات

هذا الملف فيه **الكود الفعلي الكامل** (مو وصف بس) لكل ملف انسوّى أو انعدّل، جاهز للنسخ ولصقه مباشرة.
رتّبته حسب نوع التعديل، وكل قسم فيه: اسم الملف، شو يسوي، والكود نفسه.

---

## 1) ملف جديد بالكامل: `app/src/main/java/com/example/ui/LinkPreview.kt`

يحتوي: استخراج الروابط من النص، جلب بيانات OG (عنوان/وصف/صورة) من الخادم أو محليًا، كاش بالذاكرة وعلى القرص.

```kotlin
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

```

---

## 2) ملف جديد بالكامل: `supabase/functions/link-preview/index.ts`

Edge Function (Deno/TypeScript) تجلب معاينة الرابط من سيرفر Supabase بدل الهاتف مباشرة. **يحتاج نشر يدوي:**
```bash
supabase functions deploy link-preview --no-verify-jwt
```

```typescript
// Supabase Edge Function: server-side link preview fetcher.
//
// Why this exists: fetching link previews directly from the phone means every
// request comes from a generic mobile/residential IP with no crawler
// reputation, which a lot of news sites' bot-protection (Cloudflare, Akamai,
// PerimeterX...) blocks outright regardless of the User-Agent string sent.
// Telegram never has this problem because IT fetches previews from ITS OWN
// servers (well-known, often allow-listed crawler infrastructure) - this
// function does the same thing for Owlino: the Android app calls this
// function instead of fetching the target site directly.
//
// Deploy:
//   supabase functions deploy link-preview --no-verify-jwt
// Call:
//   GET https://<project-ref>.supabase.co/functions/v1/link-preview?url=<encoded-url>

const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

function metaTag(html: string, key: string): string | null {
  const patterns = [
    new RegExp(`<meta[^>]+property=["']${key}["'][^>]*content=["']([^"']*)["']`, "i"),
    new RegExp(`<meta[^>]+content=["']([^"']*)["'][^>]*property=["']${key}["']`, "i"),
    new RegExp(`<meta[^>]+name=["']${key}["'][^>]*content=["']([^"']*)["']`, "i"),
    new RegExp(`<meta[^>]+content=["']([^"']*)["'][^>]*name=["']${key}["']`, "i"),
  ];
  for (const p of patterns) {
    const m = html.match(p);
    if (m) return decodeHtmlEntities(m[1].trim());
  }
  return null;
}

function decodeHtmlEntities(s: string): string {
  return s
    .replace(/&amp;/g, "&")
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">");
}

function resolveImageUrl(raw: string | null, pageUrl: string): string | null {
  if (!raw) return null;
  try {
    if (raw.startsWith("http://") || raw.startsWith("https://")) return raw;
    if (raw.startsWith("//")) return "https:" + raw;
    if (raw.startsWith("/")) {
      const base = new URL(pageUrl);
      return `${base.protocol}//${base.host}${raw}`;
    }
    return raw;
  } catch {
    return raw;
  }
}

interface PreviewData {
  url: string;
  siteName: string | null;
  title: string | null;
  description: string | null;
  imageUrl: string | null;
}

async function fetchOnce(url: string, userAgent: string): Promise<PreviewData | null> {
  const res = await fetch(url, {
    headers: {
      "User-Agent": userAgent,
      "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
      "Accept-Language": "en-US,en;q=0.9,ar;q=0.8",
    },
    redirect: "follow",
  });
  const html = (await res.text()).slice(0, 700_000);
  const titleMatch = html.match(/<title[^>]*>([^<]*)<\/title>/i);
  const title = metaTag(html, "og:title") ?? (titleMatch ? decodeHtmlEntities(titleMatch[1].trim()) : null);
  const description = metaTag(html, "og:description") ?? metaTag(html, "description");
  const image = resolveImageUrl(metaTag(html, "og:image") ?? metaTag(html, "twitter:image"), url);
  const siteName =
    metaTag(html, "og:site_name") ??
    (() => {
      try {
        return new URL(url).host;
      } catch {
        return null;
      }
    })();

  if (!title && !description && !image) return null;
  return { url, siteName, title, description, imageUrl: image };
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: CORS_HEADERS });
  }

  const targetUrl = new URL(req.url).searchParams.get("url");
  if (!targetUrl) {
    return new Response(JSON.stringify({ error: "missing url param" }), {
      status: 400,
      headers: { ...CORS_HEADERS, "Content-Type": "application/json" },
    });
  }

  try {
    // Same two-pass strategy as the on-device fallback: a bot-style UA first
    // (fast, and the convention most sites specifically welcome for
    // previews), then a normal desktop-browser UA if that comes back empty.
    let data = await fetchOnce(targetUrl, "TelegramBot (like TwitterBot)").catch(() => null);
    if (!data) {
      data = await fetchOnce(
        targetUrl,
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36",
      ).catch(() => null);
    }
    return new Response(JSON.stringify(data ?? { url: targetUrl }), {
      headers: { ...CORS_HEADERS, "Content-Type": "application/json" },
    });
  } catch (e) {
    return new Response(JSON.stringify({ url: targetUrl, error: String(e) }), {
      status: 200,
      headers: { ...CORS_HEADERS, "Content-Type": "application/json" },
    });
  }
});

```

---

## 3) ملف مُعاد استخدامه بالكامل (كان كود ميت): `app/src/main/java/com/example/ui/AttachmentBottomSheet.kt`

هذا الملف الكامل فيه: `AttachmentPickerPanel` (لوحة الصور/الملفات بحجم الكيبورد)، تبويب الصور مع الألبومات، تبويب الملفات كمتصفح حقيقي، وكل منطق صلاحيات التخزين (MANAGE_EXTERNAL_STORAGE، legacy، partial media access).

```kotlin
package com.example.ui

import android.Manifest
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class MediaItem(val id: Long, val uri: Uri, val type: Int, val bucketName: String)
data class RecentFileItem(val id: Long, val uri: Uri, val name: String, val size: Long)
enum class AttachmentPickerTab { PHOTOS, FILES }

// Remembers the real display name/size for a file the user just picked from the Files
// tab, keyed by its Uri string. The Uri alone (especially a plain file:// path found via
// the filesystem fallback below) isn't always re-queryable for metadata later in the send
// pipeline, so we stash what we already know here instead of re-deriving it.
object PickedFileMetaCache {
    val map = mutableMapOf<String, Pair<String, Long?>>()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentBottomSheet(
    onDismiss: () -> Unit,
    onMediaSelected: (List<Uri>) -> Unit,
    onDocumentClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onLocationClick: () -> Unit,
    onCameraClick: () -> Unit,
    onContactClick: () -> Unit
) {
    val context = LocalContext.current
    var mediaItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    val selectedUris = remember { mutableStateListOf<Uri>() }
    var hasPermission by remember { mutableStateOf(false) }
    
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                permission
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            hasPermission = true
        } else {
            permissionLauncher.launch(permission)
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            withContext(Dispatchers.IO) {
                val items = mutableListOf<MediaItem>()
                val projection = arrayOf(
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.MEDIA_TYPE
                )
                val selection = (MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                        + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                        + " OR "
                        + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                        + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)
                        
                val queryUri = MediaStore.Files.getContentUri("external")
                
                context.contentResolver.query(
                    queryUri,
                    projection,
                    selection,
                    null,
                    MediaStore.Files.FileColumns.DATE_ADDED + " DESC"
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                    val typeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                    var count = 0
                    while (cursor.moveToNext() && count < 1000) {
                        val id = cursor.getLong(idColumn)
                        val type = cursor.getInt(typeColumn)
                        val contentUri = ContentUris.withAppendedId(queryUri, id)
                        items.add(MediaItem(id, contentUri, type, "أخرى"))
                        count++
                    }
                }
                withContext(Dispatchers.Main) {
                    mediaItems = items
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            // Action Buttons Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AttachmentOption(
                    icon = Icons.Outlined.Description,
                    label = "ملف",
                    color = Color(0xFF673AB7),
                    onClick = onDocumentClick
                )
                AttachmentOption(
                    icon = Icons.Outlined.CameraAlt,
                    label = "الكاميرا",
                    color = Color(0xFFE91E63),
                    onClick = onCameraClick
                )
                AttachmentOption(
                    icon = Icons.Outlined.Image,
                    label = "المعرض",
                    color = Color(0xFF9C27B0),
                    onClick = onGalleryClick
                )
                AttachmentOption(
                    icon = Icons.Outlined.LocationOn,
                    label = "الموقع",
                    color = Color(0xFF4CAF50),
                    onClick = onLocationClick
                )
                AttachmentOption(
                    icon = Icons.Outlined.Person,
                    label = "جهة اتصال",
                    color = Color(0xFF2196F3),
                    onClick = onContactClick
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
            
            // Media Grid
            if (mediaItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    if (hasPermission) {
                         CircularProgressIndicator()
                    } else {
                         Text("يحتاج التطبيق إلى إذن للوصول إلى الصور")
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(max = 350.dp)) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(mediaItems, key = { it.uri.toString() }) { item ->
                            val isSelected = selectedUris.contains(item.uri)
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clickable {
                                        if (isSelected) {
                                            selectedUris.remove(item.uri)
                                        } else {
                                            selectedUris.add(item.uri)
                                        }
                                    }
                            ) {
                                AsyncImage(
                                    model = item.uri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                // Selection overlay
                                if (isSelected) {
                                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .size(24.dp)
                                            .align(Alignment.TopEnd)
                                            .background(Color(0xFF4CAF50), CircleShape)
                                            .border(1.dp, Color.White, CircleShape)
                                            .padding(2.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .padding(6.dp)
                                            .size(20.dp)
                                            .align(Alignment.TopEnd)
                                            .border(1.5.dp, Color.White, CircleShape)
                                            .background(Color.Black.copy(alpha = 0.2f), CircleShape)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Floating Send Button if any selected
                    if (selectedUris.isNotEmpty()) {
                        FloatingActionButton(
                            onClick = { 
                                onMediaSelected(selectedUris.toList())
                                onDismiss()
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp),
                            containerColor = Color(0xFF4CAF50),
                            contentColor = Color.White
                        ) {
                            Icon(Icons.Filled.Send, contentDescription = "Send")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AttachmentOption(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(color = color, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

// ============================================================================
// Telegram-style in-place attachment picker: replaces the keyboard (same
// height) instead of opening as a floating dialog, and is split into two
// tabs — "الصور" (Photos) and "الملفات" (Files) — that swap the content
// below them depending on which one is selected.
// ============================================================================

@Composable
fun AttachmentPickerPanel(
    panelHeight: Dp,
    onAttachmentSelected: (List<Uri>, AttachmentType) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(AttachmentPickerTab.PHOTOS) }

    var mediaItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var selectedAlbum by remember { mutableStateOf<String?>(null) }
    val selectedUris = remember { mutableStateListOf<Uri>() }
    var hasMediaPermission by remember { mutableStateOf(false) }

    var currentDir by remember { mutableStateOf<java.io.File?>(null) }
    var dirEntries by remember { mutableStateOf<List<java.io.File>>(emptyList()) }
    var dirLoading by remember { mutableStateOf(false) }
    var hasAllFilesAccess by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.os.Environment.isExternalStorageManager()
            } else {
                // Bug fixed here: this used to be hardcoded to `true` on pre-Android-11
                // devices, which skipped ever actually requesting READ_EXTERNAL_STORAGE -
                // the folder browser then failed silently (canRead=false, listFiles=null)
                // because the permission was never really granted.
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_EXTERNAL_STORAGE
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val mediaPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results -> hasMediaPermission = results.values.any { it } }
    val filesPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasAllFilesAccess = granted }

    LaunchedEffect(Unit) {
        // Android 14+ (API 34) lets the user grant "Select photos" instead of "Allow all",
        // which grants READ_MEDIA_VISUAL_USER_SELECTED instead of READ_MEDIA_IMAGES - check
        // for either so partial access doesn't keep re-prompting or reporting "no permission".
        val partialAccessPermission = if (Build.VERSION.SDK_INT >= 34) {
            "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"
        } else null

        val hasFull = androidx.core.content.ContextCompat.checkSelfPermission(
            context, mediaPermission
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasPartial = partialAccessPermission != null &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, partialAccessPermission
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasFull || hasPartial) {
            hasMediaPermission = true
        } else {
            val toRequest = listOfNotNull(mediaPermission, partialAccessPermission)
            mediaPermissionLauncher.launch(toRequest.toTypedArray())
        }
    }

    LaunchedEffect(hasMediaPermission) {
        if (hasMediaPermission) {
            mediaItems = loadRecentMedia(context)
        }
    }

    // Re-check "all files access" whenever the app comes back to the foreground (the user
    // grants it in a system Settings screen, then returns here) so the browser starts
    // itself with no extra taps once permission is granted.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val nowGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    android.os.Environment.isExternalStorageManager()
                } else {
                    androidx.core.content.ContextCompat.checkSelfPermission(
                        context, Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                }
                if (nowGranted != hasAllFilesAccess) {
                    hasAllFilesAccess = nowGranted
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Legacy (pre-Android 11) devices: MANAGE_EXTERNAL_STORAGE doesn't exist, so request the
    // normal runtime storage permission once when the Files tab is opened and not yet granted.
    LaunchedEffect(selectedTab) {
        if (selectedTab == AttachmentPickerTab.FILES &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.R && !hasAllFilesAccess
        ) {
            filesPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    // Start browsing at the storage root the first time the Files tab becomes usable.
    LaunchedEffect(selectedTab, hasAllFilesAccess) {
        if (selectedTab == AttachmentPickerTab.FILES && hasAllFilesAccess && currentDir == null) {
            currentDir = android.os.Environment.getExternalStorageDirectory()
        }
    }

    var dirDebugInfo by remember { mutableStateOf("") }

    // Load the current folder's contents whenever it changes - a real, navigable file
    // browser (folders first, then files, alphabetical) exactly like Telegram's own
    // "Internal storage" / "External storage" browser.
    LaunchedEffect(currentDir) {
        val dir = currentDir ?: return@LaunchedEffect
        dirLoading = true
        var debug = "path=${dir.path} exists=${dir.exists()} canRead=${dir.canRead()}"
        dirEntries = withContext(Dispatchers.IO) {
            try {
                val raw = dir.listFiles()
                debug += " listFiles=${if (raw == null) "null" else raw.size.toString()}"
                raw
                    ?.filter { !it.isHidden }
                    ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                    ?: emptyList()
            } catch (e: Exception) {
                debug += " exception=${e.javaClass.simpleName}:${e.message}"
                emptyList()
            }
        }
        dirDebugInfo = debug
        dirLoading = false
    }


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(panelHeight)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Two tabs: Photos / Files — selecting one swaps the content below.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PickerTabButton(
                icon = Icons.Outlined.Image,
                label = "الصور",
                selected = selectedTab == AttachmentPickerTab.PHOTOS,
                modifier = Modifier.weight(1f),
                onClick = { selectedTab = AttachmentPickerTab.PHOTOS }
            )
            PickerTabButton(
                icon = Icons.Outlined.Description,
                label = "الملفات",
                selected = selectedTab == AttachmentPickerTab.FILES,
                modifier = Modifier.weight(1f),
                onClick = { selectedTab = AttachmentPickerTab.FILES }
            )
        }
        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedTab) {
                AttachmentPickerTab.PHOTOS -> {
                    if (mediaItems.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (hasMediaPermission) {
                                CircularProgressIndicator()
                            } else {
                                Text(
                                    "يحتاج التطبيق إلى إذن للوصول إلى الصور",
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(24.dp)
                                )
                            }
                        }
                    } else {
                        val albums = remember(mediaItems) {
                            mediaItems.groupBy { it.bucketName }
                                .toList()
                                .sortedByDescending { (_, items) -> items.maxOf { it.id } }
                        }
                        val album = selectedAlbum
                        val itemsToShow = if (album != null) {
                            mediaItems.filter { it.bucketName == album }
                        } else emptyList()

                        Column(Modifier.fillMaxSize()) {
                            if (album != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedAlbum = null }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.Image, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text("..", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(Modifier.width(8.dp))
                                    Text(album, fontSize = 12.sp, color = Color.Gray)
                                }
                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                            }

                            Box(Modifier.weight(1f).fillMaxWidth()) {
                                if (album == null) {
                                    // Album grid: one cover thumbnail per folder, like Telegram's gallery.
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(2),
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(albums, key = { it.first }) { (bucketName, bucketItems) ->
                                            Column(
                                                modifier = Modifier.clickable { selectedAlbum = bucketName }
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .aspectRatio(1f)
                                                        .clip(RoundedCornerShape(10.dp))
                                                ) {
                                                    AsyncImage(
                                                        model = bucketItems.first().uri,
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    bucketName,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text("${bucketItems.size}", fontSize = 11.sp, color = Color.Gray)
                                            }
                                        }
                                    }
                                } else {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(3),
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        items(itemsToShow, key = { it.uri.toString() }) { item ->
                                            val isSelected = selectedUris.contains(item.uri)
                                            Box(
                                                modifier = Modifier
                                                    .aspectRatio(1f)
                                                    .clickable {
                                                        if (isSelected) selectedUris.remove(item.uri)
                                                        else selectedUris.add(item.uri)
                                                    }
                                            ) {
                                                AsyncImage(
                                                    model = item.uri,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                if (isSelected) {
                                                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                                                    Icon(
                                                        imageVector = Icons.Filled.Check,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier
                                                            .padding(4.dp)
                                                            .size(24.dp)
                                                            .align(Alignment.TopEnd)
                                                            .background(Color(0xFF4CAF50), CircleShape)
                                                            .border(1.dp, Color.White, CircleShape)
                                                            .padding(2.dp)
                                                    )
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .padding(6.dp)
                                                            .size(20.dp)
                                                            .align(Alignment.TopEnd)
                                                            .border(1.5.dp, Color.White, CircleShape)
                                                            .background(Color.Black.copy(alpha = 0.2f), CircleShape)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                if (selectedUris.isNotEmpty()) {
                                    FloatingActionButton(
                                        onClick = {
                                            onAttachmentSelected(selectedUris.toList(), AttachmentType.IMAGE)
                                            selectedUris.clear()
                                        },
                                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                                        containerColor = Color(0xFF4CAF50),
                                        contentColor = Color.White
                                    ) {
                                        Icon(Icons.Filled.Send, contentDescription = "Send")
                                    }
                                }
                            }
                        }
                    }
                }
                AttachmentPickerTab.FILES -> {
                    if (!hasAllFilesAccess) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFF007AFF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.Folder, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                                }
                                Spacer(Modifier.height(14.dp))
                                Text(
                                    "لعرض ملفاتك هنا مباشرة (بدون أي نافذة اختيار) يحتاج التطبيق إذن الوصول لكل الملفات — مرة واحدة فقط",
                                    textAlign = TextAlign.Center,
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                                Spacer(Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                                            // Pre-Android-11: just the normal runtime permission dialog.
                                            filesPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                                        } else {
                                            try {
                                                val intent = android.content.Intent(
                                                    android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                                    android.net.Uri.fromParts("package", context.packageName, null)
                                                )
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                try {
                                                    context.startActivity(
                                                        android.content.Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                                    )
                                                } catch (e2: Exception) {}
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
                                ) {
                                    Text("منح الإذن", color = Color.White)
                                }
                            }
                        }
                    } else {
                        val rootPath = android.os.Environment.getExternalStorageDirectory().path
                        val dir = currentDir
                        Column(Modifier.fillMaxSize()) {
                            if (dir != null && dir.path != rootPath) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { currentDir = dir.parentFile ?: android.os.Environment.getExternalStorageDirectory() }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.Folder, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text("..", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(Modifier.width(8.dp))
                                    Text(dir.name, fontSize = 12.sp, color = Color.Gray)
                                }
                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                            }
                            Box(Modifier.weight(1f).fillMaxWidth()) {
                                if (dirLoading) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                                } else if (dirEntries.isEmpty()) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                                            Text("المجلد فارغ", color = Color.Gray)
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                dirDebugInfo,
                                                color = Color.Gray,
                                                fontSize = 10.sp,
                                                textAlign = TextAlign.Center
                                            )
                                            Text(
                                                "isExternalStorageManager=${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) android.os.Environment.isExternalStorageManager() else "n/a"}",
                                                color = Color.Gray,
                                                fontSize = 10.sp,
                                                textAlign = TextAlign.Center
                                            )
                                            Text(
                                                "hasAllFilesAccess=$hasAllFilesAccess sdk=${Build.VERSION.SDK_INT} versionCode=${com.example.BuildConfig.VERSION_CODE}",
                                                color = Color.Red,
                                                fontSize = 10.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                } else {
                                    LazyColumn(Modifier.fillMaxSize()) {
                                        items(dirEntries, key = { it.absolutePath }) { entry ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        if (entry.isDirectory) {
                                                            currentDir = entry
                                                        } else {
                                                            val uri = Uri.fromFile(entry)
                                                            PickedFileMetaCache.map[uri.toString()] = Pair(entry.name, entry.length())
                                                            onAttachmentSelected(listOf(uri), AttachmentType.DOCUMENT)
                                                        }
                                                    }
                                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier.size(38.dp).clip(CircleShape)
                                                        .background(if (entry.isDirectory) Color(0xFF007AFF) else Color(0xFF673AB7)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        if (entry.isDirectory) Icons.Outlined.Folder else Icons.Outlined.Description,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Spacer(Modifier.width(12.dp))
                                                Column(Modifier.weight(1f)) {
                                                    Text(
                                                        entry.name,
                                                        fontSize = 15.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    if (!entry.isDirectory) {
                                                        Text(formatFileSize(entry.length()), fontSize = 12.sp, color = Color.Gray)
                                                    } else {
                                                        Text("مجلد", fontSize = 12.sp, color = Color.Gray)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerTabButton(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (selected) Color(0xFF007AFF) else Color(0xFFEFEFEF)
    val fg = if (selected) Color.White else Color(0xFF555555)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = fg, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = fg, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun RecentFilesList(files: List<RecentFileItem>, onFileClick: (Uri) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(files, key = { it.id }) { file ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        PickedFileMetaCache.map[file.uri.toString()] = Pair(file.name, file.size)
                        onFileClick(file.uri)
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val ext = file.name.substringAfterLast('.', "").uppercase().take(4)
                Box(
                    modifier = Modifier.size(42.dp).clip(CircleShape).background(Color(0xFF673AB7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Description,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        file.name,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val sub = listOf(formatFileSize(file.size), ext).filter { it.isNotBlank() }.joinToString("  ·  ")
                    if (sub.isNotBlank()) Text(sub, fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}

private suspend fun loadRecentMedia(context: android.content.Context): List<MediaItem> =
    withContext(Dispatchers.IO) {
        val items = mutableListOf<MediaItem>()
        try {
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.MEDIA_TYPE,
                MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME
            )
            val selection = (MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                    + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                    + " OR "
                    + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                    + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)
            val queryUri = MediaStore.Files.getContentUri("external")
            context.contentResolver.query(
                queryUri, projection, selection, null,
                MediaStore.Files.FileColumns.DATE_ADDED + " DESC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val typeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                val bucketColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
                var count = 0
                // No artificial low cap - a real gallery should show everything, grouped by
                // album below so the user isn't scrolling one giant flat grid.
                while (cursor.moveToNext() && count < 5000) {
                    val id = cursor.getLong(idColumn)
                    val type = cursor.getInt(typeColumn)
                    val bucket = if (bucketColumn >= 0) cursor.getString(bucketColumn) else null
                    items.add(MediaItem(id, ContentUris.withAppendedId(queryUri, id), type, bucket ?: "أخرى"))
                    count++
                }
            }
        } catch (e: Exception) {
            // permission or provider issue - show empty state instead of crashing
        }
        items
    }

// Recent files = merges two sources so it works reliably across OEM Android skins:
// 1) The Downloads collection (needs no extra permission on API 29+, where browsers/
//    Telegram/etc. save received files).
// 2) A broad MediaStore.Files query for anything that isn't image/video/audio (some
//    OEM skins don't populate the Downloads collection consistently, so this is a
//    second pass that often finds files the first query misses).
// Results are merged and de-duplicated by name+size.
private suspend fun loadRecentFiles(context: android.content.Context): List<RecentFileItem> =
    withContext(Dispatchers.IO) {
        val items = mutableListOf<RecentFileItem>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val queryUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                val projection = arrayOf(
                    MediaStore.Downloads._ID,
                    MediaStore.Downloads.DISPLAY_NAME,
                    MediaStore.Downloads.SIZE
                )
                context.contentResolver.query(
                    queryUri, projection, null, null,
                    MediaStore.Downloads.DATE_ADDED + " DESC"
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.SIZE)
                    var count = 0
                    while (cursor.moveToNext() && count < 50) {
                        val id = cursor.getLong(idCol)
                        val name = cursor.getString(nameCol) ?: "file"
                        val size = cursor.getLong(sizeCol)
                        items.add(RecentFileItem(id, ContentUris.withAppendedId(queryUri, id), name, size))
                        count++
                    }
                }
            } catch (e: Exception) {
                // Downloads collection unavailable on this OEM/ROM - fall through to the
                // broader MediaStore.Files query below instead of giving up entirely.
            }
        }

        // Always also try the broad Files query (not just as an API<29 fallback) since
        // some devices don't reliably populate the Downloads collection.
        try {
            val queryUri = MediaStore.Files.getContentUri("external")
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.SIZE
            )
            val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} != ? AND " +
                    "${MediaStore.Files.FileColumns.MEDIA_TYPE} != ? AND " +
                    "${MediaStore.Files.FileColumns.MEDIA_TYPE} != ?"
            val args = arrayOf(
                MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
                MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO.toString()
            )
            context.contentResolver.query(
                queryUri, projection, selection, args,
                MediaStore.Files.FileColumns.DATE_ADDED + " DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                var count = 0
                while (cursor.moveToNext() && count < 50) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "file"
                    val size = cursor.getLong(sizeCol)
                    items.add(RecentFileItem(id, ContentUris.withAppendedId(queryUri, id), name, size))
                    count++
                }
            }
        } catch (e: Exception) {
            // permission or provider issue - the Downloads results above (if any) still stand
        }

        // Third pass: a bounded recursive scan of external storage itself. Once the user
        // has granted "all files access" (MANAGE_EXTERNAL_STORAGE, API 30+) this is the
        // most reliable source of all - it doesn't depend on MediaStore indexing anything
        // correctly. Depth-limited; skips only Android/data and Android/obb (huge, private
        // app-internal caches) but still walks into Android/media, since that's exactly
        // where Telegram, WhatsApp, etc. keep the files/documents they've received on
        // modern (scoped-storage) Android versions.
        val mediaExtensions = setOf(
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic",
            "mp4", "mkv", "webm", "3gp", "mov",
            "mp3", "m4a", "aac", "ogg", "wav"
        )
        try {
            val root = android.os.Environment.getExternalStorageDirectory()
            fun scan(dir: java.io.File, depth: Int) {
                if (depth > 6 || items.size > 200) return
                if (dir.name == "data" || dir.name == "obb") {
                    val parent = dir.parentFile?.name
                    if (parent == "Android") return
                }
                val children = dir.listFiles() ?: return
                for (f in children) {
                    if (items.size > 200) return
                    if (f.isDirectory) {
                        scan(f, depth + 1)
                    } else if (f.isFile) {
                        val ext = f.name.substringAfterLast('.', "").lowercase()
                        if (ext !in mediaExtensions && !f.isHidden) {
                            items.add(RecentFileItem(f.lastModified(), Uri.fromFile(f), f.name, f.length()))
                        }
                    }
                }
            }
            if (root.exists() && root.canRead()) {
                scan(root, 0)
            }
        } catch (e: Exception) {
            // no filesystem access on this device/API level - the queries above still stand
        }

        // De-dupe (the same physical file can show up from more than one source), sort by
        // most recently modified first, and cap the list.
        items.distinctBy { "${it.name}_${it.size}" }
            .sortedByDescending { it.id }
            .take(60)
    }

```

---

## 4) ملف مُعدَّل بالكامل: `app/src/main/java/com/example/ui/ChatComponents.kt`

هذا الملف يحتوي رندر فقاعة الرسالة كاملة (موجود أصلاً بالمشروع)، مع إضافاتنا: `LinkPreviewCard`, `FileBubbleContent` (بحالة الرفع/الإلغاء), `parseMessageText`/`buildRichMessageContent` (روابط بنفسجية + إيموجي متحرك + بولد), `LinkColor`. أعطيك الملف **كامل** عشان تستبدله مباشرة (فيه أيضًا كود موجود أصلاً بالمشروع لازم يبقى كما هو).

```kotlin
package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.platform.LocalConfiguration
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import com.example.emoji.NotoEmojiMap
import com.example.emoji.EmojiMessageUtils





@Composable
fun MessageReactionBar(onReact: (String) -> Unit) {
        val emojis = listOf("⭐", "👍", "❤️", "😂", "😮", "😢", "🔥")
    Row(
        modifier = Modifier
            .background(Color.White, RoundedCornerShape(24.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .shadow(4.dp, RoundedCornerShape(24.dp)),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        emojis.forEach { emoji ->
            Text(
                text = emoji,
                fontSize = 24.sp,
                modifier = Modifier
                    .clickable { onReact(emoji) }
                    .padding(4.dp)
            )
        }
    }
}

@Composable
fun MessageMenu(msg: MessageModel, onAction: (String) -> Unit) {
    val actions = listOf(
        "reply" to "Reply",
        "copy" to "Copy",
        "pin" to (if (msg.isPinned) "Unpin" else "Pin"),
        "save" to (if (msg.isSaved) "Unsave" else "Save"),
        "delete" to "Delete"
    )
    Column(
        modifier = Modifier
            .width(180.dp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .shadow(12.dp, RoundedCornerShape(12.dp))
            .padding(vertical = 4.dp)
    ) {
        actions.forEach { (id, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAction(id) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (id == "delete") Color.Red else Color.Black)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MessageBubble(
    msg: MessageModel,
    onImageClick: ((String) -> Unit)? = null,
    isFirstInCluster: Boolean,
    isLastInCluster: Boolean,
    onReplyClick: (MessageModel) -> Unit,
    onDoubleTap: (MessageModel) -> Unit,
    onLightLongPress: (MessageModel, Offset) -> Unit,
    onHeavyLongPress: (MessageModel, Offset) -> Unit,
    onCancelUpload: ((MessageModel) -> Unit)? = null
) {
    val isMe = msg.isMine
    val bubbleColor = if (isMe) Color(0xFFFFF3D0) else Color.White
    val textColor = if (isMe) Color(0xFF222222) else Color.Black
    val timeColor = textColor.copy(alpha = 0.6f)

    val shape = RoundedCornerShape(
        topStart = if (!isMe && !isFirstInCluster) 4.dp else 20.dp,
        topEnd = if (isMe && !isFirstInCluster) 4.dp else 20.dp,
        bottomStart = if (!isMe && !isLastInCluster) 4.dp else 20.dp,
        bottomEnd = if (isMe && !isLastInCluster) 4.dp else 20.dp
    )

    var touchOffset by remember { mutableStateOf(Offset.Zero) }
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    val emojiOnlyImageAttachments = msg.attachments.filter { it.type == AttachmentType.IMAGE }
    val emojiOnlyAudioAtt = msg.attachments.firstOrNull { it.type == AttachmentType.AUDIO || it.type == AttachmentType.VOICE }
    val documentAtt = msg.attachments.firstOrNull { it.type == AttachmentType.DOCUMENT }
    val emojiOnlyCleanText = msg.text.replace("[Photo]", "").replace("[Album]", "")
        .replace("[Document]", "").replace("[Documents]", "").trim()
    val emojiOnlySequence = remember(emojiOnlyCleanText) { EmojiMessageUtils.parseSupportedEmojiSequence(emojiOnlyCleanText) }
    val isEmojiOnlyMessage = emojiOnlySequence != null && emojiOnlyImageAttachments.isEmpty() && emojiOnlyAudioAtt == null && documentAtt == null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = if (isLastInCluster) 2.dp else 0.5.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (isMe && msg.reactions.isNotEmpty()) {
            Row(
                modifier = Modifier.offset(y = (-8).dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                msg.reactions.forEach { emoji ->
                    val reactionUrl = NotoEmojiMap.remoteUrlFor(NotoEmojiMap.reactionToEmoji(emoji))
                    if (reactionUrl != null) {
                        LottieEmojiReaction(url = reactionUrl, size = 34.dp)
                    }
                }
            }
            Spacer(Modifier.width(3.dp))
        }
        Column(
            modifier = Modifier
                .widthIn(min = 60.dp, max = if (msg.reactions.isNotEmpty()) screenWidth * 0.74f else screenWidth * 0.85f)
                .let { if (isEmojiOnlyMessage) it else it.clip(shape).background(bubbleColor) }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { onDoubleTap(msg) },
                        onPress = { offset ->
                            touchOffset = offset
                            val releaseTimeout = kotlinx.coroutines.withTimeoutOrNull(250) {
                                tryAwaitRelease()
                            }
                            if (releaseTimeout == null) {
                                // 250ms passed, not released
                                onLightLongPress(msg, offset)
                                val releaseTimeout2 = kotlinx.coroutines.withTimeoutOrNull(250) {
                                    tryAwaitRelease()
                                }
                                if (releaseTimeout2 == null) {
                                    // 500ms total passed
                                    onHeavyLongPress(msg, offset)
                                    tryAwaitRelease() // wait for actual release
                                }
                            }
                        }
                    )
                }
        ) {
            if (msg.replyTo != null) {
                Row(
                    modifier = Modifier
                        .padding(start = 4.dp, top = 4.dp, end = 4.dp, bottom = 4.dp)
                        .height(IntrinsicSize.Min)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isMe) Color(0xFFFDE9B4) else Color(0xFFF2F2F2))
                        .clickable { onReplyClick(msg.replyTo) }
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .background(if (isMe) Color(0xFFC78B22) else Color(0xFF3390EC))
                    )
                    Column(
                        modifier = Modifier
                            .padding(start = 6.dp, top = 2.dp, end = 6.dp, bottom = 2.dp)
                    ) {
                        Text(
                            text = if (msg.replyTo.isMine) "You" else msg.replyTo.senderName.takeIf { it.isNotBlank() } ?: "User",
                            color = if (isMe) Color(0xFFC78B22) else Color(0xFF3390EC),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = msg.replyTo.text,
                            color = Color.Black,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = androidx.compose.ui.text.TextStyle(textDirection = androidx.compose.ui.text.style.TextDirection.Content)
                        )
                    }
                }
            }

            val imageAttachments = emojiOnlyImageAttachments
            val audioAtt = emojiOnlyAudioAtt
            val cleanText = emojiOnlyCleanText

            if (isEmojiOnlyMessage) {
                val units = emojiOnlySequence!!
                Column(modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp)) {
                    if (units.size == 1) {
                        // إيموجي وحد -> كبير (100dp)
                        val path = NotoEmojiMap.remoteUrlFor(units[0])
                        if (path != null) {
                            LottieEmojiReaction(url = path, size = 100.dp)
                        }
                    } else {
                        // حتى 5 إيموجيات -> متحركة. أكثر من 5 -> ثابتة (من نفس الرابط) بلا فقاعة
                        val animated = units.size <= 5
                        val perRow = if (units.size <= 12) 6 else 8
                        val emojiSize = if (units.size <= 12) 40.dp else 32.dp
                        units.chunked(perRow).forEach { rowUnits ->
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                rowUnits.forEach { e ->
                                    val path = NotoEmojiMap.remoteUrlFor(e)
                                    if (path != null) {
                                        LottieEmojiReaction(url = path, size = emojiSize, animate = animated)
                                    }
                                }
                            }
                        }
                    }
                    // الوقت + علامة القراءة داخل كارد رقيق وشفاف تحت الإيموجي (زي تيليجرام)
                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 2.dp, end = 2.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.18f))
                            .padding(start = 5.dp, end = 4.dp, top = 0.dp, bottom = 0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(msg.time, fontSize = 9.sp, lineHeight = 11.sp, fontWeight = FontWeight.Normal, color = Color.White)
                        MessageIndicators(
                            isMe = isMe,
                            status = msg.status,
                            isRead = false,
                            isSaved = msg.isSaved,
                            isPinned = msg.isPinned,
                            isImageOverlay = true,
                            compact = true
                        )
                    }
                }
            } else if (imageAttachments.isNotEmpty()) {
                Box(modifier = Modifier.padding(2.dp).clip(RoundedCornerShape(14.dp))) {
                    val count = imageAttachments.size
                    val spacing = 2.dp
                    // مقاسات مصغّرة زي تيليجرام: عرض أقصى 66% من الشاشة وارتفاع أقصى 40% منها
                    val collageW = screenWidth * 0.66f
                    
                    if (count == 1) {
                        val att = imageAttachments.first()
                        val singleRatio = (att.aspectRatio ?: 1f).coerceIn(0.4f, 2.5f)
                        val singleMaxH = screenHeight * 0.40f
                        var singleW = collageW
                        var singleH = singleW / singleRatio
                        if (singleH > singleMaxH) {
                            singleH = singleMaxH
                            singleW = singleH * singleRatio
                        }
                        singleW = singleW.coerceAtLeast(120.dp)
                        coil.compose.SubcomposeAsyncImage(
                                    loading = {
                                        androidx.compose.foundation.layout.Box(
                                            modifier = androidx.compose.ui.Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black.copy(alpha=0.1f)),
                                            contentAlignment = androidx.compose.ui.Alignment.Center
                                        ) {
                                            androidx.compose.material3.CircularProgressIndicator(
                                                modifier = androidx.compose.ui.Modifier.size(24.dp),
                                                color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                                strokeWidth = 2.dp
                                            )
                                        }
                                    },
                                    
                            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                .data(rememberMediaSource(androidx.compose.ui.platform.LocalContext.current, att.messageId, att.thumbnailUrl ?: att.url, isThumbnail = att.thumbnailUrl != null))
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = singleW, height = singleH)
                                .clickable { onImageClick?.invoke(att.messageId) },

                        )
                    } else {
                        // معرض بالنسبة الأصلية لكل صورة (بلا تربيع/قص كبير): كل صف يتقاسم نفس الارتفاع
                        // والعرض يتوزع حسب نسبة كل صورة، فتظهر الصور كاملة تقريباً.
                        val shown = imageAttachments.take(4)
                        val rows: List<List<Attachment>> = when (shown.size) {
                            2 -> listOf(shown)
                            3 -> listOf(listOf(shown[0]), listOf(shown[1], shown[2]))
                            else -> listOf(listOf(shown[0], shown[1]), listOf(shown[2], shown[3]))
                        }
                        Column(modifier = Modifier.width(collageW), verticalArrangement = Arrangement.spacedBy(spacing)) {
                            rows.forEach { rowItems ->
                                var sumRatio = 0f
                                rowItems.forEach { sumRatio += (it.aspectRatio ?: 1f).coerceIn(0.5f, 2.0f) }
                                val maxRowH = if (rowItems.size == 1) screenHeight * 0.30f else screenHeight * 0.22f
                                val rowH = ((collageW - spacing * (rowItems.size - 1)) / sumRatio).coerceAtMost(maxRowH)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                    rowItems.forEach { att ->
                                        val r = (att.aspectRatio ?: 1f).coerceIn(0.5f, 2.0f)
                                        val isLastTile = count > 4 && att.messageId == shown.last().messageId
                                        MediaTile(
                                            att = att,
                                            modifier = Modifier.weight(r).height(rowH),
                                            extraCount = if (isLastTile) count - 4 else 0,
                                            onClick = { onImageClick?.invoke(att.messageId) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    if (cleanText.isBlank()) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.Black.copy(alpha = 0.28f))
                                .padding(start = 5.dp, end = 4.dp, top = 0.dp, bottom = 0.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(msg.time, fontSize = 9.sp, lineHeight = 11.sp, fontWeight = FontWeight.Normal, color = Color.White)
                            MessageIndicators(
                                isMe = isMe,
                                status = msg.status,
                                isRead = false,
                                isSaved = msg.isSaved,
                                isPinned = msg.isPinned,
                                isImageOverlay = true,
                                compact = true
                            )
                        }
                    }
                }
            } else if (audioAtt != null) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).width(200.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isMe) Color.White else Color(0xFF007AFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, null, tint = if (isMe) Color(0xFF4FA953) else Color.White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(modifier = Modifier.fillMaxWidth().height(20.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            val h = listOf(2, 4, 3, 5, 8, 4, 2, 5, 7, 4, 3, 2, 4, 6)
                            h.forEach { v ->
                                Box(modifier = Modifier.weight(1f).height((v*2).dp).clip(CircleShape).background(if (isMe) Color.White.copy(0.6f) else Color(0xFF007AFF).copy(0.4f)))
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(top=2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("0:00", fontSize = 11.sp, color = timeColor)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(msg.time, fontSize = 10.sp, color = timeColor)
                                MessageIndicators(
                                    isMe = isMe,
                                    status = msg.status,
                                    isRead = false,
                                    isSaved = msg.isSaved,
                                    isPinned = msg.isPinned,
                                    isImageOverlay = false
                                )
                            }
                        }
                    }
                }
            } else if (documentAtt != null) {
                FileBubbleContent(
                    att = documentAtt,
                    caption = cleanText,
                    isMe = isMe,
                    textColor = textColor,
                    timeColor = timeColor,
                    msg = msg,
                    onCancelUpload = onCancelUpload?.let { cb -> { cb(msg) } }
                )
            } else if (cleanText.isNotBlank()) {
                val detectedUrl = remember(cleanText) { extractFirstUrl(cleanText) }
                var isSingleLine by remember(cleanText) { mutableStateOf(detectedUrl == null) }
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr
                ) {
                    if (isSingleLine) {
                        val richSingle = remember(cleanText) { buildRichMessageContent(cleanText, 19.sp, 20.dp) }
                        FlowRow(
                            modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 10.dp)
                        ) {
                            Text(
                                text = richSingle.text,
                                inlineContent = richSingle.inlineContent,
                                color = textColor,
                                fontSize = 16.5.sp,
                                lineHeight = 24.sp,
                                maxLines = 1,
                                style = androidx.compose.ui.text.TextStyle(textDirection = androidx.compose.ui.text.style.TextDirection.Content),
                                onTextLayout = { result ->
                                    if (result.lineCount > 1 || result.hasVisualOverflow) {
                                        isSingleLine = false
                                    }
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Row(
                                modifier = Modifier.align(Alignment.Bottom),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(msg.time, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = timeColor)
                                MessageIndicators(
                                    isMe = isMe,
                                    status = msg.status,
                                    isRead = false,
                                    isSaved = msg.isSaved,
                                    isPinned = msg.isPinned,
                                    isImageOverlay = false
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 8.dp)
                        ) {
                            if (detectedUrl != null) {
                                LinkPreviewCard(url = detectedUrl, isMe = isMe, textColor = textColor)
                            }
                            val richMulti = remember(cleanText) { buildRichMessageContent(cleanText, 19.sp, 20.dp) }
                            Text(
                                text = richMulti.text,
                                inlineContent = richMulti.inlineContent,
                                color = textColor,
                                fontSize = 16.5.sp,
                                lineHeight = 24.sp,
                                style = androidx.compose.ui.text.TextStyle(textDirection = androidx.compose.ui.text.style.TextDirection.Content)
                            )
                            Row(
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .padding(top = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(msg.time, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = timeColor)
                                MessageIndicators(
                                    isMe = isMe,
                                    status = msg.status,
                                    isRead = false,
                                    isSaved = msg.isSaved,
                                    isPinned = msg.isPinned,
                                    isImageOverlay = false
                                )
                            }
                        }
                    }
                }
            }
        }

        if (!isMe && msg.reactions.isNotEmpty()) {
            Spacer(Modifier.width(3.dp))
            Row(
                modifier = Modifier.offset(y = (-8).dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                msg.reactions.forEach { emoji ->
                    val reactionUrl = NotoEmojiMap.remoteUrlFor(NotoEmojiMap.reactionToEmoji(emoji))
                    if (reactionUrl != null) {
                        LottieEmojiReaction(url = reactionUrl, size = 34.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun MessageInputView(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachmentClick: () -> Unit,
    replyingTo: MessageModel?,
    onCancelReply: () -> Unit,
    chatName: String,
    isBlocked: Boolean = false,
    onUnblock: () -> Unit = {}
) {
    if (isBlocked) {
        Column(
            modifier = Modifier.fillMaxWidth().background(Color(0xFFF0F2F5)).padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(com.example.ui.i18n.LocalTranslation.current.youBlockedThisUser, fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onUnblock) {
                Text(com.example.ui.i18n.LocalTranslation.current.unblockUserAllCaps, color = Color(0xFF007AFF), fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
            }
        }
        return
    }

    var isRecording by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }
    var slideOffsetX by remember { mutableFloatStateOf(0f) }
    var slideOffsetY by remember { mutableFloatStateOf(0f) }
    var recordingDuration by remember { mutableIntStateOf(0) }

    LaunchedEffect(isRecording, isLocked) {
        if (isRecording || isLocked) {
            recordingDuration = 0
            while (true) {
                delay(1000)
                recordingDuration++
            }
        }
    }

    val isTextMode = text.trim().isNotEmpty()

    Column(modifier = Modifier.fillMaxWidth().background(Color.Transparent).padding(horizontal = 8.dp, vertical = 6.dp)) {
        AnimatedVisibility(visible = replyingTo != null) {
            if (replyingTo != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.05f))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.width(3.dp).height(36.dp).background(Color(0xFF007AFF)))
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (replyingTo.isMine) "You" else chatName, color = Color(0xFF007AFF), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(parseBoldMarkdown(replyingTo.text.take(30)), fontSize = 14.sp, color = Color.Gray, maxLines = 1, style = androidx.compose.ui.text.TextStyle(textDirection = androidx.compose.ui.text.style.TextDirection.Content))
                    }
                    IconButton(onClick = onCancelReply, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, "Cancel", tint = Color.Gray)
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(if (isRecording || isLocked) Color(0xFFF5F6F8) else Color.White)
                .border(1.dp, Color.Black.copy(0.05f), RoundedCornerShape(24.dp))
                .padding(end = 4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            if (isRecording || isLocked) {
                Row(
                    modifier = Modifier.weight(1f).height(44.dp).padding(start = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color.Red))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${recordingDuration / 60}:${(recordingDuration % 60).toString().padStart(2, '0')}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.width(16.dp))
                    if (!isLocked) {
                        Text("< Slide to cancel", color = Color.Gray, fontSize = 14.sp)
                    } else {
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { isLocked = false; isRecording = false }) {
                            Icon(Icons.Default.Delete, "Cancel", tint = Color.Red)
                        }
                    }
                }
            } else {
                IconButton(onClick = onAttachmentClick, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Outlined.AttachFile, "Attach", tint = Color.Gray)
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    placeholder = { Text(com.example.ui.i18n.LocalTranslation.current.messageInputPlaceholder, color = Color.Gray) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    maxLines = 5
                )
            }

            Box(
                modifier = Modifier.size(44.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isTextMode || isLocked) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF007AFF))
                            .clickable {
                                if (isTextMode) {
                                    onSend()
                                } else if (isLocked) {
                                    isLocked = false
                                    isRecording = false
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ArrowUpward, "Send", tint = Color.White)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(slideOffsetX.roundToInt(), slideOffsetY.roundToInt()) }
                            .size(if (isRecording && !isLocked) 60.dp else 44.dp)
                            .clip(CircleShape)
                            .background(if (isRecording && !isLocked) Color(0xFF007AFF) else Color.Transparent)
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = {
                                        isRecording = true
                                        slideOffsetX = 0f
                                        slideOffsetY = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        if (isRecording && !isLocked) {
                                            if (slideOffsetX + dragAmount.x < 0) slideOffsetX += dragAmount.x
                                            if (slideOffsetY + dragAmount.y < 0) slideOffsetY += dragAmount.y

                                            if (slideOffsetX < -200) {
                                                isRecording = false
                                                slideOffsetX = 0f
                                                slideOffsetY = 0f
                                            } else if (slideOffsetY < -150) {
                                                isLocked = true
                                                isRecording = false
                                                slideOffsetX = 0f
                                                slideOffsetY = 0f
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        if (isRecording && !isLocked) {
                                            isRecording = false
                                            slideOffsetX = 0f
                                            slideOffsetY = 0f
                                        }
                                    },
                                    onDragCancel = {
                                        if (isRecording && !isLocked) {
                                            isRecording = false
                                            slideOffsetX = 0f
                                            slideOffsetY = 0f
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Mic, "Mic", tint = if (isRecording && !isLocked) Color.White else Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun MessageIndicators(
    isMe: Boolean,
    status: MessageStatus?,
    isRead: Boolean,
    isSaved: Boolean,
    isPinned: Boolean,
    isImageOverlay: Boolean = false,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val currentStatus = if (isRead) MessageStatus.READ else (status ?: MessageStatus.SENT)
    val neutralColor = Color(0xFF8A8A8A)
    val readBlue = Color(0xFF4FA8E8)
    val iconColor = if (isImageOverlay) Color.White else neutralColor
    val readColor = if (compact) Color(0xFF4FC3F7) else if (isImageOverlay) Color.White else readBlue

    Row(modifier = modifier.padding(start = if (compact) 2.dp else 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        if (isSaved) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = "Saved",
                tint = if (isImageOverlay) Color.White else neutralColor,
                modifier = Modifier.size(if (isImageOverlay) 12.dp else 13.dp)
            )
        }
        if (isPinned) {
            Icon(
                imageVector = Icons.Filled.PushPin,
                contentDescription = "Pinned",
                tint = if (isImageOverlay) Color.White else neutralColor,
                modifier = Modifier.size(if (isImageOverlay) 12.dp else 12.dp).graphicsLayer(rotationZ = 45f)
            )
        }
        
        if (isMe) {
            androidx.compose.animation.AnimatedContent(
                targetState = currentStatus,
                transitionSpec = {
                    (androidx.compose.animation.scaleIn(animationSpec = androidx.compose.animation.core.tween(200, delayMillis = 50)) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(200))) togetherWith
                    (androidx.compose.animation.scaleOut(animationSpec = androidx.compose.animation.core.tween(150)) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(150)))
                },
                label = "status_anim"
            ) { state ->
                when (state) {
                    MessageStatus.SENDING -> Icon(Icons.Outlined.Schedule, null, tint = iconColor, modifier = Modifier.size(if (compact) 10.dp else 12.dp))
                    MessageStatus.SENT -> Icon(Icons.Filled.Check, null, tint = iconColor, modifier = Modifier.size(if (compact) 10.dp else 14.dp))
                    MessageStatus.DELIVERED -> Icon(Icons.Filled.DoneAll, null, tint = iconColor, modifier = Modifier.size(if (compact) 11.dp else 15.dp))
                    MessageStatus.READ -> Icon(Icons.Filled.DoneAll, null, tint = readColor, modifier = Modifier.size(if (compact) 11.dp else 15.dp))
                    MessageStatus.FAILED -> Icon(Icons.Outlined.ErrorOutline, null, tint = Color.Red, modifier = Modifier.size(if (compact) 11.dp else 13.dp))
                }
            }
        }
    }
}

fun parseBoldMarkdown(text: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        var currentIndex = 0
        val boldRegex = Regex("\\*\\*(.*?)\\*\\*", setOf(kotlin.text.RegexOption.DOT_MATCHES_ALL, kotlin.text.RegexOption.MULTILINE))
        val matches = boldRegex.findAll(text)
        
        for (match in matches) {
            append(text.substring(currentIndex, match.range.first))
            withStyle(style = SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)) {
                append(match.groupValues[1])
            }
            currentIndex = match.range.last + 1
        }
        
        if (currentIndex < text.length) {
            append(text.substring(currentIndex))
        }
    }
}

// Purple, Telegram-style color used for clickable links inside message text and in preview cards.
val LinkColor = Color(0xFF7C4DFF)

// Same as parseBoldMarkdown, but also turns any URL found in the text into a colored,
// underlined, tappable link (opens the link's app/browser on click) — like Telegram.
fun parseMessageText(text: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        var currentIndex = 0
        val boldRegex = Regex("\\*\\*(.*?)\\*\\*", setOf(kotlin.text.RegexOption.DOT_MATCHES_ALL, kotlin.text.RegexOption.MULTILINE))
        for (match in boldRegex.findAll(text)) {
            if (match.range.first > currentIndex) {
                appendLinkified(text.substring(currentIndex, match.range.first), null)
            }
            appendLinkified(match.groupValues[1], SpanStyle(fontWeight = FontWeight.Bold))
            currentIndex = match.range.last + 1
        }
        if (currentIndex < text.length) {
            appendLinkified(text.substring(currentIndex), null)
        }
    }
}

private fun androidx.compose.ui.text.AnnotatedString.Builder.appendLinkified(segment: String, boldStyle: SpanStyle?) {
    var idx = 0
    for (m in urlRegex.findAll(segment)) {
        if (m.range.first < idx) continue
        if (m.range.first > idx) {
            val plain = segment.substring(idx, m.range.first)
            if (boldStyle != null) withStyle(boldStyle) { append(plain) } else append(plain)
        }
        var url = m.value
        while (url.isNotEmpty() && url.last() in ".,!?)]}؛،:؟\"'”’") url = url.dropLast(1)
        val linkStyle = SpanStyle(
            color = LinkColor,
            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
            fontWeight = boldStyle?.fontWeight
        )
        withLink(LinkAnnotation.Url(url, TextLinkStyles(style = linkStyle))) {
            append(url)
        }
        val consumedEnd = m.range.first + url.length
        val leftover = segment.substring(consumedEnd, m.range.last + 1)
        if (leftover.isNotEmpty()) {
            if (boldStyle != null) withStyle(boldStyle) { append(leftover) } else append(leftover)
        }
        idx = m.range.last + 1
    }
    if (idx < segment.length) {
        val rest = segment.substring(idx)
        if (boldStyle != null) withStyle(boldStyle) { append(rest) } else append(rest)
    }
}

// Emoji that appear in the middle of a regular text message (not an emoji-only message)
// used to render as a flat, static glyph from the system font. This makes them animated
// too, like Telegram: each emoji grapheme inside the text becomes an inline placeholder
// that plays the same animated Noto emoji used for emoji-only messages and reactions.
data class RichMessageContent(
    val text: androidx.compose.ui.text.AnnotatedString,
    val inlineContent: Map<String, androidx.compose.foundation.text.InlineTextContent>
)

fun buildRichMessageContent(
    text: String,
    emojiSize: androidx.compose.ui.unit.TextUnit,
    emojiSizeDp: androidx.compose.ui.unit.Dp
): RichMessageContent {
    val inlineMap = mutableMapOf<String, androidx.compose.foundation.text.InlineTextContent>()
    val annotated = buildAnnotatedString {
        var currentIndex = 0
        val boldRegex = Regex("\\*\\*(.*?)\\*\\*", setOf(kotlin.text.RegexOption.DOT_MATCHES_ALL, kotlin.text.RegexOption.MULTILINE))
        for (match in boldRegex.findAll(text)) {
            if (match.range.first > currentIndex) {
                appendLinkifiedWithEmoji(text.substring(currentIndex, match.range.first), null, inlineMap, emojiSize, emojiSizeDp)
            }
            appendLinkifiedWithEmoji(match.groupValues[1], SpanStyle(fontWeight = FontWeight.Bold), inlineMap, emojiSize, emojiSizeDp)
            currentIndex = match.range.last + 1
        }
        if (currentIndex < text.length) {
            appendLinkifiedWithEmoji(text.substring(currentIndex), null, inlineMap, emojiSize, emojiSizeDp)
        }
    }
    return RichMessageContent(annotated, inlineMap)
}

private fun androidx.compose.ui.text.AnnotatedString.Builder.appendLinkifiedWithEmoji(
    segment: String,
    boldStyle: SpanStyle?,
    inlineMap: MutableMap<String, androidx.compose.foundation.text.InlineTextContent>,
    emojiSize: androidx.compose.ui.unit.TextUnit,
    emojiSizeDp: androidx.compose.ui.unit.Dp
) {
    var idx = 0
    for (m in urlRegex.findAll(segment)) {
        if (m.range.first < idx) continue
        if (m.range.first > idx) {
            appendTextWithEmoji(segment.substring(idx, m.range.first), boldStyle, inlineMap, emojiSize, emojiSizeDp)
        }
        var url = m.value
        while (url.isNotEmpty() && url.last() in ".,!?)]}؛،:؟\"'”’") url = url.dropLast(1)
        val linkStyle = SpanStyle(
            color = LinkColor,
            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
            fontWeight = boldStyle?.fontWeight
        )
        withLink(LinkAnnotation.Url(url, TextLinkStyles(style = linkStyle))) {
            append(url)
        }
        val consumedEnd = m.range.first + url.length
        val leftover = segment.substring(consumedEnd, m.range.last + 1)
        if (leftover.isNotEmpty()) {
            appendTextWithEmoji(leftover, boldStyle, inlineMap, emojiSize, emojiSizeDp)
        }
        idx = m.range.last + 1
    }
    if (idx < segment.length) {
        appendTextWithEmoji(segment.substring(idx), boldStyle, inlineMap, emojiSize, emojiSizeDp)
    }
}

private fun androidx.compose.ui.text.AnnotatedString.Builder.appendTextWithEmoji(
    text: String,
    boldStyle: SpanStyle?,
    inlineMap: MutableMap<String, androidx.compose.foundation.text.InlineTextContent>,
    emojiSize: androidx.compose.ui.unit.TextUnit,
    emojiSizeDp: androidx.compose.ui.unit.Dp
) {
    val graphemes = com.example.emoji.EmojiMessageUtils.splitGraphemes(text)
    val plain = StringBuilder()
    fun flushPlain() {
        if (plain.isNotEmpty()) {
            val s = plain.toString()
            if (boldStyle != null) withStyle(boldStyle) { append(s) } else append(s)
            plain.clear()
        }
    }
    for (g in graphemes) {
        if (com.example.emoji.EmojiMessageUtils.isEmojiUnit(g)) {
            flushPlain()
            val id = "emoji_${inlineMap.size}"
            inlineMap[id] = androidx.compose.foundation.text.InlineTextContent(
                androidx.compose.ui.text.Placeholder(
                    width = emojiSize,
                    height = emojiSize,
                    placeholderVerticalAlign = androidx.compose.ui.text.PlaceholderVerticalAlign.TextCenter
                )
            ) {
                val url = com.example.emoji.NotoEmojiMap.remoteUrlFor(g)
                if (url != null) {
                    LottieEmojiReaction(
                        url = url,
                        size = emojiSizeDp
                    )
                } else {
                    Text(g, fontSize = emojiSize)
                }
            }
            appendInlineContent(id, g)
        } else {
            plain.append(g)
        }
    }
    flushPlain()
}


// Telegram-style link preview card: colored accent bar + site/title/description on top,
// large full-width thumbnail at the bottom. Tapping anywhere opens the link.
@Composable
fun LinkPreviewCard(url: String, isMe: Boolean, textColor: Color, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var preview by remember(url) {
        mutableStateOf(LinkPreviewCache.cache[url] ?: LinkPreviewCache.loadPersisted(context, url))
    }
    LaunchedEffect(url) {
        preview = LinkPreviewCache.fetch(url, context)
    }
    val data = preview ?: return
    if (data.title.isNullOrBlank() && data.description.isNullOrBlank() && data.imageUrl.isNullOrBlank()) return

    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { uriHandler.openUri(url) }
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(LinkColor)
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f).padding(vertical = 1.dp)) {
                if (!data.siteName.isNullOrBlank()) {
                    Text(
                        text = data.siteName,
                        color = LinkColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!data.title.isNullOrBlank()) {
                    Text(
                        text = data.title,
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!data.description.isNullOrBlank()) {
                    Text(
                        text = data.description,
                        color = textColor.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (!data.imageUrl.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            AsyncImage(
                model = data.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp, max = 200.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
        }
    }
}

// Telegram-style file bubble: circular file icon + filename + size/extension, optional caption below.
// While the file is still uploading (status == SENDING), shows a progress ring with a
// cancel (X) button in the middle instead of the file icon, and "جاري الرفع..." instead
// of the size - matching Telegram's own upload bubble.
@Composable
fun FileBubbleContent(
    att: Attachment,
    caption: String,
    isMe: Boolean,
    textColor: Color,
    timeColor: Color,
    msg: MessageModel,
    onCancelUpload: (() -> Unit)? = null
) {
    val fileName = att.fileName?.takeIf { it.isNotBlank() } ?: att.url.substringAfterLast('/').ifBlank { "File" }
    val ext = fileName.substringAfterLast('.', "").uppercase().take(4)
    val sizeStr = formatFileSize(att.fileSize)
    val subtitle = listOf(sizeStr, ext).filter { it.isNotBlank() }.joinToString("  ·  ")
    val isUploading = msg.status == MessageStatus.SENDING
    val isFailed = msg.status == MessageStatus.FAILED

    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp).widthIn(min = 210.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isMe) Color.White else Color(0xFF3390EC)),
                contentAlignment = Alignment.Center
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(38.dp),
                        color = if (isMe) Color(0xFFC78B22) else Color.White,
                        strokeWidth = 2.5.dp
                    )
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "إلغاء",
                        tint = if (isMe) Color(0xFFC78B22) else Color.White,
                        modifier = Modifier
                            .size(18.dp)
                            .let { if (onCancelUpload != null) it.clickable { onCancelUpload() } else it }
                    )
                } else if (isFailed) {
                    Icon(
                        Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Icon(
                        Icons.Outlined.InsertDriveFile,
                        contentDescription = null,
                        tint = if (isMe) Color(0xFFC78B22) else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileName,
                    color = textColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                when {
                    isUploading -> Text(text = "جاري الرفع...", color = timeColor, fontSize = 12.sp)
                    isFailed -> Text(text = "فشل الإرسال", color = Color(0xFFE53935), fontSize = 12.sp)
                    subtitle.isNotBlank() -> Text(text = subtitle, color = timeColor, fontSize = 12.sp)
                }
            }
        }
        if (caption.isNotBlank()) {
            val richCaption = remember(caption) { buildRichMessageContent(caption, 17.sp, 18.dp) }
            Text(
                text = richCaption.text,
                inlineContent = richCaption.inlineContent,
                color = textColor,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                modifier = Modifier.padding(top = 6.dp, start = 2.dp)
            )
        }
        Row(
            modifier = Modifier.align(Alignment.End).padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(msg.time, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = timeColor)
            MessageIndicators(
                isMe = isMe,
                status = msg.status,
                isRead = false,
                isSaved = msg.isSaved,
                isPinned = msg.isPinned,
                isImageOverlay = false
            )
        }
    }
}

@Composable
private fun MediaTile(att: Attachment, modifier: Modifier, extraCount: Int, onClick: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Box(modifier = modifier.clickable { onClick() }) {
        coil.compose.SubcomposeAsyncImage(
            loading = {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.1f)))
            },
            model = rememberMediaSource(context, att.messageId, att.thumbnailUrl ?: att.url, isThumbnail = att.thumbnailUrl != null),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (extraCount > 0) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "+$extraCount", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

```

---

## 5) تعديل صغير: `app/src/main/java/com/example/emoji/EmojiMessageUtils.kt`

تغيير واحد بس: دالة `splitGraphemes` من `private` إلى عامة (بدون `private`) عشان تُستخدم من ملف `ChatComponents.kt`. ابحث عن السطر:

```kotlin
private fun splitGraphemes(text: String): List<String> {
```

وغيّره إلى:

```kotlin
fun splitGraphemes(text: String): List<String> {
```

(باقي الملف الكامل بدون تغيير، هذا السطر الوحيد):

```kotlin
package com.example.emoji

import android.icu.text.BreakIterator

object EmojiMessageUtils {

    // أقصى عدد إيموجيات باش تبقى الرسالة "إيموجي فقط" (بلا فقاعة).
    // التحريك يقرره العارض (ChatComponents): متحرك حتى 12، وأكثر يتعرض كنص خفيف.
    private const val MAX_UNITS = 60

    // يقسّم النص لإيموجيات كاملة (grapheme clusters): يتعامل صح مع الإيموجي المركّب
    // (ألوان البشرة، الأعلام، ZWJ مثل 👨‍👩‍👧، keycap ...) ويعتبره وحدة وحدة.
    fun splitGraphemes(text: String): List<String> {
        val result = mutableListOf<String>()
        val iter = BreakIterator.getCharacterInstance()
        iter.setText(text)
        var start = iter.first()
        var end = iter.next()
        while (end != BreakIterator.DONE) {
            result.add(text.substring(start, end))
            start = end
            end = iter.next()
        }
        return result
    }

    // إيموجيات تظهر كإيموجي بدون الحاجة لـ FE0F
    private fun isDefaultEmojiCodePoint(cp: Int): Boolean {
        if (cp in 0x1F300..0x1FAFF) return true
        if (cp in 0x1F1E6..0x1F1FF) return true
        if (cp == 0x1F004 || cp == 0x1F0CF || cp == 0x1F18E) return true
        if (cp in 0x1F191..0x1F19A) return true
        if (cp in 0x23E9..0x23EC) return true
        if (cp in 0x2648..0x2653) return true
        if (cp in 0x2753..0x2755) return true
        if (cp in 0x2795..0x2797) return true
        return cp == 0x231A || cp == 0x231B || cp == 0x23F0 || cp == 0x23F3 ||
            cp == 0x25FD || cp == 0x25FE || cp == 0x2614 || cp == 0x2615 ||
            cp == 0x267F || cp == 0x2693 || cp == 0x26A1 || cp == 0x26AA || cp == 0x26AB ||
            cp == 0x26BD || cp == 0x26BE || cp == 0x26C4 || cp == 0x26C5 || cp == 0x26CE ||
            cp == 0x26D4 || cp == 0x26EA || cp == 0x26F2 || cp == 0x26F3 || cp == 0x26F5 ||
            cp == 0x26FA || cp == 0x26FD || cp == 0x2705 || cp == 0x270A || cp == 0x270B ||
            cp == 0x2728 || cp == 0x274C || cp == 0x274E || cp == 0x2757 ||
            cp == 0x27B0 || cp == 0x27BF || cp == 0x2B1B || cp == 0x2B1C ||
            cp == 0x2B50 || cp == 0x2B55
    }

    private fun isEmojiGrapheme(g: String): Boolean {
        if (g.isEmpty()) return false
        if (g.contains('\u20E3')) return true // keycap مثل 1️⃣
        val cp = g.codePointAt(0)
        if (cp <= 0x7F || cp == 0xFE0F || cp == 0x200D) return false
        if (g.contains('\uFE0F')) return true // ❤️ ☺️ ✌️ ...
        return isDefaultEmojiCodePoint(cp)
    }

    fun isEmojiUnit(g: String): Boolean = isEmojiGrapheme(g)

    // إذا الرسالة كلها إيموجي (وحدة أو أكثر) يرجع القائمة، غير كذا null.
    // ما نشيلوش FE0F لأن Google تسمّي بعض الملفات بيه (مثل 2764_fe0f).
    fun parseSupportedEmojiSequence(text: String): List<String>? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        val units = splitGraphemes(trimmed)
        if (units.isEmpty() || units.size > MAX_UNITS) return null
        if (units.any { !isEmojiGrapheme(it) }) return null
        return units
    }
}

```

---

## 6) تعديلات بملف `app/src/main/java/com/example/ui/ChatDetailScreen.kt`

هذا الملف ضخم جدًا (3800+ سطر) وأغلبه غير متعلق بتعديلاتنا، فبدل ما أعطيك الملف كامل، هذي **الأجزاء المضبوطة** اللي انعدّلت أو انضافت، مع مكان كل وحدة بالضبط.

### 6.1 دالة `MessageInputBar` كاملة (لوحة الإرفاق: الحالة، الارتفاع، الحركة، زر +)

هذا استبدال كامل للدالة (كانت موجودة أصلاً بشكل أبسط، هذي نسختها بعد كل التعديلات):

```kotlin
fun MessageInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachmentSelected: (List<android.net.Uri>, AttachmentType) -> Unit,
    replyingTo: MessageModel?,
    onCancelReply: () -> Unit,
    editingMessage: MessageModel?,
    onCancelEdit: () -> Unit
) {
    var isRecording by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }
    var slideOffsetX by remember { mutableFloatStateOf(0f) }
    var slideOffsetY by remember { mutableFloatStateOf(0f) }
    var recordingDuration by remember { mutableIntStateOf(0) }

    // Telegram-style attachment panel: swaps in place of the keyboard instead of
    // opening a floating dialog. It's at least as tall as the real keyboard (once we've
    // measured it) but never shorter than ~45% of the screen height, so it reads as a
    // proper full panel like Telegram's, not a thin strip at the bottom.
    var showAttachmentPanel by remember { mutableStateOf(false) }
    val screenHeightDp = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp
    val minPanelHeight = screenHeightDp * 0.75f
    var capturedKeyboardHeight by remember { mutableStateOf(minPanelHeight) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val imeVisible = WindowInsets.isImeVisible
    LaunchedEffect(imeBottomPx, imeVisible) {
        if (imeVisible && imeBottomPx > 0) {
            val heightDp = with(density) { imeBottomPx.toDp() }
            if (heightDp > minPanelHeight) capturedKeyboardHeight = heightDp
        }
    }
    val panelHeight = if (capturedKeyboardHeight > minPanelHeight) capturedKeyboardHeight else minPanelHeight
    
    val photoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            onAttachmentSelected(uris, AttachmentType.IMAGE)
        }
    }
    
    LaunchedEffect(isRecording, isLocked) {
        if (isRecording || isLocked) {
            recordingDuration = 0
            while (true) {
                kotlinx.coroutines.delay(1000)
                recordingDuration++
            }
        }
    }
    
    val isTextMode = text.trim().isNotEmpty()
    
    Column(modifier = Modifier.fillMaxWidth().background(Color.Transparent).padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 14.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(if (isRecording || isLocked) Color(0xFFF5F6F8) else MaterialTheme.colorScheme.surface)
                .border(1.dp, Color.Black.copy(alpha = 0.05f), RoundedCornerShape(28.dp))
        ) {
            // Reply / Edit preview (Inside the rounded bubble!)
            AnimatedVisibility(visible = replyingTo != null || editingMessage != null) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (editingMessage != null) Icons.Default.Edit else Icons.AutoMirrored.Outlined.Reply,
                            contentDescription = null,
                            tint = Color(0xFF555555),
                            modifier = Modifier.padding(horizontal = 8.dp).size(20.dp)
                        )
                        Box(modifier = Modifier.width(3.dp).height(30.dp).background(Color(0xFF555555)).clip(RoundedCornerShape(1.5.dp)))
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (editingMessage != null) "Edit Message" else if (replyingTo?.isMine == true) "You" else replyingTo?.senderName?.takeIf { it.isNotBlank() } ?: "User",
                                color = Color(0xFF555555), fontSize = 13.sp, fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = (editingMessage ?: replyingTo)?.text ?: "",
                                color = Color.Gray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = {
                            if (editingMessage != null) onCancelEdit() else onCancelReply()
                        }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.Close, "Cancel", tint = Color.Gray, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 4.dp).heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isRecording || isLocked) {
                    // Recording UI
                    Row(
                        modifier = Modifier.weight(1f).height(44.dp).padding(start = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color.Red))
                        Spacer(Modifier.width(8.dp))
                        val min = recordingDuration / 60
                        val sec = recordingDuration % 60
                        Text(String.format("%d:%02d", min, sec), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        
                        if (!isLocked) {
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Slide to cancel", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.offset { androidx.compose.ui.unit.IntOffset(slideOffsetX.roundToInt(), 0) })
                            Spacer(Modifier.weight(1f))
                        } else {
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { isLocked = false; isRecording = false; slideOffsetX = 0f; slideOffsetY = 0f }) {
                                Icon(Icons.Outlined.Delete, "Delete", tint = Color.Red, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                } else {
                    // Text Input UI
                    IconButton(
                        onClick = {
                            if (showAttachmentPanel) {
                                showAttachmentPanel = false
                            } else {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                showAttachmentPanel = true
                            }
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Default.Add, "Attach", tint = Color(0xFF007AFF), modifier = Modifier.size(28.dp))
                    }
                    
                    Box(
                        modifier = Modifier.weight(1f).padding(vertical = 10.dp)
                    ) {
                        if (text.isEmpty()) {
                            Text("Message", color = Color.Gray, fontSize = 16.sp)
                        }
                        androidx.compose.foundation.text.BasicTextField(
                            value = text,
                            onValueChange = onTextChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusEvent { if (it.isFocused) showAttachmentPanel = false },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 16.sp, 
                                lineHeight = 22.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFF007AFF)),
                            maxLines = 5
                        )
                    }
                }
                
                // Send / Mic Button
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.animation.AnimatedContent(
                        targetState = isTextMode || isLocked,
                        transitionSpec = {
                            androidx.compose.animation.scaleIn() + androidx.compose.animation.fadeIn() togetherWith
                            androidx.compose.animation.scaleOut() + androidx.compose.animation.fadeOut()
                        },
                        label = "send_mic_anim"
                    ) { showSend ->
                        if (showSend) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF007AFF))
                                .clickable {
                                    if (isTextMode) {
                                        onSend()
                                    } else if (isLocked) {
                                        isLocked = false
                                        isRecording = false
                                        // Handle send audio
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = Color.White, modifier = Modifier.size(20.dp).offset(x = 2.dp))
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .offset { androidx.compose.ui.unit.IntOffset(slideOffsetX.roundToInt(), slideOffsetY.roundToInt()) }
                                .size(if (isRecording && !isLocked) 60.dp else 36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF007AFF))
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = {
                                            isRecording = true
                                            slideOffsetX = 0f
                                            slideOffsetY = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            if (isRecording && !isLocked) {
                                                if (slideOffsetX + dragAmount.x < 0) slideOffsetX += dragAmount.x
                                                if (slideOffsetY + dragAmount.y < 0) slideOffsetY += dragAmount.y
                                                
                                                if (slideOffsetX < -150) {
                                                    isRecording = false
                                                    slideOffsetX = 0f
                                                    slideOffsetY = 0f
                                                } else if (slideOffsetY < -100) {
                                                    isLocked = true
                                                    isRecording = false
                                                    slideOffsetX = 0f
                                                    slideOffsetY = 0f
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            if (isRecording && !isLocked) {
                                                isRecording = false
                                                slideOffsetX = 0f
                                                slideOffsetY = 0f
                                                // Handle send audio
                                            }
                                        },
                                        onDragCancel = {
                                            if (isRecording && !isLocked) {
                                                isRecording = false
                                                slideOffsetX = 0f
                                                slideOffsetY = 0f
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Mic, "Mic", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showAttachmentPanel,
            enter = expandVertically(animationSpec = tween(280, easing = LinearOutSlowInEasing)) +
                    fadeIn(animationSpec = tween(220)),
            exit = shrinkVertically(animationSpec = tween(220)) +
                    fadeOut(animationSpec = tween(160))
        ) {
            AttachmentPickerPanel(
                panelHeight = panelHeight,
                onAttachmentSelected = { uris, type ->
                    onAttachmentSelected(uris, type)
                    showAttachmentPanel = false
                }
            )
        }
    }

}

```

### 6.2 مكان استدعاء `MessageInputBar` (بدون تغيير بالاستدعاء نفسه غالبًا، للسياق فقط)

```kotlin
                                            Text(com.example.ui.i18n.LocalTranslation.current.unblockUserAllCaps, color = Color(0xFF3B82F6), fontWeight = FontWeight.SemiBold, fontSize = 15.sp, letterSpacing = 1.sp)
                                        }
                                    }
                                }
                            } else {
                 MessageInputBar(
                     text = messageText,
                 onTextChange = {
                     messageText = it
                     onUserType()
                 },
                 replyingTo = replyingTo,
                 onCancelReply = { replyingTo = null },
                 editingMessage = editingMessage,
                 onCancelEdit = {
                     editingMessage = null

```

### 6.3 حفظ اسم/حجم الملف الحقيقي وقت الإرسال (`docMetaByUri`)

أُضيف هذا الجزء داخل معالج `onAttachmentSelected` (قبل حلقة بناء الرسائل المؤقتة `tempMsgsWithUris`)، عشان يستخرج الاسم/الحجم الحقيقيين من الملف المُرسل:

```kotlin
                    val docMetaByUri = if (type == AttachmentType.DOCUMENT) {
                        uris.associateWith { uri ->
                            // Prefer what the Files-tab picker already knew about this file
                            // (works even for plain file:// Uris that ContentResolver can't
                            // re-query) before falling back to an OpenableColumns lookup.
                            val cached = PickedFileMetaCache.map[uri.toString()]
                            if (cached != null) {
                                cached
                            } else {
                                var name: String? = null
                                var size: Long? = null
                                try {
                                    context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                                        val nameIdx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                        val sizeIdx = c.getColumnIndex(android.provider.OpenableColumns.SIZE)
                                        if (c.moveToFirst()) {
                                            if (nameIdx >= 0) name = c.getString(nameIdx)
                                            if (sizeIdx >= 0 && !c.isNull(sizeIdx)) size = c.getLong(sizeIdx)
                                        }
                                    }
                                } catch (e: Exception) {}
                                Pair(name, size)
                            }
                        }

```

### 6.4 تمرير `fileName`/`fileSize` عند إنشاء الرسالة المؤقتة (Attachment المحلي أثناء الرفع)

```kotlin
                                }
                                val localFile = com.example.util.LocalFileManager.saveFile(context, bytes, "temp_${tempId}$ext")
                                if (localFile != null) {
                                    localUriStr = localFile.toURI().toString()
                                }
                            }
                        } catch(e: Exception) {}

                        val tempAttachment = Attachment(messageId = tempId, type = type, url = localUriStr, aspectRatio = calculatedRatio, fileName = docFileName, fileSize = docFileSize)
                        val tempMsg = MessageModel(
                            id = tempId,

```

### 6.5 تمرير `fileName`/`fileSize` عند تأكيد رفع الرسالة (Attachment النهائي بعد نجاح الرفع)

```kotlin
                                    
                                    val timeStr = formatTimeSafe(result.created_at)
                                    val docMeta = docMetaByUri[uri]
                                    val attachment = Attachment(messageId = result.id, type = type, url = signedUrl, thumbnailUrl = result.thumbnail_url, aspectRatio = calculatedRatio, fileName = docMeta?.first, fileSize = docMeta?.second)
                                    try {

```

---

## 7) تعديلات `AndroidManifest.xml` (الملف كامل، صغير)

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android" xmlns:tools="http://schemas.android.com/tools">
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.USE_BIOMETRIC" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" tools:ignore="ScopedStorage" />
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
    <uses-permission android:name="android.permission.READ_MEDIA_VISUAL_USER_SELECTED" />
    <uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:name=".MyApplication"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:label="Owlino"
        android:supportsRtl="true"
        android:requestLegacyExternalStorage="true"
        android:theme="@style/Theme.FastApp">
        
        <activity android:name="androidx.activity.ComponentActivity" android:exported="true" />
        
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="Owlino"
            android:windowSoftInputMode="adjustResize"
            android:launchMode="singleTop">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="owlinoapp" android:host="login-callback" />
            </intent-filter>
        </activity>

        <receiver android:name=".NotificationActionReceiver" android:exported="true">
            <intent-filter>
                <action android:name="ACTION_MARK_AS_READ" />
                <action android:name="ACTION_REPLY" />
            </intent-filter>
        </receiver>

        <service
            android:name=".OwlinoMessagingService"
            android:exported="false">
            <intent-filter>
                <action android:name="com.google.firebase.MESSAGING_EVENT" />
            </intent-filter>
        </service>
    </application>
</manifest>

```

---

## 8) تعديل `app/build.gradle.kts` — فقط جزء `versionCode`/`versionName`

```kotlin
        applicationId = "com.owlino.odxrgj"
        minSdk = 26
        targetSdk = 34
        versionCode = 46
        versionName = "build-46"
        vectorDrawables { useSupportLibrary = true }

```

---

## 9) ملف `.github/workflows/build-release.yml` كامل

```yaml
name: Build Release APK

on:
  workflow_dispatch:
  push:
    branches: [ main ]

permissions:
  contents: write

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout project
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'

      - name: Point Gradle at the Android SDK already on the runner
        run: echo "sdk.dir=$ANDROID_SDK_ROOT" > local.properties

      - name: Install Gradle
        uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: '8.9'

      - name: Build release APK
        run: gradle assembleRelease --stacktrace

      - name: Upload signed release APK
        uses: actions/upload-artifact@v4
        continue-on-error: true
        with:
          name: owlino-release-apk
          path: app/build/outputs/apk/release/*.apk
          retention-days: 1

      # Publishes the APK as a GitHub Release asset instead of (or alongside) the Actions
      # Artifact above. Releases don't count against the Artifact storage quota at all, so
      # this keeps working even when that quota is full. Each build gets its own tag
      # (build-<run number>) and is marked "latest", so `gh release download` (no --tag
      # needed) always grabs the newest APK.
      - name: Publish GitHub Release with APK
        uses: softprops/action-gh-release@v2
        with:
          tag_name: build-${{ github.run_number }}
          name: Owlino build ${{ github.run_number }}
          make_latest: true
          files: app/build/outputs/apk/release/*.apk

```

---

## ملاحظات تطبيق مهمة

- لصق `ChatComponents.kt` و`AttachmentBottomSheet.kt` كاملين هو **استبدال ملف بملف** — تأكد المشروع التاني ما عنده تعديلات خاصة بنفس الملفين قبل ما تستبدلهم، وإلا خذ التعديلات المحددة بس (`LinkPreviewCard`, `FileBubbleContent`, `buildRichMessageContent`, `AttachmentPickerPanel`, إلخ) بدل الاستبدال الكامل.
- قسم 6 (`ChatDetailScreen.kt`) هو أجزاء محددة فقط، مو الملف كامل — لازم تدمجها يدويًا بمكانها الصحيح داخل نسخة المشروع التاني من نفس الملف.
- الأنيميشن بالإيموجي (قسم داخل `ChatComponents.kt`) يعتمد على وجود `EmojiMessageUtils`, `NotoEmojiMap`, `LottieEmojiReaction` بنفس المشروع التاني — إذا مو موجودين، لازم تُنسخوا هم كمان أو تتجاهل هذا الجزء.
- الـ Edge Function (قسم 2) ما تشتغل تلقائيًا؛ لازم `supabase functions deploy` يدويًا بعد النسخ.
