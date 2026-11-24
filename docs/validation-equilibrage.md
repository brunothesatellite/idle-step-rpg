# Plan de validation de l’équilibrage

> Compagnon de `docs/equilibrage.md`, `docs/plan-equilibrage.md` et de `tools/equilibrage/balance_sim.py`.
> Objectif : vérifier, avant et après implémentation, que les nouvelles courbes rendent le jeu **franchissable**, **progressif** et **non trivial**, sans remplacer un mur par un autre.
> Toute modification suit l'ordre des lots de `docs/plan-equilibrage.md`.

> **État au 24/11/2026 (0.20.10).** Les lots 1 à 9 sont livrés. La **porte D** (gardiens) n'est pas encore franchie : les valeurs par défaut de `--curve target` décrivent encore la Phase A (bouclier 40 %, enrage après 15 s, diviseur d'ATQ 16, sans pitié ni Trône brisé). Les taux de gardiens produits aujourd'hui ne servent pas de référence tant que le simulateur n'est pas aligné sur les mécaniques 0.20.5+.

## 1. Périmètre

### Inclus

- Courbes de PV/ATQ des monstres, élites et gardiens.
- Courbe d’XP et gains de niveau.
- Butin, rareté, fusion, améliorations d’objets.
- Économie d’éclats, rations, or, coffres.
- Farm, repli, pitié de gardien.
- Lisibilité des combats (TTK/TTD, puissance recommandée).

### Exclu

- Refonte graphique, audio, interface hors indicateurs d’équilibrage.
- Nouveaux biomes, nouveaux monstres, nouveaux systèmes de jeu.
- Monétisation, publicité, comptes.
- Migration de sauvegarde lourde (sauf décision explicite).

## 2. Hypothèses à valider

1. Le blocage au palier 36 vient de l’ATQ ennemie et de l’XP, pas des pas.
2. Séparer les multiplicateurs PV/ATQ débloque le palier 36 avec le build actuel.
3. Un build « stuff attendu » peut franchir les paliers 50, 75 et 100 avec du farm et des améliorations, sans devenir invincible.
4. Le niveau 100 est atteignable en **2 500 à 4 000 kills** et **1 à 3 mois** à 5-10 k pas/jour.
5. Les éclats/rations permettent de sortir d’une situation à 0/0 sans boucle infinie.
6. Le butin de palier élevé est nettement meilleur, sans rendre les anciens objets inutiles.
7. Aucun build ne domine de plus de 2× un autre.

## 3. Outils

- Simulateur : `tools/equilibrage/balance_sim.py` (aucune dépendance).
- Documentation : `tools/equilibrage/README.md`.
- Rapport initial : `docs/equilibrage.md`.
- Playtest : sauvegarde actuelle (palier 36) + nouvelle sauvegarde + sauvegarde avancée (palier 75-90 si disponible).

Commandes de référence :

```bash
# Calibration : doit reproduire le blocage actuel
python tools/equilibrage/balance_sim.py --curve current \
  --build player36,reference,reference_high --floors 30,34,36,40,50 --runs 500

# Phase A : déblocage du 36
python tools/equilibrage/balance_sim.py --curve proposed \
  --build player36,reference,reference_high --floors 36,40,50 --runs 500

# Phase B : montée vers 100
python tools/equilibrage/balance_sim.py --curve proposed \
  --build reference_low,reference,reference_high,tank,crit,lifesteal,endgame \
  --floors 50,60,70,75,80,90,100 --runs 300

python tools/equilibrage/balance_sim.py --curve target --build reference,reference_high \
  --floors 36,50,75,100 --runs 300

# XP et pas
python tools/equilibrage/balance_sim.py --xp
```

Le mode `--curve target` du simulateur sert à reproduire la courbe de combat visée (monstre ~5 s, gardien ~18 s). La référence de vérité reste `docs/equilibrage.md` §3.3 (état 0.20.0) et §5 (cible).

## 4. Matrice de tests

| Axe | Valeurs |
|---|---|
| Courbe | `current` (calibration), `proposed`, `target` |
| Builds | `player36`, `reference_low`, `reference`, `reference_high`, `tank`, `crit`, `lifesteal`, `endgame` |
| Paliers | 10, 20, 30, 34, **36**, 40, 50, 60, 70, 75, 80, 90, **100** |
| Ennemis | monstre, élite, gardien |
| Capacités | `shield`, `slam` (les autres seront ajoutées ensuite) |
| Biomes | `auto`, `forge`, `throne` |
| Runs | 300 à 500 par cellule, graine fixe |

