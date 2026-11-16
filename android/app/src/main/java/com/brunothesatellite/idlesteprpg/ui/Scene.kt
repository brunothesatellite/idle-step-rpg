package com.brunothesatellite.idlesteprpg.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.brunothesatellite.idlesteprpg.audio.SoundFx
import com.brunothesatellite.idlesteprpg.engine.FLOOR_METERS
import com.brunothesatellite.idlesteprpg.engine.GameEngine
import com.brunothesatellite.idlesteprpg.engine.GameState
import com.brunothesatellite.idlesteprpg.engine.Item
import com.brunothesatellite.idlesteprpg.engine.STEPS_PER_METER
import com.brunothesatellite.idlesteprpg.engine.biomeFor
import com.brunothesatellite.idlesteprpg.engine.biomeKeyFor
import com.brunothesatellite.idlesteprpg.engine.chestRemainingMs
import com.brunothesatellite.idlesteprpg.engine.equipped
import com.brunothesatellite.idlesteprpg.engine.formatRemaining
import com.brunothesatellite.idlesteprpg.engine.milestoneBonus
import com.brunothesatellite.idlesteprpg.engine.statsOf
import com.brunothesatellite.idlesteprpg.engine.tierIndex
import com.brunothesatellite.idlesteprpg.engine.tierName
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.roundToInt

private data class BiomePalette(
    val top: Color,
    val mid: Color,
    val bottom: Color,
    val wallTint: Color,
    val torch: Color,
    val torchLight: Color,
    val torchDeep: Color,
    val floorTop: Color,
    val floorBottom: Color,
    val groundEdge: Color,
    val groundEdgeDeep: Color,
    val groundLine: Color,
    val ambient: Color,
    val ambientCount: Int
)

internal data class BiomeVisuals(
    val walls: List<String>,
    val floors: List<String>,
    val statues: List<String>,
    val props: List<String>,
    val weather: String,
    val gate: String,
    val fireClouds: Boolean = false
)

private fun biomePalette(key: String): BiomePalette = when (key) {
    "crypts" -> BiomePalette(
        top = Color(0xFF07050C), mid = Color(0xFF151022), bottom = Color(0xFF09060F),
        wallTint = Color(0xFF3A2C4C),
        torch = Color(0xFFFF9448), torchLight = Color(0xFFFFD479), torchDeep = Color(0xFF8F2D19),
        floorTop = Color(0xFF17101D), floorBottom = Color(0xFF08050C),
        groundEdge = Color(0xFF4A3C56), groundEdgeDeep = Color(0xFF2A2035), groundLine = Color(0xFF6A5678),
        ambient = Color(0xFFB09AC8), ambientCount = 14
    )
    "catacombs" -> BiomePalette(
        top = Color(0xFF03090A), mid = Color(0xFF0B1A18), bottom = Color(0xFF050D0C),
        wallTint = Color(0xFF23524A),
        torch = Color(0xFF5FD8B0), torchLight = Color(0xFFB8F5D9), torchDeep = Color(0xFF1B6B52),
        floorTop = Color(0xFF10201D), floorBottom = Color(0xFF050C0A),
        groundEdge = Color(0xFF2F5A50), groundEdgeDeep = Color(0xFF17332D), groundLine = Color(0xFF4E8A7A),
        ambient = Color(0xFF8EE8C6), ambientCount = 22
    )
    "ash" -> BiomePalette(
        top = Color(0xFF0C0705), mid = Color(0xFF221410), bottom = Color(0xFF0A0605),
        wallTint = Color(0xFF6B3A22),
        torch = Color(0xFFFF7A3D), torchLight = Color(0xFFFFC27A), torchDeep = Color(0xFF6B2410),
        floorTop = Color(0xFF241713), floorBottom = Color(0xFF0B0706),
        groundEdge = Color(0xFF6A3F2C), groundEdgeDeep = Color(0xFF3A2118), groundLine = Color(0xFF9A5B3C),
        ambient = Color(0xFFFFB07A), ambientCount = 34
    )
    "forge" -> BiomePalette(
        top = Color(0xFF05080F), mid = Color(0xFF111C2E), bottom = Color(0xFF04060A),
        wallTint = Color(0xFF2E4A6B),
        torch = Color(0xFF6FA8FF), torchLight = Color(0xFFB8D8FF), torchDeep = Color(0xFF25406B),
        floorTop = Color(0xFF131C2A), floorBottom = Color(0xFF05070C),
        groundEdge = Color(0xFF3A5470), groundEdgeDeep = Color(0xFF1C2C40), groundLine = Color(0xFF5C82A8),
        ambient = Color(0xFF8FC4FF), ambientCount = 26
    )
    "peaks" -> BiomePalette(
        top = Color(0xFF060B14), mid = Color(0xFF12253A), bottom = Color(0xFF050910),
        wallTint = Color(0xFF5C7A99),
        torch = Color(0xFFB9E6FF), torchLight = Color(0xFFF0FBFF), torchDeep = Color(0xFF48708A),
        floorTop = Color(0xFF162534), floorBottom = Color(0xFF060A10),
        groundEdge = Color(0xFF6B8299), groundEdgeDeep = Color(0xFF354556), groundLine = Color(0xFFB9D9F0),
        ambient = Color(0xFFE4F3FF), ambientCount = 60
    )
    "woods" -> BiomePalette(
        top = Color(0xFF08100A), mid = Color(0xFF1A2A16), bottom = Color(0xFF070C06),
        wallTint = Color(0xFF4A6B2E),
        torch = Color(0xFFFFC65C), torchLight = Color(0xFFFFE6A0), torchDeep = Color(0xFF8A5A1A),
        floorTop = Color(0xFF1B2712), floorBottom = Color(0xFF080D06),
        groundEdge = Color(0xFF4E6A32), groundEdgeDeep = Color(0xFF283A18), groundLine = Color(0xFF8AA85A),
        ambient = Color(0xFFC8E890), ambientCount = 34
    )
    "abyss" -> BiomePalette(
        top = Color(0xFF060A08), mid = Color(0xFF142418), bottom = Color(0xFF050806),
        wallTint = Color(0xFF3E6B4A),
        torch = Color(0xFF8CFFB0), torchLight = Color(0xFFD8FFE0), torchDeep = Color(0xFF2A6B44),
        floorTop = Color(0xFF122018), floorBottom = Color(0xFF050A07),
        groundEdge = Color(0xFF3E6B4A), groundEdgeDeep = Color(0xFF1E3A28), groundLine = Color(0xFF7ABF8A),
        ambient = Color(0xFF9CFFBE), ambientCount = 46
    )
    else -> BiomePalette(
        top = Color(0xFF120408), mid = Color(0xFF2A0A12), bottom = Color(0xFF0A0205),
        wallTint = Color(0xFF6B2230),
        torch = Color(0xFFFF5C5C), torchLight = Color(0xFFFFC0B0), torchDeep = Color(0xFF8A1A22),
        floorTop = Color(0xFF241016), floorBottom = Color(0xFF0A0306),
        groundEdge = Color(0xFF6B2230), groundEdgeDeep = Color(0xFF3A121A), groundLine = Color(0xFFB05060),
        ambient = Color(0xFFFF9C8A), ambientCount = 52
    )
}

