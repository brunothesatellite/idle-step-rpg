# Internationalisation — état final

> État vérifié : **v0.18.1**, branche **`evol-langue`**, sauvegarde **v7**, 23/11/2026.
> Périmètre livré : application Android, manuel embarqué et web, kit Play Store, README.
> Hors périmètre : démo web racine, vidéo promo, nouveaux alphabets.

## 1. Résumé

- Le **français reste la langue par défaut** ; l’anglais est proposé via le sélecteur
  **Système / Français / English** et couvre désormais l’application, les contenus dynamiques,
  le manuel et le kit de publication.
- **Tous les écrans sont extraits**, y compris le dernier zone en dur : `ui/Update.kt`
  (mise à jour disponible, téléchargement, installation, notes Play Protect).
- **333 littéraux accentués** restent dans 11 fichiers Kotlin, dont **109 clés de correspondance**
  (`ContentCatalog.kt` + `ContentText.kt`). Les 224 autres sont des données de jeu
  (`Model.kt` 144, `GameEngine.kt` 57) traduites **à l’affichage**, jamais migrées.
- **1 179 chaînes FR / 1 178 chaînes EN** : seul `app_name` reste non traduisible
  (nom de marque du lanceur, décision assumée).
- **Manuel bilingue** : ≈ 7 400 mots par langue, **36 captures par langue**, généré depuis
  `manuel/index.template.html` et `manuel/index.en.template.html`, avec sélecteur FR/EN.
- **Kit Play Store bilingue** : 5 captures EN, textes EN, feature graphic EN ; les captures FR
  existantes restent en place et passent au backlog de rafraîchissement.
- **README unique bilingue** (FR puis EN), `android/README.md` réduit à un renvoi.
- **Aucune migration de sauvegarde** : la v7 est conservée, aucun champ n’a été ajouté.
- 117 tests, lint OK, build `assembleGithubDebug` OK, passe FR/EN sur appareil.

## 2. Décisions verrouillées

| Sujet | Décision |
|---|---|
| Marque Play Store | `DungeonStep` (titre FR `DungeonStep — Marche des Âmes`, EN `DungeonStep - March of Souls`) |
| Nom affiché de l’app | `La Marche des Âmes` (FR) / `The March of Souls` (EN) |
| « Santé Connect » en anglais | **« Health Connect »** partout |
| Pluriels | `(s)` conservé (`1 item(s)`, `1 shard(s)`) ; pas de `<plurals>` dans cette passe |
| Séparateur de milliers EN | virgule (`7,464`) ; FR espace fine insécable (`7 464`) |
| Nombres compacts | FR `18,5 k` (espace fine) ; EN `18.5k` (sans espace) |
| Dates | FR `jj/mm/aaaa` ; EN `mm/dd/yyyy` (convention américaine assumée) |
| Heures | FR `14:05` ; EN `2:05 PM` |
| Captures Play | EN maintenant ; FR au backlog |
| `android/README.md` | renvoi vers le README racine |
| Release / tag / version | aucun changement |

## 3. Ce qui est couvert

### Application

- Réglages, Onboarding, Journal, Dashboard (Stats, Inventaire, Améliorations), Codex
  (Bestiaire, Ensembles, Biomes), Scène, Overlays, Diagnostic.
- **Mise à jour** : dialogues disponibles / téléchargement / installée, notes Play Protect,
  boutons et version installée.
- Notifications, toasts, widget, justification Health Connect.
- **Contenus dynamiques** : 110 créatures, ~34 gardiens, 54 objets, 16 consommables,
  6 effets, 8 biomes, 12 ensembles, titres de rang, rencontres spéciales, coffres,
  modificateurs et buffs.
- **Journal** : 45 formats traduits, y compris les entrées anciennes déjà stockées en français ;
  les messages Garmin et de synchronisation sont couverts.
- Nombres, dates, heures et pourcentages suivent `AppLocale` mis à jour dans `App.onCreate`
  et `MainActivity.onCreate`.

### Manuel

- FR : `manuel/index.template.html` → `manuel/index.html` + asset.
- EN : `manuel/index.en.template.html` → `manuel/en/index.html` + asset.
- Tâche Gradle `generateManual` : compteurs du Codex, version, copie des 36 images EN.
- `ManualActivity` charge l’asset EN si la langue est l’anglais **et** si l’asset existe ;
  sinon repli explicite sur le manuel FR.
- La capture de la clé Garmin utilise la carte **Réglages → Source de pas** (écran protégé
  par `FLAG_SECURE`).

### Kit Play Store

- `playstore/02-captures/en/01-walk.png` … `05-workshop.png` (1080×1920).
- `playstore/03-textes/en/` : titre, description courte, description complète, lore,
  notes de version, mots-clés.
- `playstore/04-feature-graphic/en/feature-graphic-1024x500.png`.
- `playstore/README.md` et `checklist-play-console.md` documentent la matrice FR/EN.

## 4. Audit des traductions

### 4.1 Littéraux restants

| Fichier | Littéraux accentués | Nature |
|---|---|---|
| `engine/Model.kt` | 144 | noms/effets de contenu, clés de sets, libellés sérialisables |
| `engine/GameEngine.kt` | 57 | messages de journal et d’événements |
| `ui/ContentText.kt` | 55 | motifs regex et correspondances FR→ressource |
| `ui/ContentCatalog.kt` | 54 | catalogue FR→ressource |
| `garmin/*`, `ui/Scene.kt`, `ui/Codex.kt`, `ui/Common.kt` | 13 | clés, libellés internes |
| **Total** | **333** | dont **109 clés** de correspondance |

