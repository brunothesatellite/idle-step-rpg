# Équilibrage de la progression — analyse et recommandations

> Demande : rapport dans `doc/equilibrage.md`. Le dépôt utilise le dossier `docs/`, ce rapport est donc dans **`docs/equilibrage.md`**.
> Version analysée : **0.19.1** (`versionCode 46`), sauvegarde **v7**. Aucun code n’a été modifié.
> Simulateur : **`tools/equilibrage/balance_sim.py`**. Plan de validation : **`docs/validation-equilibrage.md`**.

## 1. Résumé exécutif

Le blocage au **palier 36** n’est pas un problème de pas : 100 m = 1 000 pas, donc 64 paliers ne représentent que **64 000 pas** (6 à 13 jours de marche pure). Le problème est un empilement de trois murs :

1. **Mur de survie** : les dégâts ennemis croissent en `1,12^(palier−10)` alors que les PV du héros croissent linéairement. Au palier 36, un monstre normal inflige ~1 500 dégâts toutes les 1,31 s pour 3 699 PV : le héros meurt en **3 coups**. Le gardien inflige ~983 par coup et dispose de **BLINDAGE** : le héros meurt en **5 coups**.
2. **Mur d’XP** : `xpNext = 120 × 1,2^(niveau−1)`. Le passage 34→35 demande **49 222 XP**, et le total 1→100 atteint **41,4 milliards d’XP**. Au rythme actuel (~838 XP par monstre au palier 36), cela représente **~49 millions de monstres**. Le niveau 100 est mathématiquement hors d’atteinte.
3. **Mur de ressources** : le joueur est à **0 éclat et 0 ration**. Les éclats sont la monnaie des améliorations d’objets (1 par niveau), les rations la seule source de soin en combat. Sans éclats ni rations, il ne peut ni progresser en équipement ni survivre, donc il ne peut plus gagner d’éclats : **spirale de blocage**.

Le levier « pas » est sain ; ce sont les courbes de combat, d’XP, de butin et de monnaie qui sont à recalibrer.

## 2. Contexte mesuré

Capture du joueur :

| Élément | Valeur |
|---|---|
| Palier | 36 (Forge noyée) |
| Niveau | 34 |
| Puissance | 3 992 |
| ATQ | 1 349 |
| PV | 3 699 |
| VIT | 2,27 |
| Or | 181 k |
| Rations | 0 / 24 |
| Éclats | 0 / 12 |
| Consommables | 2 |
| Camp | Lame 28, Souffle 27, Pas vifs 19 |
| Prochain gardien | Seigneur de la Forge, palier 36, niveau 38, 6× PV, phase 2, BLINDAGE |

Équipement : 6 pièces épiques/rares, dont plusieurs fusionnées ; 12 objets sur 24.

## 3. Courbes actuelles

### 3.1 Pas et distance

- `10 pas = 1 m`, `100 m = 1 palier` → **1 000 pas par palier**.
- 36 → 100 = 64 paliers = **64 000 pas**.
- À 5 000 pas/jour : 12,8 jours de marche pure ; à 10 000 : 6,4 jours.
- Avec 2 à 3 tentatives par palier (replis de 20 m), compter **20 à 40 jours**.
- Simulation hors ligne : 30 % des pas du jour, plafonnée à 4 min simulées. Les coffres forcés consomment 150/400/900 pas.

**Conclusion** : le rythme de pas est compatible avec un niveau 100 en quelques semaines/mois. Ce n’est pas le goulot.

### 3.2 Ennemis

Formules actuelles :

```kotlin
depthMob  = min(1.12^(palier−10), 40)
depthBoss = min(1.08^(palier−10), 40)

PV monstre  = (55 + 34×(niveau−1)) × depthMob × règleBiome
ATQ monstre = (6 + 2.2×(niveau−1)) × depthMob × règleBiome

PV gardien  = 6 × (55 + 34×(niveau+1)) × depthBoss × règleBiome
ATQ gardien = 1.6 × (6 + 2.2×(niveau+1)) × depthBoss × règleBiome
```

