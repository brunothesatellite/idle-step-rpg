package com.brunothesatellite.idlesteprpg.garmin

import android.content.Context
import com.brunothesatellite.idlesteprpg.data.GameRepository
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
    val needsKey: Boolean = false
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
        val stamp = SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE).format(Date())
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
            "Votre clé Garmin est expirée. Remplacez-la dans Compte Garmin."
        } else {
            "Votre clé Garmin expire dans $days jour(s)."
        }
        GameNotifications.keyExpiry(appContext, message)
    }

    fun defaultExpiry(): LocalDate = LocalDate.now().plusYears(1)

    suspend fun saveKey(rawKey: String, expiry: LocalDate): Result<Unit> = withContext(Dispatchers.IO) {
        val key = rawKey.trim()
        if (key.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Collez la clé personnelle Garmin."))
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

    suspend fun sync(): SyncResult = withContext(Dispatchers.IO) {
        val key = secrets.key()
        if (key == null) {
            return@withContext SyncResult(false, "Aucune clé Garmin enregistrée.", needsKey = true)
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
            val report = game.ingestHistory(ledger, today)
            val state = game.state.value
            val total = ledger[today.toString()] ?: state.stepsToday
            val added = state.lastAdded
            prefs.edit()
                .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
                .putString(KEY_LAST_ERROR, "")
                .apply()
            val message = when {
                report != null -> "Rattrapage Garmin · ${report.days} jour(s), ${report.steps} pas"
                else -> "Garmin synchronisé"
            }
            logSync("OK · +$added pas · total $total")
            SyncResult(true, message, total = total, added = added)
        } catch (e: GarminException) {
            logSync("ERREUR ${e.code} · ${e.message}")
            prefs.edit().putString(KEY_LAST_ERROR, e.message).apply()
            SyncResult(false, e.message, needsKey = e.code == 401)
        } catch (e: Exception) {
            val message = if (e is IOException) {
                "Réseau indisponible. Dernière synchronisation conservée."
            } else {
                "Synchronisation impossible : ${e.message ?: "erreur inconnue"}"
            }
            logSync("ERREUR · $message")
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
            .apply()
    }

    companion object {
        private const val KEY_EXPIRY = "key_expiry"
        private const val KEY_LAST_SYNC = "last_sync"
        private const val KEY_LAST_ERROR = "last_error"
        private const val KEY_NOTICES = "key_notices"
        private const val KEY_SYNC_LOG = "sync_log"
    }
}

