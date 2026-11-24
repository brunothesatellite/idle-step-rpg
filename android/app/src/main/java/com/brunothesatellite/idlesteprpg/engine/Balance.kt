package com.brunothesatellite.idlesteprpg.engine

/**
 * Source de vérité des constantes d'équilibrage.
 *
 * - [Current] décrit le comportement livré en 0.20.0.
 * - [Target] contient les décisions validées le 24/11/2026, pas encore branchées.
 *
 * Toute modification doit être répercutée dans `tools/equilibrage/balance_constants.py`
 * et vérifiée par `tools/equilibrage/check_constants.py`.
 */
object Balance {
    const val VERSION = "0.20.0"

    // --- Comportement courant (0.20.0) ---
    const val XP_BASE = 120.0
    const val XP_GROWTH = 1.2
    const val XP_MOB_BASE = 12
    const val XP_MOB_PER_LEVEL = 5
    const val XP_ELITE_MULT = 1.8
    const val XP_BOSS_MULT = 10
    const val FARM_XP_MULT = 0.5

    const val HERO_ATK_BASE = 18
    const val HERO_ATK_PER_LEVEL = 3
    const val HERO_ATK_PER_CAMP = 3
    const val HERO_ATK_PER_MILESTONE = 2
    const val HERO_HP_BASE = 150
    const val HERO_HP_PER_LEVEL = 18
    const val HERO_HP_PER_CAMP = 30
    const val HERO_HP_PER_MILESTONE = 12
    const val HERO_VIT_BASE = 1.0
    const val HERO_VIT_PER_LEVEL = 0.01
    const val HERO_VIT_PER_CAMP = 0.025
    const val HERO_CRIT_BASE = 10
    const val HERO_REGEN_BASE = 0.6
    const val HERO_MIN_INTERVAL_MS = 300.0

    const val CAMP_BLADE_COST_BASE = 45.0
    const val CAMP_BLADE_COST_MULT = 1.20
    const val CAMP_BREATH_COST_BASE = 40.0
    const val CAMP_BREATH_COST_MULT = 1.18
    const val CAMP_SWIFT_COST_BASE = 70.0
    const val CAMP_SWIFT_COST_MULT = 1.24

    const val DEPTH_START_FLOOR = 10
    const val DEPTH_CAP = 40.0
    const val MOB_DEPTH_BASE = 1.12
    const val BOSS_DEPTH_BASE = 1.08
    const val TARGET_WEIGHT_START = 20.0
    const val TARGET_WEIGHT_FULL = 35.0

    const val MOB_HP_TARGET_BASE = 26.0
    const val MOB_HP_TARGET_STEP = 0.08
    const val MOB_ATK_TARGET_BASE = 9.0
    const val MOB_ATK_TARGET_STEP = 0.028
    const val BOSS_HP_TARGET_BASE = 72.0
    const val BOSS_HP_TARGET_STEP = 0.20
    const val BOSS_ATK_TARGET_BASE = 3.6
    const val BOSS_ATK_TARGET_STEP = 0.012

    const val MOB_HP_BASE = 55.0
    const val MOB_HP_PER_LEVEL = 34.0
    const val MOB_ATK_BASE = 6.0
    const val MOB_ATK_PER_LEVEL = 2.2
    const val MOB_INTERVAL_MS = 1250.0

    const val ELITE_CHANCE = 0.06
    const val ELITE_HP_MULT = 1.8
    const val ELITE_ATK_MULT = 1.15
    const val ELITE_LEVEL_BONUS = 2
    const val ELITE_INTERVAL_MS = 1100.0

