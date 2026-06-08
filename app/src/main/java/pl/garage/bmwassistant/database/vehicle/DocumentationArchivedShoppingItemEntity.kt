package pl.garage.bmwassistant.database.vehicle

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import pl.garage.bmwassistant.model.ShoppingListItem

@Entity(
    tableName = "documentation_archived_shopping_items",
    foreignKeys = [
        ForeignKey(
            entity = RepairDocumentationEntity::class,
            parentColumns = ["documentationId"],
            childColumns = ["documentationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("documentationId")]
)
data class DocumentationArchivedShoppingItemEntity(
    @PrimaryKey
    val archivedShoppingItemId: String,
    val documentationId: String,
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
    val sortOrder: Int,
)

fun DocumentationArchivedShoppingItemEntity.toModel(repairTitle: String): ShoppingListItem = ShoppingListItem(
    id = archivedShoppingItemId,
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

fun ShoppingListItem.toArchivedEntity(
    documentationId: String,
    sortOrder: Int,
): DocumentationArchivedShoppingItemEntity = DocumentationArchivedShoppingItemEntity(
    archivedShoppingItemId = id,
    documentationId = documentationId,
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
    sortOrder = sortOrder
)
