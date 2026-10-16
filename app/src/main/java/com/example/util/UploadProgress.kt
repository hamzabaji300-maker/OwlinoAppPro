package com.example.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** نسبة تقدّم تجهيز ورفع كل رسالة (من 0 إلى 1)، مفتاحها معرّف الرسالة. تقرأها فقاعات الرسائل. */
object UploadProgress {
    private val _state = MutableStateFlow<Map<String, Float>>(emptyMap())
    val state: StateFlow<Map<String, Float>> = _state

    fun set(id: String?, fraction: Float) {
        if (id == null) return
        val f = fraction.coerceIn(0f, 1f)
        _state.update { it + (id to f) }
    }

    fun clear(id: String?) {
        if (id == null) return
        _state.update { it - id }
    }
}
