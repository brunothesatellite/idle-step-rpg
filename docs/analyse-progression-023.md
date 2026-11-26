# Analyse de progression 1 → 101 — version 0.23.0

> Simulation complète du palier 1 au début du palier 101 (le gardien du palier 100 est le dernier combat).
> Date : 26/11/2026 · Version : 0.23.0 / 64 · 5 graines par couple profil × pas/jour.

## 1. Méthode

- **Pas** : 10 pas = 1 m, 100 m par palier. Chaque palier contient 8 combats normaux, un portail à 90 m et un gardien à 100 m.
- **Loot** : butin rare garanti tous les 5 monstres, butin rare de gardien avec pity de set tous les 10 gardiens, coffres (18 % après un monstre), fusions 3 → 1 à 90 % de la somme, forge +20 niveaux, camp 30 niveaux.
- **Profils** : le passif minimal équipe le meilleur objet par emplacement mais n’ouvre pas les coffres, ne fusionne pas, n’améliore pas et ne farme pas ; le normal fait un peu de tout ; l’optimisé et l’hyper optimisent, fusionnent, forgent et farment.
- **Combat** : monstres résolus analytiquement (TTK/dégâts moyens, auto-ration), gardiens simulés avec `balance_sim.py` (capacités, modificateurs, enrage, pity, potions).
- **TTK** : temps moyen des combats gagnés, stuff au moment d’arriver au palier, pleine vie + 1 ration. Les colonnes facile/moyen/coriace/élite représentent les trois pools de monstres et les élites ; les espèces partagent la même courbe de stats.
- **Fin** : le run s’arrête quand le gardien du palier 100 tombe. Un run est « non fini » si le héros reste bloqué plus de 200 000 pas sur un palier sans gagner de puissance.

### Résumé exécutif

- **Passif minimal : non finissable.** Mur au palier 21, 3 800–4 200 replis, niveau 100+ atteint mais stuff insuffisant.
- **Passif assisté : finissable** en 29–46 jours selon le rythme, mais avec 579–2 049 replis. L’auto-optimisation est la condition de la finissabilité.
- **Normal : finissable** en 24–46 jours, 358–1 234 replis. Le jeu est challengeant mais très irrégulier ; plus le joueur marche, plus il échoue.
- **Optimisé : finissable** en 18–38 jours, 215–609 replis. C’est le profil le plus récompensé, avec un endgame devenu trop facile.
- **Hyper : finissable** en 13–40 jours, 103–114 replis. Facile après le palier 40, mais le gardien final tombe en 4,5–6,8 s.

## 2. Jours pour terminer

| Profil | 5 000 pas/j | 8 000 pas/j | 15 000 pas/j | Palier final | Niveau final |
|---|---:|---:|---:|---:|---:|
| **Passif minimal (aucune optimisation)** | non fini<br>(mur p.21) | non fini<br>(mur p.21) | non fini<br>(mur p.21) | 21 / 101 | 100 |
| **Passif assisté (auto-optimisation + coffres + soins)** | 46,2 j (5/5) | 41,7 j (5/5) | 29,0 j (4/5) | 101 / 101 | 110,0 |
| **Normal** | 46,3 j (5/5) | 31,3 j (4/5) | 24,3 j (5/5) | 101 / 101 | 107 |
| **Optimisé** | 38,0 j (5/5) | 28,5 j (5/5) | 18,1 j (5/5) | 101 / 101 | 100 |
| **Hyper-optimisé** | 40,1 j (5/5) | 25,8 j (5/5) | 13,4 j (5/5) | 101 / 101 | 98 |

Le **passif minimal ne termine pas** : il reste bloqué au palier 21, même à 100 000 pas simulés par palier, avec un niveau de héros supérieur à 100. Le **passif assisté** (auto-optimisation, coffres, soins) termine, mais avec beaucoup de replis.

## 3. Statistiques de run

