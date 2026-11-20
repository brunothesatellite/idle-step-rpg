package com.brunothesatellite.idlesteprpg.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.view.WindowManager
import androidx.browser.customtabs.CustomTabsIntent
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.brunothesatellite.idlesteprpg.BuildConfig
import com.brunothesatellite.idlesteprpg.GameViewModel
import com.brunothesatellite.idlesteprpg.UiState
import com.brunothesatellite.idlesteprpg.data.SaveSummary
import com.brunothesatellite.idlesteprpg.engine.CONSUMABLES
import com.brunothesatellite.idlesteprpg.engine.ChestData
import com.brunothesatellite.idlesteprpg.engine.GameEngine
import com.brunothesatellite.idlesteprpg.engine.GameState
import com.brunothesatellite.idlesteprpg.engine.Item
import com.brunothesatellite.idlesteprpg.engine.JournalEntry
import com.brunothesatellite.idlesteprpg.engine.OfflineReport
import com.brunothesatellite.idlesteprpg.engine.SpecialData
import com.brunothesatellite.idlesteprpg.engine.SpecialOutcome
import com.brunothesatellite.idlesteprpg.engine.chestRemainingMs
import com.brunothesatellite.idlesteprpg.engine.consumableContextLabel
import com.brunothesatellite.idlesteprpg.engine.effectDescription
import com.brunothesatellite.idlesteprpg.engine.effectLabel
import com.brunothesatellite.idlesteprpg.engine.formatRemaining
import com.brunothesatellite.idlesteprpg.engine.discoveredSetSlots
import com.brunothesatellite.idlesteprpg.engine.setBonusDescription
import com.brunothesatellite.idlesteprpg.engine.setLabel
import com.brunothesatellite.idlesteprpg.engine.equipped
import com.brunothesatellite.idlesteprpg.engine.powerOf
import com.brunothesatellite.idlesteprpg.engine.rarityLabel
import com.brunothesatellite.idlesteprpg.engine.slotLabel
import com.brunothesatellite.idlesteprpg.engine.statLine
import com.brunothesatellite.idlesteprpg.engine.statsOf
import com.brunothesatellite.idlesteprpg.engine.upgradeCost
import com.brunothesatellite.idlesteprpg.engine.upgradeLevel
import com.brunothesatellite.idlesteprpg.engine.upgradeName
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun ItemDialog(
    item: Item,
    state: GameState,
    onDismiss: () -> Unit,
    onEquip: () -> Unit,
    onSell: () -> Unit,
    onToggleLock: () -> Unit,
    onFuse: () -> Unit,
    onIdentify: () -> Unit,
    onReroll: () -> Unit,
    onUpgrade: () -> Unit,
    onDismantle: () -> Unit
) {
    val equippedItem = equipped(state, item.slot)
    val equippedHere = state.equipment.any { it.id == item.id }
    val fusionCount = state.inventory.count {
        it.slot == item.slot && it.rarity == item.rarity && !it.locked && it.identified
    }
    val upgradePrice = GameEngine.upgradeCost(item)
    val missingGold = (upgradePrice - state.gold).coerceAtLeast(0)
    val missingShards = (1 - state.shards).coerceAtLeast(0)
    val canUpgrade = item.identified && missingGold == 0 && missingShards == 0
    val upgradeHint = when {
        !item.identified -> "Objet non identifié."
        canUpgrade -> "Forge · ${formatInt(upgradePrice)} or + 1 éclat"
        else -> "Il manque " + buildList {
            if (missingGold > 0) add("${formatInt(missingGold)} or")
            if (missingShards > 0) add(if (missingShards > 1) "$missingShards éclats" else "1 éclat")
        }.joinToString(" et ") + " pour améliorer"
    }
    val equippedSetCount = state.equipment.count { it.setName == item.setName }
    val collectionSetCount = discoveredSetSlots(state, item.setName).size
    AppDialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF241930), Color(0xFF130E1C))))
                    .border(1.dp, Gold.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Panel2)
                            .border(1.dp, rarityColor(item.rarity), RoundedCornerShape(5.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AssetImage(item.icon, Modifier.size(36.dp))
                    }
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (item.identified) rarityLabel(item.rarity) else "NON IDENTIFIÉ",
                            fontSize = 5.5.sp,
                            letterSpacing = 1.sp,
                            color = if (item.identified) rarityColor(item.rarity) else Ember
                        )
                        Text(
                            if (item.identified) item.name else "Objet mystérieux",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Parchment
                        )
                        Text(
                            "${slotLabel(item.slot)} · Niv. ${item.level}" +
                                if (item.locked) " · VERROUILLÉ" else "",
                            fontSize = 7.sp,
                            color = if (item.locked) Ember else Muted
                        )
                        if (item.identified && item.effect.isNotBlank()) {
                            Text(effectLabel(item.effect), fontSize = 6.sp, color = Legendary)
                        }
                        if (item.identified && item.setName.isNotBlank()) {
                            Text(
                                "${setLabel(item.setName)} · équipé $equippedSetCount/6 · collection $collectionSetCount/6",
                                fontSize = 5.8.sp,
                                color = Blue
                            )
                        }
                    }
                    CloseButton(onDismiss, size = 28.dp, bordered = false)
                }
                Spacer(Modifier.height(8.dp))
                DividerLine()
                Spacer(Modifier.height(6.dp))
                if (!item.identified) {
                    Text(
                        "Un éclat d'âme suffit pour révéler les statistiques, l'effet et l'ensemble de cet objet.",
                        fontSize = 7.sp,
                        color = Muted,
                        lineHeight = 10.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    OverlayAction("IDENTIFIER · 1 ÉCLAT", Modifier.fillMaxWidth(), onIdentify, primary = true)
                    Spacer(Modifier.height(5.dp))
                    OverlayAction(
                        if (item.locked) "VERROUILLÉ" else "VENDRE +${formatInt(item.value)} or",
                        Modifier.fillMaxWidth(),
                        onSell,
                        enabled = !item.locked
                    )
                } else {
                    StatRow("ATQ", if (item.atk != 0) "+${item.atk}" else "—")
                    StatRow("PV", if (item.hp != 0) "+${item.hp}" else "—")
                    StatRow("% VIT", if (item.vitPct != 0) "+${item.vitPct}" else "—")
                    StatRow("% critique", if (item.critPct != 0) "+${item.critPct}" else "—")
                    StatRow("Régénération", if (item.regen != 0.0) "+${"%.1f".format(item.regen)}" else "—")
                    StatRow("Valeur", "${formatInt(item.value)} or")
                    Spacer(Modifier.height(4.dp))
                    val nextLevel = GameEngine.upgradedItem(item)
                    val reference = if (equippedHere) item else equippedItem
                    val leftTitle = if (equippedHere) "ACTUEL" else "ÉQUIPÉ"
                    val rightTitle = if (equippedHere) "APRÈS +1 NIV." else "CET OBJET"
                    val leftLine = when {
                        equippedHere -> statLine(item)
                        equippedItem != null -> statLine(equippedItem)
                        else -> "Emplacement vide"
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(Panel)
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(leftTitle, fontSize = 5.5.sp, color = Muted2)
                            Text(
                                leftLine,
                                fontSize = 6.5.sp,
                                color = Parchment.copy(alpha = 0.75f),
                                maxLines = 3
                            )
                        }
                        Text("→", color = Gold, fontSize = 12.sp)
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text(rightTitle, fontSize = 5.5.sp, color = Muted2)
                            Text(
                                comparisonLine(if (equippedHere) nextLevel else item, reference),
                                fontSize = 6.5.sp,
                                maxLines = 3,
                                textAlign = TextAlign.End
                            )
                        }
                    }
                    if (item.identified && item.setName.isNotBlank()) {
                        Spacer(Modifier.height(5.dp))
                        val tier = when {
                            equippedSetCount >= 6 -> 6
                            equippedSetCount >= 4 -> 4
                            else -> 2
                        }
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(Blue.copy(alpha = 0.08f))
                                .border(1.dp, Blue.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .padding(6.dp)
                        ) {
                            Text(
                                "${setLabel(item.setName)} · équipé $equippedSetCount/6 · collection $collectionSetCount/6",
                                fontSize = 6.sp,
                                letterSpacing = 0.5.sp,
                                color = Blue
                            )
                            Text(
                                if (equippedSetCount >= 2) setBonusDescription(item.setName, tier)
                                else "2 pièces équipées : ${setBonusDescription(item.setName, 2)}",
                                fontSize = 6.3.sp,
                                color = Parchment.copy(alpha = 0.8f)
                            )
                        }
                    }
                    if (item.effect.isNotBlank()) {
                        Spacer(Modifier.height(5.dp))
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(Legendary.copy(alpha = 0.08f))
                                .border(1.dp, Legendary.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .padding(6.dp)
                        ) {
                            Text(effectLabel(item.effect), fontSize = 6.sp, letterSpacing = 0.6.sp, color = Legendary)
                            Text(effectDescription(item.effect), fontSize = 6.5.sp, color = Parchment.copy(alpha = 0.8f))
                        }
                    }
                    Spacer(Modifier.height(9.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OverlayAction(
                            if (item.locked) "DÉVERROUILLER" else "VERROUILLER",
                            Modifier.weight(1f),
                            onToggleLock
                        )
                        OverlayAction(
                            "FUSIONNER $fusionCount/3",
                            Modifier.weight(1f),
                            onFuse,
                            enabled = fusionCount >= 3 && !item.locked && !equippedHere
                        )
                    }
                    if (state.bosses > 0) {
                        Spacer(Modifier.height(5.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            OverlayAction(
                                "REFORGER · 2",
                                Modifier.weight(1f),
                                onReroll,
                                enabled = item.identified && state.shards >= 2
                            )
                            OverlayAction(
                                "AMÉLIORER · ${formatInt(upgradePrice)}",
                                Modifier.weight(1.2f),
                                onUpgrade,
                                enabled = canUpgrade
                            )
                            OverlayAction(
                                "DÉMONTER",
                                Modifier.weight(0.9f),
                                onDismantle,
                                danger = true,
                                enabled = !item.locked && !equippedHere
                            )
                        }
                        Text(
                            upgradeHint,
                            fontSize = 6.sp,
                            color = if (canUpgrade) Muted else Ember,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    } else {
                        Text(
                            "La forge se débloque après le premier gardien vaincu.",
                            fontSize = 6.sp,
                            color = Muted2,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OverlayAction(
                            if (item.locked) "VERROUILLÉ"
                            else "VENDRE +${formatInt(item.value)} or",
                            Modifier.weight(0.85f),
                            onSell,
                            enabled = !item.locked && !equippedHere
                        )
                        OverlayAction(
                            if (equippedHere) "ÉQUIPÉ" else "ÉQUIPER",
                            Modifier.weight(1.15f),
                            onEquip,
                            primary = true,
                            enabled = !equippedHere
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 8.sp, color = Muted, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(6.dp))
        Text(value, fontSize = 8.5.sp, color = Parchment, textAlign = TextAlign.End)
    }
}

private data class StatEntry(val label: String, val value: Double, val text: String)

private fun statEntries(item: Item): List<StatEntry> = buildList {
    if (item.atk != 0) add(StatEntry("ATQ", item.atk.toDouble(), "+${item.atk}"))
    if (item.hp != 0) add(StatEntry("PV", item.hp.toDouble(), "+${item.hp}"))
    if (item.vitPct != 0) add(StatEntry("% VIT", item.vitPct.toDouble(), "+${item.vitPct}"))
    if (item.critPct != 0) add(StatEntry("% crit.", item.critPct.toDouble(), "+${item.critPct}"))
    if (item.regen != 0.0) add(StatEntry("PV/s", item.regen, "+${"%.1f".format(item.regen)}"))
}

private fun formatDelta(delta: Double): String =
    if (delta % 1.0 == 0.0) delta.toInt().toString() else "%.1f".format(delta)

/**
 * Ligne de comparaison colorée : vert quand la valeur monte, rouge quand elle
 * baisse, gris quand elle ne bouge pas. [reference] null = aucune comparaison.
 */
private fun comparisonLine(target: Item, reference: Item?): AnnotatedString {
    val previous = reference?.let { ref -> statEntries(ref).associate { it.label to it.value } }.orEmpty()
    return buildAnnotatedString {
        statEntries(target).forEachIndexed { index, entry ->
            if (index > 0) append(" · ")
            val before = previous[entry.label]
            val color = when {
                before == null -> GoldLight
                entry.value > before -> Green
                entry.value < before -> Bad
                else -> Parchment.copy(alpha = 0.65f)
            }
            withStyle(SpanStyle(color = color)) {
                append(entry.text)
                append(" ")
                append(entry.label)
                if (before != null) {
                    val delta = entry.value - before
                    when {
                        delta > 0 -> append(" ▲+${formatDelta(delta)}")
                        delta < 0 -> append(" ▼${formatDelta(delta)}")
                        else -> append(" =")
                    }
                }
            }
        }
    }
}

@Composable
fun ChestDialog(
    chest: ChestData,
    state: GameState,
    onForce: () -> Unit,
    onStop: () -> Unit,
    onDismiss: () -> Unit
) {
    val forcing = state.mode == "chest"
    AppDialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxWidth().padding(22.dp), contentAlignment = Alignment.Center) {
            Box {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF241930), Color(0xFF130E1C))))
                        .border(1.dp, Gold.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ChestArt(Modifier.size(78.dp))
                Spacer(Modifier.height(4.dp))
                Text(GameEngine.chestTypeLabel(chest.type), fontSize = 5.5.sp, letterSpacing = 1.sp, color = Gold)
                Text("Serrure de ${formatInt(chest.quota)} pas", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Parchment)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Consacrer vos pas fige la marche jusqu'à l'ouverture. La progression de la serrure est conservée.",
                    fontSize = 7.5.sp,
                    color = Muted,
                    textAlign = TextAlign.Center,
                    lineHeight = 10.sp
                )
                Spacer(Modifier.height(8.dp))
                MiniBar(
                    fraction = (chest.progress / chest.quota).toFloat(),
                    color = Gold,
                    modifier = Modifier.fillMaxWidth(),
                    height = 7.dp
                )
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${formatInt(chest.progress.toInt())} / ${formatInt(chest.quota)} pas", fontSize = 6.5.sp, color = GoldLight)
                    Text(
                        when (chest.type) {
                            "bois" -> "40–80 or · objet inhabituel"
                            "fer" -> "130–220 or · objet rare"
                            else -> "380–550 or · objet épique"
                        },
                        fontSize = 6.sp,
                        color = Muted
                    )
                }
                val remaining = chestRemainingMs(chest)
                Row(Modifier.fillMaxWidth().padding(top = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "Expire dans ${formatRemaining(remaining)}",
                        fontSize = 6.5.sp,
                        color = if (remaining < 60 * 60 * 1000L) Blood else Ember
                    )
                    if (state.chestQueue.isNotEmpty()) {
                        Text("+${state.chestQueue.size} coffre en attente", fontSize = 6.5.sp, color = Gold)
                    }
                }
                Spacer(Modifier.height(9.dp))
                if (forcing) {
                    OverlayAction(
                        if (state.stepBank > 0) "FORÇAGE EN COURS…" else "EN ATTENTE DE PAS",
                        Modifier.fillMaxWidth(),
                        {},
                        primary = true,
                        enabled = false
                    )
                    Spacer(Modifier.height(4.dp))
                    OverlayAction("ARRÊTER LE FORÇAGE", Modifier.fillMaxWidth(), {
                        onStop()
                        onDismiss()
                    })
                } else {
                    OverlayAction(
                        if (chest.progress > 0) "REPRENDRE LE FORÇAGE" else "CONSACRER MES PAS",
                        Modifier.fillMaxWidth(),
                        onForce,
                        primary = true
                    )
                }
                Spacer(Modifier.height(4.dp))
                OverlayAction(
                    if (forcing) "MASQUER · LE FORÇAGE CONTINUE" else "CONTINUER LA MARCHE",
                    Modifier.fillMaxWidth(),
                    onDismiss
                )
                }
                CloseButton(onDismiss, Modifier.align(Alignment.TopEnd).padding(6.dp))
            }
        }
    }
}

