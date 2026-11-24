package com.brunothesatellite.idlesteprpg.ui

import android.content.Context
import android.content.res.Resources
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brunothesatellite.idlesteprpg.BuildConfig
import com.brunothesatellite.idlesteprpg.R
import com.brunothesatellite.idlesteprpg.engine.BIOMES
import com.brunothesatellite.idlesteprpg.engine.Balance
import com.brunothesatellite.idlesteprpg.engine.GameState
import com.brunothesatellite.idlesteprpg.engine.ITEM_SETS
import com.brunothesatellite.idlesteprpg.engine.MAX_RATIONS
import com.brunothesatellite.idlesteprpg.engine.MAX_SHARDS
import com.brunothesatellite.idlesteprpg.engine.allBossTemplates
import com.brunothesatellite.idlesteprpg.engine.allMobTemplates
import com.brunothesatellite.idlesteprpg.engine.bossFor
import com.brunothesatellite.idlesteprpg.engine.discoveredSetSlots
import com.brunothesatellite.idlesteprpg.engine.statsOf
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt
import kotlin.random.Random

private data class EndingGuardian(
    val floor: Int,
    val name: String,
    val wins: Int,
    val attempts: Int
)

internal data class EndingSummary(
    val floors: Int,
    val biomes: Int,
    val kills: Int,
    val guardians: Int,
    val days: Int,
    val steps: Int,
    val streak: Int,
    val normalWins: Int,
    val normalFights: Int,
    val bossWins: Int,
    val bossFights: Int,
    val floor100Attempts: Int,
    val floor100Wins: Int,
    val averageCombatSeconds: Double,
    val combatCount: Int,
    val creaturesFound: Int,
    val creaturesTotal: Int,
    val guardiansFound: Int,
    val guardiansTotal: Int,
    val setsCompleted: Int,
    val setsTotal: Int
)

/** Agrège les compteurs locaux affichés par l'écran de fin et le rapport HTML. */
internal fun endingSummary(state: GameState, today: LocalDate = LocalDate.now()): EndingSummary {
    val telemetry = state.telemetry
    val days = runCatching {
        ChronoUnit.DAYS.between(LocalDate.parse(state.historyStart), today).toInt() + 1
    }.getOrDefault(1).coerceAtLeast(1)
    val mobs = allMobTemplates()
    val bosses = allBossTemplates()
    return EndingSummary(
        floors = state.floor.coerceAtMost(Balance.Target.ENDGAME_FINAL_FLOOR),
        biomes = BIOMES.count { state.floor >= it.fromFloor },
        kills = state.kills,
        guardians = state.bosses,
        days = days,
        steps = state.totalSteps,
        streak = state.streak,
        normalWins = telemetry.normalWins,
        normalFights = telemetry.normalFights,
        bossWins = telemetry.bossWins,
        bossFights = telemetry.bossFights,
        floor100Attempts = telemetry.bossAttempts[Balance.Target.ENDGAME_FINAL_FLOOR] ?: 0,
        floor100Wins = telemetry.bossWinsByFloor[Balance.Target.ENDGAME_FINAL_FLOOR] ?: 0,
        averageCombatSeconds = if (telemetry.combatCount > 0) {
            telemetry.combatMs / 1000.0 / telemetry.combatCount
        } else {
            0.0
        },
        combatCount = telemetry.combatCount,
        creaturesFound = mobs.count { (state.bestiary[it.id] ?: 0) > 0 },
        creaturesTotal = mobs.size,
        guardiansFound = bosses.count { (state.bossKills[it.id] ?: 0) > 0 },
        guardiansTotal = bosses.size,
        setsCompleted = ITEM_SETS.count { discoveredSetSlots(state, it).size >= 6 },
        setsTotal = ITEM_SETS.size
    )
}

/**
 * État de démonstration pour l'aperçu debug : mêmes objets et ressources que la
 * sauvegarde réelle, mais les compteurs d'un joueur arrivé au palier 100.
 */
