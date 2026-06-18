package pl.garage.bmwassistant.feature.inventory

sealed interface VehiclePartsStorageEvent {
    data class ShowMessage(val message: String) : VehiclePartsStorageEvent
    data class OpenUrl(val url: String) : VehiclePartsStorageEvent
}