@Composable
fun CampOverlay(
    state: GameState,
    onRest: () -> Unit,
    onShard: () -> Unit,
    onFreeRest: () -> Unit,
    onWithdrawStash: () -> Unit,
    onOpenJournal: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenConsumables: () -> Unit,
    onResume: () -> Unit
) {
    val stats = statsOf(state)
    var cardHeight by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val heroBottom = with(density) { cardHeight.toDp() } + 16.dp
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF08061A), Color(0xFF15102A), Color(0xFF2A1A2E))))
            .clickable { }
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Le panneau d'actions a une hauteur variable (taille du texte choisie) :
            // tente, sapins et feu sont ancrés sur sa ligne de sol, comme le héros,
            // pour rester visibles à toutes les tailles.
            val panel = if (cardHeight > 0) cardHeight.toFloat() else h * 0.45f
            val ground = (h - panel - 16.dp.toPx()).coerceIn(h * 0.30f, h * 0.86f)
            val sceneTop = 120.dp.toPx()
            val sceneH = (ground - sceneTop).coerceAtLeast(100.dp.toPx())

            drawCircle(Color(0xFFF4E6C8), radius = 17.dp.toPx(), center = Offset(w * 0.86f, h * 0.08f))
            drawCircle(Color(0x22F4E6C8), radius = 55.dp.toPx(), center = Offset(w * 0.86f, h * 0.08f))
            val random = java.util.Random(7)
            repeat(40) {
                drawCircle(
                    Color(0x99E8DCC4),
                    radius = if (random.nextBoolean()) 1.dp.toPx() else 1.5.dp.toPx(),
                    center = Offset(random.nextFloat() * w, random.nextFloat() * (h * 0.70f).coerceAtMost(ground))
                )
            }
            // sapins
            drawPath(
                Path().apply {
                    moveTo(w * 0.04f, ground)
                    lineTo(w * 0.17f, ground - sceneH * 0.85f)
                    lineTo(w * 0.30f, ground)
                    close()
                },
                Color(0xFF0C1018)
            )
            drawPath(
                Path().apply {
                    moveTo(w * 0.70f, ground)
                    lineTo(w * 0.84f, ground - sceneH * 0.88f)
                    lineTo(w * 0.98f, ground)
                    close()
                },
                Color(0xFF0C1018)
            )
            // tente
            val tentH = (sceneH * 0.72f).coerceAtMost(190.dp.toPx())
            drawPath(
                Path().apply {
                    moveTo(w * 0.26f, ground)
                    lineTo(w * 0.42f, ground - tentH)
                    lineTo(w * 0.58f, ground)
                    close()
                },
                Color(0xFF3A2B27)
            )
            drawPath(
                Path().apply {
                    moveTo(w * 0.38f, ground)
                    lineTo(w * 0.42f, ground - tentH * 0.42f)
                    lineTo(w * 0.46f, ground)
                    close()
                },
                Color(0xFF09060D)
            )
            // feu
            val fx = w * 0.57f
            val fy = ground - 2.dp.toPx()
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0x77FF9448), Color(0x22FF9448), Color.Transparent),
                    center = Offset(fx, fy),
                    radius = 120.dp.toPx()
                ),
                radius = 120.dp.toPx(),
                center = Offset(fx, fy)
            )
            drawOval(
                Color(0xFF3A2112),
                topLeft = Offset(fx - 26.dp.toPx(), fy + 12.dp.toPx()),
                size = Size(52.dp.toPx(), 12.dp.toPx())
            )
            drawOval(
                Color(0xFF5B341D),
                topLeft = Offset(fx - 15.dp.toPx(), fy + 4.dp.toPx()),
                size = Size(30.dp.toPx(), 22.dp.toPx())
            )
            val flame = Path().apply {
                moveTo(fx, fy - 26.dp.toPx())
                quadraticBezierTo(fx + 14.dp.toPx(), fy - 4.dp.toPx(), fx + 6.dp.toPx(), fy + 12.dp.toPx())
                quadraticBezierTo(fx, fy + 18.dp.toPx(), fx - 8.dp.toPx(), fy + 8.dp.toPx())
                quadraticBezierTo(fx - 13.dp.toPx(), fy - 6.dp.toPx(), fx, fy - 26.dp.toPx())
                close()
            }
            drawPath(flame, Brush.verticalGradient(listOf(GoldLight, Ember, Color(0xFF8F2D19))))
        }

        Column(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("CAMPEMENT", fontFamily = Press, fontSize = 10.sp, color = GoldLight)
            Text("Le feu veille", fontSize = 7.5.sp, letterSpacing = 1.sp, color = Muted)
            Text("L'expédition est en pause", fontSize = 6.2.sp, color = Muted2)
        }

        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 96.dp, bottom = heroBottom)
                .width(66.dp)
                .height(10.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.45f))
        )
        AssetImage(
            "hero/human.png",
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 96.dp, bottom = heroBottom)
                .size(88.dp)
        )

        CloseButton(
            onResume,
            Modifier
                .align(Alignment.TopStart)
                .padding(top = 40.dp, start = 14.dp),
            background = Ink.copy(alpha = 0.85f)
        )

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged { cardHeight = it.height }
                .padding(14.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Ink.copy(alpha = 0.94f))
                .border(1.dp, Line2, RoundedCornerShape(8.dp))
                .padding(10.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                CampSummary("PALIER", state.floor.toString().padStart(2, '0'))
                CampSummary("DISTANCE", "${state.distance.toInt()} m")
                CampSummary("FERVEUR", state.ferveur.toString())
                CampSummary("PV", "${state.hp.toInt()} / ${stats.maxHp}")
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                CampSummary("PAS EN ATTENTE", formatInt(state.stepBank.toInt()))
                CampSummary("OR", formatInt(state.gold))
                CampSummary("SÉRIE", "${state.streak.coerceAtLeast(1)} j")
                CampSummary("TOTAL", formatInt(state.totalSteps))
            }
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CurrencyChip("item/meat_ration.png", state.rations.toString())
                CurrencyChip("item/misc_crystal.png", state.shards.toString())
            }
            Spacer(Modifier.height(7.dp))
            CampAction(
                "Se reposer",
                "+40 % PV · 1 ration",
                "item/meat_ration.png",
                state.rations.toString(),
                enabled = state.rations > 0 && state.hp < stats.maxHp,
                onClick = onRest
            )
            Spacer(Modifier.height(4.dp))
            CampAction(
                "Se ressourcer",
                "PV complets · 1 éclat",
                "item/misc_crystal.png",
                state.shards.toString(),
                enabled = state.shards > 0 && state.hp < stats.maxHp,
                onClick = onShard
            )
            Spacer(Modifier.height(4.dp))
            CampAction(
                "Veillée gratuite",
                "+15 % PV · une fois par jour",
                "fx/heart.png",
                if (state.freeRestDay == state.day) "UTILISÉE" else "GRATUIT",
                enabled = state.freeRestDay != state.day && state.hp < stats.maxHp,
                onClick = onFreeRest
            )
            if (state.stash.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                CampAction(
                    "Coffre de camp",
                    "Récupérer les objets en réserve",
                    "item/misc_crystal.png",
                    state.stash.size.toString(),
                    enabled = state.inventory.size < 24,
                    onClick = onWithdrawStash
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                OverlayAction("JOURNAL", Modifier.weight(1f), onOpenJournal)
                OverlayAction("INVENTAIRE", Modifier.weight(1f), onOpenInventory)
                OverlayAction("POTIONS", Modifier.weight(1f), onOpenConsumables)
            }
            Spacer(Modifier.height(7.dp))
            GoldButton("REPRENDRE L'EXPÉDITION", onClick = onResume)
            Spacer(Modifier.height(6.dp))
            Text(
                "« La défaite n'est qu'un détour. Vingt mètres en arrière, l'or coule encore. »",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 6.5.sp,
                color = Muted2,
                lineHeight = 9.sp
            )
        }
    }
}

