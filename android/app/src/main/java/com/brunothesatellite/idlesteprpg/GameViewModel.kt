package com.brunothesatellite.idlesteprpg

import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.brunothesatellite.idlesteprpg.audio.SoundFx
import com.brunothesatellite.idlesteprpg.data.SaveSummary
import com.brunothesatellite.idlesteprpg.engine.ForgeOutcome
import com.brunothesatellite.idlesteprpg.engine.GameEngine
import com.brunothesatellite.idlesteprpg.engine.Item
import com.brunothesatellite.idlesteprpg.engine.SpecialOutcome
import com.brunothesatellite.idlesteprpg.engine.Upgrades
import com.brunothesatellite.idlesteprpg.engine.biomeKeyFor
import com.brunothesatellite.idlesteprpg.notify.GameNotifications
import com.brunothesatellite.idlesteprpg.steps.StepFetch
import com.brunothesatellite.idlesteprpg.steps.StepSourceType
import com.brunothesatellite.idlesteprpg.ui.formatInt
import com.brunothesatellite.idlesteprpg.ui.itemName
import com.brunothesatellite.idlesteprpg.ui.sourceLabel
import com.brunothesatellite.idlesteprpg.ui.translateText
import com.brunothesatellite.idlesteprpg.update.UpdateChecker
import com.brunothesatellite.idlesteprpg.update.UpdateInfo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.time.Instant
import java.time.LocalDate

/** Durée minimale d'absence avant de lancer la simulation de reprise. */
private const val RESUME_SIM_MIN_MS = 2L * 60L * 1000L

/** Budget d'attente de la synchro sur l'écran de reprise. */
private const val RESUME_SYNC_BUDGET_MS = 3_000L

/** Au-delà, un import tardif est abandonné plutôt que d'attendre indéfiniment. */
private const val RESUME_LATE_SYNC_MAX_MS = 60_000L

/** Étape affichée par l'écran de reprise. */
enum class ResumePhase { SIMULATING, SYNCING }

data class ResumeUi(
    val startedAt: Long,
    val phase: ResumePhase,
    val sourceLabel: String
)

data class UiState(
    val campOpen: Boolean = false,
    val paused: Boolean = false,
    val settingsOpen: Boolean = false,
    val prefsOpen: Boolean = false,
    val journalOpen: Boolean = false,
    val codexOpen: Boolean = false,
    val endingPreview: Boolean = false,
    val selectedItem: Item? = null,
    val selectedStashItem: Item? = null,
    val chestOpen: Boolean = false,
    val syncing: Boolean = false,
    val connected: Boolean = false,
    val keyExpiry: LocalDate? = null,
    val tokenExpiry: Instant? = null,
    val lastSync: Long = 0L,
    val lastError: String = "",
    val lastAdded: Int = 0,
    val stepSource: String = "garmin",
    val healthAvailable: Boolean = false,
    val healthPermission: Boolean = false,
    val stepStatus: String = "",
    val textScale: Float = 1.35f,
    val reducedMotion: Boolean = false,
    val vibrations: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val notifyBoss: Boolean = true,
    val notifyLevel: Boolean = true,
    val notifyOaths: Boolean = true,
    val notifyChest: Boolean = true,
    val notifyKey: Boolean = true,
    val notifySync: Boolean = false,
    val notifySpecial: Boolean = true,
    val notifyBossWaiting: Boolean = true,
    val quietHours: Boolean = false,
    val autoSellCommons: Boolean = false,
    val autoOptimize: Boolean = false,
    val autoOpenChests: Boolean = true,
    val autoHeal: Boolean = true,
    val offlineMode: String = "percent",
    val offlinePercent: Int = 30,
    val offlineMaxSteps: Int = 1_000,
    val onboardingDone: Boolean = true,
    val soundsEnabled: Boolean = true,
    val soundVolume: Float = 0.6f,
    val musicEnabled: Boolean = true,
    val musicVolume: Float = 0.30f,
    val syncLog: List<String> = emptyList(),
    val pendingImport: SaveSummary? = null,
    val dayBanner: String? = null,
    val autoUpdateCheck: Boolean = true,
    val lastUpdateCheckAt: Long = 0L,
    val updateChecking: Boolean = false,
    val availableUpdate: UpdateInfo? = null,
    val updateDownloading: Boolean = false,
    val updateProgress: Float = 0f,
    val updateError: String? = null,
    val postUpdateTag: String? = null,
    val postUpdateUrl: String? = null,
    val specialOutcome: SpecialOutcome? = null,
    val consumablesOpen: Boolean = false,
    /** Écran de reprise (simulation + synchro) ; null quand il est fermé. */
    val resume: ResumeUi? = null,
    /** Un import est encore attendu après la fermeture de l'écran de reprise. */
    val resumePendingFetch: Boolean = false
)

class GameViewModel(private val container: AppContainer) : ViewModel() {

    val game = container.gameRepository.state

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private val _toasts = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toasts: SharedFlow<String> = _toasts.asSharedFlow()