internal fun biomeVisuals(key: String): BiomeVisuals = when (key) {
    "crypts" -> BiomeVisuals(
        walls = listOf("env/brick_gray1.png", "env/brick_gray0.png", "env/brick_gray2.png"),
        floors = listOf("env/cobble_blood1.png", "env/cobble_blood2.png", "env/cobble_blood3.png"),
        statues = listOf("env/statue_ancient_hero.png", "env/statue_angel.png", "env/statue_wraith.png"),
        props = listOf("env/crumbled_column_1.png", "env/generic.png", "env/plant_crypt.png"),
        weather = "dust",
        gate = "closed"
    )
    "catacombs" -> BiomeVisuals(
        walls = listOf("env/catacombs0.png", "env/catacombs3.png", "env/catacombs5.png"),
        floors = listOf("env/crypt10.png", "env/crypt11.png", "env/cobble_blood2.png"),
        statues = listOf("env/statue_cat.png", "env/statue_naga.png", "env/statue_hydra.png"),
        props = listOf("env/crumbled_column_2.png", "env/crumbled_column_3.png", "env/plant_crypt.png"),
        weather = "spores",
        gate = "runed"
    )
    "ash" -> BiomeVisuals(
        walls = listOf("env/volcanic_wall0.png", "env/volcanic_wall1.png", "env/volcanic_wall2.png"),
        floors = listOf("env/dirt0.png", "env/dirt1.png", "env/demonic_red1.png"),
        statues = listOf("env/statue_dragon.png", "env/statue_demonic_bust.png", "env/statue_iron.png"),
        props = listOf("env/blood_fountain.png", "env/dry_fountain.png", "env/crumbled_column_4.png"),
        weather = "embers",
        gate = "closed",
        fireClouds = true
    )
    "forge" -> BiomeVisuals(
        walls = listOf("env/lab-metal0.png", "env/lab-metal1.png", "env/metal_wall.png"),
        floors = listOf("env/black_cobalt01.png", "env/black_cobalt02.png", "env/black_cobalt03.png"),
        statues = listOf("env/statue_iron.png", "env/statue_orb_guardian.png", "env/statue_sword.png"),
        props = listOf("env/blue_fountain.png", "env/sparkling_fountain.png", "env/crumbled_column_3.png"),
        weather = "sparks",
        gate = "runed",
        fireClouds = true
    )
    "peaks" -> BiomeVisuals(
        walls = listOf("env/zot_blue0.png", "env/crystal_wall_white.png", "env/crystal_wall_lightblue.png"),
        floors = listOf("env/ice0.png", "env/ice1.png", "env/ice2.png"),
        statues = listOf("env/statue_angel.png", "env/statue_archer.png", "env/statue_dragon.png"),
        props = listOf("env/tree1_lightred.png", "env/tree2_lightred.png", "env/sparkling_fountain.png"),
        weather = "snow",
        gate = "runed"
    )
    "woods" -> BiomeVisuals(
        walls = listOf("env/lair0.png", "env/lair1.png", "env/lair2.png"),
        floors = listOf("env/grass0.png", "env/grass1.png", "env/grass_flowers_blue1.png"),
        statues = listOf("env/statue_cat.png", "env/statue_ancient_hero.png", "env/statue_archer.png"),
        props = listOf("env/tree1_yellow.png", "env/tree2_yellow.png", "env/mangrove1.png"),
        weather = "leaves",
        gate = "runed"
    )
    "abyss" -> BiomeVisuals(
        walls = listOf("env/slime3.png", "env/slime4.png", "env/slime5.png"),
        floors = listOf("env/acidic_floor0.png", "env/acidic_floor1.png", "env/liquefaction1.png"),
        statues = listOf("env/statue_hydra.png", "env/statue_naga.png", "env/statue_demonic_bust.png"),
        props = listOf("env/plant_crypt.png", "env/bush2.png", "env/bush3.png"),
        weather = "spores",
        gate = "sealed"
    )
    else -> BiomeVisuals(
        walls = listOf("env/hell01.png", "env/hell02.png", "env/hell03.png"),
        floors = listOf("env/demonic_red2.png", "env/demonic_red3.png", "env/black_cobalt04.png"),
        statues = listOf("env/statue_cerebov.png", "env/statue_demonic_bust.png", "env/statue_iron.png"),
        props = listOf("env/blood_fountain.png", "env/crumbled_column_4.png", "env/dry_fountain.png"),
        weather = "embers",
        gate = "sealed",
        fireClouds = true
    )
}


