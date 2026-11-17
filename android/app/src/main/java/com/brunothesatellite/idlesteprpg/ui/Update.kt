package com.brunothesatellite.idlesteprpg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.brunothesatellite.idlesteprpg.update.UpdateInfo
import kotlin.math.roundToInt

@Composable
fun UpdateDialog(
    info: UpdateInfo,
    currentVersion: String,
    downloading: Boolean,
    progress: Float,
    error: String?,
    onInstall: () -> Unit,
    onLater: () -> Unit,
    onRelease: () -> Unit
) {
    AppDialog(
        onDismissRequest = { if (!downloading) onLater() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxWidth().padding(22.dp), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF241930), Color(0xFF130E1C))))
                    .border(1.dp, Gold.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                    .padding(18.dp)
            ) {
                Text("MISE À JOUR DISPONIBLE", fontFamily = Press, fontSize = 9.sp, color = GoldLight)
                Spacer(Modifier.height(8.dp))
                RowLine("Nouvelle version", info.tag)
                RowLine("Version installée", "v$currentVersion")
                Spacer(Modifier.height(8.dp))
                Text(
                    "L'APK signé sera téléchargé depuis GitHub, puis Android vous demandera " +
                        "de confirmer l'installation. Vos données et la clé Garmin sont conservées.",
                    fontSize = 7.sp,
                    color = Muted,
                    lineHeight = 10.sp
                )
                if (downloading) {
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = Gold,
                        trackColor = Line2
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Téléchargement ${(progress.coerceIn(0f, 1f) * 100).roundToInt()} %",
                        fontSize = 7.sp,
                        color = GoldLight
                    )
                }
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error, fontSize = 7.sp, color = Blood, lineHeight = 10.sp)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Si Android ou Play Protect bloque l'installation, vous pouvez installer " +
                        "l'APK depuis la page GitHub de la release.",
                    fontSize = 6.5.sp,
                    color = Muted2,
                    lineHeight = 9.sp
                )
                Spacer(Modifier.height(12.dp))
                OverlayAction(
                    if (downloading) "TÉLÉCHARGEMENT…" else "INSTALLER LA MISE À JOUR",
                    Modifier.fillMaxWidth(),
                    onInstall,
                    primary = true,
                    enabled = !downloading
                )
                Spacer(Modifier.height(5.dp))
                OverlayAction("VOIR LA RELEASE SUR GITHUB", Modifier.fillMaxWidth(), onRelease)
                Spacer(Modifier.height(5.dp))
                OverlayAction(
                    "PLUS TARD",
                    Modifier.fillMaxWidth(),
                    onLater,
                    enabled = !downloading
                )
            }
        }
    }
}

@Composable
fun PostUpdateDialog(
    tag: String,
    onOpenRelease: () -> Unit,
    onDismiss: () -> Unit
) {
    AppDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxWidth().padding(22.dp), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF1D2A22), Color(0xFF130E1C))))
                    .border(1.dp, Green.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                    .padding(18.dp)
            ) {
                Text("MISE À JOUR INSTALLÉE", fontFamily = Press, fontSize = 9.sp, color = Green)
                Spacer(Modifier.height(8.dp))
                Text(
                    "La Marche des Âmes est maintenant en $tag. Vos données et la clé Garmin " +
                        "ont été conservées.",
                    fontSize = 7.5.sp,
                    color = Parchment,
                    lineHeight = 11.sp
                )
                Spacer(Modifier.height(12.dp))
                OverlayAction(
                    "VOIR LES CHANGEMENTS",
                    Modifier.fillMaxWidth(),
                    onOpenRelease,
                    primary = true
                )
                Spacer(Modifier.height(5.dp))
                OverlayAction("PLUS TARD", Modifier.fillMaxWidth(), onDismiss)
            }
        }
    }
}

@Composable
private fun RowLine(label: String, value: String) {
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 7.5.sp, color = Muted)
        Text(value, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Parchment)
    }
}
