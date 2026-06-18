package pl.garage.bmwassistant.feature.inventory

import pl.garage.bmwassistant.model.ConsumableItem
import pl.garage.bmwassistant.model.PartInventoryItem
import pl.garage.bmwassistant.model.ShoppingListItem

data class VehiclePartsStorageUiState(
    val isLoading: Boolean = false,
    val inventoryParts: List<PartInventoryItem> = emptyList(),
    val shoppingList: List<ShoppingListItem> = emptyList(),
    val consumables: List<ConsumableItem> = emptyList(),
    val selectedPartId: String? = null,
    val searchQuery: String = "",
    val activeDialog: InventoryDialog? = null,
    val errorMessage: String? = null,
)

sealed interface InventoryDialog {
    data object AddPartOptions : InventoryDialog
    data object AddManualPart : InventoryDialog
    data object AddExternalPart : InventoryDialog
    data class EditPart(val partId: String) : InventoryDialog
    data class DeletePart(val partId: String) : InventoryDialog
}
