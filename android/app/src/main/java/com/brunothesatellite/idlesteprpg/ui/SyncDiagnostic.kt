package com.brunothesatellite.idlesteprpg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.brunothesatellite.idlesteprpg.GameViewModel
import com.brunothesatellite.idlesteprpg.UiState
import com.brunothesatellite.idlesteprpg.engine.GameState
import com.brunothesatellite.idlesteprpg.engine.HISTORY_DAYS
import com.brunothesatellite.idlesteprpg.engine.HISTORY_IMPORT_CAP
import com.brunothesatellite.idlesteprpg.engine.MAX_STEPS_PER_DAY
import com.brunothesatellite.idlesteprpg.engine.MAX_STEPS_PER_MINUTE
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val frDay = Locale.FRANCE
private val dayFormat = DateTimeFormatter.ofPattern("EEE dd/MM", frDay)

private data class DayInfo(
    val date: LocalDate,
    val steps: Int,
    val label: String,
    val status: String,
    val color: Color,
    val dimmed: Boolean
)

/**
 * Fenêtre de diagnostic de la synchronisation : montre, jour par jour, ce que le
 * jeu a réellement crédité dans la fenêtre d'import, et rappelle les garde-fous
 * appliqués par [com.brunothesatellite.idlesteprpg.engine.GameEngine.ingestHistory].
 *
 * Lecture seule : aucun pas n'est ajouté ni retiré ici.
 */
@Composable
fun SyncDiagnosticDialog(
    vm: GameViewModel,
    ui: UiState,
    state: GameState,
    onClose: () -> Unit
) {
    val today = LocalDate.now()
    val start = state.historyStart.takeIf { it.isNotBlank() }
    val sourceLabel = if (ui.stepSource == "health") "Santé Connect" else "Garmin"
    val rows = remember(today, start, state.stepLedger, state.stepsToday) {
        buildDayInfos(today, start, state.stepLedger, state.stepsToday)
    }
    val startLabel = start
        ?.let { runCatching { formatDate(LocalDate.parse(it)) }.getOrNull() }
        ?: "pas encore synchronisé"

    AppDialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier.fillMaxWidth().padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 640.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF241930), Color(0xFF130E1C))))
                    .border(1.dp, Gold.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "DIAGNOSTIC",
                            fontFamily = Press,
                            fontSize = 9.sp,
                            color = GoldLight
                        )
                        Text(
                            "Synchronisation des pas · lecture seule",
                            fontSize = 6.8.sp,
                            color = Muted
                        )
                    }
                    CloseButton(onClose)
                }
                Spacer(Modifier.height(8.dp))
                DividerLine()
                Spacer(Modifier.height(9.dp))

                // ---------- source active ----------
                SectionLabel("Source active")
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(7.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (ui.connected) Green else Blood)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        sourceLabel,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Parchment
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    ui.stepStatus.ifBlank { "état inconnu" },
                    fontSize = 7.sp,
                    color = if (ui.connected) Green else Ember,
                    lineHeight = 10.sp
                )
                Spacer(Modifier.height(6.dp))
                DiagnosticLine("Première connexion", startLabel)
                DiagnosticLine(
                    "Dernière synchro",
                    formatTime(ui.lastSync) +
                        if (ui.lastAdded > 0) " · +${formatCompact(ui.lastAdded)} pas" else ""
                )
                DiagnosticLine("Pas aujourd'hui", "${formatCompact(state.stepsToday)} pas")
                if (ui.lastError.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(ui.lastError, fontSize = 6.8.sp, color = Blood, lineHeight = 10.sp)
                }
                Spacer(Modifier.height(7.dp))
                OverlayAction(
                    if (ui.syncing) "SYNCHRONISATION…" else "SYNCHRONISER MAINTENANT",
                    Modifier.fillMaxWidth(),
                    onClick = { vm.syncNow() },
                    primary = true,
                    enabled = ui.connected && !ui.syncing
                )

                Spacer(Modifier.height(10.dp))

                // ---------- fenêtre d'import ----------
                SectionLabel("Fenêtre d'import · $HISTORY_DAYS derniers jours")
                Spacer(Modifier.height(5.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    rows.forEach { row -> DayRow(row) }
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    "Seuls les jours situés après la première connexion sont créditables. " +
                        "Un jour déjà compté n'est jamais recrédité.",
                    fontSize = 6.3.sp,
                    color = Muted2,
                    lineHeight = 9.sp
                )

                Spacer(Modifier.height(10.dp))

                // ---------- garde-fous ----------
                SectionLabel("Garde-fous")
                Spacer(Modifier.height(5.dp))
                RuleLine("Fenêtre", "$HISTORY_DAYS jours", "au-delà, la journée est ignorée")
                RuleLine(
                    "Plafond par jour",
                    "${formatCompact(MAX_STEPS_PER_DAY)} pas",
                    "une journée plus haute est ramenée au plafond"
                )
                RuleLine(
                    "Import d'un coup",
                    "${formatCompact(HISTORY_IMPORT_CAP)} pas",
                    "maximum crédité par synchronisation"
                )
                RuleLine(
                    "Rythme accepté",
                    "$MAX_STEPS_PER_MINUTE pas/min",
                    "les pas du jour au-delà sont ignorés"
                )

                if (ui.syncLog.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    SectionLabel("Dernières synchros")
                    Spacer(Modifier.height(4.dp))
                    ui.syncLog.take(3).forEach { line ->
                        Text(
                            line,
                            fontSize = 6.4.sp,
                            color = if (line.contains("ERREUR")) Blood else Muted,
                            modifier = Modifier.padding(vertical = 1.5.dp)
                        )
                    }
                }

                Spacer(Modifier.height(11.dp))
                OverlayAction("FERMER", Modifier.fillMaxWidth(), onClick = onClose)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Le journal complet (20 dernières lignes) reste disponible dans " +
                        "Réglages → Journal de synchronisation.",
                    fontSize = 6.1.sp,
                    color = Muted2,
                    lineHeight = 8.5.sp
                )
            }
        }
    }
}

