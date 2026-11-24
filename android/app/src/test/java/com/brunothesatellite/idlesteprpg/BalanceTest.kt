package com.brunothesatellite.idlesteprpg

import com.brunothesatellite.idlesteprpg.engine.Balance
import com.brunothesatellite.idlesteprpg.engine.GameState
import com.brunothesatellite.idlesteprpg.engine.MAX_RATIONS
import com.brunothesatellite.idlesteprpg.engine.MAX_SHARDS
import com.brunothesatellite.idlesteprpg.engine.bossXpReward
import com.brunothesatellite.idlesteprpg.engine.mobXpReward
import com.brunothesatellite.idlesteprpg.engine.statsOf
import com.brunothesatellite.idlesteprpg.engine.upgradeCost
import com.brunothesatellite.idlesteprpg.engine.xpCatchUpMultiplier
import com.brunothesatellite.idlesteprpg.engine.xpFloorMultiplier
import com.brunothesatellite.idlesteprpg.engine.xpNext
import org.junit.Assert.assertEquals
import org.junit.Test

class BalanceTest {

    @Test
    fun xpCurveMatchesTarget() {
        assertEquals(120, xpNext(1))
        assertEquals(2_062, xpNext(34))
        assertEquals(608_702, xpNext(100))
    }

    @Test
    fun xpRewardsScaleWithFloorAndCatchUp() {
        assertEquals(1.0, xpFloorMultiplier(1), 0.0001)
        assertEquals(5.2, xpFloorMultiplier(36), 0.0001)
        assertEquals(1.0, xpCatchUpMultiplier(34, 36), 0.0001)
        assertEquals(1.25, xpCatchUpMultiplier(20, 36), 0.0001)
        assertEquals(998, mobXpReward(enemyLevel = 36, floor = 36, playerLevel = 34, elite = false))
        assertEquals(1_248, mobXpReward(enemyLevel = 36, floor = 36, playerLevel = 20, elite = false))
        assertEquals(10_504, bossXpReward(enemyLevel = 38, floor = 36, playerLevel = 34))
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
        assertEquals(4, Balance.Target.XP_CATCHUP_LEVEL_GAP)
        assertEquals(1.25, Balance.Target.XP_CATCHUP_MULT, 0.0001)
        assertEquals(0.04, Balance.Target.ITEM_GROWTH, 0.0001)
        assertEquals(20, Balance.Target.ITEM_MAX_LEVELS)
        assertEquals(30, Balance.Target.RATIONS_CAP)
        assertEquals(0.40, Balance.Target.AUTO_RATION_THRESHOLD, 0.0001)
        assertEquals(0.25, Balance.Target.PITY_CAP, 0.0001)
        assertEquals(0.90, Balance.Target.FUSION_RATIO, 0.0001)
        assertEquals(6.0, Balance.Target.LOOT_SCALE_CAP, 0.0001)
    }
}
