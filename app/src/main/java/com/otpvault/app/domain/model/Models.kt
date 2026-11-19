package com.otpvault.app.domain.model

data class Service(
    val id: String,
    val name: String,
    val category: String,
    val icon: String,
    val isCustom: Boolean,
    val createdAt: Long
)

/** Decrypted, UI-facing OTP model. Lives only in memory, never persisted. */
data class OtpItem(
    val id: String,
    val service: Service,
    val phone: String?,
    val code: String,
    val note: String?,
    val createdAt: Long,
    val expiresAt: Long,
    val isUsed: Boolean,
    val usedAt: Long?
) {
    fun isExpired(now: Long = System.currentTimeMillis()): Boolean = expiresAt <= now
    fun remainingMs(now: Long = System.currentTimeMillis()): Long = (expiresAt - now).coerceAtLeast(0)
    fun status(now: Long = System.currentTimeMillis()): OtpStatus = when {
        isExpired(now) -> OtpStatus.EXPIRED
        isUsed -> OtpStatus.USED
        else -> OtpStatus.ACTIVE
    }
}

enum class OtpStatus { ACTIVE, USED, EXPIRED }

enum class ExpiryOption(val minutes: Int) {
    FIVE(5), FIFTEEN(15), THIRTY(30), SIXTY(60), DAY(1440);

    fun toMillis(from: Long = System.currentTimeMillis()): Long = from + minutes * 60_000L
    companion object {
        fun fromMinutes(m: Int): ExpiryOption = entries.firstOrNull { it.minutes == m } ?: FIVE
    }
}
