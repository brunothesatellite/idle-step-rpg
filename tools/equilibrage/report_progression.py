"""Genere le rapport Markdown d'analyse de progression 0.23.0.

Usage :
    python tools/equilibrage/report_progression.py \
        --main C:/chemin/progression_023_full.json \
        --assisted C:/chemin/prog_passif_plus_full.json \
        --out docs/analyse-progression-023.md
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path

PROFILE_LABELS = {
    "passif": "Passif minimal",
    "normal": "Normal",
    "optimise": "Optimisé",
    "hyper": "Hyper-optimisé",
    "passif_plus": "Passif assisté",
}


def load(path: str) -> dict:
    return json.loads(Path(path).read_text(encoding="utf-8"))


def fmt(value, digits=1):
    if value is None:
        return "—"
    if isinstance(value, float):
        return f"{value:.{digits}f}".replace(".", ",")
    return str(value)


def summary_map(data: dict) -> dict[tuple[str, int], dict]:
    return {(s["profile"], s["steps"]): s for s in data["summary"]}


def days_table(main: dict, assisted: dict) -> list[str]:
    lines = [
        "| Profil | 5 000 pas/j | 8 000 pas/j | 15 000 pas/j | Palier final | Niveau final |",
        "|---|---:|---:|---:|---:|---:|",
    ]
    ms = summary_map(main)
    ass = summary_map(assisted)
    rows = [
        ("passif", "Passif minimal (aucune optimisation)"),
        ("passif_plus", "Passif assisté (auto-optimisation + coffres + soins)"),
        ("normal", "Normal"),
        ("optimise", "Optimisé"),
        ("hyper", "Hyper-optimisé"),
    ]
    for key, label in rows:
        cells = []
        floors = []
        levels = []
        for steps in (5000, 8000, 15000):
            src = ass if key == "passif_plus" else ms
            s = src.get((key, steps))
            if s is None:
                cells.append("—")
                continue
            floors.append(s["floor_median"])
            levels.append(s["level_median"])
            if s["finished"] == 0:
                cells.append(f"non fini<br>(mur p.{int(s['floor_median'])})")
            else:
                cells.append(f"{fmt(s['days_median'])} j ({s['finished']}/{s['runs']})")
        floor = f"{int(max(floors))} / 101" if floors else "—"
        level = f"{fmt(max(levels))}" if levels else "—"
        lines.append(f"| **{label}** | {cells[0]} | {cells[1]} | {cells[2]} | {floor} | {level} |")
    return lines


def stats_table(main: dict, assisted: dict) -> list[str]:
    lines = [
        "| Profil | Pas/j | Jours | Niv. héros | Replis | Tentatives gardien | Fermes | Coffres | Or gagné | Rations utilisées |",
        "|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|",
    ]
    ms = summary_map(main)
    ass = summary_map(assisted)
    for key, label in (
        ("passif", "Passif minimal"),
        ("passif_plus", "Passif assisté"),
        ("normal", "Normal"),
        ("optimise", "Optimisé"),
        ("hyper", "Hyper-optimisé"),
    ):
        src = ass if key == "passif_plus" else ms
        for steps in (5000, 8000, 15000):
            s = src.get((key, steps))
            if s is None:
                continue
            gold = f"{int(s['gold_median']):,}".replace(",", " ")
            lines.append(
                f"| {label} | {steps} | {fmt(s['days_median'])} | {fmt(s['level_median'])} | "
                f"{int(s['retreats_median'])} | {int(s['boss_attempts_median'])} | "
                f"{int(s['farm_median'])} | {int(s['chests_median'])} | "
                f"{gold} | {int(s['rations_median'])} |"
            )
    return lines


def ttk_table(data: dict, key: str, title: str) -> list[str]:
    rows = data["ttk"].get(key, [])
    lines = [
        f"#### {title}",
        "",
        "| Palier | Niv. | ATQ | PV | VIT | Crit | Camp (A/S/V) | TTK facile | TTK moyen | TTK coriace | TTK élite | TTK gardien | Victoire gardien | PV perdus gardien | Rations |",
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


def boss_pace_table(data: dict, key: str, assisted: dict | None = None) -> list[str]:
    lines = [
        "| Palier | 5 000 pas/j | 8 000 pas/j | 15 000 pas/j |",
        "|---:|---:|---:|---:|",
    ]
    rows_by_steps = {}
    for steps in (5000, 8000, 15000):
        k = f"{key}_{steps}"
        rows_by_steps[steps] = {r["floor"]: r for r in data["ttk"].get(k, [])}
    for floor in range(10, 101, 10):
        vals = []
        for steps in (5000, 8000, 15000):
            r = rows_by_steps[steps].get(floor)
            vals.append("—" if not r else fmt(r.get("ttk_boss")))
        lines.append(f"| {floor} | {vals[0]} | {vals[1]} | {vals[2]} |")
    return lines


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--main", required=True)
    parser.add_argument("--assisted", required=True)
    parser.add_argument("--out", required=True)
    args = parser.parse_args()
    main_data = load(args.main)
    assisted_data = load(args.assisted)
    main_s = summary_map(main_data)
    ass_s = summary_map(assisted_data)

    out: list[str] = []
    out.append("# Analyse de progression 1 → 101 — version 0.23.0")
    out.append("")
    out.append("> Simulation complète du palier 1 au début du palier 101 (le gardien du palier 100 est le dernier combat).")
    out.append("> Date : 26/11/2026 · Version : 0.23.0 / 64 · 5 graines par couple profil × pas/jour.")
    out.append("")
    out.append("## 1. Méthode")
    out.append("")
    out.append("- **Pas** : 10 pas = 1 m, 100 m par palier. Chaque palier contient 8 combats normaux, un portail à 90 m et un gardien à 100 m.")
    out.append("- **Loot** : butin rare garanti tous les 5 monstres, butin rare de gardien avec pity de set tous les 10 gardiens, coffres (18 % après un monstre), fusions 3 → 1 à 90 % de la somme, forge +20 niveaux, camp 30 niveaux.")
    out.append("- **Profils** : le passif minimal équipe le meilleur objet par emplacement mais n’ouvre pas les coffres, ne fusionne pas, n’améliore pas et ne farme pas ; le normal fait un peu de tout ; l’optimisé et l’hyper optimisent, fusionnent, forgent et farment.")
    out.append("- **Combat** : monstres résolus analytiquement (TTK/dégâts moyens, auto-ration), gardiens simulés avec `balance_sim.py` (capacités, modificateurs, enrage, pity, potions).")
    out.append("- **TTK** : temps moyen des combats gagnés, stuff au moment d’arriver au palier, pleine vie + 1 ration. Les colonnes facile/moyen/coriace/élite représentent les trois pools de monstres et les élites ; les espèces partagent la même courbe de stats.")
    out.append("- **Fin** : le run s’arrête quand le gardien du palier 100 tombe. Un run est « non fini » si le héros reste bloqué plus de 200 000 pas sur un palier sans gagner de puissance.")
    out.append("")
    out.append("### Résumé exécutif")
    out.append("")
    out.append("- **Passif minimal : non finissable.** Mur au palier 21, 3 800–4 200 replis, niveau 100+ atteint mais stuff insuffisant.")
    out.append("- **Passif assisté : finissable** en 29–46 jours selon le rythme, mais avec 579–2 049 replis. L’auto-optimisation est la condition de la finissabilité.")
    out.append("- **Normal : finissable** en 24–46 jours, 358–1 234 replis. Le jeu est challengeant mais très irrégulier ; plus le joueur marche, plus il échoue.")
    out.append("- **Optimisé : finissable** en 18–38 jours, 215–609 replis. C’est le profil le plus récompensé, avec un endgame devenu trop facile.")
    out.append("- **Hyper : finissable** en 13–40 jours, 103–114 replis. Facile après le palier 40, mais le gardien final tombe en 4,5–6,8 s.")
    out.append("")
    out.append("## 2. Jours pour terminer")
    out.append("")
    out.extend(days_table(main_data, assisted_data))
    out.append("")
    out.append("Le **passif minimal ne termine pas** : il reste bloqué au palier 21, même à 100 000 pas simulés par palier, avec un niveau de héros supérieur à 100. Le **passif assisté** (auto-optimisation, coffres, soins) termine, mais avec beaucoup de replis.")
    out.append("")
    out.append("## 3. Statistiques de run")
    out.append("")
    out.extend(stats_table(main_data, assisted_data))
    out.append("")
    out.append("## 4. TTK par palier")
    out.append("")
    out.append("Les tableaux ci-dessous sont donnés à **8 000 pas/jour**, le rythme médian. Les tableaux complets 5 000 / 8 000 / 15 000 sont dans le JSON de simulation ; la section 5 donne les TTK gardien pour les trois rythmes.")
    out.append("")
    out.append("Un tiret dans la colonne **TTK élite** signifie qu’aucun combat n’a été gagné sur 120 simulations, pas une donnée manquante. Les élites sont le principal générateur de replis des profils normal et optimisé.")
    out.append("")
    out.append("`Camp (A/S/V)` = niveaux de **Lame affûtée / Souffle ancien / Pas vifs**. Les TTK sont en secondes ; `PV perdus gardien` est la perte moyenne sur les victoires, avec une ration automatique.")
    out.append("")
    out.extend(ttk_table(main_data, "passif_8000", "Passif minimal (bloqué au palier 21)"))
    out.append("")
    out.extend(ttk_table(assisted_data, "passif_plus_8000", "Passif assisté (auto-optimisation + coffres + soins)"))
    out.append("")
    out.extend(ttk_table(main_data, "normal_8000", "Normal"))
    out.append("")
    out.extend(ttk_table(main_data, "optimise_8000", "Optimisé"))
    out.append("")
    out.extend(ttk_table(main_data, "hyper_8000", "Hyper-optimisé"))
    out.append("")
    out.append("## 5. TTK gardien selon le rythme de marche")
    out.append("")
    for key, label, src in (
        ("passif_plus", "Passif assisté", assisted_data),
        ("normal", "Normal", main_data),
        ("optimise", "Optimisé", main_data),
        ("hyper", "Hyper-optimisé", main_data),
    ):
        out.append(f"**{label}**")
        out.append("")
        out.extend(boss_pace_table(src, key))
        out.append("")
    out.append("Le rythme plus rapide ne rend pas le jeu plus facile : il réduit les jours, mais augmente les replis et les retours en arrière, car le stuff n’a pas le temps de suivre.")
    out.append("")
    out.append("## 6. Analyse par profil")
    out.append("")
    out.append("### Passif minimal — mur au palier 21")
    out.append("")
    out.append("Le passif atteint le palier 21 vers le niveau 60–80, puis ne gagne plus. Il a environ **3 800–4 200 replis**, dont la majorité sont des défaites contre des monstres normaux ou des élites, et **11 000–12 500 combats de farm de récupération**. Son niveau monte jusqu’à 100+, mais ses PV/ATQ de base ne compensent pas le stuff manquant. Le stuff rare du palier 21 ne suffit pas : sans fusion, forge, camp ni coffres, le héros ne peut pas passer le contrôle de stuff du palier 21. **L’XP seule ne permet donc pas de progresser**, contrairement à l’objectif annoncé. Un test avec une early curve adoucie (×2,0 PV / ×1,8 ATQ, fin au palier 20) déplace le mur du palier 21 au palier 24 : ce n’est pas suffisant seul.")
    out.append("")
    out.append("### Passif assisté — la version jouable de l’idle")
    out.append("")
    out.append("En ajoutant les automatismes qu’un joueur d’idle attend (équiper le meilleur, fusionner les doublons, forger avec réserve, acheter le camp, ouvrir les coffres, se soigner), le passif termine **5/5 à 5 000 et 8 000 pas/j, 4/5 à 15 000**. Les jours médians sont **46,2 / 41,7 / 29,0**, avec **579 / 1 321 / 2 049 replis**. C’est la preuve que le problème n’est pas l’XP mais l’accessibilité de l’optimisation. Même assisté, le passif reste très sensible au rythme : à 15 000 pas/j, il outrepasse son stuff et échoue davantage.")
    out.append("")
    out.append("### Normal — finissable mais très irrégulier")
    out.append("")
    out.append(f"Le normal termine en **{fmt(main_s[('normal', 5000)]['days_median'])} jours à 5 000 pas/j**, **{fmt(main_s[('normal', 8000)]['days_median'])} jours à 8 000** et **{fmt(main_s[('normal', 15000)]['days_median'])} jours à 15 000**. Il garde un niveau final de 98–107. Les replis montent de **358** (5 000) à **702** (8 000) puis **1 234** (15 000) : plus le joueur marche, plus il avance vite, moins il farme, et plus il échoue. Les pics sont les gardiens 12, 20–21, 26, 30, puis 50–70, et les élites des paliers 40–70 (0–50 % de victoires dans les tableaux). Le joueur ressent une alternance de « je passe facilement » et de « je dois farmer 20 minutes ».")
    out.append("")
    out.append("### Optimisé — la récompense de l’optimisation")
    out.append("")
    out.append(f"L’optimisé termine en **{fmt(main_s[('optimise', 5000)]['days_median'])} jours à 5 000**, **{fmt(main_s[('optimise', 8000)]['days_median'])} à 8 000** et **{fmt(main_s[('optimise', 15000)]['days_median'])} à 15 000**, avec 215–609 replis. Après le palier 40, les gardiens sont à 100 % et 7–14 s ; le jeu devient une formalité, sauf les élites. Le stuff, les sets et la forge sont clairement récompensés, mais la difficulté est concentrée à l’early/mid game.")
    out.append("")
    out.append("### Hyper-optimisé — facile, mais finale trop courte")
    out.append("")
    out.append(f"L’hyper termine en **{fmt(main_s[('hyper', 5000)]['days_median'])} jours à 5 000**, **{fmt(main_s[('hyper', 8000)]['days_median'])} à 8 000** et **{fmt(main_s[('hyper', 15000)]['days_median'])} à 15 000**, avec seulement **103–114 replis**. Il farme 3 gardiens par jalon, ce qui explique qu’il ne soit pas plus rapide que l’optimisé malgré sa puissance. Les gardiens 10–30 restent à 28–68 % de victoires (stuff encore en retard), puis 100 % après 40. Le **gardien final tombe en 4,5–6,8 s** : le combat le plus important du jeu n’est pas mémorable.")
    out.append("")
    out.append("## 7. Problèmes majeurs")
    out.append("")
    out.append("1. **Le passif ne peut pas finir.** Le contrôle de stuff du palier 21 est infranchissable sans fusion/forge/camp/coffres. L’XP ne compense pas.")
    out.append("2. **Le rythme de marche punit le joueur.** À 15 000 pas/j, le normal a 3,4× plus de replis qu’à 5 000. Le stuff ne suit pas la progression des paliers.")
    out.append("3. **Les élites sont trop létales.** À 40–70, un build normal a souvent 0–50 % de victoire contre une élite, ce qui crée des morts en série et des boucles de récupération.")
    out.append("4. **Les gardiens early/mid sont des murs de stuff.** 12, 20–21, 26, 30, puis 50–70 demandent 10–60 tentatives. La pitié actuelle (−1 %/défaite, plafond −3 %) ne change rien.")
    out.append("5. **Le endgame est trop facile pour l’hyper.** Le stuff légendaire forgé + camp 30 + sets donne un TTK gardien de 4–8 s après le palier 50. Il manque un vrai climax.")
    out.append("6. **L’économie n’est pas une contrainte.** 2,6–3,6 M d’or et 1 300–2 100 rations utilisées ; les ressources sont abondantes, donc les choix de camp/consommables pèsent peu.")
    out.append("")
    out.append("## 8. Améliorations proposées")
    out.append("")
    out.append("### P0 — Rendre le passif finissable sans le forcer à optimiser")
    out.append("")
    out.append("- Ajouter une **optimisation automatique** activée par défaut : équiper le meilleur objet, fusionner les doublons, forger avec une réserve d’or/éclats, acheter le camp. Un bouton « Auto » à côté de « Équiper le meilleur ».")
    out.append("- Test simulé : passif + auto-optimisation + coffres + soins = **5/5 finis, 46,2 jours à 5 000 pas/j, 579 replis**, contre 0/5 pour le passif minimal. C’est la preuve que le problème est l’accessibilité de l’optimisation, pas l’XP.")
    out.append("- Alternative/complément : **buff de ténacité** après chaque défaite (+5 % ATK/PV au prochain combat du palier, cumulable +25 %, reset à la victoire). Cela garantit qu’un joueur passif finit par passer sans rendre le jeu trivial pour les autres.")
    out.append("")
    out.append("### P0 — Faire suivre le stuff au rythme du joueur")
    out.append("")
    out.append("- Faire évoluer le **niveau des objets avec le niveau du héros**, pas seulement avec le palier : `itemLevel = max(palier, niveau - 10)`. Un joueur qui marche beaucoup et gagne des niveaux obtient un stuff utilisable, au lieu de farmer un stuff de palier déjà dépassé.")
    out.append("- Ou ajouter un **catch-up de stuff** : quand la puissance du héros est sous la puissance attendue du palier, augmenter la rareté/le niveau des drops pendant quelques combats. Ne pas augmenter la difficulté des joueurs sur-stuffés : cela punirait l’optimisation.")
    out.append("")
    out.append("### P1 — Adoucir les élites et l’early curve")
    out.append("")
    out.append("- Élites : PV ×1,45 au lieu de ×1,80, ATK ×1,05 au lieu de ×1,15, ou niveau +1 au lieu de +2. Test simulé : les replis du passif baissent de 4 177 à 3 136 avec élites ×1,40/×1,05.")
    out.append("- Early curve : garder ×3,0 sur les paliers 6–8, puis redescendre à ×1 dès le palier 18–20 (au lieu de 25). Le pic ×3 est utile là où les rapports l’ont montré, mais il crée un mur à 20–26.")
    out.append("")
    out.append("### P1 — Anti-mur de gardien")
    out.append("")
    out.append("- Pitié : passer de **−1 %/défaite, plafond −3 %** à **−3 %/défaite, plafond −15 %**, ou ajouter « après 5 défaites, le gardien perd 10 % PV/ATQ ».")
    out.append("- Afficher la progression de la pitié au portail ; le joueur doit voir que ses échecs le rapprochent de la victoire.")
    out.append("")
    out.append("### P2 — Donner un vrai final")
    out.append("")
    out.append("- Gardien final : PV ×1,8–2,0 (au lieu de ×1,40) et ATK ×1,25–1,30, avec une phase 2 unique. Objectif : 10–15 s pour l’hyper, 15–20 s pour l’optimisé, sans rescaling global des autres gardiens.")
    out.append("- Éviter un gear-score scaling global : le prototype a augmenté les jours et les replis de l’hyper (52,6 jours / 272 replis contre 40,1 / 103), ce qui punit l’optimisation. Réserver le challenge scaling au gardien final.")
    out.append("")
    out.append("### P2 — Économie")
    out.append("")
    out.append("- Ajouter des sinks d’or : reroll du modificateur de gardien, potions au camp, respec des améliorations.")
    out.append("- Rendre les rations un peu plus rares ou les soins plus coûteux pour que « quelques rations » soit une vraie décision ; actuellement 25–30 rations/jour couvrent largement les besoins.")
    out.append("")
    out.append("## 9. Limites de la simulation")
    out.append("")
    out.append("- Les combats de monstres sont analytiques (moyennes) ; les gardiens sont simulés 1 fois par tentative, avec 5 graines par couple profil × pas/jour. Les chiffres sont directionnels, pas une prédiction au jour près.")
    out.append("- Les choix de stuff du joueur réel (sets préférés, effets, objets verrouillés) sont simplifiés ; l’hyper utilise un oracle, le normal un glouton.")
    out.append("- Les événements spéciaux, l’identification, les consommables de soin et certaines interactions UI sont simplifiés ou agrégés.")
    out.append("- Le « passif minimal » est volontairement extrême : il n’ouvre pas les coffres et ne touche jamais à la forge. Le « passif assisté » représente mieux un joueur d’idle qui accepte les automatismes.")
    out.append("")
    out.append("## 10. Conclusion — ressenti attendu")
    out.append("")
    out.append("- **Passif** : aujourd’hui, frustration et sentiment d’injustice au palier 21. Avec l’auto-optimisation, il termine en ~6–7 semaines à 5 000 pas/j, mais avec encore trop de replis. C’est la priorité.")
    out.append("- **Normal** : le jeu est challengeant et lisible, mais les pics de gardiens et les élites transforment parfois le plaisir en corvée. Objectif : diviser les replis par 3–5 et lisser les paliers 12–30 et 50–70.")
    out.append("- **Optimisé** : c’est le profil le plus gratifiant : les décisions de stuff, sets et forge paient. Le milieu de partie est réussi ; le endgame manque de tension.")
    out.append("- **Hyper** : le jeu est effectivement relativement facile, avec un minimum de challenge jusqu’au palier 40, puis il devient trop court. Le gardien final doit être le sommet, pas une formalité de 4 s.")
    out.append("")
    out.append("## 11. Reproductibilité")
    out.append("")
    out.append("```bash")
    out.append("python tools/equilibrage/progression_sim.py --seeds 5 \\")
    out.append("  --profiles passif,normal,optimise,hyper \\")
    out.append("  --steps 5000,8000,15000 \\")
    out.append("  --out progression_023_full.json")
    out.append("")
    out.append("python tools/equilibrage/progression_sim.py --seeds 5 \\")
    out.append("  --profiles passif_plus --steps 5000,8000,15000 \\")
    out.append("  --out prog_passif_plus_full.json")
    out.append("")
    out.append("python tools/equilibrage/report_progression.py \\")
    out.append("  --main progression_023_full.json --assisted prog_passif_plus_full.json \\")
    out.append("  --out docs/analyse-progression-023.md")
    out.append("```")
    out.append("")
    out.append("> Les chiffres sont des médianes sur 5 graines. Le simulateur ne remplace pas un playtest : il sert à identifier les murs et les tendances, pas à prédire un joueur réel au jour près.")
    out.append("")

    Path(args.out).write_text("\n".join(out), encoding="utf-8")
    print(f"Rapport écrit : {args.out}")


if __name__ == "__main__":
    main()
