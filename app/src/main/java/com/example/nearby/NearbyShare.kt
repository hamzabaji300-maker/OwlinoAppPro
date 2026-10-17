package com.example.nearby

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.core.content.ContextCompat
import com.example.util.MediaStorage
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * إرسال واستقبال الملفات بين هاتفين قريبين بدون إنترنت (بلوتوث للاكتشاف ثم Wi-Fi مباشر للنقل)
 * عبر Google Nearby Connections، مثل SHAREit وXender.
 * البروتوكول: قبل كل ملف تُرسل رسالة نصية قصيرة بالاسم والحجم والنوع: OWL1|<fileId>|<size>|<mime>|<name>
 */
object NearbyShare {
    private const val SERVICE_ID = "com.owlino.nearby.share"

    enum class Mode { IDLE, RECEIVING, SENDING }
    enum class State { PROGRESS, DONE, FAILED }

    data class Peer(val id: String, val name: String)
    data class Incoming(val endpointId: String, val name: String)
    data class Transfer(
        val key: Long,
        val name: String,
        val total: Long,
        val done: Long,
        val outgoing: Boolean,
        val state: State,
        val savedPath: String? = null
    )
    private data class Meta(val name: String, val size: Long, val mime: String)

    private val _mode = MutableStateFlow(Mode.IDLE)
    val mode: StateFlow<Mode> = _mode
    private val _peers = MutableStateFlow<List<Peer>>(emptyList())
    val peers: StateFlow<List<Peer>> = _peers
    private val _incoming = MutableStateFlow<Incoming?>(null)
    val incoming: StateFlow<Incoming?> = _incoming
    private val _connected = MutableStateFlow<Map<String, String>>(emptyMap())
    val connected: StateFlow<Map<String, String>> = _connected
    private val _transfers = MutableStateFlow<List<Transfer>>(emptyList())
    val transfers: StateFlow<List<Transfer>> = _transfers
    private val _info = MutableStateFlow<String?>(null)
    val info: StateFlow<String?> = _info

    private var appContext: Context? = null
    private var client: ConnectionsClient? = null
    private var deviceName: String = "Owlino"
    private var queued: List<Uri> = emptyList()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val metaByFile = ConcurrentHashMap<Long, Meta>()
    private val filePayloads = ConcurrentHashMap<Long, Payload>()

    // ───────── الصلاحيات ─────────
    /**
     * الأذونات المطلوبة وقت التشغيل. مكتبة Nearby تشترط إذن الموقع (ACCESS_COARSE_LOCATION) على كل الإصدارات
     * حتى الحديثة (الخطأ 8034)، لذلك نطلب الموقع الدقيق والتقريبي معًا (مطلوب في أندرويد 12+)،
     * مع أذونات البلوتوث (12+) و«الأجهزة القريبة» (13+). لا يتم تتبع موقعك.
     */
    fun requiredPermissions(): List<String> {
        val l = mutableListOf<String>()
        l += Manifest.permission.ACCESS_FINE_LOCATION
        l += Manifest.permission.ACCESS_COARSE_LOCATION
        if (Build.VERSION.SDK_INT >= 31) {
            l += Manifest.permission.BLUETOOTH_ADVERTISE
            l += Manifest.permission.BLUETOOTH_CONNECT
            l += Manifest.permission.BLUETOOTH_SCAN
        }
        if (Build.VERSION.SDK_INT >= 33) l += Manifest.permission.NEARBY_WIFI_DEVICES
        return l
    }

    private fun granted(context: Context, perm: String) =
        ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED

