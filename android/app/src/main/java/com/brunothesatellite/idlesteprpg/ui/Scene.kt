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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
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
import com.brunothesatellite.idlesteprpg.engine.FLOOR_METERS
import com.brunothesatellite.idlesteprpg.engine.GameEngine
import com.brunothesatellite.idlesteprpg.engine.GameState
import com.brunothesatellite.idlesteprpg.engine.STEPS_PER_METER
import com.brunothesatellite.idlesteprpg.engine.biomeFor
import com.brunothesatellite.idlesteprpg.engine.chestRemainingMs
import com.brunothesatellite.idlesteprpg.engine.formatRemaining
import com.brunothesatellite.idlesteprpg.engine.statsOf
import com.brunothesatellite.idlesteprpg.engine.tierIndex
import com.brunothesatellite.idlesteprpg.engine.tierName
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.roundToInt

@Composable
fun Scene(
    state: GameState,
    onCamp: () -> Unit,
    onChest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val walking = GameEngine.isWalking(state)
    val density = LocalDensity.current
    var previousLevel by remember { mutableStateOf(state.level) }
    var levelBanner by remember { mutableStateOf(0) }

    LaunchedEffect(state.level) {
        val previous = previousLevel
        previousLevel = state.level
        if (state.level > previous) {
            levelBanner = state.level
            delay(2200)
            levelBanner = 0
        }
    }

    BoxWithConstraints(modifier.fillMaxSize().background(Ink)) {
        val sceneWidth = maxWidth
        val sceneHeight = maxHeight

        SceneBackground(state, density.density)

        if (state.distance >= 85 || state.fight?.isBoss == true) {
            GateView(
                state,
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 0.dp, bottom = 58.dp)
                    .width(166.dp)
                    .height(124.dp)
            )
        }

        TopHud(state, onCamp, Modifier.align(Alignment.TopStart))
        ProgressHeader(state, Modifier.align(Alignment.TopStart).padding(top = 38.dp))

        HeroUnit(state, walking, Modifier.align(Alignment.BottomStart).padding(start = 18.dp, bottom = 58.dp))

        state.fight?.let { fight ->
            key(fight.enemyName, fight.enemyLevel, fight.isBoss, fight.farm) {
                EnemyUnit(
                    fight,
                    Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 58.dp)
                )
            }
        }

        if (state.chest != null && state.mode != "fight" && state.mode != "boss") {
            ChestButton(state, onChest, Modifier.align(Alignment.BottomCenter).padding(bottom = 62.dp).offset(x = 48.dp))
        }

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
private fun SceneBackground(state: GameState, density: Float) {
    val wall = rememberAsset("env/brick_gray1.png")
    val cobble = rememberAsset("env/cobble_blood1.png")
    val statueHero = rememberAsset("env/statue_ancient_hero.png")
    val statueDragon = rememberAsset("env/statue_dragon.png")
    val statueSword = rememberAsset("env/statue_sword.png")

    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val tile = 68.dp.toPx()
        val groundTop = h * 0.80f
        val archTop = h * 0.30f
        val archHeight = h * 0.46f

        drawRect(
            Brush.verticalGradient(
                listOf(Color(0xFF07050C), Color(0xFF151022), Color(0xFF09060F))
            )
        )

        if (wall != null) {
            var y = 0f
            while (y < groundTop) {
                var x = 0f
                while (x < w) {
                    drawImage(
                        image = wall,
                        dstOffset = IntOffset(x.roundToInt(), y.roundToInt()),
                        dstSize = IntSize(tile.roundToInt(), tile.roundToInt()),
                        alpha = 0.22f,
                        filterQuality = FilterQuality.None
                    )
                    x += tile
                }
                y += tile
            }
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
            val statue = when (statueIndex) {
                0 -> statueHero
                1 -> statueDragon
                else -> statueSword
            }
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
                    listOf(Color(0x55FF9448), Color(0x11FF9448), Color.Transparent),
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
            drawPath(flame, Brush.verticalGradient(listOf(GoldLight, Ember, Color(0xFF8F2D19))))
            drawRoundRect(
                color = Color(0xFF3A2414),
                topLeft = Offset(cx - 3.dp.toPx(), cy + 9.dp.toPx()),
                size = Size(6.dp.toPx(), 16.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx())
            )
        }

        // sol
        drawRect(
            Brush.verticalGradient(listOf(Color(0xFF17101D), Color(0xFF08050C))),
            topLeft = Offset(0f, groundTop),
            size = Size(w, h - groundTop)
        )
        if (cobble != null) {
            val gt = 54.dp.toPx()
            var y = groundTop
            while (y < h) {
                var x = 0f
                while (x < w) {
                    drawImage(
                        image = cobble,
                        dstOffset = IntOffset(x.roundToInt(), y.roundToInt()),
                        dstSize = IntSize(gt.roundToInt(), gt.roundToInt()),
                        alpha = 0.33f,
                        filterQuality = FilterQuality.None
                    )
                    x += gt
                }
                y += gt
            }
        }
        drawRect(
            Brush.verticalGradient(listOf(Color(0xFF4A3C56), Color(0xFF2A2035))),
            topLeft = Offset(0f, groundTop - 2.dp.toPx()),
            size = Size(w, 4.dp.toPx())
        )
        drawLine(
            color = Color(0xFF6A5678),
            start = Offset(0f, groundTop - 2.dp.toPx()),
            end = Offset(w, groundTop - 2.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )

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
    width: Int = 124
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
        Spacer(Modifier.height(2.dp))
        MiniBar(
            fraction = (hp / maxHp.toDouble()).toFloat(),
            color = if (enemy) Blood else Green,
            height = 3.dp
        )
    }
}

