package pl.garage.bmwassistant.database.vehicle

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ShoppingListDao {
    @Query("SELECT * FROM shopping_list_items ORDER BY updatedAtEpochMillis DESC")
    suspend fun getAllItems(): List<ShoppingListItemEntity>

    @Query("SELECT * FROM shopping_list_items WHERE repairId = :repairId ORDER BY updatedAtEpochMillis DESC")
    suspend fun getItemsForRepair(repairId: String): List<ShoppingListItemEntity>

    @Query("SELECT * FROM shopping_list_items WHERE shoppingItemId = :shoppingItemId LIMIT 1")
    suspend fun getItemById(shoppingItemId: String): ShoppingListItemEntity?

    @Query("DELETE FROM shopping_list_items")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ShoppingListItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ShoppingListItemEntity>)

    @Update
    suspend fun update(item: ShoppingListItemEntity)

    @Query("DELETE FROM shopping_list_items WHERE shoppingItemId = :shoppingItemId")
    suspend fun deleteById(shoppingItemId: String)
}
