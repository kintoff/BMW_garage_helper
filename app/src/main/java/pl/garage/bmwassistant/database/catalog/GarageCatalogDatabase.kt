package pl.garage.bmwassistant.database.catalog

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [VehicleCatalogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class GarageCatalogDatabase : RoomDatabase() {
    abstract fun vehicleCatalogDao(): VehicleCatalogDao
}
