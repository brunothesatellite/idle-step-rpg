# Analyse de progression 0.25.0 — cinq profils, paliers 1 → 101

> 5 graines par profil × rythme · médianes · le run s’arrête au début du palier 101.

## 1. Méthode

- **Profils** : passif minimal (auto-optimisation **désactivée**, aucun coffre/fusion/forge/camp/soin), passif optimisé (auto-optimisation + coffres + soins), normal, optimisé, hyper-optimisé.
- **Pas** : 10 pas = 1 m, 100 m par palier, 8 combats normaux, portail à 90 m, gardien à 100 m.
- **Loot** : butin adapté au niveau, pity rare tous les 5 monstres, coffres, fusions non-set pour l’auto, forge et camp avec réserves.
- **Combat** : monstres analytiques ; gardiens simulés avec `balance_sim.py` (capacités, modificateurs, enrage, pity, potions).
- **Gardien final** : PV adaptés au DPS, phases 66 %/33 %, capacité accélérée ; le simulateur approxime les phases par une ATQ ×1,08 et une période de 6 s.
- **Économie** : relance du modificateur (une fois par approche, si l’estimation de victoire est < 85 %, avec 25/75/100 % de chances selon le profil), marchand de camp (achat de potions si l’inventaire passe sous 1/2/3) et respec (optimisé et hyper au palier 30, remboursement de 70 %, réallocation VIT > ATQ > PV).

## 2. Jours pour terminer

| Profil | 5 000 pas/j | 8 000 pas/j | 15 000 pas/j | Palier final | Niv. héros |
|---|---:|---:|---:|---:|---:|
| **Passif minimal (auto off)** | non fini<br>(mur p.32) | non fini<br>(mur p.31) | non fini<br>(mur p.34) | 34 / 101 | 107 |
| **Passif optimisé (auto on)** | 32,3 j (5/5) | 22,3 j (5/5) | 12,0 j (5/5) | 101 / 101 | 96 |
| **Normal** | 31,9 j (5/5) | 20,3 j (5/5) | 11,0 j (5/5) | 101 / 101 | 96 |
| **Optimisé** | 32,0 j (5/5) | 19,7 j (5/5) | 11,1 j (5/5) | 101 / 101 | 97 |
| **Hyper-optimisé** | 35,2 j (5/5) | 22,0 j (5/5) | 12,1 j (5/5) | 101 / 101 | 98 |

Le **passif minimal ne termine pas** : il atteint le palier 31–34, niveau 104–107, puis reste bloqué. Le **passif optimisé** termine 5/5 à tous les rythmes.

### Pourquoi l’hyper-optimisé met plus de jours que l’optimisé

Ce n’est pas une anomalie : **« jours » = pas totaux ÷ pas par jour**. Or l’hyper-optimisé est défini comme un joueur **prudent**, pas comme un joueur pressé :

- il n’engage un gardien qu’à partir de **75 %** de chances estimées (optimisé 55 %, normal 35 %) ;
- il farme **15 monstres** avant le gardien si la marge est insuffisante (10 / 5) ;
- il refait jusqu’à **3 fois** chaque gardien des paliers 10, 20, … 100 (1 fois / 0).

Chaque farm de gardien = un palier complet re-marché, soit **~900 pas**. En médiane, l’hyper en fait **30** (3 par gardien), l’optimisé **10** (1 par gardien) et le normal **0**. Les **20 paliers supplémentaires** représentent ~18 000 pas, soit **3,6 jours à 5 000 pas/j** ; l’écart médian est de **3,2 jours** (35,2 vs 32,0), le reste étant compensé par les replis évités.

| Profil | Jours 5k | Replis 5k | Farms gardien 5k | Jours 15k | Replis 15k | Farms gardien 15k |
|---|---:|---:|---:|---:|---:|---:|
| Passif optimisé | 32,3 | 131 | 0 | 12,0 | 204 | 0 |
| Normal | 31,9 | 91 | 0 | 11,0 | 155 | 0 |
| Optimisé | 32,0 | 69 | 10 | 11,1 | 120 | 10 |
| Hyper-optimisé | 35,2 | 22 | 30 | 12,1 | 33 | 32 |

À 15 000 pas/j, l’écart hyper/optimisé tombe à **1,0 jour** (12,1 vs 11,1) : le coût fixe du farm est dilué et l’hyper évite **87 replis** (33 contre 120). À 5 000 pas/j, l’optimisé ne meurt que 69 fois, donc le farm de l’hyper n’est pas entièrement rentabilisé en pas.

