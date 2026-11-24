# Plan d'implémentation — équilibrage 1 → 100

> **Statut : proposition, en attente de validation.** Aucun code n'est modifié par ce document.
> Base de référence : [`equilibrage.md`](equilibrage.md), version **0.20.7** (lots 1–7, `versionCode 54`), sauvegarde **v7**.
> Objectif : rendre le palier 100 **exigeant, lisible et non frustrant**, sans paywall ni barrière définitive.

## 1. Principes directeurs

1. **Une seule source de vérité** : les constantes, le simulateur et la documentation doivent décrire la même courbe.
2. **Pas de seuil 0/100** : viser des bandes de victoire (35–65 % pour le stuff attendu).
3. **Pas de statistique morte** : VIT, sets, camp et or doivent avoir un usage à tous les paliers.
4. **Pas de dead-end** : depuis 0 ration / 0 éclat, il existe toujours une action gratuite ou peu coûteuse.
5. **Échec borné et utile** : consolation XP/or, pitié visible, farm accessible.
6. **Challenge de préparation** : stuff, potions, choix d'engager ; pas de timing au pixel.
7. **Compatibilité** : ne pas recalculer les objets existants ; les nouveaux barèmes ne s'appliquent qu'aux nouveaux drops et aux upgrades futurs.

## 2. Décisions validées (24 novembre 2026)

Les sept recommandations ont été retenues. Elles sont la référence pour la Phase B.

| Décision | Choix retenu | Alternative écartée |
|---|---|---|
| Courbe XP | **1,09** + multiplicateur linéaire de palier | 1,10 |
| Croissance d'objet | **Additive +4 % de la base, +20 niveaux max** | Composée avec soft cap |
| Rations | **Drops + repos gratuit + auto-ration 40 %/45 %** | Hausse des drops uniquement |
| Lissage gardien | **Courbe + pitié en debuff visible** | Pitié en dégâts héros |
| Pitié | **−5 % PV/ATQ par défaite, cap −25 %** | +15 % dégâts, cap +60 % |
| Fin de jeu | **NG+ / Trône brisé** | Nouveaux biomes |
| Cap de butin | **×6 au palier 100** | ×8 |

Le lot 1 peut démarrer. Le lot 5 (gardiens) reste dépendant des lots 1 à 4.

## 3. Vue d'ensemble

| Lot | Sujet | Priorité | Effort | Dépend de |
|---|---|---|---|---|
| 0 | Source de vérité, constantes, simulateur | P0 | S–M | — |
| 1 | Courbe d'XP et gains de niveau | P0 | S | 0 |
| 2 | Objets, upgrades, butin, caps | P0 | M–L | 0 |
| 3 | Camp, sets, or | P1 | M | 2 |
| 4 | Rations, attrition, récupération | P0 | M | 0 |
| 5 | Gardiens, pitié, portail | P0 | M–L | 1–4 |
| 6 | Farm et `holdFloor` | P1 | S | 4 |
| 7 | Pity de butin, fusion, identification | P1 | S–M | 2 |
| 8 | Biomes et modificateurs | P2 | M | 5 |
| 9 | Fin de jeu 81–100, télémétrie | P2 | L | 5–8 |

**Ordre recommandé : 0 → 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9.**  
Le lot 5 (gardiens) doit être calibré **en dernier**, après XP, objets et rations ; sinon les chiffres seront à refaire.

## 4. Lots détaillés

### Lot 0 — Source de vérité

**Statut : socle livré.** `Balance.kt`, miroir `balance_constants.py`, vérification CI et tests unitaires sont en place. L'alignement fin des formules restantes se poursuit dans les lots suivants.

**Objectif** : arrêter les divergences doc / code / simulateur.

