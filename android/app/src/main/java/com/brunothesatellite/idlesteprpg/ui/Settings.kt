package com.brunothesatellite.idlesteprpg.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt
import com.brunothesatellite.idlesteprpg.BuildConfig
import com.brunothesatellite.idlesteprpg.GameViewModel
import com.brunothesatellite.idlesteprpg.UiState

@Composable
fun SettingsOverlay(vm: GameViewModel, ui: UiState, onClose: () -> Unit) {
    var confirmClear by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) vm.exportTo(uri) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) vm.importFrom(uri) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0B0810), Color(0xFF171121), Color(0xFF0B0810))))
            .clickable { }
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("RÉGLAGES", fontFamily = Press, fontSize = 9.sp, color = GoldLight)
                Text("Confort, données et compte", fontSize = 7.sp, color = Muted)
            }
            Text(
                "×",
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Panel2)
                    .border(1.dp, Line2, RoundedCornerShape(4.dp))
                    .clickable(onClick = onClose)
                    .padding(top = 3.dp),
                textAlign = TextAlign.Center,
                fontSize = 18.sp,
                color = Muted
            )
        }
        DividerLine()
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(14.dp)
        ) {
            PixelCard(Modifier.fillMaxWidth()) {
                SectionLabel("Affichage")
                Spacer(Modifier.height(6.dp))
                Text("Taille du texte", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Parchment)
                Spacer(Modifier.height(5.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf("Normal" to 1.0f, "Grand" to 1.35f, "Très grand" to 1.6f).forEach { (label, value) ->
                        val active = kotlin.math.abs(ui.textScale - value) < 0.01f
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (active) Gold.copy(alpha = 0.18f) else Panel2)
                                .border(1.dp, if (active) Gold.copy(alpha = 0.7f) else Line, RoundedCornerShape(4.dp))
                                .clickable { vm.setTextScale(value) }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) GoldLight else Muted,
                                maxLines = 1
                            )
                        }
                    }
                }
                Spacer(Modifier.height(9.dp))
                ToggleRow(
                    "Notifications",
                    "Gardien, niveau, serments, coffre et clé Garmin.",
                    ui.notificationsEnabled
                ) { enabled ->
                    vm.setNotificationsEnabled(enabled)
                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                if (ui.notificationsEnabled) {
                    ToggleRow("Gardien", "Quand un gardien de palier apparaît.", ui.notifyBoss) {
                        vm.setNotifyBoss(it)
                    }
                    ToggleRow("Niveau", "Chaque montée de niveau.", ui.notifyLevel) {
                        vm.setNotifyLevel(it)
                    }
                    ToggleRow("Serments", "Quand un serment est accompli.", ui.notifyOaths) {
                        vm.setNotifyOaths(it)
                    }
                    ToggleRow("Coffres", "Un coffre va bientôt expirer.", ui.notifyChest) {
                        vm.setNotifyChest(it)
                    }
                    ToggleRow("Clé Garmin", "Expiration à 30/7/1 jour.", ui.notifyKey) {
                        vm.setNotifyKey(it)
                    }
                    ToggleRow("Heures calmes", "22:00–08:00 : alertes sans vibration.", ui.quietHours) {
                        vm.setQuietHours(it)
                    }
                    val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    if (!granted) {
                        Text(
                            "La permission Android est refusée : ouvrez les réglages système pour l'autoriser.",
                            fontSize = 6.5.sp,
                            color = Blood,
                            lineHeight = 9.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        OverlayAction(
                            "OUVRIR LES RÉGLAGES SYSTÈME",
                            Modifier.fillMaxWidth(),
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                        }
                                    )
                                }
                            }
                        )
                    }
                }
                ToggleRow(
                    "Animations réduites",
                    "Supprime le balancement, les auras et les nombres flottants.",
                    ui.reducedMotion
                ) { vm.setReducedMotion(it) }
                ToggleRow(
                    "Vibrations",
                    "Vibre pour les notifications d'alerte.",
                    ui.vibrations
                ) { vm.setVibrations(it) }
                Spacer(Modifier.height(4.dp))
                OverlayAction(
                    "REVOIR L'ONBOARDING",
                    Modifier.fillMaxWidth(),
                    onClick = { vm.restartOnboarding() }
                )
            }
            Spacer(Modifier.height(10.dp))
            PixelCard(Modifier.fillMaxWidth()) {
                SectionLabel("Son")
                Spacer(Modifier.height(5.dp))
                ToggleRow(
                    "Sons 8 bits",
                    "Combat, niveau, coffre, forge et rencontres.",
                    ui.soundsEnabled
                ) { vm.setSoundsEnabled(it) }
                if (ui.soundsEnabled) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Volume", fontSize = 8.sp, color = Parchment)
                        Text(
                            "${(ui.soundVolume * 100).roundToInt()} %",
                            fontFamily = Press,
                            fontSize = 7.sp,
                            color = Gold
                        )
                    }
                    Slider(
                        value = ui.soundVolume,
                        onValueChange = { vm.setSoundVolume(it) },
                        onValueChangeFinished = { vm.previewSound() },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = GoldLight,
                            activeTrackColor = Gold,
                            inactiveTrackColor = Line2
                        )
                    )
                    OverlayAction(
                        "TESTER LE SON",
                        Modifier.fillMaxWidth(),
                        onClick = { vm.previewSound() }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            PixelCard(Modifier.fillMaxWidth()) {
                SectionLabel("Aide")
                Spacer(Modifier.height(5.dp))
                OverlayAction(
                    "OUVRIR LE MANUEL DU JEU",
                    Modifier.fillMaxWidth(),
                    onClick = {
                        context.startActivity(
                            Intent(context, com.brunothesatellite.idlesteprpg.ManualActivity::class.java)
                        )
                    }
                )
            }
            Spacer(Modifier.height(10.dp))
            PixelCard(Modifier.fillMaxWidth()) {
                SectionLabel("Inventaire")
                Spacer(Modifier.height(5.dp))
                ToggleRow(
                    "Vendre les communs automatiquement",
                    "Les objets communs non verrouillés sont convertis en or au ramassage.",
                    ui.autoSellCommons
                ) { vm.setAutoSellCommons(it) }
            }
            Spacer(Modifier.height(10.dp))
            PixelCard(Modifier.fillMaxWidth()) {
                SectionLabel("Sauvegarde")
                Spacer(Modifier.height(5.dp))
                Text(
                    "La progression est aussi sauvegardée automatiquement. Exportez-la avant de changer de téléphone.",
                    fontSize = 6.8.sp,
                    color = Muted,
                    lineHeight = 10.sp
                )
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    OverlayAction(
                        "EXPORTER",
                        Modifier.weight(1f),
                        onClick = { exportLauncher.launch("marche-ames-sauvegarde.json") }
                    )
                    OverlayAction(
                        "IMPORTER",
                        Modifier.weight(1f),
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }
                    )
                }
                Spacer(Modifier.height(5.dp))
                OverlayAction(
                    "EFFACER LA PROGRESSION",
                    Modifier.fillMaxWidth(),
                    { confirmClear = true },
                    danger = true
                )
            }
            Spacer(Modifier.height(10.dp))
            PixelCard(Modifier.fillMaxWidth()) {
                SectionLabel("Journal de synchronisation")
                Spacer(Modifier.height(4.dp))
                if (ui.syncLog.isEmpty()) {
                    Text("Aucune synchronisation enregistrée.", fontSize = 7.sp, color = Muted2)
                } else {
                    ui.syncLog.forEach { line ->
                        Text(
                            line,
                            fontSize = 6.8.sp,
                            color = if (line.contains("ERREUR")) Blood else Muted,
                            modifier = Modifier.padding(vertical = 1.5.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            PixelCard(Modifier.fillMaxWidth(), borderColor = Gold.copy(alpha = 0.3f)) {
                SectionLabel("À propos")
                Spacer(Modifier.height(4.dp))
                Text("La Marche des Âmes · v${BuildConfig.VERSION_NAME}", fontSize = 8.sp, color = Parchment)
                Spacer(Modifier.height(3.dp))
                Text(
                    "Jeu de marche personnel. Pas de Health Connect, pas de serveur, pas de compte autre que Garmin. " +
                        "La 2FA Garmin reste dans le navigateur.",
                    fontSize = 6.8.sp,
                    color = Muted,
                    lineHeight = 10.sp
                )
                Spacer(Modifier.height(6.dp))
                OverlayAction(
                    "VÉRIFIER LES MISES À JOUR",
                    Modifier.fillMaxWidth(),
                    onClick = { vm.checkForUpdate() }
                )
                if (ui.updateTag != null && ui.updateUrl != null) {
                    Spacer(Modifier.height(4.dp))
                    OverlayAction("OUVRIR ${ui.updateTag}", Modifier.fillMaxWidth(), {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ui.updateUrl)))
                        }
                    }, primary = true)
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    OverlayAction(
                        "LICENCES",
                        Modifier.weight(1f),
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://github.com/brunothesatellite/idle-step-rpg/blob/main/LICENSES.md")
                                    )
                                )
                            }
                        }
                    )
                    OverlayAction(
                        "CONFIDENTIALITÉ",
                        Modifier.weight(1.3f),
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://github.com/brunothesatellite/idle-step-rpg/blob/main/docs/privacy.md")
                                    )
                                )
                            }
                        }
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Code sous licence MIT · ressources CC0/OFL",
                    fontSize = 6.5.sp,
                    color = Muted2
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Effacer la progression ?", fontSize = 15.sp) },
            text = {
                Text(
                    "Or, objets, paliers, serments et journal seront remis à zéro. " +
                        "La clé Garmin et tes préférences seront conservées.",
                    fontSize = 9.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    vm.clearProgress()
                }) { Text("EFFACER", color = Blood) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("ANNULER", color = Muted) }
            },
            containerColor = Panel2
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    enabled: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .clickable { onChange(!enabled) }
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Parchment)
            Text(subtitle, fontSize = 6.2.sp, color = Muted, lineHeight = 9.sp)
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .width(42.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (enabled) Green.copy(alpha = 0.28f) else Panel3)
                .border(1.dp, if (enabled) Green.copy(alpha = 0.7f) else Line2, RoundedCornerShape(10.dp)),
            contentAlignment = if (enabled) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                Modifier
                    .padding(horizontal = 3.dp)
                    .size(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (enabled) Green else Muted2)
            )
        }
    }
}