| Profil | Pas/j | Jours | Niv. héros | Replis | Tentatives gardien | Fermes | Coffres | Or gagné | Rations utilisées |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Passif minimal | 5000 | 170,8 | 100 | 4177 | 3985 | 12531 | 0 | 908 376 | 4606 |
| Passif minimal | 8000 | 100,5 | 100 | 3928 | 3768 | 11784 | 0 | 893 223 | 4407 |
| Passif minimal | 15000 | 51,4 | 98 | 3764 | 3651 | 11292 | 0 | 796 280 | 3882 |
| Passif assisté | 5000 | 46,2 | 98 | 579 | 266 | 1737 | 188 | 2 605 306 | 1931 |
| Passif assisté | 8000 | 41,7 | 103 | 1321 | 256 | 3963 | 188 | 2 887 358 | 2119 |
| Passif assisté | 15000 | 29,0 | 110,0 | 2049 | 218 | 6147 | 224 | 3 623 928 | 1982 |
| Normal | 5000 | 46,3 | 98 | 358 | 232 | 1074 | 211 | 2 653 268 | 1904 |
| Normal | 8000 | 31,3 | 101,5 | 702 | 244 | 2106 | 192 | 2 945 177 | 1891 |
| Normal | 15000 | 24,3 | 107 | 1234 | 192 | 3702 | 246 | 3 589 774 | 1851 |
| Optimisé | 5000 | 38,0 | 97 | 215 | 172 | 645 | 170 | 2 654 612 | 1498 |
| Optimisé | 8000 | 28,5 | 99 | 335 | 170 | 1005 | 185 | 2 891 725 | 1664 |
| Optimisé | 15000 | 18,1 | 100 | 609 | 176 | 1827 | 192 | 3 036 518 | 1491 |
| Hyper-optimisé | 5000 | 40,1 | 98 | 103 | 141 | 309 | 200 | 3 114 971 | 1451 |
| Hyper-optimisé | 8000 | 25,8 | 98 | 114 | 188 | 342 | 209 | 3 133 600 | 1423 |
| Hyper-optimisé | 15000 | 13,4 | 98 | 113 | 168 | 339 | 190 | 3 112 112 | 1292 |

## 4. TTK par palier

Les tableaux ci-dessous sont donnés à **8 000 pas/jour**, le rythme médian. Les tableaux complets 5 000 / 8 000 / 15 000 sont dans le JSON de simulation ; la section 5 donne les TTK gardien pour les trois rythmes.

Un tiret dans une colonne **TTK** (élite ou gardien) signifie qu’aucun combat n’a été gagné sur les simulations, pas une donnée manquante. Les élites sont le principal générateur de replis des profils normal et optimisé.

Les win rates sont mesurés **à l’arrivée au palier**, avec le stuff du moment, pas après le farm et les retries. Un 0 % à l’arrivée peut donc être suivi d’une victoire après quelques replis ; à l’inverse, un 100 % peut masquer un stuff déjà sur-optimisé.

`Camp (A/S/V)` = niveaux de **Lame affûtée / Souffle ancien / Pas vifs**. Les TTK sont en secondes ; `PV perdus gardien` est la perte moyenne sur les victoires, avec une ration automatique.

#### Passif minimal (bloqué au palier 21)

| Palier | Niv. | ATQ | PV | VIT | Crit | Camp (A/S/V) | TTK facile | TTK moyen | TTK coriace | TTK élite | TTK gardien | Victoire gardien | PV perdus gardien | Rations |
|---:|---:|---:|---:|---:|---:|---|---:|---:|---:|---:|---:|---:|---:|---:|
| 10 | 39 | 199 | 1031 | 1,47 | 36 | 0/0/0 | 1,7 | 1,9 | 2,2 | 4,6 | 12,1 | 100 % | 476 | 1,00 |
| 20 | 76 | 331 | 1845 | 2,08 | 34 | 0/0/0 | 4,3 | 4,5 | 4,7 | 9,5 | 16,8 | 0 % | 1844 | 1,00 |

#### Passif assisté (auto-optimisation + coffres + soins)

