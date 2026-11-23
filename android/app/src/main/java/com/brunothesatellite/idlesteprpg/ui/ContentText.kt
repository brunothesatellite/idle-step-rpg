package com.brunothesatellite.idlesteprpg.ui

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.brunothesatellite.idlesteprpg.R
import com.brunothesatellite.idlesteprpg.engine.CONSUMABLES
import com.brunothesatellite.idlesteprpg.engine.Item

/* Traduction d'affichage du contenu dynamique. Les sauvegardes conservent les
 * textes français (objets, journal, rencontres) ; ces fonctions les traduisent
 * à l'écran sans migration. En français, le texte d'origine est renvoyé tel quel. */

private fun isFrench(res: Resources): Boolean =
    res.configuration.locales[0].language == "fr"

private val FRENCH_EFFECTS = mapOf(
    "VAMPIRISME" to "vampirisme",
    "REMPART" to "rempart",
    "FRÉNÉSIE" to "frenesie",
    "CHASSE" to "chasse",
    "VITALITÉ" to "vitalite",
    "PRÉCISION" to "precision",
)

private val FRENCH_ABILITIES = mapOf(
    "DRAIN D'ÂME" to R.string.dashboard_ability_drain,
    "RÉGÉNÉRATION" to R.string.dashboard_ability_regen,
    "RACINES" to R.string.dashboard_ability_roots,
    "ACIDE" to R.string.dashboard_ability_acid,
    "SOUFFLE" to R.string.dashboard_ability_breath,
    "BLINDAGE" to R.string.dashboard_ability_shield,
    "PRESSION" to R.string.dashboard_ability_pressure,
    "FRAPPE LOURDE" to R.string.dashboard_ability_slam,
)

private val FRENCH_CHESTS = mapOf(
    "COFFRE DE BOIS" to "bois",
    "COFFRE DE FER" to "fer",
    "COFFRE RUNIQUE" to "runique",
)

private val FRENCH_UPGRADES = mapOf(
    "Lame affûtée" to R.string.dashboard_upgrade_blade_name,
    "Souffle ancien" to R.string.dashboard_upgrade_breath_name,
    "Pas vifs" to R.string.dashboard_upgrade_swift_name,
)

private val FRENCH_RARITIES = mapOf(
    "COMMUN" to R.string.dashboard_rarity_common,
    "INHABITUEL" to R.string.dashboard_rarity_uncommon,
    "RARE" to R.string.dashboard_rarity_rare,
    "ÉPIQUE" to R.string.dashboard_rarity_epic,
    "LÉGENDAIRE" to R.string.dashboard_rarity_legendary,
)

private val FRENCH_SPECIALS = mapOf(
    "AUTEL DE BRAISE" to "autel",
    "SOURCE CLAIRE" to "source",
    "MARCHAND ERRANT" to "marchand",
    "STATUE DU SERMENT" to "statue",
    "COFFRE SCELLÉ" to "risque",
)

private val FRENCH_BIOMES = mapOf(
    "CRYPTES DE VAEL" to R.string.dashboard_biome_crypts,
    "CATACOMBES" to R.string.dashboard_biome_catacombs,
    "CAVERNES DE CENDRE" to R.string.dashboard_biome_ash,
    "FORGE NOYÉE" to R.string.dashboard_biome_forge,
    "CIMES DU SERMENT" to R.string.dashboard_biome_peaks,
    "BOIS DES ÂMES" to R.string.dashboard_biome_woods,
    "ABÎME ORGANIQUE" to R.string.dashboard_biome_abyss,
    "TRÔNE DES ÂMES" to R.string.dashboard_biome_throne,
)

