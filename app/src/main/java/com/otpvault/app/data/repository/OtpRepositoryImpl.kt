package com.otpvault.app.data.repository

import com.otpvault.app.core.crypto.CryptoManager
import com.otpvault.app.core.crypto.EncryptedData
import com.otpvault.app.data.local.dao.OtpDao
import com.otpvault.app.data.local.dao.ServiceDao
import com.otpvault.app.data.local.entity.OTPEntry
import com.otpvault.app.domain.model.OtpItem
import com.otpvault.app.domain.model.Service
import com.otpvault.app.domain.repository.OtpRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

class OtpRepositoryImpl(
    private val otpDao: OtpDao,
    private val serviceDao: ServiceDao,
    private val crypto: CryptoManager
) : OtpRepository {

    override fun observeAll(): Flow<List<OtpItem>> =
        combine(otpDao.observeAll(), serviceDao.observeAll()) { entries, services ->
            val byId = services.associateBy { it.id }
            entries.mapNotNull { e -> byId[e.serviceId]?.let { e.toDomain(it.toDomain()) } }
        }

    override fun observeById(id: String): Flow<OtpItem?> =
        otpDao.observeById(id).map { e ->
            e?.let { entry -> serviceDao.getById(entry.serviceId)?.let { entry.toDomain(it.toDomain()) } }
        }

    override fun observeActiveCount(): Flow<Int> = otpDao.observeActiveCount(System.currentTimeMillis())

    override fun observeExpiringSoonCount(): Flow<Int> {
        val now = System.currentTimeMillis()
        return otpDao.observeExpiringSoonCount(now, now + 60_000L)
    }

    override suspend fun add(
        serviceId: String, phone: String?, code: String, note: String?, expiresAt: Long
    ): OtpItem {
        val now = System.currentTimeMillis()
        val otpEnc = crypto.encrypt(code.trim())
        val phoneEnc = phone?.takeIf { it.isNotBlank() }?.let { crypto.encrypt(it.trim()) }
        val noteEnc = note?.takeIf { it.isNotBlank() }?.let { crypto.encrypt(it.trim()) }
        val entry = OTPEntry(
            id = UUID.randomUUID().toString(),
            serviceId = serviceId,
            phoneEncrypted = phoneEnc?.cipherText, phoneIv = phoneEnc?.iv,
            otpEncrypted = otpEnc.cipherText, otpIv = otpEnc.iv,
            noteEncrypted = noteEnc?.cipherText, noteIv = noteEnc?.iv,
            createdAt = now, expiresAt = expiresAt, isUsed = false, usedAt = null
        )
        otpDao.insert(entry)
        val service = serviceDao.getById(serviceId)?.toDomain()
            ?: Service(serviceId, serviceId, "Other", "apps", false, now)
        return entry.toDomain(service)
    }

    override suspend fun update(item: OtpItem, newCode: String, newPhone: String?, newNote: String?, newExpiresAt: Long) {
        val existing = otpDao.getById(item.id) ?: return
        val otpEnc = crypto.encrypt(newCode.trim())
        val phoneEnc = newPhone?.takeIf { it.isNotBlank() }?.let { crypto.encrypt(it.trim()) }
        val noteEnc = newNote?.takeIf { it.isNotBlank() }?.let { crypto.encrypt(it.trim()) }
        otpDao.update(
            existing.copy(
                phoneEncrypted = phoneEnc?.cipherText, phoneIv = phoneEnc?.iv,
                otpEncrypted = otpEnc.cipherText, otpIv = otpEnc.iv,
                noteEncrypted = noteEnc?.cipherText, noteIv = noteEnc?.iv,
                expiresAt = newExpiresAt
            )
        )
    }

    override suspend fun markUsed(id: String) {
        val e = otpDao.getById(id) ?: return
        otpDao.update(e.copy(isUsed = true, usedAt = System.currentTimeMillis()))
    }

    override suspend fun delete(id: String) = otpDao.deleteById(id)
    override suspend fun deleteAll() = otpDao.deleteAll()
    override suspend fun cleanupExpired(): Int = otpDao.deleteExpired(System.currentTimeMillis())

    override suspend fun hasDuplicate(serviceId: String, code: String): Boolean {
        val now = System.currentTimeMillis()
        return otpDao.getAllOnce().any { e ->
            e.serviceId == serviceId && e.expiresAt > now &&
                crypto.decrypt(EncryptedData(e.otpEncrypted, e.otpIv)) == code.trim()
        }
    }

    private fun OTPEntry.toDomain(service: Service): OtpItem = OtpItem(
        id = id,
        service = service,
        phone = phoneEncrypted?.let { c -> phoneIv?.let { crypto.decrypt(EncryptedData(c, it)) } },
        code = crypto.decrypt(EncryptedData(otpEncrypted, otpIv)) ?: "",
        note = noteEncrypted?.let { c -> noteIv?.let { crypto.decrypt(EncryptedData(c, it)) } },
        createdAt = createdAt, expiresAt = expiresAt, isUsed = isUsed, usedAt = usedAt
    )
}

fun com.otpvault.app.data.local.entity.ServiceEntity.toDomain(): Service =
    Service(id, name, category, icon, isCustom, createdAt)
