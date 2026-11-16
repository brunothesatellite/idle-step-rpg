package com.brunothesatellite.idlesteprpg.engine

import java.time.LocalDate
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

object GameEngine {

    private val rnd = Random.Default

    fun ensureDay(state: GameState, date: LocalDate = LocalDate.now()) {
        val d = date.toString()
        if (state.day.isEmpty()) {
            state.day = d
            state.hp = statsOf(state).maxHp.toDouble()
            return
        }
        if (state.day != d) {
            state.day = d
            state.stepsToday = 0
            state.processedSteps = 0
            state.ferveur = 0
            state.genRations = 0
            state.genShards = 0
            state.lastAdded = 0
            state.journal = addJournal(state, "Nouveau jour — 00:00", "neutral")
        }
    }

    fun tick(state: GameState, dtMs: Double) {
        ensureDay(state)
        val s = statsOf(state)
        state.hp = state.hp.coerceIn(0.0, s.maxHp.toDouble())

        val fighting = state.mode == "fight" || state.mode == "boss"
        val regen = s.maxHp * 0.006 * (1.0 + 0.008 * state.ferveur) / 1000.0 *
            (if (fighting) 1.0 / 3.0 else 1.0)
        state.hp = min(s.maxHp.toDouble(), state.hp + regen * dtMs)

        when (state.mode) {
            "walk" -> tickWalk(state, dtMs)
            "fight", "boss" -> tickFight(state, dtMs)
            "chest" -> tickChest(state, dtMs)
            "farmWait" -> {
                state.farmWaitMs -= dtMs
                if (state.farmWaitMs <= 0) startFarmFight(state)
            }
            "camp" -> Unit
            else -> state.mode = "walk"
        }
        state.hp = state.hp.coerceIn(0.0, statsOf(state).maxHp.toDouble())
    }

    private fun tickWalk(state: GameState, dtMs: Double) {
        if (state.stepBank <= 0.0) return
        val rate = if (state.stepBank > 600) 140.0 else 70.0
        val used = min(state.stepBank, rate * dtMs / 1000.0)
        state.stepBank -= used
        state.distance += used / STEPS_PER_METER
        if (state.distance >= state.nextFight) {
            state.distance = state.nextFight.toDouble()
            if (state.nextFight >= FLOOR_METERS) startBoss(state) else startFight(state)
        }
    }

    fun ingestSteps(state: GameState, totalSteps: Int, date: LocalDate = LocalDate.now()) {
        ensureDay(state, date)
        val total = max(0, totalSteps)
        val delta = max(0, total - state.processedSteps)
        state.processedSteps = max(state.processedSteps, total)
        state.stepsToday = total
        state.ferveur = clampFerveur(total)
        state.stepBank += delta
        state.lastAdded = delta
        state.lastSync = System.currentTimeMillis()
        state.lastSyncSource = "Garmin"

        val nr = min(24, total / 200)
        val ns = min(12, total / 500)
        if (nr > state.genRations) {
            state.rations = min(24, state.rations + (nr - state.genRations))
            state.genRations = nr
        }
        if (ns > state.genShards) {
            state.shards = min(12, state.shards + (ns - state.genShards))
            state.genShards = ns
        }

        if (delta > 0) {
            state.journal = addJournal(state, "$delta pas Garmin — ${"%.0f".format(delta / STEPS_PER_METER)} m", "good")
        }
    }

    fun debugAddSteps(state: GameState, steps: Int) {
        if (steps <= 0) return
        state.stepsToday += steps
        state.stepBank += steps
        state.ferveur = clampFerveur(state.stepsToday)
        state.journal = addJournal(state, "Démo +$steps pas", "neutral")
    }

    fun startFight(state: GameState, farm: Boolean = false) {
        if (state.mode == "fight" || state.mode == "boss") return
        val f = if (farm) makeFarm(state) else makeMob(state)
        state.fight = f
        state.mode = if (farm) "fight" else "fight"
        state.farmWaitMs = 0.0
    }

    fun startBoss(state: GameState) {
        state.fight = makeBoss(state)
        state.mode = "boss"
    }

    private fun startFarmFight(state: GameState) {
        state.fight = makeFarm(state)
        state.mode = "fight"
    }

