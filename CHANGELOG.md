# Journal des versions

## v0.8.0
- **Inventaire lisible** : barre de synchronisation compacte sur les onglets Inventaire/Améliorations, emplacements équipés cliquables et libellé d'ensemble sur les cartes de butin.
- **Paper doll fidèle** : l'apparence suit désormais la famille d'objet (épée, hache, masse, bâton, dague, lance, fouet ; cuir, maille, plate, robe ; capuche, heaume, couronne ; bottes de cuir ou de métal).
- **Règles de biome** : 8 modificateurs actifs pour le héros et les ennemis, affichés dans une carte BIOME de l'onglet Stats.
- **Gardiens mémorables** : capacités propres (drain, régénération, frappe lourde, blindage, souffle, racines, acide, pression) avec libellé sous la barre de vie et bandeau d'activation.
- **Manuel** : nouvelles captures du paper doll, de l'inventaire et d'un combat de gardien ; tableaux des règles de biome et des capacités.
- 63 tests unitaires, dont les règles de biome, les capacités de boss et les nouveaux assets.

## v0.7.0
- **8 biomes distincts** : Cryptes de Vael, Catacombes, Cavernes de cendre, Forge noyée, Cimes du Serment, Bois des Âmes, Abîme organique et Trône des Âmes.
- **Décors dédiés** : murs, sols, statues, props, portes runiques/scellées et météo propres à chaque biome (neige, cendres, spores, feuilles, étincelles, poussière).
- **Plus de 60 nouvelles créatures** réparties par biome et par distance dans le palier, avec des boss de biome et les gardiens de jalon.
- **Tables de butin par biome** : armes, armures, casques, bottes, gantelets et talismans thématiques.
- **8 nouveaux ensembles** : Ossature ancienne, Toile des catacombes, Cendre éternelle, Forge abyssale, Serment des cimes, Bois des âmes, Abîme organique et Trône des âmes.
- **2 nouveaux effets d'objet** : Vitalité et Précision.
- **4 nouveaux consommables** : Potion de hâte, Potion de force, Potion de régénération et Viande séchée.
- **Héros en paper doll** : l'équipement visible change le casque, l'armure, les bottes et l'arme affichés.
- **Retour visuel d'impact adouci** : halo radial autour de l'ennemi au lieu d'un grand carré translucide sur les fonds sombres.
- **Assets CC0 DCSS** vérifiés contre la liste des licences incertaines ; crédits mis à jour.
- 60 tests unitaires, dont les nouveaux tests de biomes, pools, butin, ensembles et présence des assets.

## v0.6.2
- **Mises à jour depuis GitHub** : vérification automatique au lancement (une fois par 24 h, active par défaut) et vérification manuelle dans `Réglages → Mises à jour`.
- **Installation in-app** : téléchargement de l'APK de la release, vérification **SHA-256** quand l'empreinte est publiée, puis installation confirmée via Android (FileProvider).
- **Après installation** : l'application propose d'ouvrir la page GitHub de la release pour lire les changements.
- Option pour désactiver la vérification automatique ; les versions ignorées ne réapparaissent pas seules.
- 12 tests unitaires ajoutés pour la comparaison de versions, le parsing des releases et le SHA-256.

## v0.6.1
- **Manuel utilisateur complet** en HTML (`manuel/index.html`) et intégré à l'application via `Réglages → Aide`.
- 11 captures d'écran et explication de toutes les mécaniques.
- **Plafond de rattrapage Garmin porté à 100 000 pas** par synchronisation (marathon/ultra couverts).
- **Inventaire plein** : le meilleur objet reste dans le sac, le moins bon part au coffre ; objets verrouillés protégés.
- Correction des notes de release GitHub (blocs de code mal échappés).

## v0.6.0
- **Sons 8 bits intégrés** : combat, critique, dégâts, niveau, gardien, coffre, consommable, victoire, défaite, forge.
- Sons originaux générés pour le projet, licence **CC0**.
- Réglage **Sons on/off**, **volume** et bouton **Tester le son**.
- Porte du gardien : libellé **« EN ATTENTE DE PAS »** quand il manque des pas avant les 100 m.
- Backlog : anglais, Room, Play Store, équilibrage, farm 0 pas et batterie faible restent explicitement non planifiés.

## v0.5.3
- **Catégorie Consommables** dans l'inventaire et le sac de scène.
- 12 consommables avec icônes dédiées : soins, ferveur, bombes, buffs temporaires.
- Butin : monstres, gardiens et coffres.
- Utilisation au camp, en exploration et en combat, avec buffs et compte à rebours.
- Carte **RESSOURCES** dans Stats : rations, éclats, or, consommables.
- Camp : héros, tente et feu repositionnés pour rester visibles au-dessus du panneau.

## v0.4.2
- Fenêtre **RÉSULTAT** après chaque rencontre spéciale : pari gagné/perdu, achat, offrande, soin.
- Plus de toasts empilés : un seul toast à la fois, remplacé par le suivant.
- Achat d'améliorations sans toast répétitif : la carte et l'or se mettent à jour directement.

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

