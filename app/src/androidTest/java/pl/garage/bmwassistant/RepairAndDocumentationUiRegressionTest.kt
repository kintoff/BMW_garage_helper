package pl.garage.bmwassistant

import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pl.garage.bmwassistant.model.RepairCheckpoint
import pl.garage.bmwassistant.model.RepairDocumentation
import pl.garage.bmwassistant.model.RepairProject
import pl.garage.bmwassistant.model.Vehicle
import pl.garage.bmwassistant.model.VehicleArea
import pl.garage.bmwassistant.ui.screens.VehicleDocumentationScreen
import pl.garage.bmwassistant.ui.screens.VehicleRepairListScreen
import pl.garage.bmwassistant.ui.theme.GarageTheme

@RunWith(AndroidJUnit4::class)
class RepairAndDocumentationUiRegressionTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<TestComposeActivity>()

    @Test
    fun repairDetailsAddsCheckpointAndPropagatesUpdatedRepair() {
        val vehicle = vehicle()
        val repair = repair(
            checkpoints = listOf(RepairCheckpoint("checkpoint_seed", "Stary checkpoint"))
        )
        var updatedRepair: RepairProject? = null

        setTestContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                GarageTheme {
                    VehicleRepairListScreen(
                        vehicle = vehicle,
                        repairs = listOf(repair),
                        repairDocumentation = listOf(documentation(repair)),
                        inventoryParts = emptyList(),
                        shoppingList = emptyList(),
                        initialRepairId = repair.id,
                        onRepairAdded = { _, _ -> },
                        onRepairUpdated = { updatedRepair = it },
                        onOpenDocumentation = {},
                        onDocumentationUpdated = {},
                        onOpenShoppingList = {},
                        onAddShoppingItems = {},
                        onShoppingListUpdated = {},
                        onInventoryPartAdded = {},
                        onBack = {}
                    )
                }
            }
        }

        composeRule.onNodeWithTag("repair_checkpoint_input").performTextInput("Nowy checkpoint")
        composeRule.onNodeWithTag("repair_add_checkpoint_button").performClick()
        composeRule.runOnIdle {
            assertNotNull(updatedRepair)
            assertEquals("Nowy checkpoint", updatedRepair!!.checkpoints.last().text)
        }
    }

    @Test
    fun repairDetailsSavesNotesToDocumentation() {
        val vehicle = vehicle()
        val repair = repair()
        var updatedDocumentation: RepairDocumentation? = null

        setTestContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                GarageTheme {
                    VehicleRepairListScreen(
                        vehicle = vehicle,
                        repairs = listOf(repair),
                        repairDocumentation = listOf(documentation(repair)),
                        inventoryParts = emptyList(),
                        shoppingList = emptyList(),
                        initialRepairId = repair.id,
                        onRepairAdded = { _, _ -> },
                        onRepairUpdated = {},
                        onOpenDocumentation = {},
                        onDocumentationUpdated = { updatedDocumentation = it },
                        onOpenShoppingList = {},
                        onAddShoppingItems = {},
                        onShoppingListUpdated = {},
                        onInventoryPartAdded = {},
                        onBack = {}
                    )
                }
            }
        }

        composeRule.onNodeWithText("Notatki").performClick()
        composeRule.onNodeWithTag("repair_notes_input").performTextReplacement("Nowe uwagi z testu")
        composeRule.onNodeWithTag("repair_notes_save_button").performClick()

        composeRule.runOnIdle {
            assertNotNull(updatedDocumentation)
            assertEquals("Nowe uwagi z testu", updatedDocumentation!!.userNotes)
        }
    }

    @Test
    fun documentationDetailsAddsTisLink() {
        val vehicle = vehicle()
        val repair = repair()
        var updatedDocumentation: RepairDocumentation? = null

        setTestContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                GarageTheme {
                    VehicleDocumentationScreen(
                        vehicle = vehicle,
                        repairDocumentation = listOf(documentation(repair)),
                        repairProjects = listOf(repair),
                        shoppingList = emptyList(),
                        initialRepairTitle = repair.title,
                        onDocumentationUpdated = { updatedDocumentation = it },
                        onBack = {}
                    )
                }
            }
        }

        composeRule.onNodeWithText("Dokumenty").performClick()
        composeRule.onNodeWithTag("documentation_add_tis_button").performClick()
        composeRule.onNodeWithTag("documentation_tis_title_input").performTextInput("Procedura TIS")
        composeRule.onNodeWithTag("documentation_tis_link_input").performTextInput("newtis.info/test")
        composeRule.onNodeWithTag("documentation_tis_save_button").performClick()

        composeRule.runOnIdle {
            assertNotNull(updatedDocumentation)
            assertEquals("Procedura TIS", updatedDocumentation!!.tisDocuments.single().title)
            assertEquals("https://newtis.info/test", updatedDocumentation!!.tisDocuments.single().url)
        }
    }

    @Test
    fun documentationDetailsAddsYoutubeVideo() {
        val vehicle = vehicle()
        val repair = repair()
        var updatedDocumentation: RepairDocumentation? = null

        setTestContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                GarageTheme {
                    VehicleDocumentationScreen(
                        vehicle = vehicle,
                        repairDocumentation = listOf(documentation(repair)),
                        repairProjects = listOf(repair),
                        shoppingList = emptyList(),
                        initialRepairTitle = repair.title,
                        onDocumentationUpdated = { updatedDocumentation = it },
                        onBack = {}
                    )
                }
            }
        }

        composeRule.onNodeWithText("Dokumenty").performClick()
        composeRule.onNodeWithTag("documentation_add_youtube_button").performClick()
        composeRule.onNodeWithTag("documentation_youtube_title_input").performTextInput("Film testowy")
        composeRule.onNodeWithTag("documentation_youtube_link_input")
            .performTextInput("https://www.youtube.com/watch?v=abc123xyz89")
        composeRule.onNodeWithTag("documentation_youtube_note_input").performTextInput("Wazny fragment")
        composeRule.onNodeWithTag("documentation_youtube_save_button").performClick()

        composeRule.runOnIdle {
            assertNotNull(updatedDocumentation)
            assertEquals("Film testowy", updatedDocumentation!!.youtubeVideos.single().title)
            assertEquals("Wazny fragment", updatedDocumentation!!.youtubeVideos.single().note)
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

    private fun vehicle() = Vehicle(
        brand = "BMW",
        model = "E61 520d",
        generation = "E61",
        engine = "M47N2 2.0d",
        year = "2006",
        vin = "WBAUIREGRESSIONDOC01",
        mileage = "285000",
        note = "UI regression seed",
        id = "vehicle_ui_repair_doc"
    )

    private fun repair(
        checkpoints: List<RepairCheckpoint> = listOf(RepairCheckpoint("checkpoint_seed", "Sprawdz luzy"))
    ) = RepairProject(
        title = "Tylna zwrotnica lewa",
        area = VehicleArea.Suspension,
        vehicleName = "BMW E61 520d",
        status = "W trakcie",
        priority = "Wysoki",
        problemDescription = "Stuki z tylu",
        goal = "Usunac luzy",
        checklist = checkpoints.map { it.text },
        partsToIdentify = emptyList(),
        documentsToCollect = emptyList(),
        checkpoints = checkpoints,
        id = "repair_rear_knuckle"
    )

    private fun documentation(repair: RepairProject) = RepairDocumentation(
        title = "Dokumentacja: ${repair.title}",
        area = repair.area,
        repairTitle = repair.title,
        summary = "Dokumentacja powiazana z naprawa: ${repair.title}.",
        repairId = repair.id
    )
}
