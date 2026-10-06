package com.example.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

object SharedPreferencesHelper {
    private const val PREFS_NAME = "cryptvora_settings"
    
    fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getBoolean(context: Context, key: String, defaultValue: Boolean): Boolean {
        return getPrefs(context).getBoolean(key, defaultValue)
    }

    fun setBoolean(context: Context, key: String, value: Boolean) {
        getPrefs(context).edit().putBoolean(key, value).apply()
    }
}

@Composable
fun rememberBooleanPreference(key: String, defaultValue: Boolean): MutableState<Boolean> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(SharedPreferencesHelper.getBoolean(context, key, defaultValue)) }
    return remember(key) {
        object : MutableState<Boolean> {
            override var value: Boolean
                get() = state.value
                set(v) {
                    state.value = v
                    SharedPreferencesHelper.setBoolean(context, key, v)
                }
            override fun component1() = value
            override fun component2(): (Boolean) -> Unit = { value = it }
        }
    }
}
