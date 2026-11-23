package com.brunothesatellite.idlesteprpg.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.brunothesatellite.idlesteprpg.MainActivity
import com.brunothesatellite.idlesteprpg.R
import java.text.NumberFormat

class MarcheWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { id -> updateOne(context, appWidgetManager, id) }
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(
                ComponentName(context, MarcheWidgetProvider::class.java)
            )
            ids.forEach { updateOne(context, manager, it) }
        }

        private fun updateOne(
            context: Context,
            manager: AppWidgetManager,
            id: Int
        ) {
            val prefs = context.getSharedPreferences("widget", Context.MODE_PRIVATE)
            val steps = prefs.getInt("steps", 0)
            val level = prefs.getInt("level", 1)
            val floor = prefs.getInt("floor", 1)
            val gold = prefs.getInt("gold", 0)
            val views = RemoteViews(context.packageName, R.layout.widget_marche)
            val locale = context.resources.configuration.locales[0]
            val format = NumberFormat.getIntegerInstance(locale)
            views.setTextViewText(
                R.id.widget_steps,
                context.getString(R.string.widget_steps, format.format(steps))
            )
            views.setTextViewText(
                R.id.widget_meta,
                context.getString(
                    R.string.widget_meta,
                    floor,
                    level,
                    format.format(gold)
                )
            )
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pending = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pending)
            manager.updateAppWidget(id, views)
        }
    }
}
