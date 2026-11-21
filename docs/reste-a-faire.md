# La Marche des Âmes — reste à faire

> **État au 21/11/2026 — v0.17.5.** Ce document remplace le backlog rédigé après la v0.3.1 :
> la majorité des P0/P1/P2 d’alors sont livrés. Ne restent que les écarts vérifiés ci-dessous.
>
> Rappels à jour : absence = **30 % des pas du jour par défaut** (réglable 0–30 % ou en pas,
> 0–5 000) et **4 min simulées** ; tests = **95 unitaires + 7 instrumentés** ; Play Store
> livré en **0.17.5**.
> La localisation est étudiée dans [`langue.md`](langue.md) (document de décision, non implémenté).

Légende effort : **S** < 1 h · **M** 1–3 h · **L** 3–8 h · **XL** plusieurs jours.

---

## 1. Déjà livré depuis l’ancien backlog

Pour mémoire, ne plus planifier :

- **P0** : camp qui réinitialise le combat, veillée gratuite à **+15 %**, décroissance de la
  ferveur rattrapée hors ligne, deep links des notifications (`open_screen`), écran de
  permission notifications, import avec aperçu, contrôle **250 pas/min**, worker de clé
  Garmin et worker de maintenance des coffres (préavis porté à 3 h), garde-fou de fuseau
  (retour en arrière ignoré), copie pré-migration, 7 tests UI instrumentés.
- **P1** : simulation d’absence complète et son rapport, onboarding 4 pages + relecture depuis
  les réglages, facteur de profondeur (+12 %/+8 % après le palier 10), bandeau « nouveau jour »,
  notifications par catégorie et heures calmes, camp avec journal/inventaire/potions.
- **P2** : objets non identifiés, 11 ensembles 2/4/6, effets, reroll, amélioration, démontage,
  fusion corrigée, tri/filtre (Score, Rareté, Niveau, Fusion), vente des communs, coffre de camp,
  rencontres spéciales (autel, source, marchand, statue, risque), Codex (bestiaire, ensembles,
  biomes), journal de légende, widget, mise à jour in-app, CI debug + release, manuel.
- **v0.17.5** : budget d’absence réglable (Réglages avancés), icône debug émeraude, badge ver,
  libellé `· dev`, manuel et capture mis à jour.

---

## 2. P0 — robustesse restante

| # | Sujet | Détail | Effort |
|---|---|---|---|
| P0-1 | Validation terrain du préavis de coffre | Fenêtre portée à **3 h** (worker toutes les 2 h) et couverte par un test unitaire. Reste à observer une notification réelle sur appareil. | S |
| P0-2 | Validation du fuseau sur émulateur | `ensureDay` ignore désormais une date antérieure (test unitaire). Reste la vérification manuelle d’un changement de fuseau et du passage heure d’été/hiver. | S |
| P0-3 | Migrations explicites par version | Copie `game_state.pre_migration_vN.json` et test de migration livrés. Les migrations par version ne sont à écrire que lors d’un vrai changement de schéma. | M |
| P0-4 | Étendre les tests UI | 7 tests verts (coffre, onboarding, consommables, spécial, identification, 2 fusion). Ajouter camp, forge, absence, réglages, import. | M |

---

## 3. P1 — rétention et finitions

| # | Sujet | Détail | Effort |
|---|---|---|---|
| P1-1 | Jalons complets | Livré : +5 % d’or par jalon, +5 éclats, entrée de légende, titre, rang. Restant : apparence du feu de camp par palier, héros/statue au palier 100, déblocage de la forge au premier gardien, compteur des 10 coffres, apparence « Âme ardente » à 50 000 pas. | L |
| P1-2 | Tutoriel contextuel | L’onboarding initial existe, mais pas les aides des premiers 10 m, premier combat, premier coffre, premier gardien, premier camp. | M |
| P1-3 | Notifications réseau | Action « Remplacer la clé » sur 401, backoff explicite sur 429 avec heure de nouvel essai, notification de fin de synchronisation de fond. | M |
| P1-4 | Rappel quotidien | Notification optionnelle de rappel (serments, pas du jour) ; aujourd’hui seul le bandeau « nouveau jour » existe. | S |
| P1-5 | Campement | Restant : animation de soin par flammes, bilan de veillée détaillé, apparence du feu par palier (lié à P1-1). | M |

---

## 4. P2 — confort et profondeur

### 4.1 Inventaire

| Sujet | Détail | Effort |
|---|---|---|
| Sélection multiple | Vente multiple et démontage multiple ; aujourd’hui action objet par objet. | M |
| Meilleure alternative | Comparer à la meilleure pièce du sac, pas seulement à l’objet équipé. | M |
| Loadouts | Ensembles favoris et changement rapide. | L |
| Seuil de vente auto | Vendre automatiquement les communs sous un niveau donné. | S |
| Recherche dans le coffre | Le coffre de camp liste et retire à l’unité ; pas de recherche ni de filtre. | S |

### 4.2 Rencontres et combat

