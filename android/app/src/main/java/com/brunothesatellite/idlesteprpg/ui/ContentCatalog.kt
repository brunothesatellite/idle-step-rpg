package com.brunothesatellite.idlesteprpg.ui

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.brunothesatellite.idlesteprpg.R

/* Catalogue de contenu généré depuis les modèles du moteur.
 * Les noms restent figés en français dans les sauvegardes ; ces tables
 * fournissent l'affichage traduit sans migration. */

fun creatureName(res: Resources, value: String): String = when (value) {
    "Chauve-souris" -> res.getString(R.string.content_creature_chauve_souris)
    "Goule" -> res.getString(R.string.content_creature_goule)
    "Cadavre rampant" -> res.getString(R.string.content_creature_cadavre_rampant)
    "Crâne volant" -> res.getString(R.string.content_creature_crane_volant)
    "Ver des tombes" -> res.getString(R.string.content_creature_ver_des_tombes)
    "Guerrier squelette" -> res.getString(R.string.content_creature_guerrier_squelette)
    "Spectre" -> res.getString(R.string.content_creature_spectre)
    "Revenant" -> res.getString(R.string.content_creature_revenant)
    "Spectre silencieux" -> res.getString(R.string.content_creature_spectre_silencieux)
    "Wight" -> res.getString(R.string.content_creature_wight)
    "Âme noyée" -> res.getString(R.string.content_creature_ame_noyee)
    "Vampire" -> res.getString(R.string.content_creature_vampire)
    "Chevalier vampire" -> res.getString(R.string.content_creature_chevalier_vampire)
    "Sigmund le reclus" -> res.getString(R.string.content_creature_sigmund_le_reclus)
    "Dragon d'os" -> res.getString(R.string.content_creature_dragon_dos)
    "Kobold" -> res.getString(R.string.content_creature_kobold)
    "Gobelin" -> res.getString(R.string.content_creature_gobelin)
    "Araignée" -> res.getString(R.string.content_creature_araignee)
    "Araignée sauteuse" -> res.getString(R.string.content_creature_araignee_sauteuse)
    "Cafard géant" -> res.getString(R.string.content_creature_cafard_geant)
    "Grenouille géante" -> res.getString(R.string.content_creature_grenouille_geante)
    "Chauve-souris des galeries" -> res.getString(R.string.content_creature_chauve_souris_des_galeries)
    "Gnoll" -> res.getString(R.string.content_creature_gnoll)
    "Sergent gnoll" -> res.getString(R.string.content_creature_sergent_gnoll)
    "Hobgobelin" -> res.getString(R.string.content_creature_hobgobelin)
    "Orc" -> res.getString(R.string.content_creature_orc)
    "Naga" -> res.getString(R.string.content_creature_naga)
    "Coléoptère blindé" -> res.getString(R.string.content_creature_coleoptere_blinde)
    "Guerrier orc" -> res.getString(R.string.content_creature_guerrier_orc)
    "Chevalier orc" -> res.getString(R.string.content_creature_chevalier_orc)
    "Grand kobold" -> res.getString(R.string.content_creature_grand_kobold)
    "Ogre" -> res.getString(R.string.content_creature_ogre)
    "Troll des abîmes" -> res.getString(R.string.content_creature_troll_des_abimes)
    "Diablotin" -> res.getString(R.string.content_creature_diablotin)
    "Limier des enfers" -> res.getString(R.string.content_creature_limier_des_enfers)
    "Chauve-souris de feu" -> res.getString(R.string.content_creature_chauve_souris_de_feu)
    "Scorpion" -> res.getString(R.string.content_creature_scorpion)
    "Élémentaire de feu" -> res.getString(R.string.content_creature_elementaire_de_feu)
    "Éfrit" -> res.getString(R.string.content_creature_efrit)
    "Manticore de cendre" -> res.getString(R.string.content_creature_manticore_de_cendre)
    "Légionnaire infernal" -> res.getString(R.string.content_creature_legionnaire_infernal)
    "Balrug" -> res.getString(R.string.content_creature_balrug)
    "Démon solaire" -> res.getString(R.string.content_creature_demon_solaire)
    "Flagellant de soufre" -> res.getString(R.string.content_creature_flagellant_de_soufre)
    "Sentinelle des enfers" -> res.getString(R.string.content_creature_sentinelle_des_enfers)
    "Élémentaire de fer" -> res.getString(R.string.content_creature_elementaire_de_fer)
    "Oushabti" -> res.getString(R.string.content_creature_oushabti)
    "Nain des profondeurs" -> res.getString(R.string.content_creature_nain_des_profondeurs)
    "Kobold mineur" -> res.getString(R.string.content_creature_kobold_mineur)
    "Golem d'atelier" -> res.getString(R.string.content_creature_golem_datelier)
    "Golem gardien" -> res.getString(R.string.content_creature_golem_gardien)
    "Troll des scories" -> res.getString(R.string.content_creature_troll_des_scories)
    "Troll de fonte" -> res.getString(R.string.content_creature_troll_de_fonte)
    "Gardien de cristal" -> res.getString(R.string.content_creature_gardien_de_cristal)
    "Serviteur runique" -> res.getString(R.string.content_creature_serviteur_runique)
    "Troll de fer" -> res.getString(R.string.content_creature_troll_de_fer)
    "Golem de fer" -> res.getString(R.string.content_creature_golem_de_fer)
    "Loup des neiges" -> res.getString(R.string.content_creature_loup_des_neiges)
    "Bête de glace" -> res.getString(R.string.content_creature_bete_de_glace)
    "Raiju" -> res.getString(R.string.content_creature_raiju)
    "Chauve-souris des neiges" -> res.getString(R.string.content_creature_chauve_souris_des_neiges)
    "Ours polaire" -> res.getString(R.string.content_creature_ours_polaire)
    "Griffon" -> res.getString(R.string.content_creature_griffon)
    "Diable de glace" -> res.getString(R.string.content_creature_diable_de_glace)
    "Harpie" -> res.getString(R.string.content_creature_harpie)
    "Wyverne" -> res.getString(R.string.content_creature_wyverne)
    "Géant de givre" -> res.getString(R.string.content_creature_geant_de_givre)
    "Démon blizzard" -> res.getString(R.string.content_creature_demon_blizzard)
    "Dragon d'orage" -> res.getString(R.string.content_creature_dragon_dorage)
    "Dragon d'ombre" -> res.getString(R.string.content_creature_dragon_dombre)
    "Dragon de vif-argent" -> res.getString(R.string.content_creature_dragon_de_vif_argent)
    "Faune" -> res.getString(R.string.content_creature_faune)
    "Abeille tueuse" -> res.getString(R.string.content_creature_abeille_tueuse)
    "Grenouille des sous-bois" -> res.getString(R.string.content_creature_grenouille_des_sous_bois)
    "Loup noir" -> res.getString(R.string.content_creature_loup_noir)
    "Dryade" -> res.getString(R.string.content_creature_dryade)
    "Harpie sylvestre" -> res.getString(R.string.content_creature_harpie_sylvestre)
    "Satyre" -> res.getString(R.string.content_creature_satyre)
    "Reine des abeilles" -> res.getString(R.string.content_creature_reine_des_abeilles)
    "Rôdeur des racines" -> res.getString(R.string.content_creature_rodeur_des_racines)
    "Ours noir" -> res.getString(R.string.content_creature_ours_noir)
    "Tréant" -> res.getString(R.string.content_creature_treant)
    "Chasseur d'épines" -> res.getString(R.string.content_creature_chasseur_depines)
    "Rôdeur des lianes" -> res.getString(R.string.content_creature_rodeur_des_lianes)
    "Griffon des bois" -> res.getString(R.string.content_creature_griffon_des_bois)
    "Vase" -> res.getString(R.string.content_creature_vase)
    "Gelée" -> res.getString(R.string.content_creature_gelee)
    "Gelée d'azur" -> res.getString(R.string.content_creature_gelee_dazur)
    "Œil géant" -> res.getString(R.string.content_creature_il_geant)
    "Champignon errant" -> res.getString(R.string.content_creature_champignon_errant)
    "Vase mortelle" -> res.getString(R.string.content_creature_vase_mortelle)
    "Chapeau de mort" -> res.getString(R.string.content_creature_chapeau_de_mort)
    "Œil drainant" -> res.getString(R.string.content_creature_il_drainant)
    "Rejeton étoilé" -> res.getString(R.string.content_creature_rejeton_etoile)
    "Horreur rampante" -> res.getString(R.string.content_creature_horreur_rampante)
    "Masse stellaire" -> res.getString(R.string.content_creature_masse_stellaire)
    "Grand orbe d'yeux" -> res.getString(R.string.content_creature_grand_orbe_dyeux)
    "Zyme ancienne" -> res.getString(R.string.content_creature_zyme_ancienne)
    "Diablotin de braise" -> res.getString(R.string.content_creature_diablotin_de_braise)
    "Diablotin d'ombre" -> res.getString(R.string.content_creature_diablotin_dombre)
    "Diablotin de fer" -> res.getString(R.string.content_creature_diablotin_de_fer)
    "Ufetubus" -> res.getString(R.string.content_creature_ufetubus)
    "Légionnaire des abysses" -> res.getString(R.string.content_creature_legionnaire_des_abysses)
    "Sentinelle du trône" -> res.getString(R.string.content_creature_sentinelle_du_trone)
    "Diable de lave" -> res.getString(R.string.content_creature_diable_de_lave)
    "Balrug déchu" -> res.getString(R.string.content_creature_balrug_dechu)
    "Bourreau" -> res.getString(R.string.content_creature_bourreau)
    "Faucheur" -> res.getString(R.string.content_creature_faucheur)
    "Mangeur d'âmes" -> res.getString(R.string.content_creature_mangeur_dames)
    "Géant de fer" -> res.getString(R.string.content_creature_geant_de_fer)
    "Roi des Cryptes" -> res.getString(R.string.content_creature_roi_des_cryptes)
    "Liche ancienne" -> res.getString(R.string.content_creature_liche_ancienne)
    "Hydre de Lerne" -> res.getString(R.string.content_creature_hydre_de_lerne)
    "Grande naga" -> res.getString(R.string.content_creature_grande_naga)
    "Manticore" -> res.getString(R.string.content_creature_manticore)
    "Hydre mineure" -> res.getString(R.string.content_creature_hydre_mineure)
    "Seigneur de la Forge" -> res.getString(R.string.content_creature_seigneur_de_la_forge)
    "Dragon de givre" -> res.getString(R.string.content_creature_dragon_de_givre)
    "Dispater" -> res.getString(R.string.content_creature_dispater)
    "Asmodée" -> res.getString(R.string.content_creature_asmodee)
    "Éreshkigal" -> res.getString(R.string.content_creature_ereshkigal)
    "Gloorx Vloq" -> res.getString(R.string.content_creature_gloorx_vloq)
    "Mnoleg" -> res.getString(R.string.content_creature_mnoleg)
    "L'Âme Première" -> res.getString(R.string.content_creature_lame_premiere)
    "Mère des Abysses" -> res.getString(R.string.content_creature_mere_des_abysses)
    else -> value
}

