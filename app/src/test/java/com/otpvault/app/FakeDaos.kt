package com.otpvault.app

import com.otpvault.app.data.local.dao.OtpDao
import com.otpvault.app.data.local.dao.ServiceDao
import com.otpvault.app.data.local.entity.OTPEntry
import com.otpvault.app.data.local.entity.ServiceEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Minimal in-memory fakes so pure-logic tests never need a device. */
object FakeDaos {
    fun otpDao(): OtpDao = object : OtpDao {
        private val rows = mutableListOf<OTPEntry>()
        override fun observeAll(): Flow<List<OTPEntry>> = flowOf(rows.toList())
        override fun observeActive(now: Long): Flow<List<OTPEntry>> = flowOf(rows.filter { it.expiresAt > now })
        override fun observeById(id: String): Flow<OTPEntry?> = flowOf(rows.firstOrNull { it.id == id })
        override suspend fun getById(id: String): OTPEntry? = rows.firstOrNull { it.id == id }
        override fun observeActiveCount(now: Long): Flow<Int> = flowOf(rows.count { it.expiresAt > now && !it.isUsed })
        override fun observeExpiringSoonCount(now: Long, soon: Long): Flow<Int> =
            flowOf(rows.count { it.expiresAt > now && it.expiresAt <= soon && !it.isUsed })
        override suspend fun insert(entry: OTPEntry) { rows.removeAll { it.id == entry.id }; rows.add(entry) }
        override suspend fun insertAll(entries: List<OTPEntry>) { entries.forEach { insert(it) } }
        override suspend fun update(entry: OTPEntry) { insert(entry) }
        override suspend fun delete(entry: OTPEntry) { rows.removeAll { it.id == entry.id } }
        override suspend fun deleteById(id: String) { rows.removeAll { it.id == id } }
        override suspend fun deleteExpired(now: Long): Int {
            val n = rows.count { it.expiresAt <= now }; rows.removeAll { it.expiresAt <= now }; return n
        }
        override suspend fun deleteUsed(): Int {
            val n = rows.count { it.isUsed }; rows.removeAll { it.isUsed }; return n
        }
        override suspend fun deleteAll() = rows.clear()
        override suspend fun getAllOnce(): List<OTPEntry> = rows.toList()
    }

    fun serviceDao(): ServiceDao = object : ServiceDao {
        private val rows = mutableListOf<ServiceEntity>()
        override fun observeAll(): Flow<List<ServiceEntity>> = flowOf(rows.toList())
        override suspend fun getById(id: String): ServiceEntity? = rows.firstOrNull { it.id == id }
        override fun observeCount(): Flow<Int> = flowOf(rows.size)
        override suspend fun insert(service: ServiceEntity) { rows.removeAll { it.id == service.id }; rows.add(service) }
        override suspend fun insertAll(services: List<ServiceEntity>) { services.forEach { if (rows.none { r -> r.id == it.id }) rows.add(it) } }
        override suspend fun update(service: ServiceEntity) { insert(service) }
        override suspend fun deleteCustomById(id: String) { rows.removeAll { it.id == id && it.isCustom } }
        override suspend fun countOtpsForService(serviceId: String): Int = 0
        override suspend fun getAllOnce(): List<ServiceEntity> = rows.toList()
    }
}
