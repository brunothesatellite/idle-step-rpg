package com.brunothesatellite.idlesteprpg.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.BitmapFactory
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.brunothesatellite.idlesteprpg.R
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val fr = Locale.FRANCE

/** Locale de l'application, mise à jour au démarrage et au changement de langue. */
object AppLocale {
    @Volatile
    var current: Locale = Locale.US

    /**
     * Français → conventions françaises ; anglais et toute autre langue →
     * conventions anglaises. Les ressources Android ont la même règle :
     * `values/` est anglais, `values-fr/` français.
     */
    fun updateFrom(locale: Locale) {
        current = if (locale.language == "fr") Locale.FRANCE else Locale.US
    }
}

fun formatInt(value: Int): String = NumberFormat.getIntegerInstance(AppLocale.current).format(value)

fun formatLong(value: Long): String = NumberFormat.getIntegerInstance(AppLocale.current).format(value)

private const val NNBSP = "\u202F"

/**
 * Nombre compact pour les interfaces denses : exact jusque 9 999, puis
 * « 12,3 k », « 123 k », « 1,2 M », « 1,2 Md ».
 */
fun formatCompact(value: Int): String = formatCompact(value.toLong())

fun formatCompact(value: Long): String = compactNumber(value, compactUnitSpace())

private fun compactUnitSpace(): String =
    if (AppLocale.current.language == "fr") NNBSP else ""

/**
 * Variante des nombres flottants de combat : pas de séparateur de milliers,
 * unité collée et pas de décimale, pour tenir dans une seule ligne au-dessus
 * des sprites même à la taille de texte maximale.
 */
fun formatFloatNumber(value: Int): String {
    val abs = kotlin.math.abs(value)
    val sign = if (value < 0) "-" else ""
    return when {
        abs < 10_000 -> value.toString()
        abs < 1_000_000 -> {
            val k = (abs / 1_000.0).roundToInt()
            if (k >= 1_000) sign + "1M" else sign + "${k}k"
        }
        abs < 1_000_000_000 -> sign + "${(abs / 1_000_000.0).roundToInt()}M"
        else -> sign + "${(abs / 1_000_000_000.0).roundToInt()}Md"
    }
}

/** Niveau très court pour les badges : 999, 1k, 12k, 1M, 1Md. */
fun formatLevelShort(value: Int): String = when {
    value < 1_000 -> value.toString()
    value < 1_000_000 -> "${value / 1_000}k"
    value < 1_000_000_000 -> "${value / 1_000_000}M"
    else -> "${value / 1_000_000_000}Md"
}

private fun compactNumber(value: Long, unitSpace: String): String {
    val abs = kotlin.math.abs(value)
    if (abs < 10_000L) {
        return formatInt(value.toInt())
    }
    val sign = if (value < 0) "-" else ""
    return when {
        abs < 100_000L -> sign + oneDecimal(abs / 1_000.0) + unitSpace + "k"
        abs < 1_000_000L -> sign + (abs / 1_000L) + unitSpace + "k"
        abs < 100_000_000L -> sign + oneDecimal(abs / 1_000_000.0) + unitSpace + "M"
        abs < 1_000_000_000L -> sign + (abs / 1_000_000L) + unitSpace + "M"
        abs < 100_000_000_000L -> sign + oneDecimal(abs / 1_000_000_000.0) + unitSpace + "Md"
        else -> sign + (abs / 1_000_000_000L) + unitSpace + "Md"
    }
}

private fun oneDecimal(value: Double): String =
    String.format(AppLocale.current, "%.1f", value)
        .removeSuffix(",0")
        .removeSuffix(".0")

fun formatDate(date: LocalDate): String {
    val pattern = if (AppLocale.current.language == "fr") "dd/MM/yyyy" else "MM/dd/yyyy"
    return date.format(DateTimeFormatter.ofPattern(pattern, AppLocale.current))
}

fun formatTime(epochMillis: Long): String {
    if (epochMillis <= 0) return "jamais"
    val pattern = if (AppLocale.current.language == "fr") "HH:mm" else "h:mm a"
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern(pattern, AppLocale.current))
}

@Composable
fun formatTimeLocalized(epochMillis: Long): String =
    if (epochMillis <= 0) stringResource(R.string.common_never) else formatTime(epochMillis)

fun formatTokenTime(instant: Instant?): String {
    if (instant == null) return "—"
    val pattern = if (AppLocale.current.language == "fr") "HH:mm" else "h:mm a"
    return instant.atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern(pattern, AppLocale.current))
}

fun Context.findActivity(): Activity? {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
fun rememberAsset(path: String): ImageBitmap? {
    val context = LocalContext.current
    return remember(path) {
        runCatching {
            context.assets.open(path).use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
        }.getOrNull()
    }
}

@Composable
fun AssetImage(
    path: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val bitmap = rememberAsset(path)
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.None
        )
    } else {
        Box(modifier.background(Panel2))
    }
}

@Composable
fun AssetIcon(path: String, size: Dp = 16.dp, modifier: Modifier = Modifier) {
    AssetImage(path, modifier.size(size))
}

@Composable
fun PixelCard(
    modifier: Modifier = Modifier,
    borderColor: Color = Line,
    background: Color = Panel,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Column(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .padding(8.dp),
        content = content
    )
}

