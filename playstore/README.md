# Pack de publication Play Store — DungeonStep

Tout ce qui est nécessaire pour créer la fiche Play Store, **sans APK**.

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
│       ├── mdpi/  hdpi/  xhdpi/  xxhdpi/  xxxhdpi/
│       │   ├── ic_launcher_background.png
│       │   ├── ic_launcher_foreground.png
│       │   └── ic_launcher_monochrome.png
├── 02-captures/
│   ├── 01-marche.png                 1080×1920
│   ├── 02-gardien.png                1080×1920
│   ├── 03-campement.png              1080×1920
│   ├── 04-butin.png                  1080×1920
│   └── 05-atelier.png                1080×1920
├── 03-textes/
│   ├── titre.txt
│   ├── description-courte.txt
│   ├── description-complete.txt
│   ├── lore.md
│   ├── notes-version.txt
│   └── mots-cles.txt
├── 04-feature-graphic/
│   └── feature-graphic-1024x500.png
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
| Titre | `DungeonStep — Marche des Âmes` | 30 caractères (29 ici) |
| Description courte | 76 caractères | 80 |
| Description complète | ~1 700 caractères | 4 000 |
| Notes de version | ~230 caractères | 500 |

## Icône

- Master : composition 1024×1024 avec porte, Kaelen, Balrug, buste de statue et lanterne.
- Calques adaptive : fond opaque (dégradé + pierre), avant-plan transparent (sujets + sol), monochrome (arche + silhouettes pour les icônes thématiques Android 13+).
- Toutes les densités sont fournies (108 à 432 px).
- Le contenu utile reste dans la zone sûre de 66 % : les masques ronds ne coupent ni Kaelen ni Balrug.

### Intégration dans l'app (à faire plus tard, non inclus ici)

Copier les dossiers de densité dans `android/app/src/main/res/mipmap-*` et le XML dans `mipmap-anydpi-v26/`, puis remplacer l'icône de lancement. Le nom de paquet n'est pas modifié par ce pack.

## Captures

Captures réelles de l'application, pas des rendus marketing. Les accroches sont intégrées dans les visuels. Les captures brutes ne sont pas incluses dans le pack public.

## Textes

À copier tel quel dans la Play Console. Le titre tient en 29 caractères ; ne pas rajouter « La » devant « Marche des Âmes », la limite serait dépassée (32).

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

Sprites du jeu : `assets/env/closed_door.png`, `assets/mon/balrug.png`, `assets/env/statue_demonic_bust.png`, `assets/item/misc_lantern.png`, héros du lanceur (`ic_launcher_foreground.png`, détouré). Police : `assets/fonts/press-start-2p.woff2`.
