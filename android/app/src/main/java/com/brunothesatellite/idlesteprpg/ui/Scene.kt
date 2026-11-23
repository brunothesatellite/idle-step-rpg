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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.brunothesatellite.idlesteprpg.BuildConfig
import com.brunothesatellite.idlesteprpg.R
import com.brunothesatellite.idlesteprpg.audio.SoundFx
import com.brunothesatellite.idlesteprpg.engine.Buff
import com.brunothesatellite.idlesteprpg.engine.FLOOR_METERS
import com.brunothesatellite.idlesteprpg.engine.FightData
import com.brunothesatellite.idlesteprpg.engine.GameEngine
import com.brunothesatellite.idlesteprpg.engine.GameState
import com.brunothesatellite.idlesteprpg.engine.HitEvent
import com.brunothesatellite.idlesteprpg.engine.Item
import com.brunothesatellite.idlesteprpg.engine.STEPS_PER_METER
import com.brunothesatellite.idlesteprpg.engine.biomeFor
import com.brunothesatellite.idlesteprpg.engine.biomeKeyFor
import com.brunothesatellite.idlesteprpg.engine.bossFor
import com.brunothesatellite.idlesteprpg.engine.chestRemainingMs
import com.brunothesatellite.idlesteprpg.engine.equipped
import com.brunothesatellite.idlesteprpg.engine.formatRemaining
import com.brunothesatellite.idlesteprpg.engine.milestoneBonus
import com.brunothesatellite.idlesteprpg.engine.mobPoolFor
import com.brunothesatellite.idlesteprpg.engine.statsOf
import com.brunothesatellite.idlesteprpg.engine.tierIndex
import com.brunothesatellite.idlesteprpg.engine.tierName
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.roundToInt

