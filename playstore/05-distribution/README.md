# Distribution GitHub / Play

Le projet produit maintenant **deux variantes** avec le même nom de paquet `com.brunothesatellite.idlesteprpg` et **la même clé de signature release**.

## Les deux variantes

| Variante | Commande | Sortie | Mise à jour |
|---|---|---|---|
| **github** | `gradlew :app:assembleGithubRelease` | `app/build/outputs/apk/github/release/app-github-release.apk` | Intégrée : téléchargement de l'APK depuis les releases GitHub |
| **play** | `gradlew :app:bundlePlayRelease` | `app/build/outputs/bundle/playRelease/app-play-release.aab` | Gérée par Google Play uniquement |
| play (APK de contrôle) | `gradlew :app:assemblePlayRelease` | `app/build/outputs/apk/play/release/app-play-release.apk` | — |

## Différences techniques

| Élément | github | play |
|---|---|---|
| `REQUEST_INSTALL_PACKAGES` | oui | **non** |
| `FileProvider` (installation d'APK) | oui | **non** |
| Vérification des releases GitHub | oui | **non** (`BuildConfig.PLAY_DISTRIBUTION = true`) |
| Carte « Mises à jour » dans les réglages | oui | **non** |
| Dialogues de mise à jour | oui | **non** |
| Signature release | clé release | clé release |

La variante Play compile le même code : les fonctions de mise à jour existent mais sont neutralisées par `BuildConfig.PLAY_DISTRIBUTION`. Le manifeste Play ne déclare ni la permission d'installation ni le provider.

## Signature

Les deux variantes release utilisent `signingConfigs.release`, alimentée par `.keystore/` via `keystore.properties`.

- Certificat release : `CN=La Marche des Ames, OU=Mobile, O=brunothesatellite, C=FR`
- SHA-256 : `0F:1A:F2:6C:56:21:F0:AD:7A:AD:09:DB:BB:70:9C:52:99:E5:F0:88:52:2A:D7:BB:91:E2:C2:68:E1:67:8F:BE`

Vérification :

```
apksigner verify --print-certs app-github-release.apk
apksigner verify --print-certs app-play-release.apk
jarsigner -verify -verbose -certs app-play-release.aab
```

Les deux APK et l'AAB doivent afficher cette même empreinte.

## Mise à jour croisée Play ↔ GitHub

**Oui, sans perte de données**, parce que les deux canaux partagent le même paquet et la même clé :

1. L'utilisateur installe la version Play (signée avec la clé release, voir `procedure-cle-play.md`).
2. Il installe ensuite un APK GitHub plus récent : Android accepte la mise à jour (même certificat), les données restent.
3. Inversement, un utilisateur GitHub peut passer à la version Play sans désinstaller.

Conditions :

- **Même `applicationId`** (c'est le cas).
- **Même certificat de signature** (c'est le cas).
- **`versionCode` strictement supérieur** dans le nouveau build.
- Ne jamais signer un APK GitHub avec une autre clé (debug, autre keystore) : la mise à jour serait refusée.

## Une seule séquence de versions

`versionCode` et `versionName` sont dans `defaultConfig` : ils sont communs aux deux variantes. Il faut donc :

- construire l'APK GitHub et l'AAB Play **depuis le même commit** ;
- incrémenter `versionCode` à chaque publication, même si un seul canal est mis à jour ;
- ne jamais publier sur GitHub un `versionCode` inférieur à celui déjà publié sur Play.

## Vérifications effectuées

- GitHub : `REQUEST_INSTALL_PACKAGES` présent, signature release.
- Play : permission absente, `FileProvider` absent, signature release.
- AAB : signé avec la clé release (`CN=La Marche des Ames`).
- Les deux APK release ont la même empreinte SHA-256.

## Rappels Play

- La variante Play ne doit **jamais** installer d'APK ni renvoyer vers GitHub pour une mise à jour : c'est le rôle de Play.
- L'APK GitHub peut continuer à le faire.
- La fiche Play et la politique de confidentialité sont dans les dossiers voisins.
- La clé release doit être sauvegardée hors ligne : si elle est fournie à Google comme clé de signature d'application et que vous la perdez, Google ne peut pas la réinitialiser.
