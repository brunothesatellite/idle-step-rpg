# Simulateur d’équilibrage

Outil autonome pour rejouer les combats de **La Marche des Âmes** avec les courbes actuelles ou proposées, sans Android et sans modifier le jeu.

- Script : `tools/equilibrage/balance_sim.py`
- Python 3.10+, **aucune dépendance externe**.
- Les formules sont recopiées depuis `GameEngine.kt` / `Model.kt` ; elles doivent rester synchronisées avec le code.

> **Référence de courbe** : `docs/equilibrage.md` (version 0.20.9) et `docs/plan-equilibrage.md`. Les résultats chiffrés de la section « Premiers résultats » sont **historiques (0.19.1)** et ne servent plus de cible.

## Source de vérité

- Constantes Kotlin : `android/app/src/main/java/com/brunothesatellite/idlesteprpg/engine/Balance.kt`.
- Miroir Python : `tools/equilibrage/balance_constants.py`.
- Vérification : `python tools/equilibrage/check_constants.py` (lancée par la CI Android).

Toute modification de constante doit être répercutée dans les deux fichiers, puis vérifiée avant une simulation.

## Utilisation

```bash
# Courbe du code 0.21.0 (référence pour la calibration)
python tools/equilibrage/balance_sim.py --curve code --build reference,reference_high,under \
  --floors 36,50,75,100 --runs 500 --modifier auto

# Pitié après 3 défaites (3 %)
python tools/equilibrage/balance_sim.py --curve code --build reference \
  --floors 36,50,75,100 --runs 300 --modifier auto --pity 0.03

# Courbe cible de design (TTK constant) et courbe actuelle historique
python tools/equilibrage/balance_sim.py --curve target --build reference --floors 36,100 --runs 300
python tools/equilibrage/balance_sim.py --curve current --build player36 --floors 30,36 --runs 300

# Courbes d’XP et de pas
python tools/equilibrage/balance_sim.py --xp
```

### Analyser une sauvegarde réelle

`save_import.py` reconstruit les statistiques d’une sauvegarde JSON avec les formules de `statsOf()`. `earlygame_experiment.py` l’utilise pour tester la courbe ennemie early game, l’écart de niveau et le rythme d’XP sans modifier le jeu :

```bash
python tools/equilibrage/earlygame_experiment.py \
  --save C:/chemin/lowlevel.json --runs 800

# Écrit aussi la matrice complète :
python tools/equilibrage/earlygame_experiment.py \
  --save C:/chemin/lowlevel.json --runs 800 \
  --out docs/analyse-earlygame-data.md
```

Les sauvegardes ne sont pas versionnées dans le dépôt. Le rapport inclut le stuff figé, le stuff nivelé et le stuff attendu (rareté +1, +5 niveaux de forge). Rapport d’analyse : [`docs/analyse-earlygame.md`](../../docs/analyse-earlygame.md).

Sur Windows, avec le venv du projet :

```powershell
& .\video_promo\.venv\Scripts\python.exe tools\equilibrage\balance_sim.py --curve proposed --build all --floors 36,50,75,100
```

## Presets de builds

| Preset | ATQ | PV | VIT | Particularité |
|---|---:|---:|---:|---|
| `player36` | 1 349 | 3 699 | 2,27 | Capture réelle du joueur au palier 36 |
| `reference_low` | 500 + 16×palier | 1 500 + 40×palier | 1,65 + 0,015×palier | Stuff faible |
| `reference` | 650 + 20×palier | 1 800 + 55×palier | 1,80 + 0,020×palier | Stuff attendu |
| `reference_high` | 800 + 24×palier | 2 200 + 70×palier | 1,90 + 0,022×palier | Stuff avancé |
| `under` | 0,93×reference | 0,93×reference | — | Stuff légèrement insuffisant |
| `tank` | 0,8×reference | 1,4×reference | — | Régén + réduction |
| `crit` | 0,9×reference | reference | — | 60 % crit |
| `lifesteal` | 0,9×reference | 0,9×reference | — | 12 % vol de vie |
| `endgame` | 6 000 | 15 000 | 3,5 | Cible fin de jeu |

Le build `player36` est le même à tous les paliers : il sert à montrer ce qui arrive **sans progression de stuff**.

## Paramètres de la courbe

La courbe `code` lit les constantes réelles de `Balance.kt` (miroir `balance_constants.py`). Les options `--pity`, `--modifier`, `--ability` et `--potion` permettent les balayages de la porte D. Pour la courbe `target`/`proposed`, les paramètres historiques restent modifiables :

```bash
python tools/equilibrage/balance_sim.py --curve proposed --build reference,reference_high \
  --floors 75,100 --runs 500 \
  --hp-exp 1.11 --hp-cap 30 \
  --atk-exp 1.04 --atk-cap 6 \
  --boss-hp-mult 3 --boss-atk-mult 1.3 --boss-hp-cap 20 \
  --shield-reduction 0.4 --shield-duration 3 --shield-period 8
```

## Ce que le simulateur modélise

