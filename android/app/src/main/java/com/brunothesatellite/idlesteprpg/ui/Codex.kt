package com.brunothesatellite.idlesteprpg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brunothesatellite.idlesteprpg.engine.BIOMES
import com.brunothesatellite.idlesteprpg.engine.BIOME_POOLS
import com.brunothesatellite.idlesteprpg.engine.BossTemplate
import com.brunothesatellite.idlesteprpg.engine.GameState
import com.brunothesatellite.idlesteprpg.engine.ITEM_SETS
import com.brunothesatellite.idlesteprpg.engine.MobTemplate
import com.brunothesatellite.idlesteprpg.engine.allBossTemplates
import com.brunothesatellite.idlesteprpg.engine.allMobTemplates
import com.brunothesatellite.idlesteprpg.engine.biomeKeyFor
import com.brunothesatellite.idlesteprpg.engine.biomeRuleFor
import com.brunothesatellite.idlesteprpg.engine.bossAbilityFor
import com.brunothesatellite.idlesteprpg.engine.discoveredItemNames
import com.brunothesatellite.idlesteprpg.engine.lootTemplatesFor
import com.brunothesatellite.idlesteprpg.engine.ownedSetSlots
import com.brunothesatellite.idlesteprpg.engine.setBonusDescription
import com.brunothesatellite.idlesteprpg.engine.setLabel
import com.brunothesatellite.idlesteprpg.engine.slotLabel

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
                Text("Bestiaire, ensembles et biomes", fontSize = 7.sp, color = Muted)
            }
            Text(
                "×",
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Panel2)
                    .border(1.dp, Line2, RoundedCornerShape(4.dp))
                    .clickable(onClick = onClose)
                    .padding(top = 3.dp),
                textAlign = TextAlign.Center,
                fontSize = 18.sp,
                color = Muted
            )
        }
        DividerLine()
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            CodexTab("bestiary", "BESTIAIRE", tab, Modifier.weight(1f)) { tab = it }
            CodexTab("sets", "ENSEMBLES", tab, Modifier.weight(1f)) { tab = it }
            CodexTab("biomes", "BIOMES", tab, Modifier.weight(1f)) { tab = it }
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
    val mobs = allMobTemplates()
    val bosses = allBossTemplates()
    val discoveredMobs = mobs.count { (state.bestiary[it.name] ?: 0) > 0 }
    val discoveredBosses = bosses.count { (state.bossKills[it.name] ?: 0) > 0 }
    PixelCard(Modifier.fillMaxWidth(), borderColor = Gold.copy(alpha = 0.3f)) {
        SectionLabel("BESTIAIRE")
        Text(
            "$discoveredMobs / ${mobs.size} créatures · $discoveredBosses / ${bosses.size} gardiens",
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = GoldLight
        )
        Text(
            "Les silhouettes inconnues se révèlent au premier combat ou à la première victoire.",
            fontSize = 6.3.sp,
            color = Muted,
            lineHeight = 9.sp
        )
    }
    Spacer(Modifier.height(8.dp))
    BIOME_POOLS.forEach { (key, pool) ->
        val biome = BIOMES.firstOrNull { it.key == key } ?: return@forEach
        val biomeMobs = (pool.easy + pool.mid + pool.hard).distinctBy { it.name }
        val biomeBosses = pool.bosses.distinctBy { it.name }
        val foundMobs = biomeMobs.count { (state.bestiary[it.name] ?: 0) > 0 }
        val foundBosses = biomeBosses.count { (state.bossKills[it.name] ?: 0) > 0 }
        PixelCard(Modifier.fillMaxWidth(), background = Panel2) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    SectionLabel(biome.name)
                    Text(
                        "$foundMobs / ${biomeMobs.size} créatures · $foundBosses / ${biomeBosses.size} gardiens",
                        fontSize = 6.5.sp,
                        color = Muted
                    )
                }
                if (biomeKeyFor(state.floor) == key) {
                    Text("ACTUEL", fontFamily = Press, fontSize = 5.5.sp, color = GoldLight)
                }
            }
            Spacer(Modifier.height(6.dp))
            biomeMobs.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEach { mob ->
                        MobEntry(state, mob, Modifier.weight(1f))
                    }
                    repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(4.dp))
            }
            if (biomeBosses.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text("GARDIENS", fontSize = 5.5.sp, color = Blood, letterSpacing = 0.8.sp)
                biomeBosses.forEach { boss ->
                    BossEntry(state, boss, biome.fromFloor)
                }
            }
        }
        Spacer(Modifier.height(7.dp))
    }
}

@Composable
private fun MobEntry(state: GameState, mob: MobTemplate, modifier: Modifier = Modifier) {
    val kills = state.bestiary[mob.name] ?: 0
    val known = kills > 0
    Row(
        modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Panel)
            .border(1.dp, if (known) Green.copy(alpha = 0.35f) else Line, RoundedCornerShape(4.dp))
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
                if (known) mob.name else "???",
                fontSize = 6.2.sp,
                color = if (known) Parchment else Muted2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                if (known) "$kills victime(s)" else "Non rencontré",
                fontSize = 5.2.sp,
                color = if (known) Green else Muted2
            )
        }
    }
}

