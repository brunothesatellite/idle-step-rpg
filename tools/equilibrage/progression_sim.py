"""Simulation de progression complete 1 -> 100.

Ce script rejoue une partie entiere (pas, combats, XP, butin, coffres,
equipement, fusion, forge, camp, replis, farm) pour quatre profils de joueur
et trois volumes de pas quotidiens.

Il s'appuie sur `balance_sim.py` pour les formules de combat et les courbes
d'ennemis. Les formules d'objet, d'XP, d'or et de camp sont recopiees de
`Model.kt` / `GameEngine.kt`.

Usage :
    python tools/equilibrage/progression_sim.py --seeds 8
    python tools/equilibrage/progression_sim.py --seeds 12 --profiles passif,normal
"""

from __future__ import annotations

import argparse
import json
import math
import random
import statistics
from dataclasses import dataclass, field, replace
from pathlib import Path
from typing import Iterable

from balance_constants import CURRENT, TARGET
from balance_sim import (
    Build,
    Enemy,
    Rule,
    Tuning,
    _code_boss_atk_scale,
    _code_boss_hp_scale,
    _code_mob_atk_scale,
    _code_mob_hp_scale,
    _depth,
    _early_threat,
    _ref_dps,
    ability_for,
    boss_variants,
    make_boss,
    modifier_pool,
    rule_for,
    simulate,
    simulate_mixed,
    xp_next,
)

STEPS_PER_METER = 10.0
FLOOR_METERS = 100.0
GATE_METERS = 90.0
GEAR_SCALING = False
MAX_STEPS_PER_DAY = 60_000
MAX_RATIONS = int(TARGET["RATIONS_CAP"])
MAX_SHARDS = int(CURRENT["SHARDS_CAP"])
MAX_CAMP_LEVEL = int(TARGET["MAX_CAMP_LEVEL"])
LEVEL_UP_HEAL = CURRENT["LEVEL_UP_HEAL"]
BOSS_VICTORY_HP_CAP = TARGET["BOSS_VICTORY_HP_CAP"]
DEFEAT_XP_RATIO = TARGET["DEFEAT_XP_RATIO"]
AUTO_RATION_THRESHOLD = TARGET["AUTO_RATION_THRESHOLD"]
AUTO_RATION_HEAL = TARGET["AUTO_RATION_HEAL"]
FARM_HEAL_RATIO = TARGET["FARM_HEAL_RATIO"]
FARM_HP_MULT = CURRENT["FARM_HP_MULT"]
FARM_ATK_MULT = CURRENT["FARM_ATK_MULT"]
XP_FLOOR_MULT = TARGET["XP_FLOOR_MULT"]
XP_CATCHUP_LEVEL_GAP = TARGET["XP_CATCHUP_LEVEL_GAP"]
XP_CATCHUP_MULT = TARGET["XP_CATCHUP_MULT"]
FUSION_RATIO = TARGET["FUSION_RATIO"]
ITEM_GROWTH = TARGET["ITEM_GROWTH"]
ITEM_MAX_LEVELS = int(TARGET["ITEM_MAX_LEVELS"])
RARITY_MULT = {
    "common": 1.0,
    "uncommon": TARGET["RARITY_UNCOMMON"],
    "rare": TARGET["RARITY_RARE"],
    "epic": TARGET["RARITY_EPIC"],
    "legendary": TARGET["RARITY_LEGENDARY"],
}
RARITY_RANK = {"common": 1, "uncommon": 2, "rare": 3, "epic": 4, "legendary": 5}
RARITY_NEXT = {"common": "uncommon", "uncommon": "rare", "rare": "epic", "epic": "legendary"}
SLOTS = ("weapon", "head", "body", "hands", "feet", "talisman")
EFFECTS = ("vampirisme", "rempart", "frenesie", "chasse", "vitalite", "precision")
SET_NAMES = {
    "crypts": ("veilleur", "os"),
    "catacombs": ("os", "toile"),
    "ash": ("braise", "cendre"),
    "forge": ("forge",),
    "peaks": ("givre", "serment"),
    "woods": ("bois",),
    "abyss": ("abysse",),
    "throne": ("trone",),
}


def biome_key(floor: int) -> str:
    if floor <= 10:
        return "crypts"
    if floor <= 20:
        return "catacombs"
    if floor <= 30:
        return "ash"
    if floor <= 40:
        return "forge"
    if floor <= 50:
        return "peaks"
    if floor <= 60:
        return "woods"
    if floor <= 70:
        return "abyss"
    return "throne"


# ---------------------------------------------------------------------------
# Modeles d'objet
# ---------------------------------------------------------------------------


@dataclass
class LootTemplate:
    key: str
    name: str
    slot: str
    atk: int = 0
    hp: int = 0
    vit: int = 0
    crit: int = 0
    regen: float = 0.0
    value: int = 1


GENERIC = [
    LootTemplate("loot_sword", "Lame runique", "weapon", atk=12, value=90),
    LootTemplate("loot_axe", "Hache de guerre", "weapon", atk=10, crit=2, value=65),
    LootTemplate("loot_mace", "Masse d'armes", "weapon", atk=9, crit=3, value=75),
    LootTemplate("loot_spear", "Lance du guet", "weapon", atk=8, value=35),
    LootTemplate("loot_mail", "Cotte de mailles", "body", hp=34, vit=2, value=70),
    LootTemplate("loot_plate", "Plate du gardien", "body", hp=52, value=110),
    LootTemplate("loot_helm", "Heaume grave", "head", hp=22, crit=3, value=80),
    LootTemplate("loot_glove", "Gants de braise", "hands", atk=6, value=55),
    LootTemplate("loot_boots", "Bottes de mousse", "feet", vit=5, value=55),
    LootTemplate("loot_ring", "Anneau d'ame", "talisman", regen=0.4, crit=2, value=140),
    LootTemplate("loot_idol", "Idole doree", "talisman", atk=4, value=180),
    LootTemplate("loot_art_helm", "Heaume d'art", "head", hp=24, crit=4, value=125),
    LootTemplate("loot_reliquary", "Coffret-reliquaire", "talisman", hp=18, regen=0.3, value=110),
    LootTemplate("loot_lantern", "Lanterne des ames", "talisman", atk=6, regen=0.4, value=145),
]
BIOME_LOOT = {
    "crypts": [
        LootTemplate("crypt_blade", "Lame d'ossuaire", "weapon", atk=11, crit=1, value=58),
        LootTemplate("crypt_shroud", "Suaire du veilleur", "body", hp=30, vit=2, value=72),
        LootTemplate("crypt_helm", "Heaume des cryptes", "head", hp=20, crit=3, value=76),
        LootTemplate("crypt_ward", "Amulette de garde", "talisman", hp=26, regen=0.3, value=135),
    ],
    "catacombs": [
        LootTemplate("cat_axe", "Hache gnoll", "weapon", atk=10, crit=2, value=62),
        LootTemplate("cat_shell", "Carapace de coleoptere", "body", hp=38, vit=2, value=80),
        LootTemplate("cat_boots", "Bottes de soie", "feet", vit=7, value=68),
        LootTemplate("cat_mask", "Masque de gnoll", "head", hp=18, crit=5, value=120),
    ],
    "ash": [
        LootTemplate("ash_blade", "Lame de braise", "weapon", atk=14, crit=3, value=92),
        LootTemplate("ash_plate", "Plate de cendre", "body", hp=48, atk=4, value=140),
        LootTemplate("ash_boots", "Bottes de limier", "feet", vit=6, hp=8, value=76),
        LootTemplate("ash_ring", "Anneau de soufre", "talisman", atk=9, crit=3, value=130),
    ],
    "forge": [
        LootTemplate("forge_hammer", "Marteau de forge", "weapon", atk=16, crit=2, value=98),
        LootTemplate("forge_plate", "Harnois de forge", "body", hp=55, vit=1, value=150),
        LootTemplate("forge_gauntlets", "Gantelets de fer", "hands", atk=8, hp=10, value=80),
        LootTemplate("forge_ring", "Anneau de vif-argent", "talisman", hp=30, regen=0.4, value=190),
    ],
    "peaks": [
        LootTemplate("peak_blade", "Lame de givre", "weapon", atk=13, crit=4, value=94),
        LootTemplate("peak_mail", "Cotte de glace", "body", hp=42, vit=4, value=145),
        LootTemplate("peak_boots", "Bottes des cimes", "feet", vit=7, regen=0.2, value=78),
        LootTemplate("peak_ring", "Anneau d'orage", "talisman", atk=10, crit=4, value=195),
    ],
    "woods": [
        LootTemplate("wood_staff", "Baton de dryade", "weapon", atk=12, regen=0.3, value=90),
        LootTemplate("wood_mail", "Armure d'ecorce", "body", hp=36, vit=3, value=84),
        LootTemplate("wood_boots", "Bottes de feuilles", "feet", vit=6, value=64),
        LootTemplate("wood_horn", "Corne de satyre", "talisman", atk=7, vit=4, value=125),
    ],
    "abyss": [
        LootTemplate("abyss_whip", "Fouet de tentacule", "weapon", atk=15, crit=3, value=130),
        LootTemplate("abyss_plate", "Carapace d'abime", "body", hp=44, atk=5, value=148),
        LootTemplate("abyss_ring", "Anneau de mucilage", "talisman", hp=20, regen=0.4, value=96),
        LootTemplate("abyss_orb", "Orbe du neant", "talisman", atk=12, hp=30, value=200),
    ],
    "throne": [
        LootTemplate("throne_blade", "Lame du trone", "weapon", atk=20, crit=5, value=220),
        LootTemplate("throne_mail", "Harnois du trone", "body", hp=70, atk=6, value=230),
        LootTemplate("throne_crown", "Couronne des ames", "head", hp=28, crit=6, value=210),
        LootTemplate("throne_ring", "Anneau des ames", "talisman", atk=14, crit=5, value=240),
    ],
}


