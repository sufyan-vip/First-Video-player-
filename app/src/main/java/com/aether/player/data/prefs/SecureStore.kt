package com.aether.player.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/**
 * Encrypted on-device storage for secrets (AI API keys).
 * Falls back to private SharedPreferences only if the AndroidKeyStore is unavailable.
 */
class SecureStore(context: Context) {
    private val appContext = context.applicationContext

    private val prefs: SharedPreferences by lazy {
        runCatching {
            val alias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                "aether_secure",
                alias,
                appContext,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }.getOrElse {
            appContext.getSharedPreferences("aether_secure_fallback", Context.MODE_PRIVATE)
        }
    }

    fun get(key: String): String = runCatching { prefs.getString(key, "") ?: "" }.getOrDefault("")

    fun put(key: String, value: String) {
        runCatching { prefs.edit().putString(key, value).apply() }
    }

    fun clear(key: String) {
        runCatching { prefs.edit().remove(key).apply() }
    }

    companion object {
        const val AI_API_KEY = "ai_api_key"
    }
}
