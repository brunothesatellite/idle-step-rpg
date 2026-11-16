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
}
