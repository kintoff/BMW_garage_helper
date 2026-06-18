package pl.garage.bmwassistant.database.migration

import android.content.Context
import androidx.core.content.edit
import pl.garage.bmwassistant.data.PartInventoryStorage
import pl.garage.bmwassistant.data.RepairProjectStorage
import pl.garage.bmwassistant.data.VehicleStorage
import pl.garage.bmwassistant.database.catalog.VehicleDatabaseManager
import pl.garage.bmwassistant.database.vehicle.DocumentationArchivedShoppingItemEntity
import pl.garage.bmwassistant.database.vehicle.InventoryPartEntity
import pl.garage.bmwassistant.database.vehicle.PersonalDocumentationItemEntity
import pl.garage.bmwassistant.database.vehicle.RepairCheckpointEntity
import pl.garage.bmwassistant.database.vehicle.RepairDocumentationEntity
import pl.garage.bmwassistant.database.vehicle.RepairDocumentsToCollectEntity
import pl.garage.bmwassistant.database.vehicle.RepairPartsToIdentifyEntity
import pl.garage.bmwassistant.database.vehicle.RepairProjectEntity
import pl.garage.bmwassistant.database.vehicle.ShoppingListItemEntity
import pl.garage.bmwassistant.database.vehicle.TisDocumentationLinkEntity
import pl.garage.bmwassistant.database.vehicle.TorqueDiagramAssignmentEntity
import pl.garage.bmwassistant.database.vehicle.TorqueSpecEntity
import pl.garage.bmwassistant.database.vehicle.TorqueSpecTableEntity
import pl.garage.bmwassistant.database.vehicle.VehicleDatabase
import pl.garage.bmwassistant.database.vehicle.YoutubeVideoEntity
import pl.garage.bmwassistant.database.vehicle.initialHistoryEvent
import pl.garage.bmwassistant.database.vehicle.toArchivedEntity
import pl.garage.bmwassistant.database.vehicle.toEntity
import pl.garage.bmwassistant.model.PersonalDocumentationItem
import pl.garage.bmwassistant.model.RepairDocumentation
import pl.garage.bmwassistant.model.RepairProject
import pl.garage.bmwassistant.model.ShoppingListItem
import pl.garage.bmwassistant.model.TisDocumentationLink
import pl.garage.bmwassistant.model.TorqueSpecTable
import pl.garage.bmwassistant.model.Vehicle
import pl.garage.bmwassistant.model.YoutubeVideo

