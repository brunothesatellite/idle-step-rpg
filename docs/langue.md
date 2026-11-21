# Internationalisation — étude

> État analysé : **v0.17.5**, 21/11/2026. Document de décision, aucun code écrit.
> Périmètre : application Android + manuel (embarqué et web). Hors périmètre : démo web
> racine, fiche Play, captures Play, vidéo promo, README et docs internes.

## 1. Résumé exécutif

- L’application est **intégralement en français dans le code** : une seule chaîne ressource
  (`app_name`), **559 littéraux accentués**, environ **700 à 900 chaînes** visibles, plus le
  contenu de jeu (objets, créatures, biomes, journal).
- Le manuel représente **8 433 mots** et **36 captures** dont l’interface est en français.
- Objectif réaliste : **FR + EN**, avec une architecture prête pour d’autres langues.
- Effort estimé de mon côté : **60 à 80 h** pour l’ensemble (app + manuel + captures),
  dont **35 à 45 h** pour un build anglais utilisable sans le manuel traduit.
- Le levier de non-régression : **garder `values/` en français comme défaut**, ajouter
  `values-en/`, et verrouiller le français par des captures dorées et des tests de ressources.
- Le risque principal n’est pas la traduction mais **les débordements de texte** dans une UI
  pixel art à tailles fixes, et **les 36 captures** du manuel à refaire.

## 2. Périmètre

| Inclus | Détail |
|---|---|
| Application | Compose, notifications, widget, écran manuel (WebView) |
| Contenu de jeu | noms d’objets, créatures, biomes, ensembles, effets, journal |
| Manuel | `manuel/index.html` + copie `android/app/src/main/assets/manuel/` |
| Formats | nombres, dates, pluriels, typographie française |

| Exclu (à traiter séparément) | Raison |
|---|---|
| `index.html` racine (démo web) | maquette séparée, pas l’app |
| Fiche Play, captures Play, vidéo | localisation marketing |
| README / docs | documentation interne |

## 3. État des lieux

| Élément | Mesure |
|---|---|
| Fichiers de ressources | **1** (`res/values/strings.xml`) avec `app_name` seulement |
| Littéraux contenant des accents | **559** dans 16 fichiers Kotlin |
| Appels `Text(` | **430** |
| Littéraux texte (≥ 3 caractères) | ~3 100, dont une part de clés, chemins, identifiants |
| Chaînes réellement traduisibles | **700 à 900** (estimation) |
| `engine/Model.kt` | 130 littéraux : objets, créatures, biomes, effets, ensembles |
| `engine/GameEngine.kt` | 56 : journal, événements, messages système |
| `ui/Overlays.kt` / `Settings.kt` | 103 / 56 : UI |
| `GameViewModel.kt` | 40 : toasts, états |
| Notifications / widget / pas | 7 + textes widget + statuts de synchronisation |
| Manuel | **8 433 mots**, **36 images**, 77 Ko HTML |
| Journal | `JournalEntry.text` = **chaîne figée en base** |
| Objets | `Item.name` = **chaîne figée en base** |
| Locale | aucun `localeConfig`, aucun sélecteur, aucune ressource `values-*` |
| minSdk | **34** → langue par application disponible (API 33+) |

### Conséquences directes

1. **Le journal et les noms d’objets déjà en sauvegarde ne se traduiront pas** sans migration.
   Un objet « Lame ancienne » restera français dans un inventaire anglais.
2. Le manuel est dupliqué ; toute traduction doit être appliquée aux deux copies.
3. Les captures du manuel montrent l’UI française : elles ne sont pas traduisibles.

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
| Aucune migration de sauvegarde | les sauvegardes existantes restent lisibles à l’identique |
| Manuel : `/manuel/` inchangé, `/manuel/en/` ajouté | l’URL française ne bouge pas |

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
- **Catalogues** (rareté, emplacement, effet, ensemble, biome, titre, créature) → clés/enums,
  résolus dans l’UI. C’est la partie la plus sûre à extraire.
- **Journal** → événements structurés (`key` + arguments) pour les **nouveaux** événements,
  avec fallback sur le texte stocké pour l’historique. Migration non destructive.
- **Messages temporaires** (toasts, rapports) → clés résolues côté UI.

### 5.3 Contenu et sauvegardes

| Donnée | Décision proposée |
|---|---|
| `JournalEntry.text` existant | conservé tel quel (archive historique) |
| Nouveaux événements de journal | clé + paramètres, rendus dans la langue active |
| `Item.name` existant | conservé (pas de renommage rétroactif) |
| Nouveaux objets | nom localisé à la création |
| Noms propres (Kaelen, Vael) | inchangés |
| Noms descriptifs (biomes, créatures, objets) | traduits |

