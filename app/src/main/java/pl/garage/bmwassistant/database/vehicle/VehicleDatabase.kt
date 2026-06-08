package pl.garage.bmwassistant.database.vehicle

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        RepairProjectEntity::class,
        RepairCheckpointEntity::class,
        RepairPartsToIdentifyEntity::class,
        RepairDocumentsToCollectEntity::class,
        RepairDocumentationEntity::class,
        DocumentationArchivedShoppingItemEntity::class,
        ShoppingListItemEntity::class,
        InventoryPartEntity::class,
        TisDocumentationLinkEntity::class,
        YoutubeVideoEntity::class,
        PersonalDocumentationItemEntity::class,
        TorqueSpecTableEntity::class,
        TorqueSpecEntity::class,
        TorqueDiagramAssignmentEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class VehicleDatabase : RoomDatabase() {
    abstract fun repairProjectDao(): RepairProjectDao
    abstract fun repairDocumentationDao(): RepairDocumentationDao
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun inventoryPartDao(): InventoryPartDao
}
