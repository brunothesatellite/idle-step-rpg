package com.brunothesatellite.idlesteprpg

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brunothesatellite.idlesteprpg.ui.CampOverlay
import com.brunothesatellite.idlesteprpg.ui.ChestDialog
import com.brunothesatellite.idlesteprpg.ui.Dashboard
import com.brunothesatellite.idlesteprpg.ui.GarminScreen
import com.brunothesatellite.idlesteprpg.ui.Ink
import com.brunothesatellite.idlesteprpg.ui.ItemDialog
import com.brunothesatellite.idlesteprpg.ui.JournalOverlay
import com.brunothesatellite.idlesteprpg.ui.MarcheTheme
import com.brunothesatellite.idlesteprpg.ui.OfflineOverlay
import com.brunothesatellite.idlesteprpg.ui.Scene

class MainActivity : ComponentActivity() {

    private val container get() = (application as App).container
    private val vm: GameViewModel by viewModels { GameViewModel.Factory(container) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            MarcheTheme {
                GameRoot(vm)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.onAppResume()
    }

    override fun onPause() {
        vm.onAppPause()
        super.onPause()
    }
}

@Composable
private fun GameRoot(vm: GameViewModel) {
    val state by vm.game.collectAsStateWithLifecycle()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        vm.toasts.collect { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler(
        enabled = ui.campOpen || ui.settingsOpen || ui.selectedItem != null ||
            ui.chestOpen || ui.journalOpen || state.offlineReport != null
    ) {
        when {
            state.offlineReport != null -> vm.dismissOfflineReport()
            ui.selectedItem != null -> vm.dismissItem()
            ui.chestOpen -> vm.closeChest()
            ui.journalOpen -> vm.closeJournal()
            ui.campOpen -> vm.closeCamp()
            ui.settingsOpen -> vm.closeSettings()
        }
    }

    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().background(Ink)) {
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
                modifier = Modifier.weight(0.46f)
            )
            Dashboard(
                state = state,
                ui = ui,
                onSync = { vm.syncNow() },
                onOpenSettings = vm::openSettings,
                onOpenCamp = vm::openCamp,
                onOpenJournal = vm::openJournal,
                onBuyUpgrade = vm::buyUpgrade,
                onSelectItem = vm::selectItem,
                onSelectTab = {},
                modifier = Modifier.weight(0.54f)
            )
        }

        if (ui.settingsOpen) {
            GarminScreen(vm = vm, ui = ui, onClose = vm::closeSettings)
        }
        if (ui.campOpen) {
            CampOverlay(
                state = state,
                onRest = vm::rest,
                onShard = vm::shardHeal,
                onResume = vm::closeCamp
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
                onSell = { vm.sell(item) }
            )
        }
        if (ui.journalOpen) {
            JournalOverlay(journal = state.journal, onClose = vm::closeJournal)
        }
        state.offlineReport?.let { report ->
            OfflineOverlay(report = report, onDismiss = vm::dismissOfflineReport)
        }
    }
}


