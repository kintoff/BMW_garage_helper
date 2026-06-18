package pl.garage.bmwassistant.database.vehicle

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
        InventoryHistoryEventEntity::class,
        TisDocumentationLinkEntity::class,
        YoutubeVideoEntity::class,
        PersonalDocumentationItemEntity::class,
        TorqueSpecTableEntity::class,
        TorqueSpecEntity::class,
        TorqueDiagramAssignmentEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class VehicleDatabase : RoomDatabase() {
    abstract fun repairProjectDao(): RepairProjectDao
    abstract fun repairDocumentationDao(): RepairDocumentationDao
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun inventoryPartDao(): InventoryPartDao
    abstract fun inventoryHistoryDao(): InventoryHistoryDao
}

val VEHICLE_DATABASE_MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val inventoryColumns = db.columnNames("inventory_parts")
        val originShoppingItemId = inventoryColumns.sqlColumnOrDefault("originShoppingItemId", "NULL")
        val repairId = if ("repairId" in inventoryColumns) {
            """
            CASE
                WHEN `repairId` IN (SELECT `repairId` FROM `repair_projects`)
                THEN `repairId`
                ELSE NULL
            END
            """.trimIndent()
        } else {
            "NULL"
        }
        val locationNote = inventoryColumns.sqlColumnOrDefault("locationNote", "''")
        val createdAtEpochMillis = inventoryColumns.sqlColumnOrDefault("createdAtEpochMillis", "0")
        val updatedAtEpochMillis = inventoryColumns.sqlColumnOrDefault("updatedAtEpochMillis", "0")
        val realOemUrl = inventoryColumns.sqlColumnOrDefault("realOemUrl", "NULL")
        val photoUri = inventoryColumns.sqlColumnOrDefault("photoUri", "NULL")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `inventory_parts_new` (
                `inventoryPartId` TEXT NOT NULL,
                `originShoppingItemId` TEXT,
                `repairId` TEXT,
                `oemPartNumber` TEXT NOT NULL,
                `manufacturerPartNumber` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `manufacturer` TEXT NOT NULL,
                `quantity` INTEGER NOT NULL,
                `purchasePrice` TEXT NOT NULL,
                `realOemUrl` TEXT,
                `photoUri` TEXT,
                `locationNote` TEXT NOT NULL,
                `createdAtEpochMillis` INTEGER NOT NULL,
                `updatedAtEpochMillis` INTEGER NOT NULL,
                PRIMARY KEY(`inventoryPartId`),
                FOREIGN KEY(`repairId`) REFERENCES `repair_projects`(`repairId`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `inventory_parts_new` (
                `inventoryPartId`,
                `originShoppingItemId`,
                `repairId`,
                `oemPartNumber`,
                `manufacturerPartNumber`,
                `name`,
                `manufacturer`,
                `quantity`,
                `purchasePrice`,
                `realOemUrl`,
                `photoUri`,
                `locationNote`,
                `createdAtEpochMillis`,
                `updatedAtEpochMillis`
            )
            SELECT
                `inventoryPartId`,
                $originShoppingItemId,
                $repairId,
                `oemPartNumber`,
                `manufacturerPartNumber`,
                `name`,
                `manufacturer`,
                `quantity`,
                `purchasePrice`,
                $realOemUrl,
                $photoUri,
                $locationNote,
                $createdAtEpochMillis,
                $updatedAtEpochMillis
            FROM `inventory_parts`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `inventory_parts`")
        db.execSQL("ALTER TABLE `inventory_parts_new` RENAME TO `inventory_parts`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_parts_repairId` ON `inventory_parts` (`repairId`)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_inventory_parts_originShoppingItemId` ON `inventory_parts` (`originShoppingItemId`)"
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `inventory_history_events` (
                `eventId` TEXT NOT NULL,
                `inventoryPartId` TEXT NOT NULL,
                `eventType` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `quantityDelta` INTEGER NOT NULL,
                `quantityAfter` INTEGER NOT NULL,
                `note` TEXT NOT NULL,
                `createdAtEpochMillis` INTEGER NOT NULL,
                PRIMARY KEY(`eventId`)
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_inventory_history_events_inventoryPartId` ON `inventory_history_events` (`inventoryPartId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_inventory_history_events_createdAtEpochMillis` ON `inventory_history_events` (`createdAtEpochMillis`)"
        )
        db.execSQL(
            """
            INSERT OR IGNORE INTO `inventory_history_events` (
                `eventId`,
                `inventoryPartId`,
                `eventType`,
                `title`,
                `quantityDelta`,
                `quantityAfter`,
                `note`,
                `createdAtEpochMillis`
            )
            SELECT
                `inventoryPartId` || '_created',
                `inventoryPartId`,
                CASE
                    WHEN `originShoppingItemId` IS NULL OR `originShoppingItemId` = ''
                    THEN 'ADDED_MANUALLY'
                    ELSE 'ACCEPTED_FROM_SHOPPING'
                END,
                CASE
                    WHEN `originShoppingItemId` IS NULL OR `originShoppingItemId` = ''
                    THEN 'Dodano do magazynu'
                    ELSE 'Przyjęto do magazynu'
                END,
                `quantity`,
                `quantity`,
                '',
                CASE
                    WHEN `createdAtEpochMillis` > 0
                    THEN `createdAtEpochMillis`
                    ELSE CAST(strftime('%s', 'now') AS INTEGER) * 1000
                END
            FROM `inventory_parts`
            """.trimIndent()
        )
    }
}

val VEHICLE_DATABASE_MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `inventory_history_events_new` (
                `eventId` TEXT NOT NULL,
                `inventoryPartId` TEXT NOT NULL,
                `eventType` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `quantityDelta` INTEGER NOT NULL,
                `quantityAfter` INTEGER NOT NULL,
                `note` TEXT NOT NULL,
                `createdAtEpochMillis` INTEGER NOT NULL,
                PRIMARY KEY(`eventId`)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT OR IGNORE INTO `inventory_history_events_new` (
                `eventId`,
                `inventoryPartId`,
                `eventType`,
                `title`,
                `quantityDelta`,
                `quantityAfter`,
                `note`,
                `createdAtEpochMillis`
            )
            SELECT
                `eventId`,
                `inventoryPartId`,
                `eventType`,
                `title`,
                `quantityDelta`,
                `quantityAfter`,
                `note`,
                `createdAtEpochMillis`
            FROM `inventory_history_events`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `inventory_history_events`")
        db.execSQL("ALTER TABLE `inventory_history_events_new` RENAME TO `inventory_history_events`")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_inventory_history_events_inventoryPartId` ON `inventory_history_events` (`inventoryPartId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_inventory_history_events_createdAtEpochMillis` ON `inventory_history_events` (`createdAtEpochMillis`)"
        )
    }
}

val VEHICLE_DATABASE_MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.rebuildInventoryPartsWithoutShoppingOriginForeignKey()
    }
}

