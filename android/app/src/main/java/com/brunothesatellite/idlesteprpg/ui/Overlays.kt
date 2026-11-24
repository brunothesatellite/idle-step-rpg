package com.brunothesatellite.idlesteprpg.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.SystemClock
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.brunothesatellite.idlesteprpg.BuildConfig
import com.brunothesatellite.idlesteprpg.GameViewModel
import com.brunothesatellite.idlesteprpg.R
import com.brunothesatellite.idlesteprpg.ResumePhase
import com.brunothesatellite.idlesteprpg.ResumeUi
import com.brunothesatellite.idlesteprpg.UiState
import com.brunothesatellite.idlesteprpg.data.SaveSummary
import com.brunothesatellite.idlesteprpg.engine.CONSUMABLES
import com.brunothesatellite.idlesteprpg.engine.ChestData
import com.brunothesatellite.idlesteprpg.engine.GameEngine
import com.brunothesatellite.idlesteprpg.engine.GameState
import com.brunothesatellite.idlesteprpg.engine.ITEM_EFFECTS
import com.brunothesatellite.idlesteprpg.engine.MAX_SHARDS
import com.brunothesatellite.idlesteprpg.engine.Item
import com.brunothesatellite.idlesteprpg.engine.JournalEntry
import com.brunothesatellite.idlesteprpg.engine.OfflineReport
import com.brunothesatellite.idlesteprpg.engine.SpecialData
import com.brunothesatellite.idlesteprpg.engine.allMobTemplates
import com.brunothesatellite.idlesteprpg.engine.SpecialOutcome
import com.brunothesatellite.idlesteprpg.engine.chestRemainingMs
import com.brunothesatellite.idlesteprpg.engine.consumableContextLabel
import com.brunothesatellite.idlesteprpg.engine.effectDescription
import com.brunothesatellite.idlesteprpg.engine.effectLabel
import com.brunothesatellite.idlesteprpg.engine.formatRemaining
import com.brunothesatellite.idlesteprpg.engine.fireTier
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
import com.brunothesatellite.idlesteprpg.engine.isMaxLevel
import com.brunothesatellite.idlesteprpg.engine.upgradeLevel
import com.brunothesatellite.idlesteprpg.engine.upgradeName
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private enum class ForgeMode { UPGRADE, REFORGE }

private enum class ItemConfirm { SELL, SALVAGE, REFORGE }

