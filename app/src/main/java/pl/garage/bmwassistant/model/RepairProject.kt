package pl.garage.bmwassistant.model

data class RepairProject(
    val title: String,
    val area: VehicleArea,
    val vehicleName: String,
    val status: String,
    val priority: String,
    val problemDescription: String,
    val goal: String,
    val checklist: List<String>,
    val partsToIdentify: List<String>,
    val documentsToCollect: List<String>,
    val checkpoints: List<RepairCheckpoint> = checklist.mapIndexed { index, item ->
        RepairCheckpoint(
            id = "checkpoint_${index + 1}_${item.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')}",
            text = item,
            isDone = false
        )
    },
    val id: String = stableRepairId(title = title, area = area, vehicleName = vehicleName),
)

data class RepairCheckpoint(
    val id: String,
    val text: String,
    val isDone: Boolean = false,
)

fun stableRepairId(
    title: String,
    area: VehicleArea,
    vehicleName: String,
): String {
    val rawKey = listOf(vehicleName, area.name, title)
        .joinToString("_")
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
    return "repair_${rawKey.ifBlank { "unknown" }}"
}
