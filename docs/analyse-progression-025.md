# Analyse de progression 0.25.0 — cinq profils, paliers 1 → 101

> 5 graines par profil × rythme · médianes · le run s’arrête au début du palier 101.

## 1. Méthode

- **Profils** : passif minimal (auto-optimisation **désactivée**, aucun coffre/fusion/forge/camp/soin), passif optimisé (auto-optimisation + coffres + soins), normal, optimisé, hyper-optimisé.
- **Pas** : 10 pas = 1 m, 100 m par palier, 8 combats normaux, portail à 90 m, gardien à 100 m.
- **Loot** : butin adapté au niveau, pity rare tous les 5 monstres, coffres, fusions non-set pour l’auto, forge et camp avec réserves.
- **Combat** : monstres analytiques ; gardiens simulés avec `balance_sim.py` (capacités, modificateurs, enrage, pity, potions).
- **Gardien final** : PV adaptés au DPS, phases 66 %/33 %, capacité accélérée ; le simulateur approxime les phases par une ATQ ×1,08 et une période de 6 s.
- **Économie** : la relance du modificateur, le marchand et le respec ne sont **pas** modélisés dans les combats ; ils sont traités en test appareil.

## 2. Jours pour terminer

| Profil | 5 000 pas/j | 8 000 pas/j | 15 000 pas/j | Palier final | Niv. héros |
|---|---:|---:|---:|---:|---:|
| **Passif minimal (auto off)** | non fini<br>(mur p.32) | non fini<br>(mur p.31) | non fini<br>(mur p.34) | 34 / 101 | 107 |
| **Passif optimisé (auto on)** | 32,3 j (5/5) | 22,3 j (5/5) | 12,0 j (5/5) | 101 / 101 | 96 |
| **Normal** | 30,8 j (5/5) | 20,2 j (5/5) | 13,0 j (5/5) | 101 / 101 | 97 |
| **Optimisé** | 31,8 j (5/5) | 21,5 j (5/5) | 11,2 j (5/5) | 101 / 101 | 96 |
| **Hyper-optimisé** | 36,0 j (5/5) | 22,0 j (5/5) | 11,5 j (5/5) | 101 / 101 | 98 |

Le **passif minimal ne termine pas** : il atteint le palier 31–34, niveau 104–107, puis reste bloqué. Le **passif optimisé** termine 5/5 à tous les rythmes.

## 3. 0.24 → 0.25 : ce qui change

La courbe 1 → 99 est **inchangée** pour les quatre profils déjà suivis en 0.24 : mêmes jours et mêmes replis. Le lot 2 ne touche ni au butin, ni aux monstres, ni aux gardiens intermédiaires. Deux changements structurent cette version :

- **Nouveau profil par défaut** : l’auto-optimisation étant désactivée par défaut, le « passif » de 0.24 devient le **passif optimisé** (auto on), et le **passif minimal** (auto off) apparaît comme le vrai profil subi. Il ne termine pas.
- **Gardien final rallongé** : la cible passe de « 10–15 s » à **20–30 s** avec des PV adaptatifs et trois phases.

| Profil | TTK final 0.24 (8 000 pas/j) | TTK final 0.25 (8 000 pas/j) | Victoire 0.25 |
|---|---:|---:|---:|
| Passif (0.24, auto on) → Passif optimisé | 12,8 s | 35,2 s | 100 % |
| Normal | 15,1 s | 28,7 s | 60 % |
| Optimisé | 8,4 s | 21,1 s | 100 % |
| Hyper-optimisé | 6,0 s | 19,7 s | 100 % |

Le combat final est donc passé d’un **pic de 6–15 s** à un vrai combat de **20–35 s**. La cible est tenue pour le normal, l’optimisé et l’hyper ; le passif optimisé dépasse à 35 s (voir § 6).

## 4. Statistiques de run

