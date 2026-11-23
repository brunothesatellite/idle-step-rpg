# Pack de publication Play Store — DungeonStep

Tout ce qui est nécessaire pour créer la fiche Play Store, **sans APK**. Le pack est **bilingue français / anglais**.

## Contenu

```
playstore/
├── README.md
├── checklist-play-console.md
├── 01-icone/
│   ├── icon-1024.png                 master (1024×1024)
│   ├── icon-512.png                  icône Play (512×512)
│   ├── icon-192-apercu-masque.png    aperçu avec masque Android
│   ├── icon-96-apercu.png            aperçu lanceur
│   └── adaptive/
│       ├── ic_launcher.xml           exemple de XML adaptive icon
│       └── mdpi/  hdpi/  xhdpi/  xxhdpi/  xxxhdpi/
│           ├── ic_launcher_background.png
│           ├── ic_launcher_foreground.png
│           └── ic_launcher_monochrome.png
├── 02-captures/
│   ├── 01-marche.png … 05-atelier.png      5 captures FR (1080×1920)
│   └── en/
│       ├── 01-walk.png                     1080×1920
│       ├── 02-guardian.png
│       ├── 03-camp.png
│       ├── 04-loot.png
│       └── 05-workshop.png
├── 03-textes/
│   ├── titre.txt … mots-cles.txt           textes FR
│   └── en/
│       ├── title.txt
│       ├── short-description.txt
│       ├── full-description.txt
│       ├── lore.md
│       ├── release-notes.txt
│       └── keywords.txt
├── 04-feature-graphic/
│   ├── feature-graphic-1024x500.png        FR
│   └── en/feature-graphic-1024x500.png     EN
└── 05-distribution/
    ├── README.md                 variantes github / play, signatures, mise à jour croisée
    └── procedure-cle-play.md     fournir la clé release à Play App Signing
```

## Spécifications

| Élément | Format | Limite Play |
|---|---|---|
| Icône | 512×512 PNG | 512×512, ≤ 1 Mo |
| Feature graphic | 1024×500 PNG | 1024×500, ≤ 15 Mo |
| Captures téléphone | 1080×1920 PNG (9:16) | 2 à 8, 320–3840 px |
| Titre FR | `DungeonStep — Marche des Âmes` | 30 caractères (29 ici) |
| Titre EN | `DungeonStep - March of Souls` | 30 caractères (28 ici) |
| Description courte | 76 caractères (FR), 69 (EN) | 80 |
| Description complète | ~1 900 caractères | 4 000 |
| Notes de version | ~350 caractères | 500 |

## Icône

- Master : composition 1024×1024 avec porte, Kaelen, Balrug, buste de statue et lanterne.
- Calques adaptive : fond opaque (dégradé + pierre), avant-plan transparent (sujets + sol), monochrome (arche + silhouettes pour les icônes thématiques Android 13+).
- Toutes les densités sont fournies (108 à 432 px).
- Le contenu utile reste dans la zone sûre de 66 % : les masques ronds ne coupent ni Kaelen ni Balrug.
- L’icône est commune aux deux langues.

### Intégration dans l'app (à faire plus tard, non inclus ici)

Copier les dossiers de densité dans `android/app/src/main/res/mipmap-*` et le XML dans `mipmap-anydpi-v26/`, puis remplacer l'icône de lancement. Le nom de paquet n'est pas modifié par ce pack.

## Captures

Captures réelles de l'application, pas des rendus marketing. Les accroches sont intégrées dans les visuels. Les captures brutes ne sont pas incluses dans le pack public.

- **FR** : `02-captures/01-marche.png` à `05-atelier.png`, à remplacer au prochain passage (elles datent d'une version antérieure ; voir backlog).
- **EN** : `02-captures/en/01-walk.png` à `05-workshop.png`, générées sur la version actuelle (mars 2026) : marche, gardien, camp, coffre, atelier.

## Textes

À copier tel quel dans la Play Console, par langue.

- **FR** : `03-textes/` — titre 29 caractères ; ne pas rajouter « La » devant « Marche des Âmes », la limite serait dépassée (32).
- **EN** : `03-textes/en/` — titre 28 caractères, descriptions, mots-clés et notes de version.
- Le lore long reste dans `lore.md` (FR) et `lore.md` (EN), hors fiche Play.

## Distribution

Le projet produit deux variantes avec le même paquet et la même clé release :

- **github** : `gradlew :app:assembleGithubRelease` → APK avec mise à jour intégrée depuis GitHub.
- **play** : `gradlew :app:bundlePlayRelease` → AAB sans permission d'installation ni vérification GitHub.

Les deux sont signées avec la clé release, donc les utilisateurs peuvent passer d'un canal à l'autre sans perdre leurs données. Détails et procédure de clé Play : `05-distribution/`.

## Ce qui n'est pas dans le pack

- APK / AAB (volontairement exclu).
- Clés de signature et mots de passe.
- Réponses définitives aux formulaires Play (sécurité des données, classification) : voir `checklist-play-console.md`.

## Sources des visuels

Sprites du jeu : `assets/env/closed_door.png`, `assets/mon/balrug.png`, `assets/env/statue_demonic_bust.png`, `assets/item/misc_lantern.png`, héros du lanceur (`ic_launcher_foreground.png`, détouré). Police : `assets/fonts/press-start-2p.woff2` (feature graphic) et `android/app/src/main/res/font/`.
