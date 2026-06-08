package pl.garage.bmwassistant.database.vehicle

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import pl.garage.bmwassistant.model.TorqueSpecTable

@Entity(
    tableName = "torque_spec_tables",
    foreignKeys = [
        ForeignKey(
            entity = RepairDocumentationEntity::class,
            parentColumns = ["documentationId"],
            childColumns = ["documentationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("documentationId")]
)
data class TorqueSpecTableEntity(
    @PrimaryKey
    val tableId: String,
    val documentationId: String,
    val title: String,
    val diagramImageUri: String? = null,
    val sortOrder: Int,
)

fun TorqueSpecTableEntity.toModel(
    specs: List<pl.garage.bmwassistant.model.TorqueSpec> = emptyList(),
    assignments: List<pl.garage.bmwassistant.model.TorqueDiagramAssignment> = emptyList(),
): TorqueSpecTable = TorqueSpecTable(
    id = tableId,
    title = title,
    torqueSpecs = specs,
    diagramImageUri = diagramImageUri,
    diagramAssignments = assignments
)

fun TorqueSpecTable.toEntity(
    documentationId: String,
    sortOrder: Int,
): TorqueSpecTableEntity = TorqueSpecTableEntity(
    tableId = id,
    documentationId = documentationId,
    title = title,
    diagramImageUri = diagramImageUri,
    sortOrder = sortOrder
)
