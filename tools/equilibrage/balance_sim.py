#!/usr/bin/env python3
"""
Simulateur d'équilibrage pour La Marche des Âmes.

But : rejouer les combats avec les formules actuelles ou proposées, sans
toucher au code Android, pour vérifier qu'un build peut franchir un palier
et qu'un objectif (niveau 100, économie d'éclats) reste atteignable.

Aucune dépendance externe : Python 3.10+ suffit.

Exemples :
    python tools/equilibrage/balance_sim.py --curve current --build player36
    python tools/equilibrage/balance_sim.py --curve proposed --build reference --floors 30,36,50,75,100
    python tools/equilibrage/balance_sim.py --curve proposed --build all --floors 36,50,100 --runs 500
    python tools/equilibrage/balance_sim.py --xp
    python tools/equilibrage/balance_sim.py --economy

Les formules sont volontairement recopiées depuis GameEngine.kt / Model.kt
(et non importées) : le simulateur doit rester un outil autonome, exécutable
hors Android, et servir de référence pour le tuning.
"""

from __future__ import annotations

import argparse
import math
import random
from dataclasses import dataclass, replace
from typing import Iterable

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
    regen: float = 1.0          # PV/s
    lifesteal: float = 0.0
    damage_reduction: float = 0.0
    boss_damage: float = 0.0
    ferveur: float = 100.0
    rations: int = 1


@dataclass(frozen=True)
class Enemy:
    name: str
    hp: float
    atk: float
    interval: float             # secondes
    is_boss: bool = False
    ability: str = ""           # "", "shield", "slam"
    shield_reduction: float = 0.0
    shield_duration: float = 0.0
    shield_period: float = 0.0
    ability_period: float = 6.5
    ability_mult: float = 1.8
    enrage_after: float = 0.0
    enrage_step: float = 5.0
    enrage_pct: float = 0.25
    enrage_cap: float = 0.75
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
# Biomes et règles
# ---------------------------------------------------------------------------


@dataclass(frozen=True)
class Rule:
    enemy_hp: float = 1.0
    enemy_atk: float = 1.0
    enemy_interval: float = 1.0
    hero_atk: float = 0.0
    hero_hp: float = 0.0
    hero_vit: float = 0.0
    hero_dr: float = 0.0
    boss_damage: float = 0.0


RULES = {
    "generic": Rule(),
    "crypts": Rule(enemy_hp=0.95),
    "catacombs": Rule(enemy_interval=1.05, hero_vit=0.06),
    "ash": Rule(enemy_atk=1.10, enemy_hp=0.95, hero_atk=0.10),
    "forge": Rule(enemy_hp=1.10, enemy_atk=0.95, hero_dr=0.06),
    "peaks": Rule(enemy_interval=0.90, hero_vit=0.08, hero_hp=-0.05),
    "woods": Rule(enemy_hp=0.95, enemy_atk=1.05, hero_vit=0.0),
    "abyss": Rule(enemy_hp=0.90, enemy_atk=1.10, hero_atk=0.12, hero_hp=-0.05),
    "throne": Rule(enemy_hp=1.10, enemy_atk=1.10, hero_hp=-0.08, boss_damage=0.12),
}


@dataclass(frozen=True)
class Tuning:
    """Paramètres de la courbe proposée, modifiables pour les balayages."""
    hp_exp: float = 1.11
    hp_cap: float = 30.0
    atk_exp: float = 1.04
    atk_cap: float = 6.0
    boss_hp_exp: float = 1.08
    boss_hp_cap: float = 20.0
    boss_hp_mult: float = 3.0
    boss_atk_mult: float = 1.3
    shield_reduction: float = 0.40
    shield_duration: float = 3.0
    shield_period: float = 8.0
    target_boss_atk_div: float = 16.0
    enrage_after: float = 15.0
    enrage_step: float = 5.0
    enrage_pct: float = 0.25
    enrage_cap: float = 0.75


def rule_for(floor: int, biome: str) -> Rule:
    if biome != "auto":
        return RULES[biome]
    # Approximation des bornes de biomes (1-10 cryptes, 11-20 catacombes, etc.).
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


def _mob_depth_current(floor: int) -> float:
    return 1.0 if floor <= 10 else min(1.12 ** (floor - 10), 40.0)


def _boss_depth_current(floor: int) -> float:
    return 1.0 if floor <= 10 else min(1.08 ** (floor - 10), 40.0)


def _mob_depth_proposed(floor: int, tuning: Tuning) -> float:
    return 1.0 if floor <= 10 else min(tuning.hp_exp ** (floor - 10), tuning.hp_cap)


