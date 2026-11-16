# La Marche des Âmes — Android

Version Android native du prototype. **v0.1.4** — APK sideloadé pour Pixel 7a (Android 14+).

## Ce qui est implémenté

- Scène pixel art en **Jetpack Compose** : héros, monstres, élites, gardiens, coffre, porte runique, campement.
- Boucle de jeu : **10 pas = 1 m**, rencontres tous les 10 m, gardien tous les 100 m, combat automatique, défaite + repli de 20 m + farm, ferveur, rations, éclats, coffres à quota.
- Tableau de bord : **Stats**, **Inventaire** (comparaison/équipement/vente), **Améliorations** (coûts croissants), journal, prochain gardien.
- **Compte Garmin** : clé personnelle, date d'expiration, test, synchronisation, révocation.
- **Garmin Personal API uniquement** : aucun Health Connect, aucune autre source de pas.
- **2FA Garmin dans le navigateur** (Custom Tab) : l'application ne voit jamais le mot de passe ni le code 2FA.
- Clé et jeton **chiffrés dans Android Keystore** ; écran de clé protégé contre les captures (`FLAG_SECURE`).
- Jeton OAuth2 Bearer **24 h**, sans refresh token : renouvellement automatique avec la clé.
- Reset quotidien à **00:00 local**, aligné sur `calendarDate` Garmin.
- WorkManager toutes les 30 minutes + synchro à l'ouverture + bouton manuel.

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

## Limites de cette v0.1.4

- MVP jouable, pas encore de serments journaliers, de récapitulatif hors ligne détaillé ni de notifications de gardien.
- L'API Garmin est interrogée pour la journée courante ; l'historique multi-jours n'est pas encore importé.
- L'équilibrage est celui de la démonstration.
- La synchronisation dépend de Garmin Connect : la montre doit avoir poussé ses pas.

## Documents

- Spécifications : [docs/specifications-fonctionnelles.md](../docs/specifications-fonctionnelles.md)
- Architecture et Garmin : [docs/architecture-android-garmin.md](../docs/architecture-android-garmin.md)
- Licences : [LICENSES.md](../LICENSES.md)
