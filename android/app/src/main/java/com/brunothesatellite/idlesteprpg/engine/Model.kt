package com.brunothesatellite.idlesteprpg.engine

import kotlinx.serialization.Serializable
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

const val STEPS_PER_METER = 10.0
const val FLOOR_METERS = 100
const val ENCOUNTER_METERS = 10
const val DAY_RESET_HOUR = 0
const val MAX_STEPS_PER_DAY = 60_000
const val HISTORY_DAYS = 7
const val HISTORY_IMPORT_CAP = 100_000
const val CHEST_LIFETIME_MS = 24L * 60L * 60L * 1000L
const val CHEST_WARNING_MS = 60L * 60L * 1000L
const val MAX_PENDING_CHESTS = 2
const val INVENTORY_CAP = 24
const val FERVEUR_GRACE_MS = 2L * 60L * 60L * 1000L
const val FERVEUR_DECAY_STEP_MS = 180_000.0
const val MAX_STEPS_PER_MINUTE = 250
const val SAVE_VERSION = 2

@Serializable
data class Upgrades(
    var blade: Int = 0,
    var breath: Int = 0,
    var swift: Int = 0
)

@Serializable
data class Item(
    val id: String,
    val name: String,
    val slot: String,
    val rarity: String,
    val icon: String,
    val level: Int,
    val atk: Int = 0,
    val hp: Int = 0,
    val vitPct: Int = 0,
    val critPct: Int = 0,
    val regen: Double = 0.0,
    val value: Int = 0,
    val effect: String = "",
    val locked: Boolean = false,
    val identified: Boolean = true,
    val setName: String = ""
)

@Serializable
data class JournalEntry(
    val text: String,
    val time: String,
    val type: String = "neutral"
)

@Serializable
data class FightData(
    var enemyName: String,
    var enemyIcon: String,
    var enemyLevel: Int,
    var enemyHp: Double,
    var enemyMaxHp: Double,
    var enemyAtk: Double,
    var enemyIntervalMs: Long,
    var gold: Int,
    var xp: Int,
    var isBoss: Boolean = false,
    var elite: Boolean = false,
    var farm: Boolean = false,
    var heroTimerMs: Double = 380.0,
    var enemyTimerMs: Double = 900.0,
    var elapsedMs: Double = 0.0,
    var specialMs: Double = 8000.0,
    var usedRation: Boolean = false,
    var modifier: String = "",
    var lastCrit: Boolean = false
)

@Serializable
data class Buff(
    val type: String,
    val magnitude: Double,
    val expiresAt: Long,
    val label: String
)

data class ConsumableTemplate(
    val id: String,
    val name: String,
    val icon: String,
    val rarity: String,
    val description: String,
    val effect: String,
    val magnitude: Double,
    val durationMs: Long = 0L,
    val context: String = "both",
    val value: Int = 0,
    val secondary: Double = 0.0
)

