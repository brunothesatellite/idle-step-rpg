# La Marche des Âmes — reste à faire après la v0.3.1

> **Mise à jour v0.6.0** : les sons 8 bits sont livrés (licence CC0, volume réglable,
> désactivation). Le reste de la liste ci-dessous est conservé comme backlog.
>
> **Backlog explicite — pas maintenant** : localisation anglaise, migration Room,
> publication Play Store (non prévue), équilibrage long terme, farm automatique à
> 0 pas, réduction automatique des animations sur batterie faible.

## Backlog — non planifié

| Sujet | Décision |
|---|---|
| Localisation anglaise | Plus tard, interface française pour l'instant. |
| Migration Room | Plus tard, sauvegarde JSON versionnée conservée. |
| Publication Play Store | Non prévue, distribution APK/GitHub. |
| Équilibrage long terme | À faire après plusieurs semaines de données réelles. |
| Farm automatique à 0 pas | Plus tard. |
| Animations réduites automatiquement sur batterie faible | Plus tard. |

Document de travail : fonctionnalités non terminées, écarts avec la spécification,
dette technique et ordre recommandé.

Légende effort : **S** < 1 h · **M** 1–3 h · **L** 3–8 h · **XL** plusieurs jours.

---

## 1. État actuel

Déjà livré en v0.3.1 :

- Garmin Personal API, clé chiffrée, jeton 24 h, 2FA navigateur, historique 7 jours.
- Marche, combats automatiques, gardiens, coffres 24 h / 2 en attente, farm, camp.
- Serments journaliers variables, ferveur avec décroissance, journal filtrable.
- Réglages : texte, notifications on/off, animations, vibrations, journal de sync,
  export/import JSON, effacement.
- Inventaire : équiper le meilleur, vendre les communs, verrouiller, fusionner,
  effets légendaires, coffre de camp.
- Biomes colorés, rencontres spéciales, jalons, rang, modificateurs de gardien.
- Tests unitaires (17), CI Android debug, sauvegarde de secours.

---

## 2. P0 — correctifs et robustesse

| # | Sujet | Détail | Effort |
|---|---|---|---|
| P0-1 | Camp et combat en cours | La spec demande que le combat soit **réinitialisé** à l’ouverture du camp (PV et timers de l’ennemi), avec le gardien soigné. Aujourd’hui le combat est seulement figé. | M |
| P0-2 | Veillée gratuite | La spec dit **+15 % PV**, le code applique +25 %. Aligner le texte et l’effet. | S |
| P0-3 | Décroissance hors ligne de la ferveur | La décroissance n’avance que pendant que l’app est ouverte. Après 3 jours fermés, la ferveur reste haute jusqu’à plusieurs heures d’usage. Calculer le temps écoulé à la reprise et appliquer la décroissance manquante. | M |
| P0-4 | Coffre qui expire app fermée | `expireChests()` supprime le coffre à la réouverture sans prévenir. Ajouter un Worker qui vérifie les échéances et notifie avant expiration. | M |
| P0-5 | Clé Garmin expirée app fermée | La vérification 30/7/1 jour ne tourne qu’à l’ouverture. Ajouter un Worker quotidien (ou une vérification WorkManager) qui notifie même sans lancement. | M |
| P0-6 | Deep links des notifications | Toutes les notifications ouvrent l’écran principal. Router vers : Compte Garmin, coffre, camp, journal selon le type. | M |
| P0-7 | Permission notifications refusée | Aucun écran d’explication ni bouton « Réessayer ». Afficher un état clair dans les réglages et un accès aux réglages système. | S |
| P0-8 | Import de sauvegarde | Aucun aperçu, aucune vérification de version. Ajouter : validation du schéma, résumé (date, niveau, or, palier), confirmation, migration `GameState.version`. | M |
| P0-9 | Pas suspects | Seul le plafond 60 000/jour existe. Ajouter le contrôle **250 pas/min en moyenne** et un journal explicite des pas ignorés. | M |
| P0-10 | Changement de fuseau | Le ledger est indexé par date locale. Tester et documenter le cas ; éviter le double comptage ou la perte au changement de fuseau. | M |
| P0-11 | Régression overlays | Les taps ne passent plus derrière les overlays, mais aucun test UI ne le garantit. Ajouter des tests instrumentés. | M |

---

## 3. P1 — boucle de jeu et rétention

### 3.1 Simulation hors ligne complète

La spec attend un récapitulatif **pas, combats, or, paliers**. Aujourd’hui seuls les pas
sont ajoutés à la réserve, puis les combats se déroulent en direct.

À faire :

- simuler en accéléré les pas récupérés (plafond 100 000) ;
- produire un bilan : pas, combats, or, paliers gravis, gardiens, butin ;
- afficher ce bilan dans l’overlay hors ligne ;
- conserver un mode « différé » si la simulation est trop longue.