private data class UpgradeSnapshot(
    val fromLevel: Int,
    val fromAtk: Int,
    val fromHp: Int,
    val toLevel: Int,
    val toAtk: Int,
    val toHp: Int
)

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
    val fusionCount = GameEngine.fusionGroup(state, item).size
    val upgradePrice = GameEngine.upgradeCost(item)
    val missingGold = (upgradePrice - state.gold).coerceAtLeast(0)
    val missingShards = (1 - state.shards).coerceAtLeast(0)
    val maxed = isMaxLevel(item)
    val canUpgrade = item.identified && !maxed && missingGold == 0 && missingShards == 0
    val upgradeHint = when {
        !item.identified -> stringResource(R.string.overlay_item_unidentified_hint)
        maxed -> stringResource(R.string.overlay_upgrade_max)
        canUpgrade -> stringResource(R.string.overlay_forge_hint, formatCompact(upgradePrice))
        else -> {
            val missing = buildList {
                if (missingGold > 0) add(stringResource(R.string.overlay_missing_gold, formatCompact(missingGold)))
                if (missingShards > 0) add(stringResource(R.string.overlay_missing_shards, missingShards))
            }.joinToString(stringResource(R.string.overlay_and))
            stringResource(R.string.overlay_missing_hint, missing)
        }
    }
    val equippedSetCount = state.equipment.count { it.setName == item.setName }
    val collectionSetCount = discoveredSetSlots(state, item.setName).size
    val noEffectLabel = stringResource(R.string.journal_no_effect)
    val currentEffectLabel = if (item.effect.isBlank()) noEffectLabel else effectLabelRes(item.effect)
    var forgeMode by remember(item.id) { mutableStateOf<ForgeMode?>(null) }
    var lastUpgrade by remember(item.id) { mutableStateOf<UpgradeSnapshot?>(null) }
    var lastReforgeOld by remember(item.id) { mutableStateOf<String?>(null) }
    var confirmAction by remember(item.id) { mutableStateOf<ItemConfirm?>(null) }
    val salvageBase = when (item.rarity) {
        "uncommon" -> 2
        "rare" -> 3
        "epic" -> 5
        "legendary" -> 8
        else -> 1
    }
    val salvageGain = (state.shards + salvageBase).coerceAtMost(MAX_SHARDS) - state.shards
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
                            if (item.identified) rarityLabelRes(item.rarity) else stringResource(R.string.overlay_unidentified_tag),
                            fontSize = 5.5.sp,
                            letterSpacing = 1.sp,
                            color = if (item.identified) rarityColor(item.rarity) else Ember
                        )
                        Text(
                            if (item.identified) localizedItem(item) else stringResource(R.string.overlay_mysterious_item),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Parchment
                        )
                        Text(
                            stringResource(
                                R.string.overlay_slot_level,
                                slotLabelRes(item.slot),
                                item.level
                            ) + if (item.locked) stringResource(R.string.overlay_locked_suffix) else "",
                            fontSize = 7.sp,
                            color = if (item.locked) Ember else Muted
                        )
                        if (item.identified && item.effect.isNotBlank()) {
                            Text(effectLabelRes(item.effect), fontSize = 6.sp, color = Legendary)
                        }
                        if (item.identified && item.setName.isNotBlank()) {
                            Text(
                                stringResource(
                                    R.string.overlay_set_line,
                                    setLabelRes(item.setName),
                                    equippedSetCount,
                                    collectionSetCount
                                ),
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
                if (forgeMode != null) {
                    ForgePanel(
                        mode = forgeMode!!,
                        item = item,
                        state = state,
                        equippedItem = equippedItem,
                        equippedHere = equippedHere,
                        canUpgrade = canUpgrade,
                        upgradePrice = upgradePrice,
                        upgradeHint = upgradeHint,
                        lastUpgrade = lastUpgrade,
                        lastReforgeOld = lastReforgeOld,
                        onBack = { forgeMode = null },
                        onConfirmUpgrade = {
                            val next = GameEngine.upgradedItem(item)
                            lastUpgrade = UpgradeSnapshot(
                                item.level, item.atk, item.hp,
                                next.level, next.atk, next.hp
                            )
                            onUpgrade()
                        },
                        onConfirmReforge = {
                            if (lastReforgeOld == null) {
                                confirmAction = ItemConfirm.REFORGE
                            } else {
                                lastReforgeOld = currentEffectLabel
                                onReroll()
                            }
                        },
                        onClose = onDismiss
                    )
                } else {
                if (!item.identified) {
                    Text(
                        stringResource(R.string.overlay_identify_hint),
                        fontSize = 7.sp,
                        color = Muted,
                        lineHeight = 10.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    OverlayAction(stringResource(R.string.overlay_identify_button), Modifier.fillMaxWidth(), onIdentify, primary = true)
                    Spacer(Modifier.height(5.dp))
                    OverlayAction(
                        if (item.locked) stringResource(R.string.overlay_locked) else stringResource(R.string.overlay_sell, formatCompact(item.value)),
                        Modifier.fillMaxWidth(),
                        { confirmAction = ItemConfirm.SELL },
                        enabled = !item.locked
                    )
                } else {
                    if (equippedHere) {
                        val nextLevel = GameEngine.upgradedItem(item)
                        ItemCompareTable(
                            leftTitle = stringResource(R.string.overlay_current),
                            rightTitle = stringResource(R.string.overlay_after_level),
                            left = item,
                            right = nextLevel
                        )
                        val before = statsOf(state)
                        val after = statsOf(
                            state.copy(
                                equipment = state.equipment.map {
                                    if (it.id == item.id) nextLevel else it
                                }
                            )
                        )
                        Text(
                            stringResource(
                                R.string.overlay_hero_stats,
                                formatCompact(before.atk),
                                formatCompact(after.atk),
                                formatCompact(before.maxHp),
                                formatCompact(after.maxHp)
                            ),
                            fontSize = 6.5.sp,
                            color = Blue,
                            modifier = Modifier.padding(top = 4.dp),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        ItemCompareTable(
                            leftTitle = stringResource(R.string.overlay_equipped_lower),
                            rightTitle = stringResource(R.string.overlay_this_item),
                            left = equippedItem,
                            right = item
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    StatRow(stringResource(R.string.overlay_value), stringResource(R.string.overlay_gold_value, formatCompact(item.value)))
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
                                stringResource(
                                    R.string.overlay_set_line,
                                    setLabelRes(item.setName),
                                    equippedSetCount,
                                    collectionSetCount
                                ),
                                fontSize = 6.sp,
                                letterSpacing = 0.5.sp,
                                color = Blue
                            )
                            Text(
                                if (equippedSetCount >= 2) localizedSetBonus(setBonusDescription(item.setName, tier))
                                else stringResource(
                                    R.string.overlay_set_two,
                                    localizedSetBonus(setBonusDescription(item.setName, 2))
                                ),
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
                            Text(effectLabelRes(item.effect), fontSize = 6.sp, letterSpacing = 0.6.sp, color = Legendary)
                            Text(effectDescRes(item.effect), fontSize = 6.5.sp, color = Parchment.copy(alpha = 0.8f))
                        }
                    }
                    Spacer(Modifier.height(9.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OverlayAction(
                            if (item.locked) stringResource(R.string.overlay_unlock) else stringResource(R.string.overlay_lock),
                            Modifier.weight(1f),
                            onToggleLock
                        )
                        OverlayAction(
                            if (fusionCount >= 3) stringResource(R.string.overlay_fuse) else stringResource(R.string.overlay_fuse_count, fusionCount),
                            Modifier.weight(1f),
                            onFuse,
                            enabled = GameEngine.canFuse(state, item)
                        )
                    }
                    if (state.bosses > 0) {
                        Spacer(Modifier.height(5.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            OverlayAction(
                                stringResource(R.string.overlay_reforge_cost),
                                Modifier.weight(1f),
                                { lastReforgeOld = null; forgeMode = ForgeMode.REFORGE },
                                enabled = item.identified && state.shards >= 1
                            )
                            OverlayAction(
                                stringResource(R.string.overlay_upgrade_cost, formatCompact(upgradePrice)),
                                Modifier.weight(1.2f),
                                { lastUpgrade = null; forgeMode = ForgeMode.UPGRADE },
                                enabled = canUpgrade
                            )
                            OverlayAction(
                                stringResource(R.string.overlay_salvage),
                                Modifier.weight(0.9f),
                                { confirmAction = ItemConfirm.SALVAGE },
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
                            stringResource(R.string.overlay_forge_locked),
                            fontSize = 6.sp,
                            color = Muted2,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OverlayAction(
                            if (item.locked) stringResource(R.string.overlay_locked)
                            else stringResource(R.string.overlay_sell, formatCompact(item.value)),
                            Modifier.weight(0.85f),
                            { confirmAction = ItemConfirm.SELL },
                            enabled = !item.locked && !equippedHere
                        )
                        OverlayAction(
                            if (equippedHere) stringResource(R.string.overlay_equipped) else stringResource(R.string.overlay_equip),
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

    val pending = confirmAction
    if (pending != null) {
        val itemName = localizedItem(item)
        AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = {
                WithAppFontScale {
                    Text(
                        stringResource(
                            when (pending) {
                                ItemConfirm.SELL -> R.string.overlay_confirm_sell_title
                                ItemConfirm.SALVAGE -> R.string.overlay_confirm_salvage_title
                                ItemConfirm.REFORGE -> R.string.overlay_confirm_reforge_title
                            }
                        ),
                        fontSize = 15.sp
                    )
                }
            },
            text = {
                WithAppFontScale {
                    Text(
                        when (pending) {
                            ItemConfirm.SELL -> stringResource(
                                R.string.overlay_confirm_sell_text,
                                itemName,
                                formatCompact(item.value)
                            )
                            ItemConfirm.SALVAGE -> stringResource(
                                R.string.overlay_confirm_salvage_text,
                                itemName,
                                salvageGain
                            )
                            ItemConfirm.REFORGE -> stringResource(
                                R.string.overlay_confirm_reforge_text,
                                itemName
                            )
                        },
                        fontSize = 9.sp
                    )
                }
            },
            confirmButton = {
                WithAppFontScale {
                    TextButton(onClick = {
                        confirmAction = null
                        when (pending) {
                            ItemConfirm.SELL -> onSell()
                            ItemConfirm.SALVAGE -> onDismantle()
                            ItemConfirm.REFORGE -> {
                                lastReforgeOld = currentEffectLabel
                                onReroll()
                            }
                        }
                    }) {
                        Text(
                            stringResource(
                                when (pending) {
                                    ItemConfirm.SELL -> R.string.overlay_confirm_sell
                                    ItemConfirm.SALVAGE -> R.string.overlay_salvage
                                    ItemConfirm.REFORGE -> R.string.overlay_reforge
                                }
                            ),
                            color = if (pending == ItemConfirm.REFORGE) Gold else Blood
                        )
                    }
                }
            },
            dismissButton = {
                WithAppFontScale {
                    TextButton(onClick = { confirmAction = null }) {
                        Text(stringResource(R.string.overlay_cancel), color = Muted)
                    }
                }
            },
            containerColor = Panel2
        )
    }
}

@Composable
private fun ForgePanel(
    mode: ForgeMode,
    item: Item,
    state: GameState,
    equippedItem: Item?,
    equippedHere: Boolean,
    canUpgrade: Boolean,
    upgradePrice: Int,
    upgradeHint: String,
    lastUpgrade: UpgradeSnapshot?,
    lastReforgeOld: String?,
    onBack: () -> Unit,
    onConfirmUpgrade: () -> Unit,
    onConfirmReforge: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onBack).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("←", fontSize = 15.sp, color = Gold)
        Spacer(Modifier.width(7.dp))
        Text(
            if (mode == ForgeMode.UPGRADE) stringResource(R.string.overlay_workshop_upgrade) else stringResource(R.string.overlay_workshop_reforge),
            fontFamily = Press,
            fontSize = 9.sp,
            color = GoldLight
        )
    }
    Spacer(Modifier.height(7.dp))
    if (mode == ForgeMode.UPGRADE) {
        UpgradePanel(
            item = item,
            state = state,
            equippedItem = equippedItem,
            equippedHere = equippedHere,
            canUpgrade = canUpgrade,
            upgradePrice = upgradePrice,
            upgradeHint = upgradeHint,
            lastUpgrade = lastUpgrade,
            onConfirm = onConfirmUpgrade,
            onClose = onClose
        )
    } else {
        ReforgePanel(
            item = item,
            state = state,
            lastReforgeOld = lastReforgeOld,
            onConfirm = onConfirmReforge,
            onBack = onBack,
            onClose = onClose
        )
    }
}

@Composable
private fun UpgradePanel(
    item: Item,
    state: GameState,
    equippedItem: Item?,
    equippedHere: Boolean,
    canUpgrade: Boolean,
    upgradePrice: Int,
    upgradeHint: String,
    lastUpgrade: UpgradeSnapshot?,
    onConfirm: () -> Unit,
    onClose: () -> Unit
) {
    val next = GameEngine.upgradedItem(item)
    val result = lastUpgrade?.takeIf { item.level >= it.toLevel }

    Text(
        stringResource(R.string.overlay_level_arrow, item.level, next.level),
        fontFamily = Press,
        fontSize = 11.sp,
        color = GoldLight
    )
    Spacer(Modifier.height(2.dp))
    Text(
        if (equippedHere) stringResource(R.string.overlay_upgrade_note)
        else stringResource(R.string.overlay_preview_note),
        fontSize = 6.2.sp,
        color = Muted,
        lineHeight = 9.sp
    )
    Spacer(Modifier.height(5.dp))

    if (result != null) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(Green.copy(alpha = 0.08f))
                .border(1.dp, Green.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                .padding(6.dp)
        ) {
            Text(
                stringResource(R.string.overlay_upgrade_applied, result.fromLevel, result.toLevel),
                fontSize = 6.6.sp,
                fontWeight = FontWeight.Bold,
                color = Green
            )
            Text(
                stringResource(R.string.overlay_upgrade_stats, result.fromAtk, result.toAtk, result.fromHp, result.toHp),
                fontSize = 6.2.sp,
                color = Parchment.copy(alpha = 0.85f)
            )
        }
        Spacer(Modifier.height(6.dp))
    }

    if (equippedHere) {
        UpgradeTable(reference = null, current = item, next = next)
        val before = statsOf(state)
        val afterState = state.copy(
            equipment = state.equipment.map { if (it.id == item.id) next else it }
        )
        val after = statsOf(afterState)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(
                R.string.overlay_hero_stats,
                formatCompact(before.atk),
                formatCompact(after.atk),
                formatCompact(before.maxHp),
                formatCompact(after.maxHp)
            ),
            fontSize = 6.5.sp,
            color = Blue,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    } else {
        UpgradeTable(reference = equippedItem, current = item, next = next)
        Text(
            if (equippedItem == null) stringResource(R.string.overlay_no_equipped)
            else stringResource(R.string.overlay_reference, localizedItem(equippedItem)),
            fontSize = 6.1.sp,
            color = Muted2
        )
    }

    Spacer(Modifier.height(7.dp))
    CostRow(stringResource(R.string.overlay_cost), stringResource(R.string.overlay_cost_one_shard, formatCompact(upgradePrice)))
    CostRow(stringResource(R.string.overlay_available), stringResource(R.string.overlay_cost_shards, formatCompact(state.gold), state.shards))
    if (!canUpgrade) {
        Text(upgradeHint, fontSize = 6.2.sp, color = Ember, lineHeight = 9.sp)
    }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OverlayAction(
            if (result != null) stringResource(R.string.overlay_keep_upgrading) else stringResource(R.string.overlay_upgrade),
            Modifier.weight(1.35f),
            onConfirm,
            primary = true,
            enabled = canUpgrade
        )
        OverlayAction(stringResource(R.string.overlay_close), Modifier.weight(0.65f), onClose)
    }
    Text(
        stringResource(R.string.overlay_upgrade_stays),
        fontSize = 5.9.sp,
        color = Muted2,
        modifier = Modifier.padding(top = 5.dp)
    )
}

@Composable
private fun ItemCompareTable(
    leftTitle: String,
    rightTitle: String,
    left: Item?,
    right: Item
) {
    Row(Modifier.fillMaxWidth().padding(bottom = 2.dp)) {
        Text(stringResource(R.string.overlay_stat), Modifier.weight(1.25f), fontSize = 5.7.sp, color = Muted2)
        Text(leftTitle, Modifier.weight(1f), fontSize = 5.7.sp, color = Muted2, textAlign = TextAlign.End)
        Text(rightTitle, Modifier.weight(1.15f), fontSize = 5.7.sp, color = Muted2, textAlign = TextAlign.End)
    }
    CompareLine(stringResource(R.string.overlay_atk), left?.atk, right.atk, percent = false)
    CompareLine(stringResource(R.string.overlay_hp), left?.hp, right.hp, percent = false)
    CompareLine(stringResource(R.string.overlay_pct_vit), left?.vitPct, right.vitPct, percent = true)
    CompareLine(stringResource(R.string.overlay_crit), left?.critPct, right.critPct, percent = true)
    CompareRegenLine(left?.regen, right.regen)
}

@Composable
private fun CompareLine(
    label: String,
    left: Int?,
    right: Int,
    percent: Boolean
) {
    val delta = if (left != null) right - left else 0
    val rightColor = when {
        left == null -> GoldLight
        right > left -> Green
        right < left -> Blood
        else -> Parchment
    }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.weight(1.25f), fontSize = 6.3.sp, color = Muted)
        Text(
            left?.let { statText(it, percent) } ?: "—",
            Modifier.weight(1f),
            fontSize = 6.3.sp,
            color = Muted,
            textAlign = TextAlign.End
        )
        Row(Modifier.weight(1.15f), horizontalArrangement = Arrangement.End) {
            Text(
                statText(right, percent),
                fontSize = 6.4.sp,
                fontWeight = FontWeight.Bold,
                color = rightColor
            )
            if (delta > 0) {
                Spacer(Modifier.width(3.dp))
                Text("+$delta", fontSize = 5.3.sp, color = Green)
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Line.copy(alpha = 0.45f)))
}

@Composable
private fun CompareRegenLine(left: Double?, right: Double) {
    val delta = if (left != null) right - left else 0.0
    val rightColor = when {
        left == null -> GoldLight
        right > left -> Green
        right < left -> Blood
        else -> Parchment
    }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.overlay_regeneration), Modifier.weight(1.25f), fontSize = 6.3.sp, color = Muted)
        Text(
            left?.let { regenText(it) } ?: "—",
            Modifier.weight(1f),
            fontSize = 6.3.sp,
            color = Muted,
            textAlign = TextAlign.End
        )
        Row(Modifier.weight(1.15f), horizontalArrangement = Arrangement.End) {
            Text(
                regenText(right),
                fontSize = 6.4.sp,
                fontWeight = FontWeight.Bold,
                color = rightColor
            )
            if (delta > 0.001) {
                Spacer(Modifier.width(3.dp))
                Text("+${"%.1f".format(delta)}", fontSize = 5.3.sp, color = Green)
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Line.copy(alpha = 0.45f)))
}

@Composable
private fun UpgradeTable(reference: Item?, current: Item, next: Item) {
    Row(Modifier.fillMaxWidth().padding(bottom = 2.dp)) {
        Text(stringResource(R.string.overlay_stat), Modifier.weight(1.25f), fontSize = 5.7.sp, color = Muted2)
        if (reference != null) {
            Text(stringResource(R.string.overlay_equipped_lower), Modifier.weight(1f), fontSize = 5.7.sp, color = Muted2, textAlign = TextAlign.End)
        }
        Text(
            if (reference == null) stringResource(R.string.overlay_current) else stringResource(R.string.overlay_item),
            Modifier.weight(0.9f),
            fontSize = 5.7.sp,
            color = Muted2,
            textAlign = TextAlign.End
        )
        Text(stringResource(R.string.overlay_after), Modifier.weight(1.15f), fontSize = 5.7.sp, color = Muted2, textAlign = TextAlign.End)
    }
    UpgradeLine(stringResource(R.string.overlay_atk), reference?.atk, current.atk, next.atk, percent = false)
    UpgradeLine(stringResource(R.string.overlay_hp), reference?.hp, current.hp, next.hp, percent = false)
    UpgradeLine(stringResource(R.string.overlay_pct_vit), reference?.vitPct, current.vitPct, next.vitPct, percent = true)
    UpgradeLine(stringResource(R.string.overlay_crit), reference?.critPct, current.critPct, next.critPct, percent = true)
    UpgradeRegenLine(reference?.regen, current.regen, next.regen)
}

@Composable
private fun UpgradeLine(
    label: String,
    reference: Int?,
    before: Int,
    after: Int,
    percent: Boolean
) {
    val delta = after - before
    val afterColor = when {
        reference == null -> GoldLight
        after > reference -> Green
        after < reference -> Blood
        else -> Parchment
    }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.weight(1.25f), fontSize = 6.3.sp, color = Muted)
        if (reference != null) {
            Text(
                statText(reference, percent),
                Modifier.weight(1f),
                fontSize = 6.3.sp,
                color = Muted,
                textAlign = TextAlign.End
            )
        }
        Text(
            statText(before, percent),
            Modifier.weight(0.9f),
            fontSize = 6.3.sp,
            color = Parchment,
            textAlign = TextAlign.End
        )
        Row(Modifier.weight(1.15f), horizontalArrangement = Arrangement.End) {
            Text(
                statText(after, percent),
                fontSize = 6.4.sp,
                fontWeight = FontWeight.Bold,
                color = afterColor
            )
            if (delta > 0) {
                Spacer(Modifier.width(3.dp))
                Text("+$delta", fontSize = 5.3.sp, color = Green)
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Line.copy(alpha = 0.45f)))
}

@Composable
private fun UpgradeRegenLine(reference: Double?, before: Double, after: Double) {
    val delta = after - before
    val afterColor = when {
        reference == null -> GoldLight
        after > reference -> Green
        after < reference -> Blood
        else -> Parchment
    }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.overlay_regen_short), Modifier.weight(1.25f), fontSize = 6.3.sp, color = Muted)
        if (reference != null) {
            Text(
                regenText(reference),
                Modifier.weight(1f),
                fontSize = 6.3.sp,
                color = Muted,
                textAlign = TextAlign.End
            )
        }
        Text(
            regenText(before),
            Modifier.weight(0.9f),
            fontSize = 6.3.sp,
            color = Parchment,
            textAlign = TextAlign.End
        )
        Row(Modifier.weight(1.15f), horizontalArrangement = Arrangement.End) {
            Text(
                regenText(after),
                fontSize = 6.4.sp,
                fontWeight = FontWeight.Bold,
                color = afterColor
            )
            if (delta > 0.001) {
                Spacer(Modifier.width(3.dp))
                Text("+${"%.1f".format(delta)}", fontSize = 5.3.sp, color = Green)
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Line.copy(alpha = 0.45f)))
}

