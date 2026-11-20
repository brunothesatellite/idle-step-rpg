package com.brunothesatellite.idlesteprpg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brunothesatellite.idlesteprpg.GameViewModel
import com.brunothesatellite.idlesteprpg.UiState
import com.brunothesatellite.idlesteprpg.engine.GameEngine
import com.brunothesatellite.idlesteprpg.engine.GameState
import com.brunothesatellite.idlesteprpg.engine.Item
import com.brunothesatellite.idlesteprpg.engine.ITEM_SETS
import com.brunothesatellite.idlesteprpg.engine.BIOMES
import com.brunothesatellite.idlesteprpg.engine.allBossTemplates
import com.brunothesatellite.idlesteprpg.engine.allMobTemplates
import com.brunothesatellite.idlesteprpg.engine.bossFor
import com.brunothesatellite.idlesteprpg.engine.bossAbilityFor
import com.brunothesatellite.idlesteprpg.engine.biomeFor
import com.brunothesatellite.idlesteprpg.engine.biomeRuleFor
import com.brunothesatellite.idlesteprpg.engine.CONSUMABLES
import com.brunothesatellite.idlesteprpg.engine.consumableContextLabel
import com.brunothesatellite.idlesteprpg.engine.equipped
import com.brunothesatellite.idlesteprpg.engine.itemScore
import com.brunothesatellite.idlesteprpg.engine.powerOf
import com.brunothesatellite.idlesteprpg.engine.discoveredSetSlots
import com.brunothesatellite.idlesteprpg.engine.rarityLabel
import com.brunothesatellite.idlesteprpg.engine.rankLabel
import com.brunothesatellite.idlesteprpg.engine.setLabel
import com.brunothesatellite.idlesteprpg.engine.slotLabel
import com.brunothesatellite.idlesteprpg.engine.statsOf
import com.brunothesatellite.idlesteprpg.engine.tierName
import com.brunothesatellite.idlesteprpg.engine.titleFor
import com.brunothesatellite.idlesteprpg.engine.upgradeCost
import com.brunothesatellite.idlesteprpg.engine.upgradeDesc
import com.brunothesatellite.idlesteprpg.engine.upgradeIcon
import com.brunothesatellite.idlesteprpg.engine.upgradeLevel
import com.brunothesatellite.idlesteprpg.engine.upgradeName
import com.brunothesatellite.idlesteprpg.engine.xpNext
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

@Composable
fun Dashboard(
    state: GameState,
    ui: UiState,
    activeTab: String,
    onSync: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCamp: () -> Unit,
    onOpenJournal: () -> Unit,
    onOpenPreferences: () -> Unit,
    onBuyUpgrade: (String) -> Unit,
    onSelectItem: (Item) -> Unit,
    onSellCommons: () -> Unit,
    onEquipBest: () -> Unit,
    onWithdrawStash: () -> Unit,
    onWithdrawStashItem: (Item) -> Unit,
    onUseConsumable: (String) -> Unit,
    onToggleFarm: (Boolean) -> Unit,
    onOpenCodex: () -> Unit,
    onSelectTab: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF15101F), Color(0xFF110B19), Color(0xFF0D0914))
                )
            )
            .border(1.dp, Gold.copy(alpha = 0.24f), RoundedCornerShape(0.dp))
    ) {
        DashboardHeader(state, ui, onOpenSettings, onOpenPreferences)
        Tabs(activeTab, onSelectTab)
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            when (activeTab) {
                "inventory" -> InventoryTab(
                    state,
                    onSelectItem,
                    onSellCommons,
                    onEquipBest,
                    onWithdrawStash,
                    onWithdrawStashItem,
                    onUseConsumable
                )
                "upgrades" -> UpgradesTab(state, ui, onBuyUpgrade, onOpenSettings)
                else -> StatsTab(state, ui, onOpenJournal, onToggleFarm, onOpenCodex)
            }
        }
    }
}

