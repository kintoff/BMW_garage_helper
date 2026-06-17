package pl.garage.bmwassistant.ui.screens

import pl.garage.bmwassistant.model.PersonalDocumentationItemType
import pl.garage.bmwassistant.model.RepairCheckpoint
import pl.garage.bmwassistant.model.RepairDocumentation
import pl.garage.bmwassistant.model.RepairProject
import pl.garage.bmwassistant.model.TisDocumentationLink
import pl.garage.bmwassistant.model.YoutubeVideo

internal fun RepairProject.effectiveCheckpoints(): List<RepairCheckpoint> =
    checkpoints.ifEmpty {
        checklist.mapIndexed { index, text ->
            RepairCheckpoint(
                id = "checkpoint-${index + 1}",
                text = text,
                isDone = false
            )
        }
    }

internal fun RepairProject.withCheckpoints(updatedCheckpoints: List<RepairCheckpoint>): RepairProject =
    copy(
        checkpoints = updatedCheckpoints,
        checklist = updatedCheckpoints.map { it.text }
    )

internal fun RepairDocumentation.effectiveTisDocuments(): List<TisDocumentationLink> =
    tisDocuments.ifEmpty {
        tisLinks.map { url ->
            TisDocumentationLink(
                title = url,
                url = url
            )
        }
    }

internal fun RepairDocumentation.effectiveYoutubeVideos(): List<YoutubeVideo> =
    youtubeVideos.ifEmpty {
        youtubeLinks.map { url ->
            YoutubeVideo(
                title = "Film YouTube",
                url = url
            )
        }
    }

internal fun String.withHttpsPrefix(): String =
    trim().let { value ->
        if (value.startsWith("http://") || value.startsWith("https://") || value.startsWith("content://")) {
            value
        } else {
            "https://$value"
        }
    }

internal fun PersonalDocumentationItemType.defaultDocumentationTitle(): String = when (this) {
    PersonalDocumentationItemType.Photo -> "Zdjecie"
    PersonalDocumentationItemType.Video -> "Film"
    PersonalDocumentationItemType.Document -> "Dokument"
    PersonalDocumentationItemType.File -> "Plik"
    PersonalDocumentationItemType.Link -> "Link"
    PersonalDocumentationItemType.Text -> "Notatka"
}

internal fun RepairDocumentation?.withUserNotes(
    repair: RepairProject,
    notes: String,
): RepairDocumentation =
    (this ?: RepairDocumentation(
        title = "Dokumentacja: ${repair.title}",
        area = repair.area,
        repairTitle = repair.title,
        repairId = repair.id,
        summary = "Dokumentacja powiazana z naprawa: ${repair.title}."
    )).copy(userNotes = notes.trim())
