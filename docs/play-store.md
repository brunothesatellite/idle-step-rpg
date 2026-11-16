# Publication Play Store — notes

L'application est actuellement distribuée en APK sideloadé. Ce document prépare une
publication éventuelle sur Google Play.

## Prérequis externes

- Compte Google Play Console (frais uniques).
- Identité développeur validée.
- Politique de confidentialité publiée (voir `docs/privacy.md`).
- Signature Play App Signing.

## Build Android App Bundle

```bash
cd android
./gradlew :app:bundleRelease
```

Sortie :

```
app/build/outputs/bundle/release/app-release.aab
```

## Signature

Le fichier `../.keystore/keystore.properties` doit contenir :

```properties
storeFile=idle-step-rpg-release.jks
storePassword=...
keyAlias=idle-step-rpg
keyPassword=...
```

Le keystore ne doit jamais être commité. Sauvegardez-le hors du dépôt.

## Fiche store

- Nom : La Marche des Âmes
- Catégorie : Jeu / RPG
- Public : 13+
- Description courte : vos pas Garmin deviennent un RPG de marche.
- Mots-clés : marche, RPG, Garmin, pixel art, idle.
- Captures : scène, stats, inventaire, réglages, journal.
- Icône : `ic_launcher` adaptative.

## Déclarations sensibles

- **Santé** : l'app lit uniquement les pas via la Garmin Personal API. Pas de Health
  Connect, pas de dossier médical, pas de données de santé revendues.
- **Compte** : pas de compte propriétaire. La clé Garmin reste locale.
- **Notifications** : alertes locales de jeu.
- **Sécurité des données** : aucune collecte, aucun serveur éditeur.

## Checklist avant envoi

- [ ] Politique de confidentialité accessible par URL.
- [ ] Version `versionCode` incrémentée.
- [ ] Tests unitaires verts.
- [ ] `lintDebug` sans erreur bloquante.
- [ ] APK/AAB signé et vérifié.
- [ ] Captures récentes.
- [ ] Déclaration « Sécurité des données » remplie.
- [ ] Test interne sur Pixel 7a.
- [ ] Notes de version.

## Alternative sans Play Store

Conserver la distribution GitHub Releases avec APK signé et SHA-256, qui fonctionne
déjà et ne nécessite aucun compte externe.
