# Journal des versions

## v0.4.0 — à venir

### Robustesse
- Le camp réinitialise le combat en cours (PV et timers de l'ennemi).
- Veillée gratuite alignée sur la spécification : +15 % PV.
- Décroissance de la ferveur rattrapée hors ligne.
- Worker de maintenance toutes les 15 minutes : expiration des coffres et rappels de clé.
- Notifications par catégorie, heures calmes 22:00–08:00, liens directs vers l'écran concerné.
- Aperçu et validation avant import d'une sauvegarde.
- Contrôle des pas anormaux (250 pas/min en moyenne).
- Backoff Garmin sur erreur 429 avec délai `Retry-After`.

### Boucle de jeu
- Simulation hors ligne accélérée : combats, or, paliers et victimes dans le récapitulatif.
- Onboarding en quatre écrans, revoyable depuis les réglages.
- Jalons complets : or, éclats, titres, journal de légende.
- Compteurs de vie : pas totaux, victimes, coffres, or gagné, série de jours, semaine.
- Facteur de profondeur après le palier 10.
- Campement enrichi : pas en attente, or, journal, inventaire.
- Bandeau de nouveau jour.

### Inventaire et forge
- Équiper le meilleur, tri, filtres, coffre de camp détaillé.
- Objets non identifiés, ensembles avec bonus 2/4/6 pièces.
- Forge : reforger un affixe, améliorer un objet, démonter en éclats.
- Vente automatique des communs (option).
- Effets légendaires et visuels de rareté.

### Contenu
- Biomes colorés avec particules météo animées.
- Rencontres spéciales supplémentaires, dont un pari risqué.
- Nouveaux monstres et gardiens, boss uniques aux paliers 10/25/50/100.
- Modificateurs de gardien : rapide, blindé, berserk, régénérant, voleur.
- Critique distinct, journal de légende et bestiaire.
- Exploits et série de jours.

### Technique
- Sauvegarde versionnée (version 2), validation et copie de secours.
- Tests Garmin avec MockWebServer (401, 429, réponses vides, JSON invalide).
- Tests moteur supplémentaires (camp, ferveur, simulation, forge, ensembles, série).
- CI Android avec tests, lint et APK debug ; workflow de release signée conditionnel.
- Mise à jour in-app via GitHub Releases.
- Mise en page adaptative paysage/tablette.
- Politique de confidentialité et licences accessibles depuis les réglages.

## v0.3.1
- Correctif : les overlays plein écran ne laissent plus passer les taps.
- Fermeture des dialogues empilés à l'ouverture d'un autre écran.

## v0.3.0
- Réglages, notifications on/off, animations réduites, vibrations, journal de sync,
  export/import.
- Ferveur avec décroissance et aura.
- Équiper le meilleur, vente des communs, verrouillage, fusion, effets légendaires,
  coffre de camp.
- Biomes colorés, rencontres spéciales, jalons, rang, modificateurs de gardien.
- CI Android et sauvegarde de secours.

## v0.2.0
- Historique Garmin 7 jours, récapitulatif hors ligne, serments, combat animé,
  journal complet, coffres 24 h / 2 en attente.

## v0.1.4
- Icône adaptative ronde, porte du gardien limitée aux 15 derniers mètres.

## v0.1.3
- Lisibilité : texte 1,35×, tableau de bord plus haut.

## v0.1.0
- Première version Android native avec Garmin Personal API.
