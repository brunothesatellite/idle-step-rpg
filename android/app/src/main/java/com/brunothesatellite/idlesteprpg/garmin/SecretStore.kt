package com.brunothesatellite.idlesteprpg.garmin

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecretStore(context: Context) {

    private val prefs = context.getSharedPreferences("garmin_secrets", Context.MODE_PRIVATE)
    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    @Synchronized
    fun saveKey(value: String) = save("key", value)

    @Synchronized
    fun key(): String? = read("key")

    @Synchronized
    fun saveToken(value: String, expiresAtEpochSeconds: Long) {
        save("token", value)
        prefs.edit().putLong("token_exp", expiresAtEpochSeconds).apply()
    }

    @Synchronized
    fun token(): String? = read("token")

    @Synchronized
    fun tokenExpiresAt(): Long = prefs.getLong("token_exp", 0L)

    @Synchronized
    fun hasKey(): Boolean = key() != null

    @Synchronized
    fun clearAll() {
        prefs.edit().clear().apply()
        runCatching { keyStore.deleteEntry(ALIAS) }
    }

    private fun save(name: String, value: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val packed = cipher.iv + encrypted
        prefs.edit().putString(name, Base64.encodeToString(packed, Base64.NO_WRAP)).apply()
    }

    private fun read(name: String): String? {
        val encoded = prefs.getString(name, null) ?: return null
        return runCatching {
            val packed = Base64.decode(encoded, Base64.NO_WRAP)
            val iv = packed.copyOfRange(0, 12)
            val encrypted = packed.copyOfRange(12, packed.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        }.getOrElse {
            prefs.edit().remove(name).apply()
            null
        }
    }

    private fun secretKey(): SecretKey {
        val existing = keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existing != null) return existing.secretKey
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    companion object {
        private const val ALIAS = "idle_step_rpg_garmin_v1"
    }
}
