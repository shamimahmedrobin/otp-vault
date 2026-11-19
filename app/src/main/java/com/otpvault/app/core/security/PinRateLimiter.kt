package com.otpvault.app.core.security

/**
 * Exponential-style lockout for PIN attempts:
 * 5 free failures, then 30s, doubling up to 5 minutes.
 * Pure logic with an injectable clock so it is unit-testable.
 */
class PinRateLimiter(private val clock: () -> Long = { System.currentTimeMillis() }) {
    private var failedAttempts = 0
    private var lockedUntilMs = 0L

    sealed interface Result {
        data object Allowed : Result
        data class Locked(val remainingSeconds: Int) : Result
    }

    fun check(): Result {
        val remaining = lockedUntilMs - clock()
        return if (remaining > 0) Result.Locked(((remaining + 999) / 1000).toInt()) else Result.Allowed
    }

    fun onFailure() {
        failedAttempts++
        if (failedAttempts >= FREE_ATTEMPTS) {
            val over = failedAttempts - FREE_ATTEMPTS
            val lockSeconds = minOf(BASE_LOCK_SECONDS shl over, MAX_LOCK_SECONDS)
            lockedUntilMs = clock() + lockSeconds * 1000L
        }
    }

    fun onSuccess() {
        failedAttempts = 0
        lockedUntilMs = 0L
    }

    fun failedAttempts(): Int = failedAttempts

    companion object {
        const val FREE_ATTEMPTS = 5
        const val BASE_LOCK_SECONDS = 30
        const val MAX_LOCK_SECONDS = 300
    }
}
