# DungeonStep — La Marche des Âmes v0.25.0

> Lot 2 : gardien final mémorable, économie de camp, auto-optimisation désactivée par défaut, onboarding et manuel mis à jour.

## Gardien final

- **Palier 100** : L’Âme Première adapte ses PV au DPS du héros pour viser **20–30 s** de combat. Le multiplicateur reste borné (0,75–3,5) pour éviter les combats triviaux ou infaisables.
- **Trois phases** : 100–66 %, 66–33 %, 33–0 %. L’ATQ augmente de **+8 %** puis **+12 %**, et la capacité s’accélère (6,5 s puis 5,8 s).
- **RUPTURE DU TRÔNE** : 230 % de l’ATQ, −20 ferveur et 3 % de PV rendus.
- Les multiplicateurs finaux sont maintenant **réellement appliqués** : ils étaient auparavant annulés par la courbe cible au palier 100.
- Test réel sur Pixel 7a : **30,4 s**, phase 2 atteinte, victoire, écran « Le Trône est brisé ».

## Économie et camp

- **Auto-optimisation désactivée par défaut** : elle reste disponible dans **Réglages → Inventaire**, avec les coffres et soins automatiques.
- **Relance du modificateur** au portail : une fois par gardien, coût `200 + 30 × palier` or.
- **Marchand de camp** : potions de soin, écorce de fer, potion de force, élixir du veilleur ; prix indexé sur le palier.
- **Respec du camp** : rembourse **70 %** de l’or investi et remet Lame, Souffle et Pas vifs à zéro.
- **Camp** : le marchand est replié par défaut et la carte est plafonnée en hauteur ; la tente et le feu restent visibles.

## Onboarding et manuel

- La page 1 de l’onboarding précise que le héros avance seul, mais que **les grandes décisions restent au joueur** : équiper, forger, camper, oser le gardien.
- L’auto-optimisation est présentée comme une **option**, pas une obligation : « Pas besoin d’optimiser chaque détail — et si vous préférez, l’auto-optimisation peut prendre le relais dans les réglages. »
- Le manuel **FR/EN** reprend la même nuance dans « Découvrir le jeu », avec une capture de la page 1.

## Données et compatibilité

- **Télémétrie v5** : phase finale, or investi dans le camp, nouveaux compteurs.
- **Sauvegarde v9** : migration automatique ; les sauvegardes v8 restent lisibles.
- **178 tests** unitaires, lint Android OK, CI verte.

## Simulation

- Analyse complète **5 profils × 3 rythmes × 5 graines**, paliers 1 → 101.
- Le **passif minimal** (auto-optimisation off) ne termine pas : mur aux paliers 31–34.
- Le **passif optimisé** termine 5/5, mais son gardien final monte à **35 s** à 8 000/15 000 pas/j.
- Les sinks économiques sont **modélisés mais marginaux** : 0–2 relances, 0 potion achetée, 1 respec par run.
- Rapport complet : [`docs/analyse-progression-025.md`](https://github.com/brunothesatellite/idle-step-rpg/blob/main/docs/analyse-progression-025.md).

## Installation

- **APK** : `La-Marche-des-Ames-v0.25.0.apk` (Android 14+).
- **AAB** : `La-Marche-des-Ames-v0.25.0.aab` pour la distribution Play.
- **SHA-256 APK** : `8d8440824ed35cf56e46d4ce61a480426a4637ea6c794f5dd38ae1d835d59c08`
- Les mises à jour installées avec la même signature **conservent les données**.
- Application sideloadée, sans publicité, sans achat intégré, sans serveur éditeur.
- Manuel en ligne : [FR](https://brunothesatellite.github.io/idle-step-rpg/manuel/) · [EN](https://brunothesatellite.github.io/idle-step-rpg/manuel/en/)

### Aperçu de l’onboarding

| Français | English |
|---|---|
| ![Onboarding FR](https://github.com/brunothesatellite/idle-step-rpg/releases/download/v0.25.0/onboarding-fr.jpg) | ![Onboarding EN](https://github.com/brunothesatellite/idle-step-rpg/releases/download/v0.25.0/onboarding-en.jpg) |

## Points connus

- Le **passif minimal** reste non finissable sans intervention ; un signal d’aide au premier mur est prévu.
- Le gardien final du **passif optimisé** dépasse la cible à haut rythme ; un plancher de PV final à **0,55** est à l’étude.
- Les sinks d’or (relance, marchand, respec) ne pèsent pas encore assez dans l’économie simulée ; prochain chantier.

## English summary

v0.25.0 adds a memorable final guardian with adaptive HP and three phases (20–30 s target), the camp economy (guardian modifier reroll, camp merchant, 70% camp respec), and disables auto-optimization by default. The onboarding and FR/EN manual now state that the hero walks alone but the big decisions remain the player’s; auto-optimization is an option, not an obligation. Includes telemetry v5, save v9, 178 unit tests and a green CI. Signed APK and Play AAB attached. SHA-256 APK: 8d8440824ed35cf56e46d4ce61a480426a4637ea6c794f5dd38ae1d835d59c08.

---

Journal complet : [`CHANGELOG.md`](https://github.com/brunothesatellite/idle-step-rpg/blob/main/CHANGELOG.md).
