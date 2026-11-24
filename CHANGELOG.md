# Journal des versions

## v0.21.0
- **Lot 10 — écran de fin enrichi** : diorama avec la vraie statue du palier 100, l'aura dorée du héros et le gardien final déchu ; dossier du héros, progression, télémétrie, gardiens 81–100, économie, équipement porté, collections et jalons.
- **Export HTML** : rapport autonome (mêmes données, fonte pixel embarquée) enregistré via le sélecteur de fichiers Android.
- **Aperçu debug** : bouton **Aperçu de la fin** dans les réglages des builds debug, pour relire l'écran sans attendre le palier 100.
- **Tests** : **148 / 148**, lint OK.
- **Build** : `versionCode 58`, `versionName 0.21.0`. Aucune release GitHub.

## v0.20.10
- **Correctif VIT** : l'intervalle d'attaque respecte enfin le minimum de **180 ms** décidé au lot 2 (`Target.HERO_MIN_INTERVAL_MS`). Le code utilisait encore l'ancienne valeur de 300 ms, ce qui rendait la VIT inutile au-delà de ~2,4 et le plafond 4,0 inatteignable.
- **Portail** : `GATE_METERS` (90 m) pointe désormais sur l'unique constante de `Balance.Target`, au lieu d'être dupliqué dans `Model.kt`.
- **Simulateur** : intervalle minimum 180 ms, auto-ration 40 %/45 %, table des presets alignée sur le code, courbe `target` dérivée du même profil que `reference`, Trône brisé et boss final modélisés, sortie UTF-8 sous Windows.
- **Tests** : **145 / 145**, lint OK.
- **Build** : `versionCode 57`, `versionName 0.20.10`. Aucune release GitHub.

## v0.20.9
- **Lot 9 — Trône brisé et télémétrie** : à partir du **palier 81**, les ennemis gagnent de **+15 % à +35 % PV/ATQ** jusqu’au palier 100, et les gardiens du Trône sont renforcés (+10 % PV, +5 % ATQ).
- **Fin de jeu** : **L’Âme Première** devient un boss final au palier 100 (PV ×1,40, ATQ ×1,15) avec la capacité **RUPTURE DU TRÔNE** (230 % ATQ, −20 ferveur, soin 3 %). Une **fin** s’affiche une seule fois, puis le **mode sans fin** continue au-delà du palier 100.
- **Télémétrie locale** : victoires/combats normaux, victoires/tentatives de gardiens, essais et victoires par palier, durée moyenne, progression, ressources. Données locales, hors farm.
- **Tests** : **144 / 144**, lint OK.
- **Build** : `versionCode 56`, `versionName 0.20.9`. Aucune release GitHub.

## v0.20.8
- **Lot 8 — biomes et modificateurs** : budget de menace sur les gardiens. Les capacités lourdes (frappe, souffle, drain, pression) ne peuvent plus être combinées avec **RAPIDE** ou **BERSERK**, pour éviter les pics imprévisibles.
- **Gate** : capacité et modificateur restent affichés avant l'engagement.
- **Tests** : **138 / 138**, lint OK.
- **Build** : `versionCode 55`, `versionName 0.20.8`. Aucune release GitHub.

## v0.20.7
- **Lot 7 — butin, fusion, identification** : un objet **rare garanti toutes les 5 victoires** contre un monstre normal (hors farm).
- **Pity de set** : une pièce d'ensemble garantie tous les **10 gardiens** vaincus.
- **Identification** : au maximum **1 objet non identifié sur 10** drops rares ou mieux.
- **Fusion** : le résultat vaut désormais **90 %** de la somme des trois objets (au lieu de 75 %), en conservant effet et ensemble.
- **Tests** : **137 / 137**, lint OK.
- **Build** : `versionCode 54`, `versionName 0.20.7`. Aucune release GitHub.