private val FIXED = mapOf(
    "Nouveau jour — 00:00 · serments renouvelés" to R.string.journal_new_day,
    "Camp · combat réinitialisé" to R.string.journal_camp_reset,
    "Serment de marche accompli · +150 or, +1 éclat" to R.string.journal_oath_walk,
    "Serment de chasse accompli · +250 or, +2 éclats" to R.string.journal_oath_hunt,
    "Serment de curiosité accompli · +100 or, objet inhabituel" to R.string.journal_oath_curiosity,
    "Le gardien s'embrase" to R.string.journal_guardian_ignite,
    "Fin du repli — le sentier reprend" to R.string.journal_retreat_end,
    "Défaite — repli de 20 m dans la zone de farm" to R.string.journal_defeat,
    "Éclat d'âme — soin complet" to R.string.journal_shard_heal,
    "Aucune rencontre." to R.string.msg_no_encounter,
    "Offrande acceptée · +180 XP" to R.string.msg_offering,
    "2 éclats requis." to R.string.msg_two_shards,
    "PV entièrement restaurés." to R.string.msg_hp_restored,
    "1 ration requise." to R.string.msg_one_ration,
    "Or insuffisant." to R.string.msg_not_enough_gold,
    "Ferveur +15 · +1 éclat." to R.string.msg_statue,
    "100 or requis." to R.string.msg_hundred_gold,
    "Pari gagné · +300 or" to R.string.msg_bet_won,
    "Pari perdu · −100 or" to R.string.msg_bet_lost,
    "Consommable inconnu." to R.string.msg_unknown_consumable,
    "PV déjà complets." to R.string.msg_hp_full,
    "PV et ferveur déjà au maximum." to R.string.msg_hp_fervour_full,
    "Ferveur déjà au maximum." to R.string.msg_fervour_full,
    "Effet inconnu." to R.string.msg_unknown_effect,
    "PV complets" to R.string.msg_hp_full_short,
)

private fun consumableIdFor(name: String): String? =
    CONSUMABLES.firstOrNull { it.name == name }?.id

fun consumableNameFrom(res: Resources, name: String): String {
    val id = consumableIdFor(name) ?: return name
    return consumableName(res, id)
}

fun biomeNameRes(res: Resources, raw: String): String {
    val key = FRENCH_BIOMES[raw] ?: return raw
    return res.getString(key)
}

fun creatureDisplay(res: Resources, raw: String): String {
    if (isFrench(res)) return raw
    val parts = raw.split(" · ")
    val out = StringBuilder(creatureName(res, parts.first()))
    for (i in 1 until parts.size) {
        out.append(" · ").append(modifierLabel(res, parts[i]))
    }
    return out.toString()
}

fun itemNameFrom(res: Resources, raw: String): String {
    if (isFrench(res)) return raw
    val base = raw.substringBefore(" · ")
    val suffix = raw.substringAfter(" · ", "")
    val localizedBase = itemBaseName(res, base)
    if (suffix.isBlank()) return localizedBase
    val effect = FRENCH_EFFECTS[suffix]
    return if (effect != null) localizedBase + " · " + effectLabel(res, effect) else raw
}

fun itemName(res: Resources, name: String, effect: String): String {
    if (isFrench(res)) return name
    val localizedBase = itemBaseName(res, name.substringBefore(" · "))
    return if (effect.isBlank()) localizedBase else localizedBase + " · " + effectLabel(res, effect)
}

private val FRENCH_TIERS = mapOf(
    "DÉBUT DE PALIER" to R.string.codex_tier_easy,
    "MILIEU DE PALIER" to R.string.codex_tier_mid,
    "FIN DE PALIER" to R.string.codex_tier_hard,
)

fun tierRes(res: Resources, raw: String): String {
    val key = FRENCH_TIERS[raw] ?: return raw
    return res.getString(key)
}

fun zoneRes(res: Resources, raw: String): String =
    if (raw == "palier verrouillé") res.getString(R.string.codex_locked_floor) else raw

fun setBonusRes(res: Resources, text: String): String {
    if (isFrench(res) || text.isBlank()) return text
    return text
        .replace("PV/s", "HP/s")
        .replace("ATQ", "ATK")
        .replace("PV", "HP")
        .replace("VIT", "SPD")
        .replace("dégâts gardiens", "guardian damage")
        .replace("dégâts critiques", "critical damage")
        .replace("dégâts", "damage")
        .replace("vol de vie", "lifesteal")
        .replace("crit.", "crit")
        .replace(",", ".")
}

