package pl.garage.bmwassistant.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.garage.bmwassistant.model.PersonalDocumentationItemType
import pl.garage.bmwassistant.model.RepairCheckpoint
import pl.garage.bmwassistant.model.RepairDocumentation
import pl.garage.bmwassistant.model.RepairProject
import pl.garage.bmwassistant.model.TisDocumentationLink
import pl.garage.bmwassistant.model.VehicleArea
import pl.garage.bmwassistant.model.YoutubeVideo

class VehicleRepairEditingRulesTest {

    @Test
    fun effectiveCheckpointsFallsBackToChecklistWhenStructuredCheckpointsMissing() {
        val checkpoints = repair(
            checklist = listOf("Podnies auto", "Sprawdz luzy"),
            checkpoints = emptyList()
        ).effectiveCheckpoints()

        assertEquals(2, checkpoints.size)
        assertEquals("checkpoint-1", checkpoints[0].id)
        assertEquals("Podnies auto", checkpoints[0].text)
        assertFalse(checkpoints[0].isDone)
    }

    @Test
    fun effectiveCheckpointsKeepsExplicitCheckpointProgress() {
        val explicit = listOf(
            RepairCheckpoint(id = "manual_1", text = "Zdemontuj oslone", isDone = true)
        )

        val checkpoints = repair(checkpoints = explicit).effectiveCheckpoints()

        assertEquals(explicit, checkpoints)
    }

    @Test
    fun withCheckpointsSynchronizesChecklistWithEditedValues() {
        val updated = repair().withCheckpoints(
            listOf(
                RepairCheckpoint(id = "c1", text = "Nowy krok", isDone = true),
                RepairCheckpoint(id = "c2", text = "Drugi krok", isDone = false)
            )
        )

        assertEquals(listOf("Nowy krok", "Drugi krok"), updated.checklist)
        assertTrue(updated.checkpoints.first().isDone)
    }

    @Test
    fun withUserNotesCreatesDocumentationWhenMissing() {
        val repair = repair(title = "Wymiana amorow")

        val documentation = null.withUserNotes(repair, "  Dokrecic gorne mocowanie  ")

        assertEquals(repair.id, documentation.repairId)
        assertEquals("Dokumentacja: Wymiana amorow", documentation.title)
        assertEquals("Dokrecic gorne mocowanie", documentation.userNotes)
    }

    @Test
    fun withUserNotesUpdatesExistingDocumentationWithoutChangingIdentity() {
        val repair = repair()
        val existing = documentation(repair).copy(userNotes = "stare")

        val updated = existing.withUserNotes(repair, "  nowe uwagi ")

        assertEquals(existing.repairId, updated.repairId)
        assertEquals(existing.title, updated.title)
        assertEquals("nowe uwagi", updated.userNotes)
    }

    @Test
    fun effectiveTisDocumentsFallsBackToLegacyLinks() {
        val links = documentation(repair()).copy(
            tisLinks = listOf("newtis.info/test"),
            tisDocuments = emptyList()
        ).effectiveTisDocuments()

        assertEquals(1, links.size)
        assertEquals("newtis.info/test", links.single().title)
        assertEquals("newtis.info/test", links.single().url)
    }

    @Test
    fun effectiveYoutubeVideosFallsBackToLegacyLinks() {
        val videos = documentation(repair()).copy(
            youtubeLinks = listOf("youtube.com/watch?v=abc123"),
            youtubeVideos = emptyList()
        ).effectiveYoutubeVideos()

        assertEquals(1, videos.size)
        assertEquals("Film YouTube", videos.single().title)
        assertEquals("youtube.com/watch?v=abc123", videos.single().url)
    }

    @Test
    fun effectiveTisDocumentsKeepsStructuredEntriesWhenPresent() {
        val explicit = listOf(TisDocumentationLink("TIS tyl", "https://tis.example/tyl"))

        val links = documentation(repair()).copy(
            tisLinks = listOf("legacy"),
            tisDocuments = explicit
        ).effectiveTisDocuments()

        assertEquals(explicit, links)
    }

    @Test
    fun effectiveYoutubeVideosKeepsStructuredEntriesWhenPresent() {
        val explicit = listOf(
            YoutubeVideo(
                title = "Instrukcja",
                url = "https://youtube.com/watch?v=abc123xyz89"
            )
        )

        val videos = documentation(repair()).copy(
            youtubeLinks = listOf("legacy"),
            youtubeVideos = explicit
        ).effectiveYoutubeVideos()

        assertEquals(explicit, videos)
    }

    @Test
    fun withHttpsPrefixAddsSchemeOnlyWhenMissing() {
        assertEquals("https://newtis.info/test", " newtis.info/test ".withHttpsPrefix())
        assertEquals("http://example.com", "http://example.com".withHttpsPrefix())
        assertEquals("https://example.com", "https://example.com".withHttpsPrefix())
        assertEquals("content://local/photo", "content://local/photo".withHttpsPrefix())
    }

    @Test
    fun defaultDocumentationTitleReturnsReadableLabels() {
        assertEquals("Zdjecie", PersonalDocumentationItemType.Photo.defaultDocumentationTitle())
        assertEquals("Dokument", PersonalDocumentationItemType.Document.defaultDocumentationTitle())
        assertEquals("Notatka", PersonalDocumentationItemType.Text.defaultDocumentationTitle())
        assertEquals("Plik", PersonalDocumentationItemType.File.defaultDocumentationTitle())
    }

    private fun repair(
        title: String = "Tylna zwrotnica lewa",
        checklist: List<String> = listOf("Odkrec kolo"),
        checkpoints: List<RepairCheckpoint> = checklist.mapIndexed { index, item ->
            RepairCheckpoint(id = "seed_$index", text = item, isDone = false)
        },
    ) = RepairProject(
        title = title,
        area = VehicleArea.Suspension,
        vehicleName = "BMW E61 520d",
        status = "W trakcie",
        priority = "Wysoki",
        problemDescription = "Stuki z tylu",
        goal = "Usunac luzy",
        checklist = checklist,
        partsToIdentify = emptyList(),
        documentsToCollect = emptyList(),
        checkpoints = checkpoints,
        id = "repair_${title.lowercase().replace(' ', '_')}"
    )

    private fun documentation(repair: RepairProject) = RepairDocumentation(
        title = "Dokumentacja: ${repair.title}",
        area = repair.area,
        repairTitle = repair.title,
        summary = "Dokumentacja powiazana z naprawa: ${repair.title}.",
        repairId = repair.id
    )
}
