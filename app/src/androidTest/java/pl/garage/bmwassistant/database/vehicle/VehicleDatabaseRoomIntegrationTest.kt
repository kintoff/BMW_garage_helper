package pl.garage.bmwassistant.database.vehicle

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
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

@RunWith(AndroidJUnit4::class)
class VehicleDatabaseRoomIntegrationTest {

    private lateinit var database: VehicleDatabase
    private lateinit var repairDao: RepairProjectDao
    private lateinit var documentationDao: RepairDocumentationDao
    private lateinit var shoppingDao: ShoppingListDao
    private lateinit var inventoryDao: InventoryPartDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, VehicleDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repairDao = database.repairProjectDao()
        documentationDao = database.repairDocumentationDao()
        shoppingDao = database.shoppingListDao()
        inventoryDao = database.inventoryPartDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun replaceRepairWithCheckpointsReplacesChildCollections() = runBlocking {
        val repairId = "repair_suspension_left_rear"
        repairDao.replaceRepairWithCheckpoints(
            repair = sampleRepair(repairId).toEntity(
                createdAtEpochMillis = 1L,
                updatedAtEpochMillis = 2L,
                sortOrder = 0
            ),
            checkpoints = listOf(
                RepairCheckpoint("cp_old_1", "Old checkpoint", false).toEntity(repairId, 0),
                RepairCheckpoint("cp_old_2", "Second old checkpoint", true).toEntity(repairId, 1)
            ),
            partsToIdentify = listOf(
                RepairPartsToIdentifyEntity("part_old_1", repairId, "Old part", 0)
            ),
            documentsToCollect = listOf(
                RepairDocumentsToCollectEntity("doc_old_1", repairId, "Old doc", 0)
            )
        )

        repairDao.replaceRepairWithCheckpoints(
            repair = sampleRepair(repairId).copy(priority = "Pilny").toEntity(
                createdAtEpochMillis = 3L,
                updatedAtEpochMillis = 4L,
                sortOrder = 0
            ),
            checkpoints = listOf(
                RepairCheckpoint("cp_new_1", "New checkpoint", true).toEntity(repairId, 0)
            ),
            partsToIdentify = listOf(
                RepairPartsToIdentifyEntity("part_new_1", repairId, "New part", 0),
                RepairPartsToIdentifyEntity("part_new_2", repairId, "Second new part", 1)
            ),
            documentsToCollect = emptyList()
        )

        val storedRepair = repairDao.getRepairById(repairId)
        val checkpoints = repairDao.getCheckpointsForRepair(repairId)
        val parts = repairDao.getPartsToIdentifyForRepair(repairId)
        val documents = repairDao.getDocumentsToCollectForRepair(repairId)

        assertEquals("Pilny", storedRepair?.priority)
        assertEquals(listOf("New checkpoint"), checkpoints.map { it.text })
        assertEquals(listOf("New part", "Second new part"), parts.map { it.text })
        assertEquals(emptyList<String>(), documents.map { it.text })
    }

