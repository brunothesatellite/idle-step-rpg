# Télémétrie locale et rapport d’équilibrage

> Depuis la version courante, la sauvegarde et un rapport dédié contiennent des
> données de combat locales. Aucun envoi réseau n’est effectué.

## Où trouver les données

- **Sauvegarde** : `Réglages → Sauvegarde → Exporter` (JSON complet).
- **Rapport d’équilibrage** : `Réglages → Sauvegarde → Exporter le rapport d’équilibrage`.
  Le fichier `marche-ames-rapport-equilibrage.json` contient les statistiques,
  l’équipement porté et la télémétrie, **sans journal, inventaire ni coffre**.

Le rapport est le format recommandé pour analyser une sauvegarde : il est plus
léger et ne contient pas les données de narration.

## Rétention

| Donnée | Limite |
|---|---:|
| Derniers combats détaillés | 100 |
| Jours agrégés | 60 |
| Événements de niveau | 200 |
| Paliers agrégés | 200 |

Ces limites bornent la taille du JSON. Les anciens combats sont supprimés au
profit des agrégats par palier et par jour.

## Structure `telemetry`

```json
{
  "schema": 5,
  "normalFights": 120,
  "normalWins": 118,
  "bossFights": 12,
  "bossWins": 10,
  "autoFusions": 9,
  "autoUpgrades": 14,
  "autoCampBuys": 6,
  "autoChests": 11,
  "autoHeals": 23,
  "tenacityMax": 3,
  "recentFights": [
    {
      "at": 1795610000000,
      "floor": 9,
      "biome": "crypts",
      "level": 24,
      "kind": "normal",
      "enemyId": "wight",
      "enemyLevel": 9,
      "elite": false,
      "tier": "mid",
      "win": true,
      "durationMs": 4200,
      "hpStart": 684,
      "hpEnd": 540,
      "damageTaken": 144,
      "healLifesteal": 0,
      "healRegen": 18,
      "healRation": 0,
      "healPotion": 0,
      "rations": 0,
      "potions": [],
      "buffs": [],
      "ferveur": 51,
      "tenacityStacks": 1,
      "bossPity": 0.03,
      "atk": 212,
      "maxHp": 684,
      "vit": 1.51,
      "crit": 47,
      "critDamage": 0.4,
      "regen": 5.4,
      "lifesteal": 0,
      "damageReduction": 0.2,
      "bossDamage": 0.25,
      "enemyHp": 1420,
      "enemyAtk": 38,
      "enemyIntervalMs": 1250,
      "modifier": "",
      "ability": "",
      "finalPhase": 0,
      "heroHits": 9,
      "enemyHits": 5,
      "xpGained": 112,
      "goldGained": 30
    }
  ],
  "floorStats": {
    "9": {
      "attempts": 18,
      "wins": 17,
      "losses": 1,
      "bossAttempts": 1,
      "bossWins": 1,
      "farmAttempts": 0,
      "farmWins": 0,
      "eliteAttempts": 2,
      "eliteWins": 1,
      "totalMs": 61200,
      "totalDamageTaken": 2880,
      "rations": 2,
      "potions": 1
    }
  },
  "levelHistory": [
    {
      "at": 1795610000000,
      "level": 25,
      "floor": 9,
      "xp": 120,
      "atk": 215,
      "maxHp": 702,
      "vit": 1.52,
      "crit": 47,
      "power": 780,
      "gearLevels": [10, 3, 4, 5, 3, 9]
    }
  ],
  "days": {
    "2026-11-25": {
      "day": "2026-11-25",
      "fights": 62,
      "wins": 61,
      "losses": 1,
      "bossFights": 8,
      "bossWins": 8,
      "totalMs": 112600,
      "damageTaken": 2100,
      "rations": 1,
      "potions": 0,
      "levelStart": 18,
      "levelEnd": 24
    }
  }
}
```

## Champs utiles pour l’analyse

- **TTK** : `recentFights[].durationMs` pour les victoires.
- **TTD** : `durationMs` pour les défaites (`win = false`).
- **PV perdus** : `hpStart - hpEnd`, ou `damageTaken`.
- **Sustain** : `healLifesteal`, `healRegen`, `healRation`, `healPotion`.
- **Build réel au combat** : `atk`, `maxHp`, `vit`, `crit`, `critDamage`,
  `regen`, `lifesteal`, `damageReduction`, `bossDamage`.
- **Ennemi** : `enemyHp`, `enemyAtk`, `enemyIntervalMs`, `modifier`, `ability`.
- **Progression** : `levelHistory` et `days[].levelStart/levelEnd`.
- **Par palier** : `floorStats` pour les taux de victoire et les coûts cumulés.
- **Replis** : `retreats` à la racine du rapport, et `floorStats[].losses` /
  `days[].losses` pour les défaites. Une défaite accorde 25 % de l’XP du combat
  (`recentFights[].xpGained` reflète déjà ce montant).
- **Ténacité** : `recentFights[].tenacityStacks`, et `tenacityMax` à la racine du
  rapport. Chaque pile vaut +5 % ATQ/PV (max 5).
- **Pitié** : `recentFights[].bossPity` et `bossPity` à la racine ; −3 %/défaite,
  plafond −15 %.
- **Élites** : `floorStats[].eliteAttempts` / `eliteWins`.
- **Auto-optimisation** : `autoFusions`, `autoUpgrades`, `autoCampBuys`,
  `autoChests`, `autoHeals` dans `telemetry`, et `autoOptimize` à la racine.
- **Soin vampire** : plafonné à 3 % des PV max par seconde ; `healLifesteal`
  mesure le soin réellement rendu.

## Confidentialité

- Tout est **local** : aucun réseau, aucune permission supplémentaire.
- Le rapport dédié n’embarque ni journal, ni inventaire, ni coffre, ni clé Garmin.
- La sauvegarde complète reste le seul fichier contenant l’état de jeu intégral.

## Compatibilité

- Schéma de télémétrie : **4**.
- Version de sauvegarde : **9**.
- Les anciennes sauvegardes sont complétées avec des champs vides ; les nouvelles
  sauvegardes restent lisibles par les versions qui ignorent les champs inconnus.