Effort : **L**.

### 3.2 Onboarding

- 3 à 4 écrans de première ouverture : concept, Personal API + 2FA navigateur,
  reset 00:00, permissions notifications.
- Tutoriel contextuel : premiers 10 m, premier combat, premier coffre, premier gardien,
  premier camp.
- Écran « aucune clé Garmin » avec lien direct vers la création de clé.
- Explication si permission refusée.
- Possibilité de revoir l’onboarding depuis les réglages.

Effort : **L**.

### 3.3 Jalons complets

Aujourd’hui : bonus de stats automatique + entrée de journal + rang.

Manque (spec 14.3) :

- +5 % d’or permanent par jalon ;
- titre affiché (pas seulement un rang romain) ;
- apparence du feu de camp par palier ;
- 5 éclats d’âme par jalon ;
- journal de légende persistant (séparé des 80 entrées) ;
- tous les 100 paliers : palier légendaire, apparence de héros, statue au camp ;
- premier gardien : déblocage de la forge ;
- 10 coffres ouverts : compteur de quotas avancé ;
- 50 000 pas cumulés : apparence « Âme ardente ».

Nécessite des **compteurs de vie** : pas cumulés, coffres ouverts, gardiens tués, or gagné.

Effort : **L**.

### 3.4 Facteur de profondeur

Spec 14.1 : au-delà du palier 10, +12 % PV/ATQ des monstres et +8 % des gardiens
par palier. Aujourd’hui la difficulté suit seulement le niveau linéaire. Ajouter ce
facteur, puis rééquilibrer les courbes de puissance et d’or.

Effort : **M**.

### 3.5 Campement complet

- Gestion de l’équipement depuis le camp (comparer, équiper, vendre).
- Liste du coffre de camp avec récupération objet par objet.
- Journal complet accessible depuis le camp.
- Résumé : palier, distance, **pas en attente**, **or**, ferveur.
- Réinitialisation des combats (P0-1).
- Animation de soin par flammes.
- Bilan de veillée.

Effort : **L**.

### 3.6 Notifications et synchronisation

- Cases par catégorie : gardien, niveau, serments, coffre, clé Garmin.
- Heures calmes / ne pas déranger.
- Action directe sur 401 : « Remplacer la clé ».
- Backoff explicite sur 429 avec heure de nouvel essai.
- Fin de synchronisation en arrière-plan : notification optionnelle.
- Vérification de clé planifiée (P0-5).

Effort : **M**.

### 3.7 Accueil et changement de jour

- Bandeau « nouveau jour » à 00:00 avec serments renouvelés.
- Écran de retour après plusieurs jours : résumé doux, pas de pression.
- Notification de rappel quotidien optionnelle.

Effort : **M**.

---

## 4. P2 — profondeur de gestion

### 4.1 Objets

- Objets **non identifiés** + parchemin ou éclat d’identification.
- **Sets** d’équipement avec bonus 2/4/6 pièces.
- Plus d’effets légendaires uniques (vol de vie, zone, bouclier, exécution…).
- **Reroll** d’affixe à la forge.
- **Forge** : améliorer un objet avec or + éclats, casser un objet en éclats.
- Filtres et tri de l’inventaire (rareté, emplacement, niveau, score).
- Sélection multiple, vente multiple, verrou visible dans la grille.
- Comparaison avec la **meilleure alternative** du sac, pas seulement l’objet équipé.
- Loadouts / ensembles favoris.
- Filtre de vente automatique (ex. vendre les communs sous un niveau).
- Coffre de camp avec liste et recherche.

Effort : **XL** (découper en 3 lots).

### 4.2 Rencontres spéciales

- Choix risqués : sacrifier des PV, de l’or ou un objet pour un gain aléatoire.
- Marchand avec stock tournant et achat au camp.
- Malédictions/bénédictions temporaires.
- Autels à plusieurs offrandes.
- Fréquence et cooldown pour éviter la répétition.
- Illustrations et sons dédiés.

Effort : **L**.

### 4.3 Biomes et ambiance

- Tiles, props et pools de monstres propres à chaque biome (aujourd’hui : palette).
- Météo : neige, cendre, braises, brume animée.
- Gardiens avec apparence de biome.
- Transitions de palier.
- Musique/ambiance sonore.

Effort : **XL**.

### 4.4 Combat

- Critique distingué visuellement, soin de vampirisme flottant, dégâts réduits affichés.
- Télégraphe de l’attaque spéciale du gardien.
- Troisième phase de gardien.
- Animations de victoire/défaite et butin qui vole vers l’inventaire.
- Modificateurs supplémentaires : invocateur, régénérant, voleur d’or, bouclier.
- Équilibrage des 4 effets légendaires.

Effort : **L**.

---

