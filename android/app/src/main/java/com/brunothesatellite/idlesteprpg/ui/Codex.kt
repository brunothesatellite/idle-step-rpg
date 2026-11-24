package com.brunothesatellite.idlesteprpg.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.brunothesatellite.idlesteprpg.R
import com.brunothesatellite.idlesteprpg.engine.BIOMES
import com.brunothesatellite.idlesteprpg.engine.BIOME_POOLS
import com.brunothesatellite.idlesteprpg.engine.BiomeRule
import com.brunothesatellite.idlesteprpg.engine.BossTemplate
import com.brunothesatellite.idlesteprpg.engine.GameState
import com.brunothesatellite.idlesteprpg.engine.ITEM_SETS
import com.brunothesatellite.idlesteprpg.engine.MobTemplate
import com.brunothesatellite.idlesteprpg.engine.SPECIAL_BOSSES
import com.brunothesatellite.idlesteprpg.engine.allBossTemplates
import com.brunothesatellite.idlesteprpg.engine.allMobTemplates
import com.brunothesatellite.idlesteprpg.engine.biomeKeyFor
import com.brunothesatellite.idlesteprpg.engine.biomeRuleFor
import com.brunothesatellite.idlesteprpg.engine.bossAbilityFor
import com.brunothesatellite.idlesteprpg.engine.discoveredItemNames
import com.brunothesatellite.idlesteprpg.engine.discoveredSetSlots
import com.brunothesatellite.idlesteprpg.engine.equippedSetSlots
import com.brunothesatellite.idlesteprpg.engine.lootTemplatesFor
import com.brunothesatellite.idlesteprpg.engine.setBonusDescription
import com.brunothesatellite.idlesteprpg.engine.setLabel
import com.brunothesatellite.idlesteprpg.engine.setSourceLabel
import com.brunothesatellite.idlesteprpg.engine.slotLabel
import com.brunothesatellite.idlesteprpg.engine.specialBossFloor
import java.util.Locale
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sqrt

@Composable
fun CodexOverlay(state: GameState, onClose: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf("bestiary") }
    Column(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B0810), Color(0xFF171121), Color(0xFF0B0810))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("CODEX", fontFamily = Press, fontSize = 10.sp, color = GoldLight)
                Text(stringResource(R.string.codex_subtitle), fontSize = 7.sp, color = Muted)
            }
            CloseButton(onClose)
        }
        DividerLine()
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            CodexTab("bestiary", stringResource(R.string.codex_bestiary), tab, Modifier.weight(1f)) { tab = it }
            CodexTab("sets", stringResource(R.string.codex_tab_sets), tab, Modifier.weight(1f)) { tab = it }
            CodexTab("biomes", stringResource(R.string.codex_biomes), tab, Modifier.weight(1f)) { tab = it }
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            when (tab) {
                "sets" -> SetsCodex(state)
                "biomes" -> BiomesCodex(state)
                else -> BestiaryCodex(state)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CodexTab(
    id: String,
    label: String,
    active: String,
    modifier: Modifier = Modifier,
    onClick: (String) -> Unit
) {
    val selected = active == id
    Box(
        modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (selected) Gold.copy(alpha = 0.18f) else Panel2)
            .border(1.dp, if (selected) Gold.copy(alpha = 0.7f) else Line2, RoundedCornerShape(4.dp))
            .clickable { onClick(id) }
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 6.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) GoldLight else Muted,
            maxLines = 1
        )
    }
}

