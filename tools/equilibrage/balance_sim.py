#!/usr/bin/env python3
"""
Simulateur d'équilibrage pour La Marche des Âmes.

But : rejouer les combats avec les formules actuelles ou proposées, sans
toucher au code Android, pour vérifier qu'un build peut franchir un palier
et qu'un objectif (niveau 100, économie d'éclats) reste atteignable.

Aucune dépendance externe : Python 3.10+ suffit.

Courbes :
    current  : ancienne courbe 0.20.0 (historique).
    proposed : ancienne courbe exponentielle (historique).
    target   : courbe de design (TTK constant, 18 s pour un gardien).
    code     : reproduit `GameEngine.makeBoss` / `makeMob` (0.21.0).

Exemples :
    python tools/equilibrage/balance_sim.py --curve code --build reference,reference_high \
      --floors 36,50,75,100 --runs 500
    python tools/equilibrage/balance_sim.py --xp
"""

from __future__ import annotations

import argparse
import math
import random
import sys
from collections import Counter
from dataclasses import dataclass, replace
from typing import Iterable

from balance_constants import CURRENT, TARGET

# ---------------------------------------------------------------------------
# Modèles
# ---------------------------------------------------------------------------


@dataclass(frozen=True)
class Build:
    name: str
    atk: float
    hp: float
    vit: float
    crit: float = 30.0          # pourcentage
    crit_damage: float = 0.0    # bonus additif (0.4 = +40 %)
    regen: float = 1.0          # PV/s (valeur de fiche, avant facteur de combat)
    lifesteal: float = 0.0
    damage_reduction: float = 0.0
    boss_damage: float = 0.0
    damage_taken: float = 0.0
    ferveur: float = 100.0
    rations: int = 1


@dataclass(frozen=True)
class Enemy:
    name: str
    hp: float
    atk: float
    interval: float             # secondes
    is_boss: bool = False
    ability: str = ""
    ability_first: float = 6.5
    ability_period: float = 7.0
    ability_mult: float = 0.0
    ability_heal: float = 0.0       # fraction des PV max
    ability_leech: float = 0.0      # fraction des dégâts infligés
    ability_vulnerable: float = 0.0
    ability_vulnerable_ms: float = 0.0
    ability_vit: float = 0.0
    ability_vit_ms: float = 0.0
    ability_ferveur: int = 0
    shield_reduction: float = 0.0
    shield_duration: float = 3.0
    enrage_after: float = 0.0
    enrage_step: float = 5.0
    enrage_pct: float = 0.15
    enrage_cap: float = 0.60
    regen_rate: float = 0.0     # fraction des PV max par seconde
    modifier: str = ""
    xp: int = 0
    gold: int = 0


@dataclass
class Result:
    runs: int
    wins: int
    avg_time: float
    avg_hp_left: float
    avg_rations: float
    avg_damage_taken: float
    avg_hero_hits: float
    avg_enemy_hits: float
    ttk: float | None
    ttd: float | None

    @property
    def win_rate(self) -> float:
        return 100.0 * self.wins / self.runs


# ---------------------------------------------------------------------------
# Biomes et règles (miroir de `biomeRuleFor`, Model.kt)
# ---------------------------------------------------------------------------


@dataclass(frozen=True)
class Rule:
    enemy_hp: float = 1.0
    enemy_atk: float = 1.0
    enemy_interval: float = 1.0
    boss_hp: float = 1.0
    boss_atk: float = 1.0
    hero_atk: float = 0.0
    hero_hp: float = 0.0
    hero_vit: float = 0.0
    hero_dr: float = 0.0
    hero_crit: float = 0.0
    hero_regen: float = 0.0
    boss_damage: float = 0.0


RULES = {
    "generic": Rule(),
    "crypts": Rule(enemy_hp=0.95, hero_crit=3.0),
    "catacombs": Rule(enemy_interval=1.05, hero_vit=0.06),
    "ash": Rule(enemy_hp=0.95, enemy_atk=1.10, hero_atk=0.10, hero_regen=-0.15),
    "forge": Rule(enemy_hp=1.10, enemy_atk=0.95, hero_dr=0.06),
    "peaks": Rule(enemy_interval=0.90, hero_vit=0.08, hero_hp=-0.05),
    "woods": Rule(enemy_hp=0.95, enemy_atk=1.05, hero_regen=0.5),
    "abyss": Rule(enemy_hp=0.90, enemy_atk=1.10, hero_atk=0.12, hero_hp=-0.05),
    "throne": Rule(enemy_hp=1.10, enemy_atk=1.10, hero_hp=-0.08, boss_damage=0.12),
}


def _endgame_mult(floor: int) -> float:
    start = TARGET["ENDGAME_FLOOR"]
    end = TARGET["ENDGAME_FINAL_FLOOR"]
    t = min(1.0, max(0.0, (floor - start) / (end - start)))
    return TARGET["ENDGAME_ENEMY_MULT_START"] + (
        TARGET["ENDGAME_ENEMY_MULT_END"] - TARGET["ENDGAME_ENEMY_MULT_START"]
    ) * t


def rule_for(floor: int, biome: str) -> Rule:
    if biome != "auto":
        return RULES[biome]
    if floor >= int(TARGET["ENDGAME_FLOOR"]):
        mult = _endgame_mult(floor)
        return Rule(
            enemy_hp=mult,
            enemy_atk=mult,
            boss_hp=TARGET["ENDGAME_BOSS_HP_MULT"],
            boss_atk=TARGET["ENDGAME_BOSS_ATK_MULT"],
        )
    if floor <= 10:
        return RULES["crypts"]
    if floor <= 20:
        return RULES["catacombs"]
    if floor <= 30:
        return RULES["ash"]
    if floor <= 40:
        return RULES["forge"]
    if floor <= 50:
        return RULES["peaks"]
    if floor <= 60:
        return RULES["woods"]
    if floor <= 70:
        return RULES["abyss"]
    return RULES["throne"]