- Créer une table unique de constantes d'équilibrage (dans `Model.kt` ou un fichier `Balance.kt` dédié) :
  - `XP_BASE`, `XP_GROWTH`, `XP_FLOOR_MULT` ;
  - `LOOT_FLOOR_SCALE`, `ITEM_MAX_LEVELS`, `RARITY_MULT` ;
  - `MOB_HP_SCALE`, `MOB_ATK_SCALE`, `BOSS_HP_SCALE`, `BOSS_ATK_SCALE` ;
  - `RATION_CAP`, `AUTO_RATION_THRESHOLD`, `AUTO_RATION_HEAL` ;
  - `VIT_CAP`, `MIN_HERO_INTERVAL_MS` ;
  - caps d'effets.
- Faire pointer `tools/equilibrage/balance_sim.py` sur les mêmes valeurs (ou générer un JSON commun).
- Ajouter un test de cohérence : les formules clés du simulateur et de `GameEngine` doivent correspondre à la table.
- Mettre à jour `docs/equilibrage.md` et `tools/equilibrage/README.md` en même temps que le code.

**Fichiers** : `Model.kt`, `GameEngine.kt`, `tools/equilibrage/balance_sim.py`, `docs/equilibrage.md`.

**Critère** : une modification de constante ne demande qu'un seul endroit à changer, et le simulateur reproduit le code à ±1 %.

---

### Lot 1 — Courbe d'XP

**Statut : livré en 0.20.1.** `xpNext` 1,09, récompenses linéaires, rattrapage ×1,25.

**Objectif** : rendre le niveau 100 atteignable en 2 500–4 000 kills.

**Modifications** :

- `Model.kt` : `xpNext(level) = round(120 × 1,09^(level−1))`.
- `GameEngine.kt` : récompenses XP linéaires par palier :
  ```kotlin
  xpMult(palier) = 1 + 0,12 × (palier−1)
  xpMonstre = round((12 + 5 × niveau) × xpMult(palier))
  xpGardien = 10 × xpMonstre
  ```
- Rattrapage : si `niveau < palier − 4`, appliquer ×1,25 XP jusqu’à `palier − 4`.
- Vérifier `gainXp` : la boucle `guard < 8` doit gérer les gros gains sans perdre d'XP.

**Tests** :

- table de valeurs : niveaux 1, 34, 50, 100 ;
- total 1 → 100 ≈ 6,8 M XP ;
- un monstre de palier 100 donne ~8 000 XP ;
- niveau 100 atteint vers le palier 100–120 ;
- test de non-régression : aucune save ne perd d'XP.

**Critère** : 2 500–4 000 kills estimés pour 1 → 100, mesurés par le simulateur `--xp`.

---

### Lot 2 — Objets, upgrades, butin, caps

**Statut : livré en 0.20.2.** Butin ×1–×6, raretés cibles, upgrades additifs +4 %/+20, caps VIT/effets.

**Objectif** : un drop de palier 100 doit être meilleur qu'un drop de palier 36, sans croissance exponentielle.

**Modifications** :

- `lootItem()` :
  ```kotlin
  floorScale = min(6.0, 1 + 0,055 × (palier−1))
  // rareté : uncommon 1,15 ; rare 1,5 ; epic 1,9 ; legendary 2,4
  stats = base × rareté × floorScale
  ```
- `growStat()` : croissance **additive** :
  ```kotlin
  growStat(valeur, base) = valeur + max(1, round(base × 0,04))
  ```
- `upgradedItem()` : niveau maximum = `niveauDuDrop + 20`.
- `upgradeCost()` : conserver `30 × niveau + 20`, mais refuser l'upgrade au-delà du plafond.
- Effets : caps dans `statsOf()` :
  - vol de vie **25 %** ;
  - réduction de dégâts **40 %** ;
  - critique **75 %** ;
  - dégâts critiques **+100 %** ;
  - dégâts gardiens **+50 %**.
- VIT : intervalle minimum **180 ms**, cap **4,0** ; afficher « VIT plafonnée » dans la fiche.
- Score d'objet : vérifier que « Équiper le meilleur » ne favorise plus un build glass cannon avec les nouveaux PV.

