package com.brunothesatellite.idlesteprpg.steps

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.brunothesatellite.idlesteprpg.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

/**
 * Lecture des pas depuis Health Connect (« Santé Connect »).
 *
 * Health Connect n'est pas une source de pas en soi : c'est un hub alimenté par
 * Google Fit, Samsung Health, une montre… Sans fournisseur, la lecture renvoie
 * zéro pas. Les enregistrements peuvent venir de plusieurs origines en même
 * temps (téléphone + montre) : on agrège donc **par origine** et on retient la
 * plus élevée pour chaque jour, jamais la somme brute.
 */
class HealthConnectSource(private val context: Context) {

    enum class Availability { READY, UPDATE_REQUIRED, UNAVAILABLE }

    val permission: String = HealthPermission.getReadPermission(StepsRecord::class)

    fun availability(): Availability = when (HealthConnectClient.getSdkStatus(context)) {
        HealthConnectClient.SDK_AVAILABLE -> Availability.READY
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> Availability.UPDATE_REQUIRED
        else -> Availability.UNAVAILABLE
    }

    fun isAvailable(): Boolean = availability() == Availability.READY

    fun hasPermission(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /**
     * Télécharge les [days] derniers jours. Retourne null si la permission ou le
     * service manque ; une carte vide signifie « aucun fournisseur de pas ».
     * La cause exacte d'un échec est conservée dans [lastFailure] pour le journal.
     */
    suspend fun fetch(days: Int = 7): StepFetch? = withContext(Dispatchers.IO) {
        failure = ""
        if (!isAvailable()) {
            failure = context.getString(R.string.hc_failure_unavailable)
            return@withContext null
        }
        if (!hasPermission()) {
            failure = context.getString(R.string.hc_failure_permission)
            return@withContext null
        }
        try {
            val client = HealthConnectClient.getOrCreate(context)
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val firstDay = today.minusDays((days - 1).toLong())
            val windowStart = firstDay.atStartOfDay(zone).toInstant()
            val windowEnd = today.plusDays(1).atStartOfDay(zone).toInstant()

            // 1. Origines présentes sur la fenêtre (téléphone, montre, app tierce…).
            val origins = client.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(windowStart, windowEnd),
                    ascendingOrder = false,
                    pageSize = 400
                )
            ).records
                .map { it.metadata.dataOrigin.packageName }
                .distinct()

            // 2. Pour chaque jour, on garde le total de l'origine la plus complète.
            val ledger = LinkedHashMap<String, Int>()
            for (offset in 0 until days) {
                val day = firstDay.plusDays(offset.toLong())
                val start = day.atStartOfDay(zone).toInstant()
                val end = day.plusDays(1).atStartOfDay(zone).toInstant()
                var best = 0L
                origins.forEach { origin ->
                    val response = runCatching {
                        client.aggregate(
                            AggregateRequest(
                                metrics = setOf(COUNT_TOTAL),
                                timeRangeFilter = TimeRangeFilter.between(start, end),
                                dataOriginFilter = setOf(DataOrigin(origin))
                            )
                        )
                    }.getOrNull() ?: return@forEach
                    val total = response[COUNT_TOTAL] ?: 0L
                    if (total > best) best = total
                }
                if (best > 0L) ledger[day.toString()] = best.toInt()
            }
            StepFetch(ledger = ledger, today = today, source = StepRepository.HEALTH)
        } catch (e: Exception) {
            failure = e.javaClass.simpleName +
                (e.message?.let { message -> ": ${message.take(90)}" } ?: "")
            Log.w(TAG, context.getString(R.string.hc_log_read_fail), e)
            null
        }
    }

    /** Dernière cause d'échec de lecture (vide si la dernière lecture a réussi). */
    fun lastFailure(): String = failure

    private companion object {
        const val TAG = "HealthConnect"
        val COUNT_TOTAL: AggregateMetric<Long> = StepsRecord.COUNT_TOTAL
    }

    @Volatile
    private var failure: String = ""
}