@Composable
fun Scene(
    state: GameState,
    onCamp: () -> Unit,
    onChest: () -> Unit,
    onConsumables: () -> Unit,
    reducedMotion: Boolean = false,
    vibrations: Boolean = true,
    modifier: Modifier = Modifier
) {
    val walking = GameEngine.isWalking(state)
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    var previousLevel by remember { mutableStateOf(state.level) }
    var levelBanner by remember { mutableStateOf(0) }

    LaunchedEffect(state.level) {
        val previous = previousLevel
        previousLevel = state.level
        if (state.level > previous) {
            levelBanner = state.level
            SoundFx.play(context, "level", 0.95f)
            if (vibrations) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(2200)
            levelBanner = 0
        }
    }

    val bossName = state.fight?.takeIf { it.isBoss }?.enemyName
    LaunchedEffect(bossName) {
        if (bossName != null) SoundFx.play(context, "boss", 0.9f)
    }

    BoxWithConstraints(modifier.fillMaxSize().background(Ink)) {
        val sceneWidth = maxWidth
        val sceneHeight = maxHeight

        SceneBackground(state, density.density)

        if (state.fight == null && state.distance >= 85) {
            GateView(
                state,
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 0.dp, bottom = 58.dp)
                    .width(166.dp)
                    .height(134.dp)
            )
        }

        TopHud(state, onCamp, Modifier.align(Alignment.TopStart))
        ProgressHeader(state, Modifier.align(Alignment.TopStart).padding(top = 38.dp))

        HeroUnit(
            state,
            walking,
            reducedMotion,
            Modifier.align(Alignment.BottomStart).padding(start = 18.dp, bottom = 58.dp)
        )

        state.fight?.let { fight ->
            key(fight.enemyName, fight.enemyLevel, fight.isBoss, fight.farm) {
                EnemyUnit(
                    fight,
                    reducedMotion,
                    Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 58.dp)
                )
            }
        }

        if (state.chest != null) {
            ChestChip(
                state,
                onChest,
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 92.dp, end = 10.dp)
            )
        }
        ConsumablesChip(
            state,
            onConsumables,
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 58.dp, end = 10.dp)
        )
        BuffTags(
            state,
            Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
        )

        if (state.fight?.isBoss == true) {
            BossBanner(state)
            val fight = state.fight
            if (fight != null && fight.enemyHp < fight.enemyMaxHp * 0.5) {
                SceneTag(
                    "PHASE 2 · EMBRASÉ",
                    Ember,
                    Modifier.align(Alignment.TopCenter).padding(top = 104.dp)
                )
            }
            if (state.bossBlessing > 0) {
                SceneTag(
                    "BÉNÉDICTION +25 %",
                    Green,
                    Modifier.align(Alignment.TopCenter).padding(top = 128.dp)
                )
            }
        }
        if (state.mode == "farmWait" || state.fight?.farm == true) {
            SceneTag("ZONE DE REPLI · FARM", Blood, Modifier.align(Alignment.TopCenter).padding(top = 58.dp))
        }
        if (state.mode == "chest") {
            SceneTag("FORÇAGE DU COFFRE", Gold, Modifier.align(Alignment.TopCenter).padding(top = 58.dp))
        }
        if (levelBanner > 0) {
            LevelUpBanner(
                level = levelBanner,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 152.dp)
            )
        }

        BottomHud(state, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun rememberAssets(paths: List<String>): List<ImageBitmap?> {
    val result = ArrayList<ImageBitmap?>(paths.size)
    for (path in paths) result.add(rememberAsset(path))
    return result
}

private fun DrawScope.drawWeather(
    palette: BiomePalette,
    weather: String,
    drift: Float,
    floor: Int
) {
    val w = size.width
    val h = size.height
    val random = java.util.Random((floor / 10).toLong() + 41L)
    val count = when (weather) {
        "snow" -> 70
        "sparks" -> 54
        "embers" -> 46
        "leaves" -> 38
        "spores" -> 52
        else -> palette.ambientCount
    }
    repeat(count) {
        val seed = random.nextFloat()
        val baseX = random.nextFloat() * w
        val baseY = random.nextFloat() * h * 0.85f
        val phase = (drift + seed) % 1f
        var x = baseX
        var y = baseY
        var radius = 1.6.dp.toPx()
        var color = palette.ambient
        when (weather) {
            "snow" -> {
                y = (baseY + phase * h * 0.9f) % (h * 0.85f)
                x = (baseX + phase * w * 0.03f) % w
                radius = 2.2.dp.toPx()
                color = Color.White
            }
            "embers" -> {
                y = (baseY - phase * h * 0.65f + h) % h
                x = (baseX + phase * w * 0.05f) % w
                radius = 1.8.dp.toPx()
                color = palette.torch
            }
            "sparks" -> {
                y = (baseY - phase * h * 0.8f + h) % h
                x = (baseX + phase * w * 0.02f) % w
                radius = 1.4.dp.toPx()
                color = palette.torchLight
            }
            "leaves" -> {
                y = (baseY + phase * h * 0.65f) % (h * 0.85f)
                x = (baseX + phase * w * 0.18f) % w
                radius = 2.4.dp.toPx()
                color = if (seed > 0.5f) palette.torch else palette.ambient
            }
            "spores" -> {
                y = (baseY - phase * h * 0.25f + h) % h
                x = (baseX + phase * w * 0.08f) % w
                radius = 2.0.dp.toPx()
                color = palette.ambient
            }
            else -> {
                y = (baseY + phase * h * 0.25f) % (h * 0.85f)
                x = (baseX + phase * w * 0.04f) % w
                radius = if (seed > 0.5f) 1.dp.toPx() else 1.8.dp.toPx()
            }
        }
        drawCircle(
            color = color.copy(alpha = 0.20f + seed * 0.35f),
            radius = radius,
            center = Offset(x, y)
        )
    }
}

@Composable
private fun SceneBackground(state: GameState, density: Float) {
    val biomeKey = biomeKeyFor(state.floor)
    val palette = biomePalette(biomeKey)
    val visuals = biomeVisuals(biomeKey)
    val walls = rememberAssets(visuals.walls)
    val floors = rememberAssets(visuals.floors)
    val statues = rememberAssets(visuals.statues)
    val props = rememberAssets(visuals.props)
    val fireCloud = rememberAsset("fx/cloud_fire0.png")
    val weather = rememberInfiniteTransition(label = "weather")
    val drift by weather.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart),
        label = "drift"
    )

    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val tile = 68.dp.toPx()
        val groundTop = h * 0.80f
        val archTop = h * 0.30f
        val archHeight = h * 0.46f

        drawRect(
            Brush.verticalGradient(
                listOf(palette.top, palette.mid, palette.bottom)
            )
        )

        if (walls.isNotEmpty()) {
            var row = 0
            var y = 0f
            while (y < groundTop) {
                var col = 0
                var x = 0f
                while (x < w) {
                    val wall = walls[(row + col) % walls.size]
                    if (wall != null) {
                        drawImage(
                            image = wall,
                            dstOffset = IntOffset(x.roundToInt(), y.roundToInt()),
                            dstSize = IntSize(tile.roundToInt(), tile.roundToInt()),
                            alpha = 0.22f,
                            filterQuality = FilterQuality.None
                        )
                    }
                    x += tile
                    col++
                }
                y += tile
                row++
            }
            drawRect(palette.wallTint.copy(alpha = 0.16f))
        }

        drawRect(
            Brush.verticalGradient(
                listOf(Color(0xE607050C), Color(0x33150F22), Color(0xCC09060F)),
                startY = 0f,
                endY = h
            )
        )

        val archXs = listOf(w * 0.20f, w * 0.50f, w * 0.80f)
        var statueIndex = 0
        archXs.forEach { cx ->
            val left = cx - w * 0.12f
            drawRoundRect(
                color = Color(0xFF0D0917),
                topLeft = Offset(left, archTop),
                size = Size(w * 0.24f, archHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.12f, w * 0.12f)
            )
            drawRoundRect(
                color = Color(0xFF2D223B),
                topLeft = Offset(left, archTop),
                size = Size(w * 0.24f, archHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.12f, w * 0.12f),
                style = Stroke(width = 3.dp.toPx())
            )
            drawRoundRect(
                color = Color(0x332D223B),
                topLeft = Offset(left + 4.dp.toPx(), archTop + 4.dp.toPx()),
                size = Size(w * 0.24f - 8.dp.toPx(), archHeight - 8.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.12f, w * 0.12f)
            )
            val statue = if (statues.isNotEmpty()) statues[statueIndex % statues.size] else null
            if (statue != null) {
                val ss = 58.dp.toPx()
                drawImage(
                    image = statue,
                    dstOffset = IntOffset((cx - ss / 2).roundToInt(), (groundTop - ss / 2).roundToInt()),
                    dstSize = IntSize(ss.roundToInt(), ss.roundToInt()),
                    alpha = 0.55f,
                    filterQuality = FilterQuality.None
                )
            }
            statueIndex++
        }

        if (props.isNotEmpty()) {
            val positions = listOf(0.08f, 0.92f, 0.66f)
            props.forEachIndexed { index, prop ->
                if (prop != null && index < positions.size) {
                    val ps = 52.dp.toPx()
                    drawImage(
                        image = prop,
                        dstOffset = IntOffset(
                            (w * positions[index] - ps / 2).roundToInt(),
                            (groundTop - ps * 0.9f).roundToInt()
                        ),
                        dstSize = IntSize(ps.roundToInt(), ps.roundToInt()),
                        alpha = 0.46f,
                        filterQuality = FilterQuality.None
                    )
                }
            }
        }

        if (visuals.fireClouds && fireCloud != null) {
            listOf(0.24f, 0.76f).forEach { fraction ->
                val cs = 56.dp.toPx()
                drawImage(
                    image = fireCloud,
                    dstOffset = IntOffset(
                        (w * fraction - cs / 2).roundToInt(),
                        (groundTop - cs * 0.75f).roundToInt()
                    ),
                    dstSize = IntSize(cs.roundToInt(), cs.roundToInt()),
                    alpha = 0.24f,
                    filterQuality = FilterQuality.None
                )
            }
        }

        // bannières
        listOf(0.31f, 0.62f).forEach { fraction ->
            val bw = 24.dp.toPx()
            val bh = h * 0.25f
            drawRoundRect(
                color = Color(0xFF4A1D26),
                topLeft = Offset(w * fraction - bw / 2, 0f),
                size = Size(bw, bh),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
            drawRect(
                color = Color(0x88C4984E),
                topLeft = Offset(w * fraction - bw / 2, bh * 0.38f),
                size = Size(bw, 1.5.dp.toPx())
            )
        }

        // torches
        listOf(0.20f, 0.50f, 0.80f).forEach { fraction ->
            val cx = w * fraction
            val cy = h * 0.32f
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(
                        palette.torch.copy(alpha = 0.33f),
                        palette.torch.copy(alpha = 0.07f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = 72.dp.toPx()
                ),
                radius = 72.dp.toPx(),
                center = Offset(cx, cy)
            )
            val flame = Path().apply {
                moveTo(cx, cy - 9.dp.toPx())
                quadraticBezierTo(cx + 7.dp.toPx(), cy - 1.dp.toPx(), cx + 2.dp.toPx(), cy + 8.dp.toPx())
                quadraticBezierTo(cx, cy + 10.dp.toPx(), cx - 4.dp.toPx(), cy + 6.dp.toPx())
                quadraticBezierTo(cx - 7.dp.toPx(), cy - 1.dp.toPx(), cx, cy - 9.dp.toPx())
                close()
            }
            drawPath(flame, Brush.verticalGradient(listOf(palette.torchLight, palette.torch, palette.torchDeep)))
            drawRoundRect(
                color = Color(0xFF3A2414),
                topLeft = Offset(cx - 3.dp.toPx(), cy + 9.dp.toPx()),
                size = Size(6.dp.toPx(), 16.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx())
            )
        }

        // sol
        drawRect(
            Brush.verticalGradient(listOf(palette.floorTop, palette.floorBottom)),
            topLeft = Offset(0f, groundTop),
            size = Size(w, h - groundTop)
        )
        if (floors.isNotEmpty()) {
            val gt = 54.dp.toPx()
            var row = 0
            var y = groundTop
            while (y < h) {
                var col = 0
                var x = 0f
                while (x < w) {
                    val floor = floors[(row + col) % floors.size]
                    if (floor != null) {
                        drawImage(
                            image = floor,
                            dstOffset = IntOffset(x.roundToInt(), y.roundToInt()),
                            dstSize = IntSize(gt.roundToInt(), gt.roundToInt()),
                            alpha = 0.33f,
                            filterQuality = FilterQuality.None
                        )
                    }
                    x += gt
                    col++
                }
                y += gt
                row++
            }
        }
        drawRect(
            Brush.verticalGradient(listOf(palette.groundEdge, palette.groundEdgeDeep)),
            topLeft = Offset(0f, groundTop - 2.dp.toPx()),
            size = Size(w, 4.dp.toPx())
        )
        drawLine(
            color = palette.groundLine,
            start = Offset(0f, groundTop - 2.dp.toPx()),
            end = Offset(w, groundTop - 2.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )

        drawWeather(palette, visuals.weather, drift, state.floor)

        // rochers de premier plan
        drawCircle(Color(0xFF0A0710), radius = 38.dp.toPx(), center = Offset(w * 0.04f, h + 8.dp.toPx()))
        drawCircle(Color(0xFF0A0710), radius = 46.dp.toPx(), center = Offset(w * 0.96f, h + 12.dp.toPx()))
        drawCircle(Color(0xFF0A0710), radius = 18.dp.toPx(), center = Offset(w * 0.53f, h + 2.dp.toPx()))

        drawRect(
            Brush.radialGradient(
                listOf(Color.Transparent, Color(0xCC040208)),
                center = Offset(w / 2f, h * 0.52f),
                radius = w * 0.72f
            )
        )
    }
}

