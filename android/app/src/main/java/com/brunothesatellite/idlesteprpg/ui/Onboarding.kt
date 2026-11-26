package com.brunothesatellite.idlesteprpg.ui

import androidx.annotation.StringRes
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brunothesatellite.idlesteprpg.R

private data class OnboardingPage(
    @StringRes val title: Int,
    @StringRes val text: Int,
    @StringRes val note: Int,
    val icon: String
)

private val PAGES = listOf(
    OnboardingPage(
        title = R.string.onboarding_page1_title,
        text = R.string.onboarding_page1_text,
        note = R.string.onboarding_page1_note,
        icon = "hero/human.png"
    ),
    OnboardingPage(
        title = R.string.onboarding_page2_title,
        text = R.string.onboarding_page2_text,
        note = R.string.onboarding_page2_note,
        icon = "item/misc_crystal.png"
    ),
    OnboardingPage(
        title = R.string.onboarding_page3_title,
        text = R.string.onboarding_page3_text,
        note = R.string.onboarding_page3_note,
        icon = "fx/heart.png"
    ),
    OnboardingPage(
        title = R.string.onboarding_page4_title,
        text = R.string.onboarding_page4_text,
        note = R.string.onboarding_page4_note,
        icon = "fx/cloud_fire0.png"
    )
)

@Composable
fun OnboardingOverlay(onFinish: () -> Unit, onConnect: () -> Unit, onHealth: () -> Unit) {
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
                .verticalScroll(rememberScrollState())
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
                stringResource(current.title),
                fontFamily = Press,
                fontSize = 10.sp,
                color = GoldLight,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(current.text),
                fontSize = 8.sp,
                color = Parchment.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                lineHeight = 12.sp
            )
            Spacer(Modifier.height(9.dp))
            Text(
                stringResource(current.note),
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
            if (page == 1) {
                OverlayAction(
                    stringResource(R.string.onboarding_use_garmin),
                    Modifier.fillMaxWidth(),
                    onConnect,
                    primary = true
                )
                Spacer(Modifier.height(5.dp))
                OverlayAction(
                    stringResource(R.string.onboarding_use_health),
                    Modifier.fillMaxWidth(),
                    onHealth
                )
                Spacer(Modifier.height(5.dp))
            }
            if (page < PAGES.lastIndex) {
                OverlayAction(
                    stringResource(R.string.onboarding_next),
                    Modifier.fillMaxWidth(),
                    {
                        page++
                    },
                    primary = page != 1
                )
                Spacer(Modifier.height(5.dp))
                OverlayAction(
                    stringResource(R.string.onboarding_skip),
                    Modifier.fillMaxWidth(),
                    onFinish
                )
            } else {
                OverlayAction(
                    stringResource(R.string.onboarding_start),
                    Modifier.fillMaxWidth(),
                    onFinish,
                    primary = true
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.onboarding_step, page + 1, PAGES.size),
                fontSize = 6.sp,
                color = Muted2
            )
        }
    }
}
