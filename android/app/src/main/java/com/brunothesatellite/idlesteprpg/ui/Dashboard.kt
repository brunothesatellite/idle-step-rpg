package com.brunothesatellite.idlesteprpg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brunothesatellite.idlesteprpg.GameViewModel
import com.brunothesatellite.idlesteprpg.UiState
import com.brunothesatellite.idlesteprpg.engine.GameState
import com.brunothesatellite.idlesteprpg.engine.Item
import com.brunothesatellite.idlesteprpg.engine.bossFor
import com.brunothesatellite.idlesteprpg.engine.equipped
import com.brunothesatellite.idlesteprpg.engine.powerOf
import com.brunothesatellite.idlesteprpg.engine.rarityLabel
import com.brunothesatellite.idlesteprpg.engine.slotLabel
import com.brunothesatellite.idlesteprpg.engine.statsOf
import com.brunothesatellite.idlesteprpg.engine.tierName
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
    onSync: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCamp: () -> Unit,
    onBuyUpgrade: (String) -> Unit,
    onSelectItem: (Item) -> Unit,
    onSelectTab: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var tab by rememberSaveable { mutableStateOf("stats") }

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
        DashboardHeader(state, ui, onOpenSettings)
        Tabs(tab) {
            tab = it
            onSelectTab(it)
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            when (tab) {
                "inventory" -> InventoryTab(state, onSelectItem)
                "upgrades" -> UpgradesTab(state, ui, onBuyUpgrade, onOpenSettings)
                else -> StatsTab(state, ui, onOpenSettings, onSync)
            }
        }
        SyncBar(state, ui, onSync, onOpenSettings)
    }
}

@Composable
private fun DashboardHeader(state: GameState, ui: UiState, onOpenSettings: () -> Unit) {
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
                Text("le Pèlerin", fontSize = 7.5.sp, color = Muted)
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
                CurrencyChip("item/meat_ration.png", state.rations.toString())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                CurrencyChip("item/misc_crystal.png", state.shards.toString())
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Panel2)
                        .border(1.dp, if (ui.connected) Green.copy(alpha = 0.5f) else Line2, RoundedCornerShape(4.dp))
                        .clickable(onClick = onOpenSettings)
                        .semantics { contentDescription = "Compte Garmin" },
                    contentAlignment = Alignment.Center
                ) {
                    Text("G", fontFamily = Press, fontSize = 8.sp, color = if (ui.connected) Green else Gold)
                }
            }
        }
    }
}

@Composable
private fun Tabs(active: String, onTab: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .border(0.dp, Color.Transparent)
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
                    maxLines = 1
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
    DividerLine()
}

@Composable
private fun StatsTab(
    state: GameState,
    ui: UiState,
    onOpenSettings: () -> Unit,
    onSync: () -> Unit
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
                Text("V", fontFamily = Press, fontSize = 14.sp, color = Gold)
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
    FerveurCard(state)
    Spacer(Modifier.height(5.dp))
    JournalCard(state)
    Spacer(Modifier.height(5.dp))
    GarminCard(ui, onOpenSettings, onSync)
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
private fun JournalCard(state: GameState) {
    PixelCard(Modifier.fillMaxWidth(), background = Panel.copy(alpha = 0.72f)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SectionLabel("Journal")
            Text("3 récents", fontSize = 6.sp, color = Muted2)
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
private fun GarminCard(ui: UiState, onOpenSettings: () -> Unit, onSync: () -> Unit) {
    PixelCard(
        Modifier.fillMaxWidth(),
        borderColor = if (ui.connected) Green.copy(alpha = 0.25f) else Line,
        background = Panel.copy(alpha = 0.78f)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Compte Garmin")
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (ui.connected) Green else Blood)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                if (ui.connected) "Connecté" else "Non connecté",
                fontSize = 6.5.sp,
                color = if (ui.connected) Green else Blood
            )
        }
        Spacer(Modifier.height(4.dp))
        if (ui.connected) {
            Text(
                "Dernière sync · ${formatTime(ui.lastSync)}" +
                    (ui.keyExpiry?.let { " · clé ${formatDate(it)}" } ?: ""),
                fontSize = 7.sp,
                color = Muted
            )
            val days = ui.keyExpiry?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) }
            if (days != null && days <= 30) {
                Text(
                    if (days < 0) "Clé Garmin expirée — remplacez-la." else "Clé Garmin expire dans $days jour(s).",
                    fontSize = 6.5.sp,
                    color = Blood
                )
            }
            if (ui.lastError.isNotBlank()) {
                Text(ui.lastError, fontSize = 6.5.sp, color = Blood, maxLines = 2)
            }
            Spacer(Modifier.height(5.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                SmallAction("Synchroniser", Modifier.weight(1f), onSync)
                SmallAction("Gérer la clé", Modifier.weight(1f), onOpenSettings)
            }
        } else {
            Text(
                "Connectez votre compte Garmin avec une clé Personal API. La 2FA reste chez Garmin.",
                fontSize = 7.sp,
                color = Muted,
                lineHeight = 10.sp
            )
            Spacer(Modifier.height(5.dp))
            SmallAction("Connecter Garmin", Modifier.fillMaxWidth(), onOpenSettings, primary = true)
        }
    }
}