@Composable
fun creatureNameRes(value: String): String = creatureName(LocalContext.current.resources, value)

fun itemBaseName(res: Resources, value: String): String = when (value) {
    "Lame runique" -> res.getString(R.string.content_item_lame_runique)
    "Hache de guerre" -> res.getString(R.string.content_item_hache_de_guerre)
    "Masse d'armes" -> res.getString(R.string.content_item_masse_darmes)
    "Lance du guet" -> res.getString(R.string.content_item_lance_du_guet)
    "Cotte de mailles" -> res.getString(R.string.content_item_cotte_de_mailles)
    "Plate du gardien" -> res.getString(R.string.content_item_plate_du_gardien)
    "Heaume gravé" -> res.getString(R.string.content_item_heaume_grave)
    "Gants de braise" -> res.getString(R.string.content_item_gants_de_braise)
    "Bottes de mousse" -> res.getString(R.string.content_item_bottes_de_mousse)
    "Anneau d'âme" -> res.getString(R.string.content_item_anneau_dame)
    "Idole dorée" -> res.getString(R.string.content_item_idole_doree)
    "Heaume d'art" -> res.getString(R.string.content_item_heaume_dart)
    "Coffret-reliquaire" -> res.getString(R.string.content_item_coffret_reliquaire)
    "Lanterne des âmes" -> res.getString(R.string.content_item_lanterne_des_ames)
    "Lame d'ossuaire" -> res.getString(R.string.content_item_lame_dossuaire)
    "Suaire du veilleur" -> res.getString(R.string.content_item_suaire_du_veilleur)
    "Heaume des cryptes" -> res.getString(R.string.content_item_heaume_des_cryptes)
    "Amulette de garde" -> res.getString(R.string.content_item_amulette_de_garde)
    "Hache gnoll" -> res.getString(R.string.content_item_hache_gnoll)
    "Carapace de coléoptère" -> res.getString(R.string.content_item_carapace_de_coleoptere)
    "Bottes de soie" -> res.getString(R.string.content_item_bottes_de_soie)
    "Masque de gnoll" -> res.getString(R.string.content_item_masque_de_gnoll)
    "Lame de braise" -> res.getString(R.string.content_item_lame_de_braise)
    "Plate de cendre" -> res.getString(R.string.content_item_plate_de_cendre)
    "Bottes de limier" -> res.getString(R.string.content_item_bottes_de_limier)
    "Anneau de soufre" -> res.getString(R.string.content_item_anneau_de_soufre)
    "Marteau de forge" -> res.getString(R.string.content_item_marteau_de_forge)
    "Harnois de forge" -> res.getString(R.string.content_item_harnois_de_forge)
    "Gantelets de fer" -> res.getString(R.string.content_item_gantelets_de_fer)
    "Anneau de vif-argent" -> res.getString(R.string.content_item_anneau_de_vif_argent)
    "Lame de givre" -> res.getString(R.string.content_item_lame_de_givre)
    "Cotte de glace" -> res.getString(R.string.content_item_cotte_de_glace)
    "Bottes des cimes" -> res.getString(R.string.content_item_bottes_des_cimes)
    "Anneau d'orage" -> res.getString(R.string.content_item_anneau_dorage)
    "Bâton de dryade" -> res.getString(R.string.content_item_baton_de_dryade)
    "Armure d'écorce" -> res.getString(R.string.content_item_armure_decorce)
    "Bottes de feuilles" -> res.getString(R.string.content_item_bottes_de_feuilles)
    "Corne de satyre" -> res.getString(R.string.content_item_corne_de_satyre)
    "Fouet de tentacule" -> res.getString(R.string.content_item_fouet_de_tentacule)
    "Carapace d'abîme" -> res.getString(R.string.content_item_carapace_dabime)
    "Anneau de mucilage" -> res.getString(R.string.content_item_anneau_de_mucilage)
    "Orbe du néant" -> res.getString(R.string.content_item_orbe_du_neant)
    "Lame du trône" -> res.getString(R.string.content_item_lame_du_trone)
    "Harnois du trône" -> res.getString(R.string.content_item_harnois_du_trone)
    "Couronne des âmes" -> res.getString(R.string.content_item_couronne_des_ames)
    "Anneau des âmes" -> res.getString(R.string.content_item_anneau_des_ames)
    "Lame du veilleur" -> res.getString(R.string.content_item_lame_du_veilleur)
    "Heaume de fer" -> res.getString(R.string.content_item_heaume_de_fer)
    "Gantelets de braise" -> res.getString(R.string.content_item_gantelets_de_braise)
    "Anneau de régénération" -> res.getString(R.string.content_item_anneau_de_regeneration)
    "Épée ancienne" -> res.getString(R.string.content_item_epee_ancienne)
    "Masse runique" -> res.getString(R.string.content_item_masse_runique)
    "Heaume elfique" -> res.getString(R.string.content_item_heaume_elfique)
    "Amulette d'âme" -> res.getString(R.string.content_item_amulette_dame)
    else -> value
}

