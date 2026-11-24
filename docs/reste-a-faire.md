# La Marche des Âmes — reste à faire

> **État au 24/11/2026 — v0.20.0** (`versionCode 47`, branche `main`).
> Ce document remplace le backlog précédent : la localisation FR/EN, le manuel bilingue,
> le widget et la Phase A d’équilibrage sont désormais livrés.
>
> Rappels à jour : absence = **30 % des pas du jour** (réglable 0–30 % ou 0–5 000 pas),
> **4 min simulées** ; tests = **117 unitaires** + 7 tests UI existants ; lint OK.
> Équilibrage : [`equilibrage.md`](equilibrage.md) et [`validation-equilibrage.md`](validation-equilibrage.md).
> Localisation : [`langue.md`](langue.md).

Légende effort : **S** < 1 h · **M** 1–3 h · **L** 3–8 h · **XL** plusieurs jours.

---

## 1. Priorité immédiate

| # | Sujet | Détail | Effort |
|---|---|---|---|
| 1 | **Playtest 0.20.0** | Tester le palier 36 avec le build réel : rééquiper via « Équiper le meilleur » (PV désormais valorisés), potions, farm. Vérifier qu’un gardien dure ~15-20 s et ne tombe pas en 2-3 coups. | S |
| 2 | **Publier 0.20.0** | Release GitHub `v0.20.0` + APK (et AAB si validé). La dernière release publiée est `v0.19.1`. | S |
| 3 | **AAB Play 0.20.0** | Construire l’AAB, mettre à jour la fiche et les notes. | M |
| 4 | **Phase B équilibrage** | XP, butin scalable, camp/sets en pourcentage, gains de niveau. | L |

---

## 2. Équilibrage

### Phase A — implémentée, playtest en cours

- Courbe cible interpolée entre les paliers 20 et 35.
- Gardiens : bouclier 40 % / 3 s, enrage après 15 s, bénédiction cumulative +25 %/défaite (cap +75 %).
- Élites : 6 %, 1,8× PV, 1,15× ATQ.
- Éclats : plafond **24**, gardien +3, jalon +8, coffre +2, démontage 2/3/6/10, reforge **1**.
- Rations : gardien +2, coffre +2, monstre 10 %, auto-ration 30 %, veillée gratuite **25 %**.
- Score d’objet : PV ×0,6.
- **Reste** : valider la progression réelle au palier 36 et au-delà ; le build glass cannon actuel peut encore nécessiter du stuff PV ou une potion.

### Phase B — niveau 100

- Courbe d’XP `1,09^niveau` (au lieu de `1,2^niveau`).
- Récompenses d’XP linéaires avec le palier.
- Butin : stats multipliées par le palier (`1 + 0,06×(palier−1)`), raretés plus marquées.
- Camp et sets : bonus en pourcentage.
- Gains de niveau : +5 ATQ / +25 PV / +1,5 % VIT.
- Validation : 2 000–3 500 kills pour 1→100, niveau 100 vers le palier 100-150.

### Phase C — fin de partie

- Remplacer le cap 40 par une courbe lisible ou un NG+.
- Télémétrie locale et puissance recommandée.
- Vérifier le cycle des biomes/boss au-delà du palier 80.

---

## 3. Robustesse et qualité

| # | Sujet | État / détail | Effort |
|---|---|---|---|
| P0-1 | Préavis de coffre | Fenêtre 3 h, worker 2 h, test unitaire. À observer en réel. | S |
| P0-2 | Fuseau / heure d’été | `ensureDay` ignore une date antérieure (testé). Vérification manuelle restante. | S |
| P0-3 | Migrations explicites | v7 en place. N’écrire une migration que lors d’un vrai changement de schéma. | M |
| P0-4 | Tests UI | 7 tests existants (coffre, onboarding, consommables, spécial, identification, fusion). Étendre au camp, à la forge, à l’absence, aux réglages, à l’import. | M |
| P0-5 | Crash local | Pas de journal de crash exportable. | M |

---

## 4. Produit et contenu

### 4.1 Inventaire

