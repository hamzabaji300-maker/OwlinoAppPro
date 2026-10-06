package com.example.ui

object GlobalAppState {
    var roomChats: List<com.example.data.ChatEntity>? = null
    var cachedChats: List<com.example.cache.CachedChat> = emptyList()
    var liveMyProfile: com.example.ui.Profile? = null
    // يبقى true طول ما التطبيق مفتوح على نفس الحساب — يترجع false بس عند تسجيل الخروج
    // (باش القائمة الرمادية تبان مرة وحدة بعد الدخول، ماشي كل ما نرجعو لشاشة الدردشات)
    var hasCompletedInitialChatSync: Boolean = false
    // الدردشة المفتوحة حالياً — الرسائل الواصلة فيها ما تتحسبش "غير مقروءة" في القائمة
    @Volatile var openChatId: String? = null
    // يصير true ملي الشاشة الأولى تكون جاهزة — الـ Splash كيبقى ظاهر حتى ذاك الوقت
    @Volatile var startupReady: Boolean = false
    @Volatile var lastSyncTime: Long = 0L

    private val _batterySaverFlow = kotlinx.coroutines.flow.MutableStateFlow(false)
    val batterySaverFlow: kotlinx.coroutines.flow.StateFlow<Boolean> = _batterySaverFlow
    var isBatterySaverEnabled: Boolean
        get() = _batterySaverFlow.value
        set(value) { _batterySaverFlow.value = value }
}
