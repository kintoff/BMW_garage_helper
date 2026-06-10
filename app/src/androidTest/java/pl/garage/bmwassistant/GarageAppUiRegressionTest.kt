package pl.garage.bmwassistant

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pl.garage.bmwassistant.model.Vehicle
import pl.garage.bmwassistant.ui.screens.AddVehicleWizard
import pl.garage.bmwassistant.ui.screens.GarageDashboard
import pl.garage.bmwassistant.ui.theme.GarageTheme

@RunWith(AndroidJUnit4::class)
class GarageAppUiRegressionTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<TestComposeActivity>()

    @Before
    fun setUp() {
        TestComposeContentRegistry.content = {
            GarageTheme {
                androidx.compose.material3.Text("Test host ready")
            }
        }
        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()
    }

    @Test
    fun garageDashboardInvokesCallbacksForAddAndOpenVehicle() {
        val vehicle = Vehicle(
            brand = "BMW",
            model = "E61 520d",
            generation = "E61",
            engine = "M47N2 2.0d",
            year = "2006",
            vin = "WBAUIREGRESSION01",
            mileage = "285000",
            note = "UI regression seed",
            id = "vehicle_ui_seed"
        )
        var addClicked = false
        var openedVehicle: Vehicle? = null

        TestComposeContentRegistry.content = {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                GarageTheme {
                    GarageDashboard(
                        vehicles = listOf(vehicle),
                        onAddVehicle = { addClicked = true },
                        onOpenVehicle = { openedVehicle = it },
                        onCopyVehicle = {},
                        onDeleteVehicle = {}
                    )
                }
            }
        }
        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("dashboard_add_vehicle_button").performClick()
        assertTrue(addClicked)

        composeRule.onNodeWithTag("vehicle_card_bmw_e61_520d_e61").performClick()
        assertEquals(vehicle, openedVehicle)
        composeRule.onNodeWithText("BMW E61 520d E61").assertIsDisplayed()
    }

    @Test
    fun addVehicleWizardCreatesVehicleFromTemplate() {
        var savedVehicle: Vehicle? = null

        TestComposeContentRegistry.content = {
            GarageTheme {
                AddVehicleWizard(
                    onVehicleCreated = { savedVehicle = it }
                )
            }
        }
        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Uzyj E61").performClick()
        composeRule
            .onNodeWithTag("add_vehicle_wizard_list")
            .performScrollToNode(hasTestTag("add_vehicle_save_button"))
        composeRule.onNodeWithTag("add_vehicle_save_button").performClick()

        composeRule.runOnIdle {}

        assertNotNull(savedVehicle)
        assertEquals("BMW", savedVehicle?.brand)
        assertEquals("E61 520d", savedVehicle?.model)
        assertEquals("M47N2 2.0d", savedVehicle?.engine)
        assertEquals("E61", savedVehicle?.generation)
    }
}
