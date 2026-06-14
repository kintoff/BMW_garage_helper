package pl.garage.bmwassistant.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test
import pl.garage.bmwassistant.model.PersonalDocumentationItem
import pl.garage.bmwassistant.model.PersonalDocumentationItemType
import pl.garage.bmwassistant.model.RepairDocumentation
import pl.garage.bmwassistant.model.TisDocumentationLink
import pl.garage.bmwassistant.model.VehicleArea
import pl.garage.bmwassistant.model.YoutubeVideo

class VehicleDocumentationRulesTest {

    @Test
    fun effectiveDocumentationTisDocumentsBuildsIndexedFallbackLinks() {
        val links = documentation().copy(
            tisLinks = listOf("https://newtis.info/a", "https://newtis.info/b"),
            tisDocuments = emptyList()
        ).effectiveDocumentationTisDocuments()

        assertEquals(2, links.size)
        assertEquals("TIS 1", links[0].title)
        assertEquals("https://newtis.info/b", links[1].url)
    }

    @Test
    fun withAddedDocumentationTisLinkAppendsAndClearsLegacyLinks() {
        val updated = documentation().copy(
            tisLinks = listOf("https://legacy")
        ).withAddedDocumentationTisLink(
            TisDocumentationLink(title = "Nowy TIS", url = "https://new")
        )

        assertEquals(emptyList<String>(), updated.tisLinks)
        assertEquals(2, updated.tisDocuments.size)
        assertEquals("Nowy TIS", updated.tisDocuments.last().title)
    }

    @Test
    fun withUpdatedDocumentationTisLinkReplacesSelectedIndex() {
        val updated = documentation().copy(
            tisDocuments = listOf(
                TisDocumentationLink("A", "https://a"),
                TisDocumentationLink("B", "https://b")
            )
        ).withUpdatedDocumentationTisLink(
            index = 1,
            link = TisDocumentationLink("B2", "https://b2")
        )

        assertEquals("A", updated.tisDocuments[0].title)
        assertEquals("B2", updated.tisDocuments[1].title)
    }

    @Test
    fun withRemovedDocumentationYoutubeVideoRemovesSelectedIndexAndClearsLegacyLinks() {
        val updated = documentation().copy(
            youtubeLinks = listOf("https://youtube.com/watch?v=abc123"),
            youtubeVideos = listOf(
                YoutubeVideo("A", "https://a"),
                YoutubeVideo("B", "https://b")
            )
        ).withRemovedDocumentationYoutubeVideo(0)

        assertEquals(emptyList<String>(), updated.youtubeLinks)
        assertEquals(1, updated.youtubeVideos.size)
        assertEquals("B", updated.youtubeVideos.single().title)
    }

    @Test
    fun effectiveDocumentationYoutubeVideosBuildsVideoIdFallbackWhenPossible() {
        val videos = documentation().copy(
            youtubeLinks = listOf(
                "https://www.youtube.com/watch?v=abc123xyz89",
                "https://example.com/video"
            ),
            youtubeVideos = emptyList()
        ).effectiveDocumentationYoutubeVideos()

        assertEquals("Film YouTube abc123xyz89", videos[0].title)
        assertEquals("Film YouTube 2", videos[1].title)
    }

    @Test
    fun withAddedPersonalNoteAppendsItem() {
        val item = PersonalDocumentationItem(
            id = "p2",
            type = PersonalDocumentationItemType.Text,
            title = "Pomiar",
            text = "Sprawdzic luz"
        )

        val updated = documentation().copy(
            personalNotes = listOf(
                PersonalDocumentationItem(
                    id = "p1",
                    type = PersonalDocumentationItemType.Text,
                    title = "Stara",
                    text = "Uwagi"
                )
            )
        ).withAddedPersonalNote(item)

        assertEquals(2, updated.personalNotes.size)
        assertEquals("Pomiar", updated.personalNotes.last().title)
    }

    @Test
    fun withUpdatedPersonalNoteReplacesOnlySelectedIndex() {
        val updated = documentation().copy(
            personalNotes = listOf(
                PersonalDocumentationItem("p1", PersonalDocumentationItemType.Text, "A", "1"),
                PersonalDocumentationItem("p2", PersonalDocumentationItemType.Text, "B", "2")
            )
        ).withUpdatedPersonalNote(
            0,
            PersonalDocumentationItem("p1", PersonalDocumentationItemType.Text, "A2", "3")
        )

        assertEquals("A2", updated.personalNotes[0].title)
        assertEquals("B", updated.personalNotes[1].title)
    }

    @Test
    fun withRemovedPersonalNoteRemovesOnlySelectedIndex() {
        val updated = documentation().copy(
            personalNotes = listOf(
                PersonalDocumentationItem("p1", PersonalDocumentationItemType.Text, "A", "1"),
                PersonalDocumentationItem("p2", PersonalDocumentationItemType.Text, "B", "2")
            )
        ).withRemovedPersonalNote(1)

        assertEquals(1, updated.personalNotes.size)
        assertEquals("A", updated.personalNotes.single().title)
    }

    @Test
    fun personalDocumentationLabelsStayReadable() {
        assertEquals("Dokument", PersonalDocumentationItemType.Document.defaultPersonalTitle())
        assertEquals("Link", PersonalDocumentationItemType.Link.personalLabel())
        assertEquals("Zdjecia", PersonalDocumentationItemType.Photo.categoryLabel())
    }

    private fun documentation() = RepairDocumentation(
        title = "Dokumentacja: Tylna zwrotnica lewa",
        area = VehicleArea.Suspension,
        repairTitle = "Tylna zwrotnica lewa",
        summary = "Dokumentacja powiazana z naprawa: Tylna zwrotnica lewa.",
        repairId = "repair_rear_knuckle"
    )
}