@Composable
private fun CampSummary(label: String, value: String) {
    Column {
        Text(label, fontSize = 5.sp, letterSpacing = 0.7.sp, color = Muted2)
        Text(value, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Parchment)
    }
}

@Composable
private fun CampAction(
    title: String,
    subtitle: String,
    icon: String,
    count: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(5.dp))
            .background(Panel2)
            .border(1.dp, Line, RoundedCornerShape(5.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AssetIcon(icon, 22.dp)
        Spacer(Modifier.width(7.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (enabled) Parchment else Muted2)
            Text(subtitle, fontSize = 6.5.sp, color = Muted)
        }
        Text(count, fontSize = 9.sp, color = Gold)
    }
}

@Composable
fun GarminScreen(vm: GameViewModel, ui: UiState, onClose: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    var key by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    var expiry by remember { mutableStateOf(ui.keyExpiry ?: LocalDate.now().plusYears(1)) }
    var confirmRevoke by remember { mutableStateOf(false) }

    LaunchedEffect(ui.keyExpiry) {
        if (ui.keyExpiry != null) expiry = ui.keyExpiry
    }

    DisposableEffect(Unit) {
        val window = context.findActivity()?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0B0810), Color(0xFF171121), Color(0xFF0B0810))))
            .clickable { }
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("COMPTE GARMIN", fontFamily = Press, fontSize = 9.sp, color = GoldLight)
                Text("Personal API · lecture seule", fontSize = 7.sp, color = Muted)
            }
            CloseButton(onClose)
        }
        DividerLine()
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(14.dp)
        ) {
            PixelCard(Modifier.fillMaxWidth(), borderColor = if (ui.connected) Green.copy(alpha = 0.4f) else Line) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(if (ui.connected) Green else Blood))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (ui.connected) "Connecté à Garmin" else "Aucune clé enregistrée",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (ui.connected) Green else Blood
                    )
                }
                Spacer(Modifier.height(5.dp))
                GarminInfoRow("Dernière synchronisation", formatTime(ui.lastSync))
                GarminInfoRow("Clé visible jusqu'au", formatDate(expiry))
                GarminInfoRow("Jeton OAuth2", formatTokenTime(ui.tokenExpiry))
                if (ui.keyExpiry != null) {
                    val days = ChronoUnit.DAYS.between(LocalDate.now(), ui.keyExpiry)
                    GarminInfoRow(
                        "Expiration de la clé",
                        if (days < 0) "EXPIRÉE" else "dans $days jour(s)",
                        if (days < 0) Blood else if (days <= 30) Ember else Parchment
                    )
                }
                if (ui.lastError.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(ui.lastError, fontSize = 7.sp, color = Blood, lineHeight = 10.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "La 2FA Garmin se fait dans le navigateur. L'application ne voit jamais votre mot de passe ni votre code 2FA.",
                fontSize = 7.5.sp,
                color = Muted,
                lineHeight = 11.sp
            )
            Spacer(Modifier.height(8.dp))
            OverlayAction(
                "OUVRIR GARMIN CONNECT",
                Modifier.fillMaxWidth(),
                onClick = {
                    val intent = CustomTabsIntent.Builder().setShowTitle(true).build()
                    intent.launchUrl(context, Uri.parse("https://connect.garmin.com/app/settings/personalApiToken"))
                }
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Connectez-vous, créez une clé Personal API (elle ne s'affiche qu'une fois), copiez-la, puis collez-la ci-dessous.",
                fontSize = 7.sp,
                color = Muted2,
                lineHeight = 10.sp
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = key,
                onValueChange = { key = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Clé personnelle Garmin (GCDC_…)", fontSize = 7.sp) },
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    Text(
                        if (showKey) "CACHER" else "VOIR",
                        modifier = Modifier.clickable { showKey = !showKey }.padding(6.dp),
                        fontSize = 6.sp,
                        color = Gold
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Gold,
                    unfocusedBorderColor = Line2,
                    focusedTextColor = Parchment,
                    unfocusedTextColor = Parchment,
                    focusedLabelColor = Gold,
                    unfocusedLabelColor = Muted,
                    cursorColor = Gold
                )
            )
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(5.dp))
                    .background(Panel)
                    .border(1.dp, Line, RoundedCornerShape(5.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Date d'expiration de la clé", fontSize = 9.sp, color = Parchment)
                    Text(
                        "Garmin ne l'expose pas via l'API. Renseignez-la pour les rappels.",
                        fontSize = 6.5.sp,
                        color = Muted
                    )
                }
                Text(
                    formatDate(expiry),
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(Panel2)
                        .clickable {
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    expiry = LocalDate.of(year, month + 1, day)
                                    if (ui.connected) vm.updateExpiry(expiry)
                                },
                                expiry.year,
                                expiry.monthValue - 1,
                                expiry.dayOfMonth
                            ).show()
                        }
                        .padding(horizontal = 7.dp, vertical = 5.dp),
                    fontSize = 8.sp,
                    color = GoldLight
                )
            }
            Spacer(Modifier.height(10.dp))
            GoldButton(
                text = if (ui.syncing) "VALIDATION…" else "TESTER ET ENREGISTRER",
                subtitle = "Échange la clé contre un jeton OAuth2",
                enabled = key.isNotBlank() && !ui.syncing,
                onClick = {
                    vm.saveKey(key, expiry)
                }
            )
            if (ui.connected) {
                Spacer(Modifier.height(5.dp))
                OverlayAction("SYNCHRONISER MAINTENANT", Modifier.fillMaxWidth(), { vm.syncNow() })
            }
            if (BuildConfig.DEBUG) {
                Spacer(Modifier.height(8.dp))
                OverlayAction("AJOUTER 500 PAS (DÉMO)", Modifier.fillMaxWidth(), { vm.debugAddSteps(500) })
            }
            Spacer(Modifier.height(12.dp))
            if (ui.connected) {
                OverlayAction(
                    "RÉVOQUER LA CLÉ ET EFFACER",
                    Modifier.fillMaxWidth(),
                    { confirmRevoke = true },
                    danger = true
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "La clé est chiffrée dans Android Keystore. Elle n'est jamais envoyée à un serveur de l'application ; seules les requêtes HTTPS vers Garmin l'utilisent.",
                fontSize = 6.5.sp,
                color = Muted2,
                lineHeight = 9.sp
            )
            Spacer(Modifier.height(20.dp))
        }
    }

    if (confirmRevoke) {
        AlertDialog(
            onDismissRequest = { confirmRevoke = false },
            title = { WithAppFontScale { Text("Révoquer la clé ?", fontSize = 15.sp) } },
            text = {
                WithAppFontScale {
                    Text(
                        "La clé et le jeton seront supprimés de l'appareil. La progression du jeu est conservée.",
                        fontSize = 9.sp
                    )
                }
            },
            confirmButton = {
                WithAppFontScale {
                    TextButton(onClick = {
                        confirmRevoke = false
                        vm.revokeGarmin()
                    }) { Text("RÉVOQUER", color = Blood) }
                }
            },
            dismissButton = {
                WithAppFontScale {
                    TextButton(onClick = { confirmRevoke = false }) { Text("ANNULER", color = Muted) }
                }
            },
            containerColor = Panel2
        )
    }
}