| Palier | PV monstre | ATQ monstre | PV gardien | ATQ gardien | depth mob/boss |
|---:|---:|---:|---:|---:|---:|
| 10 | 0,4 k | 26 | 2,6 k | 48 | 1,0 / 1,0 |
| 20 | 2,2 k | 148 | 10,0 k | 180 | 3,1 / 2,2 |
| 30 | 10,0 k | 673 | 31,0 k | 553 | 9,6 / 4,7 |
| 34 | 17,9 k | 1 193 | 47,4 k | 842 | 15,2 / 6,3 |
| **36** | **23,7 k** | **1 580** | **58,3 k** | **1 034** | **19,0 / 7,4** |
| 40 | 41,4 k | 2 750 | 87,5 k | 1 549 | 30,0 / 10,1 |
| 45 | 62,0 k | 4 112 | 143,6 k | 2 536 | 40,0 / 14,8 |
| 50 | 68,8 k | 4 552 | 233,2 k | 4 109 | 40,0 / 21,7 |
| 100 | 136,8 k | 8 952 | 837,4 k | 14 605 | 40,0 / 40,0 |

Remarques :

- Le multiplicateur `depthMob` est **exponentiel** et s’applique **aux PV et à l’ATQ**. C’est l’ATQ qui casse le jeu.
- Le `cap 40` crée un plateau : au-delà du palier ~41 pour les monstres (et ~73 pour les gardiens), les ennemis ne progressent plus. L’équilibrage dépend alors uniquement du stuff, ce qui rend la fin de partie incohérente.
- Le libellé « 6× PV » est trompeur : le gardien n’a que ~2,4× les PV d’un monstre normal au même palier, car son multiplicateur de profondeur est plus faible (1,08 au lieu de 1,12). En revanche, son bouclier et ses capacités le rendent bien plus dangereux.
- Les élites (10 %) ont **2,2× PV** et **1,3× ATQ** : au palier 36, 52 k PV et 1 950 ATQ. Un élite est un mini-boss.

### 3.3 Héros

```kotlin
ATQ  = 18 + 3×(niveau−1) + 3×camp_lame + 2×(palier/10) + équipement
PV   = 150 + 18×(niveau−1) + 30×camp_souffle + 12×(palier/10) + équipement
VIT  = 1 + 0,01×(niveau−1) + 0,025×camp_pas + équipement
```

- Les gains de niveau sont **linéaires et faibles** : +3 ATQ, +18 PV, +1 % VIT.
- Les améliorations de camp sont **plates** : +3 ATQ, +30 PV, +2,5 % VIT.
- Les objets gagnent **8 % par niveau d’objet**, mais chaque niveau coûte **1 éclat** + de l’or (`30×niveau + 20`).
- Le stuff du joueur apporte ~1 142 ATQ et ~2 109 PV, soit l’essentiel de sa puissance.

### 3.4 Combat au palier 36

Estimation avec 1 349 ATQ, 2,27 VIT, ~30-40 % crit :

- Intervalle du héros : `1000 / (2,27 × 1,4) ≈ 315 ms`.
- DPS de base : ~4 300 ; avec crit : ~5 000-5 300.
- Monstre Forge : 26 020 PV, 1 501 ATQ, 1 312 ms.
  - **TTK ≈ 5 s** ; le monstre frappe 4 fois → ~6 000 dégâts.
  - Héros : 3 699 PV + 1 ration automatique (~1 295) = ~5 000 PV effectifs → **mort en ~3,3 coups**.
  - **TTK > temps de survie** : défaite.
- Gardien : 64 130 PV (83 k si modificateur blindé), ATQ 983.
  - **BLINDAGE** : −65 % de dégâts pendant 4,5 s toutes les 6,5 s → multiplicateur moyen ~0,55.
  - PV effectifs ~116 k → **TTK ~22-27 s**.
  - Le gardien frappe ~17 fois → ~16 700 dégâts ; le héros meurt en **~5 coups (6,5 s)**.
  - **TTK ≫ survie** : défaite.

