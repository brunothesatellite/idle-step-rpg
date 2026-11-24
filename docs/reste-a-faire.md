# La Marche des Âmes — reste à faire

> **État au 24/11/2026 — v0.21.2** (`versionCode 60`, branche `main`).
> Les lots 1 à 10 sont livrés, le **lot 5 est calibré** et la **pitié/potion arbitrées**.
> Reste le playtest.
>
> Rappels à jour : absence = **30 % des pas du jour** (réglable 0–30 % ou 0–5 000 pas),
> **4 min simulées** ; tests = **151 unitaires** + 7 tests UI existants ; lint OK.
> Équilibrage : [`plan-equilibrage.md`](plan-equilibrage.md), [`equilibrage.md`](equilibrage.md)
> et [`validation-equilibrage.md`](validation-equilibrage.md).
> Localisation : [`langue.md`](langue.md).

Légende effort : **S** < 1 h · **M** 1–3 h · **L** 3–8 h · **XL** plusieurs jours.

---

## 1. Priorité immédiate

| # | Sujet | Détail | Effort |
|---|---|---|---|
| 1 | **Playtest 0.21.2** | Vérifier l'écran de fin via **Aperçu de la fin** (build debug), l'export HTML, les gardiens aux paliers 36/50/75/100, la pitié et les potions à risque. | S |
| 2 | **Publier** | Release GitHub + APK (et AAB Play si validé). La dernière release publiée est `v0.19.1` ; 0.20.0 à 0.21.2 ne sont pas publiées. | S–M |

---

## 2. Équilibrage

### Livré — lots 1 à 10 (0.20.1 → 0.21.0), lot 5 calibré (0.21.1)

- **XP** : `1,09^niveau`, récompenses linéaires par palier, rattrapage ×1,25.
- **Objets** : butin ×1 à ×6, raretés cibles, upgrades additifs +4 % (20 niveaux max), caps VIT/effets, fusion 90 %.
- **Camp et sets** : bonus en pourcentage, coût indexé sur le palier, plafond 30 niveaux.
- **Rations** : cap 30, revenus renforcés, auto-ration 40 %/45 %, repos gratuit 2×20 %, farm +10 % PV, farm de gardien +1 éclat/+1 ration (5/jour).
- **Gardiens** : niveau palier +1, bouclier 35 %/3 s/8 s, phase 2 télégraphiée, enrage 25 s (+15 %/5 s, cap 60 %), pitié −1 %/défaite (cap −3 %), portail à 90 m.
- **Butin** : rare garanti tous les 5 kills, set tous les 10 gardiens, 1 non identifié sur 10 maximum.
- **Biomes** : budget de menace, RAPIDE/BERSERK exclus avec les capacités lourdes.
- **Fin de jeu** : Trône brisé +15 % → +35 %, gardiens renforcés, boss final et fin unique au palier 100, mode sans fin, télémétrie locale.
- **Correctif 0.20.10** : minimum d'intervalle d'attaque **180 ms** enfin branché (le code utilisait 300 ms, ce qui rendait la VIT inutile au-delà de ~2,4) ; `GATE_METERS` dédupliqué.
- **Écran de fin 0.21.0 (lot 10)** : diorama avec la statue réelle, l'aura dorée et le gardien déchu ; dossier du héros, progression, télémétrie, gardiens 81–100, économie, équipement, collections et jalons ; **export HTML** autonome avec la fonte pixel embarquée. Aperçu debug dans les réglages.

### Reste

- **Porte A** : franchie. La courbe `code` reproduit `makeMob`/`makeBoss` (biome, bouclier, enrage, pitié, capacités, modificateurs, Trône brisé).
- **Porte D** : `reference` 44–60 % et TTK 16–20 s aux paliers 36/50/75/100. Écarts connus : `under` 0–37 %, `reference_high` 100 % (sur-stuff assumé), pitié 15 % et potion d'ATQ à 100 %. Ces deux derniers points sont à arbitrer au playtest.
- **Portes E/F** : playtest 7 jours (Phase A), puis 14 jours (Phase B) avec journal quotidien.
- Aucune famine ni dead-end : vérifier depuis 0 ration/0 éclat sur plusieurs jours.

