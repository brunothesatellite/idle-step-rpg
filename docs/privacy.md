# Politique de confidentialité — La Marche des Âmes

Dernière mise à jour : 16 novembre 2026

## En résumé

L'application **ne collecte aucune donnée personnelle** et **n'envoie rien à un serveur
de l'éditeur**. Elle n'utilise ni compte propriétaire, ni publicité, ni mesure d'audience.

## Données traitées

| Donnée | Stockage | Finalité |
|---|---|---|
| Clé personnelle Garmin | Chiffrée sur l'appareil (Android Keystore) | Interroger la Garmin Personal API |
| Jeton OAuth2 Garmin (24 h) | Chiffré sur l'appareil (Android Keystore) | Authentifier les requêtes Garmin |
| Date d'expiration de la clé | Stockage local de l'application | Rappels de renouvellement |
| Pas quotidiens et historique 7 jours | Stockage local de l'application | Progression du jeu |
| Sauvegarde de jeu | Stockage local de l'application | Export/import volontaire |
| Préférences (texte, notifications, etc.) | Stockage local de l'application | Confort d'utilisation |

## Ce que l'application ne fait pas

- Aucun Health Connect.
- Aucun serveur, backend ou base distante de l'éditeur.
- Aucune saisie du mot de passe Garmin ni du code 2FA dans l'application : la connexion
  se fait dans le navigateur, chez Garmin.
- Aucune publicité, aucun traceur, aucun profil publicitaire.
- Aucune revente ou transmission de données à un tiers.

## Requêtes réseau

L'application contacte uniquement :

- `personal.connectapi.garmin.com` pour obtenir un jeton et lire les pas ;
- `api.github.com` et `github.com` si vous appuyez sur « Vérifier les mises à jour »
  ou sur les liens de licences/confidentialité.

## Permissions Android

- `INTERNET` : synchronisation Garmin et vérification manuelle des mises à jour.
- `POST_NOTIFICATIONS` : alertes locales (gardien, niveau, serments, coffre, clé).
  Désactivables dans les réglages de l'application et d'Android.

## Conservation et suppression

- Les données restent sur votre appareil.
- « Révoquer la clé » supprime la clé et le jeton Garmin.
- « Effacer la progression » remet le jeu à zéro sans toucher à la clé.
- Désinstaller l'application supprime toutes les données locales.
- Les fichiers exportés sont sous votre contrôle.

## Enfants

L'application n'est pas destinée aux enfants de moins de 13 ans et ne collecte
aucune donnée les concernant.

## Modifications

Toute modification de cette politique sera publiée dans ce fichier et signalée
dans le journal des versions.

## Contact

Ouvrir une issue sur le dépôt :
https://github.com/brunothesatellite/idle-step-rpg/issues
