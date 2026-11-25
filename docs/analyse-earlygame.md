# Analyse du début de jeu — expériences simulateur

> **Statut : expérimentation, aucune constante du jeu n’a été modifiée.**
> Sauvegarde testée : build **LOW** actuel, palier **9**, niveau **24**.
> Matrice complète : [`analyse-earlygame-data.md`](analyse-earlygame-data.md).
> Outil : `tools/equilibrage/earlygame_experiment.py`.

## Correction de méthode

La première matrice utilisait le **stuff figé niveau 3–10 et le niveau 24** aux paliers 12, 15 et 20. C’était trop pessimiste : un joueur qui progresse naturellement monte de niveau et améliore son équipement.

Cette version projette donc :

- **le niveau** avec le rythme d’XP actuel : 24 au palier 9, 31 au 12, 36 au 15, 44 au 20 ;
- **le stuff nivelé** : niveau d’objet = palier, même rareté, sans forge ;
- **le stuff attendu** : niveau d’objet = palier, rareté +1 cran et +5 niveaux de forge ;
- 600 combats par scénario, courbe `code`, modificateurs automatiques, sans potion.

Les deux projections encadrent la réalité : le stuff attendu est plutôt optimiste, le stuff nivelé plutôt conservateur.

## Résultats — stuff attendu

| Scénario | Palier 9 | Palier 12 | Palier 15 |
|---|---|---|---|
| **Actuel** | 2,2 s · 95 % PV | 2,6 s · 93 % | 3,4 s · 85 % |
| **Early doux** | 4,2 s · 87 % | 4,3 s · 83 % | **4,7 s · 74 %** |
| **Early moyen** | 4,9 s · 82 % | 4,9 s · 79 % | **5,2 s · 70 %** |
| **Early fort** | 6,3 s · 70 % | 6,0 s · 70 % | **6,2 s · 61 %** |

Tous les scénarios restent à 100 % de victoires. Le stuff attendu est plus fort que le stuff actuel : l’arme passe niveau 15, l’armure gagne une rareté et cinq niveaux de forge.

## Résultats — stuff nivelé

| Scénario | Palier 9 | Palier 12 | Palier 15 |
|---|---|---|---|
| **Actuel** | 2,9 s · 92 % | 3,4 s · 89 % | 4,3 s · 79 % |
| **Early doux** | 5,5 s · 79 % | 5,5 s · 74 % | **6,0 s · 64 %** |
| **Early moyen** | 6,4 s · 68 % | 6,4 s · 66 % | **6,7 s · 59 %** |
| **Early fort** | 8,2 s · 55 % | 8,1 s · 56 % | **8,0 s · 66 %** |

Le stuff nivelé est le scénario le plus défavorable ; il donne la borne haute de difficulté.

## Ce que cela change

Avec le stuff figé, « early fort » semblait trop violent au palier 15 : 12,3 s et seulement 30 % de victoires. Avec le niveau et le stuff projetés, il devient **6,2 s à 61 % de PV** (stuff attendu) ou **8,0 s à 66 %** (stuff nivelé). C’est le premier réglage qui reste dans la cible **6–8 s et 30–40 % de PV perdus**.

À l’inverse, **early doux et early moyen ne suffisent plus** :

- au palier 15, ils donnent 4,7 s / 74 % et 5,2 s / 70 % avec le stuff attendu ;
- au palier 12, 4,3 s et 4,9 s ;
- le personnage gagne tellement de niveau et de stuff entre 9 et 15 que le multiplicateur dégressif est absorbé.

## Levier écart de niveau

Avec les niveaux projetés, l’écart de niveau devient très fort :

- `écart 3 %/niv, cap ×1,5` au palier 15 : 6,7 s / 64 % (stuff nivelé), mais au palier 20 : **9,2 s et 30 % de victoires** (stuff nivelé) ;
- cumulé à early fort, le palier 15 tombe à **22 % de victoires** (stuff nivelé), 78 % (stuff attendu).

Il ne faut donc **pas empiler** early fort et écart de niveau. L’écart de niveau est un levier pour le surlevel, pas pour remplacer la courbe early game.

## Rythme d’XP

| Règle XP | Niv. 5 | Niv. 9 | Niv. 15 | Niv. 20 | Niv. 25 | Niv. 30 | Niv. 36 |
|---|---:|---:|---:|---:|---:|---:|---:|
| **Actuel · gardien ×10** | 14 | **24** | 36 | 44 | 50 | 55 | 61 |
| Gardien ×5 | 11 | 21 | 32 | 40 | 46 | 51 | 57 |
| Actuel · malus surlevel −50 % | 13 | 19 | 29 | 36 | 42 | 47 | 53 |
| Gardien ×5 · malus −50 % | 11 | 17 | 26 | 33 | 38 | 44 | 49 |
| Gardien ×5 · malus −75 % | 11 | 15 | 21 | 26 | 32 | 36 | 43 |

Même la variante la plus sévère reste au niveau 15 au palier 9. L’XP seule ne ramènera pas le héros au niveau du palier ; si le surlevel est assumé, il faut plutôt un écart de niveau ennemi. Mais ce levier est plus tardif, pas prioritaire pour le début de jeu.

## Recommandation

### Premier passage

**Appliquer « early fort » seul** :

- PV monstres : ×5 maximum, retour à ×1 au palier 20 ;
- PV gardiens : ×4 maximum, retour à ×1 au palier 20 ;
- ATK : ×2 maximum, retour à ×1 au palier 20.

Facteurs réellement appliqués :

| Palier | Monstres | Gardiens | ATK |
|---:|---:|---:|---:|
| 9 | ×3,2 | ×2,65 | ×1,55 |
| 12 | ×2,6 | ×2,2 | ×1,4 |
| 15 | ×2,0 | ×1,75 | ×1,25 |
| 20 | ×1 | ×1 | ×1 |

C’est moins brutal que les maxima ne le suggèrent, et c’est absorbé par la progression naturelle du niveau et du stuff.

### Pourquoi pas early doux/moyen

Ils sont corrects sur le stuff figé, mais trop faibles dès que le niveau et le stuff progressent : 4,7 à 5,2 s au palier 15, avec 70–74 % de PV restants. Le joueur ne sentirait toujours pas la menace.

### Ce qu’il faut vérifier ensuite

1. Jouer une sauvegarde fraîche de 1 à 20 avec early fort et relever à chaque palier : niveau, niveau moyen du stuff, TTK, PV perdus, rations.
2. Vérifier le palier 20 : early fort s’arrête à 20 ; le stuff projeté y donne 4,9–6,4 s et 60–62 % de PV. Si c’est encore trop facile, traiter la transition 20–35 séparément.
3. Ne pas ajouter d’écart de niveau en même temps. Si le surlevel reste un problème après le 20, tester un écart léger (2 %/niv, cap ×1,4), pas 3–4 %.
4. Ne pas toucher au late game : `reference` est déjà à 56 % au 36, 44 % au 50, 37 % au 58.
5. Ne pas calibrer sur le build HIGH : ses objets legacy faussent tout.

### Limites du modèle

- La projection de stuff suppose une progression régulière ; elle ne modélise pas la chance de drops, la fusion ni le farm.
- Le niveau projeté suit le rythme d’XP actuel ; un joueur qui farme davantage sera plus haut.
- Les potions et buffs ne sont pas utilisés.

Le simulateur permet d’ajuster ces hypothèses avant toute modification du jeu.