Le mur commence vers le **palier 32-34** : au palier 30, le monstre fait 673 ATQ (2 coups sur 3 699 PV), ce qui reste gérable ; au 34, 1 193 (3 coups), au 36, 1 501 (2,5 coups).

### 3.5 XP

`xpNext(niveau) = 120 × 1,2^(niveau−1)`.

| Niveau | XP pour le niveau suivant | XP cumulé depuis 1 |
|---:|---:|---:|
| 20 | 3 834 | 18 570 |
| 30 | 23 738 | 118 091 |
| 34 | 49 222 | 245 515 |
| 40 | 146 977 | 734 288 |
| 50 | 910 044 | 4 549 622 |
| 70 | 34 888 896 | 174 443 883 |
| 100 | 8 281 797 452 | **41 408 986 666** |

- Au palier 36 : ~838 XP par monstre, ~5 494 par gardien.
- 34→35 : **59 monstres** ou 9 gardiens.
- 50→51 : ~1 086 monstres.
- 1→100 : **~49 millions de monstres** au rythme actuel.

La courbe 1,2^ niveau est incompatible avec un objectif niveau 100. En plus, les récompenses d’XP sont plafonnées (`sqrt(depth)` avec cap 6 pour les monstres et 10 pour les gardiens), donc elles ne suivent pas la courbe.

## 4. Diagnostic

| Problème | Effet | Gravité |
|---|---|---|
| ATQ ennemie exponentielle (1,12^palier) vs PV héros linéaires | mort en 2-4 coups dès le palier 32 | **critique** |
| Courbe d’XP 1,2^niveau | niveau 100 irréaliste (41 Md XP) | **critique** |
| Éclats rares (2/gardien, 1/coffre, cap 12) + 1 éclat/niveau d’objet | améliorations bloquées à 0 éclat | **critique** |
| Rations rares (1/gardien, 1/coffre) + 1 ration/combat | soin impossible, spirale de mort | **critique** |
| Butin non scalable avec le palier (stats de base fixes) | un objet de palier 36 vaut presque un objet de palier 5 | **élevée** |
| Camp : +3 ATQ / +30 PV pour 7 420 or | 181 k d’or inutiles | **élevée** |
| Gardien : 6× PV + bouclier 65 % + capacités | combat 4-10× plus long que normal | **élevée** |
| Farm verrouillé sur le palier courant | impossible de farmer un palier plus sûr | **élevée** |
| Élites 2,2× PV / 1,3× ATQ à 10 % | gauntlet imprévisible | moyenne |
| Cap 40 sur la profondeur | plateau de difficulté en fin de jeu | moyenne |
| Score de puissance (ATQ×2 + PV/4 + …) | ne reflète pas la survie | moyenne |

## 5. Objectifs de design proposés

1. **Le pas reste la ressource principale** : 1 000 pas/palier, 100 paliers = 100 k pas ; 1 à 3 mois selon l’activité.
2. **Niveau 100 atteignable** : ~2 500 à 4 000 monstres tués au total, soit ~2 à 4 « runs » de 100 paliers avec farm.
3. **Combats** :
   - monstre normal : **4 à 7 s** de combat, 5 à 8 coups encaissés ;
   - élite : 8 à 12 s ;
   - gardien : **20 à 30 s**, 8 à 12 coups encaissés avec 1 ration.
4. **Progression** : 1 à 3 tentatives par palier normal, 3 à 6 par gardien ; au-delà de 5 défaites, mécanisme de pitié.
5. **Économie** : 5 à 10 éclats/jour, 1 à 3 niveaux d’objet/jour ; rations suffisantes pour 3 à 5 combats difficiles/jour.
6. **Butin** : un objet de palier 100 doit être nettement meilleur qu’un objet de palier 36, à rareté égale.

## 6. Recommandations d’algorithme

> Les valeurs ci-dessous sont des points de départ à valider par simulation. Elles ne sont pas implémentées.

