package com.brunothesatellite.idlesteprpg.ui

import android.Manifest
import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.LocaleList
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt
import com.brunothesatellite.idlesteprpg.BuildConfig
import com.brunothesatellite.idlesteprpg.GameViewModel
import com.brunothesatellite.idlesteprpg.R
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
    val reportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) vm.exportBalanceReportTo(uri) }
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
                Text(
                    stringResource(R.string.settings_title),
                    fontFamily = Press,
                    fontSize = 9.sp,
                    color = GoldLight
                )
                Text(
                    stringResource(R.string.settings_subtitle),
                    fontSize = 7.sp,
                    color = Muted
                )
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
            LanguageCard(context)
            Spacer(Modifier.height(8.dp))
            PixelCard(Modifier.fillMaxWidth()) {
                SectionLabel(stringResource(R.string.settings_display))
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.settings_text_size), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Parchment)
                Spacer(Modifier.height(5.dp))
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf(
                        stringResource(R.string.settings_text_normal) to 1.0f,
                        stringResource(R.string.settings_text_large) to 1.35f,
                        stringResource(R.string.settings_text_xlarge) to 1.6f,
                        stringResource(R.string.settings_text_max) to 2.0f
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
                    stringResource(R.string.settings_notifications),
                    stringResource(R.string.settings_notifications_desc),
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
                    ToggleRow(stringResource(R.string.settings_notify_boss), stringResource(R.string.settings_notify_boss_desc), ui.notifyBoss) {
                        vm.setNotifyBoss(it)
                    }
                    ToggleRow(
                        stringResource(R.string.settings_notify_special),
                        stringResource(R.string.settings_notify_special_desc),
                        ui.notifySpecial
                    ) { vm.setNotifySpecial(it) }
                    ToggleRow(
                        stringResource(R.string.settings_notify_boss_waiting),
                        stringResource(R.string.settings_notify_boss_waiting_desc),
                        ui.notifyBossWaiting
                    ) { vm.setNotifyBossWaiting(it) }
                    ToggleRow(stringResource(R.string.settings_notify_level), stringResource(R.string.settings_notify_level_desc), ui.notifyLevel) {
                        vm.setNotifyLevel(it)
                    }
                    ToggleRow(stringResource(R.string.settings_notify_oaths), stringResource(R.string.settings_notify_oaths_desc), ui.notifyOaths) {
                        vm.setNotifyOaths(it)
                    }
                    ToggleRow(stringResource(R.string.settings_notify_chest), stringResource(R.string.settings_notify_chest_desc), ui.notifyChest) {
                        vm.setNotifyChest(it)
                    }
                    ToggleRow(stringResource(R.string.settings_notify_key), stringResource(R.string.settings_notify_key_desc), ui.notifyKey) {
                        vm.setNotifyKey(it)
                    }
                    ToggleRow(
                        stringResource(R.string.settings_notify_sync),
                        stringResource(R.string.settings_notify_sync_desc),
                        ui.notifySync
                    ) { vm.setNotifySync(it) }
                    ToggleRow(stringResource(R.string.settings_quiet_hours), stringResource(R.string.settings_quiet_hours_desc), ui.quietHours) {
                        vm.setQuietHours(it)
                    }
                    val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    if (!granted) {
                        Text(
                            stringResource(R.string.settings_notifications_denied),
                            fontSize = 6.5.sp,
                            color = Blood,
                            lineHeight = 9.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        OverlayAction(
                            stringResource(R.string.settings_open_system_settings),
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
                    stringResource(R.string.settings_reduced_motion),
                    stringResource(R.string.settings_reduced_motion_desc),
                    ui.reducedMotion
                ) { vm.setReducedMotion(it) }
                ToggleRow(
                    stringResource(R.string.settings_vibrations),
                    stringResource(R.string.settings_vibrations_desc),
                    ui.vibrations
                ) { vm.setVibrations(it) }
                Spacer(Modifier.height(4.dp))
                OverlayAction(
                    stringResource(R.string.settings_replay_onboarding),
                    Modifier.fillMaxWidth(),
                    onClick = { vm.restartOnboarding() }
                )
            }
            Spacer(Modifier.height(10.dp))
            PixelCard(Modifier.fillMaxWidth()) {
                SectionLabel(stringResource(R.string.settings_sound))
                Spacer(Modifier.height(5.dp))
                ToggleRow(
                    stringResource(R.string.settings_sound_8bit),
                    stringResource(R.string.settings_sound_8bit_desc),
                    ui.soundsEnabled
                ) { vm.setSoundsEnabled(it) }
                if (ui.soundsEnabled) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.settings_volume), fontSize = 8.sp, color = Parchment)
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
                        stringResource(R.string.settings_test_sound),
                        Modifier.fillMaxWidth(),
                        onClick = { vm.previewSound() }
                    )
                }
                Spacer(Modifier.height(9.dp))
                DividerLine()
                Spacer(Modifier.height(7.dp))
                ToggleRow(
                    stringResource(R.string.settings_music),
                    stringResource(R.string.settings_music_desc),
                    ui.musicEnabled
                ) { vm.setMusicEnabled(it) }
                if (ui.musicEnabled) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.settings_music_volume), fontSize = 8.sp, color = Parchment)
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
                        stringResource(R.string.settings_music_note),
                        fontSize = 6.2.sp,
                        color = Muted2,
                        lineHeight = 9.sp
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            if (!BuildConfig.PLAY_DISTRIBUTION) {
            PixelCard(Modifier.fillMaxWidth()) {
                SectionLabel(stringResource(R.string.settings_updates))
                Spacer(Modifier.height(5.dp))
                ToggleRow(
                    stringResource(R.string.settings_auto_update),
                    stringResource(R.string.settings_auto_update_desc),
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
                    if (ui.updateChecking) stringResource(R.string.settings_checking) else stringResource(R.string.settings_check_now),
                    Modifier.fillMaxWidth(),
                    onClick = { vm.checkForUpdate(manual = true) },
                    enabled = !ui.updateChecking
                )
                ui.availableUpdate?.let { info ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.settings_update_available, info.tag),
                        fontSize = 7.sp,
                        color = GoldLight
                    )
                    Spacer(Modifier.height(4.dp))
                    OverlayAction(
                        stringResource(R.string.settings_update_install, info.tag),
                        Modifier.fillMaxWidth(),
                        onClick = vm::installUpdate,
                        primary = true
                    )
                    Spacer(Modifier.height(4.dp))
                    OverlayAction(
                        stringResource(R.string.settings_view_release),
                        Modifier.fillMaxWidth(),
                        onClick = { vm.openRelease(info.releaseUrl) }
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.settings_update_note),
                    fontSize = 6.5.sp,
                    color = Muted2,
                    lineHeight = 9.sp
                )
            }
            }
            Spacer(Modifier.height(10.dp))
            PixelCard(Modifier.fillMaxWidth()) {
                SectionLabel(stringResource(R.string.settings_help))
                Spacer(Modifier.height(5.dp))
                OverlayAction(
                    stringResource(R.string.settings_open_manual),
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
                SectionLabel(stringResource(R.string.settings_inventory))
                Spacer(Modifier.height(5.dp))
                ToggleRow(
                    stringResource(R.string.settings_auto_sell),
                    stringResource(R.string.settings_auto_sell_desc),
                    ui.autoSellCommons
                ) { vm.setAutoSellCommons(it) }
            }
            Spacer(Modifier.height(10.dp))
            PixelCard(Modifier.fillMaxWidth()) {
                SectionLabel(stringResource(R.string.settings_save))
                Spacer(Modifier.height(5.dp))
                Text(
                    stringResource(R.string.settings_save_note),
                    fontSize = 6.8.sp,
                    color = Muted,
                    lineHeight = 10.sp
                )
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    OverlayAction(
                        stringResource(R.string.settings_export),
                        Modifier.weight(1f),
                        onClick = { exportLauncher.launch("marche-ames-sauvegarde.json") }
                    )
                    OverlayAction(
                        stringResource(R.string.settings_import),
                        Modifier.weight(1f),
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }
                    )
                }
                Spacer(Modifier.height(5.dp))
                OverlayAction(
                    stringResource(R.string.settings_export_report),
                    Modifier.fillMaxWidth(),
                    onClick = { reportLauncher.launch("marche-ames-rapport-equilibrage.json") }
                )
                Spacer(Modifier.height(5.dp))
                OverlayAction(
                    stringResource(R.string.settings_clear_progress),
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
                SectionLabel(stringResource(R.string.settings_step_source))
                Text(
                    ui.stepStatus,
                    fontSize = 7.5.sp,
                    color = if (ui.connected) Green else Ember
                )
                Spacer(Modifier.height(6.dp))
                SourceRow(
                    title = stringResource(R.string.settings_garmin),
                    subtitle = stringResource(R.string.settings_garmin_desc),
                    selected = ui.stepSource == "garmin",
                    onClick = { if (ui.stepSource != "garmin") pendingSource = "garmin" }
                )
                Spacer(Modifier.height(4.dp))
                SourceRow(
                    title = stringResource(R.string.settings_health),
                    subtitle = if (ui.healthAvailable) stringResource(R.string.settings_health_desc)
                    else stringResource(R.string.settings_health_unavailable),
                    selected = ui.stepSource == "health",
                    enabled = ui.healthAvailable,
                    onClick = { if (ui.stepSource != "health") pendingSource = "health" }
                )
                Spacer(Modifier.height(6.dp))
                if (ui.stepSource == "garmin") {
                    val keyDays = ui.keyExpiry?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) }
                    if (keyDays != null && keyDays < 30) {
                        Text(
                            if (keyDays < 0) stringResource(R.string.settings_key_expired)
                            else stringResource(R.string.settings_key_expires, keyDays),
                            fontSize = 6.8.sp,
                            color = Blood
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    if (ui.connected) {
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            OverlayAction(
                                stringResource(R.string.settings_sync),
                                Modifier.weight(1f),
                                { vm.syncNow() },
                                enabled = !ui.syncing
                            )
                            OverlayAction(stringResource(R.string.settings_manage_key), Modifier.weight(1f), { vm.openSettings() })
                        }
                    } else {
                        Text(
                            stringResource(R.string.settings_garmin_missing),
                            fontSize = 7.sp,
                            color = Muted,
                            lineHeight = 10.sp
                        )
                        Spacer(Modifier.height(5.dp))
                        OverlayAction(
                            stringResource(R.string.settings_connect_garmin),
                            Modifier.fillMaxWidth(),
                            { vm.openSettings() },
                            primary = true
                        )
                    }
                } else {
                    Text(
                        stringResource(R.string.settings_health_note),
                        fontSize = 7.sp,
                        color = Muted,
                        lineHeight = 10.sp
                    )
                    Spacer(Modifier.height(5.dp))
                    if (!ui.healthPermission) {
                        OverlayAction(
                            stringResource(R.string.settings_authorize_health),
                            Modifier.fillMaxWidth(),
                            { healthLauncher.launch(setOf(vm.healthPermissionName())) },
                            primary = true
                        )
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            OverlayAction(
                                stringResource(R.string.settings_sync),
                                Modifier.weight(1f),
                                { vm.syncNow() },
                                enabled = !ui.syncing
                            )
                            OverlayAction(
                                stringResource(R.string.settings_authorization),
                                Modifier.weight(1f),
                                { healthLauncher.launch(setOf(vm.healthPermissionName())) }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            PixelCard(Modifier.fillMaxWidth()) {
                SectionLabel(stringResource(R.string.settings_diagnostic))
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.settings_diagnostic_desc),
                    fontSize = 6.8.sp,
                    color = Muted,
                    lineHeight = 10.sp
                )
                Spacer(Modifier.height(6.dp))
                OverlayAction(
                    stringResource(R.string.settings_open_diagnostic),
                    Modifier.fillMaxWidth(),
                    onClick = { diagnosticOpen = true }
                )
            }
            Spacer(Modifier.height(10.dp))
            PixelCard(Modifier.fillMaxWidth()) {
                SectionLabel(stringResource(R.string.settings_sync_log))
                Spacer(Modifier.height(4.dp))
                if (ui.syncLog.isEmpty()) {
                    Text(stringResource(R.string.settings_sync_log_empty), fontSize = 7.sp, color = Muted2)
                } else {
                    ui.syncLog.forEach { line ->
                        val shown = localizedText(line)
                        Text(
                            shown,
                            fontSize = 6.8.sp,
                            color = if (shown.contains("ERROR") || shown.contains("ERREUR")) Blood else Muted,
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
                        SectionLabel(stringResource(R.string.settings_advanced))
                        Text(stringResource(R.string.settings_advanced_subtitle), fontSize = 6.5.sp, color = Muted2)
                    }
                }
                if (advancedOpen) {
                    Spacer(Modifier.height(6.dp))
                    DividerLine()
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.settings_offline_title),
                        fontFamily = Press,
                        fontSize = 6.5.sp,
                        letterSpacing = 1.sp,
                        color = GoldLight
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        stringResource(R.string.settings_offline_desc),
                        fontSize = 6.6.sp,
                        color = Muted,
                        lineHeight = 10.sp
                    )
                    Spacer(Modifier.height(7.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        ChoiceChip(stringResource(R.string.settings_percent), offlineMode == OfflineMode.PERCENT) {
                            vm.setOfflineMode("percent")
                        }
                        ChoiceChip(stringResource(R.string.settings_max_steps), offlineMode == OfflineMode.STEPS) {
                            vm.setOfflineMode("steps")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            if (offlineMode == OfflineMode.PERCENT) stringResource(R.string.settings_percent_of_steps) else stringResource(R.string.settings_offline_steps),
                            fontSize = 8.sp,
                            color = Parchment
                        )
                        Text(
                            if (offlineMode == OfflineMode.PERCENT) "${ui.offlinePercent} %"
                            else stringResource(R.string.settings_steps_unit, ui.offlineMaxSteps),
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
                        if (dailyBudget <= 0) stringResource(R.string.settings_offline_disabled)
                        else stringResource(
                            R.string.settings_offline_simulation,
                            offlineDurationLabel(dailyBudget / 60),
                            offlineDurationLabel(dailyBudget / 30)
                        ),
                        fontSize = 6.4.sp,
                        color = Muted
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (dailyBudget <= 0) stringResource(R.string.settings_today_none)
                        else stringResource(
                            R.string.settings_today_offline,
                            dailyBudget,
                            remainingBudget,
                            state.offlineStepsToday
                        ),
                        fontSize = 6.4.sp,
                        color = Muted2
                    )
                    Spacer(Modifier.height(7.dp))
                    OverlayAction(stringResource(R.string.settings_reset_30), Modifier.fillMaxWidth(), onClick = { vm.resetOfflinePolicy() })
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.settings_offline_note),
                        fontSize = 6.2.sp,
                        color = Muted2,
                        lineHeight = 9.sp
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            PixelCard(Modifier.fillMaxWidth(), borderColor = Gold.copy(alpha = 0.3f)) {
                SectionLabel(stringResource(R.string.settings_about))
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.settings_about_version, BuildConfig.VERSION_NAME),
                    fontSize = 8.sp,
                    color = Parchment
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    stringResource(R.string.settings_about_text),
                    fontSize = 6.8.sp,
                    color = Muted,
                    lineHeight = 10.sp
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    OverlayAction(
                        stringResource(R.string.settings_licenses),
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
                        stringResource(R.string.settings_privacy),
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
                    stringResource(R.string.settings_license_note),
                    fontSize = 6.5.sp,
                    color = Muted2
                )
            }
            Spacer(Modifier.height(10.dp))
            BalanceTelemetryCard(state)
            if (BuildConfig.DEBUG) {
                Spacer(Modifier.height(10.dp))
                OverlayAction(
                    stringResource(R.string.settings_debug_add_steps),
                    Modifier.fillMaxWidth(),
                    onClick = { vm.debugAddReserveSteps(1000) }
                )
                Spacer(Modifier.height(6.dp))
                OverlayAction(
                    stringResource(R.string.settings_debug_ending),
                    Modifier.fillMaxWidth(),
                    onClick = { vm.openEndingPreview() }
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { WithAppFontScale { Text(stringResource(R.string.settings_clear_title), fontSize = 15.sp) } },
            text = {
                WithAppFontScale {
                    Text(
                        stringResource(R.string.settings_clear_text),
                        fontSize = 9.sp
                    )
                }
            },
            confirmButton = {
                WithAppFontScale {
                    TextButton(onClick = {
                        confirmClear = false
                        vm.clearProgress()
                    }) { Text(stringResource(R.string.settings_delete), color = Blood) }
                }
            },
            dismissButton = {
                WithAppFontScale {
                    TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.settings_cancel), color = Muted) }
                }
            },
            containerColor = Panel2
        )
    }

    val target = pendingSource?.let { com.brunothesatellite.idlesteprpg.steps.StepSourceType.fromId(it) }
    if (target != null) {
        AlertDialog(
            onDismissRequest = { pendingSource = null },
            title = { WithAppFontScale { Text(stringResource(R.string.settings_change_source_title), fontSize = 15.sp) } },
            text = {
                WithAppFontScale {
                    Text(
                        stringResource(
                            R.string.settings_change_source_text,
                            sourceLabelRes(target.id)
                        ),
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
                    }) { Text(stringResource(R.string.settings_change), color = Gold) }
                }
            },
            dismissButton = {
                WithAppFontScale {
                    TextButton(onClick = { pendingSource = null }) { Text(stringResource(R.string.settings_cancel), color = Muted) }
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
        if (selected) Text(stringResource(R.string.settings_active), fontFamily = Press, fontSize = 5.5.sp, color = Gold)
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

@Composable
private fun lastCheckLabel(timestamp: Long): String {
    if (timestamp <= 0L) return stringResource(R.string.settings_last_check_never)
    val formatter = java.time.format.DateTimeFormatter
        .ofPattern("dd/MM/yyyy HH:mm")
        .withZone(java.time.ZoneId.systemDefault())
    val value = formatter.format(java.time.Instant.ofEpochMilli(timestamp))
    return stringResource(R.string.settings_last_check, value)
}

@Composable
private fun LanguageCard(context: Context) {
    val manager = context.getSystemService(LocaleManager::class.java) ?: return
    val current = manager.applicationLocales
    val currentTag = if (current.isEmpty) "system" else current[0].language

    fun apply(tag: String?) {
        manager.applicationLocales =
            if (tag == null) LocaleList.getEmptyLocaleList()
            else LocaleList.forLanguageTags(tag)
    }

    PixelCard(Modifier.fillMaxWidth()) {
        SectionLabel(stringResource(R.string.settings_language_title))
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.settings_language_hint),
            fontSize = 6.2.sp,
            color = Muted
        )
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            ChoiceChip(
                stringResource(R.string.settings_language_system),
                currentTag == "system",
                Modifier.weight(1f)
            ) { apply(null) }
            ChoiceChip(
                stringResource(R.string.settings_language_french),
                currentTag == "fr",
                Modifier.weight(1f)
            ) { apply("fr") }
            ChoiceChip(
                stringResource(R.string.settings_language_english),
                currentTag == "en",
                Modifier.weight(1f)
            ) { apply("en") }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.settings_language_partial),
            fontSize = 5.8.sp,
            color = Muted2,
            lineHeight = 8.sp
        )
    }
}

