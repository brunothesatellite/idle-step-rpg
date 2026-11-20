"Génère également une seconde version video_promo/la_marche_des_ames_github.mp4 redimensionnée en 540x960 (avec flags=neighbor), -crf 25, audio AAC 96k, en vérifiant que le fichier final fait strictement moins de 9.5 Mo pour être uploadé dans un README GitHub."

Ne pas committer le fichier dans Git : Évitez de faire un git push du fichier .mp4, cela alourdit le dépôt et n'active pas le lecteur vidéo.
Uploader via l'interface web de GitHub : Ouvrez une New Issue sur votre dépôt (sans la publier) ou éditez directement votre README.md sur le site GitHub, puis glissez-déposez votre .mp4 dans la zone de texte.
Copier le lien généré : Récupérez l'URL créée par GitHub (https://github.com...).
Coller dans le README.md : Placez cette URL seule sur une ligne vide (sans crochets ni parenthèses autour) :
https://github.com

GitHub la transformera automatiquement en lecteur vidéo intégré avec son et plein écran.