package com.brunothesatellite.idlesteprpg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class OnboardingPage(
    val title: String,
    val text: String,
    val note: String,
    val icon: String
)

private val PAGES = listOf(
    OnboardingPage(
        title = "LA MARCHE DES ÂMES",
        text = "Vos pas Garmin deviennent la distance parcourue dans un donjon infini. " +
            "10 pas = 1 mètre. Les combats, gardiens et coffres avancent sans que vous ayez à jouer en continu.",
        note = "Le jeu se joue en marchant, pas en restant sur l'écran.",
        icon = "hero/human.png"
    ),
    OnboardingPage(
        title = "GARMIN PERSONAL API",
        text = "La connexion se fait avec une clé personnelle Garmin et un jeton de 24 h. " +
            "Le mot de passe et le 2FA restent dans le navigateur : l'application ne les voit jamais.",
        note = "Clé personnelle, 2FA chez Garmin, aucun Health Connect.",
        icon = "item/misc_crystal.png"
    ),
    OnboardingPage(
        title = "RÉGLAGES ET ALERTES",
        text = "Vous pouvez ajuster la taille du texte, couper les notifications, réduire les animations " +
            "et exporter votre sauvegarde depuis l'écran Réglages.",
        note = "Notifications désactivables à tout moment.",
        icon = "fx/heart.png"
    ),
    OnboardingPage(
        title = "LE FEU DE CAMP",
        text = "Le camp met la marche en pause, soigne le héros et permet de gérer l'équipement. " +
            "Les pas continuent d'être mis en réserve pendant la pause.",
        note = "Reprenez l'expédition quand vous êtes prêt.",
        icon = "fx/cloud_fire0.png"
    )
)

@Composable
fun OnboardingOverlay(onFinish: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val current = PAGES[page]
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0B0810), Color(0xFF1B1226), Color(0xFF0B0810))))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(22.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF251A31), Color(0xFF130E1C))))
                .border(1.dp, Gold.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(38.dp))
                    .background(Panel2)
                    .border(1.dp, Gold.copy(alpha = 0.45f), RoundedCornerShape(38.dp)),
                contentAlignment = Alignment.Center
            ) {
                AssetImage(current.icon, Modifier.size(52.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(
                current.title,
                fontFamily = Press,
                fontSize = 10.sp,
                color = GoldLight,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                current.text,
                fontSize = 8.sp,
                color = Parchment.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                lineHeight = 12.sp
            )
            Spacer(Modifier.height(9.dp))
            Text(
                current.note,
                fontSize = 6.8.sp,
                color = Ember,
                textAlign = TextAlign.Center,
                lineHeight = 10.sp
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                PAGES.indices.forEach { index ->
                    Box(
                        Modifier
                            .width(if (index == page) 20.dp else 7.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (index == page) Gold else Panel3)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            if (page < PAGES.lastIndex) {
                OverlayAction("SUIVANT", Modifier.fillMaxWidth(), {
                    page++
                }, primary = true)
                Spacer(Modifier.height(5.dp))
                OverlayAction("PASSER", Modifier.fillMaxWidth(), onFinish)
            } else {
                OverlayAction("COMMENCER LA MARCHE", Modifier.fillMaxWidth(), onFinish, primary = true)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Étape ${page + 1} / ${PAGES.size}",
                fontSize = 6.sp,
                color = Muted2
            )
        }
    }
}
