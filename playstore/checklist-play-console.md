# Checklist Play Console — DungeonStep

Cocher au fur et à mesure. Les points marqués ⚠️ demandent une décision ou un changement dans l'app.

## 1. Avant de créer la fiche

- [x] Compte Play Console en règle (ou compte Android Developer Console pour la vérification hors Play).
- [x] Vérification d'identité du développeur terminée.
- [ ] Nom de paquet : `com.brunothesatellite.idlesteprpg` — il ne changera plus après publication.
- [ ] `versionCode` incrémenté et `versionName` à jour (`android/app/build.gradle.kts`).
- [ ] **AAB signé** avec la clé d'upload (ou la clé release si vous gardez une seule clé). La clé release doit être fournie à Play comme **clé de signature d'application** : voir `05-distribution/procedure-cle-play.md`, à faire **avant** le premier test ouvert/production.
- [ ] Variante **play** construite avec `gradlew :app:bundlePlayRelease` : pas de `REQUEST_INSTALL_PACKAGES`, pas d'updater GitHub (`BuildConfig.PLAY_DISTRIBUTION = true`).
- [ ] `targetSdk` conforme à l'exigence Play en vigueur (35 actuellement ; vérifier au moment de l'envoi).
- [ ] 64-bit : l'app est en Kotlin/Compose, aucune bibliothèque 32-bit bloquante.
- [ ] Politique de confidentialité accessible : <https://github.com/brunothesatellite/idle-step-rpg/blob/main/docs/privacy.md>.
- [ ] ⚠️ **Mise à jour in-app** : l'app télécharge des APK GitHub et lance l'installateur (`REQUEST_INSTALL_PACKAGES`). Play interdit les mises à jour hors Play pour les apps distribuées sur Play. Désactiver ce mécanisme dans la variante Play (ou le remplacer par le lien Play) avant l'envoi, sinon risque de rejet.
- [ ] ⚠️ **Mode démo pour la revue** : sans source de pas, l'écran de jeu reste vide. Prévoir des instructions de test (connecter Santé Connect, ou un état de démo) dans « Accès à l'application ».

## 2. Fiche Play Store

### Français

- [ ] Nom : `DungeonStep — Marche des Âmes` (29 caractères).
- [ ] Description courte : `Vos pas ouvrent les donjons. Libérez les âmes maudites, un palier à la fois.` (76 caractères) — `03-textes/description-courte.txt`.
- [ ] Description complète : `03-textes/description-complete.txt`.
- [ ] Notes de version : `03-textes/notes-version.txt`.
- [ ] Feature graphic : `04-feature-graphic/feature-graphic-1024x500.png`.
- [ ] Captures téléphone : les 5 fichiers de `02-captures/` (2 à 8 exigées). À rafraîchir : elles datent d'une version antérieure.

### English

- [ ] Name: `DungeonStep - March of Souls` (28 characters) — `03-textes/en/title.txt`.
- [ ] Short description: `Your steps open the dungeons. Free cursed souls, one floor at a time.` (69 characters) — `03-textes/en/short-description.txt`.
- [ ] Full description: `03-textes/en/full-description.txt` (uses **Health Connect**, not "Santé Connect").
- [ ] Release notes: `03-textes/en/release-notes.txt`.
- [ ] Feature graphic: `04-feature-graphic/en/feature-graphic-1024x500.png`.
- [ ] Phone screenshots: the 5 files in `02-captures/en/` (2 to 8 required).

### Commun

- [ ] Icône : `01-icone/icon-512.png`.
- [ ] Catégorie : Jeux → Jeu de rôle.
- [ ] Tags : RPG, idle, pixel art, podomètre / step counter.
- [ ] Coordonnées développeur et e-mail de support.
- [ ] Site web (facultatif) : dépôt GitHub ou page du manuel.

## 3. Formulaires obligatoires

- [ ] **Publicités** : non.
- [ ] **Achats intégrés** : non.
- [ ] **Classification du contenu (IARC)** : violence fantasy, créatures, sang stylisé. Répondre honnêtement ; ne pas minimiser.
- [ ] **Public cible** : 13+ ou 16+ selon la classification ; pas destiné aux enfants.
- [ ] **Sécurité des données** :
  - Pas de compte de jeu, pas de publicité, pas de tracker.
  - Pas collecté : nom, e-mail, contacts, localisation.
  - Traité localement : pas et données de santé via Santé Connect ; clé Garmin chiffrée dans Android Keystore.
  - Réseau : requêtes HTTPS vers Garmin (Personal API) et GitHub (mises à jour) uniquement.
  - ⚠️ Déclarer la lecture des pas comme donnée de santé, même si elle reste sur l'appareil ; vérifier les cases exactes du formulaire.
- [ ] **Applications de santé** : la lecture via Health Connect peut déclencher la politique « Health apps ». Fournir la justification d'usage et l'écran de permission.
- [ ] **Accès à l'application** : décrire comment un testeur voit le jeu (autorisation Santé Connect ou clé Garmin de test). Ne pas laisser « toutes les fonctionnalités accessibles » si ce n'est pas vrai.
- [ ] **Actualités / COVID / gouvernement / finance** : non.

## 4. Tests et déploiement

- [ ] Compte personnel créé après le 13/11/2023 : **test fermé avec 12 testeurs pendant 14 jours consécutifs** avant la production.
- [ ] Piste de test interne pour la première validation (AAB).
- [ ] Vérifier la fiche sur un appareil vierge : installation, autorisation Santé Connect, première synchronisation (pas du jour uniquement).
- [ ] Vérifier la fiche **dans les deux langues** : la mention « Santé Connect » doit devenir « Health Connect » en anglais.
- [ ] Vérifier la mise à jour depuis la version précédente : données conservées, même signature.
- [ ] Demander la production après les 14 jours, répondre aux questions de préparation.

## 5. Après publication

- [ ] Conserver la clé release et `keystore.properties` hors du dépôt.
- [ ] Activer Play App Signing (recommandé) et archiver le certificat d'inscription.
- [ ] Surveiller les rapports de plantage et les avis.
- [ ] Mettre à jour le manuel et la fiche à chaque version.
- [ ] Enregistrer le nom de paquet dans l'Android Developer Console (vérification développeur) si distribution hors Play en parallèle.

## 6. Points de vigilance

- ⚠️ L'installateur intégré (APK GitHub) est le principal risque de rejet Play. Le désactiver pour la variante Play.
- ⚠️ Les captures ne doivent pas montrer de contenu absent de l'app (elles sont réelles, donc conformes).
- ⚠️ Ne pas promettre de récompense réelle ni de gain d'argent : ce n'est pas le cas dans les textes fournis.
- ⚠️ La politique de santé peut demander une déclaration supplémentaire ; prévoir un délai.
