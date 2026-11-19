# Politique de confidentialité — DungeonStep

Dernière mise à jour : 19 novembre 2026

Page publique : <https://brunothesatellite.github.io/idle-step-rpg/privacy/>

## En résumé

L'application **n'envoie aucune donnée personnelle à un serveur de l'éditeur**. Les pas sont lus
localement (Garmin ou Santé Connect), la progression reste sur l'appareil, et il n'y a ni compte
de jeu, ni publicité, ni traceur.

## Données traitées

| Donnée | Stockage / transmission | Finalité |
|---|---|---|
| Clé personnelle Garmin | Chiffrée sur l'appareil (Android Keystore) ; envoyée uniquement à Garmin | Interroger la Garmin Personal API |
| Jeton OAuth2 Garmin (24 h) | Chiffré sur l'appareil ; envoyé uniquement à Garmin | Authentifier les requêtes Garmin |
| Date d'expiration de la clé | Stockage local | Rappels de renouvellement |
| Pas quotidiens et historique 7 jours | Stockage local ; jamais envoyé à l'éditeur | Progression du jeu |
| Sauvegarde de jeu | Stockage local ; export/import volontaire | Sauvegarde et changement d'appareil |
| Préférences (texte, notifications, son) | Stockage local | Confort d'utilisation |

## Santé Connect / Health Connect

Si vous choisissez **Santé Connect** comme source de pas, l'application lit vos pas avec la permission
Android `android.permission.health.READ_STEPS`.

- La lecture se fait **localement, sur votre téléphone**.
- Les données de pas ne sont **pas transmises à l'éditeur** ni à un serveur.
- Elles servent uniquement à faire avancer la partie (distance, combats, paliers).
- L'application **n'écrit rien** dans Santé Connect.
- Vous pouvez révoquer la permission à tout moment dans Android ou dans Santé Connect ; le jeu cesse
  alors de lire vos pas.

## Ce que l'application ne fait pas

- Aucun serveur, backend ou base distante de l'éditeur.
- Aucun compte de jeu, aucune inscription.
- Aucune saisie du mot de passe Garmin ni du code 2FA : la connexion se fait dans le navigateur, chez Garmin.
- Aucune publicité, aucun traceur, aucun profilage.
- Aucune revente ni transmission de données à un tiers.
- Aucune donnée de localisation (pas de GPS).

## Requêtes réseau

L'application contacte uniquement :

- `personal.connectapi.garmin.com` : jeton et lecture des pas, si Garmin est la source choisie ;
- `api.github.com` et `github.com` : vérification des mises à jour et liens de licences, uniquement
  dans la version distribuée sur GitHub. La version Google Play ne vérifie pas GitHub.

## Permissions Android

- `INTERNET` : synchronisation Garmin et, selon la version, vérification des mises à jour.
- `POST_NOTIFICATIONS` : alertes locales (gardien, niveau, serments, coffre, clé), désactivables
  dans l'application et dans Android.
- `android.permission.health.READ_STEPS` : lecture des pas via Santé Connect, si cette source est choisie.
- `REQUEST_INSTALL_PACKAGES` : présente uniquement dans la version GitHub, pour installer les mises
  à jour téléchargées depuis les releases. Absente de la version Google Play.

## Conservation et suppression

- Les données restent sur votre appareil.
- « Révoquer la clé » supprime la clé et le jeton Garmin.
- « Effacer la progression » remet le jeu à zéro sans toucher à la clé.
- Désinstaller l'application supprime toutes les données locales.
- Les fichiers exportés restent sous votre contrôle.

## Enfants

L'application n'est pas destinée aux enfants de moins de 13 ans et ne collecte aucune donnée les concernant.

## Modifications

Cette politique peut évoluer. La date de mise à jour figure en haut de cette page ; les changements
importants sont signalés dans le journal des versions du dépôt.

## Contact

Ouvrir une issue sur le dépôt :
<https://github.com/brunothesatellite/idle-step-rpg/issues>