# ---------------------------------------------------------------------------
# Courbes d'ennemis
# ---------------------------------------------------------------------------


def _target_weight(floor: int) -> float:
    start = CURRENT["TARGET_WEIGHT_START"]
    full = CURRENT["TARGET_WEIGHT_FULL"]
    return min(1.0, max(0.0, (floor - start) / (full - start)))


def _lerp(a: float, b: float, t: float) -> float:
    return a + (b - a) * t


def _depth(floor: int, boss: bool = False) -> float:
    start = int(CURRENT["DEPTH_START_FLOOR"])
    if floor <= start:
        return 1.0
    base = CURRENT["BOSS_DEPTH_BASE"] if boss else CURRENT["MOB_DEPTH_BASE"]
    return min(base ** (floor - start), CURRENT["DEPTH_CAP"])


def _code_mob_hp_scale(floor: int) -> float:
    target = max(1.0, CURRENT["MOB_HP_TARGET_BASE"] - CURRENT["MOB_HP_TARGET_STEP"] * (floor - 10))
    return _lerp(_depth(floor), target, _target_weight(floor))


def _code_mob_atk_scale(floor: int) -> float:
    target = max(1.0, CURRENT["MOB_ATK_TARGET_BASE"] - CURRENT["MOB_ATK_TARGET_STEP"] * (floor - 10))
    return _lerp(_depth(floor), target, _target_weight(floor))


def _code_boss_hp_scale(floor: int) -> float:
    target = max(1.0, CURRENT["BOSS_HP_TARGET_BASE"] - CURRENT["BOSS_HP_TARGET_STEP"] * (floor - 10))
    return _lerp(6.0 * _depth(floor, boss=True), target, _target_weight(floor))


def _code_boss_atk_scale(floor: int) -> float:
    target = max(1.0, CURRENT["BOSS_ATK_TARGET_BASE"] - CURRENT["BOSS_ATK_TARGET_STEP"] * (floor - 10))
    return _lerp(1.6 * _depth(floor, boss=True), target, _target_weight(floor))


def _ref_interval(floor: int, rule: Rule) -> float:
    vit = (1.8 + 0.02 * floor) * (1.0 + rule.hero_vit)
    return max(TARGET["HERO_MIN_INTERVAL_MS"] / 1000.0, 1.0 / (vit * 1.4))


def _ref_dps(floor: int, rule: Rule) -> float:
    atk = (650 + 20 * floor) * (1.0 + rule.hero_atk)
    crit = min(75.0, 30.0 + 2.0 + rule.hero_crit)
    return atk * (1.0 + crit / 100.0 * 0.6) / _ref_interval(floor, rule)


def _ref_ehp(floor: int, rule: Rule) -> float:
    hp = (1800 + 55 * floor) * (1.0 + rule.hero_hp)
    dr = min(TARGET["DAMAGE_REDUCTION_CAP"], 0.06 + rule.hero_dr)
    return hp * 1.35 / (1.0 - dr)


ATK_DIVISORS = {
    "shield": 14.5,
    "breath": 19.0,
    "pressure": 16.0,
    "rupture": 21.5,
    "slam": 17.5,
    "drain": 18.0,
    "roots": 18.0,
    "acid": 17.5,
    "regen": 17.0,
}


def _atk_divisor(ability: str) -> float:
    return ATK_DIVISORS.get(ability, TARGET["BOSS_DIV_DEFAULT"])


def _shield_factor(ability: str) -> float:
    if ability != "shield":
        return 1.0
    reduction = TARGET["SHIELD_REDUCTION"]
    duration = TARGET["SHIELD_DURATION_MS"] / 1000.0
    period = TARGET["SHIELD_PERIOD_MS"] / 1000.0
    return 1.0 - reduction * duration / period


def _mob_depth_proposed(floor: int, tuning: "Tuning") -> float:
    return 1.0 if floor <= 10 else min(tuning.hp_exp ** (floor - 10), tuning.hp_cap)


def _atk_depth_proposed(floor: int, tuning: "Tuning") -> float:
    return 1.0 if floor <= 10 else min(tuning.atk_exp ** (floor - 10), tuning.atk_cap)


def _boss_depth_proposed(floor: int, tuning: "Tuning") -> float:
    return 1.0 if floor <= 10 else min(tuning.boss_hp_exp ** (floor - 10), tuning.boss_hp_cap)


def _early_threat(floor: int, maximum: float) -> float:
    """Courbe de palier early game, miroir de GameEngine.earlyThreatMultiplier()."""
    start = int(TARGET["EARLY_CURVE_PEAK_START"])
    peak_end = int(TARGET["EARLY_CURVE_PEAK_END"])
    end = int(TARGET["EARLY_CURVE_END"])
    if floor <= 1 or floor >= end:
        return 1.0
    if floor < start:
        return 1.0 + (maximum - 1.0) * ((floor - 1) / (start - 1))
    if floor <= peak_end:
        return maximum
    return 1.0 + (maximum - 1.0) * ((end - floor) / (end - peak_end))