@Composable
private fun TopHud(state: GameState, onCamp: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier
                .clip(RoundedCornerShape(3.dp))
                .background(Ink.copy(alpha = 0.80f))
                .border(1.dp, Gold.copy(alpha = 0.3f), RoundedCornerShape(3.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(4.dp).background(Ember))
            Spacer(Modifier.width(4.dp))
            Text("PALIER ", fontSize = 7.sp, letterSpacing = 0.5.sp, color = Muted)
            Text(
                state.floor.toString().padStart(2, '0'),
                fontFamily = Press,
                fontSize = 7.sp,
                color = GoldLight
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            biomeFor(state.floor),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            fontSize = 6.5.sp,
            letterSpacing = 1.sp,
            color = Muted.copy(alpha = 0.75f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(6.dp))
        CurrencyChip("item/01.png", formatInt(state.gold))
        Spacer(Modifier.width(5.dp))
        Box(
            Modifier
                .size(29.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Brush.verticalGradient(listOf(Panel2, Ink2)))
                .border(1.dp, Gold.copy(alpha = 0.34f), RoundedCornerShape(3.dp))
                .clickable(onClick = onCamp)
                .semantics { contentDescription = "Feu de camp" },
            contentAlignment = Alignment.Center
        ) {
            FlameIcon(Modifier.size(14.dp))
        }
    }
}

@Composable
private fun ProgressHeader(state: GameState, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .weight(1f)
                .height(7.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Panel)
                .border(1.dp, Line, RoundedCornerShape(2.dp))
        ) {
            Box(
                Modifier
                    .fillMaxWidth((state.distance / FLOOR_METERS).toFloat().coerceIn(0f, 1f))
                    .fillMaxSize()
                    .background(Brush.horizontalGradient(listOf(GoldDeep, Gold, GoldLight)))
            )
            Row(Modifier.fillMaxSize()) {
                repeat(10) {
                    Box(Modifier.weight(1f).fillMaxSize().border(0.3.dp, Color(0x22FFFFFF)))
                }
            }
        }
        Spacer(Modifier.width(6.dp))
        Text(
            "${state.distance.roundToInt()} / $FLOOR_METERS m",
            fontSize = 6.5.sp,
            color = Parchment.copy(alpha = 0.75f),
            minLines = 1
        )
    }
}

