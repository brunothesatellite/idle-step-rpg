package com.brunothesatellite.idlesteprpg

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brunothesatellite.idlesteprpg.audio.SoundFx
import com.brunothesatellite.idlesteprpg.notify.GameNotifications
import com.brunothesatellite.idlesteprpg.ui.CampOverlay
import com.brunothesatellite.idlesteprpg.ui.ChestDialog
import com.brunothesatellite.idlesteprpg.ui.ConsumablesOverlay
import com.brunothesatellite.idlesteprpg.ui.CodexOverlay
import com.brunothesatellite.idlesteprpg.ui.Dashboard
import com.brunothesatellite.idlesteprpg.ui.DayBanner
import com.brunothesatellite.idlesteprpg.ui.EndingOverlay
import com.brunothesatellite.idlesteprpg.ui.endingPreviewState
import com.brunothesatellite.idlesteprpg.ui.GarminScreen
import com.brunothesatellite.idlesteprpg.ui.GuardianGateDialog
import com.brunothesatellite.idlesteprpg.ui.ImportPreviewDialog
import com.brunothesatellite.idlesteprpg.ui.Ink
import com.brunothesatellite.idlesteprpg.ui.ItemDialog
import com.brunothesatellite.idlesteprpg.ui.StashItemDialog
import com.brunothesatellite.idlesteprpg.ui.JournalOverlay
import com.brunothesatellite.idlesteprpg.ui.MarcheTheme
import com.brunothesatellite.idlesteprpg.ui.OfflineOverlay
import com.brunothesatellite.idlesteprpg.ui.OnboardingOverlay
import com.brunothesatellite.idlesteprpg.ui.PostUpdateDialog
import com.brunothesatellite.idlesteprpg.ui.ResumeOverlay
import com.brunothesatellite.idlesteprpg.ui.Scene
import com.brunothesatellite.idlesteprpg.ui.SettingsOverlay
import com.brunothesatellite.idlesteprpg.ui.SpecialDialog
import com.brunothesatellite.idlesteprpg.ui.SpecialResultDialog
import com.brunothesatellite.idlesteprpg.ui.UpdateDialog

class MainActivity : ComponentActivity() {

    private val container get() = (application as App).container
    private val vm: GameViewModel by viewModels { GameViewModel.Factory(container) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.brunothesatellite.idlesteprpg.ui.AppLocale.updateFrom(
            resources.configuration.locales[0]
        )
        com.brunothesatellite.idlesteprpg.notify.GameNotifications.ensureChannels(this)
        com.brunothesatellite.idlesteprpg.widget.MarcheWidgetProvider.updateAll(this)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            GameRoot(vm)
        }
        vm.openScreen(intent?.getStringExtra("open_screen"))
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        vm.openScreen(intent.getStringExtra("open_screen"))
    }

    override fun onResume() {
        super.onResume()
        GameNotifications.appVisible = true
        vm.onAppResume()
    }

    override fun onPause() {
        GameNotifications.appVisible = false
        vm.onAppPause()
        super.onPause()
    }
}