Conséquence assumée : un inventaire existant peut mélanger FR et EN après bascule. Pour
l’éviter, il faudrait stocker une clé de base par objet (sauvegarde v5 + migration), estimé
**+8 à 12 h**. Non recommandé pour la première livraison.

### 5.4 Manuel

- Structure : `assets/manuel/index.html` (FR), `assets/manuel/en/index.html`, images par
  langue (`images/fr/…`, `images/en/…`).
- `ManualActivity` choisit le chemin selon la locale de l’app, avec repli FR.
- Web : `manuel/` reste FR, `manuel/en/` est ajouté, avec un sélecteur de langue en haut.
- Les ancres (`#absence`, `#reglages`) doivent rester identiques entre langues.

### 5.5 Sélecteur de langue

- Réglages → « Langue » : **Système / Français / English**.
- `android:localeConfig` + `LocaleManager` (API 33+, minSdk 34).
- Défaut « Système » : zéro changement pour l’existant.
- Le manuel suit la langue de l’app.

## 6. Découpage en étapes (pour dérisquer)

### Étape 0 — Inventaire et garde-fous (2–3 h)
- Geler un jeu de captures FR de référence (écrans clés).
- Ajouter `lint` MissingTranslation et un test de couverture des clés.
- Lister les chaînes par écran avec captures d’écran.
**Sortie** : baseline FR + filet de sécurité. Aucun changement visible.

### Étape 1 — Infrastructure invisible (4–6 h)
- `values-en/` vide, `localeConfig`, sélecteur « Système ».
- Mécanisme de résolution des clés côté UI, sans migrer les textes.
- Test : l’app FR est pixel-identique.
**Sortie** : socle prêt, zéro régression.

### Étape 2 — Extraction de l’UI (12–16 h)
- Écran par écran : Réglages, Dashboard, Overlays, Codex, Scène, Onboarding.
- Remplacement des littéraux par des ressources ; extraction des pluriels.
- Corrections de mise en page au fil de l’eau.
**Sortie** : app FR identique, toutes les chaînes UI en ressources.

### Étape 3 — Moteur et contenu (10–14 h)
- Clés pour rareté/emplacement/effet/ensemble/biome/titre/créature.
- Journal : structure de clé + fallback texte.
- Noms d’objets/consommables localisés à la création.
**Sortie** : plus aucun texte de gameplay en dur.

### Étape 4 — Traduction anglaise (10–14 h)
- UI (700–900 chaînes), contenu (objets, créatures, biomes, journal).
- Relecture de cohérence (ton, lore, majuscules, longueurs).
**Sortie** : build EN complet, FR intact.

### Étape 5 — Manuel anglais (14–20 h)
- Traduction des 8 433 mots (6–8 h).
- Structure multi-langue + sélecteur (2–3 h).
- **36 captures à refaire en anglais** (6–9 h de travail sur appareil).
**Sortie** : manuel EN en ligne et embarqué.

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
| 3 — Moteur/contenu | 10–14 | 1 h | décisions journal/objets |
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
- **Manuel** : vérification des 36 images, des ancres, du sélecteur et du repli FR.
- **Appareil** : passe FR puis EN (réglages, inventaire, combat, absence, manuel).
- **Sauvegardes** : ouverture d’une sauvegarde v4 existante en FR et en EN, sans migration.

## 9. Risques et parades

| Risque | Impact | Parade |
|---|---|---|
| Débordements de texte (tailles fixes, `maxLines`) | UI cassée | pseudo-locale, captures, marges |
| Journal/objets figés en FR | incohérence | politique assumée + clés pour le futur |
| 36 captures à refaire | délai | prioriser les captures avec texte |
| Typographie française | qualité | règles de traduction dédiées |
| Polices pour d’autres langues | blocs manquants | vérifier Press Start 2P (latin), prévoir une police pour Cyrillic/CJK |
| Traduction du lore | qualité | relecture humaine obligatoire |
| Régression FR | critique | défaut FR + golden screenshots + CI |

## 10. Décisions à prendre

1. **Défaut** : garder le français comme langue par défaut, ou basculer en anglais après validation ?
2. **Contenu** : traduire les noms descriptifs et garder les noms propres ?
3. **Sauvegardes** : accepter le mélange FR/EN dans les inventaires existants, ou payer la
   migration par clés (+8–12 h) ?
4. **Manuel** : captures EN refaites pour les 36 images, ou seulement celles qui contiennent du texte ?
5. **Sélecteur** : « Système / FR / EN » ou uniquement « Système » pour la première version ?

## 11. Hors périmètre

- Démo web racine (`index.html`) : maquette séparée, à traiter si elle doit être bilingue.
- Fiche Play, captures Play, vidéo promo : localisation marketing distincte.
- Nouveaux alphabets (arabe, cyrillique, CJK) : architecture prête, mais polices et tests dédiés.