def _atk_depth_proposed(floor: int, tuning: Tuning) -> float:
    return 1.0 if floor <= 10 else min(tuning.atk_exp ** (floor - 10), tuning.atk_cap)


def _boss_depth_proposed(floor: int, tuning: Tuning) -> float:
    return 1.0 if floor <= 10 else min(tuning.boss_hp_exp ** (floor - 10), tuning.boss_hp_cap)


def make_mob(floor: int, curve: str, biome: str = "auto", elite: bool = False,
             tuning: Tuning = Tuning()) -> Enemy:
    rule = rule_for(floor, biome)
    level = floor + 2 if elite else floor
    if curve == "current":
        depth = _mob_depth_current(floor)
        hp = (55 + 34 * (level - 1)) * (2.2 if elite else 1.0) * depth
        atk = (6 + 2.2 * (level - 1)) * (1.3 if elite else 1.0) * depth
        interval = 1.10 if elite else 1.25
        reward = min(6.0, math.sqrt(depth))
        gold = (6 + 3 * level) * (1.6 if elite else 1.0) * reward
        xp = (12 + 5 * level) * (1.8 if elite else 1.0) * reward
    else:
        if curve == "target":
            ref_atk = 650 + 24 * floor
            ref_hp = 2600 + 110 * floor
            ref_vit = 1.8 + 0.02 * floor
            ref_interval = max(0.30, 1.0 / (ref_vit * 1.4))
            ref_dps = ref_atk * 1.18 / ref_interval
            ref_ehp = (ref_hp * 1.35) / 0.94
            hp = ref_dps * 5.0 * (1.8 if elite else 1.0)
            atk = (ref_ehp / 7.0) * (1.15 if elite else 1.0)
            interval = 1.10 if elite else 1.25
            mult = 1.0 + 0.15 * (floor - 1)
            gold = (6 + 3 * level) * (1.6 if elite else 1.0) * mult * 0.5
            xp = (12 + 5 * level) * (1.8 if elite else 1.0) * mult
        else:
            hp = (55 + 34 * (level - 1)) * (1.8 if elite else 1.0) * _mob_depth_proposed(floor, tuning)
            atk = (6 + 2.2 * (level - 1)) * (1.15 if elite else 1.0) * _atk_depth_proposed(floor, tuning)
            interval = 1.10 if elite else 1.25
            mult = 1.0 + 0.15 * (floor - 1)
            gold = (6 + 3 * level) * (1.6 if elite else 1.0) * mult * 0.5
            xp = (12 + 5 * level) * (1.8 if elite else 1.0) * mult
    return Enemy(
        name="élite" if elite else "monstre",
        hp=hp * rule.enemy_hp,
        atk=atk * rule.enemy_atk,
        interval=interval * rule.enemy_interval,
        xp=round(xp),
        gold=round(gold),
    )


def make_boss(floor: int, curve: str, biome: str = "auto", ability: str = "shield",
              tuning: Tuning = Tuning()) -> Enemy:
    rule = rule_for(floor, biome)
    if curve == "current":
        level = floor + 2
        depth = _boss_depth_current(floor)
        hp = 6.0 * (55 + 34 * (level - 1)) * depth
        atk = 1.6 * (6 + 2.2 * (level - 1)) * depth
        shield_reduction, shield_duration, shield_period = 0.65, 4.5, 6.5
        ability_mult = 1.8
        reward = min(10.0, math.sqrt(depth))
        gold = 12 * (6 + 3 * level) * reward
        xp = 10 * (12 + 5 * level) * reward
    else:
        level = floor + 1
        if curve == "target":
            ref_atk = 650 + 24 * floor
            ref_hp = 2600 + 110 * floor
            ref_vit = 1.8 + 0.02 * floor
            ref_interval = max(0.30, 1.0 / (ref_vit * 1.4))
            ref_dps = ref_atk * 1.18 / ref_interval
            ref_ehp = (ref_hp * 1.35) / 0.94
            hp = ref_dps * 18.0 * 0.85
            atk = ref_ehp / tuning.target_boss_atk_div
        else:
            hp = tuning.boss_hp_mult * (55 + 34 * (level - 1)) * _boss_depth_proposed(floor, tuning)
            atk = tuning.boss_atk_mult * (6 + 2.2 * (level - 1)) * _atk_depth_proposed(floor, tuning)
        shield_reduction = tuning.shield_reduction
        shield_duration = tuning.shield_duration
        shield_period = tuning.shield_period
        ability_mult = 1.5
        mult = 1.0 + 0.15 * (floor - 1)
        gold = 6 * (6 + 3 * level) * mult * 0.5
        xp = 10 * (12 + 5 * level) * mult
    return Enemy(
        name="gardien",
        hp=hp * rule.enemy_hp,
        atk=atk * rule.enemy_atk,
        interval=1.25 * rule.enemy_interval,
        is_boss=True,
        ability=ability,
        shield_reduction=shield_reduction,
        shield_duration=shield_duration,
        shield_period=shield_period,
        ability_mult=ability_mult,
        enrage_after=tuning.enrage_after,
        enrage_step=tuning.enrage_step,
        enrage_pct=tuning.enrage_pct,
        enrage_cap=tuning.enrage_cap,
        xp=round(xp),
        gold=round(gold),
    )


