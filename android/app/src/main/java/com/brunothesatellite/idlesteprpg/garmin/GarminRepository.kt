package com.brunothesatellite.idlesteprpg.garmin

import android.content.Context
import com.brunothesatellite.idlesteprpg.data.GameRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.Instant
import java.time.LocalDate

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

    fun hasKey(): Boolean = secrets.hasKey()

    fun keyExpiry(): LocalDate? {
        val raw = prefs.getString(KEY_EXPIRY, null) ?: return null
        return runCatching { LocalDate.parse(raw) }.getOrNull()
    }

    fun setKeyExpiry(date: LocalDate) {
        prefs.edit().putString(KEY_EXPIRY, date.toString()).apply()
    }

    fun tokenExpiry(): Instant? {
        val epoch = secrets.tokenExpiresAt()
        return if (epoch > 0) Instant.ofEpochSecond(epoch) else null
    }

    fun lastSync(): Long = prefs.getLong(KEY_LAST_SYNC, 0L)

    fun lastError(): String = prefs.getString(KEY_LAST_ERROR, "").orEmpty()

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
            prefs.edit().putString(KEY_LAST_ERROR, "").apply()
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
            val accessToken = token ?: throw IllegalStateException("Jeton Garmin absent.")
            val days = try {
                client.steps(accessToken, today, today)
            } catch (e: GarminException) {
                if (e.code == 401) {
                    val fresh = client.obtainToken(key)
                    secrets.saveToken(fresh.value, fresh.expiresAtEpochSeconds)
                    client.steps(fresh.value, today, today)
                } else {
                    throw e
                }
            }

            val total = days.firstOrNull { it.date == today.toString() }?.steps ?: 0
            game.ingestSteps(total, today)
            val added = game.state.value.lastAdded
            prefs.edit()
                .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
                .putString(KEY_LAST_ERROR, "")
                .apply()
            SyncResult(true, "Garmin synchronisé", total = total, added = added)
        } catch (e: GarminException) {
            prefs.edit().putString(KEY_LAST_ERROR, e.message).apply()
            SyncResult(false, e.message, needsKey = e.code == 401)
        } catch (e: Exception) {
            val message = if (e is IOException) {
                "Réseau indisponible. Dernière synchronisation conservée."
            } else {
                "Synchronisation impossible : ${e.message ?: "erreur inconnue"}"
            }
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
            .apply()
    }

    companion object {
        private const val KEY_EXPIRY = "key_expiry"
        private const val KEY_LAST_SYNC = "last_sync"
        private const val KEY_LAST_ERROR = "last_error"
    }
}