@Composable
private fun DashboardHeader(
    state: GameState,
    ui: UiState,
    onOpenSettings: () -> Unit,
    onOpenPreferences: () -> Unit
) {
    val stats = statsOf(state)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF2A1F3A), Color(0xFF140E1D))))
                .border(1.dp, Line2, RoundedCornerShape(5.dp)),
            contentAlignment = Alignment.Center
        ) {
            AssetImage("hero/human.png", Modifier.size(34.dp))
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Gold)
                    .padding(horizontal = 3.dp, vertical = 1.dp)
            ) {
                Text(
                    state.level.toString().padStart(2, '0'),
                    fontFamily = Press,
                    fontSize = 5.sp,
                    color = Ink
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("Kaelen", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Parchment)
                Spacer(Modifier.width(5.dp))
                Text(titleFor(state.floor), fontSize = 7.5.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(Modifier.fillMaxWidth().padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                MiniBar(
                    fraction = state.xp.toFloat() / xpNext(state.level).toFloat(),
                    color = Blue,
                    modifier = Modifier.weight(1f),
                    height = 3.dp
                )
                Spacer(Modifier.width(5.dp))
                Text("${state.xp} / ${xpNext(state.level)}", fontSize = 6.sp, color = Muted)
            }
        }
        Spacer(Modifier.width(6.dp))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                CurrencyChip("item/01.png", formatInt(state.gold))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                CurrencyChip("item/meat_ration.png", state.rations.toString())
                CurrencyChip("item/misc_crystal.png", state.shards.toString())
                val keyDays = if (ui.stepSource == "garmin") {
                    ui.keyExpiry?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) }
                } else {
                    null
                }
                val needsSource = !ui.connected || (keyDays != null && keyDays < 7)
                // Alerte : n'apparaît que sans source active ou quand la clé approche de l'expiration.
                if (needsSource) {
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Panel2)
                            .border(1.dp, Ember.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .clickable(onClick = onOpenSettings)
                            .semantics { contentDescription = "Configurer la source de pas" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("!", fontFamily = Press, fontSize = 8.sp, color = Ember)
                    }
                }
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Panel2)
                        .border(1.dp, Line2, RoundedCornerShape(4.dp))
                        .clickable(onClick = onOpenPreferences)
                        .semantics { contentDescription = "Réglages" },
                    contentAlignment = Alignment.Center
                ) {
                    Text("⚙", fontSize = 11.sp, color = Gold)
                }
            }
        }
    }
}

@Composable
private fun Tabs(active: String, onTab: (String) -> Unit) {
    // Libellés de navigation : largeur fixe (1/3 d'écran). On plafonne leur
    // facteur de texte, sinon « Améliorations » déborde en taille Maximal.
    val density = LocalDensity.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .border(0.dp, Color.Transparent)
    ) {
        CompositionLocalProvider(
            LocalDensity provides Density(
                density = density.density,
                fontScale = minOf(density.fontScale, 1.35f)
            )
        ) {
            listOf("stats" to "Stats", "inventory" to "Inventaire", "upgrades" to "Améliorations").forEach { (key, label) ->
                Column(
                    Modifier
                        .weight(1f)
                        .clickable { onTab(key) }
                        .padding(vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        label,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (active == key) Parchment else Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .width(if (active == key) 42.dp else 0.dp)
                            .height(2.dp)
                            .background(if (active == key) Gold else Color.Transparent)
                    )
                }
            }
        }
    }
    DividerLine()
}

@Composable
private fun StatsTab(
    state: GameState,
    ui: UiState,
    onOpenJournal: () -> Unit,
    onToggleFarm: (Boolean) -> Unit,
    onOpenCodex: () -> Unit
) {
    val stats = statsOf(state)
    val power = powerOf(state)

    PixelCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                SectionLabel("PUISSANCE")
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(formatInt(power), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = GoldLight)
                    Spacer(Modifier.width(6.dp))
                    Text("▲ ${state.level}", fontSize = 7.sp, color = Green)
                }
            }
            Box(
                Modifier
                    .width(1.dp)
                    .height(38.dp)
                    .background(Line)
            )
            Column(
                Modifier.padding(start = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("RANG", fontSize = 5.sp, letterSpacing = 1.sp, color = Muted)
                Text(rankLabel(state.floor), fontFamily = Press, fontSize = 14.sp, color = Gold)
            }
        }
    }
    Spacer(Modifier.height(5.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        StatTile("item/long_sword3.png", "ATQ", stats.atk.toString(), Modifier.weight(1f))
        StatTile("fx/heart.png", "PV", stats.maxHp.toString(), Modifier.weight(1f))
        StatTile(
            "item/boots4_green.png",
            "VIT",
            "%.2f".format(stats.vit * (1.0 + 0.004 * state.ferveur)).replace('.', ','),
            Modifier.weight(1f)
        )
    }
    Spacer(Modifier.height(5.dp))
    ResourcesCard(state)
    Spacer(Modifier.height(5.dp))
    BiomeCard(state, onToggleFarm)
    Spacer(Modifier.height(5.dp))
    CodexCard(state, onOpenCodex)
    Spacer(Modifier.height(5.dp))
    FerveurCard(state)
    Spacer(Modifier.height(5.dp))
    OathsCard(state)
    Spacer(Modifier.height(5.dp))
    AchievementsCard(state)
    Spacer(Modifier.height(5.dp))
    JournalCard(state, onOpenJournal)
}