val CONSUMABLES = listOf(
    ConsumableTemplate(
        "potion_soin", "Potion de soin", "item/cons_potion_red.png", "common",
        "Rend 30 % des PV max.", "heal_pct", 0.30, context = "both", value = 30
    ),
    ConsumableTemplate(
        "herbe_lune", "Herbe de lune", "item/cons_herb.png", "common",
        "Rend 12 % des PV et 6 ferveur.", "heal_ferveur", 0.12,
        context = "both", value = 35, secondary = 6.0
    ),
    ConsumableTemplate(
        "champignon_braise", "Champignon de braise", "item/cons_mushroom.png", "uncommon",
        "Rend 20 ferveur.", "ferveur", 20.0, context = "both", value = 40
    ),
    ConsumableTemplate(
        "elixir_veilleur", "Élixir du veilleur", "item/cons_elixir.png", "rare",
        "Restaure tous les PV.", "full_heal", 1.0, context = "both", value = 120
    ),
    ConsumableTemplate(
        "pain_route", "Pain de route", "item/bread_ration.png", "common",
        "Rend 20 % des PV et 1 ration.", "heal_ration", 0.20,
        context = "both", value = 25, secondary = 1.0
    ),
    ConsumableTemplate(
        "fiole_rage", "Fiole de rage", "item/misc_orb.png", "uncommon",
        "ATQ +50 % pendant 20 s.", "buff_atk", 0.50, 20_000,
        context = "combat", value = 60
    ),
    ConsumableTemplate(
        "ecorce_fer", "Écorce de fer", "item/cons_shield.png", "uncommon",
        "Dégâts subis −40 % pendant 20 s.", "buff_def", 0.40, 20_000,
        context = "combat", value = 65
    ),
    ConsumableTemplate(
        "poudre_eclair", "Poudre d'éclair", "item/cons_lightning.png", "uncommon",
        "VIT +60 % pendant 15 s.", "buff_vit", 0.60, 15_000,
        context = "combat", value = 60
    ),
    ConsumableTemplate(
        "bombe_feu", "Bombe de feu", "item/cons_bomb.png", "rare",
        "Inflige immédiatement 2× votre ATQ.", "damage", 2.0,
        context = "combat", value = 90
    ),
    ConsumableTemplate(
        "elixir_vampire", "Élixir de vampire", "item/cons_potion_purple.png", "rare",
        "20 % des dégâts rendus en PV pendant 30 s.", "buff_lifesteal", 0.20, 30_000,
        context = "combat", value = 110
    ),
    ConsumableTemplate(
        "parchemin_critique", "Parchemin critique", "item/cons_scroll.png", "rare",
        "Critique +50 % pendant 15 s.", "buff_crit", 0.50, 15_000,
        context = "combat", value = 95
    ),
    ConsumableTemplate(
        "champignon_berserk", "Champignon berserk", "item/cons_mushroom_red.png", "rare",
        "ATQ +80 %, mais dégâts subis +20 % pendant 15 s.", "berserk", 0.80, 15_000,
        context = "combat", value = 100, secondary = 0.20
    )
)

fun consumableById(id: String): ConsumableTemplate? = CONSUMABLES.firstOrNull { it.id == id }

fun defaultConsumables(): Map<String, Int> = mapOf(
    "potion_soin" to 2,
    "herbe_lune" to 1
)

fun consumableContextLabel(context: String): String = when (context) {
    "combat" -> "COMBAT"
    "camp" -> "CAMP"
    else -> "CAMP & COMBAT"
}

@Serializable
data class OathsState(
    var stepsClaimed: Boolean = false,
    var bossesClaimed: Boolean = false,
    var chestsClaimed: Boolean = false,
    var stepsTarget: Int = 3000,
    var bossesTarget: Int = 1,
    var chestsTarget: Int = 1
)

@Serializable
data class OfflineReport(
    val days: Int,
    val steps: Int,
    val distanceMeters: Int,
    val combats: Int = 0,
    val gold: Int = 0,
    val floors: Int = 0,
    val kills: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val acknowledgedAt: Long = 0L
)

@Serializable
data class SpecialData(
    val type: String,
    val title: String,
    val text: String,
    val acceptLabel: String,
    val refuseLabel: String = "PASSER",
    val goldCost: Int = 0,
    val shardCost: Int = 0,
    val rationCost: Int = 0,
    val offer: Item? = null,
    val rewardText: String = ""
)

@Serializable
data class ChestData(
    var type: String,
    var quota: Int,
    var progress: Double = 0.0,
    var goldMin: Int,
    var goldMax: Int,
    var rarity: String = "uncommon",
    var expiresAt: Long = 0L,
    var expiryNotified: Boolean = false
)

