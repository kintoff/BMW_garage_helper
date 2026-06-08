package pl.garage.bmwassistant.database.vehicle

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import pl.garage.bmwassistant.model.TorqueSpec

@Entity(
    tableName = "torque_specs",
    foreignKeys = [
        ForeignKey(
            entity = TorqueSpecTableEntity::class,
            parentColumns = ["tableId"],
            childColumns = ["tableId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tableId")]
)
data class TorqueSpecEntity(
    @PrimaryKey
    val torqueSpecId: String,
    val tableId: String,
    val component: String,
    val type: String,
    val thread: String,
    val tighteningSpecifications: String,
    val torque: String,
    val source: String,
    val notes: String,
    val sortOrder: Int,
)

fun TorqueSpecEntity.toModel(): TorqueSpec = TorqueSpec(
    component = component,
    type = type,
    thread = thread,
    tighteningSpecifications = tighteningSpecifications,
    torque = torque,
    source = source,
    notes = notes
)

fun TorqueSpec.toEntity(
    tableId: String,
    sortOrder: Int,
): TorqueSpecEntity = TorqueSpecEntity(
    torqueSpecId = "${tableId}_spec_$sortOrder",
    tableId = tableId,
    component = component,
    type = type,
    thread = thread,
    tighteningSpecifications = tighteningSpecifications,
    torque = torque,
    source = source,
    notes = notes,
    sortOrder = sortOrder
)