    @Volatile private var visible = false
    private var resuming = false
    private var resumeActive = false
    @Volatile private var resumeSkipRequested = false
    private var pendingImportText: String? = null
    private var lastKnownDay: String = ""
    private var lastBiomeKey: String = ""
    private var lastAutosaveAt: Long = 0L
    private var lastSavedFloor: Int = -1
    private var lastSavedLevel: Int = -1
    private var autosaving: Boolean = false

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
                notifySync = container.settings.notifySync,
                notifySpecial = container.settings.notifySpecial,
                notifyBossWaiting = container.settings.notifyBossWaiting,
                quietHours = container.settings.quietHours,
                autoSellCommons = container.settings.autoSellCommons,
                autoOptimize = container.settings.autoOptimize,
                autoOpenChests = container.settings.autoOpenChests,
                autoHeal = container.settings.autoHeal,
                offlineMode = container.settings.offlineMode,
                offlinePercent = container.settings.offlinePercent,
                offlineMaxSteps = container.settings.offlineMaxSteps,
                onboardingDone = container.settings.onboardingDone,
                soundsEnabled = container.settings.soundsEnabled,
                soundVolume = container.settings.soundVolume,
                musicEnabled = container.settings.musicEnabled,
                musicVolume = container.settings.musicVolume,
                autoUpdateCheck = container.settings.autoUpdateCheck,
                lastUpdateCheckAt = container.settings.lastUpdateCheckAt,
                stepSource = container.stepRepository.current().id,
                healthAvailable = container.stepRepository.healthAvailable(),
                healthPermission = container.stepRepository.healthPermission(),
                stepStatus = container.stepRepository.statusLine()
            )
        }
        lastKnownDay = game.value.day
        lastBiomeKey = biomeKeyFor(game.value.floor)
        lastSavedFloor = game.value.floor
        lastSavedLevel = game.value.level
        container.ambientMusic.setBiome(lastBiomeKey)
        refreshGarmin()
        viewModelScope.launch {
            while (isActive) {
                val reportOpen = game.value.offlineReport?.let { it.acknowledgedAt == 0L } == true
                var stepMs = 500L
                if (visible && !resuming && !_ui.value.paused && !_ui.value.campOpen &&
                    !_ui.value.settingsOpen &&
                    !_ui.value.prefsOpen && !_ui.value.journalOpen && !reportOpen &&
                    !_ui.value.consumablesOpen && !_ui.value.endingPreview &&
                    !game.value.endingPending
                ) {
                    val snapshot = game.value
                    // Cadence adaptative : 10 Hz quand quelque chose peut bouger,
                    // 2 Hz au repos (réserve vide, pas de combat ni de rencontre).
                    val busy = snapshot.stepBank > 0.0 ||
                        snapshot.fight != null ||
                        snapshot.special != null
                    stepMs = if (busy) 100L else 500L
                    val chestsBefore = snapshot.chestsOpened
                    container.gameRepository.tick(stepMs.toDouble())
                    if (game.value.chestsOpened > chestsBefore) {
                        _toasts.tryEmit(
                            tr(R.string.toast_chest_open, formatInt(game.value.chestFlashGold))
                        )
                    }
                    // Sauvegarde périodique pendant le jeu : une mise à jour d'APK
                    // tue le processus sans onPause, et sans elle on pouvait perdre
                    // toute la session (gardien battu, palier, butin). On sauvegarde
                    // aussi immédiatement sur changement de palier ou de niveau.
                    val now = System.currentTimeMillis()
                    val after = game.value
                    val milestone = after.floor != lastSavedFloor || after.level != lastSavedLevel
                    if (!autosaving && (milestone || now - lastAutosaveAt >= 30_000L)) {
                        autosaving = true
                        lastAutosaveAt = now
                        lastSavedFloor = after.floor
                        lastSavedLevel = after.level
                        viewModelScope.launch {
                            try {
                                container.gameRepository.save()
                            } finally {
                                autosaving = false
                            }
                        }
                    }
                }
                val today = game.value.day
                if (lastKnownDay.isNotBlank() && today.isNotBlank() && today != lastKnownDay) {
                    _ui.update { it.copy(dayBanner = tr(R.string.toast_new_day)) }
                    viewModelScope.launch {
                        delay(6_000)
                        _ui.update { it.copy(dayBanner = null) }
                    }
                }
                lastKnownDay = today
                val currentBiome = biomeKeyFor(game.value.floor)
                if (currentBiome != lastBiomeKey) {
                    lastBiomeKey = currentBiome
                    container.ambientMusic.setBiome(currentBiome)
                }
                delay(stepMs)
            }
        }
        viewModelScope.launch {
            // Horodatage d'activité : en mémoire seulement, sans sauvegarde JSON.
            while (isActive) {
                delay(30_000)
                if (visible) container.gameRepository.touchActive()
            }
        }
        viewModelScope.launch {
            delay(2_500)
            if (_ui.value.onboardingDone) maybeCheckForUpdates()
        }
        checkPendingRelease()
    }

    fun onAppResume() {
        refreshGarmin()
        checkPendingRelease()
        maybeCheckForUpdates()
        container.ambientMusic.onResume()

        val now = System.currentTimeMillis()
        val lastActive = game.value.lastActiveAt
        val elapsed = if (lastActive > 0L) {
            (now - lastActive).coerceIn(0L, 7L * 24L * 60L * 60L * 1000L)
        } else {
            0L
        }

        visible = true
        val state = game.value
        val reportOpen = state.offlineReport?.let { it.acknowledgedAt == 0L } == true
        // Un résumé ou une étape déjà ouverte fige la partie : on ne simule pas
        // par-dessus, et on ne rouvre pas un second écran.
        if (reportOpen || state.mode == "camp" || state.mode == "special") {
            viewModelScope.launch { container.gameRepository.markActive() }
            return
        }
        if (resumeActive) return

        val canSimulate = elapsed >= RESUME_SIM_MIN_MS && !_ui.value.paused
        val canSync = _ui.value.onboardingDone && shouldSyncOnResume()

        resumeActive = true
        resumeSkipRequested = false
        resuming = true
        viewModelScope.launch {
            try {
                resumeSession(elapsed, canSimulate, canSync)
            } finally {
                _ui.update { it.copy(resume = null, syncing = false) }
                resuming = false
                resumeActive = false
                container.gameRepository.markActive()
                refreshGarmin()
            }
        }
    }

    /**
     * Reprise unique : simulation d'absence et import de pas dans le même écran.
     * L'import est attendu au maximum [RESUME_SYNC_BUDGET_MS] ; au-delà (ou si
     * le joueur passe), il est appliqué en arrière-plan sans rouvrir de résumé.
     */
    private suspend fun resumeSession(elapsed: Long, canSimulate: Boolean, canSync: Boolean) {
        val startedAt = SystemClock.elapsedRealtime()
        val sourceLabel = if (container.stepRepository.current() == StepSourceType.HEALTH) {
            tr(R.string.notify_health_connect)
        } else {
            "Garmin"
        }

        if (canSimulate || canSync) {
            _ui.update {
                it.copy(
                    resume = ResumeUi(
                        startedAt = startedAt,
                        phase = if (canSimulate) ResumePhase.SIMULATING else ResumePhase.SYNCING,
                        sourceLabel = sourceLabel
                    ),
                    resumePendingFetch = false
                )
            }
        }

        // Le téléchargement démarre en parallèle de la simulation. Il ne nourrit
        // pas la simulation : l'absence travaille sur la réserve héritée.
        val fetched = CompletableDeferred<StepFetch?>()
        var fetchJob: Job? = null
        if (canSync) {
            _ui.update { it.copy(syncing = true) }
            fetchJob = viewModelScope.launch {
                val value = runCatching { container.stepRepository.fetch() }.getOrNull()
                fetched.complete(value)
            }
        } else {
            fetched.complete(null)
        }

        if (canSimulate) {
            // La simulation écrit l'état et le résumé d'absence, mais l'écran
            // de reprise reste au-dessus : un seul résumé sera montré.
            container.gameRepository.simulateAbsence(elapsed)
            if (canSync) {
                _ui.update { it.copy(resume = it.resume?.copy(phase = ResumePhase.SYNCING)) }
            }
        }

        if (!canSync) return

        // Budget global : 3 s à partir de l'ouverture. Sortie manuelle possible.
        val deadline = startedAt + RESUME_SYNC_BUDGET_MS
        while (!fetched.isCompleted && !resumeSkipRequested &&
            SystemClock.elapsedRealtime() < deadline
        ) {
            delay(50)
        }

        val result = if (fetched.isCompleted) fetched.await() else null
        val abandoned = result == null && !fetched.isCompleted
        if (abandoned) {
            _ui.update { it.copy(resumePendingFetch = true) }
            applyLateFetch(fetched, fetchJob)
        } else if (result != null) {
            val sync = container.stepRepository.apply(result)
            _ui.update {
                it.copy(
                    syncing = false,
                    lastAdded = if (sync.ok) sync.added else it.lastAdded
                )
            }
            // Sans résumé à afficher, un toast évite que les pas importés
            // passent inaperçus (comportement d'avant la fusion des écrans).
            if (sync.ok && sync.added > 0 && game.value.offlineReport == null) {
                _toasts.tryEmit(sync.message)
            }
        }
    }

    /**
     * Import arrivé après la fermeture de l'écran de reprise : il crédite les
     * pas sans rouvrir de résumé, et se contente d'un toast.
     */
    private fun applyLateFetch(fetched: CompletableDeferred<StepFetch?>, fetchJob: Job?) {
        viewModelScope.launch {
            val result = withTimeoutOrNull(RESUME_LATE_SYNC_MAX_MS) { fetched.await() }
            if (result == null) {
                fetchJob?.cancel()
                _ui.update { it.copy(resumePendingFetch = false, syncing = false) }
                return@launch
            }
            val sync = container.stepRepository.apply(result, publishReport = false)
            _ui.update {
                it.copy(
                    resumePendingFetch = false,
                    syncing = false,
                    lastAdded = if (sync.ok) sync.added else it.lastAdded
                )
            }
            if (sync.ok && sync.added > 0) {
                _toasts.tryEmit(tr(R.string.toast_steps_recovered, formatInt(sync.added)))
            }
            refreshGarmin()
        }
    }

    /** Sortie manuelle de l'écran de reprise ; l'import continue en fond. */
    fun skipResumeWait() {
        if (resumeActive) resumeSkipRequested = true
    }

    private fun shouldSyncOnResume(): Boolean {
        val last = _ui.value.lastSync
        return container.stepRepository.canSync() &&
            (last == 0L || System.currentTimeMillis() - last > 2L * 60L * 1000L)
    }

    fun onAppPause() {
        visible = false
        container.ambientMusic.onPause()
        viewModelScope.launch {
            container.gameRepository.markActive()
            container.gameRepository.refreshWidget()
        }
    }

    fun togglePause() {
        val next = !_ui.value.paused
        _ui.update { it.copy(paused = next) }
        _toasts.tryEmit(if (next) tr(R.string.toast_pause) else tr(R.string.toast_resume))
    }

    fun refreshGarmin() {
        _ui.update {
            it.copy(
                connected = container.stepRepository.canSync(),
                keyExpiry = container.stepRepository.keyExpiry(),
                tokenExpiry = container.stepRepository.tokenExpiry(),
                lastSync = container.stepRepository.lastSync(),
                lastError = container.stepRepository.lastError(),
                syncLog = container.stepRepository.syncLog(),
                stepSource = container.stepRepository.current().id,
                healthAvailable = container.stepRepository.healthAvailable(),
                healthPermission = container.stepRepository.healthPermission(),
                stepStatus = container.stepRepository.statusLine()
            )
        }
        container.stepRepository.checkKeyExpiryNotice()
    }

    fun setStepSource(id: String) {
        val type = StepSourceType.fromId(id)
        if (type.id == container.stepRepository.current().id) return
        container.stepRepository.changeSource(type)
        com.brunothesatellite.idlesteprpg.sync.SyncScheduler.schedule(
            container.context,
            requireNetwork = type == StepSourceType.GARMIN
        )
        refreshGarmin()
        _toasts.tryEmit(
            tr(R.string.toast_source_changed, sourceLabel(container.context.resources, type.id))
        )
        if (container.stepRepository.canSync()) syncNow(silent = true)
    }

    fun healthPermissionName(): String = container.stepRepository.healthPermissionRequest()

    fun onHealthPermissionResult(granted: Boolean) {
        refreshGarmin()
        if (granted) {
            _toasts.tryEmit(tr(R.string.toast_health_granted))
            syncNow(silent = true)
        } else {
            _toasts.tryEmit(tr(R.string.toast_health_denied))
        }
    }

    fun syncNow(silent: Boolean = false) {
        if (_ui.value.syncing) return
        if (!container.stepRepository.canSync()) {
            _ui.update {
                if (container.stepRepository.current() ==
                    com.brunothesatellite.idlesteprpg.steps.StepSourceType.GARMIN
                ) it.copy(settingsOpen = true) else it.copy(prefsOpen = true)
            }
            return
        }
        _ui.update { it.copy(syncing = true) }
        viewModelScope.launch {
            val result = container.stepRepository.sync()
            refreshGarmin()
            _ui.update { it.copy(syncing = false, lastAdded = result.added) }
            if (result.needsKey) {
                GameNotifications.keyExpiry(container.context, result.message)
            }
            if (!silent) _toasts.emit(result.message)
        }
    }

    fun saveKey(rawKey: String, expiry: LocalDate) {
        _ui.update { it.copy(syncing = true) }
        viewModelScope.launch {
            val result = container.stepRepository.saveGarminKey(rawKey, expiry)
            _ui.update { it.copy(syncing = false) }
            refreshGarmin()
            if (result.isSuccess) {
                _toasts.emit(tr(R.string.toast_key_saved))
                syncNow(silent = true)
            } else {
                _toasts.emit(result.exceptionOrNull()?.message ?: tr(R.string.toast_key_refused))
            }
        }
    }

    fun updateExpiry(date: LocalDate) {
        container.stepRepository.setGarminKeyExpiry(date)
        refreshGarmin()
        _toasts.tryEmit(tr(R.string.toast_expiry_updated))
    }

    fun revokeGarmin() {
        container.stepRepository.revokeGarmin()
        refreshGarmin()
        _toasts.tryEmit(tr(R.string.toast_key_removed))
    }

    fun engageBoss() {
        viewModelScope.launch {
            container.gameRepository.mutate { GameEngine.engageBoss(it) }
        }
    }

    fun farmFromGate() {
        viewModelScope.launch {
            container.gameRepository.mutate { GameEngine.farmFromGate(it) }
        }
    }

    fun openCamp() {
        viewModelScope.launch {
            container.gameRepository.mutate { GameEngine.prepareCamp(it) }
            _ui.update {
                it.copy(campOpen = true, selectedItem = null, selectedStashItem = null, chestOpen = false, journalOpen = false)
            }
        }
    }
    fun closeCamp() { _ui.update { it.copy(campOpen = false) } }
    fun openSettings() {
        _ui.update { it.copy(settingsOpen = true, selectedItem = null, selectedStashItem = null, chestOpen = false) }
    }
    fun closeSettings() { _ui.update { it.copy(settingsOpen = false) } }
    fun openJournal() {
        _ui.update { it.copy(journalOpen = true, selectedItem = null, selectedStashItem = null, campOpen = false) }
    }
    fun closeJournal() { _ui.update { it.copy(journalOpen = false) } }

    fun openConsumables() {
        _ui.update { it.copy(consumablesOpen = true, selectedItem = null, selectedStashItem = null) }
    }

    fun closeConsumables() { _ui.update { it.copy(consumablesOpen = false) } }

    fun useConsumable(id: String) {
        viewModelScope.launch {
            var outcome: SpecialOutcome? = null
            container.gameRepository.mutate { outcome = GameEngine.useConsumable(it, id) }
            outcome?.let {
                if (it.good) SoundFx.play(container.context, "consume", 1f)
                _toasts.emit(translateText(container.context.resources, it.text))
            }
        }
    }

    fun openPreferences() {
        _ui.update {
            it.copy(
                prefsOpen = true,
                selectedItem = null, selectedStashItem = null,
                chestOpen = false,
                campOpen = false,
                syncLog = container.stepRepository.syncLog()
            )
        }
    }

    fun openScreen(screen: String?) {
        when (screen) {
            "garmin" -> openSettings()
            "chest" -> if (game.value.chest != null) {
                _ui.update { it.copy(chestOpen = true, selectedItem = null, selectedStashItem = null) }
            }
            "camp" -> openCamp()
            "journal" -> openJournal()
            else -> Unit
        }
    }

    fun closePreferences() { _ui.update { it.copy(prefsOpen = false) } }

    fun openCodex() { _ui.update { it.copy(codexOpen = true) } }

    fun closeCodex() { _ui.update { it.copy(codexOpen = false) } }

    fun dismissEnding() {
        viewModelScope.launch {
            container.gameRepository.mutate { it.endingPending = false }
        }
    }

    fun openEndingPreview() {
        _ui.update { it.copy(endingPreview = true) }
    }

    fun closeEndingPreview() {
        _ui.update { it.copy(endingPreview = false) }
    }

    fun setHoldFloor(value: Boolean) {
        viewModelScope.launch {
            container.gameRepository.mutate { it.holdFloor = value }
            _toasts.emit(
                if (value) tr(R.string.toast_farm_on)
                else tr(R.string.toast_farm_off)
            )
        }
    }

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

    fun setNotifySync(value: Boolean) {
        container.settings.notifySync = value
        _ui.update { it.copy(notifySync = value) }
    }

    fun setNotifySpecial(value: Boolean) {
        container.settings.notifySpecial = value
        _ui.update { it.copy(notifySpecial = value) }
    }

    fun setNotifyBossWaiting(value: Boolean) {
        container.settings.notifyBossWaiting = value
        _ui.update { it.copy(notifyBossWaiting = value) }
    }

    fun setQuietHours(value: Boolean) {
        container.settings.quietHours = value
        _ui.update { it.copy(quietHours = value) }
    }

    fun setAutoSellCommons(value: Boolean) {
        container.settings.autoSellCommons = value
        _ui.update { it.copy(autoSellCommons = value) }
    }

    fun setAutoOptimize(value: Boolean) {
        container.settings.autoOptimize = value
        _ui.update { it.copy(autoOptimize = value) }
    }

    fun setAutoOpenChests(value: Boolean) {
        container.settings.autoOpenChests = value
        _ui.update { it.copy(autoOpenChests = value) }
    }

    fun setAutoHeal(value: Boolean) {
        container.settings.autoHeal = value
        _ui.update { it.copy(autoHeal = value) }
    }

    fun setOfflineMode(value: String) {
        val mode = if (value == "steps") "steps" else "percent"
        container.settings.offlineMode = mode
        _ui.update { it.copy(offlineMode = mode) }
    }

    fun setOfflinePercent(value: Int) {
        val percent = value.coerceIn(0, 30)
        container.settings.offlinePercent = percent
        _ui.update { it.copy(offlinePercent = percent) }
    }

    fun setOfflineMaxSteps(value: Int) {
        val steps = value.coerceIn(0, 5_000)
        container.settings.offlineMaxSteps = steps
        _ui.update { it.copy(offlineMaxSteps = steps) }
    }

    fun resetOfflinePolicy() {
        container.settings.offlineMode = "percent"
        container.settings.offlinePercent = 30
        container.settings.offlineMaxSteps = 1_000
        _ui.update { it.copy(offlineMode = "percent", offlinePercent = 30, offlineMaxSteps = 1_000) }
    }

    fun setSoundsEnabled(value: Boolean) {
        container.settings.soundsEnabled = value
        _ui.update { it.copy(soundsEnabled = value) }
        SoundFx.configure(container.context)
        if (value) SoundFx.preview(container.context)
    }

    fun setSoundVolume(value: Float) {
        val volume = value.coerceIn(0f, 1f)
        container.settings.soundVolume = volume
        _ui.update { it.copy(soundVolume = volume) }
        SoundFx.configure(container.context)
    }

    fun setMusicEnabled(value: Boolean) {
        container.settings.musicEnabled = value
        _ui.update { it.copy(musicEnabled = value) }
        container.ambientMusic.refreshSettings()
    }

    fun setMusicVolume(value: Float) {
        val volume = value.coerceIn(0f, 1f)
        container.settings.musicVolume = volume
        _ui.update { it.copy(musicVolume = volume) }
        container.ambientMusic.applyVolume()
    }

    fun previewSound() {
        SoundFx.preview(container.context)
    }

    fun completeOnboarding() {
        container.settings.onboardingDone = true
        _ui.update { it.copy(onboardingDone = true) }
        container.ambientMusic.onResume()
    }

    fun restartOnboarding() {
        container.settings.onboardingDone = false
        container.ambientMusic.onPause()
        _ui.update { it.copy(onboardingDone = false, prefsOpen = false) }
    }

    fun dismissOfflineReport() {
        viewModelScope.launch {
            // Fermer le résumé ne déclenche plus d'import : celui-ci a déjà été
            // appliqué (ou continue en fond sans rouvrir d'écran).
            container.gameRepository.mutate { it.offlineReport = null }
        }
    }

    fun clearProgress() {
        viewModelScope.launch {
            container.gameRepository.reset()
            _ui.update { it.copy(selectedItem = null, selectedStashItem = null, chestOpen = false) }
            _toasts.emit(tr(R.string.toast_progress_cleared))
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
            _toasts.emit(if (ok) tr(R.string.toast_export_ok) else tr(R.string.toast_export_fail))
        }
    }

    fun exportBalanceReportTo(uri: Uri) {
        viewModelScope.launch {
            val ok = runCatching {
                val text = container.gameRepository.exportBalanceReport(BuildConfig.VERSION_NAME)
                withContext(Dispatchers.IO) {
                    container.context.contentResolver.openOutputStream(uri)?.use {
                        it.write(text.toByteArray(Charsets.UTF_8))
                    } ?: error("Flux indisponible")
                }
            }.isSuccess
            _toasts.emit(if (ok) tr(R.string.toast_export_ok) else tr(R.string.toast_export_fail))
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
                _toasts.emit(tr(R.string.toast_import_invalid))
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
                        selectedItem = null, selectedStashItem = null,
                        chestOpen = false,
                        campOpen = false,
                        prefsOpen = false
                    )
                }
                _toasts.emit(tr(R.string.toast_import_ok))
            } else {
                _ui.update { it.copy(pendingImport = null) }
                _toasts.emit(tr(R.string.toast_import_fail))
            }
        }
    }

    fun cancelImport() {
        pendingImportText = null
        _ui.update { it.copy(pendingImport = null) }
    }

    fun checkForUpdate(manual: Boolean = false) {
        if (BuildConfig.PLAY_DISTRIBUTION) {
            if (manual) _toasts.tryEmit(tr(R.string.toast_updates_play))
            return
        }
        if (_ui.value.updateChecking) return
        _ui.update { it.copy(updateChecking = true, updateError = null) }
        viewModelScope.launch {
            val info = UpdateChecker.fetchLatest()
            val now = System.currentTimeMillis()
            container.settings.lastUpdateCheckAt = now
            if (info != null && UpdateChecker.isNewer(BuildConfig.VERSION_NAME, info.version)) {
                val show = manual || container.settings.dismissedUpdateTag != info.tag
                _ui.update {
                    it.copy(
                        updateChecking = false,
                        lastUpdateCheckAt = now,
                        availableUpdate = if (show) info else null
                    )
                }
                if (manual) _toasts.emit(tr(R.string.toast_update_available, info.tag))
            } else {
                _ui.update {
                    it.copy(
                        updateChecking = false,
                        lastUpdateCheckAt = now,
                        availableUpdate = null
                    )
                }
                if (manual) {
                    _toasts.emit(
                        if (info == null) tr(R.string.toast_check_fail)
                        else tr(R.string.toast_latest)
                    )
                }
            }
        }
    }

    private fun maybeCheckForUpdates() {
        // La copie de test (.debug) ne doit jamais proposer l'APK de production :
        // l'installer remplacerait l'autre application.
        if (BuildConfig.DEBUG) return
        // La variante Play ne vérifie jamais GitHub : Play gère les mises à jour.
        if (BuildConfig.PLAY_DISTRIBUTION) return
        if (!_ui.value.onboardingDone) return
        if (!container.settings.autoUpdateCheck) return
        val elapsed = System.currentTimeMillis() - container.settings.lastUpdateCheckAt
        if (elapsed in 0 until 24L * 60L * 60L * 1000L) return
        checkForUpdate(manual = false)
    }

    fun setAutoUpdateCheck(value: Boolean) {
        container.settings.autoUpdateCheck = value
        _ui.update { it.copy(autoUpdateCheck = value) }
        if (value) maybeCheckForUpdates()
    }

    fun dismissUpdate() {
        _ui.value.availableUpdate?.let {
            container.settings.dismissedUpdateTag = it.tag
        }
        _ui.update { it.copy(availableUpdate = null, updateError = null) }
    }

    fun openRelease(url: String) {
        runCatching {
            container.context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    private fun checkPendingRelease() {
        if (BuildConfig.PLAY_DISTRIBUTION) return
        val tag = container.settings.pendingReleaseTag
        val url = container.settings.pendingReleaseUrl
        if (tag.isBlank() || url.isBlank()) return
        if (UpdateChecker.isNewer(BuildConfig.VERSION_NAME, tag.removePrefix("v"))) return
        container.settings.pendingReleaseTag = ""
        container.settings.pendingReleaseUrl = ""
        _ui.update { it.copy(postUpdateTag = tag, postUpdateUrl = url) }
    }

    fun dismissPostUpdate() {
        _ui.update { it.copy(postUpdateTag = null, postUpdateUrl = null) }
    }

    fun openPostUpdateRelease() {
        val url = _ui.value.postUpdateUrl
        if (url != null) openRelease(url)
        dismissPostUpdate()
    }

    fun installUpdate() {
        if (BuildConfig.DEBUG) return
        if (BuildConfig.PLAY_DISTRIBUTION) return
        val info = _ui.value.availableUpdate ?: return
        if (_ui.value.updateDownloading) return
        val context = container.context
        if (!context.packageManager.canRequestPackageInstalls()) {
            _ui.update {
                it.copy(updateError = tr(R.string.toast_install_permission))
            }
            runCatching {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}")
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            return
        }
        _ui.update {
            it.copy(updateDownloading = true, updateProgress = 0f, updateError = null)
        }
        viewModelScope.launch {
            var lastProgress = -1f
            val file = UpdateChecker.downloadApk(context, info) { progress ->
                if (progress - lastProgress >= 0.01f || progress >= 1f) {
                    lastProgress = progress
                    _ui.update { it.copy(updateProgress = progress) }
                }
            }
            if (file == null) {
                _ui.update {
                    it.copy(
                        updateDownloading = false,
                        updateError = tr(R.string.toast_download_fail)
                    )
                }
                return@launch
            }
            _ui.update { it.copy(updateDownloading = false, updateProgress = 1f) }
            container.settings.pendingReleaseTag = info.tag
            container.settings.pendingReleaseUrl = info.releaseUrl
            // L'installation remplace l'APK et tue le processus : on écrit la
            // progression sur disque avant de lancer l'installateur.
            container.gameRepository.save()
            launchInstaller(file)
        }
    }

    private fun launchInstaller(file: File) {
        val context = container.context
        runCatching {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            context.startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_ACTIVITY_NEW_TASK
                    )
            )
        }.onFailure {
            _ui.update { state ->
                state.copy(
                    updateError = tr(
                        R.string.toast_install_fail,
                        it.message ?: tr(R.string.msg_unknown_error)
                    )
                )
            }
        }
    }

    fun equipBest() {
        viewModelScope.launch {
            var swaps = 0
            container.gameRepository.mutate { swaps = GameEngine.equipBest(it) }
            _toasts.emit(
                if (swaps > 0) tr(R.string.toast_equip_changed, swaps)
                else tr(R.string.toast_equip_optimal)
            )
        }
    }

    fun sellCommons() {
        viewModelScope.launch {
            var gold = 0
            container.gameRepository.mutate { gold = GameEngine.sellCommons(it) }
            _toasts.emit(
                if (gold > 0) tr(R.string.toast_sold_commons, gold) else tr(R.string.toast_no_commons)
            )
        }
    }

    fun toggleLock(item: Item) {
        viewModelScope.launch {
            container.gameRepository.mutate { GameEngine.toggleLock(it, item.id) }
            val updated = GameEngine.findItem(container.gameRepository.state.value, item.id)
            _ui.update { it.copy(selectedItem = updated) }
        }
    }

    fun fuse(item: Item) {
        viewModelScope.launch {
            var fused: Item? = null
            container.gameRepository.mutate { fused = GameEngine.fuseItem(it, item.id) }
            _ui.update { it.copy(selectedItem = null, selectedStashItem = null) }
            _toasts.emit(
                if (fused != null) tr(R.string.toast_fused)
                else tr(R.string.toast_fuse_requirements)
            )
        }
    }

    fun withdrawStash() {
        viewModelScope.launch {
            var moved = 0
            val stashWasEmpty = game.value.stash.isEmpty()
            container.gameRepository.mutate { moved = GameEngine.withdrawStash(it) }
            _toasts.emit(
                when {
                    moved > 0 -> tr(R.string.toast_stash_taken, moved)
                    stashWasEmpty -> tr(R.string.toast_stash_full_empty)
                    else -> tr(R.string.toast_stash_no_room)
                }
            )
        }
    }

    fun withdrawStashItem(item: Item) {
        viewModelScope.launch {
            var ok = false
            container.gameRepository.mutate { ok = GameEngine.withdrawStashItem(it, item.id) }
            if (ok) _ui.update { it.copy(selectedStashItem = null) }
            _toasts.emit(
                if (ok) tr(
                    R.string.toast_item_taken,
                    itemName(container.context.resources, item.name, item.effect)
                ) else tr(R.string.toast_inventory_full)
            )
        }
    }

    fun selectStashItem(item: Item) {
        _ui.update { it.copy(selectedStashItem = item, selectedItem = null) }
    }

    fun dismissStashItem() {
        _ui.update { it.copy(selectedStashItem = null) }
    }

    fun sendToStash(item: Item) {
        viewModelScope.launch {
            var ok = false
            container.gameRepository.mutate { ok = GameEngine.sendToStash(it, item.id) }
            if (ok) _ui.update { it.copy(selectedItem = null) }
            _toasts.emit(
                if (ok) tr(
                    R.string.toast_sent_to_stash,
                    itemName(container.context.resources, item.name, item.effect)
                ) else tr(R.string.toast_stash_failed)
            )
        }
    }

    fun sellStashItem(item: Item) {
        viewModelScope.launch {
            var ok = false
            container.gameRepository.mutate { ok = GameEngine.sellStashItem(it, item.id) }
            if (ok) _ui.update { it.copy(selectedStashItem = null) }
            _toasts.emit(
                if (ok) tr(
                    R.string.toast_sold,
                    itemName(container.context.resources, item.name, item.effect)
                ) else tr(R.string.toast_locked)
            )
        }
    }

    fun dismantleStashItem(item: Item) {
        viewModelScope.launch {
            var shards = 0
            container.gameRepository.mutate { shards = GameEngine.dismantleStashItem(it, item.id) }
            if (shards > 0) _ui.update { it.copy(selectedStashItem = null) }
            _toasts.emit(
                if (shards > 0) tr(R.string.toast_dismantled, shards) else tr(R.string.toast_locked)
            )
        }
    }

    private fun refreshSelected(itemId: String) {
        val state = container.gameRepository.state.value
        _ui.update { it.copy(selectedItem = GameEngine.findItem(state, itemId)) }
    }

    fun identifyItem(item: Item) {
        viewModelScope.launch {
            var ok = false
            container.gameRepository.mutate { ok = GameEngine.identifyItem(it, item.id) }
            refreshSelected(item.id)
            _toasts.emit(if (ok) tr(R.string.toast_identified) else "1 éclat requis.")
        }
    }

    fun rerollEffect(item: Item) {
        viewModelScope.launch {
            var outcome: ForgeOutcome = ForgeOutcome.NotFound
            container.gameRepository.mutate { outcome = GameEngine.rerollEffectDetailed(it, item.id) }
            refreshSelected(item.id)
            _toasts.emit(forgeMessage(outcome, tr(R.string.toast_rerolled)))
        }
    }

    fun upgradeItem(item: Item) {
        viewModelScope.launch {
            var outcome: ForgeOutcome = ForgeOutcome.NotFound
            container.gameRepository.mutate { outcome = GameEngine.upgradeItemDetailed(it, item.id) }
            refreshSelected(item.id)
            _toasts.emit(forgeMessage(outcome, tr(R.string.toast_upgraded)))
        }
    }

    /** Message de forge : n'annonce que ce qui manque vraiment. */
    private fun forgeMessage(outcome: ForgeOutcome, success: String): String = when (outcome) {
        ForgeOutcome.Success -> success
        ForgeOutcome.NotFound -> tr(R.string.toast_not_found)
        ForgeOutcome.NotIdentified -> tr(R.string.toast_not_identified)
        ForgeOutcome.Locked -> tr(R.string.toast_locked)
        ForgeOutcome.MaxLevel -> tr(R.string.toast_max_level)
        is ForgeOutcome.Missing -> {
            val missing = buildList {
                if (outcome.gold > 0) add(tr(R.string.toast_missing_gold, formatInt(outcome.gold)))
                if (outcome.shards > 0) {
                    add(
                        if (outcome.shards > 1) tr(R.string.toast_missing_shards, outcome.shards)
                        else tr(R.string.toast_one_shard)
                    )
                }
            }
            tr(R.string.toast_missing, missing.joinToString(tr(R.string.overlay_and)))
        }
    }

    private fun tr(@StringRes id: Int, vararg args: Any): String =
        container.context.getString(id, *args)

    fun dismantleItem(item: Item) {
        viewModelScope.launch {
            var shards = 0
            container.gameRepository.mutate { shards = GameEngine.dismantleItem(it, item.id) }
            _ui.update { it.copy(selectedItem = null, selectedStashItem = null) }
            _toasts.emit(
                if (shards > 0) tr(R.string.toast_dismantled, shards) else tr(R.string.toast_locked)
            )
        }
    }

    fun freeRest() {
        viewModelScope.launch {
            var ok = false
            container.gameRepository.mutate { ok = GameEngine.freeRest(it) }
            _toasts.emit(if (ok) tr(R.string.toast_vigil) else tr(R.string.toast_vigil_used))
        }
    }

    fun resolveSpecial(accept: Boolean) {
        viewModelScope.launch {
            var outcome: SpecialOutcome? = null
            container.gameRepository.mutate { outcome = GameEngine.resolveSpecial(it, accept) }
            outcome?.let {
                SoundFx.play(
                    container.context,
                    if (it.good) "chest" else "ui_tap",
                    if (it.good) 0.9f else 0.7f
                )
            }
            _ui.update { it.copy(specialOutcome = outcome) }
        }
    }

    fun dismissSpecialOutcome() {
        _ui.update { it.copy(specialOutcome = null) }
    }

    fun selectItem(item: Item) { _ui.update { it.copy(selectedItem = item) } }
    fun dismissItem() { _ui.update { it.copy(selectedItem = null, selectedStashItem = null) } }

    fun openChest() {
        if (game.value.chest != null) _ui.update { it.copy(chestOpen = true) }
    }

    fun closeChest() { _ui.update { it.copy(chestOpen = false) } }

    fun buyUpgrade(key: String) {
        viewModelScope.launch {
            var ok = false
            container.gameRepository.mutate { ok = GameEngine.buyUpgrade(it, key) }
            if (ok) SoundFx.play(container.context, "upgrade", 0.9f)
        }
    }

    fun rerollBossModifier() {
        viewModelScope.launch {
            var ok = false
            container.gameRepository.mutate { ok = GameEngine.rerollBossModifier(it) }
            _toasts.tryEmit(tr(if (ok) R.string.toast_reroll_ok else R.string.toast_reroll_fail))
        }
    }

    fun buyCampConsumable(id: String) {
        viewModelScope.launch {
            var ok = false
            container.gameRepository.mutate { ok = GameEngine.buyCampConsumable(it, id) }
            _toasts.tryEmit(tr(if (ok) R.string.toast_camp_buy_ok else R.string.toast_camp_buy_fail))
        }
    }

    fun respecCamp() {
        viewModelScope.launch {
            var refund = 0
            container.gameRepository.mutate { refund = GameEngine.respecCamp(it) }
            _toasts.tryEmit(
                if (refund > 0) tr(R.string.toast_respec_ok, refund)
                else tr(R.string.toast_respec_fail)
            )
        }
    }

    fun equip(item: Item) {
        viewModelScope.launch {
            var ok = false
            container.gameRepository.mutate { ok = GameEngine.equipItem(it, item.id) }
            _ui.update { it.copy(selectedItem = null, selectedStashItem = null) }
            if (ok) _toasts.tryEmit(
                tr(R.string.toast_equipped, itemName(container.context.resources, item.name, item.effect))
            )
        }
    }

    fun sell(item: Item) {
        viewModelScope.launch {
            var ok = false
            container.gameRepository.mutate { ok = GameEngine.sellItem(it, item.id) }
            _ui.update { it.copy(selectedItem = null, selectedStashItem = null) }
            if (ok) _toasts.tryEmit(
                tr(R.string.toast_sold, itemName(container.context.resources, item.name, item.effect))
            )
        }
    }

    fun rest() {
        viewModelScope.launch {
            var ok = false
            container.gameRepository.mutate { ok = GameEngine.rest(it) }
            _toasts.tryEmit(if (ok) tr(R.string.toast_rest_ok) else tr(R.string.toast_rest_fail))
        }
    }

    fun shardHeal() {
        viewModelScope.launch {
            var ok = false
            container.gameRepository.mutate { ok = GameEngine.shardHeal(it) }
            _toasts.tryEmit(if (ok) tr(R.string.toast_hp_restored) else tr(R.string.toast_heal_fail))
        }
    }

    fun forceChest() {
        viewModelScope.launch {
            var ok = false
            container.gameRepository.mutate { ok = GameEngine.forceChest(it) }
            if (!ok) _toasts.tryEmit(tr(R.string.toast_force_fight))
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

    fun debugAddReserveSteps(steps: Int) {
        viewModelScope.launch {
            container.gameRepository.debugAddSteps(steps)
            _toasts.emit(tr(R.string.toast_debug_steps_added, formatInt(steps)))
        }
    }

    fun upgrades(): Upgrades = game.value.upgrades

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            GameViewModel(container) as T
    }
}