private fun statText(value: Int, percent: Boolean): String = when {
    value == 0 -> "—"
    percent -> "+$value %"
    else -> "+$value"
}

private fun regenText(value: Double): String =
    if (value == 0.0) "—" else "+${"%.1f".format(value)}"

@Composable
private fun CostRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 6.5.sp, color = Muted)
        Text(
            value,
            fontSize = 6.7.sp,
            color = GoldLight,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ReforgePanel(
    item: Item,
    state: GameState,
    lastReforgeOld: String?,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    val current = if (item.effect.isBlank()) stringResource(R.string.journal_no_effect) else effectLabelRes(item.effect)
    val result = lastReforgeOld
    Text(
        stringResource(
            R.string.overlay_effect_arrow,
            current,
            if (result == null) stringResource(R.string.overlay_random) else current
        ),
        fontFamily = Press,
        fontSize = 10.sp,
        color = GoldLight
    )
    Spacer(Modifier.height(4.dp))

    if (result == null) {
        Text(
            stringResource(R.string.overlay_reforge_note1) + " " +
                stringResource(R.string.overlay_reforge_note2),
            fontSize = 6.3.sp,
            color = Muted,
            lineHeight = 9.sp
        )
        Spacer(Modifier.height(7.dp))
        ReforgePool(currentEffect = item.effect)
        Spacer(Modifier.height(7.dp))
        CostRow(stringResource(R.string.overlay_cost), stringResource(R.string.overlay_shards_count, 1))
        CostRow(stringResource(R.string.overlay_available), stringResource(R.string.overlay_shards_count, state.shards))
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OverlayAction(
                stringResource(R.string.overlay_reforge),
                Modifier.weight(1.35f),
                onConfirm,
                primary = true,
                enabled = state.shards >= 1
            )
            OverlayAction(stringResource(R.string.overlay_cancel), Modifier.weight(0.65f), onBack)
        }
        if (state.shards < 1) {
            Text(stringResource(R.string.msg_two_shards), fontSize = 6.2.sp, color = Ember, modifier = Modifier.padding(top = 5.dp))
        }
    } else {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(Green.copy(alpha = 0.08f))
                .border(1.dp, Green.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                .padding(7.dp)
        ) {
            Text(
                stringResource(
                    R.string.overlay_effect_result,
                    result,
                    current,
                    if (result == current) stringResource(R.string.overlay_identical) else ""
                ),
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                color = Green
            )
            Text(stringResource(R.string.overlay_name, localizedItem(item)), fontSize = 6.2.sp, color = Parchment.copy(alpha = 0.85f))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.overlay_reforge_note),
            fontSize = 6.1.sp,
            color = Muted,
            lineHeight = 9.sp
        )
        Spacer(Modifier.height(6.dp))
        CostRow(stringResource(R.string.overlay_shards_left), "${state.shards}")
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OverlayAction(
                stringResource(R.string.overlay_reforge_again),
                Modifier.weight(1.35f),
                onConfirm,
                primary = true,
                enabled = state.shards >= 1
            )
            OverlayAction(stringResource(R.string.overlay_close), Modifier.weight(0.65f), onClose)
        }
    }
}