| Palier | Niv. | ATQ | PV | VIT | Crit | Camp (A/S/V) | TTK facile | TTK moyen | TTK coriace | TTK élite | TTK gardien | Victoire gardien | PV perdus gardien | Rations |
|---:|---:|---:|---:|---:|---:|---|---:|---:|---:|---:|---:|---:|---:|---:|
| 10 | 29 | 235 | 857 | 1,44 | 45 | 0/0/0 | 1,6 | 1,8 | 2,1 | 4,6 | 10,4 | 21 % | 859 | 1,00 |
| 20 | 51 | 454 | 1746 | 2,17 | 75 | 2/2/0 | 1,9 | 2,0 | 2,2 | 4,4 | 8,1 | 58 % | 1542 | 1,00 |
| 30 | 70 | 786 | 2945 | 3,12 | 75 | 12/12/12 | 3,2 | 3,4 | 3,5 | 6,3 | 11,1 | 100 % | 1317 | 1,00 |
| 40 | 76 | 738 | 3087 | 3,19 | 75 | 12/12/12 | 6,4 | 6,6 | 6,7 | — | 14,5 | 88 % | 2266 | 1,00 |
| 50 | 90 | 1000 | 4989 | 4,00 | 75 | 12/12/12 | 4,1 | 4,2 | 4,3 | 8,0 | 14,6 | 100 % | 1954 | 0,40 |
| 60 | 91 | 1006 | 5288 | 3,72 | 75 | 12/12/12 | 4,9 | 4,9 | 5,0 | 8,6 | 19,7 | 100 % | 2081 | 0,72 |
| 70 | 93 | 1137 | 5080 | 3,74 | 75 | 12/12/12 | 4,6 | 4,7 | 4,7 | — | 19,8 | 100 % | 3407 | 1,00 |
| 80 | 100 | 2165 | 6611 | 3,79 | 75 | 12/12/12 | 3,5 | 3,5 | 3,6 | 6,6 | 9,9 | 100 % | 3609 | 0,00 |
| 90 | 101 | 2183 | 7237 | 3,80 | 75 | 12/12/12 | 3,8 | 3,9 | 3,9 | 7,2 | 10,4 | 100 % | 3356 | 0,00 |
| 100 | 103 | 2770 | 7294 | 3,82 | 75 | 12/12/12 | 3,1 | 3,1 | 3,1 | 5,7 | 8,4 | 100 % | 1663 | 0,00 |

#### Normal

| Palier | Niv. | ATQ | PV | VIT | Crit | Camp (A/S/V) | TTK facile | TTK moyen | TTK coriace | TTK élite | TTK gardien | Victoire gardien | PV perdus gardien | Rations |
|---:|---:|---:|---:|---:|---:|---|---:|---:|---:|---:|---:|---:|---:|---:|
| 10 | 26 | 195 | 780 | 1,58 | 38 | 2/2/0 | 1,5 | 1,8 | 1,9 | 4,3 | 11,2 | 97 % | 433 | 1,00 |
| 20 | 47 | 483 | 1614 | 2,04 | 75 | 3/4/0 | 2,0 | 2,1 | 2,1 | 4,4 | 10,0 | 16 % | 1588 | 1,00 |
| 30 | 66 | 860 | 3126 | 2,70 | 75 | 12/12/12 | 4,8 | 5,0 | 5,2 | — | 17,8 | 60 % | 2719 | 1,00 |
| 40 | 70 | 799 | 4105 | 3,06 | 75 | 12/12/12 | 7,2 | 7,4 | 7,6 | 13,1 | 25,3 | 100 % | 1769 | 0,99 |
| 50 | 80 | 1076 | 4158 | 4,00 | 75 | 12/12/12 | 4,5 | 4,6 | 4,7 | — | 16,5 | 54 % | 4026 | 1,00 |
| 60 | 84 | 1277 | 5312 | 4,00 | 75 | 12/12/12 | 3,6 | 3,6 | 3,7 | 6,4 | 14,9 | 46 % | 4780 | 1,00 |
| 70 | 90 | 1458 | 5505 | 4,00 | 75 | 12/12/12 | 3,1 | 3,2 | 3,2 | 5,4 | 14,3 | 30 % | 5557 | 1,00 |
| 80 | 97 | 1839 | 6088 | 4,00 | 75 | 12/12/12 | 3,4 | 3,4 | 3,5 | — | 10,0 | 100 % | 3320 | 1,00 |
| 90 | 99 | 1849 | 6674 | 4,00 | 75 | 12/12/12 | 3,6 | 3,7 | 3,7 | — | 11,0 | 100 % | 3255 | 1,00 |
| 100 | 102 | 1863 | 7463 | 4,00 | 75 | 12/12/12 | 4,4 | 4,5 | 4,5 | — | 14,2 | 100 % | 2888 | 1,00 |

#### Optimisé