@Composable
private fun BestiaryCodex(state: GameState) {
    var selectedMob by remember { mutableStateOf<MobTemplate?>(null) }
    var selectedBoss by remember { mutableStateOf<BossTemplate?>(null) }
    val mobs = allMobTemplates()
    val bosses = allBossTemplates()
    val discoveredMobs = mobs.count { (state.bestiary[it.id] ?: 0) > 0 }
    val discoveredBosses = bosses.count { (state.bossKills[it.id] ?: 0) > 0 }
    PixelCard(Modifier.fillMaxWidth(), borderColor = Gold.copy(alpha = 0.3f)) {
        SectionLabel(stringResource(R.string.codex_bestiary))
        Text(
            stringResource(
                R.string.codex_count,
                discoveredMobs,
                mobs.size,
                discoveredBosses,
                bosses.size
            ),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = GoldLight
        )
        Text(
            stringResource(R.string.codex_intro1) + " " + stringResource(R.string.codex_intro2),
            fontSize = 6.3.sp,
            color = Muted,
            lineHeight = 9.sp
        )
    }
    Spacer(Modifier.height(8.dp))
    BIOME_POOLS.forEach { (key, pool) ->
        val biome = BIOMES.firstOrNull { it.key == key } ?: return@forEach
        val unlocked = state.floor >= biome.fromFloor
        val biomeMobs = (pool.easy + pool.mid + pool.hard).distinctBy { it.name }
        val biomeBosses = pool.bosses.distinctBy { it.name }
        val foundMobs = biomeMobs.count { (state.bestiary[it.id] ?: 0) > 0 }
        val foundBosses = biomeBosses.count { (state.bossKills[it.id] ?: 0) > 0 }
        PixelCard(
            Modifier.fillMaxWidth(),
            borderColor = if (unlocked) Line else Line2,
            background = if (unlocked) Panel2 else Panel
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    SectionLabel(biomeLabelRes(biome.key))
                    Text(
                        stringResource(
                            R.string.codex_count,
                            foundMobs,
                            biomeMobs.size,
                            foundBosses,
                            biomeBosses.size
                        ),
                        fontSize = 6.5.sp,
                        color = Muted
                    )
                }
                if (!unlocked) {
                    Text(
                        stringResource(R.string.codex_undiscovered),
                        fontFamily = Press,
                        fontSize = 5.sp,
                        color = Muted2,
                        letterSpacing = 0.4.sp
                    )
                } else if (biomeKeyFor(state.floor) == key) {
                    Text(stringResource(R.string.codex_current), fontFamily = Press, fontSize = 5.5.sp, color = GoldLight)
                }
            }
            Spacer(Modifier.height(6.dp))
            biomeMobs.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEach { mob ->
                        MobEntry(state, mob, Modifier.weight(1f)) { selectedMob = mob }
                    }
                    repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(4.dp))
            }
            if (biomeBosses.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(stringResource(R.string.codex_guardians), fontSize = 5.5.sp, color = Blood, letterSpacing = 0.8.sp)
                biomeBosses.forEach { boss ->
                    BossEntry(state, boss, biome.fromFloor) { selectedBoss = boss }
                }
            }
        }
        Spacer(Modifier.height(7.dp))
    }
    val milestoneBosses = SPECIAL_BOSSES.filter { special ->
        BIOME_POOLS.values.none { pool -> pool.bosses.any { it.name == special.name } }
    }
    if (milestoneBosses.isNotEmpty()) {
        PixelCard(
            Modifier.fillMaxWidth(),
            borderColor = Blood.copy(alpha = 0.35f),
            background = Color(0xFF20121B)
        ) {
            SectionLabel(stringResource(R.string.codex_milestone_guardians))
            Text(
                stringResource(R.string.codex_milestone_note),
                fontSize = 6.2.sp,
                color = Muted,
                lineHeight = 9.sp
            )
            Spacer(Modifier.height(5.dp))
            milestoneBosses.forEach { boss ->
                BossEntry(state, boss, specialBossFloor(boss.name) ?: 1) { selectedBoss = boss }
            }
        }
        Spacer(Modifier.height(7.dp))
    }
    selectedMob?.let { mob ->
        MobDetailDialog(state, mob) { selectedMob = null }
    }
    selectedBoss?.let { boss ->
        BossDetailDialog(state, boss) { selectedBoss = null }
    }
}