internal fun endingPreviewState(state: GameState): GameState {
    val attempts = (81..100).associateWith { if (it == 100) 3 else 1 + it % 3 }
    val wins = attempts.mapValues { (floor, count) ->
        when {
            floor == 88 -> 0
            floor == 100 -> 1
            floor % 5 == 0 -> count
            else -> maxOf(1, count - 1)
        }
    }
    return state.copy(
        floor = 101,
        level = maxOf(state.level, 100),
        historyStart = state.historyStart.ifBlank { LocalDate.now().minusDays(41).toString() },
        totalSteps = maxOf(state.totalSteps, 1_284_000),
        kills = maxOf(state.kills, 3_482),
        bosses = maxOf(state.bosses, 124),
        streak = maxOf(state.streak, 11),
        telemetry = state.telemetry.copy(
            normalFights = maxOf(state.telemetry.normalFights, 3_412),
            normalWins = maxOf(state.telemetry.normalWins, 3_356),
            bossFights = maxOf(state.telemetry.bossFights, 151),
            bossWins = maxOf(state.telemetry.bossWins, 124),
            combatCount = maxOf(state.telemetry.combatCount, 3_563),
            combatMs = maxOf(state.telemetry.combatMs, 136_819_200L),
            bossAttempts = attempts,
            bossWinsByFloor = wins
        )
    )
}

private fun endingGuardians(state: GameState, res: Resources): List<EndingGuardian> =
    (81..100).map { floor ->
        EndingGuardian(
            floor = floor,
            name = creatureName(res, bossFor(floor).name),
            wins = state.telemetry.bossWinsByFloor[floor] ?: 0,
            attempts = state.telemetry.bossAttempts[floor] ?: 0
        )
    }

@Composable
fun EndingOverlay(
    state: GameState,
    onContinue: () -> Unit,
    reducedMotion: Boolean = false
) {
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/html")
    ) { uri ->
        if (uri != null) {
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(endingReportHtml(context, state).toByteArray(Charsets.UTF_8))
                } ?: error("flux indisponible")
            }.isSuccess
            Toast.makeText(
                context,
                context.getString(if (ok) R.string.ending_export_done else R.string.ending_export_error),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    BackHandler(onBack = onContinue)

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF040308), Color(0xFF0D0716), Color(0xFF1B0F08), Color(0xFF07060B))
                )
            )
            .clickable { }
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                EndingScene(reducedMotion)
                Spacer(Modifier.height(8.dp))
                EndingLegend()
                Spacer(Modifier.height(14.dp))
                Text(
                    stringResource(R.string.ending_eyebrow),
                    fontFamily = Pixel,
                    fontSize = 6.5.sp,
                    letterSpacing = 2.sp,
                    color = Gold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.ending_title),
                    fontFamily = Pixel,
                    fontSize = 23.sp,
                    lineHeight = 26.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldLight,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.ending_subtitle),
                    fontFamily = Pixel,
                    fontSize = 8.sp,
                    letterSpacing = 1.2.sp,
                    color = Gold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.ending_epilogue),
                    fontFamily = Pixel,
                    fontSize = 8.sp,
                    lineHeight = 12.sp,
                    color = Parchment,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(18.dp))
                EndHeroCard(state)
                Spacer(Modifier.height(10.dp))
                EndProgressionCard(state)
                Spacer(Modifier.height(10.dp))
                EndTelemetryCard(state)
                Spacer(Modifier.height(10.dp))
                EndGuardiansCard(state)
                Spacer(Modifier.height(10.dp))
                EndEconomyCard(state)
                Spacer(Modifier.height(10.dp))
                EndEquipmentCard(state)
                Spacer(Modifier.height(10.dp))
                EndCollectionsCard(state)
                Spacer(Modifier.height(10.dp))
                EndMilestonesCard()
                Spacer(Modifier.height(20.dp))
                OverlayAction(
                    stringResource(R.string.ending_continue),
                    Modifier.fillMaxWidth(),
                    onClick = onContinue,
                    primary = true
                )
                Spacer(Modifier.height(8.dp))
                OverlayAction(
                    stringResource(R.string.ending_export),
                    Modifier.fillMaxWidth(),
                    onClick = { exportLauncher.launch(context.getString(R.string.ending_export_filename)) }
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.ending_telemetry_note),
                    fontFamily = Pixel,
                    fontSize = 6.3.sp,
                    color = Muted2,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/* ---------- Scène ---------- */