fun statLineRes(res: Resources, item: Item): String {
    val parts = mutableListOf<String>()
    if (item.atk != 0) parts += res.getString(R.string.content_stat_atk, item.atk)
    if (item.hp != 0) parts += res.getString(R.string.msg_hp_gain, item.hp)
    if (item.vitPct != 0) parts += res.getString(R.string.content_stat_vit, item.vitPct)
    if (item.critPct != 0) parts += res.getString(R.string.content_stat_crit, item.critPct)
    if (item.regen != 0.0) {
        val value = String.format(res.configuration.locales[0], "%.1f", item.regen)
        parts += res.getString(R.string.content_stat_regen, value)
    }
    return parts.joinToString(" · ")
}

fun rankTitle(res: Resources, floor: Int): String = when {
    floor >= 100 -> res.getString(R.string.dashboard_title_soul_legend)
    floor >= 71 -> res.getString(R.string.dashboard_title_throne_sovereign)
    floor >= 61 -> res.getString(R.string.dashboard_title_abyss_walker)
    floor >= 51 -> res.getString(R.string.dashboard_title_wood_watcher)
    floor >= 41 -> res.getString(R.string.dashboard_title_peaks_sovereign)
    floor >= 31 -> res.getString(R.string.dashboard_title_forge_guardian)
    floor >= 21 -> res.getString(R.string.dashboard_title_ash_walker)
    floor >= 11 -> res.getString(R.string.dashboard_title_abyss_pilgrim)
    floor >= 5 -> res.getString(R.string.dashboard_title_crypt_watcher)
    else -> res.getString(R.string.dashboard_title_pilgrim)
}

/* Rank from its French label (frozen in the journal). */
private fun rankTitleFrom(res: Resources, raw: String): String = when (raw) {
    "Pèlerin" -> rankTitle(res, 1)
    "Veilleur des Cryptes" -> rankTitle(res, 5)
    "Pèlerin des Abysses" -> rankTitle(res, 11)
    "Marcheur de Cendre" -> rankTitle(res, 21)
    "Gardien de la Forge" -> rankTitle(res, 31)
    "Souverain des Cimes" -> rankTitle(res, 41)
    "Veilleur du Bois" -> rankTitle(res, 51)
    "Marcheur de l'Abîme" -> rankTitle(res, 61)
    "Souverain du Trône" -> rankTitle(res, 71)
    "Légende des Âmes" -> rankTitle(res, 100)
    else -> raw
}

private fun specialTitleFrom(res: Resources, raw: String): String {
    val type = FRENCH_SPECIALS[raw] ?: return raw
    return specialTitle(res, type)
}

private fun lootSummary(res: Resources, raw: String): String {
    val item = Regex("""^(.+) ×(\d+)$""")
    return raw.split(" · ").joinToString(" · ") { part ->
        val m = item.matchEntire(part)
        if (m == null) part
        else consumableNameFrom(res, m.groupValues[1]) + " ×" + m.groupValues[2]
    }
}

private fun normalizeDecimal(value: String): String = value.replace(',', '.')

private fun effectFrom(res: Resources, raw: String): String {
    if (raw == "aucun effet") return res.getString(R.string.journal_no_effect)
    val id = FRENCH_EFFECTS[raw] ?: return raw
    return effectLabel(res, id)
}

private class Rule(
    val regex: Regex,
    val resolve: (Resources, MatchResult) -> String,
)