    /** يكفي أحد إذني الموقع (إن اختار المستخدم «تقريبي» يُمنح coarse فقط)، وباقي الأذونات كلها مطلوبة. */
    fun hasPermissions(context: Context): Boolean {
        val locationOk = granted(context, Manifest.permission.ACCESS_COARSE_LOCATION) ||
            granted(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val others = requiredPermissions().filter {
            it != Manifest.permission.ACCESS_FINE_LOCATION && it != Manifest.permission.ACCESS_COARSE_LOCATION
        }.all { granted(context, it) }
        return locationOk && others
    }

    private fun friendlyError(prefix: String, e: Exception): String {
        val m = e.message ?: ""
        val hint = when {
            m.contains("LOCATION", true) || m.contains("MISSING_PERMISSION", true) ->
                " — امنح التطبيق إذن الموقع والأجهزة القريبة من إعدادات الهاتف، وفعّل خدمة الموقع (GPS) ثم أعد المحاولة."
            m.contains("BLUETOOTH", true) || m.contains("8038", true) || m.contains("8001", true) ->
                " — تأكد من تشغيل البلوتوث والواي فاي."
            else -> ""
        }
        return "$prefix: $m$hint"
    }

    // ───────── التحكم ─────────
    private fun ensureClient(context: Context): ConnectionsClient {
        appContext = context.applicationContext
        return client ?: Nearby.getConnectionsClient(appContext!!).also { client = it }
    }

    private fun resetLinks() {
        try { client?.stopAdvertising() } catch (e: Exception) {}
        try { client?.stopDiscovery() } catch (e: Exception) {}
        try { client?.stopAllEndpoints() } catch (e: Exception) {}
        _peers.value = emptyList()
        _incoming.value = null
        _connected.value = emptyMap()
    }

    fun stopAll() {
        resetLinks()
        queued = emptyList()
        _mode.value = Mode.IDLE
    }

    fun clearTransfers() { _transfers.value = emptyList() }

    /** وضع الاستقبال: يظهر هاتفك للمرسلين القريبين. */
    fun startReceiving(context: Context, name: String) {
        val c = ensureClient(context)
        resetLinks()
        deviceName = name
        _info.value = null
        val options = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        c.startAdvertising(name, SERVICE_ID, connectionCallback, options)
            .addOnSuccessListener { _mode.value = Mode.RECEIVING }
            .addOnFailureListener { e -> _mode.value = Mode.IDLE; _info.value = friendlyError("تعذّر بدء الاستقبال", e) }
    }

    /** وضع الإرسال: يبحث عن الهواتف القريبة، وبعد الاتصال يرسل [uris]. */
    fun startSending(context: Context, name: String, uris: List<Uri>) {
        val c = ensureClient(context)
        resetLinks()
        deviceName = name
        queued = uris
        _info.value = null
        val options = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        c.startDiscovery(SERVICE_ID, discoveryCallback, options)
            .addOnSuccessListener { _mode.value = Mode.SENDING }
            .addOnFailureListener { e -> _mode.value = Mode.IDLE; _info.value = friendlyError("تعذّر البحث عن الأجهزة", e) }
    }

    fun connectTo(peer: Peer) {
        client?.requestConnection(deviceName, peer.id, connectionCallback)
            ?.addOnFailureListener { e -> _info.value = friendlyError("تعذّر الاتصال", e) }
    }

    fun acceptIncoming() {
        val inc = _incoming.value ?: return
        client?.acceptConnection(inc.endpointId, payloadCallback)
        _incoming.value = null
    }

    fun rejectIncoming() {
        val inc = _incoming.value ?: return
        client?.rejectConnection(inc.endpointId)
        _incoming.value = null
    }

    // ───────── الاكتشاف والاتصال ─────────
    private val discoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            if (_peers.value.none { it.id == endpointId }) {
                _peers.value = _peers.value + Peer(endpointId, info.endpointName)
            }
        }
        override fun onEndpointLost(endpointId: String) {
            _peers.value = _peers.value.filterNot { it.id == endpointId }
        }
    }

    private val connectionCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            if (info.isIncomingConnection) {
                // الطرف المستقبِل: المستخدم يقرر القبول
                _incoming.value = Incoming(endpointId, info.endpointName)
            } else {
                // نحن من طلب الاتصال (المرسل): نقبل تلقائيًا، والقرار عند الطرف الآخر
                client?.acceptConnection(endpointId, payloadCallback)
            }
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                val name = _peers.value.firstOrNull { it.id == endpointId }?.name
                    ?: _incoming.value?.name ?: endpointId
                _connected.value = _connected.value + (endpointId to name)
                _incoming.value = null
                if (_mode.value == Mode.SENDING) sendQueued(endpointId)
            } else {
                _incoming.value = null
                _info.value = "لم يتم الاتصال (رُفض الطلب أو فشل)."
            }
        }

        override fun onDisconnected(endpointId: String) {
            _connected.value = _connected.value - endpointId
        }
    }

    // ───────── الإرسال ─────────
    private fun sendQueued(endpointId: String) {
        val ctx = appContext ?: return
        val c = client ?: return
        val files = queued
        scope.launch {
            for (uri in files) {
                try {
                    var name = "file"
                    var size = -1L
                    ctx.contentResolver.query(uri, null, null, null, null)?.use { cur ->
                        if (cur.moveToFirst()) {
                            val ni = cur.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            val si = cur.getColumnIndex(OpenableColumns.SIZE)
                            if (ni >= 0) name = cur.getString(ni) ?: name
                            if (si >= 0 && !cur.isNull(si)) size = cur.getLong(si)
                        }
                    }
                    val mime = ctx.contentResolver.getType(uri) ?: "application/octet-stream"
                    val pfd = ctx.contentResolver.openFileDescriptor(uri, "r") ?: continue
                    val filePayload = Payload.fromFile(pfd)
                    val meta = "OWL1|${filePayload.id}|$size|$mime|$name".toByteArray(Charsets.UTF_8)
                    addTransfer(Transfer(filePayload.id, name, size.coerceAtLeast(0), 0, true, State.PROGRESS))
                    c.sendPayload(endpointId, Payload.fromBytes(meta))
                    c.sendPayload(endpointId, filePayload)
                } catch (e: Exception) {
                    _info.value = "تعذّر إرسال ملف: ${e.message ?: ""}"
                }
            }
        }
    }

    // ───────── الاستقبال ─────────
    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            when (payload.type) {
                Payload.Type.BYTES -> {
                    val text = payload.asBytes()?.toString(Charsets.UTF_8) ?: return
                    val parts = text.split("|", limit = 5)
                    if (parts.size == 5 && parts[0] == "OWL1") {
                        val fileId = parts[1].toLongOrNull() ?: return
                        val meta = Meta(name = sanitize(parts[4]), size = parts[2].toLongOrNull() ?: -1L, mime = parts[3])
                        metaByFile[fileId] = meta
                        addTransfer(Transfer(fileId, meta.name, meta.size.coerceAtLeast(0), 0, false, State.PROGRESS))
                    }
                }
                Payload.Type.FILE -> {
                    filePayloads[payload.id] = payload
                    if (_transfers.value.none { it.key == payload.id }) {
                        addTransfer(Transfer(payload.id, "file", 0, 0, false, State.PROGRESS))
                    }
                }
                else -> {}
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            val id = update.payloadId
            when (update.status) {
                PayloadTransferUpdate.Status.IN_PROGRESS -> updateTransfer(id) {
                    it.copy(done = update.bytesTransferred, total = if (update.totalBytes > 0) update.totalBytes else it.total)
                }
                PayloadTransferUpdate.Status.SUCCESS -> {
                    if (filePayloads.containsKey(id)) finalizeIncoming(id)
                    else updateTransfer(id) { it.copy(done = it.total, state = State.DONE) }
                }
                PayloadTransferUpdate.Status.FAILURE, PayloadTransferUpdate.Status.CANCELED -> {
                    filePayloads.remove(id)
                    updateTransfer(id) { it.copy(state = State.FAILED) }
                }
                else -> {}
            }
        }
    }

    private fun finalizeIncoming(id: Long) {
        val payload = filePayloads.remove(id) ?: return
        val ctx = appContext ?: return
        scope.launch {
            try {
                // قد تصل رسالة الاسم بعد الملف بلحظات
                var meta = metaByFile[id]
                var tries = 0
                while (meta == null && tries < 30) { delay(100); meta = metaByFile[id]; tries++ }
                val name = sanitize(meta?.name ?: "file_$id")
                val kind = kindFor(meta?.mime, name)
                MediaStorage.ensureFolders(ctx)
                val dir = MediaStorage.dirFor(ctx, kind).also { it.mkdirs() }
                val dest = uniqueFile(dir, name)

                val f = payload.asFile()
                val uri = f?.asUri()
                val input = when {
                    uri != null -> ctx.contentResolver.openInputStream(uri)
                    else -> @Suppress("DEPRECATION") f?.asJavaFile()?.inputStream()
                } ?: throw java.io.IOException("no file")
                input.use { ins -> dest.outputStream().use { out -> ins.copyTo(out) } }
                // ننظّف النسخة المؤقتة التي تضعها المكتبة في التنزيلات
                try { if (uri != null) ctx.contentResolver.delete(uri, null, null) } catch (e: Exception) {}
                MediaStorage.scanFile(ctx, dest)
                metaByFile.remove(id)
                updateTransfer(id) { it.copy(name = dest.name, done = it.total.coerceAtLeast(dest.length()), total = it.total.coerceAtLeast(dest.length()), state = State.DONE, savedPath = dest.absolutePath) }
            } catch (e: Exception) {
                updateTransfer(id) { it.copy(state = State.FAILED) }
            }
        }
    }

    // ───────── أدوات ─────────
    private fun addTransfer(t: Transfer) {
        _transfers.value = _transfers.value.filterNot { it.key == t.key } + t
    }

    private fun updateTransfer(id: Long, f: (Transfer) -> Transfer) {
        _transfers.value = _transfers.value.map { if (it.key == id) f(it) else it }
    }

    private fun sanitize(raw: String): String {
        val cleaned = raw.replace(Regex("[\\u0000-\\u001f/\\\\:*?\"<>|]"), "_").trim().trimStart('.')
        return cleaned.ifEmpty { "file" }.take(120)
    }

    private fun kindFor(mime: String?, name: String): MediaStorage.Kind {
        val m = (mime ?: android.webkit.MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(name.substringAfterLast('.', "").lowercase()) ?: "").lowercase()
        return when {
            m.startsWith("image/") -> MediaStorage.Kind.IMAGE
            m.startsWith("video/") -> MediaStorage.Kind.VIDEO
            m.startsWith("audio/") -> MediaStorage.Kind.AUDIO
            else -> MediaStorage.Kind.DOCUMENT
        }
    }

    private fun uniqueFile(dir: File, name: String): File {
        var f = File(dir, name)
        if (!f.exists()) return f
        val base = name.substringBeforeLast('.', name)
        val ext = name.substringAfterLast('.', "")
        var i = 1
        while (f.exists()) {
            f = File(dir, if (ext.isEmpty()) "$base ($i)" else "$base ($i).$ext")
            i++
        }
        return f
    }
}