@Composable
private fun EndingScene(reducedMotion: Boolean) {
    val stars = remember {
        val rnd = Random(4217)
        List(46) {
            Triple(rnd.nextFloat(), rnd.nextFloat() * 0.72f, 0.18f + rnd.nextFloat() * 0.5f)
        }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF05040B), Color(0xFF080610), Color(0xFF140C18), Color(0xFF1E1218))
                )
            )
            .clipToBounds()
    ) {
        Canvas(Modifier.fillMaxSize()) {
            stars.forEach { (x, y, a) ->
                drawCircle(
                    color = Color(0xFFD9CFB6).copy(alpha = a * 0.55f),
                    radius = if (a > 0.5f) 1.6f else 1f,
                    center = Offset(x * size.width, y * size.height)
                )
            }
            val stone = Color(0xFF2A2433).copy(alpha = 0.5f)
            val stoneHi = Color(0xFF4A4058).copy(alpha = 0.45f)
            listOf(
                8.dp.toPx() to 96.dp.toPx(),
                size.width - 34.dp.toPx() to 82.dp.toPx()
            ).forEach { (x, top) ->
                drawRect(stone, Offset(x, top), size = androidx.compose.ui.geometry.Size(24.dp.toPx(), size.height - top))
                drawRect(
                    stoneHi,
                    Offset(x + 3.dp.toPx(), top + 6.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(18.dp.toPx(), 3.dp.toPx())
                )
            }
        }

        // Lune pixel.
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 40.dp, end = 34.dp)
                .size(38.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFFF9EDCB), Color(0xFFE9D9B0), Color(0xFFC9B98E))
                    ),
                    CircleShape
                )
        )

        Text(
            stringResource(R.string.ending_badge),
            fontFamily = Pixel,
            fontSize = 6.sp,
            letterSpacing = 1.1.sp,
            color = Gold,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .border(1.dp, Line.copy(alpha = 0.7f), RoundedCornerShape(3.dp))
                .background(Color(0xFF07060B).copy(alpha = 0.55f))
                .padding(horizontal = 5.dp, vertical = 3.dp)
        )

        // Vraie statue du palier 100, en monument derrière le héros.
        AssetImage(
            "env/statue_hero.png",
            Modifier
                .align(Alignment.Center)
                .offset(y = (-30).dp)
                .height(174.dp)
                .width(116.dp)
                .alpha(0.52f)
                .zIndex(0.2f)
        )

        // Petit rappel de la statue du camp.
        AssetImage(
            "env/statue_hero.png",
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 4.dp, bottom = 18.dp)
                .height(84.dp)
                .width(56.dp)
                .alpha(0.62f)
                .zIndex(0.4f)
        )

        // Aura dorée + héros.
        Box(
            Modifier
                .align(Alignment.Center)
                .offset(y = 46.dp)
                .size(188.dp)
                .zIndex(1f),
            contentAlignment = Alignment.Center
        ) {
            GoldenAura(Modifier.fillMaxSize(), animate = !reducedMotion)
            AssetImage("hero/human.png", Modifier.size(88.dp))
        }

        // Gardien final déchu.
        AssetImage(
            bossFor(Balance.Target.ENDGAME_FINAL_FLOOR).icon,
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 2.dp, bottom = 8.dp)
                .height(82.dp)
                .width(124.dp)
                .alpha(0.86f)
                .zIndex(0.6f)
        )
    }
}

@Composable
private fun EndingLegend() {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(stringResource(R.string.ending_legend_statue), Gold, Modifier.weight(1f))
        LegendItem(stringResource(R.string.ending_legend_hero), GoldLight, Modifier.weight(1f), TextAlign.Center)
        LegendItem(stringResource(R.string.ending_legend_boss), Ember, Modifier.weight(1f), TextAlign.End)
    }
}

