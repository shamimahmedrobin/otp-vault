package com.otpvault.app.domain.repository

import com.otpvault.app.domain.model.OtpItem
import com.otpvault.app.domain.model.Service
import kotlinx.coroutines.flow.Flow

interface OtpRepository {
    fun observeAll(): Flow<List<OtpItem>>
    fun observeById(id: String): Flow<OtpItem?>
    fun observeActiveCount(): Flow<Int>
    fun observeExpiringSoonCount(): Flow<Int>
    suspend fun add(serviceId: String, phone: String?, code: String, note: String?, expiresAt: Long): OtpItem
    suspend fun update(item: OtpItem, newCode: String, newPhone: String?, newNote: String?, newExpiresAt: Long)
    suspend fun markUsed(id: String)
    suspend fun delete(id: String)
    suspend fun deleteAll()
    suspend fun cleanupExpired(): Int
    suspend fun hasDuplicate(serviceId: String, code: String): Boolean
}

interface ServiceRepository {
    fun observeAll(): Flow<List<Service>>
    fun observeCount(): Flow<Int>
    suspend fun seedDefaults()
    suspend fun addCustom(name: String, category: String, icon: String): Service
    suspend fun updateCustom(service: Service)
    /** Fails with IllegalStateException when OTP records still reference the service. */
    suspend fun deleteCustom(id: String)
}
