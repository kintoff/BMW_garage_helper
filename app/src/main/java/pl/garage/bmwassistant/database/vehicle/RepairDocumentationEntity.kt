package pl.garage.bmwassistant.database.vehicle

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import pl.garage.bmwassistant.model.RepairDocumentation

@Entity(
    tableName = "repair_documentation",
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
data class RepairDocumentationEntity(
    @PrimaryKey
    val documentationId: String,
    val repairId: String,
    val title: String,
    val area: String,
    val repairTitleSnapshot: String,
    val summary: String,
    val userNotes: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

fun RepairDocumentationEntity.toModel(
    archivedShoppingList: List<pl.garage.bmwassistant.model.ShoppingListItem> = emptyList(),
    tisDocuments: List<pl.garage.bmwassistant.model.TisDocumentationLink> = emptyList(),
    torqueTables: List<pl.garage.bmwassistant.model.TorqueSpecTable> = emptyList(),
    youtubeVideos: List<pl.garage.bmwassistant.model.YoutubeVideo> = emptyList(),
    personalNotes: List<pl.garage.bmwassistant.model.PersonalDocumentationItem> = emptyList(),
): RepairDocumentation = RepairDocumentation(
    title = title,
    area = area.toVehicleArea(),
    repairTitle = repairTitleSnapshot,
    summary = summary,
    archivedShoppingList = archivedShoppingList,
    tisDocuments = tisDocuments,
    torqueTables = torqueTables,
    youtubeVideos = youtubeVideos,
    personalNotes = personalNotes,
    userNotes = userNotes,
    repairId = repairId
)

fun RepairDocumentation.toEntity(
    documentationId: String,
    createdAtEpochMillis: Long,
    updatedAtEpochMillis: Long,
): RepairDocumentationEntity = RepairDocumentationEntity(
    documentationId = documentationId,
    repairId = repairId,
    title = title,
    area = area.name,
    repairTitleSnapshot = repairTitle,
    summary = summary,
    userNotes = userNotes,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis
)
