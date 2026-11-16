package com.brunothesatellite.idlesteprpg

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.brunothesatellite.idlesteprpg.data.SaveSummary
import com.brunothesatellite.idlesteprpg.engine.GameEngine
import com.brunothesatellite.idlesteprpg.engine.Item
import com.brunothesatellite.idlesteprpg.engine.Upgrades
import com.brunothesatellite.idlesteprpg.notify.GameNotifications
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

data class UiState(
    val campOpen: Boolean = false,
    val settingsOpen: Boolean = false,
    val prefsOpen: Boolean = false,
    val journalOpen: Boolean = false,
    val selectedItem: Item? = null,
    val chestOpen: Boolean = false,
    val syncing: Boolean = false,
    val connected: Boolean = false,
    val keyExpiry: LocalDate? = null,
    val tokenExpiry: Instant? = null,
    val lastSync: Long = 0L,
    val lastError: String = "",
    val lastAdded: Int = 0,
    val textScale: Float = 1.35f,
    val reducedMotion: Boolean = false,
    val vibrations: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val notifyBoss: Boolean = true,
    val notifyLevel: Boolean = true,
    val notifyOaths: Boolean = true,
    val notifyChest: Boolean = true,
    val notifyKey: Boolean = true,
    val quietHours: Boolean = false,
    val autoSellCommons: Boolean = false,
    val onboardingDone: Boolean = true,
    val syncLog: List<String> = emptyList(),
    val pendingImport: SaveSummary? = null,
    val dayBanner: String? = null,
    val updateTag: String? = null,
    val updateUrl: String? = null
)

class GameViewModel(private val container: AppContainer) : ViewModel() {

    val game = container.gameRepository.state

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private val _toasts = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toasts: SharedFlow<String> = _toasts.asSharedFlow()

    @Volatile private var visible = false
    private var initialized = false
    private var pendingImportText: String? = null
    private var lastKnownDay: String = ""

