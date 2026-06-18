package pl.garage.bmwassistant.database.vehicle

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import pl.garage.bmwassistant.model.InventoryHistoryEvent

@Entity(
    tableName = "inventory_history_events",
    indices = [
        Index("inventoryPartId"),
        Index("createdAtEpochMillis")
    ]
)
data class InventoryHistoryEventEntity(
    @PrimaryKey
    val eventId: String,
    val inventoryPartId: String,
    val eventType: String,
    val title: String,
    val quantityDelta: Int,
    val quantityAfter: Int,
    val note: String = "",
    val createdAtEpochMillis: Long,
)

fun InventoryHistoryEventEntity.toModel(): InventoryHistoryEvent = InventoryHistoryEvent(
    id = eventId,
    inventoryPartId = inventoryPartId,
    type = eventType,
    title = title,
    quantityDelta = quantityDelta,
    quantityAfter = quantityAfter,
    note = note,
    createdAtEpochMillis = createdAtEpochMillis
)

fun InventoryHistoryEvent.toEntity(): InventoryHistoryEventEntity = InventoryHistoryEventEntity(
    eventId = id,
    inventoryPartId = inventoryPartId,
    eventType = type,
    title = title,
    quantityDelta = quantityDelta,
    quantityAfter = quantityAfter,
    note = note,
    createdAtEpochMillis = createdAtEpochMillis
)

private fun PartInventoryItemHistorySeed.toEntity(): InventoryHistoryEventEntity =
    InventoryHistoryEventEntity(
        eventId = eventId,
        inventoryPartId = inventoryPartId,
        eventType = eventType,
        title = title,
        quantityDelta = quantityDelta,
        quantityAfter = quantityAfter,
        note = note,
        createdAtEpochMillis = createdAtEpochMillis
    )

data class PartInventoryItemHistorySeed(
    val eventId: String,
    val inventoryPartId: String,
    val eventType: String,
    val title: String,
    val quantityDelta: Int,
    val quantityAfter: Int,
    val note: String = "",
    val createdAtEpochMillis: Long,
)

fun InventoryPartEntity.initialHistoryEvent(): InventoryHistoryEventEntity {
    val eventType = if (originShoppingItemId.isNullOrBlank()) {
        INVENTORY_HISTORY_ADDED_MANUALLY
    } else {
        INVENTORY_HISTORY_ACCEPTED_FROM_SHOPPING
    }
    val title = if (eventType == INVENTORY_HISTORY_ACCEPTED_FROM_SHOPPING) {
        "Przyjęto do magazynu"
    } else {
        "Dodano do magazynu"
    }
    val timestamp = createdAtEpochMillis.takeIf { it > 0L } ?: updatedAtEpochMillis
    return PartInventoryItemHistorySeed(
        eventId = "${inventoryPartId}_created",
        inventoryPartId = inventoryPartId,
        eventType = eventType,
        title = title,
        quantityDelta = quantity,
        quantityAfter = quantity,
        note = locationNote.takeIf { it.isNotBlank() }?.let { "Lokalizacja: $it" }.orEmpty(),
        createdAtEpochMillis = timestamp
    ).toEntity()
}

const val INVENTORY_HISTORY_ADDED_MANUALLY = "ADDED_MANUALLY"
const val INVENTORY_HISTORY_ACCEPTED_FROM_SHOPPING = "ACCEPTED_FROM_SHOPPING"
const val INVENTORY_HISTORY_QUANTITY_INCREASED = "QUANTITY_INCREASED"
const val INVENTORY_HISTORY_QUANTITY_DECREASED = "QUANTITY_DECREASED"
const val INVENTORY_HISTORY_EDITED = "EDITED"
const val INVENTORY_HISTORY_REMOVED = "REMOVED"
