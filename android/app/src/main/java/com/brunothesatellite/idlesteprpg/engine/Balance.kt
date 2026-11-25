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
        const val MAX_CAMP_LEVEL = 30
        const val CAMP_ATK_PCT = 0.015
        const val CAMP_HP_PCT = 0.015
        const val CAMP_VIT_PCT = 0.01
        const val CAMP_COST_FLOOR_FACTOR = 0.04
        const val RARITY_UNCOMMON = 1.15
        const val RARITY_RARE = 1.5
        const val RARITY_EPIC = 1.9
        const val RARITY_LEGENDARY = 2.4
        const val LIFESTEAL_CAP = 0.25
        const val DAMAGE_REDUCTION_CAP = 0.40
        const val CRIT_CAP = 75.0
        const val CRIT_DAMAGE_CAP = 1.0
        const val BOSS_DAMAGE_CAP = 0.5
        const val RATIONS_CAP = 30
        const val BOSS_RATIONS = 3
        const val CHEST_RATIONS = 3
        const val MOB_RATION_CHANCE = 0.20
        const val ELITE_RATION_GUARANTEED = 1
        const val FARM_HEAL_RATIO = 0.10
        const val FREE_REST_HEAL = 0.20
        const val FREE_REST_PER_DAY = 2
        const val HOLD_FLOOR_SHARDS = 1
        const val HOLD_FLOOR_RATIONS = 1
        const val FARM_BOSS_REWARD_CAP = 5
        const val AUTO_RATION_THRESHOLD = 0.40
        const val AUTO_RATION_HEAL = 0.45
        const val VIT_CAP = 4.0
        const val HERO_MIN_INTERVAL_MS = 180.0
        const val BOSS_LEVEL_OFFSET = 1
        const val SHIELD_REDUCTION = 0.35
        const val SHIELD_DURATION_MS = 3_000.0
        const val SHIELD_PERIOD_MS = 8_000.0
        const val BOSS_ABILITY_INTERVAL_MS = 7_000.0
        /** TTK visé pour un gardien avec le stuff de référence. */
        const val BOSS_TARGET_TTK = 18.0
        /** Diviseurs d'ATQ par capacité : plus la capacité frappe fort, plus le diviseur est grand. */
        const val BOSS_DIV_SHIELD = 14.5
        const val BOSS_DIV_BREATH = 19.0
        const val BOSS_DIV_PRESSURE = 16.0
        const val BOSS_DIV_RUPTURE = 20.5
        const val BOSS_DIV_SLAM = 17.5
        const val BOSS_DIV_DRAIN = 18.0
        const val BOSS_DIV_ROOTS = 18.0
        const val BOSS_DIV_ACID = 17.5
        const val BOSS_DIV_REGEN = 17.0
        const val BOSS_DIV_DEFAULT = 18.0
        const val GATE_METERS = 90.0
        const val BOSS_ENRAGE_AFTER_MS = 25_000.0
        const val BOSS_ENRAGE_STEP = 0.15
        const val BOSS_ENRAGE_CAP = 0.60
        const val ENDGAME_FLOOR = 81
        const val ENDGAME_FINAL_FLOOR = 100
        const val ENDGAME_ENEMY_MULT_START = 1.15
        const val ENDGAME_ENEMY_MULT_END = 1.35
        const val ENDGAME_BOSS_HP_MULT = 1.10
        const val ENDGAME_BOSS_ATK_MULT = 1.05
        const val FINAL_BOSS_HP_MULT = 1.40
        const val FINAL_BOSS_ATK_MULT = 1.15
        const val LEVEL_GAP_FREE = 4
        const val LEVEL_GAP_PCT = 0.05
        const val LEVEL_GAP_CAP = 2.0
        const val PITY_HP_STEP = 0.01
        const val PITY_CAP = 0.03
        const val FUSION_RATIO = 0.90
        const val LOOT_PITY_RARE_KILLS = 5
        const val LOOT_PITY_SET_BOSSES = 10
    }
}
