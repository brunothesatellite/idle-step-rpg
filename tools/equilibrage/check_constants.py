"""Vérifie que `Balance.kt` et `balance_constants.py` restent alignés.

Usage : python tools/equilibrage/check_constants.py
Code retour 0 si tout est aligné, 1 sinon.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

from balance_constants import CURRENT, TARGET

ROOT = Path(__file__).resolve().parents[2]
BALANCE_KT = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "main"
    / "java"
    / "com"
    / "brunothesatellite"
    / "idlesteprpg"
    / "engine"
    / "Balance.kt"
)

CONST_RE = re.compile(r"^\s*const val ([A-Z0-9_]+) = ([0-9_]+(?:\.[0-9]+)?)\s*$")


def parse_kotlin(path: Path) -> tuple[dict[str, float], dict[str, float]]:
    current: dict[str, float] = {}
    target: dict[str, float] = {}
    scope = current
    for line in path.read_text(encoding="utf-8").splitlines():
        if re.match(r"^\s*object Target\s*\{", line):
            scope = target
            continue
        match = CONST_RE.match(line)
        if not match:
            continue
        name, raw = match.group(1), match.group(2).replace("_", "")
        scope[name] = float(raw)
    return current, target


def compare(name: str, kotlin: dict[str, float], mirror: dict[str, float]) -> list[str]:
    errors: list[str] = []
    for key in sorted(set(kotlin) | set(mirror)):
        if key not in kotlin:
            errors.append(f"{name}.{key} présent côté Python seulement")
        elif key not in mirror:
            errors.append(f"{name}.{key} présent côté Kotlin seulement")
        elif abs(kotlin[key] - mirror[key]) > 1e-9:
            errors.append(
                f"{name}.{key} : Kotlin={kotlin[key]} / Python={mirror[key]}"
            )
    return errors


def main() -> int:
    if not BALANCE_KT.exists():
        print(f"Balance.kt introuvable : {BALANCE_KT}")
        return 1

    kotlin_current, kotlin_target = parse_kotlin(BALANCE_KT)
    errors = compare("CURRENT", kotlin_current, CURRENT)
    errors += compare("TARGET", kotlin_target, TARGET)

    if errors:
        print("Constantes désynchronisées :")
        for error in errors:
            print(f"  - {error}")
        return 1

    print(
        f"Constantes alignées : {len(kotlin_current)} courantes, "
        f"{len(kotlin_target)} cibles."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
