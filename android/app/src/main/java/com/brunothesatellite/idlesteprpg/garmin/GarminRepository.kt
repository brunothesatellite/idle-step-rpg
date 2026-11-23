package com.brunothesatellite.idlesteprpg.garmin

import android.content.Context
import com.brunothesatellite.idlesteprpg.R
import com.brunothesatellite.idlesteprpg.data.GameRepository
import com.brunothesatellite.idlesteprpg.ui.AppLocale
import com.brunothesatellite.idlesteprpg.engine.HISTORY_DAYS
import com.brunothesatellite.idlesteprpg.notify.GameNotifications
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

data class SyncResult(
    val ok: Boolean,
    val message: String,
    val total: Int = 0,
    val added: Int = 0,
    val needsKey: Boolean = false,
    val retryAt: Long = 0L
)

/** Pas téléchargés depuis Garmin, pas encore appliqués à la partie. */
data class GarminFetch(
    val ledger: Map<String, Int>,
    val today: LocalDate
)

class GarminRepository(
    context: Context,
    private val secrets: SecretStore,
    private val client: GarminClient,
    private val game: GameRepository
) {
    private val prefs = context.getSharedPreferences("garmin_settings", Context.MODE_PRIVATE)
    private val appContext = context.applicationContext

    fun hasKey(): Boolean = secrets.hasKey()

    fun keyExpiry(): LocalDate? {
        val raw = prefs.getString(KEY_EXPIRY, null) ?: return null
        return runCatching { LocalDate.parse(raw) }.getOrNull()
    }

    fun setKeyExpiry(date: LocalDate) {
        prefs.edit()
            .putString(KEY_EXPIRY, date.toString())
            .remove(KEY_NOTICES)
            .apply()
    }

    fun tokenExpiry(): Instant? {
        val epoch = secrets.tokenExpiresAt()
        return if (epoch > 0) Instant.ofEpochSecond(epoch) else null
    }

    fun lastSync(): Long = prefs.getLong(KEY_LAST_SYNC, 0L)

    fun lastError(): String = prefs.getString(KEY_LAST_ERROR, "").orEmpty()

    fun syncLog(): List<String> = prefs.getString(KEY_SYNC_LOG, "").orEmpty()
        .split("\n")
        .filter { it.isNotBlank() }
        .take(20)

    private fun logSync(text: String) {
        val pattern = if (AppLocale.current.language == "fr") "dd/MM HH:mm" else "MM/dd h:mm a"
        val stamp = SimpleDateFormat(pattern, AppLocale.current).format(Date())
        val updated = (listOf("$stamp · $text") + syncLog()).take(20)
        prefs.edit().putString(KEY_SYNC_LOG, updated.joinToString("\n")).apply()
    }

    fun checkKeyExpiryNotice() {
        val expiry = keyExpiry() ?: return
        val days = ChronoUnit.DAYS.between(LocalDate.now(), expiry)
        val threshold = when {
            days < 0 -> 0
            days <= 1 -> 1
            days <= 7 -> 7
            days <= 30 -> 30
            else -> return
        }
        val notices = prefs.getStringSet(KEY_NOTICES, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (!notices.add(threshold.toString())) return
        prefs.edit().putStringSet(KEY_NOTICES, notices).apply()
        val message = if (threshold == 0) {
            appContext.getString(R.string.msg_garmin_key_expired)
        } else {
            appContext.getString(R.string.msg_garmin_key_expires, days)
        }
        GameNotifications.keyExpiry(appContext, message)
    }

    fun defaultExpiry(): LocalDate = LocalDate.now().plusYears(1)

    suspend fun saveKey(rawKey: String, expiry: LocalDate): Result<Unit> = withContext(Dispatchers.IO) {
        val key = rawKey.trim()
        if (key.isEmpty()) {
            return@withContext Result.failure(
                IllegalArgumentException(appContext.getString(R.string.msg_garmin_paste_key))
            )
        }
        try {
            val token = client.obtainToken(key)
            secrets.saveKey(key)
            secrets.saveToken(token.value, token.expiresAtEpochSeconds)
            setKeyExpiry(expiry)
            prefs.edit()
                .putString(KEY_LAST_ERROR, "")
                .remove(KEY_NOTICES)
                .apply()
            Result.success(Unit)
        } catch (e: GarminException) {
            prefs.edit().putString(KEY_LAST_ERROR, e.message).apply()
            Result.failure(e)
        } catch (e: Exception) {
            val message = "Impossible de joindre Garmin : ${e.message}"
            prefs.edit().putString(KEY_LAST_ERROR, message).apply()
            Result.failure(IOException(message, e))
        }
    }

    suspend fun fetchSteps(): GarminFetch? = withContext(Dispatchers.IO) {
        runCatching {
            val key = secrets.key() ?: return@withContext null
            var token = secrets.token()
            val expiry = secrets.tokenExpiresAt()
            val now = System.currentTimeMillis() / 1000L
            if (token == null || expiry < now + 3600) {
                val fresh = client.obtainToken(key)
                token = fresh.value
                secrets.saveToken(fresh.value, fresh.expiresAtEpochSeconds)
            }
            val today = LocalDate.now()
            val start = today.minusDays((HISTORY_DAYS - 1).toLong())
            val access = token ?: return@withContext null
            val days = try {
                client.steps(access, start, today)
            } catch (e: GarminException) {
                if (e.code == 401) {
                    val fresh = client.obtainToken(key)
                    secrets.saveToken(fresh.value, fresh.expiresAtEpochSeconds)
                    client.steps(fresh.value, start, today)
                } else {
                    throw e
                }
            }
            GarminFetch(
                ledger = days.filter { it.date.isNotBlank() }.associate { it.date to it.steps },
                today = today
            )
        }.getOrNull()
    }

    /** Applique des pas déjà téléchargés (reprise, ou import tardif après un résumé). */
    suspend fun applySteps(fetch: GarminFetch, publishReport: Boolean = true): SyncResult =
        withContext(Dispatchers.IO) {
            val report = game.ingestHistory(
                fetch.ledger,
                fetch.today,
                simulate = false,
                publishReport = publishReport
            )
            val state = game.state.value
            val total = fetch.ledger[fetch.today.toString()] ?: state.stepsToday
            val added = state.lastAdded
            prefs.edit()
                .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
                .putString(KEY_LAST_ERROR, "")
                .remove(KEY_RETRY_AT)
                .apply()
            val message = when {
                report != null -> appContext.getString(R.string.msg_garmin_catchup, report.days, report.steps)
                added > 0 -> appContext.getString(R.string.msg_garmin_synced, added)
                else -> appContext.getString(R.string.msg_garmin_none_new)
            }
            logSync(appContext.getString(R.string.sync_log_garmin_ok, added, total))
            SyncResult(true, message, total = total, added = added)
        }

    suspend fun sync(simulateAbsence: Boolean = true): SyncResult = withContext(Dispatchers.IO) {
        val key = secrets.key()
        if (key == null) {
            return@withContext SyncResult(false, appContext.getString(R.string.msg_garmin_no_key), needsKey = true)
        }
        val retryAt = prefs.getLong(KEY_RETRY_AT, 0L)
        if (retryAt > System.currentTimeMillis()) {
            return@withContext SyncResult(
                false,
                appContext.getString(
                    R.string.msg_garmin_rate,
                    SimpleDateFormat(
                        if (AppLocale.current.language == "fr") "HH:mm" else "h:mm a",
                        AppLocale.current
                    ).format(Date(retryAt))
                ),
                retryAt = retryAt
            )
        }
        try {
            var token = secrets.token()
            var expiry = secrets.tokenExpiresAt()
            val now = System.currentTimeMillis() / 1000L
            if (token == null || expiry < now + 3600) {
                val fresh = client.obtainToken(key)
                token = fresh.value
                expiry = fresh.expiresAtEpochSeconds
                secrets.saveToken(fresh.value, fresh.expiresAtEpochSeconds)
            }

            val today = LocalDate.now()
            val start = today.minusDays((HISTORY_DAYS - 1).toLong())
            val accessToken = token ?: throw IllegalStateException("Jeton Garmin absent.")
            val days = try {
                client.steps(accessToken, start, today)
            } catch (e: GarminException) {
                if (e.code == 401) {
                    val fresh = client.obtainToken(key)
                    secrets.saveToken(fresh.value, fresh.expiresAtEpochSeconds)
                    client.steps(fresh.value, start, today)
                } else {
                    throw e
                }
            }

            val ledger = days
                .filter { it.date.isNotBlank() }
                .associate { it.date to it.steps }
            val report = game.ingestHistory(ledger, today, simulate = simulateAbsence)
            val state = game.state.value
            val total = ledger[today.toString()] ?: state.stepsToday
            val added = state.lastAdded
            prefs.edit()
                .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
                .putString(KEY_LAST_ERROR, "")
                .remove(KEY_RETRY_AT)
                .apply()
            val message = when {
                report != null -> appContext.getString(R.string.msg_garmin_catchup, report.days, report.steps)
                added > 0 -> appContext.getString(R.string.msg_garmin_synced, added)
                else -> appContext.getString(R.string.msg_garmin_none_new)
            }
            logSync(appContext.getString(R.string.sync_log_garmin_ok, added, total))
            SyncResult(true, message, total = total, added = added)
        } catch (e: GarminException) {
            val retry = if (e.code == 429) {
                val delay = (e.retryAfterSeconds.takeIf { it > 0 } ?: 900L) * 1000L
                val at = System.currentTimeMillis() + delay
                prefs.edit().putLong(KEY_RETRY_AT, at).apply()
                at
            } else {
                0L
            }
            val message = if (e.code == 429) {
                appContext.getString(
                    R.string.msg_garmin_rate,
                    SimpleDateFormat(
                        if (AppLocale.current.language == "fr") "HH:mm" else "h:mm a",
                        AppLocale.current
                    ).format(Date(retry))
                )
            } else {
                e.message
            }
            logSync(appContext.getString(R.string.sync_log_garmin_error, e.code, message))
            prefs.edit().putString(KEY_LAST_ERROR, message).apply()
            SyncResult(false, message, needsKey = e.code == 401, retryAt = retry)
        } catch (e: Exception) {
            val message = if (e is IOException) {
                appContext.getString(R.string.msg_garmin_network)
            } else {
                appContext.getString(
                    R.string.msg_sync_impossible,
                    e.message ?: appContext.getString(R.string.msg_unknown_error)
                )
            }
            logSync(appContext.getString(R.string.sync_log_garmin_error_generic, message))
            prefs.edit().putString(KEY_LAST_ERROR, message).apply()
            SyncResult(false, message)
        }
    }

    fun revoke() {
        secrets.clearAll()
        prefs.edit()
            .remove(KEY_EXPIRY)
            .remove(KEY_LAST_SYNC)
            .remove(KEY_LAST_ERROR)
            .remove(KEY_NOTICES)
            .remove(KEY_RETRY_AT)
            .apply()
    }

    companion object {
        private const val KEY_EXPIRY = "key_expiry"
        private const val KEY_LAST_SYNC = "last_sync"
        private const val KEY_LAST_ERROR = "last_error"
        private const val KEY_NOTICES = "key_notices"
        private const val KEY_SYNC_LOG = "sync_log"
        private const val KEY_RETRY_AT = "retry_at"
    }
}