@Composable
fun itemBaseNameRes(value: String): String = itemBaseName(LocalContext.current.resources, value)

fun consumableName(res: Resources, value: String): String = when (value) {
    "potion_soin" -> res.getString(R.string.content_consumable_potion_soin)
    "herbe_lune" -> res.getString(R.string.content_consumable_herbe_lune)
    "champignon_braise" -> res.getString(R.string.content_consumable_champignon_braise)
    "elixir_veilleur" -> res.getString(R.string.content_consumable_elixir_veilleur)
    "pain_route" -> res.getString(R.string.content_consumable_pain_route)
    "fiole_rage" -> res.getString(R.string.content_consumable_fiole_rage)
    "ecorce_fer" -> res.getString(R.string.content_consumable_ecorce_fer)
    "poudre_eclair" -> res.getString(R.string.content_consumable_poudre_eclair)
    "bombe_feu" -> res.getString(R.string.content_consumable_bombe_feu)
    "elixir_vampire" -> res.getString(R.string.content_consumable_elixir_vampire)
    "parchemin_critique" -> res.getString(R.string.content_consumable_parchemin_critique)
    "champignon_berserk" -> res.getString(R.string.content_consumable_champignon_berserk)
    "potion_hate" -> res.getString(R.string.content_consumable_potion_hate)
    "potion_force" -> res.getString(R.string.content_consumable_potion_force)
    "potion_regen" -> res.getString(R.string.content_consumable_potion_regen)
    "viande_sechee" -> res.getString(R.string.content_consumable_viande_sechee)
    else -> value
}

