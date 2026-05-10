package pl.garage.bmwassistant.model

data class RepairDocumentation(
    val title: String,
    val area: VehicleArea,
    val repairTitle: String,
    val summary: String,
    val tisLinks: List<String> = emptyList(),
    val torqueSpecs: List<TorqueSpec> = emptyList(),
)

data class TorqueSpec(
    val component: String,
    val type: String = "",
    val thread: String = "",
    val tighteningSpecifications: String = "",
    val torque: String,
    val source: String,
    val notes: String,
)
