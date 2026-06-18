package pl.garage.bmwassistant.database.vehicle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.garage.bmwassistant.model.PartInventoryItem
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

class VehicleEntityMappersTest {

    @Test
    fun repairProjectRoundTripPreservesCoreFields() {
        val repair = sampleRepair()

        val entity = repair.toEntity(
            createdAtEpochMillis = 100L,
            updatedAtEpochMillis = 200L,
            completedAtEpochMillis = 300L,
            sortOrder = 4,
            isArchived = true
        )
        val restored = entity.toModel(
            vehicleName = repair.vehicleName,
            checkpoints = repair.checkpoints,
            partsToIdentify = repair.partsToIdentify,
            documentsToCollect = repair.documentsToCollect
        )

        assertEquals(repair.id, entity.repairId)
        assertEquals(VehicleArea.Suspension.name, entity.area)
        assertEquals(300L, entity.completedAtEpochMillis)
        assertTrue(entity.isArchived)
        assertEquals(repair, restored)
    }

    @Test
    fun repairDocumentationRoundTripRestoresNestedCollections() {
        val documentation = sampleDocumentation()
        val torqueTable = sampleTorqueTable()
        val archivedShopping = listOf(sampleShoppingItem())
        val tisDocuments = listOf(TisDocumentationLink("TIS tyl", "https://tis.example/1"))
        val youtubeVideos = listOf(YoutubeVideo("Instrukcja", "https://youtube.com/watch?v=abc123xyz89", "warto obejrzec"))
        val notes = listOf(
            PersonalDocumentationItem(
                id = "note_1",
                type = PersonalDocumentationItemType.Photo,
                title = "Foto",
                uri = "content://photo/1"
            )
        )

        val entity = documentation.toEntity(
            documentationId = "doc_1",
            createdAtEpochMillis = 10L,
            updatedAtEpochMillis = 20L
        )
        val restored = entity.toModel(
            archivedShoppingList = archivedShopping,
            tisDocuments = tisDocuments,
            torqueTables = listOf(torqueTable),
            youtubeVideos = youtubeVideos,
            personalNotes = notes
        )

        assertEquals("doc_1", entity.documentationId)
        assertEquals(documentation.repairId, entity.repairId)
        assertEquals(documentation.title, restored.title)
        assertEquals(archivedShopping, restored.archivedShoppingList)
        assertEquals(tisDocuments, restored.tisDocuments)
        assertEquals(listOf(torqueTable), restored.torqueTables)
        assertEquals(youtubeVideos, restored.youtubeVideos)
        assertEquals(notes, restored.personalNotes)
    }

    @Test
    fun shoppingAndArchivedShoppingMappingsPreserveRepairContext() {
        val item = sampleShoppingItem()

        val shoppingEntity = item.toEntity(
            createdAtEpochMillis = 100L,
            updatedAtEpochMillis = 200L,
            status = "received",
            archivedInDocumentation = true
        )
        val archivedEntity = item.toArchivedEntity(
            documentationId = "doc_1",
            sortOrder = 3
        )

        val shoppingRestored = shoppingEntity.toModel(repairTitle = item.repairTitle)
        val archivedRestored = archivedEntity.toModel(repairTitle = item.repairTitle)

        assertEquals("received", shoppingEntity.status)
        assertTrue(shoppingEntity.archivedInDocumentation)
        assertEquals(item, shoppingRestored)
        assertEquals(item, archivedRestored)
    }

    @Test
    fun inventoryPartRoundTripPreservesOptionalRelations() {
        val part = PartInventoryItem(
            id = "inventory_1",
            oemPartNumber = "11428575211",
            manufacturerPartNumber = "MANN-HU816X",
            name = "Filtr oleju",
            manufacturer = "Mann",
            repairTitle = "Wymiana filtra oleju",
            quantity = 1,
            purchasePrice = "42.50",
            realOemUrl = "https://example.com/oem/11428575211",
            photoUri = "content://photo/filter",
            repairId = "repair_filter",
            originShoppingItemId = "shopping_1",
            locationNote = "Polka A",
            createdAtEpochMillis = 100L,
            updatedAtEpochMillis = 200L
        )

        val entity = part.toEntity(
            createdAtEpochMillis = 100L,
            updatedAtEpochMillis = 200L,
            originShoppingItemId = "shopping_1",
            locationNote = "Polka A"
        )
        val restored = entity.toModel(repairTitle = part.repairTitle)
        val historyEvent = entity.initialHistoryEvent()

        assertEquals("shopping_1", entity.originShoppingItemId)
        assertEquals("Polka A", entity.locationNote)
        assertEquals("${part.id}_created", historyEvent.eventId)
        assertEquals(part.id, historyEvent.inventoryPartId)
        assertEquals(INVENTORY_HISTORY_ACCEPTED_FROM_SHOPPING, historyEvent.eventType)
        assertEquals("Przyjęto do magazynu", historyEvent.title)
        assertEquals(1, historyEvent.quantityDelta)
        assertEquals(1, historyEvent.quantityAfter)
        assertEquals("Lokalizacja: Polka A", historyEvent.note)
        assertEquals(100L, historyEvent.createdAtEpochMillis)
        assertEquals(part, restored)
    }

