# Internationalisation — étude

> État analysé : **v0.18.1**, branche **`evol-langue`**, sauvegarde **v7**, 23/11/2026.
> Périmètre : application Android + manuel (embarqué et web). Hors périmètre : démo web
> racine, fiche Play, captures Play, vidéo promo, README et docs internes.

## 1. Résumé exécutif

- L’application est **principalement en français dans le code** : **586 littéraux accentués**
  restants, dont **≈ 143 clés de correspondance** français → ressource (catalogue de contenu et
  traducteur de journal), environ **750 à 950 chaînes** visibles, plus le contenu de jeu.
  Les écrans Réglages, Onboarding, Journal, Dashboard, Codex, Overlays et les contenus
  dynamiques (objets, créatures, effets, journal) sont déjà extraits en ressources FR/EN.
- Le Codex compte désormais **110 créatures et 35 gardiens**, avec de nouveaux libellés
  (`NON DÉCOUVERT`, `GARDIEN`, `GARDIENS DE JALON`, `Première rencontre`, tiers de spawn).
- Le manuel représente **≈ 7 400 mots** et **36 captures** dont l’interface est en français.
  Il est maintenant **généré depuis `manuel/index.template.html`** (compteurs et version injectés
  au build), et non plus maintenu en double à la main.
- Objectif réaliste : **FR + EN**, avec une architecture prête pour d’autres langues.
- Effort estimé de mon côté : **62 à 89 h** pour l’ensemble (app + manuel + captures),
  dont **38 à 53 h** pour un build anglais utilisable sans le manuel.
- Le levier de non-régression : **garder `values/` en français comme défaut**, ajouter
  `values-en/`, et verrouiller le français par des captures dorées et des tests de ressources.
- Le risque principal n’est pas la traduction mais **les débordements de texte** dans une UI
  pixel art à tailles fixes, **les clés de bestiaire figées en français**, et **les 36 captures**
  du manuel à refaire.

### État de la branche `evol-langue`

**Fait**
- `values-en/`, `locales_config.xml`, `android:localeConfig`.
- Sélecteur **Système / Français / English** dans Réglages (`LocaleManager`).
- En-tête des Réglages et carte Langue extraits en ressources.
- **Écran Réglages extrait** (Affichage, Notifications, Son, Mises à jour, Aide, Inventaire,
  Sauvegarde, Source de pas, Diagnostic, Réglages avancés, À propos, dialogues).
- Onboarding entièrement extrait et traduit (4 pages + boutons + étape).
- **Journal extrait** : titre, compteurs, onglets, filtres, messages vides et types d’entrée.
- **Dashboard entièrement extrait** : en-tête, onglets, Puissance/Rang, ATQ/PV/VIT, ressources,
  biome et règle de farm, Codex, Ferveur et paliers, Serments, Exploits, Journal, Inventaire
  (emplacements, raretés, ensembles, tri, filtres, coffre de camp), consommables, Améliorations
  (noms, descriptions, gardien et capacité) et bouton de synchronisation. Le séparateur décimal
  suit la langue (virgule en FR, point en EN).
- **ManualActivity** : titre extrait.
- **Contenus dynamiques traduits à l’affichage** : 110 créatures, 34 gardiens, 54 objets,
  16 consommables (nom + description), 6 effets (libellé + description), 8 biomes, 12 ensembles,
  10 titres de rang, 5 rencontres spéciales, 3 coffres, 7 modificateurs et 8 buffs. Les noms
  restent figés en français dans les sauvegardes ; le catalogue les traduit sans migration.
- **Overlays extraits** : fiche d’objet et comparateur, forge (amélioration, reforger, démonter),
  coffre, camp, compte Garmin, absence/rattrapage, import de sauvegarde, rencontre spéciale,
  sac de consommables, résultat de rencontre.
- **Codex extrait** : en-tête et onglets, cartes Bestiaire/Ensembles/Biomes, fiches créature et
  gardien, capacités et descriptions, progression des ensembles, sources de butin.
- **Journal et résultats traduits** : 45 formats de messages (combat, jalons, équipement,
  forge, serments, consommables, rencontres). Les anciennes entrées déjà en sauvegarde sont
  traduites à l’affichage ; le français est renvoyé tel quel.