@Serializable
data class GameState(
    var version: Int = SAVE_VERSION,
    var floor: Int = 1,
    var distance: Double = 0.0,
    var nextFight: Int = 10,
    var day: String = "",
    var stepsToday: Int = 0,
    var processedSteps: Int = 0,
    var stepBank: Double = 0.0,
    var gold: Int = 120,
    var rations: Int = 3,
    var shards: Int = 1,
    var genRations: Int = 0,
    var genShards: Int = 0,
    var ferveur: Int = 0,
    var level: Int = 1,
    var xp: Int = 0,
    var hp: Double = 204.0,
    var upgrades: Upgrades = Upgrades(),
    var equipment: List<Item> = defaultEquipment(),
    var inventory: List<Item> = defaultInventory(),
    var journal: List<JournalEntry> = defaultJournal(),
    var mode: String = "walk",
    var fight: FightData? = null,
    var chest: ChestData? = null,
    var farmKills: Int = 0,
    var farmWaitMs: Double = 0.0,
    var bossBlessing: Double = 0.0,
    var bosses: Int = 0,
    var retreats: Int = 0,
    var lastSync: Long = 0L,
    var lastAdded: Int = 0,
    var lastError: String = "",
    var lastSyncSource: String = "",
    var stepLedger: Map<String, Int> = emptyMap(),
    var offlineReport: OfflineReport? = null,
    var bossesToday: Int = 0,
    var chestsToday: Int = 0,
    var oaths: OathsState = OathsState(),
    var chestQueue: List<ChestData> = emptyList(),
    var stash: List<Item> = emptyList(),
    var special: SpecialData? = null,
    var freeRestDay: String = "",
    var lastStepAt: Long = 0L,
    var ferveurDecayMs: Double = 0.0,
    var ferveurDecayAt: Long = 0L,
    var totalSteps: Int = 0,
    var kills: Int = 0,
    var chestsOpened: Int = 0,
    var goldEarned: Int = 0,
    var legend: List<JournalEntry> = emptyList(),
    var bestiary: Map<String, Int> = emptyMap(),
    var weeklySteps: Int = 0,
    var weekKey: String = "",
    var streak: Int = 0,
    var lastActiveDay: String = "",
    var consumables: Map<String, Int> = defaultConsumables(),
    var buffs: List<Buff> = emptyList()
)

data class Stats(
    val atk: Int,
    val maxHp: Int,
    val vit: Double,
    val crit: Int,
    val regen: Double,
    val lifesteal: Double = 0.0,
    val damageReduction: Double = 0.0,
    val critDamage: Double = 0.0,
    val bossDamage: Double = 0.0,
    val damageTaken: Double = 0.0
) {
    val attackSpeed: Double get() = vit
    val power: Int
        get() = (atk * 2 + maxHp / 4.0 + vit * 20 + crit + regen * 10).roundToInt()
}

data class MobTemplate(val name: String, val icon: String)

data class BossTemplate(val name: String, val icon: String)

val EASY_MOBS = listOf(
    MobTemplate("Gobelin", "mon/goblin.png"),
    MobTemplate("Kobold", "mon/kobold.png"),
    MobTemplate("Chauve-souris", "mon/bat.png"),
    MobTemplate("Loup noir", "mon/wolf.png"),
    MobTemplate("Araignée", "mon/spider.png"),
    MobTemplate("Goule", "mon/ghoul.png"),
    MobTemplate("Scorpion", "mon/scorpion.png"),
    MobTemplate("Cafard géant", "mon/giant_cockroach.png"),
    MobTemplate("Grenouille géante", "mon/giant_frog.png"),
    MobTemplate("Ver des tombes", "mon/worm.png")
)

val MID_MOBS = listOf(
    MobTemplate("Gnoll", "mon/gnoll.png"),
    MobTemplate("Orc", "mon/orc.png"),
    MobTemplate("Guerrier squelette", "mon/skeletal_warrior.png"),
    MobTemplate("Hobgobelin", "mon/hobgoblin.png"),
    MobTemplate("Naga", "mon/naga.png"),
    MobTemplate("Diablotin", "mon/crimson_imp.png"),
    MobTemplate("Manticore", "mon/manticore.png"),
    MobTemplate("Élémentaire de feu", "mon/fire_elemental.png"),
    MobTemplate("Démon solaire", "mon/sun_demon.png")
)

val HARD_MOBS = listOf(
    MobTemplate("Guerrier orc", "mon/orc_warrior.png"),
    MobTemplate("Ogre", "mon/ogre.png"),
    MobTemplate("Troll des abîmes", "mon/deep_troll.png"),
    MobTemplate("Ours noir", "mon/black_bear.png"),
    MobTemplate("Griffon", "mon/griffon.png"),
    MobTemplate("Golem de fer", "mon/iron_golem.png"),
    MobTemplate("Vampire", "mon/vampire.png"),
    MobTemplate("Spectre", "mon/ghost.png"),
    MobTemplate("Hydre mineure", "mon/hydra1.png"),
    MobTemplate("Sigmund le reclus", "mon/sigmund.png")
)

