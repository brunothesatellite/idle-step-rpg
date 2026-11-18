package com.brunothesatellite.idlesteprpg.engine

import java.time.LocalDate
import java.time.temporal.IsoFields
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sqrt
import kotlin.random.Random

data class OfflineSim(
    val combats: Int,
    val gold: Int,
    val floors: Int,
    val kills: Int
)

/**
 * Résultat détaillé d'une action de forge, pour n'annoncer au joueur
 * que ce qui manque réellement.
 */
sealed interface ForgeOutcome {
    data object Success : ForgeOutcome
    data object NotFound : ForgeOutcome
    data object NotIdentified : ForgeOutcome
    data object Locked : ForgeOutcome

    /** [gold] et [shards] valent 0 quand la ressource est suffisante. */
    data class Missing(val gold: Int, val shards: Int) : ForgeOutcome
}

data class AbsenceSim(
    val elapsedMs: Long,
    val simulatedMs: Long,
    val stepsConsumed: Int,
    val remainingBank: Int,
    val combats: Int,
    val bosses: Int,
    val gold: Int,
    val floors: Int,
    val kills: Int,
    val levels: Int,
    val chests: Int,
    val stoppedOnSpecial: Boolean,
    val skippedCamp: Boolean,
    val defeated: Boolean,
    val stoppedOnBoss: Boolean = false,
    val stoppedOnChest: Boolean = false,
    val lootItems: List<String> = emptyList(),
    val consumables: Int = 0,
    val rations: Int = 0,
    val shards: Int = 0
)

data class SpecialOutcome(
    val text: String,
    val good: Boolean
)

object GameEngine {

    private val rnd = Random.Default

    fun migrate(state: GameState): GameState {
        if (state.totalSteps <= 0 && state.stepLedger.isNotEmpty()) {
            state.totalSteps = state.stepLedger.values.sum()
        }
        if (state.lastActiveDay.isBlank()) state.lastActiveDay = state.day
        if (state.bossKills.isEmpty() && state.bestiary.isNotEmpty()) {
            val kills = mutableMapOf<String, Int>()
            allBossTemplates().forEach { boss ->
                val count = state.bestiary.entries
                    .filter { it.key == boss.name || it.key.startsWith("${boss.name} ·") }
                    .sumOf { it.value }
                if (count > 0) kills[boss.name] = count
            }
            if (kills.isNotEmpty()) state.bossKills = kills
        }
        if (state.version < SAVE_VERSION) state.version = SAVE_VERSION
        return state
    }

    private fun weekKey(date: LocalDate): String =
        "${date.get(IsoFields.WEEK_BASED_YEAR)}-${date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)}"

    private fun advanceStreak(state: GameState, date: LocalDate) {
        val last = runCatching { LocalDate.parse(state.lastActiveDay) }.getOrNull()
        state.streak = when {
            last == null -> 1
            last == date -> state.streak.coerceAtLeast(1)
            last.plusDays(1) == date -> state.streak + 1
            else -> 1
        }
        state.lastActiveDay = date.toString()
    }

    private fun resetWeekIfNeeded(state: GameState, date: LocalDate) {
        val key = weekKey(date)
        if (state.weekKey != key) {
            state.weekKey = key
            state.weeklySteps = 0
        }
    }

    fun ensureDay(state: GameState, date: LocalDate = LocalDate.now()) {
        val d = date.toString()
        migrateChests(state)
        resetWeekIfNeeded(state, date)
        if (state.day.isEmpty()) {
            state.day = d
            state.hp = statsOf(state).maxHp.toDouble()
            advanceStreak(state, date)
            return
        }
        if (state.day != d) {
            state.day = d
            state.stepsToday = 0
            state.processedSteps = 0
            state.offlineStepsToday = 0
            state.ferveur = 0
            state.ferveurDecayMs = 0.0
            state.ferveurDecayAt = 0L
            state.genRations = 0
            state.genShards = 0
            state.lastAdded = 0
            state.bossesToday = 0
            state.chestsToday = 0
            state.oaths = oathsFor(date)
            state.special = null
            advanceStreak(state, date)
            pruneLedger(state, date)
            state.journal = addJournal(state, "Nouveau jour — 00:00 · serments renouvelés", "neutral")
            if (state.streak > 1) {
                state.journal = addJournal(state, "Série de ${state.streak} jours de marche", "good")
            }
        }
    }

    private fun pruneLedger(state: GameState, date: LocalDate) {
        val cutoff = date.minusDays(35).toString()
        if (state.stepLedger.keys.any { it < cutoff }) {
            state.stepLedger = state.stepLedger.filterKeys { it >= cutoff }
        }
    }

    fun prepareCamp(state: GameState) {
        if (state.mode == "chest") state.mode = "walk"
        if (state.mode == "farmWait") {
            state.mode = "walk"
            state.farmWaitMs = 0.0
        }
        state.farmKills = 0
        state.fight?.let { f ->
            f.enemyHp = f.enemyMaxHp
            f.heroTimerMs = 380.0
            f.enemyTimerMs = f.enemyIntervalMs.toDouble()
            f.elapsedMs = 0.0
            f.specialMs = 8000.0
            f.usedRation = false
            f.lastCrit = false
            state.hp = max(state.hp, statsOf(state).maxHp * 0.5)
            state.journal = addJournal(state, "Camp · combat réinitialisé", "neutral")
        }
    }

    fun goldGain(state: GameState, base: Int): Int =
        (base * (1.0 + 0.05 * milestoneBonus(state.floor))).roundToInt()

    fun addLegend(state: GameState, text: String, type: String = "good") {
        state.legend = (listOf(JournalEntry(text, nowTime(), type)) + state.legend).take(120)
    }