---

## 3. Robustesse et qualité

| # | Sujet | État / détail | Effort |
|---|---|---|---|
| P0-1 | Préavis de coffre | Fenêtre 3 h, worker 2 h, test unitaire. À observer en réel. | S |
| P0-2 | Fuseau / heure d'été | `ensureDay` ignore une date antérieure (testé). Vérification manuelle restante. | S |
| P0-3 | Migrations explicites | v7 en place. N'écrire une migration que lors d'un vrai changement de schéma. | M |
| P0-4 | Tests UI | 7 tests existants (coffre, onboarding, consommables, spécial, identification, fusion). Étendre au camp, à la forge, à l'absence, aux réglages, à l'import. | M |
| P0-5 | Crash local | Pas de journal de crash exportable. | M |
| P0-6 | Chaîne de build | Gradle 8.11.1 ne fonctionne qu'avec un JDK 17/21. Avec le JDK 24/25 par défaut, `testGithubDebugUnitTest` échoue (`Type T not present`). Utiliser le JBR 21 (`~/.jdks/jbr-21.0.11`) ou monter Gradle/AGP/Kotlin. | S |

---

## 4. Produit et contenu

### 4.1 Inventaire

| Sujet | Détail | Effort |
|---|---|---|
| Sélection multiple | Vente/démontage en lot ; aujourd'hui objet par objet. | M |
| Meilleure alternative | Comparer à la meilleure pièce du sac, pas seulement à l'objet équipé. | M |
| Loadouts | Ensembles favoris et changement rapide. | L |
| Seuil de vente auto | Vendre les communs sous un niveau donné. | S |
| Recherche dans le coffre | Pas de recherche ni de filtre. | S |

### 4.2 Combat et rencontres

| Sujet | Détail | Effort |
|---|---|---|
| Fréquence des rencontres | Cooldown pour éviter les répétitions rapprochées. | S |
| Télégraphe du gardien | Annoncer l'attaque spéciale avant impact. | M |
| Troisième phase de gardien | Ajouter une phase et l'équilibrer. | L |
| Animations de combat | Victoire/défaite, butin qui vole, soin de vampirisme, dégâts réduits. | L |
| Modificateurs supplémentaires | Invocateur, bouclier, etc. | M |

### 4.3 Contenu et endgame

| Sujet | Détail | Effort |
|---|---|---|
| Écran de fin enrichi (lot 10) | Livré en 0.21.0 ; à valider visuellement sur téléphone via l'aperçu debug. | S |
| Succès et statistiques de vie | Compteurs existants, pas d'écran dédié. | L |
| Serments hebdomadaires | Journaliers uniquement ; ajouter séries et reroll. | M |
| Économie de fin de jeu | Puits d'or, coûts d'artisanat, plafonds. | L |
| Transitions de biome | Décors/météo existants ; transitions animées à faire. | M |
| Tutoriel contextuel | Aides des 10 premiers mètres, premier combat, premier coffre, premier gardien. | M |

---

## 5. Localisation — livrée, maintenance

- **FR/EN** : interface, journal, Codex, notifications, widget, mise à jour, manuel et contenus dynamiques.
- **Manuel** : 36 captures EN + sélecteur FR/EN ; manuel en ligne en 0.20.0.
- **Kit Play EN** : 5 captures, textes, feature graphic, notes.
- **Promo EN** : pitch et captures.

Restant :