Chaque cellule produit : taux de victoire, TTK, TTD, PV restants, rations consommées, dégâts subis.

## 5. Critères d’acceptation

### 5.1 Combat

| Cas | Cible |
|---|---|
| Monstre normal, build `reference` | ≥ 95 % de victoires, TTK 4-7 s, ≤ 0,2 ration |
| Monstre normal, build `player36` | gagnable sans potion, PV conservés après victoire (attrition) |
| Élite, build `reference` | ≥ 80 % de victoires, TTK 8-12 s |
| Gardien, build `reference` sans potion | **35-65 %** de victoires, TTK 15-22 s, ≥ 30 coups portés |
| Gardien, build `reference` + potion | **55-75 %** de victoires, TTK 13-18 s, ≥ 30 coups portés |
| Gardien, build `reference_high` au 100 | **85-95 %** de victoires, TTK 15-22 s, ≥ 30 coups portés |
| Gardien + potion, build `reference_high` | TTK réduit de 20-40 %, jamais < 10 s ni < 25 coups |
| Gardien avec enrage après 25 s | sécurise les combats longs ; à renforcer si le meilleur stuff gagne encore trop souvent |
| Build `under` | 10-25 % sur les gardiens, gagne encore des monstres |
| Palier 36, build `player36` | ≥ 80 % sur monstre, **35-65 %** sur gardien après patch |
| Écart entre builds | aucun build > 2× plus rapide qu’un autre |

### 5.2 Progression

| Métrique | Cible |
|---|---|
| Kills 1→100 | 2 500-4 000 |
| Niveau 100 | vers le palier 100-120 |
| Pas 1→100 | 100 000-200 000 (farm/retries inclus) |
| Durée | 1-3 mois à 5-10 k pas/jour |
| Tentatives par palier normal | 1-3 en moyenne, ≤ 5 |
| Tentatives par gardien | 2-4 en moyenne, ≤ 8 |

### 5.3 Économie

| Métrique | Cible |
|---|---|
| Éclats/jour | 5-10 |
| Niveaux d’objet/jour | 1-3 |
| Rations/jour | 3-6 |
| Récupération depuis 0 éclat / 0 ration | ≤ 3 jours de farm |
| Or | dépensé régulièrement, pas accumulé sans usage |
| Coffres | ouverts sans devenir la seule source de progression |

### 5.4 Butin

- Un objet rare de palier 100 doit être **nettement** supérieur à un objet rare de palier 36 (cible : ×2 à ×3 en stats utiles).
- La fusion doit rester un choix, pas une perte sèche.
- Les sets doivent être visibles et utiles (bonus en % ou scalés).

## 6. Protocole

### Phase 0 — Calibration du simulateur

1. Vérifier que `current` reproduit les observations :
   - `player36` perd le 36 (0 %) ;
   - `player36` gagne le 30 ;
   - le gardien 36 tue en ~5-8 coups.
2. Ajuster les presets `reference_*` pour qu’ils représentent un joueur réel au palier (stuff, camp, niveau).
3. Documenter les écarts simulateur ↔ jeu réel.

**Sortie** : simulateur de confiance, tableau de calibration.

**Écart de calibration 0.20.0** : le simulateur `target` donne `player36` à **0 %** au gardien 36, alors que l’ancien rapport annonçait 59 %, et le code ajoute +2 niveaux au gardien. Cette divergence est traitée dans `docs/equilibrage.md` §3.4 et §4.3. Tant que la porte A du plan n’est pas franchie, ces chiffres ne servent pas de référence.

### Phase A — Débloquer le palier 36

**Statut : implémentée dans la version 0.20.0, playtest en cours.**

1. Courbe Phase A : voir `docs/equilibrage.md` §3.3 (gardien ~18 s, ≥ 30 coups), bouclier réduit, enrage après 15 s, éclats/rations revus, pitié cumulative, farm existant clarifié.
2. Implémenté : voir `docs/equilibrage.md` §3 et §4.
3. Playtest 7 jours sur la sauvegarde actuelle :
   - noter paliers gagnés, défaites, éclats/rations, or, niveau ;
   - vérifier qu’aucun joueur ne reste bloqué sans option.