val BOSSES = listOf(
    BossTemplate("Hydre de Lerne", "mon/lernaean_hydra01.png"),
    BossTemplate("Dragon de givre", "mon/ice_dragon.png"),
    BossTemplate("Géant de fer", "mon/iron_giant.png"),
    BossTemplate("Jörmungandr", "mon/jormungandr.png"),
    BossTemplate("Balrug", "mon/balrug.png"),
    BossTemplate("Cerebov", "mon/cerebov.png"),
    BossTemplate("Dragon d'orage", "mon/storm_dragon.png"),
    BossTemplate("Liche ancienne", "mon/lich.png"),
    BossTemplate("Sigmund, gardien fou", "mon/sigmund.png"),
    BossTemplate("Démon solaire", "mon/sun_demon.png")
)

val LOOT_TEMPLATES = listOf(
    Item("loot_sword", "Lame runique", "weapon", "rare", "item/long_sword3.png", 1, atk = 12, value = 90),
    Item("loot_axe", "Hache de guerre", "weapon", "uncommon", "item/hand_axe2.png", 1, atk = 10, critPct = 2, value = 65),
    Item("loot_mace", "Masse d'armes", "weapon", "rare", "item/mace2.png", 1, atk = 9, critPct = 3, value = 75),
    Item("loot_spear", "Lance du guet", "weapon", "common", "item/spear2.png", 1, atk = 8, value = 35),
    Item("loot_mail", "Cotte de mailles", "body", "uncommon", "item/scale_mail1.png", 1, hp = 34, vitPct = 2, value = 70),
    Item("loot_plate", "Plate du gardien", "body", "rare", "item/plate1.png", 1, hp = 52, value = 110),
    Item("loot_helm", "Heaume gravé", "head", "rare", "item/helmet3.png", 1, hp = 22, critPct = 3, value = 80),
    Item("loot_glove", "Gants de braise", "hands", "uncommon", "item/glove1.png", 1, atk = 6, value = 55),
    Item("loot_boots", "Bottes de mousse", "feet", "uncommon", "item/boots4_green.png", 1, vitPct = 5, value = 55),
    Item("loot_ring", "Anneau d'âme", "talisman", "epic", "item/ring-regen.png", 1, regen = 0.4, critPct = 2, value = 140)
)

fun defaultEquipment(): List<Item> = listOf(
    Item("eq_weapon", "Lame du veilleur", "weapon", "rare", "item/long_sword3.png", 4, atk = 9, critPct = 3, value = 64),
    Item("eq_head", "Heaume de fer", "head", "uncommon", "item/helmet3.png", 3, hp = 18, value = 40),
    Item("eq_body", "Cotte de mailles", "body", "rare", "item/scale_mail1.png", 4, hp = 36, vitPct = 4, value = 88),
    Item("eq_hands", "Gantelets de braise", "hands", "uncommon", "item/glove1.png", 3, atk = 5, value = 42),
    Item("eq_feet", "Bottes de mousse", "feet", "uncommon", "item/boots4_green.png", 3, vitPct = 4, value = 40),
    Item("eq_talisman", "Anneau de régénération", "talisman", "rare", "item/ring-regen.png", 4, regen = 0.6, critPct = 2, value = 76)
)

fun defaultInventory(): List<Item> = listOf(
    Item("inv1", "Épée ancienne", "weapon", "epic", "item/greatsword1.png", 5, atk = 24, critPct = 5, value = 140),
    Item("inv2", "Masse runique", "weapon", "rare", "item/mace2.png", 4, atk = 16, critPct = 2, value = 84),
    Item("inv3", "Plate du gardien", "body", "rare", "item/plate1.png", 4, hp = 60, vitPct = 2, value = 96),
    Item("inv4", "Heaume elfique", "head", "rare", "item/elven_leather_helm.png", 4, hp = 24, critPct = 4, value = 80),
    Item("inv5", "Amulette d'âme", "talisman", "epic", "item/amulet-regen.png", 5, regen = 0.4, critPct = 2, value = 160),
    Item("inv6", "Hache de guerre", "weapon", "uncommon", "item/hand_axe2.png", 3, atk = 12, value = 52)
)