| Sujet | Détail | Effort |
|---|---|---|
| Captures Play FR | À refaire sur la version actuelle (backlog). | M |
| Captures manuel FR | À rafraîchir après les évolutions UI. | M |
| Dates EN | Choix en-US `mm/dd/yyyy` assumé ; à réévaluer si besoin. | S |
| Pluriels | `(s)` conservé ; passer à `<plurals>` si nécessaire. | M |
| Widget | Validé visuellement FR/EN ; à revérifier après chaque évolution. | S |

---

## 6. Technique

| Sujet | État / détail | Effort |
|---|---|---|
| Room | Non fait ; JSON versionné + `.bak`. Décision à prendre. | XL |
| R8 / minification | Désactivés ; activer, tester kotlinx.serialization, fournir `mapping.txt`. | M |
| Gradle | Wrapper 8.11.1 en fin de support et limité au JDK 21 ; monter Gradle/AGP/Kotlin et rebuild. | M |
| Tests Garmin | 401, 403, 429, vide, JSON invalide, coupure réseau, fuseau. | M |
| Lint / dette Kotlin | Pas de ktlint/detekt ; warnings Canvas à traiter. | M |
| Performance | 10 Hz de recomposition, deep copies, sauvegardes JSON ; profiler. | M |
| WorkManager | Sync + maintenance ; ajouter coffres, backoff, intervalles adaptatifs. | M |
| Sécurité | Vérifier les exports, `FLAG_SECURE`, captures. | S |
| Sauvegarde | Export/import + aperçu + `.bak` ; rotation horodatée à ajouter. | M |
| Accessibilité | Audit TalkBack, contrastes, focus, taille 200 %. | L |
| Grands écrans | Pas de layout tablette/paysage. | L |
| Play Store | Kit EN prêt ; AAB 0.21.2 et captures FR à faire. | M |
| Mise à jour in-app | Livrée ; vérification périodique en arrière-plan à ajouter. | S |

---

## 7. Documentation

| Sujet | État |
|---|---|
| README bilingue | À jour (0.21.2, FR puis EN) |
| Manuel FR/EN | Généré au build, en ligne 0.20.0 |
| `langue.md` | État i18n finalisé |
| `plan-equilibrage.md` | Lots 0-10 livrés ; lot 5 calibré |
| `equilibrage.md` | Analyse + cibles 1 → 100 (base 0.20.9) |
| `validation-equilibrage.md` | Plan de validation et portes A–F |
| `ecran-fin-v2.html` | Base visuelle validée du lot 10 (implémentée en 0.21.0) |
| `CHANGELOG.md` | v0.21.2 ajouté |
| Spécification vs écarts | À resynchroniser |
| Guide de récupération | À compléter (clé perdue, téléphone changé, sauvegarde corrompue) |

---

## 8. Ordre recommandé

1. **Playtest 0.21.1** : écran de fin, export HTML, gardiens 36/50/75/100.
2. **Arbitrer** la pitié et la potion d'ATQ (aujourd'hui victoire à 100 %).
3. **Publier** (GitHub + AAB Play) si validé.
5. **Playtests 7 puis 14 jours** (portes E/F).
6. **Qualité** : tests UI, R8, Gradle/JDK, perf, accessibilité.
7. **Backlog produit** : inventaire, combat, endgame, tutoriel.
8. **Localisation** : captures FR Play/manuel, décisions dates/pluriels.

---

## 9. Critères d'acceptation transverses

- Aucun pas compté deux fois, même après changement de fuseau ou réimport.
- Toute action destructive demande confirmation.
- Toute erreur réseau a une action de sortie (réessayer, remplacer la clé, plus tard).
- L'app reste utilisable hors ligne et sans clé Garmin.
- Aucune notification si l'utilisateur les a désactivées.
- Aucune régression des données lors d'une mise à jour d'APK.
- Tests verts (**151 unitaires**, 7 UI existants) et lint OK avant chaque release.
- FR et EN restent cohérents : aucune chaîne française résiduelle en mode EN.
- Un gardien ne doit jamais tomber en 2-3 coups ; les potions accélèrent sans garantir la victoire.