@Composable
private fun ReforgePool(currentEffect: String) {
    val options: List<String?> = listOf(null) + ITEM_EFFECTS
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { effect ->
                    val active = if (effect == null) currentEffect.isBlank() else currentEffect == effect
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (active) Gold.copy(alpha = 0.12f) else Panel2)
                            .border(
                                1.dp,
                                if (active) Gold.copy(alpha = 0.55f) else Line,
                                RoundedCornerShape(3.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        Text(
                            effect?.let { effectLabelRes(it) } ?: stringResource(R.string.overlay_none),
                            fontSize = 5.6.sp,
                            color = if (active) GoldLight else Muted,
                            maxLines = 1
                        )
                        Text(
                            if (effect == null) "25 %" else "12,5 %",
                            fontSize = 5.3.sp,
                            color = Muted2
                        )
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun GuardianGateDialog(
    state: GameState,
    onEngage: () -> Unit,
    onFarm: () -> Unit,
    onCamp: () -> Unit
) {
    val boss = state.pendingBoss ?: return
    Box(
        Modifier.fillMaxSize().background(Ink.copy(alpha = 0.88f)),
        contentAlignment = Alignment.Center
    ) {
        PixelCard(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            borderColor = Gold.copy(alpha = 0.55f)
        ) {
            SectionLabel(stringResource(R.string.gate_title), color = Gold)
            Spacer(Modifier.height(5.dp))
            Text(
                boss.enemyName,
                fontFamily = Press,
                fontSize = 12.sp,
                color = GoldLight,
                lineHeight = 18.sp
            )
            Text(
                stringResource(R.string.gate_level, state.floor, boss.enemyLevel),
                fontSize = 6.5.sp,
                color = Muted
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GateStat(
                    stringResource(R.string.dashboard_hp),
                    formatCompact(boss.enemyMaxHp.roundToInt()),
                    Modifier.weight(1f)
                )
                GateStat(
                    stringResource(R.string.dashboard_atk),
                    formatCompact(boss.enemyAtk.roundToInt()),
                    Modifier.weight(1f)
                )
            }
            if (boss.abilityLabel.isNotBlank()) {
                Spacer(Modifier.height(5.dp))
                Text(
                    stringResource(R.string.gate_ability, boss.abilityLabel),
                    fontSize = 6.4.sp,
                    color = Ember
                )
            }
            if (boss.modifier.isNotBlank()) {
                Text(
                    stringResource(R.string.gate_modifier, boss.modifier),
                    fontSize = 6.4.sp,
                    color = Blood
                )
            }
            if (state.bossPity > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.gate_pity, (state.bossPity * 100).roundToInt()),
                    fontSize = 6.4.sp,
                    color = Green
                )
            }
            Spacer(Modifier.height(9.dp))
            GoldButton(stringResource(R.string.gate_engage), onClick = onEngage)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OverlayAction(
                    stringResource(R.string.gate_farm),
                    Modifier.weight(1f),
                    onFarm
                )
                OverlayAction(
                    stringResource(R.string.gate_camp),
                    Modifier.weight(1f),
                    onCamp
                )
            }
            Spacer(Modifier.height(5.dp))
            Text(
                stringResource(R.string.gate_hint),
                fontSize = 6.sp,
                color = Muted2,
                lineHeight = 9.sp
            )
        }
    }
}