@Composable
fun consumableNameRes(value: String): String = consumableName(LocalContext.current.resources, value)

fun consumableDesc(res: Resources, value: String): String = when (value) {
    "potion_soin" -> res.getString(R.string.content_consumable_potion_soin_desc)
    "herbe_lune" -> res.getString(R.string.content_consumable_herbe_lune_desc)
    "champignon_braise" -> res.getString(R.string.content_consumable_champignon_braise_desc)
    "elixir_veilleur" -> res.getString(R.string.content_consumable_elixir_veilleur_desc)
    "pain_route" -> res.getString(R.string.content_consumable_pain_route_desc)
    "fiole_rage" -> res.getString(R.string.content_consumable_fiole_rage_desc)
    "ecorce_fer" -> res.getString(R.string.content_consumable_ecorce_fer_desc)
    "poudre_eclair" -> res.getString(R.string.content_consumable_poudre_eclair_desc)
    "bombe_feu" -> res.getString(R.string.content_consumable_bombe_feu_desc)
    "elixir_vampire" -> res.getString(R.string.content_consumable_elixir_vampire_desc)
    "parchemin_critique" -> res.getString(R.string.content_consumable_parchemin_critique_desc)
    "champignon_berserk" -> res.getString(R.string.content_consumable_champignon_berserk_desc)
    "potion_hate" -> res.getString(R.string.content_consumable_potion_hate_desc)
    "potion_force" -> res.getString(R.string.content_consumable_potion_force_desc)
    "potion_regen" -> res.getString(R.string.content_consumable_potion_regen_desc)
    "viande_sechee" -> res.getString(R.string.content_consumable_viande_sechee_desc)
    else -> ""
}