4. Critère : **+5 paliers en 7 jours** pour le build actuel, ou blocage expliqué par un choix de farm.

**Sortie** : décision go/no-go pour la Phase B.

**Garde-fou** : aucun gardien ne doit être gagné en moins de 10 s ou en moins de 20 coups portés, même avec le meilleur stuff. Les potions peuvent réduire le TTK de 20-40 %, mais pas transformer le combat en exécution automatique.

### Phase B — Niveau 100

1. Simuler XP 1,09, récompenses linéaires, butin scalable, camp/sets en %.
2. Implémenter.
3. Playtest 14 jours :
   - nouvelle sauvegarde depuis le palier 1 ;
   - sauvegarde actuelle au 36 ;
   - sauvegarde avancée si disponible.
4. Critères : 2 500-4 000 kills, niveau 100 vers 100-120, pas de build dominant, économie stable.

**Sortie** : validation de la courbe longue.

### Phase C — Fin de partie

1. Remplacer le cap 40 par une courbe lisible ou un NG+.
2. Ajouter les indicateurs de puissance recommandée.
3. Playtest 14 jours et revue des métriques.

## 7. Données à collecter

### Dans le simulateur

- Taux de victoire, TTK, TTD, rations, dégâts subis.
- Coups portés par le héros et coups encaissés (pour détecter les gardiens tombés en 2-3 coups).
- XP/kill, XP total, kills estimés.
- Pas et jours.

### Dans le jeu (local, sans serveur)

- Événements : `floor_attempt`, `floor_win`, `floor_loss`, `boss_attempt`, `boss_win`, `boss_loss`.
- Contexte : palier, niveau, ATQ, PV, VIT, crit, stuff (niveau moyen), éclats, rations.
- Économie : éclats gagnés/dépensés, rations gagnées/consommées, or gagné/dépensé.
- Progression : temps par niveau, kills par niveau, paliers par jour.
- Export : un fichier `balance_report.json` ou un écran debug, pas d’analytics réseau.

### Dans le playtest

- Journal quotidien : paliers, défaites, farm, améliorations, ressenti.
- Captures avant/après aux paliers clés.

## 8. Règles de décision

| Observation | Action |
|---|---|
| Taux de victoire trop bas sur un palier | réduire d’abord l’ATQ ennemie, puis les PV |
| TTK trop long | réduire les PV ennemis ou augmenter les dégâts du héros |
| TTK trop court | augmenter les PV ennemis |
| Un build > 2× les autres | ajuster les synergies (sets, crit, vol de vie) |
| Éclats qui s’accumulent au plafond | réduire les revenus ou augmenter les coûts |
| Éclats toujours à 0 | augmenter les revenus ou réduire les coûts |
| Niveau 100 < 2 500 kills | durcir l'XP |
| Niveau 100 > 4 000 kills | adoucir l’XP |
| Joueurs bloqués > 5 tentatives | activer la pitié ou ajuster le palier |

## 9. Risques

- **Le simulateur n’est pas le jeu** : il faut un playtest réel, surtout pour la ferveur, les consommables et les sets.
- **Le build actuel est un glass cannon** : il peut surestimer ou sous-estimer la difficulté selon les courbes.
- **Inflation d’éclats** : doubler les revenus peut rendre les améliorations triviales.
- **Inflation de butin** : le scaling par palier peut rendre les anciens objets obsolètes.
- **XP trop rapide** : niveau 100 en quelques semaines, perte de sens.
- **Migration** : ne pas changer les stats des objets existants sans décision explicite.

## 10. Planning indicatif

| Étape | Durée |
|---|---|
| Phase 0 — calibration simulateur | 1-2 jours |
| Phase A — simulation + implémentation | 3-5 jours |
| Phase A — playtest | 7 jours |
| Phase B — simulation + implémentation | 1-2 semaines |
| Phase B — playtest | 14 jours |
| Phase C — fin de partie | 2 semaines |
| Bilan et décision | 2 jours |

## 11. Livrables

- `tools/equilibrage/balance_sim.py` et son README.
- `docs/equilibrage.md` (analyse initiale).
- Ce plan de validation.
- Tableau des constantes avant/après.
- Journal de playtest.
- Décision finale : conserver, ajuster ou abandonner chaque changement.
