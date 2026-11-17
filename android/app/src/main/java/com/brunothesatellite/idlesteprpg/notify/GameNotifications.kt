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
import com.brunothesatellite.idlesteprpg.engine.GATE_METERS
import com.brunothesatellite.idlesteprpg.engine.GameState
import java.time.LocalTime

object GameNotifications {

    /** Vrai quand l'application est à l'écran : inutile de notifier ce qu'il voit. */
    @Volatile
    var appVisible: Boolean = false

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
    private const val ID_SPECIAL = 17
    private const val ID_BOSS_WAIT = 18

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
                category = "level",
                vibration = vibrationsEnabled(context)
            )
        }
        if (after.fight?.isBoss == true && before.fight?.isBoss != true) {
            val boss = after.fight
            post(
                context, ID_BOSS, "Gardien du palier",
                "${boss?.enemyName ?: "Un gardien"} vous attend au palier ${after.floor}.",
                category = "boss",
                vibration = vibrationsEnabled(context)
            )
        }
        // Une rencontre spéciale est apparue (souvent en fin de simulation hors ligne).
        if (!appVisible && before.special == null && after.special != null) {
            val special = after.special
            post(
                context, ID_SPECIAL, special?.title ?: "Rencontre",
                "Une rencontre vous attend au palier ${after.floor}. Rouvrez la marche pour la résoudre.",
                category = "special",
                vibration = vibrationsEnabled(context)
            )
        }
        // Le portail du gardien est atteint mais il n'y a plus de pas : la marche
        // est en pause devant la porte, le joueur peut revenir.
        if (!appVisible && !bossWaiting(before) && bossWaiting(after)) {
            post(
                context, ID_BOSS_WAIT, "Gardien en attente",
                "Le gardien du palier ${after.floor} attend vos pas. Rouvrez la marche pour l'affronter.",
                category = "boss_waiting",
                vibration = vibrationsEnabled(context)
            )
        }
        val b = before.oaths
        val a = after.oaths
        if (!b.stepsClaimed && a.stepsClaimed) {
            post(
                context, ID_OATH_STEPS, "Serment de marche accompli",
                "Récompense : 150 or et 1 éclat.",
                category = "oaths",
                vibration = vibrationsEnabled(context)
            )
        }
        if (!b.bossesClaimed && a.bossesClaimed) {
            post(
                context, ID_OATH_BOSS, "Serment de chasse accompli",
                "Récompense : 250 or et 2 éclats.",
                category = "oaths",
                vibration = vibrationsEnabled(context)
            )
        }
        if (!b.chestsClaimed && a.chestsClaimed) {
            post(
                context, ID_OATH_CHEST, "Serment de curiosité accompli",
                "Récompense : 100 or et un objet inhabituel.",
                category = "oaths",
                vibration = vibrationsEnabled(context)
            )
        }
    }

    fun chestWarning(context: Context, message: String) {
        post(
            context, ID_CHEST, "Coffre bientôt perdu",
            "$message. Ouvrez-le ou forcez sa serrure avant expiration.",
            category = "chest",
            screen = "chest",
            vibration = vibrationsEnabled(context)
        )
    }

    fun keyExpiry(context: Context, message: String) {
        post(
            context, ID_KEY, "Compte Garmin", message,
            account = true, category = "key", screen = "garmin", vibration = true
        )
    }

    fun post(
        context: Context,
        id: Int,
        title: String,
        text: String,
        account: Boolean = false,
        category: String = "general",
        screen: String? = null,
        vibration: Boolean = true
    ) {
        if (!notificationsEnabled(context)) return
        if (!categoryEnabled(context, category)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureChannels(context)
        val quiet = !account && quietHoursEnabled(context) && inQuietHours()
        val channel = when {
            account -> CHANNEL_ACCOUNT
            quiet || !vibration -> CHANNEL_ALERTS_SILENT
            else -> CHANNEL_ALERTS
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (screen != null) putExtra("open_screen", screen)
        }
        val pending = PendingIntent.getActivity(
            context,
            id,
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

    private fun quietHoursEnabled(context: Context): Boolean =
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .getBoolean("quiet_hours", false)

    private fun categoryEnabled(context: Context, category: String): Boolean {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        return when (category) {
            "boss" -> prefs.getBoolean("notify_boss", true)
            "level" -> prefs.getBoolean("notify_level", true)
            "oaths" -> prefs.getBoolean("notify_oaths", true)
            "chest" -> prefs.getBoolean("notify_chest", true)
            "key" -> prefs.getBoolean("notify_key", true)
            "special" -> prefs.getBoolean("notify_special", true)
            "boss_waiting" -> prefs.getBoolean("notify_boss_waiting", true)
            else -> true
        }
    }

    /** Le portail du gardien est atteint et plus aucun pas n'est disponible. */
    private fun bossWaiting(state: GameState): Boolean =
        state.fight == null && state.distance >= GATE_METERS && state.stepBank < 1.0

    private fun inQuietHours(): Boolean {
        val hour = LocalTime.now().hour
        return hour >= 22 || hour < 8
    }

    fun cancelAll(context: Context) {
        runCatching {
            NotificationManagerCompat.from(context).cancelAll()
        }
    }
}
