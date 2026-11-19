# Procédure : utiliser votre clé release comme clé de signature Play

Objectif : que Google Play signe l'application avec **votre** clé release. Ainsi, l'APK Play et l'APK GitHub ont le même certificat, et les utilisateurs peuvent passer d'un canal à l'autre sans perdre leurs données.

> À faire **avant** le premier déploiement en test ouvert ou en production. Après, Play ne permet plus de changer la clé d'application (hors rotation).

## Prérequis

- Compte Play Console en règle (un compte limité bloque la publication).
- Application créée dans Play Console, **aucune release en test ouvert/production**.
- Clé release actuelle : `.keystore/` + `keystore.properties` (non versionnés).
- Sauvegarde hors ligne de la clé et des mots de passe (deux copies minimum).
- Java disponible (`keytool`, `java`).

## 1. Sauvegarder la clé

Copier `.keystore/` et `keystore.properties` sur un support hors ligne. Sans cette clé, ni GitHub ni Play ne pourront signer de mise à jour compatible.

## 2. Déclarer votre clé dans Play Console

1. Ouvrir Play Console → sélectionner **DungeonStep**.
2. **Protected with Play** → **Play Store protection** → **Manage Play app signing**.
3. Cliquer sur **Change key**.
4. Choisir **Provide a copy of your app signing key** (et non « Use the same key as another app »).
5. La console affiche :
   - une **clé de chiffrement** (`encryption-key`) ;
   - un **IV** (`encryption-iv`) ;
   - le lien de téléchargement de **PEPK**.

## 3. Chiffrer et envoyer la clé

Depuis le dossier contenant `pepk.jar` :

```
java -jar pepk.jar ^
  --keystore="C:\chemin\vers\.keystore\<storeFile>" ^
  --alias=<keyAlias> ^
  --output="play-app-signing-key.zip" ^
  --include-signing-key ^
  --encryption-key=<encryption-key> ^
  --encryption-iv=<encryption-iv>
```

Remplacer :

- `<storeFile>` et `<keyAlias>` par les valeurs de `keystore.properties` ;
- `<encryption-key>` et `<encryption-iv>` par celles affichées dans la console.

PEPK demande le mot de passe du keystore : le saisir à l'invite, ne pas l'écrire dans la commande ni dans un fichier versionné.

Puis uploader `play-app-signing-key.zip` dans la console et valider.

## 4. Vérifier l'empreinte

Dans Play Console → **Protected with Play** → **Play Store protection** → **Manage Play app signing**, la section **App signing key certificate** doit afficher :

```
SHA-256 : 0F:1A:F2:6C:56:21:F0:AD:7A:AD:09:DB:BB:70:9C:52:99:E5:F0:88:52:2A:D7:BB:91:E2:C2:68:E1:67:8F:BE
```

C'est l'empreinte de votre clé release. Si elle diffère, la clé fournie n'est pas la bonne : recommencer avant tout déploiement.

## 5. Choisir la clé d'upload

### Option A — simple : la même clé

Le projet signe déjà les AAB avec la clé release. Play vérifie l'upload avec cette même clé, puis signe avec elle. Rien à changer dans Gradle.

- Avantage : une seule clé à gérer.
- Inconvénient : la clé d'upload et la clé d'application sont identiques (moins bonne pratique de sécurité).

### Option B — recommandée : une clé d'upload séparée

Créer une clé dédiée aux envois Play :

```
keytool -genkeypair -v ^
  -keystore upload-keystore.jks ^
  -alias upload ^
  -keyalg RSA -keysize 2048 -validity 10000
```

Déclarer son certificat dans Play Console (section **Upload key certificate**), puis configurer Gradle pour que seule la variante Play signe l'AAB avec cette clé :

```kotlin
signingConfigs {
    create("upload") {
        storeFile = rootProject.file("../.keystore/upload-keystore.jks")
        storePassword = keystoreProperties.getProperty("uploadStorePassword")
        keyAlias = keystoreProperties.getProperty("uploadKeyAlias")
        keyPassword = keystoreProperties.getProperty("uploadKeyPassword")
    }
}

androidComponents {
    onVariants(selector().withFlavor("distribution" to "play")) { variant ->
        variant.signingConfig = signingConfigs.getByName("upload")
    }
}
```

Garder la clé release pour :
- l'APK GitHub ;
- la clé d'application Play (celle fournie à Google).

Ne pas activer cette option sans avoir créé `upload-keystore.jks` : le build Play échouerait.

## 6. Construire et envoyer

```
gradlew :app:bundlePlayRelease
```

Fichier : `app/build/outputs/bundle/playRelease/app-play-release.aab`.

Uploader l'AAB dans Play Console (test interne d'abord). Play le re-signe avec votre clé release.

## 7. Valider la mise à jour croisée

1. Installer la version Play (test interne) sur un appareil.
2. Vérifier l'empreinte de l'APK installé :
   ```
   adb shell pm path com.brunothesatellite.idlesteprpg
   adb pull <chemin>/base.apk
   apksigner verify --print-certs base.apk
   ```
   Elle doit être `0F:1A:F2:…:8F:BE`.
3. Construire un APK GitHub avec un `versionCode` supérieur.
4. L'installer par-dessus : Android doit accepter la mise à jour, sans désinstallation, avec les données conservées.
5. Faire l'inverse (GitHub → Play) pour confirmer.

## Avertissements

- **Avant le premier test ouvert/production** : après, le changement de clé n'est plus proposé.
- **Google conserve une copie** de la clé d'application. Si vous la perdez, Google ne peut pas la réinitialiser.
- **Signature quantique** : les nouvelles apps Play utilisent par défaut une signature hybride générée par Google. En fournissant votre clé classique, vous renoncez probablement à cet avantage ; ce n'est pas bloquant.
- **Compte Play limité** : régler l'appel avant de commencer.
- **`versionCode`** : une seule séquence pour GitHub et Play.
- **Ne jamais** publier un APK GitHub signé avec une autre clé que celle fournie à Play : la mise à jour croisée serait refusée.
