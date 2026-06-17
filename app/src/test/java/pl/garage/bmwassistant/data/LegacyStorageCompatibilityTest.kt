package pl.garage.bmwassistant.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.garage.bmwassistant.model.PartInventoryItem
import pl.garage.bmwassistant.model.ShoppingListItem
import pl.garage.bmwassistant.model.Vehicle
import pl.garage.bmwassistant.model.VehicleArea

class LegacyStorageCompatibilityTest {

    @Test
    fun vehicleJsonUsesStableFallbackIdWhenMissing() {
        val restored = JSONObject()
            .put("brand", "BMW")
            .put("model", "E61 520d")
            .put("generation", "E61")
            .put("engine", "M47N2 2.0d")
            .put("year", "2006")
            .put("vin", "")
            .put("mileage", "285000")
            .put("note", "warsztat")
            .toVehicle()

        assertEquals("BMW E61 520d E61", restored.displayName)
        assertEquals("BMW E61 520d E61", restored.id)
    }

    @Test
    fun vehicleJsonPreservesExplicitIdAndCatalogUrl() {
        val vehicle = Vehicle(
            brand = "BMW",
            model = "E61 520d",
            generation = "E61",
            engine = "M47N2 2.0d",
            year = "2006",
            vin = "WBATEST001",
            mileage = "285000",
            note = "warsztat",
            id = "vehicle_123",
            partsCatalogUrl = "https://czescidobmw.pl/test"
        )

        val restored = vehicle.toJson().toVehicle()

        assertEquals("vehicle_123", restored.id)
        assertEquals("https://czescidobmw.pl/test", restored.partsCatalogUrl)
    }

    @Test
    fun inventoryJsonInfersRepairIdFromRepairTitleWhenMissing() {
        val vehicle = sampleVehicle()
        val restored = JSONObject()
            .put("id", "part_1")
            .put("oemPartNumber", "11428575211")
            .put("manufacturerPartNumber", "MANN-HU816X")
            .put("name", "Filtr oleju")
            .put("manufacturer", "Mann")
            .put("repairTitle", "Wymiana filtra oleju")
            .put("quantity", 1)
            .put("purchasePrice", "42.50")
            .toPartInventoryItem(vehicle)

        assertTrue(restored.repairId?.startsWith("repair_") == true)
    }

    @Test
    fun shoppingJsonFallsBackToServiceAreaAndStableRepairId() {
        val vehicle = sampleVehicle()
        val restored = JSONObject()
            .put("id", "shopping_1")
            .put("partNumber", "11428575211")
            .put("manufacturerPartNumber", "MANN-HU816X")
            .put("name", "Filtr oleju")
            .put("manufacturer", "Mann")
            .put("repairTitle", "Wymiana filtra oleju")
            .put("repairId", "")
            .put("area", "UNKNOWN")
            .put("quantity", 1)
            .put("source", "Autodoc")
            .toShoppingListItem(vehicle)

        assertEquals(VehicleArea.Service, restored.area)
        assertTrue(restored.repairId.startsWith("repair_"))
    }

    @Test
    fun partAndShoppingStorageKeysUseStablePrefixes() {
        val vehicle = sampleVehicle().copy(id = "")

        assertEquals("parts_WBATEST001", vehicle.partsStorageKey())
        assertEquals("shopping_WBATEST001", vehicle.shoppingStorageKey())
    }

    @Test
    fun stableVehicleIdFallsBackToVinDisplayNameOrUnknownVehicle() {
        assertEquals("WBATEST001", sampleVehicle().stableVehicleId())
        assertEquals(
            "BMW E61 520d E61",
            sampleVehicle().copy(id = "", vin = "").stableVehicleId()
        )
        assertEquals(
            "unknown_vehicle",
            legacyVehicleId(vin = "", brand = "", model = "", generation = "")
        )
    }

    private fun sampleVehicle() = Vehicle(
        brand = "BMW",
        model = "E61 520d",
        generation = "E61",
        engine = "M47N2 2.0d",
        year = "2006",
        vin = "WBATEST001",
        mileage = "285000",
        note = "warsztat"
    )
}
