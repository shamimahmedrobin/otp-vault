package com.otpvault.app.core.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks lock state: locked on cold start (when a PIN exists), after the
 * configured inactivity timeout, and immediately on background when enabled.
 */
class AppLockManager {
    private val _locked = MutableStateFlow(true)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    @Volatile private var lastActiveMs: Long = System.currentTimeMillis()
    @Volatile var autoLockTimeoutMs: Long = 60_000L
    @Volatile var lockOnBackground: Boolean = true

    fun unlock() {
        _locked.value = false
        touch()
    }

    fun lock() {
        _locked.value = true
    }

    fun touch() {
        lastActiveMs = System.currentTimeMillis()
    }

    fun onBackground() {
        if (lockOnBackground) lock()
    }

    fun onForeground() {
        if (!_locked.value && System.currentTimeMillis() - lastActiveMs > autoLockTimeoutMs) lock()
    }

    fun isLocked(): Boolean = _locked.value
}