def make_mob(floor: int, curve: str, biome: str = "auto", elite: bool = False,
             tuning: "Tuning" = None) -> Enemy:
    tuning = tuning or Tuning()
    rule = rule_for(floor, biome)
    level = floor + 2 if elite else floor
    if curve == "code":
        hp = (55 + 34 * (level - 1)) * (1.8 if elite else 1.0) * _code_mob_hp_scale(floor) * rule.enemy_hp
        atk = (6 + 2.2 * (level - 1)) * (1.15 if elite else 1.0) * _code_mob_atk_scale(floor) * rule.enemy_atk
        hp *= _early_threat(floor, TARGET["EARLY_HP_MAX"])
        atk *= _early_threat(floor, TARGET["EARLY_ATK_MAX"])
        interval = CURRENT["MOB_INTERVAL_MS"] / 1000.0 * rule.enemy_interval
        reward = min(6.0, math.sqrt(_depth(floor)))
        gold = (6 + 3 * level) * reward
        xp = (12 + 5 * level) * (1.0 + TARGET["XP_FLOOR_MULT"] * (floor - 1))
    elif curve == "current":
        depth = _depth(floor)
        hp = (55 + 34 * (level - 1)) * (2.2 if elite else 1.0) * depth
        atk = (6 + 2.2 * (level - 1)) * (1.3 if elite else 1.0) * depth
        interval = CURRENT["MOB_INTERVAL_MS"] / 1000.0
        reward = min(6.0, math.sqrt(depth))
        gold = (6 + 3 * level) * (1.6 if elite else 1.0) * reward
        xp = (12 + 5 * level) * (1.8 if elite else 1.0) * reward
    elif curve == "target":
        ref_atk = 650 + 20 * floor
        ref_hp = 1800 + 55 * floor
        ref_vit = 1.8 + 0.02 * floor
        ref_interval = max(TARGET["HERO_MIN_INTERVAL_MS"] / 1000.0, 1.0 / (ref_vit * 1.4))
        ref_dps = ref_atk * 1.18 / ref_interval
        hp = ref_dps * 5.0 * (1.8 if elite else 1.0)
        atk = (ref_hp * 1.35 / 0.94 / tuning.target_mob_atk_div) * (1.15 if elite else 1.0)
        hp *= rule.enemy_hp
        atk *= rule.enemy_atk
        interval = CURRENT["MOB_INTERVAL_MS"] / 1000.0 * rule.enemy_interval
        gold = (6 + 3 * level) * 0.5
        xp = (12 + 5 * level) * (1.8 if elite else 1.0) * (1 + TARGET["XP_FLOOR_MULT"] * (floor - 1))
    else:  # proposed
        hp = (55 + 34 * (level - 1)) * (1.8 if elite else 1.0) * _mob_depth_proposed(floor, tuning)
        atk = (6 + 2.2 * (level - 1)) * (1.15 if elite else 1.0) * _atk_depth_proposed(floor, tuning)
        hp *= rule.enemy_hp
        atk *= rule.enemy_atk
        interval = CURRENT["MOB_INTERVAL_MS"] / 1000.0 * rule.enemy_interval
        gold = (6 + 3 * level) * 0.5
        xp = (12 + 5 * level) * (1.8 if elite else 1.0) * (1 + TARGET["XP_FLOOR_MULT"] * (floor - 1))
    return Enemy(
        name="élite" if elite else "monstre",
        hp=hp,
        atk=atk,
        interval=interval,
        xp=round(xp),
        gold=round(gold),
    )


# ---------------------------------------------------------------------------
# Capacités de gardien
# ---------------------------------------------------------------------------


def ability_for(floor: int, biome: str = "auto") -> str:
    if floor >= int(TARGET["ENDGAME_FINAL_FLOOR"]):
        return "rupture"
    if floor % 50 == 0:
        return "breath"
    if floor % 25 == 0:
        return "pressure"
    key = biome if biome != "auto" else ("crypts" if floor <= 10 else "catacombs" if floor <= 20
                                         else "ash" if floor <= 30 else "forge" if floor <= 40
                                         else "peaks" if floor <= 50 else "woods" if floor <= 60
                                         else "abyss" if floor <= 70 else "throne")
    return {
        "crypts": "drain",
        "catacombs": "roots",
        "ash": "slam",
        "forge": "shield",
        "peaks": "breath",
        "woods": "roots",
        "abyss": "acid",
        "throne": "pressure",
    }[key]


def modifier_pool(ability: str) -> list[str]:
    heavy = ability in {"slam", "breath", "drain", "pressure", "rupture"}
    pool = ["", "", "blinde", "regenerant", "voleur"]
    if not heavy:
        pool += ["rapide", "berserk"]
    return pool


def _ability_fields(ability: str) -> dict:
    if ability == "slam":
        return {"ability_mult": 2.4}
    if ability == "drain":
        return {"ability_mult": 1.5, "ability_leech": 0.8}
    if ability == "regen":
        return {"ability_heal": 0.07}
    if ability == "shield":
        return {"ability_period": 8.0}
    if ability == "breath":
        return {"ability_mult": 2.0, "ability_vulnerable": 0.15, "ability_vulnerable_ms": 6000}
    if ability == "roots":
        return {"ability_mult": 1.2, "ability_vit": -0.25, "ability_vit_ms": 5000}
    if ability == "acid":
        return {"ability_mult": 1.4, "ability_vulnerable": 0.15, "ability_vulnerable_ms": 6000}
    if ability == "pressure":
        return {"ability_mult": 1.8, "ability_ferveur": 10}
    if ability == "rupture":
        return {"ability_mult": 2.3, "ability_ferveur": 20, "ability_heal": 0.03}
    return {}


