package pl.garage.bmwassistant.ui.components

import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pl.garage.bmwassistant.TestComposeActivity
import pl.garage.bmwassistant.ui.theme.GarageTheme

@RunWith(AndroidJUnit4::class)
class UiComponentsComposeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<TestComposeActivity>()

    @Test
    fun infoCardAndHeaderDisplayTexts() {
        setTestContent {
            GarageTheme {
                androidx.compose.foundation.layout.Column {
                    Header(
                        title = "BMW Garage Assistant",
                        subtitle = "Prywatny system pracy w garażu"
                    )
                    InfoCard(
                        title = "Synchronizacja",
                        body = "Dane sa zapisane lokalnie na urzadzeniu."
                    )
                }
            }
        }

        composeRule.onNodeWithText("BMW Garage Assistant").assertIsDisplayed()
        composeRule.onNodeWithText("Prywatny system pracy w garażu").assertIsDisplayed()
        composeRule.onNodeWithText("Synchronizacja").assertIsDisplayed()
        composeRule.onNodeWithText("Dane sa zapisane lokalnie na urzadzeniu.").assertIsDisplayed()
    }

    @Test
    fun sectionTitleAndStatusPillRenderExpectedLabels() {
        setTestContent {
            GarageTheme {
                androidx.compose.foundation.layout.Column {
                    SectionTitle("Dokumentacja")
                    StatusPill("W trakcie")
                }
            }
        }

        composeRule.onNodeWithText("Dokumentacja").assertIsDisplayed()
        composeRule.onNodeWithText("W trakcie").assertIsDisplayed()
    }

    @Test
    fun garageTextFieldAcceptsUserInputAndShowsLabel() {
        var value = ""

        setTestContent {
            GarageTheme {
                GarageTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = "VIN",
                    placeholder = "Wpisz VIN",
                    singleLine = true
                )
            }
        }

        composeRule.onNodeWithText("VIN").assertIsDisplayed()
        composeRule.onNodeWithText("VIN").performTextInput("WBATEST001")
        composeRule.runOnIdle {
            assertEquals("WBATEST001", value)
        }
    }

    @Test
    fun checklistRowAndSimpleListRowDisplayStatus() {
        setTestContent {
            GarageTheme {
                androidx.compose.foundation.layout.Column {
                    ChecklistRow("Podnies auto")
                    SimpleListRow(
                        text = "Sprawdz tuleje",
                        status = "Do zamowienia"
                    )
                }
            }
        }

        composeRule.onNodeWithText("Podnies auto").assertIsDisplayed()
        composeRule.onNodeWithText("Do zrobienia").assertIsDisplayed()
        composeRule.onNodeWithText("Sprawdz tuleje").assertIsDisplayed()
        composeRule.onNodeWithText("Do zamowienia").assertIsDisplayed()
    }

    @Test
    fun segmentTabsAndBottomNavBarInvokeCallbacks() {
        var selectedTab = "Przeglad"
        var selectedBottom = "Przeglad"

        setTestContent {
            GarageTheme {
                androidx.compose.foundation.layout.Column {
                    SegmentTabs(
                        tabs = listOf("Przeglad", "Naprawy", "Czesci"),
                        selectedTab = selectedTab,
                        onSelect = { selectedTab = it }
                    )
                    BottomNavBar(
                        items = listOf("Przeglad", "Naprawy", "Czesci"),
                        selectedItem = selectedBottom,
                        onSelect = { selectedBottom = it }
                    )
                }
            }
        }

        composeRule.onAllNodesWithText("Naprawy")[0].performClick()
        composeRule.onNodeWithTag("bottom_nav_Czesci").performClick()

        composeRule.runOnIdle {
            assertEquals("Naprawy", selectedTab)
            assertEquals("Czesci", selectedBottom)
        }

        composeRule.onAllNodesWithText("Naprawy")[0].performClick()
        composeRule.onNodeWithTag("bottom_nav_Przeglad").performClick()

        composeRule.runOnIdle {
            assertEquals("Naprawy", selectedTab)
            assertEquals("Przeglad", selectedBottom)
        }
    }

    @Test
    fun repairProgressAndStatusBadgeShowDerivedValues() {
        setTestContent {
            GarageTheme {
                androidx.compose.foundation.layout.Column {
                    StatusBadge("Planowane")
                    RepairProgress(
                        completed = 2,
                        total = 5
                    )
                }
            }
        }

        composeRule.onNodeWithText("Planowane").assertIsDisplayed()
        composeRule.onNodeWithText("2/5 krokow").assertIsDisplayed()
        composeRule.onNodeWithText("40%").assertIsDisplayed()
    }

    @Test
    fun garagePanelWrapsClickableContent() {
        var clicked = false

        setTestContent {
            GarageTheme {
                GaragePanel(
                    onClick = { clicked = true }
                ) {
                    Text("Panel serwisowy")
                }
            }
        }

        composeRule.onNodeWithText("Panel serwisowy").performClick()
        composeRule.runOnIdle {
            assertTrue(clicked)
        }
    }

    private fun setTestContent(content: @Composable () -> Unit) {
        composeRule.runOnUiThread {
            composeRule.activity.setContent {
                content()
            }
        }
        composeRule.waitForIdle()
    }
}