@Composable
private fun MobEntry(
    state: GameState,
    mob: MobTemplate,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val kills = state.bestiary[mob.id] ?: 0
    val known = kills > 0
    Row(
        modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Panel)
            .border(1.dp, if (known) Green.copy(alpha = 0.35f) else Line, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (known) {
            AssetImage(mob.icon, Modifier.size(24.dp))
        } else {
            Box(
                Modifier.size(24.dp).clip(RoundedCornerShape(3.dp)).background(Panel3),
                contentAlignment = Alignment.Center
            ) {
                Text("?", fontFamily = Press, fontSize = 8.sp, color = Muted2)
            }
        }
        Spacer(Modifier.width(5.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (known) creatureNameRes(mob.name) else "???",
                fontSize = 6.2.sp,
                color = if (known) Parchment else Muted2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                if (known) stringResource(R.string.codex_kills, kills) else stringResource(R.string.codex_not_met),
                fontSize = 5.2.sp,
                color = if (known) Green else Muted2
            )
        }
        Spacer(Modifier.width(3.dp))
        Text("›", fontSize = 11.sp, color = if (known) Gold else Muted2)
    }
}

private data class MobPreview(
    val biomeName: String,
    val rule: BiomeRule,
    val floor: Int,
    val level: Int,
    val hp: Int,
    val atk: Int,
    val intervalMs: Long,
    val gold: Int,
    val xp: Int,
    val tier: String,
    val zone: String
)

private fun buildMobPreview(state: GameState, mob: MobTemplate): MobPreview {
    val entry = BIOME_POOLS.entries.firstOrNull { (_, pool) ->
        (pool.easy + pool.mid + pool.hard).any { it.name == mob.name }
    }
    val pool = entry?.value
    val poolTier = when {
        pool == null -> "easy"
        pool.easy.any { it.name == mob.name } -> "easy"
        pool.mid.any { it.name == mob.name } -> "mid"
        else -> "hard"
    }
    val seen = state.firstSeen[mob.id]
    val key = seen?.biomeKey ?: entry?.key ?: biomeKeyFor(state.floor)
    val biome = BIOMES.firstOrNull { it.key == key }
    val floor = seen?.floor ?: (if (key == biomeKeyFor(state.floor)) state.floor else (biome?.fromFloor ?: state.floor))
    val tier = seen?.tier ?: poolTier
    val rule = biomeRuleFor(floor)
    val level = seen?.level ?: (floor + when (tier) {
        "easy", "farm" -> -1
        "mid" -> 0
        else -> 1
    }).coerceAtLeast(1)
    val depth = if (floor <= 10) 1.0 else min(1.12.pow(floor - 10.0), 40.0)
    val hp = ((55 + 34 * (level - 1)) * depth * rule.enemyHpMult).roundToInt()
    val atk = ((6 + 2.2 * (level - 1)) * depth * rule.enemyAtkMult).roundToInt()
    val interval = (1250 * rule.enemyIntervalMult).roundToLong()
    val reward = min(6.0, sqrt(depth))
    val gold = ((6 + 3 * level) * reward).roundToInt()
    val xp = ((12 + 5 * level) * reward).roundToInt()
    val zone = when (tier) {
        "easy" -> "0–50 m"
        "mid" -> "50–85 m"
        "farm" -> "palier verrouillé"
        else -> "85–100 m"
    }
    val tierLabel = when (tier) {
        "easy" -> "DÉBUT DE PALIER"
        "mid" -> "MILIEU DE PALIER"
        "farm" -> "FARM"
        else -> "FIN DE PALIER"
    }
    return MobPreview(
        biomeName = biome?.name ?: "RÉGION INCONNUE",
        rule = rule,
        floor = floor,
        level = level,
        hp = hp,
        atk = atk,
        intervalMs = interval,
        gold = gold,
        xp = xp,
        tier = tierLabel,
        zone = zone
    )
}

@Composable
private fun MobStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    color: Color = GoldLight
) {
    Column(
        modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Panel)
            .border(1.dp, Line, RoundedCornerShape(4.dp))
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontFamily = Press, fontSize = 9.sp, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, fontSize = 5.sp, color = Muted, letterSpacing = 0.4.sp, maxLines = 1)
    }
}

