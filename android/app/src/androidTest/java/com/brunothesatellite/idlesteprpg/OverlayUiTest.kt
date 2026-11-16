package com.brunothesatellite.idlesteprpg

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.brunothesatellite.idlesteprpg.engine.ChestData
import com.brunothesatellite.idlesteprpg.engine.GameState
import com.brunothesatellite.idlesteprpg.engine.Item
import com.brunothesatellite.idlesteprpg.engine.SpecialOutcome
import com.brunothesatellite.idlesteprpg.ui.ChestDialog
import com.brunothesatellite.idlesteprpg.ui.ItemDialog
import com.brunothesatellite.idlesteprpg.ui.MarcheTheme
import com.brunothesatellite.idlesteprpg.ui.OnboardingOverlay
import com.brunothesatellite.idlesteprpg.ui.SpecialResultDialog
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OverlayUiTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun chestDialogAlwaysOffersAWayToContinueWalking() {
        rule.setContent {
            MarcheTheme {
                ChestDialog(
                    chest = ChestData(
                        type = "fer",
                        quota = 400,
                        progress = 120.0,
                        goldMin = 130,
                        goldMax = 220
                    ),
                    state = GameState(mode = "chest"),
                    onForce = {},
                    onStop = {},
                    onDismiss = {}
                )
            }
        }
        rule.onNodeWithText("ARRÊTER LE FORÇAGE").assertExists()
        rule.onNodeWithText("CONTINUER LA MARCHE").assertExists()
    }

    @Test
    fun unidentifiedItemShowsIdentifyAction() {
        val item = Item(
            id = "mystery",
            name = "Objet mystérieux",
            slot = "weapon",
            rarity = "epic",
            icon = "item/mace2.png",
            level = 3,
            atk = 20,
            value = 100,
            identified = false
        )
        rule.setContent {
            MarcheTheme {
                ItemDialog(
                    item = item,
                    state = GameState().copy(inventory = listOf(item)),
                    onDismiss = {},
                    onEquip = {},
                    onSell = {},
                    onToggleLock = {},
                    onFuse = {},
                    onIdentify = {},
                    onReroll = {},
                    onUpgrade = {},
                    onDismantle = {}
                )
            }
        }
        rule.onNodeWithText("IDENTIFIER · 1 ÉCLAT").assertExists()
        rule.onNodeWithText("Objet mystérieux").assertExists()
    }

    @Test
    fun onboardingAdvancesThroughPages() {
        var finished = false
        rule.setContent {
            MarcheTheme {
                OnboardingOverlay { finished = true }
            }
        }
        rule.onNodeWithText("SUIVANT").performClick()
        rule.onNodeWithText("GARMIN PERSONAL API").assertExists()
        rule.onNodeWithText("PASSER").performClick()
    }

    @Test
    fun gambleResultIsVisible() {
        rule.setContent {
            MarcheTheme {
                SpecialResultDialog(
                    outcome = SpecialOutcome("Pari perdu · −100 or", false),
                    onDismiss = {}
                )
            }
        }
        rule.onNodeWithText("RÉSULTAT").assertExists()
        rule.onNodeWithText("Pari perdu · −100 or").assertExists()
        rule.onNodeWithText("CONTINUER").assertExists()
    }
}
