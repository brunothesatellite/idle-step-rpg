package com.brunothesatellite.idlesteprpg

import com.brunothesatellite.idlesteprpg.engine.BalanceTelemetry
import com.brunothesatellite.idlesteprpg.engine.GameState
import com.brunothesatellite.idlesteprpg.ui.endingPreviewState
import com.brunothesatellite.idlesteprpg.ui.endingSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class EndingTest {

    @Test
    fun summaryCountsFinalSave() {
        val state = GameState(
            floor = 101,
            level = 100,
            totalSteps = 1_284_000,
            kills = 3_482,
            bosses = 124,
            historyStart = "2026-11-01",
            streak = 11,
            telemetry = BalanceTelemetry(
                normalFights = 3_412,
                normalWins = 3_356,
                bossFights = 151,
                bossWins = 124,
                combatCount = 3_563,
                combatMs = 136_819_200L,
                bossAttempts = mapOf(100 to 3),
                bossWinsByFloor = mapOf(100 to 1)
            )
        )

        val summary = endingSummary(state, LocalDate.of(2026, 11, 24))

        assertEquals(100, summary.floors)
        assertEquals(8, summary.biomes)
        assertEquals(24, summary.days)
        assertEquals(1_284_000, summary.steps)
        assertEquals(3_412, summary.normalFights)
        assertEquals(3_356, summary.normalWins)
        assertEquals(3, summary.floor100Attempts)
        assertEquals(1, summary.floor100Wins)
        assertEquals(38.4, summary.averageCombatSeconds, 0.01)
        assertTrue(summary.creaturesTotal > summary.creaturesFound)
        assertTrue(summary.guardiansTotal >= 20)
    }

    @Test
    fun emptySaveHasSafeDefaults() {
        val summary = endingSummary(GameState(historyStart = ""), LocalDate.of(2026, 11, 24))

        assertEquals(1, summary.floors)
        assertEquals(1, summary.days)
        assertEquals(0.0, summary.averageCombatSeconds, 0.0)
        assertEquals(0, summary.setsCompleted)
    }

    @Test
    fun previewFillsFinalCountersWithoutTouchingTheSave() {
        val state = GameState(level = 1, floor = 1)
        val preview = endingPreviewState(state)

        assertEquals(101, preview.floor)
        assertEquals(100, preview.level)
        assertEquals(20, preview.telemetry.bossAttempts.size)
        assertEquals(3, preview.telemetry.bossAttempts[100])
        assertEquals(1, preview.telemetry.bossWinsByFloor[100])
        assertEquals(1, state.floor)
        assertEquals(1, state.level)
    }
}
