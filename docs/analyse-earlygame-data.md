# Analyse du début de jeu — expériences simulateur

> Généré par `tools/equilibrage/earlygame_experiment.py`. Aucune constante du jeu n’est modifiée.

Sauvegarde importée : palier **9**, niveau **24**, ATQ **212**, PV **684**.

## Build LOW

| Scénario | Palier | Monstre | TTK | PV restants | Gardien | TTK | PV restants | Rations |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| référence actuelle | 9 | 100 % | 0.3 s | 100 % | 100 % | 2.9 s | 92 % | 0.00 |
| référence actuelle | 12 | 100 % | 0.7 s | 100 % | 100 % | 4.5 s | 80 % | 0.00 |
| référence actuelle | 15 | 100 % | 1.4 s | 96 % | 100 % | 7.2 s | 65 % | 0.57 |
| early doux · PV m×3 · g×2,5 · ATQ×1,5 | 9 | 100 % | 1.0 s | 100 % | 100 % | 5.6 s | 77 % | 0.00 |
| early doux · PV m×3 · g×2,5 · ATQ×1,5 | 12 | 100 % | 1.4 s | 97 % | 100 % | 7.6 s | 64 % | 0.34 |
| early doux · PV m×3 · g×2,5 · ATQ×1,5 | 15 | 100 % | 2.1 s | 92 % | 98 % | 10.7 s | 37 % | 1.00 |
| early moyen · PV m×4 · g×3 · ATQ×1,75 | 9 | 100 % | 1.2 s | 99 % | 100 % | 6.5 s | 67 % | 0.00 |
| early moyen · PV m×4 · g×3 · ATQ×1,75 | 12 | 100 % | 1.8 s | 95 % | 100 % | 8.8 s | 64 % | 0.70 |
| early moyen · PV m×4 · g×3 · ATQ×1,75 | 15 | 100 % | 2.5 s | 90 % | 81 % | 11.6 s | 21 % | 1.00 |
| early fort · PV m×5 · g×4 · ATQ×2 | 9 | 100 % | 1.5 s | 98 % | 100 % | 8.3 s | 53 % | 0.09 |
| early fort · PV m×5 · g×4 · ATQ×2 | 12 | 100 % | 2.1 s | 94 % | 100 % | 11.2 s | 53 % | 0.99 |
| early fort · PV m×5 · g×4 · ATQ×2 | 15 | 100 % | 2.9 s | 87 % | 30 % | 12.3 s | 5 % | 1.00 |
| écart de niveau 3 %/niv · cap×1,5 | 9 | 100 % | 0.4 s | 100 % | 100 % | 4.0 s | 84 % | 0.00 |
| écart de niveau 3 %/niv · cap×1,5 | 12 | 100 % | 1.0 s | 99 % | 100 % | 5.7 s | 66 % | 0.01 |
| écart de niveau 3 %/niv · cap×1,5 | 15 | 100 % | 1.6 s | 93 % | 100 % | 8.6 s | 55 % | 0.97 |
| écart de niveau 4 %/niv · cap×1,6 | 9 | 100 % | 0.6 s | 100 % | 100 % | 4.4 s | 81 % | 0.00 |
| écart de niveau 4 %/niv · cap×1,6 | 12 | 100 % | 1.0 s | 99 % | 100 % | 6.0 s | 64 % | 0.09 |
| écart de niveau 4 %/niv · cap×1,6 | 15 | 100 % | 1.7 s | 93 % | 100 % | 9.0 s | 47 % | 0.99 |
| early doux + écart 3 % | 9 | 100 % | 1.4 s | 98 % | 100 % | 7.6 s | 61 % | 0.26 |
| early doux + écart 3 % | 12 | 100 % | 1.8 s | 95 % | 100 % | 9.9 s | 58 % | 0.98 |
| early doux + écart 3 % | 15 | 100 % | 2.5 s | 90 % | 49 % | 11.7 s | 10 % | 1.00 |
| early moyen + écart 3 % | 9 | 100 % | 1.8 s | 96 % | 100 % | 8.9 s | 71 % | 0.85 |
| early moyen + écart 3 % | 12 | 100 % | 2.2 s | 93 % | 97 % | 11.5 s | 36 % | 1.00 |
| early moyen + écart 3 % | 15 | 100 % | 2.9 s | 86 % | 21 % | 11.8 s | 3 % | 1.00 |
| early fort + écart de niveau | 9 | 100 % | 2.2 s | 93 % | 94 % | 12.1 s | 35 % | 1.00 |
| early fort + écart de niveau | 12 | 100 % | 2.9 s | 89 % | 15 % | 12.7 s | 2 % | 1.00 |
| early fort + écart de niveau | 15 | 100 % | 3.6 s | 78 % | 0 % | 10.8 s | 0 % | 1.00 |

