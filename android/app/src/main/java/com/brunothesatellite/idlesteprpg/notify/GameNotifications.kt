package com.brunothesatellite.idlesteprpg.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.brunothesatellite.idlesteprpg.MainActivity
import com.brunothesatellite.idlesteprpg.R
import com.brunothesatellite.idlesteprpg.engine.GameState

object GameNotifications {

    private const val CHANNEL_ALERTS = "alertes_marche"
    private const val CHANNEL_ALERTS_SILENT = "alertes_marche_silencieux"
    private const val CHANNEL_ACCOUNT = "compte_garmin"

    private const val ID_LEVEL = 10
    private const val ID_BOSS = 11
    private const val ID_OATH_STEPS = 12
    private const val ID_OATH_BOSS = 13
    private const val ID_OATH_CHEST = 14
    private const val ID_CHEST = 15
    private const val ID_KEY = 16

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val alerts = NotificationChannel(
            CHANNEL_ALERTS,
            "Alertes de marche",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Gardien, niveau, serments et coffres"
            enableVibration(true)
        }
        val silent = NotificationChannel(
            CHANNEL_ALERTS_SILENT,
            "Alertes de marche (sans vibration)",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Gardien, niveau, serments et coffres"
            enableVibration(false)
            setSound(null, null)
        }
        val account = NotificationChannel(
            CHANNEL_ACCOUNT,
            "Compte Garmin",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Expiration de la clé personnelle Garmin"
        }
        manager.createNotificationChannels(listOf(alerts, silent, account))
    }

    fun diff(context: Context, before: GameState, after: GameState) {
        if (after.level > before.level) {
            post(
                context, ID_LEVEL, "Niveau ${after.level}",
                "Kaelen se renforce. Nouvelles stats disponibles.",
                vibration = vibrationsEnabled(context)
            )
        }
        if (after.fight?.isBoss == true && before.fight?.isBoss != true) {
            val boss = after.fight
            post(
                context, ID_BOSS, "Gardien du palier",
                "${boss?.enemyName ?: "Un gardien"} vous attend au palier ${after.floor}.",
                vibration = vibrationsEnabled(context)
            )
        }
        val b = before.oaths
        val a = after.oaths
        if (!b.stepsClaimed && a.stepsClaimed) {
            post(
                context, ID_OATH_STEPS, "Serment de marche accompli",
                "Récompense : 150 or et 1 éclat.",
                vibration = vibrationsEnabled(context)
            )
        }
        if (!b.bossesClaimed && a.bossesClaimed) {
            post(
                context, ID_OATH_BOSS, "Serment de chasse accompli",
                "Récompense : 250 or et 2 éclats.",
                vibration = vibrationsEnabled(context)
            )
        }
        if (!b.chestsClaimed && a.chestsClaimed) {
            post(
                context, ID_OATH_CHEST, "Serment de curiosité accompli",
                "Récompense : 100 or et un objet inhabituel.",
                vibration = vibrationsEnabled(context)
            )
        }
    }

    fun chestWarning(context: Context, message: String) {
        post(
            context, ID_CHEST, "Coffre bientôt perdu",
            "$message. Ouvrez-le ou forcez sa serrure avant expiration.",
            vibration = vibrationsEnabled(context)
        )
    }

    fun keyExpiry(context: Context, message: String) {
        post(context, ID_KEY, "Compte Garmin", message, account = true, vibration = true)
    }

    fun post(
        context: Context,
        id: Int,
        title: String,
        text: String,
        account: Boolean = false,
        vibration: Boolean = true
    ) {
        if (!notificationsEnabled(context)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureChannels(context)
        val channel = when {
            account -> CHANNEL_ACCOUNT
            vibration -> CHANNEL_ALERTS
            else -> CHANNEL_ALERTS_SILENT
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(id, notification)
        }
    }

    private fun vibrationsEnabled(context: Context): Boolean =
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .getBoolean("vibrations", true)

    private fun notificationsEnabled(context: Context): Boolean =
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .getBoolean("notifications", true)

    fun cancelAll(context: Context) {
        runCatching {
            NotificationManagerCompat.from(context).cancelAll()
        }
    }
}