**Lecture** : « plus optimisé » ne veut pas dire « plus rapide » dans ce tableau. L’hyper optimise la **puissance et la sécurité** (22 replis à 5k contre 69–131), pas le temps de calendrier. Le normal reste proche de l’optimisé (31,9 vs 32,0 à 5k ; 11,0 vs 11,1 à 15k) mais avec plus de replis (91 vs 69 ; 155 vs 120). Pour mesurer la vitesse pure, il faudrait un profil « speedrun » sans farm de gardien.

## 3. 0.24 → 0.25 : ce qui change

Les règles de butin, de monstres et de gardiens intermédiaires sont **inchangées** depuis 0.24. Les sinks économiques déplacent légèrement les trajectoires (relance, respec), mais les jours restent dans la même enveloppe : les écarts de 0,1–2,0 jour sont surtout du bruit de graines. Trois changements structurent cette version :

- **Nouveau profil par défaut** : l’auto-optimisation étant désactivée par défaut, le « passif » de 0.24 devient le **passif optimisé** (auto on), et le **passif minimal** (auto off) apparaît comme le vrai profil subi. Il ne termine pas.
- **Gardien final rallongé** : la cible passe de « 10–15 s » à **20–30 s** avec des PV adaptatifs et trois phases.
- **Économie simulée** : relance du modificateur, marchand de camp et respec sont maintenant dans le simulateur (voir § 8).

| Profil | TTK final 0.24 (8 000 pas/j) | TTK final 0.25 (8 000 pas/j) | Victoire 0.25 |
|---|---:|---:|---:|
| Passif (0.24, auto on) → Passif optimisé | 12,8 s | 35,2 s | 100 % |
| Normal | 15,1 s | 31,7 s | 88 % |
| Optimisé | 8,4 s | 18,6 s | 68 % |
| Hyper-optimisé | 6,0 s | 19,7 s | 100 % |

Le combat final est donc passé d’un **pic de 6–15 s** à un combat de **18,6–35,2 s**. La cible 20–30 s est tenue à **±2 s** pour le normal, l’optimisé et l’hyper à 8 000 pas/j ; le passif optimisé dépasse à 35 s (voir § 6).

## 4. Statistiques de run

| Profil | Pas/j | Jours | Pas totaux | Niv. | Replis | Tentatives gardien | Farm récup. | Coffres | Or | Rations |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Passif minimal (auto off) | 5000 | 237,6 | 1 187 900 | 106 | 6665 | 924 | 19995 | 0 | 2 253 077 | 6865 |
| Passif minimal (auto off) | 8000 | 113,4 | 906 900 | 104 | 5327 | 1594 | 15981 | 0 | 1 664 612 | 4584 |
| Passif minimal (auto off) | 15000 | 80,9 | 1 213 400 | 107 | 7194 | 1651 | 21582 | 0 | 2 414 268 | 4346 |
| Passif optimisé (auto on) | 5000 | 32,3 | 160 750 | 95 | 131 | 131 | 393 | 151 | 2 379 198 | 1342 |
| Passif optimisé (auto on) | 8000 | 22,3 | 177 700 | 95 | 198 | 155 | 594 | 163 | 2 395 765 | 1257 |
| Passif optimisé (auto on) | 15000 | 12,0 | 180 150 | 96 | 204 | 132 | 612 | 175 | 2 492 411 | 1116 |
| Normal | 5000 | 31,9 | 158 700 | 95 | 91 | 125 | 273 | 159 | 2 356 282 | 1325 |
| Normal | 8000 | 20,3 | 161 750 | 95 | 117 | 117 | 351 | 157 | 2 357 035 | 1156 |
| Normal | 15000 | 11,0 | 164 550 | 96 | 155 | 123 | 465 | 165 | 2 466 766 | 1031 |
| Optimisé | 5000 | 32,0 | 159 100 | 96 | 69 | 121 | 207 | 148 | 2 577 757 | 1291 |
| Optimisé | 8000 | 19,7 | 157 000 | 96 | 64 | 118 | 192 | 151 | 2 612 918 | 1124 |
| Optimisé | 15000 | 11,1 | 165 600 | 97 | 120 | 115 | 360 | 150 | 2 642 794 | 1016 |
| Hyper-optimisé | 5000 | 35,2 | 175 450 | 97 | 22 | 137 | 66 | 181 | 3 065 118 | 1110 |
| Hyper-optimisé | 8000 | 22,0 | 175 350 | 98 | 15 | 137 | 45 | 184 | 3 075 847 | 955 |
| Hyper-optimisé | 15000 | 12,1 | 182 000 | 98 | 33 | 137 | 99 | 191 | 3 083 631 | 976 |

