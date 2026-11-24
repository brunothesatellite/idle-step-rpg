# Simulateur d’équilibrage

Outil autonome pour rejouer les combats de **La Marche des Âmes** avec les courbes actuelles ou proposées, sans Android et sans modifier le jeu.

- Script : `tools/equilibrage/balance_sim.py`
- Python 3.10+, **aucune dépendance externe**.
- Les formules sont recopiées depuis `GameEngine.kt` / `Model.kt` ; elles doivent rester synchronisées avec le code.

> **Référence de courbe** : `docs/equilibrage.md` (version 0.20.8) et `docs/plan-equilibrage.md`. Les résultats chiffrés de la section « Premiers résultats » sont **historiques (0.19.1)** et ne servent plus de cible.

## Source de vérité

- Constantes Kotlin : `android/app/src/main/java/com/brunothesatellite/idlesteprpg/engine/Balance.kt`.
- Miroir Python : `tools/equilibrage/balance_constants.py`.
- Vérification : `python tools/equilibrage/check_constants.py` (lancée par la CI Android).

Toute modification de constante doit être répercutée dans les deux fichiers, puis vérifiée avant une simulation.

## Utilisation

```bash
# Courbe actuelle : reproduit le mur du palier 36
python tools/equilibrage/balance_sim.py --curve current --build player36,reference \
  --floors 30,34,36,40 --runs 300

# Courbe proposée : vérifie le déblocage du palier 36
python tools/equilibrage/balance_sim.py --curve proposed --build player36,reference,reference_high \
  --floors 36,50,75,100 --runs 300

# Courbe cible : TTK constant (monstre ~5 s, gardien ~18 s)
python tools/equilibrage/balance_sim.py --curve target --build reference,reference_high \
  --floors 36,50,75,100 --runs 300

# Courbes d’XP et de pas
python tools/equilibrage/balance_sim.py --xp
```

Sur Windows, avec le venv du projet :

```powershell
& .\video_promo\.venv\Scripts\python.exe tools\equilibrage\balance_sim.py --curve proposed --build all --floors 36,50,75,100
```

## Presets de builds

| Preset | ATQ | PV | VIT | Particularité |
|---|---:|---:|---:|---|
| `player36` | 1 349 | 3 699 | 2,27 | Capture réelle du joueur au palier 36 |
| `reference_low` | 500 + 18×palier | 2 200 + 70×palier | 1,70 + 0,015×palier | Stuff faible |
| `reference` | 650 + 24×palier | 2 600 + 110×palier | 1,80 + 0,020×palier | Stuff attendu |
| `reference_high` | 800 + 30×palier | 3 000 + 160×palier | 1,90 + 0,025×palier | Stuff avancé |
| `tank` | 0,8×reference | 1,4×reference | — | Régén + réduction |
| `crit` | 0,9×reference | reference | — | 60 % crit |
| `lifesteal` | 0,9×reference | 0,9×reference | — | 12 % vol de vie |
| `endgame` | 6 000 | 15 000 | 3,5 | Cible fin de jeu |

Le build `player36` est le même à tous les paliers : il sert à montrer ce qui arrive **sans progression de stuff**.

## Paramètres de la courbe proposée

Tout est modifiable pour les balayages :

```bash
python tools/equilibrage/balance_sim.py --curve proposed --build reference,reference_high \
  --floors 75,100 --runs 500 \
  --hp-exp 1.11 --hp-cap 30 \
  --atk-exp 1.04 --atk-cap 6 \
  --boss-hp-mult 3 --boss-atk-mult 1.3 --boss-hp-cap 20 \
  --shield-reduction 0.4 --shield-duration 3 --shield-period 8
```

## Ce que le simulateur modélise

- Attaques du héros : intervalle `1 / (VIT × (1 + 0,004 × ferveur))`, minimum 0,3 s.
- Dégâts : `ATQ × U(0,9–1,0)`, critique `1,6 + critDamage`, bonus gardien, bouclier.
- Nombre de coups portés / encaissés (pour vérifier qu’un gardien ne tombe pas en 2-3 coups).
- Potions simulées : `--potion atk` (+40 % ATQ), `--potion crit` (+50 pts crit), `--potion mixed` (+25 % ATQ, +25 crit).
- Attaques ennemies : intervalle, `ATQ × U(0,9–1,0)`, réduction de dégâts.
- Ration automatique sous 25 % des PV, une fois par combat.
- Régénération, vol de vie.
- Capacité de gardien `shield` (réduction périodique) ou `slam` (dégâts périodiques).
- Règles de biome (multiplicateurs PV/ATQ/intervalle, bonus héros).
- Élites.
- XP et pas (`--xp`).

## Ce qu’il ne modélise pas encore

- La ferveur qui décroît après 2 h.
- Les buffs de consommables, les sets (sauf s’ils sont intégrés au build), les bénédictions.
- Toutes les capacités de gardien (seuls `shield` et `slam` sont simulés).
- La simulation hors ligne, les coffres, les serments, l’économie réelle de farm.
- Les builds extraits automatiquement d’une sauvegarde (les presets sont des hypothèses).
- Le hasard de génération des objets (les builds sont des profils moyens).

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