    @Test
    fun replaceDocumentationBundleReplacesNestedRelations() = runBlocking {
        val repairId = "repair_docs_1"
        val repair = sampleRepair(repairId)
        repairDao.insertRepair(
            repair.toEntity(
                createdAtEpochMillis = 10L,
                updatedAtEpochMillis = 20L,
                sortOrder = 0
            )
        )

        val firstDocumentationId = "repair_docs_1_documentation"
        documentationDao.replaceDocumentationBundle(
            documentation = sampleDocumentation(repair).toEntity(
                documentationId = firstDocumentationId,
                createdAtEpochMillis = 30L,
                updatedAtEpochMillis = 40L
            ),
            archivedShoppingItems = listOf(
                sampleShoppingItem(repair).copy(id = "archived_old").toArchivedEntity(firstDocumentationId, 0)
            ),
            tisLinks = listOf(
                TisDocumentationLink("Old TIS", "https://example.com/old").toEntity(firstDocumentationId, 0)
            ),
            youtubeVideos = listOf(
                YoutubeVideo("Old Video", "https://example.com/video-old").toEntity(firstDocumentationId, 0)
            ),
            personalItems = listOf(
                PersonalDocumentationItem(
                    id = "personal_old",
                    type = PersonalDocumentationItemType.Text,
                    title = "Old note",
                    text = "Old body"
                ).toEntity(firstDocumentationId, 0, 50L)
            ),
            torqueTables = listOf(
                TorqueSpecTableEntity(
                    tableId = "table_old",
                    documentationId = firstDocumentationId,
                    title = "Old table",
                    sortOrder = 0
                )
            ),
            torqueSpecs = listOf(
                TorqueSpecEntity(
                    torqueSpecId = "table_old_spec_0",
                    tableId = "table_old",
                    component = "Old bolt",
                    type = "",
                    thread = "",
                    tighteningSpecifications = "",
                    torque = "50 Nm",
                    source = "TIS",
                    notes = "",
                    sortOrder = 0
                )
            ),
            torqueAssignments = listOf(
                TorqueDiagramAssignmentEntity(
                    assignmentId = "table_old_assignment_0",
                    tableId = "table_old",
                    torqueSpecId = "table_old_spec_0",
                    xRatio = 0.1f,
                    yRatio = 0.2f,
                    sortOrder = 0
                )
            )
        )

        val replacementDocumentation = sampleDocumentation(repair).copy(
            title = "Aktualna dokumentacja",
            summary = "Nowy pakiet danych"
        )
        val replacementDocumentationId = firstDocumentationId
        val replacementTable = replacementDocumentation.torqueTables.single()
        val replacementSpec = replacementTable.torqueSpecs.single().toEntity(replacementTable.id, 0)

        documentationDao.replaceDocumentationBundle(
            documentation = replacementDocumentation.toEntity(
                documentationId = replacementDocumentationId,
                createdAtEpochMillis = 60L,
                updatedAtEpochMillis = 70L
            ),
            archivedShoppingItems = replacementDocumentation.archivedShoppingList.mapIndexed { index, item ->
                item.toArchivedEntity(replacementDocumentationId, index)
            },
            tisLinks = replacementDocumentation.tisDocuments.mapIndexed { index, item ->
                item.toEntity(replacementDocumentationId, index)
            },
            youtubeVideos = replacementDocumentation.youtubeVideos.mapIndexed { index, item ->
                item.toEntity(replacementDocumentationId, index)
            },
            personalItems = replacementDocumentation.personalNotes.mapIndexed { index, item ->
                item.toEntity(replacementDocumentationId, index, 80L + index)
            },
            torqueTables = replacementDocumentation.torqueTables.mapIndexed { index, table ->
                table.toEntity(replacementDocumentationId, index)
            },
            torqueSpecs = replacementDocumentation.torqueTables.flatMap { table ->
                table.torqueSpecs.mapIndexed { index, spec -> spec.toEntity(table.id, index) }
            },
            torqueAssignments = replacementDocumentation.torqueTables.flatMap { table ->
                val specIds = table.torqueSpecs.mapIndexed { index, spec -> spec.toEntity(table.id, index).torqueSpecId }
                table.diagramAssignments.mapIndexed { index, assignment ->
                    assignment.toEntity(table.id, specIds[assignment.torqueSpecIndex], index)
                }
            }
        )

        val storedDocumentation = documentationDao.getDocumentationForRepair(repairId)
        val archivedShopping = documentationDao.getArchivedShoppingItems(replacementDocumentationId)
        val tisLinks = documentationDao.getTisLinks(replacementDocumentationId)
        val youtubeVideos = documentationDao.getYoutubeVideos(replacementDocumentationId)
        val personalItems = documentationDao.getPersonalItems(replacementDocumentationId)
        val tables = documentationDao.getTorqueTables(replacementDocumentationId)
        val specs = documentationDao.getTorqueSpecs(replacementTable.id)
        val assignments = documentationDao.getTorqueAssignments(replacementTable.id)

        assertEquals("Aktualna dokumentacja", storedDocumentation?.title)
        assertEquals(listOf("Tuleja wahacza"), archivedShopping.map { it.name })
        assertEquals(listOf("TIS Rear Knuckle"), tisLinks.map { it.title })
        assertEquals(listOf("Rear knuckle walkthrough"), youtubeVideos.map { it.title })
        assertEquals(listOf("Uwagi warsztatowe"), personalItems.map { it.title })
        assertEquals(listOf("Tabela momentow"), tables.map { it.title })
        assertEquals(listOf(replacementSpec.component), specs.map { it.component })
        assertEquals(listOf(replacementSpec.torqueSpecId), assignments.map { it.torqueSpecId })
    }