@Composable
private fun DayRow(info: DayInfo) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(if (info.dimmed) Panel.copy(alpha = 0.4f) else Panel2)
            .border(1.dp, if (info.dimmed) Line.copy(alpha = 0.5f) else Line, RoundedCornerShape(4.dp))
            .padding(horizontal = 7.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            info.label,
            modifier = Modifier.weight(1f),
            fontSize = 7.2.sp,
            fontWeight = if (info.date == LocalDate.now()) FontWeight.Bold else FontWeight.Normal,
            color = if (info.dimmed) Muted2 else Parchment,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            if (info.steps > 0) "${formatCompact(info.steps)} pas" else "—",
            fontSize = 7.sp,
            color = if (info.steps > 0) Gold else Muted2,
            maxLines = 1
        )
        Spacer(Modifier.width(7.dp))
        Text(
            info.status,
            modifier = Modifier.width(86.dp),
            textAlign = TextAlign.End,
            fontSize = 5.6.sp,
            fontWeight = FontWeight.Bold,
            color = info.color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DiagnosticLine(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 1.5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 6.8.sp, color = Muted)
        Text(value, fontSize = 6.8.sp, color = Parchment, textAlign = TextAlign.End)
    }
}

@Composable
private fun RuleLine(label: String, value: String, detail: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Parchment)
            Text(detail, fontSize = 6.1.sp, color = Muted2, lineHeight = 8.5.sp)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            value,
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            color = Gold,
            textAlign = TextAlign.End,
            maxLines = 1
        )
    }
}

private fun buildDayInfos(
    today: LocalDate,
    start: String?,
    ledger: Map<String, Int>,
    stepsToday: Int
): List<DayInfo> {
    val rows = ArrayList<DayInfo>(HISTORY_DAYS)
    for (offset in (HISTORY_DAYS - 1) downTo 0) {
        val date = today.minusDays(offset.toLong())
        val key = date.toString()
        val credited = ledger[key] ?: 0
        val isToday = date == today
        val steps = if (isToday) maxOf(credited, stepsToday) else credited
        val label = if (isToday) {
            "Aujourd'hui"
        } else {
            date.format(dayFormat).replaceFirstChar { it.uppercase(frDay) }
        }
        val info = when {
            start != null && key < start -> DayInfo(
                date, 0, label, "AVANT CONNEXION", Muted2, true
            )
            isToday -> if (credited > 0) {
                DayInfo(date, steps, label, "CRÉDITÉ", Green, false)
            } else {
                DayInfo(date, steps, label, "AUJOURD'HUI", Ember, false)
            }
            credited > 0 -> DayInfo(date, credited, label, "CRÉDITÉ", Green, false)
            else -> DayInfo(date, 0, label, "AUCUN PAS", Muted2, true)
        }
        rows.add(info)
    }
    return rows
}