### 6.1 Séparer les multiplicateurs PV et ATQ

Le multiplicateur de profondeur doit être **différent** pour les PV et l’ATQ :

```kotlin
fun hpDepth(floor: Int)  = min(1.11^(floor−10), 30.0)
fun atkDepth(floor: Int) = min(1.04^(floor−10), 6.0)
fun bossHpDepth(floor)   = min(1.08^(floor−10), 20.0)
```

- Les PV gardent une croissance forte (fights qui durent).
- L’ATQ est plafonnée à **6×** : le héros peut encaisser.
- Le gardien utilise `bossHpDepth`, **3× PV** (au lieu de 6×), **1,3× ATQ** (au lieu de 1,6×), niveau `palier + 1` (au lieu de +2).
- Élites : 6 % de chance, **1,8× PV**, **1,15× ATQ**.

Exemple de courbe proposée :

| Palier | PV monstre | ATQ monstre | PV gardien | ATQ gardien |
|---:|---:|---:|---:|---:|
| 30 | 8,4 k | 153 | 15,0 k | 205 |
| 36 | 18,8 k | 230 | 28,4 k | 307 |
| 50 | 51,6 k | 546 | 105,3 k | 724 |
| 70 | 72,0 k | 947 | 146,1 k | 1 248 |
| 100 | 102,6 k | 1 343 | 207,3 k | 1 763 |

Au palier 36, avec le stuff actuel : monstre tué en ~3,6 s en encaissant ~620 dégâts ; gardien tué en ~10 s (bouclier inclus) en encaissant ~2 300. Le combat redevient gagnable sans être trivial.

### 6.2 Courbe d’XP

Remplacer `1,2^niveau` par une courbe plus douce :

```kotlin
fun xpNext(level: Int) = (120 * 1.09^(level−1)).roundToInt()
```

- Total 1→100 : **~6,8 M XP** (au lieu de 41,4 Md).
- Niveau 34 : 2 064 XP (au lieu de 49 222).
- Niveau 100 : 608 k XP.
- Récompenses : supprimer le plafond `sqrt(depth)` et utiliser un multiplicateur linéaire de palier :

```kotlin
xpMult(floor) = 1 + 0.15 * (floor − 1)
xpMob   = (12 + 5×niveau) × xpMult(floor)
xpBoss  = 10 × xpMob
```

- Palier 36 : ~1 200 XP/monstre, ~12 000/gardien.
- Palier 100 : ~8 000/monstre, ~80 000/gardien.
- Objectif : **20 à 50 monstres par niveau** en fin de partie.

Gains de niveau à renforcer :

- +5 ATQ (au lieu de +3)
- +25 PV (au lieu de +18)
- +1,5 % VIT (au lieu de +1 %)
- éventuellement +1 % crit tous les 5 niveaux.

### 6.3 Butin et objets

Le problème principal : `lootItem` fixe `level = floor` mais **ne scale pas les statistiques**. Un objet rare de palier 36 a les mêmes stats de base qu’un objet rare de palier 5, à 1,6× près.

Ajouter un facteur de palier :

```kotlin
val floorScale = 1 + 0.06 * (floor − 1)   // plafonné à 8
atk  = base.atk  * rarityMult * floorScale
hp   = base.hp   * rarityMult * floorScale
vit  = base.vit  * rarityMult * floorScale
crit = base.crit * rarityMult * floorScale
```

- Palier 36 : ×3,1 ; palier 100 : ×6,9.
- Raretés : 1,2 / 1,6 / 2,1 / 2,8 (au lieu de 1,0 / 1,25 / 1,6 / 2,0 / 2,4).
- Les objets existants gardent leurs stats (pas de migration) ; seuls les nouveaux drops et les améliorations profitent du nouveau barème. Une migration v8 de recalcul est possible mais risquée : à éviter dans un premier temps.

Bonus d’ensemble : passer de valeurs plates (+10 ATQ, +30 PV) à des **pourcentages** (2 pièces : +5 % ATQ/PV ; 4 pièces : +10 % ; 6 pièces : +15 %) ou à des valeurs multipliées par le palier.