@Composable
private fun GateStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Panel2)
            .border(1.dp, Line, RoundedCornerShape(4.dp))
            .padding(6.dp)
    ) {
        Text(label, fontSize = 5.5.sp, color = Muted2)
        Text(value, fontFamily = Press, fontSize = 8.sp, color = GoldLight)
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 8.sp, color = Muted, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(6.dp))
        Text(
            value,
            fontSize = 8.5.sp,
            color = Parchment,
            textAlign = TextAlign.End,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
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
                Text(chestTypeRes(chest.type), fontSize = 5.5.sp, letterSpacing = 1.sp, color = Gold)
                Text(
                    stringResource(R.string.overlay_chest_lock, formatCompact(chest.quota)),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Parchment,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.overlay_chest_note),
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
                    Text(
                        stringResource(
                            R.string.overlay_chest_progress,
                            formatCompact(chest.progress.toInt()),
                            formatCompact(chest.quota)
                        ),
                        fontSize = 6.5.sp,
                        color = GoldLight,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        when (chest.type) {
                            "bois" -> stringResource(R.string.overlay_chest_loot_wood)
                            "fer" -> stringResource(R.string.overlay_chest_loot_iron)
                            else -> stringResource(R.string.overlay_chest_loot_runic)
                        },
                        fontSize = 6.sp,
                        color = Muted
                    )
                }
                val remaining = chestRemainingMs(chest)
                Row(Modifier.fillMaxWidth().padding(top = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        stringResource(R.string.overlay_chest_expire, formatRemaining(remaining)),
                        fontSize = 6.5.sp,
                        color = if (remaining < 60 * 60 * 1000L) Blood else Ember
                    )
                    if (state.chestQueue.isNotEmpty()) {
                        Text(stringResource(R.string.overlay_chest_pending, state.chestQueue.size), fontSize = 6.5.sp, color = Gold)
                    }
                }
                Spacer(Modifier.height(9.dp))
                if (forcing) {
                    OverlayAction(
                        if (state.stepBank > 0) stringResource(R.string.overlay_chest_forcing) else stringResource(R.string.overlay_chest_waiting),
                        Modifier.fillMaxWidth(),
                        {},
                        primary = true,
                        enabled = false
                    )
                    Spacer(Modifier.height(4.dp))
                    OverlayAction(stringResource(R.string.overlay_chest_stop), Modifier.fillMaxWidth(), {
                        onStop()
                        onDismiss()
                    })
                } else {
                    OverlayAction(
                        if (chest.progress > 0) stringResource(R.string.overlay_chest_resume) else stringResource(R.string.overlay_chest_commit),
                        Modifier.fillMaxWidth(),
                        onForce,
                        primary = true
                    )
                }
                Spacer(Modifier.height(4.dp))
                OverlayAction(
                    if (forcing) stringResource(R.string.overlay_chest_hide) else stringResource(R.string.overlay_keep_walking),
                    Modifier.fillMaxWidth(),
                    onDismiss
                )
                }
                CloseButton(onDismiss, Modifier.align(Alignment.TopEnd).padding(6.dp))
            }
        }
    }
}

private val FIRE_PALETTES = listOf(
    Triple(Color(0xFFFF9448), Color(0xFFFFD06B), Color(0xFFFFF3C4)),
    Triple(Color(0xFF4FD08A), Color(0xFF9AF0B8), Color(0xFFE8FFF2)),
    Triple(Color(0xFFE4572E), Color(0xFFFF9A3C), Color(0xFFFFE0A3)),
    Triple(Color(0xFF3FA9E0), Color(0xFF8FD8FF), Color(0xFFEAF9FF)),
    Triple(Color(0xFF7FC8E8), Color(0xFFCBEFFF), Color(0xFFFFFFFF)),
    Triple(Color(0xFF7AC943), Color(0xFFC8F08A), Color(0xFFF6FFD9)),
    Triple(Color(0xFFB44BE0), Color(0xFFE89AFF), Color(0xFFFFE6FF)),
    Triple(Color(0xFFE8C27A), Color(0xFFFFE6A8), Color(0xFFFFFFFF))
)