    @Test
    fun torqueMappingsBuildStableIdsAndRestoreSpecIndex() {
        val table = sampleTorqueTable()
        val spec = sampleTorqueSpec()
        val assignment = TorqueDiagramAssignment(
            torqueSpecIndex = 2,
            xRatio = 0.25f,
            yRatio = 0.75f
        )

        val tableEntity = table.toEntity(documentationId = "doc_1", sortOrder = 5)
        val specEntity = spec.toEntity(tableId = table.id, sortOrder = 7)
        val assignmentEntity = assignment.toEntity(
            tableId = table.id,
            torqueSpecId = specEntity.torqueSpecId,
            sortOrder = 9
        )

        assertEquals("${table.id}_spec_7", specEntity.torqueSpecId)
        assertEquals("${table.id}_assignment_9", assignmentEntity.assignmentId)
        assertEquals(table, tableEntity.toModel(specs = table.torqueSpecs, assignments = table.diagramAssignments))
        assertEquals(spec, specEntity.toModel())
        assertEquals(assignment, assignmentEntity.toModel(specIndex = 2))
    }

    @Test
    fun personalItemTisAndYoutubeMappingsRoundTrip() {
        val personal = PersonalDocumentationItem(
            id = "note_1",
            type = PersonalDocumentationItemType.Link,
            title = "Forum",
            text = "",
            uri = null,
            url = "https://forum.example"
        )
        val tis = TisDocumentationLink("TIS 1", "https://tis.example/1")
        val video = YoutubeVideo("Film", "https://youtube.com/watch?v=abc123xyz89", "Notatka")

        assertEquals(personal, personal.toEntity("doc_1", 1, 100L).toModel())
        assertEquals(tis, tis.toEntity("doc_1", 2).toModel())
        assertEquals(video, video.toEntity("doc_1", 3).toModel())
    }

    @Test
    fun mapperFallbacksHandleUnknownAreaAndUnknownPersonalType() {
        val documentation = RepairDocumentationEntity(
            documentationId = "doc_1",
            repairId = "repair_1",
            title = "Dokumentacja",
            area = "UNKNOWN",
            repairTitleSnapshot = "Naprawa",
            summary = "opis",
            userNotes = "uwagi",
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 2L
        ).toModel()
        val shopping = ShoppingListItemEntity(
            shoppingItemId = "shopping_1",
            repairId = "repair_1",
            partNumber = "33326763092",
            manufacturerPartNumber = "",
            name = "Tuleja",
            manufacturer = "",
            quantity = 1,
            source = "Autodoc",
            price = "249.99",
            area = "UNKNOWN",
            status = "planned",
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 2L
        ).toModel(repairTitle = "Naprawa")
        val personal = PersonalDocumentationItemEntity(
            itemId = "note_1",
            documentationId = "doc_1",
            type = "UNKNOWN",
            title = "Uwagi",
            text = "tekst",
            sortOrder = 0,
            createdAtEpochMillis = 1L
        ).toModel()

        assertEquals(VehicleArea.Service, documentation.area)
        assertEquals(VehicleArea.Service, shopping.area)
        assertEquals(PersonalDocumentationItemType.Text, personal.type)
    }

    @Test
    fun checkpointMapperAndAreaHelperRemainStable() {
        val checkpoint = RepairCheckpoint(
            id = "cp_1",
            text = "Dokręć śruby",
            isDone = true
        )
        val entity = checkpoint.toEntity(repairId = "repair_1", sortOrder = 4)

        assertEquals(checkpoint, entity.toModel())
        assertEquals(VehicleArea.Engine, "Engine".toVehicleArea())
        assertEquals(VehicleArea.Service, "BAD_VALUE".toVehicleArea())
    }

    @Test
    fun simpleRepairChildEntitiesKeepProvidedValues() {
        val partsToIdentify = RepairPartsToIdentifyEntity(
            itemId = "part_1",
            repairId = "repair_1",
            text = "Śruba mimośrodowa",
            sortOrder = 2
        )
        val documentsToCollect = RepairDocumentsToCollectEntity(
            itemId = "doc_1",
            repairId = "repair_1",
            text = "TIS tylnej osi",
            sortOrder = 3
        )

        assertEquals("repair_1", partsToIdentify.repairId)
        assertEquals("Śruba mimośrodowa", partsToIdentify.text)
        assertEquals(3, documentsToCollect.sortOrder)
    }

    private fun sampleRepair() = RepairProject(
        title = "Tylna zwrotnica lewa",
        area = VehicleArea.Suspension,
        vehicleName = "BMW E61 520d",
        status = "W trakcie",
        priority = "Wysoki",
        problemDescription = "Zapieczona sruba",
        goal = "Naprawa",
        checklist = listOf("Podnies auto"),
        partsToIdentify = listOf("Sruba mimozrodowa"),
        documentsToCollect = listOf("TIS"),
        checkpoints = listOf(RepairCheckpoint("cp1", "Podnies auto", true)),
        id = "repair_rear_knuckle"
    )

    private fun sampleDocumentation() = RepairDocumentation(
        title = "Dokumentacja: Tylna zwrotnica lewa",
        area = VehicleArea.Suspension,
        repairTitle = "Tylna zwrotnica lewa",
        summary = "opis",
        userNotes = "uwagi",
        repairId = "repair_rear_knuckle"
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
        imageUri = "content://images/1",
        shopUrl = "https://shop.example/1",
        realOemUrl = "https://oem.example/1"
    )

    private fun sampleTorqueSpec() = TorqueSpec(
        component = "Sruba zwrotnicy",
        type = "M12",
        thread = "1.5",
        tighteningSpecifications = "100 Nm + 90 deg",
        torque = "100 Nm",
        source = "TIS",
        notes = "Na obciazonym aucie"
    )

    private fun sampleTorqueTable() = TorqueSpecTable(
        id = "table_1",
        title = "Tabela tyl",
        torqueSpecs = listOf(sampleTorqueSpec()),
        diagramImageUri = "content://diagram/1",
        diagramAssignments = listOf(TorqueDiagramAssignment(0, 0.4f, 0.6f))
    )
}