internal data class BiomePalette(
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

internal fun biomePalette(key: String): BiomePalette = when (key) {
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
    onSync: () -> Unit = {},
    onTogglePause: () -> Unit = {},
    connected: Boolean = false,
    syncing: Boolean = false,
    lastSync: Long = 0L,
    lastError: String = "",
    reducedMotion: Boolean = false,
    vibrations: Boolean = true,
    paused: Boolean = false,
    modifier: Modifier = Modifier
) {
    val walking = GameEngine.isWalking(state) && !paused
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

    // Garde le dernier ennemi à l'écran quelques instants après sa mort :
    // sinon le float de dégâts, qui vit dans EnemyUnit, disparaît avec lui et
    // le joueur ne voit jamais le coup qui a tué le monstre (surtout en farm,
    // où le héros one-shot). On distingue une victoire d'une défaite par le
    // compteur de retraites : une défaite incrémente `retreats`.
    var lastFight by remember { mutableStateOf<FightData?>(state.fight) }
    var lastRetreats by remember { mutableIntStateOf(state.retreats) }
    LaunchedEffect(state.fight, state.retreats) {
        val current = state.fight
        if (current != null) {
            lastFight = current
            lastRetreats = state.retreats
        } else {
            val dying = lastFight
            val victory = state.retreats == lastRetreats
            if (dying != null && victory) {
                delay(900)
                if (state.fight == null) lastFight = null
            } else {
                lastFight = null
            }
        }
    }
    val lingers = state.fight == null && lastFight != null && state.retreats == lastRetreats
    val displayedFight = state.fight ?: lastFight?.takeIf { lingers }?.let { dying ->
        // Le dernier état vu peut encore avoir ses PV pleins : la barre doit
        // être vide pendant l'affichage du cadavre.
        if (dying.enemyHp > 0.5) dying.copy(enemyHp = 0.0) else dying
    }

    // Rien ne peut bouger (réserve vide, pas de combat) : au bout de 10 s on gèle
    // le décor animé pour ne plus redessiner 60 fois par seconde.
    val sceneIdle = paused || (!walking && state.fight == null && state.special == null &&
        state.stepBank <= 0.0)
    var decorationsFrozen by remember { mutableStateOf(false) }
    LaunchedEffect(sceneIdle) {
        if (!sceneIdle) {
            decorationsFrozen = false
        } else {
            delay(10_000)
            decorationsFrozen = true
        }
    }
    val animateDecor = !reducedMotion && !decorationsFrozen && !paused

    BoxWithConstraints(modifier.fillMaxSize().background(Ink)) {
        val sceneWidth = maxWidth
        val sceneHeight = maxHeight

        SceneBackground(state, density.density, animateDecor)

        if (state.fight == null && state.distance >= 85) {
            GateView(
                state,
                animateDecor,
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 0.dp, bottom = 58.dp)
                    .width(160.dp)
                    .height(170.dp)
            )
        }

        TopHud(state, onCamp, Modifier.align(Alignment.TopStart))
        ProgressHeader(state, Modifier.align(Alignment.TopStart).padding(top = 38.dp))
        SyncButton(
            connected = connected,
            syncing = syncing,
            lastSync = lastSync,
            lastError = lastError,
            onClick = onSync,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 10.dp, top = 58.dp)
        )
        PauseButton(
            paused = paused,
            onClick = onTogglePause,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 10.dp, top = 104.dp)
        )
        if (paused) {
            SceneTag(
                stringResource(R.string.scene_pause),
                Ember,
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 56.dp, top = 112.dp)
            )
        }

        HeroUnit(
            state,
            walking,
            reducedMotion,
            animateDecor,
            Modifier.align(Alignment.BottomStart).padding(start = 18.dp, bottom = 58.dp)
        )

        displayedFight?.let { fight ->
            key(fight.enemyName, fight.enemyLevel, fight.isBoss, fight.farm) {
                EnemyUnit(
                    fight = fight,
                    reducedMotion = reducedMotion,
                    hits = state.recentHits,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 58.dp)
                )
            }
        }

        val waitingForSteps = state.stepBank <= 0.0 && displayedFight == null
        if (waitingForSteps && state.distance < 85) {
            WaitingMob(
                state,
                animateDecor,
                Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 58.dp)
            )
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

        if (state.fight?.isBoss == true) {
            BossBanner(state)
            val fight = state.fight
            if (fight != null && fight.enemyHp < fight.enemyMaxHp * 0.5) {
                SceneTag(
                    stringResource(R.string.scene_phase2),
                    Ember,
                    Modifier.align(Alignment.TopCenter).padding(top = 74.dp)
                )
            }
            if (state.bossBlessing > 0) {
                SceneTag(
                    stringResource(R.string.scene_blessing),
                    Green,
                    Modifier.align(Alignment.TopCenter).padding(top = 96.dp)
                )
            }
        }
        if (state.mode == "farmWait" || state.fight?.farm == true) {
            val step = (state.farmKills + 1).coerceIn(1, 3)
            SceneTag(
                stringResource(R.string.scene_farm_zone, step),
                Blood,
                Modifier.align(Alignment.TopCenter).padding(top = 58.dp)
            )
        }
        if (state.mode == "chest") {
            SceneTag(stringResource(R.string.scene_chest_force), Gold, Modifier.align(Alignment.TopCenter).padding(top = 58.dp))
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
internal fun rememberAssets(paths: List<String>): List<ImageBitmap?> {
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

/**
 * Dessine le couloir d'un biome : fond, mur estompé, arches (les « fenêtres »),
 * statues, props au sol, bannières, torches, sol et ligne d'arête.
 *
 * Partagé par la scène et par le bandeau du Codex, pour qu'ils montrent
 * exactement la même chose. Toutes les tailles sont proportionnelles au canevas
 * (fractions calibrées sur le rendu de la scène, ~327 dp de haut).
 */
internal fun DrawScope.drawCorridor(
    palette: BiomePalette,
    walls: List<ImageBitmap?>,
    floors: List<ImageBitmap?>,
    statues: List<ImageBitmap?>,
    props: List<ImageBitmap?>,
    fireCloud: ImageBitmap?,
    withFireClouds: Boolean
) {
    val w = size.width
    val h = size.height
    val tile = h * 0.208f
    val groundTop = h * 0.80f
    val archTop = h * 0.30f
    val archHeight = h * 0.46f
    val archWidth = w * 0.24f
    // Le rayon suit la hauteur de l'arche (et non la largeur seule), sinon la
    // forme devient une pilule quand le canevas est très aplati (bandeau).
    val archRadiusValue = minOf(w * 0.12f, archHeight * 0.29f)
    val archRadius = androidx.compose.ui.geometry.CornerRadius(archRadiusValue, archRadiusValue)
    val archInset = h * 0.012f
    val archStroke = (h * 0.009f).coerceAtLeast(1f)

    // Fond atmosphérique
    drawRect(
        Brush.verticalGradient(
            listOf(palette.top, palette.mid, palette.bottom)
        )
    )

    // Mur : simple toile de fond très estompée
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

    // Arches et statues
    val archXs = listOf(w * 0.20f, w * 0.50f, w * 0.80f)
    val statueSize = h * 0.177f
    var statueIndex = 0
    archXs.forEach { cx ->
        val left = cx - archWidth / 2f
        drawRoundRect(
            color = Color(0xFF0D0917),
            topLeft = Offset(left, archTop),
            size = Size(archWidth, archHeight),
            cornerRadius = archRadius
        )
        drawRoundRect(
            color = Color(0xFF2D223B),
            topLeft = Offset(left, archTop),
            size = Size(archWidth, archHeight),
            cornerRadius = archRadius,
            style = Stroke(width = archStroke)
        )
        drawRoundRect(
            color = Color(0x332D223B),
            topLeft = Offset(left + archInset, archTop + archInset),
            size = Size(archWidth - archInset * 2f, archHeight - archInset * 2f),
            cornerRadius = archRadius
        )
        val statue = if (statues.isNotEmpty()) statues[statueIndex % statues.size] else null
        if (statue != null) {
            drawImage(
                image = statue,
                dstOffset = IntOffset(
                    (cx - statueSize / 2f).roundToInt(),
                    (groundTop - statueSize / 2f).roundToInt()
                ),
                dstSize = IntSize(statueSize.roundToInt(), statueSize.roundToInt()),
                alpha = 0.55f,
                filterQuality = FilterQuality.None
            )
        }
        statueIndex++
    }

    // Props posés sur la ligne de sol
    if (props.isNotEmpty()) {
        val positions = listOf(0.08f, 0.92f, 0.66f)
        val propSize = h * 0.159f
        props.forEachIndexed { index, prop ->
            if (prop != null && index < positions.size) {
                drawImage(
                    image = prop,
                    dstOffset = IntOffset(
                        (w * positions[index] - propSize / 2f).roundToInt(),
                        (groundTop - propSize * 0.9f).roundToInt()
                    ),
                    dstSize = IntSize(propSize.roundToInt(), propSize.roundToInt()),
                    alpha = 0.46f,
                    filterQuality = FilterQuality.None
                )
            }
        }
    }

    // Nuages de feu (cavernes de cendre)
    if (withFireClouds && fireCloud != null) {
        val cloudSize = h * 0.171f
        listOf(0.24f, 0.76f).forEach { fraction ->
            drawImage(
                image = fireCloud,
                dstOffset = IntOffset(
                    (w * fraction - cloudSize / 2f).roundToInt(),
                    (groundTop - cloudSize * 0.75f).roundToInt()
                ),
                dstSize = IntSize(cloudSize.roundToInt(), cloudSize.roundToInt()),
                alpha = 0.24f,
                filterQuality = FilterQuality.None
            )
        }
    }

    // Bannières suspendues
    val bannerWidth = w * 0.067f
    val bannerHeight = h * 0.25f
    listOf(0.31f, 0.62f).forEach { fraction ->
        drawRoundRect(
            color = Color(0xFF4A1D26),
            topLeft = Offset(w * fraction - bannerWidth / 2f, 0f),
            size = Size(bannerWidth, bannerHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
        drawRect(
            color = Color(0x88C4984E),
            topLeft = Offset(w * fraction - bannerWidth / 2f, bannerHeight * 0.38f),
            size = Size(bannerWidth, (h * 0.0046f).coerceAtLeast(1f))
        )
    }

    // Torches
    val glow = h * 0.220f
    val flame = h * 0.030f
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
                radius = glow
            ),
            radius = glow,
            center = Offset(cx, cy)
        )
        val flamePath = Path().apply {
            moveTo(cx, cy - flame)
            quadraticBezierTo(cx + flame * 0.78f, cy - flame * 0.11f, cx + flame * 0.22f, cy + flame * 0.89f)
            quadraticBezierTo(cx, cy + flame * 1.11f, cx - flame * 0.44f, cy + flame * 0.67f)
            quadraticBezierTo(cx - flame * 0.78f, cy - flame * 0.11f, cx, cy - flame)
            close()
        }
        drawPath(flamePath, Brush.verticalGradient(listOf(palette.torchLight, palette.torch, palette.torchDeep)))
        drawRoundRect(
            color = Color(0xFF3A2414),
            topLeft = Offset(cx - w * 0.008f, cy + flame),
            size = Size(w * 0.017f, h * 0.049f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx())
        )
    }

    // Sol
    drawRect(
        Brush.verticalGradient(listOf(palette.floorTop, palette.floorBottom)),
        topLeft = Offset(0f, groundTop),
        size = Size(w, h - groundTop)
    )
    if (floors.isNotEmpty()) {
        val floorTile = h * 0.165f
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
                        dstSize = IntSize(floorTile.roundToInt(), floorTile.roundToInt()),
                        alpha = 0.33f,
                        filterQuality = FilterQuality.None
                    )
                }
                x += floorTile
                col++
            }
            y += floorTile
            row++
        }
    }
    val edgeHeight = h * 0.012f
    drawRect(
        Brush.verticalGradient(listOf(palette.groundEdge, palette.groundEdgeDeep)),
        topLeft = Offset(0f, groundTop - edgeHeight / 2f),
        size = Size(w, edgeHeight)
    )
    drawLine(
        color = palette.groundLine,
        start = Offset(0f, groundTop - edgeHeight / 2f),
        end = Offset(w, groundTop - edgeHeight / 2f),
        strokeWidth = (h * 0.003f).coerceAtLeast(1f)
    )
}