@Composable
private fun GameRoot(vm: GameViewModel) {
    val state by vm.game.collectAsStateWithLifecycle()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var activeTab by rememberSaveable { mutableStateOf("stats") }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(ui.onboardingDone) {
        if (ui.onboardingDone && ui.notificationsEnabled &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(Unit) {
        var toast: android.widget.Toast? = null
        vm.toasts.collect { message ->
            toast?.cancel()
            toast = android.widget.Toast.makeText(
                context,
                message,
                android.widget.Toast.LENGTH_SHORT
            ).also { it.show() }
        }
    }

    var previousChests by remember { mutableStateOf(state.chestsOpened) }
    LaunchedEffect(state.chestsOpened) {
        if (state.chestsOpened > previousChests) {
            SoundFx.play(context, "chest", 0.9f)
        }
        previousChests = state.chestsOpened
    }

    var previousFloor by remember { mutableStateOf(state.floor) }
    LaunchedEffect(state.floor) {
        if (state.floor > previousFloor) {
            SoundFx.play(context, "victory", 0.9f)
        }
        previousFloor = state.floor
    }

    var previousMode by remember { mutableStateOf(state.mode) }
    LaunchedEffect(state.mode) {
        if (state.mode == "farmWait" && previousMode != "farmWait") {
            SoundFx.play(context, "defeat", 0.85f)
        }
        previousMode = state.mode
    }

    // Un coffre peut disparaître ou être ouvert pendant que sa fenêtre est
    // encore marquée ouverte. Sans ce garde-fou, le drapeau restait vrai et
    // masquait le portail du gardien alors que le héros était bloqué à 90 m.
    LaunchedEffect(state.chest) {
        if (state.chest == null && ui.chestOpen) vm.closeChest()
    }

    BackHandler(
        enabled = ui.resume != null || ui.campOpen || ui.settingsOpen || ui.prefsOpen ||
            ui.selectedItem != null || ui.chestOpen || ui.journalOpen ||
            state.offlineReport?.acknowledgedAt == 0L || state.special != null ||
            ui.pendingImport != null || !ui.onboardingDone || ui.specialOutcome != null ||
            ui.consumablesOpen || ui.availableUpdate != null || ui.postUpdateTag != null ||
            ui.codexOpen
    ) {
        when {
            ui.resume != null -> Unit
            !ui.onboardingDone -> vm.completeOnboarding()
            ui.availableUpdate != null -> vm.dismissUpdate()
            ui.postUpdateTag != null -> vm.dismissPostUpdate()
            ui.codexOpen -> vm.closeCodex()
            ui.pendingImport != null -> vm.cancelImport()
            ui.specialOutcome != null -> vm.dismissSpecialOutcome()
            ui.consumablesOpen -> vm.closeConsumables()
            state.special != null -> vm.resolveSpecial(false)
            state.offlineReport?.acknowledgedAt == 0L -> vm.dismissOfflineReport()
            ui.selectedItem != null -> vm.dismissItem()
            ui.chestOpen -> vm.closeChest()
            ui.journalOpen -> vm.closeJournal()
            ui.settingsOpen -> vm.closeSettings()
            ui.prefsOpen -> vm.closePreferences()
            ui.campOpen -> vm.closeCamp()
        }
    }

    MarcheTheme(fontScale = ui.textScale) {
        androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().background(Ink)) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val wide = maxWidth >= 620.dp || maxWidth > maxHeight
                if (wide) {
                    Row(
                        Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                    ) {
                        Scene(
                            state = state,
                            onCamp = vm::openCamp,
                            onChest = vm::openChest,
                            onConsumables = vm::openConsumables,
                            onSync = vm::syncNow,
                            onTogglePause = vm::togglePause,
                            connected = ui.connected,
                            syncing = ui.syncing,
                            lastSync = ui.lastSync,
                            lastError = ui.lastError,
                            reducedMotion = ui.reducedMotion,
                            vibrations = ui.vibrations,
                            paused = ui.paused,
                            modifier = Modifier.weight(0.58f).fillMaxHeight()
                        )
                        Dashboard(
                            state = state,
                            ui = ui,
                            activeTab = activeTab,
                            onSync = { vm.syncNow() },
                            onOpenSettings = vm::openSettings,
                            onOpenCamp = vm::openCamp,
                            onOpenJournal = vm::openJournal,
                            onOpenPreferences = vm::openPreferences,
                            onBuyUpgrade = vm::buyUpgrade,
                            onSelectItem = vm::selectItem,
                            onSellCommons = vm::sellCommons,
                            onEquipBest = vm::equipBest,
                            onWithdrawStash = vm::withdrawStash,
                            onWithdrawStashItem = vm::withdrawStashItem,
                            onSelectStashItem = vm::selectStashItem,
                            onUseConsumable = vm::useConsumable,
                            onToggleFarm = vm::setHoldFloor,
                            onOpenCodex = vm::openCodex,
                            onSelectTab = { activeTab = it },
                            modifier = Modifier.weight(0.42f).fillMaxHeight()
                        )
                    }
                } else {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                    ) {
                        Scene(
                            state = state,
                            onCamp = vm::openCamp,
                            onChest = vm::openChest,
                            onConsumables = vm::openConsumables,
                            onSync = vm::syncNow,
                            onTogglePause = vm::togglePause,
                            connected = ui.connected,
                            syncing = ui.syncing,
                            lastSync = ui.lastSync,
                            lastError = ui.lastError,
                            reducedMotion = ui.reducedMotion,
                            vibrations = ui.vibrations,
                            paused = ui.paused,
                            modifier = Modifier.weight(0.46f)
                        )
                        Dashboard(
                            state = state,
                            ui = ui,
                            activeTab = activeTab,
                            onSync = { vm.syncNow() },
                            onOpenSettings = vm::openSettings,
                            onOpenCamp = vm::openCamp,
                            onOpenJournal = vm::openJournal,
                            onOpenPreferences = vm::openPreferences,
                            onBuyUpgrade = vm::buyUpgrade,
                            onSelectItem = vm::selectItem,
                            onSellCommons = vm::sellCommons,
                            onEquipBest = vm::equipBest,
                            onWithdrawStash = vm::withdrawStash,
                            onWithdrawStashItem = vm::withdrawStashItem,
                            onSelectStashItem = vm::selectStashItem,
                            onUseConsumable = vm::useConsumable,
                            onToggleFarm = vm::setHoldFloor,
                            onOpenCodex = vm::openCodex,
                            onSelectTab = { activeTab = it },
                            modifier = Modifier.weight(0.54f)
                        )
                    }
                }
            }

            ui.dayBanner?.let { banner ->
                DayBanner(
                    text = banner,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 10.dp)
                )
            }

            if (ui.prefsOpen) {
                SettingsOverlay(vm = vm, ui = ui, state = state, onClose = vm::closePreferences)
            }
            // GarminScreen est dessiné après les réglages pour s'empiler par-dessus
            // (on y accède depuis la section « Compte Garmin »).
            if (ui.settingsOpen) {
                GarminScreen(vm = vm, ui = ui, onClose = vm::closeSettings)
            }
            if (ui.codexOpen) {
                CodexOverlay(state = state, onClose = vm::closeCodex)
            }
            if (ui.campOpen) {
                CampOverlay(
                    state = state,
                    onRest = vm::rest,
                    onShard = vm::shardHeal,
                    onFreeRest = vm::freeRest,
                    onWithdrawStash = vm::withdrawStash,
                    onOpenJournal = vm::openJournal,
                    onOpenInventory = {
                        vm.closeCamp()
                        activeTab = "inventory"
                    },
                    onOpenConsumables = vm::openConsumables,
                    onBuyConsumable = vm::buyCampConsumable,
                    onRespec = vm::respecCamp,
                    onResume = vm::closeCamp,
                    reducedMotion = ui.reducedMotion
                )
            }
            if (state.mode == "gate" && state.pendingBoss != null &&
                !ui.campOpen && !ui.settingsOpen && !ui.prefsOpen &&
                !ui.codexOpen && !ui.journalOpen && !(ui.chestOpen && state.chest != null) &&
                ui.selectedItem == null && ui.selectedStashItem == null && !state.endingPending && !ui.endingPreview
            ) {
                GuardianGateDialog(
                    state = state,
                    onEngage = vm::engageBoss,
                    onFarm = vm::farmFromGate,
                    onCamp = vm::openCamp,
                    onReroll = vm::rerollBossModifier
                )
            }
            if (state.endingPending || ui.endingPreview) {
                EndingOverlay(
                    state = if (ui.endingPreview) endingPreviewState(state) else state,
                    onContinue = {
                        if (ui.endingPreview) vm.closeEndingPreview() else vm.dismissEnding()
                    },
                    reducedMotion = ui.reducedMotion
                )
            }
            if (ui.chestOpen && state.chest != null) {
                ChestDialog(
                    chest = state.chest!!,
                    state = state,
                    onForce = vm::forceChest,
                    onStop = vm::stopForcing,
                    onDismiss = vm::closeChest
                )
            }
            ui.selectedItem?.let { item ->
                ItemDialog(
                    item = item,
                    state = state,
                    onDismiss = vm::dismissItem,
                    onEquip = { vm.equip(item) },
                    onSell = { vm.sell(item) },
                    onToggleLock = { vm.toggleLock(item) },
                    onFuse = { vm.fuse(item) },
                    onIdentify = { vm.identifyItem(item) },
                    onReroll = { vm.rerollEffect(item) },
                    onUpgrade = { vm.upgradeItem(item) },
                    onDismantle = { vm.dismantleItem(item) },
                    onSendToStash = { vm.sendToStash(item) }
                )
            }
            ui.selectedStashItem?.let { item ->
                StashItemDialog(
                    item = item,
                    state = state,
                    onDismiss = vm::dismissStashItem,
                    onTake = { vm.withdrawStashItem(item) },
                    onSell = { vm.sellStashItem(item) },
                    onDismantle = { vm.dismantleStashItem(item) }
                )
            }
            if (ui.journalOpen) {
                JournalOverlay(
                    journal = state.journal,
                    legend = state.legend,
                    bestiary = state.bestiary,
                    onClose = vm::closeJournal
                )
            }
            if (ui.consumablesOpen) {
                ConsumablesOverlay(
                    state = state,
                    onUse = vm::useConsumable,
                    onClose = vm::closeConsumables
                )
            }
            state.special?.let { special ->
                SpecialDialog(
                    special = special,
                    state = state,
                    onAccept = { vm.resolveSpecial(true) },
                    onDecline = { vm.resolveSpecial(false) }
                )
            }
            ui.specialOutcome?.let { outcome ->
                SpecialResultDialog(
                    outcome = outcome,
                    onDismiss = vm::dismissSpecialOutcome
                )
            }
            if (ui.resume == null) {
                state.offlineReport?.takeIf { it.acknowledgedAt == 0L }?.let { report ->
                    OfflineOverlay(
                        report = report,
                        pendingSync = ui.resumePendingFetch,
                        onDismiss = vm::dismissOfflineReport,
                        onOpenJournal = {
                            vm.dismissOfflineReport()
                            vm.openJournal()
                        }
                    )
                }
            }
            ui.pendingImport?.let { summary ->
                ImportPreviewDialog(
                    summary = summary,
                    onConfirm = vm::confirmImport,
                    onCancel = vm::cancelImport
                )
            }
            if (!BuildConfig.PLAY_DISTRIBUTION) {
            ui.availableUpdate?.let { info ->
                UpdateDialog(
                    info = info,
                    currentVersion = BuildConfig.VERSION_NAME,
                    downloading = ui.updateDownloading,
                    progress = ui.updateProgress,
                    error = ui.updateError,
                    onInstall = vm::installUpdate,
                    onLater = vm::dismissUpdate,
                    onRelease = { vm.openRelease(info.releaseUrl) }
                )
            }
            if (ui.postUpdateTag != null) {
                PostUpdateDialog(
                    tag = ui.postUpdateTag!!,
                    onOpenRelease = vm::openPostUpdateRelease,
                    onDismiss = vm::dismissPostUpdate
                )
            }
            }
            if (!ui.onboardingDone) {
                OnboardingOverlay(
                    onFinish = vm::completeOnboarding,
                    onConnect = {
                        vm.completeOnboarding()
                        vm.openSettings()
                    },
                    onHealth = {
                        vm.completeOnboarding()
                        vm.setStepSource("health")
                        vm.openPreferences()
                    }
                )
            }
            ui.resume?.let { resume ->
                ResumeOverlay(
                    resume = resume,
                    reducedMotion = ui.reducedMotion,
                    onSkip = vm::skipResumeWait
                )
            }
        }
    }
}