@Composable
private fun StatTile(icon: String, label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Panel)
            .border(1.dp, Line, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AssetIcon(icon, 18.dp)
        Spacer(Modifier.width(4.dp))
        Column {
            Text(label, fontSize = 5.5.sp, letterSpacing = 0.7.sp, color = Muted)
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Parchment, maxLines = 1)
        }
    }
}

@Composable
private fun BiomeCard(state: GameState, onToggleFarm: (Boolean) -> Unit) {
    val rule = biomeRuleFor(state.floor)
    PixelCard(
        Modifier.fillMaxWidth(),
        borderColor = if (state.holdFloor) Ember.copy(alpha = 0.5f) else Green.copy(alpha = 0.28f),
        background = Panel2
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                SectionLabel("BIOME · ${biomeFor(state.floor)}")
                Text(rule.label, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Green)
                Text(rule.description, fontSize = 6.3.sp, color = Muted, lineHeight = 9.sp)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "PALIER\n${state.floor.toString().padStart(2, '0')}",
                fontSize = 6.sp,
                color = Muted2,
                textAlign = TextAlign.End
            )
        }
        Spacer(Modifier.height(5.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (state.holdFloor) "FARM ACTIF · PALIER VERROUILLÉ" else "FARM INACTIF",
                    fontSize = 6.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (state.holdFloor) Ember else Muted
                )
                Text(
                    if (state.holdFloor) {
                        "Le gardien revient sans faire avancer le palier. Les jalons sont désactivés."
                    } else {
                        "Verrouille ce palier pour farmer sans passer au biome suivant."
                    },
                    fontSize = 5.9.sp,
                    color = Muted,
                    lineHeight = 8.sp
                )
            }
            Spacer(Modifier.width(8.dp))
            SmallAction(
                if (state.holdFloor) "DÉSACTIVER" else "ACTIVER",
                Modifier.width(104.dp),
                onClick = { onToggleFarm(!state.holdFloor) },
                primary = !state.holdFloor
            )
        }
    }
}

@Composable
private fun CodexCard(state: GameState, onOpen: () -> Unit) {
    val mobs = allMobTemplates()
    val bosses = allBossTemplates()
    val discoveredMobs = mobs.count { (state.bestiary[it.name] ?: 0) > 0 }
    val discoveredBosses = bosses.count { (state.bossKills[it.name] ?: 0) > 0 }
    val sets = ITEM_SETS.count { discoveredSetSlots(state, it).size >= 6 }
    val biomes = BIOMES.count { state.floor >= it.fromFloor }
    PixelCard(
        Modifier.fillMaxWidth(),
        borderColor = Gold.copy(alpha = 0.32f),
        background = Panel2
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                SectionLabel("CODEX")
                Text(
                    "Bestiaire $discoveredMobs / ${mobs.size} · Gardiens $discoveredBosses / ${bosses.size}",
                    fontSize = 6.8.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldLight
                )
                Text(
                    "Ensembles $sets / ${ITEM_SETS.size} · Biomes $biomes / ${BIOMES.size}",
                    fontSize = 6.2.sp,
                    color = Muted
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        OverlayAction("OUVRIR LE CODEX", Modifier.fillMaxWidth(), onOpen, primary = true)
    }
}

@Composable
private fun ResourcesCard(state: GameState) {
    PixelCard(Modifier.fillMaxWidth(), background = Panel.copy(alpha = 0.72f)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ResourceStat("item/meat_ration.png", "RATIONS", "${state.rations} / 24")
            ResourceStat("item/misc_crystal.png", "ÉCLATS", "${state.shards} / 12")
            ResourceStat("item/01.png", "OR", formatInt(state.gold))
            ResourceStat(
                "item/cons_potion_red.png",
                "CONSOMMABLES",
                state.consumables.values.sum().toString()
            )
        }
    }
}

@Composable
private fun ResourceStat(icon: String, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AssetIcon(icon, 15.dp)
        Spacer(Modifier.width(4.dp))
        Column {
            Text(label, fontSize = 5.sp, letterSpacing = 0.5.sp, color = Muted2)
            Text(value, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Parchment, maxLines = 1)
        }
    }
}

