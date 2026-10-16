package com.example.ui.i18n

import android.content.Context
import android.content.SharedPreferences
import com.example.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

object TranslationManager {
    private const val PREFS_NAME = "translation_prefs"
    private const val KEY_LANGUAGE = "language_code"
    private var prefs: SharedPreferences? = null

    private val _currentLanguageCode = MutableStateFlow("en")
    val currentLanguageCode: StateFlow<String> = _currentLanguageCode.asStateFlow()

    private val _currentTranslation = MutableStateFlow(englishTranslation)
    val currentTranslation: StateFlow<Translation> = _currentTranslation.asStateFlow()

    fun init(context: Context, scope: CoroutineScope) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedLang = prefs?.getString(KEY_LANGUAGE, "en") ?: "en"
        setLanguage(savedLang, saveToPrefs = false)

        scope.launch(Dispatchers.IO) {
            try {
                supabase.auth.awaitInitialization()
                val userId = supabase.auth.currentUserOrNull()?.id
                if (userId != null) {
                    val result = supabase.postgrest["profiles"]
                        .select {
                            filter { eq("id", userId) }
                        }.decodeSingleOrNull<JsonObject>()
                    
                    val code = result?.get("language_code")?.jsonPrimitive?.contentOrNull
                    if (code != null && code.isNotBlank()) {
                        // Always sync from DB if it exists, to handle cross-device
                        if (code != currentLanguageCode.value) {
                            setLanguage(code, saveToPrefs = true)
                        }
                    } else {
                        // If DB doesn't have it, save the current one to DB
                        supabase.postgrest["profiles"].update(
                            mapOf("language_code" to currentLanguageCode.value)
                        ) {
                            filter { eq("id", userId) }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setLanguage(code: String, saveToPrefs: Boolean = true) {
        _currentLanguageCode.value = code
        _currentTranslation.value = when (code.lowercase()) {
            "ar" -> arabicTranslation
            "fr" -> frenchTranslation
            else -> englishTranslation
        }
        if (saveToPrefs) {
            prefs?.edit()?.putString(KEY_LANGUAGE, code)?.apply()
        }
    }
}