## 5. Contenu et endgame

- Plus de monstres, élites et gardiens (actuellement 7 / 7 / 8 / 8).
- Biomes au-delà du palier 50 : la spec veut un donjon infini, mais les noms s’arrêtent à « Cimes du Serment ».
- Boss uniques aux paliers 10, 25, 50, 100.
- Journal de légende persistant.
- Bestiaire / collection.
- Succès et statistiques de vie.
- Serments hebdomadaires, séries, reroll de serment.
- Bilan quotidien/hebdomadaire.
- Économie de fin de jeu : puits d’or, coûts d’artisanat, plafonds de ressources.

Effort : **XL**, à découper.

---

## 6. Technique et qualité

| Sujet | Détail | Effort |
|---|---|---|
| Room | Remplacer le JSON par Room avec migrations testées. Sinon, conserver JSON mais ajouter `version` + migrations explicites. | XL |
| Tests Garmin | 401, 403, 429, réponse vide, JSON invalide, coupure réseau, changement de fuseau. | M |
| Tests UI | Camp, overlays, journal, coffres, inventaire, réglages, imports. | L |
| CI release | Compiler et signer la release via secrets GitHub (keystore base64), publier l’APK + SHA. Aujourd’hui CI debug uniquement. | M |
| Lint / static analysis | ktlint ou detekt, avertissements Kotlin, dépendances obsolètes. | M |
| Crash et diagnostics | Journal local circulaire, export de diagnostic sans données sensibles. | M |
| Performance | 10 Hz de recomposition, deep copies à chaque tick, sauvegardes JSON. Profiler et optimiser. | M |
| WorkManager | Intervalle adaptatif, contraintes batterie, backoff exponentiel, workers séparés (sync, clé, coffre). | M |
| Sécurité | Vérifier qu’aucune clé ne peut partir dans un export, option FLAG_SECURE globale, politique de capture. | S |
| Sauvegarde | Export horodaté, aperçu avant import, rotation des `.bak`, restauration manuelle. | M |
| Localisation | Extraire les chaînes, ajouter l’anglais. Aujourd’hui français en dur. | L |
| Accessibilité | Audit TalkBack, descriptions des éléments de scène, contraste, ordre de focus, mode daltonien, test 200 %. | L |
| Grands écrans | Paysage et tablette : la spec prévoit un tableau de bord élargi. Aujourd’hui portrait uniquement. | L |
| Widget / raccourci | Widget pas du jour + bouton synchroniser. | M |
| Play Store | AAB, fiche, politique de confidentialité, signature Play. Optionnel si sideload suffit. | L |
| Mise à jour in-app | Vérifier la dernière release GitHub et proposer le téléchargement. | S |

---

## 7. Légal et documentation

- Politique de confidentialité : aucune collecte, clé locale, Garmin uniquement.
- Écran licences cliquable dans l’app (MIT, CC0, OFL).
- CHANGELOG.md.
- Mettre à jour la spécification pour les écarts assumés (veillée +25 %, notifications,
  auto-équipement, coffre de camp).
- Guide de récupération : clé perdue, téléphone changé, sauvegarde corrompue.

Effort : **M**.

---

## 8. Hors périmètre confirmé

- Health Connect.
- Backend ou compte propriétaire.
- Saisie du mot de passe ou du 2FA Garmin dans l’app.
- Classements et guildes en ligne (aucun serveur).

---

## 9. Ordre recommandé

### Sprint 1 — robustesse (P0)
Camp/combat, ferveur hors ligne, workers coffre et clé, deep links, import validé,
plafond de pas/minute, tests de régression.

### Sprint 2 — rétention (P1)
Onboarding, simulation hors ligne complète, notifications par catégorie, retour après
plusieurs jours.

### Sprint 3 — progression
Jalons complets + compteurs de vie, facteur de profondeur après le palier 10,
rééquilibrage, campement complet.

### Sprint 4 — objets
Forge, objets non identifiés, sets, reroll, filtres/tri, loadouts, coffre de camp listé.

### Sprint 5 — contenu
Biomes illustrés, météo, rencontres enrichies, modificateurs de boss, plus de pools,
journal de légende, succès.

### Sprint 6 — industrialisation
Room ou migrations, CI release signée, tests UI, accessibilité, localisation,
grands écrans, politique de confidentialité.

---

## 10. Critères d’acceptation transverses

- Aucun pas compté deux fois, même après changement de fuseau ou réimport.
- Toute action destructive demande confirmation.
- Toute erreur réseau a une action de sortie (réessayer, remplacer la clé, plus tard).
- L’app reste utilisable hors ligne et sans clé Garmin.
- Aucune notification si l’utilisateur les a désactivées.
- Aucune régression des données lors d’une mise à jour d’APK.
- Tests unitaires verts et CI verte avant chaque release.