| Sujet | Détail | Effort |
|---|---|---|
| Sélection multiple | Vente/démontage en lot ; aujourd’hui objet par objet. | M |
| Meilleure alternative | Comparer à la meilleure pièce du sac, pas seulement à l’objet équipé. | M |
| Loadouts | Ensembles favoris et changement rapide. | L |
| Seuil de vente auto | Vendre les communs sous un niveau donné. | S |
| Recherche dans le coffre | Pas de recherche ni de filtre. | S |

### 4.2 Combat et rencontres

| Sujet | Détail | Effort |
|---|---|---|
| Fréquence des rencontres | Cooldown pour éviter les répétitions rapprochées. | S |
| Télégraphe du gardien | Annoncer l’attaque spéciale avant impact. | M |
| Troisième phase de gardien | Ajouter une phase et l’équilibrer. | L |
| Animations de combat | Victoire/défaite, butin qui vole, soin de vampirisme, dégâts réduits. | L |
| Modificateurs supplémentaires | Invocateur, bouclier, etc. | M |

### 4.3 Contenu et endgame

| Sujet | Détail | Effort |
|---|---|---|
| Succès et statistiques de vie | Compteurs existants, pas d’écran dédié. | L |
| Serments hebdomadaires | Journaliers uniquement ; ajouter séries et reroll. | M |
| Économie de fin de jeu | Puits d’or, coûts d’artisanat, plafonds. | L |
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
| Gradle | Wrapper 8.11.1 en fin de support ; monter Gradle/AGP/Kotlin et rebuild. | M |
| Tests Garmin | 401, 403, 429, vide, JSON invalide, coupure réseau, fuseau. | M |
| Lint / dette Kotlin | Pas de ktlint/detekt ; warnings Canvas à traiter. | M |
| Performance | 10 Hz de recomposition, deep copies, sauvegardes JSON ; profiler. | M |
| WorkManager | Sync + maintenance ; ajouter coffres, backoff, intervalles adaptatifs. | M |
| Sécurité | Vérifier les exports, `FLAG_SECURE`, captures. | S |
| Sauvegarde | Export/import + aperçu + `.bak` ; rotation horodatée à ajouter. | M |
| Accessibilité | Audit TalkBack, contrastes, focus, taille 200 %. | L |
| Grands écrans | Pas de layout tablette/paysage. | L |
| Play Store | Kit EN prêt ; AAB 0.20.0 et captures FR à faire. | M |
| Mise à jour in-app | Livrée ; vérification périodique en arrière-plan à ajouter. | S |

---

## 7. Documentation

| Sujet | État |
|---|---|
| README bilingue | À jour (0.20.0, FR puis EN) |
| Manuel FR/EN | Généré au build, en ligne 0.20.0 |
| `langue.md` | État i18n finalisé |
| `equilibrage.md` | Analyse + Phase A |
| `validation-equilibrage.md` | Plan de validation |
| `CHANGELOG.md` | v0.20.0 ajouté |
| Spécification vs écarts | À resynchroniser |
| Guide de récupération | À compléter (clé perdue, téléphone changé, sauvegarde corrompue) |

---

## 8. Ordre recommandé

1. **Playtest 0.20.0** au palier 36, puis décision go/no-go.
2. **Publier 0.20.0** (GitHub + AAB Play) si validé.
3. **Phase B** : XP, butin, camp/sets, gains de niveau ; playtest 14 jours.
4. **Phase C** : fin de partie, télémétrie.
5. **Qualité** : tests UI, R8, Gradle, perf, accessibilité.
6. **Backlog produit** : inventaire, combat, endgame, tutoriel.
7. **Localisation** : captures FR Play/manuel, décisions dates/pluriels.

---

## 9. Critères d’acceptation transverses

- Aucun pas compté deux fois, même après changement de fuseau ou réimport.
- Toute action destructive demande confirmation.
- Toute erreur réseau a une action de sortie (réessayer, remplacer la clé, plus tard).
- L’app reste utilisable hors ligne et sans clé Garmin.
- Aucune notification si l’utilisateur les a désactivées.
- Aucune régression des données lors d’une mise à jour d’APK.
- Tests verts (**117 unitaires**, 7 UI existants) et lint OK avant chaque release.
- FR et EN restent cohérents : aucune chaîne française résiduelle en mode EN.
- Un gardien ne doit jamais tomber en 2-3 coups ; les potions accélèrent sans garantir la victoire.
