package com.brunothesatellite.idlesteprpg.update

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class UpdateInfo(
    val tag: String,
    val version: String,
    val releaseUrl: String,
    val apkUrl: String,
    val sha256: String?
)

object UpdateChecker {

    private const val API_URL =
        "https://api.github.com/repos/brunothesatellite/idle-step-rpg/releases/latest"

    fun isNewer(current: String, latest: String): Boolean {
        val a = versionParts(current)
        val b = versionParts(latest)
        val size = maxOf(a.size, b.size)
        for (i in 0 until size) {
            val left = a.getOrElse(i) { 0 }
            val right = b.getOrElse(i) { 0 }
            if (right != left) return right > left
        }
        return false
    }

    private fun versionParts(value: String): List<Int> =
        value.removePrefix("v")
            .substringBefore("-")
            .split(".")
            .map { it.toIntOrNull() ?: 0 }

    fun extractSha256(body: String): String? =
        Regex("SHA-256 APK\\s*:\\s*([A-Fa-f0-9]{64})")
            .find(body)
            ?.groupValues
            ?.getOrNull(1)
            ?.lowercase()

    fun parseLatest(json: String): UpdateInfo? = runCatching {
        val root = JSONObject(json)
        val tag = root.optString("tag_name").takeIf { it.isNotBlank() } ?: return null
        val releaseUrl = root.optString("html_url").takeIf { it.isNotBlank() } ?: return null
        val assets = root.optJSONArray("assets") ?: return null
        var apkUrl: String? = null
        for (i in 0 until assets.length()) {
            val asset = assets.optJSONObject(i) ?: continue
            val name = asset.optString("name")
            if (name.endsWith(".apk", ignoreCase = true)) {
                apkUrl = asset.optString("browser_download_url").takeIf { it.isNotBlank() }
                if (apkUrl != null) break
            }
        }
        val url = apkUrl ?: return null
        UpdateInfo(
            tag = tag,
            version = tag.removePrefix("v").substringBefore("-"),
            releaseUrl = releaseUrl,
            apkUrl = url,
            sha256 = extractSha256(root.optString("body"))
        )
    }.getOrNull()

    suspend fun fetchLatest(): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val connection = URL(API_URL).openConnection() as HttpURLConnection
            connection.connectTimeout = 12_000
            connection.readTimeout = 12_000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "LaMarcheDesAmes-Android")
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            parseLatest(body)
        }.getOrNull()
    }

    suspend fun downloadApk(
        context: Context,
        info: UpdateInfo,
        onProgress: (Float) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, "updates").apply { mkdirs() }
        val target = File(directory, "marche-ames-${info.tag}.apk")
        target.delete()
        runCatching {
            val connection = URL(info.apkUrl).openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 20_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("User-Agent", "LaMarcheDesAmes-Android")
            val total = connection.contentLengthLong
            var read = 0L
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        read += count
                        if (total > 0) {
                            onProgress((read.toDouble() / total).toFloat().coerceIn(0f, 1f))
                        }
                    }
                    output.flush()
                }
            }
            if (total > 0) onProgress(1f)
            if (info.sha256 != null && sha256(target) != info.sha256) {
                target.delete()
                return@runCatching null
            }
            target
        }.getOrNull()
    }

    internal fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(16 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}
