package com.brunothesatellite.idlesteprpg

import com.brunothesatellite.idlesteprpg.engine.GameEngine
import com.brunothesatellite.idlesteprpg.engine.GameState
import com.brunothesatellite.idlesteprpg.engine.ChestData
import com.brunothesatellite.idlesteprpg.engine.FightData
import com.brunothesatellite.idlesteprpg.engine.ForgeOutcome
import com.brunothesatellite.idlesteprpg.engine.INVENTORY_CAP
import com.brunothesatellite.idlesteprpg.engine.Item
import com.brunothesatellite.idlesteprpg.engine.MAX_STEPS_PER_DAY
import com.brunothesatellite.idlesteprpg.engine.OfflineReport
import com.brunothesatellite.idlesteprpg.engine.OfflineMode
import com.brunothesatellite.idlesteprpg.engine.OfflinePolicy
import com.brunothesatellite.idlesteprpg.engine.SAVE_VERSION
import com.brunothesatellite.idlesteprpg.engine.SpecialData
import com.brunothesatellite.idlesteprpg.engine.fireTier
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
    fun farmModeIsOffByDefault() {
        assertEquals(false, GameState().holdFloor)
    }

    @Test
    fun holdFloorKeepsFloorAfterBoss() {
        val state = GameState().apply {
            floor = 10
            holdFloor = true
            shards = 0
            oaths.bossesClaimed = true
        }
        val fight = FightData(
            enemyName = "Roi des Cryptes",
            enemyIcon = "mon/lich.png",
            enemyLevel = 12,
            enemyHp = 1.0,
            enemyMaxHp = 100.0,
            enemyAtk = 0.0,
            enemyIntervalMs = 1250,
            gold = 100,
            xp = 100,
            isBoss = true
        )
        GameEngine.victory(state, fight)
        assertEquals(10, state.floor)
        assertEquals(0, state.shards)
        assertEquals(0.0, state.distance, 0.001)
        assertEquals(10, state.nextFight)
        assertEquals(1, state.bossKills["Roi des Cryptes"])
        assertEquals("walk", state.mode)
    }

    @Test
    fun normalBossVictoryAdvancesFloor() {
        val state = GameState().apply {
            floor = 10
            holdFloor = false
            shards = 0
            oaths.bossesClaimed = true
        }
        val fight = FightData(
            enemyName = "Roi des Cryptes",
            enemyIcon = "mon/lich.png",
            enemyLevel = 12,
            enemyHp = 1.0,
            enemyMaxHp = 100.0,
            enemyAtk = 0.0,
            enemyIntervalMs = 1250,
            gold = 100,
            xp = 100,
            isBoss = true
        )
        GameEngine.victory(state, fight)
        assertEquals(11, state.floor)
        assertEquals(7, state.shards)
        assertEquals(1, state.bossKills["Roi des Cryptes"])
    }

    @Test
    fun discoveredItemsAreTracked() {
        val state = GameState()
        GameEngine.addLoot(
            state,
            Item("codex_test", "Relique de test · VAMPIRISME", "talisman", "rare", "item/misc_box.png", 1)
        )
        assertEquals(true, "Relique de test" in state.discoveredItems)
    }

    @Test
    fun absenceSimulationConsumesPartOfTheReserve() {
        val state = GameState().apply {
            floor = 1
            level = 50
            hp = 20_000.0
            stepsToday = 20_000
            stepBank = 20_000.0
            mode = "walk"
            oaths.bossesClaimed = true
            upgrades.blade = 40
            upgrades.breath = 40
            upgrades.swift = 40
        }
        val sim = GameEngine.simulateAbsence(state, 10L * 60L * 1000L)
        assertTrue(sim.stepsConsumed > 0)
        assertTrue(sim.remainingBank < 20_000)
        assertTrue(sim.simulatedMs > 0L)
        // Jamais plus de 30 % des pas du jour.
        assertTrue(sim.stepsConsumed <= 6_100)
        assertEquals(sim.stepsConsumed, state.offlineStepsToday)
    }

    @Test
    fun absenceSimulationRespectsTheDailyBudget() {
        // 30 % des pas du jour, indépendamment du stock disponible.
        val reference = GameState().apply { stepsToday = 10_000 }
        assertEquals(3_000, GameEngine.offlineBudget(reference))

        val state = GameState().apply {
            floor = 1
            level = 50
            hp = 20_000.0
            stepsToday = 10_000
            stepBank = 20_000.0
            mode = "walk"
            oaths.bossesClaimed = true
        }
        var total = 0
        repeat(20) {
            total += GameEngine.simulateAbsence(state, 60L * 60L * 1000L).stepsConsumed
        }
        // Plusieurs reprises dans la même journée ne peuvent pas dépasser le quota.
        assertTrue("total consommé: $total", total <= 3_100)
        assertEquals(total, state.offlineStepsToday)
        assertTrue(
            "budget restant: ${GameEngine.offlineBudget(state)}",
            GameEngine.offlineBudget(state) < 3_100
        )
    }

    @Test
    fun absenceSimulationSkipsCamp() {
        val state = GameState().apply {
            stepsToday = 20_000
            stepBank = 20_000.0
            mode = "camp"
        }
        val sim = GameEngine.simulateAbsence(state, 60L * 60L * 1000L)
        assertEquals(true, sim.skippedCamp)
        assertEquals(0, sim.stepsConsumed)
        assertEquals(20_000, sim.remainingBank)
    }

    @Test
    fun offlineReportsMergeIntoOneSummary() {
        val absence = OfflineReport(
            days = 0, steps = 0, distanceMeters = 120, combats = 3,
            gold = 40, floors = 1, kills = 3, minutes = 12,
            stepsConsumed = 1_200, remainingSteps = 800, reason = "absence"
        )
        val garmin = OfflineReport(
            days = 2, steps = 5_000, distanceMeters = 500,
            reason = "garmin", remainingSteps = 5_800
        )
        val merged = GameEngine.mergeOfflineReports(absence, garmin)!!
        assertEquals(2, merged.days)
        assertEquals(5_000, merged.steps)
        assertEquals(1_200, merged.stepsConsumed)
        assertEquals(3, merged.combats)
        assertEquals("mixed", merged.reason)
        assertEquals(5_800, merged.remainingSteps)
    }

    @Test
    fun bossKillsAreMigratedFromBestiary() {
        val state = GameState().apply {
            version = 2
            bestiary = mapOf("Roi des Cryptes · RAPIDE" to 2, "Hydre de Lerne" to 1)
        }
        GameEngine.migrate(state)
        assertEquals(2, state.bossKills["Roi des Cryptes"])
        assertEquals(1, state.bossKills["Hydre de Lerne"])
        assertEquals(SAVE_VERSION, state.version)
    }

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
    fun rebaseAfterSourceChangeDoesNotCreditSteps() {
        val state = GameState()
        val day = LocalDate.of(2026, 11, 16)
        GameEngine.ingestSteps(state, 4000, day)
        val bankBefore = state.stepBank

        // Nouvelle source qui annonce beaucoup plus le même jour.
        GameEngine.rebaseHistory(state, mapOf(day.toString() to 9000), day)
        assertEquals(9000, state.stepsToday)
        assertEquals(bankBefore, state.stepBank, 0.001)

        // Le premier import qui recoupe la base ne crédite rien…
        GameEngine.ingestSteps(state, 9000, day)
        assertEquals(bankBefore, state.stepBank, 0.001)

        // … et seuls les pas suivants comptent.
        GameEngine.ingestSteps(state, 9400, day)
        assertEquals(bankBefore + 400.0, state.stepBank, 0.001)
    }

    @Test
    fun rebaseWithLowerTotalKeepsCurrentProgress() {
        val state = GameState()
        val day = LocalDate.of(2026, 11, 17)
        GameEngine.ingestSteps(state, 6000, day)
        val bankBefore = state.stepBank

        GameEngine.rebaseHistory(state, mapOf(day.toString() to 2000), day)
        assertEquals(6000, state.stepsToday)
        assertEquals(bankBefore, state.stepBank, 0.001)

        GameEngine.ingestSteps(state, 6500, day)
        assertEquals(bankBefore + 500.0, state.stepBank, 0.001)
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
    fun firstImportOnlyCountsTodayThenRecoversSinceThatDate() {
        val state = GameState()
        val first = LocalDate.of(2026, 11, 16)
        val initial = GameEngine.ingestHistory(
            state,
            mapOf("2026-11-14" to 4_000, "2026-11-15" to 5_000, "2026-11-16" to 2_500),
            first
        )
        assertNull(initial)
        assertEquals("2026-11-16", state.historyStart)
        assertEquals(2_500, state.stepsToday)
        assertEquals(2_500.0, state.stepBank, 0.001)

        // Deux jours plus tard : la veille manquante est rattrapée, mais jamais
        // les jours antérieurs à la première connexion.
        val report = GameEngine.ingestHistory(
            state,
            mapOf(
                "2026-11-14" to 4_000,
                "2026-11-15" to 5_000,
                "2026-11-16" to 2_500,
                "2026-11-17" to 8_000,
                "2026-11-18" to 300
            ),
            first.plusDays(2)
        )
        assertNotNull(report)
        assertEquals(1, report!!.days)
        assertEquals(8_000, report.steps)
        assertEquals(300, state.stepsToday)
        assertEquals(10_800.0, state.stepBank, 0.001)
    }

    @Test
    fun historyImportIsIdempotentAndReported() {
        val state = GameState()
        val start = LocalDate.of(2026, 11, 14)
        val today = LocalDate.of(2026, 11, 16)
        GameEngine.ingestHistory(state, mapOf("2026-11-14" to 4_000), start)
        state.stepBank = 0.0

        val days = mapOf(
            "2026-11-14" to 4_000,
            "2026-11-15" to 5_000,
            "2026-11-16" to 2_500
        )
        val report = GameEngine.ingestHistory(state, days, today)
        assertNotNull(report)
        assertEquals(1, report!!.days)
        assertEquals(5_000, report.steps)
        assertEquals(2_500, state.stepsToday)
        assertEquals(7_500.0, state.stepBank, 0.001)

        val again = GameEngine.ingestHistory(state, days, today)
        assertNull(again)
        assertEquals(7_500.0, state.stepBank, 0.001)
        assertEquals(0, state.lastAdded)
    }

    @Test
    fun historyImportIsCappedButLedgerAdvances() {
        val state = GameState()
        state.historyStart = "2026-11-01"
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
    fun daysBeforeFirstConnectionAreNeverCounted() {
        val state = GameState()
        state.historyStart = "2026-11-10"
        val today = LocalDate.of(2026, 11, 12)
        val report = GameEngine.ingestHistory(
            state,
            mapOf("2026-11-08" to 30_000, "2026-11-09" to 12_000),
            today
        )
        assertNull(report)
        assertEquals(0, state.stepsToday)
        assertEquals(0.0, state.stepBank, 0.001)
        assertEquals(0, state.stepLedger.size)
    }

    @Test
    fun marathonDayIsImportedWithoutTruncation() {
        val state = GameState()
        state.historyStart = "2026-11-15"
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
    fun fusionAlwaysKeepsTheOpenedItem() {
        val state = GameState()
        state.inventory = (1..4).map {
            Item("rare_$it", "Rare $it", "weapon", "rare", "item/mace2.png", 3, atk = 10 + it, value = 50)
        }
        val weakest = state.inventory.first { it.id == "rare_1" }
        assertTrue(GameEngine.canFuse(state, weakest))
        val fused = GameEngine.fuseItem(state, "rare_1")
        assertNotNull(fused)
        assertEquals("epic", fused!!.rarity)
        assertTrue(state.inventory.none { it.id == "rare_1" })
        assertEquals(2, state.inventory.size)
    }

    @Test
    fun canFuseRequiresThreeEligibleItems() {
        val state = GameState()
        val item = Item("rare_1", "Rare 1", "weapon", "rare", "item/mace2.png", 3, atk = 10, value = 50)
        state.inventory = listOf(item, item.copy(id = "rare_2"))
        assertEquals(false, GameEngine.canFuse(state, item))
        state.inventory = listOf(item, item.copy(id = "rare_2"), item.copy(id = "rare_3", locked = true))
        assertEquals(false, GameEngine.canFuse(state, item))
        state.inventory = state.inventory + item.copy(id = "rare_4")
        assertTrue(GameEngine.canFuse(state, item))
    }

    @Test
    fun offlinePolicyDefaultsToThirtyPercent() {
        val state = GameState()
        state.stepsToday = 1_000
        assertEquals(300, GameEngine.offlineBudget(state))
        assertEquals(300, OfflinePolicy.Default.budgetFor(1_000))
    }

    @Test
    fun offlinePolicyAbsoluteIsClampedToTheDay() {
        val policy = OfflinePolicy(mode = OfflineMode.STEPS, maxSteps = 1_000)
        val state = GameState()
        state.stepsToday = 5_000
        assertEquals(1_000, GameEngine.offlineBudget(state, policy))
        state.stepsToday = 300
        assertEquals(300, GameEngine.offlineBudget(state, policy))
    }

    @Test
    fun offlinePolicyZeroDisablesProgression() {
        val state = GameState()
        state.stepsToday = 2_000
        state.stepBank = 2_000.0
        val policy = OfflinePolicy(percent = 0)
        assertEquals(0, GameEngine.offlineBudget(state, policy))
        val sim = GameEngine.simulateAbsence(state, 60_000L, policy = policy)
        assertEquals(0, sim.stepsConsumed)
        assertEquals(2_000.0, state.stepBank, 0.001)
    }

    @Test
    fun offlineBudgetStaysDaily() {
        val state = GameState()
        state.stepsToday = 1_000
        state.offlineStepsToday = 250
        assertEquals(50, GameEngine.offlineBudget(state))
    }

    @Test
    fun offlineSimulationUsesTheConfiguredAbsoluteBudget() {
        val state = GameState()
        state.stepsToday = 5_000
        state.stepBank = 5_000.0
        state.mode = "walk"
        val sim = GameEngine.simulateAbsence(
            state,
            elapsedMs = 60_000L,
            policy = OfflinePolicy(mode = OfflineMode.STEPS, maxSteps = 200)
        )
        assertTrue(sim.stepsConsumed in 1..230)
        assertEquals(sim.stepsConsumed, state.offlineStepsToday)
    }

    @Test
    fun dayChangeResetsDailyCountersOnce() {
        val state = GameState()
        state.day = "2026-11-20"
        state.lastActiveDay = "2026-11-20"
        state.stepsToday = 5_000
        state.offlineStepsToday = 900
        state.ferveur = 40
        state.streak = 3

        GameEngine.ensureDay(state, LocalDate.of(2026, 11, 21))

        assertEquals("2026-11-21", state.day)
        assertEquals(0, state.stepsToday)
        assertEquals(0, state.offlineStepsToday)
        assertEquals(0, state.ferveur)
        assertEquals(4, state.streak)
    }

    @Test
    fun backwardDateDoesNotResetTheDay() {
        val state = GameState()
        state.day = "2026-11-21"
        state.lastActiveDay = "2026-11-21"
        state.stepsToday = 5_000
        state.offlineStepsToday = 900
        state.ferveur = 40
        state.streak = 4

        GameEngine.ensureDay(state, LocalDate.of(2026, 11, 20))

        assertEquals("2026-11-21", state.day)
        assertEquals(5_000, state.stepsToday)
        assertEquals(900, state.offlineStepsToday)
        assertEquals(40, state.ferveur)
        assertEquals(4, state.streak)
    }

    @Test
    fun chestWarningUsesTheThreeHourWindow() {
        val now = 1_000_000_000L
        val inside = GameState().apply {
            chest = ChestData(
                "fer", 400,
                goldMin = 10, goldMax = 20,
                expiresAt = now + 2L * 60L * 60L * 1000L
            )
        }
        assertTrue(GameEngine.chestWarnings(inside, now).isNotEmpty())

        val tooEarly = GameState().apply {
            chest = ChestData(
                "fer", 400,
                goldMin = 10, goldMax = 20,
                expiresAt = now + 4L * 60L * 60L * 1000L
            )
        }
        assertTrue(GameEngine.chestWarnings(tooEarly, now).isEmpty())
    }

    @Test
    fun fireTierFollowsBiomes() {
        assertEquals(0, fireTier(1))
        assertEquals(0, fireTier(10))
        assertEquals(1, fireTier(11))
        assertEquals(1, fireTier(20))
        assertEquals(6, fireTier(61))
        assertEquals(7, fireTier(71))
        assertEquals(7, fireTier(999))
    }

    @Test
    fun migrateKeepsPlayerDataAndStampsTheVersion() {
        val state = GameState()
        state.version = 1
        state.gold = 1_234
        state.stepBank = 500.0
        state.inventory = listOf(
            Item("old", "Ancienne", "weapon", "rare", "item/mace2.png", 3, atk = 10, value = 50)
        )

        GameEngine.migrate(state)

        assertEquals(SAVE_VERSION, state.version)
        assertEquals(1_234, state.gold)
        assertEquals(500.0, state.stepBank, 0.001)
        assertEquals(1, state.inventory.size)
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
        state.stepsToday = 5_000
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
    fun equippedItemCanBeUpgraded() {
        val state = GameState()
        state.gold = 10_000
        state.shards = 5
        val weapon = Item(
            "worn_mace", "Hache de guerre", "weapon", "rare", "item/mace2.png", 17,
            atk = 40, hp = 24, value = 300
        )
        state.equipment = state.equipment.filter { it.slot != "weapon" } + weapon
        val price = GameEngine.upgradeCost(weapon)

        assertEquals(ForgeOutcome.Success, GameEngine.upgradeItemDetailed(state, "worn_mace"))
        assertEquals(18, state.equipment.first { it.id == "worn_mace" }.level)
        assertEquals(10_000 - price, state.gold)
        assertEquals(4, state.shards)
        assertTrue(GameEngine.findItem(state, "worn_mace") != null)
    }

    @Test
    fun upgradeGrowsEveryStatByAtLeastOne() {
        val axe = Item(
            "worn_mace", "Hache de guerre", "weapon", "epic", "item/hand_axe2.png", 17,
            atk = 36, hp = 2, vitPct = 2, critPct = 6, value = 432
        )
        val next = GameEngine.upgradedItem(axe)
        assertEquals(18, next.level)
        assertEquals(39, next.atk)
        assertEquals(3, next.hp)
        assertEquals(3, next.vitPct)
        assertEquals(7, next.critPct)
        assertEquals(518, next.value)
    }

    @Test
    fun upgradeReportsWhatIsActuallyMissing() {
        val state = GameState()
        state.gold = 0
        state.shards = 0
        state.inventory = listOf(
            Item("poor", "Lame", "weapon", "rare", "item/long_sword3.png", 5, atk = 10, value = 50)
        )

        assertEquals(ForgeOutcome.Missing(170, 1), GameEngine.upgradeItemDetailed(state, "poor"))
        assertEquals(0, state.gold)
        assertEquals(0, state.shards)
        assertEquals(5, state.inventory.first().level)
        assertEquals(ForgeOutcome.NotFound, GameEngine.upgradeItemDetailed(state, "absent"))
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