@Composable
private fun MobDetailDialog(state: GameState, mob: MobTemplate, onDismiss: () -> Unit) {
    val kills = state.bestiary[mob.id] ?: 0
    val known = kills > 0
    val preview = remember(mob.id, state.floor, kills) { buildMobPreview(state, mob) }
    AppDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxWidth().padding(22.dp), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 620.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF241930), Color(0xFF130E1C))))
                    .border(1.dp, Gold.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.codex_bestiary), fontFamily = Press, fontSize = 6.5.sp, color = Muted)
                        Text(
                            if (known) creatureNameRes(mob.name) else stringResource(R.string.codex_unknown_creature),
                            fontFamily = Press,
                            fontSize = 10.sp,
                            color = if (known) GoldLight else Muted2,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    CloseButton(onDismiss)
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Panel2)
                        .border(
                            1.dp,
                            if (known) Green.copy(alpha = 0.4f) else Line,
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (known) {
                        AssetImage(mob.icon, Modifier.size(84.dp), contentDescription = creatureNameRes(mob.name))
                    } else {
                        Text("?", fontFamily = Press, fontSize = 30.sp, color = Muted2)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    localizedBiomeName(preview.biomeName),
                    fontFamily = Press,
                    fontSize = 7.sp,
                    color = Gold,
                    textAlign = TextAlign.Center
                )
                Text(
                    stringResource(
                        R.string.codex_preview_line,
                        localizedTier(preview.tier),
                        localizedZoneName(preview.zone),
                        preview.floor,
                        preview.level
                    ),
                    fontSize = 6.sp,
                    color = Muted,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                if (!known) {
                    Text(
                        stringResource(R.string.codex_mob_unknown1) + " " + stringResource(R.string.codex_mob_unknown2),
                        fontSize = 7.sp,
                        color = Muted,
                        textAlign = TextAlign.Center,
                        lineHeight = 10.sp
                    )
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        MobStat(stringResource(R.string.overlay_hp), formatCompact(preview.hp), Modifier.weight(1f))
                        MobStat(stringResource(R.string.overlay_atk), formatCompact(preview.atk), Modifier.weight(1f), Ember)
                        MobStat(
                            stringResource(R.string.codex_cadence),
                            decimalRes(preview.intervalMs / 1000.0, 2) + " s",
                            Modifier.weight(1f),
                            Color(0xFF86B8D9)
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        MobStat(stringResource(R.string.overlay_gold), formatCompact(preview.gold), Modifier.weight(1f))
                        MobStat(stringResource(R.string.codex_xp), formatCompact(preview.xp), Modifier.weight(1f), Color(0xFF8FD39B))
                        MobStat(stringResource(R.string.overlay_recap_kills), formatCompact(kills), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Panel.copy(alpha = 0.7f))
                            .border(1.dp, Line2, RoundedCornerShape(6.dp))
                            .padding(9.dp)
                    ) {
                        Column {
                            SectionLabel(stringResource(R.string.codex_biome_rule), color = Ember)
                            Text(
                                biomeRuleLabelRes(preview.rule.id),
                                fontSize = 7.sp,
                                color = GoldLight,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(biomeRuleDescRes(preview.rule.id), fontSize = 6.3.sp, color = Muted, lineHeight = 9.sp)
                        }
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(
                        if (state.firstSeen.containsKey(mob.id)) {
                            stringResource(R.string.codex_stats_seen, preview.floor)
                        } else {
                            stringResource(R.string.codex_stats_base, preview.floor)
                        },
                        fontSize = 5.6.sp,
                        color = Muted2,
                        textAlign = TextAlign.Center,
                        lineHeight = 8.sp
                    )
                }
                Spacer(Modifier.height(12.dp))
                GoldButton(if (known) stringResource(R.string.overlay_close) else stringResource(R.string.codex_understood), onClick = onDismiss)
            }
        }
    }
}

private data class BossPreview(
    val biomeName: String,
    val rule: BiomeRule,
    val floor: Int,
    val level: Int,
    val hp: Int,
    val atk: Int,
    val intervalMs: Long,
    val gold: Int,
    val xp: Int,
    val abilityLabel: String,
    val abilityId: String
)