def make_boss(floor: int, curve: str, biome: str = "auto", ability: str = "auto",
              modifier: str = "", pity: float = 0.0, tuning: "Tuning" = None) -> Enemy:
    tuning = tuning or Tuning()
    rule = rule_for(floor, biome)
    if ability == "auto":
        ability = ability_for(floor, biome)
    if ability == "none":
        ability = ""
    level = floor + 1
    if curve == "code":
        base_hp = 55 + 34 * (level - 1)
        base_atk = 6 + 2.2 * (level - 1)
        final_hp = TARGET["FINAL_BOSS_HP_MULT"] if floor >= int(TARGET["ENDGAME_FINAL_FLOOR"]) else 1.0
        final_atk = TARGET["FINAL_BOSS_ATK_MULT"] if floor >= int(TARGET["ENDGAME_FINAL_FLOOR"]) else 1.0
        legacy_hp = base_hp * _code_boss_hp_scale(floor) * rule.enemy_hp * rule.boss_hp
        legacy_atk = base_atk * _code_boss_atk_scale(floor) * rule.enemy_atk * rule.boss_atk
        ttk = TARGET["BOSS_TARGET_TTK"]
        divisor = _atk_divisor(ability)
        target_hp = _ref_dps(floor, rule) * ttk * _shield_factor(ability)
        target_atk = _ref_ehp(floor, rule) / divisor
        weight = _target_weight(floor)
        hp = _lerp(legacy_hp, target_hp, weight)
        atk = _lerp(legacy_atk, target_atk, weight)
        hp *= final_hp
        atk *= final_atk
        hp *= _early_threat(floor, TARGET["EARLY_HP_MAX"])
        atk *= _early_threat(floor, TARGET["EARLY_ATK_MAX"])
        interval = CURRENT["MOB_INTERVAL_MS"] / 1000.0
        if modifier == "rapide":
            interval = CURRENT["BOSS_MOD_FAST_INTERVAL_MS"] / 1000.0
        if modifier == "blinde":
            hp *= CURRENT["BOSS_MOD_BLINDED_HP"]
        if modifier == "regenerant":
            hp *= CURRENT["BOSS_MOD_REGEN_HP"]
        if modifier == "berserk":
            atk *= CURRENT["BOSS_MOD_BERSERK_ATK"]
        if modifier == "voleur":
            atk *= CURRENT["BOSS_MOD_THIEF_ATK"]
        hp *= (1.0 - pity)
        atk *= (1.0 - pity)
        interval *= rule.enemy_interval
        enrage_after = TARGET["BOSS_ENRAGE_AFTER_MS"] / 1000.0
        enrage_pct = TARGET["BOSS_ENRAGE_STEP"]
        enrage_cap = TARGET["BOSS_ENRAGE_CAP"]
        shield_reduction = TARGET["SHIELD_REDUCTION"]
        reward = min(10.0, math.sqrt(_depth(floor, boss=True)))
        gold = 12 * (6 + 3 * level) * reward
        xp = 10 * (12 + 5 * level) * (1 + TARGET["XP_FLOOR_MULT"] * (floor - 1))
    elif curve == "current":
        depth = _depth(floor, boss=True)
        level = floor + 2
        hp = 6.0 * (55 + 34 * (level - 1)) * depth
        atk = 1.6 * (6 + 2.2 * (level - 1)) * depth
        interval = CURRENT["MOB_INTERVAL_MS"] / 1000.0
        enrage_after = CURRENT["BOSS_ENRAGE_AFTER_MS"] / 1000.0
        enrage_pct = CURRENT["BOSS_ENRAGE_STEP"]
        enrage_cap = CURRENT["BOSS_ENRAGE_CAP"]
        shield_reduction = 0.65
        hp *= rule.enemy_hp
        atk *= rule.enemy_atk
        interval *= rule.enemy_interval
        reward = min(10.0, math.sqrt(depth))
        gold = 12 * (6 + 3 * level) * reward
        xp = 10 * (12 + 5 * level) * reward
    elif curve == "target":
        ref_atk = 650 + 20 * floor
        ref_hp = 1800 + 55 * floor
        ref_vit = 1.8 + 0.02 * floor
        ref_interval = max(TARGET["HERO_MIN_INTERVAL_MS"] / 1000.0, 1.0 / (ref_vit * 1.4))
        ref_dps = ref_atk * 1.18 / ref_interval
        hp = ref_dps * 18.0 * 0.85
        atk = (ref_hp * 1.35 / 0.94) / tuning.target_boss_atk_div
        interval = CURRENT["MOB_INTERVAL_MS"] / 1000.0
        if floor >= int(TARGET["ENDGAME_FLOOR"]):
            hp *= TARGET["ENDGAME_BOSS_HP_MULT"]
            atk *= TARGET["ENDGAME_BOSS_ATK_MULT"]
        if floor >= int(TARGET["ENDGAME_FINAL_FLOOR"]):
            hp *= TARGET["FINAL_BOSS_HP_MULT"]
            atk *= TARGET["FINAL_BOSS_ATK_MULT"]
        hp *= rule.enemy_hp * rule.boss_hp * (1.0 - pity)
        atk *= rule.enemy_atk * rule.boss_atk * (1.0 - pity)
        interval *= rule.enemy_interval
        enrage_after = tuning.enrage_after
        enrage_pct = tuning.enrage_pct
        enrage_cap = tuning.enrage_cap
        shield_reduction = tuning.shield_reduction
        gold = 6 * (6 + 3 * level) * 0.5
        xp = 10 * (12 + 5 * level) * (1 + TARGET["XP_FLOOR_MULT"] * (floor - 1))
    else:  # proposed
        hp = tuning.boss_hp_mult * (55 + 34 * (level - 1)) * _boss_depth_proposed(floor, tuning)
        atk = tuning.boss_atk_mult * (6 + 2.2 * (level - 1)) * _atk_depth_proposed(floor, tuning)
        interval = CURRENT["MOB_INTERVAL_MS"] / 1000.0
        hp *= rule.enemy_hp * rule.boss_hp
        atk *= rule.enemy_atk * rule.boss_atk
        interval *= rule.enemy_interval
        enrage_after = tuning.enrage_after
        enrage_pct = tuning.enrage_pct
        enrage_cap = tuning.enrage_cap
        shield_reduction = tuning.shield_reduction
        gold = 6 * (6 + 3 * level) * 0.5
        xp = 10 * (12 + 5 * level) * (1 + TARGET["XP_FLOOR_MULT"] * (floor - 1))
    fields = _ability_fields(ability)
    return Enemy(
        name="gardien",
        hp=hp,
        atk=atk,
        interval=interval,
        is_boss=True,
        ability=ability,
        ability_first=CURRENT["BOSS_ABILITY_FIRST_MS"] / 1000.0,
        ability_period=fields.get("ability_period", CURRENT["BOSS_ABILITY_INTERVAL_MS"] / 1000.0),
        ability_mult=fields.get("ability_mult", 0.0),
        ability_heal=fields.get("ability_heal", 0.0),
        ability_leech=fields.get("ability_leech", 0.0),
        ability_vulnerable=fields.get("ability_vulnerable", 0.0),
        ability_vulnerable_ms=fields.get("ability_vulnerable_ms", 0.0),
        ability_vit=fields.get("ability_vit", 0.0),
        ability_vit_ms=fields.get("ability_vit_ms", 0.0),
        ability_ferveur=fields.get("ability_ferveur", 0),
        shield_reduction=shield_reduction,
        shield_duration=TARGET["SHIELD_DURATION_MS"] / 1000.0,
        enrage_after=enrage_after,
        enrage_step=CURRENT["BOSS_ENRAGE_STEP_MS"] / 1000.0,
        enrage_pct=enrage_pct,
        enrage_cap=enrage_cap,
        regen_rate=CURRENT["BOSS_REGEN_RATE"] if modifier == "regenerant" else 0.0,
        modifier=modifier,
        xp=round(xp),
        gold=round(gold),
    )