**Tests** :

- un rare du 100 > un rare du 36 d'au moins ×2 en stats utiles ;
- un objet ne peut pas dépasser `drop + 20` niveaux ;
- 20 upgrades = +80 % de la base, pas ×4,6 ;
- caps d'effets respectés (6 Rempart ne donne pas 60 % DR) ;
- VIT au-delà de 4,0 n'améliore plus l'intervalle ;
- migration : les anciens objets gardent leurs stats.

**Critère** : aucun build ne dépasse 2× un autre en TTK/TTD ; aucun upgrade ne permet de one-shot un gardien du même palier.

---

### Lot 3 — Camp, sets, or

**Statut : livré en 0.20.3.** Camp et sets en pourcentage, coût indexé, plafond 30. Les sinks « reroll de gardien » et « potions au camp » restent liés aux lots 4–5.

**Objectif** : rendre l'or et les sets utiles au palier 100.

**Modifications** :

- Camp : +1,5 % ATQ, +1,5 % PV, +1 % VIT par niveau, appliqués à la **base héros**, plafond 30 niveaux.
- Coût : `ceil(45 × 1,18^niveau × (1 + 0,04 × palier) / 5) × 5`.
- Sets : bonus en pourcentage (2/4/6 = +4/8/12 %), plus de valeurs plates.
- Ajouter des sinks d'or : reroll de modificateur de gardien, potions au camp, respec de camp.
- Vérifier que le camp ne devient pas obligatoire : un joueur sans or doit pouvoir progresser par le stuff.

**Tests** :

- or dépensé régulièrement sur 1 → 100 ;
- camp 30 niveaux = puissance significative mais < 50 % du stuff ;
- sets 6/6 utiles mais pas obligatoires ;
- aucun set ne domine tous les autres.

---

### Lot 4 — Rations, attrition, récupération

**Statut : livré en 0.20.4.** Cap 30, revenus renforcés, auto-ration 40/45, farm 10 %, repos gratuit 2×20 %, farm de gardien +1/+1.

**Objectif** : garder l'attrition sans famine.

**Modifications** :

- `RATION_CAP = 30`.
- Revenus : gardien +3, coffre +3, monstre **20 %**, élite +1 garanti.
- Auto-ration : seuil **40 %**, soin **45 %**, une fois par combat.
- Farm : **+10 % PV** par kill, journalisé et affiché.
- Camp : **repos gratuit** 20 % PV, 2 fois/jour ; repos payant 1 ration +40 % ; soin complet 1 éclat.
- `holdFloor` : gardien farmé donne **+1 éclat et +1 ration**, sans avancer le palier.
- Objectif : 15–20 rations/jour pour 5 paliers/jour.

**Tests** :

- simulation : consommation ≤ revenus sur 5 paliers/jour ;
- depuis 0 ration / 0 éclat, récupération possible en ≤ 3 jours ;
- aucune situation où le joueur ne peut ni farmer, ni camper, ni attendre ;
- l'auto-ration ne se déclenche pas si elle soigne moins de 10 % (évite le gaspillage).

**Critère** : pas de famine ; le farm est une option, pas une obligation permanente.

---

### Lot 5 — Gardiens, pitié, portail

**Statut : livré en 0.20.5.** Niveau +1, bouclier 35 %/3 s/8 s, phase 2 télégraphiée, enrage 25 s, pitié −5 %/défaite, portail à 90 m.

**Objectif** : supprimer le seuil 0/100 et rendre le combat lisible.

**Modifications** :

