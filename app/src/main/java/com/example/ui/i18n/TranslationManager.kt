package com.example.ui.i18n

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** نسخة مبسّطة للمختبر: بدون Supabase. اللغة الافتراضية العربية. */
object TranslationManager {
    private val _currentLanguageCode = MutableStateFlow("ar")
    val currentLanguageCode: StateFlow<String> = _currentLanguageCode.asStateFlow()
    private val _currentTranslation = MutableStateFlow(arabicTranslation)
    val currentTranslation: StateFlow<Translation> = _currentTranslation.asStateFlow()
}
