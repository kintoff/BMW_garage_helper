package pl.garage.bmwassistant.database.vehicle

import androidx.room.Entity
import androidx.room.PrimaryKey
import pl.garage.bmwassistant.model.RepairCheckpoint
import pl.garage.bmwassistant.model.RepairProject
import pl.garage.bmwassistant.model.VehicleArea

@Entity(tableName = "repair_projects")
data class RepairProjectEntity(
    @PrimaryKey
    val repairId: String,
    val title: String,
    val area: String,
    val status: String,
    val priority: String,
    val problemDescription: String,
    val goal: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val completedAtEpochMillis: Long? = null,
    val sortOrder: Int = 0,
    val isArchived: Boolean = false,
)

fun RepairProjectEntity.toModel(
    vehicleName: String,
    checkpoints: List<RepairCheckpoint> = emptyList(),
    partsToIdentify: List<String> = emptyList(),
    documentsToCollect: List<String> = emptyList(),
): RepairProject = RepairProject(
    title = title,
    area = area.toVehicleArea(),
    vehicleName = vehicleName,
    status = status,
    priority = priority,
    problemDescription = problemDescription,
    goal = goal,
    checklist = checkpoints.map { it.text },
    partsToIdentify = partsToIdentify,
    documentsToCollect = documentsToCollect,
    checkpoints = checkpoints,
    id = repairId
)

fun RepairProject.toEntity(
    createdAtEpochMillis: Long,
    updatedAtEpochMillis: Long,
    completedAtEpochMillis: Long? = null,
    sortOrder: Int = 0,
    isArchived: Boolean = false,
): RepairProjectEntity = RepairProjectEntity(
    repairId = id,
    title = title,
    area = area.name,
    status = status,
    priority = priority,
    problemDescription = problemDescription,
    goal = goal,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
    completedAtEpochMillis = completedAtEpochMillis,
    sortOrder = sortOrder,
    isArchived = isArchived
)

internal fun String.toVehicleArea(): VehicleArea =
    VehicleArea.entries.firstOrNull { it.name == this } ?: VehicleArea.Service
