package com.example.ui

import androidx.compose.ui.graphics.Color

fun isUserOnline(lastSeenAt: String?, currentTime: java.time.Instant = java.time.Instant.now()): Boolean {
    if (lastSeenAt == null) return false
    return try {
        val cleanTimestamp = lastSeenAt.replace(" ", "T").let {
            if (it.matches(Regex(".*[+-]\\d{2}$"))) it + ":00" else it
        }
        val lastSeenTime = if (cleanTimestamp.contains("Z") || cleanTimestamp.contains("+") || cleanTimestamp.indexOf('-', cleanTimestamp.indexOf('T')) != -1) {
            java.time.Instant.parse(cleanTimestamp)
        } else {
            java.time.Instant.parse(cleanTimestamp + "Z")
        }
        val diffSeconds = java.time.temporal.ChronoUnit.SECONDS.between(lastSeenTime, currentTime)
        
        // المهلة الأصلية الصارمة (60 ثانية للماضي) و (5 ثوانٍ كحد أقصى للمستقبل بسبب تأخير الشبكة Ping)
        diffSeconds in -5..60
    } catch (e: Exception) {
        false
    }
}

@androidx.compose.runtime.Stable
@kotlinx.serialization.Serializable
data class PrivacySettings(
    @kotlinx.serialization.SerialName("profile_photo")
    val profilePhoto: String = "everyone",
    @kotlinx.serialization.SerialName("last_seen")
    val lastSeen: String = "everyone",
    @kotlinx.serialization.SerialName("read_receipts")
    val readReceipts: Boolean = true,
    @kotlinx.serialization.SerialName("typing_indicator")
    val typingIndicator: Boolean = true,
    @kotlinx.serialization.SerialName("disappearing_messages")
    val disappearingMessages: Boolean = false,
    @kotlinx.serialization.SerialName("screenshot_protection")
    val screenshotProtection: Boolean = false
)

@androidx.compose.runtime.Stable
@kotlinx.serialization.Serializable
data class Profile(
    val id: String,
    val username: String? = null,
    @kotlinx.serialization.SerialName("full_name")
    val fullName: String? = null,
    @kotlinx.serialization.SerialName("avatar_url")
    val avatarUrl: String? = null,
    @kotlinx.serialization.SerialName("is_online")
    val isOnline: Boolean? = false,
    @kotlinx.serialization.SerialName("last_seen_at")
    val lastSeenAt: String? = null,
    @kotlinx.serialization.SerialName("last_seen")
    val lastSeen: String? = null,
    val bio: String? = null,
    @kotlinx.serialization.SerialName("wallet_address")
    val walletAddress: String? = null,
    val chain: String? = null,
    @kotlinx.serialization.SerialName("contact_email")
    val contactEmail: String? = null,
    @kotlinx.serialization.SerialName("contact_phone")
    val contactPhone: String? = null,
    @kotlinx.serialization.SerialName("login_alerts_enabled")
    val loginAlertsEnabled: Boolean? = true,
    @kotlinx.serialization.SerialName("cryptvora_id")
    val cryptvoraId: String? = null,
    @kotlinx.serialization.SerialName("is_verified")
    val isVerified: Boolean? = false,
    @kotlinx.serialization.SerialName("privacy_settings")
    val privacySettings: PrivacySettings? = null
) {
    val isOnlineNow: Boolean
        get() = isUserOnline(lastSeenAt)

    val displayUsername: String get() { if (!username.isNullOrBlank()) return username; val derived = fullName?.replace(" ", "")?.lowercase(); return if (!derived.isNullOrBlank()) derived else "user_${id.take(6)}" }
    val displayName: String get() = fullName ?: displayUsername
    val avatarColor: Color
        get() {
            val colors = listOf(Color(0xFFE57373), Color(0xFF81C784), Color(0xFF64B5F6), Color(0xFFFFB74D), Color(0xFFBA68C8))
            val index = kotlin.math.abs(id.hashCode()) % colors.size
            return colors[index]
        }
}
