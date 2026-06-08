package pl.garage.bmwassistant.database.vehicle

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "repair_documents_to_collect",
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
data class RepairDocumentsToCollectEntity(
    @PrimaryKey
    val itemId: String,
    val repairId: String,
    val text: String,
    val sortOrder: Int,
)