    private fun recordKill(state: GameState, name: String) {
        state.kills++
        state.bestiary = state.bestiary + (name to ((state.bestiary[name] ?: 0) + 1))
    }

    fun simulateOffline(
        state: GameState,
        maxIterations: Int = 900,
        dtMs: Double = 500.0
    ): OfflineSim {
        // La synchro de fond partage le budget hors ligne de la reprise.
        val cap = min(maxIterations * dtMs.toLong(), OFFLINE_SIM_MAX_MS)
        val sim = simulateAbsence(state, elapsedMs = cap, capMs = cap, dtMs = dtMs)
        return OfflineSim(
            combats = sim.combats,
            gold = sim.gold,
            floors = sim.floors,
            kills = sim.kills
        )
    }

    /** Nombre de pas encore autorisés à être consommés hors ligne aujourd'hui. */
    fun offlineBudget(state: GameState): Int =
        (state.stepsToday * OFFLINE_BANK_SHARE).toInt()
            .minus(state.offlineStepsToday)
            .coerceAtLeast(0)

    /**
     * Progression hors ligne : simule le jeu avec un budget de pas (30 % des pas
     * du jour) et une durée maximale, en s'arrêtant sur un événement pour que le
     * joueur retrouve toujours quelque chose à regarder.
     */
    fun simulateAbsence(
        state: GameState,
        elapsedMs: Long,
        capMs: Long = OFFLINE_SIM_MAX_MS,
        dtMs: Double = 500.0
    ): AbsenceSim {
        val startBank = state.stepBank.toInt()
        val startGold = state.gold
        val startFloor = state.floor
        val startKills = state.kills
        val startBosses = state.bosses
        val startLevel = state.level
        val startChests = state.chestsOpened
        val startRetreats = state.retreats
        val startChest = state.chest
        val namesBefore = (state.inventory + state.stash).map { it.name }.toSet()
        val consumablesBefore = state.consumables.values.sum()
        val rationsBefore = state.rations
        val shardsBefore = state.shards

        val budget = min(offlineBudget(state), startBank)
        if (elapsedMs <= 0L || startBank <= 0 || budget <= 0 ||
            state.mode == "camp" || state.mode == "special"
        ) {
            return AbsenceSim(
                elapsedMs = elapsedMs,
                simulatedMs = 0L,
                stepsConsumed = 0,
                remainingBank = startBank,
                combats = 0,
                bosses = 0,
                gold = 0,
                floors = 0,
                kills = 0,
                levels = 0,
                chests = 0,
                stoppedOnSpecial = state.mode == "special",
                skippedCamp = state.mode == "camp",
                defeated = false
            )
        }

        val floorSteps = (FLOOR_METERS * STEPS_PER_METER).toInt()
        val maxIterations = (capMs / dtMs.toLong()).toInt().coerceAtLeast(1)
        var iterations = 0
        var stoppedOnSpecial = false
        var stoppedOnBoss = false
        var stoppedOnChest = false
        var stoppedOnCamp = false
        while (iterations < maxIterations && state.stepBank > 0.0) {
            val consumed = (startBank - state.stepBank).toInt().coerceAtLeast(0)
            if (consumed >= budget) break
            tick(state, dtMs)
            iterations++
            if (state.mode == "special") {
                stoppedOnSpecial = true
                break
            }
            if (state.mode == "camp") {
                stoppedOnCamp = true
                break
            }
            if (startChest == null && state.chest != null) {
                stoppedOnChest = true
                break
            }
            // Ne pas engloutir un gardien qu'on ne pourrait pas terminer : on
            // s'arrête devant le portail s'il reste moins d'un palier de budget.
            val remaining = budget - (startBank - state.stepBank).toInt().coerceAtLeast(0)
            if (state.fight == null && state.distance >= GATE_METERS && remaining < floorSteps) {
                stoppedOnBoss = true
                break
            }
        }
        val consumed = (startBank - state.stepBank).toInt().coerceAtLeast(0)
        state.offlineStepsToday += consumed
        val loot = (state.inventory + state.stash)
            .map { it.name }
            .filterNot { it in namesBefore }
            .distinct()
            .take(12)
        return AbsenceSim(
            elapsedMs = elapsedMs,
            simulatedMs = (iterations * dtMs).toLong(),
            stepsConsumed = consumed,
            remainingBank = state.stepBank.toInt(),
            combats = (state.kills - startKills) + (state.bosses - startBosses),
            bosses = state.bosses - startBosses,
            gold = max(0, state.gold - startGold),
            floors = max(0, state.floor - startFloor),
            kills = state.kills - startKills,
            levels = (state.level - startLevel).coerceAtLeast(0),
            chests = (state.chestsOpened - startChests).coerceAtLeast(0),
            stoppedOnSpecial = stoppedOnSpecial,
            skippedCamp = stoppedOnCamp,
            defeated = state.retreats > startRetreats,
            stoppedOnBoss = stoppedOnBoss,
            stoppedOnChest = stoppedOnChest,
            lootItems = loot,
            consumables = (state.consumables.values.sum() - consumablesBefore).coerceAtLeast(0),
            rations = (state.rations - rationsBefore).coerceAtLeast(0),
            shards = (state.shards - shardsBefore).coerceAtLeast(0)
        )
    }