## Build `reference`

| Scénario | Palier | Monstre | TTK | PV restants | Gardien | TTK | PV restants | Rations |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| référence actuelle | 9 | 100 % | 0.0 s | 100 % | 100 % | 0.6 s | 100 % | 0.00 |
| référence actuelle | 15 | 100 % | 0.1 s | 100 % | 100 % | 1.4 s | 99 % | 0.00 |
| référence actuelle | 20 | 100 % | 0.4 s | 100 % | 100 % | 2.4 s | 93 % | 0.00 |
| référence actuelle | 25 | 100 % | 1.9 s | 88 % | 100 % | 11.4 s | 57 % | 1.00 |
| référence actuelle | 36 | 100 % | 5.6 s | 44 % | 58 % | 18.6 s | 12 % | 1.00 |
| early doux · PV m×3 · g×2,5 · ATQ×1,5 | 9 | 100 % | 0.0 s | 100 % | 100 % | 1.3 s | 99 % | 0.00 |
| early doux · PV m×3 · g×2,5 · ATQ×1,5 | 15 | 100 % | 0.2 s | 100 % | 100 % | 1.9 s | 96 % | 0.00 |
| early doux · PV m×3 · g×2,5 · ATQ×1,5 | 20 | 100 % | 0.4 s | 100 % | 100 % | 2.4 s | 93 % | 0.00 |
| early doux · PV m×3 · g×2,5 · ATQ×1,5 | 25 | 100 % | 1.9 s | 88 % | 100 % | 11.4 s | 57 % | 1.00 |
| early doux · PV m×3 · g×2,5 · ATQ×1,5 | 36 | 100 % | 5.6 s | 44 % | 58 % | 18.6 s | 12 % | 1.00 |
| early moyen · PV m×4 · g×3 · ATQ×1,75 | 9 | 100 % | 0.1 s | 100 % | 100 % | 1.5 s | 98 % | 0.00 |
| early moyen · PV m×4 · g×3 · ATQ×1,75 | 15 | 100 % | 0.3 s | 100 % | 100 % | 2.1 s | 96 % | 0.00 |
| early moyen · PV m×4 · g×3 · ATQ×1,75 | 20 | 100 % | 0.4 s | 100 % | 100 % | 2.4 s | 93 % | 0.00 |
| early moyen · PV m×4 · g×3 · ATQ×1,75 | 25 | 100 % | 1.9 s | 88 % | 100 % | 11.4 s | 57 % | 1.00 |
| early moyen · PV m×4 · g×3 · ATQ×1,75 | 36 | 100 % | 5.6 s | 44 % | 58 % | 18.6 s | 12 % | 1.00 |
| early fort · PV m×5 · g×4 · ATQ×2 | 9 | 100 % | 0.2 s | 100 % | 100 % | 2.0 s | 97 % | 0.00 |
| early fort · PV m×5 · g×4 · ATQ×2 | 15 | 100 % | 0.4 s | 100 % | 100 % | 2.5 s | 95 % | 0.00 |
| early fort · PV m×5 · g×4 · ATQ×2 | 20 | 100 % | 0.4 s | 100 % | 100 % | 2.4 s | 93 % | 0.00 |
| early fort · PV m×5 · g×4 · ATQ×2 | 25 | 100 % | 1.9 s | 88 % | 100 % | 11.4 s | 57 % | 1.00 |
| early fort · PV m×5 · g×4 · ATQ×2 | 36 | 100 % | 5.6 s | 44 % | 58 % | 18.6 s | 12 % | 1.00 |
| écart de niveau 3 %/niv · cap×1,5 | 9 | 100 % | 0.0 s | 100 % | 100 % | 0.6 s | 100 % | 0.00 |
| écart de niveau 3 %/niv · cap×1,5 | 15 | 100 % | 0.1 s | 100 % | 100 % | 1.4 s | 99 % | 0.00 |
| écart de niveau 3 %/niv · cap×1,5 | 20 | 100 % | 0.4 s | 100 % | 100 % | 2.4 s | 93 % | 0.00 |
| écart de niveau 3 %/niv · cap×1,5 | 25 | 100 % | 1.9 s | 88 % | 100 % | 11.4 s | 57 % | 1.00 |
| écart de niveau 3 %/niv · cap×1,5 | 36 | 100 % | 5.6 s | 44 % | 58 % | 18.6 s | 12 % | 1.00 |
| écart de niveau 4 %/niv · cap×1,6 | 9 | 100 % | 0.0 s | 100 % | 100 % | 0.6 s | 100 % | 0.00 |
| écart de niveau 4 %/niv · cap×1,6 | 15 | 100 % | 0.1 s | 100 % | 100 % | 1.4 s | 99 % | 0.00 |
| écart de niveau 4 %/niv · cap×1,6 | 20 | 100 % | 0.4 s | 100 % | 100 % | 2.4 s | 93 % | 0.00 |
| écart de niveau 4 %/niv · cap×1,6 | 25 | 100 % | 1.9 s | 88 % | 100 % | 11.4 s | 57 % | 1.00 |
| écart de niveau 4 %/niv · cap×1,6 | 36 | 100 % | 5.6 s | 44 % | 58 % | 18.6 s | 12 % | 1.00 |
| early doux + écart 3 % | 9 | 100 % | 0.0 s | 100 % | 100 % | 1.3 s | 99 % | 0.00 |
| early doux + écart 3 % | 15 | 100 % | 0.2 s | 100 % | 100 % | 1.9 s | 96 % | 0.00 |
| early doux + écart 3 % | 20 | 100 % | 0.4 s | 100 % | 100 % | 2.4 s | 93 % | 0.00 |
| early doux + écart 3 % | 25 | 100 % | 1.9 s | 88 % | 100 % | 11.4 s | 57 % | 1.00 |
| early doux + écart 3 % | 36 | 100 % | 5.6 s | 44 % | 58 % | 18.6 s | 12 % | 1.00 |
| early moyen + écart 3 % | 9 | 100 % | 0.1 s | 100 % | 100 % | 1.5 s | 98 % | 0.00 |
| early moyen + écart 3 % | 15 | 100 % | 0.3 s | 100 % | 100 % | 2.1 s | 96 % | 0.00 |
| early moyen + écart 3 % | 20 | 100 % | 0.4 s | 100 % | 100 % | 2.4 s | 93 % | 0.00 |
| early moyen + écart 3 % | 25 | 100 % | 1.9 s | 88 % | 100 % | 11.4 s | 57 % | 1.00 |
| early moyen + écart 3 % | 36 | 100 % | 5.6 s | 44 % | 58 % | 18.6 s | 12 % | 1.00 |
| early fort + écart de niveau | 9 | 100 % | 0.2 s | 100 % | 100 % | 2.0 s | 97 % | 0.00 |
| early fort + écart de niveau | 15 | 100 % | 0.4 s | 100 % | 100 % | 2.5 s | 95 % | 0.00 |
| early fort + écart de niveau | 20 | 100 % | 0.4 s | 100 % | 100 % | 2.4 s | 93 % | 0.00 |
| early fort + écart de niveau | 25 | 100 % | 1.9 s | 88 % | 100 % | 11.4 s | 57 % | 1.00 |
| early fort + écart de niveau | 36 | 100 % | 5.6 s | 44 % | 58 % | 18.6 s | 12 % | 1.00 |

## Rythme d’XP

| Règle XP | Niv. 5 | Niv. 9 | Niv. 15 | Niv. 20 | Niv. 25 | Niv. 30 | Niv. 36 |
|---|---:|---:|---:|---:|---:|---:|---:|
| XP actuelle · gardien ×10 | 14 | 24 | 36 | 44 | 50 | 55 | 61 |
| gardien ×5 | 11 | 21 | 32 | 40 | 46 | 51 | 57 |
| XP actuelle · malus surlevel −50 % | 13 | 19 | 29 | 36 | 42 | 47 | 53 |
| gardien ×5 · malus surlevel −50 % | 11 | 17 | 26 | 33 | 38 | 44 | 49 |
| gardien ×5 · malus surlevel −75 % | 11 | 15 | 21 | 26 | 32 | 36 | 43 |