La colonne **Farm récup.** compte les combats de récupération après un repli (**3 par défaite**), pas le farm volontaire de gardiens. Les pas totaux incluent tout : marche, combats, replis et farm.

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
| 20 | 43 | 407 | 1902 | 2,27 | 75 | 3/3/0 | 0,8 | 0,9 | 0,9 | 1,5 | 4,6 | 100 % | 592 | 0,00 |
| 30 | 60 | 1152 | 2544 | 2,68 | 75 | 10/9/4 | 3,0 | 3,1 | 3,2 | 4,9 | 11,1 | 48 % | 2275 | 0,80 |
| 40 | 68 | 1142 | 3910 | 2,95 | 75 | 12/12/12 | 5,2 | 5,3 | 5,4 | 8,3 | 14,3 | 100 % | 1513 | 0,99 |
| 50 | 75 | 1150 | 4327 | 4,00 | 75 | 12/12/12 | 4,2 | 4,3 | 4,4 | 6,5 | 12,4 | 100 % | 2015 | 1,00 |
| 60 | 80 | 1261 | 5648 | 4,00 | 75 | 12/12/12 | 3,4 | 3,4 | 3,5 | 5,2 | 13,1 | 100 % | 2630 | 1,00 |
| 70 | 84 | 1431 | 5460 | 4,00 | 75 | 12/12/12 | 3,0 | 3,1 | 3,1 | 4,6 | 13,1 | 100 % | 3747 | 1,00 |
| 80 | 89 | 1691 | 6341 | 4,00 | 75 | 12/12/12 | 3,2 | 3,2 | 3,2 | 4,8 | 12,3 | 100 % | 3091 | 0,00 |
| 90 | 92 | 1704 | 7850 | 4,00 | 75 | 12/12/12 | 3,6 | 3,6 | 3,6 | 5,4 | 11,6 | 100 % | 2083 | 0,00 |
| 100 | 95 | 2125 | 7931 | 4,00 | 75 | 12/12/12 | 3,2 | 3,3 | 3,3 | 4,9 | 31,7 | 88 % | 4287 | 0,59 |

#### Optimisé

| Palier | Niv. | ATQ | PV | VIT | Crit | Camp A/S/V | TTK facile | TTK moyen | TTK coriace | TTK élite | TTK gardien | Victoire gardien | PV perdus | Rations |
|---:|---:|---:|---:|---:|---:|---|---:|---:|---:|---:|---:|---:|---:|---:|
| 10 | 27 | 217 | 1070 | 1,42 | 42 | 2/2/0 | 0,9 | 0,9 | 0,9 | 1,9 | 6,4 | 100 % | 541 | 0,23 |
| 20 | 44 | 524 | 2121 | 2,25 | 75 | 5/5/0 | 0,5 | 0,5 | 0,5 | 0,9 | 1,8 | 100 % | 205 | 0,00 |
| 30 | 57 | 769 | 2896 | 2,75 | 75 | 5/2/6 | 3,3 | 3,4 | 3,6 | 5,3 | 10,2 | 26 % | 2846 | 0,56 |
| 40 | 67 | 1024 | 4273 | 3,16 | 75 | 14/13/13 | 3,3 | 3,4 | 3,5 | 5,3 | 8,8 | 100 % | 1503 | 0,71 |
| 50 | 73 | 1088 | 5657 | 3,70 | 75 | 25/25/20 | 2,9 | 3,0 | 3,0 | 4,6 | 10,3 | 100 % | 2315 | 0,52 |
| 60 | 79 | 1279 | 6017 | 3,58 | 75 | 25/25/25 | 2,7 | 2,6 | 2,7 | 4,0 | 9,8 | 100 % | 1824 | 0,87 |
| 70 | 83 | 1903 | 6018 | 3,67 | 75 | 25/25/25 | 1,8 | 1,8 | 1,9 | 2,8 | 6,6 | 100 % | 2506 | 0,19 |
| 80 | 88 | 1723 | 5957 | 3,74 | 75 | 25/25/25 | 2,5 | 2,5 | 2,5 | 3,8 | 7,0 | 100 % | 2895 | 0,32 |
| 90 | 92 | 1742 | 6591 | 3,79 | 75 | 25/25/25 | 2,8 | 2,7 | 2,8 | 4,1 | 7,3 | 100 % | 2827 | 0,39 |
| 100 | 96 | 1761 | 6706 | 3,84 | 75 | 25/25/25 | 3,1 | 3,1 | 3,1 | 4,3 | 18,6 | 68 % | 4862 | 1,00 |

