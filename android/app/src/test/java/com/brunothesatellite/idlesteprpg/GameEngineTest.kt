package com.brunothesatellite.idlesteprpg

import com.brunothesatellite.idlesteprpg.engine.GameEngine
import com.brunothesatellite.idlesteprpg.engine.GameState
import com.brunothesatellite.idlesteprpg.engine.INVENTORY_CAP
import com.brunothesatellite.idlesteprpg.engine.Item
import com.brunothesatellite.idlesteprpg.engine.MAX_STEPS_PER_DAY
import com.brunothesatellite.idlesteprpg.engine.SpecialData
import com.brunothesatellite.idlesteprpg.engine.oathsFor
import com.brunothesatellite.idlesteprpg.engine.statsOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

    @Test
    fun historyImportIsIdempotentAndReported() {
        val state = GameState()
        val today = LocalDate.of(2026, 11, 16)
        val days = mapOf(
            "2026-11-14" to 4_000,
            "2026-11-15" to 5_000,
            "2026-11-16" to 2_500
        )
        val report = GameEngine.ingestHistory(state, days, today)
        assertNotNull(report)
        assertEquals(2, report!!.days)
        assertEquals(9_000, report.steps)
        assertEquals(2_500, state.stepsToday)
        assertEquals(11_500.0, state.stepBank, 0.001)

        val again = GameEngine.ingestHistory(state, days, today)
        assertNull(again)
        assertEquals(11_500.0, state.stepBank, 0.001)
        assertEquals(0, state.lastAdded)
    }

    @Test
    fun historyImportIsCappedButLedgerAdvances() {
        val state = GameState()
        val today = LocalDate.of(2026, 11, 16)
        val days = (1..6).associate {
            "2026-11-${it.toString().padStart(2, '0')}" to 20_000
        }
        val report = GameEngine.ingestHistory(state, days, today)
        assertNotNull(report)
        assertEquals(100_000, report!!.steps)
        assertEquals(100_000.0, state.stepBank, 0.001)

        val again = GameEngine.ingestHistory(state, days, today)
        assertNull(again)
        assertEquals(100_000.0, state.stepBank, 0.001)
    }

    @Test
    fun marathonDayIsImportedWithoutTruncation() {
        val state = GameState()
        val today = LocalDate.of(2026, 11, 16)
        val report = GameEngine.ingestHistory(
            state,
            mapOf("2026-11-15" to 45_000),
            today
        )
        assertNotNull(report)
        assertEquals(45_000, report!!.steps)
        assertEquals(45_000.0, state.stepBank, 0.001)
    }

    @Test
    fun dailyStepsAreClamped() {
        val state = GameState()
        val today = LocalDate.of(2026, 11, 16)
        GameEngine.ingestSteps(state, MAX_STEPS_PER_DAY + 25_000, today)
        assertEquals(MAX_STEPS_PER_DAY, state.stepsToday)
    }

    @Test
    fun dailyOathsGrantRewardsAutomatically() {
        val state = GameState()
        val today = LocalDate.of(2026, 11, 16)
        state.genRations = 24
        state.genShards = 12
        val gold = state.gold
        val shards = state.shards
        GameEngine.ingestSteps(state, 3_000, today)
        assertTrue(state.oaths.stepsClaimed)
        assertEquals(gold + 150, state.gold)
        assertEquals(shards + 1, state.shards)
        assertTrue(state.journal.any { it.text.contains("Serment de marche") })
    }

    @Test
    fun dayChangeRenewsOathsAndCounters() {
        val state = GameState()
        val day1 = LocalDate.of(2026, 11, 16)
        val day2 = LocalDate.of(2026, 11, 17)
        GameEngine.ingestSteps(state, 3_000, day1)
        assertTrue(state.oaths.stepsClaimed)
        GameEngine.ensureDay(state, day2)
        assertEquals(false, state.oaths.stepsClaimed)
        assertEquals(0, state.bossesToday)
        assertEquals(0, state.chestsToday)
        assertEquals(3_000, state.stepLedger["2026-11-16"])
    }

    @Test
    fun chestQueueIsLimitedAndExpires() {
        val state = GameState()
        assertTrue(GameEngine.spawnChest(state))
        assertTrue(GameEngine.spawnChest(state))
        assertEquals(1, state.chestQueue.size)
        assertEquals(false, GameEngine.spawnChest(state))

        val first = state.chest!!
        state.chest = first.copy(expiresAt = System.currentTimeMillis() - 1_000)
        GameEngine.expireChests(state)
        assertNotNull(state.chest)
        assertTrue(state.chestQueue.isEmpty())
        assertTrue(state.journal.any { it.text.contains("disparu après 24 h") })
    }

    @Test
    fun ferveurDecaysAfterTwoHoursWithoutSteps() {
        val state = GameState()
        state.ferveur = 20
        state.lastStepAt = System.currentTimeMillis() - 2L * 60L * 60L * 1000L - 10 * 60_000L
        GameEngine.tick(state, 100.0)
        assertTrue(state.ferveur < 20)
    }

    @Test
    fun equipBestSelectsStrongestPerSlot() {
        val state = GameState()
        val strong = Item(
            id = "strong_weapon",
            name = "Lame de test",
            slot = "weapon",
            rarity = "legendary",
            icon = "item/long_sword3.png",
            level = 10,
            atk = 120,
            effect = "vampirisme",
            value = 999
        )
        state.inventory = state.inventory + strong
        val equippedBefore = state.equipment.first { it.slot == "weapon" }.id
        val swaps = GameEngine.equipBest(state)
        assertTrue(swaps >= 1)
        assertEquals("strong_weapon", state.equipment.first { it.slot == "weapon" }.id)
        assertTrue(state.inventory.any { it.id == equippedBefore })
    }

    @Test
    fun effectStatsAreAggregated() {
        val state = GameState()
        state.equipment = state.equipment.map {
            if (it.slot == "weapon") it.copy(effect = "vampirisme") else it
        } + Item(
            id = "guard",
            name = "Rempart",
            slot = "body",
            rarity = "epic",
            icon = "item/plate1.png",
            level = 5,
            hp = 40,
            effect = "rempart",
            value = 100
        )
        val stats = statsOf(state)
        assertTrue(stats.lifesteal > 0)
        assertTrue(stats.damageReduction > 0)
    }

    @Test
    fun lockedItemsAreNotSoldAndCommonsCanBeBulkSold() {
        val state = GameState()
        state.inventory = listOf(
            Item("common_locked", "Commun verrouillé", "weapon", "common", "item/spear2.png", 1, atk = 2, value = 10, locked = true),
            Item("common_free", "Commun libre", "weapon", "common", "item/spear2.png", 1, atk = 2, value = 12)
        )
        assertEquals(false, GameEngine.sellItem(state, "common_locked"))
        val gold = GameEngine.sellCommons(state)
        assertEquals(12, gold)
        assertTrue(state.inventory.any { it.id == "common_locked" })
        assertTrue(state.inventory.none { it.id == "common_free" })
    }

    @Test
    fun fusionNeedsThreeAndRaisesRarity() {
        val state = GameState()
        state.inventory = (1..3).map {
            Item("rare_$it", "Rare $it", "weapon", "rare", "item/mace2.png", 3, atk = 10 + it, value = 50)
        }
        val fused = GameEngine.fuseItem(state, "rare_1")
        assertNotNull(fused)
        assertEquals("epic", fused!!.rarity)
        assertEquals(1, state.inventory.size)
        assertEquals(fused.id, state.inventory.first().id)
    }

    @Test
    fun fullInventoryGoesToCampStash() {
        val state = GameState()
        state.inventory = (1..INVENTORY_CAP).map {
            Item("fill_$it", "Objet $it", "talisman", "common", "item/ring-regen.png", 1, value = 1)
        }
        GameEngine.addLoot(
            state,
            Item("overflow", "Trop-plein", "talisman", "rare", "item/ring-regen.png", 1, value = 20)
        )
        assertEquals(INVENTORY_CAP, state.inventory.size)
        assertEquals(1, state.stash.size)
        val moved = GameEngine.withdrawStash(state)
        assertEquals(0, moved)
    }

    @Test
    fun specialEncounterResolvesStatue() {
        val state = GameState()
        state.gold = 200
        state.mode = "special"
        state.special = SpecialData(
            type = "statue",
            title = "STATUE",
            text = "Test",
            acceptLabel = "PRIER",
            goldCost = 100,
            rewardText = "+15 ferveur"
        )
        val outcome = GameEngine.resolveSpecial(state, true)
        assertTrue(outcome.good)
        assertEquals(100, state.gold)
        assertEquals(15, state.ferveur)
        assertNull(state.special)
        assertEquals("walk", state.mode)
    }

    @Test
    fun gambleAnnouncesItsResult() {
        val state = GameState()
        state.gold = 200
        state.mode = "special"
        state.special = SpecialData(
            type = "risque",
            title = "COFFRE SCELLÉ",
            text = "Test",
            acceptLabel = "PARIER",
            goldCost = 100,
            rewardText = "300 or ou rien"
        )
        val outcome = GameEngine.resolveSpecial(state, true)
        assertTrue(outcome.text.startsWith("Pari"))
        assertTrue(outcome.text == "Pari gagné · +300 or" || outcome.text == "Pari perdu · −100 or")
        assertNull(state.special)
    }

    @Test
    fun dailyOathsVaryWithTheDate() {
        val seen = (1..40).map { oathsFor(LocalDate.of(2026, 1, 1).plusDays(it.toLong())).stepsTarget }.toSet()
        assertTrue(seen.all { it in setOf(2_500, 3_000, 3_500, 4_000) })
        assertTrue(seen.size > 1)
    }

    @Test
    fun campResetsTheCurrentFight() {
        val state = GameState()
        GameEngine.startBoss(state)
        val fight = state.fight!!
        fight.enemyHp = 10.0
        fight.enemyTimerMs = 1.0
        GameEngine.prepareCamp(state)
        assertEquals(fight.enemyMaxHp, state.fight!!.enemyHp, 0.001)
        assertEquals(380.0, state.fight!!.heroTimerMs, 0.001)
        assertTrue(state.journal.any { it.text.contains("combat réinitialisé") })
    }

    @Test
    fun offlineSimulationConsumesTheBank() {
        val state = GameState()
        state.stepBank = 5_000.0
        state.distance = 0.0
        state.nextFight = 10
        val sim = GameEngine.simulateOffline(state, maxIterations = 200)
        assertTrue(sim.combats >= 0)
        assertTrue(state.stepBank < 5_000.0)
    }

    @Test
    fun streakAndWeeklyStepsAdvance() {
        val state = GameState()
        val day1 = LocalDate.of(2026, 11, 16)
        val day2 = LocalDate.of(2026, 11, 17)
        GameEngine.ensureDay(state, day1)
        GameEngine.ingestSteps(state, 1_000, day1)
        assertEquals(1_000, state.weeklySteps)
        assertEquals(1_000, state.totalSteps)
        GameEngine.ensureDay(state, day2)
        assertEquals(2, state.streak)
    }

    @Test
    fun stepRateIsCappedPerSync() {
        val state = GameState()
        state.lastSync = System.currentTimeMillis()
        state.lastStepAt = state.lastSync
        GameEngine.ingestSteps(state, 12_000, LocalDate.of(2026, 11, 16))
        assertTrue(state.stepBank <= 5_000.0)
    }

    @Test
    fun forgeOperationsWork() {
        val state = GameState()
        state.shards = 12
        state.gold = 10_000
        state.bosses = 1
        state.inventory = listOf(
            Item("forge", "Lame", "weapon", "rare", "item/long_sword3.png", 3, atk = 12, value = 80)
        )
        assertTrue(GameEngine.rerollEffect(state, "forge"))
        val upgradedBefore = state.inventory.first().level
        assertTrue(GameEngine.upgradeItem(state, "forge"))
        assertEquals(upgradedBefore + 1, state.inventory.first().level)
        val shardsBefore = state.shards
        val gained = GameEngine.dismantleItem(state, "forge")
        assertEquals(3, gained)
        assertTrue(state.shards >= shardsBefore)
        assertTrue(state.inventory.isEmpty())
    }

    @Test
    fun unidentifiedItemCannotBeEquipped() {
        val state = GameState()
        val mystery = Item(
            "mystery", "Mystère", "weapon", "epic", "item/mace2.png", 3,
            atk = 50, value = 100, identified = false
        )
        state.inventory = state.inventory + mystery
        assertEquals(false, GameEngine.equipItem(state, "mystery"))
        assertTrue(state.shards > 0)
        assertTrue(GameEngine.identifyItem(state, "mystery"))
        assertTrue(state.inventory.first { it.id == "mystery" }.identified)
    }

    @Test
    fun setBonusesApply() {
        val state = GameState()
        state.equipment = state.equipment.map {
            it.copy(setName = "veilleur")
        }
        val before = statsOf(GameState())
        val after = statsOf(state)
        assertTrue(after.atk > before.atk)
        assertTrue(after.maxHp > before.maxHp)
    }

    @Test
    fun healingConsumableRestoresHpAndIsConsumed() {
        val state = GameState()
        state.hp = 10.0
        state.consumables = mapOf("potion_soin" to 1)
        val outcome = GameEngine.useConsumable(state, "potion_soin")
        assertTrue(outcome.good)
        assertTrue(state.hp > 10.0)
        assertTrue(state.consumables.isEmpty())
    }

    @Test
    fun combatOnlyConsumableIsRejectedOutsideFight() {
        val state = GameState()
        state.consumables = mapOf("fiole_rage" to 1)
        val outcome = GameEngine.useConsumable(state, "fiole_rage")
        assertEquals(false, outcome.good)
        assertEquals(1, state.consumables["fiole_rage"])
    }

    @Test
    fun combatBuffRaisesAttackAndExpires() {
        val state = GameState()
        GameEngine.startBoss(state)
        state.consumables = mapOf("fiole_rage" to 1)
        val before = statsOf(state).atk
        val outcome = GameEngine.useConsumable(state, "fiole_rage")
        assertTrue(outcome.good)
        assertTrue(statsOf(state).atk > before)
        assertTrue(state.buffs.any { it.type == "atk" })
        state.buffs = state.buffs.map { it.copy(expiresAt = 0L) }
        assertEquals(before, statsOf(state).atk)
    }

    @Test
    fun fireBombDamagesTheEnemy() {
        val state = GameState()
        GameEngine.startBoss(state)
        val hpBefore = state.fight!!.enemyHp
        state.consumables = mapOf("bombe_feu" to 1)
        val outcome = GameEngine.useConsumable(state, "bombe_feu")
        assertTrue(outcome.good)
        assertTrue(state.fight == null || state.fight!!.enemyHp < hpBefore)
    }

    @Test
    fun chestLootCanGrantConsumables() {
        val state = GameState()
        state.consumables = emptyMap()
        GameEngine.dropConsumables(state, "chest", forcedCount = 3)
        assertEquals(3, state.consumables.values.sum())
    }

    @Test
    fun fullInventoryKeepsTheBetterLoot() {
        val state = GameState()
        state.inventory = (1..24).map {
            Item("weak_$it", "Vieux bidon $it", "weapon", "common", "item/spear2.png", 1, atk = 1, value = 1)
        }
        val strong = Item(
            "strong", "Lame de test", "weapon", "rare", "item/long_sword3.png", 5,
            atk = 80, value = 120
        )
        GameEngine.addLoot(state, strong)
        assertEquals(24, state.inventory.size)
        assertTrue(state.inventory.any { it.id == "strong" })
        assertEquals(1, state.stash.size)
    }

    @Test
    fun fullInventorySendsTheWorseLootToStash() {
        val state = GameState()
        state.inventory = (1..24).map {
            Item("strong_$it", "Bonne lame $it", "weapon", "rare", "item/long_sword3.png", 5, atk = 80, value = 100)
        }
        val weak = Item(
            "weak", "Vieux bidon", "weapon", "common", "item/spear2.png", 1,
            atk = 1, value = 2
        )
        GameEngine.addLoot(state, weak)
        assertEquals(24, state.inventory.size)
        assertEquals(1, state.stash.size)
        assertEquals("weak", state.stash.first().id)
    }

    @Test
    fun lockedItemsAreNeverDisplaced() {
        val state = GameState()
        state.inventory = (1..24).map {
            Item(
                "locked_$it", "Objet verrouillé $it", "weapon", "common", "item/spear2.png", 1,
                atk = 1, value = 1, locked = true
            )
        }
        val strong = Item(
            "strong", "Lame de test", "weapon", "rare", "item/long_sword3.png", 5,
            atk = 80, value = 120
        )
        GameEngine.addLoot(state, strong)
        assertEquals(24, state.inventory.size)
        assertEquals(1, state.stash.size)
        assertEquals("strong", state.stash.first().id)
        assertTrue(state.inventory.all { it.locked })
    }
}