private val RULES: List<Rule> = listOf(
    Rule(Regex("""^Série de (\d+) jours de marche$""")) { r, m ->
        r.getString(R.string.journal_streak, m.groupValues[1].toInt())
    },
    Rule(Regex("""^Rencontre : (.+)$""")) { r, m ->
        r.getString(R.string.journal_encounter, specialTitleFrom(r, m.groupValues[1]))
    },
    Rule(Regex("""^Consommé · (.+) · (.+)$""")) { r, m ->
        r.getString(
            R.string.journal_consumed,
            consumableNameFrom(r, m.groupValues[1]),
            translateText(r, m.groupValues[2])
        )
    },
    Rule(Regex("""^Butin · (.+)$""")) { r, m ->
        r.getString(R.string.journal_loot, lootSummary(r, m.groupValues[1]))
    },
    Rule(Regex("""^Rattrapage (.+) · (\d+) jour\(s\) · (\d+) pas$""")) { r, m ->
        r.getString(
            R.string.journal_catchup,
            m.groupValues[1],
            m.groupValues[2].toInt(),
            m.groupValues[3].toInt()
        )
    },
    Rule(Regex("""^(\d+) pas (.+) - (\d+) m$""")) { r, m ->
        r.getString(
            R.string.journal_steps_added,
            m.groupValues[1].toInt(),
            m.groupValues[2],
            m.groupValues[3].toInt()
        )
    },
    Rule(Regex("""^(\d+) pas ignorés \(rythme supérieur à 250/min\)$""")) { r, m ->
        r.getString(R.string.journal_steps_ignored, m.groupValues[1].toInt())
    },
    Rule(Regex("""^Démo \+(\d+) pas$""")) { r, m ->
        r.getString(R.string.journal_demo, m.groupValues[1].toInt())
    },
    Rule(Regex("""^Ration consommée \(\+(\d+) PV\)$""")) { r, m ->
        r.getString(R.string.journal_ration, m.groupValues[1].toInt())
    },
    Rule(Regex("""^(.+) vaincu — palier (\d+) conservé \(farm\)$""")) { r, m ->
        r.getString(
            R.string.journal_kill_farm_floor,
            creatureDisplay(r, m.groupValues[1]),
            m.groupValues[2].toInt()
        )
    },
    Rule(Regex("""^Farm · (.+) vaincu · \+(\d+) PV$""")) { r, m ->
        r.getString(
            R.string.journal_farm_heal,
            creatureDisplay(r, m.groupValues[1]),
            m.groupValues[2].toInt()
        )
    },
    Rule(Regex("""^Jalon · palier (\d+) franchi — (.+) · \+5 éclats$""")) { r, m ->
        r.getString(
            R.string.journal_milestone_floor,
            m.groupValues[1].toInt(),
            rankTitleFrom(r, m.groupValues[2])
        )
    },
    Rule(Regex("""^(.+) vaincu — palier (\d+)$""")) { r, m ->
        r.getString(
            R.string.journal_kill_floor,
            creatureDisplay(r, m.groupValues[1]),
            m.groupValues[2].toInt()
        )
    },
    Rule(Regex("""^(.+) vaincu · \+(\d+) or$""")) { r, m ->
        r.getString(
            R.string.journal_kill_gold,
            creatureDisplay(r, m.groupValues[1]),
            m.groupValues[2].toInt()
        )
    },
    Rule(Regex("""^Niveau (\d+) atteint$""")) { r, m ->
        r.getString(R.string.journal_level, m.groupValues[1].toInt())
    },
    Rule(Regex("""^Coffre forcé · \+(\d+) or$""")) { r, m ->
        r.getString(R.string.journal_chest_forced, m.groupValues[1].toInt())
    },
    Rule(Regex("""^(COFFRE DE BOIS|COFFRE DE FER|COFFRE RUNIQUE) disparu après 24 h$""")) { r, m ->
        val type = FRENCH_CHESTS[m.groupValues[1]] ?: "runique"
        r.getString(R.string.journal_chest_expired, chestType(r, type))
    },
    Rule(Regex("""^(Lame affûtée|Souffle ancien|Pas vifs) niveau (\d+)$""")) { r, m ->
        r.getString(
            R.string.journal_upgrade,
            r.getString(FRENCH_UPGRADES.getValue(m.groupValues[1])),
            m.groupValues[2].toInt()
        )
    },
    Rule(Regex("""^Équipement optimisé · (\d+) pièce\(s\)$""")) { r, m ->
        r.getString(R.string.journal_equip_optimized, m.groupValues[1].toInt())
    },
    Rule(Regex("""^(.+) vendu \(\+(\d+) or\)$""")) { r, m ->
        r.getString(
            R.string.journal_sold,
            itemNameFrom(r, m.groupValues[1]),
            m.groupValues[2].toInt()
        )
    },
    Rule(Regex("""^(\d+) objet\(s\) commun\(s\) vendu\(s\) · \+(\d+) or$""")) { r, m ->
        r.getString(
            R.string.journal_sold_commons,
            m.groupValues[1].toInt(),
            m.groupValues[2].toInt()
        )
    },
    Rule(Regex("""^(.+) verrouillé$""")) { r, m ->
        r.getString(R.string.journal_locked, itemNameFrom(r, m.groupValues[1]))
    },
    Rule(Regex("""^(.+) déverrouillé$""")) { r, m ->
        r.getString(R.string.journal_unlocked, itemNameFrom(r, m.groupValues[1]))
    },
    Rule(Regex("""^Objet identifié · (.+)$""")) { r, m ->
        r.getString(R.string.journal_identified, itemNameFrom(r, m.groupValues[1]))
    },
    Rule(Regex("""^Forge · (.+) → (.+)$""")) { r, m ->
        r.getString(
            R.string.journal_forge_effect,
            itemNameFrom(r, m.groupValues[1]),
            effectFrom(r, m.groupValues[2])
        )
    },
    Rule(Regex("""^Forge · (.+) niveau (\d+)$""")) { r, m ->
        r.getString(
            R.string.journal_forge_level,
            itemNameFrom(r, m.groupValues[1]),
            m.groupValues[2].toInt()
        )
    },
    Rule(Regex("""^(.+) démonté · \+(\d+) éclat\(s\)$""")) { r, m ->
        r.getString(
            R.string.journal_dismantled,
            itemNameFrom(r, m.groupValues[1]),
            m.groupValues[2].toInt()
        )
    },
    Rule(Regex("""^Fusion · (.+) \((.+)\)$""")) { r, m ->
        val rarity = FRENCH_RARITIES[m.groupValues[2]]?.let { r.getString(it) } ?: m.groupValues[2]
        r.getString(R.string.journal_fused, itemNameFrom(r, m.groupValues[1]), rarity)
    },
    Rule(Regex("""^Inventaire plein · (.+) au coffre, (.+) conservé$""")) { r, m ->
        r.getString(
            R.string.journal_stash_kept,
            itemNameFrom(r, m.groupValues[1]),
            itemNameFrom(r, m.groupValues[2])
        )
    },
    Rule(Regex("""^Inventaire plein · (.+) au coffre$""")) { r, m ->
        r.getString(R.string.journal_stash_full, itemNameFrom(r, m.groupValues[1]))
    },
    Rule(Regex("""^(\d+) objet\(s\) repris au coffre de camp$""")) { r, m ->
        r.getString(R.string.journal_stash_taken, m.groupValues[1].toInt())
    },
    Rule(Regex("""^Veillée gratuite \(\+(\d+) PV\)$""")) { r, m ->
        r.getString(R.string.journal_free_vigil, m.groupValues[1].toInt())
    },
    Rule(Regex("""^Repos au feu \(\+(\d+) PV\)$""")) { r, m ->
        r.getString(R.string.journal_campfire, m.groupValues[1].toInt())
    },
    Rule(Regex("""^Jalon · palier (\d+) — (.+)$""")) { r, m ->
        r.getString(
            R.string.journal_legend,
            m.groupValues[1].toInt(),
            rankTitleFrom(r, m.groupValues[2])
        )
    },
    Rule(Regex("""^(.+) acheté\.$""")) { r, m ->
        r.getString(R.string.msg_bought, itemNameFrom(r, m.groupValues[1]))
    },
    Rule(Regex("""^(.+) · passé sans s'arrêter$""")) { r, m ->
        r.getString(R.string.msg_passed, specialTitleFrom(r, m.groupValues[1]))
    },
    Rule(Regex("""^Aucun (.+)\.$""")) { r, m ->
        r.getString(R.string.msg_none_left, consumableNameFrom(r, m.groupValues[1]))
    },
    Rule(Regex("""^(.+) · utilisable en combat uniquement\.$""")) { r, m ->
        r.getString(R.string.msg_combat_only, consumableNameFrom(r, m.groupValues[1]))
    },
    Rule(Regex("""^\+(\d+) PV · \+(\d+) ferveur$""")) { r, m ->
        r.getString(R.string.msg_hp_fervour_gain, m.groupValues[1].toInt(), m.groupValues[2].toInt())
    },
    Rule(Regex("""^\+(\d+) PV · \+(\d+) ration$""")) { r, m ->
        r.getString(R.string.msg_hp_ration_gain, m.groupValues[1].toInt(), m.groupValues[2].toInt())
    },
    Rule(Regex("""^\+(\d+) PV$""")) { r, m ->
        r.getString(R.string.msg_hp_gain, m.groupValues[1].toInt())
    },
    Rule(Regex("""^\+(\d+) ferveur$""")) { r, m ->
        r.getString(R.string.msg_fervour_gain, m.groupValues[1].toInt())
    },
    Rule(Regex("""^(\d+) dégâts$""")) { r, m ->
        r.getString(R.string.msg_damage, m.groupValues[1].toInt())
    },
    Rule(Regex("""^ATQ \+(\d+) % · (\d+) s$""")) { r, m ->
        r.getString(R.string.msg_stat_atk, m.groupValues[1].toInt(), m.groupValues[2].toInt())
    },
    Rule(Regex("""^Dégâts −(\d+) % · (\d+) s$""")) { r, m ->
        r.getString(R.string.msg_stat_def, m.groupValues[1].toInt(), m.groupValues[2].toInt())
    },
    Rule(Regex("""^VIT \+(\d+) % · (\d+) s$""")) { r, m ->
        r.getString(R.string.msg_stat_vit, m.groupValues[1].toInt(), m.groupValues[2].toInt())
    },
    Rule(Regex("""^Critique \+(\d+) % · (\d+) s$""")) { r, m ->
        r.getString(R.string.msg_stat_crit, m.groupValues[1].toInt(), m.groupValues[2].toInt())
    },
    Rule(Regex("""^Vol de vie \+(\d+) % · (\d+) s$""")) { r, m ->
        r.getString(R.string.msg_stat_lifesteal, m.groupValues[1].toInt(), m.groupValues[2].toInt())
    },
    Rule(Regex("""^Régénération \+([\d,.]+) PV/s · (\d+) s$""")) { r, m ->
        r.getString(
            R.string.msg_stat_regen,
            normalizeDecimal(m.groupValues[1]),
            m.groupValues[2].toInt()
        )
    },
    Rule(Regex("""^ATQ \+(\d+) % · vulnérable$""")) { r, m ->
        r.getString(R.string.msg_berserk, m.groupValues[1].toInt())
    },
    // Dernière règle : nom de créature suivi d'une capacité de gardien.
    Rule(Regex("""^(.+) · (.+)$""")) { r, m ->
        val ability = FRENCH_ABILITIES[m.groupValues[2]]
        if (ability == null) m.value
        else r.getString(
            R.string.journal_ability,
            creatureDisplay(r, m.groupValues[1]),
            r.getString(ability)
        )
    },
)