@dataclass
class Item:
    slot: str
    name: str
    rarity: str
    level: int
    atk: int = 0
    hp: int = 0
    vit: int = 0
    crit: int = 0
    regen: float = 0.0
    value: int = 0
    effect: str = ""
    set_name: str = ""
    identified: bool = True
    max_level: int = 0
    base_atk: int = 0
    base_hp: int = 0
    base_vit: int = 0
    base_crit: int = 0

    def score(self) -> float:
        effect_bonus = {
            "vampirisme": 30,
            "rempart": 38,
            "frenesie": 30,
            "chasse": 26,
            "vitalite": 34,
            "precision": 30,
        }.get(self.effect, 0)
        return (
            self.atk * 2
            + self.hp * 0.6
            + self.vit * 3
            + self.crit * 4
            + self.regen * 20
            + effect_bonus
        )


def templates_for(floor: int) -> list[LootTemplate]:
    return GENERIC + BIOME_LOOT[biome_key(floor)]


def loot_item(floor: int, rarity: str, rng: random.Random, hero_level: int, force_set: bool = False) -> Item:
    final = rarity
    if rarity == "epic" and rng.random() < CURRENT["LOOT_EPIC_LEGENDARY_CHANCE"]:
        final = "legendary"
    if rarity == "rare" and rng.random() < CURRENT["LOOT_RARE_LEGENDARY_CHANCE"]:
        final = "legendary"
    tpl = rng.choice(templates_for(floor))
    mult = RARITY_MULT[final]
    loot_floor = min(floor + 10, max(floor, hero_level - 10))
    scale = min(TARGET["LOOT_SCALE_CAP"], 1.0 + TARGET["LOOT_FLOOR_SCALE"] * max(0, loot_floor - 1))

    def scaled(v: int) -> int:
        return max(1, round(v * mult * scale)) if v > 0 else 0

    effect = ""
    if final == "legendary":
        effect = rng.choice(EFFECTS)
    elif final in ("epic", "rare") and rng.random() < CURRENT["LOOT_EFFECT_CHANCE"]:
        effect = rng.choice(EFFECTS)
    set_name = ""
    if force_set or (
        final in ("rare", "epic", "legendary")
        and rng.random() < CURRENT["LOOT_SET_CHANCE"]
    ):
        set_name = rng.choice(SET_NAMES[biome_key(floor)])
    identified = final not in ("rare", "epic", "legendary") or rng.random() >= CURRENT["LOOT_UNIDENTIFIED_CHANCE"]
    item = Item(
        slot=tpl.slot,
        name=tpl.name + (f" - {effect}" if effect else ""),
        rarity=final,
        level=max(1, loot_floor),
        atk=scaled(tpl.atk),
        hp=scaled(tpl.hp),
        vit=scaled(tpl.vit),
        crit=scaled(tpl.crit),
        regen=round(tpl.regen * mult * scale * 10) / 10.0,
        value=scaled(tpl.value),
        effect=effect,
        set_name=set_name,
        identified=identified,
        max_level=max(1, loot_floor) + ITEM_MAX_LEVELS,
    )
    item.base_atk = item.atk
    item.base_hp = item.hp
    item.base_vit = item.vit
    item.base_crit = item.crit
    return item


# ---------------------------------------------------------------------------
# Statistiques du heros
# ---------------------------------------------------------------------------


@dataclass
class Stats:
    atk: float
    max_hp: float
    vit: float
    crit: float
    crit_damage: float
    regen: float
    lifesteal: float
    damage_reduction: float
    boss_damage: float
    power: float


SET_BONUS = {
    "veilleur": dict(atk=0.04, hp=0.04),
    "braise": dict(crit=3, lifesteal=0.05),
    "givre": dict(vit=0.05, regen=0.3),
    "os": dict(atk=0.04, hp=0.04),
    "toile": dict(vit=0.03, crit=2),
    "cendre": dict(atk=0.04, crit=2),
    "forge": dict(hp=0.04, dr=0.03),
    "serment": dict(atk=0.04, boss_damage=0.08),
    "bois": dict(vit=0.04, regen=0.35),
    "abysse": dict(hp=0.04, lifesteal=0.04),
    "trone": dict(atk=0.04, crit_damage=0.15),
}


@dataclass
class Hero:
    level: int = 1
    xp: int = 0
    hp: float = 150.0
    gold: int = 0
    shards: int = 0
    rations: int = 0
    upgrades: dict[str, int] = field(default_factory=lambda: {"blade": 0, "breath": 0, "swift": 0})
    equipment: dict[str, Item] = field(default_factory=dict)
    pool: list[Item] = field(default_factory=list)
    potions: int = 0
    steps: int = 0
    tenacity: int = 0

    def raw_stats(self) -> Stats:
        milestone = 0  # filled by stats_for_floor
        return self._stats(0)

    def _stats(self, floor: int) -> Stats:
        milestone = floor // 10
        base_atk = CURRENT["HERO_ATK_BASE"] + CURRENT["HERO_ATK_PER_LEVEL"] * (self.level - 1) + CURRENT["HERO_ATK_PER_MILESTONE"] * milestone
        base_hp = CURRENT["HERO_HP_BASE"] + CURRENT["HERO_HP_PER_LEVEL"] * (self.level - 1) + CURRENT["HERO_HP_PER_MILESTONE"] * milestone
        base_vit = CURRENT["HERO_VIT_BASE"] + CURRENT["HERO_VIT_PER_LEVEL"] * (self.level - 1)
        atk = base_atk * (1.0 + TARGET["CAMP_ATK_PCT"] * self.upgrades["blade"])
        max_hp = base_hp * (1.0 + TARGET["CAMP_HP_PCT"] * self.upgrades["breath"])
        vit = base_vit * (1.0 + TARGET["CAMP_VIT_PCT"] * self.upgrades["swift"])
        crit = CURRENT["HERO_CRIT_BASE"]
        regen = CURRENT["HERO_REGEN_BASE"]
        lifesteal = 0.0
        dr = 0.0
        crit_damage = 0.0
        boss_damage = 0.0
        set_counts: dict[str, int] = {}
        for item in self.equipment.values():
            atk += item.atk
            max_hp += item.hp
            vit += item.vit / 100.0
            crit += item.crit
            regen += item.regen
            if item.effect == "vampirisme":
                lifesteal += 0.08
            elif item.effect == "rempart":
                dr += 0.10
            elif item.effect == "frenesie":
                crit_damage += 0.40
            elif item.effect == "chasse":
                boss_damage += 0.25
            elif item.effect == "vitalite":
                regen += 1.2
            elif item.effect == "precision":
                crit += 5
            if item.set_name:
                set_counts[item.set_name] = set_counts.get(item.set_name, 0) + 1
        for set_name, count in set_counts.items():
            if count < 2:
                continue
            tier = 3 if count >= 6 else 2 if count >= 4 else 1
            bonus = SET_BONUS.get(set_name, {})
            atk *= 1.0 + bonus.get("atk", 0.0) * tier
            max_hp *= 1.0 + bonus.get("hp", 0.0) * tier
            vit += bonus.get("vit", 0.0) * tier
            crit += bonus.get("crit", 0) * tier
            regen += bonus.get("regen", 0.0) * tier
            lifesteal += bonus.get("lifesteal", 0.0) * tier
            dr += bonus.get("dr", 0.0) * tier
            boss_damage += bonus.get("boss_damage", 0.0) * tier
            crit_damage += bonus.get("crit_damage", 0.0) * tier
        vit = min(vit, TARGET["VIT_CAP"])
        crit = min(crit, TARGET["CRIT_CAP"])
        lifesteal = min(lifesteal, TARGET["LIFESTEAL_CAP"])
        crit_damage = min(crit_damage, TARGET["CRIT_DAMAGE_CAP"])
        boss_damage = min(boss_damage, TARGET["BOSS_DAMAGE_CAP"])
        dr = min(max(dr, 0.0), TARGET["DAMAGE_REDUCTION_CAP"])
        tenacity = 1.0 + 0.05 * min(5, self.tenacity)
        atk *= tenacity
        max_hp *= tenacity
        power = atk * 2 + max_hp * 0.2 + vit * 100 + crit * 4 + regen * 20 + lifesteal * 500 + dr * 500
        return Stats(atk, max_hp, vit, crit, crit_damage, regen, lifesteal, dr, boss_damage, power)

    def stats(self, floor: int) -> Stats:
        s = self._stats(floor)
        rule = rule_for(floor, "auto")
        atk = s.atk * (1.0 + rule.hero_atk)
        max_hp = s.max_hp * (1.0 + rule.hero_hp)
        vit = min(s.vit * (1.0 + rule.hero_vit), TARGET["VIT_CAP"])
        crit = min(s.crit + rule.hero_crit, TARGET["CRIT_CAP"])
        regen = s.regen + rule.hero_regen
        dr = min(s.damage_reduction + rule.hero_dr, TARGET["DAMAGE_REDUCTION_CAP"])
        boss_damage = min(s.boss_damage + rule.boss_damage, TARGET["BOSS_DAMAGE_CAP"])
        return Stats(atk, max_hp, vit, crit, s.crit_damage, regen, s.lifesteal, dr, boss_damage, s.power)

    def build(self, floor: int) -> Build:
        s = self._stats(floor)
        return Build(
            name="hero",
            atk=s.atk,
            hp=s.max_hp,
            vit=s.vit,
            crit=s.crit,
            crit_damage=s.crit_damage,
            regen=s.regen,
            lifesteal=s.lifesteal,
            damage_reduction=s.damage_reduction,
            boss_damage=s.boss_damage,
            ferveur=100.0,
            rations=1,
        )