Fusion : le résultat vaut actuellement **75 % de la somme** des 3 objets. Le passer à **90 %** (ou 100 % avec une petite perte de rareté) pour que fusionner ne soit pas une punition, et conserver l’effet/rareté.

### 6.4 Éclats et rations

Objectif : sortir de la spirale 0 éclat / 0 ration.

Éclats :

- Gardien : **+3** (au lieu de +2), jalon : **+8** (au lieu de +5).
- Coffre : **+2** (au lieu de +1).
- Élite : 20 % de chance de +1.
- Démontage : 2 / 3 / 6 / 10 selon rareté.
- Plafond : **24** (au lieu de 12).
- Coûts : amélioration 1 éclat (inchangé), reforge **1** (au lieu de 2), resupply 1.

Rations :

- Gardien : **+2** (au lieu de +1), coffre : **+2** (au lieu de +1).
- Monstres : 10 % de chance de drop une ration.
- Plafond : 30.
- Seuil d’utilisation automatique : **30 %** des PV (au lieu de 25 %).
- Veillée gratuite : **+25-30 %** PV (au lieu de 15 %), une fois par jour.
- Camp : repos 1 ration +40 % (inchangé), resupply 1 éclat (inchangé).

### 6.5 Camp

Les améliorations plates sont trop faibles face à l’exponentiel :

- Lame : **+10 ATQ** par niveau (au lieu de +3).
- Souffle : **+80 PV** (au lieu de +30).
- Pas vifs : **+4 % VIT** (au lieu de +2,5 %).
- Ou passer à des bonus **multiplicatifs** : +2 % ATQ, +2 % PV, +1,5 % VIT par niveau.
- Conserver la courbe de coût `1,2^niveau` ; l’or devient enfin un choix (181 k = ~15 niveaux utiles).

### 6.6 Gardiens et pitié

- Bouclier : **−40 % pendant 3 s toutes les 8 s** (au lieu de −65 % / 4,5 s / 6,5 s), ou absorption d’un montant fixe (ex. 15 % des PV max).
- Capacités : réduire les multiplicateurs (slam 2,4→1,8 ; souffle 2,0→1,6 ; drain 1,5→1,2 ; etc.).
- Pitié : chaque défaite sur un gardien donne **+25 % de dégâts** au prochain essai, cumulable jusqu’à **+75 %**, remis à zéro à la victoire. Alternative : −5 % PV/ATQ du gardien par défaite, plafonné à −25 %.
- Afficher les PV/ATQ réels et un indicateur « dangereux » basé sur le ratio ATQ ennemi / PV héros.

### 6.7 Farm et récupération

- Autoriser le farm du **palier précédent** (ou d’un palier choisi), pas seulement du palier courant. Utiliser le niveau et la profondeur du palier farmé.
- Farm : PV ×0,5, ATQ ×0,5, récompenses ×0,4 ; 3 kills puis reprise.
- Après une défaite, proposer **« Reculer d’un palier »** en plus du repli de 20 m.
- Conserver le repli de 20 m et les 60 % de PV ; ajouter un petit gain d’XP/or de consolation.

### 6.8 Score de puissance et télémétrie

Le score actuel `ATQ×2 + PV/4 + VIT×20 + crit + regen×10` survalorise l’ATQ et ignore la survie. Pour le tuning :

- Afficher en debug : DPS estimé, PV effectifs, TTK/TTD par palier, taux de victoire simulé.
- Journaliser : tentatives par palier, défaites, éclats/rations par jour, temps par niveau, niveaux d’objets.
- Seuils d’alerte : >5 tentatives par palier normal, >10 par gardien, >30 % de joueurs bloqués au même palier.

## 7. Plan de tuning conseillé

### Phase A — débloquer le palier 36 (priorité)