- Migration **v7** : `bestiary`, `bossKills`, `firstSeen` et `bossFirstSeen` utilisent des
  identifiants stables (`bat_neiges`, `cerebov`). Les anciennes clés inconnues sont conservées.
- 117 tests, lint OK, debug migré et vérifié sur appareil.

**Reste**
- Réglages : chaînes dynamiques restantes (statuts de source, journal de synchronisation,
  libellés de `StepSourceType`).
- Scène : textes d’interface du HUD, du camp latéral, des tags de combat et des bannières.
- Notifications et toasts : textes système encore en français.
- Manuel EN et captures anglaises.

## 2. Périmètre

| Inclus | Détail |
|---|---|
| Application | Compose, notifications, widget, écran manuel (WebView) |
| Contenu de jeu | noms d’objets, créatures, biomes, ensembles, effets, journal |
| Manuel | `manuel/index.template.html` → `manuel/index.html` + copie `android/app/src/main/assets/manuel/` |
| Formats | nombres, dates, pluriels (`victime(s)`, `créature(s)`), typographie française |

| Exclu (à traiter séparément) | Raison |
|---|---|
| `index.html` racine (démo web) | maquette séparée, pas l’app |
| Fiche Play, captures Play, vidéo | localisation marketing |
| README / docs | documentation interne |

## 3. État des lieux

| Élément | Mesure |
|---|---|
| Fichiers de ressources | `res/values/strings.xml` + `res/values-en/strings.xml` |
| Littéraux contenant des accents | **586** dans 34 fichiers Kotlin, dont ≈ 143 clés de correspondance |
| Appels `Text(` | **403** |
| Littéraux texte | ~3 400, dont une part de clés, chemins, identifiants |
| Chaînes réellement traduisibles | **750 à 950** (estimation) |
| `engine/Model.kt` | 154 littéraux accentués : objets, créatures, biomes, effets, ensembles, renommages |
| `engine/GameEngine.kt` | 94 : journal, événements, messages système |
| `ui/Overlays.kt` / `Settings.kt` | 12 / 0 : overlays et Réglages extraits |
| `ui/Codex.kt` / `Dashboard.kt` | 11 / 7 : Codex, cartes, fiches (Dashboard extrait) |
| `ui/ContentCatalog.kt` / `ContentText.kt` | 105 clés : catalogue FR→ressource, traducteur de journal |
| `GameViewModel.kt` | 40 : toasts, états |
| Notifications / widget / pas | 7 + textes widget + statuts de synchronisation |
| Manuel | **≈ 7 400 mots**, **36 images**, 77 Ko HTML, généré depuis un template |
| Journal | `JournalEntry.text` = chaîne figée en base, traduite à l’affichage |
| Objets | `Item.name` = chaîne figée en base, traduite à l’affichage via son nom de base |
| Bestiaire / gardiens | **clés stables depuis la v7** (`bat_neiges`, `cerebov`) ; anciennes clés legacy conservées |
| Locale | aucun `localeConfig`, aucun sélecteur, aucune ressource `values-*` |
| minSdk | **34** → langue par application disponible (API 33+) |

### Conséquences directes

1. **Le journal et les noms d’objets déjà en sauvegarde sont traduits à l’affichage**, sans
   migration : le catalogue `ContentCatalog.kt` mappe les noms français vers les ressources et
   `ContentText.kt` reconnaît les formats de messages du journal. Un nom inconnu du catalogue
   reste affiché en français plutôt que d’être perdu.
2. Le bestiaire et les gardiens utilisent des **identifiants stables** depuis la v7
   (`bestiary`, `bossKills`, `firstSeen`, `bossFirstSeen`). Traduire les noms affichés ne touche
   plus les compteurs.
3. Le manuel n’est plus maintenu en double : il est généré depuis `manuel/index.template.html`.
   Toute traduction doit passer par un template EN (ou un générateur bilingue), pas par une
   édition manuelle des fichiers générés.
4. Les captures du manuel montrent l’UI française : elles ne sont pas traduisibles.
5. La sauvegarde est en **v7** : la migration des anciens noms vers les identifiants est faite
   et testée. Les clés inconnues sont conservées pour éviter toute perte de données.

## 4. Stratégie de non-régression

Principe : **le français reste le comportement par défaut**, l’anglais est additif.