@Composable
private fun LegendItem(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    align: TextAlign = TextAlign.Start
) {
    Row(
        modifier,
        horizontalArrangement = if (align == TextAlign.End) Arrangement.End else if (align == TextAlign.Center) Arrangement.Center else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(3.dp).rotate(45f).background(color.copy(alpha = 0.7f)))
        Spacer(Modifier.width(4.dp))
        Text(
            text,
            fontFamily = Pixel,
            fontSize = 6.2.sp,
            letterSpacing = 0.8.sp,
            color = Muted2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = align
        )
    }
}

/* ---------- Cartes ---------- */

@Composable
private fun EndHeroCard(state: GameState) {
    val res = LocalContext.current.resources
    val stats = statsOf(state)
    val summary = remember(state) { endingSummary(state) }
    val speed = stats.vit * (1.0 + 0.004 * state.ferveur)
    PixelCard(Modifier.fillMaxWidth(), borderColor = Gold.copy(alpha = 0.35f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(listOf(GoldLight, Gold, GoldDeep))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    state.level.toString(),
                    fontFamily = Press,
                    fontSize = 9.sp,
                    color = Ink
                )
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.ending_legend_hero),
                    fontFamily = Pixel,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Parchment
                )
                Text(
                    stringResource(
                        R.string.ending_days_steps,
                        summary.days,
                        formatCompact(summary.steps)
                    ),
                    fontFamily = Pixel,
                    fontSize = 7.sp,
                    color = Muted
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    stringResource(R.string.ending_dossier),
                    fontFamily = Pixel,
                    fontSize = 6.sp,
                    letterSpacing = 1.sp,
                    color = Gold
                )
                Text(
                    stringResource(R.string.ending_save_meta, state.version),
                    fontFamily = Pixel,
                    fontSize = 5.8.sp,
                    color = Muted2
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            EndingStat(stringResource(R.string.ending_stat_hp), formatCompact(stats.maxHp), Modifier.weight(1f))
            EndingStat(stringResource(R.string.ending_stat_atk), formatCompact(stats.atk), Modifier.weight(1f))
            EndingStat(stringResource(R.string.ending_stat_vit), decimalRes(speed), Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            EndingStat(stringResource(R.string.ending_stat_crit), "${stats.crit} %", Modifier.weight(1f))
            EndingStat(
                stringResource(R.string.ending_stat_regen),
                decimalRes(stats.regen) + " PV/s",
                Modifier.weight(1f)
            )
            EndingStat(stringResource(R.string.ending_stat_ferveur), state.ferveur.toString(), Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            rankTitle(res, state.floor),
            fontFamily = Pixel,
            fontSize = 7.sp,
            color = GoldLight
        )
    }
}

