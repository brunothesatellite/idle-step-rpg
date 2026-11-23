package com.brunothesatellite.idlesteprpg.engine

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

const val STEPS_PER_METER = 10.0
const val FLOOR_METERS = 100
const val ENCOUNTER_METERS = 10

/** Distance à laquelle le portail du gardien apparaît. */
const val GATE_METERS = 85.0

/** Marche : pas consommés par seconde, et seuil de passage au rythme lent. */
const val WALK_STEPS_PER_SECOND = 60.0
const val WALK_STEPS_PER_SECOND_SLOW = 30.0
const val WALK_SLOW_THRESHOLD = 600.0

/**
 * Progression hors ligne : part des pas du jour qui peut être consommée par les
 * simulations (reprise + synchro de fond), et durée maximale simulée d'un coup.
 */
const val OFFLINE_BANK_SHARE = 0.30
const val OFFLINE_SIM_MAX_MS = 4L * 60L * 1000L

/**
 * Prolongation maximale d'une simulation qui se termine au milieu d'un combat :
 * on résout ce combat (sans consommer de pas) pour que l'interface n'affiche
 * jamais un combat dont les ticks ont déjà eu lieu hors écran.
 */
const val FIGHT_RESOLVE_MAX_MS = 2L * 60L * 1000L

/** Bornes des réglages avancés d'absence. */
const val OFFLINE_DEFAULT_PERCENT = 30
const val OFFLINE_MAX_PERCENT = 30
const val OFFLINE_DEFAULT_STEPS = 1_000
const val OFFLINE_MAX_STEPS = 5_000

enum class OfflineMode { PERCENT, STEPS }

/**
 * Budget de pas consommable hors ligne, réglé dans les réglages avancés.
 * Le budget reste journalier : [GameEngine.offlineBudget] retire ce qui a déjà
 * été consommé aujourd'hui.
 */
data class OfflinePolicy(
    val mode: OfflineMode = OfflineMode.PERCENT,
    val percent: Int = OFFLINE_DEFAULT_PERCENT,
    val maxSteps: Int = OFFLINE_DEFAULT_STEPS
) {
    /** Budget du jour, borné par les pas réellement comptés aujourd'hui. */
    fun budgetFor(stepsToday: Int): Int {
        val raw = when (mode) {
            OfflineMode.PERCENT ->
                stepsToday * percent.coerceIn(0, OFFLINE_MAX_PERCENT) / 100
            OfflineMode.STEPS -> maxSteps.coerceIn(0, OFFLINE_MAX_STEPS)
        }
        return minOf(raw, stepsToday).coerceAtLeast(0)
    }

    companion object {
        val Default = OfflinePolicy()
    }
}
const val DAY_RESET_HOUR = 0
const val MAX_STEPS_PER_DAY = 60_000
const val HISTORY_DAYS = 7
const val HISTORY_IMPORT_CAP = 100_000
const val CHEST_LIFETIME_MS = 24L * 60L * 60L * 1000L
// Le worker de maintenance passe toutes les 2 h : un préavis de 3 h garantit
// qu'au moins un passage tombe dans la fenêtre avant expiration.
const val CHEST_WARNING_MS = 3L * 60L * 60L * 1000L
const val MAX_PENDING_CHESTS = 2
const val INVENTORY_CAP = 24
const val MAX_SHARDS = 24
const val FERVEUR_GRACE_MS = 2L * 60L * 60L * 1000L
const val FERVEUR_DECAY_STEP_MS = 180_000.0
const val MAX_STEPS_PER_MINUTE = 250
const val SAVE_VERSION = 7

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
    var lastCrit: Boolean = false,
    var ability: String = "",
    var abilityLabel: String = "",
    var abilityFlashMs: Double = 0.0,
    var shieldMs: Double = 0.0,
    /** Pool d'apparition : easy, mid, hard ou farm. */
    var tier: String = "easy",
    /** Identifiant stable de la créature, indépendant du nom affiché. */
    var enemyId: String = ""
)