def default_equipment() -> dict[str, Item]:
    return {
        "weapon": Item("weapon", "Lame du veilleur", "rare", 4, atk=9, crit=3, value=64, max_level=24, base_atk=9),
        "head": Item("head", "Heaume de fer", "uncommon", 3, hp=18, value=40, max_level=23, base_hp=18),
        "body": Item("body", "Cotte de mailles", "rare", 4, hp=36, vit=4, value=88, max_level=24, base_hp=36, base_vit=4),
        "hands": Item("hands", "Gantelets de braise", "uncommon", 3, atk=5, value=42, max_level=23, base_atk=5),
        "feet": Item("feet", "Bottes de mousse", "uncommon", 3, vit=4, value=40, max_level=23, base_vit=4),
        "talisman": Item("talisman", "Anneau de regeneration", "rare", 4, regen=0.6, crit=2, value=76, max_level=24),
    }


# ---------------------------------------------------------------------------
# Profil et decisions
# ---------------------------------------------------------------------------


@dataclass(frozen=True)
class Profile:
    key: str
    label: str
    equip: str              # none, greedy, smart, oracle
    fuse: str               # none, light, normal, heavy
    upgrade: str            # none, light, normal, max
    camp: str               # none, light, normal, max
    chests: bool
    specials: str           # none, safe, smart
    boss_threshold: float
    farm_mobs: int
    farm_boss: int
    preheal: bool
    potions: str            # none, situational, aggressive
    protect_sets: bool = False


PROFILES = {
    "passif": Profile(
        "passif", "Passif minimal", "greedy", "none", "none", "none",
        chests=False, specials="none", boss_threshold=0.0, farm_mobs=0,
        farm_boss=0, preheal=False, potions="none",
    ),
    "passif_strict": Profile(
        "passif_strict", "Passif strict (aucune action)", "none", "none", "none", "none",
        chests=False, specials="none", boss_threshold=0.0, farm_mobs=0,
        farm_boss=0, preheal=False, potions="none",
    ),
    "passif_auto": Profile(
        "passif_auto", "Passif + auto-optimisation", "smart", "normal", "normal", "light",
        chests=False, specials="none", boss_threshold=0.0, farm_mobs=0,
        farm_boss=0, preheal=False, potions="none",
    ),
    "passif_auto_heal": Profile(
        "passif_auto_heal", "Passif + auto-optimisation + soins", "smart", "normal", "normal", "light",
        chests=False, specials="none", boss_threshold=0.0, farm_mobs=0,
        farm_boss=0, preheal=True, potions="none",
    ),
    "passif_heal": Profile(
        "passif_heal", "Passif + soins", "greedy", "none", "none", "none",
        chests=False, specials="none", boss_threshold=0.0, farm_mobs=0,
        farm_boss=0, preheal=True, potions="none",
    ),
    "passif_plus": Profile(
        "passif_plus", "Passif + coffres + auto-optimisation", "smart", "normal", "normal", "light",
        chests=True, specials="safe", boss_threshold=0.0, farm_mobs=0,
        farm_boss=0, preheal=True, potions="none",
    ),
    "passif_auto_game": Profile(
        "passif_auto_game", "Passif + auto-optimisation du jeu", "greedy", "normal", "max", "max",
        chests=True, specials="safe", boss_threshold=0.0, farm_mobs=0,
        farm_boss=0, preheal=True, potions="none", protect_sets=True,
    ),
    "normal": Profile(
        "normal", "Normal", "greedy", "light", "light", "light",
        chests=True, specials="safe", boss_threshold=0.35, farm_mobs=5,
        farm_boss=0, preheal=True, potions="situational",
    ),
    "optimise": Profile(
        "optimise", "Optimise", "smart", "normal", "normal", "normal",
        chests=True, specials="smart", boss_threshold=0.55, farm_mobs=10,
        farm_boss=1, preheal=True, potions="situational",
    ),
    "hyper": Profile(
        "hyper", "Hyper-optimise", "oracle", "heavy", "max", "max",
        chests=True, specials="smart", boss_threshold=0.75, farm_mobs=15,
        farm_boss=3, preheal=True, potions="aggressive",
    ),
}


# ---------------------------------------------------------------------------
# Outils de combat
# ---------------------------------------------------------------------------


def hero_xp_next(level: int) -> int:
    return round(CURRENT["XP_BASE"] * (TARGET["XP_GROWTH"] ** (level - 1)))


def xp_floor_mult(floor: int) -> float:
    return 1.0 + XP_FLOOR_MULT * max(0, floor - 1)


def catchup_mult(level: int, floor: int) -> float:
    return XP_CATCHUP_MULT if level < floor - XP_CATCHUP_LEVEL_GAP else 1.0


def mob_xp(level: int, floor: int, hero_level: int, elite: bool) -> int:
    return round((CURRENT["XP_MOB_BASE"] + CURRENT["XP_MOB_PER_LEVEL"] * level)
                 * (CURRENT["XP_ELITE_MULT"] if elite else 1.0)
                 * xp_floor_mult(floor) * catchup_mult(hero_level, floor))


def boss_xp(level: int, floor: int, hero_level: int) -> int:
    return round(CURRENT["XP_BOSS_MULT"] * (CURRENT["XP_MOB_BASE"] + CURRENT["XP_MOB_PER_LEVEL"] * level)
                 * xp_floor_mult(floor) * catchup_mult(hero_level, floor))


