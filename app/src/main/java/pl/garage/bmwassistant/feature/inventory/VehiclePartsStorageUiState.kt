package pl.garage.bmwassistant.feature.inventory

import pl.garage.bmwassistant.model.ConsumableItem
import pl.garage.bmwassistant.model.PartInventoryItem
import pl.garage.bmwassistant.model.ShoppingListItem
import pl.garage.bmwassistant.model.VehicleArea

data class VehiclePartsStorageUiState(
    val isLoading: Boolean = false,
    val selectedTab: InventoryTab = InventoryTab.Overview,
    val inventoryParts: List<PartInventoryItem> = emptyList(),
    val shoppingList: List<ShoppingListItem> = emptyList(),
    val consumables: List<ConsumableItem> = emptyList(),
    val selectedPartId: String? = null,
    val selectedShoppingItemId: String? = null,
    val searchQuery: String = "",
    val activeDialog: InventoryDialogState? = null,
    val errorMessage: String? = null,
) {
    val selectedInventoryPart: PartInventoryItem?
        get() = inventoryParts.firstOrNull { it.id == selectedPartId }

    val selectedShoppingItem: ShoppingListItem?
        get() = shoppingList.firstOrNull { it.id == selectedShoppingItemId }

    val filteredInventoryParts: List<PartInventoryItem>
        get() {
            val normalizedQuery = searchQuery.trim().lowercase()
            if (normalizedQuery.isBlank()) return inventoryParts
            return inventoryParts.filter { part ->
                listOf(
                    part.name,
                    part.oemPartNumber,
                    part.manufacturerPartNumber,
                    part.manufacturer,
                    part.locationNote
                ).any { value -> value.lowercase().contains(normalizedQuery) }
            }
        }
}

enum class InventoryTab {
    Overview,
    Storage,
    Shopping,
    Consumables,
}

data class InventoryItemInput(
    val oemPartNumber: String,
    val manufacturerPartNumber: String,
    val name: String,
    val manufacturer: String,
    val repairId: String?,
    val repairTitle: String?,
    val quantity: Int,
    val purchasePrice: String,
    val realOemUrl: String?,
    val photoUri: String?,
    val locationNote: String,
    val area: VehicleArea = VehicleArea.Service,
)

sealed interface InventoryDialogState {
    data object AddPartOptions : InventoryDialogState
    data object AddManualPart : InventoryDialogState
    data object AddExternalPart : InventoryDialogState
    data class EditPart(val partId: String) : InventoryDialogState
    data class DeletePart(val partId: String) : InventoryDialogState
    data class DeleteShoppingItem(val shoppingItemId: String) : InventoryDialogState
    data class AcceptShoppingItem(val shoppingItemId: String) : InventoryDialogState
    data class ShoppingItemDetails(val shoppingItemId: String) : InventoryDialogState
    data class InventoryItemDetails(val partId: String) : InventoryDialogState
}