class LegacyStorageRoomMigrator(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val migrationPreferences = appContext.getSharedPreferences(MIGRATION_PREFS_NAME, Context.MODE_PRIVATE)
    private val vehicleStorage = VehicleStorage(appContext)
    private val repairStorage = RepairProjectStorage(appContext)
    private val partStorage = PartInventoryStorage(appContext)
    private val vehicleDatabaseManager = VehicleDatabaseManager(appContext)

    suspend fun migrateIfNeeded(): LegacyMigrationResult {
        if (isMigrationComplete()) {
            return LegacyMigrationResult(
                migrated = false,
                migratedVehicles = 0
            )
        }

        val vehicles = vehicleStorage.loadVehicles()
        vehicles.forEach { vehicle ->
            migrateVehicle(vehicle)
        }

        migrationPreferences.edit {
            putBoolean(KEY_LEGACY_TO_ROOM_COMPLETE, true)
            putLong(KEY_LEGACY_TO_ROOM_COMPLETED_AT, System.currentTimeMillis())
        }

        return LegacyMigrationResult(
            migrated = true,
            migratedVehicles = vehicles.size
        )
    }

    fun isMigrationComplete(): Boolean =
        migrationPreferences.getBoolean(KEY_LEGACY_TO_ROOM_COMPLETE, false)

    private suspend fun migrateVehicle(vehicle: Vehicle) {
        val registeredVehicle = vehicleDatabaseManager.registerVehicle(vehicle)
        val database = vehicleDatabaseManager.openVehicleDatabase(registeredVehicle.vehicleId)
        val repairs = repairStorage.loadRepairs(vehicle)
        val documentation = repairStorage.loadDocumentation(vehicle)
        val shoppingItems = partStorage.loadShoppingList(vehicle)
        val inventoryParts = partStorage.loadParts(vehicle)

        migrateRepairs(database, repairs)
        migrateShoppingItems(database, shoppingItems)
        migrateInventoryParts(database, inventoryParts)
        migrateDocumentation(database, documentation)
    }

    private suspend fun migrateRepairs(
        database: VehicleDatabase,
        repairs: List<RepairProject>,
    ) {
        val repairDao = database.repairProjectDao()
        repairs.forEachIndexed { index, repair ->
            val now = System.currentTimeMillis()
            val repairId = repair.id.ifBlank { "repair_${index + 1}" }
            val repairEntity = repair.copy(id = repairId).toEntity(
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now,
                sortOrder = index
            )
            val checkpoints = repair.checkpoints.mapIndexed { checkpointIndex, checkpoint ->
                RepairCheckpointEntity(
                    checkpointId = checkpoint.id.ifBlank { "${repairId}_checkpoint_$checkpointIndex" },
                    repairId = repairId,
                    text = checkpoint.text,
                    isDone = checkpoint.isDone,
                    sortOrder = checkpointIndex
                )
            }
            val partsToIdentify = repair.partsToIdentify.mapIndexed { itemIndex, text ->
                RepairPartsToIdentifyEntity(
                    itemId = "${repairId}_part_to_identify_$itemIndex",
                    repairId = repairId,
                    text = text,
                    sortOrder = itemIndex
                )
            }
            val documentsToCollect = repair.documentsToCollect.mapIndexed { itemIndex, text ->
                RepairDocumentsToCollectEntity(
                    itemId = "${repairId}_document_to_collect_$itemIndex",
                    repairId = repairId,
                    text = text,
                    sortOrder = itemIndex
                )
            }
            repairDao.replaceRepairWithCheckpoints(
                repair = repairEntity,
                checkpoints = checkpoints,
                partsToIdentify = partsToIdentify,
                documentsToCollect = documentsToCollect
            )
        }
    }

    private suspend fun migrateShoppingItems(
        database: VehicleDatabase,
        items: List<ShoppingListItem>,
    ) {
        val shoppingDao = database.shoppingListDao()
        val now = System.currentTimeMillis()
        val entities = items.mapIndexed { index, item ->
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

    private suspend fun migrateInventoryParts(
        database: VehicleDatabase,
        parts: List<pl.garage.bmwassistant.model.PartInventoryItem>,
    ) {
        val inventoryDao = database.inventoryPartDao()
        val historyDao = database.inventoryHistoryDao()
        val now = System.currentTimeMillis()
        val entities = parts.mapIndexed { index, part ->
            val inventoryId = part.id.ifBlank { "${part.repairId ?: "inventory"}_part_$index" }
            part.copy(id = inventoryId).toEntity(
                createdAtEpochMillis = part.createdAtEpochMillis.takeIf { it > 0L } ?: now,
                updatedAtEpochMillis = part.updatedAtEpochMillis.takeIf { it > 0L } ?: now
            )
        }
        if (entities.isNotEmpty()) {
            inventoryDao.insertAll(entities)
            historyDao.insertAll(entities.map { it.initialHistoryEvent() })
        }
    }

    private suspend fun migrateDocumentation(
        database: VehicleDatabase,
        items: List<RepairDocumentation>,
    ) {
        val documentationDao = database.repairDocumentationDao()
        val now = System.currentTimeMillis()

        items.forEachIndexed { index, item ->
            val documentationId = "${item.repairId.ifBlank { "repair" }}_documentation_$index"
            val documentationEntity = item.toEntity(
                documentationId = documentationId,
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now
            )

            val archivedShoppingItems = item.archivedShoppingList.mapIndexed { archivedIndex, shoppingItem ->
                val archivedId = shoppingItem.id.ifBlank { "${documentationId}_archived_$archivedIndex" }
                shoppingItem.copy(id = archivedId).toArchivedEntity(
                    documentationId = documentationId,
                    sortOrder = archivedIndex
                )
            }
            val tisLinks = migrationEffectiveTisLinks(item).mapIndexed { linkIndex, link ->
                link.toEntity(
                    documentationId = documentationId,
                    sortOrder = linkIndex
                )
            }
            val youtubeVideos = migrationEffectiveYoutubeVideos(item).mapIndexed { videoIndex, video ->
                video.toEntity(
                    documentationId = documentationId,
                    sortOrder = videoIndex
                )
            }
            val personalItems = item.personalNotes.mapIndexed { personalIndex, personalItem ->
                val itemId = personalItem.id.ifBlank { "${documentationId}_personal_$personalIndex" }
                personalItem.copy(id = itemId).toEntity(
                    documentationId = documentationId,
                    sortOrder = personalIndex,
                    createdAtEpochMillis = now
                )
            }

            val torqueTableBundle = migrationEffectiveTorqueTables(item).flatMapIndexed { tableIndex, table ->
                val tableId = table.id.ifBlank { "${documentationId}_table_$tableIndex" }
                val tableEntity = table.copy(id = tableId).toEntity(
                    documentationId = documentationId,
                    sortOrder = tableIndex
                )
                val specEntities = table.torqueSpecs.mapIndexed { specIndex, spec ->
                    spec.toEntity(tableId = tableId, sortOrder = specIndex)
                }
                val assignmentEntities = table.diagramAssignments.mapIndexedNotNull { assignmentIndex, assignment ->
                    val torqueSpecId = specEntities.getOrNull(assignment.torqueSpecIndex)?.torqueSpecId
                        ?: return@mapIndexedNotNull null
                    assignment.toEntity(
                        tableId = tableId,
                        torqueSpecId = torqueSpecId,
                        sortOrder = assignmentIndex
                    )
                }
                listOf(
                    TorqueTableBundle(
                        tableEntity = tableEntity,
                        specEntities = specEntities,
                        assignmentEntities = assignmentEntities
                    )
                )
            }

            documentationDao.replaceDocumentationBundle(
                documentation = documentationEntity,
                archivedShoppingItems = archivedShoppingItems,
                tisLinks = tisLinks,
                youtubeVideos = youtubeVideos,
                personalItems = personalItems,
                torqueTables = torqueTableBundle.map(TorqueTableBundle::tableEntity),
                torqueSpecs = torqueTableBundle.flatMap(TorqueTableBundle::specEntities),
                torqueAssignments = torqueTableBundle.flatMap(TorqueTableBundle::assignmentEntities)
            )
        }
    }

    private companion object {
        const val MIGRATION_PREFS_NAME = "garage_database_migration"
        const val KEY_LEGACY_TO_ROOM_COMPLETE = "legacy_to_room_complete"
        const val KEY_LEGACY_TO_ROOM_COMPLETED_AT = "legacy_to_room_completed_at"
    }
}

data class LegacyMigrationResult(
    val migrated: Boolean,
    val migratedVehicles: Int,
)

private data class TorqueTableBundle(
    val tableEntity: TorqueSpecTableEntity,
    val specEntities: List<TorqueSpecEntity>,
    val assignmentEntities: List<TorqueDiagramAssignmentEntity>,
)

internal fun migrationEffectiveTisLinks(item: RepairDocumentation): List<TisDocumentationLink> =
    item.tisDocuments.ifEmpty {
        item.tisLinks.mapIndexed { index, url ->
            TisDocumentationLink(
                title = "TIS ${index + 1}",
                url = url
            )
        }
    }

internal fun migrationEffectiveYoutubeVideos(item: RepairDocumentation): List<YoutubeVideo> =
    item.youtubeVideos.ifEmpty {
        item.youtubeLinks.mapIndexed { index, url ->
            YoutubeVideo(
                title = "Film YouTube ${index + 1}",
                url = url
            )
        }
    }

internal fun migrationEffectiveTorqueTables(item: RepairDocumentation): List<TorqueSpecTable> =
    item.torqueTables.ifEmpty {
        if (
            item.torqueSpecs.isEmpty() &&
            item.torqueDiagramImageUri == null &&
            item.torqueDiagramAssignments.isEmpty()
        ) {
            emptyList()
        } else {
            listOf(
                TorqueSpecTable(
                    id = "${item.repairId}_table_0",
                    title = "Tabela momentow 1",
                    torqueSpecs = item.torqueSpecs,
                    diagramImageUri = item.torqueDiagramImageUri,
                    diagramAssignments = item.torqueDiagramAssignments
                )
            )
        }
    }