@Composable
private fun UnitPlate(
    name: String,
    level: Int,
    hp: Double,
    maxHp: Int,
    enemy: Boolean = false,
    width: Int = 124,
    subtitle: String? = null
) {
    Column(
        Modifier
            .width(width.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Ink.copy(alpha = 0.82f))
            .border(1.dp, if (enemy) Blood.copy(alpha = 0.42f) else Gold.copy(alpha = 0.3f), RoundedCornerShape(3.dp))
            .padding(horizontal = 5.dp, vertical = 3.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                name.uppercase(),
                fontSize = 6.5.sp,
                letterSpacing = 0.4.sp,
                fontWeight = FontWeight.Bold,
                color = if (enemy) Color(0xFFFFD5C8) else Parchment,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(3.dp))
            Text("NIV.${level.toString().padStart(2, '0')}", fontSize = 5.5.sp, color = Muted)
        }
        if (subtitle != null) {
            Text(
                subtitle,
                fontSize = 5.2.sp,
                color = if (enemy) Gold else Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(1.dp))
        }
        Spacer(Modifier.height(2.dp))
        MiniBar(
            fraction = (hp / maxHp.toDouble()).toFloat(),
            color = if (enemy) Blood else Green,
            height = 3.dp
        )
    }
}

private fun heroBodyLayer(body: Item?): String? {
    if (body == null) return null
    val name = body.name.lowercase()
    val icon = body.icon.lowercase()
    return when {
        name.contains("robe") || name.contains("suaire") || icon.contains("robe") ->
            "hero/body_robe.png"
        icon.contains("ice_dragon") -> "hero/body_metal_blue.png"
        icon.contains("shadow_dragon") -> "hero/body_mesh_black.png"
        name.contains("plate") || name.contains("harnois") || name.contains("carapace") ||
            icon.contains("plate") || icon.contains("gold_dragon") ->
            "hero/body_plate.png"
        name.contains("maille") || name.contains("cotte") ||
            icon.contains("scale_mail") || icon.contains("ring_mail") ->
            "hero/body_mail_gray.png"
        name.contains("cuir") || name.contains("peau") || name.contains("écorce") ||
            name.contains("mousse") || name.contains("feuille") ||
            icon.contains("leather") || icon.contains("animal_skin") ->
            "hero/body_leather.png"
        name.contains("tunique") || name.contains("manteau") || name.contains("veste") ||
            icon.contains("coat") ->
            "hero/body_coat.png"
        body.rarity == "legendary" -> "hero/aragorn.png"
        else -> "hero/coat_red.png"
    }
}

private fun heroHeadLayer(head: Item?): String? {
    if (head == null) return null
    val name = head.name.lowercase()
    val icon = head.icon.lowercase()
    val high = head.rarity == "rare" || head.rarity == "epic" || head.rarity == "legendary"
    return when {
        name.contains("capuche") || name.contains("masque") || icon.contains("hood") ->
            "hero/head_hood.png"
        name.contains("couronne") || icon.contains("crown") -> "hero/head_crown.png"
        name.contains("elfique") -> "hero/head_hood_green.png"
        name.contains("art") -> "hero/helm_plume.png"
        name.contains("gravé") || name.contains("grave") || name.contains("cryptes") ->
            "hero/head_helm_gray.png"
        name.contains("plume") -> "hero/helm_plume.png"
        name.contains("heaume") || name.contains("casque") ||
            icon.contains("helmet") || icon.contains("helm") ->
            when {
                head.rarity == "legendary" -> "hero/horned.png"
                high -> "hero/head_helm_gray.png"
                else -> "hero/helm_green.png"
            }
        head.rarity == "legendary" -> "hero/horned.png"
        high -> "hero/head_helm_gray.png"
        else -> "hero/helm_green.png"
    }
}

private fun heroBootsLayer(feet: Item?): String? {
    if (feet == null) return null
    val name = feet.name.lowercase()
    val icon = feet.icon.lowercase()
    return when {
        name.contains("mousse") -> "hero/middle_green.png"
        name.contains("cime") || name.contains("givre") || name.contains("glace") ->
            "hero/boots_metal.png"
        name.contains("soie") || name.contains("limier") || name.contains("feuille") ||
            name.contains("cuir") || icon.contains("leather") -> "hero/boots_leather.png"
        name.contains("métal") || name.contains("fer") || name.contains("acier") ||
            icon.contains("metal") -> "hero/boots_metal.png"
        feet.rarity == "epic" || feet.rarity == "rare" || feet.rarity == "legendary" ->
            "hero/middle_green.png"
        else -> "hero/middle_brown2.png"
    }
}

private fun heroLegsLayer(body: Item?): String? {
    if (body == null) return "hero/pants_brown.png"
    val name = body.name.lowercase()
    val icon = body.icon.lowercase()
    return when {
        name.contains("plate") || name.contains("harnois") || name.contains("carapace") ||
            icon.contains("plate") || icon.contains("gold_dragon") -> "hero/legs_plate.png"
        name.contains("maille") || name.contains("cotte") ||
            icon.contains("scale_mail") || icon.contains("ring_mail") -> "hero/legs_mail.png"
        name.contains("robe") || name.contains("suaire") || name.contains("tunique") ||
            name.contains("manteau") || icon.contains("robe") || icon.contains("coat") ->
            "hero/legs_cloth.png"
        else -> "hero/pants_brown.png"
    }
}

