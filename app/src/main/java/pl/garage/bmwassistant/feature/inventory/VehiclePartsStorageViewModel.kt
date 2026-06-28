package pl.garage.bmwassistant.feature.inventory

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pl.garage.bmwassistant.model.ConsumableItem
import pl.garage.bmwassistant.model.PartInventoryItem
import pl.garage.bmwassistant.model.ShoppingListItem
import java.util.UUID

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
    private val events = Channel<VehiclePartsStorageEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    fun updateInventoryParts(parts: List<PartInventoryItem>) {
        _uiState.value = _uiState.value.copy(
            inventoryParts = parts,
            errorMessage = null
        )
    }

    fun updateShoppingList(items: List<ShoppingListItem>) {
        _uiState.value = _uiState.value.copy(
            shoppingList = items,
            errorMessage = null
        )
    }

    fun onAction(action: VehiclePartsStorageAction) {
        _uiState.value = when (action) {
            VehiclePartsStorageAction.LoadData -> _uiState.value
            is VehiclePartsStorageAction.SelectTab -> _uiState.value.copy(selectedTab = action.tab, errorMessage = null)
            is VehiclePartsStorageAction.SearchChanged -> _uiState.value.copy(searchQuery = action.query, errorMessage = null)
            is VehiclePartsStorageAction.PartSelected -> _uiState.value.copy(selectedPartId = action.partId, errorMessage = null)
            is VehiclePartsStorageAction.ShoppingItemSelected -> _uiState.value.copy(selectedShoppingItemId = action.shoppingItemId, errorMessage = null)
            VehiclePartsStorageAction.AddPartClicked -> _uiState.value.copy(
                activeDialog = InventoryDialogState.AddPartOptions,
                errorMessage = null
            )
            is VehiclePartsStorageAction.OpenDialog -> _uiState.value.copy(activeDialog = action.dialog, errorMessage = null)
            is VehiclePartsStorageAction.EditPartClicked -> _uiState.value.copy(
                activeDialog = InventoryDialogState.EditPart(action.partId),
                errorMessage = null
            )
            is VehiclePartsStorageAction.DeletePartClicked -> _uiState.value.copy(
                activeDialog = InventoryDialogState.DeletePart(action.partId),
                errorMessage = null
            )
            is VehiclePartsStorageAction.DeleteShoppingItemClicked -> _uiState.value.copy(
                activeDialog = InventoryDialogState.DeleteShoppingItem(action.shoppingItemId),
                errorMessage = null
            )
            VehiclePartsStorageAction.DialogDismissed -> _uiState.value.copy(activeDialog = null, errorMessage = null)
            is VehiclePartsStorageAction.ConfirmDeletePart -> deletePart(action.partId)
            is VehiclePartsStorageAction.ConfirmDeleteShoppingItem -> deleteShoppingItem(action.shoppingItemId)
            is VehiclePartsStorageAction.SavePart -> savePart(action.part)
            is VehiclePartsStorageAction.AddInventoryItem -> addInventoryItem(action.input)
            is VehiclePartsStorageAction.EditInventoryItem -> editInventoryItem(action.partId, action.input)
            is VehiclePartsStorageAction.AcceptShoppingItemToInventory -> acceptShoppingItem(action.shoppingItemId, action.quantity)
            is VehiclePartsStorageAction.ShoppingListChanged -> _uiState.value.copy(shoppingList = action.items, errorMessage = null)
            is VehiclePartsStorageAction.InventoryChanged -> _uiState.value.copy(inventoryParts = action.items, errorMessage = null)
            is VehiclePartsStorageAction.ShowOnDiagram -> showOnDiagram(action.partId)
        }
    }

    private fun addInventoryItem(input: InventoryItemInput): VehiclePartsStorageUiState {
        val now = System.currentTimeMillis()
        val part = input.toInventoryPart(
            id = UUID.randomUUID().toString(),
            createdAt = now,
            updatedAt = now
        )
        return _uiState.value.copy(
            inventoryParts = _uiState.value.inventoryParts + part,
            selectedPartId = part.id,
            activeDialog = null,
            errorMessage = null
        )
    }

    private fun editInventoryItem(
        partId: String,
        input: InventoryItemInput,
    ): VehiclePartsStorageUiState {
        val current = _uiState.value.inventoryParts.firstOrNull { it.id == partId }
            ?: return _uiState.value.copy(errorMessage = "Nie znaleziono części w magazynie.")
        val now = System.currentTimeMillis()
        val updated = input.toInventoryPart(
            id = current.id,
            createdAt = current.createdAtEpochMillis,
            updatedAt = now,
            originShoppingItemId = current.originShoppingItemId
        )
        return _uiState.value.copy(
            inventoryParts = _uiState.value.inventoryParts.map { part ->
                if (part.id == partId) updated else part
            },
            selectedPartId = updated.id,
            activeDialog = null,
            errorMessage = null
        )
    }

    private fun savePart(part: PartInventoryItem): VehiclePartsStorageUiState =
        if (_uiState.value.inventoryParts.any { it.id == part.id }) {
            _uiState.value.copy(
                inventoryParts = _uiState.value.inventoryParts.map { current ->
                    if (current.id == part.id) part else current
                },
                selectedPartId = part.id,
                activeDialog = null,
                errorMessage = null
            )
        } else {
            _uiState.value.copy(
                inventoryParts = _uiState.value.inventoryParts + part,
                selectedPartId = part.id,
                activeDialog = null,
                errorMessage = null
            )
        }

    private fun deletePart(partId: String): VehiclePartsStorageUiState =
        _uiState.value.copy(
            inventoryParts = _uiState.value.inventoryParts.filterNot { it.id == partId },
            selectedPartId = _uiState.value.selectedPartId.takeUnless { it == partId },
            activeDialog = null,
            errorMessage = null
        )

    private fun deleteShoppingItem(shoppingItemId: String): VehiclePartsStorageUiState {
        _uiState.value.shoppingList.firstOrNull { it.id == shoppingItemId }
            ?: return _uiState.value.copy(errorMessage = "Nie znaleziono części na liście zakupów.")
        events.trySend(VehiclePartsStorageEvent.ShowMessage("Usunięto część z listy zakupów."))
        return _uiState.value.copy(
            shoppingList = _uiState.value.shoppingList.filterNot { it.id == shoppingItemId },
            selectedShoppingItemId = _uiState.value.selectedShoppingItemId.takeUnless { it == shoppingItemId },
            activeDialog = null,
            errorMessage = null
        )
    }

    private fun acceptShoppingItem(
        shoppingItemId: String,
        quantity: Int,
    ): VehiclePartsStorageUiState {
        val item = _uiState.value.shoppingList.firstOrNull { it.id == shoppingItemId }
            ?: return _uiState.value.copy(errorMessage = "Nie znaleziono części na liście zakupów.")
        val receivedQuantity = quantity.coerceAtLeast(1).coerceAtMost(item.quantity.coerceAtLeast(1))
        val now = System.currentTimeMillis()
        val inventoryPart = item.toInventoryPart(
            id = UUID.randomUUID().toString(),
            quantity = receivedQuantity,
            createdAt = now,
            updatedAt = now
        )
        val remainingQuantity = item.quantity - receivedQuantity
        val updatedShoppingList = _uiState.value.shoppingList.mapNotNull { current ->
            if (current.id != item.id) {
                current
            } else if (remainingQuantity > 0) {
                current.copy(quantity = remainingQuantity)
            } else {
                null
            }
        }
        events.trySend(VehiclePartsStorageEvent.ShowMessage("Część została przyjęta do magazynu."))
        return _uiState.value.copy(
            inventoryParts = _uiState.value.inventoryParts + inventoryPart,
            shoppingList = updatedShoppingList,
            selectedPartId = inventoryPart.id,
            selectedShoppingItemId = if (remainingQuantity > 0) item.id else null,
            activeDialog = null,
            errorMessage = null
        )
    }

    private fun showOnDiagram(partId: String): VehiclePartsStorageUiState {
        val part = _uiState.value.inventoryParts.firstOrNull { it.id == partId }
            ?: return _uiState.value.copy(errorMessage = "Nie znaleziono części w magazynie.")
        val url = part.realOemUrl?.takeIf { it.isNotBlank() } ?: part.imageSearchUrl()
        events.trySend(VehiclePartsStorageEvent.OpenUrl(url))
        return _uiState.value.copy(
            selectedPartId = partId,
            errorMessage = null
        )
    }
}