| Palier | Niv. | ATQ | PV | VIT | Crit | Camp (A/S/V) | TTK facile | TTK moyen | TTK coriace | TTK élite | TTK gardien | Victoire gardien | PV perdus gardien | Rations |
|---:|---:|---:|---:|---:|---:|---|---:|---:|---:|---:|---:|---:|---:|---:|
| 10 | 29 | 219 | 1066 | 1,41 | 47 | 2/2/0 | 1,1 | 1,2 | 1,4 | 3,1 | 8,3 | 95 % | 909 | 1,00 |
| 20 | 50 | 464 | 2047 | 2,00 | 75 | 3/4/0 | 1,7 | 1,8 | 1,9 | 3,9 | 7,2 | 72 % | 1483 | 1,00 |
| 30 | 62 | 723 | 2933 | 2,60 | 75 | 12/8/2 | 2,7 | 2,9 | 3,0 | — | 9,7 | 13 % | 2899 | 1,00 |
| 40 | 72 | 784 | 3620 | 3,04 | 75 | 24/25/17 | 4,5 | 4,6 | 4,7 | — | 10,2 | 99 % | 2154 | 1,00 |
| 50 | 78 | 1120 | 5195 | 4,00 | 75 | 25/25/23 | 2,6 | 2,7 | 2,7 | 5,1 | 7,7 | 100 % | 2522 | 0,17 |
| 60 | 82 | 1139 | 5549 | 4,00 | 75 | 25/25/25 | 2,8 | 2,9 | 2,9 | 5,2 | 8,7 | 100 % | 1963 | 0,56 |
| 70 | 86 | 1298 | 5734 | 4,00 | 75 | 25/25/25 | 2,6 | 2,7 | 2,7 | 5,0 | 9,5 | 100 % | 2276 | 0,89 |
| 80 | 91 | 1182 | 5687 | 4,00 | 75 | 25/25/25 | 3,6 | 3,7 | 3,7 | — | 10,3 | 100 % | 2591 | 1,00 |
| 90 | 96 | 1869 | 6235 | 4,00 | 75 | 25/25/25 | 2,4 | 2,4 | 2,5 | 4,2 | 6,4 | 100 % | 2857 | 0,14 |
| 100 | 99 | 1885 | 6326 | 4,00 | 75 | 25/25/25 | 2,7 | 2,7 | 2,7 | — | 7,1 | 100 % | 1899 | 0,32 |

#### Hyper-optimisé

| Palier | Niv. | ATQ | PV | VIT | Crit | Camp (A/S/V) | TTK facile | TTK moyen | TTK coriace | TTK élite | TTK gardien | Victoire gardien | PV perdus gardien | Rations |
|---:|---:|---:|---:|---:|---:|---|---:|---:|---:|---:|---:|---:|---:|---:|
| 10 | 30 | 243 | 1082 | 1,39 | 73 | 4/5/0 | 1,0 | 1,1 | 1,2 | 2,9 | 8,8 | 52 % | 1020 | 1,00 |
| 20 | 46 | 440 | 1642 | 1,88 | 75 | 6/5/1 | 1,7 | 1,7 | 1,8 | 3,7 | 6,3 | 52 % | 1411 | 1,00 |
| 30 | 63 | 886 | 2818 | 2,48 | 75 | 7/6/2 | 2,2 | 2,3 | 2,4 | 4,3 | 7,1 | 68 % | 2388 | 0,30 |
| 40 | 71 | 1037 | 5942 | 4,00 | 75 | 9/9/4 | 2,9 | 3,0 | 3,1 | 5,7 | 7,4 | 100 % | 2126 | 0,00 |
| 50 | 76 | 1169 | 6296 | 4,00 | 75 | 26/28/18 | 2,9 | 3,0 | 3,1 | 5,8 | 10,4 | 100 % | 2861 | 0,30 |
| 60 | 81 | 1210 | 6822 | 4,00 | 75 | 30/30/25 | 3,2 | 3,2 | 3,2 | 6,0 | 12,1 | 100 % | 2509 | 0,61 |
| 70 | 86 | 1631 | 6788 | 4,00 | 75 | 30/30/28 | 2,1 | 2,1 | 2,1 | 4,0 | 7,5 | 100 % | 3323 | 0,56 |
| 80 | 90 | 1431 | 8630 | 4,00 | 75 | 30/30/30 | 2,7 | 2,7 | 2,8 | 5,2 | 7,7 | 100 % | 4217 | 0,00 |
| 90 | 94 | 2117 | 9665 | 4,00 | 75 | 30/30/30 | 2,0 | 2,0 | 2,1 | 3,8 | 5,4 | 100 % | 1216 | 0,00 |
| 100 | 97 | 2134 | 9765 | 4,00 | 75 | 30/30/30 | 2,3 | 2,4 | 2,4 | 4,4 | 6,2 | 100 % | 1148 | 0,00 |

