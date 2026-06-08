package pl.garage.bmwassistant.database.catalog

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface VehicleCatalogDao {
    @Query("SELECT * FROM vehicles WHERE isArchived = 0 ORDER BY updatedAtEpochMillis DESC")
    suspend fun getActiveVehicles(): List<VehicleCatalogEntity>

    @Query("SELECT * FROM vehicles ORDER BY updatedAtEpochMillis DESC")
    suspend fun getAllVehicles(): List<VehicleCatalogEntity>

    @Query("SELECT * FROM vehicles WHERE vehicleId = :vehicleId LIMIT 1")
    suspend fun getVehicleById(vehicleId: String): VehicleCatalogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vehicle: VehicleCatalogEntity)

    @Update
    suspend fun update(vehicle: VehicleCatalogEntity)

    @Query("DELETE FROM vehicles WHERE vehicleId = :vehicleId")
    suspend fun deleteById(vehicleId: String)
}