    init {
        _ui.update {
            it.copy(
                textScale = container.settings.textScale,
                reducedMotion = container.settings.reducedMotion,
                vibrations = container.settings.vibrations,
                notificationsEnabled = container.settings.notificationsEnabled,
                notifyBoss = container.settings.notifyBoss,
                notifyLevel = container.settings.notifyLevel,
                notifyOaths = container.settings.notifyOaths,
                notifyChest = container.settings.notifyChest,
                notifyKey = container.settings.notifyKey,
                quietHours = container.settings.quietHours,
                autoSellCommons = container.settings.autoSellCommons,
                onboardingDone = container.settings.onboardingDone
            )
        }
        lastKnownDay = game.value.day
        refreshGarmin()
        viewModelScope.launch {
            while (isActive) {
                val reportOpen = game.value.offlineReport?.let { it.acknowledgedAt == 0L } == true
                if (visible && !_ui.value.campOpen && !_ui.value.settingsOpen &&
                    !_ui.value.prefsOpen && !_ui.value.journalOpen && !reportOpen
                ) {
                    container.gameRepository.tick(100.0)
                }
                val today = game.value.day
                if (lastKnownDay.isNotBlank() && today.isNotBlank() && today != lastKnownDay) {
                    _ui.update { it.copy(dayBanner = "Nouveau jour · serments renouvelés") }
                    viewModelScope.launch {
                        delay(6_000)
                        _ui.update { it.copy(dayBanner = null) }
                    }
                }
                lastKnownDay = today
                delay(100)
            }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(5000)
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
        viewModelScope.launch {
            container.gameRepository.mutate { s ->
                val report = s.offlineReport
                if (report != null && report.acknowledgedAt == 0L) {
                    s.offlineReport = report.copy(acknowledgedAt = System.currentTimeMillis())
                }
            }
            container.gameRepository.save()
        }
    }

    fun refreshGarmin() {
        _ui.update {
            it.copy(
                connected = container.garminRepository.hasKey(),
                keyExpiry = container.garminRepository.keyExpiry(),
                tokenExpiry = container.garminRepository.tokenExpiry(),
                lastSync = container.garminRepository.lastSync(),
                lastError = container.garminRepository.lastError(),
                syncLog = container.garminRepository.syncLog()
            )
        }
        container.garminRepository.checkKeyExpiryNotice()
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

    fun openCamp() {
        viewModelScope.launch {
            container.gameRepository.mutate { GameEngine.prepareCamp(it) }
            _ui.update {
                it.copy(campOpen = true, selectedItem = null, chestOpen = false, journalOpen = false)
            }
        }
    }
    fun closeCamp() { _ui.update { it.copy(campOpen = false) } }
    fun openSettings() {
        _ui.update { it.copy(settingsOpen = true, selectedItem = null, chestOpen = false) }
    }
    fun closeSettings() { _ui.update { it.copy(settingsOpen = false) } }
    fun openJournal() {
        _ui.update { it.copy(journalOpen = true, selectedItem = null, campOpen = false) }
    }
    fun closeJournal() { _ui.update { it.copy(journalOpen = false) } }

    fun openPreferences() {
        _ui.update {
            it.copy(
                prefsOpen = true,
                selectedItem = null,
                chestOpen = false,
                campOpen = false,
                syncLog = container.garminRepository.syncLog()
            )
        }
    }

    fun openScreen(screen: String?) {
        when (screen) {
            "garmin" -> openSettings()
            "chest" -> if (game.value.chest != null) {
                _ui.update { it.copy(chestOpen = true, selectedItem = null) }
            }
            "camp" -> openCamp()
            "journal" -> openJournal()
            else -> Unit
        }
    }

    fun closePreferences() { _ui.update { it.copy(prefsOpen = false) } }

    fun setTextScale(value: Float) {
        container.settings.textScale = value
        _ui.update { it.copy(textScale = value) }
    }

    fun setReducedMotion(value: Boolean) {
        container.settings.reducedMotion = value
        _ui.update { it.copy(reducedMotion = value) }
    }

    fun setVibrations(value: Boolean) {
        container.settings.vibrations = value
        _ui.update { it.copy(vibrations = value) }
    }

    fun setNotificationsEnabled(value: Boolean) {
        container.settings.notificationsEnabled = value
        _ui.update { it.copy(notificationsEnabled = value) }
        if (!value) GameNotifications.cancelAll(container.context)
    }

    fun setNotifyBoss(value: Boolean) {
        container.settings.notifyBoss = value
        _ui.update { it.copy(notifyBoss = value) }
    }

    fun setNotifyLevel(value: Boolean) {
        container.settings.notifyLevel = value
        _ui.update { it.copy(notifyLevel = value) }
    }

    fun setNotifyOaths(value: Boolean) {
        container.settings.notifyOaths = value
        _ui.update { it.copy(notifyOaths = value) }
    }

    fun setNotifyChest(value: Boolean) {
        container.settings.notifyChest = value
        _ui.update { it.copy(notifyChest = value) }
    }

    fun setNotifyKey(value: Boolean) {
        container.settings.notifyKey = value
        _ui.update { it.copy(notifyKey = value) }
    }

    fun setQuietHours(value: Boolean) {
        container.settings.quietHours = value
        _ui.update { it.copy(quietHours = value) }
    }

    fun setAutoSellCommons(value: Boolean) {
        container.settings.autoSellCommons = value
        _ui.update { it.copy(autoSellCommons = value) }
    }

    fun completeOnboarding() {
        container.settings.onboardingDone = true
        _ui.update { it.copy(onboardingDone = true) }
    }

    fun restartOnboarding() {
        container.settings.onboardingDone = false
        _ui.update { it.copy(onboardingDone = false, prefsOpen = false) }
    }

    fun dismissOfflineReport() {
        viewModelScope.launch {
            container.gameRepository.mutate { it.offlineReport = null }
        }
    }

    fun clearProgress() {
        viewModelScope.launch {
            container.gameRepository.reset()
            _ui.update { it.copy(selectedItem = null, chestOpen = false) }
            _toasts.emit("Progression effacée. La clé Garmin est conservée.")
        }
    }

    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            val ok = runCatching {
                val text = container.gameRepository.exportJson()
                withContext(Dispatchers.IO) {
                    container.context.contentResolver.openOutputStream(uri)?.use {
                        it.write(text.toByteArray(Charsets.UTF_8))
                    } ?: error("Flux indisponible")
                }
            }.isSuccess
            _toasts.emit(if (ok) "Sauvegarde exportée." else "Export impossible.")
        }
    }

    fun importFrom(uri: Uri) {
        inspectImport(uri)
    }