@Composable
private fun GarminInfoRow(label: String, value: String, valueColor: Color = Parchment) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 7.sp, color = Muted)
        Text(value, fontSize = 7.sp, color = valueColor)
    }
}

@Composable
fun JournalOverlay(
    journal: List<JournalEntry>,
    legend: List<JournalEntry>,
    bestiary: Map<String, Int>,
    onClose: () -> Unit
) {
    var tab by remember { mutableStateOf("journal") }
    var filter by remember { mutableStateOf("all") }
    val filtered = when (filter) {
        "good" -> journal.filter { it.type == "good" }
        "bad" -> journal.filter { it.type == "bad" }
        "neutral" -> journal.filter { it.type == "neutral" }
        else -> journal
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0B0810), Color(0xFF171121), Color(0xFF0B0810))))
            .statusBarsPadding()
            .navigationBarsPadding()
            .clickable { }
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("JOURNAL DE LA MARCHE", fontFamily = Press, fontSize = 9.sp, color = GoldLight)
                    Text(
                        when (tab) {
                            "legend" -> "${legend.size} entrée(s) de légende"
                            "bestiary" -> "${bestiary.size} créature(s) connue(s)"
                            else -> "${journal.size} événements conservés"
                        },
                        fontSize = 7.sp,
                        color = Muted
                    )
                }
                    CloseButton(onClose)
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                listOf(
                    "journal" to "Journal",
                    "legend" to "Légende",
                    "bestiary" to "Bestiaire"
                ).forEach { (key, label) ->
                    val active = tab == key
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (active) Gold.copy(alpha = 0.16f) else Panel)
                            .border(1.dp, if (active) Gold.copy(alpha = 0.6f) else Line, RoundedCornerShape(4.dp))
                            .clickable { tab = key }
                            .padding(vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, fontSize = 6.5.sp, color = if (active) GoldLight else Muted, maxLines = 1)
                    }
                }
            }
            if (tab == "journal") {
                Spacer(Modifier.height(5.dp))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    listOf(
                        "all" to "Tous",
                        "good" to "Favorables",
                        "bad" to "Alertes",
                        "neutral" to "Neutres"
                    ).forEach { (key, label) ->
                        val active = filter == key
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (active) Gold.copy(alpha = 0.12f) else Panel.copy(alpha = 0.6f))
                                .border(1.dp, if (active) Gold.copy(alpha = 0.45f) else Line, RoundedCornerShape(4.dp))
                                .clickable { filter = key }
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, fontSize = 5.8.sp, color = if (active) GoldLight else Muted, maxLines = 1)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            DividerLine()
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                when (tab) {
                    "legend" -> {
                        if (legend.isEmpty()) {
                            EmptyJournal("Aucun jalon pour l'instant. Franchi un palier multiple de 10.")
                        }
                        legend.forEach { entry -> JournalRow(entry) }
                    }
                    "bestiary" -> {
                        if (bestiary.isEmpty()) {
                            EmptyJournal("Aucune créature vaincue pour l'instant.")
                        }
                        bestiary.entries.sortedByDescending { it.value }.forEach { (name, kills) ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(name, fontSize = 8.sp, color = Parchment)
                                Text("× $kills", fontFamily = Press, fontSize = 7.sp, color = Gold)
                            }
                            DividerLine()
                        }
                    }
                    else -> {
                        if (filtered.isEmpty()) {
                            EmptyJournal("Aucun événement dans ce filtre.")
                        }
                        filtered.forEach { entry -> JournalRow(entry) }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun EmptyJournal(text: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().padding(top = 30.dp),
        textAlign = TextAlign.Center,
        fontSize = 8.sp,
        color = Muted2
    )
}