    private fun makeMob(state: GameState): FightData {
        val pos = state.distance
        val pool = when {
            pos >= 85 -> HARD_MOBS
            pos >= 50 -> MID_MOBS
            else -> EASY_MOBS
        }
        val t = pool[rnd.nextInt(pool.size)]
        val elite = rnd.nextDouble() < 0.10
        var level = state.floor + when {
            pos < 30 -> -1
            pos < 70 -> 0
            else -> 1
        }
        if (elite) level += 2
        level = max(1, level)
        val hp = (55 + 34 * (level - 1)) * if (elite) 2.2 else 1.0
        val atk = (6 + 2.2 * (level - 1)) * if (elite) 1.3 else 1.0
        val gold = ((6 + 3 * level) * if (elite) 1.6 else 1.0).roundToInt()
        val xp = ((12 + 5 * level) * if (elite) 1.8 else 1.0).roundToInt()
        return FightData(
            enemyName = t.name,
            enemyIcon = t.icon,
            enemyLevel = level,
            enemyHp = hp,
            enemyMaxHp = hp,
            enemyAtk = atk,
            enemyIntervalMs = if (elite) 1100 else 1250,
            gold = gold,
            xp = xp,
            elite = elite
        )
    }

    private fun makeBoss(state: GameState): FightData {
        val t = bossFor(state.floor)
        val level = state.floor + 2
        val hp = 6.0 * (55 + 34 * (level - 1))
        val atk = 1.6 * (6 + 2.2 * (level - 1))
        return FightData(
            enemyName = t.name,
            enemyIcon = t.icon,
            enemyLevel = level,
            enemyHp = hp,
            enemyMaxHp = hp,
            enemyAtk = atk,
            enemyIntervalMs = 1250,
            gold = 12 * (6 + 3 * level),
            xp = 10 * (12 + 5 * level),
            isBoss = true
        )
    }

    private fun makeFarm(state: GameState): FightData {
        val t = EASY_MOBS[rnd.nextInt(EASY_MOBS.size)]
        val level = max(1, state.floor - 2)
        val hp = (55 + 34 * (level - 1)) * 0.65
        val atk = (6 + 2.2 * (level - 1)) * 0.6
        return FightData(
            enemyName = t.name,
            enemyIcon = t.icon,
            enemyLevel = level,
            enemyHp = hp,
            enemyMaxHp = hp,
            enemyAtk = atk,
            enemyIntervalMs = 1250,
            gold = ((6 + 3 * level) * 0.5).roundToInt(),
            xp = ((12 + 5 * level) * 0.5).roundToInt(),
            farm = true
        )
    }

    private fun tickFight(state: GameState, dtMs: Double) {
        val f = state.fight ?: run { state.mode = "walk"; return }
        f.heroTimerMs -= dtMs
        f.enemyTimerMs -= dtMs
        f.elapsedMs += dtMs
        f.specialMs -= dtMs

        if (f.isBoss && f.enemyHp < f.enemyMaxHp * 0.5 && f.enemyIntervalMs > 1000) {
            f.enemyIntervalMs = 950
            state.journal = addJournal(state, "Le gardien s'embrase", "bad")
        }

        if (f.heroTimerMs <= 0) {
            heroAttack(state, f)
            if (state.fight == null) return
            f.heroTimerMs = heroInterval(state)
        }
        if (f.enemyTimerMs <= 0) {
            enemyAttack(state, f)
            if (state.fight == null) return
            f.enemyTimerMs = f.enemyIntervalMs.toDouble()
        }
    }

    private fun heroInterval(state: GameState): Double {
        val s = statsOf(state)
        return (1000.0 / (s.vit * (1.0 + 0.004 * state.ferveur))).coerceAtLeast(300.0)
    }

    private fun heroAttack(state: GameState, f: FightData) {
        val s = statsOf(state)
        var atk = s.atk * if (f.isBoss && state.bossBlessing > 0) 1.25 else 1.0
        var dmg = atk * (0.9 + rnd.nextDouble() * 0.2)
        val critChance = (s.crit + state.ferveur * 0.02) / 100.0
        if (rnd.nextDouble() < critChance) dmg *= 1.6
        f.enemyHp -= dmg
        if (f.enemyHp <= 0) victory(state, f)
    }

