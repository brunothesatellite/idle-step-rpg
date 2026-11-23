package com.brunothesatellite.idlesteprpg.steps

import android.content.Context
import com.brunothesatellite.idlesteprpg.R
import com.brunothesatellite.idlesteprpg.data.GameRepository
import com.brunothesatellite.idlesteprpg.data.SettingsStore
import com.brunothesatellite.idlesteprpg.garmin.GarminFetch
import com.brunothesatellite.idlesteprpg.garmin.GarminRepository
import com.brunothesatellite.idlesteprpg.garmin.SyncResult
import com.brunothesatellite.idlesteprpg.ui.AppLocale
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class StepSourceType(val id: String, val label: String) {
    GARMIN("garmin", "Garmin"),
    HEALTH("health", "Santé Connect");

    companion object {
        fun fromId(id: String): StepSourceType =
            entries.firstOrNull { it.id == id } ?: GARMIN
    }
}

/** Pas téléchargés, pas encore appliqués à la partie. */
data class StepFetch(
    val ledger: Map<String, Int>,
    val today: LocalDate,
    val source: String
)

/**
 * Point d'entrée unique de la synchronisation des pas. Le jeu ne connaît que
 * [StepFetch] et `ingestHistory`, il ignore si les pas viennent de Garmin ou de
 * Santé Connect.
 */