fun defaultJournal(): List<JournalEntry> = listOf(
    JournalEntry("Le sentier attend vos pas Garmin", nowTime(), "neutral"),
    JournalEntry("Palier 1 — les cryptes s'ouvrent", nowTime(), "good")
)

fun nowTime(): String {
    val d = java.time.LocalDateTime.now()
    return "%02d:%02d".format(d.hour, d.minute)
}

fun slotLabel(slot: String): String = when (slot) {
    "weapon" -> "Arme"
    "head" -> "Tête"
    "body" -> "Torse"
    "hands" -> "Mains"
    "feet" -> "Bottes"
    "talisman" -> "Talisman"
    else -> "Objet"
}

fun rarityLabel(r: String): String = when (r) {
    "uncommon" -> "INHABITUEL"
    "rare" -> "RARE"
    "epic" -> "ÉPIQUE"
    "legendary" -> "LÉGENDAIRE"
    else -> "COMMUN"
}

fun nextRarity(r: String): String = when (r) {
    "common" -> "uncommon"
    "uncommon" -> "rare"
    "rare" -> "epic"
    else -> "legendary"
}

val ITEM_EFFECTS = listOf("vampirisme", "rempart", "frenesie", "chasse")

val ITEM_SETS = listOf("veilleur", "braise", "givre")

fun setLabel(name: String): String = when (name) {
    "veilleur" -> "SERVICE DU VEILLEUR"
    "braise" -> "BRAISE ANCIENNE"
    "givre" -> "SERMENT DE GIVRE"
    else -> ""
}

fun setBonusDescription(name: String, pieces: Int): String {
    if (pieces < 2) return ""
    return when (name) {
        "veilleur" -> if (pieces >= 6) "+30 ATQ, +90 PV" else if (pieces >= 4) "+20 ATQ, +60 PV" else "+10 ATQ, +30 PV"
        "braise" -> if (pieces >= 6) "+9 % crit., +15 % vol de vie" else if (pieces >= 4) "+6 % crit., +10 % vol de vie" else "+3 % crit., +5 % vol de vie"
        "givre" -> if (pieces >= 6) "+15 % VIT, +0,9 PV/s" else if (pieces >= 4) "+10 % VIT, +0,6 PV/s" else "+5 % VIT, +0,3 PV/s"
        else -> ""
    }
}

fun titleFor(floor: Int): String = when {
    floor >= 100 -> "Légende des Âmes"
    floor >= 50 -> "Souverain des Cimes"
    floor >= 40 -> "Gardien de la Forge"
    floor >= 30 -> "Marcheur de Cendre"
    floor >= 20 -> "Pèlerin des Abysses"
    floor >= 10 -> "Veilleur des Cryptes"
    else -> "Pèlerin"
}

fun effectLabel(effect: String): String = when (effect) {
    "vampirisme" -> "VAMPIRISME"
    "rempart" -> "REMPART"
    "frenesie" -> "FRÉNÉSIE"
    "chasse" -> "CHASSE"
    else -> ""
}

fun effectDescription(effect: String): String = when (effect) {
    "vampirisme" -> "+8 % des dégâts infligés rendus en PV"
    "rempart" -> "−10 % de dégâts subis"
    "frenesie" -> "+40 % de dégâts critiques"
    "chasse" -> "+25 % de dégâts contre les gardiens"
    else -> ""
}

fun itemScore(item: Item): Int {
    val effectBonus = when (item.effect) {
        "vampirisme" -> 30
        "rempart" -> 38
        "frenesie" -> 30
        "chasse" -> 26
        else -> 0
    }
    return (item.atk * 2 + item.hp * 0.25 + item.vitPct * 3 +
        item.critPct * 4 + item.regen * 20 + effectBonus).roundToInt()
}

fun rarityRank(rarity: String): Int = when (rarity) {
    "legendary" -> 5
    "epic" -> 4
    "rare" -> 3
    "uncommon" -> 2
    else -> 1
}

fun comparisonScore(item: Item): Int =
    if (item.identified) itemScore(item) else rarityRank(item.rarity) * 20 + item.level * 2

