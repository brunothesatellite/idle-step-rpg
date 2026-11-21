package com.brunothesatellite.idlesteprpg.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.brunothesatellite.idlesteprpg.App
import com.brunothesatellite.idlesteprpg.notify.GameNotifications
import com.brunothesatellite.idlesteprpg.steps.StepSourceType
import java.util.concurrent.TimeUnit

class GarminSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? App ?: return Result.failure()
        val steps = app.container.stepRepository
        // Santé Connect refuse la lecture en arrière-plan sans la permission
        // READ_HEALTH_DATA_IN_BACKGROUND : le worker périodique ne doit pas
        // tenter la lecture (elle échouait et polluait le journal). La synchro
        // Santé Connect se fait au retour au premier plan.
        if (steps.current() != StepSourceType.GARMIN) return Result.success()
        if (!steps.canSync()) return Result.success()
        val result = steps.sync()
        if (result.needsKey) {
            GameNotifications.keyExpiry(app, result.message)
        } else if (result.added > 0 || !result.ok) {
            GameNotifications.syncDone(app, result.message, result.ok)
        }
        return when {
            result.needsKey -> Result.success()
            result.ok -> Result.success()
            else -> Result.retry()
        }
    }
}

object SyncScheduler {
    private const val WORK_NAME = "garmin-periodic-sync"

    fun schedule(context: Context, requireNetwork: Boolean = true) {
        val request = PeriodicWorkRequestBuilder<GarminSyncWorker>(60, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(
                        if (requireNetwork) NetworkType.CONNECTED else NetworkType.NOT_REQUIRED
                    )
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
