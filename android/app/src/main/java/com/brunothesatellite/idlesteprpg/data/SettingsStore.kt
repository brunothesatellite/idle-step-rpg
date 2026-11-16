package com.brunothesatellite.idlesteprpg.data

import android.content.Context

class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    var textScale: Float
        get() = prefs.getFloat("text_scale", 1.35f).coerceIn(1f, 1.7f)
        set(value) = prefs.edit().putFloat("text_scale", value.coerceIn(1f, 1.7f)).apply()

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

    var quietHours: Boolean
        get() = prefs.getBoolean("quiet_hours", false)
        set(value) = prefs.edit().putBoolean("quiet_hours", value).apply()

    var autoSellCommons: Boolean
        get() = prefs.getBoolean("auto_sell_commons", false)
        set(value) = prefs.edit().putBoolean("auto_sell_commons", value).apply()

    var onboardingDone: Boolean
        get() = prefs.getBoolean("onboarding_done", false)
        set(value) = prefs.edit().putBoolean("onboarding_done", value).apply()
}
