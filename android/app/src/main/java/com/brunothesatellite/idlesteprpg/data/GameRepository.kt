package com.brunothesatellite.idlesteprpg.data

import android.content.Context
import com.brunothesatellite.idlesteprpg.engine.GameEngine
import com.brunothesatellite.idlesteprpg.engine.GameState
import com.brunothesatellite.idlesteprpg.engine.OfflineReport
import com.brunothesatellite.idlesteprpg.engine.deepCopy
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

class GameRepository(private val context: Context) {

    private val file = File(context.filesDir, "game_state.json")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }
    private val mutex = Mutex()
    private val _state = MutableStateFlow(load())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private fun load(): GameState {
        val loaded = runCatching {
            if (file.exists()) json.decodeFromString<GameState>(file.readText()) else GameState()
        }.getOrElse {
            runCatching {
                if (file.exists()) file.copyTo(File(context.filesDir, "game_state.corrupt.json"), overwrite = true)
            }
            GameState()
        }
        if (loaded.equipment.isEmpty()) {
            val defaults = GameState()
            loaded.equipment = defaults.equipment
            loaded.inventory = if (loaded.inventory.isEmpty()) defaults.inventory else loaded.inventory
        }
        GameEngine.ensureDay(loaded)
        return loaded
    }

    suspend fun tick(dtMs: Double) {
        mutex.withLock {
            val s = _state.value.deepCopy()
            GameEngine.tick(s, dtMs)
            _state.value = s
        }
    }

    suspend fun mutate(block: (GameState) -> Unit) {
        mutex.withLock {
            val s = _state.value.deepCopy()
            block(s)
            _state.value = s
        }
    }

    suspend fun ingestSteps(total: Int, date: LocalDate) {
        mutex.withLock {
            val s = _state.value.deepCopy()
            GameEngine.ingestSteps(s, total, date)
            _state.value = s
            saveLocked(s)
        }
    }

    suspend fun ingestHistory(days: Map<String, Int>, today: LocalDate): OfflineReport? {
        var report: OfflineReport? = null
        mutex.withLock {
            val s = _state.value.deepCopy()
            report = GameEngine.ingestHistory(s, days, today)
            _state.value = s
            saveLocked(s)
        }
        return report
    }

    suspend fun debugAddSteps(steps: Int) {
        mutex.withLock {
            val s = _state.value.deepCopy()
            GameEngine.debugAddSteps(s, steps)
            _state.value = s
        }
    }

    suspend fun save() {
        val snapshot = _state.value.deepCopy()
        withContext(Dispatchers.IO) { saveLocked(snapshot) }
    }

    private fun saveLocked(snapshot: GameState) {
        runCatching {
            val tmp = File(context.filesDir, "game_state.json.tmp")
            tmp.writeText(json.encodeToString(snapshot))
            if (file.exists()) file.delete()
            if (!tmp.renameTo(file)) {
                tmp.copyTo(file, overwrite = true)
                tmp.delete()
            }
        }
    }
}

