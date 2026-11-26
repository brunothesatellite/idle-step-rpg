# Historique des releases GitHub

> Archive créée le 26/11/2026 avant le ménage des releases GitHub.
> Les releases **v0.25.0** (dernière) et **v0.22.1** (précédente) restent en ligne.
> Les **tags Git** de toutes les versions sont conservés : le code reste accessible.
> Les binaires des releases ci-dessous ne sont plus disponibles sur GitHub.

## v0.21.3 — 2026-11-25

**Titre :** DungeonStep - La Marche des Ames v0.21.3
**Assets :** `La-Marche-des-Ames-v0.21.3.aab` (31.6 Mo, 1 téléchargement(s)), `La-Marche-des-Ames-v0.21.3.apk` (51.3 Mo, 1 téléchargement(s))

# DungeonStep — La Marche des Âmes v0.21.3

> Mise à jour de confort centrée sur l’inventaire et le coffre de camp. Les sauvegardes existantes sont conservées.

## Coffre de camp

- **PRENDRE** est grisé quand l’inventaire est plein.
- **TOUT PRENDRE** n’est actif que si tous les objets du coffre tiennent dans les places libres. Sinon, le bouton reste grisé et indique combien de places manquent.
- Le raccourci **Coffre de camp** du camp suit la même règle.
- Le coffre affiche les objets en réserve et les **places libres / 24**.

## Aller-retour inventaire ↔ coffre

- Une **icône coffre** apparaît dans la fiche d’un objet de l’inventaire, à côté de la croix.
- Elle envoie l’objet au coffre immédiatement, avec un toast.
- Elle est **grisée** si l’objet est verrouillé et **absente** si l’objet est équipé.
- Un appui sur un objet du coffre ouvre une **fiche réduite** : comparaison avec l’équipement porté, **PRENDRE** si une place est libre, **VENDRE** et **DÉMONTER** avec confirmation.

## Correctifs

- Les éclats de démontage affichés et réels sont alignés : **6** pour l’épique, **10** pour le légendaire, plafond d’éclats compris.
- Les séquences de **mojibake** de l’interface sont corrigées : compteur `×`, puces de butin, tirets, flèche retour et libellé Garmin.
- Un garde-fou d’encodage (`tools/check_encoding.py`) est exécuté en CI.

## Validation

- **155 tests** unitaires, 0 échec.
- Lint Android OK, CI verte.
- **versionCode 61**, **versionName 0.21.3**.

## Installation

- **APK** : `La-Marche-des-Ames-v0.21.3.apk` (Android 14+).
- **AAB** : `La-Marche-des-Ames-v0.21.3.aab` pour la distribution Play.
- Même signature que les releases précédentes : les données sont conservées lors de la mise à jour.
- Application sideloadée, sans publicité, sans achat intégré, sans serveur éditeur.

## English summary

Comfort update focused on inventory and camp stash. Take buttons are greyed out when the bag is full, Take All requires enough free slots, and the camp shortcut follows the same rule. Inventory item sheets get a chest icon to send an item to the stash (greyed when locked, hidden when equipped). Tapping a stashed item opens a reduced sheet with equipment comparison, Take, Sell and Dismantle. Salvage shard values are now consistent, and UI mojibake is fixed with a CI encoding guard. 155 unit tests, lint OK. Android 14+.

---

