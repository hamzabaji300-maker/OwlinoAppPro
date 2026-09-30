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
}
