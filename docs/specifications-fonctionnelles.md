# LA MARCHE DES ÂMES

**Spécifications fonctionnelles — Idle Step RPG mobile en pixel art**

| | |
|---|---|
| Version | 1.0 — Spécifications de conception |
| Statut | Prêt pour prototypage et playtest |
| Auteur | Game Design |
| Plateforme cible | Mobile, portrait, usage à une main |
| Genre | Idle RPG / step RPG / fantasy sombre |
| Durée de référence d'une session | 20 s à 3 min, plusieurs fois par jour |
| Device de référence | 390 × 844 (portrait), adaptatif de 360 × 640 à 430 × 932 |

## Sommaire

1. Résumé exécutif
2. Piliers de design et intentions
3. Public, contexte et sessions
4. Boucle de jeu
5. Vocabulaire du jeu
6. Conversion des pas et progression
7. Structure du donjon
8. Combat automatique
9. La ferveur
10. Ressources et économie
11. Défaite et boucle de farm
12. Le campement — le Feu de camp
13. Inventaire et équipement
14. Progression, difficulté et jalons
15. Interface mobile
16. Onboarding
17. Cas limites et règles de robustesse
18. Équilibrage — repères de tuning
19. Indicateurs et critères d'acceptation
20. Hors périmètre et évolutions
21. Annexes

---

## 1. Résumé exécutif

**La Marche des Âmes** transforme les pas de la vie réelle en expédition dans un donjon infini. Le héros avance seul, de gauche à droite, dans des galeries de pixel art. Tous les dix mètres, il affronte un monstre. Tous les cent mètres, un gardien scelle le palier et impose un combat décisif.

Le joueur ne pilote ni la marche ni les combats : il **marche dans la vraie vie**, puis synchronise ses pas. Il intervient sur la **préparation** (campement, améliorations, équipement), sur les **choix d'opportunité** (forcer un coffre au prix de sa progression) et sur le **rythme** (synchroniser au bon moment, se replier au feu de camp).

La promesse :

> **« Chaque pas compte. Même quand vous ne jouez pas. »**

Trois piliers :

1. **La marche est la seule monnaie de temps.** Pas de fausse énergie à attendre : la progression est le reflet direct de l'activité réelle.
2. **Aucun mur définitif.** La défaite fait reculer de 20 mètres dans une zone de farm plus faible, où le héros continue de gagner de l'or et de l'expérience. On ne bloque jamais le joueur, on le fait rebondir.
3. **Une respiration chaleureuse dans un monde sombre.** Le campement au feu de camp est un havre de gestion, de soin et de personnalisation, accessible à tout moment.

---

## 2. Piliers de design et intentions

### 2.1 Piliers

| Pilier | Intention | Traduction concrète |
|---|---|---|
| Marche = progression | Le joueur sent que ses pas réels ont une valeur tangible | 10 pas = 1 m ; 1 000 pas = 1 palier ; compteur de pas toujours visible |
| Idle sans impasse | Ne jamais punir l'absence ni la faiblesse | Repli automatique, farm continu, pas de perte d'objet, pas de pénalité d'or |
| Fantasy lisible | Un RPG d'ambiance, pas un tableur | Scène de profil en pixel art, sprites variés, ambiance feu de camp, journal narratif |
| Préparation > exécution | Le joueur décide avant, pas pendant | Combats automatiques, build via équipement et améliorations |
| Récompense fréquente | Chaque session apporte quelque chose | Butin à chaque monstre, palier tous les 1 000 pas, jalon chaque jour |

### 2.2 Boucle émotionnelle visée

1. **Curiosité** : que cache le prochain palier ?
2. **Tension** : le gardien va-t-il tomber cette fois ?
3. **Soulagement** : le repli n'est pas une fin, c'est un farm.
4. **Fierté** : le build progresse, le feu de camp brille plus fort.
5. **Anticipation** : « Je reprendrai après ma prochaine marche. »

### 2.3 Ce que le jeu n'est pas

- Pas un jeu de marche compétitif.
- Pas un jeu de combat demandant des actions en temps réel.
- Pas un jeu de gestion de ville : le campement reste un écran de pause compact.
- Pas un jeu punitif : la perte de distance est un **déplacement**, pas une confiscation de pas.

---

## 3. Public, contexte et sessions

### 3.1 Public

- 16–45 ans, joueurs mobiles occasionnels, amateurs de fantasy et de pixel art.
- Joueurs de jeux de marche ou de fitness ludique cherchant une carotte narrative.
- Joueurs d'idle games cherchant une raison de revenir sans notifications agressives.

### 3.2 Contexte d'usage

- Téléphone en poche pendant la marche, le travail, les courses.
- Session courte : ouverture, synchronisation, lecture du journal, achat d'une amélioration, fermeture.
- Session longue : gestion du build au campement, comparaison d'équipement, observation de quelques combats.
- Le jeu doit rester **lisible en extérieur** (contrastes élevés) et **utilisable à une main**.

### 3.3 Rythme de référence

| Profil | Pas/jour | Distance/jour | Paliers/jour | Sessions/jour |
|---|---:|---:|---:|---:|
| Sédentaire actif | 5 000 | 500 m | 5 | 2–4 |
| Actif moyen | 6 500 | 650 m | 6–7 | 3–5 |
| Marcheur | 8 000 | 800 m | 8 | 4–6 |
| Très actif | 12 000+ | 1 200 m+ | 12+ | 5–8 |

> Le jeu est calibré pour qu'une journée normale (5 000–8 000 pas) traverse **5 à 8 paliers**, soit 5 à 8 gardiens et environ 50 à 80 rencontres.

---

## 4. Boucle de jeu

### 4.1 Boucle micro — le pas

```
Pas réel
  └─> conversion en distance (10 pas = 1 m)
        └─> avancée automatique du héros
              ├─> tous les 10 m : rencontre automatique
              │     ├─> victoire : or + expérience + butin éventuel
              │     └─> défaite : repli de 20 m, farm en zone sûre
              ├─> tous les 25–45 m : coffre éventuel
              └─> tous les 100 m : gardien de palier
```

### 4.2 Boucle méta — la journée

```
Marcher
  └─> synchroniser
        ├─> distance parcourue
        ├─> ferveur (bonus de combat)
        ├─> rations et éclats d'âme
        └─> or et expérience
              └─> campement : améliorations + équipement
                    └─> gardien suivant plus abordable
```

### 4.3 Boucle de rétention — la semaine

