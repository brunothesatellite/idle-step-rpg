"""Expériences d'équilibrage du début de jeu (paliers 1 à 36).

Trois leviers sont comparés sans modifier le jeu :

1. courbe ennemie early game (multiplicateur dégressif jusqu'au palier 20) ;
2. écart de niveau héros / palier (les ennemis gagnent des PV/ATQ si le héros
   est surlevelé) ;
3. rythme d'XP (XP des gardiens et malus de surlevel).

Usage :
    python tools/equilibrage/earlygame_experiment.py --save C:/chemin/lowlevel.json
    python tools/equilibrage/earlygame_experiment.py --save ... --out docs/analyse-earlygame.md
"""

from __future__ import annotations

import argparse
import json
import math
import sys
from dataclasses import dataclass, replace
from pathlib import Path

from balance_sim import (
    Build,
    Enemy,
    Rule,
    Tuning,
    ability_for,
    boss_variants,
    build_preset,
    make_mob,
    rule_for,
    simulate,
    simulate_mixed,
)
from save_import import (
    LoadedSave,
    apply_rule,
    base_stats,
    build_from_stats,
    load_save,
    project_save,
)


@dataclass(frozen=True)
class EarlyConfig:
    name: str
    mob_hp: float = 1.0
    boss_hp: float = 1.0
    atk: float = 1.0
    end_floor: int = 20
    gap_per_level: float = 0.0
    gap_cap: float = 1.6
    gap_free: int = 4


@dataclass(frozen=True)
class ResultRow:
    scenario: str
    floor: int
    mob_win: float
    mob_ttk: float | None
    mob_hp_pct: float
    boss_win: float
    boss_ttk: float | None
    boss_hp_pct: float
    boss_rations: float


def _rint(value: float) -> int:
    return int(math.floor(value + 0.5))


def xp_next(level: int) -> int:
    return _rint(120.0 * (1.09 ** (level - 1)))


def xp_floor_multiplier(floor: int) -> float:
    return 1.0 + 0.12 * (floor - 1)


def xp_projection(
    boss_mult: float,
    overlevel_penalty: float,
    mobs_per_floor: int = 7,
    free_gap: int = 4,
    max_floor: int = 36,
) -> dict[int, int]:
    level = 1
    xp = 0
    rows: dict[int, int] = {}
    for floor in range(1, max_floor + 1):
        mob = (12 + 5 * floor) * xp_floor_multiplier(floor)
        gain = mobs_per_floor * mob + boss_mult * mob
        if level > floor + free_gap:
            gain *= 1.0 - overlevel_penalty
        xp += gain
        while xp >= xp_next(level):
            xp -= xp_next(level)
            level += 1
        rows[floor] = level
    return rows


def xp_level_at(target_floor: int, mobs_per_floor: int = 7) -> int:
    """Niveau atteint en jouant normalement jusqu'au palier demandé."""
    return xp_projection(10.0, 0.0, mobs_per_floor, max_floor=target_floor)[target_floor]


def early_factor(value: float, floor: int, end_floor: int) -> float:
    if floor >= end_floor:
        return 1.0
    return 1.0 + (value - 1.0) * (1.0 - floor / end_floor)


def gap_factor(config: EarlyConfig, level: int, floor: int) -> float:
    if config.gap_per_level <= 0.0:
        return 1.0
    gap = max(0, level - floor - config.gap_free)
    return min(config.gap_cap, 1.0 + config.gap_per_level * gap)


def scale_enemy(enemy: Enemy, hp: float, atk: float) -> Enemy:
    return replace(enemy, hp=enemy.hp * hp, atk=enemy.atk * atk)


def apply_rule_to_build(build: Build, floor: int) -> Build:
    rule = rule_for(floor, "auto")
    return replace(
        build,
        atk=build.atk * (1.0 + rule.hero_atk),
        hp=build.hp * (1.0 + rule.hero_hp),
        vit=min(4.0, build.vit * (1.0 + rule.hero_vit)),
        crit=min(75.0, build.crit + rule.hero_crit),
        regen=build.regen + rule.hero_regen,
        damage_reduction=min(0.40, build.damage_reduction + rule.hero_dr),
        boss_damage=min(0.50, build.boss_damage + rule.boss_damage),
    )


