package com.example.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecurityPreferences(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPrefs = EncryptedSharedPreferences.create(
        context,
        "secure_app_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var isBiometricEnabled: Boolean
        get() = sharedPrefs.getBoolean("biometric_enabled", false)
        set(value) = sharedPrefs.edit().putBoolean("biometric_enabled", value).apply()

    var pinCode: String?
        get() = sharedPrefs.getString("pin_code", null)
        set(value) = sharedPrefs.edit().putString("pin_code", value).apply()

    var appPassword: String?
        get() = sharedPrefs.getString("app_password", null)
        set(value) = sharedPrefs.edit().putString("app_password", value).apply()

    var autoLockTime: String
        get() = sharedPrefs.getString("auto_lock_time", "Immediately") ?: "Immediately"
        set(value) = sharedPrefs.edit().putString("auto_lock_time", value).apply()
}
