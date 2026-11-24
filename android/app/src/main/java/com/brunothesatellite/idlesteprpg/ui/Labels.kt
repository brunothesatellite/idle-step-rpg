package com.brunothesatellite.idlesteprpg.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.brunothesatellite.idlesteprpg.R
import java.util.Locale

/* Libellés d'affichage traduits. Les fonctions du moteur restent la source
 * de vérité pour les identifiants et les règles ; ces helpers fournissent
 * uniquement le texte visible en français ou en anglais. */

@Composable
fun slotLabelRes(slot: String): String = when (slot) {
    "weapon" -> stringResource(R.string.dashboard_slot_weapon)
    "head" -> stringResource(R.string.dashboard_slot_head)
    "body" -> stringResource(R.string.dashboard_slot_body)
    "hands" -> stringResource(R.string.dashboard_slot_hands)
    "feet" -> stringResource(R.string.dashboard_slot_feet)
    "talisman" -> stringResource(R.string.dashboard_slot_talisman)
    else -> stringResource(R.string.dashboard_item_generic)
}

@Composable
fun rarityLabelRes(rarity: String): String = when (rarity) {
    "uncommon" -> stringResource(R.string.dashboard_rarity_uncommon)
    "rare" -> stringResource(R.string.dashboard_rarity_rare)
    "epic" -> stringResource(R.string.dashboard_rarity_epic)
    "legendary" -> stringResource(R.string.dashboard_rarity_legendary)
    else -> stringResource(R.string.dashboard_rarity_common)
}

@Composable
fun setLabelRes(name: String): String = when (name) {
    "veilleur" -> stringResource(R.string.dashboard_set_veilleur)
    "braise" -> stringResource(R.string.dashboard_set_braise)
    "givre" -> stringResource(R.string.dashboard_set_givre)
    "os" -> stringResource(R.string.dashboard_set_os)
    "toile" -> stringResource(R.string.dashboard_set_toile)
    "cendre" -> stringResource(R.string.dashboard_set_cendre)
    "forge" -> stringResource(R.string.dashboard_set_forge)
    "serment" -> stringResource(R.string.dashboard_set_serment)
    "bois" -> stringResource(R.string.dashboard_set_bois)
    "abysse" -> stringResource(R.string.dashboard_set_abysse)
    "trone" -> stringResource(R.string.dashboard_set_trone)
    else -> ""
}

@Composable
fun tierLabelRes(ferveur: Int): String = when {
    ferveur >= 100 -> stringResource(R.string.dashboard_tier_burning_soul)
    ferveur >= 75 -> stringResource(R.string.dashboard_tier_tempest)
    ferveur >= 50 -> stringResource(R.string.dashboard_tier_furnace)
    ferveur >= 25 -> stringResource(R.string.dashboard_tier_ember)
    else -> stringResource(R.string.dashboard_tier_kindling)
}

@Composable
fun titleLabelRes(floor: Int): String = when {
    floor >= 100 -> stringResource(R.string.dashboard_title_soul_legend)
    floor >= 71 -> stringResource(R.string.dashboard_title_throne_sovereign)
    floor >= 61 -> stringResource(R.string.dashboard_title_abyss_walker)
    floor >= 51 -> stringResource(R.string.dashboard_title_wood_watcher)
    floor >= 41 -> stringResource(R.string.dashboard_title_peaks_sovereign)
    floor >= 31 -> stringResource(R.string.dashboard_title_forge_guardian)
    floor >= 21 -> stringResource(R.string.dashboard_title_ash_walker)
    floor >= 11 -> stringResource(R.string.dashboard_title_abyss_pilgrim)
    floor >= 5 -> stringResource(R.string.dashboard_title_crypt_watcher)
    else -> stringResource(R.string.dashboard_title_pilgrim)
}