@dataclass(frozen=True)
class Tuning:
    hp_exp: float = 1.11
    hp_cap: float = 30.0
    atk_exp: float = 1.04
    atk_cap: float = 6.0
    boss_hp_exp: float = 1.08
    boss_hp_cap: float = 20.0
    boss_hp_mult: float = 3.0
    boss_atk_mult: float = 1.3
    shield_reduction: float = 0.35
    shield_duration: float = 3.0
    shield_period: float = 8.0
    target_mob_atk_div: float = 8.0
    target_boss_atk_div: float = 21.0
    enrage_after: float = 25.0
    enrage_step: float = 5.0
    enrage_pct: float = 0.15
    enrage_cap: float = 0.60


# ---------------------------------------------------------------------------
# Builds de référence
# ---------------------------------------------------------------------------


def build_preset(name: str, floor: int) -> Build:
    if name == "player36":
        return Build("player36", atk=1349, hp=3699, vit=2.27, crit=30,
                     regen=1.0, damage_reduction=0.06, ferveur=100, rations=1)
    if name == "endgame":
        return Build("endgame", atk=6000, hp=15000, vit=3.5, crit=40,
                     crit_damage=0.2, regen=3.0, lifesteal=0.08,
                     damage_reduction=0.10, ferveur=100, rations=2)
    def profile(prefix: str, atk_base: float, atk_floor: float,
                hp_base: float, hp_floor: float, vit_base: float,
                vit_floor: float) -> Build:
        return Build(prefix, atk=atk_base + atk_floor * floor,
                     hp=hp_base + hp_floor * floor,
                     vit=vit_base + vit_floor * floor,
                     crit=30, regen=1.0, damage_reduction=0.06,
                     ferveur=100, rations=1)
    low = profile("reference_low", 500, 16, 1500, 40, 1.65, 0.015)
    mid = profile("reference", 650, 20, 1800, 55, 1.80, 0.020)
    high = profile("reference_high", 800, 24, 2200, 70, 1.90, 0.022)
    if name == "reference_low":
        return low
    if name == "reference":
        return mid
    if name == "reference_high":
        return high
    if name == "under":
        return replace(mid, name="under", atk=mid.atk * 0.93, hp=mid.hp * 0.93)
    if name == "tank":
        return replace(mid, name="tank", atk=mid.atk * 0.8, hp=mid.hp * 1.4,
                       regen=3.0, damage_reduction=0.16)
    if name == "crit":
        return replace(mid, name="crit", atk=mid.atk * 0.9, crit=60, crit_damage=0.5)
    if name == "lifesteal":
        return replace(mid, name="lifesteal", atk=mid.atk * 0.9, hp=mid.hp * 0.9,
                       lifesteal=0.12)
    raise SystemExit(f"build inconnu : {name}")


# ---------------------------------------------------------------------------
# Simulation de combat
# ---------------------------------------------------------------------------


def _apply_build_modifiers(build: Build, rule: Rule, potion: str) -> Build:
    if potion == "atk":
        # Potion de force : +40 % ATQ, +40 % dégâts subis.
        build = replace(build, atk=build.atk * 1.40,
                        damage_taken=build.damage_taken + 0.40)
    elif potion == "rage":
        # Fiole de rage : +50 % ATQ, +50 % dégâts subis.
        build = replace(build, atk=build.atk * 1.50,
                        damage_taken=build.damage_taken + 0.50)
    elif potion == "atk_pure":
        build = replace(build, atk=build.atk * 1.40)
    elif potion == "crit":
        build = replace(build, crit=min(100.0, build.crit + 50.0),
                        crit_damage=build.crit_damage + 0.20)
    elif potion == "mixed":
        build = replace(build, atk=build.atk * 1.25, crit=min(100.0, build.crit + 25.0))
    return replace(
        build,
        atk=build.atk * (1.0 + rule.hero_atk),
        hp=build.hp * (1.0 + rule.hero_hp),
        vit=build.vit * (1.0 + rule.hero_vit),
        crit=min(75.0, build.crit + rule.hero_crit),
        regen=build.regen + rule.hero_regen,
        damage_reduction=min(TARGET["DAMAGE_REDUCTION_CAP"], build.damage_reduction + rule.hero_dr),
        boss_damage=min(TARGET["BOSS_DAMAGE_CAP"], build.boss_damage + rule.boss_damage),
    )