@Composable
private fun JournalRow(entry: JournalEntry) {
    val color = when (entry.type) {
        "good" -> Green
        "bad" -> Blood
        else -> Gold
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(Modifier.padding(top = 4.dp).size(5.dp).background(color))
        Spacer(Modifier.width(7.dp))
        Column(Modifier.weight(1f)) {
            Text(
                entry.text,
                fontSize = 8.sp,
                color = Parchment.copy(alpha = 0.9f),
                lineHeight = 11.sp
            )
            Text(
                when (entry.type) {
                    "good" -> "FAVORABLE"
                    "bad" -> "ALERTE"
                    else -> "NOTE"
                },
                fontSize = 5.5.sp,
                color = color.copy(alpha = 0.8f),
                letterSpacing = 0.5.sp
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(entry.time, fontSize = 6.5.sp, color = Muted2)
    }
    DividerLine()
}

@Composable
fun OfflineOverlay(
    report: OfflineReport,
    onDismiss: () -> Unit,
    onOpenJournal: (() -> Unit)? = null
) {
    val absence = report.minutes > 0 || report.stepsConsumed > 0 ||
        report.reason == "absence" || report.reason == "mixed"
    val mixed = report.reason == "mixed" || (report.days > 0 && report.stepsConsumed > 0)
    AppDialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 620.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF241930), Color(0xFF130E1C))))
                    .border(1.dp, Gold.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (absence) "BON RETOUR" else "LE SENTIER A CONTINUÉ",
                    fontFamily = Press,
                    fontSize = 9.sp,
                    color = GoldLight
                )
                Text(
                    when {
                        mixed -> "Absence et rattrapage des pas"
                        absence -> "Pendant votre absence"
                        else -> "Récapitulatif hors ligne"
                    },
                    fontSize = 7.5.sp,
                    color = Muted
                )
                Spacer(Modifier.height(12.dp))
                if (absence) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RecapTile(formatAbsence(report.minutes), "ABSENCE", Modifier.weight(1f))
                        RecapTile(formatInt(report.stepsConsumed), "PAS CONSOMMÉS", Modifier.weight(1f))
                        RecapTile(formatInt(report.distanceMeters), "MÈTRES", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RecapTile(formatInt(report.combats), "COMBATS", Modifier.weight(1f))
                        RecapTile(formatInt(report.gold), "OR", Modifier.weight(1f))
                        RecapTile(formatInt(report.floors), "PALIERS", Modifier.weight(1f))
                        RecapTile(formatInt(report.bosses), "GARDIENS", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RecapTile(formatInt(report.levels), "NIVEAUX", Modifier.weight(1f))
                        RecapTile(formatInt(report.chests), "COFFRES", Modifier.weight(1f))
                        RecapTile(formatInt(report.remainingSteps), "RÉSERVE", Modifier.weight(1f))
                    }
                    if (mixed) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Synchro : ${report.days} jour(s) · ${formatInt(report.steps)} pas importés",
                            fontSize = 6.8.sp,
                            color = Gold,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RecapTile(report.days.toString(), "JOURS", Modifier.weight(1f))
                        RecapTile(formatInt(report.steps), "PAS", Modifier.weight(1f))
                        RecapTile(formatInt(report.distanceMeters), "MÈTRES", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RecapTile(formatInt(report.combats), "COMBATS", Modifier.weight(1f))
                        RecapTile(formatInt(report.gold), "OR", Modifier.weight(1f))
                        RecapTile(formatInt(report.floors), "PALIERS", Modifier.weight(1f))
                        RecapTile(formatInt(report.kills), "VICTIMES", Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    when {
                        mixed -> "Votre réserve de pas et les pas synchronisés ont été traités dans le même calcul. Le reste de la réserve vous attend."
                        report.stepsConsumed == 0 -> "La réserve de pas était vide à la fermeture : rien n'a avancé. Les pas de la synchronisation arrivent à la fermeture de cette fenêtre."
                        absence -> "Votre réserve de pas a été consommée pendant votre absence. Le reste de la réserve vous attend."
                        else -> "Vos pas des derniers jours ont été récupérés et ajoutés à la marche, sans double comptage."
                    },
                    fontSize = 7.sp,
                    color = Muted,
                    textAlign = TextAlign.Center,
                    lineHeight = 10.sp
                )
                val hasLoot = report.lootItems.isNotEmpty() || report.consumables > 0 ||
                    report.rations > 0 || report.shards > 0
                if (hasLoot) {
                    Spacer(Modifier.height(8.dp))
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(Panel2)
                            .border(1.dp, Gold.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                            .padding(7.dp)
                    ) {
                        Text("BUTIN", fontFamily = Press, fontSize = 6.sp, color = Gold)
                        val counts = buildList {
                            if (report.consumables > 0) add("${report.consumables} consommable(s)")
                            if (report.rations > 0) add("${report.rations} ration(s)")
                            if (report.shards > 0) add("${report.shards} éclat(s)")
                        }
                        if (counts.isNotEmpty()) {
                            Text(counts.joinToString(" · "), fontSize = 6.5.sp, color = Parchment)
                        }
                        report.lootItems.take(8).forEach { name ->
                            Text("• $name", fontSize = 6.2.sp, color = Green, maxLines = 1)
                        }
                        if (report.lootItems.size > 8) {
                            Text("… et ${report.lootItems.size - 8} autre(s)", fontSize = 6.sp, color = Muted)
                        }
                    }
                }
                if (report.special) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Une rencontre vous attend sur le sentier.",
                        fontSize = 7.sp,
                        color = GoldLight,
                        textAlign = TextAlign.Center
                    )
                }
                if (report.defeated) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Le héros a été repoussé et a repris des forces.",
                        fontSize = 7.sp,
                        color = Blood,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(12.dp))
                GoldButton("REPRENDRE LA MARCHE", onClick = onDismiss)
                if (onOpenJournal != null) {
                    TextButton(onClick = onOpenJournal) {
                        Text("VOIR LE JOURNAL", fontSize = 7.sp, color = Muted)
                    }
                }
            }
        }
    }
}

