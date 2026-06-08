package pl.garage.bmwassistant.database.vehicle

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import pl.garage.bmwassistant.model.YoutubeVideo

@Entity(
    tableName = "youtube_videos",
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
data class YoutubeVideoEntity(
    @PrimaryKey
    val youtubeVideoId: String,
    val documentationId: String,
    val title: String,
    val url: String,
    val note: String,
    val sortOrder: Int,
)

fun YoutubeVideoEntity.toModel(): YoutubeVideo = YoutubeVideo(
    title = title,
    url = url,
    note = note
)

fun YoutubeVideo.toEntity(
    documentationId: String,
    sortOrder: Int,
): YoutubeVideoEntity = YoutubeVideoEntity(
    youtubeVideoId = "${documentationId}_youtube_$sortOrder",
    documentationId = documentationId,
    title = title,
    url = url,
    note = note,
    sortOrder = sortOrder
)
