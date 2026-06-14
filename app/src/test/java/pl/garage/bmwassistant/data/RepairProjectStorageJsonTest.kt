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
