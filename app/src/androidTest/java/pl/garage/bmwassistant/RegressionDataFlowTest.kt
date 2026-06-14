package pl.garage.bmwassistant

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pl.garage.bmwassistant.data.PartInventoryStorage
import pl.garage.bmwassistant.data.RepairProjectStorage
import pl.garage.bmwassistant.data.VehicleStorage
import pl.garage.bmwassistant.database.catalog.VehicleDatabaseManager
import pl.garage.bmwassistant.database.migration.LegacyStorageRoomMigrator
import pl.garage.bmwassistant.database.repository.GarageRepository
import pl.garage.bmwassistant.database.repository.VehicleDataSnapshot
import pl.garage.bmwassistant.model.PartInventoryItem
import pl.garage.bmwassistant.model.PersonalDocumentationItem
import pl.garage.bmwassistant.model.PersonalDocumentationItemType
import pl.garage.bmwassistant.model.REPAIR_STATUS_FINISHED
import pl.garage.bmwassistant.model.REPAIR_STATUS_IN_PROGRESS
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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(AndroidJUnit4::class)
class RegressionDataFlowTest {

    private lateinit var context: Context
    private lateinit var repository: GarageRepository
    private lateinit var vehicleStorage: VehicleStorage
    private lateinit var repairStorage: RepairProjectStorage
    private lateinit var partStorage: PartInventoryStorage

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        clearAppData()
        repository = GarageRepository(context)
        vehicleStorage = VehicleStorage(context)
        repairStorage = RepairProjectStorage(context)
        partStorage = PartInventoryStorage(context)
    }

    @After
    fun tearDown() {
        clearAppData()
    }

    @Test
    fun vehicleBackupExportAndImportRestoresSnapshotAndFiles() = runBlocking {
        val vehicle = repository.saveVehicle(
            sampleVehicle(
                id = "vehicle_backup_regression",
                vin = "WBATESTBACKUP001"
            )
        )
        val snapshot = sampleSnapshot(vehicle)
        repository.saveVehicleSnapshot(vehicle.id, snapshot)

        val vehicleFilesDirectory = VehicleDatabaseManager(context).createDescriptor(vehicle.id).filesDirectory
        val exportedFile = File(vehicleFilesDirectory, "docs/inspection.txt").apply {
            parentFile?.mkdirs()
            writeText("kontrola tylnej zwrotnicy")
        }

        val backupBytes = ByteArrayOutputStream().use { output ->
            val exported = repository.exportVehicleBackup(vehicle, output)
            assertTrue(exported)
            output.toByteArray()
        }

        repository.deleteVehicle(vehicle.id)

        val importedVehicle = repository.importVehicleBackup(ByteArrayInputStream(backupBytes))
        assertNotNull(importedVehicle)

        val restoredVehicle = importedVehicle!!
        val restoredSnapshot = repository.loadVehicleSnapshot(restoredVehicle)

        assertEquals(vehicle.copy(id = restoredVehicle.id), restoredVehicle)
        assertSnapshotEquals(snapshot, restoredSnapshot)

        val restoredFilesDirectory = VehicleDatabaseManager(context).createDescriptor(restoredVehicle.id).filesDirectory
        val restoredFile = File(restoredFilesDirectory, exportedFile.relativeTo(vehicleFilesDirectory).path)
        assertTrue(restoredFile.exists())
        assertEquals("kontrola tylnej zwrotnicy", restoredFile.readText())
    }

    @Test
    fun legacyMigrationMovesDataIntoRoomOnlyOnce() = runBlocking {
        val vehicle = sampleVehicle(
            id = "vehicle_legacy_regression",
            vin = "WBALEGACY001"
        )
        val snapshot = sampleSnapshot(vehicle)

        vehicleStorage.saveVehicles(listOf(vehicle))
        repairStorage.saveRepairs(vehicle, snapshot.repairs)
        repairStorage.saveDocumentation(vehicle, snapshot.documentation)
        partStorage.saveShoppingList(vehicle, snapshot.shoppingList)
        partStorage.saveParts(vehicle, snapshot.inventoryParts)

        val migrator = LegacyStorageRoomMigrator(context)
        val firstResult = migrator.migrateIfNeeded()

        assertTrue(firstResult.migrated)
        assertEquals(1, firstResult.migratedVehicles)
        assertTrue(migrator.isMigrationComplete())

        val migratedVehicle = repository.loadVehicles().single()
        assertEquals(vehicle, migratedVehicle)
        assertSnapshotEquals(snapshot, repository.loadVehicleSnapshot(migratedVehicle))

        val secondResult = migrator.migrateIfNeeded()
        assertFalse(secondResult.migrated)
        assertEquals(0, secondResult.migratedVehicles)
    }

    @Test
    fun repairArchiveExportAndImportPreservesDocumentationAndArchivedShopping() = runBlocking {
        val vehicle = repository.saveVehicle(
            sampleVehicle(
                id = "vehicle_archive_regression",
                vin = "WBAARCHIVE001"
            )
        )
        val snapshot = sampleSnapshot(vehicle)
        val repair = snapshot.repairs.first()
        val documentation = snapshot.documentation.first()
        val shoppingItems = snapshot.shoppingList

        val rawArchive = repository.createRepairArchiveExport(
            vehicle = vehicle,
            repair = repair,
            documentation = documentation,
            shoppingItems = shoppingItems
        )

        val imported = repository.importRepairArchive(
            vehicle = vehicle,
            rawArchive = rawArchive,
            importAsArchived = true
        )

        assertNotNull(imported)
        assertEquals(repair.title, imported?.repair?.title)
        assertEquals(REPAIR_STATUS_FINISHED, imported?.repair?.status)
        assertTrue(imported?.shoppingList?.isEmpty() == true)
        assertEquals(shoppingItems.map { it.name }, imported?.documentation?.archivedShoppingList?.map { it.name })
        assertEquals(documentation.tisDocuments.map { it.url }, imported?.documentation?.tisDocuments?.map { it.url })
        assertEquals(documentation.youtubeVideos.map { it.url }, imported?.documentation?.youtubeVideos?.map { it.url })
    }

    private fun assertSnapshotEquals(
        expected: VehicleDataSnapshot,
        actual: VehicleDataSnapshot,
    ) {
        assertEquals(expected.repairs, actual.repairs)
        assertEquals(expected.documentation, actual.documentation)
        assertEquals(expected.shoppingList, actual.shoppingList)
        assertEquals(expected.inventoryParts, actual.inventoryParts)
    }

    private fun sampleVehicle(
        id: String,
        vin: String,
    ): Vehicle = Vehicle(
        brand = "BMW",
        model = "520d",
        generation = "E61",
        engine = "M47N2",
        year = "2007",
        vin = vin,
        mileage = "286000",
        note = "Scenariusz regresyjny",
        id = id,
        partsCatalogUrl = "https://czescidobmw.pl/test"
    )

    private fun sampleSnapshot(vehicle: Vehicle): VehicleDataSnapshot {
        val repair = RepairProject(
            title = "Tylna zwrotnica lewa",
            area = VehicleArea.Suspension,
            vehicleName = vehicle.displayName,
            status = REPAIR_STATUS_IN_PROGRESS,
            priority = "Wysoki",
            problemDescription = "Zapieczona sruba i luz na tulei",
            goal = "Rozebrac zwrotnice i przygotowac komplet czesci",
            checklist = listOf("Potwierdzic luz i korozje", "Spisac komplet czesci"),
            partsToIdentify = listOf("Sruba mimozrodowa", "Tuleja wahacza"),
            documentsToCollect = listOf("TIS wymiany zwrotnicy"),
            checkpoints = listOf(
                RepairCheckpoint(
                    id = "checkpoint-diagnosis",
                    text = "Potwierdzic luz i korozje",
                    isDone = true
                ),
                RepairCheckpoint(
                    id = "checkpoint-parts",
                    text = "Spisac komplet czesci",
                    isDone = false
                )
            ),
            id = "repair_rear_knuckle"
        )

        val shoppingItem = ShoppingListItem(
            id = "shopping_rear_knuckle_1",
            partNumber = "33326763092",
            manufacturerPartNumber = "LEM-123",
            name = "Tuleja wahacza",
            manufacturer = "Lemforder",
            repairTitle = repair.title,
            repairId = repair.id,
            area = repair.area,
            quantity = 2,
            source = "Autodoc",
            price = "249.99",
            shopUrl = "https://example.com/tuleja",
            realOemUrl = "https://czescidobmw.pl/oem/33326763092"
        )

        val documentation = RepairDocumentation(
            title = "Dokumentacja tylnej zwrotnicy",
            area = repair.area,
            repairTitle = repair.title,
            summary = "Komplet danych przed rozbiorka",
            archivedShoppingList = listOf(
                shoppingItem.copy(
                    id = "archived_rear_knuckle_1",
                    source = "Magazyn"
                )
            ),
            tisLinks = listOf("https://tis.example/demontaz"),
            tisDocuments = listOf(
                TisDocumentationLink(
                    title = "TIS Demontaz",
                    url = "https://tis.example/demontaz"
                )
            ),
            torqueSpecs = listOf(
                TorqueSpec(
                    component = "Sruba zwrotnicy",
                    torque = "100 Nm + 90 deg",
                    source = "TIS",
                    notes = "Dokrecic na obciazonym aucie"
                )
            ),
            torqueDiagramImageUri = "content://diagram/rear-knuckle",
            torqueDiagramAssignments = listOf(
                TorqueDiagramAssignment(
                    torqueSpecIndex = 0,
                    xRatio = 0.35f,
                    yRatio = 0.72f
                )
            ),
            torqueTables = listOf(
                TorqueSpecTable(
                    id = "torque_table_rear_knuckle",
                    title = "Tabela momentow zwrotnicy",
                    torqueSpecs = listOf(
                        TorqueSpec(
                            component = "Sruba zwrotnicy",
                            torque = "100 Nm + 90 deg",
                            source = "TIS",
                            notes = "Dokrecic na obciazonym aucie"
                        )
                    ),
                    diagramImageUri = "content://diagram/rear-knuckle",
                    diagramAssignments = listOf(
                        TorqueDiagramAssignment(
                            torqueSpecIndex = 0,
                            xRatio = 0.35f,
                            yRatio = 0.72f
                        )
                    )
                )
            ),
            youtubeLinks = listOf("https://youtube.com/watch?v=test"),
            youtubeVideos = listOf(
                YoutubeVideo(
                    title = "Instrukcja wymiany",
                    url = "https://youtube.com/watch?v=test",
                    note = "Uwaga na zapieczona srube"
                )
            ),
            personalNotes = listOf(
                PersonalDocumentationItem(
                    id = "note_rear_knuckle",
                    type = PersonalDocumentationItemType.Text,
                    title = "Notatka mechanika",
                    text = "Najpierw przygotowac palnik i penetrant"
                )
            ),
            userNotes = "Zamowic nowe sruby samokontrujace",
            repairId = repair.id
        )

        val finishedRepair = RepairProject(
            title = "Wymiana filtra oleju",
            area = VehicleArea.Service,
            vehicleName = vehicle.displayName,
            status = REPAIR_STATUS_FINISHED,
            priority = "Normalny",
            problemDescription = "Przeglad po zakupie",
            goal = "Wymienic materialy eksploatacyjne",
            checklist = listOf("Wymienic filtr oleju"),
            partsToIdentify = emptyList(),
            documentsToCollect = emptyList(),
            id = "repair_service_oil_filter"
        )

        val inventoryPart = PartInventoryItem(
            id = "inventory_part_1",
            oemPartNumber = "11428575211",
            manufacturerPartNumber = "MANN-HU816X",
            name = "Filtr oleju",
            manufacturer = "Mann",
            repairTitle = finishedRepair.title,
            quantity = 1,
            purchasePrice = "42.50",
            realOemUrl = "https://czescidobmw.pl/oem/11428575211",
            photoUri = null,
            repairId = finishedRepair.id
        )

        return VehicleDataSnapshot(
            repairs = listOf(repair, finishedRepair),
            documentation = listOf(documentation),
            shoppingList = listOf(shoppingItem),
            inventoryParts = listOf(inventoryPart)
        )
    }

    private fun clearAppData() {
        runBlocking {
            val cleanupRepository = GarageRepository(context)
            cleanupRepository.loadVehicles().forEach { vehicle ->
                cleanupRepository.deleteVehicle(vehicle.id)
            }
        }
        context.deleteSharedPreferences("garage_data")
        context.deleteSharedPreferences("garage_repair_projects")
        context.deleteSharedPreferences("garage_parts_data")
        context.deleteSharedPreferences("garage_database_migration")
        context.filesDir.resolve("vehicles").deleteRecursively()
        context.filesDir.resolve("imported_repair_archives").deleteRecursively()
        context.cacheDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("vehicle-import-")) {
                file.deleteRecursively()
            }
        }
    }
}
