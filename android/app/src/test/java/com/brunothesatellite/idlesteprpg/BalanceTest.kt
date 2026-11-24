package com.brunothesatellite.idlesteprpg

import com.brunothesatellite.idlesteprpg.engine.Balance
import com.brunothesatellite.idlesteprpg.engine.GameState
import com.brunothesatellite.idlesteprpg.engine.MAX_RATIONS
import com.brunothesatellite.idlesteprpg.engine.MAX_SHARDS
import com.brunothesatellite.idlesteprpg.engine.statsOf
import com.brunothesatellite.idlesteprpg.engine.upgradeCost
import com.brunothesatellite.idlesteprpg.engine.xpNext
import org.junit.Assert.assertEquals
import org.junit.Test

class BalanceTest {

    @Test
    fun currentXpCurveMatchesDocumentedValues() {
        assertEquals(120, xpNext(1))
        assertEquals(49_222, xpNext(34))
    }

    @Test
    fun campCostsMatchDocumentedValues() {
        assertEquals(45, upgradeCost("blade", 0))
        assertEquals(40, upgradeCost("breath", 0))
        assertEquals(70, upgradeCost("swift", 0))
        assertEquals(65, upgradeCost("blade", 2))
    }

    @Test
    fun currentCapsAreExposedThroughModelConstants() {
        assertEquals(24, MAX_SHARDS)
        assertEquals(24, MAX_RATIONS)
    }

    @Test
    fun heroBaseStatsMatchReference() {
        val state = GameState().copy(equipment = emptyList(), level = 1, floor = 1)
        val stats = statsOf(state)
        assertEquals(18, stats.atk)
        assertEquals(150, stats.maxHp)
        assertEquals(1.0, stats.vit, 0.0001)
    }

    @Test
    fun targetDecisionsAreLocked() {
        assertEquals(1.09, Balance.Target.XP_GROWTH, 0.0001)
        assertEquals(0.12, Balance.Target.XP_FLOOR_MULT, 0.0001)
        assertEquals(0.04, Balance.Target.ITEM_GROWTH, 0.0001)
        assertEquals(20, Balance.Target.ITEM_MAX_LEVELS)
        assertEquals(30, Balance.Target.RATIONS_CAP)
        assertEquals(0.40, Balance.Target.AUTO_RATION_THRESHOLD, 0.0001)
        assertEquals(0.25, Balance.Target.PITY_CAP, 0.0001)
        assertEquals(0.90, Balance.Target.FUSION_RATIO, 0.0001)
        assertEquals(6.0, Balance.Target.LOOT_SCALE_CAP, 0.0001)
    }
}
