package com.brunothesatellite.idlesteprpg.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brunothesatellite.idlesteprpg.R

val Ink = Color(0xFF0B0810)
val Ink2 = Color(0xFF15101F)
val Panel = Color(0xFF171121)
val Panel2 = Color(0xFF21182D)
val Panel3 = Color(0xFF2A1F3A)
val Line = Color(0xFF33263F)
val Line2 = Color(0xFF443153)
val Gold = Color(0xFFE8C27A)
val GoldLight = Color(0xFFF6DDA6)
val GoldDeep = Color(0xFFA9793C)
val Parchment = Color(0xFFF0E2C4)
val Muted = Color(0xFF9587A8)
val Muted2 = Color(0xFF6E617F)
val Ember = Color(0xFFFF9448)
val Blood = Color(0xFFD15458)
val Green = Color(0xFF8ED39B)
val Blue = Color(0xFF6FA8FF)
val Epic = Color(0xFFB57BFF)
val Legendary = Color(0xFFFFC857)

val Pixel = FontFamily(Font(R.font.pixelify_sans))
val Press = FontFamily(Font(R.font.press_start_2p))

private val colors = darkColorScheme(
    primary = Gold,
    onPrimary = Ink,
    secondary = Ember,
    onSecondary = Ink,
    background = Ink,
    onBackground = Parchment,
    surface = Panel,
    onSurface = Parchment,
    surfaceVariant = Panel2,
    onSurfaceVariant = Muted,
    outline = Line2,
    error = Blood
)

private val typography = Typography(
    bodyLarge = TextStyle(fontFamily = Pixel, fontSize = 14.sp, color = Parchment),
    bodyMedium = TextStyle(fontFamily = Pixel, fontSize = 12.sp, color = Parchment),
    bodySmall = TextStyle(fontFamily = Pixel, fontSize = 10.sp, color = Muted),
    labelLarge = TextStyle(fontFamily = Pixel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Parchment),
    labelMedium = TextStyle(fontFamily = Pixel, fontSize = 10.sp, color = Muted),
    labelSmall = TextStyle(fontFamily = Pixel, fontSize = 8.sp, color = Muted),
    titleLarge = TextStyle(fontFamily = Press, fontSize = 12.sp, color = GoldLight),
    titleMedium = TextStyle(fontFamily = Pixel, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Parchment),
    titleSmall = TextStyle(fontFamily = Pixel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Parchment)
)

@Composable
fun MarcheTheme(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = density.density,
            fontScale = density.fontScale * 1.35f
        )
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = typography,
            shapes = Shapes(
                small = RoundedCornerShape(4.dp),
                medium = RoundedCornerShape(6.dp),
                large = RoundedCornerShape(10.dp)
            ),
            content = content
        )
    }
}

fun rarityColor(rarity: String): Color = when (rarity) {
    "uncommon" -> Green
    "rare" -> Blue
    "epic" -> Epic
    "legendary" -> Legendary
    else -> Muted
}
