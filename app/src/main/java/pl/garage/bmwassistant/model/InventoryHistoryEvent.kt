package pl.garage.bmwassistant.model

data class InventoryHistoryEvent(
    val id: String,
    val inventoryPartId: String,
    val type: String,
    val title: String,
    val quantityDelta: Int,
    val quantityAfter: Int,
    val note: String = "",
    val createdAtEpochMillis: Long,
)
