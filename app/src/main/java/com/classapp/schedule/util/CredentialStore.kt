package com.classapp.schedule.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

object CredentialStore {

    private const val PREFS_FILE = "secure_credentials"
    private const val KEY_PORTAL_URL = "portal_url"
    private const val KEY_USERNAME = "username"
    private const val KEY_PASSWORD = "password"
    private const val KEY_AUTO_FETCH = "auto_fetch_enabled"
    private const val KEY_LAST_SYNC = "last_sync_time"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        prefs = EncryptedSharedPreferences.create(
            PREFS_FILE,
            masterKey,
            context.applicationContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    var portalUrl: String
        get() = prefs.getString(KEY_PORTAL_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PORTAL_URL, value).apply()

    var username: String
        get() = prefs.getString(KEY_USERNAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USERNAME, value).apply()

    var password: String
        get() = prefs.getString(KEY_PASSWORD, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PASSWORD, value).apply()

    var autoFetchEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_FETCH, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_FETCH, value).apply()

    var lastSyncTime: Long
        get() = prefs.getLong(KEY_LAST_SYNC, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC, value).apply()

    fun hasCredentials(): Boolean {
        return portalUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()
    }

    fun clearCredentials() {
        prefs.edit()
            .remove(KEY_PORTAL_URL)
            .remove(KEY_USERNAME)
            .remove(KEY_PASSWORD)
            .apply()
    }
}
