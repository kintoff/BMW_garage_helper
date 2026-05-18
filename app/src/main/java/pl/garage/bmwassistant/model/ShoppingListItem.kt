package pl.garage.bmwassistant.model

data class ShoppingListItem(
    val id: String = "",
    val partNumber: String,
    val manufacturerPartNumber: String = "",
    val name: String,
    val manufacturer: String = "",
    val repairTitle: String,
    val repairId: String = "",
    val area: VehicleArea = VehicleArea.Service,
    val quantity: Int,
    val source: String,
    val price: String = "",
    val imageUri: String? = null,
    val shopUrl: String? = null,
    val realOemUrl: String? = null,
)