@Composable
private fun FerveurCard(state: GameState) {
    PixelCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FlameIcon(Modifier.size(11.dp))
            Spacer(Modifier.width(4.dp))
            Text("Ferveur", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Parchment)
            Spacer(Modifier.weight(1f))
            Text("${state.ferveur}", fontFamily = Press, fontSize = 8.sp, color = Ember)
            Text(" / 100", fontSize = 7.sp, color = Muted)
            Spacer(Modifier.width(5.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(2.dp))
                    .background(Ember.copy(alpha = 0.12f))
                    .border(1.dp, Ember.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(tierName(state.ferveur), fontSize = 5.5.sp, color = Ember)
            }
        }
        Spacer(Modifier.height(5.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(1.5.dp)) {
            repeat(20) { index ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(if (index < (state.ferveur + 4) / 5) Ember else Panel3)
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            BonusText("VIT", "+${"%.1f".format(state.ferveur * 0.4).replace('.', ',')} %")
            BonusText("RÉGÉN", "+${"%.1f".format(state.ferveur * 0.8).replace('.', ',')} %")
            BonusText("CRIT", "+${"%.1f".format(state.ferveur * 0.02).replace('.', ',')} %")
        }
    }
}

@Composable
private fun BonusText(label: String, value: String) {
    Text("$label $value", fontSize = 5.8.sp, color = Muted)
}

@Composable
private fun OathsCard(state: GameState) {
    val oaths = state.oaths
    val done = listOf(oaths.stepsClaimed, oaths.bossesClaimed, oaths.chestsClaimed).count { it }
    PixelCard(Modifier.fillMaxWidth(), background = Panel.copy(alpha = 0.72f)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Serments du jour")
            Spacer(Modifier.weight(1f))
            Text("$done / 3", fontFamily = Press, fontSize = 6.5.sp, color = if (done == 3) Green else Ember)
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Série ${state.streak.coerceAtLeast(1)} jour(s)", fontSize = 5.8.sp, color = if (state.streak >= 7) Gold else Muted2)
            Text("${formatInt(state.weeklySteps)} pas cette semaine", fontSize = 5.8.sp, color = Muted2)
        }
        Spacer(Modifier.height(3.dp))
        OathRow(
            "Marche",
            "${state.stepsToday.coerceAtMost(oaths.stepsTarget)} / ${oaths.stepsTarget} pas",
            oaths.stepsClaimed,
            "150 or · 1 éclat",
            "item/boots4_green.png"
        )
        OathRow(
            "Chasse",
            "${state.bossesToday.coerceAtMost(oaths.bossesTarget)} / ${oaths.bossesTarget} gardien",
            oaths.bossesClaimed,
            "250 or · 2 éclats",
            "item/long_sword3.png"
        )
        OathRow(
            "Curiosité",
            "${state.chestsToday.coerceAtMost(oaths.chestsTarget)} / ${oaths.chestsTarget} coffre",
            oaths.chestsClaimed,
            "100 or · objet",
            "item/misc_crystal.png"
        )
    }
}

@Composable
private fun AchievementsCard(state: GameState) {
    val items = listOf(
        "Premier gardien vaincu" to (state.bosses > 0),
        "10 coffres ouverts" to (state.chestsOpened >= 10),
        "Palier 10 atteint" to (state.floor >= 10),
        "50 000 pas cumulés" to (state.totalSteps >= 50_000),
        "Série de 7 jours" to (state.streak >= 7)
    )
    PixelCard(Modifier.fillMaxWidth(), background = Panel.copy(alpha = 0.72f)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Exploits")
            Spacer(Modifier.weight(1f))
            Text(
                "${items.count { it.second }} / ${items.size}",
                fontFamily = Press,
                fontSize = 6.sp,
                color = if (items.all { it.second }) Green else Muted
            )
        }
        Spacer(Modifier.height(3.dp))
        items.forEach { (label, done) ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 1.8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (done) "✓" else "○",
                    fontSize = 7.sp,
                    color = if (done) Green else Muted2,
                    modifier = Modifier.width(12.dp)
                )
                Text(
                    label,
                    fontSize = 6.5.sp,
                    color = if (done) Parchment else Muted2
                )
            }
        }
    }
}

