package com.otpvault.app

import com.otpvault.app.data.local.entity.OTPEntry
import com.otpvault.app.data.repository.ServiceRepositoryImpl
import com.otpvault.app.domain.model.ExpiryOption
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpiryAndServiceTest {

    private fun entry(id: String, expiresAt: Long, used: Boolean = false) = OTPEntry(
        id = id, serviceId = "bkash",
        phoneEncrypted = null, phoneIv = null,
        otpEncrypted = byteArrayOf(1), otpIv = byteArrayOf(2),
        noteEncrypted = null, noteIv = null,
        createdAt = 0, expiresAt = expiresAt, isUsed = used, usedAt = null
    )

    @Test
    fun `expired entries are deleted and used entries can be purged`() = runBlocking {
        val dao = FakeDaos.otpDao()
        val now = System.currentTimeMillis()
        dao.insert(entry("a", now - 1_000))
        dao.insert(entry("b", now + 60_000))
        dao.insert(entry("c", now + 60_000, used = true))
        assertEquals(1, dao.deleteExpired(now))
        assertEquals(listOf("b", "c"), dao.getAllOnce().map { it.id }.sorted())
        assertEquals(1, dao.deleteUsed())
        assertEquals(listOf("b"), dao.getAllOnce().map { it.id })
    }

    @Test
    fun `expiry options map to minutes`() {
        assertEquals(5, ExpiryOption.FIVE.minutes)
        assertEquals(1440, ExpiryOption.DAY.minutes)
        assertEquals(ExpiryOption.FIVE, ExpiryOption.fromMinutes(999))
    }

    @Test
    fun `custom service lifecycle`() = runBlocking {
        val repo = ServiceRepositoryImpl(FakeDaos.serviceDao())
        repo.seedDefaults()
        val created = repo.addCustom("My Bank OTP", "Bank", "account_balance")
        assertTrue(created.isCustom)
        val all = repo.observeAll().first()
        assertTrue(all.any { it.id == created.id })
        repo.deleteCustom(created.id)
        assertTrue(repo.observeAll().first().none { it.id == created.id })
    }
}