def simulate(build: Build, enemy: Enemy, runs: int = 200, max_seconds: float = 180.0,
             seed: int = 42, potion: str = "", rule: Rule = Rule(),
             start_hp: float | None = None) -> Result:
    build = _apply_build_modifiers(build, rule, potion)
    rng = random.Random(seed)
    wins = 0
    times: list[float] = []
    hp_left: list[float] = []
    rations: list[int] = []
    damage_taken: list[float] = []
    hero_hits: list[int] = []
    enemy_hits: list[int] = []
    win_times: list[float] = []
    loss_times: list[float] = []

    for _ in range(runs):
        hp = build.hp if start_hp is None else min(start_hp, build.hp)
        enemy_hp = enemy.hp
        ferveur = build.ferveur
        t = 0.0
        hero_timer = 0.0
        enemy_timer = enemy.interval
        ability_timer = enemy.ability_first
        shield_until = -1.0
        vulnerable_until = -1.0
        vit_until = -1.0
        ration_used = False
        taken = 0.0
        hero_swings = 0
        enemy_swings = 0
        lifesteal_cap = build.hp * TARGET["LIFESTEAL_PER_SECOND_CAP"]
        lifesteal_budget = 0.0

        def crit_chance() -> float:
            return min(TARGET["CRIT_CAP"] / 100.0, (build.crit + ferveur * 0.02) / 100.0)

        def attack_interval() -> float:
            vit = build.vit * (0.75 if t < vit_until else 1.0)
            return max(TARGET["HERO_MIN_INTERVAL_MS"] / 1000.0,
                       1.0 / (vit * (1.0 + 0.004 * ferveur)))

        def enemy_damage(multiplier: float) -> float:
            steps = 0
            if enemy.enrage_after > 0 and t >= enemy.enrage_after:
                steps = int((t - enemy.enrage_after) / enemy.enrage_step) + 1
            enrage = 1.0 + min(enemy.enrage_cap, steps * enemy.enrage_pct)
            raw = enemy.atk * multiplier * enrage * (0.9 + rng.random() * 0.1)
            return raw * (1.0 - build.damage_reduction) * (
                1.0 + build.damage_taken + (0.15 if t < vulnerable_until else 0.0)
            )

        while t < max_seconds:
            dt = 0.01
            t += dt
            hero_timer -= dt
            enemy_timer -= dt
            ability_timer -= dt
            lifesteal_budget = min(
                lifesteal_cap, lifesteal_budget + lifesteal_cap * dt
            )

            if enemy.regen_rate > 0 and enemy_hp < enemy.hp:
                enemy_hp = min(enemy.hp, enemy_hp + enemy.hp * enemy.regen_rate * dt)

            if enemy.ability and ability_timer <= 0:
                if enemy.ability == "regen":
                    enemy_hp = min(enemy.hp, enemy_hp + enemy.hp * enemy.ability_heal)
                elif enemy.ability == "shield":
                    shield_until = t + enemy.shield_duration
                else:
                    dmg = enemy_damage(enemy.ability_mult)
                    hp -= dmg
                    taken += dmg
                    if enemy.ability_leech:
                        enemy_hp = min(enemy.hp, enemy_hp + dmg * enemy.ability_leech)
                    if enemy.ability_heal:
                        enemy_hp = min(enemy.hp, enemy_hp + enemy.hp * enemy.ability_heal)
                    if enemy.ability_vulnerable:
                        vulnerable_until = t + enemy.ability_vulnerable_ms / 1000.0
                    if enemy.ability_vit:
                        vit_until = t + enemy.ability_vit_ms / 1000.0
                    if enemy.ability_ferveur:
                        ferveur = max(0.0, ferveur - enemy.ability_ferveur)
                    if hp <= 0:
                        loss_times.append(t)
                        times.append(t)
                        hp_left.append(0.0)
                        rations.append(1 if ration_used else 0)
                        damage_taken.append(taken)
                        hero_hits.append(hero_swings)
                        enemy_hits.append(enemy_swings)
                        break
                ability_timer = enemy.ability_period

            if hero_timer <= 0:
                hero_swings += 1
                dmg = build.atk * (0.9 + rng.random() * 0.2)
                if enemy.is_boss:
                    dmg *= 1.0 + build.boss_damage
                crit = rng.random() < crit_chance()
                if crit:
                    dmg *= 1.6 + build.crit_damage
                if t < shield_until:
                    dmg *= 1.0 - enemy.shield_reduction
                enemy_hp -= dmg
                if build.lifesteal > 0:
                    heal = min(dmg * build.lifesteal, lifesteal_budget)
                    before = hp
                    hp = min(build.hp, hp + heal)
                    lifesteal_budget = max(0.0, lifesteal_budget - (hp - before))
                hero_timer += attack_interval()
                if enemy_hp <= 0:
                    wins += 1
                    win_times.append(t)
                    times.append(t)
                    hp_left.append(hp)
                    rations.append(1 if ration_used else 0)
                    damage_taken.append(taken)
                    hero_hits.append(hero_swings)
                    enemy_hits.append(enemy_swings)
                    break

            if enemy_timer <= 0:
                enemy_swings += 1
                dmg = enemy_damage(1.0)
                hp -= dmg
                taken += dmg
                enemy_timer += enemy.interval
                if hp <= 0:
                    loss_times.append(t)
                    times.append(t)
                    hp_left.append(0.0)
                    rations.append(1 if ration_used else 0)
                    damage_taken.append(taken)
                    hero_hits.append(hero_swings)
                    enemy_hits.append(enemy_swings)
                    break
                if (not ration_used and build.rations > 0
                        and hp < build.hp * TARGET["AUTO_RATION_THRESHOLD"]):
                    ration_used = True
                    hp = min(build.hp, hp + build.hp * TARGET["AUTO_RATION_HEAL"])

            # Régénération hors capacité : 1/3 en combat (GameEngine.tickFight).
            regen_s = (build.hp * 0.006 * (1.0 + 0.008 * ferveur)
                       + max(0.0, build.regen - 0.6)) / 3.0
            hp = min(build.hp, hp + regen_s * dt)

        else:
            loss_times.append(max_seconds)
            times.append(max_seconds)
            hp_left.append(hp)
            rations.append(1 if ration_used else 0)
            damage_taken.append(taken)
            hero_hits.append(hero_swings)
            enemy_hits.append(enemy_swings)

    return Result(
        runs=runs,
        wins=wins,
        avg_time=sum(times) / len(times) if times else 0.0,
        avg_hp_left=sum(hp_left) / len(hp_left) if hp_left else 0.0,
        avg_rations=sum(rations) / len(rations) if rations else 0.0,
        avg_damage_taken=sum(damage_taken) / len(damage_taken) if damage_taken else 0.0,
        avg_hero_hits=sum(hero_hits) / len(hero_hits) if hero_hits else 0.0,
        avg_enemy_hits=sum(enemy_hits) / len(enemy_hits) if enemy_hits else 0.0,
        ttk=sum(win_times) / len(win_times) if win_times else None,
        ttd=sum(loss_times) / len(loss_times) if loss_times else None,
    )


