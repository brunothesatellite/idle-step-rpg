"""Détecte les séquences de mojibake dans les fichiers texte du dépôt.

Un fichier UTF-8 relu par erreur en Windows-1252 puis réenregistré en UTF-8
transforme par exemple « × » en « Ã— », « — » en « â€” » ou « • » en « â€¢ ».
Ces séquences sont valides en UTF-8, donc le compilateur ne les signale pas :
elles s'affichent simplement mal dans l'interface.

Usage : python tools/check_encoding.py
Code retour 0 si tout est propre, 1 sinon.
"""

from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

SKIP_DIRS = {
    ".git",
    ".gradle",
    ".idea",
    ".venv",
    "build",
    "dist",
    "node_modules",
    "out",
    "venv",
}

TEXT_EXTENSIONS = {
    ".css",
    ".gradle",
    ".html",
    ".htm",
    ".json",
    ".js",
    ".kt",
    ".kts",
    ".md",
    ".properties",
    ".py",
    ".svg",
    ".toml",
    ".ts",
    ".txt",
    ".xml",
    ".yaml",
    ".yml",
}

# Motifs assez spécifiques pour ne pas confondre avec du texte français normal.
# « Â » seul est laissé tranquille : « ÂME » est légitime.
PATTERNS = {
    "\u00c3": "Ã (préfixe de mojibake)",
    "\u00e2\u20ac": "â€ (ponctuation UTF-8 relue en cp1252)",
    "\u00e2\u2020": "â† (flèche UTF-8 relue en cp1252)",
    "\u00e2\u02dc": "â˜ (symbole UTF-8 relu en cp1252)",
    "\u00c5\u201c": "Å“ (œ mal encodé)",
    "\u00c5\u2019": "Å’ (Œ mal encodé)",
    "\u00c4\u2122": "Ä™ (ę/™ mal encodé)",
    "\u00c4\u203a": "Ä› (ě mal encodé)",
    "\ufffd": "caractère de remplacement U+FFFD",
}


def should_scan(path: Path) -> bool:
    if path.suffix.lower() not in TEXT_EXTENSIONS:
        return False
    if path.resolve() == Path(__file__).resolve():
        return False
    return not any(part in SKIP_DIRS for part in path.parts)


def main() -> int:
    problems: list[str] = []

    for path in sorted(ROOT.rglob("*")):
        if not path.is_file() or not should_scan(path):
            continue

        try:
            with open(path, "r", encoding="utf-8") as handle:
                lines = handle.read().splitlines()
        except UnicodeDecodeError as exc:
            problems.append(f"{path.relative_to(ROOT)}: fichier non UTF-8 ({exc})")
            continue

        for number, line in enumerate(lines, 1):
            for pattern, label in PATTERNS.items():
                if pattern in line:
                    problems.append(
                        f"{path.relative_to(ROOT)}:{number}: {label} — {line.strip()[:120]}"
                    )

    if problems:
        print("Encodage corrompu détecté :")
        for problem in problems:
            print(f"  - {problem}")
        print()
        print("Corrige les séquences ci-dessus en UTF-8 propre, sans changer la logique.")
        return 1

    print("Encodage propre : aucun mojibake détecté.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
