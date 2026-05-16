package pl.garage.bmwassistant.model

data class RepairDocumentation(
    val title: String,
    val area: VehicleArea,
    val repairTitle: String,
    val summary: String,
    val tisLinks: List<String> = emptyList(),
    val tisDocuments: List<TisDocumentationLink> = emptyList(),
    val torqueSpecs: List<TorqueSpec> = emptyList(),
    val torqueDiagramImageUri: String? = null,
    val torqueDiagramAssignments: List<TorqueDiagramAssignment> = emptyList(),
    val torqueTables: List<TorqueSpecTable> = emptyList(),
    val youtubeLinks: List<String> = emptyList(),
    val youtubeVideos: List<YoutubeVideo> = emptyList(),
    val personalNotes: List<PersonalDocumentationItem> = emptyList(),
)

data class TisDocumentationLink(
    val title: String,
    val url: String,
)

data class YoutubeVideo(
    val title: String,
    val url: String,
    val note: String = "",
)

data class PersonalDocumentationItem(
    val id: String,
    val type: PersonalDocumentationItemType,
    val title: String,
    val text: String = "",
    val uri: String? = null,
    val url: String? = null,
)

enum class PersonalDocumentationItemType {
    Text,
    Photo,
    Video,
    Document,
    Link,
    File
}

data class TorqueSpecTable(
    val id: String,
    val title: String,
    val torqueSpecs: List<TorqueSpec> = emptyList(),
    val diagramImageUri: String? = null,
    val diagramAssignments: List<TorqueDiagramAssignment> = emptyList(),
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

data class TorqueDiagramAssignment(
    val torqueSpecIndex: Int,
    val xRatio: Float,
    val yRatio: Float,
)