fun translateText(res: Resources, raw: String): String {
    if (isFrench(res)) return raw
    FIXED[raw]?.let { return res.getString(it) }
    for (rule in RULES) {
        val match = rule.regex.matchEntire(raw) ?: continue
        return rule.resolve(res, match)
    }
    return raw
}

@Composable
fun localizedText(raw: String): String = translateText(LocalContext.current.resources, raw)

@Composable
fun statLineRes(item: Item): String = statLineRes(LocalContext.current.resources, item)

@Composable
fun localizedItem(item: Item): String =
    itemName(LocalContext.current.resources, item.name, item.effect)

@Composable
fun localizedItemName(raw: String): String =
    itemNameFrom(LocalContext.current.resources, raw)

@Composable
fun localizedCreature(raw: String): String =
    creatureDisplay(LocalContext.current.resources, raw)

@Composable
fun localizedTier(raw: String): String = tierRes(LocalContext.current.resources, raw)

@Composable
fun localizedZoneName(raw: String): String = zoneRes(LocalContext.current.resources, raw)

@Composable
fun localizedSetBonus(text: String): String =
    setBonusRes(LocalContext.current.resources, text)

@Composable
fun localizedBiomeName(raw: String): String =
    biomeNameRes(LocalContext.current.resources, raw)

@Composable
fun localizedBiomeText(raw: String): String {
    var out = raw
    for ((fr, key) in FRENCH_BIOMES) {
        out = out.replace(fr, LocalContext.current.resources.getString(key))
    }
    return out
}
