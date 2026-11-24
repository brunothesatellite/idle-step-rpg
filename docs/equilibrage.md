# Équilibrage de la progression — état 0.20.0 et cible 1 → 100

> **Document de référence.** Version analysée : **0.20.6** (lots 1–6, `versionCode 53`), sauvegarde **v7**, 24 novembre 2026.
> Compagnons : [`validation-equilibrage.md`](validation-equilibrage.md), [`plan-equilibrage.md`](plan-equilibrage.md), [`tools/equilibrage/`](../tools/equilibrage/).
>
> Cette version remplace les anciennes sections 6.1, 6.1bis, 6.9 et 6.10, qui contenaient **plusieurs courbes cibles incompatibles**. Il n'y a plus qu'une seule table de constantes et qu'une seule cible de référence.

## 1. Résumé exécutif

Le rythme de marche est sain : 1 000 pas par palier, 100 paliers = 100 000 pas. Le problème n'est pas le pas, ni le farm, ni l'absence de paywall. Le problème est un empilement de **quatre systèmes qui ne suivent pas la même échelle** :

1. **L'XP** était en `1,2^niveau` : le niveau 100 demandait ~41 milliards d'XP, soit des millions de monstres. **Corrigé par le lot 1** : `1,09`, ~6,8 M XP, 2 500–4 000 kills.
2. **Les objets** avaient des drops plats et des améliorations exponentielles. **Corrigé par le lot 2** : butin ×1 à ×6 selon le palier, croissance additive +4 % de la base, +20 niveaux max.
3. **Les gardiens** sont un interrupteur : le build de référence gagne 100 % des monstres normaux mais 0 à 8 % des gardiens ; le build au-dessus gagne 100 % des gardiens. Il n'y a presque pas de zone 35–65 %.
4. **L'attrition**, rétablie récemment, consomme **5 à 9 rations par palier**, alors que l'économie cible en produit 3 à 6 par jour. La ration devient la vraie barrière.

La Phase A a correctement traité le **mur de survie du palier 36**. Pour un palier 100 exigeant mais non frustrant, il faut maintenant traiter l'**XP**, la **croissance des objets**, le **seuil des gardiens** et l'**économie des rations**, puis rendre la difficulté lisible et récupérable.

## 2. Méthode et périmètre

Sources utilisées :

- code Android `0.20.0` (`GameEngine.kt`, `Model.kt`, UI) ;
- simulateur `tools/equilibrage/balance_sim.py`, 300 à 500 runs par cellule ;
- rapport initial 0.19.1 et playtest en cours de la Phase A ;
- `docs/validation-equilibrage.md` pour les critères.

Limites :

- le simulateur ne modélise pas encore la ferveur qui décroît, les sets complets, toutes les capacités de gardien, les coffres et l'économie réelle ;
- les presets `reference_*` sont des profils moyens, pas des sauvegardes réelles ;
- le code et le simulateur ne sont pas encore garantis identiques (voir §4.3).

## 3. État mesuré en 0.20.0

### 3.1 Pas et distance

- 10 pas = 1 m, 100 m = 1 palier → **1 000 pas par palier**.
- 1 → 100 = **100 000 pas** de marche pure, plus farm et retries.
- À 5 000–10 000 pas/jour : 10–20 jours de marche pure, mais **1 à 3 mois** avec progression, farm et apprentissage.
- Simulation hors ligne : 30 % du budget quotidien, plafonnée à 4 minutes simulées.

**Conclusion : le levier « pas » est sain. Il ne faut pas le durcir.**

### 3.2 Héros

Formules actuelles (`statsOf`) :

```kotlin
ATQ  = 18 + 3 × (niveau − 1) + 3 × camp_lame + 2 × (palier / 10) + équipement
PV   = 150 + 18 × (niveau − 1) + 30 × camp_souffle + 12 × (palier / 10) + équipement
VIT  = 1 + 0,01 × (niveau − 1) + 0,025 × camp_pas + équipement
```

