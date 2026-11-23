package com.brunothesatellite.idlesteprpg.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import com.brunothesatellite.idlesteprpg.engine.GameState
import com.brunothesatellite.idlesteprpg.ui.heroLookLayers
import java.io.File

/**
 * Rend le paper doll du héros (équipement actuel) pour le widget.
 * Le rendu est aussi possible directement depuis les couches stockées en
 * préférences, pour ne pas dépendre d'un fichier PNG intermédiaire.
 */
object WidgetHero {
    private const val SIZE = 192
    private const val FILE_NAME = "widget_hero.png"

    /** Change dès qu'une pièce d'équipement change de nature ou de slot. */
    fun signature(state: GameState): String =
        state.equipment
            .sortedBy { it.slot }
            .joinToString("|") { "${it.slot}:${it.icon}:${it.name}" }

    fun renderLayers(context: Context, layers: List<String>): Bitmap? = runCatching {
        val out = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint().apply {
            isFilterBitmap = false
            isAntiAlias = false
        }
        val paths = layers.ifEmpty { listOf("hero/human_m.png") }
        for (path in paths) {
            val sprite = runCatching {
                context.assets.open(path).use { BitmapFactory.decodeStream(it) }
            }.getOrNull() ?: continue
            val scaled = Bitmap.createScaledBitmap(sprite, SIZE, SIZE, false)
            canvas.drawBitmap(scaled, 0f, 0f, paint)
        }
        out
    }.getOrNull()

    fun render(context: Context, state: GameState): Boolean {
        val out = renderLayers(context, heroLookLayers(state)) ?: return false
        return runCatching {
            val file = File(context.filesDir, FILE_NAME)
            val tmp = File(context.filesDir, "$FILE_NAME.tmp")
            java.io.FileOutputStream(tmp).use { stream ->
                out.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.flush()
            }
            tmp.renameTo(file) || tmp.copyTo(file, overwrite = true).let { tmp.delete(); true }
        }.getOrDefault(false)
    }

    fun load(context: Context): Bitmap? = runCatching {
        BitmapFactory.decodeFile(File(context.filesDir, FILE_NAME).absolutePath)
    }.getOrNull()
}