private fun heroWeaponLayer(weapon: Item?): String? {
    if (weapon == null) return null
    val name = weapon.name.lowercase()
    val icon = weapon.icon.lowercase()
    val high = weapon.rarity == "rare" || weapon.rarity == "epic" || weapon.rarity == "legendary"
    return when {
        icon.contains("staff") || name.contains("bâton") || name.contains("sceptre") ->
            "hero/weapon_staff.png"
        icon.contains("dagger") || name.contains("dague") -> "hero/weapon_dagger.png"
        icon.contains("axe") || name.contains("hache") -> "hero/weapon_axe.png"
        icon.contains("mace") || icon.contains("eveningstar") || icon.contains("flail") ||
            name.contains("masse") || name.contains("marteau") || name.contains("fléau") ->
            "hero/weapon_mace.png"
        icon.contains("spear") || icon.contains("lance") || icon.contains("halberd") ||
            name.contains("lance") || name.contains("hallebarde") -> "hero/weapon_spear.png"
        icon.contains("whip") || name.contains("fouet") -> "hero/weapon_whip.png"
        icon.contains("greatsword") || (high && (name.contains("épée") || name.contains("lame"))) ->
            "hero/weapon_greatsword.png"
        high -> "hero/long_sword_slant.png"
        else -> "hero/broadsword.png"
    }
}

@Composable
private fun HeroUnit(
    state: GameState,
    walking: Boolean,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val stats = statsOf(state)
    val weapon = equipped(state, "weapon")
    val body = equipped(state, "body")
    val head = equipped(state, "head")
    val feet = equipped(state, "feet")
    val bodyLayer = heroBodyLayer(body)
    val headLayer = heroHeadLayer(head)
    val bootsLayer = heroBootsLayer(feet)
    val legsLayer = heroLegsLayer(body)
    val weaponLayer = heroWeaponLayer(weapon)
    val context = LocalContext.current
    val floats = remember { mutableStateListOf<FloatFx>() }
    var previousHp by remember { mutableStateOf(state.hp) }
    var nextId by remember { mutableLongStateOf(0L) }

    LaunchedEffect(state.hp) {
        val diff = state.hp - previousHp
        previousHp = state.hp
        if (reducedMotion) return@LaunchedEffect
        if (diff < -3.0) {
            SoundFx.play(context, "hurt", 0.8f)
            floats += FloatFx(
                id = nextId++,
                text = "-${(-diff).roundToInt()}",
                color = Color(0xFFFFB0A6),
                drift = ((nextId % 5) - 2) * 6f
            )
        } else if (diff > 3.0) {
            floats += FloatFx(
                id = nextId++,
                text = "+${diff.roundToInt()}",
                color = Green,
                drift = ((nextId % 5) - 2) * 6f
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "hero")
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = -3f,
        animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse),
        label = "bob"
    )
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        UnitPlate("Kaelen", state.level, state.hp, stats.maxHp)
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier.size(100.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (!reducedMotion && state.ferveur > 0) {
                FerveurAura(
                    ferveur = state.ferveur,
                    modifier = Modifier
                        .size(104.dp)
                        .align(Alignment.BottomCenter)
                )
            }
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .width(68.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.5f))
            )
            Box(
                Modifier
                    .size(96.dp)
                    .align(Alignment.BottomCenter)
                    .offset(y = if (walking && !reducedMotion) bob.dp else 0.dp)
            ) {
                AssetImage("hero/human_m.png", Modifier.fillMaxSize())
                bodyLayer?.let { AssetImage(it, Modifier.fillMaxSize()) }
                legsLayer?.let { AssetImage(it, Modifier.fillMaxSize()) }
                bootsLayer?.let { AssetImage(it, Modifier.fillMaxSize()) }
                headLayer?.let { AssetImage(it, Modifier.fillMaxSize()) }
                weaponLayer?.let { AssetImage(it, Modifier.fillMaxSize()) }
                AssetImage("hero/buckler_round.png", Modifier.fillMaxSize())
            }
            floats.forEach { fx ->
                key(fx.id) {
                    Box(Modifier.align(Alignment.TopCenter)) {
                        FloatingNumber(fx) { floats.remove(fx) }
                    }
                }
            }
        }
    }
}

@Composable
private fun EnemyUnit(
    fight: com.brunothesatellite.idlesteprpg.engine.FightData,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val height = if (fight.isBoss) 148.dp else 108.dp
    val plateWidth = if (fight.isBoss) 154 else 126
    val spriteSize = if (fight.isBoss) 136.dp else 102.dp
    val context = LocalContext.current
    val floats = remember { mutableStateListOf<FloatFx>() }
    val flash = remember { Animatable(0f) }
    val slash = remember { Animatable(0f) }
    var previousHp by remember { mutableStateOf(fight.enemyHp) }
    var nextId by remember { mutableLongStateOf(0L) }

    LaunchedEffect(fight.enemyHp) {
        val damage = previousHp - fight.enemyHp
        previousHp = fight.enemyHp
        if (damage > 0.5) {
            SoundFx.play(
                context,
                if (fight.lastCrit) "crit" else "hit",
                if (fight.lastCrit) 1f else 0.55f
            )
            if (!reducedMotion) {
                floats += FloatFx(
                    id = nextId++,
                    text = "-${damage.roundToInt()}" + if (fight.lastCrit) " !" else "",
                    color = if (fight.lastCrit) GoldLight else Color(0xFFFFC6B8),
                    drift = ((nextId % 5) - 2) * 7f
                )
            }
            coroutineScope {
                launch {
                    flash.snapTo(0.55f)
                    flash.animateTo(0f, tween(220))
                }
                if (!reducedMotion) {
                    launch {
                        slash.snapTo(0f)
                        slash.animateTo(1f, tween(260, easing = LinearEasing))
                    }
                }
            }
        }
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        UnitPlate(
            fight.enemyName,
            fight.enemyLevel,
            fight.enemyHp,
            fight.enemyMaxHp.roundToInt(),
            enemy = true,
            width = plateWidth,
            subtitle = if (fight.isBoss && fight.abilityLabel.isNotBlank()) {
                "CAPACITÉ · ${fight.abilityLabel}"
            } else null
        )
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier.height(height).width(if (fight.isBoss) 140.dp else 112.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (fight.isBoss && fight.enemyHp < fight.enemyMaxHp * 0.5 && !reducedMotion) {
                BossAura(Modifier.size(spriteSize).align(Alignment.BottomCenter))
            }
            if (fight.abilityFlashMs > 0.0 && fight.abilityLabel.isNotBlank()) {
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 2.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Blood.copy(alpha = 0.88f))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        fight.abilityLabel,
                        fontFamily = Press,
                        fontSize = 5.5.sp,
                        color = GoldLight,
                        maxLines = 1
                    )
                }
            }
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .width(if (fight.isBoss) 86.dp else 54.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.5f))
            )
            AssetImage(fight.enemyIcon, Modifier.size(spriteSize))
            Box(
                Modifier
                    .size(spriteSize)
                    .align(Alignment.BottomCenter)
                    .alpha(flash.value * 0.7f)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFFFFF1D0), Color(0x33FFF1D0), Color.Transparent)
                        ),
                        CircleShape
                    )
            )
            Canvas(Modifier.size(spriteSize).align(Alignment.BottomCenter)) {
                val p = slash.value
                if (p > 0f && p < 1f) {
                    val startX = size.width * 0.12f
                    val startY = size.height * 0.12f
                    val endX = size.width * 0.88f
                    val endY = size.height * 0.88f
                    drawLine(
                        color = Color.White.copy(alpha = (1f - p) * 0.9f),
                        start = Offset(startX, startY),
                        end = Offset(startX + (endX - startX) * p, startY + (endY - startY) * p),
                        strokeWidth = 4.dp.toPx()
                    )
                    drawLine(
                        color = GoldLight.copy(alpha = (1f - p) * 0.5f),
                        start = Offset(startX, startY),
                        end = Offset(startX + (endX - startX) * p, startY + (endY - startY) * p),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
            }
            floats.forEach { fx ->
                key(fx.id) {
                    Box(Modifier.align(Alignment.TopCenter)) {
                        FloatingNumber(fx) { floats.remove(fx) }
                    }
                }
            }
            if (fight.elite) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF2A1A0D))
                        .border(1.dp, Gold, RoundedCornerShape(2.dp))
                        .padding(horizontal = 3.dp, vertical = 1.dp)
                ) {
                    Text("ÉLITE", fontFamily = Press, fontSize = 4.5.sp, color = GoldLight)
                }
            }
        }
    }
}

