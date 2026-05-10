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
)