@Composable
private fun SmallAction(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    primary: Boolean = false
) {
    Box(
        modifier
            .height(30.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (primary) Gold else Panel2)
            .border(1.dp, if (primary) GoldLight else Line2, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            color = if (primary) Ink else Parchment
        )
    }
}

@Composable
private fun InventoryTab(state: GameState, onSelectItem: (Item) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("ÉQUIPEMENT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Parchment)
        Spacer(Modifier.weight(1f))
        Text("Puissance ${powerOf(state)}", fontSize = 6.5.sp, color = Muted)
    }
    Spacer(Modifier.height(5.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf("weapon", "head", "body", "hands", "feet", "talisman").forEach { slot ->
            val item = equipped(state, slot)
            Column(
                Modifier
                    .weight(1f)
                    .height(64.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Panel)
                    .border(
                        1.dp,
                        if (item != null) rarityColor(item.rarity).copy(alpha = 0.65f) else Line,
                        RoundedCornerShape(4.dp)
                    )
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
    Spacer(Modifier.height(9.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("BUTIN", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Parchment)
        Spacer(Modifier.weight(1f))
        Text("${state.inventory.size} objets", fontSize = 6.sp, color = Muted)
    }
    Spacer(Modifier.height(4.dp))
    state.inventory.chunked(3).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            row.forEach { item ->
                InventoryCard(item, Modifier.weight(1f)) { onSelectItem(item) }
            }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(4.dp))
    }
    Text(
        "Touchez un objet pour comparer avant de l'équiper.",
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        fontSize = 6.sp,
        color = Muted2
    )
}

@Composable
private fun InventoryCard(item: Item, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .height(62.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Panel)
            .border(1.dp, rarityColor(item.rarity).copy(alpha = 0.62f), RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 3.dp, horizontal = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AssetIcon(item.icon, 27.dp)
        Text(
            item.name,
            fontSize = 6.sp,
            color = Parchment.copy(alpha = 0.85f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(rarityLabel(item.rarity), fontSize = 4.5.sp, color = rarityColor(item.rarity), maxLines = 1)
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
                    .width(62.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (afford) Gold else Panel3)
                    .border(1.dp, if (afford) GoldLight else Line2, RoundedCornerShape(3.dp))
                    .clickable(enabled = afford) { onBuy(key) },
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
    onOpenSettings: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xFF100B19), Color(0xFF0C0813))))
            .border(1.dp, Gold.copy(alpha = 0.10f), RoundedCornerShape(0.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
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
        when {
            !ui.connected -> GoldButton(
                text = "CONNECTER GARMIN",
                subtitle = "10 pas = 1 m · Personal API",
                onClick = onOpenSettings
            )
            ui.syncing -> GoldButton(
                text = "SYNCHRONISATION…",
                subtitle = "Garmin Personal API",
                enabled = false,
                onClick = {}
            )
            else -> GoldButton(
                text = "SYNCHRONISER GARMIN",
                subtitle = "Garmin · ${formatInt(state.stepsToday)} pas aujourd'hui",
                badge = if (ui.lastAdded > 0) "+${formatInt(ui.lastAdded)}" else null,
                onClick = onSync
            )
        }
    }
}
