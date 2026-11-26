"""Rapport complet 0.25.0 : cinq profils, trois rythmes, paliers 1 -> 101.

Usage :
    python tools/equilibrage/report_progression_025.py \
        --data progression_025_full.json \
        --out docs/analyse-progression-025.md
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path

PROFILES = [
    ("passif", "Passif minimal (auto off)"),
    ("passif_auto_game", "Passif optimisé (auto on)"),
    ("normal", "Normal"),
    ("optimise", "Optimisé"),
    ("hyper", "Hyper-optimisé"),
]


def load(path: str) -> dict:
    return json.loads(Path(path).read_text(encoding="utf-8"))


def fmt(value, digits: int = 1) -> str:
    if value is None:
        return "—"
    if isinstance(value, float):
        return f"{value:.{digits}f}".replace(".", ",")
    return str(value)


def summary_map(data: dict) -> dict[tuple[str, int], dict]:
    return {(s["profile"], s["steps"]): s for s in data["summary"]}


def days_table(data: dict) -> list[str]:
    sm = summary_map(data)
    lines = [
        "| Profil | 5 000 pas/j | 8 000 pas/j | 15 000 pas/j | Palier final | Niv. héros |",
        "|---|---:|---:|---:|---:|---:|",
    ]
    for key, label in PROFILES:
        cells, floors, levels = [], [], []
        for steps in (5000, 8000, 15000):
            s = sm[(key, steps)]
            floors.append(s["floor_median"])
            levels.append(s["level_median"])
            if s["finished"] == 0:
                cells.append(f"non fini<br>(mur p.{int(s['floor_median'])})")
            else:
                cells.append(f"{fmt(s['days_median'])} j ({s['finished']}/{s['runs']})")
        lines.append(
            f"| **{label}** | {cells[0]} | {cells[1]} | {cells[2]} | "
            f"{int(max(floors))} / 101 | {fmt(max(levels))} |"
        )
    return lines


def stats_table(data: dict) -> list[str]:
    sm = summary_map(data)
    lines = [
        "| Profil | Pas/j | Jours | Niv. | Replis | Tentatives gardien | Fermes | Coffres | Or | Rations |",
        "|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|",
    ]
    for key, label in PROFILES:
        for steps in (5000, 8000, 15000):
            s = sm[(key, steps)]
            gold = f"{int(s['gold_median']):,}".replace(",", " ")
            lines.append(
                f"| {label} | {steps} | {fmt(s['days_median'])} | {fmt(s['level_median'])} | "
                f"{int(s['retreats_median'])} | {int(s['boss_attempts_median'])} | "
                f"{int(s['farm_median'])} | {int(s['chests_median'])} | {gold} | "
                f"{int(s['rations_median'])} |"
            )
    return lines


def ttk_table(data: dict, key: str, title: str) -> list[str]:
    rows = data["ttk"].get(key, [])
    lines = [
        f"#### {title}",
        "",
        "| Palier | Niv. | ATQ | PV | VIT | Crit | Camp A/S/V | TTK facile | TTK moyen | TTK coriace | TTK élite | TTK gardien | Victoire gardien | PV perdus | Rations |",
        "|---:|---:|---:|---:|---:|---:|---|---:|---:|---:|---:|---:|---:|---:|---:|",
    ]
    for r in rows:
        up = r["upgrades"]
        lines.append(
            f"| {r['floor']} | {r['level']} | {fmt(r['atk'], 0)} | {fmt(r['hp'], 0)} | "
            f"{fmt(r['vit'], 2)} | {fmt(r['crit'], 0)} | {up['blade']}/{up['breath']}/{up['swift']} | "
            f"{fmt(r.get('ttk_easy'))} | {fmt(r.get('ttk_mid'))} | {fmt(r.get('ttk_hard'))} | "
            f"{fmt(r.get('ttk_elite'))} | {fmt(r.get('ttk_boss'))} | {fmt(r.get('win_boss'), 0)} % | "
            f"{fmt(r.get('hp_loss_boss'), 0)} | {fmt(r.get('rations_boss'), 2)} |"
        )
    if not rows:
        lines.append("| — | — | — | — | — | — | — | — | — | — | — | — | — | — | — |")
    return lines


def final_boss_table(data: dict) -> list[str]:
    sm = summary_map(data)
    lines = [
        "| Profil | 5 000 pas/j | 8 000 pas/j | 15 000 pas/j |",
        "|---|---:|---:|---:|",
    ]
    for key, label in PROFILES:
        cells = []
        for steps in (5000, 8000, 15000):
            rows = {r["floor"]: r for r in data["ttk"].get(f"{key}_{steps}", [])}
            r = rows.get(100)
            if r is None:
                cells.append("non atteint")
            else:
                cells.append(f"{fmt(r.get('ttk_boss'))} s · {fmt(r.get('win_boss'), 0)} %")
        lines.append(f"| {label} | {cells[0]} | {cells[1]} | {cells[2]} |")
    return lines


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", required=True)
    parser.add_argument("--out", required=True)
    args = parser.parse_args()
    data = load(args.data)
    sm = summary_map(data)

    out: list[str] = []
    out.append("# Analyse de progression 0.25.0 — cinq profils, paliers 1 → 101")
    out.append("")
    out.append("> 5 graines par profil × rythme · médianes · le run s’arrête au début du palier 101.")
    out.append("")
    out.append("## 1. Méthode")
    out.append("")
    out.append("- **Profils** : passif minimal (auto-optimisation **désactivée**, aucun coffre/fusion/forge/camp/soin), passif optimisé (auto-optimisation + coffres + soins), normal, optimisé, hyper-optimisé.")
    out.append("- **Pas** : 10 pas = 1 m, 100 m par palier, 8 combats normaux, portail à 90 m, gardien à 100 m.")
    out.append("- **Loot** : butin adapté au niveau, pity rare tous les 5 monstres, coffres, fusions non-set pour l’auto, forge et camp avec réserves.")
    out.append("- **Combat** : monstres analytiques ; gardiens simulés avec `balance_sim.py` (capacités, modificateurs, enrage, pity, potions).")
    out.append("- **Gardien final** : PV adaptés au DPS, phases 66 %/33 %, capacité accélérée ; le simulateur approxime les phases par une ATQ ×1,08 et une période de 6 s.")
    out.append("- **Économie** : la relance du modificateur, le marchand et le respec ne sont **pas** modélisés dans les combats ; ils sont traités en test appareil.")
    out.append("")
    out.append("## 2. Jours pour terminer")
    out.append("")
    out.extend(days_table(data))
    out.append("")
    out.append("Le **passif minimal ne termine pas** : il atteint le palier 31–34, niveau 104–107, puis reste bloqué. Le **passif optimisé** termine 5/5 à tous les rythmes.")
    out.append("")
    out.append("## 3. 0.24 → 0.25 : ce qui change")
    out.append("")
    out.append("La courbe 1 → 99 est **inchangée** pour les quatre profils déjà suivis en 0.24 : mêmes jours et mêmes replis. Le lot 2 ne touche ni au butin, ni aux monstres, ni aux gardiens intermédiaires. Deux changements structurent cette version :")
    out.append("")
    out.append("- **Nouveau profil par défaut** : l’auto-optimisation étant désactivée par défaut, le « passif » de 0.24 devient le **passif optimisé** (auto on), et le **passif minimal** (auto off) apparaît comme le vrai profil subi. Il ne termine pas.")
    out.append("- **Gardien final rallongé** : la cible passe de « 10–15 s » à **20–30 s** avec des PV adaptatifs et trois phases.")
    out.append("")
    out.append("| Profil | TTK final 0.24 (8 000 pas/j) | TTK final 0.25 (8 000 pas/j) | Victoire 0.25 |")
    out.append("|---|---:|---:|---:|")
    out.append("| Passif (0.24, auto on) → Passif optimisé | 12,8 s | 35,2 s | 100 % |")
    out.append("| Normal | 15,1 s | 28,7 s | 60 % |")
    out.append("| Optimisé | 8,4 s | 21,1 s | 100 % |")
    out.append("| Hyper-optimisé | 6,0 s | 19,7 s | 100 % |")
    out.append("")
    out.append("Le combat final est donc passé d’un **pic de 6–15 s** à un vrai combat de **20–35 s**. La cible est tenue pour le normal, l’optimisé et l’hyper ; le passif optimisé dépasse à 35 s (voir § 6).")
    out.append("")
    out.append("## 4. Statistiques de run")
    out.append("")
    out.extend(stats_table(data))
    out.append("")
    out.append("## 5. TTK par palier — 8 000 pas/j")
    out.append("")
    out.append("Un tiret dans une colonne TTK signifie qu’aucun combat n’a été gagné sur les simulations, pas une donnée manquante. Les win rates sont mesurés à l’arrivée au palier, pas après le farm.")
    out.append("")
    out.extend(ttk_table(data, "passif_8000", "Passif minimal (auto off)"))
    out.append("")
    out.extend(ttk_table(data, "passif_auto_game_8000", "Passif optimisé (auto on)"))
    out.append("")
    out.extend(ttk_table(data, "normal_8000", "Normal"))
    out.append("")
    out.extend(ttk_table(data, "optimise_8000", "Optimisé"))
    out.append("")
    out.extend(ttk_table(data, "hyper_8000", "Hyper-optimisé"))
    out.append("")
    out.append("## 6. Gardien final — 20–30 s ?")
    out.append("")
    out.extend(final_boss_table(data))
    out.append("")
    out.append("Le gardien final est dans la cible pour l’optimisé et l’hyper (≈20–21 s) et pour le normal (≈28–29 s). Le **passif optimisé** est à 28 s à 5 000 pas/j, mais à **35 s à 8 000/15 000** : son DPS plus faible déclenche le plancher de PV et allonge le combat. Il gagne quand même 100 %, mais le combat devient long et peu tendu.")
    out.append("")
    out.append("## 7. Analyse par profil")
    out.append("")
    out.append("### Passif minimal — le vrai « ne rien faire »")
    out.append("")
    out.append("- **0/5** à tous les rythmes, mur aux paliers **31–34**.")
    out.append("- **5 327–7 194 replis**, **16 000–21 600 combats de farm de récupération**, niveau 104–107.")
    out.append("- Il progresse plus loin qu’en 0.23 (mur 21), grâce à la ténacité, au butin adapté et aux élites adoucies, mais il finit par bloquer.")
    out.append("- C’est le profil par défaut depuis que l’auto-optimisation est désactivée. Il faut donc un **signal clair** pour l’activer, sinon le joueur passif abandonne.")
    out.append("")
    out.append("### Passif optimisé — la version idle jouable")
    out.append("")
    out.append("- **5/5** à 5 000, 8 000 et 15 000 pas/j.")
    out.append("- **32,3 / 22,3 / 12,0 jours**, **131 / 198 / 204 replis**.")
    out.append("- Les replis restent autour de **1,3–2 par palier** : c’est mieux que 0.23 (4 177), mais encore loin de « quelques replis ».")
    out.append("- Le stuff suit mieux le rythme : à 15 000 pas/j, il termine en 12 jours sans mur.")
    out.append("- Le gardien final est son point faible à 8 000/15 000 : **35 s**, 100 % de victoire, donc long et facile.")
    out.append("")
    out.append("### Normal — finissable, encore irrégulier")
    out.append("")
    out.append("- **30,8 / 20,2 / 13,0 jours**, **92 / 129 / 260 replis**.")
    out.append("- Le rythme rapide reste défavorable : **260 replis** à 15 000, à cause des paliers 40–70 et des élites.")
    out.append("- Le gardien final est **28–29 s** avec **46–60 %** de victoire : le combat est exigeant et mémorable, sans être un mur.")
    out.append("- Les pics de gardiens restent 12, 20–21, 26, 30, 50–70 ; la ténacité/pitié les rendent finissables.")
    out.append("")
    out.append("### Optimisé — le profil récompensé")
    out.append("")
    out.append("- **31,8 / 21,5 / 11,2 jours**, **50 / 88 / 102 replis**.")
    out.append("- Le gardien final est à **19,7–21,1 s**, 100 % : rapide mais lisible, avec les phases.")
    out.append("- Les décisions de stuff, sets et forge paient. Le milieu de partie reste le plus intéressant.")
    out.append("")
    out.append("### Hyper-optimisé — facile, avec un vrai final")
    out.append("")
    out.append("- **36,0 / 22,0 / 11,5 jours**, **28 / 16 / 31 replis**.")
    out.append("- Le farm de gardiens explique le 36 jours à 5 000 pas/j : il prend du temps pour sécuriser le stuff.")
    out.append("- Le gardien final est à **19,7–20,9 s**, 100 %, avec un minimum de tension grâce aux phases.")
    out.append("- C’est le profil le plus confortable ; le jeu reste relativement facile comme demandé.")
    out.append("")
    out.append("## 8. Points à corriger")
    out.append("")
    out.append("1. **Passif minimal non finissable** : l’auto-optimisation est off par défaut. Ajouter une **suggestion au premier lancement** ou un rappel quand le joueur atteint un mur, sans réactiver le mode automatiquement.")
    out.append("2. **Gardien final du passif à 35 s** à 8 000/15 000 : baisser le plancher de PV final de **0,75 à 0,55** pour les builds à faible DPS, ou cibler 24 s. Le combat resterait gagnable mais plus court.")
    out.append("3. **Rythme rapide du normal** : 260 replis à 15 000. Le butin adapté a réduit le problème (1 234 en 0.23), mais un catch-up de stuff ou un adoucissement des élites 40–70 reste utile.")
    out.append("4. **Économie non modélisée** : la relance du modificateur et les potions de camp devraient réduire les replis en jeu réel ; à mesurer avec les prochains rapports.")
    out.append("5. **Pics 40–70** : encore irréguliers pour le normal et le passif optimisé ; la ténacité/pitié évitent le mur, mais le ressenti reste en dents de scie.")
    out.append("")
    out.append("## 9. Conclusion — ressenti attendu")
    out.append("")
    out.append("- **Passif minimal** : frustration au palier 31, après beaucoup de temps. Priorité : lui proposer l’auto-optimisation.")
    out.append("- **Passif optimisé** : expérience idle crédible, 12–32 jours, mais encore trop de replis et un final trop long à haut rythme.")
    out.append("- **Normal** : bon challenge, final réussi, mais le rythme rapide et les élites restent punitifs.")
    out.append("- **Optimisé** : le meilleur équilibre ; le final est court mais les phases et la pitié gardent un intérêt.")
    out.append("- **Hyper** : relativement facile, final réussi, farm de gardiens assumé ; le minimum de challenge est là.")
    out.append("")
    out.append("## 10. Limites")
    out.append("")
    out.append("- 5 graines, combats de monstres analytiques, phases du gardien final approximées.")
    out.append("- L’auto-optimisation simulée est une politique modèle (gloutonne, non-set) ; le jeu réel peut différer.")
    out.append("- La relance, le marchand et le respec ne sont pas dans la simulation ; ils sont validés par les tests unitaires et le test appareil.")
    out.append("- Le test de partie fraîche reste la référence pour le ressenti.")
    out.append("")
    out.append("## 11. Reproductibilité")
    out.append("")
    out.append("```bash")
    out.append("python tools/equilibrage/progression_sim.py --seeds 5 \\")
    out.append("  --profiles passif,passif_auto_game,normal,optimise,hyper \\")
    out.append("  --steps 5000,8000,15000 \\")
    out.append("  --out progression_025_full.json")
    out.append("")
    out.append("python tools/equilibrage/report_progression_025.py \\")
    out.append("  --data progression_025_full.json \\")
    out.append("  --out docs/analyse-progression-025.md")
    out.append("```")
    out.append("")

    Path(args.out).write_text("\n".join(out), encoding="utf-8")
    print(f"Rapport écrit : {args.out}")


if __name__ == "__main__":
    main()
