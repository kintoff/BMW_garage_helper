package pl.garage.bmwassistant.feature.inventory

import pl.garage.bmwassistant.model.PartInventoryItem

sealed interface VehiclePartsStorageAction {
    data class SearchChanged(val query: String) : VehiclePartsStorageAction
    data class PartSelected(val partId: String) : VehiclePartsStorageAction
    data object AddPartClicked : VehiclePartsStorageAction
    data class EditPartClicked(val partId: String) : VehiclePartsStorageAction
    data class DeletePartClicked(val partId: String) : VehiclePartsStorageAction
    data class ConfirmDeletePart(val partId: String) : VehiclePartsStorageAction
    data class SavePart(val part: PartInventoryItem) : VehiclePartsStorageAction
    data class ShowOnDiagram(val partId: String) : VehiclePartsStorageAction
    data object DialogDismissed : VehiclePartsStorageAction
}
