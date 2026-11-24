# Playtest 0.21.2 — fiche de suivi

> **Durée** : 7 jours (porte E), puis 14 jours (porte F) si le bilan est bon.
> **But** : vérifier que la progression 1 → 100 reste fluide, que les gardiens sont exigeants sans être bloquants, et que l'écran de fin / l'export HTML fonctionnent en conditions réelles.

## 1. À faire avant de commencer

1. Installer la **release 0.21.2** (données conservées) et garder le **debug 0.21.2** pour l'aperçu de la fin.
2. Vérifier que la source de pas (Garmin ou Santé Connect) est connectée.
3. Faire un **export de sauvegarde** de référence (Réglages → Sauvegarde → Exporter).
4. Dans le debug : **Réglages → Aperçu de la fin (debug)** et **Exporter les stats · HTML** une fois, pour valider l'écran et le rapport.

## 2. Journal quotidien — 5 lignes

Copier-coller ce bloc chaque soir et le compléter :

```text
J__ — pas : ____ · palier : ____ · niveau : ____ · or : ____ · rations : ____ · éclats : ____
Combats : ____ victoires / ____ · Gardiens : ____ victoires / ____ tentatives
Dernier gardien : palier ____ · capacité/modificateur : ____________ · pitié avant : ____ %
Potion utilisée : oui/non · type : ____________ · défaite(s) : ____ · farm : ____
Ressenti : ____________ · bug : ____________
```

## 3. Tableau de suivi

| Jour | Pas | Palier / niveau | Gardiens (V/T) | Rations / or | Potions / pitié | Ressenti / bug |
|---|---|---:|---:|---|---|---|
| J1 |  |  |  |  |  |  |
| J2 |  |  |  |  |  |  |
| J3 |  |  |  |  |  |  |
| J4 |  |  |  |  |  |  |
| J5 |  |  |  |  |  |  |
| J6 |  |  |  |  |  |  |
| J7 |  |  |  |  |  |  |
| J8 |  |  |  |  |  |  |
| J9 |  |  |  |  |  |  |
| J10 |  |  |  |  |  |  |
| J11 |  |  |  |  |  |  |
| J12 |  |  |  |  |  |  |
| J13 |  |  |  |  |  |  |
| J14 |  |  |  |  |  |  |

## 4. Points à noter absolument

- **Gardiens** : palier, capacité, modificateur, pitié avant le combat, potion utilisée, victoire/défaite, durée ressentie, nombre de coups.
- **Paliers 36 / 50 / 75 / 100** : noter le stuff, les PV/ATQ, le nombre d'essais et si un farm a été nécessaire.
- **Potions à risque** : la Potion de force et la Fiole de rage doivent accélérer le combat sans le rendre automatique. Noter si la fragilité se ressent.
- **Pitié** : après 2–3 défaites, noter si le combat devient plus juste sans devenir trivial.
- **Rations / or / éclats** : noter toute famine ou accumulation sans usage.
- **Écran de fin** : à 100, vérifier le diorama, les panneaux, l'export HTML et le mode sans fin.
- **Bugs** : écran, texte FR/EN, combat, sauvegarde, notification, widget.

## 5. Exports à m'envoyer

| Moment | Action |
|---|---|
| J+3 | Exporter la sauvegarde et l'envoyer pour un premier relevé |
| J+7 | Export + bilan de la porte E (paliers gagnés, blocages, ressources) |
| J+14 | Export + bilan de la porte F (1 → 100, dead-ends, plaisir) |

L'export se fait dans **Réglages → Sauvegarde → Exporter** (fichier JSON). Je peux ensuite analyser les paliers, les combats, les ressources et les gardiens.

## 6. Critères de réussite

- +5 paliers sur 7 jours, ou blocage expliqué par un choix (stuff, farm, potions).
- Aucun gardien gagné en moins de 10 s ou moins de 30 coups.
- Aucune situation où le joueur ne peut ni farmer, ni camper, ni attendre.
- Potions utiles mais non automatiques ; pitié visible mais non automatique.
- Écran de fin et export HTML utilisables sur le téléphone.
- Aucune régression de sauvegarde entre 0.20.x et 0.21.2.
