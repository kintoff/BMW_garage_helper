package pl.garage.bmwassistant.database.vehicle

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import pl.garage.bmwassistant.model.RepairCheckpoint

@Entity(
    tableName = "repair_checkpoints",
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
data class RepairCheckpointEntity(
    @PrimaryKey
    val checkpointId: String,
    val repairId: String,
    val text: String,
    val isDone: Boolean,
    val sortOrder: Int,
)

fun RepairCheckpointEntity.toModel(): RepairCheckpoint = RepairCheckpoint(
    id = checkpointId,
    text = text,
    isDone = isDone
)

fun RepairCheckpoint.toEntity(
    repairId: String,
    sortOrder: Int,
): RepairCheckpointEntity = RepairCheckpointEntity(
    checkpointId = id,
    repairId = repairId,
    text = text,
    isDone = isDone,
    sortOrder = sortOrder
)