- **Chaque jour** : reset à 00:00, nouveaux serments, nouvelle ferveur.
- **Chaque palier** : nouveau gardien, nouveau butin, nouveau décor.
- **Chaque 10 paliers** : jalon permanent (bonus d'or, apparence du feu de camp, titre).
- **Chaque semaine** : bilan de marche, meilleur palier, équipement légendaire potentiel.

---

## 5. Vocabulaire du jeu

| Terme | Définition |
|---|---|
| **Pas** | Unité de base. Pas valides du jour, comptés depuis 00:00 heure locale. |
| **Distance** | Conversion des pas en mètres. 10 pas = 1 m. |
| **Palier** | Un étage de donjon, long de 100 m (1 000 pas). |
| **Segment** | Tranche de 10 m entre deux rencontres. |
| **Halte** | Zone sûre de 10 m, matérialisée par une borne. |
| **Gardien** | Boss de fin de palier, rencontré à 100 m. |
| **Ferveur** | Jauge d'élan quotidien alimentée par les pas. Accélère attaque et régénération. |
| **Ration** | Consommable de soin généré par une fraction des pas. |
| **Éclat d'âme** | Ressource rare générée par les pas, soin complet et forge. |
| **Repli** | Recul automatique de 20 m après une défaite. |
| **Farm de halte** | Combat continu contre des ennemis affaiblis dans la zone de repli. |
| **Coffre à serrure de pas** | Coffre dont l'ouverture exige de consacrer un quota de pas. |
| **Feu de camp** | Écran de pause et de gestion, accessible à tout moment. |
| **Serment** | Objectif journalier optionnel. |

---

## 6. Conversion des pas et progression

### 6.1 Règle de conversion

- **10 pas = 1 mètre.** Échelle fixe, non modifiable par le joueur.
- **100 pas = 10 mètres** = un segment.
- **1 000 pas = 100 mètres** = un palier complet, gardien inclus.
- Les pas sont convertis **à l'unité** ; les fractions de mètre sont conservées et cumulées.
- Un pas ne peut jamais être compté deux fois : chaque synchronisation n'apporte que les pas **nouveaux**.

### 6.2 Table de conversion

| Pas | Distance | Événement |
|---:|---:|---|
| 1 | 0,1 m | avancée |
| 10 | 1 m | avancée |
| 100 | 10 m | rencontre de monstre |
| 250 | 25 m | premier coffre possible |
| 500 | 50 m | milieu de palier |
| 1 000 | 100 m | gardien de palier |
| 5 000 | 500 m | 5 paliers |
| 8 000 | 800 m | 8 paliers |

### 6.3 Cumul et réserve de marche

- Les pas sont comptabilisés en continu, même quand l'écran du jeu est fermé, dans la limite des règles de plausibilité (§17).
- À la synchronisation, les pas sont appliqués à la **réserve de marche**, puis consommés par l'avancée.
- Si le héros est immobilisé (combat, coffre en cours de forçage, campement), la réserve **attend** et s'applique dès la reprise.
- La réserve n'est jamais perdue. Elle peut être consultée à tout moment (« pas en attente »).
- **Plafond par synchronisation : 100 000 pas.** Au-delà, l'excédent est ignoré et un message pédagogique explique la limite. Ce plafond couvre un marathon (environ 45 000 pas) et plusieurs jours de rattrapage.
- **Plafond journalier : 60 000 pas.** Au-delà, les pas restent dans le compteur de santé du téléphone mais ne nourrissent plus le jeu.

### 6.4 Simulation d'une journée

Hypothèses : héros de niveau 4, équipement rare partiel, aucun coffre forcé.

| Pas/jour | Distance | Paliers | Monstres | Gardiens | Coffres estimés | Or estimé | XP estimée |
|---:|---:|---:|---:|---:|---:|---:|---:|
| 5 000 | 500 m | 5 | 50 | 5 | 8 | ~2 200 | ~3 000 |
| 6 500 | 650 m | 6,5 | 65 | 6 | 10 | ~2 900 | ~3 900 |
| 8 000 | 800 m | 8 | 80 | 8 | 13 | ~3 600 | ~4 800 |

> Ces valeurs servent de cible d'équilibrage, pas de garantie. Les coffres et le farm de halte ajoutent une variance volontaire.

---

## 7. Structure du donjon

### 7.1 Avancée automatique

- Le héros se déplace **de gauche à droite**, en vue de profil, sans intervention.
- La caméra suit le héros ; le décor défile en parallaxe.
- La vitesse visuelle est constante et décorative : elle ne dépend jamais de la vitesse réelle de marche, seulement des pas convertis.
- Le héros s'arrête net à chaque rencontre, à chaque coffre forcé et à chaque gardien.
- Aucune commande de déplacement manuel n'existe.

### 7.2 Rencontres

- **Tous les 10 m**, une rencontre de monstre est obligatoire.
- Une rencontre prend la forme d'un combat automatique de 3 à 12 secondes (hors gardien).
- Le monstre est choisi dans un pool adapté au palier et à la position dans le palier :
  - Du mètre 0 à 29 : monstres de niveau `palier − 1`.
  - Du mètre 30 à 69 : monstres de niveau `palier`.
  - Du mètre 70 à 99 : monstres de niveau `palier + 1`.
- **10 % de chances** qu'un monstre soit **élite** : niveau +2, points de vie ×2,2, or ×1,6, butin garanti inhabituel ou mieux.
- Chaque monstre possède un nom, un sprite distinct et une courte description dans le journal.

### 7.3 Haltes et zones sûres

- Après chaque rencontre vaincue, le tronçon parcouru devient une **halte**.
- Les haltes sont matérialisées par une borne de pierre tous les 10 m.
- Une halte est **sans danger** : aucune rencontre ne s'y déclenche.
- Le repli après défaite place toujours le héros sur une halte, à 20 m en arrière.
- La halte est le point de reprise après fermeture du jeu.

### 7.4 Coffres

- Un coffre peut apparaître **après un monstre vaincu**, avec 20 % de chances, à 4 m devant le héros.
- Deux coffres non ouverts au maximum peuvent être en attente.
- Le coffre n'est pas obligatoire : le héros le dépasse si le joueur ne choisit pas de le forcer.
- Un coffre dépasse reste accessible pendant 24 h, puis disparaît (le journal l'indique).

### 7.5 Rencontres spéciales (saveur)

| Rencontre | Fréquence | Effet |
|---|---:|---|
| Autel ancien | 6 % des paliers | Choix gratuit : +10 % ATQ pour le palier ou +15 % PV pour le palier |
| Marchand errant | 5 % des paliers | Échange 100 or contre 1 ration ou 1 éclat d'âme |
| Statue du héros | 3 % des paliers | Petit texte narratif, +20 or |
| Source souterraine | 4 % des paliers | Soin gratuit de 25 % des PV, une fois |

> Ces rencontres ne bloquent jamais l'avancée et ne demandent aucune action obligatoire.

### 7.6 Le gardien de palier

- À **100 m**, une **porte runique** barre le passage.
- Le combat est **obligatoire** : impossible de contourner, impossible de changer de palier sans vaincre.
- Le gardien est annoncé 10 m avant par un **présage** (sol qui tremble, corne lointaine, bannière « GARDIEN »).
- Le gardien possède :
  - 6 × les PV d'un monstre de niveau `palier + 2` ;
  - 1,15 × son attaque ;
  - une cadence de 0,8 attaque/s ;
  - une **phase 2** à 50 % des PV : cadence +30 %, aura rouge, attaque spéciale toutes les 8 s (180 % de dégâts, télégraphiée 2 s).
- Vaincre le gardien :
  - scelle le palier, ouvre la porte, fait passer au palier suivant (distance remise à 0) ;
  - accorde 12 × l'or d'un monstre normal, 10 × l'XP, un objet **rare ou mieux garanti**, 2 éclats d'âme ;
  - accorde un bonus de première victoire : 1 ration + 1 éclat.
- Échouer :
  - repli de 20 m (à 80 m), farm de halte, nouvelle tentative possible après avoir reparcouru 20 m ;
  - le gardien retrouve tous ses PV à chaque tentative.
- Se reposer au campement pendant un combat de gardien **réinitialise** le gardien (PV pleins) et soigne le héros selon les règles du camp. Aucun avantage gratuit.

### 7.7 Biomes et variété

Le décor change tous les 10 paliers, avec des palettes et des pools de monstres propres :

| Paliers | Biome | Ambiance | Exemples de créatures |
|---|---|---|---|
| 1–10 | Cryptes de Vael | pierre froide, cierges, bannières | gobelins, goules, squelettes |
| 11–20 | Catacombes | os, racines, humidité | gnolls, araignées, spectres |
| 21–30 | Cavernes de cendre | lave, fumée, or rougi | orcs, élémentaires, ogres |
| 31–40 | Forge noyée | métal, vapeur, éclairs | golems, diablotins, dragons |
| 41+ | Cimes du Serment | neige, vent, ruines célestes | géants, wyvernes, gardiens uniques |

---

## 8. Combat automatique

### 8.1 Principes

- Aucune action de combat demandée au joueur.
- Le joueur prépare le combat **avant** : améliorations, équipement, rations.
- Le combat se déroule en temps réel, en arrière-plan visuel de la scène.
- Le joueur peut interrompre le combat en allant au campement (reset du combat en cours, voir §12).
- Aucun combat ne peut durer indéfiniment : au-delà de 60 s, la ferveur s'emballe (+10 % de cadence par tranche de 10 s) et le combat se résout naturellement.

### 8.2 Statistiques

| Statistique | Rôle | Base au niveau 1 |
|---|---|---:|
| **PV** | Points de vie | 240 |
| **ATQ** | Dégâts par coup | 22 |
| **VIT** | Attaques par seconde | 1,00 |
| **Critique** | Chance de coup critique | 10 % |
| **Dégâts critiques** | Multiplicateur | ×1,6 |
| **Régénération** | PV récupérés hors combat | 0,6 % des PV max/s |

Gains par niveau de héros : **+18 PV, +3 ATQ, +1 % VIT**. Les niveaux sont gagnés par l'expérience.

### 8.3 Formule de dégâts

```
Dégâts = ATQ × (0,90 à 1,10) × multiplicateur critique × (1 − mitigation)
```

- Coup critique : ×1,6, chance = critique de base + équipement + ferveur (voir §9).
- Mitigation : réduction plate de 5 % par point de défense, plafonnée à 40 %.
- Les dégâts sont arrondis à l'unité supérieure.
- Aucun coup ne peut infliger moins de 1 dégât.

### 8.4 Cadence et régénération

```
Cadence effective = VIT × (1 + 0,004 × Ferveur) × (1 + bonus d'équipement)
Régénération effective = 0,6 % PV max/s × (1 + 0,008 × Ferveur)
```

- La régénération s'active **4 secondes** après le dernier coup reçu.
- En combat, elle est divisée par 3.
- Au campement, elle est doublée (ambiance de repos).

### 8.5 Déroulement

1. Le monstre apparaît à droite, le héros s'arrête.
2. Les deux combattants attaquent selon leur cadence, sans temps mort.
3. Les nombres de dégâts flottent, les sprites clignotent, la caméra tremble légèrement sur les critiques.
4. À 25 % des PV, **une ration est consommée automatiquement** (une seule par combat) si le héros en possède.
5. À 0 PV d'un côté, le combat se termine.
6. Le butin est attribué, le journal s'écrit, le héros reprend sa marche après 1,2 s.

### 8.6 Montée de niveau

- L'expérience requise pour passer au niveau suivant : `120 × 1,20^(niveau − 1)`.
- À chaque niveau : +18 PV, +3 ATQ, +1 % VIT, soin de 50 % des PV.
- Une montée de niveau interrompt la marche 2 secondes, avec un flash doré et une bannière.
- Les PV excédentaires ne sont pas conservés.

### 8.7 Table de référence — héros

| Niveau | PV | ATQ | VIT | Critique |
|---:|---:|---:|---:|---:|
| 1 | 240 | 22 | 1,00 | 10 % |
| 2 | 258 | 25 | 1,01 | 10 % |
| 3 | 276 | 28 | 1,02 | 10 % |
| 4 | 294 | 31 | 1,03 | 10 % |
| 5 | 312 | 34 | 1,04 | 10 % |
| 6 | 330 | 37 | 1,05 | 10 % |
| 7 | 348 | 40 | 1,06 | 10 % |
| 8 | 366 | 43 | 1,07 | 10 % |
| 9 | 384 | 46 | 1,08 | 10 % |
| 10 | 402 | 49 | 1,09 | 10 % |

> Les valeurs ci-dessus sont **avant** améliorations de camp et équipement. Ces deux sources peuvent doubler la puissance effective.

### 8.8 Table de référence — monstres

PV : `55 + 34 × (niveau − 1)` — ATQ : `6 + 2,2 × (niveau − 1)` — cadence : 0,8/s.

| Niveau | PV | ATQ | DPS brut | Or | XP |
|---:|---:|---:|---:|---:|---:|
| 1 | 55 | 6,0 | 4,8 | 9 | 17 |
| 2 | 89 | 8,2 | 6,6 | 12 | 22 |
| 3 | 123 | 10,4 | 8,3 | 15 | 27 |
| 4 | 157 | 12,6 | 10,1 | 18 | 32 |
| 5 | 191 | 14,8 | 11,8 | 21 | 37 |
| 6 | 225 | 17,0 | 13,6 | 24 | 42 |
| 7 | 259 | 19,2 | 15,4 | 27 | 47 |
| 8 | 293 | 21,4 | 17,1 | 30 | 52 |
| 9 | 327 | 23,6 | 18,9 | 33 | 57 |
| 10 | 361 | 25,8 | 20,6 | 36 | 62 |

### 8.9 Gardiens

Un gardien de palier `P` est de niveau `P + 2` pour ses points de vie ; son attaque suit la référence `P + 1` :

- **PV** = 6 × PV du monstre de niveau `P + 2` ;
- **ATQ** = 1,15 × ATQ du monstre de niveau `P + 1` ;
- **Cadence** = 0,8/s, → 1,04/s en phase 2 ;
- **Butin** : 12 × or normal, 10 × XP normale, 1 objet rare+, 2 éclats.

| Palier | PV gardien | ATQ gardien | DPS phase 1 | DPS phase 2 |
|---:|---:|---:|---:|---:|
| 1 | 738 | 9,4 | 7,5 | 9,8 |
| 2 | 942 | 12,0 | 9,6 | 12,5 |
| 3 | 1 146 | 14,5 | 11,6 | 15,1 |
| 4 | 1 350 | 17,0 | 13,6 | 17,7 |
| 5 | 1 554 | 19,6 | 15,7 | 20,4 |
| 6 | 1 758 | 22,1 | 17,7 | 23,0 |
| 7 | 1 962 | 24,6 | 19,7 | 25,6 |
| 8 | 2 166 | 27,1 | 21,7 | 28,2 |
| 9 | 2 370 | 29,7 | 23,8 | 30,9 |
| 10 | 2 574 | 32,2 | 25,8 | 33,5 |

**Cible d'équilibrage** : un gardien doit tomber en 25–35 secondes avec un build à jour, en laissant le héros entre 20 et 40 % de PV. Le premier gardien doit être **gagnable de justesse** ; le deuxième doit souvent demander un repli et une amélioration.

---

## 9. La ferveur

### 9.1 Source

- La ferveur est une jauge de **0 à 100**, alimentée par les **pas du jour**.
- **1 point de ferveur par 50 pas.** 5 000 pas remplissent la jauge.
- Les pas au-delà de 100 points de ferveur ne sont pas perdus : ils continuent d'alimenter rations, éclats et progression.
- La ferveur est **quotidienne** : elle repart de 0 à 00:00 heure locale.

### 9.2 Effets

| Effet | Valeur |
|---|---|
| Vitesse d'attaque | +0,4 % par point, soit **+40 % à 100** |
| Régénération | +0,8 % par point, soit **+80 % à 100** |
| Critique | +0,02 % par point, soit **+2 % à 100** |
| Soin au campement | +0,2 % par point, soit **+20 % à 100** |

> La ferveur ne modifie **ni les PV ni l'ATQ** : elle accélère et régénère, elle ne rend pas plus fort au coup par coup. Cela préserve la valeur du stuff et des améliorations.

### 9.3 Paliers visuels

| Ferveur | Nom | Ambiance |
|---:|---|---|
| 0–24 | Tison | braise discrète, aura absente |
| 25–49 | Braise | lueur orange légère |
| 50–74 | Fournaise | flammes dans le sillage du héros |
| 75–99 | Tempête | aura complète, braises montantes |
| 100 | Âme ardente | aura dorée, traînée d'étincelles, cadre de scène animé |

### 9.4 Décroissance

- La ferveur ne baisse pas tant que le joueur marche régulièrement.
- Après **120 minutes sans nouveau pas**, elle perd **2 points par heure**, jusqu'à un plancher de 0.
- La décroissance s'arrête dès qu'un nouveau pas est synchronisé.
- La décroissance est **suspendue** pendant les combats de gardien et au campement.

### 9.5 Affichage

- Jauge horizontale en bas de la scène, avec icône de flamme et valeur en pourcentage.
- Le palier de ferveur est écrit à côté (« Fournaise »).
- Au passage d'un palier, une brève animation de flamme et un texte flottant (« FOURNAISE »).
- L'effet chiffré est visible dans l'onglet Stats : « +24,8 % vitesse · +49,6 % régénération ».

---

## 10. Ressources et économie

### 10.1 Ressources

| Ressource | Source | Usage | Plafond |
|---|---|---|---|
| **Pas du jour** | Marche réelle | Distance, ferveur, quotas de coffre | 60 000/jour |
| **Or** | Monstres, gardiens, coffres, farm | Améliorations, forge, achat au marchand | Aucun |
| **Rations** | 1 par 200 pas + gardiens + coffres | Soin au camp, soin automatique en combat | 24/jour |
| **Éclats d'âme** | 1 par 500 pas + gardiens + coffres | Soin complet, forge, reroll | 12/jour |
| **Expérience** | Monstres, gardiens, serments | Niveaux du héros | Aucun |

### 10.2 Règle de génération

- **Ration** : `min(24, pas du jour / 200)` — la 1re ration arrive à 200 pas, la 24e à 4 800 pas.
- **Éclat d'âme** : `min(12, pas du jour / 500)` — le 1er à 500 pas, le 12e à 6 000 pas.
- Les générations sont **rétroactives** et visibles dans un compteur « généré par vos pas ».
- Les consommations ne font pas baisser la génération future : le jeu suit un **solde** (généré − consommé).
- Un gardien vaincu ajoute **2 éclats** et **1 ration** (hors plafond journalier).
- Les pas qui dépassent 100 de ferveur ne génèrent pas de ressource supplémentaire.

### 10.3 Sources et puits d'or

| Source | Or approximatif |
|---|---:|
| Monstre normal | 9 à 36 selon niveau |
| Monstre élite | ×1,6 |
| Gardien | 12 × monstre de niveau +2 |
| Coffre de bois | 40–80 |
| Coffre de fer | 130–220 |
| Coffre runique | 380–550 |
| Farm de halte | 50 % puis dégressif (voir §11) |

| Puits | Coût |
|---|---:|
| Amélioration de camp | 40 → plusieurs milliers |
| Forge d'équipement | 120 + 40 × niveau d'objet |
| Achat au marchand | 100 or = 1 ration ou 1 éclat |
| Réparation d'objet maudit | 200 or |

### 10.4 Rations

- **Soin au campement** : 1 ration = 40 % des PV max (bonus de ferveur inclus).
- **Soin automatique en combat** : à 25 % des PV, 1 ration consommée, +35 % des PV max, une fois par combat.
- **Soin d'urgence** : si aucune ration et PV < 10 %, le héros quitte le combat et se replie immédiatement de 20 m (considéré comme une défaite, sans perte).
- Une ration ne peut jamais être consommée à PV pleins.

### 10.5 Éclats d'âme

- **Soin complet au campement** : 1 éclat = 100 % des PV + purge des malus temporaires.
- **Forge** : 1 éclat permet de révéler un objet non identifié ; 3 éclats permettent de reroll un affixe ; 5 éclats permettent d'élever un objet d'une rareté (dans la limite d'un objet légendaire par slot).
- **Reroll de gardien** : 2 éclats permettent de relancer le pool du prochain gardien avant de l'affronter (une fois par palier).
- Les éclats sont **rares** : leur usage doit être un choix, jamais un réflexe.

### 10.6 Expérience

- Monstre : `12 + 5 × niveau`.
- Élite : ×1,8.
- Gardien : 10 × l'XP d'un monstre de niveau `palier + 2`.
- Serment accompli : 100 à 300 XP.
- Courbe : `120 × 1,20^(niveau − 1)`.

---

## 11. Défaite et boucle de farm

### 11.1 Déclenchement

La défaite survient quand les PV du héros atteignent 0, contre un monstre, un gardien ou lors d'un événement.

### 11.2 Conséquences immédiates

- **Aucune perte d'or, d'expérience, d'équipement ni de pas.**
- Le héros est téléporté **20 mètres en arrière**, sur la halte la plus proche.
- Il revient avec **60 % de ses PV max**.
- Il bénéficie de **3 secondes d'invulnérabilité** visuelle et effective.
- Un bandeau s'affiche : « VAINCU — REPLI DE 20 M — LE FEU DE CAMP VOUS ATTEND ».
- Le monstre ou le gardien vainqueur disparaît ; les PV du gardien sont réinitialisés pour la prochaine tentative.

### 11.3 Zone de repli et farm continu

- La zone de repli couvre les **20 derniers mètres** parcourus.
- Le héros y patrouille automatiquement et affronte des **ennemis affaiblis** :
  - niveau = `palier − 2` (minimum 1) ;
  - PV ×0,7 ;
  - ATQ ×0,7 ;
  - un ennemi toutes les **8 secondes**.
- **Récompenses** : 50 % de l'or et de l'XP normale, 30 % de chance de butin commun.
- **Le héros ne peut pas mourir en zone de repli.** À 1 PV, il est soigné de 10 % et l'ennemi est repoussé.
- Chaque ennemi vaincu soigne le héros de **8 % de ses PV max**.
- Le farm s'arrête :
  - après **20 minutes** sans nouveau pas ;
  - si le joueur appuie sur « Repartir » ;
  - si 5 objets de butin sont en attente ;
  - si le joueur va au campement.
- Le rendement du farm est **dégressif** :

| Temps dans la zone de repli | Or et XP |
|---|---:|
| 0–4 min | 50 % |
| 4–8 min | 40 % |
| 8–12 min | 30 % |
| 12–16 min | 20 % |
| 16–20 min | 10 % |
| Au-delà | arrêt du farm |

- Un nouveau pas synchronisé **relance le farm** et remet le compteur à 0.
- Si le joueur ne synchronise plus, l'écran affiche une veilleuse : « Le sentier attend vos pas. » Aucun blocage, aucune punition.

### 11.4 Repartir

- Un bouton « Repartir » apparaît dans le bandeau de repli et dans le journal.
- Il remet le héros en marche vers l'avant, avec les PV actuels.
- La prochaine rencontre est celle du segment suivant, pas celle qui a causé la défaite.
- Le joueur peut aussi rester au campement pour s'équiper avant de repartir.

### 11.5 Anti-frustration

- Si le héros échoue **3 fois de suite** contre le même gardien, le jeu propose un **conseil contextuel** : « Votre attaque est en retard sur son armure. Le feu de camp peut vous aider. »
- Au 3e échec, une **offre de repli long** apparaît : farm accentué 30 minutes, ou amélioration gratuite de 1 niveau (une fois par palier).
- Cette aide ne s'active jamais automatiquement : c'est une proposition.

---

## 12. Le campement — le Feu de camp

### 12.1 Accès

- Accessible **à tout moment** depuis n'importe quel écran, via le bouton feu de camp.
- Il fait office d'**écran de pause** :
  - le temps de jeu est suspendu ;
  - la distance est suspendue ;
  - les combats sont suspendus et **réinitialisés** (le monstre ou le gardien retrouve ses PV) ;
  - les pas réels continuent d'être comptés et mis en réserve ;
  - les pas en réserve sont appliqués à la reprise.
- Le campement n'est **pas** un lieu de progression : on n'y gagne pas d'or, on n'y combat pas.

### 12.2 Ambiance et rôle

- Feu de camp chaleureux, nuit étoilée, tente, héros assis, braises.
- Écran de respiration : le joueur y vient pour **comprendre**, **comparer**, **améliorer**, **soigner**.
- Le journal des dernières aventures y est consultable en entier.
- Le campement affiche un résumé : palier, distance, pas en attente, or, ferveur.

### 12.3 Se soigner

| Action | Coût | Effet |
|---|---|---|
| Se reposer | 1 ration | +40 % PV max (+bonus ferveur) |
| Se ressourcer | 1 éclat d'âme | 100 % PV + purge des malus |
| Veillée gratuite | 0 | +15 % PV, une fois par jour |
| Reprendre l'expédition | 0 | Ferme le camp, applique les pas en attente |

- Le soin est instantané, avec une animation de flammes.
- Si les PV sont pleins, les boutons sont désactivés avec la mention « Déjà au sommet ».
- Une ration/éclat consommé est clairement décompté et journalisé.

### 12.4 Améliorations fondamentales

Trois pistes, achetables avec de l'or, **coût croissant par niveau**.

| Amélioration | Effet par niveau | Coût niveau 1 | Croissance | Niveau max |
|---|---|---:|---:|---:|
| **Lame affûtée** | +3 ATQ | 45 or | ×1,20 par niveau | 50 |
| **Souffle ancien** | +30 PV max | 40 or | ×1,18 par niveau | 50 |
| **Pas vifs** | +2,5 % VIT de base | 70 or | ×1,24 par niveau | 50 |

**Formule de coût** : `coût = coût de base × croissance^(niveau − 1)`, arrondi au multiple de 5 supérieur.

| Niveau | Lame | Souffle | Pas vifs |
|---:|---:|---:|---:|
| 1 | 45 | 40 | 70 |
| 2 | 55 | 50 | 90 |
| 3 | 65 | 60 | 110 |
| 4 | 80 | 70 | 140 |
| 5 | 95 | 85 | 175 |
| 6 | 115 | 100 | 215 |
| 7 | 140 | 120 | 270 |
| 8 | 170 | 140 | 335 |
| 9 | 205 | 165 | 415 |
| 10 | 245 | 195 | 515 |

- Le bouton affiche le coût, l'effet immédiat et l'effet suivant.
- Si l'or est insuffisant, le bouton est désactivé et indique le manque (« 120 or manquants »).
- Un achat déclenche une animation de forge et un son satisfaisant.
- Les améliorations sont **permanentes** et se cumulent avec l'équipement et les niveaux.

### 12.5 Équipement

- Le campement donne accès à l'inventaire complet, à la comparaison et à l'équipement.
- Le joueur peut :
  - équiper un objet ;
  - comparer avant/après avec des flèches vertes/rouges ;
  - vendre un objet ;
  - verrouiller un objet pour éviter une vente accidentelle ;
  - fusionner 3 objets identiques de même rareté pour tenter une rareté supérieure (coût : 1 éclat).
- Tout changement d'équipement recalcule immédiatement les statistiques et la puissance.

### 12.6 Journal et réglages

- Journal complet : combats, butin, paliers, défaites, soins, achats.
- Réglages : son, vibrations, animations réduites, taille de texte, confidentialité, compte.
- Les réglages respectent le mode silencieux du téléphone.

---

## 13. Inventaire et équipement

### 13.1 Emplacements

| Emplacement | Statistique principale | Exemple |
|---|---|---|
| **Arme** | ATQ | épée, hache, lance, masse |
| **Tête** | PV, critique | heaume, capuche |
| **Torse** | PV, VIT | cotte, cuirasse, robe |
| **Mains** | ATQ, critique | gantelets, gants |
| **Bottes** | VIT, régénération | bottes, sandales |
| **Talisman** | régénération, ferveur, or | anneau, amulette |

### 13.2 Raretés

| Rareté | Couleur | Multiplicateur de puissance | Affixes |
|---|---|---:|---:|
| Commun | Gris | 1,00 | 0 |
| Inhabituel | Vert | 1,15 | 1 |
| Rare | Bleu | 1,35 | 2 |
| Épique | Violet | 1,60 | 2 |
| Légendaire | Or | 2,00 | 3 + effet unique |

### 13.3 Génération d'objet

- Niveau d'objet = palier où il tombe (ou palier − 1 pour un coffre de bois).
- La statistique principale dépend de l'emplacement et du niveau :
  - Arme : `6 + 3 × niveau` ATQ ;
  - Tête/Torse/Mains/Bottes : `20 + 10 × niveau` PV ;
  - Talisman : `0,2 + 0,1 × niveau` régénération/s.
- Le multiplicateur de rareté s'applique à la statistique principale.
- Affixes possibles : +ATQ, +PV, +VIT %, +critique %, +or %, +gain de ferveur %, +régénération.
- Un objet **non identifié** affiche « ? » et nécessite 1 éclat ou un parchemin d'identification (trouvable en coffre).
- Les doublons sont possibles ; la fusion permet de les valoriser.

### 13.4 Comparaison

- Au survol/tap d'un objet, une fiche affiche :
  - nom, rareté, niveau, emplacement ;
  - statistique principale ;
  - affixes ;
  - **delta** par rapport à l'objet équipé : `+4 ATQ`, `−6 PV`, `+2 % VIT` ;
  - **score de puissance** global ;
  - boutons « Équiper », « Vendre », « Verrouiller ».
- Les deltas positifs sont verts avec une flèche haute, les négatifs rouges avec une flèche basse.
- Aucune comparaison ne masque la perte : un objet plus puissant peut faire perdre une statistique utile.
- L'aperçu du héros (équipement visible sur le sprite) change à l'équipement.

### 13.5 Vente et encombrement

- Inventaire : 60 emplacements, 20 par onglet de rangement (armes, armures, talismans).
- En cas de surcharge, les objets vont au **coffre de camp** (illimité) et le joueur est prévenu.
- Vente : `8 + 4 × niveau` or, × multiplicateur de rareté.
- Vente en masse des communs possible au camp.
- Objets verrouillés : jamais vendus, jamais fusionnés.

### 13.6 Puissance

Score de puissance, affiché dans les stats et utilisé pour les conseils :

```
Puissance = ATQ × 2 + PV / 4 + VIT × 20 + critique × 100 + régénération × 10
```

Le score sert à comparer les builds, jamais à calculer les dégâts.

---

## 14. Progression, difficulté et jalons

### 14.1 Difficulté

- Les monstres et gardiens évoluent avec le palier selon les formules du §8.
- Au-delà du palier 10, un **facteur de profondeur** de +12 % par palier s'ajoute aux PV et à l'ATQ des monstres, et +8 % pour les gardiens.
- Ce facteur est **compensé** par les niveaux, l'équipement et les améliorations.
- Aucun plafond de palier : le donjon est infini.

### 14.2 Rythme cible

| Moment | Cible d'expérience |
|---|---|
| Premiers 10 m | comprendre la marche automatique |
| Premier monstre | comprendre le combat automatique |
| Premier coffre | découvrir le quota de pas |
| Premier gardien | tension, défaite possible, découverte du repli |
| Premier farm | comprendre que l'échec nourrit la progression |
| Premier achat | fierté, courbe de coût lisible |
| Premier équipement | comparaison, choix |
| Palier 10 | jalon, nouveau biome, nouvelle couleur de feu |

### 14.3 Jalons

- **Tous les 10 paliers** :
  - +5 % d'or permanent ;
  - nouveau titre (« Veilleur des Cryptes », etc.) ;
  - apparence du feu de camp ;
  - 5 éclats d'âme ;
  - entrée dans le journal de légende.
- **Tous les 100 paliers** : palier « légendaire », apparence de héros, statue dans le camp.
- **Premier gardien vaincu** : déblocage de la forge.
- **10 coffres ouverts** : déblocage du compteur de quotas avancé.
- **50 000 pas cumulés** : apparence « Âme ardente » permanente.

### 14.4 Serments journaliers

Trois objectifs optionnels, renouvelés à 00:00, récompensés en or, XP et éclats :

| Serment | Exemple | Récompense |
|---|---|---|
| Marche | Parcourir 3 000 pas | 150 or + 1 éclat |
| Chasse | Vaincre 2 gardiens | 250 or + 2 éclats |
| Curiosité | Ouvrir 2 coffres | 1 objet inhabituel + 100 or |

- Les serments ne bloquent aucune progression.
- Ils s'affichent dans l'onglet Stats et dans le journal.
- Un serment non accompli disparaît à 00:00 sans pénalité.

---

## 15. Interface mobile

### 15.1 Principe général

Écran divisé en **deux parties verticales** :

- **Moitié supérieure : la scène.** Vue de profil en pixel art. Héros, monstres, combats, distance, ferveur.
- **Moitié inférieure : le tableau de bord.** Onglets Stats, Inventaire, Améliorations, bouton de synchronisation des pas.

Répartition de référence : **48 % scène / 52 % tableau de bord** sur un écran de 390 × 844. Sur les petits écrans (≤ 360 × 640), la scène passe à 44 % et le tableau de bord défile.

**Wireframe de référence :**

```
┌────────────────────────────────────┐
│ 21:47            réseau  batterie  │  barre d'état
├────────────────────────────────────┤
│ PALIER 03   CRYPTES   or  [feu]    │  bandeau de palier
│ ▓▓▓▓▓▓▓▓▓▓▓░░░░░░░░░  💀  70/100 m │  jauge de palier
│                                    │
│   [KAELEN]              [MONSTRE]  │  noms + PV
│    héros                  ennemi   │
│      ⚔       10 m        ☠         │  scène de profil
│                                    │
│  ~ braises ~   statues   porte     │  décor
│  [ferveur 62]       [3 124 pas]    │  HUD bas de scène
├────────────────────────────────────┤
│ avatar  Kaelen   niv.04   or/objets│  en-tête
│ [Stats] [Inventaire] [Améliorations]│ onglets
│                                    │
│  puissance · ATQ · PV · VIT        │
│  ferveur · serments · journal      │  contenu défilant
│                                    │
├────────────────────────────────────┤
│ dernière synchro · 1 240 en attente│
│ [     SYNCHRONISER LES PAS    ]    │  action principale
└────────────────────────────────────┘
```

### 15.2 Scène (haut)

De haut en bas :

1. **Barre d'état du téléphone** (heure, réseau, batterie) — discrète, intégrée au décor.
2. **Bandeau de palier** : « PALIER 03 · CRYPTES DE VAEL » + bouton feu de camp à droite.
3. **Jauge de palier** : 0 → 100 m, avec des graduations tous les 10 m et une tête de gardien au bout.
4. **Décor en parallaxe** : murs, arches, torches, statues, brume.
5. **Sol** : dalles, halte, bornes.
6. **Héros** à gauche du centre, monstre à droite, barres de vie et noms au-dessus.
7. **Effets** : dégâts flottants, butin, aura de ferveur, braises.
8. **Bas de scène** : jauge de ferveur (gauche) et compteur de pas du jour (droite).

### 15.3 Tableau de bord (bas)

**En-tête fixe :**

- avatar du héros (sprite dans un cadre) ;
- nom + niveau ;
- barre d'XP ;
- bouton feu de camp (rappel) ;
- or, rations, éclats en pastilles compactes.

**Onglets :**

| Onglet | Contenu |
|---|---|
| **Stats** | Puissance, ATQ, PV, VIT, critique, ferveur détaillée, serments, journal |
| **Inventaire** | 6 emplacements équipés, grille d'objets, fiche de comparaison |
| **Améliorations** | 3 pistes d'amélioration, coût, effet, achat |

**Barre d'action fixe en bas :**

- **Bouton de synchronisation des pas**, pleine largeur :
  - état prêt : « SYNCHRONISER LES PAS » + « + 1 240 pas en attente » ;
  - état en cours : « SYNCHRONISATION… » avec animation de points ;
  - état à jour : « AUCUN NOUVEAU PAS » avec prochaine actualisation ;
  - état refus : « ACTIVER LE COMPTEUR DE PAS » avec explication.
- Sous le bouton : dernière synchronisation, pas cumulés du jour.

### 15.4 Navigation et interactions

- Un tap sur un onglet change le contenu sans recharger la scène.
- L'onglet actif est souligné d'or et son libellé passe en clair.
- Un tap sur un monstre ou le héros ouvre une fiche courte (nom, niveau, PV, faiblesse).
- Un tap sur un coffre ouvre sa fiche de forçage.
- Un tap sur le feu de camp ouvre le campement.
- Un tap sur un objet ouvre la fiche de comparaison.
- Les gestes de retour ferment les fiches avant de quitter le jeu.
- Aucune action ne déclenche un combat manuel.

### 15.5 États et messages

| Situation | Message | Ton |
|---|---|---|
| Défaite | « Vaincu. Repli de 20 m dans la zone sûre. » | rassurant |
| Farm actif | « Repos de combat — butin réduit » | informatif |
| Gardien | « Le gardien scelle le passage. » | solennel |
| Coffre | « Serrure de 400 pas. Consacrer vos pas ? » | tentant |
| Ferveur pleine | « Âme ardente — +40 % vitesse » | exaltant |
| Pas insuffisants | « Encore 320 pas avant la prochaine ration. » | encourageant |
| Permission refusée | « Le compteur de pas est en veille. » | pédagogique |
| Camp | « Le feu veille. L'expédition est en pause. » | chaleureux |

### 15.6 Direction artistique de l'interface

- **Palette** : nuit indigo, pierre charbon, or vieilli, braise orange, sang séché.
- **Typographie** : pixel pour les titres et chiffres clés, sans-serif lisible pour les descriptions.
- **Cadres** : bordures pixel de 2 px, coins chanfreinés, légers liserés d'or.
- **Iconographie** : sprites d'objets du jeu, jamais d'icônes génériques.
- **Contrastes** : texte clair sur fond sombre, jamais de texte sur un sprite chargé.
- **Animations** : courtes (120–300 ms), jamais bloquantes ; mode « animations réduites ».
- **Zone pouce** : bouton de synchronisation dans le tiers inférieur, accessible à une main.
- **Cibles tactiles** : 44 × 44 minimum.

### 15.7 Accessibilité

- Ne jamais coder la rareté par la couleur seule : lettre + cadre + libellé.
- Mode daltonien : palette alternative bleu/orange.
- Taille de texte réglable (100 %, 115 %, 130 %).
- Animations réduites : suppression des secousses, des braises et des flashes.
- Pas de clignotement > 3 Hz.
- Lecteur d'écran : libellés explicites sur les boutons et les jauges.
- Aucun geste complexe : taps uniquement.
- Le jeu reste jouable sans son.

---

## 16. Onboarding

### 16.1 Premier lancement

1. Écran-titre : logo, ambiance feu de camp, bouton « Commencer ».
2. Explication du compteur de pas : « Vos pas réels font avancer le héros. 10 pas = 1 mètre. »
3. Demande d'autorisation du compteur de pas, avec explication claire et bouton « Plus tard ».
4. Choix du nom du héros (facultatif, modifiable plus tard).
5. Premier palier : le héros marche, un monstre apparaît, premier combat automatique.
6. Premier coffre : découverte du quota de pas.
7. Premier gardien : défaite scriptée possible, découverte du repli et du farm.
8. Premier passage au camp : achat guidé de « Lame affûtée ».
9. Premier équipement : comparaison guidée.

### 16.2 Règles d'onboarding

- Aucun tutoriel de plus de 3 écrans consécutifs.
- Chaque concept est introduit **au moment où il devient utile**.
- Les bulles explicatives sont refermables et ne reviennent pas.
- Le joueur peut sauter l'onboarding ; les aides restent consultables dans le camp.
- La première journée ne demande aucune dépense réelle.

### 16.3 Premiers jours

| Jour | Découverte | Objectif |
|---|---|---|
| J1 | marche, combat, coffre, gardien, camp | 1 000 pas, premier gardien |
| J2 | équipement, comparaison, forge | 1 objet équipé |
| J3 | serments, ferveur, farm | 3 serments |
| J4 | élite, autel, biome | palier 10 |
| J7 | jalon, apparence | première semaine de marche |

---

## 17. Cas limites et règles de robustesse

| Cas | Règle |
|---|---|
| **0 pas dans la journée** | Aucune progression. Le héros farme la dernière halte 20 min, puis veille. Aucune punition. |
| **Permission refusée** | Le jeu affiche une explication, propose de réessayer, et permet de consulter le jeu. La synchronisation manuelle reste possible si le téléphone fournit un compteur. |
| **Changement de jour à 00:00** | Les pas de la veille restent acquis dans la distance déjà parcourue. Le compteur du jour, la ferveur, les plafonds de rations/éclats et les serments repartent à zéro. |
| **Changement de fuseau** | La journée suit l'heure locale. Aucun pas n'est compté deux fois. |
| **Changement d'appareil** | Les pas déjà synchronisés ne sont pas rejoués. La progression (or, stuff, paliers) est conservée. |
| **Fermeture pendant un combat** | Au retour, le héros reprend à la dernière halte avec 50 % de PV. Le combat est abandonné sans perte. |
| **Fermeture prolongée** | Les pas disponibles sont récupérés à la réouverture. La simulation hors ligne est résumée en une ligne (pas, combats, or, paliers). Plafond : 100 000 pas par synchronisation (marathon/ultra couverts). |
| **Pas suspects** | Les pas au-delà de 250/min en moyenne ou 60 000/jour ne sont pas comptés. Aucun bannissement, aucune perte. |
| **Coffre non ouvert** | Conservé 24 h. Passé ce délai, il disparaît et le journal l'indique. |
| **Coffre forcé au changement de jour** | La progression de la serrure est conservée 24 h. |
| **Inventaire plein** | Les objets partent au coffre de camp. Aucun objet n'est détruit. |
| **Or insuffisant** | Bouton désactivé, indication du manque, aucune erreur. |
| **PV pleins au camp** | Boutons de soin désactivés avec un message bienveillant. |
| **Combat de gardien + camp** | Le gardien est réinitialisé, le héros soigné. Pas d'exploit : le soin coûte une ressource. |
| **Mode avion** | Le jeu fonctionne avec les pas déjà disponibles et la progression locale. La synchronisation reprend au retour du réseau. |
| **Batterie faible** | Le jeu réduit les animations et la fréquence des effets, sans changer les règles. |
| **Écran très petit** | La scène se comprime, le tableau de bord défile, le bouton de synchronisation reste fixe. |
| **Écran très grand / tablette** | La scène garde un ratio lisible, le tableau de bord s'élargit avec des marges. |

---

## 18. Équilibrage — repères de tuning

### 18.1 Temps cible

| Séquence | Durée cible |
|---|---:|
| Combat normal | 3–8 s |
| Combat élite | 6–12 s |
| Combat de gardien | 25–35 s |
| Farm de halte | 8 s par ennemi, 20 min max |
| Synchronisation | < 2 s de feedback |
| Achat/équipement | < 5 s |
| Session courte | 20–60 s |
| Session de gestion | 2–4 min |

### 18.2 Courbe de puissance

- La puissance doit augmenter de **8 à 15 % par jour** lors des 7 premiers jours.
- Un palier supplémentaire doit demander **10 à 20 % de puissance en plus**.
- Les améliorations de camp doivent représenter **40 %** de la puissance, l'équipement **40 %**, les niveaux **20 %**.
- Aucune source ne doit dépasser 60 % de la puissance totale.

### 18.3 Cible de conversion

| Métrique | Cible |
|---|---:|
| Taux de synchronisation à J1 | > 65 % |
| Pas synchronisés / pas détectés | > 80 % |
| Sessions par jour | 2,5–4 |
| Temps avant premier gardien | 6–10 min de marche cumulée |
| Taux de victoire du 1er gardien | 55–65 % |
| Taux de victoire du 2e gardien | 45–55 % |
| Recours au farm après défaite | > 70 % |
| Achat au camp dans les 24 h | > 80 % |

### 18.4 Signaux d'alerte

- Si le joueur reste bloqué 3 jours sur le même palier → proposer une aide ou un farm accentué.
- Si le joueur n'ouvre plus le jeu après 7 jours → réduire la pression, proposer un récapitulatif doux.
- Si la ferveur est toujours à 0 en fin de journée → revoir le coût de remplissage.
- Si les coffres ne sont jamais forcés → revoir les quotas ou les récompenses.

---

## 19. Indicateurs et critères d'acceptation

### 19.1 Indicateurs de succès

- **Rétention J1, J7, J30.**
- **Pas synchronisés par jour** et **taux de conversion** (pas détectés / pas utilisés).
- **Sessions par jour** et **durée moyenne**.
- **Progression** : paliers par jour, gardiens tentés/vaincus.
- **Économie** : or gagné/dépensé, taux d'achat, objets équipés.
- **Boucle de défaite** : nombre de replis, temps passé en farm, retours au combat.
- **Satisfaction** : notes, retours sur le camp, sur les coffres, sur la ferveur.

### 19.2 Critères d'acceptation fonctionnels

1. 10 pas convertissent toujours 1 m ; 1 000 pas traversent un palier.
2. Une rencontre se déclenche tous les 10 m, sans exception.
3. Un gardien bloque les 100 m et déclenche un combat obligatoire.
4. Tous les combats sont automatiques et se résolvent sans action du joueur.
5. Une défaite replace le héros exactement 20 m en arrière, sur une halte.
6. Après une défaite, le farm de halte produit de l'or et de l'XP sans bloquer la reprise.
7. Le héros ne peut pas mourir en farm de halte.
8. La ferveur est alimentée par les pas du jour et modifie la vitesse d'attaque et la régénération.
9. Une fraction des pas génère des rations et des éclats d'âme selon les seuils définis.
10. Un coffre peut exiger un quota de pas ; les pas consacrés ne font pas avancer la distance.
11. Le campement est accessible à tout moment et met l'expédition en pause.
12. Les améliorations ont un coût croissant et un effet immédiat.
13. L'inventaire permet de comparer, équiper, verrouiller et vendre.
14. L'écran est divisé en scène (haut) et tableau de bord (bas) avec les onglets Stats, Inventaire, Améliorations et le bouton de synchronisation.
15. Les pas sont réinitialisés à 00:00 sans perte de progression.
16. Aucun état ne bloque définitivement le joueur.
17. L'interface respecte les zones tactiles, les contrastes et les modes d'accessibilité.
18. Les ressources sont conservées après fermeture et reprise.
19. Le jeu reste lisible et jouable en mode avion et en mode silencieux.
20. Le journal retrace les événements clés de la session.

---

## 20. Hors périmètre et évolutions

### 20.1 Hors périmètre de la V1

- PvP, guildes, classements mondiaux.
- Achats réels et monnaie premium.
- Événements saisonniers.
- Compagnons combattants.
- Construction de campement (bâtiments multiples).
- Multijoueur en temps réel.
- Import de pas depuis plusieurs sources simultanées.

### 20.2 Évolutions candidates

- **Compagnon** : un familier qui farme automatiquement la halte.
- **Serments de guilde** : objectifs collectifs hebdomadaires.
- **Apparences** : skins de héros et de feu de camp.
- **Chasse au trésor** : cartes de palier à collectionner.
- **Modes de donjon** : paliers « cauchemar » à haut risque.
- **Récit** : chapitres narratifs tous les 10 paliers.
- **Accessibilité avancée** : lecture vocale complète, contraste renforcé.

---

## 21. Annexes

### 21.1 Glossaire

Voir §5.

### 21.2 Tableau récapitulatif des seuils

| Seuil | Valeur | Effet |
|---|---:|---|
| 1 pas | 0,1 m | avancée |
| 50 pas | 1 point de ferveur | bonus |
| 100 pas | 10 m | rencontre |
| 200 pas | 1 ration | soin |
| 500 pas | 1 éclat d'âme | soin complet / forge |
| 1 000 pas | 1 palier | gardien |
| 5 000 pas | 500 m | 5 paliers |
| 8 000 pas | 800 m | 8 paliers |

### 21.3 Exemple de calcul — une journée de 6 500 pas

- Distance : 650 m → 6 paliers complets + 50 m.
- Rencontres : 65 monstres, dont ~6 élites.
- Gardiens : 6 combats, ~4 victoires si le build suit.
- Coffres : ~10, dont 4 forcés (1 600 pas consacrés, soit 160 m de progression « sacrifiés »).
- Or : ~1 800 (monstres) + ~1 200 (gardiens) + ~600 (coffres) + ~250 (farm) = **~3 850 or**.
- XP : ~2 600 (monstres) + ~2 200 (gardiens) = **~4 800 XP**.
- Rations générées : 24 (plafond). Éclats générés : 12 (plafond).
- Ferveur : 100 (plafond atteint à 5 000 pas).
- Améliorations possibles : ~6 niveaux répartis sur les trois pistes.

### 21.4 Exemple de calcul — défaite contre le gardien du palier 3

- Héros : niveau 5, 312 PV, 40 ATQ, 1,04 VIT, équipement +8 ATQ / +40 PV.
- Dégâts effectifs : ~52 par coup, cadence ~1,1/s, soit ~57 DPS.
- Gardien palier 3 : 1 146 PV, 14,5 ATQ, 1,04/s en phase 2.
- Temps de kill théorique : ~22 s.
- Dégâts subis : ~130 en phase 1 + ~160 en phase 2 = ~290.
- Héros : 352 PV effectifs + 1 ration (123 PV) = 475 → **victoire à l'usure** (il reste ~185 PV).
- Sans équipement ni ration, le héros tombe vers 60 % du combat → **repli**, farm, amélioration, nouvelle tentative.

### 21.5 Crédits des ressources visuelles de la maquette

- Pixel art : **Dungeon Crawl Stone Soup — tiles**, publié sous **CC0 1.0**.
- Source : `github.com/crawl/tiles`, export `releases/Nov-2015`.
- Polices : **Pixelify Sans** et **Press Start 2P**, Google Fonts, **SIL Open Font License 1.1**.
- Aucune ressource propriétaire n'est utilisée dans la maquette.

---

*Fin des spécifications fonctionnelles — version 1.0.*