    @Test
    fun deletingRepairCascadesDocumentationAndNullsInventoryRepairReference() = runBlocking {
        val repair = sampleRepair("repair_delete_1")
        val repairEntity = repair.toEntity(
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 2L,
            sortOrder = 0
        )
        repairDao.insertRepair(repairEntity)

        val documentationId = "repair_delete_1_docs"
        documentationDao.insertDocumentation(
            sampleDocumentation(repair).toEntity(
                documentationId = documentationId,
                createdAtEpochMillis = 3L,
                updatedAtEpochMillis = 4L
            )
        )
        shoppingDao.insert(
            sampleShoppingItem(repair).toEntity(
                createdAtEpochMillis = 5L,
                updatedAtEpochMillis = 6L
            )
        )
        inventoryDao.insert(
            sampleInventoryItem(repair).toEntity(
                createdAtEpochMillis = 7L,
                updatedAtEpochMillis = 8L
            )
        )

        repairDao.deleteRepair(repair.id)

        assertEquals(emptyList<RepairProjectEntity>(), repairDao.getAllRepairs())
        assertNull(documentationDao.getDocumentationForRepair(repair.id))
        assertEquals(emptyList<ShoppingListItemEntity>(), shoppingDao.getItemsForRepair(repair.id))
        assertEquals(listOf(null), inventoryDao.getAllParts().map { it.repairId })
    }

    private fun sampleRepair(repairId: String): RepairProject = RepairProject(
        title = "Rear knuckle left",
        area = VehicleArea.Suspension,
        vehicleName = "BMW 520d E61",
        status = "W trakcie",
        priority = "Normalny",
        problemDescription = "Rust on the lower bolt",
        goal = "Prepare a complete repair set",
        checklist = emptyList(),
        partsToIdentify = emptyList(),
        documentsToCollect = emptyList(),
        id = repairId
    )

    private fun sampleDocumentation(repair: RepairProject): RepairDocumentation =
        RepairDocumentation(
            title = "Rear knuckle documentation",
            area = repair.area,
            repairTitle = repair.title,
            summary = "Baseline documentation",
            archivedShoppingList = listOf(sampleShoppingItem(repair).copy(id = "archived_shopping_1")),
            tisDocuments = listOf(
                TisDocumentationLink(
                    title = "TIS Rear Knuckle",
                    url = "https://example.com/tis/rear-knuckle"
                )
            ),
            torqueTables = listOf(
                TorqueSpecTable(
                    id = "torque_table_1",
                    title = "Tabela momentow",
                    torqueSpecs = listOf(
                        TorqueSpec(
                            component = "Lower control arm bolt",
                            type = "",
                            thread = "M12",
                            tighteningSpecifications = "new bolt",
                            torque = "100 Nm + 90 deg",
                            source = "TIS",
                            notes = "Load the suspension first"
                        )
                    ),
                    diagramImageUri = "content://diagram/rear-knuckle",
                    diagramAssignments = listOf(
                        TorqueDiagramAssignment(
                            torqueSpecIndex = 0,
                            xRatio = 0.4f,
                            yRatio = 0.6f
                        )
                    )
                )
            ),
            youtubeVideos = listOf(
                YoutubeVideo(
                    title = "Rear knuckle walkthrough",
                    url = "https://example.com/video/rear-knuckle",
                    note = "Watch the bolt removal section"
                )
            ),
            personalNotes = listOf(
                PersonalDocumentationItem(
                    id = "personal_note_1",
                    type = PersonalDocumentationItemType.Text,
                    title = "Uwagi warsztatowe",
                    text = "Prepare heat and penetrant"
                )
            ),
            userNotes = "Order fresh self-locking nuts",
            repairId = repair.id
        )

    private fun sampleShoppingItem(repair: RepairProject): ShoppingListItem =
        ShoppingListItem(
            id = "shopping_item_1",
            partNumber = "33326763092",
            manufacturerPartNumber = "LEM-123",
            name = "Tuleja wahacza",
            manufacturer = "Lemforder",
            repairTitle = repair.title,
            repairId = repair.id,
            area = repair.area,
            quantity = 2,
            source = "Autodoc",
            price = "249.99"
        )

    private fun sampleInventoryItem(repair: RepairProject): PartInventoryItem =
        PartInventoryItem(
            id = "inventory_item_1",
            oemPartNumber = "33326763092",
            manufacturerPartNumber = "LEM-123",
            name = "Tuleja wahacza",
            manufacturer = "Lemforder",
            repairTitle = repair.title,
            quantity = 1,
            purchasePrice = "249.99",
            realOemUrl = null,
            photoUri = null,
            repairId = repair.id
        )
}