@Composable
private fun OathRow(
    title: String,
    progress: String,
    claimed: Boolean,
    reward: String,
    icon: String
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(15.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(if (claimed) Green.copy(alpha = 0.18f) else Panel2)
                .border(1.dp, if (claimed) Green.copy(alpha = 0.55f) else Line, RoundedCornerShape(3.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (claimed) {
                Text("✓", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Green)
            } else {
                AssetIcon(icon, 10.dp)
            }
        }
        Spacer(Modifier.width(5.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = if (claimed) Green else Parchment)
            Text(progress, fontSize = 6.sp, color = Muted)
        }
        Text(
            if (claimed) "ACCOMPLI" else reward,
            fontSize = 5.5.sp,
            color = if (claimed) Green else Muted2,
            maxLines = 1
        )
    }
}

@Composable
private fun JournalCard(state: GameState, onOpen: () -> Unit) {
    PixelCard(Modifier.fillMaxWidth(), background = Panel.copy(alpha = 0.72f)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Journal")
            Spacer(Modifier.weight(1f))
            Row(
                Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .clickable(onClick = onOpen)
                    .padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Tout voir", fontSize = 6.sp, color = Gold)
                Text(" ›", fontSize = 7.sp, color = Gold)
            }
        }
        Spacer(Modifier.height(4.dp))
        state.journal.take(3).forEach { entry ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 2.5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(4.dp)
                        .background(
                            when (entry.type) {
                                "good" -> Green
                                "bad" -> Blood
                                else -> Gold
                            }
                        )
                )
                Spacer(Modifier.width(5.dp))
                Text(entry.text, fontSize = 7.5.sp, color = Parchment.copy(alpha = 0.78f), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(entry.time, fontSize = 5.8.sp, color = Muted2)
            }
        }
    }
}

