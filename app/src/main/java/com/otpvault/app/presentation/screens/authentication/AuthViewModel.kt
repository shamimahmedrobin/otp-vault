package com.otpvault.app.presentation.screens.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otpvault.app.core.preferences.AppPreferences
import com.otpvault.app.core.security.AppLockManager
import com.otpvault.app.core.security.PinManager
import com.otpvault.app.core.security.PinRateLimiter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val onboarded: Boolean = false,
    val pinSet: Boolean = false,
    val locked: Boolean = true,
    val error: String? = null,
    val lockedSeconds: Int = 0
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val pinManager: PinManager,
    private val rateLimiter: PinRateLimiter,
    private val lockManager: AppLockManager,
    private val prefs: AppPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { prefs.refreshSnapshot() }
        viewModelScope.launch {
            prefs.onboarded.collect { onboardedValue ->
                _state.value = _state.value.copy(
                    onboarded = onboardedValue,
                    pinSet = pinManager.isPinSet(),
                    locked = pinManager.isPinSet()
                )
                if (!pinManager.isPinSet()) lockManager.lock() else lockManager.lock()
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch { prefs.setOnboarded(true) }
    }

    fun createPin(pin: String): Boolean {
        return try {
            pinManager.setPin(pin)
            rateLimiter.onSuccess()
            lockManager.unlock()
            _state.value = _state.value.copy(pinSet = true, locked = false, error = null)
            true
        } catch (e: Exception) {
            _state.value = _state.value.copy(error = e.message)
            false
        }
    }

    fun unlockWithPin(pin: String): Boolean {
        when (val check = rateLimiter.check()) {
            is PinRateLimiter.Result.Locked -> {
                _state.value = _state.value.copy(lockedSeconds = check.remainingSeconds, error = "locked")
                return false
            }
            PinRateLimiter.Result.Allowed -> Unit
        }
        return if (pinManager.verifyPin(pin)) {
            rateLimiter.onSuccess()
            lockManager.unlock()
            _state.value = _state.value.copy(locked = false, error = null, lockedSeconds = 0)
            true
        } else {
            rateLimiter.onFailure()
            _state.value = _state.value.copy(error = "wrong")
            false
        }
    }

    fun unlockWithBiometric() {
        rateLimiter.onSuccess()
        lockManager.unlock()
        _state.value = _state.value.copy(locked = false, error = null)
    }

    fun lock() {
        lockManager.lock()
        _state.value = _state.value.copy(locked = true)
    }

    fun isLocked(): Boolean = lockManager.isLocked()
}
