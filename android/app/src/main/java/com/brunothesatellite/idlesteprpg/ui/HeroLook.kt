package com.brunothesatellite.idlesteprpg.ui

import com.brunothesatellite.idlesteprpg.engine.GameState
import com.brunothesatellite.idlesteprpg.engine.Item
import com.brunothesatellite.idlesteprpg.engine.equipped

internal fun heroBodyLayer(body: Item?): String? {
    if (body == null) return null
    val name = body.name.lowercase()
    val icon = body.icon.lowercase()
    return when {
        name.contains("robe") || name.contains("suaire") || icon.contains("robe") ->
            "hero/body_robe.png"
        icon.contains("ice_dragon") -> "hero/body_metal_blue.png"
        icon.contains("shadow_dragon") -> "hero/body_mesh_black.png"
        name.contains("plate") || name.contains("harnois") || name.contains("carapace") ||
            icon.contains("plate") || icon.contains("gold_dragon") ->
            "hero/body_plate.png"
        name.contains("maille") || name.contains("cotte") ||
            icon.contains("scale_mail") || icon.contains("ring_mail") ->
            "hero/body_mail_gray.png"
        name.contains("cuir") || name.contains("peau") || name.contains("écorce") ||
            name.contains("mousse") || name.contains("feuille") ||
            icon.contains("leather") || icon.contains("animal_skin") ->
            "hero/body_leather.png"
        name.contains("tunique") || name.contains("manteau") || name.contains("veste") ||
            icon.contains("coat") ->
            "hero/body_coat.png"
        body.rarity == "legendary" -> "hero/aragorn.png"
        else -> "hero/coat_red.png"
    }
}

internal fun heroHeadLayer(head: Item?): String? {
    if (head == null) return null
    val name = head.name.lowercase()
    val icon = head.icon.lowercase()
    val high = head.rarity == "rare" || head.rarity == "epic" || head.rarity == "legendary"
    return when {
        name.contains("capuche") || name.contains("masque") || icon.contains("hood") ->
            "hero/head_hood.png"
        name.contains("couronne") || icon.contains("crown") -> "hero/head_crown.png"
        name.contains("elfique") -> "hero/head_hood_green.png"
        name.contains("art") -> "hero/helm_plume.png"
        name.contains("gravé") || name.contains("grave") || name.contains("cryptes") ->
            "hero/head_helm_gray.png"
        name.contains("plume") -> "hero/helm_plume.png"
        name.contains("heaume") || name.contains("casque") ||
            icon.contains("helmet") || icon.contains("helm") ->
            when {
                head.rarity == "legendary" -> "hero/horned.png"
                high -> "hero/head_helm_gray.png"
                else -> "hero/helm_green.png"
            }
        head.rarity == "legendary" -> "hero/horned.png"
        high -> "hero/head_helm_gray.png"
        else -> "hero/helm_green.png"
    }
}

internal fun heroBootsLayer(feet: Item?): String? {
    if (feet == null) return null
    val name = feet.name.lowercase()
    val icon = feet.icon.lowercase()
    return when {
        name.contains("mousse") -> "hero/middle_green.png"
        name.contains("cime") || name.contains("givre") || name.contains("glace") ->
            "hero/boots_metal.png"
        name.contains("soie") || name.contains("limier") || name.contains("feuille") ||
            name.contains("cuir") || icon.contains("leather") -> "hero/boots_leather.png"
        name.contains("métal") || name.contains("fer") || name.contains("acier") ||
            icon.contains("metal") -> "hero/boots_metal.png"
        feet.rarity == "epic" || feet.rarity == "rare" || feet.rarity == "legendary" ->
            "hero/middle_green.png"
        else -> "hero/middle_brown2.png"
    }
}

internal fun heroLegsLayer(body: Item?): String? {
    if (body == null) return "hero/pants_brown.png"
    val name = body.name.lowercase()
    val icon = body.icon.lowercase()
    return when {
        name.contains("plate") || name.contains("harnois") || name.contains("carapace") ||
            icon.contains("plate") || icon.contains("gold_dragon") -> "hero/legs_plate.png"
        name.contains("maille") || name.contains("cotte") ||
            icon.contains("scale_mail") || icon.contains("ring_mail") -> "hero/legs_mail.png"
        name.contains("robe") || name.contains("suaire") || name.contains("tunique") ||
            name.contains("manteau") || icon.contains("robe") || icon.contains("coat") ->
            "hero/legs_cloth.png"
        else -> "hero/pants_brown.png"
    }
}

internal fun heroWeaponLayer(weapon: Item?): String? {
    if (weapon == null) return null
    val name = weapon.name.lowercase()
    val icon = weapon.icon.lowercase()
    val high = weapon.rarity == "rare" || weapon.rarity == "epic" || weapon.rarity == "legendary"
    return when {
        icon.contains("staff") || name.contains("bâton") || name.contains("sceptre") ->
            "hero/weapon_staff.png"
        icon.contains("dagger") || name.contains("dague") -> "hero/weapon_dagger.png"
        icon.contains("axe") || name.contains("hache") -> "hero/weapon_axe.png"
        icon.contains("mace") || icon.contains("eveningstar") || icon.contains("flail") ||
            name.contains("masse") || name.contains("marteau") || name.contains("fléau") ->
            "hero/weapon_mace.png"
        icon.contains("spear") || icon.contains("lance") || icon.contains("halberd") ||
            name.contains("lance") || name.contains("hallebarde") -> "hero/weapon_spear.png"
        icon.contains("whip") || name.contains("fouet") -> "hero/weapon_whip.png"
        icon.contains("greatsword") || (high && (name.contains("épée") || name.contains("lame"))) ->
            "hero/weapon_greatsword.png"
        high -> "hero/long_sword_slant.png"
        else -> "hero/broadsword.png"
    }
}


/** Couches du paper doll, dans l'ordre de dessin (fond puis premier plan). */
internal fun heroLookLayers(state: GameState): List<String> {
    val weapon = equipped(state, "weapon")
    val body = equipped(state, "body")
    val head = equipped(state, "head")
    val feet = equipped(state, "feet")
    return buildList {
        add("hero/human_m.png")
        heroBodyLayer(body)?.let(::add)
        heroLegsLayer(body)?.let(::add)
        heroBootsLayer(feet)?.let(::add)
        heroHeadLayer(head)?.let(::add)
        heroWeaponLayer(weapon)?.let(::add)
        add("hero/buckler_round.png")
    }
}