| Mesure | Effet |
|---|---|
| `values/` = français (défaut), ajout de `values-en/` | un appareil FR ne voit aucune différence |
| Langue de l’app = « Système » par défaut | aucun changement de comportement à l’installation |
| Captures dorées FR avant/après (pixel-diff) | prouve l’absence de régression visuelle |
| `lint` : `MissingTranslation` en erreur | empêche une release avec une clé manquante |
| Test unitaire de couverture des clés | toutes les langues ont les mêmes clés |
| Pseudo-locale `en-XA` | détecte les débordements avant la traduction réelle |
| Aucune migration supplémentaire pour la traduction | les migrations v6 et v7 existantes restent jouables en FR et en EN |
| Manuel : template FR inchangé, template EN + génération | l'URL française ne bouge pas |

Option à trancher plus tard : basculer le défaut en anglais. Cela change le fallback des
langues non supportées ; à ne faire qu’après une période de validation.

## 5. Architecture cible

### 5.1 Application

- Toutes les chaînes d’UI passent en ressources (`strings.xml`), une clé par texte.
- `values/` conserve le français ; `values-en/` reçoit l’anglais ; `values-de/`, etc. ensuite.
- Pluriels via `plurals` (anglais et français n’ont pas les mêmes règles).
- Nombres et dates : `NumberFormat` / `DateTimeFormatter` localisés (aujourd’hui formatage
  manuel avec espaces).
- Typographie : conserver les espaces français avant `: ; ! ?` dans les chaînes FR ;
  ne pas les appliquer en anglais.

### 5.2 Moteur de jeu

Le moteur ne doit pas dépendre d’`android.content.Context`. Trois options :

| Option | Principe | Coût | Risque |
|---|---|---|---|
| A. Clés dans le moteur | le moteur renvoie une clé + paramètres, l’UI résout | moyen | faible |
| B. Moteur FR + table de correspondance UI | traduction à l’affichage | faible au début | dérive, textes non couverts |
| C. Objets localisés partout | `LocalizedText` typé | élevé | refonte large |

**Recommandation** : hybride.
- **Catalogues** (rareté, emplacement, effet, ensemble, biome, titre, créature, capacité de
  gardien, tier de spawn, zone, libellés du Codex) → clés/enums, résolus dans l’UI. C’est la
  partie la plus sûre à extraire. `FirstSeen` stocke déjà `biomeKey` et `tier` comme identifiants
  stables (`crypts`, `easy`, `mid`, `hard`, `boss`, `farm`) : seuls les noms affichés restent à
  traduire.
- **Journal** → événements structurés (`key` + arguments) pour les **nouveaux** événements,
  avec fallback sur le texte stocké pour l’historique. Migration non destructive.
- **Messages temporaires** (toasts, rapports) → clés résolues côté UI.

### 5.3 Contenu et sauvegardes

| Donnée | Décision proposée |
|---|---|
| `JournalEntry.text` existant | conservé tel quel (archive historique) |
| Nouveaux événements de journal | clé + paramètres, rendus dans la langue active |
| `Item.name` existant | conservé (pas de renommage rétroactif) |
| Nouveaux objets | nom localisé à la création, avec identifiant stable |
| `bestiary` / `bossKills` existants | **migrés en v7** vers un identifiant stable (`bat_neiges`, `cerebov`) ; clés legacy inconnues conservées |
| `firstSeen` / `bossFirstSeen` | même identifiant que le bestiaire ; `biomeKey` et `tier` sont déjà stables |
| `MOB_RENAMES` | table de migration v5 → v6 conservée pour les anciennes sauvegardes |
| Noms propres (Kaelen, Vael) | inchangés |
| Noms descriptifs (biomes, créatures, objets) | traduits, avec un catalogue localisé pour les créatures |

Conséquence assumée : un inventaire existant peut mélanger FR et EN après bascule. Pour
l’éviter, il faudrait stocker une clé de base par objet (sauvegarde v8 + migration), estimé
**+8 à 12 h**. Les créatures sont déjà couvertes par la v7.

### 5.4 Manuel

- Aujourd’hui : `manuel/index.template.html` → `manuel/index.html` et
  `android/app/src/main/assets/manuel/index.html`, via la tâche Gradle `generateManual`
  (compteurs du Codex et version de l’application injectés depuis `Model.kt` / Gradle).
