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
    val kills: Int,
    val defeats: Int = 0
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
    data object MaxLevel : ForgeOutcome

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
    val shards: Int = 0,
    /** Défaites subies pendant la simulation, prolongation de combat comprise. */
    val defeats: Int = 0
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
        // Les anciennes sauvegardes stockaient le nom du gardien avec son
        // modificateur ("Balrug · BLINDÉ"). On fusionne ces clés vers le nom de
        // base pour que le bestiaire et le Codex restent cohérents.
        if (state.bestiary.isNotEmpty()) {
            val merged = state.bestiary.toMutableMap()
            var changed = false
            allBossTemplates().forEach { boss ->
                val keys = merged.keys.filter { it == boss.name || it.startsWith("${boss.name} ·") }
                if (keys.isNotEmpty() && (keys.size > 1 || keys.first() != boss.name)) {
                    val total = keys.sumOf { merged[it] ?: 0 }
                    keys.forEach { merged.remove(it) }
                    if (total > 0) merged[boss.name] = total
                    changed = true
                }
            }
            if (changed) state.bestiary = merged
        }
        // Rétro-remplit la première rencontre des sauvegardes existantes : on
        // reprend le premier pool et son palier d'ouverture, faute d'historique.
        if (state.bestiary.isNotEmpty()) {
            val seen = state.firstSeen.toMutableMap()
            state.bestiary.forEach { (name, kills) ->
                if (kills <= 0 || seen.containsKey(name)) return@forEach
                val entry = BIOME_POOLS.entries.firstOrNull { (_, pool) ->
                    (pool.easy + pool.mid + pool.hard).any { it.name == name }
                } ?: return@forEach
                val pool = entry.value
                val tier = when {
                    pool.easy.any { it.name == name } -> "easy"
                    pool.mid.any { it.name == name } -> "mid"
                    else -> "hard"
                }
                val floor = BIOMES.firstOrNull { it.key == entry.key }?.fromFloor ?: 1
                val level = (floor + when (tier) {
                    "easy" -> -1
                    "mid" -> 0
                    else -> 1
                }).coerceAtLeast(1)
                seen[name] = FirstSeen(entry.key, floor, tier, level)
            }
            if (seen.size != state.firstSeen.size) state.firstSeen = seen
        }
        if (state.bossKills.isNotEmpty()) {
            val seen = state.bossFirstSeen.toMutableMap()
            state.bossKills.forEach { (name, kills) ->
                if (kills <= 0 || seen.containsKey(name)) return@forEach
                val special = specialBossFloor(name)
                val entry = BIOME_POOLS.entries.firstOrNull { (_, pool) ->
                    pool.bosses.any { it.name == name }
                }
                val floor = special
                    ?: entry?.let { (key, _) -> BIOMES.firstOrNull { it.key == key }?.fromFloor }
                    ?: return@forEach
                val biomeKey = entry?.key ?: biomeKeyFor(floor)
                seen[name] = FirstSeen(biomeKey, floor, "boss", floor + 2)
            }
            if (seen.size != state.bossFirstSeen.size) state.bossFirstSeen = seen
        }
        // Un combat sauvegardé avant l'ajout du tier reste lisible : on le
        // déduit de la distance, ou du mode farm.
        state.fight?.let { fight ->
            if (fight.farm) {
                fight.tier = "farm"
            } else if (fight.tier == "easy" && state.distance >= 50) {
                fight.tier = if (state.distance >= 85) "hard" else "mid"
            }
        }
        // v5 → v6 : les anciens noms partagés deviennent des variantes uniques.
        // v6 → v7 : les noms affichés deviennent des identifiants stables.
        // Les deux migrations ne s'exécutent qu'une fois ; le nettoyage des
        // biomes non découverts reste actif à chaque chargement.
        if (state.version < 6) migrateMobRenames(state)
        if (state.version < 7) migrateMobIds(state)
        cleanupUndiscoveredMobs(state)
        if (state.version < SAVE_VERSION) state.version = SAVE_VERSION
        // Collection permanente : recale les pièces déjà possédées (anciennes
        // sauvegardes, import, pièces acquises avant la v0.17.2).
        seedSetCollections(state)
        return state
    }

    /**
     * Copie les compteurs des anciens noms partagés vers leurs variantes
     * uniques. Les kills de gardien sont retirés du compteur de monstre
     * (Option B), et seuls les biomes déjà découverts reçoivent une copie.
     * La première rencontre n'est conservée que sur la variante qui correspond
     * au biome et au tier d'origine.
     */
    private fun migrateMobRenames(state: GameState) {
        if (MOB_RENAMES.isEmpty()) return
        val oldBestiary = state.bestiary
        val oldFirstSeen = state.firstSeen
        val bestiary = oldBestiary.toMutableMap()
        val firstSeen = oldFirstSeen.toMutableMap()

        MOB_RENAMES.forEach { (oldName, targets) ->
            val rawKills = oldBestiary[oldName] ?: 0
            val bossKillsForName = state.bossKills[oldName] ?: 0
            val mobKills = (rawKills - bossKillsForName).coerceAtLeast(0)
            val oldSeen = oldFirstSeen[oldName]
            bestiary.remove(oldName)
            firstSeen.remove(oldName)
            targets.forEach targetLoop@ { targetName ->
                val location = mobLocation(targetName) ?: return@targetLoop
                if (state.floor < location.fromFloor) return@targetLoop
                if (mobKills > 0) {
                    bestiary[targetName] = max(bestiary[targetName] ?: 0, mobKills)
                }
                if (oldSeen != null &&
                    oldSeen.biomeKey == location.biomeKey &&
                    oldSeen.tier == location.tier
                ) {
                    firstSeen[targetName] = oldSeen
                }
            }
        }

        state.bestiary = bestiary
        state.firstSeen = firstSeen
    }

    /**
     * v6 → v7 : remplace les clés de bestiaire par les identifiants stables
     * des créatures (nom d'asset sans extension). Les clés inconnues sont
     * conservées telles quelles pour ne perdre aucune donnée ; le Codex les
     * ignore simplement.
     */
    private fun migrateMobIds(state: GameState) {
        val mobs = allMobTemplates()
        val bosses = allBossTemplates()
        val mobNames = mobs.associate { it.name to it.id }
        val bossNames = bosses.associate { it.name to it.id }

        fun convertCounts(
            source: Map<String, Int>,
            names: Map<String, String>
        ): Map<String, Int> {
            val out = mutableMapOf<String, Int>()
            source.forEach { (key, count) ->
                val id = names[key] ?: key
                out[id] = (out[id] ?: 0) + count
            }
            return out
        }

        fun convertSeen(
            source: Map<String, FirstSeen>,
            names: Map<String, String>
        ): Map<String, FirstSeen> {
            val out = mutableMapOf<String, FirstSeen>()
            source.forEach { (key, seen) ->
                val id = names[key] ?: key
                val current = out[id]
                if (current == null || seen.floor < current.floor) out[id] = seen
            }
            return out
        }

        state.bestiary = convertCounts(state.bestiary, mobNames)
        state.bossKills = convertCounts(state.bossKills, bossNames)
        state.firstSeen = convertSeen(state.firstSeen, mobNames)
        state.bossFirstSeen = convertSeen(state.bossFirstSeen, bossNames)
    }

    /**
     * Efface les compteurs et premières rencontres des créatures dont le biome
     * n'a pas encore été atteint. Idempotent : tourne à chaque chargement.
     * Les clés inconnues ne sont pas supprimées.
     */
    private fun cleanupUndiscoveredMobs(state: GameState) {
        val bestiary = state.bestiary.toMutableMap()
        val firstSeen = state.firstSeen.toMutableMap()
        val bossKills = state.bossKills.toMutableMap()
        val bossSeen = state.bossFirstSeen.toMutableMap()

        BIOME_POOLS.forEach { (key, pool) ->
            val fromFloor = BIOMES.firstOrNull { it.key == key }?.fromFloor ?: Int.MAX_VALUE
            val discovered = state.floor >= fromFloor
            if (!discovered) {
                (pool.easy + pool.mid + pool.hard).forEach { mob ->
                    bestiary.remove(mob.id)
                    firstSeen.remove(mob.id)
                }
                // Un gardien de biome ne peut pas être vaincu avant d'avoir
                // atteint ce biome : on efface les compteurs hérités de la
                // migration du bestiaire, qui ne connaissait pas le biome.
                pool.bosses.forEach { boss ->
                    bestiary.remove(boss.id)
                    bossKills.remove(boss.id)
                    bossSeen.remove(boss.id)
                }
            }
        }

        // Les gardiens de jalon ont un palier fixe, parfois avant leur biome
        // d'accueil (Mère des Abysses au 25, L'Âme Première au 100).
        SPECIAL_BOSSES.forEach { boss ->
            val floor = specialBossFloor(boss.name) ?: return@forEach
            if (state.floor < floor) {
                bestiary.remove(boss.id)
                bossKills.remove(boss.id)
                bossSeen.remove(boss.id)
            }
        }

        state.bestiary = bestiary
        state.firstSeen = firstSeen
        state.bossKills = bossKills
        state.bossFirstSeen = bossSeen
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
        // Un retour en arrière (fuseau, horloge) ne doit pas rejouer le reset du
        // jour : sinon les compteurs quotidiens, les serments et le budget hors
        // ligne seraient recrédités une seconde fois.
        val previous = runCatching { LocalDate.parse(state.day) }.getOrNull()
        if (previous != null && date.isBefore(previous)) return
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
        state.farmBossesToday = 0
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
        (base * (1.0 + Balance.GOLD_MILESTONE_BONUS * milestoneBonus(state.floor))).roundToInt()

    fun addLegend(state: GameState, text: String, type: String = "good") {
        state.legend = (listOf(JournalEntry(text, nowTime(), type)) + state.legend).take(120)
    }

    private fun recordKill(state: GameState, id: String) {
        state.kills++
        state.bestiary = state.bestiary + (id to ((state.bestiary[id] ?: 0) + 1))
    }

    private fun recordCombat(state: GameState, f: FightData) {
        if (f.elapsedMs <= 0.0) return
        state.telemetry.combatMs += f.elapsedMs.toLong()
        state.telemetry.combatCount++
    }

    fun simulateOffline(
        state: GameState,
        maxIterations: Int = 900,
        dtMs: Double = 500.0,
        policy: OfflinePolicy = OfflinePolicy.Default
    ): OfflineSim {
        // La synchro de fond partage le budget hors ligne de la reprise.
        val cap = min(maxIterations * dtMs.toLong(), OFFLINE_SIM_MAX_MS)
        val sim = simulateAbsence(state, elapsedMs = cap, capMs = cap, dtMs = dtMs, policy = policy)
        return OfflineSim(
            combats = sim.combats,
            gold = sim.gold,
            floors = sim.floors,
            kills = sim.kills,
            defeats = sim.defeats
        )
    }

    /** Nombre de pas encore autorisés à être consommés hors ligne aujourd'hui. */
    fun offlineBudget(state: GameState, policy: OfflinePolicy = OfflinePolicy.Default): Int =
        policy.budgetFor(state.stepsToday)
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
        dtMs: Double = 500.0,
        policy: OfflinePolicy = OfflinePolicy.Default
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

        val budget = min(offlineBudget(state, policy), startBank)
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
        // La simulation peut s'arrêter sur le plafond de durée au milieu d'un
        // combat. On termine alors ce combat sans consommer de pas : sinon
        // l'interface rouvrirait un combat déjà entamé hors écran (monstre
        // affiché sans attaque, puis repli immédiat).
        var extraIterations = 0
        if (state.mode == "fight" || state.mode == "boss") {
            val extraMax = (FIGHT_RESOLVE_MAX_MS / dtMs.toLong()).toInt().coerceAtLeast(1)
            while (state.fight != null && extraIterations < extraMax) {
                tick(state, dtMs)
                extraIterations++
            }
            // Combat anormalement long : on le clôt proprement en défaite plutôt
            // que de laisser un état intermédiaire à l'écran.
            state.fight?.let { defeat(state, it) }
        }
        if (state.fight == null && (state.mode == "fight" || state.mode == "boss")) {
            state.mode = "walk"
        }
        val consumed = (startBank - state.stepBank).toInt().coerceAtLeast(0)
        state.offlineStepsToday += consumed
        val loot = (state.inventory + state.stash)
            .map { it.name }
            .filterNot { it in namesBefore }
            .distinct()
            .take(12)
        // Les coups simulés ne doivent pas produire de float à la reprise.
        state.recentHits = emptyList()
        return AbsenceSim(
            elapsedMs = elapsedMs,
            simulatedMs = ((iterations + extraIterations) * dtMs).toLong(),
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
            shards = (state.shards - shardsBefore).coerceAtLeast(0),
            defeats = (state.retreats - startRetreats).coerceAtLeast(0)
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
            defeated = a.defeated || b.defeated || (a.defeats + b.defeats) > 0,
            defeats = a.defeats + b.defeats,
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
        normalizeProgress(state)
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
            "gate" -> Unit
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

    /**
     * Répare un état de marche incohérent (sauvegarde ancienne, état de debug) :
     * une distance ou un prochain combat au-delà du palier bloquait le gardien.
     */
    private fun normalizeProgress(state: GameState) {
        if (state.mode != "walk") return
        if (state.nextFight <= 0 || state.nextFight > FLOOR_METERS) {
            state.nextFight = min(
                FLOOR_METERS,
                max(10, ((state.distance / 10.0).toInt() + 1) * 10)
            )
        }
        if (state.distance > FLOOR_METERS) {
            state.distance = FLOOR_METERS.toDouble()
        }
    }

    private fun tickWalk(state: GameState, dtMs: Double) {
        if (state.stepBank <= 0.0) return
        val rate = if (state.stepBank > WALK_SLOW_THRESHOLD) WALK_STEPS_PER_SECOND else WALK_STEPS_PER_SECOND_SLOW
        val used = min(state.stepBank, rate * dtMs / 1000.0)
        state.stepBank -= used
        state.distance += used / STEPS_PER_METER
        if (state.nextFight >= GATE_METERS && state.distance >= GATE_METERS) {
            state.distance = GATE_METERS
            prepareGate(state)
            return
        }
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
                val offer = lootItem(
            state,
            max(1, state.floor),
            if (rnd.nextDouble() < 0.3) "epic" else "rare"
        )
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
                    state.shards = min(MAX_SHARDS, state.shards + 1)
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
                state.rations = min(MAX_RATIONS, state.rations + template.secondary.toInt())
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
                message = "ATQ +${(template.magnitude * 100).roundToInt()} % · " +
                    "dégâts subis +${(template.secondary * 100).roundToInt()} %"
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
     * Recale la base de pas après un changement de source de pas : les totaux
     * annoncés deviennent la référence, sans créditer de pas. Le total affiché
     * ne recule jamais et `processedSteps` empêche tout crédit rétroactif.
     */
    fun rebaseHistory(
        state: GameState,
        days: Map<String, Int>,
        today: LocalDate = LocalDate.now()
    ) {
        ensureDay(state, today)
        val todayKey = today.toString()
        if (state.historyStart.isBlank()) state.historyStart = todayKey
        val startKey = state.historyStart
        days.forEach { (key, raw) ->
            if (key < startKey) return@forEach
            val parsed = runCatching { LocalDate.parse(key) }.getOrNull() ?: return@forEach
            if (parsed.isAfter(today)) return@forEach
            val steps = raw.coerceIn(0, MAX_STEPS_PER_DAY)
            val known = state.stepLedger[key] ?: 0
            state.stepLedger = state.stepLedger + (key to max(known, steps))
            if (key == todayKey) {
                state.stepsToday = max(state.stepsToday, steps)
                state.processedSteps = max(state.processedSteps, state.stepsToday)
            }
        }
        state.lastSync = System.currentTimeMillis()
    }

    /**
     * Imports a day ledger (Garmin or Santé Connect). Each calendar day is only
     * ever counted once, so syncing repeatedly (or importing the same 7 days
     * again) is idempotent. [sourceLabel] only feeds the journal and the status.
     */
    fun ingestHistory(
        state: GameState,
        days: Map<String, Int>,
        today: LocalDate = LocalDate.now(),
        sourceLabel: String = "Garmin"
    ): OfflineReport? {
        ensureDay(state, today)
        val todayKey = today.toString()
        // Première connexion à une source : on ne récupère que la journée en
        // cours, jamais la semaine passée (sinon on démarre avec une réserve
        // énorme). Les jours antérieurs restent exclus par la suite aussi.
        if (state.historyStart.isBlank()) state.historyStart = todayKey
        val startKey = state.historyStart
        var todayAdded = 0
        var todayTotal = state.stepsToday
        var historyAdded = 0
        var recoveredDays = 0

        days.toSortedMap().forEach { (key, raw) ->
            if (key < startKey) return@forEach
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
            val nr = min(MAX_RATIONS, todayTotal / 200)
            val ns = min(12, todayTotal / 500)
            if (nr > state.genRations) {
                state.rations = min(MAX_RATIONS, state.rations + (nr - state.genRations))
                state.genRations = nr
            }
            if (ns > state.genShards) {
                state.shards = min(MAX_SHARDS, state.shards + (ns - state.genShards))
                state.genShards = ns
            }
            state.journal = addJournal(
                state,
                "$accepted pas $sourceLabel - ${"%.0f".format(accepted / STEPS_PER_METER)} m",
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
            state.rations = min(MAX_RATIONS, state.rations + min(12, importedHistory / 800))
            state.shards = min(MAX_SHARDS, state.shards + min(6, importedHistory / 2000))
            state.journal = addJournal(
                state,
                "Rattrapage $sourceLabel · $recoveredDays jour(s) · $importedHistory pas",
                "good"
            )
        }

        state.lastAdded = todayAdded + importedHistory
        state.lastSync = System.currentTimeMillis()
        state.lastSyncSource = sourceLabel
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
            state.shards = min(MAX_SHARDS, state.shards + 1)
            gainXp(state, 80)
            state.journal = addJournal(state, "Serment de marche accompli · +150 or, +1 éclat", "good")
        }
        if (!o.bossesClaimed && state.bossesToday >= o.bossesTarget) {
            o.bossesClaimed = true
            state.gold += 250
            state.goldEarned += 250
            state.shards = min(MAX_SHARDS, state.shards + 2)
            gainXp(state, 120)
            state.journal = addJournal(state, "Serment de chasse accompli · +250 or, +2 éclats", "good")
        }
        if (!o.chestsClaimed && state.chestsToday >= o.chestsTarget) {
            o.chestsClaimed = true
            state.gold += 100
            state.goldEarned += 100
            addLoot(state, lootItem(state, state.floor, "uncommon"))
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
        if (!farm) state.telemetry.normalFights++
    }

    fun startBoss(state: GameState) {
        state.fight = state.pendingBoss ?: makeBoss(state)
        state.pendingBoss = null
        state.mode = "boss"
        state.telemetry.bossFights++
        state.telemetry.bossAttempts = state.telemetry.bossAttempts + (
            state.floor to ((state.telemetry.bossAttempts[state.floor] ?: 0) + 1)
            )
    }

    /** Prépare le portail à 90 m : aperçu du gardien et choix du joueur. */
    fun prepareGate(state: GameState) {
        if (state.mode != "walk") return
        state.distance = GATE_METERS
        state.nextFight = FLOOR_METERS
        state.pendingBoss = makeBoss(state)
        state.mode = "gate"
        state.journal = addJournal(state, "Portail du gardien — ${state.pendingBoss?.enemyName}", "neutral")
    }

    fun engageBoss(state: GameState): Boolean {
        if (state.mode != "gate") return false
        startBoss(state)
        return true
    }

    /** Depuis le portail, verrouille le palier et repart au début pour farmer. */
    fun farmFromGate(state: GameState): Boolean {
        if (state.mode != "gate") return false
        state.holdFloor = true
        state.pendingBoss = null
        state.distance = 0.0
        state.nextFight = 10
        state.mode = "walk"
        state.journal = addJournal(state, "Farm activé depuis le portail", "neutral")
        return true
    }

    private fun startFarmFight(state: GameState) {
        state.fight = makeFarm(state)
        state.mode = "fight"
    }

    private fun depthMultiplier(floor: Int, boss: Boolean = false): Double {
        if (floor <= Balance.DEPTH_START_FLOOR) return 1.0
        val base = if (boss) Balance.BOSS_DEPTH_BASE else Balance.MOB_DEPTH_BASE
        val raw = base.pow((floor - Balance.DEPTH_START_FLOOR).toDouble())
        return min(raw, Balance.DEPTH_CAP)
    }

    /** Transition douce entre l'ancienne courbe (≤ 20) et la courbe cible (≥ 35). */
    private fun targetWeight(floor: Int): Double =
        ((floor - Balance.TARGET_WEIGHT_START) /
            (Balance.TARGET_WEIGHT_FULL - Balance.TARGET_WEIGHT_START)).coerceIn(0.0, 1.0)

    private fun lerp(from: Double, to: Double, t: Double): Double =
        from + (to - from) * t

    private fun mobHpScale(floor: Int): Double {
        val target = max(
            1.0,
            Balance.MOB_HP_TARGET_BASE - Balance.MOB_HP_TARGET_STEP * (floor - Balance.DEPTH_START_FLOOR)
        )
        return lerp(depthMultiplier(floor), target, targetWeight(floor))
    }

    private fun mobAtkScale(floor: Int): Double {
        val target = max(
            1.0,
            Balance.MOB_ATK_TARGET_BASE - Balance.MOB_ATK_TARGET_STEP * (floor - Balance.DEPTH_START_FLOOR)
        )
        return lerp(depthMultiplier(floor), target, targetWeight(floor))
    }

    private fun bossHpScale(floor: Int): Double {
        val target = max(
            1.0,
            Balance.BOSS_HP_TARGET_BASE - Balance.BOSS_HP_TARGET_STEP * (floor - Balance.DEPTH_START_FLOOR)
        )
        return lerp(6.0 * depthMultiplier(floor, boss = true), target, targetWeight(floor))
    }

    private fun bossAtkScale(floor: Int): Double {
        val target = max(
            1.0,
            Balance.BOSS_ATK_TARGET_BASE - Balance.BOSS_ATK_TARGET_STEP * (floor - Balance.DEPTH_START_FLOOR)
        )
        return lerp(1.6 * depthMultiplier(floor, boss = true), target, targetWeight(floor))
    }

    private fun makeMob(state: GameState): FightData {
        val pos = state.distance
        val biomePool = mobPoolFor(state.floor)
        val tier = when {
            pos >= 85 -> "hard"
            pos >= 50 -> "mid"
            else -> "easy"
        }
        val pool = when (tier) {
            "hard" -> biomePool.hard
            "mid" -> biomePool.mid
            else -> biomePool.easy
        }
        val t = pool[rnd.nextInt(pool.size)]
        val elite = rnd.nextDouble() < Balance.ELITE_CHANCE
        val rule = biomeRuleFor(state.floor)
        var level = state.floor + when {
            pos < 30 -> -1
            pos < 70 -> 0
            else -> 1
        }
        if (elite) level += Balance.ELITE_LEVEL_BONUS
        level = max(1, level)
        val hpScale = mobHpScale(state.floor)
        val atkScale = mobAtkScale(state.floor)
        val hp = (Balance.MOB_HP_BASE + Balance.MOB_HP_PER_LEVEL * (level - 1)) *
            (if (elite) Balance.ELITE_HP_MULT else 1.0)
        val atk = (Balance.MOB_ATK_BASE + Balance.MOB_ATK_PER_LEVEL * (level - 1)) *
            (if (elite) Balance.ELITE_ATK_MULT else 1.0)
        val reward = min(Balance.REWARD_MOB_CAP, sqrt(depthMultiplier(state.floor)))
        val gold = ((6 + 3 * level) * (if (elite) 1.6 else 1.0) * reward).roundToInt()
        val xp = mobXpReward(level, state.floor, state.level, elite)
        return FightData(
            enemyName = t.name,
            enemyIcon = t.icon,
            enemyId = t.id,
            enemyLevel = level,
            enemyHp = hp * hpScale * rule.enemyHpMult,
            enemyMaxHp = hp * hpScale * rule.enemyHpMult,
            enemyAtk = atk * atkScale * rule.enemyAtkMult,
            enemyIntervalMs = ((if (elite) Balance.ELITE_INTERVAL_MS else Balance.MOB_INTERVAL_MS) *
                rule.enemyIntervalMult).roundToLong(),
            gold = gold,
            xp = xp,
            elite = elite,
            tier = tier
        )
    }

    /**
     * Budget de menace : les capacités lourdes (frappe, souffle, drain, pression)
     * ne se combinent pas avec RAPIDE ou BERSERK. Le joueur garde une marge de
     * réaction et la gate reste lisible.
     */
    internal fun bossModifierPool(abilityId: String): List<String> {
        val heavy = abilityId in setOf("slam", "breath", "drain", "pressure", "rupture")
        return buildList {
            add("")
            add("")
            add("blinde")
            add("regenerant")
            add("voleur")
            if (!heavy) {
                add("rapide")
                add("berserk")
            }
        }
    }

    /** Intervalle du stuff de référence (30 % crit, ferveur 100). */
    private fun referenceInterval(floor: Int, rule: BiomeRule): Double {
        val vit = (1.8 + 0.02 * floor) * (1.0 + rule.vitPct)
        return max(Balance.Target.HERO_MIN_INTERVAL_MS / 1000.0, 1.0 / (vit * 1.4))
    }

    private fun referenceDps(floor: Int, rule: BiomeRule): Double {
        val atk = (650.0 + 20.0 * floor) * (1.0 + rule.atkPct)
        val crit = min(75.0, 32.0 + rule.crit)
        return atk * (1.0 + crit / 100.0 * 0.6) / referenceInterval(floor, rule)
    }

    private fun referenceEhp(floor: Int, rule: BiomeRule): Double {
        val hp = (1800.0 + 55.0 * floor) * (1.0 + rule.hpPct)
        val dr = min(Balance.Target.DAMAGE_REDUCTION_CAP, 0.06 + rule.damageReduction)
        return hp * 1.35 / (1.0 - dr)
    }

    private fun shieldFactor(abilityId: String): Double =
        if (abilityId == "shield") {
            1.0 - Balance.Target.SHIELD_REDUCTION *
                (Balance.Target.SHIELD_DURATION_MS / Balance.Target.SHIELD_PERIOD_MS)
        } else {
            1.0
        }

    private fun bossAtkDivisor(abilityId: String): Double = when (abilityId) {
        "shield" -> Balance.Target.BOSS_DIV_SHIELD
        "breath" -> Balance.Target.BOSS_DIV_BREATH
        "pressure" -> Balance.Target.BOSS_DIV_PRESSURE
        "rupture" -> Balance.Target.BOSS_DIV_RUPTURE
        "slam" -> Balance.Target.BOSS_DIV_SLAM
        "drain" -> Balance.Target.BOSS_DIV_DRAIN
        "roots" -> Balance.Target.BOSS_DIV_ROOTS
        "acid" -> Balance.Target.BOSS_DIV_ACID
        "regen" -> Balance.Target.BOSS_DIV_REGEN
        else -> Balance.Target.BOSS_DIV_DEFAULT
    }

    private fun makeBoss(state: GameState): FightData {
        val t = bossFor(state.floor)
        val ability = bossAbilityFor(t.name, state.floor)
        val rule = biomeRuleFor(state.floor)
        val level = state.floor + Balance.Target.BOSS_LEVEL_OFFSET
        val finalHpMult =
            if (state.floor >= Balance.Target.ENDGAME_FINAL_FLOOR) Balance.Target.FINAL_BOSS_HP_MULT else 1.0
        val finalAtkMult =
            if (state.floor >= Balance.Target.ENDGAME_FINAL_FLOOR) Balance.Target.FINAL_BOSS_ATK_MULT else 1.0
        val hpMult = rule.enemyHpMult * rule.bossHpMult * finalHpMult
        val atkMult = rule.enemyAtkMult * rule.bossAtkMult * finalAtkMult
        val weight = targetWeight(state.floor)
        val baseHp = Balance.MOB_HP_BASE + Balance.MOB_HP_PER_LEVEL * (level - 1)
        val baseAtk = Balance.MOB_ATK_BASE + Balance.MOB_ATK_PER_LEVEL * (level - 1)
        val targetHpBase = referenceDps(state.floor, rule) *
            Balance.Target.BOSS_TARGET_TTK * shieldFactor(ability.id) / hpMult
        val targetAtkBase = referenceEhp(state.floor, rule) / bossAtkDivisor(ability.id) / atkMult
        var hp = lerp(baseHp * bossHpScale(state.floor), targetHpBase, weight)
        var atk = lerp(baseAtk * bossAtkScale(state.floor), targetAtkBase, weight)
        var interval = Balance.MOB_INTERVAL_MS.toLong()
        val modifier = bossModifierPool(ability.id).random(rnd)
        when (modifier) {
            "rapide" -> interval = Balance.BOSS_MOD_FAST_INTERVAL_MS.toLong()
            "blinde" -> hp *= Balance.BOSS_MOD_BLINDED_HP
            "berserk" -> atk *= Balance.BOSS_MOD_BERSERK_ATK
            "regenerant" -> hp *= Balance.BOSS_MOD_REGEN_HP
            "voleur" -> atk *= Balance.BOSS_MOD_THIEF_ATK
        }
        val suffix = when (modifier) {
            "rapide" -> " · RAPIDE"
            "blinde" -> " · BLINDÉ"
            "berserk" -> " · BERSERK"
            "regenerant" -> " · RÉGÉNÉRANT"
            "voleur" -> " · VOLEUR"
            else -> ""
        }
        val reward = min(Balance.REWARD_BOSS_CAP, sqrt(depthMultiplier(state.floor, boss = true)))
        val pity = state.bossPity.coerceIn(0.0, Balance.Target.PITY_CAP)
        return FightData(
            enemyName = t.name + suffix,
            enemyIcon = t.icon,
            enemyId = t.id,
            enemyLevel = level,
            enemyHp = hp * hpMult * (1.0 - pity),
            enemyMaxHp = hp * hpMult * (1.0 - pity),
            enemyAtk = atk * atkMult * (1.0 - pity),
            enemyIntervalMs = (interval * rule.enemyIntervalMult).roundToLong(),
            gold = (12 * (6 + 3 * level) * reward).roundToInt(),
            xp = bossXpReward(level, state.floor, state.level),
            isBoss = true,
            ability = ability.id,
            abilityLabel = ability.label,
            specialMs = Balance.BOSS_ABILITY_FIRST_MS,
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
        val level = max(1, state.floor - Balance.FARM_LEVEL_OFFSET)
        val hp = (Balance.MOB_HP_BASE + Balance.MOB_HP_PER_LEVEL * (level - 1)) *
            Balance.FARM_HP_MULT * rule.enemyHpMult
        val atk = (Balance.MOB_ATK_BASE + Balance.MOB_ATK_PER_LEVEL * (level - 1)) *
            Balance.FARM_ATK_MULT * rule.enemyAtkMult
        return FightData(
            enemyName = t.name,
            enemyIcon = t.icon,
            enemyId = t.id,
            enemyLevel = level,
            enemyHp = hp,
            enemyMaxHp = hp,
            enemyAtk = atk,
            enemyIntervalMs = (Balance.MOB_INTERVAL_MS * rule.enemyIntervalMult).roundToLong(),
            gold = ((6 + 3 * level) * 0.5).roundToInt(),
            xp = (mobXpReward(level, state.floor, state.level, false) * Balance.FARM_XP_MULT).roundToInt(),
            farm = true,
            tier = "farm"
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

        if (f.isBoss && !f.phase2 &&
            f.enemyHp < f.enemyMaxHp * Balance.BOSS_PHASE_THRESHOLD
        ) {
            f.phase2 = true
            state.journal = addJournal(state, "Le gardien entre en phase 2", "bad")
        }
        if (f.isBoss && f.modifier == "RÉGÉNÉRANT" && f.enemyHp < f.enemyMaxHp) {
            f.enemyHp = min(
                f.enemyMaxHp,
                f.enemyHp + f.enemyMaxHp * Balance.BOSS_REGEN_RATE * dtMs / 1000.0
            )
        }
        if (f.isBoss && f.specialMs <= 0.0) {
            triggerBossAbility(state, f)
            if (state.fight == null) return
            f.specialMs = if (f.ability == "shield") {
                Balance.Target.SHIELD_PERIOD_MS
            } else {
                Balance.Target.BOSS_ABILITY_INTERVAL_MS
            }
        }

        if (f.heroTimerMs <= 0) {
            heroAttack(state, f)
            if (state.fight == null) return
            f.heroTimerMs = heroIntervalMs(state)
        }
        if (f.enemyTimerMs <= 0) {
            enemyAttack(state, f)
            if (state.fight == null) return
            f.enemyTimerMs = f.enemyIntervalMs.toDouble()
        }
    }

    private fun bossEnrage(f: FightData): Double {
        if (!f.isBoss) return 1.0
        val after = Balance.Target.BOSS_ENRAGE_AFTER_MS
        if (f.elapsedMs < after) return 1.0
        val steps = ((f.elapsedMs - after) / Balance.BOSS_ENRAGE_STEP_MS).toInt() + 1
        return 1.0 + min(Balance.Target.BOSS_ENRAGE_CAP, steps * Balance.Target.BOSS_ENRAGE_STEP)
    }

    private fun triggerBossAbility(state: GameState, f: FightData) {
        val s = statsOf(state)
        fun damage(multiplier: Double): Double {
            val raw = f.enemyAtk * multiplier * bossEnrage(f) * (0.9 + rnd.nextDouble() * 0.1)
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
            "shield" -> f.shieldMs = Balance.Target.SHIELD_DURATION_MS
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
            "rupture" -> {
                damage(2.3)
                state.ferveur = max(0, state.ferveur - 20)
                f.enemyHp = min(f.enemyMaxHp, f.enemyHp + f.enemyMaxHp * 0.03)
            }
            else -> damage(1.8)
        }
        f.abilityFlashMs = 1_400.0
        state.journal = addJournal(state, "${f.enemyName} · ${f.abilityLabel}", "alert")
        if (state.hp <= 0) defeat(state, f)
    }

    internal fun heroIntervalMs(state: GameState): Double {
        val s = statsOf(state)
        return (1000.0 / (s.vit * (1.0 + 0.004 * state.ferveur)))
            .coerceAtLeast(Balance.Target.HERO_MIN_INTERVAL_MS)
    }

    /** Conserve les derniers coups pour que l'interface puisse les afficher. */
    private fun recordHit(
        state: GameState,
        damage: Int,
        crit: Boolean,
        onEnemy: Boolean
    ) {
        val seq = state.hitSeq + 1L
        state.hitSeq = seq
        state.recentHits = (state.recentHits + HitEvent(
            seq = seq,
            damage = damage,
            crit = crit,
            onEnemy = onEnemy,
            at = System.currentTimeMillis()
        )).takeLast(4)
    }

    private fun heroAttack(state: GameState, f: FightData) {
        val s = statsOf(state)
        var atk = s.atk
        var dmg = atk * (0.9 + rnd.nextDouble() * 0.2)
        if (f.isBoss) dmg *= 1.0 + s.bossDamage
        val critChance = ((s.crit + state.ferveur * 0.02) / 100.0)
            .coerceAtMost(Balance.Target.CRIT_CAP / 100.0)
        val crit = rnd.nextDouble() < critChance
        f.lastCrit = crit
        if (crit) dmg *= 1.6 + s.critDamage
        if (f.shieldMs > 0.0) dmg *= 1.0 - Balance.Target.SHIELD_REDUCTION
        f.enemyHp -= dmg
        recordHit(state, dmg.roundToInt().coerceAtLeast(1), crit, onEnemy = true)
        if (s.lifesteal > 0.0) {
            state.hp = min(s.maxHp.toDouble(), state.hp + dmg * s.lifesteal)
        }
        if (f.enemyHp <= 0) victory(state, f)
    }

    private fun enemyAttack(state: GameState, f: FightData) {
        val s = statsOf(state)
        var dmg = f.enemyAtk * bossEnrage(f) * (0.9 + rnd.nextDouble() * 0.1)
        dmg *= (1.0 - s.damageReduction) * (1.0 + s.damageTaken)
        state.hp -= dmg
        recordHit(state, dmg.roundToInt().coerceAtLeast(1), crit = false, onEnemy = false)
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
        if (!f.usedRation && !f.farm && state.hp < s.maxHp * Balance.Target.AUTO_RATION_THRESHOLD && state.rations > 0) {
            f.usedRation = true
            state.rations--
            val heal = s.maxHp * Balance.Target.AUTO_RATION_HEAL
            state.hp = min(s.maxHp.toDouble(), state.hp + heal)
            state.journal = addJournal(state, "Ration consommée (+${heal.roundToInt()} PV)", "neutral")
        }
    }

    internal fun victory(state: GameState, f: FightData) {
        recordCombat(state, f)
        val earned = goldGain(state, f.gold)
        state.gold += earned
        state.goldEarned += earned
        gainXp(state, f.xp)
        state.fight = null
        val clearedFloor = state.floor
        val killedId = if (f.isBoss) {
            bossFor(clearedFloor).id
        } else {
            f.enemyId.ifBlank {
                allMobTemplates().firstOrNull { it.name == f.enemyName }?.id
                    ?: f.enemyIcon.substringAfterLast('/').substringBeforeLast('.')
            }
        }
        recordKill(state, killedId)
        val baseLevel = (f.enemyLevel - if (f.elite) 2 else 0).coerceAtLeast(1)
        if (f.isBoss) {
            if (state.bossFirstSeen[killedId] == null) {
                state.bossFirstSeen = state.bossFirstSeen + (
                    killedId to FirstSeen(
                        biomeKey = biomeKeyFor(clearedFloor),
                        floor = clearedFloor,
                        tier = "boss",
                        level = f.enemyLevel
                    )
                    )
            }
        } else if (state.firstSeen[killedId] == null) {
            state.firstSeen = state.firstSeen + (
                killedId to FirstSeen(
                    biomeKey = biomeKeyFor(clearedFloor),
                    floor = clearedFloor,
                    tier = f.tier,
                    level = baseLevel
                )
                )
        }

        if (f.isBoss) {
            state.bosses++
            state.bossesToday++
            state.telemetry.bossWins++
            state.telemetry.bossWinsByFloor = state.telemetry.bossWinsByFloor + (
                clearedFloor to ((state.telemetry.bossWinsByFloor[clearedFloor] ?: 0) + 1)
                )
            val baseBoss = killedId
            state.bossKills = state.bossKills + (
                baseBoss to ((state.bossKills[baseBoss] ?: 0) + 1)
                )
            state.bossBlessing = 0.0
            state.bossPity = 0.0
            state.pendingBoss = null
            state.mode = "walk"
            state.distance = 0.0
            state.nextFight = 10
            if (state.holdFloor) {
                val canReward = state.farmBossesToday < Balance.Target.FARM_BOSS_REWARD_CAP
                if (canReward) {
                    state.farmBossesToday++
                    state.shards = min(MAX_SHARDS, state.shards + Balance.Target.HOLD_FLOOR_SHARDS)
                    state.rations = min(MAX_RATIONS, state.rations + Balance.Target.HOLD_FLOOR_RATIONS)
                }
                grantBossLoot(state, clearedFloor)
                dropConsumables(state, "boss")
                state.journal = addJournal(
                    state,
                    if (canReward) {
                        "${f.enemyName} vaincu — palier $clearedFloor conservé (farm) · " +
                            "+${Balance.Target.HOLD_FLOOR_SHARDS} éclat, " +
                            "+${Balance.Target.HOLD_FLOOR_RATIONS} ration"
                    } else {
                        "${f.enemyName} vaincu — palier $clearedFloor conservé (farm) · " +
                            "récompenses de farm du jour épuisées"
                    },
                    "good"
                )
            } else {
                state.shards = min(MAX_SHARDS, state.shards + Balance.BOSS_SHARDS)
                state.rations = min(MAX_RATIONS, state.rations + Balance.Target.BOSS_RATIONS)
                state.floor++
                grantBossLoot(state, state.floor)
                dropConsumables(state, "boss")
                state.journal = addJournal(state, "${f.enemyName} vaincu — palier ${state.floor}", "good")
                if (clearedFloor == Balance.Target.ENDGAME_FINAL_FLOOR && !state.endingSeen) {
                    state.endingSeen = true
                    state.endingPending = true
                    addLegend(state, "Le Trône est brisé — L'Âme Première est tombée")
                    state.journal = addJournal(
                        state,
                        "Fin du sentier — le Trône est brisé · mode sans fin débloqué",
                        "good"
                    )
                }
                if (clearedFloor % 10 == 0) {
                    state.shards = min(MAX_SHARDS, state.shards + Balance.MILESTONE_SHARDS)
                    addLegend(state, "Jalon · palier $clearedFloor — ${titleFor(state.floor)}")
                    state.journal = addJournal(
                        state,
                        "Jalon · palier $clearedFloor franchi — ${titleFor(state.floor)} · " +
                            "+${Balance.MILESTONE_SHARDS} éclats",
                        "good"
                    )
                }
            }
            checkOaths(state)
        } else if (f.farm) {
            state.farmKills++
            val s = statsOf(state)
            val beforeHeal = state.hp
            state.hp = min(s.maxHp.toDouble(), state.hp + s.maxHp * Balance.Target.FARM_HEAL_RATIO)
            val healed = (state.hp - beforeHeal).roundToInt().coerceAtLeast(0)
            state.journal = addJournal(
                state,
                "Farm · ${f.enemyName} vaincu · +$healed PV",
                "good"
            )
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
            state.telemetry.normalWins++
            state.journal = addJournal(state, "${f.enemyName} vaincu · +$earned or", "good")
            dropConsumables(state, "mob")
            if (rnd.nextDouble() < Balance.Target.MOB_RATION_CHANCE) {
                state.rations = min(MAX_RATIONS, state.rations + 1)
            }
            if (f.elite) {
                state.rations = min(
                    MAX_RATIONS,
                    state.rations + Balance.Target.ELITE_RATION_GUARANTEED
                )
                if (rnd.nextDouble() < Balance.ELITE_SHARD_CHANCE) {
                    state.shards = min(MAX_SHARDS, state.shards + 1)
                }
            }
            if (rnd.nextDouble() < Balance.CHEST_CHANCE) spawnChest(state)
            state.mobsSinceRare++
            if (state.mobsSinceRare >= Balance.Target.LOOT_PITY_RARE_KILLS) {
                state.mobsSinceRare = 0
                addLoot(state, lootItem(state, state.floor, "rare"))
                state.journal = addJournal(state, "Butin garanti — objet rare après 5 victoires", "good")
            }
        }
    }

    private fun defeat(state: GameState, f: FightData) {
        recordCombat(state, f)
        state.fight = null
        state.retreats++
        state.distance = max(0.0, floor((state.distance - 20.0) / 10.0) * 10.0)
        state.nextFight = (state.distance + 10).toInt()
        state.hp = statsOf(state).maxHp * Balance.DEFEAT_HP_RATIO
        state.mode = "farmWait"
        state.farmKills = 0
        state.farmWaitMs = 800.0
        if (f.isBoss) {
            state.bossPity = min(
                Balance.Target.PITY_CAP,
                state.bossPity + Balance.Target.PITY_HP_STEP
            )
            state.pendingBoss = null
        }
        state.journal = addJournal(state, "Défaite — repli de 20 m dans la zone de farm", "bad")
    }

    private fun gainXp(state: GameState, amount: Int) {
        state.xp += amount
        var guard = 0
        while (state.xp >= xpNext(state.level) && guard < 8) {
            state.xp -= xpNext(state.level)
            state.level++
            val s = statsOf(state)
            state.hp = min(s.maxHp.toDouble(), state.hp + s.maxHp * Balance.LEVEL_UP_HEAL)
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
        state.rations = min(MAX_RATIONS, state.rations + Balance.Target.CHEST_RATIONS)
        state.shards = min(MAX_SHARDS, state.shards + 2)
        state.chestsToday++
        state.chestsOpened++
        addLoot(state, lootItem(state, state.floor, c.rarity))
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
        if (level >= Balance.Target.MAX_CAMP_LEVEL) return false
        val cost = upgradeCost(key, level, state.floor)
        if (state.gold < cost) return false
        val oldMax = statsOf(state).maxHp
        state.gold -= cost
        when (key) {
            "blade" -> state.upgrades.blade++
            "breath" -> state.upgrades.breath++
            "swift" -> state.upgrades.swift++
        }
        val newMax = statsOf(state).maxHp
        state.hp = state.hp * newMax / oldMax
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
        val missingShards = (1 - state.shards).coerceAtLeast(0)
        if (missingShards > 0) return ForgeOutcome.Missing(0, missingShards)
        val effect = if (rnd.nextDouble() < 0.25) "" else ITEM_EFFECTS.random(rnd)
        val baseName = item.name.substringBefore(" · ")
        updateItem(state, itemId) {
            it.copy(
                name = if (effect.isEmpty()) baseName else "$baseName · ${effectLabel(effect)}",
                effect = effect
            )
        } ?: return ForgeOutcome.NotFound
        state.shards -= 1
        state.journal = addJournal(
            state,
            "Forge · ${item.name} → ${if (effect.isEmpty()) "aucun effet" else effectLabel(effect)}",
            "good"
        )
        return ForgeOutcome.Success
    }

    fun upgradeCost(item: Item): Int = 30 * item.level + 20

    /**
     * Version niveau +1 d'un objet. Chaque statistique non nulle progresse de
     * [Balance.Target.ITEM_GROWTH] de sa valeur de base, avec un gain minimal de +1.
     * La croissance est additive et plafonnée par [maxUpgradeLevel].
     */
    fun upgradedItem(item: Item): Item {
        val baseAtk = if (item.baseAtk > 0) item.baseAtk else item.atk
        val baseHp = if (item.baseHp > 0) item.baseHp else item.hp
        val baseVit = if (item.baseVitPct > 0) item.baseVitPct else item.vitPct
        val baseCrit = if (item.baseCritPct > 0) item.baseCritPct else item.critPct
        return item.copy(
            level = item.level + 1,
            atk = growStat(item.atk, baseAtk),
            hp = growStat(item.hp, baseHp),
            vitPct = growStat(item.vitPct, baseVit),
            critPct = growStat(item.critPct, baseCrit),
            regen = if (item.regen > 0) ((item.regen + 0.1) * 10).roundToInt() / 10.0 else item.regen,
            value = (item.value * 1.2).roundToInt(),
            maxLevel = maxUpgradeLevel(item),
            baseAtk = baseAtk,
            baseHp = baseHp,
            baseVitPct = baseVit,
            baseCritPct = baseCrit
        )
    }

    private fun growStat(current: Int, base: Int): Int =
        if (current <= 0) current
        else current + max(1, (base * Balance.Target.ITEM_GROWTH).roundToInt())

    fun upgradeItem(state: GameState, itemId: String): Boolean =
        upgradeItemDetailed(state, itemId) == ForgeOutcome.Success

    fun upgradeItemDetailed(state: GameState, itemId: String): ForgeOutcome {
        val item = findItem(state, itemId) ?: return ForgeOutcome.NotFound
        if (!item.identified) return ForgeOutcome.NotIdentified
        if (isMaxLevel(item)) return ForgeOutcome.MaxLevel
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
            "epic" -> 6
            "legendary" -> 10
            else -> 1
        }
        state.inventory = state.inventory.filter { it.id != itemId }
        state.shards = min(MAX_SHARDS, state.shards + shards)
        state.journal = addJournal(state, "${item.name} démonté · +$shards éclat(s)", "neutral")
        return shards
    }

    /** Objets éligibles à la fusion : même emplacement, même rareté, non verrouillés et identifiés. */
    fun fusionGroup(state: GameState, item: Item): List<Item> =
        state.inventory.filter {
            it.slot == item.slot && it.rarity == item.rarity && !it.locked && it.identified
        }

    /** Vrai si le bouton « Fusionner » doit être actif pour cet objet. */
    fun canFuse(state: GameState, item: Item): Boolean =
        !item.locked &&
            item.identified &&
            state.equipment.none { it.id == item.id } &&
            fusionGroup(state, item).size >= 3

    fun fuseItem(state: GameState, itemId: String): Item? {
        val item = state.inventory.firstOrNull { it.id == itemId } ?: return null
        if (!canFuse(state, item)) return null
        // L'objet ouvert fait toujours partie du trio, complété par les 2 meilleurs autres.
        val chosen = listOf(item) + fusionGroup(state, item)
            .asSequence()
            .filter { it.id != item.id }
            .sortedByDescending { itemScore(it) }
            .take(2)
            .toList()
        val next = nextRarity(item.rarity)
        val effect = when {
            next == "legendary" -> ITEM_EFFECTS.random(rnd)
            else -> chosen.firstNotNullOfOrNull { it.effect.ifBlank { null } } ?: ""
        }
        val atk = (chosen.sumOf { it.atk } * Balance.Target.FUSION_RATIO).roundToInt()
        val hp = (chosen.sumOf { it.hp } * Balance.Target.FUSION_RATIO).roundToInt()
        val vit = (chosen.sumOf { it.vitPct } * Balance.Target.FUSION_RATIO).roundToInt()
        val crit = (chosen.sumOf { it.critPct } * Balance.Target.FUSION_RATIO).roundToInt()
        val level = chosen.maxOf { it.level }
        val fused = item.copy(
            id = "fused_${System.nanoTime()}",
            name = "${item.name} fusionné",
            rarity = next,
            level = level,
            atk = atk,
            hp = hp,
            vitPct = vit,
            critPct = crit,
            regen = (chosen.sumOf { it.regen } * Balance.Target.FUSION_RATIO * 10).roundToInt() / 10.0,
            value = chosen.sumOf { it.value } + 120,
            effect = effect,
            locked = false,
            maxLevel = level + Balance.Target.ITEM_MAX_LEVELS,
            baseAtk = atk,
            baseHp = hp,
            baseVitPct = vit,
            baseCritPct = crit
        )
        state.inventory = state.inventory.filterNot { it.id in chosen.map { c -> c.id } }
        addLoot(state, fused)
        state.journal = addJournal(state, "Fusion · ${fused.name} (${rarityLabel(next)})", "good")
        return fused
    }

    fun addLoot(state: GameState, item: Item) {
        // La collection d'ensembles est permanente : on l'enregistre au ramassage,
        // avant de savoir si l'objet finit au sac ou au coffre.
        recordSetPiece(state, item)
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
        if (state.freeRestDay != state.day) {
            state.freeRestDay = state.day
            state.freeRestUses = 0
        }
        if (state.freeRestUses >= Balance.Target.FREE_REST_PER_DAY) return false
        val s = statsOf(state)
        if (state.hp >= s.maxHp) return false
        state.freeRestUses++
        val heal = s.maxHp * Balance.Target.FREE_REST_HEAL
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

    private fun grantBossLoot(state: GameState, floor: Int) {
        val forceSet = state.bossesSinceSet >= Balance.Target.LOOT_PITY_SET_BOSSES
        val item = lootItem(state, floor, "rare", forceSet = forceSet)
        if (item.setName.isNotBlank()) {
            state.bossesSinceSet = 0
        } else {
            state.bossesSinceSet++
        }
        addLoot(state, item)
    }

    private fun lootItem(
        state: GameState,
        floor: Int,
        rarity: String,
        forceSet: Boolean = false
    ): Item {
        var finalRarity = rarity
        if (rarity == "epic" && rnd.nextDouble() < Balance.LOOT_EPIC_LEGENDARY_CHANCE) {
            finalRarity = "legendary"
        }
        if (rarity == "rare" && rnd.nextDouble() < Balance.LOOT_RARE_LEGENDARY_CHANCE) {
            finalRarity = "legendary"
        }
        val templates = lootTemplatesFor(floor)
        val base = templates[rnd.nextInt(templates.size)]
        val mult = rarityMultiplier(finalRarity)
        val floorScale = lootFloorScale(floor)
        val effect = when {
            finalRarity == "legendary" -> ITEM_EFFECTS.random(rnd)
            (finalRarity == "epic" || finalRarity == "rare") &&
                rnd.nextDouble() < Balance.LOOT_EFFECT_CHANCE -> ITEM_EFFECTS.random(rnd)
            else -> ""
        }
        val setPool = BIOME_SETS[biomeKeyFor(floor)] ?: ITEM_SETS
        val setName = when {
            forceSet || (
                finalRarity in setOf("rare", "epic", "legendary") &&
                    rnd.nextDouble() < Balance.LOOT_SET_CHANCE
                ) -> setPool.random(rnd)
            else -> ""
        }
        val rarePlus = finalRarity in setOf("rare", "epic", "legendary")
        val identified = if (!rarePlus) {
            true
        } else {
            val forceIdentified = state.lootSinceUnidentified >= 9
            val dropIdentified = forceIdentified ||
                rnd.nextDouble() >= Balance.LOOT_UNIDENTIFIED_CHANCE
            if (!dropIdentified) {
                state.lootSinceUnidentified = 0
            } else if (forceIdentified) {
                state.lootSinceUnidentified = 0
            } else {
                state.lootSinceUnidentified++
            }
            dropIdentified
        }
        fun scaled(v: Int): Int = max(1, (v * mult * floorScale).roundToInt())
        val atk = scaled(base.atk)
        val hp = scaled(base.hp)
        val vit = scaled(base.vitPct)
        val crit = scaled(base.critPct)
        val level = max(1, floor)
        return base.copy(
            id = "loot_${System.nanoTime()}",
            name = if (effect.isEmpty()) base.name else "${base.name} · ${effectLabel(effect)}",
            level = level,
            rarity = finalRarity,
            atk = atk,
            hp = hp,
            vitPct = vit,
            critPct = crit,
            regen = (base.regen * mult * floorScale * 10).roundToInt() / 10.0,
            value = scaled(base.value),
            effect = effect,
            identified = identified,
            setName = setName,
            maxLevel = level + Balance.Target.ITEM_MAX_LEVELS,
            baseAtk = atk,
            baseHp = hp,
            baseVitPct = vit,
            baseCritPct = crit
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


