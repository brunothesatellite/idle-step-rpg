package com.brunothesatellite.idlesteprpg.audio

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.LoopingMediaSource
import com.brunothesatellite.idlesteprpg.data.SettingsStore
import kotlin.random.Random

/**
 * Musique d'ambiance par biome.
 *
 * Chaque biome possède deux variantes de 3 minutes. Le lecteur démarre à une
 * position aléatoire, boucle sans interruption et change de piste avec un
 * court fondu quand le biome change.
 */
@OptIn(UnstableApi::class)
class AmbientMusic(
    private val context: Context,
    private val settings: SettingsStore
) {
    private var player: ExoPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var fadeRunnable: Runnable? = null
    private var visible = false
    private var started = false
    private var currentBiome = "crypts"
    private var currentTrackBiome = ""
    private var currentVariant = 0

    private val trackMillis = 180_000L

    fun setBiome(key: String) {
        if (key.isBlank() || key == currentBiome) return
        currentBiome = key
        if (visible && started) switchTrack()
    }

    fun onResume() {
        visible = true
        if (!settings.musicEnabled || !settings.onboardingDone) {
            pause()
            return
        }
        val p = ensurePlayer()
        if (!started) {
            playRandom()
            return
        }
        if (currentTrackBiome != currentBiome) {
            switchTrack()
            return
        }
        if (!p.isPlaying) p.play()
        applyVolume()
    }

    fun onPause() {
        visible = false
        player?.pause()
    }

    fun refreshSettings() {
        if (!settings.musicEnabled) {
            fadeTo(0f, 300) { player?.pause() }
            return
        }
        if (!settings.onboardingDone) {
            player?.pause()
            return
        }
        if (visible) {
            if (!started) playRandom() else onResume()
        }
    }

    fun applyVolume() {
        if (fadeRunnable == null) {
            player?.volume = settings.musicVolume
        }
    }

    private fun ensurePlayer(): ExoPlayer {
        player?.let { return it }
        val p = ExoPlayer.Builder(context).build()
        p.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    started = true
                    Log.d(
                        "AmbientMusic",
                        "ready ${currentTrackBiome}_$currentVariant pos=${player?.currentPosition} duration=${player?.duration}"
                    )
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                started = false
                Log.e("AmbientMusic", "player error", error)
            }
        })
        player = p
        return p
    }

    private fun playRandom() {
        val variant = randomVariant()
        startTrack(currentBiome, variant, randomStart = true, fadeIn = true)
    }

    private fun switchTrack() {
        if (!settings.musicEnabled) return
        fadeTo(0f, 400) {
            val variant = randomVariant()
            startTrack(currentBiome, variant, randomStart = true, fadeIn = false)
            fadeTo(settings.musicVolume, 600)
        }
    }

    private fun startTrack(biome: String, variant: Int, randomStart: Boolean, fadeIn: Boolean) {
        val p = ensurePlayer()
        currentTrackBiome = biome
        currentVariant = variant
        val uri = "asset:///music/${biome}_${variant}.ogg"
        val mediaSource = DefaultMediaSourceFactory(context)
            .createMediaSource(MediaItem.fromUri(uri))
        val source = LoopingMediaSource(mediaSource)
        p.setMediaSource(source)
        p.prepare()
        p.playWhenReady = true
        val start = if (randomStart) Random.nextLong(0L, trackMillis - 2_000L) else 0L
        p.seekTo(start)
        Log.d("AmbientMusic", "start $biome/$variant at ${start}ms")
        if (fadeIn) {
            p.volume = 0f
            fadeTo(settings.musicVolume, 700)
        } else {
            p.volume = 0f
        }
        started = true
    }

    private fun randomVariant(): Int {
        val variants = if (currentTrackBiome == currentBiome) {
            listOf(1, 2).filter { it != currentVariant }.ifEmpty { listOf(1, 2) }
        } else {
            listOf(1, 2)
        }
        return variants.random()
    }

    private fun pause() {
        player?.pause()
    }

    private fun fadeTo(target: Float, durationMs: Long, onEnd: (() -> Unit)? = null) {
        val p = player ?: run {
            onEnd?.invoke()
            return
        }
        fadeRunnable?.let { handler.removeCallbacks(it) }
        val start = p.volume
        val steps = 20
        var step = 0
        val runnable = object : Runnable {
            override fun run() {
                step++
                val fraction = step.toFloat() / steps
                p.volume = (start + (target - start) * fraction).coerceIn(0f, 1f)
                if (step >= steps) {
                    fadeRunnable = null
                    onEnd?.invoke()
                } else {
                    handler.postDelayed(this, (durationMs / steps).coerceAtLeast(10L))
                }
            }
        }
        fadeRunnable = runnable
        handler.post(runnable)
    }
}
