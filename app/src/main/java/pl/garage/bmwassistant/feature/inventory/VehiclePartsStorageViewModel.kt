package pl.garage.bmwassistant.feature.inventory

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pl.garage.bmwassistant.model.ConsumableItem
import pl.garage.bmwassistant.model.PartInventoryItem
import pl.garage.bmwassistant.model.ShoppingListItem

class VehiclePartsStorageViewModel(
    initialInventoryParts: List<PartInventoryItem> = emptyList(),
    initialShoppingList: List<ShoppingListItem> = emptyList(),
    initialConsumables: List<ConsumableItem> = emptyList(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        VehiclePartsStorageUiState(
            inventoryParts = initialInventoryParts,
            shoppingList = initialShoppingList,
            consumables = initialConsumables
        )
    )
    val uiState: StateFlow<VehiclePartsStorageUiState> = _uiState.asStateFlow()

    fun updateInventoryParts(parts: List<PartInventoryItem>) {
        _uiState.value = _uiState.value.copy(inventoryParts = parts)
    }

    fun updateShoppingList(items: List<ShoppingListItem>) {
        _uiState.value = _uiState.value.copy(shoppingList = items)
    }

    fun onAction(action: VehiclePartsStorageAction) {
        _uiState.value = when (action) {
            is VehiclePartsStorageAction.SearchChanged -> _uiState.value.copy(searchQuery = action.query)
            is VehiclePartsStorageAction.PartSelected -> _uiState.value.copy(selectedPartId = action.partId)
            VehiclePartsStorageAction.AddPartClicked -> _uiState.value.copy(activeDialog = InventoryDialog.AddPartOptions)
            is VehiclePartsStorageAction.EditPartClicked -> _uiState.value.copy(activeDialog = InventoryDialog.EditPart(action.partId))
            is VehiclePartsStorageAction.DeletePartClicked -> _uiState.value.copy(activeDialog = InventoryDialog.DeletePart(action.partId))
            VehiclePartsStorageAction.DialogDismissed -> _uiState.value.copy(activeDialog = null)
            is VehiclePartsStorageAction.ConfirmDeletePart,
            is VehiclePartsStorageAction.SavePart,
            is VehiclePartsStorageAction.ShowOnDiagram -> _uiState.value
        }
    }
}
