package com.brunothesatellite.idlesteprpg.data

import android.content.Context
import com.brunothesatellite.idlesteprpg.engine.OFFLINE_DEFAULT_PERCENT
import com.brunothesatellite.idlesteprpg.engine.OFFLINE_DEFAULT_STEPS
import com.brunothesatellite.idlesteprpg.engine.OFFLINE_MAX_PERCENT
import com.brunothesatellite.idlesteprpg.engine.OFFLINE_MAX_STEPS
import com.brunothesatellite.idlesteprpg.engine.OfflineMode
import com.brunothesatellite.idlesteprpg.engine.OfflinePolicy

class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    var textScale: Float
        get() = prefs.getFloat("text_scale", 1.35f).coerceIn(1f, 2f)
        set(value) = prefs.edit().putFloat("text_scale", value.coerceIn(1f, 2f)).apply()

    var reducedMotion: Boolean
        get() = prefs.getBoolean("reduced_motion", false)
        set(value) = prefs.edit().putBoolean("reduced_motion", value).apply()

    var vibrations: Boolean
        get() = prefs.getBoolean("vibrations", true)
        set(value) = prefs.edit().putBoolean("vibrations", value).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean("notifications", true)
        set(value) = prefs.edit().putBoolean("notifications", value).apply()

    var notifyBoss: Boolean
        get() = prefs.getBoolean("notify_boss", true)
        set(value) = prefs.edit().putBoolean("notify_boss", value).apply()

    var notifyLevel: Boolean
        get() = prefs.getBoolean("notify_level", true)
        set(value) = prefs.edit().putBoolean("notify_level", value).apply()

    var notifyOaths: Boolean
        get() = prefs.getBoolean("notify_oaths", true)
        set(value) = prefs.edit().putBoolean("notify_oaths", value).apply()

    var notifyChest: Boolean
        get() = prefs.getBoolean("notify_chest", true)
        set(value) = prefs.edit().putBoolean("notify_chest", value).apply()

    var notifyKey: Boolean
        get() = prefs.getBoolean("notify_key", true)
        set(value) = prefs.edit().putBoolean("notify_key", value).apply()

    var notifySync: Boolean
        get() = prefs.getBoolean("notify_sync", false)
        set(value) = prefs.edit().putBoolean("notify_sync", value).apply()

    var notifySpecial: Boolean
        get() = prefs.getBoolean("notify_special", true)
        set(value) = prefs.edit().putBoolean("notify_special", value).apply()

    var notifyBossWaiting: Boolean
        get() = prefs.getBoolean("notify_boss_waiting", true)
        set(value) = prefs.edit().putBoolean("notify_boss_waiting", value).apply()

    var quietHours: Boolean
        get() = prefs.getBoolean("quiet_hours", false)
        set(value) = prefs.edit().putBoolean("quiet_hours", value).apply()

    var autoSellCommons: Boolean
        get() = prefs.getBoolean("auto_sell_commons", false)
        set(value) = prefs.edit().putBoolean("auto_sell_commons", value).apply()

    var onboardingDone: Boolean
        get() = prefs.getBoolean("onboarding_done", false)
        set(value) = prefs.edit().putBoolean("onboarding_done", value).apply()

    var soundsEnabled: Boolean
        get() = prefs.getBoolean("sounds_enabled", true)
        set(value) = prefs.edit().putBoolean("sounds_enabled", value).apply()

    var soundVolume: Float
        get() = prefs.getFloat("sound_volume", 0.6f).coerceIn(0f, 1f)
        set(value) = prefs.edit().putFloat("sound_volume", value.coerceIn(0f, 1f)).apply()

    var musicEnabled: Boolean
        get() = prefs.getBoolean("music_enabled", true)
        set(value) = prefs.edit().putBoolean("music_enabled", value).apply()

    var musicVolume: Float
        get() = prefs.getFloat("music_volume", 0.30f).coerceIn(0f, 1f)
        set(value) = prefs.edit().putFloat("music_volume", value.coerceIn(0f, 1f)).apply()

    var autoUpdateCheck: Boolean
        get() = prefs.getBoolean("auto_update_check", true)
        set(value) = prefs.edit().putBoolean("auto_update_check", value).apply()

    var lastUpdateCheckAt: Long
        get() = prefs.getLong("last_update_check", 0L)
        set(value) = prefs.edit().putLong("last_update_check", value).apply()

    var dismissedUpdateTag: String
        get() = prefs.getString("dismissed_update_tag", "").orEmpty()
        set(value) = prefs.edit().putString("dismissed_update_tag", value).apply()

    var pendingReleaseTag: String
        get() = prefs.getString("pending_release_tag", "").orEmpty()
        set(value) = prefs.edit().putString("pending_release_tag", value).apply()

    var pendingReleaseUrl: String
        get() = prefs.getString("pending_release_url", "").orEmpty()
        set(value) = prefs.edit().putString("pending_release_url", value).apply()

    /** Source de pas active : "garmin" ou "health" (Santé Connect). */
    var stepSource: String
        get() = prefs.getString("step_source", "garmin").orEmpty().ifBlank { "garmin" }
        set(value) = prefs.edit().putString("step_source", value).apply()

    /**
     * Vrai juste après un changement de source : le premier import sert de base
     * (les totaux du jour ne sont pas recrédités), pour éviter un saut de pas
     * quand la nouvelle source annonce plus que l'ancienne.
     */
    var pendingSourceRebase: Boolean
        get() = prefs.getBoolean("pending_source_rebase", false)
        set(value) = prefs.edit().putBoolean("pending_source_rebase", value).apply()

    /** Réglages avancés d'absence : mode "percent" ou "steps". */
    var offlineMode: String
        get() = prefs.getString("offline_mode", "percent").orEmpty().ifBlank { "percent" }
        set(value) = prefs.edit().putString("offline_mode", value).apply()

    var offlinePercent: Int
        get() = prefs.getInt("offline_percent", OFFLINE_DEFAULT_PERCENT)
            .coerceIn(0, OFFLINE_MAX_PERCENT)
        set(value) = prefs.edit()
            .putInt("offline_percent", value.coerceIn(0, OFFLINE_MAX_PERCENT))
            .apply()

    var offlineMaxSteps: Int
        get() = prefs.getInt("offline_steps", OFFLINE_DEFAULT_STEPS)
            .coerceIn(0, OFFLINE_MAX_STEPS)
        set(value) = prefs.edit()
            .putInt("offline_steps", value.coerceIn(0, OFFLINE_MAX_STEPS))
            .apply()

    val offlinePolicy: OfflinePolicy
        get() = OfflinePolicy(
            mode = if (offlineMode == "steps") OfflineMode.STEPS else OfflineMode.PERCENT,
            percent = offlinePercent,
            maxSteps = offlineMaxSteps
        )
}
