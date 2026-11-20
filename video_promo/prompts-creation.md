Agis comme un réalisateur et développeur Python spécialisé en automatisation vidéo.
Dans le dossier `video_promo/`, j'ai un fichier `audio.mp3` (~2m30) qui lit la section "## Histoire" du fichier `README.md`.

Ta mission pour cette étape 1 :
1. Vérifie les dépendances Python disponibles ou installe `faster-whisper` (ou `openai-whisper`) dans un environnement virtuel `video_promo/.venv`.
2. Écris et exécute un script `video_promo/01_transcribe.py` qui transcrit `video_promo/audio.mp3` en français avec les timestamps par mot/segment (le texte est celui de lore.md - version longue)), et génère `video_promo/timeline.json`.
3. Associe chaque segment de `timeline.json` à l'une des 8 scènes suivantes du jeu (`com.brunothesatellite.idlesteprpg.debug`) pour couvrir l'intégralité de la durée exacte de `audio.mp3` :
   - `01_intro_marche.mp4` : La marche de Kaelen dans les Cryptes de Vael (écran principal).
   - `02_equipement.mp4` : L'écran d'inventaire et le paper doll du personnage, l'atelier d'amélioration.
   - `03_combat_gardien.mp4` : Un combat automatique contre un monstre ou un gardien de palier.
   - `04_biomes_codex.mp4` : Le Codex montrant les biomes et le bestiaire (ou défilement des paliers).
   - `05_camp_braise.mp4` : L'écran du Campement avec la braise au centre et les soins.
   - `06_coffre_pas.mp4` : L'ouverture d'un coffre ou le détail de la synchronisation des pas.
   - `07_defaite_detour.mp4` : La gestion de la ferveur, des consommables ou de l'atelier pour reforger.
   - `08_outro.mp4` : Retour sur la marche infinie de Kaelen.

Sauvegarde ce plan de montage détaillé (avec durées exactes nécessaires pour chaque clip) dans `video_promo/storyboard.json`.

-----------------

Maintenant, passe à l'étape 2 : la capture vidéo 100 % authentique depuis mon appareil Android connecté via ADB.

Écris un script Python interactif `video_promo/02_capture_adb.py` qui lit `video_promo/storyboard.json` et enregistre chaque clip dans `video_promo/clips/` :
1. Vérifie qu'un appareil est connecté via `adb devices`.
2. Lance l'application debug (`adb shell monkey -p com.brunothesatellite.idlesteprpg.debug -c android.intent.category.LAUNCHER 1`).
3. Pour chaque scène listée dans `storyboard.json` :
   - Affiche dans le terminal le nom du clip, la scène demandée (ex: "Ouvre l'écran Campement") et la durée requise (ajoute +3 secondes de marge de sécurité par rapport au storyboard).
   - injecte les données nécessaires pour que l'application réagisse conformément au script
   - effectue toi même les opérations sur l'application pour réaliser le script tel que défini
   - Lance `adb shell screenrecord --bit-rate 8000000 --time-limit <DUREE> /sdcard/<nom_clip>.mp4`.
   - À la fin de l'enregistrement, rapatrie automatiquement le fichier dans `video_promo/clips/<nom_clip>.mp4` via `adb pull` et supprime le fichier temporaire sur le téléphone (`adb shell rm`).
4. Permets de relancer uniquement un clip spécifique si je passe son nom en argument (ex: `python 02_capture_adb.py 03_combat_gardien.mp4`).

Exécute ensuite ce script pour que tu puisses enregistrer les séquences.
Les clips sont enregistrés dans `video_promo/clips/`. 

-----------------


Passe à l'étape 3 : le montage final automatisé avec FFmpeg et Python.

Écris et exécute `video_promo/03_render_video.py` pour produire `video_promo/la_marche_des_ames_vertical.mp4` en respectant strictement ces règles :
1. **Format & Pixel Art :**
   - Résolution de sortie : `1080x1920` (9:16), 30 fps, codec H.264 (`libx264`, `-crf 18`, `-pix_fmt yuv420p`).
   - Redimensionne les captures ADB pour remplir le cadre vertical tout en forçant l'algorithme `flags=neighbor` dans FFmpeg afin de préserver la netteté absolue du pixel art (aucun flou bilinéaire).
2. **Montage & Rythme :**
   - Assemble les clips de `video_promo/clips/` selon les durées exactes définies dans `video_promo/storyboard.json` (utilise des coupes franches ou de courts fondus au noir de 0.2s entre les scènes).
   - Si un clip est légèrement plus court que prévu, boucle-le proprement ; s'il est plus long, coupe-le à la durée exacte du segment audio.
3. **Sous-titres dynamiques intégrés :**
   - Génère un fichier `.ass` (Advanced SubStation Alpha) à partir de `video_promo/timeline.json` et du texte exact de `README.md` (pour corriger d'éventuelles fautes d'orthographe de Whisper).
   - Utilise la police pixel art présente dans le dépôt (`Press Start 2P` ou `Pixelify Sans`, cherche le fichier `.ttf` dans `assets/` ou `android/app/src/main/res/font/`).
   - Place les sous-titres centrés horizontalement, à `y=1420` (au-dessus de l'interface TikTok/YouTube Shorts), texte blanc avec contour noir épais (4px) et ombre portée, max 2 lignes par carton.
4. **Mixage Audio :**
   - Piste principale : `video_promo/audio.mp3` à 100 % de volume (normalisé à -14 LUFS).
   - Piste secondaire : choisis une piste musicale d'ambiance dans `assets/music/`, boucle-la sur toute la durée de la vidéo, baisse son volume à `-22dB` et applique un fondu entrant/sortant de 2 secondes.
5. **Écran de fin (3 dernières secondes) :**
   - Sur les 3 dernières secondes de la vidéo (ou juste après la dernière phrase), superpose le titre « LA MARCHE DES ÂMES », la mention « RPG de marche Android · Garmin & Santé Connect » et « github.com/brunothesatellite/idle-step-rpg » avec la police pixel art du jeu.
6. N'utilise aucune image ou asset généré par IA : uniquement les captures ADB et les fichiers déjà présents dans le dépôt.

Lance le rendu et affiche la durée finale et le poids du fichier généré.