@Composable
private fun HeroUnit(state: GameState, walking: Boolean, modifier: Modifier = Modifier) {
    val stats = statsOf(state)
    val floats = remember { mutableStateListOf<FloatFx>() }
    var previousHp by remember { mutableStateOf(state.hp) }
    var nextId by remember { mutableLongStateOf(0L) }

    LaunchedEffect(state.hp) {
        val diff = state.hp - previousHp
        previousHp = state.hp
        if (diff < -3.0) {
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
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .width(68.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.5f))
            )
            AssetImage(
                "hero/human.png",
                Modifier
                    .size(96.dp)
                    .align(Alignment.BottomCenter)
                    .offset(y = if (walking) bob.dp else 0.dp)
            )
            AssetImage(
                "hero/buckler_round.png",
                Modifier
                    .size(28.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = 0.dp, y = (-22).dp)
                    .graphicsLayer(rotationZ = -8f)
            )
            AssetImage(
                "hero/broadsword.png",
                Modifier
                    .size(48.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = (-1).dp, y = (-21).dp)
                    .graphicsLayer(rotationZ = 10f)
            )
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
    modifier: Modifier = Modifier
) {
    val height = if (fight.isBoss) 148.dp else 108.dp
    val plateWidth = if (fight.isBoss) 154 else 126
    val spriteSize = if (fight.isBoss) 136.dp else 102.dp
    val floats = remember { mutableStateListOf<FloatFx>() }
    val flash = remember { Animatable(0f) }
    val slash = remember { Animatable(0f) }
    var previousHp by remember { mutableStateOf(fight.enemyHp) }
    var nextId by remember { mutableLongStateOf(0L) }

    LaunchedEffect(fight.enemyHp) {
        val damage = previousHp - fight.enemyHp
        previousHp = fight.enemyHp
        if (damage > 0.5) {
            floats += FloatFx(
                id = nextId++,
                text = "-${damage.roundToInt()}",
                color = Color(0xFFFFC6B8),
                drift = ((nextId % 5) - 2) * 7f
            )
            coroutineScope {
                launch {
                    flash.snapTo(0.55f)
                    flash.animateTo(0f, tween(220))
                }
                launch {
                    slash.snapTo(0f)
                    slash.animateTo(1f, tween(260, easing = LinearEasing))
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
            width = plateWidth
        )
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier.height(height).width(if (fight.isBoss) 140.dp else 112.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (fight.isBoss && fight.enemyHp < fight.enemyMaxHp * 0.5) {
                BossAura(Modifier.size(spriteSize).align(Alignment.BottomCenter))
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
                    .alpha(flash.value)
                    .background(Color(0xFFFFF1D0), RoundedCornerShape(8.dp))
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
    val left = rememberAsset("env/gate_closed_left.png")
    val middle = rememberAsset("env/gate_closed_middle.png")
    val right = rememberAsset("env/gate_closed_right.png")
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
                modifier = Modifier.padding(bottom = 5.dp)
            )
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

