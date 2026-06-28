package pl.garage.bmwassistant.feature.inventory

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.garage.bmwassistant.model.PartInventoryItem
import pl.garage.bmwassistant.model.ShoppingListItem
import pl.garage.bmwassistant.model.VehicleArea

class VehiclePartsStorageViewModelTest {

    @Test
    fun addInventoryItemCreatesPartAndSelectsIt() {
        val viewModel = VehiclePartsStorageViewModel()

        viewModel.onAction(
            VehiclePartsStorageAction.AddInventoryItem(
                InventoryItemInput(
                    oemPartNumber = "33326763092",
                    manufacturerPartNumber = "LEM-123",
                    name = "Tuleja wahacza",
                    manufacturer = "Lemforder",
                    repairId = "repair_1",
                    repairTitle = "Tylna zwrotnica lewa",
                    quantity = 2,
                    purchasePrice = "249,99 PLN",
                    realOemUrl = "https://example.com/oem",
                    photoUri = null,
                    locationNote = "Polka A"
                )
            )
        )

        val state = viewModel.uiState.value
        assertEquals(1, state.inventoryParts.size)
        assertEquals(state.inventoryParts.first().id, state.selectedPartId)
        assertEquals("Polka A", state.inventoryParts.first().locationNote)
        assertTrue(state.inventoryParts.first().createdAtEpochMillis > 0L)
    }

    @Test
    fun acceptShoppingItemMovesRequestedQuantityToInventory() = runBlocking {
        val shoppingItem = shoppingItem(quantity = 3)
        val viewModel = VehiclePartsStorageViewModel(initialShoppingList = listOf(shoppingItem))

        viewModel.onAction(
            VehiclePartsStorageAction.AcceptShoppingItemToInventory(
                shoppingItemId = shoppingItem.id,
                quantity = 2
            )
        )

        val event = viewModel.eventFlow.first()
        val state = viewModel.uiState.value

        assertEquals(
            VehiclePartsStorageEvent.ShowMessage("Część została przyjęta do magazynu."),
            event
        )
        assertEquals(1, state.inventoryParts.size)
        assertEquals(1, state.shoppingList.size)
        assertEquals(1, state.shoppingList.first().quantity)
        assertEquals("Naprawa tylnej osi", state.inventoryParts.first().repairTitle)
        assertEquals(shoppingItem.id, state.selectedShoppingItemId)
    }

    @Test
    fun deleteShoppingItemRemovesItAndEmitsMessage() = runBlocking {
        val shoppingItem = shoppingItem()
        val viewModel = VehiclePartsStorageViewModel(initialShoppingList = listOf(shoppingItem))
        viewModel.onAction(VehiclePartsStorageAction.ShoppingItemSelected(shoppingItem.id))

        viewModel.onAction(VehiclePartsStorageAction.ConfirmDeleteShoppingItem(shoppingItem.id))

        val event = viewModel.eventFlow.first()
        val state = viewModel.uiState.value

        assertEquals(
            VehiclePartsStorageEvent.ShowMessage("Usunięto część z listy zakupów."),
            event
        )
        assertTrue(state.shoppingList.isEmpty())
        assertNull(state.selectedShoppingItemId)
    }

    @Test
    fun showOnDiagramUsesPartnerUrlWhenAvailable() = runBlocking {
        val part = inventoryPart(realOemUrl = "https://example.com/diagram")
        val viewModel = VehiclePartsStorageViewModel(initialInventoryParts = listOf(part))

        viewModel.onAction(VehiclePartsStorageAction.ShowOnDiagram(part.id))

        assertEquals(
            VehiclePartsStorageEvent.OpenUrl("https://example.com/diagram"),
            viewModel.eventFlow.first()
        )
        assertEquals(part.id, viewModel.uiState.value.selectedPartId)
    }

    @Test
    fun showOnDiagramFallsBackToImageSearchForPartWithoutUrl() = runBlocking {
        val part = inventoryPart(realOemUrl = null)
        val viewModel = VehiclePartsStorageViewModel(initialInventoryParts = listOf(part))

        viewModel.onAction(VehiclePartsStorageAction.ShowOnDiagram(part.id))

        val event = viewModel.eventFlow.first() as VehiclePartsStorageEvent.OpenUrl
        assertTrue(event.url.contains("google.com/search?tbm=isch"))
        assertTrue(event.url.contains("LEM-123"))
    }

    private fun shoppingItem(
        id: String = "shopping_1",
        quantity: Int = 2,
    ) = ShoppingListItem(
        id = id,
        partNumber = "33326763092",
        manufacturerPartNumber = "LEM-123",
        name = "Tuleja wahacza",
        manufacturer = "Lemforder",
        repairTitle = "Naprawa tylnej osi",
        repairId = "repair_1",
        area = VehicleArea.Suspension,
        quantity = quantity,
        source = "Autodoc",
        price = "249,99 PLN",
        realOemUrl = "https://example.com/oem"
    )

    private fun inventoryPart(
        id: String = "inventory_1",
        realOemUrl: String?,
    ) = PartInventoryItem(
        id = id,
        oemPartNumber = "33326763092",
        manufacturerPartNumber = "LEM-123",
        name = "Tuleja wahacza",
        manufacturer = "Lemforder",
        repairTitle = "Naprawa tylnej osi",
        quantity = 1,
        purchasePrice = "249,99 PLN",
        realOemUrl = realOemUrl,
        repairId = "repair_1"
    )
}
