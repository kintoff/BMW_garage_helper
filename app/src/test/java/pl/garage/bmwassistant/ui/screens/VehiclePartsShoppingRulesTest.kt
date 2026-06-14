package pl.garage.bmwassistant.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.garage.bmwassistant.model.ShoppingListItem
import pl.garage.bmwassistant.model.VehicleArea

class VehiclePartsShoppingRulesTest {

    @Test
    fun shoppingItemStableIdFallsBackToKeyFields() {
        val item = shoppingItem(id = "")

        assertEquals(
            "Tylna zwrotnica lewa|33326763092|LEM-123|Tuleja wahacza",
            item.stableId()
        )
    }

    @Test
    fun toInventoryPartMapsShoppingItemAndCoercesQuantity() {
        val inventory = shoppingItem(quantity = 0, price = "", manufacturer = "").toInventoryPart(
            nextId = "inventory_1",
            receivedQuantity = 0
        )

        assertEquals("inventory_1", inventory.id)
        assertEquals("33326763092", inventory.oemPartNumber)
        assertEquals(1, inventory.quantity)
        assertEquals("do uzupelnienia", inventory.purchasePrice)
        assertEquals("do uzupelnienia", inventory.manufacturer)
    }

    @Test
    fun primaryAreaReturnsMostFrequentArea() {
        val items = listOf(
            shoppingItem(area = VehicleArea.Suspension),
            shoppingItem(area = VehicleArea.Suspension),
            shoppingItem(area = VehicleArea.Engine)
        )

        assertEquals(VehicleArea.Suspension, items.primaryArea())
    }

    @Test
    fun areaSummaryLabelReturnsNullForEmptyAndCountForManyAreas() {
        assertNull(emptyList<ShoppingListItem>().areaSummaryLabel())
        assertEquals(
            "2 obszary",
            listOf(
                shoppingItem(area = VehicleArea.Engine),
                shoppingItem(area = VehicleArea.Suspension)
            ).areaSummaryLabel()
        )
    }

    @Test
    fun shoppingTotalLabelCalculatesWeightedTotal() {
        val items = listOf(
            shoppingItem(quantity = 2, price = "10,50 PLN"),
            shoppingItem(id = "shopping_2", quantity = 1, price = "20 PLN")
        )

        assertEquals("Wartosc: 41,00 PLN", shoppingTotalLabel(items))
    }

    @Test
    fun shoppingTotalLabelFallsBackWhenNoValidPrices() {
        assertEquals(
            "Wartosc do uzupelnienia",
            shoppingTotalLabel(listOf(shoppingItem(price = "brak")))
        )
    }

    @Test
    fun afterReceivingRemovesOrReducesMatchingItem() {
        val first = shoppingItem(quantity = 3)
        val second = shoppingItem(id = "shopping_2", partNumber = "11428575211", name = "Filtr oleju")

        val updated = listOf(first, second).afterReceiving(
            receivedItem = first,
            receivedQuantity = 2
        )

        assertEquals(2, updated.size)
        assertEquals(1, updated.first().quantity)

        val removed = listOf(first, second).afterReceiving(
            receivedItem = first,
            receivedQuantity = 3
        )

        assertEquals(1, removed.size)
        assertTrue(removed.none { it.stableId() == first.stableId() })
    }

    private fun shoppingItem(
        id: String = "shopping_1",
        area: VehicleArea = VehicleArea.Suspension,
        quantity: Int = 2,
        price: String = "249.99",
        manufacturer: String = "Lemforder",
        partNumber: String = "33326763092",
        manufacturerPartNumber: String = "LEM-123",
        name: String = "Tuleja wahacza"
    ) = ShoppingListItem(
        id = id,
        partNumber = partNumber,
        manufacturerPartNumber = manufacturerPartNumber,
        name = name,
        manufacturer = manufacturer,
        repairTitle = "Tylna zwrotnica lewa",
        repairId = "repair_rear_knuckle",
        area = area,
        quantity = quantity,
        source = "Autodoc",
        price = price
    )
}
