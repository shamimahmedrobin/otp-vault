package com.otpvault.app.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otpvault.app.core.crypto.CryptoManager
import com.otpvault.app.core.preferences.AppPreferences
import com.otpvault.app.core.preferences.ThemeMode
import com.otpvault.app.core.security.PinManager
import com.otpvault.app.data.local.dao.OtpDao
import com.otpvault.app.data.local.dao.ServiceDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val biometric: Boolean = false,
    val autoLockSeconds: Int = 60,
    val lockOnBackground: Boolean = true,
    val defaultExpiryMinutes: Int = 5,
    val autoDelete: Boolean = true,
    val clipboardClear: Boolean = true,
    val confirmDelete: Boolean = true,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val language: String = "system"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: AppPreferences,
    private val pinManager: PinManager,
    private val otpDao: OtpDao,
    private val serviceDao: ServiceDao,
    private val crypto: CryptoManager
) : ViewModel() {

    val state: StateFlow<SettingsUiState> = combine(
        prefs.biometricEnabled, prefs.autoLockSeconds, prefs.lockOnBackground,
        prefs.defaultExpiryMinutes, prefs.autoDeleteExpired
    ) { b, a, l, d, ad -> SettingsUiState(b, a, l, d, ad) }.combine(prefs.clipboardClear) { s, c -> s.copy(clipboardClear = c) }
        .combine(prefs.confirmBeforeDelete) { s, c -> s.copy(confirmDelete = c) }
        .combine(prefs.themeMode) { s, t -> s.copy(theme = t) }
        .combine(prefs.language) { s, l -> s.copy(language = l) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setBiometric(v: Boolean) = viewModelScope.launch { prefs.setBiometric(v) }
    fun setAutoLock(v: Int) = viewModelScope.launch { prefs.setAutoLockSeconds(v) }
    fun setLockOnBg(v: Boolean) = viewModelScope.launch { prefs.setLockOnBackground(v) }
    fun setDefaultExpiry(v: Int) = viewModelScope.launch { prefs.setDefaultExpiryMinutes(v) }
    fun setAutoDelete(v: Boolean) = viewModelScope.launch { prefs.setAutoDeleteExpired(v) }
    fun setClipboardClear(v: Boolean) = viewModelScope.launch { prefs.setClipboardClear(v) }
    fun setConfirmDelete(v: Boolean) = viewModelScope.launch { prefs.setConfirmBeforeDelete(v) }
    fun setTheme(v: ThemeMode) = viewModelScope.launch { prefs.setTheme(v) }
    fun setLanguage(v: String) = viewModelScope.launch { prefs.setLanguage(v) }

    fun verifyPin(pin: String): Boolean = pinManager.verifyPin(pin)
    fun changePin(newPin: String) { pinManager.setPin(newPin) }

    fun deleteAllData(onDone: () -> Unit) {
        viewModelScope.launch {
            otpDao.deleteAll()
            pinManager.clear()
            crypto.deleteKey()
            onDone()
        }
    }
}