    private fun enemyAttack(state: GameState, f: FightData) {
        var dmg = f.enemyAtk * (0.9 + rnd.nextDouble() * 0.1)
        if (f.isBoss && f.specialMs <= 0) {
            dmg *= 1.8
            f.specialMs = 8000.0
        }
        state.hp -= dmg
        if (state.hp <= 0) {
            if (f.farm) {
                state.hp = 1.0
            } else {
                defeat(state, f)
                return
            }
        }
        val s = statsOf(state)
        if (!f.usedRation && !f.farm && state.hp < s.maxHp * 0.25 && state.rations > 0) {
            f.usedRation = true
            state.rations--
            val heal = s.maxHp * 0.35
            state.hp = min(s.maxHp.toDouble(), state.hp + heal)
            state.journal = addJournal(state, "Ration consommée (+${heal.roundToInt()} PV)", "neutral")
        }
    }

    private fun victory(state: GameState, f: FightData) {
        state.gold += f.gold
        gainXp(state, f.xp)
        state.fight = null

        if (f.isBoss) {
            state.bosses++
            state.shards += 2
            state.rations += 1
            state.bossBlessing = 0.0
            state.floor++
            state.distance = 0.0
            state.nextFight = 10
            state.chest = null
            state.inventory = state.inventory + lootItem(state.floor, "rare")
            state.journal = addJournal(state, "${f.enemyName} vaincu — palier ${state.floor}", "good")
        } else if (f.farm) {
            state.farmKills++
            val s = statsOf(state)
            state.hp = min(s.maxHp.toDouble(), state.hp + s.maxHp * 0.08)
            if (state.farmKills >= 3) {
                state.mode = "walk"
                state.nextFight = ((state.distance / 10).toInt() + 1) * 10
                state.farmKills = 0
                state.journal = addJournal(state, "Fin du repli — le sentier reprend", "neutral")
            } else {
                state.mode = "farmWait"
                state.farmWaitMs = 900.0
            }
        } else {
            state.mode = "walk"
            state.nextFight += 10
            state.journal = addJournal(state, "${f.enemyName} vaincu · +${f.gold} or", "good")
            if (state.chest == null && rnd.nextDouble() < 0.18) spawnChest(state)
        }
    }

    private fun defeat(state: GameState, f: FightData) {
        state.fight = null
        state.retreats++
        state.distance = max(0.0, floor((state.distance - 20.0) / 10.0) * 10.0)
        state.nextFight = (state.distance + 10).toInt()
        state.hp = statsOf(state).maxHp * 0.6
        state.mode = "farmWait"
        state.farmKills = 0
        state.farmWaitMs = 800.0
        if (f.isBoss) state.bossBlessing = 0.25
        state.journal = addJournal(state, "Défaite — repli de 20 m dans la zone de farm", "bad")
    }

    private fun gainXp(state: GameState, amount: Int) {
        state.xp += amount
        var guard = 0
        while (state.xp >= xpNext(state.level) && guard < 8) {
            state.xp -= xpNext(state.level)
            state.level++
            val s = statsOf(state)
            state.hp = min(s.maxHp.toDouble(), state.hp + s.maxHp * 0.5)
            state.journal = addJournal(state, "Niveau ${state.level} atteint", "good")
            guard++
        }
    }

    private fun tickChest(state: GameState, dtMs: Double) {
        val c = state.chest ?: run { state.mode = "walk"; return }
        if (state.stepBank <= 0) return
        val used = min(state.stepBank, 55.0 * dtMs / 1000.0)
        state.stepBank -= used
        c.progress = min(c.quota.toDouble(), c.progress + used)
        if (c.progress >= c.quota) openChest(state, c)
    }

    fun forceChest(state: GameState): Boolean {
        if (state.chest == null || state.mode != "walk") return false
        state.mode = "chest"
        return true
    }

    fun stopForcing(state: GameState) {
        if (state.mode == "chest") state.mode = "walk"
    }

    private fun openChest(state: GameState, c: ChestData) {
        val gold = if (c.goldMax > c.goldMin) rnd.nextInt(c.goldMin, c.goldMax + 1) else c.goldMin
        state.gold += gold
        state.rations = min(24, state.rations + 1)
        state.shards = min(12, state.shards + 1)
        state.inventory = state.inventory + lootItem(state.floor, c.rarity)
        state.chest = null
        state.mode = "walk"
        state.journal = addJournal(state, "Coffre forcé · +$gold or", "good")
    }