@Composable
private fun SceneBackground(state: GameState, density: Float, animate: Boolean) {
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
    // Valeur figée quand le décor est gelé : plus aucune lecture de l'animation
    // dans le dessin, donc plus aucune invalidation par frame.
    var frozenDrift by remember { mutableStateOf(0f) }
    LaunchedEffect(animate) {
        if (!animate) frozenDrift = drift
    }

    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawCorridor(palette, walls, floors, statues, props, fireCloud, visuals.fireClouds)

        drawWeather(palette, visuals.weather, if (animate) drift else frozenDrift, state.floor)

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
private fun SyncButton(
    connected: Boolean,
    syncing: Boolean,
    lastSync: Long,
    lastError: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dotColor = when {
        !connected -> Muted2
        lastError.isNotBlank() -> Blood
        lastSync == 0L -> Ember
        System.currentTimeMillis() - lastSync < 30L * 60L * 1000L -> Green
        else -> Ember
    }
    val label = when {
        syncing -> stringResource(R.string.scene_sync_syncing)
        connected -> stringResource(R.string.scene_sync_sync)
        else -> stringResource(R.string.scene_sync_connect)
    }
    Box(
        modifier
            .size(40.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Ink.copy(alpha = 0.88f))
            .border(
                1.dp,
                if (connected) Gold.copy(alpha = 0.55f) else Line2,
                RoundedCornerShape(5.dp)
            )
            .clickable(enabled = !syncing, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        if (syncing) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = GoldLight,
                strokeWidth = 2.dp
            )
        } else {
            SyncGlyph(
                color = if (connected) GoldLight else Muted,
                modifier = Modifier.size(20.dp)
            )
        }
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
    }
}

