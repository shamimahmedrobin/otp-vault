package com.otpvault.app.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.otpvault.app.domain.repository.OtpRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Periodically hard-deletes expired OTP rows. Also runs on app startup
 * (see HomeViewModel init) so correctness never depends only on this worker.
 * Survives device restarts because WorkManager persists the schedule.
 */
@HiltWorker
class ExpiryCleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val otpRepository: OtpRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            otpRepository.cleanupExpired()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_NAME = "expiry_cleanup"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ExpiryCleanupWorker>(15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }
    }
}