Un contenu inconnu du catalogue **retombe en français** : aucun crash, aucune perte.

### 4.2 Textes paramétrables

- Audit automatique des `%` : aucun désaccord d’arguments entre FR et EN.
- Les chaînes à argument utilisent `%1$s` / `%1$d` ; les pourcentages littéraux sont
  appelés **sans argument** (donc non formatés) et ne posent pas de problème.
- **Incident trouvé et corrigé** : la chaîne EN `settings_about_version` avait perdu son
  `$s` (`v%1`), ce qui provoquait `UnknownFormatConversionException` à l’ouverture des
  Réglages. La chaîne est désormais `The March of Souls — v%1$s` ; vérifié sur appareil.
- Les pluriels `(s)` restent volontaires ; aucune chaîne n’utilise `<plurals>`.

### 4.3 Nombres et formats

- `formatInt` / `formatLong` : `NumberFormat` avec `AppLocale` → `7,464` en EN, `7 464` en FR.
- `formatCompact` : corrigé pour grouper aussi sous 10 000 en EN (auparavant `7464`).
- `oneDecimal` : `18.5` en EN, `18,5` en FR.
- Dates et heures formatées via `AppLocale` ; journal de synchronisation horodaté selon la langue.
- Widget et diagnostic utilisent la même locale.

### 4.4 Erreurs, régressions et performances

| Sujet | Constat |
|---|---|
| Changement de langue | recréation d’activité, `GameViewModel` conservé ; testé FR↔EN sans redémarrage |
| Canaux de notification | `ensureChannels` est appelé à chaque démarrage : les noms/descriptions sont réappliqués. Une notification déjà affichée garde sa langue d’émission (normal). |
| Widget | Le lanceur résout les `@string` du layout avec la **locale système**, pas celle de l’app : le titre et l’invite sont donc reposés par le provider avec les ressources de l’app. Validé sur écran d’accueil en EN (`THE MARCH OF SOULS`, `Tap to open and sync`). |
| Manuel EN absent | repli explicite FR, pas d’écran vide. |
| Regex du journal | compilées une fois, motifs simples ; coût mesuré acceptable sur 80 entrées. |
| Traductions imbriquées | récursion bornée (journal, butin, logs) ; aucun cycle. |
| Mémoire | aucun `Context` retenu dans les catalogues ; pas de cache ajouté ; pas de fuite identifiée. |
| Débordements | textes EN plus courts en général ; vérifiés aux tailles Grand / Maximal sur les écrans critiques. |

### 4.5 Perte de données

- Aucun champ de sauvegarde ajouté ou modifié depuis la v7.
- Les catalogues et le traducteur ne font que **lire** l’état.
- Les noms restent figés en français dans les sauvegardes ; la traduction se fait à l’affichage.
- Aucun scénario de perte identifié : ouverture v4/v5/v6/v7, switch de langue avec partie
  en cours, mise à jour FR→EN et EN→FR.

## 5. Architecture

- `AppLocale` : locale unique de formatage (`Locale.US` pour l’anglais, `Locale.FRANCE` sinon).
- `ContentCatalog.kt` : correspondance nom français → `R.string`.
- `ContentText.kt` : traduction du journal et des textes paramétrés, avec repli FR.
- `ManualActivity` : sélection FR/EN + repli.
- `generateManual` : génération des deux manuels et copie des images.
- `values/` = français ; `values-en/` = anglais ; clés identiques (hors `app_name`).

## 6. Validation

- **117 tests** unitaires, lint, `assembleGithubDebug`.
- Passe sur appareil FR puis EN : Dashboard, inventaire, fiche d’objet, atelier, camp,
  journal, Codex, réglages (haut et bas), diagnostic, mise à jour, manuel.
- Vérification visuelle des captures : nombres `7,464`, `2,441`, `18k`, dates et heures.
- Widget vérifié sur l’écran d’accueil en EN et en FR (titre, invite, pas, palier, or).
- Sauvegarde v7 conservée (bestiaire 18/146, 1 gardien, 16 `firstSeen`, 21 objets, palier 11).
- Contrôle automatique : aucune capture noire.

## 7. Limites connues

- Un contenu sans entrée de catalogue s’affiche en français (fallback assumé, sans perte).
- Les notifications déjà émises ne sont pas retraduites.
- Les captures FR du kit Play et du manuel datent d’une version antérieure.
- Le format de date EN est américain (`mm/dd/yyyy`) ; un public britannique pourrait préférer
  `jj/mm/aaaa`, à trancher plus tard.
- Pluriels `(s)` conservés ; une passe `<plurals>` reste possible.

## 8. Backlog

1. Refaire les **captures Play FR** sur la version actuelle.
2. Refaire les **captures du manuel FR** après les évolutions UI.
3. Valider le **widget** sur un écran d’accueil (FR/EN).
4. Étudier `<plurals>` et, si besoin, une date EN non ambiguë (`23 Nov 2026`).
5. Migration Room (hors i18n).
6. Démo web bilingue, vidéo promo, alphabets supplémentaires (polices dédiées).