- Les gains de niveau sont **linéaires et faibles** (+3 ATQ, +18 PV, +1 % VIT).
- Les améliorations de camp sont désormais **en pourcentage de la base héros** : +1,5 % ATQ, +1,5 % PV, +1 % VIT par niveau, plafond **30**.
- Le stuff représente l'essentiel de la puissance.
- **La VIT est plafonnée par le rythme d'attaque** : `intervalle = 1000 / (VIT × (1 + 0,004 × ferveur))`, minimum **180 ms** depuis le lot 2. La VIT utile est plafonnée à **4,0** ; au-delà, la fiche Stats affiche « MAX » et la valeur n'est plus prise en compte.

### 3.3 Ennemis

```kotlin
depthMob  = min(1,12^(palier−10), 40)
depthBoss = min(1,08^(palier−10), 40)

// Phase A : interpolation entre l'ancienne courbe (≤ 20) et la cible (≥ 35)
mobHpScale  = lerp(depthMob,  26,0 − 0,08  × (palier−10), poids)
mobAtkScale = lerp(depthMob,   9,0 − 0,028 × (palier−10), poids)
bossHpScale = lerp(6 × depthBoss, 72,0 − 0,20 × (palier−10), poids)
bossAtkScale= lerp(1,6 × depthBoss, 3,6 − 0,012 × (palier−10), poids)

PV monstre  = (55 + 34 × (niveau−1)) × mobHpScale  × règleBiome
ATQ monstre = (6 + 2,2 × (niveau−1)) × mobAtkScale × règleBiome
```

Valeurs au palier 36 (cible Phase A) :

| Élément | PV | ATQ |
|---|---:|---:|
| Monstre normal | ~30,6 k | ~670 |
| Gardien | ~87,7 k | ~286 |

Le gardien est au **niveau palier + 1**. Les monstres ont **6 % de chance d'être élites** (1,8× PV, 1,15× ATQ). Le gardien est choisi par rotation dans le pool du biome : `(palier − début du biome) % nombre de gardiens`; les gros jalons 25/50/100 gardent leurs gardiens spéciaux. Il dispose de :

- un bouclier de **35 % / 3 s / 8 s** ;
- une **phase 2** sous 50 % de PV : capacité télégraphiée, sans accélération permanente à 950 ms ;
- un **enrage** après 25 s : +15 % ATQ toutes les 5 s, plafond +60 % ;
- un modificateur aléatoire parmi RAPIDE, BLINDÉ, BERSERK, RÉGÉNÉRANT, VOLEUR ;
- une capacité de gardien (slam 2,4×, souffle 2,0×, drain 1,5×, etc.) ;
- une **pitié** de −5 % PV/ATQ par défaite, plafond −25 %, remise à zéro à la victoire.

Le gardien est précédé d'un **portail à 90 m** : le joueur voit ses PV, son ATQ, sa capacité, son modificateur et peut choisir **Engager / Farmer ce palier / Camper**.

### 3.4 Combat : le seuil binaire