def merge_results(results: list[Result]) -> Result:
    runs = sum(r.runs for r in results)
    wins = sum(r.wins for r in results)
    losses = runs - wins

    def weighted(attr: str, weight: int) -> float:
        pairs = []
        for r in results:
            value = getattr(r, attr)
            if value is None:
                continue
            w = r.runs if weight == 1 else (r.wins if weight == 2 else r.runs - r.wins)
            if w > 0:
                pairs.append((value, w))
        denom = sum(w for _, w in pairs)
        return sum(v * w for v, w in pairs) / denom if denom else 0.0

    win_ttk = weighted("ttk", 2) if wins else None
    loss_ttd = weighted("ttd", 3) if losses else None
    return Result(
        runs=runs,
        wins=wins,
        avg_time=weighted("avg_time", 1),
        avg_hp_left=weighted("avg_hp_left", 1),
        avg_rations=weighted("avg_rations", 1),
        avg_damage_taken=weighted("avg_damage_taken", 1),
        avg_hero_hits=weighted("avg_hero_hits", 1),
        avg_enemy_hits=weighted("avg_enemy_hits", 1),
        ttk=win_ttk,
        ttd=loss_ttd,
    )


def simulate_mixed(build: Build, variants: list[tuple[float, Enemy]], runs: int,
                   potion: str, rule: Rule, seed: int = 42) -> Result:
    results = []
    allocated = 0
    for i, (weight, enemy) in enumerate(variants):
        n = max(1, round(runs * weight)) if i < len(variants) - 1 else max(1, runs - allocated)
        allocated += n
        results.append(simulate(build, enemy, n, seed=seed + i * 17, potion=potion, rule=rule))
    return merge_results(results)


def boss_variants(floor: int, curve: str, biome: str, ability: str, pity: float,
                  tuning: Tuning) -> list[tuple[float, Enemy]]:
    pool = modifier_pool(ability)
    counts = Counter(pool)
    return [(count / len(pool), make_boss(floor, curve, biome, ability, mod, pity, tuning))
            for mod, count in counts.items()]


# ---------------------------------------------------------------------------
# XP et économie
# ---------------------------------------------------------------------------


def xp_next(level: int, curve: str) -> int:
    if curve == "current":
        return round(CURRENT["XP_BASE"] * CURRENT["XP_GROWTH"] ** (level - 1))
    return round(CURRENT["XP_BASE"] * TARGET["XP_GROWTH"] ** (level - 1))


def total_xp(level: int, curve: str) -> int:
    return sum(xp_next(i, curve) for i in range(1, level))


def xp_to_100(curve: str) -> int:
    return total_xp(100, curve)


def xp_kill(floor: int, curve: str) -> tuple[int, int]:
    return make_mob(floor, curve).xp, make_boss(floor, curve).xp


def estimate_kills_to_100(curve: str, floor: int = 50) -> float:
    mob_xp, boss_xp = xp_kill(floor, curve)
    avg = (mob_xp * 10 + boss_xp) / 11
    return xp_to_100(curve) / avg


def economy_summary() -> str:
    lines = ["| Courbe | XP 1→100 | XP niv. 34 | XP niv. 100 | Kills estimés (palier 50) |",
             "|---|---:|---:|---:|---:|"]
    for curve in ("current", "proposed"):
        lines.append(
            f"| {curve} | {xp_to_100(curve):,} | {xp_next(34, curve):,} | "
            f"{xp_next(100, curve):,} | {estimate_kills_to_100(curve):,.0f} |"
        )
    lines.append("")
    lines.append("| Palier | Pas jusqu'au 100 | Jours à 5 k pas/j | Jours à 10 k pas/j |")
    lines.append("|---:|---:|---:|---:|")
    for floor in (10, 20, 36, 50, 75):
        remaining = (100 - floor) * 1000
        lines.append(f"| {floor} | {remaining:,} | {remaining / 5000:.1f} | {remaining / 10000:.1f} |")
    return "\n".join(lines)


# ---------------------------------------------------------------------------
# Rapport
# ---------------------------------------------------------------------------


def fmt(value: float | None, digits: int = 1) -> str:
    return "—" if value is None else f"{value:.{digits}f}"