## v0.20.6
- **Lot 6 — farm** : chaque victoire de farm rend **+10 % PV max** ; un badge **RÉCUPÉRATION** l'affiche dans la scène.
- **Farm de gardien** : **+1 éclat et +1 ration**, plafonnés à **5 gardiens farmés par jour** ; au-delà, le butin continue mais les ressources du jour sont épuisées.
- **Interface** : la carte Biome rappelle le soin de farm quand le mode est actif.
- **Tests** : **137 / 137**, lint OK.
- **Build** : `versionCode 53`, `versionName 0.20.6`. Aucune release GitHub.

## v0.20.5
- **Lot 5 — gardiens, pitié, portail** : niveau **palier + 1**, bouclier **35 % / 3 s / 8 s**, phase 2 télégraphiée sans accélération permanente, enrage après **25 s** (+15 %/5 s, cap +60 %).
- **Pitié** : **−5 % PV/ATQ du gardien par défaite**, cap **−25 %**, visible au portail et en combat.
- **Portail à 90 m** : arrêt avant le gardien, aperçu PV/ATQ/capacité/modificateur, choix **Engager / Farmer ce palier / Camper**.
- **Rotation des gardiens** confirmée par palier dans chaque biome ; gardiens spéciaux 25/50/100 conservés.
- **Codex HTML local** : `codex-complet.html`, ignoré par Git.
- **Tests** : **135 / 135**, lint OK.
- **Build** : `versionCode 52`, `versionName 0.20.5`. Aucune release GitHub.

## v0.20.4
- **Correctif gardiens** : rotation réelle dans chaque biome (`index = (palier − début du biome) % nombre de gardiens`). L'Hydre de Lerne ne revient plus à chaque palier ; les 4 gardiens de chaque biome alternent.
- **Lot 4 — rations et attrition** : plafond **30**, gardien +3, coffre +3, monstre 20 %, élite +1 garanti.
- **Auto-ration** : seuil **40 %**, soin **45 %**.
- **Farm** : **+10 % PV** par kill.
- **Repos gratuit** : **2 fois par jour**, **20 % PV** par repos.
- **Farm de gardien** : +1 éclat et +1 ration, sans avancer le palier.
- **Codex local** : généré dans `codex-complet.html`, ignoré par Git.
- **Tests** : **133 / 133**, lint OK.
- **Build** : `versionCode 51`, `versionName 0.20.4`. Aucune release GitHub.

## v0.20.3
- **Lot 3 — camp, sets et or** : camp en pourcentage (+1,5 % ATQ, +1,5 % PV, +1 % VIT par niveau), plafond **30 niveaux**, coût indexé sur le palier.
- **Sets en pourcentage** : 2/4/6 pièces donnent +4/8/12 % (ATQ/PV ou effet thématique), au lieu de valeurs plates.
- **Or** : le coût de camp augmente avec le palier ; le plafond de 30 niveaux évite l'accumulation sans usage.
- **Tests** : **131 / 131**, lint OK.
- **Build** : `versionCode 50`, `versionName 0.20.3`. Aucune release GitHub.

## v0.20.2
- **Lot 2 — objets et caps** : butin scalable par palier (`1 + 0,055 × (palier−1)`, cap ×6), raretés 1,15 / 1,5 / 1,9 / 2,4.
- **Amélioration additive** : +4 % de la statistique de base par niveau, plafond **+20 niveaux** au-dessus du drop ; les anciens objets restent compatibles et sont plafonnés à leur niveau actuel +20.
- **Caps** : vol de vie 25 %, réduction de dégâts 40 %, critique 75 %, dégâts critiques +100 %, dégâts gardiens +50 %, VIT 4,0.
- **VIT** : intervalle minimum 180 ms ; la fiche Stats affiche « MAX » quand la VIT est plafonnée.
- **Tests** : **128 / 128**, lint OK.
- **Build** : `versionCode 49`, `versionName 0.20.2`. Aucune release GitHub.

## v0.20.1
- **Lot 1 — courbe d'XP** : `xpNext = 120 × 1,09^(niveau−1)`, récompenses linéaires par palier (`1 + 0,12 × (palier−1)`), gardien ×10. Total 1 → 100 : **~6,8 M XP**, soit ~2 500–4 000 kills.
- **Rattrapage** : si le niveau est à plus de 4 paliers sous le palier courant, l'XP gagnée est majorée de 25 % jusqu'au retour à `palier − 4`.
- **Source de vérité** : `Balance.kt` + miroir `balance_constants.py` + vérification CI (`check_constants.py`).
- **Tests** : **125 / 125**, lint OK.
- **Build** : `versionCode 48`, `versionName 0.20.1`. Aucune release GitHub.

