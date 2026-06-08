package pl.garage.bmwassistant.database.repository

import android.content.Context
import androidx.room.withTransaction
import pl.garage.bmwassistant.data.ImportedRepairArchive
import pl.garage.bmwassistant.data.RepairProjectStorage
import pl.garage.bmwassistant.database.catalog.VehicleCatalogEntity
import pl.garage.bmwassistant.database.catalog.VehicleDatabaseManager
import pl.garage.bmwassistant.database.catalog.toVehicle
import pl.garage.bmwassistant.database.vehicle.PersonalDocumentationItemEntity
import pl.garage.bmwassistant.database.vehicle.RepairCheckpointEntity
import pl.garage.bmwassistant.database.vehicle.RepairDocumentationEntity
import pl.garage.bmwassistant.database.vehicle.RepairDocumentsToCollectEntity
import pl.garage.bmwassistant.database.vehicle.RepairPartsToIdentifyEntity
import pl.garage.bmwassistant.database.vehicle.TorqueDiagramAssignmentEntity
import pl.garage.bmwassistant.database.vehicle.TorqueSpecEntity
import pl.garage.bmwassistant.database.vehicle.TorqueSpecTableEntity
import pl.garage.bmwassistant.database.vehicle.VehicleDatabase
import pl.garage.bmwassistant.database.vehicle.toArchivedEntity
import pl.garage.bmwassistant.database.vehicle.toEntity
import pl.garage.bmwassistant.database.vehicle.toModel
import pl.garage.bmwassistant.database.vehicle.toVehicleArea
import pl.garage.bmwassistant.model.PartInventoryItem
import pl.garage.bmwassistant.model.RepairDocumentation
import pl.garage.bmwassistant.model.RepairProject
import pl.garage.bmwassistant.model.ShoppingListItem
import pl.garage.bmwassistant.model.TisDocumentationLink
import pl.garage.bmwassistant.model.TorqueSpecTable
import pl.garage.bmwassistant.model.Vehicle
import pl.garage.bmwassistant.model.YoutubeVideo
import pl.garage.bmwassistant.model.isFinishedRepairStatus
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class VehicleDataSnapshot(
    val repairs: List<RepairProject> = emptyList(),
    val documentation: List<RepairDocumentation> = emptyList(),
    val shoppingList: List<ShoppingListItem> = emptyList(),
    val inventoryParts: List<PartInventoryItem> = emptyList(),
)