private fun formatAbsence(minutes: Int): String =
    if (minutes >= 60) "${minutes / 60} h ${minutes % 60}" else "$minutes min"

@Composable
private fun RecapTile(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(5.dp))
            .background(Panel)
            .border(1.dp, Line, RoundedCornerShape(5.dp))
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontFamily = Press, fontSize = 8.sp, color = GoldLight, maxLines = 1)
        Text(label, fontSize = 5.5.sp, letterSpacing = 0.7.sp, color = Muted)
    }
}

@Composable
fun DayBanner(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Ink.copy(alpha = 0.94f))
            .border(1.dp, Gold.copy(alpha = 0.55f), RoundedCornerShape(4.dp))
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(text, fontSize = 7.sp, color = GoldLight, maxLines = 1)
    }
}

@Composable
fun ImportPreviewDialog(
    summary: SaveSummary,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AppDialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF241930), Color(0xFF130E1C))))
                    .border(1.dp, Gold.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                    .padding(18.dp)
            ) {
                Text("IMPORTER CETTE SAUVEGARDE ?", fontFamily = Press, fontSize = 9.sp, color = GoldLight)
                Spacer(Modifier.height(8.dp))
                ImportRow("Niveau", summary.level.toString())
                ImportRow("Palier", summary.floor.toString())
                ImportRow("Or", formatInt(summary.gold))
                ImportRow("Jour", summary.day.ifBlank { "—" })
                ImportRow("Objets", summary.items.toString())
                ImportRow("Version", "v${summary.version}")
                Spacer(Modifier.height(8.dp))
                Text(
                    "La progression actuelle sera remplacée. La clé Garmin et les réglages restent inchangés.",
                    fontSize = 7.sp,
                    color = Blood,
                    lineHeight = 10.sp
                )
                Spacer(Modifier.height(12.dp))
                OverlayAction("REMPLACER LA PROGRESSION", Modifier.fillMaxWidth(), onConfirm, primary = true)
                Spacer(Modifier.height(5.dp))
                OverlayAction("ANNULER", Modifier.fillMaxWidth(), onCancel)
            }
        }
    }
}

