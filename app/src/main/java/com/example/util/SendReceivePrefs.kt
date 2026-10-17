package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.ui.Attachment
import com.example.ui.AttachmentType

/**
 * إعدادات «الإرسال والاستقبال»: وضع السرعة، التنزيل التلقائي (واي فاي / بيانات الجوال)، جودة الرفع.
 * الحقول حالة Compose، فتتحدث الشاشة فورًا، ويقرؤها باقي التطبيق مباشرة (التنزيل، ضغط الصور، سؤال جودة الفيديو).
 */
object SendReceivePrefs {
    private const val PREFS = "send_receive_prefs"

    const val MODE_SAVER = 0
    const val MODE_BALANCED = 1
    const val MODE_FAST = 2

    /** محطات شريط «أقصى حجم للتنزيل التلقائي» بالميغابايت. */
    val SIZE_STEPS_MB = intArrayOf(1, 3, 5, 10, 25, 50)

    /** محطات جودة الصور عند الإرسال: (جودة JPEG، أطول ضلع بالبكسل). */
    private val PHOTO_QUALITY = arrayOf(68 to 1280, 80 to 1600, 90 to 2560)

    var speedMode by mutableStateOf(MODE_BALANCED)

    var wifiPhoto by mutableStateOf(true)
    var wifiVideo by mutableStateOf(true)
    var wifiFile by mutableStateOf(false)
    var wifiMaxIdx by mutableStateOf(4)

    var mobPhoto by mutableStateOf(true)
    var mobVideo by mutableStateOf(false)
    var mobFile by mutableStateOf(false)
    var mobMaxIdx by mutableStateOf(1)

    /** 0 = خفيفة (إرسال أسرع)، 1 = عادية، 2 = عالية. */
    var photoQualityIdx by mutableStateOf(1)

    /** 0 = اسألني كل مرة، 1 = 720p، 2 = 1080p. */
    var videoMode by mutableStateOf(0)

    val jpegQuality: Int get() = PHOTO_QUALITY[photoQualityIdx.coerceIn(0, 2)].first
    val maxImageEdge: Int get() = PHOTO_QUALITY[photoQualityIdx.coerceIn(0, 2)].second

    fun load(context: Context) {
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        speedMode = p.getInt("speedMode", MODE_BALANCED)
        wifiPhoto = p.getBoolean("wifiPhoto", true)
        wifiVideo = p.getBoolean("wifiVideo", true)
        wifiFile = p.getBoolean("wifiFile", false)
        wifiMaxIdx = p.getInt("wifiMaxIdx", 4).coerceIn(0, SIZE_STEPS_MB.size - 1)
        mobPhoto = p.getBoolean("mobPhoto", true)
        mobVideo = p.getBoolean("mobVideo", false)
        mobFile = p.getBoolean("mobFile", false)
        mobMaxIdx = p.getInt("mobMaxIdx", 1).coerceIn(0, SIZE_STEPS_MB.size - 1)
        photoQualityIdx = p.getInt("photoQualityIdx", 1).coerceIn(0, 2)
        videoMode = p.getInt("videoMode", 0).coerceIn(0, 2)
    }

    fun save(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt("speedMode", speedMode)
            .putBoolean("wifiPhoto", wifiPhoto).putBoolean("wifiVideo", wifiVideo).putBoolean("wifiFile", wifiFile)
            .putInt("wifiMaxIdx", wifiMaxIdx)
            .putBoolean("mobPhoto", mobPhoto).putBoolean("mobVideo", mobVideo).putBoolean("mobFile", mobFile)
            .putInt("mobMaxIdx", mobMaxIdx)
            .putInt("photoQualityIdx", photoQualityIdx)
            .putInt("videoMode", videoMode)
            .apply()
    }

    /** يضبط كل الخيارات دفعة واحدة حسب وضع السرعة المختار. */
    fun applyPreset(context: Context, mode: Int) {
        speedMode = mode
        when (mode) {
            MODE_SAVER -> {
                wifiPhoto = true; wifiVideo = false; wifiFile = false; wifiMaxIdx = 2
                mobPhoto = false; mobVideo = false; mobFile = false; mobMaxIdx = 0
                photoQualityIdx = 0; videoMode = 1
            }
            MODE_FAST -> {
                wifiPhoto = true; wifiVideo = true; wifiFile = true; wifiMaxIdx = 5
                mobPhoto = true; mobVideo = true; mobFile = false; mobMaxIdx = 3
                photoQualityIdx = 1; videoMode = 1
            }
            else -> {
                wifiPhoto = true; wifiVideo = true; wifiFile = false; wifiMaxIdx = 4
                mobPhoto = true; mobVideo = false; mobFile = false; mobMaxIdx = 1
                photoQualityIdx = 1; videoMode = 0
            }
        }
        save(context)
    }

    enum class Net { WIFI, MOBILE, ROAMING, NONE }

    fun currentNet(context: Context): Net {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return Net.NONE
            val net = cm.activeNetwork ?: return Net.NONE
            val caps = cm.getNetworkCapabilities(net) ?: return Net.NONE
            when {
                !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> Net.NONE
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> Net.WIFI
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->
                    if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING)) Net.MOBILE else Net.ROAMING
                else -> Net.WIFI
            }
        } catch (e: Exception) {
            Net.NONE
        }
    }

    /** هل يُنزَّل هذا المرفق تلقائيًا الآن؟ (نوع الشبكة + نوع الملف + الحجم). التجوال: لا تنزيل تلقائي أبدًا. */
    fun allowsAutoDownload(context: Context, att: Attachment): Boolean {
        val net = currentNet(context)
        if (net == Net.NONE || net == Net.ROAMING) return false
        val wifi = net == Net.WIFI
        val typeAllowed = when (att.type) {
            AttachmentType.IMAGE -> if (wifi) wifiPhoto else mobPhoto
            AttachmentType.VIDEO -> if (wifi) wifiVideo else mobVideo
            AttachmentType.DOCUMENT -> if (wifi) wifiFile else mobFile
            else -> false
        }
        if (!typeAllowed) return false
        val limit = SIZE_STEPS_MB[if (wifi) wifiMaxIdx else mobMaxIdx] * 1024L * 1024L
        val size = att.fileSize
        // الحجم مجهول (رسائل قديمة): نسمح للصور فقط لأنها مضغوطة وصغيرة
        if (size == null || size <= 0L) return att.type == AttachmentType.IMAGE
        return size <= limit
    }
}