@Composable
fun GoldButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    subtitle: String? = null,
    badge: String? = null,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(5.dp)
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = if (subtitle == null) 44.dp else 50.dp)
            .clip(shape)
            .background(
                if (enabled) {
                    Brush.verticalGradient(listOf(GoldLight, Gold, Color(0xFFB9863F)))
                } else {
                    Brush.verticalGradient(listOf(Color(0xFF30263C), Color(0xFF251D30)))
                }
            )
            .border(1.dp, if (enabled) GoldLight else Line2, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text,
                fontFamily = Press,
                fontSize = 7.5.sp,
                color = if (enabled) Ink else Muted,
                maxLines = 2
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    fontFamily = Pixel,
                    fontSize = 6.5.sp,
                    color = if (enabled) Ink.copy(alpha = 0.65f) else Muted2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (badge != null) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (enabled) Ink.copy(alpha = 0.82f) else Panel2)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    badge,
                    fontFamily = Pixel,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) GoldLight else Muted
                )
            }
        }
    }
}

@Composable
fun MiniBar(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 4.dp,
    track: Color = Panel3
) {
    Box(
        modifier
            .height(height)
            .clip(RoundedCornerShape(1.dp))
            .background(track)
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(height)
                .background(
                    Brush.horizontalGradient(
                        listOf(color.copy(alpha = 0.75f), color)
                    )
                )
        )
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = Muted) {
    Text(
        text.uppercase(fr),
        modifier = modifier,
        fontFamily = Pixel,
        fontSize = 7.sp,
        letterSpacing = 1.sp,
        color = color
    )
}

@Composable
fun CurrencyChip(icon: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(3.dp))
            .background(Panel2)
            .border(1.dp, Line, RoundedCornerShape(3.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AssetIcon(icon, 11.dp)
        Spacer(Modifier.width(3.dp))
        Text(
            value,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = Parchment,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun DividerLine(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Line.copy(alpha = 0.65f))
    )
}

/** Halo doré du héros au palier 100. */
@Composable
fun GoldenAura(modifier: Modifier = Modifier, animate: Boolean = true) {
    val transition = rememberInfiniteTransition(label = "aura")
    val pulse by transition.animateFloat(
        initialValue = if (animate) 0.94f else 1f,
        targetValue = if (animate) 1.06f else 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "auraPulse"
    )
    Canvas(modifier) {
        val maxR = size.minDimension * 0.5f * pulse
        val c = Offset(size.width / 2f, size.height * 0.44f)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0x9AFFD24A), Color(0x5AFFB020), Color.Transparent),
                center = c,
                radius = maxR
            ),
            radius = maxR,
            center = c
        )
        val spikes = 14
        for (i in 0 until spikes) {
            val a = (Math.PI * 2.0 / spikes * i - Math.PI / 2).toFloat()
            val inner = maxR * 0.42f
            val outer = maxR * (0.82f + 0.08f * (i % 3))
            drawLine(
                color = Color(0xCCFFE066),
                start = Offset(c.x + cos(a) * inner, c.y + sin(a) * inner),
                end = Offset(c.x + cos(a) * outer, c.y + sin(a) * outer),
                strokeWidth = maxR * 0.07f
            )
        }
        drawOval(
            color = Color(0x88FFC83C),
            topLeft = Offset(size.width / 2f - maxR * 0.8f, size.height * 0.84f),
            size = Size(maxR * 1.6f, maxR * 0.24f)
        )
        for (i in 0 until 10) {
            val px = c.x + cos(i * 2.1f) * maxR * (0.35f + 0.05f * (i % 4))
            val py = c.y + sin(i * 1.7f) * maxR * 0.55f
            drawRect(
                color = Color(0xEEFFE680),
                topLeft = Offset(px, py),
                size = Size(maxR * 0.05f, maxR * 0.05f)
            )
        }
    }
}

/**
 * Les fenêtres de dialogue vivent dans une fenêtre Android séparée : la densité
 * locale (donc le réglage « Taille du texte ») n'y est pas transmise. Ce
 * fournisseur la réapplique depuis [AppTypography].
 */
@Composable
fun WithAppFontScale(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val scale = (density.fontScale * AppTypography.fontScale).coerceAtMost(MAX_FONT_SCALE)
    CompositionLocalProvider(
        LocalDensity provides Density(density = density.density, fontScale = scale),
        content = content
    )
}

/** [Dialog] qui applique la taille de texte de l'application. */
@Composable
fun AppDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        WithAppFontScale { content() }
    }
}

/** Croix de fermeture commune à toutes les fenêtres, centrée quelle que soit la taille. */
@Composable
fun CloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 30.dp,
    background: Color = Panel2,
    bordered: Boolean = true
) {
    Box(
        modifier
            .sizeIn(minWidth = size, minHeight = size)
            .clip(RoundedCornerShape(4.dp))
            .background(background)
            .then(if (bordered) Modifier.border(1.dp, Line2, RoundedCornerShape(4.dp)) else Modifier)
            .clickable(onClick = onClick)
            .padding(3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "×",
            fontSize = 15.sp,
            lineHeight = 15.sp,
            color = Muted,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

fun Int.coerceAtLeastZero(): Int = if (this < 0) 0 else this