private fun SupportSQLiteDatabase.columnNames(tableName: String): Set<String> {
    val columns = mutableSetOf<String>()
    query("PRAGMA table_info(`$tableName`)").use { cursor ->
        val nameIndex = cursor.getColumnIndex("name")
        while (cursor.moveToNext()) {
            if (nameIndex >= 0) {
                columns += cursor.getString(nameIndex)
            }
        }
    }
    return columns
}

private fun Set<String>.sqlColumnOrDefault(columnName: String, defaultValue: String): String =
    if (columnName in this) "`$columnName`" else defaultValue

private fun SupportSQLiteDatabase.rebuildInventoryPartsWithoutShoppingOriginForeignKey() {
    execSQL(
        """
        CREATE TABLE IF NOT EXISTS `inventory_parts_new` (
            `inventoryPartId` TEXT NOT NULL,
            `originShoppingItemId` TEXT,
            `repairId` TEXT,
            `oemPartNumber` TEXT NOT NULL,
            `manufacturerPartNumber` TEXT NOT NULL,
            `name` TEXT NOT NULL,
            `manufacturer` TEXT NOT NULL,
            `quantity` INTEGER NOT NULL,
            `purchasePrice` TEXT NOT NULL,
            `realOemUrl` TEXT,
            `photoUri` TEXT,
            `locationNote` TEXT NOT NULL,
            `createdAtEpochMillis` INTEGER NOT NULL,
            `updatedAtEpochMillis` INTEGER NOT NULL,
            PRIMARY KEY(`inventoryPartId`),
            FOREIGN KEY(`repairId`) REFERENCES `repair_projects`(`repairId`) ON UPDATE NO ACTION ON DELETE SET NULL
        )
        """.trimIndent()
    )
    execSQL(
        """
        INSERT INTO `inventory_parts_new` (
            `inventoryPartId`,
            `originShoppingItemId`,
            `repairId`,
            `oemPartNumber`,
            `manufacturerPartNumber`,
            `name`,
            `manufacturer`,
            `quantity`,
            `purchasePrice`,
            `realOemUrl`,
            `photoUri`,
            `locationNote`,
            `createdAtEpochMillis`,
            `updatedAtEpochMillis`
        )
        SELECT
            `inventoryPartId`,
            `originShoppingItemId`,
            `repairId`,
            `oemPartNumber`,
            `manufacturerPartNumber`,
            `name`,
            `manufacturer`,
            `quantity`,
            `purchasePrice`,
            `realOemUrl`,
            `photoUri`,
            `locationNote`,
            `createdAtEpochMillis`,
            `updatedAtEpochMillis`
        FROM `inventory_parts`
        """.trimIndent()
    )
    execSQL("DROP TABLE `inventory_parts`")
    execSQL("ALTER TABLE `inventory_parts_new` RENAME TO `inventory_parts`")
    execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_parts_repairId` ON `inventory_parts` (`repairId`)")
    execSQL(
        "CREATE INDEX IF NOT EXISTS `index_inventory_parts_originShoppingItemId` ON `inventory_parts` (`originShoppingItemId`)"
    )
}