| Profil | Pas/j | Jours | Niv. | Replis | Tentatives gardien | Fermes | Coffres | Or | Rations |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Passif minimal (auto off) | 5000 | 237,6 | 106 | 6665 | 924 | 19995 | 0 | 2 253 077 | 6865 |
| Passif minimal (auto off) | 8000 | 113,4 | 104 | 5327 | 1594 | 15981 | 0 | 1 664 612 | 4584 |
| Passif minimal (auto off) | 15000 | 80,9 | 107 | 7194 | 1651 | 21582 | 0 | 2 414 268 | 4346 |
| Passif optimisé (auto on) | 5000 | 32,3 | 95 | 131 | 131 | 393 | 151 | 2 379 198 | 1342 |
| Passif optimisé (auto on) | 8000 | 22,3 | 95 | 198 | 155 | 594 | 163 | 2 395 765 | 1257 |
| Passif optimisé (auto on) | 15000 | 12,0 | 96 | 204 | 132 | 612 | 175 | 2 492 411 | 1116 |
| Normal | 5000 | 30,8 | 95 | 92 | 125 | 276 | 145 | 2 345 236 | 1303 |
| Normal | 8000 | 20,2 | 95 | 129 | 118 | 387 | 156 | 2 379 698 | 1184 |
| Normal | 15000 | 13,0 | 97 | 260 | 120 | 780 | 166 | 2 559 095 | 1140 |
| Optimisé | 5000 | 31,8 | 96 | 50 | 118 | 150 | 146 | 2 579 997 | 1239 |
| Optimisé | 8000 | 21,5 | 96 | 88 | 116 | 264 | 165 | 2 616 637 | 1150 |
| Optimisé | 15000 | 11,2 | 96 | 102 | 116 | 306 | 153 | 2 630 431 | 1011 |
| Hyper-optimisé | 5000 | 36,0 | 98 | 28 | 136 | 84 | 187 | 3 087 794 | 1183 |
| Hyper-optimisé | 8000 | 22,0 | 97 | 16 | 133 | 48 | 170 | 3 084 953 | 1019 |
| Hyper-optimisé | 15000 | 11,5 | 98 | 31 | 133 | 93 | 167 | 3 063 394 | 972 |

## 5. TTK par palier — 8 000 pas/j

Un tiret dans une colonne TTK signifie qu’aucun combat n’a été gagné sur les simulations, pas une donnée manquante. Les win rates sont mesurés à l’arrivée au palier, pas après le farm.

#### Passif minimal (auto off)

| Palier | Niv. | ATQ | PV | VIT | Crit | Camp A/S/V | TTK facile | TTK moyen | TTK coriace | TTK élite | TTK gardien | Victoire gardien | PV perdus | Rations |
|---:|---:|---:|---:|---:|---:|---|---:|---:|---:|---:|---:|---:|---:|---:|
| 10 | 33 | 176 | 951 | 1,45 | 26 | 0/0/0 | 2,0 | 2,4 | 2,7 | 4,5 | — | 0 % | — | 1,00 |
| 20 | 50 | 239 | 1254 | 1,87 | 46 | 0/0/0 | 2,7 | 2,8 | 2,9 | 4,7 | 9,1 | 42 % | 1144 | 1,00 |
| 30 | 93 | 515 | 2337 | 2,19 | 54 | 0/0/0 | 9,2 | 9,3 | 9,3 | — | — | 0 % | — | 1,00 |

#### Passif optimisé (auto on)

| Palier | Niv. | ATQ | PV | VIT | Crit | Camp A/S/V | TTK facile | TTK moyen | TTK coriace | TTK élite | TTK gardien | Victoire gardien | PV perdus | Rations |
|---:|---:|---:|---:|---:|---:|---|---:|---:|---:|---:|---:|---:|---:|---:|
| 10 | 27 | 213 | 811 | 1,45 | 43 | 3/3/0 | 1,1 | 1,3 | 1,4 | 2,7 | 7,3 | 100 % | 440 | 0,80 |
| 20 | 45 | 469 | 1561 | 1,87 | 75 | 3/4/1 | 0,5 | 0,5 | 0,5 | 0,9 | 2,4 | 100 % | 294 | 0,00 |
| 30 | 64 | 908 | 2615 | 2,63 | 75 | 8/8/3 | 3,1 | 3,3 | 3,4 | 5,1 | — | 0 % | — | 1,00 |
| 40 | 72 | 976 | 3651 | 3,04 | 75 | 10/9/4 | 5,9 | 6,0 | 6,2 | — | 18,9 | 34 % | 3470 | 1,00 |
| 50 | 78 | 1014 | 6638 | 4,00 | 75 | 26/27/16 | 4,8 | 4,9 | 5,0 | 7,6 | 21,6 | 99 % | 4757 | 1,00 |
| 60 | 82 | 1050 | 7356 | 3,94 | 75 | 30/30/22 | 5,1 | 5,2 | 5,4 | 8,0 | 24,3 | 58 % | 6274 | 1,00 |
| 70 | 86 | 1199 | 7104 | 4,00 | 75 | 30/30/28 | 4,7 | 4,8 | 4,9 | 7,2 | 23,5 | 31 % | 7237 | 1,00 |
| 80 | 90 | 1899 | 6992 | 4,00 | 75 | 30/30/30 | 4,3 | 4,4 | 4,5 | 6,7 | 17,2 | 100 % | 4564 | 1,00 |
| 90 | 93 | 2509 | 7635 | 4,00 | 75 | 30/30/30 | 3,6 | 3,7 | 3,7 | 5,5 | 11,7 | 100 % | 2889 | 0,57 |
| 100 | 96 | 2786 | 11829 | 4,00 | 75 | 30/30/30 | 3,7 | 3,8 | 3,8 | 5,7 | 35,2 | 100 % | 1949 | 0,18 |