@Composable
private fun ImportRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 7.5.sp, color = Muted)
        Text(value, fontSize = 8.sp, color = Parchment)
    }
}

@Composable
fun SpecialDialog(
    special: SpecialData,
    state: GameState,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val affordable = when (special.type) {
        "autel" -> state.shards >= special.shardCost
        "source" -> state.rations >= special.rationCost
        "marchand", "statue", "risque" -> state.gold >= special.goldCost
        else -> false
    }
    AppDialog(onDismissRequest = onDecline, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxWidth().padding(22.dp), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF2A1A22), Color(0xFF130E1C))))
                    .border(1.dp, Ember.copy(alpha = 0.75f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(special.title, fontFamily = Press, fontSize = 9.sp, color = GoldLight)
                Spacer(Modifier.height(6.dp))
                Text(
                    special.text,
                    fontSize = 8.sp,
                    color = Parchment.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    lineHeight = 11.sp
                )
                special.offer?.let { offer ->
                    Spacer(Modifier.height(9.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(5.dp))
                            .background(Panel)
                            .border(1.dp, rarityColor(offer.rarity).copy(alpha = 0.6f), RoundedCornerShape(5.dp))
                            .padding(7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssetImage(offer.icon, Modifier.size(30.dp))
                        Spacer(Modifier.width(7.dp))
                        Column(Modifier.weight(1f)) {
                            Text(offer.name, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Parchment)
                            Text(rarityLabel(offer.rarity) + " · " + statLine(offer), fontSize = 6.sp, color = Muted, maxLines = 2)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (special.goldCost > 0) {
                        SpecialCost("${formatInt(special.goldCost)} or", state.gold >= special.goldCost)
                    }
                    if (special.shardCost > 0) {
                        SpecialCost("${special.shardCost} éclat(s)", state.shards >= special.shardCost)
                    }
                    if (special.rationCost > 0) {
                        SpecialCost("${special.rationCost} ration", state.rations >= special.rationCost)
                    }
                }
                Spacer(Modifier.height(7.dp))
                Text(
                    "Récompense : ${special.rewardText}",
                    fontSize = 6.8.sp,
                    color = GoldLight,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                OverlayAction(
                    special.acceptLabel,
                    Modifier.fillMaxWidth(),
                    onAccept,
                    primary = affordable,
                    enabled = affordable
                )
                Spacer(Modifier.height(5.dp))
                OverlayAction(special.refuseLabel, Modifier.fillMaxWidth(), onDecline)
            }
        }
    }
}

@Composable
private fun SpecialCost(label: String, affordable: Boolean) {
    Box(
        Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(if (affordable) Panel2 else Blood.copy(alpha = 0.14f))
            .border(1.dp, if (affordable) Line else Blood.copy(alpha = 0.4f), RoundedCornerShape(3.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(label, fontSize = 6.5.sp, color = if (affordable) Gold else Blood)
    }
}

@Composable
fun SpecialResultDialog(outcome: SpecialOutcome, onDismiss: () -> Unit) {
    AppDialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxWidth().padding(26.dp), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF251A31), Color(0xFF130E1C))))
                    .border(
                        1.dp,
                        if (outcome.good) Green.copy(alpha = 0.7f) else Blood.copy(alpha = 0.7f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("RÉSULTAT", fontFamily = Press, fontSize = 10.sp, color = GoldLight)
                Spacer(Modifier.height(9.dp))
                Text(
                    outcome.text,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (outcome.good) Green else Blood,
                    textAlign = TextAlign.Center,
                    lineHeight = 15.sp
                )
                Spacer(Modifier.height(14.dp))
                OverlayAction("CONTINUER", Modifier.fillMaxWidth(), onDismiss, primary = true)
            }
        }
    }
}

