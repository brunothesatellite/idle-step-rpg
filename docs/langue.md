# Internationalisation — étude

> État analysé : **v0.18.1**, 23/11/2026. Document de décision, aucun code écrit.
> Périmètre : application Android + manuel (embarqué et web). Hors périmètre : démo web
> racine, fiche Play, captures Play, vidéo promo, README et docs internes.

## 1. Résumé exécutif

- L’application est **intégralement en français dans le code** : une seule chaîne ressource
  (`app_name`), **734 littéraux accentués**, environ **750 à 950 chaînes** visibles, plus le
  contenu de jeu (objets, créatures, biomes, journal).
- Le Codex compte désormais **110 créatures et 35 gardiens**, avec de nouveaux libellés
  (`NON DÉCOUVERT`, `GARDIEN`, `GARDIENS DE JALON`, `Première rencontre`, tiers de spawn).
- Le manuel représente **≈ 7 400 mots** et **36 captures** dont l’interface est en français.
  Il est maintenant **généré depuis `manuel/index.template.html`** (compteurs et version injectés
  au build), et non plus maintenu en double à la main.
- Objectif réaliste : **FR + EN**, avec une architecture prête pour d’autres langues.
- Effort estimé de mon côté : **66 à 95 h** pour l’ensemble (app + manuel + captures +
  identifiants de créatures), dont **42 à 59 h** pour un build anglais utilisable sans le manuel.
- Le levier de non-régression : **garder `values/` en français comme défaut**, ajouter
  `values-en/`, et verrouiller le français par des captures dorées et des tests de ressources.
- Le risque principal n’est pas la traduction mais **les débordements de texte** dans une UI
  pixel art à tailles fixes, **les clés de bestiaire figées en français**, et **les 36 captures**
  du manuel à refaire.

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
| Fichiers de ressources | **1** (`res/values/strings.xml`) avec `app_name` seulement |
| Littéraux contenant des accents | **734** dans 31 fichiers Kotlin |
| Appels `Text(` | **401** |
| Littéraux texte | ~3 400, dont une part de clés, chemins, identifiants |
| Chaînes réellement traduisibles | **750 à 950** (estimation) |
| `engine/Model.kt` | 154 littéraux accentués : objets, créatures, biomes, effets, ensembles, renommages |
| `engine/GameEngine.kt` | 94 : journal, événements, messages système |
| `ui/Overlays.kt` / `Settings.kt` | 140 / 61 : UI |
| `ui/Codex.kt` / `Dashboard.kt` | 56 / 52 : Codex, cartes, fiches |
| `GameViewModel.kt` | 40 : toasts, états |
| Notifications / widget / pas | 7 + textes widget + statuts de synchronisation |
| Manuel | **≈ 7 400 mots**, **36 images**, 77 Ko HTML, généré depuis un template |
| Journal | `JournalEntry.text` = **chaîne figée en base** |
| Objets | `Item.name` = **chaîne figée en base** |
| Bestiaire / gardiens | `bestiary`, `bossKills`, `firstSeen`, `bossFirstSeen` = **clés en noms français** |
| Locale | aucun `localeConfig`, aucun sélecteur, aucune ressource `values-*` |
| minSdk | **34** → langue par application disponible (API 33+) |

### Conséquences directes

1. **Le journal et les noms d’objets déjà en sauvegarde ne se traduiront pas** sans migration.
   Un objet « Lame ancienne » restera français dans un inventaire anglais.
2. **Le bestiaire et les gardiens sont keyés par leur nom français** (`bestiary`, `bossKills`,
   `firstSeen`, `bossFirstSeen`). Traduire « Chauve-souris des neiges » en anglais ne mettra pas
   à jour les compteurs : il faut des identifiants stables (`bat_neiges`) ou une table de
   correspondance. Les 110 créatures et 35 gardiens sont concernés.
3. Le manuel n’est plus maintenu en double : il est généré depuis `manuel/index.template.html`.
   Toute traduction doit passer par un template EN (ou un générateur bilingue), pas par une
   édition manuelle des fichiers générés.