@Composable
private fun EndingStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color.Black.copy(alpha = 0.18f))
            .border(1.dp, Line.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 5.dp)
    ) {
        Text(
            label.uppercase(),
            fontFamily = Pixel,
            fontSize = 6.sp,
            letterSpacing = 0.8.sp,
            color = Muted2,
            maxLines = 1
        )
        Text(
            value,
            fontFamily = Pixel,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = GoldLight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EndProgressionCard(state: GameState) {
    val summary = remember(state) { endingSummary(state) }
    PixelCard(Modifier.fillMaxWidth()) {
        SectionLabel(stringResource(R.string.ending_progression), color = Gold)
        Spacer(Modifier.height(6.dp))
        EndLine(stringResource(R.string.ending_floors_cleared), summary.floors.toString())
        EndLine(stringResource(R.string.ending_biomes), "${summary.biomes} / ${BIOMES.size}")
        EndLine(stringResource(R.string.ending_enemies), formatCompact(summary.kills))
        EndLine(stringResource(R.string.ending_guardians), formatCompact(summary.guardians))
        EndLine(stringResource(R.string.ending_streak), stringResource(R.string.ending_days_short, summary.streak))
        EndLine(stringResource(R.string.ending_steps), formatCompact(summary.steps))
    }
}

@Composable
private fun EndTelemetryCard(state: GameState) {
    val summary = remember(state) { endingSummary(state) }
    PixelCard(Modifier.fillMaxWidth()) {
        SectionLabel(stringResource(R.string.ending_telemetry), color = Gold)
        Spacer(Modifier.height(8.dp))
        EndMeter(
            stringResource(R.string.ending_normal_fights),
            stringResource(R.string.ending_wins_fights, summary.normalWins, summary.normalFights),
            fraction(summary.normalWins, summary.normalFights),
            Ember
        )
        Spacer(Modifier.height(8.dp))
        EndMeter(
            stringResource(R.string.ending_guardian_fights),
            stringResource(R.string.ending_wins_fights, summary.bossWins, summary.bossFights),
            fraction(summary.bossWins, summary.bossFights),
            Gold
        )
        Spacer(Modifier.height(8.dp))
        EndMeter(
            stringResource(R.string.ending_floor100),
            stringResource(
                R.string.ending_floor100_value,
                summary.floor100Attempts,
                summary.floor100Wins
            ),
            fraction(summary.floor100Wins, summary.floor100Attempts),
            Blue
        )
        Spacer(Modifier.height(8.dp))
        EndLine(
            stringResource(R.string.ending_avg_duration),
            stringResource(
                R.string.ending_avg_value,
                decimalRes(summary.averageCombatSeconds),
                formatCompact(summary.combatCount)
            )
        )
        Spacer(Modifier.height(5.dp))
        Text(
            stringResource(R.string.ending_telemetry_note),
            fontFamily = Pixel,
            fontSize = 6.2.sp,
            color = Muted2
        )
    }
}

@Composable
private fun EndGuardiansCard(state: GameState) {
    val res = LocalContext.current.resources
    val guardians = remember(state) { endingGuardians(state, res) }
    PixelCard(Modifier.fillMaxWidth()) {
        SectionLabel(stringResource(R.string.ending_guardians_title), color = Gold)
        Spacer(Modifier.height(6.dp))
        guardians.forEach { guardian ->
            val last = guardian.floor == Balance.Target.ENDGAME_FINAL_FLOOR
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 15.dp)
                    .padding(vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    guardian.floor.toString(),
                    fontFamily = Pixel,
                    fontSize = 6.5.sp,
                    color = if (last) Gold else Muted2,
                    modifier = Modifier.width(20.dp)
                )
                Text(
                    guardian.name,
                    fontFamily = Pixel,
                    fontSize = 7.sp,
                    color = if (last) GoldLight else if (guardian.attempts > 0) Parchment else Muted2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    Modifier
                        .width(40.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Line.copy(alpha = 0.65f))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(fraction(guardian.wins, guardian.attempts))
                            .fillMaxHeight()
                            .background(if (last) Gold else Ember.copy(alpha = 0.8f))
                    )
                }
                Text(
                    stringResource(R.string.ending_wins_attempts, guardian.wins, guardian.attempts),
                    fontFamily = Pixel,
                    fontSize = 6.2.sp,
                    color = Muted,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(42.dp)
                )
            }
        }
    }
}