fun itemDisplayName(item: Item): String =
    if (item.identified) item.name else "Objet mystérieux"

fun oathsFor(date: LocalDate): OathsState {
    val seed = date.dayOfYear
    val steps = listOf(2_500, 3_000, 3_500, 4_000)[seed % 4]
    return OathsState(
        stepsTarget = steps,
        bossesTarget = if (seed % 3 == 0) 2 else 1,
        chestsTarget = if (seed % 4 == 0) 2 else 1
    )
}

fun rankLabel(floor: Int): String = when {
    floor >= 50 -> "VI"
    floor >= 40 -> "V"
    floor >= 30 -> "IV"
    floor >= 20 -> "III"
    floor >= 10 -> "II"
    else -> "I"
}

fun milestoneBonus(floor: Int): Int = floor / 10

fun equipped(state: GameState, slot: String): Item? = state.equipment.firstOrNull { it.slot == slot }

fun statsOf(state: GameState): Stats {
    val milestone = milestoneBonus(state.floor)
    var atk = 18 + 3 * (state.level - 1) + 3 * state.upgrades.blade + 2 * milestone
    var maxHp = 150 + 18 * (state.level - 1) + 30 * state.upgrades.breath + 12 * milestone
    var vit = 1.0 + 0.01 * (state.level - 1) + 0.025 * state.upgrades.swift
    var crit = 10
    var regen = 0.6
    var lifesteal = 0.0
    var damageReduction = 0.0
    var critDamage = 0.0
    var bossDamage = 0.0
    state.equipment.forEach {
        atk += it.atk
        maxHp += it.hp
        vit += it.vitPct / 100.0
        crit += it.critPct
        regen += it.regen
        when (it.effect) {
            "vampirisme" -> lifesteal += 0.08
            "rempart" -> damageReduction += 0.10
            "frenesie" -> critDamage += 0.40
            "chasse" -> bossDamage += 0.25
        }
    }
    state.equipment
        .filter { it.setName.isNotBlank() }
        .groupingBy { it.setName }
        .eachCount()
        .forEach { (setName, count) ->
            if (count < 2) return@forEach
            val tier = when {
                count >= 6 -> 3
                count >= 4 -> 2
                else -> 1
            }
            when (setName) {
                "veilleur" -> {
                    atk += 10 * tier
                    maxHp += 30 * tier
                }
                "braise" -> {
                    crit += 3 * tier
                    lifesteal += 0.05 * tier
                }
                "givre" -> {
                    vit += 0.05 * tier
                    regen += 0.3 * tier
                }
            }
        }
    val now = System.currentTimeMillis()
    var damageTaken = 0.0
    state.buffs.filter { it.expiresAt > now }.forEach { buff ->
        when (buff.type) {
            "atk" -> atk = (atk * (1.0 + buff.magnitude)).roundToInt()
            "vit" -> vit *= 1.0 + buff.magnitude
            "crit" -> crit += (buff.magnitude * 100).roundToInt()
            "def" -> damageReduction += buff.magnitude
            "lifesteal" -> lifesteal += buff.magnitude
            "vulnerable" -> damageTaken += buff.magnitude
        }
    }
    return Stats(
        atk = atk,
        maxHp = maxHp,
        vit = vit,
        crit = crit,
        regen = regen,
        lifesteal = lifesteal,
        damageReduction = damageReduction.coerceIn(0.0, 0.7),
        critDamage = critDamage,
        bossDamage = bossDamage,
        damageTaken = damageTaken
    )
}

fun xpNext(level: Int): Int = (120 * 1.2.pow(level - 1)).roundToInt()

fun powerOf(state: GameState): Int = statsOf(state).power

fun tierName(ferveur: Int): String = when {
    ferveur >= 100 -> "Âme ardente"
    ferveur >= 75 -> "Tempête"
    ferveur >= 50 -> "Fournaise"
    ferveur >= 25 -> "Braise"
    else -> "Tison"
}

fun tierIndex(ferveur: Int): Int = when {
    ferveur >= 100 -> 4
    ferveur >= 75 -> 3
    ferveur >= 50 -> 2
    ferveur >= 25 -> 1
    else -> 0
}