## v0.20.0
- **Courbe cible** : courbe des monstres et gardiens (combat ~18 s, ≥ 30 coups portés), interpolation entre l’ancienne courbe (≤ palier 20) et la cible (≥ palier 35), **ATQ recalibrée sur le build réel (~3 700 PV au palier 36)** : le build actuel gagne 59 % sans potion, 100 % avec.
- **Gardiens** : bouclier 40 % / 3 s, **enrage** après 15 s (+25 % ATQ toutes les 5 s, cap +75 %), bénédiction de défaite cumulative (+25 % par échec, cap +75 %).
- **Élites** : 6 % de chance, 1,8× PV, 1,15× ATQ.
- **Éclats** : plafond **24**, gardien +3, jalon +8, coffre +2, démontage 2/3/6/10, reforge **1** éclat, élite 20 % +1.
- **Rations** : gardien +2, coffre +2, monstre 10 % +1, auto-ration à 30 %, veillée gratuite 25 %.
- **Score d’objet** : PV pondérés ×0,6 pour que « Équiper le meilleur » ne privilégie plus un build glass cannon.
- **Monstres normaux** : ATQ cible −10 %, **sans soin automatique après la victoire** : l’attrition est conservée et les rations/potions restent utiles entre les combats.
- **Correctif** : une distance ou un prochain combat incohérents (sauvegarde/debug) ne bloquent plus le gardien ; la progression est normalisée au tick.
- **Tests** : **118 / 118**, lint OK.
- **Build** : `versionCode 47`, `versionName 0.20.0`.

## v0.19.1
- **Atelier** : vendre, démonter et reforger demandent désormais une **confirmation** (FR/EN). « Reforger encore » reste direct dans le panneau de résultat ; « Vendre les communs » est inchangé.
- **Tests** : **117 / 117**, lint OK.
- **Build** : `versionCode 46`, `versionName 0.19.1`.

## v0.19.0
- **Internationalisation FR/EN** : interface, journal, Codex, notifications, widget, dialogues de mise à jour et contenus dynamiques. En anglais, « Santé Connect » devient « Health Connect ».
- **Formats localisés** : séparateur de milliers (`7 464` / `7,464`), nombres compacts, dates (`jj/mm/aaaa` / `mm/dd/yyyy`), heures (24 h / 12 h), journal de synchronisation.
- **Manuel bilingue** : 36 captures anglaises, sélecteur FR/EN, génération Gradle des deux versions ; README unique FR puis EN.
- **Kit Play Store EN** : 5 captures, descriptions, mots-clés, notes de version et feature graphic.
- **Promo EN** : pitch et captures anglaises.
- **Widget** : héros avec l'équipement actuel, réserve de pas en gros chiffre, pas du jour, invite à synchroniser quand la réserve est vide. Il suit la langue de l'app et se rafraîchit en arrière-plan.
- **Correctifs** : ouverture des Réglages en anglais (format `%1$s`), textes du widget résolus par le lanceur, captures EN contaminées.
- **Sauvegarde** : v7 inchangée, aucune migration ajoutée.
- **Tests** : **117 / 117**, lint OK.
- **Build** : `versionCode 45`, `versionName 0.19.0`.