    fun inspectImport(uri: Uri) {
        viewModelScope.launch {
            val text = runCatching {
                withContext(Dispatchers.IO) {
                    container.context.contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                        it.readText()
                    }
                }
            }.getOrNull()
            val summary = text?.let { container.gameRepository.inspectJson(it) }
            if (text == null || summary == null) {
                pendingImportText = null
                _ui.update { it.copy(pendingImport = null) }
                _toasts.emit("Fichier de sauvegarde invalide ou incompatible.")
            } else {
                pendingImportText = text
                _ui.update { it.copy(pendingImport = summary) }
            }
        }
    }

    fun confirmImport() {
        val text = pendingImportText ?: return
        viewModelScope.launch {
            val ok = container.gameRepository.importJson(text)
            pendingImportText = null
            if (ok) {
                _ui.update {
                    it.copy(
                        pendingImport = null,
                        selectedItem = null,
                        chestOpen = false,
                        campOpen = false,
                        prefsOpen = false
                    )
                }
                _toasts.emit("Sauvegarde importée.")
            } else {
                _ui.update { it.copy(pendingImport = null) }
                _toasts.emit("Import impossible.")
            }
        }
    }

    fun cancelImport() {
        pendingImportText = null
        _ui.update { it.copy(pendingImport = null) }
    }

    fun checkForUpdate() {
        viewModelScope.launch {
            val release = withContext(Dispatchers.IO) {
                runCatching {
                    val connection = URL(
                        "https://api.github.com/repos/brunothesatellite/idle-step-rpg/releases/latest"
                    ).openConnection() as HttpURLConnection
                    connection.connectTimeout = 10_000
                    connection.readTimeout = 10_000
                    connection.setRequestProperty("Accept", "application/vnd.github+json")
                    connection.setRequestProperty("User-Agent", "LaMarcheDesAmes")
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    JSONObject(body)
                }.getOrNull()
            }
            val tag = release?.optString("tag_name")?.takeIf { it.isNotBlank() }
            val url = release?.optString("html_url")?.takeIf { it.isNotBlank() }
            if (tag == null) {
                _toasts.emit("Vérification impossible. Réessayez plus tard.")
                return@launch
            }
            if (tag != "v${BuildConfig.VERSION_NAME}") {
                _ui.update { it.copy(updateTag = tag, updateUrl = url) }
                _toasts.emit("Mise à jour $tag disponible.")
            } else {
                _ui.update { it.copy(updateTag = null, updateUrl = null) }
                _toasts.emit("Vous avez la dernière version.")
            }
        }
    }

    fun equipBest() {
        viewModelScope.launch {
            var swaps = 0
            container.gameRepository.mutate { swaps = GameEngine.equipBest(it) }
            _toasts.emit(
                if (swaps > 0) "$swaps pièce(s) équipée(s)." else "Votre équipement est déjà optimal."
            )
        }
    }

    fun sellCommons() {
        viewModelScope.launch {
            var gold = 0
            container.gameRepository.mutate { gold = GameEngine.sellCommons(it) }
            _toasts.emit(
                if (gold > 0) "Objets communs vendus · +$gold or." else "Aucun objet commun à vendre."
            )
        }
    }

    fun toggleLock(item: Item) {
        viewModelScope.launch {
            container.gameRepository.mutate { GameEngine.toggleLock(it, item.id) }
            val updated = container.gameRepository.state.value.inventory.firstOrNull { it.id == item.id }
            _ui.update { it.copy(selectedItem = updated) }
        }
    }

    fun fuse(item: Item) {
        viewModelScope.launch {
            var fused: Item? = null
            container.gameRepository.mutate { fused = GameEngine.fuseItem(it, item.id) }
            _ui.update { it.copy(selectedItem = null) }
            _toasts.emit(
                if (fused != null) "Fusion réussie."
                else "Il faut 3 objets du même emplacement et de même rareté."
            )
        }
    }

    fun withdrawStash() {
        viewModelScope.launch {
            var moved = 0
            container.gameRepository.mutate { moved = GameEngine.withdrawStash(it) }
            _toasts.emit(
                if (moved > 0) "$moved objet(s) récupéré(s)." else "Inventaire plein ou coffre vide."
            )
        }
    }

    fun withdrawStashItem(item: Item) {
        viewModelScope.launch {
            var ok = false
            container.gameRepository.mutate { ok = GameEngine.withdrawStashItem(it, item.id) }
            _toasts.emit(if (ok) "${item.name} récupéré." else "Inventaire plein.")
        }
    }

    private fun refreshSelected(itemId: String) {
        val updated = container.gameRepository.state.value.inventory.firstOrNull { it.id == itemId }
        _ui.update { it.copy(selectedItem = updated) }
    }

    fun identifyItem(item: Item) {
        viewModelScope.launch {
            var ok = false
            container.gameRepository.mutate { ok = GameEngine.identifyItem(it, item.id) }
            refreshSelected(item.id)
            _toasts.emit(if (ok) "Objet identifié." else "1 éclat requis.")
        }
    }

    fun rerollEffect(item: Item) {
        viewModelScope.launch {
            var ok = false
            container.gameRepository.mutate { ok = GameEngine.rerollEffect(it, item.id) }
            refreshSelected(item.id)
            _toasts.emit(if (ok) "Affixe reforgé." else "2 éclats requis.")
        }
    }

    fun upgradeItem(item: Item) {
        viewModelScope.launch {
            var ok = false
            container.gameRepository.mutate { ok = GameEngine.upgradeItem(it, item.id) }
            refreshSelected(item.id)
            _toasts.emit(if (ok) "Objet amélioré." else "Or ou éclat insuffisant.")
        }
    }

    fun dismantleItem(item: Item) {
        viewModelScope.launch {
            var shards = 0
            container.gameRepository.mutate { shards = GameEngine.dismantleItem(it, item.id) }
            _ui.update { it.copy(selectedItem = null) }
            _toasts.emit(if (shards > 0) "Objet démonté · +$shards éclat(s)." else "Objet verrouillé.")
        }
    }

    fun freeRest() {
        viewModelScope.launch {
            var ok = false
            container.gameRepository.mutate { ok = GameEngine.freeRest(it) }
            _toasts.emit(if (ok) "Veillée gratuite." else "Veillée déjà utilisée ou PV complets.")
        }
    }

    fun resolveSpecial(accept: Boolean) {
        viewModelScope.launch {
            container.gameRepository.mutate { GameEngine.resolveSpecial(it, accept) }
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