fun biomeFor(floor: Int): String = when {
    floor <= 10 -> "CRYPTES DE VAEL"
    floor <= 20 -> "CATACOMBES"
    floor <= 30 -> "CAVERNES DE CENDRE"
    floor <= 40 -> "FORGE NOYÉE"
    else -> "CIMES DU SERMENT"
}

fun bossFor(floor: Int): BossTemplate = when {
    floor % 100 == 0 -> BossTemplate("L'Âme Première", "mon/cerebov.png")
    floor % 50 == 0 -> BossTemplate("Seigneur de la Forge", "mon/iron_giant.png")
    floor % 25 == 0 -> BossTemplate("Mère des Abysses", "mon/jormungandr.png")
    floor % 10 == 0 -> BossTemplate("Roi des Cryptes", "mon/lich.png")
    else -> BOSSES[((floor - 1) % BOSSES.size)]
}

fun upgradeCost(key: String, currentLevel: Int): Int {
    val (base, mult) = when (key) {
        "blade" -> 45.0 to 1.20
        "breath" -> 40.0 to 1.18
        "swift" -> 70.0 to 1.24
        else -> 45.0 to 1.20
    }
    return (Math.ceil(base * mult.pow(currentLevel.toDouble()) / 5.0) * 5).toInt()
}

fun upgradeLevel(state: GameState, key: String): Int = when (key) {
    "blade" -> state.upgrades.blade
    "breath" -> state.upgrades.breath
    "swift" -> state.upgrades.swift
    else -> 0
}

fun upgradeName(key: String): String = when (key) {
    "blade" -> "Lame affûtée"
    "breath" -> "Souffle ancien"
    "swift" -> "Pas vifs"
    else -> "Amélioration"
}

fun upgradeDesc(key: String): String = when (key) {
    "blade" -> "+3 ATQ par niveau"
    "breath" -> "+30 PV max par niveau"
    "swift" -> "+2,5 % VIT par niveau"
    else -> ""
}

fun upgradeIcon(key: String): String = when (key) {
    "blade" -> "item/long_sword3.png"
    "breath" -> "item/i-heal-wounds.png"
    "swift" -> "item/boots4_green.png"
    else -> "item/long_sword3.png"
}

fun statLine(item: Item): String {
    val parts = mutableListOf<String>()
    if (item.atk != 0) parts += "+${item.atk} ATQ"
    if (item.hp != 0) parts += "+${item.hp} PV"
    if (item.vitPct != 0) parts += "+${item.vitPct} % VIT"
    if (item.critPct != 0) parts += "+${item.critPct} % crit."
    if (item.regen != 0.0) parts += "+${"%.1f".format(item.regen)} PV/s"
    return parts.joinToString(" · ")
}

fun todayString(): String = LocalDate.now().toString()

fun currentStepDelta(state: GameState, total: Int): Int = max(0, total - state.processedSteps)

fun clampFerveur(steps: Int): Int = min(100, steps / 50)

fun chestRemainingMs(chest: ChestData, now: Long = System.currentTimeMillis()): Long =
    if (chest.expiresAt <= 0L) Long.MAX_VALUE else (chest.expiresAt - now).coerceAtLeast(0L)

fun formatRemaining(ms: Long): String {
    if (ms == Long.MAX_VALUE) return "—"
    if (ms <= 0L) return "expiré"
    val totalMinutes = ms / 60_000L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return if (hours > 0) "${hours} h ${minutes.toString().padStart(2, '0')}" else "${minutes} min"
}

fun GameState.deepCopy(): GameState = copy(
    upgrades = upgrades.copy(),
    equipment = equipment.toList(),
    inventory = inventory.toList(),
    journal = journal.toList(),
    fight = fight?.copy(),
    chest = chest?.copy(),
    oaths = oaths.copy(),
    stepLedger = stepLedger.toMap(),
    offlineReport = offlineReport?.copy(),
    chestQueue = chestQueue.map { it.copy() },
    stash = stash.toList(),
    legend = legend.toList(),
    bestiary = bestiary.toMap(),
    consumables = consumables.toMap(),
    buffs = buffs.toList(),
    special = special?.let { it.copy(offer = it.offer?.copy()) }
)