@Composable
private fun EndEconomyCard(state: GameState) {
    PixelCard(Modifier.fillMaxWidth()) {
        SectionLabel(stringResource(R.string.ending_economy), color = Gold)
        Spacer(Modifier.height(7.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            EndingCoin(stringResource(R.string.ending_gold), formatCompact(state.gold), Modifier.weight(1f))
            EndingCoin(
                stringResource(R.string.ending_shards),
                "${state.shards} / $MAX_SHARDS",
                Modifier.weight(1f)
            )
            EndingCoin(
                stringResource(R.string.ending_rations),
                "${state.rations} / $MAX_RATIONS",
                Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun EndingCoin(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color.Black.copy(alpha = 0.18f))
            .border(1.dp, Line.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label.uppercase(),
            fontFamily = Pixel,
            fontSize = 5.8.sp,
            letterSpacing = 0.7.sp,
            color = Muted2,
            maxLines = 1
        )
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            fontFamily = Pixel,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = GoldLight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EndEquipmentCard(state: GameState) {
    val res = LocalContext.current.resources
    PixelCard(Modifier.fillMaxWidth()) {
        SectionLabel(stringResource(R.string.ending_equipment), color = Gold)
        Spacer(Modifier.height(5.dp))
        state.equipment.forEach { item ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(4.dp).rotate(45f).background(rarityColor(item.rarity)))
                Spacer(Modifier.width(6.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        slotLabelRes(item.slot).uppercase(),
                        fontFamily = Pixel,
                        fontSize = 5.6.sp,
                        letterSpacing = 0.6.sp,
                        color = Muted2
                    )
                    Text(
                        itemName(res, item.name, item.effect),
                        fontFamily = Pixel,
                        fontSize = 7.2.sp,
                        color = Parchment,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    statLineRes(res, item),
                    fontFamily = Pixel,
                    fontSize = 6.sp,
                    color = Muted,
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    modifier = Modifier.width(120.dp)
                )
            }
        }
    }
}

@Composable
private fun EndCollectionsCard(state: GameState) {
    val summary = remember(state) { endingSummary(state) }
    PixelCard(Modifier.fillMaxWidth()) {
        SectionLabel(stringResource(R.string.ending_collections), color = Gold)
        Spacer(Modifier.height(7.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            EndingCoin(
                stringResource(R.string.ending_creatures),
                "${summary.creaturesFound} / ${summary.creaturesTotal}",
                Modifier.weight(1f)
            )
            EndingCoin(
                stringResource(R.string.ending_guardians),
                "${summary.guardiansFound} / ${summary.guardiansTotal}",
                Modifier.weight(1f)
            )
            EndingCoin(
                stringResource(R.string.ending_sets),
                "${summary.setsCompleted} / ${summary.setsTotal}",
                Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun EndMilestonesCard() {
    PixelCard(Modifier.fillMaxWidth()) {
        SectionLabel(stringResource(R.string.ending_milestones), color = Gold)
        Spacer(Modifier.height(6.dp))
        listOf(
            R.string.ending_milestone_100,
            R.string.ending_milestone_final,
            R.string.ending_milestone_endless
        ).forEach { text ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(4.dp).rotate(45f).background(Gold.copy(alpha = 0.8f)))
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(text),
                    fontFamily = Pixel,
                    fontSize = 7.sp,
                    color = Parchment
                )
            }
        }
    }
}

@Composable
private fun EndLine(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontFamily = Pixel,
            fontSize = 7.5.sp,
            color = Muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            fontFamily = Pixel,
            fontSize = 7.8.sp,
            fontWeight = FontWeight.Bold,
            color = Parchment,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun EndMeter(label: String, value: String, fraction: Float, color: Color) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                fontFamily = Pixel,
                fontSize = 7.5.sp,
                color = Muted,
                maxLines = 1
            )
            Text(
                value,
                fontFamily = Pixel,
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                color = GoldLight,
                textAlign = TextAlign.End,
                maxLines = 1
            )
        }
        Spacer(Modifier.height(3.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(Line.copy(alpha = 0.65f))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(color)
            )
        }
    }
}

private fun fraction(value: Int, total: Int): Float =
    if (total <= 0) 0f else (value.toFloat() / total).coerceIn(0f, 1f)

/* ---------- Rapport HTML ---------- */

private fun pixelFontCss(context: Context): String = runCatching {
    val bytes = context.resources.openRawResource(R.raw.pixelify_sans).readBytes()
    val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
    "@font-face{font-family:'Pixelify Sans';src:url(data:font/woff2;base64,$encoded) format('woff2');font-weight:400 700;font-display:swap;}"
}.getOrDefault("")

private fun escapeHtml(value: String): String = value
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")

internal fun endingReportHtml(context: Context, state: GameState): String {
    val res = context.resources
    val locale = res.configuration.locales[0]
    val number = NumberFormat.getIntegerInstance(locale)
    val summary = endingSummary(state)
    val stats = statsOf(state)
    val guardians = endingGuardians(state, res)
    val generated = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG)
        .withLocale(locale)
        .format(LocalDateTime.now())

    fun n(value: Int): String = number.format(value)
    fun row(label: String, value: String): String =
        "<tr><td>${escapeHtml(label)}</td><td>${escapeHtml(value)}</td></tr>"

    val gearRows = state.equipment.joinToString("") { item ->
        "<tr><td>${escapeHtml(slotLabelRes(res, item.slot))}</td>" +
            "<td>${escapeHtml(itemName(res, item.name, item.effect))}</td>" +
            "<td>${escapeHtml(rarityLabelRes(res, item.rarity))}</td>" +
            "<td>${escapeHtml(statLineRes(res, item))}</td></tr>"
    }
    val guardianRows = guardians.joinToString("") { guardian ->
        val rate = if (guardian.attempts > 0) {
            (guardian.wins * 100.0 / guardian.attempts).roundToInt()
        } else {
            0
        }
        "<tr${if (guardian.floor == 100) " class=\"final\"" else ""}>" +
            "<td>${guardian.floor}</td><td>${escapeHtml(guardian.name)}</td>" +
            "<td>${guardian.wins} / ${guardian.attempts}</td><td>$rate %</td></tr>"
    }

    return """<!doctype html>
<html lang="${locale.language}">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>${escapeHtml(res.getString(R.string.ending_report_title))}</title>
<style>
${pixelFontCss(context)}
:root{color-scheme:dark}
*{box-sizing:border-box}
body{margin:0;padding:32px 18px;background:radial-gradient(900px 500px at 50% -10%,#2a1b0d,transparent 60%),#08070c;color:#ecdfc6;font-family:'Pixelify Sans',Georgia,serif}
main{max-width:900px;margin:0 auto}
.eyebrow{margin:0;font-size:11px;letter-spacing:.24em;color:#d8a94b;text-transform:uppercase}
h1{margin:10px 0 4px;font-size:clamp(30px,6vw,54px);line-height:1;color:#ffe6a3;letter-spacing:.03em;text-transform:uppercase}
.sub{margin:0 0 24px;font-size:12px;letter-spacing:.16em;color:#a89a86;text-transform:uppercase}
h2{margin:30px 0 10px;font-size:12px;letter-spacing:.2em;color:#d8a94b;text-transform:uppercase}
table{width:100%;border-collapse:collapse;font-size:14px}
th,td{padding:8px 9px;border-bottom:1px solid rgba(216,169,75,.16);text-align:left;vertical-align:top}
th{font-size:10px;letter-spacing:.14em;color:#7c7162;text-transform:uppercase}
td:first-child{color:#a89a86;white-space:nowrap}
tr.final td{color:#ffd09a}
.grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:8px}
.card{padding:12px;border:1px solid rgba(216,169,75,.18);background:rgba(255,255,255,.025)}
.card span{display:block;font-size:10px;letter-spacing:.12em;color:#7c7162;text-transform:uppercase}
.card b{display:block;margin-top:4px;font-size:18px;color:#ffe6a3}
.dot{display:inline-block;width:7px;height:7px;margin-right:5px;transform:rotate(45deg);background:#9aa0a6}
footer{margin-top:34px;padding-top:14px;border-top:1px solid rgba(216,169,75,.16);font-size:11px;line-height:1.6;color:#7c7162}
@media(max-width:620px){.grid{grid-template-columns:1fr 1fr}}
</style>
</head>
<body>
<main>
<p class="eyebrow">${escapeHtml(res.getString(R.string.ending_report_eyebrow))}</p>
<h1>${escapeHtml(res.getString(R.string.ending_title))}</h1>
<p class="sub">${escapeHtml(res.getString(R.string.ending_subtitle))} · ${escapeHtml(res.getString(R.string.ending_report_generated, generated))}</p>

<h2>${escapeHtml(res.getString(R.string.ending_report_hero))}</h2>
<div class="grid">
<div class="card"><span>${escapeHtml(res.getString(R.string.ending_report_level))}</span><b>${n(state.level)}</b></div>
<div class="card"><span>${escapeHtml(res.getString(R.string.ending_report_title_label))}</span><b>${escapeHtml(rankTitle(res, state.floor))}</b></div>
<div class="card"><span>${escapeHtml(res.getString(R.string.ending_stat_hp))}</span><b>${n(stats.maxHp)}</b></div>
<div class="card"><span>${escapeHtml(res.getString(R.string.ending_stat_atk))}</span><b>${n(stats.atk)}</b></div>
<div class="card"><span>${escapeHtml(res.getString(R.string.ending_stat_vit))}</span><b>${decimalRes(res, stats.vit * (1.0 + 0.004 * state.ferveur))}</b></div>
<div class="card"><span>${escapeHtml(res.getString(R.string.ending_stat_crit))}</span><b>${stats.crit} %</b></div>
</div>

<h2>${escapeHtml(res.getString(R.string.ending_progression))}</h2>
<table>
${row(res.getString(R.string.ending_floors_cleared), n(summary.floors))}
${row(res.getString(R.string.ending_biomes), "${summary.biomes} / ${BIOMES.size}")}
${row(res.getString(R.string.ending_enemies), n(summary.kills))}
${row(res.getString(R.string.ending_guardians), n(summary.guardians))}
${row(res.getString(R.string.ending_streak), res.getString(R.string.ending_days_short, summary.streak))}
${row(res.getString(R.string.ending_steps), n(summary.steps))}
</table>

<h2>${escapeHtml(res.getString(R.string.ending_telemetry))}</h2>
<table>
${row(res.getString(R.string.ending_normal_fights), res.getString(R.string.ending_wins_fights, summary.normalWins, summary.normalFights))}
${row(res.getString(R.string.ending_guardian_fights), res.getString(R.string.ending_wins_fights, summary.bossWins, summary.bossFights))}
${row(res.getString(R.string.ending_floor100), res.getString(R.string.ending_floor100_value, summary.floor100Attempts, summary.floor100Wins))}
${row(res.getString(R.string.ending_avg_duration), res.getString(R.string.ending_avg_value, decimalRes(res, summary.averageCombatSeconds), n(summary.combatCount)))}
</table>

<h2>${escapeHtml(res.getString(R.string.ending_equipment))}</h2>
<table>
<thead><tr><th>${escapeHtml(res.getString(R.string.ending_report_slot))}</th><th>${escapeHtml(res.getString(R.string.ending_report_item))}</th><th>${escapeHtml(res.getString(R.string.ending_report_rarity))}</th><th>${escapeHtml(res.getString(R.string.ending_report_stats))}</th></tr></thead>
<tbody>$gearRows</tbody>
</table>

<h2>${escapeHtml(res.getString(R.string.ending_guardians_title))}</h2>
<table>
<thead><tr><th>${escapeHtml(res.getString(R.string.ending_report_floor))}</th><th>${escapeHtml(res.getString(R.string.ending_report_guardian))}</th><th>${escapeHtml(res.getString(R.string.ending_report_wins))}</th><th>${escapeHtml(res.getString(R.string.ending_report_rate))}</th></tr></thead>
<tbody>$guardianRows</tbody>
</table>

<h2>${escapeHtml(res.getString(R.string.ending_collections))}</h2>
<div class="grid">
<div class="card"><span>${escapeHtml(res.getString(R.string.ending_creatures))}</span><b>${summary.creaturesFound} / ${summary.creaturesTotal}</b></div>
<div class="card"><span>${escapeHtml(res.getString(R.string.ending_guardians))}</span><b>${summary.guardiansFound} / ${summary.guardiansTotal}</b></div>
<div class="card"><span>${escapeHtml(res.getString(R.string.ending_sets))}</span><b>${summary.setsCompleted} / ${summary.setsTotal}</b></div>
</div>

<footer>${escapeHtml(res.getString(R.string.ending_report_footer, BuildConfig.VERSION_NAME, state.version))}</footer>
</main>
</body>
</html>"""
}