## 5. TTK gardien selon le rythme de marche

**Passif assisté**

| Palier | 5 000 pas/j | 8 000 pas/j | 15 000 pas/j |
|---:|---:|---:|---:|
| 10 | 7,3 | 10,4 | 11,4 |
| 20 | 9,2 | 8,1 | 7,1 |
| 30 | 9,7 | 11,1 | 14,3 |
| 40 | 10,3 | 14,5 | 19,1 |
| 50 | 13,1 | 14,6 | 13,6 |
| 60 | 16,7 | 19,7 | 12,2 |
| 70 | 11,6 | 19,8 | 12,6 |
| 80 | 13,7 | 9,9 | 14,4 |
| 90 | 10,9 | 10,4 | 14,2 |
| 100 | 11,8 | 8,4 | 15,2 |

**Normal**

| Palier | 5 000 pas/j | 8 000 pas/j | 15 000 pas/j |
|---:|---:|---:|---:|
| 10 | 15,3 | 11,2 | 15,5 |
| 20 | 9,5 | 10,0 | 6,3 |
| 30 | 13,2 | 17,8 | — |
| 40 | 14,9 | 25,3 | 13,8 |
| 50 | 19,5 | 16,5 | 15,0 |
| 60 | 24,2 | 14,9 | 13,5 |
| 70 | 21,5 | 14,3 | 13,9 |
| 80 | 17,0 | 10,0 | 19,1 |
| 90 | 16,0 | 11,0 | 21,4 |
| 100 | 19,0 | 14,2 | 19,4 |

**Optimisé**

| Palier | 5 000 pas/j | 8 000 pas/j | 15 000 pas/j |
|---:|---:|---:|---:|
| 10 | 11,3 | 8,3 | 8,4 |
| 20 | 6,8 | 7,2 | 7,6 |
| 30 | 14,1 | 9,7 | 8,3 |
| 40 | 10,9 | 10,2 | 8,6 |
| 50 | 8,6 | 7,7 | 7,7 |
| 60 | 8,8 | 8,7 | 8,6 |
| 70 | 8,5 | 9,5 | 9,5 |
| 80 | 9,2 | 10,3 | 7,5 |
| 90 | 8,2 | 6,4 | 7,1 |
| 100 | 7,1 | 7,1 | 7,4 |

**Hyper-optimisé**

| Palier | 5 000 pas/j | 8 000 pas/j | 15 000 pas/j |
|---:|---:|---:|---:|
| 10 | 7,3 | 8,8 | 8,2 |
| 20 | 4,9 | 6,3 | 6,1 |
| 30 | 12,0 | 7,1 | 9,6 |
| 40 | 12,1 | 7,4 | 10,1 |
| 50 | 12,5 | 10,4 | 10,8 |
| 60 | 5,4 | 12,1 | 12,4 |
| 70 | 6,0 | 7,5 | 8,6 |
| 80 | 6,4 | 7,7 | 6,2 |
| 90 | 6,6 | 5,4 | 5,9 |
| 100 | 4,5 | 6,2 | 6,8 |

Le rythme plus rapide ne rend pas le jeu plus facile : il réduit les jours, mais augmente les replis et les retours en arrière, car le stuff n’a pas le temps de suivre.

## 6. Analyse par profil

### Passif minimal — mur au palier 21

Le passif atteint le palier 21 vers le niveau 60–80, puis ne gagne plus. Il a environ **3 800–4 200 replis**, dont la majorité sont des défaites contre des monstres normaux ou des élites, et **11 000–12 500 combats de farm de récupération**. Son niveau monte jusqu’à 100+, mais ses PV/ATQ de base ne compensent pas le stuff manquant. Le stuff rare du palier 21 ne suffit pas : sans fusion, forge, camp ni coffres, le héros ne peut pas passer le contrôle de stuff du palier 21. **L’XP seule ne permet donc pas de progresser**, contrairement à l’objectif annoncé. Un test avec une early curve adoucie (×2,0 PV / ×1,8 ATQ, fin au palier 20) déplace le mur du palier 21 au palier 24 : ce n’est pas suffisant seul.

### Passif assisté — la version jouable de l’idle

