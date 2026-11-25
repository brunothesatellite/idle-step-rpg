"""Import d'une sauvegarde Android dans le simulateur d'équilibrage.

Le simulateur historique ne connaît que des profils théoriques. Ce module
reconstruit les statistiques réelles d'une sauvegarde JSON (version 7) avec les
mêmes formules que `statsOf()` dans `Model.kt`.

Les sauvegardes ne sont pas versionnées dans le dépôt : passez leur chemin en
argument aux scripts d'analyse.
"""

from __future__ import annotations

import json
import math
from dataclasses import dataclass
from pathlib import Path
from typing import Any

from balance_sim import Build, rule_for

ITEM_EFFECTS = {
    "vampirisme": ("lifesteal", 0.08),
    "rempart": ("dr", 0.10),
    "frenesie": ("crit_damage", 0.40),
    "chasse": ("boss_damage", 0.25),
    "vitalite": ("regen", 1.2),
    "precision": ("crit", 5.0),
}

SET_BONUSES = {
    "veilleur": (0.04, 0.04, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
    "braise": (0.0, 0.0, 0.0, 3.0, 0.0, 0.05, 0.0, 0.0),
    "givre": (0.0, 0.0, 0.05, 0.0, 0.3, 0.0, 0.0, 0.0),
    "os": (0.04, 0.04, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
    "toile": (0.0, 0.0, 0.03, 2.0, 0.0, 0.0, 0.0, 0.0),
    "cendre": (0.04, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0),
    "forge": (0.0, 0.04, 0.0, 0.0, 0.0, 0.0, 0.03, 0.0),
    "serment": (0.04, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.08),
    "bois": (0.0, 0.0, 0.04, 0.0, 0.35, 0.0, 0.0, 0.0),
    "abysse": (0.0, 0.04, 0.0, 0.0, 0.0, 0.04, 0.0, 0.0),
    "trone": (0.04, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
}

CAPS = {
    "vit": 4.0,
    "crit": 75.0,
    "lifesteal": 0.25,
    "crit_damage": 1.0,
    "boss_damage": 0.5,
    "dr": 0.40,
}


def _rint(value: float) -> int:
    return int(math.floor(value + 0.5))


def _set_bonus(name: str, tier: int) -> dict[str, float]:
    atk, hp, vit, crit, regen, ls, dr, boss = SET_BONUSES.get(
        name, (0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    )
    return {
        "atk": atk * tier,
        "hp": hp * tier,
        "vit": vit * tier,
        "crit": crit * tier,
        "regen": regen * tier,
        "ls": ls * tier,
        "dr": dr * tier,
        "boss": boss * tier,
        "crit_damage": 0.15 * tier if name == "trone" else 0.0,
    }


def base_stats(save: dict[str, Any]) -> dict[str, float]:
    """Statistiques sans règle de biome, comme la partie commune de statsOf()."""
    floor = int(save["floor"])
    level = int(save["level"])
    milestone = floor // 10
    upgrades = save.get("upgrades", {})
    blade = int(upgrades.get("blade", 0))
    breath = int(upgrades.get("breath", 0))
    swift = int(upgrades.get("swift", 0))

    base_atk = 18 + 3 * (level - 1) + 2 * milestone
    base_hp = 150 + 18 * (level - 1) + 12 * milestone
    base_vit = 1.0 + 0.01 * (level - 1)

    atk = _rint(base_atk * (1.0 + 0.015 * blade))
    hp = _rint(base_hp * (1.0 + 0.015 * breath))
    vit = base_vit * (1.0 + 0.01 * swift)
    crit = 10.0
    regen = 0.6
    lifesteal = 0.0
    dr = 0.0
    crit_damage = 0.0
    boss_damage = 0.0

    for item in save.get("equipment", []):
        atk += item.get("atk", 0)
        hp += item.get("hp", 0)
        vit += item.get("vitPct", 0) / 100.0
        crit += item.get("critPct", 0)
        regen += item.get("regen", 0.0)
        effect = ITEM_EFFECTS.get(item.get("effect", ""))
        if effect is None:
            continue
        field, value = effect
        if field == "lifesteal":
            lifesteal += value
        elif field == "dr":
            dr += value
        elif field == "crit_damage":
            crit_damage += value
        elif field == "boss_damage":
            boss_damage += value
        elif field == "regen":
            regen += value
        elif field == "crit":
            crit += value

    counts: dict[str, int] = {}
    for item in save.get("equipment", []):
        name = item.get("setName", "")
        if name:
            counts[name] = counts.get(name, 0) + 1

    set_atk = set_hp = set_vit = set_crit = set_regen = set_ls = set_dr = set_boss = set_cd = 0.0
    for name, count in counts.items():
        if count < 2:
            continue
        tier = 3 if count >= 6 else 2 if count >= 4 else 1
        bonus = _set_bonus(name, tier)
        set_atk += bonus["atk"]
        set_hp += bonus["hp"]
        set_vit += bonus["vit"]
        set_crit += bonus["crit"]
        set_regen += bonus["regen"]
        set_ls += bonus["ls"]
        set_dr += bonus["dr"]
        set_boss += bonus["boss"]
        set_cd += bonus["crit_damage"]

    atk = _rint(atk * (1.0 + set_atk))
    hp = _rint(hp * (1.0 + set_hp))
    vit += set_vit
    crit += set_crit
    regen += set_regen
    lifesteal += set_ls
    dr += set_dr
    boss_damage += set_boss
    crit_damage += set_cd

    return {
        "atk": float(atk),
        "hp": float(hp),
        "vit": vit,
        "crit": crit,
        "regen": regen,
        "lifesteal": lifesteal,
        "dr": dr,
        "crit_damage": crit_damage,
        "boss_damage": boss_damage,
        "damage_taken": 0.0,
    }


def apply_rule(stats: dict[str, float], floor: int) -> dict[str, float]:
    rule = rule_for(floor, "auto")
    out = dict(stats)
    out["atk"] *= 1.0 + rule.hero_atk
    out["hp"] *= 1.0 + rule.hero_hp
    out["vit"] *= 1.0 + rule.hero_vit
    out["crit"] += rule.hero_crit
    out["regen"] += rule.hero_regen
    out["dr"] += rule.hero_dr
    out["boss_damage"] += rule.boss_damage

    for key, cap in CAPS.items():
        out[key] = min(cap, out[key])
    return out


def build_from_stats(stats: dict[str, float], name: str, ferveur: float) -> Build:
    return Build(
        name=name,
        atk=stats["atk"],
        hp=stats["hp"],
        vit=stats["vit"],
        crit=stats["crit"],
        crit_damage=stats["crit_damage"],
        regen=stats["regen"],
        lifesteal=stats["lifesteal"],
        damage_reduction=stats["dr"],
        boss_damage=stats["boss_damage"],
        damage_taken=stats["damage_taken"],
        ferveur=ferveur,
        rations=1,
    )


@dataclass(frozen=True)
class LoadedSave:
    path: Path
    floor: int
    level: int
    ferveur: float
    base: dict[str, float]
    stats: dict[str, float]
    build: Build
    equipment_levels: tuple[int, ...]

    def build_at(self, floor: int) -> Build:
        """Même niveau et même stuff, règle de biome du palier demandé."""
        return build_from_stats(apply_rule(self.base, floor), self.build.name, self.ferveur)


def load_save(path: str | Path, name: str | None = None) -> LoadedSave:
    source = Path(path)
    save = json.loads(source.read_text(encoding="utf-8"))
    floor = int(save["floor"])
    level = int(save["level"])
    ferveur = float(save.get("ferveur", 100))
    base = base_stats(save)
    stats = apply_rule(base, floor)
    return LoadedSave(
        path=source,
        floor=floor,
        level=level,
        ferveur=ferveur,
        base=base,
        stats=stats,
        build=build_from_stats(stats, name or source.stem, ferveur),
        equipment_levels=tuple(
            int(item.get("level", 0)) for item in save.get("equipment", [])
        ),
    )


def _floor_scale(floor: int) -> float:
    return min(6.0, 1.0 + 0.055 * max(0, floor - 1))


def _rarity_multiplier(rarity: str) -> float:
    return {
        "common": 1.0,
        "uncommon": 1.15,
        "rare": 1.5,
        "epic": 1.9,
        "legendary": 2.4,
    }.get(rarity, 1.0)


def project_save(
    save: dict[str, Any],
    floor: int,
    level: int,
    gear_mode: str = "leveled",
) -> dict[str, Any]:
    """Projette une sauvegarde à un autre palier.

    `leveled` : niveau d'objet = palier, même rareté, sans forge.
    `expected` : niveau d'objet = palier, rareté +1 cran et +5 niveaux de forge.
    """
    if gear_mode not in {"leveled", "expected"}:
        raise ValueError(f"gear_mode inconnu : {gear_mode}")

    out = json.loads(json.dumps(save))
    out["floor"] = floor
    out["level"] = level
    next_rarity = {
        "common": "uncommon",
        "uncommon": "rare",
        "rare": "epic",
        "epic": "legendary",
        "legendary": "legendary",
    }
    for item in out.get("equipment", []):
        old_level = max(1, int(item.get("level", 1)))
        ratio = _floor_scale(floor) / _floor_scale(old_level)
        atk_hp_ratio = ratio
        if gear_mode == "expected":
            old_rarity = item.get("rarity", "common")
            new_rarity = next_rarity.get(old_rarity, old_rarity)
            ratio *= _rarity_multiplier(new_rarity) / _rarity_multiplier(old_rarity)
            atk_hp_ratio = ratio * 1.20  # cinq niveaux de forge, +4 % de base
            item["rarity"] = new_rarity
        item["level"] = floor
        item["maxLevel"] = floor + 20
        for key in ("atk", "hp"):
            item[key] = round(item.get(key, 0) * atk_hp_ratio)
        for key in ("vitPct", "critPct"):
            item[key] = round(item.get(key, 0) * ratio)
        item["regen"] = round(item.get("regen", 0.0) * ratio, 1)
    return out
