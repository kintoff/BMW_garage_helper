package pl.garage.bmwassistant.database.vehicle

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import pl.garage.bmwassistant.model.PartInventoryItem

@Entity(
    tableName = "inventory_parts",
    foreignKeys = [
        ForeignKey(
            entity = RepairProjectEntity::class,
            parentColumns = ["repairId"],
            childColumns = ["repairId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = ShoppingListItemEntity::class,
            parentColumns = ["shoppingItemId"],
            childColumns = ["originShoppingItemId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("repairId"), Index("originShoppingItemId")]
)
data class InventoryPartEntity(
    @PrimaryKey
    val inventoryPartId: String,
    val originShoppingItemId: String? = null,
    val repairId: String? = null,
    val oemPartNumber: String,
    val manufacturerPartNumber: String,
    val name: String,
    val manufacturer: String,
    val quantity: Int,
    val purchasePrice: String,
    val realOemUrl: String? = null,
    val photoUri: String? = null,
    val locationNote: String = "",
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

fun InventoryPartEntity.toModel(repairTitle: String?): PartInventoryItem = PartInventoryItem(
    id = inventoryPartId,
    oemPartNumber = oemPartNumber,
    manufacturerPartNumber = manufacturerPartNumber,
    name = name,
    manufacturer = manufacturer,
    repairTitle = repairTitle,
    quantity = quantity,
    purchasePrice = purchasePrice,
    realOemUrl = realOemUrl,
    photoUri = photoUri,
    repairId = repairId
)

fun PartInventoryItem.toEntity(
    createdAtEpochMillis: Long,
    updatedAtEpochMillis: Long,
    originShoppingItemId: String? = null,
    locationNote: String = "",
): InventoryPartEntity = InventoryPartEntity(
    inventoryPartId = id,
    originShoppingItemId = originShoppingItemId,
    repairId = repairId,
    oemPartNumber = oemPartNumber,
    manufacturerPartNumber = manufacturerPartNumber,
    name = name,
    manufacturer = manufacturer,
    quantity = quantity,
    purchasePrice = purchasePrice,
    realOemUrl = realOemUrl,
    photoUri = photoUri,
    locationNote = locationNote,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis
)
