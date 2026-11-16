package com.brunothesatellite.idlesteprpg.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.BitmapFactory
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val fr = Locale.FRANCE

fun formatInt(value: Int): String = NumberFormat.getIntegerInstance(fr).format(value)

fun formatLong(value: Long): String = NumberFormat.getIntegerInstance(fr).format(value)

fun formatDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", fr))

fun formatTime(epochMillis: Long): String {
    if (epochMillis <= 0) return "jamais"
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm", fr))
}

fun formatTokenTime(instant: Instant?): String {
    if (instant == null) return "—"
    return instant.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm", fr))
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
            .height(if (subtitle == null) 44.dp else 50.dp)
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
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text,
                fontFamily = Press,
                fontSize = 7.5.sp,
                color = if (enabled) Ink else Muted,
                maxLines = 1
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
        Text(value, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Parchment, maxLines = 1)
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

fun Int.coerceAtLeastZero(): Int = if (this < 0) 0 else this