@Composable
private fun SyncGlyph(color: Color, modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(com.brunothesatellite.idlesteprpg.R.drawable.ic_sync),
        contentDescription = null,
        tint = color,
        modifier = modifier
    )
}

@Composable
private fun PauseButton(
    paused: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pauseDescription = if (paused) {
        stringResource(R.string.scene_resume_walk)
    } else {
        stringResource(R.string.scene_pause_walk)
    }
    Box(
        modifier
            .size(40.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Ink.copy(alpha = 0.88f))
            .border(
                1.dp,
                if (paused) Ember.copy(alpha = 0.75f) else Line2,
                RoundedCornerShape(5.dp)
            )
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = pauseDescription
            },
        contentAlignment = Alignment.Center
    ) {
        PauseGlyph(
            paused = paused,
            color = if (paused) GoldLight else Muted,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun PauseGlyph(paused: Boolean, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (paused) {
            val path = Path().apply {
                moveTo(size.width * 0.28f, 0f)
                lineTo(size.width, size.height * 0.5f)
                lineTo(size.width * 0.28f, size.height)
                close()
            }
            drawPath(path, color)
        } else {
            val bar = size.width * 0.24f
            val gap = size.width * 0.20f
            val left = (size.width - bar * 2f - gap) / 2f
            drawRect(color, topLeft = Offset(left, 0f), size = Size(bar, size.height))
            drawRect(
                color,
                topLeft = Offset(left + bar + gap, 0f),
                size = Size(bar, size.height)
            )
        }
    }
}

@Composable
private fun TopHud(state: GameState, onCamp: () -> Unit, modifier: Modifier = Modifier) {
    val devBuildDescription = stringResource(R.string.scene_dev_build)
    val campDescription = stringResource(R.string.scene_camp)
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
            Text(stringResource(R.string.scene_floor_prefix) + " ", fontSize = 7.sp, letterSpacing = 0.5.sp, color = Muted)
            Text(
                state.floor.toString().padStart(2, '0'),
                fontFamily = Press,
                fontSize = 7.sp,
                color = GoldLight
            )
        }
        if (BuildConfig.DEBUG) {
            Spacer(Modifier.width(5.dp))
            Box(
                Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Ink.copy(alpha = 0.80f))
                    .border(1.dp, Gold.copy(alpha = 0.35f), RoundedCornerShape(3.dp))
                    .semantics { contentDescription = devBuildDescription },
                contentAlignment = Alignment.Center
            ) {
                AssetIcon("mon/worm.png", 16.dp)
            }
        }
        Spacer(Modifier.width(6.dp))
        Text(
            localizedBiomeName(biomeFor(state.floor)),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            fontSize = 6.5.sp,
            letterSpacing = 1.sp,
            color = Muted.copy(alpha = 0.75f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (state.holdFloor) {
            Spacer(Modifier.width(4.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(Ember.copy(alpha = 0.20f))
                    .border(1.dp, Ember.copy(alpha = 0.60f), RoundedCornerShape(3.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text("FARM", fontFamily = Press, fontSize = 5.sp, color = Ember)
            }
        }
        Spacer(Modifier.width(6.dp))
        CurrencyChip("item/01.png", formatCompact(state.gold))
        Spacer(Modifier.width(5.dp))
        Box(
            Modifier
                .size(29.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Brush.verticalGradient(listOf(Panel2, Ink2)))
                .border(1.dp, Gold.copy(alpha = 0.34f), RoundedCornerShape(3.dp))
                .clickable(onClick = onCamp)
                .semantics { contentDescription = campDescription },
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
            Text(
                formatLevelShort(level),
                fontSize = 5.5.sp,
                color = Muted,
                maxLines = 1,
                softWrap = false
            )
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
    animateDecor: Boolean,
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
    var pendingHeal by remember { mutableFloatStateOf(0f) }
    var nextId by remember { mutableLongStateOf(0L) }
    var lastRetreats by remember { mutableIntStateOf(state.retreats) }
    var lastHitSeq by remember { mutableLongStateOf(state.recentHits.maxOfOrNull { it.seq } ?: 0L) }

    LaunchedEffect(state.hp, state.retreats) {
        val diff = state.hp - previousHp
        previousHp = state.hp
        val retreated = state.retreats > lastRetreats
        lastRetreats = state.retreats
        if (reducedMotion) return@LaunchedEffect
        // La remontée à 60 % après une défaite n'est pas un soin : on ne
        // l'affiche pas comme un float vert.
        if (diff > 3.0 && !retreated) {
            // Les soins sont cumulés : au plus 4 floats par seconde, plutôt
            // qu'un float par tick (10/s) qui finissait par masquer le héros.
            pendingHeal += diff.toFloat()
        }
    }
    LaunchedEffect(state.recentHits) {
        val newHits = state.recentHits.filter { it.seq > lastHitSeq && !it.onEnemy }
        if (newHits.isEmpty()) return@LaunchedEffect
        lastHitSeq = state.recentHits.maxOfOrNull { it.seq } ?: lastHitSeq
        if (reducedMotion) return@LaunchedEffect
        val now = System.currentTimeMillis()
        newHits.forEach { hit ->
            if (now - hit.at <= 1_500L) {
                SoundFx.play(context, "hurt", 0.8f)
                addFloat(
                    floats,
                    FloatFx(nextId++, hit.damage, "-", Color(0xFFFFB0A6), driftOf(nextId))
                )
            }
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(250)
            if (pendingHeal >= 3f) {
                val amount = pendingHeal.roundToInt()
                pendingHeal = 0f
                addFloat(
                    floats,
                    FloatFx(nextId++, amount, "+", Green, driftOf(nextId), durationMs = 360)
                )
            }
        }
    }

    val transition = rememberInfiniteTransition(label = "hero")
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (animateDecor) -3f else 0f,
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
                    animate = animateDecor,
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
                    .offset(y = if (walking && animateDecor) bob.dp else 0.dp)
            ) {
                if (state.floor >= 100) {
                    GoldenAura(
                        Modifier.align(Alignment.Center).size(176.dp),
                        animate = !reducedMotion
                    )
                }
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
                    Box(Modifier.align(Alignment.Center)) {
                        FloatingNumber(fx, maxWidth = 70.dp) { floats.remove(fx) }
                    }
                }
            }
        }
    }
}

@Composable
private fun EnemyUnit(
    fight: FightData,
    reducedMotion: Boolean,
    hits: List<HitEvent>,
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
    var lastHitSeq by remember { mutableLongStateOf(hits.maxOfOrNull { it.seq } ?: 0L) }

    LaunchedEffect(hits) {
        val newHits = hits.filter { it.seq > lastHitSeq && it.onEnemy }
        if (newHits.isEmpty()) return@LaunchedEffect
        lastHitSeq = hits.maxOfOrNull { it.seq } ?: lastHitSeq
        if (reducedMotion) return@LaunchedEffect
        val now = System.currentTimeMillis()
        newHits.forEach { hit ->
            if (now - hit.at <= 1_500L) {
                SoundFx.play(
                    context,
                    if (hit.crit) "crit" else "hit",
                    if (hit.crit) 1f else 0.55f
                )
                addFloat(
                    floats,
                    FloatFx(
                        nextId++,
                        hit.damage,
                        "-",
                        if (hit.crit) GoldLight else Color(0xFFFFC6B8),
                        driftOf(nextId)
                    )
                )
            }
        }
    }

    LaunchedEffect(fight.enemyHp) {
        val damage = previousHp - fight.enemyHp
        previousHp = fight.enemyHp
        if (damage > 0.5) {
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
            localizedCreature(fight.enemyName),
            fight.enemyLevel,
            fight.enemyHp,
            fight.enemyMaxHp.roundToInt(),
            enemy = true,
            width = plateWidth,
            subtitle = if (fight.isBoss && fight.abilityLabel.isNotBlank()) {
                stringResource(R.string.codex_ability_line, abilityLabelRes(fight.ability))
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
                    Box(Modifier.align(Alignment.Center)) {
                        FloatingNumber(
                            fx,
                            maxWidth = if (fight.isBoss) 100.dp else 78.dp
                        ) { floats.remove(fx) }
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
                    Text(stringResource(R.string.scene_elite), fontFamily = Press, fontSize = 4.5.sp, color = GoldLight)
                }
            }
        }
    }
}

private data class FloatFx(
    val id: Long,
    val amount: Int,
    val sign: String,
    val color: Color,
    val drift: Float,
    val durationMs: Int = 820
)

/** Plafond visuel : jamais plus de 4 nombres vivants par unité. */
private fun addFloat(list: MutableList<FloatFx>, fx: FloatFx) {
    while (list.size >= 4) list.removeAt(0)
    list += fx
}

private fun driftOf(id: Long): Float = ((id % 5) - 2) * 1.5f

@Composable
private fun FloatingNumber(fx: FloatFx, maxWidth: Dp, onDone: () -> Unit) {
    val anim = remember { Animatable(0f) }
    val fontScale = LocalDensity.current.fontScale
    val text = fx.sign + formatFloatNumber(fx.amount)
    // Press Start 2P est monospace : 1 caractère = 1 em. On réduit la taille
    // juste assez pour que le nombre reste dans la boîte du sprite, même à
    // l'échelle de texte maximale.
    val fontSize = (maxWidth.value / (text.length.coerceAtLeast(1) * fontScale))
        .coerceIn(6.5f, 10f)
    LaunchedEffect(Unit) {
        anim.animateTo(1f, tween(fx.durationMs, easing = LinearEasing))
        onDone()
    }
    Text(
        text,
        modifier = Modifier
            .offset(x = fx.drift.dp, y = (12f - 40f * anim.value).dp)
            .alpha((1f - anim.value * 0.85f).coerceIn(0f, 1f)),
        fontFamily = Press,
        fontSize = fontSize.sp,
        fontWeight = FontWeight.Bold,
        color = fx.color,
        style = TextStyle(
            shadow = Shadow(
                color = Color.Black.copy(alpha = 0.85f),
                offset = Offset(2f, 2f),
                blurRadius = 4f
            )
        ),
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        textAlign = TextAlign.Center
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
private fun FerveurAura(ferveur: Int, animate: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "ferveur")
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "ferveurPulse"
    )
    val strength = (ferveur / 100f).coerceIn(0.15f, 1f)
    Canvas(modifier) {
        // Sans animation, la lueur reste fixe : aucune lecture de `pulse` dans le
        // dessin, donc plus d'invalidation par frame.
        val pulseValue = if (animate) pulse else 0.78f
        val center = Offset(size.width / 2f, size.height * 0.62f)
        val radius = size.minDimension * 0.55f
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    Ember.copy(alpha = 0.34f * strength * pulseValue),
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
            Text(stringResource(R.string.scene_level, level), fontFamily = Press, fontSize = 11.sp, color = Ink)
            Text(stringResource(R.string.scene_level_note), fontSize = 6.sp, color = Ink.copy(alpha = 0.7f))
        }
    }
}