@Composable
fun ConsumablesOverlay(
    state: GameState,
    onUse: (String) -> Unit,
    onClose: () -> Unit
) {
    val stats = statsOf(state)
    val owned = CONSUMABLES.filter { (state.consumables[it.id] ?: 0) > 0 }
    val now = System.currentTimeMillis()
    val activeBuffs = state.buffs.filter { it.expiresAt > now }
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0B0810), Color(0xFF171121), Color(0xFF0B0810))))
            .statusBarsPadding()
            .navigationBarsPadding()
            .clickable { }
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("SAC DE CONSOMMABLES", fontFamily = Press, fontSize = 9.sp, color = GoldLight)
                    Text(
                        "${state.consumables.values.sum()} objet(s) · ${activeBuffs.size} effet(s) actif(s)",
                        fontSize = 7.sp,
                        color = Muted
                    )
                }
                    CloseButton(onClose)
            }
            if (activeBuffs.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    activeBuffs.take(3).forEach { buff ->
                        val seconds = ((buff.expiresAt - now) / 1000L).coerceAtLeast(0L)
                        OverlayAction(
                            "${buff.label} ${seconds}s",
                            Modifier.weight(1f),
                            {},
                            primary = true,
                            enabled = false
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            DividerLine()
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp)
            ) {
                if (owned.isEmpty()) {
                    Text(
                        "Aucun consommable. Les monstres et les coffres en laissent parfois.",
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 8.sp,
                        color = Muted2
                    )
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
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Panel)
                            .border(1.dp, rarityColor(template.rarity).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Panel2)
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
                            Text(template.description, fontSize = 6.3.sp, color = Muted, lineHeight = 9.sp)
                            Row(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(rarityLabel(template.rarity), fontSize = 5.sp, color = rarityColor(template.rarity))
                                Text(
                                    consumableContextLabel(template.context),
                                    fontSize = 5.sp,
                                    color = if (template.context == "combat") Ember else Green
                                )
                            }
                        }
                        Spacer(Modifier.width(6.dp))
                        OverlayAction(
                            "UTILISER",
                            Modifier.width(86.dp),
                            { onUse(template.id) },
                            primary = true,
                            enabled = usable
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Les soins et la ferveur servent au camp comme en exploration. Les objets de combat exigent un combat en cours.",
                    fontSize = 6.3.sp,
                    color = Muted2,
                    lineHeight = 9.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun ChestArt(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = Color(0xFF4A2A13),
            topLeft = Offset(w * 0.08f, h * 0.26f),
            size = Size(w * 0.84f, h * 0.66f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
        )
        drawRect(Color(0xFF7A4A22), Offset(w * 0.12f, h * 0.30f), Size(w * 0.76f, h * 0.22f))
        drawRect(Color(0xFF5C3518), Offset(w * 0.12f, h * 0.52f), Size(w * 0.76f, h * 0.34f))
        drawRect(Color(0xFFE8C27A), Offset(w * 0.0f, h * 0.48f), Size(w, h * 0.10f))
        drawRect(Color(0xFFFFD479), Offset(w * 0.04f, h * 0.20f), Size(w * 0.12f, h * 0.30f))
        drawRect(Color(0xFFFFD479), Offset(w * 0.84f, h * 0.20f), Size(w * 0.12f, h * 0.30f))
        drawRect(Color(0xFF2A1A0E), Offset(w * 0.40f, h * 0.54f), Size(w * 0.20f, h * 0.20f))
        drawRect(Color(0xFFE8C27A), Offset(w * 0.44f, h * 0.57f), Size(w * 0.12f, h * 0.10f))
        drawRect(Color(0xFF2A1A0E), Offset(w * 0.48f, h * 0.60f), Size(w * 0.04f, h * 0.07f))
    }
}

@Composable
fun OverlayAction(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    primary: Boolean = false,
    danger: Boolean = false,
    enabled: Boolean = true
) {
    val border = when {
        danger -> Blood.copy(alpha = 0.7f)
        primary -> GoldLight
        else -> Line2
    }
    Box(
        modifier
            .heightIn(min = 38.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(
                when {
                    !enabled -> Panel2
                    primary -> Gold
                    danger -> Color(0xFF2A0F12)
                    else -> Panel2
                }
            )
            .border(1.dp, if (enabled) border else Line, RoundedCornerShape(4.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                !enabled -> Muted2
                primary -> Ink
                danger -> Color(0xFFFFB0A6)
                else -> Parchment
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}


