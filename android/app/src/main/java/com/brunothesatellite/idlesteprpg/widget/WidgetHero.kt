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
 * Rend le paper doll du héros (équipement actuel) dans un PNG que le widget
 * peut afficher. Le widget n'a pas accès à l'état du jeu : le rendu est fait
 * côté application, puis relu par le provider.
 */
object WidgetHero {
    private const val SIZE = 192
    private const val FILE_NAME = "widget_hero.png"

    /** Change dès qu'une pièce d'équipement change de nature ou de slot. */
    fun signature(state: GameState): String =
        state.equipment
            .sortedBy { it.slot }
            .joinToString("|") { "${it.slot}:${it.icon}:${it.name}" }

    fun render(context: Context, state: GameState): Boolean = runCatching {
        val out = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint().apply {
            isFilterBitmap = false
            isAntiAlias = false
        }
        for (path in heroLookLayers(state)) {
            val sprite = context.assets.open(path).use { BitmapFactory.decodeStream(it) }
                ?: continue
            val scaled = Bitmap.createScaledBitmap(sprite, SIZE, SIZE, false)
            canvas.drawBitmap(scaled, 0f, 0f, paint)
        }
        val file = File(context.filesDir, FILE_NAME)
        val tmp = File(context.filesDir, "$FILE_NAME.tmp")
        java.io.FileOutputStream(tmp).use { stream ->
            out.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
        }
        tmp.renameTo(file) || tmp.copyTo(file, overwrite = true).let { tmp.delete(); true }
    }.getOrDefault(false)

    fun load(context: Context): Bitmap? = runCatching {
        BitmapFactory.decodeFile(File(context.filesDir, FILE_NAME).absolutePath)
    }.getOrNull()
}