/** Première victoire sur une créature ou un gardien, pour la fiche du Codex. */
@Serializable
data class FirstSeen(
    val biomeKey: String,
    val floor: Int,
    val tier: String,
    val level: Int
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
    ),
    ConsumableTemplate(
        "potion_hate", "Potion de hâte", "item/i-haste.png", "rare",
        "VIT +60 % pendant 20 s.", "buff_vit", 0.60, 20_000,
        context = "combat", value = 75
    ),
    ConsumableTemplate(
        "potion_force", "Potion de force", "item/i-might.png", "uncommon",
        "ATQ +40 % pendant 20 s.", "buff_atk", 0.40, 20_000,
        context = "combat", value = 65
    ),
    ConsumableTemplate(
        "potion_regen", "Potion de régénération", "item/i-regeneration.png", "rare",
        "Régénération +2 PV/s pendant 30 s.", "buff_regen", 2.0, 30_000,
        context = "combat", value = 90
    ),
    ConsumableTemplate(
        "viande_sechee", "Viande séchée", "item/beef_jerky.png", "common",
        "Rend 25 % des PV et 1 ration.", "heal_ration", 0.25,
        context = "both", value = 30, secondary = 1.0
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
    val minutes: Int = 0,
    val stepsConsumed: Int = 0,
    val remainingSteps: Int = 0,
    val bosses: Int = 0,
    val levels: Int = 0,
    val chests: Int = 0,
    val reason: String = "",
    val special: Boolean = false,
    val defeated: Boolean = false,
    /** Nombre de défaites subies pendant la simulation (recul de 20 m chacune). */
    val defeats: Int = 0,
    /** Butin récupéré pendant la simulation : noms d'objets, consommables, rations, éclats. */
    val lootItems: List<String> = emptyList(),
    val consumables: Int = 0,
    val rations: Int = 0,
    val shards: Int = 0,
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

/** Dernier coup porté, pour l'affichage (événement transitoire). */
@Serializable
data class HitEvent(
    val seq: Long,
    val damage: Int,
    val crit: Boolean,
    val onEnemy: Boolean,
    val at: Long
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
    /** Pas déjà consommés par les simulations hors ligne aujourd'hui (plafond 30 %). */
    var offlineStepsToday: Int = 0,
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
    /** Or du dernier coffre ouvert, pour le toast (non sérialisé). */
    @Transient var chestFlashGold: Int = 0,
    var farmKills: Int = 0,
    var farmWaitMs: Double = 0.0,
    var bossBlessing: Double = 0.0,
    var bosses: Int = 0,
    var retreats: Int = 0,
    var lastSync: Long = 0L,
    var lastAdded: Int = 0,
    var lastError: String = "",
    var lastSyncSource: String = "",
    /**
     * Date de la première synchronisation de pas (AAAA-MM-JJ). Le rattrapage
     * d'historique ne remonte jamais avant cette date : la première connexion
     * ne crédite que la journée en cours.
     */
    var historyStart: String = "",
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
    var lastActiveAt: Long = 0L,
    var ferveurDecayMs: Double = 0.0,
    var ferveurDecayAt: Long = 0L,
    var totalSteps: Int = 0,
    var kills: Int = 0,
    var chestsOpened: Int = 0,
    var goldEarned: Int = 0,
    var legend: List<JournalEntry> = emptyList(),
    var bestiary: Map<String, Int> = emptyMap(),
    var bossKills: Map<String, Int> = emptyMap(),
    /** Biome, palier et niveau de la première victoire sur un monstre, par nom. */
    var firstSeen: Map<String, FirstSeen> = emptyMap(),
    /** Biome, palier et niveau de la première victoire sur un gardien, par nom. */
    var bossFirstSeen: Map<String, FirstSeen> = emptyMap(),
    var discoveredItems: List<String> = emptyList(),
    /**
     * Collection permanente des ensembles : nom d'ensemble → slots découverts.
     * Une pièce reste acquise même si elle est vendue, démontée ou échangée.
     */
    var setPiecesDiscovered: Map<String, List<String>> = emptyMap(),
    var holdFloor: Boolean = false,
    var weeklySteps: Int = 0,
    var weekKey: String = "",
    var streak: Int = 0,
    var lastActiveDay: String = "",
    var consumables: Map<String, Int> = defaultConsumables(),
    var buffs: List<Buff> = emptyList(),
    /** Compteur monotone des coups, même après nettoyage de la liste (non sérialisé). */
    @Transient var hitSeq: Long = 0L,
    /** Derniers coups portés, pour l'affichage (non sérialisé). */
    @Transient var recentHits: List<HitEvent> = emptyList()
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

data class MobTemplate(val name: String, val icon: String) {
    /** Identifiant stable, indépendant de la langue et du nom affiché. */
    val id: String get() = icon.substringAfterLast('/').substringBeforeLast('.')
}

data class BossTemplate(val name: String, val icon: String) {
    /** Identifiant stable, indépendant de la langue et du nom affiché. */
    val id: String get() = icon.substringAfterLast('/').substringBeforeLast('.')
}

data class MobPool(
    val easy: List<MobTemplate>,
    val mid: List<MobTemplate>,
    val hard: List<MobTemplate>,
    val bosses: List<BossTemplate>
)

data class BiomeDefinition(
    val key: String,
    val name: String,
    val fromFloor: Int,
    val title: String
)

val BIOMES = listOf(
    BiomeDefinition("crypts", "CRYPTES DE VAEL", 1, "Pèlerin des Cryptes"),
    BiomeDefinition("catacombs", "CATACOMBES", 11, "Pèlerin des Abysses"),
    BiomeDefinition("ash", "CAVERNES DE CENDRE", 21, "Marcheur de Cendre"),
    BiomeDefinition("forge", "FORGE NOYÉE", 31, "Gardien de la Forge"),
    BiomeDefinition("peaks", "CIMES DU SERMENT", 41, "Souverain des Cimes"),
    BiomeDefinition("woods", "BOIS DES ÂMES", 51, "Veilleur du Bois"),
    BiomeDefinition("abyss", "ABÎME ORGANIQUE", 61, "Marcheur de l'Abîme"),
    BiomeDefinition("throne", "TRÔNE DES ÂMES", 71, "Légende des Âmes")
)

fun biomeDefFor(floor: Int): BiomeDefinition =
    BIOMES.last { floor >= it.fromFloor }

fun biomeKeyFor(floor: Int): String = biomeDefFor(floor).key

fun biomeFor(floor: Int): String = biomeDefFor(floor).name

data class BiomeRule(
    val id: String,
    val label: String,
    val description: String,
    val atkPct: Double = 0.0,
    val hpPct: Double = 0.0,
    val vitPct: Double = 0.0,
    val crit: Int = 0,
    val regen: Double = 0.0,
    val damageReduction: Double = 0.0,
    val bossDamage: Double = 0.0,
    val enemyHpMult: Double = 1.0,
    val enemyAtkMult: Double = 1.0,
    val enemyIntervalMult: Double = 1.0
)

fun biomeRuleFor(floor: Int): BiomeRule = when (biomeKeyFor(floor)) {
    "crypts" -> BiomeRule(
        "crypts", "Veillée des cryptes", "+3 % critique · ennemis fragilisés",
        crit = 3, enemyHpMult = 0.95
    )
    "catacombs" -> BiomeRule(
        "catacombs", "Passages étroits", "+6 % VIT · ennemis plus lents",
        vitPct = 0.06, enemyIntervalMult = 1.05
    )
    "ash" -> BiomeRule(
        "ash", "Air empoisonné", "+10 % ATQ · régénération réduite · ennemis agressifs",
        atkPct = 0.10, regen = -0.15, enemyAtkMult = 1.10, enemyHpMult = 0.95
    )
    "forge" -> BiomeRule(
        "forge", "Peaux de fer", "−6 % dégâts subis · ennemis plus résistants",
        damageReduction = 0.06, enemyHpMult = 1.10, enemyAtkMult = 0.95
    )
    "peaks" -> BiomeRule(
        "peaks", "Vent glacial", "+8 % VIT · −5 % PV max · ennemis rapides",
        vitPct = 0.08, hpPct = -0.05, enemyIntervalMult = 0.90
    )
    "woods" -> BiomeRule(
        "woods", "Sève ancienne", "+0,5 PV/s · ennemis coriaces",
        regen = 0.5, enemyHpMult = 0.95, enemyAtkMult = 1.05
    )
    "abyss" -> BiomeRule(
        "abyss", "Acidité", "+12 % ATQ · −5 % PV max · ennemis fragiles mais violents",
        atkPct = 0.12, hpPct = -0.05, enemyHpMult = 0.90, enemyAtkMult = 1.10
    )
    else -> BiomeRule(
        "throne", "Pression des âmes", "+12 % dégâts gardiens · −8 % PV max · ennemis renforcés",
        hpPct = -0.08, bossDamage = 0.12, enemyHpMult = 1.10, enemyAtkMult = 1.10
    )
}

data class BossAbility(val id: String, val label: String)

fun bossAbilityFor(name: String, floor: Int): BossAbility {
    val n = name.lowercase()
    return when {
        n.contains("liche") || n.contains("roi des cryptes") ->
            BossAbility("drain", "DRAIN D'ÂME")
        n.contains("hydre") -> BossAbility("regen", "RÉGÉNÉRATION")
        n.contains("tréant") || n.contains("dryade") || n.contains("chasseur d'épines") ||
            n.contains("rôdeur des lianes") -> BossAbility("roots", "RACINES")
        n.contains("vase") || n.contains("zyme") || n.contains("horreur") ||
            n.contains("orbe d'yeux") -> BossAbility("acid", "ACIDE")
        n.contains("dragon") || n.contains("wyverne") -> BossAbility("breath", "SOUFFLE")
        n.contains("forge") || n.contains("géant de fer") || n.contains("gardien de cristal") ||
            n.contains("serviteur runique") || n.contains("golem") -> BossAbility("shield", "BLINDAGE")
        n.contains("asmodee") || n.contains("asmode") || n.contains("dispater") ||
            n.contains("éreshkigal") || n.contains("gloorx") || n.contains("mnoleg") ||
            n.contains("âme première") || n.contains("mère des abysses") ->
            BossAbility("pressure", "PRESSION")
        n.contains("balrug") || n.contains("démon solaire") || n.contains("flagellant") ||
            n.contains("sentinelle") || n.contains("bourreau") || n.contains("faucheur") ||
            n.contains("mangeur") -> BossAbility("slam", "FRAPPE LOURDE")
        else -> when (biomeKeyFor(floor)) {
            "crypts" -> BossAbility("drain", "DRAIN D'ÂME")
            "catacombs", "woods" -> BossAbility("roots", "RACINES")
            "ash" -> BossAbility("slam", "FRAPPE LOURDE")
            "forge" -> BossAbility("shield", "BLINDAGE")
            "peaks" -> BossAbility("breath", "SOUFFLE")
            "abyss" -> BossAbility("acid", "ACIDE")
            else -> BossAbility("pressure", "PRESSION")
        }
    }
}

val CRYPT_POOL = MobPool(
    easy = listOf(
        MobTemplate("Chauve-souris", "mon/bat.png"),
        MobTemplate("Goule", "mon/ghoul.png"),
        MobTemplate("Cadavre rampant", "mon/crawling_corpse.png"),
        MobTemplate("Crâne volant", "mon/flying_skull.png"),
        MobTemplate("Ver des tombes", "mon/worm.png")
    ),
    mid = listOf(
        MobTemplate("Guerrier squelette", "mon/skeletal_warrior.png"),
        MobTemplate("Spectre", "mon/ghost.png"),
        MobTemplate("Revenant", "mon/revenant.png"),
        MobTemplate("Spectre silencieux", "mon/silent_spectre.png"),
        MobTemplate("Wight", "mon/wight.png"),
        MobTemplate("Âme noyée", "mon/drowned_soul.png")
    ),
    hard = listOf(
        MobTemplate("Vampire", "mon/vampire.png"),
        MobTemplate("Chevalier vampire", "mon/vampire_knight.png"),
        MobTemplate("Sigmund le reclus", "mon/sigmund.png"),
        MobTemplate("Dragon d'os", "mon/bone_dragon.png")
    ),
    bosses = listOf(
        BossTemplate("Roi des Cryptes", "mon/lich.png"),
        BossTemplate("Dragon d'os", "mon/bone_dragon.png"),
        BossTemplate("Liche ancienne", "mon/ancient_lich.png"),
        BossTemplate("Chevalier vampire", "mon/vampire_knight.png")
    )
)

val CATACOMB_POOL = MobPool(
    easy = listOf(
        MobTemplate("Kobold", "mon/kobold.png"),
        MobTemplate("Gobelin", "mon/goblin.png"),
        MobTemplate("Araignée", "mon/spider.png"),
        MobTemplate("Araignée sauteuse", "mon/jumping_spider.png"),
        MobTemplate("Cafard géant", "mon/giant_cockroach.png"),
        MobTemplate("Grenouille géante", "mon/giant_frog.png"),
        MobTemplate("Chauve-souris des galeries", "mon/bat_galeries.png")
    ),
    mid = listOf(
        MobTemplate("Gnoll", "mon/gnoll.png"),
        MobTemplate("Sergent gnoll", "mon/gnoll_sergeant.png"),
        MobTemplate("Hobgobelin", "mon/hobgoblin.png"),
        MobTemplate("Orc", "mon/orc.png"),
        MobTemplate("Naga", "mon/naga.png"),
        MobTemplate("Coléoptère blindé", "mon/boulder_beetle.png")
    ),
    hard = listOf(
        MobTemplate("Guerrier orc", "mon/orc_warrior.png"),
        MobTemplate("Chevalier orc", "mon/orc_knight.png"),
        MobTemplate("Grand kobold", "mon/big_kobold.png"),
        MobTemplate("Ogre", "mon/ogre.png"),
        MobTemplate("Troll des abîmes", "mon/deep_troll.png")
    ),
    bosses = listOf(
        BossTemplate("Hydre de Lerne", "mon/lernaean_hydra01.png"),
        BossTemplate("Grande naga", "mon/greater_naga.png"),
        BossTemplate("Manticore", "mon/manticore.png"),
        BossTemplate("Hydre mineure", "mon/hydra1.png")
    )
)

val ASH_POOL = MobPool(
    easy = listOf(
        MobTemplate("Diablotin", "mon/crimson_imp.png"),
        MobTemplate("Limier des enfers", "mon/hell_hound.png"),
        MobTemplate("Chauve-souris de feu", "mon/fire_bat.png"),
        MobTemplate("Scorpion", "mon/scorpion.png")
    ),
    mid = listOf(
        MobTemplate("Élémentaire de feu", "mon/fire_elemental.png"),
        MobTemplate("Éfrit", "mon/efreet.png"),
        MobTemplate("Manticore de cendre", "mon/manticore_ember.png"),
        MobTemplate("Légionnaire infernal", "mon/hellion.png")
    ),
    hard = listOf(
        MobTemplate("Balrug", "mon/balrug.png"),
        MobTemplate("Démon solaire", "mon/sun_demon.png"),
        MobTemplate("Flagellant de soufre", "mon/brimstone_fiend.png"),
        MobTemplate("Sentinelle des enfers", "mon/hell_sentinel.png")
    ),
    bosses = listOf(
        BossTemplate("Balrug", "mon/balrug.png"),
        BossTemplate("Démon solaire", "mon/sun_demon.png"),
        BossTemplate("Flagellant de soufre", "mon/brimstone_fiend.png"),
        BossTemplate("Sentinelle des enfers", "mon/hell_sentinel.png")
    )
)

val FORGE_POOL = MobPool(
    easy = listOf(
        MobTemplate("Élémentaire de fer", "mon/iron_elemental.png"),
        MobTemplate("Oushabti", "mon/ushabti.png"),
        MobTemplate("Nain des profondeurs", "mon/deep_dwarf.png"),
        MobTemplate("Kobold mineur", "mon/kobold_mineur.png")
    ),
    mid = listOf(
        MobTemplate("Golem d'atelier", "mon/iron_golem_workshop.png"),
        MobTemplate("Golem gardien", "mon/guardian_golem.png"),
        MobTemplate("Troll des scories", "mon/deep_troll_slag.png"),
        MobTemplate("Troll de fonte", "mon/iron_troll_molten.png")
    ),
    hard = listOf(
        MobTemplate("Gardien de cristal", "mon/crystal_guardian.png"),
        MobTemplate("Serviteur runique", "mon/spellforged_servitor.png"),
        MobTemplate("Troll de fer", "mon/iron_troll.png"),
        MobTemplate("Golem de fer", "mon/iron_golem.png")
    ),
    bosses = listOf(
        BossTemplate("Seigneur de la Forge", "mon/iron_giant.png"),
        BossTemplate("Gardien de cristal", "mon/crystal_guardian.png"),
        BossTemplate("Serviteur runique", "mon/spellforged_servitor.png"),
        BossTemplate("Golem de fer", "mon/iron_golem.png")
    )
)

val PEAK_POOL = MobPool(
    easy = listOf(
        MobTemplate("Loup des neiges", "mon/wolf_snow.png"),
        MobTemplate("Bête de glace", "mon/ice_beast.png"),
        MobTemplate("Raiju", "mon/raiju.png"),
        MobTemplate("Chauve-souris des neiges", "mon/bat_neiges.png")
    ),
    mid = listOf(
        MobTemplate("Ours polaire", "mon/polar_bear.png"),
        MobTemplate("Griffon", "mon/griffon.png"),
        MobTemplate("Diable de glace", "mon/ice_devil.png"),
        MobTemplate("Harpie", "mon/harpy.png"),
        MobTemplate("Wyverne", "mon/wyvern.png")
    ),
    hard = listOf(
        MobTemplate("Géant de givre", "mon/frost_giant.png"),
        MobTemplate("Démon blizzard", "mon/blizzard_demon.png"),
        MobTemplate("Dragon d'orage", "mon/storm_dragon.png"),
        MobTemplate("Dragon d'ombre", "mon/shadow_dragon.png"),
        MobTemplate("Dragon de vif-argent", "mon/quicksilver_dragon.png")
    ),
    bosses = listOf(
        BossTemplate("Dragon de givre", "mon/ice_dragon.png"),
        BossTemplate("Dragon d'orage", "mon/storm_dragon.png"),
        BossTemplate("Géant de givre", "mon/frost_giant.png"),
        BossTemplate("Dragon d'ombre", "mon/shadow_dragon.png")
    )
)

val WOOD_POOL = MobPool(
    easy = listOf(
        MobTemplate("Faune", "mon/faun.png"),
        MobTemplate("Abeille tueuse", "mon/killer_bee.png"),
        MobTemplate("Grenouille des sous-bois", "mon/giant_frog_woods.png"),
        MobTemplate("Loup noir", "mon/wolf.png")
    ),
    mid = listOf(
        MobTemplate("Dryade", "mon/dryad.png"),
        MobTemplate("Harpie sylvestre", "mon/harpy_woods.png"),
        MobTemplate("Satyre", "mon/satyr.png"),
        MobTemplate("Reine des abeilles", "mon/queen_bee.png"),
        MobTemplate("Rôdeur des racines", "mon/vine_stalker_roots.png"),
        MobTemplate("Ours noir", "mon/black_bear.png")
    ),
    hard = listOf(
        MobTemplate("Tréant", "mon/treant.png"),
        MobTemplate("Chasseur d'épines", "mon/thorn_hunter.png"),
        MobTemplate("Rôdeur des lianes", "mon/vine_stalker.png"),
        MobTemplate("Griffon des bois", "mon/griffon_woods.png")
    ),
    bosses = listOf(
        BossTemplate("Tréant", "mon/treant.png"),
        BossTemplate("Dryade", "mon/dryad.png"),
        BossTemplate("Chasseur d'épines", "mon/thorn_hunter.png"),
        BossTemplate("Rôdeur des lianes", "mon/vine_stalker.png")
    )
)

val ABYSS_POOL = MobPool(
    easy = listOf(
        MobTemplate("Vase", "mon/ooze.png"),
        MobTemplate("Gelée", "mon/jelly.png"),
        MobTemplate("Gelée d'azur", "mon/azure_jelly.png"),
        MobTemplate("Œil géant", "mon/giant_eyeball.png"),
        MobTemplate("Champignon errant", "mon/wandering_mushroom.png")
    ),
    mid = listOf(
        MobTemplate("Vase mortelle", "mon/death_ooze.png"),
        MobTemplate("Chapeau de mort", "mon/deathcap.png"),
        MobTemplate("Œil drainant", "mon/eye_of_draining.png"),
        MobTemplate("Rejeton étoilé", "mon/tentacled_starspawn.png")
    ),
    hard = listOf(
        MobTemplate("Horreur rampante", "mon/lurking_horror.png"),
        MobTemplate("Masse stellaire", "mon/starcursed_mass.png"),
        MobTemplate("Grand orbe d'yeux", "mon/great_orb_of_eyes.png"),
        MobTemplate("Zyme ancienne", "mon/ancient_zyme.png")
    ),
    bosses = listOf(
        BossTemplate("Vase mortelle", "mon/death_ooze.png"),
        BossTemplate("Zyme ancienne", "mon/ancient_zyme.png"),
        BossTemplate("Grand orbe d'yeux", "mon/great_orb_of_eyes.png"),
        BossTemplate("Horreur rampante", "mon/lurking_horror.png")
    )
)

val THRONE_POOL = MobPool(
    easy = listOf(
        MobTemplate("Diablotin de braise", "mon/crimson_imp_ember.png"),
        MobTemplate("Diablotin d'ombre", "mon/shadow_imp.png"),
        MobTemplate("Diablotin de fer", "mon/iron_imp.png"),
        MobTemplate("Ufetubus", "mon/ufetubus.png")
    ),
    mid = listOf(
        MobTemplate("Légionnaire des abysses", "mon/hellion_abyss.png"),
        MobTemplate("Sentinelle du trône", "mon/hell_sentinel_throne.png"),
        MobTemplate("Diable de lave", "mon/ice_devil_lava.png"),
        MobTemplate("Balrug déchu", "mon/balrug_fallen.png")
    ),
    hard = listOf(
        MobTemplate("Bourreau", "mon/executioner.png"),
        MobTemplate("Faucheur", "mon/reaper.png"),
        MobTemplate("Mangeur d'âmes", "mon/soul_eater.png"),
        MobTemplate("Géant de fer", "mon/iron_giant.png")
    ),
    bosses = listOf(
        BossTemplate("Dispater", "mon/dispater.png"),
        BossTemplate("Asmodée", "mon/asmodeus.png"),
        BossTemplate("Éreshkigal", "mon/ereshkigal.png"),
        BossTemplate("Gloorx Vloq", "mon/gloorx_vloq.png"),
        BossTemplate("Mnoleg", "mon/mnoleg.png"),
        BossTemplate("L'Âme Première", "mon/cerebov.png")
    )
)

val BIOME_POOLS = mapOf(
    "crypts" to CRYPT_POOL,
    "catacombs" to CATACOMB_POOL,
    "ash" to ASH_POOL,
    "forge" to FORGE_POOL,
    "peaks" to PEAK_POOL,
    "woods" to WOOD_POOL,
    "abyss" to ABYSS_POOL,
    "throne" to THRONE_POOL
)

val SPECIAL_BOSSES = listOf(
    BossTemplate("Mère des Abysses", "mon/jormungandr.png"),
    BossTemplate("L'Âme Première", "mon/cerebov.png")
)

/** Palier de référence des gardiens de jalon qui n'appartiennent à aucun pool. */
fun specialBossFloor(name: String): Int? = when (name) {
    "Mère des Abysses" -> 25
    "L'Âme Première" -> 100
    else -> null
}

fun allMobTemplates(): List<MobTemplate> =
    BIOME_POOLS.values.flatMap { it.easy + it.mid + it.hard }.distinctBy { it.name }

fun allBossTemplates(): List<BossTemplate> =
    (BIOME_POOLS.values.flatMap { it.bosses } + SPECIAL_BOSSES).distinctBy { it.name }

/** Biome et pool d'un monstre, ou null s'il n'existe plus dans les pools actuels. */
data class MobLocation(val biomeKey: String, val tier: String, val fromFloor: Int)

fun mobLocation(name: String): MobLocation? {
    BIOME_POOLS.forEach { (key, pool) ->
        val tier = when {
            pool.easy.any { it.name == name } -> "easy"
            pool.mid.any { it.name == name } -> "mid"
            pool.hard.any { it.name == name } -> "hard"
            else -> null
        }
        if (tier != null) {
            return MobLocation(
                biomeKey = key,
                tier = tier,
                fromFloor = BIOMES.firstOrNull { it.key == key }?.fromFloor ?: 1
            )
        }
    }
    return null
}

/**
 * Anciens noms partagés entre plusieurs pools, remplacés par des variantes
 * uniques. Sert une seule fois à la migration des sauvegardes v5 → v6.
 */
val MOB_RENAMES: Map<String, List<String>> = mapOf(
    "Chauve-souris" to listOf(
        "Chauve-souris", "Chauve-souris des galeries", "Chauve-souris des neiges"
    ),
    "Kobold" to listOf("Kobold", "Kobold mineur"),
    "Grenouille géante" to listOf("Grenouille géante", "Grenouille des sous-bois"),
    "Loup noir" to listOf("Loup noir", "Loup des neiges"),
    "Harpie" to listOf("Harpie", "Harpie sylvestre"),
    "Griffon" to listOf("Griffon", "Griffon des bois"),
    "Diablotin" to listOf("Diablotin", "Diablotin de braise"),
    "Légionnaire infernal" to listOf("Légionnaire infernal", "Légionnaire des abysses"),
    "Sentinelle des enfers" to listOf("Sentinelle des enfers", "Sentinelle du trône"),
    "Balrug" to listOf("Balrug", "Balrug déchu"),
    "Diable de glace" to listOf("Diable de glace", "Diable de lave"),
    "Troll des abîmes" to listOf("Troll des abîmes", "Troll des scories"),
    "Golem de fer" to listOf("Golem de fer", "Golem d'atelier"),
    "Troll de fer" to listOf("Troll de fer", "Troll de fonte"),
    "Rôdeur des lianes" to listOf("Rôdeur des lianes", "Rôdeur des racines"),
    "Manticore" to listOf("Manticore de cendre")
)

fun ownedItemNames(state: GameState): Set<String> =
    (state.equipment + state.inventory + state.stash)
        .map { it.name.substringBefore(" · ") }
        .toSet()

fun discoveredItemNames(state: GameState): Set<String> =
    state.discoveredItems.toSet() + ownedItemNames(state)

fun ownedSetSlots(state: GameState, setName: String): Set<String> =
    (state.equipment + state.inventory + state.stash)
        .filter { it.setName == setName }
        .map { it.slot }
        .toSet()

/** Ajoute une pièce à la collection permanente (si ce slot n'est pas déjà connu). */
fun recordSetPiece(state: GameState, item: Item) {
    if (item.setName.isBlank() || item.slot.isBlank()) return
    val known = state.setPiecesDiscovered[item.setName].orEmpty()
    if (item.slot in known) return
    state.setPiecesDiscovered = state.setPiecesDiscovered + (item.setName to (known + item.slot))
}

/** Recale la collection permanente sur les pièces déjà possédées (migration, import). */
fun seedSetCollections(state: GameState) {
    (state.equipment + state.inventory + state.stash).forEach { recordSetPiece(state, it) }
}

/** Slots découverts d'un ensemble : la collection est permanente. */
fun discoveredSetSlots(state: GameState, setName: String): Set<String> =
    state.setPiecesDiscovered[setName].orEmpty().toSet()

/** Slots réellement équipés d'un ensemble : seuls ceux-ci donnent le bonus. */
fun equippedSetSlots(state: GameState, setName: String): Set<String> =
    state.equipment.filter { it.setName == setName }.map { it.slot }.toSet()

/** Biomes où l'ensemble peut tomber, avec leur plage de paliers. */
fun setSourceLabel(setName: String): String {
    val keys = BIOME_SETS.filterValues { setName in it }.keys
    if (keys.isEmpty()) return ""
    return keys.mapNotNull { key ->
        val biome = BIOMES.firstOrNull { it.key == key } ?: return@mapNotNull null
        val next = BIOMES.firstOrNull { it.fromFloor > biome.fromFloor }
        val range = if (next == null) "${biome.fromFloor}+" else "${biome.fromFloor}–${next.fromFloor - 1}"
        "${biome.name} ($range)"
    }.joinToString(" · ")
}

fun mobPoolFor(floor: Int): MobPool =
    BIOME_POOLS[biomeKeyFor(floor)] ?: CRYPT_POOL

val GENERIC_LOOT = listOf(
    Item("loot_sword", "Lame runique", "weapon", "rare", "item/long_sword3.png", 1, atk = 12, value = 90),
    Item("loot_axe", "Hache de guerre", "weapon", "uncommon", "item/hand_axe2.png", 1, atk = 10, critPct = 2, value = 65),
    Item("loot_mace", "Masse d'armes", "weapon", "rare", "item/mace2.png", 1, atk = 9, critPct = 3, value = 75),
    Item("loot_spear", "Lance du guet", "weapon", "common", "item/spear2.png", 1, atk = 8, value = 35),
    Item("loot_mail", "Cotte de mailles", "body", "uncommon", "item/scale_mail1.png", 1, hp = 34, vitPct = 2, value = 70),
    Item("loot_plate", "Plate du gardien", "body", "rare", "item/plate1.png", 1, hp = 52, value = 110),
    Item("loot_helm", "Heaume gravé", "head", "rare", "item/helmet3.png", 1, hp = 22, critPct = 3, value = 80),
    Item("loot_glove", "Gants de braise", "hands", "uncommon", "item/glove1.png", 1, atk = 6, value = 55),
    Item("loot_boots", "Bottes de mousse", "feet", "uncommon", "item/boots4_green.png", 1, vitPct = 5, value = 55),
    Item("loot_ring", "Anneau d'âme", "talisman", "epic", "item/ring-regen.png", 1, regen = 0.4, critPct = 2, value = 140),
    Item("loot_idol", "Idole dorée", "talisman", "rare", "item/04.png", 1, atk = 4, value = 180),
    Item("loot_art_helm", "Heaume d'art", "head", "epic", "item/helmet_art1.png", 1, hp = 24, critPct = 4, value = 125),
    Item("loot_reliquary", "Coffret-reliquaire", "talisman", "rare", "item/misc_box.png", 1, hp = 18, regen = 0.3, value = 110),
    Item("loot_lantern", "Lanterne des âmes", "talisman", "epic", "item/misc_lantern.png", 1, atk = 6, regen = 0.4, value = 145)
)

val CRYPT_LOOT = listOf(
    Item("crypt_blade", "Lame d'ossuaire", "weapon", "uncommon", "item/short_sword1.png", 1, atk = 11, critPct = 1, value = 58),
    Item("crypt_shroud", "Suaire du veilleur", "body", "rare", "item/leather_armour1.png", 1, hp = 30, vitPct = 2, value = 72),
    Item("crypt_helm", "Heaume des cryptes", "head", "rare", "item/helmet2.png", 1, hp = 20, critPct = 3, value = 76),
    Item("crypt_ward", "Amulette de garde", "talisman", "epic", "item/i-warding.png", 1, hp = 26, regen = 0.3, value = 135)
)

val CATACOMB_LOOT = listOf(
    Item("cat_axe", "Hache gnoll", "weapon", "uncommon", "item/hand_axe1.png", 1, atk = 10, critPct = 2, value = 62),
    Item("cat_shell", "Carapace de coléoptère", "body", "rare", "item/animal_skin1.png", 1, hp = 38, vitPct = 2, value = 80),
    Item("cat_boots", "Bottes de soie", "feet", "rare", "item/boots3_stripe.png", 1, vitPct = 7, value = 68),
    Item("cat_mask", "Masque de gnoll", "head", "epic", "item/helmet4.png", 1, hp = 18, critPct = 5, value = 120)
)

val ASH_LOOT = listOf(
    Item("ash_blade", "Lame de braise", "weapon", "rare", "item/falchion1.png", 1, atk = 14, critPct = 3, value = 92),
    Item("ash_plate", "Plate de cendre", "body", "epic", "item/ring_mail1.png", 1, hp = 48, atk = 4, value = 140),
    Item("ash_boots", "Bottes de limier", "feet", "rare", "item/boots2_jackboots.png", 1, vitPct = 6, hp = 8, value = 76),
    Item("ash_ring", "Anneau de soufre", "talisman", "epic", "item/i-rage.png", 1, atk = 9, critPct = 3, value = 130)
)

val FORGE_LOOT = listOf(
    Item("forge_hammer", "Marteau de forge", "weapon", "rare", "item/eveningstar1.png", 1, atk = 16, critPct = 2, value = 98),
    Item("forge_plate", "Harnois de forge", "body", "epic", "item/ring_mail1.png", 1, hp = 55, vitPct = 1, value = 150),
    Item("forge_gauntlets", "Gantelets de fer", "hands", "rare", "item/glove2.png", 1, atk = 8, hp = 10, value = 80),
    Item("forge_ring", "Anneau de vif-argent", "talisman", "legendary", "item/i-protection.png", 1, hp = 30, regen = 0.4, value = 190)
)

val PEAK_LOOT = listOf(
    Item("peak_blade", "Lame de givre", "weapon", "rare", "item/rapier1.png", 1, atk = 13, critPct = 4, value = 94),
    Item("peak_mail", "Cotte de glace", "body", "epic", "item/ice_dragon_armour.png", 1, hp = 42, vitPct = 4, value = 145),
    Item("peak_boots", "Bottes des cimes", "feet", "rare", "item/boots1_brown.png", 1, vitPct = 7, regen = 0.2, value = 78),
    Item("peak_ring", "Anneau d'orage", "talisman", "legendary", "item/i-r-cold.png", 1, atk = 10, critPct = 4, value = 195)
)

val WOOD_LOOT = listOf(
    Item("wood_staff", "Bâton de dryade", "weapon", "rare", "item/quarterstaff.png", 1, atk = 12, regen = 0.3, value = 90),
    Item("wood_mail", "Armure d'écorce", "body", "rare", "item/animal_skin1.png", 1, hp = 36, vitPct = 3, value = 84),
    Item("wood_boots", "Bottes de feuilles", "feet", "uncommon", "item/boots3_stripe.png", 1, vitPct = 6, value = 64),
    Item("wood_horn", "Corne de satyre", "talisman", "epic", "item/misc_horn.png", 1, atk = 7, vitPct = 4, value = 125)
)

val ABYSS_LOOT = listOf(
    Item("abyss_whip", "Fouet de tentacule", "weapon", "epic", "item/bullwhip2.png", 1, atk = 15, critPct = 3, value = 130),
    Item("abyss_plate", "Carapace d'abîme", "body", "epic", "item/shadow_dragon_scale_mail.png", 1, hp = 44, atk = 5, value = 148),
    Item("abyss_ring", "Anneau de mucilage", "talisman", "rare", "item/i-r-poison.png", 1, hp = 20, regen = 0.4, value = 96),
    Item("abyss_orb", "Orbe du néant", "talisman", "legendary", "item/misc_orb2.png", 1, atk = 12, hp = 30, value = 200)
)

val THRONE_LOOT = listOf(
    Item("throne_blade", "Lame du trône", "weapon", "legendary", "item/greatsword3.png", 1, atk = 20, critPct = 5, value = 220),
    Item("throne_mail", "Harnois du trône", "body", "legendary", "item/gold_dragon_armour.png", 1, hp = 70, atk = 6, value = 230),
    Item("throne_crown", "Couronne des âmes", "head", "legendary", "item/helmet_art2.png", 1, hp = 28, critPct = 6, value = 210),
    Item("throne_ring", "Anneau des âmes", "talisman", "legendary", "item/i-slaying.png", 1, atk = 14, critPct = 5, value = 240)
)

val BIOME_LOOT = mapOf(
    "crypts" to GENERIC_LOOT + CRYPT_LOOT,
    "catacombs" to GENERIC_LOOT + CATACOMB_LOOT,
    "ash" to GENERIC_LOOT + ASH_LOOT,
    "forge" to GENERIC_LOOT + FORGE_LOOT,
    "peaks" to GENERIC_LOOT + PEAK_LOOT,
    "woods" to GENERIC_LOOT + WOOD_LOOT,
    "abyss" to GENERIC_LOOT + ABYSS_LOOT,
    "throne" to GENERIC_LOOT + THRONE_LOOT
)

val LOOT_TEMPLATES = BIOME_LOOT.values.flatten().distinctBy { it.id }

fun lootTemplatesFor(floor: Int): List<Item> =
    BIOME_LOOT[biomeKeyFor(floor)] ?: GENERIC_LOOT

val BIOME_SETS = mapOf(
    "crypts" to listOf("veilleur", "os"),
    "catacombs" to listOf("os", "toile"),
    "ash" to listOf("braise", "cendre"),
    "forge" to listOf("forge"),
    "peaks" to listOf("givre", "serment"),
    "woods" to listOf("bois"),
    "abyss" to listOf("abysse"),
    "throne" to listOf("trone")
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
    JournalEntry("Le sentier attend vos pas", nowTime(), "neutral"),
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

val ITEM_EFFECTS = listOf("vampirisme", "rempart", "frenesie", "chasse", "vitalite", "precision")

val ITEM_SETS = listOf(
    "veilleur", "braise", "givre", "os", "toile", "cendre",
    "forge", "serment", "bois", "abysse", "trone"
)

fun setLabel(name: String): String = when (name) {
    "veilleur" -> "SERVICE DU VEILLEUR"
    "braise" -> "BRAISE ANCIENNE"
    "givre" -> "SERMENT DE GIVRE"
    "os" -> "OSSATURE ANCIENNE"
    "toile" -> "TOILE DES CATACOMBES"
    "cendre" -> "CENDRE ÉTERNELLE"
    "forge" -> "FORGE ABYSSALE"
    "serment" -> "SERMENT DES CIMES"
    "bois" -> "BOIS DES ÂMES"
    "abysse" -> "ABÎME ORGANIQUE"
    "trone" -> "TRÔNE DES ÂMES"
    else -> ""
}

fun setBonusDescription(name: String, pieces: Int): String {
    if (pieces < 2) return ""
    return when (name) {
        "veilleur" -> if (pieces >= 6) "+30 ATQ, +90 PV" else if (pieces >= 4) "+20 ATQ, +60 PV" else "+10 ATQ, +30 PV"
        "braise" -> if (pieces >= 6) "+9 % crit., +15 % vol de vie" else if (pieces >= 4) "+6 % crit., +10 % vol de vie" else "+3 % crit., +5 % vol de vie"
        "givre" -> if (pieces >= 6) "+15 % VIT, +0,9 PV/s" else if (pieces >= 4) "+10 % VIT, +0,6 PV/s" else "+5 % VIT, +0,3 PV/s"
        "os" -> if (pieces >= 6) "+18 ATQ, +75 PV" else if (pieces >= 4) "+12 ATQ, +50 PV" else "+6 ATQ, +25 PV"
        "toile" -> if (pieces >= 6) "+9 % VIT, +6 % crit." else if (pieces >= 4) "+6 % VIT, +4 % crit." else "+3 % VIT, +2 % crit."
        "cendre" -> if (pieces >= 6) "+24 ATQ, +6 % crit." else if (pieces >= 4) "+16 ATQ, +3 % crit." else "+8 ATQ"
        "forge" -> if (pieces >= 6) "+105 PV, −6 % dégâts" else if (pieces >= 4) "+70 PV, −3 % dégâts" else "+35 PV"
        "serment" -> if (pieces >= 6) "+21 ATQ, +16 % dégâts gardiens" else if (pieces >= 4) "+14 ATQ, +8 % dégâts gardiens" else "+7 ATQ"
        "bois" -> if (pieces >= 6) "+12 % VIT, +1,2 PV/s" else if (pieces >= 4) "+8 % VIT, +0,7 PV/s" else "+4 % VIT"
        "abysse" -> if (pieces >= 6) "+60 PV, +12 % vol de vie" else if (pieces >= 4) "+40 PV, +8 % vol de vie" else "+20 PV, +4 % vol de vie"
        "trone" -> if (pieces >= 6) "+30 ATQ, +30 % dégâts critiques" else if (pieces >= 4) "+20 ATQ, +15 % dégâts critiques" else "+10 ATQ"
        else -> ""
    }
}

fun titleFor(floor: Int): String = when {
    floor >= 100 -> "Légende des Âmes"
    floor >= 71 -> "Souverain du Trône"
    floor >= 61 -> "Marcheur de l'Abîme"
    floor >= 51 -> "Veilleur du Bois"
    floor >= 41 -> "Souverain des Cimes"
    floor >= 31 -> "Gardien de la Forge"
    floor >= 21 -> "Marcheur de Cendre"
    floor >= 11 -> "Pèlerin des Abysses"
    floor >= 5 -> "Veilleur des Cryptes"
    else -> "Pèlerin"
}

fun effectLabel(effect: String): String = when (effect) {
    "vampirisme" -> "VAMPIRISME"
    "rempart" -> "REMPART"
    "frenesie" -> "FRÉNÉSIE"
    "chasse" -> "CHASSE"
    "vitalite" -> "VITALITÉ"
    "precision" -> "PRÉCISION"
    else -> ""
}

fun effectDescription(effect: String): String = when (effect) {
    "vampirisme" -> "+8 % des dégâts infligés rendus en PV"
    "rempart" -> "−10 % de dégâts subis"
    "frenesie" -> "+40 % de dégâts critiques"
    "chasse" -> "+25 % de dégâts contre les gardiens"
    "vitalite" -> "+1,2 PV/s"
    "precision" -> "+5 % critique"
    else -> ""
}

fun itemScore(item: Item): Int {
    val effectBonus = when (item.effect) {
        "vampirisme" -> 30
        "rempart" -> 38
        "frenesie" -> 30
        "chasse" -> 26
        "vitalite" -> 34
        "precision" -> 30
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
    floor >= 70 -> "VIII"
    floor >= 60 -> "VII"
    floor >= 50 -> "VI"
    floor >= 40 -> "V"
    floor >= 30 -> "IV"
    floor >= 20 -> "III"
    floor >= 10 -> "II"
    else -> "I"
}

fun milestoneBonus(floor: Int): Int = floor / 10

/** Palier du feu de camp : un par biome, tous les 10 paliers. */
fun fireTier(floor: Int): Int = ((floor - 1).coerceAtLeast(0) / 10).coerceIn(0, 7)

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
            "vitalite" -> regen += 1.2
            "precision" -> crit += 5
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
                "os" -> {
                    atk += 6 * tier
                    maxHp += 25 * tier
                }
                "toile" -> {
                    vit += 0.03 * tier
                    crit += 2 * tier
                }
                "cendre" -> {
                    atk += 8 * tier
                    crit += 2 * tier
                }
                "forge" -> {
                    maxHp += 35 * tier
                    damageReduction += 0.03 * tier
                }
                "serment" -> {
                    atk += 7 * tier
                    bossDamage += 0.08 * tier
                }
                "bois" -> {
                    vit += 0.04 * tier
                    regen += 0.35 * tier
                }
                "abysse" -> {
                    maxHp += 20 * tier
                    lifesteal += 0.04 * tier
                }
                "trone" -> {
                    atk += 10 * tier
                    critDamage += 0.15 * tier
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
            "regen" -> regen += buff.magnitude
            "vulnerable" -> damageTaken += buff.magnitude
        }
    }
    val rule = biomeRuleFor(state.floor)
    atk = (atk * (1.0 + rule.atkPct)).roundToInt()
    maxHp = (maxHp * (1.0 + rule.hpPct)).roundToInt()
    vit *= 1.0 + rule.vitPct
    crit += rule.crit
    regen += rule.regen
    damageReduction += rule.damageReduction
    bossDamage += rule.bossDamage
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

fun biomeBossFor(floor: Int): BossTemplate {
    val pool = mobPoolFor(floor)
    val def = biomeDefFor(floor)
    val index = ((floor - def.fromFloor) / 10).coerceAtLeast(0) % pool.bosses.size
    return pool.bosses[index]
}

fun bossFor(floor: Int): BossTemplate = when {
    floor % 100 == 0 -> BossTemplate("L'Âme Première", "mon/cerebov.png")
    floor % 50 == 0 -> BossTemplate("Dragon de givre", "mon/ice_dragon.png")
    floor % 25 == 0 -> BossTemplate("Mère des Abysses", "mon/jormungandr.png")
    else -> biomeBossFor(floor)
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
    setPiecesDiscovered = setPiecesDiscovered.mapValues { it.value.toList() },
    consumables = consumables.toMap(),
    buffs = buffs.toList(),
    special = special?.let { it.copy(offer = it.offer?.copy()) }
)
