package pl.garage.bmwassistant.ui.screens

import pl.garage.bmwassistant.model.PersonalDocumentationItem
import pl.garage.bmwassistant.model.PersonalDocumentationItemType
import pl.garage.bmwassistant.model.RepairDocumentation
import pl.garage.bmwassistant.model.TisDocumentationLink
import pl.garage.bmwassistant.model.YoutubeVideo

internal fun RepairDocumentation.effectiveDocumentationTisDocuments(): List<TisDocumentationLink> =
    tisDocuments.ifEmpty {
        tisLinks.mapIndexed { index, link ->
            TisDocumentationLink(
                title = "TIS ${index + 1}",
                url = link
            )
        }
    }

internal fun RepairDocumentation.effectiveDocumentationYoutubeVideos(): List<YoutubeVideo> =
    youtubeVideos.ifEmpty {
        youtubeLinks.mapIndexed { index, link ->
            YoutubeVideo(
                title = link.documentationYoutubeVideoId()?.let { "Film YouTube $it" } ?: "Film YouTube ${index + 1}",
                url = link
            )
        }
    }

internal fun RepairDocumentation.withAddedDocumentationTisLink(
    link: TisDocumentationLink,
): RepairDocumentation =
    copy(
        tisDocuments = effectiveDocumentationTisDocuments() + link,
        tisLinks = emptyList()
    )

internal fun RepairDocumentation.withUpdatedDocumentationTisLink(
    index: Int,
    link: TisDocumentationLink,
): RepairDocumentation =
    copy(
        tisDocuments = effectiveDocumentationTisDocuments()
            .mapIndexed { currentIndex, currentLink ->
                if (currentIndex == index) link else currentLink
            },
        tisLinks = emptyList()
    )

internal fun RepairDocumentation.withRemovedDocumentationTisLink(index: Int): RepairDocumentation =
    copy(
        tisDocuments = effectiveDocumentationTisDocuments()
            .filterIndexed { currentIndex, _ -> currentIndex != index },
        tisLinks = emptyList()
    )

internal fun RepairDocumentation.withAddedDocumentationYoutubeVideo(
    video: YoutubeVideo,
): RepairDocumentation =
    copy(
        youtubeVideos = effectiveDocumentationYoutubeVideos() + video,
        youtubeLinks = emptyList()
    )

internal fun RepairDocumentation.withUpdatedDocumentationYoutubeVideo(
    index: Int,
    video: YoutubeVideo,
): RepairDocumentation =
    copy(
        youtubeVideos = effectiveDocumentationYoutubeVideos()
            .mapIndexed { currentIndex, currentVideo ->
                if (currentIndex == index) video else currentVideo
            },
        youtubeLinks = emptyList()
    )

internal fun RepairDocumentation.withRemovedDocumentationYoutubeVideo(index: Int): RepairDocumentation =
    copy(
        youtubeVideos = effectiveDocumentationYoutubeVideos()
            .filterIndexed { currentIndex, _ -> currentIndex != index },
        youtubeLinks = emptyList()
    )

internal fun RepairDocumentation.withAddedPersonalNote(
    item: PersonalDocumentationItem,
): RepairDocumentation =
    copy(personalNotes = personalNotes + item)

internal fun RepairDocumentation.withUpdatedPersonalNote(
    index: Int,
    item: PersonalDocumentationItem,
): RepairDocumentation =
    copy(
        personalNotes = personalNotes.mapIndexed { currentIndex, currentItem ->
            if (currentIndex == index) item else currentItem
        }
    )

internal fun RepairDocumentation.withRemovedPersonalNote(index: Int): RepairDocumentation =
    copy(
        personalNotes = personalNotes.filterIndexed { currentIndex, _ -> currentIndex != index }
    )

internal fun PersonalDocumentationItemType.defaultPersonalTitle(): String =
    when (this) {
        PersonalDocumentationItemType.Text -> "Notatka"
        PersonalDocumentationItemType.Photo -> "Zdjecie"
        PersonalDocumentationItemType.Video -> "Film"
        PersonalDocumentationItemType.Document -> "Dokument"
        PersonalDocumentationItemType.Link -> "Link"
        PersonalDocumentationItemType.File -> "Plik"
    }

internal fun PersonalDocumentationItemType.personalLabel(): String =
    when (this) {
        PersonalDocumentationItemType.Text -> "Notatka tekstowa"
        PersonalDocumentationItemType.Photo -> "Zdjecie"
        PersonalDocumentationItemType.Video -> "Film"
        PersonalDocumentationItemType.Document -> "Dokument"
        PersonalDocumentationItemType.Link -> "Link"
        PersonalDocumentationItemType.File -> "Plik"
    }

internal fun PersonalDocumentationItemType.categoryLabel(): String =
    when (this) {
        PersonalDocumentationItemType.Text -> "Notatki"
        PersonalDocumentationItemType.Photo -> "Zdjecia"
        PersonalDocumentationItemType.Video -> "Filmy"
        PersonalDocumentationItemType.Document -> "Dokumenty"
        PersonalDocumentationItemType.Link -> "Linki"
        PersonalDocumentationItemType.File -> "Pliki"
    }

internal fun String.documentationYoutubeVideoId(): String? {
    val normalized = trim()
    val patterns = listOf(
        Regex("[?&]v=([A-Za-z0-9_-]{11})"),
        Regex("youtu\\.be/([A-Za-z0-9_-]{11})"),
        Regex("youtube\\.com/embed/([A-Za-z0-9_-]{11})"),
        Regex("youtube\\.com/shorts/([A-Za-z0-9_-]{11})")
    )
    return patterns.firstNotNullOfOrNull { pattern ->
        pattern.find(normalized)?.groupValues?.getOrNull(1)
    }
}