4. Les captures du manuel montrent l’UI française : elles ne sont pas traduisibles.
5. La sauvegarde est en **v6** : la migration des anciens noms de monstres/gardiens est déjà
   jouée une fois. Une traduction qui change les clés de bestiaire demandera une **v7**.

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
| Aucune migration supplémentaire pour la traduction | la migration v6 existante reste jouable en FR et en EN |
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
| `bestiary` / `bossKills` existants | clés en noms français : à migrer vers un **identifiant de créature** stable (`bat_neiges`, `cerebov`) |
| `firstSeen` / `bossFirstSeen` | même identifiant que le bestiaire ; `biomeKey` et `tier` sont déjà stables |
| `MOB_RENAMES` | table de migration v5 → v6 à garder ; ne remplace pas un système d’identifiants |
| Noms propres (Kaelen, Vael) | inchangés |
| Noms descriptifs (biomes, créatures, objets) | traduits |

Conséquence assumée : un inventaire ou un bestiaire existant peut mélanger FR et EN après
bascule. Pour l’éviter, il faudrait stocker une clé de base par objet **et par créature**
(sauvegarde v7 + migration). Estimation : **+8 à 12 h** pour les objets, **+4 à 6 h** pour les
110 créatures et 35 gardiens (renommage des clés, table de correspondance, tests). Non
recommandé pour la première livraison, mais à décider avant de traduire les noms de créatures.

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

### Étape 3 — Moteur et contenu (14–20 h)
- Clés pour rareté/emplacement/effet/ensemble/biome/titre/créature/capacité de gardien.
- **Identifiants stables de créatures** (110 monstres + 35 gardiens) pour `bestiary`,
  `bossKills`, `firstSeen`, `bossFirstSeen`, avec migration des clés françaises.
- Journal : structure de clé + fallback texte.
- Noms d’objets/consommables localisés à la création.
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
| 3 — Moteur/contenu | 14–20 | 1,5 h | décisions journal/objets/identifiants de créatures |
| 4 — Traduction EN | 10–14 | **4–6 h** | relecture indispensable |
| 5 — Manuel EN | 14–20 | **3–5 h** | captures + validation |
| 6 — QA | 8–12 | 2 h | tests sur ton téléphone |
| 7 — Doc/bascule | 2–4 | 1 h | décision finale |
| **Total** | **66–95 h** | **12,5–17,5 h** | ~9–12 jours de dev |

Jalon intermédiaire : **étapes 0–4 = 42–59 h** pour une app anglaise complète sans manuel.

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
- **Sauvegardes** : ouverture d’une sauvegarde v4/v5 en FR et en EN ; la migration v6
  (`MOB_RENAMES`, nettoyage des biomes non découverts, `firstSeen`) doit être identique dans
  les deux langues. Une future migration d’identifiants de créatures sera testée en v7.

## 9. Risques et parades

| Risque | Impact | Parade |
|---|---|---|
| Débordements de texte (tailles fixes, `maxLines`) | UI cassée | pseudo-locale, captures, marges |
| Journal/objets figés en FR | incohérence | politique assumée + clés pour le futur |
| Bestiaire/gardiens keyés en noms FR | compteurs faux après traduction | identifiants stables de créatures + migration v7 |
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
   migration par clés (+8–12 h pour les objets, +4–6 h pour les 110 créatures / 35 gardiens) ?
4. **Bestiaire** : garder les clés en noms français (traduction impossible sans migration) ou
   introduire des identifiants stables (`bat_neiges`, `cerebov`) en sauvegarde v7 ?
5. **Manuel** : captures EN refaites pour les 36 images, ou seulement celles qui contiennent du texte ?
6. **Manuel généré** : un template par langue (`index.template.html`, `index.en.template.html`)
   ou un template unique avec libellés localisés injectés au build ?
7. **Sélecteur** : « Système / FR / EN » ou uniquement « Système » pour la première version ?

## 11. Hors périmètre

- Démo web racine (`index.html`) : maquette séparée, à traiter si elle doit être bilingue.
- Fiche Play, captures Play, vidéo promo : localisation marketing distincte.
- Nouveaux alphabets (arabe, cyrillique, CJK) : architecture prête, mais polices et tests dédiés.
