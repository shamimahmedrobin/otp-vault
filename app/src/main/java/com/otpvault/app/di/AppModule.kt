package com.otpvault.app.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.otpvault.app.core.crypto.CryptoManager
import com.otpvault.app.core.crypto.EncryptedData
import com.otpvault.app.core.database.OtpVaultDatabase
import com.otpvault.app.core.preferences.AppPreferences
import com.otpvault.app.core.security.AppLockManager
import com.otpvault.app.core.security.PinManager
import com.otpvault.app.core.security.PinRateLimiter
import com.otpvault.app.data.backup.BackupManager
import com.otpvault.app.data.local.dao.OtpDao
import com.otpvault.app.data.local.dao.ServiceDao
import com.otpvault.app.data.repository.OtpRepositoryImpl
import com.otpvault.app.data.repository.ServiceRepositoryImpl
import com.otpvault.app.domain.repository.OtpRepository
import com.otpvault.app.domain.repository.ServiceRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): OtpVaultDatabase =
        Room.databaseBuilder(context, OtpVaultDatabase::class.java, OtpVaultDatabase.NAME)
            .addMigrations(OtpVaultDatabase.MIGRATION_1_2)
            .build()

    @Provides fun provideOtpDao(db: OtpVaultDatabase): OtpDao = db.otpDao()
    @Provides fun provideServiceDao(db: OtpVaultDatabase): ServiceDao = db.serviceDao()

    @Provides @Singleton fun provideCrypto(): CryptoManager = CryptoManager()
    @Provides @Singleton fun providePreferences(@ApplicationContext context: Context): AppPreferences = AppPreferences(context)
    @Provides @Singleton fun providePinManager(prefs: AppPreferences): PinManager = PinManager(prefs)
    @Provides @Singleton fun provideRateLimiter(): PinRateLimiter = PinRateLimiter()
    @Provides @Singleton fun provideLockManager(): AppLockManager = AppLockManager()

    @Provides @Singleton
    fun provideOtpRepository(otpDao: OtpDao, serviceDao: ServiceDao, crypto: CryptoManager): OtpRepository =
        OtpRepositoryImpl(otpDao, serviceDao, crypto)

    @Provides @Singleton
    fun provideServiceRepository(serviceDao: ServiceDao): ServiceRepository = ServiceRepositoryImpl(serviceDao)

    @Provides @Singleton
    fun provideBackupManager(otpDao: OtpDao, serviceDao: ServiceDao, crypto: CryptoManager): BackupManager =
        BackupManager(
            otpDao = otpDao,
            serviceDao = serviceDao,
            decryptLocal = { c, iv -> crypto.decrypt(EncryptedData(c, iv)) },
            encryptLocal = { plain -> val e = crypto.encrypt(plain); e.cipherText to e.iv }
        )

    @Provides fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)
}