#### Hyper-optimisé

| Palier | Niv. | ATQ | PV | VIT | Crit | Camp A/S/V | TTK facile | TTK moyen | TTK coriace | TTK élite | TTK gardien | Victoire gardien | PV perdus | Rations |
|---:|---:|---:|---:|---:|---:|---|---:|---:|---:|---:|---:|---:|---:|---:|
| 10 | 26 | 201 | 951 | 1,45 | 54 | 4/4/1 | 0,7 | 0,8 | 0,8 | 1,5 | 6,7 | 100 % | 442 | 0,67 |
| 20 | 45 | 619 | 1866 | 2,04 | 75 | 7/6/1 | 0,3 | 0,3 | 0,4 | 0,5 | 1,5 | 100 % | 150 | 0,00 |
| 30 | 57 | 845 | 3792 | 3,13 | 75 | 3/0/6 | 2,0 | 2,0 | 2,1 | 3,3 | 6,7 | 100 % | 1586 | 0,24 |
| 40 | 66 | 950 | 5104 | 3,80 | 75 | 9/7/6 | 2,6 | 2,7 | 2,8 | 4,2 | 5,4 | 100 % | 1906 | 0,01 |
| 50 | 73 | 1146 | 5426 | 4,00 | 75 | 26/26/20 | 2,6 | 2,6 | 2,7 | 4,0 | 7,5 | 100 % | 2009 | 0,53 |
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
| Normal | — s · 0 % | 31,7 s · 88 % | 28,2 s · 60 % |
| Optimisé | 19,4 s · 100 % | 18,6 s · 68 % | 19,4 s · 100 % |
| Hyper-optimisé | 18,7 s · 100 % | 19,7 s · 100 % | 20,0 s · 100 % |

Le gardien final est dans la cible basse pour l’optimisé et l’hyper (**18,6–20,0 s**) et dans la cible haute pour le normal (**28,2–31,7 s**), mais avec une forte variance : sur le build médian **à l’arrivée** au palier, le normal à 5 000 pas/j ne gagne pas et l’optimisé à 8 000 tombe à 68 %. Ces taux sont mesurés avant le farm et la ténacité, pas sur l’état exact du combat. Le **passif optimisé** reste à 28 s à 5 000, mais à **35 s à 8 000/15 000** : son DPS plus faible déclenche le plancher de PV et allonge le combat.

Un tiret signifie qu’aucune des 200 simulations n’a gagné avec le build médian à l’arrivée, pas que le profil ne finit pas le jeu.

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

- **31,9 / 20,3 / 11,0 jours**, **91 / 117 / 155 replis**.
- Le rythme rapide reste le plus irrégulier : **155 replis** à 15 000, contre 120 pour l’optimisé. La relance et la pitié limitent la casse.
- Le gardien final est **28,2–31,7 s** avec **0–88 %** de victoire à l’arrivée : le build médian à 5 000 ne gagne pas, mais les runs finissent via le farm et la ténacité. C’est le final le plus tendu des profils finisseurs.
- Les pics de gardiens restent 12, 20–21, 26, 30 et 50–70 ; la ténacité/pitié les rendent finissables.

### Optimisé — le profil récompensé

- **32,0 / 19,7 / 11,1 jours**, **69 / 64 / 120 replis**.
- Le gardien final est à **18,6–19,4 s**, **68–100 %** à l’arrivée : rapide mais lisible, avec les phases.
- La relance et le respec au palier 30 sont utilisés, mais restent marginaux dans l’économie actuelle.
- Les décisions de stuff, sets et forge paient. Le milieu de partie reste le plus intéressant.

### Hyper-optimisé — facile, avec un vrai final

- **35,2 / 22,0 / 12,1 jours**, **22 / 15 / 33 replis**.
- Le farm de gardiens explique le **35,2 jours** à 5 000 pas/j : il prend du temps pour sécuriser le stuff.
- Le gardien final est à **18,7–20,0 s**, 100 %, avec un minimum de tension grâce aux phases.
- C’est le profil le plus confortable ; le jeu reste relativement facile comme demandé.

## 8. Économie — relance, marchand, respec

Les trois sinks sont maintenant simulés avec des politiques par profil :

