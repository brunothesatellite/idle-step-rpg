# Crédits et licences des ressources

## Pixel art — Dungeon Crawl Stone Soup (CC0 1.0)

Les sprites de monstres, de héros, d'environnement et d'objets proviennent de l'export public des tuiles de **Dungeon Crawl Stone Soup** :

- Dépôt : <https://github.com/crawl/tiles>
- Export utilisé : `releases/Nov-2015`
- Licence : **CC0 1.0 Universal (domaine public)** — voir <https://creativecommons.org/publicdomain/zero/1.0/>
- Texte intégral : `licenses/CC0-1.0.txt`
- Le dépôt signale quelques tuiles dont la licence est incertaine ; elles ne sont pas utilisées ici. Voir `TILES_UNDER_UNKNOWN_LICENSE.md` dans le dépôt source.

### Fichiers utilisés

**Héros**
- `player/base/human_m.png` (base de composition, archive)
- `mon/human.png` (héros de la maquette)
- `player/hand1/broadsword.png`, `player/hand2/buckler_round.png` (arme et bouclier)
- `player/body/bplate_metal1.png`, `player/head/helm_green.png`, `player/head/helm_plume.png`, `player/head/horned.png`, `player/hand1/long_sword_slant.png`, `player/legs/pants_brown.png`, `player/legs/metal_gray.png`, `player/boots/middle_brown2.png`, `player/boots/middle_green.png`, `player/body/coat_red.png`, `player/body/aragorn.png` (bibliothèque de variantes)

**Monstres**
- `mon/animals/` : `bat`, `wolf`, `spider`, `scorpion`, `black_bear`, `worm`, `giant_frog`, `giant_cockroach`
- `mon/` : `goblin`, `kobold`, `gnoll`, `hobgoblin`, `orc`, `orc_warrior`, `ogre`, `deep_troll`, `naga`, `manticore`, `griffon`, `skeletal_warrior`, `vampire`
- `mon/undead/` : `ghoul`, `ghost`, `lich`
- `mon/demons/` : `crimson_imp`, `balrug`, `sun_demon`
- `mon/dragons/` : `hydra1`, `ice_dragon`, `storm_dragon`
- `mon/nonliving/` : `iron_golem`, `fire_elemental`
- `mon/unique/` : `sigmund`, `cerebov`, `lernaean_hydra01`, `iron_giant`, `jormungandr`

**Environnement**
- `dngn/wall/` : `brick_gray0-3`, `brick_brown0`, `catacombs0-3`, `slime0-2`
- `dngn/floor/` : `cobble_blood1-3`, `crypt10-11`, `dirt0-1`
- `dngn/statues/` : `statue_ancient_hero`, `statue_dragon`, `statue_sword`, `statue_archer`, `statue_angel`, `statue_wraith`, `crumbled_column_1`
- `dngn/doors/` : `gate_closed_left/middle/right`, `gate_runed_left/middle/right`, `gate_open_left/middle/right`, `closed_door`
- `dngn/altars/generic`, `dngn/trees/tree1_red`, `dngn/trees/tree2_red`

**Objets et consommables**
- `item/weapon/` : `long_sword3`, `greatsword1`, `hand_axe2`, `mace2`, `spear2`
- `item/armour/` : `plate1`, `leather_armour1`, `scale_mail1`, `headgear/helmet3`, `headgear/helmet_art1`, `headgear/elven_leather_helm`, `boots4_green`, `glove1`
- `item/ring/i-regeneration`, `item/amulet/i-regeneration` (renommés `ring-regen.png` et `amulet-regen.png`)
- `item/food/` : `meat_ration`, `bread_ration`
- `item/potion/` : `i-heal-wounds`, `i-might`, `i-haste`
- `item/gold/01`, `item/gold/04`
- `item/misc/` : `misc_crystal`, `misc_orb`, `misc_lantern`, `misc_box`
- `misc/icons/heart` (renommé `assets/fx/heart.png`)
- `effect/cloud_fire0-1`

> Le coffre de la maquette est un **SVG original** dessiné pour ce prototype (pas une tuile DCSS).

---

## Polices — Google Fonts (SIL Open Font License 1.1)

- **Pixelify Sans** — © The Pixelify Sans Project Authors.
  Source : <https://fonts.google.com/specimen/Pixelify+Sans>
- **Press Start 2P** — © CodeMan38.
  Source : <https://fonts.google.com/specimen/Press+Start+2P>
- Licence : **SIL Open Font License 1.1** — <https://scripts.sil.org/OFL>
- Textes complets et copyrights : `assets/fonts/licenses/OFL-PixelifySans.txt` et `assets/fonts/licenses/OFL-PressStart2P.txt`.
- Les fichiers `.woff2` sont inclus localement dans `assets/fonts/` pour un fonctionnement hors ligne.

---

## Extension v0.7.0 - biomes, monstres et objets

Les ressources ajoutées pour les biomes, les monstres et les objets viennent du **même export `releases/Nov-2015`** du dépôt <https://github.com/crawl/tiles>, sous **CC0 1.0**. Chaque nom a été vérifié contre `TILES_UNDER_UNKNOWN_LICENSE.md` : aucune tuile de licence incertaine n'a été utilisée.

**Monstres (70)**