private fun buildBossPreview(state: GameState, boss: BossTemplate): BossPreview {
    val entry = BIOME_POOLS.entries.firstOrNull { (_, pool) ->
        pool.bosses.any { it.name == boss.name }
    }
    val seen = state.bossFirstSeen[boss.id]
    val special = specialBossFloor(boss.name)
    val referenceFloor = seen?.floor ?: special ?: entry?.let { (key, _) ->
        BIOMES.firstOrNull { it.key == key }?.fromFloor
    } ?: state.floor
    val key = seen?.biomeKey ?: entry?.key ?: biomeKeyFor(referenceFloor)
    val biome = BIOMES.firstOrNull { it.key == key }
    val floor = referenceFloor
    val rule = biomeRuleFor(floor)
    val level = seen?.level ?: (floor + 2)
    val depth = if (floor <= 10) 1.0 else min(1.08.pow(floor - 10.0), 40.0)
    val ability = bossAbilityFor(boss.name, floor)
    val reward = min(10.0, sqrt(depth))
    return BossPreview(
        biomeName = biome?.name ?: "RÉGION INCONNUE",
        rule = rule,
        floor = floor,
        level = level,
        hp = (6.0 * (55 + 34 * (level - 1)) * depth * rule.enemyHpMult).roundToInt(),
        atk = (1.6 * (6 + 2.2 * (level - 1)) * depth * rule.enemyAtkMult).roundToInt(),
        intervalMs = (1250 * rule.enemyIntervalMult).roundToLong(),
        gold = (12 * (6 + 3 * level) * reward).roundToInt(),
        xp = (10 * (12 + 5 * level) * reward).roundToInt(),
        abilityLabel = ability.label,
        abilityId = ability.id
    )
}

@Composable
private fun bossAbilityDescription(id: String): String = when (id) {
    "slam" -> stringResource(R.string.codex_ability_slam)
    "drain" -> stringResource(R.string.codex_ability_drain)
    "regen" -> stringResource(R.string.codex_ability_regen)
    "shield" -> stringResource(R.string.codex_ability_shield)
    "breath" -> stringResource(R.string.codex_ability_breath)
    "roots" -> stringResource(R.string.codex_ability_roots)
    "acid" -> stringResource(R.string.codex_ability_acid)
    "pressure" -> stringResource(R.string.codex_ability_pressure)
    "rupture" -> stringResource(R.string.codex_ability_rupture)
    else -> stringResource(R.string.codex_ability_hit)
}

