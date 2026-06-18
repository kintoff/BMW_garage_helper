package pl.garage.bmwassistant.database.vehicle

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface InventoryPartDao {
    @Query("SELECT * FROM inventory_parts ORDER BY updatedAtEpochMillis DESC")
    suspend fun getAllParts(): List<InventoryPartEntity>

    @Query("SELECT * FROM inventory_parts WHERE repairId = :repairId ORDER BY updatedAtEpochMillis DESC")
    suspend fun getPartsForRepair(repairId: String): List<InventoryPartEntity>

    @Query("SELECT * FROM inventory_parts WHERE inventoryPartId = :inventoryPartId LIMIT 1")
    suspend fun getPartById(inventoryPartId: String): InventoryPartEntity?

    @Query("DELETE FROM inventory_parts")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(part: InventoryPartEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(parts: List<InventoryPartEntity>)

    @Update
    suspend fun update(part: InventoryPartEntity)

    @Query("DELETE FROM inventory_parts WHERE inventoryPartId = :inventoryPartId")
    suspend fun deleteById(inventoryPartId: String)
}
