# Analyse du début de jeu — expériences simulateur

> **Statut : expérimentation, aucune constante du jeu n’a été modifiée.**
> Sauvegarde testée : build **LOW** actuel, palier **9**, niveau **24**.
> Matrice complète : [`analyse-earlygame-data.md`](analyse-earlygame-data.md).
> Outil : `tools/equilibrage/earlygame_experiment.py`.
> Méthode : courbe `code`, 800 combats par scénario, modificateurs automatiques, sans potion.

## Contexte

Le build HIGH précédent a été écarté : il contient des objets legacy migrés, hors budget. Le build LOW est, lui, cohérent avec l’implémentation actuelle :

- arme légendaire niveau 10 à **87 ATQ**, pour ~86 ATQ attendus au palier 10 ;
- armure niveau 3–5, talisman niveau 9 ;
- **212 ATQ**, **684 PV**, 47 % crit, 20 % réduction, aucun lifesteal.

Son seul écart est le **niveau 24 au palier 9**. Le modèle d’XP actuel le reproduit exactement : avec ~7 combats normaux par palier et les gardiens à ×10 XP, on atteint le niveau 24 au palier 9. Ce n’est donc pas une sauvegarde trafiquée, mais le rythme d’XP actuel.

## Constat de base

| Palier | Monstre | Gardien | PV restants | Rations |
|---:|---|---|---:|---:|
| 9 | 0,3 s | 2,9 s | 632 / 684 (92 %) | 0 |
| 12 | 0,7 s | 4,5 s | 547 / 684 (80 %) | 0 |
| 15 | 1,4 s | 7,2 s | 445 / 684 (65 %) | 0,57 |

Le palier 9 est trivial. Le palier 15 commence à peine à compter. La courbe ennemie early game est la principale responsable : les paliers 1 à 19 utilisent encore l’ancienne courbe, la courbe cible ne prenant le relais qu’entre 20 et 35.

## Levier 1 — courbe ennemie early game

Multiplicateur dégressif jusqu’au palier 20, puis retour à 1.

| Scénario | Palier 9 | Palier 12 | Palier 15 |
|---|---|---|---|
| **Actuel** | 2,9 s · 92 % PV | 4,5 s · 80 % | 7,2 s · 65 % |
| **Early doux** : PV monstres ×3, gardiens ×2,5, ATK ×1,5 | **5,6 s · 77 %** | **7,6 s · 64 %** | **10,7 s · 37 %** (98 % de victoires) |
| **Early moyen** : ×4 / ×3 / ×1,75 | 6,5 s · 67 % | 8,8 s · 64 % | 11,6 s · 21 % (81 % de victoires) |
| **Early fort** : ×5 / ×4 / ×2 | 8,3 s · 53 % | 11,2 s · 53 % | 12,3 s · 5 % (30 % de victoires) |

La formule exacte testée est :

```text
facteur(palier) = 1 + (maximum - 1) × (1 - palier / 20)
```

Au palier 9, « early doux » applique donc réellement ×2,1 aux PV des monstres, ×1,825 aux PV des gardiens et ×1,275 à l’ATK. Au palier 15 : ×1,75 / ×1,375 / ×1,125. Au palier 20 : ×1.

**Lecture :**

- **Early doux** transforme le gardien du 9 en combat de 5,6 s avec 23 % de PV perdus, sans rendre le 15 injuste (98 % de victoires). C’est le meilleur premier candidat.
- **Early moyen** rend le 15 risqué (81 % de victoires) avec le stuff niveau 10 actuel. Un stuff naturel niveau 15 serait sans doute plus solide, mais il faut le vérifier sur une sauvegarde fraîche.
- **Early fort** casse le palier 15 : 30 % de victoires. À écarter tel quel.

## Levier 2 — écart de niveau

Les ennemis gagnent des PV/ATQ quand le héros dépasse le palier de plus de 4 niveaux.