Journal complet : [`CHANGELOG.md`](https://github.com/brunothesatellite/idle-step-rpg/blob/main/CHANGELOG.md).

## v0.21.2 — 2026-11-24

**Titre :** DungeonStep - La Marche des Ames v0.21.2
**Assets :** `La-Marche-des-Ames-v0.21.2.aab` (31.6 Mo, 1 téléchargement(s)), `La-Marche-des-Ames-v0.21.2.apk` (51.2 Mo, 1 téléchargement(s))

# DungeonStep — La Marche des Âmes v0.21.2

> Première release publiée depuis **v0.19.1**. Elle regroupe les lots 0 à 10 du plan d'équilibrage, l'écran de fin enrichi, l'export HTML, la localisation FR/EN et la calibration des gardiens.

## Points forts

### Équilibrage 1 → 100

- **XP** : courbe `1,09^niveau`, récompenses linéaires par palier, rattrapage ×1,25.
- **Butin** : stats ×1 à ×6 selon le palier, raretés 1,15 / 1,5 / 1,9 / 2,4, upgrades additifs +4 % (20 niveaux max).
- **Camp et sets** : bonus en pourcentage, coût indexé sur le palier, plafond 30 niveaux.
- **Rations** : cap 30, revenus renforcés, auto-ration 40 %/45 %, repos gratuit 2×20 %, farm +10 % PV.
- **Gardiens** : niveau palier +1, bouclier 35 %/3 s/8 s, enrage 25 s, phase 2 télégraphiée, pitié −1 %/défaite (cap −3 %), portail à 90 m.
- **Calibration 0.21.1/0.21.2** : TTK visé 18 s, ATQ par capacité, potions d'ATQ à risque, `reference` mesuré à 44–60 % sans potion.
- **Farm et pity** : rare garanti tous les 5 kills, set tous les 10 gardiens, farm de gardien +1 éclat/+1 ration.
- **Fin de jeu** : Trône brisé +15 % → +35 %, boss final **L'Âme Première**, fin unique au palier 100, mode sans fin, télémétrie locale.

### Écran de fin et export

- Diorama avec la **vraie statue du palier 100**, l'**aura dorée** et le gardien déchu.
- Dossier du héros, progression, télémétrie, gardiens 81–100, économie, équipement, collections et jalons.
- **Export HTML autonome** (fonte pixel embarquée), enregistré via le sélecteur Android.
- **Aperçu de la fin** dans les réglages des builds debug.

### Localisation et qualité

- Interface, journal, Codex, notifications, widget et manuel en **français et anglais**.
- Noms d'objets fusionnés traduits (`Gnoll Axe (fused ×3)`).
- Correctif VIT : intervalle minimum **180 ms** réellement appliqué.
- 151 tests unitaires, lint OK.

## Installation

- **APK** : `La-Marche-des-Ames-v0.21.2.apk` (Android 14+).
- **AAB** : `La-Marche-des-Ames-v0.21.2.aab` pour la distribution Play.
- Les mises à jour installées avec la même signature **conservent les données**.
- Application sideloadée, sans publicité, sans achat intégré, sans serveur éditeur.

## Playtest

La fiche de suivi est dans [`docs/playtest-0.21.2.md`](https://github.com/brunothesatellite/idle-step-rpg/blob/main/docs/playtest-0.21.2.md) : journal quotidien, points à noter et exports à J+3/J+7/J+14.

Points connus à surveiller : `under` à 0–37 % selon le palier, sur-stuff à 100 %, et le ressenti des potions à risque.

## English summary

First public release since v0.19.1. It includes the full 1→100 balance pass, the enriched ending screen with a standalone HTML report, French/English localisation, the 180 ms VIT fix, and guardian calibration (18 s TTK, ability-based ATK, risk/reward attack potions). 151 unit tests, lint OK. Android 14+.

---

Journal complet : [`CHANGELOG.md`](https://github.com/brunothesatellite/idle-step-rpg/blob/main/CHANGELOG.md).

## v0.19.1 — 2026-11-23

**Titre :** DungeonStep - La Marche des Ames v0.19.1
**Assets :** `La-Marche-des-Ames-v0.19.1.apk` (50.9 Mo, 2 téléchargement(s))

## DungeonStep — La Marche des Âmes v0.19.1

### Atelier
- **Vendre**, **démonter** et **reforger** demandent désormais une **confirmation** (FR/EN).
- **« Reforger encore »** reste direct dans le panneau de résultat : pas de seconde confirmation.
- **« Vendre les communs »** est inchangé.

### Build
- **versionCode 46**, **versionName 0.19.1**, **targetSdk 36**.
- Tests : **117 / 117**, lint Android OK.
- Manuel FR/EN régénéré en 0.19.1 et publié sur GitHub Pages.

### Asset
- `La-Marche-des-Ames-v0.19.1.apk` : APK GitHub signé, avec mise à jour intégrée.
- **Pas d’AAB pour cette version** (release APK uniquement).

### Installation
- Même clé de signature que la v0.19.0 : la progression est conservée.
- SHA-256 APK : `A8AAC04BAB78B5961240539B642C66E4FEAD7DF484B23D7939CD315283812A2C`

## v0.19.0 — 2026-11-23

**Titre :** DungeonStep - La Marche des Ames v0.19.0
**Assets :** `La-Marche-des-Ames-v0.19.0-play.aab` (31.4 Mo, 1 téléchargement(s)), `La-Marche-des-Ames-v0.19.0.apk` (50.8 Mo, 3 téléchargement(s))

## DungeonStep — La Marche des Âmes v0.19.0

### Internationalisation FR/EN
- Interface, journal, Codex, notifications, widget, dialogues de mise à jour et contenus dynamiques en **français et en anglais**.
- En anglais, « Santé Connect » devient **« Health Connect »**.
- Formats localisés : séparateur de milliers (`7 464` / `7,464`), nombres compacts (`18,5 k` / `18.5k`), dates (`jj/mm/aaaa` / `mm/dd/yyyy`), heures (24 h / 12 h) et journal de synchronisation.
- **Manuel bilingue** : 36 captures anglaises, sélecteur FR/EN, génération Gradle des deux versions.
- **README bilingue** (FR puis EN) et **kit Play Store EN** : captures, descriptions, mots-clés, notes de version, feature graphic.
- **Promo EN** : pitch et captures anglaises.

### Widget
- Le **héros avec son équipement actuel** est affiché à droite.
- Le gros chiffre est la **réserve de pas** ; les pas du jour restent en dernière ligne.
- Réserve vide : la dernière ligne devient **« Touchez pour synchroniser »**.
- Le widget suit la langue de l’app et se rafraîchit quand elle passe en arrière-plan.

### Correctifs
- Ouverture des **Réglages en anglais** (format `%1$s` de la version).
- Titre et invite du widget résolus avec la langue de l’app, pas celle du lanceur.
- Captures anglaises du manuel et du kit Play corrigées.

### Build
- **versionCode 45**, **versionName 0.19.0**, **targetSdk 36**.
- Tests : **117 / 117**, lint Android OK.
- Sauvegarde **v7 inchangée** : aucune migration ajoutée.

### Assets
- `La-Marche-des-Ames-v0.19.0.apk` : APK GitHub signé, avec mise à jour intégrée.
- `La-Marche-des-Ames-v0.19.0-play.aab` : Android App Bundle pour Google Play, sans l’updater GitHub.

### Installation
- Même clé de signature que la v0.18.1 : la progression est conservée.
- SHA-256 APK : `FB0317D92E9C41525280E5EDC530C1119E8E7B3F7B91E8DD744E7A9195BF053B`
- SHA-256 AAB : `5417637EF23DE9EEA9F89C3479D289966C664E30BFA68381EB5A637E175BAF05`

## v0.18.1 — 2026-11-23

**Titre :** DungeonStep - La Marche des Ames v0.18.1
**Assets :** `La-Marche-des-Ames-v0.18.1-play.aab` (28.4 Mo, 1 téléchargement(s)), `La-Marche-des-Ames-v0.18.1.apk` (47.0 Mo, 2 téléchargement(s))

## DungeonStep — La Marche des Âmes v0.18.1

### Correctif Codex
- Les **gardiens des biomes non découverts** ne sont plus affichés comme découverts.
- Cause : la migration v6 effaçait les compteurs des **monstres** des biomes verrouillés, mais pas ceux des **gardiens**. Les anciens compteurs du bestiaire avaient rempli `bossKills` et `bossFirstSeen` pour des gardiens de biomes pas encore atteints (par exemple les Cimes du Serment).
- `bossKills`, `bossFirstSeen` et les entrées de bestiaire des gardiens sont maintenant nettoyés pour les biomes verrouillés.
- Les gardiens de jalon restent visibles uniquement à partir de leur palier fixe : **Mère des Abysses au 25**, **L'Âme Première au 100**.
- Vérifié sur la sauvegarde release au palier 36 : les Cimes affichent désormais **0 / 4 gardiens**, et le compteur global passe de 12 à **10 / 35**.

### Build
- **versionCode 44**, **versionName 0.18.1**, **targetSdk 36**.
- Tests : **115 / 115**, lint Android OK.

### Assets
- `La-Marche-des-Ames-v0.18.1.apk` : APK GitHub signé, avec mise à jour intégrée.
- `La-Marche-des-Ames-v0.18.1-play.aab` : Android App Bundle pour Google Play, sans l'updater GitHub.

### Installation
- Même clé de signature que la v0.18.0 : la progression est conservée.
- Le correctif s'applique automatiquement au premier lancement, sans réinitialisation.
- SHA-256 APK : `321574273003180EAAB48FC62215E7D9F87DFB759B7066073D52B91AAFD2D4D4`
- SHA-256 AAB : `1373D50BC026168FF455C4FDC6821B2C3CA658560CF3E474844DECEAD478A571`

## v0.18.0 — 2026-11-23

**Titre :** DungeonStep - La Marche des Ames v0.18.0
**Assets :** `La-Marche-des-Ames-v0.18.0-play.aab` (28.4 Mo, 1 téléchargement(s)), `La-Marche-des-Ames-v0.18.0.apk` (47.0 Mo, 2 téléchargement(s))

## DungeonStep — La Marche des Âmes v0.18.0

### Codex
- **110 monstres uniques** : 13 variantes inter-biomes, 3 variantes internes et Manticore recolorée. Plus aucun nom n'est partagé entre plusieurs pools ni entre deux tiers d'un même biome.
- **35 gardiens** au total, avec un chip **GARDIEN** sur les entrées du Codex.
- **Badge `NON DÉCOUVERT`** dans le Bestiaire pour les biomes non atteints.
- **Noms des biomes verrouillés** affichés dans l'onglet Biomes, avec leur palier d'ouverture.
- **Première rencontre** : biome, palier, tier et niveau enregistrés à la première victoire. Les fiches affichent les statistiques de cette rencontre ; Mère des Abysses et L'Âme Première ont un palier fixe (25 et 100).
- Nouvelle carte **GARDIENS DE JALON** pour les gardiens hors rotation.

### Migration
- Sauvegarde **v6** : fusion des anciens noms suffixés (`Balrug · BLINDÉ`), copie des compteurs vers les variantes découvertes et suppression des compteurs des biomes non découverts.
- Les kills de gardien sont retirés du compteur de monstre (Option B).
- La première rencontre est conservée uniquement sur la variante du biome/tier d'origine.
- Une sauvegarde `pre_migration_v5` est créée avant migration.

### Manuel
- Les compteurs du Codex (**110 créatures, 35 gardiens, 11 ensembles**) et la version de l'application sont injectés au build depuis `manuel/index.template.html`.

### Assets
- 17 variantes recolorées, créditées dans `assets/CREDITS.md`.
- `statue_hero.png` ajouté à la source `assets/env`.

### Build
- **versionCode 43**, **versionName 0.18.0**, **targetSdk 36**.
- Tests : **113 / 113**, lint Android OK.

### Assets
- `La-Marche-des-Ames-v0.18.0.apk` : APK GitHub signé, avec mise à jour intégrée.
- `La-Marche-des-Ames-v0.18.0-play.aab` : Android App Bundle pour Google Play, sans l'updater GitHub.

### Installation
- Même clé de signature que les versions précédentes : la progression est conservée.
- La migration vers la sauvegarde v6 est automatique au premier lancement.
- SHA-256 APK : `5B778FE2AAB6FFB8F0C6B3121E7963B8FC2C6D369C5D8D723231F43CA9BED5C5`
- SHA-256 AAB : `FADBE4C411F2D6D7769A13856677A5206977459891A27177BEE2B43FA8EE370B`

### Google Play
- L'AAB est destiné à la Play Console (versionCode 43). Il n'embarque pas la vérification de mise à jour GitHub.

## v0.17.8 — 2026-11-22

**Titre :** DungeonStep - La Marche des Ames v0.17.8
**Assets :** `La-Marche-des-Ames-v0.17.8-play.aab` (28.4 Mo, 1 téléchargement(s)), `La-Marche-des-Ames-v0.17.8.apk` (46.9 Mo, 2 téléchargement(s))

## DungeonStep — La Marche des Âmes v0.17.8

### Combat
- Les dégâts affichés proviennent désormais d'un **événement de coup exact** : le coup fatal est enfin visible, y compris quand le héros tue en un seul coup.
- L'ennemi reste affiché **~0,9 s après sa mort**, avec sa barre vide et son chiffre de dégâts, au lieu de disparaître instantanément.
- Les coups sur le héros et sur l'ennemi sont traités séparément, sans doublon.
- La remontée à 60 % des PV après une défaite n'est plus affichée comme un soin vert.

### Zone de repli
- Le tag indique la progression : **« ZONE DE REPLI · FARM 1/3 »**, puis `2/3`, `3/3`.
- Chaque ferme est journalisée : **« Farm · … vaincu · +N PV »**.
- Le soin de farm de **8 % des PV max** est visible dans le journal et dans les floats de soin.
- **« Fin du repli — le sentier reprend »** apparaît toujours après la troisième ferme.

### Build
- **versionCode 42**, **versionName 0.17.8**, **targetSdk 36**.
- Tests : **106 / 106**.

### Assets
- `La-Marche-des-Ames-v0.17.8.apk` : APK GitHub signé, avec mise à jour intégrée.
- `La-Marche-des-Ames-v0.17.8-play.aab` : Android App Bundle pour Google Play, sans l'updater GitHub.

### Installation
- Même clé de signature que les versions précédentes : la progression est conservée.
- SHA-256 APK : `B0D92B40FE04A5A3C198608E3E818B745479D0740AC73E164D91E215FAC05DCF`
- SHA-256 AAB : `76A19C5FE72A0139633A79F1D12D2FC07B805FE4CD3493553C51F74BD6404753`

### Google Play
- L'AAB est destiné à la Play Console (versionCode 42). Il n'embarque pas la vérification de mise à jour GitHub.

## v0.17.7 — 2026-11-22

**Titre :** DungeonStep - La Marche des Ames v0.17.7
**Assets :** `La-Marche-des-Ames-v0.17.7-play.aab` (28.4 Mo, 1 téléchargement(s)), `La-Marche-des-Ames-v0.17.7.apk` (46.9 Mo, 1 téléchargement(s))

## DungeonStep — La Marche des Âmes v0.17.7

### Reprise
- Un seul résumé au lancement : la simulation d'absence et l'import des pas sont désormais fusionnés.
- Le résumé indique les défaites simulées : « N défaites simulées · X m de recul ».
- Plus de combat fantôme : une simulation qui s'arrête en plein combat le termine hors écran, ou le clôt en défaite.
- Si la synchro dépasse le budget de 3 s, le résumé s'affiche sans l'attendre ; les pas sont ajoutés ensuite, sans second écran.

### Affichage
- Nombres compacts (k / M / Md) dans les stats, ressources, camp, rapport et codex.
- Nombres de combat sur une ligne, auto-réduits pour rester dans la zone du sprite ; le « ! » de critique est remplacé par la couleur or.
- Régénération cumulée à 4 nombres par seconde au maximum, et 4 nombres visibles par unité.
- Puissance et niveau réorganisés, badge de niveau compact, tuiles PV/ATQ sans troncature.
- Camp et ressources en grille, camp défilable en paysage aux grandes tailles de texte.

### Build
- **versionCode 41**, **versionName 0.17.7**, **targetSdk 36**.
- Tests : **103 / 103**.

### Assets
- `La-Marche-des-Ames-v0.17.7.apk` : APK GitHub signé, avec mise à jour intégrée.
- `La-Marche-des-Ames-v0.17.7-play.aab` : Android App Bundle pour Google Play, sans l'updater GitHub.

### Installation
- Même clé de signature que les versions précédentes : la progression est conservée.
- SHA-256 APK : `2DF7BADF9812A276D3DECFF7B9C3FB3170D04C7A7297099380387E128740AC76`
- SHA-256 AAB : `65E1C490268BC19C87D207513C9EEAC957F49B5D3A686699E701F0484895648D`

### Google Play
- L'AAB est destiné à la Play Console (versionCode 41). Il n'embarque pas la vérification de mise à jour GitHub.

## v0.17.6 — 2026-11-21

**Titre :** DungeonStep - La Marche des Ames v0.17.6
**Assets :** `La-Marche-des-Ames-v0.17.6.apk` (46.9 Mo, 1 téléchargement(s))

## DungeonStep — La Marche des Âmes v0.17.6

### Notifications
- Clé Garmin expirée ou refusée : la notification propose désormais **« Remplacer la clé »** et ouvre directement l'écran Compte Garmin.
- Limite Garmin (429) : le message indique l'**heure exacte du nouvel essai** au lieu d'un délai approximatif.
- Nouveau réglage **« Synchro de fond »** (désactivé par défaut) pour être prévenu des pas crédités ou d'une erreur lors des synchronisations automatiques.

### Campement
- **Animation de soin** par flammes lorsque vous vous reposez.
- **Bilan de veillée** : PV rendus et ressource consommée, affichés après le soin.

### Build
- **versionCode 40**, **versionName 0.17.6**, **targetSdk 36**.
- Tests : **96 / 96**.

### Assets
- `La-Marche-des-Ames-v0.17.6.apk` : APK GitHub, à installer sur le téléphone.

### Installation
- Même clé de signature que les versions précédentes : la progression est conservée.
- SHA-256 APK : `2211EC87587C9D3E33B74E64A62E1AFDDED0DFC95DA0A01B85E4A3C96989D75A`

## v0.17.5 — 2026-11-21

**Titre :** DungeonStep - La Marche des Ames v0.17.5
**Assets :** `La-Marche-des-Ames-v0.17.5.apk` (46.9 Mo, 1 téléchargement(s))

## DungeonStep — La Marche des Âmes v0.17.5

### Réglages avancés : budget d'absence
- Nouvelle carte **Réglages avancés** dans les réglages, **repliée par défaut**.
- Budget de progression hors ligne réglable : **pourcentage** (0–30 % des pas du jour, **30 % par défaut**) ou **pas maximum** (0–5 000, raccourcis 300 / 1 000 / 3 000).
- Budget **journalier** : ce qui a déjà été consommé hors ligne aujourd'hui est déduit ; deux fermetures partagent la même enveloppe.
- La durée de simulation découle des pas (~1 s pour 60 pas, jusqu'à 2× si la réserve est presque vide) et reste plafonnée à 4 min.
- `0` désactive la progression hors ligne ; **RÉTABLIR 30 %** revient aux valeurs recommandées.

### Manuel
- Nouvelle sous-section **Réglages avancés** avec capture d'écran.
- Section **Résumé d'absence** mise à jour : budget réglable, plafond journalier, arrêt devant le gardien.

### Build debug
- Icône debug **émeraude** et libellé **La Marche des Âmes · dev** pour distinguer les deux installations.
- Badge **ver** à droite du palier dans le HUD, uniquement dans le build debug.

### Build
- **versionCode 39**, **versionName 0.17.5**, **targetSdk 36**.
- Tests : **91 / 91**.

### Assets
- `La-Marche-des-Ames-v0.17.5.apk` : APK GitHub, à installer sur le téléphone.

### Installation
- Même clé de signature que les versions précédentes : la progression est conservée.
- SHA-256 APK : `274C7343BE74D221C0BE19F43AB17A879C886F23F4FE94434B181811180911A5`

## v0.17.4 — 2026-11-20

**Titre :** DungeonStep - La Marche des Ames v0.17.4
**Assets :** `La-Marche-des-Ames-v0.17.4.apk` (46.8 Mo, 1 téléchargement(s))

## DungeonStep — La Marche des Âmes v0.17.4

### Fusion d'objets
- **Le bouton FUSIONNER tient toujours compte de l'objet ouvert** : il fait partie des trois objets consommés, complété par les **deux meilleurs** objets éligibles (même emplacement, même rareté, identifiés et non verrouillés).
- L'échec injustifié est corrigé : dès qu'un quatrième objet éligible existait, la fiche pouvait afficher « Il faut 3 objets… » alors que le bouton était actif.
- Le bouton affiche **FUSIONNER** quand la fusion est possible, sinon **FUSIONNER 2/3** et reste grisé.
- Les légendaires restent fusionnables entre eux et donnent un légendaire fusionné.

### Filtre Fusion dans l'inventaire
- Nouveau bouton **Fusion** à côté de **Score / Rareté / Niveau** : il ne montre que les objets dont le bouton **FUSIONNER** est actif.
- Indépendant du tri, cumulable avec le filtre d'emplacement, conservé au changement d'onglet.
- Liste vide : « Aucun objet fusionnable. »

### Manuel
- Section Inventaire : règle de fusion détaillée, nouveau paragraphe **Trier et filtrer le butin**.

### Build
- **versionCode 38**, **versionName 0.17.4**, **targetSdk 36**.
- Tests : **86 / 86**.

### Assets
- `La-Marche-des-Ames-v0.17.4.apk` : APK GitHub, à installer sur le téléphone.

### Installation
- Même clé de signature que les versions précédentes : la progression est conservée.
- SHA-256 APK : `38799539DFCFD14BF1A5EAF50FFF81A66BECBB4699E97B207CCAEA870ACE7BBB`

## v0.17.3 — 2026-11-20

**Titre :** DungeonStep - La Marche des Ames v0.17.3
**Assets :** `La-Marche-des-Ames-v0.17.3.aab` (28.3 Mo, 2 téléchargement(s)), `La-Marche-des-Ames-v0.17.3.apk` (46.8 Mo, 2 téléchargement(s))

## DungeonStep — La Marche des Âmes v0.17.3

### Atelier d'amélioration
- **AMÉLIORER** ouvre un atelier dans la fiche d'objet : niveau actuel → suivant, tableau `Équipé / Objet / Après`, coût en or et éclats, puis validation.
- Après une amélioration, **la feuille reste ouverte** : bandeau du gain appliqué, aperçu du niveau suivant et bouton **CONTINUER À AMÉLIORER**.
- Pour un objet équipé, le tableau devient `Actuel / Après +1 niv.` avec une ligne **Héros** (ATQ et PV totaux).
- La flèche **←** revient à la fiche, **FERMER** quitte.

### Atelier de reforge
- **REFORGER** affiche le pool complet : **25 %** aucun effet, **12,5 %** pour chacun des six effets.
- Coût **2 éclats** ; le niveau, les statistiques et l'ensemble ne changent pas.
- Le résultat affiche **ancien → nouveau** (ou « identique »), puis **REFORGER ENCORE** relance un tirage.

### Tableau de statistiques sur la fiche d'objet
- Remplace l'ancienne comparaison sur une ligne par un tableau `Statistique / Équipé / Cet objet`.
- Valeurs vertes ou rouges selon l'équipement, gains affichés en petit, valeur de vente conservée sous le tableau.

### Manuel et Play Store
- Nouvelle section **« Atelier : comparer, améliorer, reforger »**.
- Captures `item.jpg`, `atelier.jpg` et `codex_sets.jpg` actualisées.
- Cinquième capture Play `05-atelier.png`.

### Build
- **versionCode 37**, **versionName 0.17.3**, **targetSdk 36**.
- Tests : **84 / 84**.

### Assets
- `La-Marche-des-Ames-v0.17.3.apk` : APK GitHub, à installer sur le téléphone.
- `La-Marche-des-Ames-v0.17.3.aab` : **AAB pour la Play Console** (upload de publication), **pas** à installer sur un téléphone.

### Installation
- Même clé de signature que les versions précédentes : la progression est conservée.
- SHA-256 APK : `8B9D94ED620141D854011028B033BE6032E0B0A76CC055E923E88F0F84C0ECB1`
- SHA-256 AAB : `7C4BC8057FCA0371C98F738C40E89DD641C37FAD57B9BE89A13D4E2C8D6FC0B8`

## v0.17.2 — 2026-11-20

**Titre :** DungeonStep - La Marche des Ames v0.17.2
**Assets :** `La-Marche-des-Ames-v0.17.2.aab` (28.2 Mo, 2 téléchargement(s)), `La-Marche-des-Ames-v0.17.2.apk` (46.7 Mo, 3 téléchargement(s))

## DungeonStep — La Marche des Âmes v0.17.2

### Collections d'ensembles permanentes
- Une pièce d'ensemble **reste acquise** même si vous la vendez, la démontez ou la fusionnez. La collection ne recule plus.
- Nouvelle sauvegarde **version 4** : `setPiecesDiscovered` est recalé automatiquement depuis l'équipement, le sac et le coffre au chargement ou à l'import.
- Les pièces non identifiées comptent déjà dans la collection ; identifiez-les pour révéler leur ensemble.

### Codex clarifié
- **Collection x/6** (permanente) et **Équipé y/6** (bonus) sont affichés séparément.
- Le badge **BONUS ACTIF** ne s'affiche que si 2, 4 ou 6 pièces sont réellement équipées ; sinon « À ÉQUIPER ».
- Les slots manquants, le biome où farmer (« Se trouve : … ») et le prochain palier sont indiqués.
- La fiche d'objet affiche « équipé y/6 · collection x/6 ».

### Manuel
- Nouvelle section **« Compléter une collection »** : deux compteurs, règles de drop (rare+ / 30 % / biome), pièces non identifiées, tableau des 11 ensembles avec biomes et bonus 2/4/6.

### Build
- **versionCode 36**, **versionName 0.17.2**, **targetSdk 36**.
- Tests : **84 / 84**.

### Assets
- `La-Marche-des-Ames-v0.17.2.apk` : APK GitHub, à installer sur le téléphone.
- `La-Marche-des-Ames-v0.17.2.aab` : **AAB pour la Play Console** (upload de publication), **pas** à installer sur un téléphone.

### Installation
- Même clé de signature que les versions précédentes : la progression est conservée.
- SHA-256 APK : `5F0399481859C0A4F89678E826CBD7D3C29663222C8CC48157204F924B6C16B1`
- SHA-256 AAB : `15469BB30F62A4C08AB780DC7158E7A51CE87E0EBCAF976F336F20D35EE63C91`
- Manuel : <https://brunothesatellite.github.io/idle-step-rpg/manuel/>

## v0.17.1 — 2026-11-19

**Titre :** v0.17.1 - Pause de la marche
**Assets :** `La-Marche-des-Ames-v0.17.1.aab` (28.2 Mo, 1 téléchargement(s)), `La-Marche-des-Ames-v0.17.1.apk` (46.7 Mo, 6 téléchargement(s))

# v0.17.1 — Pause de la marche

## Nouveautés

- **Bouton de pause** sous le bouton de synchronisation, en haut à gauche de la scène.
- **Badge PAUSE** et toast de confirmation ; l'icône devient lecture pour reprendre.
- La pause fige la **marche, les combats, l'ouverture des coffres et les animations**.
- Un second appui reprend exactement au même point.
- **Pause de session** : elle reste active si l'application passe en arrière-plan. Fermer complètement l'application la termine et la simulation d'absence reprend normalement.
- **Temps réel conservé** : ferveur, buffs temporisés, coffres et reset de minuit continuent de suivre l'horloge réelle.
- **Manuel** : nouvelle section « Pause de la marche » avec capture.

## Distribution

- **APK GitHub** : mise à jour intégrée depuis GitHub, installable par-dessus la version précédente.
- **AAB Play** : variante sans installateur d'APK ni vérification GitHub, à envoyer dans la Play Console.
- Même nom de paquet et même clé release : la mise à jour croisée GitHub ↔ Play conserve les données.
- Cible Android 16 (`targetSdk 36`).

## Technique

- 81 tests unitaires, lint Android sans erreur.
- `versionCode` 35, `versionName` 0.17.1, Android 14+.

## Fichiers

| Fichier | Taille | SHA-256 |
|---|---|---|
| `La-Marche-des-Ames-v0.17.1.apk` | 48 928 245 o | `54D7F46E8254E07010B47D968E265EC052A7FA435CA887A289751BDAC70B7E9B` |
| `La-Marche-des-Ames-v0.17.1.aab` | 29 565 806 o | — |

Manuel en ligne : https://brunothesatellite.github.io/idle-step-rpg/manuel/

## Mise à jour

L'APK se met à jour par-dessus la 0.17.0 ou la 0.16.0 : la clé Garmin, la source de pas et la progression sont conservées. La v0.17.0 reste disponible ; cette 0.17.1 ajoute le bouton de pause et la section correspondante du manuel.

## v0.17.0 — 2026-11-19

**Titre :** DungeonStep - La Marche des Ames v0.17.0
**Assets :** `La-Marche-des-Ames-v0.17.0.apk` (46.5 Mo, 2 téléchargement(s))

## DungeonStep — La Marche des Âmes v0.17.0

> **Réédition** : l'icône définitive est maintenant celle de l'application (release **et** debug), et l'Histoire est intégrée au README et au manuel. Le numéro de version reste 0.17.0 : si vous avez déjà cette version, réinstallez l'APK pour voir la nouvelle icône.

### Distribution
- **Deux variantes** avec le même nom de paquet et la même clé release :
  - **GitHub** (cet APK) : mise à jour intégrée depuis les releases.
  - **Play** (AAB) : sans installateur d'APK, sans vérification GitHub ; Play gère les mises à jour.
- **Mise à jour croisée Play ↔ GitHub** : même paquet, même certificat, donc les données sont conservées dans les deux sens (avec un `versionCode` supérieur).
- Pack Play Store complet dans `playstore/`.

### Application
- **Nouvelle icône** : porte scellée, Kaelen, Balrug, buste de statue et lanterne, avec calques adaptive icon et toutes les densités. Utilisée par la version release et la copie debug.
- **Histoire** : le lore long (Kaelen, les âmes retenues, les huit biomes) est ajouté au README et au manuel, dans une section « Histoire » au début.
- Variante Play : `REQUEST_INSTALL_PACKAGES` et le `FileProvider` d'installation retirés, carte « Mises à jour » et dialogues masqués.
- Manuel : section **Durée de vie estimée** (5 000, 8 000 et 10 000 pas par jour).
- Tests : **81 / 81**.

### Installation
- APK signé avec la même clé que les versions précédentes : la progression est conservée.
- Android peut demander d'autoriser « Installer des applications inconnues » pour La Marche des Âmes.
- SHA-256 APK : `B0777420B9A080F7117E35AD2BAA2439570C6ACA031328CEDA87FF2843ED51F6`
- Manuel : <https://brunothesatellite.github.io/idle-step-rpg/manuel/>

## v0.16.0 — 2026-11-18

**Titre :** v0.16.0 - Effets actifs et fiches du Codex
**Assets :** `La-Marche-des-Ames-v0.16.0.aab` (27.7 Mo, 1 téléchargement(s)), `La-Marche-des-Ames-v0.16.0.apk` (46.2 Mo, 2 téléchargement(s))

# v0.16.0 — Effets actifs et fiches du Codex

## Bandeau des effets actifs

- Une **seule ligne** juste au-dessus de la ferveur et de la réserve.
- Jusqu'à **6 effets** simultanés, avec la **durée restante en direct**.
- Largeur adaptée au nombre d'effets, **plafonnée à un tiers**.
- Bonus colorés par famille, **malus en rouge** (FRAGILE, RACINES, ACIDE, GIVRE).
- Compte à rebours rafraîchi chaque seconde, même en pause ou le sac ouvert.
- Les durées sont **réelles** : un effet de 30 s expire au bout de 30 s, même application fermée.
- Le bandeau disparaît quand aucun effet n'est actif.

## Codex

- **Fiche de créature** : image, biome, zone d'apparition, niveau, PV, ATQ, cadence, or, XP, victimes et règle de biome.
- **Fiche de gardien** : les mêmes statistiques, plus la capacité, ses effets réels et les modificateurs aléatoires possibles.
- Les créatures et gardiens non vaincus restent **verrouillés**.
- Les fiches respectent la **taille de texte** choisie dans les réglages.

## Technique

- 81 tests unitaires, lint Android sans erreur.
- `versionCode` 33, `versionName` 0.16.0, Android 14+.
- APK universel signé, AAB inclus.

## Fichiers

| Fichier | Taille | SHA-256 |
|---|---|---|
| `La-Marche-des-Ames-v0.16.0.apk` | 48 408 477 o | `BE9553D43556A4A8A8EBD131628792BED1A569733F27C6FFA763ED6C0067EB3A` |
| `La-Marche-des-Ames-v0.16.0.aab` | 29 075 410 o | — |

Le manuel est aussi en ligne : https://brunothesatellite.github.io/idle-step-rpg/manuel/

## Mise à jour

L'APK se met à jour par-dessus la version précédente : la clé Garmin, la source de pas et la progression sont conservées. Le manuel contient les nouvelles captures des effets actifs et des fiches du Codex.

## v0.15.0 — 2026-11-18

**Titre :** La Marche des Âmes v0.15.0
**Assets :** `La-Marche-des-Ames-v0.15.0.apk` (45.6 Mo, 2 téléchargement(s))

## La Marche des Âmes v0.15.0

### Nouveau
- **Diagnostic de synchronisation** — Réglages → Diagnostic. Source active, fenêtre d'import des 7 derniers jours avec l'état de chaque journée (crédité, aujourd'hui, avant connexion, aucun pas), garde-fous appliqués (7 jours, 60 000 pas/jour, 100 000 pas par import, 250 pas/min) et bouton de synchronisation manuelle. Lecture seule : aucun pas n'y est ajouté.

### Corrigé
- **Santé Connect en arrière-plan** : le worker périodique ne tente plus la lecture. Android la refuse sans `READ_HEALTH_DATA_IN_BACKGROUND`, ce qui remplissait le journal d'erreurs « lecture impossible » toutes les heures. La synchronisation Santé Connect se fait à l'ouverture et manuellement, sans perte de pas.
- **Cause réelle des échecs** : le type et le message de l'exception sont désormais conservés, écrits dans logcat et ajoutés au journal, au lieu d'un message générique.

### Amélioré
- **Journal de synchronisation horodaté** : `18/11 16:32 · OK · +12 pas` (jour et heure).
- **Manuel** : section diagnostic, règle Santé Connect et arrière-plan, journal horodaté.

### Installation
- APK signé avec la même signature que les versions précédentes : la progression est conservée.
- Android peut demander d'autoriser « Installer des applications inconnues » pour La Marche des Âmes.
- SHA-256 APK : `F852E231F78E21D9F99ABBEC5202E407952623E90E4EC2270B6A711617BCD731`
- Manuel en ligne : <https://brunothesatellite.github.io/idle-step-rpg/manuel/>

## v0.14.0 — 2026-11-18

**Titre :** v0.14.0 - choix de la source de pas
**Assets :** `La-Marche-des-Ames-v0.14.0.apk` (45.6 Mo, 2 téléchargement(s))

﻿- **Choix de la source de pas** : **Garmin** (Personal API) ou **Santé Connect**. Le choix se fait à l'onboarding, puis se change dans **Réglages → Source de pas**, avec une carte par source, son état et son journal de synchronisation.
- **Santé Connect** : lecture locale des pas (`android.permission.health.READ_STEPS`), agrégation par origine (téléphone, montre, application de santé) en gardant le total le plus élevé par jour, sans double comptage. Fonctionne avec Google Fit, Samsung Health ou une montre qui écrit dans le hub.
- **Correction « lecture des pas refusée »** : Android 14+ exige une activité sur `VIEW_PERMISSION_USAGE` + `HEALTH_PERMISSIONS`. Sans elle, Santé Connect refermait la demande d'autorisation aussitôt.
- **Changement de source sans double comptage** : le premier import de la nouvelle source sert de base, le total du jour ne recule jamais et aucun pas n'est recrédité.
- **Nouvelle fenêtre d'import** : la toute première synchronisation ne crédite que les **pas de la journée**. Ensuite, le rattrapage lit **7 jours maximum**, borné par la **date de première connexion**. Un oubli de synchronisation est donc rattrapé, mais on ne démarre pas le jeu avec une réserve héritée de la semaine passée.
- **Journal générique** : les messages suivent la source active (« Garmin synchronisé » ou « Santé Connect synchronisé »).
- **Mise à jour technique** : `compileSdk 36`, AGP 8.10.1, Health Connect 1.1.0 stable.

---

### Installation

APK signé avec la même clé : `La-Marche-des-Ames-v0.14.0.apk` (Android 14+).

SHA-256 APK : 18B0B7A9E0044D9F8BCA5126EFF30089B7CB4044F3DBBC1D972B462B345DC940

Manuel : <https://brunothesatellite.github.io/idle-step-rpg/manuel/>

Les données existantes sont conservées : même identifiant d'application et même signature.

## v0.13.2 — 2026-11-18

**Titre :** v0.13.2 - sauvegardes fiables et autosave
**Assets :** `La-Marche-des-Ames-v0.13.2.apk` (43.2 Mo, 3 téléchargement(s))

- **Sauvegarde atomique** : le fichier temporaire est désormais écrit et synchronisé sur disque, puis déplacé sur `game_state.json` en un seul `rename`. L'ancien code **supprimait** le fichier principal avant de le remplacer : un arrêt du processus entre les deux pouvait faire repartir du `.bak` (jusqu'à 10 min de retard) ou d'une partie neuve si le `.bak` n'existait pas encore.
- **Sauvegarde périodique pendant le jeu** : toutes les 30 secondes environ, et immédiatement à chaque changement de palier ou de niveau. Jusqu'ici rien n'était écrit entre deux événements : installer une mise à jour pouvait ramener le jeu **avant un gardien déjà battu**.
- **Sauvegarde avant l'installateur de mise à jour** : la progression est écrite sur disque juste avant de lancer l'installation de l'APK, qui tue le processus sans passer par `onPause`.
- **Manuel** : la FAQ précise la sauvegarde automatique.

---

### Installation

APK signé avec la même clé : `La-Marche-des-Ames-v0.13.2.apk` (Android 14+).

SHA-256 APK : 682AA71AFF8C1957D2549C4598209900133F5D9950C0BC3C386636C166390B9B

Manuel : <https://brunothesatellite.github.io/idle-step-rpg/manuel/>

## v0.13.1 — 2026-11-18

**Titre :** v0.13.1 - reserve de pas et idle sur la reserve heritee
**Assets :** `La-Marche-des-Ames-v0.13.1.apk` (43.2 Mo, 1 téléchargement(s))

- **Réserve de pas visible** : une seconde jauge sous celle de la ferveur, avec la valeur (pleine à 4 000 pas).
- **Idle sur la réserve héritée** : la synchronisation Garmin est téléchargée **en parallèle** mais n'est **appliquée qu'à la fermeture du résumé**. La simulation d'absence travaille donc uniquement sur la réserve présente au moment de la fermeture, et plus jamais sur les pas qui viennent d'arriver.
- **Résumé systématique** dès 2 min d'absence, y compris réserve vide : le rapport explique alors que rien n'a avancé et annonce que les pas de la synchro arrivent ensuite.
- **Butin détaillé** dans le résumé : objets récupérés (nom, 8 max puis « … et N autres »), consommables, rations et éclats.
- **Message de synchro après fermeture** : « Garmin synchronisé · +N pas » (ou le rattrapage) n'apparaît qu'une fois le résumé refermé.
- **Bandeau de gardien compact** : une seule ligne `GARDIEN · nom · modificateur · Niv.` placée sous la barre de distance, qui ne recouvre plus le SAC, le COFFRE, le bouton de synchronisation ni le nom du gardien. Les étiquettes « BÉNÉDICTION » et « PHASE 2 » sont remontées au-dessus.
- **Manuel** : légende de l'écran mise à jour avec la réserve de pas (17 repères) et nouvelle section sur l'enchaînement résumé → synchro.

---

### Installation

APK signé avec la même clé : `La-Marche-des-Ames-v0.13.1.apk` (45,3 Mo, Android 14+).

SHA-256 APK : 81A122864014F46502E5ED2D0E9BF083D0D028709CF2487E43267EB3E9C2847D

Manuel : <https://brunothesatellite.github.io/idle-step-rpg/manuel/>

## v0.13.0 — 2026-11-17

**Titre :** v0.13.0 - consommation, codex et compte Garmin
**Assets :** `La-Marche-des-Ames-v0.13.0.apk` (43.2 Mo, 1 téléchargement(s))

- **Consommation** : le décor animé se fige après 10 s sans pas (météo, aura de ferveur, balancements) et la simulation passe de 10 Hz à 2 Hz au repos. Mesuré sur appareil : **~60 fps → 2 fps**, CPU à l'écran **~60-70 % → ~30 %**, arrière-plan inchangé à 0 %.
- **Boucles supprimées** : l'horodatage d'activité n'écrit plus la sauvegarde toutes les 5 s (mémoire seule, toutes les 30 s) ; maintenance toutes les **2 h** (au lieu de 15 min), synchro de fond toutes les **60 min** avec backoff, widget toutes les 15 min.
- **Codex** : bandeau de couloir sous chaque biome découvert, dessiné par la **même fonction** que la scène (arches, torches, statues, props, sol).
- **Interface** : bouton « G » supprimé ; carte « Compte Garmin » déplacée des Stats vers les **réglages** ; devises réorganisées (or seul en haut, rations/éclats/réglages en bas) ; alerte « ! » affichée uniquement si Garmin n'est pas configuré ou si la clé approche de l'expiration.
- **Onboarding** : la page Garmin propose « CONNECTER GARMIN » et précise que sans clé la marche ne peut pas démarrer.
- **Manuel** : captures refaites (accueil, réglages, camp, objet, inventaire, codex) et nouvelle **légende numérotée** de l'écran principal.
- **Résumé d'absence** (depuis 0.12.1) : la synchro Garmin est effectuée avant la simulation, l'écran « Bon retour » réapparaît.

---

### Installation

APK signé avec la même clé que les versions précédentes : `La-Marche-des-Ames-v0.13.0.apk` (45,3 Mo, Android 14+).

SHA-256 APK : 663B58058F12377FFF1ACBF09EACD687EDD7CD5070DF329735B65367E9F4CFFB

Le manuel est publié sur <https://brunothesatellite.github.io/idle-step-rpg/manuel/>. L'application vérifie l'empreinte après téléchargement depuis *Réglages > Mises à jour*.

## v0.12.1 — 2026-11-17

**Titre :** v0.12.1 - le resume d'absence repart de la synchro
**Assets :** `La-Marche-des-Ames-v0.12.1.apk` (43.1 Mo, 1 téléchargement(s))

- **Résumé d'absence rétabli** : la synchronisation Garmin est désormais effectuée **avant** la simulation d'absence. La réserve étant presque toujours vide à la fermeture (la marche l'a consommée pendant la session), la simulation ne se déclenchait jamais et l'écran « Bon retour » n'apparaissait plus.
- **Synchro de reprise** silencieuse et attendue : pas de toast avant le résumé, indicateur de synchronisation et « +N pas » mis à jour, simulation lancée sur une réserve fraîche.
- **Le jeu continue de tourner pendant l'appel réseau** ; seule la simulation suspend la marche.
- Aucun changement de règle de jeu : 30 % des pas du jour, 4 minutes simulées, arrêt sur événement (rencontre spéciale, portail du gardien, coffre, défaite).

---

### Installation

APK signé avec la même clé que les versions précédentes : `La-Marche-des-Ames-v0.12.1.apk` (45,2 Mo, Android 14+).

SHA-256 APK : 44719ADB08493687A376D731FC532237BC481A26FBD89FFE3A43C9369A98AB32

L'application vérifie cette empreinte après téléchargement depuis *Réglages > Mises à jour*. La progression est conservée lors de la mise à jour.

## v0.12.0 — 2026-11-17

**Titre :** v0.12.0 - progression hors ligne, forge et lisibilite
**Assets :** `La-Marche-des-Ames-v0.12.0.apk` (43.1 Mo, 2 téléchargement(s))

- **Progression hors ligne repensée** : la simulation ne consomme plus que **30 % des pas du jour** (et **4 minutes de jeu** au maximum), au lieu de 100 % du rythme jusqu'à 15 minutes. La réserve restante vous attend à l'ouverture.
- **Budget partagé** : la synchro de fond de 30 minutes puise dans le même quota ; une synchronisation manuelle ne simule plus rien, les pas importés restent à vivre à l'écran.
- **Arrêt sur événement** : la simulation s'interrompt sur une rencontre spéciale, devant le portail du gardien (s'il reste moins d'un palier de budget), sur un coffre ou une défaite, et laisse le reliquat.
- **Marche ralentie** : 140 → **60 pas/s** (30 sous 600 pas), pour que la consommation des pas soit visible.
- **Deux nouvelles notifications** : « Rencontre en attente » et « Gardien en attente », avec leurs cases dans les réglages ; elles ne se déclenchent que si l'application n'est pas à l'écran et respectent les heures calmes.
- **Forge corrigée** : améliorer, reforger, verrouiller et démonter fonctionnent désormais sur un **objet équipé** (le moteur ne cherchait que dans l'inventaire). Les messages disent ce qui manque (« Il manque 120 or et 1 éclat ») au lieu du générique « or ou éclat insuffisant ».
- **Aperçu d'amélioration** : le bandeau compare `ACTUEL → APRÈS +1 NIV.` avec des deltas colorés (vert/rouge) et chiffrés ; le bouton affiche le coût complet et se désactive quand il n'est pas payable.
- **Progression des objets** : chaque statistique non nulle gagne au moins +1 par niveau (8 % de 2 PV ou de 6 % crit. ne s'arrondissent plus à zéro).
- **Taille de texte « Maximal »** (2,0×), cumul system+app plafonné à 2,2×, et les fenêtres de dialogue suivent enfin le réglage. Croix de fermeture recentrée dans toutes les fenêtres, débordements corrigés en grand texte.
- **Campement** : titre « CAMPEMENT » (le feu veille en sous-titre), pastilles rations et éclats, et tente, feu et héros restent visibles au-dessus du panneau à toutes les tailles.
- **Portail du gardien** : porte plus haute que le gardien (132 × 116 dp), gardien à 96 dp, ovale décoratif supprimé au profit d'un halo.
- **Coffre** : croix et bouton « Masquer · le forçage continue », progression de la serrure sur la pastille du HUD, toast à l'ouverture.
- **Manuel et spécifications** mis à jour (règles d'absence, forçage de coffre).

---

### Installation

APK signé avec la même clé que les versions précédentes : `La-Marche-des-Ames-v0.12.0.apk` (45,2 Mo, Android 14+).

SHA-256 APK : 480A56E3C36A27CAFB6A4FB92AC5B258B2C6B4C5B52B7186D2FEDDFF6D9777A2

L'application vérifie cette empreinte après téléchargement depuis *Réglages > Mises à jour*. La progression est conservée lors de la mise à jour.

## v0.11.0 — 2026-11-16

**Titre :** v0.11.0 - Resume d'absence
**Assets :** `La-Marche-des-Ames-v0.11.0.aab` (26.4 Mo, 1 téléchargement(s)), `La-Marche-des-Ames-v0.11.0.apk` (43.1 Mo, 2 téléchargement(s))

# v0.11.0 — Résumé d'absence

Quand vous fermez le jeu avec une réserve de pas, cette réserve continue de s'écouler pendant votre absence. Au retour, un écran **« Bon retour »** résume ce qui s'est passé.

## Nouveautés

- **Résumé d'absence** : simulation à la réouverture de l'application, avec un écran récapitulatif complet.
- **Consommation** : 100 % du rythme actif, plafonnée à **60 000 pas** ou **15 minutes simulées**, au premier des deux atteint. La réserve restante est conservée.
- **Déclenchement** : absence d'au moins 2 minutes, réserve non vide, hors camp et hors rencontre spéciale.
- **Contenu** : durée, pas consommés, mètres, combats, or, paliers, gardiens, niveaux, coffres et réserve restante.
- **Rencontre spéciale** : la simulation s'arrête dessus et le résumé l'indique, sans résoudre la rencontre à votre place.
- **Défaite** : repli signalé, zone de farm retrouvée au retour.
- **Fusion Garmin** : si une synchronisation et une absence arrivent ensemble, un seul rapport combine les pas importés et la réserve, sans double comptage.
- **Rapport persistant** : il reste disponible tant que « REPRENDRE LA MARCHE » n'a pas été utilisé.
- **Anti-abus** : absence bornée à 7 jours, ferveur décrémentée selon le temps réel.
- **Manuel** : nouvelle section 19 « Résumé d'absence » avec capture de l'écran « Bon retour ».

## Technique

- 74 tests unitaires (simulation d'absence, plafond, camp, fusion des rapports), lint Android OK.
- `versionCode` 25, `versionName` 0.11.0.
- APK universel signé, Android 14+.

## Fichiers

| Fichier | Taille | SHA-256 |
|---|---|---|
| `La-Marche-des-Ames-v0.11.0.apk` | 45 166 023 o | `BB1BA1D8975556762CEADD58F22B42A0C73E8ED45583323516D6A7ACC5754C42` |
| `La-Marche-des-Ames-v0.11.0.aab` | 27 662 229 o | — |

Le manuel est aussi en ligne : https://brunothesatellite.github.io/idle-step-rpg/manuel/

## Mise à jour

L'APK se met à jour par-dessus la version précédente : la clé Garmin et la progression sont conservées.

## v0.10.0 — 2026-11-16

**Titre :** v0.10.0 - Musiques d'ambiance 8 bits
**Assets :** `La-Marche-des-Ames-v0.10.0.aab` (26.3 Mo, 1 téléchargement(s)), `La-Marche-des-Ames-v0.10.0.apk` (43.0 Mo, 1 téléchargement(s))

## La Marche des Âmes - v0.10.0

- **16 musiques d'ambiance 8 bits** : deux variantes de 3 minutes pour chacun des 8 biomes, générées pour le projet et publiées en **CC0**.
- **Boucle longue** avec démarrage à une **position aléatoire** à chaque ouverture, pour éviter la redondance.
- **Fondu court** au changement de biome.
- **Réglages musique** : toggle `Musique d'ambiance` et volume indépendant des effets sonores, **activée à 30 % par défaut**.
- **Cycle de vie** : pause automatique quand l'application passe en arrière-plan, reprise au retour.
- **Technique** : Media3 ExoPlayer, pistes OGG Vorbis mono 22 050 Hz, environ 16 Mo de musiques.
- **Générateur inclus** : `tools/generate_music.py` permet de régénérer les 16 pistes.
- **Manuel** : nouvelle section musique avec capture des réglages.
- **Qualité** : 70 tests unitaires et lint Android OK.

### Installation

```bash
adb install -r La-Marche-des-Ames-v0.10.0.apk
```

SHA-256 APK : E44B2CC4749C552725479141A5F06C54281E823D424B2C7F0E747033083AA586

## v0.9.1 — 2026-11-16

**Titre :** v0.9.1 - Icône de synchronisation
**Assets :** `La-Marche-des-Ames-v0.9.1.aab` (9.5 Mo, 1 téléchargement(s)), `La-Marche-des-Ames-v0.9.1.apk` (23.4 Mo, 1 téléchargement(s))

## La Marche des Âmes - v0.9.1

- **Icône de synchronisation Garmin redessinée** : remplacement du glyphe dessiné à la main par une icône vectorielle à deux flèches, beaucoup plus lisible dans le petit bouton carré.
- **Licence** : l'icône est dérivée de Material Icons (Apache 2.0), créditée dans `LICENSES.md`.
- **Qualité** : 70 tests unitaires et lint Android OK.

### Installation

```bash
adb install -r La-Marche-des-Ames-v0.9.1.apk
```

SHA-256 APK : 62C55B31419C5E598F0D8302E884A4277C8D73FB37287B2E301E53274C8A4F07

## v0.9.0 — 2026-11-16

**Titre :** v0.9.0 - Codex, mode Farm et synchro compacte
**Assets :** `La-Marche-des-Ames-v0.9.0.aab` (9.5 Mo, 1 téléchargement(s)), `La-Marche-des-Ames-v0.9.0.apk` (23.4 Mo, 1 téléchargement(s))

## La Marche des Âmes - v0.9.0

- **Codex** : nouvel écran de collection avec trois onglets — **Bestiaire**, **Ensembles** et **Biomes**.
- **Bestiaire** : créatures et gardiens découverts, silhouettes inconnues, victimes et capacités des gardiens.
- **Ensembles** : pièces possédées par emplacement, bonus 2/4/6 et prochain palier.
- **Biomes** : règle active, créatures, gardiens et objets découverts pour chaque biome.
- **Mode Farm** : bouton `ACTIVER` / `DÉSACTIVER` dans la carte BIOME. Inactif par défaut. Le palier est verrouillé, le gardien revient et les jalons sont désactivés. Badge **FARM** dans le HUD.
- **Synchronisation compacte** : petit bouton carré en haut à gauche de la scène, aligné avec le SAC, avec icône ↻ et point d'état. La grosse barre du bas est supprimée.
- **Migration** : les gardiens déjà vaincus sont récupérés depuis le bestiaire pour le Codex.
- **Manuel** : captures du Codex, du mode Farm et du nouveau bouton de synchronisation.
- **Qualité** : 70 tests unitaires et lint Android OK.

### Installation

```bash
adb install -r La-Marche-des-Ames-v0.9.0.apk
```

Ou utilisez la mise à jour intégrée depuis l'application.

SHA-256 APK : 28E712F66CC2C750F233B4F8F0EB79FEE90890F8B16DDDBE28D7C5C9F4ACD362

## v0.8.1 — 2026-11-16

**Titre :** v0.8.1 - Casques et armures distincts
**Assets :** `La-Marche-des-Ames-v0.8.1.aab` (9.1 Mo, 1 téléchargement(s)), `La-Marche-des-Ames-v0.8.1.apk` (22.9 Mo, 1 téléchargement(s))

## La Marche des Âmes - v0.8.1

- **Correction du paper doll** : les heaumes rares/épiques affichent désormais un vrai casque au lieu d'une couche de cheveux rouges.
- **Armures différenciées** : cotte de mailles en acier, plate sombre, cuir, robe, carapace et armure de glace ont des sprites distincts.
- **Casques et bottes différenciés** : heaume gravé, capuche elfique, casque vert, bottes de mousse, bottes des cimes et bottes de cuir/métal ne partagent plus la même couche.
- **Jambes cohérentes** : les jambes suivent le matériau du torse (maille sombre, plate claire, tissu ou pantalon).
- **Qualité** : 63 tests unitaires et lint Android OK.

### Installation

```bash
adb install -r La-Marche-des-Ames-v0.8.1.apk
```

Ou utilisez la mise à jour intégrée depuis l'application.

SHA-256 APK : BF389DF31003F1B49F8188CEB090513036EDA6180DFFF99AC3288CCBA4A2F322

## v0.8.0 — 2026-11-16

**Titre :** v0.8.0 - Inventaire lisible, paper doll fidèle et gardiens mémorables
**Assets :** `La-Marche-des-Ames-v0.8.0.aab` (9.1 Mo, 1 téléchargement(s)), `La-Marche-des-Ames-v0.8.0.apk` (22.9 Mo, 1 téléchargement(s))

## La Marche des Âmes - v0.8.0

- **Inventaire lisible** : barre de synchronisation compacte sur les onglets Inventaire et Améliorations, emplacements équipés cliquables et libellé d'ensemble visible sur les cartes de butin.
- **Paper doll fidèle** : l'apparence suit la famille d'objet (épée, hache, masse, bâton, dague, lance, fouet ; cuir, maille, plate, robe ; capuche, heaume, couronne ; bottes de cuir ou de métal), plus seulement la rareté.
- **Règles de biome** : 8 modificateurs actifs pour le héros et les ennemis, affichés dans une carte BIOME de l'onglet Stats.
- **Gardiens mémorables** : capacités propres (drain d'âme, régénération, frappe lourde, blindage, souffle, racines, acide, pression) avec libellé sous la barre de vie et bandeau d'activation.
- **Manuel enrichi** : nouvelles captures du paper doll, de l'inventaire et d'un combat de gardien ; tableaux des règles de biome et des capacités.
- **Qualité** : 63 tests unitaires et lint Android OK.

### Installation

```bash
adb install -r La-Marche-des-Ames-v0.8.0.apk
```

Ou utilisez la mise à jour intégrée depuis l'application.

SHA-256 APK : 0BF7F10852E052825812460867A3F7E1BD9D074FA70BE2A0C96FC9F46F806CEC

## v0.7.0 — 2026-11-16

**Titre :** v0.7.0 - Huit biomes, nouveau bestiaire et paper doll
**Assets :** `La-Marche-des-Ames-v0.7.0.aab` (8.9 Mo, 1 téléchargement(s)), `La-Marche-des-Ames-v0.7.0.apk` (22.7 Mo, 1 téléchargement(s))

## La Marche des Âmes - v0.7.0

- **8 biomes distincts** : Cryptes de Vael, Catacombes, Cavernes de cendre, Forge noyée, Cimes du Serment, Bois des Âmes, Abîme organique et Trône des Âmes.
- **Décors dédiés** : murs, sols, statues, props, portes runiques/scellées et météo propres à chaque biome (neige, cendres, spores, feuilles, étincelles, poussière).
- **Plus de 60 nouvelles créatures** réparties par biome et par distance dans le palier, avec des boss de biome et les gardiens de jalon.
- **36 nouveaux objets de butin** : armes, armures, casques, bottes, gantelets et talismans thématiques.
- **8 nouveaux ensembles** : Ossature ancienne, Toile des catacombes, Cendre éternelle, Forge abyssale, Serment des cimes, Bois des âmes, Abîme organique et Trône des âmes.
- **2 nouveaux effets d'objet** : Vitalité et Précision.
- **4 nouveaux consommables** : Potion de hâte, Potion de force, Potion de régénération et Viande séchée.
- **Héros en paper doll** : casque, armure, bottes et arme changent selon l'équipement porté.
- **Retour visuel d'impact adouci** : halo radial autour de l'ennemi au lieu d'un grand carré translucide.
- **Manuel enrichi** : galerie de 8 captures de biomes et section consacrée aux nouveaux décors, créatures et tables de butin.
- **Assets CC0 DCSS** vérifiés contre la liste des licences incertaines ; crédits mis à jour.

### Installation

```bash
adb install -r La-Marche-des-Ames-v0.7.0.apk
```

Ou utilisez la mise à jour intégrée depuis l'application.

SHA-256 APK : 4CC61F7A8A9F695BFCE72F4334B22294D06CF1110EFC399F904F20017BC2D2D5

## v0.6.2 — 2026-11-16

**Titre :** v0.6.2 - Mises a jour GitHub et installation in-app
**Assets :** `La-Marche-des-Ames-v0.6.2.aab` (8.4 Mo, 1 téléchargement(s)), `La-Marche-des-Ames-v0.6.2.apk` (22.0 Mo, 2 téléchargement(s))

## La Marche des Âmes - v0.6.2

- **Mises à jour depuis GitHub** : vérification automatique au lancement, au plus une fois toutes les 24 h, active par défaut et désactivable dans les réglages.
- **Vérification manuelle** : `Réglages → Mises à jour → Vérifier maintenant`.
- **Installation in-app** : l'APK de la dernière release est téléchargé, vérifié par **SHA-256** quand l'empreinte est publiée, puis installé via Android après confirmation. La première fois, autorisez « Installer des applications inconnues » pour La Marche des Âmes.
- **Après installation** : l'application propose d'ouvrir la **page GitHub de la release** pour lire les changements, ou de le faire plus tard.
- **Données conservées** : progression, réglages et clé Garmin restent en place tant que l'APK est signé avec la même clé.
- **Tests** : 12 nouveaux tests unitaires pour la comparaison de versions, le parsing des releases et le calcul SHA-256.
- **Manuel** : nouvelle section « Mises à jour » et documentation README/CHANGELOG mise à jour.

### Installation

```bash
adb install -r La-Marche-des-Ames-v0.6.2.apk
```

Ou utilisez directement la mise à jour intégrée depuis une version qui l'embarque.

SHA-256 APK : C5EF6FB5A48AC4E4AEBA73958CB9AFE39AF7BD784E5CE1C29BD705709ED7A31C

## v0.6.1 — 2026-11-16

**Titre :** v0.6.1 - Manuel complet et plafond marathon
**Assets :** `app-release.aab` (8.3 Mo, 1 téléchargement(s)), `app-release.apk` (22.0 Mo, 1 téléchargement(s))

## La Marche des Âmes - v0.6.1

- **Manuel utilisateur complet** en HTML : [`manuel/index.html`](https://github.com/brunothesatellite/idle-step-rpg/blob/main/manuel/index.html)
- **Manuel intégré** dans l'application : `Réglages → Aide → Ouvrir le manuel du jeu`, avec les captures et toutes les mécaniques.
- **Plafond de rattrapage Garmin porté à 100 000 pas** par synchronisation : un marathon (~45 000 pas) et plusieurs jours passent sans troncature.
- **Inventaire plein** : le meilleur objet reste dans le sac, le moins bon part au coffre de camp ; les objets verrouillés sont protégés.
- **Notes de release GitHub corrigées** : les blocs de code mal échappés ont été réparés.
- README réécrit : présentation du jeu, manuel, architecture, compilation, installation et connexion Garmin.

### Installation

```bash
adb install -r La-Marche-des-Ames-v0.6.1.apk
```

Ou télécharger l'APK sur le téléphone et autoriser l'installation d'applications inconnues.

SHA-256 APK : `23E0DBCC3D54207486437B2C1B1F4E3DED4BA785EE6F8D6FFE13F800D51B18F8`

## v0.6.0 — 2026-11-16

**Titre :** v0.6.0 - Sons 8 bits et réglages audio
**Assets :** `app-release.aab` (7.4 Mo, 1 téléchargement(s)), `app-release.apk` (20.8 Mo, 1 téléchargement(s))

## La Marche des Ames - v0.6.0

- **11 sons 8 bits originaux** sous licence CC0 : combat, critique, degats, niveau, gardien, coffre, consommable, victoire, defaite, forge.
- **Reglages audio** : sons on/off, volume 0-100 %, bouton Tester le son.
- Porte du gardien : libelle **EN ATTENTE DE PAS** quand il manque des pas avant les 100 m.
- Backlog : anglais, Room, Play Store, equilibrage long terme, farm 0 pas et batterie faible restent explicitement non planifies.
- Inclut les consommables v0.5.3, le camp lisible, les rations affichees et la fenetre de resultat des rencontres.

### Installation

```bash
adb install -r La-Marche-des-Ames-v0.6.0.apk
```
SHA-256 APK : 878B3ABC4CE379E95371A285E3A6D8683B5EF5C92DC25E3F24D67E609DEEBD72

## v0.5.3 — 2026-11-16

**Titre :** v0.5.3 - Consommables et camp lisible
**Assets :** `app-release.aab` (7.3 Mo, 1 téléchargement(s)), `app-release.apk` (20.7 Mo, 1 téléchargement(s))

## La Marche des Ames - v0.5.3

- **Categorie Consommables** dans l'inventaire et le sac de scene : 12 objets avec icones dediees.
- Soins et ferveur : potion, herbe de lune, champignon de braise, elixir du veilleur, pain de route.
- Combat : fiole de rage, ecorce de fer, poudre d'eclair, bombe de feu, elixir de vampire, parchemin critique, champignon berserk.
- Butin sur les monstres, gardiens et coffres ; buffs temporaires avec compte a rebours.
- **Rations enfin visibles** dans la carte Ressources (rations, eclats, or, consommables), dans l'en-tete et au camp.
- **Camp** : heros, tente et feu repositionnes au-dessus du panneau ; le feu et la tente sont visibles.
- Bouton **POTIONS** au camp et **SAC** en scene.

### Installation

```bash
adb install -r La-Marche-des-Ames-v0.5.3.apk
```
SHA-256 APK : 45106206FC817FEC225C92153107273B5EF6051332228B909D8817E603AE7BAE

## v0.4.2 — 2026-11-16

**Titre :** v0.4.2 - Résultats des rencontres et toasts
**Assets :** `app-release.aab` (7.3 Mo, 1 téléchargement(s)), `app-release.apk` (20.6 Mo, 2 téléchargement(s))

## La Marche des Ames - v0.4.2

- **Resultat visible** apres chaque rencontre speciale : pari gagne/perdu, achat, offrande, soin. Le pari affiche clairement ┬½ Pari gagne ┬À +300 or ┬╗ ou ┬½ Pari perdu ┬À -100 or ┬╗.
- **Plus de toasts empiles** : un seul toast a la fois, remplace par le suivant.
- **Ameliorations** : plus de toast repetitif a chaque achat, la carte et l'or se mettent a jour directement.
- Inclut toutes les nouveautes v0.4.0 : simulation hors ligne, onboarding, forge, sets, objets non identifies, meteo, boss uniques, widget, tests et CI.

### Installation

```bash
adb install -r La-Marche-des-Ames-v0.4.2.apk
```
SHA-256 APK : 13D443FD008B59FC7E8AD681B9090EA67B700503A4151A24DDA2043003216521

## v0.4.0 — 2026-11-16

**Titre :** v0.4.0 - Simulation hors ligne, forge et robustesse
**Assets :** `app-release.aab` (7.3 Mo, 1 téléchargement(s)), `app-release.apk` (20.6 Mo, 1 téléchargement(s))

## La Marche des Ames - APK Android v0.4.0

- **Simulation hors ligne** : les pas Garmin rattrapes font avancer combats, or et paliers, avec un recap detaille.
- **Robustesse** : camp qui reinitialise le combat, ferveur rattrapee hors ligne, workers coffre/cle, notifications par categorie, heures calmes, liens directs, import avec apercu, limite 250 pas/min, backoff 429.
- **Onboarding** en 4 ecrans, revoyable.
- **Jalons complets** : or, eclats, titres, journal de legende, compteurs de vie, serie de jours, semaine.
- **Camp** : pas en attente, or, journal, inventaire, veillee gratuite +15 %.
- **Inventaire** : tri/filtres, coffre de camp detaille, objets non identifies, ensembles 2/4/6, forge (reforge, amelioration, demontage), vente auto des communs.
- **Contenu** : meteo animee, rencontres supplementaires, boss uniques 10/25/50/100, modificateurs regenerant/voleur, bestiaire.
- **Technique** : sauvegarde versionnee + validation, tests Garmin MockWebServer (401/429), 30 tests unitaires, 3 tests UI Compose, lint, CI, workflow release signee conditionnel, widget d'ecran d'accueil, mise a jour in-app.
- **Mise en page paysage/tablette**, confidentialite et licences dans l'app.
- Garmin Personal API uniquement, 2FA navigateur, aucun Health Connect.

### Installation

```bash
adb install -r La-Marche-des-Ames-v0.4.0.apk
```
SHA-256 APK : A875334E5A126707FE4B6F11129E388665F937EA66F26D72E715C9FB83E7B054

L'AAB est fourni pour une publication Play Store eventuelle.

## v0.3.1 — 2026-11-16

**Titre :** v0.3.1 - Correctif overlays
**Assets :** `app-release.apk` (20.4 Mo, 1 téléchargement(s))

## La Marche des Ames - APK Android v0.3.1

Correctif : les ecrans plein ecran (campement, reglages, compte Garmin) bloquent desormais les taps vers l'interface derriere. Les dialogues ouverts sont fermes quand on ouvre un autre ecran.

Inclut toutes les nouveautes de la v0.3.0 : reglages, notifications on/off, ferveur qui decroit, equiper le meilleur, fusion, verrouillage, effets legendaires, coffre de camp, biomes, rencontres speciales, jalons et export/import.

### Installation

```bash
adb install -r La-Marche-des-Ames-v0.3.1.apk
```
SHA-256 APK : 6F5B0BC6D31C9360623CC83AD29DC36FE667FAAA4D1D45BDF3CBD3EC1FB66FEC

## v0.3.0 — 2026-11-16

**Titre :** v0.3.0 - Réglages, notifications et inventaire avancé
**Assets :** `app-release.apk` (20.4 Mo, 2 téléchargement(s))

## La Marche des Ames - APK Android v0.3.0

- **Reglages** : taille du texte (Normal / Grand / Tres grand), notifications on/off, animations reduites, vibrations, journal de synchronisation, export/import JSON, effacement de la progression.
- **Notifications** : cle Garmin a 30/7/1 jour, gardien, niveau, serments et coffre qui expire. Desactivable dans les reglages.
- **Ferveur** : decroissance apres 2 h sans pas et aura dans la scene.
- **Inventaire** : bouton **Equiper le meilleur**, vente en masse des communs, verrouillage, fusion 3 vers 1, effets legendaires (vampirisme, rempart, frenesie, chasse), coffre de camp quand l'inventaire est plein.
- **Campement** : veillee gratuite quotidienne et recuperation du coffre de camp.
- **Contenu** : biomes colores tous les 10 paliers, rencontres speciales (autel, source, marchand, statue), jalons, rang et modificateurs de gardien (rapide, blinde, berserk).
- **Sauvegarde** : export/import local, copie de secours automatique et workflow GitHub Actions.
- Corrections : porte du gardien masquee pendant les combats, coffre repositionne, journal et recapitulatif qui mettent la simulation en pause.
- Donnees de jeu et cle Garmin conservees. Garmin Personal API uniquement, 2FA via navigateur, aucun Health Connect.

### Installation

```bash
adb install -r La-Marche-des-Ames-v0.3.0.apk
```
SHA-256 APK : A539C93B08EA22D53D2F0925D2089EDD8AA6D401D62FE558492CAD724636172F

## v0.2.0 — 2026-11-16

**Titre :** v0.2.0 - Historique Garmin, serments et combats animés
**Assets :** `app-release.apk` (20.3 Mo, 1 téléchargement(s))

## La Marche des Ames - APK Android v0.2.0

- **Historique Garmin 7 jours** : chaque journ├®e est compt├®e une seule fois (ledger idempotent), plafond de rattrapage de 20 000 pas par synchronisation.
- **R├®capitulatif hors ligne** au retour : jours, pas et m├¿tres r├®cup├®r├®s.
- **Serments journaliers** : 3 000 pas, 1 gardien, 1 coffre ; r├®compenses automatiques en or, ├®clats et objet ; renouvel├®s ├á 00:00.
- **Combat anim├®** : d├®g├óts et soins flottants, flash de coup, estafilade, banni├¿re de niveau, aura et tag de phase 2, b├®n├®diction +25 %.
- **Journal complet** : 80 ├®v├®nements, filtres favorables / alertes / neutres.
- **Coffres** : expiration 24 h, 2 en attente maximum, compte ├á rebours et badge de file.
- Donn├®es de jeu et cl├® Garmin conserv├®es lors de la mise ├á jour.
- Garmin Personal API uniquement, 2FA via navigateur, jeton OAuth2 24 h, aucun Health Connect.

### Installation

```bash
adb install -r La-Marche-des-Ames-v0.2.0.apk
```
SHA-256 APK : 2319C69A94A888A2C11AF82E456FEC79E136A20D922570D07C6E9111BF0A1911

## v0.1.4 — 2026-11-16

**Titre :** v0.1.4 - Icône ronde et porte du gardien
**Assets :** `app-release.apk` (20.2 Mo, 1 téléchargement(s))

## La Marche des Ames - APK Android v0.1.4

- Ic├┤ne adaptative ronde plein cadre (fond sombre, anneau dor├®, h├®ros), fini le rond blanc avec un carr├® ├á l'int├®rieur.
- La porte du gardien n'appara├«t plus en permanence derri├¿re les monstres : elle n'est visible que dans les 15 derniers m├¿tres du palier ou pendant le combat du gardien.
- Texte agrandi (1,35x), tableau de bord lisible.
- Garmin Personal API uniquement, 2FA via navigateur, jeton OAuth2 24 h, aucun Health Connect.
- Reset quotidien ├á 00:00 local.

### Installation

```bash
adb install -r La-Marche-des-Ames-v0.1.4.apk
```
SHA-256 APK : 661D5A512DD3627320A4A8B40959C685A4DFDEBD3F465D33A06CB0ABC5FFDBEA

## v0.1.3 — 2026-11-16

**Titre :** v0.1.3 - Lisibilité et Garmin Personal API
**Assets :** `app-release.apk` (20.1 Mo, 1 téléchargement(s))

## La Marche des Ames - APK Android v0.1.3

- Texte agrandi (facteur 1,35) et tableau de bord plus haut pour la lisibilit├® sur Pixel 7a.
- Application native Kotlin/Compose, minSdk 34, APK release sign├®.
- Garmin Personal API uniquement : cl├® personnelle, 2FA dans le navigateur, jeton OAuth2 24 h, aucun Health Connect.
- IHM Compte Garmin : cl├®, date d'expiration, test, synchronisation, r├®vocation.
- Reset quotidien ├á 00:00 local, align├® sur Garmin.
- Sc├¿ne pixel art, combats automatiques, gardiens, coffre, camp, inventaire, am├®liorations.

### Installation

```bash
adb install -r La-Marche-des-Ames-v0.1.3.apk
```
Ou t├®l├®charger l'APK sur le t├®l├®phone et autoriser les applications inconnues.

### Garmin

Ouvrir l'app, Compte Garmin, ouvrir Garmin Connect, se connecter avec la 2FA, cr├®er une Personal API Key, la coller, saisir la date d'expiration, tester et synchroniser.

Ne jamais committer une cl├® Garmin. Si une cl├® a ├®t├® expos├®e, la r├®voquer et en cr├®er une nouvelle.

SHA-256 APK : 8FCDBA5EDC734449A7682A6176DB26AE97E3432BD99FDAE33EF1FBE7542780F9