#### Normal

| Palier | Niv. | ATQ | PV | VIT | Crit | Camp A/S/V | TTK facile | TTK moyen | TTK coriace | TTK élite | TTK gardien | Victoire gardien | PV perdus | Rations |
|---:|---:|---:|---:|---:|---:|---|---:|---:|---:|---:|---:|---:|---:|---:|
| 10 | 27 | 201 | 1111 | 1,53 | 43 | 2/2/0 | 1,5 | 1,6 | 1,8 | 3,2 | 13,5 | 100 % | 520 | 1,00 |
| 20 | 43 | 407 | 1902 | 2,27 | 75 | 3/3/0 | 0,8 | 0,8 | 0,9 | 1,5 | 4,5 | 100 % | 555 | 0,00 |
| 30 | 59 | 1039 | 2481 | 2,58 | 75 | 12/12/6 | 2,9 | 3,0 | 3,2 | 4,8 | 10,6 | 32 % | 2406 | 1,00 |
| 40 | 68 | 1015 | 3707 | 2,67 | 75 | 12/12/12 | 6,4 | 6,7 | 6,8 | — | 14,7 | 100 % | 2148 | 1,00 |
| 50 | 75 | 1182 | 5288 | 3,49 | 75 | 12/12/12 | 4,7 | 4,8 | 4,9 | 7,3 | 16,7 | 100 % | 3292 | 1,00 |
| 60 | 80 | 1202 | 6254 | 4,00 | 75 | 12/12/12 | 5,4 | 5,5 | 5,6 | 8,3 | 17,7 | 100 % | 3436 | 1,00 |
| 70 | 84 | 1548 | 6162 | 4,00 | 75 | 12/12/12 | 4,3 | 4,3 | 4,4 | 6,6 | 15,4 | 100 % | 3756 | 1,00 |
| 80 | 88 | 1399 | 6682 | 4,00 | 75 | 12/12/12 | 5,8 | 5,8 | 5,9 | 8,8 | 18,5 | 100 % | 3134 | 0,00 |
| 90 | 92 | 2110 | 8688 | 3,95 | 75 | 12/12/12 | 4,0 | 4,0 | 4,1 | 6,1 | 13,2 | 100 % | 3682 | 0,41 |
| 100 | 95 | 2406 | 8988 | 3,45 | 75 | 12/12/12 | 4,3 | 4,4 | 4,4 | 6,5 | 28,7 | 60 % | 6800 | 1,00 |

#### Optimisé

| Palier | Niv. | ATQ | PV | VIT | Crit | Camp A/S/V | TTK facile | TTK moyen | TTK coriace | TTK élite | TTK gardien | Victoire gardien | PV perdus | Rations |
|---:|---:|---:|---:|---:|---:|---|---:|---:|---:|---:|---:|---:|---:|---:|
| 10 | 27 | 303 | 1101 | 1,46 | 46 | 3/3/0 | 0,5 | 0,6 | 0,6 | 1,1 | 5,0 | 100 % | 439 | 0,01 |
| 20 | 44 | 534 | 1697 | 1,93 | 75 | 6/6/1 | 0,5 | 0,5 | 0,5 | 1,0 | 2,5 | 100 % | 298 | 0,00 |
| 30 | 57 | 845 | 2498 | 2,74 | 75 | 8/9/4 | 2,5 | 2,6 | 2,7 | 4,0 | 7,2 | 12 % | 2495 | 1,00 |
| 40 | 66 | 898 | 4438 | 3,22 | 75 | 19/17/8 | 4,3 | 4,5 | 4,5 | 6,6 | 9,8 | 100 % | 1900 | 1,00 |
| 50 | 73 | 1124 | 4350 | 3,78 | 75 | 25/25/20 | 2,8 | 2,8 | 2,9 | 4,3 | 8,0 | 100 % | 2067 | 0,96 |
| 60 | 79 | 1151 | 4744 | 3,67 | 75 | 25/25/25 | 3,0 | 3,1 | 3,2 | 4,3 | 9,5 | 100 % | 2515 | 0,99 |
| 70 | 85 | 1619 | 6922 | 4,00 | 75 | 25/25/25 | 2,4 | 2,4 | 2,5 | 3,7 | 8,6 | 100 % | 2722 | 0,61 |
| 80 | 89 | 1849 | 6849 | 4,00 | 75 | 25/25/25 | 2,5 | 2,6 | 2,7 | 4,0 | 7,3 | 100 % | 3600 | 0,00 |
| 90 | 92 | 1938 | 8427 | 4,00 | 75 | 25/25/25 | 2,2 | 2,2 | 2,3 | 3,4 | 7,1 | 100 % | 2740 | 0,00 |
| 100 | 96 | 1958 | 8542 | 4,00 | 75 | 25/25/25 | 2,5 | 2,5 | 2,6 | 3,8 | 21,1 | 100 % | 3250 | 0,66 |