- Pour l’anglais : `manuel/index.en.template.html` → `manuel/index.en.html` et
  `assets/manuel/en/index.html`, avec les mêmes jetons mais des libellés EN
  (`110 creatures`, `35 guardians`, `11 sets`) et la version injectée.
- `ManualActivity` choisit le chemin selon la locale de l’app, avec repli FR.
- Web : `manuel/` reste FR, `manuel/en/` est ajouté, avec un sélecteur de langue en haut.
- Les ancres (`#absence`, `#reglages`) doivent rester identiques entre langues.
- **Ne jamais éditer les fichiers générés** : modifier le template, puis relancer
  `generateManual`. Un template bilingue unique est une alternative, mais il faudra alors
  injecter les libellés localisés et non plus seulement les nombres.

### 5.5 Sélecteur de langue

- Réglages → « Langue » : **Système / Français / English**.
- `android:localeConfig` + `LocaleManager` (API 33+, minSdk 34).
- Défaut « Système » : zéro changement pour l’existant.
- Le manuel suit la langue de l’app.

## 6. Découpage en étapes (pour dérisquer)

### Étape 0 — Inventaire et garde-fous (2–3 h)
- Geler un jeu de captures FR de référence (écrans clés, dont Codex : Bestiaire,
  `NON DÉCOUVERT`, chip `GARDIEN`, fiche de première rencontre, onglet Biomes).
- Ajouter `lint` MissingTranslation et un test de couverture des clés.
- Lister les chaînes par écran avec captures d’écran, et vérifier le manuel généré.
**Sortie** : baseline FR + filet de sécurité. Aucun changement visible.

### Étape 1 — Infrastructure invisible (4–6 h)
- `values-en/` vide, `localeConfig`, sélecteur « Système ».
- Mécanisme de résolution des clés côté UI, sans migrer les textes.
- Test : l’app FR est pixel-identique.
**Sortie** : socle prêt, zéro régression.

### Étape 2 — Extraction de l’UI (12–16 h)
- Écran par écran : Réglages, Dashboard, Overlays, Codex, Scène, Onboarding.
- Inclure les nouveaux libellés du Codex : `NON DÉCOUVERT`, `GARDIEN`,
  `GARDIENS DE JALON`, `Première rencontre`, tiers de spawn, `victime(s)`.
- Remplacement des littéraux par des ressources ; extraction des pluriels.
- Corrections de mise en page au fil de l’eau.
**Sortie** : app FR identique, toutes les chaînes UI en ressources.

### Étape 3 — Moteur et contenu (10–14 h)
- Clés pour rareté/emplacement/effet/ensemble/biome/titre/créature/capacité de gardien.
- **Identifiants stables de créatures** : fait en v7 (110 monstres + 35 gardiens).
- Journal : structure de clé + fallback texte.
- Noms d’objets/consommables/créatures localisés à la création.
**Sortie** : plus aucun texte de gameplay en dur, bestiaire traduisible.

### Étape 4 — Traduction anglaise (10–14 h)
- UI (750–950 chaînes), contenu (objets, créatures, biomes, journal).
- Relecture de cohérence (ton, lore, majuscules, longueurs).
**Sortie** : build EN complet, FR intact.

### Étape 5 — Manuel anglais (14–20 h)
- Traduction des **≈ 7 400 mots** (6–8 h).
- Création de `manuel/index.en.template.html`, génération des deux sorties EN et
  branchement de `ManualActivity` / du web (2–3 h). Ne pas éditer les fichiers générés.
- **36 captures à refaire en anglais** (6–9 h de travail sur appareil).
**Sortie** : manuel EN en ligne et embarqué, compteurs et version injectés.

### Étape 6 — QA non-régression (8–12 h)
- Pseudo-locale, débordements, tests FR/EN sur appareil.
- Diff des captures FR, vérification des liens et ancres.
- Tests unitaires + lint + CI.
**Sortie** : preuve de non-régression.

### Étape 7 — Documentation et bascule (2–4 h)
- `docs/langue.md` finalisé, README, manuel (section langue).
- Décision sur le défaut FR/EN et sur la Play Store.
**Sortie** : livraison.

## 7. Estimations

