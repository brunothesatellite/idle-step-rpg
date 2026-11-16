package com.brunothesatellite.idlesteprpg.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import kotlin.math.roundToInt

object SoundFx {

    private val NAMES = listOf(
        "ui_tap", "hit", "crit", "hurt", "level", "boss",
        "chest", "consume", "victory", "defeat", "upgrade"
    )

    private var pool: SoundPool? = null
    private val ids = mutableMapOf<String, Int>()

    @Volatile private var enabled = true
    @Volatile private var volume = 0.6f

    fun init(context: Context) {
        configure(context)
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(attributes)
            .build()
        NAMES.forEach { name ->
            runCatching {
                context.assets.openFd("audio/$name.wav").use { fd ->
                    soundPool.load(fd, 1)
                }
            }.onSuccess { id -> ids[name] = id }
        }
        pool = soundPool
    }

    fun configure(context: Context) {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        enabled = prefs.getBoolean("sounds_enabled", true)
        volume = prefs.getFloat("sound_volume", 0.6f).coerceIn(0f, 1f)
    }

    fun play(context: Context, name: String, scale: Float = 1f) {
        if (!enabled || volume <= 0.001f) return
        val soundPool = pool ?: return
        val id = ids[name] ?: return
        val level = (volume * scale).coerceIn(0f, 1f)
        runCatching {
            soundPool.play(id, level, level, 1, 0, 1f)
        }
    }

    fun preview(context: Context) {
        configure(context)
        play(context, "ui_tap", 1.2f)
    }

    fun volumePercent(context: Context): Int {
        configure(context)
        return (volume * 100).roundToInt()
    }
}
