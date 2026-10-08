package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object NetworkUtils {

    /** هل يوجد اتصال بالإنترنت الآن؟ */
    fun isOnline(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return true
            val net = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(net) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            true
        }
    }

    private val keywords = listOf(
        "timeout", "timed out", "network", "connection", "unable to resolve host",
        "failed to connect", "unreachable", "no address associated", "socket", "ssl", "eof"
    )

    /**
     * هل سبب الفشل هو الشبكة (فتُعاد المحاولة لاحقًا) أم خطأ حقيقي لا تنفع معه الإعادة؟
     * نفحص الخطأ وأسبابه الداخلية، وإن لم نتأكد وكان الهاتف دون إنترنت نعدّه خطأ شبكة.
     */
    fun isNetworkError(context: Context?, e: Throwable): Boolean {
        if (e is UploadLimitException || e is LocalMediaException || e is java.io.FileNotFoundException) return false

        var t: Throwable? = e
        var depth = 0
        while (t != null && depth < 6) {
            if (t is java.io.FileNotFoundException) return false
            if (t is java.net.UnknownHostException ||
                t is java.net.ConnectException ||
                t is java.net.SocketException ||
                t is java.net.SocketTimeoutException ||
                t is javax.net.ssl.SSLException ||
                t is java.nio.channels.UnresolvedAddressException ||
                t is java.io.IOException
            ) return true
            val m = t.message?.lowercase() ?: ""
            if (keywords.any { it in m }) return true
            t = t.cause
            depth++
        }
        return context != null && !isOnline(context)
    }
}