- Attaques du héros : intervalle `1 / (VIT × (1 + 0,004 × ferveur))`, minimum **0,18 s** (`Target.HERO_MIN_INTERVAL_MS`, plafond VIT 4,0).
- Dégâts : `ATQ × U(0,9–1,1)`, critique `1,6 + critDamage`, bonus gardien, bouclier.
- Nombre de coups portés / encaissés (pour vérifier qu’un gardien ne tombe pas en 2-3 coups).
- Potions simulées : `--potion atk` (+40 % ATQ, +40 % dégâts subis), `--potion rage` (+50 %/+50 %), `--potion atk_pure` (+40 % sans risque, hypothèse), `--potion crit`, `--potion mixed`.
- Attaques ennemies : intervalle, `ATQ × U(0,9–1,0)`, réduction de dégâts.
- Ration automatique sous **40 %** des PV, soin de **45 %**, une fois par combat.
- Régénération de combat réelle (`0,6 % PV max/s / 3` + bonus), vol de vie.
- **Toutes les capacités** : bouclier, frappe, souffle, drain, régénération, racines, acide, pression, rupture (dégâts, soins, vulnérabilité, VIT, ferveur).
- **Modificateurs** : RAPIDE, BLINDÉ, BERSERK, RÉGÉNÉRANT, VOLEUR, avec le budget de menace (pas de RAPIDE/BERSERK sur une capacité lourde) et `--modifier auto`.
- **Pitié** de gardien (`--pity 0…0,25`) appliquée aux PV et à l’ATQ.
- Règles de biome exactes (PV/ATQ/intervalle ennemis, bonus héros ATQ/PV/VIT/crit/DR/régén/boss).
- Trône brisé (81-100) : rampe +15 % → +35 %, gardiens +10 % PV/+5 % ATQ, boss final ×1,40 PV/×1,15 ATQ.
- Courbe `code` : reproduit `makeMob`/`makeBoss` (courbe cible de 18 s, ATQ par capacité, bouclier, enrage, pitié, modificateurs).
- Élites, XP et pas (`--xp`).

## Ce qu’il ne modélise pas encore

- La ferveur qui décroît après 2 h.
- Les buffs de consommables autres que les potions d'ATQ/crit, les bénédictions.
- La simulation hors ligne, les coffres, les serments, l'économie réelle de farm.
- Le hasard de génération des objets (les presets sont des profils moyens). L'import de sauvegarde reconstruit les statistiques réelles de l'équipement, mais pas les buffs temporaires ni les potions consommées.
- Le vol d'or du VOLEUR (sans effet sur le combat).

## Calibration 0.21.1 — courbe `code`

`--curve code --modifier auto --runs 500` :

| Palier | `reference` | TTK | `reference_high` | `under` |
|---:|---:|---:|---:|---:|
| 36 | 57 % | 18,3 s | 100 % | 37 % |
| 50 | 44 % | 17,8 s | 100 % | 1 % |
| 75 | 60 % | 16,3 s | 100 % | 21 % |
| 100 | 57 % | 20,3 s | 100 % | 2 % |

- `reference` tient la bande 35–65 % et 16–20 s, avec 55–110 coups portés.
- `reference_high` gagne à 100 % : sur-stuff assumé (récompense).
- `under` (0,93×) reste instable à cause du seuil de combat : 1–37 %.
- Pitié 3 % (cap) : `reference` 56–79 %. Potions à risque : 58–78 %. Aucun ne rend le combat automatique.

## Premiers résultats (0.19.1)

### Courbe actuelle

- `player36` gagne le palier 30, perd le **palier 36** : monstre en ~3,8 s (0 % de victoires) et gardien en ~7,5 s (0 %).
- `reference` perd aussi le gardien du 36 (~1 % de victoires) et tous les paliers ≥ 40.
- Le simulateur **reproduit le blocage observé**.

### Courbe proposée (`3× PV`, `1,3× ATQ`, bouclier 40 % / 3 s / 8 s)

- `player36` gagne le **palier 36** : monstre 4,2 s, gardien 6,4 s, sans ration.
- `reference_high` gagne 75 et 100. Au 100 : **18,2 s**, **62 coups portés**, 14 coups encaissés, **83 %** de victoires.
- Avec une potion d’ATQ, le gardien 100 passe à **12,5 s / 43 coups / 100 %** pour le build avancé : la potion accélère, mais le gardien reste très loin d’un combat en 2-3 coups.
- `reference` (stuff moyen) perd encore les gardiens 75 et 100 : il faut du stuff avancé ou du farm.
- Conclusion : la proposition **débloque le palier 36** et atteint la cible « gardien ~18 s, beaucoup de coups » pour le build avancé au 100 ; les gardiens 75/100 restent à calibrer pour le stuff moyen. C’est exactement le rôle du plan de validation.

### Courbe `target` (TTK constant)

La courbe `target` dérive les PV/ATQ ennemis du build de référence pour viser **monstre ~5 s** et **gardien ~18 s** à tous les paliers :

- `reference_high` : gardiens 36/50/75/100 en **17-19 s**, **57-65 coups portés**, 100 % de victoires.
- `reference` (stuff moyen) : **100 % au 36**, **98 % au 50**, puis **~0-1 % aux 75/100** sans potion.
- `player36` (build réel) : **59 % au 36** sans potion, **100 % avec potion** (16,9 s / 55 coups).
- `player36` aux paliers 50+ : 0 %, il faut d’abord du stuff/farm.

Aucun gardien ne tombe en 2-3 coups : le combat dure 17-23 s et demande 55-75 coups. La courbe `target` (ATQ gardien ~EHP/19, enrage après 15 s) débloque le 36 pour le build réel, tout en gardant 75-100 comme objectif de stuff. Les monstres normaux infligent 10 % de moins, mais les PV **ne sont pas restaurés** après victoire : l'attrition est conservée et les potions/rations restent utiles.

## Prochaines étapes

1. Calibrer les profils `reference_*` sur une sauvegarde réelle et sur le barème de butin proposé.
2. Ajouter les sets, les capacités de boss et les consommables au simulateur.
3. Utiliser `docs/validation-equilibrage.md` pour les critères et le protocole.
