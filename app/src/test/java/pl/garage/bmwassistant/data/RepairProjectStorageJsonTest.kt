package pl.garage.bmwassistant.data

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.garage.bmwassistant.model.PersonalDocumentationItem
import pl.garage.bmwassistant.model.PersonalDocumentationItemType
import pl.garage.bmwassistant.model.RepairCheckpoint
import pl.garage.bmwassistant.model.RepairDocumentation
import pl.garage.bmwassistant.model.RepairProject
import pl.garage.bmwassistant.model.ShoppingListItem
import pl.garage.bmwassistant.model.TisDocumentationLink
import pl.garage.bmwassistant.model.TorqueDiagramAssignment
import pl.garage.bmwassistant.model.TorqueSpec
import pl.garage.bmwassistant.model.TorqueSpecTable
import pl.garage.bmwassistant.model.Vehicle
import pl.garage.bmwassistant.model.VehicleArea
import pl.garage.bmwassistant.model.YoutubeVideo
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class RepairProjectStorageJsonTest {

    @Test
    fun repairsRoundTripPreservesCheckpointsAndStableFallbacks() {
        val repairs = listOf(
            RepairProject(
                title = "Tylna zwrotnica lewa",
                area = VehicleArea.Suspension,
                vehicleName = "BMW E61 520d",
                status = "W trakcie",
                priority = "Wysoki",
                problemDescription = "Zapieczona sruba",
                goal = "Naprawa",
                checklist = listOf("Podniesc auto"),
                partsToIdentify = listOf("Sruba mimozrodowa"),
                documentsToCollect = listOf("TIS"),
                checkpoints = listOf(RepairCheckpoint("cp1", "Podniesc auto", true)),
                id = "repair_1"
            )
        )

        val restored = repairsFromJson(repairsToJson(repairs).toString())

        assertEquals(repairs, restored)
    }

    @Test
    fun repairsFromJsonUsesChecklistAsFallbackWhenCheckpointsMissing() {
        val raw = JSONArray().put(
            JSONObject()
                .put("title", "Filtr oleju")
                .put("area", VehicleArea.Service.name)
                .put("vehicleName", "BMW E61 520d")
                .put("status", "W trakcie")
                .put("priority", "Normalny")
                .put("problemDescription", "Przeglad")
                .put("goal", "Wymiana")
                .put("checklist", JSONArray().put("Odkrecic obudowe"))
        ).toString()

        val restored = repairsFromJson(raw).single()

        assertEquals(1, restored.checkpoints.size)
        assertEquals("Odkrecic obudowe", restored.checkpoints.single().text)
    }

    @Test
    fun repairsFromJsonNormalizesStatusAndFallsBackToStableIdAndArea() {
        val raw = JSONArray().put(
            JSONObject()
                .put("title", "Naprawa testowa")
                .put("area", "UNKNOWN")
                .put("vehicleName", "BMW E61 520d")
                .put("status", "finished")
                .put("priority", "Wysoki")
                .put("problemDescription", "opis")
                .put("goal", "cel")
        ).toString()

        val restored = repairsFromJson(raw).single()

        assertEquals(VehicleArea.Engine, restored.area)
        assertEquals("Zakonczona", restored.status)
        assertTrue(restored.id.startsWith("repair_bmw_e61_520d_engine_naprawa_testowa"))
    }

    @Test
    fun toRepairCheckpointsSkipsBlankEntriesAndBuildsFallbackIds() {
        val checkpoints = JSONArray()
            .put(JSONObject().put("text", "Sprawdz srube"))
            .put(JSONObject().put("id", "manual").put("text", "Dokręc"))
            .put(JSONObject().put("id", "empty").put("text", ""))
            .toRepairCheckpoints()

        assertEquals(2, checkpoints.size)
        assertEquals("checkpoint-1", checkpoints.first().id)
        assertEquals("manual", checkpoints.last().id)
    }

    @Test
    fun documentationRoundTripPreservesTorqueTablesAndFallbackDocuments() {
        val repair = sampleRepair()
        val documentation = listOf(
            RepairDocumentation(
                title = "Dokumentacja: ${repair.title}",
                area = repair.area,
                repairTitle = repair.title,
                summary = "Plan pracy",
                archivedShoppingList = listOf(sampleShoppingItem()),
                tisLinks = listOf("https://tis.example/1"),
                tisDocuments = emptyList(),
                torqueSpecs = listOf(sampleTorqueSpec()),
                torqueDiagramImageUri = "content://diagram/1",
                torqueDiagramAssignments = listOf(TorqueDiagramAssignment(0, 0.25f, 0.5f)),
                torqueTables = emptyList(),
                youtubeLinks = listOf("https://youtube.com/watch?v=test"),
                youtubeVideos = emptyList(),
                personalNotes = listOf(
                    PersonalDocumentationItem(
                        id = "note_1",
                        type = PersonalDocumentationItemType.Text,
                        title = "Uwagi",
                        text = "Przygotowac palnik"
                    )
                ),
                userNotes = "Zamowic sruby",
                repairId = repair.id
            )
        )

        val restored = documentationFromJson(
            documentationToJson(documentation).toString(),
            repairs = listOf(repair)
        ).single()

        assertEquals("https://tis.example/1", restored.tisDocuments.single().url)
        assertEquals("Film YouTube 1", restored.youtubeVideos.single().title)
        assertEquals(1, restored.torqueTables.size)
        assertEquals("Uwagi", restored.personalNotes.single().title)
    }

    @Test
    fun documentationFromJsonInfersRepairIdFromMatchingRepair() {
        val repair = sampleRepair()
        val raw = JSONArray().put(
            JSONObject()
                .put("title", "Dokumentacja")
                .put("repairId", "")
                .put("area", repair.area.name)
                .put("repairTitle", repair.title)
                .put("summary", "opis")
        ).toString()

        val restored = documentationFromJson(raw, repairs = listOf(repair)).single()

        assertEquals(repair.id, restored.repairId)
    }

    @Test
    fun collectionParsersBuildFallbackTitlesAndSkipInvalidEntries() {
        val tisDocuments = JSONArray()
            .put(JSONObject().put("title", "").put("url", "https://tis.example/1"))
            .put(JSONObject().put("title", "Brak"))
            .toTisDocuments()
        val youtubeVideos = JSONArray()
            .put(JSONObject().put("title", "").put("url", "https://youtube.com/watch?v=abc123xyz89"))
            .put(JSONObject().put("title", "Puste"))
            .toYoutubeVideos()
        val notes = JSONArray().put(
            JSONObject()
                .put("type", "UNKNOWN")
                .put("title", "")
                .put("text", "Uwagi")
        ).toPersonalNotes()

        assertEquals("TIS 1", tisDocuments.single().title)
        assertEquals("Film YouTube 1", youtubeVideos.single().title)
        assertEquals(PersonalDocumentationItemType.Text, notes.single().type)
        assertEquals("Wpis 1", notes.single().title)
    }

    @Test
    fun torqueTablesAndAssignmentsRestoreFallbackValues() {
        val tables = JSONArray().put(
            JSONObject()
                .put("torqueSpecs", JSONArray().put(JSONObject().put("component", "Sruba A")))
                .put("diagramAssignments", JSONArray().put(
                    JSONObject()
                        .put("torqueSpecIndex", 2)
                        .put("xRatio", 0.25)
                        .put("yRatio", 0.75)
                ))
        ).toTorqueTables()

        assertEquals("table-1", tables.single().id)
        assertEquals("Tabela momentow 1", tables.single().title)
        assertEquals("Sruba A", tables.single().torqueSpecs.single().component)
        assertEquals(2, tables.single().diagramAssignments.single().torqueSpecIndex)
    }

    @Test
    fun withMappedUrisUpdatesNestedDocumentationAndShoppingAssets() {
        val documentation = RepairDocumentation(
            title = "Dokumentacja",
            area = VehicleArea.Suspension,
            repairTitle = "Tylna zwrotnica lewa",
            summary = "opis",
            archivedShoppingList = listOf(sampleShoppingItem()),
            torqueSpecs = listOf(sampleTorqueSpec()),
            torqueDiagramImageUri = "asset://diagram",
            torqueDiagramAssignments = listOf(TorqueDiagramAssignment(0, 0.1f, 0.2f)),
            torqueTables = listOf(
                TorqueSpecTable(
                    id = "table_1",
                    title = "Tabela",
                    torqueSpecs = listOf(sampleTorqueSpec()),
                    diagramImageUri = "asset://table-diagram",
                    diagramAssignments = listOf(TorqueDiagramAssignment(0, 0.3f, 0.4f))
                )
            ),
            personalNotes = listOf(
                PersonalDocumentationItem(
                    id = "note_1",
                    type = PersonalDocumentationItemType.Photo,
                    title = "Foto",
                    uri = "asset://note-photo"
                )
            ),
            repairId = "repair_rear_knuckle"
        )

        val mapped = documentation.withMappedUris { raw -> raw?.replace("asset://", "content://") }

        assertEquals("content://diagram", mapped.torqueDiagramImageUri)
        assertEquals("content://table-diagram", mapped.torqueTables.single().diagramImageUri)
        assertEquals("content://note-photo", mapped.personalNotes.single().uri)
        assertEquals("content://images/1", mapped.archivedShoppingList.single().imageUri)
    }

    @Test
    fun assetAndArchiveHelpersBuildExpectedFallbackKeys() {
        val assetMap = JSONArray()
            .put(JSONObject().put("id", "asset_1").put("fileName", "").put("path", ""))
            .put(JSONObject().put("id", "").put("fileName", "skip.bin"))
            .toAssetMap()
        val shoppingItem = sampleShoppingItem().copy(id = "", imageUri = null, shopUrl = null, realOemUrl = null)

        assertEquals("asset_1.bin", assetMap["asset_1"]?.fileName)
        assertEquals("asset_1", assetMap["asset_1"]?.path)
        assertEquals(1, assetMap.size)
        assertTrue(shoppingItem.archiveImportKey().contains("33326763092"))
    }

    @Test
    fun stringListAndStorageKeyFallbacksRemainStable() {
        val values = JSONArray().put("A").put("B").toStringList()
        val vehicle = Vehicle(
            brand = "BMW",
            model = "E61 520d",
            generation = "E61",
            engine = "M47N2 2.0d",
            year = "2006",
            vin = "",
            mileage = "285000",
            note = "warsztat",
            id = ""
        )

        assertEquals(listOf("A", "B"), values)
        assertEquals("BMW E61 520d E61", vehicle.storageKey())
    }

    @Test
    fun readRepairArchiveSupportsZippedManifest() {
        val archiveBytes = ByteArrayOutputStream().use { output ->
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(
                    JSONObject()
                        .put("format", "BMW_GARAGE_REPAIR_ARCHIVE")
                        .put("repair", JSONObject().put("title", "Test"))
                        .toString()
                        .toByteArray()
                )
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("assets/photo.txt"))
                zip.write("sample".toByteArray())
                zip.closeEntry()
            }
            output.toByteArray()
        }

        val payload = readRepairArchive(archiveBytes)

        assertNotNull(payload)
        assertEquals("BMW_GARAGE_REPAIR_ARCHIVE", payload?.manifest?.optString("format"))
        assertEquals("sample", payload?.assetBytes?.get("assets/photo.txt")?.toString(Charsets.UTF_8))
    }

    @Test
    fun readRepairArchiveSupportsLegacyJsonAssets() {
        val raw = JSONObject()
            .put("format", "BMW_GARAGE_REPAIR_ARCHIVE")
            .put(
                "assets",
                JSONArray().put(
                    JSONObject()
                        .put("id", "asset_1")
                        .put("data", Base64.getEncoder().encodeToString("legacy".toByteArray()))
                )
            )
            .toString()
            .toByteArray()

        val payload = readRepairArchive(raw)

        assertEquals("legacy", payload?.assetBytes?.get("asset_1")?.toString(Charsets.UTF_8))
    }

    @Test
    fun readRepairArchiveReturnsNullForInvalidPayload() {
        assertNull(readRepairArchive("not-json".toByteArray()))
    }

    @Test
    fun readRepairArchivePrefersZipAndLegacyReaderRejectsWrongFormat() {
        val zipped = ByteArrayOutputStream().use { output ->
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(
                    JSONObject()
                        .put("format", "BMW_GARAGE_REPAIR_ARCHIVE")
                        .put("repair", JSONObject().put("title", "Zip first"))
                        .toString()
                        .toByteArray()
                )
                zip.closeEntry()
            }
            output.toByteArray()
        }

        assertEquals("Zip first", readRepairArchive(zipped)?.manifest?.optJSONObject("repair")?.optString("title"))
        assertNull(readLegacyJsonRepairArchive("""{"format":"OTHER"}""".toByteArray()))
    }

    private fun sampleRepair() = RepairProject(
        title = "Tylna zwrotnica lewa",
        area = VehicleArea.Suspension,
        vehicleName = "BMW E61 520d",
        status = "W trakcie",
        priority = "Wysoki",
        problemDescription = "Zapieczona sruba",
        goal = "Naprawa",
        checklist = emptyList(),
        partsToIdentify = emptyList(),
        documentsToCollect = emptyList(),
        id = "repair_rear_knuckle"
    )

    private fun sampleShoppingItem() = ShoppingListItem(
        id = "shopping_1",
        partNumber = "33326763092",
        manufacturerPartNumber = "LEM-123",
        name = "Tuleja wahacza",
        manufacturer = "Lemforder",
        repairTitle = "Tylna zwrotnica lewa",
        repairId = "repair_rear_knuckle",
        area = VehicleArea.Suspension,
        quantity = 2,
        source = "Autodoc",
        price = "249.99",
        imageUri = "content://images/1"
    )

    private fun sampleTorqueSpec() = TorqueSpec(
        component = "Sruba zwrotnicy",
        torque = "100 Nm + 90 deg",
        source = "TIS",
        notes = "Na obciazonym aucie"
    )
}
