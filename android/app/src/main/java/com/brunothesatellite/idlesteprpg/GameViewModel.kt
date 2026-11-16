package com.brunothesatellite.idlesteprpg

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.brunothesatellite.idlesteprpg.engine.GameEngine
import com.brunothesatellite.idlesteprpg.engine.Item
import com.brunothesatellite.idlesteprpg.engine.Upgrades
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

data class UiState(
    val campOpen: Boolean = false,
    val settingsOpen: Boolean = false,
    val journalOpen: Boolean = false,
    val selectedItem: Item? = null,
    val chestOpen: Boolean = false,
    val syncing: Boolean = false,
    val connected: Boolean = false,
    val keyExpiry: LocalDate? = null,
    val tokenExpiry: Instant? = null,
    val lastSync: Long = 0L,
    val lastError: String = "",
    val lastAdded: Int = 0
)

class GameViewModel(private val container: AppContainer) : ViewModel() {

    val game = container.gameRepository.state

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private val _toasts = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toasts: SharedFlow<String> = _toasts.asSharedFlow()

    @Volatile private var visible = false
    private var initialized = false

    init {
        refreshGarmin()
        viewModelScope.launch {
            while (isActive) {
                if (visible && !_ui.value.campOpen && !_ui.value.settingsOpen) {
                    container.gameRepository.tick(100.0)
                }
                delay(100)
            }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(2000)
                container.gameRepository.save()
            }
        }
        viewModelScope.launch {
            delay(350)
            refreshGarmin()
            if (container.garminRepository.hasKey()) syncNow(silent = true)
        }
    }

    fun onAppResume() {
        visible = true
        refreshGarmin()
        if (!initialized) {
            initialized = true
            return
        }
        val last = _ui.value.lastSync
        if (container.garminRepository.hasKey() &&
            (last == 0L || System.currentTimeMillis() - last > 2 * 60_000L)
        ) {
            syncNow(silent = true)
        }
    }

    fun onAppPause() {
        visible = false
        viewModelScope.launch { container.gameRepository.save() }
    }

    fun refreshGarmin() {
        _ui.update {
            it.copy(
                connected = container.garminRepository.hasKey(),
                keyExpiry = container.garminRepository.keyExpiry(),
                tokenExpiry = container.garminRepository.tokenExpiry(),
                lastSync = container.garminRepository.lastSync(),
                lastError = container.garminRepository.lastError()
            )
        }
    }

    fun syncNow(silent: Boolean = false) {
        if (_ui.value.syncing) return
        if (!container.garminRepository.hasKey()) {
            _ui.update { it.copy(settingsOpen = true) }
            return
        }
        _ui.update { it.copy(syncing = true) }
        viewModelScope.launch {
            val result = container.garminRepository.sync()
            refreshGarmin()
            _ui.update { it.copy(syncing = false, lastAdded = result.added) }
            if (!silent) _toasts.emit(result.message)
        }
    }

    fun saveKey(rawKey: String, expiry: LocalDate) {
        _ui.update { it.copy(syncing = true) }
        viewModelScope.launch {
            val result = container.garminRepository.saveKey(rawKey, expiry)
            _ui.update { it.copy(syncing = false) }
            refreshGarmin()
            if (result.isSuccess) {
                _toasts.emit("Clé Garmin enregistrée.")
                syncNow(silent = true)
            } else {
                _toasts.emit(result.exceptionOrNull()?.message ?: "Clé Garmin refusée.")
            }
        }
    }

    fun updateExpiry(date: LocalDate) {
        container.garminRepository.setKeyExpiry(date)
        refreshGarmin()
        _toasts.tryEmit("Date d'expiration mise à jour.")
    }

    fun revokeGarmin() {
        container.garminRepository.revoke()
        refreshGarmin()
        _toasts.tryEmit("Clé Garmin supprimée de l'appareil.")
    }

    fun openCamp() { _ui.update { it.copy(campOpen = true) } }
    fun closeCamp() { _ui.update { it.copy(campOpen = false) } }
    fun openSettings() { _ui.update { it.copy(settingsOpen = true) } }
    fun closeSettings() { _ui.update { it.copy(settingsOpen = false) } }
    fun openJournal() { _ui.update { it.copy(journalOpen = true) } }
    fun closeJournal() { _ui.update { it.copy(journalOpen = false) } }

    fun dismissOfflineReport() {
        viewModelScope.launch {
            container.gameRepository.mutate { it.offlineReport = null }
        }
    }

    fun selectItem(item: Item) { _ui.update { it.copy(selectedItem = item) } }
    fun dismissItem() { _ui.update { it.copy(selectedItem = null) } }

    fun openChest() {
        if (game.value.chest != null) _ui.update { it.copy(chestOpen = true) }
    }

    fun closeChest() { _ui.update { it.copy(chestOpen = false) } }

    fun buyUpgrade(key: String) {
        viewModelScope.launch {
            var ok = false
            container.gameRepository.mutate { ok = GameEngine.buyUpgrade(it, key) }
            _toasts.tryEmit(if (ok) "Amélioration achetée." else "Or insuffisant.")
        }
    }

    fun equip(item: Item) {
        viewModelScope.launch {
            var ok = false
            container.gameRepository.mutate { ok = GameEngine.equipItem(it, item.id) }
            _ui.update { it.copy(selectedItem = null) }
            if (ok) _toasts.tryEmit("${item.name} équipé.")
        }
    }

    fun sell(item: Item) {
        viewModelScope.launch {
            var ok = false
            container.gameRepository.mutate { ok = GameEngine.sellItem(it, item.id) }
            _ui.update { it.copy(selectedItem = null) }
            if (ok) _toasts.tryEmit("${item.name} vendu.")
        }
    }

    fun rest() {
        viewModelScope.launch {
            var ok = false
            container.gameRepository.mutate { ok = GameEngine.rest(it) }
            _toasts.tryEmit(if (ok) "Repos au feu." else "Impossible de se reposer.")
        }
    }

    fun shardHeal() {
        viewModelScope.launch {
            var ok = false
            container.gameRepository.mutate { ok = GameEngine.shardHeal(it) }
            _toasts.tryEmit(if (ok) "PV restaurés." else "Impossible de se soigner.")
        }
    }

    fun forceChest() {
        viewModelScope.launch {
            var ok = false
            container.gameRepository.mutate { ok = GameEngine.forceChest(it) }
            if (!ok) _toasts.tryEmit("Terminez le combat avant de forcer le coffre.")
        }
    }

    fun stopForcing() {
        viewModelScope.launch {
            container.gameRepository.mutate { GameEngine.stopForcing(it) }
        }
    }

    fun debugAddSteps(steps: Int) {
        viewModelScope.launch { container.gameRepository.debugAddSteps(steps) }
    }

    fun upgrades(): Upgrades = game.value.upgrades

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            GameViewModel(container) as T
    }
}

