# La Marche des Âmes

**Maquette interactive d'un idle step RPG fantasy en pixel art.**
Les pas réels du joueur font avancer un pèlerin dans un donjon infini. Le héros marche seul, combat seul, tombe parfois — et se relève toujours vingt mètres plus tôt, là où le farm est plus doux.

> **Prototype de démonstration.** Aucune donnée de marche réelle n'est lue : la maquette simule le compteur de pas pour rendre la boucle jouable immédiatement.

**Voir la maquette en ligne :** [brunothesatellite.github.io/idle-step-rpg](https://brunothesatellite.github.io/idle-step-rpg/)

**Dépôt :** [github.com/brunothesatellite/idle-step-rpg](https://github.com/brunothesatellite/idle-step-rpg)

---

## Lancer la maquette

1. Ouvrir `index.html` dans un navigateur récent (Chrome, Edge, Firefox).
2. C'est tout. Aucun serveur, aucune installation, aucune dépendance externe : les sprites et les polices sont locaux.

La maquette est optimisée pour un écran mobile portrait **390 × 844**. Sur ordinateur, elle s'affiche dans un cadre de téléphone avec une présentation à gauche. En dessous de 960 px de large, la présentation disparaît et l'écran devient plein cadre.

---

## Ce que la maquette démontre

### La scène (haut)

- Donjon en pixel art avec parallaxe, arches, statues, torches, bannières, chaînes, brume et braises.
- Héros animé (marche, frappe, aura de ferveur, épée et bouclier).
- Monstres variés (gobelins, kobolds, gnolls, orcs, squelettes, nagas, manticores, goules, élémentaires…), dont des **élites**.
- Rencontre automatique tous les **10 m**, gardien tous les **100 m**.
- Barre de palier avec tête de gardien, nom du biome, or, jauge de ferveur et compteur de pas.
- Porte runique, présage de gardien, bannières de combat, nombre de dégâts, butin flottant, écran de repli.

### La boucle de pas

- **10 pas = 1 mètre** ; **1 000 pas = 1 palier**.
- Le bouton **Synchroniser les pas** ajoute les pas en attente, remplit la ferveur et convertit les seuils en rations et éclats d'âme.
- Les pas alimentent aussi la **réserve de marche** : les mètres sont consommés progressivement, combat après combat.
- Une simulation de marche lente entretient la scène entre deux synchronisations.

### Les combats et la défaite

- Combats entièrement automatiques : cadence, critiques, régénération, ration d'urgence à 25 % des PV, phase 2 du gardien.
- En cas de défaite, le héros recule de **20 m**, revient à 60 % de ses PV et entre en **farm de halte** (ennemis affaiblis, butin réduit, soin à chaque kill).
- Le farm se déclenche réellement dans la maquette si le héros tombe. Il est aussi accessible par un raccourci de revue (`#farm`).

### Le campement

- Accessible à tout moment depuis le bouton flamme de la scène.
- L'expédition est mise en pause, les pas réels continuent de s'accumuler et sont appliqués à la reprise.
- Soin par ration (+40 % PV) ou par éclat d'âme (PV complets).
- Journal des dernières braises, bilan de la veillée, conseil du feu.
- Gestion de l'équipement accessible depuis le camp.

### Le tableau de bord (bas)

- **Stats** : puissance, ATQ, PV, VIT, ferveur détaillée, serments du jour, journal.
- **Inventaire** : 6 emplacements équipés, butin, comparaison avant équipement, vente d'objet et vente en masse des communs.
- **Améliorations** : trois pistes (Lame affûtée, Souffle ancien, Pas vifs), coûts croissants, or, aperçu du prochain gardien.
- **Bouton de synchronisation** pleine largeur, avec pas en attente et dernière synchro.

---

## Interactions à essayer

| Action | Résultat |
|---|---|
| **Synchroniser les pas** | Ajoute ~1 240 pas, remplit la ferveur, peut générer rations/éclats et déclencher les combats. |
| Attendre quelques secondes | Le héros atteint 70 m et déclenche un combat automatique. |
| **Coffre doré** sur le sol | Ouvre la fiche de serrure ; « Consacrer mes pas » lance le forçage et fige la marche. |
| **Onglet Inventaire** | Toucher « Épée ancienne » ouvre la comparaison ; « Équiper » change les statistiques et l'arme du héros. |
| **Onglet Améliorations** | Acheter une amélioration dépense l'or, augmente la statistique et le coût suivant. |
| **Bouton feu de camp** | Ouvre le camp, met le jeu en pause, permet de se soigner ou de gérer l'équipement. |
| **Gérer l'équipement** (camp) | Ferme le camp et bascule sur l'inventaire. |
| **Vendre les communs** | Vend tous les objets communs du butin. |
| Attendre le gardien | À 100 m, le gardien scelle le palier ; le vaincre ouvre le palier suivant. |
| **Échap** | Ferme le camp ou la fiche ouverte. |

### Raccourcis de revue

Pour montrer un état précis sans attendre, ouvrir l'URL avec un fragment :

- `index.html#camp` — campement ouvert
- `index.html#inventory` — onglet Inventaire
- `index.html#upgrades` — onglet Améliorations
- `index.html#chest` — fiche du coffre de démonstration
- `index.html#sheet` — fiche de l'Épée ancienne
- `index.html#fight` — combat immédiat à 70 m
- `index.html#boss` — gardien du palier à 100 m
- `index.html#farm` — zone de repli et farm de halte

---

## Ce qui est simulé volontairement

Cette maquette est une **tranche verticale de présentation**, pas un build de production :

- les pas sont simulés (pas de capteur réel) ;
- les combats sont raccourcis pour être observables en quelques secondes ;
- le farm de halte dure trois ennemis au lieu de vingt minutes ;
- pas encore de sauvegarde, de reset quotidien à 04:00 ni de récupération hors ligne détaillée ;
- l'équilibrage est celui de la démonstration, pas la courbe finale.

Les règles complètes, les formules, les tableaux d'équilibrage, les cas limites, l'accessibilité et les critères d'acceptation sont dans **[docs/specifications-fonctionnelles.md](docs/specifications-fonctionnelles.md)**.

---

## Arborescence

```
idle-step-rpg/
├── index.html                     # la maquette
├── styles.css                     # direction artistique, scène, tableau de bord
├── game.js                        # simulation, combats, camp, inventaire
├── docs/
│   └── specifications-fonctionnelles.md
├── assets/
│   ├── hero/                      # héros et pièces d'équipement visuel
│   ├── mon/                       # monstres et gardiens
│   ├── env/                       # murs, sols, statues, portes, arbres
│   ├── item/                      # armes, armures, consommables, or
│   ├── fx/                        # cœurs, flammes
│   ├── fonts/                     # Pixelify Sans, Press Start 2P
│   └── CREDITS.md                 # licences et sources
└── README.md
```

---

## Credits & licences

- **Pixel art** : [Dungeon Crawl Stone Soup — tiles](https://github.com/crawl/tiles), export `releases/Nov-2015`, publié sous **CC0 1.0**. Voir `assets/CREDITS.md` et le README du dépôt source pour les tuiles exclues de la réutilisation.
- **Polices** : **Pixelify Sans** et **Press Start 2P**, Google Fonts, **SIL Open Font License 1.1**. Textes complets dans `assets/fonts/licenses/`.
- **Code, textes et direction artistique originale** : © 2026 brunothesatellite — tous droits réservés, publication de démonstration. Voir `LICENSES.md`.
- **Aucune ressource propriétaire** n'est utilisée.

Le récapitulatif complet des licences est dans **[LICENSES.md](LICENSES.md)**.

---

*La Marche des Âmes — maquette de game design, 2026.*
