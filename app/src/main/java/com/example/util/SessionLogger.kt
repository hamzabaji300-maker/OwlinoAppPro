package com.example.util

import android.os.Build
import io.github.jan.supabase.postgrest.postgrest
import com.example.supabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object SessionLogger {
    private val lastEventTimes = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private const val DEBOUNCE_TIME_MS = 5000L
    private val mutex = Mutex()

    suspend fun logSession(userId: String, eventType: String, location: String? = null) {
        val eventKey = "$userId-$eventType"
        val now = System.currentTimeMillis()

        mutex.withLock {
            val lastTime = lastEventTimes[eventKey] ?: 0L
            if (now - lastTime < DEBOUNCE_TIME_MS) {
                return
            }
            lastEventTimes[eventKey] = now
        }

        try {
            withContext(Dispatchers.IO) {
                val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
                val osVersion = Build.VERSION.RELEASE
                val data = mutableMapOf<String, String>(
                    "user_id" to userId,
                    "event_type" to eventType,
                    "device_name" to deviceName,
                    "os_version" to osVersion
                )
                if (location != null) {
                    data["location"] = location
                }
                supabase.postgrest["login_sessions"].insert(data)
            }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun fetchLocation(): String? {
        return try {
            val jsonResponse = withTimeoutOrNull(3000) {
                withContext(Dispatchers.IO) {
                    java.net.URL("https://get.geojs.io/v1/ip/geo.json").readText()
                }
            }
            if (jsonResponse != null) {
                val jsonObject = org.json.JSONObject(jsonResponse)
                val city = jsonObject.optString("city", "")
                val country = jsonObject.optString("country", "")
                if (city.isNotBlank() || country.isNotBlank()) {
                    val locStr = listOf(city, country).filter { it.isNotBlank() }.joinToString(", ")
                    locStr
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Throwable) {
            null
        }
    }
}