def combat_table(curve: str, builds: Iterable[str], floors: Iterable[int], runs: int,
                 biome: str, ability: str, modifier: str, pity: float,
                 tuning: Tuning, potion: str = "") -> str:
    lines = [
        f"### Courbe `{curve}` — biome `{biome}` — modificateur `{modifier}` — pitié {pity:.0%}"
        + (f" — potion `{potion}`" if potion else ""),
        "",
        "| Palier | Ennemi | Build | Victoires | TTK | TTD | Coups héros | Coups ennemis | PV restants | Rations | Dégâts subis |",
        "|---:|---|---|---:|---:|---:|---:|---:|---:|---:|---:|",
    ]
    for floor in floors:
        rule = rule_for(floor, biome)
        for build_name in builds:
            build = build_preset(build_name, floor)
            label = build.name + (f"+{potion}" if potion else "")
            mob = make_mob(floor, curve, biome, tuning=tuning)
            mob_result = simulate(build, mob, runs=runs, potion=potion, rule=rule)
            lines.append(
                f"| {floor} | {mob.name} | {label} | {mob_result.win_rate:.0f} % | "
                f"{fmt(mob_result.ttk)} | {fmt(mob_result.ttd)} | {mob_result.avg_hero_hits:.0f} | "
                f"{mob_result.avg_enemy_hits:.0f} | {fmt(mob_result.avg_hp_left, 0)} | "
                f"{mob_result.avg_rations:.2f} | {fmt(mob_result.avg_damage_taken, 0)} |"
            )
            boss_ability = ability_for(floor, biome) if ability == "auto" else ability
            if modifier == "auto":
                variants = boss_variants(floor, curve, biome, boss_ability, pity, tuning)
                boss_result = simulate_mixed(build, variants, runs, potion, rule)
            else:
                boss = make_boss(floor, curve, biome, boss_ability,
                                 "" if modifier == "none" else modifier, pity, tuning)
                boss_result = simulate(build, boss, runs=runs, potion=potion, rule=rule)
            lines.append(
                f"| {floor} | gardien | {label} | {boss_result.win_rate:.0f} % | "
                f"{fmt(boss_result.ttk)} | {fmt(boss_result.ttd)} | {boss_result.avg_hero_hits:.0f} | "
                f"{boss_result.avg_enemy_hits:.0f} | {fmt(boss_result.avg_hp_left, 0)} | "
                f"{boss_result.avg_rations:.2f} | {fmt(boss_result.avg_damage_taken, 0)} |"
            )
    return "\n".join(lines)


def main() -> None:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    parser = argparse.ArgumentParser(description="Simulateur d'équilibrage")
    parser.add_argument("--curve", choices=["current", "proposed", "target", "code", "both"],
                        default="code")
    parser.add_argument("--build", default="player36,reference",
                        help="preset ou liste : player36,reference,reference_low,reference_high,"
                             "under,tank,crit,lifesteal,endgame")
    parser.add_argument("--floors", default="36,50,75,100")
    parser.add_argument("--runs", type=int, default=200)
    parser.add_argument("--biome", default="auto")
    parser.add_argument("--ability", default="auto",
                        choices=["auto", "none", "shield", "slam", "drain", "regen", "breath",
                                 "roots", "acid", "pressure", "rupture"])
    parser.add_argument("--modifier", default="auto",
                        choices=["auto", "none", "blinde", "rapide", "berserk", "regenerant", "voleur"])
    parser.add_argument("--pity", type=float, default=0.0,
                        help="pitié de gardien (0 à 0.25)")
    parser.add_argument("--potion", default="",
                        choices=["", "atk", "rage", "atk_pure", "crit", "mixed"],
                        help="atk/rage = potions à risque, atk_pure = hypothèse +40 % sans risque")
    parser.add_argument("--hp-exp", type=float, default=1.11)
    parser.add_argument("--hp-cap", type=float, default=30.0)
    parser.add_argument("--atk-exp", type=float, default=1.04)
    parser.add_argument("--atk-cap", type=float, default=6.0)
    parser.add_argument("--boss-hp-exp", type=float, default=1.08)
    parser.add_argument("--boss-hp-cap", type=float, default=20.0)
    parser.add_argument("--boss-hp-mult", type=float, default=3.0)
    parser.add_argument("--boss-atk-mult", type=float, default=1.3)
    parser.add_argument("--shield-reduction", type=float, default=0.35)
    parser.add_argument("--shield-duration", type=float, default=3.0)
    parser.add_argument("--shield-period", type=float, default=8.0)
    parser.add_argument("--target-mob-atk-div", type=float, default=8.0)
    parser.add_argument("--target-boss-atk-div", type=float, default=21.0)
    parser.add_argument("--enrage-after", type=float, default=25.0)
    parser.add_argument("--enrage-step", type=float, default=5.0)
    parser.add_argument("--enrage-pct", type=float, default=0.15)
    parser.add_argument("--enrage-cap", type=float, default=0.60)
    parser.add_argument("--xp", action="store_true", help="affiche les courbes d'XP et de pas")
    parser.add_argument("--economy", action="store_true", help="affiche le résumé XP/pas")
    args = parser.parse_args()

    if args.xp or args.economy:
        print(economy_summary())
        return

    tuning = Tuning(
        hp_exp=args.hp_exp, hp_cap=args.hp_cap,
        atk_exp=args.atk_exp, atk_cap=args.atk_cap,
        boss_hp_exp=args.boss_hp_exp, boss_hp_cap=args.boss_hp_cap,
        boss_hp_mult=args.boss_hp_mult, boss_atk_mult=args.boss_atk_mult,
        shield_reduction=args.shield_reduction,
        shield_duration=args.shield_duration, shield_period=args.shield_period,
        target_mob_atk_div=args.target_mob_atk_div,
        target_boss_atk_div=args.target_boss_atk_div,
        enrage_after=args.enrage_after, enrage_step=args.enrage_step,
        enrage_pct=args.enrage_pct, enrage_cap=args.enrage_cap,
    )
    floors = [int(x) for x in args.floors.split(",") if x.strip()]
    builds = [x.strip() for x in args.build.split(",") if x.strip()]
    if "all" in builds:
        builds = ["player36", "reference_low", "reference", "reference_high",
                  "under", "tank", "crit", "lifesteal", "endgame"]
    curves = ["current", "proposed"] if args.curve == "both" else [args.curve]
    for curve in curves:
        print(combat_table(curve, builds, floors, args.runs, args.biome,
                           args.ability, args.modifier, args.pity, tuning, args.potion))
        print()


if __name__ == "__main__":
    main()
