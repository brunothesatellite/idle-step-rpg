# DungeonStep — La Marche des Âmes v0.21.2

> Première release publiée depuis **v0.19.1**. Elle regroupe les lots 0 à 10 du plan d'équilibrage, l'écran de fin enrichi, l'export HTML, la localisation FR/EN et la calibration des gardiens.

## Points forts

### Équilibrage 1 → 100

- **XP** : courbe `1,09^niveau`, récompenses linéaires par palier, rattrapage ×1,25.
- **Butin** : stats ×1 à ×6 selon le palier, raretés 1,15 / 1,5 / 1,9 / 2,4, upgrades additifs +4 % (20 niveaux max).
- **Camp et sets** : bonus en pourcentage, coût indexé sur le palier, plafond 30 niveaux.
- **Rations** : cap 30, revenus renforcés, auto-ration 40 %/45 %, repos gratuit 2×20 %, farm +10 % PV.
- **Gardiens** : niveau palier +1, bouclier 35 %/3 s/8 s, enrage 25 s, phase 2 télégraphiée, pitié −1 %/défaite (cap −3 %), portail à 90 m.
- **Calibration 0.21.1/0.21.2** : TTK visé 18 s, ATQ par capacité, potions d'ATQ à risque, `reference` mesuré à 44–60 % sans potion.
- **Farm et pity** : rare garanti tous les 5 kills, set tous les 10 gardiens, farm de gardien +1 éclat/+1 ration.
- **Fin de jeu** : Trône brisé +15 % → +35 %, boss final **L'Âme Première**, fin unique au palier 100, mode sans fin, télémétrie locale.

### Écran de fin et export

- Diorama avec la **vraie statue du palier 100**, l'**aura dorée** et le gardien déchu.
- Dossier du héros, progression, télémétrie, gardiens 81–100, économie, équipement, collections et jalons.
- **Export HTML autonome** (fonte pixel embarquée), enregistré via le sélecteur Android.
- **Aperçu de la fin** dans les réglages des builds debug.

### Localisation et qualité

- Interface, journal, Codex, notifications, widget et manuel en **français et anglais**.
- Noms d'objets fusionnés traduits (`Gnoll Axe (fused ×3)`).
- Correctif VIT : intervalle minimum **180 ms** réellement appliqué.
- 151 tests unitaires, lint OK.

## Installation

- **APK** : `La-Marche-des-Ames-v0.21.2.apk` (Android 14+).
- **AAB** : `La-Marche-des-Ames-v0.21.2.aab` pour la distribution Play.
- Les mises à jour installées avec la même signature **conservent les données**.
- Application sideloadée, sans publicité, sans achat intégré, sans serveur éditeur.

## Playtest

La fiche de suivi est dans [`docs/playtest-0.21.2.md`](https://github.com/brunothesatellite/idle-step-rpg/blob/main/docs/playtest-0.21.2.md) : journal quotidien, points à noter et exports à J+3/J+7/J+14.

Points connus à surveiller : `under` à 0–37 % selon le palier, sur-stuff à 100 %, et le ressenti des potions à risque.

## English summary

First public release since v0.19.1. It includes the full 1→100 balance pass, the enriched ending screen with a standalone HTML report, French/English localisation, the 180 ms VIT fix, and guardian calibration (18 s TTK, ability-based ATK, risk/reward attack potions). 151 unit tests, lint OK. Android 14+.

---

Journal complet : [`CHANGELOG.md`](https://github.com/brunothesatellite/idle-step-rpg/blob/main/CHANGELOG.md).