1. Séparer `hpDepth` / `atkDepth` (ATQ cap 6×).
2. Gardien : 3× PV, 1,3× ATQ, niveau +1, bouclier réduit.
3. Éclats/rations : revenus doublés, plafonds relevés.
4. Farm du palier précédent.
5. Pitié gardien (+25 %/défaite, cap +75 %).

**Critère** : le build actuel (1 349 ATQ / 3 699 PV) doit gagner un monstre normal en 4-6 s et le gardien en 20-30 s avec 1 ration.

### Phase B — rendre le niveau 100 atteignable

1. Courbe XP 1,09 et récompenses linéaires par palier.
2. Butin scalable par palier.
3. Camp/sets en pourcentage.
4. Gains de niveau renforcés.

**Critère** : 2 500-4 000 kills pour 1→100 ; ~1 niveau par 1-2 paliers ; niveau 100 vers le palier 100.

### Phase C — fin de partie et polish

1. Remplacer le cap 40 par une courbe lisible ou un NG+.
2. Télémétrie et tableau de bord d’équilibrage.
3. Ajuster les biomes (règles, élites) avec les nouvelles courbes.

## 8. Impacts et précautions

- **Aucune migration de sauvegarde n’est nécessaire** pour les formules de combat/XP. Les objets existants gardent leurs stats ; les nouveaux drops utilisent le nouveau barème. Une migration v8 de recalcul des objets serait plus risquée (inflation, perte de repères) et n’est pas recommandée dans un premier temps.
- Les saves v4/v5/v6/v7 doivent rester lisibles ; les tests de migration existants couvrent déjà ce point.
- Le upscale du butin peut rendre les anciens objets obsolètes : prévoir un message ou un « recalcul » optionnel plus tard.
- Les seuils de pitié et les revenus d’éclats doivent être testés sur 7 à 14 jours de jeu réel, pas seulement en simulation.

## 9. Annexes — formules et cibles

### 9.1 Cibles de combat

| Type | TTK cible | Coups encaissés | Notes |
|---|---:|---:|---|
| Monstre normal | 4-7 s | 5-8 | sans ration |
| Élite | 8-12 s | 4-6 | 6 % de chance |
| Gardien | 20-30 s | 8-12 | 1 ration, bouclier |

### 9.2 Cibles de progression

| Élément | Cible |
|---|---|
| Pas par palier | 1 000 (inchangé) |
| Paliers 1→100 | 100 k pas (+ farm/retries) |
| Durée 1→100 | 1 à 3 mois à 5-10 k pas/jour |
| Kills 1→100 | 2 500-4 000 |
| Niveau vs palier | niveau ≈ palier − 2 à palier |
| Éclats/jour | 5-10 |
| Niveaux d’objet/jour | 1-3 |

### 9.3 Courbe XP proposée (1,09)

| Niveau | XP suivant | Total depuis 1 |
|---:|---:|---:|
| 34 | 2 064 | ~21 600 |
| 50 | 8 184 | ~89 600 |
| 70 | 45 840 | ~508 000 |
| 100 | 608 280 | ~6 760 000 |

> Les totaux sont approximatifs ; ils servent d’ordre de grandeur pour le tuning.

### 9.4 Formules actuelles (référence)

```kotlin
xpNext(level) = 120 * 1.2^(level-1)
upgradeCost(item) = 30 * item.level + 20
growStat(v) = v + max(1, round(v * 0.08))
campCost(key, n) = round(45 * 1.2^n / 5) * 5
fusion = 0.75 * somme(3 objets)
depthMob = min(1.12^(floor-10), 40)
depthBoss = min(1.08^(floor-10), 40)
```

---

**Conclusion** : le jeu n’a pas un problème de pas, il a un problème de **courbe de dégâts ennemis**, de **courbe d’XP** et de **monnaie de progression**. En corrigeant d’abord l’ATQ ennemie, les gardiens et l’économie d’éclats/rations, le palier 36 redevient franchissable. En corrigeant ensuite l’XP et le butin, le niveau 100 devient un objectif de quelques semaines/mois, porté par la marche, avec du farm et des améliorations utiles.
