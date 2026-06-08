package pl.garage.bmwassistant.database.vehicle

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "repair_parts_to_identify",
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
data class RepairPartsToIdentifyEntity(
    @PrimaryKey
    val itemId: String,
    val repairId: String,
    val text: String,
    val sortOrder: Int,
)
