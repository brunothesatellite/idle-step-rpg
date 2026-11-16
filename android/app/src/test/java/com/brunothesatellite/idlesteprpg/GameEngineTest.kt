package com.brunothesatellite.idlesteprpg

import com.brunothesatellite.idlesteprpg.engine.GameEngine
import com.brunothesatellite.idlesteprpg.engine.GameState
import com.brunothesatellite.idlesteprpg.engine.statsOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GameEngineTest {

    @Test
    fun ingestStepsIsIdempotent() {
        val state = GameState()
        val day = LocalDate.of(2026, 11, 16)
        GameEngine.ingestSteps(state, 500, day)
        assertEquals(500, state.stepsToday)
        assertEquals(500.0, state.stepBank, 0.001)
        assertEquals(10, state.ferveur)

        GameEngine.ingestSteps(state, 500, day)
        assertEquals(500.0, state.stepBank, 0.001)

        GameEngine.ingestSteps(state, 700, day)
        assertEquals(700.0, state.stepBank, 0.001)
        assertEquals(6, state.rations)
    }

    @Test
    fun dayChangeResetsDailyCounters() {
        val state = GameState()
        val day1 = LocalDate.of(2026, 11, 16)
        val day2 = LocalDate.of(2026, 11, 17)
        GameEngine.ensureDay(state, day1)
        state.stepsToday = 3200
        state.ferveur = 64
        state.processedSteps = 3200

        GameEngine.ensureDay(state, day2)
        assertEquals(0, state.stepsToday)
        assertEquals(0, state.ferveur)
        assertEquals(0, state.processedSteps)
        assertEquals(day2.toString(), state.day)
    }

    @Test
    fun baseStatsAreCoherent() {
        val state = GameState()
        val stats = statsOf(state)
        assertTrue(stats.maxHp > 0)
        assertTrue(stats.atk > 0)
        assertTrue(stats.vit >= 1.0)
        state.level = 5
        assertTrue(statsOf(state).maxHp > stats.maxHp)
    }
}