    fun mergeOfflineReports(a: OfflineReport?, b: OfflineReport?): OfflineReport? {
        if (a == null) return b
        if (b == null) return a
        return a.copy(
            days = a.days + b.days,
            steps = a.steps + b.steps,
            distanceMeters = a.distanceMeters + b.distanceMeters,
            combats = a.combats + b.combats,
            gold = a.gold + b.gold,
            floors = a.floors + b.floors,
            kills = a.kills + b.kills,
            minutes = a.minutes + b.minutes,
            stepsConsumed = a.stepsConsumed + b.stepsConsumed,
            remainingSteps = b.remainingSteps,
            bosses = a.bosses + b.bosses,
            levels = a.levels + b.levels,
            chests = a.chests + b.chests,
            lootItems = (a.lootItems + b.lootItems).distinct(),
            consumables = a.consumables + b.consumables,
            rations = a.rations + b.rations,
            shards = a.shards + b.shards,
            reason = when {
                a.reason.isBlank() -> b.reason
                b.reason.isBlank() || a.reason == b.reason -> a.reason
                else -> "mixed"
            },
            special = a.special || b.special,
            defeated = a.defeated || b.defeated,
            createdAt = minOf(a.createdAt, b.createdAt),
            acknowledgedAt = 0L
        )
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
        val now = System.currentTimeMillis()
        if (state.buffs.any { it.expiresAt <= now }) {
            state.buffs = state.buffs.filter { it.expiresAt > now }
        }
        tickFerveur(state, dtMs)
        val s = statsOf(state)
        state.hp = state.hp.coerceIn(0.0, s.maxHp.toDouble())

        val fighting = state.mode == "fight" || state.mode == "boss"
        val bonusRegen = (s.regen - 0.6).coerceAtLeast(0.0)
        val regen = (s.maxHp * 0.006 * (1.0 + 0.008 * state.ferveur) + bonusRegen) / 1000.0 *
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
        val now = System.currentTimeMillis()
        if (state.ferveur <= 0) {
            state.ferveurDecayMs = 0.0
            state.ferveurDecayAt = now
            return
        }
        if (state.lastStepAt <= 0L) {
            state.lastStepAt = now
            state.ferveurDecayAt = now
            return
        }
        val graceEnd = state.lastStepAt + FERVEUR_GRACE_MS
        val from = max(graceEnd, if (state.ferveurDecayAt > 0L) state.ferveurDecayAt else graceEnd)
        val elapsed = now - from
        if (elapsed <= 0L) {
            state.ferveurDecayAt = now
            state.ferveurDecayMs = 0.0
            return
        }
        val total = state.ferveurDecayMs + elapsed.toDouble()
        val points = (total / FERVEUR_DECAY_STEP_MS).toInt()
        if (points > 0) {
            state.ferveur = max(0, state.ferveur - points)
            state.ferveurDecayMs = total - points * FERVEUR_DECAY_STEP_MS
        } else {
            state.ferveurDecayMs = total
        }
        state.ferveurDecayAt = now
    }