@Composable
private fun BossEntry(state: GameState, boss: BossTemplate, floor: Int) {
    val kills = state.bossKills[boss.name] ?: 0
    val known = kills > 0
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 3.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF20121B))
            .border(1.dp, if (known) Blood.copy(alpha = 0.55f) else Line, RoundedCornerShape(4.dp))
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
                if (known) boss.name else "Gardien inconnu",
                fontSize = 6.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (known) GoldLight else Muted2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "CAPACITÉ · ${bossAbilityFor(boss.name, floor).label}",
                fontSize = 5.2.sp,
                color = if (known) Ember else Muted2,
                maxLines = 1
            )
        }
        Text(
            if (known) "$kills×" else "—",
            fontSize = 6.sp,
            color = if (known) Gold else Muted2
        )
    }
}

@Composable
private fun SetsCodex(state: GameState) {
    val ownedSets = ITEM_SETS.count { ownedSetSlots(state, it).isNotEmpty() }
    PixelCard(Modifier.fillMaxWidth(), borderColor = Gold.copy(alpha = 0.3f)) {
        SectionLabel("ENSEMBLES")
        Text(
            "$ownedSets / ${ITEM_SETS.size} ensembles commencés",
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = GoldLight
        )
        Text(
            "Les bonus s'activent à 2, 4 et 6 pièces équipées. Les pièces du sac et du coffre comptent pour la collection.",
            fontSize = 6.3.sp,
            color = Muted,
            lineHeight = 9.sp
        )
    }
    Spacer(Modifier.height(8.dp))
    ITEM_SETS.forEach { set ->
        val slots = ownedSetSlots(state, set)
        val count = slots.size
        val next = when {
            count < 2 -> 2
            count < 4 -> 4
            count < 6 -> 6
            else -> 6
        }
        PixelCard(
            Modifier.fillMaxWidth(),
            borderColor = if (count >= 2) Gold.copy(alpha = 0.35f) else Line
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    SectionLabel(setLabel(set))
                    Text(
                        "$count / 6 pièces",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (count >= 2) GoldLight else Muted
                    )
                }
                Text(
                    if (count >= 2) "BONUS ACTIF" else "À COMPLÉTER",
                    fontFamily = Press,
                    fontSize = 5.sp,
                    color = if (count >= 2) Green else Muted2
                )
            }
            Spacer(Modifier.height(5.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                listOf("weapon", "head", "body", "hands", "feet", "talisman").forEach { slot ->
                    val has = slot in slots
                    Box(
                        Modifier
                            .weight(1f)
                            .height(26.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (has) Gold.copy(alpha = 0.18f) else Panel3)
                            .border(1.dp, if (has) Gold.copy(alpha = 0.55f) else Line, RoundedCornerShape(3.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            slotLabel(slot).take(4).uppercase(),
                            fontSize = 4.5.sp,
                            color = if (has) GoldLight else Muted2,
                            maxLines = 1
                        )
                    }
                }
            }
            if (count >= 2) {
                Spacer(Modifier.height(4.dp))
                Text(setBonusDescription(set, count), fontSize = 6.2.sp, color = Green)
            }
            if (count < 6) {
                Text(
                    "Prochain palier ($next pièces) : ${setBonusDescription(set, next)}",
                    fontSize = 5.6.sp,
                    color = Muted,
                    lineHeight = 8.sp
                )
            }
        }
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun BiomesCodex(state: GameState) {
    val visited = BIOMES.count { state.floor >= it.fromFloor }
    PixelCard(Modifier.fillMaxWidth(), borderColor = Green.copy(alpha = 0.3f)) {
        SectionLabel("BIOMES")
        Text(
            "$visited / ${BIOMES.size} biomes découverts",
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = GoldLight
        )
        Text(
            "Chaque biome a sa règle, ses créatures, ses gardiens et ses objets.",
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
        val foundMobs = mobs.count { (state.bestiary[it.name] ?: 0) > 0 }
        val foundBosses = bosses.count { (state.bossKills[it.name] ?: 0) > 0 }
        val foundItems = items.count { it.name in discoveredItemNames(state) }
        val rule = biomeRuleFor(biome.fromFloor)
        PixelCard(
            Modifier.fillMaxWidth(),
            borderColor = if (known) Green.copy(alpha = 0.3f) else Line,
            background = if (known) Panel2 else Panel
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    SectionLabel(if (known) biome.name else "???")
                    Text(
                        if (known) rule.label else "Biome non découvert",
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (known) Green else Muted2
                    )
                }
                if (biomeKeyFor(state.floor) == biome.key) {
                    Text("ACTUEL", fontFamily = Press, fontSize = 5.5.sp, color = GoldLight)
                }
            }
            if (known) {
                Text(rule.description, fontSize = 6.2.sp, color = Muted, lineHeight = 9.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Créatures $foundMobs / ${mobs.size} · Gardiens $foundBosses / ${bosses.size} · Objets $foundItems / ${items.size}",
                    fontSize = 6.sp,
                    color = Parchment
                )
            }
        }
        Spacer(Modifier.height(6.dp))
    }
}
