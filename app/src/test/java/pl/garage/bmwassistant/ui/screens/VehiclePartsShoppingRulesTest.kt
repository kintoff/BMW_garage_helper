package pl.garage.bmwassistant.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.garage.bmwassistant.model.PartInventoryItem
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
    fun inventoryPartStableIdFallsBackToRepairAndPartFields() {
        val item = PartInventoryItem(
            id = "",
            oemPartNumber = "33326763092",
            manufacturerPartNumber = "LEM-123",
            name = "Tuleja wahacza",
            manufacturer = "Lemforder",
            repairTitle = "Tylna zwrotnica lewa",
            quantity = 1,
            purchasePrice = "249.99",
            realOemUrl = null,
            repairId = "repair_rear_knuckle"
        )

        assertEquals(
            "repair_rear_knuckle|Tylna zwrotnica lewa|33326763092|LEM-123|Tuleja wahacza",
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
        assertEquals("shopping_1", inventory.originShoppingItemId)
        assertTrue(inventory.createdAtEpochMillis > 0L)
        assertTrue(inventory.updatedAtEpochMillis > 0L)
    }

    @Test
    fun toInventoryPartFallsBackWhenPartNumbersAndRepairIdAreBlank() {
        val inventory = shoppingItem(
            id = "",
            partNumber = "",
            manufacturerPartNumber = "",
            manufacturer = "",
            price = "",
            repairTitle = "",
            repairId = ""
        ).toInventoryPart(nextId = "inventory_2")

        assertEquals("do uzupelnienia", inventory.oemPartNumber)
        assertEquals("do uzupelnienia", inventory.manufacturerPartNumber)
        assertNull(inventory.repairTitle)
        assertNull(inventory.repairId)
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
    fun areaSummaryLabelReturnsReadableLabelForSingleArea() {
        assertEquals(
            VehicleArea.Engine.label,
            listOf(
                shoppingItem(area = VehicleArea.Engine),
                shoppingItem(id = "shopping_2", area = VehicleArea.Engine)
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
    fun shoppingItemPriceLabelsShowTotalAndUnitPriceForMultiplePieces() {
        val item = shoppingItem(quantity = 3, price = "12,50 PLN")

        assertEquals(37.50, item.totalPriceAmount(), 0.001)
        assertEquals("37,50 PLN", item.totalPriceLabel())
        assertEquals("12,50 PLN / szt.", item.unitPriceInfoLabel())
    }

    @Test
    fun inventoryPriceLabelsShowTotalAndUnitPriceForMultiplePieces() {
        val item = PartInventoryItem(
            id = "inventory_1",
            oemPartNumber = "33326763092",
            manufacturerPartNumber = "LEM-123",
            name = "Tuleja wahacza",
            manufacturer = "Lemforder",
            repairTitle = "Tylna zwrotnica lewa",
            quantity = 2,
            purchasePrice = "249,99 PLN",
            realOemUrl = null,
            repairId = "repair_rear_knuckle"
        )

        assertEquals(499.98, item.totalPurchasePriceAmount(), 0.001)
        assertEquals("499,98 PLN", item.totalPurchasePriceLabel())
        assertEquals("249,99 PLN / szt.", item.unitPurchasePriceInfoLabel())
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

    @Test
    fun afterReceivingLeavesListUntouchedWhenNoStableIdMatches() {
        val first = shoppingItem(id = "shopping_1", quantity = 3)
        val second = shoppingItem(id = "shopping_2", partNumber = "11428575211", name = "Filtr oleju")

        val updated = listOf(first, second).afterReceiving(
            receivedItem = shoppingItem(id = "shopping_3", partNumber = "999", name = "Inna czesc"),
            receivedQuantity = 1
        )

        assertEquals(listOf(first, second), updated)
    }

    private fun shoppingItem(
        id: String = "shopping_1",
        area: VehicleArea = VehicleArea.Suspension,
        quantity: Int = 2,
        price: String = "249.99",
        manufacturer: String = "Lemforder",
        partNumber: String = "33326763092",
        manufacturerPartNumber: String = "LEM-123",
        name: String = "Tuleja wahacza",
        repairTitle: String = "Tylna zwrotnica lewa",
        repairId: String = "repair_rear_knuckle"
    ) = ShoppingListItem(
        id = id,
        partNumber = partNumber,
        manufacturerPartNumber = manufacturerPartNumber,
        name = name,
        manufacturer = manufacturer,
        repairTitle = repairTitle,
        repairId = repairId,
        area = area,
        quantity = quantity,
        source = "Autodoc",
        price = price
    )
}
