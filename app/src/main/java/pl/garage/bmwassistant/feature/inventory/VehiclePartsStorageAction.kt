package pl.garage.bmwassistant.feature.inventory

import pl.garage.bmwassistant.model.PartInventoryItem
import pl.garage.bmwassistant.model.ShoppingListItem

sealed interface VehiclePartsStorageAction {
    data object LoadData : VehiclePartsStorageAction
    data class SelectTab(val tab: InventoryTab) : VehiclePartsStorageAction
    data class SearchChanged(val query: String) : VehiclePartsStorageAction
    data class PartSelected(val partId: String) : VehiclePartsStorageAction
    data class ShoppingItemSelected(val shoppingItemId: String) : VehiclePartsStorageAction
    data object AddPartClicked : VehiclePartsStorageAction
    data class OpenDialog(val dialog: InventoryDialogState) : VehiclePartsStorageAction
    data class EditPartClicked(val partId: String) : VehiclePartsStorageAction
    data class DeletePartClicked(val partId: String) : VehiclePartsStorageAction
    data class DeleteShoppingItemClicked(val shoppingItemId: String) : VehiclePartsStorageAction
    data class ConfirmDeletePart(val partId: String) : VehiclePartsStorageAction
    data class ConfirmDeleteShoppingItem(val shoppingItemId: String) : VehiclePartsStorageAction
    data class SavePart(val part: PartInventoryItem) : VehiclePartsStorageAction
    data class AddInventoryItem(val input: InventoryItemInput) : VehiclePartsStorageAction
    data class EditInventoryItem(val partId: String, val input: InventoryItemInput) : VehiclePartsStorageAction
    data class AcceptShoppingItemToInventory(
        val shoppingItemId: String,
        val quantity: Int,
    ) : VehiclePartsStorageAction
    data class ShoppingListChanged(val items: List<ShoppingListItem>) : VehiclePartsStorageAction
    data class InventoryChanged(val items: List<PartInventoryItem>) : VehiclePartsStorageAction
    data class ShowOnDiagram(val partId: String) : VehiclePartsStorageAction
    data object DialogDismissed : VehiclePartsStorageAction
}