Simulation `--curve target`, 300 runs, biome auto (mesure **avant le lot 5** ; le portail, la pitié et l'enrage 25 s modifient les résultats) :

| Palier | `reference` monstre | `reference` gardien | `reference_high` gardien | `player36` gardien |
|---:|---:|---:|---:|---:|
| 36 | 100 % | **8 %** | 100 % | **0 %** |
| 50 | 100 % | **0 %** | 100 % | 0 % |
| 75 | 100 % | **0 %** | 100 % | 0 % |
| 100 | 100 % | **0 %** | 100 % | 0 % |

Ce tableau dit trois choses :

- les monstres normaux ne challengent plus le build de référence ;
- le gardien est un **seuil de stuff**, pas une difficulté progressive ;
- le rapport initial annonçait `player36` à **59 %** au gardien 36, le simulateur le donne à **0 %**. Le code ajoute encore **+2 niveaux** au gardien, donc le jeu réel est probablement plus dur que la simulation.

Une variation de 3 % de stats peut faire basculer 59 % → 0 % : c'est le propre d'un système à seuil. Il faut viser des **bandes de victoire**, pas des seuils.

### 3.5 XP — lot 1 appliqué

```kotlin
xpNext(level) = round(120 × 1,09^(level−1))
xpMult(palier) = 1 + 0,12 × (palier−1)
xpMonstre = round((12 + 5 × niveau) × xpMult(palier))
xpGardien = 10 × xpMonstre
```

| Niveau | XP pour le suivant | Total depuis 1 |
|---:|---:|---:|
| 34 | 2 062 | ~21 600 |
| 50 | 8 186 | ~89 600 |
| 100 | 608 702 | **~6 762 000** |

- Au palier 36 : **~998 XP** par monstre, **~10 500** par gardien.
- Au palier 100 : **~6 600 XP** par monstre, **~66 000** par gardien.
- **Rattrapage** : si `niveau < palier − 4`, l'XP gagnée est majorée de **25 %** jusqu'au retour à `palier − 4`.
- L'ancienne courbe `1,2^niveau` (41,4 Md XP) est conservée en historique au §8.4 ; elle n'est plus utilisée.

### 3.6 Butin et objets — lot 2 appliqué

- `lootItem()` applique un **facteur de palier** : `floorScale = min(6,0, 1 + 0,055 × (palier−1))`.
- Raretés cibles : **1,15 / 1,5 / 1,9 / 2,4**.
- Un rare du palier 100 vaut donc environ **×9** sa base (1,5 × 6), contre ×1,5 avant le lot 2.
- `growStat()` est **additif** : `+4 % de la statistique de base` par niveau, avec un minimum de +1.
- Chaque objet neuf porte `maxLevel = palier du drop + 20` ; les anciens objets sont plafonnés à leur niveau actuel +20 lors de leur première amélioration.
- Coût d'amélioration : `30 × niveau + 20` or + 1 éclat.
- Fusion : **75 %** de la somme (le lot 7 passera à 90 %).
- Effets plafonnés : vol de vie 25 %, réduction de dégâts 40 %, critique 75 %, dégâts critiques +100 %, dégâts gardiens +50 %.
- Les anciens objets gardent leurs statistiques ; seuls les nouveaux drops et les upgrades futurs utilisent le barème.

### 3.7 Économie

Plafonds et revenus actuels :

- éclats : **24** max ; gardien +3, jalon +8, coffre +2, élite 20 % +1, démontage 2/3/6/10 ; amélioration et reforge 1 éclat ;
- rations : **30** max ; gardien +3, coffre +3, monstre **20 %**, élite **+1 garanti** ; auto-ration sous **40 %** des PV pour **+45 %** ;
- or : butin de monstre/gardien, camp, marchand, statue ;
- repos gratuit : **2 fois par jour**, **20 % PV** par repos ;
- farm : **+10 % PV** par kill ;
- `holdFloor` (farm de palier) : le gardien vaincu donne du butin, des consommables, **+1 éclat et +1 ration** (5 gardiens/jour), sans avancer le palier.

La table ci-dessous est la **mesure 0.20.2 avant le lot 4** (auto-ration 30 %/35 %, farm 8 %). Le lot 4 augmente les revenus et le soin automatique ; la consommation réelle doit être re-mesurée au playtest.

Simulation de l'attrition (`reference`) :

| Palier | Rations / monstre | Rations / palier (9 mobs + gardien) |
|---:|---:|---:|
| 36 | 0,68 | **~7,1** |
| 50 | 0,79 | **~8,1** |
| 75 | 0,93 | **~9,4** |
| 100 | 0,96 | **~9,6** |

À 5 paliers/jour, il faut **35 à 50 rations/jour**, alors que la cible est **3 à 6**. Même avec les coffres, les gardiens et les 10 % de drop, l'économie ne suit pas.

### 3.8 Ce qui fonctionne déjà

- Le pas comme ressource principale, sans paywall.
- Le farm de palier : ennemis sans profondeur, niveau `palier−2`, PV ×0,65, ATQ ×0,6 ; le héros ne peut pas mourir en farm (PV remis à 1), et chaque kill rend **+10 % PV**. Un badge **RÉCUPÉRATION** l'affiche.
- Le farm de gardien donne **+1 éclat et +1 ration**, plafonné à **5 gardiens farmés par jour** ; au-delà, seul le butin continue.
- Depuis 0.20.0, **aucun soin automatique après un monstre normal** : seuls le farm, le camp, les rations et les éclats rendent des PV. C'est le choix d'attrition assumé.
- La défaite est bornée : repli de 20 m, 60 % PV, aucune perte d'objet ni d'or.
- La bénédiction de gardien (+25 %/défaite, cap +75 %) est visible dans la scène.
- La Phase A a déjà amélioré les éclats/rations, les élites, le bouclier et le score d'objet.

## 4. Diagnostic

### 4.1 Failles critiques (P0)

| # | Faille | Effet | Priorité |
|---|---|---|---|
| C1 | XP `1,2^n` | **corrigé lot 1** : 1,09, ~6,8 M XP, 2 500–4 000 kills | résolu |
| C2 | Drops plats + upgrades composés | **corrigé lot 2** : butin ×6, upgrades additifs +4 %/+20 | résolu |
| C3 | Gardien à seuil 0/100 | **corrigé lot 5** : courbe, pitié visible, portail à 90 m | résolu |
| C4 | Attrition sans rations suffisantes | **corrigé lot 4** : cap 30, revenus +, auto-ration 40/45, repos 2×20 %, farm 10 % | résolu |
| C5 | Trois courbes cibles dans l'ancien rapport | playtest non interprétable | **critique** |
| C6 | VIT morte au-delà de ~2,8 | **corrigé lot 2** : cap 4,0, intervalle 180 ms, affichage MAX | résolu |
| C7 | Effets empilables sans cap | **corrigé lot 2** : caps 25/40/75/100/50 | résolu |
| C8 | Gardien auto-déclenché à 100 m | **corrigé lot 5** : portail à 90 m avec choix | résolu |
| C9 | `holdFloor` sans éclats/rations | **corrigé lot 6** : +1/+1, cap 5 gardiens/jour | résolu |
| C10 | Camp/sets plats + or sans sink | **corrigé lot 3** : camp et sets en %, coût indexé, plafond 30 | résolu |
| C11 | Enrage dans une zone morte | **corrigé lot 5** : enrage après 25 s, phase 2 télégraphiée | résolu |
| C12 | Fusion 75 %, loot sans pity | frustration et séries sèches | moyenne |
| C13 | Biomes/modificateurs non budgétés | pics imprévisibles | moyenne |
| C14 | Fin de jeu 81–100 répétitive | lassitude avant le palier 100 | moyenne |

### 4.2 Failles de frustration

1. **Arriver à 25 % de PV devant un gardien** après 9 monstres, sans possibilité de potion avant l'engagement.
2. **L'auto-ration se déclenche après le coup** : un coup fatal depuis 35 % de PV passe à travers le filet.
3. **La pitié est visible mais peu lisible** : le joueur ne sait pas quel sera son prochain essai.
4. **Les modificateurs de gardien** ne sont pas annoncés avant le combat ; RAPIDE + phase 2 + enrage peut créer un pic brutal.
5. **Le farm de gardien** ne rend pas les ressources promises, alors qu'il est présenté comme le filet de sécurité.
6. **Le stuff de haut palier** ne récompense pas la progression ; le joueur ne comprend pas pourquoi un drop du 100 vaut un drop du 36.
7. **La VIT** continue d'apparaître sur les objets alors qu'elle ne sert plus.
8. **Les sets** restent à surveiller : vérifier que les paliers 2/4/6 sont lisibles et utiles.
9. **L'or** s'accumule sans usage à haut niveau.
10. **La fusion** à 75 % décourage une mécanique qui devrait être un choix.

### 4.3 Dette de documentation

L'ancien document contenait :

- §6.1 : monstre 36 à **18,8 k PV / 230 ATQ**, gardien 28,4 k / 307 ;
- §6.1bis / §6.10 : monstre **30,6 k / 670**, gardien **87,7 k / 286** ;
- §6.9 : `player36` gardien 36 à **100 %, 6,4 s** ;
- §6.10 : `player36` gardien 36 à **59 %, 22,9 s** ;
- simulateur `target` : `player36` à **0 %**.

Ces chiffres ne peuvent pas tous être vrais. Cette version du document ne conserve que la **courbe Phase A** (§3.3), qui est celle du code, et fixe une cible unique (§5).

## 5. Modèle cible 1 → 100

### 5.1 Principes

1. **Lisible** : le joueur voit sa puissance, le pronostic et ce qui lui manque.
2. **Progressif** : 35–65 % de victoires pour le stuff attendu, jamais 0/100.
3. **Récupérable** : chaque échec donne de l'XP/or de consolation et un chemin de farm/camp.
4. **Sans statistique morte** : VIT, sets, camp et or ont un usage à tous les paliers.
5. **Sans dead-end** : depuis 0 ration/0 éclat, il existe toujours une action gratuite ou peu coûteuse.
6. **Sans exécution obligatoire** : le challenge est la préparation (stuff, potions, choix d'engager), pas le timing au pixel.

### 5.2 Bandes de difficulté

| Type | TTK cible | Coups portés | Coups encaissés | Victoires stuff attendu |
|---|---:|---:|---:|---:|
| Monstre normal | 4–6 s | 12–20 | 4–6 | ≥ 95 % |
| Élite | 8–12 s | 20–30 | 4–6 | ≥ 80 % |
| Gardien | 15–22 s | ≥ 30 | 8–12 | **35–65 %** |
| Gardien + potion | 13–18 s | ≥ 25 | 8–12 | 55–75 % |
| Stuff faible | — | — | — | 10–25 % |
| Stuff avancé | — | — | — | 85–95 % |

### 5.3 Formules cibles (proposition à valider)

**XP**

```kotlin
xpNext(level) = round(120 × 1,09^(level−1))
xpMult(palier) = 1 + 0,12 × (palier−1)
xpMonstre = round((12 + 5 × niveau) × xpMult(palier))
xpGardien = 10 × xpMonstre
```

- Total 1 → 100 : **~6,8 M XP**, soit **2 500–4 000 kills**.
- Niveau 100 visé vers le **palier 100–120**.
- Rattrapage : si `niveau < palier − 4`, appliquer ×1,25 XP jusqu’à revenir à `palier − 4` (évite le farm d'XP forcé).

**Objets**

```kotlin
floorScale = min(6.0, 1 + 0,055 × (palier−1))
// rareté : uncommon 1,15 ; rare 1,5 ; epic 1,9 ; legendary 2,4
stats = base × rareté × floorScale
```

- Un rare du palier 100 devient **nettement** supérieur à un rare du 36, sans inflation infinie.
- Les anciens objets gardent leurs stats (pas de migration) ; seuls les nouveaux drops et les améliorations futures utilisent le barème.

**Amélioration d'objet**

```kotlin
niveauMax = palierDuDrop + 20
growStat(valeur, base) = valeur + max(1, round(base × 0,04))
coût = 30 × niveau + 20 // or, +1 éclat
```

- Croissance **additive**, pas composée : un objet ne peut plus être multiplié par 2 200.
- Plafond de 20 niveaux au-dessus du drop : un objet du palier 100 monte au maximum au niveau 120, pas au niveau 200.

**Camp**

- +1,5 % ATQ, +1,5 % PV, +1 % VIT par niveau, appliqués à la **base héros** (pas au stuff), plafonnés à 30 niveaux.
- Coût indexé sur le palier : `ceil(45 × 1,18^niveau × (1 + 0,04 × palier) / 5) × 5`.
- L'or retrouve un usage à tous les paliers.

**Sets**

- Bonus en pourcentage : 2 pièces +4 %, 4 pièces +8 %, 6 pièces +12 % (ATQ/PV ou effet thématique).
- Les bonus restent liés aux pièces **équipées** ; la collection reste permanente, sans bonus.

**VIT**

- Intervalle minimum : **180 ms** (au lieu de 300 ms).
- Cap de VIT utile : **4,0** ; au-delà, la stat est plafonnée et affichée comme telle.
- Les objets de haut palier ne génèrent plus de VIT au-delà du cap.

**Effets**

| Effet | Cap |
|---|---:|
| Vol de vie | 25 % |
| Réduction de dégâts | 40 % |
| Critique | 75 % |
| Dégâts critiques | +100 % |
| Dégâts contre gardiens | +50 % |

### 5.4 Économie et récupération

- Rations : plafond **30** ; gardien +3, coffre +3, monstre 20 %, élite +1 garanti.
- Auto-ration : seuil **40 %**, soin **45 %**, une fois par combat.
- Farm : **+10 % PV** par kill, visible dans le journal et dans l'écran de farm.
- Camp : **repos gratuit** 20 % PV, 2 fois par jour ; repos payant 1 ration +40 % ; soin complet 1 éclat.
- Objectif de consommation : **15–20 rations/jour** pour 5 paliers/jour.
- `holdFloor` : gardien farmé donne **+1 éclat et +1 ration** (pas de palier), pour que le farm soit réellement une récupération.
- Éclats : 5–10/jour, cap 24 ; les upgrades plafonnés évitent l'inflation.

### 5.5 Butin, sets, fusion

- Pity : rare garanti tous les **5 kills** de monstre ; pièce de set garantie tous les **10 gardiens** (ou jeton de set).
- Non identifié : maximum **1 drop sur 10**.
- Fusion : **90 %** de la somme, en conservant l'effet et le set.
- Les sets doivent rester un choix : 2/4/6 pièces, pas une obligation de 6/6.

### 5.6 Gardiens

- Niveau : **palier + 1** (au lieu de +2), pour coller à la cible.
- PV : calibrés pour **18 s** de TTK avec le stuff attendu.
- ATQ : calibrée sur l'EHP du stuff attendu, diviseur **20–22** (au lieu de 16).
- Bouclier : **35 % / 3 s / 8 s**.
- Phase 2 à 50 % PV : **capacité télégraphiée**, sans réduction permanente de l'intervalle à 950 ms.
- Enrage : après **25 s**, +15 % ATQ toutes les 5 s, plafond **+60 %**.
- Pitié : **−5 % PV/ATQ du gardien par défaite**, plafond −25 %, affichée avant le combat. Alternative : +15 % dégâts héros, plafond +60 %.
- Portail à **90 m** : arrêt, écran « Gardien devant », pronostic, choix **Engager / Farmer / Camper**, potion utilisable avant.
- Modificateur et capacité **annoncés** avant l'engagement ; un reroll de modificateur peut être proposé au camp contre de l'or.

### 5.7 Fin de jeu 81–100

- Éviter 20 paliers identiques dans le même biome.
- Après le palier 80 : **Trône brisé** avec modificateurs de biome, ou NG+ avec conservation des collections.
- Les jalons 90 et 100 doivent être des **événements**, pas seulement des multiplicateurs.

## 6. Décisions validées (24 novembre 2026)

| Décision | Choix retenu | Alternative écartée |
|---|---|---|
| Courbe XP | **1,09** + multiplicateur linéaire | 1,10 |
| Croissance d'objet | **Additive +4 % de la base, +20 niveaux max** | Composée avec soft cap |
| Rations | **Drops + repos gratuit + auto-ration 40 %/45 %** | Drops seuls, repos seul |
| Lissage gardien | **Courbe + pitié en debuff visible** | Pitié en dégâts héros |
| Pitié | **−5 % PV/ATQ par défaite, cap −25 %** | +15 % dégâts, cap +60 % |
| Fin de jeu | **NG+ / Trône brisé** | Nouveaux biomes |
| Cap de butin | **×6 au palier 100** | ×8 |

> Ces choix sont la référence pour la Phase B. Les alternatives ne sont conservées que comme historique ; toute modification devra être justifiée par un playtest.

## 7. Risques et précautions

- **Migration** : ne pas recalculer les objets existants ; les nouveaux barèmes ne s'appliquent qu'aux nouveaux drops et upgrades.
- **Inflation** : le plafond d'upgrade et le cap de butin doivent être testés avant d'être publiés.
- **Playtest** : le simulateur ne remplace pas 7–14 jours de jeu réel ; noter paliers, défaites, rations, or, niveaux.
- **Documentation** : toute modification de formule doit être répercutée dans `balance_sim.py` et dans ce document, sinon les chiffres divergent à nouveau.
- **Sauvegardes** : conserver la compatibilité v4 → v7 et les tests de migration.

## 8. Annexes

### 8.1 Formules actuelles (référence code 0.20.2)

```kotlin
xpNext(l) = round(120 * 1.09^(l-1))
upgradeCost(item) = 30 * item.level + 20
growStat(v, base) = v + max(1, round(base * 0.04))
maxLevel(item) = niveau du drop + 20
floorScale = min(6.0, 1 + 0.055 * (palier-1))
fusion = 0.75 * somme(3 objets)
depthMob = min(1.12^(palier-10), 40)
depthBoss = min(1.08^(palier-10), 40)
intervalleHéros = max(180 ms, 1000 / (VIT * (1 + 0.004 * ferveur)))
autoRation = PV < 30 % max, +35 %, 1 fois/combat
```

### 8.2 Commandes de simulation

```bash
# Courbe du code (Phase A) : monstres et gardiens
python tools/equilibrage/balance_sim.py --curve target \
  --build player36,reference,reference_high \
  --floors 36,50,75,100 --runs 300

# XP et pas
python tools/equilibrage/balance_sim.py --xp

# Économie
python tools/equilibrage/balance_sim.py --economy
```

### 8.3 Source de vérité

- Kotlin : `android/app/src/main/java/com/brunothesatellite/idlesteprpg/engine/Balance.kt`.
- Miroir Python : `tools/equilibrage/balance_constants.py`.
- Vérification : `python tools/equilibrage/check_constants.py` (exécutée par la CI Android).
- Toute nouvelle constante doit être ajoutée dans les trois endroits, puis vérifiée avant le playtest.

### 8.4 Historique

- **0.19.1** : rapport initial ; diagnostic du mur du palier 36 (ATQ ennemie exponentielle, XP, économie).
- **0.20.0 / Phase A** : interpolation des courbes, bouclier 40 %, enrage, élites 6 %, pitié +25 %, éclats/rations revus, score d'objet PV ×0,6.
- **0.20.0 / attrition** : suppression du soin complet après les monstres normaux ; l'attrition est conservée. Conséquence : l'économie des rations doit être recalibrée (§5.4).
- **0.20.1 / lot 1** : XP 1,09, récompenses linéaires par palier, rattrapage ×1,25, source de vérité `Balance.kt`. Ancienne courbe 1,2 : 41,4 Md XP, conservée ici comme historique.
- **0.20.2 / lot 2** : butin ×1 à ×6, raretés cibles, upgrades additifs +4 %/+20, caps VIT/effets. Ancien upgrade composé ×2 199 et anciens drops plats conservés en historique.
- **0.20.3 / lot 3** : camp et sets en pourcentage, coût de camp indexé sur le palier, plafond 30 niveaux.
- **0.20.4 / correctif + lot 4** : rotation des gardiens par palier dans chaque biome ; rations cap 30, revenus augmentés, auto-ration 40/45, farm 10 %, repos gratuit 2×20 %, farm de gardien +1/+1.
- **0.20.5 / lot 5** : niveau palier +1, bouclier 35 %/3 s/8 s, phase 2 télégraphiée, enrage 25 s, pitié −5 %/défaite cap −25 %, portail à 90 m avec Engager/Farmer/Camper.
- **0.20.6 / lot 6** : farm +10 % PV avec badge RÉCUPÉRATION, farm de gardien +1/+1 plafonné à 5/jour.
- **Ce document** : remplace les cibles contradictoires de l'ancien rapport et sert de base au plan `docs/plan-equilibrage.md`.