# ---------------------------------------------------------------------------
# Builds de référence
# ---------------------------------------------------------------------------


def build_preset(name: str, floor: int) -> Build:
    if name == "player36":
        # Capture du joueur : palier 36, niveau 34.
        return Build("player36", atk=1349, hp=3699, vit=2.27, crit=30,
                     regen=1.0, damage_reduction=0.06, ferveur=100, rations=1)
    if name == "endgame":
        return Build("endgame", atk=6000, hp=15000, vit=3.5, crit=40,
                     crit_damage=0.2, regen=3.0, lifesteal=0.08,
                     damage_reduction=0.10, ferveur=100, rations=2)
    # Builds "reference" calés sur le palier : stuff attendu, camp ~palier/2.
    def profile(prefix: str, atk_base: float, atk_floor: float,
                hp_base: float, hp_floor: float, vit_base: float,
                vit_floor: float) -> Build:
        return Build(
            prefix,
            atk=atk_base + atk_floor * floor,
            hp=hp_base + hp_floor * floor,
            vit=vit_base + vit_floor * floor,
            crit=30, regen=1.0, damage_reduction=0.06,
            ferveur=100, rations=1,
        )

    low = profile("reference_low", 500, 18, 2200, 70, 1.70, 0.015)
    mid = profile("reference", 650, 24, 2600, 110, 1.80, 0.020)
    high = profile("reference_high", 800, 30, 3000, 160, 1.90, 0.025)
    if name == "reference_low":
        return low
    if name == "reference":
        return mid
    if name == "reference_high":
        return high
    if name == "under":
        return replace(mid, name="under", atk=mid.atk * 0.7, hp=mid.hp * 0.7)
    if name == "tank":
        return replace(mid, name="tank", atk=mid.atk * 0.8, hp=mid.hp * 1.4,
                       regen=3.0, damage_reduction=0.16)
    if name == "crit":
        return replace(mid, name="crit", atk=mid.atk * 0.9, crit=60,
                       crit_damage=0.5)
    if name == "lifesteal":
        return replace(mid, name="lifesteal", atk=mid.atk * 0.9, hp=mid.hp * 0.9,
                       lifesteal=0.12)
    raise SystemExit(f"build inconnu : {name}")


# ---------------------------------------------------------------------------
# Simulation de combat
# ---------------------------------------------------------------------------


