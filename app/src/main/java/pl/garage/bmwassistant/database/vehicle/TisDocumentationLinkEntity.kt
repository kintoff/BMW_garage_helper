package pl.garage.bmwassistant.database.vehicle

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import pl.garage.bmwassistant.model.TisDocumentationLink

@Entity(
    tableName = "tis_documentation_links",
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
data class TisDocumentationLinkEntity(
    @PrimaryKey
    val tisLinkId: String,
    val documentationId: String,
    val title: String,
    val url: String,
    val sortOrder: Int,
)

fun TisDocumentationLinkEntity.toModel(): TisDocumentationLink = TisDocumentationLink(
    title = title,
    url = url
)

fun TisDocumentationLink.toEntity(
    documentationId: String,
    sortOrder: Int,
): TisDocumentationLinkEntity = TisDocumentationLinkEntity(
    tisLinkId = "${documentationId}_tis_$sortOrder",
    documentationId = documentationId,
    title = title,
    url = url,
    sortOrder = sortOrder
)
