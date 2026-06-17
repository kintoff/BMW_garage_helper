package pl.garage.bmwassistant.database.catalog

import org.junit.Assert.assertEquals
import org.junit.Test

class VehicleCatalogEntityTest {

    @Test
    fun toVehicleMapsCatalogEntityToDomainModel() {
        val entity = VehicleCatalogEntity(
            vehicleId = "vehicle_1",
            brand = "BMW",
            model = "E61 520d",
            generation = "E61",
            engine = "M47N2 2.0d",
            year = "2006",
            vin = "WBATEST001",
            mileage = "285000",
            note = "warsztat",
            partsCatalogUrl = "https://czescidobmw.pl/test",
            databasePath = "/tmp/vehicle.db",
            filesDirectoryPath = "/tmp/files",
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 2L
        )

        val vehicle = entity.toVehicle()

        assertEquals("vehicle_1", vehicle.id)
        assertEquals("BMW", vehicle.brand)
        assertEquals("E61 520d", vehicle.model)
        assertEquals("https://czescidobmw.pl/test", vehicle.partsCatalogUrl)
    }
}
