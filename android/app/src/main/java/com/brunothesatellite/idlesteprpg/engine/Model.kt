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
    val value: Int = 0
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
    var usedRation: Boolean = false
)

@Serializable
data class ChestData(
    var type: String,
    var quota: Int,
    var progress: Double = 0.0,
    var goldMin: Int,
    var goldMax: Int,
    var rarity: String = "uncommon"
)

@Serializable
data class GameState(
    var version: Int = 1,
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
    var lastSyncSource: String = ""
)

data class Stats(
    val atk: Int,
    val maxHp: Int,
    val vit: Double,
    val crit: Int,
    val regen: Double
) {
    val attackSpeed: Double get() = vit
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
    MobTemplate("Scorpion", "mon/scorpion.png")
)

val MID_MOBS = listOf(
    MobTemplate("Gnoll", "mon/gnoll.png"),
    MobTemplate("Orc", "mon/orc.png"),
    MobTemplate("Guerrier squelette", "mon/skeletal_warrior.png"),
    MobTemplate("Hobgobelin", "mon/hobgoblin.png"),
    MobTemplate("Naga", "mon/naga.png"),
    MobTemplate("Diablotin", "mon/crimson_imp.png"),
    MobTemplate("Manticore", "mon/manticore.png")
)

val HARD_MOBS = listOf(
    MobTemplate("Guerrier orc", "mon/orc_warrior.png"),
    MobTemplate("Ogre", "mon/ogre.png"),
    MobTemplate("Troll des abîmes", "mon/deep_troll.png"),
    MobTemplate("Ours noir", "mon/black_bear.png"),
    MobTemplate("Griffon", "mon/griffon.png"),
    MobTemplate("Golem de fer", "mon/iron_golem.png"),
    MobTemplate("Vampire", "mon/vampire.png"),
    MobTemplate("Spectre", "mon/ghost.png")
)

val BOSSES = listOf(
    BossTemplate("Hydre de Lerne", "mon/lernaean_hydra01.png"),
    BossTemplate("Dragon de givre", "mon/ice_dragon.png"),
    BossTemplate("Géant de fer", "mon/iron_giant.png"),
    BossTemplate("Jörmungandr", "mon/jormungandr.png"),
    BossTemplate("Balrug", "mon/balrug.png"),
    BossTemplate("Cerebov", "mon/cerebov.png"),
    BossTemplate("Dragon d'orage", "mon/storm_dragon.png"),
    BossTemplate("Liche ancienne", "mon/lich.png")
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

fun equipped(state: GameState, slot: String): Item? = state.equipment.firstOrNull { it.slot == slot }

fun statsOf(state: GameState): Stats {
    var atk = 18 + 3 * (state.level - 1) + 3 * state.upgrades.blade
    var maxHp = 150 + 18 * (state.level - 1) + 30 * state.upgrades.breath
    var vit = 1.0 + 0.01 * (state.level - 1) + 0.025 * state.upgrades.swift
    var crit = 10
    var regen = 0.6
    state.equipment.forEach {
        atk += it.atk
        maxHp += it.hp
        vit += it.vitPct / 100.0
        crit += it.critPct
        regen += it.regen
    }
    return Stats(atk, maxHp, vit, crit, regen)
}

fun xpNext(level: Int): Int = (120 * 1.2.pow(level - 1)).roundToInt()

fun powerOf(state: GameState): Int {
    val s = statsOf(state)
    return (s.atk * 2 + s.maxHp / 4.0 + s.vit * 20 + s.crit + s.regen * 10).roundToInt()
}

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

fun bossFor(floor: Int): BossTemplate = BOSSES[((floor - 1) % BOSSES.size)]

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

fun GameState.deepCopy(): GameState = copy(
    upgrades = upgrades.copy(),
    equipment = equipment.toList(),
    inventory = inventory.toList(),
    journal = journal.toList(),
    fight = fight?.copy(),
    chest = chest?.copy()
)
