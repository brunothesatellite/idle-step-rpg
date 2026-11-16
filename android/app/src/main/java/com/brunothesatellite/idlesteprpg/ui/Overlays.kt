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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.brunothesatellite.idlesteprpg.BuildConfig
import com.brunothesatellite.idlesteprpg.GameViewModel
import com.brunothesatellite.idlesteprpg.UiState
import com.brunothesatellite.idlesteprpg.engine.ChestData
import com.brunothesatellite.idlesteprpg.engine.GameEngine
import com.brunothesatellite.idlesteprpg.engine.GameState
import com.brunothesatellite.idlesteprpg.engine.Item
import com.brunothesatellite.idlesteprpg.engine.JournalEntry
import com.brunothesatellite.idlesteprpg.engine.OfflineReport
import com.brunothesatellite.idlesteprpg.engine.SpecialData
import com.brunothesatellite.idlesteprpg.engine.chestRemainingMs
import com.brunothesatellite.idlesteprpg.engine.effectDescription
import com.brunothesatellite.idlesteprpg.engine.effectLabel
import com.brunothesatellite.idlesteprpg.engine.formatRemaining
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
    onFuse: () -> Unit
) {
    val equippedItem = equipped(state, item.slot)
    val fusionCount = state.inventory.count {
        it.slot == item.slot && it.rarity == item.rarity && !it.locked
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .fillMaxWidth()
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
                        Text(rarityLabel(item.rarity), fontSize = 5.5.sp, letterSpacing = 1.sp, color = rarityColor(item.rarity))
                        Text(item.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Parchment)
                        Text(
                            "${slotLabel(item.slot)} · Niv. ${item.level}" +
                                if (item.locked) " · VERROUILLÉ" else "",
                            fontSize = 7.sp,
                            color = if (item.locked) Ember else Muted
                        )
                        if (item.effect.isNotBlank()) {
                            Text(effectLabel(item.effect), fontSize = 6.sp, color = Legendary)
                        }
                    }
                    Text(
                        "×",
                        modifier = Modifier
                            .size(28.dp)
                            .clickable(onClick = onDismiss)
                            .background(Panel2, RoundedCornerShape(3.dp))
                            .padding(top = 3.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 17.sp,
                        color = Muted
                    )
                }
                Spacer(Modifier.height(8.dp))
                DividerLine()
                Spacer(Modifier.height(6.dp))
                StatRow("ATQ", if (item.atk != 0) "+${item.atk}" else "—")
                StatRow("PV", if (item.hp != 0) "+${item.hp}" else "—")
                StatRow("% VIT", if (item.vitPct != 0) "+${item.vitPct}" else "—")
                StatRow("% critique", if (item.critPct != 0) "+${item.critPct}" else "—")
                StatRow("Régénération", if (item.regen != 0.0) "+${"%.1f".format(item.regen)}" else "—")
                StatRow("Valeur", "${formatInt(item.value)} or")
                Spacer(Modifier.height(4.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Panel)
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("ÉQUIPÉ", fontSize = 5.5.sp, color = Muted2)
                        Text(
                            equippedItem?.let { statLine(it) } ?: "Emplacement vide",
                            fontSize = 6.5.sp,
                            color = Parchment.copy(alpha = 0.75f),
                            maxLines = 2
                        )
                    }
                    Text("→", color = Gold, fontSize = 12.sp)
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text("NOUVEAU", fontSize = 5.5.sp, color = Muted2)
                        Text(statLine(item), fontSize = 6.5.sp, color = GoldLight, maxLines = 2, textAlign = TextAlign.End)
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
                        enabled = fusionCount >= 3 && !item.locked
                    )
                }
                Spacer(Modifier.height(5.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OverlayAction(
                        if (item.locked) "VERROUILLÉ" else "VENDRE +${formatInt(item.value)} or",
                        Modifier.weight(0.85f),
                        onSell,
                        enabled = !item.locked
                    )
                    OverlayAction("ÉQUIPER", Modifier.weight(1.15f), onEquip, primary = true)
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 8.sp, color = Muted)
        Text(value, fontSize = 8.5.sp, color = Parchment)
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
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxWidth().padding(22.dp), contentAlignment = Alignment.Center) {
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
                    OverlayAction("ARRÊTER LE FORÇAGE", Modifier.fillMaxWidth(), onStop)
                } else {
                    OverlayAction(
                        if (chest.progress > 0) "REPRENDRE LE FORÇAGE" else "CONSACRER MES PAS",
                        Modifier.fillMaxWidth(),
                        onForce,
                        primary = true
                    )
                }
                Spacer(Modifier.height(4.dp))
                OverlayAction("PLUS TARD", Modifier.fillMaxWidth(), onDismiss)
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
    onResume: () -> Unit
) {
    val stats = statsOf(state)
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF08061A), Color(0xFF15102A), Color(0xFF2A1A2E))))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawCircle(Color(0xFFF4E6C8), radius = 17.dp.toPx(), center = Offset(w * 0.86f, h * 0.08f))
            drawCircle(Color(0x22F4E6C8), radius = 55.dp.toPx(), center = Offset(w * 0.86f, h * 0.08f))
            val random = java.util.Random(7)
            repeat(40) {
                drawCircle(
                    Color(0x99E8DCC4),
                    radius = if (random.nextBoolean()) 1.dp.toPx() else 1.5.dp.toPx(),
                    center = Offset(random.nextFloat() * w, random.nextFloat() * h * 0.70f)
                )
            }
            // sapins
            drawPath(
                Path().apply {
                    moveTo(w * 0.04f, h * 0.58f)
                    lineTo(w * 0.17f, h * 0.28f)
                    lineTo(w * 0.30f, h * 0.58f)
                    close()
                },
                Color(0xFF0C1018)
            )
            drawPath(
                Path().apply {
                    moveTo(w * 0.70f, h * 0.58f)
                    lineTo(w * 0.84f, h * 0.26f)
                    lineTo(w * 0.98f, h * 0.58f)
                    close()
                },
                Color(0xFF0C1018)
            )
            // tente
            drawPath(
                Path().apply {
                    moveTo(w * 0.26f, h * 0.61f)
                    lineTo(w * 0.42f, h * 0.43f)
                    lineTo(w * 0.58f, h * 0.61f)
                    close()
                },
                Color(0xFF3A2B27)
            )
            drawPath(
                Path().apply {
                    moveTo(w * 0.38f, h * 0.61f)
                    lineTo(w * 0.42f, h * 0.54f)
                    lineTo(w * 0.46f, h * 0.61f)
                    close()
                },
                Color(0xFF09060D)
            )
            // feu
            val fx = w * 0.57f
            val fy = h * 0.66f
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
            Text("LE FEU VEILLE", fontFamily = Press, fontSize = 10.sp, color = GoldLight)
            Text("L'expédition est en pause", fontSize = 7.5.sp, letterSpacing = 1.sp, color = Muted)
        }

        AssetImage(
            "hero/human.png",
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 470.dp, end = 50.dp)
                .size(82.dp)
        )

        Text(
            "×",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 40.dp, start = 14.dp)
                .size(30.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Ink.copy(alpha = 0.85f))
                .border(1.dp, Line2, RoundedCornerShape(4.dp))
                .clickable(onClick = onResume)
                .padding(top = 3.dp),
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
            color = Muted
        )

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
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
                "+25 % PV · une fois par jour",
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
            title = { Text("Révoquer la clé ?", fontSize = 15.sp) },
            text = { Text("La clé et le jeton seront supprimés de l'appareil. La progression du jeu est conservée.", fontSize = 9.sp) },
            confirmButton = {
                TextButton(onClick = {
                    confirmRevoke = false
                    vm.revokeGarmin()
                }) { Text("RÉVOQUER", color = Blood) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRevoke = false }) { Text("ANNULER", color = Muted) }
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
fun JournalOverlay(journal: List<JournalEntry>, onClose: () -> Unit) {
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
                    Text("${journal.size} événements conservés", fontSize = 7.sp, color = Muted)
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
                            .background(if (active) Gold.copy(alpha = 0.16f) else Panel)
                            .border(1.dp, if (active) Gold.copy(alpha = 0.6f) else Line, RoundedCornerShape(4.dp))
                            .clickable { filter = key }
                            .padding(vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, fontSize = 6.5.sp, color = if (active) GoldLight else Muted, maxLines = 1)
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
                if (filtered.isEmpty()) {
                    Text(
                        "Aucun événement dans ce filtre.",
                        modifier = Modifier.fillMaxWidth().padding(top = 30.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 8.sp,
                        color = Muted2
                    )
                }
                filtered.forEach { entry ->
                    JournalRow(entry)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
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
fun OfflineOverlay(report: OfflineReport, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF241930), Color(0xFF130E1C))))
                    .border(1.dp, Gold.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("LE SENTIER A CONTINUÉ", fontFamily = Press, fontSize = 9.sp, color = GoldLight)
                Text("Récapitulatif hors ligne", fontSize = 7.5.sp, color = Muted)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RecapTile(report.days.toString(), "JOURS", Modifier.weight(1f))
                    RecapTile(formatInt(report.steps), "PAS", Modifier.weight(1f))
                    RecapTile(formatInt(report.distanceMeters), "MÈTRES", Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Vos pas Garmin des derniers jours ont été récupérés et ajoutés à la marche, " +
                        "sans double comptage. Plafond de rattrapage : 20 000 pas par synchronisation.",
                    fontSize = 7.sp,
                    color = Muted,
                    textAlign = TextAlign.Center,
                    lineHeight = 10.sp
                )
                Spacer(Modifier.height(14.dp))
                GoldButton("REPRENDRE LA MARCHE", onClick = onDismiss)
            }
        }
    }
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
        Text(value, fontFamily = Press, fontSize = 8.sp, color = GoldLight, maxLines = 1)
        Text(label, fontSize = 5.5.sp, letterSpacing = 0.7.sp, color = Muted)
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
        "marchand", "statue" -> state.gold >= special.goldCost
        else -> false
    }
    Dialog(onDismissRequest = onDecline, properties = DialogProperties(usePlatformDefaultWidth = false)) {
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
            .height(38.dp)
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
            .clickable(enabled = enabled, onClick = onClick),
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