@Composable
private fun BossDetailDialog(state: GameState, boss: BossTemplate, onDismiss: () -> Unit) {
    val kills = state.bossKills[boss.id] ?: 0
    val known = kills > 0
    val preview = remember(boss.id, state.floor, kills) { buildBossPreview(state, boss) }
    AppDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxWidth().padding(22.dp), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 640.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF2A1620), Color(0xFF140B12))))
                    .border(1.dp, Blood.copy(alpha = 0.75f), RoundedCornerShape(12.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.codex_guardian_title), fontFamily = Press, fontSize = 6.5.sp, color = Muted)
                        Text(
                            if (known) creatureNameRes(boss.name) else stringResource(R.string.codex_unknown_guardian),
                            fontFamily = Press,
                            fontSize = 10.sp,
                            color = if (known) GoldLight else Muted2,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    CloseButton(onDismiss)
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .size(104.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF20121B))
                        .border(
                            1.dp,
                            if (known) Blood.copy(alpha = 0.6f) else Line,
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (known) {
                        AssetImage(boss.icon, Modifier.size(92.dp), contentDescription = creatureNameRes(boss.name))
                    } else {
                        Text("?", fontFamily = Press, fontSize = 30.sp, color = Muted2)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    localizedBiomeName(preview.biomeName),
                    fontFamily = Press,
                    fontSize = 7.sp,
                    color = Blood,
                    textAlign = TextAlign.Center
                )
                Text(
                    stringResource(R.string.codex_guardian_header, preview.floor, preview.level),
                    fontSize = 6.sp,
                    color = Muted,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                if (!known) {
                    Text(
                        stringResource(R.string.codex_boss_unknown1) + " " + stringResource(R.string.codex_boss_unknown2),
                        fontSize = 7.sp,
                        color = Muted,
                        textAlign = TextAlign.Center,
                        lineHeight = 10.sp
                    )
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        MobStat(stringResource(R.string.overlay_hp), formatCompact(preview.hp), Modifier.weight(1f))
                        MobStat(stringResource(R.string.overlay_atk), formatCompact(preview.atk), Modifier.weight(1f), Blood)
                        MobStat(
                            stringResource(R.string.codex_cadence),
                            decimalRes(preview.intervalMs / 1000.0, 2) + " s",
                            Modifier.weight(1f),
                            Color(0xFF86B8D9)
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        MobStat(stringResource(R.string.overlay_gold), formatCompact(preview.gold), Modifier.weight(1f))
                        MobStat(stringResource(R.string.codex_xp), formatCompact(preview.xp), Modifier.weight(1f), Color(0xFF8FD39B))
                        MobStat(stringResource(R.string.overlay_recap_kills), formatCompact(kills), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF20121B))
                            .border(1.dp, Blood.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                            .padding(9.dp)
                    ) {
                        Column {
                            SectionLabel(stringResource(R.string.codex_ability), color = Blood)
                            Text(
                                abilityLabelRes(preview.abilityId),
                                fontSize = 7.sp,
                                color = GoldLight,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                bossAbilityDescription(preview.abilityId),
                                fontSize = 6.3.sp,
                                color = Muted,
                                lineHeight = 9.sp
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(stringResource(R.string.codex_ability_every), fontSize = 5.6.sp, color = Muted2)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Panel.copy(alpha = 0.7f))
                            .border(1.dp, Line2, RoundedCornerShape(6.dp))
                            .padding(9.dp)
                    ) {
                        Column {
                            SectionLabel(stringResource(R.string.codex_biome_rule), color = Ember)
                            Text(
                                biomeRuleLabelRes(preview.rule.id),
                                fontSize = 7.sp,
                                color = GoldLight,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(biomeRuleDescRes(preview.rule.id), fontSize = 6.3.sp, color = Muted, lineHeight = 9.sp)
                        }
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(
                        (if (state.bossFirstSeen.containsKey(boss.id)) {
                            stringResource(R.string.codex_boss_seen1, preview.floor)
                        } else {
                            stringResource(R.string.codex_boss_stats1, preview.floor)
                        }) + " " + stringResource(R.string.codex_boss_stats2) + " " +
                            stringResource(R.string.codex_boss_stats3) + " " +
                            stringResource(R.string.codex_boss_stats4),
                        fontSize = 5.6.sp,
                        color = Muted2,
                        textAlign = TextAlign.Center,
                        lineHeight = 8.sp
                    )
                }
                Spacer(Modifier.height(12.dp))
                GoldButton(if (known) stringResource(R.string.overlay_close) else stringResource(R.string.codex_understood), onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun BossEntry(
    state: GameState,
    boss: BossTemplate,
    floor: Int,
    onClick: () -> Unit = {}
) {
    val kills = state.bossKills[boss.id] ?: 0
    val known = kills > 0
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 3.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF20121B))
            .border(1.dp, if (known) Blood.copy(alpha = 0.55f) else Line, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (known) {
            AssetImage(boss.icon, Modifier.size(26.dp))
        } else {
            Box(
                Modifier.size(26.dp).clip(RoundedCornerShape(3.dp)).background(Panel3),
                contentAlignment = Alignment.Center
            ) {
                Text("?", fontFamily = Press, fontSize = 8.sp, color = Muted2)
            }
        }
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (known) creatureNameRes(boss.name) else stringResource(R.string.codex_unknown_guardian),
                fontSize = 6.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (known) GoldLight else Muted2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                stringResource(
                    R.string.codex_ability_line,
                    abilityLabelRes(bossAbilityFor(boss.name, floor).id)
                ),
                fontSize = 5.2.sp,
                color = if (known) Ember else Muted2,
                maxLines = 1
            )
        }
        Text(
            stringResource(R.string.codex_guardian_badge),
            fontFamily = Press,
            fontSize = 4.6.sp,
            color = Blood,
            modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .background(Blood.copy(alpha = 0.10f))
                .border(1.dp, Blood.copy(alpha = 0.35f), RoundedCornerShape(3.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            if (known) "$kills×" else "—",
            fontSize = 6.sp,
            color = if (known) Gold else Muted2
        )
        Spacer(Modifier.width(4.dp))
        Text("›", fontSize = 11.sp, color = if (known) Blood else Muted2)
    }
}

@Composable
private fun SetsCodex(state: GameState) {
    val completed = ITEM_SETS.count { discoveredSetSlots(state, it).size >= 6 }
    val started = ITEM_SETS.count { discoveredSetSlots(state, it).isNotEmpty() }
    PixelCard(Modifier.fillMaxWidth(), borderColor = Gold.copy(alpha = 0.3f)) {
        SectionLabel(stringResource(R.string.codex_sets))
        Text(
            stringResource(R.string.codex_sets_count, completed, ITEM_SETS.size, started),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = GoldLight
        )
        Text(
            stringResource(R.string.codex_set_note1) + " " +
                stringResource(R.string.codex_set_note2),
            fontSize = 6.3.sp,
            color = Muted,
            lineHeight = 9.sp
        )
    }
    Spacer(Modifier.height(8.dp))
    ITEM_SETS.forEach { set ->
        val collected = discoveredSetSlots(state, set)
        val equipped = equippedSetSlots(state, set)
        val collectedCount = collected.size
        val equippedCount = equipped.size
        val next = when {
            equippedCount < 2 -> 2
            equippedCount < 4 -> 4
            else -> 6
        }
        PixelCard(
            Modifier.fillMaxWidth(),
            borderColor = when {
                equippedCount >= 2 -> Gold.copy(alpha = 0.45f)
                collectedCount >= 2 -> Gold.copy(alpha = 0.22f)
                else -> Line
            }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    SectionLabel(setLabelRes(set))
                    Text(
                        stringResource(R.string.codex_set_line, collectedCount, equippedCount),
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (collectedCount >= 2) GoldLight else Muted
                    )
                }
                Text(
                    when {
                        equippedCount >= 2 -> stringResource(R.string.codex_set_active)
                        collectedCount >= 2 -> stringResource(R.string.codex_set_equip)
                        else -> stringResource(R.string.codex_set_collect)
                    },
                    fontFamily = Press,
                    fontSize = 5.sp,
                    color = when {
                        equippedCount >= 2 -> Green
                        collectedCount >= 2 -> Gold
                        else -> Muted2
                    }
                )
            }
            Spacer(Modifier.height(5.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                listOf("weapon", "head", "body", "hands", "feet", "talisman").forEach { slot ->
                    val has = slot in collected
                    val on = slot in equipped
                    Box(
                        Modifier
                            .weight(1f)
                            .heightIn(min = 28.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                when {
                                    on -> Gold.copy(alpha = 0.28f)
                                    has -> Gold.copy(alpha = 0.10f)
                                    else -> Panel3
                                }
                            )
                            .border(
                                1.dp,
                                when {
                                    on -> Gold
                                    has -> Gold.copy(alpha = 0.45f)
                                    else -> Line
                                },
                                RoundedCornerShape(3.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            slotLabelRes(slot).take(4).uppercase(),
                            fontSize = 4.5.sp,
                            color = when {
                                on -> GoldLight
                                has -> Gold
                                else -> Muted2
                            },
                            maxLines = 1
                        )
                    }
                }
            }
            if (collectedCount >= 1) {
                Spacer(Modifier.height(4.dp))
                Text(
                    if (equippedCount >= 2) localizedSetBonus(setBonusDescription(set, equippedCount))
                    else stringResource(
                        R.string.overlay_set_two,
                        localizedSetBonus(setBonusDescription(set, 2))
                    ),
                    fontSize = 6.2.sp,
                    color = if (equippedCount >= 2) Green else Muted
                )
            }
            if (equippedCount in 2..5) {
                Text(
                    stringResource(
                        R.string.codex_set_next,
                        next,
                        localizedSetBonus(setBonusDescription(set, next))
                    ),
                    fontSize = 5.6.sp,
                    color = Muted,
                    lineHeight = 8.sp
                )
            } else if (equippedCount >= 6) {
                Text(stringResource(R.string.codex_set_max), fontSize = 5.6.sp, color = Green)
            }
            val source = setSourceLabel(set)
            if (source.isNotBlank()) {
                Text(
                    stringResource(R.string.codex_set_source, localizedBiomeText(setSourceLabel(set))),
                    fontSize = 5.6.sp,
                    color = Muted2,
                    lineHeight = 8.sp
                )
            }
        }
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun BiomeBanner(key: String, modifier: Modifier = Modifier) {
    val visuals = biomeVisuals(key)
    val palette = biomePalette(key)
    val walls = rememberAssets(visuals.walls)
    val floors = rememberAssets(visuals.floors)
    val statues = rememberAssets(visuals.statues)
    val props = rememberAssets(visuals.props)
    val fireCloud = rememberAsset("fx/cloud_fire0.png")

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(74.dp)
            .clip(RoundedCornerShape(5.dp))
    ) {
        // MÃªme dessin que la scÃ¨ne : le bandeau est une miniature du couloir.
        drawCorridor(palette, walls, floors, statues, props, fireCloud, visuals.fireClouds)
    }
}


@Composable
private fun BiomesCodex(state: GameState) {
    val visited = BIOMES.count { state.floor >= it.fromFloor }
    PixelCard(Modifier.fillMaxWidth(), borderColor = Green.copy(alpha = 0.3f)) {
        SectionLabel(stringResource(R.string.codex_biomes))
        Text(
            stringResource(R.string.codex_biomes_count, visited, BIOMES.size),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = GoldLight
        )
        Text(
            stringResource(R.string.codex_biomes_note),
            fontSize = 6.3.sp,
            color = Muted
        )
    }
    Spacer(Modifier.height(8.dp))
    BIOMES.forEach { biome ->
        val known = state.floor >= biome.fromFloor
        val pool = BIOME_POOLS[biome.key]
        val mobs = pool?.let { (it.easy + it.mid + it.hard).distinctBy { mob -> mob.name } } ?: emptyList()
        val bosses = pool?.bosses?.distinctBy { it.name } ?: emptyList()
        val items = lootTemplatesFor(biome.fromFloor).distinctBy { it.name }
        val foundMobs = mobs.count { (state.bestiary[it.id] ?: 0) > 0 }
        val foundBosses = bosses.count { (state.bossKills[it.id] ?: 0) > 0 }
        val foundItems = items.count { it.name in discoveredItemNames(state) }
        val rule = biomeRuleFor(biome.fromFloor)
        PixelCard(
            Modifier.fillMaxWidth(),
            borderColor = if (known) Green.copy(alpha = 0.3f) else Line,
            background = if (known) Panel2 else Panel
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    SectionLabel(biomeLabelRes(biome.key))
                    Text(
                        if (known) biomeRuleLabelRes(rule.id) else stringResource(R.string.codex_biome_locked, biome.fromFloor),
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (known) Green else Muted2
                    )
                }
                if (biomeKeyFor(state.floor) == biome.key) {
                    Text(stringResource(R.string.codex_current), fontFamily = Press, fontSize = 5.5.sp, color = GoldLight)
                }
            }
            if (known) {
                Spacer(Modifier.height(5.dp))
                BiomeBanner(biome.key)
                Spacer(Modifier.height(5.dp))
                Text(biomeRuleDescRes(rule.id), fontSize = 6.2.sp, color = Muted, lineHeight = 9.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(
                        R.string.codex_biome_counts,
                        foundMobs,
                        mobs.size,
                        foundBosses,
                        bosses.size,
                        foundItems,
                        items.size
                    ),
                    fontSize = 6.sp,
                    color = Parchment
                )
            }
        }
        Spacer(Modifier.height(6.dp))
    }
}