#### Hyper-optimisé

| Palier | Niv. | ATQ | PV | VIT | Crit | Camp A/S/V | TTK facile | TTK moyen | TTK coriace | TTK élite | TTK gardien | Victoire gardien | PV perdus | Rations |
|---:|---:|---:|---:|---:|---:|---|---:|---:|---:|---:|---:|---:|---:|---:|
| 10 | 26 | 201 | 951 | 1,45 | 54 | 4/4/1 | 0,7 | 0,8 | 0,8 | 1,5 | 6,7 | 100 % | 442 | 0,67 |
| 20 | 45 | 619 | 1866 | 2,04 | 75 | 7/6/1 | 0,3 | 0,3 | 0,4 | 0,5 | 1,5 | 100 % | 150 | 0,00 |
| 30 | 57 | 861 | 3941 | 3,10 | 75 | 8/8/4 | 2,0 | 2,0 | 2,1 | 3,2 | 6,7 | 100 % | 1431 | 0,28 |
| 40 | 66 | 954 | 5189 | 3,78 | 75 | 10/11/5 | 2,7 | 2,7 | 2,7 | 4,2 | 5,4 | 100 % | 1876 | 0,01 |
| 50 | 73 | 1150 | 5449 | 4,00 | 75 | 27/27/18 | 2,6 | 2,6 | 2,7 | 4,0 | 7,5 | 100 % | 2010 | 0,53 |
| 60 | 79 | 1732 | 5757 | 4,00 | 75 | 30/30/24 | 1,9 | 1,9 | 1,9 | 2,9 | 5,3 | 100 % | 1889 | 0,01 |
| 70 | 85 | 2011 | 7267 | 4,00 | 75 | 30/30/28 | 1,5 | 1,5 | 1,6 | 2,4 | 5,5 | 100 % | 2359 | 0,00 |
| 80 | 89 | 2313 | 9890 | 4,00 | 75 | 30/30/30 | 1,7 | 1,7 | 1,8 | 2,7 | 5,0 | 100 % | 1954 | 0,00 |
| 90 | 93 | 2333 | 10872 | 4,00 | 75 | 30/30/30 | 1,9 | 2,0 | 2,0 | 2,9 | 5,1 | 100 % | 1202 | 0,00 |
| 100 | 96 | 2349 | 10968 | 4,00 | 75 | 30/30/30 | 2,2 | 2,2 | 2,3 | 3,4 | 19,7 | 100 % | 4347 | 0,11 |

## 6. Gardien final — 20–30 s ?

| Profil | 5 000 pas/j | 8 000 pas/j | 15 000 pas/j |
|---|---:|---:|---:|
| Passif minimal (auto off) | non atteint | non atteint | non atteint |
| Passif optimisé (auto on) | 27,9 s · 59 % | 35,2 s · 100 % | 34,9 s · 100 % |
| Normal | 28,2 s · 46 % | 28,7 s · 60 % | 29,3 s · 60 % |
| Optimisé | 19,7 s · 100 % | 21,1 s · 100 % | 19,6 s · 100 % |
| Hyper-optimisé | 20,9 s · 100 % | 19,7 s · 100 % | 20,2 s · 100 % |

Le gardien final est dans la cible pour l’optimisé et l’hyper (≈20–21 s) et pour le normal (≈28–29 s). Le **passif optimisé** est à 28 s à 5 000 pas/j, mais à **35 s à 8 000/15 000** : son DPS plus faible déclenche le plancher de PV et allonge le combat. Il gagne quand même 100 %, mais le combat devient long et peu tendu.

## 7. Analyse par profil

### Passif minimal — le vrai « ne rien faire »