@Composable
private fun SmallAction(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    primary: Boolean = false,
    enabled: Boolean = true
) {
    Box(
        modifier
            .heightIn(min = 30.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(
                when {
                    !enabled -> Panel3
                    primary -> Gold
                    else -> Panel2
                }
            )
            .border(
                1.dp,
                when {
                    !enabled -> Line
                    primary -> GoldLight
                    else -> Line2
                },
                RoundedCornerShape(4.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = when {
                !enabled -> Muted2
                primary -> Ink
                else -> Parchment
            }
        )
    }
}

@Composable
private fun InventoryTab(
    state: GameState,
    onSelectItem: (Item) -> Unit,
    onSellCommons: () -> Unit,
    onEquipBest: () -> Unit,
    onWithdrawStash: () -> Unit,
    onWithdrawStashItem: (Item) -> Unit,
    onUseConsumable: (String) -> Unit
) {
    var sort by rememberSaveable { mutableStateOf("score") }
    var slotFilter by rememberSaveable { mutableStateOf("all") }
    var fusionOnly by rememberSaveable { mutableStateOf(false) }
    var stashOpen by remember { mutableStateOf(false) }
    var invSection by rememberSaveable { mutableStateOf("gear") }
    val consumableCount = state.consumables.values.sum()
    val filtered = state.inventory
        .filter { slotFilter == "all" || it.slot == slotFilter }
        .filter { !fusionOnly || GameEngine.canFuse(state, it) }
        .sortedWith(
            when (sort) {
                "rarity" -> compareByDescending<Item> { rarityRank(it.rarity) }
                    .thenByDescending { itemScore(it) }
                "level" -> compareByDescending<Item> { it.level }
                    .thenByDescending { itemScore(it) }
                else -> compareByDescending { itemScore(it) }
            }
        )

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("ÉQUIPEMENT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Parchment)
        Spacer(Modifier.weight(1f))
        Text("Puissance ${powerOf(state)}", fontSize = 6.5.sp, color = Muted)
    }
    Spacer(Modifier.height(5.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        MiniChip("Équipement", invSection == "gear") { invSection = "gear" }
        MiniChip("Consommables ($consumableCount)", invSection == "consumables") {
            invSection = "consumables"
        }
    }
    if (invSection == "consumables") {
        ConsumablesSection(state, onUseConsumable)
        return
    }
    Spacer(Modifier.height(5.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf("weapon", "head", "body", "hands", "feet", "talisman").forEach { slot ->
            val item = equipped(state, slot)
            Column(
                Modifier
                    .weight(1f)
                    .heightIn(min = 64.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Panel)
                    .border(
                        1.dp,
                        if (item != null) rarityColor(item.rarity).copy(alpha = 0.65f) else Line,
                        RoundedCornerShape(4.dp)
                    )
                    .clickable(enabled = item != null) { item?.let(onSelectItem) }
                    .semantics {
                        contentDescription = "${slotLabel(slot)} : ${item?.name ?: "vide"}"
                    }
                    .padding(3.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (item != null) {
                    AssetIcon(item.icon, 24.dp)
                } else {
                    Text("+", fontSize = 12.sp, color = Muted2)
                }
                Text(slotLabel(slot), fontSize = 4.8.sp, color = Muted, maxLines = 1)
            }
        }
    }
    Spacer(Modifier.height(7.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        SmallAction("Équiper le meilleur", Modifier.weight(1.35f), onEquipBest, primary = true)
        SmallAction("Vendre les communs", Modifier.weight(1f), onSellCommons)
    }
    Spacer(Modifier.height(7.dp))
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        MiniChip("Score", sort == "score") { sort = "score" }
        MiniChip("Rareté", sort == "rarity") { sort = "rarity" }
        MiniChip("Niveau", sort == "level") { sort = "level" }
        MiniChip("Fusion", fusionOnly) { fusionOnly = !fusionOnly }
    }
    Spacer(Modifier.height(4.dp))
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        MiniChip("Tous", slotFilter == "all") { slotFilter = "all" }
        listOf(
            "weapon" to "Arme",
            "head" to "Tête",
            "body" to "Torse",
            "hands" to "Mains",
            "feet" to "Bottes",
            "talisman" to "Talisman"
        ).forEach { (slot, label) ->
            MiniChip(label, slotFilter == slot) { slotFilter = slot }
        }
    }
    Spacer(Modifier.height(9.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("BUTIN", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Parchment)
        Spacer(Modifier.weight(1f))
        Text(
            "${state.inventory.size} / 24 objets · ${filtered.size} affiché(s)",
            fontSize = 6.sp,
            color = Muted
        )
    }
    Spacer(Modifier.height(4.dp))
    if (filtered.isEmpty()) {
        Text(
            if (fusionOnly) "Aucun objet fusionnable." else "Aucun objet dans ce filtre.",
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            textAlign = TextAlign.Center,
            fontSize = 7.sp,
            color = Muted2
        )
    }
    filtered.chunked(3).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            row.forEach { item ->
                InventoryCard(item, Modifier.weight(1f)) { onSelectItem(item) }
            }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(4.dp))
    }
    if (state.stash.isNotEmpty()) {
        PixelCard(
            Modifier.fillMaxWidth(),
            borderColor = Gold.copy(alpha = 0.35f),
            background = Panel2
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    SectionLabel("Coffre de camp")
                    Text(
                        "${state.stash.size} objet(s) en réserve · inventaire plein",
                        fontSize = 6.5.sp,
                        color = Muted
                    )
                }
                Spacer(Modifier.width(6.dp))
                SmallAction(
                    if (stashOpen) "FERMER" else "VOIR",
                    Modifier.width(70.dp),
                    onClick = { stashOpen = !stashOpen }
                )
            }
            if (stashOpen) {
                Spacer(Modifier.height(6.dp))
                state.stash.forEach { item ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssetIcon(item.icon, 18.dp)
                        Spacer(Modifier.width(5.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (item.identified) item.name else "Objet mystérieux",
                                fontSize = 6.5.sp,
                                color = Parchment,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                rarityLabel(item.rarity) + if (item.locked) " · VERROU" else "",
                                fontSize = 5.2.sp,
                                color = if (item.locked) Ember else rarityColor(item.rarity)
                            )
                        }
                        SmallAction(
                            "PRENDRE",
                            Modifier.width(74.dp),
                            onClick = { onWithdrawStashItem(item) }
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                SmallAction("TOUT PRENDRE", Modifier.fillMaxWidth(), onWithdrawStash, primary = true)
            }
        }
        Spacer(Modifier.height(5.dp))
    }
    Text(
        "Touchez un objet pour comparer avant de l'équiper. Les objets verrouillés ne peuvent pas être vendus.",
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        fontSize = 6.sp,
        color = Muted2
    )
    Spacer(Modifier.height(8.dp))
}

private fun rarityRank(rarity: String): Int = when (rarity) {
    "legendary" -> 5
    "epic" -> 4
    "rare" -> 3
    "uncommon" -> 2
    else -> 1
}

@Composable
private fun ConsumablesSection(state: GameState, onUse: (String) -> Unit) {
    val stats = statsOf(state)
    val owned = CONSUMABLES.filter { (state.consumables[it.id] ?: 0) > 0 }
    Spacer(Modifier.height(9.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("CONSOMMABLES", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Parchment)
        Spacer(Modifier.weight(1f))
        Text(
            "${state.consumables.values.sum()} objet(s) · soins et buffs",
            fontSize = 6.sp,
            color = Muted
        )
    }
    Spacer(Modifier.height(4.dp))
    if (owned.isEmpty()) {
        Text(
            "Aucun consommable. Les monstres et les coffres en laissent parfois.",
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            textAlign = TextAlign.Center,
            fontSize = 7.sp,
            color = Muted2
        )
        return
    }
    owned.forEach { template ->
        val quantity = state.consumables[template.id] ?: 0
        val usable = when {
            template.context == "combat" && state.fight == null -> false
            template.effect == "heal_pct" || template.effect == "full_heal" -> state.hp < stats.maxHp
            template.effect == "heal_ration" -> state.hp < stats.maxHp
            template.effect == "ferveur" -> state.ferveur < 100
            template.effect == "heal_ferveur" -> state.hp < stats.maxHp || state.ferveur < 100
            else -> true
        }
        PixelCard(
            Modifier.fillMaxWidth(),
            borderColor = rarityColor(template.rarity).copy(alpha = 0.5f),
            background = Panel2
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Panel)
                        .border(1.dp, rarityColor(template.rarity).copy(alpha = 0.6f), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AssetIcon(template.icon, 28.dp)
                }
                Spacer(Modifier.width(7.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            template.name,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Parchment,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("×$quantity", fontFamily = Press, fontSize = 6.sp, color = Gold)
                    }
                    Text(
                        template.description,
                        fontSize = 6.2.sp,
                        color = Muted,
                        lineHeight = 9.sp
                    )
                    Row(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            rarityLabel(template.rarity),
                            fontSize = 5.sp,
                            color = rarityColor(template.rarity)
                        )
                        Text(
                            consumableContextLabel(template.context),
                            fontSize = 5.sp,
                            color = if (template.context == "combat") Ember else Green
                        )
                    }
                }
                Spacer(Modifier.width(5.dp))
                SmallAction(
                    "UTILISER",
                    Modifier.width(70.dp),
                    onClick = { onUse(template.id) },
                    primary = true,
                    enabled = usable
                )
            }
        }
        Spacer(Modifier.height(4.dp))
    }
    Text(
        "Les objets de combat s'utilisent pendant un combat. Les soins et la ferveur servent au camp comme en exploration.",
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        textAlign = TextAlign.Center,
        fontSize = 5.8.sp,
        color = Muted2,
        lineHeight = 8.sp
    )
}

@Composable
private fun MiniChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (active) Gold.copy(alpha = 0.16f) else Panel)
            .border(1.dp, if (active) Gold.copy(alpha = 0.55f) else Line, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 4.dp)
    ) {
        Text(label, fontSize = 5.8.sp, color = if (active) GoldLight else Muted, maxLines = 1)
    }
}

