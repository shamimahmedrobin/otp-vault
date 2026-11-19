package com.otpvault.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.otpvault.app.data.local.dao.OtpDao
import com.otpvault.app.data.local.dao.PreferenceDao
import com.otpvault.app.data.local.dao.ServiceDao
import com.otpvault.app.data.local.entity.AppPreference
import com.otpvault.app.data.local.entity.OTPEntry
import com.otpvault.app.data.local.entity.ServiceEntity

@Database(
    entities = [OTPEntry::class, ServiceEntity::class, AppPreference::class],
    version = 1,
    exportSchema = true
)
abstract class OtpVaultDatabase : RoomDatabase() {
    abstract fun otpDao(): OtpDao
    abstract fun serviceDao(): ServiceDao
    abstract fun preferenceDao(): PreferenceDao

    companion object {
        const val NAME = "otp_vault.db"

        /** Example future migration, kept wired so upgrades never wipe user data. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE otp_entries ADD COLUMN noteIv BLOB")
            }
        }
    }
}