En ajoutant les automatismes qu’un joueur d’idle attend (équiper le meilleur, fusionner les doublons, forger avec réserve, acheter le camp, ouvrir les coffres, se soigner), le passif termine **5/5 à 5 000 et 8 000 pas/j, 4/5 à 15 000**. Les jours médians sont **46,2 / 41,7 / 29,0**, avec **579 / 1 321 / 2 049 replis**. C’est la preuve que le problème n’est pas l’XP mais l’accessibilité de l’optimisation. Même assisté, le passif reste très sensible au rythme : à 15 000 pas/j, il outrepasse son stuff et échoue davantage.

### Normal — finissable mais très irrégulier

Le normal termine en **46,3 jours à 5 000 pas/j**, **31,3 jours à 8 000** et **24,3 jours à 15 000**. Il garde un niveau final de 98–107. Les replis montent de **358** (5 000) à **702** (8 000) puis **1 234** (15 000) : plus le joueur marche, plus il avance vite, moins il farme, et plus il échoue. Les pics sont les gardiens 12, 20–21, 26, 30, puis 50–70, et les élites des paliers 40–70 (0–50 % de victoires dans les tableaux). Le joueur ressent une alternance de « je passe facilement » et de « je dois farmer 20 minutes ».

### Optimisé — la récompense de l’optimisation

L’optimisé termine en **38,0 jours à 5 000**, **28,5 à 8 000** et **18,1 à 15 000**, avec 215–609 replis. Après le palier 40, les gardiens sont à 100 % et 7–14 s ; le jeu devient une formalité, sauf les élites. Le stuff, les sets et la forge sont clairement récompensés, mais la difficulté est concentrée à l’early/mid game.

### Hyper-optimisé — facile, mais finale trop courte

L’hyper termine en **40,1 jours à 5 000**, **25,8 à 8 000** et **13,4 à 15 000**, avec seulement **103–114 replis**. Il farme 3 gardiens par jalon, ce qui explique qu’il ne soit pas plus rapide que l’optimisé malgré sa puissance. Les gardiens 10–30 restent à 28–68 % de victoires (stuff encore en retard), puis 100 % après 40. Le **gardien final tombe en 4,5–6,8 s** : le combat le plus important du jeu n’est pas mémorable.

## 7. Problèmes majeurs

1. **Le passif ne peut pas finir.** Le contrôle de stuff du palier 21 est infranchissable sans fusion/forge/camp/coffres. L’XP ne compense pas.
2. **Le rythme de marche punit le joueur.** À 15 000 pas/j, le normal a 3,4× plus de replis qu’à 5 000. Le stuff ne suit pas la progression des paliers.
3. **Les élites sont trop létales.** À 40–70, un build normal a souvent 0–50 % de victoire contre une élite, ce qui crée des morts en série et des boucles de récupération.
4. **Les gardiens early/mid sont des murs de stuff.** 12, 20–21, 26, 30, puis 50–70 demandent 10–60 tentatives. La pitié actuelle (−1 %/défaite, plafond −3 %) ne change rien.
5. **Le endgame est trop facile pour l’hyper.** Le stuff légendaire forgé + camp 30 + sets donne un TTK gardien de 4–8 s après le palier 50. Il manque un vrai climax.
6. **L’économie n’est pas une contrainte.** 2,6–3,6 M d’or et 1 300–2 100 rations utilisées ; les ressources sont abondantes, donc les choix de camp/consommables pèsent peu.

## 8. Améliorations proposées

### P0 — Rendre le passif finissable sans le forcer à optimiser

- Ajouter une **optimisation automatique** activée par défaut : équiper le meilleur objet, fusionner les doublons, forger avec une réserve d’or/éclats, acheter le camp. Un bouton « Auto » à côté de « Équiper le meilleur ».
- Test simulé : passif + auto-optimisation + coffres + soins = **5/5 finis, 46,2 jours à 5 000 pas/j, 579 replis**, contre 0/5 pour le passif minimal. C’est la preuve que le problème est l’accessibilité de l’optimisation, pas l’XP.
- Alternative/complément : **buff de ténacité** après chaque défaite (+5 % ATK/PV au prochain combat du palier, cumulable +25 %, reset à la victoire). Cela garantit qu’un joueur passif finit par passer sans rendre le jeu trivial pour les autres.

### P0 — Faire suivre le stuff au rythme du joueur

