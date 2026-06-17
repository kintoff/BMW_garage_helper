package pl.garage.bmwassistant.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import androidx.compose.ui.graphics.toArgb
import pl.garage.bmwassistant.R
import pl.garage.bmwassistant.model.Vehicle
import pl.garage.bmwassistant.model.VehicleArea

class UiComponentsRulesTest {

    @Test
    fun statusColorForMapsKnownStatusGroups() {
        assertEquals(AccentGreen, statusColorFor("Zakonczona"))
        assertEquals(AccentBlue, statusColorFor("Planowane"))
        assertEquals(AccentBlue, statusColorFor("plan serwisu"))
        assertEquals(AccentRed, statusColorFor("Problem krytyczny"))
        assertEquals(AccentRed, statusColorFor("pilny temat"))
        assertEquals(AccentYellow, statusColorFor("W trakcie"))
        assertEquals(AccentYellow, statusColorFor("Aktywny wysoki priorytet"))
        assertEquals(AccentBlue, statusColorFor("Inny status"))
    }

    @Test
    fun vehicleAreaIconResourceReturnsExpectedDrawable() {
        assertEquals(R.drawable.ic_area_engine, VehicleArea.Engine.iconResource())
        assertEquals(R.drawable.ic_area_body, VehicleArea.Body.iconResource())
        assertEquals(R.drawable.ic_area_suspension, VehicleArea.Suspension.iconResource())
        assertEquals(R.drawable.ic_area_electronics, VehicleArea.Electronics.iconResource())
        assertEquals(R.drawable.ic_area_service, VehicleArea.Service.iconResource())
    }

    @Test
    fun vehicleImageResourcesPreferSpecificKnownModels() {
        val e61 = vehicle(model = "E61 520d", generation = "E61")
        val e60 = vehicle(model = "530d", generation = "E60")
        val lowercase = vehicle(model = "520d", generation = "e61")

        assertEquals(R.drawable.car_bmw_e61_selection, e61.selectionImageResource())
        assertEquals(R.drawable.car_bmw_e61_detail, e61.detailImageResource())
        assertEquals(R.drawable.e60_bok, e60.selectionImageResource())
        assertEquals(R.drawable.e60_front, e60.detailImageResource())
        assertEquals(R.drawable.car_bmw_e61_selection, lowercase.selectionImageResource())
    }

    @Test
    fun vehicleImageResourcesFallBackToDefaultSet() {
        val other = vehicle(brand = "BMW", model = "F10", generation = "F10")

        assertEquals(R.drawable.car_bmw_e61, other.selectionImageResource())
        assertEquals(R.drawable.car_bmw_e61, other.detailImageResource())
    }

    @Test
    fun privateNavIconMappingCoversKnownTabsAndFallback() {
        assertEquals("Garage", invokeNavIconFor("Przeglad"))
        assertEquals("Wrench", invokeNavIconFor("Naprawy"))
        assertEquals("Box", invokeNavIconFor("Czesci"))
        assertEquals("Document", invokeNavIconFor("Dokumenty"))
        assertEquals("Profile", invokeNavIconFor("Wiecej"))
        assertEquals("Garage", invokeNavIconFor("Nieznane"))
    }

    @Test
    fun accentPaletteStaysDistinct() {
        assertTrue(AccentBlue != AccentYellow)
        assertTrue(AccentGreen != AccentRed)
        assertTrue(AccentPurple != AccentBlue)
    }

    @Test
    fun accentPaletteKeepsExpectedArgbValues() {
        assertEquals(0xFF4FB6FF.toInt(), AccentBlue.toArgb())
        assertEquals(0xFFFFB51F.toInt(), AccentYellow.toArgb())
        assertEquals(0xFF3ED66F.toInt(), AccentGreen.toArgb())
        assertEquals(0xFFFF5757.toInt(), AccentRed.toArgb())
        assertEquals(0xFFA77DFF.toInt(), AccentPurple.toArgb())
    }

    @Test
    fun vehicleImageSetStoresProvidedResources() {
        val set = VehicleImageSet(
            selectionImage = R.drawable.e60_bok,
            detailImage = R.drawable.e60_front
        )

        assertEquals(R.drawable.e60_bok, set.selectionImage)
        assertEquals(R.drawable.e60_front, set.detailImage)
    }

    private fun vehicle(
        brand: String = "BMW",
        model: String = "E61 520d",
        generation: String = "E61"
    ) = Vehicle(
        brand = brand,
        model = model,
        generation = generation,
        engine = "M47N2 2.0d",
        year = "2006",
        vin = "WBATEST001",
        mileage = "285000",
        note = "warsztat"
    )

    private fun invokeNavIconFor(item: String): String {
        val method = Class.forName("pl.garage.bmwassistant.ui.components.GarageUxKt")
            .getDeclaredMethod("navIconFor", String::class.java)
        method.isAccessible = true
        return java.lang.String.valueOf(method.invoke(null, item))
    }
}