def simulate_row(
    scenario: str,
    build: Build,
    floor: int,
    level: int,
    config: EarlyConfig,
    runs: int,
) -> ResultRow:
    hp_mult = early_factor(config.mob_hp, floor, config.end_floor) * gap_factor(config, level, floor)
    atk_mult = early_factor(config.atk, floor, config.end_floor) * gap_factor(config, level, floor)

    mob = scale_enemy(make_mob(floor, "code", "auto"), hp_mult, atk_mult)
    mob_result = simulate(build, mob, runs=runs, rule=Rule())

    boss_hp_mult = early_factor(config.boss_hp, floor, config.end_floor) * gap_factor(config, level, floor)
    variants = [
        (weight, scale_enemy(enemy, boss_hp_mult, atk_mult))
        for weight, enemy in boss_variants(
            floor, "code", "auto", ability_for(floor, "auto"), 0.0, Tuning()
        )
    ]
    boss_result = simulate_mixed(build, variants, runs, "", Rule())

    return ResultRow(
        scenario=scenario,
        floor=floor,
        mob_win=mob_result.win_rate,
        mob_ttk=mob_result.avg_time,
        mob_hp_pct=100.0 * mob_result.avg_hp_left / max(1.0, build.hp),
        boss_win=boss_result.win_rate,
        boss_ttk=boss_result.avg_time,
        boss_hp_pct=100.0 * boss_result.avg_hp_left / max(1.0, build.hp),
        boss_rations=boss_result.avg_rations,
    )


def fmt_seconds(value: float | None) -> str:
    return "—" if value is None else f"{value:.1f} s"


def format_row(row: ResultRow) -> str:
    return (
        f"| {row.scenario} | {row.floor} | {row.mob_win:.0f} % | {fmt_seconds(row.mob_ttk)} | "
        f"{row.mob_hp_pct:.0f} % | {row.boss_win:.0f} % | {fmt_seconds(row.boss_ttk)} | "
        f"{row.boss_hp_pct:.0f} % | {row.boss_rations:.2f} |"
    )


def configs() -> list[EarlyConfig]:
    return [
        EarlyConfig("référence actuelle"),
        EarlyConfig("early doux · PV m×3 · g×2,5 · ATQ×1,5", mob_hp=3.0, boss_hp=2.5, atk=1.5),
        EarlyConfig("early moyen · PV m×4 · g×3 · ATQ×1,75", mob_hp=4.0, boss_hp=3.0, atk=1.75),
        EarlyConfig("early fort · PV m×5 · g×4 · ATQ×2", mob_hp=5.0, boss_hp=4.0, atk=2.0),
        EarlyConfig("écart de niveau 3 %/niv · cap×1,5", gap_per_level=0.03, gap_cap=1.5),
        EarlyConfig("écart de niveau 4 %/niv · cap×1,6", gap_per_level=0.04),
        EarlyConfig(
            "early doux + écart 3 %",
            mob_hp=3.0, boss_hp=2.5, atk=1.5,
            gap_per_level=0.03, gap_cap=1.5,
        ),
        EarlyConfig(
            "early moyen + écart 3 %",
            mob_hp=4.0, boss_hp=3.0, atk=1.75,
            gap_per_level=0.03, gap_cap=1.5,
        ),
        EarlyConfig(
            "early fort + écart de niveau",
            mob_hp=5.0, boss_hp=4.0, atk=2.0, gap_per_level=0.04,
        ),
    ]


def low_build_at(
    save: LoadedSave, raw: dict, floor: int, gear_mode: str
) -> tuple[Build, int]:
    if gear_mode == "frozen":
        return save.build_at(floor), save.level
    level = xp_level_at(floor)
    doc = project_save(raw, floor, level, gear_mode)
    stats = apply_rule(base_stats(doc), floor)
    return build_from_stats(stats, "LOW", save.ferveur), level


def low_rows(save: LoadedSave, raw: dict, runs: int, gear_mode: str) -> list[ResultRow]:
    rows: list[ResultRow] = []
    for config in configs():
        for floor in (9, 12, 15, 20):
            build, level = low_build_at(save, raw, floor, gear_mode)
            rows.append(simulate_row(config.name, build, floor, level, config, runs))
    return rows