    private fun tickWalk(state: GameState, dtMs: Double) {
        if (state.stepBank <= 0.0) return
        val rate = if (state.stepBank > WALK_SLOW_THRESHOLD) WALK_STEPS_PER_SECOND else WALK_STEPS_PER_SECOND_SLOW
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
        val type = listOf("autel", "source", "marchand", "statue", "risque").random(rnd)
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
            "risque" -> SpecialData(
                type = type,
                title = "COFFRE SCELLÉ",
                text = "Une serrure sans clé. Le métal vibre faiblement.",
                acceptLabel = "PARIER 100 OR",
                goldCost = 100,
                rewardText = "300 or ou rien"
            )
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

    fun resolveSpecial(state: GameState, accept: Boolean): SpecialOutcome {
        val special = state.special ?: return SpecialOutcome("Aucune rencontre.", false)
        var outcome: SpecialOutcome? = null
        if (accept) {
            when (special.type) {
                "autel" -> outcome = if (state.shards >= special.shardCost) {
                    state.shards -= special.shardCost
                    gainXp(state, 180)
                    SpecialOutcome("Offrande acceptée · +180 XP", true)
                } else {
                    SpecialOutcome("2 éclats requis.", false)
                }
                "source" -> outcome = if (state.rations >= special.rationCost) {
                    state.rations -= special.rationCost
                    state.hp = statsOf(state).maxHp.toDouble()
                    SpecialOutcome("PV entièrement restaurés.", true)
                } else {
                    SpecialOutcome("1 ration requise.", false)
                }
                "marchand" -> {
                    val offer = special.offer
                    outcome = if (offer != null && state.gold >= special.goldCost) {
                        state.gold -= special.goldCost
                        addLoot(state, offer)
                        SpecialOutcome("${offer.name} acheté.", true)
                    } else {
                        SpecialOutcome("Or insuffisant.", false)
                    }
                }
                "statue" -> outcome = if (state.gold >= special.goldCost) {
                    state.gold -= special.goldCost
                    state.ferveur = min(100, state.ferveur + 15)
                    state.lastStepAt = System.currentTimeMillis()
                    state.shards = min(12, state.shards + 1)
                    SpecialOutcome("Ferveur +15 · +1 éclat.", true)
                } else {
                    SpecialOutcome("100 or requis.", false)
                }
                "risque" -> outcome = if (state.gold >= special.goldCost) {
                    state.gold -= special.goldCost
                    if (rnd.nextDouble() < 0.45) {
                        state.gold += 300
                        state.goldEarned += 200
                        SpecialOutcome("Pari gagné · +300 or", true)
                    } else {
                        SpecialOutcome("Pari perdu · −100 or", false)
                    }
                } else {
                    SpecialOutcome("100 or requis.", false)
                }
            }
        }
        val finalOutcome = outcome ?: SpecialOutcome(
            "${special.title} · passé sans s'arrêter",
            false
        )
        state.journal = addJournal(
            state,
            finalOutcome.text,
            if (finalOutcome.good) "good" else "neutral"
        )
        state.special = null
        state.mode = "walk"
        state.nextFight += 10
        return finalOutcome
    }

    fun addConsumable(state: GameState, id: String, quantity: Int = 1) {
        if (quantity <= 0) return
        val current = state.consumables[id] ?: 0
        state.consumables = state.consumables + (id to min(99, current + quantity))
    }

    private fun addBuff(
        state: GameState,
        type: String,
        magnitude: Double,
        durationMs: Long,
        label: String
    ) {
        val expiresAt = System.currentTimeMillis() + durationMs
        state.buffs = state.buffs.filter { it.type != type } +
            Buff(type, magnitude, expiresAt, label)
    }

    fun useConsumable(state: GameState, id: String): SpecialOutcome {
        val template = consumableById(id)
            ?: return SpecialOutcome("Consommable inconnu.", false)
        val count = state.consumables[id] ?: 0
        if (count <= 0) return SpecialOutcome("Aucun ${template.name}.", false)
        if (template.context == "combat" && state.fight == null) {
            return SpecialOutcome("${template.name} · utilisable en combat uniquement.", false)
        }
        val stats = statsOf(state)
        val maxHp = stats.maxHp.toDouble()
        var message: String
        when (template.effect) {
            "heal_pct" -> {
                if (state.hp >= maxHp) return SpecialOutcome("PV déjà complets.", false)
                val heal = maxHp * template.magnitude
                state.hp = min(maxHp, state.hp + heal)
                message = "+${heal.roundToInt()} PV"
            }
            "heal_ferveur" -> {
                if (state.hp >= maxHp && state.ferveur >= 100) {
                    return SpecialOutcome("PV et ferveur déjà au maximum.", false)
                }
                val heal = maxHp * template.magnitude
                state.hp = min(maxHp, state.hp + heal)
                state.ferveur = min(100, state.ferveur + template.secondary.toInt())
                state.lastStepAt = System.currentTimeMillis()
                message = "+${heal.roundToInt()} PV · +${template.secondary.toInt()} ferveur"
            }
            "ferveur" -> {
                if (state.ferveur >= 100) return SpecialOutcome("Ferveur déjà au maximum.", false)
                state.ferveur = min(100, state.ferveur + template.magnitude.toInt())
                state.lastStepAt = System.currentTimeMillis()
                message = "+${template.magnitude.toInt()} ferveur"
            }
            "full_heal" -> {
                if (state.hp >= maxHp) return SpecialOutcome("PV déjà complets.", false)
                state.hp = maxHp
                message = "PV complets"
            }
            "heal_ration" -> {
                if (state.hp >= maxHp) return SpecialOutcome("PV déjà complets.", false)
                val heal = maxHp * template.magnitude
                state.hp = min(maxHp, state.hp + heal)
                state.rations = min(24, state.rations + template.secondary.toInt())
                message = "+${heal.roundToInt()} PV · +${template.secondary.toInt()} ration"
            }
            "damage" -> {
                val fight = state.fight
                    ?: return SpecialOutcome("${template.name} · utilisable en combat uniquement.", false)
                val damage = (stats.atk * template.magnitude).roundToInt()
                fight.enemyHp -= damage
                message = "$damage dégâts"
                if (fight.enemyHp <= 0) victory(state, fight)
            }
            "buff_atk" -> {
                addBuff(state, "atk", template.magnitude, template.durationMs, "RAGE")
                message = "ATQ +${(template.magnitude * 100).roundToInt()} % · ${template.durationMs / 1000} s"
            }
            "buff_def" -> {
                addBuff(state, "def", template.magnitude, template.durationMs, "ÉCORCE")
                message = "Dégâts −${(template.magnitude * 100).roundToInt()} % · ${template.durationMs / 1000} s"
            }
            "buff_vit" -> {
                addBuff(state, "vit", template.magnitude, template.durationMs, "VIF")
                message = "VIT +${(template.magnitude * 100).roundToInt()} % · ${template.durationMs / 1000} s"
            }
            "buff_crit" -> {
                addBuff(state, "crit", template.magnitude, template.durationMs, "CRIT")
                message = "Critique +${(template.magnitude * 100).roundToInt()} % · ${template.durationMs / 1000} s"
            }
            "buff_lifesteal" -> {
                addBuff(state, "lifesteal", template.magnitude, template.durationMs, "VAMP")
                message = "Vol de vie +${(template.magnitude * 100).roundToInt()} % · ${template.durationMs / 1000} s"
            }
            "buff_regen" -> {
                addBuff(state, "regen", template.magnitude, template.durationMs, "RÉGÉN")
                message = "Régénération +${"%.1f".format(template.magnitude)} PV/s · ${template.durationMs / 1000} s"
            }
            "berserk" -> {
                addBuff(state, "atk", template.magnitude, template.durationMs, "BERSERK")
                addBuff(state, "vulnerable", template.secondary, template.durationMs, "FRAGILE")
                message = "ATQ +${(template.magnitude * 100).roundToInt()} % · vulnérable"
            }
            else -> return SpecialOutcome("Effet inconnu.", false)
        }
        val next = state.consumables.toMutableMap()
        if (count <= 1) next.remove(id) else next[id] = count - 1
        state.consumables = next
        state.journal = addJournal(state, "Consommé · ${template.name} · $message", "good")
        return SpecialOutcome("${template.name} · $message", true)
    }

    private fun rollConsumable(floor: Int, rich: Boolean): ConsumableTemplate {
        val weighted = CONSUMABLES.map { template ->
            val base = when (template.rarity) {
                "common" -> 60.0
                "uncommon" -> 28.0
                "rare" -> 10.0 + min(15, floor / 4)
                else -> 4.0
            }
            val boost = if (rich && template.rarity != "common") 1.6 else 1.0
            template to base * boost
        }
        val total = weighted.sumOf { it.second }
        var roll = rnd.nextDouble() * total
        for ((template, weight) in weighted) {
            roll -= weight
            if (roll <= 0.0) return template
        }
        return weighted.last().first
    }

    fun dropConsumables(state: GameState, source: String, forcedCount: Int = 0) {
        val chance = when (source) {
            "boss" -> 1.0
            "chest" -> 0.70
            else -> 0.18
        }
        if (forcedCount <= 0 && rnd.nextDouble() > chance) return
        val quantity = if (forcedCount > 0) forcedCount else when (source) {
            "boss" -> rnd.nextInt(1, 3)
            "chest" -> rnd.nextInt(1, 3)
            else -> 1
        }
        val rich = source != "mob" || state.floor >= 10
        val names = mutableListOf<String>()
        repeat(quantity) {
            val template = rollConsumable(state.floor, rich)
            addConsumable(state, template.id)
            names += template.name
        }
        if (names.isNotEmpty()) {
            val text = names.groupingBy { it }.eachCount()
                .entries.joinToString(" · ") { "${it.key} ×${it.value}" }
            state.journal = addJournal(state, "Butin · $text", "good")
        }
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
            val now = System.currentTimeMillis()
            val minutes = max(1L, (now - state.lastSync) / 60_000L)
            val allowed = max(5_000, MAX_STEPS_PER_MINUTE * minutes.toInt())
            val accepted = min(todayAdded, allowed)
            val ignored = todayAdded - accepted
            state.stepsToday = todayTotal
            state.processedSteps = max(state.processedSteps, todayTotal)
            state.ferveur = min(100, state.ferveur + (accepted + 49) / 50)
            state.lastStepAt = now
            state.ferveurDecayMs = 0.0
            state.ferveurDecayAt = now
            state.stepBank += accepted
            state.totalSteps += accepted
            state.weeklySteps += accepted
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
                "$accepted pas Garmin — ${"%.0f".format(accepted / STEPS_PER_METER)} m",
                "good"
            )
            if (ignored > 0) {
                state.journal = addJournal(
                    state,
                    "$ignored pas ignorés (rythme supérieur à $MAX_STEPS_PER_MINUTE/min)",
                    "bad"
                )
            }
        }

