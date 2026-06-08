package pl.garage.bmwassistant.database.vehicle

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import pl.garage.bmwassistant.model.ShoppingListItem

@Entity(
    tableName = "shopping_list_items",
    foreignKeys = [
        ForeignKey(
            entity = RepairProjectEntity::class,
            parentColumns = ["repairId"],
            childColumns = ["repairId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("repairId")]
)
data class ShoppingListItemEntity(
    @PrimaryKey
    val shoppingItemId: String,
    val repairId: String,
    val partNumber: String,
    val manufacturerPartNumber: String,
    val name: String,
    val manufacturer: String,
    val quantity: Int,
    val source: String,
    val price: String,
    val imageUri: String? = null,
    val shopUrl: String? = null,
    val realOemUrl: String? = null,
    val area: String,
    val status: String,
    val archivedInDocumentation: Boolean = false,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

fun ShoppingListItemEntity.toModel(repairTitle: String): ShoppingListItem = ShoppingListItem(
    id = shoppingItemId,
    partNumber = partNumber,
    manufacturerPartNumber = manufacturerPartNumber,
    name = name,
    manufacturer = manufacturer,
    repairTitle = repairTitle,
    repairId = repairId,
    area = area.toVehicleArea(),
    quantity = quantity,
    source = source,
    price = price,
    imageUri = imageUri,
    shopUrl = shopUrl,
    realOemUrl = realOemUrl
)

fun ShoppingListItem.toEntity(
    createdAtEpochMillis: Long,
    updatedAtEpochMillis: Long,
    status: String = if (source.isNotBlank()) "planned" else "unknown",
    archivedInDocumentation: Boolean = false,
): ShoppingListItemEntity = ShoppingListItemEntity(
    shoppingItemId = id,
    repairId = repairId,
    partNumber = partNumber,
    manufacturerPartNumber = manufacturerPartNumber,
    name = name,
    manufacturer = manufacturer,
    quantity = quantity,
    source = source,
    price = price,
    imageUri = imageUri,
    shopUrl = shopUrl,
    realOemUrl = realOemUrl,
    area = area.name,
    status = status,
    archivedInDocumentation = archivedInDocumentation,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis
)
