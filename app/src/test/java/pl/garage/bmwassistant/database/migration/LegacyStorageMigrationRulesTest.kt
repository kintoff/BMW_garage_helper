package pl.garage.bmwassistant.database.migration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.garage.bmwassistant.model.RepairDocumentation
import pl.garage.bmwassistant.model.TisDocumentationLink
import pl.garage.bmwassistant.model.TorqueDiagramAssignment
import pl.garage.bmwassistant.model.TorqueSpec
import pl.garage.bmwassistant.model.TorqueSpecTable
import pl.garage.bmwassistant.model.VehicleArea
import pl.garage.bmwassistant.model.YoutubeVideo

class LegacyStorageMigrationRulesTest {

    @Test
    fun migrationUsesExistingStructuredLinksWhenPresent() {
        val documentation = sampleDocumentation().copy(
            tisLinks = listOf("https://legacy.example/tis"),
            tisDocuments = listOf(
                TisDocumentationLink(
                    title = "TIS Rear Knuckle",
                    url = "https://structured.example/tis"
                )
            )
        )

        val links = migrationEffectiveTisLinks(documentation)

        assertEquals(1, links.size)
        assertEquals("https://structured.example/tis", links.single().url)
    }

    @Test
    fun migrationBuildsStructuredLinksFromLegacyUrls() {
        val links = migrationEffectiveTisLinks(
            sampleDocumentation().copy(
                tisLinks = listOf("https://legacy.example/tis"),
                tisDocuments = emptyList()
            )
        )

        assertEquals("TIS 1", links.single().title)
        assertEquals("https://legacy.example/tis", links.single().url)
    }

    @Test
    fun migrationBuildsStructuredYoutubeVideosFromLegacyUrls() {
        val videos = migrationEffectiveYoutubeVideos(
            sampleDocumentation().copy(
                youtubeLinks = listOf("https://youtube.com/watch?v=test"),
                youtubeVideos = emptyList()
            )
        )

        assertEquals("Film YouTube 1", videos.single().title)
        assertEquals("https://youtube.com/watch?v=test", videos.single().url)
    }

    @Test
    fun migrationCreatesTorqueTableFromLegacyTorqueFields() {
        val tables = migrationEffectiveTorqueTables(
            sampleDocumentation().copy(
                torqueTables = emptyList(),
                torqueSpecs = listOf(
                    TorqueSpec(
                        component = "Sruba zwrotnicy",
                        torque = "100 Nm + 90 deg",
                        source = "TIS",
                        notes = "Na obciazonym aucie"
                    )
                ),
                torqueDiagramImageUri = "content://diagram/rear-knuckle",
                torqueDiagramAssignments = listOf(TorqueDiagramAssignment(0, 0.3f, 0.7f))
            )
        )

        assertEquals(1, tables.size)
        assertEquals("Tabela momentow 1", tables.single().title)
        assertEquals(1, tables.single().torqueSpecs.size)
        assertEquals("content://diagram/rear-knuckle", tables.single().diagramImageUri)
    }

    @Test
    fun migrationLeavesStructuredTorqueTablesUntouched() {
        val table = TorqueSpecTable(
            id = "table_1",
            title = "Moja tabela",
            torqueSpecs = listOf(
                TorqueSpec(
                    component = "Sruba",
                    torque = "10 Nm",
                    source = "TIS",
                    notes = ""
                )
            )
        )

        val tables = migrationEffectiveTorqueTables(
            sampleDocumentation().copy(torqueTables = listOf(table))
        )

        assertEquals(listOf(table), tables)
    }

    @Test
    fun migrationReturnsNoTorqueTablesWhenNoLegacyTorqueDataExists() {
        val tables = migrationEffectiveTorqueTables(
            sampleDocumentation().copy(
                torqueTables = emptyList(),
                torqueSpecs = emptyList(),
                torqueDiagramImageUri = null,
                torqueDiagramAssignments = emptyList()
            )
        )

        assertTrue(tables.isEmpty())
    }

    private fun sampleDocumentation() = RepairDocumentation(
        title = "Dokumentacja tylnej zwrotnicy",
        area = VehicleArea.Suspension,
        repairTitle = "Tylna zwrotnica lewa",
        summary = "Plan pracy",
        tisLinks = emptyList(),
        tisDocuments = emptyList(),
        youtubeLinks = emptyList(),
        youtubeVideos = emptyList(),
        torqueTables = emptyList(),
        torqueSpecs = emptyList(),
        torqueDiagramAssignments = emptyList(),
        repairId = "repair_rear_knuckle"
    )
}
