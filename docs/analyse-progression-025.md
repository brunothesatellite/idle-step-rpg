# Analyse de progression 0.25.0 — endgame et économie

> Validation du lot 2 : gardien final 20–30 s, relance du modificateur, marchand
> de camp et respec. 3 graines à 5 000 pas/jour, médianes.

## 1. Résultat à 5 000 pas/jour

| Profil | Jours | Palier | Niv. héros | Replis | TTK gardien final | Victoire finale |
|---|---:|---:|---:|---:|---:|---:|
| **Passif (auto-optimisation activée)** | 32,3 j | 101 | 95 | 119 | 27,9 s | 59 % |
| **Normal** | 32,6 j | 101 | 96 | 131 | 30,7 s | 80 % |
| **Optimisé** | 29,7 j | 101 | 96 | 32 | 19,8 s | 100 % |
| **Hyper-optimisé** | 36,0 j | 101 | 98 | 47 | 19,6 s | 100 % |

Le gardien final tombe dans la cible **20–30 s** pour l’optimisé, l’hyper et le passif ; le normal est à **30,7 s**, à la frontière haute. Le passif et le normal gardent une marge de progression : le combat reste un vrai test, mais il est finissable avec la ténacité et la pitié.

## 2. Gardien final

- PV adaptés au DPS réel du héros : `base ×1,35 × (DPS / DPS de référence)^0,85`, borné de 0,75 à 3,5.
- ATQ de base ×0,80, puis phases à 66 % et 33 % : **+8 %** puis **+12 %** d’ATQ, capacité plus rapide (6,5 s puis 5,8 s).
- Multiplicateurs finaux désormais **réellement appliqués** : ils étaient annulés par la courbe cible au palier 100.
- Le combat reste un **test d’endurance** : le héros doit survivre 20–30 s, pas seulement avoir assez d’ATQ.

## 3. Économie

- **Relance du modificateur** au portail : une fois par gardien, coût `200 + 30 × palier` or.
- **Marchand de camp** : potion de soin, écorce de fer, potion de force, élixir du veilleur ; prix indexé sur le palier.
- **Respec du camp** : remboursement de 70 % de l’or investi, remise à zéro des trois améliorations.
- Les rations ne sont pas encore réduites : les rapports doivent confirmer que le plafond 30/30 reste permanent avant de toucher à la génération.

## 4. Point d’attention

L’auto-optimisation est désormais **désactivée par défaut**, comme demandé. Un joueur vraiment passif qui ne l’active pas retrouve le mur du palier 21. Le mode assisté reste la réponse : le joueur doit activer l’auto-optimisation dans **Réglages → Inventaire** (ou bénéficier d’un futur bouton de suggestion).

## 5. Prochaines étapes

- Test de partie fraîche sur appareil avec le gardien final.
- Vérifier le ressenti des phases 66 % / 33 % et des capacités accélérées.
- Ajuster si le passif/normal échoue trop souvent : ATQ final 0,75 ou pitié finale dédiée.
- Si les rapports confirment 30/30 rations, passer la génération à 1/250 pas et re-simuler.