| Étape | Mes heures | Ton temps | Remarque |
|---|---|---|---|
| 0 — Inventaire | 2–3 | 0,5 h | validation des écrans |
| 1 — Infrastructure | 4–6 | 0 | invisible |
| 2 — Extraction UI | 12–16 | 1 h | arbitrages de formulation |
| 3 — Moteur/contenu | 10–14 | 1,5 h | décisions journal/objets/catalogues de créatures |
| 4 — Traduction EN | 10–14 | **4–6 h** | relecture indispensable |
| 5 — Manuel EN | 14–20 | **3–5 h** | captures + validation |
| 6 — QA | 8–12 | 2 h | tests sur ton téléphone |
| 7 — Doc/bascule | 2–4 | 1 h | décision finale |
| **Total** | **62–89 h** | **12–17 h** | ~8–11 jours de dev |

Jalon intermédiaire : **étapes 0–4 = 38–53 h** pour une app anglaise complète sans manuel.

## 8. Plan de test

- **Ressources** : test unitaire « chaque clé existe dans toutes les langues », pas de chaîne vide,
  pas de clé orpheline.
- **Lint** : `MissingTranslation`, `ExtraTranslation`, `Typos` si activé.
- **Pseudo-locale** : `en-XA` pour les débordements, avec captures des écrans critiques.
- **Captures dorées FR** : seuil zéro pixel de différence sur les écrans non touchés.
- **Tests UI existants** : `OverlayUiTest` à étendre aux réglages et au manuel.
- **Codex** : bestiaire 110/35, sections `NON DÉCOUVERT`, chip `GARDIEN`, fiche de première
  rencontre, noms de biomes verrouillés, absence de gardiens fantômes dans un biome fermé.
- **Manuel** : vérification des 36 images, des ancres, du sélecteur, du repli FR, et des
  compteurs/version injectés dans les deux langues.
- **Appareil** : passe FR puis EN (réglages, inventaire, combat, absence, Codex, manuel).
- **Sauvegardes** : ouverture d’une sauvegarde v4/v5/v6 en FR et en EN ; les migrations v6
  (`MOB_RENAMES`, nettoyage des biomes non découverts, `firstSeen`) et v7 (identifiants stables,
  conservation des clés legacy, backup `pre_migration_v6`) doivent être identiques dans les
  deux langues.

## 9. Risques et parades

| Risque | Impact | Parade |
|---|---|---|
| Débordements de texte (tailles fixes, `maxLines`) | UI cassée | pseudo-locale, captures, marges |
| Journal/objets figés en FR | incohérence | politique assumée + clés pour le futur |
| Noms de créatures encore en dur en FR | bestiaire anglais avec noms français | catalogue localisé (identifiants v7 déjà en place) |
| Fichiers de manuel générés édités à la main | divergence template/sortie | toujours modifier `*.template.html` puis régénérer |
| 36 captures à refaire | délai | prioriser les captures avec texte |
| Typographie française | qualité | règles de traduction dédiées |
| Polices pour d’autres langues | blocs manquants | vérifier Press Start 2P (latin), prévoir une police pour Cyrillic/CJK |
| Traduction du lore | qualité | relecture humaine obligatoire |
| Régression FR | critique | défaut FR + golden screenshots + CI |

## 10. Décisions à prendre

1. **Défaut** : garder le français comme langue par défaut, ou basculer en anglais après validation ?
2. **Contenu** : traduire les noms descriptifs et garder les noms propres ?
3. **Sauvegardes** : accepter le mélange FR/EN dans les inventaires existants, ou payer la
   migration par clés pour les objets (+8–12 h) ? Les 110 créatures / 35 gardiens sont déjà
   couverts par la v7.
4. **Bestiaire** : les identifiants stables sont en place (v7). Reste à décider si les noms de
   créatures sont traduits en anglais ou restent des noms propres français.
5. **Manuel** : captures EN refaites pour les 36 images, ou seulement celles qui contiennent du texte ?
6. **Manuel généré** : un template par langue (`index.template.html`, `index.en.template.html`)
   ou un template unique avec libellés localisés injectés au build ?
7. **Sélecteur** : « Système / FR / EN » ou uniquement « Système » pour la première version ?

## 11. Hors périmètre

- Démo web racine (`index.html`) : maquette séparée, à traiter si elle doit être bilingue.
- Fiche Play, captures Play, vidéo promo : localisation marketing distincte.
- Nouveaux alphabets (arabe, cyrillique, CJK) : architecture prête, mais polices et tests dédiés.
