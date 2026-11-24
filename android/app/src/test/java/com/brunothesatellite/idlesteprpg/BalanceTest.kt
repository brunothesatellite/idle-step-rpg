package com.brunothesatellite.idlesteprpg

import com.brunothesatellite.idlesteprpg.engine.Balance
import com.brunothesatellite.idlesteprpg.engine.GameEngine
import com.brunothesatellite.idlesteprpg.engine.GameState
import com.brunothesatellite.idlesteprpg.engine.Item
import com.brunothesatellite.idlesteprpg.engine.MAX_RATIONS
import com.brunothesatellite.idlesteprpg.engine.MAX_SHARDS
import com.brunothesatellite.idlesteprpg.engine.bossXpReward
import com.brunothesatellite.idlesteprpg.engine.bossAbilityFor
import com.brunothesatellite.idlesteprpg.engine.biomeBossFor
import com.brunothesatellite.idlesteprpg.engine.biomeRuleFor
import com.brunothesatellite.idlesteprpg.engine.endgameEnemyMult
import com.brunothesatellite.idlesteprpg.engine.isMaxLevel
import com.brunothesatellite.idlesteprpg.engine.lootFloorScale
import com.brunothesatellite.idlesteprpg.engine.maxUpgradeLevel
import com.brunothesatellite.idlesteprpg.engine.mobXpReward
import com.brunothesatellite.idlesteprpg.engine.rarityMultiplier
import com.brunothesatellite.idlesteprpg.engine.statsOf
import com.brunothesatellite.idlesteprpg.engine.upgradeCost
import com.brunothesatellite.idlesteprpg.engine.xpCatchUpMultiplier
import com.brunothesatellite.idlesteprpg.engine.xpFloorMultiplier
import com.brunothesatellite.idlesteprpg.engine.xpNext
import com.brunothesatellite.idlesteprpg.engine.CONSUMABLES
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

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
        assertEquals(30, MAX_RATIONS)
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
    fun lootScalingAndRarityFollowTarget() {
        assertEquals(1.0, lootFloorScale(1), 0.0001)
        assertEquals(2.925, lootFloorScale(36), 0.0001)
        assertEquals(6.0, lootFloorScale(100), 0.0001)
        assertEquals(2.4, rarityMultiplier("legendary"), 0.0001)
        assertEquals(1.15, rarityMultiplier("uncommon"), 0.0001)
    }

    @Test
    fun upgradeIsAdditiveAndCapped() {
        val item = Item(
            id = "test",
            name = "Test",
            slot = "weapon",
            rarity = "rare",
            icon = "item/long_sword3.png",
            level = 10,
            atk = 100,
            hp = 50,
            baseAtk = 100,
            baseHp = 50,
            maxLevel = 30
        )
        val next = GameEngine.upgradedItem(item)
        assertEquals(11, next.level)
        assertEquals(104, next.atk)
        assertEquals(52, next.hp)
        assertEquals(30, maxUpgradeLevel(item))
        assertFalse(isMaxLevel(next))
        assertTrue(isMaxLevel(item.copy(level = 30)))
        assertEquals(30, maxUpgradeLevel(item.copy(maxLevel = 0)))
    }

    @Test
    fun effectAndSpeedCapsAreApplied() {
        fun gear(effect: String, vit: Int = 0, crit: Int = 0): GameState = GameState().copy(
            level = 1,
            floor = 1,
            equipment = List(6) { index ->
                Item(
                    id = "cap-$effect-$index",
                    name = "Cap",
                    slot = "weapon",
                    rarity = "rare",
                    icon = "item/long_sword3.png",
                    level = 1,
                    vitPct = vit,
                    critPct = crit,
                    effect = effect
                )
            }
        )

        assertEquals(0.40, statsOf(gear("rempart")).damageReduction, 0.0001)
        assertEquals(0.25, statsOf(gear("vampirisme")).lifesteal, 0.0001)
        assertEquals(1.0, statsOf(gear("frenesie")).critDamage, 0.0001)
        assertEquals(0.5, statsOf(gear("chasse")).bossDamage, 0.0001)
        assertEquals(75, statsOf(gear("", vit = 100, crit = 20)).crit)
        assertEquals(4.0, statsOf(gear("", vit = 100)).vit, 0.0001)
    }

    @Test
    fun campCostScalesWithFloorAndCaps() {
        assertEquals(110, upgradeCost("blade", 0, 36))
        assertEquals(30, Balance.Target.MAX_CAMP_LEVEL)
    }

    @Test
    fun campBonusesArePercentOfBase() {
        val state = GameState().copy(
            equipment = emptyList(),
            level = 1,
            floor = 1,
            upgrades = com.brunothesatellite.idlesteprpg.engine.Upgrades(blade = 10, breath = 10, swift = 10)
        )
        val stats = statsOf(state)
        assertEquals(21, stats.atk)
        assertEquals(173, stats.maxHp)
        assertEquals(1.1, stats.vit, 0.0001)
    }

    @Test
    fun setBonusesArePercentOfCurrentStats() {
        val base = GameState()
        val setState = base.copy(equipment = base.equipment.map { it.copy(setName = "veilleur") })
        val before = statsOf(base)
        val after = statsOf(setState)
        assertEquals((before.atk * 1.12).roundToInt(), after.atk)
        assertEquals((before.maxHp * 1.12).roundToInt(), after.maxHp)
    }

    @Test
    fun biomeBossRotatesWithinEachBiome() {
        assertEquals("Hydre de Lerne", biomeBossFor(11).name)
        assertEquals("Grande naga", biomeBossFor(12).name)
        assertEquals("Manticore", biomeBossFor(13).name)
        assertEquals("Hydre mineure", biomeBossFor(14).name)
        assertEquals("Hydre de Lerne", biomeBossFor(15).name)
        assertEquals("Roi des Cryptes", biomeBossFor(1).name)
        assertEquals("Dragon d'os", biomeBossFor(2).name)
    }

    @Test
    fun freeRestAllowsTwoUsesPerDay() {
        val state = GameState().copy(level = 1, floor = 1, day = "2026-11-24", hp = 1.0)
        assertTrue(GameEngine.freeRest(state))
        assertTrue(GameEngine.freeRest(state))
        assertFalse(GameEngine.freeRest(state))
    }

    @Test
    fun heavyGuardianAbilitiesAvoidBurstModifiers() {
        val heavy = com.brunothesatellite.idlesteprpg.engine.GameEngine.bossModifierPool("slam")
        assertFalse(heavy.contains("rapide"))
        assertFalse(heavy.contains("berserk"))
        val light = com.brunothesatellite.idlesteprpg.engine.GameEngine.bossModifierPool("shield")
        assertTrue(light.contains("rapide"))
        assertTrue(light.contains("berserk"))
    }

    @Test
    fun endgameRampRisesFromEightyOneToHundred() {
        assertEquals(1.15, endgameEnemyMult(81), 0.0001)
        assertEquals(1.15, endgameEnemyMult(50), 0.0001)
        assertEquals(1.25, endgameEnemyMult(90), 0.01)
        assertEquals(1.35, endgameEnemyMult(100), 0.0001)
        assertEquals(1.35, endgameEnemyMult(160), 0.0001)
        assertTrue(endgameEnemyMult(100) > endgameEnemyMult(81))
    }

    @Test
    fun brokenThroneRuleIsHarderThanSoulPressure() {
        val throne = biomeRuleFor(80)
        val broken = biomeRuleFor(81)
        assertEquals("trone_broken", broken.id)
        assertTrue(broken.enemyHpMult > throne.enemyHpMult)
        assertTrue(broken.enemyAtkMult > throne.enemyAtkMult)
        assertTrue(broken.bossHpMult > 1.0)
        assertTrue(broken.bossAtkMult > 1.0)
        assertEquals(1.0, throne.bossHpMult, 0.0001)
        assertTrue(biomeRuleFor(100).enemyHpMult > broken.enemyHpMult)
    }

    @Test
    fun finalGuardianUsesThroneRupture() {
        assertEquals("pressure", bossAbilityFor("L'Âme Première", 99).id)
        assertEquals("rupture", bossAbilityFor("L'Âme Première", 100).id)
    }

    @Test
    fun everyEndgameFloorHasFiniteRule() {
        for (floor in 1..200) {
            val rule = biomeRuleFor(floor)
            assertTrue(rule.enemyHpMult.isFinite())
            assertTrue(rule.enemyAtkMult.isFinite())
            assertTrue(rule.bossHpMult.isFinite())
            assertTrue(rule.bossAtkMult.isFinite())
            assertTrue(rule.enemyHpMult >= 0.9)
            assertTrue(rule.enemyAtkMult >= 0.9)
        }
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
        assertEquals(0.03, Balance.Target.PITY_CAP, 0.0001)
        assertEquals(0.90, Balance.Target.FUSION_RATIO, 0.0001)
        assertEquals(6.0, Balance.Target.LOOT_SCALE_CAP, 0.0001)
    }

    @Test
    fun guardianCalibrationIsLocked() {
        assertEquals(18.0, Balance.Target.BOSS_TARGET_TTK, 0.0001)
        assertEquals(14.5, Balance.Target.BOSS_DIV_SHIELD, 0.0001)
        assertEquals(19.0, Balance.Target.BOSS_DIV_BREATH, 0.0001)
        assertEquals(16.0, Balance.Target.BOSS_DIV_PRESSURE, 0.0001)
        assertEquals(20.5, Balance.Target.BOSS_DIV_RUPTURE, 0.0001)
    }

    @Test
    fun attackPotionsCarryARealRisk() {
        val force = CONSUMABLES.first { it.id == "potion_force" }
        assertEquals("berserk", force.effect)
        assertEquals(0.40, force.secondary, 0.0001)
        val rage = CONSUMABLES.first { it.id == "fiole_rage" }
        assertEquals("berserk", rage.effect)
        assertEquals(0.50, rage.secondary, 0.0001)
    }
}