class GarageRepository(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val vehicleDatabaseManager = VehicleDatabaseManager(appContext)
    private val repairArchiveStorage = RepairProjectStorage(appContext)

    suspend fun loadVehicles(): List<Vehicle> =
        vehicleDatabaseManager.getActiveVehicles().map(VehicleCatalogEntity::toVehicle)

    suspend fun saveVehicle(vehicle: Vehicle): Vehicle =
        vehicleDatabaseManager.registerVehicle(vehicle).toVehicle()

    suspend fun deleteVehicle(vehicleId: String) {
        vehicleDatabaseManager.deleteVehicle(vehicleId)
    }

    suspend fun loadVehicleSnapshot(vehicle: Vehicle): VehicleDataSnapshot {
        val database = vehicleDatabaseManager.openVehicleDatabase(vehicle.id)
        val repairs = loadRepairs(database, vehicle.displayName)
        val documentation = loadDocumentation(database)
        val repairTitlesById = repairs.associateBy({ it.id }, { it.title })
        val shoppingList = loadShoppingList(database, repairTitlesById)
        val inventoryParts = loadInventoryParts(database, repairTitlesById)
        return VehicleDataSnapshot(
            repairs = repairs,
            documentation = documentation,
            shoppingList = shoppingList,
            inventoryParts = inventoryParts
        )
    }

    suspend fun saveVehicleSnapshot(
        vehicleId: String,
        snapshot: VehicleDataSnapshot,
    ) {
        val database = vehicleDatabaseManager.openVehicleDatabase(vehicleId)
        database.withTransaction {
            replaceRepairs(database, snapshot.repairs)
            replaceDocumentation(database, snapshot.documentation)
            replaceShoppingList(database, snapshot.shoppingList)
            replaceInventoryParts(database, snapshot.inventoryParts)
        }
    }

    fun createRepairArchiveExport(
        vehicle: Vehicle,
        repair: RepairProject,
        documentation: RepairDocumentation,
        shoppingItems: List<ShoppingListItem>,
    ): ByteArray = repairArchiveStorage.createRepairArchiveExport(
        vehicle = vehicle,
        repair = repair,
        documentation = documentation,
        shoppingItems = shoppingItems
    )

    fun peekRepairArchiveTitle(rawArchive: ByteArray): String? =
        repairArchiveStorage.peekRepairArchiveTitle(rawArchive)

    fun importRepairArchive(
        vehicle: Vehicle,
        rawArchive: ByteArray,
        importAsArchived: Boolean,
    ): ImportedRepairArchive? = repairArchiveStorage.importRepairArchive(
        vehicle = vehicle,
        rawArchive = rawArchive,
        importAsArchived = importAsArchived
    )

    suspend fun exportVehicleBackup(
        vehicle: Vehicle,
        outputStream: OutputStream,
    ): Boolean = runCatching {
        val descriptor = vehicleDatabaseManager.createDescriptor(vehicle.id)
        flushVehicleDatabase(vehicle.id)
        val manifest = JSONObject()
            .put("format", VEHICLE_BACKUP_FORMAT)
            .put("version", VEHICLE_BACKUP_VERSION)
            .put("exportedAtEpochMillis", System.currentTimeMillis())
            .put("vehicle", vehicle.toJson())

        ZipOutputStream(outputStream).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST_ENTRY_NAME))
            zip.write(manifest.toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            if (descriptor.databaseFile.exists()) {
                zip.putNextEntry(ZipEntry(DATABASE_ENTRY_NAME))
                descriptor.databaseFile.inputStream().use { input -> input.copyTo(zip) }
                zip.closeEntry()
            }

            if (descriptor.filesDirectory.exists()) {
                descriptor.filesDirectory.walkTopDown()
                    .filter { it.isFile }
                    .forEach { file ->
                        val relativePath = file.relativeTo(descriptor.filesDirectory)
                            .invariantSeparatorsPath
                        zip.putNextEntry(ZipEntry("$FILES_DIRECTORY_ENTRY_NAME/$relativePath"))
                        file.inputStream().use { input -> input.copyTo(zip) }
                        zip.closeEntry()
                    }
            }
        }
        true
    }.getOrDefault(false)

    suspend fun importVehicleBackup(
        inputStream: InputStream,
    ): Vehicle? = runCatching {
        val tempDirectory = File(appContext.cacheDir, "vehicle-import-${System.currentTimeMillis()}")
        tempDirectory.mkdirs()
        try {
            unzipVehicleBackup(inputStream, tempDirectory)

            val manifestFile = File(tempDirectory, MANIFEST_ENTRY_NAME)
            if (!manifestFile.exists()) return@runCatching null

            val manifest = JSONObject(manifestFile.readText(Charsets.UTF_8))
            if (manifest.optString("format") != VEHICLE_BACKUP_FORMAT) return@runCatching null

            val vehicleJson = manifest.optJSONObject("vehicle") ?: return@runCatching null
            val importedVehicle = vehicleJson.toVehicle().copy(id = "")
            val savedVehicle = saveVehicle(importedVehicle)
            val descriptor = vehicleDatabaseManager.createDescriptor(savedVehicle.id)

            val databaseBackupFile = File(tempDirectory, DATABASE_ENTRY_NAME)
            if (databaseBackupFile.exists()) {
                descriptor.databaseFile.parentFile?.mkdirs()
                databaseBackupFile.copyTo(descriptor.databaseFile, overwrite = true)
            }

            val extractedFilesDirectory = File(tempDirectory, FILES_DIRECTORY_ENTRY_NAME)
            if (extractedFilesDirectory.exists()) {
                descriptor.filesDirectory.deleteRecursively()
                descriptor.filesDirectory.mkdirs()
                extractedFilesDirectory.walkTopDown()
                    .filter { it.isFile }
                    .forEach { file ->
                        val destination = File(descriptor.filesDirectory, file.relativeTo(extractedFilesDirectory).invariantSeparatorsPath)
                        destination.parentFile?.mkdirs()
                        file.copyTo(destination, overwrite = true)
                    }
            }

            savedVehicle
        } finally {
            tempDirectory.deleteRecursively()
        }
    }.getOrNull()

    private suspend fun loadRepairs(
        database: VehicleDatabase,
        vehicleName: String,
    ): List<RepairProject> {
        val repairDao = database.repairProjectDao()
        return repairDao.getAllRepairs().map { repair ->
            val checkpoints = repairDao.getCheckpointsForRepair(repair.repairId).map(RepairCheckpointEntity::toModel)
            val partsToIdentify = repairDao.getPartsToIdentifyForRepair(repair.repairId).map { it.text }
            val documentsToCollect = repairDao.getDocumentsToCollectForRepair(repair.repairId).map { it.text }
            repair.toModel(
                vehicleName = vehicleName,
                checkpoints = checkpoints,
                partsToIdentify = partsToIdentify,
                documentsToCollect = documentsToCollect
            )
        }
    }

    private suspend fun loadDocumentation(
        database: VehicleDatabase,
    ): List<RepairDocumentation> {
        val documentationDao = database.repairDocumentationDao()
        return documentationDao.getAllDocumentation().map { documentation ->
            val archivedShopping = documentationDao
                .getArchivedShoppingItems(documentation.documentationId)
                .map { it.toModel(documentation.repairTitleSnapshot) }
            val tisDocuments = documentationDao
                .getTisLinks(documentation.documentationId)
                .map { it.toModel() }
            val youtubeVideos = documentationDao
                .getYoutubeVideos(documentation.documentationId)
                .map { it.toModel() }
            val personalNotes = documentationDao
                .getPersonalItems(documentation.documentationId)
                .map(PersonalDocumentationItemEntity::toModel)
            val torqueTables = documentationDao
                .getTorqueTables(documentation.documentationId)
                .map { table ->
                    val specEntities = documentationDao.getTorqueSpecs(table.tableId)
                    val specs = specEntities.map(TorqueSpecEntity::toModel)
                    val specIndexById = specEntities.mapIndexed { index, entity -> entity.torqueSpecId to index }.toMap()
                    val assignments = documentationDao
                        .getTorqueAssignments(table.tableId)
                        .mapNotNull { assignment ->
                            specIndexById[assignment.torqueSpecId]?.let(assignment::toModel)
                        }
                    table.toModel(
                        specs = specs,
                        assignments = assignments
                    )
                }

            RepairDocumentation(
                title = documentation.title,
                area = documentation.area.toVehicleArea(),
                repairTitle = documentation.repairTitleSnapshot,
                summary = documentation.summary,
                archivedShoppingList = archivedShopping,
                tisLinks = tisDocuments.map { it.url },
                tisDocuments = tisDocuments,
                torqueSpecs = torqueTables.firstOrNull()?.torqueSpecs.orEmpty(),
                torqueDiagramImageUri = torqueTables.firstOrNull()?.diagramImageUri,
                torqueDiagramAssignments = torqueTables.firstOrNull()?.diagramAssignments.orEmpty(),
                torqueTables = torqueTables,
                youtubeLinks = youtubeVideos.map { it.url },
                youtubeVideos = youtubeVideos,
                personalNotes = personalNotes,
                userNotes = documentation.userNotes,
                repairId = documentation.repairId
            )
        }
    }

    private suspend fun loadShoppingList(
        database: VehicleDatabase,
        repairTitlesById: Map<String, String>,
    ): List<ShoppingListItem> =
        database.shoppingListDao().getAllItems().map { item ->
            item.toModel(repairTitlesById[item.repairId].orEmpty())
        }

    private suspend fun loadInventoryParts(
        database: VehicleDatabase,
        repairTitlesById: Map<String, String>,
    ): List<PartInventoryItem> =
        database.inventoryPartDao().getAllParts().map { item ->
            item.toModel(item.repairId?.let(repairTitlesById::get))
        }

    private suspend fun replaceRepairs(
        database: VehicleDatabase,
        repairs: List<RepairProject>,
    ) {
        val repairDao = database.repairProjectDao()
        repairDao.clearAll()
        val now = System.currentTimeMillis()
        repairs.forEachIndexed { index, repair ->
            val repairId = repair.id.ifBlank { "repair_${index + 1}" }
            val normalizedRepair = repair.copy(id = repairId)
            repairDao.replaceRepairWithCheckpoints(
                repair = normalizedRepair.toEntity(
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                    sortOrder = index,
                    isArchived = normalizedRepair.status.isFinishedRepairStatus()
                ),
                checkpoints = normalizedRepair.checkpoints.mapIndexed { checkpointIndex, checkpoint ->
                    RepairCheckpointEntity(
                        checkpointId = checkpoint.id.ifBlank { "${repairId}_checkpoint_$checkpointIndex" },
                        repairId = repairId,
                        text = checkpoint.text,
                        isDone = checkpoint.isDone,
                        sortOrder = checkpointIndex
                    )
                },
                partsToIdentify = normalizedRepair.partsToIdentify.mapIndexed { itemIndex, text ->
                    RepairPartsToIdentifyEntity(
                        itemId = "${repairId}_part_to_identify_$itemIndex",
                        repairId = repairId,
                        text = text,
                        sortOrder = itemIndex
                    )
                },
                documentsToCollect = normalizedRepair.documentsToCollect.mapIndexed { itemIndex, text ->
                    RepairDocumentsToCollectEntity(
                        itemId = "${repairId}_document_to_collect_$itemIndex",
                        repairId = repairId,
                        text = text,
                        sortOrder = itemIndex
                    )
                }
            )
        }
    }

    private suspend fun replaceDocumentation(
        database: VehicleDatabase,
        documentationItems: List<RepairDocumentation>,
    ) {
        val documentationDao = database.repairDocumentationDao()
        documentationDao.clearAll()
        val now = System.currentTimeMillis()
        documentationItems.forEachIndexed { index, documentation ->
            val documentationId = "${documentation.repairId.ifBlank { "repair" }}_documentation_$index"
            val tisDocuments = documentation.tisDocuments.ifEmpty {
                documentation.tisLinks.mapIndexed { linkIndex, url ->
                    TisDocumentationLink(
                        title = "TIS ${linkIndex + 1}",
                        url = url
                    )
                }
            }
            val youtubeVideos = documentation.youtubeVideos.ifEmpty {
                documentation.youtubeLinks.mapIndexed { videoIndex, url ->
                    YoutubeVideo(
                        title = "Film YouTube ${videoIndex + 1}",
                        url = url
                    )
                }
            }
            val torqueTables = documentation.torqueTables.ifEmpty {
                if (
                    documentation.torqueSpecs.isEmpty() &&
                    documentation.torqueDiagramImageUri == null &&
                    documentation.torqueDiagramAssignments.isEmpty()
                ) {
                    emptyList()
                } else {
                    listOf(
                        TorqueSpecTable(
                            id = "${documentation.repairId}_table_0",
                            title = "Tabela momentow 1",
                            torqueSpecs = documentation.torqueSpecs,
                            diagramImageUri = documentation.torqueDiagramImageUri,
                            diagramAssignments = documentation.torqueDiagramAssignments
                        )
                    )
                }
            }

            val torqueBundles = torqueTables.mapIndexed { tableIndex, table ->
                val tableId = table.id.ifBlank { "${documentationId}_table_$tableIndex" }
                val normalizedTable = table.copy(id = tableId)
                val tableEntity = normalizedTable.toEntity(
                    documentationId = documentationId,
                    sortOrder = tableIndex
                )
                val specEntities = normalizedTable.torqueSpecs.mapIndexed { specIndex, spec ->
                    spec.toEntity(tableId = tableId, sortOrder = specIndex)
                }
                val assignmentEntities = normalizedTable.diagramAssignments.mapIndexedNotNull { assignmentIndex, assignment ->
                    val torqueSpecId = specEntities.getOrNull(assignment.torqueSpecIndex)?.torqueSpecId
                        ?: return@mapIndexedNotNull null
                    assignment.toEntity(
                        tableId = tableId,
                        torqueSpecId = torqueSpecId,
                        sortOrder = assignmentIndex
                    )
                }
                DocumentationTorqueBundle(
                    tableEntity = tableEntity,
                    specEntities = specEntities,
                    assignmentEntities = assignmentEntities
                )
            }

            documentationDao.replaceDocumentationBundle(
                documentation = documentation.toEntity(
                    documentationId = documentationId,
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now
                ),
                archivedShoppingItems = documentation.archivedShoppingList.mapIndexed { archivedIndex, item ->
                    item.toArchivedEntity(
                        documentationId = documentationId,
                        sortOrder = archivedIndex
                    )
                },
                tisLinks = tisDocuments.mapIndexed { linkIndex, link ->
                    link.toEntity(documentationId = documentationId, sortOrder = linkIndex)
                },
                youtubeVideos = youtubeVideos.mapIndexed { videoIndex, video ->
                    video.toEntity(documentationId = documentationId, sortOrder = videoIndex)
                },
                personalItems = documentation.personalNotes.mapIndexed { personalIndex, item ->
                    val itemId = item.id.ifBlank { "${documentationId}_personal_$personalIndex" }
                    item.copy(id = itemId).toEntity(
                        documentationId = documentationId,
                        sortOrder = personalIndex,
                        createdAtEpochMillis = now
                    )
                },
                torqueTables = torqueBundles.map(DocumentationTorqueBundle::tableEntity),
                torqueSpecs = torqueBundles.flatMap(DocumentationTorqueBundle::specEntities),
                torqueAssignments = torqueBundles.flatMap(DocumentationTorqueBundle::assignmentEntities)
            )
        }
    }

    private suspend fun replaceShoppingList(
        database: VehicleDatabase,
        shoppingListItems: List<ShoppingListItem>,
    ) {
        val shoppingDao = database.shoppingListDao()
        shoppingDao.clearAll()
        val now = System.currentTimeMillis()
        val entities = shoppingListItems.mapIndexed { index, item ->
            val itemId = item.id.ifBlank { "${item.repairId.ifBlank { "repair" }}_shopping_$index" }
            item.copy(id = itemId).toEntity(
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now
            )
        }
        if (entities.isNotEmpty()) {
            shoppingDao.insertAll(entities)
        }
    }

    private suspend fun replaceInventoryParts(
        database: VehicleDatabase,
        inventoryParts: List<PartInventoryItem>,
    ) {
        val inventoryDao = database.inventoryPartDao()
        inventoryDao.clearAll()
        val now = System.currentTimeMillis()
        val entities = inventoryParts.mapIndexed { index, part ->
            val partId = part.id.ifBlank { "${part.repairId ?: "inventory"}_part_$index" }
            part.copy(id = partId).toEntity(
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now
            )
        }
        if (entities.isNotEmpty()) {
            inventoryDao.insertAll(entities)
        }
    }

    private fun flushVehicleDatabase(vehicleId: String) {
        val database = vehicleDatabaseManager.openVehicleDatabase(vehicleId)
        database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use {
            while (it.moveToNext()) {
                // We only need to force the checkpoint before copying the db file.
            }
        }
    }

    private fun unzipVehicleBackup(
        inputStream: InputStream,
        targetDirectory: File,
    ) {
        ZipInputStream(inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val safeFile = File(targetDirectory, entry.name).canonicalFile
                if (!safeFile.path.startsWith(targetDirectory.canonicalPath + File.separator)) {
                    throw IllegalStateException("Invalid backup entry path.")
                }
                if (entry.isDirectory) {
                    safeFile.mkdirs()
                } else {
                    safeFile.parentFile?.mkdirs()
                    safeFile.outputStream().use { output -> zip.copyTo(output) }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }

    private fun Vehicle.toJson(): JSONObject = JSONObject()
        .put("brand", brand)
        .put("model", model)
        .put("generation", generation)
        .put("engine", engine)
        .put("year", year)
        .put("vin", vin)
        .put("mileage", mileage)
        .put("note", note)
        .put("partsCatalogUrl", partsCatalogUrl)

    private fun JSONObject.toVehicle(): Vehicle = Vehicle(
        brand = optString("brand"),
        model = optString("model"),
        generation = optString("generation"),
        engine = optString("engine"),
        year = optString("year"),
        vin = optString("vin"),
        mileage = optString("mileage"),
        note = optString("note"),
        id = optString("id"),
        partsCatalogUrl = optString("partsCatalogUrl")
    )

    private companion object {
        const val VEHICLE_BACKUP_FORMAT = "BMW_GARAGE_VEHICLE_BACKUP"
        const val VEHICLE_BACKUP_VERSION = 1
        const val MANIFEST_ENTRY_NAME = "manifest.json"
        const val DATABASE_ENTRY_NAME = "vehicle.db"
        const val FILES_DIRECTORY_ENTRY_NAME = "files"
    }
}

private data class DocumentationTorqueBundle(
    val tableEntity: TorqueSpecTableEntity,
    val specEntities: List<TorqueSpecEntity>,
    val assignmentEntities: List<TorqueDiagramAssignmentEntity>,
)