private data class FloatFx(
    val id: Long,
    val text: String,
    val color: Color,
    val drift: Float
)

@Composable
private fun FloatingNumber(fx: FloatFx, onDone: () -> Unit) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        anim.animateTo(1f, tween(820, easing = LinearEasing))
        onDone()
    }
    Text(
        fx.text,
        modifier = Modifier
            .offset(x = fx.drift.dp, y = (-8f - 34f * anim.value).dp)
            .alpha((1f - anim.value * 0.85f).coerceIn(0f, 1f)),
        fontFamily = Press,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = fx.color
    )
}

@Composable
private fun BossAura(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "bossAura")
    val pulse by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(720), RepeatMode.Reverse),
        label = "pulse"
    )
    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.62f
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xAAFF6B3A).copy(alpha = 0.5f * pulse), Color(0x33FF9448), Color.Transparent),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
    }
}

@Composable
private fun FerveurAura(ferveur: Int, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "ferveur")
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "ferveurPulse"
    )
    val strength = (ferveur / 100f).coerceIn(0.15f, 1f)
    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height * 0.62f)
        val radius = size.minDimension * 0.55f
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    Ember.copy(alpha = 0.34f * strength * pulse),
                    Color(0x22FF6B3A),
                    Color.Transparent
                ),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
    }
}