private fun fireColors(floor: Int): Triple<Color, Color, Color> = FIRE_PALETTES[fireTier(floor)]

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
    onResume: () -> Unit,
    reducedMotion: Boolean = false
) {
    val stats = statsOf(state)
    var cardHeight by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val constrainedCard = density.fontScale > 1.5f && landscape
    val heroBottom = with(density) { cardHeight.toDp() } + 16.dp
    var lastHp by remember { mutableStateOf(state.hp) }
    var healTick by remember { mutableIntStateOf(0) }
    var healReport by remember { mutableStateOf<String?>(null) }
    var pendingAction by remember { mutableStateOf<String?>(null) }
    val campResources = androidx.compose.ui.platform.LocalContext.current.resources

    LaunchedEffect(state.hp) {
        val delta = state.hp - lastHp
        if (delta > 0.5 && pendingAction != null) {
            healReport = campResources.getString(
                R.string.overlay_pending_action,
                pendingAction,
                delta.roundToInt()
            )
            healTick++
            pendingAction = null
        }
        lastHp = state.hp
    }
    LaunchedEffect(healReport) {
        if (healReport != null) {
            delay(4000)
            healReport = null
        }
    }
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
            val (fireOuter, fireMid, fireCore) = fireColors(state.floor)
            drawCircle(
                Brush.radialGradient(
                    listOf(
                        fireOuter.copy(alpha = 0.47f),
                        fireOuter.copy(alpha = 0.13f),
                        Color.Transparent
                    ),
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
            drawPath(flame, Brush.verticalGradient(listOf(fireCore, fireMid, fireOuter)))
        }

        if (!reducedMotion) {
            HealBurst(healTick, cardHeight, Modifier.fillMaxSize())
        }

        Column(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.overlay_camp_title), fontFamily = Press, fontSize = 10.sp, color = GoldLight)
            Text(stringResource(R.string.overlay_camp_fire), fontSize = 7.5.sp, letterSpacing = 1.sp, color = Muted)
            Text(stringResource(R.string.overlay_camp_paused), fontSize = 6.2.sp, color = Muted2)
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
        if (state.floor >= 100) {
            AssetImage(
                "env/statue_hero.png",
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp, bottom = heroBottom)
                    .width(100.dp)
                    .height(150.dp)
            )
        }
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 96.dp, bottom = heroBottom)
                .size(88.dp),
            contentAlignment = Alignment.Center
        ) {
            if (state.floor >= 100) {
                GoldenAura(Modifier.size(170.dp), animate = !reducedMotion)
            }
            AssetImage("hero/human.png", Modifier.size(88.dp))
        }

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
                .then(if (constrainedCard) Modifier.fillMaxHeight(0.86f) else Modifier)
                .onSizeChanged { cardHeight = it.height }
                .padding(14.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Ink.copy(alpha = 0.94f))
                .border(1.dp, Line2, RoundedCornerShape(8.dp))
                .padding(10.dp)
                .verticalScroll(rememberScrollState())
        ) {
            healReport?.let { report ->
                Text(
                    report,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Green.copy(alpha = 0.12f))
                        .border(1.dp, Green.copy(alpha = 0.45f), RoundedCornerShape(4.dp))
                        .padding(vertical = 4.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 7.sp,
                    color = Green
                )
                Spacer(Modifier.height(5.dp))
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CampSummary(stringResource(R.string.overlay_floor), state.floor.toString().padStart(2, '0'), Modifier.weight(0.7f))
                CampSummary(stringResource(R.string.overlay_distance), stringResource(R.string.overlay_meters, state.distance.toInt()), Modifier.weight(0.9f))
                CampSummary(stringResource(R.string.overlay_fervour), state.ferveur.toString(), Modifier.weight(0.7f))
                CampSummary(
                    stringResource(R.string.overlay_hp),
                    stringResource(
                        R.string.overlay_hp_of,
                        formatCompact(state.hp.toInt()),
                        formatCompact(stats.maxHp)
                    ),
                    Modifier.weight(1.7f)
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CampSummary(stringResource(R.string.overlay_pending_steps), formatCompact(state.stepBank.toInt()), Modifier.weight(1.3f))
                CampSummary(stringResource(R.string.overlay_gold), formatCompact(state.gold), Modifier.weight(1.0f))
                CampSummary(stringResource(R.string.overlay_streak), stringResource(R.string.overlay_days_short, state.streak.coerceAtLeast(1)), Modifier.weight(0.7f))
                CampSummary(stringResource(R.string.overlay_total), formatCompact(state.totalSteps), Modifier.weight(1.0f))
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
            val restLabel = stringResource(R.string.overlay_camp_rest)
            val resupplyLabel = stringResource(R.string.overlay_camp_resupply)
            val vigilLabel = stringResource(R.string.overlay_camp_vigil)
            CampAction(
                restLabel,
                stringResource(R.string.overlay_camp_rest_desc),
                "item/meat_ration.png",
                state.rations.toString(),
                enabled = state.rations > 0 && state.hp < stats.maxHp,
                onClick = { pendingAction = restLabel; onRest() }
            )
            Spacer(Modifier.height(4.dp))
            CampAction(
                resupplyLabel,
                stringResource(R.string.overlay_camp_resupply_desc),
                "item/misc_crystal.png",
                state.shards.toString(),
                enabled = state.shards > 0 && state.hp < stats.maxHp,
                onClick = { pendingAction = resupplyLabel; onShard() }
            )
            Spacer(Modifier.height(4.dp))
            CampAction(
                vigilLabel,
                stringResource(R.string.overlay_camp_vigil_desc),
                "fx/heart.png",
                if (state.freeRestDay == state.day) stringResource(R.string.overlay_used) else stringResource(R.string.overlay_free),
                enabled = state.freeRestDay != state.day && state.hp < stats.maxHp,
                onClick = { pendingAction = vigilLabel; onFreeRest() }
            )
            if (state.stash.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                CampAction(
                    stringResource(R.string.overlay_camp_stash),
                    stringResource(R.string.overlay_camp_stash_desc),
                    "item/misc_crystal.png",
                    state.stash.size.toString(),
                    enabled = state.inventory.size < 24,
                    onClick = onWithdrawStash
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                OverlayAction(stringResource(R.string.overlay_journal), Modifier.weight(1f), onOpenJournal)
                OverlayAction(stringResource(R.string.overlay_inventory), Modifier.weight(1f), onOpenInventory)
                OverlayAction(stringResource(R.string.overlay_potions), Modifier.weight(1f), onOpenConsumables)
            }
            Spacer(Modifier.height(7.dp))
            GoldButton(stringResource(R.string.overlay_resume_expedition), onClick = onResume)
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.overlay_camp_quote),
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
private fun HealBurst(trigger: Int, cardHeight: Int, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(1100))
        }
    }
    Canvas(modifier) {
        val t = progress.value
        if (t <= 0f || t >= 1f) return@Canvas
        val panel = if (cardHeight > 0) cardHeight.toFloat() else size.height * 0.45f
        val ground = (size.height - panel - 16.dp.toPx())
            .coerceIn(size.height * 0.30f, size.height * 0.86f)
        val fx = size.width * 0.57f
        drawCircle(
            color = Color(0xFFFFD24A).copy(alpha = 0.20f * (1f - t)),
            radius = 90.dp.toPx() * (0.7f + t),
            center = Offset(fx, ground)
        )
        for (i in 0 until 12) {
            val spread = ((i * 53) % 70 - 35) * t
            val rise = t * (70 + (i % 5) * 26)
            drawCircle(
                color = Color(0xFFFFC83C).copy(alpha = (1f - t) * 0.9f),
                radius = 3.dp.toPx() * (1f - t * 0.6f),
                center = Offset(fx + spread, ground - rise)
            )
        }
    }
}

