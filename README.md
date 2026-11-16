# La Marche des Âmes

**Maquette interactive d'un idle step RPG fantasy en pixel art.**
Les pas réels du joueur font avancer un pèlerin dans un donjon infini. Le héros marche seul, combat seul, tombe parfois — et se relève toujours vingt mètres plus tôt, là où le farm est plus doux.

> **Prototype de démonstration.** Aucune donnée de marche réelle n'est lue : la maquette simule le compteur de pas pour rendre la boucle jouable immédiatement.

**Voir la maquette en ligne :** [brunothesatellite.github.io/idle-step-rpg](https://brunothesatellite.github.io/idle-step-rpg/)

**Dépôt :** [github.com/brunothesatellite/idle-step-rpg](https://github.com/brunothesatellite/idle-step-rpg)  
**Licence :** [MIT](LICENSE)

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
- pas encore de sauvegarde, de reset quotidien à 00:00 ni de récupération hors ligne détaillée ;
- l'équilibrage est celui de la démonstration, pas la courbe finale.

Les règles complètes, les formules, les tableaux d'équilibrage, les cas limites, l'accessibilité et les critères d'acceptation sont dans **[docs/specifications-fonctionnelles.md](docs/specifications-fonctionnelles.md)**.

L'architecture Android proposée (Kotlin/Compose, APK sideloadé, **Garmin Personal API uniquement**, 2FA, jeton OAuth, clé et date d'expiration, WorkManager, sans Health Connect) est détaillée dans **[docs/architecture-android-garmin.md](docs/architecture-android-garmin.md)**.

---

## Version Android (APK)

La version native **v0.4.0** est dans `android/` :

- **Kotlin + Jetpack Compose**, `minSdk 34` (Android 14+), package `com.brunothesatellite.idlesteprpg`.
- **Texte ajustable** (Normal / Grand / Très grand), Grand par défaut, mise en page **paysage et tablette**.
- **Icône adaptative ronde**, **porte du gardien** hors combat et **coffre dans le bandeau HUD**.
- **Historique Garmin 7 jours** importé sans double comptage, **simulation hors ligne** (combats, or, paliers) et récapitulatif.
- **Serments journaliers variables**, série de jours, pas hebdomadaires, récompenses automatiques.
- **Combat animé** : dégâts/soins flottants, critiques distincts, bannière de niveau, phase 2, bénédiction et modificateurs de gardien.
- **Journal complet** avec filtres, **journal de légende** et **bestiaire**.
- **Coffres** conservés 24 h, 2 en attente, compte à rebours et alerte d'expiration.
- **Réglages** : taille du texte, notifications par catégorie, heures calmes, animations, vibrations, journal de sync, export/import avec aperçu, effacement.
- **Onboarding** en quatre écrans, revoyable depuis les réglages.
- **Ferveur** qui décroît après 2 h et se rattrape hors ligne, avec aura.
- **Inventaire avancé** : équiper le meilleur, tri/filtres, vendre les communs, verrouiller, fusionner, objets non identifiés, ensembles, forge, effets légendaires et coffre de camp.
- **Contenu** : biomes colorés avec météo, rencontres spéciales (autel, source, marchand, statue, pari), jalons complets, boss uniques 10/25/50/100.
- **Sauvegarde** : JSON versionné, copie de secours, export/import contrôlé, widget d'écran d'accueil, mise à jour in-app.
- **Qualité** : tests moteur, tests Garmin MockWebServer, tests UI Compose, lint et CI Android.
- Scène pixel art, combats automatiques, gardiens, coffre, campement, inventaire, améliorations, journal.
- **Garmin Personal API uniquement** : clé personnelle, échange contre un jeton OAuth2 de 24 h, aucun Health Connect.
- **2FA Garmin dans le navigateur** : l'application ne voit jamais le mot de passe ni le code 2FA.
- Écran de compte Garmin : clé masquée, date d'expiration modifiable, test/synchronisation, révocation, jeton et dernière sync.
- Reset quotidien à **00:00 local**, aligné sur Garmin.
- Clé et jeton chiffrés dans **Android Keystore** ; écran de clé protégé contre les captures.
- Synchronisation à l'ouverture, bouton manuel et **WorkManager** toutes les 30 minutes.

**Installer la dernière version :** [GitHub Releases](https://github.com/brunothesatellite/idle-step-rpg/releases/latest)

```bash
adb install -r app-release.apk
```

Ou télécharger l'APK sur le téléphone et autoriser « Installer des applis inconnues ». Le build de release est signé ; le keystore et ses mots de passe ne sont **pas** dans le dépôt.

**Première ouverture :** aller dans **Compte Garmin** (bouton G ou « Connecter Garmin »), ouvrir Garmin Connect, se connecter avec la 2FA, créer une clé Personal API, la coller dans l'application, puis renseigner sa date d'expiration. Garmin n'expose pas cette date via l'API : c'est l'utilisateur qui la saisit, pour les rappels.

**Build local :**

```bash
cd android
# JDK 21 et Android SDK 35 sont nécessaires
./gradlew :app:assembleRelease
```

Le build de release lit `../.keystore/keystore.properties` (fichier local ignoré par Git). Sans ce fichier, seul le build debug peut être généré.

> **Sécurité** : ne jamais committer une clé Garmin. Si une clé a été exposée, la révoquer sur Garmin Connect et en créer une nouvelle.

---

## Arborescence

```
idle-step-rpg/
├── index.html                     # la maquette
├── styles.css                     # direction artistique, scène, tableau de bord
├── game.js                        # simulation, combats, camp, inventaire
├── LICENSE                        # MIT
├── LICENSES.md                    # récapitulatif des licences
├── docs/
│   ├── specifications-fonctionnelles.md
│   └── architecture-android-garmin.md
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
- **Code, textes et direction artistique originale** : © 2026 brunothesatellite, sous **licence MIT**. Voir `LICENSE`.
- **Aucune ressource propriétaire** n'est utilisée.

Le récapitulatif complet des licences est dans **[LICENSES.md](LICENSES.md)**.

---

*La Marche des Âmes — maquette de game design, 2026.*

