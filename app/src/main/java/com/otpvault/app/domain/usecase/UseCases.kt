package com.otpvault.app.domain.usecase

import com.otpvault.app.domain.model.ExpiryOption
import com.otpvault.app.domain.model.OtpItem
import com.otpvault.app.domain.repository.OtpRepository
import com.otpvault.app.domain.repository.ServiceRepository

class AddOtpUseCase(private val repo: OtpRepository) {
    suspend operator fun invoke(
        serviceId: String, phone: String?, code: String, note: String?,
        expiry: ExpiryOption, customMinutes: Int? = null
    ): OtpItem {
        val now = System.currentTimeMillis()
        val expiresAt = if (customMinutes != null) now + customMinutes * 60_000L else expiry.toMillis(now)
        return repo.add(serviceId, phone, code, note, expiresAt)
    }
}

class CleanupExpiredUseCase(private val repo: OtpRepository) {
    suspend operator fun invoke(): Int = repo.cleanupExpired()
}

class SeedServicesUseCase(private val repo: ServiceRepository) {
    suspend operator fun invoke() = repo.seedDefaults()
}