def simulate(build: Build, enemy: Enemy, runs: int = 200, max_seconds: float = 180.0,
             seed: int = 42, potion: str = "") -> Result:
    if potion == "atk":
        build = replace(build, atk=build.atk * 1.40)
    elif potion == "crit":
        build = replace(build, crit=min(100.0, build.crit + 50.0),
                        crit_damage=build.crit_damage + 0.20)
    elif potion == "mixed":
        build = replace(build, atk=build.atk * 1.25,
                        crit=min(100.0, build.crit + 25.0))
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
    attack_interval = max(0.30, 1.0 / (build.vit * (1.0 + 0.004 * build.ferveur)))

    for _ in range(runs):
        hp = build.hp
        enemy_hp = enemy.hp
        t = 0.0
        hero_timer = 0.0
        enemy_timer = enemy.interval
        ability_timer = enemy.ability_period
        shield_timer = 0.0
        ration_used = False
        taken = 0.0
        hero_swings = 0
        enemy_swings = 0

        while t < max_seconds:
            dt = 0.01
            t += dt
            hero_timer -= dt
            enemy_timer -= dt
            ability_timer -= dt
            if shield_timer > 0:
                shield_timer -= dt

            if shield_timer <= 0 and enemy.ability == "shield" and ability_timer <= 0:
                shield_timer = enemy.shield_duration
                ability_timer = enemy.shield_period

            if hero_timer <= 0:
                hero_swings += 1
                dmg = build.atk * (0.9 + rng.random() * 0.1)
                if rng.random() < min(1.0, build.crit / 100.0):
                    dmg *= 1.6 + build.crit_damage
                if enemy.is_boss:
                    dmg *= 1.0 + build.boss_damage
                if shield_timer > 0:
                    dmg *= 1.0 - enemy.shield_reduction
                enemy_hp -= dmg
                if build.lifesteal > 0:
                    hp = min(build.hp, hp + dmg * build.lifesteal)
                hero_timer += attack_interval
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
                enrage = 1.0
                if enemy.enrage_after > 0 and t >= enemy.enrage_after:
                    steps = int((t - enemy.enrage_after) / enemy.enrage_step) + 1
                    enrage += min(enemy.enrage_cap, steps * enemy.enrage_pct)
                dmg = enemy.atk * (0.9 + rng.random() * 0.1) * enrage * (1.0 - build.damage_reduction)
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
                if not ration_used and build.rations > 0 and hp < build.hp * 0.25:
                    ration_used = True
                    hp = min(build.hp, hp + build.hp * 0.35)

            if enemy.ability == "slam" and ability_timer <= 0:
                enemy_swings += 1
                enrage = 1.0
                if enemy.enrage_after > 0 and t >= enemy.enrage_after:
                    steps = int((t - enemy.enrage_after) / enemy.enrage_step) + 1
                    enrage += min(enemy.enrage_cap, steps * enemy.enrage_pct)
                dmg = enemy.atk * enemy.ability_mult * enrage * (1.0 - build.damage_reduction)
                hp -= dmg
                taken += dmg
                ability_timer = enemy.ability_period
                if hp <= 0:
                    loss_times.append(t)
                    times.append(t)
                    hp_left.append(0.0)
                    rations.append(1 if ration_used else 0)
                    damage_taken.append(taken)
                    hero_hits.append(hero_swings)
                    enemy_hits.append(enemy_swings)
                    break

            hp = min(build.hp, hp + build.regen * dt)

        else:
            # timeout = défaite
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


# ---------------------------------------------------------------------------
# XP et économie
# ---------------------------------------------------------------------------


def xp_next(level: int, curve: str) -> int:
    if curve == "current":
        return round(120 * 1.2 ** (level - 1))
    return round(120 * 1.09 ** (level - 1))


def total_xp(level: int, curve: str) -> int:
    return sum(xp_next(i, curve) for i in range(1, level))


def xp_to_100(curve: str) -> int:
    return total_xp(100, curve)


def xp_kill(floor: int, curve: str) -> tuple[int, int]:
    mob = make_mob(floor, curve)
    boss = make_boss(floor, curve)
    return mob.xp, boss.xp


def estimate_kills_to_100(curve: str, floor: int = 50) -> float:
    """Estimation grossière : XP totale / XP moyen par kill au palier `floor`."""
    mob_xp, boss_xp = xp_kill(floor, curve)
    avg = (mob_xp * 10 + boss_xp) / 11
    return xp_to_100(curve) / avg


def economy_summary() -> str:
    lines = []
    lines.append("| Courbe | XP 1→100 | XP niv. 34 | XP niv. 100 | Kills estimés (palier 50) |")
    lines.append("|---|---:|---:|---:|---:|")
    for curve in ("current", "proposed"):
        lines.append(
            f"| {curve} | {xp_to_100(curve):,} | {xp_next(34, curve):,} | "
            f"{xp_next(100, curve):,} | {estimate_kills_to_100(curve):,.0f} |"
        )
    lines.append("")
    lines.append("| Palier | Pas jusqu'au 100 | Jours à 5 k pas/j | Jours à 10 k pas/j |")
    lines.append("|---:|---:|---:|---:|")
    for floor in (10, 20, 36, 50, 75):
        steps = (100 - floor) * 1000
        lines.append(f"| {floor} | {steps:,} | {steps/5000:.1f} | {steps/10000:.1f} |")
    return "\n".join(lines)


# ---------------------------------------------------------------------------
# Sortie
# ---------------------------------------------------------------------------


def fmt(value: float | None, digits: int = 1) -> str:
    return "—" if value is None else f"{value:.{digits}f}"


