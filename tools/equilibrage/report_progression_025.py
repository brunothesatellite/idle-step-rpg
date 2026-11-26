"""Rapport complet 0.25.0 : cinq profils, trois rythmes, paliers 1 -> 101.

Usage :
    python tools/equilibrage/report_progression_025.py \
        --data progression_025_full.json \
        --out docs/analyse-progression-025.md
"""

from __future__ import annotations

import argparse
import json
import statistics
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


def steps_median_map(data: dict) -> dict[tuple[str, int], float]:
    groups: dict[tuple[str, int], list[int]] = {}
    for run in data["runs"]:
        groups.setdefault((run["profile"], run["steps"]), []).append(run["steps_total"])
    return {key: statistics.median(values) for key, values in groups.items()}


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
    steps_med = steps_median_map(data)
    lines = [
        "| Profil | Pas/j | Jours | Pas totaux | Niv. | Replis | Tentatives gardien | Farm récup. | Coffres | Or | Rations |",
        "|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|",
    ]
    for key, label in PROFILES:
        for steps in (5000, 8000, 15000):
            s = sm[(key, steps)]
            gold = f"{int(s['gold_median']):,}".replace(",", " ")
            total_steps = f"{int(steps_med[(key, steps)]):,}".replace(",", " ")
            lines.append(
                f"| {label} | {steps} | {fmt(s['days_median'])} | {total_steps} | "
                f"{fmt(s['level_median'])} | {int(s['retreats_median'])} | "
                f"{int(s['boss_attempts_median'])} | {int(s['farm_median'])} | "
                f"{int(s['chests_median'])} | {gold} | {int(s['rations_median'])} |"
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
    out.append("- **Économie** : relance du modificateur (une fois par approche, si l’estimation de victoire est < 85 %, avec 25/75/100 % de chances selon le profil), marchand de camp (achat de potions si l’inventaire passe sous 1/2/3) et respec (optimisé et hyper au palier 30, remboursement de 70 %, réallocation VIT > ATQ > PV).")
    out.append("")
    out.append("## 2. Jours pour terminer")
    out.append("")
    out.extend(days_table(data))
    out.append("")
    out.append("Le **passif minimal ne termine pas** : il atteint le palier 31–34, niveau 104–107, puis reste bloqué. Le **passif optimisé** termine 5/5 à tous les rythmes.")
    out.append("")
    out.append("### Pourquoi l’hyper-optimisé met plus de jours que l’optimisé")
    out.append("")
    out.append("Ce n’est pas une anomalie : **« jours » = pas totaux ÷ pas par jour**. Or l’hyper-optimisé est défini comme un joueur **prudent**, pas comme un joueur pressé :")
    out.append("")
    out.append("- il n’engage un gardien qu’à partir de **75 %** de chances estimées (optimisé 55 %, normal 35 %) ;")
    out.append("- il farme **15 monstres** avant le gardien si la marge est insuffisante (10 / 5) ;")
    out.append("- il refait jusqu’à **3 fois** chaque gardien des paliers 10, 20, … 100 (1 fois / 0).")
    out.append("")
    out.append("Chaque farm de gardien = un palier complet re-marché, soit **~900 pas**. En médiane, l’hyper en fait **30** (3 par gardien), l’optimisé **10** (1 par gardien) et le normal **0**. Les **20 paliers supplémentaires** représentent ~18 000 pas, soit **3,6 jours à 5 000 pas/j** ; l’écart médian est de **3,2 jours** (35,2 vs 32,0), le reste étant compensé par les replis évités.")
    out.append("")
    out.append("| Profil | Jours 5k | Replis 5k | Farms gardien 5k | Jours 15k | Replis 15k | Farms gardien 15k |")
    out.append("|---|---:|---:|---:|---:|---:|---:|")
    out.append("| Passif optimisé | 32,3 | 131 | 0 | 12,0 | 204 | 0 |")
    out.append("| Normal | 31,9 | 91 | 0 | 11,0 | 155 | 0 |")
    out.append("| Optimisé | 32,0 | 69 | 10 | 11,1 | 120 | 10 |")
    out.append("| Hyper-optimisé | 35,2 | 22 | 30 | 12,1 | 33 | 32 |")
    out.append("")
    out.append("À 15 000 pas/j, l’écart hyper/optimisé tombe à **1,0 jour** (12,1 vs 11,1) : le coût fixe du farm est dilué et l’hyper évite **87 replis** (33 contre 120). À 5 000 pas/j, l’optimisé ne meurt que 69 fois, donc le farm de l’hyper n’est pas entièrement rentabilisé en pas.")
    out.append("")
    out.append("**Lecture** : « plus optimisé » ne veut pas dire « plus rapide » dans ce tableau. L’hyper optimise la **puissance et la sécurité** (22 replis à 5k contre 69–131), pas le temps de calendrier. Le normal reste proche de l’optimisé (31,9 vs 32,0 à 5k ; 11,0 vs 11,1 à 15k) mais avec plus de replis (91 vs 69 ; 155 vs 120). Pour mesurer la vitesse pure, il faudrait un profil « speedrun » sans farm de gardien.")
    out.append("")
    out.append("## 3. 0.24 → 0.25 : ce qui change")
    out.append("")
    out.append("Les règles de butin, de monstres et de gardiens intermédiaires sont **inchangées** depuis 0.24. Les sinks économiques déplacent légèrement les trajectoires (relance, respec), mais les jours restent dans la même enveloppe : les écarts de 0,1–2,0 jour sont surtout du bruit de graines. Trois changements structurent cette version :")
    out.append("")
    out.append("- **Nouveau profil par défaut** : l’auto-optimisation étant désactivée par défaut, le « passif » de 0.24 devient le **passif optimisé** (auto on), et le **passif minimal** (auto off) apparaît comme le vrai profil subi. Il ne termine pas.")
    out.append("- **Gardien final rallongé** : la cible passe de « 10–15 s » à **20–30 s** avec des PV adaptatifs et trois phases.")
    out.append("- **Économie simulée** : relance du modificateur, marchand de camp et respec sont maintenant dans le simulateur (voir § 8).")
    out.append("")
    out.append("| Profil | TTK final 0.24 (8 000 pas/j) | TTK final 0.25 (8 000 pas/j) | Victoire 0.25 |")
    out.append("|---|---:|---:|---:|")
    out.append("| Passif (0.24, auto on) → Passif optimisé | 12,8 s | 35,2 s | 100 % |")
    out.append("| Normal | 15,1 s | 31,7 s | 88 % |")
    out.append("| Optimisé | 8,4 s | 18,6 s | 68 % |")
    out.append("| Hyper-optimisé | 6,0 s | 19,7 s | 100 % |")
    out.append("")
    out.append("Le combat final est donc passé d’un **pic de 6–15 s** à un combat de **18,6–35,2 s**. La cible 20–30 s est tenue à **±2 s** pour le normal, l’optimisé et l’hyper à 8 000 pas/j ; le passif optimisé dépasse à 35 s (voir § 6).")
    out.append("")
    out.append("## 4. Statistiques de run")
    out.append("")
    out.extend(stats_table(data))
    out.append("")
    out.append("La colonne **Farm récup.** compte les combats de récupération après un repli (**3 par défaite**), pas le farm volontaire de gardiens. Les pas totaux incluent tout : marche, combats, replis et farm.")
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
    out.append("Le gardien final est dans la cible basse pour l’optimisé et l’hyper (**18,6–20,0 s**) et dans la cible haute pour le normal (**28,2–31,7 s**), mais avec une forte variance : sur le build médian **à l’arrivée** au palier, le normal à 5 000 pas/j ne gagne pas et l’optimisé à 8 000 tombe à 68 %. Ces taux sont mesurés avant le farm et la ténacité, pas sur l’état exact du combat. Le **passif optimisé** reste à 28 s à 5 000, mais à **35 s à 8 000/15 000** : son DPS plus faible déclenche le plancher de PV et allonge le combat.")
    out.append("")
    out.append("Un tiret signifie qu’aucune des 200 simulations n’a gagné avec le build médian à l’arrivée, pas que le profil ne finit pas le jeu.")
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
    out.append("- **31,9 / 20,3 / 11,0 jours**, **91 / 117 / 155 replis**.")
    out.append("- Le rythme rapide reste le plus irrégulier : **155 replis** à 15 000, contre 120 pour l’optimisé. La relance et la pitié limitent la casse.")
    out.append("- Le gardien final est **28,2–31,7 s** avec **0–88 %** de victoire à l’arrivée : le build médian à 5 000 ne gagne pas, mais les runs finissent via le farm et la ténacité. C’est le final le plus tendu des profils finisseurs.")
    out.append("- Les pics de gardiens restent 12, 20–21, 26, 30 et 50–70 ; la ténacité/pitié les rendent finissables.")
    out.append("")
    out.append("### Optimisé — le profil récompensé")
    out.append("")
    out.append("- **32,0 / 19,7 / 11,1 jours**, **69 / 64 / 120 replis**.")
    out.append("- Le gardien final est à **18,6–19,4 s**, **68–100 %** à l’arrivée : rapide mais lisible, avec les phases.")
    out.append("- La relance et le respec au palier 30 sont utilisés, mais restent marginaux dans l’économie actuelle.")
    out.append("- Les décisions de stuff, sets et forge paient. Le milieu de partie reste le plus intéressant.")
    out.append("")
    out.append("### Hyper-optimisé — facile, avec un vrai final")
    out.append("")
    out.append("- **35,2 / 22,0 / 12,1 jours**, **22 / 15 / 33 replis**.")
    out.append("- Le farm de gardiens explique le **35,2 jours** à 5 000 pas/j : il prend du temps pour sécuriser le stuff.")
    out.append("- Le gardien final est à **18,7–20,0 s**, 100 %, avec un minimum de tension grâce aux phases.")
    out.append("- C’est le profil le plus confortable ; le jeu reste relativement facile comme demandé.")
    out.append("")
    out.append("## 8. Économie — relance, marchand, respec")
    out.append("")
    out.append("Les trois sinks sont maintenant simulés avec des politiques par profil :")
    out.append("")
    out.append("| Profil | Relances 5k / 8k / 15k | Potions achetées | Respecs | Or remboursé (respec) |")
    out.append("|---|---:|---:|---:|---:|")
    out.append("| Passif minimal | 0 / 0 / 0 | 0 | 0 | 0 |")
    out.append("| Passif optimisé | 0 / 0 / 0 | 0 | 0 | 0 |")
    out.append("| Normal | 1 / 1 / 2 | 0 | 0 | 0 |")
    out.append("| Optimisé | 0 / 0 / 1 | 0 | 1 | 1 498 / 1 974 / 1 194 |")
    out.append("| Hyper-optimisé | 0 / 0 / 0 | 0 | 1 | 1 676 / 1 918 / 2 208 |")
    out.append("")
    out.append("- **Relance** : utilisée seulement si l’estimation de victoire est < 85 %, avec 25/75/100 % de chances. Les profils forts (hyper) ne relancent presque jamais, ce qui est rationnel : ils n’en ont pas besoin. Coût 200 + 30 × palier, négligeable face aux 2,3–3,1 M d’or gagnés.")
    out.append("- **Marchand** : aucune potion achetée. L’inventaire moyen avant un gardien est de ~148 potions (maximum > 300) : les coffres et les gardiens en donnent bien plus que la consommation. Le marchand est donc un sink sans rareté.")
    out.append("- **Respec** : utilisé une fois au palier 30 par l’optimisé et l’hyper ; le remboursement de 70 % ne représente que 1 200–2 200 or, et le camp est reconstruit aussitôt. Effet combat nul ou dans le bruit des graines.")
    out.append("")
    out.append("**Conclusion** : les trois sinks sont **modélisés**, mais restent **marginaux** dans l’économie actuelle. Pour qu’ils pèsent, il faut soit réduire l’or gagné, soit augmenter les coûts, soit rendre les potions plus rares/plus utiles (ou proposer des offres de marchand plus fortes). C’est le prochain chantier économique.")
    out.append("")
    out.append("## 9. Points à corriger")
    out.append("")
    out.append("1. **Passif minimal non finissable** : l’auto-optimisation est off par défaut. Ajouter une **suggestion au premier lancement** ou un rappel quand le joueur atteint un mur, sans réactiver le mode automatiquement.")
    out.append("2. **Gardien final des builds faibles** : passif optimisé à 35 s, normal à 5 000 sans victoire sur le build médian, optimisé à 8 000 à 68 %. Baisser le plancher de PV final de **0,75 à 0,55** pour les builds à faible DPS, ou cibler 24 s.")
    out.append("3. **Rythme rapide du normal** : encore **155 replis** à 15 000 (contre 120 pour l’optimisé). Le butin adapté a réduit le problème (1 234 en 0.23), mais un catch-up de stuff ou un adoucissement des élites 40–70 reste utile.")
    out.append("4. **Économie marginale** : relances 0–2 par run, **0 potion achetée** (inventaire moyen ~148), respec une fois pour ~1,2–2,2k or remboursé. Les sinks sont modélisés mais ne pèsent pas ; calibrer l’or, les coûts ou la rareté des potions.")
    out.append("5. **Pics 40–70** : encore irréguliers pour le normal et le passif optimisé ; la ténacité/pitié évitent le mur, mais le ressenti reste en dents de scie.")
    out.append("")
    out.append("## 10. Conclusion — ressenti attendu")
    out.append("")
    out.append("- **Passif minimal** : frustration au palier 31, après beaucoup de temps. Priorité : lui proposer l’auto-optimisation.")
    out.append("- **Passif optimisé** : expérience idle crédible, 12–32 jours, mais encore trop de replis et un final trop long à haut rythme.")
    out.append("- **Normal** : bon challenge, final tendu et variable (0–88 % à l’arrivée), rythme rapide encore irrégulier.")
    out.append("- **Optimisé** : le meilleur équilibre ; le final est court mais les phases et la pitié gardent un intérêt.")
    out.append("- **Hyper** : relativement facile, final réussi, farm de gardiens assumé ; le minimum de challenge est là.")
    out.append("")
    out.append("## 11. Limites")
    out.append("")
    out.append("- 5 graines, combats de monstres analytiques, phases du gardien final approximées.")
    out.append("- L’auto-optimisation simulée est une politique modèle (gloutonne, non-set) ; le jeu réel peut différer.")
    out.append("- Les politiques d’économie sont simplifiées : relance si l’estimation < 85 %, marchand si l’inventaire est bas, respec au palier 30. Le marchand n’est pas utilisé car les potions sont abondantes.")
    out.append("- Les taux du gardien final sont mesurés sur le build médian à l’arrivée au palier, avant le farm et la ténacité ; le combat réel peut différer.")
    out.append("- Le test de partie fraîche reste la référence pour le ressenti.")
    out.append("")
    out.append("## 12. Reproductibilité")
    out.append("")
    out.append("```bash")
    out.append("python tools/equilibrage/progression_sim.py --seeds 5 \\")
    out.append("  --profiles passif,passif_auto_game,normal,optimise,hyper \\")
    out.append("  --steps 5000,8000,15000 \\")
    out.append("  --out progression_025_econ_full.json")
    out.append("")
    out.append("python tools/equilibrage/report_progression_025.py \\")
    out.append("  --data progression_025_econ_full.json \\")
    out.append("  --out docs/analyse-progression-025.md")
    out.append("```")
    out.append("")

    Path(args.out).write_text("\n".join(out), encoding="utf-8")
    print(f"Rapport écrit : {args.out}")


if __name__ == "__main__":
    main()