@Composable
private fun LevelUpBanner(level: Int, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(5.dp))
            .background(Brush.horizontalGradient(listOf(GoldLight, Gold, Color(0xFFC8954A))))
            .border(1.dp, GoldLight, RoundedCornerShape(5.dp))
            .padding(horizontal = 18.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("NIVEAU $level", fontFamily = Press, fontSize = 11.sp, color = Ink)
            Text("Kaelen se renforce", fontSize = 6.sp, color = Ink.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun GateView(state: GameState, modifier: Modifier = Modifier) {
    val active = state.distance >= 90 || state.fight?.isBoss == true
    val gateStyle = biomeVisuals(biomeKeyFor(state.floor)).gate
    val gatePrefix = when (gateStyle) {
        "runed" -> "env/gate_runed"
        "sealed" -> "env/gate_sealed"
        else -> "env/gate_closed"
    }
    val left = rememberAsset("${gatePrefix}_left.png")
    val middle = rememberAsset("${gatePrefix}_middle.png")
    val right = rememberAsset("${gatePrefix}_right.png")
    Box(modifier, contentAlignment = Alignment.BottomCenter) {
        Canvas(Modifier.fillMaxSize()) {
            drawRoundRect(
                color = Color(0xCC090611),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.42f, size.width * 0.42f),
                size = size
            )
            drawRoundRect(
                color = if (active) Gold.copy(alpha = 0.5f) else Color(0xFF3A2C4C),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.42f, size.width * 0.42f),
                size = size,
                style = Stroke(width = 3.dp.toPx())
            )
            if (active) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0x44FF6B4A), Color.Transparent),
                        center = Offset(size.width / 2, size.height * 0.65f),
                        radius = size.width * 0.55f
                    ),
                    radius = size.width * 0.55f,
                    center = Offset(size.width / 2, size.height * 0.65f)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "GARDIEN",
                fontFamily = Press,
                fontSize = 4.5.sp,
                color = if (active) GoldLight else Muted2,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(bottom = 1.dp)
            )
            if (state.distance < FLOOR_METERS) {
                val remaining = (FLOOR_METERS - state.distance).roundToInt().coerceAtLeast(0)
                val waiting = state.stepBank <= 0.0 && state.fight == null
                Text(
                    if (waiting) "EN ATTENTE DE PAS" else "À $remaining m",
                    fontSize = 4.2.sp,
                    color = if (waiting) Ember else Muted,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Row(Modifier.alpha(if (active) 1f else 0.55f)) {
                listOf(left, middle, right).forEach { bmp ->
                    if (bmp != null) {
                        Image(
                            bitmap = bmp,
                            contentDescription = null,
                            modifier = Modifier.size(52.dp),
                            filterQuality = FilterQuality.None
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsumablesChip(state: GameState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val count = state.consumables.values.sum()
    Row(
        modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Ink.copy(alpha = 0.88f))
            .border(1.dp, if (count > 0) Gold.copy(alpha = 0.5f) else Line, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Sac de consommables" }
            .padding(horizontal = 7.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AssetIcon("item/cons_potion_red.png", 15.dp)
        Spacer(Modifier.width(5.dp))
        Column {
            Text("SAC", fontFamily = Press, fontSize = 5.sp, color = GoldLight)
            Text(if (count > 0) "×$count" else "vide", fontSize = 4.8.sp, color = if (count > 0) Ember else Muted2)
        }
    }
}

@Composable
private fun BuffTags(state: GameState, modifier: Modifier = Modifier) {
    val now = System.currentTimeMillis()
    val active = state.buffs.filter { it.expiresAt > now }.take(4)
    if (active.isEmpty()) return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        active.forEach { buff ->
            val seconds = ((buff.expiresAt - now) / 1000L).coerceAtLeast(0L)
            Box(
                Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(Ink.copy(alpha = 0.82f))
                    .border(1.dp, Ember.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    "${buff.label} ${seconds}s",
                    fontSize = 4.8.sp,
                    color = Ember,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ChestChip(state: GameState, onChest: () -> Unit, modifier: Modifier = Modifier) {
    val chest = state.chest ?: return
    val pending = 1 + state.chestQueue.size
    Row(
        modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Ink.copy(alpha = 0.88f))
            .border(1.dp, Gold.copy(alpha = 0.55f), RoundedCornerShape(4.dp))
            .clickable(onClick = onChest)
            .semantics { contentDescription = "Coffre en attente" }
            .padding(horizontal = 7.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Gold.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text("C", fontFamily = Press, fontSize = 5.sp, color = GoldLight)
        }
        Spacer(Modifier.width(5.dp))
        Column {
            Text(
                "COFFRE" + if (pending > 1) " ×$pending" else "",
                fontFamily = Press,
                fontSize = 5.sp,
                color = GoldLight
            )
            Text(formatRemaining(chestRemainingMs(chest)), fontSize = 4.8.sp, color = Ember)
        }
    }
}

@Composable
private fun ChestButton(state: GameState, onChest: () -> Unit, modifier: Modifier = Modifier) {
    val chest = state.chest ?: return
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(58.dp)
                .clickable(onClick = onChest)
                .semantics { contentDescription = "Coffre" }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawRoundRect(
                    color = Color(0x665A3418),
                    topLeft = Offset(w * 0.08f, h * 0.20f),
                    size = Size(w * 0.84f, h * 0.72f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                )
                drawRect(Color(0xFF6B3F1D), Offset(w * 0.12f, h * 0.28f), Size(w * 0.76f, h * 0.22f))
                drawRect(Color(0xFF4A2A13), Offset(w * 0.12f, h * 0.50f), Size(w * 0.76f, h * 0.36f))
                drawRect(Color(0xFFE8C27A), Offset(w * 0f, h * 0.48f), Size(w, h * 0.10f))
                drawRect(Color(0xFFFFD479), Offset(w * 0.08f, h * 0.16f), Size(w * 0.12f, h * 0.26f))
                drawRect(Color(0xFFFFD479), Offset(w * 0.80f, h * 0.16f), Size(w * 0.12f, h * 0.26f))
                drawRect(Color(0xFF2A1A0E), Offset(w * 0.41f, h * 0.54f), Size(w * 0.18f, h * 0.18f))
                drawRect(Color(0xFFE8C27A), Offset(w * 0.44f, h * 0.57f), Size(w * 0.12f, h * 0.10f))
                drawRect(Color(0xFF2A1A0E), Offset(w * 0.48f, h * 0.60f), Size(w * 0.04f, h * 0.07f))
            }
            if (state.chestQueue.isNotEmpty()) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Gold)
                        .padding(horizontal = 3.dp, vertical = 1.dp)
                ) {
                    Text(
                        "×${1 + state.chestQueue.size}",
                        fontFamily = Press,
                        fontSize = 5.sp,
                        color = Ink
                    )
                }
            }
        }
        if (chest.progress > 0) {
            MiniBar(
                fraction = (chest.progress / chest.quota).toFloat(),
                color = Gold,
                modifier = Modifier.width(52.dp),
                height = 3.dp
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            formatRemaining(chestRemainingMs(chest)),
            fontSize = 5.sp,
            color = Ember
        )
    }
}

@Composable
private fun BottomHud(state: GameState, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(
            Modifier
                .weight(1.15f)
                .clip(RoundedCornerShape(4.dp))
                .background(Ink.copy(alpha = 0.84f))
                .border(1.dp, Gold.copy(alpha = 0.18f), RoundedCornerShape(4.dp))
                .padding(horizontal = 7.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FlameIcon(Modifier.size(11.dp))
            Spacer(Modifier.width(5.dp))
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Ferveur", fontSize = 6.5.sp, color = Muted)
                    Text("${state.ferveur}", fontFamily = Press, fontSize = 6.5.sp, color = Ember)
                }
                MiniBar(state.ferveur / 100f, Ember, Modifier.fillMaxWidth(), height = 3.dp)
            }
            Spacer(Modifier.width(4.dp))
            Text(tierName(state.ferveur), fontSize = 5.5.sp, color = Ember)
        }
        Row(
            Modifier
                .weight(0.85f)
                .clip(RoundedCornerShape(4.dp))
                .background(Ink.copy(alpha = 0.84f))
                .border(1.dp, Gold.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                .padding(horizontal = 7.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssetIcon("item/boots4_green.png", 12.dp)
            Spacer(Modifier.width(5.dp))
            Column {
                Text(formatInt(state.stepsToday), fontFamily = Press, fontSize = 7.sp, color = Parchment, maxLines = 1)
                Text("pas aujourd'hui", fontSize = 5.5.sp, color = Muted)
            }
        }
    }
}

@Composable
private fun BossBanner(state: GameState) {
    val fight = state.fight ?: return
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp)
            .padding(top = 72.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Ink.copy(alpha = 0.95f))
            .border(1.dp, Blood, RoundedCornerShape(4.dp))
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("GARDIEN DU PALIER", fontFamily = Press, fontSize = 7.sp, color = Blood)
            Text("${fight.enemyName} · Niv. ${fight.enemyLevel}", fontSize = 7.sp, color = Muted)
            if (fight.modifier.isNotBlank()) {
                Text("MODIFICATEUR · ${fight.modifier}", fontSize = 5.5.sp, color = Ember)
            }
        }
    }
}

@Composable
private fun SceneTag(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(2.dp))
            .background(Ink.copy(alpha = 0.88f))
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(text, fontSize = 5.5.sp, letterSpacing = 0.5.sp, color = color)
    }
}

@Composable
fun FlameIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.50f, 0f)
            quadraticBezierTo(w * 0.74f, h * 0.24f, w * 0.70f, h * 0.44f)
            quadraticBezierTo(w * 0.88f, h * 0.40f, w * 0.92f, h * 0.64f)
            quadraticBezierTo(w * 0.96f, h * 0.92f, w * 0.50f, h)
            quadraticBezierTo(w * 0.04f, h * 0.92f, w * 0.08f, h * 0.64f)
            quadraticBezierTo(w * 0.12f, h * 0.40f, w * 0.30f, h * 0.44f)
            quadraticBezierTo(w * 0.26f, h * 0.24f, w * 0.50f, 0f)
            close()
        }
        drawPath(path, Brush.verticalGradient(listOf(GoldLight, Ember, Color(0xFF8F2D19))))
    }
}