- Faire évoluer le **niveau des objets avec le niveau du héros**, pas seulement avec le palier : `itemLevel = max(palier, niveau - 10)`. Un joueur qui marche beaucoup et gagne des niveaux obtient un stuff utilisable, au lieu de farmer un stuff de palier déjà dépassé.
- Ou ajouter un **catch-up de stuff** : quand la puissance du héros est sous la puissance attendue du palier, augmenter la rareté/le niveau des drops pendant quelques combats. Ne pas augmenter la difficulté des joueurs sur-stuffés : cela punirait l’optimisation.

### P1 — Adoucir les élites et l’early curve

- Élites : PV ×1,45 au lieu de ×1,80, ATK ×1,05 au lieu de ×1,15, ou niveau +1 au lieu de +2. Test simulé : les replis du passif baissent de 4 177 à 3 136 avec élites ×1,40/×1,05.
- Early curve : garder ×3,0 sur les paliers 6–8, puis redescendre à ×1 dès le palier 18–20 (au lieu de 25). Le pic ×3 est utile là où les rapports l’ont montré, mais il crée un mur à 20–26.

### P1 — Anti-mur de gardien

- Pitié : passer de **−1 %/défaite, plafond −3 %** à **−3 %/défaite, plafond −15 %**, ou ajouter « après 5 défaites, le gardien perd 10 % PV/ATQ ».
- Afficher la progression de la pitié au portail ; le joueur doit voir que ses échecs le rapprochent de la victoire.

### P2 — Donner un vrai final

- Gardien final : PV ×1,8–2,0 (au lieu de ×1,40) et ATK ×1,25–1,30, avec une phase 2 unique. Objectif : 10–15 s pour l’hyper, 15–20 s pour l’optimisé, sans rescaling global des autres gardiens.
- Éviter un gear-score scaling global : le prototype a augmenté les jours et les replis de l’hyper (52,6 jours / 272 replis contre 40,1 / 103), ce qui punit l’optimisation. Réserver le challenge scaling au gardien final.

### P2 — Économie

- Ajouter des sinks d’or : reroll du modificateur de gardien, potions au camp, respec des améliorations.
- Rendre les rations un peu plus rares ou les soins plus coûteux pour que « quelques rations » soit une vraie décision ; actuellement 25–30 rations/jour couvrent largement les besoins.

## 9. Limites de la simulation

- Les combats de monstres sont analytiques (moyennes) ; les gardiens sont simulés 1 fois par tentative, avec 5 graines par couple profil × pas/jour. Les chiffres sont directionnels, pas une prédiction au jour près.
- Les choix de stuff du joueur réel (sets préférés, effets, objets verrouillés) sont simplifiés ; l’hyper utilise un oracle, le normal un glouton.
- Les événements spéciaux, l’identification, les consommables de soin et certaines interactions UI sont simplifiés ou agrégés.
- Le « passif minimal » est volontairement extrême : il n’ouvre pas les coffres et ne touche jamais à la forge. Le « passif assisté » représente mieux un joueur d’idle qui accepte les automatismes.

## 10. Conclusion — ressenti attendu

- **Passif** : aujourd’hui, frustration et sentiment d’injustice au palier 21. Avec l’auto-optimisation, il termine en ~6–7 semaines à 5 000 pas/j, mais avec encore trop de replis. C’est la priorité.
- **Normal** : le jeu est challengeant et lisible, mais les pics de gardiens et les élites transforment parfois le plaisir en corvée. Objectif : diviser les replis par 3–5 et lisser les paliers 12–30 et 50–70.
- **Optimisé** : c’est le profil le plus gratifiant : les décisions de stuff, sets et forge paient. Le milieu de partie est réussi ; le endgame manque de tension.
- **Hyper** : le jeu est effectivement relativement facile, avec un minimum de challenge jusqu’au palier 40, puis il devient trop court. Le gardien final doit être le sommet, pas une formalité de 4 s.

## 11. Reproductibilité

```bash
python tools/equilibrage/progression_sim.py --seeds 5 \
  --profiles passif,normal,optimise,hyper \
  --steps 5000,8000,15000 \
  --out progression_023_full.json

python tools/equilibrage/progression_sim.py --seeds 5 \
  --profiles passif_plus --steps 5000,8000,15000 \
  --out prog_passif_plus_full.json

python tools/equilibrage/report_progression.py \
  --main progression_023_full.json --assisted prog_passif_plus_full.json \
  --out docs/analyse-progression-023.md
```

> Les chiffres sont des médianes sur 5 graines. Le simulateur ne remplace pas un playtest : il sert à identifier les murs et les tendances, pas à prédire un joueur réel au jour près.