@Composable
fun biomeLabelRes(id: String): String = when (id) {
    "crypts" -> stringResource(R.string.dashboard_biome_crypts)
    "catacombs" -> stringResource(R.string.dashboard_biome_catacombs)
    "ash" -> stringResource(R.string.dashboard_biome_ash)
    "forge" -> stringResource(R.string.dashboard_biome_forge)
    "peaks" -> stringResource(R.string.dashboard_biome_peaks)
    "woods" -> stringResource(R.string.dashboard_biome_woods)
    "abyss" -> stringResource(R.string.dashboard_biome_abyss)
    else -> stringResource(R.string.dashboard_biome_throne)
}

@Composable
fun biomeRuleLabelRes(id: String): String = when (id) {
    "crypts" -> stringResource(R.string.dashboard_rule_crypts_label)
    "catacombs" -> stringResource(R.string.dashboard_rule_catacombs_label)
    "ash" -> stringResource(R.string.dashboard_rule_ash_label)
    "forge" -> stringResource(R.string.dashboard_rule_forge_label)
    "peaks" -> stringResource(R.string.dashboard_rule_peaks_label)
    "woods" -> stringResource(R.string.dashboard_rule_woods_label)
    "abyss" -> stringResource(R.string.dashboard_rule_abyss_label)
    "trone_broken" -> stringResource(R.string.dashboard_rule_trone_broken_label)
    else -> stringResource(R.string.dashboard_rule_throne_label)
}

@Composable
fun biomeRuleDescRes(id: String): String = when (id) {
    "crypts" -> stringResource(R.string.dashboard_rule_crypts_desc)
    "catacombs" -> stringResource(R.string.dashboard_rule_catacombs_desc)
    "ash" -> stringResource(R.string.dashboard_rule_ash_desc)
    "forge" -> stringResource(R.string.dashboard_rule_forge_desc)
    "peaks" -> stringResource(R.string.dashboard_rule_peaks_desc)
    "woods" -> stringResource(R.string.dashboard_rule_woods_desc)
    "abyss" -> stringResource(R.string.dashboard_rule_abyss_desc)
    "trone_broken" -> stringResource(R.string.dashboard_rule_trone_broken_desc)
    else -> stringResource(R.string.dashboard_rule_throne_desc)
}

@Composable
fun upgradeNameRes(key: String): String = when (key) {
    "blade" -> stringResource(R.string.dashboard_upgrade_blade_name)
    "breath" -> stringResource(R.string.dashboard_upgrade_breath_name)
    "swift" -> stringResource(R.string.dashboard_upgrade_swift_name)
    else -> stringResource(R.string.dashboard_upgrade_generic_name)
}

@Composable
fun upgradeDescRes(key: String): String = when (key) {
    "blade" -> stringResource(R.string.dashboard_upgrade_blade_desc)
    "breath" -> stringResource(R.string.dashboard_upgrade_breath_desc)
    "swift" -> stringResource(R.string.dashboard_upgrade_swift_desc)
    else -> stringResource(R.string.dashboard_upgrade_generic_desc)
}

@Composable
fun abilityLabelRes(id: String): String = when (id) {
    "drain" -> stringResource(R.string.dashboard_ability_drain)
    "regen" -> stringResource(R.string.dashboard_ability_regen)
    "roots" -> stringResource(R.string.dashboard_ability_roots)
    "acid" -> stringResource(R.string.dashboard_ability_acid)
    "breath" -> stringResource(R.string.dashboard_ability_breath)
    "shield" -> stringResource(R.string.dashboard_ability_shield)
    "pressure" -> stringResource(R.string.dashboard_ability_pressure)
    "rupture" -> stringResource(R.string.dashboard_ability_rupture)
    else -> stringResource(R.string.dashboard_ability_slam)
}

@Composable
fun contextLabelRes(context: String): String = when (context) {
    "combat" -> stringResource(R.string.dashboard_context_combat)
    "camp" -> stringResource(R.string.dashboard_context_camp)
    else -> stringResource(R.string.dashboard_context_both)
}

/** Nombre formaté selon la langue de l'application (virgule ou point décimal). */
@Composable
fun decimalRes(value: Double, digits: Int = 1): String {
    val locale: Locale = LocalConfiguration.current.locales[0]
    return String.format(locale, "%.${digits}f", value)
}