    fun spawnChest(state: GameState) {
        if (state.chest != null) return
        val r = rnd.nextDouble()
        val type = when {
            r < 0.60 -> "bois"
            r < 0.90 -> "fer"
            else -> "runique"
        }
        state.chest = when (type) {
            "bois" -> ChestData("bois", 150, goldMin = 40, goldMax = 80, rarity = "uncommon")
            "fer" -> ChestData("fer", 400, goldMin = 130, goldMax = 220, rarity = "rare")
            else -> ChestData("runique", 900, goldMin = 380, goldMax = 550, rarity = "epic")
        }
    }

    fun buyUpgrade(state: GameState, key: String): Boolean {
        val level = upgradeLevel(state, key)
        val cost = upgradeCost(key, level)
        if (state.gold < cost) return false
        val oldMax = statsOf(state).maxHp
        state.gold -= cost
        when (key) {
            "blade" -> state.upgrades.blade++
            "breath" -> state.upgrades.breath++
            "swift" -> state.upgrades.swift++
        }
        val newMax = statsOf(state).maxHp
        state.hp = if (key == "breath") state.hp + 30.0 else state.hp * newMax / oldMax
        state.hp = min(state.hp, newMax.toDouble())
        state.journal = addJournal(state, "${upgradeName(key)} niveau ${upgradeLevel(state, key)}", "good")
        return true
    }

    fun equipItem(state: GameState, itemId: String): Boolean {
        val item = state.inventory.firstOrNull { it.id == itemId } ?: return false
        val oldMax = statsOf(state).maxHp
        val previous = state.equipment.firstOrNull { it.slot == item.slot }
        state.equipment = state.equipment.filter { it.slot != item.slot } + item
        state.inventory = state.inventory.filter { it.id != item.id } + listOfNotNull(previous)
        val newMax = statsOf(state).maxHp
        state.hp = (state.hp * newMax / oldMax).coerceIn(1.0, newMax.toDouble())
        state.journal = addJournal(state, "${item.name} équipé", "good")
        return true
    }

    fun sellItem(state: GameState, itemId: String): Boolean {
        val item = state.inventory.firstOrNull { it.id == itemId } ?: return false
        state.inventory = state.inventory.filter { it.id != itemId }
        state.gold += item.value
        state.journal = addJournal(state, "${item.name} vendu (+${item.value} or)", "neutral")
        return true
    }

    fun rest(state: GameState): Boolean {
        val s = statsOf(state)
        if (state.rations <= 0 || state.hp >= s.maxHp) return false
        state.rations--
        val heal = s.maxHp * 0.4 * (1.0 + 0.002 * state.ferveur)
        state.hp = min(s.maxHp.toDouble(), state.hp + heal)
        state.journal = addJournal(state, "Repos au feu (+${heal.roundToInt()} PV)", "good")
        return true
    }

    fun shardHeal(state: GameState): Boolean {
        val s = statsOf(state)
        if (state.shards <= 0 || state.hp >= s.maxHp) return false
        state.shards--
        state.hp = s.maxHp.toDouble()
        state.journal = addJournal(state, "Éclat d'âme — soin complet", "good")
        return true
    }

    private fun lootItem(floor: Int, rarity: String): Item {
        val base = LOOT_TEMPLATES[rnd.nextInt(LOOT_TEMPLATES.size)]
        val mult = when (rarity) {
            "uncommon" -> 1.25
            "rare" -> 1.6
            "epic" -> 2.0
            else -> 1.0
        }
        fun scaled(v: Int): Int = max(1, (v * mult).roundToInt())
        return base.copy(
            id = "loot_${System.nanoTime()}",
            level = max(1, floor),
            atk = scaled(base.atk),
            hp = scaled(base.hp),
            vitPct = scaled(base.vitPct),
            critPct = scaled(base.critPct),
            regen = (base.regen * mult * 10).roundToInt() / 10.0,
            value = scaled(base.value)
        )
    }

    fun addJournal(state: GameState, text: String, type: String = "neutral"): List<JournalEntry> {
        val entry = JournalEntry(text, nowTime(), type)
        return (listOf(entry) + state.journal).take(10)
    }

    fun nextBoss(state: GameState): BossTemplate = bossFor(state.floor)

    fun chestTypeLabel(type: String): String = when (type) {
        "bois" -> "COFFRE DE BOIS"
        "fer" -> "COFFRE DE FER"
        else -> "COFFRE RUNIQUE"
    }

    fun isWalking(state: GameState): Boolean =
        state.mode == "walk" && state.stepBank > 0.0
}

