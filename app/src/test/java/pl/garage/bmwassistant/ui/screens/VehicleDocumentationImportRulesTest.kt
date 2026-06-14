package pl.garage.bmwassistant.ui.screens

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pl.garage.bmwassistant.model.PersonalDocumentationItemType
import pl.garage.bmwassistant.model.RepairDocumentation
import pl.garage.bmwassistant.model.TorqueDiagramAssignment
import pl.garage.bmwassistant.model.TorqueSpec
import pl.garage.bmwassistant.model.TorqueSpecTable
import pl.garage.bmwassistant.model.VehicleArea

class VehicleDocumentationImportRulesTest {

    @Test
    fun effectiveTorqueTablesBuildsFallbackFromLegacyFields() {
        val tables = documentation().copy(
            torqueSpecs = listOf(
                TorqueSpec(
                    component = "Sruba amortyzatora",
                    torque = "100 Nm",
                    source = "TIS",
                    notes = ""
                )
            ),
            torqueDiagramImageUri = "content://diagram/1",
            torqueDiagramAssignments = listOf(TorqueDiagramAssignment(0, 0.4f, 0.6f)),
            torqueTables = emptyList()
        ).effectiveTorqueTables()

        assertEquals(1, tables.size)
        assertEquals("Tabela momentow 1", tables.single().title)
        assertEquals("content://diagram/1", tables.single().diagramImageUri)
    }

    @Test
    fun effectiveTorqueTablesReturnsEmptyWhenNoLegacyOrNewDataExists() {
        val tables = documentation().effectiveTorqueTables()

        assertEquals(emptyList<TorqueSpecTable>(), tables)
    }

    @Test
    fun toImportedDocumentationRestoresTorqueTablesAndFlattensFirstTable() {
        val imported = JSONObject()
            .put("title", "Dokumentacja po imporcie")
            .put("summary", "Nowy opis")
            .put("torqueTables", JSONArray().put(
                JSONObject()
                    .put("id", "table_custom")
                    .put("title", "Tabela tyl")
                    .put("diagramImageUri", "package://diagram.png")
                    .put("torqueSpecs", JSONArray().put(
                        JSONObject()
                            .put("component", "Sruba A")
                            .put("torque", "60 Nm")
                            .put("source", "TIS")
                            .put("notes", "")
                    ))
                    .put("diagramAssignments", JSONArray().put(
                        JSONObject()
                            .put("torqueSpecIndex", 0)
                            .put("xRatio", 0.25)
                            .put("yRatio", 0.75)
                    ))
            ))
            .put("personalNotes", JSONArray().put(
                JSONObject()
                    .put("type", "Link")
                    .put("title", "Forum")
                    .put("url", "https://example.com")
            ))

        val result = imported.toImportedDocumentation(
            currentDocumentation = documentation(),
            resolveAsset = { raw -> raw?.replace("package://", "content://imported/") },
            personalIdFactory = { index -> "generated_$index" }
        )

        assertEquals("Dokumentacja po imporcie", result.title)
        assertEquals("Nowy opis", result.summary)
        assertEquals(1, result.torqueTables.size)
        assertEquals("table_custom", result.torqueTables.single().id)
        assertEquals("content://imported/diagram.png", result.torqueTables.single().diagramImageUri)
        assertEquals("60 Nm", result.torqueSpecs.single().torque)
        assertEquals(1, result.personalNotes.size)
        assertEquals("generated_0", result.personalNotes.single().id)
        assertEquals(PersonalDocumentationItemType.Link, result.personalNotes.single().type)
    }

    @Test
    fun toImportedTorqueTablesAppliesFallbackIdsAndTitles() {
        val tables = JSONArray().put(
            JSONObject()
                .put("torqueSpecs", JSONArray())
                .put("diagramAssignments", JSONArray())
        ).toImportedTorqueTables { it }

        assertEquals("table-1", tables.single().id)
        assertEquals("Tabela momentow 1", tables.single().title)
    }

    @Test
    fun toImportedPersonalNotesFallsBackToTextTypeAndGeneratedId() {
        val notes = JSONArray().put(
            JSONObject()
                .put("type", "UNKNOWN")
                .put("title", "")
                .put("text", "Uwagi")
                .put("uri", "package://file.jpg")
        ).toImportedPersonalNotes(
            resolveAsset = { raw -> raw?.replace("package://", "content://") },
            personalIdFactory = { index -> "note_$index" }
        )

        assertEquals("note_0", notes.single().id)
        assertEquals(PersonalDocumentationItemType.Text, notes.single().type)
        assertEquals("Wpis 1", notes.single().title)
        assertEquals("content://file.jpg", notes.single().uri)
    }

    @Test
    fun toImportedYoutubeVideosSkipsEntriesWithoutUrl() {
        val videos = JSONArray()
            .put(JSONObject().put("title", "Pusty"))
            .put(JSONObject().put("url", "https://youtube.com/watch?v=abc123xyz89"))
            .toImportedYoutubeVideos()

        assertEquals(1, videos.size)
        assertEquals("Film YouTube 2", videos.single().title)
    }

    @Test
    fun toImportedShoppingListDefaultsUnknownAreaToService() {
        val items = JSONArray().put(
            JSONObject()
                .put("id", "s1")
                .put("name", "Sruba")
                .put("area", "UNKNOWN")
        ).toImportedShoppingList()

        assertEquals(VehicleArea.Service, items.single().area)
        assertNull(items.single().imageUri)
    }

    private fun documentation() = RepairDocumentation(
        title = "Dokumentacja: Tylna zwrotnica lewa",
        area = VehicleArea.Suspension,
        repairTitle = "Tylna zwrotnica lewa",
        summary = "Dokumentacja powiazana z naprawa: Tylna zwrotnica lewa.",
        repairId = "repair_rear_knuckle"
    )
}
