package com.otpvault.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.otpvault.app.core.database.OtpVaultDatabase
import com.otpvault.app.data.local.DefaultServices
import com.otpvault.app.data.local.entity.OTPEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseInstrumentedTest {
    private lateinit var db: OtpVaultDatabase

    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, OtpVaultDatabase::class.java).build()
    }

    @After fun teardown() = db.close()

    private fun entry(id: String, expiresAt: Long) = OTPEntry(
        id = id, serviceId = "bkash", phoneEncrypted = null, phoneIv = null,
        otpEncrypted = byteArrayOf(9), otpIv = byteArrayOf(8),
        noteEncrypted = null, noteIv = null,
        createdAt = 0, expiresAt = expiresAt
    )

    @Test fun crudAndExpiryCleanup() = runBlocking {
        db.serviceDao().insertAll(DefaultServices.all)
        val now = System.currentTimeMillis()
        db.otpDao().insert(entry("x1", now + 60_000))
        db.otpDao().insert(entry("x2", now - 5_000))
        assertEquals(2, db.otpDao().getAllOnce().size)
        assertEquals(1, db.otpDao().deleteExpired(now))
        val remaining = db.otpDao().observeAll().first()
        assertEquals(listOf("x1"), remaining.map { it.id })
        db.otpDao().deleteById("x1")
        assertTrue(db.otpDao().getAllOnce().isEmpty())
    }
}
