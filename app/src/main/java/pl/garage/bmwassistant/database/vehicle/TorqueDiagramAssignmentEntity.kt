package pl.garage.bmwassistant.database.vehicle

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import pl.garage.bmwassistant.model.TorqueDiagramAssignment

@Entity(
    tableName = "torque_diagram_assignments",
    foreignKeys = [
        ForeignKey(
            entity = TorqueSpecTableEntity::class,
            parentColumns = ["tableId"],
            childColumns = ["tableId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TorqueSpecEntity::class,
            parentColumns = ["torqueSpecId"],
            childColumns = ["torqueSpecId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tableId"), Index("torqueSpecId")]
)
data class TorqueDiagramAssignmentEntity(
    @PrimaryKey
    val assignmentId: String,
    val tableId: String,
    val torqueSpecId: String,
    val xRatio: Float,
    val yRatio: Float,
    val sortOrder: Int,
)

fun TorqueDiagramAssignmentEntity.toModel(
    specIndex: Int,
): TorqueDiagramAssignment = TorqueDiagramAssignment(
    torqueSpecIndex = specIndex,
    xRatio = xRatio,
    yRatio = yRatio
)

fun TorqueDiagramAssignment.toEntity(
    tableId: String,
    torqueSpecId: String,
    sortOrder: Int,
): TorqueDiagramAssignmentEntity = TorqueDiagramAssignmentEntity(
    assignmentId = "${tableId}_assignment_$sortOrder",
    tableId = tableId,
    torqueSpecId = torqueSpecId,
    xRatio = xRatio,
    yRatio = yRatio,
    sortOrder = sortOrder
)