| Sujet | Détail | Effort |
|---|---|---|
| Fréquence des rencontres | Cooldown pour éviter les répétitions rapprochées. | S |
| Illustrations/sons dédiés | Les rencontres utilisent le décor existant. | M |
| Télégraphe du gardien | Annoncer l’attaque spéciale avant impact. | M |
| Troisième phase de gardien | Ajouter une phase et l’équilibrer. | L |
| Animations de combat | Victoire/défaite, butin qui vole vers l’inventaire, soin de vampirisme flottant, dégâts réduits affichés. | L |
| Modificateurs supplémentaires | Invocateur, bouclier, etc. ; les 5 actuels couvrent déjà RAPIDE, BLINDÉ, BERSERK, RÉGÉNÉRANT, VOLEUR. | M |

### 4.3 Contenu et endgame

| Sujet | Détail | Effort |
|---|---|---|
| Succès et statistiques de vie | Les compteurs existent (`kills`, `bossKills`, `goldEarned`, `chestsOpened`, `retreats`), mais pas d’écran de succès ni de statistiques de vie. | L |
| Serments hebdomadaires | Aujourd’hui journaliers uniquement ; ajouter séries et reroll. | M |
| Boucle au-delà du palier 80 | Vérifier le cycle des biomes et des boss sur très longue durée. | M |
| Économie de fin de jeu | Puits d’or, coûts d’artisanat, plafonds de ressources. | L |
| Transitions de biome | La météo et les décors existent ; les transitions animées entre paliers restent à faire. | M |

---

## 5. Technique et qualité

| Sujet | État / détail | Effort |
|---|---|---|
| Room | Non fait ; JSON versionné + `.bak`. Décision : garder JSON avec migrations explicites, ou migrer Room. | XL |
| R8 / minification | `isMinifyEnabled = false`, `isShrinkResources = false`. Activer, conserver kotlinx.serialization, tester, fournir `mapping.txt`. | M |
| Gradle | Wrapper **8.11.1** en fin de support ; monter Gradle puis AGP/Kotlin et rebuild complet. | M |
| Tests Garmin | 401, 403, 429, réponse vide, JSON invalide, coupure réseau, fuseau. | M |
| Tests UI | Un seul fichier (`OverlayUiTest`) ; à étendre (voir P0-4). | L |
| Lint / dette Kotlin | Pas de ktlint/detekt ; warnings Kotlin (dépréciations Canvas) à traiter. | M |
| Crash et diagnostic | Panneau Diagnostic et journal de sync existent ; pas de journal de crash local exportable. | M |
| Performance | 10 Hz de recomposition, deep copies par tick, sauvegardes JSON : profiler et optimiser. | M |
| WorkManager | 2 workers (sync Garmin, maintenance clé) ; ajouter coffres, intervalles adaptatifs, backoff. | M |
| Sécurité | Vérifier qu’aucune clé ne peut sortir dans un export ; envisager `FLAG_SECURE` ; politique de capture. | S |
| Sauvegarde | Export/import + aperçu + `.bak` livrés ; ajouter rotation horodatée et restauration manuelle. | M |
| Localisation | Étude dans [`langue.md`](langue.md) ; non implémentée. | Voir l’étude |
| Accessibilité | Quelques `contentDescription` ; audit TalkBack, contrastes, ordre de focus, test 200 %. | L |
| Grands écrans | Pas de layout tablette/paysage dédié ; la spec prévoit un tableau de bord élargi. | L |
| Play Store | Livré ; restant : R8/mapping et symboles natifs (`libandroidx.graphics.path.so`). | S |
| Mise à jour in-app | Livrée ; restant : vérification périodique en arrière-plan via WorkManager. | S |

---

## 6. Documentation

| Sujet | État |
|---|---|
| Politique de confidentialité, licences, manuel | Livrés |
| CHANGELOG | Présent |
| Spécification vs écarts assumés | À resynchroniser (absence réglable, icône debug, réglages avancés) |
| Guide de récupération | À compléter : clé perdue, téléphone changé, sauvegarde corrompue |
| Localisation | [`langue.md`](langue.md) |

---

## 7. Ordre recommandé

1. **Robustesse** : valider le préavis de coffre et le fuseau sur appareil/émulateur, étendre les tests UI ; migrations explicites seulement si le schéma change.
2. **Finitions** : jalons complets, tutoriel contextuel, notifications réseau, rappel quotidien.
3. **Confort** : sélection multiple, meilleure alternative, recherche du coffre.
4. **Combat/contenu** : télégraphe, animations, succès, serments hebdomadaires.
5. **Industrialisation** : R8, Gradle, ktlint, perf, accessibilité, grands écrans.
6. **Localisation** : suivre le découpage de `langue.md`.

---

## 8. Critères d’acceptation transverses

- Aucun pas compté deux fois, même après changement de fuseau ou réimport.
- Toute action destructive demande confirmation.
- Toute erreur réseau a une action de sortie (réessayer, remplacer la clé, plus tard).
- L’app reste utilisable hors ligne et sans clé Garmin.
- Aucune notification si l’utilisateur les a désactivées.
- Aucune régression des données lors d’une mise à jour d’APK.
- Tests verts (95 unitaires + 7 instrumentés) et CI verte avant chaque release.
- Le français reste pixel-identique tant que la localisation n’est pas validée.
