package com.brunothesatellite.idlesteprpg.garmin

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate
import java.util.Base64
import java.util.concurrent.TimeUnit

@Serializable
private data class GarminStepsDto(
    val calendarDate: String = "",
    val totalSteps: Int = 0,
    val totalDistance: Double = 0.0
)

data class GarminToken(val value: String, val expiresAtEpochSeconds: Long)

data class GarminSteps(val date: String, val steps: Int)

class GarminException(
    val code: Int,
    override val message: String,
    val retryAfterSeconds: Long = 0L
) : Exception(message)

class GarminClient(
    private val baseUrl: String = HOST,
    private val client: OkHttpClient = defaultClient()
) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun obtainToken(apiKey: String): GarminToken = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl$TOKEN_PATH")
            .header("Authorization", "ApiKey ${apiKey.trim()}")
            .header("Accept", "*/*")
            .post(ByteArray(0).toRequestBody(null))
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw GarminException(
                    response.code,
                    friendlyError(response.code, body),
                    response.header("Retry-After")?.toLongOrNull() ?: 0L
                )
            }
            val token = body.trim()
            if (token.isEmpty()) {
                throw GarminException(0, "Garmin n'a pas renvoyé de jeton.")
            }
            GarminToken(token, jwtExpiry(token))
        }
    }

    suspend fun steps(
        token: String,
        start: LocalDate,
        end: LocalDate
    ): List<GarminSteps> = withContext(Dispatchers.IO) {
        val url = "$baseUrl$STEPS_PATH/${start}/${end}"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("Accept", "*/*")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code == 204) return@use emptyList()
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw GarminException(
                    response.code,
                    friendlyError(response.code, body),
                    response.header("Retry-After")?.toLongOrNull() ?: 0L
                )
            }
            if (body.isBlank()) return@use emptyList()
            runCatching {
                json.decodeFromString<List<GarminStepsDto>>(body)
                    .map { GarminSteps(it.calendarDate, it.totalSteps) }
            }.getOrElse {
                throw GarminException(0, "Réponse Garmin illisible.")
            }
        }
    }

    private fun jwtExpiry(token: String): Long {
        val fallback = System.currentTimeMillis() / 1000L + 24 * 3600
        return runCatching {
            val payload = token.split(".").getOrNull(1) ?: return@runCatching fallback
            val padded = payload + "=".repeat((4 - payload.length % 4) % 4)
            val decoded = Base64.getUrlDecoder().decode(padded)
            val exp = JSONObject(String(decoded, Charsets.UTF_8)).optLong("exp", fallback)
            if (exp > 0) exp else fallback
        }.getOrDefault(fallback)
    }

    private fun friendlyError(code: Int, body: String): String = when (code) {
        401 -> "Clé Garmin invalide, expirée ou révoquée."
        403 -> "Accès Garmin refusé."
        404 -> "Ressource Garmin introuvable."
        429 -> "Limite Garmin atteinte. Réessayez dans un instant."
        in 500..599 -> "Garmin est momentanément indisponible."
        else -> "Erreur Garmin ($code)." + if (body.isBlank()) "" else " ${body.take(120)}"
    }

    companion object {
        private const val HOST = "https://personal.connectapi.garmin.com"
        private const val TOKEN_PATH =
            "/personalapitoken-service/personal-api/personal-api-key/token"
        private const val STEPS_PATH = "/usersummary-service/stats/steps/daily"

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}