## v0.18.1
- **Correctif Codex** : les gardiens des biomes non découverts ne sont plus affichés comme découverts. La migration v6 effaçait les compteurs des monstres mais pas ceux des gardiens ; `bossKills`, `bossFirstSeen` et les entrées de bestiaire des gardiens sont maintenant nettoyés pour les biomes verrouillés.
- Les gardiens de jalon restent visibles uniquement à partir de leur palier fixe (Mère des Abysses au 25, L'Âme Première au 100).
- **Tests** : **115 / 115**.
- **Build** : `versionCode 44`, `versionName 0.18.1`.

## v0.18.0
- **Codex — 110 monstres uniques** : 13 variantes inter-biomes, 3 variantes internes et Manticore recolorée. Plus aucun nom n'est partagé entre plusieurs pools ni entre deux tiers d'un même biome. Le Codex affiche **110 créatures / 35 gardiens**.
- **Badge `NON DÉCOUVERT`** dans le Bestiaire pour les biomes non atteints, et **noms des biomes verrouillés** dans l'onglet Biomes, avec leur palier d'ouverture.
- **Chip `GARDIEN`** sur les entrées de gardiens, en complément du bandeau de combat et de la fiche.
- **Première rencontre** : biome, palier, tier et niveau enregistrés à la première victoire. Les fiches affichent les statistiques de cette rencontre ; Mère des Abysses et L'Âme Première ont un palier fixe (25 et 100).
- **Migration v6** : fusion des anciens noms suffixés (`Balrug · BLINDÉ`), copie des compteurs vers les variantes découvertes, suppression des compteurs des biomes non découverts, conservation de la première rencontre sur la variante d'origine. Les kills de gardien sont retirés du compteur de monstre (Option B). Sauvegarde `pre_migration_v5` avant migration.
- **Manuel** : compteurs du Codex et version de l'application injectés au build depuis `manuel/index.template.html`.
- **Assets** : 17 variantes recolorées, créditées dans `assets/CREDITS.md` ; `statue_hero.png` ajouté à la source `assets/env`.
- **Tests** : **113 / 113**, lint Android OK.
- **Build** : `versionCode 43`, `versionName 0.18.0`.

## v0.17.3
- **Atelier d'amélioration** : la fiche d'objet ouvre un atelier persistant. `AMÉLIORER` affiche le niveau suivant, le tableau `Équipé / Objet / Après`, le coût en or et éclats ; après validation, la feuille reste ouverte avec le gain appliqué, l'aperçu du niveau suivant et `CONTINUER À AMÉLIORER`.
- **Atelier de reforge** : `REFORGER` montre le pool complet (25 % aucun effet, 12,5 % par effet), applique le tirage pour 2 éclats, puis affiche `ancien → nouveau` avec `REFORGER ENCORE`. Le niveau, les statistiques et l'ensemble restent inchangés.
- **Tableau de statistiques sur la fiche d'objet** : `Statistique / Équipé / Cet objet` avec valeurs vertes ou rouges et gains, ou `Actuel / Après +1 niv.` pour un objet équipé avec la ligne `Héros` (ATQ et PV totaux). La valeur de vente reste sous le tableau.
- **Manuel** : nouvelle section « Atelier : comparer, améliorer, reforger », captures `item.jpg`, `atelier.jpg` et `codex_sets.jpg` actualisées.
- **Play Store** : cinquième capture `05-atelier.png`.
- **Build** : `versionCode 37`, `versionName 0.17.3`.

## v0.17.2
- **Collections d'ensembles permanentes** : une pièce découverte reste acquise même si elle est vendue, démontée ou fusionnée. Nouveau champ `setPiecesDiscovered` dans la sauvegarde (version 4), recalé automatiquement depuis l'équipement, le sac et le coffre au chargement ou à l'import.
- **Codex → Ensembles clarifié** : deux compteurs distincts, **Collection x/6** (permanente) et **Équipé y/6** (bonus 2/4/6). Le badge « BONUS ACTIF » ne s'affiche plus que si les pièces sont réellement équipées ; les slots manquants et le biome où farmer sont indiqués.
- **Fiche d'objet** : affiche « équipé y/6 · collection x/6 » ; le détail de l'ensemble reste masqué tant que l'objet n'est pas identifié, mais la pièce compte déjà dans la collection.
- **Carte Codex** : le compteur d'ensembles affiche désormais les collections **complètes** (6/6).
- **Manuel** : nouvelle section « Compléter une collection » avec les règles de drop, les deux compteurs et le tableau des onze ensembles (biomes, bonus 2/4/6).
- **Build** : `versionCode 36`, `versionName 0.17.2`.

## v0.17.1
- **Bouton de pause** : sous la synchronisation, il fige la marche, les combats, l'ouverture des coffres et les animations. Un second appui reprend exactement au même point.
- **Pause de session** : elle reste active en arrière-plan ; fermer complètement l'application la termine et la simulation d'absence reprend normalement.
- **Temps réel conservé** : la ferveur, les buffs temporisés, les coffres et le reset de minuit continuent de suivre l'horloge réelle.
- **Manuel** : nouvelle section « Pause de la marche » avec capture.
- 81 tests unitaires, lint Android OK.

## v0.17.0
- **Deux variantes de distribution** : `github` (APK avec mise à jour intégrée depuis GitHub) et `play` (AAB sans installateur d'APK ni vérification GitHub). Même nom de paquet et même clé release : la mise à jour croisée Play ↔ GitHub conserve les données.
- **Variante Play** : `REQUEST_INSTALL_PACKAGES` et le `FileProvider` d'installation sont retirés du manifeste ; la carte « Mises à jour », les dialogues et la vérification GitHub sont neutralisés (`BuildConfig.PLAY_DISTRIBUTION`).
- **Pack Play Store** (`playstore/`) : icône définitive composée avec les sprites du jeu (porte, Kaelen, Balrug, statue, lanterne), calques adaptive icon, 4 captures 1080×1920, feature graphic, titre, descriptions, lore, checklist et procédure de clé Play.
- **Manuel** : section « Durée de vie estimée » avec trois scénarios de marche (5 000, 8 000 et 10 000 pas par jour).
- **Build** : commandes `assembleGithubRelease` / `bundlePlayRelease` documentées.

## v0.16.0
- **Bandeau des effets actifs** : une seule ligne au-dessus du HUD bas, jusqu'à **6 effets** simultanés, avec la **durée restante en direct** (rafraîchie chaque seconde, même en pause). La largeur de chaque chip s'adapte au nombre d'effets, **plafonnée à un tiers**. Les bonus sont colorés, les malus apparaissent en rouge. Il remplace les anciennes chips discrètes à droite de la scène.
- **Fiche de créature dans le Codex** : appuyer sur un monstre ouvre une **popup dans le thème du jeu** avec son image, son biome, sa zone d'apparition, son niveau, ses PV, son ATQ, sa cadence, l'or et l'XP donnés, le nombre de victimes et la règle de biome appliquée. Les créatures non rencontrées restent verrouillées.
- **Fiche de gardien** : la même popup est disponible pour les gardiens, avec en plus leur capacité, ses effets réels et les modificateurs aléatoires possibles (RAPIDE, BLINDÉ, BERSERK, RÉGÉNÉRANT, VOLEUR).
- **Taille du texte** : les fiches du Codex utilisent la taille de texte choisie dans les réglages, comme les autres fenêtres de dialogue.
- 81 tests unitaires et lint Android OK.

## v0.15.0
- **Diagnostic de synchronisation** : nouvel écran dans **Réglages → Diagnostic**. Il montre la source active, la fenêtre d'import des 7 derniers jours avec l'état de chaque journée (crédité, aujourd'hui, avant connexion, aucun pas), les garde-fous appliqués (7 jours, 60 000 pas/jour, 100 000 pas par import, 250 pas/min) et un bouton de synchronisation manuelle. Lecture seule : aucun pas n'y est ajouté.
- **Journal de synchronisation horodaté** : les lignes portent désormais le jour **et l'heure** (`18/11 16:32 · OK · +12 pas`).
- **Santé Connect et arrière-plan** : le worker périodique ne tente plus la lecture Santé Connect. Android refuse la lecture en arrière-plan sans la permission `READ_HEALTH_DATA_IN_BACKGROUND`, ce qui remplissait le journal d'erreurs « lecture impossible » toutes les heures. La synchronisation Santé Connect se fait à l'ouverture et manuellement.
- **Cause réelle des échecs** : une lecture Santé Connect en échec conserve désormais l'exception (type et message), l'écrit dans le journal et dans logcat, au lieu d'un message générique.

## v0.14.0
- **Choix de la source de pas** : Garmin (Personal API) ou **Santé Connect**. Le choix se fait à l'onboarding et se change à tout moment dans **Réglages → Source de pas**, avec une carte par source, son état et son journal de synchronisation.
- **Santé Connect** : lecture locale des pas (`android.permission.health.READ_STEPS`), agrégation par origine (téléphone, montre, application de santé) en gardant le total le plus élevé par jour, sans double comptage.
- **Écran de justification Santé Connect** : Android 14+ exige une activité sur `VIEW_PERMISSION_USAGE` + `HEALTH_PERMISSIONS`. Sans elle, la demande d'autorisation était refermée aussitôt (« lecture des pas refusée »).
- **Changement de source sans double comptage** : le premier import de la nouvelle source sert de base, le total du jour ne recule jamais et aucun pas n'est recrédité.
- **Nouvelle fenêtre d'import** : la toute première synchronisation ne crédite que les **pas du jour**. Ensuite, le rattrapage lit **7 jours maximum**, borné par la **date de première connexion** (`historyStart`). Un oubli de synchro est donc rattrapé, sans démarrer le jeu avec une réserve héritée de la semaine passée.
- **Journal générique** : les messages de synchronisation suivent la source active (« Garmin synchronisé » ou « Santé Connect synchronisé »).
- **Démo web** : choix de la source de pas, fenêtre d'import (1re connexion = pas du jour, puis ≤ 7 jours) et simulation d'un oubli de synchronisation.
- **Build** : `compileSdk 36`, AGP 8.10.1, Health Connect 1.1.0 stable.

## v0.13.2
- **Sauvegarde atomique** : le fichier temporaire est écrit et synchronisé sur disque, puis déplacé sur `game_state.json` en un seul `rename`. L'ancien code supprimait le fichier principal avant de le remplacer : un arrêt du processus entre les deux pouvait faire repartir du `.bak` (ou d'une partie neuve).
- **Sauvegarde périodique pendant le jeu** : toutes les 30 s environ, et immédiatement à chaque changement de palier ou de niveau. Jusqu'ici rien n'était écrit entre deux événements : installer une mise à jour pouvait ramener le jeu avant un gardien déjà battu.
- **Sauvegarde avant l'installateur de mise à jour** : la progression est écrite sur disque juste avant de lancer l'installation de l'APK, qui tue le processus sans passer par `onPause`.

## v0.13.1
- **Réserve de pas visible** : une seconde jauge sous celle de la ferveur, avec la valeur (pleine à 4 000 pas).
- **Idle sur la réserve héritée** : la synchronisation Garmin est téléchargée **en parallèle** mais n'est **appliquée qu'à la fermeture du résumé**. La simulation d'absence travaille donc uniquement sur la réserve présente au moment de la fermeture, et plus jamais sur les pas qui viennent d'arriver.
- **Résumé systématique** dès 2 min d'absence, y compris réserve vide : le rapport explique alors que rien n'a avancé et annonce que les pas de la synchro arrivent ensuite.
- **Butin détaillé** dans le résumé : objets récupérés (nom, 8 max puis « … et N autres »), consommables, rations et éclats, en plus de l'or, des combats, paliers, gardiens, niveaux et coffres.
- **Message de synchro après fermeture** : « Garmin synchronisé · +N pas » (ou le rattrapage) n'apparaît qu'une fois le résumé refermé ; si aucun résumé n'est affiché, l'application est immédiate.

## v0.13.0
- **Consommation** : le décor animé se fige après 10 s sans pas (météo, aura de ferveur, balancements) et la simulation passe de 10 Hz à 2 Hz au repos. Mesuré sur appareil : **~60 fps → 2 fps**, CPU à l'écran **~60-70 % → ~30 %**, arrière-plan inchangé à 0 %.
- **Boucles supprimées** : l'horodatage d'activité n'écrit plus la sauvegarde toutes les 5 s (mémoire seule, toutes les 30 s) ; maintenance toutes les **2 h** (au lieu de 15 min), synchro de fond toutes les **60 min** avec backoff, widget toutes les 15 min.
- **Codex** : bandeau de couloir sous chaque biome découvert, dessiné par la **même fonction** que la scène (arches, torches, statues, props, sol).
- **Interface** : bouton « G » supprimé ; carte « Compte Garmin » déplacée des Stats vers les **réglages** ; devises réorganisées (or seul en haut, rations/éclats/réglages en bas) ; alerte « ! » affichée uniquement si Garmin n'est pas configuré ou si la clé approche de l'expiration.
- **Onboarding** : la page Garmin propose « CONNECTER GARMIN » et précise que sans clé la marche ne peut pas démarrer.
- **Manuel** : captures refaites (accueil, réglages, camp, objet, inventaire, codex) et nouvelle **légende numérotée** de l'écran principal.

## v0.12.1
- **Résumé d'absence rétabli** : la synchronisation Garmin est désormais effectuée **avant** la simulation d'absence. La réserve étant presque toujours vide à la fermeture (la marche l'a consommée), la simulation ne se déclenchait jamais et l'écran « Bon retour » n'apparaissait plus.
- La synchro de reprise est **silencieuse et attendue** : pas de toast avant le résumé, indicateur de synchronisation et `lastAdded` mis à jour, simulation lancée sur une réserve fraîche.
- Le jeu continue de tourner pendant l'appel réseau ; seule la simulation suspend la marche.
- Aucun changement de règle : 30 % des pas du jour, 4 minutes simulées, arrêt sur événement.

## v0.12.0
- **Progression hors ligne repensée** : la simulation ne consomme plus que **30 % des pas du jour** (et **4 minutes de jeu** au maximum), au lieu de 100 % du rythme jusqu'à 15 minutes. La réserve restante vous attend à l'ouverture.
- **Budget partagé** : la synchro de fond de 30 minutes puise dans le même quota ; une synchronisation manuelle ne simule plus rien, les pas importés restent à vivre à l'écran.
- **Arrêt sur événement** : la simulation s'interrompt sur une rencontre spéciale, devant le portail du gardien (s'il reste moins d'un palier de budget), sur un coffre ou une défaite, et laisse le reliquat.
- **Marche ralentie** : 140 → **60 pas/s** (30 sous 600 pas), pour que la consommation des pas soit visible.
- **Deux nouvelles notifications** : « Rencontre en attente » et « Gardien en attente », avec leurs cases dans les réglages ; elles ne se déclenchent que si l'application n'est pas à l'écran et respectent les heures calmes.
- **Forge corrigée** : améliorer, reforger, verrouiller et démonter fonctionnent désormais sur un **objet équipé** (le moteur ne cherchait que dans l'inventaire). Les messages disent ce qui manque (« Il manque 120 or et 1 éclat ») au lieu du générique « or ou éclat insuffisant ».
- **Aperçu d'amélioration** : le bandeau compare `ACTUEL → APRÈS +1 NIV.` avec des deltas colorés (vert/rouge) et chiffrés ; le bouton affiche le coût complet et se désactive quand il n'est pas payable.
- **Progression des objets** : chaque statistique non nulle gagne au moins +1 par niveau (8 % de 2 PV ou de 6 % crit. ne s'arrondissent plus à zéro).
- **Taille de texte « Maximal »** (2,0×), cumul system+app plafonné à 2,2×, et les fenêtres de dialogue suivent enfin le réglage. Croix de fermeture recentrée dans toutes les fenêtres, débordements corrigés en grand texte.
- **Campement** : titre « CAMPEMENT » (le feu veille en sous-titre), pastilles rations et éclats, et tente, feu et héros restent visibles au-dessus du panneau à toutes les tailles.
- **Portail du gardien** : porte plus haute que le gardien (132 × 116 dp), gardien à 96 dp, ovale décoratif supprimé au profit d'un halo.
- **Coffre** : croix et bouton « Masquer · le forçage continue », progression de la serrure sur la pastille du HUD, toast à l'ouverture.
- **Manuel et spécifications** mis à jour (règles d'absence, forçage de coffre).

## v0.11.0
- **Résumé d'absence** : à la réouverture, la réserve de pas s'écoule pendant la fermeture de l'application et un écran **« Bon retour »** récapitule l'absence.
- **Consommation** : 100 % du rythme actif, plafonnée à **60 000 pas** ou **15 minutes simulées**, au premier des deux atteint. La réserve restante est conservée.
- **Déclenchement** : absence d'au moins 2 minutes, réserve non vide, hors camp et hors rencontre spéciale.
- **Contenu** : durée, pas consommés, mètres, combats, or, paliers, gardiens, niveaux, coffres et réserve restante.
- **Rencontre spéciale** : la simulation s'arrête dessus et le résumé l'indique, sans résoudre la rencontre à votre place.
- **Défaite** : repli signalé dans le résumé, zone de farm retrouvée au retour.
- **Fusion Garmin** : si une synchronisation et une absence arrivent ensemble, un seul rapport combine les pas importés et la réserve.
- **Rapport persistant** : il reste disponible tant que « REPRENDRE LA MARCHE » n'a pas été utilisé ; quitter l'application ne le marque pas comme lu.
- **Anti-abus** : absence bornée à 7 jours, ferveur décrémentée selon le temps réel.
- **Manuel** : nouvelle section 19 avec capture de l'écran « Bon retour ».
- 74 tests unitaires (dont simulation d'absence, plafond et fusion), lint Android OK.

## v0.10.0
- **16 musiques d'ambiance 8 bits** : deux variantes de 3 minutes pour chacun des 8 biomes, générées pour le projet et publiées en **CC0**.
- **Lecture en boucle** avec démarrage à une position aléatoire à chaque ouverture, pour éviter la redondance.
- **Fondu court** au changement de biome.
- **Réglages musique** : toggle `Musique d'ambiance` et volume indépendant des effets sonores, **activée à 30 % par défaut**.
- **Cycle de vie** : pause automatique quand l'application passe en arrière-plan, reprise au retour.
- **Technique** : Media3 ExoPlayer, pistes OGG Vorbis mono 22 050 Hz, ~16 Mo ajoutés.
- **Générateur** : `tools/generate_music.py` permet de régénérer les 16 pistes.
- 70 tests unitaires et lint Android OK.

## v0.9.1
- **Icône de synchronisation Garmin redessinée** : deux flèches circulaires équilibrées au lieu du glyphe simple, plus lisible dans le petit bouton carré.

## v0.9.0
- **Codex** : nouvel écran de collection avec trois onglets — **Bestiaire** (créatures et gardiens découverts, silhouettes inconnues), **Ensembles** (pièces possédées par emplacement, bonus 2/4/6, prochain palier) et **Biomes** (règle, créatures, gardiens et objets découverts).
- **Suivi des collections** : les gardiens vaincus et les objets ramassés sont enregistrés. Les sauvegardes existantes récupèrent leurs gardiens depuis le bestiaire.
- **Mode Farm** : bouton `ACTIVER` / `DÉSACTIVER` dans la carte BIOME. Inactif par défaut. Le palier est verrouillé, le gardien revient, les jalons sont désactivés. Badge **FARM** dans le HUD.
- **Bouton de synchronisation compact** : petit carré en haut à gauche de la scène, aligné avec le SAC, avec icône ↻ et point d'état. La grosse barre du bas est supprimée.
- **Manuel** : captures du Codex, du mode farm et du nouveau bouton de synchro.
- 70 tests unitaires, dont le mode farm, la migration des gardiens et les helpers du Codex.

## v0.8.1
- **Correction du paper doll** : les heaumes rares/épiques utilisent désormais un vrai casque au lieu d'une couche de cheveux rouges. Les heaumes communs/inhabituels gardent le casque vert, les légendaires le heaume à cornes.
- **Couches d'armure distinctes** : cotte de mailles en acier, plate sombre, cuir, robe, carapace et armure de glace ont maintenant des sprites différents.
- **Casques et bottes différenciés** : heaume gravé, capuche elfique, casque vert, bottes de mousse, bottes des cimes et bottes de cuir/métal ne partagent plus la même couche.

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