@Composable
fun consumableDescRes(value: String): String = consumableDesc(LocalContext.current.resources, value)

fun effectLabel(res: Resources, value: String): String = when (value) {
    "vampirisme" -> res.getString(R.string.content_effect_vampirisme)
    "rempart" -> res.getString(R.string.content_effect_rempart)
    "frenesie" -> res.getString(R.string.content_effect_frenesie)
    "chasse" -> res.getString(R.string.content_effect_chasse)
    "vitalite" -> res.getString(R.string.content_effect_vitalite)
    "precision" -> res.getString(R.string.content_effect_precision)
    else -> ""
}

@Composable
fun effectLabelRes(value: String): String = effectLabel(LocalContext.current.resources, value)

fun effectDesc(res: Resources, value: String): String = when (value) {
    "vampirisme" -> res.getString(R.string.content_effect_vampirisme_desc)
    "rempart" -> res.getString(R.string.content_effect_rempart_desc)
    "frenesie" -> res.getString(R.string.content_effect_frenesie_desc)
    "chasse" -> res.getString(R.string.content_effect_chasse_desc)
    "vitalite" -> res.getString(R.string.content_effect_vitalite_desc)
    "precision" -> res.getString(R.string.content_effect_precision_desc)
    else -> ""
}

@Composable
fun effectDescRes(value: String): String = effectDesc(LocalContext.current.resources, value)

