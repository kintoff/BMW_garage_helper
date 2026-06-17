package pl.garage.bmwassistant.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelRulesTest {

    @Test
    fun stableRepairIdBuildsNormalizedFallbackKey() {
        val id = stableRepairId(
            title = "Tylna zwrotnica lewa",
            area = VehicleArea.Suspension,
            vehicleName = "BMW E61 520d"
        )

        assertEquals(
            "repair_bmw_e61_520d_suspension_tylna_zwrotnica_lewa",
            id
        )
    }

    @Test
    fun stableRepairIdFallsBackToUnknownWhenInputsAreBlank() {
        assertEquals(
            "repair_engine",
            stableRepairId(title = "", area = VehicleArea.Engine, vehicleName = "")
        )
    }

    @Test
    fun normalizedRepairStatusLabelRecognizesCommonVariants() {
        assertEquals(REPAIR_STATUS_FINISHED, "Finished".normalizedRepairStatusLabel())
        assertEquals(REPAIR_STATUS_IN_PROGRESS, "active".normalizedRepairStatusLabel())
        assertEquals(REPAIR_STATUS_PLANNED, "Planowane".normalizedRepairStatusLabel())
        assertEquals(REPAIR_STATUS_PLANNED, "   ".normalizedRepairStatusLabel())
    }

    @Test
    fun isFinishedRepairStatusRecognizesPolishAndEnglishLabels() {
        assertTrue("Zrobione".isFinishedRepairStatus())
        assertTrue("complete".isFinishedRepairStatus())
        assertFalse("W trakcie".isFinishedRepairStatus())
    }

    @Test
    fun vehicleDisplayNameAndTechnicalSummarySkipBlankFields() {
        val vehicle = Vehicle(
            brand = "BMW",
            model = "E61 520d",
            generation = "E61",
            engine = "M47N2 2.0d",
            year = "2006",
            vin = "WBATEST001",
            mileage = "285000",
            note = "warsztat"
        )

        assertEquals("BMW E61 520d E61", vehicle.displayName)
        assertEquals("M47N2 2.0d / Rok 2006 / 285000 km", vehicle.technicalSummary)
    }

    @Test
    fun vehicleTechnicalSummaryOmitsBlankSegments() {
        val vehicle = Vehicle(
            brand = "BMW",
            model = "E39",
            generation = "",
            engine = "",
            year = "",
            vin = "",
            mileage = "123456",
            note = ""
        )

        assertEquals("BMW E39", vehicle.displayName)
        assertEquals("123456 km", vehicle.technicalSummary)
    }

    @Test
    fun repairDocumentationDefaultsRepairIdFromRepairTitleAndArea() {
        val documentation = RepairDocumentation(
            title = "Dokumentacja: Tylna zwrotnica lewa",
            area = VehicleArea.Suspension,
            repairTitle = "Tylna zwrotnica lewa",
            summary = "opis"
        )

        assertEquals(
            "repair_suspension_tylna_zwrotnica_lewa",
            documentation.repairId
        )
    }

    @Test
    fun repairProjectDefaultCheckpointsAreDerivedFromChecklist() {
        val repair = RepairProject(
            title = "Wymiana filtra oleju",
            area = VehicleArea.Service,
            vehicleName = "BMW E61 520d",
            status = REPAIR_STATUS_PLANNED,
            priority = "Normalny",
            problemDescription = "Przeglad",
            goal = "Wymiana",
            checklist = listOf("Odkrec obudowe", "Wloz nowy filtr"),
            partsToIdentify = emptyList(),
            documentsToCollect = emptyList()
        )

        assertEquals(2, repair.checkpoints.size)
        assertEquals("Odkrec obudowe", repair.checkpoints.first().text)
        assertTrue(repair.checkpoints.first().id.startsWith("checkpoint_1_"))
        assertFalse(repair.checkpoints.first().isDone)
    }

    @Test
    fun vehicleAreaLabelsStayReadable() {
        assertEquals("Silnik", VehicleArea.Engine.label)
        assertEquals("Zawieszenie", VehicleArea.Suspension.label)
        assertEquals("Serwis standardowy", VehicleArea.Service.label)
    }
}
