package com.otpvault.app.core.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.otpvault.app.core.security.PinManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "otp_vault_prefs")

enum class ThemeMode { LIGHT, DARK, SYSTEM }

/**
 * Non-secret app preferences in DataStore. The PIN verifier lives here too,
 * but ONLY as a salted PBKDF2 hash (see PinManager) — never the raw PIN,
 * and never any encryption key (the master key stays in the Keystore).
 */
class AppPreferences(private val context: Context) : PinManager.PinStore {

    private object Keys {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val BIOMETRIC = booleanPreferencesKey("biometric_enabled")
        val AUTO_LOCK_SECONDS = intPreferencesKey("auto_lock_seconds")
        val LOCK_ON_BG = booleanPreferencesKey("lock_on_background")
        val DEFAULT_EXPIRY_MINUTES = intPreferencesKey("default_expiry_minutes")
        val AUTO_DELETE = booleanPreferencesKey("auto_delete_expired")
        val CLIPBOARD_CLEAR = booleanPreferencesKey("clipboard_clear")
        val CONFIRM_DELETE = booleanPreferencesKey("confirm_before_delete")
        val THEME = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val PIN_VERIFIER = stringPreferencesKey("pin_verifier")
    }

    val onboarded: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDED] ?: false }
    val biometricEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.BIOMETRIC] ?: false }
    val autoLockSeconds: Flow<Int> = context.dataStore.data.map { it[Keys.AUTO_LOCK_SECONDS] ?: 60 }
    val lockOnBackground: Flow<Boolean> = context.dataStore.data.map { it[Keys.LOCK_ON_BG] ?: true }
    val defaultExpiryMinutes: Flow<Int> = context.dataStore.data.map { it[Keys.DEFAULT_EXPIRY_MINUTES] ?: 5 }
    val autoDeleteExpired: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_DELETE] ?: true }
    val clipboardClear: Flow<Boolean> = context.dataStore.data.map { it[Keys.CLIPBOARD_CLEAR] ?: true }
    val confirmBeforeDelete: Flow<Boolean> = context.dataStore.data.map { it[Keys.CONFIRM_DELETE] ?: true }
    val themeMode: Flow<ThemeMode> = context.dataStore.data.map {
        runCatching { ThemeMode.valueOf(it[Keys.THEME] ?: "SYSTEM") }.getOrDefault(ThemeMode.SYSTEM)
    }
    val language: Flow<String> = context.dataStore.data.map { it[Keys.LANGUAGE] ?: "system" }

    suspend fun setOnboarded(v: Boolean) { context.dataStore.edit { it[Keys.ONBOARDED] = v } }
    suspend fun setBiometric(v: Boolean) { context.dataStore.edit { it[Keys.BIOMETRIC] = v } }
    suspend fun setAutoLockSeconds(v: Int) { context.dataStore.edit { it[Keys.AUTO_LOCK_SECONDS] = v } }
    suspend fun setLockOnBackground(v: Boolean) { context.dataStore.edit { it[Keys.LOCK_ON_BG] = v } }
    suspend fun setDefaultExpiryMinutes(v: Int) { context.dataStore.edit { it[Keys.DEFAULT_EXPIRY_MINUTES] = v } }
    suspend fun setAutoDeleteExpired(v: Boolean) { context.dataStore.edit { it[Keys.AUTO_DELETE] = v } }
    suspend fun setClipboardClear(v: Boolean) { context.dataStore.edit { it[Keys.CLIPBOARD_CLEAR] = v } }
    suspend fun setConfirmBeforeDelete(v: Boolean) { context.dataStore.edit { it[Keys.CONFIRM_DELETE] = v } }
    suspend fun setTheme(v: ThemeMode) { context.dataStore.edit { it[Keys.THEME] = v.name } }
    suspend fun setLanguage(v: String) { context.dataStore.edit { it[Keys.LANGUAGE] = v } }

    // ---- PinManager.PinStore bridge ----
    @Volatile private var snapshot: Map<String, String> = emptyMap()

    suspend fun refreshSnapshot() {
        val prefs = context.dataStore.data.first()
        snapshot = listOfNotNull(
            prefs[Keys.PIN_SALT]?.let { "pin_salt" to it },
            prefs[Keys.PIN_VERIFIER]?.let { "pin_verifier" to it }
        ).toMap()
    }

    override fun read(key: String): String? = snapshot[key]

    override fun write(key: String, value: String) {
        snapshot = snapshot + (key to value)
        val k = if (key == "pin_salt") Keys.PIN_SALT else Keys.PIN_VERIFIER
        CoroutineScope(Dispatchers.IO).launch { context.dataStore.edit { it[k] = value } }
    }

    override fun clear() {
        snapshot = emptyMap()
        CoroutineScope(Dispatchers.IO).launch {
            context.dataStore.edit { it.remove(Keys.PIN_SALT); it.remove(Keys.PIN_VERIFIER) }
        }
    }
}
