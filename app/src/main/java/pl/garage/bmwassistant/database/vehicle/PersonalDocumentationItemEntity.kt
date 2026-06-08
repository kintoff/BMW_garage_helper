package pl.garage.bmwassistant.database.vehicle

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import pl.garage.bmwassistant.model.PersonalDocumentationItem
import pl.garage.bmwassistant.model.PersonalDocumentationItemType

@Entity(
    tableName = "personal_documentation_items",
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
data class PersonalDocumentationItemEntity(
    @PrimaryKey
    val itemId: String,
    val documentationId: String,
    val type: String,
    val title: String,
    val text: String,
    val uri: String? = null,
    val url: String? = null,
    val sortOrder: Int,
    val createdAtEpochMillis: Long,
)

fun PersonalDocumentationItemEntity.toModel(): PersonalDocumentationItem = PersonalDocumentationItem(
    id = itemId,
    type = runCatching { PersonalDocumentationItemType.valueOf(type) }
        .getOrDefault(PersonalDocumentationItemType.Text),
    title = title,
    text = text,
    uri = uri,
    url = url
)

fun PersonalDocumentationItem.toEntity(
    documentationId: String,
    sortOrder: Int,
    createdAtEpochMillis: Long,
): PersonalDocumentationItemEntity = PersonalDocumentationItemEntity(
    itemId = id,
    documentationId = documentationId,
    type = type.name,
    title = title,
    text = text,
    uri = uri,
    url = url,
    sortOrder = sortOrder,
    createdAtEpochMillis = createdAtEpochMillis
)