fun chestType(res: Resources, value: String): String = when (value) {
    "bois" -> res.getString(R.string.content_chest_bois)
    "fer" -> res.getString(R.string.content_chest_fer)
    "runique" -> res.getString(R.string.content_chest_runique)
    else -> res.getString(R.string.content_chest_runique)
}

@Composable
fun chestTypeRes(value: String): String = chestType(LocalContext.current.resources, value)

fun modifierLabel(res: Resources, value: String): String = when (value) {
    "RAPIDE" -> res.getString(R.string.content_modifier_rapide)
    "BLINDÉ" -> res.getString(R.string.content_modifier_blinde)
    "BERSERK" -> res.getString(R.string.content_modifier_berserk)
    "RÉGÉNÉRANT" -> res.getString(R.string.content_modifier_regenerant)
    "VOLEUR" -> res.getString(R.string.content_modifier_voleur)
    else -> value
}

@Composable
fun modifierLabelRes(value: String): String = modifierLabel(LocalContext.current.resources, value)

fun buffLabel(res: Resources, value: String): String = when (value) {
    "RAGE" -> res.getString(R.string.content_buff_rage)
    "ÉCORCE" -> res.getString(R.string.content_buff_ecorce)
    "VIF" -> res.getString(R.string.content_buff_vif)
    "CRIT" -> res.getString(R.string.content_buff_crit)
    "VAMP" -> res.getString(R.string.content_buff_vamp)
    "RÉGÉN" -> res.getString(R.string.content_buff_regen)
    "BERSERK" -> res.getString(R.string.content_buff_berserk)
    "FRAGILE" -> res.getString(R.string.content_buff_fragile)
    "ATQ" -> res.getString(R.string.content_buff_atq)
    else -> value
}

@Composable
fun buffLabelRes(value: String): String = buffLabel(LocalContext.current.resources, value)

fun specialTitle(res: Resources, value: String): String = when (value) {
    "autel" -> res.getString(R.string.content_special_autel_title)
    "source" -> res.getString(R.string.content_special_source_title)
    "marchand" -> res.getString(R.string.content_special_marchand_title)
    "statue" -> res.getString(R.string.content_special_statue_title)
    "risque" -> res.getString(R.string.content_special_risque_title)
    else -> ""
}

@Composable
fun specialTitleRes(value: String): String = specialTitle(LocalContext.current.resources, value)

fun specialText(res: Resources, value: String): String = when (value) {
    "autel" -> res.getString(R.string.content_special_autel_text)
    "source" -> res.getString(R.string.content_special_source_text)
    "marchand" -> res.getString(R.string.content_special_marchand_text)
    "statue" -> res.getString(R.string.content_special_statue_text)
    "risque" -> res.getString(R.string.content_special_risque_text)
    else -> ""
}

@Composable
fun specialTextRes(value: String): String = specialText(LocalContext.current.resources, value)

fun specialAccept(res: Resources, value: String): String = when (value) {
    "autel" -> res.getString(R.string.content_special_autel_accept)
    "source" -> res.getString(R.string.content_special_source_accept)
    "marchand" -> res.getString(R.string.content_special_marchand_accept)
    "statue" -> res.getString(R.string.content_special_statue_accept)
    "risque" -> res.getString(R.string.content_special_risque_accept)
    else -> ""
}

@Composable
fun specialAcceptRes(value: String): String = specialAccept(LocalContext.current.resources, value)

fun specialReward(res: Resources, value: String): String = when (value) {
    "autel" -> res.getString(R.string.content_special_autel_reward)
    "source" -> res.getString(R.string.content_special_source_reward)
    "statue" -> res.getString(R.string.content_special_statue_reward)
    "risque" -> res.getString(R.string.content_special_risque_reward)
    else -> ""
}

@Composable
fun specialRewardRes(value: String): String = specialReward(LocalContext.current.resources, value)