@Composable
private fun ChoiceChip(
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier
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

@Composable
private fun BalanceTelemetryCard(state: GameState) {
    val telemetry = state.telemetry
    val focusFloor = telemetry.bossAttempts.keys.maxOrNull() ?: state.floor
    val attempts = telemetry.bossAttempts[focusFloor] ?: 0
    val wins = telemetry.bossWinsByFloor[focusFloor] ?: 0
    val average = if (telemetry.combatCount > 0) {
        telemetry.combatMs / 1000.0 / telemetry.combatCount
    } else {
        0.0
    }
    PixelCard(Modifier.fillMaxWidth(), borderColor = Blue.copy(alpha = 0.3f)) {
        SectionLabel(stringResource(R.string.settings_balance_telemetry), color = Blue)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(
                R.string.settings_telemetry_normal,
                telemetry.normalFights,
                telemetry.normalWins
            ),
            fontSize = 6.4.sp,
            color = Parchment
        )
        Text(
            stringResource(
                R.string.settings_telemetry_boss,
                telemetry.bossFights,
                telemetry.bossWins
            ),
            fontSize = 6.4.sp,
            color = Parchment
        )
        Text(
            stringResource(R.string.settings_telemetry_floor, focusFloor, attempts, wins),
            fontSize = 6.4.sp,
            color = Parchment
        )
        Text(
            stringResource(
                R.string.settings_telemetry_time,
                decimalRes(average, 1),
                telemetry.combatCount
            ),
            fontSize = 6.4.sp,
            color = Muted
        )
        Text(
            stringResource(R.string.settings_telemetry_progress, state.floor - 1, state.kills),
            fontSize = 6.4.sp,
            color = Muted
        )
        Text(
            stringResource(
                R.string.settings_telemetry_resources,
                state.rations,
                state.shards,
                formatCompact(state.gold)
            ),
            fontSize = 6.4.sp,
            color = Muted
        )
        Text(
            stringResource(R.string.settings_telemetry_note),
            fontSize = 5.9.sp,
            color = Muted2
        )
    }
}
