package pl.garage.bmwassistant.model

data class ShoppingListItem(
    val partNumber: String,
    val name: String,
    val repairTitle: String,
    val quantity: Int,
    val source: String,
)