@Composable
private fun InventoryCard(item: Item, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .heightIn(min = 62.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Panel)
            .border(
                1.dp,
                if (item.locked) Ember.copy(alpha = 0.7f) else rarityColor(item.rarity).copy(alpha = 0.62f),
                RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = if (item.identified) {
                    "${item.name}, ${rarityLabel(item.rarity)}"
                } else {
                    "Objet non identifié"
                }
            }
            .padding(vertical = 3.dp, horizontal = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AssetIcon(item.icon, 27.dp)
        Text(
            if (item.identified) item.name else "Objet mystérieux",
            fontSize = 6.sp,
            color = Parchment.copy(alpha = 0.85f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            lineHeight = 8.sp
        )
        Text(
            when {
                item.locked -> "VERROU"
                !item.identified -> "NON IDENTIFIÉ"
                item.setName.isNotBlank() -> "${rarityLabel(item.rarity)} · ${setLabel(item.setName).split(" ").first()}"
                else -> rarityLabel(item.rarity)
            },
            fontSize = 4.5.sp,
            color = when {
                item.locked -> Ember
                !item.identified -> Ember
                item.setName.isNotBlank() -> Gold
                else -> rarityColor(item.rarity)
            },
            maxLines = 1
        )
    }
}

@Composable
private fun UpgradesTab(
    state: GameState,
    ui: UiState,
    onBuy: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column {
            SectionLabel("FEU DE CAMP")
            Text("Forger la marche", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Parchment)
        }
        Spacer(Modifier.weight(1f))
        CurrencyChip("item/01.png", formatInt(state.gold))
    }
    Spacer(Modifier.height(6.dp))
    listOf("blade", "breath", "swift").forEach { key ->
        UpgradeCard(state, key, onBuy)
        Spacer(Modifier.height(4.dp))
    }
    Spacer(Modifier.height(2.dp))
    val boss = bossFor(state.floor)
    PixelCard(
        Modifier.fillMaxWidth(),
        borderColor = Blood.copy(alpha = 0.3f),
        background = Color(0xFF20121B)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AssetImage(boss.icon, Modifier.size(38.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("PROCHAIN GARDIEN", fontSize = 5.sp, letterSpacing = 0.8.sp, color = Blood)
                Text(boss.name, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GoldLight)
                Text("Palier ${state.floor.toString().padStart(2, '0')} · Niv. ${state.floor + 2}", fontSize = 6.sp, color = Muted)
                Text(
                    "CAPACITÉ · ${bossAbilityFor(boss.name, state.floor).label}",
                    fontSize = 5.5.sp,
                    color = Ember,
                    maxLines = 1
                )
            }
            Text("6× PV\nPHASE 2", fontSize = 5.5.sp, color = Ember, textAlign = TextAlign.End)
        }
    }
    Spacer(Modifier.height(4.dp))
    Text(
        if (ui.lastError.isNotBlank()) ui.lastError else "Les bonus de camp sont permanents. Le coût augmente à chaque niveau.",
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        fontSize = 6.sp,
        color = if (ui.lastError.isNotBlank()) Blood else Muted2
    )
}