    const val BOSS_LEVEL_OFFSET = 2
    const val BOSS_PHASE_THRESHOLD = 0.5
    const val BOSS_PHASE_INTERVAL_MS = 950.0
    const val BOSS_ENRAGE_AFTER_MS = 15_000.0
    const val BOSS_ENRAGE_STEP_MS = 5_000.0
    const val BOSS_ENRAGE_STEP = 0.25
    const val BOSS_ENRAGE_CAP = 0.75
    const val SHIELD_REDUCTION = 0.40
    const val SHIELD_DURATION_MS = 3_000.0
    const val BOSS_ABILITY_FIRST_MS = 6_500.0
    const val BOSS_ABILITY_INTERVAL_MS = 7_000.0
    const val BOSS_REGEN_RATE = 0.008

    const val BOSS_MOD_BLINDED_HP = 1.30
    const val BOSS_MOD_BERSERK_ATK = 1.30
    const val BOSS_MOD_REGEN_HP = 1.15
    const val BOSS_MOD_THIEF_ATK = 1.10
    const val BOSS_MOD_FAST_INTERVAL_MS = 900.0

    const val FARM_LEVEL_OFFSET = 2
    const val FARM_HP_MULT = 0.65
    const val FARM_ATK_MULT = 0.6
    const val FARM_HEAL_RATIO = 0.08

    const val RARITY_UNCOMMON = 1.25
    const val RARITY_RARE = 1.6
    const val RARITY_EPIC = 2.0
    const val RARITY_LEGENDARY = 2.4
    const val ITEM_GROWTH = 0.08
    const val FUSION_RATIO = 0.75
    const val FUSION_NUMERATOR = 3
    const val FUSION_DENOMINATOR = 4

    const val SHARDS_CAP = 24
    const val RATIONS_CAP = 24
    const val AUTO_RATION_THRESHOLD = 0.30
    const val AUTO_RATION_HEAL = 0.35
    const val DEFEAT_HP_RATIO = 0.6
    const val LEVEL_UP_HEAL = 0.5
    const val BOSS_BLESSING_STEP = 0.25
    const val BOSS_BLESSING_CAP = 0.75

    const val REWARD_MOB_CAP = 6.0
    const val REWARD_BOSS_CAP = 10.0
    const val GOLD_MILESTONE_BONUS = 0.05

    const val BOSS_SHARDS = 3
    const val BOSS_RATIONS = 2
    const val MILESTONE_SHARDS = 8
    const val MOB_RATION_CHANCE = 0.10
    const val ELITE_SHARD_CHANCE = 0.20
    const val CHEST_CHANCE = 0.18
    const val LOOT_EPIC_LEGENDARY_CHANCE = 0.12
    const val LOOT_RARE_LEGENDARY_CHANCE = 0.03
    const val LOOT_EFFECT_CHANCE = 0.35
    const val LOOT_SET_CHANCE = 0.30
    const val LOOT_UNIDENTIFIED_CHANCE = 0.10

    /** Décisions validées le 24/11/2026, à brancher dans les lots suivants. */
    object Target {
        const val XP_GROWTH = 1.09
        const val XP_FLOOR_MULT = 0.12
        const val XP_CATCHUP_LEVEL_GAP = 4
        const val XP_CATCHUP_MULT = 1.25
        const val LOOT_FLOOR_SCALE = 0.055
        const val LOOT_SCALE_CAP = 6.0
        const val ITEM_MAX_LEVELS = 20
        const val ITEM_GROWTH = 0.04
        const val RATIONS_CAP = 30
        const val AUTO_RATION_THRESHOLD = 0.40
        const val AUTO_RATION_HEAL = 0.45
        const val VIT_CAP = 4.0
        const val HERO_MIN_INTERVAL_MS = 180.0
        const val BOSS_LEVEL_OFFSET = 1
        const val SHIELD_REDUCTION = 0.35
        const val BOSS_ENRAGE_AFTER_MS = 25_000.0
        const val BOSS_ENRAGE_STEP = 0.15
        const val BOSS_ENRAGE_CAP = 0.60
        const val PITY_HP_STEP = 0.05
        const val PITY_CAP = 0.25
        const val FUSION_RATIO = 0.90
        const val LOOT_PITY_RARE_KILLS = 5
        const val LOOT_PITY_SET_BOSSES = 10
    }
}
