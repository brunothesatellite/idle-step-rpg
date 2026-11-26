# Analyse de progression 0.24.0 — déblocage

> Simulation des paliers 1 → 101 après le lot 1 (auto-optimisation, ténacité,
> butin adapté au niveau, élites adoucies, early curve resserrée, pitié renforcée).
> 5 graines par couple profil × pas/jour, médianes.

## 1. Résultat principal

| Profil | 5 000 pas/j | 8 000 pas/j | 15 000 pas/j | Replis 5k / 8k / 15k | Niv. final |
|---|---:|---:|---:|---:|---:|
| **Passif (auto)** | 32,3 j (5/5) | 22,3 j (5/5) | 12,0 j (5/5) | 131 / 198 / 204 | 96 |
| **Normal** | 30,8 j (5/5) | 20,2 j (5/5) | 13,0 j (5/5) | 92 / 129 / 260 | 97 |
| **Optimisé** | 31,8 j (5/5) | 21,5 j (5/5) | 11,2 j (5/5) | 50 / 88 / 102 | 96 |
| **Hyper-optimisé** | 36,0 j (5/5) | 22,0 j (5/5) | 11,5 j (5/5) | 28 / 16 / 31 | 98 |

Le passif **termine désormais 5/5 à tous les rythmes**, contre 0/5 et un mur au palier 21 en 0.23.0. Le normal passe de 46,3 j / 358 replis à 30,8 j / 92 replis à 5 000 pas/j.

## 2. Avant / après à 5 000 pas/j

| Profil | 0.23.0 | 0.24.0 | Gain |
|---|---:|---:|---:|
| Passif | non fini, mur p.21, 4 177 replis | 32,3 j, 5/5, 131 replis | finissable |
| Normal | 46,3 j, 358 replis | 30,8 j, 92 replis | −33 % de jours, −74 % de replis |
| Optimisé | 38,0 j, 215 replis | 31,8 j, 50 replis | −16 % de jours, −77 % de replis |
| Hyper | 40,1 j, 103 replis | 36,0 j, 28 replis | −10 % de jours, −73 % de replis |

## 3. TTK gardien à 8 000 pas/j

| Palier | Passif (auto) | Normal | Optimisé | Hyper |
|---:|---:|---:|---:|---:|
| 10 | 7,3 s (100 %) | 13,5 s (100 %) | 5,0 s (100 %) | 6,7 s (100 %) |
| 20 | 2,4 s (100 %) | 4,5 s (100 %) | 2,5 s (100 %) | 1,5 s (100 %) |
| 30 | — (0 %) | 10,6 s (32 %) | 7,2 s (12 %) | 6,7 s (100 %) |
| 40 | 18,9 s (34 %) | 14,7 s (100 %) | 9,8 s (100 %) | 5,4 s (100 %) |
| 50 | 21,6 s (99 %) | 16,7 s (100 %) | 8,0 s (100 %) | 7,5 s (100 %) |
| 60 | 24,3 s (58 %) | 17,7 s (100 %) | 9,5 s (100 %) | 5,3 s (100 %) |
| 70 | 23,5 s (31 %) | 15,4 s (100 %) | 8,6 s (100 %) | 5,5 s (100 %) |
| 80 | 17,2 s (100 %) | 18,5 s (100 %) | 7,3 s (100 %) | 5,0 s (100 %) |
| 90 | 11,7 s (100 %) | 13,2 s (100 %) | 7,1 s (100 %) | 5,1 s (100 %) |
| 100 | 12,8 s (100 %) | 15,1 s (100 %) | 8,4 s (100 %) | 6,0 s (100 %) |

Le milieu de partie reste exigeant pour le passif et le normal (gardiens 40–70 à 15–24 s, avec des retours), mais les replis restent gérables grâce à la ténacité et à la pitié. L’hyper est à 100 % partout ; son gardien final à 6 s reste le point à traiter au lot 2.

## 4. Ce que le lot 1 a corrigé

- **Finissabilité** : le passif termine avec l’auto-optimisation et les soins automatiques.
- **Rythme de marche** : les replis du normal passent de 358 à 92 à 5 000 pas/j ; à 15 000, ils restent plus élevés (260), mais le jeu n’est plus bloqué.
- **Pics de gardiens** : l’early curve resserrée et la pitié à −15 % suppriment les murs de 20–30 tentatives.
- **Élites** : les élites ne provoquent plus de boucles de mort pour le normal ; le TTK élite reste supérieur au monstre coriace.
- **Butin** : le butin adapté au niveau donne un stuff utilisable aux joueurs qui avancent plus vite que leur palier.

### Test sur appareil

Au premier lancement de la debug 0.24.0, l’auto-optimisation a, en un passage : identifié 1 objet, forgé l’arme de +8 à +11, acheté 2 niveaux de camp et repris 2 objets du coffre. Le coffre présent au portail n’a **pas** été ouvert automatiquement, pour ne pas bloquer la gate. Le héros est passé de 141 à 190 ATQ, l’or de 1 430 à 415 et les éclats de 24 à 20. La sauvegarde a été migrée en v8 sans erreur.

## 5. Points encore ouverts (lot 2)

1. **Gardien final trop court pour l’hyper** : 6,0 s à 8 000 pas/j, 4–6 s selon les runs. Objectif 10–15 s.
2. **Pics passif/normal aux paliers 30–40 et 60–70** : les gardiens restent à 0–58 % à l’arrivée ; la ténacité/pitié les rendent finissables, mais le ressenti reste irrégulier.
3. **Rythme rapide** : à 15 000 pas/j, le normal a encore 260 replis. Le butin adapté a réduit le problème, sans le supprimer.
4. **Économie** : l’or et les rations restent abondants ; les sinks du lot 2 sont toujours pertinents.

## 6. Prochain lot

- Gardien final : PV ×1,8–2,0, ATQ ×1,25–1,30, phase 2 unique.
- Reroll du modificateur au portail, potions au camp, respec.
- Ajustement des rations si les rapports confirment le plafond 30/30 permanent.
- Nouvelle simulation 1 → 101 et test de partie fraîche avant release 0.25.0 / 66.