def milestone_mult(floor: int) -> float:
    return 1.0 + CURRENT["GOLD_MILESTONE_BONUS"] * (floor // 10)


def mob_gold(level: int, floor: int, elite: bool) -> int:
    reward = min(CURRENT["REWARD_MOB_CAP"], math.sqrt(_depth(floor)))
    return round((6 + 3 * level) * (1.6 if elite else 1.0) * reward * milestone_mult(floor))


def boss_gold(level: int, floor: int) -> int:
    reward = min(CURRENT["REWARD_BOSS_CAP"], math.sqrt(_depth(floor, boss=True)))
    return round(12 * (6 + 3 * level) * reward * milestone_mult(floor))


def gap_mult(level: int, floor: int) -> float:
    gap = max(0, level - floor - int(TARGET["LEVEL_GAP_FREE"]))
    return min(TARGET["LEVEL_GAP_CAP"], 1.0 + gap * TARGET["LEVEL_GAP_PCT"])


def make_tier_enemy(floor: int, tier: str, elite: bool, gap: float) -> Enemy:
    rule = rule_for(floor, "auto")
    offset = -1 if tier == "easy" else 0 if tier == "mid" else 1
    level = max(1, floor + offset + (2 if elite else 0))
    hp = (CURRENT["MOB_HP_BASE"] + CURRENT["MOB_HP_PER_LEVEL"] * (level - 1))
    hp *= CURRENT["ELITE_HP_MULT"] if elite else 1.0
    hp *= _code_mob_hp_scale(floor) * rule.enemy_hp * gap
    hp *= _early_threat(floor, TARGET["EARLY_HP_MAX"])
    atk = (CURRENT["MOB_ATK_BASE"] + CURRENT["MOB_ATK_PER_LEVEL"] * (level - 1))
    atk *= CURRENT["ELITE_ATK_MULT"] if elite else 1.0
    atk *= _code_mob_atk_scale(floor) * rule.enemy_atk * gap
    atk *= _early_threat(floor, TARGET["EARLY_ATK_MAX"])
    interval = (CURRENT["ELITE_INTERVAL_MS"] if elite else CURRENT["MOB_INTERVAL_MS"]) / 1000.0 * rule.enemy_interval
    return Enemy(
        name=f"{tier}{' elite' if elite else ''}",
        hp=hp,
        atk=atk,
        interval=interval,
        xp=mob_xp(level, floor, floor, elite),
        gold=mob_gold(level, floor, elite),
    )


def make_farm_enemy(floor: int, gap: float) -> Enemy:
    rule = rule_for(floor, "auto")
    level = max(1, floor - int(CURRENT["FARM_LEVEL_OFFSET"]))
    hp = (CURRENT["MOB_HP_BASE"] + CURRENT["MOB_HP_PER_LEVEL"] * (level - 1))
    hp *= FARM_HP_MULT * rule.enemy_hp * gap * _early_threat(floor, TARGET["EARLY_HP_MAX"])
    atk = (CURRENT["MOB_ATK_BASE"] + CURRENT["MOB_ATK_PER_LEVEL"] * (level - 1))
    atk *= FARM_ATK_MULT * rule.enemy_atk * gap * _early_threat(floor, TARGET["EARLY_ATK_MAX"])
    interval = CURRENT["MOB_INTERVAL_MS"] / 1000.0 * rule.enemy_interval
    return Enemy(
        name="farm",
        hp=hp,
        atk=atk,
        interval=interval,
        xp=round(mob_xp(level, floor, floor, False) * CURRENT["FARM_XP_MULT"]),
        gold=round((6 + 3 * level) * 0.5 * milestone_mult(floor)),
    )


def expected_multiplier(stats: Stats) -> float:
    crit_chance = min(TARGET["CRIT_CAP"] / 100.0, (stats.crit + 2.0) / 100.0)
    return 1.0 + crit_chance * (0.6 + stats.crit_damage)


def hero_interval(stats: Stats) -> float:
    return max(TARGET["HERO_MIN_INTERVAL_MS"] / 1000.0, 1.0 / (stats.vit * 1.4))


def combat_metrics(hero: Hero, floor: int, enemy: Enemy, boss: bool = False) -> tuple[float, float, float]:
    """Retourne (TTK, degats nets attendus, dps heros)."""
    s = hero.stats(floor)
    interval = hero_interval(s)
    dps = s.atk * expected_multiplier(s) / interval
    if boss:
        dps *= 1.0 + s.boss_damage
    ttk = max(0.1, enemy.hp / dps)
    enemy_dps = enemy.atk * (1.0 - s.damage_reduction) / enemy.interval
    regen = (s.max_hp * 0.006 * 1.8 + max(0.0, s.regen - 0.6)) / 3.0
    lifesteal = min(s.lifesteal * dps, TARGET["LIFESTEAL_PER_SECOND_CAP"] * s.max_hp)
    net = max(0.0, enemy_dps - regen - lifesteal)
    return ttk, net * ttk, dps


def fight_normal(hero: Hero, floor: int, tier: str, rng: random.Random) -> dict:
    s = hero.stats(floor)
    hero.hp = min(hero.hp, s.max_hp)
    gap = gap_mult(hero.level, floor)
    elite = rng.random() < CURRENT["ELITE_CHANCE"]
    enemy = make_tier_enemy(floor, tier, elite, gap)
    level = max(1, floor + (-1 if tier == "easy" else 0 if tier == "mid" else 1) + (2 if elite else 0))
    enemy = replace(enemy, xp=mob_xp(level, floor, hero.level, elite))
    ttk, dmg, dps = combat_metrics(hero, floor, enemy, boss=False)
    # variance legere pour eviter les combats parfaitement deterministes
    dmg *= rng.uniform(0.9, 1.1)
    ttk *= rng.uniform(0.95, 1.05)
    used_ration = False
    heal = 0.0
    if hero.hp - dmg <= AUTO_RATION_THRESHOLD * s.max_hp and hero.rations > 0:
        used_ration = True
        hero.rations -= 1
        heal = AUTO_RATION_HEAL * s.max_hp
    lethal = hero.hp + heal <= dmg
    if not lethal:
        hero.hp = min(s.max_hp, hero.hp - dmg + heal)
    level = enemy.name
    if lethal:
        return {"win": False, "ttk": ttk, "damage": dmg, "ration": used_ration, "enemy": enemy, "elite": elite, "dps": dps}
    # regen de marche entre deux rencontres
    hero.hp = min(s.max_hp, hero.hp + (s.max_hp * 0.006 * 1.8 + max(0.0, s.regen - 0.6)) * 1.8)
    return {"win": True, "ttk": ttk, "damage": dmg, "ration": used_ration, "enemy": enemy, "elite": elite, "dps": dps}


def stats_power_score(s: Stats, floor: int) -> float:
    crit_chance = min(0.75, (s.crit + 2.0) / 100.0)
    interval = max(0.18, 1.0 / (s.vit * 1.4))
    dps = s.atk * (1.0 + crit_chance * (0.6 + s.crit_damage)) / interval
    dps *= 1.0 + s.boss_damage * 0.5
    ehp = s.max_hp * (1.0 + s.damage_reduction) + 0.45 * s.max_hp
    heal = min(s.lifesteal * dps, 0.03 * s.max_hp) + s.regen
    return (dps ** 0.55) * ((ehp + heal * 12.0) ** 0.45)


def reference_stats(floor: int) -> Stats:
    rule = rule_for(floor, "auto")
    atk = (650.0 + 20.0 * floor) * (1.0 + rule.hero_atk)
    hp = (1800.0 + 55.0 * floor) * (1.0 + rule.hero_hp)
    vit = min(TARGET["VIT_CAP"], (1.8 + 0.02 * floor) * (1.0 + rule.hero_vit))
    crit = min(TARGET["CRIT_CAP"], 30.0 + rule.hero_crit)
    dr = min(TARGET["DAMAGE_REDUCTION_CAP"], 0.06 + rule.hero_dr)
    return Stats(atk, hp, vit, crit, 0.0, 1.0 + rule.hero_regen, 0.0, dr, rule.boss_damage, 0.0)


def gear_scale(hero: Hero, floor: int) -> float:
    """Mercy/challenge de gardien base sur la puissance reelle du heros."""
    hero_score = stats_power_score(hero.stats(floor), floor)
    ref_score = stats_power_score(reference_stats(floor), floor)
    ratio = hero_score / max(ref_score, 1.0)
    if ratio < 1.0:
        return max(0.55, ratio)
    return min(2.20, 1.0 + 0.90 * max(0.0, ratio - 1.15))


def final_boss_gear_hp(hero: Hero, floor: int = 100) -> float:
    s = hero.stats(floor)
    interval = max(0.18, 1.0 / (s.vit * 1.4))
    crit = min(0.75, (s.crit + 2.0) / 100.0)
    dps = s.atk * (1.0 + crit * (0.6 + s.crit_damage)) / interval * (1.0 + s.boss_damage)
    ref = _ref_dps(floor, rule_for(floor, "auto"))
    ratio = min(4.0, max(0.35, dps / max(ref, 1.0)))
    return min(3.5, max(0.75, ratio ** 0.85))


def boss_estimate(hero: Hero, floor: int) -> float:
    """Estimation rapide de la marge de victoire (0 = perdu, 1 = confortable)."""
    rule = rule_for(floor, "auto")
    ability = ability_for(floor, "auto")
    enemy = make_boss(floor, "code", "auto", ability, "", 0.0, Tuning())
    gap = gap_mult(hero.level, floor)
    enemy = replace(enemy, hp=enemy.hp * gap, atk=enemy.atk * gap)
    if GEAR_SCALING:
        scale = gear_scale(hero, floor)
        enemy = replace(enemy, hp=enemy.hp * scale, atk=enemy.atk * scale)
    if floor >= int(TARGET["ENDGAME_FINAL_FLOOR"]):
        enemy = replace(
            enemy,
            hp=enemy.hp * final_boss_gear_hp(hero, floor),
            atk=enemy.atk * 1.08,
            ability_period=6.0,
        )
    s = hero.stats(floor)
    interval = hero_interval(s)
    dps = s.atk * expected_multiplier(s) / interval * (1.0 + s.boss_damage)
    ttk = enemy.hp / dps
    enemy_dps = enemy.atk * (1.0 - s.damage_reduction) / enemy.interval
    regen = (s.max_hp * 0.006 * 1.8 + max(0.0, s.regen - 0.6)) / 3.0
    lifesteal = min(s.lifesteal * dps, TARGET["LIFESTEAL_PER_SECOND_CAP"] * s.max_hp)
    net = max(0.0001, enemy_dps - regen - lifesteal)
    ehp = s.max_hp + AUTO_RATION_HEAL * s.max_hp + (regen + lifesteal) * ttk
    ttd = ehp / net
    ratio = ttd / max(ttk, 0.1)
    # ajustement capacites lourdes
    if ability == "shield":
        ratio *= 0.82
    elif ability in ("breath", "slam", "rupture"):
        ratio *= 0.88
    elif ability == "regen":
        ratio *= 0.90
    elif ability == "drain":
        ratio *= 0.90
    return min(0.98, max(0.0, (ratio - 0.65) / 0.85))


# ---------------------------------------------------------------------------
# Gestion equipement / or / camp
# ---------------------------------------------------------------------------


def item_score(item: Item) -> float:
    return item.score()


def apply_item(item: Item, stats: Stats) -> Stats:
    atk = stats.atk + item.atk
    hp = stats.max_hp + item.hp
    vit = stats.vit + item.vit / 100.0
    crit = stats.crit + item.crit
    regen = stats.regen + item.regen
    lifesteal = stats.lifesteal
    dr = stats.damage_reduction
    crit_damage = stats.crit_damage
    boss_damage = stats.boss_damage
    if item.effect == "vampirisme":
        lifesteal += 0.08
    elif item.effect == "rempart":
        dr += 0.10
    elif item.effect == "frenesie":
        crit_damage += 0.40
    elif item.effect == "chasse":
        boss_damage += 0.25
    elif item.effect == "vitalite":
        regen += 1.2
    elif item.effect == "precision":
        crit += 5
    return Stats(atk, hp, vit, crit, crit_damage, regen, lifesteal, dr, boss_damage, stats.power)


def loadout_stats(hero: Hero, floor: int, items: Iterable[Item]) -> Stats:
    """Stats brutes (sans regle de biome) d'un ensemble d'objets."""
    base = Hero(level=hero.level, upgrades=dict(hero.upgrades))
    base.equipment = {}
    s = base._stats(floor)
    for item in items:
        s = apply_item(item, s)
    # set bonuses
    counts: dict[str, int] = {}
    for item in items:
        if item.set_name:
            counts[item.set_name] = counts.get(item.set_name, 0) + 1
    atk, hp, vit, crit, regen = s.atk, s.max_hp, s.vit, s.crit, s.regen
    lifesteal, dr, crit_damage, boss_damage = s.lifesteal, s.damage_reduction, s.crit_damage, s.boss_damage
    for set_name, count in counts.items():
        if count < 2:
            continue
        tier = 3 if count >= 6 else 2 if count >= 4 else 1
        bonus = SET_BONUS.get(set_name, {})
        atk *= 1.0 + bonus.get("atk", 0.0) * tier
        hp *= 1.0 + bonus.get("hp", 0.0) * tier
        vit += bonus.get("vit", 0.0) * tier
        crit += bonus.get("crit", 0) * tier
        regen += bonus.get("regen", 0.0) * tier
        lifesteal += bonus.get("lifesteal", 0.0) * tier
        dr += bonus.get("dr", 0.0) * tier
        boss_damage += bonus.get("boss_damage", 0.0) * tier
        crit_damage += bonus.get("crit_damage", 0.0) * tier
    vit = min(vit, TARGET["VIT_CAP"])
    crit = min(crit, TARGET["CRIT_CAP"])
    lifesteal = min(lifesteal, TARGET["LIFESTEAL_CAP"])
    crit_damage = min(crit_damage, TARGET["CRIT_DAMAGE_CAP"])
    boss_damage = min(boss_damage, TARGET["BOSS_DAMAGE_CAP"])
    dr = min(max(dr, 0.0), TARGET["DAMAGE_REDUCTION_CAP"])
    power = atk * 2 + hp * 0.2 + vit * 100 + crit * 4 + regen * 20 + lifesteal * 500 + dr * 500
    return Stats(atk, hp, vit, crit, crit_damage, regen, lifesteal, dr, boss_damage, power)


def loadout_combat_score(hero: Hero, floor: int, items: list[Item], boss_weight: float = 1.0) -> float:
    s = loadout_stats(hero, floor, items)
    rule = rule_for(floor, "auto")
    atk = s.atk * (1.0 + rule.hero_atk)
    max_hp = s.max_hp * (1.0 + rule.hero_hp)
    vit = min(s.vit * (1.0 + rule.hero_vit), TARGET["VIT_CAP"])
    crit = min(s.crit + rule.hero_crit, TARGET["CRIT_CAP"])
    crit_chance = min(0.75, (crit + 2) / 100.0)
    interval = max(0.18, 1.0 / (vit * 1.4))
    dps = atk * (1.0 + crit_chance * (0.6 + s.crit_damage)) / interval
    dps *= 1.0 + s.boss_damage * boss_weight
    ehp = max_hp * (1.0 + s.damage_reduction) + max_hp * AUTO_RATION_HEAL
    lifesteal = min(s.lifesteal * dps, 0.03 * max_hp)
    regen = (max_hp * 0.006 * 1.8 + max(0.0, s.regen - 0.6)) / 3.0
    return (dps ** 0.55) * ((ehp + (lifesteal + regen) * 12.0) ** 0.45)


def candidates_by_slot(hero: Hero) -> dict[str, list[Item]]:
    out: dict[str, list[Item]] = {slot: [] for slot in SLOTS}
    for slot, item in hero.equipment.items():
        out[slot].append(item)
    for item in hero.pool:
        if item.identified:
            out[item.slot].append(item)
    return out


def equip_greedy(hero: Hero, floor: int) -> None:
    by_slot = candidates_by_slot(hero)
    for slot in SLOTS:
        pool = by_slot.get(slot, [])
        if not pool:
            continue
        best = max(pool, key=item_score)
        current = hero.equipment.get(slot)
        if current is None or item_score(best) > item_score(current):
            hero.equipment[slot] = best
    # retire les objets equipes du pool
    equipped_ids = {id(item) for item in hero.equipment.values()}
    hero.pool = [item for item in hero.pool if id(item) not in equipped_ids]


def equip_smart(hero: Hero, floor: int) -> None:
    """Recherche gloutonne avec bonus d'ensemble."""
    by_slot = candidates_by_slot(hero)
    chosen: dict[str, Item] = {}
    for _ in range(3):
        for slot in SLOTS:
            pool = by_slot.get(slot, [])
            if not pool:
                continue
            best = None
            best_score = -1.0
            for item in pool:
                trial = [chosen.get(s, hero.equipment.get(s)) for s in SLOTS]
                trial = [x for x in trial if x is not None]
                trial = [x for x in trial if x.slot != slot] + [item]
                score = loadout_combat_score(hero, floor, trial)
                if score > best_score:
                    best_score = score
                    best = item
            if best is not None:
                chosen[slot] = best
    for slot, item in chosen.items():
        hero.equipment[slot] = item
    equipped_ids = {id(item) for item in hero.equipment.values()}
    hero.pool = [item for item in hero.pool if id(item) not in equipped_ids]


def equip_oracle(hero: Hero, floor: int) -> None:
    """Essaie les 3 meilleurs objets de chaque emplacement (3^6 combinaisons)."""
    by_slot = candidates_by_slot(hero)
    top: dict[str, list[Item]] = {}
    for slot in SLOTS:
        pool = sorted(by_slot.get(slot, []), key=item_score, reverse=True)
        top[slot] = pool[:3]
    best_combo: tuple[Item, ...] | None = None
    best_score = -1.0
    def rec(i: int, acc: list[Item]) -> None:
        nonlocal best_combo, best_score
        if i == len(SLOTS):
            score = loadout_combat_score(hero, floor, acc)
            if score > best_score:
                best_score = score
                best_combo = tuple(acc)
            return
        slot = SLOTS[i]
        options = top.get(slot, [])
        if not options:
            rec(i + 1, acc)
            return
        for item in options:
            acc.append(item)
            rec(i + 1, acc)
            acc.pop()
    rec(0, [])
    if best_combo:
        for item in best_combo:
            hero.equipment[item.slot] = item
    equipped_ids = {id(item) for item in hero.equipment.values()}
    hero.pool = [item for item in hero.pool if id(item) not in equipped_ids]


def fuse_items(hero: Hero, profile: Profile, rng: random.Random) -> None:
    if profile.fuse == "none":
        return
    equipped_ids = {id(item) for item in hero.equipment.values()}
    changed = True
    guard = 0
    while changed and guard < 20:
        changed = False
        guard += 1
        groups: dict[tuple[str, str], list[Item]] = {}
        for item in hero.pool:
            if id(item) in equipped_ids or not item.identified:
                continue
            if item.rarity == "legendary":
                continue
            if profile.protect_sets and item.set_name:
                continue
            groups.setdefault((item.slot, item.rarity), []).append(item)
        for (slot, rarity), group in groups.items():
            if len(group) < 3:
                continue
            group = sorted(group, key=item_score, reverse=(profile.fuse == "heavy"))
            chosen = group[:3]
            next_rarity = RARITY_NEXT[rarity]
            atk = round(sum(i.atk for i in chosen) * FUSION_RATIO)
            hp = round(sum(i.hp for i in chosen) * FUSION_RATIO)
            vit = round(sum(i.vit for i in chosen) * FUSION_RATIO)
            crit = round(sum(i.crit for i in chosen) * FUSION_RATIO)
            regen = round(sum(i.regen for i in chosen) * FUSION_RATIO * 10) / 10.0
            effect = ""
            if next_rarity == "legendary":
                effect = rng.choice(EFFECTS)
            else:
                effect = next((i.effect for i in chosen if i.effect), "")
            set_name = next((i.set_name for i in chosen if i.set_name), "")
            best = max(chosen, key=item_score)
            fused = Item(
                slot=slot,
                name=f"{best.name} fusionne",
                rarity=next_rarity,
                level=max(i.level for i in chosen),
                atk=atk,
                hp=hp,
                vit=vit,
                crit=crit,
                regen=regen,
                value=sum(i.value for i in chosen) + 120,
                effect=effect,
                set_name=set_name,
                identified=True,
                max_level=max(i.level for i in chosen) + ITEM_MAX_LEVELS,
                base_atk=atk,
                base_hp=hp,
                base_vit=vit,
                base_crit=crit,
            )
            for item in chosen:
                hero.pool.remove(item)
            hero.pool.append(fused)
            changed = True
            break


def upgrade_equipment(hero: Hero, profile: Profile) -> None:
    if profile.upgrade == "none":
        return
    limits = {"light": 5, "normal": 10, "max": ITEM_MAX_LEVELS}
    limit = limits[profile.upgrade]
    order = ("weapon", "body", "head", "hands", "feet", "talisman")
    for slot in order:
        item = hero.equipment.get(slot)
        if item is None:
            continue
        drop_level = max(1, item.max_level - ITEM_MAX_LEVELS)
        allowed = min(item.max_level, drop_level + limit)
        while item.level < allowed:
            cost = 30 * item.level + 20
            reserve = 200 if profile.upgrade == "light" else 100
            if hero.gold < cost + reserve or hero.shards < 1:
                break
            hero.gold -= cost
            hero.shards -= 1
            item.atk += max(1, round(item.base_atk * ITEM_GROWTH)) if item.atk > 0 else 0
            item.hp += max(1, round(item.base_hp * ITEM_GROWTH)) if item.hp > 0 else 0
            item.vit += max(1, round(item.base_vit * ITEM_GROWTH)) if item.vit > 0 else 0
            item.crit += max(1, round(item.base_crit * ITEM_GROWTH)) if item.crit > 0 else 0
            if item.regen > 0:
                item.regen = round((item.regen + 0.1) * 10) / 10.0
            item.level += 1
            item.value = round(item.value * 1.2)


def camp_cost(key: str, level: int, floor: int) -> int:
    if key == "blade":
        base, mult = CURRENT["CAMP_BLADE_COST_BASE"], CURRENT["CAMP_BLADE_COST_MULT"]
    elif key == "breath":
        base, mult = CURRENT["CAMP_BREATH_COST_BASE"], CURRENT["CAMP_BREATH_COST_MULT"]
    else:
        base, mult = CURRENT["CAMP_SWIFT_COST_BASE"], CURRENT["CAMP_SWIFT_COST_MULT"]
    factor = 1.0 + TARGET["CAMP_COST_FLOOR_FACTOR"] * max(0, floor - 1)
    return math.ceil(base * (mult ** level) * factor / 5.0) * 5


def buy_camp(hero: Hero, profile: Profile, floor: int) -> None:
    if profile.camp == "none":
        return
    caps = {"light": 12, "normal": 25, "max": 30}
    cap = caps[profile.camp]
    order = ("blade", "breath", "swift") if profile.camp != "light" else ("blade", "breath", "swift")
    for key in order:
        while hero.upgrades[key] < cap:
            level = hero.upgrades[key]
            cost = camp_cost(key, level, floor)
            reserve = 3 * cost if profile.camp == "light" else 1.5 * cost if profile.camp == "normal" else cost
            if hero.gold < cost + reserve:
                break
            hero.gold -= cost
            hero.upgrades[key] += 1


def manage_gear(hero: Hero, profile: Profile, floor: int, rng: random.Random) -> None:
    # identification
    if profile.key != "passif_strict":
        for item in hero.pool:
            if not item.identified and hero.shards > 0:
                item.identified = True
                hero.shards -= 1
    if profile.equip == "greedy":
        equip_greedy(hero, floor)
    elif profile.equip == "smart":
        equip_smart(hero, floor)
    elif profile.equip == "oracle":
        equip_oracle(hero, floor)
    fuse_items(hero, profile, rng)
    # apres fusion, re-equiper
    if profile.equip != "none":
        if profile.equip == "greedy":
            equip_greedy(hero, floor)
        elif profile.equip == "smart":
            equip_smart(hero, floor)
        else:
            equip_oracle(hero, floor)
    upgrade_equipment(hero, profile)
    buy_camp(hero, profile, floor)
    # nettoyage du pool
    keep = 24 if profile.equip != "none" else 12
    hero.pool.sort(key=item_score, reverse=True)
    while len(hero.pool) > keep:
        sold = hero.pool.pop()
        hero.gold += sold.value


# ---------------------------------------------------------------------------
# Progression
# ---------------------------------------------------------------------------


@dataclass
class Snapshot:
    floor: int
    level: int
    days: float
    steps: int
    stats: Stats
    equipment: list[Item]
    upgrades: dict[str, int]
    boss_attempts: int
    retreats: int
    farm_kills: int
    tenacity: int = 0


@dataclass
class RunResult:
    profile: str
    steps_per_day: int
    seed: int
    finished: bool
    finish_floor: int
    final_level: int
    days: float
    steps: int
    kills_normal: int = 0
    kills_boss: int = 0
    farm_kills: int = 0
    retreats: int = 0
    boss_attempts: int = 0
    chests: int = 0
    specials: int = 0
    gold_earned: int = 0
    gold_spent: int = 0
    rations_used: int = 0
    max_floor: int = 1
    level_100_floor: int = 0
    tenacity_max: int = 0
    guardian_farms: int = 0
    preboss_farm_mobs: int = 0
    snapshots: dict[int, Snapshot] = field(default_factory=dict)
    floor_stats: dict[int, dict] = field(default_factory=dict)
    wall_floor: int | None = None


class Progression:
    def __init__(self, profile: Profile, steps_per_day: int, seed: int):
        self.profile = profile
        self.steps_per_day = min(steps_per_day, MAX_STEPS_PER_DAY)
        self.rng = random.Random(seed)
        self.hero = Hero(equipment=default_equipment())
        self.hero.hp = self.hero.stats(1).max_hp
        self.day = 1.0
        self.steps_left = self.steps_per_day
        self.floor = 1
        self.distance = 0.0
        self.next_fight = 10
        self.mode = "walk"
        self.pending_chests: list[str] = []
        self.boss_pity = 0.0
        self.bosses_since_set = 0
        self.mobs_since_rare = 0
        self.special = None
        self.result = RunResult(profile.key, steps_per_day, seed, False, 1, 1, 0.0, 0)
        self.tenacity_stacks = 0
        self.tenacity_floor = 1
        self.hero.tenacity = 0
        self.floor_start_steps = 0
        self.floor_start_power = self.hero.stats(1).power
        self.floor_entry_steps = 0
        self.floor_entry_day = 1.0
        self.daily_rewards()
        manage_gear(self.hero, profile, self.floor, self.rng)

    def daily_rewards(self) -> None:
        steps = self.steps_per_day
        self.hero.rations = min(MAX_RATIONS, self.hero.rations + min(MAX_RATIONS, steps // 200))
        self.hero.shards = min(MAX_SHARDS, self.hero.shards + min(12, steps // 500))
        # repos gratuit : 2 x 20 % par jour pour les profils actifs
        if self.profile.preheal:
            for _ in range(2):
                s = self.hero.stats(self.floor)
                if self.hero.hp < s.max_hp:
                    self.hero.hp = min(s.max_hp, self.hero.hp + TARGET["FREE_REST_HEAL"] * s.max_hp)

    def day_advance(self) -> None:
        self.day += 1
        self.steps_left = self.steps_per_day
        self.daily_rewards()
        # ouvrir les coffres en attente
        while self.pending_chests and self.steps_left >= 150:
            ctype = self.pending_chests[0]
            cost = {"bois": 150, "fer": 400, "runique": 900}[ctype]
            if self.steps_left < cost:
                break
            self.pending_chests.pop(0)
            self.steps_left -= cost
            self.result.steps += cost
            self.open_chest(ctype)

    def on_defeat(self) -> None:
        if self.tenacity_floor != self.floor:
            self.tenacity_stacks = 0
            self.tenacity_floor = self.floor
        self.tenacity_stacks = min(5, self.tenacity_stacks + 1)
        self.hero.tenacity = self.tenacity_stacks
        self.result.tenacity_max = max(self.result.tenacity_max, self.tenacity_stacks)

    def on_guardian_victory(self) -> None:
        self.tenacity_stacks = 0
        self.hero.tenacity = 0
        self.tenacity_floor = self.floor

    def floor_stat(self, floor: int) -> dict:        return self.result.floor_stats.setdefault(
            floor,
            {"attempts": 0, "wins": 0, "retreats": 0, "normal_retreats": 0,
             "farm_kills": 0, "steps": 0, "days": 0.0, "level_in": self.hero.level},
        )

    def xp_gain(self, amount: int) -> None:
        if amount <= 0:
            return
        self.hero.xp += amount
        guard = 0
        while self.hero.xp >= hero_xp_next(self.hero.level) and guard < 30:
            self.hero.xp -= hero_xp_next(self.hero.level)
            self.hero.level += 1
            if self.hero.level >= 100 and self.result.level_100_floor == 0:
                self.result.level_100_floor = self.floor
            s = self.hero.stats(self.floor)
            self.hero.hp = min(s.max_hp, self.hero.hp + LEVEL_UP_HEAL * s.max_hp)
            guard += 1

    def add_loot(self, item: Item) -> None:
        if item.rarity in ("rare", "epic", "legendary") and not item.identified:
            if self.profile.key == "passif_strict":
                return
            if self.hero.shards > 0:
                item.identified = True
                self.hero.shards -= 1
            else:
                return
        self.hero.pool.append(item)

    def open_chest(self, ctype: str) -> None:
        if ctype == "bois":
            gold = self.rng.randint(40, 80)
            rarity = "uncommon"
        elif ctype == "fer":
            gold = self.rng.randint(130, 220)
            rarity = "rare"
        else:
            gold = self.rng.randint(380, 550)
            rarity = "epic"
        gold = round(gold * milestone_mult(self.floor))
        self.hero.gold += gold
        self.result.gold_earned += gold
        self.hero.rations = min(MAX_RATIONS, self.hero.rations + 3)
        self.hero.shards = min(MAX_SHARDS, self.hero.shards + 2)
        self.add_loot(loot_item(self.floor, rarity, self.rng, self.hero.level))
        if self.rng.random() < 0.7:
            self.hero.potions += self.rng.randint(1, 3)
        self.result.chests += 1

    def spawn_chest(self) -> None:
        if len(self.pending_chests) >= 2:
            return
        r = self.rng.random()
        ctype = "bois" if r < 0.60 else "fer" if r < 0.90 else "runique"
        if self.profile.chests:
            cost = {"bois": 150, "fer": 400, "runique": 900}[ctype]
            if self.steps_left >= cost:
                self.steps_left -= cost
                self.result.steps += cost
                self.open_chest(ctype)
            else:
                self.pending_chests.append(ctype)

    def resolve_special(self) -> None:
        self.result.specials += 1
        s = self.hero.stats(self.floor)
        accept = self.profile.specials != "none"
        if not accept:
            return
        kind = self.rng.choice(("autel", "source", "marchand", "statue", "risque"))
        if kind == "autel":
            if self.hero.shards >= 2:
                self.hero.shards -= 2
                self.xp_gain(180)
        elif kind == "source":
            if self.hero.rations >= 1 and self.hero.hp < 0.7 * s.max_hp:
                self.hero.rations -= 1
                self.hero.hp = s.max_hp
        elif kind == "marchand":
            item = loot_item(self.floor, "epic" if self.rng.random() < 0.3 else "rare", self.rng, self.hero.level)
            cost = max(60, item.value * 2)
            if self.hero.gold >= cost and item.score() > item_score(self.hero.equipment.get(item.slot, item)) * 0.9:
                self.hero.gold -= cost
                self.result.gold_spent += cost
                self.add_loot(item)
        elif kind == "risque":
            if self.hero.gold >= 100 and self.profile.specials == "smart":
                self.hero.gold -= 100
                self.result.gold_spent += 100
                if self.rng.random() < 0.45:
                    self.hero.gold += 300
                    self.result.gold_earned += 200
        elif kind == "statue":
            if self.hero.gold >= 300 and self.profile.specials == "smart":
                self.hero.gold -= 100
                self.result.gold_spent += 100
                self.hero.shards = min(MAX_SHARDS, self.hero.shards + 1)

    def heal_before_boss(self) -> None:
        if not self.profile.preheal:
            return
        s = self.hero.stats(self.floor)
        # deux repos gratuits sont deja utilises au debut de journee ; ici on
        # utilise des rations pour remonter a 85 %
        target = 0.95 * s.max_hp
        guard = 0
        while self.hero.hp < target and self.hero.rations > 0 and self.hero.hp < s.max_hp and guard < 5:
            self.hero.rations -= 1
            self.result.rations_used += 1
            self.hero.hp = min(s.max_hp, self.hero.hp + 0.40 * s.max_hp)
            guard += 1

    def farm_recovery(self, gap: float) -> None:
        s = self.hero.stats(self.floor)
        self.hero.hp = s.max_hp * CURRENT["DEFEAT_HP_RATIO"]
        for _ in range(3):
            enemy = make_farm_enemy(self.floor, gap)
            level = max(1, self.floor - int(CURRENT["FARM_LEVEL_OFFSET"]))
            enemy = replace(
                enemy,
                xp=round(mob_xp(level, self.floor, self.hero.level, False) * CURRENT["FARM_XP_MULT"]),
            )
            ttk, dmg, _ = combat_metrics(self.hero, self.floor, enemy)
            # un combat de farm ne peut pas tuer
            self.hero.hp = max(1.0, self.hero.hp - dmg)
            self.xp_gain(enemy.xp)
            self.hero.gold += enemy.gold
            self.result.gold_earned += enemy.gold
            self.result.farm_kills += 1
            self.floor_stat(self.floor)["farm_kills"] += 1
            s = self.hero.stats(self.floor)
            self.hero.hp = min(s.max_hp, self.hero.hp + FARM_HEAL_RATIO * s.max_hp)

    def default_potion(self) -> str:
        if self.profile.potions == "none":
            return ""
        if self.hero.potions <= 0:
            return ""
        if self.profile.potions == "aggressive":
            return "rage" if self.rng.random() < 0.5 else "atk"
        return "atk" if self.rng.random() < 0.35 else ""

    def attempt_boss(self) -> bool:
        self.heal_before_boss()
        s_max = self.hero.stats(self.floor)
        self.hero.hp = min(self.hero.hp, s_max.max_hp)
        self.result.boss_attempts += 1
        fs = self.floor_stat(self.floor)
        fs["attempts"] += 1
        ability = ability_for(self.floor, "auto")
        mod = self.rng.choice(modifier_pool(ability))
        enemy = make_boss(self.floor, "code", "auto", ability, mod, self.boss_pity, Tuning())
        gap = gap_mult(self.hero.level, self.floor)
        enemy = replace(
            enemy,
            hp=enemy.hp * gap,
            atk=enemy.atk * gap,
            xp=boss_xp(self.floor + 1, self.floor, self.hero.level),
            gold=boss_gold(self.floor + 1, self.floor),
        )
        if GEAR_SCALING:
            scale = gear_scale(self.hero, self.floor)
            enemy = replace(enemy, hp=enemy.hp * scale, atk=enemy.atk * scale)
        if self.floor >= int(TARGET["ENDGAME_FINAL_FLOOR"]):
            enemy = replace(
                enemy,
                hp=enemy.hp * final_boss_gear_hp(self.hero, self.floor),
                atk=enemy.atk * 1.08,
                ability_period=6.0,
            )
        build = self.hero.build(self.floor)
        potion = self.default_potion()
        if potion:
            self.hero.potions = max(0, self.hero.potions - 1)
        start_hp = None if self.profile.preheal else self.hero.hp
        res = simulate(
            build,
            enemy,
            runs=1,
            seed=self.rng.randrange(1_000_000_000),
            potion=potion,
            rule=rule_for(self.floor, "auto"),
            start_hp=start_hp,
        )
        win = res.wins > 0
        self.hero.rations = max(0, self.hero.rations - int(res.avg_rations))
        self.result.rations_used += int(res.avg_rations)
        if win:
            self.hero.hp = min(build.hp, res.avg_hp_left)
            self.xp_gain(enemy.xp)
            gold = enemy.gold
            self.hero.gold += gold
            self.result.gold_earned += gold
            self.hero.rations = min(MAX_RATIONS, self.hero.rations + int(TARGET["BOSS_RATIONS"]))
            self.hero.shards = min(MAX_SHARDS, self.hero.shards + int(CURRENT["BOSS_SHARDS"]))
            # butin de gardien
            force_set = self.bosses_since_set >= int(TARGET["LOOT_PITY_SET_BOSSES"])
            item = loot_item(self.floor, "rare", self.rng, self.hero.level, force_set=force_set)
            if item.set_name:
                self.bosses_since_set = 0
            else:
                self.bosses_since_set += 1
            self.add_loot(item)
            if self.rng.random() < 1.0:  # dropConsumables boss = 100 %
                self.hero.potions += self.rng.randint(1, 2)
            self.boss_pity = 0.0
            self.result.kills_boss += 1
            fs["wins"] += 1
            self.on_guardian_victory()
            # plafond post-gardien
            s = self.hero.stats(self.floor)
            self.hero.hp = min(self.hero.hp, BOSS_VICTORY_HP_CAP * s.max_hp)
            return True
        # defaite
        self.result.retreats += 1
        fs["retreats"] += 1
        self.xp_gain(round(enemy.xp * DEFEAT_XP_RATIO))
        self.on_defeat()
        self.boss_pity = min(TARGET["PITY_CAP"], self.boss_pity + TARGET["PITY_HP_STEP"])
        self.farm_recovery(gap)
        self.distance = max(0.0, math.floor((self.distance - 20.0) / 10.0) * 10.0)
        self.next_fight = int(self.distance + 10)
        self.mode = "walk"
        return False

    def do_normal_fight(self, tier: str, farm: bool = False) -> dict:
        if self.profile.preheal:
            s = self.hero.stats(self.floor)
            guard = 0
            while self.hero.hp < 0.80 * s.max_hp and self.hero.rations > 0 and guard < 3:
                self.hero.rations -= 1
                self.result.rations_used += 1
                self.hero.hp = min(s.max_hp, self.hero.hp + 0.40 * s.max_hp)
                guard += 1
        if self.steps_left < 100:
            self.day_advance()
        self.steps_left -= 100
        self.result.steps += 100
        out = fight_normal(self.hero, self.floor, tier, self.rng)
        if out["ration"]:
            self.result.rations_used += 1
        if not out["win"]:
            self.result.retreats += 1
            self.floor_stat(self.floor)["normal_retreats"] += 1
            self.xp_gain(round(out["enemy"].xp * DEFEAT_XP_RATIO))
            self.on_defeat()
            self.farm_recovery(gap_mult(self.hero.level, self.floor))
            return out
        self.result.kills_normal += 1
        if farm:
            self.floor_stat(self.floor)["farm_kills"] += 1
        self.xp_gain(out["enemy"].xp)
        self.hero.gold += out["enemy"].gold
        self.result.gold_earned += out["enemy"].gold
        self.after_mob_loot(out)
        return out

    def farm_floor_walk(self) -> None:
        self.result.guardian_farms += 1
        for pos in range(10, 90, 10):
            tier = "easy" if pos < 30 else "mid" if pos < 70 else "hard"
            self.do_normal_fight(tier, farm=True)
        # segment 90 m : portail
        if self.steps_left < 100:
            self.day_advance()
        self.steps_left -= 100
        self.result.steps += 100

    def maybe_farm_before_boss(self) -> None:
        if self.profile.farm_mobs <= 0:
            return
        estimate = boss_estimate(self.hero, self.floor)
        if estimate >= self.profile.boss_threshold:
            return
        # farm de mobs au palier courant
        self.result.preboss_farm_mobs += self.profile.farm_mobs
        for _ in range(self.profile.farm_mobs):
            tier = "mid" if self.rng.random() < 0.7 else "hard"
            self.do_normal_fight(tier, farm=True)
        manage_gear(self.hero, self.profile, self.floor, self.rng)

    def after_mob_loot(self, out: dict) -> None:
        self.mobs_since_rare += 1
        if self.mobs_since_rare >= int(TARGET["LOOT_PITY_RARE_KILLS"]):
            self.mobs_since_rare = 0
            self.add_loot(loot_item(self.floor, "rare", self.rng, self.hero.level))
        if self.rng.random() < TARGET["MOB_RATION_CHANCE"]:
            self.hero.rations = min(MAX_RATIONS, self.hero.rations + 1)
        if out["elite"]:
            self.hero.rations = min(MAX_RATIONS, self.hero.rations + int(TARGET["ELITE_RATION_GUARANTEED"]))
            if self.rng.random() < CURRENT["ELITE_SHARD_CHANCE"]:
                self.hero.shards = min(MAX_SHARDS, self.hero.shards + 1)
        if self.rng.random() < CURRENT["CHEST_CHANCE"]:
            self.spawn_chest()

    def record_snapshot(self) -> None:
        if self.floor % 10 != 0 or self.floor in self.result.snapshots:
            return
        s = self.hero.stats(self.floor)
        self.result.snapshots[self.floor] = Snapshot(
            floor=self.floor,
            level=self.hero.level,
            days=self.day - 1 + (self.steps_per_day - self.steps_left) / self.steps_per_day,
            steps=self.result.steps,
            stats=s,
            equipment=list(self.hero.equipment.values()),
            upgrades=dict(self.hero.upgrades),
            boss_attempts=self.result.boss_attempts,
            retreats=self.result.retreats,
            farm_kills=self.result.farm_kills,
            tenacity=self.tenacity_stacks,
        )

    def run(self, max_days: int = 2000) -> RunResult:
        self.record_snapshot()
        while self.floor <= 100 and self.day <= max_days:
            if self.result.steps - self.floor_start_steps > 200_000:
                current_power = self.hero.stats(self.floor).power
                if current_power <= self.floor_start_power * 1.03:
                    self.result.wall_floor = self.floor
                    break
                self.floor_start_steps = self.result.steps
                self.floor_start_power = current_power
            if self.mode == "walk":
                self.distance = self.next_fight
                if self.next_fight >= int(GATE_METERS):
                    if self.steps_left < 100:
                        self.day_advance()
                    self.steps_left -= 100
                    self.result.steps += 100
                    self.hero.steps += 100
                    self.mode = "gate"
                    continue
                if self.rng.random() < 0.08:
                    if self.steps_left < 100:
                        self.day_advance()
                    self.steps_left -= 100
                    self.result.steps += 100
                    self.hero.steps += 100
                    self.resolve_special()
                    self.next_fight += 10
                else:
                    pos = self.next_fight
                    tier = "easy" if pos < 30 else "mid" if pos < 70 else "hard"
                    out = self.do_normal_fight(tier)
                    if not out["win"]:
                        self.distance = max(0.0, math.floor((self.distance - 20.0) / 10.0) * 10.0)
                        self.next_fight = int(self.distance + 10)
                    else:
                        self.next_fight += 10
                    self.record_snapshot()
                if self.next_fight > 100:
                    self.next_fight = 100
            elif self.mode == "gate":
                self.maybe_farm_before_boss()
                if self.floor % 10 == 0 and self.profile.farm_boss > 0:
                    # farm du gardien : il faut refaire un palier complet
                    for _ in range(self.profile.farm_boss):
                        if boss_estimate(self.hero, self.floor) < 0.80:
                            break
                        self.farm_floor_walk()
                        if not self.attempt_boss():
                            break
                        manage_gear(self.hero, self.profile, self.floor, self.rng)
                if self.attempt_boss():
                    fs = self.floor_stat(self.floor)
                    fs["steps"] = self.result.steps - self.floor_entry_steps
                    fs["days"] = self.day - self.floor_entry_day
                    self.floor += 1
                    self.distance = 0.0
                    self.next_fight = 10
                    self.mode = "walk"
                    self.result.max_floor = max(self.result.max_floor, self.floor)
                    self.floor_start_steps = self.result.steps
                    self.floor_start_power = self.hero.stats(self.floor).power
                    self.tenacity_floor = self.floor
                    self.floor_entry_steps = self.result.steps
                    self.floor_entry_day = self.day
                    manage_gear(self.hero, self.profile, self.floor, self.rng)
                    self.record_snapshot()
                else:
                    self.mode = "walk"
            else:
                self.mode = "walk"
        self.result.finished = self.floor > 100
        self.result.finish_floor = min(self.floor, 101)
        self.result.final_level = self.hero.level
        self.result.days = self.day - 1 + (self.steps_per_day - self.steps_left) / self.steps_per_day
        self.result.max_floor = max(self.result.max_floor, self.floor)
        return self.result


# ---------------------------------------------------------------------------
# TTK de reference
# ---------------------------------------------------------------------------


def ttk_table(result: RunResult, potion: str = "") -> list[dict]:
    rows = []
    for floor in range(10, 101, 10):
        snap = result.snapshots.get(floor)
        if snap is None:
            continue
        hero = Hero(level=snap.level, upgrades=dict(snap.upgrades))
        hero.tenacity = snap.tenacity
        hero.equipment = {item.slot: item for item in snap.equipment}
        build = hero.build(floor)
        gap = gap_mult(snap.level, floor)
        row = {
            "floor": floor,
            "level": snap.level,
            "atk": round(snap.stats.atk),
            "hp": round(snap.stats.max_hp),
            "vit": round(snap.stats.vit, 2),
            "crit": round(snap.stats.crit),
            "upgrades": dict(snap.upgrades),
            "days": round(snap.days, 1),
        }
        for tier in ("easy", "mid", "hard"):
            enemy = make_tier_enemy(floor, tier, False, gap)
            res = simulate(build, enemy, runs=120, seed=1234, rule=rule_for(floor, "auto"), potion=potion)
            row[f"ttk_{tier}"] = round(res.ttk, 1) if res.ttk else None
            row[f"win_{tier}"] = round(res.win_rate, 0)
        elite = make_tier_enemy(floor, "hard", True, gap)
        res = simulate(build, elite, runs=120, seed=1235, rule=rule_for(floor, "auto"), potion=potion)
        row["ttk_elite"] = round(res.ttk, 1) if res.ttk else None
        row["win_elite"] = round(res.win_rate, 0)
        variants = boss_variants(floor, "code", "auto", ability_for(floor, "auto"), 0.0, Tuning())
        variants = [(w, replace(e, hp=e.hp * gap, atk=e.atk * gap)) for w, e in variants]
        if GEAR_SCALING:
            scale = gear_scale(hero, floor)
            variants = [(w, replace(e, hp=e.hp * scale, atk=e.atk * scale)) for w, e in variants]
        if floor >= int(TARGET["ENDGAME_FINAL_FLOOR"]):
            final_scale = final_boss_gear_hp(hero, floor)
            variants = [
                (w, replace(e, hp=e.hp * final_scale, atk=e.atk * 1.08, ability_period=6.0))
                for w, e in variants
            ]
        res = simulate_mixed(build, variants, 200, potion, rule_for(floor, "auto"), seed=1236)
        row["ttk_boss"] = round(res.ttk, 1) if res.ttk else None
        row["win_boss"] = round(res.win_rate, 0)
        row["hp_loss_boss"] = round(build.hp - res.avg_hp_left) if res.avg_hp_left else None
        row["rations_boss"] = round(res.avg_rations, 2)
        rows.append(row)
    return rows


def fmt(value: float | None, digits: int = 1) -> str:
    if value is None:
        return "-"
    return f"{value:.{digits}f}".replace(".", ",")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--seeds", type=int, default=8)
    parser.add_argument("--profiles", default="passif,normal,optimise,hyper")
    parser.add_argument("--steps", default="5000,8000,15000")
    parser.add_argument("--out", default="")
    parser.add_argument("--early-hp", type=float, default=None)
    parser.add_argument("--early-atk", type=float, default=None)
    parser.add_argument("--early-peak-start", type=int, default=None)
    parser.add_argument("--early-peak-end", type=int, default=None)
    parser.add_argument("--early-end", type=int, default=None)
    parser.add_argument("--elite-hp", type=float, default=None)
    parser.add_argument("--elite-atk", type=float, default=None)
    parser.add_argument("--pity-step", type=float, default=None)
    parser.add_argument("--pity-cap", type=float, default=None)
    parser.add_argument("--boss-ttk", type=float, default=None)
    parser.add_argument("--gear-scaling", action="store_true")
    args = parser.parse_args()
    global GEAR_SCALING
    GEAR_SCALING = args.gear_scaling
    if args.early_hp is not None:
        TARGET["EARLY_HP_MAX"] = args.early_hp
    if args.early_atk is not None:
        TARGET["EARLY_ATK_MAX"] = args.early_atk
    if args.early_peak_start is not None:
        TARGET["EARLY_CURVE_PEAK_START"] = args.early_peak_start
    if args.early_peak_end is not None:
        TARGET["EARLY_CURVE_PEAK_END"] = args.early_peak_end
    if args.early_end is not None:
        TARGET["EARLY_CURVE_END"] = args.early_end
    if args.elite_hp is not None:
        CURRENT["ELITE_HP_MULT"] = args.elite_hp
    if args.elite_atk is not None:
        CURRENT["ELITE_ATK_MULT"] = args.elite_atk
    if args.pity_step is not None:
        TARGET["PITY_HP_STEP"] = args.pity_step
    if args.pity_cap is not None:
        TARGET["PITY_CAP"] = args.pity_cap
    if args.boss_ttk is not None:
        TARGET["BOSS_TARGET_TTK"] = args.boss_ttk
    profile_keys = [p.strip() for p in args.profiles.split(",") if p.strip()]
    step_values = [int(s) for s in args.steps.split(",") if s.strip()]
    output = {"runs": [], "ttk": {}, "summary": []}
    for key in profile_keys:
        profile = PROFILES[key]
        for steps in step_values:
            runs: list[RunResult] = []
            for seed in range(args.seeds):
                sim = Progression(profile, steps, seed=seed * 7919 + 17)
                res = sim.run()
                runs.append(res)
                output["runs"].append({
                    "profile": key,
                    "steps": steps,
                    "seed": seed,
                    "finished": res.finished,
                    "floor": res.finish_floor,
                    "level": res.final_level,
                    "level_100_floor": res.level_100_floor,
                    "days": round(res.days, 1),
                    "steps_total": res.steps,
                    "kills_normal": res.kills_normal,
                    "kills_boss": res.kills_boss,
                    "farm_kills": res.farm_kills,
                    "retreats": res.retreats,
                    "boss_attempts": res.boss_attempts,
                    "guardian_farms": res.guardian_farms,
                    "preboss_farm_mobs": res.preboss_farm_mobs,
                    "chests": res.chests,
                    "gold_earned": res.gold_earned,
                    "floor_stats": res.floor_stats,
                })
            finished = [r for r in runs if r.finished]
            pool = finished if finished else runs
            pool_sorted = sorted(pool, key=lambda r: (r.days, r.final_level))
            median = pool_sorted[len(pool_sorted) // 2]
            days_values = [r.days for r in finished] if finished else [r.days for r in runs]
            output["summary"].append({
                "profile": key,
                "steps": steps,
                "runs": len(runs),
                "finished": len(finished),
                "days_median": round(statistics.median(days_values), 1),
                "days_min": round(min(days_values), 1),
                "days_max": round(max(days_values), 1),
                "level_median": round(statistics.median([r.final_level for r in pool]), 1),
                "floor_median": round(statistics.median([r.finish_floor for r in runs]), 1),
                "retreats_median": round(statistics.median([r.retreats for r in runs]), 1),
                "boss_attempts_median": round(statistics.median([r.boss_attempts for r in runs]), 1),
                "kills_median": round(statistics.median([r.kills_normal + r.kills_boss for r in runs]), 0),
                "farm_median": round(statistics.median([r.farm_kills for r in runs]), 0),
                "guardian_farms_median": round(statistics.median([r.guardian_farms for r in runs]), 1),
                "preboss_farm_mobs_median": round(statistics.median([r.preboss_farm_mobs for r in runs]), 1),
                "chests_median": round(statistics.median([r.chests for r in runs]), 0),
                "rations_median": round(statistics.median([r.rations_used for r in runs]), 0),
                "gold_median": round(statistics.median([r.gold_earned for r in runs]), 0),
                "level_100_floor_median": round(statistics.median([r.level_100_floor or 101 for r in runs]), 0),
            })
            output["ttk"][f"{key}_{steps}"] = ttk_table(
                median,
                potion="atk" if key in ("optimise", "hyper", "passif_auto", "passif_auto_heal") else "",
            )
            print(f"{profile.label:24} {steps:5} pas/j : {len(finished)}/{len(runs)} finis, "
                  f"jours median={output['summary'][-1]['days_median']}, "
                  f"niveau={output['summary'][-1]['level_median']}, "
                  f"replis={output['summary'][-1]['retreats_median']}")
    if args.out:
        Path(args.out).write_text(json.dumps(output, ensure_ascii=False, indent=2), encoding="utf-8")
        print(f"Rapport ecrit : {args.out}")
    else:
        print(json.dumps(output["summary"], ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
