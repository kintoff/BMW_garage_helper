package pl.garage.bmwassistant.model

data class ConsumableItem(
    val id: String,
    val name: String,
    val producer: String,
    val quantity: String,
    val purchasePrice: String,
    val notes: String,
)