@Composable
private fun CampSummary(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            label,
            fontSize = 5.sp,
            letterSpacing = 0.7.sp,
            color = Muted2,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            value,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Parchment,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
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
                Text(stringResource(R.string.overlay_garmin_account), fontFamily = Press, fontSize = 9.sp, color = GoldLight)
                Text(stringResource(R.string.overlay_garmin_readonly), fontSize = 7.sp, color = Muted)
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
                        if (ui.connected) stringResource(R.string.overlay_garmin_connected) else stringResource(R.string.overlay_garmin_no_key),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (ui.connected) Green else Blood
                    )
                }
                Spacer(Modifier.height(5.dp))
                GarminInfoRow(stringResource(R.string.overlay_garmin_last_sync), formatTimeLocalized(ui.lastSync))
                GarminInfoRow(stringResource(R.string.overlay_garmin_key_visible), formatDate(expiry))
                GarminInfoRow(stringResource(R.string.overlay_garmin_oauth), formatTokenTime(ui.tokenExpiry))
                if (ui.keyExpiry != null) {
                    val days = ChronoUnit.DAYS.between(LocalDate.now(), ui.keyExpiry)
                    GarminInfoRow(
                        stringResource(R.string.overlay_garmin_expiry),
                        if (days < 0) stringResource(R.string.overlay_garmin_expired) else stringResource(R.string.overlay_in_days, days),
                        if (days < 0) Blood else if (days <= 30) Ember else Parchment
                    )
                }
                if (ui.lastError.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(localizedText(ui.lastError), fontSize = 7.sp, color = Blood, lineHeight = 10.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.overlay_garmin_2fa),
                fontSize = 7.5.sp,
                color = Muted,
                lineHeight = 11.sp
            )
            Spacer(Modifier.height(8.dp))
            OverlayAction(
                stringResource(R.string.overlay_garmin_open),
                Modifier.fillMaxWidth(),
                onClick = {
                    val intent = CustomTabsIntent.Builder().setShowTitle(true).build()
                    intent.launchUrl(context, Uri.parse("https://connect.garmin.com/app/settings/personalApiToken"))
                }
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.overlay_garmin_instructions),
                fontSize = 7.sp,
                color = Muted2,
                lineHeight = 10.sp
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = key,
                onValueChange = { key = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.overlay_garmin_key_hint), fontSize = 7.sp) },
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    Text(
                        if (showKey) stringResource(R.string.overlay_hide) else stringResource(R.string.overlay_show),
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
                    Text(stringResource(R.string.overlay_garmin_expiry_date), fontSize = 9.sp, color = Parchment)
                    Text(
                        stringResource(R.string.overlay_garmin_expiry_note),
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
                text = if (ui.syncing) "VALIDATION…" else stringResource(R.string.overlay_garmin_test_save),
                subtitle = stringResource(R.string.overlay_garmin_exchange),
                enabled = key.isNotBlank() && !ui.syncing,
                onClick = {
                    vm.saveKey(key, expiry)
                }
            )
            if (ui.connected) {
                Spacer(Modifier.height(5.dp))
                OverlayAction(stringResource(R.string.overlay_garmin_sync_now), Modifier.fillMaxWidth(), { vm.syncNow() })
            }
            if (BuildConfig.DEBUG) {
                Spacer(Modifier.height(8.dp))
                OverlayAction(stringResource(R.string.overlay_garmin_add_demo), Modifier.fillMaxWidth(), { vm.debugAddSteps(500) })
            }
            Spacer(Modifier.height(12.dp))
            if (ui.connected) {
                OverlayAction(
                    stringResource(R.string.overlay_garmin_revoke_erase),
                    Modifier.fillMaxWidth(),
                    { confirmRevoke = true },
                    danger = true
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(R.string.overlay_garmin_keystore),
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
            title = { WithAppFontScale { Text(stringResource(R.string.overlay_garmin_revoke_title), fontSize = 15.sp) } },
            text = {
                WithAppFontScale {
                    Text(
                        stringResource(R.string.overlay_garmin_revoke_note),
                        fontSize = 9.sp
                    )
                }
            },
            confirmButton = {
                WithAppFontScale {
                    TextButton(onClick = {
                        confirmRevoke = false
                        vm.revokeGarmin()
                    }) { Text(stringResource(R.string.overlay_garmin_revoke), color = Blood) }
                }
            },
            dismissButton = {
                WithAppFontScale {
                    TextButton(onClick = { confirmRevoke = false }) { Text(stringResource(R.string.overlay_cancel), color = Muted) }
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
                    Text(stringResource(R.string.journal_title), fontFamily = Press, fontSize = 9.sp, color = GoldLight)
                    Text(
                        when (tab) {
                            "legend" -> stringResource(R.string.journal_legend_count, legend.size)
                            "bestiary" -> stringResource(R.string.journal_bestiary_count, bestiary.size)
                            else -> stringResource(R.string.journal_event_count, journal.size)
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
                    "journal" to stringResource(R.string.journal_tab_journal),
                    "legend" to stringResource(R.string.journal_tab_legend),
                    "bestiary" to stringResource(R.string.journal_tab_bestiary)
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
                        "all" to stringResource(R.string.journal_filter_all),
                        "good" to stringResource(R.string.journal_filter_good),
                        "bad" to stringResource(R.string.journal_filter_bad),
                        "neutral" to stringResource(R.string.journal_filter_neutral)
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
                            EmptyJournal(stringResource(R.string.journal_empty_legend))
                        }
                        legend.forEach { entry -> JournalRow(entry) }
                    }
                    "bestiary" -> {
                        if (bestiary.isEmpty()) {
                            EmptyJournal(stringResource(R.string.journal_empty_bestiary))
                        }
                        bestiary.entries.sortedByDescending { it.value }.forEach { (id, kills) ->
                            val name = allMobTemplates().firstOrNull { it.id == id }?.name ?: id
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(creatureNameRes(name), fontSize = 8.sp, color = Parchment)
                                Text("× $kills", fontFamily = Press, fontSize = 7.sp, color = Gold)
                            }
                            DividerLine()
                        }
                    }
                    else -> {
                        if (filtered.isEmpty()) {
                            EmptyJournal(stringResource(R.string.journal_empty_filter))
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
                localizedText(entry.text),
                fontSize = 8.sp,
                color = Parchment.copy(alpha = 0.9f),
                lineHeight = 11.sp
            )
            Text(
                when (entry.type) {
                    "good" -> stringResource(R.string.journal_type_good)
                    "bad" -> stringResource(R.string.journal_type_bad)
                    else -> stringResource(R.string.journal_type_neutral)
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
    pendingSync: Boolean = false,
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
                    if (absence) stringResource(R.string.overlay_absence_title) else stringResource(R.string.overlay_absence_title_mixed),
                    fontFamily = Press,
                    fontSize = 9.sp,
                    color = GoldLight
                )
                Text(
                    when {
                        mixed -> stringResource(R.string.overlay_absence_desc)
                        absence -> stringResource(R.string.overlay_absence_during)
                        else -> stringResource(R.string.overlay_absence_summary)
                    },
                    fontSize = 7.5.sp,
                    color = Muted
                )
                Spacer(Modifier.height(12.dp))
                if (absence) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RecapTile(formatAbsence(report.minutes), stringResource(R.string.overlay_recap_absence), Modifier.weight(1f))
                        RecapTile(formatCompact(report.stepsConsumed), stringResource(R.string.overlay_recap_steps_used), Modifier.weight(1f))
                        RecapTile(formatCompact(report.distanceMeters), stringResource(R.string.overlay_recap_meters), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RecapTile(formatCompact(report.combats), stringResource(R.string.overlay_recap_fights), Modifier.weight(1f))
                        RecapTile(formatCompact(report.gold), stringResource(R.string.overlay_gold), Modifier.weight(1f))
                        RecapTile(formatCompact(report.floors), stringResource(R.string.overlay_recap_floors), Modifier.weight(1f))
                        RecapTile(formatCompact(report.bosses), stringResource(R.string.overlay_recap_guardians), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RecapTile(formatCompact(report.levels), stringResource(R.string.overlay_recap_levels), Modifier.weight(1f))
                        RecapTile(formatCompact(report.chests), stringResource(R.string.overlay_recap_chests), Modifier.weight(1f))
                        RecapTile(formatCompact(report.remainingSteps), stringResource(R.string.overlay_recap_reserve), Modifier.weight(1f))
                    }
                    if (mixed) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(
                                R.string.overlay_sync_summary,
                                report.days,
                                formatCompact(report.steps)
                            ),
                            fontSize = 6.8.sp,
                            color = Gold,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RecapTile(report.days.toString(), stringResource(R.string.overlay_recap_days), Modifier.weight(1f))
                        RecapTile(formatCompact(report.steps), stringResource(R.string.overlay_recap_steps), Modifier.weight(1f))
                        RecapTile(formatCompact(report.distanceMeters), stringResource(R.string.overlay_recap_meters), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RecapTile(formatCompact(report.combats), stringResource(R.string.overlay_recap_fights), Modifier.weight(1f))
                        RecapTile(formatCompact(report.gold), stringResource(R.string.overlay_gold), Modifier.weight(1f))
                        RecapTile(formatCompact(report.floors), stringResource(R.string.overlay_recap_floors), Modifier.weight(1f))
                        RecapTile(formatCompact(report.kills), stringResource(R.string.overlay_recap_kills), Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    when {
                        mixed -> stringResource(R.string.overlay_report_mixed)
                        report.stepsConsumed == 0 && report.remainingSteps > 0 ->
                            stringResource(R.string.overlay_report_quota)
                        report.stepsConsumed == 0 -> stringResource(R.string.overlay_report_empty)
                        absence -> stringResource(R.string.overlay_report_consumed)
                        else -> stringResource(R.string.overlay_report_recovered)
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
                        Text(stringResource(R.string.overlay_recap_loot), fontFamily = Press, fontSize = 6.sp, color = Gold)
                        val counts = buildList {
                            if (report.consumables > 0) add(stringResource(R.string.overlay_report_consumables, report.consumables))
                            if (report.rations > 0) add(stringResource(R.string.overlay_report_rations, report.rations))
                            if (report.shards > 0) add(stringResource(R.string.overlay_report_shards, report.shards))
                        }
                        if (counts.isNotEmpty()) {
                            Text(counts.joinToString(" · "), fontSize = 6.5.sp, color = Parchment)
                        }
                        report.lootItems.take(8).forEach { name ->
                            Text("• ${localizedItemName(name)}", fontSize = 6.2.sp, color = Green, maxLines = 1)
                        }
                        if (report.lootItems.size > 8) {
                            Text(stringResource(R.string.overlay_report_more, report.lootItems.size - 8), fontSize = 6.sp, color = Muted)
                        }
                    }
                }
                if (report.special) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.overlay_report_special),
                        fontSize = 7.sp,
                        color = GoldLight,
                        textAlign = TextAlign.Center
                    )
                }
                if (report.defeated) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        defeatSummary(report),
                        fontSize = 7.sp,
                        color = Blood,
                        textAlign = TextAlign.Center
                    )
                }
                if (pendingSync) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.overlay_report_pending),
                        fontSize = 6.5.sp,
                        color = Muted,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(12.dp))
                GoldButton(stringResource(R.string.overlay_resume_walk), onClick = onDismiss)
                if (onOpenJournal != null) {
                    TextButton(onClick = onOpenJournal) {
                        Text(stringResource(R.string.overlay_view_journal), fontSize = 7.sp, color = Muted)
                    }
                }
            }
        }
    }
}