- `mon/undead/` : `crawling_corpse`, `flying_skull`, `wight`, `bone_dragon`, `ancient_lich`, `revenant`, `drowned_soul`, `silent_spectre`, `vampire_knight`
- `mon/animals/` : `jumping_spider`, `boulder_beetle`, `hell_hound`, `fire_bat`, `ice_beast`, `polar_bear`, `raiju`, `killer_bee`, `queen_bee`
- `mon/` : `gnoll_sergeant`, `big_kobold`, `orc_knight`, `greater_naga`, `iron_troll`, `deep_dwarf`, `frost_giant`, `faun`, `harpy`, `satyr`, `dryad`
- `mon/demons/` : `efreet`, `brimstone_fiend`, `hell_sentinel`, `ice_devil`, `blizzard_demon`, `executioner`, `reaper`, `soul_eater`, `shadow_imp`, `iron_imp`, `ufetubus`, `hellion`
- `mon/nonliving/` : `iron_elemental`, `guardian_golem`, `crystal_guardian`, `ushabti`, `spellforged_servitor`
- `mon/dragons/` : `wyvern`, `shadow_dragon`, `quicksilver_dragon`
- `mon/fungi_plants/` : `treant`, `thorn_hunter`, `vine_stalker`, `wandering_mushroom`, `deathcap`
- `mon/amorphous/` : `ooze`, `jelly`, `azure_jelly`, `death_ooze`
- `mon/eyes/` : `giant_eyeball`, `eye_of_draining`, `great_orb_of_eyes`
- `mon/abyss/` : `ancient_zyme`, `tentacled_starspawn`, `lurking_horror`, `starcursed_mass`
- `mon/unique/` : `dispater`, `asmodeus`, `ereshkigal`, `gloorx_vloq`, `mnoleg`

**Objets (34)**

- `item/weapon/` : `short_sword1`, `falchion1`, `eveningstar1`, `rapier1`, `quarterstaff`, `bullwhip2`, `greatsword3`, `hand_axe1`
- `item/armour/` : `animal_skin1`, `ring_mail1`, `ice_dragon_armour`, `shadow_dragon_scale_mail`, `gold_dragon_armour`, `glove2`, `glove3`, `boots1_brown`, `boots2_jackboots`, `boots3_stripe`
- `item/armour/headgear/` : `helmet2`, `helmet4`, `helmet5`, `helmet_art2`
- `item/ring/` : `i-protection`, `i-slaying`, `i-r-poison`, `i-r-cold`
- `item/amulet/` : `i-warding`, `i-spirit`, `i-rage`
- `item/misc/` : `misc_horn`, `misc_orb2`
- `item/staff/` : `i-staff_earth`, `i-staff_cold`
- `item/food/` : `beef_jerky`

**Environnement**

- `dngn/wall/` : `volcanic_wall0-3`, `lab-metal0-3`, `zot_blue0-3`, `lair0-3`, `slime3-7`, `hell01-06`, `crystal_wall_white`, `crystal_wall_lightblue`, `metal_wall`, `catacombs4-7`
- `dngn/floor/` : `frozen0-5`, `ice0-3`, `grass0-2`, `grass/grass_flowers_blue1`, `grass/grass_flowers_red1`, `grass/grass_flowers_yellow1`, `acidic_floor0-3`, `bog_green0-3`, `black_cobalt01-04`, `demonic_red1-4`
- `dngn/trees/` : `tree1_yellow`, `tree2_yellow`, `tree1_lightred`, `tree2_lightred`, `mangrove1`
- `dngn/statues/` : `crumbled_column_2-4`, `statue_iron`, `statue_orb_guardian`, `statue_cerebov`, `statue_cat`, `statue_naga`, `statue_hydra`, `statue_demonic_bust`
- `dngn/` : `blood_fountain`, `blue_fountain`, `dry_fountain`, `sparkling_fountain`
- `dngn/doors/` : `gate_sealed_left/middle/right`, `runed_door`, `sealed_door`, `fleshy_orifice_closed`
- `mon/fungi_plants/` : `plant_crypt`, `bush2`, `bush3`, `bush4`
- `dngn/water/` : `liquefaction1`, `liquefaction2`, `ink_full`

---

## Extension v0.8.0 - paper doll

Pièces de héros supplémentaires, même export `releases/Nov-2015`, **CC0 1.0** :

- `player/hand1/` : `hand_axe2` (`weapon_axe`), `great_mace2` (`weapon_mace`), `great_staff` (`weapon_staff`), `dagger_slant` (`weapon_dagger`), `lance` (`weapon_spear`), `black_whip` (`weapon_whip`), `great_sword_slant` (`weapon_greatsword`)
- `player/body/` : `leather2` (`body_leather`), `green_chain` (`body_mail`), `plate_black` (`body_plate`), `coat_black` (`body_coat`), `robe_blue` (`body_robe`), `mesh_black` (`body_mesh_black`), `metal_blue` (`body_metal_blue`)
- `body_mail_gray` : dérivé CC0 de `green_chain`, recoloré en acier.
- `player/head/` : `hood_black2` (`head_hood`), `hood_green` (`head_hood_green`), `fhelm_gray3` (`head_helm_gray`), `crown_gold1` (`head_crown`)
- `player/legs/` : `leg_armour01` (`legs_plate`), `leg_armour03` (`legs_mail`), `pants_black` (`legs_cloth`)
- `player/boots/` : `middle_gray` (`boots_metal`), `middle_brown3` (`boots_leather`)

---

## Musiques d'ambiance (16 pistes, CC0 1.0)

Les pistes de `assets/music/` et `android/app/src/main/assets/music/` sont **générées pour ce projet** par `tools/generate_music.py` :

- 8 biomes × 2 variantes = 16 pistes ;
- 3 minutes par piste, OGG Vorbis mono 22 050 Hz ;
- boucle sans couture, démarrage à une position aléatoire ;
- aucun sample externe, licence **CC0 1.0**.

---

## Code et direction artistique

Le code de la maquette, la mise en scène, l'interface et le coffre SVG sont produits pour ce prototype de game design. Les ressources visuelles restent sous leurs licences respectives (CC0 / OFL).

*Dernière mise à jour : novembre 2026.*