@Composable
private fun UpgradeCard(state: GameState, key: String, onBuy: (String) -> Unit) {
    val level = upgradeLevel(state, key)
    val cost = upgradeCost(key, level)
    val afford = state.gold >= cost
    PixelCard(Modifier.fillMaxWidth(), background = Panel2) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AssetIcon(upgradeIcon(key), 28.dp)
            Spacer(Modifier.width(7.dp))
            Column(Modifier.weight(1f)) {
                Text(upgradeName(key), fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Parchment)
                Text("${upgradeDesc(key)} · Niv. $level", fontSize = 6.2.sp, color = Muted)
                Row(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(10) { i ->
                        Box(
                            Modifier
                                .width(7.dp)
                                .height(2.5.dp)
                                .background(if (i < level) Gold else Panel3)
                        )
                    }
                }
            }
            Spacer(Modifier.width(5.dp))
            Box(
                Modifier
                    .widthIn(min = 62.dp)
                    .heightIn(min = 34.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (afford) Gold else Panel3)
                    .border(1.dp, if (afford) GoldLight else Line2, RoundedCornerShape(3.dp))
                    .clickable(enabled = afford) { onBuy(key) }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (afford) formatInt(cost) else "MANQUE",
                    fontFamily = Press,
                    fontSize = 6.sp,
                    color = if (afford) Ink else Muted2
                )
            }
        }
    }
}

@Composable
private fun SyncBar(
    state: GameState,
    ui: UiState,
    onSync: () -> Unit,
    onOpenSettings: () -> Unit,
    compact: Boolean = false
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xFF100B19), Color(0xFF0C0813))))
            .border(1.dp, Gold.copy(alpha = 0.10f), RoundedCornerShape(0.dp))
            .padding(horizontal = 12.dp, vertical = if (compact) 5.dp else 7.dp)
    ) {
        if (compact) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Dernière sync · ${formatTime(ui.lastSync)}",
                        fontSize = 6.sp,
                        color = Muted2
                    )
                    Text(
                        if (ui.lastAdded > 0) {
                            "+${formatInt(ui.lastAdded)} pas"
                        } else {
                            "${formatInt(state.stepsToday)} pas aujourd'hui"
                        },
                        fontSize = 6.sp,
                        color = if (ui.lastAdded > 0) Gold else Muted2
                    )
                }
                Spacer(Modifier.width(8.dp))
                SmallAction(
                    if (!ui.connected) "CONNECTER" else if (ui.syncing) "SYNC…" else "SYNC",
                    Modifier.width(88.dp),
                    onClick = if (ui.connected) onSync else onOpenSettings,
                    primary = ui.connected,
                    enabled = !ui.syncing
                )
            }
            return@Column
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "Dernière sync · ${formatTime(ui.lastSync)}",
                fontSize = 6.sp,
                color = Muted2
            )
            Text(
                if (ui.lastAdded > 0) "+${formatInt(ui.lastAdded)} pas" else "${formatInt(state.stepsToday)} pas aujourd'hui",
                fontSize = 6.sp,
                color = if (ui.lastAdded > 0) Gold else Muted2
            )
        }
        Spacer(Modifier.height(4.dp))
        val sourceLabel = if (ui.stepSource == "health") "SANTÉ CONNECT" else "GARMIN"
        val sourceDetail = if (ui.stepSource == "health") "Santé Connect" else "Personal API"
        when {
            !ui.connected -> GoldButton(
                text = "CONNECTER $sourceLabel",
                subtitle = "10 pas = 1 m · $sourceDetail",
                onClick = onOpenSettings
            )
            ui.syncing -> GoldButton(
                text = "SYNCHRONISATION…",
                subtitle = sourceDetail,
                enabled = false,
                onClick = {}
            )
            else -> GoldButton(
                text = "SYNCHRONISER $sourceLabel",
                subtitle = "$sourceDetail · ${formatInt(state.stepsToday)} pas aujourd'hui",
                badge = if (ui.lastAdded > 0) "+${formatInt(ui.lastAdded)}" else null,
                onClick = onSync
            )
        }
    }
}