- `makeBoss` : niveau **palier + 1**.
- Courbe : PV pour **18 s** de TTK avec le stuff attendu ; ATQ calibrée sur l'EHP avec un diviseur **20–22**.
- Bouclier : **35 % / 3 s / 8 s**.
- Phase 2 : capacité **télégraphiée** à 50 % PV, sans passage permanent à 950 ms.
- Enrage : après **25 s**, +15 % ATQ / 5 s, cap **+60 %**.
- Pitié : **−5 % PV/ATQ par défaite**, cap **−25 %**, affichée avant le combat.
- Portail à **90 m** : arrêt, écran de gardien, pronostic, choix **Engager / Farmer / Camper**, potion utilisable avant.
- Afficher le modificateur et la capacité avant l'engagement.
- Prévoir un reroll de modificateur au camp (or) si le tirage est injuste.

**Tests** :

- simulateur : `reference` entre **35 et 65 %** aux paliers 36, 50, 75, 100 ;
- `reference_high` entre **85 et 95 %** ;
- `under` entre **10 et 25 %** ;
- aucun gardien gagné en < 10 s ou < 30 coups portés ;
- pitié : après 3 défaites, le taux de victoire progresse visiblement sans devenir automatique ;
- portail : aucune attaque ne démarre avant le choix du joueur.

**Critère** : plus aucun palier à 0 % ou 100 % pour le stuff attendu.

---

### Lot 6 — Farm et `holdFloor`

**Statut : livré en 0.20.6.** Farm +10 % PV avec badge, farm de gardien +1/+1 plafonné à 5/jour.

**Objectif** : faire du farm un vrai filet de sécurité.

**Modifications** :

- `holdFloor` : +1 éclat, +1 ration par gardien farmé.
- Farm normal : +10 % PV par kill.
- Afficher « Farm : récupération » dans l'écran de farm, avec le gain de PV.
- Limiter éventuellement les récompenses de farm à 3–5 gardiens/jour pour éviter l'exploit.

**Tests** :

- farmer 3 gardiens donne des ressources ;
- farmer ne fait pas avancer le palier ;
- le farm reste plus sûr que le sentier normal ;
- pas d'exploit infini (cap quotidien si nécessaire).

---

### Lot 7 — Pity de butin, fusion, identification

**Statut : livré en 0.20.7.** Rare/5 kills hors farm, set/10 gardiens, 1 non identifié max/10, fusion 90 %.

**Objectif** : réduire les séries sèches.

**Modifications** :

- Rare garanti tous les **5 kills** de monstre (compteur de pity).
- Pièce de set garantie tous les **10 gardiens** (ou jeton de set).
- Non identifié : maximum **1 drop sur 10**.
- Fusion : **90 %** de la somme, en conservant effet et set.
- Éventuellement : « poussière de relique » pour choisir une pièce manquante.

**Tests** :

- 50 kills sans rare impossible (au moins 10 rares garantis) ;
- 10 gardiens sans set impossible ;
- fusion 90 % > fusion 75 % en stats attendues ;
- pas d'inflation de sets légendaires.

---

### Lot 8 — Biomes et modificateurs

**Objectif** : éviter les pics injustes.

**Modifications** :

- Budget de menace par palier : somme des multiplicateurs biome + modificateur + capacité plafonnée.
- Afficher le modificateur et la capacité avant le combat.
- Interdire les combinaisons « RAPIDE + capacité lourde » sur un même gardien, ou les compenser par moins de PV.
- Vérifier chaque biome aux paliers 10, 30, 50, 70, 90.

**Tests** :

- aucun biome ne dépasse 1,3× la difficulté moyenne ;
- aucun modificateur ne crée un one-shot non télégraphié ;
- le stuff attendu garde 35–65 % dans tous les biomes.

---

### Lot 9 — Fin de jeu et télémétrie

**Objectif** : donner une raison de finir 81–100.

**Modifications** :

- « Trône brisé » après 80 : modificateurs, événements, variantes de gardiens.
- Ou NG+ avec conservation des collections.
- Télémétrie locale (sans réseau) :
  - tentatives/palier, win rate, TTK/TTD ;
  - rations/éclats gagnés et dépensés ;
  - temps par niveau, kills par niveau ;
  - export `balance_report.json` ou écran debug.
- Tableau de bord debug : puissance recommandée, pronostic, DPS/EHP estimés.

