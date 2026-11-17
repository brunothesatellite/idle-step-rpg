package com.brunothesatellite.idlesteprpg.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.brunothesatellite.idlesteprpg.App
import java.util.concurrent.TimeUnit

class MaintenanceWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? App ?: return Result.failure()
        runCatching {
            app.container.gameRepository.maintenance()
            app.container.garminRepository.checkKeyExpiryNotice()
        }
        return Result.success()
    }
}

object MaintenanceScheduler {
    private const val WORK_NAME = "maintenance-coffres-cle"

    fun schedule(context: Context) {
        // Les coffres vivent 24 h : une passe toutes les 2 h suffit largement et
        // évite de réveiller le processus 96 fois par jour.
        val request = PeriodicWorkRequestBuilder<MaintenanceWorker>(120, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