def reference_rows(runs: int) -> list[ResultRow]:
    rows: list[ResultRow] = []
    for config in configs():
        for floor in (9, 15, 20, 25, 36):
            build = apply_rule_to_build(build_preset("reference", floor), floor)
            rows.append(simulate_row(config.name, build, floor, floor, config, runs))
    return rows


def xp_table() -> list[str]:
    variants = [
        ("XP actuelle · gardien ×10", 10.0, 0.0),
        ("gardien ×5", 5.0, 0.0),
        ("XP actuelle · malus surlevel −50 %", 10.0, 0.50),
        ("gardien ×5 · malus surlevel −50 %", 5.0, 0.50),
        ("gardien ×5 · malus surlevel −75 %", 5.0, 0.75),
    ]
    lines = [
        "| Règle XP | Niv. 5 | Niv. 9 | Niv. 15 | Niv. 20 | Niv. 25 | Niv. 30 | Niv. 36 |",
        "|---|---:|---:|---:|---:|---:|---:|---:|",
    ]
    for label, boss, penalty in variants:
        levels = xp_projection(boss, penalty)
        lines.append(
            f"| {label} | " + " | ".join(str(levels[f]) for f in (5, 9, 15, 20, 25, 30, 36)) + " |"
        )
    return lines


def main() -> None:
    parser = argparse.ArgumentParser(description="Expériences début de jeu")
    parser.add_argument("--save", required=True, help="sauvegarde JSON à importer")
    parser.add_argument("--runs", type=int, default=1200)
    parser.add_argument("--out", default="docs/analyse-earlygame-data.md")
    args = parser.parse_args()

    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")

    save = load_save(args.save, name="LOW")
    raw = json.loads(save.path.read_text(encoding="utf-8"))
    sections = {
        "figé": low_rows(save, raw, args.runs, "frozen"),
        "nivelé": low_rows(save, raw, args.runs, "leveled"),
        "attendu": low_rows(save, raw, args.runs, "expected"),
    }
    reference = reference_rows(args.runs)
    xp_lines = xp_table()

    def print_section(title: str, rows: list[ResultRow]) -> None:
        print(f"\n## {title}\n")
        print("| Scénario | Palier | Monstre | TTK | PV restants | Gardien | TTK | PV restants | Rations |")
        print("|---|---:|---:|---:|---:|---:|---:|---:|---:|")
        for row in rows:
            print(format_row(row))

    print(f"Sauvegarde : {save.path}")
    print(f"Palier {save.floor} · niveau {save.level} · PV {save.stats['hp']:.0f} · ATQ {save.stats['atk']:.0f}")
    for title, rows in sections.items():
        print_section(f"LOW — stuff {title}", rows)
    print_section("REFERENCE", reference)
    print("\n## XP\n")
    print("\n".join(xp_lines))

    out = Path(args.out)
    if out:
        lines = [
            "# Analyse du début de jeu — expériences simulateur",
            "",
            "> Généré par `tools/equilibrage/earlygame_experiment.py`. Aucune constante du jeu n’est modifiée.",
            "",
            f"Sauvegarde importée : palier **{save.floor}**, niveau **{save.level}**, "
            f"ATQ **{save.stats['atk']:.0f}**, PV **{save.stats['hp']:.0f}**.",
            "",
        ]
        for title, rows in sections.items():
            lines += [
                f"## Build LOW — stuff {title}",
                "",
                "| Scénario | Palier | Monstre | TTK | PV restants | Gardien | TTK | PV restants | Rations |",
                "|---|---:|---:|---:|---:|---:|---:|---:|---:|",
                *[format_row(row) for row in rows],
                "",
            ]
        lines += [
            "## Build `reference`",
            "",
            "| Scénario | Palier | Monstre | TTK | PV restants | Gardien | TTK | PV restants | Rations |",
            "|---|---:|---:|---:|---:|---:|---:|---:|---:|",
            *[format_row(row) for row in reference],
            "",
            "## Rythme d’XP",
            "",
            *xp_lines,
            "",
        ]
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text("\n".join(lines), encoding="utf-8")
        print(f"\nRapport écrit : {out}")


if __name__ == "__main__":
    main()
