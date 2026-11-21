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
import com.brunothesatellite.idlesteprpg.engine.GameState
import com.brunothesatellite.idlesteprpg.engine.OfflineMode
import com.brunothesatellite.idlesteprpg.engine.OfflinePolicy
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun SettingsOverlay(vm: GameViewModel, ui: UiState, state: GameState, onClose: () -> Unit) {
    var confirmClear by remember { mutableStateOf(false) }
    var pendingSource by remember { mutableStateOf<String?>(null) }
    var diagnosticOpen by remember { mutableStateOf(false) }
    var advancedOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val healthLauncher = rememberLauncherForActivityResult(
        androidx.health.connect.client.PermissionController.createRequestPermissionResultContract()
    ) { granted -> vm.onHealthPermissionResult(granted.isNotEmpty()) }
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
            CloseButton(onClose)
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
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf(
                        "Normal" to 1.0f,
                        "Grand" to 1.35f,
                        "Très grand" to 1.6f,
                        "Maximal" to 2.0f
                    ).chunked(2).forEach { half ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            half.forEach { (label, value) ->
                                val active = kotlin.math.abs(ui.textScale - value) < 0.01f
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (active) Gold.copy(alpha = 0.18f) else Panel2)
                                        .border(
                                            1.dp,
                                            if (active) Gold.copy(alpha = 0.7f) else Line,
                                            RoundedCornerShape(4.dp)
                                        )
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
                            if (half.size == 1) {
                                Box(Modifier.weight(1f))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(9.dp))
                ToggleRow(
                    "Notifications",
                    "Rencontre, gardien, niveau, serments, coffre et clé Garmin.",
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
                    ToggleRow(
                        "Rencontre en attente",
                        "Une rencontre spéciale attend votre retour après une absence.",
                        ui.notifySpecial
                    ) { vm.setNotifySpecial(it) }
                    ToggleRow(
                        "Gardien en attente",
                        "Le portail est atteint mais il ne reste plus de pas.",
                        ui.notifyBossWaiting
                    ) { vm.setNotifyBossWaiting(it) }
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
                Spacer(Modifier.height(9.dp))
                DividerLine()
                Spacer(Modifier.height(7.dp))
                ToggleRow(
                    "Musique d'ambiance",
                    "Deux pistes par biome, reprise à une position aléatoire.",
                    ui.musicEnabled
                ) { vm.setMusicEnabled(it) }
                if (ui.musicEnabled) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Volume musique", fontSize = 8.sp, color = Parchment)
                        Text(
                            "${(ui.musicVolume * 100).roundToInt()} %",
                            fontFamily = Press,
                            fontSize = 7.sp,
                            color = Gold
                        )
                    }
                    Slider(
                        value = ui.musicVolume,
                        onValueChange = { vm.setMusicVolume(it) },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = GoldLight,
                            activeTrackColor = Gold,
                            inactiveTrackColor = Line2
                        )
                    )
                    Text(
                        "Volume indépendant des effets sonores. La musique se met en pause quand l'application passe en arrière-plan.",
                        fontSize = 6.2.sp,
                        color = Muted2,
                        lineHeight = 9.sp
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            if (!BuildConfig.PLAY_DISTRIBUTION) {
            PixelCard(Modifier.fillMaxWidth()) {
                SectionLabel("Mises à jour")
                Spacer(Modifier.height(5.dp))
                ToggleRow(
                    "Vérification automatique",
                    "Une fois par jour au lancement. L'installation reste toujours confirmée par vous.",
                    ui.autoUpdateCheck
                ) { vm.setAutoUpdateCheck(it) }
                Spacer(Modifier.height(4.dp))
                Text(
                    lastCheckLabel(ui.lastUpdateCheckAt),
                    fontSize = 6.8.sp,
                    color = Muted
                )
                Spacer(Modifier.height(6.dp))
                OverlayAction(
                    if (ui.updateChecking) "VÉRIFICATION…" else "VÉRIFIER MAINTENANT",
                    Modifier.fillMaxWidth(),
                    onClick = { vm.checkForUpdate(manual = true) },
                    enabled = !ui.updateChecking
                )
                ui.availableUpdate?.let { info ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Version ${info.tag} disponible.",
                        fontSize = 7.sp,
                        color = GoldLight
                    )
                    Spacer(Modifier.height(4.dp))
                    OverlayAction(
                        "INSTALLER ${info.tag}",
                        Modifier.fillMaxWidth(),
                        onClick = vm::installUpdate,
                        primary = true
                    )
                    Spacer(Modifier.height(4.dp))
                    OverlayAction(
                        "VOIR LA RELEASE",
                        Modifier.fillMaxWidth(),
                        onClick = { vm.openRelease(info.releaseUrl) }
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "L'APK est téléchargé depuis les releases GitHub et vérifié par SHA-256 " +
                        "quand l'empreinte est publiée.",
                    fontSize = 6.5.sp,
                    color = Muted2,
                    lineHeight = 9.sp
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
            PixelCard(
                Modifier.fillMaxWidth(),
                borderColor = if (ui.connected) Green.copy(alpha = 0.3f) else Ember.copy(alpha = 0.35f)
            ) {
                SectionLabel("Source de pas")
                Text(
                    ui.stepStatus,
                    fontSize = 7.5.sp,
                    color = if (ui.connected) Green else Ember
                )
                Spacer(Modifier.height(6.dp))
                SourceRow(
                    title = "Garmin",
                    subtitle = "Personal API · clé personnelle",
                    selected = ui.stepSource == "garmin",
                    onClick = { if (ui.stepSource != "garmin") pendingSource = "garmin" }
                )
                Spacer(Modifier.height(4.dp))
                SourceRow(
                    title = "Santé Connect",
                    subtitle = if (ui.healthAvailable) "Google Fit, Samsung Health, montre…"
                    else "Indisponible sur cet appareil",
                    selected = ui.stepSource == "health",
                    enabled = ui.healthAvailable,
                    onClick = { if (ui.stepSource != "health") pendingSource = "health" }
                )
                Spacer(Modifier.height(6.dp))
                if (ui.stepSource == "garmin") {
                    val keyDays = ui.keyExpiry?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) }
                    if (keyDays != null && keyDays < 30) {
                        Text(
                            if (keyDays < 0) "Clé Garmin expirée — remplacez-la."
                            else "Clé Garmin expire dans $keyDays jour(s).",
                            fontSize = 6.8.sp,
                            color = Blood
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    if (ui.connected) {
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            OverlayAction(
                                "SYNCHRONISER",
                                Modifier.weight(1f),
                                { vm.syncNow() },
                                enabled = !ui.syncing
                            )
                            OverlayAction("GÉRER LA CLÉ", Modifier.weight(1f), { vm.openSettings() })
                        }
                    } else {
                        Text(
                            "Sans clé Garmin, la marche ne peut pas démarrer avec cette source.",
                            fontSize = 7.sp,
                            color = Muted,
                            lineHeight = 10.sp
                        )
                        Spacer(Modifier.height(5.dp))
                        OverlayAction(
                            "CONNECTER GARMIN",
                            Modifier.fillMaxWidth(),
                            { vm.openSettings() },
                            primary = true
                        )
                    }
                } else {
                    Text(
                        "Les pas sont lus dans Santé Connect. Aucune application ne fournit de pas ? " +
                            "Installez Google Fit ou synchronisez une montre.",
                        fontSize = 7.sp,
                        color = Muted,
                        lineHeight = 10.sp
                    )
                    Spacer(Modifier.height(5.dp))
                    if (!ui.healthPermission) {
                        OverlayAction(
                            "AUTORISER SANTÉ CONNECT",
                            Modifier.fillMaxWidth(),
                            { healthLauncher.launch(setOf(vm.healthPermissionName())) },
                            primary = true
                        )
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            OverlayAction(
                                "SYNCHRONISER",
                                Modifier.weight(1f),
                                { vm.syncNow() },
                                enabled = !ui.syncing
                            )
                            OverlayAction(
                                "AUTORISATION",
                                Modifier.weight(1f),
                                { healthLauncher.launch(setOf(vm.healthPermissionName())) }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            PixelCard(Modifier.fillMaxWidth()) {
                SectionLabel("Diagnostic")
                Spacer(Modifier.height(4.dp))
                Text(
                    "Fenêtre d'import, jours crédités et garde-fous appliqués par la synchronisation.",
                    fontSize = 6.8.sp,
                    color = Muted,
                    lineHeight = 10.sp
                )
                Spacer(Modifier.height(6.dp))
                OverlayAction(
                    "OUVRIR LE DIAGNOSTIC",
                    Modifier.fillMaxWidth(),
                    onClick = { diagnosticOpen = true }
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
            PixelCard(Modifier.fillMaxWidth()) {
                val offlineMode = if (ui.offlineMode == "steps") OfflineMode.STEPS else OfflineMode.PERCENT
                val offlinePolicy = OfflinePolicy(
                    mode = offlineMode,
                    percent = ui.offlinePercent,
                    maxSteps = ui.offlineMaxSteps
                )
                val dailyBudget = offlinePolicy.budgetFor(state.stepsToday)
                val remainingBudget = (dailyBudget - state.offlineStepsToday).coerceAtLeast(0)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { advancedOpen = !advancedOpen }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Panel2)
                            .border(1.dp, Line2, RoundedCornerShape(3.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (advancedOpen) "-" else "+",
                            fontFamily = Press,
                            fontSize = 9.sp,
                            color = GoldLight
                        )
                    }
                    Spacer(Modifier.width(7.dp))
                    Column(Modifier.weight(1f)) {
                        SectionLabel("Réglages avancés")
                        Text("Absence et équilibrage", fontSize = 6.5.sp, color = Muted2)
                    }
                }
                if (advancedOpen) {
                    Spacer(Modifier.height(6.dp))
                    DividerLine()
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "ABSENCE HORS LIGNE",
                        fontFamily = Press,
                        fontSize = 6.5.sp,
                        letterSpacing = 1.sp,
                        color = GoldLight
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "La réserve s'écoule quand l'application est fermée, dans la limite du budget choisi. " +
                            "La durée de simulation découle des pas.",
                        fontSize = 6.6.sp,
                        color = Muted,
                        lineHeight = 10.sp
                    )
                    Spacer(Modifier.height(7.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        ChoiceChip("Pourcentage", offlineMode == OfflineMode.PERCENT) {
                            vm.setOfflineMode("percent")
                        }
                        ChoiceChip("Pas maximum", offlineMode == OfflineMode.STEPS) {
                            vm.setOfflineMode("steps")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            if (offlineMode == OfflineMode.PERCENT) "% des pas du jour" else "Pas hors ligne",
                            fontSize = 8.sp,
                            color = Parchment
                        )
                        Text(
                            if (offlineMode == OfflineMode.PERCENT) "${ui.offlinePercent} %"
                            else "${ui.offlineMaxSteps} pas",
                            fontFamily = Press,
                            fontSize = 7.sp,
                            color = Gold
                        )
                    }
                    Slider(
                        value = if (offlineMode == OfflineMode.PERCENT) ui.offlinePercent.toFloat()
                        else ui.offlineMaxSteps.toFloat(),
                        onValueChange = {
                            if (offlineMode == OfflineMode.PERCENT) {
                                vm.setOfflinePercent(it.roundToInt())
                            } else {
                                vm.setOfflineMaxSteps((it / 50f).roundToInt() * 50)
                            }
                        },
                        valueRange = if (offlineMode == OfflineMode.PERCENT) 0f..30f else 0f..5_000f,
                        steps = if (offlineMode == OfflineMode.PERCENT) 29 else 99,
                        colors = SliderDefaults.colors(
                            thumbColor = GoldLight,
                            activeTrackColor = Gold,
                            inactiveTrackColor = Line2
                        )
                    )
                    if (offlineMode == OfflineMode.STEPS) {
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            listOf(300, 1_000, 3_000).forEach { preset ->
                                ChoiceChip("$preset", ui.offlineMaxSteps == preset) {
                                    vm.setOfflineMaxSteps(preset)
                                }
                            }
                        }
                        Spacer(Modifier.height(5.dp))
                    }
                    Text(
                        if (dailyBudget <= 0) "Absence hors ligne désactivée."
                        else "≈ ${offlineDurationLabel(dailyBudget / 60)} de simulation · jusqu'à " +
                            "${offlineDurationLabel(dailyBudget / 30)} si la réserve est presque vide",
                        fontSize = 6.4.sp,
                        color = Muted
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (dailyBudget <= 0) "Aujourd'hui : aucun pas autorisé hors ligne."
                        else "Aujourd'hui : $dailyBudget pas max · $remainingBudget encore disponibles " +
                            "(${state.offlineStepsToday} consommés)",
                        fontSize = 6.4.sp,
                        color = Muted2
                    )
                    Spacer(Modifier.height(7.dp))
                    OverlayAction("RÉTABLIR 30 %", Modifier.fillMaxWidth(), onClick = { vm.resetOfflinePolicy() })
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Ces réglages réduisent la progression hors ligne. 30 % est la valeur recommandée.",
                        fontSize = 6.2.sp,
                        color = Muted2,
                        lineHeight = 9.sp
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            PixelCard(Modifier.fillMaxWidth(), borderColor = Gold.copy(alpha = 0.3f)) {
                SectionLabel("À propos")
                Spacer(Modifier.height(4.dp))
                Text("La Marche des Âmes · v${BuildConfig.VERSION_NAME}", fontSize = 8.sp, color = Parchment)
                Spacer(Modifier.height(3.dp))
                Text(
                    "Jeu de marche personnel. Aucun serveur de jeu : les pas viennent de Garmin " +
                        "(Personal API) ou de Santé Connect, et la 2FA Garmin reste dans le navigateur.",
                    fontSize = 6.8.sp,
                    color = Muted,
                    lineHeight = 10.sp
                )
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
            title = { WithAppFontScale { Text("Effacer la progression ?", fontSize = 15.sp) } },
            text = {
                WithAppFontScale {
                    Text(
                        "Or, objets, paliers, serments et journal seront remis à zéro. " +
                            "La clé Garmin et tes préférences seront conservées.",
                        fontSize = 9.sp
                    )
                }
            },
            confirmButton = {
                WithAppFontScale {
                    TextButton(onClick = {
                        confirmClear = false
                        vm.clearProgress()
                    }) { Text("EFFACER", color = Blood) }
                }
            },
            dismissButton = {
                WithAppFontScale {
                    TextButton(onClick = { confirmClear = false }) { Text("ANNULER", color = Muted) }
                }
            },
            containerColor = Panel2
        )
    }

    val target = pendingSource?.let { com.brunothesatellite.idlesteprpg.steps.StepSourceType.fromId(it) }
    if (target != null) {
        AlertDialog(
            onDismissRequest = { pendingSource = null },
            title = { WithAppFontScale { Text("Changer de source de pas ?", fontSize = 15.sp) } },
            text = {
                WithAppFontScale {
                    Text(
                        "La source active devient ${target.label}. Les pas déjà comptés aujourd'hui sont " +
                            "conservés : le premier import de la nouvelle source sert de base et ne " +
                            "recrédite rien. Les jours suivants comptent normalement.",
                        fontSize = 9.sp
                    )
                }
            },
            confirmButton = {
                WithAppFontScale {
                    TextButton(onClick = {
                        val id = pendingSource
                        pendingSource = null
                        if (id != null) vm.setStepSource(id)
                    }) { Text("CHANGER", color = Gold) }
                }
            },
            dismissButton = {
                WithAppFontScale {
                    TextButton(onClick = { pendingSource = null }) { Text("ANNULER", color = Muted) }
                }
            },
            containerColor = Panel2
        )
    }

    if (diagnosticOpen) {
        SyncDiagnosticDialog(
            vm = vm,
            ui = ui,
            state = state,
            onClose = { diagnosticOpen = false }
        )
    }
}

@Composable
private fun SourceRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(if (selected) Gold.copy(alpha = 0.10f) else Panel3.copy(alpha = 0.35f))
            .border(
                1.dp,
                if (selected) Gold.copy(alpha = 0.55f) else Line.copy(alpha = 0.5f),
                RoundedCornerShape(4.dp)
            )
            .clickable(enabled = enabled && !selected) { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(9.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(if (selected) Gold else Color.Transparent)
                .border(1.dp, if (enabled) (if (selected) Gold else Muted2) else Muted2.copy(alpha = 0.4f), RoundedCornerShape(5.dp))
        )
        Spacer(Modifier.width(7.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = if (enabled) Parchment else Muted2
            )
            Text(subtitle, fontSize = 6.4.sp, color = Muted)
        }
        if (selected) Text("ACTIVE", fontFamily = Press, fontSize = 5.5.sp, color = Gold)
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

private fun lastCheckLabel(timestamp: Long): String {
    if (timestamp <= 0L) return "Dernière vérification : jamais"
    val formatter = java.time.format.DateTimeFormatter
        .ofPattern("dd/MM/yyyy HH:mm")
        .withZone(java.time.ZoneId.systemDefault())
    val value = formatter.format(java.time.Instant.ofEpochMilli(timestamp))
    return "Dernière vérification : $value"
}

@Composable
private fun ChoiceChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (active) Gold.copy(alpha = 0.18f) else Panel2)
            .border(1.dp, if (active) Gold.copy(alpha = 0.7f) else Line, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
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

/** Durée indicative à 60 pas/s, puis 30 pas/s quand la réserve est presque vide. */
private fun offlineDurationLabel(seconds: Int): String {
    val value = seconds.coerceAtLeast(0)
    return when {
        value < 60 -> "$value s"
        value % 60 == 0 -> "${value / 60} min"
        else -> "${value / 60} min ${value % 60} s"
    }
}