**Tests** :

- aucun crash sur 1 → 100 simulé ;
- les métriques sont cohérentes avec le simulateur ;
- le joueur peut lire sa progression.

---

## 5. Portes de validation

| Porte | Contenu | Critère |
|---|---|---|
| **A** | Constantes figées, simulateur synchronisé | code ↔ sim à ±1 % |
| **B** | XP + objets + caps | 2 500–4 000 kills, aucun build > 2× |
| **C** | Rations + farm + camp | aucune famine sur 5 paliers/jour |
| **D** | Gardiens + pitié + portail | bandes 35–65 / 10–25 / 85–95 |
| **E** | Playtest 7 jours Phase A | +5 paliers ou blocage expliqué par un choix |
| **F** | Playtest 14 jours Phase B | 1 → 100 en 1–3 mois, pas de dead-end |

## 6. Tests et commandes

```bash
# Formules et courbes
python tools/equilibrage/balance_sim.py --curve target \
  --build player36,reference,reference_low,reference_high,under,tank,crit,lifesteal,endgame \
  --floors 36,50,75,100 --runs 500

# XP et économie
python tools/equilibrage/balance_sim.py --xp
python tools/equilibrage/balance_sim.py --economy

# Tests Android
./gradlew :app:testGithubDebugUnitTest
```

Tests unitaires à ajouter :

- `xpNext` et total 1 → 100 ;
- `floorScale` et monotonie du butin ;
- plafond d'upgrade ;
- caps VIT/effets ;
- pitié de gardien ;
- rations : consommation vs revenus ;
- `holdFloor` : récompenses ;
- migration v4 → v7.

## 7. Risques et rollback

| Risque | Mitigation |
|---|---|
| Inflation de butin | cap ×6, test 500 runs, playtest |
| Upgrade trop fort | plafond +20, croissance additive |
| Rations trop généreuses | cible 15–20/jour, ajuster les drops |
| Gardien trop facile | bande 85–95 % max pour le stuff avancé |
| Anciennes saves | aucun recalcul d'objet ; tests de migration |
| Doc/sim désynchronisés | porte A obligatoire avant tout tuning |
| Playtest biaisé | journal quotidien + métriques locales |

**Rollback** : toutes les constantes du lot 0 sont centralisées ; revenir à la courbe 0.20.0 = remplacer la table de constantes, sans migration de sauvegarde.

## 8. Ce qui n'est pas dans ce plan

- Refonte graphique, audio, interface hors indicateurs.
- Nouveaux biomes ou nouveaux systèmes de jeu.
- Monétisation, publicité, comptes.
- Migration Room.
- Rééquilibrage des Garmin/Santé Connect.

## 9. Prochaine étape

1. ~~Valider les sept décisions du §2.~~ **Fait le 24/11/2026.**
2. ~~Exécuter le lot 0 (constantes + simulateur + doc).~~ **Socle livré : `Balance.kt`, miroir Python, check CI, tests.**
3. ~~Lot 1 — courbe d'XP.~~ **Livré en 0.20.1.**
4. ~~Lot 2 — objets, upgrades, butin, caps.~~ **Livré en 0.20.2.**
5. ~~Lot 3 — camp, sets, or.~~ **Livré en 0.20.3.**
6. ~~Lot 4 — rations, attrition.~~ **Livré en 0.20.4.**
7. ~~Lot 5 — gardiens, pitié, portail.~~ **Livré en 0.20.5.**
8. ~~Lot 6 — farm et `holdFloor`.~~ **Livré en 0.20.6.**
9. ~~Lot 7 — pity de butin, fusion, identification.~~ **Livré en 0.20.7.**
10. Dérouler le lot 8 (biomes et modificateurs).
4. Calibrer les gardiens (lot 5) seulement après.
5. Playtest 7 jours, puis 14 jours.

**Lots 0 à 7 livrés. En attente du go pour le lot 8.**
