package com.otpvault.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.otpvault.app.data.local.entity.AppPreference
import com.otpvault.app.data.local.entity.OTPEntry
import com.otpvault.app.data.local.entity.ServiceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OtpDao {
    @Query("SELECT * FROM otp_entries ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<OTPEntry>>

    @Query("SELECT * FROM otp_entries WHERE expiresAt > :now ORDER BY createdAt DESC")
    fun observeActive(now: Long): Flow<List<OTPEntry>>

    @Query("SELECT * FROM otp_entries WHERE id = :id")
    fun observeById(id: String): Flow<OTPEntry?>

    @Query("SELECT * FROM otp_entries WHERE id = :id")
    suspend fun getById(id: String): OTPEntry?

    @Query("SELECT COUNT(*) FROM otp_entries WHERE expiresAt > :now AND isUsed = 0")
    fun observeActiveCount(now: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM otp_entries WHERE expiresAt > :now AND expiresAt <= :soon AND isUsed = 0")
    fun observeExpiringSoonCount(now: Long, soon: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: OTPEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<OTPEntry>)

    @Update
    suspend fun update(entry: OTPEntry)

    @Delete
    suspend fun delete(entry: OTPEntry)

    @Query("DELETE FROM otp_entries WHERE id = :id")
    suspend fun deleteById(id: String)

    /** Hard-deletes expired rows. Expired OTPs are never kept as history. */
    @Query("DELETE FROM otp_entries WHERE expiresAt <= :now")
    suspend fun deleteExpired(now: Long): Int

    @Query("DELETE FROM otp_entries WHERE isUsed = 1")
    suspend fun deleteUsed(): Int

    @Query("DELETE FROM otp_entries")
    suspend fun deleteAll()

    @Query("SELECT * FROM otp_entries")
    suspend fun getAllOnce(): List<OTPEntry>
}

@Dao
interface ServiceDao {
    @Query("SELECT * FROM services ORDER BY isCustom ASC, name ASC")
    fun observeAll(): Flow<List<ServiceEntity>>

    @Query("SELECT * FROM services WHERE id = :id")
    suspend fun getById(id: String): ServiceEntity?

    @Query("SELECT COUNT(*) FROM services")
    fun observeCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(service: ServiceEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(services: List<ServiceEntity>)

    @Update
    suspend fun update(service: ServiceEntity)

    @Query("DELETE FROM services WHERE id = :id AND isCustom = 1")
    suspend fun deleteCustomById(id: String)

    @Query("SELECT COUNT(*) FROM otp_entries WHERE serviceId = :serviceId")
    suspend fun countOtpsForService(serviceId: String): Int

    @Query("SELECT * FROM services")
    suspend fun getAllOnce(): List<ServiceEntity>
}

@Dao
interface PreferenceDao {
    @Query("SELECT * FROM app_preferences WHERE key = :key")
    suspend fun get(key: String): AppPreference?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(pref: AppPreference)

    @Query("DELETE FROM app_preferences")
    suspend fun clear()
}
