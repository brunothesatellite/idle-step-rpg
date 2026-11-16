package com.brunothesatellite.idlesteprpg.data

import android.content.Context
import com.brunothesatellite.idlesteprpg.engine.GameEngine
import com.brunothesatellite.idlesteprpg.engine.GameState
import com.brunothesatellite.idlesteprpg.engine.OfflineReport
import com.brunothesatellite.idlesteprpg.engine.SAVE_VERSION
import com.brunothesatellite.idlesteprpg.engine.deepCopy
import com.brunothesatellite.idlesteprpg.notify.GameNotifications
import com.brunothesatellite.idlesteprpg.widget.MarcheWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate

data class SaveSummary(
    val level: Int,
    val floor: Int,
    val gold: Int,
    val day: String,
    val items: Int,
    val version: Int
)

class GameRepository(private val context: Context) {

    private val file = File(context.filesDir, "game_state.json")
    private val backup = File(context.filesDir, "game_state.bak.json")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }
    private val mutex = Mutex()
    private var lastBackupAt = 0L
    private var lastWidgetAt = 0L
    private val _state = MutableStateFlow(load())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private fun load(): GameState {
        val loaded = runCatching {
            if (file.exists()) json.decodeFromString<GameState>(file.readText()) else GameState()
        }.getOrElse {
            runCatching {
                if (file.exists()) file.copyTo(File(context.filesDir, "game_state.corrupt.json"), overwrite = true)
            }
            runCatching {
                if (backup.exists()) json.decodeFromString<GameState>(backup.readText()) else null
            }.getOrNull() ?: GameState()
        }
        if (loaded.equipment.isEmpty()) {
            val defaults = GameState()
            loaded.equipment = defaults.equipment
            loaded.inventory = if (loaded.inventory.isEmpty()) defaults.inventory else loaded.inventory
        }
        GameEngine.migrate(loaded)
        GameEngine.ensureDay(loaded)
        return loaded
    }

    suspend fun tick(dtMs: Double) {
        val before = _state.value
        var after: GameState = before
        var warnings: List<String> = emptyList()
        mutex.withLock {
            val s = before.deepCopy()
            GameEngine.tick(s, dtMs)
            applyAutoSell(s)
            warnings = GameEngine.chestWarnings(s)
            _state.value = s
            after = s
            if (warnings.isNotEmpty()) saveLocked(s)
        }
        GameNotifications.diff(context, before, after)
        warnings.forEach { GameNotifications.chestWarning(context, it) }
    }

    suspend fun mutate(block: (GameState) -> Unit) {
        val before = _state.value
        var after: GameState = before
        mutex.withLock {
            val s = before.deepCopy()
            block(s)
            applyAutoSell(s)
            _state.value = s
            after = s
        }
        GameNotifications.diff(context, before, after)
    }

    suspend fun ingestSteps(total: Int, date: LocalDate) {
        ingestHistory(mapOf(date.toString() to total), date)
    }

    suspend fun ingestHistory(days: Map<String, Int>, today: LocalDate): OfflineReport? {
        val before = _state.value
        var report: OfflineReport? = null
        var after: GameState = before
        var warnings: List<String> = emptyList()
        mutex.withLock {
            val s = before.deepCopy()
            report = GameEngine.ingestHistory(s, days, today)
            val rawReport = report
            if (rawReport != null) {
                val sim = GameEngine.simulateOffline(s)
                report = rawReport.copy(
                    combats = sim.combats,
                    gold = sim.gold,
                    floors = sim.floors,
                    kills = sim.kills
                )
                s.offlineReport = report
            }
            applyAutoSell(s)
            warnings = GameEngine.chestWarnings(s)
            _state.value = s
            after = s
            saveLocked(s)
        }
        GameNotifications.diff(context, before, after)
        warnings.forEach { GameNotifications.chestWarning(context, it) }
        return report
    }

    suspend fun debugAddSteps(steps: Int) {
        val before = _state.value
        var after: GameState = before
        mutex.withLock {
            val s = before.deepCopy()
            GameEngine.debugAddSteps(s, steps)
            applyAutoSell(s)
            _state.value = s
            after = s
        }
        GameNotifications.diff(context, before, after)
    }

    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        json.encodeToString(_state.value.deepCopy())
    }

    suspend fun inspectJson(text: String): SaveSummary? = withContext(Dispatchers.IO) {
        val decoded = runCatching { json.decodeFromString<GameState>(text) }.getOrNull()
            ?: return@withContext null
        if (!isValid(decoded)) return@withContext null
        GameEngine.migrate(decoded)
        SaveSummary(
            level = decoded.level,
            floor = decoded.floor,
            gold = decoded.gold,
            day = decoded.day,
            items = decoded.inventory.size + decoded.stash.size,
            version = decoded.version
        )
    }

    suspend fun importJson(text: String): Boolean = withContext(Dispatchers.IO) {
        val decoded = runCatching { json.decodeFromString<GameState>(text) }.getOrNull()
            ?: return@withContext false
        if (!isValid(decoded)) return@withContext false
        mutex.withLock {
            GameEngine.migrate(decoded)
            if (decoded.equipment.isEmpty()) {
                val defaults = GameState()
                decoded.equipment = defaults.equipment
                if (decoded.inventory.isEmpty()) decoded.inventory = defaults.inventory
            }
            GameEngine.ensureDay(decoded)
            GameEngine.expireChests(decoded)
            _state.value = decoded
            saveLocked(decoded)
        }
        true
    }

    private fun isValid(state: GameState): Boolean {
        if (state.version > SAVE_VERSION) return false
        if (state.level !in 1..2_000) return false
        if (state.floor !in 1..200_000) return false
        if (state.gold < 0 || state.stepsToday < 0 || state.stepBank < 0) return false
        if (state.inventory.size > 500 || state.stash.size > 500) return false
        if (state.day.isNotBlank() && runCatching { LocalDate.parse(state.day) }.isFailure) return false
        val slots = setOf("weapon", "head", "body", "hands", "feet", "talisman")
        if (state.equipment.any { it.slot !in slots }) return false
        if (state.inventory.any { it.slot !in slots }) return false
        return true
    }

    suspend fun maintenance() {
        var warnings: List<String> = emptyList()
        mutex.withLock {
            val s = _state.value.deepCopy()
            GameEngine.expireChests(s)
            warnings = GameEngine.chestWarnings(s)
            _state.value = s
            saveLocked(s)
        }
        warnings.forEach { GameNotifications.chestWarning(context, it) }
    }

    private fun autoSellEnabled(): Boolean =
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .getBoolean("auto_sell_commons", false)

    private fun applyAutoSell(state: GameState) {
        if (!autoSellEnabled()) return
        GameEngine.sellCommons(state)
    }

    suspend fun reset() {
        mutex.withLock {
            val fresh = GameState()
            GameEngine.ensureDay(fresh)
            _state.value = fresh
            saveLocked(fresh)
        }
    }

    suspend fun save() {
        val snapshot = _state.value.deepCopy()
        withContext(Dispatchers.IO) { saveLocked(snapshot) }
    }

    private fun saveLocked(snapshot: GameState) {
        runCatching {
            val now = System.currentTimeMillis()
            if (file.exists() && file.length() > 0 && now - lastBackupAt > 10 * 60_000L) {
                file.copyTo(backup, overwrite = true)
                lastBackupAt = now
            }
            val tmp = File(context.filesDir, "game_state.json.tmp")
            tmp.writeText(json.encodeToString(snapshot))
            if (file.exists()) file.delete()
            if (!tmp.renameTo(file)) {
                tmp.copyTo(file, overwrite = true)
                tmp.delete()
            }
            context.getSharedPreferences("widget", Context.MODE_PRIVATE).edit()
                .putInt("steps", snapshot.stepsToday)
                .putInt("level", snapshot.level)
                .putInt("floor", snapshot.floor)
                .putInt("gold", snapshot.gold)
                .apply()
            if (now - lastWidgetAt > 60_000L) {
                lastWidgetAt = now
                MarcheWidgetProvider.updateAll(context)
            }
        }
    }
}