def combat_table(curve: str, builds: Iterable[str], floors: Iterable[int], runs: int,
                 biome: str, ability: str, tuning: Tuning = Tuning(),
                 potion: str = "") -> str:
    lines = [
        f"### Courbe `{curve}` — biome `{biome}` — {runs} simulations"
        + (f" — potion `{potion}`" if potion else ""),
        "",
        "| Palier | Ennemi | Build | Victoires | TTK | TTD | Coups héros | Coups ennemis | PV restants | Rations | Dégâts subis |",
        "|---:|---|---|---:|---:|---:|---:|---:|---:|---:|---:|",
    ]
    for floor in floors:
        for build_name in builds:
            build = build_preset(build_name, floor)
            label = build.name + (f"+{potion}" if potion else "")
            enemies = [make_mob(floor, curve, biome, tuning=tuning)]
            enemies.append(make_boss(floor, curve, biome, ability, tuning=tuning))
            for enemy in enemies:
                result = simulate(build, enemy, runs=runs, potion=potion)
                lines.append(
                    f"| {floor} | {enemy.name} | {label} | {result.win_rate:.0f} % | "
                    f"{fmt(result.ttk)} | {fmt(result.ttd)} | {result.avg_hero_hits:.0f} | "
                    f"{result.avg_enemy_hits:.0f} | {fmt(result.avg_hp_left, 0)} | "
                    f"{result.avg_rations:.2f} | {fmt(result.avg_damage_taken, 0)} |"
                )
    return "\n".join(lines)


def main() -> None:
    parser = argparse.ArgumentParser(description="Simulateur d'équilibrage")
    parser.add_argument("--curve", choices=["current", "proposed", "target", "both"], default="both")
    parser.add_argument("--build", default="player36,reference",
                        help="preset ou liste : player36,reference,reference_low,reference_high,"
                             "under,tank,crit,lifesteal,endgame")
    parser.add_argument("--floors", default="30,34,36,40,50,75,100")
    parser.add_argument("--runs", type=int, default=200)
    parser.add_argument("--biome", default="auto")
    parser.add_argument("--ability", default="shield", choices=["shield", "slam", ""])
    parser.add_argument("--potion", default="", choices=["", "atk", "crit", "mixed"],
                        help="simule une potion (atk +40%%, crit +50 pts, ou mixte)")
    parser.add_argument("--hp-exp", type=float, default=1.11)
    parser.add_argument("--hp-cap", type=float, default=30.0)
    parser.add_argument("--atk-exp", type=float, default=1.04)
    parser.add_argument("--atk-cap", type=float, default=6.0)
    parser.add_argument("--boss-hp-exp", type=float, default=1.08)
    parser.add_argument("--boss-hp-cap", type=float, default=20.0)
    parser.add_argument("--boss-hp-mult", type=float, default=3.0)
    parser.add_argument("--boss-atk-mult", type=float, default=1.3)
    parser.add_argument("--shield-reduction", type=float, default=0.40)
    parser.add_argument("--shield-duration", type=float, default=3.0)
    parser.add_argument("--shield-period", type=float, default=8.0)
    parser.add_argument("--target-boss-atk-div", type=float, default=16.0)
    parser.add_argument("--enrage-after", type=float, default=15.0)
    parser.add_argument("--enrage-step", type=float, default=5.0)
    parser.add_argument("--enrage-pct", type=float, default=0.25)
    parser.add_argument("--enrage-cap", type=float, default=0.75)
    parser.add_argument("--xp", action="store_true", help="affiche les courbes d'XP et de pas")
    parser.add_argument("--economy", action="store_true", help="affiche le résumé XP/pas")
    args = parser.parse_args()

    if args.xp or args.economy:
        print(economy_summary())
        return

    tuning = Tuning(
        hp_exp=args.hp_exp,
        hp_cap=args.hp_cap,
        atk_exp=args.atk_exp,
        atk_cap=args.atk_cap,
        boss_hp_exp=args.boss_hp_exp,
        boss_hp_cap=args.boss_hp_cap,
        boss_hp_mult=args.boss_hp_mult,
        boss_atk_mult=args.boss_atk_mult,
        shield_reduction=args.shield_reduction,
        shield_duration=args.shield_duration,
        shield_period=args.shield_period,
        target_boss_atk_div=args.target_boss_atk_div,
        enrage_after=args.enrage_after,
        enrage_step=args.enrage_step,
        enrage_pct=args.enrage_pct,
        enrage_cap=args.enrage_cap,
    )
    floors = [int(x) for x in args.floors.split(",") if x.strip()]
    builds = [x.strip() for x in args.build.split(",") if x.strip()]
    if "all" in builds:
        builds = ["player36", "reference_low", "reference", "reference_high",
                  "under", "tank", "crit", "lifesteal", "endgame"]
    curves = ["current", "proposed"] if args.curve == "both" else [args.curve]
    for curve in curves:
        print(combat_table(curve, builds, floors, args.runs, args.biome, args.ability,
                           tuning, args.potion))
        print()


if __name__ == "__main__":
    main()
