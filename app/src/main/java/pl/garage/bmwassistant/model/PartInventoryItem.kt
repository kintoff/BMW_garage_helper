package pl.garage.bmwassistant.model

data class PartInventoryItem(
    val id: String,
    val oemPartNumber: String,
    val manufacturerPartNumber: String,
    val name: String,
    val manufacturer: String,
    val repairTitle: String?,
    val quantity: Int,
    val purchasePrice: String,
    val realOemUrl: String?,
    val photoUri: String? = null,
    val repairId: String? = null,
    val originShoppingItemId: String? = null,
    val locationNote: String = "",
    val createdAtEpochMillis: Long = 0L,
    val updatedAtEpochMillis: Long = 0L,
) {
    val partNumber: String
        get() = oemPartNumber.ifBlank { manufacturerPartNumber }
}