/** Silhouette d'un rôdeur : teinte unie, lisible sur le sol sombre du couloir. */
private val Silhouette = Color(0xFF4A3A63)

@Composable
private fun rememberIdleBob(animate: Boolean, label: String): Float {
    val transition = rememberInfiniteTransition(label = label)
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (animate) 1f else 0f,
        animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse),
        label = "${label}Bob"
    )
    return bob
}

/**
 * Le prochain combat n'est tiré qu'au démarrage (`Random.Default`) : on ne peut
 * donc pas annoncer son identité. On montre une silhouette du biome qui sautille,
 * avec la distance restante avant la rencontre, plutôt qu'un couloir vide.
 */
@Composable
private fun WaitingMob(state: GameState, animateDecor: Boolean, modifier: Modifier = Modifier) {
    val pool = remember(state.floor) {
        val biomePool = mobPoolFor(state.floor)
        (biomePool.mid + biomePool.hard + biomePool.easy).ifEmpty { biomePool.easy }
    }
    val template = remember(pool, state.floor, state.nextFight) {
        if (pool.isEmpty()) {
            null
        } else {
            pool[Math.floorMod(state.floor * 7 + state.nextFight / 10, pool.size)]
        }
    }
    val art = template?.let { rememberAsset(it.icon) }
    val bob = rememberIdleBob(animateDecor, "waitingMob")
    val remaining = (state.nextFight - state.distance).roundToInt().coerceAtLeast(1)

    Column(
        modifier = modifier.graphicsLayer {
            translationY = if (animateDecor) -bob * 3.dp.toPx() else 0f
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (art != null) {
            Image(
                bitmap = art,
                contentDescription = null,
                modifier = Modifier.size(102.dp),
                filterQuality = FilterQuality.None,
                colorFilter = ColorFilter.tint(Silhouette, BlendMode.SrcIn)
            )
            Spacer(Modifier.height(3.dp))
        }
        SceneTag(stringResource(R.string.scene_waiting_steps), Ember)
        Spacer(Modifier.height(2.dp))
        Text(stringResource(R.string.scene_next_fight, remaining), fontSize = 4.6.sp, color = Muted)
    }
}

@Composable
private fun GateView(state: GameState, animateDecor: Boolean = false, modifier: Modifier = Modifier) {
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
    val guardian = remember(state.floor) { bossFor(state.floor) }
    val guardianArt = rememberAsset(guardian.icon)
    val guardianBob = rememberIdleBob(animateDecor, "guardian")
    Box(modifier, contentAlignment = Alignment.BottomCenter) {
        Canvas(Modifier.fillMaxSize()) {
            if (active) {
                // Halo discret derrière le gardien : plus de cadre en pilule.
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0x33FF6B4A), Color(0x11FF6B4A), Color.Transparent),
                        center = Offset(size.width / 2, size.height * 0.78f),
                        radius = size.width * 0.60f
                    ),
                    radius = size.width * 0.60f,
                    center = Offset(size.width / 2, size.height * 0.78f)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.codex_guardian_badge),
                fontFamily = Press,
                fontSize = 4.5.sp,
                color = if (active) GoldLight else Muted2,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(bottom = 1.dp)
            )
            Text(
                creatureNameRes(guardian.name),
                fontSize = 4.4.sp,
                color = if (active) Blood else Muted2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Box(contentAlignment = Alignment.BottomCenter) {
                Row(Modifier.alpha(if (active) 1f else 0.55f)) {
                    listOf(left, middle, right).forEach { bmp ->
                        if (bmp != null) {
                            Image(
                                bitmap = bmp,
                                contentDescription = null,
                                modifier = Modifier.width(44.dp).height(116.dp),
                                contentScale = ContentScale.FillBounds,
                                filterQuality = FilterQuality.None
                            )
                        }
                    }
                }
                if (guardianArt != null) {
                    Image(
                        bitmap = guardianArt,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(bottom = 2.dp)
                            .size(96.dp)
                            .graphicsLayer {
                                translationY = if (animateDecor) -guardianBob * 2.dp.toPx() else 0f
                            },
                        filterQuality = FilterQuality.None
                    )
                }
            }
            if (state.distance < FLOOR_METERS) {
                val remaining = (FLOOR_METERS - state.distance).roundToInt().coerceAtLeast(0)
                val waiting = state.stepBank <= 0.0 && state.fight == null
                Text(
                    if (waiting) stringResource(R.string.scene_waiting_steps) else stringResource(R.string.scene_away, remaining),
                    fontSize = 4.2.sp,
                    color = if (waiting) Ember else Muted,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun ConsumablesChip(state: GameState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val count = state.consumables.values.sum()
    val bagDescription = stringResource(R.string.scene_bag_desc)
    Row(
        modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Ink.copy(alpha = 0.88f))
            .border(1.dp, if (count > 0) Gold.copy(alpha = 0.5f) else Line, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = bagDescription }
            .padding(horizontal = 7.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AssetIcon("item/cons_potion_red.png", 15.dp)
        Spacer(Modifier.width(5.dp))
        Column {
            Text(stringResource(R.string.scene_bag), fontFamily = Press, fontSize = 5.sp, color = GoldLight)
            Text(if (count > 0) "×$count" else stringResource(R.string.scene_empty), fontSize = 4.8.sp, color = if (count > 0) Ember else Muted2)
        }
    }
}

@Composable
private fun BuffStrip(state: GameState, modifier: Modifier = Modifier) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val hasBuffs = state.buffs.any { it.expiresAt > now }
    LaunchedEffect(hasBuffs) {
        if (!hasBuffs) return@LaunchedEffect
        while (true) {
            delay(500)
            now = System.currentTimeMillis()
        }
    }
    if (!hasBuffs) return
    val active = state.buffs
        .filter { it.expiresAt > now }
        .sortedBy { it.expiresAt }
        .take(6)
    val descriptionParts = mutableListOf<String>()
    for (buff in active) {
        descriptionParts += "${buffLabelRes(buff.label)} ${buffTime(buff.expiresAt - now)}"
    }
    val description = descriptionParts.joinToString(", ")
    val effectsDescription = stringResource(R.string.scene_active_effects, description)
    Row(
        modifier.semantics { contentDescription = effectsDescription },
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        active.forEach { buff ->
            val color = buffColor(buff)
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Ink.copy(alpha = 0.86f))
                    .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(3.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${buffLabelRes(buff.label)} ${buffTime(buff.expiresAt - now)}",
                    fontSize = 4.6.sp,
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        // Largeur plafonnée à 1/3 : on complète la ligne avec des slots vides
        // quand il y a moins de trois effets.
        repeat((3 - active.size).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
    }
}

private fun buffTime(remainingMs: Long): String {
    val total = (remainingMs / 1000L).coerceAtLeast(0L)
    return if (total < 60L) "${total}s" else "${total / 60}:${(total % 60).toString().padStart(2, '0')}"
}

private fun buffColor(buff: Buff): Color = when {
    buff.type == "vulnerable" || buff.magnitude < 0.0 -> Blood
    buff.type == "def" -> Color(0xFF86B8D9)
    buff.type == "vit" -> Color(0xFF8FD39B)
    buff.type == "crit" -> GoldLight
    buff.type == "lifesteal" -> Color(0xFFC79BE0)
    buff.type == "regen" -> Color(0xFF9BE0B0)
    else -> Ember
}

@Composable
private fun ChestChip(state: GameState, onChest: () -> Unit, modifier: Modifier = Modifier) {
    val chest = state.chest ?: return
    val pending = 1 + state.chestQueue.size
    val chestPendingDescription = stringResource(R.string.scene_chest_pending_desc)
    Row(
        modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Ink.copy(alpha = 0.88f))
            .border(1.dp, Gold.copy(alpha = 0.55f), RoundedCornerShape(4.dp))
            .clickable(onClick = onChest)
            .semantics { contentDescription = chestPendingDescription }
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
                stringResource(R.string.scene_chest) + if (pending > 1) " ×$pending" else "",
                fontFamily = Press,
                fontSize = 5.sp,
                color = GoldLight
            )
            val stalled = state.mode == "chest" && state.stepBank <= 0.0
            Text(
                if (stalled) stringResource(R.string.scene_pending) else formatRemaining(chestRemainingMs(chest)),
                fontSize = 4.8.sp,
                color = if (stalled) Ember else Muted2
            )
            if (chest.progress > 0.0) {
                Spacer(Modifier.height(2.dp))
                MiniBar(
                    fraction = (chest.progress / chest.quota).toFloat(),
                    color = Gold,
                    modifier = Modifier.width(58.dp),
                    height = 3.dp
                )
            }
        }
    }
}

@Composable
private fun ChestButton(state: GameState, onChest: () -> Unit, modifier: Modifier = Modifier) {
    val chest = state.chest ?: return
    val chestDescription = stringResource(R.string.scene_chest_desc)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(58.dp)
                .clickable(onClick = onChest)
                .semantics { contentDescription = chestDescription }
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
    Column(modifier.fillMaxWidth()) {
        BuffStrip(state, Modifier.fillMaxWidth().padding(horizontal = 10.dp))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
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
                    Text(stringResource(R.string.scene_fervour), fontSize = 6.5.sp, color = Muted)
                    Text("${state.ferveur}", fontFamily = Press, fontSize = 6.5.sp, color = Ember)
                }
                MiniBar(state.ferveur / 100f, Ember, Modifier.fillMaxWidth(), height = 3.dp)
                Spacer(Modifier.height(3.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.scene_reserve), fontSize = 6.5.sp, color = Muted)
                    Text(
                        formatCompact(state.stepBank.toInt()),
                        fontFamily = Press,
                        fontSize = 6.5.sp,
                        color = GoldLight,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                MiniBar(
                    (state.stepBank / 4000.0).toFloat().coerceIn(0f, 1f),
                    Gold,
                    Modifier.fillMaxWidth(),
                    height = 3.dp
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(tierLabelRes(state.ferveur), fontSize = 5.5.sp, color = Ember)
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
                Text(
                    formatCompact(state.stepsToday),
                    fontFamily = Press,
                    fontSize = 7.sp,
                    color = Parchment,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Text(stringResource(R.string.scene_steps_today), fontSize = 5.5.sp, color = Muted)
            }
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
            .padding(horizontal = 26.dp)
            .padding(top = 122.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Ink.copy(alpha = 0.95f))
            .border(1.dp, Blood, RoundedCornerShape(4.dp))
            .padding(vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(
                    R.string.scene_boss_banner,
                    localizedCreature(fight.enemyName),
                    if (fight.modifier.isNotBlank()) " · ${modifierLabelRes(fight.modifier)}" else "",
                    fight.enemyLevel
                ),
                fontSize = 6.sp,
                color = Blood,
                maxLines = 1
            )
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

