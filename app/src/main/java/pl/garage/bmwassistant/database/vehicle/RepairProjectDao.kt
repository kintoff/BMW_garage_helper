package pl.garage.bmwassistant.database.vehicle

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface RepairProjectDao {
    @Query("SELECT * FROM repair_projects ORDER BY isArchived ASC, sortOrder ASC, updatedAtEpochMillis DESC")
    suspend fun getAllRepairs(): List<RepairProjectEntity>

    @Query("SELECT * FROM repair_projects WHERE repairId = :repairId LIMIT 1")
    suspend fun getRepairById(repairId: String): RepairProjectEntity?

    @Query("DELETE FROM repair_projects")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepair(repair: RepairProjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckpoints(checkpoints: List<RepairCheckpointEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPartsToIdentify(items: List<RepairPartsToIdentifyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocumentsToCollect(items: List<RepairDocumentsToCollectEntity>)

    @Query("DELETE FROM repair_checkpoints WHERE repairId = :repairId")
    suspend fun deleteCheckpointsForRepair(repairId: String)

    @Query("DELETE FROM repair_parts_to_identify WHERE repairId = :repairId")
    suspend fun deletePartsToIdentifyForRepair(repairId: String)

    @Query("DELETE FROM repair_documents_to_collect WHERE repairId = :repairId")
    suspend fun deleteDocumentsToCollectForRepair(repairId: String)

    @Query("SELECT * FROM repair_checkpoints WHERE repairId = :repairId ORDER BY sortOrder ASC")
    suspend fun getCheckpointsForRepair(repairId: String): List<RepairCheckpointEntity>

    @Query("SELECT * FROM repair_parts_to_identify WHERE repairId = :repairId ORDER BY sortOrder ASC")
    suspend fun getPartsToIdentifyForRepair(repairId: String): List<RepairPartsToIdentifyEntity>

    @Query("SELECT * FROM repair_documents_to_collect WHERE repairId = :repairId ORDER BY sortOrder ASC")
    suspend fun getDocumentsToCollectForRepair(repairId: String): List<RepairDocumentsToCollectEntity>

    @Update
    suspend fun updateRepair(repair: RepairProjectEntity)

    @Query("DELETE FROM repair_projects WHERE repairId = :repairId")
    suspend fun deleteRepair(repairId: String)

    @Transaction
    suspend fun replaceRepairWithCheckpoints(
        repair: RepairProjectEntity,
        checkpoints: List<RepairCheckpointEntity>,
        partsToIdentify: List<RepairPartsToIdentifyEntity>,
        documentsToCollect: List<RepairDocumentsToCollectEntity>,
    ) {
        insertRepair(repair)
        deleteCheckpointsForRepair(repair.repairId)
        deletePartsToIdentifyForRepair(repair.repairId)
        deleteDocumentsToCollectForRepair(repair.repairId)
        if (checkpoints.isNotEmpty()) {
            insertCheckpoints(checkpoints)
        }
        if (partsToIdentify.isNotEmpty()) {
            insertPartsToIdentify(partsToIdentify)
        }
        if (documentsToCollect.isNotEmpty()) {
            insertDocumentsToCollect(documentsToCollect)
        }
    }
}