- **0/5** à tous les rythmes, mur aux paliers **31–34**.
- **5 327–7 194 replis**, **16 000–21 600 combats de farm de récupération**, niveau 104–107.
- Il progresse plus loin qu’en 0.23 (mur 21), grâce à la ténacité, au butin adapté et aux élites adoucies, mais il finit par bloquer.
- C’est le profil par défaut depuis que l’auto-optimisation est désactivée. Il faut donc un **signal clair** pour l’activer, sinon le joueur passif abandonne.

### Passif optimisé — la version idle jouable

- **5/5** à 5 000, 8 000 et 15 000 pas/j.
- **32,3 / 22,3 / 12,0 jours**, **131 / 198 / 204 replis**.
- Les replis restent autour de **1,3–2 par palier** : c’est mieux que 0.23 (4 177), mais encore loin de « quelques replis ».
- Le stuff suit mieux le rythme : à 15 000 pas/j, il termine en 12 jours sans mur.
- Le gardien final est son point faible à 8 000/15 000 : **35 s**, 100 % de victoire, donc long et facile.

### Normal — finissable, encore irrégulier

- **30,8 / 20,2 / 13,0 jours**, **92 / 129 / 260 replis**.
- Le rythme rapide reste défavorable : **260 replis** à 15 000, à cause des paliers 40–70 et des élites.
- Le gardien final est **28–29 s** avec **46–60 %** de victoire : le combat est exigeant et mémorable, sans être un mur.
- Les pics de gardiens restent 12, 20–21, 26, 30, 50–70 ; la ténacité/pitié les rendent finissables.

### Optimisé — le profil récompensé

- **31,8 / 21,5 / 11,2 jours**, **50 / 88 / 102 replis**.
- Le gardien final est à **19,7–21,1 s**, 100 % : rapide mais lisible, avec les phases.
- Les décisions de stuff, sets et forge paient. Le milieu de partie reste le plus intéressant.

### Hyper-optimisé — facile, avec un vrai final

- **36,0 / 22,0 / 11,5 jours**, **28 / 16 / 31 replis**.
- Le farm de gardiens explique le 36 jours à 5 000 pas/j : il prend du temps pour sécuriser le stuff.
- Le gardien final est à **19,7–20,9 s**, 100 %, avec un minimum de tension grâce aux phases.
- C’est le profil le plus confortable ; le jeu reste relativement facile comme demandé.

## 8. Points à corriger

1. **Passif minimal non finissable** : l’auto-optimisation est off par défaut. Ajouter une **suggestion au premier lancement** ou un rappel quand le joueur atteint un mur, sans réactiver le mode automatiquement.
2. **Gardien final du passif à 35 s** à 8 000/15 000 : baisser le plancher de PV final de **0,75 à 0,55** pour les builds à faible DPS, ou cibler 24 s. Le combat resterait gagnable mais plus court.
3. **Rythme rapide du normal** : 260 replis à 15 000. Le butin adapté a réduit le problème (1 234 en 0.23), mais un catch-up de stuff ou un adoucissement des élites 40–70 reste utile.
4. **Économie non modélisée** : la relance du modificateur et les potions de camp devraient réduire les replis en jeu réel ; à mesurer avec les prochains rapports.
5. **Pics 40–70** : encore irréguliers pour le normal et le passif optimisé ; la ténacité/pitié évitent le mur, mais le ressenti reste en dents de scie.

## 9. Conclusion — ressenti attendu

- **Passif minimal** : frustration au palier 31, après beaucoup de temps. Priorité : lui proposer l’auto-optimisation.
- **Passif optimisé** : expérience idle crédible, 12–32 jours, mais encore trop de replis et un final trop long à haut rythme.
- **Normal** : bon challenge, final réussi, mais le rythme rapide et les élites restent punitifs.
- **Optimisé** : le meilleur équilibre ; le final est court mais les phases et la pitié gardent un intérêt.
- **Hyper** : relativement facile, final réussi, farm de gardiens assumé ; le minimum de challenge est là.

## 10. Limites

- 5 graines, combats de monstres analytiques, phases du gardien final approximées.
- L’auto-optimisation simulée est une politique modèle (gloutonne, non-set) ; le jeu réel peut différer.
- La relance, le marchand et le respec ne sont pas dans la simulation ; ils sont validés par les tests unitaires et le test appareil.
- Le test de partie fraîche reste la référence pour le ressenti.

## 11. Reproductibilité

```bash
python tools/equilibrage/progression_sim.py --seeds 5 \
  --profiles passif,passif_auto_game,normal,optimise,hyper \
  --steps 5000,8000,15000 \
  --out progression_025_full.json

python tools/equilibrage/report_progression_025.py \
  --data progression_025_full.json \
  --out docs/analyse-progression-025.md
```