private fun formatAbsence(minutes: Int): String =
    if (minutes >= 60) "${minutes / 60} h ${minutes % 60}" else "$minutes min"

/** Phrase de défaite : nombre explicite et recul total (20 m par défaite). */
@Composable
private fun defeatSummary(report: OfflineReport): String {
    val defeats = report.defeats
    if (defeats <= 0) return stringResource(R.string.overlay_report_defeated)
    val word = if (defeats == 1) stringResource(R.string.overlay_report_defeat) else stringResource(R.string.overlay_report_defeats)
    return stringResource(R.string.overlay_report_defeats_line, defeats, word, defeats * 20)
}

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
        Text(
            value,
            fontFamily = Press,
            fontSize = 8.sp,
            color = GoldLight,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            label,
            fontSize = 5.5.sp,
            letterSpacing = 0.7.sp,
            color = Muted,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Écran d'attente de la reprise : simulation d'absence puis récupération des
 * pas. Il n'apparaît qu'après 350 ms pour ne pas clignoter sur un retour rapide.
 */
@Composable
fun ResumeOverlay(
    resume: ResumeUi,
    reducedMotion: Boolean = false,
    onSkip: () -> Unit
) {
    var shown by remember(resume.startedAt) { mutableStateOf(false) }
    var elapsedMs by remember(resume.startedAt) { mutableLongStateOf(0L) }

    LaunchedEffect(resume.startedAt) {
        delay(350)
        shown = true
    }
    LaunchedEffect(resume.startedAt) {
        while (true) {
            elapsedMs = SystemClock.elapsedRealtime() - resume.startedAt
            delay(200)
        }
    }

    if (!shown) return

    Box(
        Modifier
            .fillMaxSize()
            .background(Ink.copy(alpha = 0.94f))
            .clickable { }
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.overlay_resuming),
                fontFamily = Press,
                fontSize = 10.sp,
                color = GoldLight,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                when (resume.phase) {
                    ResumePhase.SIMULATING -> stringResource(R.string.overlay_vigil_passing)
                    ResumePhase.SYNCING -> stringResource(R.string.overlay_reading, resume.sourceLabel)
                },
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                fontSize = 8.sp,
                color = Parchment,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (resume.phase == ResumePhase.SIMULATING) {
                    stringResource(R.string.overlay_sim_fights)
                } else {
                    stringResource(R.string.overlay_sim_steps)
                },
                fontSize = 6.5.sp,
                color = Muted,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            if (reducedMotion) {
                Box(
                    Modifier
                        .fillMaxWidth(0.7f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Line)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(0.35f)
                            .height(3.dp)
                            .background(Gold)
                    )
                }
            } else {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(0.7f),
                    color = Gold,
                    trackColor = Line
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                if (elapsedMs >= 1000L) "${elapsedMs / 1000} s" else " ",
                fontSize = 6.5.sp,
                color = Muted2
            )
            Spacer(Modifier.height(14.dp))
            if (elapsedMs >= 800L) {
                TextButton(onClick = onSkip) {
                    Text(
                        stringResource(R.string.overlay_continue_no_wait),
                        fontFamily = Pixel,
                        fontSize = 6.5.sp,
                        color = Muted
                    )
                }
                Text(
                    stringResource(R.string.overlay_sync_background),
                    fontSize = 6.sp,
                    color = Muted2,
                    textAlign = TextAlign.Center
                )
            }
        }
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
                Text(stringResource(R.string.overlay_import_title), fontFamily = Press, fontSize = 9.sp, color = GoldLight)
                Spacer(Modifier.height(8.dp))
                ImportRow(stringResource(R.string.overlay_import_level), summary.level.toString())
                ImportRow(stringResource(R.string.overlay_import_floor), summary.floor.toString())
                ImportRow(stringResource(R.string.overlay_import_gold), formatCompact(summary.gold))
                ImportRow(stringResource(R.string.overlay_import_day), summary.day.ifBlank { "—" })
                ImportRow(stringResource(R.string.overlay_import_items), summary.items.toString())
                ImportRow(stringResource(R.string.overlay_import_version), "v${summary.version}")
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.overlay_import_note),
                    fontSize = 7.sp,
                    color = Blood,
                    lineHeight = 10.sp
                )
                Spacer(Modifier.height(12.dp))
                OverlayAction(stringResource(R.string.overlay_import_replace), Modifier.fillMaxWidth(), onConfirm, primary = true)
                Spacer(Modifier.height(5.dp))
                OverlayAction(stringResource(R.string.overlay_cancel), Modifier.fillMaxWidth(), onCancel)
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
        Text(
            value,
            fontSize = 8.sp,
            color = Parchment,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
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
                Text(specialTitleRes(special.type), fontFamily = Press, fontSize = 9.sp, color = GoldLight)
                Spacer(Modifier.height(6.dp))
                Text(
                    specialTextRes(special.type),
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
                            Text(localizedItem(offer), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Parchment)
                            Text(rarityLabelRes(offer.rarity) + " · " + statLineRes(offer), fontSize = 6.sp, color = Muted, maxLines = 2)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (special.goldCost > 0) {
                        SpecialCost(stringResource(R.string.overlay_special_gold, formatCompact(special.goldCost)), state.gold >= special.goldCost)
                    }
                    if (special.shardCost > 0) {
                        SpecialCost(stringResource(R.string.overlay_special_shards, special.shardCost), state.shards >= special.shardCost)
                    }
                    if (special.rationCost > 0) {
                        SpecialCost(stringResource(R.string.overlay_special_ration, special.rationCost), state.rations >= special.rationCost)
                    }
                }
                Spacer(Modifier.height(7.dp))
                Text(
                    stringResource(R.string.overlay_special_reward) + if (special.type == "marchand") {
                        special.offer?.let { localizedItem(it) } ?: ""
                    } else {
                        specialRewardRes(special.type)
                    },
                    fontSize = 6.8.sp,
                    color = GoldLight,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                OverlayAction(
                    specialAcceptRes(special.type),
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
        Text(
            label,
            fontSize = 6.5.sp,
            color = if (affordable) Gold else Blood,
            maxLines = 1,
            softWrap = false
        )
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
                Text(stringResource(R.string.overlay_special_result), fontFamily = Press, fontSize = 10.sp, color = GoldLight)
                Spacer(Modifier.height(9.dp))
                Text(
                    localizedText(outcome.text),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (outcome.good) Green else Blood,
                    textAlign = TextAlign.Center,
                    lineHeight = 15.sp
                )
                Spacer(Modifier.height(14.dp))
                OverlayAction(stringResource(R.string.overlay_continue), Modifier.fillMaxWidth(), onDismiss, primary = true)
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
                    Text(stringResource(R.string.overlay_bag_title), fontFamily = Press, fontSize = 9.sp, color = GoldLight)
                    Text(
                        stringResource(
                            R.string.overlay_bag_count,
                            state.consumables.values.sum(),
                            activeBuffs.size
                        ),
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
                            "${buffLabelRes(buff.label)} ${seconds}s",
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
                        stringResource(R.string.dashboard_consumables_empty),
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
                                    consumableNameRes(template.id),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Parchment,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("×$quantity", fontFamily = Press, fontSize = 6.sp, color = Gold)
                            }
                            Text(consumableDescRes(template.id), fontSize = 6.3.sp, color = Muted, lineHeight = 9.sp)
                            Row(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(rarityLabelRes(template.rarity), fontSize = 5.sp, color = rarityColor(template.rarity))
                                Text(
                                    consumableContextLabel(template.context),
                                    fontSize = 5.sp,
                                    color = if (template.context == "combat") Ember else Green
                                )
                            }
                        }
                        Spacer(Modifier.width(6.dp))
                        OverlayAction(
                            stringResource(R.string.overlay_bag_use),
                            Modifier.width(86.dp),
                            { onUse(template.id) },
                            primary = true,
                            enabled = usable
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.overlay_bag_note),
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