| Scénario | Palier 9 | Palier 12 | Palier 15 |
|---|---|---|---|
| **Écart 3 %/niv, cap ×1,5** | 4,0 s · 84 % | 5,7 s · 66 % | 8,6 s · 55 % |
| **Écart 4 %/niv, cap ×1,6** | 4,4 s · 81 % | 6,0 s · 64 % | 9,0 s · 47 % |
| **Early doux + écart 3 %** | 7,6 s · 61 % | 9,9 s · 58 % | **49 % de victoires** |
| **Early moyen + écart 3 %** | 8,9 s · 71 % | 11,5 s · 36 % | **21 % de victoires** |

L’écart de niveau seul reste insuffisant au palier 9 (4,0–4,4 s), mais il agit sans toucher au build `reference` (qui est au niveau du palier). En revanche, **cumulé à un early moyen ou fort, il devient trop punitif** : le palier 15 tombe à 49 %, puis 21 % de victoires. Il ne faut pas empiler les deux leviers sans mesure.

## Levier 3 — rythme d’XP

Modèle calibré sur la sauvegarde : ~7 combats normaux par palier, gardien ×10.

| Règle XP | Niv. 5 | Niv. 9 | Niv. 15 | Niv. 20 | Niv. 25 | Niv. 30 | Niv. 36 |
|---|---:|---:|---:|---:|---:|---:|---:|
| **Actuel · gardien ×10** | 14 | **24** | 36 | 44 | 50 | 55 | 61 |
| Gardien ×5 | 11 | 21 | 32 | 40 | 46 | 51 | 57 |
| Actuel · malus surlevel −50 % | 13 | 19 | 29 | 36 | 42 | 47 | 53 |
| Gardien ×5 · malus −50 % | 11 | 17 | 26 | 33 | 38 | 44 | 49 |
| Gardien ×5 · malus −75 % | 11 | 15 | 21 | 26 | 32 | 36 | 43 |

Même la variante la plus sévère reste au niveau 15 au palier 9, et au niveau 43 au palier 36. **L’XP seule ne ramènera pas le héros au niveau du palier.** Si le surlevel est assumé, il faut plutôt faire évoluer les ennemis avec l’écart de niveau. Si le niveau doit suivre le palier, il faudra une refonte plus large de l’XP, pas seulement un ajustement du multiplicateur de gardien.

## Recommandation

### Premier passage à tester

**Appliquer uniquement « early doux »** :

- PV monstres : ×3 maximum au palier 1, retour à ×1 au palier 20 ;
- PV gardiens : ×2,5 maximum, retour à ×1 au palier 20 ;
- ATK : ×1,5 maximum, retour à ×1 au palier 20.

Pourquoi ce choix :

- il corrige le palier 9 sans toucher aux paliers 20+ ;
- il laisse le palier 15 exigeant mais gagnable (98 %) ;
- il ne cumule pas avec un malus de niveau, donc on peut mesurer son effet seul ;
- il ne modifie ni l’XP ni le stuff, donc pas de régression économique.

### Ce qu’il faut vérifier ensuite

1. **Jouer une sauvegarde fraîche de 1 à 20** avec ce réglage, en relevant à chaque palier :
   - niveau du héros ;
   - niveau moyen de l’équipement ;
   - TTK monstre et gardien ;
   - PV perdus ;
   - rations consommées.
2. Si le palier 9 reste trop facile, passer à **early moyen**, mais surveiller le palier 15.
3. Si le surlevel continue de poser problème plus tard, tester **un écart de niveau léger (2–3 %/niv, cap ×1,4–1,5)** seul, sans early fort.
4. Ne pas toucher au late game : le profil `reference` est déjà à 58 % au palier 36, 44 % au 50, 37 % au 58.
5. Ne pas calibrer sur le build HIGH : ses objets legacy faussent tout.

### Ce qui reste hors de ce lot

- l’intégration des multiplicateurs dans `makeMob`/`makeBoss` ;
- la décision produit sur le rythme d’XP ;
- la refonte éventuelle des objets legacy ;
- la courbe 36–100.

Tant que ces décisions ne sont pas prises, le simulateur permet d’itérer sans modifier le jeu.
