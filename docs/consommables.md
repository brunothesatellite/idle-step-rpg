# Consommables — La Marche des Âmes

Les consommables se ramassent sur les monstres et dans les coffres. Ils s'utilisent
depuis le **SAC** (bouton en haut à droite de la scène), depuis l'onglet
**Inventaire → Consommables**, ou depuis le bouton **POTIONS** du camp.

## Soins et ferveur (camp et exploration)

| Icône | Nom | Rareté | Effet | Valeur |
|---|---|---|---|---|
| Potion rouge | **Potion de soin** | Commun | Rend 30 % des PV max. | 30 or |
| Herbe | **Herbe de lune** | Commun | Rend 12 % des PV et 6 ferveur. | 35 or |
| Champignon orange | **Champignon de braise** | Inhabituel | Rend 20 ferveur. | 40 or |
| Fiole bleue | **Élixir du veilleur** | Rare | Restaure tous les PV. | 120 or |
| Pain | **Pain de route** | Commun | Rend 20 % des PV et 1 ration. | 25 or |

## Combat (uniquement pendant un combat)

| Icône | Nom | Rareté | Effet | Valeur |
|---|---|---|---|---|
| Orbe rouge | **Fiole de rage** | Inhabituel | ATQ +50 % pendant 20 s. | 60 or |
| Bouclier | **Écorce de fer** | Inhabituel | Dégâts subis −40 % pendant 20 s. | 65 or |
| Éclair | **Poudre d'éclair** | Inhabituel | VIT +60 % pendant 15 s. | 60 or |
| Bombe | **Bombe de feu** | Rare | Inflige immédiatement 2× votre ATQ. | 90 or |
| Potion violette | **Élixir de vampire** | Rare | 20 % des dégâts rendus en PV pendant 30 s. | 110 or |
| Parchemin | **Parchemin critique** | Rare | Critique +50 % pendant 15 s. | 95 or |
| Champignon rouge | **Champignon berserk** | Rare | ATQ +80 %, mais dégâts subis +20 % pendant 15 s. | 100 or |

## Où les trouver

- **Monstres normaux** : 18 % de chance, 1 objet.
- **Gardiens** : 1 à 2 objets garantis, meilleure chance de rareté.
- **Coffres** : 1 objet (bois), 1 à 2 (fer), 2 à 3 (runique).
- **Départ** : 2 potions de soin et 1 herbe de lune.

## Affichage

- Les **rations** sont visibles dans la carte **RESSOURCES** de l'onglet Stats
  (`RATIONS 3 / 24`), dans l'en-tête et dans le camp.
- Les **buffs actifs** apparaissent à droite de la scène avec leur compte à rebours
  (`RAGE 18s`, `ÉCORCE 12s`).
- Le **SAC** affiche le nombre total de consommables.

## Fichiers

Icônes : `assets/item/cons_*.png` et `android/app/src/main/assets/item/cons_*.png`.
Génération : `tools/generate_consumable_icons.ps1`.
