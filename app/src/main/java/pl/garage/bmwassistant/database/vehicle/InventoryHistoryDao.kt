package pl.garage.bmwassistant.database.vehicle

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface InventoryHistoryDao {
    @Query("SELECT * FROM inventory_history_events WHERE inventoryPartId = :inventoryPartId ORDER BY createdAtEpochMillis DESC")
    suspend fun getEventsForPart(inventoryPartId: String): List<InventoryHistoryEventEntity>

    @Query("SELECT * FROM inventory_history_events ORDER BY createdAtEpochMillis DESC")
    suspend fun getAllEvents(): List<InventoryHistoryEventEntity>

    @Query("DELETE FROM inventory_history_events")
    suspend fun clearAll()

    @Query("DELETE FROM inventory_history_events WHERE inventoryPartId = :inventoryPartId")
    suspend fun deleteForPart(inventoryPartId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: InventoryHistoryEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<InventoryHistoryEventEntity>)
}