        val importedHistory = min(historyAdded, HISTORY_IMPORT_CAP)
        if (importedHistory > 0) {
            state.stepBank += importedHistory
            state.totalSteps += importedHistory
            state.weeklySteps += importedHistory
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
        state.totalSteps += steps
        state.weeklySteps += steps
        state.ferveur = min(100, state.ferveur + (steps + 49) / 50)
        state.lastStepAt = System.currentTimeMillis()
        state.ferveurDecayMs = 0.0
        state.ferveurDecayAt = state.lastStepAt
        state.stepLedger = state.stepLedger + (todayString() to state.stepsToday)
        state.journal = addJournal(state, "Démo +$steps pas", "neutral")
    }

    fun checkOaths(state: GameState) {
        val o = state.oaths
        if (!o.stepsClaimed && state.stepsToday >= o.stepsTarget) {
            o.stepsClaimed = true
            state.gold += 150
            state.goldEarned += 150
            state.shards = min(12, state.shards + 1)
            gainXp(state, 80)
            state.journal = addJournal(state, "Serment de marche accompli · +150 or, +1 éclat", "good")
        }
        if (!o.bossesClaimed && state.bossesToday >= o.bossesTarget) {
            o.bossesClaimed = true
            state.gold += 250
            state.goldEarned += 250
            state.shards = min(12, state.shards + 2)
            gainXp(state, 120)
            state.journal = addJournal(state, "Serment de chasse accompli · +250 or, +2 éclats", "good")
        }
        if (!o.chestsClaimed && state.chestsToday >= o.chestsTarget) {
            o.chestsClaimed = true
            state.gold += 100
            state.goldEarned += 100
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

    private fun depthMultiplier(floor: Int, boss: Boolean = false): Double {
        if (floor <= 10) return 1.0
        val raw = (if (boss) 1.08 else 1.12).pow((floor - 10).toDouble())
        return min(raw, 40.0)
    }

    private fun makeMob(state: GameState): FightData {
        val pos = state.distance
        val biomePool = mobPoolFor(state.floor)
        val pool = when {
            pos >= 85 -> biomePool.hard
            pos >= 50 -> biomePool.mid
            else -> biomePool.easy
        }
        val t = pool[rnd.nextInt(pool.size)]
        val elite = rnd.nextDouble() < 0.10
        val rule = biomeRuleFor(state.floor)
        var level = state.floor + when {
            pos < 30 -> -1
            pos < 70 -> 0
            else -> 1
        }
        if (elite) level += 2
        level = max(1, level)
        val depth = depthMultiplier(state.floor)
        val hp = (55 + 34 * (level - 1)) * (if (elite) 2.2 else 1.0)
        val atk = (6 + 2.2 * (level - 1)) * (if (elite) 1.3 else 1.0)
        val reward = min(6.0, sqrt(depth))
        val gold = ((6 + 3 * level) * (if (elite) 1.6 else 1.0) * reward).roundToInt()
        val xp = ((12 + 5 * level) * (if (elite) 1.8 else 1.0) * reward).roundToInt()
        return FightData(
            enemyName = t.name,
            enemyIcon = t.icon,
            enemyLevel = level,
            enemyHp = hp * depth * rule.enemyHpMult,
            enemyMaxHp = hp * depth * rule.enemyHpMult,
            enemyAtk = atk * depth * rule.enemyAtkMult,
            enemyIntervalMs = ((if (elite) 1100 else 1250) * rule.enemyIntervalMult).roundToLong(),
            gold = gold,
            xp = xp,
            elite = elite
        )
    }

    private fun makeBoss(state: GameState): FightData {
        val t = bossFor(state.floor)
        val ability = bossAbilityFor(t.name, state.floor)
        val rule = biomeRuleFor(state.floor)
        val level = state.floor + 2
        val depth = depthMultiplier(state.floor, boss = true)
        var hp = 6.0 * (55 + 34 * (level - 1)) * depth
        var atk = 1.6 * (6 + 2.2 * (level - 1)) * depth
        var interval = 1250L
        val modifier = listOf("", "", "rapide", "blinde", "berserk", "regenerant", "voleur").random(rnd)
        when (modifier) {
            "rapide" -> interval = 900L
            "blinde" -> hp *= 1.30
            "berserk" -> atk *= 1.30
            "regenerant" -> hp *= 1.15
            "voleur" -> atk *= 1.10
        }
        val suffix = when (modifier) {
            "rapide" -> " · RAPIDE"
            "blinde" -> " · BLINDÉ"
            "berserk" -> " · BERSERK"
            "regenerant" -> " · RÉGÉNÉRANT"
            "voleur" -> " · VOLEUR"
            else -> ""
        }
        val reward = min(10.0, sqrt(depth))
        return FightData(
            enemyName = t.name + suffix,
            enemyIcon = t.icon,
            enemyLevel = level,
            enemyHp = hp * rule.enemyHpMult,
            enemyMaxHp = hp * rule.enemyHpMult,
            enemyAtk = atk * rule.enemyAtkMult,
            enemyIntervalMs = (interval * rule.enemyIntervalMult).roundToLong(),
            gold = (12 * (6 + 3 * level) * reward).roundToInt(),
            xp = (10 * (12 + 5 * level) * reward).roundToInt(),
            isBoss = true,
            ability = ability.id,
            abilityLabel = ability.label,
            specialMs = 6500.0,
            modifier = when (modifier) {
                "rapide" -> "RAPIDE"
                "blinde" -> "BLINDÉ"
                "berserk" -> "BERSERK"
                "regenerant" -> "RÉGÉNÉRANT"
                "voleur" -> "VOLEUR"
                else -> ""
            }
        )
    }

    private fun makeFarm(state: GameState): FightData {
        val easy = mobPoolFor(state.floor).easy
        val t = easy[rnd.nextInt(easy.size)]
        val rule = biomeRuleFor(state.floor)
        val level = max(1, state.floor - 2)
        val hp = (55 + 34 * (level - 1)) * 0.65 * rule.enemyHpMult
        val atk = (6 + 2.2 * (level - 1)) * 0.6 * rule.enemyAtkMult
        return FightData(
            enemyName = t.name,
            enemyIcon = t.icon,
            enemyLevel = level,
            enemyHp = hp,
            enemyMaxHp = hp,
            enemyAtk = atk,
            enemyIntervalMs = (1250 * rule.enemyIntervalMult).roundToLong(),
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
        f.abilityFlashMs = (f.abilityFlashMs - dtMs).coerceAtLeast(0.0)
        f.shieldMs = (f.shieldMs - dtMs).coerceAtLeast(0.0)

        if (f.isBoss && f.enemyHp < f.enemyMaxHp * 0.5 && f.enemyIntervalMs > 1000) {
            f.enemyIntervalMs = 950
            state.journal = addJournal(state, "Le gardien s'embrase", "bad")
        }
        if (f.isBoss && f.modifier == "RÉGÉNÉRANT" && f.enemyHp < f.enemyMaxHp) {
            f.enemyHp = min(f.enemyMaxHp, f.enemyHp + f.enemyMaxHp * 0.008 * dtMs / 1000.0)
        }
        if (f.isBoss && f.specialMs <= 0.0) {
            triggerBossAbility(state, f)
            if (state.fight == null) return
            f.specialMs = 7000.0
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

    private fun triggerBossAbility(state: GameState, f: FightData) {
        val s = statsOf(state)
        fun damage(multiplier: Double): Double {
            val raw = f.enemyAtk * multiplier * (0.9 + rnd.nextDouble() * 0.1)
            val dmg = raw * (1.0 - s.damageReduction) * (1.0 + s.damageTaken)
            state.hp -= dmg
            return dmg
        }
        when (f.ability) {
            "slam" -> damage(2.4)
            "drain" -> {
                val dmg = damage(1.5)
                f.enemyHp = min(f.enemyMaxHp, f.enemyHp + dmg * 0.8)
            }
            "regen" -> f.enemyHp = min(f.enemyMaxHp, f.enemyHp + f.enemyMaxHp * 0.07)
            "shield" -> f.shieldMs = 4500.0
            "breath" -> {
                damage(2.0)
                addBuff(state, "vulnerable", 0.15, 6_000, "GIVRE")
            }
            "roots" -> {
                damage(1.2)
                addBuff(state, "vit", -0.25, 5_000, "RACINES")
            }
            "acid" -> {
                damage(1.4)
                addBuff(state, "vulnerable", 0.15, 6_000, "ACIDE")
            }
            "pressure" -> {
                damage(1.8)
                state.ferveur = max(0, state.ferveur - 10)
            }
            else -> damage(1.8)
        }
        f.abilityFlashMs = 1_400.0
        state.journal = addJournal(state, "${f.enemyName} · ${f.abilityLabel}", "alert")
        if (state.hp <= 0) defeat(state, f)
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
        val crit = rnd.nextDouble() < critChance
        f.lastCrit = crit
        if (crit) dmg *= 1.6 + s.critDamage
        if (f.shieldMs > 0.0) dmg *= 0.35
        f.enemyHp -= dmg
        if (s.lifesteal > 0.0) {
            state.hp = min(s.maxHp.toDouble(), state.hp + dmg * s.lifesteal)
        }
        if (f.enemyHp <= 0) victory(state, f)
    }

    private fun enemyAttack(state: GameState, f: FightData) {
        val s = statsOf(state)
        var dmg = f.enemyAtk * (0.9 + rnd.nextDouble() * 0.1)
        dmg *= (1.0 - s.damageReduction) * (1.0 + s.damageTaken)
        state.hp -= dmg
        if (f.isBoss && f.modifier == "VOLEUR") {
            val stolen = max(1, (dmg * 0.5).roundToInt())
            state.gold = max(0, state.gold - stolen)
        }
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

    internal fun victory(state: GameState, f: FightData) {
        val earned = goldGain(state, f.gold)
        state.gold += earned
        state.goldEarned += earned
        gainXp(state, f.xp)
        state.fight = null
        recordKill(state, f.enemyName)

        if (f.isBoss) {
            state.bosses++
            state.bossesToday++
            val clearedFloor = state.floor
            val baseBoss = bossFor(clearedFloor).name
            state.bossKills = state.bossKills + (
                baseBoss to ((state.bossKills[baseBoss] ?: 0) + 1)
                )
            state.bossBlessing = 0.0
            state.mode = "walk"
            state.distance = 0.0
            state.nextFight = 10
            if (state.holdFloor) {
                addLoot(state, lootItem(clearedFloor, "rare"))
                dropConsumables(state, "boss")
                state.journal = addJournal(
                    state,
                    "${f.enemyName} vaincu — palier $clearedFloor conservé (farm)",
                    "good"
                )
            } else {
                state.shards += 2
                state.rations += 1
                state.floor++
                addLoot(state, lootItem(state.floor, "rare"))
                dropConsumables(state, "boss")
                state.journal = addJournal(state, "${f.enemyName} vaincu — palier ${state.floor}", "good")
                if (clearedFloor % 10 == 0) {
                    state.shards += 5
                    addLegend(state, "Jalon · palier $clearedFloor — ${titleFor(state.floor)}")
                    state.journal = addJournal(
                        state,
                        "Jalon · palier $clearedFloor franchi — ${titleFor(state.floor)} · +5 éclats",
                        "good"
                    )
                }
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
            state.journal = addJournal(state, "${f.enemyName} vaincu · +$earned or", "good")
            dropConsumables(state, "mob")
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
        val baseGold = if (c.goldMax > c.goldMin) rnd.nextInt(c.goldMin, c.goldMax + 1) else c.goldMin
        val gold = goldGain(state, baseGold)
        state.gold += gold
        state.goldEarned += gold
        state.chestFlashGold = gold
        state.rations = min(24, state.rations + 1)
        state.shards = min(12, state.shards + 1)
        state.chestsToday++
        state.chestsOpened++
        addLoot(state, lootItem(state.floor, c.rarity))
        val extra = when (c.type) {
            "bois" -> 1
            "fer" -> rnd.nextInt(1, 3)
            else -> rnd.nextInt(2, 4)
        }
        dropConsumables(state, "chest", extra)
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
        if (!item.identified) return false
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
                .filter { it.slot == slot && it.identified }
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
        val item = findItem(state, itemId) ?: return null
        val updated = updateItem(state, itemId) { it.copy(locked = !it.locked) } ?: return null
        state.journal = addJournal(
            state,
            if (updated.locked) "${item.name} verrouillé" else "${item.name} déverrouillé",
            "neutral"
        )
        return updated.locked
    }

    fun withdrawStashItem(state: GameState, itemId: String): Boolean {
        val item = state.stash.firstOrNull { it.id == itemId } ?: return false
        if (state.inventory.size >= INVENTORY_CAP) return false
        state.stash = state.stash.filter { it.id != itemId }
        state.inventory = state.inventory + item
        return true
    }

    fun identifyItem(state: GameState, itemId: String): Boolean {
        val item = findItem(state, itemId) ?: return false
        if (item.identified || state.shards < 1) return false
        updateItem(state, itemId) { it.copy(identified = true) }
        state.shards--
        state.journal = addJournal(state, "Objet identifié · ${item.name}", "good")
        return true
    }

    fun rerollEffect(state: GameState, itemId: String): Boolean =
        rerollEffectDetailed(state, itemId) == ForgeOutcome.Success

    fun rerollEffectDetailed(state: GameState, itemId: String): ForgeOutcome {
        val item = findItem(state, itemId) ?: return ForgeOutcome.NotFound
        if (!item.identified) return ForgeOutcome.NotIdentified
        val missingShards = (2 - state.shards).coerceAtLeast(0)
        if (missingShards > 0) return ForgeOutcome.Missing(0, missingShards)
        val effect = if (rnd.nextDouble() < 0.25) "" else ITEM_EFFECTS.random(rnd)
        val baseName = item.name.substringBefore(" · ")
        updateItem(state, itemId) {
            it.copy(
                name = if (effect.isEmpty()) baseName else "$baseName · ${effectLabel(effect)}",
                effect = effect
            )
        } ?: return ForgeOutcome.NotFound
        state.shards -= 2
        state.journal = addJournal(
            state,
            "Forge · ${item.name} → ${if (effect.isEmpty()) "aucun effet" else effectLabel(effect)}",
            "good"
        )
        return ForgeOutcome.Success
    }

    fun upgradeCost(item: Item): Int = 30 * item.level + 20

    /**
     * Version niveau +1 d'un objet. Chaque statistique non nulle progresse de 8 %
     * avec un gain minimal de +1 : sans ce plancher, 8 % de 2 PV ou de 6 % crit.
     * s'arrondissaient à zéro et le niveau n'apportait rien.
     */
    fun upgradedItem(item: Item): Item = item.copy(
        level = item.level + 1,
        atk = growStat(item.atk),
        hp = growStat(item.hp),
        vitPct = growStat(item.vitPct),
        critPct = growStat(item.critPct),
        regen = if (item.regen > 0) ((item.regen + 0.1) * 10).roundToInt() / 10.0 else item.regen,
        value = (item.value * 1.2).roundToInt()
    )

    private fun growStat(value: Int): Int =
        if (value <= 0) value else value + max(1, (value * 0.08).roundToInt())

    fun upgradeItem(state: GameState, itemId: String): Boolean =
        upgradeItemDetailed(state, itemId) == ForgeOutcome.Success

    fun upgradeItemDetailed(state: GameState, itemId: String): ForgeOutcome {
        val item = findItem(state, itemId) ?: return ForgeOutcome.NotFound
        if (!item.identified) return ForgeOutcome.NotIdentified
        val cost = upgradeCost(item)
        val missingGold = (cost - state.gold).coerceAtLeast(0)
        val missingShards = (1 - state.shards).coerceAtLeast(0)
        if (missingGold > 0 || missingShards > 0) {
            return ForgeOutcome.Missing(missingGold, missingShards)
        }
        val updated = updateItem(state, itemId) { upgradedItem(it) } ?: return ForgeOutcome.NotFound
        state.gold -= cost
        state.shards -= 1
        state.journal = addJournal(state, "Forge · ${updated.name} niveau ${updated.level}", "good")
        return ForgeOutcome.Success
    }

    /** Retrouve un objet, qu'il soit dans le sac ou équipé. */
    fun findItem(state: GameState, itemId: String): Item? =
        state.inventory.firstOrNull { it.id == itemId }
            ?: state.equipment.firstOrNull { it.id == itemId }

    /**
     * Applique [transform] à l'objet où qu'il soit et réajuste les PV
     * proportionnellement quand l'objet modifié est équipé.
     */
    private fun updateItem(state: GameState, itemId: String, transform: (Item) -> Item): Item? {
        val backpackIndex = state.inventory.indexOfFirst { it.id == itemId }
        if (backpackIndex >= 0) {
            val updated = transform(state.inventory[backpackIndex])
            state.inventory = state.inventory.toMutableList().also { it[backpackIndex] = updated }
            return updated
        }
        val equippedIndex = state.equipment.indexOfFirst { it.id == itemId }
        if (equippedIndex >= 0) {
            val before = state.equipment[equippedIndex]
            val updated = transform(before)
            if (updated == before) return updated
            val oldMax = statsOf(state).maxHp
            state.equipment = state.equipment.toMutableList().also { it[equippedIndex] = updated }
            val newMax = statsOf(state).maxHp
            if (oldMax > 0 && newMax != oldMax) {
                state.hp = (state.hp * newMax / oldMax).coerceIn(1.0, newMax.toDouble())
            }
            return updated
        }
        return null
    }

    fun dismantleItem(state: GameState, itemId: String): Int {
        val item = state.inventory.firstOrNull { it.id == itemId } ?: return 0
        if (item.locked) return 0
        val shards = when (item.rarity) {
            "uncommon" -> 2
            "rare" -> 3
            "epic" -> 5
            "legendary" -> 8
            else -> 1
        }
        state.inventory = state.inventory.filter { it.id != itemId }
        state.shards = min(12, state.shards + shards)
        state.journal = addJournal(state, "${item.name} démonté · +$shards éclat(s)", "neutral")
        return shards
    }

    fun fuseItem(state: GameState, itemId: String): Item? {
        val item = state.inventory.firstOrNull { it.id == itemId } ?: return null
        if (item.locked || !item.identified) return null
        val group = state.inventory
            .filter { it.slot == item.slot && it.rarity == item.rarity && !it.locked && it.identified }
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
        val baseName = item.name.substringBefore(" · ")
        if (baseName.isNotBlank() && baseName !in state.discoveredItems) {
            state.discoveredItems = state.discoveredItems + baseName
        }
        if (state.inventory.size < INVENTORY_CAP) {
            state.inventory = state.inventory + item
            return
        }
        val newScore = comparisonScore(item)
        val worst = state.inventory
            .filter { !it.locked }
            .minByOrNull { comparisonScore(it) }
        if (worst != null && newScore > comparisonScore(worst)) {
            state.inventory = state.inventory.filter { it.id != worst.id } + item
            state.stash = state.stash + worst
            state.journal = addJournal(
                state,
                "Inventaire plein · ${itemDisplayName(worst)} au coffre, ${itemDisplayName(item)} conservé",
                "neutral"
            )
        } else {
            state.stash = state.stash + item
            state.journal = addJournal(
                state,
                "Inventaire plein · ${itemDisplayName(item)} au coffre",
                "neutral"
            )
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
        val heal = s.maxHp * 0.15
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
        val templates = lootTemplatesFor(floor)
        val base = templates[rnd.nextInt(templates.size)]
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
        val setPool = BIOME_SETS[biomeKeyFor(floor)] ?: ITEM_SETS
        val setName = when {
            finalRarity in setOf("rare", "epic", "legendary") && rnd.nextDouble() < 0.30 ->
                setPool.random(rnd)
            else -> ""
        }
        val identified = !(finalRarity in setOf("rare", "epic", "legendary") && rnd.nextDouble() < 0.10)
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
            effect = effect,
            identified = identified,
            setName = setName
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


