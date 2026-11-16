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
        migrateChests(state)
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
            state.ferveurDecayMs = 0.0
            state.genRations = 0
            state.genShards = 0
            state.lastAdded = 0
            state.bossesToday = 0
            state.chestsToday = 0
            state.oaths = oathsFor(date)
            state.special = null
            pruneLedger(state, date)
            state.journal = addJournal(state, "Nouveau jour — 00:00 · serments renouvelés", "neutral")
        }
    }

    private fun pruneLedger(state: GameState, date: LocalDate) {
        val cutoff = date.minusDays(35).toString()
        if (state.stepLedger.keys.any { it < cutoff }) {
            state.stepLedger = state.stepLedger.filterKeys { it >= cutoff }
        }
    }

    private fun migrateChests(state: GameState) {
        val now = System.currentTimeMillis()
        state.chest = state.chest?.let {
            if (it.expiresAt <= 0L) it.copy(expiresAt = now + CHEST_LIFETIME_MS) else it
        }
        if (state.chestQueue.any { it.expiresAt <= 0L }) {
            state.chestQueue = state.chestQueue.map {
                if (it.expiresAt <= 0L) it.copy(expiresAt = now + CHEST_LIFETIME_MS) else it
            }
        }
    }

    fun tick(state: GameState, dtMs: Double) {
        ensureDay(state)
        expireChests(state)
        tickFerveur(state, dtMs)
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
            "camp", "special" -> Unit
            else -> state.mode = "walk"
        }
        state.hp = state.hp.coerceIn(0.0, statsOf(state).maxHp.toDouble())
    }

    private fun tickFerveur(state: GameState, dtMs: Double) {
        if (state.ferveur <= 0) {
            state.ferveurDecayMs = 0.0
            return
        }
        val now = System.currentTimeMillis()
        if (state.lastStepAt <= 0L) {
            state.lastStepAt = now
            return
        }
        if (now - state.lastStepAt < FERVEUR_GRACE_MS) {
            state.ferveurDecayMs = 0.0
            return
        }
        state.ferveurDecayMs += dtMs
        while (state.ferveurDecayMs >= FERVEUR_DECAY_STEP_MS && state.ferveur > 0) {
            state.ferveurDecayMs -= FERVEUR_DECAY_STEP_MS
            state.ferveur--
        }
    }

    private fun tickWalk(state: GameState, dtMs: Double) {
        if (state.stepBank <= 0.0) return
        val rate = if (state.stepBank > 600) 140.0 else 70.0
        val used = min(state.stepBank, rate * dtMs / 1000.0)
        state.stepBank -= used
        state.distance += used / STEPS_PER_METER
        if (state.distance >= state.nextFight) {
            state.distance = state.nextFight.toDouble()
            if (state.nextFight >= FLOOR_METERS) {
                startBoss(state)
            } else if (!maybeStartSpecial(state)) {
                startFight(state)
            }
        }
    }

    private fun maybeStartSpecial(state: GameState): Boolean {
        if (state.special != null) return false
        if (rnd.nextDouble() >= 0.08) return false
        val type = listOf("autel", "source", "marchand", "statue").random(rnd)
        state.special = when (type) {
            "autel" -> SpecialData(
                type = type,
                title = "AUTEL DE BRAISE",
                text = "Une flamme ancienne réclame deux éclats d'âme.",
                acceptLabel = "OFFRIR 2 ÉCLATS",
                shardCost = 2,
                rewardText = "+180 XP"
            )
            "source" -> SpecialData(
                type = type,
                title = "SOURCE CLAIRE",
                text = "Une eau tiède miroite entre les dalles.",
                acceptLabel = "BOIRE · 1 RATION",
                rationCost = 1,
                rewardText = "PV complets"
            )
            "marchand" -> {
                val offer = lootItem(max(1, state.floor), if (rnd.nextDouble() < 0.3) "epic" else "rare")
                SpecialData(
                    type = type,
                    title = "MARCHAND ERRANT",
                    text = "« Une pièce pour une relique, voyageur. »",
                    acceptLabel = "ACHETER",
                    goldCost = max(60, offer.value * 2),
                    offer = offer,
                    rewardText = offer.name
                )
            }
            else -> SpecialData(
                type = type,
                title = "STATUE DU SERMENT",
                text = "La pierre garde la chaleur des serments tenus.",
                acceptLabel = "PRIER · 100 OR",
                goldCost = 100,
                rewardText = "+15 ferveur, +1 éclat"
            )
        }
        state.mode = "special"
        state.journal = addJournal(state, "Rencontre : ${state.special?.title ?: ""}", "neutral")
        return true
    }

    fun resolveSpecial(state: GameState, accept: Boolean) {
        val special = state.special ?: return
        var done = false
        if (accept) {
            when (special.type) {
                "autel" -> if (state.shards >= special.shardCost) {
                    state.shards -= special.shardCost
                    gainXp(state, 180)
                    done = true
                }
                "source" -> if (state.rations >= special.rationCost) {
                    state.rations -= special.rationCost
                    state.hp = statsOf(state).maxHp.toDouble()
                    done = true
                }
                "marchand" -> if (special.offer != null && state.gold >= special.goldCost) {
                    state.gold -= special.goldCost
                    addLoot(state, special.offer)
                    done = true
                }
                "statue" -> if (state.gold >= special.goldCost) {
                    state.gold -= special.goldCost
                    state.ferveur = min(100, state.ferveur + 15)
                    state.lastStepAt = System.currentTimeMillis()
                    state.shards = min(12, state.shards + 1)
                    done = true
                }
            }
        }
        state.journal = addJournal(
            state,
            when {
                done -> "${special.title} · ${special.rewardText}"
                accept -> "${special.title} · ressource insuffisante"
                else -> "${special.title} · passé sans s'arrêter"
            },
            if (done) "good" else "neutral"
        )
        state.special = null
        state.mode = "walk"
        state.nextFight += 10
    }

    fun ingestSteps(state: GameState, totalSteps: Int, date: LocalDate = LocalDate.now()) {
        ingestHistory(state, mapOf(date.toString() to max(0, totalSteps)), date)
    }

    /**
     * Imports a Garmin day ledger. Each calendar day is only ever counted once,
     * so syncing repeatedly (or importing the same 7 days again) is idempotent.
     */
    fun ingestHistory(
        state: GameState,
        days: Map<String, Int>,
        today: LocalDate = LocalDate.now()
    ): OfflineReport? {
        ensureDay(state, today)
        val todayKey = today.toString()
        var todayAdded = 0
        var todayTotal = state.stepsToday
        var historyAdded = 0
        var recoveredDays = 0

        days.toSortedMap().forEach { (key, raw) ->
            val parsed = runCatching { LocalDate.parse(key) }.getOrNull() ?: return@forEach
            if (parsed.isAfter(today)) return@forEach
            val steps = raw.coerceIn(0, MAX_STEPS_PER_DAY)
            val ledger = state.stepLedger[key] ?: 0
            val baseline = if (key == todayKey) max(ledger, state.processedSteps) else ledger
            if (steps <= baseline) return@forEach
            val delta = steps - baseline
            state.stepLedger = state.stepLedger + (key to steps)
            if (key == todayKey) {
                todayAdded += delta
                todayTotal = steps
            } else {
                historyAdded += delta
                recoveredDays++
            }
        }

        if (todayAdded > 0) {
            state.stepsToday = todayTotal
            state.processedSteps = max(state.processedSteps, todayTotal)
            state.ferveur = min(100, state.ferveur + (todayAdded + 49) / 50)
            state.lastStepAt = System.currentTimeMillis()
            state.ferveurDecayMs = 0.0
            state.stepBank += todayAdded
            val nr = min(24, todayTotal / 200)
            val ns = min(12, todayTotal / 500)
            if (nr > state.genRations) {
                state.rations = min(24, state.rations + (nr - state.genRations))
                state.genRations = nr
            }
            if (ns > state.genShards) {
                state.shards = min(12, state.shards + (ns - state.genShards))
                state.genShards = ns
            }
            state.journal = addJournal(
                state,
                "$todayAdded pas Garmin — ${"%.0f".format(todayAdded / STEPS_PER_METER)} m",
                "good"
            )
        }

        val importedHistory = min(historyAdded, HISTORY_IMPORT_CAP)
        if (importedHistory > 0) {
            state.stepBank += importedHistory
            state.rations = min(24, state.rations + min(12, importedHistory / 800))
            state.shards = min(12, state.shards + min(6, importedHistory / 2000))
            state.journal = addJournal(
                state,
                "Rattrapage Garmin · $recoveredDays jour(s) · $importedHistory pas",
                "good"
            )
        }

        state.lastAdded = todayAdded + importedHistory
        state.lastSync = System.currentTimeMillis()
        state.lastSyncSource = "Garmin"
        checkOaths(state)

        val report = if (recoveredDays > 0) {
            OfflineReport(
                days = recoveredDays,
                steps = importedHistory,
                distanceMeters = (importedHistory / STEPS_PER_METER).toInt()
            ).also { state.offlineReport = it }
        } else {
            null
        }
        return report
    }

    fun debugAddSteps(state: GameState, steps: Int) {
        if (steps <= 0) return
        state.stepsToday += steps
        state.stepBank += steps
        state.ferveur = min(100, state.ferveur + (steps + 49) / 50)
        state.lastStepAt = System.currentTimeMillis()
        state.ferveurDecayMs = 0.0
        state.stepLedger = state.stepLedger + (todayString() to state.stepsToday)
        state.journal = addJournal(state, "Démo +$steps pas", "neutral")
    }

    fun checkOaths(state: GameState) {
        val o = state.oaths
        if (!o.stepsClaimed && state.stepsToday >= o.stepsTarget) {
            o.stepsClaimed = true
            state.gold += 150
            state.shards = min(12, state.shards + 1)
            gainXp(state, 80)
            state.journal = addJournal(state, "Serment de marche accompli · +150 or, +1 éclat", "good")
        }
        if (!o.bossesClaimed && state.bossesToday >= o.bossesTarget) {
            o.bossesClaimed = true
            state.gold += 250
            state.shards = min(12, state.shards + 2)
            gainXp(state, 120)
            state.journal = addJournal(state, "Serment de chasse accompli · +250 or, +2 éclats", "good")
        }
        if (!o.chestsClaimed && state.chestsToday >= o.chestsTarget) {
            o.chestsClaimed = true
            state.gold += 100
            addLoot(state, lootItem(state.floor, "uncommon"))
            gainXp(state, 60)
            state.journal = addJournal(state, "Serment de curiosité accompli · +100 or, objet inhabituel", "good")
        }
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
        var hp = 6.0 * (55 + 34 * (level - 1))
        var atk = 1.6 * (6 + 2.2 * (level - 1))
        var interval = 1250L
        val modifier = listOf("", "", "rapide", "blinde", "berserk").random(rnd)
        when (modifier) {
            "rapide" -> interval = 900L
            "blinde" -> hp *= 1.30
            "berserk" -> atk *= 1.30
        }
        val suffix = when (modifier) {
            "rapide" -> " · RAPIDE"
            "blinde" -> " · BLINDÉ"
            "berserk" -> " · BERSERK"
            else -> ""
        }
        return FightData(
            enemyName = t.name + suffix,
            enemyIcon = t.icon,
            enemyLevel = level,
            enemyHp = hp,
            enemyMaxHp = hp,
            enemyAtk = atk,
            enemyIntervalMs = interval,
            gold = 12 * (6 + 3 * level),
            xp = 10 * (12 + 5 * level),
            isBoss = true,
            modifier = when (modifier) {
                "rapide" -> "RAPIDE"
                "blinde" -> "BLINDÉ"
                "berserk" -> "BERSERK"
                else -> ""
            }
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
        if (f.isBoss) dmg *= 1.0 + s.bossDamage
        val critChance = (s.crit + state.ferveur * 0.02) / 100.0
        if (rnd.nextDouble() < critChance) dmg *= 1.6 + s.critDamage
        f.enemyHp -= dmg
        if (s.lifesteal > 0.0) {
            state.hp = min(s.maxHp.toDouble(), state.hp + dmg * s.lifesteal)
        }
        if (f.enemyHp <= 0) victory(state, f)
    }

    private fun enemyAttack(state: GameState, f: FightData) {
        val s = statsOf(state)
        var dmg = f.enemyAtk * (0.9 + rnd.nextDouble() * 0.1)
        if (f.isBoss && f.specialMs <= 0) {
            dmg *= 1.8
            f.specialMs = 8000.0
        }
        dmg *= (1.0 - s.damageReduction)
        state.hp -= dmg
        if (state.hp <= 0) {
            if (f.farm) {
                state.hp = 1.0
            } else {
                defeat(state, f)
                return
            }
        }
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
            state.bossesToday++
            val clearedFloor = state.floor
            state.shards += 2
            state.rations += 1
            state.bossBlessing = 0.0
            state.floor++
            state.distance = 0.0
            state.nextFight = 10
            addLoot(state, lootItem(state.floor, "rare"))
            state.journal = addJournal(state, "${f.enemyName} vaincu — palier ${state.floor}", "good")
            if (clearedFloor % 10 == 0) {
                state.journal = addJournal(
                    state,
                    "Jalon · palier $clearedFloor franchi — rang ${rankLabel(state.floor)}",
                    "good"
                )
            }
            checkOaths(state)
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
            if (rnd.nextDouble() < 0.18) spawnChest(state)
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
        state.chestsToday++
        addLoot(state, lootItem(state.floor, c.rarity))
        state.chest = state.chestQueue.firstOrNull()
        if (state.chestQueue.isNotEmpty()) state.chestQueue = state.chestQueue.drop(1)
        state.mode = "walk"
        state.journal = addJournal(state, "Coffre forcé · +$gold or", "good")
        checkOaths(state)
    }

    fun spawnChest(state: GameState): Boolean {
        val pending = (if (state.chest != null) 1 else 0) + state.chestQueue.size
        if (pending >= MAX_PENDING_CHESTS) return false
        val r = rnd.nextDouble()
        val type = when {
            r < 0.60 -> "bois"
            r < 0.90 -> "fer"
            else -> "runique"
        }
        val expires = System.currentTimeMillis() + CHEST_LIFETIME_MS
        val chest = when (type) {
            "bois" -> ChestData("bois", 150, goldMin = 40, goldMax = 80, rarity = "uncommon", expiresAt = expires)
            "fer" -> ChestData("fer", 400, goldMin = 130, goldMax = 220, rarity = "rare", expiresAt = expires)
            else -> ChestData("runique", 900, goldMin = 380, goldMax = 550, rarity = "epic", expiresAt = expires)
        }
        if (state.chest == null) {
            state.chest = chest
        } else {
            state.chestQueue = state.chestQueue + chest
        }
        return true
    }

    fun expireChests(state: GameState, now: Long = System.currentTimeMillis()) {
        val current = state.chest
        if (current != null && current.expiresAt in 1..now) {
            state.chest = null
            state.journal = addJournal(state, "${chestTypeLabel(current.type)} disparu après 24 h", "bad")
            if (state.mode == "chest") state.mode = "walk"
        }
        if (state.chestQueue.any { it.expiresAt in 1..now }) {
            val expired = state.chestQueue.filter { it.expiresAt in 1..now }
            expired.forEach {
                state.journal = addJournal(state, "${chestTypeLabel(it.type)} disparu après 24 h", "bad")
            }
            state.chestQueue = state.chestQueue.filter { it.expiresAt !in 1..now }
        }
        if (state.chest == null && state.chestQueue.isNotEmpty()) {
            state.chest = state.chestQueue.first()
            state.chestQueue = state.chestQueue.drop(1)
            if (state.mode == "chest") state.mode = "walk"
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
        val previous = state.equipment.firstOrNull { it.slot == item.slot }
        swapEquipment(state, item, previous)
        state.journal = addJournal(state, "${item.name} équipé", "good")
        return true
    }

    /**
     * Compares every slot with the backpack and equips each strict improvement.
     * Returns the number of pieces swapped.
     */
    fun equipBest(state: GameState): Int {
        val oldMax = statsOf(state).maxHp
        var changes = 0
        listOf("weapon", "head", "body", "hands", "feet", "talisman").forEach { slot ->
            val current = state.equipment.firstOrNull { it.slot == slot }
            val best = state.inventory
                .filter { it.slot == slot }
                .maxByOrNull { itemScore(it) }
                ?: return@forEach
            if (current == null || itemScore(best) > itemScore(current)) {
                state.equipment = state.equipment.filter { it.slot != slot } + best
                state.inventory = state.inventory.filter { it.id != best.id } +
                    listOfNotNull(current)
                changes++
            }
        }
        if (changes > 0) {
            val newMax = statsOf(state).maxHp
            state.hp = (state.hp * newMax / oldMax).coerceIn(1.0, newMax.toDouble())
            state.journal = addJournal(state, "Équipement optimisé · $changes pièce(s)", "good")
        }
        return changes
    }

    private fun swapEquipment(state: GameState, item: Item, previous: Item?) {
        val oldMax = statsOf(state).maxHp
        state.equipment = state.equipment.filter { it.slot != item.slot } + item
        state.inventory = state.inventory.filter { it.id != item.id } + listOfNotNull(previous)
        val newMax = statsOf(state).maxHp
        state.hp = (state.hp * newMax / oldMax).coerceIn(1.0, newMax.toDouble())
    }

    fun sellItem(state: GameState, itemId: String): Boolean {
        val item = state.inventory.firstOrNull { it.id == itemId } ?: return false
        if (item.locked) return false
        state.inventory = state.inventory.filter { it.id != itemId }
        state.gold += item.value
        state.journal = addJournal(state, "${item.name} vendu (+${item.value} or)", "neutral")
        return true
    }

    fun sellCommons(state: GameState): Int {
        val commons = state.inventory.filter { it.rarity == "common" && !it.locked }
        if (commons.isEmpty()) return 0
        val gold = commons.sumOf { it.value }
        state.inventory = state.inventory.filterNot { it.rarity == "common" && !it.locked }
        state.gold += gold
        state.journal = addJournal(state, "${commons.size} objet(s) commun(s) vendu(s) · +$gold or", "neutral")
        return gold
    }

    fun toggleLock(state: GameState, itemId: String): Boolean? {
        val item = state.inventory.firstOrNull { it.id == itemId } ?: return null
        val updated = item.copy(locked = !item.locked)
        state.inventory = state.inventory.map { if (it.id == itemId) updated else it }
        state.journal = addJournal(
            state,
            if (updated.locked) "${item.name} verrouillé" else "${item.name} déverrouillé",
            "neutral"
        )
        return updated.locked
    }

    fun fuseItem(state: GameState, itemId: String): Item? {
        val item = state.inventory.firstOrNull { it.id == itemId } ?: return null
        if (item.locked) return null
        val group = state.inventory
            .filter { it.slot == item.slot && it.rarity == item.rarity && !it.locked }
            .sortedByDescending { itemScore(it) }
        if (group.size < 3) return null
        val chosen = group.take(3)
        if (chosen.none { it.id == itemId }) return null
        val next = nextRarity(item.rarity)
        val effect = when {
            next == "legendary" -> ITEM_EFFECTS.random(rnd)
            else -> chosen.firstNotNullOfOrNull { it.effect.ifBlank { null } } ?: ""
        }
        val fused = item.copy(
            id = "fused_${System.nanoTime()}",
            name = "${item.name} fusionné",
            rarity = next,
            level = chosen.maxOf { it.level },
            atk = chosen.sumOf { it.atk } * 3 / 4,
            hp = chosen.sumOf { it.hp } * 3 / 4,
            vitPct = chosen.sumOf { it.vitPct } * 3 / 4,
            critPct = chosen.sumOf { it.critPct } * 3 / 4,
            regen = (chosen.sumOf { it.regen } * 0.75 * 10).roundToInt() / 10.0,
            value = chosen.sumOf { it.value } + 120,
            effect = effect,
            locked = false
        )
        state.inventory = state.inventory.filterNot { it.id in chosen.map { c -> c.id } }
        addLoot(state, fused)
        state.journal = addJournal(state, "Fusion · ${fused.name} (${rarityLabel(next)})", "good")
        return fused
    }

    fun addLoot(state: GameState, item: Item) {
        if (state.inventory.size < INVENTORY_CAP) {
            state.inventory = state.inventory + item
        } else {
            state.stash = state.stash + item
            state.journal = addJournal(state, "Inventaire plein · ${item.name} au coffre de camp", "neutral")
        }
    }

    fun withdrawStash(state: GameState): Int {
        var moved = 0
        while (state.stash.isNotEmpty() && state.inventory.size < INVENTORY_CAP) {
            state.inventory = state.inventory + state.stash.first()
            state.stash = state.stash.drop(1)
            moved++
        }
        if (moved > 0) {
            state.journal = addJournal(state, "$moved objet(s) repris au coffre de camp", "neutral")
        }
        return moved
    }

    fun freeRest(state: GameState): Boolean {
        if (state.freeRestDay == state.day) return false
        val s = statsOf(state)
        if (state.hp >= s.maxHp) return false
        state.freeRestDay = state.day
        val heal = s.maxHp * 0.25
        state.hp = min(s.maxHp.toDouble(), state.hp + heal)
        state.journal = addJournal(state, "Veillée gratuite (+${heal.roundToInt()} PV)", "good")
        return true
    }

    fun chestWarnings(state: GameState, now: Long = System.currentTimeMillis()): List<String> {
        val out = mutableListOf<String>()
        fun inspect(c: ChestData?) {
            if (c == null || c.expiryNotified) return
            if (c.expiresAt > 0L && c.expiresAt - now in 1..CHEST_WARNING_MS) {
                c.expiryNotified = true
                out += "${chestTypeLabel(c.type)} expire dans ${formatRemaining(c.expiresAt - now)}"
            }
        }
        inspect(state.chest)
        state.chestQueue.forEach { inspect(it) }
        return out
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
        var finalRarity = rarity
        if (rarity == "epic" && rnd.nextDouble() < 0.12) finalRarity = "legendary"
        if (rarity == "rare" && rnd.nextDouble() < 0.03) finalRarity = "legendary"
        val base = LOOT_TEMPLATES[rnd.nextInt(LOOT_TEMPLATES.size)]
        val mult = when (finalRarity) {
            "uncommon" -> 1.25
            "rare" -> 1.6
            "epic" -> 2.0
            "legendary" -> 2.4
            else -> 1.0
        }
        val effect = when {
            finalRarity == "legendary" -> ITEM_EFFECTS.random(rnd)
            (finalRarity == "epic" || finalRarity == "rare") && rnd.nextDouble() < 0.35 ->
                ITEM_EFFECTS.random(rnd)
            else -> ""
        }
        fun scaled(v: Int): Int = max(1, (v * mult).roundToInt())
        return base.copy(
            id = "loot_${System.nanoTime()}",
            name = if (effect.isEmpty()) base.name else "${base.name} · ${effectLabel(effect)}",
            level = max(1, floor),
            rarity = finalRarity,
            atk = scaled(base.atk),
            hp = scaled(base.hp),
            vitPct = scaled(base.vitPct),
            critPct = scaled(base.critPct),
            regen = (base.regen * mult * 10).roundToInt() / 10.0,
            value = scaled(base.value),
            effect = effect
        )
    }

    fun addJournal(state: GameState, text: String, type: String = "neutral"): List<JournalEntry> {
        val entry = JournalEntry(text, nowTime(), type)
        return (listOf(entry) + state.journal).take(80)
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