class StepRepository(
    private val context: Context,
    private val settings: SettingsStore,
    private val garmin: GarminRepository,
    private val health: HealthConnectSource,
    private val game: GameRepository
) {
    companion object {
        const val GARMIN = "garmin"
        const val HEALTH = "health"
        private const val HISTORY_DAYS = 7
    }

    private val prefs = context.getSharedPreferences("step_sync", Context.MODE_PRIVATE)

    fun current(): StepSourceType = StepSourceType.fromId(settings.stepSource)

    fun healthAvailable(): Boolean = health.isAvailable()

    fun healthPermission(): Boolean = health.hasPermission()

    fun healthPermissionRequest(): String = health.permission

    fun hasKeyGarmin(): Boolean = garmin.hasKey()

    /** La source active est-elle utilisable (clé Garmin ou permission HC) ? */
    fun canSync(): Boolean = when (current()) {
        StepSourceType.GARMIN -> garmin.hasKey()
        StepSourceType.HEALTH -> health.hasPermission()
    }

    fun statusLine(): String = when (current()) {
        StepSourceType.GARMIN ->
            if (garmin.hasKey()) context.getString(R.string.status_garmin_connected)
            else context.getString(R.string.overlay_garmin_no_key)
        StepSourceType.HEALTH -> when {
            !health.isAvailable() -> context.getString(R.string.status_health_unavailable)
            !health.hasPermission() -> context.getString(R.string.status_health_permission)
            else -> context.getString(R.string.status_health_ok)
        }
    }

    fun changeSource(type: StepSourceType) {
        if (type.id == current().id) return
        settings.stepSource = type.id
        // Le premier import de la nouvelle source sert de base : il ne crédite
        // pas les pas déjà comptés par l'ancienne. Inutile sur une partie neuve.
        val state = game.state.value
        settings.pendingSourceRebase = state.stepsToday > 0 || state.totalSteps > 0
        prefs.edit().remove(keyLastSync(type.id)).apply()
    }

    fun lastSync(): Long = when (current()) {
        StepSourceType.GARMIN -> garmin.lastSync()
        StepSourceType.HEALTH -> prefs.getLong(keyLastSync(HEALTH), 0L)
    }

    fun lastError(): String = when (current()) {
        StepSourceType.GARMIN -> garmin.lastError()
        StepSourceType.HEALTH -> prefs.getString(keyError(HEALTH), "").orEmpty()
    }

    fun syncLog(): List<String> = when (current()) {
        StepSourceType.GARMIN -> garmin.syncLog()
        StepSourceType.HEALTH -> prefs.getString(keyLog(HEALTH), "").orEmpty()
            .lines().filter { it.isNotBlank() }
    }

    fun keyExpiry(): LocalDate? = garmin.keyExpiry()

    fun tokenExpiry() = garmin.tokenExpiry()

    suspend fun saveGarminKey(rawKey: String, expiry: LocalDate): Result<Unit> =
        garmin.saveKey(rawKey, expiry)

    fun revokeGarmin() = garmin.revoke()

    fun setGarminKeyExpiry(date: LocalDate) = garmin.setKeyExpiry(date)

    fun checkKeyExpiryNotice() {
        if (current() == StepSourceType.GARMIN) garmin.checkKeyExpiryNotice()
    }

    suspend fun fetch(): StepFetch? = when (current()) {
        StepSourceType.GARMIN ->
            garmin.fetchSteps()?.let { StepFetch(it.ledger, it.today, GARMIN) }
        StepSourceType.HEALTH -> health.fetch(HISTORY_DAYS)
    }

    /** Applique des pas téléchargés (reprise, ou import tardif après un résumé). */
    suspend fun apply(fetch: StepFetch, publishReport: Boolean = true): SyncResult {
        // Changement de source : on recale la base sans recréditer le jour.
        if (settings.pendingSourceRebase) {
            game.rebaseHistory(fetch.ledger, fetch.today)
            settings.pendingSourceRebase = false
            record(fetch.source, added = 0, error = "")
            return SyncResult(
                ok = true,
                message = context.getString(R.string.sync_source_changed),
                total = game.state.value.stepsToday,
                added = 0
            )
        }
        if (fetch.source == GARMIN) {
            return garmin.applySteps(GarminFetch(fetch.ledger, fetch.today), publishReport)
        }
        val report = game.ingestHistory(
            fetch.ledger,
            fetch.today,
            simulate = false,
            sourceLabel = StepSourceType.fromId(fetch.source).label,
            publishReport = publishReport
        )
        val state = game.state.value
        val total = fetch.ledger[fetch.today.toString()] ?: state.stepsToday
        val added = state.lastAdded
        record(HEALTH, added, "")
        val message = when {
            report != null -> context.getString(R.string.sync_health_catchup, report.days, report.steps)
            added > 0 -> context.getString(R.string.sync_health_synced, added)
            fetch.ledger.isEmpty() -> context.getString(R.string.sync_health_empty)
            else -> context.getString(R.string.sync_health_none_new)
        }
        return SyncResult(true, message, total = total, added = added)
    }

    suspend fun sync(): SyncResult = when (current()) {
        StepSourceType.GARMIN -> {
            if (settings.pendingSourceRebase) {
                // Changement de source : on passe par apply() pour recaler la base.
                val fetch = garmin.fetchSteps()
                if (fetch == null) {
                    SyncResult(false, context.getString(R.string.sync_garmin_download_fail), needsKey = !garmin.hasKey())
                } else {
                    apply(StepFetch(fetch.ledger, fetch.today, GARMIN))
                }
            } else {
                garmin.sync(simulateAbsence = false)
            }
        }
        StepSourceType.HEALTH -> {
            val fetch = health.fetch(HISTORY_DAYS)
            if (fetch == null) {
                val reason = health.lastFailure().takeIf { it.isNotBlank() }
                val message = when {
                    !health.isAvailable() -> context.getString(R.string.sync_health_unavailable)
                    !health.hasPermission() -> context.getString(R.string.sync_health_denied)
                    else -> context.getString(R.string.sync_health_read_fail) +
                        (reason?.let { " ($it)" } ?: "")
                }
                record(HEALTH, 0, message)
                SyncResult(false, message, needsKey = !health.hasPermission())
            } else {
                apply(fetch)
            }
        }
    }

    private fun record(source: String, added: Int, error: String) {
        val now = System.currentTimeMillis()
        val pattern = if (AppLocale.current.language == "fr") "dd/MM HH:mm" else "MM/dd h:mm a"
        val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern(pattern, AppLocale.current))

        val line = if (error.isBlank()) {
            context.getString(R.string.sync_log_ok, stamp, added)
        } else {
            context.getString(R.string.sync_log_error, stamp, error)
        }
        val existing = prefs.getString(keyLog(source), "").orEmpty().lines()
            .filter { it.isNotBlank() }
        val log = (listOf(line) + existing).take(20).joinToString("\n")
        prefs.edit()
            .putLong(keyLastSync(source), now)
            .putString(keyError(source), error)
            .putString(keyLog(source), log)
            .apply()
    }

    private fun keyLastSync(source: String) = "last_sync_$source"
    private fun keyError(source: String) = "last_error_$source"
    private fun keyLog(source: String) = "log_$source"
}
