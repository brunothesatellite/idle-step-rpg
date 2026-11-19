# La Marche des Âmes — Android

Version Android native du prototype. **v0.17.1** — APK sideloadé pour Pixel 7a (Android 14+).

**Manuel complet :** [`../manuel/index.html`](../manuel/index.html) · accessible dans l'app via `Réglages → Aide → Ouvrir le manuel du jeu`.

## Ce qui est implémenté

- Scène pixel art en **Jetpack Compose** : héros, monstres, élites, gardiens, coffre, porte runique, campement.
- Boucle de jeu : **10 pas = 1 m**, rencontres tous les 10 m, gardien tous les 100 m, combat automatique, défaite + repli de 20 m + farm, ferveur, rations, éclats, coffres à quota.
- **Résumé d'absence** : reprise après fermeture, consommation de la réserve à 100 % du rythme actif, plafond 60 000 pas / 15 min simulées, overlay « Bon retour », arrêt sur rencontre spéciale et fusion avec la synchro Garmin.
- Tableau de bord : **Stats**, **Inventaire** (comparaison/équipement/vente/tri/filtres), **Améliorations**, journal, prochain gardien.
- **Compte Garmin** : clé personnelle, date d'expiration, test, synchronisation, révocation.
- **Garmin Personal API uniquement** : aucun Health Connect, aucune autre source de pas.
- **2FA Garmin dans le navigateur** (Custom Tab) : l'application ne voit jamais le mot de passe ni le code 2FA.
- Clé et jeton **chiffrés dans Android Keystore** ; écran de clé protégé contre les captures (`FLAG_SECURE`).
- Jeton OAuth2 Bearer **24 h**, sans refresh token : renouvellement automatique avec la clé.
- Reset quotidien à **00:00 local**, aligné sur `calendarDate` Garmin.
- **Historique Garmin 7 jours** : ledger par jour, rattrapage idempotent, plafond de 100 000 pas par synchronisation, contrôle 250 pas/min, **simulation hors ligne** (combats, or, paliers, victimes).
- **Serments journaliers variables**, série de jours, pas hebdomadaires, récompenses en or/éclats/objet.
- **Combat animé** : dégâts/soins flottants, critiques distincts, flash, bannière de niveau, phase 2, bénédiction +25 %, modificateurs de gardien (rapide, blindé, berserk, régénérant, voleur).
- **Journal complet** : 80 événements, filtres, **journal de légende** et **bestiaire**.
- **Coffres** : expiration 24 h, 2 en attente, compte à rebours, alerte d'expiration, bouton HUD.
- **Réglages** : taille du texte, notifications par catégorie, heures calmes, animations réduites, vibrations, journal de synchronisation, export/import avec aperçu, effacement.
- **Onboarding** en quatre écrans, revoyable.
- **Ferveur** : décroissance après 2 h, rattrapage hors ligne, aura.
- **Inventaire avancé** : « Équiper le meilleur », tri/filtres, « Vendre les communs », verrouillage, fusion 3→1, objets non identifiés, ensembles 2/4/6, forge (reforge/amélioration/démontage), effets légendaires, coffre de camp détaillé. Sac plein : le meilleur objet reste dans le sac, le moins bon part au coffre. Les emplacements équipés sont cliquables et les cartes de butin indiquent l'ensemble.
- **Consommables** : 16 objets de soin, ferveur, bombes et buffs temporaires, butin des monstres/coffres, sac en scène, au camp et en combat.
- **Ressources visibles** : rations, éclats, or et consommables dans une carte Stats dédiée.
- **Sons 8 bits** : effets originaux CC0, volume réglable, désactivation et test dans les réglages.
- **Musiques d'ambiance** : 16 pistes originales CC0, deux variantes de 3 minutes par biome, démarrage aléatoire, boucle, fondu au changement de biome, volume indépendant.
- **Campement** : combat réinitialisé, veillée gratuite +15 %, soins, coffre, journal et inventaire accessibles.
- **Contenu** : 8 biomes avec murs, sols, statues, props, météo et portes dédiés ; plus de 60 nouvelles créatures ; 36 nouveaux objets de butin ; 8 nouveaux ensembles ; 2 nouveaux effets ; paper doll fidèle aux familles d'objets ; règles de biome ; capacités de gardien ; Codex de collection ; mode Farm par palier ; rencontres spéciales, jalons complets, boss uniques 10/25/50/100, bestiaire.
- **Sauvegarde** : JSON versionné, copie de secours, export/import contrôlé, widget d'écran d'accueil.
- **Mises à jour GitHub** : vérification automatique quotidienne (désactivable) et manuelle, téléchargement de l'APK signé, vérification SHA-256, installation confirmée par l'utilisateur via FileProvider, puis proposition d'ouvrir la release pour lire les changements.
- **Qualité** : tests moteur, tests Garmin MockWebServer, tests UI Compose, lint, CI Android, workflow de release signée conditionnel.
- WorkManager : synchro toutes les 30 minutes, maintenance coffres/clé toutes les 15 minutes.