| Profil | Relances 5k / 8k / 15k | Potions achetées | Respecs | Or remboursé (respec) |
|---|---:|---:|---:|---:|
| Passif minimal | 0 / 0 / 0 | 0 | 0 | 0 |
| Passif optimisé | 0 / 0 / 0 | 0 | 0 | 0 |
| Normal | 1 / 1 / 2 | 0 | 0 | 0 |
| Optimisé | 0 / 0 / 1 | 0 | 1 | 1 498 / 1 974 / 1 194 |
| Hyper-optimisé | 0 / 0 / 0 | 0 | 1 | 1 676 / 1 918 / 2 208 |

- **Relance** : utilisée seulement si l’estimation de victoire est < 85 %, avec 25/75/100 % de chances. Les profils forts (hyper) ne relancent presque jamais, ce qui est rationnel : ils n’en ont pas besoin. Coût 200 + 30 × palier, négligeable face aux 2,3–3,1 M d’or gagnés.
- **Marchand** : aucune potion achetée. L’inventaire moyen avant un gardien est de ~148 potions (maximum > 300) : les coffres et les gardiens en donnent bien plus que la consommation. Le marchand est donc un sink sans rareté.
- **Respec** : utilisé une fois au palier 30 par l’optimisé et l’hyper ; le remboursement de 70 % ne représente que 1 200–2 200 or, et le camp est reconstruit aussitôt. Effet combat nul ou dans le bruit des graines.

**Conclusion** : les trois sinks sont **modélisés**, mais restent **marginaux** dans l’économie actuelle. Pour qu’ils pèsent, il faut soit réduire l’or gagné, soit augmenter les coûts, soit rendre les potions plus rares/plus utiles (ou proposer des offres de marchand plus fortes). C’est le prochain chantier économique.

## 9. Points à corriger

1. **Passif minimal non finissable** : l’auto-optimisation est off par défaut. Ajouter une **suggestion au premier lancement** ou un rappel quand le joueur atteint un mur, sans réactiver le mode automatiquement.
2. **Gardien final des builds faibles** : passif optimisé à 35 s, normal à 5 000 sans victoire sur le build médian, optimisé à 8 000 à 68 %. Baisser le plancher de PV final de **0,75 à 0,55** pour les builds à faible DPS, ou cibler 24 s.
3. **Rythme rapide du normal** : encore **155 replis** à 15 000 (contre 120 pour l’optimisé). Le butin adapté a réduit le problème (1 234 en 0.23), mais un catch-up de stuff ou un adoucissement des élites 40–70 reste utile.
4. **Économie marginale** : relances 0–2 par run, **0 potion achetée** (inventaire moyen ~148), respec une fois pour ~1,2–2,2k or remboursé. Les sinks sont modélisés mais ne pèsent pas ; calibrer l’or, les coûts ou la rareté des potions.
5. **Pics 40–70** : encore irréguliers pour le normal et le passif optimisé ; la ténacité/pitié évitent le mur, mais le ressenti reste en dents de scie.

## 10. Conclusion — ressenti attendu

- **Passif minimal** : frustration au palier 31, après beaucoup de temps. Priorité : lui proposer l’auto-optimisation.
- **Passif optimisé** : expérience idle crédible, 12–32 jours, mais encore trop de replis et un final trop long à haut rythme.
- **Normal** : bon challenge, final tendu et variable (0–88 % à l’arrivée), rythme rapide encore irrégulier.
- **Optimisé** : le meilleur équilibre ; le final est court mais les phases et la pitié gardent un intérêt.
- **Hyper** : relativement facile, final réussi, farm de gardiens assumé ; le minimum de challenge est là.

## 11. Limites

- 5 graines, combats de monstres analytiques, phases du gardien final approximées.
- L’auto-optimisation simulée est une politique modèle (gloutonne, non-set) ; le jeu réel peut différer.
- Les politiques d’économie sont simplifiées : relance si l’estimation < 85 %, marchand si l’inventaire est bas, respec au palier 30. Le marchand n’est pas utilisé car les potions sont abondantes.
- Les taux du gardien final sont mesurés sur le build médian à l’arrivée au palier, avant le farm et la ténacité ; le combat réel peut différer.
- Le test de partie fraîche reste la référence pour le ressenti.

## 12. Reproductibilité

```bash
python tools/equilibrage/progression_sim.py --seeds 5 \
  --profiles passif,passif_auto_game,normal,optimise,hyper \
  --steps 5000,8000,15000 \
  --out progression_025_econ_full.json

python tools/equilibrage/report_progression_025.py \
  --data progression_025_econ_full.json \
  --out docs/analyse-progression-025.md
```