## Prérequis de build

- **JDK 21** (Temurin recommandé).
- **Android SDK 35** et build-tools 35.0.0.
- Le fichier de signature local `../.keystore/keystore.properties` (ignoré par Git). Il contient :

```properties
storeFile=idle-step-rpg-release.jks
storePassword=...
keyAlias=idle-step-rpg
keyPassword=...
```

## Build

```bash
cd android
./gradlew :app:assembleRelease
```

ou, sous Windows :

```powershell
.\gradlew.bat :app:assembleRelease
```

APK généré :

```
app/build/outputs/apk/release/app-release.apk
```

## Installation

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

Ou télécharger l'APK depuis les **GitHub Releases** et l'installer sur le téléphone (autoriser les applications inconnues).

## Mises à jour

L'application interroge `https://api.github.com/repos/brunothesatellite/idle-step-rpg/releases/latest` :

- vérification automatique au plus une fois par 24 h, désactivable dans les réglages ;
- vérification manuelle dans `Réglages → Mises à jour` ;
- téléchargement de l'APK de la release, vérification SHA-256 si l'empreinte est dans les notes ;
- installation via `FileProvider` après confirmation Android (`REQUEST_INSTALL_PACKAGES`) ;
- après redémarrage, proposition d'ouvrir la page GitHub de la release installée.

`UpdateChecker` garde la logique de version et de parsing testable ; le téléchargement et l'installation passent par le `GameViewModel`.

Play Protect peut avertir ou bloquer une application inconnue de Google lors de l'installation in-app : l'APK reste installable manuellement depuis la page GitHub de la release, et son SHA-256 est publié dans les notes.

## Première ouverture Garmin

1. Ouvrir l'application → **Compte Garmin** (bouton `G` ou « Connecter Garmin »).
2. « Ouvrir Garmin Connect » : se connecter avec la **2FA**.
3. Créer une **Personal API Key**, la copier.
4. La coller dans l'application, saisir la date d'expiration (Garmin ne l'expose pas via l'API), puis « Tester et enregistrer ».
5. Synchroniser. Le jeu convertit les pas du jour en distance.

> Ne jamais committer la clé Garmin. Si elle a été exposée, la révoquer sur Garmin Connect et en créer une nouvelle.

## Debug vs release

- **Release** : bouton de connexion Garmin uniquement, pas d'outil de test.
- **Debug** : un bouton « Ajouter 500 pas (démo) » est présent dans l'écran Garmin pour tester le moteur sans clé. Il est exclu du release (`BuildConfig.DEBUG`).

## Limites de cette v0.17.1

- L'historique Garmin est limité à 7 jours et à 100 000 pas de rattrapage par synchronisation.
- La base reste un fichier JSON versionné avec copie de secours, pas encore Room.
- L'interface est en français uniquement.
- La publication Play Store est préparée mais pas effectuée ; l'APK reste sideloadé.
- L'équilibrage est celui de la démonstration et sera à affiner sur plusieurs semaines.
- La synchronisation dépend de Garmin Connect : la montre doit avoir poussé ses pas.
- La mise à jour in-app dépend de l'autorisation Android « Installer des applications inconnues » pour La Marche des Âmes.

## Documents

- Spécifications : [docs/specifications-fonctionnelles.md](../docs/specifications-fonctionnelles.md)
- Architecture et Garmin : [docs/architecture-android-garmin.md](../docs/architecture-android-garmin.md)
- Licences : [LICENSES.md](../LICENSES.md)








